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
package org.netbeans.modules.scala.editor.overridden;

import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.TreePath;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import org.netbeans.api.java.source.Task;
import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.JavaSource.Phase;
import org.netbeans.editor.BaseAction;
import org.netbeans.editor.ext.ExtKit;
//import org.netbeans.modules.editor.java.GoToSupport;
//import org.netbeans.modules.editor.java.JavaKit;
import org.openide.awt.StatusDisplayer;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;

/**
 *
 * @author Jan Lahoda
 */
class GoToSuperTypeAction extends BaseAction {

  def actionPerformed(evt: ActionEvent, target: JTextComponent) {}

/*     public GoToSuperTypeAction() {
        super(JavaKit.gotoSuperImplementationAction, SAVE_POSITION | ABBREV_RESET);
        putValue(SHORT_DESCRIPTION, NbBundle.getBundle(JavaKit.class).getString("goto-super-implementation"));
        String name = NbBundle.getBundle(JavaKit.class).getString("goto-super-implementation-trimmed");
        putValue(ExtKit.TRIMMED_TEXT,name);
        putValue(POPUP_MENU_TEXT, name);
    }

    public void actionPerformed(ActionEvent evt, final JTextComponent target) {
        JavaSource js = JavaSource.forDocument(target.getDocument());

        if (js == null) {
            StatusDisplayer.getDefault().setStatusText(NbBundle.getMessage(GoToSupport.class, "WARN_CannotGoToGeneric",1));
            return;
        }

        final List<ElementDescription> result = new ArrayList<ElementDescription>();
        final AnnotationType[] type  = new AnnotationType[1];

        try {
            js.runUserActionTask(new Task<CompilationController>() {
                public void run(CompilationController parameter) throws Exception {
                    parameter.toPhase(Phase.RESOLVED); //!!!

                    TreePath path = parameter.getTreeUtilities().pathFor(target.getCaretPosition());

                    while (path != null && path.getLeaf().getKind() != Kind.METHOD) {
                        path = path.getParentPath();
                    }

                    if (path == null) {
                        Toolkit.getDefaultToolkit().beep();
                        return ;
                    }

                    Element resolved = parameter.getTrees().getElement(path);

                    if (resolved == null || resolved.getKind() != ElementKind.METHOD) {
                        Toolkit.getDefaultToolkit().beep();
                        return ;
                    }

                    ExecutableElement ee = (ExecutableElement) resolved;

                    type[0] = IsOverriddenAnnotationHandler.detectOverrides(parameter, (TypeElement) ee.getEnclosingElement(), ee, result);
                }

            }, true);

            if (type[0] == null) {
                Toolkit.getDefaultToolkit().beep();
                return ;
            }

            Point p = new Point(target.modelToView(target.getCaretPosition()).getLocation());

            SwingUtilities.convertPointToScreen(p, target);

            IsOverriddenAnnotation.performGoToAction(type[0], result, p, "");
        } catch (IOException e) {
            Exceptions.printStackTrace(e);
        } catch (BadLocationException e) {
            Exceptions.printStackTrace(e);
        }
    }
 */
}
