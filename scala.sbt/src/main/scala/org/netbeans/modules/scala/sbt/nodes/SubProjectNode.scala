package org.netbeans.modules.scala.sbt.nodes

import java.awt.Image
import java.awt.event.ActionEvent
import javax.swing.AbstractAction
import javax.swing.Action
import org.netbeans.api.project.Project
import org.netbeans.api.project.ui.OpenProjects
import org.netbeans.modules.scala.sbt.project.SBTProject
import org.openide.nodes.AbstractNode
import org.openide.nodes.Children
import org.openide.util.NbBundle

/**
 * 
 * @author Caoyuan Deng
 */
class SubProjectNode(project: Project) extends AbstractNode(Children.LEAF) {

  override
  def getIcon(tpe: Int): Image = SBTProject.SBT_ICON

  override
  def getOpenedIcon(tpe: Int): Image = getIcon(tpe)

  override
  def getDisplayName: String = {
    project.getProjectDirectory.getName
  }

  /**
   * Tooltip
   */
  override 
  def getShortDescription = project.getProjectDirectory.getPath

  override
  def getPreferredAction = OpenAction
  
  override
  def getActions(arg0: Boolean): Array[Action] = Array(
    OpenAction
  )

  object OpenAction extends AbstractAction {
    putValue(Action.NAME, NbBundle.getMessage(classOf[SubProjectNode], "BTN_Open_Project"))

    def actionPerformed(event: ActionEvent) {
      OpenProjects.getDefault.open(Array(project), false)
    }
  }
}