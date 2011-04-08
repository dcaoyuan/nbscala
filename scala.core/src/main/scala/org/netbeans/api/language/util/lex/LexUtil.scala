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
package org.netbeans.api.language.util.lex

import java.io.IOException
import javax.swing.text.{BadLocationException, Document}

import org.netbeans.modules.csl.api.OffsetRange
import org.netbeans.api.lexer.{Language, Token, TokenHierarchy, TokenId, TokenSequence}
import org.netbeans.editor.{BaseDocument, Utilities}
import org.netbeans.modules.parsing.spi.Parser
import org.openide.cookies.EditorCookie
import org.openide.filesystems.{FileObject, FileUtil}
import org.openide.loaders.{DataObject, DataObjectNotFoundException}
import org.openide.util.Exceptions

import scala.collection.mutable.{Stack}


/**
 *
 * @author Caoyuan Deng
 */
trait LexUtil {
  val LANGUAGE: Language[TokenId]
  val WS_COMMENTS: Set[TokenId]
  val WS: Set[TokenId]
  val DOC_COMMENTS: Set[TokenId]
  val BLOCK_COMMENTS: Set[TokenId]
  val LINE_COMMENTS: Set[TokenId]

  /**
   * Tokens that should cause indentation of the next line. This is true for all {@link #END_PAIRS},
   * but also includes tokens like "else" that are not themselves matched with end but also contribute
   * structure for indentation.
   *
   */
  val INDENT_WORDS: Set[TokenId]
  /** Tokens that match a corresponding END statement. Even though while, unless etc.
   * can be statement modifiers, those luckily have different token ids so are not a problem
   * here.
   */
  val END_PAIRS: Set[TokenId] = Set[TokenId]()

  val WHITE_SPACE: TokenId
  val NEW_LINE: TokenId
  val LPAREN: TokenId
  val RPAREN: TokenId

  def getDocCommentRangeBefore(th: TokenHierarchy[_], lexOffset: Int): OffsetRange

  /**
   * Return the comment sequence (if any) for the comment prior to the given offset.
   */
  //    def TokenSequence<? extends FortressCommentTokenId> getCommentFor(doc:BaseDocument, offset:Int) {
  //        TokenSequence<?extends ScalaTokenId> jts = getTokenSequence(doc, offset);
  //        if (jts == null) {
  //            return null;
  //        }
  //        jts.move(offset);
  //
  //        while (jts.movePrevious()) {
  //            id:TokenId = jts.token().id();
  //            if (id == ScalaTokenId.BLOCK_COMMENT) {
  //                return jts.embedded(FortressCommentTokenId.language());
  //            } else if (id != ScalaTokenId.WHITESPACE && id != ScalaTokenId.EOL) {
  //                return null;
  //            }
  //        }
  //
  //        return null;
  //    }
  
  /** For a possibly generated offset in an AST, return the corresponding lexing/true document offset */
  def getLexerOffset(info: Parser.Result, astOffset: Int): Int = {
    if (info != null) {
      info.getSnapshot.getOriginalOffset(astOffset)
    } else {
      astOffset
    }
  }

  def getLexerOffsets(info: Parser.Result, astRange: OffsetRange): OffsetRange = {
    if (info != null) {
      val rangeStart = astRange.getStart
      val start = info.getSnapshot.getOriginalOffset(rangeStart)
      if (start == rangeStart) {
        astRange
      } else if (start == -1) {
        OffsetRange.NONE
      } else {
        // Assumes the translated range maintains size
        new OffsetRange(start, start + astRange.getLength)
      }
    } else {
      astRange
    }
  }

  def getAstOffset(pResult: Parser.Result, lexOffset: Int): Int = {
    if (pResult != null) {
      pResult.getSnapshot.getEmbeddedOffset(lexOffset)
    } else lexOffset
  }

  def getAstOffsets(pResult: Parser.Result, lexicalRange: OffsetRange): OffsetRange = {
    if (pResult != null) {
      val rangeStart = lexicalRange.getStart
      pResult.getSnapshot.getEmbeddedOffset(rangeStart) match {
        case `rangeStart` => lexicalRange
        case -1 => OffsetRange.NONE
        case start =>
          // Assumes the translated range maintains size
          new OffsetRange(start, start + lexicalRange.getLength)
      }
    } else lexicalRange
  }

  /** Find the token hierarchy (in case it's embedded in something else at the top level */
  final def getTokenHierarchy(doc: BaseDocument, offset: Int): Option[TokenHierarchy[_]] = {
    TokenHierarchy.get(doc) match {
      case null => None
      case x => Some(x)
    }
  }

  /** Find the token sequence (in case it's embedded in something else at the top level */
  final def getTokenSequence(doc: BaseDocument, offset: Int): Option[TokenSequence[TokenId]] = {
    val th = TokenHierarchy.get(doc)
    getTokenSequence(th, offset)
  }

  final def getTokenSequence(th: TokenHierarchy[_], offset: Int): Option[TokenSequence[TokenId]] = {
    var ts = th.tokenSequence(LANGUAGE)
    if (ts == null) {
      // Possibly an embedding scenario such as an RHTML file
      // First try with backward bias true
      var list = th.embeddedTokenSequences(offset, true)
      var itr = list.iterator
      var break = false
      while (itr.hasNext && !break) {
        val t = itr.next
        if (t.language == LANGUAGE) {
          ts = t.asInstanceOf[TokenSequence[TokenId]]

          break = true
        }
      }

      if (ts == null) {
        list = th.embeddedTokenSequences(offset, false)
        itr = list.iterator
        break = false
        while (itr.hasNext && !break) {
          val t = itr.next
          if (t.language == LANGUAGE) {
            ts = t.asInstanceOf[TokenSequence[TokenId]]
            break = true
          }
        }
      }
    }

    if (ts != null) Some(ts) else None
  }

  def getPositionedSequence(doc: BaseDocument, offset: Int): Option[TokenSequence[TokenId]] = {
    getPositionedSequence(doc, offset, true)
  }

  def getPositionedSequence(doc: BaseDocument, offset: Int, lookBack: Boolean): Option[TokenSequence[TokenId]] = {
    getTokenSequence(doc, offset) match {
      case Some(ts) =>
        try {
          ts.move(offset)
        } catch {
          case ex: AssertionError => doc.getProperty(Document.StreamDescriptionProperty) match {
              case dobj: DataObject => Exceptions.attachMessage(ex, FileUtil.getFileDisplayName(dobj.getPrimaryFile))
              case _ =>
            }
            throw ex
        }

        if (!lookBack && !ts.moveNext || lookBack && !ts.moveNext && !ts.movePrevious) {
          None
        } else Some(ts)
      case None => None
    }
  }

  def getToken(doc: BaseDocument, offset: Int): Option[Token[TokenId]] = {
    getPositionedSequence(doc, offset) match {
      case Some(x) => x.token match {
          case null => None
          case token => Some(token)
        }
      case None => None
    }
  }

  def getTokenId(doc: BaseDocument, offset: Int): Option[TokenId] = {
    getToken(doc, offset).map{_.id}
  }

  def getTokenChar(doc: BaseDocument, offset: Int): Char = {
    getToken(doc, offset) match {
      case Some(x) =>
        val text = x.text.toString
        if (text.length > 0) { // Usually true, but I could have gotten EOF right?
          text.charAt(0)
        } else 0
      case None => 0
    }
  }

  def moveTo(ts: TokenSequence[TokenId], th: TokenHierarchy[_], token: Token[TokenId]) {
    val offset = token.offset(th)
    ts.move(offset)
    ts.moveNext
  }

  final def findNextNoWsNoComment(ts: TokenSequence[TokenId]): Option[Token[TokenId]] = {
    findNextNotIn(ts, WS_COMMENTS)
  }

  final def findPreviousNoWsNoComment(ts: TokenSequence[TokenId]): Option[Token[TokenId]] = {
    findPreviousNotIn(ts, WS_COMMENTS)
  }

  final def findNextNoWs(ts: TokenSequence[TokenId]): Option[Token[TokenId]] = {
    findNextNotIn(ts, WS)
  }

  final def findPreviousNoWs(ts: TokenSequence[TokenId]): Option[Token[TokenId]] = {
    findPreviousNotIn(ts, WS)
  }

  final def findNextNotIn(ts: TokenSequence[TokenId], excludes: Set[TokenId]): Option[Token[TokenId]] = {
    if (excludes.contains(ts.token.id)) {
      while (ts.moveNext && excludes.contains(ts.token.id)) {}
    }

    val token = ts.token
    if (token == null) None else Some(token)
  }

  final def findPreviousNotIn(ts:TokenSequence[TokenId], excludes:Set[TokenId]): Option[Token[TokenId]] = {
    if (excludes.contains(ts.token.id)) {
      while (ts.movePrevious && excludes.contains(ts.token.id)) {}
    }

    val token = ts.token
    if (token == null) None else Some(token)
  }

  final def findNext(ts: TokenSequence[TokenId], id: TokenId): Option[Token[TokenId]] = {
    if (ts.token.id != id) {
      while (ts.moveNext && ts.token.id != id) {}
    }

    val token = ts.token
    if (token == null) None else Some(token)
  }

  final def findNextIn(ts: TokenSequence[TokenId], includes: Set[TokenId]): Option[Token[TokenId]] = {
    if (!includes.contains(ts.token.id)) {
      while (ts.moveNext && !includes.contains(ts.token.id)) {}
    }

    val token = ts.token
    if (token == null) None else Some(token)
  }

  final def findPrevious(ts: TokenSequence[TokenId], id: TokenId): Option[Token[TokenId]] = {
    if (ts.token.id != id) {
      while (ts.movePrevious && ts.token.id != id) {}
    }

    val token = ts.token
    if (token == null) None else Some(token)
  }

  def findNextIncluding(ts: TokenSequence[TokenId], includes: Set[TokenId]): Option[Token[TokenId]] = {
    while (ts.moveNext && !includes.contains(ts.token.id)) {}

    val token = ts.token
    if (token == null) None else Some(token)
  }

  final def findPreviousIn(ts: TokenSequence[TokenId], includes: Set[TokenId]): Option[Token[TokenId]] = {
    if (!includes.contains(ts.token.id)) {
      while (ts.movePrevious && !includes.contains(ts.token.id)) {}
    }

    val token = ts.token
    if (token == null) None else Some(token)
  }

  def skipParenthesis(ts: TokenSequence[TokenId]): Boolean = {
    skipParenthesis(ts, false)
  }

  /**
   * Tries to skip parenthesis
   */
  def skipParenthesis(ts: TokenSequence[TokenId], back: Boolean, left: TokenId = LPAREN, right: TokenId = RPAREN): Boolean = {
    var balance = 0

    var token = ts.token
    if (token == null) {
      return false
    }

    var id = token.id

    // skip whitespace and comment
    if (isWsComment(id)) {
      while ((if (back) ts.movePrevious else ts.moveNext) && isWsComment(id)) {}
    }

    // if current token is not parenthesis
    if (ts.token.id != (if (back) RPAREN else LPAREN)) {
      return false
    }

    do {
      token = ts.token
      id = token.id

      if (id == (if (back) RPAREN else LPAREN)) {
        balance += 1
      } else if (id == (if (back) LPAREN else RPAREN)) {
        if (balance == 0) {
          return false
        } else if (balance == 1) {
          if (back) {
            ts.movePrevious
          } else {
            ts.moveNext
          }
          return true
        }

        balance -= 1
      }
    } while (if (back) ts.movePrevious else ts.moveNext)

    false
  }

  /**
   * Tries to skip pair, ts will be put at the found `left` token
   */
  def skipPair(ts: TokenSequence[TokenId], back: Boolean, left: TokenId, right: TokenId): Boolean = {
    var balance = 0

    var token = ts.token
    if (token == null) {
      return false
    }

    // * skip whitespace and comment
    var id = token.id
    if (isWsComment(id)) {
      while ((if (back) ts.movePrevious else ts.moveNext) && isWsComment(id)) {}
    }

    // * if current token is not of pair
    if (ts.token.id != (if (back) right else left)) {
      return false
    }

    do {
      token = ts.token
      id = token.id

      if (id == (if (back) right else left)) {
        balance += 1
      } else if (id == (if (back) left else right)) {
        if (balance == 0) {
          return false
        } else if (balance == 1) {
          if (back) {
            ts.movePrevious
          } else {
            ts.moveNext
          }
          return true
        }

        balance -= 1
      }
    } while (if (back) ts.movePrevious else ts.moveNext)

    false
  }

  /** Search forwards in the token sequence until a token of type <code>down</code> is found */
  def findPairFwd(ts: TokenSequence[TokenId], up: TokenId, down: TokenId): Option[Token[_]] = {
    var balance = 0
    while (ts.moveNext) {
      val token = ts.token
      val id = token.id

      if (id == up) {
        balance += 1
      } else if (id == down) {
        if (balance == 0) {
          return Some(token)
        }

        balance -= 1
      }
    }

    None
  }

  /** Search backwards in the token sequence until a token of type <code>up</code> is found */
  def findPairBwd(ts: TokenSequence[TokenId], up: TokenId, down: TokenId): Option[Token[_]] = {
    var balance = 0
    while (ts.movePrevious) {
      val token = ts.token
      val id = token.id

      if (id == up) {
        if (balance == 0) {
          return Some(token)
        }

        balance += 1
      } else if (id == down) {
        balance -= 1
      }
    }

    None
  }


  /** Search forwards in the token sequence until a token of type <code>down</code> is found */
  def findFwd(ts: TokenSequence[TokenId], up: TokenId, down: TokenId): OffsetRange = {
    var balance = 0
    while (ts.moveNext) {
      val token = ts.token
      val id = token.id

      if (id == up) {
        balance += 1
      } else if (id == down) {
        if (balance == 0) {
          return new OffsetRange(ts.offset, ts.offset + token.length)
        }

        balance -= 1
      }
    }

    OffsetRange.NONE
  }

  /** Search backwards in the token sequence until a token of type <code>up</code> is found */
  def findBwd(ts: TokenSequence[TokenId], up: TokenId, down: TokenId): OffsetRange = {
    var balance = 0
    while (ts.movePrevious) {
      val token = ts.token
      val id = token.id

      if (id == up) {
        if (balance == 0) {
          return new OffsetRange(ts.offset, ts.offset + token.length)
        }

        balance += 1
      } else if (id == down) {
        balance -= 1
      }
    }

    OffsetRange.NONE
  }

  /** Search forwards in the token sequence until a token of type <code>down</code> is found */
  def findFwd(ts: TokenSequence[TokenId], up: String, down: String): OffsetRange = {
    var balance = 0
    while (ts.moveNext) {
      val token = ts.token
      val id = token.id
      val text = token.text.toString

      if (text.equals(up)) {
        balance += 1
      } else if (text.equals(down)) {
        if (balance == 0) {
          return new OffsetRange(ts.offset, ts.offset + token.length)
        }

        balance -= 1
      }
    }

    OffsetRange.NONE
  }

  /** Search backwards in the token sequence until a token of type <code>up</code> is found */
  def findBwd(ts: TokenSequence[TokenId], up: String, down: String): OffsetRange = {
    var balance = 0
    while (ts.movePrevious) {
      val token = ts.token
      val id = token.id
      val text = token.text.toString

      if (text.equals(up)) {
        if (balance == 0) {
          return new OffsetRange(ts.offset, ts.offset + token.length)
        }

        balance += 1
      } else if (text.equals(down)) {
        balance -= 1
      }
    }

    OffsetRange.NONE
  }

  /** Find the token that begins a block terminated by "end". This is a token
   * in the END_PAIRS array. Walk backwards and find the corresponding token.
   * It does not use indentation for clues since this could be wrong and be
   * precisely the reason why the user is using pair matching to see what's wrong.
   */
  def findBegin(ts: TokenSequence[TokenId]): OffsetRange = {
    var balance = 0
    while (ts.movePrevious) {
      val token = ts.token
      val text = token.text.toString

      if (isBegin(token.id)) {
        // No matching dot for "do" used in conditionals etc.)) {
        if (balance == 0) {
          return new OffsetRange(ts.offset, ts.offset + token.length)
        }

        balance -= 1
      } else if (isEnd(token.id)) {
        balance += 1
      }
    }

    OffsetRange.NONE
  }

  def findEnd(ts: TokenSequence[TokenId]): OffsetRange = {
    var balance = 0
    while (ts.moveNext) {
      val token = ts.token
      val text = token.text.toString

      if (isBegin(token.id)) {
        balance -= 1
      } else if (isEnd(token.id)) {
        if (balance == 0) {
          return new OffsetRange(ts.offset, ts.offset + token.length)
        }

        balance += 1
      }
    }

    OffsetRange.NONE
  }

  /** Determine whether "do" is an indent-token (e.g. matches an end) or if
   * it's simply a separator in while,until,for expressions)
   */
  def isEndmatchingDo(doc: BaseDocument, offset: Int): Boolean = {
    // In the following case, do is dominant:
    //     expression.do
    //        whatever
    //     end
    //
    // However, not here:
    //     while true do
    //        whatever
    //     end
    //
    // In the second case, the end matches the while, but in the first case
    // the end matches the do

    // Look at the first token of the current line
    try {
      val first = Utilities.getRowFirstNonWhite(doc, offset)
      if (first != -1) {
        getToken(doc, first) match {
          case Some(x) =>
            val text = x.text.toString
            if (text.equals("while") || text.equals("for")) {
              return false
            }
          case None => return true
        }
      }
    } catch {case ble: BadLocationException => Exceptions.printStackTrace(ble)}

    true
  }

  /** Compute the balance of begin/end tokens on the line.
   * @param doc the document
   * @param offset The offset somewhere on the line
   * @param upToOffset If true, only compute the line balance up to the given offset (inclusive),
   *   and if false compute the balance for the whole line
   */
  def getBeginEndLineBalance(doc: BaseDocument, offset: Int, upToOffset: Boolean): Int = {
    try {
      val begin = Utilities.getRowStart(doc, offset);
      val end = if (upToOffset) offset else Utilities.getRowEnd(doc, offset)

      val ts = getTokenSequence(doc, begin).getOrElse(return 0)
      ts.move(begin)
      if (!ts.moveNext) {
        return 0
      }

      var balance = 0
      do {
        val token = ts.token
        val text = token.text.toString

        if (isBegin(token.id)) {
          balance += 1
        } else if (isEnd(token.id)) {
          balance -= 1
        }
      } while (ts.moveNext && ts.offset <= end)

      balance
    } catch {
      case ble: BadLocationException => Exceptions.printStackTrace(ble); 0
    }
  }

  /** Compute the balance of up/down tokens on the line */
  def getLineBalance(doc: BaseDocument, offset: Int, up: TokenId, down: TokenId): Stack[Token[TokenId]] = {
    val balanceStack = new Stack[Token[TokenId]]
    try {
      val begin = Utilities.getRowStart(doc, offset)
      val end = Utilities.getRowEnd(doc, offset)

      val ts = getTokenSequence(doc, begin).getOrElse(return balanceStack)
      ts.move(begin)
      if (!ts.moveNext) {
        return balanceStack
      }

      var balance = 0
      do {
        val token = ts.offsetToken
        val id = token.id

        if (id == up) {
          balanceStack.push(token)
          balance += 1
        } else if (id == down) {
          if (!balanceStack.isEmpty) {
            balanceStack.pop
          }
          balance -= 1
        }
      } while (ts.moveNext && ts.offset <= end)

      balanceStack
    } catch {
      case ble: BadLocationException => Exceptions.printStackTrace(ble); balanceStack
    }
  }

  /**
   * The same as braceBalance but generalized to any pair of matching
   * tokens.
   * @param open the token that increses the count
   * @param close the token that decreses the count
   */
  @throws(classOf[BadLocationException])
  def getTokenBalance(doc: BaseDocument, open: TokenId, close: TokenId, offset: Int): Int = {
    val ts = getTokenSequence(doc, 0).getOrElse(return 0)
    // XXX Why 0? Why not offset?
    ts.moveIndex(0)
    if (!ts.moveNext) {
      return 0
    }

    var balance = 0
    do {
      val t = ts.token

      if (t.id == open) {
        balance += 1
      } else if (t.id == close) {
        balance -= 1
      }
    } while (ts.moveNext)

    balance
  }

  /**
   * The same as braceBalance but generalized to any pair of matching
   * tokens.
   * @param open the token that increses the count
   * @param close the token that decreses the count
   */
  @throws(classOf[BadLocationException])
  def getTokenBalance(doc: BaseDocument, open: String, close: String, offset: Int): Int = {
    val ts = getTokenSequence(doc, 0).getOrElse(return 0)
    // XXX Why 0? Why not offset?
    ts.moveIndex(0)
    if (!ts.moveNext) {
      return 0
    }

    var balance = 0
    do {
      val token = ts.token
      val text = token.text.toString

      if (text.equals(open)) {
        balance += 1
      } else if (text.equals(text)) {
        balance -= 1
      }
    } while (ts.moveNext)

    balance
  }

  /**
   * Return true iff the line for the given offset is a JavaScript comment line.
   * This will return false for lines that contain comments (even when the
   * offset is within the comment portion) but also contain code.
   */
  @throws(classOf[BadLocationException])
  def isCommentOnlyLine(doc: BaseDocument, offset: Int): Boolean = {
    val begin = Utilities.getRowFirstNonWhite(doc, offset)

    if (begin == -1) {
      return false // whitespace only
    }

    getTokenId(doc, begin) match {
      case Some(x) if isLineComment(x) => true
      case _ => false
    }
  }

  /**
   * Return the string at the given position, or null if none
   */
  /*_
   def getStringAt(caretOffset:Int, th:TokenHierarchy[Document]): String = {
   val ts = getTokenSequence(th, caretOffset)

   if (ts == null) {
   return null
   }

   ts.move(caretOffset)

   if (!ts.moveNext() && !ts.movePrevious) {
   return null
   }

   if (ts.offset == caretOffset) {
   // We're looking at the offset to the RIGHT of the caret
   // and here I care about what's on the left
   ts.movePrevious
   }

   var token = ts.token
   if (token != null) {
   var id = token.id

   //            // We're within a String that has embedded Js. Drop into the
   //            // embedded language and see if we're within a literal string there.
   //            if (id == ScalaTokenId.EMBEDDED_RUBY) {
   //                ts = (TokenSequence)ts.embedded();
   //                assert ts != null;
   //                ts.move(caretOffset);
   //
   //                if (!ts.moveNext() && !ts.movePrevious()) {
   //                    return null;
   //                }
   //
   //                token = ts.token();
   //                id = token.id();
   //            }
   //

   var string:String = null
   // Skip over embedded Js segments and literal strings until you find the beginning
   var segments = 0
   while (id == ScalaTokenId.Error || id == ScalaTokenId.StringLiteral) {
   string = token.text.toString
   segments += 1
   ts.movePrevious
   token = ts.token
   id = token.id
   }

   if (id == ScalaTokenId.STRING_BEGIN) {
   if (segments == 1) {
   return string
   } else {
   // Build up the String from the sequence
   val sb = new StringBuilder

   while (ts.moveNext) {
   token = ts.token
   id = token.id

   if (id == ScalaTokenId.Error || id == ScalaTokenId.StringLiteral) {
   sb.append(token.text)
   } else {
   break
   }
   }

   return sb.toString
   }
   }
   }

   null
   }
   */

  //    /**
  //     * Check if the caret is inside a literal string that is associated with
  //     * a require statement.
  //     *
  //     * @return The offset of the beginning of the require string, or -1
  //     *     if the offset is not inside a require string.
  //     */
  //    def int getRequireStringOffset(caretOffset:Int, th:TokenHierarchy[Document]) {
  //        TokenSequence<?extends ScalaTokenId> ts = getTokenSequence(th, caretOffset);
  //
  //        if (ts == null) {
  //            return -1;
  //        }
  //
  //        ts.move(caretOffset);
  //
  //        if (!ts.moveNext() && !ts.movePrevious()) {
  //            return -1;
  //        }
  //
  //        if (ts.offset() == caretOffset) {
  //            // We're looking at the offset to the RIGHT of the caret
  //            // and here I care about what's on the left
  //            ts.movePrevious();
  //        }
  //
  //        Token<?extends ScalaTokenId> token = ts.token();
  //
  //        if (token != null) {
  //            id:TokenId = token.id();
  //
  //            // Skip over embedded Js segments and literal strings until you find the beginning
  //            while ((id == ScalaTokenId.ERROR) || (id == ScalaTokenId.STRING_LITERAL)) {
  //                ts.movePrevious();
  //                token = ts.token();
  //                id = token.id();
  //            }
  //
  //            int stringStart = ts.offset() + token.length();
  //
  //            if (id == ScalaTokenId.STRING_BEGIN) {
  //                // Completion of literal strings within require calls
  //                while (ts.movePrevious()) {
  //                    token = ts.token();
  //
  //                    id = token.id();
  //
  //                    if ((id == ScalaTokenId.WHITESPACE) || (id == ScalaTokenId.LPAREN) ||
  //                            (id == ScalaTokenId.STRING_LITERAL)) {
  //                        continue;
  //                    }
  //
  //                    if (id == ScalaTokenId.IDENTIFIER) {
  //                        String text = token.text().toString();
  //
  //                        if (text.equals("require") || text.equals("load")) {
  //                            return stringStart;
  //                        } else {
  //                            return -1;
  //                        }
  //                    } else {
  //                        return -1;
  //                    }
  //                }
  //            }
  //        }
  //
  //        return -1;
  //    }
  //
  /*_
   def getSingleQuotedStringOffset(caretOffset:Int, th:TokenHierarchy[Document]): Int = {
   getLiteralStringOffset(caretOffset, th, ScalaTokenId.STRING_BEGIN)
   }

   def getRegexpOffset(caretOffset:Int, th:TokenHierarchy[Document]): Int = {
   getLiteralStringOffset(caretOffset, th, ScalaTokenId.REGEXP_BEGIN)
   }
   */
  /**
   * Determine if the caret is inside a literal string, and if so, return its starting
   * offset. Return -1 otherwise.
   */
  /*
   private def getLiteralStringOffset(caretOffset:Int, th:TokenHierarchy[Document], begin:ScalaTokenId): Int = {
   val ts = getTokenSequence(th, caretOffset)

   if (ts == null) {
   return -1
   }

   ts.move(caretOffset)

   if (!ts.moveNext && !ts.movePrevious) {
   return -1
   }

   if (ts.offset == caretOffset) {
   // We're looking at the offset to the RIGHT of the caret
   // and here I care about what's on the left
   ts.movePrevious
   }

   var token = ts.token

   if (token != null) {
   var id = token.id

   //            // We're within a String that has embedded Js. Drop into the
   //            // embedded language and see if we're within a literal string there.
   //            if (id == ScalaTokenId.EMBEDDED_RUBY) {
   //                ts = (TokenSequence)ts.embedded();
   //                assert ts != null;
   //                ts.move(caretOffset);
   //
   //                if (!ts.moveNext() && !ts.movePrevious()) {
   //                    return -1;
   //                }
   //
   //                token = ts.token();
   //                id = token.id();
   //            }

   // Skip over embedded Js segments and literal strings until you find the beginning
   while ((id == ScalaTokenId.Error) || (id == ScalaTokenId.StringLiteral) ||
   (id == ScalaTokenId.REGEXP_LITERAL)) {
   ts.movePrevious
   token = ts.token
   id = token.id
   }

   if (id == begin) {
   if (!ts.moveNext) {
   return -1
   }

   return ts.offset
   }
   }

   -1
   }
   */

  /*_
   def isInsideRegexp(doc:BaseDocument, offset:Int): Boolean = {
   val ts = getTokenSequence(doc, offset)

   if (ts == null) {
   return false
   }

   ts.move(offset)

   if (ts.moveNext) {
   val token = ts.token
   val id = token.id
   if (id == ScalaTokenId.REGEXP_LITERAL || id == ScalaTokenId.REGEXP_END) {
   return true
   }
   }
   if (ts.movePrevious()) {
   val token = ts.token
   val id = token.id
   if (id == ScalaTokenId.REGEXP_LITERAL || id == ScalaTokenId.REGEXP_BEGIN) {
   return true
   }
   }

   false
   }
   */

  def getDocumentationRange(th: TokenHierarchy[_], nodeOffset: Int): OffsetRange = {
    val astOffset = nodeOffset
    // XXX This is wrong; I should do a
    //int lexOffset = LexUtilities.getLexerOffset(result, astOffset);
    // but I don't have the CompilationInfo in the ParseResult handed to the indexer!!
    val lexOffset = astOffset
    getDocCommentRangeBefore(th, lexOffset)
  }

  /**
   * Get the comment block for the given offset. The offset may be either within the comment
   * block, or the comment corresponding to a code node, depending on isAfter.
   *
   * @param doc The document
   * @param caretOffset The offset in the document
   * @param isAfter If true, the offset is pointing to some code AFTER the code block
   *   such as a method node. In this case it needs to back up to find the comment.
   * @return
   */
  def getCommentBlock(doc: BaseDocument, caretOffset: Int, isAfter: Boolean): OffsetRange = {
    // Check if the caret is within a comment, and if so insert a new
    // leaf "node" which contains the comment line and then comment block
    try {
      val ts = getTokenSequence(doc, caretOffset).getOrElse(return OffsetRange.NONE)

      ts.move(caretOffset)
      if (isAfter) {
        while (ts.movePrevious) {
          val id = ts.token.id
          if (isComment(id)) {
            return getCommentBlock(doc, ts.offset, false)
          } else if (!isWs(id)) {
            return OffsetRange.NONE
          }
        }
        return OffsetRange.NONE
      }

      if (!ts.moveNext && !ts.movePrevious) {
        return OffsetRange.NONE
      }
      val token = ts.token

      if (token != null && isBlockComment(token.id)) {
        return new OffsetRange(ts.offset, ts.offset + token.length)
      }

      if (token != null && isLineComment(token.id)) {
        // First add a range for the current line
        var begin = Utilities.getRowStart(doc, caretOffset)
        var end = Utilities.getRowEnd(doc, caretOffset)

        if (isCommentOnlyLine(doc, caretOffset)) {
          var break = false
          while (begin > 0 && !break) {
            val newBegin = Utilities.getRowStart(doc, begin - 1)
            if (newBegin < 0 || !isCommentOnlyLine(doc, newBegin)) {
              begin = Utilities.getRowFirstNonWhite(doc, begin)
              break = true
            } else {
              begin = newBegin
            }
          }

          val length = doc.getLength

          break = false
          while (!break) {
            val newEnd = Utilities.getRowEnd(doc, end + 1)
            if (newEnd >= length || !isCommentOnlyLine(doc, newEnd)) {
              end = Utilities.getRowLastNonWhite(doc, end) + 1
              break = true
            } else {
              end = newEnd
            }
          }

          if (begin < end) {
            return new OffsetRange(begin, end)
          }
        } else {
          // It's just a line comment next to some code
          val th = TokenHierarchy.get(doc)
          val offset = token.offset(th)
          return new OffsetRange(offset, offset + token.length)
        }
      }
    } catch {
      case ble: BadLocationException => Exceptions.printStackTrace(ble)
    }

    OffsetRange.NONE
  }
  //    def boolean isInsideQuotedString(doc:BaseDocument, offset:Int) {
  //        TokenSequence<?extends ScalaTokenId> ts = FortressLexUtilities.getTokenSequence(doc, offset);
  //
  //        if (ts == null) {
  //            return false;
  //        }
  //
  //        ts.move(offset);
  //
  //        if (ts.moveNext()) {
  //            Token<?extends ScalaTokenId> token = ts.token();
  //            id:TokenId = token.id();
  //            if (id == ScalaTokenId.QUOTED_STRING_LITERAL || id == ScalaTokenId.QUOTED_STRING_END) {
  //                return true;
  //            }
  //        }
  //        if (ts.movePrevious()) {
  //            Token<?extends ScalaTokenId> token = ts.token();
  //            id:TokenId = token.id();
  //            if (id == ScalaTokenId.QUOTED_STRING_LITERAL || id == ScalaTokenId.QUOTED_STRING_BEGIN) {
  //                return true;
  //            }
  //        }
  //
  //        return false;
  //    }
  //


  /**
   * Back up to the first space character prior to the given offset - as long as
   * it's on the same line!  If there's only leading whitespace on the line up
   * to the lex offset, return the offset itself
   * @todo Rewrite this now that I have a separate newline token, EOL, that I can
   *   break on - no need to call Utilities.getRowStart.
   */
  def findSpaceBegin(doc: BaseDocument, lexOffset: Int): Int = {
    val ts = getTokenSequence(doc, lexOffset).getOrElse(return lexOffset)
    var allowPrevLine = false
    var lineStart: Int = 0
    try {
      lineStart = Utilities.getRowStart(doc, math.min(lexOffset, doc.getLength))
      var prevLast = lineStart - 1
      if (lineStart > 0) {
        prevLast = Utilities.getRowLastNonWhite(doc, lineStart - 1);
        if (prevLast != -1) {
          val c = doc.getText(prevLast, 1).charAt(0)
          if (c == ',') {
            // Arglist continuation? // TODO:  check lexing
            allowPrevLine = true
          }
        }
      }
      if (!allowPrevLine) {
        val firstNonWhite = Utilities.getRowFirstNonWhite(doc, lineStart)
        if (lexOffset <= firstNonWhite || firstNonWhite == -1) {
          return lexOffset
        }
      } else {
        // Make lineStart so small that math.max won't cause any problems
        val firstNonWhite = Utilities.getRowFirstNonWhite(doc, lineStart)
        if (prevLast >= 0 && (lexOffset <= firstNonWhite || firstNonWhite == -1)) {
          return prevLast + 1
        }
        lineStart = 0
      }
    } catch {
      case ble:BadLocationException =>
        Exceptions.printStackTrace(ble)
        return lexOffset
    }
    ts.move(lexOffset)
    if (ts.moveNext) {
      if (lexOffset > ts.offset()) {
        // We're in the middle of a token
        return math.max(if (ts.token.id == WHITE_SPACE) ts.offset else lexOffset, lineStart)
      }
      while (ts.movePrevious) {
        val token = ts.token
        if (token.id != WHITE_SPACE) {
          return math.max(ts.offset + token.length, lineStart)
        }
      }
    }

    lexOffset
  }

  /**
   * Get the documentation associated with the given node in the given document.
   * TODO: handle proper block comments
   */
  def gatherDocumentation(info: Parser.Result, baseDoc: BaseDocument, nodeOffset: Int): List[String] = {
    var comments: List[String] = Nil
    var elementBegin = nodeOffset
    if (info != null && info.getSnapshot.getSource.getDocument(true) == baseDoc) {
      elementBegin = getLexerOffset(info, elementBegin)
      if (elementBegin == -1) {
        return Nil
      }
    }

    try {
      if (elementBegin >= baseDoc.getLength) {
        return Nil
      }

      // Search to previous lines, locate comments. Once we have a non-whitespace line that isn't
      // a comment, we're done

      var offset = Utilities.getRowStart(baseDoc, elementBegin)
      offset -= 1

      // Skip empty and whitespace lines
      var break = false
      while (offset >= 0 && !break) {
        // Find beginning of line
        offset = Utilities.getRowStart(baseDoc, offset)

        if (!Utilities.isRowEmpty(baseDoc, offset) &&
            !Utilities.isRowWhite(baseDoc, offset)) {
          break = true
        } else {
          offset -= 1
        }
      }

      if (offset < 0) {
        return Nil
      }

      break = true
      while (offset >= 0 && !break) {
        // Find beginning of line
        offset = Utilities.getRowStart(baseDoc, offset)

        if (Utilities.isRowEmpty(baseDoc, offset) || Utilities.isRowWhite(baseDoc, offset)) {
          // Empty lines not allowed within an rdoc
          break = true
        } else {
          // This is a comment line we should include
          val lineBegin = Utilities.getRowFirstNonWhite(baseDoc, offset)
          val lineEnd = Utilities.getRowLastNonWhite(baseDoc, offset) + 1
          val line = baseDoc.getText(lineBegin, lineEnd - lineBegin)

          // Tolerate "public", "private" and "protected" here --
          // Test::Unit::Assertions likes to put these in front of each
          // method.
          if (line.startsWith("*")) {
            // ignore end of block comment: "*/"
            if (line.length == 1 || (line.length > 1 && line.charAt(1) != '/')) {
              comments = line.substring(1).trim :: comments
            }
            // Previous line
            offset -= 1
          } else {
            // No longer in a comment
            break = true
          }
        }
      }
    } catch {
      case ble:BadLocationException => Exceptions.printStackTrace(ble)
    }

    comments.toList
  }

  /**
   * Return true iff the given token is a token that should be matched
   * with a corresponding "end" token, such as "begin", "def", "module",
   * etc.
   */
  def isBegin(id: TokenId): Boolean = {
    END_PAIRS.contains(id)
  }

  /**
   * Return true iff the given token is a token that should be matched
   * with a corresponding "end" token, such as "begin", "def", "module",
   * etc.
   */
  def isEnd(id: TokenId): Boolean = {
    END_PAIRS.contains(id)
  }

  final def isWs(id: TokenId): Boolean = {
    WS.contains(id)
  }

  final def isWsComment(id: TokenId): Boolean = {
    WS_COMMENTS.contains(id)
  }

  final def isComment(id: TokenId): Boolean = {
    isLineComment(id) || isBlockComment(id) || isDocComment(id)
  }

  final def isLineComment(id: TokenId): Boolean = {
    LINE_COMMENTS.contains(id)
  }

  final def isDocComment(id: TokenId): Boolean = {
    DOC_COMMENTS.contains(id)

  }

  final def isBlockComment(id: TokenId): Boolean = {
    BLOCK_COMMENTS.contains(id)
  }

  /**
   * Return true iff the given token is a token that indents its content,
   * such as the various begin tokens as well as "else", "when", etc.
   */
  def isIndent(id: TokenId): Boolean = {
    INDENT_WORDS.contains(id)
  }


  def isKeyword(id: TokenId): Boolean = {
    id.primaryCategory.equals("keyword")
  }

  final def getRangeOfToken(th: TokenHierarchy[_], token: Token[_ <: TokenId]): OffsetRange = {
    val offset = token.offset(th)
    new OffsetRange(offset, offset + token.length)
  }

  def getDocument(fo: FileObject, openIfNecessary: Boolean): Option[BaseDocument] = {
    try {
      val dobj = DataObject.find(fo)
      val ec = dobj.getCookie(classOf[EditorCookie])
      if (ec != null) {
        return (if (openIfNecessary) Some(ec.openDocument) else Some(ec.getDocument)).asInstanceOf[Option[BaseDocument]]
      }
    } catch {
      case ex:DataObjectNotFoundException => Exceptions.printStackTrace(ex)
      case ex:IOException => Exceptions.printStackTrace(ex)
    }

    None
  }

}
