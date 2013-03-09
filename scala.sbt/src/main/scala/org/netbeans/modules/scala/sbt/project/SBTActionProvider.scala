package org.netbeans.modules.scala.sbt.project

import org.netbeans.modules.scala.sbt.console.SBTConsoleTopComponent
import org.netbeans.modules.scala.console.shell.ScalaConsoleTopComponent
import org.netbeans.spi.project.ActionProvider
import org.openide.util.Lookup

/**
 * 
 * Used for predefined project actions, @see org.netbeans.spi.project.ActionProvider
 * 
 * @author Caoyuan Deng
 */
class SBTActionProvider(project: SBTProject) extends ActionProvider {
  import SBTActionProvider._
  
  /**
   * also @see ProjectSensitiveActions.projectCommandAction(SBTActionProvider.COMMAND_SBT_CONSOLE, "Open sbt", null) in
   * SBTProjectLogicalView.getActions
   */
  def getSupportedActions() = Array(
    COMMAND_SBT_CONSOLE,
    COMMAND_SCALA_CONSOLE,
    COMMAND_SBT_RELOAD,
    COMMAND_BUILD,
    COMMAND_REBUILD,
    COMMAND_CLEAN
  )
  
  @throws(classOf[IllegalArgumentException])
  def isActionEnabled(command: String, context: Lookup): Boolean = {
    true
  }
  
  def invokeAction(command: String, context: Lookup) {
    command match {
      case COMMAND_SBT_CONSOLE => 
        val rootProject = project.getRootProject
        val id = project.getId
        val commands = project.getId match {
          case null => Nil
          case id   => List("project " + id)
        }
        SBTConsoleTopComponent.openInstance(rootProject, false, commands)()
        
      case COMMAND_SCALA_CONSOLE => 
        ScalaConsoleTopComponent.openInstance(project, false, Nil)()

      case COMMAND_SBT_RELOAD => 
        val sbtResolver = project.getLookup.lookup(classOf[SBTResolver])
        sbtResolver.isResolvedOrResolving = false
        sbtResolver.triggerSbtResolution
        
      case COMMAND_BUILD =>
        val rootProject = project.getRootProject
        val commands = project.getId match {
          case null => List("compile")
          case id   => List("project " + id, 
                            "compile")
        }
        SBTConsoleTopComponent.openInstance(rootProject, false, commands)()
        
      case COMMAND_REBUILD =>
        val rootProject = project.getRootProject
        val commands = project.getId match {
          case null => List("compile")
          case id   => List("project " + id,
                            "clean",
                            "compile")
        }
        SBTConsoleTopComponent.openInstance(rootProject, false, commands)()
        
      case COMMAND_CLEAN =>
        val rootProject = project.getRootProject
        val commands = project.getId match {
          case null => List("compile")
          case id   => List("project " + id, 
                            "clean")
        }
        SBTConsoleTopComponent.openInstance(rootProject, false, commands)()
        
      case _ =>
        
    }
  }
  
}

object SBTActionProvider {
  val COMMAND_SBT_CONSOLE = "sbt-console"
  val COMMAND_SCALA_CONSOLE = "scala-console"
  val COMMAND_SBT_RELOAD  = "sbt-reload"
  
  val COMMAND_BUILD   = ActionProvider.COMMAND_BUILD    // compile
  val COMMAND_REBUILD = ActionProvider.COMMAND_REBUILD  // clean and compile
  val COMMAND_CLEAN   = ActionProvider.COMMAND_CLEAN    // clean
}
