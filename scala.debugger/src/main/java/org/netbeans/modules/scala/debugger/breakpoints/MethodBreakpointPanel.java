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

package org.netbeans.modules.scala.debugger.breakpoints;

import java.awt.Dimension;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import org.netbeans.api.debugger.DebuggerManager;
import org.netbeans.api.debugger.Breakpoint.HIT_COUNT_FILTERING_STYLE;
import org.netbeans.api.debugger.jpda.MethodBreakpoint;
import org.netbeans.modules.scala.debugger.EditorContextBridge;
import org.netbeans.modules.scala.debugger.WatchPanel;
import org.netbeans.spi.debugger.ui.Controller;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.ErrorManager;
import org.openide.util.NbBundle;

/**
 * @author  Jan Jancura
 */
// <RAVE>
// Implement HelpCtx.Provider interface to provide help ids for help system
// public class MethodBreakpointPanel extends JPanel implements Controller {
// ====
public class MethodBreakpointPanel extends JPanel implements Controller, org.openide.util.HelpCtx.Provider {
// </RAVE>
    
    private ConditionsPanel             conditionsPanel;
    private ActionsPanel                actionsPanel; 
    private MethodBreakpoint            breakpoint;
    private boolean                     createBreakpoint = false;
    private JEditorPane                 tfClassName;
    
    
    private static MethodBreakpoint createBreakpoint () {
        String className;
        try {
            className = EditorContextBridge.getContext().getCurrentClassName();
        } catch (java.awt.IllegalComponentStateException icsex) {
            className = "";
        }
        String methodName;
        try {
            methodName = EditorContextBridge.getContext().getCurrentMethodName();
        } catch (java.awt.IllegalComponentStateException icsex) {
            methodName = "";
        }
        MethodBreakpoint mb = MethodBreakpoint.create (
            className,
            methodName
        );
        try {
            mb.setMethodSignature(EditorContextBridge.getCurrentMethodSignature());
        } catch (java.awt.IllegalComponentStateException icsex) {}
        mb.setPrintText (
            NbBundle.getBundle (MethodBreakpointPanel.class).getString 
                ("CTL_Method_Breakpoint_Print_Text")
        );
        return mb;
    }
    
    
    /** Creates new form LineBreakpointPanel */
    public MethodBreakpointPanel () {
        this (createBreakpoint ());
        createBreakpoint = true;
    }
    
    /** Creates new form LineBreakpointPanel */
    public MethodBreakpointPanel (MethodBreakpoint b) {
        breakpoint = b;
        initComponents ();
        
        String className = "";
        String[] cf = b.getClassFilters ();
        className = ClassBreakpointPanel.concatClassFilters(cf);
        tfClassName = new JEditorPane("text/x-scala", className);
        tfClassName.getAccessibleContext().setAccessibleName(NbBundle.getMessage(MethodBreakpointPanel.class, "ACSN_Method_Breakpoint_ClassName"));
        tfClassName.getAccessibleContext().setAccessibleDescription(NbBundle.getMessage(MethodBreakpointPanel.class, "ACSD_Method_Breakpoint_ClassName"));
        jLabel3.setLabelFor(tfClassName);
        panelClassName.add(java.awt.BorderLayout.CENTER, WatchPanel.createScrollableLineEditor(tfClassName));
        if ("".equals (b.getMethodName ())) {
            tfMethodName.setText (org.openide.util.NbBundle.getMessage(MethodBreakpointPanel.class, "Method_Breakpoint_ALL_METHODS"));
            cbAllMethods.setSelected (true);
            tfMethodName.setEnabled (false);
        } else {
            tfMethodName.setText (b.getMethodName () + " " + createParamTypesFromSignature(b.getMethodSignature()));
        }
        cbBreakpointType.addItem (NbBundle.getMessage(MethodBreakpointPanel.class, "LBL_Method_Breakpoint_Type_Entry"));
        cbBreakpointType.addItem (NbBundle.getMessage(MethodBreakpointPanel.class, "LBL_Method_Breakpoint_Type_Exit"));
        cbBreakpointType.addItem (NbBundle.getMessage(MethodBreakpointPanel.class, "LBL_Method_Breakpoint_Type_Entry_or_Exit"));
        switch (b.getBreakpointType ()) {
            case MethodBreakpoint.TYPE_METHOD_ENTRY:
                cbBreakpointType.setSelectedIndex (0);
                break;
            case MethodBreakpoint.TYPE_METHOD_EXIT:
                cbBreakpointType.setSelectedIndex (1);
                break;
            case (MethodBreakpoint.TYPE_METHOD_ENTRY | MethodBreakpoint.TYPE_METHOD_EXIT):
                cbBreakpointType.setSelectedIndex (2);
                break;
        }
        
        conditionsPanel = new ConditionsPanel();
        conditionsPanel.showClassFilter(false);
        conditionsPanel.setCondition(b.getCondition());
        conditionsPanel.setHitCountFilteringStyle(b.getHitCountFilteringStyle());
        conditionsPanel.setHitCount(b.getHitCountFilter());
        cPanel.add(conditionsPanel, "Center");
        
        actionsPanel = new ActionsPanel (b);
        pActions.add (actionsPanel, "Center");
        // <RAVE>
        // The help IDs for the AddBreakpointPanel panels have to be different from the
        // values returned by getHelpCtx() because they provide different help
        // in the 'Add Breakpoint' dialog and when invoked in the 'Breakpoints' view
        putClientProperty("HelpID_AddBreakpointPanel", "debug.add.breakpoint.java.method"); // NOI18N
        // </RAVE>
    }
    
    /** @return comma-separated parameter types */
    private static String createParamTypesFromSignature(String signature) {
        if (signature == null || signature.length() == 0) return "";
        int end = signature.lastIndexOf(")");
        if (end < 0) {
            ErrorManager.getDefault().notify(new IllegalArgumentException("Bad signature: "+signature));
            return "";
        }
        StringBuilder paramTypes = new StringBuilder("(");
        int[] s = new int[] { 1 }; // skipping the opening '('
        while (s[0] < signature.length()) {
            if (signature.charAt(s[0]) == ')') {
                break; // We're done
            }
            if (s[0] > 1) paramTypes.append(',');
            paramTypes.append(getType(signature, s));
        }
        paramTypes.append(')');
        return paramTypes.toString();
    }
    
    /** @param paramTypes comma-separated parameter types */
    private static String createSignatureFromParamTypes(String paramTypes) {
        StringBuilder signature = new StringBuilder("(");
        int s = 0;
        int e;
        while (s < paramTypes.length()) {
            e = paramTypes.indexOf(',', s);
            if (e < 0) {
                e = paramTypes.length();
            }
            String type = paramTypes.substring(s, e);
            signature.append(getSignature(type.trim()));
            s = e + 1;
        }
        signature.append(')');
        // ignoring return type
        return signature.toString();
    }
    
    private static String getSignature(String javaType) {
        if (javaType.equals("boolean")) {
            return "Z";
        } else if (javaType.equals("byte")) {
            return "B";
        } else if (javaType.equals("char")) {
            return "C";
        } else if (javaType.equals("short")) {
            return "S";
        } else if (javaType.equals("int")) {
            return "I";
        } else if (javaType.equals("long")) {
            return "J";
        } else if (javaType.equals("float")) {
            return "F";
        } else if (javaType.equals("double")) {
            return "D";
        } else if (javaType.endsWith("[]")) {
            return "["+getSignature(javaType.substring(0, javaType.length() - 2));
        } else {
            return "L"+javaType.replace('.', '/')+";";
        }
    }
    
    private static String getType(String signature, int[] pos) throws IllegalArgumentException {
        char c = signature.charAt(pos[0]);
        if (c == 'Z') {
            pos[0]++;
            return "boolean";
        }
        if (c == 'B') {
            pos[0]++;
            return "byte";
        }
        if (c == 'C') {
            pos[0]++;
            return "char";
        }
        if (c == 'S') {
            pos[0]++;
            return "short";
        }
        if (c == 'I') {
            pos[0]++;
            return "int";
        }
        if (c == 'J') {
            pos[0]++;
            return "long";
        }
        if (c == 'F') {
            pos[0]++;
            return "float";
        }
        if (c == 'D') {
            pos[0]++;
            return "double";
        }
        if (c == 'L') {
            pos[0]++;
            int typeEnd = signature.indexOf(";", pos[0]);
            if (typeEnd < 0) {
                throw new IllegalArgumentException("Bad signature: '"+signature+"', 'L' not followed by ';' at position "+pos[0]);
            }
            String type = signature.substring(pos[0], typeEnd);
            type = type.replace('/', '.');
            pos[0] = typeEnd + 1;
            return type;
        }
        if (c == '[') {
            pos[0]++;
            String type = getType(signature, pos);
            return type + "[]";
        }
        throw new IllegalArgumentException("Bad signature: '"+signature+"', unrecognized element '"+c+"' at position "+pos[0]);
    }
    
    // <RAVE>
    // Implement getHelpCtx() with the correct helpID
    public org.openide.util.HelpCtx getHelpCtx() {
        return new org.openide.util.HelpCtx("NetbeansDebuggerBreakpointMethodJPDA"); // NOI18N
    }
    // </RAVE>
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        pSettings = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        cbAllMethods = new javax.swing.JCheckBox();
        jLabel1 = new javax.swing.JLabel();
        tfMethodName = new javax.swing.JTextField();
        panelClassName = new javax.swing.JPanel();
        stopOnLabel = new javax.swing.JLabel();
        cbBreakpointType = new javax.swing.JComboBox();
        cPanel = new javax.swing.JPanel();
        pActions = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();

        setLayout(new java.awt.GridBagLayout());

        pSettings.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(MethodBreakpointPanel.class, "L_Method_Breakpoint_BorderTitle"))); // NOI18N
        pSettings.setLayout(new java.awt.GridBagLayout());

        org.openide.awt.Mnemonics.setLocalizedText(jLabel3, org.openide.util.NbBundle.getMessage(MethodBreakpointPanel.class, "L_Method_Breakpoint_Class_Name")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(3, 3, 3, 3);
        pSettings.add(jLabel3, gridBagConstraints);
        jLabel3.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(MethodBreakpointPanel.class, "ACSD_L_Method_Breakpoint_Class_Name")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(cbAllMethods, org.openide.util.NbBundle.getMessage(MethodBreakpointPanel.class, "CB_Method_Breakpoint_All_Methods")); // NOI18N
        cbAllMethods.setToolTipText(org.openide.util.NbBundle.getMessage(MethodBreakpointPanel.class, "TTT_CB_Method_Breakpoint_All_Methods")); // NOI18N
        cbAllMethods.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        cbAllMethods.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cbAllMethodsActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(3, 3, 3, 3);
        pSettings.add(cbAllMethods, gridBagConstraints);
        cbAllMethods.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(MethodBreakpointPanel.class, "ACSD_CB_Method_Breakpoint_All_Methods")); // NOI18N

        jLabel1.setLabelFor(tfMethodName);
        org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(MethodBreakpointPanel.class, "L_Method_Breakpoint_Method_Name")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(3, 3, 3, 3);
        pSettings.add(jLabel1, gridBagConstraints);
        jLabel1.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(MethodBreakpointPanel.class, "ACSD_L_Method_Breakpoint_Method_Name")); // NOI18N

        tfMethodName.setToolTipText(org.openide.util.NbBundle.getMessage(MethodBreakpointPanel.class, "TTT_TF_Method_Breakpoint_Method_Name")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(3, 3, 3, 3);
        pSettings.add(tfMethodName, gridBagConstraints);
        tfMethodName.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(MethodBreakpointPanel.class, "ACSD_TF_Method_Breakpoint_Method_Name")); // NOI18N

        panelClassName.setLayout(new java.awt.BorderLayout());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(3, 3, 3, 3);
        pSettings.add(panelClassName, gridBagConstraints);

        stopOnLabel.setLabelFor(cbBreakpointType);
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("org/netbeans/modules/debugger/jpda/ui/breakpoints/Bundle"); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(stopOnLabel, bundle.getString("L_Method_Breakpoint_Type")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(3, 3, 3, 3);
        pSettings.add(stopOnLabel, gridBagConstraints);
        stopOnLabel.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(MethodBreakpointPanel.class, "ACSD_StopOn_LBL")); // NOI18N

        cbBreakpointType.setToolTipText(bundle.getString("TTT_CB_Class_Breakpoint_Type")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(3, 3, 3, 3);
        pSettings.add(cbBreakpointType, gridBagConstraints);
        cbBreakpointType.getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getMessage(MethodBreakpointPanel.class, "ACSN_CB_Method_Breakpoint_Type")); // NOI18N

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        add(pSettings, gridBagConstraints);

        cPanel.setLayout(new java.awt.BorderLayout());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        add(cPanel, gridBagConstraints);

        pActions.setLayout(new java.awt.BorderLayout());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        add(pActions, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        add(jPanel1, gridBagConstraints);

        getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getMessage(MethodBreakpointPanel.class, "ACSN_MethodBreakpointPanel")); // NOI18N
    }// </editor-fold>//GEN-END:initComponents

    private void cbAllMethodsActionPerformed (java.awt.event.ActionEvent evt)//GEN-FIRST:event_cbAllMethodsActionPerformed
    {//GEN-HEADEREND:event_cbAllMethodsActionPerformed
        if (cbAllMethods.isSelected ()) {
            tfMethodName.setText (org.openide.util.NbBundle.getMessage(MethodBreakpointPanel.class, "Method_Breakpoint_ALL_METHODS"));
            tfMethodName.setEnabled (false);
        } else {
            tfMethodName.setText ("");
            tfMethodName.setEnabled (true);
        }
    }//GEN-LAST:event_cbAllMethodsActionPerformed

    
    // Controller implementation ...............................................
    
    /**
     * Called when "Ok" button is pressed.
     *
     * @return whether customizer can be closed
     */
    public boolean ok () {
        String msg = valiadateMsg();
        if (msg == null) {
            msg = conditionsPanel.valiadateMsg();
        }
        if (msg != null) {
            DialogDisplayer.getDefault().notify(new NotifyDescriptor.Message(msg));
            return false;
        }
        actionsPanel.ok ();
        //String className = ((String) tfPackageName.getText ()).trim ();
        //if (className.length () > 0)
        //    className += '.';
        String className = tfClassName.getText ().trim ();
        breakpoint.setClassFilters (new String[] {className});
        if (!cbAllMethods.isSelected ()) {
            String methodAndSignature = tfMethodName.getText ().trim ();
            String methodName;
            String signature;
            int index = methodAndSignature.indexOf("(");
            if (index < 0) {
                methodName = methodAndSignature;
                signature = null;
            } else {
                methodName = methodAndSignature.substring(0, index).trim();
                int end = methodAndSignature.indexOf(")", index);
                if (end < 0) {
                    end = methodAndSignature.length();
                }
                signature = methodAndSignature.substring(index + 1, end);
                signature = createSignatureFromParamTypes(signature);
            }
            breakpoint.setMethodName (methodName);
            breakpoint.setMethodSignature(signature);
        } else {
            breakpoint.setMethodName ("");
        }
        switch (cbBreakpointType.getSelectedIndex ()) {
            case 0:
                breakpoint.setBreakpointType (MethodBreakpoint.TYPE_METHOD_ENTRY);
                break;
            case 1:
                breakpoint.setBreakpointType (MethodBreakpoint.TYPE_METHOD_EXIT);
                break;
            case 2:
                breakpoint.setBreakpointType (MethodBreakpoint.TYPE_METHOD_ENTRY | MethodBreakpoint.TYPE_METHOD_EXIT);
                break;
        }
        breakpoint.setCondition (conditionsPanel.getCondition());
        breakpoint.setHitCountFilter(conditionsPanel.getHitCount(),
                conditionsPanel.getHitCountFilteringStyle());
        
        if (createBreakpoint) 
            DebuggerManager.getDebuggerManager ().addBreakpoint (breakpoint);
        return true;
    }
    
    /**
     * Called when "Cancel" button is pressed.
     *
     * @return whether customizer can be closed
     */
    public boolean cancel () {
        return true;
    }
    
    /**
     * Return <code>true</code> whether value of this customizer 
     * is valid (and OK button can be enabled).
     *
     * @return <code>true</code> whether value of this customizer 
     * is valid
     */
    public boolean isValid () {
        return true;
    }
    
    private String valiadateMsg () {
        if (tfClassName.getText().trim ().length() == 0 || (tfMethodName.getText().trim ().length() == 0 && !cbAllMethods.isSelected())) {
            return NbBundle.getMessage(MethodBreakpointPanel.class, "MSG_No_Class_or_Method_Name_Spec");
        }
        return null;
    }
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel cPanel;
    private javax.swing.JCheckBox cbAllMethods;
    private javax.swing.JComboBox cbBreakpointType;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel pActions;
    private javax.swing.JPanel pSettings;
    private javax.swing.JPanel panelClassName;
    private javax.swing.JLabel stopOnLabel;
    private javax.swing.JTextField tfMethodName;
    // End of variables declaration//GEN-END:variables

    /*
    public static class ClassEditorKit extends NbEditorKit {
        
        static final String MIME_TYPE = "text/x-nb-debugger-jpda-class"; // NOI18N
        
        public ClassEditorKit() {
            //updateActions();
        }
        
        @Override
        public String getContentType() {
            return MIME_TYPE;
        }

    }
     */
}
