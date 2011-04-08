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

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.*;
import java.net.URL;
import java.net.MalformedURLException;
import java.io.File;

import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.scala.platform.ScalaPlatform;
import org.netbeans.api.scala.platform.ScalaPlatformManager;
import org.netbeans.api.scala.platform.Specification;
import org.netbeans.spi.java.classpath.PathResourceImplementation;
import org.netbeans.spi.java.classpath.support.ClassPathSupport;
import org.netbeans.spi.project.support.ant.PropertyUtils;
import org.openide.filesystems.FileUtil;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.URLMapper;
import org.openide.ErrorManager;

/**
 * Implementation of the ScalaPlatform API class, which serves proper
 * bootstrap classpath information.
 */
public class J2SEPlatformImpl extends ScalaPlatform {
    
    public static final String PROP_ANT_NAME = "antName";                   //NOI18N
    public static final String PLATFORM_J2SE = "std";                      //NOI18N

    protected static final String PLAT_PROP_ANT_NAME = ScalaPlatformManager.SCALA_PLATFORM_PROP_ID;             //NOI18N
    protected static final String PLAT_PROP_ARCH_FOLDER="scala.platform.arch.folder";       //NOI18N
    protected static final String SYSPROP_BOOT_CLASSPATH = "java.boot.class.path";     // NOI18N
    protected static final String SYSPROP_SCALA_CLASS_PATH = "scala.class.path";        // NOI18N
    protected static final String SYSPROP_SCALA_SOURCE_PATH = "scala.source.path";        // NOI18N
    protected static final String SYSPROP_JAVA_EXT_PATH = "java.ext.dirs";            //NOI18N
    protected static final String SYSPROP_USER_DIR = "user.dir";                      //NOI18N

    /**
     * Holds the display name of the platform
     */
    private String displayName;
    /**
     * Holds the properties of the platform
     */
    private Map<String,String> properties;

    /**
     * List&lt;URL&gt;
     */
    private ClassPath sources;

    /**
     * List&lt;URL&gt;
     */
    private List<URL> javadoc;

    /**
     * List&lt;URL&gt;
     */
    private List<URL> installFolders;

    /**
     * Holds bootstrap libraries for the platform
     */
    Reference<ClassPath> bootstrap = new WeakReference<ClassPath>(null);
    /**
     * Holds standard libraries of the platform
     */
    Reference<ClassPath> standardLibs = new WeakReference<ClassPath>(null);

    /**
     * Holds the specification of the platform
     */
    private Specification spec;

    J2SEPlatformImpl (String dispName, List<URL> installFolders, Map<String,String> initialProperties, Map<String,String> sysProperties, List<URL> sources, List<URL> javadoc) {
        super();
        this.displayName = dispName;
        if (installFolders != null) {
            this.installFolders = installFolders;       //No copy needed, called from this module => safe
        }
        else {
            //Old version, repair
            String home = initialProperties.remove ("platform.home");        //NOI18N
            if (home != null) {
                this.installFolders = new ArrayList<URL> ();
                StringTokenizer tk = new StringTokenizer (home, File.pathSeparator);
                while (tk.hasMoreTokens()) {
                    File f = new File (tk.nextToken());
                    try {
                        this.installFolders.add (f.toURI().toURL());
                    } catch (MalformedURLException mue) {
                        ErrorManager.getDefault().notify (mue);
                    }
                }
            }
            else {
                throw new IllegalArgumentException ("Invalid platform, platform must have install folder.");    //NOI18N
            }
        }
        this.properties = initialProperties;
        this.sources = createClassPath(sources);
        if (javadoc != null) {
            this.javadoc = Collections.unmodifiableList(javadoc);   //No copy needed, called from this module => safe
        }
        else {
            this.javadoc = Collections.<URL>emptyList();
        }
        setSystemProperties(filterProbe(sysProperties));
    }

    protected J2SEPlatformImpl (String dispName, String antName, List<URL> installFolders, Map<String,String> initialProperties,
        Map<String,String> sysProperties, List<URL> sources, List<URL> javadoc) {
        this (dispName,  installFolders, initialProperties, sysProperties,sources, javadoc);
        this.properties.put (PLAT_PROP_ANT_NAME,antName);
    }

    /**
     * @return  a descriptive, human-readable name of the platform
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Alters the human-readable name of the platform
     * @param name the new display name
     */
    public void setDisplayName(String name) {
        this.displayName = name;
        firePropertyChange(PROP_DISPLAY_NAME, null, null); // NOI18N
    }
    
    /**
     * Alters the human-readable name of the platform without firing
     * events. This method is an internal contract to allow lazy creation
     * of display name
     * @param name the new display name
     */
    final protected void internalSetDisplayName (String name) {
        this.displayName = name;
    }


    public String getAntName () {
        return (String) this.properties.get (PLAT_PROP_ANT_NAME);
    }

    public void setAntName (String antName) {
        if (antName == null || antName.length()==0) {
            throw new IllegalArgumentException ();
        }
        this.properties.put(PLAT_PROP_ANT_NAME, antName);
        this.firePropertyChange (PROP_ANT_NAME,null,null);
    }
    
    public void setArchFolder (final String folder) {
        if (folder == null || folder.length() == 0) {
            throw new IllegalArgumentException ();
        }
        this.properties.put (PLAT_PROP_ARCH_FOLDER, folder);
    }


    public ClassPath getBootstrapLibraries() {
        synchronized (this) {
            ClassPath cp = (bootstrap == null ? null : bootstrap.get());
            if (cp != null)
                return cp;
            String pathSpec = getSystemProperties().get(SYSPROP_BOOT_CLASSPATH);
            String extPathSpec = Util.getExtensions((String)getSystemProperties().get(SYSPROP_JAVA_EXT_PATH));
            if (extPathSpec != null) {
                pathSpec = pathSpec + File.pathSeparator + extPathSpec;
            }
            cp = Util.createClassPath (pathSpec);
            bootstrap = new WeakReference<ClassPath>(cp);
            return cp;
        }
    }

    /**
     * This implementation simply reads and parses `java.class.path' property and creates a ClassPath
     * out of it.
     * @return  ClassPath that represents contents of system property java.class.path.
     */
    public ClassPath getStandardLibraries() {
        synchronized (this) {
            ClassPath cp = (standardLibs == null ? null : standardLibs.get());
            if (cp != null)
                return cp;
            String pathSpec = getSystemProperties().get(SYSPROP_SCALA_CLASS_PATH);
            cp = Util.createClassPath (pathSpec);
            standardLibs = new WeakReference<ClassPath>(cp);
            return cp;
        }
    }
    
    /**
     * Retrieves a collection of {@link org.openide.filesystems.FileObject}s of one or more folders
     * where the Platform is installed. Typically it returns one folder, but
     * in some cases there can be more of them.
     */
    public final Collection<FileObject> getInstallFolders() {
        Collection<FileObject> result = new ArrayList<FileObject> ();
        for (Iterator<URL> it = this.installFolders.iterator(); it.hasNext();) {
            URL url = it.next ();
            FileObject root = URLMapper.findFileObject(url);
            if (root != null) {
                result.add (root); 
            }
        }
        return result;
    }


    public final FileObject findTool(final String toolName) {
        String archFolder = getProperties().get(PLAT_PROP_ARCH_FOLDER);        
        FileObject tool = null;
        if (archFolder != null) {
            tool = Util.findTool (toolName, this.getInstallFolders(), archFolder);            
        }
        if (tool == null) {
            tool = Util.findTool (toolName, this.getInstallFolders());
        }
        return tool;
    }


    /**
     * Returns the location of the source of platform
     * @return List&lt;URL&gt;
     */
    public final ClassPath getSourceFolders () {
        return this.sources;
    }

    public final void setSourceFolders (ClassPath c) {
        assert c != null;
        this.sources = c;
        this.firePropertyChange(PROP_SOURCE_FOLDER, null, null);
    }

        /**
     * Returns the location of the Javadoc for this platform
     * @return FileObject
     */
    public final List<URL> getJavadocFolders () {
        return this.javadoc;
    }

    public final void setJavadocFolders (List<URL> c) {
        assert c != null;
        List<URL> safeCopy = Collections.unmodifiableList (new ArrayList<URL> (c));
        for (Iterator<URL> it = safeCopy.iterator(); it.hasNext();) {
            URL url = it.next ();
            if (!"jar".equals (url.getProtocol()) && FileUtil.isArchiveFile(url)) {
                throw new IllegalArgumentException ("JavadocFolder must be a folder.");
            }
        }
        this.javadoc = safeCopy;
        this.firePropertyChange(PROP_JAVADOC_FOLDER, null, null);
    }

    public String getVendor() {
        String s = getSystemProperties().get("java.vm.vendor"); // NOI18N
        return s == null ? "" : s; // NOI18N
    }

    public Specification getSpecification() {
        if (spec == null) {
            spec = new Specification (PLATFORM_J2SE, Util.getSpecificationVersion(this)); //NOI18N
        }
        return spec;
    }

    public Map<String,String> getProperties() {
        return Collections.unmodifiableMap (this.properties);
    }
    
    Collection getInstallFolderURLs () {
        return Collections.unmodifiableList(this.installFolders);
    }
    
    protected static String filterProbe (String v, final String probePath) {
        if (v != null) {
            final String[] pes = PropertyUtils.tokenizePath(v);
            final StringBuilder sb = new StringBuilder ();
            for (String pe : pes) {
                if (probePath != null ?  probePath.equals(pe) : (pe != null &&
                pe.endsWith("org-netbeans-modules-scala-stdplatform-probe.jar"))) { //NOI18N
                    //Skeep
                }
                else {
                    if (sb.length() > 0) {
                        sb.append(File.pathSeparatorChar);
                    }
                    sb.append(pe);
                }
            }
            v = sb.toString();
        }
        return v;
    }
    
    private static Map<String,String> filterProbe (final Map<String,String> p) {
        if (p!=null) {
            final String val = p.get(SYSPROP_SCALA_CLASS_PATH);
            if (val != null) {
                p.put(SYSPROP_SCALA_CLASS_PATH, filterProbe(val, null));
            }
        }
        return p;
    }


    private static ClassPath createClassPath (final List<? extends URL> urls) {
        List<PathResourceImplementation> resources = new ArrayList<PathResourceImplementation> ();
        if (urls != null) {
            for (URL url : urls) {
                resources.add (ClassPathSupport.createResource (url));
            }
        }
        return ClassPathSupport.createClassPath (resources);
    }    
}
