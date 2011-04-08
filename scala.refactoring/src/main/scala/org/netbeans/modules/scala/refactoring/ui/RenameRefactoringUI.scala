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

import java.io.IOException
import java.text.MessageFormat
import javax.swing.event.ChangeListener
import org.netbeans.api.fileinfo.NonRecursiveFolder
import org.netbeans.modules.csl.api.ElementKind
import org.netbeans.modules.refactoring.api.AbstractRefactoring
import org.netbeans.modules.refactoring.api.RenameRefactoring
import org.netbeans.modules.refactoring.api.Problem
import org.netbeans.modules.refactoring.spi.ui.CustomRefactoringPanel
import org.netbeans.modules.refactoring.spi.ui.RefactoringUI
import org.netbeans.modules.refactoring.spi.ui.RefactoringUIBypass
import org.netbeans.modules.refactoring.spi.ui.UI
import org.openide.filesystems.FileObject
import org.openide.loaders.DataFolder
import org.openide.loaders.DataObject
import org.openide.util.HelpCtx
import org.openide.util.NbBundle
import org.openide.util.lookup.Lookups

import org.netbeans.modules.scala.core.ast.ScalaItems
import org.netbeans.modules.scala.refactoring.RetoucheUtils

/**
 *
 * @todo There are a lot of constructors here; figure out which ones are unused, and
 *   nuke them!
 * 
 * @author Martin Matula, Jan Becicka
 */
object RenameRefactoringUI {
  def apply(file: FileObject, newName: String, handle: ScalaItems#ScalaItem) = {
    val refactoring = if (handle != null) {
      new RenameRefactoring(Lookups.fixed(file, handle))
    } else {
      new RenameRefactoring(Lookups.fixed(file))
    }
    val oldName = newName
    //[FIXME] this should be oldName of refactored object
    val dispOldName = newName
    val stripPrefix = null
    val pkgRename = true
    val fromListener = true

    // Force refresh!
    refactoring.getContext.add(true)

    new RenameRefactoringUI(refactoring, handle, oldName, dispOldName, stripPrefix, pkgRename, fromListener)
  }

  def apply(handle: ScalaItems#ScalaItem) = {
    val refactoring = new RenameRefactoring(Lookups.singleton(handle))
    val oldName = handle.symbol.nameString
    val dispOldName = oldName
    val stripPrefix = null
    val pkgRename = true
    val fromListener = false

    refactoring.getContext.add(UI.Constants.REQUEST_PREVIEW)

    new RenameRefactoringUI(refactoring, handle, oldName, dispOldName, stripPrefix, pkgRename, fromListener)
  }

  def apply(file: FileObject, handle: ScalaItems#ScalaItem) = {
    val refactoring = if (handle != null) {
      new RenameRefactoring(Lookups.fixed(file, handle))
    } else {
      new RenameRefactoring(Lookups.fixed(file))
    }
    val oldName = if (handle != null) handle.symbol.nameString else file.getName
    val dispOldName = oldName
    val stripPrefix = null
    val pkgRename = true
    val fromListener = false

    // Force refresh!
    refactoring.getContext.add(UI.Constants.REQUEST_PREVIEW)

    new RenameRefactoringUI(refactoring, handle, oldName, dispOldName, stripPrefix, pkgRename, fromListener)
  }

  def apply(file: NonRecursiveFolder) = {
    val handle = null
    val refactoring = new RenameRefactoring(Lookups.singleton(file))
    val oldName = RetoucheUtils.getPackageName(file.getFolder)
    val dispOldName = oldName
    val stripPrefix = null
    val pkgRename = true
    val fromListener = false

    // Force refresh!
    refactoring.getContext.add(UI.Constants.REQUEST_PREVIEW)

    new RenameRefactoringUI(refactoring, handle, oldName, dispOldName, stripPrefix, pkgRename, fromListener)
  }

  def apply(file: NonRecursiveFolder, newName: String) = {
    val handle = null
    val refactoring = new RenameRefactoring(Lookups.singleton(file))
    val oldName = newName
    //[FIXME] this should be oldName of refactored object
    val dispOldName = newName
    val stripPrefix = null
    val fromListener = true
    val pkgRename = true

    // Force refresh!
    refactoring.getContext.add(UI.Constants.REQUEST_PREVIEW)
    new RenameRefactoringUI(refactoring, handle, oldName, dispOldName, stripPrefix, pkgRename, fromListener)
  }

  private def getString(key: String): String = {
    NbBundle.getMessage(classOf[RenameRefactoringUI], key)
  }


}

class RenameRefactoringUI(refactoring: AbstractRefactoring,
                          handle: ScalaItems#ScalaItem,
                          oldName: String,
                          dispOldName: String,
                          stripPrefix: String,
                          pkgRename: Boolean,
                          fromListener: Boolean
) extends RefactoringUI with RefactoringUIBypass {
  import RenameRefactoringUI._

  private var newName: String = _
  private var panel: RenamePanel = _
  private var byPassFolder: FileObject = _
  private var byPassPakageRename: Boolean = _
    
  def isQuery = false

  def getPanel(parent: ChangeListener): CustomRefactoringPanel = {
    if (panel == null) {
      var name = oldName
      if (stripPrefix != null && name.startsWith(stripPrefix)) {
        name = name.substring(stripPrefix.length)
      }
            
      var suffix = if (handle != null) {
        handle.kind match {
          //if (kind.isClass() || kind.isInterface()) {
          case ElementKind.CLASS /* || kind == ElementKind.MODULE*/ =>
            /*kind.isInterface() ? getString("LBL_Interface") : */getString("LBL_Class")
          case ElementKind.METHOD =>
            getString("LBL_Method")
          case ElementKind.FIELD =>
            getString("LBL_Field")
          case ElementKind.VARIABLE =>
            getString("LBL_LocalVar")
          case ElementKind.MODULE /*(jmiObject == null && fromListener)*/ =>
            if (pkgRename) getString("LBL_Package") else getString("LBL_Folder")
          case ElementKind.PARAMETER =>
            getString("LBL_Parameter")
          case _ => ""
        }
      } else ""
      suffix = suffix + " " + name // NOI18N
            
      panel = new RenamePanel(name, parent, NbBundle.getMessage(classOf[RenamePanel], "LBL_Rename") + " " + suffix, !fromListener, fromListener && !byPassPakageRename)
    }

    panel
  }
    
  private def getPanelName: String = {
    var name = panel.getNameValue
    if (stripPrefix != null && !name.startsWith(stripPrefix)) {
      name = stripPrefix + name
    }
        
    name
  }

  def setParameters: Problem = {
    newName = getPanelName
    refactoring match {
      case x: RenameRefactoring =>
        x.setNewName(newName)
        x.setSearchInComments(panel.searchJavadoc)
      case _ => // MoveClassRefactoring etc
    }
    
    refactoring.checkParameters
  }
    
  def checkParameters: Problem = {
    if (!panel.isUpdateReferences) return null
    newName = getPanelName
    refactoring match {
      case x: RenameRefactoring =>
        x.setNewName(newName)
      case _ => // MoveClassRefactoring etc
    }

    refactoring.fastCheckParameters
  }

  def getRefactoring: AbstractRefactoring = {
    refactoring
  }

  def getDescription: String = {
    new MessageFormat(NbBundle.getMessage(classOf[RenamePanel], "DSC_Rename")).format(Array(dispOldName, newName).asInstanceOf[Array[Object]])
  }

  def getName: String = {
    NbBundle.getMessage(classOf[RenamePanel], "LBL_Rename")
  }

  def hasParameters: Boolean = {
    true
  }

  def getHelpCtx: HelpCtx = {
    null
  }
    
  def isRefactoringBypassRequired: Boolean =  {
    !panel.isUpdateReferences
  }

  @throws(classOf[IOException])
  def doRefactoringBypass {
    val dob = if (byPassFolder != null) {
      DataFolder.findFolder(byPassFolder);
    } else {
      DataObject.find(refactoring.getRefactoringSource.lookup(classOf[FileObject]))
    }
    dob.rename(getPanelName)
  }
}
