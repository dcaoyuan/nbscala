package org.netbeans.modules.scala.core

import java.io.File
import java.io.IOException
import java.net.MalformedURLException
import java.net.URISyntaxException
import java.net.URL
import java.util.logging.Logger
import org.netbeans.api.java.classpath.ClassPath
import org.netbeans.api.project.FileOwnerQuery
import org.netbeans.api.project.Project
import org.netbeans.api.project.ProjectUtils
import org.netbeans.api.project.SourceGroup
import org.netbeans.spi.java.classpath.ClassPathProvider
import org.netbeans.spi.java.queries.BinaryForSourceQueryImplementation
import org.openide.filesystems.FileObject
import org.openide.filesystems.FileStateInvalidException
import org.openide.filesystems.FileUtil
import org.openide.modules.InstalledFileLocator
import org.openide.util.Exceptions
import scala.collection.mutable
import scala.reflect.io.AbstractFile

/**
 *
 * @author Caoyuan Deng
 */
object ProjectResources {
  private val log = Logger.getLogger(getClass.getName)

  /** @see org.netbeans.api.java.project.JavaProjectConstants */
  val SOURCES_TYPE_JAVA = "java"
  /** a source group type for separate scala source roots, as seen in maven projects for example */
  val SOURCES_TYPE_SCALA = "scala"
  /** a source group type for managed source roots, as seen in sbt projects for example */
  val SOURCES_TYPE_MANAGED = "managed"
  /** @see org.netbeans.api.project.Sources Package root sources type for resources, if these are not put together with Java sources. */
  val SOURCES_TYPE_RESOURCES = "resources" // NOI18N

  @deprecated("Use ClassPath.get(fo, ClassPath.SOURCE) to find src", "1.6.2")
  private lazy val projectToResources = new mutable.WeakHashMap[Project, ProjectResource]

  @deprecated("Use ClassPath.get(fo, ClassPath.SOURCE) to find src", "1.6.2")
  class ProjectResource {
    var mainSrcToOut = Map[FileObject, FileObject]()
    var testSrcToOut = Map[FileObject, FileObject]()

    def mainSrcOutDirsPath = toDirPaths(mainSrcToOut)
    def testSrcOutDirsPath = toDirPaths(testSrcToOut)

    def scalaMainSrcToOut: Map[AbstractFile, AbstractFile] = toScalaDirs(mainSrcToOut)
    def scalaTestSrcToOut: Map[AbstractFile, AbstractFile] = toScalaDirs(testSrcToOut)

    private def toDirPaths(dirs: Map[FileObject, FileObject]): Map[String, String] = {
      for ((src, out) <- dirs) yield (toDirPath(src), toDirPath(out))
    }

    private def toScalaDirs(dirs: Map[FileObject, FileObject]): Map[AbstractFile, AbstractFile] = {
      for ((src, out) <- dirs) yield (toScalaDir(src), toScalaDir(out))
    }

    private def toDirPath(fo: FileObject) = FileUtil.toFile(fo).getAbsolutePath
    private def toScalaDir(fo: FileObject) = AbstractFile.getDirectory(FileUtil.toFile(fo))
  }

  @deprecated("Use ClassPath.get(fo, ClassPath.SOURCE) to find src", "1.6.2")
  def getResourceOf(fo: FileObject, refresh: Boolean): Map[FileObject, FileObject] = {
    val project = FileOwnerQuery.getOwner(fo)
    if (project eq null) {
      // * it may be a standalone file, or file in standard lib
      return Map()
    }

    val resource = if (refresh) {
      collectProjectResource(project)
    } else {
      projectToResources.getOrElseUpdate(project, collectProjectResource(project))
    }

    if (isForTest(resource, fo)) resource.testSrcToOut else resource.mainSrcToOut
  }

  @deprecated("Use ClassPath.get(fo, ClassPath.SOURCE) to find src", "1.6.2")
  def getSrcFileObjects(fo: FileObject, refresh: Boolean): Array[FileObject] = {
    val resource = getResourceOf(fo, refresh)
    val srcPaths = for ((src, out) <- resource) yield src

    srcPaths.toArray
  }

  @deprecated("Use ClassPath.get(fo, ClassPath.SOURCE) to find src", "1.6.2")
  def getOutFileObject(fo: FileObject, refresh: Boolean): Option[FileObject] = {
    val resource = getResourceOf(fo, refresh)
    for ((src, out) <- resource) {
      try {
        val file = FileUtil.toFile(out)
        if (!file.exists) file.mkdirs
      } catch {
        case _: Throwable =>
      }

      return Some(out)
    }

    None
  }

  /** is this `fo` under test source? */
  @deprecated("Use ClassPath.get(fo, ClassPath.SOURCE) to find src", "1.6.2")
  def isForTest(resource: ProjectResource, fo: FileObject) = {
    // we should check mainSrcs first, since I'm not sure if the testSrcs includes this fo too.
    if (resource.mainSrcToOut exists { case (src, _) => src.equals(fo) || FileUtil.isParentOf(src, fo) }) {
      false
    } else {
      resource.testSrcToOut exists { case (src, _) => src.equals(fo) || FileUtil.isParentOf(src, fo) }
    }
  }

  def findAllSourcesOf(mimeType: String, result: mutable.ListBuffer[FileObject])(dirFo: FileObject) {
    dirFo.getChildren foreach { x =>
      if (x.isFolder) {
        findAllSourcesOf(mimeType, result)(x)
      } else if (x.getMIMEType == mimeType) {
        result += x
      }
    }
  }

  def getScalaJavaSourceGroups(project: Project): Array[SourceGroup] = {
    val sources = ProjectUtils.getSources(project)
    val scalaSgs = sources.getSourceGroups(SOURCES_TYPE_SCALA)
    val javaSgs = sources.getSourceGroups(SOURCES_TYPE_JAVA)
    val managedSgs = sources.getSourceGroups(SOURCES_TYPE_MANAGED)
    scalaSgs ++ javaSgs ++ managedSgs
  }

  @deprecated("Use ClassPath.get(fo, ClassPath.SOURCE) to find src", "1.6.2")
  def collectProjectResource(project: Project): ProjectResource = {
    val resource = new ProjectResource

    val (mainSrcs, testSrcs) = findMainAndTestSrcs(project)

    mainSrcs foreach { src =>
      val out = findOutDir(project, src)
      resource.mainSrcToOut += (src -> out)
    }

    testSrcs foreach { src =>
      val out = findOutDir(project, src)
      resource.testSrcToOut += (src -> out)
    }

    resource
  }

  def findMainAndTestSrcs(project: Project): (Set[FileObject], Set[FileObject]) = {
    var mainSrcs = Set[FileObject]()
    var testSrcs = Set[FileObject]()

    val sources = ProjectUtils.getSources(project)
    val scalaSgs = sources.getSourceGroups(SOURCES_TYPE_SCALA)
    val javaSgs = sources.getSourceGroups(SOURCES_TYPE_JAVA)
    val managedSgs = sources.getSourceGroups(SOURCES_TYPE_MANAGED)

    log.fine((scalaSgs map (_.getRootFolder.getPath)).mkString("Project's src group[Scala]    dir: [", ", ", "]"))
    log.fine((javaSgs map (_.getRootFolder.getPath)).mkString("Project's src group[Java]     dir: [", ", ", "]"))
    log.fine((managedSgs map (_.getRootFolder.getPath)).mkString("Project's src group[Managed]  dir: [", ", ", "]"))

    List(scalaSgs, javaSgs, managedSgs) foreach {
      case Array(mainSg) =>
        mainSrcs += mainSg.getRootFolder

      case Array(mainSg, testSg, _*) =>
        mainSrcs += mainSg.getRootFolder
        testSrcs += testSg.getRootFolder

      case _ =>
      // @todo add other srcs
    }

    (mainSrcs, testSrcs)
  }

  def findOutDir(project: Project, srcRoot: FileObject): FileObject = {
    val srcRootUrl: URL =
      try {
        // make sure the url is in same form of BinaryForSourceQueryImplementation
        FileUtil.toFile(srcRoot).toURI.toURL
      } catch {
        case ex: MalformedURLException => Exceptions.printStackTrace(ex); null
      }

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
            } catch { case ex: URISyntaxException => Exceptions.printStackTrace(ex); null }

            if (uri != null) {
              val file = new File(uri)
              break = if (file != null) {
                if (file.isDirectory) {
                  out = FileUtil.toFileObject(file)
                  true
                } else if (file.exists) {
                  false
                } else {
                  // global requires an exist out path, so we should create
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
        val candidates = execCp.getRoots filter { x => FileUtil.getArchiveFile(x) == null }
        out = candidates find (_.getPath.endsWith("classes")) match {
          case Some(x)                       => x
          case None if candidates.length > 0 => candidates(0)
          case _                             => null
        }
      }
    }

    // global requires an exist out path, so we have to create a tmp folder
    if (out == null) {
      val projectDir = project.getProjectDirectory
      if (projectDir != null && projectDir.isFolder) {
        try {
          val tmpClasses = "build.classes.tmp"
          out = projectDir.getFileObject(tmpClasses) match {
            case null => projectDir.createFolder(tmpClasses)
            case x    => x
          }
        } catch { case ex: IOException => Exceptions.printStackTrace(ex) }
      }
    }

    out
  }

  def getOutFileObjectForSrc(src: FileObject): Option[FileObject] = {
    val execCp = ClassPath.getClassPath(src, ClassPath.EXECUTE)
    if (execCp != null) {
      val candidates = execCp.getRoots filter (FileUtil.getArchiveFile(_) == null)
      candidates find (_.getPath.endsWith("classes")) match {
        case None =>
          if (candidates.length > 0) {
            Some(candidates(0))
          } else None
        case x => x
      }
    } else None
  }

  /**
   * @return (javaSources, scalaSources)
   */
  def findAllSources(srcCp: ClassPath): (List[FileObject], List[FileObject]) = {
    if (srcCp != null) {
      val javaSrcs = new mutable.ListBuffer[FileObject]
      srcCp.getRoots foreach findAllSourcesOf("text/x-java", javaSrcs)

      val scalaSrcs = new mutable.ListBuffer[FileObject]
      srcCp.getRoots foreach findAllSourcesOf("text/x-scala", scalaSrcs)

      (javaSrcs.toList, scalaSrcs.toList)
    } else {
      (Nil, Nil)
    }
  }

}
