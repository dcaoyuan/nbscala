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

package org.netbeans.modules.scala.refactoring.ui

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.Collator;
import java.util.Arrays;
import java.util.Comparator;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.UIResource;
import org.netbeans.api.java.project.JavaProjectConstants;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectInformation;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.api.project.Sources;
import org.netbeans.api.project.ui.OpenProjects;
import org.netbeans.modules.refactoring.spi.ui.CustomRefactoringPanel;
import org.netbeans.spi.java.project.support.ui.PackageView;
import org.openide.filesystems.FileObject;

/**
 * Asks where to move a class to.
 * @author Jan Becicka, Jesse Glick
 */
class MoveClassPanel(parent: ChangeListener, startPackage: String, headLine: String, fo: FileObject
) extends JPanel with ActionListener with DocumentListener with CustomRefactoringPanel {
  
  private val GROUP_CELL_RENDERER = new GroupCellRenderer
  private val PROJECT_CELL_RENDERER = new ProjectCellRenderer
    
  private var groups: Array[SourceGroup] = _

  // Variables declaration - do not modify//GEN-BEGIN:variables
  protected var bottomPanel: javax.swing.JPanel = _
  private var labelHeadLine: javax.swing.JLabel = _
  private var labelLocation: javax.swing.JLabel = _
  private var labelPackage: javax.swing.JLabel = _
  private var labelProject: javax.swing.JLabel = _
  private var packageComboBox: javax.swing.JComboBox = _
  private var projectsComboBox: javax.swing.JComboBox = _
  private var rootComboBox: javax.swing.JComboBox = _
  private var updateReferencesCheckBox: javax.swing.JCheckBox = _
  // End of variables declaration//GEN-END:variables



  initComponents
  setCombosEnabled(true)
  labelHeadLine.setText(headLine)
  rootComboBox.setRenderer(GROUP_CELL_RENDERER)
  packageComboBox.setRenderer(PackageView.listRenderer)
  projectsComboBox.setRenderer( PROJECT_CELL_RENDERER )
                
  rootComboBox.addActionListener( this )
  packageComboBox.addActionListener( this )
  projectsComboBox.addActionListener( this )
        
  val textField = packageComboBox.getEditor().getEditorComponent
  if (textField.isInstanceOf[JTextField]) {
    textField.asInstanceOf[JTextField].getDocument.addDocumentListener(this)
  }
        
  private var project = if (fo != null) FileOwnerQuery.getOwner(fo) else OpenProjects.getDefault.getOpenProjects()(0)
        
    
  private var initialized = false
  def initialize {
    if (initialized)
      return
    //put initialization code here
    initValues(startPackage)
    initialized = true
  }
    
  def initValues(preselectedFolder: String) {
        
    val openProjects = OpenProjects.getDefault.getOpenProjects sortWith {(p1, p2) => 
      Collator.getInstance.compare(ProjectUtils.getInformation(p1).getDisplayName, 
                                   ProjectUtils.getInformation(p2).getDisplayName) > 0
    }

    val projectsModel = new DefaultComboBoxModel(openProjects.asInstanceOf[Array[Object]])
    projectsComboBox.setModel( projectsModel );
    projectsComboBox.setSelectedItem( project );
        
    updateRoots
    updatePackages
    if (preselectedFolder != null) {
      packageComboBox.setSelectedItem(preselectedFolder)
    }
    // Determine the extension
  }
    
  override def requestFocus {
    packageComboBox.requestFocus
  }
    
  def getRootFolder: FileObject = {
    rootComboBox.getSelectedItem.asInstanceOf[SourceGroup].getRootFolder
  }
    
  def getPackageName: String = {
    val packageName = packageComboBox.getEditor.getItem.toString;
    return packageName; // NOI18N
  }
    
  private def fireChange {
    parent.stateChanged(null)
  }
    
  /** This method is called from within the constructor to
   * initialize the form.
   * WARNING: Do NOT modify this code. The content of this method is
   * always regenerated by the Form Editor.
   */
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private def initComponents {
    var gridBagConstraints: java.awt.GridBagConstraints = null

    labelProject = new javax.swing.JLabel();
    projectsComboBox = new javax.swing.JComboBox();
    labelLocation = new javax.swing.JLabel();
    rootComboBox = new javax.swing.JComboBox();
    labelPackage = new javax.swing.JLabel();
    packageComboBox = new javax.swing.JComboBox();
    bottomPanel = new javax.swing.JPanel();
    labelHeadLine = new javax.swing.JLabel();
    updateReferencesCheckBox = new javax.swing.JCheckBox();

    setLayout(new java.awt.GridBagLayout());

    labelProject.setLabelFor(projectsComboBox);
    org.openide.awt.Mnemonics.setLocalizedText(labelProject, org.openide.util.NbBundle.getMessage(classOf[MoveClassPanel], "LBL_Project")); // NOI18N
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(0, 0, 6, 0);
    add(labelProject, gridBagConstraints);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(0, 6, 6, 0);
    add(projectsComboBox, gridBagConstraints);
    val bundle = java.util.ResourceBundle.getBundle("org/netbeans/modules/refactoring/java/ui/Bundle"); // NOI18N
    projectsComboBox.getAccessibleContext().setAccessibleDescription(bundle.getString("ACSD_projectsCombo")); // NOI18N

    labelLocation.setLabelFor(rootComboBox);
    org.openide.awt.Mnemonics.setLocalizedText(labelLocation, org.openide.util.NbBundle.getMessage(classOf[MoveClassPanel], "LBL_Location")); // NOI18N
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(0, 0, 6, 0);
    add(labelLocation, gridBagConstraints);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(0, 6, 6, 0);
    add(rootComboBox, gridBagConstraints);
    rootComboBox.getAccessibleContext().setAccessibleDescription(bundle.getString("ACSD_rootCombo")); // NOI18N

    labelPackage.setLabelFor(packageComboBox);
    org.openide.awt.Mnemonics.setLocalizedText(labelPackage, org.openide.util.NbBundle.getMessage(classOf[MoveClassPanel], "LBL_ToPackage")); // NOI18N
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(0, 0, 6, 0);
    add(labelPackage, gridBagConstraints);

    packageComboBox.setEditable(true);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(0, 6, 6, 0);
    add(packageComboBox, gridBagConstraints);
    packageComboBox.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(classOf[MoveClassPanel], "MoveClassPanel.packageComboBox.AccessibleContext.accessibleDescription")); // NOI18N

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    add(bottomPanel, gridBagConstraints);

    labelHeadLine.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 6, 1));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    add(labelHeadLine, gridBagConstraints);

    org.openide.awt.Mnemonics.setLocalizedText(updateReferencesCheckBox, org.openide.util.NbBundle.getBundle(classOf[MoveClassPanel]).getString("LBL_MoveWithoutReferences")); // NOI18N
    updateReferencesCheckBox.setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 4, 0, 4));
    updateReferencesCheckBox.setMargin(new java.awt.Insets(2, 2, 0, 2));
    updateReferencesCheckBox.addItemListener(new java.awt.event.ItemListener() {
        def itemStateChanged(evt: java.awt.event.ItemEvent) {
          updateReferencesCheckBoxItemStateChanged(evt);
        }
      });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 5;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    add(updateReferencesCheckBox, gridBagConstraints);
    updateReferencesCheckBox.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(classOf[MoveClassPanel], "MoveClassPanel.updateReferencesCheckBox.AccessibleContext.accessibleDescription")); // NOI18N
  }// </editor-fold>//GEN-END:initComponents

  private def updateReferencesCheckBoxItemStateChanged(evt: java.awt.event.ItemEvent) {//GEN-FIRST:event_updateReferencesCheckBoxItemStateChanged
    parent.stateChanged(null);
  }//GEN-LAST:event_updateReferencesCheckBoxItemStateChanged

    
    
  // ActionListener implementation -------------------------------------------
        
  def actionPerformed(e: ActionEvent) {
    if (projectsComboBox == e.getSource()) {
      project = projectsComboBox.getSelectedItem.asInstanceOf[Project]
      updateRoots
      updatePackages
    } else
      if ( rootComboBox == e.getSource ) {
        updatePackages
      }
    else if ( packageComboBox == e.getSource ) {
    }
  }
    
  // DocumentListener implementation -----------------------------------------
    
  def changedUpdate(e: DocumentEvent) {
    fireChange
  }
    
  def insertUpdate(e: DocumentEvent) {
    fireChange
  }
    
  def removeUpdate(e: DocumentEvent) {
    fireChange
  }
    
  // Private methods ---------------------------------------------------------
        
  private def updatePackages {
    val g = rootComboBox.getSelectedItem.asInstanceOf[SourceGroup]
    packageComboBox.setModel(PackageView.createListView(g))
  }
    
  def setCombosEnabled(enabled: Boolean) {
    packageComboBox.setEnabled(enabled);
    rootComboBox.setEnabled(enabled);
    projectsComboBox.setEnabled(enabled);
    updateReferencesCheckBox.setVisible(!enabled);
    this.setEnabled(enabled);
  }

  def isUpdateReferences: Boolean = {
    if (updateReferencesCheckBox.isVisible && updateReferencesCheckBox.isSelected)
      return false
    return true
  }
    
  private def updateRoots {
    val sources = ProjectUtils.getSources(project);
    groups = sources.getSourceGroups(JavaProjectConstants.SOURCES_TYPE_JAVA);
    if (groups.length == 0) {
      // XXX why?? This is probably wrong. If the project has no Java groups,
      // you cannot move anything into it.
      groups = sources.getSourceGroups( Sources.TYPE_GENERIC );
    }

    var preselectedItem = 0
    for( i <- 0 until groups.length) {
      if (fo != null) {
        try {
          if (groups(i).contains(fo)) {
            preselectedItem = i
          }
        } catch {case e: IllegalArgumentException =>
            // XXX this is a poor abuse of exception handling
        }
      }
    }
                
    // Setup comboboxes
    rootComboBox.setModel(new DefaultComboBoxModel(groups.asInstanceOf[Array[Object]]))
    rootComboBox.setSelectedIndex(preselectedItem)
  }
    
  abstract class BaseCellRenderer extends JLabel with ListCellRenderer with UIResource {
        
    setOpaque(true)
        
    // #89393: GTK needs name to render cell renderer "natively"
    override def getName: String = {
      val name = super.getName
      if (name == null) "ComboBox.renderer" else name  // NOI18N
    }
  }
    
  /** Groups combo renderer, used also in CopyClassPanel */
  class GroupCellRenderer extends BaseCellRenderer {
        
    def getListCellRendererComponent(list: JList,
                                     value: Object,
                                     index: Int,
                                     isSelected: Boolean,
                                     cellHasFocus: Boolean): Component = {
        
      // #89393: GTK needs name to render cell renderer "natively"
      setName("ComboBox.listRenderer") // NOI18N
            
      if (value.isInstanceOf[SourceGroup]) {
        val g = value.asInstanceOf[SourceGroup]
        setText(g.getDisplayName)
        setIcon(g.getIcon(false))
      } else {
        setText("") // NOI18N
        setIcon(null)
      }
            
      if ( isSelected ) {
        setBackground(list.getSelectionBackground)
        setForeground(list.getSelectionForeground)
      }
      else {
        setBackground(list.getBackground)
        setForeground(list.getForeground)
      }
            
      this
    }
  }
    
  /** Projects combo renderer, used also in CopyClassPanel */
  class ProjectCellRenderer extends BaseCellRenderer {
        
    def getListCellRendererComponent(list: JList,
                                     value: Object,
                                     index: Int,
                                     isSelected: Boolean,
                                     cellHasFocus: Boolean): Component = {
        
      // #89393: GTK needs name to render cell renderer "natively"
      setName("ComboBox.listRenderer"); // NOI18N
            
      if ( value != null ) {
        val pi = ProjectUtils.getInformation(value.asInstanceOf[Project]);
        setText(pi.getDisplayName);
        setIcon(pi.getIcon);
      }
            
      if ( isSelected ) {
        setBackground(list.getSelectionBackground());
        setForeground(list.getSelectionForeground());
      }
      else {
        setBackground(list.getBackground());
        setForeground(list.getForeground());
      }
            
      return this;
    }
  }

  def getComponent: Component = {
    this;
  }
}
