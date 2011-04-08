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

package org.netbeans.modules.scala.editor.wizards

import java.awt.Component
import java.io.IOException
import java.{util=>ju}
import javax.swing.JComponent
import javax.swing.event.ChangeListener
import org.netbeans.api.java.project.JavaProjectConstants
import org.netbeans.api.project.{FileOwnerQuery, Project, ProjectUtils, SourceGroup, Sources}
import org.netbeans.spi.java.project.support.ui.templates.JavaTemplates
import org.netbeans.spi.project.ui.templates.support.Templates
import org.openide.WizardDescriptor
import org.openide.filesystems.FileObject
import org.openide.filesystems.FileUtil
import org.openide.loaders.DataFolder
import org.openide.loaders.DataObject
import org.openide.util.ChangeSupport

/**
 * Wizard to create a new Scala file.
 */


class ScalaFileWizardIterator extends WizardDescriptor.InstantiatingIterator[WizardDescriptor] {
    
    import ScalaFileUtil._
    var index : Int = 0
    var panels : Array[WizardDescriptor.Panel[WizardDescriptor]] = null
    var wiz : WizardDescriptor = null;
    
    def createPanels (wizardDescriptor : WizardDescriptor) : Array[WizardDescriptor.Panel[WizardDescriptor]] = {
        val project = Templates.getProject(wizardDescriptor)
        val sources = ProjectUtils.getSources(project)
        val groupList = getScalaSourceGroups(sources)
        val groups = groupList.toArray
        if (groups.length == 0) {
            Array(Templates.createSimpleTargetChooser(project, sources.getSourceGroups(Sources.TYPE_GENERIC)))
        } else {
            Array(JavaTemplates.createPackageChooser(project, groups))
        }
    }
    
    def createSteps(before : Array[String], panels : Array[WizardDescriptor.Panel[WizardDescriptor]]) : Array[String] = {
        // hack to use the steps set before this panel processed
        val diff = if (before.length > 0) {
              if ("...".equals (before(before.length - 1))) 1 else 0 //NOI18N
            } else 0

        val ret = for (i <- 0 until ((before.length - diff) + panels.length)) yield
                      if (i < (before.length - diff)) {
                          before(i)
                      } else {
                          panels(i - before.length + diff).getComponent().getName()
                      }
        ret.toArray
    }

    @throws(classOf[java.io.IOException])
    def instantiate() : ju.Set[FileObject] = {
        val dir = Templates.getTargetFolder(wiz)
        val targetName = Templates.getTargetName(wiz)
        
        val df = DataFolder.findFolder(dir)
        val template = Templates.getTemplate(wiz)
        
        val dTemplate = DataObject.find(template)
        val pkgName = getPackageName(dir)
        val dobj = pkgName match {
          case null => dTemplate.createFromTemplate(df, targetName)
          case _ => dTemplate.createFromTemplate(df, targetName, ju.Collections.singletonMap("package", pkgName)) // NOI18N
        }
        ju.Collections.singleton(dobj.getPrimaryFile())
    }
    
    def initialize(wiz : WizardDescriptor) : Unit = {
        this.wiz = wiz;
        index = 0;
        panels = createPanels( wiz );
        // Make sure list of steps is accurate.
        val beforeSteps = wiz.getProperty (WizardDescriptor.PROP_CONTENT_DATA) match {
            case s : Array[String] => s
            case _ => Array[String]()
        }
        val steps = createSteps (beforeSteps, panels)
        var i = 0
        for (p  <- panels) {
            p.getComponent() match {
              case c : JComponent => {
                c.putClientProperty(WizardDescriptor.PROP_CONTENT_SELECTED_INDEX, Integer.valueOf(i)); // NOI18N
                c.putClientProperty(WizardDescriptor.PROP_CONTENT_DATA, steps) // NOI18N
              }
              case _ =>
            }
        }
    }

    def uninitialize(wiz : WizardDescriptor) : Unit = {
        this.wiz = null
        panels = null
    }
    
    def name() = "" //NOI18N
    
    def hasNext() = index < panels.length - 1
    def hasPrevious() = index > 0
    def current() : WizardDescriptor.Panel[WizardDescriptor] = panels(index)
    def nextPanel() : Unit = {
        if (!hasNext()) throw new NoSuchElementException()
        index = index + 1
    }
    def previousPanel() : Unit = {
        if (!hasPrevious()) throw new NoSuchElementException()
        index = index - 1
    }
    
    val changeSupport = new ChangeSupport(this)
    def addChangeListener(l : ChangeListener ) : Unit = changeSupport.addChangeListener(l)
    def removeChangeListener(l : ChangeListener ) : Unit = changeSupport.removeChangeListener(l)
    protected def fireChangeEvent() : Unit = changeSupport.fireChange()
}


object ScalaFileUtil {
    def create() : WizardDescriptor.InstantiatingIterator[WizardDescriptor] = {
      new ScalaFileWizardIterator()
    }

    def getPackageName(targetFolder : FileObject) : String =  {
        val project = FileOwnerQuery.getOwner(targetFolder)
        val sources = ProjectUtils.getSources(project)
        val groups = getScalaSourceGroups(sources)
        var packageName : String = null
        if (groups.exists( (gr) => {
            packageName = FileUtil.getRelativePath(gr.getRootFolder(), targetFolder)
            packageName != null
          })) 
        {
          packageName.replaceAll("/", ".") // NOI18N
        } else {
          null
        }
    }

    def getScalaSourceGroups(sources : Sources ) :  Array[SourceGroup] = {
        Array[SourceGroup]() ++
            sources.getSourceGroups("scala") ++
            sources.getSourceGroups(JavaProjectConstants.SOURCES_TYPE_JAVA)
    }

}
