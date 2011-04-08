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
package org.netbeans.api.language.util.ast

import org.netbeans.api.lexer.{Token, TokenId, TokenHierarchy}
import org.netbeans.editor.{BaseDocument}
import org.netbeans.modules.csl.api.{ElementKind, Modifier, OffsetRange}
import org.netbeans.modules.csl.spi.{GsfUtilities, ParserResult}
import org.openide.filesystems.FileObject

/**
 * AST Definition
 * 
 * Represents a program element such as a package, class, or method. Each element 
 * represents a static, language-level construct (and not, for example, a runtime 
 * construct of the virtual machine). 
 * 
 * @author Caoyuan Deng
 */
trait AstDfn extends AstItem with AstElementHandle {
  private var _bindingScope: AstScope = _
  
  def make(_idToken: Token[TokenId],
           _kind: ElementKind,
           abindingScope: AstScope,
           _fo: Option[FileObject]) = {

    super.make(_idToken, _kind, _fo)
    // we allow _bindingScope to be set later
    if (abindingScope != null) {
      _bindingScope = abindingScope
      _bindingScope.bindingDfn = Some(this)
    }
  }
   
  protected var modifiers: Option[java.util.Set[Modifier]] = None

  override def getFileObject: FileObject = fo.getOrElse(null)

  override def getModifiers: java.util.Set[Modifier] = {
    modifiers.getOrElse(java.util.Collections.emptySet[Modifier])
  }

  override def getOffsetRange(pResult: ParserResult): OffsetRange = {
    pResult.getSnapshot.getTokenHierarchy match {
      case null => OffsetRange.NONE
      case th =>
        val offset = boundsOffset(th)
        val endOffset = boundsEndOffset(th)
        if (offset >= 0 && endOffset >= offset) {
          new OffsetRange(boundsOffset(th), boundsEndOffset(th))
        } else {
          OffsetRange.NONE
        }
    }
  }

  def tpe: String = {
    "NoType"
  }

  def enclosedElements: Seq[AstDfn] = {
    if (_bindingScope != null) {
      _bindingScope.dfns
    } else Nil
  }

  def enclosingDfn: Option[AstDfn] = enclosingScope.bindingDfn

  def bindingScope: AstScope = {
    assert(_bindingScope != null, toString + ": Each definition should set binding scope!")
    _bindingScope
  }

  def boundsOffset(th: TokenHierarchy[_]): Int = {
    bindingScope.boundsOffset(th)
  }

  def boundsEndOffset(th: TokenHierarchy[_]): Int = {
    bindingScope.boundsEndOffset(th)
  }

  def range(th: TokenHierarchy[_]): OffsetRange = {
    bindingScope.range(th)
  }

  def mayEqual(dfn: AstDfn): Boolean = {
    this == dfn
    //return getName().equals(def.getName())
  }

  def getDoc: Option[BaseDocument] = {
    fo match {
      case Some(x) => GsfUtilities.getDocument(x, true) match {
          case null => None
          case docx => Some(docx)
        }
      case None => None
    }
  }

  def packageName: String = {
    null
  }

  def qualifiedName: String = {
    null
  }

  var isInherited  = false
  var isDeprecated = false
  var isEmphasize  = false
  var isImplicit   = false

  def isReferredBy(ref: AstRef): Boolean

  override def toString = {
    "Dfn: " + "name=" + name + ", idToken=" + idToken + ", kind=" + kind + ", sym=" + symbol + ", mods" + getModifiers
  }
}
