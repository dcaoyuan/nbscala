package org.netbeans.modules.scala.sbt.queries

import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import java.io.File
import java.net.URL
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener
import org.netbeans.spi.java.queries.SourceForBinaryQueryImplementation2
import org.openide.filesystems.FileObject
import org.openide.filesystems.FileUtil
import org.openide.filesystems.URLMapper
import org.netbeans.api.java.classpath.ClassPath
import org.netbeans.api.java.queries.JavadocForBinaryQuery
import org.netbeans.api.java.queries.SourceForBinaryQuery
import org.netbeans.api.java.queries.SourceForBinaryQuery.Result
import org.netbeans.api.project.Project
import org.netbeans.modules.scala.sbt.nodes.ArtifactInfo
import org.netbeans.modules.scala.sbt.project.ProjectConstants
import org.netbeans.modules.scala.sbt.project.SBTResolver
import org.netbeans.spi.java.queries.JavadocForBinaryQueryImplementation
import scala.collection.mutable

/**
 * It will be used first by org.netbeans.modules.parsing.impl.indexing.PathRegistry.getSources
 * and by GlobalPathRegistry when get debugging sources.
 * 
 * @author Caoyuan Deng
 */
class SBTSourceForBinaryQuery(project: Project) extends SourceForBinaryQueryImplementation2 with JavadocForBinaryQueryImplementation {
  private val map = new mutable.HashMap[String, SrcResult]()
  private lazy val sbtResolver = {
    val x = project.getLookup.lookup(classOf[SBTResolver])
    
    x.addPropertyChangeListener(new PropertyChangeListener {
        override
        def propertyChange(evt: PropertyChangeEvent) {
          evt.getPropertyName match {
            case SBTResolver.DESCRIPTOR_CHANGE =>
              map synchronized {
                map.values foreach (_.fireChanged)
                map.clear
              }
            case _ =>
          }
        }
      }
    )
    
    x
  }

  override 
  def findSourceRoots(url: URL): SourceForBinaryQuery.Result = findSourceRoots2(url)

  override
  def findSourceRoots2(url: URL): SourceForBinaryQueryImplementation2.Result = map synchronized {
    map.getOrElseUpdate(url.toURI.normalize.toString, new SrcResult(url))
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
  override
  def findJavadoc(url: URL): JavadocForBinaryQuery.Result = new DocResult(url)
    
  def jarify(path: String): String = { // #200088
    if (path != null) path.replaceFirst("[.][^./]+$", ".jar") else null
  }
    
  private def getSrcRoot(url: URL): Array[FileObject] = {
    val toReturn = url.getProtocol match {
      case "file" =>
        // true for directories.
        val uri = url.toURI.normalize
        val mainSrcs = sbtResolver.getSources(ProjectConstants.SOURCES_TYPE_JAVA, false) ++ sbtResolver.getSources(ProjectConstants.SOURCES_TYPE_SCALA, false)
        val testSrcs = sbtResolver.getSources(ProjectConstants.SOURCES_TYPE_JAVA, true)  ++ sbtResolver.getSources(ProjectConstants.SOURCES_TYPE_SCALA, true)
        
        val mains = (mainSrcs filter {case (s, o) => uri == FileUtil.urlForArchiveOrDir(o).toURI.normalize} map (_._1))
        val tests = (testSrcs filter {case (s, o) => uri == FileUtil.urlForArchiveOrDir(o).toURI.normalize} map (_._1))
        (mains ++ tests).distinct
        
      case "jar" =>
        // XXX todo
        val artifacts = sbtResolver.getResolvedLibraries(ClassPath.COMPILE) map FileUtil.toFileObject filter {fo => 
          fo != null && FileUtil.isArchiveFile(fo)
        } map {fo =>
          ArtifactInfo(fo.getNameExt, "", "", FileUtil.toFile(fo), null, null)
        }
    
        val archiveFileURL = FileUtil.getArchiveFile(url)
        val jarFo = URLMapper.findFileObject(archiveFileURL)
        if (jarFo != null) {
          val jarFile = FileUtil.toFile(jarFo)
          if (jarFile != null) {
            artifacts find (_.jarFile == jarFile) match {
              case Some(x) if x.sourceFile != null => Array(x.sourceFile)
              case _ => Array[File]()
            }
          } else Array[File]()
        } else Array[File]()
        
      case _ => Array[File]()
    }
    
    toReturn map FileUtil.toFileObject
  }
    
  private def getJavadocRoot: Array[URL] = {
    //TODO shall we delegate to "possibly" generated javadoc in project or in site?
    Array[URL]()
  }
    
  class SrcResult(url: URL) extends SourceForBinaryQueryImplementation2.Result  {
    private val listeners = new mutable.ArrayBuffer[ChangeListener]()
        
    override
    def getRoots: Array[FileObject] = getSrcRoot(url)
        
    override 
    def addChangeListener(changeListener: ChangeListener): Unit = listeners synchronized {
      listeners += changeListener
    }
        
    override 
    def removeChangeListener(changeListener: ChangeListener): Unit = listeners synchronized {
      listeners -= changeListener
    }
        
    def fireChanged {
      val lists = new mutable.ArrayBuffer[ChangeListener]()
      listeners synchronized {
        lists ++= listeners
      }
      lists foreach (_.stateChanged(new ChangeEvent(this)))
    }

    override 
    def preferSources: Boolean = true
  }
    
  private class DocResult(url: URL) extends JavadocForBinaryQuery.Result  {
    private val listeners = new mutable.ArrayBuffer[ChangeListener]()
        
    override 
    def addChangeListener(changeListener: ChangeListener): Unit = listeners synchronized {
      listeners += changeListener
    }
        
    override 
    def removeChangeListener(changeListener: ChangeListener): Unit = listeners synchronized {
      listeners -= changeListener
    }
        
    def fireChanged {
      val lists = new mutable.ArrayBuffer[ChangeListener]()
      listeners synchronized {
        lists ++= listeners
      }
      lists foreach (_.stateChanged(new ChangeEvent(this)))
    }
        
    override 
    def getRoots: Array[URL] = getJavadocRoot
  }
}
