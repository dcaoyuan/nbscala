package org.netbeans.modules.scala.console

import java.awt.Color
import java.awt.Point
import java.awt.Component
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.io.PrintStream
import java.util.concurrent.Future
import java.util.logging.Level
import java.util.logging.Logger
import java.util.regex.Pattern
import javax.swing.DefaultListCellRenderer
import javax.swing.JComboBox
import javax.swing.JPopupMenu
import javax.swing.JScrollPane
import javax.swing.JTextPane
import javax.swing.JViewport
import javax.swing.SwingUtilities
import javax.swing.plaf.basic.BasicComboPopup
import javax.swing.text.AttributeSet
import javax.swing.text.Document
import javax.swing.text.JTextComponent
import javax.swing.text.SimpleAttributeSet
import javax.swing.text.StyleConstants
import javax.swing.text.StyledDocument
import scala.collection.mutable
import org.netbeans.api.java.classpath.GlobalPathRegistry
import org.openide.filesystems.FileObject
import org.openide.filesystems.FileUtil

final case class StyledText(text: String, style: AttributeSet)

class ConsoleOutputLineParser(defaultStyle: AttributeSet) {
  val INFO_PREFIX = "[info]"
  val WARN_PREFIX = "[warn]"
  val ERROR_PREFIX = "[error]"
  val SUCCESS_PREFIX = "[success]"

  val WINDOWS_DRIVE = "(?:[a-zA-Z]\\:)?"
  val FILE_CHAR = "[^\\[\\]\\:\\\"]" // not []:", \s is allowd
  val FILE = "(" + WINDOWS_DRIVE + "(?:" + FILE_CHAR + "*))"
  val LINE = "(([1-9][0-9]*))" // line number
  val ROL = ".*\\s?\\s?" // rest of line (may end with "\n" or "\r\n")
  val SEP = "\\:" // seperator between file path and line number
  val FILE_PATH_SUFFIX = FILE + SEP + LINE + ROL // ((?:[a-zA-Z]\:)?(?:[^\[\]\:\"]*))\:(([1-9][0-9]*)).*\s?

  val METHOD = "((?:[\\w\\$]+)|<init>)"

  val PACKAGE_PREFIX = "((?:(?:\\w+)\\.)*?)"

  val CLASS = PACKAGE_PREFIX + "((?:[\\w\\$])+)"
  val STACK_FRAME_PREFIX = "\\]?\\s+at\\s"
  val STACK_FRAME = STACK_FRAME_PREFIX + CLASS + "\\." + METHOD + "\\((\\w+\\.\\w+)" + SEP + LINE + "\\)" + ROL

  val rERROR_WITH_FILE = Pattern.compile("\\Q" + ERROR_PREFIX + "\\E" + "\\s?" + FILE_PATH_SUFFIX) // \Q[error]\E\s?((?:[a-zA-Z]\:)?(?:[^\[\]\:\"]*))\:(([1-9][0-9]*)).*\s?
  val rWARN_WITH_FILE = Pattern.compile("\\Q" + WARN_PREFIX + "\\E" + "\\s?" + FILE_PATH_SUFFIX) //  \Q[warn]\E\s?((?:[a-zA-Z]\:)?(?:[^\[\]\:\"]*))\:(([1-9][0-9]*)).*\s?

  val rFILE_PATH = Pattern.compile("\\]?\\s*" + FILE_PATH_SUFFIX)
  val rSTACK_FRAME_PATTERN = Pattern.compile(STACK_FRAME)

  val linkFg = Color.BLUE

  lazy val infoStyle = {
    val x = new SimpleAttributeSet()
    StyleConstants.setForeground(x, defaultStyle.getAttribute(StyleConstants.Foreground).asInstanceOf[Color])
    StyleConstants.setBackground(x, defaultStyle.getAttribute(StyleConstants.Background).asInstanceOf[Color])
    x
  }

  lazy val warnStyle = {
    val x = new SimpleAttributeSet()
    StyleConstants.setForeground(x, AnsiConsoleOutputStream.YELLOW)
    StyleConstants.setBackground(x, defaultStyle.getAttribute(StyleConstants.Background).asInstanceOf[Color])
    x
  }

  lazy val errorStyle = {
    val x = new SimpleAttributeSet()
    StyleConstants.setForeground(x, AnsiConsoleOutputStream.RED)
    StyleConstants.setBackground(x, defaultStyle.getAttribute(StyleConstants.Background).asInstanceOf[Color])
    x
  }

  lazy val successStyle = {
    val x = new SimpleAttributeSet()
    StyleConstants.setForeground(x, AnsiConsoleOutputStream.GREEN)
    StyleConstants.setBackground(x, defaultStyle.getAttribute(StyleConstants.Background).asInstanceOf[Color])
    x
  }

  /**
   * fully qualified classname to relative path in classpath
   */
  private def classNameToPath(pkg: String, fileName: String): String = {
    val fileSep = System.getProperty("file.separator", "/")
    pkg.replaceAll("\\.", fileSep) + fileSep + fileName
  }

  def parseLine(lineTexts: Array[StyledText]): Array[StyledText] = {
    val texts = new mutable.ArrayBuffer[StyledText]()
    var i = 0

    def addStyledTexts(line: String, fileName: String, lineNo: String, linkStart: Int, linkEnd: Int) {
      if (linkStart >= 0 && linkEnd >= 0) {
        val linkStyle = new SimpleAttributeSet()
        StyleConstants.setForeground(linkStyle, linkFg)
        StyleConstants.setUnderline(linkStyle, true)
        linkStyle.addAttribute("file", fileName)
        linkStyle.addAttribute("line", lineNo)

        texts += StyledText(line.substring(0, linkStart), defaultStyle)
        texts += StyledText(line.substring(linkStart, linkEnd), linkStyle)
        texts += StyledText(line.substring(linkEnd, line.length), defaultStyle)
      } else {
        texts += StyledText(line, defaultStyle)
      }
    }

    while (i < lineTexts.length) {
      val text = lineTexts(i)
      val mFile = rFILE_PATH.matcher(text.text)
      val mStackFrame = rSTACK_FRAME_PATTERN.matcher(text.text)

      if (mStackFrame.matches && mStackFrame.groupCount >= 5) {
        val pkg = mStackFrame.group(1)
        val fileName = mStackFrame.group(4)
        val lineNo = mStackFrame.group(5)
        val file = getFile(classNameToPath(pkg, fileName))
        val filePath = if (file != null) file.getAbsolutePath else pkg + "." + fileName
        val linkStart = if (file != null) mStackFrame.start(4) else -1
        val linkEnd = if (file != null) mStackFrame.end(5) else -1

        addStyledTexts(text.text, filePath, lineNo, linkStart, linkEnd)

      } else if (mFile.matches && mFile.groupCount >= 3) {
        val fileName = mFile.group(1)
        val lineNo = mFile.group(2)
        val linkStart = mFile.start(1)
        val linkEnd = mFile.end(2)

        addStyledTexts(text.text, fileName, lineNo, linkStart, linkEnd)

      } else {
        texts += text
      }
      i += 1
    }

    texts.toArray
  }

  private def getFile(relativePath: String): File = {
    val fileObject = GlobalPathRegistry.getDefault().findResource(relativePath)

    if (fileObject != null) FileUtil.toFile(fileObject) else null
  }
}

class ConsoleCapturer {
  import ConsoleCapturer._

  private var _isCapturing = false
  private var _linesUnderCapturing = new StringBuilder()
  private var _endAction: EndAction = _

  /** a temperary variable that will be injected to _endAction at once, and then reset to false */
  private var _isHidingOutput = false

  def action = _endAction

  def isCapturing = _isCapturing
  def discard {
    _isCapturing = false
  }

  /**
   * XXX The logic is too complex for jline1 and windows terminal, todo
   */
  def hideOutput = {
    _isHidingOutput = true
    this
  }

  def capture(endAction: EndAction => Unit) {
    _isCapturing = true
    _endAction = new EndAction {
      // should keep a self copy of of _isHidingOutput as soon as possible to avoid being changed by outside
      val isHidingOutput = _isHidingOutput
      def apply() = endAction(this)
    }
    _isHidingOutput = false // reset back to default
  }

  def endWith(lastOutput: String): EndAction = {
    _isCapturing = false

    _endAction.lines = _linesUnderCapturing.toString
    _linesUnderCapturing.delete(0, _linesUnderCapturing.length)
    _endAction.lastOutput = lastOutput
    _endAction
  }

  def append(str: String) {
    _linesUnderCapturing.append(str)
  }
}

object ConsoleCapturer {
  trait EndAction {
    /** print captured output on console or not */
    def isHidingOutput: Boolean
    def apply(): Unit
    var lines: String = ""
    var lastOutput: String = ""
  }
}

/**
 *
 * @author Caoyuan Deng
 */
class ConsoleTerminal(val area: JTextPane, pipedIn: PipedInputStream, welcome: String) extends OutputStream {
  private val log = Logger.getLogger(getClass.getName)

  def this(area: JTextPane) = this(area, null, null)

  trait Completer {
    val popup: JPopupMenu
    val combo: JComboBox[String]
    var invokeOffset: Int = 0

    def matches(input: String, candicate: String): Int = {
      val start = (if (CompleteTriggerChar != -1) {
        input.lastIndexOf(CompleteTriggerChar)
      } else {
        input.lastIndexOf(' ')
      }) match {
        case -1  => input.lastIndexOf('\t')
        case idx => idx
      }

      if (start == -1) {
        backMatches(input, candicate) // best try
      } else if (start == input.length - 1) { // exactly the last char
        0
      } else {
        val word = input.substring(start + 1, input.length)
        if (candicate.startsWith(word)) {
          word.length
        } else {
          -1
        }
      }
    }

    /**
     * matches backward maximal length
     */
    private def backMatches(input: String, candicate: String): Int = {
      val len = math.min(input.length, candicate.length)
      var matchedLength = -1
      var i = 0
      while (i < len) {
        val toCompare = candicate.substring(0, i + 1)
        if (input.endsWith(toCompare)) {
          matchedLength = i + 1
        }
        i += 1
      }
      matchedLength
    }

    /**
     * matches backward maximal length
     */
    def wordMatches(word: String, candicate: String): Int = {
      val len = math.min(word.length, candicate.length)
      var matchedLength = 0
      var i = 0
      while (i < len) {
        val toCompare = candicate.substring(0, i + 1)
        if (word.endsWith(toCompare)) {
          matchedLength = i + 1
        }
        i += 1
      }
      matchedLength
    }

  }

  /** buffer which will be used for the next line */
  private val buf = new StringBuilder(1000)
  private val styles = new mutable.HashMap[Int, AttributeSet]()
  private val linesBuf = new mutable.ArrayBuffer[Array[StyledText]]()
  private val textsBuf = new mutable.ArrayBuffer[StyledText]()
  private var isWaitingUserInput = false

  /** override me to enable it, -1 means no trigger */
  protected val CompleteTriggerChar = -1.toChar
  protected val outputCapturer = new ConsoleCapturer()

  lazy val defaultStyle = {
    val x = new SimpleAttributeSet()
    StyleConstants.setForeground(x, area.getForeground)
    StyleConstants.setBackground(x, area.getBackground)
    x
  }

  lazy val sequenceStyle = {
    val x = new SimpleAttributeSet()
    StyleConstants.setForeground(x, area.getForeground)
    StyleConstants.setBackground(x, area.getBackground)
    x
  }

  private var prevStyle = defaultStyle
  private var _currStyle = defaultStyle
  def currStyle = _currStyle
  def currStyle_=(x: SimpleAttributeSet) {
    prevStyle = _currStyle
    _currStyle = x
  }

  protected def copyStyle(style: AttributeSet) = {
    val x = new SimpleAttributeSet()
    StyleConstants.setForeground(x, style.getAttribute(StyleConstants.Foreground).asInstanceOf[Color])
    StyleConstants.setBackground(x, style.getAttribute(StyleConstants.Background).asInstanceOf[Color])
    x
  }

  /**
   * override to define custom line parser
   */
  protected val lineParser = new ConsoleOutputLineParser(defaultStyle)

  val pipedOut = new PrintStream(new PipedOutputStream(pipedIn))
  val terminalInput = new ConsoleTerminalInput
  private val mouseListener = new ConsoleMouseListener(area)
  private val doc = area.getDocument.asInstanceOf[StyledDocument]

  area.setCaret(new BlockCaret)
  area.addKeyListener(terminalInput)
  area.addMouseListener(mouseListener)
  area.addMouseMotionListener(mouseListener)

  protected val completer = new Completer {
    lazy val combo = {
      val x = new JComboBox[String]()
      x.setFont(area.getFont)
      x.setRenderer(new DefaultListCellRenderer())
      x
    }

    lazy val popup = {
      val x = new BasicComboPopup(combo) {
        //override def getInsets = new Insets(4, 4, 4, 4) // looks ugly under Windows
        // JPopupMenu will aleays show from (x, y) to right-lower, but we want to it shows to right-upper
        override def show(invoker: Component, _x: Int, _y: Int) {
          val x = _x
          val y = _y - getPreferredSize.height
          super.show(invoker, x, y)
        }
      }
      x.setFont(area.getFont)
      x
    }
  }

  if (welcome != null) {
    val messageStyle = new SimpleAttributeSet()
    StyleConstants.setBackground(messageStyle, area.getForeground)
    StyleConstants.setForeground(messageStyle, area.getBackground)
    overwrite(StyledText(welcome, messageStyle))
  }

  /**
   * We'll cancel underlyingTask here to make sure all opened streams are properly closed
   */
  private var _underlyingTask: Option[Future[Integer]] = None
  def underlyingTask = _underlyingTask
  def underlyingTask_=(underlyingTask: Option[Future[Integer]]) {
    _underlyingTask = underlyingTask
  }

  def runCommand(command: String): String = {
    pipedOut.println(command)
    ""
  }

  @throws(classOf[IOException])
  override def close() {
    flush()
    handleClose()
  }

  /**
   * This method is called when the stream is closed and it allows
   * extensions of this class to do additional tasks.
   * For example, closing an underlying stream, etc.
   *
   * By override this method, extra tasks can be performance before/after the
   * existed task that have been defined here.
   */
  @throws(classOf[IOException])
  protected def handleClose() {
    underlyingTask map (_.cancel(true))

    area.removeKeyListener(terminalInput)
    area.removeMouseListener(mouseListener)
    area.removeMouseMotionListener(mouseListener)
  }

  @throws(classOf[IOException])
  override def write(b: Array[Byte]) {
    isWaitingUserInput = false
    if (currStyle != prevStyle) {
      styles(buf.length) = copyStyle(currStyle)
    }
    buf.append(new String(b, 0, b.length))
  }

  @throws(classOf[IOException])
  override def write(b: Array[Byte], offset: Int, length: Int) {
    isWaitingUserInput = false
    if (currStyle != prevStyle) {
      styles(buf.length) = copyStyle(currStyle)
    }
    buf.append(new String(b, offset, length))
  }

  @throws(classOf[IOException])
  override def write(b: Int) {
    isWaitingUserInput = false
    if (currStyle != prevStyle) {
      styles(buf.length) = copyStyle(currStyle)
    }
    buf.append(b.toChar)
  }

  @throws(classOf[IOException])
  override def flush() {
    doFlushWith(true)(())
  }

  /**
   * @param   is from a carry out flush
   * @param   an afterward action, with param 'current inputing line' which could be used or just ignore
   * @return  the last non-teminated line. if it's waiting for user input, this
   *          line.length should > 0 (with at least one char)
   */
  protected[console] def doFlushWith(isCarryOut: Boolean)(postAction: => Unit) {
    val (lines, nonLineTeminatedText) = toLines(buf)

    if (isCarryOut) {
      isWaitingUserInput = nonLineTeminatedText.length > 0
    }

    // @Note
    // doFlushWith is usaully called by inputstream/outputstream processing thread,
    // whatever, we will force Swing related code to be executed in event dispatch thread
    if (!outputCapturer.isCapturing) {
      val theIsWaitingUserInput = isWaitingUserInput // keep a copy for async call
      SwingUtilities.invokeLater(new Runnable() {
        def run() {
          try {
            writeLines(lines)
            writeNonLineTeminatedText(nonLineTeminatedText)
            postAction
            if (theIsWaitingUserInput) {
              val input = getInputingLine
              if (CompleteTriggerChar != -1
                && input.length > 0
                && input.charAt(input.length - 1) == CompleteTriggerChar) {
                terminalInput.invokeCompleteAction
              }
            }
          } catch {
            case ex: Throwable => log.log(Level.SEVERE, ex.getMessage, ex)
          }
        }
      })
    } else {
      lines.flatten map (_.text) foreach outputCapturer.append
      val theIsWaitingUserInput = isWaitingUserInput // keep a copy for async call
      SwingUtilities.invokeLater(new Runnable() {
        def run() {
          try {
            writeLines(lines)
            writeNonLineTeminatedText(nonLineTeminatedText)
            postAction
            if (theIsWaitingUserInput) { // it's time to end capturing
              val captureEndAction = outputCapturer.endWith(nonLineTeminatedText)
              captureEndAction()
            }
          } catch {
            case ex: Throwable => log.log(Level.SEVERE, ex.getMessage, ex)
          }
        }
      })
    }
  }

  /**
   * Read lines from buf, keep not-line-completed chars in buf
   * @return (lines, non-teminated line ie. current input line)
   */
  private def toLines(buf: StringBuilder): (Array[Array[StyledText]], String) = {
    var currStyle: AttributeSet = defaultStyle
    var newTextPos = 0
    val l = buf.length
    var i = 0
    while (i < l) {
      val c = buf.charAt(i)
      val style = styles.getOrElse(i, currStyle)

      if (style != currStyle && !(c == '\n' || c == '\r')) {
        val text = buf.substring(newTextPos, i)
        textsBuf += StyledText(text, copyStyle(currStyle))
        currStyle = style
        newTextPos = i
      }

      if (c == '\n' || c == '\r') {
        val text = buf.substring(newTextPos, i) + "\n" // strip '\r' for Windows
        textsBuf += StyledText(text, copyStyle(currStyle))
        // trick: '\u0000' is a char that is non-equalable with '\n' or '\r'
        val c1 = if (i + 1 < l) buf.charAt(i + 1) else '\u0000'
        if (c == '\n' && c1 == '\r' || c == '\r' && c1 == '\n') {
          i += 1 // bypass c1, which could be '\r' for Windows
        }

        linesBuf += textsBuf.toArray
        textsBuf.clear
        newTextPos = i + 1 // strip c 
      }

      i += 1
    }

    val rest = buf.substring(newTextPos, buf.length)

    val lines = linesBuf.toArray
    linesBuf.clear
    styles.clear
    buf.delete(0, buf.length)

    (lines, rest)
  }

  @throws(classOf[Exception])
  private def writeLines(lines: Array[Array[StyledText]]) {
    lines foreach writeLine
  }

  /**
   * Write a line string to doc, to start a new line afterward, the line string should end with "\n"
   */
  @throws(classOf[Exception])
  private def writeLine(lineTexts: Array[StyledText]) {
    lineParser.parseLine(lineTexts) foreach overwrite
  }

  /**
   * The non teminated text line is usaully the line that is accepting user input, ie. an editable
   * line, we should process '\b' here (JTextPane just print ' ' for '\b')
   * @Note The '\b' from JLine, is actually a back cursor, which works with
   * followed overwriting in combination for non-ansi terminal to move cursor.
   */
  @throws(classOf[Exception])
  private def writeNonLineTeminatedText(text: String) {
    val len = text.length
    var i = 0
    while (i < len) {
      text.charAt(i) match {
        case '\b' =>
          backCursor(1)
        case c =>
          overwrite(StyledText("" + c, currStyle))
      }
      i += 1
    }
  }

  @throws(classOf[Exception])
  private def backCursor(num: Int) {
    val backNum = math.min(area.getCaretPosition, num)
    area.setCaretPosition(area.getCaretPosition - backNum)
  }

  /**
   * @param string to overwrite
   * @param style
   */
  @throws(classOf[Exception])
  private def overwrite(text: StyledText) {
    val from = area.getCaretPosition
    val overwriteLen = math.min(doc.getLength - from, text.text.length)
    doc.remove(from, overwriteLen)
    doc.insertString(from, text.text, text.style)
  }

  protected def replaceText(start: Int, end: Int, replacement: String) {
    try {
      doc.remove(start, end - start)
      doc.insertString(start, replacement, currStyle)
    } catch {
      case ex: Throwable => // Ifnore
    }
  }

  /**
   * @see getLastLine
   */
  private def getInputingLine = {
    stripEndingCR(getLastLine)
  }

  /**
   * A document is modelled as a list of lines (Element)=> index = line number
   *
   * @return the text at the last line, @Note the doc's always ending with a '\n' even there was never printing of this CR?
   */
  private def getLastLine: String = {
    val root = doc.getDefaultRootElement
    val numLines = root.getElementCount
    val line = root.getElement(numLines - 1)
    try {
      doc.getText(line.getStartOffset, line.getEndOffset - line.getStartOffset)
    } catch {
      case ex: Exception => ""
    }
  }

  private def stripEndingCR(line: String): String = {
    val len = line.length
    if (len > 0) {
      if (line.charAt(len - 1) == '\n' || line.charAt(len - 1) == '\r') {
        if (len > 1 && (line.charAt(len - 2) == '\n' || line.charAt(len - 2) == '\r')) {
          line.substring(0, len - 2)
        } else {
          line.substring(0, len - 1)
        }
      } else {
        line
      }
    } else {
      line
    }
  }

  private def getTextFrom(offset: Int): String = {
    try {
      doc.getText(offset, doc.getLength - offset)
    } catch {
      case ex: Exception => ""
    }
  }

  // --- complete actions

  protected def completePopupAction(endAction: ConsoleCapturer.EndAction) {
    val text = endAction.lines
    if (text.trim == "{invalid input}") {
      return
    }

    val candidates = text.split("\\s+") filter (_.length > 0)
    if (candidates.length > 1) {
      completer.popup.getList.setVisibleRowCount(math.min(10, candidates.length))
      completer.combo.removeAllItems
      candidates foreach completer.combo.addItem

      val pos = area.getCaretPosition
      if (pos >= 0) {
        completer.invokeOffset = pos
        val rec = area.modelToView(pos)
        completer.popup.show(area, rec.x, rec.y + area.getFontMetrics(area.getFont).getHeight)
      }
    }
  }

  protected def completeIncrementalAction(endAction: ConsoleCapturer.EndAction) {
    val pos = area.getCaretPosition
    if (pos >= 0) {
      val input = getInputingLine
      val candidates = new mutable.ArrayBuffer[String]()
      val count = completer.combo.getItemCount
      var i = 0
      while (i < count) {
        val candidate = completer.combo.getItemAt(i)
        val matchedLength = completer.matches(input, candidate)
        if (matchedLength >= 0) {
          candidates += candidate
        }
        i += 1
      }

      completer.combo.removeAllItems
      if (candidates.length > 0) {
        completer.popup.getList.setVisibleRowCount(math.min(10, candidates.length))
        candidates foreach completer.combo.addItem
        val rec = area.modelToView(pos)
        completer.popup.show(area, rec.x, rec.y + area.getFontMetrics(area.getFont).getHeight)
      } else {
        completer.popup.setVisible(false)
      }
    } else {
      completer.popup.setVisible(false)
    }
  }

  protected def completeSelectedAction(evt: KeyEvent) {
    completer.combo.getSelectedItem match {
      case selectedText: String =>
        val pos = area.getCaretPosition
        if (pos >= 0) {
          val input = getInputingLine
          val matchedLength = completer.matches(input, selectedText)
          if (matchedLength >= 0) {
            val toAppend = selectedText.substring(matchedLength, selectedText.length)
            if (toAppend.length > 0) {
              terminalInput.write(toAppend.getBytes("utf-8"))
            }
          }
        }
      case _ =>
    }
  }

  protected def completeUpSelectAction(evt: KeyEvent) {
    val selected = completer.combo.getSelectedIndex - 1
    if (selected >= 0) {
      completer.combo.setSelectedIndex(selected)
    } else {
      if (completer.combo.getItemCount > 0) {
        completer.combo.setSelectedIndex(completer.combo.getItemCount - 1)
      }
    }
  }

  protected def completeDownSelectAction(evt: KeyEvent) {
    val selected = completer.combo.getSelectedIndex + 1
    if (selected < completer.combo.getItemCount) {
      completer.combo.setSelectedIndex(selected)
    } else {
      if (completer.combo.getItemCount > 0) {
        completer.combo.setSelectedIndex(0)
      }
    }
  }

  class ConsoleTerminalInput extends TerminalInput with KeyListener {
    import KeyEvent._

    def invokeCompleteAction {
      outputCapturer.hideOutput capture completePopupAction
      keyTyped(0, VK_TAB, 0)
    }

    override def write(b: Array[Byte]) {
      pipedOut.write(b)
    }

    override def keyReleased(evt: KeyEvent) {
      evt.consume
    }

    override def keyPressed(evt: KeyEvent) {
      val keyCode = evt.getKeyCode

      // --- complete visiblilty
      if (completer.popup.isVisible) {
        keyCode match {
          case VK_TAB =>
          // ignore it
          case VK_UP =>
            completeUpSelectAction(evt)
          case VK_DOWN =>
            completeDownSelectAction(evt)
          case VK_ENTER  =>
          // terminalInput process VK_ENTER in keyTyped, so keep completePopup visible here
          case VK_ESCAPE =>
          // terminalInput process VK_ESCAPE in keyTyped, so keep completePopup visible here
          case _ if isPrintableChar(evt.getKeyChar) =>
            // may be under incremental complete
            keyPressed(evt.getKeyCode, evt.getKeyChar, getModifiers(evt))
          case _ =>
            if (!(evt.isControlDown || evt.isAltDown || evt.isMetaDown || evt.isShiftDown)) {
              completer.popup.setVisible(false)
            }
            keyPressed(evt.getKeyCode, evt.getKeyChar, getModifiers(evt))
        }
      } else {
        keyPressed(evt.getKeyCode, evt.getKeyChar, getModifiers(evt))
      }

      // --- evt consumes
      keyCode match {
        case VK_C if evt.isMetaDown | evt.isControlDown => // copy action (@Note Ctrl+A is used to move to line begin)
        case VK_V if evt.isMetaDown | evt.isControlDown => // paste action
          // for console, only paste at the end is meaningful. Anyway, just write them to terminalInput dicarding the caret position
          val data = Toolkit.getDefaultToolkit.getSystemClipboard.getData(DataFlavor.stringFlavor).asInstanceOf[String]
          terminalInput.write(data.getBytes("utf-8"))
          evt.consume
        case _ =>
          evt.consume
      }
    }

    override def keyTyped(evt: KeyEvent) {
      // under keyTyped, always use evt.getKeyChar
      val keyChar = evt.getKeyChar

      if (completer.popup.isVisible) {
        keyChar match {
          case VK_TAB =>
          // ignore it

          case VK_ENTER =>
            completeSelectedAction(evt)
            completer.popup.setVisible(false)

          case VK_ESCAPE =>
            completer.popup.setVisible(false)

          case _ if isPrintableChar(keyChar) =>
            outputCapturer capture completeIncrementalAction
            keyTyped(evt.getKeyCode, evt.getKeyChar, getModifiers(evt))

          case _ =>
            keyTyped(evt.getKeyCode, evt.getKeyChar, getModifiers(evt))
        }
      } else {
        keyChar match {
          case VK_TAB =>
            if (!outputCapturer.isCapturing) {
              outputCapturer.hideOutput capture completePopupAction // do completion
              keyTyped(evt.getKeyCode, evt.getKeyChar, getModifiers(evt))
            }

          case VK_ESCAPE =>
          // ignore it. Under sbt console, <escape> followed by <backspace> behaves strange

          case _ =>
            keyTyped(evt.getKeyCode, evt.getKeyChar, getModifiers(evt))
        }

      }

      evt.consume // consume it, will be handled by echo etc
    }

    private def getModifiers(e: KeyEvent): Int = {
      (if (e.isControlDown) TerminalInput.KEY_CONTROL else 0) |
        (if (e.isShiftDown) TerminalInput.KEY_SHIFT else 0) |
        (if (e.isAltDown) TerminalInput.KEY_ALT else 0) |
        (if (e.isActionKey) TerminalInput.KEY_ACTION else 0)
    }

    private def isPrintableChar(c: Char): Boolean = {
      val block = Character.UnicodeBlock.of(c)

      !Character.isISOControl(c) &&
        c != KeyEvent.CHAR_UNDEFINED &&
        block != null &&
        block != Character.UnicodeBlock.SPECIALS
    }
  }
}

class AnsiConsoleOutputStream(term: ConsoleTerminal) extends AnsiOutputStream(term) {
  import AnsiConsoleOutputStream._

  private val area = term.area
  private val doc = area.getDocument.asInstanceOf[StyledDocument]

  override protected def processSetForegroundColor(color: Int) {
    StyleConstants.setForeground(term.sequenceStyle, ANSI_COLOR_MAP(color))
    term.currStyle = term.sequenceStyle
  }

  override protected def processSetBackgroundColor(color: Int) {
    StyleConstants.setBackground(term.sequenceStyle, ANSI_COLOR_MAP(color))
    term.currStyle = term.sequenceStyle
  }

  override protected def processDefaultTextColor {
    StyleConstants.setForeground(term.sequenceStyle, term.defaultStyle.getAttribute(StyleConstants.Foreground).asInstanceOf[Color])
    term.currStyle = term.sequenceStyle
  }

  override protected def processDefaultBackgroundColor {
    StyleConstants.setBackground(term.sequenceStyle, term.defaultStyle.getAttribute(StyleConstants.Background).asInstanceOf[Color])
    term.currStyle = term.sequenceStyle
  }

  override protected def processSetAttribute(attribute: Int) {
    import Ansi._

    attribute match {
      case ATTRIBUTE_CONCEAL_ON       =>
      //write("\u001B[8m")
      //concealOn = true
      case ATTRIBUTE_INTENSITY_BOLD   => StyleConstants.setBold(term.sequenceStyle, true)
      case ATTRIBUTE_INTENSITY_NORMAL => StyleConstants.setBold(term.sequenceStyle, false)
      case ATTRIBUTE_UNDERLINE        => StyleConstants.setUnderline(term.sequenceStyle, true)
      case ATTRIBUTE_UNDERLINE_OFF    => StyleConstants.setUnderline(term.sequenceStyle, false)
      case ATTRIBUTE_NEGATIVE_ON      =>
      case ATTRIBUTE_NEGATIVE_Off     =>
      case _                          =>
    }

    term.currStyle = term.sequenceStyle
  }

  override protected def processAttributeRest() {
    term.currStyle = term.defaultStyle
  }

  // @Note before do any ansi cursor command, we should flush first to keep the
  // proper caret position which is sensitive to the order of ansi command and chars to print
  override protected def processCursorToColumn(screenCol: Int) {
    term.doFlushWith(false) {
      try {
        val (lineOffset, lineEndOffset) = getLineOffsetOfPos(doc, area.getCaretPosition)
        val toPos = lineOffset + screenCol - 1
        if (toPos < lineEndOffset) {
          area.setCaretPosition(toPos)
        } else { // may have to move to previous line
          val (lineOffset1, lineEndOffset1) = getLineOffsetOfPos(doc, lineOffset - 1)
          val toPos1 = lineOffset1 + screenCol - 1
          if (lineOffset1 >= 0 && toPos < lineEndOffset1) {
            area.setCaretPosition(toPos)
          } // else no idea now, do nothing
        }
      } catch {
        case ex: Throwable => log.log(Level.WARNING, ex.getMessage, ex)
      }
    }
  }

  override protected def processCursorLeft(count: Int) {
    term.doFlushWith(false) {
      try {
        val pos = area.getCaretPosition
        if (pos - count >= 0) {
          area.setCaretPosition(pos - count)
        } else {
          area.setCaretPosition(0)
        }
      } catch {
        case ex: Throwable => log.log(Level.WARNING, ex.getMessage, ex)
      }
    }
  }

  override protected def processCursorRight(count: Int) {
    term.doFlushWith(false) {
      try {
        val pos = area.getCaretPosition
        if (pos + count < doc.getLength) {
          area.setCaretPosition(pos + count)
        } else {
          val append = count - (doc.getLength - 1 - pos)
          var i = 0
          while (i < count) {
            out.write(' ')
            i += 1
          }
          area.setCaretPosition(doc.getLength - 1)
        }
      } catch {
        case ex: Throwable => log.log(Level.WARNING, ex.getMessage, ex)
      }
    }
  }

  override protected def processCursorDown(count: Int) {
    term.doFlushWith(false) {
      try {
        val pos = area.getCaretPosition
        val root = doc.getDefaultRootElement
        val lineNo = root.getElementIndex(pos)
        val line = root.getElement(lineNo)
        val col = pos - line.getStartOffset
        val toLineNo = math.min(lineNo + count, root.getElementCount - 1)
        val toLine = root.getElement(toLineNo)
        val toPos = math.min(toLine.getStartOffset + col, toLine.getEndOffset - 1)
        area.setCaretPosition(toPos)
      } catch {
        case ex: Throwable => log.log(Level.WARNING, ex.getMessage, ex)
      }
    }
  }

  override protected def processCursorUp(count: Int) {
    term.doFlushWith(false) {
      try {
        val pos = area.getCaretPosition
        val root = doc.getDefaultRootElement
        val lineNo = root.getElementIndex(pos)
        val line = root.getElement(lineNo)
        val col = pos - line.getStartOffset
        val toLineNo = math.max(lineNo - count, 0)
        val toLine = root.getElement(toLineNo)
        val toPos = math.min(toLine.getStartOffset + col, toLine.getEndOffset - 1)
        area.setCaretPosition(toPos)
      } catch {
        case ex: Throwable => log.log(Level.WARNING, ex.getMessage, ex)
      }
    }
  }

  /**
   * Clears part of the screen. If n is zero (or missing), clear from cursor to
   * end of screen. If n is one, clear from cursor to beginning of the screen.
   * If n is two, clear entire screen (and moves cursor to upper left on MS-DOS ANSI.SYS).
   */
  override protected def processEraseScreen(eraseOption: Int) {
    eraseOption match {
      case 0 =>
        term.doFlushWith(false) {
          try {
            val pos = area.getCaretPosition
            doc.remove(pos, doc.getLength - pos)
          } catch {
            case ex: Throwable => log.log(Level.WARNING, ex.getMessage, ex)
          }
        }
      case _ =>
    }
  }

  /**
   * Erases part of the line. If n is zero (or missing), clear from cursor to
   * the end of the line. If n is one, clear from cursor to beginning of the
   * line. If n is two, clear entire line. Cursor position does not change.
   */
  override protected def processEraseInLine(eraseOption: Int) {
    eraseOption match {
      case 0 => // clear from cursor to the end of the line
        term.doFlushWith(false) {
          try {
            val pos = area.getCaretPosition
            doc.remove(pos, doc.getLength - pos)
          } catch {
            case ex: Throwable => log.log(Level.WARNING, ex.getMessage, ex)
          }
        }

      case 1 => // clear from cursor to beginning of the line
        term.doFlushWith(false) {
          try {
            val pos = area.getCaretPosition
            val root = doc.getDefaultRootElement
            val lineNo = root.getElementIndex(pos)
            val line = root.getElement(lineNo)
            doc.remove(line.getStartOffset, pos - line.getStartOffset)
          } catch {
            case ex: Throwable => log.log(Level.WARNING, ex.getMessage, ex)
          }
        }

      case 2 => // clear entire line, cursor position does not change.
        term.doFlushWith(false) {
          try {
            val pos = area.getCaretPosition
            val root = doc.getDefaultRootElement
            val lineNo = root.getElementIndex(pos)
            val line = root.getElement(lineNo)
            doc.remove(line.getStartOffset, line.getEndOffset - 1 - line.getStartOffset)
            val toPos = math.min(pos, doc.getLength - 1)
            area.setCaretPosition(toPos)
          } catch {
            case ex: Throwable => log.log(Level.WARNING, ex.getMessage, ex)
          }
        }

      case _ =>
    }
  }

  override protected def processReportCursorPosition() {
    val DEFAULT_HEIGHT = 24
    term.doFlushWith(false) {
      try {
        val pos = area.getCaretPosition
        val root = doc.getDefaultRootElement
        val lineNo = root.getElementIndex(pos)
        val line = root.getElement(lineNo)
        val screenCol = pos - line.getStartOffset + 1
        val screenRow = DEFAULT_HEIGHT - ((root.getElementCount - 1 - lineNo) % DEFAULT_HEIGHT)
        val report = "\u001b[" + screenRow + ";" + screenCol + "R"
        term.terminalInput.write(report.getBytes("utf-8"))
      } catch {
        case ex: Throwable => log.log(Level.WARNING, ex.getMessage, ex)
      }
    }
  }

}

object AnsiConsoleOutputStream {
  private val log = Logger.getLogger(getClass.getName)

  val BLACK = new Color(0, 0, 0)
  val RED = new Color(187, 0, 0)
  val GREEN = new Color(0, 187, 0)
  val YELLOW = new Color(187, 187, 0)
  val BLUE = new Color(0, 0, 187)
  val MAGENTA = new Color(187, 0, 187)
  val CYAN = new Color(0, 187, 187)
  val WHITE = new Color(255, 255, 255)

  private val ANSI_COLOR_MAP = Array(BLACK, RED, GREEN, YELLOW, BLUE, MAGENTA, CYAN, WHITE)

  // --- Document utilities

  /**
   * indent the lines between the given document positions (NOT LINE NUMBERS !)
   */
  def indentLines(doc: Document, fromPos: Int, toPos: Int) {
    val lineStart = getLineNumber(doc, fromPos)
    val lineEnd = getLineNumber(doc, toPos)

    var line = lineStart
    while (line <= lineEnd) {
      try {
        val li = doc.getDefaultRootElement.getElement(line)
        doc.insertString(li.getStartOffset, " ", li.getAttributes)
      } catch {
        case ex: Exception => log.warning(ex.getMessage)
      }
      line += 1
    }
  }

  /**
   * Indents the lines between the given document positions (positions, NOT LINE NUMBERS !)
   */
  def unindentLines(doc: Document, fromPos: Int, toPos: Int) {
    val lineStart = getLineNumber(doc, fromPos)
    val lineEnd = getLineNumber(doc, toPos)

    var line = lineStart
    while (line <= lineEnd) {
      try {
        val li = doc.getDefaultRootElement().getElement(line)
        val ci = doc.getText(li.getStartOffset, 1).charAt(0)
        if (Character.isWhitespace(ci) && ci != '\n') {
          doc.remove(li.getStartOffset, 1)
        }
      } catch {
        case ex: Exception => log.warning(ex.getMessage)
      }
      line += 1
    }
  }

  /**
   * comment out the lines between the given document positions (NOT LINE NUMBERS !)
   * comment out means adding "//" at the beginning of the line.
   * the uncommentout is the reverse operation
   */
  def commentOutLines(doc: Document, fromPos: Int, toPos: Int) {
    val lineStart = getLineNumber(doc, fromPos)
    val lineEnd = getLineNumber(doc, toPos)

    var line = lineStart
    while (line <= lineEnd) {
      try {
        val li = doc.getDefaultRootElement.getElement(line);
        doc.insertString(li.getStartOffset, "//", li.getAttributes)
      } catch {
        case ex: Exception => log.warning(ex.getMessage)
      }
      line += 1
    }
  }

  /**
   * uncomment out the lines between the given document positions (NOT LINE NUMBERS !)
   * reverse operation of commentOut. This only removes // at the beginning
   */
  def unCommentOutLines(doc: Document, fromPos: Int, toPos: Int) {
    val lineStart = getLineNumber(doc, fromPos)
    val lineEnd = getLineNumber(doc, toPos)

    var line = lineStart
    while (line <= lineEnd) {
      try {
        val li = doc.getDefaultRootElement.getElement(line)
        if (li.getEndOffset() - li.getStartOffset > 1) {
          if (doc.getText(li.getStartOffset, 2).equals("//")) {
            doc.remove(li.getStartOffset, 2)
          }
        }
      } catch {
        case ex: Exception => log.warning(ex.getMessage)
      }
      line += 1
    }
  }

  /**
   * @return the line number corresponding to the document position.
   * the first line has index 0.
   */
  def getLineNumber(doc: Document, pos: Int): Int = {
    // a document is modelled as a list of lines (Element)=> index = line number
    val root = doc.getDefaultRootElement
    root.getElementIndex(pos)
  }

  /* not used alone... always together with the line. so we minimize mismatch with offset !
   * @return the column, first is 0
   * use getLineColumnNumbers() to directly receive line and column numbers !
   *
   public static int getColumnNumber_(Document doc, int pos)
   {
   Element map = doc.getDefaultRootElement();
   int line = map.getElementIndex(pos);
   Element lineElt = map.getElement(line);
   return pos-lineElt.getStartOffset();
   }*/

  /**
   * @return the {line,column}, start from 0
   */
  def getLineColumnNumbers(doc: Document, pos: Int): Array[Int] = {
    val root = doc.getDefaultRootElement
    val line = root.getElementIndex(pos)
    val lineElt = root.getElement(line)
    Array(line, pos - lineElt.getStartOffset)
  }

  /**
   * @return (startOffset, endOffset). Line endOffset is always point to '\n'
   */
  def getLineOffsetOfPos(doc: Document, pos: Int): (Int, Int) = {
    // a document is modelled as a list of lines (Element)=> index = line number
    val line = doc match {
      case sDoc: StyledDocument =>
        sDoc.getParagraphElement(pos)
      case _ =>
        val root = doc.getDefaultRootElement
        val lineIdx = root.getElementIndex(pos)
        root.getElement(lineIdx)
    }

    if (line != null) {
      (line.getStartOffset, line.getEndOffset)
    } else {
      (-1, -1)
    }
  }

  @throws(classOf[Exception])
  def deleteLineAtPos(doc: StyledDocument, pos: Int) {
    val line = doc.getParagraphElement(pos)
    doc.remove(line.getStartOffset, line.getEndOffset - 1 - line.getStartOffset)
  }

  /**
   * @return the text at the line containing the given position. With eventual carriage return and line feeds...
   */
  def getTextOfLineAtPosition(doc: StyledDocument, pos: Int): String = {
    // a document is modelled as a list of lines (Element)=> index = line number
    val line = doc.getParagraphElement(pos)
    try {
      doc.getText(line.getStartOffset, line.getEndOffset - 1 - line.getStartOffset)
    } catch {
      case ex: Exception => null
    }
  }

  /**
   * @return the text at the line containing the given position
   */
  def getTextOfLineAtPosition_onlyUpToPos(doc: StyledDocument, pos: Int): String = {
    // a document is modelled as a list of lines (Element)=> index = line number
    val line = doc.getParagraphElement(pos)
    try {
      doc.getText(line.getStartOffset, pos - line.getStartOffset)
    } catch {
      case ex: Exception => null
    }
  }

  /**
   * @return the text at the line containing the given position
   */
  def getTextOfLine(doc: Document, line: Int): String = {
    // a document is modelled as a list of lines (Element)=> index = line number
    val map = doc.getDefaultRootElement
    val lineElt = map.getElement(line)
    try {
      doc.getText(lineElt.getStartOffset, lineElt.getEndOffset - lineElt.getStartOffset)
    } catch {
      case ex: Exception => null
    }
  }

  def getSpacesAtBeginning(text: String): String = {
    val sb = new StringBuilder()
    var break = false
    var i = 0
    while (i < text.length && !break) {
      val ci = text.charAt(i)
      if (Character.isWhitespace(ci) && ci != '\r' && ci != '\n') {
        sb.append(text.charAt(i))
      } else {
        break = true
      }
      i += 1
    }

    sb.toString
  }

  def scrollPosToMiddle(pos: Int, textPane: JTextPane, editorScrollPane: JScrollPane, h: Int) {
    try {
      val r = textPane.modelToView(pos)
      //r.translate(0,-this.getHeight()/4);
      //textPane.scrollRectToVisible( r );
      //this.scroll
      //int h2 = (int)editorScrollPane.getViewport().getExtentSize().getHeight()/2;
      //System.out.println(""+h2);
      editorScrollPane.getViewport().setViewPosition(new Point(0, Math.max(r.y - h, 0)))
      //textPane.requestFocus(); // to make the selection visible

    } catch {
      case ex: Exception =>
    }
  }

  /**
   * in fact, 1/4 below upper limit is nicer...
   * scrolls the start of the line to the middle of the screen
   */
  def scrollToMiddle(tp: JTextComponent, pos: Int) {
    // look if a parent exists, this is only the case IF the text pane has been added in a scrollpane
    if (tp.getParent == null) return
    tp.getParent match {
      case null => return
      case vp: JViewport =>
        try {
          val pt = tp.modelToView(pos)
          var h = (pt.getY - vp.getHeight / 4).toInt
          if (h < 0) h = 0
          vp.setViewPosition(new Point(0, h))
        } catch {
          case ex: Exception => log.warning(ex.getMessage)
        }
      case _ =>
        new Throwable("parent of textpane is not a viewport !").printStackTrace
    }
  }

  def getVisibleDocPosBounds(textPane: JTextPane, editorScrollPane: JScrollPane): Array[Int] = {
    val pos = Array.ofDim[Int](2)
    try {
      val pt1 = editorScrollPane.getViewport.getViewPosition
      pos(0) = textPane.viewToModel(pt1)
      val dim = editorScrollPane.getViewport.getExtentSize

      val pt2 = new Point((pt1.getX + dim.getWidth).toInt, (pt1.getY + dim.getHeight).toInt)
      pos(1) = textPane.viewToModel(pt2)
    } catch {
      case ex: Exception => log.warning(ex.getMessage)
    }
    pos
  }

  /**
   * @param line zero based.
   * @param column zero based
   */
  def getDocPositionFor(doc: Document, line: Int, column: Int): Int = {
    if (line < 0) return -1
    val map = doc.getDefaultRootElement
    val lineElt = map.getElement(line)
    try {
      val pos = lineElt.getStartOffset + (if (column > 0) column else 0)
      if (pos < 0) return 0
      if (pos > doc.getLength) return doc.getLength
      pos
    } catch {
      case ex: Exception => throw new RuntimeException(ex)
    }
  }
}
