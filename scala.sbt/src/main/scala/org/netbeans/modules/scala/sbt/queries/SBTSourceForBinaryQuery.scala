package org.netbeans.modules.scala.sbt.queries

import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import java.net.URL
import javax.swing.event.ChangeListener
import org.netbeans.spi.java.queries.SourceForBinaryQueryImplementation2
import org.openide.filesystems.FileObject
import org.openide.filesystems.FileUtil
import org.openide.filesystems.URLMapper
import org.netbeans.api.java.classpath.ClassPath
import org.netbeans.api.java.queries.JavadocForBinaryQuery
import org.netbeans.api.java.queries.SourceForBinaryQuery
import org.netbeans.api.java.queries.SourceForBinaryQuery.Result
import org.netbeans.api.project.FileOwnerQuery
import org.netbeans.api.project.Project
import org.netbeans.modules.scala.core.ProjectResources
import org.netbeans.modules.scala.sbt.nodes.ArtifactInfo
import org.netbeans.modules.scala.sbt.project.SBTResolver
import org.netbeans.spi.java.queries.JavadocForBinaryQueryImplementation
import org.openide.util.ChangeSupport
import scala.collection.mutable

/**
 * It will be used first by org.netbeans.modules.parsing.impl.indexing.PathRegistry.getSources
 * and by GlobalPathRegistry when get debugging sources.
 *
 * @author Caoyuan Deng
 */
class SBTSourceForBinaryQuery(project: Project) extends SourceForBinaryQueryImplementation2 with JavadocForBinaryQueryImplementation {
  private val cache = new mutable.HashMap[String, SrcResult]()
  private val cache2 = new mutable.HashMap[String, SrcResult]()
  private lazy val sbtResolver = {
    val x = project.getLookup.lookup(classOf[SBTResolver])

    x.addPropertyChangeListener(new PropertyChangeListener {
      override def propertyChange(evt: PropertyChangeEvent) {
        evt.getPropertyName match {
          case SBTResolver.DESCRIPTOR_CHANGE =>
            cache synchronized {
              cache.values foreach (_.fireChange)
              cache.clear
            }
          case _ =>
        }
      }
    })

    x
  }

  /*
   * Information about where Java sources corresponding to binaries (classfiles) can be found.
   * A default implementation is registered by the org.netbeans.modules.java.project
   *  module which looks up the project corresponding to the file (if any; jar-protocol 
   *  URLs actually check the owner of the JAR file itself) and checks whether that 
   *  project has an implementation of this interface in its lookup. If so, it delegates 
   *  to that implementation. Therefore it is not generally necessary for a project type 
   *  provider to register its own global implementation of this query, if it depends on 
   *  the Java Project module and uses this style.
   */
  def findSourceRoots(url: URL): SourceForBinaryQuery.Result = cache synchronized {
    cache.getOrElseUpdate(url.toURI.normalize.toString, new SrcResult(getSourceRoots(url)))
  }

  /**
   * In addition to the original SourceForBinaryQueryImplementation this interface
   * also provides information used by the java infrastructure if sources should be
   * preferred over the binaries. When sources are preferred the java infrastructure
   * will use sources as a primary source of the metadata otherwise the binaries
   * (classfiles) are used as a primary source of information and sources are used
   * as a source of formal parameter names and javadoc only. In general sources should
   * be preferred for projects which are user editable but not for libraries or
   * platforms where the sources may not be complete or up to date.
   */
  def findSourceRoots2(url: URL): SourceForBinaryQueryImplementation2.Result = cache2 synchronized {
    cache2.getOrElseUpdate(url.toURI.normalize.toString, new SrcResult(getSourceRoots2(url)))
  }

  /**
   * Find any Javadoc corresponding to the given classpath root containing
   * Java classes.
   * <p>
   * Any absolute URL may be used but typically it will use the <code>file</code>
   * protocol for directory entries and <code>jar</code> protocol for JAR entries
   * (e.g. <samp>jar:file:/tmp/foo.jar!/</samp>).
   * </p>
   * @param binaryRoot the class path root of Java class files
   * @return a result object encapsulating the roots and permitting changes to
   *         be listened to, or null if the binary root is not recognized
   */
  def findJavadoc(url: URL): JavadocForBinaryQuery.Result = new DocResult(getJavadocRoot(url))

  def jarify(path: String): String = { // #200088
    if (path != null) path.replaceFirst("[.][^./]+$", ".jar") else null
  }

  private def getSourceRoots(url: URL): Array[FileObject] = {
    import ProjectResources._
    url.getProtocol match {
      case "file" =>
        val uri = url.toURI.normalize
        val mainSrcs = sbtResolver.getSources(SOURCES_TYPE_JAVA, false) ++ sbtResolver.getSources(SOURCES_TYPE_SCALA, false)
        val testSrcs = sbtResolver.getSources(SOURCES_TYPE_JAVA, true) ++ sbtResolver.getSources(SOURCES_TYPE_SCALA, true)

        val mains = (mainSrcs filter { case (s, o) => uri == FileUtil.urlForArchiveOrDir(o).toURI.normalize } map (_._1))
        val tests = (testSrcs filter { case (s, o) => uri == FileUtil.urlForArchiveOrDir(o).toURI.normalize } map (_._1))
        (mains ++ tests).distinct map FileUtil.toFileObject

      case "jar" =>
        val archiveFileURL = FileUtil.getArchiveFile(url)
        val jarFo = URLMapper.findFileObject(archiveFileURL)
        if (jarFo != null) {
          val jarFile = FileUtil.toFile(jarFo)
          if (jarFile != null) {
            val artifacts = sbtResolver.getResolvedClassPath(ClassPath.COMPILE, isTest = false) map FileUtil.toFileObject filter { fo =>
              fo != null && FileUtil.isArchiveFile(fo)
            } map { fo =>
              val (sources, javadoc) = try {
                val srcs = fo.getParent.getParent.getFileObject("srcs")
                val docs = fo.getParent.getParent.getFileObject("docs")
                val sources = if (srcs != null) srcs.getFileObject(fo.getName + "-sources." + fo.getExt) else null
                val javadoc = if (docs != null) docs.getFileObject(fo.getName + "-javadoc." + fo.getExt) else null
                (sources, javadoc)
              } catch {
                case _: Throwable => (null, null)
              }
              ArtifactInfo(fo.getNameExt, "", "",
                FileUtil.toFile(fo),
                if (sources != null) FileUtil.toFile(sources) else null,
                if (javadoc != null) FileUtil.toFile(javadoc) else null)
            }
            artifacts find (_.jarFile == jarFile) match {
              case Some(x) if x.sourceFile != null =>
                val srcsJar = FileUtil.toFileObject(x.sourceFile)
                FileOwnerQuery.markExternalOwner(srcsJar, project, FileOwnerQuery.EXTERNAL_ALGORITHM_TRANSIENT)
                val srcsJarFo = if (FileUtil.isArchiveFile(srcsJar)) {
                  FileUtil.getArchiveRoot(srcsJar)
                } else srcsJar
                Array(srcsJarFo)
              case _ =>
                Array[FileObject]()
            }

          } else Array[FileObject]()
        } else Array[FileObject]()

      case _ => Array[FileObject]()
    }
  }

  private def getSourceRoots2(url: URL): Array[FileObject] = {
    import ProjectResources._
    url.getProtocol match {
      case "file" =>
        // true for directories.
        val uri = url.toURI.normalize
        val mainSrcs = sbtResolver.getSources(SOURCES_TYPE_JAVA, false) ++ sbtResolver.getSources(SOURCES_TYPE_SCALA, false)
        val testSrcs = sbtResolver.getSources(SOURCES_TYPE_JAVA, true) ++ sbtResolver.getSources(SOURCES_TYPE_SCALA, true)

        val mains = (mainSrcs filter { case (s, o) => uri == FileUtil.urlForArchiveOrDir(o).toURI.normalize } map (_._1))
        val tests = (testSrcs filter { case (s, o) => uri == FileUtil.urlForArchiveOrDir(o).toURI.normalize } map (_._1))
        (mains ++ tests).distinct map FileUtil.toFileObject

      case "jar" => Array[FileObject]()

      case _     => Array[FileObject]()
    }
  }

  private def getJavadocRoot(url: URL): Array[URL] = {
    //TODO shall we delegate to "possibly" generated javadoc in project or in site?
    Array[URL]()
  }

  class SrcResult(roots: Array[FileObject]) extends SourceForBinaryQueryImplementation2.Result {
    private val changeSupport = new ChangeSupport(this)

    def getRoots: Array[FileObject] = roots

    def addChangeListener(l: ChangeListener) {
      changeSupport.addChangeListener(l)
    }

    def removeChangeListener(l: ChangeListener) {
      changeSupport.removeChangeListener(l)
    }

    def fireChange {
      changeSupport.fireChange
    }

    def preferSources: Boolean = true
  }

  private class DocResult(roots: Array[URL]) extends JavadocForBinaryQuery.Result {
    private val changeSupport = new ChangeSupport(this)

    def getRoots: Array[URL] = roots

    def addChangeListener(l: ChangeListener) {
      changeSupport.addChangeListener(l)
    }

    def removeChangeListener(l: ChangeListener) {
      changeSupport.removeChangeListener(l)
    }

    def fireChange {
      changeSupport.fireChange
    }

  }
}
