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

package org.netbeans.modules.scala.editor.imports

import java.util.EnumSet;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.regex.Pattern
import org.netbeans.api.java.source.{ClassIndex};
import org.netbeans.api.java.source.ClassIndex.NameKind;
import org.netbeans.editor.BaseDocument;
import org.netbeans.api.lexer.{Token, TokenHierarchy, TokenId, TokenSequence}
import org.netbeans.editor.Utilities;
import org.netbeans.api.java.source.ui.ElementIcons
import org.netbeans.modules.csl.api.{EditList, OffsetRange}
import org.netbeans.modules.scala.core.{ScalaSourceUtil}
import org.netbeans.modules.scala.core.lexer.{ScalaLexUtil, ScalaTokenId}
import org.openide.filesystems.FileObject
import scala.collection.mutable.ArrayBuffer

/**
 *
 * @author Milos Kleint
 */
object FixImportsHelper{

  private val LOG = Logger.getLogger(classOf[FixImportsHelper].getName)

  val NotFoundValue = Pattern.compile("not found: value (.*)") // NOI18N
  val NotFoundType  = Pattern.compile("not found: type (.*)")  // NOI18N

  def checkMissingImport(desc: String): Option[String] = {
    NotFoundValue.matcher(desc) match {
      case x if x.matches => Some(x.group(1))
      case _ => NotFoundType.matcher(desc) match {
          case x if x.matches => Some(x.group(1))
          case _ => None
        }
    }
  }

  def getImportanceLevel(fqn: String): Int = {
    var weight = 50
    if (fqn.startsWith("scala") || fqn.startsWith("scala.util")) {
      weight -= 20
    } else if (fqn.startsWith("java.lang") || fqn.startsWith("java.util")) {
      weight -= 10
    } else if (fqn.startsWith("org.omg") || fqn.startsWith("org.apache")) {
      weight += 10
    } else if (fqn.startsWith("com.sun") || fqn.startsWith("com.ibm") || fqn.startsWith("com.apple")) {
      weight += 20
    } else if (fqn.startsWith("sun") || fqn.startsWith("sunw") || fqn.startsWith("netscape")) {
      weight += 30
    }
    weight
  }

  def calcOffsetRange(doc: BaseDocument, start: Int, end: Int) : Option[OffsetRange] = {
    try {
      Some(new OffsetRange(Utilities.getRowStart(doc, start), Utilities.getRowEnd(doc, end)))
    } catch {case x: Exception => None}
  }

  /**
   * @return a list of Tuples(start, end, import string)
   */
  def allGlobalImports(doc: BaseDocument): List[(Int, Int, String)] = {
    val ts = ScalaLexUtil.getTokenSequence(doc, 0).getOrElse(return Nil)
    ts.move(0)
    var importStatement = findNextImport(ts, ts.token)
    // +1 means the dot
    val toRet = new ArrayBuffer[(Int, Int, String)]
    while (importStatement != null && importStatement._1 != -1 && importStatement._3.trim.length > 0) {
      toRet += importStatement
      importStatement = findNextImport(ts, ts.token)
    }
    toRet.toList
  }

  def findNextImport(ts: TokenSequence[TokenId], current: Token[_]): (Int, Int, String) = {
    val sb = new StringBuilder
    var collecting = false
    var starter = -1
    var finisher = -1
    while (ts.isValid && ts.moveNext) {
      val token = ts.token
      token.id match {
        case ScalaTokenId.Import =>
          if (collecting) {
            ts.movePrevious
            return (starter, finisher, sb.toString)
          } else {
            collecting = true
            starter = ts.offset
          }
        case ScalaTokenId.Package =>
        case ScalaTokenId.Case | ScalaTokenId.Class | ScalaTokenId.Trait | ScalaTokenId.Object | ScalaTokenId.Sealed |
          ScalaTokenId.At | ScalaTokenId.Abstract | ScalaTokenId.Final | ScalaTokenId.Private | ScalaTokenId.Protected =>
          if (collecting) {
            //too far
            ts.movePrevious
            return (starter, finisher, sb.toString)
          } else {
            return null
          }
        case ScalaTokenId.Semicolon => //ignore semicolons
        case id if ScalaLexUtil.isWsComment(id) =>
        case _ =>
          if (collecting) {
            sb.append(token.text.toString)
            finisher = ts.offset + token.length
          }
      }
    }

    if (collecting) {
      //reasonable case?
      (starter, finisher, sb.toString)
    } else null
  }

  def doImport(doc: BaseDocument, missing: String, fqn: String, offsetRange: OffsetRange) = {
    val th = TokenHierarchy.get(doc)
    val ts = ScalaLexUtil.getTokenSequence(th, 0).get
    ts.move(0)

    val imports = allGlobalImports(doc)
    // first find if the package itself is imported eg.
    //   import org.apache.maven.model  or
    //   import org.apache.maven.{model=>mavenmodel}
    //If so, add prefix to declaration, rather than adding new import
    val packageName = fqn.substring(0, fqn.length - (missing.length + 1))
    val splitted = packageName.split('.')
    val lastPack = splitted.last
    val headPack = splitted.dropRight(1).mkString("""\.""")
    val impPattern = Pattern.compile(headPack + """\.\{""" + lastPack + """=>([\w]*)\}""")
    imports.foreach{p => println("-" + p._3 + "-")}
    val packMatch = imports.find{curr => curr._3.equals(packageName) || impPattern.matcher(curr._3).matches}
    if (packMatch != None) {
      val matcher = impPattern.matcher(packMatch.get._3)
      val toWrite = if (matcher.matches) {
        matcher.group(1)
      } else {
        packageName.split('.').last
      }
      val start = calcErrorStartPosition(offsetRange, missing, ts)
      if (start != -1) {
        simpleEdit(start, toWrite + ".", doc)
      }
    } else {
      // *then figure if a list of classes in a package is being imported eg.
      // import org.netbeans.api.lexer.{Language, Token}
      val listPattern = Pattern.compile(packageName + """\.\{([\w\,\s]*)\}""")
      val listMatch = imports.find((curr) => listPattern.matcher(curr._3).matches)
      if (listMatch != None) {
        val pos = listMatch.get._2 - 1 //-1 for the bracket?
        simpleEdit(pos, ", " + missing, doc)
      } else {
        // * if none of the above applies, add as single import
        val pos = imports.sortWith{(one, two) => one._3 < two._3}.find((curr) => curr._3 > fqn) match {
          case None =>
            if (imports.isEmpty) {
              findFirstPositionForImport(doc)
            } else {
              imports.last._2 + 1 // + 1 for newline
            }
          case Some(t) => t._1
        }
        if (pos != -1) {
          simpleEdit(pos, "import " + fqn + (if (imports.isEmpty) "\n\n" else "\n"), doc)
        }
      }
    }
  }

  private def calcErrorStartPosition(range: OffsetRange, name: String, ts: TokenSequence[TokenId]): Int = {
    ts.move(range.getStart)
    val includes: Set[TokenId] = Set(ScalaTokenId.Type, ScalaTokenId.Identifier)
    val end = range.getEnd
    var token = ScalaLexUtil.findNextIncluding(ts, includes)
    while (token.isDefined && ts.offset <= end) {
      if (name == token.get.text.toString) return ts.offset
      token = ScalaLexUtil.findNextIncluding(ts, includes)
    }
    -1
  }

  private def simpleEdit(position: Int, addition: String, doc : BaseDocument): Unit = {
    val edits = new EditList(doc)
    edits.replace(position, 0, addition, false, 0)
    edits.apply
  }

  def getImportCandidate(fo: FileObject, missingClass: String, range: OffsetRange): List[ImportCandidate] = {
    LOG.log(Level.FINEST, "Looking for class: " + missingClass)

    var result: List[ImportCandidate] = Nil

    //        ScalaIndex index = ScalaIndex.get(pResult.getSnapshot().getSource().getFileObject());
    //        Set<GsfElement> cslElements = index.getDeclaredTypes(missingClass, QuerySupport.Kind.PREFIX, pResult);
    //        for (GsfElement cslElement : cslElements) {
    //            javax.lang.model.element.Element element = cslElement.getElement();
    //            javax.lang.model.element.ElementKind ekind = element.getKind();
    //            if (ekind == javax.lang.model.element.ElementKind.CLASS ||
    //                    ekind == javax.lang.model.element.ElementKind.INTERFACE) {
    //                String fqnName = element.asType();
    //                //Icon icon = ElementIcons.getElementIcon(ek, null);
    //                int level = getImportanceLevel(fqnName);
    //
    //                ImportCandidate candidate = new ImportCandidate(missingClass, fqnName, icon, level);
    //                result.add(candidate);
    //
    //            }
    //        }

    val cpInfo = ScalaSourceUtil.getClasspathInfo(fo).getOrElse(return result)
    val typeNames = cpInfo.getClassIndex.getDeclaredTypes(missingClass, NameKind.SIMPLE_NAME,
                                                          EnumSet.allOf(classOf[ClassIndex.SearchScope]))
    val itr = typeNames.iterator
    while (itr.hasNext) {
      val typeName = itr.next
      typeName.getKind match {
        case ek@(javax.lang.model.element.ElementKind.CLASS | javax.lang.model.element.ElementKind.INTERFACE) =>
          val fqn = typeName.getQualifiedName
          val icon = ElementIcons.getElementIcon(ek, null)
          val level = getImportanceLevel(fqn)

          val candidate = new ImportCandidate(missingClass, fqn, range, icon, level)
          result = candidate :: result
        case _ =>
      }

    }

    result
  }

  def findFirstPositionForImport(doc: BaseDocument): Int = {
    val ts = ScalaLexUtil.getTokenSequence(doc, 1).getOrElse(return -1)
    var candidateOffset = -1
    var break = false
    while (ts.moveNext && !break) {
      ts.token.id match {
        case ScalaTokenId.Case | ScalaTokenId.Class | ScalaTokenId.Object | ScalaTokenId.Trait | ScalaTokenId.Import | ScalaTokenId.Sealed |
          ScalaTokenId.At | ScalaTokenId.Abstract | ScalaTokenId.Final | ScalaTokenId.Private | ScalaTokenId.Protected =>
          val lineBegin = Utilities.getRowStart(doc, ts.offset)
          candidateOffset = lineBegin
          break = true
        case _ =>
      }
    }
    candidateOffset
  }

}

/** for classOf[FixImportsHelper] */
class FixImportsHelper {}
