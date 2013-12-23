/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2009 Sun Microsystems, Inc. All rights reserved.
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
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2009 Sun Microsystems, Inc.
 */

package org.netbeans.modules.scala.hints

import java.util.prefs.Preferences
import javax.swing.JComponent
import javax.swing.JPanel
import org.netbeans.api.language.util.ast._
import org.netbeans.editor.BaseDocument
import org.netbeans.modules.scala.core.lexer.ScalaLexUtil
import org.netbeans.modules.scala.core.lexer.ScalaTokenId
import org.netbeans.modules.csl.api.Hint
import org.netbeans.modules.csl.api.HintSeverity
import org.netbeans.modules.csl.api.RuleContext
import org.netbeans.modules.scala.editor.util.NbBundler
import org.netbeans.modules.scala.core.ast.ScalaRootScope
import org.netbeans.modules.scala.core.ScalaParserResult
import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.collection.immutable
import scala.tools.nsc.symtab._

class RemoveImportRule() extends ScalaAstRule with NbBundler {
  val DEFAULT_PRIORITY = 293

  /**
   * Gets unique ID of the rule
   */
  def getId(): String = "RemoveImportRule"

  /**
   * Gets longer description of the rule
   */
  def getDescription(): String = "Desc"

  /**
   * Finds out whether the rule is currently enabled.
   * @return true if enabled false otherwise.
   */
  def getDefaultEnabled(): Boolean = true

  /**
   * Gets the UI description for this rule. It is fine to return null
   * to get the default behavior. Notice that the Preferences node is a copy
   * of the node returned from {link:getPreferences()}. This is in oder to permit
   * canceling changes done in the options dialog.<BR>
   * Default implementation return null, which results in no customizer.
   * It is fine to return null (as default implementation does)
   * @param node Preferences node the customizer should work on.
   * @return Component which will be shown in the options dialog.
   */
  def getCustomizer(node: Preferences): JComponent = new JPanel()

  /**
   * Return true iff this hint applies to the given file
   */
  def appliesTo(context: RuleContext): Boolean = true

  /**
   * Get's UI usable name of the rule
   */
  def getDisplayName: String = "Remove import"

  /**
   * Whether this task should be shown in the tasklist
   */
  def showInTasklist: Boolean = false

  /**
   * Gets current severiry of the hint.
   * @return Hints severity in current profile.
   */
  def getDefaultSeverity: HintSeverity = HintSeverity.WARNING

  def getKinds(): java.util.Set[_] = java.util.Collections.singleton(ScalaAstRule.ROOT)

  def createHints(context: ScalaRuleContext, scope: ScalaRootScope): List[Hint] = {
    val pr = context.parserResult.asInstanceOf[ScalaParserResult]
    val global = pr.global

    val th = pr.getSnapshot.getTokenHierarchy
    val content = pr.getSnapshot.getText
    val doc = pr.getSnapshot.getSource.getDocument(false).asInstanceOf[BaseDocument]
    val importings = scope.importingItems.asInstanceOf[Set[global.ScalaItem]]
    val usages = scope.idTokenToItems.values.flatten filter (!importings.contains(_)) map (_.symbol.asInstanceOf[global.Symbol].fullName)

    val implicits = new mutable.HashSet[String]
    val unused = new mutable.HashMap[String, global.ScalaItem]
    importings foreach { imp =>
      //println("import: " + imp)
      val impSym = imp.symbol
      if (impSym.hasFlag(Flags.PACKAGE)) {
        // @todo
      } else {
        if (imp.idToken.id == ScalaTokenId.Wild) { // qual._, symbol is pointed to qual
          val qual = impSym.fullName
          //println("wild import: " + qual)
          // @todo
          //added.add(qual) && !(usages exists {qualName(_) == qual})
        } else {
          val qName = impSym.fullName
          if (impSym.hasFlag(Flags.IMPLICIT)) {
            // TODO complicate condition
            implicits.add(qName)
          } else {
            if (!unused.contains(qName) && !usages.contains(qName)) {
              unused(qName) = imp
            }
          }
        }
      }
    }

    (unused filter { xy => !implicits.contains(xy._1) } values) map { item =>
      var offset = item.idOffset(th)
      var endOffset = item.idEndOffset(th)
      var text = item.idToken.text

      ScalaLexUtil.findImportAt(doc, th, offset) match {
        case me @ ScalaLexUtil.ImportTokens(start, end, qual, hd :: Nil) => // has only one selector
          offset = start.offset(th)
          endOffset = end.offset(th) + end.length
          text = content.subSequence(offset, endOffset + 1)
        case _ =>
      }

      val rangeOpt = context.calcOffsetRange(offset, endOffset)
      new Hint(this, "Remove Unused " + text, context.getFileObject, rangeOpt.get, new java.util.ArrayList() /**new RemoveImportFix(context, offset, endOffset, text)) */ , DEFAULT_PRIORITY)
    } toList
  }

  //debug method
  private def printSymbolDetails(prefix: String, s: scala.reflect.internal.Symbols#Symbol): Unit = {
    println(prefix + "=" + s)
    println("    fullname=" + s.fullName)
  }

}
