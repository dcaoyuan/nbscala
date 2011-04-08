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

import org.netbeans.modules.csl.api.{InstantRenamer, ElementKind, OffsetRange}
import org.netbeans.modules.csl.spi.ParserResult

import org.netbeans.modules.scala.core.ScalaParserResult
import org.netbeans.modules.scala.core.lexer.ScalaLexUtil

/**
 * Handle instant rename for Scala
 *
 * @author Caoyuan Deng
 */
class ScalaInstantRenamer extends InstantRenamer {

  override def isRenameAllowed(info: ParserResult, caretOffset: Int, explanationRetValue: Array[String]): Boolean = {
    val pResult = info.asInstanceOf[ScalaParserResult]
    val rootScope = pResult.rootScope

    val document = info.getSnapshot.getSource.getDocument(true)
    if (document == null) {
      return false
    }

    val th = info.getSnapshot.getTokenHierarchy

    val astOffset = ScalaLexUtil.getAstOffset(info, caretOffset);
    if (astOffset == -1) {
      return false
    }

    val closest = rootScope.findItemsAt(th, caretOffset) match {
      case Nil => return false
      case xs => xs.reverse.head
    }

    val dfn = rootScope.findDfnOf(closest).getOrElse(return false)

    dfn.getName match {
      case "this" | "super" => return false
      case _ =>
    }

    dfn.getKind match {
      case ElementKind.FIELD | ElementKind.PARAMETER | ElementKind.VARIABLE | ElementKind.METHOD => true
      case _ => false
    }
  }

  override def getRenameRegions(info: ParserResult, caretOffset: Int): java.util.Set[OffsetRange] = {
    if (info == null) {
      return java.util.Collections.emptySet[OffsetRange]
    }
    
    val pResult = info.asInstanceOf[ScalaParserResult]

    val document = pResult.getSnapshot.getSource.getDocument(true)
    if (document == null) {
      return java.util.Collections.emptySet[OffsetRange]
    }

    val th = pResult.getSnapshot.getTokenHierarchy

    val astOffset = ScalaLexUtil.getAstOffset(pResult, caretOffset)
    if (astOffset == -1) {
      return java.util.Collections.emptySet[OffsetRange]
    }

    val rootScope = pResult.rootScope

    val occurrences = rootScope.findItemsAt(th, caretOffset) match {
      case Nil => return java.util.Collections.emptySet[OffsetRange]
      case xs => rootScope.findOccurrences(xs.reverse.head)
    }

    val regions = new java.util.HashSet[OffsetRange]
    for (item <- occurrences;
         idToken = item.idToken
    ) {
      regions.add(ScalaLexUtil.getRangeOfToken(th, idToken))
    }

    if (!regions.isEmpty) {
      val translated = new java.util.HashSet[OffsetRange](2 * regions.size)
      val itr = regions.iterator
      while (itr.hasNext) {
        val astRange = itr.next
        ScalaLexUtil.getLexerOffsets(info, astRange) match {
          case OffsetRange.NONE =>
          case lexRange => translated.add(lexRange)
        }
      }

      translated
    } else regions
  }
}
