package org.netbeans.modules.scala.sbt.project

import org.netbeans.modules.scala.sbt.console.SBTConsoleTopComponent
import org.netbeans.spi.project.ActionProvider
import org.openide.util.Lookup

/**
 * 
 * Used for predefined project actions, @see org.netbeans.spi.project.ActionProvider
 */
class SBTActionProvider(project: SBTProject) extends ActionProvider {
  import SBTActionProvider._
  
  def getSupportedActions() = Array[String](
    COMMAND_SBT_CONSOLE
  )
  
  @throws(classOf[IllegalArgumentException])
  def isActionEnabled(command: String, context: Lookup): Boolean = {
    true
  }
  
  def invokeAction(command: String, context: Lookup) {
    command.toLowerCase match {
      case COMMAND_SBT_CONSOLE =>
        val win = SBTConsoleTopComponent.findInstance(project)
        if (win != null) {
          win.open
          win.requestActive
        }
    }
  }
}

object SBTActionProvider {
  val COMMAND_SBT_CONSOLE = "sbt-console"
}
