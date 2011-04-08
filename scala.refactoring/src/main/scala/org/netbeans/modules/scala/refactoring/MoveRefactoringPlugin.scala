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
package org.netbeans.modules.scala.refactoring

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;
import org.netbeans.api.fileinfo.NonRecursiveFolder;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.queries.VisibilityQuery;
import org.netbeans.modules.csl.spi.support.ModificationResult
import org.netbeans.modules.refactoring.api.AbstractRefactoring
import org.netbeans.modules.refactoring.api.MoveRefactoring
import org.netbeans.modules.refactoring.api.Problem
import org.netbeans.modules.refactoring.api.ProgressEvent
import org.netbeans.modules.refactoring.api.RenameRefactoring
import org.netbeans.modules.refactoring.spi.RefactoringElementsBag;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.filesystems.URLMapper;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet
import org.netbeans.modules.scala.core.ScalaParserResult
import org.netbeans.modules.scala.core.ScalaSourceUtil
import org.netbeans.modules.scala.core.ast.ScalaItems

/**
 * Implemented abilities:
 * <ul>
 * <li>Move file(s)</li>
 * <li>Move folder(s)</li>
 * <li>Rename folder</li>
 * <li>Rename package</li>
 * </ul>
 */
class MoveRefactoringPlugin(refactoring: AbstractRefactoring) extends ScalaRefactoringPlugin {

  val isRenameRefactoring = refactoring match {
    case _ : MoveRefactoring =>
      val files = refactoring.getRefactoringSource.lookupAll(classOf[FileObject])
      setup(files.toArray.asInstanceOf[Array[FileObject]], "", true)
      
      false
    case _ : RenameRefactoring =>
      val fo = refactoring.getRefactoringSource.lookup(classOf[FileObject])
      if (fo != null) {
        setup(List(fo), "", true)
      } else {
        setup(List(refactoring.getRefactoringSource.lookup(classOf[NonRecursiveFolder]).getFolder), "", false) // NOI18N
      }
      
      true
  }

  private var packagePostfix = new HashMap[FileObject, String]
  val filesToMove = new ArrayBuffer[FileObject]
  var classes: HashMap[FileObject, ScalaItems#ScalaItem] = _
  /** list of folders grouped by source roots */
  val foldersToMove = new ArrayBuffer[ArrayBuffer[FileObject]]
  /** collection of packages that will change its name */
  var packages = new HashSet[String]
  val whoReferences = new HashMap[FileObject, Set[FileObject]]
    

  private def setup(fileObjects: Seq[FileObject], postfix: String, recursively: Boolean, sameRootList: ArrayBuffer[FileObject] = null) {
    val itr = fileObjects.iterator
    while (itr.hasNext) {
      val fo = itr.next
      if (RetoucheUtils.isScalaFile(fo)) {
        packagePostfix.put(fo, postfix.replace('/', '.'))
        filesToMove += fo
      } else if (!(fo.isFolder)) {
        packagePostfix.put(fo, postfix.replace('/', '.'))
      } else if (VisibilityQuery.getDefault.isVisible(fo)) {
        //o instanceof DataFolder
        //CVS folders are ignored
        val addDot = postfix != ""
        val col = new ArrayBuffer[FileObject]
        for (fo2 <- fo.getChildren) {
          if (!fo2.isFolder || (fo2.isFolder && recursively))
            col += fo2
        }
        val curRootList = sameRootList match {
          case null =>
            val x = new ArrayBuffer[FileObject]
            foldersToMove += x
            x
          case x => x
        }

        curRootList += fo
        setup(col,
              postfix + (if (addDot) "." else "") + fo.getName, // NOI18N
              recursively,
              curRootList)
      }
    }
  }

  override def preCheck: Problem = {
    var preCheckProblem: Problem = null
    for (file <- filesToMove) {
      if (!RetoucheUtils.isElementInOpenProject(file)) {
        preCheckProblem = createProblem(preCheckProblem, true, NbBundle.getMessage(
            classOf[MoveRefactoringPlugin],
            "ERR_ProjectNotOpened",
            FileUtil.getFileDisplayName(file)))
      }
    }
    preCheckProblem
  }

  override def checkParameters: Problem = {
    null
  }

  override def fastCheckParameters: Problem = {
    if (isRenameRefactoring) {
      //folder rename
      val f = refactoring.getRefactoringSource.lookup(classOf[FileObject])
      if (f != null) {
        val newName = refactoring.asInstanceOf[RenameRefactoring].getNewName
        if (!RetoucheUtils.isValidPackageName(newName)) {
          val msg = new MessageFormat(NbBundle.getMessage(classOf[RenameRefactoringPlugin], "ERR_InvalidPackage")).format(
            Array(newName).asInstanceOf[Array[Object]]
          )
          return new Problem(true, msg)
        }
                
        if (f.getParent.getFileObject(newName, f.getExt) != null) {
          val msg = new MessageFormat(NbBundle.getMessage(classOf[RenameRefactoringPlugin],"ERR_PackageExists")).format(
            Array(newName).asInstanceOf[Array[Object]]
          )
          return new Problem(true, msg)
        }
      }
      return null //super.fastCheckParameters
    }
    if (!isRenameRefactoring) {
      try {
        for (f<- filesToMove) {
          if (RetoucheUtils.isScalaFile(f)) {
            val targetPackageName = this.getTargetPackageName(f)
            if (!RetoucheUtils.isValidPackageName(targetPackageName)) {
              val s = NbBundle.getMessage(classOf[RenameRefactoringPlugin], "ERR_InvalidPackage") //NOI18N
              val msg = new MessageFormat(s).format(
                Array(targetPackageName).asInstanceOf[Array[Object]]
              );
              return new Problem(true, msg);
            }
            val targetRoot = RetoucheUtils.getClassPathRoot(refactoring.asInstanceOf[MoveRefactoring].getTarget.lookup(classOf[URL]))
            val targetF = targetRoot.getFileObject(targetPackageName.replace('.', '/'));
                    
            if ((targetF!=null && !targetF.canWrite())) {
              return new Problem(true, new MessageFormat(NbBundle.getMessage(classOf[MoveRefactoringPlugin], "ERR_PackageIsReadOnly")).format( // NOI18N
                  Array(targetPackageName).asInstanceOf[Array[Object]]
                ))
            }
                    
            //                this.movingToDefaultPackageMap.put(r, Boolean.valueOf(targetF!= null && targetF.equals(classPath.findOwnerRoot(targetF))));
            var pkgName = targetPackageName;
                    
            if (pkgName == null) {
              pkgName = "" // NOI18N
            } else if (pkgName.length > 0) {
              pkgName = pkgName + '.'
            }
            //targetPrefix = pkgName;
                    
            //                JavaClass[] sourceClasses = (JavaClass[]) sourceClassesMap.get(r);
            //                String[] names = new String [sourceClasses.length];
            //                for (int x = 0; x < names.length; x++) {
            //                    names [x] = sourceClasses [x].getName();
            //                }
            //
            //                FileObject movedFile = JavaMetamodel.getManager().getDataObject(r).getPrimaryFile();
            val fileName = f.getName
            if (targetF != null) {
              for (child <- targetF.getChildren) {
                if (child.getName().equals(fileName) && "java".equals(child.getExt()) && !child.equals(f) && !child.isVirtual) { //NOI18N
                  return new Problem(true, new MessageFormat(
                      NbBundle.getMessage(classOf[MoveRefactoringPlugin], "ERR_ClassToMoveClashes")).format(Array(fileName).asInstanceOf[Array[Object]] // NOI18N
                    ))
                }
              } // for
            }
                    
            //                boolean accessedByOriginalPackage = ((Boolean) accessedByOriginalPackageMap.get(r)).booleanValue();
            //                boolean movingToDefaultPackage = ((Boolean) movingToDefaultPackageMap.get(r)).booleanValue();
            //                if (p==null && accessedByOriginalPackage && movingToDefaultPackage) {
            //                    p= new Problem(false, getString("ERR_MovingClassToDefaultPackage")); // NOI18N
            //                }
                    
            //                if (f.getFolder().getPrimaryFile().equals(targetF) && isPackageCorrect(r)) {
            //                    return new Problem(true, getString("ERR_CannotMoveIntoSamePackage"));
            //                }
          }
        }
      } catch {case ioe: IOException =>}
    }
    return null//super.fastCheckParameters
  }

  private def checkProjectDeps(a: Set[FileObject]): Problem = {
    if (!isRenameRefactoring) {
      val sourceRoots = new HashSet[FileObject]
      for (file <- filesToMove) {
        val cp = ClassPath.getClassPath(file, ClassPath.SOURCE)
        if (cp != null) {
          val root = cp.findOwnerRoot(file)
          sourceRoots.add(root)
        }
      }
      val target = refactoring.asInstanceOf[MoveRefactoring].getTarget.lookup(classOf[URL])
      if (target == null) {
        return null
      }
      try {
        val r = RetoucheUtils.getClassPathRoot(target);
        val targetUrl = URLMapper.findURL(r, URLMapper.EXTERNAL)
        val deps = ScalaSourceUtil.getDependentRoots(targetUrl)
        for (sourceRoot <- sourceRoots) {
          val sourceUrl = URLMapper.findURL(sourceRoot, URLMapper.INTERNAL)
          if (!deps.contains(sourceUrl)) {
            val sourceProject = FileOwnerQuery.getOwner(sourceRoot)
            for (affected <- a) {
              if (FileOwnerQuery.getOwner(affected).equals(sourceProject) && !filesToMove.contains(affected)) {
                val targetProject = FileOwnerQuery.getOwner(r)
                assert(sourceProject != null)
                assert(targetProject != null)
                val sourceName = ProjectUtils.getInformation(sourceProject).getDisplayName
                val targetName = ProjectUtils.getInformation(targetProject).getDisplayName
                return createProblem(null, false, NbBundle.getMessage(classOf[MoveRefactoringPlugin], "ERR_MissingProjectDeps", sourceName, targetName));
              }
            }
          }
        }
      } catch {case ex: IOException => Exceptions.printStackTrace(ex)}
    }
    null;
  }

  private def getRelevantFiles: Set[FileObject] = {
    val cpInfo = getClasspathInfo(refactoring);
    val idx = cpInfo.getClassIndex();
    val set = new HashSet[FileObject]
    for ((fo, item) <- classes) {
      //set.add(SourceUtils.getFile(el, cpInfo));
      val files = Set[FileObject]()
      //val files = idx.getResources(item, EnumSet.of(ClassIndex.SearchKind.TYPE_REFERENCES, ClassIndex.SearchKind.IMPLEMENTORS),EnumSet.of(ClassIndex.SearchScope.SOURCE));
      set ++= files
      whoReferences.put(fo, files)
    }
    set ++= filesToMove
    set.toSet
  }
    
  private def initClasses {
    /* val = new HashMap[FileObject,ElementHandle]
    for (i <- 0 until filesToMove.size) {
      final val j = i;
      try {
        JavaSource source = JavaSource.forFileObject(filesToMove.get(i));

        source.runUserActionTask(new CancellableTask<CompilationController>() {

            public void cancel() {
              throw new UnsupportedOperationException("Not supported yet."); // NOI18N
            }

            public void run(final CompilationController parameter) throws Exception {
              parameter.toPhase(JavaSource.Phase.ELEMENTS_RESOLVED);
              List<? extends Tree> trees= parameter.getCompilationUnit().getTypeDecls();
              for (Tree t: trees) {
                if (t.getKind() == Tree.Kind.CLASS) {
                  if (((ClassTree) t).getSimpleName().toString().equals(filesToMove.get(j).getName())) {
                    classes.put(filesToMove.get(j), ElementHandle.create(parameter.getTrees().getElement(TreePath.getPath(parameter.getCompilationUnit(), t))));
                    return ;
                  }
                }
              }

            }
          }, true);
      } catch {case ex: IOException =>}

    } */
  }

  private def initPackages {
    packages.clear    
    for (folders <- foldersToMove) {
      val cp = ClassPath.getClassPath(folders(0), ClassPath.SOURCE)
      for (folder <- folders) {
        val pkgName = cp.getResourceName(folder, '.', false)
        packages.add(pkgName)
      }
    }
  }
    
  def prepare(elements: RefactoringElementsBag): Problem = {
    fireProgressListenerStart(ProgressEvent.START, -1)

    initClasses
    initPackages
        
    val a = getRelevantFiles
    val p = checkProjectDeps(a)
    fireProgressListenerStep(a.size)
    val t = new MoveTransformer(this)
    val task = new TransformTask { //new TransformTask(t, null) {

        override protected def process(pr: ScalaParserResult): Seq[ModificationResult] = {
          /* val rt = new RenameTransformer(refactoring.getNewName, allMethods)
          rt.workingCopy_=(pr)
          rt.scan
          if (rt.diffs.isEmpty) {
            return Nil
          } else {
            val mr = new ModificationResult
            mr.addDifferences(pr.getSnapshot.getSource.getFileObject, java.util.Arrays.asList(rt.diffs.toArray: _*))
            return List(mr)
          } */
         return Nil
        }

    }
    val prob = null//createAndAddElements(a, task, elements, refactoring)
    fireProgressListenerStop
    if (prob != null) prob else chainProblems(p, t.getProblem)
  }
    
  private def chainProblems(p: Problem, p1: Problem): Problem = {        
    if (p  == null) return p1
    if (p1 == null) return p

    var problem: Problem  = p
    while (problem.getNext != null) {
      problem = problem.getNext
    }
    problem.setNext(p1)
    p
  }

  def getNewPackageName: String = {
    if (isRenameRefactoring) {
      refactoring.asInstanceOf[RenameRefactoring].getNewName
    } else {
      RetoucheUtils.getPackageName(refactoring.asInstanceOf[MoveRefactoring].getTarget.lookup(classOf[URL]))
    }
  }
    
  def getTargetPackageName(fo: FileObject): String = {
    if (isRenameRefactoring) {
      if (refactoring.getRefactoringSource.lookup(classOf[NonRecursiveFolder]) != null){
        //package rename
        return getNewPackageName
      } else {
        //folder rename
        val folder = refactoring.getRefactoringSource.lookup(classOf[FileObject])
        val cp = ClassPath.getClassPath(folder, ClassPath.SOURCE)
        val root = cp.findOwnerRoot(folder)
        val prefix = FileUtil.getRelativePath(root, folder.getParent()).replace('/','.')
        val postfix = FileUtil.getRelativePath(folder, if (fo.isFolder) fo else fo.getParent()).replace('/', '.')
        return concat(prefix, getNewPackageName, postfix)
      }
    } else if (packagePostfix != null) {
      if (fo == null) {
        return getNewPackageName
      }
      val postfix = packagePostfix.get(fo).getOrElse(null)
      val packageName = concat(null, getNewPackageName, postfix)
      return packageName
    } else
      return getNewPackageName
  }
    
  private def concat(s1: String, s2: String, s3: String): String = {
    var result = ""
    if (s1 != null && !"".equals(s1)) {
      result += s1 + "." // NOI18N
    }
    result += s2
    if (s3 != null && !"".equals(s3)) {
      result += (if (result == "") "" else ".") + s3 // NOI18N
    }
    result
  }

  /* protected def getJavaSource(p: Phase): JavaSource = {
    null;
  } */
}    
