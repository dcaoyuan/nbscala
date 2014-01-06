/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved.
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
 * Portions Copyrighted 2008 Sun Microsystems, Inc.
 */
package org.netbeans.modules.scala.core

import java.io.File
import java.util.logging.{ Logger, Level }
import org.netbeans.api.java.classpath.ClassPath
import org.netbeans.api.project.{ FileOwnerQuery, Project }
import org.openide.filesystems.FileChangeAdapter
import org.openide.filesystems.FileEvent
import org.openide.filesystems.FileObject
import org.openide.filesystems.FileRenameEvent
import org.openide.filesystems.FileUtil
import org.openide.filesystems.FileChangeListener
import org.openide.util.RequestProcessor
import org.netbeans.modules.scala.core.ast.ScalaAstVisitor
import org.netbeans.modules.scala.core.ast.ScalaItems
import org.netbeans.modules.scala.core.ast.ScalaDfns
import org.netbeans.modules.scala.core.ast.ScalaRefs
import org.netbeans.modules.scala.core.ast.ScalaRootScope
import org.netbeans.modules.scala.core.ast.ScalaUtils
import org.netbeans.modules.scala.core.element.ScalaElements
import org.netbeans.modules.scala.core.element.JavaElements
import org.netbeans.modules.scala.core.interactive.Global
import scala.collection.mutable
import scala.tools.nsc.Settings
import scala.tools.nsc.reporters.Reporter
import scala.reflect.internal.util.{ Position, SourceFile }

/**
 *
 * @author Caoyuan Deng
 */
case class ScalaError(pos: Position, msg: String, severity: org.netbeans.modules.csl.api.Severity, force: Boolean)
case class ErrorReporter(var errors: List[ScalaError] = Nil) extends Reporter {

  override def reset {
    super.reset
    errors = Nil
  }

  def info0(pos: Position, msg: String, severity: Severity, force: Boolean) {
    val sev = toCslSeverity(severity)
    if ((sev ne null) && msg != "this code must be compiled with the Scala continuations plugin enabled") {
      errors ::= ScalaError(pos, msg, sev, force)
    }
  }

  private def toCslSeverity(severity: Severity) = severity match {
    case INFO    => org.netbeans.modules.csl.api.Severity.INFO
    case WARNING => org.netbeans.modules.csl.api.Severity.WARNING
    case ERROR   => org.netbeans.modules.csl.api.Severity.ERROR
    case _       => null
  }
}

class ScalaGlobal(_settings: Settings, _reporter: Reporter, projectName: String = "") extends Global(_settings, _reporter, projectName)
    with ScalaAstVisitor
    with ScalaItems
    with ScalaDfns
    with ScalaRefs
    with ScalaElements
    with JavaElements
    with ScalaUtils {
  override def forInteractive = true

  override def logError(msg: String, t: Throwable) {}

  private val log1 = Logger.getLogger(this.getClass.getName)

  private val sourceToResponse = new java.util.concurrent.ConcurrentHashMap[SourceFile, Response[_]]

  protected def isCancelled(srcFile: SourceFile) = {
    sourceToResponse.get(srcFile) match {
      case null => false
      case resp => resp.isCancelled
    }
  }

  private def newResponse[T](srcFile: SourceFile) = {
    val resp = new Response[T]
    sourceToResponse.put(srcFile, resp)
    resp
  }

  private def resetReporter {
    reporter match {
      case x: ErrorReporter => x.reset
      case _                =>
    }
  }

  def askForReload(srcFiles: List[SourceFile]) {
    resetReporter

    val resp = new Response[Unit]
    askReload(srcFiles, resp)

    resp.get match {
      case Left(_)   =>
      case Right(ex) => processGlobalException(ex)
    }
  }

  /**
   * We should carefully design askForSemantic(.) and tryCancelSemantic(.) to make them thread safe,
   * so tryCancelSemantic could be called during askForSemantic and the rootScope is always which we want.
   * @return    Some root when everything goes smooth
   *            Empty root when exception happens
   *            None when cancelled
   */
  def askForSemantic(srcFile: ScalaSourceFile): Option[ScalaRootScope] = {
    resetReporter // is reporter thread safe? or, since it's a global report, do not need to care.
    qualToRecoveredType.clear

    try {
      val loadResp = newResponse[Unit](srcFile)
      askReload(List(srcFile), loadResp)
      if (isCancelled(srcFile)) return None
      loadResp.get match {
        case Left(_) =>
          val typeResp = newResponse[Tree](srcFile)
          askLoadedTyped(srcFile, typeResp)
          if (isCancelled(srcFile)) return None
          typeResp.get match {
            case Left(rootTree) =>
              val rootResp = newResponse[Option[ScalaRootScope]](srcFile)
              askSemanticRoot(srcFile, rootTree, rootResp)
              if (isCancelled(srcFile)) return None
              rootResp get match {
                case Left(x)   => x
                case Right(ex) => processGlobalException(ex, Some(ScalaRootScope.EMPTY))
              }

            case Right(ex) => processGlobalException(ex, Some(ScalaRootScope.EMPTY))
          }

        case Right(ex) => processGlobalException(ex, Some(ScalaRootScope.EMPTY))
      }

    } finally {
      sourceToResponse.remove(srcFile)
    }
  }

  def processGlobalException[T](ex: Throwable, toReturn: T = ()): T = {
    ex match {
      case _: AssertionError =>
        /**
         * @Note: avoid scala nsc's assert error. Since global's
         * symbol table may have been broken, we have to reset ScalaGlobal
         * to clean this global
         */
        log1.log(Level.WARNING, ex.getMessage, ex)
        ScalaGlobal.resetLate(this, ex)
      case _: java.lang.Error => // avoid scala nsc's Error error
        log1.log(Level.WARNING, ex.getMessage, ex)
      case _: Throwable => // just ignore all ex
        log1.log(Level.WARNING, ex.getMessage, ex)
    }

    toReturn
  }

  private def askSemanticRoot(source: ScalaSourceFile, rootTree: Tree, resp: Response[Option[ScalaRootScope]]) {
    askForResponse(resp) { () =>
      val start = System.currentTimeMillis
      val rootScope = astVisit(source, rootTree)
      log1.info("Visited " + source.file.file.getName + " in " + (System.currentTimeMillis - start) + "ms")
      if (isCancelled(source)) None else Some(rootScope)
    }
  }

  /**
   * Asks for a computation to be done on presentation compiler thread, returning
   *  a response with the result or an exception
   *  Also @see scala.tools.nsc.interactive.CompilerControl#askForResponse
   */
  protected def askForResponse[A](r: Response[A])(op: () => A) = {
    if (onCompilerThread) {
      try { r set op() }
      catch { case exc: Throwable => r raise exc }
      r
    } else {
      val ir = scheduler askDoQuickly op
      ir onComplete {
        case Left(result) => r set result
        case Right(exc)   => r raise exc
      }
      r
    }
  }

  /**
   * @return will cancel or not
   */
  private[core] def tryCancelSemantic(srcFile: SourceFile): Boolean = {
    if (srcFile != null && sourceToResponse.containsKey(srcFile)) {
      sourceToResponse.remove(srcFile)
      log1.info("Will cancel semantic " + srcFile.file.file.getName)
      true
    } else {
      log1.info("Won't cancel semantic " + srcFile.file.file.getName + ", since it's not under semantic.")
      false
    }
  }

  // @Note Should pass phase "lambdalift" to get anonfun's class symbol built
  def compileSourceForDebug(srcFile: ScalaSourceFile): ScalaRootScope = {
    compileSource(srcFile, constructors.phaseName)
  }

  // @Note the following setting excludes 'stopPhase' itself
  def compileSource(source: ScalaSourceFile, stopPhase: String): ScalaRootScope = synchronized {
    resetReporter

    settings.stop.value = Nil
    settings.stop.tryToSetColon(List(stopPhase))
    qualToRecoveredType.clear

    val run = new this.Run
    val srcFiles = List(source)
    try {
      run.compileSources(srcFiles)
    } catch {
      case ex: Throwable => processGlobalException(ex)
    }

    //println("selectTypeErrors:" + selectTypeErrors)

    run.units find { _.source eq source } match {
      case Some(unit) =>
        if (ScalaGlobal.debug) {
          RequestProcessor.getDefault.post(new Runnable {
            def run {
              treeBrowser.browse(unit.body)
            }
          })
        }

        astVisit(source, unit.body)
      case None => ScalaRootScope.EMPTY
    }
  }

}

object ScalaGlobal {
  private val log = Logger.getLogger(this.getClass.getName)

  /** index of globals */
  private val Global = 0
  private val GlobalForTest = 1
  private val GlobalForDebug = 2
  private val GlobalForTestDebug = 3

  private val debug = false

  /** project -> List([global, srcClasspath] */
  private val projectToGlobals = new mutable.WeakHashMap[Project, mutable.HashMap[ScalaGlobal, ClassPath]]
  private var globalToListeners = Map[ScalaGlobal, List[FileChangeListener]]()
  private var globalForStdLib: Option[ScalaGlobal] = None
  private var toResetGlobals = Set[ScalaGlobal]()

  case class NormalReason(msg: String) extends Throwable(msg)
  object userRequest extends NormalReason("User's action")
  object compCpChanged extends NormalReason("Change of compile classpath")

  def resetLate(global: ScalaGlobal, reason: Throwable) = synchronized {
    reason match {
      case NormalReason(msg) => log.info("Will reset global late due to: " + msg)
      case _                 => log.log(Level.WARNING, "Will reset global late due to:", reason)
    }

    if (globalForStdLib.isDefined && global == globalForStdLib.get) {
      globalForStdLib = None
    } else {
      projectToGlobals.keySet foreach { project =>
        val globals = projectToGlobals(project)
        for {
          found <- globals.remove(global)
          listeners <- globalToListeners.get(global)
        } {
          listeners foreach project.getProjectDirectory.getFileSystem.removeFileChangeListener
        }
      }
    }

    toResetGlobals += global
    globalToListeners -= global
  }

  /**
   * @Note
   * Tried to use to reset global instead of create a new one, but for current scala Global,
   * it seems reset operation cannot got a clean global?
   */
  def resetBadGlobals = synchronized {
    for (global <- toResetGlobals) {
      log.info("Reset global: " + global)

      // this will cause global create a new TypeRun so as to release all unitbuf and filebuf.
      // But, it seems askReset will only reset current unit, when exception is throw inside
      // for example, typeCheck, the dependent units may have been damaged, and the symbols in
      // global may need to be reset too. So the best way is to drop this gloal, use a new
      // created one instead.
      //global.askReset

      //global.analyzer.resetTyper
      //global.firsts = Nil
      //global.unitOfFile.clear

      // stop compiler daemon thread
      global.askShutdown
    }

    toResetGlobals = Set[ScalaGlobal]()
  }

  /**
   * Scala's global is not thread safed
   */
  def getGlobal(fo: FileObject): ScalaGlobal = synchronized {
    resetBadGlobals

    val project = FileOwnerQuery.getOwner(fo)
    if (project == null) {
      // it may be a standalone file, or file in standard lib
      return globalForStdLib getOrElse {
        val g = ScalaHome.getGlobalForStdLib
        globalForStdLib = Some(g)
        g
      }
    }

    projectToGlobals.get(project) map { globalToSrcCp =>
      globalToSrcCp find { _._2.contains(fo) } match {
        case Some(xy) => return xy._1
        case None     => // create a new one
      }
    }

    // ----- need to create a new global:

    val settings = new Settings
    if (debug) {
      settings.Yidedebug.value = true
      settings.debug.value = true
      settings.verbose.value = true
    } else {
      settings.Yidedebug.value = false
      settings.debug.value = false
      settings.verbose.value = false
    }

    val bootCp = ClassPath.getClassPath(fo, ClassPath.BOOT)
    val compCp = ClassPath.getClassPath(fo, ClassPath.COMPILE)
    val srcCp = ClassPath.getClassPath(fo, ClassPath.SOURCE)

    // in case of `fo` is in standard libaray jar
    val inStdLib = bootCp == null || compCp == null

    log.info("scala.home: " + System.getProperty("scala.home"))

    // ----- set bootclasspath, classpath

    val bootCpStr = ScalaExecution.toClassPathString(bootCp)
    settings.bootclasspath.value = bootCpStr

    val compCpStr = ScalaExecution.toClassPathString(compCp)
    settings.classpath.value = compCpStr

    // Should override extdirs to empty, otherwise all jars under scala.home/lib will be added
    // which brings unwanted scala runtime (scala runtime should be set in compCpStr).
    // @see scala.tools.nsc.Settings#extdirsDefault
    settings.extdirs.value = ""

    // Should explictly set the pluginsDir, otherwise the default will be set to scala.home/misc
    // which may bring uncompitable verions of scala's runtime call
    // @see scala.tools.util.PathResolver.Defaults
    val pluginJarsDir = ScalaExecution.getPluginJarsDir
    log.info("Bundled plugin jars dir is: " + pluginJarsDir)
    settings.pluginsDir.value = pluginJarsDir map (_.getAbsolutePath) getOrElse ""
    settings.plugin.value = Nil

    // ----- set sourcepath, outpath

    val srcOutDirsPath = srcCp.getRoots map { srcRoot => (FileUtil.toFile(srcRoot).getAbsolutePath, FileUtil.toFile(ProjectResources.findOutDir(project, srcRoot)).getAbsolutePath) }
    // @Note do not add src path to global for test?, since the corresponding build/classes has been added to compCp
    //val srcOutDirsPath = (if (isForTest) resource.testSrcOutDirsPath else resource.mainSrcOutDirsPath)
    // @Note settings.outputDirs.add(src, out) seems cannot resolve symbols in other source files, why?\
    // since Scala 2.10.0, this seems working. if not, we should global askForReload srcFiles later
    srcOutDirsPath foreach { src_out =>
      settings.outputDirs.add(src_out._1, src_out._2)
      log.info("settings.outputDirs.add" + (src_out._1 -> src_out._2))
    }
    settings.sourcepath.value = srcOutDirsPath.toList map (_._1) mkString ("", File.pathSeparator, "")

    // we need at least one existed out path
    // if we do not set outdir explicitly, it will now point to ".", that's ok for presentation compiler
    //settings.outdir.value = srcOutDirsPath find {x => try {new File(x._2).exists} catch {case _: Throwable => false}} getOrElse "."

    // ----- now, the new global

    // Setter of Global.reporter is useless due to interative.Global's direct reference
    // to the constructor's param reporter, so we have to make sure only one reporter
    // is assigned to Global (during create new instance)
    val global = new ScalaGlobal(settings, ErrorReporter())
    projectToGlobals.getOrElseUpdate(project, new mutable.HashMap[ScalaGlobal, ClassPath]) += (global -> srcCp)

    // listen to compCp's change
    if (compCp != null) {
      val compCpListener = new CompCpListener(global, compCp)

      globalToListeners += (global -> (compCpListener :: globalToListeners.getOrElse(global, Nil)))
      project.getProjectDirectory.getFileSystem.addFileChangeListener(compCpListener)
    }

    if (srcCp != null) {
      log.info(srcCp.getRoots.map(_.getPath).mkString("Project's srcCp: [", ", ", "]"))

      val srcCpListener = new SrcCpListener(global, srcCp)
      globalToListeners += (global -> (srcCpListener :: globalToListeners.getOrElse(global, Nil)))
      project.getProjectDirectory.getFileSystem.addFileChangeListener(srcCpListener)

      // we have to do following step to get mixed java sources visible to scala sources
      // since scala 2.10.0, this does not seem to be necessary.

      // should push java srcs before scala srcs
      // push scala src files to get classes that with different name from file name to be recognized properly
      // the reporter should be set previous, otherwise, no java source is resolved, may throw exception already.
      //val (javaSrcs, scalaSrcs) = ProjectResources.findAllSources(srcCp)
      //val srcFiles = (javaSrcs ++ scalaSrcs).toList map toSourceFile
      //global askForReload srcFiles
    } else {
      log.warning("Project's srcCp is null !")
    }

    log.info("Project's global.settings: " + global.settings)
    global
  }

  private def toSourceFile(fo: FileObject): ScalaSourceFile = {
    val sourceFile = ScalaSourceFile.sourceFileOf(fo)
    sourceFile.refreshSnapshot
    sourceFile
  }

  private class SrcCpListener(global: ScalaGlobal, srcCp: ClassPath) extends FileChangeAdapter {
    val JavaMimeType = "text/x-java"
    val ScalaMimeType = "text/x-scala"
    val srcRoots = srcCp.getRoots

    private def isUnderSrcDir(fo: FileObject) = {
      srcRoots exists { x => FileUtil.isParentOf(x, fo) }
    }

    private def isInterestedMime(mimeType: String) = mimeType match {
      case JavaMimeType | ScalaMimeType => true
      case _                            => false
    }

    override def fileDataCreated(fe: FileEvent) {
      val fo = fe.getFile
      if (isInterestedMime(fo.getMIMEType) && isUnderSrcDir(fo) && global != null) {
        global askForReload List(toSourceFile(fo))
      }
    }

    override def fileChanged(fe: FileEvent) {
      val fo = fe.getFile
      if (isInterestedMime(fo.getMIMEType) && isUnderSrcDir(fo) && global != null) {
        global askForReload List(toSourceFile(fo))
      }
    }

    override def fileRenamed(fe: FileRenameEvent) {
      val fo = fe.getFile
      if (isInterestedMime(fo.getMIMEType) && isUnderSrcDir(fo) && global != null) {
        global askForReload List(toSourceFile(fo))
      }
    }

    override def fileDeleted(fe: FileEvent) {
      val fo = fe.getFile
      if (isInterestedMime(fo.getMIMEType) && isUnderSrcDir(fo) && global != null) {
        val resp = new global.Response[Unit]
        global askFilesDeleted (List(toSourceFile(fo)), resp)
        resp.get match {
          case Left(_)   =>
          case Right(ex) => global.processGlobalException(ex)
        }
      }
    }
  }

  private class CompCpListener(global: ScalaGlobal, compCp: ClassPath) extends FileChangeAdapter {
    val compRoots = compCp.getRoots

    private def isUnderCompCp(fo: FileObject) = {
      // when there are series of folder/file created, only top created folder can be listener
      val found = compRoots find { x => FileUtil.isParentOf(fo, x) || x == fo }
      if (found.isDefined) log.finest("under compCp: fo=" + fo + ", found=" + found)
      found isDefined
    }

    override def fileFolderCreated(fe: FileEvent) {
      val fo = fe.getFile
      if (isUnderCompCp(fo) && (global ne null)) {
        log.finest("folder created: " + fo)
        resetLate(global, compCpChanged)
      }
    }

    override def fileDataCreated(fe: FileEvent) {
      val fo = fe.getFile
      if (isUnderCompCp(fo) && (global ne null)) {
        log.finest("data created: " + fo)
        resetLate(global, compCpChanged)
      }
    }

    override def fileChanged(fe: FileEvent) {
      val fo = fe.getFile
      if (isUnderCompCp(fo) && (global ne null)) {
        log.finest("file changed: " + fo)
        resetLate(global, compCpChanged)
      }
    }

    override def fileRenamed(fe: FileRenameEvent) {
      val fo = fe.getFile
      if (isUnderCompCp(fo) && (global ne null)) {
        log.finest("file renamed: " + fo)
        resetLate(global, compCpChanged)
      }
    }

    override def fileDeleted(fe: FileEvent) {
      val fo = fe.getFile
      if (isUnderCompCp(fo) && (global ne null)) {
        log.finest("file deleted: " + fo)
        resetLate(global, compCpChanged)
      }
    }
  }
}
