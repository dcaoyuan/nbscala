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

import java.io.{File, IOException}
import java.net.{MalformedURLException, URISyntaxException, URL}
import java.util.logging.{Logger, Level}
import org.netbeans.api.java.classpath.ClassPath
import org.netbeans.api.lexer.{TokenHierarchy}
import org.netbeans.api.project.{FileOwnerQuery, Project, ProjectUtils}
import org.netbeans.spi.java.queries.BinaryForSourceQueryImplementation
import org.openide.filesystems.{FileChangeAdapter, FileEvent, FileObject, FileRenameEvent,
                                FileStateInvalidException, FileUtil, JarFileSystem, FileChangeListener}
import org.openide.util.{Exceptions, RequestProcessor, Utilities}

import org.netbeans.modules.scala.core.ast.{ScalaItems, ScalaDfns, ScalaRefs, ScalaRootScope, ScalaAstVisitor, ScalaUtils}
import org.netbeans.modules.scala.core.element.{ScalaElements, JavaElements}

import scala.collection.mutable.{ArrayBuffer, LinkedHashMap, WeakHashMap}

import scala.tools.nsc.{Settings}

import org.netbeans.modules.scala.core.interactive.Global
import scala.tools.nsc.io.AbstractFile
import scala.tools.nsc.io.PlainFile
import scala.tools.nsc.reporters.{Reporter}
import scala.tools.nsc.util.{Position, SourceFile}

/**
 *
 * @author Caoyuan Deng
 */
object ScalaGlobal {

  private val logger = Logger.getLogger(this.getClass.getName)

  private val nbUserPath = System.getProperty("netbeans.user")
  
  /** index of globals */
  private val Global = 0
  private val GlobalForTest = 1
  private val GlobalForDebug = 2
  private val GlobalForTestDebug = 3

  private class DirResource {
    var srcToOut:  Map[FileObject, FileObject] = Map()
    var testToOut: Map[FileObject, FileObject] = Map()

    def srcOutDirsPath = toDirPaths(srcToOut)
    def testSrcOutDirsPath = toDirPaths(testToOut)

    def scalaSrcToOut:  Map[AbstractFile, AbstractFile] = toScalaDirs(srcToOut)
    def scalaTestToOut: Map[AbstractFile, AbstractFile] = toScalaDirs(testToOut)

    private def toDirPaths(dirs: Map[FileObject, FileObject]): Map[String, String] = {
      for ((src, out) <- dirs) yield (toDirPath(src), toDirPath(out))
    }

    private def toScalaDirs(dirs: Map[FileObject, FileObject]): Map[AbstractFile, AbstractFile] = {
      for ((src, out) <- dirs) yield (toScalaDir(src), toScalaDir(out))
    }

    private def toDirPath(fo: FileObject) = FileUtil.toFile(fo).getAbsolutePath
    private def toScalaDir(fo: FileObject) = AbstractFile.getDirectory(FileUtil.toFile(fo))
  }
  
  private val debug = false

  private val projectToResources = new WeakHashMap[Project, DirResource]
  private val projectToGlobals = new WeakHashMap[Project, Array[ScalaGlobal]]
  private var globalToListeners = Map[ScalaGlobal, List[FileChangeListener]]()
  private var globalForStdLib: Option[ScalaGlobal] = None
  private var toResetGlobals = Map[ScalaGlobal, Project]()

  val dummyReporter = new Reporter {def info0(pos: Position, msg: String, severity: Severity, force: Boolean) {}}

  case class NormalReason(msg: String) extends Throwable(msg)
  object userRequest extends NormalReason("User's action")
  object compCpChanged extends NormalReason("Change of compile classpath")

  def resetLate(global: ScalaGlobal, reason: Throwable) = synchronized {
    reason match {
      case NormalReason(msg) => logger.info("Will reset global late due to: " + msg)
      case _ => logger.log(Level.WARNING, "Will reset global late due to:", reason)
    }

    if (globalForStdLib.isDefined && global == globalForStdLib.get) {
      globalForStdLib = None
    } else {
      projectToGlobals foreach {case (project, globals) =>
          var found = false
          var i = 0
          val len = globals.length
          while (i < len && !found) {
            if (globals(i) == global) {
              globals(i) = null
              toResetGlobals += (global -> project)
              globalToListeners.get(global) foreach {xs =>
                xs foreach {x =>
                  project.getProjectDirectory.getFileSystem.removeFileChangeListener(x)
                }
              }
              globalToListeners -= global
              found = true
            }
            i += 1
          }
      }
    }
  }

  /**
   * @Note
   * Tried to use to reset global instead of create a new one, but for current scala Global,
   * it seems reset operation cannot got a clean global?
   */
  def resetBadGlobals = synchronized {
    for ((global, project) <- toResetGlobals) {
      logger.info("Reset global: " + global)

      projectToResources.remove(project)
      
      // * this will cause global create a new TypeRun so as to release all unitbuf and filebuf.
      // * But, it seems askReset will only reset current unit, when exception is throw inside
      // * for example, typeCheck, the dependent units may have been damaged, and the symbols in
      // * global may need to be reset too. So the best way is to drop this gloal, use a new
      // * created one instead.
      //global.askReset
      
      //global.analyzer.resetTyper
      //global.firsts = Nil
      //global.unitOfFile.clear

      // * stop compiler daemon thread
      global.askShutdown
    }

    toResetGlobals = Map[ScalaGlobal, Project]()
  }

  private def getResourceOf(fo: FileObject, refresh: Boolean): Map[FileObject, FileObject] = {
    val project = FileOwnerQuery.getOwner(fo)
    if (project == null) {
      // * it may be a standalone file, or file in standard lib
      return Map()
    }

    val resource = if (refresh ) {
      findDirResource(project)
    } else {
      projectToResources.get(project) getOrElse findDirResource(project)
    }

    if (isForTest(resource, fo)) resource.testToOut else resource.srcToOut
  }

  def getSrcFileObjects(fo: FileObject, refresh: Boolean): Array[FileObject] = {
    val resource = getResourceOf(fo, refresh)
    val srcPaths = for ((src, out) <- resource) yield src

    srcPaths.toArray
  }

  def getOutFileObject(fo: FileObject, refresh: Boolean): Option[FileObject] = {
    val resource = getResourceOf(fo, refresh)
    for ((src, out) <- resource) {
      try {
        val file = FileUtil.toFile(out)
        if (!file.exists) file.mkdirs
      } catch {case _ =>}

      return Some(out)
    }

    None
  }

  private def isForTest(resource: DirResource, fo: FileObject) = {
    // * is this `fo` under test source?
    resource.testToOut exists {case (src, _) => src.equals(fo) || FileUtil.isParentOf(src, fo)}
  }

  /**
   * Scala's global is not thread safed
   */
  def getGlobal(fo: FileObject, forDebug: Boolean = false): ScalaGlobal = synchronized {
    resetBadGlobals
    
    val project = FileOwnerQuery.getOwner(fo)
    if (project == null) {
      // * it may be a standalone file, or file in standard lib
      return globalForStdLib getOrElse {
        val g = ScalaHome.getGlobalForStdLib
        globalForStdLib = Some(g)
        g
      }
    }

    val resource = projectToResources.get(project) getOrElse {
      val x = findDirResource(project)
      projectToResources += (project -> x)
      x
    }

    val forTest = isForTest(resource, fo)

    // * Do not use `srcCp` as the key, different `fo` under same src dir seems returning diff instance of srcCp
    val idx = if (forDebug) {
      if (forTest) GlobalForTestDebug else GlobalForDebug
    } else {
      if (forTest) GlobalForTest else Global
    }

    val globals = projectToGlobals.get(project) getOrElse {
      val x = new Array[ScalaGlobal](4)
      projectToGlobals += (project -> x)
      x
    }

    globals(idx) match {
      case null =>
      case g => return g
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

    var bootCp = ClassPath.getClassPath(fo, ClassPath.BOOT)
    var compCp = ClassPath.getClassPath(fo, ClassPath.COMPILE)
    val srcCp  = ClassPath.getClassPath(fo, ClassPath.SOURCE)

    val inStdLib =
      if (bootCp == null || compCp == null) {
        true // * in case of `fo` in standard libaray
      } else false

    logger.info("scala.home: " + System.getProperty("scala.home"))

    // ----- set bootclasspath, classpath
    
    val bootCpStr = toClassPathString(project, bootCp)
    settings.bootclasspath.value = bootCpStr
    logger.info("project's bootclasspath: " + settings.bootclasspath.value)

    val compCpStr = toClassPathString(project, compCp)
    settings.classpath.value = compCpStr
    logger.info("project's classpath: " + settings.classpath.value)

    // * should set extdirs to empty, otherwise all jars under scala.home/lib will be added
    // * which brings unwanted scala runtime (scala runtime should be set in compCpStr).
    // * @see scala.tools.nsc.Settings#extdirsDefault
    settings.extdirs.value = ""

    // ----- set sourcepath, outpath
    
    var outPath = ""
    var srcPaths: List[String] = Nil
    for ((src, out) <- if (forTest) resource.testSrcOutDirsPath else resource.srcOutDirsPath) {
      srcPaths ::= src

      // * we only need one out path
      if (outPath == "") {
        outPath = out

        // * Had out path dir been deleted? (a clean task etc), if so, create it, since scalac
        // * can't parse anything correctly without an exist out dir (sounds a bit strange)
        try {
          val file = new File(outPath)
          if (!file.exists) file.mkdirs
        } catch {case _ =>}
      }
    }

    // * @Note: do not add src path to global for test, since the corresponding build/classes has been added to compCp

    settings.sourcepath.tryToSet(srcPaths.reverse)
    settings.outdir.value = outPath

    logger.info("project's source paths set for global: " + srcPaths)
    logger.info("project's output paths set for global: " + outPath)
    if (srcCp != null){
      logger.info(srcCp.getRoots.mkString("project's srcCp: [", ", ", "]"))
    } else {
      logger.info("project's srcCp is null !")
    }
    
    // * @Note: settings.outputDirs.add(src, out) seems cannot resolve symbols in other source files, why?
    /*_
     for ((src, out) <- if (forTest) dirs.scalaTestSrcOutDirs else dirs.scalaSrcOutDirs) {
     settings.outputDirs.add(src, out)
     }
     */

    // ----- now, the new global

    val global = new ScalaGlobal(settings, dummyReporter)
    globals(idx) = global

    // * listen to compCp's change
    if (compCp != null) {
      val compCpListener = new CompCpListener(global, compCp)
      globalToListeners += (global -> (compCpListener :: globalToListeners.getOrElse(global, Nil)))
      project.getProjectDirectory.getFileSystem.addFileChangeListener(compCpListener)
    }
   
    if (!forDebug) {
      // * we have to do following step to get mixed java sources visible to scala sources
      if (srcCp != null) {
        val srcCpListener = new SrcCpListener(global, srcCp)
        globalToListeners += (global -> (srcCpListener :: globalToListeners.getOrElse(global, Nil)))
        project.getProjectDirectory.getFileSystem.addFileChangeListener(srcCpListener)

        // * should push java srcs before scala srcs
        val javaSrcs = new ArrayBuffer[FileObject]
        srcCp.getRoots foreach {x => findAllSourcesOf("text/x-java", x, javaSrcs)}

        val scalaSrcs = new ArrayBuffer[FileObject]
        // * push scala src files to get classes that with different name from file name to be recognized properly
        srcCp.getRoots foreach {x => findAllSourcesOf("text/x-scala", x, scalaSrcs)}

        // * the reporter should be set, otherwise, no java source is resolved, maybe throws exception already.
        global.reporter = dummyReporter
        global askForReLoad (javaSrcs ++= scalaSrcs).toList
      }
    }

    global
  }

  private def findAllSourcesOf(mimeType: String, dirFo: FileObject, result: ArrayBuffer[FileObject]): Unit = {
    dirFo.getChildren foreach {
      case x if x.isFolder => findAllSourcesOf(mimeType, x, result)
      case x if x.getMIMEType == mimeType => result += x
      case _ =>
    }
  }

  private def findDirResource(project: Project): DirResource = {
    val resource = new DirResource

    val sources = ProjectUtils.getSources(project)
    val scalaSgs = sources.getSourceGroups(ScalaSourceUtil.SOURCES_TYPE_SCALA)
    val javaSgs  = sources.getSourceGroups(ScalaSourceUtil.SOURCES_TYPE_JAVA)

    logger.info((scalaSgs map (_.getRootFolder)).mkString("project's src group[ScalaType] dir: [", ", ", "]"))
    logger.info((javaSgs  map (_.getRootFolder)).mkString("project's src group[JavaType]  dir: [", ", ", "]"))

    List(scalaSgs, javaSgs) foreach {
      case Array(srcSg) =>
        val src = srcSg.getRootFolder
        val out = findOutDir(project, src)
        resource.srcToOut += (src -> out)

      case Array(srcSg, testSg, _*) =>
        val src = srcSg.getRootFolder
        val out = findOutDir(project, src)
        resource.srcToOut += (src -> out)

        val test = testSg.getRootFolder
        val testOut = findOutDir(project, test)
        resource.testToOut += (test -> testOut)

      case x =>
        // @todo add other srcs
    }
    
    resource
  }

  private def findOutDir(project: Project, srcRoot: FileObject): FileObject = {
    val srcRootUrl: URL =
      try {
        // * make sure the url is in same form of BinaryForSourceQueryImplementation
        FileUtil.toFile(srcRoot).toURI.toURL
      } catch {case ex: MalformedURLException => Exceptions.printStackTrace(ex); null}

    var out: FileObject = null
    val query = project.getLookup.lookup(classOf[BinaryForSourceQueryImplementation])
    if (query != null && srcRootUrl != null) {
      val result = query.findBinaryRoots(srcRootUrl)
      if (result != null) {
        var break = false
        val itr = result.getRoots.iterator
        while (itr.hasNext && !break) {
          val url = itr.next
          if (!FileUtil.isArchiveFile(url)) {
            val uri = try {
              url.toURI
            } catch {case ex: URISyntaxException => Exceptions.printStackTrace(ex); null}

            if (uri != null) {
              val file = new File(uri)
              break =
                if (file != null) {
                  if (file.isDirectory) {
                    out = FileUtil.toFileObject(file)
                    true
                  } else if (file.exists) {
                    false
                  } else {
                    // * global requires an exist out path, so we should create
                    if (file.mkdirs) {
                      out = FileUtil.toFileObject(file)
                      true
                    } else false
                  }
                } else false
            }
          }
        }
      }
    }

    if (out == null) {
      val execCp = ClassPath.getClassPath(srcRoot, ClassPath.EXECUTE)
      if (execCp != null) {
        val candidates = execCp.getRoots filter (!_.getFileSystem.isInstanceOf[JarFileSystem])
        candidates find (x => x.getPath.endsWith("classes")) foreach {out = _}
        if (out == null & candidates.length > 0) {
          out = candidates(0)
        }
      }
    }

    // * global requires an exist out path, so we have to create a tmp folder
    if (out == null) {
      val projectDir = project.getProjectDirectory
      if (projectDir != null && projectDir.isFolder) {
        try {
          val tmpClasses = "build.classes"
          out = projectDir.getFileObject(tmpClasses) match {
            case null => projectDir.createFolder(tmpClasses)
            case x => x
          }
        } catch {case ex: IOException => Exceptions.printStackTrace(ex)}
      }
    }

    out
  }

  private def toClassPathString(project: Project, cp: ClassPath): String = {
    if (cp == null) return ""

    val sb = new StringBuilder
    val itr = cp.entries.iterator
    while (itr.hasNext) {
      val rootFile =
        try {
          itr.next.getRoot match {
            case null => null
            case entryRoot => entryRoot.getFileSystem match {
                case jfs: JarFileSystem => jfs.getJarFile
                case _ => FileUtil.toFile(entryRoot)
              }
          }
        } catch {case ex:FileStateInvalidException => Exceptions.printStackTrace(ex); null}

      if (rootFile != null) {
        sb.append(rootFile.getAbsolutePath)
        if (itr.hasNext) sb.append(File.pathSeparator)
      }
    }

    sb.toString
  }

  private class SrcCpListener(global: ScalaGlobal, srcCp: ClassPath) extends FileChangeAdapter {
    val javaMimeType = "text/x-java"
    val srcRoots = srcCp.getRoots

    private def isUnderSrcDir(fo: FileObject) = {
      srcRoots exists {x => FileUtil.isParentOf(x, fo)}
    }

    override def fileDataCreated(fe: FileEvent): Unit = {
      val fo = fe.getFile
      if (fo.getMIMEType == javaMimeType && isUnderSrcDir(fo) && global != null) {
        global.reporter = dummyReporter
        global askForReLoad List(fo)
      }
    }

    override def fileChanged(fe: FileEvent): Unit = {
      val fo = fe.getFile
      if (fo.getMIMEType == javaMimeType && isUnderSrcDir(fo) && global != null) {
        global.reporter = dummyReporter
        global askForReLoad List(fo)
      }
    }

    override def fileRenamed(fe: FileRenameEvent): Unit = {
      val fo = fe.getFile
      if (fo.getMIMEType == javaMimeType && isUnderSrcDir(fo) && global != null) {
        global.reporter = dummyReporter
        global askForReLoad List(fo)
      }
    }

    override def fileDeleted(fe: FileEvent): Unit = {
      // @todo get the dependency ot just recompile all?
    }
  }

  private class CompCpListener(global: ScalaGlobal, compCp: ClassPath) extends FileChangeAdapter {
    val compRoots = compCp.getRoots

    private def isUnderCompCp(fo: FileObject) = {
      // * when there are series of folder/file created, only top created folder can be listener
      val found = compRoots find {x => FileUtil.isParentOf(fo, x) || x == fo}
      if (found.isDefined) logger.finest("under compCp: fo=" + fo + ", found=" + found)
      found isDefined
    }

    override def fileFolderCreated(fe: FileEvent) {
      val fo = fe.getFile
      if (isUnderCompCp(fo) && global != null) {
        logger.finest("folder created: " + fo)
        resetLate(global, compCpChanged)
      }
    }

    override def fileDataCreated(fe: FileEvent): Unit = {
      val fo = fe.getFile
      if (isUnderCompCp(fo) && global != null) {
        logger.finest("data created: " + fo)
        resetLate(global, compCpChanged)
      }
    }

    override def fileChanged(fe: FileEvent): Unit = {
      val fo = fe.getFile
      if (isUnderCompCp(fo) && global != null) {
        logger.finest("file changed: " + fo)
        resetLate(global, compCpChanged)
      }
    }

    override def fileRenamed(fe: FileRenameEvent): Unit = {
      val fo = fe.getFile
      if (isUnderCompCp(fo) && global != null) {
        logger.finest("file renamed: " + fo)
        resetLate(global, compCpChanged)
      }
    }

    override def fileDeleted(fe: FileEvent): Unit = {
      val fo = fe.getFile
      if (isUnderCompCp(fo) && global != null) {
        logger.finest("file deleted: " + fo)
        resetLate(global, compCpChanged)
      }
    }
  }
}

class ScalaGlobal(_settings: Settings, _reporter: Reporter) extends Global(_settings, _reporter)
                                                               with ScalaItems
                                                               with ScalaDfns
                                                               with ScalaRefs
                                                               with ScalaElements
                                                               with JavaElements
                                                               with ScalaUtils {
  import ScalaGlobal._

  // * Inner object inside a class is not singleton, so it's safe for each instance of ScalaGlobal,
  // * but, is it thread safe? http://lampsvn.epfl.ch/trac/scala/ticket/1591
  private object scalaAstVisitor extends {
    val global: ScalaGlobal.this.type = ScalaGlobal.this
  } with ScalaAstVisitor

  override def onlyPresentation = true

  override def logError(msg: String, t: Throwable): Unit = {}

  def askForReLoad(srcFos: List[FileObject]) : Unit = {
    val srcFiles = srcFos map {fo => getSourceFile(new PlainFile(FileUtil.toFile(fo)))}

    try {
      val resp = new Response[Unit]
      askReload(srcFiles, resp)
      resp.get
    } catch {
      case ex: AssertionError =>
        /**
         * @Note: avoid scala nsc's assert error. Since global's
         * symbol table may have been broken, we have to reset ScalaGlobal
         * to clean this global
         */
        ScalaGlobal.resetLate(this, ex)
      case ex: java.lang.Error => // avoid scala nsc's Error error
      case ex: Throwable => // just ignore all ex
    }
  }

  def askForSemantic(srcFile: SourceFile, forceReload: Boolean, th: TokenHierarchy[_]): ScalaRootScope = {
    qualToRecoveredType = Map()

    val resp = new Response[ScalaRootScope]
    try {
      askSemantic(srcFile, forceReload, resp, th)
    } catch {
      case ex: AssertionError =>
        /**
         * @Note: avoid scala nsc's assert error. Since global's
         * symbol table may have been broken, we have to reset ScalaGlobal
         * to clean this global
         */
        ScalaGlobal.resetLate(this, ex)
      case ex: java.lang.Error => // avoid scala nsc's Error error
      case ex: Throwable => // just ignore all ex
    }

    resp.get.left.toOption getOrElse ScalaRootScope.EMPTY
  }

  def askSemantic(source: SourceFile, forceReload: Boolean, result: Response[ScalaRootScope], th: TokenHierarchy[_]) =
    scheduler postWorkItem new WorkItem(List(source)) {
      def apply() = getSemanticRoot(source, forceReload, result, th)
      override def toString = "semantic"
    }

  def getSemanticRoot(source : SourceFile, forceReload: Boolean, result: Response[ScalaRootScope], th: TokenHierarchy[_]) {
    respond(result)(semanticRoot(source, forceReload, th))
  }

  private var semanticCancelled = false
  def semanticRoot(source: SourceFile, forceReload: Boolean, th: TokenHierarchy[_]): ScalaRootScope = {
    semanticCancelled = false
    val unit = unitOf(source)
    val sources = List(source)
    if (unit.status == NotLoaded || forceReload) reloadSources(sources)
    moveToFront(sources)

    if (semanticCancelled) return ScalaRootScope.EMPTY

    currentTyperRun.typedTree(unitOf(source))

    if (semanticCancelled) return ScalaRootScope.EMPTY

    val start = System.currentTimeMillis
    val root = scalaAstVisitor(unitOf(source), th)
    GlobalLog.info("Visit took " + (System.currentTimeMillis - start) + "ms")
    root
  }

  def cancelSemantic(source: SourceFile) {
    currentAction match {
      case workItem: WorkItem if workItem.toString.startsWith("semantic") =>
        val fileA = workItem.sources.head.file.file
        val fileB = source.file.file
        if (fileA != null && fileB != null && fileA.getAbsolutePath == fileB.getAbsolutePath) {
          GlobalLog.info("CancelSemantic " + fileA.getName)
          semanticCancelled = true
          scalaAstVisitor.cancel
        }
      case _ =>
    }
  }

  // ----- Code that has been deprecated, for reference only
  
  /** batch complie */
  def compileSourcesForPresentation(srcFiles: List[FileObject]): Unit = {
    settings.stop.value = Nil
    settings.stop.tryToSetColon(List(superAccessors.phaseName))
    try {
      new this.Run compile (srcFiles map (FileUtil.toFile(_).getAbsolutePath))
    } catch {
      case ex: AssertionError =>
        /**
         * @Note: avoid scala nsc's assert error. Since global's
         * symbol table may have been broken, we have to reset ScalaGlobal
         * to clean this global
         */
        ScalaGlobal.resetLate(this, ex)
      case ex: java.lang.Error => // avoid scala nsc's Error error
      case ex: Throwable => // just ignore all ex
    }
  }

  def compileSourceForPresentation(srcFile: SourceFile, th: TokenHierarchy[_]): ScalaRootScope = {
    compileSource(srcFile, superAccessors.phaseName, th)
  }

  // * @Note Should pass phase "lambdalift" to get anonfun's class symbol built
  def compileSourceForDebug(srcFile: SourceFile, th: TokenHierarchy[_]): ScalaRootScope = {
    compileSource(srcFile, constructors.phaseName, th)
  }

  // * @Note the following setting exlcudes 'stopPhase' itself
  def compileSource(srcFile: SourceFile, stopPhaseName: String, th: TokenHierarchy[_]): ScalaRootScope = synchronized {
    settings.stop.value = Nil
    settings.stop.tryToSetColon(List(stopPhaseName))
    qualToRecoveredType = Map()

    val run = new this.Run

    val srcFiles = List(srcFile)
    try {
      run.compileSources(srcFiles)
    } catch {
      case ex: AssertionError =>
        /**
         * @Note: avoid scala nsc's assert error. Since global's
         * symbol table may have been broken, we have to reset ScalaGlobal
         * to clean this global
         */
        ScalaGlobal.resetLate(this, ex)
      case ex: java.lang.Error => // avoid scala nsc's Error error
      case ex: Throwable => // just ignore all ex
    }

    //println("selectTypeErrors:" + selectTypeErrors)

    run.units find {_.source eq srcFile} map {unit =>
      if (ScalaGlobal.debug) {
        RequestProcessor.getDefault.post(new Runnable {
            def run {
              treeBrowser.browse(unit.body)
            }
          })
      }

      scalaAstVisitor(unit, th)
    } getOrElse ScalaRootScope.EMPTY
  }


}
