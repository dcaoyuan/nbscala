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
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2007 Sun
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

package org.netbeans.modules.scala.console.readline

import scala.collection.mutable.ArrayBuffer

object ConsoleLineReader {

  var _currentCompletor: Completor = null
  var _history: History = null
  var created: Boolean = false

  def createConsoleLineReader() {
    if (!created) {
      _history = new History()
      _currentCompletor = null
      initReadline
    }
  }

  protected def initReadline() {
    if (_currentCompletor eq null)
      _currentCompletor = new DefaultFileNameCompletor()
  }

  def history = _history

  def completor = _currentCompletor
  def completor_=(completor: Completor) {
    _currentCompletor = completor
  }

  // Fix FileNameCompletor to work mid-line
  class DefaultFileNameCompletor extends FileNameCompletor {
    override def complete(inBuffer: String, cursor: Int, candidates: ArrayBuffer[String]): Int = {
      var buffer = inBuffer.substring(0, cursor);
      val index = buffer.lastIndexOf(" ");
      if (index != -1) buffer = buffer.substring(index + 1);
      index + 1 + super.complete(buffer, cursor, candidates);
    }
  }

}
