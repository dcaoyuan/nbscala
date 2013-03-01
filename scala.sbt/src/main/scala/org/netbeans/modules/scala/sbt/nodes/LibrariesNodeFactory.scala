package org.netbeans.modules.scala.sbt.nodes

import java.awt.Image
import javax.swing.Action
import javax.swing.event.ChangeListener
import org.netbeans.api.java.classpath.ClassPath
import org.netbeans.api.project.Project
import org.netbeans.modules.scala.sbt.project.ProjectConstants
import org.netbeans.spi.project.ui.support.NodeFactory
import org.netbeans.spi.project.ui.support.NodeList
import org.openide.nodes.AbstractNode
import org.openide.nodes.Children
import org.openide.nodes.Node
import org.openide.util.ChangeSupport
import org.openide.util.ImageUtilities
import org.openide.util.NbBundle

class LibrariesNodeFactory extends NodeFactory {
  def createNodes(project: Project): NodeList[_] = new LibrariesNodeFactory.LibrariesNodeList(project)
}

object LibrariesNodeFactory {
  private val LIBRARIES = "Libs" //NOI18N
  private val TEST_LIBRARIES = "TestLibs" //NOI18N
  
  private val ICON_LIB_BADGE = ImageUtilities.loadImage("org/netbeans/modules/java/j2seproject/ui/resources/libraries-badge.png")    //NOI18N
    
  private class LibrariesNodeList(project: Project) extends NodeList[String] {
    private val cs = new ChangeSupport(this)

    def keys: java.util.List[String] = {
      val theKeys = new java.util.ArrayList[String]()
      theKeys.add(LIBRARIES)
      val addTestSources = true // @TODO
      if (addTestSources) {
        theKeys.add(TEST_LIBRARIES)
      }
      theKeys
    }

    def node(key: String): Node = {
      key match {
        case LIBRARIES => new LibrariesNode(project, isTest = false)
        case TEST_LIBRARIES => new LibrariesNode(project, isTest = true)
        case _ => assert(false, "No node for key: " + key); null
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
  
  class LibrariesNode(project: Project, isTest: Boolean) extends AbstractNode(new LibrariesChildren(project, isTest)) {
    private val DISPLAY_NAME = NbBundle.getMessage(classOf[LibrariesNodeFactory], "CTL_LibrariesNode" + (if (isTest) "_Test" else ""))

    override
    def getDisplayName: String = DISPLAY_NAME

    override
    def getName: String = ProjectConstants.NAME_DEP_LIBRARIES

    override
    def getIcon(tpe: Int) = getIcon(false, tpe)

    override
    def getOpenedIcon(tpe: Int) = getIcon(true, tpe)

    private def getIcon(opened: Boolean, tpe: Int) = ImageUtilities.mergeImages(Icons.getFolderIcon(opened), getBadge, 7, 7)
    private def getBadge: Image = ICON_LIB_BADGE

    override
    def canCopy = false

    override
    def getActions(context: Boolean): Array[Action] = Array()
  }
  
  private class LibrariesChildren(project: Project, isTest: Boolean) extends Children.Keys[String] {

    setKeys()

    override
    protected def createNodes(key: String): Array[Node] = {
      Array(new ScopeNode(project, key, isTest))
    }

    private def setKeys() {
      setKeys(Array(ClassPath.COMPILE/* , ClassPath.EXECUTE */))
    }
  }

}