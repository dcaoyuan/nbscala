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

import javax.swing.text.{BadLocationException, JTextComponent}
import org.netbeans.editor.{BaseDocument, Utilities}
import org.netbeans.modules.csl.api.CodeCompletionHandler.QueryType
import org.netbeans.modules.csl.api.{CodeCompletionContext, CodeCompletionHandler, CodeCompletionResult, CompletionProposal,
                                     ElementHandle, OffsetRange, ParameterInfo}
import org.netbeans.modules.csl.spi.{DefaultCompletionResult, ParserResult}
import org.netbeans.modules.parsing.spi.indexing.support.QuerySupport
import org.openide.util.{Exceptions, NbBundle}

import org.netbeans.api.language.util.ast.AstElementHandle
import org.netbeans.modules.scala.core.ScalaParserResult
import org.netbeans.modules.scala.core.ScalaSourceUtil
import org.netbeans.modules.scala.core.ast.ScalaRootScope
import org.netbeans.modules.scala.core.lexer.{ScalaLexUtil, ScalaTokenId}
import scala.tools.nsc.symtab.Flags

/**
 * Code completion handler for JavaScript
 *
 * @todo Do completion on node id's inside $() calls (prototype.js) and $$() calls for CSS rules.
 *   See http://www.sitepoint.com/article/painless-javascript-prototype
 * @todo Track logical classes and inheritance ("extend")
 * @todo Track global variables (these are vars which aren't local). Somehow cooperate work between
 *    semantic highlighter and structure analyzer. I need to only store a single instance of each
 *    global var in the index. The variable visitor should probably be part of the structure analyzer,
 *    since global variables also need to be tracked there. Another possibility is having the
 *    parser track variables - but that's trickier. Perhaps a second pass over the parse tree
 *    (where I set parent pointers) is where I can do this? I can even change node types to be
 *    more obvious...
 * @todo I should NOT include in queries functions that are known to be methods if you're not doing
 *    "unnown type" completion!
 * @todo Today's feature work:
 *    - this.-completion should do something useful
 *    - I need to model prototype inheritance, and then use it in code completion queries
 *    - Skip no-doc'ed methods
 *    - Improve type analysis:
 *        - known types (node, document, ...)
 *        - variable-name guessing (el, doc, etc ...)
 *        - return value tracking
 *    - Improve indexing:
 *        - store @-private, etc.
 *        - more efficient browser-compat flags
 *    - Fix case-sensitivity on index queries such that open type and other forms of completion
 *      work better!
 *  @todo Distinguish properties and globals and functions? Perhaps with attributes in the flags!
 *  @todo Display more information in parameter tooltips, such as type hints (perhaps do smart
 *    filtering Java-style?), and explanations for each parameter
 *  @todo Need preindexing support for unit tests - and separate files
 *
 * @author Tor Norbye
 * @author Caoyuan Deng
 */
object ScalaCodeCompletionHandler {

  def isJsContext(doc: BaseDocument, offset: Int): Boolean = {
    val ts = ScalaLexUtil.getTokenSequence(doc, offset).getOrElse(return false)
    ts.move(offset)
    if (!ts.movePrevious && !ts.moveNext) {
      return true
    }

    val id = ts.token.id
    id.primaryCategory match {
      case "comment" | "string" | "regexp" => false
      case _ => true
    }
  }

}

class ScalaCodeCompletionHandler extends CodeCompletionHandler with ScalaHtmlFormatters {
  import ScalaCodeCompletionHandler._

  override def complete(context: CodeCompletionContext): CodeCompletionResult = {
    // * skip processing other queryType: DOCUMENTATION_QUERY_TYPE, TOOLTIP_QUERY_TYPE etc
    context.getQueryType match {
      case QueryType.ALL_COMPLETION | QueryType.COMPLETION => // go on
      case _ => return CodeCompletionResult.NONE
    }
    
    val pr = context.getParserResult.asInstanceOf[ScalaParserResult]
    val lexOffset = context.getCaretOffset
    val prefix = context.getPrefix match {
      case null => ""
      case x => x
    }

    val doc = pr.getSnapshot.getSource.getDocument(true) match {
      case null => return CodeCompletionResult.NONE
      case x => x.asInstanceOf[BaseDocument]
    }

    val astOffset = ScalaLexUtil.getAstOffset(pr, lexOffset) match {
      case -1 => return CodeCompletionResult.NONE
      case x => x
    }

    val proposals = new java.util.ArrayList[CompletionProposal]
    val completionResult = new DefaultCompletionResult(proposals, false)

    // * Read-lock due to Token hierarchy use
    //doc.readLock
    try {
      val th = pr.getSnapshot.getTokenHierarchy

      val global = pr.global
      import global._

      val completer = new ScalaCodeCompleter(global)
      completer.completionResult = completionResult
      completer.caseSensitive = context.isCaseSensitive
      completer.queryType = context.getQueryType
      completer.pResult = pr
      completer.lexOffset = lexOffset
      completer.astOffset = astOffset
      completer.doc = doc
      completer.prefix = prefix
      completer.kind = QuerySupport.Kind.PREFIX
      completer.th = th
      completer.fileObject = pr.getSnapshot.getSource.getFileObject
      completer.anchor = lexOffset - prefix.length
      //completer.index = ScalaIndex.get(info.getSnapshot().getSource().getFileObject());

      ScalaLexUtil.getTokenId(doc, lexOffset - 1)  match {
        case None => return completionResult
        case ScalaTokenId.LineComment =>
          // TODO - Complete symbols in comments?
          return completionResult
        case ScalaTokenId.BlockCommentData =>
          try {
            completer.completeComments(proposals)
          } catch {case ex: BadLocationException => Exceptions.printStackTrace(ex)}
          return completionResult
        case ScalaTokenId.StringLiteral =>
          //completeStrings(proposals, request)
          return completionResult
        case ScalaTokenId.REGEXP_LITERAL | ScalaTokenId.REGEXP_END =>
          completeRegexps(proposals, completer)
          return completionResult
        case _ =>
      }

      val ts = ScalaLexUtil.getTokenSequence(th, lexOffset - 1).getOrElse(return completionResult)
      ts.move(lexOffset - 1)
      if (!ts.moveNext && !ts.movePrevious) {
        return completionResult
      }

      val caretToken = ts.token
      val closestToken = ScalaLexUtil.findPreviousNoWsNoComment(ts).get
      val lineEnd = Utilities.getRowEnd(doc, ts.offset)
      val isAtNewLine = lexOffset > lineEnd

      /* if (closestToken.id == ScalaTokenId.Import) {
       completer.prefix = ""
       completer.completeImport(proposals)
       return completionResult
       } */

      var offset = astOffset

      val sanitizedRange = pr.sanitizedRange
      if (sanitizedRange != OffsetRange.NONE && sanitizedRange.containsInclusive(offset)) {
        offset = sanitizedRange.getStart
      }

      ts.move(lexOffset)
      if (!ts.moveNext && !ts.movePrevious) {
        return completionResult
      }
      
      // ----- try to complete import first

      (ScalaLexUtil.findImportPrefix(th, lexOffset) match {
          case Nil => None
          case List(selector, dot, qual, _*) if dot.id == ScalaTokenId.Dot => Some((qual, selector.text.toString))
          case List(dot, qual, _*) if dot.id == ScalaTokenId.Dot => Some(qual, "")
          case _ => None
        }) match {
        case Some((qual, selector)) =>
          completer.prefix = selector
          completer.completeSymbolMembers(qual, proposals)
          return completionResult
        case None =>
      }

      // ----- try to complete call

      completer.findCall(ts, th) match {
        case completer.Call(null, _, _) =>
        case completer.Call(base, dot, select) =>
          val go = dot != null || !isAtNewLine

          if (go) {
            completer.prefix = if (select != null) select.text.toString else ""
            // * it should be expecting call proposals, so just return right
            // * now to avoid keyword local vars proposals
            if (completer.completeSymbolMembers(if (dot != null) dot else base, proposals)) {
              return completionResult
            }

            if (dot != null) {
              // * what ever, it should be expecting call proposals, so just return right now to avoid keyword local vars proposals
              return completionResult
            }
          }
      }

      /* val root = pr.rootScope
       if (root != ScalaRootScope.EMPTY) {
       var offset = astOffset

       val sanitizedRange = pr.sanitizedRange
       if (sanitizedRange != OffsetRange.NONE && sanitizedRange.containsInclusive(offset)) {
       offset = sanitizedRange.getStart
       }

       completer.root = root

       ts.move(lexOffset)
       if (!ts.moveNext && !ts.movePrevious) {
       return completionResult
       }

       completer.findCall(s, th) match {
       case completer.Call(null, _, _) =>
       case completer.Call(base, select, caretAfterDot) =>
       val items = root.findItemsAt(th, base.offset(th))
       val baseItem = items find {_.resultType != null} getOrElse {
       items find {x => x.symbol.asInstanceOf[Symbol].hasFlag(Flags.METHOD)} getOrElse {
       if (items.isEmpty) null else items.head
       }
       }

       if (baseItem != null) {
       val go = if (caretAfterDot) {
       true
       } else !isAtNewLine

       if (go) {
       if (select.length > 0) completer.prefix = select
       if (baseItem.symbol != null) {
       if (completer.completeSymbolMembers(baseItem, proposals)) {
       // * it should be expecting call proposals, so just return right
       // * now to avoid keyword local vars proposals
       return completionResult
       }
       }

       if (caretAfterDot) {
       // * what ever, it should be expecting call proposals, so just return right now to avoid keyword local vars proposals
       return completionResult
       }
       }
       }
       }

       // ----- try to complete import

       (ScalaLexUtil.findImportPrefix(th, lexOffset) match {
       case Nil => None
       case List(selector, dot, qual, _*) if dot.id == ScalaTokenId.Dot => Some((qual, selector.text.toString))
       case List(dot, qual, _*) if dot.id == ScalaTokenId.Dot => Some(qual, "")
       case _ => None
       }) match {
       case None =>
       case Some((qualToken, selector)) => root.findItemsAt(qualToken) match {
       case Nil =>
       case head :: xs =>
       completer.completeImport(head, selector, proposals)
       return completionResult
       }
       }
       } */

      if (completer.completeNew(proposals)) {
        return completionResult
      }

      completer.completeLocals(proposals)
      completer.completeKeywords(proposals)

    } finally {
      //doc.readUnlock
    }

    completionResult
  }

  private def completeRegexps(proposals: java.util.List[CompletionProposal], request: ScalaCodeCompleter): Boolean = {
    val prefix = request.prefix

    // Regular expression matching.  {
    //    for (i <- 0 to n = REGEXP_WORDS.length; i < n; i += 2) {
    //      String word = REGEXP_WORDS[i];
    //      String desc = REGEXP_WORDS[i + 1];
    //
    //      if (startsWith(word, prefix)) {
    //        KeywordItem item = new KeywordItem(word, desc, request);
    //        proposals.add(item);
    //      }
    //    }
    //
    true
  }


  //    private boolean completeStrings(List<CompletionProposal> proposals, CompletionRequest request) {
  //        String prefix = request.prefix;
  //
  //        // See if we're in prototype js functions, $() and $F(), and if so,
  //        // offer to complete the function ids
  //        TokenSequence<ScalaTokenId> ts = ScalaLexUtil.getPositionedSequence(request.doc, request.lexOffset);
  //        assert ts != null; // or we wouldn't have been called in the first place
  //        //Token<? extends ScalaTokenId> stringToken = ts.token();
  //        int stringOffset = ts.offset();
  //
  //    tokenLoop:
  //        while (ts.movePrevious()) {
  //            Token<? extends ScalaTokenId> token = ts.token();
  //            TokenId id = token.id();
  //            if (id == ScalaTokenId.Identifier) {
  //                String text = token.text().toString();
  //
  //                if (text.startsWith("$") || text.equals("getElementById") ||  // NOI18N
  //                        text.startsWith("getElementsByTagName") || text.equals("getElementsByName") || // NOI18N
  //                        "addClass".equals(text) || "toggleClass".equals(text)) { // NOI18N
  //
  //                    // Compute a custom prefix
  //                    int lexOffset = request.lexOffset;
  //                    if (lexOffset > stringOffset) {
  //                        try {
  //                            prefix = request.doc.getText(stringOffset, lexOffset - stringOffset);
  //                        } catch (BadLocationException ex) {
  //                            Exceptions.printStackTrace(ex);
  //                        }
  //                    } else {
  //                        prefix = "";
  //                    }
  //                    // Update anchor
  //                    request.anchor = stringOffset;
  //
  //                    boolean jQuery = false;
  //                    if (text.equals("$")) {
  //                        for (String imp : request.result.getStructure().getImports()) {
  //                            if (imp.indexOf("jquery") != -1) { // NOI18N
  //                                jQuery = true;
  //                            }
  //                        }
  //                        if (!jQuery) {
  //                            jQuery = request.index.getType("jQuery") != null;
  //                        }
  //                    }
  //
  //                    if ("getElementById".equals(text) || (!jQuery && ("$".equals(text) || "$F".equals(text)))) { // NOI18N
  //                        addElementIds(proposals, request, prefix);
  //
  //                    } else if ("getElementsByName".equals(text)) { // NOI18N
  //                        addElementClasses(proposals, request, prefix);
  //                    } else if ("addClass".equals(text) || "toggleClass".equals(text)) { // NOI18N
  //                        // From jQuery
  //                        addElementClasses(proposals, request, prefix);
  //                    } else if (text.startsWith("getElementsByTagName")) { // NOI18N
  //                        addTagNames(proposals, request, prefix);
  //                    } else if ("$$".equals(text) || (jQuery && "$".equals(text) && jQuery)) { // NOI18N
  //                        // Selectors
  //                        // Determine whether we want to include elements or classes
  //                        // Classes after [ and .
  //
  //                        int showClasses = 1;
  //                        int showElements = 2;
  //                        int showIds = 3;
  //                        int showSpecial = 4;
  //                        int expect = showElements;
  //                        int i = prefix.length()-1;
  //                     findEnd:
  //                        for (; i >= 0; i--) {
  //                            char c = prefix.charAt(i);
  //                            switch (c) {
  //                            case '.':
  //                            case '[':
  //                                expect = showClasses;
  //                                break findEnd;
  //                            case '#':
  //                                expect = showIds;
  //                                break findEnd;
  //                            case ':':
  //                                expect = showSpecial;
  //                                if (i > 0 && prefix.charAt(i-1) == ':') {
  //                                    // Handle ::'s
  //                                    i--;
  //                                }
  //                                break findEnd;
  //                            case ' ':
  //                            case '/':
  //                            case '>':
  //                            case '+':
  //                            case '~':
  //                            case ',':
  //                                expect = showElements;
  //                                break findEnd;
  //                            default:
  //                                if (!Character.isLetter(c)) {
  //                                    expect = showElements;
  //                                    break findEnd;
  //                                }
  //                            }
  //                        }
  //                        if (i >= 0) {
  //                            prefix = prefix.substring(i+1);
  //                        }
  //                        // Update anchor
  //                        request.anchor = stringOffset+i+1;
  //
  //                        if (expect == showElements) {
  //                            addTagNames(proposals, request, prefix);
  //                        } else if (expect == showIds) {
  //                            addElementIds(proposals, request, prefix);
  //                        } else if (expect == showSpecial) {
  //                            // Regular expression matching.  {
  //                            for (int j = 0, n = CSS_WORDS.length; j < n; j += 2) {
  //                                String word = CSS_WORDS[j];
  //                                String desc = CSS_WORDS[j + 1];
  //                                if (word.startsWith(":") && prefix.length() == 0) {
  //                                    // Filter out the double words
  //                                    continue;
  //                                }
  //                                if (startsWith(word, prefix)) {
  //                                    if (word.startsWith(":")) { // NOI18N
  //                                        word = word.substring(1);
  //                                    }
  //                                    //KeywordItem item = new KeywordItem(word, desc, request);
  //                                    TagItem item = new TagItem(word, desc, request, ElementKind.RULE);
  //                                    proposals.add(item);
  //                                }
  //                            }
  //                        } else {
  //                            assert expect == showClasses;
  //                            addElementClasses(proposals, request, prefix);
  //                        }
  //                    }
  //                }
  //
  //                return true;
  //            } else if (id == ScalaTokenId.STRING_BEGIN) {
  //                stringOffset = ts.offset() + token.length();
  //            } else if (!(id == ScalaTokenId.Ws ||
  //                    id == ScalaTokenId.StringLiteral || id == ScalaTokenId.LParen)) {
  //                break tokenLoop;
  //            }
  //        }
  //
  //        for (int i = 0, n = STRING_ESCAPES.length; i < n; i += 2) {
  //            String word = STRING_ESCAPES[i];
  //            String desc = STRING_ESCAPES[i + 1];
  //
  //            if (startsWith(word, prefix)) {
  //                KeywordItem item = new KeywordItem(word, desc, request);
  //                proposals.add(item);
  //            }
  //        }
  //
  //        return true;
  //    }
  //    private void addElementClasses(List<CompletionProposal> proposals, CompletionRequest request, String prefix) {
  //        ParserResult result = request.info.getEmbeddedResult(JsUtils.HTML_MIME_TYPE, 0);
  //        if (result != null) {
  //            HtmlParserResult htmlResult = (HtmlParserResult)result;
  //            List<SyntaxElement> elementsList = htmlResult.elementsList();
  //            Set<String> classes = new HashSet<String>();
  //            for (SyntaxElement s : elementsList) {
  //                if (s.type() == SyntaxElement.TYPE_TAG) {
  //                    String node = s.text();
  //                    int classIdx = node.indexOf("class=\""); // NOI18N
  //                    if (classIdx != -1) {
  //                        int classIdxEnd = node.indexOf('"', classIdx+7);
  //                        if (classIdxEnd != -1 && classIdxEnd > classIdx+1) {
  //                            String clz = node.substring(classIdx+7, classIdxEnd);
  //                            classes.add(clz);
  //                        }
  //                    }
  //                }
  //            }
  //
  //            String filename = request.fileObject.getNameExt();
  //            for (String tag : classes) {
  //                if (startsWith(tag, prefix)) {
  //                    TagItem item = new TagItem(tag, filename, request, ElementKind.TAG);
  //                    proposals.add(item);
  //                }
  //            }
  //        }
  //    }
  //
  //    private void addTagNames(List<CompletionProposal> proposals, CompletionRequest request, String prefix) {
  //        ParserResult result = request.info.getEmbeddedResult(JsUtils.HTML_MIME_TYPE, 0);
  //        if (result != null) {
  //            HtmlParserResult htmlResult = (HtmlParserResult)result;
  //            List<SyntaxElement> elementsList = htmlResult.elementsList();
  //            Set<String> tagNames = new HashSet<String>();
  //            for (SyntaxElement s : elementsList) {
  //                if (s.type() == SyntaxElement.TYPE_TAG) {
  //                    String node = s.text();
  //                    int start = 1;
  //                    int end = node.indexOf(' ');
  //                    if (end == -1) {
  //                        end = node.length()-1;
  //                    }
  //                    String tag = node.substring(start, end);
  //                    tagNames.add(tag);
  //                }
  //            }
  //
  //            String filename = request.fileObject.getNameExt();
  //
  //            for (String tag : tagNames) {
  //                if (startsWith(tag, prefix)) {
  //                    TagItem item = new TagItem(tag, filename, request, ElementKind.TAG);
  //                    proposals.add(item);
  //                }
  //            }
  //        }
  //    }
  //    private void addElementIds(List<CompletionProposal> proposals, CompletionRequest request, String prefix) {
  //        ParserResult result = request.info.getEmbeddedResult(JsUtils.HTML_MIME_TYPE, 0);
  //        if (result != null) {
  //            HtmlParserResult htmlResult = (HtmlParserResult)result;
  //            Set<SyntaxElement.TagAttribute> elementIds = htmlResult.elementsIds();
  //            String filename = request.fileObject.getNameExt();
  //            for (SyntaxElement.TagAttribute tag : elementIds) {
  //                String elementId = tag.getValue();
  //                // Strip "'s surrounding value, if any
  //                if (elementId.length() > 2 && elementId.startsWith("\"") && // NOI18N
  //                        elementId.endsWith("\"")) { // NOI18N
  //                    elementId = elementId.substring(1, elementId.length()-1);
  //                }
  //
  //                if (startsWith(elementId, prefix)) {
  //                    TagItem item = new TagItem(elementId, filename, request, ElementKind.TAG);
  //                    proposals.add(item);
  //                }
  //            }
  //        }
  //    }
  /**
   * Compute an appropriate prefix to use for code completion.
   * In Strings, we want to return the -whole- string if you're in a
   * require-statement string, otherwise we want to return simply "" or the previous "\"
   * for quoted strings, and ditto for regular expressions.
   * For non-string contexts, just return null to let the default identifier-computation
   * kick in.
   */
  override def getPrefix(info: ParserResult, lexOffset: Int, upToOffset: Boolean): String = {
    try {
      val doc = info.getSnapshot.getSource.getDocument(true).asInstanceOf[BaseDocument]

      val th = info.getSnapshot.getTokenHierarchy

      //            int requireStart = ScalaLexUtil.getRequireStringOffset(lexOffset, th);
      //
      //            if (requireStart != -1) {
      //                // XXX todo - do upToOffset
      //                return doc.getText(requireStart, lexOffset - requireStart);
      //            }

      val ts = ScalaLexUtil.getTokenSequence(th, lexOffset).getOrElse(return null)
      ts.move(lexOffset)
      if (!ts.moveNext && !ts.movePrevious) {
        return null
      }

      if (ts.offset == lexOffset) {
        // We're looking at the offset to the RIGHT of the caret
        // and here I care about what's on the left
        ts.movePrevious
      }

      val token = ts.token

      if (token != null) {
        token.id match {
          case
            ScalaTokenId.STRING_BEGIN | ScalaTokenId.STRING_END | ScalaTokenId.StringLiteral |
            ScalaTokenId.REGEXP_LITERAL | ScalaTokenId.REGEXP_BEGIN | ScalaTokenId.REGEXP_END if lexOffset > 0 =>
            doc.getText(lexOffset - 1, 1).charAt(0) match {
              case '\\' => return "\\"
              case _ => return ""
            }
          case _ =>
        }
        //
        //                // We're within a String that has embedded Js. Drop into the
        //                // embedded language and see if we're within a literal string there.
        //                if (id == ScalaTokenId.EMBEDDED_RUBY) {
        //                    ts = (TokenSequence)ts.embedded();
        //                    assert ts != null;
        //                    ts.move(lexOffset);
        //
        //                    if (!ts.moveNext() && !ts.movePrevious()) {
        //                        return null;
        //                    }
        //
        //                    token = ts.token();
        //                    id = token.id();
        //                }
        //
        //                String tokenText = token.text().toString();
        //
        //                if ((id == ScalaTokenId.STRING_BEGIN) || (id == ScalaTokenId.QUOTED_STRING_BEGIN) ||
        //                        ((id == ScalaTokenId.ERROR) && tokenText.equals("%"))) {
        //                    int currOffset = ts.offset();
        //
        //                    // Percent completion
        //                    if ((currOffset == (lexOffset - 1)) && (tokenText.length() > 0) &&
        //                            (tokenText.charAt(0) == '%')) {
        //                        return "%";
        //                    }
        //                }
        //            }
        //
        //            int doubleQuotedOffset = ScalaLexUtil.getDoubleQuotedStringOffset(lexOffset, th);
        //
        //            if (doubleQuotedOffset != -1) {
        //                // Tokenize the string and offer the current token portion as the text
        //                if (doubleQuotedOffset == lexOffset) {
        //                    return "";
        //                } else if (doubleQuotedOffset < lexOffset) {
        //                    String text = doc.getText(doubleQuotedOffset, lexOffset - doubleQuotedOffset);
        //                    TokenHierarchy hi =
        //                        TokenHierarchy.create(text, JsStringTokenId.languageDouble());
        //
        //                    TokenSequence seq = hi.tokenSequence();
        //
        //                    seq.move(lexOffset - doubleQuotedOffset);
        //
        //                    if (!seq.moveNext() && !seq.movePrevious()) {
        //                        return "";
        //                    }
        //
        //                    TokenId id = seq.token().id();
        //                    String s = seq.token().text().toString();
        //
        //                    if ((id == JsStringTokenId.STRING_ESCAPE) ||
        //                            (id == JsStringTokenId.STRING_INVALID)) {
        //                        return s;
        //                    } else if (s.startsWith("\\")) {
        //                        return s;
        //                    } else {
        //                        return "";
        //                    }
        //                } else {
        //                    // The String offset is greater than the caret position.
        //                    // This means that we're inside the string-begin section,
        //                    // for example here: %q|(
        //                    // In this case, report no prefix
        //                    return "";
        //                }
        //            }
        //
        //            int singleQuotedOffset = ScalaLexUtil.getSingleQuotedStringOffset(lexOffset, th);
        //
        //            if (singleQuotedOffset != -1) {
        //                if (singleQuotedOffset == lexOffset) {
        //                    return "";
        //                } else if (singleQuotedOffset < lexOffset) {
        //                    String text = doc.getText(singleQuotedOffset, lexOffset - singleQuotedOffset);
        //                    TokenHierarchy hi =
        //                        TokenHierarchy.create(text, JsStringTokenId.languageSingle());
        //
        //                    TokenSequence seq = hi.tokenSequence();
        //
        //                    seq.move(lexOffset - singleQuotedOffset);
        //
        //                    if (!seq.moveNext() && !seq.movePrevious()) {
        //                        return "";
        //                    }
        //
        //                    TokenId id = seq.token().id();
        //                    String s = seq.token().text().toString();
        //
        //                    if ((id == JsStringTokenId.STRING_ESCAPE) ||
        //                            (id == JsStringTokenId.STRING_INVALID)) {
        //                        return s;
        //                    } else if (s.startsWith("\\")) {
        //                        return s;
        //                    } else {
        //                        return "";
        //                    }
        //                } else {
        //                    // The String offset is greater than the caret position.
        //                    // This means that we're inside the string-begin section,
        //                    // for example here: %q|(
        //                    // In this case, report no prefix
        //                    return "";
        //                }
        //            }
        //
        //            // Regular expression
        //            int regexpOffset = ScalaLexUtil.getRegexpOffset(lexOffset, th);
        //
        //            if ((regexpOffset != -1) && (regexpOffset <= lexOffset)) {
        //                // This is not right... I need to actually parse the regexp
        //                // (I should use my Regexp lexer tokens which will be embedded here)
        //                // such that escaping sequences (/\\\\\/) will work right, or
        //                // character classes (/[foo\]). In both cases the \ may not mean escape.
        //                String tokenText = token.text().toString();
        //                int index = lexOffset - ts.offset();
        //
        //                if ((index > 0) && (index <= tokenText.length()) &&
        //                        (tokenText.charAt(index - 1) == '\\')) {
        //                    return "\\";
        //                } else {
        //                    // No prefix for regexps unless it's \
        //                    return "";
        //                }
        //
        //                //return doc.getText(regexpOffset, offset-regexpOffset);
        //            }
      }

      val lineBegin = Utilities.getRowStart(doc, lexOffset)
      if (lineBegin != -1) {
        val lineEnd = Utilities.getRowEnd(doc, lexOffset)
        val line = doc.getText(lineBegin, lineEnd - lineBegin)
        val lineOffset = lexOffset - lineBegin
        var start = lineOffset
        if (lineOffset > 0) {
          for (i <- lineOffset - 1 to 0;
               c = line.charAt(i) if ScalaSourceUtil.isIdentifierChar(c))
          {
            start = i
          }
        }

        // Find identifier end
        var prefix = if (upToOffset) {
          line.substring(start, lineOffset)
        } else {
          if (lineOffset == line.length) {
            line.substring(start);
          } else {
            val n = line.length
            var end = lineOffset
            for (j <- lineOffset until n; 
                 d = line.charAt(j) if ScalaSourceUtil.isStrictIdentifierChar(d))
            {
              // Try to accept Foo::Bar as well
              end = j + 1
            }
            line.substring(start, end)
          }
        }

        if (prefix.length > 0) {
          if (prefix.endsWith("::")) {
            return ""
          }

          if (prefix.endsWith(":") && prefix.length > 1) {
            return null
          }

          // Strip out LHS if it's a qualified method, e.g.  Benchmark::measure -> measure
          val q = prefix.lastIndexOf("::")

          if (q != -1) {
            prefix = prefix.substring(q + 2)
          }

          // The identifier chars identified by JsLanguage are a bit too permissive;
          // they include things like "=", "!" and even "&" such that double-clicks will
          // pick up the whole "token" the user is after. But "=" is only allowed at the
          // end of identifiers for example.
          if (prefix.length == 1) {
            val c = prefix.charAt(0)
            if (!(Character.isJavaIdentifierPart(c) || c == '@' || c == '$' || c == ':')) {
              return null
            }
          } else {
            var break = false
            for (i <- prefix.length - 2 to 0; // -2: the last position (-1) can legally be =, ! or ?
                 c = prefix.charAt(i) if !break)
            {
              if (i == 0 && c == ':') {
                // : is okay at the begining of prefixes
              } else if (!(Character.isJavaIdentifierPart(c) || c == '@' || c == '$')) {
                prefix = prefix.substring(i + 1)
                break = true
              }
            }
          }

          prefix
        }
      }
      // Else: normal identifier: just return null and let the machinery do the rest
    } catch {case ble: BadLocationException => Exceptions.printStackTrace(ble)}

    // Default behavior
    null
  }

  override def resolveTemplateVariable(variable: String, info: ParserResult, caretOffset: Int,
                                       name: String , parameters: java.util.Map[_, _]): String = {
    throw new UnsupportedOperationException("Not supported yet.")
  }

  override def resolveLink(link: String, elementHandle: ElementHandle): ElementHandle = {
    if (link.indexOf(':') != -1) {
      new ElementHandle.UrlHandle(link.replace(':', '.'))
    } else null
  }

  /** Determine if we're trying to complete the name of a method on another object rather
   * than an inherited or local one. These should list ALL known methods, unless of course
   * we know the type of the method we're operating on (such as strings or regexps),
   * or types inferred through data flow analysis
   *
   * @todo Look for self or this or super; these should be limited to inherited.
   */
  //    private boolean completeTemplateMembers(List<CompletionProposal> proposals, CompletionRequest request) {
  //
  //        ScalaIndex index = request.index;
  //        String prefix = request.prefix;
  //        int astOffset = request.astOffset;
  //        int lexOffset = request.lexOffset;
  //        AstScope root = request.root;
  //        TokenHierarchy<Document> th = request.th;
  //        BaseDocument doc = request.doc;
  //        NameKind kind = request.kind;
  //        FileObject fileObject = request.fileObject;
  //        AstNode closest = request.node;
  //        ScalaParserResult result = request.result;
  //        CompilationInfo info = request.info;
  //
  //        String fqn = request.fqn;
  //        MaybeCall call = request.call;
  //
  //        TokenSequence<ScalaTokenId> ts = ScalaLexUtil.getTokenSequence(th, lexOffset);
  //
  //        // Look in the token stream for constructs of the type
  //        //   foo.x^
  //        // or
  //        //   foo.^
  //        // and if found, add all methods
  //        // (no keywords etc. are possible matches)
  //        if ((index != null) && (ts != null)) {
  //            boolean skipPrivate = true;
  //
  //            if ((call == MaybeCall.LOCAL) || (call == MaybeCall.NONE)) {
  //                return false;
  //            }
  //
  //            // If we're not sure we're only looking for a method, don't abort after this
  //            boolean done = call.isMethodExpected();
  //
  ////            boolean skipInstanceMethods = call.isStatic();
  //
  //            Set<GsfElement> elements = Collections.emptySet();
  //
  //            String typeQName = call.getType();
  //            String lhs = call.getLhs();
  //
  //            if (typeQName == null) {
  //                if (closest != null) {
  //                    TypeMirror type = null;
  //                    if (closest instanceof FieldCall) {
  //                        // dog.tal|
  //                        type = closest.asType();
  //                    } else if (closest instanceof FunctionCall) {
  //                        // dog.talk().
  //                        type = closest.asType();
  //                    } else if (closest instanceof IdCall) {
  //                        // dog.|
  //                        type = closest.asType();
  //                    } else {
  //                        type = closest.asType();
  //                    }
  //
  //                    if (type != null) {
  //                        typeQName = Type.qualifiedNameOf(type);
  //                    }
  //                }
  //            //Node method = AstUtilities.findLocalScope(node, path);
  //            //if (method != null) {
  //            //    List<Node> nodes = new ArrayList<Node>();
  //            //    AstUtilities.addNodesByType(method, new int[] { org.mozilla.javascript.Token.MISSING_DOT }, nodes);
  //            //    if (nodes.size() > 0) {
  //            //        Node exprNode = nodes.get(0);
  //            //        JsTypeAnalyzer analyzer = new JsTypeAnalyzer(info, /*request.info.getParserResult(),*/ index, method, node, astOffset, lexOffset, doc, fileObject);
  //            //        type = analyzer.getType(exprNode.getParentNode());
  //            //    }
  //            //}
  //            }
  //
  //            if (typeQName == null && call.getPrevCallParenPos() != -1) {
  //                // It's some sort of call
  //                assert call.getType() == null;
  //                assert call.getLhs() == null;
  //
  //                // Try to figure out the call in question
  //                int callEndAstOffset = AstUtilities.getAstOffset(info, call.getPrevCallParenPos());
  //                if (callEndAstOffset != -1) {
  ////                    AstPath callPath = new AstPath(root, callEndAstOffset);
  ////                    Iterator<Node> it = callPath.leafToRoot();
  ////                    while (it.hasNext()) {
  ////                        Node callNode = it.next();
  ////                        if (callNode.getType() == org.mozilla.javascript.Token.FUNCTION) {
  ////                            break;
  ////                        } else if (callNode.getType() == org.mozilla.javascript.Token.CALL) {
  ////                            Node method = AstUtilities.findLocalScope(node, path);
  ////
  ////                            if (method != null) {
  ////                                JsTypeAnalyzer analyzer = new JsTypeAnalyzer(info, /*request.info.getParserResult(),*/ index, method, node, astOffset, lexOffset, doc, fileObject);
  ////                                type = analyzer.getType(callNode);
  ////                            }
  ////                            break;
  ////                        } else if (callNode.getType() == org.mozilla.javascript.Token.GETELEM) {
  ////                            Node method = AstUtilities.findLocalScope(node, path);
  ////
  ////                            if (method != null) {
  ////                                JsTypeAnalyzer analyzer = new JsTypeAnalyzer(info, /*request.info.getParserResult(),*/ index, method, node, astOffset, lexOffset, doc, fileObject);
  ////                                type = analyzer.getType(callNode);
  ////                            }
  ////                            break;
  ////                        }
  ////                    }
  //                }
  //            } else if (typeQName == null && lhs != null && closest != null) {
  ////                Node method = AstUtilities.findLocalScope(node, path);
  ////
  ////                if (method != null) {
  ////                    JsTypeAnalyzer analyzer = new JsTypeAnalyzer(info, /*request.info.getParserResult(),*/ index, method, node, astOffset, lexOffset, doc, fileObject);
  ////                    type = analyzer.getType(node);
  ////                }
  //            }
  //
  //            if ((typeQName == null) && (lhs != null) && (closest != null) && call.isSimpleIdentifier()) {
  ////                Node method = AstUtilities.findLocalScope(node, path);
  ////
  ////                if (method != null) {
  ////                    // TODO - if the lhs is "foo.bar." I need to split this
  ////                    // up and do it a bit more cleverly
  ////                    JsTypeAnalyzer analyzer = new JsTypeAnalyzer(info, /*request.info.getParserResult(),*/ index, method, node, astOffset, lexOffset, doc, fileObject);
  ////                    type = analyzer.getType(lhs);
  ////                }
  //            }
  //
  //            // I'm not doing any data flow analysis at this point, so
  //            // I can't do anything with a LHS like "foo.". Only actual types.
  //            if (typeQName != null && typeQName.length() > 0) {
  //                if ("this".equals(lhs)) {
  //                    typeQName = fqn;
  //                    skipPrivate = false;
  ////                } else if ("super".equals(lhs)) {
  ////                    skipPrivate = false;
  ////
  ////                    IndexedClass sc = index.getSuperclass(fqn);
  ////
  ////                    if (sc != null) {
  ////                        type = sc.getFqn();
  ////                    } else {
  ////                        ClassNode cls = AstUtilities.findClass(path);
  ////
  ////                        if (cls != null) {
  ////                            type = AstUtilities.getSuperclass(cls);
  ////                        }
  ////                    }
  ////
  ////                    if (type == null) {
  ////                        type = "Object"; // NOI18N
  ////                    }
  //                }
  //
  //                if (typeQName != null && typeQName.length() > 0) {
  //                    // Possibly a class on the left hand side: try searching with the class as a qualifier.
  //                    // Try with the LHS + current FQN recursively. E.g. if we're in
  //                    // Test::Unit when there's a call to Foo.x, we'll try
  //                    // Test::Unit::Foo, and Test::Foo
  //                    while (elements.size() == 0 && fqn != null && !fqn.equals(typeQName)) {
  //                        elements = index.getMembers(prefix, fqn + "." + typeQName, kind, ScalaIndex.ALL_SCOPE, result, false);
  //
  //                        int f = fqn.lastIndexOf("::");
  //
  //                        if (f == -1) {
  //                            break;
  //                        } else {
  //                            fqn = fqn.substring(0, f);
  //                        }
  //                    }
  //
  //                    // Add methods in the class (without an FQN)
  //                    Set<GsfElement> m = index.getMembers(prefix, typeQName, kind, ScalaIndex.ALL_SCOPE, result, false);
  //
  //                    if (m.size() > 0) {
  //                        elements = m;
  //                    }
  //                }
  //            } else if (lhs != null && lhs.length() > 0) {
  //                // No type but an LHS - perhaps it's a type?
  //                Set<GsfElement> m = index.getMembers(prefix, lhs, kind, ScalaIndex.ALL_SCOPE, result, false);
  //
  //                if (m.size() > 0) {
  //                    elements = m;
  //                }
  //            }
  //
  //            // Try just the method call (e.g. across all classes). This is ignoring the
  //            // left hand side because we can't resolve it.
  //            if ((elements.size() == 0) && (prefix.length() > 0 || typeQName == null)) {
  ////                if (prefix.length() == 0) {
  ////                    proposals.clear();
  ////                    proposals.add(new KeywordItem("", "Type more characters to see matches", request));
  ////                    return true;
  ////                } else {
  //                //elements = index.getAllNames(prefix, kind, ScalaIndex.ALL_SCOPE, result);
  ////                }
  //            }
  //
  //            for (GsfElement gsfElement : elements) {
  //                Element element = gsfElement.getElement();
  //                // Skip constructors - you don't want to call
  //                //   x.Foo !
  //                if (element.getKind() == ElementKind.CONSTRUCTOR) {
  //                    continue;
  //                }
  //
  //                // Don't include private or protected methods on other objects
  //                if (skipPrivate && element.getModifiers().contains(Modifier.PRIVATE)) {
  //                    continue;
  //                }
  //
  //
  //
  ////                // We can only call static methods
  ////                if (skipInstanceMethods && !method.isStatic()) {
  ////                    continue;
  ////                }
  //
  ////                if (element.isNoDoc()) {
  ////                    continue;
  ////                }
  //
  //                if (element instanceof ExecutableElement) {
  //                    FunctionItem item = new FunctionItem(gsfElement, request);
  //                    proposals.add(item);
  //                } else if (element instanceof VariableElement) {
  //                    PlainItem item = new PlainItem(gsfElement, request);
  //                    proposals.add(item);
  //                }
  //            }
  //
  //            return done;
  //        }
  //
  //        return false;
  //    }

  override def getAutoQuery(component: JTextComponent, typedText: String): QueryType = {
    typedText.charAt(0) match {
      // TODO - auto query on ' and " when you're in $() or $F()
      case '\n' | '(' | '[' | '{' |';' => return QueryType.STOP
      case c if c != '.' => return QueryType.NONE
      case _ =>
    }

    val offset = component.getCaretPosition
    val doc = component.getDocument.asInstanceOf[BaseDocument]
    typedText match {
      case "." => // NOI18N
        // See if we're in Js context

        val ts = ScalaLexUtil.getTokenSequence(doc, offset).getOrElse(return QueryType.NONE)
        ts.move(offset)
        if (!ts.moveNext && !ts.movePrevious) {
          return QueryType.NONE
        }
        if (ts.offset == offset && !ts.movePrevious) {
          return QueryType.NONE
        }
        val token = ts.token
        val id = token.id

        //            // ".." is a range, not dot completion
        //            if (id == ScalaTokenId.RANGE) {
        //                return QueryType.NONE;
        //            }

        // TODO - handle embedded JavaScript
        id.primaryCategory match {
          case "comment" | "string" | "regexp" => return QueryType.NONE
          case _ => return QueryType.COMPLETION
        }
    }

    //        if (":".equals(typedText)) { // NOI18N
    //            // See if it was "::" and we're in ruby context
    //            int dot = component.getSelectionStart();
    //            try {
    //                if ((dot > 1 && component.getText(dot-2, 1).charAt(0) == ':') && // NOI18N
    //                        isJsContext(doc, dot-1)) {
    //                    return QueryType.COMPLETION;
    //                }
    //            } catch (BadLocationException ble) {
    //                Exceptions.printStackTrace(ble);
    //            }
    //        }
    //
    QueryType.NONE
  }

  override def document(pr: ParserResult, element: ElementHandle): String = {
    val sigFm = new SignatureHtmlFormatter

    val comment = element match {
      case x: AstElementHandle =>
        x.sigFormat(sigFm)
        x.getDocComment
      case _ => ""
    }
    
    val html = new StringBuilder
    element.getFileObject match {
      case null =>
      case fo => html.append("<b>").append(fo.getPath).append("</b><br>")
    }

    if (comment.length > 0) {
      val commentFm = new ScalaCommentFormatter(comment)
      element.getName match {
        case null =>
        case name => commentFm.setSeqName(name)
      }

      html.append(sigFm).append("\n<hr>\n").append(commentFm.toHtml)
    } else {
      html.append(sigFm).append("\n<hr>\n<i>").append(NbBundle.getMessage(classOf[ScalaCodeCompletionHandler], "NoCommentFound")).append("</i>")
    }

    html.toString
  }

  override def getApplicableTemplates(info: javax.swing.text.Document, selectionBegin: Int, selectionEnd: Int): java.util.Set[String] = {
    java.util.Collections.emptySet[String]
  }

  override def parameters(info: ParserResult, lexOffset: Int, proposal: CompletionProposal): ParameterInfo = {
    ParameterInfo.NONE
    /*_
     Function[] methodHolder = new Function[1];
     val paramIndexHolder = Array(1)
     val anchorOffsetHolder = Array(1)
     val astOffset = ScalaLexUtil.getAstOffset(info, lexOffset)
     if (!computeMethodCall(info, lexOffset, astOffset,
     methodHolder, paramIndexHolder, anchorOffsetHolder, null)) {

     return ParameterInfo.NONE
     }

     Function method = methodHolder[0];
     if (method == null) {
     return ParameterInfo.NONE;
     }
     val index = paramIndexHolder(0)
     val anchorOffset = anchorOffsetHolder(0)


     // TODO: Make sure the caret offset is inside the arguments portion
     // (parameter hints shouldn't work on the method call name itself
     // See if we can find the method corresponding to this call
     //        if (proposal != null) {
     //            Element node = proposal.getElement();
     //            if (node instanceof IndexedFunction) {
     //                method = ((IndexedFunction)node);
     //            }
     //        }
     val params = method.getParameters();
     val paramsInStr = if (!params.isEmpty()) {
     val paramsInStr = new ArrayList<String>();
     for (Var param : params) {
     paramsInStr.add(param.getSimpleName().toString());
     }
     paramsInStr
     } else {
     java.util.Collections.emptyList[String]
     }

     if (!paramsInStr.isEmpty) {
     new ParameterInfo(paramsInStr, index, anchorOffset);
     } else ParameterInfo.NONE
     */
  }
}

