package org.netbeans.modules.scala.sbt.project

import java.beans.PropertyChangeListener
import javax.swing.Icon
import javax.swing.ImageIcon
import org.netbeans.api.project.Project
import org.netbeans.api.project.ProjectInformation
import org.netbeans.modules.scala.sbt.classpath.SBTClassPathProvider
import org.netbeans.modules.scala.sbt.classpath.SBTController
import org.netbeans.modules.scala.sbt.classpath.SBTSources
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
  private lazy val lookup: Lookup = Lookups.fixed(
    this,
    new Info(),
    new SBTController(this, true),
    new SBTProjectLogicalView(this),
    new SBTClassPathProvider(this),
    new SBTSources(this),
    new SBTProjectOpenedHook(this),
    new SBTActionProvider(this),
    new SBTDepProjectProvider(this)
  )

  override
  def getProjectDirectory = projectDir

  override
  def getLookup: Lookup = lookup
  
  private final class Info extends ProjectInformation {

    override
    def getIcon: Icon = new ImageIcon(SBTProject.SBT_ICON)

    override
    def getName: String = {
      getProjectDirectory.getName
    }

    override
    def getDisplayName: String = {
      getName
    }

    override
    def addPropertyChangeListener(pcl: PropertyChangeListener) {
      //do nothing, won't change
    }

    override
    def removePropertyChangeListener(pcl: PropertyChangeListener) {
      //do nothing, won't change
    }

    override
    def getProject: Project = SBTProject.this
  }
  
}

object SBTProject {
  lazy val SBT_ICON = ImageUtilities.loadImage("org/netbeans/modules/scala/sbt/resources/sbt.png")
}