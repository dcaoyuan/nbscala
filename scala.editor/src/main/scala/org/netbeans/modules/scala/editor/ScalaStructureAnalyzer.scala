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

import javax.swing.ImageIcon
import javax.swing.text.{BadLocationException}
import org.netbeans.api.language.util.ast.{AstDfn, AstScope}
import org.netbeans.editor.{BaseDocument, Utilities}
import org.netbeans.modules.csl.api.{ElementHandle, ElementKind, Modifier, OffsetRange,
                                     HtmlFormatter, StructureItem, StructureScanner}
import org.netbeans.modules.csl.api.StructureScanner._
import org.netbeans.modules.csl.spi.ParserResult
import org.netbeans.modules.scala.core.ScalaParserResult
import org.netbeans.modules.scala.core.ast.{ScalaDfns}
import org.netbeans.modules.scala.core.lexer.{ScalaTokenId, ScalaLexUtil}
import org.openide.util.Exceptions

import scala.collection.mutable.{Stack}

/**
 *
 * @author Caoyuan Deng
 */
class ScalaStructureAnalyzer extends StructureScanner {

  override def getConfiguration: Configuration = null

  override def scan(result: ParserResult): java.util.List[StructureItem] = {
    result match {
      case pResult: ScalaParserResult =>
        val rootScope = pResult.rootScope

        val items = new java.util.ArrayList[StructureItem]
        scanTopForms(rootScope, items, pResult)
      
        items
      case _ => java.util.Collections.emptyList[StructureItem]
    }
  }

  private def scanTopForms(scope: AstScope, items: java.util.List[StructureItem], pResult: ScalaParserResult): Unit = {
    scope.dfns foreach {
      case dfn: ScalaDfns#ScalaDfn => dfn.getKind match {
          case ElementKind.CLASS | ElementKind.MODULE =>
            (dfn.enclosingScope, dfn.enclosingDfn) match {
              case (x, _) if x.isRoot => items.add(new ScalaStructureItem(dfn, pResult))
              case (_, Some(x)) if x.getKind == ElementKind.PACKAGE => items.add(new ScalaStructureItem(dfn, pResult))
              case _ =>
            }
          case _ =>
        }
        scanTopForms(dfn.bindingScope, items, pResult)
    }
  }

  val emptyFolds = java.util.Collections.emptyMap[String, java.util.List[OffsetRange]]
  override def folds(result: ParserResult): java.util.Map[String, java.util.List[OffsetRange]] = {
    if (result == null) {
      return emptyFolds
    }

    val doc = result.getSnapshot.getSource.getDocument(true) match {
      case null => return emptyFolds
      case x => x.asInstanceOf[BaseDocument]
    }

    val th = result.getSnapshot.getTokenHierarchy match {
      case null => return emptyFolds
      case x => x
    }

    val ts = ScalaLexUtil.getTokenSequence(th, 1).getOrElse(return emptyFolds)
    
    val folds = new java.util.HashMap[String, java.util.List[OffsetRange]]
    val codefolds = new java.util.ArrayList[OffsetRange]
    folds.put("codeblocks", codefolds) // NOI18N

    val commentfolds = new java.util.ArrayList[OffsetRange]

    var importStart = 0
    var importEnd = 0
    var startImportSet = false
    var endImportSet = false

    val comments = new Stack[Array[Int]]
    val blocks = new Stack[Int]

    while (ts.isValid && ts.moveNext) {
      val tk = ts.token
      tk.id match {
        case ScalaTokenId.Import =>
          val offset = ts.offset
          if (!startImportSet) {
            importStart = offset
            startImportSet = true
          }
          if (!endImportSet) {
            importEnd = offset
          }
        case ScalaTokenId.BlockCommentStart | ScalaTokenId.DocCommentStart =>
          val commentStart = ts.offset
          val commentLines = 0
          comments push Array(commentStart, commentLines)
        case ScalaTokenId.BlockCommentData | ScalaTokenId.DocCommentData =>
          // * does this block comment (per BlockCommentData/DocCommentData per line as lexer) span multiple lines?
          comments.top(1) = comments.top(1) + 1
        case ScalaTokenId.BlockCommentEnd | ScalaTokenId.DocCommentEnd =>
          if (!comments.isEmpty) {
            val comment = comments.pop
            if (comment(1) > 1) {
              // * multiple lines
              val commentRange = new OffsetRange(comment(0), ts.offset + tk.length)
              commentfolds.add(commentRange)
            }
          }
        case ScalaTokenId.LBrace =>
          val blockStart = ts.offset
          blocks push blockStart
        case ScalaTokenId.RBrace =>
          if (!blocks.isEmpty) {
            val blockStart = blocks.pop
            val lineEnd = Utilities.getRowEnd(doc, blockStart)
            if (ts.offset + tk.length > lineEnd) { // not in same line
              val blockRange = new OffsetRange(blockStart, ts.offset + tk.length)
              codefolds.add(blockRange)
            }
          }
        case ScalaTokenId.Object | ScalaTokenId.Class | ScalaTokenId.Trait => endImportSet = true
        case _ =>
      }
    }

    try {
      /** @see GsfFoldManager#addTree() for suitable fold names. */
      importEnd = Utilities.getRowEnd(doc, importEnd)

      // * same strategy here for the import statements: We have to have
      // * *more* than one line to fold them.
      if (Utilities.getRowCount(doc, importStart, importEnd) > 1) {
        val importfolds = new java.util.ArrayList[OffsetRange]
        val range = new OffsetRange(importStart, importEnd)
        importfolds.add(range)
        folds.put("imports", importfolds) // NOI18N
      }

      folds.put("comments", commentfolds) // NOI18N
    } catch {case ex: BadLocationException => Exceptions.printStackTrace(ex)}

    folds
  }
    
  @throws(classOf[BadLocationException])
  private def addCodeFolds(pResult: ScalaParserResult, doc: BaseDocument, defs: Seq[AstDfn],
                           codeblocks: java.util.List[OffsetRange]): Unit = {
    import ElementKind._
       
    for (dfn <- defs) {
      dfn.getKind match {
        case FIELD | METHOD | CONSTRUCTOR | CLASS | ATTRIBUTE =>
          var range = dfn.getOffsetRange(pResult)
          var start = range.getStart
          // * start the fold at the end of the line behind last non-whitespace, should add 1 to start after "->"
          start = Utilities.getRowLastNonWhite(doc, start) + 1
          val end = range.getEnd
          if (start != -1 && end != -1 && start < end && end <= doc.getLength) {
            range = new OffsetRange(start, end)
            codeblocks.add(range)
          }
        case _ =>
      }
    
      val children = dfn.bindingScope.dfns
      addCodeFolds(pResult, doc, children, codeblocks)
    }
  }

  private class ScalaStructureItem(val dfn: ScalaDfns#ScalaDfn, pResult: ScalaParserResult) extends StructureItem {
    import ElementKind._

    override def getName: String = dfn.getName

    override def getSortText: String = getName

    override def getHtml(formatter:HtmlFormatter): String = {
      dfn.htmlFormat(formatter)
      formatter.getText
    }

    override def getElementHandle: ElementHandle = dfn

    override def getKind: ElementKind = dfn.getKind
        
    override def getModifiers: java.util.Set[Modifier] = dfn.getModifiers

    override def isLeaf: Boolean = {
      dfn.getKind match {
        case MODULE | CLASS => false
        case METHOD | CONSTRUCTOR | FIELD | VARIABLE | OTHER | PARAMETER | ATTRIBUTE => true
        case _ => true
      }
    }

    override def getNestedItems: java.util.List[StructureItem] = {
      val nested = dfn.bindingScope.dfns
      if (!nested.isEmpty) {
        val children = new java.util.ArrayList[StructureItem]

        nested foreach {
          case child: ScalaDfns#ScalaDfn => child.getKind match {
              case PARAMETER | OTHER =>
              case _ => children.add(new ScalaStructureItem(child, pResult))
            }
        }

        children
      } else java.util.Collections.emptyList[StructureItem]
    }

    override def getPosition: Long = {
      try {
        pResult.getSnapshot.getTokenHierarchy match {
          case null => 0
          case th => dfn.boundsOffset(th)
        }
      } catch {case ex:Exception => 0}
    }

    override def getEndPosition: Long = {
      try {
        pResult.getSnapshot.getTokenHierarchy match {
          case null => 0
          case th => dfn.boundsEndOffset(th)
        }
      } catch {case ex: Exception => 0}
    }

    override def equals(o: Any): Boolean = o match {
      case null => false
      case x:ScalaStructureItem if dfn.getKind == x.dfn.getKind && getName.equals(x.getName) => true
      case _ => false
    }

    override def hashCode: Int = {
      var hash = 7
      hash = (29 * hash) + (if (getName != null) getName.hashCode else 0)
      hash = (29 * hash) + (if (dfn.getKind != null) dfn.getKind.hashCode else 0)
      hash
    }

    override def toString = getName

    override def getCustomIcon: ImageIcon = null
  }
}
