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

class SBTNodeFactory extends NodeFactory {
  import SBTNodeFactory._
  
  override
  def createNodes(project: Project): NodeList[_] = {
    new SourcesNodeList(project)
  }
}

object SBTNodeFactory {
  private val RP = new RequestProcessor(classOf[SBTNodeFactory])
  
  private class SourcesNodeList(project: Project) extends NodeList[SourceGroup] with ChangeListener {
    private val changeSupport = new ChangeSupport(this)
      
    override
    def keys: java.util.List[SourceGroup] = {
      if (project.getProjectDirectory == null || !project.getProjectDirectory.isValid) {
        return java.util.Collections.emptyList()
      }

      val theKeys = new java.util.ArrayList[SourceGroup]()
      
      val sources = ProjectUtils.getSources(project)
      val javasgs = sources.getSourceGroups(ProjectConstants.SOURCES_TYPE_JAVA)
      for (sg <- javasgs) theKeys.add(sg)
      val scalasgs = sources.getSourceGroups(ProjectConstants.SOURCES_TYPE_SCALA)
      for (sg <- scalasgs) theKeys.add(sg)
      val jarsgs = sources.getSourceGroups(ProjectConstants.ARTIFACT_TYPE_JAR)
      for (sg <- jarsgs) theKeys.add(sg)
      
      theKeys
    }
        
    override
    def node(group: SourceGroup): Node = {
      PackageView.createPackageView(group)
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