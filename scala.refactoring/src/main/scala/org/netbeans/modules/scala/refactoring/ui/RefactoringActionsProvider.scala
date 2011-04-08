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

package org.netbeans.modules.scala.refactoring.ui

import java.util.Dictionary
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import org.netbeans.api.fileinfo.NonRecursiveFolder;
import org.netbeans.editor.BaseDocument;
import org.netbeans.api.language.util.ast.{AstDfn, AstScope}
import org.netbeans.modules.csl.api.ElementKind
import org.netbeans.modules.parsing.api.ResultIterator;
import org.netbeans.modules.parsing.api.UserTask;
import org.netbeans.modules.refactoring.spi.ui.UI;
import org.netbeans.modules.refactoring.spi.ui.ActionsImplementationProvider;
import org.netbeans.modules.refactoring.spi.ui.RefactoringUI;
import org.netbeans.modules.scala.core.{ScalaMimeResolver, ScalaParserResult}
import org.netbeans.modules.scala.core.ast.{ScalaItems, ScalaRootScope}
import org.netbeans.modules.scala.core.lexer.ScalaLexUtil;
import org.netbeans.modules.scala.refactoring.RetoucheUtils;

import org.netbeans.modules.parsing.api.Embedding;
import org.netbeans.modules.parsing.api.ParserManager;
import org.netbeans.modules.parsing.api.Source;
import org.netbeans.modules.parsing.spi.ParseException;
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.nodes.Node;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashSet

/**
 *
 * @author Jan Becicka
 */
object RefactoringActionsProvider {
  private var isFindUsages: Boolean = _

  def getName(dict: Dictionary[_, _]): String = {
    if (dict != null) {
      dict.get("name").asInstanceOf[String] //NOI18N
    } else null
  }

  def isFromEditor(ec: EditorCookie): Boolean = {
    if (ec != null && ec.getOpenedPanes != null) {
      // This doesn't seem to work well - a lot of the time, I'm right clicking
      // on the editor and it still has another activated view (this is on the mac)
      // and as a result does file-oriented refactoring rather than the specific
      // editor node...
      //            TopComponent activetc = TopComponent.getRegistry().getActivated();
      //            if (activetc instanceof CloneableEditorSupport.Pane) {
      //
      true
      //            }
    } else false
  }

}

@org.openide.util.lookup.ServiceProvider(service = classOf[ActionsImplementationProvider], position = 400)
class RefactoringActionsProvider extends ActionsImplementationProvider {
  import RefactoringActionsProvider._

  private val logger = Logger.getLogger(classOf[RefactoringActionsProvider].getName)

  logger.info(this.getClass.getSimpleName + " is created")

  override def doRename(lookup: Lookup) {
    val ec = lookup.lookup(classOf[EditorCookie])
    val dictionary = lookup.lookup(classOf[Dictionary[_, _]])
    val task: Runnable =
      if (isFromEditor(ec)) {
        new TextComponentTask(ec) {
          
          override protected def createRefactoringUI(selectedElement: ScalaItems#ScalaItem, startOffset: Int, endOffset: Int, info: ScalaParserResult): RefactoringUI = {
            // If you're trying to rename a constructor, rename the enclosing class instead
            RenameRefactoringUI(selectedElement)
          }
        }
      } else {
        new NodeToFileObjectTask(lookup.lookupAll(classOf[Node])) {

          override protected def createRefactoringUI(selectedElements: Array[FileObject], handles: Seq[ScalaItems#ScalaItem]): RefactoringUI= {
            val newName = getName(dictionary)
            if (newName != null) {
              if (pkg(0) != null)
                RenameRefactoringUI(pkg(0), newName)
              else
                RenameRefactoringUI(selectedElements(0), newName, if (handles == null || handles.isEmpty) null else handles.iterator.next)
            } else{
              if (pkg(0) != null)
                RenameRefactoringUI(pkg(0))
              else
                RenameRefactoringUI(selectedElements(0), if (handles == null|| handles.isEmpty) null else handles.iterator.next)
            }
          }
        }
      }
    
    task.run
  }

  /**
   * returns true if exactly one refactorable file is selected
   */
  override def canRename(lookup: Lookup): Boolean = {
    val nodes = lookup.lookupAll(classOf[Node])
    if (nodes.size != 1) {
      return false
    }
    val n = nodes.iterator.next
    val dob = n.getCookie(classOf[DataObject])
    if (dob==null) {
      return false;
    }
    val fo = dob.getPrimaryFile

    if (isOutsideScala(lookup, fo)) {
      return false
    }

    if (RetoucheUtils.isRefactorable(fo)) {
      return true
    }

    false
  }

  private def isOutsideScala(lookup: Lookup, fo: FileObject): Boolean = {
    if (!RetoucheUtils.isScalaFile(fo)) {
      // We're attempting to refactor in an embedded scenario...
      // Make sure it's actually in a JavaScript section.
      val ec = lookup.lookup(classOf[EditorCookie])
      if (isFromEditor(ec)) {
        val textC = ec.getOpenedPanes()(0)
        val d = textC.getDocument match {
          case x: BaseDocument => x
          case _ => return true
        }
        d.readLock
        try {
          val caret = textC.getCaretPosition
          if (ScalaLexUtil.getToken(d, caret) == null) {
            // Not in Scala code!
            return true
          }
        } finally {
          d.readUnlock
        }

      }
    }

    false
  }

  override def canCopy(lookup: Lookup): Boolean = {
    false
  }

  override def canFindUsages(lookup: Lookup): Boolean = {
    val nodes = lookup.lookupAll(classOf[Node])
    if (nodes.size != 1) {
      return false
    }
    val n = nodes.iterator.next
    val dob = n.getCookie(classOf[DataObject])
    if (dob == null) {
      return false
    }

    val fo = dob.getPrimaryFile

    if (RetoucheUtils.isScalaFile(fo) && isOutsideScala(lookup, fo)) {
      return false
    }

    if (dob != null && RetoucheUtils.isScalaFile(fo)) { //NOI18N
      return true
    }
    
    false
  }

  override def doFindUsages(lookup: Lookup) {
       
    val ec = lookup.lookup(classOf[EditorCookie])
    val task: Runnable = if (isFromEditor(ec)) {
      new TextComponentTask(ec) {
        
        override protected def createRefactoringUI(selectedElement: ScalaItems#ScalaItem, startOffset: Int, endOffset: Int, info: ScalaParserResult): RefactoringUI = {
          WhereUsedRefactoringUI(selectedElement)
        }
      }
    } else {
      new NodeToElementTask(lookup.lookupAll(classOf[Node])) {
        
        protected def createRefactoringUI(selectedElement: ScalaItems#ScalaItem, info: ScalaParserResult): RefactoringUI = {
          WhereUsedRefactoringUI(selectedElement)
        }
      }
    }
    try {
      isFindUsages = true
      task.run
    } finally {
      isFindUsages = false
    }
  }

  override def canDelete(lookup: Lookup): Boolean = {
    false
  }

  /**
   * returns true if there is at least one java file in the selection
   * and all java files are refactorable
   */
  override def canMove(lookup: Lookup): Boolean = {
    false
  }

  override def doMove(lookup: Lookup) {}

  abstract class TextComponentTask(ec: EditorCookie) extends UserTask with Runnable {
    private val textC = ec.getOpenedPanes()(0)
    private val caret = textC.getCaretPosition
    private val start = textC.getSelectionStart
    private val end = textC.getSelectionEnd
    assert (caret != -1)
    assert (start != -1)
    assert (end != -1)

    private var ui: RefactoringUI = _


    @throws(classOf[ParseException])
    def run(ri: ResultIterator) {
      if (ri.getSnapshot.getMimeType.equals(ScalaMimeResolver.MIME_TYPE)) {
        val pr = ri.getParserResult.asInstanceOf[ScalaParserResult]
        val th = ri.getSnapshot.getTokenHierarchy
        val root = pr.rootScope
        val global = pr.global
        import global._

        /* val sorted = root.findItemsAt(th, caret) sortWith {(x1, x2) =>
         def weight(sym: Symbol) =
         if (sym.isTrait || sym.isModule || sym.isClass) 0
         else if (sym.isValue) 10
         else if (sym.isMethod) 20
         else 30
         weight(x1.asInstanceOf[ScalaItem].symbol) < weight(x2.asInstanceOf[ScalaItem].symbol)
         } */
        val inPlaceItem = root.findItemsAt(th, caret) match {
          case Nil => return
          case xs => xs find {_.idToken != null} getOrElse {return}
        }
        
        val handle = root.findDfnOf(inPlaceItem) getOrElse inPlaceItem
        logger.info("Refactoring handle's token symbols: " + handle.samePlaceSymbols)
        
        // @todo ("FAILURE - can't refactor a reference identifier") ?
        ui = createRefactoringUI(handle.asInstanceOf[ScalaItem], start, end, pr)
      } else {
        val itr = ri.getEmbeddings.iterator
        while (itr.hasNext) {
          run(ri.getResultIterator(itr.next))
        }
      }
    }

    def run {
      try {
        val source = Source.create(textC.getDocument)
        ParserManager.parse(java.util.Collections.singleton(source), this)
      } catch {case ex: ParseException => logger.log(Level.WARNING, null, ex); return}

      val activetc = TopComponent.getRegistry.getActivated

      if (ui != null) {
// XXX: what is this supposed to do??
//                if (fo != null) {
//                    ClasspathInfo classpathInfoFor = RetoucheUtils.getClasspathInfoFor(fo);
//                    if (classpathInfoFor == null) {
//                        JOptionPane.showMessageDialog(null, NbBundle.getMessage(RefactoringActionsProvider.class, "ERR_CannotFindClasspath"));
//                        return;
//                    }
//                }

        UI.openRefactoringUI(ui, activetc)
      } else {
        val key = if (isFindUsages) {
          "ERR_CannotFindUsages" // NOI18N
        } else "ERR_CannotRenameLoc" // NOI18N
        JOptionPane.showMessageDialog(null,NbBundle.getMessage(classOf[RefactoringActionsProvider], key))
      }
    }

    protected def createRefactoringUI(selectedElement: ScalaItems#ScalaItem, startOffset: Int, endOffset: Int, pr: ScalaParserResult): RefactoringUI
  }

  abstract class NodeToElementTask(nodes: java.util.Collection[_ <: Node]) extends UserTask with Runnable  {
    assert(nodes.size == 1)
    private val node = nodes.iterator.next
    private var ui: RefactoringUI = _

    @throws(classOf[ParseException])
    def run(ri: ResultIterator) {
      if (ri.getSnapshot.getMimeType.equals(ScalaMimeResolver.MIME_TYPE)) {
        val pr = ri.getParserResult.asInstanceOf[ScalaParserResult]
        val root = pr.rootScope
        val tmpls = new ArrayBuffer[AstDfn]
        RetoucheUtils.getTopTemplates(List(root), tmpls)
        if (!tmpls.isEmpty) {
          // @todo multiple tmpls
          ui = createRefactoringUI(tmpls(0).asInstanceOf[ScalaItems#ScalaItem], pr)
        }
      } else {
        val itr = ri.getEmbeddings.iterator
        while (itr.hasNext) {
          run(ri.getResultIterator(itr.next))
        }
      }
    }

    def run {
      try {
        val o = node.getCookie(classOf[DataObject])
        val source = Source.create(o.getPrimaryFile)
        ParserManager.parse(java.util.Collections.singleton(source), this)
      } catch {case ex: ParseException => logger.log(Level.WARNING, null, ex); return}

      if (ui != null) {
        UI.openRefactoringUI(ui)
      } else {
        val key = if (isFindUsages) {
          "ERR_CannotFindUsages" // NOI18N
        } else "ERR_CannotRenameLoc" // NOI18N
        JOptionPane.showMessageDialog(null, NbBundle.getMessage(classOf[RefactoringActionsProvider], key))
      }
    }
    
    protected def createRefactoringUI(selectedElement: ScalaItems#ScalaItem, info: ScalaParserResult): RefactoringUI
  }

  abstract class NodeToFileObjectTask(nodes: java.util.Collection[_ <: Node]) extends UserTask with Runnable {
    assert(nodes != null)
//        private RefactoringUI ui;
    protected val pkg =  new Array[NonRecursiveFolder](nodes.size)
//        public WeakReference<JsParseResult> cinfo;
    val handles = new ArrayBuffer[ScalaItems#ScalaItem]


    @throws(classOf[Exception])
    def run(ri: ResultIterator) {
      if (ri.getSnapshot.getMimeType.equals(ScalaMimeResolver.MIME_TYPE)) {
        val pr = ri.getParserResult.asInstanceOf[ScalaParserResult]
        val root = pr.rootScope
        val tmpls = new ArrayBuffer[AstDfn]
        RetoucheUtils.getTopTemplates(List(root), tmpls)
        if (!tmpls.isEmpty) {
          // @todo multiple tmpls
          handles += (tmpls(0).asInstanceOf[ScalaItems#ScalaItem])
        }
      } else {
        val itr = ri.getEmbeddings.iterator
        while (itr.hasNext) {
          run(ri.getResultIterator(itr.next))
        }
      }
    }

    def run {
      val fobs = new Array[FileObject](nodes.size)
      var i = 0
      val itr = nodes.iterator
      while (itr.hasNext) {
        val node = itr.next
        val dob = node.getCookie(classOf[DataObject])
        if (dob != null) {
          fobs(i) = dob.getPrimaryFile
          val source = Source.create(fobs(i))
          try {
            ParserManager.parse(java.util.Collections.singleton(source), this)
          } catch {case ex: ParseException => logger.log(Level.WARNING, null, ex)}
          pkg(i) = node.getLookup.lookup(classOf[NonRecursiveFolder])
          i += 1
        }
      }
      UI.openRefactoringUI(createRefactoringUI(fobs, handles))
    }

    protected def createRefactoringUI(selectedElement: Array[FileObject], handles: Seq[ScalaItems#ScalaItem]): RefactoringUI
  }

}

