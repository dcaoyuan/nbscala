package org.netbeans.modules.scala.sbt.classpath

import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import java.beans.PropertyChangeSupport
import org.netbeans.api.java.classpath.ClassPath
import org.netbeans.api.java.platform.JavaPlatformManager
import org.netbeans.api.project.FileOwnerQuery
import org.netbeans.api.project.Project
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
final class SBTClassPath(project: Project, scope: String) extends ClassPathImplementation with PropertyChangeListener {

  private val pcs = new PropertyChangeSupport(this)
  private val sbtController = project.getLookup.lookup(classOf[SBTController])
  sbtController.addPropertyChangeListener(this)

  def getResources: java.util.List[PathResourceImplementation] = {
    if (!sbtController.isEnabled) {
      java.util.Collections.emptyList()
    } else {
      val result = new java.util.ArrayList[PathResourceImplementation]()
      if (scope == ClassPath.BOOT) {
        result.addAll(getJavaBootResources)
      }

      for (fo <- sbtController.getResolvedLibraries(scope)) {
        try {
          FileOwnerQuery.markExternalOwner(fo, project, FileOwnerQuery.EXTERNAL_ALGORITHM_TRANSIENT)
          val root = if (FileUtil.isArchiveFile(fo)) {
            FileUtil.getArchiveRoot(fo)
          } else {
            fo
          }
          result.add(ClassPathSupport.createResource(root.toURL))
        } catch {
          case ex: FileStateInvalidException => Exceptions.printStackTrace(ex)
        }
      }

      result
    }
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

  def propertyChange(evt: PropertyChangeEvent) {
    if (SBTController.SBT_LIBRARY_RESOLVED == evt.getPropertyName) {
      pcs.firePropertyChange(ClassPathImplementation.PROP_RESOURCES, null, null)
    }
  }

}
