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

import javax.swing.text.{AbstractDocument, BadLocationException}
import org.netbeans.editor.BaseDocument
import org.netbeans.modules.scala.core.lexer.{ScalaTokenId, ScalaLexUtil}
import org.netbeans.spi.editor.bracesmatching.{BracesMatcher, MatcherContext}

/**
 *
 * @author Caoyuan Deng
 */
class ScalaBracesMatcher(context: MatcherContext) extends BracesMatcher {

  @throws(classOf[InterruptedException])
  @throws(classOf[BadLocationException])
  override def findOrigin: Array[Int] = {
    var offset = context.getSearchOffset
    val doc = context.getDocument.asInstanceOf[BaseDocument]

    //doc.readLock
    try {
      ScalaLexUtil.getTokenSequence(doc, offset) foreach {ts =>
        ts.move(offset)
        if (!ts.moveNext) {
          return null
        }

        var token = ts.token
        if (token == null) {
          return null
        }

        var id = token.id

        if (id == ScalaTokenId.Ws) {
          // ts.move(offset) gives the token to the left of the caret.
          // If you have the caret right at the beginning of a token, try
          // the token to the right too - this means that if you have
          //  "   |def" it will show the matching "end" for the "def".
          offset += 1
          ts.move(offset)
          if (ts.moveNext && ts.offset <= offset) {
            token = ts.token
            id = token.id
          }
        }

        id match {
          case ScalaTokenId.STRING_BEGIN =>
            return Array(ts.offset, ts.offset + token.length)
          case ScalaTokenId.STRING_END =>
            return Array(ts.offset, ts.offset + token.length)
          case ScalaTokenId.REGEXP_BEGIN =>
            return Array(ts.offset, ts.offset + token.length)
          case ScalaTokenId.REGEXP_END =>
            return Array(ts.offset, ts.offset + token.length)
          case ScalaTokenId.LParen =>
            return Array(ts.offset, ts.offset + token.length)
          case ScalaTokenId.RParen =>
            return Array(ts.offset, ts.offset + token.length)
          case ScalaTokenId.LBrace =>
            return Array(ts.offset, ts.offset + token.length)
          case ScalaTokenId.RBrace =>
            return Array(ts.offset, ts.offset + token.length)
          case ScalaTokenId.LBracket =>
            return Array(ts.offset, ts.offset + token.length)
            //            } else if (id == ScalaTokenId.DO && !ScalaLexUtil.isEndmatchingDo(doc, ts.offset())) {
            //                // No matching dot for "do" used in conditionals etc.
            //                return OffsetRange.NONE;
          case ScalaTokenId.RBracket =>
            return Array(ts.offset, ts.offset + token.length)
            //            } else if (id.primaryCategory().equals("keyword")) {
            //                if (ScalaLexUtil.isBeginToken(id, doc, ts)) {
            //                    return ScalaLexUtil.findEnd(doc, ts);
            //                } else if ((id == ScalaTokenId.END) || ScalaLexUtil.isIndentToken(id)) { // Find matching block
            //
            //                    return ScalaLexUtil.findBegin(doc, ts);
            //                }
          case _ =>
        }
      }

      null
    } finally {
      //doc.readUnlock
    }
  }


  @throws(classOf[InterruptedException])
  @throws(classOf[BadLocationException])
  override def findMatches: Array[Int] = {
    var offset = context.getSearchOffset
    val doc = context.getDocument.asInstanceOf[BaseDocument]
    
    //doc.readLock
    try {
      ScalaLexUtil.getTokenSequence(doc, offset) foreach {ts =>
        ts.move(offset)
        if (!ts.moveNext) {
          return null
        }

        var token = ts.token
        if (token == null) {
          return null
        }

        var id = token.id

        if (id == ScalaTokenId.Ws) {
          // ts.move(offset) gives the token to the left of the caret.
          // If you have the caret right at the beginning of a token, try
          // the token to the right too - this means that if you have
          //  "   |def" it will show the matching "end" for the "def".
          offset += 1
          ts.move(offset)
          if (ts.moveNext && ts.offset <= offset) {
            token = ts.token
            id = token.id
          }
        }

        id match {
          case ScalaTokenId.STRING_BEGIN =>
            val range = ScalaLexUtil.findFwd(ts, ScalaTokenId.STRING_BEGIN, ScalaTokenId.STRING_END)
            return Array(range.getStart, range.getEnd)
          case ScalaTokenId.STRING_END =>
            val range = ScalaLexUtil.findBwd(ts, ScalaTokenId.STRING_BEGIN, ScalaTokenId.STRING_END);
            return Array(range.getStart, range.getEnd)
          case ScalaTokenId.REGEXP_BEGIN =>
            val range = ScalaLexUtil.findFwd(ts, ScalaTokenId.REGEXP_BEGIN, ScalaTokenId.REGEXP_END);
            return Array(range.getStart, range.getEnd)
          case ScalaTokenId.REGEXP_END =>
            val range = ScalaLexUtil.findBwd(ts, ScalaTokenId.REGEXP_BEGIN, ScalaTokenId.REGEXP_END);
            return Array(range.getStart, range.getEnd)
          case ScalaTokenId.LParen =>
            val range = ScalaLexUtil.findFwd(ts, ScalaTokenId.LParen, ScalaTokenId.RParen);
            return Array(range.getStart, range.getEnd)
          case ScalaTokenId.RParen =>
            val range = ScalaLexUtil.findBwd(ts, ScalaTokenId.LParen, ScalaTokenId.RParen);
            return Array(range.getStart, range.getEnd)
          case ScalaTokenId.LBrace =>
            val range = ScalaLexUtil.findFwd(ts, ScalaTokenId.LBrace, ScalaTokenId.RBrace);
            return Array(range.getStart, range.getEnd)
          case ScalaTokenId.RBrace =>
            val range = ScalaLexUtil.findBwd(ts, ScalaTokenId.LBrace, ScalaTokenId.RBrace);
            return Array(range.getStart, range.getEnd)
          case ScalaTokenId.LBracket =>
            val range = ScalaLexUtil.findFwd(ts, ScalaTokenId.LBracket, ScalaTokenId.RBracket);
            return Array(range.getStart, range.getEnd)
            //            } else if (id == ScalaTokenId.DO && !ScalaLexUtil.isEndmatchingDo(doc, ts.offset())) {
            //                // No matching dot for "do" used in conditionals etc.
            //                return OffsetRange.NONE;
          case ScalaTokenId.RBracket =>
            val range = ScalaLexUtil.findBwd(ts, ScalaTokenId.LBracket, ScalaTokenId.RBracket);
            return Array(range.getStart, range.getEnd)
            //            } else if (id.primaryCategory().equals("keyword")) {
            //                if (ScalaLexUtil.isBeginToken(id, doc, ts)) {
            //                    return ScalaLexUtil.findEnd(doc, ts);
            //                } else if ((id == ScalaTokenId.END) || ScalaLexUtil.isIndentToken(id)) { // Find matching block
            //
            //                    return ScalaLexUtil.findBegin(doc, ts);
            //                }
          case _ =>
        }
      }

      null
    } finally {
      //doc.readUnlock
    }
  }

}
