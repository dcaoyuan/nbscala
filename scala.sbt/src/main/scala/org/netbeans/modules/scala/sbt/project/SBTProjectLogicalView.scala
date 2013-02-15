package org.netbeans.modules.scala.sbt.project

import java.awt.Image
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import javax.swing.Action
import javax.swing.SwingUtilities
import org.netbeans.api.project.FileOwnerQuery
import org.netbeans.api.project.Project
import org.netbeans.spi.java.project.support.ui.PackageView
import org.netbeans.spi.project.ui.LogicalViewProvider
import org.netbeans.spi.project.ui.support.CommonProjectActions
import org.netbeans.spi.project.ui.support.NodeFactorySupport
import org.netbeans.spi.project.ui.support.ProjectSensitiveActions
import org.openide.filesystems.FileObject
import org.openide.loaders.DataFolder
import org.openide.loaders.DataObjectNotFoundException
import org.openide.nodes.AbstractNode
import org.openide.nodes.Children
import org.openide.nodes.Node
import org.openide.util.Exceptions
import org.openide.util.NbBundle
import org.openide.util.lookup.Lookups
import org.openide.util.lookup.ProxyLookup

/**
 * 
 * @author Caoyuan Deng
 */
class SBTProjectLogicalView(project: Project) extends LogicalViewProvider {
  import SBTProjectLogicalView._
  
  private lazy val sbtResolver = project.getLookup.lookup(classOf[SBTResolver])
    
  override
  def createLogicalView: Node = {
    try {
      // obtain the project directory's node:
      val projectDirectory = project.getProjectDirectory
      val projectFolder = DataFolder.findFolder(projectDirectory)
      val nodeOfProjectFolder = projectFolder.getNodeDelegate
      // decorate the project directory's node:
      new ProjectNode(nodeOfProjectFolder, project)
    } catch {
      case donfe: DataObjectNotFoundException =>
        Exceptions.printStackTrace(donfe)
        // fallback-the directory couldn't be created -
        // read-only filesystem or something evil happened
        new AbstractNode(Children.LEAF)
    }
  }

  private final class ProjectNode(node: Node, project: Project) extends AbstractNode(
    NodeFactorySupport.createCompositeChildren(project, NODE_FACTORY_FOLDER_PATH),
    new ProxyLookup(Lookups.singleton(project), node.getLookup)
  ) with PropertyChangeListener {

    sbtResolver.addPropertyChangeListener(this)
    
    override
    def getActions(arg0: Boolean): Array[Action] = Array(
      ProjectSensitiveActions.projectCommandAction(SBTActionProvider.COMMAND_SBT_CONSOLE, NbBundle.getMessage(classOf[SBTActionProvider], "CTL_OpenSbtAction"), null),
      ProjectSensitiveActions.projectCommandAction(SBTActionProvider.COMMAND_SBT_RELOAD,  NbBundle.getMessage(classOf[SBTActionProvider], "CTL_ReloadSbtAction"), null),
      ProjectSensitiveActions.projectCommandAction(SBTActionProvider.COMMAND_BUILD, NbBundle.getMessage(classOf[SBTActionProvider], "CTL_BuildAction"), null),
      ProjectSensitiveActions.projectCommandAction(SBTActionProvider.COMMAND_CLEAN,  NbBundle.getMessage(classOf[SBTActionProvider], "CTL_CleanAction"), null),
      ProjectSensitiveActions.projectCommandAction(SBTActionProvider.COMMAND_REBUILD, NbBundle.getMessage(classOf[SBTActionProvider], "CTL_RebuildAction"), null),
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
    
    override
    def getShortDescription = project.getProjectDirectory.getPath
    
    override 
    def destroy() {
      sbtResolver.removePropertyChangeListener(this)
      super.destroy
    }

    def propertyChange(evt: PropertyChangeEvent) {
      evt.getPropertyName match {
        case SBTResolver.DESCRIPTOR_CHANGE => 
          // The caller holds ProjectManager.mutex() read lock
          SwingUtilities.invokeLater(new Runnable() {
              def run() {
                ProjectNode.this.setChildren(NodeFactorySupport.createCompositeChildren(project, NODE_FACTORY_FOLDER_PATH))
              }
            })
        case _ =>
      }
    }
  }
  
  /**
   * Try to find a given node in the logical view. If some node within the logical 
   * view tree has the supplied object in its lookup, it ought to be returned if 
   * that is practical. If there are multiple such nodes, the one most suitable 
   * for display to the user should be returned.
   * This may be used to select nodes corresponding to files, etc. 
   */
  override
  def findPath(root: Node, target: Object): Node = {
    val project = root.getLookup().lookup(classOf[Project])
    if (project == null) {
      return null
    }
        
    target match {
      case fo: FileObject =>
        val owner = FileOwnerQuery.getOwner(fo)
        if (project != owner) {
          return null // Don't waste time if project does not own the fo
        }
            
        for (n <- root.getChildren.getNodes(true)) {
          val result = PackageView.findPath(n, target)
          if (result != null) {
            return result
          }
        }
      case _ => 
    }
        
    null
  }
  
}

object SBTProjectLogicalView {
  val NODE_FACTORY_FOLDER_PATH = "Projects/org-netbeans-modules-scala-sbt/Nodes"
}