package org.netbeans.modules.scala.sbt.nodes

import java.awt.Image
import javax.swing.event.ChangeListener
import javax.swing.text.StyledDocument
import java.awt.event.ActionEvent
import javax.swing.AbstractAction
import javax.swing.Action
import org.netbeans.api.project.Project
import org.netbeans.api.project.ProjectUtils
import org.netbeans.api.project.SourceGroup
import org.netbeans.modules.scala.core.ProjectResources
import org.netbeans.modules.scala.sbt.project.SBTResolver
import org.netbeans.spi.java.project.support.ui.PackageView
import org.netbeans.spi.project.ui.support.NodeFactory
import org.netbeans.spi.project.ui.support.NodeList
import org.netbeans.modules.scala.sbt.project.SBTProject
import org.openide.filesystems.FileObject
import org.openide.loaders.DataObject
import org.openide.cookies.EditorCookie
import org.openide.nodes.AbstractNode
import org.openide.nodes.Children
import org.openide.nodes.Node
import org.openide.util.ChangeSupport
import org.openide.util.NbBundle

import scala.collection.JavaConversions._

class SbtFilesNodeFactory extends NodeFactory {
  def createNodes(project: Project): NodeList[_] = new SbtFilesNodeFactory.SbtFilesNodeList(project)
}

class SbtFileNode(file: FileObject) extends AbstractNode(Children.LEAF) {
  override def getIcon(tpe: Int): Image = SBTProject.SBT_ICON

  override def getDisplayName: String = file.getNameExt

  override def getShortDescription = file.getNameExt

  override def getActions(arg0: Boolean): Array[Action] = Array(OpenAction)

  object OpenAction extends AbstractAction {
    putValue(Action.NAME, NbBundle.getMessage(classOf[SbtFileNode], "BTN_Open_Sbt_File"))

    def actionPerformed(event: ActionEvent) {
      val d = DataObject.find(file)
      val ec = d.getCookie(classOf[EditorCookie]).asInstanceOf[EditorCookie];
      ec.open
      ec.openDocument
    }
  }
}

object SbtFilesNodeFactory {
  private class SbtFilesNodeList(project: Project) extends NodeList[FileObject] {
    private val cs = new ChangeSupport(this)

    override def keys: java.util.List[FileObject] =
      project.getProjectDirectory.getChildren.filter(_.hasExt("sbt")).toList

    override def node(key: FileObject): Node = new SbtFileNode(key)

    def addNotify() {
    }

    def removeNotify() {
    }

    override def addChangeListener(l: ChangeListener) {
      cs.addChangeListener(l)
    }

    override def removeChangeListener(l: ChangeListener) {
      cs.removeChangeListener(l)
    }
  }
}