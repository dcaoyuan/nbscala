package org.netbeans.modules.scala.sbt.project

import java.beans.PropertyChangeListener
import javax.swing.Icon
import javax.swing.ImageIcon
import org.netbeans.api.project.Project
import org.netbeans.api.project.ProjectInformation
import org.netbeans.api.project.ProjectManager
import org.netbeans.modules.scala.sbt.classpath.SBTClassPathProvider
import org.netbeans.modules.scala.sbt.classpath.SBTSources
import org.netbeans.modules.scala.sbt.queries.SBTBinaryForSourceQuery
import org.netbeans.modules.scala.sbt.queries.SBTSourceForBinaryQuery
import org.netbeans.spi.project.ProjectState
import org.openide.filesystems.FileObject
import org.openide.util.ImageUtilities
import org.openide.util.Lookup
import org.openide.util.lookup.Lookups

/**
 *
 * @author Caoyuan Deng
 */
class SBTProject(projectDir: FileObject, state: ProjectState) extends Project {
  // TODO @see org.netbeans.api.project.ProjectUtil for more providers
  private lazy val lookup: Lookup = Lookups.fixed(
    this,
    new Info(),
    new SBTResolver(this),
    new SBTProjectLogicalView(this),
    new SBTClassPathProvider(this),
    new SBTSources(this),
    new SBTProjectOpenedHook(this),
    new SBTActionProvider(this),
    new SBTSourceForBinaryQuery(this),
    new SBTBinaryForSourceQuery(this),
    new ScalariformPrefs(this))

  override def getProjectDirectory = projectDir

  override def getLookup: Lookup = lookup

  private def findParentProject: Option[SBTProject] = findParentProject(projectDir)
  private def findParentProject(dir: FileObject): Option[SBTProject] = {
    if (dir.isRoot) {
      None
    } else {
      val parentDir = dir.getParent
      SBTProjectType.findSbtProjectFolder(parentDir) match {
        case Some(x) =>
          ProjectManager.getDefault.findProject(parentDir) match {
            case x: SBTProject => Some(x)
            case _             => findParentProject(parentDir)
          }
        case None => findParentProject(parentDir)
      }
    }
  }

  /**
   * Top parent project or myself
   */
  def getRootProject: SBTProject = getProjectChain.head

  def getProjectChain: List[SBTProject] = getProjectChain(this, List(this))

  private def getProjectChain(project: SBTProject, chain: List[SBTProject]): List[SBTProject] = {
    project.findParentProject match {
      case None    => chain
      case Some(x) => getProjectChain(x, x :: chain)
    }
  }

  def getName: String = {
    val resolvedName = getLookup.lookup(classOf[SBTResolver]) match {
      case null     => null
      case resolver => resolver.getName
    }
    if (resolvedName != null) {
      resolvedName
    } else {
      getLookup.lookup(classOf[ProjectInformation]).getName
    }
  }

  /**
   * May be null
   */
  def getId: String = {
    getLookup.lookup(classOf[SBTResolver]) match {
      case null     => null
      case resolver => resolver.getId
    }
  }

  def getDisplayName = {
    val name = getName
    val id = getId
    if (id != null && id != "" && id != name)
      name + " (" + id + ")"
    else
      name
  }

  private final class Info extends ProjectInformation {

    override def getIcon: Icon = new ImageIcon(SBTProject.SBT_ICON)

    override def getName: String = {
      getProjectDirectory.getName
    }

    override def getDisplayName: String = {
      getName
    }

    override def addPropertyChangeListener(pcl: PropertyChangeListener) {
      //do nothing, won't change
    }

    override def removePropertyChangeListener(pcl: PropertyChangeListener) {
      //do nothing, won't change
    }

    override def getProject: Project = SBTProject.this
  }

}

object SBTProject {
  lazy val SBT_ICON = ImageUtilities.loadImage("org/netbeans/modules/scala/sbt/resources/sbt.png")
}