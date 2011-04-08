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

import java.io.File
import javax.swing.text.BadLocationException
import org.netbeans.editor.BaseDocument
import org.netbeans.editor.Utilities
import org.netbeans.modules.csl.api.{Error, OffsetRange}
import org.netbeans.modules.csl.spi.DefaultError
import org.netbeans.modules.csl.spi.ParserResult
import org.netbeans.modules.parsing.api.Snapshot
import org.netbeans.modules.scala.core.ast.ScalaRootScope
import org.openide.filesystems.{FileUtil}
import scala.collection.mutable.WeakHashMap
import scala.tools.nsc.io.{PlainFile, VirtualFile}
import scala.tools.nsc.reporters.Reporter
import scala.tools.nsc.util.{BatchSourceFile, Position, SourceFile}

/**
 *
 * @author Caoyuan Deng
 */
class ScalaParserResult(snapshot: Snapshot, parser: ScalaParser) extends ParserResult(snapshot) {

  if (ScalaParserResult.debug) {
    ScalaParserResult.unreleasedResults.put(this, snapshot.getSource.getFileObject.getPath)
    println("==== unreleased parser results: ")
    for ((k, v) <- ScalaParserResult.unreleasedResults) println(v)
  }

  private var errors: java.util.List[Error] = null

  var sanitizedRange = OffsetRange.NONE
  /**
   * Return whether the source code for the parse result was "cleaned"
   * or "sanitized" (modified to reduce chance of parser errors) or not.
   * This method returns OffsetRange.NONE if the source was not sanitized,
   * otherwise returns the actual sanitized range.
   */
  var sanitizedContents: String = _
  var commentsAdded: Boolean = _
  private var sanitized: ScalaParser.Sanitize = _

  private var _root: ScalaRootScope = _
  @volatile var loaded = false


  override protected def invalidate: Unit = {
    // XXX: what exactly should we do here?
  }

  /** @todo since only call rootScope will cause actual parsing, those parsing task
   * that won't get rootScope will not be truly parsed, I choose this approach because
   * the TaskListIndexer will re-parse all dependent source files, with bad scala
   * comiler performance right now, it's better to bypass it.
   *
   * When background scanning project truly no to block the code-completion and
   * other editor behavior, or, the performance of complier is not the bottleneck
   * any more, I'll add it back.
   */
  override def getDiagnostics: java.util.List[_ <: Error] = {
    if (errors == null) {
      java.util.Collections.emptyList[Error]
    } else errors
  }

  lazy val srcFile: SourceFile = {
    val fo = snapshot.getSource.getFileObject
    val file: File = if (fo != null) FileUtil.toFile(fo) else null
    val af = if (file != null) new PlainFile(file) else new VirtualFile("<current>", "")
    new BatchSourceFile(af, snapshot.getText.toString.toCharArray)
  }

  lazy val global: ScalaGlobal = {
    ScalaGlobal.getGlobal(snapshot.getSource.getFileObject)
  }

  def rootScope: ScalaRootScope = {
    if (loaded) return _root

    val th = snapshot.getTokenHierarchy
    val doc = snapshot.getSource.getDocument(true).asInstanceOf[BaseDocument]

    global.reporter = new ErrorReporter(doc)
    // @Note it's more safe to force reload here, since a partial typed tree may cause unpredicted error :
    _root = global.askForSemantic(srcFile, true, th)
    loaded = true
    _root
  }

  lazy val rootScopeForDebug: ScalaRootScope = {
    val fo = getSnapshot.getSource.getFileObject
    val file: File = if (fo != null) FileUtil.toFile(fo) else null
    // We should use absolutionPath here for real file, otherwise, symbol.sourcefile.path won't be abs path
    //val filePath = if (file != null) file.getAbsolutePath):  "<current>";
    val th = getSnapshot.getTokenHierarchy

    val global = ScalaGlobal.getGlobal(fo, true)

    val af = if (file != null) new PlainFile(file) else new VirtualFile("<current>", "")
    val srcFile = new BatchSourceFile(af, getSnapshot.getText.toString.toCharArray)
    //rootScopeForDebug = Some(global.askForDebug(srcFile, th))
    global.compileSourceForDebug(srcFile, th)
  }

  /**
   * Set the range of source that was sanitized, if any.
   */
  def setSanitized(sanitized: ScalaParser.Sanitize, sanitizedRange: OffsetRange, sanitizedContents: String): Unit = {
    this.sanitized = sanitized
    this.sanitizedRange = sanitizedRange
    this.sanitizedContents = sanitizedContents
  }

  def getSanitized: ScalaParser.Sanitize = {
    sanitized
  }

  override def toString = {
    "ParserResult(file=" + snapshot.getSource.getFileObject
  }

  private class ErrorReporter(doc: BaseDocument) extends Reporter {

    override def info0(pos: Position, msg: String, severity: Severity, force: Boolean) {
      if (pos.isDefined) {
        val offset = pos.startOrPoint

        // * It seems scalac's errors may contain those from other source files that are deep referred, try to filter them here
        if (srcFile ne pos.source) {
          //println("Error from another source: " + msg + " (" + pos.source + " "  + offset + ")")
          return
        }

        val fo = snapshot.getSource.getFileObject

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
        val error = DefaultError.createDefaultError("SYNTAX_ERROR", msg, msg, fo, offset, end, isLineError, sev)
        //println(msg + " (" + offset + ")")

        if (errors == null) {
          errors = new java.util.ArrayList
        }
        errors.add(error)
        //error.setParameters(Array(offset, msg))
      }
    }
  }
}

object ScalaParserResult {
  // ----- for debug
  private val debug = false
  private val unreleasedResults = new WeakHashMap[ScalaParserResult, String]
}

