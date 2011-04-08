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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
import javax.swing.event.ChangeListener;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.source.TreePathHandle;
import org.netbeans.modules.refactoring.api.AbstractRefactoring;
import org.netbeans.modules.refactoring.api.MoveRefactoring;
import org.netbeans.modules.refactoring.api.Problem;
import org.netbeans.modules.refactoring.spi.ui.CustomRefactoringPanel;
import org.netbeans.modules.refactoring.spi.ui.RefactoringUI;
import org.netbeans.modules.refactoring.spi.ui.RefactoringUIBypass;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.URLMapper;
import org.openide.loaders.DataObject;
import org.openide.util.Exceptions;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.datatransfer.PasteType;
import org.openide.util.lookup.Lookups;

import org.netbeans.modules.scala.refactoring.RetoucheUtils

class MoveClassUI(javaObject: DataObject, targetFolder: FileObject, pasteType: PasteType, handles: Seq[TreePathHandle]
) extends RefactoringUI with RefactoringUIBypass {
    
  private var panel: MoveClassPanel = _
  private var targetPkgName: String = ""
  private val disable = targetFolder != null
  private val refactoring = new MoveRefactoring(Lookups.fixed(javaObject.getPrimaryFile, handles.toArray))
  refactoring.getContext.add(RetoucheUtils.getClasspathInfoFor(Array(javaObject.getPrimaryFile)))
    
  final def getString(key: String): String = {
    NbBundle.getMessage(classOf[MoveClassUI], key)
  }
    
  def this(javaObject: DataObject) = {
    this(javaObject, null, null, Nil)
  }
    
    
  override def getName: String = {
    getString ("LBL_MoveClass")
  }
     
  def getDescription: String = {
    new MessageFormat(getString("DSC_MoveClass")).format(
      Array(javaObject.getName, packageName).asInstanceOf[Array[Object]]
    )
  }
    
  def isQuery: Boolean = {
    false
  }
        
  def getPanel(parent: ChangeListener): CustomRefactoringPanel = {
    if (panel == null) {
      val pkgName = if (targetFolder != null) getPackageName(targetFolder) else getPackageName(javaObject.getPrimaryFile.getParent)
      panel = new MoveClassPanel (parent, pkgName,
                                  new MessageFormat(getString("LBL_MoveClassNamed")).format (
          Array(javaObject.getPrimaryFile.getName).asInstanceOf[Array[Object]]),
                                  if (targetFolder != null) targetFolder else (if (javaObject != null) javaObject.getPrimaryFile else null)
      )
      panel.setCombosEnabled(!disable)
    }
    panel
  }
    
  private def getPackageName(file: FileObject): String = {
    val cp = ClassPath.getClassPath(file, ClassPath.SOURCE)
    cp.getResourceName(file, '.', false)
  }

  private def packageName: String = {
    if (targetPkgName.trim.length == 0) getString ("LBL_DefaultPackage") else targetPkgName.trim
  }
    
  private def setParameters(checkOnly: Boolean): Problem = {
    if (panel==null)
      return null;
    targetPkgName = panel.getPackageName

    val url = URLMapper.findURL(panel.getRootFolder, URLMapper.EXTERNAL)
    try {
      refactoring.setTarget(Lookups.singleton(new URL(url.toExternalForm + URLEncoder.encode(panel.getPackageName.replace('.','/'), "utf-8")))) // NOI18N
    } catch {
      case ex: UnsupportedEncodingException => Exceptions.printStackTrace(ex)
      case ex: MalformedURLException => Exceptions.printStackTrace(ex)
    }
    if (checkOnly) {
      refactoring.fastCheckParameters
    } else {
      refactoring.checkParameters
    }
  }
    
  def checkParameters: Problem = {
    setParameters(true)
  }
    
  def setParameters: Problem = {
    setParameters(false)
  }
    
  def getRefactoring: AbstractRefactoring = {
    refactoring
  }
    
  def hasParameters: Boolean = {
    return true;
  }
    
  def getHelpCtx: HelpCtx = {
    new HelpCtx(classOf[MoveClassUI])
  }

  def isRefactoringBypassRequired: Boolean = {
    !panel.isUpdateReferences
  }

  @throws(classOf[IOException])
  def doRefactoringBypass {
    pasteType.paste
  }
}
