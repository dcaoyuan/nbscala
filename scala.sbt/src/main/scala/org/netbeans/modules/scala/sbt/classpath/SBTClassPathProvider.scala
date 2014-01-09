package org.netbeans.modules.scala.sbt.classpath

import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import java.util.concurrent.locks.ReentrantReadWriteLock
import org.netbeans.api.java.classpath.ClassPath
import org.netbeans.api.project.Project
import org.netbeans.modules.scala.core.ProjectResources
import org.netbeans.modules.scala.sbt.project.ProjectConstants
import org.netbeans.spi.java.classpath.ClassPathFactory
import org.netbeans.spi.java.classpath.ClassPathImplementation
import org.netbeans.spi.java.classpath.ClassPathProvider
import org.openide.filesystems.FileObject
import org.openide.filesystems.FileUtil
import scala.collection.mutable

/**
 * Defines the various class paths for a sbt project.
 *
 * @author Caoyuan Deng
 */
class SBTClassPathProvider(project: Project) extends ClassPathProvider with PropertyChangeListener {
  import ProjectConstants._

  private trait FileType
  private case object MAIN_SOURCE extends FileType
  private case object TEST_SOURCE extends FileType
  private case object UNKNOWN extends FileType

  private val rock = new ReentrantReadWriteLock()
  private var mainSrcRoots: Set[FileObject] = _
  private var testSrcRoots: Set[FileObject] = _
  private val cache = new mutable.HashMap[String, ClassPath]()

  def findClassPath(fileObject: FileObject, tpe: String): ClassPath = {
    getFileType(fileObject) match {
      case MAIN_SOURCE => getClassPath(tpe, isTest = false)
      case TEST_SOURCE => getClassPath(tpe, isTest = true)
      case _           => null
    }
  }

  def getClassPath(tpe: String, isTest: Boolean): ClassPath = cache synchronized {
    val cpi = new SBTClassPath(project, tpe, isTest)
    ClassPathFactory.createClassPath(cpi)
  }

  /**
   * It seems this method cannot refresh "clean" build, which will recreate a "classes"
   * folder  TODO where is the properties source? implemented in ClassPathProvider ?
   */
  def getClassPath_cached(tpe: String, isTest: Boolean): ClassPath = cache synchronized {
    val cacheKey = tpe + (if (isTest) "/main" else "/test")
    cache.getOrElseUpdate(cacheKey, {
      val cpi = new SBTClassPath(project, tpe, isTest)
      val cp = ClassPathFactory.createClassPath(cpi)
      cpi.addPropertyChangeListener(this)
      cache += cacheKey -> cp
      cp
    })
  }

  def propertyChange(evt: PropertyChangeEvent) {
    evt.getPropertyName match {
      case ClassPathImplementation.PROP_RESOURCES => cache synchronized {
        clearCache
        mainSrcRoots = null
        testSrcRoots = null
      }
      case _ =>
    }
  }

  private def getFileType(fo: FileObject): FileType = {
    rock.readLock.lock
    try {
      if (mainSrcRoots == null) {
        try {
          rock.readLock.unlock
          rock.writeLock.lock
          val (mainSrcs, testSrcs) = ProjectResources.findMainAndTestSrcs(project)
          mainSrcRoots = mainSrcs
          testSrcRoots = testSrcs
        } finally {
          rock.readLock.lock
          rock.writeLock.unlock
        }
      }

      // always find mainSrcRoots first, since the fo may also be included in testSrcRoots
      mainSrcRoots find contains(fo) match {
        case None =>
          testSrcRoots find contains(fo) match {
            case None => UNKNOWN
            case _    => TEST_SOURCE
          }
        case _ => MAIN_SOURCE
      }
    } finally {
      rock.readLock.unlock
    }
  }

  private def contains(fo: FileObject)(root: FileObject) = root.equals(fo) || FileUtil.isParentOf(root, fo)

  private def clearCache {
    cache.clear
  }
}
