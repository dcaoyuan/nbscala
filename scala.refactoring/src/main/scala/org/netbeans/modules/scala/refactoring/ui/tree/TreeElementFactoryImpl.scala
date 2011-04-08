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

package org.netbeans.modules.scala.refactoring.ui.tree

import java.util.Map
import org.netbeans.api.project.Project
import org.netbeans.api.project.SourceGroup
import org.netbeans.modules.refactoring.api.RefactoringElement
import org.netbeans.modules.refactoring.spi.ui.TreeElement
import org.netbeans.modules.refactoring.spi.ui.TreeElementFactoryImplementation
import org.openide.filesystems.FileObject
import scala.collection.mutable.WeakHashMap

/**
 *
 * @author Jan Becicka
 */
object TreeElementFactoryImpl {
  var instance: TreeElementFactoryImpl = _
}
@org.openide.util.lookup.ServiceProvider(service = classOf[TreeElementFactoryImplementation], position=100)
class TreeElementFactoryImpl extends TreeElementFactoryImplementation {

  private val map = new WeakHashMap[Object, TreeElement]

  TreeElementFactoryImpl.instance = this
  
  override def getTreeElement(o: Object): TreeElement = {
    val result = o match {
      case x: SourceGroup => map.get(x.getRootFolder)
      case _ => map.get(o)
    }
    
    if (result.isDefined) {
      return result.get
    }

    val r = o match {
      case fo: FileObject =>
        if (fo.isFolder) {
// No package/directory related refactoring for Ruby
//                SourceGroup sg = FolderTreeElement.getSourceGroup(fo);
//                if (sg!=null && fo.equals(sg.getRootFolder()))
//                    result = new SourceGroupTreeElement(sg);
//                else
//                    result = new FolderTreeElement(fo);
          null
        } else {
          new FileTreeElement(fo)
        }
      case x: SourceGroup =>
        new SourceGroupTreeElement(x)
      case x: ElementGrip =>
        new ElementGripTreeElement(x)
      case x: Project =>
        new ProjectTreeElement(x)
      case x: RefactoringElement =>
        x.getLookup.lookup(classOf[ElementGrip]) match {
          case null => null
          case grip => new RefactoringTreeElement(x)
        }
    }

    if (r != null) {
      o match {
        case x: SourceGroup => map.put(x.getRootFolder, r)
        case _ => map.put(o, r)
      }
    }
    
    r
  }

  override def cleanUp {
    map.clear
    ElementGripFactory.getDefault.cleanUp
  }
}
