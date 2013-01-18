package org.netbeans.modules.scala.sbt.console

import java.awt.event.ActionEvent
import javax.swing.AbstractAction
import javax.swing.ImageIcon
import org.openide.util.ImageUtilities
import org.openide.util.NbBundle

/**
 * Action which shows SBT Console component.
 */
class SBTConsoleAction extends AbstractAction(NbBundle.getMessage(classOf[SBTConsoleAction], "CTL_SBTConsoleAction")) {
    
  putValue(javax.swing.Action.SMALL_ICON, new ImageIcon(ImageUtilities.loadImage(SBTConsoleTopComponent.ICON_PATH, true)))
    
  def actionPerformed(evt: ActionEvent) {
    val win = SBTConsoleTopComponent.findInstance
    win.open
    win.requestActive
  }
    
}
