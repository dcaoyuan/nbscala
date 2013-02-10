package org.netbeans.modules.scala.sbt.nodes

import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener
import org.netbeans.api.project.Project
import org.netbeans.api.project.ProjectUtils
import org.netbeans.api.project.SourceGroup
import org.netbeans.modules.scala.sbt.project.ProjectConstants
import org.netbeans.spi.java.project.support.ui.PackageView
import org.netbeans.spi.project.ui.support.NodeFactory
import org.netbeans.spi.project.ui.support.NodeList
import org.openide.nodes.Node
import org.openide.util.ChangeSupport
import org.openide.util.RequestProcessor

class SourcesNodeFactory extends NodeFactory {
  import SourcesNodeFactory._
  
  override
  def createNodes(project: Project): NodeList[_] = new SourcesNodeList(project)
}

object SourcesNodeFactory {
  private val RP = new RequestProcessor(classOf[SourcesNodeFactory])
  
  private class SourcesNodeList(project: Project) extends NodeList[SourceGroup] with ChangeListener {
    private val changeSupport = new ChangeSupport(this)
      
    override
    def keys: java.util.List[SourceGroup] = {
      val theKeys = new java.util.ArrayList[SourceGroup]()
      
      val sources = ProjectUtils.getSources(project)
      val javasgs = sources.getSourceGroups(ProjectConstants.SOURCES_TYPE_JAVA)
      javasgs foreach theKeys.add
      val scalasgs = sources.getSourceGroups(ProjectConstants.SOURCES_TYPE_SCALA)
      scalasgs foreach theKeys.add
      
      java.util.Collections.sort(theKeys, new java.util.Comparator[SourceGroup]() {
          def compare(o1: SourceGroup, o2: SourceGroup) = o1.getName.compareTo(o2.getName)
        })
      
      theKeys
    }
        
    override
    def node(key: SourceGroup): Node = {
      PackageView.createPackageView(key)
    }
        
    override
    def addNotify() {
      val srcs = ProjectUtils.getSources(project)
      srcs.addChangeListener(this)
    }
        
    override
    def removeNotify() {
      val srcs = ProjectUtils.getSources(project)
      srcs.removeChangeListener(this)
    }

    override
    def addChangeListener(l: ChangeListener) {
      changeSupport.addChangeListener(l)
    }

    override
    def removeChangeListener(l: ChangeListener) {
      changeSupport.removeChangeListener(l)
    }

    override
    def stateChanged(arg0: ChangeEvent) {
      //#167372 break the stack trace chain to prevent deadlocks.
      RP.post(new Runnable() {
          def run {
            changeSupport.fireChange
          }
        }
      )
    }
  }
}