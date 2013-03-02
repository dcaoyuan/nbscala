package org.netbeans.modules.scala.console.shell

import java.awt.Color
import java.awt.Font
import java.awt.Insets
import java.io.IOException
import java.io.InputStreamReader
import java.io.PipedInputStream
import java.io.PrintStream
import java.io.PrintWriter
import java.util.logging.Logger
import java.util.regex.Pattern
import javax.swing._
import javax.swing.text.AttributeSet
import javax.swing.text.SimpleAttributeSet
import javax.swing.text.StyleConstants
import org.netbeans.api.extexecution.ExecutionDescriptor
import org.netbeans.api.extexecution.ExecutionService
import org.netbeans.api.extexecution.ExternalProcessBuilder
import org.netbeans.api.progress.ProgressHandleFactory
import org.netbeans.api.project.Project
import org.netbeans.modules.scala.console.AnsiConsoleOutputStream
import org.netbeans.modules.scala.console.ConsoleInputOutput
import org.netbeans.modules.scala.console.ConsoleTerminal
import org.netbeans.modules.scala.console.ConsoleOutputLineParser
import org.netbeans.modules.scala.console.TopComponentId
import org.openide.ErrorManager
import org.openide.filesystems.FileUtil
import org.openide.util.Cancellable
import org.openide.util.ImageUtilities
import org.openide.util.NbBundle
import org.openide.windows.TopComponent
import org.openide.windows.WindowManager
import scala.collection.mutable.ArrayBuffer

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
  override
  protected val preferredID = toPreferredId(project)

  override
  def getPersistenceType = TopComponent.PERSISTENCE_NEVER

  override
  def canClose = true // make sure this tc can be truely closed
  
  override 
  def open() {
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
  
  override
  protected def componentOpened() {
    // always create a new terminal when is opened/reopend
    console = createTerminal
    super.componentOpened
  }

  override
  protected def componentClosed() {
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

  override
  protected def componentActivated() {
    // Make the caret visible. See comment under componentDeactivated.
    if (console != null) {
      val caret = console.area.getCaret
      if (caret != null) {
        caret.setVisible(true)
      }
    }
    super.componentActivated
  }

  override
  protected def componentDeactivated() {
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
  
  override
  def requestFocus() {
    if (console != null) {
      console.area.requestFocus
    }
  }

  override
  def requestFocusInWindow: Boolean = {
    if (console != null) {
      console.area.requestFocusInWindow
    } else {
      false
    }
  }

  private def createTerminal: ScalaConsoleTerminal = {
    // From core/output2/**/AbstractOutputPane
    val fontSize = UIManager.get("customFontSize") match { //NOI18N
      case null =>
        UIManager.get("controlFont") match { // NOI18N
          case null => 11
          case f: Font => f.getSize
        }
      case i: java.lang.Integer => i.intValue
    }

    val font = new Font("Monospaced", Font.PLAIN, fontSize) match { 
      case null => new Font("Lucida Sans Typewriter", Font.PLAIN, fontSize)
      case x => x
    } 

    val textPane = new JTextPane()
    textPane.getDocument.putProperty("mimeType", mimeType)
    textPane.setMargin(new Insets(8, 8, 8, 8))
    textPane.setFont(font)
    textPane.setForeground(Color.BLACK)
    textPane.setBackground(Color.WHITE)
    textPane.setCaretColor(Color.BLACK)

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

    setBorder(BorderFactory.createEmptyBorder)

    val pane = new JScrollPane()
    pane.setViewportView(textPane)
    pane.setBorder(BorderFactory.createLineBorder(Color.darkGray))
    add(pane)
    validate

    val scalaHome = ScalaExecution.getScalaHome
    val scalaFile = ScalaExecution.getScala
    if (scalaFile == null) {
      return null
    }

    val (executable, args) = ScalaExecution.getScalaArgs(scalaHome, project)
    var builder = args.foldLeft(new ExternalProcessBuilder(executable))(_ addArgument _)
    log.info(args.mkString("==== Scala console args ====\n" + executable + "\n", "\n", "\n==== End of Scala console args ===="))

    // XXX under Mac OS jdk7, the java.home is point to /Library/Java/JavaVirtualMachines/jdk1.7.0_xx.jdk/Contents/Home/jre
    // instead of /Library/Java/JavaVirtualMachines/jdk1.7.0_xx.jdk/Contents/Home/, which cause the lack of javac
    //builder = builder.addEnvironmentVariable("JAVA_HOME", SBTExecution.getJavaHome)
    builder = builder.addEnvironmentVariable("SCALA_HOME", ScalaExecution.getScalaHome)
    val pwd = FileUtil.toFile(project.getProjectDirectory)
    builder = builder.workingDirectory(pwd)
    
    val pipedIn = new PipedInputStream()
    val console = new ScalaConsoleTerminal(
      textPane, pipedIn,
      " " + NbBundle.getMessage(classOf[ScalaConsoleTopComponent], "ScalaConsoleWelcome") + " " + "scala.home=" + scalaHome + "\n"
    )
    
    val consoleOut = new AnsiConsoleOutputStream(console)
    val in = new InputStreamReader(pipedIn)
    val out = new PrintWriter(new PrintStream(consoleOut))
    val err = new PrintWriter(new PrintStream(consoleOut))
    val inputOutput = new ConsoleInputOutput(in, out, err)
    
    val execDescriptor = new ExecutionDescriptor().frontWindow(true).inputVisible(true)
    .inputOutput(inputOutput).postExecution(new Runnable() {
        override
        def run() {
          textPane.setEditable(false)
          SwingUtilities.invokeLater(new Runnable() {
              override
              def run() {
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
  
  val defaultFg = Color.BLACK
  val defaultBg = Color.WHITE
  val linkFg = Color.BLUE
  
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
  def openInstance(project: Project, background: Boolean, commands: List[String], message: String = null)(postAction: String => Unit = null) {
    val progressHandle = ProgressHandleFactory.createHandle(message, new Cancellable() {
        def cancel: Boolean = false // XXX todo possible for a AWT Event dispatch thread?
      }
    )
    
    val runnableTask = new Runnable() {
      def run {
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
              
        if (postAction != null) {
          postAction(results.lastOption getOrElse null)
        }
          
        progressHandle.finish
      }
    }
      
    SwingUtilities.invokeLater(runnableTask)
  }
  
  class ScalaConsoleTerminal(_area: JTextPane, pipedIn: PipedInputStream, welcome: String) extends ConsoleTerminal(_area, pipedIn, welcome) {
  
    @throws(classOf[IOException])
    override 
    protected def handleClose() {
      runCommand(":quit") // try to exit underlying process gracefully 
      super.handleClose()
    }
    
    override 
    protected val CompleteTriggerChar = '.'
    
    override 
    protected val lineParser = new ConsoleOutputLineParser() {
      
      val INFO_PREFIX    = "[info]"
      val WARN_PREFIX    = "[warn]"
      val ERROR_PREFIX   = "[error]"
      val SUCCESS_PREFIX = "[success]"
  
      val WINDOWS_DRIVE = "(?:[a-zA-Z]\\:)?"
      val FILE_CHAR = "[^\\[\\]\\:\\\"]" // not []:", \s is allowd
      val FILE = "(" + WINDOWS_DRIVE + "(?:" + FILE_CHAR + "*))"
      val LINE = "(([1-9][0-9]*))"  // line number
      val ROL = ".*\\s?\\s?"        // rest of line (may end with "\n" or "\r\n")
      val SEP = "\\:"               // seperator between file path and line number
      val STD_SUFFIX = FILE + SEP + LINE + ROL  // ((?:[a-zA-Z]\:)?(?:[^\[\]\:\"]*))\:(([1-9][0-9]*)).*\s?
  
      val rERROR_WITH_FILE = Pattern.compile("\\Q" + ERROR_PREFIX + "\\E" + "\\s?" + STD_SUFFIX) // \Q[error]\E\s?((?:[a-zA-Z]\:)?(?:[^\[\]\:\"]*))\:(([1-9][0-9]*)).*\s?
      val rWARN_WITH_FILE =  Pattern.compile("\\Q" + WARN_PREFIX  + "\\E" + "\\s?" + STD_SUFFIX) //  \Q[warn]\E\s?((?:[a-zA-Z]\:)?(?:[^\[\]\:\"]*))\:(([1-9][0-9]*)).*\s?

      lazy val infoStyle = {
        val x = new SimpleAttributeSet()
        StyleConstants.setForeground(x, defaultStyle.getAttribute(StyleConstants.Foreground).asInstanceOf[Color])
        StyleConstants.setBackground(x, defaultStyle.getAttribute(StyleConstants.Background).asInstanceOf[Color])
        x
      }
      lazy val warnStyle = {
        val x = new SimpleAttributeSet()
        StyleConstants.setForeground(x, new Color(0xB9, 0x7C, 0x00))
        StyleConstants.setBackground(x, defaultStyle.getAttribute(StyleConstants.Background).asInstanceOf[Color])
        x
      }
      lazy val errorStyle = {
        val x = new SimpleAttributeSet()
        StyleConstants.setForeground(x, Color.RED)
        StyleConstants.setBackground(x, defaultStyle.getAttribute(StyleConstants.Background).asInstanceOf[Color])
        x
      }
      lazy val successStyle = {
        val x = new SimpleAttributeSet()
        StyleConstants.setForeground(x, Color.GREEN)
        StyleConstants.setBackground(x, defaultStyle.getAttribute(StyleConstants.Background).asInstanceOf[Color])
        x
      }
  
      def parseLine(line: String): Array[(String, AttributeSet)] = {
        if (line.length < 6) {
          Array((line, defaultStyle))
        } else {
          val texts = new ArrayBuffer[(String, AttributeSet)]()
          val testRest_style = if (line.startsWith(ERROR_PREFIX)) {
      
            val m = rERROR_WITH_FILE.matcher(line)
            if (m.matches && m.groupCount >= 3) {
              texts += (("[", defaultStyle))
              texts += (("error", errorStyle))
              texts += (("] ", defaultStyle))
              val textRest = line.substring(ERROR_PREFIX.length + 1, line.length)
        
              val fileName = m.group(1)
              val lineNo = m.group(2)
              val linkStyle = new SimpleAttributeSet()
              StyleConstants.setForeground(linkStyle, linkFg)
              StyleConstants.setUnderline(linkStyle, true)
              linkStyle.addAttribute("file", fileName)
              linkStyle.addAttribute("line", lineNo)
        
              (textRest, linkStyle)
            } else {
              texts += (("[", defaultStyle))
              texts += (("error", errorStyle))
              texts += (("]", defaultStyle))
              val textRest = line.substring(ERROR_PREFIX.length, line.length)
        
              (textRest, errorStyle)
            }
      
          } else if (line.startsWith(WARN_PREFIX)) {
      
            val m = rWARN_WITH_FILE.matcher(line)
            if (m.matches && m.groupCount >= 3) {
              texts += (("[", defaultStyle))
              texts += (("warn", warnStyle))
              texts += (("] ", defaultStyle))
              val textRest = line.substring(WARN_PREFIX.length + 1, line.length)
        
              val fileName = m.group(1)
              val lineNo = m.group(2)
              val linkStyle = new SimpleAttributeSet()
              StyleConstants.setForeground(linkStyle, linkFg)
              StyleConstants.setUnderline(linkStyle, true)
              linkStyle.addAttribute("file", fileName)
              linkStyle.addAttribute("line", lineNo)
        
              (textRest, linkStyle)
            } else {
              texts += (("[", defaultStyle))
              texts += (("warn", warnStyle))
              texts += (("]", defaultStyle))
              val textRest = line.substring(WARN_PREFIX.length, line.length)
        
              (textRest, warnStyle)
            }
      
          } else if (line.startsWith(INFO_PREFIX)) {
      
            texts += (("[", defaultStyle))
            texts += (("info", infoStyle))
            texts += (("]", defaultStyle))
            val textRest = line.substring(INFO_PREFIX.length, line.length)
      
            (textRest, defaultStyle)
      
          } else if (line.startsWith(SUCCESS_PREFIX)) {
      
            texts += (("[", defaultStyle))
            texts += (("success", successStyle))
            texts += (("]", defaultStyle))
            val textRest = line.substring(SUCCESS_PREFIX.length, line.length)
      
            (textRest, defaultStyle)
      
          } else {
            (line, defaultStyle)
          }
    
          texts += testRest_style
      
          texts.toArray
        }
      }
    }
  }
  
}
