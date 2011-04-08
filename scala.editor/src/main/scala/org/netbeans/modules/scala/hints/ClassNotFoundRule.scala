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

import scala.collection.JavaConversions._

import scala.collection.mutable
import scala.collection.immutable

import org.netbeans.modules.csl.api.Error
import org.netbeans.modules.csl.api.Hint
import org.netbeans.modules.csl.api.HintFix
import org.netbeans.modules.csl.api.HintSeverity
import org.netbeans.modules.csl.api.OffsetRange
import org.netbeans.modules.csl.api.RuleContext
import org.netbeans.modules.scala.editor.util.NbBundler
import java.{util=>ju}
import java.util.prefs.Preferences;
import javax.swing.JComponent;
import java.util.regex.{Pattern, Matcher}
import org.netbeans.api.java.source.ElementHandle
import org.netbeans.api.java.source.ClassIndex
import javax.lang.model.element.ElementKind
import org.openide.filesystems.FileObject
import javax.lang.model.element.TypeElement
import org.netbeans.api.language.util.ast.{AstRef, AstDfn, AstItem, AstRootScope}
import org.netbeans.api.lexer.{Language, Token, TokenHierarchy, TokenId, TokenSequence}

import org.netbeans.modules.csl.api.EditList
import org.netbeans.modules.scala.core.ScalaParserResult
import org.netbeans.modules.scala.core.lexer.ScalaTokenId
import org.netbeans.modules.scala.core.lexer.ScalaLexUtil
import org.netbeans.editor.BaseDocument

import org.netbeans.modules.scala.editor.imports.FixImportsHelper

class ClassNotFoundRule extends ScalaErrorRule with NbBundler {

    val DEFAULT_PRIORITY = 292

    override def appliesTo(context : RuleContext) : Boolean = true

    override def getDisplayName : String = locMessage("LBL_ClassNotFound")

    override def showInTasklist : Boolean = false

    override def getDefaultSeverity : HintSeverity = HintSeverity.ERROR


//    override def getKinds : java.util.Set[_] = new java.util.HashSet()
    override def getCodes : ju.Set[String] = {
        val codes = new ju.HashSet[String]()
        codes.add(ScalaErrorRule.SYNTAX_ERROR)
        codes
    }

    override def createHints(context : ScalaRuleContext, error : Error) : List[Hint] =  {
        val desc = error.getDescription
        //println("desc=" + desc)
        val rangeOpt = context.calcOffsetRange(error.getStartPosition, error.getEndPosition)
        if (rangeOpt == None || desc == null) return List[Hint]()
        val hintfixes = mutable.ListBuffer[HintFix]()
        FixImportsHelper.checkMissingImport(desc) match {
            case Some(missing) => hintfixes.addAll(createImportHints(missing, context, error, rangeOpt.get))
            case None =>
        }

        new Hint(this, error.getDescription, context.getFileObject, rangeOpt.get,
               hintfixes, DEFAULT_PRIORITY) :: Nil

    }

  private def createImportHints(missing : String, context : ScalaRuleContext, error : Error, range : OffsetRange) : mutable.ListBuffer[HintFix] = {
    val pathInfo = context.getClasspathInfo match {
      case Some(x) => x
      case None => return mutable.ListBuffer[HintFix]()
    }
    val typeNames : mutable.Set[ElementHandle[TypeElement]] = pathInfo.getClassIndex.getDeclaredTypes(missing, ClassIndex.NameKind.SIMPLE_NAME,
                                                                  java.util.EnumSet.allOf(classOf[ClassIndex.SearchScope]))
    val toRet = mutable.ListBuffer[HintFix]()
    for (typeName <- typeNames;
         ek = typeName.getKind;
         if ek == ElementKind.CLASS || ek == ElementKind.INTERFACE
    ) {
      toRet += new AddImportFix(missing, typeName.getQualifiedName, context, range)
    }
    //TODO we need to also check the scala sources in the current project, how?
    toRet
  }

  private def createNewDefVal(missing : String, context : ScalaRuleContext, error : Error, range : OffsetRange) : mutable.ListBuffer[HintFix] = {
    mutable.ListBuffer[HintFix]() += new AddDefValFix(missing, context, range)
  }

  private def calcErrorStartPosition(range : OffsetRange, name : String, ts : TokenSequence[TokenId]) : Int = {
      ts.move(range.getStart)
      val includes : Set[TokenId] = Set(ScalaTokenId.Type, ScalaTokenId.Identifier)
      var token = ScalaLexUtil.findNextIncluding(ts, includes)
      while (token != None && ts.offset <= range.getEnd) {
          if (name == token.get.text.toString) return ts.offset
          token = ScalaLexUtil.findNextIncluding(ts, includes)
      }
      -1
  }


    class AddImportFix(name : String, fqn : String, context : ScalaRuleContext, offsetRange : OffsetRange) extends HintFix  {

        override def getDescription = locMessage("AddImport", fqn)
        override val isSafe = true
        override val isInteractive = false

        @throws(classOf[Exception])
        override def implement : Unit = {
            FixImportsHelper.doImport(context.doc, name, fqn, offsetRange)

        }
    }

   class AddDefValFix(name : String, context : ScalaRuleContext, offsetRange : OffsetRange) extends HintFix  {

        override def getDescription = locMessage("CreateDef", name)
        override val isSafe = true
        override val isInteractive = false

        private def findParams(ts : TokenSequence[TokenId], root : AstRootScope) : List[Tuple2[AstRef, AstDfn]] = {
          val buffer = mutable.ListBuffer[Tuple2[AstRef, AstDfn]]()
          var done = false
          var collecting = false;

          while (ts.isValid && ts.moveNext && !done) {
            val token : Token[_] = ts.token
            done = token.id match {
              case ScalaTokenId.LParen => {
                  collecting = true
                  false
              }
              case ScalaTokenId.RParen => true
              case ScalaTokenId.ANY_KEYWORD => true
              case ScalaTokenId.Identifier => {
                  //TODO
                  false
              }
              case wsComment if ScalaLexUtil.WS_COMMENTS.contains(wsComment) => false
              case _ => false
            }
          }

//          val defn = root.findDfnOf(closest) match {
//            case Some(dfn) => println("found dfn=" + dfn)
//              // is local
//            case None => println("no dfn")
//          }
//          defn
          buffer.toList
        }


        @throws(classOf[Exception])
        override def implement : Unit = {
            val doc = context.doc

            val th = TokenHierarchy.get(doc)
            val ts = ScalaLexUtil.getTokenSequence(th, 0).get
            val start = calcErrorStartPosition(offsetRange, name, ts)
            var collecting = false
            val result = context.parserResult.asInstanceOf[ScalaParserResult]
            println("result=" + result)
            val root = result.rootScope

            if (start != -1) {
              ts.move(start)
              //just double check we are at the correct place..
              if (ts.isValid && ts.moveNext && ts.token.text.toString == name) {
                val astOffset = ScalaLexUtil.getAstOffset(result, start)
                val current = root.findItemsAt(th, astOffset) match {
                  case Nil => return
                  case xs =>  {
                          val x = xs.reverse.head
                          if (x.name == name) x else return
                  }
                }
                val paramTuples = findParams(ts, root)
                current
              }

//              println("astOffset=" + astOffset)
//
//              println("closest=" + closest)

              
/**              ts.move(start)
              while (ts.isValid && ts.moveNext) {
                val token : Token[_] = ts.token
                token.id match {
                  case ScalaTokenId.Import =>
                    collecting = true;
                  case wsComment if ScalaLexUtil.WS_COMMENTS.contains(wsComment) =>

                  case _ => if (collecting) {
                      buffer.append(token.text.toString)
                      finisher = ts.offset + token.length
                    }
                }
              }
            }
**/

            println("end")
          }

        }
   }
}
