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

package org.netbeans.modules.scala.stdplatform.wizard;

import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.scala.platform.ScalaPlatform;
import org.netbeans.api.scala.platform.ScalaPlatformManager;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.modules.scala.stdplatform.platformdefinition.J2SEPlatformImpl;
import org.netbeans.spi.java.classpath.PathResourceImplementation;
import org.netbeans.spi.java.classpath.support.ClassPathSupport;
import org.netbeans.spi.project.support.ant.PropertyUtils;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.util.Task;
import org.openide.util.TaskListener;
import org.openide.util.HelpCtx;
import org.openide.ErrorManager;
import org.openide.WizardDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileStateInvalidException;
import org.openide.filesystems.FileUtil;
import org.openide.filesystems.URLMapper;
import org.openide.util.ChangeSupport;

/**
 * This Panel launches autoconfiguration during the New J2SE Platform sequence.
 * The UI views properties of the platform, reacts to the end of detection by
 * updating itself. It triggers the detection task when the button is pressed.
 * The inner class WizardPanel acts as a controller, reacts to the UI completness
 * (jdk name filled in) and autoconfig result (passed successfully) - and manages
 * Next/Finish button (valid state) according to those.
 *
 * @author Svata Dedic
 */
public class DetectPanel extends javax.swing.JPanel {

    private NewJ2SEPlatform primaryPlatform;
    private final ChangeSupport cs = new ChangeSupport(this);

    /**
     * Creates a detect panel
     * start the task and update on its completion
     * @param primaryPlatform the platform being customized.
     */
    public DetectPanel(NewJ2SEPlatform primaryPlatform) {
        initComponents();
        postInitComponents ();
        putClientProperty("WizardPanel_contentData",
            new String[] {
                NbBundle.getMessage(DetectPanel.class,"TITLE_PlatformName"),
        });
        this.primaryPlatform = primaryPlatform;
        this.setName (NbBundle.getMessage(DetectPanel.class,"TITLE_PlatformName"));
    }

    public void addNotify() {
        super.addNotify();        
    }    

    private void postInitComponents () {
        this.jdkName.getDocument().addDocumentListener (new DocumentListener () {

            public void insertUpdate(DocumentEvent e) {
                handleNameChange ();
            }

            public void removeUpdate(DocumentEvent e) {
                handleNameChange ();
            }

            public void changedUpdate(DocumentEvent e) {
                handleNameChange ();
            }
        });                
        this.progressLabel.setVisible(false);
        this.progressPanel.setVisible(false);
    }

    private void handleNameChange () {
        cs.fireChange();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        jLabel3 = new javax.swing.JLabel();
        jdkName = new javax.swing.JTextField();
        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        javadoc = new javax.swing.JTextField();
        sources = new javax.swing.JTextField();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        progressLabel = new javax.swing.JLabel();
        progressPanel = new javax.swing.JPanel();

        setLayout(new java.awt.GridBagLayout());

        getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getBundle(DetectPanel.class).getString("AD_DetectPanel"));
        jLabel3.setLabelFor(jdkName);
        org.openide.awt.Mnemonics.setLocalizedText(jLabel3, NbBundle.getBundle(DetectPanel.class).getString("LBL_DetailsPanel_Name"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        add(jLabel3, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 6, 0, 0);
        add(jdkName, gridBagConstraints);
        jdkName.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getBundle(DetectPanel.class).getString("AD_PlatformName"));

        jPanel1.setLayout(new java.awt.GridBagLayout());

        jLabel1.setLabelFor(sources);
        org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getBundle(DetectPanel.class).getString("TXT_Sources"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel1.add(jLabel1, gridBagConstraints);

        jLabel4.setLabelFor(javadoc);
        org.openide.awt.Mnemonics.setLocalizedText(jLabel4, NbBundle.getBundle(DetectPanel.class).getString("TXT_JavaDoc"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(12, 0, 0, 0);
        jPanel1.add(jLabel4, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(12, 6, 0, 0);
        jPanel1.add(javadoc, gridBagConstraints);
        javadoc.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getBundle(DetectPanel.class).getString("AD_PlatformJavadoc"));

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 6, 0, 0);
        jPanel1.add(sources, gridBagConstraints);
        sources.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getBundle(DetectPanel.class).getString("AD_PlatformSources"));

        org.openide.awt.Mnemonics.setLocalizedText(jButton1, org.openide.util.NbBundle.getBundle(DetectPanel.class).getString("LBL_BrowseSources"));
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectSources(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 6, 0, 0);
        jPanel1.add(jButton1, gridBagConstraints);
        jButton1.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getBundle(DetectPanel.class).getString("AD_SelectSources"));

        org.openide.awt.Mnemonics.setLocalizedText(jButton2, org.openide.util.NbBundle.getBundle(DetectPanel.class).getString("LBL_BrowseJavadoc"));
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectJavadoc(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(12, 6, 0, 0);
        jPanel1.add(jButton2, gridBagConstraints);
        jButton2.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getBundle(DetectPanel.class).getString("AD_SelectJavadoc"));

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(24, 0, 0, 0);
        add(jPanel1, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        add(jPanel2, gridBagConstraints);

        progressLabel.setLabelFor(progressPanel);
        org.openide.awt.Mnemonics.setLocalizedText(progressLabel, java.util.ResourceBundle.getBundle("org/netbeans/modules/scala/stdplatform/wizard/Bundle").getString("TXT_PlatfromDetectProgress"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(12, 0, 6, 0);
        add(progressLabel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.gridheight = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        add(progressPanel, gridBagConstraints);

    }
    // </editor-fold>//GEN-END:initComponents

    private void selectJavadoc(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selectJavadoc
        String newValue = this.browse(this.javadoc.getText(),NbBundle.getMessage(DetectPanel.class,"TXT_SelectJavadoc"));
        if (newValue != null) {
            this.javadoc.setText(newValue);
        }
        
    }//GEN-LAST:event_selectJavadoc

    private void selectSources(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selectSources
        String newValue = this.browse(this.sources.getText(),NbBundle.getMessage(DetectPanel.class,"TXT_SelectSources"));
        if (newValue != null) {
            this.sources.setText(newValue);
        }
    }//GEN-LAST:event_selectSources
    
    public final synchronized void addChangeListener (ChangeListener listener) {
        cs.addChangeListener(listener);
    }

    public final synchronized void removeChangeListener (ChangeListener listener) {
        cs.removeChangeListener(listener);
    }

    public String getPlatformName() {
	    return jdkName.getText();
    }
    
    String getSources () {
        String val = this.sources.getText();
        return val.length() == 0 ? null : val;
    }

    void setSources (String sources) {
        this.sources.setText (sources == null ? "" : sources);      //NOI18N
    }

    String getJavadoc () {
        String val = this.javadoc.getText();
        return val.length() == 0 ? null : val;
    }

    void setJavadoc (String jdoc) {
        this.javadoc.setText(jdoc == null ? "" : jdoc);             //NOI18N
    }

    /**
     * Updates static information from the detected platform's properties
     */
    void updateData() {
        Map<String,String> m = primaryPlatform.getSystemProperties();
        // if the name is empty, fill something in:
        if ("".equals(jdkName.getText())) {
            jdkName.setText(getInitialName (m));
            this.jdkName.selectAll();
        }
    }


    private static String getInitialName (Map<String,String> m) {        
        String scalaVersion = m.get("scala.version.number");        //NOI18N
        StringBuilder result = new StringBuilder(NbBundle.getMessage(DetectPanel.class,"TXT_DetectPanel_Java"));        
        if (scalaVersion != null) {
            result.append(" ").append(scalaVersion);
        }
        return result.toString();
    }
    
    
    private String browse (String oldValue, String title) {
        JFileChooser chooser = new JFileChooser ();
        FileUtil.preventFileChooserSymlinkTraversal(chooser, null);
        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        chooser.setFileFilter (new FileFilter () {
            public boolean accept(File f) {
                return (f.exists() && f.canRead() && (f.isDirectory() || (f.getName().endsWith(".zip") || f.getName().endsWith(".jar"))));
            }

            public String getDescription() {
                return NbBundle.getMessage(DetectPanel.class,"TXT_ZipFilter");
            }
        });
        File f = new File (oldValue);
        chooser.setSelectedFile(f);
        chooser.setDialogTitle (title);
        if (chooser.showOpenDialog (this) == JFileChooser.APPROVE_OPTION) {
            return chooser.getSelectedFile().getAbsolutePath();
        }
        return null;
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JTextField javadoc;
    private javax.swing.JTextField jdkName;
    private javax.swing.JLabel progressLabel;
    private javax.swing.JPanel progressPanel;
    private javax.swing.JTextField sources;
    // End of variables declaration//GEN-END:variables

    /**
     * Controller for the outer class: manages wizard panel's valid state
     * according to the user's input and detection state.
     */
    static class WizardPanel implements WizardDescriptor.Panel<WizardDescriptor>, TaskListener, ChangeListener {
        private DetectPanel         component;
        private RequestProcessor.Task task;
        private final J2SEWizardIterator  iterator;
        private final ChangeSupport cs = new ChangeSupport(this);
        private boolean             detected;
        private boolean             valid;
        private boolean             firstPass=true;
        private WizardDescriptor    wiz;
        private ProgressHandle      progressHandle;

        WizardPanel(J2SEWizardIterator iterator) {            
	    this.iterator = iterator;
        }

        public void addChangeListener(ChangeListener l) {
            cs.addChangeListener(l);
        }

        public java.awt.Component getComponent() {
            if (component == null) {
                final NewJ2SEPlatform primaryPlatform = this.iterator.getPlatform();
                final NewJ2SEPlatform secondaryPlatform = this.iterator.getSecondaryPlatform();
                component = new DetectPanel(primaryPlatform);
                component.addChangeListener (this);
                task = RequestProcessor.getDefault().create(
                    new Runnable() {
                        public void run() {
                            primaryPlatform.run();
                            if (secondaryPlatform != null) {
                                secondaryPlatform.run();
                            }
                        }
                });
                task.addTaskListener(this);
            }
            return component;
        }

        void setValid(boolean v) {
            if (v == valid)
                return;
            valid = v;
            cs.fireChange();
        }

        public HelpCtx getHelp() {
            return new HelpCtx (DetectPanel.class);
        }

        public boolean isValid() {
            return valid;
        }

        public void readSettings(WizardDescriptor settings) {           
            this.wiz = settings;
            ScalaPlatform platform = this.iterator.getPlatform();
            String srcPath = null;
            String jdocPath = null;
            ClassPath src = platform.getSourceFolders();
            if (src.entries().size()>0) {
                URL folderRoot = src.entries().get(0).getURL();
                if ("jar".equals(folderRoot.getProtocol())) {   //NOI18N
                    folderRoot = FileUtil.getArchiveFile (folderRoot);
                }
                srcPath = new File(URI.create(folderRoot.toExternalForm())).getAbsolutePath();
            }
            else if (firstPass) {
                for (FileObject folder : platform.getInstallFolders()) {
                    File base = FileUtil.toFile(folder);
                    if (base!=null) {
                        File f = new File (base,"src.zip"); //NOI18N
                        if (f.canRead()) {
                            srcPath = f.getAbsolutePath();
                        }
                        else {
                            f = new File (base,"src.jar");  //NOI18N
                            if (f.canRead()) {
                                srcPath = f.getAbsolutePath();
                            }
                        }
                    }
                }                
            }
            List<URL> jdoc = platform.getJavadocFolders();
            if (jdoc.size()>0) {
                URL folderRoot = jdoc.get(0);
                if ("jar".equals(folderRoot.getProtocol())) {
                    folderRoot = FileUtil.getArchiveFile (folderRoot);
                }
                jdocPath = new File (URI.create(folderRoot.toExternalForm())).getAbsolutePath();
            }
            else if (firstPass) {
                for (FileObject folder : platform.getInstallFolders()) {
                    File base = FileUtil.toFile(folder);
                    if (base!=null) {
                        File f = new File (base,"docs"); //NOI18N
                        if (f.isDirectory() && f.canRead()) {
                            jdocPath = f.getAbsolutePath();
                        }                        
                    }
                }
                firstPass = false;
            }
            this.component.setSources (srcPath);
            this.component.setJavadoc (jdocPath);
            this.component.jdkName.setEditable(false);
            this.component.progressPanel.setVisible (true);
            this.component.progressLabel.setVisible (true);
            
            this.progressHandle = ProgressHandleFactory.createHandle(NbBundle.getMessage(DetectPanel.class,"TXT_PlatfromDetectProgress"));
            this.component.progressPanel.removeAll();
            this.component.progressPanel.setLayout (new GridBagLayout ());
            GridBagConstraints c = new GridBagConstraints ();
            c.gridx = c.gridy = GridBagConstraints.RELATIVE;
            c.gridheight = c.gridwidth = GridBagConstraints.REMAINDER;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.weightx = 1.0;
            JComponent pc = ProgressHandleFactory.createProgressComponent(this.progressHandle);
            ((GridBagLayout)this.component.progressPanel.getLayout ()).setConstraints(pc,c);
            this.component.progressPanel.add (pc);
            this.progressHandle.start ();
            task.schedule(0);
        }

        public void removeChangeListener(ChangeListener l) {
            cs.removeChangeListener(l);
        }

	/**
	 Updates the Platform's display name with the one the user
	 has entered. Stores user-customized display name into the Platform.
	 */
        public void storeSettings(WizardDescriptor settings) {
            if (isValid()) {                                
                String name = component.getPlatformName();                
                List<PathResourceImplementation> src = new ArrayList<PathResourceImplementation>();
                List<URL> jdoc = new ArrayList<URL>();
                String srcPath = this.component.getSources();
                if (srcPath!=null) {
                    File f = new File (srcPath);
                    try {
                        URL url = f.toURI().toURL();
                        if (FileUtil.isArchiveFile(url)) {
                            url = FileUtil.getArchiveRoot(url);
                            FileObject fo = URLMapper.findFileObject(url);
                            if (fo != null) {
                                fo = fo.getFileObject("src");   //NOI18N
                                if (fo != null) {
                                    url = fo.getURL();
                                }
                            }
                            src.add (ClassPathSupport.createResource(url));
                        }
                        else {
                            src.add (ClassPathSupport.createResource(url));
                        }
                    } catch (MalformedURLException mue) {
                        ErrorManager.getDefault().notify (mue);
                    }
                    catch (FileStateInvalidException e) {
                        ErrorManager.getDefault().notify(e);
                    }
                }
                String jdocPath = this.component.getJavadoc();
                if (jdocPath!=null) {
                    File f = new File (jdocPath);
                    try {
                        URL url = f.toURI().toURL();
                        if (FileUtil.isArchiveFile(url)) {
                            url = FileUtil.getArchiveRoot(url);
                        }
                        else if (!f.exists()){
                            url = new URL (url.toExternalForm()+'/');
                        }
                        jdoc.add (url);
                    } catch (MalformedURLException mue) {
                        ErrorManager.getDefault().notify (mue);
                    }
                }
                
                NewJ2SEPlatform platform = this.iterator.getPlatform();
                platform.setDisplayName (name);
                platform.setAntName (createAntName (name));
                platform.setSourceFolders (ClassPathSupport.createClassPath(src));
                platform.setJavadocFolders (jdoc);
                
                platform = this.iterator.getSecondaryPlatform();
                if (platform != null) {
                    name = NbBundle.getMessage(DetectPanel.class,"FMT_64BIT", name);
                    platform.setDisplayName (name);
                    platform.setAntName (createAntName(name));
                    platform.setSourceFolders (ClassPathSupport.createClassPath(src));
                    platform.setJavadocFolders (jdoc);
                }                                
            }
        }

        /**
         * Revalidates the Wizard Panel
         */
        public void taskFinished(Task task) {
            EventQueue.invokeLater(new Runnable() {
                public void run () {
                    component.updateData ();
                    component.jdkName.setEditable(true);
                    assert progressHandle != null;
                    progressHandle.finish ();
                    component.progressPanel.setVisible (false);
                    component.progressLabel.setVisible (false);                    
                    detected = iterator.getPlatform().isValid();                    
                    checkValid ();
                }
            });            
        }


        public void stateChanged(ChangeEvent e) {
             this.checkValid();
        }

        private void checkValid () {
            this.wiz.putProperty( "WizardPanel_errorMessage", ""); //NOI18N
            String name = this.component.getPlatformName ();            
            boolean validDisplayName = name.length() > 0;            
            boolean usedDisplayName = false;            
            if (!detected) {
                this.wiz.putProperty( "WizardPanel_errorMessage",NbBundle.getMessage(DetectPanel.class,"ERROR_NoSDKRegistry"));         //NOI18N
            }
            else if (!validDisplayName) {
                this.wiz.putProperty( "WizardPanel_errorMessage",NbBundle.getMessage(DetectPanel.class,"ERROR_InvalidDisplayName"));    //NOI18N
            }
            else {                
                ScalaPlatform[] platforms = ScalaPlatformManager.getDefault().getInstalledPlatforms();                
                for (int i=0; i<platforms.length; i++) {
                    if (name.equals (platforms[i].getDisplayName())) {
                        usedDisplayName = true;
                        this.wiz.putProperty( "WizardPanel_errorMessage",NbBundle.getMessage(DetectPanel.class,"ERROR_UsedDisplayName"));    //NOI18N
                        break;
                    }
                }                
            }
            boolean v = detected && validDisplayName && !usedDisplayName;
            setValid(v);            
        }

        private static String createAntName (String name) {
            if (name == null || name.length() == 0) {
                throw new IllegalArgumentException ();
            }                        
            String antName = PropertyUtils.getUsablePropertyName(name);            
            if (platformExists (antName)) {
                String baseName = antName;
                int index = 1;
                antName = baseName + Integer.toString (index);
                while (platformExists (antName)) {
                    index ++;
                    antName = baseName + Integer.toString (index);
                }
            }
            return antName;
        }
        
        private static boolean platformExists (String antName) {
            ScalaPlatformManager mgr = ScalaPlatformManager.getDefault();
            ScalaPlatform[] platforms = mgr.getInstalledPlatforms();
            for (int i=0; i < platforms.length; i++) {
                if (platforms[i] instanceof J2SEPlatformImpl) {
                    String val = ((J2SEPlatformImpl)platforms[i]).getAntName();
                    if (antName.equals(val)) {
                        return true;
                    }
                }
            }
            return false;
        }
        
    }    
}
