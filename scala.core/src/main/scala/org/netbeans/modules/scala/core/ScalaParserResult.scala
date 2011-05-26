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
import javax.swing.text.BadLocationException
import org.netbeans.editor.BaseDocument
import org.netbeans.editor.Utilities
import org.netbeans.modules.csl.api.Error
import org.netbeans.modules.csl.spi.DefaultError
import org.netbeans.modules.csl.spi.ParserResult
import org.netbeans.modules.parsing.api.Snapshot
import org.netbeans.modules.scala.core.ast.ScalaRootScope
import scala.collection.mutable.WeakHashMap

/**
 *
 * @author Caoyuan Deng
 */
class ScalaParserResult(snapshot: Snapshot) extends ParserResult(snapshot) {
  private val log = Logger.getLogger(this.getClass.getName)

  if (ScalaParserResult.debug) {
    ScalaParserResult.unreleasedResults.put(this, snapshot.getSource.getFileObject.getPath)
    log.info("==== unreleased parser results: ")
    for ((k, v) <- ScalaParserResult.unreleasedResults) log.info(v)
  }

  private val fileObject = snapshot.getSource.getFileObject
  val srcFile = ScalaSourceFile.sourceFileOf(fileObject)
  val global = ScalaGlobal.getGlobal(fileObject)
  // when a new ScalaParserResult is created, we need to reset the content(via snapshot) and unit of srcFile
  srcFile.snapshot = snapshot
  reset

  @volatile private var isInSemantic = false
  @volatile private var _root: ScalaRootScope = ScalaRootScope.EMPTY
  private var _errors: java.util.List[Error] = _

  private def reset {
    global.resetUnitOf(srcFile)
    _root = ScalaRootScope.EMPTY
    _errors = null
  }

  private def isInvalid = _root eq ScalaRootScope.EMPTY
  
  /**
   * Utility method to get the raw end in doc, reserved here for reference
   */
  private def getRawEnd(doc: org.netbeans.editor.BaseDocument, offset: Int) = {
    val end = try {
      org.netbeans.editor.Utilities.getRowLastNonWhite(doc, offset) + 1 // * @Note row should plus 1 to equal NetBeans' doc offset
    } catch {
      case ex: javax.swing.text.BadLocationException => -1
    }

    if (end != -1 && end <= offset) {
      end + 1
    } else end
  }
  
  override protected def invalidate {
    // do nothing, so we can maintain the status of parsed result by ourselves
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
    if (_errors eq null) {
      _errors = global.reporter match {
        case x: ErrorReporter => x.errors match {
            case Nil => java.util.Collections.emptyList[Error]
            case scalaErrors =>
              val errs = new java.util.ArrayList[Error]
              for (ScalaError(pos, msg, severity, force) <- scalaErrors if pos.isDefined) {
                // It seems scalac's errors may contain those from other sources that are deep referred, try to filter them here
                if (srcFile.file eq pos.source.file) {
                  val offset = pos.startOrPoint
                  val end = pos.endOrPoint

                  val isLineError = (end == -1)
                  val error = DefaultError.createDefaultError("SYNTAX_ERROR", msg, msg, fileObject, offset, end, isLineError, severity)
                  //error.setParameters(Array(offset, msg))                

                  errs.add(error)
                } else {
                  //println("Not same SourceFile: " + pos.source.file)
                }

              }
              errs
          }
        case _ => java.util.Collections.emptyList[Error]
      }
    }
    _errors
  }
  
  def toTyped {
    global.askForType(srcFile, false)
  }
  
  def toSemanticed {
    // although the unit may have been ahead to typed phase during autocompletion, 
    // the typed trees may not be correct for semantic analysis, it's better to reset 
    // the unit to get the best error report.
    // An example is that when try completing on x. and then press esc, the error won't
    // be reported if do not call reset here 
    reset
    _root = global.askForSemantic(srcFile, false)
  }
  
  def rootScope: ScalaRootScope = {
    if (isInvalid) {
      isInSemantic = true
      toSemanticed
      isInSemantic = false
    }
    _root
  }

  def cancelSemantic: Boolean = {
    val willCancel = if (isInSemantic) {
      global.cancelSemantic(srcFile)
    } else false

    if (willCancel) reset
    
    isInSemantic = false
    willCancel
  }

  lazy val rootScopeForDebug: ScalaRootScope = {
    val global = ScalaGlobal.getGlobal(fileObject, true)
    global.compileSourceForDebug(srcFile)
  }
  
  override def toString = "ParserResult(file=" + this.fileObject.getNameExt + ")"
}

object ScalaParserResult {
  // ----- for debug
  private val debug = false
  private val unreleasedResults = new WeakHashMap[ScalaParserResult, String]
}

