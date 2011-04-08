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

import javax.lang.model.element.{ExecutableElement}
import javax.swing.text.{BadLocationException}
import org.netbeans.api.java.source.ClassIndex
import org.netbeans.api.java.source.ClassIndex.NameKind
import org.netbeans.api.lexer.{Token, TokenHierarchy, TokenId, TokenSequence}
import org.netbeans.editor.{BaseDocument, Utilities}
import org.netbeans.modules.csl.api.CodeCompletionHandler.QueryType
import org.netbeans.modules.csl.api.{CodeCompletionHandler, CompletionProposal, OffsetRange}
import org.netbeans.modules.csl.spi.{DefaultCompletionResult, ParserResult}
import org.netbeans.modules.parsing.spi.indexing.support.QuerySupport
import org.openide.filesystems.FileObject
import org.openide.util.{Exceptions}

import org.netbeans.api.language.util.ast.{AstItem}
import org.netbeans.modules.scala.core.ScalaParserResult
import org.netbeans.modules.scala.core.ScalaSourceUtil
import org.netbeans.modules.scala.core.ScalaSymbolResolver
import org.netbeans.modules.scala.core.lexer.{ScalaLexUtil, ScalaTokenId}
import org.netbeans.modules.scala.core.ScalaGlobal
import org.netbeans.modules.scala.core.ScalaParser.Sanitize
import org.netbeans.modules.scala.core.rats.ParserScala

import scala.concurrent.SyncVar
import scala.tools.nsc.symtab.Flags
import scala.tools.nsc.util.OffsetPosition

/**
 * @author Caoyuan Deng
 * 
 */
object ScalaCodeCompleter {
  // Dbl-space lines to keep formatter from collapsing pairs into a block
  private val REGEXP_WORDS = Array("\\0", "The NUL character (\\u0000)",
                                   "\\t", "Tab (\\u0009)",
                                   "\\n", "Newline (\\u000A)",
                                   "\\v", "Vertical tab (\\u000B)",
                                   "\\f", "Form feed (\\u000C)",
                                   "\\r", "Carriage return (\\u000D)",
                                   "\\x", "\\x<i>nn</i>: The latin character in hex <i>nn</i>",
                                   "\\u", "\\u<i>xxxx</i>: The Unicode character in hex <i>xxxx</i>",
                                   "\\c", "\\c<i>X</i>: The control character ^<i>X</i>",
                                   // Character classes
                                   "[]", "Any one character between the brackets",
                                   "[^]", "Any one character not between the brackets",
                                   "\\w", "Any ASCII word character; same as [0-9A-Za-z_]",
                                   "\\W", "Not a word character; same as [^0-9A-Za-z_]",
                                   "\\s", "Unicode space character",
                                   "\\S", "Non-space character",
                                   "\\d", "Digit character; same as [0-9]",
                                   "\\D", "Non-digit character; same as [^0-9]",
                                   "[\\b]", "Literal backspace",
                                   // Match positions
                                   "^", "Start of line",
                                   "$", "End of line",
                                   "\\b", "Word boundary (if not in a range specification)",
                                   "\\B", "Non-word boundary",
                                   // According to JavaScript The Definitive Guide, the following are not supported
                                   // in JavaScript:
                                   // \\a, \\e, \\l, \\u, \\L, \\U, \\E, \\Q, \\A, \\Z, \\z, and \\G
                                   //
                                   //"\\A", "Beginning of string",
                                   //"\\z", "End of string",
                                   //"\\Z", "End of string (except \\n)",

                                   "*", "Zero or more repetitions of the preceding",
                                   "+", "One or more repetitions of the preceding",
                                   "{m,n}", "At least m and at most n repetitions of the preceding",
                                   "?", "At most one repetition of the preceding; same as {0,1}",
                                   "|", "Either preceding or next expression may match",
                                   "()", "Grouping" //"[:alnum:]", "Alphanumeric character class",
                                   //"[:alpha:]", "Uppercase or lowercase letter",
                                   //"[:blank:]", "Blank and tab",
                                   //"[:cntrl:]", "Control characters (at least 0x00-0x1f,0x7f)",
                                   //"[:digit:]", "Digit",
                                   //"[:graph:]", "Printable character excluding space",
                                   //"[:lower:]", "Lowecase letter",
                                   //"[:print:]", "Any printable letter (including space)",
                                   //"[:punct:]", "Printable character excluding space and alphanumeric",
                                   //"[:space:]", "Whitespace (same as \\s)",
                                   //"[:upper:]", "Uppercase letter",
                                   //"[:xdigit:]", "Hex digit (0-9, a-f, A-F)",
  )    // Strings section 7.8

  private val STRING_ESCAPES = Array("\\0", "The NUL character (\\u0000)",
                                     "\\b", "Backspace (0x08)",
                                     "\\t", "Tab (\\u0009)",
                                     "\\n", "Newline (\\u000A)",
                                     "\\v", "Vertical tab (\\u000B)",
                                     "\\f", "Form feed (\\u000C)",
                                     "\\r", "Carriage return (\\u000D)",
                                     "\\\"", "Double Quote (\\u0022)",
                                     "\\'", "Single Quote (\\u0027)",
                                     "\\\\", "Backslash (\\u005C)",
                                     "\\x", "\\x<i>nn</i>: The latin character in hex <i>nn</i>",
                                     "\\u", "\\u<i>xxxx</i>: The Unicode character in hex <i>xxxx</i>",
                                     "\\", "\\<i>ooo</i>: The latin character in octal <i>ooo</i>",
                                     // PENDING: Is this supported?
                                     "\\c", "\\c<i>X</i>: The control character ^<i>X</i>"
  )


  private val scalaDocWords = Array("@augments",
                                    "@class",
                                    "@config",
                                    "@constructor",
                                    "@deprecated",
                                    "@description",
                                    "@event",
                                    "@example",
                                    "@exception",
                                    "@fileOverview",
                                    "@function",
                                    "@ignore",
                                    "@inherits",
                                    "@memberOf",
                                    "@name",
                                    "@namespace",
                                    "@param",
                                    "@param",
                                    "@private",
                                    "@property",
                                    "@return",
                                    "@scope",
                                    "@scope",
                                    "@static",
                                    "@type"
  )

  var callLineStart = -1
  var callMethod: ExecutableElement = _
}

class ScalaCodeCompleter(val global: ScalaGlobal) {
  import ScalaCodeCompleter._

  import global._

  private object completionProposals extends {
    val global = ScalaCodeCompleter.this.global
  } with ScalaCompletionProposals

  import completionProposals._

  private object resolver extends {
    val global = ScalaCodeCompleter.this.global
  } with ScalaSymbolResolver

  var caseSensitive: Boolean = _
  var completionResult: DefaultCompletionResult = _
  var th: TokenHierarchy[_] = _
  var anchor: Int = _
  var lexOffset: Int = _
  var astOffset: Int = _
  var doc: BaseDocument = _
  var prefix: String = _
  var kind: QuerySupport.Kind = _
  var pResult: ScalaParserResult = _
  var queryType: QueryType = _
  var fileObject: FileObject = _
  var fqn: String = _
  //var index: ScalaIndex = _


  case class Call(base: Token[TokenId], dot: Token[TokenId], select: Token[TokenId])

  private val CALL_IDs: Set[TokenId] = Set(ScalaTokenId.Identifier,
                                           ScalaTokenId.This,
                                           ScalaTokenId.Super,
                                           ScalaTokenId.Class,
                                           ScalaTokenId.Wild
  )

  private def isCallId(id: TokenId) = CALL_IDs.contains(id)

  def findCall(ts: TokenSequence[TokenId], th: TokenHierarchy[_]): Call = {
    var collector: List[Token[TokenId]] = Nil
    var break = false
    while (ts.movePrevious && !break) {
      val token = ts.token
      token.id match {
        case ScalaTokenId.Dot =>
          val dot = ts.offsetToken
          collector match {
            case x :: xs if ScalaLexUtil.isWsComment(x.id) =>
              // * replace previous sep token with this Dot
              collector = dot :: xs
            case _ => collector = dot :: collector
          }

        case ScalaTokenId.Nl =>
        case id if ScalaLexUtil.isWsComment(id) =>
          collector match {
            case x :: xs if ScalaLexUtil.isWsComment(x.id) | x.id == ScalaTokenId.Dot =>
              // * do not add more, combined all ws comment tokens
            case _ => collector = token :: collector
          }

        case id if isCallId(id) => collector = token :: collector

        case ScalaTokenId.RParen =>
          ScalaLexUtil.findPairBwd(ts, ScalaTokenId.LParen, ScalaTokenId.RParen)
        case ScalaTokenId.RBrace =>
          ScalaLexUtil.findPairBwd(ts, ScalaTokenId.LBrace, ScalaTokenId.RBrace)
        case ScalaTokenId.RBracket =>
          ScalaLexUtil.findPairBwd(ts, ScalaTokenId.LBracket, ScalaTokenId.RBracket)

        case _ => break = true
      }

      collector map {_.id} match {
        case List(a, b) if ScalaLexUtil.isWsComment(b) | b == ScalaTokenId.Dot => break = true
        case List(a, b, c) => break = true // collect no more than 3 tokens
        case _ =>
      }
    }

    val (base, dot, select) =
      collector match {
        case List(basex, sep, selectx) =>
          if (isCallId(basex.id) && isCallId(selectx.id)) {
            (basex, if (sep.id == ScalaTokenId.Dot) sep else null, selectx)
          } else (null, null, null)

        case List(basex, sep) =>
          if (isCallId(basex.id)) {
            (basex, if (sep.id == ScalaTokenId.Dot) sep else null, null)
          } else (null, null, null)

        case _ => (null, null, null)
      }

    Call(base, dot, select)
  }

  private def startsWith(theString: String, prefix: String): Boolean = {
    if (prefix.length == 0) {
      return true
    }

    if (caseSensitive) theString.startsWith(prefix)
    else theString.toLowerCase.startsWith(prefix.toLowerCase)
  }


  def completeKeywords(proposals: java.util.List[CompletionProposal]): Unit = {
    // No keywords possible in the RHS of a call (except for "this"?)
    //        if (request.call.getLhs() != null) {
    //            return;
    //        }
    val itr = ParserScala.SCALA_KEYWORDS.iterator
    while (itr.hasNext) {
      val keyword = itr.next
      if (startsWith(keyword, prefix)) {
        val item = KeywordProposal(keyword, null, this)
        proposals.add(item)
      }
    }
  }

  @throws(classOf[BadLocationException])
  def completeComments(proposals: java.util.List[CompletionProposal]): Boolean = {
    val rowStart = Utilities.getRowFirstNonWhite(doc, lexOffset)
    if (rowStart == -1) {
      return false
    }

    val line = doc.getText(rowStart, Utilities.getRowEnd(doc, lexOffset) - rowStart)
    val delta = lexOffset - rowStart

    var i = delta - 1
    var break = false
    while (i >= 0 && !break) {
      val c = line.charAt(i)
      if (Character.isWhitespace(c) || (!Character.isLetterOrDigit(c) && c != '@' && c != '.' && c != '_')) {
        break = true
      }
      i -= 1
    }
    i += 1
    prefix = line.substring(i, delta)
    anchor = rowStart + i

    // Regular expression matching.  {
    for (j <- 0 to scalaDocWords.length) {
      val word = scalaDocWords(j)
      if (startsWith(word, prefix)) {
        val item = KeywordProposal(word, null, this)
        proposals.add(item)
      }
    }

    true
  }

  /**
   * Determine if we're trying to complete the name for a "new" (in which case
   * we show available constructors.
   */
  def completeNew(proposals: java.util.List[CompletionProposal]): Boolean = {
    val ts = ScalaLexUtil.getTokenSequence(th, lexOffset).getOrElse(return true)
    ts.move(lexOffset)
    if (!ts.moveNext && !ts.movePrevious) {
      return true
    }

    if (true /* && index != null */) {

      if (ts.offset == lexOffset) {
        // We're looking at the offset to the RIGHT of the caret
        // position, which could be whitespace, e.g.
        //  "def fo| " <-- looking at the whitespace
        ts.movePrevious
      }

      var token = ts.token
      if (token != null) {
        var id = token.id

        // See if we're in the identifier - "foo" in "def foo"
        // I could also be a keyword in case the prefix happens to currently
        // match a keyword, such as "next"
        if (id == ScalaTokenId.Identifier || id == ScalaTokenId.CONSTANT || id.primaryCategory == "keyword") {
          if (!ts.movePrevious) {
            return false
          }

          token = ts.token
          id = token.id
        }

        // If we're not in the identifier we need to be in the whitespace after "def"
        if (id != ScalaTokenId.Ws && id != ScalaTokenId.Nl & id != ScalaTokenId.Colon) {
          // Do something about http://www.netbeans.org/issues/show_bug.cgi?id=100452 here
          // In addition to checking for whitespace I should look for "Foo." here
          return false
        }

        // There may be more than one whitespace; skip them
        if (id == ScalaTokenId.Ws || id == ScalaTokenId.Nl) {
          var break = false
          while (ts.movePrevious && !break) {
            token = ts.token
            if (token.id != ScalaTokenId.Ws) {
              break = true
            }
          }
        }

        if (token.id == ScalaTokenId.New || token.id == ScalaTokenId.Colon) {
          if (prefix.length < 1) {
            /** @todo return imported types */
            return true
          }

          /**
           * @Todo : we should implement completion for "new" in two phase:
           * 1. get Type name
           * 2. get constructors of this type when use pressed enter
           */
          val cpInfo = ScalaSourceUtil.getClasspathInfo(pResult.getSnapshot.getSource.getFileObject).getOrElse(return true)
          val tpElements = cpInfo.getClassIndex.getDeclaredTypes(prefix, NameKind.CASE_INSENSITIVE_PREFIX,
                                                                 java.util.EnumSet.allOf(classOf[ClassIndex.SearchScope]))

          val itr = tpElements.iterator
          while (itr.hasNext) {
            val tpElement = itr.next
            val qname = tpElement.getQualifiedName
            val sname = qname.lastIndexOf(".") match {
              case -1 => qname
              case i => qname.substring(i + 1, qname.length)
            }
            if (sname.startsWith(prefix)) {
              val jElement = JavaElement(tpElement)
              val proposal = TypeProposal(jElement, this)
              proposals.add(proposal)
            }
          }
          
          return true
        }
      }
    }

    false
  }

  /*
   @Deprecated
   def completeImport(proposals: java.util.List[CompletionProposal]): Boolean = {
   val fqnPrefix = prefix match {
   case null => return false
   case x => x
   }

   val cpInfo = ScalaSourceUtil.getClasspathInfo(fileObject).getOrElse(return false)

   val lastDot = fqnPrefix.lastIndexOf('.')
   val (fulledPath, lastPart) =  if (lastDot == -1) {
   (fqnPrefix, "")
   } else if (lastDot == fqnPrefix.length - 1) {
   (fqnPrefix.substring(0, lastDot), "")
   } else {
   (fqnPrefix.substring(0, lastDot), fqnPrefix.substring(lastDot + 1, fqnPrefix.length))
   }

   resolver.resolveQualifiedName("", fulledPath) match {
   case None => false
   case Some(x) =>
   prefix = lastPart
   completeSymbolMembers(x, proposals)
   }
   }
   */

  /** Compute the current method call at the given offset. Returns false if we're not in a method call.
   * The argument index is returned in parameterIndexHolder[0] and the method being
   * called in methodHolder[0].
   */
  protected def computeMethodCall(info: ParserResult, lexOffset: Int, astOffset: Int,
                                  methodHolder: Array[ExecutableElement],
                                  parameterIndexHolder: Array[Int],
                                  anchorOffsetHolder: Array[Int],
                                  alternativesHolder: Array[Set[Function]]): Boolean = {
    try {
      val pResult = info.asInstanceOf[ScalaParserResult]
      val root = pResult.rootScope

      var targetMethod: ExecutableElement = null
      var index = -1

      // Account for input sanitation
      // TODO - also back up over whitespace, and if I hit the method
      // I'm parameter number 0
      val originalAstOffset = astOffset

      // Adjust offset to the left
      val doc = info.getSnapshot.getSource.getDocument(true).asInstanceOf[BaseDocument]

      val th = info.getSnapshot.getTokenHierarchy
      val newLexOffset = ScalaLexUtil.findSpaceBegin(doc, lexOffset)
      var astOffset1 = if (newLexOffset < lexOffset) {
        astOffset - (lexOffset - newLexOffset)
      } else astOffset

      val range = pResult.sanitizedRange
      if (range != OffsetRange.NONE && range.containsInclusive(astOffset1)) {
        if (astOffset1 != range.getStart) {
          astOffset1 = range.getStart - 1
          if (astOffset1 < 0) {
            astOffset1 = 0
          }
        }
      }

      val ts = ScalaLexUtil.getTokenSequence(th, lexOffset).getOrElse(return false)
      ts.move(lexOffset)
      if (!ts.moveNext && !ts.movePrevious) {
        return false
      }

      var closestOpt = root.findItemsAt(th, astOffset1) match {
        case Nil => None
        case xs => Some(xs.reverse.head)
      }
      var closestOffset = astOffset1 - 1
      while (closestOpt == None && closestOffset > 0) {
        closestOffset -= 1
        closestOpt = root.findItemsAt(th, closestOffset) match {
          case Nil => None
          case xs => Some(xs.reverse.head)
        }
      }

      val call = closestOpt.getOrElse(null)

      val currentLineStart = Utilities.getRowStart(doc, lexOffset)
      if (callLineStart != -1 && currentLineStart == callLineStart) {
        // We know the method call
        targetMethod = callMethod
        if (targetMethod != null) {
          // Somehow figure out the argument index
          // Perhaps I can keep the node tree around and look in it
          // (This is all trying to deal with temporarily broken
          // or ambiguous calls.
        }
      }
      // Compute the argument index

      var anchorOffset = -1

      //            if (targetMethod != null) {
      //                Iterator<Node> it = path.leafToRoot();
      //                String name = targetMethod.getName();
      //                while (it.hasNext()) {
      //                    Node node = it.next();
      //                }
      //            }

      var haveSanitizedComma = (pResult.getSanitized == Sanitize.EDITED_DOT ||
                                pResult.getSanitized == Sanitize.ERROR_DOT)
      if (haveSanitizedComma) {
        // We only care about removed commas since that
        // affects the parameter count
        if (pResult.sanitizedContents.indexOf(',') == -1) {
          haveSanitizedComma = false
        }
      }

      if (call == null) {
        // Find the call in around the caret. Beware of
        // input sanitization which could have completely
        // removed the current parameter (e.g. with just
        // a comma, or something like ", @" or ", :")
        // where we accidentally end up in the previous
        // parameter.
        //                ListIterator<Node> it = path.leafToRoot();
        //             nodesearch:
        //                while (it.hasNext()) {
        //                    Node node = it.next();
        //
        //                    if (node.getType() == org.mozilla.javascript.Token.CALL) {
        //                        call = node;
        //                        index = AstUtilities.findArgumentIndex(call, astOffset, path);
        //                        break;
        //                    }
        //
        //                }
      }

      if (index != -1 && haveSanitizedComma && call != null) {
        //                if (call.nodeId == NodeTypes.FCALLNODE) {
        //                    an = ((FCallNode)call).getArgsNode();
        //                } else if (call.nodeId == NodeTypes.CALLNODE) {
        //                    an = ((CallNode)call).getArgsNode();
        //                }
        //                if (an != null && index < an.childNodes().size() &&
        //                        ((Node)an.childNodes().get(index)).nodeId == NodeTypes.HASHNODE) {
        //                    // We should stay within the hashnode, so counteract the
        //                    // index++ which follows this if-block
        //                    index--;
        //                }

        // Adjust the index to account for our removed
        // comma
        index += 1
      }

      if (call == null || index == -1) {
        callLineStart = -1
        callMethod = null
        return false
      } else if (targetMethod == null) {
        // Look up the
        // See if we can find the method corresponding to this call

        //targetMethod = new ScalaDeclarationFinder().findMethodDeclaration(info, call, alternativesHolder);
        if (targetMethod == null) {
          return false
        }
      }

      callLineStart = currentLineStart
      callMethod = targetMethod

      methodHolder(0) = callMethod
      parameterIndexHolder(0) = index

      if (anchorOffset == -1) {
        anchorOffset = call.idToken.offset(th) // TODO - compute

      }
      anchorOffsetHolder(0) = anchorOffset
    } catch {
      case ble: BadLocationException => Exceptions.printStackTrace(ble); return false
    }

    true
  }

  def completeLocals(proposals: java.util.List[CompletionProposal]): Unit = {
    val root = pResult.rootScope

    val pos = rangePos(pResult.srcFile, lexOffset, lexOffset, lexOffset)

    global.cancelSemantic(pos.source)
    val resp = new Response[List[Member]]
    try {
      global.askScopeCompletion(pos, true, resp)
      resp.get match {
        case Left(members) =>
          for (ScopeMember(sym, tpe, accessible, viaImport) <- members
               if accessible && startsWith(sym.nameString, prefix) && !sym.isConstructor
          ) {
            createSymbolProposal(sym) foreach {proposals add _}
          }
        case Right(thr) => ScalaGlobal.resetLate(global, thr)
      }
    } catch {case ex => ScalaGlobal.resetLate(global, ex)} // there is: scala.tools.nsc.FatalError: no context found for scala.tools.nsc.util.OffsetPosition@e302cef1
  }

  def completeSymbolMembers(baseToken: Token[TokenId], proposals: java.util.List[CompletionProposal]): Boolean = {
    try {
      val offset = baseToken.offset(th)
      val endOffset = offset + baseToken.length - 1
      val pos = rangePos(pResult.srcFile, offset, offset, endOffset)

      // * it seems CompleteHandle will always be called before other csl features (semantic, structure etc)
      // * that's good. But then, we may need to reload source first:
      global.cancelSemantic(pos.source)
      val resp = new Response[List[Member]]
      global.askTypeCompletion(pos, true, resp)
      resp.get match {
        case Left(members) =>
          for (TypeMember(sym, tpe, accessible, inherited, viaView) <- members
               if accessible && startsWith(sym.nameString, prefix) && !sym.isConstructor
          ) {
            createSymbolProposal(sym) foreach {proposal =>
              proposal.getElement.asInstanceOf[ScalaElement].isInherited = inherited
              proposal.getElement.asInstanceOf[ScalaElement].isImplicit = (viaView != NoSymbol)
              proposals.add(proposal)
            }
          }
        case Right(ex) => {ScalaGlobal.resetLate(global, ex)}
      }
    } catch {case ex => ScalaGlobal.resetLate(global, ex)}

    // always return true ?
    true
  }

  @deprecated("For reference only")
  def completeScopeImplicits(item: AstItem, proposals: java.util.List[CompletionProposal]): Boolean = {
    val sym = item.symbol.asInstanceOf[Symbol]

    // * use explict assigned `resultType` first
    val resType = item.resultType match {
      case null => getResultType(sym)
      case x => x.asInstanceOf[Type]
    }

    if (resType == null) {
      return false
    }

    val pos = new OffsetPosition(pResult.srcFile, item.idOffset(th))
    try {
      for (ScopeMember(sym, tpe, accessible, viaImport) <- global.scopeMembers(pos, true)
           if sym.hasFlag(Flags.IMPLICIT) && tpe.paramTypes.size == 1 && tpe.paramTypes.head == resType;
           member <- tpe.resultType.members 
           if accessible && startsWith(member.nameString, prefix) && !member.isConstructor
      ) {
        createSymbolProposal(member) foreach {proposal =>
          proposal.getElement.asInstanceOf[ScalaElement].isImplicit = true
          proposals.add(proposal)
        }
      }
      true
    } catch {case ex => ScalaGlobal.resetLate(global, ex); false}
  }

  private def createSymbolProposal(sym: Symbol): Option[CompletionProposal] = {
    if (sym.hasFlag(Flags.PRIVATE)) return None

    var element: ScalaElement = null
    var proposal: CompletionProposal = null
    if (sym.isMethod) {
      element = ScalaElement(sym, pResult)
      proposal = FunctionProposal(element, this)
    } else if (sym.isVariable) {
      element = ScalaElement(sym, pResult)
      proposal = PlainProposal(element, this)
    } else if (sym.isValue) {
      element = ScalaElement(sym, pResult)
      proposal = PlainProposal(element, this)
    } else if (sym.isClass || sym.isTrait || sym.isModule || sym.isPackage) {
      element = ScalaElement(sym, pResult)
      proposal = PlainProposal(element, this)
    }

    if (proposal != null) Some(proposal) else None
  }

  private def getResultType(sym: Symbol): Option[Type] = {
    try {
      sym.tpe match {
        case null | ErrorType | NoType => None
        case tpe => tpe.resultType match {
            case null => None
            case x => Some(x)
          }
      }
    } catch {case ex => ScalaGlobal.resetLate(global, ex); None}
  }

}
