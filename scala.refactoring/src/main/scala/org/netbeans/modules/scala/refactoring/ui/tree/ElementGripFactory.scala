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

import javax.swing.Icon;
import org.netbeans.modules.csl.api.OffsetRange;
import org.openide.filesystems.FileObject;
import scala.collection.mutable.HashSet
import scala.collection.mutable.WeakHashMap

/**
 *
 * Based on the Java refactoring one, but hacked for Ruby (plus I didn't fully understand
 * what this class was for so it probably needs some cleanup and some work)
 * 
 * @author Jan Becicka
 * @author Tor Norbye
 */
object ElementGripFactory {
  private var instance: ElementGripFactory = _
  def getDefault: ElementGripFactory = {
    if (instance == null) {
      instance = new ElementGripFactory
    }
    instance
  }
}
class ElementGripFactory {

  private val map = new WeakHashMap[FileObject, Interval]
    
  def cleanUp {
    map.clear
  }
    
  def get(fileObject: FileObject, position: Int): ElementGrip = {
    val start = map.get(fileObject).getOrElse(return null)
    try {
      return start.get(position).item
    } catch {case ex: RuntimeException => return start.item}
  }
    
  def getParent(el: ElementGrip): ElementGrip =  {
    val start = map.get(el.fileObject).get
    start.getParent(el)
  }

  def put(parentFile: FileObject, name: String, range: OffsetRange, icon: Icon) {
    val root = map.get(parentFile).getOrElse(null)
    val i = Interval(range, name, icon, root, null, parentFile)
    if (i != null) {
      map.put(parentFile, i)
    }
  }


  private object Interval {
    // TODO - figure out what is intended here!?
    def apply(range: OffsetRange, name: String, icon: Icon,
              root: Interval, p: Interval, parentFile: FileObject) = {
      //Tree t = tp.getLeaf();
      //long start = info.getTrees().getSourcePositions().getStartPosition(info.getCompilationUnit(), t);
      //long end = info.getTrees().getSourcePositions().getEndPosition(info.getCompilationUnit(), t);
      val start = range.getStart
      val end = range.getEnd
//                Element current = info.getTrees().getElement(tp);
//                Tree.Kind kind = tp.getLeaf().getKind();
//                if (kind != Tree.Kind.CLASS && kind != Tree.Kind.METHOD) {
//                    if (tp.getParentPath().getLeaf().getKind() == Tree.Kind.COMPILATION_UNIT) {
//                        //xxx: rather workaround. should be fixed better.
//                        return null;
//                    } else {
//                        return createInterval(tp.getParentPath(), info, root, p, parentFile);
//                    }
//                }
      var i: Interval = null
//                if (root != null) {
//                    Interval o = root.get(start);
//                    if (o!= null && o.item.resolveElement(info).equals(current)) {
//                        if (p!=null)
//                            o.subintervals.add(p);
//                        return null;
//                    }
//                }
      if (i==null)
        i = new Interval
      if (i.from != start) {
        i.from = start
        i.to = end
        val currentHandle2 = new ElementGrip(name, parentFile, icon)
        i.item = currentHandle2
      }
      if (p!=null) {
        i.subintervals.add(p)
      }
//                if (tp.getParentPath().getLeaf().getKind() == Tree.Kind.COMPILATION_UNIT) {
      i
//                }
//                return createInterval(tp.getParentPath(), info, root, i, parentFile);
//            }
    }

  }
  private class Interval {
    var from: Long = -1
    var to: Long = -1
    val subintervals= new HashSet[Interval]
    var item: ElementGrip = null
        
    def get(position: Long): Interval = {
      if (from <= position && to >= position) {
        for (o <- subintervals) {
          val ob = o.get(position)
          if (ob != null)
            return ob
        }
        return this
      }

      null
    }
        
    def getParent(eh: ElementGrip): ElementGrip = {
      for (i <- subintervals) {
        if (i.item.equals(eh)) {
          return this.item;
        } else {
          val e = i.getParent(eh)
          if (e != null) {
            return e
          }
        }
      }
      
      null
    }
        
  }
}
    
