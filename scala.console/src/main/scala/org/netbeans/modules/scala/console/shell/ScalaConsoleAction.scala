package org.netbeans.modules.scala.console.shell

import java.awt.event.ActionEvent
import javax.swing.AbstractAction
import javax.swing.Action
import org.netbeans.api.project.Project
import org.openide.awt.DynamicMenuContent
import org.openide.util.ContextAwareAction
import org.openide.util.Lookup
import org.openide.util.NbBundle

/**
 *
 * @author Caoyuan Deng
 */
class ScalaConsoleAction extends AbstractAction with ContextAwareAction {

  override def actionPerformed(e: ActionEvent) {
    assert(false, "")
  }

  override def createContextAwareInstance(context: Lookup): Action = new ContextAction(context)

  private final class ContextAction(context: Lookup) extends AbstractAction {
    val project = context.lookup(classOf[Project])

    setEnabled(true)
    //    if (ProjectResources.getScalaJavaSourceGroups(project).length > 0) {
    //      setEnabled(true) // sbt and maven root project may has empty source group
    //    } else {
    //      setEnabled(false)
    //    }

    putValue(DynamicMenuContent.HIDE_WHEN_DISABLED, true)
    putValue(Action.NAME, NbBundle.getMessage(classOf[ScalaConsoleAction], "CTL_ScalaConsoleAction"))

    override def actionPerformed(e: ActionEvent) {
      ScalaConsoleTopComponent.openInstance(project, Nil)()
    }
  }
}