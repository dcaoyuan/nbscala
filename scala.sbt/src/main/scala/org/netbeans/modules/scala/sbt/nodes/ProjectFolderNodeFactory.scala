package org.netbeans.modules.scala.sbt.nodes

import javax.swing.event.ChangeListener
import org.netbeans.api.project.Project
import org.netbeans.modules.scala.sbt.project.ProjectConstants
import org.netbeans.spi.project.ui.support.NodeFactory
import org.netbeans.spi.project.ui.support.NodeList
import org.openide.filesystems.FileObject
import org.openide.loaders.DataObject
import org.openide.loaders.DataObjectNotFoundException
import org.openide.nodes.FilterNode
import org.openide.nodes.Node
import org.openide.util.ChangeSupport
import org.openide.util.Exceptions
import org.openide.util.NbBundle

class ProjectFolderNodeFactory extends NodeFactory {
  def createNodes(project: Project): NodeList[_] = new ProjectFolderNodeFactory.ProjectFolderNodeList(project)
}

object ProjectFolderNodeFactory {
  private val PROJECT_FOLDER = "project-folder"
  private val PROJECT_FOLDER_NAME = "project"
  private val DISPLAY_NAME = NbBundle.getMessage(classOf[ProjectFolderNodeFactory], "CTL_ProjectFolder")

  private class ProjectFolderNodeList(project: Project) extends NodeList[String] {
    private val cs = new ChangeSupport(this)

    def keys: java.util.List[String] = {
      val theKeys = new java.util.ArrayList[String]()
      theKeys.add(PROJECT_FOLDER)
      theKeys
    }

    /**
     * return null if node for this key doesn't exist currently
     */
    def node(key: String): Node = {
      project.getProjectDirectory.getFileObject(ProjectConstants.PROJECT_FOLDER_NAME) match {
        case projectFolder: FileObject if projectFolder.isFolder =>
          try {
            DataObject.find(projectFolder) match {
              case null => null
              case dobj =>
                new FilterNode(dobj.getNodeDelegate) {
                  override def getDisplayName = DISPLAY_NAME
                }
            }
          } catch {
            case ex: DataObjectNotFoundException => Exceptions.printStackTrace(ex); null
          }
        case _ => null
      }
    }

    def addNotify() {

    }

    def removeNotify() {

    }

    def addChangeListener(l: ChangeListener) {
      cs.addChangeListener(l)
    }

    def removeChangeListener(l: ChangeListener) {
      cs.removeChangeListener(l)
    }
  }

}
