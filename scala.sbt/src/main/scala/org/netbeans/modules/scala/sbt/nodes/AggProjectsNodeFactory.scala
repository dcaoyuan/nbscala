package org.netbeans.modules.scala.sbt.nodes

import java.awt.Image
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import java.io.IOException
import javax.swing.Action
import javax.swing.SwingUtilities
import javax.swing.event.ChangeListener
import org.netbeans.api.project.Project
import org.netbeans.api.project.ProjectManager
import org.netbeans.modules.scala.sbt.project.ProjectConstants
import org.netbeans.modules.scala.sbt.project.SBTProject
import org.netbeans.modules.scala.sbt.project.SBTResolver
import org.netbeans.spi.project.ui.support.NodeFactory
import org.netbeans.spi.project.ui.support.NodeList
import org.openide.filesystems.FileUtil
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

class AggProjectsNodeFactory extends NodeFactory {
  def createNodes(project: Project): NodeList[_] = new AggProjectsNodeFactory.ProjectsNodeList(project)
}

object AggProjectsNodeFactory {
  private val AGG_PROJECTS = "agg-projects"
  private val ICON_LIB_BADGE = ImageUtilities.loadImage("org/netbeans/modules/java/j2seproject/ui/resources/libraries-badge.png")    //NOI18N
    
  private class ProjectsNodeList(project: Project) extends NodeList[String] {
    private val changeSupport = new ChangeSupport(this)

    private lazy val sbtResolver = {
      val x = project.getLookup.lookup(classOf[SBTResolver])
      
      x.addPropertyChangeListener(new PropertyChangeListener() {
          def propertyChange(evt: PropertyChangeEvent) {
            evt.getPropertyName match {
              case SBTResolver.DESCRIPTOR_CHANGE => 
                // The caller holds ProjectManager.mutex() read lock
                SwingUtilities.invokeLater(new Runnable() {
                    def run() {
                      changeSupport.fireChange
                    }
                  })
                
              case _ =>
            }
          }
        }
      )
      
      x
    }

    def keys: java.util.List[String] = {
      val theKeys = new java.util.ArrayList[String]()
      theKeys.add(AGG_PROJECTS)
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
      val aggProjects = new java.util.HashSet[SBTProject]()
      try {
        val projectFos = sbtResolver.getAggregateProjects map FileUtil.toFileObject
        for (projectFo <- projectFos) {
          ProjectManager.getDefault.findProject(projectFo) match {
            case x: SBTProject => aggProjects.add(x)
            case _ =>
          }
        }
      } catch {
        case ex: IOException => Exceptions.printStackTrace(ex)
        case ex: IllegalArgumentException => Exceptions.printStackTrace(ex)
      }
    
      if (aggProjects.size == 0) {
        null 
      } else {
        try {
          new ProjectNode(java.util.Collections.unmodifiableSet(aggProjects))
        } catch {
          case ex: DataObjectNotFoundException => Exceptions.printStackTrace(ex); null
        }
      }
    }

    def addNotify() {}

    def removeNotify() {}
  }
  
  private class ProjectNode(projects: java.util.Set[_ <: Project]) extends AbstractNode(new ProjectsChildren(projects)) {
    private val DISPLAY_NAME = NbBundle.getMessage(classOf[AggProjectsNodeFactory], "CTL_AggProjectsNode")

    override
    def getDisplayName: String = DISPLAY_NAME

    override
    def getName: String = ProjectConstants.NAME_DEP_PROJECTS

    override
    def getIcon(tpe: Int) = getIcon(false, tpe)

    override
    def getOpenedIcon(tpe: Int) = getIcon(true, tpe)

    private def getIcon(opened: Boolean, tpe: Int) = ImageUtilities.mergeImages(Icons.getFolderIcon(opened), getBadge, 7, 7)
    private def getBadge: Image = ICON_LIB_BADGE

    override
    def getActions(context: Boolean): Array[Action] = Array[Action]()
  }
  
  private class ProjectsChildren(depProjects: java.util.Set[_ <: Project]) extends Children.Keys[Project] with PropertyChangeListener {
    private val changeSupport = new ChangeSupport(this)

    setKeys(depProjects)

    override
    protected def createNodes(key: Project): Array[Node] = {
      DataObject.find(key.getProjectDirectory) match {
        case null => 
          Array()
        case depProjectFolder =>
          Array(new FilterNode(depProjectFolder.getNodeDelegate) {
              override def getIcon(tpe: Int) = SBTProject.SBT_ICON
              override def getOpenedIcon(tpe: Int) = SBTProject.SBT_ICON
            }
          )
      }
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
