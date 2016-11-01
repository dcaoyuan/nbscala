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
package org.netbeans.modules.scala.refactoring

import javax.swing.Icon
import javax.swing.text.Position.Bias
import org.netbeans.api.editor.document.LineDocumentUtils
import org.netbeans.modules.csl.api.OffsetRange
import org.netbeans.modules.csl.spi.GsfUtilities
import org.netbeans.modules.refactoring.spi.SimpleRefactoringElementImplementation
import org.openide.filesystems.FileObject
import org.openide.text.PositionBounds
import org.openide.util.Exceptions
import org.openide.util.Lookup
import org.openide.util.lookup.Lookups
import org.netbeans.modules.scala.core.ScalaParserResult
import org.netbeans.modules.scala.core.ast.ScalaItems
import org.netbeans.modules.scala.refactoring.ui.tree.ElementGripFactory

/**
 * An element in the refactoring preview list which holds information about the find-usages-match
 *
 * @author Tor Norbye
 */
object WhereUsedElement {
  def apply(pr: ScalaParserResult, handle: ScalaItems#ScalaItem): WhereUsedElement = {
    val th = pr.getSnapshot.getTokenHierarchy
    val range = new OffsetRange(handle.idOffset(th), handle.idEndOffset(th))
    assert(range != OffsetRange.NONE)

    val icon = handle.getIcon

    apply(pr, handle.symbol.nameString, range, icon)
  }

  def apply(pr: ScalaParserResult, name: String, range: OffsetRange, icon: Icon): WhereUsedElement = {
    var start = range.getStart
    var end = range.getEnd

    var sta = start
    var en = start // ! Same line as start
    var content: String = null

    val bdoc = GsfUtilities.getDocument(pr.getSnapshot.getSource.getFileObject, true)
    try {
      bdoc.readLock

      // I should be able to just call tree.getInfo().getText() to get cached
      // copy - but since I'm playing fast and loose with compilationinfos
      // for for example find subclasses (using a singly dummy FileInfo) I need
      // to read it here instead
      content = bdoc.getText(0, bdoc.getLength)
      sta = LineDocumentUtils.getLineFirstNonWhitespace(bdoc, start)

      if (sta == -1) {
        sta = LineDocumentUtils.getLineStart(bdoc, start)
      }

      en = LineDocumentUtils.getLineLastNonWhitespace(bdoc, start)

      if (en == -1) {
        en = LineDocumentUtils.getLineEnd(bdoc, start)
      } else {
        // Last nonwhite - left side of the last char, not inclusive
        en += 1
      }

      // Sometimes the node we get from the AST is for the whole block
      // (e.g. such as the whole class), not the argument node. This happens
      // for example in Find Subclasses out of the index. In this case
      if (end > en) {
        end = start + name.length

        if (end > bdoc.getLength) {
          end = bdoc.getLength
        }
      }
    } catch {
      case ex: Exception => Exceptions.printStackTrace(ex)
    } finally {
      bdoc.readUnlock
    }

    val sb = new StringBuilder
    if (end < sta) {
      // XXX Shouldn't happen, but I still have AST offset errors
      sta = end
    }
    if (start < sta) {
      // XXX Shouldn't happen, but I still have AST offset errors
      start = sta
    }
    if (en < end) {
      // XXX Shouldn't happen, but I still have AST offset errors
      en = end
    }
    sb.append(RetoucheUtils.getHtml(content.subSequence(sta, start).toString))
    sb.append("<b>") // NOI18N
    sb.append(content.subSequence(start, end))
    sb.append("</b>") // NOI18N
    sb.append(RetoucheUtils.getHtml(content.subSequence(end, en).toString))

    val ces = RetoucheUtils.findCloneableEditorSupport(pr)
    val ref1 = ces.createPositionRef(start, Bias.Forward)
    val ref2 = ces.createPositionRef(end, Bias.Forward)
    val bounds = new PositionBounds(ref1, ref2)

    return new WhereUsedElement(bounds, sb.toString.trim,
      pr.getSnapshot.getSource.getFileObject, name,
      new OffsetRange(start, end), icon)
  }

  def apply(pr: ScalaParserResult, name: String, html: String, range: OffsetRange, icon: Icon): WhereUsedElement = {
    val start = range.getStart
    val end = range.getEnd

    val ces = RetoucheUtils.findCloneableEditorSupport(pr)
    val ref1 = ces.createPositionRef(start, Bias.Forward)
    val ref2 = ces.createPositionRef(end, Bias.Forward)
    val bounds = new PositionBounds(ref1, ref2)

    return new WhereUsedElement(bounds, html,
      pr.getSnapshot.getSource.getFileObject, name,
      new OffsetRange(start, end), icon)
  }
}

class WhereUsedElement(bounds: PositionBounds, displayText: String, parentFile: FileObject,
                       name: String, range: OffsetRange, icon: Icon) extends SimpleRefactoringElementImplementation
      with scala.math.Ordered[WhereUsedElement] {

  ElementGripFactory.getDefault.put(parentFile, name, range, icon)

  def compare(that: WhereUsedElement) = (this.getPosition().getBegin().getLine() - that.getPosition().getBegin().getLine())
  
  def getLookup: Lookup = {
    val composite = ElementGripFactory.getDefault.get(parentFile, bounds.getBegin.getOffset) match {
      case null => parentFile
      case x    => x
    }

    Lookups.singleton(composite)
  }

  def performChange {}

  override def getPosition = bounds
  override def getText = displayText
  override def getDisplayText = displayText
  override def getParentFile = parentFile
}
