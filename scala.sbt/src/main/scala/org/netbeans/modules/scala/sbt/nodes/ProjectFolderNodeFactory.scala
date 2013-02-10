package org.netbeans.modules.scala.sbt.nodes

import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import javax.swing.SwingUtilities
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

class ProjectFolderNodeFactory extends NodeFactory {
  import ProjectFolderNodeFactory._
  
  def createNodes(project: Project): NodeList[_] = new ProjectFolderNodeList(project)
}

object ProjectFolderNodeFactory {
  private val PROJECT_FOLDER = "project-folder"
  private val PROJECT_FOLDER_NAME = "project"
    
  private class ProjectFolderNodeList(project: Project) extends NodeList[String] with PropertyChangeListener {
    private val changeSupport = new ChangeSupport(this)

    def keys: java.util.List[String] = {
      val theKeys = new java.util.ArrayList[String]()
      theKeys.add(PROJECT_FOLDER)
      theKeys
    }

    def addChangeListener(l: ChangeListener) {
      changeSupport.addChangeListener(l)
    }

    def removeChangeListener(l: ChangeListener) {
      changeSupport.removeChangeListener(l)
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
              case dobj => new FilterNode(dobj.getNodeDelegate)
            }
          } catch {
            case ex: DataObjectNotFoundException => Exceptions.printStackTrace(ex); null
          }        
        case _ => null
      }
    }

    def addNotify() {}

    def removeNotify() {}

    def propertyChange(evt: PropertyChangeEvent) {
      // The caller holds ProjectManager.mutex() read lock
      SwingUtilities.invokeLater(new Runnable() {
          def run() {
            changeSupport.fireChange
          }
        })
    }
  }
  
}
