package org.netbeans.modules.scala.sbt.nodes

import java.awt.Image
import java.beans.BeanInfo
import javax.swing.UIManager
import org.openide.filesystems.FileUtil
import org.openide.loaders.DataFolder
import org.openide.util.ImageUtilities

object Icons {
    
  private val ICON_KEY_UIMANAGER = "Tree.closedIcon" // NOI18N
  private val OPENED_ICON_KEY_UIMANAGER = "Tree.openIcon" // NOI18N
  private val ICON_KEY_UIMANAGER_NB = "Nb.Explorer.Folder.icon" // NOI18N
  private val OPENED_ICON_KEY_UIMANAGER_NB = "Nb.Explorer.Folder.openedIcon" // NOI18N
  
  lazy val ICON_LIBARARIES_BADGE = ImageUtilities.loadImage("org/netbeans/modules/java/j2seproject/ui/resources/libraries-badge.png")   //NOI18N
    
  private lazy val folderIconCache = getTreeFolderIcon(false)
  private lazy val openedFolderIconCache = getTreeFolderIcon(true)

  /**
   * Returns default folder icon as {@link java.awt.Image}. Never returns
   * <code>null</code>.
   *
   * @param opened wheter closed or opened icon should be returned.
   */
  def getTreeFolderIcon(opened: Boolean): Image = {
    UIManager.getIcon(if (opened) OPENED_ICON_KEY_UIMANAGER else ICON_KEY_UIMANAGER) match {// #70263
      case null =>
        UIManager.get(if (opened) OPENED_ICON_KEY_UIMANAGER_NB else ICON_KEY_UIMANAGER_NB) match {// #70263
          case null => // fallback to our owns                
            val n = DataFolder.findFolder(FileUtil.getConfigRoot).getNodeDelegate
            if (opened) n.getOpenedIcon(BeanInfo.ICON_COLOR_16x16) else n.getIcon(BeanInfo.ICON_COLOR_16x16)
          case img: Image => img
        }
      case baseIcon => ImageUtilities.icon2Image(baseIcon)
    }
  }
  
  
  /**
   * Returns Icon of folder on active platform
   * @param opened should the icon represent opened folder
   * @return the folder icon
   */
  def getFolderIcon(opened: Boolean): Image = synchronized {
    if (opened) {
      openedFolderIconCache
    } else {
      folderIconCache
    }
  }
}