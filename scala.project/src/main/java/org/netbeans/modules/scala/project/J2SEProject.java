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
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2007 Sun
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
package org.netbeans.modules.scala.project;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.Icon;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.classpath.GlobalPathRegistry;
import org.netbeans.api.java.project.JavaProjectConstants;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectInformation;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.api.project.ant.AntArtifact;
import org.netbeans.api.project.ant.AntBuildExtender;
import org.netbeans.modules.java.api.common.SourceRoots;
import org.netbeans.modules.java.api.common.ant.UpdateHelper;
import org.netbeans.modules.java.api.common.ant.UpdateImplementation;
import org.netbeans.modules.java.api.common.classpath.ClassPathModifier;
import org.netbeans.modules.java.api.common.project.ProjectProperties;
import org.netbeans.modules.java.api.common.queries.QuerySupport;
import org.netbeans.modules.scala.project.api.J2SEPropertyEvaluator;
import org.netbeans.modules.scala.project.classpath.ClassPathProviderImpl;
import org.netbeans.modules.scala.project.queries.BinaryForSourceQueryImpl;
import org.netbeans.modules.scala.project.ui.J2SELogicalViewProvider;
import org.netbeans.modules.scala.project.ui.customizer.CustomizerProviderImpl;
import org.netbeans.modules.scala.project.ui.customizer.J2SEProjectProperties;
import org.netbeans.spi.java.project.support.ExtraSourceJavadocSupport;
import org.netbeans.spi.java.project.support.LookupMergerSupport;
import org.netbeans.spi.java.project.support.ui.BrokenReferencesSupport;
import org.netbeans.spi.project.ActionProvider;
import org.netbeans.spi.project.AuxiliaryConfiguration;
import org.netbeans.spi.project.SubprojectProvider;
import org.netbeans.spi.project.ant.AntArtifactProvider;
import org.netbeans.spi.project.ant.AntBuildExtenderFactory;
import org.netbeans.spi.project.support.LookupProviderSupport;
import org.netbeans.spi.project.ant.AntBuildExtenderImplementation;
import org.netbeans.spi.project.support.ant.AntProjectEvent;
import org.netbeans.spi.project.support.ant.AntProjectHelper;
import org.netbeans.spi.project.support.ant.AntProjectListener;
import org.netbeans.spi.project.support.ant.EditableProperties;
import org.netbeans.spi.project.support.ant.FilterPropertyProvider;
import org.netbeans.spi.project.support.ant.GeneratedFilesHelper;
import org.netbeans.spi.project.support.ant.ProjectXmlSavedHook;
import org.netbeans.spi.project.support.ant.PropertyEvaluator;
import org.netbeans.spi.project.support.ant.PropertyProvider;
import org.netbeans.spi.project.support.ant.PropertyUtils;
import org.netbeans.spi.project.support.ant.ReferenceHelper;
import org.netbeans.spi.project.ui.PrivilegedTemplates;
import org.netbeans.spi.project.ui.ProjectOpenedHook;
import org.netbeans.spi.project.ui.RecommendedTemplates;
import org.netbeans.spi.project.ui.support.UILookupMergerSupport;
import org.netbeans.spi.queries.FileEncodingQueryImplementation;
import org.openide.DialogDisplayer;
import org.openide.ErrorManager;
import org.openide.NotifyDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileSystem;
import org.openide.filesystems.FileUtil;
import org.openide.modules.InstalledFileLocator;
import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.Mutex;
import org.openide.util.NbBundle;
import org.openide.util.lookup.Lookups;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Represents one plain J2SE project.
 * @author Jesse Glick, et al.
 */
public final class J2SEProject implements Project, AntProjectListener {

    private static final Icon J2SE_PROJECT_ICON = ImageUtilities.loadImageIcon("org/netbeans/modules/scala/project/ui/resources/scalaProject.png", true); // NOI18N

    private static final Logger LOG = Logger.getLogger(J2SEProject.class.getName());
    private final AuxiliaryConfiguration aux;
    private final AntProjectHelper helper;
    private final PropertyEvaluator eval;
    private final ReferenceHelper refHelper;
    private final GeneratedFilesHelper genFilesHelper;
    private final Lookup lookup;
    private final UpdateHelper updateHelper;
    private MainClassUpdater mainClassUpdater;
    private SourceRoots sourceRoots;
    private SourceRoots testRoots;
    private final ClassPathProviderImpl cpProvider;
    private final ClassPathModifier cpMod;
    private AntBuildExtender buildExtender;

    J2SEProject(AntProjectHelper helper) throws IOException {
        this.helper = helper;
        eval = createEvaluator();
        aux = helper.createAuxiliaryConfiguration();
        for (int v = 4; v < 10; v++) {
            if (aux.getConfigurationFragment("data", "http://www.netbeans.org/ns/scala-project/" + v, true) != null) { // NOI18N
                throw Exceptions.attachLocalizedMessage(new IOException("too new"), // NOI18N
                        NbBundle.getMessage(J2SEProject.class, "ScalaProject.too_new", FileUtil.getFileDisplayName(helper.getProjectDirectory())));
            }
        }
        refHelper = new ReferenceHelper(helper, aux, eval);
        buildExtender = AntBuildExtenderFactory.createAntExtender(new J2SEExtenderImplementation(), refHelper);
        /// TODO replace this GeneratedFilesHelper with the default one when fixing #101710
        genFilesHelper = new GeneratedFilesHelper(helper, buildExtender);
        UpdateImplementation updateProject = new UpdateProjectImpl(this, helper, aux);
        this.updateHelper = new UpdateHelper(updateProject, helper);

        this.cpProvider = new ClassPathProviderImpl(this.helper, evaluator(), getSourceRoots(), getTestSourceRoots()); //Does not use APH to get/put properties/cfgdata
        this.cpMod = new ClassPathModifier(this, this.updateHelper, eval, refHelper, null, createClassPathModifierCallback(), null);
        final J2SEActionProvider actionProvider = new J2SEActionProvider(this, this.updateHelper);
        lookup = createLookup(aux, actionProvider);
        actionProvider.startFSListener();
        helper.addAntProjectListener(this);
    }

    private ClassPathModifier.Callback createClassPathModifierCallback() {
        return new ClassPathModifier.Callback() {
            public String getClassPathProperty(SourceGroup sg, String type) {
                assert sg != null : "SourceGroup cannot be null";  //NOI18N
                assert type != null : "Type cannot be null";  //NOI18N
                final String[] classPathProperty = getClassPathProvider().getPropertyName (sg, type);
                if (classPathProperty == null || classPathProperty.length == 0) {
                    throw new UnsupportedOperationException ("Modification of [" + sg.getRootFolder().getPath() +", " + type + "] is not supported"); //NOI18N
                }
                return classPathProperty[0];
            }

            public String getElementName(String classpathProperty) {
                return null;
            }
        };
    }

    /**
     * Returns the project directory
     * @return the directory the project is located in
     */
    public FileObject getProjectDirectory() {
        return helper.getProjectDirectory();
    }

    @Override
    public String toString() {
        return "ScalaProject[" + FileUtil.getFileDisplayName(getProjectDirectory()) + "]"; // NOI18N
    }

    private PropertyEvaluator createEvaluator() {
        // It is currently safe to not use the UpdateHelper for PropertyEvaluator; UH.getProperties() delegates to APH
        // Adapted from APH.getStandardPropertyEvaluator (delegates to ProjectProperties):
        PropertyEvaluator baseEval1 = PropertyUtils.sequentialPropertyEvaluator(
                helper.getStockPropertyPreprovider(),
                helper.getPropertyProvider(J2SEConfigurationProvider.CONFIG_PROPS_PATH));
        PropertyEvaluator baseEval2 = PropertyUtils.sequentialPropertyEvaluator(
                helper.getStockPropertyPreprovider(),
                helper.getPropertyProvider(AntProjectHelper.PRIVATE_PROPERTIES_PATH));
        return PropertyUtils.sequentialPropertyEvaluator(
                helper.getStockPropertyPreprovider(),
                helper.getPropertyProvider(J2SEConfigurationProvider.CONFIG_PROPS_PATH),
                new ConfigPropertyProvider(baseEval1, "nbproject/private/configs", helper), // NOI18N
                helper.getPropertyProvider(AntProjectHelper.PRIVATE_PROPERTIES_PATH),
                helper.getProjectLibrariesPropertyProvider(),
                PropertyUtils.userPropertiesProvider(baseEval2,
                "user.properties.file", FileUtil.toFile(getProjectDirectory())), // NOI18N
                new ConfigPropertyProvider(baseEval1, "nbproject/configs", helper), // NOI18N
                helper.getPropertyProvider(AntProjectHelper.PROJECT_PROPERTIES_PATH));
    }

    private static final class ConfigPropertyProvider extends FilterPropertyProvider implements PropertyChangeListener {

        private final PropertyEvaluator baseEval;
        private final String prefix;
        private final AntProjectHelper helper;

        public ConfigPropertyProvider(PropertyEvaluator baseEval, String prefix, AntProjectHelper helper) {
            super(computeDelegate(baseEval, prefix, helper));
            this.baseEval = baseEval;
            this.prefix = prefix;
            this.helper = helper;
            baseEval.addPropertyChangeListener(this);
        }

        public void propertyChange(PropertyChangeEvent ev) {
            if (J2SEConfigurationProvider.PROP_CONFIG.equals(ev.getPropertyName())) {
                setDelegate(computeDelegate(baseEval, prefix, helper));
            }
        }

        private static PropertyProvider computeDelegate(PropertyEvaluator baseEval, String prefix, AntProjectHelper helper) {
            String config = baseEval.getProperty(J2SEConfigurationProvider.PROP_CONFIG);
            if (config != null) {
                return helper.getPropertyProvider(prefix + "/" + config + ".properties"); // NOI18N
            } else {
                return PropertyUtils.fixedPropertyProvider(Collections.<String, String>emptyMap());
            }
        }
    }

    public PropertyEvaluator evaluator() {
        return eval;
    }

    public ReferenceHelper getReferenceHelper() {
        return this.refHelper;
    }

    public UpdateHelper getUpdateHelper() {
        return this.updateHelper;
    }

    public Lookup getLookup() {
        return lookup;
    }

    public AntProjectHelper getAntProjectHelper() {
        return helper;
    }

    private Lookup createLookup(final AuxiliaryConfiguration aux,
            final ActionProvider actionProvider) {
        final SubprojectProvider spp = refHelper.createSubprojectProvider();
        FileEncodingQueryImplementation encodingQuery = QuerySupport.createFileEncodingQuery(evaluator(), J2SEProjectProperties.SOURCE_ENCODING);
        @SuppressWarnings("deprecation") Object cpe = new org.netbeans.modules.java.api.common.classpath.ClassPathExtender(
            cpMod, ProjectProperties.JAVAC_CLASSPATH, null);
        final Lookup base = Lookups.fixed(new Object[]{
                    J2SEProject.this,
                    new Info(),
                    aux,
                    helper.createAuxiliaryProperties(),
                    helper.createCacheDirectoryProvider(),
                    spp,
                    actionProvider,
                    new J2SELogicalViewProvider(this, this.updateHelper, evaluator(), spp, refHelper),
                    // new J2SECustomizerProvider(this, this.updateHelper, evaluator(), refHelper),
                    new CustomizerProviderImpl(this, this.updateHelper, evaluator(), refHelper, this.genFilesHelper),
                    LookupMergerSupport.createClassPathProviderMerger(cpProvider),
                    QuerySupport.createCompiledSourceForBinaryQuery(helper, evaluator(), getSourceRoots(), getTestSourceRoots()),
                    QuerySupport.createJavadocForBinaryQuery(helper, evaluator()),
                    new AntArtifactProviderImpl(),
                    new ProjectXmlSavedHookImpl(),
                    UILookupMergerSupport.createProjectOpenHookMerger(new ProjectOpenedHookImpl()),
                    QuerySupport.createUnitTestForSourceQuery(getSourceRoots(), getTestSourceRoots()),
                    QuerySupport.createSourceLevelQuery(evaluator()),
                    new J2SESources(this, helper, evaluator(), getSourceRoots(), getTestSourceRoots()),
                    QuerySupport.createSharabilityQuery(helper, evaluator(), getSourceRoots(), getTestSourceRoots()),
                    QuerySupport.createFileBuiltQuery(helper, evaluator(), getSourceRoots(), getTestSourceRoots()),
                    new RecommendedTemplatesImpl(this.updateHelper),
                    cpe,
                    buildExtender,
                    cpMod,
                    this, // never cast an externally obtained Project to J2SEProject - use lookup instead
                    new J2SEProjectOperations(this),
                    new J2SEConfigurationProvider(this),
                    //new J2SEPersistenceProvider(this, cpProvider),
                    UILookupMergerSupport.createPrivilegedTemplatesMerger(),
                    UILookupMergerSupport.createRecommendedTemplatesMerger(),
                    LookupProviderSupport.createSourcesMerger(),
                    encodingQuery,
                    new J2SEPropertyEvaluatorImpl(evaluator()),
                    new J2SETemplateAttributesProvider(this.helper),
                    ExtraSourceJavadocSupport.createExtraSourceQueryImplementation(this, helper, eval),
                    LookupMergerSupport.createSFBLookupMerger(),
                    ExtraSourceJavadocSupport.createExtraJavadocQueryImplementation(this, helper, eval),
                    LookupMergerSupport.createJFBLookupMerger(),
                    new BinaryForSourceQueryImpl(this.sourceRoots, this.testRoots, this.helper, this.eval) //Does not use APH to get/put properties/cfgdata

                });
        return LookupProviderSupport.createCompositeLookup(base, "Projects/org-netbeans-modules-scala-project/Lookup"); //NOI18N
    }

    public ClassPathProviderImpl getClassPathProvider() {
        return this.cpProvider;
    }

    public ClassPathModifier getProjectClassPathModifier() {
        return this.cpMod;
    }

    public void configurationXmlChanged(AntProjectEvent ev) {
        if (ev.getPath().equals(AntProjectHelper.PROJECT_XML_PATH)) {
            // Could be various kinds of changes, but name & displayName might have changed.
            Info info = (Info) getLookup().lookup(ProjectInformation.class);
            info.firePropertyChange(ProjectInformation.PROP_NAME);
            info.firePropertyChange(ProjectInformation.PROP_DISPLAY_NAME);
        }
    }

    public void propertiesChanged(AntProjectEvent ev) {
        // currently ignored (probably better to listen to evaluator() if you need to)
    }

    // Package private methods -------------------------------------------------
    /**
     * Returns the source roots of this project
     * @return project's source roots
     */
    public synchronized SourceRoots getSourceRoots() {
        if (this.sourceRoots == null) { //Local caching, no project metadata access
            this.sourceRoots = SourceRoots.create(updateHelper, evaluator(), getReferenceHelper(),
                    J2SEProjectType.PROJECT_CONFIGURATION_NAMESPACE, "source-roots", false, "src.{0}{1}.dir"); //NOI18N
        }
        return this.sourceRoots;
    }

    public synchronized SourceRoots getTestSourceRoots() {
        if (this.testRoots == null) { //Local caching, no project metadata access
            this.testRoots = SourceRoots.create(updateHelper, evaluator(), getReferenceHelper(),
                    J2SEProjectType.PROJECT_CONFIGURATION_NAMESPACE, "test-roots", true, "test.{0}{1}.dir"); //NOI18N
        }
        return this.testRoots;
    }

    File getTestClassesDirectory() {
        String testClassesDir = evaluator().getProperty(ProjectProperties.BUILD_TEST_CLASSES_DIR);
        if (testClassesDir == null) {
            return null;
        }
        return helper.resolveFile(testClassesDir);
    }

    // Currently unused (but see #47230):
    /** Store configured project name. */
    public void setName(final String name) {
        ProjectManager.mutex().writeAccess(new Mutex.Action<Void>() {

            public Void run() {
                Element data = helper.getPrimaryConfigurationData(true);
                // XXX replace by XMLUtil when that has findElement, findText, etc.
                NodeList nl = data.getElementsByTagNameNS(J2SEProjectType.PROJECT_CONFIGURATION_NAMESPACE, "name");
                Element nameEl;
                if (nl.getLength() == 1) {
                    nameEl = (Element) nl.item(0);
                    NodeList deadKids = nameEl.getChildNodes();
                    while (deadKids.getLength() > 0) {
                        nameEl.removeChild(deadKids.item(0));
                    }
                } else {
                    nameEl = data.getOwnerDocument().createElementNS(J2SEProjectType.PROJECT_CONFIGURATION_NAMESPACE, "name");
                    data.insertBefore(nameEl, /* OK if null */ data.getChildNodes().item(0));
                }
                nameEl.appendChild(data.getOwnerDocument().createTextNode(name));
                helper.putPrimaryConfigurationData(data, true);
                return null;
            }
        });
    }

    // Private innerclasses ----------------------------------------------------
    //when #110886 gets implemented, this class is obsolete
    private final class Info implements ProjectInformation {

        private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
        private WeakReference<String> cachedName = null;

        Info() {
        }

        void firePropertyChange(String prop) {
            pcs.firePropertyChange(prop, null, null);
            synchronized (pcs) {
                cachedName = null;
            }
        }

        public String getName() {
            return PropertyUtils.getUsablePropertyName(getDisplayName());
        }

        public String getDisplayName() {
            synchronized (pcs) {
                if (cachedName != null) {
                    String dn = cachedName.get();
                    if (dn != null) {
                        return dn;
                    }
                }
            }
            String dn = ProjectManager.mutex().readAccess(new Mutex.Action<String>() {

                public String run() {
                    Element data = updateHelper.getPrimaryConfigurationData(true);
                    // XXX replace by XMLUtil when that has findElement, findText, etc.
                    NodeList nl = data.getElementsByTagNameNS(J2SEProjectType.PROJECT_CONFIGURATION_NAMESPACE, "name"); // NOI18N
                    if (nl.getLength() == 1) {
                        nl = nl.item(0).getChildNodes();
                        if (nl.getLength() == 1 && nl.item(0).getNodeType() == Node.TEXT_NODE) {
                            return ((Text) nl.item(0)).getNodeValue();
                        }
                    }
                    return "???"; // NOI18N
                }
            });
            synchronized (pcs) {
                cachedName = new WeakReference<String>(dn);
            }
            return dn;
        }

        public Icon getIcon() {
            return J2SE_PROJECT_ICON;
        }

        public Project getProject() {
            return J2SEProject.this;
        }

        public void addPropertyChangeListener(PropertyChangeListener listener) {
            pcs.addPropertyChangeListener(listener);
        }

        public void removePropertyChangeListener(PropertyChangeListener listener) {
            pcs.removePropertyChangeListener(listener);
        }
    }

    private final class ProjectXmlSavedHookImpl extends ProjectXmlSavedHook {

        ProjectXmlSavedHookImpl() {
        }

        protected void projectXmlSaved() throws IOException {
            //May be called by {@link AuxiliaryConfiguration#putConfigurationFragment}
            //which didn't affect the j2seproject 
            if (updateHelper.isCurrent()) {
                //Refresh build-impl.xml only for j2seproject/2
                genFilesHelper.refreshBuildScript(
                        GeneratedFilesHelper.BUILD_IMPL_XML_PATH,
                        J2SEProject.class.getResource("resources/build-impl.xsl"),
                        false);
                genFilesHelper.refreshBuildScript(
                        GeneratedFilesHelper.BUILD_XML_PATH,
                        J2SEProject.class.getResource("resources/build.xsl"),
                        false);
            }
        }
    }

    private final class ProjectOpenedHookImpl extends ProjectOpenedHook {

        ProjectOpenedHookImpl() {
        }
        private static final String JAX_RPC_NAMESPACE = "http://www.netbeans.org/ns/j2se-project/jax-rpc"; //NOI18N

        private static final String JAX_RPC_CLIENTS = "web-service-clients"; //NOI18N

        private static final String JAX_RPC_CLIENT = "web-service-client"; //NOI18N

        private ClassPath coreLibsCp;

        protected void projectOpened() {
            // Check up on build scripts.
            try {
                if (updateHelper.isCurrent()) {
                    //Refresh build-impl.xml only for j2seproject/2
                    genFilesHelper.refreshBuildScript(
                            GeneratedFilesHelper.BUILD_IMPL_XML_PATH,
                            J2SEProject.class.getResource("resources/build-impl.xsl"),
                            true);
                    genFilesHelper.refreshBuildScript(
                            GeneratedFilesHelper.BUILD_XML_PATH,
                            J2SEProject.class.getResource("resources/build.xsl"),
                            true);
                }
            } catch (IOException e) {
                ErrorManager.getDefault().notify(ErrorManager.INFORMATIONAL, e);
            }

            // register project's classpaths to GlobalPathRegistry            

            /** register java classpaths, so the java's query will update it */
            GlobalPathRegistry.getDefault().register(
                    ClassPath.BOOT, 
                    cpProvider.getProjectClassPaths(ClassPath.BOOT));
            GlobalPathRegistry.getDefault().register(
                    ClassPath.SOURCE, 
                    cpProvider.getProjectClassPaths(ClassPath.SOURCE));
            GlobalPathRegistry.getDefault().register(
                    ClassPath.COMPILE, 
                    cpProvider.getProjectClassPaths(ClassPath.COMPILE));
            /**
             * @Note:
             * Register classpaths to GlobalPathRegistry will cause GSF indexer to monitor and indexing them.
             * 
             * Per org.netbeans.modules.gsfret.source.GlobalSourcePath#createResources(Request),
             * Tor' midifications: Treat bootCps as a source path, not a binary -  I want to scan directories.
             * 
             * We should here register boot source's classpath instead of binary boot classpath.
             * 
             * GlobalPathRegistry.getDefault().register(ClassPath.BOOT, cpProvider.getProjectClassPaths(ClassPath.BOOT));
             */            
//            FileObject scalaStubsFo = ScalaLanguage.getScalaStubFo();
//            if (scalaStubsFo != null) {
//                coreLibsCp = ClassPathSupport.createClassPath(new FileObject[]{scalaStubsFo});
//                GlobalPathRegistry.getDefault().register(ClassPath.BOOT, new ClassPath[]{coreLibsCp});
//            }
//
//            GlobalPathRegistry.getDefault().register(ClassPath.BOOT, cpProvider.getProjectSourcesClassPaths(ClassPath.BOOT));
//            GlobalPathRegistry.getDefault().register(ClassPath.COMPILE, cpProvider.getProjectSourcesClassPaths(ClassPath.COMPILE));
//            GlobalPathRegistry.getDefault().register(ClassPath.SOURCE, cpProvider.getProjectSourcesClassPaths(ClassPath.SOURCE));

            //register updater of main.class
            //the updater is active only on the opened projects
            mainClassUpdater = new MainClassUpdater(J2SEProject.this, eval, updateHelper,
                    cpProvider.getProjectClassPaths(ClassPath.SOURCE)[0], J2SEProjectProperties.MAIN_CLASS);

            // Make it easier to run headless builds on the same machine at least.
            try {
                getProjectDirectory().getFileSystem().runAtomicAction(new FileSystem.AtomicAction() {

                    public void run() throws IOException {
                        ProjectManager.mutex().writeAccess(new Mutex.Action<Void>() {

                            public Void run() {
                                EditableProperties ep = updateHelper.getProperties(AntProjectHelper.PRIVATE_PROPERTIES_PATH);
                                File buildProperties = new File(System.getProperty("netbeans.user"), "build.properties"); // NOI18N
                                ep.setProperty("user.properties.file", buildProperties.getAbsolutePath()); //NOI18N

                                // set jaxws.endorsed.dir property (for endorsed mechanism to be used with wsimport, wsgen)
                                setJaxWsEndorsedDirProperty(ep);

                                // move web-service-clients one level up from in project.xml
                                // WS should be part of auxiliary configuration
                                Element data = helper.getPrimaryConfigurationData(true);
                                NodeList nodes = data.getElementsByTagName(JAX_RPC_CLIENTS);
                                if (nodes.getLength() > 0) {
                                    Element oldJaxRpcClients = (Element) nodes.item(0);
                                    Document doc = createNewDocument();
                                    Element newJaxRpcClients = doc.createElementNS(JAX_RPC_NAMESPACE, JAX_RPC_CLIENTS);
                                    NodeList childNodes = oldJaxRpcClients.getElementsByTagName(JAX_RPC_CLIENT);
                                    for (int i = 0; i < childNodes.getLength(); i++) {
                                        Element oldJaxRpcClient = (Element) childNodes.item(i);
                                        Element newJaxRpcClient = doc.createElementNS(JAX_RPC_NAMESPACE, JAX_RPC_CLIENT);
                                        NodeList nodeProps = oldJaxRpcClient.getChildNodes();
                                        for (int j = 0; j < nodeProps.getLength(); j++) {
                                            Node n = nodeProps.item(j);
                                            if (n instanceof Element) {
                                                Element oldProp = (Element) n;
                                                Element newProp = doc.createElementNS(JAX_RPC_NAMESPACE, oldProp.getLocalName());
                                                String text = oldProp.getTextContent();
                                                newProp.setTextContent(text);
                                                newJaxRpcClient.appendChild(newProp);
                                            }
                                        }
                                        newJaxRpcClients.appendChild(newJaxRpcClient);
                                    }
                                    aux.putConfigurationFragment(newJaxRpcClients, true);
                                    data.removeChild(oldJaxRpcClients);
                                    helper.putPrimaryConfigurationData(data, true);
                                }

                                updateHelper.putProperties(AntProjectHelper.PRIVATE_PROPERTIES_PATH, ep);
                                ep = helper.getProperties(AntProjectHelper.PROJECT_PROPERTIES_PATH);
                                if (!ep.containsKey(ProjectProperties.INCLUDES)) {
                                    ep.setProperty(ProjectProperties.INCLUDES, "**"); // NOI18N
                                }
                                if (!ep.containsKey(ProjectProperties.EXCLUDES)) {
                                    ep.setProperty(ProjectProperties.EXCLUDES, ""); // NOI18N
                                }
                                helper.putProperties(AntProjectHelper.PROJECT_PROPERTIES_PATH, ep);
                                try {
                                    ProjectManager.getDefault().saveProject(J2SEProject.this);
                                } catch (IOException e) {
                                    //#91398 provide a better error message in case of read-only location of project.
                                    if (!J2SEProject.this.getProjectDirectory().canWrite()) {
                                        NotifyDescriptor nd = new NotifyDescriptor.Message(NbBundle.getMessage(J2SEProject.class, "ERR_ProjectReadOnly",
                                                J2SEProject.this.getProjectDirectory().getName()));
                                        DialogDisplayer.getDefault().notify(nd);
                                    } else {
                                        ErrorManager.getDefault().notify(e);
                                    }
                                }
                                return null;
                            }
                        });
                    }
                });
            } catch (IOException e) {
                Exceptions.printStackTrace(e);
            }
            J2SELogicalViewProvider physicalViewProvider = getLookup().lookup(J2SELogicalViewProvider.class);
            if (physicalViewProvider != null && physicalViewProvider.hasBrokenLinks()) {
                BrokenReferencesSupport.showAlert();
            }
            String prop = eval.getProperty(J2SEProjectProperties.SOURCE_ENCODING);
            if (prop != null) {
                try {
                    Charset c = Charset.forName(prop);
                } catch (IllegalCharsetNameException e) {
                    //Broken property, log & ignore
                    LOG.warning("Illegal charset: " + prop + " in project: " + FileUtil.getFileDisplayName(getProjectDirectory())); //NOI18N
                } catch (UnsupportedCharsetException e) {
                    //todo: Needs UI notification like broken references.
                    LOG.warning("Unsupported charset: " + prop + " in project: " + FileUtil.getFileDisplayName(getProjectDirectory())); //NOI18N
                }
            }
        }

        protected void projectClosed() {
            // just do if the whole project was not deleted...
            if (getProjectDirectory().isValid()) {
                // Probably unnecessary, but just in case:
                try {
                    ProjectManager.getDefault().saveProject(J2SEProject.this);
                } catch (IOException e) {
                    if (!J2SEProject.this.getProjectDirectory().canWrite()) {
                        // #91398 - ignore, we already reported on project open. 
                        // not counting with someone setting the ro flag while the project is opened.
                    } else {
                        ErrorManager.getDefault().notify(e);
                    }
                }
            }

            // --- unregister project's classpaths to GlobalPathRegistry            
            /** unregister java classpaths */
            GlobalPathRegistry.getDefault().unregister(
                    ClassPath.BOOT, 
                    cpProvider.getProjectClassPaths(ClassPath.BOOT));
            GlobalPathRegistry.getDefault().unregister(
                    ClassPath.SOURCE, 
                    cpProvider.getProjectClassPaths(ClassPath.SOURCE));
            GlobalPathRegistry.getDefault().unregister(
                    ClassPath.COMPILE, 
                    cpProvider.getProjectClassPaths(ClassPath.COMPILE));

            if (coreLibsCp != null) {
                // Do we need unregister coreLibsCp?
                //GlobalPathRegistry.getDefault().unregister(ClassPath.BOOT, new ClassPath[]{coreLibsCp});
            }

            /** 
             * Why unreister COMPILE/BOOT classpath will cause:
             * java.lang.IllegalArgumentException: Attempt to remove nonexistent path ClassPath[Entry[file:/Users/dcaoyuan/my-project/nbsrc/main/nbbuild/netbeans/extra/scala/scala-2.7.1.final/src/META-INF/], Entry[file:/Users/dcaoyuan/my-project/nbsrc/main/nbbuild/netbeans/extra/scala/scala-2.7.1.final/src/sbaz/], Entry[file:/Users/dcaoyuan/my-project/nbsrc/main/nbbuild/netbeans/extra/scala/scala-2.7.1.final/src/scala/], Entry[file:/Users/dcaoyuan/my-project/nbsrc/main/nbbuild/netbeans/extra/scalastubs/]]
             * This never happen on Java classpath
             * Anyway, we can keep COMPILE/BOOT classpath there, since they are shared by all scala projects.
             */
            //GlobalPathRegistry.getDefault().unregister(ClassPath.BOOT, cpProvider.getProjectClassPaths(ClassPath.BOOT));
            //GlobalPathRegistry.getDefault().unregister(ClassPath.COMPILE, cpProvider.getProjectClassPaths(ClassPath.COMPILE));
            //GlobalPathRegistry.getDefault().unregister(ClassPath.SOURCE, cpProvider.getProjectClassPaths(ClassPath.SOURCE));
            if (mainClassUpdater != null) {
                mainClassUpdater.unregister();
                mainClassUpdater = null;
            }
        }
    }

    /**
     * Exports the main JAR as an official build product for use from other scripts.
     * The type of the artifact will be {@link AntArtifact#TYPE_JAR}.
     */
    private final class AntArtifactProviderImpl implements AntArtifactProvider {

        public AntArtifact[] getBuildArtifacts() {
            return new AntArtifact[]{
                        helper.createSimpleAntArtifact(JavaProjectConstants.ARTIFACT_TYPE_JAR, "dist.jar", evaluator(), "jar", "clean"), // NOI18N

                    };
        }
    }

    private static final class RecommendedTemplatesImpl implements RecommendedTemplates, PrivilegedTemplates {

        RecommendedTemplatesImpl(UpdateHelper helper) {
            this.helper = helper;
        }
        private UpdateHelper helper;
        // List of primarily supported templates
        private static final String[] APPLICATION_TYPES = new String[]{
            "java-classes", // NOI18N
            "java-main-class", // NOI18N
            "java-forms", // NOI18N
            "gui-java-application", // NOI18N
            "java-beans", // NOI18N
            "persistence", // NOI18N
            "oasis-XML-catalogs", // NOI18N
            "XML", // NOI18N
            "ant-script", // NOI18N
            "ant-task", // NOI18N
            "web-service-clients", // NOI18N
            "wsdl", // NOI18N
            // "servlet-types",     // NOI18N
            // "web-types",         // NOI18N
            "junit", // NOI18N
            // "MIDP",              // NOI18N
            "simple-files"          // NOI18N

        };
        private static final String[] LIBRARY_TYPES = new String[]{
            "java-classes", // NOI18N
            "java-main-class", // NOI18N
            "java-forms", // NOI18N
            //"gui-java-application", // NOI18N
            "java-beans", // NOI18N
            "persistence", // NOI18N
            "oasis-XML-catalogs", // NOI18N
            "XML", // NOI18N
            "ant-script", // NOI18N
            "ant-task", // NOI18N
            "servlet-types", // NOI18N
            "web-service-clients", // NOI18N
            "wsdl", // NOI18N
            // "web-types",         // NOI18N
            "junit", // NOI18N
            // "MIDP",              // NOI18N
            "simple-files"          // NOI18N

        };
        private static final String[] PRIVILEGED_NAMES = new String[]{
            "Templates/Classes/Class.java", // NOI18N
            "Templates/Classes/Package", // NOI18N
            "Templates/Classes/Interface.java", // NOI18N
            "Templates/GUIForms/JPanel.java", // NOI18N
            "Templates/GUIForms/JFrame.java", // NOI18N
            "Templates/Persistence/Entity.java", // NOI18N
            "Templates/Persistence/RelatedCMP", // NOI18N                    
            "Templates/WebServices/WebServiceClient"   // NOI18N                    

        };

        public String[] getRecommendedTypes() {

            EditableProperties ep = helper.getProperties(AntProjectHelper.PROJECT_PROPERTIES_PATH);
            // if the project has no main class, it's not really an application
            boolean isLibrary = ep.getProperty(J2SEProjectProperties.MAIN_CLASS) == null || "".equals(ep.getProperty(J2SEProjectProperties.MAIN_CLASS)); // NOI18N
            return isLibrary ? LIBRARY_TYPES : APPLICATION_TYPES;
        }

        public String[] getPrivilegedTemplates() {
            return PRIVILEGED_NAMES;
        }
    }

    private static final class J2SEPropertyEvaluatorImpl implements J2SEPropertyEvaluator {

        private PropertyEvaluator evaluator;

        public J2SEPropertyEvaluatorImpl(PropertyEvaluator eval) {
            evaluator = eval;
        }

        public PropertyEvaluator evaluator() {
            return evaluator;
        }
    }

    private class J2SEExtenderImplementation implements AntBuildExtenderImplementation {
        //add targets here as required by the external plugins..

        public List<String> getExtensibleTargets() {
            String[] targets = new String[]{
                "-do-init", "-init-check", "-post-clean", "jar", "-pre-pre-compile", "-do-compile", "-do-compile-single" //NOI18N

            };
            return Arrays.asList(targets);
        }

        public Project getOwningProject() {
            return J2SEProject.this;
        }
    }
    private static final String ENDORSED_DIR_PROPERTY = "jaxws.endorsed.dir"; //NOI18N


    /** Set jaxws.endorsed.dir property for wsimport, wsgen tasks
     *  to specify jvmarg value : -Djava.endorsed.dirs=${jaxws.endorsed.dir}"
     */
    public static void setJaxWsEndorsedDirProperty(EditableProperties ep) {
        String oldJaxWsEndorsedDirs = ep.getProperty(ENDORSED_DIR_PROPERTY);
        String javaVersion = System.getProperty("java.specification.version"); //NOI18N
        if ("1.6".equals(javaVersion)) { //NOI18N
            String jaxWsEndorsedDirs = getJaxWsApiDir();
            if (jaxWsEndorsedDirs != null && !jaxWsEndorsedDirs.equals(oldJaxWsEndorsedDirs)) {
                ep.setProperty(ENDORSED_DIR_PROPERTY, jaxWsEndorsedDirs);
            }
        } else {
            if (oldJaxWsEndorsedDirs != null) {
                ep.remove(ENDORSED_DIR_PROPERTY);
            }
        }
    }

    private static String getJaxWsApiDir() {
        File file = InstalledFileLocator.getDefault().locate("modules/ext/jaxws21/api/jaxws-api.jar", null, false); // NOI18N
        if (file != null) {
            return file.getParent();
        }
        return null;
    }
    private static final DocumentBuilder db;
    

    static {
        try {
            db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new AssertionError(e);
        }
    }

    private static Document createNewDocument() {
        // #50198: for thread safety, use a separate document.
        // Using XMLUtil.createDocument is much too slow.
        synchronized (db) {
            return db.newDocument();
        }
    }
}
