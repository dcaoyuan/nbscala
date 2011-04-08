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

package org.netbeans.modules.scala.core

import org.netbeans.api.lexer.TokenHierarchy

import org.netbeans.api.language.util.ast.AstItem

import org.netbeans.modules.scala.core.lexer.{ScalaTokenId}

import scala.tools.nsc.reporters.{Reporter}
import scala.tools.nsc.io.VirtualFile
import scala.tools.nsc.util.BatchSourceFile
import scala.tools.nsc.util.{Position}

abstract class ScalaSymbolResolver {
  
  private val dummyReport = new Reporter {def info0(pos: Position, msg: String, severity: Severity, force: Boolean) {}}

  val global: ScalaGlobal
  import global._

  def resolveQualifiedName(srcPkg: String, fqn: String): Option[AstItem] = {
    val sb = new StringBuilder
    if (srcPkg.length > 0) {
      sb.append("package ").append(srcPkg)
    }
    sb.append("object NetBeansErrorRecover {")
    sb.append(fqn).append(".") // should put a `.` at the end to create a Select tree and corresponding selectTypeErrors
    sb.append("}")

    val srcFile = new BatchSourceFile(new VirtualFile("<NetBeansErrorRecover.scala>", ""), sb)
    val th = TokenHierarchy.create(sb.toString, ScalaTokenId.language)
    global.reporter = dummyReport
    val rootScope = global.askForSemantic(srcFile, true, th)
    
    val lastDot = fqn.lastIndexOf('.')
    val lastPart = if (lastDot == -1) fqn else fqn.substring(lastDot + 1, fqn.length)

    rootScope.findFirstItemWithName(lastPart)
  }
}
