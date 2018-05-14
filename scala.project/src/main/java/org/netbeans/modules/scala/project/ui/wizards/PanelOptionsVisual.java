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


package org.netbeans.modules.scala.project.ui.wizards;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.text.MessageFormat;
import java.util.StringTokenizer;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import org.netbeans.api.queries.CollocationQuery;
import org.netbeans.spi.java.project.support.ui.SharableLibrariesUtils;
import org.netbeans.spi.project.support.ant.PropertyUtils;
import org.openide.WizardDescriptor;
import org.openide.WizardValidationException;
import org.openide.filesystems.FileUtil;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;

import javax.swing.GroupLayout;
import javax.swing.LayoutStyle;

/**
 * @author  phrebejk
 */
public class PanelOptionsVisual extends SettingsPanel implements ActionListener, PropertyChangeListener {
    
    private static boolean lastMainClassCheck = true; // XXX Store somewhere
    
    public static final String SHARED_LIBRARIES = "sharedLibraries"; //NOI18N
    
    private PanelConfigureProject panel;
    private boolean valid;
    private String currentLibrariesLocation;
    private String projectLocation;
    
    public PanelOptionsVisual(PanelConfigureProject panel, NewJ2SEProjectWizardIterator.WizardType type) {
        initComponents();
        this.panel = panel;
        currentLibrariesLocation = "." + File.separatorChar + "lib"; // NOI18N
        txtLibFolder.setText(currentLibrariesLocation);
        cbSharableActionPerformed(null);

        switch (type) {
            case LIB:
                setAsMainCheckBox.setVisible( false );
                createMainCheckBox.setVisible( false );
                mainClassTextField.setVisible( false );
                break;
            case APP:
                createMainCheckBox.addActionListener( this );
                createMainCheckBox.setSelected( lastMainClassCheck );
                mainClassTextField.setEnabled( lastMainClassCheck );
                break;
            case EXT:
                setAsMainCheckBox.setVisible( true );
                createMainCheckBox.setVisible( false );
                mainClassTextField.setVisible( false );
                break;
        }
        
        this.mainClassTextField.getDocument().addDocumentListener( new DocumentListener () {
            
            public void insertUpdate(DocumentEvent e) {
                mainClassChanged ();
            }
            
            public void removeUpdate(DocumentEvent e) {
                mainClassChanged ();
            }
            
            public void changedUpdate(DocumentEvent e) {
                mainClassChanged ();
            }
            
        });
        this.txtLibFolder.getDocument().addDocumentListener( new DocumentListener () {
            
            public void insertUpdate(DocumentEvent e) {
                librariesLocationChanged ();
            }
            
            public void removeUpdate(DocumentEvent e) {
                librariesLocationChanged ();
            }
            
            public void changedUpdate(DocumentEvent e) {
                librariesLocationChanged ();
            }

            
        });
        
    }

    public void actionPerformed( ActionEvent e ) {        
        if ( e.getSource() == createMainCheckBox ) {
            lastMainClassCheck = createMainCheckBox.isSelected();
            mainClassTextField.setEnabled( lastMainClassCheck );        
            this.panel.fireChangeEvent();
        }                
    }
    
    public void propertyChange (PropertyChangeEvent event) {
        if (PanelProjectLocationVisual.PROP_PROJECT_NAME.equals(event.getPropertyName())) {
            String newProjectName = NewJ2SEProjectWizardIterator.getPackageName((String) event.getNewValue());
            if (!Utilities.isJavaIdentifier(newProjectName)) {
                newProjectName = NbBundle.getMessage (PanelOptionsVisual.class, "TXT_PackageNameSuffix", newProjectName); 
            }
            this.mainClassTextField.setText (MessageFormat.format(
                NbBundle.getMessage (PanelOptionsVisual.class,"TXT_ClassName"), new Object[] {newProjectName}
            ));
        }
        if (PanelProjectLocationVisual.PROP_PROJECT_LOCATION.equals(event.getPropertyName())) {
            projectLocation = (String)event.getNewValue();
        }
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        cbSharable = new javax.swing.JCheckBox();
        lblLibFolder = new javax.swing.JLabel();
        txtLibFolder = new javax.swing.JTextField();
        btnLibFolder = new javax.swing.JButton();
        lblHint = new javax.swing.JLabel();
        createMainCheckBox = new javax.swing.JCheckBox();
        mainClassTextField = new javax.swing.JTextField();
        setAsMainCheckBox = new javax.swing.JCheckBox();

        cbSharable.setSelected(SharableLibrariesUtils.isLastProjectSharable());
        org.openide.awt.Mnemonics.setLocalizedText(cbSharable, org.openide.util.NbBundle.getMessage(PanelOptionsVisual.class, "LBL_PanelOptions_SharableProject_Checkbox")); // NOI18N
        cbSharable.setMargin(new java.awt.Insets(0, 0, 0, 0));
        cbSharable.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cbSharableActionPerformed(evt);
            }
        });

        lblLibFolder.setLabelFor(txtLibFolder);
        org.openide.awt.Mnemonics.setLocalizedText(lblLibFolder, org.openide.util.NbBundle.getMessage(PanelOptionsVisual.class, "LBL_PanelOptions_Location_Label")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(btnLibFolder, org.openide.util.NbBundle.getMessage(PanelOptionsVisual.class, "LBL_PanelOptions_Browse_Button")); // NOI18N
        btnLibFolder.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnLibFolderActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(lblHint, org.openide.util.NbBundle.getMessage(PanelOptionsVisual.class, "HINT_LibrariesFolder")); // NOI18N

        createMainCheckBox.setSelected(true);
        org.openide.awt.Mnemonics.setLocalizedText(createMainCheckBox, org.openide.util.NbBundle.getBundle(PanelOptionsVisual.class).getString("LBL_createMainCheckBox")); // NOI18N
        createMainCheckBox.setMargin(new java.awt.Insets(0, 0, 0, 0));

        mainClassTextField.setText("com.myapp.Main");

        setAsMainCheckBox.setSelected(true);
        org.openide.awt.Mnemonics.setLocalizedText(setAsMainCheckBox, org.openide.util.NbBundle.getBundle(PanelOptionsVisual.class).getString("LBL_setAsMainCheckBox")); // NOI18N
        setAsMainCheckBox.setMargin(new java.awt.Insets(0, 0, 0, 0));

        GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(cbSharable)
                        .addGroup(layout.createSequentialGroup()
                            .addGap(19,19,19)
                            .addComponent(lblLibFolder)
                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)    
                            .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(lblHint, GroupLayout.PREFERRED_SIZE, 243, Short.MAX_VALUE)
                                .addComponent(txtLibFolder, GroupLayout.DEFAULT_SIZE, 243, Short.MAX_VALUE))))
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(btnLibFolder))
                .addGroup(layout.createSequentialGroup()
                    .addComponent(setAsMainCheckBox)
                    .addContainerGap())
                .addGroup(layout.createSequentialGroup()
                    .addComponent(createMainCheckBox)
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(mainClassTextField, GroupLayout.DEFAULT_SIZE, 319, Short.MAX_VALUE)));

        layout.setVerticalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(cbSharable)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(btnLibFolder)
                    .addComponent(txtLibFolder, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblLibFolder))
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblHint)
                .addGap(30, 30, 30)
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(createMainCheckBox)
                    .addComponent(mainClassTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(setAsMainCheckBox)
                .addContainerGap(50, Short.MAX_VALUE)));

        cbSharable.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(PanelOptionsVisual.class, "ACSD_sharableProject")); // NOI18N
        txtLibFolder.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(PanelOptionsVisual.class, "ACSD_LibrariesLocation")); // NOI18N
        btnLibFolder.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(PanelOptionsVisual.class, "ACSD_browseLibraries")); // NOI18N
        createMainCheckBox.getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getBundle(PanelOptionsVisual.class).getString("ACSN_createMainCheckBox")); // NOI18N
        createMainCheckBox.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getBundle(PanelOptionsVisual.class).getString("ACSD_createMainCheckBox")); // NOI18N
        mainClassTextField.getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getBundle(PanelOptionsVisual.class).getString("ASCN_mainClassTextFiled")); // NOI18N
        mainClassTextField.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getBundle(PanelOptionsVisual.class).getString("ASCD_mainClassTextFiled")); // NOI18N
        setAsMainCheckBox.getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getBundle(PanelOptionsVisual.class).getString("ACSN_setAsMainCheckBox")); // NOI18N
        setAsMainCheckBox.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getBundle(PanelOptionsVisual.class).getString("ACSD_setAsMainCheckBox")); // NOI18N

        getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getMessage(PanelOptionsVisual.class, "ACSN_PanelOptionsVisual")); // NOI18N
        getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(PanelOptionsVisual.class, "ACSD_PanelOptionsVisual")); // NOI18N
    }// </editor-fold>//GEN-END:initComponents

    private void cbSharableActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cbSharableActionPerformed
        txtLibFolder.setEnabled(cbSharable.isSelected());
        btnLibFolder.setEnabled(cbSharable.isSelected());
        lblHint.setEnabled(cbSharable.isSelected());
        lblLibFolder.setEnabled(cbSharable.isSelected());
        if (cbSharable.isSelected()) {
            txtLibFolder.setText(currentLibrariesLocation);
        } else {
            txtLibFolder.setText(""); //NOi18N
        }
}//GEN-LAST:event_cbSharableActionPerformed

    private void btnLibFolderActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnLibFolderActionPerformed
        // below folder is used just for relativization:
        File f = FileUtil.normalizeFile(new File(projectLocation + 
                File.separatorChar + "project_folder")); // NOI18N
        String curr = SharableLibrariesUtils.browseForLibraryLocation(txtLibFolder.getText().trim(), this, f);
        if (curr != null) {
            currentLibrariesLocation = curr;
            if (cbSharable.isSelected()) {
                txtLibFolder.setText(currentLibrariesLocation);
            }
        }
}//GEN-LAST:event_btnLibFolderActionPerformed
    

    
    boolean valid(WizardDescriptor settings) {
        
        if (cbSharable.isSelected()) {
            String location = txtLibFolder.getText();
            if (projectLocation != null) {
                if (new File(location).isAbsolute()) {
                    settings.putProperty(WizardDescriptor.PROP_ERROR_MESSAGE,
                        NbBundle.getMessage(PanelOptionsVisual.class, "WARN_PanelOptionsVisual.absolutePath"));
                
                } else {
                    File projectLoc = FileUtil.normalizeFile(new File(projectLocation));
                    File libLoc = PropertyUtils.resolveFile(projectLoc, location);
                    if (!CollocationQuery.areCollocated(projectLoc, libLoc)) {
                        settings.putProperty(WizardDescriptor.PROP_ERROR_MESSAGE,
                            NbBundle.getMessage(PanelOptionsVisual.class, "WARN_PanelOptionsVisual.relativePath")); 
                    }
                }
            }
        }
        
        if (mainClassTextField.isVisible () && mainClassTextField.isEnabled ()) {
            if (!valid) {
                settings.putProperty(WizardDescriptor.PROP_ERROR_MESSAGE,
                    NbBundle.getMessage(PanelOptionsVisual.class,"ERROR_IllegalMainClassName")); //NOI18N
            }
            return this.valid;
        }
        else {
            return true;
        }
    }
    
    void read (WizardDescriptor d) {
    }
    
    void validate (WizardDescriptor d) throws WizardValidationException {
        // nothing to validate
    }

    void store( WizardDescriptor d ) {
        d.putProperty( /*XXX Define somewhere */ "setAsMain", setAsMainCheckBox.isSelected() && setAsMainCheckBox.isVisible() ? Boolean.TRUE : Boolean.FALSE ); // NOI18N
        d.putProperty( /*XXX Define somewhere */ "mainClass", createMainCheckBox.isSelected() && createMainCheckBox.isVisible() ? mainClassTextField.getText() : null ); // NOI18N
        d.putProperty( SHARED_LIBRARIES, cbSharable.isSelected() ? txtLibFolder.getText() : null ); // NOI18N
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnLibFolder;
    private javax.swing.JCheckBox cbSharable;
    private javax.swing.JCheckBox createMainCheckBox;
    private javax.swing.JLabel lblHint;
    private javax.swing.JLabel lblLibFolder;
    private javax.swing.JTextField mainClassTextField;
    private javax.swing.JCheckBox setAsMainCheckBox;
    private javax.swing.JTextField txtLibFolder;
    // End of variables declaration//GEN-END:variables
    
    private void mainClassChanged () {
        String mainClassName = this.mainClassTextField.getText ();
        StringTokenizer tk = new StringTokenizer (mainClassName, "."); //NOI18N
        boolean valid = true;
        while (tk.hasMoreTokens()) {
            String token = tk.nextToken();
            if (token.length() == 0 || !Utilities.isJavaIdentifier(token)) {
                valid = false;
                break;
            }            
        }
        this.valid = valid;
        this.panel.fireChangeEvent();
    }
    
    private void librariesLocationChanged() {
        this.panel.fireChangeEvent();
        
    }
    
}

