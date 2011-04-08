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

import javax.swing.ImageIcon
import javax.swing.text.BadLocationException
import org.netbeans.modules.csl.api.{CompletionProposal, ElementHandle, ElementKind, HtmlFormatter, Modifier,OffsetRange}
import org.netbeans.modules.csl.spi.ParserResult
import org.netbeans.modules.scala.core.ScalaGlobal
import org.openide.filesystems.FileObject
import org.openide.util.{Exceptions, ImageUtilities}

import org.netbeans.api.language.util.ast.{AstElementHandle}

/**
 *
 * @author Caoyuan Deng
 */
abstract class ScalaCompletionProposals {

  val global: ScalaGlobal
  import global._

  object ScalaCompletionProposal {
    val KEYWORD = "org/netbeans/modules/scala/editor/resources/scala16x16.png" //NOI18N
    val keywordIcon = ImageUtilities.loadImageIcon(KEYWORD, false)
  }

  abstract class ScalaCompletionProposal(element: AstElementHandle, completer: ScalaCodeCompleter
  ) extends CompletionProposal {

    def getAnchorOffset: Int = completer.anchor

    override def getName: String = element.getName

    override def getInsertPrefix: String = getName

    override def getSortText: String = {
      val name = getName
      val order = name.charAt(0) match {
        case c if c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z' => "0"
        case _ => "1"
      }
      order + name
    }

    def getSortPrioOverride: Int = {
      0
    }

    def getElement: ElementHandle = element

    def getKind: ElementKind = {
      getElement match {
        case x: ScalaDfn     if x.symbol.isGetter => ElementKind.FIELD
        case x: ScalaElement if x.symbol.isGetter => ElementKind.FIELD
        case x => x.getKind
      }
    }

    def getIcon: ImageIcon = null

    def getLhsHtml(fm: HtmlFormatter): String = {
      val emphasize = !element.isInherited
      val strike = element.isDeprecated

      if (emphasize) fm.emphasis(true)
      if (strike)    fm.deprecated(true)
      
      val kind = getKind
      fm.name(kind, true)
      fm.appendText(getName)
      fm.name(kind, false)

      if (strike)    fm.deprecated(false)
      if (emphasize) fm.emphasis(false)

      fm.getText
    }

    override def getRhsHtml(fm: HtmlFormatter): String = {
      element match {
        case x: ScalaElement =>
          val sym = x.symbol

          ScalaUtil.completeIfWithLazyType(sym)

          fm.`type`(true)
          val retType = try {
            sym.tpe match {
              case null => null
              case x => x.resultType
            }
          } catch {case ex => ScalaGlobal.resetLate(global, ex); null}

          if (retType != null && !sym.isConstructor) {
            fm.appendText(ScalaUtil.typeToString(retType))
          }
          fm.`type`(false)
        case _ =>
      }

      fm.getText
    }

    override def getModifiers: java.util.Set[Modifier] = element.getModifiers

    override def toString: String = {
      var cls = this.getClass.getName
      cls = cls.substring(cls.lastIndexOf('.') + 1)

      cls + "(" + getKind + "): " + getName
    }

    def isSmart: Boolean = false

    override def getCustomInsertTemplate: String = null
  }

  case class FunctionProposal(element: AstElementHandle, completer: ScalaCodeCompleter
  ) extends ScalaCompletionProposal(element, completer) {
    private val sym = element.asInstanceOf[ScalaElement].symbol


    override def getInsertPrefix: String = getName

    override def getKind: ElementKind = ElementKind.METHOD

    override def getLhsHtml(fm: HtmlFormatter): String = {
      val strike = element.isDeprecated
      val emphasize = !element.isInherited
      val preStar = element.isImplicit
      
      if (preStar)   fm.appendHtml("<u>")
      if (strike)    fm.deprecated(true)
      if (emphasize) fm.emphasis(true)

      val kind = getKind
      fm.name(kind, true)
      fm.appendText(getName)
      fm.name(kind, false)

      if (emphasize) fm.emphasis(false)
      if (strike)    fm.deprecated(false)
      if (preStar)   fm.appendHtml("</u>")

      val typeParams = try {
        sym.tpe match {
          case null => Nil
          case tpe => tpe.typeParams
        }
      } catch {case ex => ScalaGlobal.resetLate(completer.global, ex); Nil}
      if (!typeParams.isEmpty) {
        fm.appendHtml("[")
        fm.appendText(typeParams map (_.nameString) mkString(", "))
        fm.appendHtml("]")
      }

      try {
        sym.tpe match {
          case MethodType(params, resultType) =>
            if (!params.isEmpty) {
              fm.appendHtml("(") // NOI18N
              val itr = params.iterator
              while (itr.hasNext) {
                val param = itr.next
              
                fm.parameters(true)
                fm.appendText(param.nameString)
                fm.parameters(false)

                fm.appendText(": ")

                val paramTpe = try {
                  val t = param.tpe
                  if (t != null) {
                    t.toString
                  } else "<unknown>"
                } catch {case ex => ScalaGlobal.resetLate(completer.global, ex); "<unknown>"}

                fm.`type`(true)
                fm.appendText(paramTpe)
                fm.`type`(false)
                
                if (itr.hasNext) fm.appendText(", ") // NOI18N
              }
              fm.appendHtml(")") // NOI18N
            }
          case _ =>
        }
      } catch {case ex => ScalaGlobal.resetLate(completer.global, ex)}

      fm.getText
    }

    def getInsertParams: List[String] = {
      try {
        sym.tpe match {
          case MethodType(params, resultType) => params map (_.nameString)
          case _ => Nil
        }
      } catch {case ex => ScalaGlobal.resetLate(completer.global, ex); Nil}
    }

    override def getCustomInsertTemplate: String = {
      val sb = new StringBuilder

      val insertPrefix = getInsertPrefix
      sb.append(insertPrefix)

      val params = getInsertParams
      if (params.isEmpty) {
        return sb.toString
      }

      sb.append("(")

      var i = 1
      val itr = params.iterator
      while (itr.hasNext) {
        val paramDesc = itr.next
        sb.append("${") //NOI18N

        sb.append("scala-cc-") // NOI18N
        i += 1
        sb.append(i)
        
        sb.append(" default=\"") // NOI18N
        paramDesc.indexOf(':') match {
          case -1 => sb.append(paramDesc)
          case typeIdx => sb.appendAll(paramDesc.toArray, 0, typeIdx)
        }
        sb.append("\"") // NOI18N

        sb.append("}") //NOI18N

        if (itr.hasNext) {
          sb.append(", ") //NOI18N
        }
      }

      sb.append(")")
      sb.append("${cursor}") // NOI18N

      // Facilitate method parameter completion on this item
      try {
        //ScalaCodeCompleter.callLineStart = Utilities.getRowStart(completer.doc, completer.anchor)
        //ScalaCodeCompletion.callMethod = function;
      } catch {case ble: BadLocationException => Exceptions.printStackTrace(ble)}

      sb.toString
    }
  }

  case class KeywordProposal(keyword: String, description: String, completer: ScalaCodeCompleter
  ) extends ScalaCompletionProposal(null, completer) {

    override def getName: String = keyword

    override def getKind: ElementKind = ElementKind.KEYWORD

    override def getLhsHtml(fm: HtmlFormatter): String = {
      val kind = getKind
      fm.name(kind, true)
      fm.appendHtml(getName)
      fm.name(kind, false)

      fm.getText
    }

    override def getRhsHtml(fm: HtmlFormatter): String = {
      if (description != null) {
        fm.appendText(description)

        fm.getText
      } else null
    }

    override def getIcon: ImageIcon = ScalaCompletionProposal.keywordIcon

    override def getModifiers: java.util.Set[Modifier] = java.util.Collections.emptySet[Modifier]

    override def getElement: ElementHandle = {
      PseudoElement(keyword, ElementKind.KEYWORD) // For completion documentation
    }

    override def isSmart: Boolean = false
  }

  case class PlainProposal(element: AstElementHandle, completer: ScalaCodeCompleter
  ) extends ScalaCompletionProposal(element, completer) {}

  case class PackageItem(element: AstElementHandle, completer: ScalaCodeCompleter
  ) extends ScalaCompletionProposal(element, completer) {

    override def getKind: ElementKind = ElementKind.PACKAGE

    override def getName: String = {
      val name = element.getName
      name.lastIndexOf('.') match {
        case -1 => name
        case lastDot => name.substring(lastDot + 1, name.length)
      }
    }

    override def getLhsHtml(formatter: HtmlFormatter): String = {
      val kind = getKind
      formatter.name(kind, true)
      formatter.appendText(getName)
      formatter.name(kind, false)

      formatter.getText
    }

    override def getRhsHtml(fm: HtmlFormatter): String = null

    override def isSmart: Boolean = true
  }

  case class TypeProposal(element: AstElementHandle, completer: ScalaCodeCompleter
  ) extends ScalaCompletionProposal(element, completer) {

    override def getKind: ElementKind = ElementKind.CLASS

    override def getName: String = {
      val name = element.qualifiedName
      name.lastIndexOf('.') match {
        case -1 => name
        case i => name.substring(i + 1, name.length)
      }
    }

    override def getLhsHtml(fm: HtmlFormatter): String = {
      val kind = getKind
      val strike = element.isDeprecated
      if (strike) fm.deprecated(true)
      
      fm.name(kind, true)
      fm.appendText(getName)
      fm.name(kind, false)
      
      if (strike) fm.deprecated(false)

      fm.getText
    }

    override def getRhsHtml(fm: HtmlFormatter): String = {
      val qname = element.qualifiedName
      val in = qname.lastIndexOf('.') match {
        case -1 => ""
        case i => qname.substring(0, i)
      }
      fm.appendText(in)
      fm.getText
    }
  }

  case class PseudoElement(name: String, kind: ElementKind) extends ElementHandle {

    def getFileObject: FileObject = null

    def getMimeType: String = "text/x-scala"

    def getName :String = name

    def getIn: String = null

    def getKind: ElementKind = kind

    def getModifiers: java.util.Set[Modifier] = java.util.Collections.emptySet[Modifier]

    def signatureEquals(handle: ElementHandle): Boolean = false

    def getOffsetRange(result: ParserResult): OffsetRange = OffsetRange.NONE
  }

}
