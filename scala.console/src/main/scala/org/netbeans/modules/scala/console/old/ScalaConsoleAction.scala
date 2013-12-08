package org.netbeans.modules.scala.console.old

import java.awt.event.ActionEvent
import javax.swing.AbstractAction
import javax.swing.ImageIcon
import org.openide.util.ImageUtilities
import org.openide.util.NbBundle

/**
 * Action which shows Scala Console component.
 */
class ScalaConsoleAction extends AbstractAction(NbBundle.getMessage(classOf[ScalaConsoleAction], "CTL_ScalaConsoleAction")) {

  putValue(javax.swing.Action.SMALL_ICON, new ImageIcon(ImageUtilities.loadImage(ScalaConsoleTopComponent.ICON_PATH, true)))

  def actionPerformed(evt: ActionEvent) {
    val win = ScalaConsoleTopComponent.findInstance
    win.open
    win.requestActive
  }

}
