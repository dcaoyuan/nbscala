/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved.
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
 * Portions Copyrighted 2008 Sun Microsystems, Inc.
 */
package org.netbeans.modules.scala.core.element

import org.netbeans.modules.csl.api.{ElementHandle, ElementKind, Modifier, HtmlFormatter, OffsetRange}
import org.netbeans.modules.csl.api.UiUtils
import org.netbeans.modules.csl.spi.ParserResult
import org.openide.filesystems.FileObject

import javax.swing.Icon
import org.netbeans.api.language.util.ast.{AstElementHandle}

import org.netbeans.modules.scala.core.ScalaGlobal

/**
 * Wrap org.netbeans.api.java.source.ElementHandle to org.netbeans.modules.csl.api.ElementHandle
 *
 * Represents a program element such as a package, class, or method. Each element
 * represents a static, language-level construct (and not, for example, a runtime
 * construct of the virtual machine).
 *
 * @author Caoyuan Deng
 */
trait JavaElements {self: ScalaGlobal =>
  
  object JavaElement {
    def apply(element: org.netbeans.api.java.source.ElementHandle[_]) = {
      new JavaElement(element)
    }
    /*_
     def isMirroredBy(element:Element, mirror:AstMirror): Boolean = {
     if (element.isInstanceOf[ExecutableElement] && mirror.isInstanceOf[FunctionCall]) {
     val function = element.asInstanceOf[ExecutableElement]
     val funCall = mirror.asInstanceOf[FunctionCall]
     val params = function.getParameters
     // only check local call only
     if (funCall.isLocal) {
     return element.getSimpleName.toString.equals(funCall.getCall().getSimpleName().toString()) &&
     params != null &&
     params.size == funCall.getArgs.size
     } else {
     val containsVariableLengthArg = Function.isVarArgs(function)
     if (element.getSimpleName.toString.equals(funCall.getCall().getSimpleName().toString()) || element.getSimpleName.toString.equals("apply") && funCall.isLocal) {
     if (params.size == funCall.getArgs.size || containsVariableLengthArg) {
     return true
     }
     }

     return false
     }
     } else if (element.isInstanceOf[VariableElement]) {
     if (element.getSimpleName.toString.equals(mirror.getSimpleName.toString)) {
     return true
     }
     }

     false
     }
     */
  }

  class JavaElement(element: org.netbeans.api.java.source.ElementHandle[_]) extends AstElementHandle {

    def getFileObject: FileObject = null
    
    def getMimeType: String = "text/x-java"
    
    def getName: String = {
      element.getBinaryName
    }

    def getIn: String = {
      element.getQualifiedName
    }

    def getKind: ElementKind = {
      element.getKind match {
        case javax.lang.model.element.ElementKind.PACKAGE => ElementKind.PACKAGE
        case javax.lang.model.element.ElementKind.CLASS => ElementKind.CLASS
        case javax.lang.model.element.ElementKind.INTERFACE => ElementKind.MODULE
        case javax.lang.model.element.ElementKind.METHOD => ElementKind.METHOD
        case _ => ElementKind.OTHER
      }
    }

    /** @todo */
    def getModifiers: java.util.Set[Modifier] = java.util.Collections.emptySet[Modifier]

    def signatureEquals(handle: ElementHandle): Boolean = {
      getIn == handle.getIn && getName == handle.getName
    }

    def getOffsetRange(result: ParserResult): OffsetRange = OffsetRange.NONE

    // ----- AstElementHandel methods:

    def symbol: Any = element

    def tpe: String = ""

    def getDocComment: String = ""

    def qualifiedName: String = element.getQualifiedName

    def isDeprecated: Boolean = false
    def isDeprecated_=(b: Boolean) {}

    def isInherited: Boolean = false
    def isInherited_=(b: Boolean) {}

    def isEmphasize: Boolean = false
    def isEmphasize_=(b: Boolean) {}

    def isImplicit: Boolean = false
    def isImplicit_=(b: Boolean) {}

    def getIcon: Icon = UiUtils.getElementIcon(getKind, getModifiers)

    // -----

    override def toString = {
      getName + "(kind=" + getKind + ", element=" + element + ")"
    }

    def mayEquals(handle: JavaElement): Boolean = {
      getName == handle.getName
    }

    def htmlFormat(formatter: HtmlFormatter): Unit = {
      formatter.appendText(getName)
    }

    def sigFormat(fm: HtmlFormatter) : Unit = {
    }
  }
}
