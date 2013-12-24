package org.netbeans.modules.scala.console

import java.awt.Graphics
import java.awt.Rectangle
import javax.swing.text.BadLocationException
import javax.swing.text.DefaultCaret

@SerialVersionUID(1L)
class BlockCaret extends DefaultCaret {

  setBlinkRate(0)

  override protected def damage(r: Rectangle): Unit = synchronized {
    if (r == null)
      return

    // give values to x,y,width,height (inherited from java.awt.Rectangle)
    x = r.x
    y = r.y
    height = r.height
    // A value for width was probably set by paint(), which we leave alone.
    // But the first call to damage() precedes the first call to paint(), so
    // in this case we must be prepared to set a valid width, or else
    // paint()
    // will receive a bogus clip area and caret will not get drawn properly.
    if (width <= 0)
      width = getComponent.getWidth

    repaint() // Calls getComponent().repaint(x, y, width, height) to erase
    repaint() // previous location of caret. Sometimes one call isn't enough.
  }

  override def paint(g: Graphics) {
    val comp = getComponent
    if (comp == null)
      return

    var r: Rectangle = null
    var dotChar: Char = 0
    try {
      val dot = getDot
      r = comp.modelToView(dot)
      if (r == null)
        return
      dotChar = comp.getText(dot, 1).charAt(0)
    } catch {
      case ex: BadLocationException => return
    }

    if ((x != r.x) || (y != r.y)) {
      // paint() has been called directly, without a previous call to
      // damage(), so do some cleanup. (This happens, for example, when
      // the text component is resized.)
      damage(r)
      return
    }

    if (Character.isWhitespace(dotChar)) dotChar = '_'

    g.setColor(comp.getCaretColor)
    g.setXORMode(comp.getBackground) // do this to draw in XOR mode

    width = g.getFontMetrics.charWidth(dotChar)
    if (isVisible)
      g.fillRect(r.x, r.y, width, r.height)
  }
}