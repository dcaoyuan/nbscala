package org.netbeans.modules.scala.sbt.project

import org.netbeans.spi.project.ActionProvider
import org.openide.util.Lookup

/**
 * 
 * Used for predefined project actions, @see org.netbeans.spi.project.ActionProvider
 */
class SBTActionProvider(project: SBTProject) extends ActionProvider {
  def getSupportedActions() = {
    Array[String]()
  }
  
  @throws(classOf[IllegalArgumentException])
  def isActionEnabled(command: String, context: Lookup): Boolean = {
    true
  }
  
  def invokeAction(command: String, context: Lookup) {
    if (command.equalsIgnoreCase("Open SBT Console")){
    }
  }
}
