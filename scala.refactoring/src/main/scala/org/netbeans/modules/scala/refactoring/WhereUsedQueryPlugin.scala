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
package org.netbeans.modules.scala.refactoring;

import java.io.IOException
import java.util.logging.Logger
//import javax.swing.Icon;
//import javax.swing.text.Document;
import org.netbeans.modules.csl.api.ElementKind;
import org.netbeans.modules.csl.api.Error;
import org.netbeans.modules.csl.api.Modifier;
import org.netbeans.modules.csl.api.OffsetRange;
import org.netbeans.modules.csl.api.Severity;
//import org.netbeans.api.java.classpath.ClassPath
import org.netbeans.api.java.source.ClasspathInfo
import org.netbeans.api.language.util.text.BoyerMoore
import org.netbeans.api.lexer.Token;
//import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenId;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.api.lexer.TokenUtilities;
//import org.netbeans.editor.BaseDocument;
import org.netbeans.modules.csl.api.UiUtils
//import org.netbeans.modules.csl.spi.GsfUtilities;
import org.netbeans.modules.csl.spi.support.ModificationResult;
//import org.netbeans.modules.parsing.api.Source
import org.netbeans.modules.refactoring.api.Problem;
import org.netbeans.modules.refactoring.api.ProgressEvent;
import org.netbeans.modules.refactoring.api.WhereUsedQuery;
import org.netbeans.modules.refactoring.spi.RefactoringElementsBag;

import org.netbeans.modules.scala.core.ScalaParserResult
import org.netbeans.modules.scala.core.ast.{ ScalaItems, ScalaRootScope }
import org.netbeans.modules.scala.core.lexer.ScalaLexUtil
import org.openide.filesystems.FileObject
import org.openide.util.NbBundle;
import scala.collection.mutable.MutableList
import scala.collection.mutable.HashSet
import scala.collection.JavaConversions._
import scala.reflect.internal.Flags

/**
 * Actual implementation of Find Usages query search
 *
 * @todo Perform index lookups to determine the set of files to be checked!
 * @todo Do more prechecks of the elements we're trying to find usages for
 *
 * @author Jan Becicka
 * @author Tor Norbye
 */
class WhereUsedQueryPlugin(refactoring: WhereUsedQuery) extends ScalaRefactoringPlugin {
  private val logger = Logger.getLogger(this.getClass.getName)

  type SItem = ScalaItems#ScalaItem

  private val searchHandle = refactoring.getRefactoringSource.lookup(classOf[SItem])
  private val targetName = searchHandle.getName
  private val samePlaceSyms = searchHandle.samePlaceSymbols.asInstanceOf[Seq[SItem#S]]
  private var samePlaceSymToDSimpleSig: Seq[(SItem#S, String)] = Nil

  override def preCheck: Problem = {
    searchHandle.fo match {
      case Some(x) if x.isValid => null
      case _                    => return new Problem(true, NbBundle.getMessage(classOf[WhereUsedQueryPlugin], "DSC_ElNotAvail")) // NOI18N
    }
  }

  private def getRelevantFiles(handle: ScalaItems#ScalaItem): Set[FileObject] = {
    val cpInfo = getClasspathInfo(refactoring)
    val set = new HashSet[FileObject]

    handle.fo foreach { fo =>
      set.add(fo)

      // * is there any symbol in this place not private?
      val notLocal = samePlaceSyms exists { x => !x.hasFlag(Flags.PRIVATE) }

      if (notLocal) {
        val srcCp = cpInfo.getClassPath(ClasspathInfo.PathKind.SOURCE)
        if (srcCp ne null) {
          set ++= RetoucheUtils.getScalaFilesInSrcCp(srcCp, false)
        }
      }
    }

    /*
     val index = cpInfo.getClassIndex
     val file = handle.fo.getOrElse(return Set[FileObject]())
     val source = if (file ne null) {
     set.add(file)
     Source.create(file)
     } else {
     Source.create(cpInfo)
     }

     //XXX: This is slow!
     CancellableTask<CompilationController> task = new CancellableTask<CompilationController>() {
     public void cancel() {
     }

     public void run(CompilationController info) throws Exception {
     info.toPhase(JavaSource.Phase.RESOLVED);
     final Element el = tph.resolveElement(info);
     if (el eq null) {
     throw new NullPointerException(String.format("#145291: Cannot resolve handle: %s\n%s", tph, info.getClasspathInfo())); // NOI18N
     }
     if (el.getKind().isField()) {
     //get field references from index
     set.addAll(idx.getResources(ElementHandle.create((TypeElement)el.getEnclosingElement()), EnumSet.of(ClassIndex.SearchKind.FIELD_REFERENCES), EnumSet.of(ClassIndex.SearchScope.SOURCE)));
     } else if (el.getKind().isClass() || el.getKind().isInterface()) {
     if (isFindSubclasses || isFindDirectSubclassesOnly) {
     if (isFindDirectSubclassesOnly) {
     //get direct implementors from index
     EnumSet searchKind = EnumSet.of(ClassIndex.SearchKind.IMPLEMENTORS);
     set.addAll(idx.getResources(ElementHandle.create((TypeElement)el), searchKind,EnumSet.of(ClassIndex.SearchScope.SOURCE)));
     } else {
     //itererate implementors recursively
     set.addAll(getImplementorsRecursive(idx, cpInfo, (TypeElement)el));
     }
     } else {
     //get type references from index
     set.addAll(idx.getResources(ElementHandle.create((TypeElement) el), EnumSet.of(ClassIndex.SearchKind.TYPE_REFERENCES, ClassIndex.SearchKind.IMPLEMENTORS),EnumSet.of(ClassIndex.SearchScope.SOURCE)));
     }
     } else if (el.getKind() == ElementKind.METHOD && isFindOverridingMethods) {
     //Find overriding methods
     TypeElement type = (TypeElement) el.getEnclosingElement();
     set.addAll(getImplementorsRecursive(idx, cpInfo, type));
     }
     if (el.getKind() == ElementKind.METHOD && isFindUsages) {
     //get method references for method and for all it's overriders
     Set<ElementHandle<TypeElement>> s = RetoucheUtils.getImplementorsAsHandles(idx, cpInfo, (TypeElement)el.getEnclosingElement());
     for (ElementHandle<TypeElement> eh:s) {
     TypeElement te = eh.resolve(info);
     if (te==null) {
     continue;
     }
     for (Element e:te.getEnclosedElements()) {
     if (e instanceof ExecutableElement) {
     if (info.getElements().overrides((ExecutableElement)e, (ExecutableElement)el, te)) {
     set.addAll(idx.getResources(ElementHandle.create(te), EnumSet.of(ClassIndex.SearchKind.METHOD_REFERENCES),EnumSet.of(ClassIndex.SearchScope.SOURCE)));
     }
     }
     }
     }
     set.addAll(idx.getResources(ElementHandle.create((TypeElement) el.getEnclosingElement()), EnumSet.of(ClassIndex.SearchKind.METHOD_REFERENCES),EnumSet.of(ClassIndex.SearchScope.SOURCE))); //?????
     } else if (el.getKind() == ElementKind.CONSTRUCTOR) {
     set.addAll(idx.getResources(ElementHandle.create((TypeElement) el.getEnclosingElement()), EnumSet.of(ClassIndex.SearchKind.TYPE_REFERENCES, ClassIndex.SearchKind.IMPLEMENTORS),EnumSet.of(ClassIndex.SearchScope.SOURCE)));
     }

     }
     };
     try {
     source.runUserActionTask(task, true);
     } catch (IOException ioe) {
     throw (RuntimeException) new RuntimeException().initCause(ioe);
     } */

    if (isFindUsages || isFindDirectSubclassesOnly || isFindOverridingMethods) {
      (set filter { x =>
        try {
          BoyerMoore.indexOf(x.asText, targetName) != -1
        } catch { case _: IOException => true }
      }).toSet
    } else set.toSet
  }

  override def prepare(elements: RefactoringElementsBag): Problem = {
    val a = getRelevantFiles(searchHandle)
    fireProgressListenerStart(ProgressEvent.START, a.size)
    processFiles(a, new FindTask(elements))
    fireProgressListenerStop
    null
  }

  override def fastCheckParameters: Problem = {
    if (targetName eq null) {
      return new Problem(true, "Cannot determine target name. Please file a bug with detailed information on how to reproduce (preferably including the current source file and the cursor position)");
    }
    if (searchHandle.kind == ElementKind.METHOD) {
      return checkParametersForMethod(isFindOverridingMethods, isFindUsages)
    }
    null
  }

  override def checkParameters: Problem = {
    null
  }

  private def checkParametersForMethod(overriders: Boolean, usages: Boolean): Problem = {
    if (!(usages || overriders)) {
      new Problem(true, NbBundle.getMessage(classOf[WhereUsedQueryPlugin], "MSG_NothingToFind"))
    } else null
  }

  private def isFindSubclasses: Boolean = {
    refactoring.getBooleanValue(WhereUsedQueryConstants.FIND_SUBCLASSES)
  }
  private def isFindUsages: Boolean = {
    refactoring.getBooleanValue(WhereUsedQuery.FIND_REFERENCES)
  }
  private def isFindDirectSubclassesOnly: Boolean = {
    refactoring.getBooleanValue(WhereUsedQueryConstants.FIND_DIRECT_SUBCLASSES)
  }
  private def isFindOverridingMethods: Boolean = {
    refactoring.getBooleanValue(WhereUsedQueryConstants.FIND_OVERRIDING_METHODS)
  }
  private def isSearchInComments: Boolean = {
    refactoring.getBooleanValue(WhereUsedQuery.SEARCH_IN_COMMENTS)
  }
  private def isSearchFromBaseClass: Boolean = {
    false
  }

  private class FindTask(elements: RefactoringElementsBag) extends TransformTask {

    protected def process(pr: ScalaParserResult): Seq[ModificationResult] = {
      if (isCancelled) {
        return null
      }

      var error: Error = null

      val th = pr.getSnapshot.getTokenHierarchy
      val root = pr.rootScope
      val global = pr.global
      val foundElements = new MutableList[WhereUsedElement]()

      if (root == ScalaRootScope.EMPTY) {
        val sourceText = pr.getSnapshot.getText.toString
        //System.out.println("Skipping file " + workingCopy.getFileObject());
        // See if the document contains references to this symbol and if so, put a warning in
        if ((sourceText ne null) && sourceText.indexOf(targetName) != -1) {
          var start = 0
          var end = 0
          var desc = "Parse error in file which contains " + targetName + " reference - skipping it"
          val errors = pr.getDiagnostics
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
            if (error eq null) {
              error = errors.get(0)
            }

            var errorMsg = error.getDisplayName
            if (errorMsg.length > 80) {
              errorMsg = errorMsg.substring(0, 77) + "..." // NOI18N
            }

            desc = desc + "; " + errorMsg
            start = error.getStartPosition
            start = ScalaLexUtil.getLexerOffset(pr, start)
            if (start == -1) {
              start = 0
            }
            end = start
          }

          val icon = UiUtils.getElementIcon(ElementKind.ERROR, java.util.Collections.emptySet[Modifier])
          val range = new OffsetRange(start, end)
          val element = WhereUsedElement(pr, targetName, desc, range, icon)
          foundElements += element
        }
      }

      if ((error eq null) && isSearchInComments) {
        val doc = RetoucheUtils.getDocument(pr)
        if (doc ne null) {
          val th = pr.getSnapshot.getTokenHierarchy
          val ts = th.tokenSequence.asInstanceOf[TokenSequence[TokenId]]

          ts.move(0)
          searchTokenSequence(pr, ts)
        }
      }

      if (root eq null) {
        // TODO - warn that this file isn't compileable and is skipped?
        return Nil
      }

      // If it's a local search, use a simpler search routine
      // TODO: ArgumentNode - look to see if we're in a parameter list, and if so its a localvar
      // (if not, it's a method)

      /*            if (isFindSubclasses() || isFindDirectSubclassesOnly()) {
       // I'm only looking for the specific classes
       assert subclasses ne null;
       // Look in these files for the given classes
       //findSubClass(root);
       for (IndexedClass clz : subclasses) {
       ScalaItems#ScalaItem matchCtx = new ScalaItems#ScalaItem(clz, compiler);
       elements.add(refactoring, WhereUsedElement.create(matchCtx));
       }
       } else*/

      if (samePlaceSymToDSimpleSig.isEmpty) {
        samePlaceSymToDSimpleSig = samePlaceSyms map { case x: global.Symbol => (x, global.ScalaUtil.askForSymSimpleSig(x)) }
      }

      if (isFindUsages) {
        global.askForResponse { () =>
          import global._

          def isRef(sym: Symbol) = try {
            lazy val overriddens = sym.allOverriddenSymbols
            val mySig = ScalaUtil.symSimpleSig(sym)
            val myQName = sym.fullName
            samePlaceSymToDSimpleSig exists {
              case (symx, sigx) if mySig == sigx =>
                val qNamex = symx.fullName
                if (myQName == qNamex) true
                else overriddens exists { _.fullName == qNamex }
              case _ => false
            }
          } catch {
            case _: Throwable => false
          }

          val tokens = new HashSet[Token[_]]
          for {
            (token, items) <- root.idTokenToItems
            item <- items
            sym = item.asInstanceOf[ScalaItem].symbol
            // * tokens.add(token) should be the last condition
            if token.text.toString == targetName && isRef(sym) && tokens.add(token)
          } {
            logger.info(pr.getSnapshot.getSource.getFileObject + ": find where used element " + sym.fullName)
            foundElements += WhereUsedElement(pr, item.asInstanceOf[ScalaItem])
          }
        } get match {
          case Left(_)   =>
          case Right(ex) => global.processGlobalException(ex)
        }

      } else if (isFindOverridingMethods) {
        // TODO
      } else if (isSearchFromBaseClass) {
        // TODO
      }

      elements.addAll(refactoring, foundElements.sortWith(_.compare(_) < 0))
      
      Nil
    }

    private def searchTokenSequence(pr: ScalaParserResult, ts: TokenSequence[TokenId]) {
      if (ts.moveNext) {
        do {
          val token = ts.token
          val id = token.id

          id.primaryCategory match {
            case "comment" | "block-comment" => // NOI18N
              // search this comment
              assert(targetName ne null)
              val tokenText = token.text
              if ((tokenText ne null) && (targetName ne null)) {
                TokenUtilities.indexOf(tokenText, targetName) match {
                  case -1 =>
                  case idx =>
                    val text = tokenText.toString
                    // TODO make sure it's its own word. Technically I could
                    // look at identifier chars like "_" here but since they are
                    // used for other purposes in comments, consider letters
                    // and numbers as enough
                    if ((idx == 0 || !Character.isLetterOrDigit(text.charAt(idx - 1))) &&
                      (idx + targetName.length >= text.length ||
                        !Character.isLetterOrDigit(text.charAt(idx + targetName.length)))) {
                      val start = ts.offset + idx
                      val end = start + targetName.length

                      // TODO - get a comment-reference icon? For now, just use the icon type
                      // of the search target
                      val icon = searchHandle.getIcon
                      val range = new OffsetRange(start, end)
                      val element = WhereUsedElement(pr, targetName, range, icon)
                      elements.add(refactoring, element)
                    }
                }
              }
            case _ =>
              ts.embedded.asInstanceOf[TokenSequence[TokenId]] match {
                case null     =>
                case embedded => searchTokenSequence(pr, embedded)
              }
          }
        } while (ts.moveNext)
      }
    }

  }
}
