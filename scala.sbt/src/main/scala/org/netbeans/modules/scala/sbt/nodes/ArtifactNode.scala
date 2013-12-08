package org.netbeans.modules.scala.sbt.nodes

import java.awt.Image
import java.beans.PropertyChangeListener
import java.io.File
import javax.swing.Action
import javax.swing.Icon
import org.netbeans.api.project.Project
import org.netbeans.api.project.SourceGroup
import org.netbeans.spi.java.project.support.ui.PackageView
import org.openide.filesystems.FileObject
import org.openide.filesystems.FileUtil
import org.openide.nodes.AbstractNode
import org.openide.nodes.FilterNode
import org.openide.nodes.Node
import org.openide.util.ImageUtilities
import org.openide.util.NbBundle

case class ArtifactInfo(name: String, version: String, organization: String, jarFile: File, sourceFile: File, docFile: File) {
  def hasJar = jarFile != null
  def hasSource = sourceFile != null
  def hasDoc = docFile != null
}

class ArtifactNode(artifactInfo: ArtifactInfo, project: Project) extends AbstractNode(
  new ArtifactNode.JarContentFilterChildren(PackageView.createPackageView(new ArtifactNode.ArtifactSourceGroup(artifactInfo)))) {
  import ArtifactNode._

  setIconBaseWithExtension(LIBRARIES_ICON)

  override def getDisplayName: String = {
    artifactInfo.name + " " + artifactInfo.version
  }

  override def getName: String = {
    artifactInfo.name + " " + artifactInfo.version
  }

  /**
   * Tooltip
   */
  override def getShortDescription = if (artifactInfo.hasJar) artifactInfo.jarFile.getAbsolutePath else getDisplayName

  override def getIcon(tpe: Int): Image = {
    var icon = super.getIcon(tpe)
    if (artifactInfo.hasDoc) {
      var badge = ImageUtilities.loadImage(JAVADOC_BADGE_ICON)
      badge = ImageUtilities.addToolTipToImage(badge, toolTipJavadoc)
      icon = ImageUtilities.mergeImages(icon, badge, 12, 0)
    }
    if (artifactInfo.hasSource) {
      var badge = ImageUtilities.loadImage(SOURCE_BADGE_ICON)
      badge = ImageUtilities.addToolTipToImage(badge, toolTipSource)
      icon = ImageUtilities.mergeImages(icon, badge, 12, 8)
    }
    if (!artifactInfo.hasJar) {
      var badge = ImageUtilities.loadImage(MISSING_JAR_BADGE_ICON)
      badge = ImageUtilities.addToolTipToImage(badge, toolTipMissing)
      icon = ImageUtilities.mergeImages(icon, badge, 0, 0)
    }

    icon
  }

  override def getOpenedIcon(tpe: Int): Image = {
    getIcon(tpe)
  }

  override def getActions(context: Boolean): Array[Action] = {
    Array[Action]()
  }
}

object ArtifactNode {
  private val LIBRARIES_ICON = "org/netbeans/modules/java/j2seproject/ui/resources/libraries.gif" //NOI18N
  private val SOURCE_BADGE_ICON = "org/netbeans/modules/scala/sbt/resources/DependencySrcIncluded.png" //NOI18N
  private val JAVADOC_BADGE_ICON = "org/netbeans/modules/scala/sbt/resources/DependencyJavadocIncluded.png" //NOI18N
  private val MISSING_JAR_BADGE_ICON = "org/netbeans/modules/scala/sbt/resources/ResourceNotIncluded.gif" //NOI18N

  private val toolTipJavadoc = "<img src=\"" + classOf[ArtifactNode].getClassLoader.getResource(JAVADOC_BADGE_ICON) + "\">&nbsp;" + NbBundle.getMessage(classOf[ArtifactNode], "ICON_JavadocBadge") //NOI18N
  private val toolTipSource = "<img src=\"" + classOf[ArtifactNode].getClassLoader.getResource(SOURCE_BADGE_ICON) + "\">&nbsp;" + NbBundle.getMessage(classOf[ArtifactNode], "ICON_SourceBadge") //NOI18N
  private val toolTipMissing = "<img src=\"" + classOf[ArtifactNode].getClassLoader.getResource(MISSING_JAR_BADGE_ICON) + "\">&nbsp;" + NbBundle.getMessage(classOf[ArtifactNode], "ICON_MissingBadge") //NOI18N

  private class ArtifactSourceGroup(artInfo: ArtifactInfo) extends SourceGroup {

    @throws(classOf[IllegalArgumentException])
    def contains(file: FileObject): Boolean = true

    def getDisplayName: String = artInfo.name

    def getIcon(opened: Boolean): Icon = null

    def getName: String = artInfo.name

    def getRootFolder: FileObject = {
      val file = if (artInfo.sourceFile != null) artInfo.sourceFile else artInfo.jarFile
      if (file != null) {
        val fo = FileUtil.toFileObject(FileUtil.normalizeFile(file))
        if (fo != null) {
          return FileUtil.getArchiveRoot(fo)
        }
      }
      return null
    }

    def addPropertyChangeListener(l: PropertyChangeListener) {}

    def removePropertyChangeListener(l: PropertyChangeListener) {}
  }

  private class JarContentFilterChildren(original: Node) extends FilterNode.Children(original) {
    override protected def copyNode(node: Node): Node = new JarFilterNode(node)
  }

  private class JarFilterNode(original: Node) extends FilterNode(original, new JarContentFilterChildren(original))
}
