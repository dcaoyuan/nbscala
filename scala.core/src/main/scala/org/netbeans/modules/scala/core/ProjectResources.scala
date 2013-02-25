/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

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
import org.netbeans.spi.java.queries.BinaryForSourceQueryImplementation
import org.openide.filesystems.FileObject
import org.openide.filesystems.FileStateInvalidException
import org.openide.filesystems.FileUtil
import org.openide.filesystems.JarFileSystem
import org.openide.modules.InstalledFileLocator
import org.openide.util.Exceptions
import scala.collection.mutable
import scala.reflect.io.AbstractFile

object ProjectResources {
  private val log = Logger.getLogger(getClass.getName)
  
  private val projectToResources = new mutable.WeakHashMap[Project, DirResource]

  class DirResource {
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
  
  def getResourceOf(fo: FileObject, refresh: Boolean): Map[FileObject, FileObject] = {
    val project = FileOwnerQuery.getOwner(fo)
    if (project eq null) {
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
      } catch {
        case _: Throwable =>
      }

      return Some(out)
    }

    None
  }
  
  def isForTest(resource: DirResource, fo: FileObject) = {
    // * is this `fo` under test source?
    resource.testToOut exists {case (src, _) => src.equals(fo) || FileUtil.isParentOf(src, fo)}
  }
  
  def findAllSourcesOf(mimeType: String, result: mutable.ListBuffer[FileObject])(dirFo: FileObject) {
    dirFo.getChildren foreach {x =>
      if (x.isFolder) {
        findAllSourcesOf(mimeType, result)(x)
      } else if (x.getMIMEType == mimeType) {
        result += x
      }
    }
  }

  def findDirResource(project: Project): DirResource = {
    val resource = new DirResource

    val sources = ProjectUtils.getSources(project)
    val scalaSgs = sources.getSourceGroups(ScalaSourceUtil.SOURCES_TYPE_SCALA)
    val javaSgs = sources.getSourceGroups(ScalaSourceUtil.SOURCES_TYPE_JAVA)

    log.fine((scalaSgs map (_.getRootFolder.getPath)).mkString("Project's src group[ScalaType] dir: [", ", ", "]"))
    log.fine((javaSgs  map (_.getRootFolder.getPath)).mkString("Project's src group[JavaType]  dir: [", ", ", "]"))

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
    if ((query ne null) && (srcRootUrl ne null)) {
      val result = query.findBinaryRoots(srcRootUrl)
      if (result ne null) {
        var break = false
        val itr = result.getRoots.iterator
        while (itr.hasNext && !break) {
          val url = itr.next
          if (!FileUtil.isArchiveFile(url)) {
            val uri = try {
              url.toURI
            } catch {case ex: URISyntaxException => Exceptions.printStackTrace(ex); null}

            if (uri ne null) {
              val file = new File(uri)
              break =
                if (file ne null) {
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

    if (out eq null) {
      val execCp = ClassPath.getClassPath(srcRoot, ClassPath.EXECUTE)
      if (execCp ne null) {
        val candidates = execCp.getRoots filter (!_.getFileSystem.isInstanceOf[JarFileSystem])
        candidates find (x => x.getPath.endsWith("classes")) foreach {out = _}
        if ((out eq null) & candidates.length > 0) {
          out = candidates(0)
        }
      }
    }

    // global requires an exist out path, so we have to create a tmp folder
    if (out eq null) {
      val projectDir = project.getProjectDirectory
      if ((projectDir ne null) && projectDir.isFolder) {
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

  def toClassPathString(cp: ClassPath): String = {
    if (cp eq null) return ""

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
        } catch {case ex: FileStateInvalidException => Exceptions.printStackTrace(ex); null}

      if (rootFile ne null) {
        sb.append(rootFile.getAbsolutePath)
        if (itr.hasNext) sb.append(File.pathSeparator)
      }
    }

    sb.toString
  }

  
  def getPluginJarsDir = {
    InstalledFileLocator.getDefault.locate("modules/ext/org.scala-lang.plugins", "org.netbeans.libs.scala.continuations", false)
  }

}
