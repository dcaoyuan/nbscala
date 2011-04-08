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

import com.sun.javadoc.Doc;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Scope;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;

import java.io.IOException
import java.lang.ref.Reference
import java.util._;
import java.util.regex.Pattern

import javax.lang.model.element._
import javax.lang.model.`type`._
import javax.lang.model.util._

import org.netbeans.api.java.source.ClasspathInfo
import org.netbeans.api.java.source.CompilationController
import org.netbeans.api.java.source.CompilationInfo
import org.netbeans.api.java.source.ElementHandle
import org.netbeans.api.java.source.JavaSource
import org.netbeans.api.java.source.SourceUtils
import org.netbeans.api.java.source.Task
import org.openide.filesystems.FileObject
import org.openide.util.Exceptions
import scala.tools.nsc.symtab.Symbols

/**
 *
 * @author Caoyuan Deng
 */
object JavaSourceUtil {

  /** trick to avoid Enum_Tag constant returns NoType */
  val PHASE_ELEMENTS_RESOLVED = JavaSource.Phase.ELEMENTS_RESOLVED
  val PHASE_RESOLVED = JavaSource.Phase.RESOLVED

  private val CAPTURED_WILDCARD = "<captured wildcard>" //NOI18N
  private val ERROR = "<error>" //NOI18N
  private val UNKNOWN = "<unknown>" //NOI18N
  private var caseSensitive = true
  private var showDeprecatedMembers = true
  private var inited :Boolean = _
  private var cachedPrefix: String = _
  private var cachedPattern: Pattern = _

  def startsWith(theString: String, prefix: String): Boolean = {
    if (theString == null || theString.length == 0 || ERROR.equals(theString)) {
      return false
    }
    if (prefix == null || prefix.length == 0) {
      return true
    }
    if (isCaseSensitive) theString.startsWith(prefix) else theString.toLowerCase.startsWith(prefix.toLowerCase)
  }

  def startsWithCamelCase(theString: String, prefix: String): Boolean = {
    if (theString == null || theString.length == 0 || prefix == null || prefix.length == 0) {
      return false
    }
    if (!prefix.equals(cachedPrefix) || cachedPattern == null) {
      val sb = new StringBuilder
      var lastIndex = 0
      var index = 0
      do {
        index = findNextUpper(prefix, lastIndex + 1)
        val token = prefix.substring(lastIndex, if (index == -1) prefix.length else index)
        sb.append(token)
        sb.append(if (index != -1) "[\\p{javaLowerCase}\\p{Digit}_\\$]*" else ".*") // NOI18N
        lastIndex = index
      } while (index != -1)
      cachedPrefix = prefix
      cachedPattern = Pattern.compile(sb.toString)
    }
    cachedPattern.matcher(theString).matches
  }

  private def findNextUpper(text: String, offset: Int): Int = {
    for (i <- offset until text.length) {
      if (Character.isUpperCase(text.charAt(i))) {
        return i
      }
    }
    -1
  }

  def isCaseSensitive: Boolean = {
    return caseSensitive
  }

  def setCaseSensitive(b: Boolean) {
    caseSensitive = b
  }

  def isShowDeprecatedMembers: Boolean = {
    return showDeprecatedMembers
  }

  def setShowDeprecatedMembers(b: Boolean) {
    showDeprecatedMembers = b
  }

  def getImportanceLevel(fqn: String): Int = {
    var weight = 50
    if (fqn.startsWith("java.lang") || fqn.startsWith("java.util")) { // NOI18N
      weight -= 10
    } else if (fqn.startsWith("org.omg") || fqn.startsWith("org.apache")) { // NOI18N
      weight += 10
    } else if (fqn.startsWith("com.sun") || fqn.startsWith("com.ibm") || fqn.startsWith("com.apple")) { // NOI18N
      weight += 20
    } else if (fqn.startsWith("sun") || fqn.startsWith("sunw") || fqn.startsWith("netscape")) { // NOI18N
      weight += 30
    }
    weight
  }

  private val scalaFileToJavaSource = new WeakHashMap[FileObject, Reference[JavaSource]]
  private val scalaFileToJavaCompilationInfo = new WeakHashMap[FileObject, Reference[CompilationInfo]]

  def getCompilationInfoForScalaFile(fo: FileObject): CompilationInfo = {
    var info = scalaFileToJavaCompilationInfo.get(fo) match {
      case null => null
      case ref => ref.get
    }

    if (info == null) {
      val javaControllers = new Array[CompilationInfo](1)
      val source = getJavaSourceForScalaFile(fo)
      try {
        source.runUserActionTask(new Task[CompilationController] {

            @throws(classOf[Exception])
            def run(controller: CompilationController) {
              controller.toPhase(PHASE_ELEMENTS_RESOLVED)
              javaControllers(0) = controller
            }
          }, true)
      } catch {case ex: IOException => Exceptions.printStackTrace(ex)}

      info = javaControllers(0)
      //scalaFileToJavaCompilationInfo.put(fo, new WeakReference<CompilationInfo>(info));
    }

    info
  }

  /**
   * @Note: We cannot create javasource via JavaSource.forFileObject(fo) here, which
   * does not support virtual source yet (only ".java" and ".class" files
   * are supported), but we can create js via JavaSource.create(cpInfo);
   *
   * @Note: Do not cache javaSource, becuase javaSource here doesn't binding to fo,
   * so, when fo is changed, there won't be any respond for related CompilationInfo,
   * and ElementHandle#resolve(info) in JavaIndex won't refect to the any file changes
   * under classpaths
   */
  private def getJavaSourceForScalaFile(fo: FileObject): JavaSource = {
    val sourceRef = scalaFileToJavaSource.get(fo)
    var source = if (sourceRef != null) sourceRef.get else null

    if (source == null) {
      val javaCpInfo = ClasspathInfo.create(fo)
      source = JavaSource.create(javaCpInfo)
      //scalaFileToJavaSource.put(fo, new WeakReference<JavaSource>(source));
    }

    source
  }

  @throws(classOf[IOException])
  def getDocComment(info: CompilationInfo, e: Element): String = {
    // to resolve javadoc, only needs Phase.ELEMENT_RESOLVED, and we have reached when create info
    info.getElementUtilities.javaDocFor(e) match {
      case null => ""
      case javaDoc => javaDoc.getRawCommentText
    }
  }

  /**
   * Get fileobject that is origin source of element from current comilationInfo
   */
  def getOriginFileObject(info: CompilationInfo, e: Element): Option[FileObject] = {
    val handle = ElementHandle.create(e)
    SourceUtils.getFile(handle, info.getClasspathInfo) match {
      case null => None
      case x => Some(x)
    }
  }

  @throws(classOf[IOException])
  def getOffset(info: CompilationInfo, e: Element): Int = {
    val offset = Array(-1)

    val originFo = getOriginFileObject(info, e).getOrElse(return -1)

    /** @Note
     * We should create a element handle and a new CompilationInfo, then resolve
     * a new element from this hanlde and info
     */
    val handle = ElementHandle.create(e)
    val source = JavaSource.forFileObject(originFo)
    /**
     * @Note
     * Removed if (JavaSourceAccessor.getINSTANCE().isDispatchThread()) {}
     * as the parsing lock is reentrant.
     */
    try {
      source.runUserActionTask(new Task[CompilationController]() {
          @throws(classOf[Exception])
          def run(controller: CompilationController) {
            controller.toPhase(PHASE_RESOLVED)

            val el = handle.resolve(controller)
            val v = new FindDeclarationVisitor(el, controller)

            val cu = controller.getCompilationUnit

            v.scan(cu, null)
            val elTree = v.declTree

            if (elTree != null) {
              offset(0) = controller.getTrees.getSourcePositions.getStartPosition(cu, elTree).toInt
            }
          }
        }, true)
    } catch {case ex: IOException => Exceptions.printStackTrace(ex)}

    offset(0)
  }

  def getJavaElement(info: CompilationInfo, sym: Symbols#Symbol): Option[Element] = {
    val theElements = info.getElements
    val sName = sym.nameString
    val typeQName = sym.enclClass.fullName
    val te: TypeElement = theElements.getTypeElement(typeQName) match {
      case null => null
      case namedTe =>
        if (sym.isClass || sym.isTrait || sym.isModule) {
          return Some(namedTe)
        }
        ElementHandle.create(namedTe).resolve(info)
    }

    if (te == null) {
      return None
    }
    
    val itr = te.getEnclosedElements.iterator
    while (itr.hasNext) {
      val element = itr.next
      if (element.getSimpleName.toString.equals(sName)) {
        element.getKind match {
          case ElementKind.METHOD => element match {
              case ee: ExecutableElement =>
                val params1 = ee.getParameters
                val params2 = try {
                  sym.tpe.paramTypes
                } catch {case _ => /**@todo reset global */ Nil}
                if (params1.size == params2.size) {
                  var i = 0
                  for (param2 <- params2) {
                    val param1 = params1.get(i).asType
                    i += 1
                    // @todo compare param's type, should convert primary type between Java and Scala
                  }
                  return Some(element)
                }
              case _ =>
            }
          case ElementKind.FIELD => return Some(element)
          case _ =>
        }
      }
    }

    None
  }

  // Private innerclasses ----------------------------------------------------
  private class FindDeclarationVisitor(element: Element, info: CompilationInfo) extends TreePathScanner[Void, Void] {

    var declTree: Tree = _

    override def visitClass(tree: ClassTree, d: Void): Void = {
      handleDeclaration
      super.visitClass(tree, d)
      null
    }

    override def visitMethod(tree: MethodTree, d: Void): Void = {
      handleDeclaration
      super.visitMethod(tree, d)
      null
    }

    override def visitVariable(tree: VariableTree, d: Void): Void = {
      handleDeclaration
      super.visitVariable(tree, d)
      null
    }

    def handleDeclaration {
      val found = info.getTrees.getElement(getCurrentPath)
      if (element.equals(found)) {
        declTree = getCurrentPath.getLeaf
      }
    }
  }
}
