package org.netbeans.modules.scala.sbt.console

import java.awt.Color
import java.awt.EventQueue
import java.awt.Point
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.io.IOException
import java.io.OutputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.io.PrintStream
import java.util.logging.Level
import java.util.logging.Logger
import java.util.regex.Pattern
import javax.swing.DefaultListCellRenderer
import javax.swing.JComboBox
import javax.swing.JScrollPane
import javax.swing.JTextPane
import javax.swing.JViewport
import javax.swing.plaf.basic.BasicComboPopup
import javax.swing.text.AbstractDocument
import javax.swing.text.AttributeSet
import javax.swing.text.BadLocationException
import javax.swing.text.Document
import javax.swing.text.DocumentFilter
import javax.swing.text.JTextComponent
import javax.swing.text.SimpleAttributeSet
import javax.swing.text.StyleConstants
import javax.swing.text.StyledDocument
import scala.collection.mutable.ArrayBuffer

/**
 *
 * @author Caoyuan Deng
 */
class ConsoleOutputStream(val area: JTextComponent, welcome: String, pipedIn: PipedInputStream) extends OutputStream {
  import ConsoleOutputStream._
  
  def this(area: JTextComponent) = this(area, null, null)
    
  /** buffer which will be used for the next line */
  private val buf = new StringBuffer(1000)
  private val linesBuf = new ArrayBuffer[String]()
  private var promptPos = 0
  private var currentLine: String = _
  private var isWaitingUserInput = false

  area.setForeground(defaultFg)
  area.setBackground(defaultBg)
  area.setCaretColor(defaultFg)
  val sequenceStyle = new SimpleAttributeSet()
  val defaultStyle  = new SimpleAttributeSet()
  
  val infoStyle = new SimpleAttributeSet()
  val warnStyle = new SimpleAttributeSet()
  val errorStyle = new SimpleAttributeSet()
  val successStyle = new SimpleAttributeSet()
  val linkStyle = new SimpleAttributeSet()
  
  StyleConstants.setForeground(sequenceStyle, defaultFg)     
  StyleConstants.setBackground(sequenceStyle, defaultBg)     
  StyleConstants.setForeground(defaultStyle, defaultFg)     
  StyleConstants.setBackground(defaultStyle, defaultBg)

  StyleConstants.setForeground(infoStyle, defaultFg)
  StyleConstants.setForeground(warnStyle, new Color(0xB9, 0x7C, 0x00))
  StyleConstants.setForeground(errorStyle, Color.RED)
  StyleConstants.setForeground(successStyle, Color.GREEN)

  StyleConstants.setForeground(linkStyle, linkFg)
  StyleConstants.setUnderline(linkStyle, true)
  
  var currentStyle = defaultStyle
  
  private val completeCombo = new JComboBox[String]()
  private var completeStart: Int = _
  private var completeEnd: Int = _
    
  private val pipedOut = new PrintStream(new PipedOutputStream(pipedIn))
  
  private val doc = area.getDocument
  //ConsoleLineReader.createConsoleLineReader
        
  area.setCaret(BlockCaret)
  area.addKeyListener(terminalInput)
  
  private val docFilter = new DocumentFilter() {
    override 
    def insertString(fb: DocumentFilter.FilterBypass, offset: Int, str: String, attr: AttributeSet) {
      if (offset >= promptPos) {
        super.insertString(fb, offset, str, attr)
      }
    }
                
    override 
    def remove(fb: DocumentFilter.FilterBypass, offset: Int, length: Int) {
      if (offset >= promptPos) {
        super.remove(fb, offset, length)
      }
    }
                
    override 
    def replace(fb: DocumentFilter.FilterBypass, offset: Int, length: Int, str: String ,  attrs: AttributeSet) {
      if (offset >= promptPos) {
        super.replace(fb, offset, length, str, attrs)
      }
    }
  }
  
  // No editing before startPos
  doc match {
    case styleDoc: AbstractDocument => //styleDoc.setDocumentFilter(docFilter)
  }
        
  completeCombo.setRenderer(new DefaultListCellRenderer())
  val completePopup = new BasicComboPopup(completeCombo)
        
  if (welcome ne null) {
    val messageStyle = new SimpleAttributeSet()
    StyleConstants.setBackground(messageStyle, area.getForeground)
    StyleConstants.setForeground(messageStyle, area.getBackground)
    overwrite(welcome, messageStyle)
  }
  
  def runSbtCommand(command: String): String = synchronized {
    pipedOut.println(command)
    ""
  }
  
  def exitSbt {
    runSbtCommand("exit")   
  }
 
  @throws(classOf[IOException])
  override
  def close() {
    flush
    handleClose
  }

  /**
   * This method is called when the stream is closed and it allows
   * entensions of this class to do additional tasks.
   * For example, closing an underlying stream, etc.
   * The default implementation does nothing.
   */
  @throws(classOf[IOException])
  protected def handleClose() {}

  @throws(classOf[IOException])
  override
  def write(b: Array[Byte]) {
    write(b, 0, b.length)
  }

  @throws(classOf[IOException])
  override
  def write(b: Array[Byte], offset: Int, length: Int) {
    buf.append(new String(b, offset, length))
  }

  @throws(classOf[IOException])
  override
  def write(b: Int) {
    buf.append(b.toChar)
  }
  
  @throws(classOf[IOException])
  override
  def flush() {
    doFlush()
    isWaitingUserInput = true
  }
  
  @throws(classOf[IOException])
  protected[console] def doFlush(postAction: () => Unit = () => ()) {
    if (EventQueue.isDispatchThread) {
      try {
        writeLines(readLines)
        writeLastLine(readLastLine)
        postAction()
      } catch {
        case ex: Exception => log.log(Level.SEVERE, ex.getMessage, ex)
      }
    } else {
      EventQueue.invokeLater(new Runnable() {
          def run {
            try {
              writeLines(readLines)
              writeLastLine(readLastLine)
              postAction()
            } catch {
              case ex: Exception => log.log(Level.SEVERE, ex.getMessage, ex)
            }
          }
        }
      )
    }
  }
  
  @throws(classOf[IOException])
  private def writeLines(lines: ArrayBuffer[String]) {
    lines foreach writeLine
    lines.clear
  }

  /**
   * Read lines from buf, keep not-line-completed chars in buf
   * @return lines
   */
  private def readLines: ArrayBuffer[String] = {
    val len = buf.length
    var newLineOffset = 0
    var readOffset = -1 // offset that has read to lines
    var i = 0
    while (i < len) {
      if (buf.charAt(i) == '\n') {
        val line = if (i > 0 && buf.charAt(i - 1) == '\r') { 
          buf.substring(newLineOffset, i) + "\n" // strip '\r' for Windows
        } else {
          buf.substring(newLineOffset, i + 1)
        }
        linesBuf += line
        readOffset = i
        newLineOffset = i + 1
      }
      i += 1
    }
    
    if (readOffset >= 0) {
      buf.delete(0, readOffset + 1)
    }
    
    linesBuf
  }
  
  private def readLastLine: String = {
    val line = buf.substring(0, buf.length)
    buf.delete(0, buf.length)
    line
  }
  
  /**
   * Write a line string to doc, to start a new line afterward, the line string should end with "\n"
   */
  private def writeLine(line: String) {
    if (line.length >= 6) {
      for ((text, style) <- parseLine(line)) overwrite(text, style)
    } else {
      overwrite(line, currentStyle)
    }
  }
  
  /**
   * The last line is usaully the line that is accepting user input, ie. an editable 
   * line, we should process '\b' here (JTextPane just print ' ' for '\b')
   * @Note The '\b' from JLine, is actually a back cursor, which works with 
   * followed overwriting in combination for non-ansi terminal to move cursor.
   */
  private def writeLastLine(str: String) {
    val len = str.length
    var i = 0
    while (i < len) {
      str.charAt(i) match {
        case '\b' =>
          backCursor(1)
        case c =>
          overwrite("" + c, currentStyle)
      }
      i += 1
    }
  }
  
  @throws(classOf[BadLocationException])
  private def backCursor(num: Int) {
    val backNum = math.min(area.getCaretPosition, num)
    area.setCaretPosition(area.getCaretPosition - backNum)
  }
  
  /**
   * @param string to overwrite
   * @param style
   */
  @throws(classOf[BadLocationException])
  private def overwrite(str: String, style: AttributeSet) {
    val from = area.getCaretPosition
    val overwriteLen = math.min(doc.getLength - from, str.length)
    doc.remove(from, overwriteLen)
    doc.insertString(from, str, style)
    area.setCaretPosition(from + str.length)
  }
    
  protected def parseLine(line: String): ArrayBuffer[(String, AttributeSet)] = {
    val texts = new ArrayBuffer[(String, AttributeSet)]()
    val testRest_style = if (line.startsWith(ERROR_PREFIX)) {
      
      val m = rERROR_WITH_FILE.matcher(line)
      if (m.matches && m.groupCount >= 3) {
        texts += (("[", currentStyle))
        texts += (("error", errorStyle))
        texts += (("] ", currentStyle))
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
        texts += (("[", currentStyle))
        texts += (("error", errorStyle))
        texts += (("]", currentStyle))
        val textRest = line.substring(ERROR_PREFIX.length, line.length)
        
        (textRest, errorStyle)
      }
      
    } else if (line.startsWith(WARN_PREFIX)) {
      
      val m = rWARN_WITH_FILE.matcher(line)
      if (m.matches && m.groupCount >= 3) {
        texts += (("[", currentStyle))
        texts += (("warn", warnStyle))
        texts += (("] ", currentStyle))
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
        texts += (("[", currentStyle))
        texts += (("warn", warnStyle))
        texts += (("]", currentStyle))
        val textRest = line.substring(WARN_PREFIX.length, line.length)
        
        (textRest, warnStyle)
      }
      
    } else if (line.startsWith(INFO_PREFIX)) {
      
      texts += (("[", currentStyle))
      texts += (("info", infoStyle))
      texts += (("]", currentStyle))
      val textRest = line.substring(INFO_PREFIX.length, line.length)
      
      (textRest, currentStyle)
      
    } else if (line.startsWith(SUCCESS_PREFIX)) {
      
      texts += (("[", currentStyle))
      texts += (("success", successStyle))
      texts += (("]", currentStyle))
      val textRest = line.substring(SUCCESS_PREFIX.length, line.length)
      
      (textRest, currentStyle)
      
    } else {
      (line, currentStyle)
    }
    
    texts += testRest_style
  }
  
  protected def replaceText(start: Int, end: Int, replacement: String) {
    try {
      doc.remove(start, end - start)
      doc.insertString(start, replacement, sequenceStyle)
    } catch {
      case ex: BadLocationException => // Ifnore
    }
  }
    
  protected def getInputLine(): String = {
    try {
      doc.getText(promptPos, doc.getLength - promptPos)
    } catch {
      case ex: BadLocationException => null 
    }
  }
    
  protected def clearInputLine() = {
    try {
      doc.remove(promptPos, doc.getLength - promptPos)
    } catch {
      case ex: BadLocationException =>
    }
  }

  object terminalInput extends TerminalInput with KeyListener {

    System.getProperty("os.name").toLowerCase match {
      case os if os.indexOf("windows") != -1 => terminalId = TerminalInput.JLineWindows
      case _ =>
    }

    override 
    def write(b: Array[Byte]) {
      pipedOut.write(b)
    }
    
    override 
    def keyPressed(evt: KeyEvent) {
      import KeyEvent._

      val consumeIt = evt.getKeyCode match {
        case VK_PAUSE => true
        case VK_F1 | VK_F2 | VK_F3 | VK_F4 | VK_F5 | VK_F6 | VK_F7 | VK_F8 | VK_F9 | VK_F10 | VK_F11 | VK_F12 => true
        case VK_PAGE_DOWN | VK_PAGE_UP | VK_HOME | VK_END => true
        case VK_NUM_LOCK | VK_CAPS_LOCK => true
        case VK_SHIFT | VK_CONTROL | VK_ALT => true
        case VK_INSERT => true
        case VK_DELETE | VK_BACK_SPACE => true
        case VK_LEFT | VK_RIGHT        => true
        
        case VK_UP    => true   // search history
        case VK_DOWN  => true   // search history
        case VK_TAB   => true   // do completion
        case VK_ENTER => true   // enter command
          
        case VK_V => 
          // we need more carefully implementation for Paste action, since should 
          // also move the cursor of backed jline to proper position and send chars in clipboard.
          true
          
        case (VK_C | VK_A) if evt.isMetaDown => false // copy keys
        
        case _ => true
      }
      
      if (consumeIt) {
        evt.consume // consume it, will be handled by echo etc
      }
      keyPressed(evt.getKeyCode, evt.getKeyChar, getModifiers(evt))
    }
  
    override 
    def keyReleased(evt: KeyEvent) {
      evt.consume
    }
    
    override 
    def keyTyped(evt: KeyEvent) {
      evt.consume // consume it, will be handled by echo etc
      keyTyped(evt.getKeyCode, evt.getKeyChar, getModifiers(evt))
    }
    
    private def getModifiers(e: KeyEvent): Int = {
      (if (e.isControlDown) TerminalInput.KEY_CONTROL else 0) |
      (if (e.isShiftDown) TerminalInput.KEY_SHIFT else 0) |
      (if (e.isAltDown) TerminalInput.KEY_ALT else 0) |
      (if (e.isActionKey) TerminalInput.KEY_ACTION else 0)
    }
  }
    
  @deprecated("Need to re-do", "1.6.1")
  object keyListener extends KeyListener {
    override 
    def keyPressed(evt: KeyEvent) {
      val code = evt.getKeyCode
      code match {
        case KeyEvent.VK_TAB        => evt.consume; pipedOut.write("\u0009".getBytes) //completeAction(evt)
        case KeyEvent.VK_LEFT       => backAction(evt)
        case KeyEvent.VK_BACK_SPACE => backAction(evt)
        case KeyEvent.VK_UP         => upAction(evt)
        case KeyEvent.VK_DOWN       => downAction(evt)
        case KeyEvent.VK_ENTER      => enterAction(evt)
        case KeyEvent.VK_HOME       => evt.consume; area.setCaretPosition(promptPos)  
        case _ => // Ignore
      }
        
      if (
        completePopup.isVisible &&
        code != KeyEvent.VK_TAB &&
        code != KeyEvent.VK_UP  &&
        code != KeyEvent.VK_DOWN
      ) {
        completePopup.setVisible(false)
      }
    }
  
    override 
    def keyReleased(event: KeyEvent) {}
    
    override 
    def keyTyped(event: KeyEvent) {}
    
    // ----- mouse actions
    
    private def enterAction(evt: KeyEvent) {
      evt.consume
        
      if (completePopup.isVisible) {
        if (completeCombo.getSelectedItem ne null) {
          replaceText(completeStart, completeEnd, completeCombo.getSelectedItem.asInstanceOf[String])
        }
      
        completePopup.setVisible(false)
        return
      }
        
      val inputStr = getInputLine.trim
      pipedOut.println(inputStr)
      overwrite("\n", null)
        
      //ConsoleLineReader.history.addToHistory(inputStr)
      //ConsoleLineReader.history.moveToEnd
        
      val len = doc.getLength
      area.setCaretPosition(len)
      promptPos = len

//    notify
//        synchronized (inEditing) {
//            inEditing.notify();
//        }
    }
    
    private def backAction(evt: KeyEvent) {
      if (area.getCaretPosition <= promptPos) {
        evt.consume
      }
    }
    
    private def upAction(evt: KeyEvent) {
      evt.consume
      clearInputLine
      pipedOut.write("\033[A".getBytes)
      isWaitingUserInput = true

      if (completePopup.isVisible) {
        val selected = completeCombo.getSelectedIndex - 1
        if (selected < 0) return
        completeCombo.setSelectedIndex(selected)
        return
      }
        
      //if (! ConsoleLineReader.history.next) // at end
      currentLine = getInputLine
      //else
      //  ConsoleLineReader.history.previous // undo check
        
      //if (! ConsoleLineReader.history.previous) {
      //  return
      //}
        
      //val oldLine = ConsoleLineReader.history.current.trim
      //replaceText(startPos, area.getDocument.getLength, oldLine)
    }
    
    private def downAction(evt: KeyEvent) {
      evt.consume
      clearInputLine
      pipedOut.write("\033[B".getBytes)
      isWaitingUserInput = true
        
      if (completePopup.isVisible) {
        val selected = completeCombo.getSelectedIndex + 1
        if (selected == completeCombo.getItemCount) return
        completeCombo.setSelectedIndex(selected)
        return
      }
        
      //if (! ConsoleLineReader.history.next) {
      //  return
      //}
        
      val oldLine = //if (! ConsoleLineReader.history.next) // at end
        currentLine
      //else {
      //  ConsoleLineReader.history.previous // undo check
      //  ConsoleLineReader.history.current.trim
      //}
        
      //replaceText(startPos, area.getDocument.getLength, oldLine)
    }
    
    private def tabAction(evt: KeyEvent) {
      //if (ConsoleLineReader.completor eq null) {
      //  return
      //}
        
      evt.consume
        
      if (completePopup.isVisible) return
        
      val candidates = new ArrayBuffer[String]()
      val bufstr = try {
        area.getText(promptPos, area.getCaretPosition - promptPos)
      } catch  {
        case ex: BadLocationException => return
      }
        
      val cursor = area.getCaretPosition - promptPos
        
      //val position = ConsoleLineReader.completor.complete(bufstr, cursor, candidates)
        
      // no candidates? Fail.
      if (candidates.isEmpty) {
        return
      }
        
      //if (candidates.size == 1) {
      //  replaceText(startPos + position, area.getCaretPosition, candidates(0))
      //  return
      //}
        
      completeStart = promptPos //+ position
      completeEnd = area.getCaretPosition
        
      val pos = area.getCaret.getMagicCaretPosition
        
      // bit risky if someone changes completor, but useful for method calls
      val cutoff = 0//bufstr.substring(position).lastIndexOf('.') + 1
      completeStart += cutoff
        
      val candicateSize = math.max(10, candidates.size)
      completePopup.getList.setVisibleRowCount(candicateSize)
        
      completeCombo.removeAllItems
      if (cutoff != 0) {
        candidates foreach {item => completeCombo.addItem(item.substring(cutoff))}
      } else {
        candidates foreach {item => completeCombo.addItem(item)}
      }
        
      completePopup.show(area, pos.x, pos.y + area.getFontMetrics(area.getFont).getHeight)
    }
    
  }
    
}

object ConsoleOutputStream {
  private val log = Logger.getLogger(getClass.getName)
  
  val defaultFg = Color.BLACK
  val defaultBg = Color.WHITE
  val linkFg = Color.BLUE

  private val INFO_PREFIX    = "[info]"
  private val WARN_PREFIX    = "[warn]"
  private val ERROR_PREFIX   = "[error]"
  private val SUCCESS_PREFIX = "[success]"
  
  private val WINDOWS_DRIVE = "(?:[a-zA-Z]\\:)?"
  private val FILE_CHAR = "[^\\[\\]\\:\\\"]" // not []:", \s is allowd
  private val FILE = "(" + WINDOWS_DRIVE + "(?:" + FILE_CHAR + "*))"
  private val LINE = "(([1-9][0-9]*))"  // line number
  private val ROL = ".*\\s?"            // rest of line
  private val SEP = "\\:"               // seperator between file path and line number
  private val STD_SUFFIX = FILE + SEP + LINE + ROL  // ((?:[a-zA-Z]\:)?(?:[^\[\]\:\"]*))\:(([1-9][0-9]*)).*\s?
  
  private val rERROR_WITH_FILE = Pattern.compile("\\Q" + ERROR_PREFIX + "\\E" + "\\s?" + STD_SUFFIX) // \Q[error]\E\s?((?:[a-zA-Z]\:)?(?:[^\[\]\:\"]*))\:(([1-9][0-9]*)).*\s?
  private val rWARN_WITH_FILE =  Pattern.compile("\\Q" + WARN_PREFIX  + "\\E" + "\\s?" + STD_SUFFIX) //  \Q[warn]\E\s?((?:[a-zA-Z]\:)?(?:[^\[\]\:\"]*))\:(([1-9][0-9]*)).*\s?

}

class AnsiConsoleOutputStream(term: ConsoleOutputStream) extends AnsiOutputStream(term) {
  import AnsiConsoleOutputStream._
  
  private val area = term.area
  private val doc = area.getDocument.asInstanceOf[StyledDocument]
  
  override
  protected def processSetForegroundColor(color: Int) {
    StyleConstants.setForeground(term.sequenceStyle, ANSI_COLOR_MAP(color))
    term.currentStyle = term.sequenceStyle
  }

  override
  protected def processSetBackgroundColor(color: Int) {
    StyleConstants.setBackground(term.sequenceStyle, ANSI_COLOR_MAP(color))
    term.currentStyle = term.sequenceStyle
  }
  
  override
  protected def processDefaultTextColor {
    StyleConstants.setForeground(term.sequenceStyle, ConsoleOutputStream.defaultFg)
    term.currentStyle = term.sequenceStyle
  }

  override
  protected def processDefaultBackgroundColor {
    StyleConstants.setBackground(term.sequenceStyle, ConsoleOutputStream.defaultBg)
    term.currentStyle = term.sequenceStyle
  }

  override
  protected def processSetAttribute(attribute: Int) {
    import Ansi._
    
    attribute match {
      case ATTRIBUTE_CONCEAL_ON =>
        //write("\u001B[8m")
        //concealOn = true
      case ATTRIBUTE_INTENSITY_BOLD =>
        StyleConstants.setBold(term.sequenceStyle, true)
      case ATTRIBUTE_INTENSITY_NORMAL =>
        StyleConstants.setBold(term.sequenceStyle, false)
      case ATTRIBUTE_UNDERLINE =>
        StyleConstants.setUnderline(term.sequenceStyle, true)
      case ATTRIBUTE_UNDERLINE_OFF =>
        StyleConstants.setUnderline(term.sequenceStyle, false)
      case ATTRIBUTE_NEGATIVE_ON =>
      case ATTRIBUTE_NEGATIVE_Off =>
      case _ =>
    }
    
    term.currentStyle = term.sequenceStyle
  }
	
  override
  protected def processAttributeRest() {
    term.currentStyle = term.defaultStyle
  }
  
  // @Note before do any ansi cursor command, we should flush first to keep the 
  // proper caret position which is sensitive to the order of ansi command and chars to print
  @throws(classOf[BadLocationException])
  override 
  protected def processCursorToColumn(col: Int) {
    term.doFlush{() => 
      val lineStart = getLineStartOffsetForPos(doc, area.getCaretPosition)
      val toPos = lineStart + col - 1
      area.setCaretPosition(toPos)
    }
  }
  
  /**
   * Clears part of the screen. If n is zero (or missing), clear from cursor to 
   * end of screen. If n is one, clear from cursor to beginning of the screen. 
   * If n is two, clear entire screen (and moves cursor to upper left on MS-DOS ANSI.SYS).
   */
  @throws(classOf[BadLocationException])
  override 
  protected def processEraseScreen(eraseOption: Int) {
    eraseOption match {
      case 0 => 
        term.doFlush{() =>
          val currPos = area.getCaretPosition
          doc.remove(currPos, doc.getLength - currPos)
        }
      case _ =>
    }
  }

}

object AnsiConsoleOutputStream {
  private val log = Logger.getLogger(getClass.getName)
  
  private val ANSI_COLOR_MAP = Array(
    Color.BLACK, Color.RED, Color.GREEN, Color.YELLOW, Color.BLUE, Color.MAGENTA, Color.CYAN, Color.WHITE
  )
  
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
        doc.insertString(li.getStartOffset(), " ", li.getAttributes());
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
    val lineStart = getLineNumber( doc, fromPos);
    val lineEnd = getLineNumber( doc, toPos );

    var line = lineStart
    while (line <= lineEnd) {
      try {
        val li = doc.getDefaultRootElement().getElement(line);
        val ci = doc.getText(li.getStartOffset(), 1).charAt(0);
        if(Character.isWhitespace(ci) && ci !='\n') {
          doc.remove(li.getStartOffset(), 1)
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
    val lineStart = getLineNumber( doc, fromPos);
    val lineEnd = getLineNumber( doc, toPos );

    var line = lineStart
    while (line <= lineEnd) {
      try {
        val li = doc.getDefaultRootElement().getElement(line);
        doc.insertString(li.getStartOffset(), "//", li.getAttributes());
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
    val lineStart = getLineNumber( doc, fromPos);
    val lineEnd = getLineNumber( doc, toPos );

    var line = lineStart
    while (line <= lineEnd) {
      try {
        val li = doc.getDefaultRootElement.getElement(line)
        if(li.getEndOffset()-li.getStartOffset()>1) {
          if(doc.getText(li.getStartOffset(), 2).equals("//"))
          {
            doc.remove(li.getStartOffset(), 2);
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
    val rrot = doc.getDefaultRootElement
    rrot.getElementIndex(pos)
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
   * @return the {line,column}, first is 0
   */
  def getLineColumnNumbers(doc: Document, pos: Int): Array[Int] = {
    val root = doc.getDefaultRootElement
    val line = root.getElementIndex(pos)
    val lineElt = root.getElement(line)
    Array(line, pos - lineElt.getStartOffset)
  }

  def getLineStartOffsetForPos(doc: Document, pos: Int): Int = {
    // a document is modelled as a list of lines (Element)=> index = line number
    doc match {
      case sDoc: StyledDocument =>
        val line = sDoc.getParagraphElement(pos)
        line.getStartOffset
      case _ =>
        val root = doc.getDefaultRootElement
        val line = root.getElementIndex(pos)
        val lineElt = root.getElement(line)
        lineElt.getStartOffset
    }
  }

  @throws(classOf[Exception])
  def deleteLineAtPos(doc: StyledDocument, pos: Int) {
    val line = doc.getParagraphElement(pos)
    doc.remove(line.getStartOffset, line.getEndOffset - line.getStartOffset)
  }

  /** 
   * @return the text at the line containing the given position. With eventual carriage return and line feeds...
   */
  def getTextOfLineAtPosition(doc: StyledDocument, pos: Int): String = {
    // a document is modelled as a list of lines (Element)=> index = line number
    val line = doc.getParagraphElement(pos);
    try {
      doc.getText(line.getStartOffset(), line.getEndOffset()-line.getStartOffset());
    } catch {
      case ex: Exception => null
    }
  }

  /** @return the text at the line containing the given position
   */
  def getTextOfLineAtPosition_onlyUpToPos(doc: StyledDocument, pos: Int): String = {
    // a document is modelled as a list of lines (Element)=> index = line number
    val line = doc.getParagraphElement(pos)
    try {
      doc.getText(line.getStartOffset(), pos-line.getStartOffset());
    } catch {
      case ex: Exception => null
    }
  }

  /** @return the text at the line containing the given position
   */
  def getTextOfLine(doc: Document, line: Int): String = {
    // a document is modelled as a list of lines (Element)=> index = line number
    val map = doc.getDefaultRootElement();
    val lineElt = map.getElement(line);
    try {
      doc.getText(lineElt.getStartOffset(), lineElt.getEndOffset()-lineElt.getStartOffset());
    } catch {
      case ex: Exception => null
    }
  }

  def getSpacesAtBeginning(text: String): String = {
    val sb = new StringBuilder()
    var break = false
    var i = 0
    while (i < text.length && !break) {
      val ci = text.charAt(i);
      if (Character.isWhitespace(ci) && ci!='\r' && ci!='\n') {
        sb.append(text.charAt(i))
      } else {
        break = true
      }
      i += 1
    }
    
    sb.toString
  }


  def scrollPosToMiddle(pos: Int, textPane: JTextPane, editorScrollPane: JScrollPane, h: Int) {
    try
    {
      val r = textPane.modelToView(pos)
      //r.translate(0,-this.getHeight()/4);
      //textPane.scrollRectToVisible( r );
      //this.scroll
      //int h2 = (int)editorScrollPane.getViewport().getExtentSize().getHeight()/2;
      //System.out.println(""+h2);
      editorScrollPane.getViewport().setViewPosition(new Point(0, Math.max(r.y - h, 0)) )
      //textPane.requestFocus(); // to make the selection visible

    } catch {
      case ex: Exception =>
    }
  }


  /** in fact, 1/4 below upper limit is nicer...
   scrolls the start of the line to the middle of the screen
   */
  def scrollToMiddle(tp: JTextComponent, pos: Int) {
    // look if a parent exists, this is only the case IF the text pane has been added in a scrollpane
    if (tp.getParent == null) return
    tp.getParent match {
      case null => return
      case vp: JViewport =>
        try {
          val pt = tp.modelToView(pos);
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

  /** @param line zero based.
   * @param column zero based
   */
  def getDocPositionFor(doc: Document, line: Int, column: Int): Int = {
    if (line < 0) return -1;
    val map = doc.getDefaultRootElement
    val lineElt = map.getElement(line)
    try {
      val pos = lineElt.getStartOffset + (if (column > 0) column else 0)
      if (pos < 0) return 0
      if (pos > doc.getLength) return doc.getLength
      pos
    } catch {
      case ex: Exception => throw new RuntimeException(ex)}
  }
}