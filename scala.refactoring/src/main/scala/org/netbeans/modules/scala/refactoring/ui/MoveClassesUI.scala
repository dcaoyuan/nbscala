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

import java.awt.BorderLayout;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.logging.Logger;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeListener;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.queries.VisibilityQuery;
import org.netbeans.modules.refactoring.api.AbstractRefactoring;
import org.netbeans.modules.refactoring.api.MoveRefactoring;
import org.netbeans.modules.refactoring.api.Problem;
import org.netbeans.modules.refactoring.spi.ui.CustomRefactoringPanel;
import org.netbeans.modules.refactoring.spi.ui.RefactoringUI;
import org.netbeans.modules.refactoring.spi.ui.RefactoringUIBypass;
import org.openide.awt.Mnemonics;
import org.openide.explorer.view.NodeRenderer
import org.openide.filesystems.FileObject;
import org.openide.filesystems.URLMapper;
import org.openide.loaders.DataFolder;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.datatransfer.PasteType;
import org.openide.util.lookup.Lookups;

import org.netbeans.modules.scala.refactoring.RetoucheUtils;

/**
 * @author Jan Becicka
 */
class MoveClassesUI(javaObjects: Set[FileObject], targetFolder: FileObject, pasteType: PasteType
) extends RefactoringUI with RefactoringUIBypass {
    
  private var panel: MovePanel = _
  private var refactoring: MoveRefactoring = _
  private var targetPkgName = ""
  val isDisable = targetFolder != null
  private val resources = if (!isDisable) {
    javaObjects
  } else Set()

    
  final def getString(key: String): String = {
    NbBundle.getMessage(classOf[MoveClassUI], key)
  }
    
  def this(javaObjects: Set[FileObject]) = {
    this(javaObjects, null, null)
  }

  def getName: String = {
    getString("LBL_MoveClasses")
  }
     
  def getDescription: String = {
    getName
  }
    
  def isQuery: Boolean = {
    false
  }
        
  def getPanel(parent: ChangeListener): CustomRefactoringPanel = {
    if (panel == null) {
      var pkgName: String = null
      if (targetFolder != null) {
        val cp = ClassPath.getClassPath(targetFolder, ClassPath.SOURCE)
        if (cp != null)
          pkgName = cp.getResourceName(targetFolder, '.', false)
      }
      panel = new MovePanel (parent,
                             if (pkgName != null) pkgName else getDOPackageName(javaObjects.iterator.next.asInstanceOf[FileObject].getParent),
                             getString("LBL_MoveClassesHeadline")
      );
    }
    panel;
  }
    
//    private static String getResPackageName(Resource res) {
//        String name = res.getName();
//        if ( name.indexOf('/') == -1 )
//            return "";
//        return name.substring(0, name.lastIndexOf('/')).replace('/','.');
//    }
  private def getDOPackageName(f: FileObject): String = {
    val cp = ClassPath.getClassPath(f, ClassPath.SOURCE);
    if (cp != null) {
      cp.getResourceName(f, '.', false);
    } else {
      Logger.getLogger("org.netbeans.modules.scala.refactoring").info("Cannot find classpath for " + f.getPath());
      f.getName
    }
  }

  private def packageName: String = {
    if (targetPkgName.trim.length == 0) getString ("LBL_DefaultPackage") else targetPkgName.trim
  }
    
  private def setParameters(checkOnly: Boolean): Problem = {
    if (panel==null) return null
      
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
    if (refactoring == null) {
      if (isDisable) {
        refactoring = new MoveRefactoring(Lookups.fixed(javaObjects.toArray.asInstanceOf[Array[java.lang.Object]]: _*))
        refactoring.getContext().add(RetoucheUtils.getClasspathInfoFor(javaObjects.toArray))
      } else {
        refactoring = new MoveRefactoring (Lookups.fixed(resources.toArray.asInstanceOf[Array[java.lang.Object]]: _*))
        refactoring.getContext().add(RetoucheUtils.getClasspathInfoFor(resources.toArray))
      }
    }
    refactoring
  }

  private final def getNodes: java.util.Vector[Node] = {
    val result = new java.util.Vector[Node](javaObjects.size)
    var q: List[FileObject] = Nil
    while (!q.isEmpty) {
      val f = q.head
      q = q.tail
      if (VisibilityQuery.getDefault.isVisible(f)) {
        val d = try {
          DataObject.find(f)
        } catch {case ex: DataObjectNotFoundException => ex.printStackTrace; null}

        d match {
          case null =>
          case df: DataFolder =>
            for (o <- df.getChildren) {
              q = o.getPrimaryFile :: q
            }
          case _ => result.add(d.getNodeDelegate)
        }
      }
    }
    result
  }
 
  def hasParameters: Boolean = {
    true;
  }
    
  def getHelpCtx: HelpCtx = {
    new HelpCtx(classOf[MoveClassesUI])
  }

  def isRefactoringBypassRequired: Boolean = {
    !panel.isUpdateReferences
  }

  @throws(classOf[IOException])
  def doRefactoringBypass {
    pasteType.paste
  }

  // MovePanel ...............................................................
  class MovePanel(parent: ChangeListener, startPackage: String, headLine: String
  ) extends MoveClassPanel(parent, startPackage, headLine, if (targetFolder != null) targetFolder else javaObjects.iterator.next) {
    setCombosEnabled(!isDisable)
    val nodelist = new JList(getNodes)
    nodelist.setCellRenderer(new NodeRenderer)
    nodelist.setVisibleRowCount(5)
    val pane = new JScrollPane(nodelist)
    bottomPanel.setBorder(new EmptyBorder(8,0,0,0))
    bottomPanel.setLayout(new BorderLayout());
    bottomPanel.add(pane, BorderLayout.CENTER);
    val listOf = new JLabel
    Mnemonics.setLocalizedText(listOf, NbBundle.getMessage(classOf[MoveClassesUI], "LBL_ListOfClasses"))
    bottomPanel.add(listOf, BorderLayout.NORTH)
  }
}
