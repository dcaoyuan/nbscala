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
import org.netbeans.modules.csl.api.Error
import org.netbeans.modules.csl.spi.DefaultError
import org.netbeans.modules.csl.spi.ParserResult
import org.netbeans.modules.parsing.api.Snapshot
import org.netbeans.modules.scala.core.ast.ScalaRootScope
import scala.collection.mutable.WeakHashMap
import scala.tools.nsc.reporters.Reporter

/**
 *
 * @author Caoyuan Deng
 */
class ScalaParserResult private (snapshot: Snapshot) extends ParserResult(snapshot) {
  private val fo = snapshot.getSource.getFileObject
  val global = ScalaGlobal.getGlobal(fo)
  val srcFile = {
    val x = ScalaSourceFile.sourceFileOf(fo)
    x.snapshot = snapshot
    x
  }

  @volatile private var _root: Option[ScalaRootScope] = None
  @volatile private var _errors: java.util.List[Error] = java.util.Collections.emptyList[Error]

  /**
   * @see http://forums.netbeans.org/topic43738.html
   * The Result.invalidate method does not (AFAIK) mean that the outcome of 
   * the parsing process is no longer valid. Result.invalidate is called 
   * after invocation of each Task. The intention is to use this to ensure 
   * that the Tasks are not keeping the results somewhere, which might lead 
   * to memory leaks. E.g. Java creates a fresh new Result on each invocation 
   * of getResult, wrapping the actual internal parsing data, which are kept 
   * across invocations of getResult, and then may validate that all access 
   * to the parsing data is done only from inside appropriate Tasks. It is 
   * AFAIK safe to ignore the invalidate method if you do not need to check 
   * that the tasks are behaving correctly. 
   */
  override 
  protected def invalidate {
    // do nothing
  }

  /** 
   * @TODO since only call rootScope will cause actual parsing, those parsing task
   * that won't get rootScope will not be truly parsed, I choose this approach because
   * the TaskListIndexer will re-parse all dependent source files, with bad scala
   * compiler performance right now, it's better to bypass it.
   *
   * When background scanning project truly do no to block the code-completion and
   * other editor behavior, or, the performance of complier is not the bottleneck
   * any more, I'll add it back.
   */
  override 
  def getDiagnostics: java.util.List[_ <: Error] = _errors

  /**
   * If toSemanticed procedure is cancelled, a new ScalaParserResult will be created, 
   * so it's safe to use 'val' instead of 'def' here.
   * 
   * In the theory, if a cancel request called, the parse should reparse later, but 
   * that's not true for current parsing api (bug?). Anyway, when root is ScalaRootScope.EMPTY,
   * we may try to re-parse always for my best (XXX need more thinking).
   */
  lazy val rootScope: ScalaRootScope = _root synchronized {
    _root match {
      case None => toSemanticed
        //case Some(ScalaRootScope.EMPTY) => toSemanticed
      case _ =>
    }
    
    // for some race conditions during cancel, the root may still be None,
    // so at lease return a ScalaRootScope.EMPTY
    _root getOrElse ScalaRootScope.EMPTY
  }

  private def toSemanticed() {
    _root = global.askForSemantic(srcFile)
    _errors = collectErrors(global.reporter)
  }
  
  /**
   * @Note _root synchronized call here will cause whole things blocked, why? 
   *       The cause may happen when cancelSemantic and toSemanticed both synchronized 
   *       on _root
   *       
   * Actually, we need not to care about the root that is fetched during cancelSemantic,
   * since NetBeans' parsing system should try to get a new one later.
   * 
   * Some other concerns: 
   * Although the unit may have been ahead to typed phase during autocompletion, 
   * the typed trees may not be correct for semantic analysis, it's better to reset 
   * the unit to get the best error report.
   * An example is that when try completing on x. and then press esc, the error won't
   * be reported if do not call reset here 
   */
  private[core] def tryCancelSemantic() {
    // only reset when global confirmed that the target srcFile is exactly the working one
    if (global.tryCancelSemantic(srcFile)) {
      // reset the _root and _errors
      _root = None
      _errors = java.util.Collections.emptyList[Error]
    }
  }

  private def collectErrors(reporter: Reporter) = {
    reporter match {
      case ErrorReporter(Nil) => java.util.Collections.emptyList[Error]
      case ErrorReporter(scalaErrors) =>
        val errs = new java.util.ArrayList[Error]
        for (ScalaError(pos, msg, severity, force) <- scalaErrors if pos.isDefined) {
          // It seems scalac's errors may contain those from other sources that are deep referred, try to filter them here
          if (srcFile.file eq pos.source.file) {
            val offset = pos.startOrPoint
            val end = pos.endOrPoint

            val isLineError = (end == -1)
            val error = DefaultError.createDefaultError("SYNTAX_ERROR", msg, msg, fo, offset, end, isLineError, severity)
            //error.setParameters(Array(offset, msg))                

            errs.add(error)
          } else {
            //println("Not same SourceFile: " + pos.source.file)
          }
        }
        errs
      case _ => java.util.Collections.emptyList[Error]
    }
  }

  lazy val rootScopeForDebug: ScalaRootScope = {
    ScalaGlobal.getGlobal(fo, true).compileSourceForDebug(srcFile)
  }
  
  override 
  def toString = "ParserResult of " + fo.getNameExt + ", root is " + (_root match {
      case None => "None"
      case Some(ScalaRootScope.EMPTY) => "Empty"
      case _ => "Ok"
    }
  )
}

object ScalaParserResult {
  private val log = Logger.getLogger(this.getClass.getName)

  // ----- for debug
  private val debug = false
  private val unreleasedResults = new WeakHashMap[ScalaParserResult, String]
  
  def apply(snapshot: Snapshot) = {
    val pr = new ScalaParserResult(snapshot)
    if (debug) {
      unreleasedResults.put(pr, snapshot.getSource.getFileObject.getPath)
      log.info("==== unreleased parser results: ")
      for ((k, v) <- ScalaParserResult.unreleasedResults) log.info(k.toString)
    }
    pr
  }
}
