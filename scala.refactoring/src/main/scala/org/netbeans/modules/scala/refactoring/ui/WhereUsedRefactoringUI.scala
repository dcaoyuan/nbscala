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
package org.netbeans.modules.scala.refactoring.ui

import java.text.MessageFormat
import java.util.ResourceBundle
import javax.swing.event.ChangeListener
import org.netbeans.api.java.source.ClasspathInfo
import org.netbeans.api.project.FileOwnerQuery
import org.netbeans.api.project.ProjectUtils
import org.netbeans.modules.csl.api.ElementKind
import org.netbeans.modules.refactoring.api.AbstractRefactoring
import org.netbeans.modules.refactoring.api.Problem
import org.netbeans.modules.refactoring.api.WhereUsedQuery
import org.netbeans.modules.refactoring.spi.ui.CustomRefactoringPanel
import org.netbeans.modules.refactoring.spi.ui.RefactoringUI
import org.netbeans.spi.java.classpath.support.ClassPathSupport
import org.openide.filesystems.FileObject
import org.openide.util.HelpCtx
import org.openide.util.NbBundle
import org.openide.util.lookup.Lookups

import org.netbeans.modules.scala.core.ScalaSourceUtil
import org.netbeans.modules.scala.core.ast.ScalaItems
import org.netbeans.modules.scala.refactoring.RetoucheUtils
import org.netbeans.modules.scala.refactoring.WhereUsedQueryConstants
import scala.collection.mutable.HashSet
/**
 * WhereUsedQueryUI from the Java refactoring module, only moderately modified for Ruby
 * 
 * @author Martin Matula, Jan Becicka
 */
object WhereUsedRefactoringUI {
  def apply(searchHandle: ScalaItems#ScalaItem) = {
    val query = new WhereUsedQuery(Lookups.singleton(searchHandle))
    query.getContext.add(RetoucheUtils.getClasspathInfoFor(Array(searchHandle)))
    val name = searchHandle.symbol.nameString
    val kind = searchHandle.kind
    new WhereUsedRefactoringUI(query, name, kind, searchHandle, null)
  }

  def apply(searchHandle: ScalaItems#ScalaItem, name: String, delegate: AbstractRefactoring) = {
    new WhereUsedRefactoringUI(null, name, null, searchHandle, delegate)
  }
}

class WhereUsedRefactoringUI(query: WhereUsedQuery, name: String, kind: ElementKind, handle: ScalaItems#ScalaItem, delegate: AbstractRefactoring) extends RefactoringUI {
  private var panel: WhereUsedPanel = _

  def isQuery = true

  def getPanel(parent: ChangeListener): CustomRefactoringPanel = {
    if (panel == null) {
      panel = new WhereUsedPanel(name, handle, parent)
    }
    panel
  }

  def setParameters: Problem = {
    query.putValue(WhereUsedQuery.SEARCH_IN_COMMENTS, panel.isSearchInComments)

    if (panel.getScope == WhereUsedPanel.Scope.ALL) {
      if (kind == ElementKind.METHOD && panel.isMethodFromBaseClass) {
        val basem = panel.getBaseMethod
        if (basem != null && (basem.fo == None || basem.fo.get.getNameExt.endsWith("class"))) { //NOI18N
          query.getContext.add(RetoucheUtils.getClasspathInfoFor(Array(handle, basem)))
        } else {
          query.getContext.add(RetoucheUtils.getClasspathInfoFor(Array(basem)))
        }
      } else {
        query.getContext.add(RetoucheUtils.getClasspathInfoFor(Array(handle)))
      }
    } else {
      val cpInfo = query.getContext.lookup(classOf[ClasspathInfo])
      val p = FileOwnerQuery.getOwner(handle.fo.get)
      val roots = new HashSet[FileObject] ++= ScalaSourceUtil.getScalaJavaSourceGroups(p).map(_.getRootFolder)

      val srcCp = ClassPathSupport.createClassPath(roots.toArray: _*)
      val bootCp = cpInfo.getClassPath(ClasspathInfo.PathKind.BOOT)
      val compCp = cpInfo.getClassPath(ClasspathInfo.PathKind.COMPILE)
      query.getContext.add(ClasspathInfo.create(bootCp, compCp, srcCp))
    }

    kind match {
      case ElementKind.METHOD =>
        setForMethod
        query.checkParameters
      case ElementKind.MODULE | ElementKind.CLASS =>
        setForClass
        query.checkParameters
      case _ => null
    }
  }
    
  private def setForMethod {
    if (panel.isMethodFromBaseClass) {
      query.setRefactoringSource(Lookups.singleton(panel.getBaseMethod))
    } else {
      query.setRefactoringSource(Lookups.singleton(handle))
    }
    query.putValue(WhereUsedQueryConstants.FIND_OVERRIDING_METHODS, panel.isMethodOverriders)
    query.putValue(WhereUsedQuery.FIND_REFERENCES, panel.isMethodFindUsages)
  }
    
  private def setForClass {
    query.putValue(WhereUsedQueryConstants.FIND_SUBCLASSES, panel.isClassSubTypes)
    query.putValue(WhereUsedQueryConstants.FIND_DIRECT_SUBCLASSES, panel.isClassSubTypesDirectOnly)
    query.putValue(WhereUsedQuery.FIND_REFERENCES, panel.isClassFindUsages)
  }
    
  def checkParameters: Problem = {
    kind match {
      case ElementKind.METHOD =>
        setForMethod
        query.fastCheckParameters
      case ElementKind.CLASS | ElementKind.MODULE =>
        setForClass
        query.fastCheckParameters
      case _ => null
    } 
  }

  def getRefactoring: AbstractRefactoring = {
    if (query != null) query else delegate
  }

  def getDescription: String = {
    if (panel != null) {
      kind match {
        case ElementKind.MODULE | ElementKind.CLASS =>
          if (!panel.isClassFindUsages)
            if (!panel.isClassSubTypesDirectOnly) {
              return getString("DSC_WhereUsedFindAllSubTypes", name)
            } else {
              return getString("DSC_WhereUsedFindDirectSubTypes", name)
            }
        case ElementKind.METHOD =>
          var description: String = null
          if (panel.isMethodFindUsages) {
            description = getString("DSC_FindUsages")
          }
                    
          if (panel.isMethodOverriders) {
            if (description != null) {
              description += " " + getString("DSC_And") + " "
            } else {
              description = ""
            }
            description += getString("DSC_WhereUsedMethodOverriders")
          }
                    
          description += " " + getString("DSC_WhereUsedOf", panel.getMethodDeclaringClass + '.' + name) //NOI18N
          return description
        case _ => 
      }
    }
  
    getString("DSC_WhereUsed", name)
  }
    
  private var bundle: ResourceBundle = _
  private def getString(key: String): String = {
    if (bundle == null) {
      bundle = NbBundle.getBundle(classOf[WhereUsedRefactoringUI])
    }
    bundle.getString(key)
  }
    
  private def getString(key: String, value: String): String = {
    new MessageFormat(getString(key)).format(Array(value).asInstanceOf[Array[Object]])
  }

  def getName: String = {
    new MessageFormat(NbBundle.getMessage(classOf[WhereUsedPanel], "LBL_WhereUsed")).format(Array(name).asInstanceOf[Array[Object]])
  }
    
  def hasParameters: Boolean = {
    true
  }

  def getHelpCtx: HelpCtx = {
    null
  }
}
