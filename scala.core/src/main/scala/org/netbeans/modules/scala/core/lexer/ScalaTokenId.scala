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

import org.netbeans.api.lexer.{
  InputAttributes,
  LanguagePath,
  Token,
  TokenId
}
import org.netbeans.spi.lexer.{
  LanguageHierarchy,
  Lexer,
  LexerRestartInfo
}
import org.netbeans.modules.scala.core.ScalaMimeResolver
import scala.collection.mutable

/**
 *
 * @author Caoyuan Deng
 */
class ScalaTokenId private (val ordinal: Int, val name: String, val fixedText: String, val primaryCategory: String) extends TokenId {
  override def hashCode = ordinal

  override def equals(o: Any) = {
    if (o == null) false
    else if (o.isInstanceOf[ScalaTokenId]) {
      o.asInstanceOf[ScalaTokenId].ordinal == this.ordinal
    } else false
  }

  override def toString = name
}

object ScalaTokenId {
  private var lastOrdinal = 0
  private val values = new mutable.HashMap[String, ScalaTokenId]()

  private def apply(name: String, fixedText: String, primaryCategory: String) = {
    val x = new ScalaTokenId(lastOrdinal, name, fixedText, primaryCategory)
    lastOrdinal += 1
    values(name) = x
    x
  }

  def tokenIdOf(name: String): Option[ScalaTokenId] = values.get(name)

  val IGNORED = ScalaTokenId("IGNORED", null, "ignored")

  val Keyword = ScalaTokenId("Keyword", null, "keyword")

  val Identifier = ScalaTokenId("Identifier", null, "identifier")

  val DocCommentStart = ScalaTokenId("DocCommentStart", null, "comment")
  val DocCommentEnd = ScalaTokenId("DocCommentEnd", null, "comment")
  val DocCommentData = ScalaTokenId("DocCommentData", null, "comment")
  val BlockCommentStart = ScalaTokenId("BlockCommentStart", null, "comment")
  val BlockCommentEnd = ScalaTokenId("BlockCommentEnd", null, "comment")
  val BlockCommentData = ScalaTokenId("BlockCommentData", null, "comment")
  val CommentTag = ScalaTokenId("CommentTag", null, "comment")
  val LineComment = ScalaTokenId("LineComment", null, "comment")

  val Ws = ScalaTokenId("Ws", null, "whitespace")
  val Nl = ScalaTokenId("Nl", null, "whitespace")

  val IntegerLiteral = ScalaTokenId("IntegerLiteral", null, "number")
  val FloatingPointLiteral = ScalaTokenId("FloatingPointLiteral", null, "number")
  val CharacterLiteral = ScalaTokenId("CharacterLiteral", null, "character")
  val StringLiteral = ScalaTokenId("StringLiteral", null, "string")
  val SymbolLiteral = ScalaTokenId("SymbolLiteral", null, "identifier")

  val Operator = ScalaTokenId("Operator", null, "operator")
  val Separator = ScalaTokenId("Separator", null, "separator")
  val Error = ScalaTokenId("Error", null, "error")

  val LParen = ScalaTokenId("LParen", "(", "separator")
  val RParen = ScalaTokenId("RParen", ")", "separator")
  val LBrace = ScalaTokenId("LBrace", "{", "separator")
  val RBrace = ScalaTokenId("RBrace", "}", "separator")
  val LBracket = ScalaTokenId("LBracket", "[", "separator")
  val RBracket = ScalaTokenId("RBracket", "]", "separator")
  val Comma = ScalaTokenId("Comma", ",", "separator")
  val Dot = ScalaTokenId("Dot", ".", "separator")
  val Semicolon = ScalaTokenId("Semicolon", ";", "separator")
  val Bar = ScalaTokenId("Bar", "|", "separator")

  val XmlEmptyTagName = ScalaTokenId("XmlEmptyTagName", null, "xml")
  val XmlSTagName = ScalaTokenId("XmlSTagName", null, "xml")
  val XmlETagName = ScalaTokenId("XmlETagName", null, "xml")
  val XmlAttName = ScalaTokenId("XmlAttName", null, "xml")
  val XmlAttValue = ScalaTokenId("XmlAttValue", null, "string")
  val XmlLt = ScalaTokenId("XmlLt", "<", "xml")
  val XmlGt = ScalaTokenId("XmlGt", ">", "xml")
  val XmlLtSlash = ScalaTokenId("XmlLtSlash", "</", "xml")
  val XmlSlashGt = ScalaTokenId("XmlSlashGt", "/>", "xml")
  val XmlCharData = ScalaTokenId("XmlCharData", null, "xmlchardata")
  val XmlEq = ScalaTokenId("XmlEq", "=", "separator")
  val XmlComment = ScalaTokenId("XmlComment", null, "comment")
  val XmlWs = ScalaTokenId("XmlWs", null, "whitespace")
  val XmlCDStart = ScalaTokenId("XmlCDStart", null, "comment")
  val XmlCDEnd = ScalaTokenId("XmlCDEnd", null, "comment")
  val XmlCDData = ScalaTokenId("XmlCDData", null, "xmlcddata")

  val Abstract = ScalaTokenId("Abstract", "abstract", "keyword")
  val Case = ScalaTokenId("Case", "case", "keyword")
  val Catch = ScalaTokenId("Catch", "catch", "keyword")
  val Class = ScalaTokenId("Class", "class", "keyword")
  val Def = ScalaTokenId("Def", "def", "keyword")
  val Do = ScalaTokenId("Do", "do", "keyword")
  val Else = ScalaTokenId("Else", "else", "keyword")
  val Extends = ScalaTokenId("Extends", "extends", "keyword")
  val False = ScalaTokenId("False", "false", "keyword")
  val Final = ScalaTokenId("Final", "final", "keyword")
  val Finally = ScalaTokenId("Finally", "finally", "keyword")
  val For = ScalaTokenId("For", "for", "keyword")
  val ForSome = ScalaTokenId("ForSome", "forSome", "keyword")
  val If = ScalaTokenId("If", "if", "keyword")
  val Implicit = ScalaTokenId("Implicit", "implicit", "keyword")
  val Import = ScalaTokenId("Import", "import", "keyword")
  val Lazy = ScalaTokenId("Lazy", "lazy", "keyword")
  val Match = ScalaTokenId("Match", "match", "keyword")
  val New = ScalaTokenId("New", "new", "keyword")
  val Null = ScalaTokenId("Null", "null", "keyword")
  val Object = ScalaTokenId("Object", "object", "keyword")
  val Override = ScalaTokenId("Override", "override", "keyword")
  val Package = ScalaTokenId("Package", "package", "keyword")
  val Private = ScalaTokenId("Private", "private", "keyword")
  val Protected = ScalaTokenId("Protected", "protected", "keyword")
  val Requires = ScalaTokenId("Requires", "requires", "keyword")
  val Return = ScalaTokenId("Return", "return", "keyword")
  val Sealed = ScalaTokenId("Sealed", "sealed", "keyword")
  val Super = ScalaTokenId("Super", "super", "keyword")
  val This = ScalaTokenId("This", "this", "keyword")
  val Throw = ScalaTokenId("Throw", "throw", "keyword")
  val Trait = ScalaTokenId("Trait", "trait", "keyword")
  val Try = ScalaTokenId("Try", "try", "keyword")
  val True = ScalaTokenId("True", "true", "keyword")
  val Type = ScalaTokenId("Type", "type", "keyword")
  val Val = ScalaTokenId("Val", "val", "keyword")
  val Var = ScalaTokenId("Var", "var", "keyword")
  val While = ScalaTokenId("While", "while", "keyword")
  val With = ScalaTokenId("With", "with", "keyword")
  val Yield = ScalaTokenId("Yield", "yield", "keyword")
  val Wild = ScalaTokenId("Wild", "_", "s_keyword")
  val RArrow = ScalaTokenId("RArrow", null, "s_keyword") // "=>" or "\u21D2", no fixed
  val LArrow = ScalaTokenId("LArrow", null, "s_keyword") // "<-" or "\u2190", no fixed
  val UBound = ScalaTokenId("UBound", "<:", "s_keyword")
  val VBound = ScalaTokenId("VBound", "<%", "s_keyword")
  val LBound = ScalaTokenId("LBound", ">:", "s_keyword")
  val Eq = ScalaTokenId("Eq", "=", "s_keyword")
  val Colon = ScalaTokenId("Colon", ":", "s_keyword")
  val Pound = ScalaTokenId("Pound", "#", "s_keyword")
  val At = ScalaTokenId("At", "@", "s_keyword")

  val GLOBAL_VAR = ScalaTokenId("GLOBAL_VAR", null, "static")
  val CONSTANT = ScalaTokenId("CONSTANT", null, "constant")
  val REGEXP_LITERAL = ScalaTokenId("REGEXP_LITERAL", null, "regexp")
  val STRING_BEGIN = ScalaTokenId("STRING_BEGIN", null, "string")
  val STRING_END = ScalaTokenId("STRING_END", null, "string")
  val REGEXP_BEGIN = ScalaTokenId("REGEXP_BEGIN", null, "regexp") // or separator,
  val REGEXP_END = ScalaTokenId("REGEXP_END", null, "regexp")
  // Cheating: out of laziness just map all keywords returning from JRuby
  // into a single KEYWORD token; eventually I will have separate tokens
  // for each here such that the various helper methods for formatting,
  // smart indent, brace matching etc. can refer to specific keywords
  val ANY_KEYWORD = ScalaTokenId("ANY_KEYWORD", null, "keyword")
  val ANY_OPERATOR = ScalaTokenId("ANY_OPERATOR", null, "operator")

  val SEMI = ScalaTokenId("SEMI", null, "operator")
  // Non-unary operators which indicate a line continuation if used at the end of a line
  val NONUNARY_OP = ScalaTokenId("NONUNARY_OP", null, "operator")

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

    protected def createTokenIds: java.util.Collection[TokenId] = {
      val ids = new java.util.HashSet[TokenId]
      values foreach (ids add _._2)
      ids
    }

    protected def createLexer(info: LexerRestartInfo[TokenId]): Lexer[TokenId] = ScalaLexer.create(info)

    override protected def createTokenCategories: java.util.Map[String, java.util.Collection[TokenId]] = {
      val cats = new java.util.HashMap[String, java.util.Collection[TokenId]]

      for ((name, value) <- values) {
        val category = value.primaryCategory
        val tokenIds = cats.get(category) match {
          case null =>
            val x = new java.util.ArrayList[TokenId]()
            cats.put(category, x)
            x
          case x => x
        }
        tokenIds.add(value)
      }

      cats
    }

    override protected def embedding(token: Token[TokenId], languagePath: LanguagePath, inputAttributes: InputAttributes) = {
      null // No embedding
    }
  }.language

}
