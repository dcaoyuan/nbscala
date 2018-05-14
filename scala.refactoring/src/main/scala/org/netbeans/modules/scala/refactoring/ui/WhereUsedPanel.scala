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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ItemEvent;
import java.text.MessageFormat;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JList;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.plaf.UIResource;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeListener;
import org.netbeans.api.project.FileOwnerQuery
import org.netbeans.api.project.ProjectUtils
import org.netbeans.modules.csl.api.{ ElementKind, Modifier }
import org.netbeans.modules.parsing.spi.ParseException
import org.netbeans.modules.parsing.api.ParserManager
import org.netbeans.modules.parsing.api.ResultIterator
import org.netbeans.modules.parsing.api.Source
import org.netbeans.modules.parsing.api.UserTask
import org.netbeans.modules.refactoring.spi.ui.CustomRefactoringPanel
import org.openide.awt.Mnemonics
import org.openide.util.NbBundle

import org.netbeans.modules.scala.core.ScalaParserResult
import org.netbeans.modules.scala.core.ast.ScalaItems
import org.netbeans.modules.scala.refactoring.RefactoringModule
import scala.reflect.internal.Flags

import javax.swing.GroupLayout
import javax.swing.LayoutStyle

/**
 * @author  Jan Becicka
 */
object WhereUsedPanel {
  abstract class Scope
  object Scope {
    case object ALL extends Scope
    case object CURRENT extends Scope
  }
}

class WhereUsedPanel(name: String, element: ScalaItems#ScalaItem, parent: ChangeListener) extends JPanel with CustomRefactoringPanel {
  import WhereUsedPanel._

  private val MAX_NAME = 50

  // Variables declaration - do not modify//GEN-BEGIN:variables
  private var buttonGroup: javax.swing.ButtonGroup = _
  private var c_directOnly: javax.swing.JRadioButton = _
  private var c_subclasses: javax.swing.JRadioButton = _
  private var c_usages: javax.swing.JRadioButton = _
  private var classesPanel: javax.swing.JPanel = _
  private var commentsPanel: javax.swing.JPanel = _
  private var jPanel1: javax.swing.JPanel = _
  private var jPanel2: javax.swing.JPanel = _
  private var label: javax.swing.JLabel = _
  private var m_isBaseClass: javax.swing.JCheckBox = _
  private var m_overriders: javax.swing.JCheckBox = _
  private var m_usages: javax.swing.JCheckBox = _
  private var methodsPanel: javax.swing.JPanel = _
  private var scope: javax.swing.JComboBox[JLabel] = _
  private var scopeLabel: javax.swing.JLabel = _
  private var scopePanel: javax.swing.JPanel = _
  private var searchInComments: javax.swing.JCheckBox = _
  // End of variables declaration//GEN-END:variables

  private var newElement: ScalaItems#ScalaItem = _

  setName(NbBundle.getMessage(classOf[WhereUsedPanel], "LBL_WhereUsed"));
  initComponents

  def getScope: Scope = {
    if (scope.getSelectedIndex == 1) Scope.CURRENT else Scope.ALL
  }

  private var initialized = false
  private var methodDeclaringSuperClass: String = null
  private var methodDeclaringClass: String = null

  def getMethodDeclaringClass: String = {
    if (isMethodFromBaseClass) methodDeclaringSuperClass else methodDeclaringClass
  }

  def initialize {
    if (initialized) return
    val fo = element.fo.get
    val source = Source.create(fo)
    val p = FileOwnerQuery.getOwner(fo)
    val (currentProject, allProjects) =
      if (p ne null) {
        val pi = ProjectUtils.getInformation(FileOwnerQuery.getOwner(fo))
        (new JLabel(pi.getDisplayName(), pi.getIcon(), SwingConstants.LEFT),
          new JLabel(NbBundle.getMessage(classOf[WhereUsedPanel], "LBL_AllProjects"), pi.getIcon(), SwingConstants.LEFT))
      } else (null, null)
    try {
      ParserManager.parse(java.util.Collections.singleton(source), new UserTask {
        @throws(classOf[Exception])
        override def run(ri: ResultIterator) {
          val pr = ri.getParserResult.asInstanceOf[ScalaParserResult]
          val global = pr.global

          var m_isBaseClassText: String = null
          var labelText: String = null
          var modif = java.util.Collections.emptySet[Modifier]

          global.askForResponse { () =>
            import global._

            val sName = element.symbol.nameString
            val clzName = element.symbol.enclClass.nameString

            element.kind match {
              case ElementKind.METHOD | ElementKind.CALL =>
                modif = element.getModifiers
                methodDeclaringClass = clzName
                labelText = NbBundle.getMessage(classOf[WhereUsedPanel], "DSC_MethodUsages", element.symbol.nameString, methodDeclaringClass) // NOI18N

                val overridens = element.symbol.allOverriddenSymbols
                if (!overridens.isEmpty) {
                  val overriden = overridens.head
                  m_isBaseClassText = new MessageFormat(NbBundle.getMessage(classOf[WhereUsedPanel], "LBL_UsagesOfBaseClass")).format(
                    Array(methodDeclaringSuperClass).asInstanceOf[Array[Object]])
                  newElement = ScalaElement(overriden.asInstanceOf[Symbol], pr)
                }
              case ElementKind.CLASS | ElementKind.MODULE =>
                labelText = NbBundle.getMessage(classOf[WhereUsedPanel], "DSC_ClassUsages", sName) // NOI18N
              case ElementKind.CONSTRUCTOR =>
                labelText = NbBundle.getMessage(classOf[WhereUsedPanel], "DSC_ConstructorUsages", sName, clzName) // NOI18N
              case ElementKind.FIELD =>
                labelText = NbBundle.getMessage(classOf[WhereUsedPanel], "DSC_FieldUsages", sName, clzName) // NOI18N
              case _ =>
                labelText = NbBundle.getMessage(classOf[WhereUsedPanel], "DSC_VariableUsages", sName) // NOI18N
            }

          } get

          val modifiers = modif
          val isBaseClassText = m_isBaseClassText

          SwingUtilities.invokeLater(new Runnable {
            def run {
              remove(classesPanel)
              remove(methodsPanel)
              // WARNING for now since this feature is not ready yet
              //label.setText(labelText);
              val combinedLabelText = NbBundle.getMessage(classOf[WhereUsedPanel], "DSC_WhereUsedWarningInDevelopment", labelText)
              label.setText(combinedLabelText)

              global.askForResponse { () =>
                //import global._

                element.kind match {
                  case ElementKind.METHOD =>
                    add(methodsPanel, BorderLayout.CENTER)
                    methodsPanel.setVisible(true)
                    m_usages.setVisible(!modifiers.contains(Modifier.STATIC))
                    val enclClass = element.symbol.enclClass
                    m_overriders.setVisible(!(enclClass.hasFlag(Flags.STATIC) ||
                      enclClass.hasFlag(Flags.FINAL) ||
                      modifiers.contains(Modifier.STATIC) ||
                      modifiers.contains(Modifier.PRIVATE)))
                    if (methodDeclaringSuperClass ne null) {
                      m_isBaseClass.setVisible(true)
                      m_isBaseClass.setSelected(true)
                      Mnemonics.setLocalizedText(m_isBaseClass, isBaseClassText)
                    } else {
                      m_isBaseClass.setVisible(false)
                      m_isBaseClass.setSelected(false)
                    }
                  case ElementKind.CLASS | ElementKind.MODULE =>
                    add(classesPanel, BorderLayout.CENTER)
                    classesPanel.setVisible(true)
                  case _ =>
                    remove(classesPanel)
                    remove(methodsPanel)
                    c_subclasses.setVisible(false)
                    m_usages.setVisible(false)
                    c_usages.setVisible(false)
                    c_directOnly.setVisible(false)
                }
              } get

              if (currentProject ne null) {
                scope.setModel(new DefaultComboBoxModel(Array(allProjects, currentProject).asInstanceOf[Array[JLabel]]))
                val defaultItem = RefactoringModule.getOption("whereUsed.scope", 0).toInt // NOI18N
                scope.setSelectedIndex(defaultItem)
                scope.setRenderer(new JLabelRenderer)
              } else {
                scopePanel.setVisible(false)
              }
              validate
            }
          })
        }
      })
    } catch { case ex: ParseException => throw (new RuntimeException).initCause(ex) }

    initialized = true
  }

  private class JLabelRenderer extends JLabel with ListCellRenderer[JLabel] with UIResource {
    setOpaque(true)
    def getListCellRendererComponent(list: JList[_ <: JLabel],
                                     value: JLabel,
                                     index: Int,
                                     isSelected: Boolean,
                                     cellHasFocus: Boolean): Component = {

      // #89393: GTK needs name to render cell renderer "natively"
      setName("ComboBox.listRenderer") // NOI18N

      if (value ne null) {
        setText(value.getText)
        setIcon(value.getIcon)
      }

      if (isSelected) {
        setBackground(list.getSelectionBackground)
        setForeground(list.getSelectionForeground)
      } else {
        setBackground(list.getBackground)
        setForeground(list.getForeground)
      }

      this
    }

    // #89393: GTK needs name to render cell renderer "natively"
    override def getName: String = {
      super.getName match {
        case null => "ComboBox.renderer" // NOI18N
        case x    => x
      }
    }
  }

  def getBaseMethod: ScalaItems#ScalaItem = newElement

  override def requestFocus {
    super.requestFocus
  }

  /**
   * This method is called from within the constructor to
   * initialize the form.
   * WARNING: Do NOT modify this code. The content of this method is
   * always regenerated by the Form Editor.
   */
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private def initComponents {
    var gridBagConstraints: java.awt.GridBagConstraints = null

    buttonGroup = new javax.swing.ButtonGroup();
    methodsPanel = new javax.swing.JPanel();
    m_isBaseClass = new javax.swing.JCheckBox();
    jPanel1 = new javax.swing.JPanel();
    m_overriders = new javax.swing.JCheckBox();
    m_usages = new javax.swing.JCheckBox();
    classesPanel = new javax.swing.JPanel();
    jPanel2 = new javax.swing.JPanel();
    c_subclasses = new javax.swing.JRadioButton();
    c_usages = new javax.swing.JRadioButton();
    c_directOnly = new javax.swing.JRadioButton();
    commentsPanel = new javax.swing.JPanel();
    label = new javax.swing.JLabel();
    searchInComments = new javax.swing.JCheckBox();
    scopePanel = new javax.swing.JPanel();
    scopeLabel = new javax.swing.JLabel();
    scope = new javax.swing.JComboBox();

    setLayout(new java.awt.BorderLayout());

    methodsPanel.setLayout(new java.awt.GridBagLayout());

    m_isBaseClass.setSelected(true);
    m_isBaseClass.addActionListener(new java.awt.event.ActionListener() {
      def actionPerformed(evt: java.awt.event.ActionEvent) {
        m_isBaseClassActionPerformed(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints()
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST
    gridBagConstraints.insets = new java.awt.Insets(0, 12, 0, 0)
    methodsPanel.add(m_isBaseClass, gridBagConstraints)
    val bundle = java.util.ResourceBundle.getBundle("org/netbeans/modules/scala/refactoring/ui/Bundle") // NOI18N
    m_isBaseClass.getAccessibleContext().setAccessibleDescription(bundle.getString("ACSD_isBaseClass")) // NOI18N

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    methodsPanel.add(jPanel1, gridBagConstraints);

    org.openide.awt.Mnemonics.setLocalizedText(m_overriders, org.openide.util.NbBundle.getMessage(classOf[WhereUsedPanel], "LBL_FindOverridingMethods")); // NOI18N
    m_overriders.addActionListener(new java.awt.event.ActionListener() {
      def actionPerformed(evt: java.awt.event.ActionEvent) {
        m_overridersActionPerformed(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(0, 12, 0, 0);
    methodsPanel.add(m_overriders, gridBagConstraints);
    m_overriders.getAccessibleContext().setAccessibleDescription(bundle.getString("ACSD_overriders")); // NOI18N

    m_usages.setSelected(true);
    org.openide.awt.Mnemonics.setLocalizedText(m_usages, org.openide.util.NbBundle.getMessage(classOf[WhereUsedPanel], "LBL_FindUsages")); // NOI18N
    m_usages.setMargin(new java.awt.Insets(10, 2, 2, 2));
    m_usages.addActionListener(new java.awt.event.ActionListener() {
      def actionPerformed(evt: java.awt.event.ActionEvent) {
        m_usagesActionPerformed(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(0, 12, 0, 0);
    methodsPanel.add(m_usages, gridBagConstraints);
    m_usages.getAccessibleContext().setAccessibleDescription(bundle.getString("ACSD_usages")); // NOI18N

    add(methodsPanel, java.awt.BorderLayout.CENTER);

    classesPanel.setLayout(new java.awt.GridBagLayout());
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    classesPanel.add(jPanel2, gridBagConstraints);

    buttonGroup.add(c_subclasses);
    org.openide.awt.Mnemonics.setLocalizedText(c_subclasses, org.openide.util.NbBundle.getMessage(classOf[WhereUsedPanel], "LBL_FindAllSubtypes")); // NOI18N
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(0, 12, 0, 0);
    classesPanel.add(c_subclasses, gridBagConstraints);
    c_subclasses.getAccessibleContext().setAccessibleDescription(bundle.getString("ACSD_subclasses")); // NOI18N

    buttonGroup.add(c_usages);
    c_usages.setSelected(true);
    org.openide.awt.Mnemonics.setLocalizedText(c_usages, org.openide.util.NbBundle.getMessage(classOf[WhereUsedPanel], "LBL_FindUsages")); // NOI18N
    c_usages.setMargin(new java.awt.Insets(4, 2, 2, 2));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(0, 12, 0, 0);
    classesPanel.add(c_usages, gridBagConstraints);
    c_usages.getAccessibleContext().setAccessibleDescription(bundle.getString("ACSD_usages")); // NOI18N

    buttonGroup.add(c_directOnly);
    org.openide.awt.Mnemonics.setLocalizedText(c_directOnly, org.openide.util.NbBundle.getMessage(classOf[WhereUsedPanel], "LBL_FindDirectSubtypesOnly")); // NOI18N
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(0, 12, 0, 0);
    classesPanel.add(c_directOnly, gridBagConstraints);
    c_directOnly.getAccessibleContext().setAccessibleDescription(bundle.getString("ACSD_directOnly")); // NOI18N

    add(classesPanel, java.awt.BorderLayout.CENTER);

    commentsPanel.setLayout(new java.awt.BorderLayout());
    commentsPanel.add(label, java.awt.BorderLayout.NORTH);

    searchInComments.setSelected((RefactoringModule.getOption("searchInComments.whereUsed", false)).booleanValue());
    org.openide.awt.Mnemonics.setLocalizedText(searchInComments, org.openide.util.NbBundle.getBundle(classOf[WhereUsedPanel]).getString("LBL_SearchInComents")); // NOI18N
    searchInComments.setMargin(new java.awt.Insets(10, 14, 2, 2));
    searchInComments.addItemListener(new java.awt.event.ItemListener() {
      def itemStateChanged(evt: java.awt.event.ItemEvent) {
        searchInCommentsItemStateChanged(evt);
      }
    });
    commentsPanel.add(searchInComments, java.awt.BorderLayout.SOUTH);
    searchInComments.getAccessibleContext().setAccessibleDescription(searchInComments.getText());

    add(commentsPanel, java.awt.BorderLayout.NORTH);

    scopeLabel.setLabelFor(scope);
    org.openide.awt.Mnemonics.setLocalizedText(scopeLabel, org.openide.util.NbBundle.getMessage(classOf[WhereUsedPanel], "LBL_Scope")); // NOI18N

    scope.addActionListener(new java.awt.event.ActionListener() {
      def actionPerformed(evt: java.awt.event.ActionEvent) {
        scopeActionPerformed(evt);
      }
    });

    val scopePanelLayout = new GroupLayout(scopePanel);
    scopePanel.setLayout(scopePanelLayout);
    scopePanelLayout.setHorizontalGroup(
      scopePanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
        .addGroup(scopePanelLayout.createSequentialGroup
          .addContainerGap
          .addComponent(scopeLabel)
          .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
          .addComponent(scope, 0, 266, Short.MaxValue)
          .addContainerGap));
    scopePanelLayout.setVerticalGroup(
      scopePanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
        .addComponent(scopeLabel)
        .addComponent(scope, GroupLayout.PREFERRED_SIZE, 20, Short.MaxValue));

    scope.getAccessibleContext().setAccessibleDescription("N/A");

    add(scopePanel, java.awt.BorderLayout.PAGE_END);
  } // </editor-fold>//GEN-END:initComponents

  private def scopeActionPerformed(evt: java.awt.event.ActionEvent) { //GEN-FIRST:event_scopeActionPerformed
    RefactoringModule.setOption("whereUsed.scope", scope.getSelectedIndex()); // NOI18N
  } //GEN-LAST:event_scopeActionPerformed

  private def searchInCommentsItemStateChanged(evt: java.awt.event.ItemEvent) { //GEN-FIRST:event_searchInCommentsItemStateChanged
    // used for change default value for searchInComments check-box.
    // The value is persisted and then used as default in next IDE run.
    val b = if (evt.getStateChange == ItemEvent.SELECTED) true else false
    RefactoringModule.setOption("searchInComments.whereUsed", b) // NOI18N
  } //GEN-LAST:event_searchInCommentsItemStateChanged

  private def m_isBaseClassActionPerformed(evt: java.awt.event.ActionEvent) { //GEN-FIRST:event_m_isBaseClassActionPerformed
    parent.stateChanged(null)
  } //GEN-LAST:event_m_isBaseClassActionPerformed

  private def m_overridersActionPerformed(evt: java.awt.event.ActionEvent) { //GEN-FIRST:event_m_overridersActionPerformed
    parent.stateChanged(null)
  } //GEN-LAST:event_m_overridersActionPerformed

  private def m_usagesActionPerformed(evt: java.awt.event.ActionEvent) { //GEN-FIRST:event_m_usagesActionPerformed
    parent.stateChanged(null)
  } //GEN-LAST:event_m_usagesActionPerformed

  def isMethodFromBaseClass: Boolean = {
    m_isBaseClass.isSelected
  }

  def isMethodOverriders: Boolean = {
    m_overriders.isSelected
  }

  def isClassSubTypes: Boolean = {
    c_subclasses.isSelected
  }

  def isClassSubTypesDirectOnly: Boolean = {
    c_directOnly.isSelected
  }

  def isMethodFindUsages: Boolean = {
    m_usages.isSelected
  }

  def isClassFindUsages: Boolean = {
    c_usages.isSelected
  }

  //    public Dimension getPreferredSize() {
  //        Dimension orig = super.getPreferredSize();
  //        return new Dimension(orig.width + 30 , orig.height + 30);
  //    }

  def isSearchInComments: Boolean = {
    searchInComments.isSelected
  }

  def getComponent: Component = this
}

