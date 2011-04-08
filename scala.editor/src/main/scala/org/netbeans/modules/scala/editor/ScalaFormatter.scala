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

import javax.swing.text.{BadLocationException, Document}
import org.netbeans.api.lexer.{Token, TokenId}
import org.netbeans.editor.{BaseDocument, Utilities}
import org.netbeans.modules.csl.api.Formatter
import org.netbeans.modules.csl.api.OffsetRange
import org.netbeans.modules.csl.spi.{GsfUtilities, ParserResult}
import org.netbeans.modules.editor.indent.spi.Context
import org.openide.filesystems.FileUtil
import org.openide.loaders.DataObject
import org.openide.util.Exceptions

import org.netbeans.modules.scala.core.lexer.{ScalaLexUtil, ScalaTokenId}
import org.netbeans.modules.scala.editor.options.{CodeStyle}

import scala.collection.mutable.{ArrayBuffer, Stack}

/**
 * Formatting and indentation.
 *
 *
 * @author Caoyuan Deng
 */
object ScalaFormatter {
  val BRACE_MATCH_MAP: Map[TokenId, Set[TokenId]] =
    Map(ScalaTokenId.LParen            -> Set(ScalaTokenId.RParen),
        ScalaTokenId.LBrace            -> Set(ScalaTokenId.RBrace),
        ScalaTokenId.LBracket          -> Set(ScalaTokenId.RBracket),
        ScalaTokenId.Case              -> Set(ScalaTokenId.Case,
                                              ScalaTokenId.RBrace),
        ScalaTokenId.DocCommentStart   -> Set(ScalaTokenId.DocCommentEnd),
        ScalaTokenId.BlockCommentStart -> Set(ScalaTokenId.BlockCommentEnd),
        ScalaTokenId.XmlLt             -> Set(ScalaTokenId.XmlSlashGt,
                                              ScalaTokenId.XmlLtSlash)
    )


}

import ScalaFormatter._
class ScalaFormatter(codeStyle: CodeStyle, rightMarginOverride: Int) extends Formatter {

  def this() = this(null, -1)
  
  def needsParserResult: Boolean = {
    false
  }

  override def reindent(context: Context) {
    val document = context.document
    val startOffset = context.startOffset
    val endOffset = context.endOffset

    if (codeStyle != null) {
      // Make sure we're not reindenting HTML content
      reindent(context, document, startOffset, endOffset, null, true)
    } else {
      val f = new ScalaFormatter(CodeStyle.get(document), -1)
      f.reindent(context, document, startOffset, endOffset, null, true)
    }
  }

  override def reformat(context: Context, info: ParserResult) {
    val document = context.document
    val startOffset = context.startOffset
    val endOffset = context.endOffset

    if (codeStyle != null) {
      // Make sure we're not reindenting HTML content
      reindent(context, document, startOffset, endOffset, info, true)
    } else {
      val f = new ScalaFormatter(CodeStyle.get(document), -1)
      f.reindent(context, document, startOffset, endOffset, info, true)
    }
  }

  def indentSize: Int = {
    if (codeStyle != null) {
      codeStyle.indentSize
    } else {
      CodeStyle.get(null.asInstanceOf[Document]).indentSize
    }
  }

  def hangingIndentSize: Int = {
    if (codeStyle != null) {
      codeStyle.continuationIndentSize
    } else {
      CodeStyle.get(null.asInstanceOf[Document]).continuationIndentSize
    }
  }

  /** Compute the initial balance of brackets at the given offset. */
  private def getFormatStableStart(doc: BaseDocument, offset: Int): Int = {
    val ts = ScalaLexUtil.getTokenSequence(doc, offset).getOrElse(return 0)
    ts.move(offset)
    if (!ts.movePrevious) {
      return 0
    }

    // Look backwards to find a suitable context - a class, module or method definition
    // which we will assume is properly indented and balanced
    do {
      val token = ts.token
      token.id match {
        case ScalaTokenId.Object | ScalaTokenId.Trait | ScalaTokenId.Class => 
          // * is this `class`/`object`/`trait` enlcosed in an outer `class`/`object`/`trait`?
          ScalaLexUtil.findBwd(ts, ScalaTokenId.LBrace, ScalaTokenId.RBrace) match {
            case OffsetRange.NONE => return ts.offset
            case range => // go on for outer `class`/`object`/`trait`
          }
        case _ =>
      }
    } while (ts.movePrevious)

    ts.offset
  }

  /**
   * Get the first token on the given line. Similar to LexUtilities.getToken(doc, lineBegin)
   * except (a) it computes the line begin from the offset itself, and more importantly,
   * (b) it handles RHTML tokens specially; e.g. if a line begins with
   * {@code
   *    <% if %>
   * }
   * then the "if" embedded token will be returned rather than the RHTML delimiter, or even
   * the whitespace token (which is the first Ruby token in the embedded sequence).
   *
   * </pre>
   */
  @throws(classOf[BadLocationException])
  private def getFirstTokenOnLine(doc: BaseDocument, offset: Int): Option[Token[_]] = {
    val lineBegin = Utilities.getRowFirstNonWhite(doc, offset)
    if (lineBegin != -1) {
      return ScalaLexUtil.getToken(doc, lineBegin)
    }

    None
  }

  def reindent(context: Context, document:Document, astartOffset: Int, aendOffset: Int, info: ParserResult, indentOnly: Boolean): Unit = {
    var startOffset = astartOffset
    var endOffset = aendOffset
    try {
      val doc = document.asInstanceOf[BaseDocument]

      if (endOffset > doc.getLength) {
        endOffset = doc.getLength
      }

      startOffset = Utilities.getRowStart(doc, startOffset)
      val lineStart = startOffset

      var initialOffset = 0
      var initialIndent = 0
      if (startOffset > 0) {
        val prevOffset = Utilities.getRowStart(doc, startOffset - 1)
        initialOffset = getFormatStableStart(doc, prevOffset)
        initialIndent = GsfUtilities.getLineIndent(doc, initialOffset)
      }

      // When we're formatting sections, include whitespace on empty lines; this
      // is used during live code template insertions for example. However, when
      // wholesale formatting a whole document, leave these lines alone.
      val indentEmptyLines = startOffset != 0 || endOffset != doc.getLength

      // In case of indentOnly (use press <enter>), the endOffset will be the
      // position of newline inserted, to compute the new added line's indent,
      // we need includeEnd.
      val includeEnd = endOffset == doc.getLength || indentOnly

      // Build up a set of offsets and indents for lines where I know I need
      // to adjust the offset. I will then go back over the document and adjust
      // lines that are different from the intended indent. By doing piecemeal
      // replacements in the document rather than replacing the whole thing,
      // a lot of things will work better: breakpoints and other line annotations
      // will be left in place, semantic coloring info will not be temporarily
      // damaged, and the caret will stay roughly where it belongs.
      // TODO - remove initialbalance etc.
      val (offsets, indents) =
        computeIndents(doc, initialIndent, initialOffset, endOffset, info, indentEmptyLines, includeEnd)

      doc.runAtomic(new Runnable {
          def run {
            try {

              // Iterate in reverse order such that offsets are not affected by our edits
              assert(indents.size == offsets.size)
              var break = false
              var i = indents.size - 1
              while (i >= 0 && !break) {
                val lineBegin = offsets(i)
                if (lineBegin < lineStart) {
                  // We're now outside the region that the user wanted reformatting;
                  // these offsets were computed to get the correct continuation context etc.
                  // for the formatter
                  break = true
                } else {
                  var indent = indents(i)
                  if (lineBegin == lineStart && i > 0) {
                    // Look at the previous line, and see how it's indented
                    // in the buffer.  If it differs from the computed position,
                    // offset my computed position (thus, I'm only going to adjust
                    // the new line position relative to the existing editing.
                    // This avoids the situation where you're inserting a newline
                    // in the middle of "incorrectly" indented code (e.g. different
                    // size than the IDE is using) and the newline position ending
                    // up "out of sync"
                    val prevOffset = offsets(i - 1)
                    val prevIndent = indents(i - 1)
                    val actualPrevIndent = GsfUtilities.getLineIndent(doc, prevOffset)
                    if (actualPrevIndent != prevIndent) {
                      // For blank lines, indentation may be 0, so don't adjust in that case
                      if (!(Utilities.isRowEmpty(doc, prevOffset) || Utilities.isRowWhite(doc, prevOffset))) {
                        indent = actualPrevIndent + (indent - prevIndent);
                      }
                    }
                  }

                  if (indent >= 0) { // @todo why? #150319
                    // Adjust the indent at the given line (specified by offset) to the given indent
                    val currentIndent = GsfUtilities.getLineIndent(doc, lineBegin)

                    if (currentIndent != indent) {
                      context.modifyIndent(lineBegin, indent)
                    }
                  }
                }
                
                i -= 1
              }

              if (!indentOnly /* && codeStyle.reformatComments */) {
                //                    reformatComments(doc, startOffset, endOffset);
              }
            } catch {case ble: BadLocationException => Exceptions.printStackTrace(ble)}
          }
        })
    } catch {case ble: BadLocationException => Exceptions.printStackTrace(ble)}
  }

  protected class Brace {

    var token: Token[TokenId] = _
    var lineIdx: Int = _ // idx of `offsets` and `indents` arrays
    var offsetOnline: Int = _ // offset of this token on its line after indent
    var isLatestOnLine: Boolean = _ // last one on this line?
    var onProcessingLine: Boolean = _ // on the processing line?
    var lasestTokenOnLine: Token[TokenId] = _ // lastest non-white token on this line

    override def toString = {
      token.text.toString
    }
  }

  def computeIndents(doc: BaseDocument, initialIndent: Int, startOffset: Int, endOffset: Int, info: ParserResult,
                     indentEmptyLines: Boolean, includeEnd: Boolean): (Array[Int], Array[Int]) = {

    val offsets = new ArrayBuffer[Int]
    val indents = new ArrayBuffer[Int]
    // PENDING:
    // The reformatting APIs in NetBeans should be lexer based. They are still
    // based on the old TokenID apis. Once we get a lexer version, convert this over.
    // I just need -something- in place until that is provided.
    try {
      // Algorithm:
      // Iterate over the range.
      // Accumulate a token balance ( {,(,[, and keywords like class, case, etc. increases the balance,
      //      },),] and "end" decreases it
      // If the line starts with an end marker, indent the line to the level AFTER the token
      // else indent the line to the level BEFORE the token (the level being the balance * indentationSize)
      // Compute the initial balance and indentation level and use that as a "base".
      // If the previous line is not "done" (ends with a comma or a binary operator like "+" etc.
      // add a "hanging indent" modifier.
      // At the end of the day, we're recording a set of line offsets and indents.
      // This can be used either to reformat the buffer, or indent a new line.
      // State:
      var offset = Utilities.getRowStart(doc, startOffset) // The line's offset

      val end = endOffset

      // Pending - apply comment formatting too?
      // XXX Look up RHTML too
      //int indentSize = EditorOptions.get(RubyInstallation.RUBY_MIME_TYPE).getSpacesPerTab();
      //int hangingIndentSize = indentSize;
      // Build up a set of offsets and indents for lines where I know I need
      // to adjust the offset. I will then go back over the document and adjust
      // lines that are different from the intended indent. By doing piecemeal
      // replacements in the document rather than replacing the whole thing,
      // a lot of things will work better: breakpoints and other line annotations
      // will be left in place, semantic coloring info will not be temporarily
      // damaged, and the caret will stay roughly where it belongs.
      // The token balance at the offset
      var indent = 0 // The indentation to be used for the current line

      var prevIndent = 0
      var nextIndent = 0
      var continueIndent = -1

      val openingBraces = new Stack[Brace]
      val specialTokens = new Stack[Brace]

      var idx = 0
      while (!includeEnd && offset < end || includeEnd && offset <= end) {
        val lineBegin = Utilities.getRowFirstNonWhite(doc, offset)
        val lineEnd = Utilities.getRowEnd(doc, offset)

        if (lineBegin != -1) {
          val results = computeLineIndent(indent, prevIndent, continueIndent,
                                          openingBraces, specialTokens,
                                          offsets, indents,
                                          doc, lineBegin, lineEnd, idx)

          indent = results(0)
          nextIndent = results(1)
          continueIndent = results(2)
        }

        if (indent == -1) {
          // Skip this line - leave formatting as it is prior to reformatting
          indent = GsfUtilities.getLineIndent(doc, offset)
        }

        if (indent < 0) {
          indent = 0
        }

        // Insert whitespace on empty lines too -- needed for abbreviations expansion
        if (lineBegin != -1 || indentEmptyLines) {
          indents += indent
          offsets += offset
          idx += 1
        }

        // Shift to next line
        offset = lineEnd + 1
        prevIndent = indent
        indent = nextIndent
      }
    } catch {case ble: BadLocationException => Exceptions.printStackTrace(ble)}

    (offsets.toArray, indents.toArray)
  }

  /**
   * Compute indent for next line, and adjust this line's indent if necessary
   * @return Array[Int]
   *      int(0) - adjusted indent of this line
   *      int(1) - indent for next line
   */
  private def computeLineIndent(aindent: Int, prevIndent: Int, acontinueIndent: Int,
                                openingBraces: Stack[Brace], specialBraces: Stack[Brace],
                                offsets: ArrayBuffer[Int], indents: ArrayBuffer[Int],
                                doc: BaseDocument, lineBegin: Int, lineEnd: Int, lineIdx: Int): Array[Int] = {

    val ts = ScalaLexUtil.getTokenSequence(doc, lineBegin).getOrElse(return Array(aindent, aindent, -1))

    // * Well, a new line begin
    openingBraces foreach {_.onProcessingLine = false}
    specialBraces foreach {_.onProcessingLine = false}

    //val sb = new StringBuilder(); // for debug
    
    // --- Compute new balance and adjust indent (computed by previous `computeLineIndent`) of this line

    var indent = aindent
    var continueIndent = acontinueIndent

    val rawStart = Utilities.getRowStart(doc, lineBegin) // lineBegin is the RowFirstNonWhite
    // * token index on this line (we only count not-white tokens,
    // * if noWSIdx == 0, means the first non-white token on this line
    var noWSIdx = -1
    var latestNoWSToken: Token[TokenId] = null
    var latestNoWSTokenOffset: Int = -1

    try {
      ts.move(lineBegin)
      do {
        val token = ts.token
        if (token != null) {

          val offset = ts.offset
          val id = token.id

          //sb.append(text); // for debug

          if (!ScalaLexUtil.isWsComment(id)) {
            noWSIdx += 1
            latestNoWSToken = token
            latestNoWSTokenOffset = offset
          }

          // * match/add brace
          id.primaryCategory match {
            case "keyword" | "s_keyword" | "separator" | "operator" | "xml" | "comment" =>

              var justClosedBrace: Brace = null

              if (!openingBraces.isEmpty) {
                val brace = openingBraces.top
                val braceId = brace.token.id

                // * `if`, `else`, `for` etc is not contained in BRACE_MATCH_MAP, we'll process them late
                val matchingIds = BRACE_MATCH_MAP.get(braceId)
                if (matchingIds.isDefined && matchingIds.get.contains(id)) { // matched

                  var numClosed = 1 // default

                  // we may need to lookahead 2 steps for some cases:
                  if (braceId == ScalaTokenId.Case) {
                    val backup = openingBraces.pop

                    if (!openingBraces.isEmpty) {
                      //TokenId lookaheadId = openingBraces.peek().token.id();
                      // if resolved is "=>", we may have matched two braces:
                      if (id == ScalaTokenId.RBrace) {
                        numClosed = 2
                      }
                    }

                    openingBraces push backup
                  }

                  for (i <- 0 until numClosed) {
                    justClosedBrace = openingBraces.pop
                  }

                  if (noWSIdx == 0) {
                    // * this token is at the beginning of this line, adjust this line's indent if necessary
                    indent = id match {
                      case ScalaTokenId.Case | ScalaTokenId.RParen | ScalaTokenId.RBracket | ScalaTokenId.RBrace =>
                        openingBraces.size * indentSize
                      case _ => justClosedBrace.offsetOnline
                    }
                  }

                }

              }

              // * determine some cases when first token of this line is:
              if (offset == rawStart) {
                id match {
                  case ScalaTokenId.LineComment =>
                    indent = -1
                  case _ =>
                }
              }

              // * determine some cases when first noWSComment token is:
              if (noWSIdx == 0) {
                id match {
                  case ScalaTokenId.With =>
                    specialBraces.find{_.token.id == ScalaTokenId.Extends} match {
                      case Some(x) =>
                        indent = x.offsetOnline + 3 // indent `with` right align with `extends`
                      case _ =>
                    }
                    /* case ScalaTokenId.Else =>
                     specialBraces.find{_.token.id == ScalaTokenId.If} match {
                     case Some(x) if x.lineIdx != lineIdx => // not on same line
                     val nextLineOfIf = x.lineIdx + 1
                     if (indents.size > nextLineOfIf) {
                     indent = indents(nextLineOfIf) - indentSize // indent `else` left align with next line of `if`
                     }
                     case _ =>
                     } */
                  case _ =>
                }
              }

              // * add new special brace
              id match {
                case ScalaTokenId.Extends =>
                  val newBrace = new Brace
                  newBrace.token = token
                  newBrace.lineIdx = lineIdx
                  // will add indent of this line to offsetOnline late
                  newBrace.offsetOnline = offset - lineBegin
                  newBrace.onProcessingLine = true
                  specialBraces push newBrace
                  /* case ScalaTokenId.If =>
                   // * remember `if` for `else`
                   val newBrace = new Brace
                   newBrace.token = token
                   newBrace.lineIdx = lineIdx
                   // will add indent of this line to offsetOnline late
                   newBrace.offsetOnline = offset - lineBegin
                   newBrace.onProcessingLine = true
                   specialBraces push newBrace */
                case _ =>
              }

              // * add new opening brace
              if (BRACE_MATCH_MAP.contains(id)) {
                var ignore = false
                // is it a case object or class?, if so, do not indent
                if (id == ScalaTokenId.Case) {
                  if (ts.moveNext) {
                    val next = ScalaLexUtil.findNextNoWs(ts).get
                    next.id match {
                      case ScalaTokenId.Object | ScalaTokenId.Class =>
                        ignore = true
                      case _ =>
                    }
                    ts.movePrevious
                  }
                }

                if (!ignore) {
                  val newBrace = new Brace
                  newBrace.token = token
                  // will add indent of this line to offsetOnline later
                  newBrace.offsetOnline = offset - lineBegin
                  newBrace.onProcessingLine = true
                  openingBraces push newBrace
                }
              }
            case _ if id == ScalaTokenId.XmlCDData || (id == ScalaTokenId.StringLiteral && offset < lineBegin) =>
              // * A literal string with more than one line is a whole token and when goes
              // * to second or following lines, will has offset < lineBegin
              if (noWSIdx == 0 || noWSIdx == -1) {
                // * No indentation for literal strings from 2nd line.
                indent = -1
              }
            case _ =>
          }
        }
      } while (ts.moveNext && ts.offset < lineEnd)
        
      // --- now tokens of this line have been processed totally, let's go on some special cases

      if (!openingBraces.isEmpty) {
        openingBraces.top.token.id match {
          case ScalaTokenId.Eq | ScalaTokenId.Else | ScalaTokenId.If | ScalaTokenId.For | ScalaTokenId.Yield | ScalaTokenId.While =>
            // * close these braces here, now, since the indentation has been done in previous computation
            openingBraces pop
          case _ =>
        }
      }

      // * special case for next line indent with `=` is at the end of this line, or unfinished `if` `else` `for`
      // * since this line has been processed totally, we can now traverse ts freely
      if (latestNoWSToken != null) {
        // * move `ts` to `latestNoWSToken` by following 2 statements:
        ts.move(latestNoWSTokenOffset); ts.moveNext
        // * is the next token LBrace? if true, don't open any brace
        ts.moveNext
        ScalaLexUtil.findNextNoWsNoComment(ts) match {
          case Some(x) if x.id == ScalaTokenId.LBrace =>
          case _ =>
            // * go back to latestNoWSToken
            ts.move(latestNoWSTokenOffset); ts.moveNext
              
            latestNoWSToken.id match {
              case ScalaTokenId.Eq | ScalaTokenId.Else | ScalaTokenId.Yield =>
                val offset = ts.offset
                val newBrace = new Brace
                newBrace.token = latestNoWSToken
                // will add indent of this line to offsetOnline late
                newBrace.offsetOnline = offset - lineBegin
                newBrace.onProcessingLine = true
                openingBraces push newBrace
              case ScalaTokenId.RParen =>
                ScalaLexUtil.skipPair(ts, true, ScalaTokenId.LParen, ScalaTokenId.RParen)
                // * check if there is a `if` or `for` extractly before the matched `LParen`
                ScalaLexUtil.findPreviousNoWsNoComment(ts) match {
                  case Some(x) => x.id match {
                      case ScalaTokenId.If =>
                        val offset = ts.offset
                        val newBrace = new Brace
                        newBrace.token = x
                        // will add indent of this line to offsetOnline late
                        newBrace.offsetOnline = offset - lineBegin
                        newBrace.onProcessingLine = true
                        openingBraces push newBrace

                        ts.movePrevious
                        ScalaLexUtil.findPreviousNoWsNoComment(ts) match {
                          case Some(y) if y.id == ScalaTokenId.Else =>
                            // * "else if": keep `offset` to `else` for later hanging
                            newBrace.offsetOnline = ts.offset - lineBegin
                          case _ =>
                        }
                      case ScalaTokenId.While =>
                        val offset = ts.offset
                        ts.movePrevious
                        ScalaLexUtil.findPreviousNoWsNoComment(ts) match {
                          case Some(x) if x.id == ScalaTokenId.RBrace =>
                            ScalaLexUtil.skipPair(ts, true, ScalaTokenId.LBrace, ScalaTokenId.RBrace)
                            ScalaLexUtil.findPreviousNoWsNoComment(ts) match {
                              case Some(y) if y.id != ScalaTokenId.Do =>
                                val newBrace = new Brace
                                newBrace.token = x
                                // will add indent of this line to offsetOnline late
                                newBrace.offsetOnline = offset - lineBegin
                                newBrace.onProcessingLine = true
                                openingBraces push newBrace
                              case _ =>
                            }
                          case _ =>
                            val newBrace = new Brace
                            newBrace.token = x
                            // will add indent of this line to offsetOnline later
                            newBrace.offsetOnline = offset - lineBegin
                            newBrace.onProcessingLine = true
                            openingBraces push newBrace
                        }
                      case ScalaTokenId.For =>
                        val offset = ts.offset
                        val newBrace = new Brace
                        newBrace.token = x
                        // will add indent of this line to offsetOnline late
                        newBrace.offsetOnline = offset - lineBegin
                        newBrace.onProcessingLine = true
                        openingBraces push newBrace
                      case _ =>
                    }
                  case _ =>
                }
              case _ =>
            }
        }
      }

    } catch {
      case e: AssertionError =>
        doc.getProperty(Document.StreamDescriptionProperty) match {
          case dobj: DataObject => Exceptions.attachMessage(e, FileUtil.getFileDisplayName(dobj.getPrimaryFile))
          case _ =>
        }
        
        throw e
    }
    

    // * Now we've got the final indent of this line, adjust offset for new added
    // * braces (which should be on this line)
    for (brace <- openingBraces if brace.onProcessingLine) {
      brace.offsetOnline += indent
      if (brace.token == latestNoWSToken) {
        brace.isLatestOnLine = true
      }
      brace.lasestTokenOnLine = latestNoWSToken
    }

    for (brace <- specialBraces if brace.onProcessingLine) {
      brace.offsetOnline += indent
      if (brace.token == latestNoWSToken) {
        brace.isLatestOnLine = true
      }
      brace.lasestTokenOnLine = latestNoWSToken
    }

    // --- Compute indent for next line
    
    var nextIndent = 0
    val latestOpenBrace = if (!openingBraces.isEmpty) {
      openingBraces.top
    } else null

    // * decide if next line is new or continued continute line
    val isContinueLine = if (latestNoWSToken == null) {
      // * empty line or comment line
      false
    } else {
      if (latestOpenBrace != null && latestOpenBrace.isLatestOnLine) {
        // * we have special case
        (latestOpenBrace.token.id, latestNoWSToken.id) match {
          //case (ScalaTokenId.LParen | ScalaTokenId.LBracket | ScalaTokenId.LBrace, ScalaTokenId.Comma) => true
          case _ => false
        }
      } else false
    }

    if (isContinueLine) {
      // Compute or reset continue indent
      if (continueIndent == -1) {
        // new continue indent
        continueIndent = indent + hangingIndentSize
      } else {
        // keep the same continue indent
      }

      // Continue line
      nextIndent = continueIndent
    } else {
      // Reset continueIndent
      continueIndent = -1

      if (latestOpenBrace == null) {
        // All braces resolved
        nextIndent = 0
      } else {
        nextIndent = latestOpenBrace.token.id match {
          case ScalaTokenId.RArrow =>
            var nearestHangableBrace: Brace = null
            var depth = 0
            var break = false
            val braces = openingBraces.toList
            for (i <- openingBraces.size - 1 to 0 if !break) {
              val brace = braces(i)
              depth += 1
              if (brace.token.id != ScalaTokenId.RArrow) {
                nearestHangableBrace = brace
                break = true
              }
            }

            if (nearestHangableBrace != null) {
              // * Hang it from this brace
              nearestHangableBrace.offsetOnline + depth * indentSize
            } else {
              openingBraces.size * indentSize
            }
            
          case ScalaTokenId.LParen | ScalaTokenId.LBracket | ScalaTokenId.LBrace
            if !latestOpenBrace.isLatestOnLine && (latestOpenBrace.lasestTokenOnLine == null ||
                                                   latestOpenBrace.lasestTokenOnLine.id != ScalaTokenId.RArrow) =>

            latestOpenBrace.offsetOnline + latestOpenBrace.token.text.length

          case ScalaTokenId.BlockCommentStart | ScalaTokenId.DocCommentStart =>

            latestOpenBrace.offsetOnline + 1

          case ScalaTokenId.Eq | ScalaTokenId.Else | ScalaTokenId.If | ScalaTokenId.For | ScalaTokenId.Yield | ScalaTokenId.While =>

            indent + indentSize

          case _ => openingBraces.size * indentSize // default
        }
      }
    }

    Array(indent, nextIndent, continueIndent)
  }
}
