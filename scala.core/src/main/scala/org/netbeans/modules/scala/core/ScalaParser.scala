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
package org.netbeans.modules.scala.core

import java.util.logging.Logger
import javax.swing.event.ChangeListener
import javax.swing.text.BadLocationException
import org.netbeans.api.java.classpath.ClassPath
import org.netbeans.editor.{BaseDocument, Utilities}
import org.netbeans.modules.csl.api.{Error, OffsetRange, Severity}
import org.netbeans.modules.csl.spi.{DefaultError, GsfUtilities}
import org.netbeans.modules.parsing.api.{Snapshot, Task}
import org.netbeans.modules.parsing.impl.indexing.TimeStamps
import org.netbeans.modules.parsing.spi.{ParseException, Parser, ParserFactory, SourceModificationEvent}
import org.netbeans.modules.scala.core.ast.{ScalaRootScope}
import org.netbeans.modules.scala.core.lexer.{ScalaLexUtil, ScalaTokenId}
import org.openide.filesystems.{FileObject, FileStateInvalidException, FileUtil}
import org.openide.util.Exceptions

import scala.collection.mutable.ArrayBuffer
import scala.tools.nsc.io.{PlainFile, VirtualFile}
import scala.tools.nsc.reporters.Reporter
import scala.tools.nsc.util.{Position, SourceFile, BatchSourceFile}

/**
 * 
 * @author Caoyuan Deng
 * @author Tor Norbye
 */
class ScalaParser extends Parser {
  import ScalaParser._

  private val logger = Logger.getLogger(this.getClass.getName)

  private var lastResult: ScalaParserResult = _

  @throws(classOf[ParseException])
  override def getResult(task: Task): Parser.Result = {
    assert(lastResult != null, "getResult() called prior parse()") //NOI18N
    lastResult
  }

  override def cancel {
    if (lastResult != null && !lastResult.loaded) {
      lastResult.global.cancelSemantic(lastResult.srcFile)
      lastResult.loaded = false
    }
  }

  @throws(classOf[ParseException])
  override def parse(snapshot: Snapshot, task: Task, event: SourceModificationEvent): Unit = {
    logger.info("Ready to parse " + snapshot.getSource.getFileObject.getNameExt)
    // * We'll lazily doing true parsing in ScalaParserResult
    lastResult = new ScalaParserResult(snapshot, this)
    //val context = new Context(snapshot, event)
    //lastResult = parseBuffer(context, Sanitize.NONE)
  }

  private def isIndexUpToDate(fo: FileObject): Boolean = {
    val srcCp = ClassPath.getClassPath(fo, ClassPath.SOURCE)
    if (srcCp != null) {
      srcCp.getRoots find {x => FileUtil.isParentOf(x, fo)} foreach {root =>
        val timeStamps = TimeStamps.forRoot(root.getURL, false)
        return timeStamps != null && timeStamps.checkAndStoreTimestamp(fo, FileUtil.getRelativePath(root, fo))
      }
    }
    
    return false
  }

  override def addChangeListener(changeListener: ChangeListener): Unit = {
    // no-op, we don't support state changes
  }

  override def removeChangeListener(changeListener: ChangeListener): Unit = {
    // no-op, we don't support state changes
  }

  private final class Factory extends ParserFactory {
    override def createParser(snapshots: java.util.Collection[Snapshot]): Parser = new ScalaParser
  }

  /**
   * Try cleaning up the source buffer around the current offset to increase
   * likelihood of parse success. Initially this method had a lot of
   * logic to determine whether a parse was likely to fail (e.g. invoking
   * the isEndMissing method from bracket completion etc.).
   * However, I am now trying a parse with the real source first, and then
   * only if that fails do I try parsing with sanitized source. Therefore,
   * this method has to be less conservative in ripping out code since it
   * will only be used when the regular source is failing.
   *
   * @todo Automatically close current statement by inserting ";"
   * @todo Handle sanitizing "new ^" from parse errors
   * @todo Replace "end" insertion fix with "}" insertion
   */
  private def sanitizeSource(context: Context, sanitizing: Sanitize): Boolean = {

    if (sanitizing == Sanitize.MISSING_END) {
      context.sanitizedSource = context.source + "end"
      val start = context.source.length
      context.sanitizedRange = new OffsetRange(start, start + 4)
      context.sanitizedContents = ""
      return true
    }

    var offset = context.caretOffset

    // Let caretOffset represent the offset of the portion of the buffer we'll be operating on
    if ((sanitizing == Sanitize.ERROR_DOT) || (sanitizing == Sanitize.ERROR_LINE)) {
      offset = context.errorOffset
    }

    // Don't attempt cleaning up the source if we don't have the buffer position we need
    if (offset == -1) {
      return false
    }

    // The user might be editing around the given caretOffset.
    // See if it looks modified
    // Insert an end statement? Insert a } marker?
    val source = context.source
    if (offset > source.length) {
      return false
    }

    try {
      // Sometimes the offset shows up on the next line
      if (ScalaSourceUtil.isRowEmpty(source, offset) || ScalaSourceUtil.isRowWhite(source, offset)) {
        offset = ScalaSourceUtil.getRowStart(source, offset) - 1
        if (offset < 0) {
          offset = 0
        }
      }

      if (!(ScalaSourceUtil.isRowEmpty(source, offset) || ScalaSourceUtil.isRowWhite(source, offset))) {
        if ((sanitizing == Sanitize.EDITED_LINE) || (sanitizing == Sanitize.ERROR_LINE)) {
          // See if I should try to remove the current line, since it has text on it.
          val lineEnd = ScalaSourceUtil.getRowLastNonWhite(source, offset)

          if (lineEnd != -1) {
            val sb = new StringBuilder(source.length)
            val lineStart = ScalaSourceUtil.getRowStart(source, offset)
            val rest = lineStart + 1

            sb.append(source.substring(0, lineStart))
            sb.append('#')

            if (rest < source.length) {
              sb.append(source.substring(rest, source.length))
            }
            assert(sb.length == source.length)

            context.sanitizedRange = new OffsetRange(lineStart, lineEnd)
            context.sanitizedSource = sb.toString
            context.sanitizedContents = source.substring(lineStart, lineEnd)
            return true
          }
        } else {
          assert(sanitizing == Sanitize.ERROR_DOT || sanitizing == Sanitize.EDITED_DOT)
          // Try nuking dots/colons from this line
          // See if I should try to remove the current line, since it has text on it.
          val lineStart = ScalaSourceUtil.getRowStart(source, offset)
          var lineEnd = offset - 1
          var break = false
          while (lineEnd >= lineStart && lineEnd < source.length && !break) {
            if (!Character.isWhitespace(source.charAt(lineEnd))) {
              break = true
            } else {
              lineEnd -= 1
            }
          }
          
          if (lineEnd > lineStart) {
            val sb = new StringBuilder(source.length)
            val line = source.substring(lineStart, lineEnd + 1)
            var removeChars = 0
            var removeEnd = lineEnd + 1

            if (line.endsWith(".") || line.endsWith("(")) { // NOI18N
              removeChars = 1
            } else if (line.endsWith(",")) { // NOI18N
              removeChars = 1
            } else if (line.endsWith(", ")) { // NOI18N
              removeChars = 2
            } else if (line.endsWith(",)")) { // NOI18N
              // Handle lone comma in parameter list - e.g.
              // type "foo(a," -> you end up with "foo(a,|)" which doesn't parse - but
              // the line ends with ")", not "," !
              // Just remove the comma
              removeChars = 1
              removeEnd -= 1
            } else if (line.endsWith(", )")) { // NOI18N
              // Just remove the comma

              removeChars = 1
              removeEnd -= 2
            } else {
              // Make sure the line doesn't end with one of the JavaScript keywords
              // (new, do, etc) - we can't handle that!
              /*_
               for (keyword <- LexerScala.SCALA_KEYWORDS) { // reserved words are okay
               if (line.endsWith(keyword)) {
               removeChars = 1;
               break;
               }
               }
               */
            }

            if (removeChars == 0) {
              return false
            }

            val removeStart = removeEnd - removeChars

            sb.append(source.substring(0, removeStart))

            for (i <- 0 until removeChars) {
              sb.append(' ')
            }

            if (removeEnd < source.length) {
              sb.append(source.substring(removeEnd, source.length))
            }
            assert(sb.length == source.length)

            context.sanitizedRange = new OffsetRange(removeStart, removeEnd)
            context.sanitizedSource = sb.toString
            context.sanitizedContents = source.substring(removeStart, removeEnd)
            return true
          }
        }
      }
    } catch {case ble: BadLocationException => Exceptions.printStackTrace(ble)}

    false
  }

  @deprecated("Will lazily parse in ScalaPaserResult")
  private def sanitize(context: Context, sanitizing: Sanitize): ScalaParserResult = {

    sanitizing match {
      case Sanitize.NEVER =>
        return createParserResult(context)

      case Sanitize.NONE =>

        // We've currently tried with no sanitization: try first level
        // of sanitization - removing dots/colons at the edited offset.
        // First try removing the dots or double colons around the failing position
        if (context.caretOffset != -1) {
          return parseBuffer(context, Sanitize.EDITED_DOT)
        }

        // Fall through to try the next trick
      case Sanitize.EDITED_DOT =>

        // We've tried editing the caret location - now try editing the error location
        // (Don't bother doing this if errorOffset==caretOffset since that would try the same
        // source as EDITED_DOT which has no better chance of succeeding...)
        if (context.errorOffset != -1 && context.errorOffset != context.caretOffset) {
          return parseBuffer(context, Sanitize.ERROR_DOT)
        }

        // Fall through to try the next trick
      case Sanitize.ERROR_DOT =>

        // We've tried removing dots - now try removing the whole line at the error position
        if (context.errorOffset != -1) {
          return parseBuffer(context, Sanitize.ERROR_LINE)
        }

        // Fall through to try the next trick
      case Sanitize.ERROR_LINE =>

        // Messing with the error line didn't work - we could try "around" the error line
        // but I'm not attempting that now.
        // Finally try removing the whole line around the user editing position
        // (which could be far from where the error is showing up - but if you're typing
        // say a new "def" statement in a class, this will show up as an error on a mismatched
        // "end" statement rather than here
        if (context.caretOffset != -1) {
          return parseBuffer(context, Sanitize.EDITED_LINE)
        }

        // Fall through to try the next trick
      case Sanitize.EDITED_LINE =>
        return parseBuffer(context, Sanitize.MISSING_END)

        // Fall through for default handling
      case Sanitize.MISSING_END =>
      case _ =>
        // We're out of tricks - just return the failed parse result
        return createParserResult(context)
    }
    
    createParserResult(context)
  }

  @deprecated("Will lazily parse in ScalaPaserResult")
  protected def parseBuffer(context: Context, sanitizing: Sanitize): ScalaParserResult = {
    var sanitizedSource = false
    var source = context.source

    sanitizing match {
      case Sanitize.NONE | Sanitize.NEVER =>
      case _ =>
        val ok = sanitizeSource(context, sanitizing)
        if (ok) {
          assert(context.sanitizedSource != null)
          sanitizedSource = true
          source = context.sanitizedSource
        } else {
          // Try next trick
          return sanitize(context, sanitizing)
        }
    }

    if (sanitizing == Sanitize.NONE) {
      context.errorOffset = -1
    }

    val doc = context.snapshot.getSource.getDocument(true).asInstanceOf[BaseDocument]

    val th = context.snapshot.getTokenHierarchy

    val ignoreErrors = sanitizedSource

    val file = if (context.fileObject != null) FileUtil.toFile(context.fileObject) else null
    val af = if (file != null)  new PlainFile(file) else new VirtualFile("<current>", "")
    val srcFile = new BatchSourceFile(af, source.toCharArray)
    // We should use absolutionPath here for real file, otherwise, symbol.sourcefile.path won't be abs path
    //String filePath = file != null ? file.getAbsolutePath():  "<current>";
    context.srcFile = srcFile
    
    val global = ScalaGlobal.getGlobal(context.fileObject)
    global.reporter = new ErrorReporter(context, doc, sanitizing)

    context.global = global

    var root: Option[ScalaRootScope] = None
    try {
      root = Some(global.askForSemantic(srcFile, true, th))
      //rootScope = Some(global.compileSourceForPresentation(srcFile, th))
    } catch {
      case ex: AssertionError =>
        // avoid scala nsc's assert error
        ScalaGlobal.resetLate(global, ex)
      case ex: java.lang.Error =>
        // avoid scala nsc's exceptions
        ex.printStackTrace
      case ex: IllegalArgumentException =>
        // An internal exception thrown by ParserScala, just catch it and notify
        notifyError(context, "SYNTAX_ERROR", ex.getMessage, 0, 0, true, sanitizing, Severity.ERROR, Array(ex))
      case ex: Exception =>
        // Scala's global throws too many exceptions
        ex.printStackTrace
    }

    if (root.isDefined) {
      context.rootScope = root
      context.sanitized = sanitizing
      val pResult = createParserResult(context)
      pResult.setSanitized(context.sanitized, context.sanitizedRange, context.sanitizedContents)
      pResult
    } else {
      // Don't try sanitizing:
      //return sanitize(context, sanitizing);
      val pResult = createParserResult(context)
      pResult.setSanitized(context.sanitized, context.sanitizedRange, context.sanitizedContents)
      pResult
    }
  }

  private def createParserResult(context: Context): ScalaParserResult = {
    if (!context.errors.isEmpty) {
      val fo = context.fileObject
      if (fo != null) {
        try {
          val inError = java.util.Collections.singleton(fo.getURL)
          //                        ErrorAnnotator eAnnot = ErrorAnnotator.getAnnotator();
          //                        if (eAnnot != null) {
          //                            eAnnot.updateInError(inError);
          //                        }
          inError
        } catch {case ex:FileStateInvalidException => Exceptions.printStackTrace(ex)}
      }
    }

    new ScalaParserResult(context.snapshot, this)
  }

  private def processObjectSymbolError(context: Context, root: ScalaRootScope): Sanitize = {
    val th = context.snapshot.getTokenHierarchy
    if (th == null) {
      return Sanitize.NONE
    }

    for (error <- context.errors) {
      val msg = error.getDescription
      if (msg.startsWith("identifier expected but")) {
        val start = error.getStartPosition

        val ts = ScalaLexUtil.getTokenSequence(th, start - 1).getOrElse(return Sanitize.NONE)
        ts.move(start - 1)
        if (!ts.moveNext && !ts.movePrevious) {
        } else {
          ScalaLexUtil.findPreviousNoWsNoComment(ts) match {
            case Some(tokenx) if tokenx.id == ScalaTokenId.Dot && context.caretOffset == tokenx.offset(th) + 1 && ts.movePrevious =>
              ScalaLexUtil.findPreviousNoWsNoComment(ts) match {
                case Some(tokeny) if tokeny.id == ScalaTokenId.Identifier =>
                  return Sanitize.EDITED_DOT
                case _ =>
              }
            case _ =>
          }
        }
      }
    }

    Sanitize.NONE
  }

  @deprecated("Will lazily parse in ScalaPaserResult")
  protected def notifyError(context: Context, key: String, msg: String,
                            start: Int, end: Int, isLineError: Boolean,
                            sanitizing: Sanitize, severity: Severity,
                            params: Object): Unit = {

    val error = DefaultError.createDefaultError(key, msg, msg, context.fileObject,
                                                start, end, isLineError, severity).asInstanceOf[DefaultError]
    params match {
      case null =>
      case x: Array[Object] => error.setParameters(x)
      case _ => error.setParameters(Array(params))
    }

    context.addError(error)

    if (sanitizing == Sanitize.NONE) {
      context.errorOffset = end
    }
  }

  /** Parsing context */
  @deprecated("Will lazily parse in ScalaPaserResult")
  class Context(val snapshot: Snapshot, event: SourceModificationEvent) {

    val fileObject: FileObject = snapshot.getSource.getFileObject
    lazy val source = snapshot.getText match {
      case x: String => x
      case x => x.toString
    }
    var errorOffset: Int = _
    var caretOffset: Int = GsfUtilities.getLastKnownCaretOffset(snapshot, event)
    var sanitizedSource: String = _
    var sanitizedRange: OffsetRange = OffsetRange.NONE
    var sanitizedContents: String = _
    var sanitized: Sanitize = Sanitize.NONE
    val errors = new ArrayBuffer[Error]
    var rootScope: Option[ScalaRootScope] = None
    var global: ScalaGlobal = _
    var srcFile: SourceFile = _

    def addError(error: Error): Unit = {
      errors += error
    }

    def cleanErrors = errors.clear

    override def toString = {
      "ScalaParser.Context(" + fileObject.toString + ")" // NOI18N
    }
  }

  @deprecated("Will lazily parse in ScalaPaserResult")
  private class ErrorReporter(context: Context, doc: BaseDocument, sanitizing: Sanitize) extends Reporter {

    override def info0(pos: Position, msg: String, severity: Severity, force: Boolean) {
      val ignoreError = context.sanitizedSource != null
      if (!ignoreError) {
        if (pos.isDefined) {
          val offset = pos.startOrPoint

          // * It seems scalac's errors may contain those from other source files that are deep referred, try to filter them here
          if (context.srcFile ne pos.source) {
            return
          }
          
          val sev = severity.id match {
            case 0 => return
            case 1 => org.netbeans.modules.csl.api.Severity.WARNING
            case 2 => org.netbeans.modules.csl.api.Severity.ERROR
            case _ => return
          }

          var end = try {
            // * @Note row should plus 1 to equal NetBeans' doc offset
            Utilities.getRowLastNonWhite(doc, offset) + 1
          } catch {case ex: BadLocationException => -1}

          if (end != -1 && end <= offset) {
            end += 1
          }

          val isLineError = (end == -1)
          notifyError(context, "SYNTAX_ERROR", msg, offset, end, isLineError, sanitizing, sev, Array(offset, msg))
        }
      }
    }
  }
}

object ScalaParser {

  private var version: Long = _
  private val profile = Array(0.0f, 0.0f)

  /** Attempts to sanitize the input buffer */
  abstract class Sanitize
  object Sanitize {
    /** Only parse the current file accurately, don't try heuristics */
    case object NEVER extends Sanitize
    /** Perform no sanitization */
    case object NONE extends Sanitize
    /** Try to remove the trailing . at the caret line */
    case object EDITED_DOT extends Sanitize
    /** Try to remove the trailing . at the error position, or the prior
     * line, or the caret line */
    case object ERROR_DOT extends Sanitize
    /** Try to cut out the error line */
    case object ERROR_LINE extends Sanitize
    /** Try to cut out the current edited line, if known */
    case object EDITED_LINE extends Sanitize
    /** Attempt to add an "end" to the end of the buffer to make it compile */
    case object MISSING_END extends Sanitize
  }
}
