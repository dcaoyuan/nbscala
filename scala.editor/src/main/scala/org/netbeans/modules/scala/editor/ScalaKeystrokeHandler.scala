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

import javax.swing.text.{BadLocationException, Caret, Document, JTextComponent}
import org.netbeans.api.lexer.{TokenHierarchy, TokenId}
import org.netbeans.editor.{BaseDocument, Utilities}
import org.netbeans.modules.csl.api.{EditorOptions, KeystrokeHandler, OffsetRange}
import org.netbeans.modules.csl.spi.{GsfUtilities, ParserResult}
import org.netbeans.modules.editor.indent.api.IndentUtils
import org.openide.util.Exceptions

import org.netbeans.modules.scala.core.ScalaMimeResolver
import org.netbeans.modules.scala.core.ScalaParserResult
import org.netbeans.modules.scala.core.lexer.{ScalaLexUtil, ScalaTokenId}

/**
 * (Based on BracketCompletion class in NetBeans' java editor support)
 *
 * A hook method called after a character was inserted into the
 * document. The function checks for special characters for
 * completion ()[]'"{} and other conditions and optionally performs
 * changes to the doc and or caret (complets braces, moves caret,
 * etc.)
 *
 * Return true if the character was already inserted (and the IDE
 * should not further insert anything)
 *
 */
object ScalaKeystrokeHandler {
  /**
   * When true, continue comments if you press return in a line comment (that does not
   * also have code on the same line
   */
  val CONTINUE_COMMENTS = true //Properties.getBoolean("scala.cont.comment")
  // XXX What about ScalaTokenId.STRING_BEGIN or QUOTED_STRING_BEGIN?
  private val STRING_TOKENS: Set[TokenId] = Set(ScalaTokenId.StringLiteral,
                                                ScalaTokenId.STRING_END)
  /** Tokens which indicate that we're within a regexp string */
  // XXX What about ScalaTokenId.REGEXP_BEGIN?
  private val REGEXP_TOKENS: Set[TokenId] = Set(ScalaTokenId.REGEXP_LITERAL,
                                                ScalaTokenId.REGEXP_END)
}

class ScalaKeystrokeHandler extends KeystrokeHandler {
  import ScalaKeystrokeHandler._
  /**
   * When != -1, this indicates that we previously adjusted the indentation of the
   * line to the given offset, and if it turns out that the user changes that token,
   * we revert to the original indentation
   */
  private var previousAdjustmentOffset = -1
  /** True iff we're processing bracket matching AFTER the key has been inserted rather than before  */
  private var isAfter: Boolean = _
  /**
   * The indentation to revert to when previousAdjustmentOffset is set and the token
   * changed
   */
  private var previousAdjustmentIndent: Int = _

  def isInsertMatchingEnabled(doc: BaseDocument): Boolean = {
    // The editor options code is calling methods on BaseOptions instead of looking in the settings map :(
    //Boolean b = ((Boolean)Settings.getValue(doc.getKitClass(), SettingsNames.PAIR_CHARACTERS_COMPLETION));
    //return b == null || b.booleanValue();
    EditorOptions.get(ScalaMimeResolver.MIME_TYPE) match {
      case null => true
      case options => options.getMatchBrackets
    }
  }

  /** replaced by ScalaBracesMatcher#findMatching */
  override def findMatching(document: Document, aoffset: Int /*, boolean simpleSearch*/): OffsetRange = {
    OffsetRange.NONE
  }

  @throws(classOf[BadLocationException])
  override def beforeBreak(document: Document, aoffset: Int, target: JTextComponent): Int = {
    var offset = aoffset
    isAfter = false

    val caret = target.getCaret
    val doc = document.asInstanceOf[BaseDocument]

    val lineBeg = Utilities.getRowStart(doc, offset)
    val lineEnd = Utilities.getRowEnd(doc, offset)

    if (lineBeg == offset && lineEnd == offset) {
      return -1 // pressed return on a blank newline - do nothing
    }

    val ts = ScalaLexUtil.getTokenSequence(doc, offset).getOrElse(return -1)
    ts.move(offset)
    if (!ts.moveNext && !ts.movePrevious) {
      return -1
    }

    var token = ts.token
    var id = token.id
    // * I don't know why sometimes the token is `Nl`, anyway if this happens, just movePrevious one step
    if (id == ScalaTokenId.Nl && ts.movePrevious) {
      token = ts.token
      id = token.id
    }


    // * Insert an `end` or `}` ?
    val insertMatching = isInsertMatchingEnabled(doc)

    val insertEndResult = Array(false)
    val insertRBraceResult = Array(false)
    val indentResult = Array(1)
    val insert = insertMatching && isPairMissing(doc, offset, false, insertEndResult, insertRBraceResult, null, indentResult)

    if (insert) {
      val insertEnd = insertEndResult(0)
      val insertRBrace = insertRBraceResult(0)
      val indent = indentResult(0)

      val offsetLastNonWhite = Utilities.getRowLastNonWhite(doc, offset)

      // We've either encountered a further indented line, or a line that doesn't
      // look like the end we're after, so insert a matching end.
      val sb = new StringBuilder
      if (offset > offsetLastNonWhite) {
        sb.append("\n")
        sb.append(IndentUtils.createIndentString(doc, indent))
      } else {
        // I'm inserting a newline in the middle of a sentence, such as the scenario in #118656
        // I should insert the end AFTER the text on the line
        val restOfLine = doc.getText(offset, Utilities.getRowEnd(doc, offsetLastNonWhite) - offset)
        sb.append(restOfLine)
        sb.append("\n")
        sb.append(IndentUtils.createIndentString(doc, indent))
        doc.remove(offset, restOfLine.length)
      }

      if (insertEnd) {
        sb.append("end") // NOI18N
      } else {
        assert(insertRBrace)
        sb.append("}") // NOI18N
      }

      val insertOffset = offset
      doc.insertString(insertOffset, sb.toString, null)
      caret.setDot(insertOffset)

      return -1
    }
    
    id match {
      case ScalaTokenId.Identifier =>
        // * See if it's a block comment opener, since this token is Identifier,
        // * means it's not comment closed yet
        val text = token.text.toString 
        if (text.startsWith("/*") && ts.offset == Utilities.getRowFirstNonWhite(doc, offset)) {
          val indent = GsfUtilities.getLineIndent(doc, offset)
          val sb = new StringBuilder
          sb.append(IndentUtils.createIndentString(doc, indent))
          sb.append(" * ") // NOI18N
          val offsetDelta = sb.length + 1
          sb.append("\n")  // NOI18N
          sb.append(IndentUtils.createIndentString(doc, indent))
          sb.append(" */") // NOI18N
          doc.insertString(offset, sb.toString, null)
          caret.setDot(offset)

          return offset + offsetDelta
        }
      case _ =>
    }

    /*_
     if (id == ScalaTokenId.StringLiteral ||
     id == ScalaTokenId.STRING_END && offset < ts.offset + ts.token.length) {
     // Instead of splitting a string "foobar" into "foo"+"bar", just insert a \ instead!
     //int indent = GsfUtilities.getLineIndent(doc, offset);
     //int delimiterOffset = id == ScalaTokenId.STRING_END ? ts.offset() : ts.offset()-1;
     //char delimiter = doc.getText(delimiterOffset,1).charAt(0);
     //doc.insertString(offset, delimiter + " + " + delimiter, null);
     //caret.setDot(offset+3);
     //return offset + 5 + indent;
     val str = if (id != ScalaTokenId.StringLiteral || offset > ts.offset) "\\n\\" else "\\"
     doc.insertString(offset, str, null)
     caret.setDot(offset + str.length)

     return offset + 1 + str.length
     }

     if (id == ScalaTokenId.REGEXP_LITERAL ||
     id == ScalaTokenId.REGEXP_END && offset < ts.offset + ts.token.length) {
     // Instead of splitting a string "foobar" into "foo"+"bar", just insert a \ instead!
     //int indent = GsfUtilities.getLineIndent(doc, offset);
     //doc.insertString(offset, "/ + /", null);
     //caret.setDot(offset+3);
     //return offset + 5 + indent;
     val str = if (id != ScalaTokenId.REGEXP_LITERAL || offset > ts.offset) "\\n\\" else "\\"
     doc.insertString(offset, str, null)
     caret.setDot(offset + str.length)

     return offset + 1 + str.length
     }
     */

    // Special case: since I do hash completion, if you try to type
    //     y = Thread.start {
    //         code here
    //     }
    // you end up with
    //     y = Thread.start {|}
    // If you hit newline at this point, you end up with
    //     y = Thread.start {
    //     |}
    // which is not as helpful as it would be if we were not doing hash-matching
    // (in that case we'd notice the brace imbalance, and insert the closing
    // brace on the line below the insert position, and indent properly.
    // Catch this scenario and handle it properly.
    id match {
      case ScalaTokenId.RBrace | ScalaTokenId.RBracket if Utilities.getRowLastNonWhite(doc, offset) == offset =>
        def insertIndent = {
          val indent = GsfUtilities.getLineIndent(doc, offset)
          val sb = new StringBuilder
          sb.append("\n") // NOI18N
          sb.append(IndentUtils.createIndentString(doc, indent))
          val insertOffset = offset
          doc.insertString(insertOffset, sb.toString, null)
          caret.setDot(insertOffset)
        }
        ScalaLexUtil.getTokenId(doc, offset - 1) match {
          case Some(prevId) => (id, prevId) match {
              case (ScalaTokenId.RBrace,   ScalaTokenId.LBrace)   => insertIndent
              case (ScalaTokenId.RBracket, ScalaTokenId.LBracket) => insertIndent
              case _ =>
            }
          case None =>
        }
      case _ =>
    }

    id match {
      case ScalaTokenId.Ws =>
        // * Pressing newline in the whitespace before a comment
        // * should be identical to pressing newline with the caret
        // * at the beginning of the comment
        val begin = Utilities.getRowFirstNonWhite(doc, offset)
        if (begin != -1 && offset < begin) {
          ts.move(begin)
          if (ts.moveNext) {
            id = ts.token.id
            if (id == ScalaTokenId.LineComment) {
              offset = begin
            }
          }
        }
      case _ =>
    }

    id match {
      case _ if (ScalaLexUtil.isBlockComment(id) || ScalaLexUtil.isDocComment(id)) && 
        id != ScalaTokenId.BlockCommentEnd && id != ScalaTokenId.DocCommentEnd &&
        offset > ts.offset =>
        // * continue stars
        val begin = Utilities.getRowFirstNonWhite(doc, offset)
        val end = Utilities.getRowEnd(doc, offset) + 1
        var line = doc.getText(begin, end - begin)
        val isBlockStart = line.startsWith("/*")
        if (isBlockStart || line.startsWith("*")) {
          var indent = GsfUtilities.getLineIndent(doc, offset)
          val sb = new StringBuilder
          if (isBlockStart) {
            indent += 1
          }
          sb.append(IndentUtils.createIndentString(doc, indent))
          sb.append("*") // NOI18N
          // * copy existing indentation after `*`
          val afterStar = if (isBlockStart) begin + 2 else begin + 1
          line = doc.getText(afterStar, Utilities.getRowEnd(doc, afterStar) - afterStar)
          var i = 0
          var break = false
          while (i < line.length && !break) {
            line.charAt(i) match {
              case c@(' ' | '\t') => sb.append(c)
              case _ => break = true
            }
            i += 1
          }

          var insertOffset = offset
          if (offset == begin && insertOffset > 0) {
            insertOffset = Utilities.getRowStart(doc, offset)
            val sp = Utilities.getRowStart(doc, offset) + sb.length
            doc.insertString(insertOffset, sb.toString, null)
            caret.setDot(sp)

            return sp
          } else {
            doc.insertString(insertOffset, sb.toString, null)
            caret.setDot(insertOffset)

            return insertOffset + sb.length + 1
          }
        }
      case _ =>
    }

    val isLineComment = id match {
      case ScalaTokenId.LineComment => true
      case ScalaTokenId.Nl if ts.movePrevious && ts.token.id == ScalaTokenId.LineComment => true
      case _ => false
    }

    if (isLineComment) {
      // * Only do this if the line only contains comments OR if there is content to the right on this line,
      // * or if the next line is a comment!

      var continueComment = false
      val begin = Utilities.getRowFirstNonWhite(doc, offset)

      // * We should only continue comments if the previous line had a comment
      // * (and a comment from the beginning, not a trailing comment)
      var prevLineIsComment = false
      var nextLineIsComment = false
      val rowStart = Utilities.getRowStart(doc, offset)
      if (rowStart > 0) {
        Utilities.getRowFirstNonWhite(doc, rowStart - 1) match {
          case -1 =>
          case prevBegin => ScalaLexUtil.getTokenId(doc, prevBegin) match {
              case Some(ScalaTokenId.LineComment) => prevLineIsComment = true
              case _ =>
            }
        }
      }
      val rowEnd = Utilities.getRowEnd(doc, offset)
      if (rowEnd < doc.getLength) {
        Utilities.getRowFirstNonWhite(doc, rowEnd + 1) match {
          case -1 =>
          case nextBegin => ScalaLexUtil.getTokenId(doc, nextBegin) match {
              case Some(ScalaTokenId.LineComment) => nextLineIsComment = true
              case _ =>
            }
        }
      }

      // * See if we have more input on this comment line (to the right
      // * of the inserted newline); if so it's a "split" operation on
      // * the comment
      if (prevLineIsComment || nextLineIsComment ||
          offset > ts.offset && offset < ts.offset + ts.token.length) {
        if (ts.offset + token.length > offset + 1) {
          // See if the remaining text is just whitespace
          val trailing = doc.getText(offset, Utilities.getRowEnd(doc, offset) - offset)
          if (trailing.trim.length != 0) {
            continueComment = true
          }
        } else if (CONTINUE_COMMENTS) {
          // * See if the "continue comments" options is turned on, and this is a line that
          // * contains only a comment (after leading whitespace)
          ScalaLexUtil.getTokenId(doc, begin) match {
            case Some(ScalaTokenId.LineComment) => continueComment = true
            case _ =>
          }
        }
        if (!continueComment) {
          // * See if the next line is a comment; if so we want to continue
          // * comments editing the middle of the comment
          val nextLine = Utilities.getRowEnd(doc, offset) + 1
          if (nextLine < doc.getLength) {
            Utilities.getRowFirstNonWhite(doc, nextLine) match {
              case -1 =>
              case nextLineFirst => ScalaLexUtil.getTokenId(doc, nextLineFirst) match {
                  case Some(ScalaTokenId.LineComment) => continueComment = true
                  case _ =>
                }
            }
          }
        }
      }

      if (continueComment) {
        // * Line comments should continue
        val indent = GsfUtilities.getLineIndent(doc, offset)
        val sb = new StringBuilder
        sb.append(IndentUtils.createIndentString(doc, indent))
        sb.append("//") // NOI18N
        // * Copy existing indentation
        val afterSlash = begin + 2
        val line = doc.getText(afterSlash, Utilities.getRowEnd(doc, afterSlash) - afterSlash)
        var i = 0
        var break = false
        while (i < line.length && !break) {
          line.charAt(i) match {
            case c@(' ' | '\t') => sb.append(c)
            case _ => break = true
          }
          i += 1
        }

        var insertOffset = offset
        if (offset == begin && insertOffset > 0) {
          insertOffset = Utilities.getRowStart(doc, offset)
          val sp = Utilities.getRowStart(doc, offset) + sb.length
          doc.insertString(insertOffset, sb.toString, null)
          caret.setDot(sp)

          return sp
        } else {
          doc.insertString(insertOffset, sb.toString, null)
          caret.setDot(insertOffset)
          
          return insertOffset + sb.length + 1
        }
      }
    }

    -1
  }

  /**
   * Determine if an "end" or "}" is missing following the caret offset.
   * The logic used is to check the text on the current line for block initiators
   * (e.g. "def", "for", "{" etc.) and then see if a corresponding close is
   * found after the same indentation level.
   *
   * @param doc The document to be checked
   * @param offset The offset of the current line
   * @param skipJunk If false, only consider the current line (of the offset)
   *   as the possible "block opener"; if true, look backwards across empty
   *   lines and comment lines as well.
   * @param insertEndResult Null, or a boolean 1-element array whose first
   *   element will be set to true iff this method determines that "end" should
   *   be inserted
   * @param insertRBraceResult Null, or a boolean 1-element array whose first
   *   element will be set to true iff this method determines that "}" should
   *   be inserted
   * @param startOffsetResult Null, or an integer 1-element array whose first
   *   element will be set to the starting offset of the opening block.
   * @param indentResult Null, or an integer 1-element array whose first
   *   element will be set to the indentation level "end" or "}" should be
   *   indented to when inserted.
   * @return true if something is missing; insertEndResult, insertRBraceResult
   *   and identResult will provide the more specific return values in their
   *   first elements.
   */
  @throws(classOf[BadLocationException])
  def isPairMissing(doc: BaseDocument, offset: Int, skipJunk: Boolean,
                    insertEndResult: Array[Boolean], insertRBraceResult: Array[Boolean],
                    startOffsetResult: Array[Int], indentResult: Array[Int]): Boolean =  {

    val th = TokenHierarchy.get(doc)

    val length = doc.getLength

    // Insert an end statement? Insert a } marker?
    // Do so if the current line contains an unmatched begin marker,
    // AND a "corresponding" marker does not exist.
    // This will be determined as follows: Look forward, and check
    // that we don't have "indented" code already (tokens at an
    // indentation level higher than the current line was), OR that
    // there is no actual end or } coming up.
    if (startOffsetResult != null) {
      startOffsetResult(0) = Utilities.getRowFirstNonWhite(doc, offset)
    }

    val beginEndBalance = ScalaLexUtil.getBeginEndLineBalance(doc, offset, true)
    val braceBalance = ScalaLexUtil.getLineBalance(doc, offset, ScalaTokenId.LBrace, ScalaTokenId.RBrace)

    /** Do not try to guess the condition when offset is before the unbalanced brace */
    if ((beginEndBalance == 1 || braceBalance.size == 1) && offset > braceBalance.top.offset(th)) {
      // There is one more opening token on the line than a corresponding
      // closing token.  (If there's is more than one we don't try to help.)
      val indent = GsfUtilities.getLineIndent(doc, offset)

      // Look for the next nonempty line, and if its indent is > indent,
      // or if its line balance is -1 (e.g. it's an end) we're done
      var insertEnd = beginEndBalance > 0
      var insertRBrace = braceBalance.size > 0
      var next = Utilities.getRowEnd(doc, offset) + 1
      while (next < length) {
        if (Utilities.isRowEmpty(doc, next) || Utilities.isRowWhite(doc, next) ||
            ScalaLexUtil.isCommentOnlyLine(doc, next)) {
        } else {
          val nextIndent = GsfUtilities.getLineIndent(doc, next)
          if (nextIndent > indent) {
            insertEnd = false
            insertRBrace = false
          } else if (nextIndent == indent) {
            if (insertEnd) {
              if (ScalaLexUtil.getBeginEndLineBalance(doc, next, false) < 0) {
                insertEnd = false
              } else {
                // See if I have a structure word like "else", "ensure", etc.
                // (These are indent words that are not also begin words)
                // and if so refrain from inserting the end
                val lineBegin = Utilities.getRowFirstNonWhite(doc, next)
                ScalaLexUtil.getTokenId(doc, lineBegin) match {
                  case Some(id) if ScalaLexUtil.isIndent(id) && !ScalaLexUtil.isBegin(id) => insertEnd = false
                  case _ =>
                }
              }
            } else if (insertRBrace && ScalaLexUtil.getLineBalance(doc, next, ScalaTokenId.LBrace, ScalaTokenId.RBrace).size < 0) {
              insertRBrace = false
            }
          }
        }
        next = Utilities.getRowEnd(doc, next) + 1
      }

      if (insertEndResult != null) {
        insertEndResult(0) = insertEnd
      }

      if (insertRBraceResult != null) {
        insertRBraceResult(0) = insertRBrace
      }

      if (indentResult != null) {
        indentResult(0) = indent
      }

      insertEnd || insertRBrace
    } else false
  }

  @throws(classOf[BadLocationException])
  override def beforeCharInserted(document: Document, acaretOffset: Int, target: JTextComponent, c: Char): Boolean = {
    val doc =  document.asInstanceOf[BaseDocument]
    if (!isInsertMatchingEnabled(doc)) {
      return false
    }
    
    var caretOffset = acaretOffset
    isAfter = false
    val caret = target.getCaret

    if (target.getSelectionStart != -1) {
      val isCodeTemplateEditing = GsfUtilities.isCodeTemplateEditing(doc)
      if (isCodeTemplateEditing) {
        val start = target.getSelectionStart
        val end = target.getSelectionEnd
        if (start < end) {
          target.setSelectionStart(start)
          target.setSelectionEnd(start)
          caretOffset = start
          caret.setDot(caretOffset)
          doc.remove(start, end - start)
        }
      } else { // Fall through to do normal insert matching work
        c match {
          case '"' | '\'' | '`' | '(' | '{' | '[' | '<' | '/' | '~' =>
            // * Bracket the selection
            val selection = target.getSelectedText
            if (selection != null && selection.length > 0) {
              val firstChar = selection.charAt(0)
              if (firstChar != c) {
                val start = target.getSelectionStart
                val end = target.getSelectionEnd
                ScalaLexUtil.getPositionedSequence(doc, start) foreach {
                  case ts if ts.token.id != ScalaTokenId.StringLiteral => // * Not inside strings!
                    val lastChar = selection.charAt(selection.length - 1)
                    // * Replace the surround-with chars?
                    (c, firstChar) match {
                      case ('~', '"' | '\'' | '`' | '(' | '{' | '[' | '<' | '/') if selection.length > 1 && lastChar == matching(firstChar) =>
                        // * remove surround pair
                        if (selection.startsWith("/* ") && selection.endsWith(" */")) {
                          doc.remove(end - 3, 3)
                          doc.remove(start, 3)
                          target.getCaret.setDot(end - 6)
                        } else {
                          doc.remove(end - 1, 1)
                          doc.remove(start, 1)
                          target.getCaret.setDot(end - 2)
                        }
                      case ('~', _) =>
                      case (_, '"' | '\'' | '`' | '(' | '{' | '[' | '<' | '/') if selection.length > 1 && lastChar == matching(firstChar) =>
                        doc.remove(end - 1, 1)
                        doc.insertString(end - 1, "" + matching(c), null)
                        doc.remove(start, 1)
                        doc.insertString(start, "" + c, null)
                        target.getCaret.setDot(end)
                      case ('/', _) =>
                        // * No. insert around with /* */
                        doc.remove(start, end - start)
                        doc.insertString(start, "/* " + selection + " */", null)
                        target.getCaret.setDot(start + selection.length + 6)
                      case (_, _) =>
                        // * No, insert around
                        doc.remove(start, end - start)
                        doc.insertString(start, c + selection + matching(c), null)
                        target.getCaret.setDot(start + selection.length + 2)
                    }
                    return true
                  case _ =>
                }
              }
            }
          case _ =>
        }
      }
    }

    val ts = ScalaLexUtil.getTokenSequence(doc, caretOffset).getOrElse(return false)
    ts.move(caretOffset)
    if (!ts.moveNext && !ts.movePrevious) {
      return false
    }

    val token = ts.token
    val id = token.id

    // "/" is handled AFTER the character has been inserted since we need the lexer's help
    var stringTokens = Set[TokenId]()
    var beginTokenId: TokenId = null
    c match {
      case '"' | '\'' =>
        stringTokens = STRING_TOKENS
        beginTokenId = ScalaTokenId.STRING_BEGIN
      case _ =>
        id match {
          case ScalaTokenId.Error if ts.movePrevious =>
            ts.token.id match {
              case ScalaTokenId.STRING_BEGIN =>
                stringTokens = STRING_TOKENS
                beginTokenId = ScalaTokenId.STRING_BEGIN
              case ScalaTokenId.REGEXP_BEGIN =>
                stringTokens = REGEXP_TOKENS
                beginTokenId = ScalaTokenId.REGEXP_BEGIN
              case _ =>
            }
          case ScalaTokenId.STRING_BEGIN if caretOffset == ts.offset + 1 && !Character.isLetter(c) => // %q, %x, etc. Only %[], %!!, %<space> etc. is allowed
            stringTokens = STRING_TOKENS
            beginTokenId = ScalaTokenId.STRING_BEGIN
          case ScalaTokenId.STRING_BEGIN if caretOffset == ts.offset + 2 =>
            stringTokens = STRING_TOKENS
            beginTokenId = ScalaTokenId.STRING_BEGIN
          case ScalaTokenId.STRING_END =>
            stringTokens = STRING_TOKENS
            beginTokenId = ScalaTokenId.STRING_BEGIN
          case ScalaTokenId.REGEXP_BEGIN if caretOffset == ts.offset + 2 =>
            stringTokens = REGEXP_TOKENS
            beginTokenId = ScalaTokenId.REGEXP_BEGIN
          case ScalaTokenId.REGEXP_END =>
            stringTokens = REGEXP_TOKENS
            beginTokenId = ScalaTokenId.REGEXP_BEGIN
          case _ =>
        }
    }

    if (!stringTokens.isEmpty) {
      val inserted = completeQuote(doc, caretOffset, caret, c, stringTokens, beginTokenId)
      if (inserted) {
        caret.setDot(caretOffset + 1)
        true
      } else false
    } else false
  }

  /**
   * A hook method called after a character was inserted into the
   * document. The function checks for special characters for
   * completion ()[]'"{} and other conditions and optionally performs
   * changes to the doc and or caret (complets braces, moves caret,
   * etc.)
   * @param document the document where the change occurred
   * @param dotPos position of the character insertion
   * @param target The target
   * @param ch the character that was inserted
   * @return Whether the insert was handled
   * @throws BadLocationException if dotPos is not correct
   */
  @throws(classOf[BadLocationException])
  override def afterCharInserted(document: Document, dotPos: Int, target: JTextComponent, ch: Char): Boolean = {
    isAfter = true
    val caret = target.getCaret
    val doc = document.asInstanceOf[BaseDocument]

    //        if (REFLOW_COMMENTS) {
    //            Token<?extends ScalaTokenId> token = ScalaLexUtil.getToken(doc, dotPos);
    //            if (token != null) {
    //                TokenId id = token.id();
    //                if (id == ScalaTokenId.LINE_COMMENT || id == ScalaTokenId.DOCUMENTATION) {
    //                    new ReflowParagraphAction().reflowEditedComment(target);
    //                }
    //            }
    //        }

    // See if our automatic adjustment of indentation when typing (for example) "end" was
    // premature - if you were typing a longer word beginning with one of my adjustment
    // prefixes, such as "endian", then put the indentation back.
    if (previousAdjustmentOffset != -1) {
      if (dotPos == previousAdjustmentOffset) {
        // Revert indentation iff the character at the insert position does
        // not start a new token (e.g. the previous token that we reindented
        // was not complete)
        ScalaLexUtil.getTokenSequence(doc, dotPos) foreach {ts =>
          ts.move(dotPos)
          if (ts.moveNext && ts.offset < dotPos) {
            GsfUtilities.setLineIndentation(doc, dotPos, previousAdjustmentIndent)
          }
        }
      }

      previousAdjustmentOffset = -1
    }

    //dumpTokens(doc, dotPos);
    ch match {
      //        case '#': {
      //            // Automatically insert #{^} when typing "#" in a quoted string or regexp
      //            Token<?extends ScalaTokenId> token = ScalaLexUtil.getToken(doc, dotPos);
      //            if (token == null) {
      //                return true;
      //            }
      //            TokenId id = token.id();
      //
      //            if (id == ScalaTokenId.QUOTED_STRING_LITERAL || id == ScalaTokenId.REGEXP_LITERAL) {
      //                document.insertString(dotPos+1, "{}", null);
      //                // Skip the "{" to place the caret between { and }
      //                caret.setDot(dotPos+2);
      //            }
      //            break;
      //        }
      case '}' | '{' | ')' | ']' | '(' | '[' =>
        if (!isInsertMatchingEnabled(doc)) {
          return false
        }

        ScalaLexUtil.getToken(doc, dotPos) match {
          case Some(token) => token.id match {
              case ScalaTokenId.ANY_OPERATOR =>
                val length = token.length
                val s = token.text.toString
                if (length == 2 && "[]" == s || "[]=" == s) { // Special case
                  skipClosingBracket(doc, caret, ch, ScalaTokenId.RBracket)
                  return true
                }
              case ScalaTokenId.Identifier if token.length == 1 =>
                ch match {
                  case ']' => skipClosingBracket(doc, caret, ch, ScalaTokenId.RBracket)
                  case ')' => skipClosingBracket(doc, caret, ch, ScalaTokenId.RParen)
                  case '}' => skipClosingBracket(doc, caret, ch, ScalaTokenId.RBrace)
                  case '[' | '(' | '{' =>
                    completeOpeningBracket(doc, dotPos, caret, ch)
                }
              case ScalaTokenId.LBracket | ScalaTokenId.RBracket | ScalaTokenId.LBrace | ScalaTokenId.RBrace | ScalaTokenId.LParen | ScalaTokenId.RParen =>
                ch match {
                  case ']' => skipClosingBracket(doc, caret, ch, ScalaTokenId.RBracket)
                  case ')' => skipClosingBracket(doc, caret, ch, ScalaTokenId.RParen)
                  case '}' => skipClosingBracket(doc, caret, ch, ScalaTokenId.RBrace)
                  case '[' | '(' | '{' =>
                    completeOpeningBracket(doc, dotPos, caret, ch)
                }
              case _ =>
            }
          case None => return true
        }

        // Reindent blocks (won't do anything if `}` is not at the beginning of a line
        ch match {
          case '}' =>
            reindent(doc, dotPos, ScalaTokenId.RBrace, caret)
          case ']' =>
            reindent(doc, dotPos, ScalaTokenId.RBracket, caret)
          case ')' =>
            reindent(doc, dotPos, ScalaTokenId.RBracket, caret)
          case _ =>
        }
        
      case 'e' =>
        //* See if it's the end of and `case`, if so, reindent
        reindent(doc, dotPos, ScalaTokenId.Case, caret)


        //        case 'e':
        //            // See if it's the end of an "else" or an "ensure" - if so, reindent
        //            reindent(doc, dotPos, ScalaTokenId.ELSE, caret);
        //            reindent(doc, dotPos, ScalaTokenId.ENSURE, caret);
        //            reindent(doc, dotPos, ScalaTokenId.RESCUE, caret);
        //
        //            break;
        //
        //        case 'f':
        //            // See if it's the end of an "else" - if so, reindent
        //            reindent(doc, dotPos, ScalaTokenId.ELSIF, caret);
        //
        //            break;
        //
        //        case 'n':
        //            // See if it's the end of an "when" - if so, reindent
        //            reindent(doc, dotPos, ScalaTokenId.WHEN, caret);
        //
        //            break;

      case '/' =>
        if (!isInsertMatchingEnabled(doc)) {
          return false
        }

        // Bracket matching for regular expressions has to be done AFTER the
        // character is inserted into the document such that I can use the lexer
        // to determine whether it's a division (e.g. x/y) or a regular expression (/foo/)
        ScalaLexUtil.getPositionedSequence(doc, dotPos) foreach {ts =>
          val token = ts.token
          token.id match {
            case ScalaTokenId.LineComment =>
              // Did you just type "//" - make sure this didn't turn into ///
              // where typing the first "/" inserted "//" and the second "/" appended
              // another "/" to make "///"
              if (dotPos == ts.offset + 1 && dotPos + 1 < doc.getLength &&
                  doc.getText(dotPos + 1, 1).charAt(0) == '/') {
                doc.remove(dotPos, 1)
                caret.setDot(dotPos + 1)
                return true
              }
            case ScalaTokenId.REGEXP_BEGIN | ScalaTokenId.REGEXP_END =>
              val stringTokens = REGEXP_TOKENS
              val beginTokenId = ScalaTokenId.REGEXP_BEGIN

              val inserted = completeQuote(doc, dotPos, caret, ch, stringTokens, beginTokenId)
              if (inserted) {
                caret.setDot(dotPos + 1)
              }

              return inserted
            case _ =>
          }

        }
      case _ =>
    }


    true
  }

  @throws(classOf[BadLocationException])
  private def reindent(doc: BaseDocument, offset: Int, id: TokenId, caret: Caret): Unit = {
    val ts = ScalaLexUtil.getTokenSequence(doc, offset).getOrElse(return)
    ts.move(offset)
    if (!ts.moveNext && !ts.movePrevious) {
      return
    }

    val token = ts.token
      
    if (token.id == id) {
      val rowFirstNonWhite = Utilities.getRowFirstNonWhite(doc, offset)
      // Ensure that this token is at the beginning of the line
      if (ts.offset > rowFirstNonWhite) {
        return
      }

      val begin = id match {
        case ScalaTokenId.RBrace =>
          ScalaLexUtil.findBwd(ts, ScalaTokenId.LBrace, ScalaTokenId.RBrace)
        case ScalaTokenId.RBracket =>
          ScalaLexUtil.findBwd(ts, ScalaTokenId.LBracket, ScalaTokenId.RBracket)
        case ScalaTokenId.Case =>
          // * find the first unbalanced LBrace, then next `case`
          ScalaLexUtil.findBwd(ts, ScalaTokenId.LBrace, ScalaTokenId.RBrace) match {
            case OffsetRange.NONE => OffsetRange.NONE
            case _ if ts.moveNext => // LBrace found, now find followed `case`
              ScalaLexUtil.findNextNoWsNoComment(ts) match {
                case Some(tk) if tk.id == ScalaTokenId.Case =>
                  new OffsetRange(ts.offset, ts.offset + 1)
                case _ => OffsetRange.NONE
              }
          }
        case _ => OffsetRange.NONE
      }

      if (begin != OffsetRange.NONE) {
        val beginOffset = begin.getStart
        val indent = GsfUtilities.getLineIndent(doc, beginOffset)
        previousAdjustmentIndent = GsfUtilities.getLineIndent(doc, offset)
        GsfUtilities.setLineIndentation(doc, offset, indent)
        previousAdjustmentOffset = caret.getDot
      }
    }
    
  }

  /**
   * Hook called after a character *ch* was backspace-deleted from
   * *doc*. The function possibly removes bracket or quote pair if
   * appropriate.
   * @param doc the document
   * @param dotPos position of the change
   * @param caret caret
   * @param ch the character that was deleted
   */
  @throws(classOf[BadLocationException])
  override def charBackspaced(document: Document, dotPos: Int, target: JTextComponent, ch: Char): Boolean = {
    val doc = document.asInstanceOf[BaseDocument]

    ch match {
      case ' ' =>
        // Backspacing over "// " ? Delete the "//" too!
        ScalaLexUtil.getPositionedSequence(doc, dotPos) foreach {ts =>
          if (ts.token.id == ScalaTokenId.LineComment) {
            if (ts.offset == dotPos - 2) {
              doc.remove(dotPos - 2, 2)
              target.getCaret.setDot(dotPos - 2)

              return true
            }
          }
        }
      case '{' | '(' | '[' => // and '{' via fallthrough
        ScalaLexUtil.getTokenChar(doc, dotPos) match { // tokenAtDot
          case ']' if ScalaLexUtil.getTokenBalance(doc, ScalaTokenId.LBracket, ScalaTokenId.RBracket, dotPos) != 0 =>
            doc.remove(dotPos, 1)
          case ')' if ScalaLexUtil.getTokenBalance(doc, ScalaTokenId.LParen, ScalaTokenId.RParen, dotPos) != 0 =>
            doc.remove(dotPos, 1)
          case '}' if ScalaLexUtil.getTokenBalance(doc, ScalaTokenId.LBrace, ScalaTokenId.RBrace, dotPos) != 0 =>
            doc.remove(dotPos, 1)
          case _ =>
        }

      case '/' =>
        // Backspacing over "//" ? Delete the whole "//"
        ScalaLexUtil.getPositionedSequence(doc, dotPos) foreach {ts =>
          if (ts.token.id == ScalaTokenId.REGEXP_BEGIN) {
            if (ts.offset == dotPos - 1) {
              doc.remove(dotPos - 1, 1)
              target.getCaret.setDot(dotPos - 1)

              return true
            }
          }
        }
        // Fallthrough for match-deletion
      case '|' | '\"' | '\'' =>
        val mtch = doc.getChars(dotPos, 1)
        if (mtch != null && mtch(0) == ch) {
          doc.remove(dotPos, 1)
        }
      case _ =>
        // TODO: Test other auto-completion chars, like %q-foo-
    }
    
    true
  }

  /**
   * A hook to be called after closing bracket ) or ] was inserted into
   * the document. The method checks if the bracket should stay there
   * or be removed and some exisitng bracket just skipped.
   *
   * @param doc the document
   * @param dotPos position of the inserted bracket
   * @param caret caret
   * @param bracket the bracket character ']' or ')'
   */
  @throws(classOf[BadLocationException])
  private def skipClosingBracket(doc: BaseDocument, caret: Caret, bracket: Char, bracketId: TokenId): Unit = {
    val caretOffset = caret.getDot
    if (isSkipClosingBracket(doc, caretOffset, bracketId)) {
      doc.remove(caretOffset - 1, 1)
      caret.setDot(caretOffset) // skip closing bracket
    }
  }

  /**
   * Check whether the typed bracket should stay in the document
   * or be removed.
   * <br>
   * This method is called by <code>skipClosingBracket()</code>.
   *
   * @param doc document into which typing was done.
   * @param caretOffset
   */
  @throws(classOf[BadLocationException])
  private def isSkipClosingBracket(doc: BaseDocument, caretOffset: Int, bracketId: TokenId): Boolean = {
    // First check whether the caret is not after the last char in the document
    // because no bracket would follow then so it could not be skipped.
    if (caretOffset == doc.getLength) {
      return false // no skip in this case
    }

    var skipClosingBracket = false // by default do not remove

    val ts = ScalaLexUtil.getTokenSequence(doc, caretOffset).getOrElse(return false)
    // XXX BEGIN TOR MODIFICATIONS
    //ts.move(caretOffset+1);
    ts.move(caretOffset)
    if (!ts.moveNext) {
      return false
    }

    var token = ts.token
    // Check whether character follows the bracket is the same bracket
    if (token != null && token.id == bracketId) {
      val bracketIntId = bracketId.ordinal
      val leftBracketIntId = if (bracketIntId == ScalaTokenId.RParen.ordinal) {
        ScalaTokenId.LParen.ordinal
      } else {
        ScalaTokenId.LBracket.ordinal
      }

      // Skip all the brackets of the same type that follow the last one
      ts.moveNext

      var nextToken = ts.token
      var break = false
      while (nextToken != null && nextToken.id == bracketId && ts.moveNext && !break) {
        token = nextToken

        if (!ts.moveNext) {
          break = true
        } else {
          nextToken = ts.token
        }
      }

      // token var points to the last bracket in a group of two or more right brackets
      // Attempt to find the left matching bracket for it
      // Search would stop on an extra opening left brace if found
      var braceBalance = 0 // balance of '{' and '}'
      var bracketBalance = -1 // balance of the brackets or parenthesis
      val lastRBracket = token
      ts.movePrevious
      token = ts.token

      var finished = false
      while (!finished && token != null) {
        val tokenIntId = token.id.ordinal
        token.id match {
          case ScalaTokenId.LParen | ScalaTokenId.LBracket =>
            if (tokenIntId == bracketIntId) {
              bracketBalance += 1

              if (bracketBalance == 0) {
                if (braceBalance != 0) {
                  // Here the bracket is matched but it is located
                  // inside an unclosed brace block
                  // e.g. ... ->( } a()|)
                  // which is in fact illegal but it's a question
                  // of what's best to do in this case.
                  // We chose to leave the typed bracket
                  // by setting bracketBalance to 1.
                  // It can be revised in the future.
                  bracketBalance = 1
                }

                finished = true
              }
            }
          case ScalaTokenId.RParen | ScalaTokenId.RBracket =>
            if (tokenIntId == bracketIntId) {
              bracketBalance -= 1
            }
          case ScalaTokenId.LBrace =>
            braceBalance += 1

            if (braceBalance > 0) { // stop on extra left brace
              finished = true
            }
          case ScalaTokenId.RBrace =>
            braceBalance -= 1
          case _ =>
        }

        if (!ts.movePrevious) {
          finished = true
        } else {
          token = ts.token
        }
      }

      if (bracketBalance != 0) { // not found matching bracket
        // Remove the typed bracket as it's unmatched
        skipClosingBracket = true
      } else { // the bracket is matched
        // Now check whether the bracket would be matched
        // when the closing bracket would be removed
        // i.e. starting from the original lastRBracket token
        // and search for the same bracket to the right in the text
        // The search would stop on an extra right brace if found
        braceBalance = 0
        bracketBalance = 1 // simulate one extra left bracket

        //token = lastRBracket.getNext();
        val th = TokenHierarchy.get(doc)

        val ofs = lastRBracket.offset(th)

        ts.move(ofs)
        ts.moveNext
        token = ts.token
        finished = false
        while (!finished && token != null) {
          //int tokenIntId = token.getTokenID().getNumericID();
          token.id match {
            case ScalaTokenId.LParen | ScalaTokenId.LBracket =>
              if (token.id.ordinal == leftBracketIntId) {
                bracketBalance += 1
              }
            case ScalaTokenId.RParen | ScalaTokenId.RBracket =>
              if (token.id.ordinal == bracketIntId) {
                bracketBalance -= 1

                if (bracketBalance == 0) {
                  if (braceBalance != 0) {
                    // Here the bracket is matched but it is located
                    // inside an unclosed brace block
                    // which is in fact illegal but it's a question
                    // of what's best to do in this case.
                    // We chose to leave the typed bracket
                    // by setting bracketBalance to -1.
                    // It can be revised in the future.
                    bracketBalance = -1
                  }

                  finished = true
                }
              }
            case ScalaTokenId.LBrace =>
              braceBalance += 1
            case ScalaTokenId.RBrace =>
              braceBalance -= 1

              if (braceBalance < 0) { // stop on extra right brace
                finished = true
              }
            case _ =>
          }

          //token = token.getPrevious(); // done regardless of finished flag state
          if (!ts.movePrevious) {
            finished = true
          } else {
            token = ts.token
          }
        }

        // If bracketBalance == 0 the bracket would be matched
        // by the bracket that follows the last right bracket.
        skipClosingBracket = (bracketBalance == 0)
      }
    }

    skipClosingBracket
  }

  /**
   * Check for various conditions and possibly add a pairing bracket
   * to the already inserted.
   * @param doc the document
   * @param dotPos position of the opening bracket (already in the doc)
   * @param caret caret
   * @param bracket the bracket that was inserted
   */
  @throws(classOf[BadLocationException])
  private def completeOpeningBracket(doc: BaseDocument, dotPos: Int, caret: Caret, bracket: Char): Unit =  {
    if (isCompletablePosition(doc, dotPos + 1)) {
      val matchingBracket = "" + matching(bracket)
      doc.insertString(dotPos + 1, matchingBracket, null)
      caret.setDot(dotPos + 1)
    }
  }

  // XXX TODO Use embedded string sequence here and see if it
  // really is escaped. I know where those are!
  // TODO Adjust for JavaScript
  @throws(classOf[BadLocationException])
  private def isEscapeSequence(doc: BaseDocument, dotPos: Int): Boolean = {
    if (dotPos <= 0) {
      return false
    }

    val previousChar = doc.getChars(dotPos - 1, 1)(0)

    previousChar == '\\'
  }

  /**
   * Check for conditions and possibly complete an already inserted
   * quote .
   * @param doc the document
   * @param dotPos position of the opening bracket (already in the doc)
   * @param caret caret
   * @param bracket the character that was inserted
   */
  @throws(classOf[BadLocationException])
  private def completeQuote(doc: BaseDocument, dotPos: Int, caret: Caret, bracket: Char,
                            stringTokens: Set[TokenId], beginToken: TokenId): Boolean =  {
    if (isEscapeSequence(doc, dotPos)) { // \" or \' typed
      return false
    }

    // Examine token at the caret offset
    if (doc.getLength < dotPos) {
      return false
    }

    val ts = ScalaLexUtil.getTokenSequence(doc, dotPos).getOrElse(return false)
    ts.move(dotPos)
    if (!ts.moveNext && !ts.movePrevious) {
      return false
    }

    var token = ts.token
    val previousToken =  if (ts.movePrevious) {
      ts.token
    } else null

    val lastNonWhite = Utilities.getRowLastNonWhite(doc, dotPos)

    // eol - true if the caret is at the end of line (ignoring whitespaces)
    val eol = lastNonWhite < dotPos

    if (ScalaLexUtil.isComment(token.id)) {
      return false
    } else if (token.id == ScalaTokenId.Ws && eol && (dotPos - 1) > 0) {
      // check if the caret is at the very end of the line comment
      ScalaLexUtil.getTokenId(doc, dotPos - 1) match {
        case Some(ScalaTokenId.LineComment) => return false
        case _ =>
      }
    }

    val completablePosition = isQuoteCompletablePosition(doc, dotPos)

    var insideString = false
    val id = token.id

    var break = false
    for (currId <- stringTokens if !break) {
      if (id == currId) {
        insideString = true
        break = true
      }
    }

    if (id == ScalaTokenId.Error && previousToken != null && previousToken.id == beginToken) {
      insideString = true
    }

    if (id == ScalaTokenId.Nl && previousToken != null) {
      if (previousToken.id == beginToken) {
        insideString = true;
      } else if (previousToken.id == ScalaTokenId.Error) {
        if (ts.movePrevious) {
          if (ts.token.id == beginToken) {
            insideString = true
          }
        }
      }
    }

    if (!insideString) {
      // check if the caret is at the very end of the line and there
      // is an unterminated string literal
      if (token.id == ScalaTokenId.Ws && eol) {
        if ((dotPos - 1) > 0) {
          ScalaLexUtil.getTokenId(doc, dotPos - 1) match {
            case Some(ScalaTokenId.StringLiteral) => insideString = true  // XXX TODO use language embedding to handle this
            case _ =>
          }
        }
      }
    }

    if (insideString) {
      if (eol) {
        return false // do not complete
      } else {
        //#69524
        val chr = doc.getChars(dotPos, 1)(0)

        if (chr == bracket) {
          if (!isAfter) {
            doc.insertString(dotPos, "" + bracket, null) //NOI18N
          } else {
            if (!(dotPos < doc.getLength - 1 && doc.getText(dotPos + 1, 1).charAt(0) == bracket)) {
              return true
            }
          }

          doc.remove(dotPos, 1)

          return true
        }
      }
    }

    if ((completablePosition && !insideString) || eol) {
      doc.insertString(dotPos, "" + bracket + (if (isAfter) "" else matching(bracket)), null); //NOI18N
      true
    } else false
  }

  /**
   * Checks whether dotPos is a position at which bracket and quote
   * completion is performed. Brackets and quotes are not completed
   * everywhere but just at suitable places .
   * @param doc the document
   * @param dotPos position to be tested
   */
  @throws(classOf[BadLocationException])
  private def isCompletablePosition(doc: BaseDocument, dotPos: Int): Boolean = {
    if (dotPos == doc.getLength) { // there's no other character to test
      return true
    } else {
      // test that we are in front of ) , " or '
      doc.getChars(dotPos, 1)(0) match {
        case ')' |  ',' | '\"' | '\'' | ' ' | ']' | '}' | '\n' | '\t' | ';' => true
        case _ => false
      }
    }
  }

  @ throws(classOf[BadLocationException])
  private def isQuoteCompletablePosition(doc: BaseDocument, dotPos: Int): Boolean = {
    if (dotPos == doc.getLength) { // there's no other character to test
      return true
    } else {
      // test that we are in front of ) , " or ' ... etc.
      val eol = Utilities.getRowEnd(doc, dotPos)

      if (dotPos == eol || eol == -1) {
        return false
      }

      val firstNonWhiteFwd = Utilities.getFirstNonWhiteFwd(doc, dotPos, eol)
      if (firstNonWhiteFwd == -1) {
        return false
      }

      doc.getChars(firstNonWhiteFwd, 1)(0) match {
        case ')' | ',' | '+' | '}' | ';' | ']' | '/' => true
        case _ => false
      }
    }
  }

  /**
   * Returns for an opening bracket or quote the appropriate closing
   * character.
   */
  private def matching(bracket: Char): Char = {
    bracket match {
      case '"'  => '"'
      case '\'' => '\''
      case '('  => ')'
      case '/'  => '/'
      case '['  => ']'
      case '{'  => '}'
      case '}'  => '{'
      case '`'  => '`'
      case '<'  => '>'
      case _ => bracket
    }
  }

  override def findLogicalRanges(info: ParserResult, caretOffset: Int): java.util.List[OffsetRange] = {
    val pResult = info.asInstanceOf[ScalaParserResult]
    val root = pResult.rootScope

    val astOffset = ScalaLexUtil.getAstOffset(info, caretOffset)
    if (astOffset == -1) {
      return java.util.Collections.emptyList[OffsetRange]
    }

    val ranges = new  java.util.ArrayList[OffsetRange]

    /** Furthest we can go back in the buffer (in RHTML documents, this
     * may be limited to the surrounding &lt;% starting tag
     */
    var min = 0
    var max = Integer.MAX_VALUE
    var length = 0

    // Check if the caret is within a comment, and if so insert a new
    // leaf "node" which contains the comment line and then comment block
    try {
      val doc = info.getSnapshot.getSource.getDocument(true) match {
        case null => return ranges
        case x: BaseDocument => x
      }
      length = doc.getLength

      //            if (RubyUtils.isRhtmlDocument(doc)) {
      //                TokenHierarchy th = TokenHierarchy.get(doc);
      //                TokenSequence ts = th.tokenSequence();
      //                ts.move(caretOffset);
      //                if (ts.moveNext() || ts.movePrevious()) {
      //                    Token t = ts.token();
      //                    if (t.id().primaryCategory().startsWith("ruby")) { // NOI18N
      //                        min = ts.offset();
      //                        max = min+t.length();
      //                        // Try to extend with delimiters too
      //                        if (ts.movePrevious()) {
      //                            t = ts.token();
      //                            if ("ruby-delimiter".equals(t.id().primaryCategory())) { // NOI18N
      //                                min = ts.offset();
      //                                if (ts.moveNext() && ts.moveNext()) {
      //                                    t = ts.token();
      //                                    if ("ruby-delimiter".equals(t.id().primaryCategory())) { // NOI18N
      //                                        max = ts.offset()+t.length();
      //                                    }
      //                                }
      //                            }
      //                        }
      //                    }
      //                }
      //            }


      ScalaLexUtil.getPositionedSequence(doc, caretOffset) foreach {ts =>
        val token = ts.token
        if (token == null) {
          return ranges
        }
        token.id match {
          case id if ScalaLexUtil.isBlockComment(id) || ScalaLexUtil.isDocComment(id) =>
            // First add a range for the current line
            val begin = ts.offset
            val end = begin + token.length
            ranges.add(new OffsetRange(begin, end))
          case ScalaTokenId.LineComment =>
            // First add a range for the current line
            var begin = Utilities.getRowStart(doc, caretOffset)
            var end = Utilities.getRowEnd(doc, caretOffset)

            if (ScalaLexUtil.isCommentOnlyLine(doc, caretOffset)) {
              ranges.add(new OffsetRange(Utilities.getRowFirstNonWhite(doc, begin),
                                         Utilities.getRowLastNonWhite(doc, end) + 1))

              val lineBegin = begin
              val lineEnd = end
              var break = false
              while (begin > 0 && !break) {
                val newBegin = Utilities.getRowStart(doc, begin - 1)

                begin = if (newBegin < 0 || !ScalaLexUtil.isCommentOnlyLine(doc, newBegin)) {
                  break = true
                  Utilities.getRowFirstNonWhite(doc, begin)
                } else {
                  newBegin
                }
              }

              break = false
              while (!break) {
                val newEnd = Utilities.getRowEnd(doc, end + 1)

                end = if (newEnd >= length || !ScalaLexUtil.isCommentOnlyLine(doc, newEnd)) {
                  break = true
                  Utilities.getRowLastNonWhite(doc, end) + 1
                } else {
                  newEnd
                }
              }

              if (lineBegin > begin || lineEnd < end) {
                ranges.add(new OffsetRange(begin, end))
              }
            } else {
              // It's just a line comment next to some code; select the comment
              val th = TokenHierarchy.get(doc)
              val offset = token.offset(th)
              ranges.add(new OffsetRange(offset, offset + token.length))
            }
        }
      }
    } catch {case ble: BadLocationException =>
        Exceptions.printStackTrace(ble)
        return ranges
    }

    /** @TODO caoyuan */

    //        Iterator<Node> it = (Iterator<Node>) root.iterator();//path.leafToRoot();
    //
    //        OffsetRange previous = OffsetRange.NONE;
    //        while (it.hasNext()) {
    //            Node node = it.next();
    //
    ////            // Filter out some uninteresting nodes
    ////            if (node instanceof NewlineNode) {
    ////                continue;
    ////            }
    //
    //            //OffsetRange range = AstUtilities.getRange(node);
    //            OffsetRange range = new OffsetRange(node.getLocation().offset, node.getLocation().endOffset);
    //
    //            // The contains check should be unnecessary, but I end up getting
    //            // some weird positions for some Rhino AST nodes
    //            if (range.containsInclusive(astOffset) && !range.equals(previous)) {
    //                range = ScalaLexUtil.getLexerOffsets(info, range);
    //                if (range != OffsetRange.NONE) {
    //                    if (range.getStart() < min) {
    //                        ranges.add(new OffsetRange(min, max));
    //                        ranges.add(new OffsetRange(0, length));
    //                        break;
    //                    }
    //                    ranges.add(range);
    //                    previous = range;
    //                }
    //            }
    //        }
    ranges
  }

  // UGH - this method has gotten really ugly after successive refinements based on unit tests - consider cleaning up
  override def getNextWordOffset(document: Document, offset: Int, reverse: Boolean): Int = {
    val doc = document.asInstanceOf[BaseDocument]
    val ts = ScalaLexUtil.getTokenSequence(doc, offset).getOrElse(return -1)
    ts.move(offset)
    if (!ts.moveNext && !ts.movePrevious) {
      return -1
    }

    if (reverse && ts.offset == offset) {
      if (!ts.movePrevious) {
        return -1
      }
    }

    var token = ts.token
    var id = token.id
    
    if (id == ScalaTokenId.Ws) {
      // Just eat up the space in the normal IDE way
      if (reverse && ts.offset < offset || !reverse && ts.offset > offset) {
        return ts.offset
      }

      while (id == ScalaTokenId.Ws) {
        if (reverse && !ts.movePrevious) {
          return -1
        } else if (!reverse && !ts.moveNext) {
          return -1
        }

        token = ts.token
        id = token.id
      }

      if (reverse) {
        val start = ts.offset + token.length
        if (start < offset) {
          return start
        }
      } else {
        val start = ts.offset
        if (start > offset) {
          return start
        }
      }
    }

    id match {
      case ScalaTokenId.Identifier | ScalaTokenId.CONSTANT | ScalaTokenId.GLOBAL_VAR =>
        val s = token.text.toString
        val length = s.length
        val wordOffset = offset - ts.offset
        if (reverse) {
          // Find previous
          val offsetInImage = offset - 1 - ts.offset
          if (offsetInImage < 0) {
            return -1
          }
          if (offsetInImage < length && Character.isUpperCase(s.charAt(offsetInImage))) {
            for (i <- offsetInImage - 1 to 0 if i >= 0) {
              val charAtI = s.charAt(i)
              if (charAtI == '_') {
                // return offset of previous uppercase char in the identifier
                return ts.offset + i + 1
              } else if (!Character.isUpperCase(charAtI)) {
                // return offset of previous uppercase char in the identifier
                return ts.offset + i + 1
              }
            }
            return ts.offset
          } else {
            for (i <- offsetInImage - 1 to 0 if i >= 0) {
              val charAtI = s.charAt(i)
              if (charAtI == '_') {
                return ts.offset + i + 1
              }
              if (Character.isUpperCase(charAtI)) {
                // now skip over previous uppercase chars in the identifier
                for (j <- i to 0 if j >= 0) {
                  val charAtJ = s.charAt(j)
                  if (charAtJ == '_') {
                    return ts.offset + j + 1
                  }
                  if (!Character.isUpperCase(charAtJ)) {
                    // return offset of previous uppercase char in the identifier
                    return ts.offset + j + 1
                  }
                }
                return ts.offset
              }
            }

            return ts.offset
          }
        } else {
          // Find next
          var start = wordOffset + 1
          if (wordOffset < 0 || wordOffset >= s.length) {
            // Probably the end of a token sequence, such as this:
            // <%s|%>
            return -1
          }
          if (Character.isUpperCase(s.charAt(wordOffset))) {
            // if starting from a Uppercase char, first skip over follwing upper case chars
            var break = false
            for (i <- start until length if !break) {
              val charAtI = s.charAt(i)
              if (!Character.isUpperCase(charAtI)) {
                break = true
              } else {
                if (s.charAt(i) == '_') {
                  return ts.offset + i
                }
                start += 1
              }
            }
          }
          for (i <- start until length) {
            val charAtI = s.charAt(i)
            if (charAtI == '_' || Character.isUpperCase(charAtI)) {
              return ts.offset + i
            }
          }
        }
      case _ => return -1
    }

    // Default handling in the IDE
    return -1
  }

  /**
   * For debugging purposes
   * Probably obsolete - see the tokenspy utility in gsf debugging tools for better help
   */
  private def dumpTokens(doc: BaseDocument, dot: Int) {
    val ts = ScalaLexUtil.getTokenSequence(doc, dot) match {
      case Some(x) => x
      case None => return false
    }

    println("Dumping tokens for dot=" + dot)
    var prevOffset = -1
    ts.moveStart
    var index = 0
    do {
      val token = ts.token
      val offset = ts.offset
      val id = token.id.toString
      val text = token.text.toString.replaceAll("\n", "\\\\n")
      if (prevOffset < dot && dot <= offset) {
        print(" ===> ")
      }
      println("Token " + index + ": offset=" + offset + ": id=" + id + ": text=" + text)
      index += 1
      prevOffset = offset
    } while (ts.moveNext)
  }
  
}
