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
package org.netbeans.modules.scala.refactoring;

import java.io.IOException
import java.lang.ref.WeakReference
import org.netbeans.modules.csl.spi.support.ModificationResult
import org.netbeans.modules.csl.spi.support.ModificationResult.Difference
import org.netbeans.modules.refactoring.spi.SimpleRefactoringElementImplementation
import org.openide.filesystems.FileObject
import org.openide.text.PositionBounds
import org.openide.text.PositionRef
import org.openide.util.Exceptions
import org.openide.util.Lookup
import org.openide.util.lookup.Lookups

import org.netbeans.modules.scala.refactoring.ui.tree.ElementGripFactory

/**
 *
 * @author Jan Becicka
 */
object DiffElement {
  def apply(diff: Difference, fileObject: FileObject, modification: ModificationResult): DiffElement = {
    val start = diff.getStartPosition
    val end = diff.getEndPosition
    val bounds = new PositionBounds(start, end)
    new DiffElement(diff, bounds, fileObject, modification)
  }
}

class DiffElement(diff: Difference, bounds: PositionBounds, parentFile: FileObject, modification: ModificationResult) extends SimpleRefactoringElementImplementation {
  val displayText = diff.getDescription
  private var newFileContent: WeakReference[String] = _

  def getLookup: Lookup = {
    val composite = ElementGripFactory.getDefault.get(parentFile, bounds.getBegin.getOffset) match {
      case null => parentFile
      case x => x
    }
    Lookups.fixed(composite, diff)
  }

  override def setEnabled(enabled: Boolean) {
    diff.exclude(!enabled)
    newFileContent = null
    super.setEnabled(enabled);
  }

  def performChange {}

  override def getPosition = bounds
  override def getText = displayText
  override def getDisplayText = displayText
  override def getParentFile = parentFile

  override protected def getNewFileContent: String = {
    if (newFileContent != null) {
      newFileContent.get match {
        case null =>
        case x => return x
      }
    }
    
    val result = try {
      modification.getResultingSource(parentFile)
    } catch {case ex: IOException => Exceptions.printStackTrace(ex); return null}

    newFileContent = new WeakReference[String](result)
    result
  }
}
