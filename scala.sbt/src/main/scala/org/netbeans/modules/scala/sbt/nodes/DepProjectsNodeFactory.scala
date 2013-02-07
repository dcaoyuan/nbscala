package org.netbeans.modules.scala.sbt.nodes

import java.awt.Image
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import javax.swing.Action
import javax.swing.SwingUtilities
import javax.swing.event.ChangeListener
import org.netbeans.api.project.Project
import org.netbeans.modules.scala.sbt.project.SBTDepProjectProvider
import org.netbeans.modules.scala.sbt.project.SBTProject
import org.netbeans.spi.project.ui.support.NodeFactory
import org.netbeans.spi.project.ui.support.NodeList
import org.openide.loaders.DataObject
import org.openide.loaders.DataObjectNotFoundException
import org.openide.nodes.AbstractNode
import org.openide.nodes.Children
import org.openide.nodes.FilterNode
import org.openide.nodes.Node
import org.openide.util.ChangeSupport
import org.openide.util.Exceptions
import org.openide.util.ImageUtilities
import org.openide.util.NbBundle

class DepProjectsNodeFactory extends NodeFactory {
  import DepProjectsNodeFactory._
  
  def createNodes(project: Project): NodeList[_] = {
    new DepProjectsNodeList(project)
  }
}

object DepProjectsNodeFactory {
  private val DEP_PROJECTS = "dep-projects"
  private val ICON_LIB_BADGE = ImageUtilities.loadImage("org/netbeans/modules/java/j2seproject/ui/resources/libraries-badge.png")    //NOI18N
    
  private class DepProjectsNodeList(project: Project) extends NodeList[String] with PropertyChangeListener {
    private val changeSupport = new ChangeSupport(this)

    def keys: java.util.List[String] = {
      if (project.getProjectDirectory == null || !project.getProjectDirectory.isValid) {
        return java.util.Collections.emptyList()
      }

      val theKeys = new java.util.ArrayList[String]()
      theKeys.add(DEP_PROJECTS)
      theKeys
    }

    def addChangeListener(l: ChangeListener) {
      changeSupport.addChangeListener(l)
    }

    def removeChangeListener(l: ChangeListener) {
      changeSupport.removeChangeListener(l)
    }

    def node(key: String): Node = {
      try {
        new DepProjectNode(project)
      } catch {
        case ex: DataObjectNotFoundException => Exceptions.printStackTrace(ex); null
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
  
  private class DepProjectNode(project: Project) extends AbstractNode(new DepProjectsChildren(project)) {
    private val NODE_NAME = NbBundle.getMessage(classOf[DepProjectsNodeFactory], "CTL_DepProjectsNode")

    override
    def getDisplayName: String = NODE_NAME

    override
    def getName: String = NODE_NAME

    override
    def getIcon(tpe: Int) = getIcon(false, tpe)

    override
    def getOpenedIcon(tpe: Int) = getIcon(true, tpe)

    private def getIcon(opened: Boolean, tpe: Int) = ImageUtilities.mergeImages(Icons.getFolderIcon(opened), getBadge, 7, 7)
    private def getBadge: Image = ICON_LIB_BADGE

    override
    def getActions(context: Boolean): Array[Action] = Array[Action]()
  }
  
  private class DepProjectsChildren(project: Project) extends Children.Keys[Project] with PropertyChangeListener {

    private val changeSupport = new ChangeSupport(this)

    setKeys()

    override
    protected def createNodes(key: Project): Array[Node] = {
      DataObject.find(key.getProjectDirectory) match {
        case null => 
          Array()
        case depProjectFolder =>
          Array(new FilterNode(depProjectFolder.getNodeDelegate) {
              override
              def getIcon(tpe: Int) = SBTProject.SBT_ICON
          
              override
              def getOpenedIcon(tpe: Int) = SBTProject.SBT_ICON
            }
          )
      }
    }

    private def setKeys() {
      val provider = project.getLookup.lookup(classOf[SBTDepProjectProvider])
      setKeys(provider.getSubprojects)
    }

    def addChangeListener(l: ChangeListener) {
      changeSupport.addChangeListener(l)
    }

    def removeChangeListener(l: ChangeListener) {
      changeSupport.removeChangeListener(l)
    }

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
