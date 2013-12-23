package org.netbeans.modules.scala.sbt.project

import javax.swing.Action
import org.netbeans.spi.project.ActionProvider
import org.netbeans.spi.project.ui.support.FileSensitiveActions
import org.openide.util.NbBundle

/**
 * Supplies project-specific file actions (e.g. compile/run) for *.scala files.
 * @see layer.xml
 *
 * @author Caoyuan Deng
 */
object SBTProjectModule {

  def run(): Action = FileSensitiveActions.fileCommandAction(
    ActionProvider.COMMAND_RUN_SINGLE,
    NbBundle.getMessage(this.getClass, "LBL_RunFile_Action"), // NOI18N
    null)

  def debug(): Action = FileSensitiveActions.fileCommandAction(
    ActionProvider.COMMAND_DEBUG_SINGLE,
    NbBundle.getMessage(this.getClass, "LBL_DebugFile_Action"), // NOI18N
    null)

  def debugTest(): Action = FileSensitiveActions.fileCommandAction(
    ActionProvider.COMMAND_DEBUG_TEST_SINGLE,
    NbBundle.getMessage(this.getClass, "LBL_DebugTestFile_Action"), // NOI18N
    null)
}
