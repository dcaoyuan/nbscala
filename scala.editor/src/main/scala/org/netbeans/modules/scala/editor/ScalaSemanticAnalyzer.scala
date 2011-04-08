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

package org.netbeans.modules.scala.editor

import org.netbeans.api.lexer.{TokenHierarchy}
import org.netbeans.modules.csl.api.{ElementKind, ColoringAttributes, OffsetRange, SemanticAnalyzer}
import org.netbeans.modules.parsing.spi.{Scheduler, SchedulerEvent}
import org.netbeans.modules.scala.core.ScalaGlobal
import org.netbeans.modules.scala.core.ScalaParserResult
import org.netbeans.modules.scala.core.ast.{ScalaRootScope}
import org.netbeans.modules.scala.core.lexer.{ScalaLexUtil, ScalaTokenId}

import scala.tools.nsc.symtab.Flags

/**
 *
 * @author Caoyuan Deng
 */
class ScalaSemanticAnalyzer extends SemanticAnalyzer[ScalaParserResult] {

  private var cancelled: Boolean = _
  private var semanticHighlights: java.util.Map[OffsetRange, java.util.Set[ColoringAttributes]] = _

  protected final def isCancelled: Boolean = synchronized {
    cancelled
  }

  protected final def resume: Unit = synchronized {
    cancelled = false
  }

  override def getHighlights: java.util.Map[OffsetRange, java.util.Set[ColoringAttributes]] = {
    semanticHighlights
  }

  override def getPriority: Int = 0

  override def getSchedulerClass: Class[_ <: Scheduler] = {
    Scheduler.EDITOR_SENSITIVE_TASK_SCHEDULER
  }

  override def cancel: Unit = {
    cancelled = true
  }

  @throws(classOf[Exception])
  override def run(pr: ScalaParserResult, event: SchedulerEvent): Unit = {
    resume

    if (isCancelled) return

    val root = pr.rootScope

    val doc = pr.getSnapshot.getSource.getDocument(true)
    if (doc == null) return
    val th = pr.getSnapshot.getTokenHierarchy
    val global = pr.global

    val highlights = new java.util.HashMap[OffsetRange, java.util.Set[ColoringAttributes]](100)
    //visitScopeRecursively(doc, th, rootScope, highlights);
    visitItems(global, th, root, highlights)

    this.semanticHighlights = if (!highlights.isEmpty) {
      //            if (result.getTranslatedSource() != null) {
      //                Map<OffsetRange, ColoringAttributes> translated = new HashMap<OffsetRange, ColoringAttributes>(2 * highlights.size());
      //                for (Map.Entry<OffsetRange, ColoringAttributes> entry:  highlights.entrySet()) {
      //                    OffsetRange range = LexUtilities.getLexerOffsets(info, entry.getKey());
      //                    if (range != OffsetRange.NONE) {
      //                        translated.put(range, entry.getValue());
      //                    }
      //                }
      //
      //                highlights = translated;
      //            }

      highlights
    } else null
  }

  private def visitItems(global: ScalaGlobal, th: TokenHierarchy[_], root: ScalaRootScope,
                         highlights: java.util.Map[OffsetRange, java.util.Set[ColoringAttributes]]
  ): Unit = {

    import global._

    for ((idToken, items) <- root.idTokenToItems;
         item = ScalaUtil.importantItem(items);
         name = item.getName if name != "this" && name != "super"
    ) {
      if (isCancelled) return

      // * token may be xml tokens, @see AstVisit#getTokenId
      idToken.id match {
        case ScalaTokenId.Identifier | ScalaTokenId.This | ScalaTokenId.Super =>
          val hiRange = ScalaLexUtil.getRangeOfToken(th, idToken)
          val coloringSet = new java.util.HashSet[ColoringAttributes]
          val sym = item.asInstanceOf[ScalaItem].symbol
          
          item match {
            
            case dfn: ScalaDfn =>
              
              if (sym.isModule) {

                coloringSet.add(ColoringAttributes.CLASS)
                coloringSet.add(ColoringAttributes.DECLARATION)
                coloringSet.add(ColoringAttributes.GLOBAL)

              } else if (sym.isClass || sym.isType || sym.isTrait || sym.isTypeParameter) {

                coloringSet.add(ColoringAttributes.CLASS)
                coloringSet.add(ColoringAttributes.DECLARATION)

              } else if (sym.isSetter) {

                coloringSet.add(ColoringAttributes.LOCAL_VARIABLE)
                coloringSet.add(ColoringAttributes.GLOBAL)

              } else if (sym.isGetter) {

                coloringSet.add(ColoringAttributes.FIELD)
                coloringSet.add(ColoringAttributes.GLOBAL)

              } else if (sym.hasFlag(Flags.LAZY)) {

                coloringSet.add(ColoringAttributes.FIELD)
                val owner = sym.owner
                if (owner != null && (owner.isClass || owner.isTrait || owner.isModule)) {
                  coloringSet.add(ColoringAttributes.GLOBAL)
                }

              } else if (sym.isMethod && sym.hasFlag(Flags.DEFERRED)) {

                coloringSet.add(ColoringAttributes.METHOD)
                coloringSet.add(ColoringAttributes.DECLARATION)
                coloringSet.add(ColoringAttributes.GLOBAL)

              } else if (sym.isMethod) {

                coloringSet.add(ColoringAttributes.METHOD)
                coloringSet.add(ColoringAttributes.DECLARATION)
                  
              } else if (sym.hasFlag(Flags.PARAM)) {

                coloringSet.add(ColoringAttributes.PARAMETER)

              } else if (sym.hasFlag(Flags.MUTABLE)) {

                coloringSet.add(ColoringAttributes.LOCAL_VARIABLE)
                  
              } else if (sym.isValue && !sym.hasFlag(Flags.PACKAGE)) {

                coloringSet.add(ColoringAttributes.FIELD)
                
              } 
              
            case ref: ScalaRef =>
              def isSinletonType = try {
                sym.tpe.resultType.isInstanceOf[SingletonType]
              } catch {case _ => false}
              
              if (sym.isClass || sym.isType || sym.isTrait || sym.isTypeParameter || sym.isConstructor) {

                coloringSet.add(ColoringAttributes.CLASS)

              } else if (sym.isModule && !sym.hasFlag(Flags.PACKAGE) || isSinletonType) {

                coloringSet.add(ColoringAttributes.CLASS)
                coloringSet.add(ColoringAttributes.GLOBAL)

              } else if (sym.hasFlag(Flags.LAZY)) { // why it's also setter/getter?

                coloringSet.add(ColoringAttributes.FIELD)
                val owner = sym.owner
                if (owner != null && (owner.isClass || owner.isTrait || owner.isModule)) {
                  coloringSet.add(ColoringAttributes.GLOBAL)
                }

              } else if (sym.isSetter) {

                coloringSet.add(ColoringAttributes.LOCAL_VARIABLE)
                coloringSet.add(ColoringAttributes.GLOBAL)

              } else if (sym.isGetter) {

                val name = sym.nameString
                val isVariable = try {
                  val owntpe = sym.owner.tpe
                  owntpe.members exists {x => x.isVariable && x.nameString == name}
                } catch {case _ => false}

                if (isVariable) {
                  coloringSet.add(ColoringAttributes.LOCAL_VARIABLE)
                } else {
                  coloringSet.add(ColoringAttributes.FIELD)
                }
                coloringSet.add(ColoringAttributes.GLOBAL)

              } else if (sym.hasFlag(Flags.PARAM) || sym.hasFlag(Flags.PARAMACCESSOR)) {

                coloringSet.add(ColoringAttributes.PARAMETER)

              } else if (sym.isMethod && ref.getKind == ElementKind.RULE) { // implicit call

                coloringSet.add(ColoringAttributes.INTERFACE)

              } else if (sym.isMethod) {

                coloringSet.add(ColoringAttributes.METHOD)

              } else if (sym.hasFlag(Flags.IMPLICIT)) {

                coloringSet.add(ColoringAttributes.INTERFACE)

              } else if (sym.hasFlag(Flags.MUTABLE)) {

                coloringSet.add(ColoringAttributes.LOCAL_VARIABLE)

              } else if (sym.isValue && !sym.hasFlag(Flags.PACKAGE)) {
                
                coloringSet.add(ColoringAttributes.FIELD)

              }
          }

          if (sym.isDeprecated) coloringSet.add(ColoringAttributes.DEPRECATED)
          if (sym.hasFlag(Flags.LAZY)) coloringSet.add(ColoringAttributes.INTERFACE)
          if (sym.hasFlag(Flags.BYNAMEPARAM)) coloringSet.add(ColoringAttributes.INTERFACE)

          if (!coloringSet.isEmpty) highlights.put(hiRange, coloringSet)

        case _ =>
      }

    }
    
  }
}
