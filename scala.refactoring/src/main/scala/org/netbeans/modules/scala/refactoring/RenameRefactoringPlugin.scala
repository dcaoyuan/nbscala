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
package org.netbeans.modules.scala.refactoring

import java.util.logging.Level;
import org.netbeans.modules.csl.api.Error;
import org.netbeans.modules.csl.api.Severity;
import org.netbeans.modules.csl.spi.GsfUtilities;
import org.netbeans.editor.Utilities;
import org.netbeans.modules.parsing.api.ParserManager;
import org.netbeans.modules.parsing.api.UserTask;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.logging.Logger;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Position.Bias;
import org.netbeans.api.java.source.ClasspathInfo
import org.netbeans.api.language.util.ast.{AstDfn, AstScope}
import org.netbeans.api.language.util.text.BoyerMoore
import org.netbeans.api.lexer.Token
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenId;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.api.lexer.TokenUtilities;
import org.netbeans.editor.BaseDocument;
import org.netbeans.modules.csl.api.ElementKind;
import org.netbeans.modules.csl.api.OffsetRange;
import org.netbeans.modules.csl.spi.support.ModificationResult;
import org.netbeans.modules.csl.spi.support.ModificationResult.Difference;
import org.netbeans.modules.scala.core.{ScalaMimeResolver, ScalaParserResult}
import org.netbeans.modules.scala.core.ast.{ScalaItems, ScalaRootScope}
import org.netbeans.modules.scala.core.lexer.{ScalaTokenId, ScalaLexUtil}
import org.netbeans.modules.parsing.spi.Parser
import org.netbeans.modules.refactoring.api._
import org.openide.filesystems.FileObject
import org.netbeans.modules.refactoring.spi.RefactoringElementsBag
import org.netbeans.modules.parsing.api.ResultIterator;
import org.netbeans.modules.parsing.api.Source;
import org.netbeans.modules.parsing.spi.ParseException;
import org.openide.text.PositionRef;
import org.openide.util.NbBundle;
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet
import scala.tools.nsc.ast.Trees

import org.openide.text.CloneableEditorSupport
import scala.tools.nsc.symtab.Flags

/**
 * The actual Renaming refactoring work for Python.
 *
 * @author Tor Norbye
 * 
 * @todo Perform index lookups to determine the set of files to be checked!
 * @todo Check that the new name doesn't conflict with an existing name
 * @todo Check unknown files!
 * @todo More prechecks
 * @todo When invoking refactoring on a file object, I also rename the file. I should (a) list the
 *   name it's going to change the file to, and (b) definitely "filenamize" it - e.g. for class FooBar the
 *   filename should be foo_bar.
 * @todo If you rename a Model, I should add a corresponding rename_table entry in the migrations...
 *
 * @todo Complete this. Most of the prechecks are not implemented - and the refactorings themselves need a lot of work.
 */
class RenameRefactoringPlugin(rename: RenameRefactoring) extends ScalaRefactoringPlugin {
  private val logger = Logger.getLogger(this.getClass.getName)

  type SItem = ScalaItems#ScalaItem
  
  private val refactoring = rename
  private var searchHandle: SItem = _
  private var overriddenByMethods: Seq[_] = null // methods that override the method to be renamed
  private var overridesMethods: Seq[_] = null // methods that are overridden by the method to be renamed
  private var doCheckName: Boolean = true

  init

  // should after init, since we need searchHandle is inited
  private val targetName = searchHandle.getName
  private val samePlaceSyms = searchHandle.samePlaceSymbols.asInstanceOf[Seq[SItem#S]]
  private var samePlaceSymToDSimpleSig: Seq[(SItem#S, String)] = Nil
  
  /** Creates a new instance of RenameRefactoring */
  private def init {
    val item = rename.getRefactoringSource.lookup(classOf[SItem])
    if (item != null) {
      searchHandle = item
    } else {
      val source = Source.create(rename.getRefactoringSource.lookup(classOf[FileObject]))
      try {
        ParserManager.parse(java.util.Collections.singleton(source), new UserTask {
            @throws(classOf[Exception])
            override def run(ri: ResultIterator) {
              if (ri.getSnapshot.getMimeType == ScalaMimeResolver.MIME_TYPE) {
                val pr = ri.getParserResult.asInstanceOf[ScalaParserResult]
                val root = pr.rootScope
                val tmpls = new ArrayBuffer[AstDfn]
                RetoucheUtils.getTopTemplates(List(root), tmpls)
                if (!tmpls.isEmpty) {
                  // @todo multiple tmpls
                  searchHandle = tmpls(0).asInstanceOf[ScalaItems#ScalaItem]
                  refactoring.getContext.add(ri)
                }
              }
            }
          })
      } catch {case ex: ParseException => Logger.getLogger(classOf[RenameRefactoringPlugin].getName).log(Level.WARNING, null, ex)}
    }
  }

  override def fastCheckParameters: Problem = {
    var fastCheckProblem: Problem = null
    if (searchHandle == null) {
      return null; //no refactoring, not params check
    }

    val kind = searchHandle.kind
    val newName = refactoring.getNewName
    val oldName = searchHandle.symbol.fullName
    if (oldName == null) {
      return new Problem(true, "Cannot determine target name. Please file a bug with detailed information on how to reproduce (preferably including the current source file and the cursor position)");
    }

    if (oldName.equals(newName)) {
      val nameNotChanged = true
      //if (kind == ElementKind.CLASS || kind == ElementKind.MODULE) {
      //    if (!((TypeElement) element).getNestingKind().isNested()) {
      //        nameNotChanged = info.getFileObject().getName().equals(element);
      //    }
      //}
      if (nameNotChanged) {
        return createProblem(fastCheckProblem, true, getString("ERR_NameNotChanged"))
      }

    }

    // TODO - get a better Js name picker - and check for invalid Js symbol names etc.
    // TODO - call JsUtils.isValidLocalVariableName if we're renaming a local symbol!
    /*if (kind == ElementKind.CLASS && !JsUtils.isValidJsClassName(newName)) {
     String s = getString("ERR_InvalidClassName"); //NOI18N
     String msg = new MessageFormat(s).format(Array(newName).asInstanceOf[Array[Object]])
     fastCheckProblem = createProblem(fastCheckProblem, true, msg);
     return fastCheckProblem;
     } else*/


    // by Caoyuan
    /* if (kind == ElementKind.METHOD && !JsUtils.isValidJsMethodName(newName)) {
     val s = getString("ERR_InvalidMethodName"); //NOI18N
     val msg = new MessageFormat(s).format(Array(newName).asInstanceOf[Array[Object]])
     return ScalaRefactoringPlugin.createProblem(fastCheckProblem, true, msg)
     } else if (!JsUtils.isValidJsIdentifier(newName)) {
     val s = getString("ERR_InvalidIdentifier"); //NOI18N
     val msg = new MessageFormat(s).format(Array(newName).asInstanceOf[Array[Object]])
     return ScalaRefactoringPlugin.createProblem(fastCheckProblem, true, msg)
     }


     val msg = JsUtils.getIdentifierWarning(newName, 0);
     if (msg != null) {
     fastCheckProblem = ScalaRefactoringPlugin.createProblem(fastCheckProblem, false, msg);
     } */
    // ----- by Caoyuan
    
    // TODO
//        System.out.println("TODO - look for variable clashes etc");


//        if (kind.isClass() && !((TypeElement) element).getNestingKind().isNested()) {
//            if (doCheckName) {
//                String oldfqn = RetoucheUtils.getQualifiedName(treePathHandle);
//                String newFqn = oldfqn.substring(0, oldfqn.lastIndexOf(oldName));
//
//                String pkgname = oldfqn;
//                int i = pkgname.indexOf('.');
//                if (i>=0)
//                    pkgname = pkgname.substring(0,i);
//                else
//                    pkgname = "";
//
//                String fqn = "".equals(pkgname) ? newName : pkgname + '.' + newName;
//                FileObject fo = treePathHandle.getFileObject();
//                ClassPath cp = ClassPath.getClassPath(fo, ClassPath.SOURCE);
//                if (RetoucheUtils.typeExist(treePathHandle, newFqn)) {
//                    String msg = new MessageFormat(getString("ERR_ClassClash")).format(
//                            Array(newName, pkgname).asInstanceOf[Array[Object]])
//                    fastCheckProblem = createProblem(fastCheckProblem, true, msg);
//                    return fastCheckProblem;
//                }
//            }
//            FileObject primFile = treePathHandle.getFileObject();
//            FileObject folder = primFile.getParent();
//            FileObject[] children = folder.getChildren();
//            for (int x = 0; x < children.length; x++) {
//                if (children[x] != primFile && !children[x].isVirtual() && children[x].getName().equals(newName) && "java".equals(children[x].getExt())) { //NOI18N
//                    String msg = new MessageFormat(getString("ERR_ClassClash")).format(
//                            Array(newName, folder.getPath)).asInstanceOf[Array[Object]])
//                    );
//                    fastCheckProblem = createProblem(fastCheckProblem, true, msg);
//                    break;
//                }
//            } // for
//        } else if (kind == ElementKind.LOCAL_VARIABLE || kind == ElementKind.PARAMETER) {
//            String msg = variableClashes(newName,treePath, info);
//            if (msg != null) {
//                fastCheckProblem = createProblem(fastCheckProblem, true, msg);
//                return fastCheckProblem;
//            }
//        } else {
//            String msg = clashes(element, newName, info);
//            if (msg != null) {
//                fastCheckProblem = createProblem(fastCheckProblem, true, msg);
//                return fastCheckProblem;
//            }
//        }
    fastCheckProblem
  }

  override def checkParameters: Problem = {

    var checkProblem: Problem = null
    var steps = 0
    if (overriddenByMethods != null) {
      steps += overriddenByMethods.size
    }
    if (overridesMethods != null) {
      steps += overridesMethods.size
    }

    fireProgressListenerStart(AbstractRefactoring.PARAMETERS_CHECK, 8 + 3*steps);

//        Element element = treePathHandle.resolveElement(info);

    fireProgressListenerStep
    fireProgressListenerStep

    // TODO - check more parameters
    //System.out.println("TODO - need to check parameters for hiding etc.");


//        if (treePathHandle.getKind() == ElementKind.METHOD) {
//            checkProblem = checkMethodForOverriding((ExecutableElement)element, refactoring.getNewName(), checkProblem, info);
//            fireProgressListenerStep();
//            fireProgressListenerStep();
//        } else if (element.getKind().isField()) {
//            fireProgressListenerStep();
//            fireProgressListenerStep();
//            Element hiddenField = hides(element, refactoring.getNewName(), info);
//            fireProgressListenerStep();
//            fireProgressListenerStep();
//            fireProgressListenerStep();
//            if (hiddenField != null) {
//                msg = new MessageFormat(getString("ERR_WillHide")).format(
//                        new Object[] {SourceUtils.getEnclosingTypeElement(hiddenField).toString()}
//                );
//                checkProblem = createProblem(checkProblem, false, msg);
//            }
//        }
    fireProgressListenerStop
    checkProblem
  }

//        private Problem checkMethodForOverriding(ExecutableElement m, String newName, Problem problem, CompilationInfo info) {
//            ElementUtilities ut = info.getElementUtilities();
//            //problem = willBeOverridden(m, newName, argTypes, problem);
//            fireProgressListenerStep();
//            problem = willOverride(m, newName, problem, info);
//            fireProgressListenerStep();
//            return problem;
//        }
//
//    private Set<searchHandle<ExecutableElement>> allMethods;

  override def preCheck: Problem = {
    if (searchHandle == null) {
      return null
    }
    searchHandle.fo match {
      case Some(x) if x.isValid => return null
      case _ => return new Problem(true, NbBundle.getMessage(classOf[RenameRefactoringPlugin], "DSC_ElNotAvail")) // NOI18N
    }
  }

  private def getRelevantFiles: Set[FileObject] = {
    val cpInfo = getClasspathInfo(refactoring)
    val set = new HashSet[FileObject]

    searchHandle.fo foreach {fo =>
      set.add(fo)

      // * is there any symbol in this place not private?
      val notLocal = samePlaceSyms exists {x => 
        !x.hasFlag(Flags.PRIVATE)
      }

      if (notLocal) {
        val srcCp = cpInfo.getClassPath(ClasspathInfo.PathKind.SOURCE)
        if (srcCp != null) {
          set ++= RetoucheUtils.getScalaFilesInSrcCp(srcCp, true)
        }
      }
    }

    (set filter {x =>
        try {
          BoyerMoore.indexOf(x.asText, targetName) != -1
        } catch {case _: IOException => true}
      }).toSet

    /*
     try {
     source.runUserActionTask(new CancellableTask<CompilationController>() {

     public void cancel() {
     throw new UnsupportedOperationException("Not supported yet."); // NOI18N
     }

     public void run(CompilationController info) throws Exception {
     final ClassIndex idx = info.getClasspathInfo().getClassIndex();
     info.toPhase(JavaSource.Phase.RESOLVED);
     Element el = treePathHandle.resolveElement(info);
     ElementKind kind = el.getKind();
     ElementHandle<TypeElement> enclosingType;
     if (el instanceof TypeElement) {
     enclosingType = ElementHandle.create((TypeElement)el);
     } else {
     enclosingType = ElementHandle.create(info.getElementUtilities().enclosingTypeElement(el));
     }
     set.add(SourceUtils.getFile(el, info.getClasspathInfo()));
     if (el.getModifiers().contains(Modifier.PRIVATE)) {
     if (kind == ElementKind.METHOD) {
     //add all references of overriding methods
     allMethods = new HashSet<ElementHandle<ExecutableElement>>();
     allMethods.add(ElementHandle.create((ExecutableElement)el));
     }
     } else {
     if (kind.isField()) {
     set.addAll(idx.getResources(enclosingType, EnumSet.of(ClassIndex.SearchKind.FIELD_REFERENCES), EnumSet.of(ClassIndex.SearchScope.SOURCE)));
     } else if (el instanceof TypeElement) {
     set.addAll(idx.getResources(enclosingType, EnumSet.of(ClassIndex.SearchKind.TYPE_REFERENCES, ClassIndex.SearchKind.IMPLEMENTORS),EnumSet.of(ClassIndex.SearchScope.SOURCE)));
     } else if (kind == ElementKind.METHOD) {
     //add all references of overriding methods
     allMethods = new HashSet<ElementHandle<ExecutableElement>>();
     allMethods.add(ElementHandle.create((ExecutableElement)el));
     for (ExecutableElement e:RetoucheUtils.getOverridingMethods((ExecutableElement)el, info)) {
     addMethods(e, set, info, idx);
     }
     //add all references of overriden methods
     for (ExecutableElement ov: RetoucheUtils.getOverridenMethods((ExecutableElement)el, info)) {
     addMethods(ov, set, info, idx);
     for (ExecutableElement e:RetoucheUtils.getOverridingMethods( ov,info)) {
     addMethods(e, set, info, idx);
     }
     }
     set.addAll(idx.getResources(enclosingType, EnumSet.of(ClassIndex.SearchKind.METHOD_REFERENCES),EnumSet.of(ClassIndex.SearchScope.SOURCE))); //?????
     }
     }
     }
     }, true);
     } catch (IOException ioe) {
     throw (RuntimeException) new RuntimeException().initCause(ioe);
     }
     */
  }

  /*
   private void addMethods(ExecutableElement e, Set set, CompilationInfo info, ClassIndex idx) {
   set.add(SourceUtils.getFile(e, info.getClasspathInfo()));
   searchHandle<TypeElement> encl = searchHandle.create(SourceUtils.getEnclosingTypeElement(e));
   set.addAll(idx.getResources(encl, EnumSet.of(ClassIndex.SearchKind.METHOD_REFERENCES),EnumSet.of(ClassIndex.SearchScope.SOURCE)));
   allMethods.add(searchHandle.create(e));
   }
   */

  private var allMethods: Set[ScalaItems#ScalaItem] = _

  override def prepare(elements: RefactoringElementsBag): Problem = {
    if (searchHandle == null) {
      return null
    }
    val files = getRelevantFiles
    fireProgressListenerStart(ProgressEvent.START, files.size)
    if (!files.isEmpty) {
      val transform = new TransformTask {

        override protected def process(pr: ScalaParserResult): Seq[ModificationResult] = {
          val rt = new RenameTransformer(refactoring.getNewName, allMethods)
          rt.workingCopy_=(pr)
          rt.scan
          if (rt.diffs.isEmpty) {
            return Nil
          } else {
            val mr = new ModificationResult
            mr.addDifferences(pr.getSnapshot.getSource.getFileObject, java.util.Arrays.asList(rt.diffs.toArray: _*))
            return List(mr)
          }
        }
      }

      val results = processFiles(files, transform)
      elements.registerTransaction(new ScalaTransaction(results))
      for (result <- results) {
        val fItr = result.getModifiedFileObjects.iterator
        while (fItr.hasNext) {
          val fo = fItr.next
          val dItr = result.getDifferences(fo).iterator
          while (dItr.hasNext) {
            val diff = dItr.next
            val old = diff.getOldText
            if (old!=null) {
              //TODO: workaround
              //generator issue?
              elements.add(refactoring, DiffElement(diff, fo, result))
            }
          }
        }
      }
    }
    fireProgressListenerStop

    null
  }


  private def getString(key: String): String = {
    NbBundle.getMessage(classOf[RenameRefactoringPlugin], key)
  }

  /**
   *
   * @author Jan Becicka
   */
  class RenameTransformer(newName: String, allMethods: Set[ScalaItems#ScalaItem]) extends SearchVisitor {

    private val oldName = searchHandle.symbol.nameString
    private var ces: CloneableEditorSupport = _
    var diffs: ArrayBuffer[Difference] = _

    override def workingCopy_=(workingCopy: ScalaParserResult) {
      // Cached per working copy
      this.ces = null
      this.diffs = null
      super.workingCopy = workingCopy
    }

    override def scan {
      diffs = new ArrayBuffer[Difference]
      val searchCtx = searchHandle
      var error: Error = null
      val th = workingCopy.getSnapshot.getTokenHierarchy
      val root = workingCopy.rootScope
      val workingCopyFo = workingCopy.getSnapshot.getSource.getFileObject
      val global = workingCopy.global
      import global._


      if (root != ScalaRootScope.EMPTY) {
        val doc = GsfUtilities.getDocument(workingCopyFo, true)
        try {
          if (doc != null) doc.readLock

          if (samePlaceSymToDSimpleSig.isEmpty) {
            samePlaceSymToDSimpleSig = samePlaceSyms map {case x: Symbol => (x, ScalaUtil.symSimpleSig(x))}
          }


          def isRef(sym: Symbol) = try {
            lazy val overriddens = sym.allOverriddenSymbols
            val mySig = ScalaUtil.symSimpleSig(sym)
            val myQName = sym.fullName
            samePlaceSymToDSimpleSig exists {
              case (symx, sigx) if mySig == sigx =>
                val qNamex = symx.fullName
                if (myQName == qNamex) true
                else overriddens exists {_.fullName == qNamex}
              case _ => false
            }
          } catch {case _ => false}

          val tokens = new HashSet[Token[_]]
          for {(token, items) <- root.idTokenToItems
               item <- items
               sym = item.asInstanceOf[ScalaItem].symbol
               // * tokens.add(token) should be last condition
               if token.text.toString == sym.nameString && isRef(sym) && tokens.add(token)
          } {
            logger.info(workingCopyFo + ": find where used element " + sym.fullName)
            rename(item.asInstanceOf[ScalaItem], sym.nameString, null, getString("UpdateLocalvar"), th)
          }

        } finally {
          if (doc != null) doc.readUnlock
        }
      } else {
        //System.out.println("Skipping file " + workingCopy.getFileObject());
        // See if the document contains references to this symbol and if so, put a warning in
        val workingCopyText = workingCopy.getSnapshot.getText.toString
        if (workingCopyText.indexOf(oldName) != -1) {
          // TODO - icon??
          if (ces == null) {
            ces = RetoucheUtils.findCloneableEditorSupport(workingCopy)
          }
          var start = 0
          var end = 0
          var desc = NbBundle.getMessage(classOf[RenameRefactoringPlugin], "ParseErrorFile", oldName)
          val errors = workingCopy.getDiagnostics
          if (errors.size > 0) {
            var break = false
            val itr = errors.iterator
            while (itr.hasNext && !break) {
              val e = itr.next
              if (e.getSeverity == Severity.ERROR) {
                error = e
                break = true
              }
            }
            if (error == null) {
              error = errors.get(0)
            }

            var errorMsg = error.getDisplayName
            if (errorMsg.length > 80) {
              errorMsg = errorMsg.substring(0, 77) + "..." // NOI18N
            }

            desc = desc + "; " + errorMsg
            start = error.getStartPosition
            start = ScalaLexUtil.getLexerOffset(workingCopy, start)
            if (start == -1) {
              start = 0
            }
            end = start
          }
          val startPos = ces.createPositionRef(start, Bias.Forward);
          val endPos = ces.createPositionRef(end, Bias.Forward);
          val diff = new Difference(Difference.Kind.CHANGE, startPos, endPos, "", "", desc) // NOI18N
          diffs += diff
        }
      }

      if (error == null && refactoring.isSearchInComments) {
        val doc = RetoucheUtils.getDocument(workingCopy)
        if (doc != null) {
          //force open
          val th = TokenHierarchy.get(doc)
          val ts = th.tokenSequence.asInstanceOf[TokenSequence[TokenId]]

          ts.move(0)
          searchTokenSequence(ts)
        }
      }

      ces = null
    }

    private def searchTokenSequence(ts: TokenSequence[TokenId]) {
      if (ts.moveNext) {
        do {
          val token = ts.token
          val id = token.id

          id.primaryCategory match {
            case "comment" | "block-comment" => // NOI18N
              // search this comment
              val tokenText = token.text
              if (tokenText != null && oldName != null) {
                val index = TokenUtilities.indexOf(tokenText, oldName) match {
                  case -1 =>
                  case idx =>
                    val text = tokenText.toString
                    // TODO make sure it's its own word. Technically I could
                    // look at identifier chars like "_" here but since they are
                    // used for other purposes in comments, consider letters
                    // and numbers as enough
                    if ((idx == 0 || !Character.isLetterOrDigit(text.charAt(idx-1))) &&
                        (idx+oldName.length >= text.length ||
                         !Character.isLetterOrDigit(text.charAt(idx+oldName.length)))) {
                      val start = ts.offset + idx
                      val end = start + oldName.length
                      if (ces == null) {
                        ces = RetoucheUtils.findCloneableEditorSupport(workingCopy)
                      }
                      val startPos = ces.createPositionRef(start, Bias.Forward)
                      val endPos = ces.createPositionRef(end, Bias.Forward)
                      val desc = getString("ChangeComment")
                      val diff = new Difference(Difference.Kind.CHANGE, startPos, endPos, oldName, newName, desc)
                      diffs += diff
                    }
                }
              }
            case _ =>
              ts.embedded.asInstanceOf[TokenSequence[TokenId]] match {
                case null =>
                case embedded => searchTokenSequence(embedded)
              }
          }
        } while (ts.moveNext)
      }
    }

    private def rename(item: ScalaItems#ScalaItem, oldCode: String, anewCode: String, adesc: String, th: TokenHierarchy[_]) {
      var newCode = anewCode
      var desc = adesc
      item.idOffset(th)
      val range = new OffsetRange(item.idOffset(th), item.idEndOffset(th))

      assert(range != OffsetRange.NONE)
      var pos = range.getStart

      // Convert from AST to lexer offsets if necessary
      pos = ScalaLexUtil.getLexerOffset(workingCopy, pos)
      if (pos == -1) {
        // Translation failed
        return
      }

      if (desc == null) {
        desc = NbBundle.getMessage(classOf[RenameRefactoringPlugin], "UpdateRef", oldCode)
      }

      if (ces == null) {
        ces = RetoucheUtils.findCloneableEditorSupport(workingCopy)
      }

      var start = pos
      var end = pos + oldCode.length
      // TODO if a SymbolNode, +=1 since the symbolnode includes the ":"
      var doc: BaseDocument = null
      try {
        doc = ces.openDocument.asInstanceOf[BaseDocument]
        doc.readLock

        if (start > doc.getLength) {
          end = doc.getLength
          start = end
        }

        if (end > doc.getLength) {
          end = doc.getLength
        }

        // Look in the document and search around a bit to detect the exact method reference
        // (and adjust position accordingly). Thus, if I have off by one errors in the AST (which
        // occasionally happens) the user's source won't get munged
        if (!oldCode.equals(doc.getText(start, end - start))) {
          // Look back and forwards by 1 at first
          val lineStart = Utilities.getRowFirstNonWhite(doc, start)
          val lineEnd = Utilities.getRowLastNonWhite(doc, start) + 1 // +1: after last char
          if (lineStart == -1 || lineEnd == -1) { // We're really on the wrong line!
            println("Empty line entry in " + FileUtil.getFileDisplayName(workingCopy.getSnapshot.getSource.getFileObject) +
                    "; no match for " + oldCode + " in line " + start + " referenced by node " +
                    item + " of type " + item.symbol)
            return;
          }

          if (lineStart < 0 || lineEnd - lineStart < 0) {
            return; // Can't process this one
          }

          val line = doc.getText(lineStart, lineEnd - lineStart);
          if (line.indexOf(oldCode) == -1) {
            println("Skipping entry in " + FileUtil.getFileDisplayName(workingCopy.getSnapshot.getSource.getFileObject) +
                    "; no match for " + oldCode + " in line " + line + " referenced by node " +
                    item + " of type " + item.symbol)
          } else {
            val lineOffset = start - lineStart
            var newOffset = -1
            // Search up and down by one
            var distance = -1
            var break = false
            while (distance < line.length && !break) {
              // Ahead first
              if (lineOffset + distance + oldCode.length <= line.length &&
                  oldCode.equals(line.substring(lineOffset + distance, lineOffset + distance + oldCode.length))) {
                newOffset = lineOffset + distance
                break = true
              }
              if (lineOffset - distance >= 0 && lineOffset - distance + oldCode.length() <= line.length() &&
                  oldCode.equals(line.substring(lineOffset - distance, lineOffset - distance + oldCode.length))) {
                newOffset = lineOffset - distance
                break = true
              }
              distance += 1
            }

            if (newOffset != -1) {
              start = newOffset + lineStart
              end = start + oldCode.length
            }

          }
        }
      } catch {
        case ex: IOException => Exceptions.printStackTrace(ex)
        case ex: BadLocationException => Exceptions.printStackTrace(ex)
      } finally {
        if (doc != null) {
          doc.readUnlock
        }
      }

      if (newCode == null) {
        // Usually it's the new name so allow client code to refer to it as just null
        newCode = refactoring.getNewName // XXX isn't this == our field "newName"?
      }

      val startPos = ces.createPositionRef(start, Bias.Forward)
      val endPos = ces.createPositionRef(end, Bias.Forward)
      val diff = new Difference(Difference.Kind.CHANGE, startPos, endPos, oldCode, newCode, desc)
      diffs += diff
    }

  }
}
