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
package org.netbeans.modules.scala.refactoring

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.java.source.ClasspathInfo
import org.netbeans.api.java.source.WorkingCopy
import org.netbeans.modules.parsing.api.UserTask
import org.netbeans.modules.parsing.spi.ParseException
import org.netbeans.modules.parsing.spi.Parser
import org.netbeans.modules.csl.spi.support.ModificationResult
import org.netbeans.modules.parsing.api.ParserManager
import org.netbeans.modules.parsing.api.ResultIterator
import org.netbeans.modules.parsing.api.Source


import org.netbeans.modules.scala.core.ast.ScalaItems
import org.netbeans.modules.scala.core.{ScalaMimeResolver, ScalaParserResult}

import org.netbeans.modules.refactoring.api.AbstractRefactoring
import org.netbeans.modules.refactoring.api.Problem
import org.netbeans.modules.refactoring.spi.{RefactoringPlugin, ProgressProviderAdapter}
import org.openide.filesystems.FileObject;
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet

/**
 * Plugin implementation based on the one for Java refactoring.
 * 
 * @author Jan Becicka
 * @author Tor Norbye
 */
abstract class ScalaRefactoringPlugin extends ProgressProviderAdapter with RefactoringPlugin {
  protected var cancelled = false  
  protected def isCancelled: Boolean = synchronized {cancelled}

  override def cancelRequest: Unit = synchronized {cancelled = true}

  def createProblem(result: Problem, isFatal: Boolean, message: String): Problem = {
    val problem = new Problem(isFatal, message)

    if (result == null) {
      problem
    } else if (isFatal) {
      problem.setNext(result)
      problem
    } else {
      //problem.setNext(result.getNext());
      //result.setNext(problem);

      // [TODO] performance
      var p = result
      while (p.getNext != null) {
        p = p.getNext
      }
      p.setNext(problem)
      result
    }
  }

  protected def getClasspathInfo(refactoring: AbstractRefactoring): ClasspathInfo = {
    refactoring.getContext.lookup(classOf[ClasspathInfo]) match {
      case null =>
        val handles = refactoring.getRefactoringSource.lookupAll(classOf[ScalaItems#ScalaItem])
        val cpInfo = if (!handles.isEmpty) {
          RetoucheUtils.getClasspathInfoFor(handles.toArray(new Array[ScalaItems#ScalaItem](handles.size)))
        } else {
          RetoucheUtils.getClasspathInfoFor(Array[FileObject](null))
        }
        refactoring.getContext.add(cpInfo)
        
        cpInfo
      case x => x
    }
  }

  protected def processFiles(fos: Set[FileObject], task: TransformTask): Seq[ModificationResult] = {
    val sources = new java.util.HashSet[Source](2 * fos.size)
    
    for (fo <- fos if RetoucheUtils.isScalaFile(fo)) sources.add(Source.create(fo))

    try {
      ParserManager.parse(sources, task)
      return task.results
    } catch {case ex: ParseException => throw new RuntimeException(ex)}
  }

  protected abstract class TransformTask extends UserTask {
    val results = new ArrayBuffer[ModificationResult]

    @throws(classOf[ParseException])
    def run(ri: ResultIterator) {
      visit(ri)
      fireProgressListenerStep
    }

    protected def process(pResult: ScalaParserResult): Seq[ModificationResult]

    @throws(classOf[ParseException])
    private def visit(ri: ResultIterator) {
      if (ri.getSnapshot.getMimeType == ScalaMimeResolver.MIME_TYPE) {
        val pr = ri.getParserResult
        val r = process(pr.asInstanceOf[ScalaParserResult])
        results ++= r
      }

      val itr = ri.getEmbeddings.iterator
      while (itr.hasNext) {
        visit(ri.getResultIterator(itr.next))
      }
    }
  }
  
}
