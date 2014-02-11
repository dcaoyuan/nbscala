package org.netbeans.modules.scala.sbt.nodes

import java.awt.Image
import javax.swing.event.ChangeListener
import org.netbeans.api.project.Project
import org.netbeans.spi.project.ui.support.NodeFactory
import org.netbeans.spi.project.ui.support.NodeList
import org.netbeans.modules.scala.sbt.project.SBTProject
import org.openide.filesystems.FileObject
import org.openide.loaders.DataNode
import org.openide.loaders.DataObject
import org.openide.nodes.Children
import org.openide.nodes.Node
import org.openide.util.ChangeSupport

import scala.collection.JavaConversions._

class SbtFilesNodeFactory extends NodeFactory {
  def createNodes(project: Project): NodeList[_] = new SbtFilesNodeFactory.SbtFilesNodeList(project)
}

class SbtFileNode(file: FileObject) extends DataNode(DataObject.find(file), Children.LEAF) {
  override def getIcon(tpe: Int): Image = SBTProject.SBT_ICON
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
