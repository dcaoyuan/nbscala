/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.netbeans.modules.scala.sbt.project

import java.io.IOException
import javax.swing.event.ChangeListener
import org.netbeans.api.project.Project
import org.netbeans.api.project.ProjectManager
import org.netbeans.modules.scala.sbt.classpath.SBTController
import org.netbeans.spi.project.SubprojectProvider
import org.openide.filesystems.FileUtil
import org.openide.util.Exceptions

class SBTDepProjectProvider(project: SBTProject) extends SubprojectProvider {

  override
  def getSubprojects: java.util.Set[_ <: Project] = {
    loadProjects
  }

  private def loadProjects: java.util.Set[_ <: Project] = {
    val depProjects = new java.util.HashSet[SBTProject]()
    val sbtController = project.getLookup.lookup(classOf[SBTController])
    try {
      val projectFos = sbtController.getDependenciesProjects map FileUtil.toFileObject
      for (projectFo <- projectFos) {
        ProjectManager.getDefault.findProject(projectFo) match {
          case depProject: SBTProject => depProjects.add(depProject)
          case _ =>
        }
      }
    } catch {
      case ex: IOException => Exceptions.printStackTrace(ex)
      case ex: IllegalArgumentException => Exceptions.printStackTrace(ex)
    }
    
    java.util.Collections.unmodifiableSet(depProjects)
  }

  override
  def addChangeListener(cl: ChangeListener) {
  }

  override
  def removeChangeListener(cl: ChangeListener) {
  }
    
}