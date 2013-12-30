package org.netbeans.modules.scala.sbt.nodes

import java.awt.Image
import java.io.IOException
import javax.swing.Action
import javax.swing.event.ChangeListener
import org.netbeans.api.project.Project
import org.netbeans.api.project.ProjectManager
import org.netbeans.modules.scala.sbt.project.ProjectConstants
import org.netbeans.modules.scala.sbt.project.SBTProject
import org.netbeans.modules.scala.sbt.project.SBTResolver
import org.netbeans.spi.project.ui.support.NodeFactory
import org.netbeans.spi.project.ui.support.NodeList
import org.openide.filesystems.FileUtil
import org.openide.loaders.DataObjectNotFoundException
import org.openide.nodes.AbstractNode
import org.openide.nodes.ChildFactory
import org.openide.nodes.Children
import org.openide.nodes.Node
import org.openide.util.ChangeSupport
import org.openide.util.Exceptions
import org.openide.util.ImageUtilities
import org.openide.util.NbBundle

class AggProjectsNodeFactory extends NodeFactory {
  def createNodes(project: Project): NodeList[_] = new AggProjectsNodeFactory.ProjectsNodeList(project.asInstanceOf[SBTProject])
}

object AggProjectsNodeFactory {
  private val AGG_PROJECTS = "agg-projects"
  private val ICON_LIB_BADGE = ImageUtilities.loadImage("org/netbeans/modules/java/j2seproject/ui/resources/libraries-badge.png") //NOI18N

  private class ProjectsNodeList(project: SBTProject) extends NodeList[String] {
    private val cs = new ChangeSupport(this)
    private lazy val sbtResolver = project.getLookup.lookup(classOf[SBTResolver])

    def keys: java.util.List[String] = {
      val theKeys = new java.util.ArrayList[String]()
      theKeys.add(AGG_PROJECTS)
      theKeys
    }

    /**
     * return null if node for this key doesn't exist currently
     */
    def node(key: String): Node = {
      if (sbtResolver.getAggregateProjects.length == 0) {
        null
      } else {
        try {
          new ProjectNode(project)
        } catch {
          case ex: DataObjectNotFoundException => Exceptions.printStackTrace(ex); null
        }
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

  private class ProjectNode(project: SBTProject) extends AbstractNode(Children.create(new ProjectsChildFactory(project), true)) {
    private val DISPLAY_NAME = NbBundle.getMessage(classOf[AggProjectsNodeFactory], "CTL_AggProjectsNode")

    override def getDisplayName: String = DISPLAY_NAME

    override def getName: String = ProjectConstants.NAME_DEP_PROJECTS

    override def getIcon(tpe: Int) = getIcon(false, tpe)

    override def getOpenedIcon(tpe: Int) = getIcon(true, tpe)

    private def getIcon(opened: Boolean, tpe: Int) = ImageUtilities.mergeImages(Icons.getFolderIcon(opened), getBadge, 7, 7)
    private def getBadge: Image = ICON_LIB_BADGE

    override def getActions(context: Boolean): Array[Action] = Array[Action]()
  }

  private class ProjectsChildFactory(parentProject: SBTProject) extends ChildFactory.Detachable[SBTProject] {
    private lazy val sbtResolver = parentProject.getLookup.lookup(classOf[SBTResolver])

    override protected def createKeys(toPopulate: java.util.List[SBTProject]): Boolean = {
      val toSort = new java.util.TreeMap[String, SBTProject]()
      try {
        val projectFos = sbtResolver.getAggregateProjects map FileUtil.toFileObject
        for (projectFo <- projectFos) {
          ProjectManager.getDefault.findProject(projectFo) match {
            case x: SBTProject => toSort.put(x.getName, x)
            case _             =>
          }
        }
      } catch {
        case ex: IOException              => Exceptions.printStackTrace(ex)
        case ex: IllegalArgumentException => Exceptions.printStackTrace(ex)
      }

      toPopulate.addAll(toSort.values)
      true
    }

    override protected def createNodeForKey(key: SBTProject): Node = new SubProjectNode(key)
  }

}
