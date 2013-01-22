package org.netbeans.modules.scala.sbt.project

import java.awt.Image
import java.beans.PropertyChangeListener
import javax.swing.Action
import javax.swing.Icon
import javax.swing.ImageIcon
import org.netbeans.api.project.Project
import org.netbeans.api.project.ProjectInformation
import org.netbeans.spi.project.ProjectState
import org.netbeans.spi.project.ui.LogicalViewProvider
import org.netbeans.spi.project.ui.support.CommonProjectActions
import org.openide.filesystems.FileObject
import org.openide.loaders.DataFolder
import org.openide.loaders.DataObjectNotFoundException
import org.openide.nodes.AbstractNode
import org.openide.nodes.Children
import org.openide.nodes.FilterNode
import org.openide.nodes.Node
import org.openide.util.Exceptions
import org.openide.util.ImageUtilities
import org.openide.util.Lookup
import org.openide.util.lookup.Lookups
import org.openide.util.lookup.ProxyLookup

/**
 * 
 * @author Caoyuan Deng
 */
class SBTProject(projectDir: FileObject, state: ProjectState) extends Project {
  private lazy val lookup: Lookup = Lookups.fixed(
    new Info(),
    new SBTProjectLogicalView(this),
    new SBTProjectOpenedHook(this)
  )

  override
  def getProjectDirectory = projectDir

  override
  def getLookup: Lookup = lookup
  
  private final class Info extends ProjectInformation {

    override
    def getIcon: Icon = new ImageIcon(SBTProject.SBT_ICON)

    override
    def getName: String = {
      getProjectDirectory.getName
    }

    override
    def getDisplayName: String = {
      getName
    }

    override
    def addPropertyChangeListener(pcl: PropertyChangeListener) {
      //do nothing, won't change
    }

    override
    def removePropertyChangeListener(pcl: PropertyChangeListener) {
      //do nothing, won't change
    }

    override
    def getProject: Project = SBTProject.this
  }
  
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
        new SBTConsoleAction(project),
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
}

object SBTProject {
  private lazy val SBT_ICON = ImageUtilities.loadImage("org/netbeans/modules/scala/sbt/resources/sbt.png")
}