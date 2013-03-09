package org.netbeans.modules.scala.sbt.classpath

import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import java.beans.PropertyChangeSupport
import java.net.URI
import org.netbeans.api.java.classpath.ClassPath
import org.netbeans.api.java.platform.JavaPlatformManager
import org.netbeans.api.project.FileOwnerQuery
import org.netbeans.api.project.Project
import org.netbeans.modules.scala.sbt.project.SBTResolver
import org.netbeans.spi.java.classpath.ClassPathImplementation
import org.netbeans.spi.java.classpath.PathResourceImplementation
import org.netbeans.spi.java.classpath.support.ClassPathSupport
import org.openide.filesystems.FileStateInvalidException
import org.openide.filesystems.FileUtil
import org.openide.util.Exceptions


/**
 *
 * @author Caoyuan Deng
 */
final class SBTClassPath(project: Project, scope: String, isTest: Boolean) extends ClassPathImplementation {
  private val pcs = new PropertyChangeSupport(this)
  
  private lazy val sbtResolver = {
    val x = project.getLookup.lookup(classOf[SBTResolver])

    x.addPropertyChangeListener(new PropertyChangeListener() {
        def propertyChange(evt: PropertyChangeEvent) {
          evt.getPropertyName match {
            case SBTResolver.DESCRIPTOR_CHANGE => 
              pcs.firePropertyChange(ClassPathImplementation.PROP_RESOURCES, null, null)
            case _ =>
          }
        }
      })
    
    x
  }

  def getResources: java.util.List[PathResourceImplementation] = {
    val result = new java.util.ArrayList[PathResourceImplementation]()
    if (scope == ClassPath.BOOT) {
      result.addAll(getJavaBootResources)
    }

    for (file <- sbtResolver.getResolvedClassPath(scope, isTest)) {
      val fo = FileUtil.toFileObject(file)
      try {
        val rootUrl = 
          if (fo != null) {
            if (FileUtil.isArchiveFile(fo)) {
              FileOwnerQuery.markExternalOwner(fo, project, FileOwnerQuery.EXTERNAL_ALGORITHM_TRANSIENT)
              FileUtil.getArchiveRoot(fo).toURL
            } else {
              file.toURI.toURL
            }
          } else { // fo is null, file does not exist
            // file is a classes/srcs *folder* and may not exist yet, we must add a slash at the end.
            // to tell ClassPathSupport that it's a folder instead of a general file
            // @Note should avoid url string ends with doubled "/", i.e. "//"
            val uriStr = file.toURI.toString
            URI.create(if (uriStr.endsWith("/")) uriStr else uriStr + "/").toURL
          }
        result.add(ClassPathSupport.createResource(rootUrl))
      } catch {
        case ex: FileStateInvalidException => Exceptions.printStackTrace(ex)
      }
    }

    result
  }

  private def getJavaBootResources: java.util.List[PathResourceImplementation] = {
    val result = new java.util.ArrayList[PathResourceImplementation]()
    val platformManager = JavaPlatformManager.getDefault
    val javaPlatform = platformManager.getDefaultPlatform

    // XXX todo cache it ?
    if (javaPlatform != null) {
      val cp = javaPlatform.getBootstrapLibraries
      assert(cp != null, javaPlatform)
      val entries = cp.entries.iterator
      while (entries.hasNext) {
        val entry = entries.next
        result.add(ClassPathSupport.createResource(entry.getURL))
      }
    }
    result
  }

  def removePropertyChangeListener(listener: PropertyChangeListener) {
    pcs.removePropertyChangeListener(listener)
  }

  def addPropertyChangeListener(listener: PropertyChangeListener) {
    pcs.addPropertyChangeListener(listener)
  }
}
