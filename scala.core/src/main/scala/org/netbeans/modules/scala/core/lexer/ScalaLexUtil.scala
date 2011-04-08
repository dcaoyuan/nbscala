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
 * Contributor(s):
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
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
 */
package org.netbeans.modules.scala.core.lexer

import java.io.IOException
import javax.swing.text.{BadLocationException, Document}

import org.netbeans.modules.csl.api.OffsetRange
import org.netbeans.api.lexer.{Token, TokenHierarchy, TokenId, TokenSequence}
import org.netbeans.editor.{BaseDocument}
import org.openide.loaders.{DataObject, DataObjectNotFoundException}

import scala.collection.mutable.ArrayBuffer

import org.netbeans.api.language.util.lex.LexUtil

/**
 * Utilities associated with lexing or analyzing the document at the
 * lexical level, unlike AstUtilities which is contains utilities
 * to analyze parsed information about a document.
 *
 * @author Caoyuan Deng
 * @author Tor Norbye
 */

object ScalaLexUtil extends LexUtil {

  override val LANGUAGE = ScalaTokenId.language

  override val WS_COMMENTS: Set[TokenId] =
    Set(ScalaTokenId.Ws,
        ScalaTokenId.Nl,
        ScalaTokenId.LineComment,
        ScalaTokenId.DocCommentStart,
        ScalaTokenId.DocCommentData,
        ScalaTokenId.DocCommentEnd,
        ScalaTokenId.BlockCommentStart,
        ScalaTokenId.BlockCommentEnd,
        ScalaTokenId.BlockCommentData,
        ScalaTokenId.CommentTag
    )

  override val WS: Set[TokenId] =
    Set(ScalaTokenId.Ws,
        ScalaTokenId.Nl
    )

  /**
   * Tokens that should cause indentation of the next line. This is true for all {@link #END_PAIRS},
   * but also includes tokens like "else" that are not themselves matched with end but also contribute
   * structure for indentation.
   *
   */
  override val INDENT_WORDS: Set[TokenId] =
    Set(ScalaTokenId.Class,
        ScalaTokenId.Object,
        ScalaTokenId.Trait,
        ScalaTokenId.Do,
        ScalaTokenId.For,
        ScalaTokenId.While,
        ScalaTokenId.Case,
        ScalaTokenId.If,
        ScalaTokenId.Else
    )

  override val BLOCK_COMMENTS: Set[TokenId] =
    Set(ScalaTokenId.BlockCommentStart,
        ScalaTokenId.BlockCommentEnd,
        ScalaTokenId.BlockCommentData,
        ScalaTokenId.CommentTag
    )

  override val DOC_COMMENTS: Set[TokenId] =
    Set(ScalaTokenId.DocCommentStart,
        ScalaTokenId.DocCommentEnd,
        ScalaTokenId.DocCommentData,
        ScalaTokenId.CommentTag
    )

  override val LINE_COMMENTS: Set[TokenId] =
    Set(
      ScalaTokenId.LineComment
    )

  override val WHITE_SPACE: TokenId = ScalaTokenId.Ws
  override val NEW_LINE: TokenId = ScalaTokenId.Nl
  override val LPAREN: TokenId = ScalaTokenId.LParen
  override val RPAREN: TokenId = ScalaTokenId.RParen

  val PotentialIdTokens: Set[TokenId] =
    Set(ScalaTokenId.Identifier,
        ScalaTokenId.True,
        ScalaTokenId.False,
        ScalaTokenId.Null,
        ScalaTokenId.SymbolLiteral,
        ScalaTokenId.IntegerLiteral,
        ScalaTokenId.FloatingPointLiteral,
        ScalaTokenId.StringLiteral,
        ScalaTokenId.CharacterLiteral,
        ScalaTokenId.XmlAttName,
        ScalaTokenId.XmlAttValue,
        ScalaTokenId.XmlCDData,
        ScalaTokenId.XmlCDEnd,
        ScalaTokenId.XmlComment,
        ScalaTokenId.XmlSTagName,
        ScalaTokenId.XmlSTagName,
        ScalaTokenId.XmlCharData,
        ScalaTokenId.LArrow,
        ScalaTokenId.Wild
    )

  override def getDocCommentRangeBefore(th: TokenHierarchy[_], lexOffset: Int): OffsetRange = {
    val ts = getTokenSequence(th, lexOffset).getOrElse(return OffsetRange.NONE)

    ts.move(lexOffset)
    var offset = -1
    var endOffset = -1
    var done = false
    while (ts.movePrevious && !done) {
      ts.token.id match {
        case ScalaTokenId.DocCommentEnd =>
          val token = ts.offsetToken
          endOffset = token.offset(th) + token.length
        case ScalaTokenId.DocCommentStart =>
          val token = ts.offsetToken
          offset = token.offset(th)
          done = true
        case id if !isWsComment(id) && !isKeyword(id) =>
          ts.moveNext // recheck from this id
          findAnnotationBwd(ts) match {
            case None => done = true
            case Some(x) => // ts is moved to '@' now
          }
        case _ =>
      }
    }

    if (offset != -1 && endOffset != -1) {
      new OffsetRange(offset, endOffset)
    } else OffsetRange.NONE
  }


  private def findMultilineRange(ts: TokenSequence[TokenId]): OffsetRange = {
    val startOffset = ts.offset
    val token = ts.token
    var id = token.id
    id match {
      case ScalaTokenId.Else =>
        ts.moveNext
        id = ts.token.id
      case ScalaTokenId.If | ScalaTokenId.For | ScalaTokenId.While =>
        ts.moveNext
        if (!skipParenthesis(ts, false)) {
          return OffsetRange.NONE
        }
        id = ts.token.id
      case _ =>
        return OffsetRange.NONE
    }

    var eolFound = false
    var lastEolOffset = ts.offset
    // skip whitespaces and comments
    if (isWsComment(id)) {
      if (ts.token.id == NEW_LINE) {
        lastEolOffset = ts.offset
        eolFound = true
      }
      while (ts.moveNext && isWsComment(ts.token.id)) {
        if (ts.token.id == NEW_LINE) {
          lastEolOffset = ts.offset
          eolFound = true
        }
      }
    }
    // if we found end of sequence or end of line
    if (ts.token == null || (ts.token.id != ScalaTokenId.LBrace && eolFound)) {
      new OffsetRange(startOffset, lastEolOffset);
    } else OffsetRange.NONE
  }

  def getMultilineRange(doc :BaseDocument, ts :TokenSequence[TokenId]): OffsetRange = {
    val index = ts.index
    val offsetRange = findMultilineRange(ts)
    ts.moveIndex(index)
    ts.moveNext
    offsetRange
  }

  /** Some AstItems have Xml Nl etc type of idToken, here we just pick following as proper one */
  def isProperIdToken(id: TokenId): Boolean = {
    id match {
      case ScalaTokenId.Identifier | ScalaTokenId.This | ScalaTokenId.Super | ScalaTokenId.Wild => true
      case _ => false
    }
  }

  def findImportPrefix(th: TokenHierarchy[_], lexOffset: Int): List[Token[TokenId]] = {
    val ts = getTokenSequence(th, lexOffset).getOrElse(return Nil)
    ts.move(lexOffset)
    var lbraceMet = false
    var lbraceExpected = false
    var exactBehindComma = false
    var paths = new ArrayBuffer[Token[TokenId]]
    while (ts.isValid && ts.movePrevious) {
      val token = ts.token
      token.id match {
        case ScalaTokenId.Import =>
          if (!lbraceExpected || lbraceExpected && lbraceMet) {
            // * since we are looking backward, the result is reversed
            return paths.toList
          }
        case ScalaTokenId.Dot =>
          paths += token
        case ScalaTokenId.Identifier =>
          paths += token
        case ScalaTokenId.LBrace =>
          if (lbraceMet) {
            // * we can only meet LBrace one time
            return Nil
          }
          lbraceMet = true
          if (!paths.isEmpty) {
            // * keep first met idToken only
            val idToken = paths(0)
            paths.clear
            if (!exactBehindComma) {
              paths += idToken
            }
          }
        case ScalaTokenId.Comma =>
          lbraceExpected = true
          if (paths.isEmpty) {
            exactBehindComma = true
          }
        case id if isWsComment(id) =>
        case _ => return Nil
      }
    }

    Nil
  }

  case class ImportTokens(start: Token[TokenId], end: Token[TokenId], qual: List[Token[TokenId]], selectors: List[(Token[TokenId], Token[TokenId])])
  val NullImportTokens = ImportTokens(null, null, Nil, Nil)
  def findImportAt(th: TokenHierarchy[_], offsetInImporting: Int): ImportTokens = {
    val ts = getTokenSequence(th, offsetInImporting).getOrElse(return NullImportTokens)
    ts.move(offsetInImporting)
    ts.moveNext
    val importToken = findPrevious(ts, ScalaTokenId.Import).getOrElse(return NullImportTokens)
    var start = if (importToken.isFlyweight) ts.offsetToken else importToken
    var end = start

    var qual: List[Token[TokenId]] = Nil
    var selectors: List[(Token[TokenId], Token[TokenId])] = Nil

    var inBrace = false
    var rarrowMet = false
    var newlineAllowed = false
    while (ts.isValid && ts.moveNext) {
      val token = ts.token match {
        case x if x.isFlyweight => ts.offsetToken
        case x => x
      }

      token.id match {
        case ScalaTokenId.Identifier | ScalaTokenId.Wild =>
          end = token
          if (inBrace) {
            if (rarrowMet) {
              selectors match {
                case (x, _) :: xs =>
                  selectors = (x, token) :: xs
                  rarrowMet = false
                case _ =>
              }
            } else {
              selectors = (token, token) :: selectors
            }
          } else {
            newlineAllowed = false
            qual = token :: qual
          }
        case ScalaTokenId.Dot =>
          newlineAllowed = true
        case ScalaTokenId.LBrace =>
          if (inBrace) return NullImportTokens // * we can only meet LBrace one time
          inBrace = true
          newlineAllowed = true
        case ScalaTokenId.RBrace =>
          end = token
          newlineAllowed = false
        case ScalaTokenId.RArrow =>
          rarrowMet = true
        case ScalaTokenId.Comma =>
        case ScalaTokenId.Nl =>
          if (!newlineAllowed) {
            if (selectors.isEmpty) {
              selectors = (qual.head, qual.head) :: selectors
              qual = qual.tail
            }
            return ImportTokens(start, end, qual.reverse, selectors.reverse)
          }
        case id if isWsComment(id) =>
        case _ => return NullImportTokens
      }
    }

    NullImportTokens
  }

  /** @Require: move ts to `else` token first */
  def findMatchedIfOfElse(ts: TokenSequence[TokenId]) = {
    assert(ts.token.id == ScalaTokenId.Else, "Should move TokenSequence to `else` token first!")

    while (ts.movePrevious) {
      ts.token.id match {
        case ScalaTokenId.If =>
        
      }
    }
  }

  /**
   * @return annotation id token or None
   *         if found, ts will be located to '@'
   */
  def findAnnotationBwd(ts: TokenSequence[TokenId]): Option[Token[TokenId]] = {
    var collector: List[Token[TokenId]] = Nil
    var atExpected = false
    var break = false
    while (ts.movePrevious && !break) {
      val token = ts.token
      token.id match {
        case id if ScalaLexUtil.isWsComment(id) =>
        case ScalaTokenId.At =>
          collector = token :: collector
        case ScalaTokenId.Identifier =>
          collector = token :: collector
        case ScalaTokenId.RParen =>
          ScalaLexUtil.findPairBwd(ts, ScalaTokenId.LParen, ScalaTokenId.RParen)
        case ScalaTokenId.RBrace =>
          ScalaLexUtil.findPairBwd(ts, ScalaTokenId.LBrace, ScalaTokenId.RBrace)
        case ScalaTokenId.RBracket =>
          ScalaLexUtil.findPairBwd(ts, ScalaTokenId.LBracket, ScalaTokenId.RBracket)
        case _ => break = true
      }

      collector map {_.id} match {
        case List(ScalaTokenId.At, ScalaTokenId.Identifier) => return Some(collector.last)
        case List(_, _, _) => break = true // collect no more than 3 tokens
        case _ =>
      }
    }

    None
  }
}

