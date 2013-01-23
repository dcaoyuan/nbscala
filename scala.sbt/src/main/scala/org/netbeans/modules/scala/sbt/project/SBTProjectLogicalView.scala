package org.netbeans.modules.scala.sbt.project

import java.awt.Image
import javax.swing.Action
import org.netbeans.spi.project.ui.LogicalViewProvider
import org.netbeans.spi.project.ui.support.CommonProjectActions
import org.netbeans.spi.project.ui.support.ProjectSensitiveActions
import org.openide.loaders.DataFolder
import org.openide.loaders.DataObjectNotFoundException
import org.openide.nodes.AbstractNode
import org.openide.nodes.Children
import org.openide.nodes.FilterNode
import org.openide.nodes.Node
import org.openide.util.Exceptions
import org.openide.util.lookup.Lookups
import org.openide.util.lookup.ProxyLookup

class SBTProjectLogicalView(project: SBTProject) extends LogicalViewProvider {

  override
  def createLogicalView: Node = {
    try {
      //Obtain the project directory's node:
      val projectDirectory = project.getProjectDirectory
      val projectFolder = DataFolder.findFolder(projectDirectory)
      val nodeOfProjectFolder = projectFolder.getNodeDelegate
      //Decorate the project directory's node:
      new ProjectNode(nodeOfProjectFolder, project)
    } catch {
      case donfe: DataObjectNotFoundException =>
        Exceptions.printStackTrace(donfe)
        //Fallback-the directory couldn't be created -
        //read-only filesystem or something evil happened
        new AbstractNode(Children.LEAF)
    }
  }

  private final class ProjectNode(node: Node, project: SBTProject) extends FilterNode(
    node, 
    new FilterNode.Children(node), 
    new ProxyLookup(Lookups.singleton(project), node.getLookup)
  ) {

    override
    def getActions(arg0: Boolean): Array[Action] = Array(
      ProjectSensitiveActions.projectCommandAction(SBTActionProvider.COMMAND_SBT_CONSOLE, "Sbt", null),
      CommonProjectActions.newFileAction,
      CommonProjectActions.copyProjectAction,
      CommonProjectActions.deleteProjectAction,
      CommonProjectActions.closeProjectAction
    )

    override
    def getIcon(tpe: Int): Image = SBTProject.SBT_ICON

    override
    def getOpenedIcon(tpe: Int): Image = getIcon(tpe)

    override
    def getDisplayName: String = {
      project.getProjectDirectory.getName
    }
  }

  override
  def findPath(root: Node, target: Object): Node = {
    //leave unimplemented for now
    null
  }

}