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
 * Portions Copyrighted 2008 Sun Microsystems, Inc.
 */
package org.netbeans.api.language.util.ast

import javax.swing.Icon
import org.netbeans.api.lexer.{Token, TokenId, TokenHierarchy}
import org.netbeans.modules.csl.api.{ElementKind, ElementHandle, Modifier, OffsetRange}
import org.netbeans.modules.csl.api.UiUtils
import org.netbeans.modules.csl.spi.{ParserResult}
import org.openide.filesystems.{FileObject}

/**
 *
 * @author Caoyuan Deng
 */
trait AstItem extends ForElementHandle {

  type S  // type of symbol
  type T  // type of symbol's type

  def make(idToken: Token[TokenId], kind: ElementKind, fo: Option[FileObject]): Unit = {
    this.idToken = idToken
    this.kind = kind
    this.fo = fo
  }

  var resultType: T = _
  
  /**
   * @Note:
   * 1. Not all AstItem has pickToken, such as Expr etc.
   * 2. Due to strange behavior of StructureAnalyzer, we can not rely on
   *    pickToken's text as name, pickToken may be <null> and pickToken.text()
   *    will return null when an Identifier token modified, seems sync issue
   */
  private var _symbol: S = _
  private var _idToken: Token[TokenId] = _
  private var _name: String = _
  private var _enclosingScope: AstScope = _
  private var _properties: Map[String, Any] = Map()
  var kind: ElementKind = ElementKind.OTHER
  var fo: Option[FileObject] = None

  def symbol = _symbol
  def symbol_=(symbol: S) = {
    this._symbol = symbol
  }

  def idToken = _idToken
  def idToken_=(aidToken: Token[TokenId]) = {
    this._idToken = aidToken
    name = idToken.text.toString
  }

  def name = _name
  def name_=(name: String) = this._name = name
  def name_=(idToken: Token[TokenId]): Unit = {
    if (idToken == null) {
      _name = "" // should not happen?
      return
    }
        
    try {
      _name = idToken.text.toString
    } catch {
      case ex: Exception =>
        val l = idToken.length
        val sb = new StringBuilder(l)
        var i = 0
        while (i < l) {
          sb.append(" ")
          i += 1
        }
        _name = sb.toString
        println("NPE in AstItem#name:" + idToken.id)
    }
  }

  def idOffset(th: TokenHierarchy[_]): Int = {
    idToken.offset(th)
  }

  def idEndOffset(th: TokenHierarchy[_]): Int = {
    idToken.offset(th) + idToken.length
  }

  def binaryName = name

  def enclosingDfn[A <: AstDfn](clazz: Class[A]): Option[A] = {
    enclosingScope.enclosingDfn(clazz)
  }

  /**
   * @Note: enclosingScope will be set when call
   *   {@link AstScope#addElement(Element)} or {@link AstScope#addMirror(Mirror)}
   */
  def enclosingScope_=(enclosingScope: AstScope): AstItem = {
    if (enclosingScope != null) {
      this._enclosingScope = enclosingScope
    }
    this
  }

  /**
   * @return the scope that encloses this item
   */
  def enclosingScope: AstScope = {
    assert(_enclosingScope != null, name + ": Each item should set enclosing scope!, except native TypeRef")
    _enclosingScope
  }

  final def rootScope: AstRootScope = enclosingScope.root

  final def samePlaceSymbols: Seq[AstItem#S] = {
    samePlaceItems map (_.symbol)
  }
  
  final def samePlaceItems: Seq[AstItem] = {
    rootScope.samePlaceItems(this)
  }

  def property(k: String): Option[Any] = {
    _properties.get(k)
  }

  def property(k: String, v: Any): Unit = {
    _properties += (k -> v)
  }

  override def toString = {
    symbol.toString
  }
}

/**
 * Wrap functions that implemented some ElementHandle's methods
 */
trait ForElementHandle {self: AstItem =>
    
  def getMimeType: String

  def getName = self.name

  def getIn: String = ""

  def getOffsetRange(result: ParserResult): OffsetRange = OffsetRange.NONE

  def getKind: ElementKind = self.kind

  def signatureEquals(handle: ElementHandle) = false

  def getModifiers: java.util.Set[Modifier] = java.util.Collections.emptySet[Modifier]

  def getIcon: Icon = UiUtils.getElementIcon(getKind, getModifiers)
}
