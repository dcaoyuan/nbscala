package org.netbeans.modules.scala.sbt.nodes

import org.netbeans.api.project.Project
import org.netbeans.modules.scala.sbt.project.SBTResolver
import org.openide.filesystems.FileUtil
import org.openide.nodes.AbstractNode
import org.openide.nodes.ChildFactory
import org.openide.nodes.Children
import org.openide.nodes.Node
import org.openide.util.ImageUtilities
import org.openide.util.NbBundle

class ScopeNode(project: Project, scope: String) extends AbstractNode(Children.create(new ScopesChildFactory(project, scope), true)) {

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

private class ScopesChildFactory(project: Project, scope: String) extends ChildFactory.Detachable[ArtifactInfo] {
  private lazy val sbtResolver = project.getLookup.lookup(classOf[SBTResolver])

  override 
  protected def createKeys(toPopulate: java.util.List[ArtifactInfo]): Boolean = {
    val artifacts = sbtResolver.getResolvedLibraries(scope) map FileUtil.toFileObject filter {fo => 
      fo != null && FileUtil.isArchiveFile(fo)
    } map {fo =>
      ArtifactInfo(fo.getNameExt, "", "", FileUtil.toFile(fo), null, null)
    }

    toPopulate.addAll(java.util.Arrays.asList(artifacts.sortBy(_.name): _*))
    true
  }
  
  override
  protected def createNodeForKey(key: ArtifactInfo): Node = new ArtifactNode(key, project)
}