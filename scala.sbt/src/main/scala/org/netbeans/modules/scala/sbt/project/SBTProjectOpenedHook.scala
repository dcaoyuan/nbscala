package org.netbeans.modules.scala.sbt.project

import org.netbeans.api.java.classpath.ClassPath
import org.netbeans.api.java.classpath.GlobalPathRegistry
import org.netbeans.api.project.Project
import org.netbeans.modules.scala.sbt.classpath.SBTClassPathProvider
import org.netbeans.spi.project.ui.ProjectOpenedHook

/**
 * 
 * @author Caoyuan Deng
 */
class SBTProjectOpenedHook(project: Project) extends ProjectOpenedHook {
  private var classpaths: Array[ClassPath] = _
  
  override
  protected def projectOpened() {
    val cpProvider = project.getLookup.lookup(classOf[SBTClassPathProvider])
    classpaths = Array(
      cpProvider.getClassPath(ClassPath.COMPILE, isTest = false),
      cpProvider.getClassPath(ClassPath.COMPILE, isTest = true)
    )
    
    GlobalPathRegistry.getDefault.register(ClassPath.COMPILE, classpaths)
  }

  override
  protected def projectClosed() {
    GlobalPathRegistry.getDefault.unregister(ClassPath.COMPILE, classpaths)
    SBTResolver.dirWatcher.removeChangeListener(project.getLookup.lookup(classOf[SBTResolver]))
  }
}
