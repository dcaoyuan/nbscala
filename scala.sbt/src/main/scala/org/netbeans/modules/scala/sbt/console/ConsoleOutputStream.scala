package org.netbeans.modules.scala.sbt.console

import java.awt.Color
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.io.IOException
import java.io.OutputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.io.PrintStream

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

/**
 * @author Caoyuan Deng
 */
class ConsoleOutputStream(area: JTextComponent, message: String, pipedIn: PipedInputStream) extends OutputStream {

  def this(area: JTextComponent) = this(area, null, null)
    
  private var startPos = 0
  private var currentLine: String = _
  
  val defaultFg = Color.WHITE
  val defaultBg = Color.BLACK
  area.setForeground(defaultFg)
  area.setBackground(defaultBg)
  area.setCaretColor(defaultFg)
  val sequenceStyle = new SimpleAttributeSet()
  val defaultStyle  = new SimpleAttributeSet()
  StyleConstants.setForeground(sequenceStyle, defaultFg)     
  StyleConstants.setBackground(sequenceStyle, defaultBg)     
  StyleConstants.setForeground(defaultStyle, defaultFg)     
  StyleConstants.setBackground(defaultStyle, defaultBg)
  
  var currentStyle = defaultStyle

  private val completeCombo = new JComboBox[String]()
  private var completeStart: Int = _
  private var completeEnd: Int = _
    
  private val pipedOut = new PrintStream(new PipedOutputStream(pipedIn))
  
  private val doc = area.getDocument
  //ConsoleLineReader.createConsoleLineReader
        
  area.addKeyListener(myKeyListener)
        
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
        
  if (message ne null) {
    val messageStyle = new SimpleAttributeSet()
    StyleConstants.setBackground(messageStyle, area.getForeground)
    StyleConstants.setForeground(messageStyle, area.getBackground)
    append(message, messageStyle)
  }
  
  override 
  def write(b: Int) {
    writeString(Character.toString(b.toChar))
  }
    
  override 
  def write(b: Array[Byte], off: Int, len: Int) {
    writeString(new String(b, off, len))
  }
    
  override 
  def write(b: Array[Byte]) {
    writeString(new String(b))
  }
  
  private def writeString(str: String) {
    append(str, currentStyle)
    
    startPos = doc.getLength
    area.setCaretPosition(startPos)
  }
  
  private def append(str: String, style: AttributeSet) {
    try {
      doc.insertString(doc.getLength, str, style)
    } catch  {
      case ex: BadLocationException => // just ignore
    }
  }
  
  protected def completeAction(event: KeyEvent) {
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
    pipedOut.println("tasks")
    if (completePopup.isVisible()) {
      val selected = completeCombo.getSelectedIndex() - 1
      if (selected < 0) return
      completeCombo.setSelectedIndex(selected)
      return
    }
        
    //if (! ConsoleLineReader.history.next) // at end
    currentLine = getLine
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
    
  protected def replaceText(start: Int , end: Int , replacement: String) {
    try {
      doc.remove(start, end - start)
      doc.insertString(start, replacement, sequenceStyle)
    } catch {
      case ex: BadLocationException => // Ifnore
    }
  }
    
  protected def getLine(): String = {
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
        
    val inputStr = getLine.trim
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
  
  object myKeyListener extends KeyListener {
    override 
    def keyPressed(event: KeyEvent) {
      val code = event.getKeyCode
      code match {
        case KeyEvent.VK_TAB        => completeAction(event)
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
    StyleConstants.setForeground(os.sequenceStyle, os.defaultFg)
    os.currentStyle = os.sequenceStyle
  }

  @throws(classOf[IOException])
  override
  protected def processDefaultBackgroundColor {
    StyleConstants.setBackground(os.sequenceStyle, os.defaultBg)
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
//    if (concealOn) {
//      write("\u001B[0m")
//      concealOn = false
//    }
//    closeAttributes
  }


}

object AnsiConsoleOutputStream {
  private val ANSI_COLOR_MAP = Array(
    Color.BLACK, Color.RED, Color.GREEN, Color.YELLOW, Color.BLUE, Color.MAGENTA, Color.CYAN, Color.WHITE
  )
}