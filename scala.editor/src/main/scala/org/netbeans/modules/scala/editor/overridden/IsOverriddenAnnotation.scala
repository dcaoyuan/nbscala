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

import java.awt.Point
import java.awt.Toolkit
import javax.swing.SwingUtilities
import javax.swing.text.JTextComponent
import javax.swing.text.Position
import javax.swing.text.StyledDocument
import org.netbeans.modules.csl.api.UiUtils
import org.openide.filesystems.FileObject
import org.openide.text.Annotation
import org.openide.text.NbDocument
import org.openide.util.NbBundle

/**
 *
 * @author Jan Lahoda
 */
object IsOverriddenAnnotation {
  import AnnotationType._
  
  def performGoToAction(tpe: AnnotationType, declarations: List[ElementDescription], position: Point, shortDescription: String) {
    if (tpe == IMPLEMENTS || tpe == OVERRIDES) {
      declarations match {
        case desc :: Nil =>
          val file = desc.getSourceFile

          if (file != null) {
            UiUtils.open(file, desc.getOffset)
          } else {
            Toolkit.getDefaultToolkit.beep
          }

          return
        case _ =>
      }
    }

    val caption = tpe match {
      case IMPLEMENTS => NbBundle.getMessage(classOf[IsOverriddenAnnotation], "CAP_Implements")
      case OVERRIDES  => NbBundle.getMessage(classOf[IsOverriddenAnnotation], "CAP_Overrides")
      case HAS_IMPLEMENTATION | IS_OVERRIDDEN => shortDescription
      case _ => throw new IllegalStateException("Currently not implemented: " + tpe) //NOI18N
    }

    PopupUtil.showPopup(new IsOverriddenPopup(caption, declarations), caption, position.x, position.y, true, 0)
  }
}

class IsOverriddenAnnotation(document: StyledDocument,
                             pos: Position,
                             tpe: AnnotationType,
                             shortDescription: String,
                             declarations: List[ElementDescription]
) extends Annotation {
  import IsOverriddenAnnotation._
  import AnnotationType._

  assert(pos != null)
        
  def getShortDescription: String = shortDescription

  def getAnnotationType: String = {
    tpe match {
      case IS_OVERRIDDEN      => "org-netbeans-modules-editor-annotations-is_overridden" //NOI18N
      case HAS_IMPLEMENTATION => "org-netbeans-modules-editor-annotations-has_implementations" //NOI18N
      case IMPLEMENTS         => "org-netbeans-modules-editor-annotations-implements" //NOI18N
      case OVERRIDES          => "org-netbeans-modules-editor-annotations-overrides" //NOI18N
      case _ =>  throw new IllegalStateException("Currently not implemented: " + tpe) //NOI18N
    }
  }
    
  def attach {
    NbDocument.addAnnotation(document, pos, -1, this)
  }
    
  def detachImpl {
    NbDocument.removeAnnotation(document, this)
  }
    
  override def toString = {
    "[IsOverriddenAnnotation: " + shortDescription + "]" //NOI18N
  }
    
  def getPosition: Position = pos
    
  def debugDump: String = {
    val elementNames = declarations map (_.getDisplayName)
    "IsOverriddenAnnotation: type=" + tpe.toString + ", elements:" + elementNames //NOI18N
  }
    
  def mouseClicked(c: JTextComponent, p: Point) {
    val position = new Point(p)
        
    SwingUtilities.convertPointToScreen(position, c)
        
    performGoToAction(tpe, declarations, position, shortDescription)
  }

  def getType: AnnotationType = tpe

  def getDeclarations: List[ElementDescription] = declarations
}
