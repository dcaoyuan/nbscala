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

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.util.logging.Level;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import org.netbeans.editor.AnnotationDesc;
import org.netbeans.editor.Annotations;
import org.netbeans.editor.BaseDocument;
import org.netbeans.editor.ImplementationProvider;
import org.netbeans.editor.JumpList;
import org.netbeans.editor.Utilities;
import org.openide.ErrorManager;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;

/**
 *
 * @author Jan Lahoda
 */
class IsOverriddenAnnotationAction extends AbstractAction {

  putValue(Action.NAME, NbBundle.getMessage(classOf[IsOverriddenAnnotationAction], "CTL_IsOverriddenAnnotationAction")) //NOI18N
  setEnabled(true)
    
  def actionPerformed(e: ActionEvent) {
    if (!invokeDefaultAction(e.getSource.asInstanceOf[JTextComponent])) {
      val actions = ImplementationProvider.getDefault.getGlyphGutterActions(e.getSource.asInstanceOf[JTextComponent])
            
      if (actions == null) return
            
      var nextAction = 0
      while (nextAction < actions.length && actions(nextAction) != this) {
        nextAction += 1
      }
      nextAction += 1
            
      if (actions.length > nextAction) {
        val action = actions(nextAction)
        if (action != null && action.isEnabled){
          action.actionPerformed(e)
        }
      }
    }
  }
    
  private def getFile(component: JTextComponent): FileObject = {
    val doc = component.getDocument
    doc.getProperty(Document.StreamDescriptionProperty) match {
      case null => null
      case od: DataObject => od.getPrimaryFile
    }
  }
    
  private def findAnnotation(component: JTextComponent, desc: AnnotationDesc, offset: Int): IsOverriddenAnnotation = {
    val file = getFile(component)
    if (file == null) {
      if (ErrorManager.getDefault.isLoggable(ErrorManager.WARNING)) {
        ErrorManager.getDefault.log(ErrorManager.WARNING, "component=" + component + " does not have a file specified in the document."); //NOI18N
      }
      return null
    }
        
    val ah = AnnotationsHolder(file)
    if (ah == null) {
      IsOverriddenAnnotationHandler.Log.log(Level.INFO, "component=" + component + " does not have attached a IsOverriddenAnnotationHandler"); //NOI18N
      return null
    }

    ah.getAnnotations find (x => x.getPosition.getOffset == offset && x.getShortDescription == desc.getShortDescription) match {
      case Some(x) => x
      case _ => null
    }
  }
    
  def invokeDefaultAction(comp: JTextComponent): Boolean = {
    comp.getDocument match {
      case doc: BaseDocument =>
        val currentPosition = comp.getCaretPosition
        val annotations = doc.getAnnotations
        val annotation = new Array[IsOverriddenAnnotation](1)
        val p = new Array[Point](1)
            
        doc.render(new Runnable {
            def run {
              try {
                val line = Utilities.getLineOffset(doc, currentPosition)
                val startOffset = Utilities.getRowStartFromLineOffset(doc, line)
                val desc = annotations.getActiveAnnotation(line)
                p(0) = comp.modelToView(startOffset).getLocation
                annotation(0) = findAnnotation(comp, desc, startOffset)
              } catch {case ex: BadLocationException => Exceptions.printStackTrace(ex)}
            }
          })
            
        if (annotation(0) == null) return false
            
        JumpList.checkAddEntry(comp, currentPosition)
            
        annotation(0).mouseClicked(comp, p(0))
            
        true
      case _ => false
    }
  }
    
}
