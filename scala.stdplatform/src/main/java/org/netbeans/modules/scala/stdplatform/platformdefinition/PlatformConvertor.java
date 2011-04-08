/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */

package org.netbeans.modules.scala.stdplatform.platformdefinition;

import java.beans.*;
import java.io.*;
import java.lang.ref.*;
import java.util.*;
import java.util.List;
import java.net.URL;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.api.scala.platform.ScalaPlatform;
import org.netbeans.modules.scala.stdplatform.wizard.J2SEWizardIterator;
import org.netbeans.spi.project.support.ant.EditableProperties;
import org.netbeans.spi.project.support.ant.PropertyUtils;

import org.openide.ErrorManager;
import org.openide.modules.SpecificationVersion;
import org.openide.cookies.*;
import org.openide.filesystems.*;
import org.openide.filesystems.FileChangeAdapter;
import org.openide.loaders.*;
import org.openide.nodes.Node;
import org.openide.util.*;
import org.openide.util.lookup.*;
import org.openide.xml.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.xml.sax.*;


/**
 * Reads and writes the standard platform format implemented by PlatformImpl2.
 *
 * @author Svata Dedic
 */
public class PlatformConvertor implements Environment.Provider, InstanceCookie.Of, PropertyChangeListener, Runnable, InstanceContent.Convertor {

    private static final String CLASSIC = "classic";        //NOI18N
    private static final String MODERN = "modern";          //NOI18N
    private static final String JAVAC13 = "javac1.3";       //NOI18N
    static final String[] IMPORTANT_TOOLS = {
        // Used by scala project:
        "scalac", // NOI18N
        "scala", // NOI18N
        // Might be used, though currently not (cf. #46901):
        "scaladoc", // NOI18N
    };
    
    private static final String PLATFORM_DTD_ID = "-//NetBeans//DTD Scala PlatformDefinition 1.0//EN"; // NOI18N

    private PlatformConvertor() {}

    public static PlatformConvertor createProvider(FileObject reg) {
        return new PlatformConvertor();
    }
    
    public Lookup getEnvironment(DataObject obj) {
        return new PlatformConvertor((XMLDataObject)obj).getLookup();
    }
    
    InstanceContent cookies = new InstanceContent();
    
    private XMLDataObject   holder;

    private boolean defaultPlatform;

    private Lookup  lookup;
    
    private RequestProcessor.Task    saveTask;
    
    private Reference<ScalaPlatform>   refPlatform = new WeakReference<ScalaPlatform>(null);
    
    private LinkedList<PropertyChangeEvent> keepAlive = new LinkedList<PropertyChangeEvent>();
    
    private PlatformConvertor(XMLDataObject  object) {
        this.holder = object;
        this.holder.getPrimaryFile().addFileChangeListener( new FileChangeAdapter () {
            public void fileDeleted (final FileEvent fe) {
                if (!defaultPlatform) {
                    try {
                    ProjectManager.mutex().writeAccess( new Mutex.ExceptionAction<Void> () {
                        public Void run () throws IOException {
                            String systemName = fe.getFile().getName();
                            String propPrefix =  "platforms." + systemName + ".";   //NOI18N
                            boolean changed = false;
                            EditableProperties props = PropertyUtils.getGlobalProperties();
                            for (Iterator<String> it = props.keySet().iterator(); it.hasNext(); ) {
                                String key = it.next ();
                                if (key.startsWith(propPrefix)) {
                                    it.remove();
                                    changed =true;
                                }
                            }
                            if (changed) {
                                PropertyUtils.putGlobalProperties(props);
                            }
                            return null;
                        }
                    });
                    } catch (MutexException e) {
                        ErrorManager.getDefault().notify(e);
                    }
                }
            }
        });
        cookies = new InstanceContent();
        cookies.add(this);
        lookup = new AbstractLookup(cookies);
        cookies.add(Node.class, this);
    }
    
    Lookup getLookup() {
        return lookup;
    }
    
    public Class instanceClass() {
        return ScalaPlatform.class;
    }
    
    public Object instanceCreate() throws java.io.IOException, ClassNotFoundException {
        synchronized (this) {
            Object o = refPlatform.get();
            if (o != null)
                return o;
            H handler = new H();
            try {
                XMLReader reader = XMLUtil.createXMLReader();
                InputSource is = new org.xml.sax.InputSource(
                    holder.getPrimaryFile().getInputStream());
                is.setSystemId(holder.getPrimaryFile().getURL().toExternalForm());
                reader.setContentHandler(handler);
                reader.setErrorHandler(handler);
                reader.setEntityResolver(handler);

                reader.parse(is);
            } catch (SAXException ex) {
                Exception x = ex.getException();
                ex.printStackTrace();
                if (x instanceof java.io.IOException)
                    throw (IOException)x;
                else
                    throw new java.io.IOException(ex.getMessage());
            }

            ScalaPlatform inst = createPlatform(handler);
            refPlatform = new WeakReference<ScalaPlatform>(inst);
            return inst;
        }
    }
    
    ScalaPlatform createPlatform(H handler) {
        ScalaPlatform p;
        
        if (handler.isDefault) {
            p = DefaultPlatformImpl.create (handler.properties, handler.sources, handler.javadoc);
            defaultPlatform = true;
        } else {
            p = new J2SEPlatformImpl(handler.name,handler.installFolders, handler.properties, handler.sysProperties,handler.sources, handler.javadoc);
            defaultPlatform = false;
        }
        p.addPropertyChangeListener(this);
        return p;
    }
    
    public String instanceName() {
        return holder.getName();
    }
    
    public boolean instanceOf(Class<?> type) {
        return (type.isAssignableFrom(ScalaPlatform.class));
    }
    
    static int DELAY = 2000;
    
    public void propertyChange(PropertyChangeEvent evt) {
        synchronized (this) {
            if (saveTask == null)
                saveTask = RequestProcessor.getDefault().create(this);
        }
        synchronized (this) {
            keepAlive.add(evt);
        }
        saveTask.schedule(DELAY);
    }
    
    public void run() {
        PropertyChangeEvent e;
        
        synchronized (this) {
            e = keepAlive.removeFirst();
        }
        ScalaPlatform plat = (ScalaPlatform)e.getSource();
        try {
            holder.getPrimaryFile().getFileSystem().runAtomicAction(
                new W(plat, holder, defaultPlatform));
        } catch (java.io.IOException ex) {
            ErrorManager.getDefault().notify(ErrorManager.INFORMATIONAL, ex);
        }
    }
    
    public Object convert(Object obj) {
        if (obj == Node.class) {
            try {
                J2SEPlatformImpl p = (J2SEPlatformImpl) instanceCreate();
                return new J2SEPlatformNode (p,this.holder);
            } catch (IOException ex) {
                ErrorManager.getDefault().notify(ex);
            } catch (ClassNotFoundException ex) {
                ErrorManager.getDefault().notify(ex);
            } catch (Exception ex) {
                ErrorManager.getDefault().notify(ex);
            }
        }
        return null;
    }
    
    public String displayName(Object obj) {
        return ((Class)obj).getName();
    }
    
    public String id(Object obj) {
        return obj.toString();
    }
    
    public Class type(Object obj) {
        return (Class)obj;
    }
    
    public static DataObject create(final ScalaPlatform plat, final DataFolder f, final String idName) throws IOException {
        W w = new W(plat, f, idName);
        f.getPrimaryFile().getFileSystem().runAtomicAction(w);
        try {
            ProjectManager.mutex().writeAccess(
                    new Mutex.ExceptionAction<Void> () {
                        public Void run () throws Exception {
                            EditableProperties props = PropertyUtils.getGlobalProperties();
                            generatePlatformProperties(plat, idName, props);
                            PropertyUtils.putGlobalProperties (props);
                            return null;
                        }
                    });
        } catch (MutexException me) {
            Exception originalException = me.getException();
            if (originalException instanceof RuntimeException) {
                throw (RuntimeException) originalException;
            }
            else if (originalException instanceof IOException) {
                throw (IOException) originalException;
            }
            else
            {
                throw new IllegalStateException (); //Should never happen
            }
        }
        return w.holder;
    }

    public static void generatePlatformProperties (ScalaPlatform platform, String systemName, EditableProperties props) throws IOException {
        String homePropName = createName(systemName,"home");      //NOI18N
        String bootClassPathPropName = createName(systemName,"bootclasspath");    //NOI18N
        String compilerType= createName (systemName,"compiler");  //NOI18N
        if (props.getProperty(homePropName) != null || props.getProperty(bootClassPathPropName) != null
                || props.getProperty(compilerType)!=null) {
            //Already defined warn user
            String msg = NbBundle.getMessage(J2SEWizardIterator.class,"ERROR_InvalidName"); //NOI18N
            throw (IllegalStateException)ErrorManager.getDefault().annotate(
                    new IllegalStateException(msg), ErrorManager.USER, null, msg,null, null);
        }
        Collection installFolders = platform.getInstallFolders();
        if (installFolders.size()>0) {
            File scalaHome = FileUtil.toFile ((FileObject)installFolders.iterator().next());
            props.setProperty(homePropName, scalaHome.getAbsolutePath());
            ClassPath bootCP = platform.getBootstrapLibraries();
            StringBuffer sbootcp = new StringBuffer();
            for (ClassPath.Entry entry : bootCP.entries()) {
                URL url = entry.getURL();
                if ("jar".equals(url.getProtocol())) {              //NOI18N
                    url = FileUtil.getArchiveFile(url);
                }
                File root = new File (URI.create(url.toExternalForm()));
                if (sbootcp.length()>0) {
                    sbootcp.append(File.pathSeparator);
                }
                sbootcp.append(normalizePath(root, scalaHome, homePropName));
            }
            props.setProperty(bootClassPathPropName,sbootcp.toString());   //NOI18N
            props.setProperty(compilerType,getCompilerType(platform));
            for (int i = 0; i < IMPORTANT_TOOLS.length; i++) {
                String name = IMPORTANT_TOOLS[i];
                FileObject tool = platform.findTool(name);
                if (tool != null) {
                    if (!isDefaultLocation(tool, platform.getInstallFolders())) {
                        String toolName = createName(systemName, name);
                        props.setProperty(toolName, normalizePath(getToolPath(tool), scalaHome, homePropName));
                    }
                } else {
                    throw new BrokenPlatformException (name);
                }
            }
        }
    }

    public static String createName (String platName, String propType) {
        return "platforms." + platName + "." + propType;        //NOI18N
    }

    private static String getCompilerType (ScalaPlatform platform) {
        assert platform != null;
        String prop = platform.getSystemProperties().get("java.specification.version"); //NOI18N
        assert prop != null;
        SpecificationVersion specificationVersion = new SpecificationVersion (prop);
        SpecificationVersion jdk13 = new SpecificationVersion("1.3");   //NOI18N
        int c = specificationVersion.compareTo (jdk13);
        if (c<0) {
            return CLASSIC;
        }
        else if (c == 0) {
            return JAVAC13;
        }
        else {
            return MODERN;
        }
    }

    private static boolean isDefaultLocation (FileObject tool, Collection<FileObject> installFolders) {
        assert tool != null && installFolders != null;
        if (installFolders.size()!=1)
            return false;
        FileObject root = installFolders.iterator().next();
        String relativePath = FileUtil.getRelativePath(root,tool);
        if (relativePath == null) {
            return false;
        }
        StringTokenizer tk = new StringTokenizer(relativePath, "/");
        return (tk.countTokens()== 2 && "bin".equals(tk.nextToken()));
    }


    private static File getToolPath (FileObject tool) throws IOException {
        assert tool != null;
        return new File (URI.create(tool.getURL().toExternalForm()));
    }

    private static String normalizePath (File path,  File scalaHome, String propName) {
        String jdkLoc = scalaHome.getAbsolutePath();
        if (!jdkLoc.endsWith(File.separator)) {
            jdkLoc = jdkLoc + File.separator;
        }
        String loc = path.getAbsolutePath();
        if (loc.startsWith(jdkLoc)) {
            return "${"+propName+"}"+File.separator+loc.substring(jdkLoc.length());           //NOI18N
        }
        else {
            return loc;
        }
    }
    
    public static class BrokenPlatformException extends IOException {
        
        private final String toolName;
        
        public BrokenPlatformException (final String toolName) {
            super ("Cannot locate " + toolName + " command");   //NOI18N
            this.toolName = toolName;
        }
        
        public String getMissingTool () {
            return this.toolName;
        }
        
    }

    static class W implements FileSystem.AtomicAction {
        ScalaPlatform instance;
        MultiDataObject holder;
        String name;
        DataFolder f;
        boolean defaultPlatform;

        W(ScalaPlatform instance, MultiDataObject holder, boolean defaultPlatform) {
            this.instance = instance;
            this.holder = holder;
            this.defaultPlatform = defaultPlatform;
        }
        
        W(ScalaPlatform instance, DataFolder f, String n) {
            this.instance = instance;
            this.name = n;
            this.f = f;
            this.defaultPlatform = false;
        }
        
        public void run() throws java.io.IOException {
            FileLock lck;
            FileObject data;
            
            
            ByteArrayOutputStream buffer = new ByteArrayOutputStream ();            
            try {
                write (buffer);
            } finally {
                buffer.close();                        
            }
            if (holder != null) {
                data = holder.getPrimaryEntry().getFile();
                lck = holder.getPrimaryEntry().takeLock();
            } else {
                FileObject folder = f.getPrimaryFile();
                String fn = FileUtil.findFreeFileName(folder, name, "xml");
                data = folder.createData(fn, "xml");
                lck = data.lock();
            }
            try {
                OutputStream out = data.getOutputStream(lck);
                try {
                    out.write(buffer.toByteArray());
                    out.flush();
                } finally {
                    out.close();
                }
            } finally {
                lck.releaseLock();
            }
            if (holder == null) {
                holder = (MultiDataObject)DataObject.find(data);
            }
        }
        
        void write(final  OutputStream out) throws IOException {
            final Map<String,String> props = instance.getProperties();
            final Map<String,String> sysProps = instance.getSystemProperties();
            final Document doc = XMLUtil.createDocument(ELEMENT_PLATFORM,null,PLATFORM_DTD_ID,"http://www.netbeans.org/dtds/scala-platformdefinition-1_0.dtd"); //NOI18N
            final Element platformElement = doc.getDocumentElement();
            platformElement.setAttribute(ATTR_PLATFORM_NAME,instance.getDisplayName());
            platformElement.setAttribute(ATTR_PLATFORM_DEFAULT,defaultPlatform ? "yes" : "no"); //NOI18N
            final Element propsElement = doc.createElement(ELEMENT_PROPERTIES);
            writeProperties(props, propsElement, doc);
            platformElement.appendChild(propsElement);
            if (!defaultPlatform) {
                final Element sysPropsElement = doc.createElement(ELEMENT_SYSPROPERTIES);
                writeProperties(sysProps, sysPropsElement, doc);
                platformElement.appendChild(sysPropsElement);
                final Element scalaHomeElement = doc.createElement(ELEMENT_SCALAHOME);
                for (Iterator<FileObject> it = instance.getInstallFolders().iterator(); it.hasNext();) {
                    URL url = it.next ().getURL();
                    final Element resourceElement = doc.createElement(ELEMENT_RESOURCE);
                    resourceElement.appendChild(doc.createTextNode(url.toExternalForm()));
                    scalaHomeElement.appendChild(resourceElement);
                }                
                platformElement.appendChild(scalaHomeElement);                
            }            
            final List<ClassPath.Entry> psl = this.instance.getSourceFolders().entries();
            if (psl.size()>0 && shouldWriteSources ()) {                
                final Element sourcesElement = doc.createElement (ELEMENT_SOURCEPATH);
                for (Iterator<ClassPath.Entry> it = psl.iterator(); it.hasNext();) {
                    URL url = it.next ().getURL();
                    final Element resourceElement = doc.createElement (ELEMENT_RESOURCE);
                    resourceElement.appendChild(doc.createTextNode(url.toExternalForm()));
                    sourcesElement.appendChild(resourceElement);
                }
                platformElement.appendChild(sourcesElement);
            }
            final List<URL> pdl = this.instance.getJavadocFolders();
            if (pdl.size()>0 && shouldWriteJavadoc ()) {
                final Element javadocElement = doc.createElement(ELEMENT_JAVADOC);
                for (URL url : pdl) {                    
                    final Element resourceElement = doc.createElement(ELEMENT_RESOURCE);
                    resourceElement.appendChild(doc.createTextNode(url.toExternalForm()));
                    javadocElement.appendChild(resourceElement);
                }
                platformElement.appendChild(javadocElement);
            }
            XMLUtil.write(doc, out, "UTF8");                                                    //NOI18N
        }
        
        void writeProperties(final Map<String,String> props, final Element element, final Document doc) throws IOException {
            final Collection<String> sortedProps = new TreeSet<String>(props.keySet());
            for (Iterator<String> it = sortedProps.iterator(); it.hasNext(); ) {
                final String n = it.next();
                final String val = props.get(n);                
                try {
                    XMLUtil.toAttributeValue(n);
                    XMLUtil.toAttributeValue(val);
                    final Element propElement = doc.createElement(ELEMENT_PROPERTY);
                    propElement.setAttribute(ATTR_PROPERTY_NAME,n);
                    propElement.setAttribute(ATTR_PROPERTY_VALUE,val);
                    element.appendChild(propElement);
                } catch (CharConversionException e) {
                    Logger.getLogger("global").log(Level.WARNING,"Cannot store property: " + n + " value: " + val);   //NOI18N
                }
            }
        }
        
        private boolean shouldWriteSources () {
            if (defaultPlatform) {
                assert this.instance instanceof DefaultPlatformImpl;
                DefaultPlatformImpl dp = (DefaultPlatformImpl) this.instance;
                List<ClassPath.Entry> sfEntries = dp.getSourceFolders().entries();
                List<URL> defaultSf = DefaultPlatformImpl.getSources (FileUtil.normalizeFile(new File((String)dp.getSystemProperties().get("jdk.home"))));   //NOI18N
                if (defaultSf == null || sfEntries.size() != defaultSf.size()) {
                    return true;
                }
                Iterator<ClassPath.Entry> sfit = sfEntries.iterator();
                Iterator<URL> defif = defaultSf.iterator();
                while (sfit.hasNext()) {
                    ClassPath.Entry entry = sfit.next ();                    
                    URL url = defif.next();
                    if (!url.equals(entry.getURL())) {
                        return true;
                    }
                }
                return false;
            }
            return true;
        }
        
        private boolean shouldWriteJavadoc () {
            if (defaultPlatform) {
                assert this.instance instanceof DefaultPlatformImpl;
                DefaultPlatformImpl dp = (DefaultPlatformImpl) this.instance;
                List jdf = dp.getJavadocFolders();
                List defaultJdf = DefaultPlatformImpl.getJavadoc (FileUtil.normalizeFile(new File((String)dp.getSystemProperties().get("jdk.home"))));  //NOI18N
                return defaultJdf == null || !jdf.equals (defaultJdf);
            }
            return true;
        }
    }
    
    static final String ELEMENT_PROPERTIES = "properties"; // NOI18N
    static final String ELEMENT_SYSPROPERTIES = "sysproperties"; // NOI18N
    static final String ELEMENT_PROPERTY = "property"; // NOI18N
    static final String ELEMENT_PLATFORM = "platform"; // NOI18N
    static final String ELEMENT_SCALAHOME = "scalahome";    //NOI18N
    static final String ELEMENT_SOURCEPATH = "sources";  //NOI18N
    static final String ELEMENT_JAVADOC = "javadoc";    //NOI18N
    static final String ELEMENT_RESOURCE = "resource";  //NOI18N
    static final String ATTR_PLATFORM_NAME = "name"; // NOI18N
    static final String ATTR_PLATFORM_DEFAULT = "default"; // NOI18N
    static final String ATTR_PROPERTY_NAME = "name"; // NOI18N
    static final String ATTR_PROPERTY_VALUE = "value"; // NOI18N
    
    static class H extends org.xml.sax.helpers.DefaultHandler implements EntityResolver {
        Map<String,String> properties;
        Map<String,String> sysProperties;
        List<URL> sources;
        List<URL> javadoc;
        List<URL> installFolders;
        String  name;
        boolean isDefault;

        private Map<String,String> propertyMap;
        private StringBuffer buffer;
        private List<URL> path;


        public void startDocument () throws org.xml.sax.SAXException {
        }
        
        public void endDocument () throws org.xml.sax.SAXException {
        }
        
        public void startElement (String uri, String localName, String qName, org.xml.sax.Attributes attrs)
        throws org.xml.sax.SAXException {
            if (ELEMENT_PLATFORM.equals(qName)) {
                name = attrs.getValue(ATTR_PLATFORM_NAME);
                isDefault = "yes".equals(attrs.getValue(ATTR_PLATFORM_DEFAULT));
            } else if (ELEMENT_PROPERTIES.equals(qName)) {
                if (properties == null)
                    properties = new HashMap<String,String>(17);
                propertyMap = properties;
            } else if (ELEMENT_SYSPROPERTIES.equals(qName)) {
                if (sysProperties == null)
                    sysProperties = new HashMap<String,String>(17);
                propertyMap = sysProperties;
            } else if (ELEMENT_PROPERTY.equals(qName)) {
                if (propertyMap == null)
                    throw new SAXException("property w/o properties or sysproperties");
                String name = attrs.getValue(ATTR_PROPERTY_NAME);
                if (name == null || "".equals(name))
                    throw new SAXException("missing name");
                String val = attrs.getValue(ATTR_PROPERTY_VALUE);
                propertyMap.put(name, val);
            }
            else if (ELEMENT_SOURCEPATH.equals(qName)) {
                this.sources = new ArrayList<URL> ();
                this.path = this.sources;
            }
            else if (ELEMENT_JAVADOC.equals(qName)) {
                this.javadoc = new ArrayList<URL> ();
                this.path = this.javadoc;
            }
            else if (ELEMENT_SCALAHOME.equals(qName)) {
                this.installFolders = new ArrayList<URL> ();
                this.path =  this.installFolders;
            }
            else if (ELEMENT_RESOURCE.equals(qName)) {
                this.buffer = new StringBuffer ();
            }
        }
        
        public void endElement (String uri, String localName, String qName) throws org.xml.sax.SAXException {
            if (ELEMENT_PROPERTIES.equals(qName) ||
                ELEMENT_SYSPROPERTIES.equals(qName)) {
                propertyMap = null;
            }
            else if (ELEMENT_SOURCEPATH.equals(qName) || ELEMENT_JAVADOC.equals(qName)) {
                path = null;
            }
            else if (ELEMENT_RESOURCE.equals(qName)) {
                try {
                    this.path.add (new URL(this.buffer.toString()));                    
                } catch (MalformedURLException mue) {
                    ErrorManager.getDefault().notify(mue); 
                }
                this.buffer = null;
            }
        }

        public void characters(char chars[], int start, int length) throws SAXException {
            if (this.buffer != null) {
                this.buffer.append(chars, start, length);
            }
        }
        
        public org.xml.sax.InputSource resolveEntity(String publicId, String systemId)
        throws SAXException {
            if (PLATFORM_DTD_ID.equals (publicId)) {
                return new org.xml.sax.InputSource (new ByteArrayInputStream (new byte[0]));
            } else {
                return null; // i.e. follow advice of systemID
            }
        }
        
    }

}
