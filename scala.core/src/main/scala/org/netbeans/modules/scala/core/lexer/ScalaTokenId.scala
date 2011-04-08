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
package org.netbeans.modules.scala.core.lexer

import java.util.{Collection,
                  HashMap,
                  HashSet,
                  Map}
import org.netbeans.api.lexer.{InputAttributes,
                               LanguagePath,
                               Token,
                               TokenId}
import org.netbeans.spi.lexer.{LanguageHierarchy,
                               Lexer,
                               LexerRestartInfo}
import org.netbeans.modules.scala.core.ScalaMimeResolver
/**
 * 
 * @author Caoyuan Deng
 */
object ScalaTokenId extends Enumeration {
  // Let type of enum's value the same as enum itself
  type ScalaTokenId = V

  // Extends Enumeration.Val to get custom enumeration value
  class V(val name: String, val fixedText: String, val primaryCategory: String) extends Val(name) with TokenId {
    override def ordinal = id
  }
  object V {
    def apply(name: String, fixedText: String, primaryCategory: String) = new V(name, fixedText, primaryCategory)
  }

  val IGNORED = V("IGNORED", null, "ignored")

  val Keyword = V("Keyword", null, "keyword")

  val Identifier = V("Identifier", null, "identifier")

  val DocCommentStart = V("DocCommentStart", null, "comment")
  val DocCommentEnd = V("DocCommentEnd", null, "comment")
  val DocCommentData = V("DocCommentData", null, "comment")
  val BlockCommentStart = V("BlockCommentStart", null, "comment")
  val BlockCommentEnd = V("BlockCommentEnd", null, "comment")
  val BlockCommentData = V("BlockCommentData", null, "comment")
  val CommentTag = V("CommentTag", null, "comment")
  val LineComment = V("LineComment", null, "comment")

  val Ws = V("Ws", null, "whitespace")
  val Nl = V("Nl", null, "whitespace")

  val IntegerLiteral = V("IntegerLiteral", null, "number")
  val FloatingPointLiteral = V("FloatingPointLiteral", null, "number")
  val CharacterLiteral = V("CharacterLiteral", null, "character")
  val StringLiteral = V("StringLiteral", null, "string")
  val SymbolLiteral = V("SymbolLiteral", null, "identifier")

  val Operator = V("Operator", null, "operator")
  val Separator = V("Separator", null, "separator")
  val Error = V("Error", null, "error")

  val LParen = V("LParen", "(", "separator")
  val RParen = V("RParen", ")", "separator")
  val LBrace = V("LBrace", "{", "separator")
  val RBrace = V("RBrace", "}", "separator")
  val LBracket = V("LBracket", "[", "separator")
  val RBracket = V("RBracket", "]", "separator")
  val Comma = V("Comma", ",", "separator")
  val Dot = V("Dot", ".", "separator")
  val Semicolon = V("Semicolon", ";", "separator")
  val Bar = V("Bar", "|", "separator")

  val XmlEmptyTagName = V("XmlEmptyTagName", null, "xml")
  val XmlSTagName = V("XmlSTagName", null, "xml")
  val XmlETagName = V("XmlETagName", null, "xml")
  val XmlAttName = V("XmlAttName", null, "xml")
  val XmlAttValue = V("XmlAttValue", null, "string")
  val XmlLt = V("XmlLt", "<", "xml")
  val XmlGt = V("XmlGt", ">", "xml")
  val XmlLtSlash = V("XmlLtSlash", "</", "xml")
  val XmlSlashGt = V("XmlSlashGt", "/>", "xml")
  val XmlCharData = V("XmlCharData", null, "xmlchardata")
  val XmlEq = V("XmlEq", "=", "separator")
  val XmlComment = V("XmlComment", null, "comment")
  val XmlWs = V("XmlWs", null, "whitespace")
  val XmlCDStart = V("XmlCDStart", null, "comment")
  val XmlCDEnd = V("XmlCDEnd", null, "comment")
  val XmlCDData = V("XmlCDData", null, "xmlcddata")

  val Abstract = V("Abstract", "abstract", "keyword")
  val Case = V("Case", "case", "keyword")
  val Catch = V("Catch", "catch", "keyword")
  val Class = V("Class", "class", "keyword")
  val Def = V("Def", "def", "keyword")
  val Do = V("Do", "do", "keyword")
  val Else = V("Else", "else", "keyword")
  val Extends = V("Extends", "extends", "keyword")
  val False = V("False", "false", "keyword")
  val Final = V("Final", "final", "keyword")
  val Finally = V("Finally", "finally", "keyword")
  val For = V("For", "for", "keyword")
  val ForSome = V("ForSome", "forSome", "keyword")
  val If = V("If", "if", "keyword")
  val Implicit = V("Implicit", "implicit", "keyword")
  val Import = V("Import", "import", "keyword")
  val Lazy = V("Lazy", "lazy", "keyword")
  val Match = V("Match", "match", "keyword")
  val New = V("New", "new", "keyword")
  val Null = V("Null", "null", "keyword")
  val Object = V("Object", "object", "keyword")
  val Override = V("Override", "override", "keyword")
  val Package = V("Package", "package", "keyword")
  val Private = V("Private", "private", "keyword")
  val Protected = V("Protected", "protected", "keyword")
  val Requires = V("Requires", "requires", "keyword")
  val Return = V("Return", "return", "keyword")
  val Sealed = V("Sealed", "sealed", "keyword")
  val Super = V("Super", "super", "keyword")
  val This = V("This", "this", "keyword")
  val Throw = V("Throw", "throw", "keyword")
  val Trait = V("Trait", "trait", "keyword")
  val Try = V("Try", "try", "keyword")
  val True = V("True", "true", "keyword")
  val Type = V("Type", "type", "keyword")
  val Val = V("Val", "val", "keyword")
  val Var = V("Var", "var", "keyword")
  val While = V("While", "while", "keyword")
  val With = V("With", "with", "keyword")
  val Yield = V("Yield", "yield", "keyword")
  val Wild = V("Wild", "_", "s_keyword")
  val RArrow = V("RArrow", null, "s_keyword") // "=>" or "\u21D2", no fixed
  val LArrow = V("LArrow", null, "s_keyword") // "<-" or "\u2190", no fixed
  val UBound = V("UBound", "<:", "s_keyword")
  val VBound = V("VBound", "<%", "s_keyword")
  val LBound = V("LBound", ">:", "s_keyword")
  val Eq = V("Eq", "=", "s_keyword")
  val Colon = V("Colon", ":", "s_keyword")
  val Pound = V("Pound", "#", "s_keyword")
  val At = V("At", "@", "s_keyword")

  val GLOBAL_VAR = V("GLOBAL_VAR", null, "static")
  val CONSTANT = V("CONSTANT", null, "constant")
  val REGEXP_LITERAL = V("REGEXP_LITERAL", null, "regexp")
  val STRING_BEGIN = V("STRING_BEGIN", null, "string")
  val STRING_END = V("STRING_END", null, "string")
  val REGEXP_BEGIN = V("REGEXP_BEGIN", null, "regexp") // or separator,
  val REGEXP_END = V("REGEXP_END", null, "regexp")
  // Cheating: out of laziness just map all keywords returning from JRuby
  // into a single KEYWORD token; eventually I will have separate tokens
  // for each here such that the various helper methods for formatting,
  // smart indent, brace matching etc. can refer to specific keywords
  val ANY_KEYWORD = V("ANY_KEYWORD", null, "keyword")
  val ANY_OPERATOR = V("ANY_OPERATOR", null, "operator")

  val SEMI = V("SEMI", null, "operator")
  // Non-unary operators which indicate a line continuation if used at the end of a line
  val NONUNARY_OP = V("NONUNARY_OP", null, "operator")

  /**
   * MIME type for Erlang. Don't change this without also consulting the various XML files
   * that cannot reference this value directly.
   */
  val SCALA_MIME_TYPE = ScalaMimeResolver.MIME_TYPE

  // * should use "val" instead of "def" here to get a singleton language val, which
  // * will be used to identity the token's language by "==" comparasion by other classes.
  // * Be aware of the init order! to get createTokenIds gathers all TokenIds, should
  // * be put after all token id val definition
  val language = new LanguageHierarchy[TokenId] {
    protected def mimeType = ScalaMimeResolver.MIME_TYPE

    protected def createTokenIds: Collection[TokenId] = {
      val ids = new HashSet[TokenId]
      values.foreach{ids add _.asInstanceOf[TokenId]}
      ids
    }
    
    protected def createLexer(info: LexerRestartInfo[TokenId]): Lexer[TokenId] = ScalaLexer.create(info)

    override protected def createTokenCategories: Map[String, Collection[TokenId]] = {
      val cats = new HashMap[String, Collection[TokenId]]
      cats
    }

    override protected def embedding(token: Token[TokenId], languagePath: LanguagePath, inputAttributes: InputAttributes) = {
      null // No embedding
    }
  }.language

}
