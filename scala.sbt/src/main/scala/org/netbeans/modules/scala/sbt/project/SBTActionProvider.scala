package org.netbeans.modules.scala.sbt.project

import java.awt.event.MouseEvent
import javax.swing.JButton
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener
import org.netbeans.modules.scala.core.ProjectResources
import org.netbeans.modules.scala.core.ScalaSourceUtil
import org.netbeans.modules.scala.project.ui.customizer.MainClassWarning
import org.netbeans.modules.scala.sbt.console.SBTConsoleTopComponent
import org.apache.tools.ant.module.api.support.ActionUtils
import org.netbeans.api.language.util.ast.AstDfn
import org.netbeans.modules.scala.console.shell.ScalaConsoleTopComponent
import org.netbeans.spi.project.ActionProvider
import org.openide.DialogDescriptor
import org.openide.DialogDisplayer
import org.openide.NotifyDescriptor
import org.openide.awt.MouseUtils
import org.openide.filesystems.FileObject
import org.openide.filesystems.FileUtil
import org.openide.util.Lookup
import org.openide.util.NbBundle

/**
 *
 * Used for predefined project actions, @see org.netbeans.spi.project.ActionProvider
 *
 * @author Caoyuan Deng
 */
class SBTActionProvider(project: SBTProject) extends ActionProvider {
  import SBTActionProvider._

  private lazy val sbtResolver = project.getLookup.lookup(classOf[SBTResolver])

  /**
   * also @see ProjectSensitiveActions.projectCommandAction(SBTActionProvider.COMMAND_SBT_CONSOLE, "Open sbt", null) in
   * SBTProjectLogicalView.getActions
   */
  def getSupportedActions() = Array(
    COMMAND_SBT_CONSOLE,
    COMMAND_SCALA_CONSOLE,
    COMMAND_SBT_RELOAD,
    COMMAND_BUILD,
    COMMAND_REBUILD,
    COMMAND_CLEAN,
    COMMAND_RUN_SINGLE,
    COMMAND_DEBUG_SINGLE,
    COMMAND_DEBUG_TEST_SINGLE)

  @throws(classOf[IllegalArgumentException])
  def isActionEnabled(command: String, context: Lookup): Boolean = {
    true
  }

  def invokeAction(command: String, context: Lookup) {
    val rootProject = project.getRootProject
    command match {
      case COMMAND_SBT_CONSOLE =>
        val commands = project.getId match {
          case null => Nil
          case id => List("project " + id)
        }
        SBTConsoleTopComponent.openNewInstance(rootProject, false, commands)()

      case COMMAND_SCALA_CONSOLE =>
        ScalaConsoleTopComponent.openInstance(project, false, Nil)()

      case COMMAND_SBT_RELOAD =>
        val sbtResolver = project.getLookup.lookup(classOf[SBTResolver])
        sbtResolver.isResolvedOrResolving = false
        sbtResolver.triggerSbtResolution

      case COMMAND_BUILD =>
        val commands = project.getId match {
          case null => List("compile")
          case id => List("project " + id, "compile")
        }
        SBTConsoleTopComponent.openInstance(rootProject, false, commands, null)()

      case COMMAND_REBUILD =>
        val commands = project.getId match {
          case null => List("clean", "compile")
          case id => List("project " + id, "clean", "compile")
        }
        SBTConsoleTopComponent.openInstance(rootProject, false, commands, null)()

      case COMMAND_CLEAN =>
        val commands = project.getId match {
          case null => List("clean")
          case id => List("project " + id, "clean")
        }
        SBTConsoleTopComponent.openInstance(rootProject, false, commands, null)()

      case COMMAND_RUN_SINGLE | COMMAND_DEBUG_SINGLE =>
        val file = findSources(context, false)(0)
        val mainClasses = getMainClasses(file)
        val clazz = if (mainClasses.size == 1) {
          val next = mainClasses.iterator.next()
          // Just one main class, resolve from the symbol
          next.qualifiedName
        } else if (mainClasses.size > 1) {
          // Several main classes, let the user choose
          showMainClassWarning(file, mainClasses)
        } else {
          null
        }
        if (clazz != null) {
          val commands = project.getId match {
            case null => List("run-main " + clazz)
            case id => List("project " + id, "run-main " + clazz)
          }
          // TODO debug file
          SBTConsoleTopComponent.openInstance(rootProject, false, commands, null)()
        }

      case _ =>
    }
  }

  /**
   * Find selected sources, the sources has to be under single source root,
   *  @param context the lookup in which files should be found
   */
  private def findSources(context: Lookup, isTest: Boolean): Array[FileObject] = {
    val srcs = sbtResolver.getSources(ProjectResources.SOURCES_TYPE_JAVA, isTest) ++ sbtResolver.getSources(ProjectResources.SOURCES_TYPE_SCALA, isTest)

    var files: Array[FileObject] = null
    srcs find { srcPath_xy =>
      files = ActionUtils.findSelectedFiles(context, FileUtil.toFileObject(srcPath_xy._1), ".scala", true)
      files != null
    }
    files
  }

  private def getMainClasses(fo: FileObject) = {
    // TODO support for unit testing
    if (fo == null /* || MainClassChooser.unitTestingSupport_hasMainMethodResult != null */ ) {
      java.util.Collections.emptySet[AstDfn]
    }
    ScalaSourceUtil.getMainClassesAsJavaCollection(fo)
  }

  private def showMainClassWarning(file: FileObject, mainClasses: java.util.Collection[AstDfn]): String = {
    val okButton = new JButton(NbBundle.getMessage(classOf[MainClassWarning], "LBL_MainClassWarning_ChooseMainClass_OK")) // NOI18N
    okButton.getAccessibleContext.setAccessibleDescription(NbBundle.getMessage(classOf[MainClassWarning], "AD_MainClassWarning_ChooseMainClass_OK"))

    val panel = new MainClassWarning(NbBundle.getMessage(classOf[MainClassWarning], "CTL_FileMultipleMain", file.getNameExt), mainClasses)
    val options = Array(okButton, NotifyDescriptor.CANCEL_OPTION)

    panel.addChangeListener(new ChangeListener() {
      def stateChanged(e: ChangeEvent) {
        e.getSource match {
          case x: MouseEvent if MouseUtils.isDoubleClick(x) =>
            // click button and the finish dialog with selected class
            okButton.doClick()
          case _ =>
            okButton.setEnabled(panel.getSelectedMainClass() != null);
        }
      }
    })
    val desc = new DialogDescriptor(
      panel, NbBundle.getMessage(classOf[MainClassWarning], "CTL_FileMainClass_Title"), // NOI18N
      true, options, options(0), DialogDescriptor.BOTTOM_ALIGN, null, null)
    desc.setMessageType(NotifyDescriptor.INFORMATION_MESSAGE)
    val dlg = DialogDisplayer.getDefault.createDialog(desc)
    dlg.setVisible(true)
    val mainClass = if (desc.getValue == options(0)) {
      panel.getSelectedMainClass()
    } else null
    dlg.dispose()
    mainClass
  }

}

object SBTActionProvider {
  val COMMAND_SBT_CONSOLE = "sbt.console"
  val COMMAND_SCALA_CONSOLE = "scala.console"
  val COMMAND_SBT_RELOAD = "sbt.reload"

  val COMMAND_BUILD = ActionProvider.COMMAND_BUILD // compile
  val COMMAND_REBUILD = ActionProvider.COMMAND_REBUILD // clean and compile
  val COMMAND_CLEAN = ActionProvider.COMMAND_CLEAN // clean

  val COMMAND_RUN_SINGLE = ActionProvider.COMMAND_RUN_SINGLE
  val COMMAND_DEBUG_SINGLE = ActionProvider.COMMAND_DEBUG_SINGLE
  val COMMAND_DEBUG_TEST_SINGLE = ActionProvider.COMMAND_DEBUG_TEST_SINGLE
}
