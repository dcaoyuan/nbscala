package org.netbeans.modules.scala.sbt.nodes

import java.awt.Image
import java.awt.event.ActionEvent
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import javax.swing.AbstractAction
import javax.swing.Action
import javax.swing.SwingUtilities
import javax.swing.event.ChangeListener
import org.netbeans.api.java.classpath.ClassPath
import org.netbeans.api.project.Project
import org.netbeans.spi.project.ui.support.NodeFactory
import org.netbeans.spi.project.ui.support.NodeList
import org.openide.nodes.Children
import org.openide.nodes.Node
import org.openide.util.ChangeSupport
import org.openide.util.ImageUtilities
import org.openide.util.NbBundle

class LibrariesNodeFactory extends NodeFactory {
  import LibrariesNodeFactory._
  
  def createNodes(project: Project): NodeList[_] = {
    new LibrariesNodeList(project)
  }
}

object LibrariesNodeFactory {
  private val LIBRARIES = "Libs" //NOI18N
  private val TEST_LIBRARIES = "TestLibs" //NOI18N
  
  private val ICON_LIB_BADGE = ImageUtilities.loadImage("org/netbeans/modules/java/j2seproject/ui/resources/libraries-badge.png")    //NOI18N

    
  private class LibrariesNodeList(project: Project) extends NodeList[String] with PropertyChangeListener {

    private val changeSupport = new ChangeSupport(this)

    def keys: java.util.List[String] = {
      val result = new java.util.ArrayList[String]()
      result.add(LIBRARIES)
      val addTestSources = false // @TODO
      if (addTestSources) {
        result.add(TEST_LIBRARIES)
      }
      result
    }

    def addChangeListener(l: ChangeListener) {
      changeSupport.addChangeListener(l)
    }

    def removeChangeListener(l: ChangeListener) {
      changeSupport.removeChangeListener(l)
    }

    def node(key: String): Node = {
      key match {
        case LIBRARIES =>
          //Libraries Node
          new LibrariesNode(project)
        case TEST_LIBRARIES =>
          new LibrariesNode(project)
        case _ => 
          assert(false, "No node for key: " + key)
          null
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
  
  
  class LibrariesNode(project: Project) extends AbstractFolderNode(new LibrariesChildren(project)) {

    private val NODE_NAME = NbBundle.getMessage(classOf[LibrariesNodeFactory], "CTL_LibrariesNode")
    //static final RequestProcessor rp = new RequestProcessor();

    override
    def getDisplayName: String = NODE_NAME

    override
    def getName: String = NODE_NAME

    override
    protected def getBadge: Image = ICON_LIB_BADGE

    override
    def canCopy = false

    override
    def getActions(context: Boolean): Array[Action] = {
      Array(new ForceResolveAction())
    }

    private class ForceResolveAction extends AbstractAction {

      def ForceResolveAction() {
        putValue(Action.NAME, NbBundle.getMessage(classOf[LibrariesNodeFactory], "BTN_Force_Resolve"))
      }

      def actionPerformed(event: ActionEvent) {
        //SBTResourceController sbtController = project.getLookup().lookup(classOf[SBTResourceController])
        //sbtController.triggerResolution
      }
    }
  }
  
  private class LibrariesChildren(project: Project) extends Children.Keys[String]  {

    setKeys()

    override
    protected def createNodes(key: String): Array[Node] = {
      Array(new ScopeNode(project, key))
    }

    private def setKeys() {
      setKeys(Array(ClassPath.COMPILE, ClassPath.EXECUTE))
    }
  }

}