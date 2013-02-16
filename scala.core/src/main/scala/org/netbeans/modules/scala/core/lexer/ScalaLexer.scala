/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2008 Sun Microsystems, Inc. All rights reserved.
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
package org.netbeans.modules.scala.core.lexer

import java.io.Reader
import java.util.logging.Level
import java.util.logging.Logger
import org.netbeans.api.lexer.{Token, TokenId}
import org.netbeans.modules.scala.core.lexer.ScalaTokenId._
import org.netbeans.modules.scala.core.rats.LexerScala
import org.netbeans.spi.lexer.{Lexer, LexerInput, LexerRestartInfo}
import xtc.tree.GNode
import xtc.util.Pair

/**
 *
 * @author Caoyuan Deng
 */

class ScalaLexer(info: LexerRestartInfo[TokenId]) extends Lexer[TokenId] {
  import ScalaLexer._

  /** 
   * @Note:
   * it seems input at this time is empty, so we can not do scanning here.
   * input will be filled in chars when call nextToken
   */
  private val input = info.input
  private val tokenFactory = info.tokenFactory
  private val lexerInputReader = new LexerInputReader(input)
  private val st = new State(Nil, 0)
    
  override 
  def release = {}

  override 
  def state: Object = null

  override 
  def nextToken: Token[TokenId] = st synchronized {
    // In case of embedded tokens, there may be tokens that had been scanned
    // but not taken yet, check first
    if (st.tokenStream.isEmpty) {
      st.tokenStream = reScanTokens

      /**
       * @Bug of LexerInput.backup(int) ?
       * backup(0) will cause input.readLength() increase 1
       */
      st.lookahead = input.readLength
      if (st.lookahead > 0) {
        // backup all, we will re-read from begin to create token at following step
        input.backup(st.lookahead)
      } else {
        return null
      }
    }

    st.tokenStream match {
      case tokenInfo@TokenInfo(len, id) :: tail =>
        // shift tokenStream
        st.tokenStream = tail
        
        if (len == 0) { // EOF
          return null
        }
        
        // read token's chars according to tokenInfo.length
        var i = 0
        while (i < len) {
          input.read
          i += 1
        }

        // see if needs to lookahead, if true, perform it
        st.lookahead -= len
        // to cheat incremently lexer, we needs to lookahead one more char when
        // tokens.size() > 1 (batched tokens that are not context free),
        // so, when modification happens exactly behind latest token, will
        // force lexer relexer from the 1st token of tokenStream
        val lookahead = if (tail.isEmpty) st.lookahead else st.lookahead + 1
        if (lookahead > 0) {
          var i = 0
          while (i < lookahead) {
            input.read
            i += 1
          }
          input.backup(lookahead)
        }

        val readLen = input.readLength
        assert(readLen > 0, "Token's read length " + readLen + " should > 0: " + tokenInfo)
        createToken(id, readLen)

      case Nil => 
        assert(false, "unrecognized input: " + input.read.toChar)
        null
    }
  }

  private def createToken(id: ScalaTokenId, tokenLen: Int): Token[TokenId] = id.fixedText match {
    case null      => tokenFactory.createToken(id, tokenLen)
    case fixedText => tokenFactory.getFlyweightToken(id, fixedText)
  }

  private def reScanTokens: List[TokenInfo] = {
    /**
     * We cannot keep an instance scope lexer, since lexer (sub-class of ParserBase)
     * has internal states which keeps the read-in chars, index and others, it really
     * difficult to handle.
     */
    val scanner = new LexerScala(lexerInputReader, "<current>")
    val tokens = try {
      // ust scan from position 0, incrmental lexer engine will handle start char in lexerInputReader
      val r = scanner.pToken(0)
      if (r.hasValue) {
        val node = r.semanticValue.asInstanceOf[GNode]
        flattenTokens(node, Nil)
      } else {
        log.warning(r.parseError.msg)
        Nil
      }
    } catch {
      case ex: Exception => log.log(Level.SEVERE, ex.getMessage, ex); Nil
    }
    
    tokens.reverse
  }

  private def flattenTokens(node: GNode, _tokens: List[TokenInfo]): List[TokenInfo] = {
    var tokens = _tokens
    val size = node.size
    if (size == 0) {
      /** 
       * @Note:
       * When node.size == 0, it's a void node. This should be limited to
       * EOF when you define lexical rats.
       *
       * And in Rats!, EOF is !_, the input.readLength() will return 0
       */
      val tokenInfo = if (input.readLength == 0) {
        TokenInfo(0, null)
      } else {
        log.severe("This GNode: '" + node.getName + "' is a void node, this should happen only on EOF. Check you rats file.")
        // best try:
        TokenInfo(input.readLength, ScalaTokenId.Ws)
      }

      tokens ::= tokenInfo
    } else {
      var i = 0
      while (i < size) {
        node.get(i) match {
          case null => // child may be null
          case child: GNode =>
            tokens = flattenTokens(child, tokens)
          case child: Pair[_] =>
            assert(false, "Pair:" + child + " to be process, do you add 'flatten' option on grammar file?")
          case child: String =>
            val length = child.length
            val id = ScalaTokenId.tokenIdOf(node.getName).getOrElse(ScalaTokenId.IGNORED)
            val tokenInfo = TokenInfo(length, id)
            tokens ::= tokenInfo
          case child =>
            log.warning("To be process: " + child)
        }
        i += 1
      }
    }
        
    tokens
  }

}

object ScalaLexer {
  val log = Logger.getLogger(classOf[ScalaLexer].getName)

  private case class TokenInfo(length: Int, id: ScalaTokenId)
  private class State(var tokenStream: List[TokenInfo], var lookahead: Int)
  
  /**
   * Hacking for <code>xtc.parser.ParserBase</code> of Rats! which use <code>java.io.Reader</code>
   * as the chars input, but uses only {@link java.io.Reader#read()} of all methods in
   * {@link xtc.parser.ParserBase#character(int)}
   */
  private class LexerInputReader(input: LexerInput) extends Reader {
    override 
    def read: Int = input.read match {
      case LexerInput.EOF => -1
      case c => c
    }

    override 
    def read(cbuf: Array[Char], off: Int, len: Int): Int = {
      throw new UnsupportedOperationException("Not supported yet.")
    }

    override 
    def close = {}
  }

  /** 
   * @Note:
   * ScalaLexer class is not Reentrant safe, it seems when source size is large than 16 * 1024,
   * there will be more than one input are used, which causes the offset states, such as readed
   * token length, offset etc in these inputs conflict?. Anyway it's safe to create a new one always.
   */
  def create(info: LexerRestartInfo[TokenId]) = new ScalaLexer(info)
}
