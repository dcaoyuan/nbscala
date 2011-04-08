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

package org.netbeans.modules.scala.debugger.projects;

import java.io.IOException;
import java.util.List;
import javax.swing.JEditorPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.StyledDocument;

import org.openide.cookies.EditorCookie;
import org.openide.loaders.DataObject;
import org.openide.text.Annotation;
import org.openide.text.DataEditorSupport;
import org.openide.text.Line;
import org.openide.text.NbDocument;
import org.openide.text.Line.Part;
import org.openide.util.RequestProcessor;
import org.netbeans.api.debugger.DebuggerEngine;
import org.netbeans.api.debugger.DebuggerManager;
import org.netbeans.api.debugger.jpda.InvalidExpressionException;
import org.netbeans.api.debugger.jpda.JPDADebugger;
import org.netbeans.api.debugger.jpda.JPDAThread;
import org.netbeans.api.debugger.jpda.ObjectVariable;
import org.netbeans.api.debugger.jpda.Variable;
import org.netbeans.spi.debugger.jpda.EditorContext.Operation;

import org.netbeans.spi.debugger.ui.EditorContextDispatcher;


public class ToolTipAnnotation extends Annotation implements Runnable {
    
    private static final int TO_STRING_LENGTH_LIMIT = 10000;

    private Part lp;
    private EditorCookie ec;

    public String getShortDescription () {
        // [TODO] hack for org.netbeans.modules.debugger.jpda.actions.MethodChooser that disables tooltips
        if ("true".equals(System.getProperty("org.netbeans.modules.debugger.jpda.doNotShowTooltips"))) { // NOI18N
            return null;
        }
        DebuggerEngine currentEngine = DebuggerManager.getDebuggerManager ().
            getCurrentEngine ();
        if (currentEngine == null) return null;
        JPDADebugger d = currentEngine.lookupFirst(null, JPDADebugger.class);
        if (d == null) return null;

        Part lp = (Part) getAttachedAnnotatable();
        if (lp == null) return null;
        Line line = lp.getLine ();
        DataObject dob = DataEditorSupport.findDataObject (line);
        if (dob == null) return null;
        EditorCookie ec = dob.getCookie(EditorCookie.class);
        if (ec == null) 
            return null;
            // Only for editable dataobjects

        this.lp = lp;
        this.ec = ec;
        RequestProcessor.getDefault ().post (this);
        return null;
    }

    public void run () {
        if (lp == null || ec == null) return ;
        StyledDocument doc;
        try {
            doc = ec.openDocument();
        } catch (IOException ex) {
            return ;
        }                    
        JEditorPane ep = EditorContextDispatcher.getDefault().getCurrentEditor ();
        if (ep == null) return ;
        int offset;
        String expression = getIdentifier (
            doc, 
            ep,
            offset = NbDocument.findLineOffset (
                doc,
                lp.getLine ().getLineNumber ()
            ) + lp.getColumn ()
        );
        if (expression == null) return ;
        DebuggerEngine currentEngine = DebuggerManager.getDebuggerManager ().
            getCurrentEngine ();
        if (currentEngine == null) return;
        JPDADebugger d = currentEngine.lookupFirst(null, JPDADebugger.class);
        if (d == null) return;
        JPDAThread t = d.getCurrentThread();
        if (t == null || !t.isSuspended()) return ;
        String toolTipText = null;
        try {
            Variable v = null;
            List<Operation> operations = t.getLastOperations();
            if (operations != null) {
                for (Operation operation: operations) {
                    if (!expression.endsWith(operation.getMethodName())) {
                        continue;
                    }
                    if (operation.getMethodStartPosition().getOffset() <= offset &&
                        offset <= operation.getMethodEndPosition().getOffset()) {
                        v = operation.getReturnValue();
                    }
                }
            }
            if (v == null) {
                v = d.evaluate (expression);
            }
            String type = v.getType ();
            if (v instanceof ObjectVariable)
                try {
                    String toString = null;
                    try {
                        java.lang.reflect.Method toStringMethod =
                                v.getClass().getMethod("getToStringValue",  // NOI18N
                                                       new Class[] { Integer.TYPE });
                        toStringMethod.setAccessible(true);
                        toString = (String) toStringMethod.invoke(v, TO_STRING_LENGTH_LIMIT);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    if (toString == null) {
                        toString = ((ObjectVariable) v).getToStringValue();
                    }
                    toolTipText = expression + " = " + 
                        (type.length () == 0 ? 
                            "" : 
                            "(" + type + ") ") +
                        toString;
                } catch (InvalidExpressionException ex) {
                    toolTipText = expression + " = " +
                        (type.length () == 0 ? 
                            "" : 
                            "(" + type + ") ") +
                        v.getValue ();
                }
            else 
                toolTipText = expression + " = " + 
                    (type.length () == 0 ? 
                        "" : 
                        "(" + type + ") ") +
                    v.getValue ();
        } catch (InvalidExpressionException e) {
            toolTipText = expression + " = >" + e.getMessage () + "<";
        }
        firePropertyChange (PROP_SHORT_DESCRIPTION, null, toolTipText);
    }

    public String getAnnotationType () {
        return null; // Currently return null annotation type
    }

    private static String getIdentifier (
        StyledDocument doc, 
        JEditorPane ep, 
        int offset
    ) {
        String t = null;
        if ( (ep.getSelectionStart () <= offset) &&
             (offset <= ep.getSelectionEnd ())
        )   t = ep.getSelectedText ();
        if (t != null) return t;
        
        int line = NbDocument.findLineNumber (
            doc,
            offset
        );
        int col = NbDocument.findLineColumn (
            doc,
            offset
        );
        try {
            Element lineElem = 
                NbDocument.findLineRootElement (doc).
                getElement (line);

            if (lineElem == null) return null;
            int lineStartOffset = lineElem.getStartOffset ();
            int lineLen = lineElem.getEndOffset() - lineStartOffset;
            t = doc.getText (lineStartOffset, lineLen);
            int identStart = col;
            while (identStart > 0 && 
                (Character.isJavaIdentifierPart (
                    t.charAt (identStart - 1)
                ) ||
                (t.charAt (identStart - 1) == '.'))) {
                identStart--;
            }
            int identEnd = col;
            while (identEnd < lineLen && 
                   Character.isJavaIdentifierPart(t.charAt(identEnd))
            ) {
                identEnd++;
            }

            if (identStart == identEnd) return null;
            return t.substring (identStart, identEnd);
        } catch (BadLocationException e) {
            return null;
        }
    }
    
}

