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

package org.netbeans.modules.scala.editor.overridden

import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.text.BadLocationException;
import javax.swing.text.Position;
import javax.swing.text.StyledDocument;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.queries.SourceForBinaryQuery;
import org.netbeans.api.java.source.CancellableTask;
import org.netbeans.api.java.source.Task;
import org.netbeans.api.java.source.ClassIndex.SearchKind;
import org.netbeans.api.java.source.ClasspathInfo;
import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.api.java.source.SourceUtils;
import org.netbeans.api.java.source.ClassIndex;
import org.netbeans.modules.parsing.api.{ResultIterator}
import org.netbeans.modules.parsing.spi.{ParserResultTask, ParseException, Scheduler, SchedulerEvent}
import org.netbeans.modules.parsing.spi.SchedulerTask
import org.netbeans.spi.java.classpath.support.ClassPathSupport
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.text.NbDocument;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.util.TopologicalSortException;
import org.openide.util.Utilities;
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet

import org.netbeans.modules.scala.core.{ScalaMimeResolver, ScalaParserResult}
import org.netbeans.modules.scala.core.ast.{ScalaRootScope}
import scala.tools.nsc.symtab.Flags
import scala.tools.nsc.symtab.Symbols

/**
 *
 * @author Jan Lahoda
 */
object IsOverriddenAnnotationHandler {
  private val enableReverseLookups = java.lang.Boolean.getBoolean("org.netbeans.java.editor.enableReverseLookups")
  val Log = Logger.getLogger(classOf[IsOverriddenAnnotationHandler].getName)

  /*   def detectOverrides(info: ScalaParserResult, tpe: TypeElement, ee: ExecutableElement, result: ArrayBuffer[ElementDescription]): AnnotationType = {
   val nameToMethod = new HashMap[Name, List[ExecutableElement]]

   sortOutMethods(info, nameToMethod, tpe, false)

   val lee = nameToMethod.get(ee.getSimpleName).getOrElse(return null)

   val seenMethods = new HashSet[ExecutableElement]

   for (overridee <- lee) {
   if (info.getElements().overrides(ee, overridee, SourceUtils.getEnclosingTypeElement(ee))) {
   if (seenMethods.add(overridee)) {
   result.add(new ElementDescription(info, overridee))
   }
   }
   }

   if (!result.isEmpty) {
   for (ed <- result) {
   if (!ed.getModifiers.contains(Modifier.ABSTRACT)) {
   return AnnotationType.OVERRIDES
   }
   }

   AnnotationType.IMPLEMENTS
   } else null
   }

   private def sortOutMethods(info: ScalaParserResult, where: HashMap[Name, List[ExecutableElement]], td: Element, current: Boolean) {
   if (current) {
   val newlyAdded = new HashMap[Name, List[ExecutableElement]]


   /* OUTTER: for (ExecutableElement ee : ElementFilter.methodsIn(td.getEnclosedElements())) {
    Name name = ee.getSimpleName();
    List<ExecutableElement> alreadySeen = where.get(name);

    if (alreadySeen != null) {
    for (ExecutableElement seen : alreadySeen) {
    if (info.getElements().overrides(seen, ee, (TypeElement) seen.getEnclosingElement())) {
    continue OUTTER; //a method that overrides this one was already handled, ignore
    }
    }
    }
    } */

   val lee = newlyAdded.get(name)

   if (lee == null) {
   newlyAdded.put(name, lee = new ArrayBuffer<ExecutableElement>());
   }

   lee.add(ee);

   for ((k, v) <- newlyAdded) {
   val lee = where.get(k)

   if (lee == null) {
   where += (k, v)
   } else {
   lee ++= (v)
   }
   }
   }

   for (TypeMirror superType : info.getTypes().directSupertypes(td.asType())) {
   if (superType.getKind() == TypeKind.DECLARED) {
   sortOutMethods(info, where, ((DeclaredType) superType).asElement(), true);
   }
   }
   } */

  private def getPosition(doc: StyledDocument, offset: Int): Position = {
    val task = new Runnable {
      var pos: Position = _
      def run {
        if (offset < 0 || offset >= doc.getLength) return
        try {
          pos = doc.createPosition(offset - NbDocument.findLineColumn(doc, offset))
        } catch {case ex: BadLocationException => Log.log(Level.FINE, null, ex)} //should not happen?
      }
    }

    doc.render(task)

    task.pos
  }

}

class IsOverriddenAnnotationHandler(file: FileObject) extends ParserResultTask[ScalaParserResult] {
  import IsOverriddenAnnotationHandler._

  Logger.getLogger("TIMER").log(Level.FINE, "IsOverriddenAnnotationHandler", Array(file, this).asInstanceOf[Array[Object]]) //NOI18N

  private var canceled: Boolean = _

  private var results: List[IsOverriddenAnnotation] = Nil

  override def getPriority: Int = 0

  override def getSchedulerClass: Class[_ <: Scheduler] = {
    Scheduler.EDITOR_SENSITIVE_TASK_SCHEDULER
  }

  override def cancel: Unit = synchronized {
    canceled = true
    wakeUp
  }

  private def resume: Unit = synchronized {
    canceled = false
  }

  private def wakeUp: Unit = synchronized {
    notifyAll
  }

  private def isCanceled: Boolean = synchronized {
    canceled
  }

  @throws(classOf[ParseException])
  override def run(pr: ScalaParserResult, event: SchedulerEvent) {
    resume

    val startTime = System.currentTimeMillis
    try {
      results = process(pr.asInstanceOf[ScalaParserResult])
      
      if (results.isEmpty) return
            
      newAnnotations(results)
    } finally {
      Logger.getLogger("TIMER").log(Level.FINE, "Overridden in", //NOI18N
                                    Array(file, System.currentTimeMillis - startTime).asInstanceOf[Array[Object]])
    }
  }

  private def newAnnotations(as: List[IsOverriddenAnnotation]) {
    AnnotationsHolder(file) match {
      case null =>
      case x => x.setNewAnnotations(as)
    }
  }

  protected def process(pr: ScalaParserResult): List[IsOverriddenAnnotation] = {
    synchronized {
      if (isCanceled) return Nil
    }

    val root = pr.rootScope
    val th = pr.getSnapshot.getTokenHierarchy

    if (root == ScalaRootScope.EMPTY || th == null) return Nil
    
    val doc = pr.getSnapshot.getSource.getDocument(true) match {
      case x: StyledDocument => x
      case _ => return Nil
    }

    val global = pr.global
    
    val thisSourceRoot = findSourceRoot
    if (thisSourceRoot == null) return Nil
    
    val reverseSourceRoots = if (enableReverseLookups) {
      //XXX: special case "this" source root (no need to create a new JS and load the classes again for it):
      findReverseSourceRoots(thisSourceRoot, pr.getSnapshot.getSource.getFileObject) + thisSourceRoot
    } else null
        
    Log.log(Level.FINE, "reverseSourceRoots: {0}", reverseSourceRoots) //NOI18N
        
    val annotations = new ArrayBuffer[IsOverriddenAnnotation]

    for ((idToken, items) <- root.idTokenToItems;
         item <- items if item.isInstanceOf[global.ScalaDfn];
         sym = item.asInstanceOf[global.ScalaDfn].symbol if sym != global.NoSymbol;
         pos = getPosition(doc, item.idOffset(th)) if pos != null
    ) {
      if (isCanceled) return Nil

      val overridees = sym.allOverriddenSymbols
      if (!overridees.isEmpty) {
        val seenMethods = new HashSet[global.Symbol]
        val descs = overridees filter (seenMethods add _) map (x =>
          new ElementDescription(global.ScalaElement(x, pr))
        )

        if (!descs.isEmpty) {
          val tooltip = new StringBuffer
          var wasOverrides = false
                    
          var newline = false
                    
          for (desc <- descs) {
            if (newline) {
              tooltip.append("\n") //NOI18N
            }

            newline = true

            if (desc.handle.symbol.hasFlag(Flags.DEFERRED)) {
              tooltip.append(NbBundle.getMessage(classOf[IsOverriddenAnnotationHandler], "TP_Implements", desc.getDisplayName))
            } else {
              tooltip.append(NbBundle.getMessage(classOf[IsOverriddenAnnotationHandler], "TP_Overrides",  desc.getDisplayName))
              wasOverrides = true
            }
          }
                    
          annotations += (new IsOverriddenAnnotation(doc, pos, if (wasOverrides) AnnotationType.OVERRIDES else AnnotationType.IMPLEMENTS, tooltip.toString, descs))
        }
      }
    }
        
    /* for (td <- v.type2Declaration.keySet) {
     if (isCanceled)
     return Nil

     Log.log(Level.FINE, "type: {0}", td.getQualifiedName) //NOI18N

     val name2Method = new HashMap[Name, List[ExecutableElement]]

     val resolvedType = td.resolve(info);

     if (resolvedType == null)
     continue;

     sortOutMethods(info, name2Method, resolvedType, false)

     for (methodHandle <- v.type2Declaration.get(td).get) {
     if (isCanceled)
     return Nil

     val ee = methodHandle.resolve(info);

     if (ee == null)
     continue;

     Log.log(Level.FINE, "method: {0}", ee.toString()) //NOI18N

     val lee = name2Method.get(ee.getSimpleName()).getOrElse(null);

     if (lee == null || lee.isEmpty) {
     continue;
     }

     val seenMethods = new HashSet[ExecutableElement]();
     val overrides = new ArrayBuffer[ElementDescription]();

     for (overridee <- lee) {
     if (info.getElements().overrides(ee, overridee, SourceUtils.getEnclosingTypeElement(ee))) {
     if (seenMethods.add(overridee)) {
     overrides += (new ElementDescription(info, overridee))
     }
     }
     }

     if (!overrides.isEmpty) {
     val position: Int = info.getTrees().getSourcePositions().getStartPosition(info.getCompilationUnit(), v.declaration2Tree.get(methodHandle).getOrElse(null));
     val pos = getPosition(doc, position);

     if (pos == null) {
     //cannot compute the position, skip
     continue;
     }

     val tooltip = new StringBuffer();
     var wasOverrides = false;

     var newline = false;

     for (ed <- overrides) {
     if (newline) {
     tooltip.append("\n") //NOI18N
     }

     newline = true;

     if (ed.getModifiers.contains(Modifier.ABSTRACT)) {
     tooltip.append(NbBundle.getMessage(classOf[IsOverriddenAnnotationHandler], "TP_Implements", ed.getDisplayName));
     } else {
     tooltip.append(NbBundle.getMessage(classOf[IsOverriddenAnnotationHandler], "TP_Overrides", ed.getDisplayName));
     wasOverrides = true;
     }
     }

     annotations += (new IsOverriddenAnnotation(doc, pos, if (wasOverrides) AnnotationType.OVERRIDES else AnnotationType.IMPLEMENTS, tooltip.toString, overrides));
     }
     }

     if (enableReverseLookups) {
     var typeOverridden: String = null;
     var typeType: AnnotationType = null;
     var resolved: TypeElement = td.resolve(info);


     if (resolved == null) {
     Logger.getLogger("global").log(Level.SEVERE, "IsOverriddenAnnotationHandler: resolved == null!"); //NOI18N
     continue;
     }

     if (resolved.getKind().isInterface()) {
     typeOverridden = NbBundle.getMessage(classOf[IsOverriddenAnnotationHandler], "CAP_HasImplementations");
     typeType = AnnotationType.HAS_IMPLEMENTATION;
     }

     if (resolved.getKind().isClass()) {
     typeOverridden = NbBundle.getMessage(classOf[IsOverriddenAnnotationHandler], "CAP_IsOverridden");
     typeType = AnnotationType.IS_OVERRIDDEN;
     }

     val overriding = new HashMap[ElementHandle[ExecutableElement], List[ElementDescription]]();
     val overridingClasses = new ArrayBuffer[ElementDescription]();

     val startTime = System.currentTimeMillis();
     val classIndexTime = Array(0L)
     val users = computeUsers(reverseSourceRoots, ElementHandle.create(resolved), classIndexTime);
     val endTime = System.currentTimeMillis();

     if (users == null) {
     return Nil
     }

     Logger.getLogger("TIMER").log(Level.FINE, "Overridden Users Class Index", //NOI18N
     Array(file, classIndexTime(0)).asInstanceOf[Array[Object]]);
     Logger.getLogger("TIMER").log(Level.FINE, "Overridden Users", //NOI18N
     Array(file, endTime - startTime).asInstanceOf[Array[Object]]);

     for ((k, v) <- users) {
     if (isCanceled)
     return Nil

     findOverriddenAnnotations(k, v, td, v.type2Declaration.get(td).get, overriding, overridingClasses);
     }

     if (!overridingClasses.isEmpty) {
     val t = v.declaration2Class.get(td).getOrElse(null)

     if (t != null) {
     val pos = getPosition(doc, info.getTrees().getSourcePositions().getStartPosition(unit, t).toInt);

     if (pos == null) {
     //cannot compute the position, skip
     continue;
     }

     annotations += (new IsOverriddenAnnotation(doc, pos, typeType, typeOverridden.toString(), overridingClasses));
     }
     }

     for (original <- overriding.keySet) {
     if (isCanceled)
     return Nil

     val pos = getPosition(doc, info.getTrees().getSourcePositions().getStartPosition(unit, v.declaration2Tree.get(original).getOrElse(null)).toInt);

     if (pos == null) {
     //cannot compute the position, skip
     continue;
     }

     val mods = original.resolve(info).getModifiers();
     var tooltip: String = null;

     if (mods.contains(Modifier.ABSTRACT)) {
     tooltip = NbBundle.getMessage(classOf[IsOverriddenAnnotationHandler], "TP_HasImplementations");
     } else {
     tooltip = NbBundle.getMessage(classOf[IsOverriddenAnnotationHandler], "TP_IsOverridden");
     }

     val ann = new IsOverriddenAnnotation(doc, pos, if (mods.contains(Modifier.ABSTRACT)) AnnotationType.HAS_IMPLEMENTATION else AnnotationType.IS_OVERRIDDEN, tooltip, overriding.get(original).getOrElse(null));

     annotations += (ann)
     }
     }
     } */
        
    if (isCanceled) Nil else annotations.toList
  }

  private def findSourceRoot: FileObject = {
    // null is a valid value for files which have no source path (default filesystem).
    ClassPath.getClassPath(file, ClassPath.SOURCE) match {
      case null => null
      case cp => cp.getRoots find (root => FileUtil.isParentOf(root, file)) getOrElse null
    }
  }

  //temporary hack:
  private def findReverseSourceRoots(thisSourceRoot: FileObject, thisFile: FileObject): Set[FileObject] = synchronized {
    val o = new Object
    val reverseSourceRoots = new HashSet[FileObject]

    RequestProcessor.getDefault.post(new Runnable {
        def run {
          val startTime = System.currentTimeMillis
          val reverseSourceRootsInt = new HashSet[FileObject] ++= ReverseSourceRootsLookup.reverseSourceRootsLookup(thisSourceRoot)
          val endTime = System.currentTimeMillis

          Logger.getLogger("TIMER").log(Level.FINE, "Find Reverse Source Roots", //NOI18N
                                        Array(thisFile, endTime - startTime).asInstanceOf[Array[Object]])

          o synchronized {
            reverseSourceRoots ++= reverseSourceRootsInt
          }

          wakeUp
        }
      })

    try {
      wait
    } catch {case ex: InterruptedException => Exceptions.printStackTrace(ex)}

    reverseSourceRoots.toSet
  }



  /*   private val EMPTY = ClassPathSupport.createClassPath(Array[URL]())

   private def computeUsers(source: FileObject, base: Set[ElementHandle[TypeElement]], classIndexCumulative: Array[Long]): Set[ElementHandle[TypeElement]] = {
   val cpinfo = ClasspathInfo.create(/*source);*/EMPTY, EMPTY, ClassPathSupport.createClassPath(Array(source)));

   val startTime = System.currentTimeMillis

   try {
   val l = new ArrayBuffer[ElementHandle[TypeElement]] ++= base
   val result = new HashSet[ElementHandle[TypeElement]]();

   while (!l.isEmpty) {
   val eh = l.remove(0);

   result.add(eh);
   val typeElements = cpinfo.getClassIndex().getElements(eh, Collections.singleton(SearchKind.IMPLEMENTORS), EnumSet.of(ClassIndex.SearchScope.SOURCE));
   //XXX: Canceling
   if (typeElements != null) {
   l ++= (typeElements);
   }
   }
   return result.toSet
   } finally {
   classIndexCumulative(0) += (System.currentTimeMillis() - startTime)
   }
   }

   private def computeUsers(sources: Set[FileObject], base: ElementHandle[TypeElement], classIndexCumulative: Array[Long]): Map[FileObject, Set[ElementHandle[TypeElement]]] = {
   val edges = new HashMap[FileObject, Collection[FileObject]]();
   val dependsOn = new HashMap[FileObject, Collection[FileObject]]();

   for (source <- sources) {
   edges.put(source, new ArrayBuffer[FileObject]());
   }

   for (source <- sources) {
   val deps = new ArrayBuffer[FileObject]();

   dependsOn.put(source, deps);

   for (entry <- ClassPath.getClassPath(source, ClassPath.COMPILE).entries()) { //TODO: should also check BOOT?
   for (s <- SourceForBinaryQuery.findSourceRoots(entry.getURL()).getRoots()) {
   val targets = edges.get(s);

   if (targets != null) {
   targets.add(source);
   }

   deps.add(s);
   }
   }
   }

   val sourceRoots = new ArrayBuffer[FileObject] ++= sources

   try {
   Utilities.topologicalSort(sourceRoots, edges);
   } catch {case ex: TopologicalSortException =>
   Log.log(Level.WARNING, "internal error", ex); //NOI18N
   return null;
   }

   val result = new HashMap[FileObject, Set[ElementHandle[TypeElement]]]();

   for (file <- sourceRoots) {
   val baseTypes = new HashSet[ElementHandle[TypeElement]];

   baseTypes.add(base);

   for (dep <- dependsOn.get(file)) {
   val depTypes = result.get(dep);

   if (depTypes != null) {
   baseTypes.addAll(depTypes);
   }
   }

   val types = computeUsers(file, baseTypes, classIndexCumulative);

   types.removeAll(baseTypes);

   result.put(file, types);
   }

   result.to
   }

   private def findOverriddenAnnotations(sourceRoot: FileObject,
   users: Set[ElementHandle[TypeElement]],
   originalType: ElementHandle[TypeElement] ,
   methods: List[ElementHandle[ExecutableElement]],
   overriding: Map[ElementHandle[ExecutableElement], List[ElementDescription]] ,
   overridingClasses: List[ElementDescription]) {
   val cpinfo = ClasspathInfo.create(sourceRoot)

   if (!users.isEmpty) {
   val js = JavaSource.create(cpinfo);

   try {
   js.runUserActionTask(new Task[CompilationController] {

   @throws(classOf[Exception])
   def run(controller: CompilationController) {
   val seenElements = new HashSet[Element]();

   for (typeHandle <- users) {
   if (isCanceled)
   return;
   val tpe = typeHandle.resolve(controller);
   val resolvedOriginalType = originalType.resolve(controller);

   if (!seenElements.add(resolvedOriginalType))
   continue;

   if (controller.getTypes().isSubtype(tpe.asType(), resolvedOriginalType.asType())) {
   overridingClasses.add(new ElementDescription(controller, tpe));

   for (originalMethodHandle <- methods) {
   val originalMethod = originalMethodHandle.resolve(controller);

   if (originalMethod != null) {
   val overrider = getImplementationOf(controller, originalMethod, tpe);

   if (overrider == null)
   continue;

   val overriddingMethods = overriding.get(originalMethodHandle);

   if (overriddingMethods == null) {
   overriding.put(originalMethodHandle, overriddingMethods = new ArrayBuffer<ElementDescription>());
   }

   overriddingMethods.add(new ElementDescription(controller, overrider));
   } else {
   Logger.getLogger("global").log(Level.SEVERE, "IsOverriddenAnnotationHandler: originalMethod == null!"); //NOI18N
   }
   }
   }
   }
   }
   },true);
   } catch {case ex: Exception =>
   Exceptions.printStackTrace(ex);
   }
   }
   }

   private def getImplementationOf(info: CompilationInfo, overridee: ExecutableElement, implementor: TypeElement): ExecutableElement = {
   for (overrider <- ElementFilter.methodsIn(implementor.getEnclosedElements)) {
   if (info.getElements().overrides(overrider, overridee, implementor)) {
   return overrider;
   }
   }

   return null;
   } */
            
}
