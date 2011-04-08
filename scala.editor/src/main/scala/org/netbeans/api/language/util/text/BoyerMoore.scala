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

package org.netbeans.api.language.util.text

object BoyerMoore {

  /** Pattern length under which might as well use String#indexOf instead of the BoyerMoore algorithm. */
  private val patternThreshold = 4
  /** Text length under which might as well use String#indexOf instead of the BoyerMoore algorithm. */
  private val textThreshold = 2048

  def indexOf(text: String, pattern: String): Int = {
    indexOf(text, pattern, 0)
  }

  def indexOf(text: Array[Char], pattern: String): Int = {
    indexOf(text, pattern, 0)
  }

  def indexOf(text: String, pattern: String, index: Int): Int = {
    BoyerMoore(text).indexOf(pattern, index)
  }

  def indexOf(text: Array[Char], pattern: String, index: Int): Int = {
    BoyerMoore(text).indexOf(pattern, index)
  }
  
  def apply(text: String) = {
    new BoyerMoore(text, null, text.length)
  }
  
  def apply(text: Array[Char]) = {
    new BoyerMoore(null, text, text.length)
  }
}

class BoyerMoore(text: String, textArray: Array[Char], lenText: Int) {
  import BoyerMoore._

  private var pattern: String = _
  private var prevPattern: String = _
  private var lenPat = 0

  private var skip: Array[Int] = new Array[Int](256)
  private def setPattern(pattern: String) {
    if (pattern == prevPattern) return

    this.pattern = pattern
    this.lenPat = pattern.length

    compilePattern
    
    prevPattern = pattern
  }

  private final def compilePattern {
    var i = 0
    while (i < 256) {
      skip(i) = lenPat
      i += 1
    }

    var j = 0
    while (j < lenPat - 1) {
      skip(pattern.charAt(j) & 0xff) = lenPat - j - 1
      j += 1
    }
  }

  def indexOf(pattern: String, index: Int = 0): Int = {
    setPattern(pattern)
    indexOf(index)
  }

  private final def indexOf(index: Int): Int = {
    if (lenText <= textThreshold / 2 || lenPat <= patternThreshold) {
      return (if (text != null) text else new String(textArray)).indexOf(pattern, index)
    }

    if (text != null) {
      indexOfTextString(index)
    } else {
      indexOfTextArray(index)
    }
  }

  private final def indexOfTextString(index: Int): Int = {
    val plast = pattern.charAt(lenPat - 1)
    var tforward = index + lenPat - 1
    while (tforward < lenText) {
      val c = text.charAt(tforward)
      if (c == plast) {
        var tback = tforward - 1
        var pback = lenPat - 2
        var breakInner = false
        while (pback >= 0 && !breakInner) {
          if (text.charAt(tback) == pattern.charAt(pback)) {
            tback -= 1
            pback -= 1
          } else {
            tforward += skip(c & 0xff)
            breakInner = true
          }
        }

        if (!breakInner) {
          return tforward - lenPat + 1
        }
      }

      tforward += skip(c & 0xff)
    }

    -1
  }

  private final def indexOfTextArray(index: Int): Int = {
    val plast = pattern.charAt(lenPat - 1)
    var tforward = index + lenPat - 1
    while (tforward < lenText) {
      val c = textArray(tforward)
      if (c == plast) {
        var tback = tforward - 1
        var pback = lenPat - 2
        var breakInner = false
        while (pback >= 0 && !breakInner) {
          if (textArray(tback) == pattern.charAt(pback)) {
            tback -= 1
            pback -= 1
          } else {
            tforward += skip(c & 0xff)
            breakInner = true
          }
        }
        
        if (!breakInner) {
          return tforward - lenPat + 1
        }
      }
      
      tforward += skip(c & 0xff)
    }
    
    -1
  }
}
