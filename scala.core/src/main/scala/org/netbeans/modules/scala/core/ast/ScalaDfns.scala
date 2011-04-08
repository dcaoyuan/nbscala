/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved.
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
package org.netbeans.modules.scala.core.ast

import org.netbeans.api.lexer.{Token, TokenId, TokenHierarchy}
import org.netbeans.modules.csl.api.{ElementKind, HtmlFormatter, Modifier}
import org.openide.filesystems.FileObject

import org.netbeans.api.language.util.ast.{AstDfn, AstRef, AstScope}
import org.netbeans.modules.scala.core.{ScalaGlobal, ScalaMimeResolver, ScalaSourceUtil}

/**
 * Scala AstDfn special functions, which will be enabled in ScalaGlobal
 */
trait ScalaDfns {self: ScalaGlobal =>

  object ScalaDfn {
    def apply(symbol: Symbol,
              idToken: Token[TokenId],
              kind: ElementKind,
              bindingScope: AstScope,
              fo: Option[FileObject]) = {
      new ScalaDfn(symbol, idToken, kind, bindingScope, fo)
    }
  }
  
  class ScalaDfn(asymbol: Symbol,
                 aidToken: Token[TokenId],
                 akind: ElementKind,
                 abindingScope: AstScope,
                 afo: Option[FileObject]
  ) extends ScalaItem with AstDfn {

    make(aidToken, akind, abindingScope, afo)

    symbol = asymbol

    override def getMimeType: String = ScalaMimeResolver.MIME_TYPE

    override def getModifiers: java.util.Set[Modifier] = {
      if (!modifiers.isDefined) {
        modifiers = Some(ScalaUtil.getModifiers(symbol))
      }
      modifiers.get
    }

    override def qualifiedName: String = symbol.fullName

    /** @Note: do not call ref.getKind here, which will recursively call this function, use ref.kind ! */
    def isReferredBy(ref: AstRef): Boolean = {
      if (ref.getName == getName) {
        //            if ((symbol.value.isClass || getSymbol().isModule()) && ref.isSameNameAsEnclClass()) {
        //                return true;
        //            }
        ref.symbol == symbol
      } else false
    }

    def getDocComment: String = {
      val srcDoc = getDoc.getOrElse(return "")
      TokenHierarchy.get(srcDoc) match {
        case null => return ""
        case th => ScalaSourceUtil.getDocComment(srcDoc, idOffset(th))
      }
    }

    def htmlFormat(fm: HtmlFormatter): Unit = {
      ScalaUtil.htmlFormat(symbol, fm)
    }

    def sigFormat(fm: HtmlFormatter) : Unit = {
      try {
        fm.appendHtml("<i>")
        fm.appendText(symbol.enclClass.fullName)
        fm.appendHtml("</i><p>")
        ScalaUtil.htmlDef(symbol, fm)
      } catch {case ex => ScalaGlobal.resetLate(self, ex)}
    }
  }

}
