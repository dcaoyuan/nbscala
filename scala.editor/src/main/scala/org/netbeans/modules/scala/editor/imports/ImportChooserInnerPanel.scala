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

package org.netbeans.modules.scala.editor.imports

import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JComponent
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.UIManager;
import org.openide.util.NbBundle;


/**
 * JTable with custom renderer, so second column looks editable (JComboBox).
 * Second column also has CellEditor (also a JComboBox).
 *
 * @author  eakle, Martin Roskanin
 * @author  Matthias Schmidt collectified it.
 */
class ImportChooserInnerPanel extends javax.swing.JPanel {
  private var combos: Array[JComboBox] = _
  // Variables declaration - do not modify//GEN-BEGIN:variables
  private var bottomPanel: javax.swing.JPanel = _
  private var contentPanel: javax.swing.JPanel = _
  private var jScrollPane1: javax.swing.JScrollPane = _
  private var lblHeader: javax.swing.JLabel = _
  private var lblTitle: javax.swing.JLabel = _
  // End of variables declaration//GEN-END:variables
    
  initComponents

  def initPanel(multipleCandidates: Map[String, List[ImportCandidate]]) {
    initComponentsMore(multipleCandidates);
    setAccessible
  }
    
  private def initComponentsMore(multipleCandidates: Map[String, List[ImportCandidate]]) {
    contentPanel.setLayout( new GridBagLayout() );
    contentPanel.setBackground( UIManager.getColor("Table.background") ); //NOI18N
    jScrollPane1.setBorder( UIManager.getBorder("ScrollPane.border") ); //NOI18N
    jScrollPane1.getVerticalScrollBar().setUnitIncrement( new JLabel("X").getPreferredSize().height );
    jScrollPane1.getVerticalScrollBar().setBlockIncrement( new JLabel("X").getPreferredSize().height*10 );
        
    val candidateSize = multipleCandidates.size
        
    if( candidateSize > 0 ) {
        
      var row = 0

      combos = new Array[JComboBox](candidateSize)

      val monoSpaced = new Font( "Monospaced", Font.PLAIN, new JLabel().getFont.getSize)
      val focusListener = new FocusListener {
        def focusGained(e: FocusEvent) {
          val c = e.getComponent
          val r = c.getBounds
          contentPanel.scrollRectToVisible(r)
        }
        def focusLost(arg0: FocusEvent) {
        }
      }
            
      var i = 0

      for ((name, importCandidates) <- multipleCandidates) {
        val size = importCandidates.size
        var iNum = 0;
                
        val choices = new Array[String](size)
        val icons = new Array[Icon](size)
        var defaultSelection: String = null;
        var maxImportantsLevel = 0;
                
        for (ImportCandidate(missing, fqn, range, icon, importantsLevel) <- importCandidates) {
          choices(iNum) = fqn
          icons(iNum) = icon
                    
          val level = importantsLevel
                    
          if(level > maxImportantsLevel){
            defaultSelection = choices(iNum)
            maxImportantsLevel = level
          }
                    
          iNum += 1
        }
                
        combos(i) = createComboBox(choices, defaultSelection,
                                   icons, monoSpaced, focusListener)

        val lblSimpleName = new JLabel( name );
        lblSimpleName.setOpaque( false );
        lblSimpleName.setFont( monoSpaced );
        lblSimpleName.setLabelFor( combos(i) );

        contentPanel.add( lblSimpleName, new GridBagConstraints(0,row,1,1,0.0,0.0,GridBagConstraints.WEST,GridBagConstraints.NONE,new Insets(3,5,2,5),0,0) );
        row += 1
        contentPanel.add( combos(i), new GridBagConstraints(1,row,1,1,1.0,0.0,GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,new Insets(3,5,2,5),0,0) );
        i += 1
      }

      contentPanel.add( new JLabel(), new GridBagConstraints(2,row,2,1,0.0,1.0,GridBagConstraints.CENTER,GridBagConstraints.NONE,new Insets(0,0,0,0),0,0) );

      val d = contentPanel.getPreferredSize();
      d.height = getRowHeight * math.min(combos.length, 6)
      jScrollPane1.getViewport.setPreferredSize( d )
    } else {
      contentPanel.add( new JLabel(getBundleString("FixDupImportStmts_NothingToFix")), new GridBagConstraints(0,0,1,1,1.0,1.0,GridBagConstraints.CENTER,GridBagConstraints.NONE,new Insets(20,20,20,20),0,0) );
    }
        
    // load localized text into widgets:
    lblTitle.setText(getBundleString("FixDupImportStmts_IntroLbl")); //NOI18N
    lblHeader.setText(getBundleString("FixDupImportStmts_Header")); //NOI18N

  }
    
  private def createComboBox(choices: Array[String], defaultValue: String, icons: Array[Icon], font: Font, listener: FocusListener): JComboBox = {
    val combo = new JComboBox(choices.asInstanceOf[Array[Object]])
    combo.setSelectedItem(defaultValue);
    combo.getAccessibleContext().setAccessibleDescription(getBundleString("FixDupImportStmts_Combo_ACSD")) //NOI18N
    combo.getAccessibleContext().setAccessibleName(getBundleString("FixDupImportStmts_Combo_Name_ACSD"))   //NOI18N
    combo.setOpaque(false);
    combo.setFont( font );
    combo.addFocusListener( listener );
    combo.setEnabled( choices.length > 1 );
    combo.setRenderer( new DelegatingRenderer(combo.getRenderer, choices, icons))
    val inputMap = combo.getInputMap(JComponent.WHEN_FOCUSED)
    inputMap.put( KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "showPopup") //NOI18N
    combo.getActionMap().put("showPopup", new TogglePopupAction) //NOI18N
    combo
  }
    
  private def getRowHeight: Int = {
    return if (combos.length == 0) 0 else combos(0).getPreferredSize().height+6;
  }
    
  private def getBundleString(s: String): String = {
    NbBundle.getMessage(classOf[ImportChooserInnerPanel], s)
  }
    
    
  private def setAccessible {
    getAccessibleContext().setAccessibleDescription(getBundleString("FixDupImportStmts_IntroLbl")) // NOI18N
  }
    
  def getSelections: Array[String] = {
    combos map {x => x.getSelectedItem.toString}
  }
    
    
  /** This method is called from within the constructor to
   * initialize the form.
   * WARNING: Do NOT modify this code. The content of this method is
   * always regenerated by the Form Editor.
   */
  // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
  private def initComponents {

    lblTitle = new javax.swing.JLabel();
    jScrollPane1 = new javax.swing.JScrollPane();
    contentPanel = new javax.swing.JPanel();
    bottomPanel = new javax.swing.JPanel();
    lblHeader = new javax.swing.JLabel();

    setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 12, 12, 12));
    setPreferredSize(null);
    setLayout(new java.awt.GridBagLayout());

    lblTitle.setText("~Select the fully qualified name to use in the import statement.");
    var gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.weightx = 0.1;
    gridBagConstraints.insets = new java.awt.Insets(0, 0, 6, 0);
    add(lblTitle, gridBagConstraints);

    jScrollPane1.setBorder(null);

    contentPanel.setLayout(new java.awt.GridBagLayout());
    jScrollPane1.setViewportView(contentPanel);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    add(jScrollPane1, gridBagConstraints);

    bottomPanel.setLayout(new java.awt.BorderLayout());
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.weightx = 1.0;
    add(bottomPanel, gridBagConstraints);

    lblHeader.setText("~Import Statements:");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(3, 0, 3, 0);
    add(lblHeader, gridBagConstraints);
  }// </editor-fold>//GEN-END:initComponents
    
    
    
  private class DelegatingRenderer(orig: ListCellRenderer, values: Array[String], icons: Array[Icon]) extends ListCellRenderer {

    def getListCellRendererComponent(list: JList, value: Object, index: Int, isSelected: Boolean, cellHasFocus: Boolean): Component = {
      val res = orig.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
      if (res.isInstanceOf[JLabel] && null != icons ) {
        var i = 0
        var break = false
        while (i < values.length && !break) {
          if(values(i).equals(value)) {
            res.asInstanceOf[JLabel].setIcon(icons(i))
            break = true
          }
          i += 1
        }
      }
      res
    }
  }
    
  private class TogglePopupAction extends AbstractAction {
    def actionPerformed(e: ActionEvent) {
      e.getSource match {
        case combo: JComboBox => combo.setPopupVisible( !combo.isPopupVisible)
        case _ =>
      }
    }
  }
}
