package org.netbeans.modules.scala.sbt.console

import java.awt.Color
import java.awt.Font
import java.awt.Insets
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io._
import java.util.logging.Level
import java.util.logging.Logger
import javax.swing._
import javax.swing.text.BadLocationException
import org.netbeans.api.extexecution.ExecutionDescriptor
import org.netbeans.api.extexecution.ExecutionService
import org.netbeans.api.extexecution.ExternalProcessBuilder
import org.netbeans.api.project.Project
import org.netbeans.api.project.ui.OpenProjects
import org.openide.ErrorManager
import org.openide.filesystems.FileUtil
import org.openide.loaders.InstanceDataObject
import org.openide.util.Exceptions
import org.openide.util.ImageUtilities
import org.openide.util.NbBundle
import org.openide.windows._

/**
 *
 * @author Caoyuan Deng
 */
final class SBTConsoleTopComponent private (project: Project) extends TopComponent {
  import SBTConsoleTopComponent._
  
  private var finished: Boolean = true
  private var textPane: JTextPane = _
  private val mimeType = "text/x-console"
  private val log = Logger.getLogger(getClass.getName)

  initComponents
  setName("SBT " + project.getProjectDirectory.getName)
  setToolTipText(NbBundle.getMessage(classOf[SBTConsoleTopComponent], "HINT_SBTConsoleTopComponent") + " for " + project.getProjectDirectory.getPath)
  setIcon(ImageUtilities.loadImage(ICON_PATH, true))
 
  private def initComponents() {
    setLayout(new java.awt.BorderLayout())
  }            

  /**
   * @Note this id will be escaped by PersistenceManager and for findTopCompoment(id)
   */
  override
  protected val preferredID = toPreferredId(project)

  override
  def getPersistenceType = TopComponent.PERSISTENCE_NEVER

  override
  def componentOpened() {
    if (finished) {
      // Start a new one
      finished = false
      removeAll
      createTerminal
    }
  }

  override
  def componentClosed() {
    // Leave the terminal session running
  }

  override
  def componentActivated() {
    // Make the caret visible. See comment under componentDeactivated.
    if (textPane != null) {
      val caret = textPane.getCaret
      if (caret != null) {
        caret.setVisible(true)
      }
    }
  }

  override
  def componentDeactivated() {
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
  //override
  //def writeReplace: AnyRef = new ResolvableHelper()

  private def createTerminal {
    val pipeIn = new PipedInputStream()

    textPane = new JTextPane()
    textPane.getDocument.putProperty("mimeType", mimeType)
    textPane.setMargin(new Insets(8, 8, 8, 8))
    textPane.setBackground(new Color(0xf2, 0xf2, 0xf2))
    textPane.setForeground(new Color(0xa4, 0x00, 0x00))

    // From core/output2/**/AbstractOutputPane
    val size = UIManager.get("customFontSize") match { //NOI18N
      case null =>
        UIManager.get("controlFont") match { // NOI18N
          case null => 11
          case f: Font => f.getSize
        }
      case i: java.lang.Integer => i.intValue
    }

    val font = new Font("Monospaced", Font.PLAIN, size) match { 
      case null => new Font("Lucida Sans Typewriter", Font.PLAIN, size)
      case f => f
    } 
    textPane.setFont(font)

    setBorder(BorderFactory.createEmptyBorder)

    // Try to initialize colors from NetBeans properties, see core/output2
    UIManager.getColor("nb.output.selectionBackground") match {
      case null => 
      case c => textPane.setSelectionColor(c)
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

    val sbtHome = SBTExecution.getSbtHome
    val sbtLaunchJar = SBTExecution.getSbtJar(sbtHome)
    if (sbtLaunchJar == null) {
      return
    }

    val (executable, args) = SBTExecution.getArgs(sbtHome)
    
    val consoleOut = new AnsiConsoleOutputStream(new ConsoleOutputStream(
        textPane, 
        " " + NbBundle.getMessage(classOf[SBTConsoleTopComponent], "SBTConsoleWelcome") + " " + "sbt.home=" + sbtHome + "\n",
        pipeIn)
    )
    
    val in = new InputStreamReader(pipeIn)
    val out = new PrintWriter(new PrintStream(consoleOut))
    val err = new PrintWriter(new PrintStream(consoleOut))

    var builder = new ExternalProcessBuilder(executable)
    log.info("==== Sbt console args ====")
    log.info(executable)
    for (arg <- args) {
      log.info(arg)
      builder = builder.addArgument(arg)
    }
    log.info("==== End of Sbt console args ====")

    builder = builder.addEnvironmentVariable("JAVA_HOME", SBTExecution.getJavaHome)
    val pwd = FileUtil.toFile(project.getProjectDirectory)
    builder = builder.workingDirectory(pwd)

    var execDescriptor = new ExecutionDescriptor()
    .frontWindow(true).inputVisible(true)
    .inputOutput(new CustomInputOutput(in, out, err))

    execDescriptor = execDescriptor.postExecution(new Runnable() {
        override
        def run() {
          finished = true
          textPane.setEditable(false)
          SwingUtilities.invokeLater(new Runnable() {
              override
              def run() {
                SBTConsoleTopComponent.this.close
                SBTConsoleTopComponent.this.removeAll
                textPane = null
              }
            })
        }
      })

    val executionService = ExecutionService.newService(builder, execDescriptor, "Sbt Shell")

    executionService.run()

    // [Issue 91208]  avoid of putting cursor in IRB console on line where is not a prompt
    textPane.addMouseListener(new MouseAdapter() {
        override
        def mouseClicked(ev: MouseEvent) {
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

  override
  def requestFocus() {
    if (textPane != null) {
      textPane.requestFocus
    }
  }

  override
  def requestFocusInWindow: Boolean = {
    if (textPane != null) {
      textPane.requestFocusInWindow
    } else false
  }

}

object SBTConsoleTopComponent {
  private val log = Logger.getLogger(this.getClass.getName)
  
  /**
   * path to the icon used by the component and its open action
   */
  val ICON_PATH = "org/netbeans/modules/scala/sbt/resources/sbt.png" 
  
  private val compName = "SBTConsole"
  private val idEscape = try {
    val x = classOf[InstanceDataObject].getDeclaredMethod("escapeAndCut", classOf[String])
    x.setAccessible(true)
    x
  } catch {
    case ex: Exception => null
  }
  private val idUnescape = try {
    val x = classOf[InstanceDataObject].getDeclaredMethod("unescape", classOf[String])
    x.setAccessible(true)
    x
  } catch {
    case ex: Exception => null
  }

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
    escape(compName + project.getProjectDirectory.getPath)
  }
  
  /** 
   * compute filename in the same manner as InstanceDataObject.create
   * [PENDING] in next version this should be replaced by public support
   * likely from FileUtil
   * @see issue #17142
   * 
   */
  private def escape(name: String) = {
    if (idEscape != null) {
      try {
        idEscape.invoke(null, name).asInstanceOf[String]
      } catch {
        case ex: Exception => log.log(Level.INFO, "Escape support failed", ex); name
      }
    } else name
  }
    
  /** 
   * compute filename in the same manner as InstanceDataObject.create
   * [PENDING] in next version this should be replaced by public support
   * likely from FileUtil
   * @see issue #17142
   */
  private def unescape(name: String) = {
    if (idUnescape != null) {
      try {
        idUnescape.invoke(null, name).asInstanceOf[String]
      } catch {
        case ex: Exception => log.log(Level.INFO, "Escape support failed", ex); name
      }
    } else name
  }
  
  /**
   * Obtain the SBTConsoleTopComponent instance by project
   */
  def findInstance(project: Project) = {
    val id = toEscapedPreferredId(project)
    WindowManager.getDefault.findTopComponent(id) match {
      case null => 
        val tc = new SBTConsoleTopComponent(project)
        SwingUtilities.invokeLater(new Runnable() {
            def run {
              val mode = WindowManager.getDefault.findMode("output")
              if (mode != null) {
                mode.dockInto(tc)
              }
            }
          }
        )
        tc
      case x: SBTConsoleTopComponent => x
      case _ =>
        ErrorManager.getDefault.log(ErrorManager.WARNING,
                                    "There seem to be multiple components with the '" + id + 
                                    "' ID. That is a potential source of errors and unexpected behavior.")
        null
    } 
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
      pwd = new File(userHome, "project")
      if (pwd.exists) {
        pwd = if (pwd.isDirectory) new File(userHome) else pwd
      } else {
        pwd.mkdir
        pwd
      }
    }
    pwd
  }

  //@SerialVersionUID(1L)
  //class ResolvableHelper extends Serializable {
  //  def readResolve: AnyRef = getDefault
  //}
  
  private class CustomInputOutput(input: Reader, out: PrintWriter, err: PrintWriter) extends InputOutput {
    private var closed: Boolean = false

    override
    def closeInputOutput() {
      closed = true
    }

    override
    def flushReader: Reader = input

    override
    def getErr: OutputWriter = new CustomOutputWriter(err)

    override
    def getIn: Reader = return input

    override
    def getOut: OutputWriter = new CustomOutputWriter(out)

    override
    def isClosed = closed

    override
    def isErrSeparated = false

    override
    def isFocusTaken = false

    override
    def select() {}

    override
    def setErrSeparated(value: Boolean) {}

    override
    def setErrVisible(value: Boolean) {}

    override
    def setFocusTaken(value: Boolean) {}

    override
    def setInputVisible(value: Boolean) {}

    override
    def setOutputVisible(value: Boolean) {}
  }
  
  private class CustomOutputWriter(pw: PrintWriter) extends OutputWriter(pw) {

    @throws(classOf[IOException])
    override
    def println(s: String, l: OutputListener) {
      println(s)
    }

    @throws(classOf[IOException])
    override
    def reset() {}
  }

}