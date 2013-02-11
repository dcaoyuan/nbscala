package org.netbeans.modules.scala.sbt.classpath

/**
 * Defines the various class paths for a sbt project.
 */
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import java.util.concurrent.locks.ReentrantReadWriteLock
import org.netbeans.api.java.classpath.ClassPath
import org.netbeans.api.java.queries.UnitTestForSourceQuery
import org.netbeans.api.project.Project
import org.netbeans.api.project.ProjectUtils
import org.netbeans.modules.scala.sbt.project.ProjectConstants
import org.netbeans.spi.java.classpath.ClassPathFactory
import org.netbeans.spi.java.classpath.ClassPathImplementation
import org.netbeans.spi.java.classpath.ClassPathProvider
import org.openide.filesystems.FileObject
import org.openide.filesystems.FileUtil

import scala.collection.mutable

/**
 * 
 * @author Caoyuan Deng
 */
class SBTClassPathProvider(project: Project) extends ClassPathProvider with PropertyChangeListener {
  import ProjectConstants._
  
  private val reentrantLock = new ReentrantReadWriteLock()
  private val rlock = reentrantLock.readLock
  private val wlock = reentrantLock.writeLock
  private var sourceRoots: List[FileObject] = _
  private var testSourceRoots: List[FileObject] = _
  private val cache = new mutable.HashMap[String, ClassPath]()

  def findClassPath(fileObject: FileObject, scope: String): ClassPath = {
    getFileType(fileObject) match {
      case SOURCE => getClassPath(scope)
      case TEST_SOURCE => getClassPath(scope)
      case _ => null
    }
  }

  def getClassPath(scope: String): ClassPath = synchronized {
    cache.getOrElseUpdate(scope, {
        val scpi = new SBTClassPath(project, scope)
        val cp = ClassPathFactory.createClassPath(scpi)
        scpi.addPropertyChangeListener(this)
        cache += scope -> cp
        cp
      }
    )
  }

  def propertyChange(evt: PropertyChangeEvent) {
    if (ClassPathImplementation.PROP_RESOURCES == evt.getPropertyName) {
      clearCache
    }
  }
  
  def getFileType(fo: FileObject): FileType = {
    rlock.lock
    try {
      if (sourceRoots == null) {
        try {
          rlock.unlock
          wlock.lock
          val sources = ProjectUtils.getSources(project)
          val allSources = sources.getSourceGroups(SOURCES_TYPE_JAVA) ++ sources.getSourceGroups(SOURCES_TYPE_SCALA)
          sourceRoots = List[FileObject]()
          testSourceRoots = List[FileObject]()
          for (sourceGroup <- allSources) {
            val sourceFo = sourceGroup.getRootFolder
            if (UnitTestForSourceQuery.findSources(sourceFo).length > 0) {
              testSourceRoots ::= sourceFo
            } else {
              sourceRoots ::= sourceFo
            }
          }
        } finally {
          rlock.lock
          wlock.unlock
        }
      }

      def contains(root: FileObject) = (root eq fo) || FileUtil.isParentOf(root, fo)
      sourceRoots find contains match {
        case None =>
          testSourceRoots find contains match {
            case None => UNKNOWN
            case _ => TEST_SOURCE
          }
        case _ => SOURCE
      }
    } finally {
      rlock.unlock
    }
  }

  private def clearCache {
    cache.clear
  }
}
