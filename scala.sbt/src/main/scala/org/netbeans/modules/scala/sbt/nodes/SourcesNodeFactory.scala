package org.netbeans.modules.scala.sbt.nodes

import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import javax.swing.SwingUtilities
import javax.swing.event.ChangeListener
import org.netbeans.api.project.Project
import org.netbeans.api.project.ProjectUtils
import org.netbeans.api.project.SourceGroup
import org.netbeans.modules.scala.sbt.project.ProjectConstants
import org.netbeans.modules.scala.sbt.project.SBTResolver
import org.netbeans.spi.java.project.support.ui.PackageView
import org.netbeans.spi.project.ui.support.NodeFactory
import org.netbeans.spi.project.ui.support.NodeList
import org.openide.nodes.Node
import org.openide.util.ChangeSupport

class SourcesNodeFactory extends NodeFactory {
  def createNodes(project: Project): NodeList[_] = new SourcesNodeFactory.SourcesNodeList(project)
}

object SourcesNodeFactory {
  private class SourcesNodeList(project: Project) extends NodeList[SourceGroup] with PropertyChangeListener {
    private val cs = new ChangeSupport(this)
    private lazy val sbtResolver = {
      val x = project.getLookup.lookup(classOf[SBTResolver])
      x.addPropertyChangeListener(this)
      x
    }
      
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
        
    def addNotify() {
      // addNotify will be called only when if node(key) returns non-null and node is 
      // thus we won't sbtResolver.addPropertyChangeListener(this) here
    }

    def removeNotify() {
      sbtResolver.removePropertyChangeListener(this)
    }

    override
    def addChangeListener(l: ChangeListener) {
      cs.addChangeListener(l)
    }

    override
    def removeChangeListener(l: ChangeListener) {
      cs.removeChangeListener(l)
    }

    def propertyChange(evt: PropertyChangeEvent) {
      evt.getPropertyName match {
        case SBTResolver.DESCRIPTOR_CHANGE => 
          // The caller holds ProjectManager.mutex() read lock
          SwingUtilities.invokeLater(new Runnable() {
              def run() {
                keys
                cs.fireChange
              }
            })
        case _ =>
      }
    }
  }
}