package org.netbeans.modules.scala.sbt.project

import org.netbeans.modules.scala.sbt.classpath.SBTResolver
import org.netbeans.modules.scala.sbt.console.SBTConsoleTopComponent
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
  def getSupportedActions() = Array[String](
    COMMAND_SBT_CONSOLE,
    COMMAND_SBT_RELOAD
  )
  
  @throws(classOf[IllegalArgumentException])
  def isActionEnabled(command: String, context: Lookup): Boolean = {
    true
  }
  
  def invokeAction(command: String, context: Lookup) {
    command.toLowerCase match {
      case COMMAND_SBT_CONSOLE => 
        (project.getProjectChain match {
            case root :: Nil  => (root, List("project " + root.getName))
            case root :: rest => (root, rest map ("project " + _.getName))
          }
        ) match {case (root, commands) => SBTConsoleTopComponent.openInstance(root, false, commands)()}
        
      case COMMAND_SBT_RELOAD => 
        val sbtResolver = project.getLookup.lookup(classOf[SBTResolver])
        sbtResolver.triggerSbtResolution
    }
  }
}

object SBTActionProvider {
  val COMMAND_SBT_CONSOLE = "sbt-console"
  val COMMAND_SBT_RELOAD  = "sbt-reload"
}
