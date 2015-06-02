package org.netbeans.modules.scala.sbt.project

import java.beans.PropertyChangeListener
import java.beans.PropertyChangeSupport
import java.io.File
import java.util.Timer
import java.util.logging.Logger
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener
import org.netbeans.api.java.classpath.ClassPath
import org.netbeans.modules.scala.core.ProjectResources
import org.netbeans.modules.scala.sbt.console.SBTConsoleTopComponent
import org.openide.filesystems.FileUtil
import scala.collection.mutable.ArrayBuffer
import scalariform.formatter.preferences.AllPreferences
import scalariform.formatter.preferences.FormattingPreferences
import scalariform.formatter.preferences.IFormattingPreferences
import scalariform.formatter.preferences.PreferenceDescriptor

case class ProjectContext(
  name: String,
  id: String,
  mainJavaSrcs: Array[(File, File)],
  testJavaSrcs: Array[(File, File)],
  mainScalaSrcs: Array[(File, File)],
  testScalaSrcs: Array[(File, File)],
  mainResourcesSrcs: Array[(File, File)],
  testResourcesSrcs: Array[(File, File)],
  mainManagedSrcs: Array[(File, File)],
  testManagedSrcs: Array[(File, File)],
  mainCps: Array[File],
  testCps: Array[File],
  mainDepPrjs: Array[File],
  testDepPrjs: Array[File],
  aggPrjs: Array[File],
  scalariformPrefs: IFormattingPreferences)

/**
 *
 * @author Caoyuan Deng
 */
class SBTResolver(project: SBTProject) extends ChangeListener {
  import SBTResolver._

  private val log = Logger.getLogger(getClass.getName)

  private val pcs = new PropertyChangeSupport(this)
  private val projectDir = project.getProjectDirectory
  private var _projectContext: ProjectContext = _
  @volatile private var _isResolvedOrResolving = false
  private var _isDescriptorFileMissed = false

  def isResolvedOrResolving = _isResolvedOrResolving
  def isResolvedOrResolving_=(b: Boolean) {
    val oldvalue = _isResolvedOrResolving
    _isResolvedOrResolving = b
    if (oldvalue != _isResolvedOrResolving) {
      pcs.firePropertyChange(SBT_RESOLVED_STATE_CHANGE, oldvalue, _isResolvedOrResolving)
    }
  }

  def triggerSbtResolution {
    if (!_isResolvedOrResolving) {
      _isResolvedOrResolving = true
      val rootProject = project.getRootProject
      def rootResolver = rootProject.getLookup.lookup(classOf[SBTResolver])
      if (rootProject == project || !rootResolver.isResolvedOrResolving) {
        val commands = List("netbeans")
        SBTConsoleTopComponent.openInstance(rootProject, commands, isDebug = false) { _ =>
          pcs.firePropertyChange(SBT_RESOLVED, null, null)
        }
      }
    }
  }

  def projectContext = synchronized {
    if (_projectContext == null) {
      projectDir.getFileObject(DESCRIPTOR_FILE_NAME) match {
        case null =>
          _isDescriptorFileMissed = true
          dirWatcher.addChangeListener(projectDir, this)
          // set Empty one as soon as possible, so it can be cover by the one get via triggerSbtResolution laster
          _projectContext = EmptyContext
          triggerSbtResolution
        case file =>
          _isDescriptorFileMissed = false
          dirWatcher.addChangeListener(projectDir, this)
          _projectContext = parseClasspathXml(FileUtil.toFile(file))
      }
    }

    _projectContext
  }

  def stateChanged(evt: ChangeEvent) {
    evt match {
      case FileAdded(file, time) if file.getParent == projectDir && _isDescriptorFileMissed =>
        log.info("Got " + evt + ", " + file.getPath)
        _isDescriptorFileMissed = false
        val oldContext = _projectContext
        _projectContext = parseClasspathXml(FileUtil.toFile(file))
        pcs.firePropertyChange(DESCRIPTOR_CHANGE, oldContext, _projectContext)

      case FileModified(file, time) if file.getParent == projectDir =>
        log.info("Got " + evt + ", " + file.getPath)
        val oldContext = _projectContext
        _projectContext = parseClasspathXml(FileUtil.toFile(file))
        pcs.firePropertyChange(DESCRIPTOR_CHANGE, oldContext, _projectContext)

      case _ =>
    }
  }

  private def parseClasspathXml(file: File): ProjectContext = {
    var name: String = null
    var id: String = null
    var mainOutput: File = null
    var testOutput: File = null
    val mainJavaSrcs = new ArrayBuffer[(File, File)]()
    val testJavaSrcs = new ArrayBuffer[(File, File)]()
    val mainScalaSrcs = new ArrayBuffer[(File, File)]()
    val testScalaSrcs = new ArrayBuffer[(File, File)]()
    val mainResourcesSrcs = new ArrayBuffer[(File, File)]()
    val testResourcesSrcs = new ArrayBuffer[(File, File)]()
    val mainManagedSrcs = new ArrayBuffer[(File, File)]()
    val testManagedSrcs = new ArrayBuffer[(File, File)]()
    val mainCps = new ArrayBuffer[File]()
    val testCps = new ArrayBuffer[File]()
    val mainDepPrjs = new ArrayBuffer[File]()
    val testDepPrjs = new ArrayBuffer[File]()
    val aggPrjs = new ArrayBuffer[File]()

    var scalatiformPrefs = Map[PreferenceDescriptor[_], Any]()

    val projectFo = project.getProjectDirectory
    val projectDir = FileUtil.toFile(projectFo)
    try {
      val classpath = scala.xml.XML.loadFile(file)
      classpath match {
        case context @ <classpath>{ entries @ _* }</classpath> =>
          name = (context \ "@name").text.trim
          id = (context \ "@id").text.trim
          entries foreach {
            case entry @ <classpathentry>{ _* }</classpathentry> =>
              (entry \ "@kind").text match {
                case "src" =>
                  val path1 = (entry \ "@path").text.trim.replace("\\", "/")
                  val path = if (path1.startsWith("multi-jvm")) "src/" + path1 else path1 // TODO
                  val isTest = (entry \ "@scope").text.trim.equalsIgnoreCase("test")
                  val isManaged = (entry \ "@managed").text.trim.equalsIgnoreCase("true")
                  val isDepProject = !((entry \ "@exported") isEmpty)

                  val srcFo = projectFo.getFileObject(path)

                  val output = (entry \ "@output").text.trim.replace("\\", "/") // classes folder
                  val outDir = if (isDepProject) {
                    new File(output)
                  } else {
                    new File(projectDir, output)
                  }

                  if (srcFo != null && !isDepProject) {
                    // use 'path' directly instead of srcFo as the full path could contain 'java' in the parent
                    // e.g. /usr/local/java/projects/fooProj/src/main/scala is identified as a "java" source
                    val isJava = path.split("/") find (_ == "java") isDefined
                    val isResources = path.split("/") find (_ == "resources") isDefined
                    val srcDir = FileUtil.toFile(srcFo)
                    val srcs = if (isTest) {
                      if (isManaged) {
                        testManagedSrcs
                      } else {
                        if (isJava) testJavaSrcs else if (isResources) testResourcesSrcs else testScalaSrcs
                      }
                    } else { // main
                      if (isManaged) {
                        mainManagedSrcs
                      } else {
                        if (isJava) mainJavaSrcs else if (isResources) mainResourcesSrcs else mainScalaSrcs
                      }
                    }
                    srcs += srcDir -> outDir
                  }

                  if (isTest) {
                    testCps += outDir
                  } else {
                    mainCps += outDir
                  }

                  if (isDepProject) {
                    val base = (entry \ "@base").text.trim.replace("\\", "/")
                    val baseDir = new File(base)
                    if (baseDir.exists) {
                      if (isTest) {
                        testDepPrjs += baseDir
                      } else {
                        mainDepPrjs += baseDir
                      }
                    }
                  }

                case "lib" =>
                  val path = (entry \ "@path").text.trim.replace("\\", "/")
                  val isTest = (entry \ "@scope").text.trim.equalsIgnoreCase("test")
                  val libFile = new File(path) match {
                    case x if x.isAbsolute => x
                    case _                 => new File(projectDir, path)
                  }

                  if (libFile.exists) {
                    if (isTest) {
                      testCps += libFile
                    } else {
                      mainCps += libFile
                    }
                  }

                case "agg" =>
                  val base = (entry \ "@base").text.trim.replace("\\", "/")
                  val baseFile = new File(base)
                  if (baseFile.exists) {
                    aggPrjs += baseFile
                  }

                case _ =>
              }

            case entry @ <scalariform>{ _* }</scalariform> =>
              val scope = (entry \ "@scope").text.trim.toLowerCase
              if (scope == "compile") {
                val k = (entry \ "@key").text
                AllPreferences.preferencesByKey.get(k) match {
                  case Some(key) =>
                    val v = (entry \ "@value").text
                    key.preferenceType.parseValue(v) match {
                      case Right(value) => scalatiformPrefs += (key -> value)
                      case Left(ex)     =>
                    }
                  case _ =>
                }
              }

            case _ =>
          }

        case _ =>

      }

    } catch {
      case ex: Exception => ex.printStackTrace
    }

    ProjectContext(
      name,
      id,
      mainJavaSrcs map { case (s, o) => FileUtil.normalizeFile(s) -> FileUtil.normalizeFile(o) } toArray,
      testJavaSrcs map { case (s, o) => FileUtil.normalizeFile(s) -> FileUtil.normalizeFile(o) } toArray,
      mainScalaSrcs map { case (s, o) => FileUtil.normalizeFile(s) -> FileUtil.normalizeFile(o) } toArray,
      testScalaSrcs map { case (s, o) => FileUtil.normalizeFile(s) -> FileUtil.normalizeFile(o) } toArray,
      mainResourcesSrcs map { case (s, o) => FileUtil.normalizeFile(s) -> FileUtil.normalizeFile(o) } toArray,
      testResourcesSrcs map { case (s, o) => FileUtil.normalizeFile(s) -> FileUtil.normalizeFile(o) } toArray,
      mainManagedSrcs map { case (s, o) => FileUtil.normalizeFile(s) -> FileUtil.normalizeFile(o) } toArray,
      testManagedSrcs map { case (s, o) => FileUtil.normalizeFile(s) -> FileUtil.normalizeFile(o) } toArray,
      mainCps map FileUtil.normalizeFile toArray,
      testCps map FileUtil.normalizeFile toArray,
      mainDepPrjs map FileUtil.normalizeFile toArray,
      testDepPrjs map FileUtil.normalizeFile toArray,
      aggPrjs map FileUtil.normalizeFile toArray,
      new FormattingPreferences(scalatiformPrefs))
  }

  def addPropertyChangeListener(propertyChangeListener: PropertyChangeListener) {
    pcs.addPropertyChangeListener(propertyChangeListener)
  }

  def removePropertyChangeListener(propertyChangeListener: PropertyChangeListener) {
    pcs.removePropertyChangeListener(propertyChangeListener)
  }

  def getName: String = projectContext.name

  def getId: String = projectContext.id

  def getResolvedClassPath(scope: String, isTest: Boolean): Array[File] = {
    scope match {
      case ClassPath.COMPILE =>
        if (isTest) projectContext.mainCps ++ projectContext.testCps else projectContext.mainCps
      case ClassPath.EXECUTE =>
        if (isTest) projectContext.mainCps ++ projectContext.testCps else projectContext.mainCps
      case ClassPath.SOURCE =>
        if (isTest) {
          projectContext.mainJavaSrcs ++ projectContext.mainScalaSrcs ++ projectContext.mainManagedSrcs ++
            projectContext.testJavaSrcs ++ projectContext.testScalaSrcs ++ projectContext.testManagedSrcs map (_._1)
        } else {
          projectContext.mainJavaSrcs ++ projectContext.mainScalaSrcs ++ projectContext.mainManagedSrcs map (_._1)
        }
      case ClassPath.BOOT =>
        projectContext.mainCps filter { cp =>
          val name = cp.getName
          name.endsWith(".jar") && (name.startsWith("scala-library") ||
            name.startsWith("scala-compiler") || // necessary?
            name.startsWith("scala-reflect") // necessary?
            )
        }
      case _ => Array()
    }
  }

  def getSources(tpe: String, isTest: Boolean): Array[(File, File)] = {
    tpe match {
      case ProjectResources.SOURCES_TYPE_JAVA =>
        if (isTest) projectContext.testJavaSrcs else projectContext.mainJavaSrcs
      case ProjectResources.SOURCES_TYPE_SCALA =>
        if (isTest) projectContext.testScalaSrcs else projectContext.mainScalaSrcs
      case ProjectResources.SOURCES_TYPE_RESOURCES =>
        if (isTest) projectContext.testResourcesSrcs else projectContext.mainResourcesSrcs
      case ProjectResources.SOURCES_TYPE_MANAGED =>
        if (isTest) projectContext.testManagedSrcs else projectContext.mainManagedSrcs
      case _ => Array()
    }
  }

  def getDependenciesProjects(isTest: Boolean): Array[File] = if (isTest) projectContext.testDepPrjs else projectContext.mainDepPrjs
  def getAggregateProjects: Array[File] = projectContext.aggPrjs
}

object SBTResolver {
  val DESCRIPTOR_FILE_NAME = ".classpath_nb"

  val DESCRIPTOR_CHANGE = "sbtDescriptorChange"
  val SBT_RESOLVED_STATE_CHANGE = "sbtResolvedStateChange"
  val SBT_RESOLVED = "sbtResolved"

  val EmptyContext = ProjectContext(null,
    null,
    Array[(File, File)](),
    Array[(File, File)](),
    Array[(File, File)](),
    Array[(File, File)](),
    Array[(File, File)](),
    Array[(File, File)](),
    Array[(File, File)](),
    Array[(File, File)](),
    Array[File](),
    Array[File](),
    Array[File](),
    Array[File](),
    Array[File](),
    FormattingPreferences())

  val dirWatcher = new DirWatcher(DESCRIPTOR_FILE_NAME)

  private val timer = new Timer
  timer.schedule(dirWatcher, 0, 1500)
}