package org.netbeans.modules.scala.console.readline

import java.awt.Color
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
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
 *
 * @author Caoyuan Deng
 */
class TextAreaReadline(area: JTextComponent, message: String, pipedIn: PipedInputStream) extends OutputStream with KeyListener {

  var startPos: Int = 0
  var currentLine: String = _

  val promptStyle = new SimpleAttributeSet()
  val inputStyle = new SimpleAttributeSet()
  val outputStyle = new SimpleAttributeSet()
  val resultStyle = new SimpleAttributeSet()

  val completeCombo = new JComboBox[String]()
  var start: Int = _
  var end: Int = _

  val pipedPrintOut = new PrintStream(new PipedOutputStream(pipedIn))
  ConsoleLineReader.createConsoleLineReader

  area.addKeyListener(this)

  // No editing before startPos
  area.getDocument match {
    case doc: AbstractDocument =>
      doc.setDocumentFilter(new DocumentFilter() {
        override def insertString(fb: DocumentFilter.FilterBypass, offset: Int, str: String, attr: AttributeSet) {
          if (offset >= startPos) super.insertString(fb, offset, str, attr)
        }

        override def remove(fb: DocumentFilter.FilterBypass, offset: Int, length: Int) {
          if (offset >= startPos) super.remove(fb, offset, length)
        }

        override def replace(fb: DocumentFilter.FilterBypass, offset: Int, length: Int, str: String, attrs: AttributeSet) {
          if (offset >= startPos) super.replace(fb, offset, length, str, attrs)
        }
      })
  }

  StyleConstants.setForeground(promptStyle, new Color(0xa4, 0x00, 0x00))
  StyleConstants.setForeground(inputStyle, new Color(0x20, 0x4a, 0x87))
  StyleConstants.setForeground(outputStyle, Color.darkGray)
  StyleConstants.setItalic(resultStyle, true)
  StyleConstants.setForeground(resultStyle, new Color(0x20, 0x4a, 0x87))

  completeCombo.setRenderer(new DefaultListCellRenderer()) // no silly ticks!
  val completePopup = new BasicComboPopup(completeCombo)

  if (message ne null) {
    val messageStyle = new SimpleAttributeSet()
    StyleConstants.setBackground(messageStyle, area.getForeground)
    StyleConstants.setForeground(messageStyle, area.getBackground)
    append(message, messageStyle)
  }

  def this(area: JTextComponent) = this(area, null, null)

  override def keyPressed(event: KeyEvent) {
    val code = event.getKeyCode

    code match {
      case KeyEvent.VK_TAB        => completeAction(event)
      case KeyEvent.VK_LEFT       => backAction(event)
      case KeyEvent.VK_BACK_SPACE => backAction(event)
      case KeyEvent.VK_UP         => upAction(event)
      case KeyEvent.VK_DOWN       => downAction(event)
      case KeyEvent.VK_ENTER      => enterAction(event)
      case KeyEvent.VK_HOME =>
        event.consume; area.setCaretPosition(startPos)
      case _ => // Ignore
    }

    if (completePopup.isVisible &&
      code != KeyEvent.VK_TAB &&
      code != KeyEvent.VK_UP &&
      code != KeyEvent.VK_DOWN) {
      completePopup.setVisible(false)
    }
  }

  override def keyReleased(event: KeyEvent) {}

  override def keyTyped(event: KeyEvent) {}

  override def write(b: Int) {
    writeString("" + b)
  }

  override def write(b: Array[Byte], off: Int, len: Int) {
    writeString(new String(b, off, len))
  }

  override def write(b: Array[Byte]) {
    writeString(new String(b))
  }

  def append(str: String, style: AttributeSet) {
    try {
      area.getDocument.insertString(area.getDocument.getLength, str, style)
    } catch {
      case ex: BadLocationException => // just ignore
    }
  }

  def writeString(str: String) {
    if (str.startsWith("=>")) {
      append(str, resultStyle)
    } else {
      append(str, outputStyle)
    }
    area.setCaretPosition(area.getDocument.getLength)
    startPos = area.getDocument.getLength
  }

  protected def completeAction(event: KeyEvent) {
    if (ConsoleLineReader.completor eq null) {
      return
    }

    event.consume

    if (completePopup.isVisible) return ;

    val candidates = new ArrayBuffer[String]()
    val bufstr = try {
      area.getText(startPos, area.getCaretPosition - startPos)
    } catch {
      case ex: BadLocationException => return
    }

    val cursor = area.getCaretPosition - startPos

    val position = ConsoleLineReader.completor.complete(bufstr, cursor, candidates)

    // no candidates? Fail.
    if (candidates.isEmpty) {
      return
    }

    if (candidates.size == 1) {
      replaceText(startPos + position, area.getCaretPosition, candidates(0))
      return
    }

    start = startPos + position
    end = area.getCaretPosition

    val pos = area.getCaret().getMagicCaretPosition

    // bit risky if someone changes completor, but useful for method calls
    val cutoff = bufstr.substring(position).lastIndexOf('.') + 1
    start += cutoff

    val candicateSize = math.max(10, candidates.size)
    completePopup.getList.setVisibleRowCount(candicateSize)

    completeCombo.removeAllItems
    if (cutoff != 0) {
      candidates foreach { item => completeCombo.addItem(item.substring(cutoff)) }
    } else {
      candidates foreach { item => completeCombo.addItem(item) }
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

    if (completePopup.isVisible()) {
      val selected = completeCombo.getSelectedIndex() - 1
      if (selected < 0) return
      completeCombo.setSelectedIndex(selected)
      return
    }

    if (!ConsoleLineReader.history.next) // at end
      currentLine = getLine
    else
      ConsoleLineReader.history.previous // undo check

    if (!ConsoleLineReader.history.previous) {
      return
    }

    val oldLine = ConsoleLineReader.history.current.trim
    replaceText(startPos, area.getDocument.getLength, oldLine)
  }

  protected def downAction(event: KeyEvent) {
    event.consume();

    if (completePopup.isVisible()) {
      val selected = completeCombo.getSelectedIndex + 1
      if (selected == completeCombo.getItemCount) return
      completeCombo.setSelectedIndex(selected)
      return
    }

    if (!ConsoleLineReader.history.next) {
      return ;
    }

    val oldLine = if (!ConsoleLineReader.history.next) // at end
      currentLine
    else {
      ConsoleLineReader.history.previous // undo check
      ConsoleLineReader.history.current.trim
    }

    replaceText(startPos, area.getDocument.getLength, oldLine)
  }

  protected def replaceText(start: Int, end: Int, replacement: String) {
    try {
      area.getDocument.remove(start, end - start)
      area.getDocument.insertString(start, replacement, inputStyle)
    } catch {
      case ex: BadLocationException => // Ifnore
    }
  }

  protected def getLine(): String = {
    try {
      area.getText(startPos, area.getDocument.getLength - startPos)
    } catch {
      case ex: BadLocationException => null // Ifnore
    }
  }

  protected def enterAction(event: KeyEvent) {
    event.consume

    if (completePopup.isVisible) {
      if (completeCombo.getSelectedItem ne null)
        replaceText(start, end, completeCombo.getSelectedItem.asInstanceOf[String])
      completePopup.setVisible(false)
      return
    }

    append("\n", null)

    val inputStr = getLine.trim
    pipedPrintOut.println(inputStr)

    ConsoleLineReader.history.addToHistory(inputStr)
    ConsoleLineReader.history.moveToEnd

    area.setCaretPosition(area.getDocument.getLength)
    startPos = area.getDocument.getLength

    //    notify
    //        synchronized (inEditing) {
    //            inEditing.notify();
    //        }
  }

}
