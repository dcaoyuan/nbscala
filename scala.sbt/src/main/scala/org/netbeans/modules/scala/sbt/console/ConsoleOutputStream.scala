package org.netbeans.modules.scala.sbt.console

import java.awt.Color
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.io.IOException
import java.io.OutputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.io.PrintStream
import java.util.regex.Pattern
import javax.swing.DefaultListCellRenderer
import javax.swing.JComboBox
import javax.swing.plaf.basic.BasicComboPopup
import javax.swing.text.AbstractDocument
import javax.swing.text.AttributeSet
import javax.swing.text.BadLocationException
import javax.swing.text.DocumentFilter
import javax.swing.text.JTextComponent
import javax.swing.text.SimpleAttributeSet
import javax.swing.text.StyleConstants
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.SyncVar

/**
 *
 * @author Caoyuan Deng
 */
class ConsoleOutputStream(area: JTextComponent, welcome: String, pipedIn: PipedInputStream) extends OutputStream {
  import ConsoleOutputStream._
  
  def this(area: JTextComponent) = this(area, null, null)
    
  /** buffer which will be used for the next line */
  private val buf = new StringBuffer(1000)
  private val captureBuf = new StringBuffer(1000)
  private val notUnderCaptureOut = {val x = new SyncVar[Boolean]; x.put(true); x}
  private var startPos = 0
  private var currentLine: String = _

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
        
  area.addKeyListener(MyKeyListener)
        
  // No editing before startPos
  doc match {
    case styleDoc: AbstractDocument =>
      styleDoc.setDocumentFilter(new DocumentFilter() {
          override 
          def insertString(fb: DocumentFilter.FilterBypass, offset: Int, str: String, attr: AttributeSet) {
            if (offset >= startPos) super.insertString(fb, offset, str, attr)
          }
                
          override 
          def remove(fb: DocumentFilter.FilterBypass, offset: Int, length: Int) {
            if (offset >= startPos) super.remove(fb, offset, length)
          }
                
          override 
          def replace(fb: DocumentFilter.FilterBypass, offset: Int, length: Int, str: String ,  attrs: AttributeSet) {
            if (offset >= startPos) super.replace(fb, offset, length, str, attrs)
          }
        })
  }
        
  completeCombo.setRenderer(new DefaultListCellRenderer())
  val completePopup = new BasicComboPopup(completeCombo)
        
  if (welcome ne null) {
    val messageStyle = new SimpleAttributeSet()
    StyleConstants.setBackground(messageStyle, area.getForeground)
    StyleConstants.setForeground(messageStyle, area.getBackground)
    append(welcome, messageStyle)
  }
  
  def runSbtCommand(command: String): String = {
    notUnderCaptureOut.take // take away, also means setting to underCaptureOut
    pipedOut.println(command)
    notUnderCaptureOut.get  // wait here until notUnderCaptureOut is put again
    
    val out = captureBuf.toString
    captureBuf.delete(0, captureBuf.length)
    out
  }
  
  def exitSbt {
    pipedOut.println("exit")   
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
  def flush() {
    flushLines(true)
  }

  @throws(classOf[IOException])
  override
  def write(b: Array[Byte]) {
    write(b, 0, b.length)
  }

  @throws(classOf[IOException])
  override
  def write(b: Array[Byte], offset: Int, length: Int) {
    buf.append(new String(b, offset, length))
    // will usually contain at least one newline
    flushLines(false)
  }

  @throws(classOf[IOException])
  override
  def write(b: Int) {
    buf.append(b.toChar)
    if (b.toChar == '\n') {
      flushLines(false)
    }
  }

  @throws(classOf[IOException])
  private def flushLines(flushEverything: Boolean) {
    var breakMain = false
    while (!breakMain) {
      var breakInner = false
      val len = buf.length
      var i = 0
      while (i < len && !breakInner) {
        if (buf.charAt(i) == '\n') {
          val end = if (i > 0 && buf.charAt(i - 1) == '\r') { // for Windows
            i - 1
          } else {
            i
          }
          val line = buf.substring(0, end) + "\n"
          flushLine(line)
          buf.delete(0, i + 1)
          breakInner = true
        }
        i += 1
      }
      breakMain = true
    }
    
    if (flushEverything) {
      flushLine(buf.substring(0, buf.length))
      buf.delete(0, buf.length)
    }
  }
    
  @throws(classOf[IOException])
  private def flushLine(line: String) {
    writeLine(line)
  }
  
  private def writeLine(line: String) {
    if (!notUnderCaptureOut.isSet) {
      if (line != "> ") {
        captureBuf.append(line)
      } else {
        notUnderCaptureOut.put(true)
      }
    } else {
      for ((text, style) <- parseLine(line)) append(text, style)
      startPos = doc.getLength
      area.setCaretPosition(startPos)
    }
  }
  
  private def append(str: String, style: AttributeSet) {
    try {
      doc.insertString(doc.getLength, str, style)
    } catch  {
      case ex: BadLocationException => // just ignore
    }
  }
  
  protected def parseLine(line: String): ArrayBuffer[(String, AttributeSet)] = {
    val texts = new ArrayBuffer[(String, AttributeSet)]()
    val (textRest, style) = if (line.startsWith(ERROR_PREFIX)) {
      
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
    
    texts += ((textRest, style))
    texts
  }
  
  // ----- mouse actions
  
  protected def tabAction(event: KeyEvent) {
    //if (ConsoleLineReader.completor eq null) {
    //  return
    //}
        
    event.consume
        
    if (completePopup.isVisible) return
        
    val candidates = new ArrayBuffer[String]()
    val bufstr = try {
      area.getText(startPos, area.getCaretPosition - startPos)
    } catch  {
      case ex: BadLocationException => return
    }
        
    val cursor = area.getCaretPosition - startPos
        
    //val position = ConsoleLineReader.completor.complete(bufstr, cursor, candidates)
        
    // no candidates? Fail.
    if (candidates.isEmpty) {
      return
    }
        
    //if (candidates.size == 1) {
    //  replaceText(startPos + position, area.getCaretPosition, candidates(0))
    //  return
    //}
        
    completeStart = startPos //+ position
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
    
  protected def backAction(event: KeyEvent) {
    if (area.getCaretPosition <= startPos) {
      event.consume
    }
  }
    
  protected def upAction(event: KeyEvent) {
    event.consume
    val resp = runSbtCommand("eclipse")
    println(resp)
    if (completePopup.isVisible()) {
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
    
  protected def downAction(event: KeyEvent) {
    event.consume
        
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
      area.getText(startPos, doc.getLength - startPos)
    } catch {
      case ex: BadLocationException => null // Ifnore
    }
  }
    
  protected def enterAction(event: KeyEvent) {
    event.consume
        
    if (completePopup.isVisible) {
      if (completeCombo.getSelectedItem ne null) {
        replaceText(completeStart, completeEnd, completeCombo.getSelectedItem.asInstanceOf[String])
      }
      
      completePopup.setVisible(false)
      return
    }
        
    append("\n", null)
        
    val inputStr = getInputLine.trim
    pipedOut.println(inputStr)
        
    //ConsoleLineReader.history.addToHistory(inputStr)
    //ConsoleLineReader.history.moveToEnd
        
    val len = doc.getLength
    area.setCaretPosition(len)
    startPos = len

//    notify
//        synchronized (inEditing) {
//            inEditing.notify();
//        }
  }
  
  object MyKeyListener extends KeyListener {
    override 
    def keyPressed(event: KeyEvent) {
      val code = event.getKeyCode
      code match {
        case KeyEvent.VK_TAB        => //completeAction(event)
        case KeyEvent.VK_LEFT       => backAction(event)
        case KeyEvent.VK_BACK_SPACE => backAction(event)
        case KeyEvent.VK_UP         => upAction(event)
        case KeyEvent.VK_DOWN       => downAction(event)
        case KeyEvent.VK_ENTER      => enterAction(event)
        case KeyEvent.VK_HOME       => event.consume; area.setCaretPosition(startPos)  
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
  }
    
}

object ConsoleOutputStream {
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

class AnsiConsoleOutputStream(os: ConsoleOutputStream) extends AnsiOutputStream(os) {
  import AnsiConsoleOutputStream._
  
  @throws(classOf[IOException])
  override
  protected def processSetForegroundColor(color: Int) {
    StyleConstants.setForeground(os.sequenceStyle, ANSI_COLOR_MAP(color))
    os.currentStyle = os.sequenceStyle
  }

  @throws(classOf[IOException])
  override
  protected def processSetBackgroundColor(color: Int) {
    StyleConstants.setBackground(os.sequenceStyle, ANSI_COLOR_MAP(color))
    os.currentStyle = os.sequenceStyle
  }
  
  @throws(classOf[IOException])
  override
  protected def processDefaultTextColor {
    StyleConstants.setForeground(os.sequenceStyle, ConsoleOutputStream.defaultFg)
    os.currentStyle = os.sequenceStyle
  }

  @throws(classOf[IOException])
  override
  protected def processDefaultBackgroundColor {
    StyleConstants.setBackground(os.sequenceStyle, ConsoleOutputStream.defaultBg)
    os.currentStyle = os.sequenceStyle
  }

  @throws(classOf[IOException])
  override
  protected def processSetAttribute(attribute: Int) {
    import Ansi._
    
    attribute match {
      case ATTRIBUTE_CONCEAL_ON =>
        //write("\u001B[8m")
        //concealOn = true
      case ATTRIBUTE_INTENSITY_BOLD =>
        StyleConstants.setBold(os.sequenceStyle, true)
      case ATTRIBUTE_INTENSITY_NORMAL =>
        StyleConstants.setBold(os.sequenceStyle, false)
      case ATTRIBUTE_UNDERLINE =>
        StyleConstants.setUnderline(os.sequenceStyle, true)
      case ATTRIBUTE_UNDERLINE_OFF =>
        StyleConstants.setUnderline(os.sequenceStyle, false)
      case ATTRIBUTE_NEGATIVE_ON =>
      case ATTRIBUTE_NEGATIVE_Off =>
      case _ =>
    }
    
    os.currentStyle = os.sequenceStyle
  }
	
  @throws(classOf[IOException])
  override
  protected def processAttributeRest() {
    os.currentStyle = os.defaultStyle
  }
}

object AnsiConsoleOutputStream {
  private val ANSI_COLOR_MAP = Array(
    Color.BLACK, Color.RED, Color.GREEN, Color.YELLOW, Color.BLUE, Color.MAGENTA, Color.CYAN, Color.WHITE
  )
}