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

package org.netbeans.modules.scala.core.element

import java.io.IOException
import javax.lang.model.element.Element
import javax.swing.Icon
import javax.swing.text.BadLocationException
import org.netbeans.editor.BaseDocument
import org.netbeans.modules.csl.api.{ ElementHandle, ElementKind, Modifier, OffsetRange, HtmlFormatter }
import org.netbeans.modules.csl.api.UiUtils
import org.netbeans.modules.csl.spi.{ GsfUtilities, ParserResult }
import org.netbeans.modules.scala.core.ScalaSourceFile
import org.openide.filesystems.{ FileObject }
import org.openide.util.Exceptions

import scala.reflect.internal.Flags

import org.netbeans.api.language.util.ast.AstElementHandle
import org.netbeans.modules.scala.core.{ JavaSourceUtil, ScalaGlobal, ScalaSourceUtil, ScalaMimeResolver }

/**
 *
 * Wrapper scalac's symbol to AstElementHandle, these symbols are created by
 * scala compiler, that may be from class file instead of source files by
 * invoking ScalaGlobal#compilexxx methods.
 *
 * @author Caoyuan Deng
 */
trait ScalaElements { self: ScalaGlobal =>

  object ScalaElement {
    def apply(symbol: Symbol, pResult: ParserResult) = {
      new ScalaElement(symbol, pResult)
    }
  }

  class ScalaElement(val asymbol: Symbol, val parserResult: ParserResult) extends ScalaItem with AstElementHandle {
    import ScalaElement._

    symbol = asymbol

    private var _modifiers: Option[java.util.Set[Modifier]] = None
    private var _isInherited: Boolean = _
    private var _isImplicite: Boolean = _

    private var _path: String = _
    private var _doc: Option[BaseDocument] = None
    private var _offset: Int = _
    private var _javaElement: Option[Element] = None
    private var _isLoaded: Boolean = _

    private var _triedGetFo: Boolean = _

    def this(kind: ElementKind) = {
      this(null, null)
      this.kind = kind
    }

    def tpe: String = {
      ""
    }

    override def getIn: String = {
      try {
        symbol.owner.nameString
      } catch {
        case _: Throwable => ""
      }
    }

    override def getKind: ElementKind = {
      ScalaUtil.askForKind(symbol)
    }

    override def getMimeType: String = {
      ScalaMimeResolver.MIME_TYPE
    }

    override def getModifiers: java.util.Set[Modifier] = {
      if (!_modifiers.isDefined) {
        _modifiers = Some(ScalaUtil.askForModifiers(symbol))
      }
      _modifiers.get
    }

    override def getName: String = symbol.nameString

    override def qualifiedName: String = symbol.fullName

    override def signatureEquals(handle: ElementHandle): Boolean = {
      false
    }

    def getDocComment: String = {
      if (!isLoaded) load

      getDoc foreach { srcDoc =>
        if (isJava) {
          _javaElement foreach { x =>
            try {
              val docComment: String = JavaSourceUtil.getDocComment(JavaSourceUtil.getCompilationInfoForScalaFile(parserResult.getSnapshot.getSource.getFileObject), x)
              if (docComment.length > 0) {
                return new StringBuilder(docComment.length + 5).append("/**").append(docComment).append("*/").toString
              }
            } catch { case ex: IOException => Exceptions.printStackTrace(ex) }
          }
        } else {
          return ScalaSourceUtil.getDocComment(srcDoc, getOffset)
        }
      }

      ""
    }

    override def getFileObject(): FileObject = {
      if (!_triedGetFo) {
        fo getOrElse {
          fo = ScalaSourceUtil.getFileObject(parserResult, symbol) // try to get
          fo match {
            case Some(x) =>
              _path = x.getPath
              x
            case None =>
              _triedGetFo = true
              null
          }
        }
      } else fo getOrElse null
    }

    def getOffset(): Int = {
      if (!isLoaded) load

      if (isJava) {
        _javaElement foreach { x =>
          try {
            return JavaSourceUtil.getOffset(JavaSourceUtil.getCompilationInfoForScalaFile(parserResult.getSnapshot.getSource.getFileObject), x)
          } catch { case ex: IOException => Exceptions.printStackTrace(ex) }
        }
      } else {
        val pos = symbol.pos
        _offset = if (pos.isDefined) {
          pos.start
        } else {
          val fo = getFileObject
          if (fo != null) {
            ScalaSourceUtil.getOffset(parserResult, symbol, fo)
          } else 0
        }
      }

      _offset
    }

    override def getOffsetRange(result: ParserResult): OffsetRange = {
      throw new UnsupportedOperationException("Not supported yet.")
    }

    def getDoc: Option[BaseDocument] = {
      val srcFo = getFileObject
      if (srcFo ne null) {
        _doc match {
          case None => GsfUtilities.getDocument(srcFo, true) match {
            case null =>
            case x    => _doc = Some(x)
          }
          case _ =>
        }
        _doc
      } else None
    }

    private def isLoaded: Boolean = {
      if (_isLoaded) return true

      if (isJava) {
        _javaElement.isDefined
      } else {
        symbol.pos.isDefined
      }
    }

    private def load {
      if (isLoaded) return

      if (isJava) {
        _javaElement = JavaSourceUtil.getJavaElement(JavaSourceUtil.getCompilationInfoForScalaFile(parserResult.getSnapshot.getSource.getFileObject), symbol)
      } else {
        val fo = getFileObject
        if (fo ne null) {
          val srcFile = ScalaSourceFile.sourceFileOf(fo)
          try {
            /**
             * @Note by compiling the related source file, this symbol will
             * be automatically loaded next time, but the position/sourcefile
             * info is not updated for this symbol yet. But we can find the
             * position via the AST Tree, or use a tree visitor to update
             * all symbols Position
             */
            val root = askForSemantic(srcFile) match {
              case Some(root) =>
                root.findDfnMatched(symbol) match {
                  case Some(x) => _offset = x.idOffset(srcFile.tokenHierarchy)
                  case None    =>
                }
              case None =>
            }
          } catch { case ex: BadLocationException => Exceptions.printStackTrace(ex) }
        }
      }

      _isLoaded = true
    }

    def isJava: Boolean = {
      symbol hasFlag Flags.JAVA
    }

    def isDeprecated = {
      try {
        symbol.isDeprecated
      } catch {
        case _: Throwable => false
      }
    }
    def isDeprecated_=(b: Boolean) {

    }

    def isInherited = _isInherited
    def isInherited_=(b: Boolean) {
      _isInherited = b
    }

    def isEmphasize = !isInherited
    def isEmphasize_=(b: Boolean) {

    }

    def isImplicit = _isImplicite
    def isImplicit_=(b: Boolean) {
      _isImplicite = b
    }

    override def getIcon: Icon = UiUtils.getElementIcon(getKind, getModifiers)

    override def toString = {
      symbol.toString
    }

    def htmlFormat(fm: HtmlFormatter) {
      ScalaUtil.askForHtmlFormat(symbol, fm)
    }

    def sigFormat(fm: HtmlFormatter) {
      ScalaUtil.askForHtmlDef(symbol, fm)
    }

  }
}
