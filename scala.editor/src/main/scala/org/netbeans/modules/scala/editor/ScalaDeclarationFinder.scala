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

import javax.swing.text.Document
import org.netbeans.api.lexer.{TokenHierarchy, TokenSequence}
import org.netbeans.modules.csl.api.{DeclarationFinder, OffsetRange}
import org.netbeans.modules.csl.api.DeclarationFinder.DeclarationLocation
import org.netbeans.modules.csl.spi.ParserResult
//import org.netbeans.modules.parsing.spi.indexing.support.QuerySupport

import org.netbeans.modules.scala.core.ScalaParserResult
import org.netbeans.modules.scala.core.lexer.{ScalaLexUtil, ScalaTokenId}

/**
 *
 * @author Caoyuan Deng
 */
class ScalaDeclarationFinder extends DeclarationFinder {
  
  override def getReferenceSpan(document: Document, lexOffset: Int): OffsetRange = {
    val th = TokenHierarchy.get(document)

    val ts = ScalaLexUtil.getTokenSequence(th, lexOffset).getOrElse(return OffsetRange.NONE)
    ts.move(lexOffset)
    if (!ts.moveNext && !ts.movePrevious) {
      return OffsetRange.NONE
    }

    getReferenceSpan(ts, th, lexOffset) match {
      case OffsetRange.NONE if lexOffset == ts.offset =>
        // The caret is between two tokens, and the token on the right
        // wasn't linkable. Try on the left instead.
        if (ts.movePrevious) {
          getReferenceSpan(ts, th, lexOffset)
        } else OffsetRange.NONE
      case x => x
    }
  }

  private def getReferenceSpan(ts: TokenSequence[_], th: TokenHierarchy[_], lexOffset: Int): OffsetRange = {
    val token = ts.token
    token.id match {
      case ScalaTokenId.Identifier if token.length == 1 && token.text.toString == "," => OffsetRange.NONE
      case ScalaTokenId.Identifier | ScalaTokenId.This | ScalaTokenId.Super | ScalaTokenId.LArrow | ScalaTokenId.RArrow | ScalaTokenId.Wild =>
        new OffsetRange(ts.offset, ts.offset + token.length)
      case _ => OffsetRange.NONE
    }
  }

  override def findDeclaration(info: ParserResult, lexOffset: Int): DeclarationLocation = {
    val pr = info.asInstanceOf[ScalaParserResult]
    val global = pr.global
    import global._

    val root = pr.rootScope

    val astOffset = ScalaLexUtil.getAstOffset(pr, lexOffset)
    if (astOffset == -1) return DeclarationLocation.NONE

    val th = pr.getSnapshot.getTokenHierarchy

    val closest = root.findItemsAt(th, astOffset) match {
      case Nil => return DeclarationLocation.NONE
      case xs => ScalaUtil.importantItem(xs)
    }
        
    root.findDfnOf(closest) match {
      case Some(dfn) =>
        // is local
        val offset = dfn.idOffset(th)
        new DeclarationLocation(info.getSnapshot.getSource.getFileObject, offset, dfn)
      case None =>
        val ts = ScalaLexUtil.getTokenSequence(th, lexOffset).getOrElse(return DeclarationLocation.NONE)
        ts.move(lexOffset)
        if (!ts.moveNext && !ts.movePrevious) return DeclarationLocation.NONE
        
        val token = ts.token
        token.id match {
          case ScalaTokenId.Identifier | ScalaTokenId.SymbolLiteral =>
            root.findItemsAt(th, token.offset(th)) match {
              case Nil => DeclarationLocation.NONE
              case xs =>
                val item = ScalaUtil.importantItem(xs).asInstanceOf[ScalaItem]
                val remoteDfn = ScalaElement(item.symbol, info)
                val location = new DeclarationLocation(remoteDfn.getFileObject, remoteDfn.getOffset, remoteDfn)
                if (remoteDfn.getFileObject == null) {
                  // even fo is null, we should return a location to enable popping up a declaration string
                  location.setInvalidMessage("No source file found!")
                }
                location
            }
          case _ => DeclarationLocation.NONE
        }

        
    }
  }
}
