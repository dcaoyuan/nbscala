package org.netbeans.modules.scala.console.old

import java.awt.Color
import java.awt.Font
import java.awt.Insets
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io._
import java.util.logging.Logger
import javax.swing._
import javax.swing.text.BadLocationException
import org.netbeans.api.extexecution.ExecutionDescriptor
import org.netbeans.api.extexecution.ExecutionService
import org.netbeans.api.project.ui.OpenProjects
import org.netbeans.api.extexecution.ExternalProcessBuilder
import org.netbeans.modules.scala.console.readline.TextAreaReadline
import org.openide.ErrorManager
import org.openide.filesystems.FileUtil
import org.openide.util.Exceptions
import org.openide.util.ImageUtilities
import org.openide.util.NbBundle
import org.openide.windows._

/**
 *
 * @author Tor Norbye, Caoyuan Deng
 */
final class ScalaConsoleTopComponent private () extends TopComponent {
  import ScalaConsoleTopComponent._

  private var finished: Boolean = true
  private var textPane: JTextPane = _
  private val mimeType = "text/x-console"
  private val log = Logger.getLogger(getClass.getName)

  initComponents
  setName(NbBundle.getMessage(classOf[ScalaConsoleTopComponent], "CTL_ScalaConsoleTopComponent"))
  setToolTipText(NbBundle.getMessage(classOf[ScalaConsoleTopComponent], "HINT_ScalaConsoleTopComponent"))
  setIcon(ImageUtilities.loadImage(ICON_PATH, true))

  private def initComponents() {
    setLayout(new java.awt.BorderLayout())
  }

  override def getPersistenceType = TopComponent.PERSISTENCE_ALWAYS

  override def componentOpened() {
    if (finished) {
      // Start a new one
      finished = false
      removeAll
      createTerminal
    }
  }

  override def componentClosed() {
    // Leave the terminal session running
  }

  override def componentActivated() {
    // Make the caret visible. See comment under componentDeactivated.
    if (textPane != null) {
      val caret = textPane.getCaret
      if (caret != null) {
        caret.setVisible(true)
      }
    }
  }

  override def componentDeactivated() {
    // I have to turn off the caret when the window loses focus. Text components
    // normally do this by themselves, but the TextAreaReadline component seems
    // to mess around with the editable property of the text pane, and
    // the caret will not turn itself on/off for noneditable text areas.
    if (textPane != null) {
      val caret = textPane.getCaret
      if (caret != null) {
        caret.setVisible(false)
      }
    }
  }

  /**
   * replaces this in object stream
   */
  override def writeReplace: AnyRef = new ResolvableHelper()

  override protected def preferredID = PREFERRED_ID

  def createTerminal {
    val pipeIn = new PipedInputStream()

    textPane = new JTextPane()
    textPane.getDocument.putProperty("mimeType", mimeType)

    textPane.setMargin(new Insets(8, 8, 8, 8))
    textPane.setCaretColor(new Color(0xa4, 0x00, 0x00))
    textPane.setBackground(new Color(0xf2, 0xf2, 0xf2))
    textPane.setForeground(new Color(0xa4, 0x00, 0x00))

    // From core/output2/**/AbstractOutputPane
    val size = UIManager.get("customFontSize") match { //NOI18N
      case null =>
        UIManager.get("controlFont") match { // NOI18N
          case null    => 11
          case f: Font => f.getSize
        }
      case i: java.lang.Integer => i.intValue
    }

    val font = new Font("Monospaced", Font.PLAIN, size) match {
      case null => new Font("Lucida Sans Typewriter", Font.PLAIN, size)
      case f    => f
    }
    textPane.setFont(font)

    setBorder(BorderFactory.createEmptyBorder)

    // Try to initialize colors from NetBeans properties, see core/output2
    UIManager.getColor("nb.output.selectionBackground") match {
      case null =>
      case c    => textPane.setSelectionColor(c)
    }

    //Object value = Settings.getValue(BaseKit.class, SettingsNames.CARET_COLOR_INSERT_MODE);
    //Color caretColor;
    //if (value instanceof Color) {
    //    caretColor = (Color)value;
    //} else {
    //    caretColor = SettingsDefaults.defaultCaretColorInsertMode;
    //}
    //text.setCaretColor(caretColor);
    //text.setBackground(UIManager.getColor("text")); //NOI18N
    //Color selectedFg = UIManager.getColor ("nb.output.foreground.selected"); //NOI18N
    //if (selectedFg == null) {
    //    selectedFg = UIManager.getColor("textText") == null ? Color.BLACK : //NOI18N
    //       UIManager.getColor("textText"); //NOI18N
    //}
    //
    //Color unselectedFg = UIManager.getColor ("nb.output.foreground"); //NOI18N
    //if (unselectedFg == null) {
    //    unselectedFg = selectedFg;
    //}
    //text.setForeground(unselectedFg);
    //text.setSelectedTextColor(selectedFg);
    //
    //Color selectedErr = UIManager.getColor ("nb.output.err.foreground.selected"); //NOI18N
    //if (selectedErr == null) {
    //    selectedErr = new Color (164, 0, 0);
    //}
    //Color unselectedErr = UIManager.getColor ("nb.output.err.foreground"); //NOI18N
    //if (unselectedErr == null) {
    //    unselectedErr = selectedErr;
    //}

    val pane = new JScrollPane()
    pane.setViewportView(textPane)
    pane.setBorder(BorderFactory.createLineBorder(Color.darkGray))
    add(pane)
    validate

    val scalaHome = ScalaExecution.getScalaHome
    val scalaFile = ScalaExecution.getScala
    if (scalaFile == null) {
      return
    }

    val scalaName = scalaFile.getName
    val scalaArgs = if (scalaName.equals("scala") || scalaName.equalsIgnoreCase("scala.bat") || scalaName.equalsIgnoreCase("scala.exe")) { // NOI18N
      ScalaExecution.getScalaArgs(scalaHome)
    } else Array[String]()

    val taReadline = new TextAreaReadline(textPane,
      " " + NbBundle.getMessage(classOf[ScalaConsoleTopComponent], "ScalaConsoleWelcome") + " " + "scala.home=" + scalaHome + "\n",
      pipeIn) // NOI18N
    val pwd = getMainProjectWorkPath
    val workPath = pwd.getPath
    val in = new InputStreamReader(pipeIn)
    val out = new PrintWriter(new PrintStream(taReadline))
    val err = new PrintWriter(new PrintStream(taReadline))

    var builder: ExternalProcessBuilder = null
    log.info("==== Scala console args ====")
    for (arg <- scalaArgs) {
      log.info(arg)
      if (builder == null) {
        builder = new ExternalProcessBuilder(arg)
      } else {
        builder = builder.addArgument(arg)
      }
    }
    log.info("==== End of Scala console args ====")

    // XXX under Mac OS jdk7, the java.home is point to /Library/Java/JavaVirtualMachines/jdk1.7.0_xx.jdk/Contents/Home/jre
    // instead of /Library/Java/JavaVirtualMachines/jdk1.7.0_xx.jdk/Contents/Home/, which causes the lack of javac
    //builder = builder.addEnvironmentVariable("JAVA_HOME", ScalaExecution.getJavaHome)
    builder = builder.addEnvironmentVariable("SCALA_HOME", ScalaExecution.getScalaHome)
    builder = builder.workingDirectory(pwd)

    var execDescriptor = new ExecutionDescriptor()
      .frontWindow(true).inputVisible(true)
      .inputOutput(new CustomInputOutput(in, out, err))

    execDescriptor = execDescriptor.postExecution(new Runnable() {
      override def run() {
        finished = true
        textPane.setEditable(false)
        SwingUtilities.invokeLater(new Runnable() {
          override def run() {
            ScalaConsoleTopComponent.this.close
            ScalaConsoleTopComponent.this.removeAll
            textPane = null
          }
        })
      }
    })

    val executionService = ExecutionService.newService(builder, execDescriptor, "Scala Shell")

    executionService.run()

    // [Issue 91208]  avoid of putting cursor in IRB console on line where is not a prompt
    textPane.addMouseListener(new MouseAdapter() {
      override def mouseClicked(ev: MouseEvent) {
        val mouseX = ev.getX
        val mouseY = ev.getY
        // Ensure that this is done after the textpane's own mouse listener
        SwingUtilities.invokeLater(new Runnable() {
          def run() {
            // Attempt to force the mouse click to appear on the last line of the text input
            var pos = textPane.getDocument.getEndPosition.getOffset - 1
            if (pos == -1) {
              return
            }

            try {
              val r = textPane.modelToView(pos)

              if (mouseY >= r.y) {
                // The click was on the last line; try to set the X to the position where
                // the user clicked since perhaps it was an attempt to edit the existing
                // input string. Later I could perhaps cast the text document to a StyledDocument,
                // then iterate through the document positions and locate the end of the
                // input prompt (by comparing to the promptStyle in TextAreaReadline).
                r.x = mouseX
                pos = textPane.viewToModel(r.getLocation)
              }

              textPane.getCaret.setDot(pos)
            } catch {
              case ex: BadLocationException => Exceptions.printStackTrace(ex)
            }
          }
        })
      }
    })
  }

  override def requestFocus() {
    if (textPane != null) {
      textPane.requestFocus
    }
  }

  override def requestFocusInWindow: Boolean = {
    if (textPane != null) {
      textPane.requestFocusInWindow
    } else false
  }

  private def getMainProjectWorkPath: File = {
    var pwd: File = null
    val mainProject = OpenProjects.getDefault.getMainProject
    if (mainProject != null) {
      var fo = mainProject.getProjectDirectory
      if (!fo.isFolder) {
        fo = fo.getParent
      }
      pwd = FileUtil.toFile(fo)
    }
    if (pwd == null) {
      val userHome = System.getProperty("user.home")
      pwd = new File(userHome, "NetBeansProjects")
      if (pwd.exists) {
        pwd = if (pwd.isDirectory) new File(userHome) else pwd
      } else {
        pwd.mkdir
        pwd
      }
    }
    pwd
  }

}

object ScalaConsoleTopComponent {
  private lazy val instance: ScalaConsoleTopComponent = new ScalaConsoleTopComponent()

  /**
   * path to the icon used by the component and its open action
   */
  val ICON_PATH = "org/netbeans/modules/scala/console/resources/scala16x16.png" // NOI18N
  val PREFERRED_ID = "ScalaConsoleTopComponent" // NOI18N

  /**
   * Gets default instance. Do not use directly: reserved for *.settings files
   * only, i.e. deserialization routines; otherwise you could get a
   * non-deserialized instance. To obtain the singleton instance, use
   * {@link findInstance}.
   */
  def getDefault = synchronized { instance }

  /**
   * Obtain the IrbTopComponent instance. Never call {@link #getDefault}
   * directly!
   */
  def findInstance() = synchronized {
    val win = WindowManager.getDefault().findTopComponent(PREFERRED_ID)
    win match {
      case null =>
        ErrorManager.getDefault.log(ErrorManager.WARNING,
          "Cannot find MyWindow component. It will not be located properly in the window system.")
        instance
      case x: ScalaConsoleTopComponent => x
      case _ =>
        ErrorManager.getDefault.log(ErrorManager.WARNING,
          "There seem to be multiple components with the '" + PREFERRED_ID +
            "' ID. That is a potential source of errors and unexpected behavior.")
        instance
    }

  }

  @SerialVersionUID(1L)
  class ResolvableHelper extends Serializable {
    def readResolve: AnyRef = getDefault
  }

  private class CustomInputOutput(input: Reader, out: PrintWriter, err: PrintWriter) extends InputOutput {

    private var closed: Boolean = false

    override def closeInputOutput() {
      closed = true
    }

    override def flushReader: Reader = input

    override def getErr: OutputWriter = new CustomOutputWriter(err)

    override def getIn: Reader = return input

    override def getOut: OutputWriter = new CustomOutputWriter(out)

    override def isClosed = closed

    override def isErrSeparated = false

    override def isFocusTaken = false

    override def select() {
    }

    override def setErrSeparated(value: Boolean) {
    }

    override def setErrVisible(value: Boolean) {
    }

    override def setFocusTaken(value: Boolean) {
    }

    override def setInputVisible(value: Boolean) {
    }

    override def setOutputVisible(value: Boolean) {
    }
  }

  private class CustomOutputWriter(pw: PrintWriter) extends OutputWriter(pw) {

    @throws(classOf[IOException])
    override def println(s: String, l: OutputListener) {
      println(s)
    }

    @throws(classOf[IOException])
    override def reset() {
    }
  }

}