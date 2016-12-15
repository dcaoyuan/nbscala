package org.netbeans.modules.scala.console.shell

import java.awt.Color
import java.awt.Font
import java.io.IOException
import java.io.InputStreamReader
import java.io.PipedInputStream
import java.io.PrintStream
import java.io.PrintWriter
import java.util.logging.Logger
import javax.swing.BorderFactory
import javax.swing.JScrollPane
import javax.swing.JTextPane
import javax.swing.SwingUtilities
import javax.swing.UIManager
import javax.swing.text.DefaultCaret
import org.netbeans.api.extexecution.ExecutionDescriptor
import org.netbeans.api.extexecution.ExecutionService
import org.netbeans.api.progress.ProgressHandle
import org.netbeans.api.project.Project
import org.netbeans.api.extexecution.ExternalProcessBuilder
import org.netbeans.modules.scala.core.ScalaExecution
import org.netbeans.modules.scala.console.AnsiConsoleOutputStream
import org.netbeans.modules.scala.console.ConsoleInputOutput
import org.netbeans.modules.scala.console.ConsoleTerminal
import org.netbeans.modules.scala.console.TopComponentId
import org.openide.ErrorManager
import org.openide.filesystems.FileUtil
import org.openide.util.Cancellable
import org.openide.util.ImageUtilities
import org.openide.util.NbBundle
import org.openide.windows.TopComponent
import org.openide.windows.WindowManager

/**
 *
 * @author Caoyuan Deng
 */
final class ScalaConsoleTopComponent private (project: Project) extends TopComponent {
  import ScalaConsoleTopComponent._

  private val log = Logger.getLogger(getClass.getName)

  private val mimeType = "text/x-scala"
  private var console: ScalaConsoleTerminal = _

  initComponents

  private def initComponents() {
    setLayout(new java.awt.BorderLayout())
    setName("Scala " + project.getProjectDirectory.getName)
    setToolTipText(NbBundle.getMessage(classOf[ScalaConsoleTopComponent], "HINT_ScalaConsoleTopComponent") + " for " + project.getProjectDirectory.getPath)
    setIcon(ImageUtilities.loadImage(ICON_PATH, true))
  }

  /**
   * @Note this id will be escaped by PersistenceManager and for findTopCompoment(id)
   */
  override protected val preferredID = toPreferredId(project)

  override def getPersistenceType = TopComponent.PERSISTENCE_NEVER

  override def canClose = true // make sure this tc can be truely closed

  override def open() {
    /**
     * @Note
     * mode.dockInto(this) seems will close this first if this.isOpened()
     * So, when call open(), try to check if it was already opened, if true,
     * no need to call open() again
     */
    val mode = WindowManager.getDefault.findMode("output")
    if (mode != null) {
      mode.dockInto(this)
    }
    super.open
  }

  override protected def componentOpened() {
    // always create a new terminal when is opened/reopend
    console = createTerminal
    super.componentOpened
  }

  override protected def componentClosed() {
    if (console != null) {
      try {
        console.close
      } catch {
        case ex: Exception => log.warning(ex.getMessage)
      }
      console == null
    }
    super.componentClosed
  }

  override protected def componentActivated() {
    // Make the caret visible. See comment under componentDeactivated.
    if (console != null) {
      val caret = console.area.getCaret
      if (caret != null) {
        caret.setVisible(true)
      }
    }
    super.componentActivated
  }

  override protected def componentDeactivated() {
    // I have to turn off the caret when the window loses focus. Text components
    // normally do this by themselves, but the TextAreaReadline component seems
    // to mess around with the editable property of the text pane, and
    // the caret will not turn itself on/off for noneditable text areas.
    if (console != null) {
      val caret = console.area.getCaret
      if (caret != null) {
        caret.setVisible(false)
      }
    }
    super.componentDeactivated
  }

  override def requestFocus() {
    if (console != null) {
      console.area.requestFocus
    }
  }

  override def requestFocusInWindow: Boolean = {
    if (console != null) {
      console.area.requestFocusInWindow
    } else {
      false
    }
  }

  private def getDefaultFont = {
    var size = UIManager.getInt("uiFontSize")
    if (size < 3) {
      size = UIManager.getInt("customFontSize")
      val f = UIManager.get("controlFont").asInstanceOf[Font]
      if (f != null) {
        size = f.getSize
      }
    }
    if (size < 3) {
      size = 11
    }
    new Font("Monospaced", Font.PLAIN, size)
  }

  private def createTerminal: ScalaConsoleTerminal = {
    val textView = new JTextPane()
    textView.setFont(new Font("Monospaced", Font.PLAIN, 13))
    setBorder(BorderFactory.createEmptyBorder)

    // @see core.output2/org.netbeans.core.output2.ui/AbstractOutputPane
    val c = UIManager.getColor("nb.output.selectionBackground")
    if (c != null) {
      textView.setSelectionColor(c)
    }

    textView.getDocument.putProperty("mimeType", mimeType)
    textView.setForeground(Color.BLACK)
    textView.setBackground(Color.WHITE)
    textView.setCaretColor(Color.BLACK)
    textView.getCaret.asInstanceOf[DefaultCaret].setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE)

    val pane = new JScrollPane()
    pane.setViewportView(textView)
    pane.setBorder(BorderFactory.createEmptyBorder)
    pane.setViewportBorder(BorderFactory.createEmptyBorder)
    add(pane)
    validate

    val pwd = FileUtil.toFile(project.getProjectDirectory)
    val scalaHome = ScalaExecution.getScalaHome
    val scalaFile = ScalaExecution.getScala

    val (executable, args) = ScalaExecution.getScalaArgs(scalaHome, project)
    // XXX under Mac OS jdk7, the java.home is point to /Library/Java/JavaVirtualMachines/jdk1.7.0_xx.jdk/Contents/Home/jre
    // instead of /Library/Java/JavaVirtualMachines/jdk1.7.0_xx.jdk/Contents/Home/, which cause the lack of javac
    //builder = builder.addEnvironmentVariable("JAVA_HOME", SBTExecution.getJavaHome)
    val builder = args.foldLeft(new ExternalProcessBuilder(executable))(_ addArgument _)
      .addEnvironmentVariable("SCALA_HOME", ScalaExecution.getScalaHome)
      .workingDirectory(pwd)
    log.info(args.mkString("==== Scala console args ====\n" + executable + "\n", "\n", "\n==== End of Scala console args ===="))

    val pipedIn = new PipedInputStream()
    val console = new ScalaConsoleTerminal(
      textView, pipedIn,
      " " + NbBundle.getMessage(classOf[ScalaConsoleTopComponent], "ScalaConsoleWelcome") + " " + "scala.home=" + scalaHome + "\n")

    val consoleOut = new AnsiConsoleOutputStream(console)
    val in = new InputStreamReader(pipedIn)
    val out = new PrintWriter(new PrintStream(consoleOut))
    val err = new PrintWriter(new PrintStream(consoleOut))
    val inputOutput = new ConsoleInputOutput(in, out, err)

    val execDescriptor = new ExecutionDescriptor().frontWindow(true).inputVisible(true)
      .inputOutput(inputOutput).postExecution(new Runnable() {
        override def run() {
          textView.setEditable(false)
          SwingUtilities.invokeLater(new Runnable() {
            override def run() {
              ScalaConsoleTopComponent.this.close
              ScalaConsoleTopComponent.this.removeAll
            }
          })
        }
      })

    val executionService = ExecutionService.newService(builder, execDescriptor, "Sbt Shell")
    console.underlyingTask = Option(executionService.run())

    console
  }

}

object ScalaConsoleTopComponent {
  private val log = Logger.getLogger(this.getClass.getName)

  /**
   * path to the icon used by the component and its open action
   */
  val ICON_PATH = "org/netbeans/modules/scala/console/resources/scala16x16.png"

  private val compName = "ScalaConsole"
  /**
   * @see org.netbeans.core.windows.persistence.PersistenceManager
   */
  private def toPreferredId(project: Project) = {
    compName + project.getProjectDirectory.getPath
  }

  /**
   * @see org.netbeans.core.windows.persistence.PersistenceManager
   */
  private def toEscapedPreferredId(project: Project) = {
    TopComponentId.escape(compName + project.getProjectDirectory.getPath)
  }

  /**
   * Obtain the SBTConsoleTopComponent instance by project
   */
  def openInstance(project: Project, commands: List[String], background: Boolean = false)(postAction: String => Unit = (_) => ()) {

    val runnableTask = new Runnable() {
      def run {
        val progressHandle = ProgressHandle.createHandle("Openning Scala console...", new Cancellable() {
          def cancel: Boolean = false // XXX todo possible for a AWT Event dispatch thread?
        })
        progressHandle.start

        val tcId = toEscapedPreferredId(project)
        val (tc, isNewCreated) = WindowManager.getDefault.findTopComponent(tcId) match {
          case null =>
            (new ScalaConsoleTopComponent(project), true)
          case tc: ScalaConsoleTopComponent =>
            (tc, false)
          case _ =>
            ErrorManager.getDefault.log(ErrorManager.WARNING,
              "There seem to be multiple components with the '" + tcId +
                "' ID. That is a potential source of errors and unexpected behavior.")
            (null, false)
        }

        if (!tc.isOpened) {
          tc.open
        }

        if (!background) {
          tc.requestActive
        }

        val results = commands map tc.console.runCommand

        if (background && !isNewCreated) {
          tc.close
        }

        postAction(results.lastOption getOrElse null)

        progressHandle.finish
      }
    }

    SwingUtilities.invokeLater(runnableTask)
  }

  class ScalaConsoleTerminal(_area: JTextPane, pipedIn: PipedInputStream, welcome: String) extends ConsoleTerminal(_area, pipedIn, welcome) {

    @throws(classOf[IOException])
    override protected def handleClose() {
      runCommand(":quit") // try to exit underlying process gracefully
      super.handleClose()
    }

    override protected val CompleteTriggerChar = '.'
  }

}
