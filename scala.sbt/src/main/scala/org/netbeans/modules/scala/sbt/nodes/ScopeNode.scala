package org.netbeans.modules.scala.sbt.nodes

import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import javax.swing.SwingUtilities
import org.netbeans.api.project.Project
import org.netbeans.modules.scala.sbt.classpath.SBTResolver
import org.openide.filesystems.FileUtil
import org.openide.nodes.AbstractNode
import org.openide.nodes.Children
import org.openide.nodes.Node
import org.openide.util.ImageUtilities
import org.openide.util.NbBundle

class ScopeNode(project: Project, scope: String) extends AbstractNode(new ScopesChildren(project, scope)) {

  override
  def getDisplayName = NbBundle.getMessage(classOf[ScopeNode], "CTL_Scope_" + scope)

  override
  def getName = scope
  
  override
  def getIcon(tpe: Int) = getIcon(false, tpe)

  override
  def getOpenedIcon(tpe: Int) = getIcon(true, tpe)

  private def getIcon(opened: Boolean, tpe: Int) = ImageUtilities.mergeImages(Icons.getFolderIcon(opened), getBadge, 7, 7)
  private def getBadge = Icons.ICON_LIBARARIES_BADGE
}

private class ScopesChildren(project: Project, scope: String) extends Children.Keys[ArtifactInfo] with PropertyChangeListener {

  setKeys

  override
  protected def createNodes(key: ArtifactInfo): Array[Node] = {
    Array(new ArtifactNode(key, project))
  }

  def propertyChange(evt: PropertyChangeEvent) {
    evt.getPropertyName match {
      case SBTResolver.SBT_LIBRARY_RESOLVED => setKeys
      case _ =>
    }
  }

  private def setKeys {
    val sbtResolver = project.getLookup.lookup(classOf[SBTResolver])
    val artifacts = sbtResolver.getResolvedLibraries(scope) map FileUtil.toFileObject filter {fo => 
      fo != null && FileUtil.isArchiveFile(fo)
    } map {fo =>
      ArtifactInfo(fo.getNameExt, "", "", FileUtil.toFile(fo), null, null)
    }
    
    SwingUtilities.invokeLater(new Runnable() {
        def run() {
          setKeys(artifacts.sortBy(_.name))
        }
      })
  }
}