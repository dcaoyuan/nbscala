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


import org.netbeans.modules.csl.api.{ElementKind, HtmlFormatter}

trait ScalaHtmlFormatters {

  class SignatureHtmlFormatter extends HtmlFormatter {
    protected var isDeprecated: Boolean = _
    protected var isParameter: Boolean = _
    protected var isType: Boolean = _
    protected var isName: Boolean = _
    protected var isEmphasis: Boolean = _

    protected val sb = new StringBuilder

    def reset {
      textLength = 0
      sb.setLength(0)
    }

    def appendHtml(html: String) {
      sb.append(html)
      // Not sure what to do about maxLength here... but presumably
    }

    override def appendText(text: String, fromInclusive: Int, toExclusive: Int) {
      var i = fromInclusive
      var break = false
      while (i < toExclusive && !break) {
        if (textLength >= maxLength) {
          if (textLength == maxLength) {
            sb.append("...")
            textLength += 3
          }
          break = true
        } else {
          text.charAt(i) match {
            case '<' =>
              sb.append("&lt;") // NOI18N
            case '>' => // Only ]]> is dangerous
              if (i > 1 && text.charAt(i - 2) == ']' && text.charAt(i - 1) == ']') {
                sb.append("&gt;") // NOI18N
              } else {
                sb.append('>')
              }

            case '&' =>
              sb.append("&amp;") // NOI18N

            case c =>
              sb.append(c)
          }
          textLength += 1
        }
        i += 1
      }
    }

    override def name(kind: ElementKind, start: Boolean) {
      assert(start != isName)
      isName = start

      if (isName) {
        sb.append("<b>")
      } else {
        sb.append("</b>")
      }
    }

    def parameters(start: Boolean) {
      assert(start != isParameter)
      isParameter = start

      if (isParameter) {
        sb.append("<font color=\"#808080\">")
      } else {
        sb.append("</font>")
      }
    }

    def `type`(start: Boolean) {
      assert(start != isType)
      isType = start

      if (isType) {
        sb.append("<font color=\"#808080\">")
      } else {
        sb.append("</font>")
      }
    }

    def deprecated(start: Boolean) {
      assert(start != isDeprecated)
      isDeprecated = start

      if (isDeprecated) {
        sb.append("<s>")
      } else {
        sb.append("</s>")
      }
    }

    def emphasis(start: Boolean) {
      assert(start != isEmphasis)
      isEmphasis = start

      if (isEmphasis) {
        sb.append("<b>")
      } else {
        sb.append("</b>")
      }
    }

    override def active(start: Boolean) {
      emphasis(start)
    }

    override def getText: String = {
      assert(!isParameter && !isDeprecated && !isName && !isType)
      sb.toString
    }

    override def toString = {
      getText
    }
  }


  import org.netbeans.api.lexer.{TokenHierarchy}
  import org.netbeans.modules.scala.core.lexer.ScalaTokenId
  import org.openide.util.NbBundle

  /**
   *
   * @author Martin Adamek
   * @author Caoyuan Deng
   */
  object ScalaCommentFormatter {
    private val PARAM_TAG = "@param" //NOI18N
    private val RETURN_TAG = "@return" //NOI18N
    private val TYPE_TAG = "@type" //NOI18N
    private val THROWS_TAG = "@throws" //NOI18N
    private val DEPRECATED_TAG = "@deprecated" //NOI18N
    private val CODE_TAG = "@code" //NOI18N
    private val EXAMPLE_TAG = "@example" //NOI18N
    private val DESCRIPTION_TAG = "@description" //NOI18N
  }

  class ScalaCommentFormatter(comment: String) {
    import ScalaCommentFormatter._

    private var returnTag: String = _
    private var returnType: String = _
    private var deprecation: String = _
    private var code: String = _
    // flag to see if this is already formatted comment with all html stuff
    private var formattedComment :Boolean = _

    private val summary = new StringBuilder
    private val rest = new StringBuilder
    private var params: List[String] = Nil
    private var exceptions: List[String] = Nil
    private val th = TokenHierarchy.create(comment, ScalaTokenId.language)
    private val ts = th.tokenSequence(ScalaTokenId.language)
    process


    def setSeqName(name: String) {}

    def toHtml: String = {
      val sb = new StringBuilder

      if (!formattedComment && summary.length > 0) {
        val summaryText = summary.toString.trim
        if (summaryText.length > 0) {
          sb.append("<b>")
          sb.append(NbBundle.getMessage(classOf[ScalaCommentFormatter], "Summary"))
          sb.append("</b><blockquote>").append(summaryText).append("</blockquote>") //NOI18N
        }
      } else {
        sb.append(summary)
      }

      if (deprecation != null) {
        val hasDescription = deprecation.trim.length > 0
        sb.append("<b")
        if (!hasDescription) {
          sb.append(" style=\"background:#ffcccc\"")
        }
        sb.append(">")
        sb.append(NbBundle.getMessage(classOf[ScalaCommentFormatter], "Deprecated"))
        sb.append("</b>")
        sb.append("<blockquote")
        if (hasDescription) {
          sb.append(" style=\"background:#ffcccc\">")
          sb.append(deprecation)
        } else {
          sb.append(">")
        }
        sb.append("</blockquote>") //NOI18N
      }

      if (!params.isEmpty) {
        sb.append("<b>")
        sb.append(NbBundle.getMessage(classOf[ScalaCommentFormatter], "Parameters"))
        sb.append("</b><blockquote>") //NOI18N
        var i = 0
        for (param <- params) {
          if (i > 0) {
            sb.append("<br><br>") // NOI18N
          }
          sb.append(param)
          i += 1
        }
        sb.append("</blockquote>") // NOI18N
      }

      if (returnTag != null || returnType != null) {
        sb.append("<b>"); // NOI18N
        sb.append(NbBundle.getMessage(classOf[ScalaCommentFormatter], "Returns"))
        sb.append("</b><blockquote>") //NOI18N
        if (returnTag != null) {
          sb.append(returnTag)
          if (returnType != null) {
            sb.append("<br>") // NOI18N
          }
        }
        if (returnType != null) {
          sb.append(NbBundle.getMessage(classOf[ScalaCommentFormatter], "ReturnType"))
          sb.append(" <i>") // NOI18N
          sb.append(returnType)
          sb.append("</i>") // NOI18N
        }
        sb.append("</blockquote>") //NOI18N
      }

      if (exceptions.size > 0) {
        sb.append("<b>")
        sb.append(NbBundle.getMessage(classOf[ScalaCommentFormatter], "Throws"))
        sb.append("</b><blockquote>") //NOI18N
        for (tag <- exceptions) {
          sb.append(tag)
          sb.append("<br>") // NOI18N
        }
        sb.append("</blockquote>") // NOI18N
      }

      if (code != null) {
        sb.append("<b>")
        sb.append(NbBundle.getMessage(classOf[ScalaCommentFormatter], "CodeExample"))
        sb.append("</b><blockquote>") //NOI18N
        sb.append("<pre>").append(code).append("</pre></blockquote>") //NOI18N
      }

      if (!rest.isEmpty) {
        sb.append("<b>")
        sb.append(NbBundle.getMessage(classOf[ScalaCommentFormatter], "Miscellaneous"))
        sb.append("</b><blockquote>") //NOI18N
        sb.append(rest)
        sb.append("</blockquote>") // NOI18N
      }

      sb.toString
    }

    def getSummary: String = {
      summary.toString.trim
    }

    def getParams: List[String] = {
      params
    }

    def getExceptions: List[String] = {
      exceptions
    }

    def getReturn: String = {
      returnTag
    }

    private def process {
      while (ts.moveNext && ts.token.id != ScalaTokenId.CommentTag) {
        val token = ts.token
        val line = token.text.toString.trim
        summary.append(removeStar(line)).append(' ')
      }

      ts.movePrevious
      var sb :StringBuilder = null
      while (ts.moveNext) {
        val token = ts.token
        if (token.id == ScalaTokenId.CommentTag) {
          if (sb != null) {
            processTag(sb.toString.trim)
          }
          sb = new StringBuilder
        }
        if (sb != null) { // we have some tags
          val line = token.text.toString.trim
          sb.append(removeStar(line)).append(' ')
        }
      }

      if (sb != null) {
        processTag(sb.toString.trim)
      }

      params.reverse
      exceptions.reverse
    }

    private def processTag(tag: String) {
      if (tag.startsWith(PARAM_TAG)) {
        // Try to make the parameter name bold, and the type italic
        val s = tag.substring(PARAM_TAG.length).trim
        if (s.length == 0) {
          return
        }

        val sb = new StringBuilder
        var index = 0
        if (s.charAt(0) == '{') {
          // We have a type
          var end = s.indexOf('}')
          if (end != -1) {
            end += 1
            sb.append("<i>") // NOI18N
            sb.append(s.substring(0, end))
            sb.append("</i>") // NOI18N
          }
          index = end
          while (index < s.length && Character.isWhitespace((s.charAt(index)))) {
            index += 1
          }
        }

        if (index < s.length) {
          var end = index
          while (end < s.length && !Character.isWhitespace((s.charAt(end)))) {
            end += 1
          }
          if (end < s.length) {
            sb.append(" <b>") // NOI18N
            sb.append(s.substring(index, end))
            sb.append("</b>") // NOI18N
            sb.append(s.substring(end))
            params = sb.toString :: params
            return
          }
        }
        params = s :: params
      } else if (tag.startsWith(DESCRIPTION_TAG)) {
        val desc = tag.substring(DESCRIPTION_TAG.length).trim
        summary.insert(0, desc)
      } else if (tag.startsWith(RETURN_TAG)) {
        returnTag = tag.substring(RETURN_TAG.length).trim
      } else if (tag.startsWith(TYPE_TAG)) {
        returnType = tag.substring(TYPE_TAG.length).trim
      } else if (tag.startsWith(THROWS_TAG)) {
        exceptions = tag.substring(THROWS_TAG.length).trim :: exceptions
      } else if (tag.startsWith(DEPRECATED_TAG)) {
        deprecation = tag.substring(DEPRECATED_TAG.length).trim
      } else if (tag.startsWith(CODE_TAG)) {
        code = tag.substring(CODE_TAG.length).trim
        code = code.replace("&", "&amp;") // NOI18N
        code = code.replace("<", "&lt;")  // NOI18N
        code = code.replace(">", "&gt;")  // NOI18N
      } else if (tag.startsWith(EXAMPLE_TAG)) {
        code = tag.substring(EXAMPLE_TAG.length).trim
        code = code.replace("&", "&amp;") // NOI18N
        code = code.replace("<", "&lt;")  // NOI18N
        code = code.replace(">", "&gt;")  // NOI18N
      } else { // NOI18N
        // Store up the rest of the stuff so we don't miss unexpected tags,
        // like @private, @config, etc.
        if (!tag.startsWith("@id ") &&
            !tag.startsWith("@name ") && // NOI18N
            !tag.startsWith("@attribute") && // NOI18N
            !tag.startsWith("@method") &&
            !tag.startsWith("@property")) { // NOI18N
          rest.append(tag)
          rest.append("<br>") // NOI18N
        }
      }
    }

    private def removeStar(line: String): String = {
      val line1 = line.trim
      if (line1.startsWith("/**")) {
        line1.substring(3)
      } else if (line1.endsWith("*/")) {
        line1.substring(0, line1.length - 2)
      } else if (line1.startsWith("//")) {
        line1.substring(2)
      } else if (line1.startsWith("/*")) {
        line1.substring(2)
      } else if (line1.startsWith("*")) {
        line1.substring(1)
      } else {
        line1
      }
    }
  }
}
