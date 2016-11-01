package org.netbeans.modules.scala.sbt.project

import java.awt.event.MouseEvent
import java.util.logging.Level
import java.util.logging.Logger
import javax.swing.JButton
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener
import org.netbeans.modules.scala.core.ProjectResources
import org.netbeans.modules.scala.core.ScalaSourceUtil
import org.netbeans.modules.scala.sbt.console.SBTConsoleTopComponent
import org.apache.tools.ant.module.api.support.ActionUtils
import org.netbeans.api.debugger.jpda.DebuggerStartException
import org.netbeans.api.debugger.jpda.JPDADebugger
import org.netbeans.api.language.util.ast.AstDfn
import org.netbeans.api.project.ProjectManager
import org.netbeans.modules.scala.console.shell.ScalaConsoleTopComponent
import org.netbeans.spi.project.ActionProvider
import org.openide.DialogDescriptor
import org.openide.DialogDisplayer
import org.openide.LifecycleManager
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

  private val log = Logger.getLogger(this.getClass.getName)

  private lazy val sbtResolver = project.getLookup.lookup(classOf[SBTResolver])

  /**
   * also @see ProjectSensitiveActions.projectCommandAction(SBTActionProvider.COMMAND_SBT_CONSOLE, "Open sbt", null) in
   * SBTProjectLogicalView.getActions
   */
  def getSupportedActions() = Array(
    COMMAND_SBT_CONSOLE,
    COMMAND_SCALA_CONSOLE,
    COMMAND_SBT_RELOAD,
    COMMAND_SBT_UPDATE_CLASSIFIERS,
    COMMAND_BUILD,
    COMMAND_REBUILD,
    COMMAND_CLEAN,
    COMMAND_RUN,
    COMMAND_DEBUG,
    COMMAND_RUN_SINGLE,
    COMMAND_DEBUG_SINGLE,
    COMMAND_TEST,
    COMMAND_TEST_ONLY)

  @throws(classOf[IllegalArgumentException])
  def isActionEnabled(command: String, context: Lookup): Boolean = {
    true
  }

  def invokeAction(command: String, context: Lookup) {
    val rootProject = project.getRootProject

    command match {
      case COMMAND_SCALA_CONSOLE =>
        ScalaConsoleTopComponent.openInstance(project, Nil)()

      case COMMAND_SBT_CONSOLE =>
        val commands = selectProject
        SBTConsoleTopComponent.openNewInstance(rootProject, commands, isDebug = false)()

      case COMMAND_SBT_RELOAD =>
        val sbtResolver = project.getLookup.lookup(classOf[SBTResolver])
        sbtResolver.isResolvedOrResolving = false
        sbtResolver.triggerSbtResolution

      case COMMAND_SBT_UPDATE_CLASSIFIERS =>
        ProjectManager.getDefault.saveProject(project)
        LifecycleManager.getDefault.saveAll
        val commands = selectProject ::: List("updateClassifiers")
        SBTConsoleTopComponent.openInstance(rootProject, commands, isDebug = false)()

      case COMMAND_BUILD =>
        ProjectManager.getDefault.saveProject(project)
        LifecycleManager.getDefault.saveAll
        val commands = selectProject ::: List("compile")
        SBTConsoleTopComponent.openInstance(rootProject, commands, isDebug = false)()

      case COMMAND_REBUILD =>
        ProjectManager.getDefault.saveProject(project)
        LifecycleManager.getDefault.saveAll
        val commands = selectProject ::: List("clean", "compile")
        SBTConsoleTopComponent.openInstance(rootProject, commands, isDebug = false)()

      case COMMAND_CLEAN =>
        ProjectManager.getDefault.saveProject(project)
        LifecycleManager.getDefault.saveAll
        val commands = selectProject ::: List("clean")
        SBTConsoleTopComponent.openInstance(rootProject, commands, isDebug = false)()

      case COMMAND_RUN =>
        ProjectManager.getDefault.saveProject(project)
        LifecycleManager.getDefault.saveAll
        val commands = selectProject ::: List(
          //"set fork := false",
          //"set javaOptions := Nil",
          "run")
        SBTConsoleTopComponent.openNewInstance(rootProject, commands, isDebug = false)()

      case COMMAND_DEBUG =>
        ProjectManager.getDefault.saveProject(project)
        LifecycleManager.getDefault.saveAll
        val commands = selectProject ::: List(
          //"set fork := true",
          //"set javaOptions := List(\"" + debugOpts(5005) + "\")",
          "run ")
        SBTConsoleTopComponent.openNewInstance(rootProject, commands, isDebug = true)()

      case COMMAND_RUN_SINGLE =>
        ProjectManager.getDefault.saveProject(project)
        LifecycleManager.getDefault.saveAll
        findMainClass(context) foreach { clazz =>
          val commands = selectProject ::: List(
            //"set fork := false",
            //"set javaOptions := Nil",
            "run-main " + clazz)
          SBTConsoleTopComponent.openNewInstance(rootProject, commands, isDebug = false)()
        }

      case COMMAND_DEBUG_SINGLE =>
        ProjectManager.getDefault.saveProject(project)
        LifecycleManager.getDefault.saveAll
        findMainClass(context) foreach { clazz =>
          val commands = selectProject ::: List(
            //"set fork := true",
            //"set javaOptions := List(\"" + debugOpts(5005) + "\")",
            "run-main " + clazz)
          SBTConsoleTopComponent.openNewInstance(rootProject, commands, isDebug = true)()
        }

      case COMMAND_TEST =>
        ProjectManager.getDefault.saveProject(project)
        LifecycleManager.getDefault.saveAll
        val commands = selectProject ::: List("test")
        SBTConsoleTopComponent.openInstance(rootProject, commands, isDebug = false)()

      case COMMAND_TEST_ONLY => // TODO
        ProjectManager.getDefault.saveProject(project)
        LifecycleManager.getDefault.saveAll
        val commands = selectProject ::: List("test-only")
        SBTConsoleTopComponent.openInstance(rootProject, commands, isDebug = false)()

      case _ =>
    }
  }

  private def selectProject = project.getId match {
    case null => Nil
    case id   => List("project " + id)
  }

  private def findMainClass(context: Lookup) = {
    val file = findSources(context, false)(0)
    val mainClasses = getMainClasses(file)
    if (mainClasses.size == 1) {
      // just one main class, resolve from the symbol
      Option(mainClasses.iterator.next().qualifiedName)
    } else if (mainClasses.size > 1) {
      // several main classes, let the user choose
      Option(showMainClassWarning(file, mainClasses))
    } else {
      None
    }
  }

  /**
   * @Note "-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005" does not work under JVM invoked in java.lang.Process
   */
  private def debugOpts(port: Int) = "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=" + port

  /**
   * @TODO minor, arrange a proper place to run it ...
   */
  private def attachDebugger(host: String, port: Int) {
    try {
      JPDADebugger.attach(host, port, Array[AnyRef](null))
    } catch {
      case ex: DebuggerStartException =>
        log.log(Level.WARNING, ex.getMessage, ex.getCause)
        val msg = "Failed to connect debugger to " + host + ":" + port
        DialogDisplayer.getDefault.notify(new NotifyDescriptor.Message(msg, NotifyDescriptor.ERROR_MESSAGE))
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
  val COMMAND_SBT_UPDATE_CLASSIFIERS = "sbt.updateclassifiers"

  val COMMAND_BUILD = ActionProvider.COMMAND_BUILD // compile
  val COMMAND_REBUILD = ActionProvider.COMMAND_REBUILD // clean and compile
  val COMMAND_CLEAN = ActionProvider.COMMAND_CLEAN // clean
  val COMMAND_RUN = ActionProvider.COMMAND_RUN
  val COMMAND_DEBUG = ActionProvider.COMMAND_DEBUG

  val COMMAND_TEST = ActionProvider.COMMAND_TEST
  val COMMAND_TEST_ONLY = ActionProvider.COMMAND_TEST_SINGLE

  val COMMAND_RUN_SINGLE = ActionProvider.COMMAND_RUN_SINGLE
  val COMMAND_DEBUG_SINGLE = ActionProvider.COMMAND_DEBUG_SINGLE
  val COMMAND_DEBUG_TEST_SINGLE = ActionProvider.COMMAND_DEBUG_TEST_SINGLE
}
