package org.netbeans.modules.scala.sbt.nodes


import java.awt.Image
import org.openide.nodes.AbstractNode
import org.openide.nodes.Children
import org.openide.util.ImageUtilities

/**
 * 
 * @author Caoyuan Deng
 */
abstract class AbstractFolderNode(children: Children) extends AbstractNode(children) {

  override
  def getIcon(tpe: Int) = getIcon(false, tpe)

  override
  def getOpenedIcon(tpe: Int) = getIcon(true, tpe)

  private def getIcon(opened: Boolean, tpe: Int) = ImageUtilities.mergeImages(Icons.getFolderIcon(opened), getBadge, 7, 7)

  protected def getBadge: Image
}
