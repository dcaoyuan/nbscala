package org.netbeans.modules.scala.sbt.project

import java.io.IOException
import javax.swing.event.ChangeListener
import org.netbeans.api.project.Project
import org.netbeans.api.project.ProjectManager
import org.netbeans.spi.project.SubprojectProvider
import org.openide.filesystems.FileUtil
import org.openide.util.Exceptions

/**
 * 
 * @author Caoyuan Deng
 */
class SBTSubProjectProvider(project: SBTProject) extends SubprojectProvider {
  private lazy val sbtResolver = project.getLookup.lookup(classOf[SBTResolver])
  
  override
  def getSubprojects: java.util.Set[_ <: Project] = {
    loadProjects
  }

  private def loadProjects: java.util.Set[_ <: Project] = {
    val subProjects = new java.util.HashSet[SBTProject]()
    try {
      val projectFos = sbtResolver.getAggregateProjects map FileUtil.toFileObject
      for (projectFo <- projectFos) {
        ProjectManager.getDefault.findProject(projectFo) match {
          case x: SBTProject => subProjects.add(x)
          case _ =>
        }
      }
    } catch {
      case ex: IOException => Exceptions.printStackTrace(ex)
      case ex: IllegalArgumentException => Exceptions.printStackTrace(ex)
    }
    
    java.util.Collections.unmodifiableSet(subProjects)
  }

  override
  def addChangeListener(cl: ChangeListener) {
  }

  override
  def removeChangeListener(cl: ChangeListener) {
  }
    
}
