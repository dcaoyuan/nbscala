package org.netbeans.modules.scala.console

import java.awt.Cursor
import java.awt.Toolkit
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import java.io.IOException
import javax.swing.JTextPane
import javax.swing.SwingUtilities
import javax.swing.text.BadLocationException
import org.openide.DialogDisplayer
import org.openide.ErrorManager
import org.openide.NotifyDescriptor
import org.openide.cookies.EditorCookie
import org.openide.filesystems.FileUtil
import org.openide.loaders.DataObject
import org.openide.loaders.DataObjectNotFoundException
import org.openide.text.Line
import org.openide.util.Exceptions
import org.openide.util.RequestProcessor
import org.openide.util.UserQuestionException
/**
 *
 * @author Caoyuan Deng
 */
class ConsoleMouseListener(textPane: JTextPane) extends MouseAdapter {
  private val handCursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
  private val defaultCursor = Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR)

  override def mouseMoved(e: MouseEvent) {
    val offset = textPane.viewToModel(e.getPoint)
    val element = textPane.getStyledDocument.getCharacterElement(offset)
    element.getAttributes.getAttribute("file") match {
      case x: String => textPane.setCursor(handCursor)
      case _         => textPane.setCursor(defaultCursor)
    }
  }

  override def mouseClicked(evt: MouseEvent) {
    val offset = textPane.viewToModel(evt.getPoint)
    val element = textPane.getStyledDocument.getCharacterElement(offset)
    val attrs = element.getAttributes()
    attrs.getAttribute("file") match {
      case filePath: String =>
        val file = new File(filePath.trim)
        if (file == null || !file.exists) {
          Toolkit.getDefaultToolkit.beep
          return
        }
        val lineNo = try {
          attrs.getAttribute("line") match {
            case line: String => line.toInt
            case _            => -1
          }
        } catch {
          case _: Exception => -1
        }

        openFile(file, lineNo)

      case _ =>
        if (evt.getClickCount != 2) { // double click may be a text selection action
          // [Issue 91208]  avoid of putting cursor in console on line where is not a prompt
          val mouseX = evt.getX
          val mouseY = evt.getY
          // Ensure that this is done after the textpane's own mouse listener
          SwingUtilities.invokeLater(new Runnable() {
            def run() {
              // Attempt to force the mouse click to appear on the last line of the text input
              var pos = textPane.getDocument.getLength
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

                textPane.setCaretPosition(pos)
              } catch {
                case ex: BadLocationException => Exceptions.printStackTrace(ex)
              }
            }
          })
        }

    }

  }

  private def openFile(file: File, lineNo: Int) {

    ConsoleMouseListener.FileOpenRP.post(new Runnable() {
      override def run() {
        try {
          val fo = FileUtil.toFileObject(file)
          val dob = DataObject.find(fo)
          val ed = dob.getLookup.lookup(classOf[EditorCookie])
          if (ed != null && /* not true e.g. for *_ja.properties */ (fo eq dob.getPrimaryFile)) {
            if (lineNo == -1) {
              // OK, just open it.
              ed.open
            } else {
              // Fix for IZ#97727 - warning dialogue for opening large files is meaningless if opened via a hyperlink
              try {
                ed.openDocument // XXX getLineSet does not do it for you!
              } catch {
                case exc: UserQuestionException =>
                  if (!askUserAndDoOpen(exc, ed)) {
                    return
                  }
              }

              try {
                val lineSet = ed.getLineSet
                val line = lineSet.getOriginal(lineNo - 1) // the lineSet is indiced from 0
                if (!line.isDeleted) {
                  SwingUtilities.invokeLater(new Runnable() {
                    override def run() {
                      line.show(Line.ShowOpenType.REUSE, Line.ShowVisibilityType.FOCUS, -1)
                    }
                  })
                }
              } catch {
                case ex: IndexOutOfBoundsException => ed.open // Probably harmless. Bogus line number.
              }
            }
          } else {
            Toolkit.getDefaultToolkit.beep
          }
        } catch {
          case ex: DataObjectNotFoundException => ErrorManager.getDefault.notify(ErrorManager.WARNING, ex)
          case ex: IOException =>
            // XXX see above, should not be necessary to call openDocument at all
            ErrorManager.getDefault.notify(ErrorManager.WARNING, ex)
        }
      }
    })
  }

  // Fix for IZ#97727 - warning dialogue for opening large files is meaningless if opened via a hyperlink
  private def askUserAndDoOpen(_ex: UserQuestionException, cookie: EditorCookie): Boolean = {
    var e = _ex
    while (e != null) {
      val nd = new NotifyDescriptor.Confirmation(e.getLocalizedMessage, NotifyDescriptor.YES_NO_OPTION)
      nd.setOptions(Array[AnyRef](NotifyDescriptor.YES_OPTION, NotifyDescriptor.NO_OPTION))

      val res = DialogDisplayer.getDefault.notify(nd)

      if (NotifyDescriptor.OK_OPTION.equals(res)) {
        try {
          e.confirmed
        } catch {
          case ex: IOException => Exceptions.printStackTrace(ex); return true
        }
      } else {
        return false
      }

      e = null

      try {
        cookie.openDocument
      } catch {
        case ex: UserQuestionException => e = ex
        case ex: IOException           =>
        case ex: Exception             =>
      }
    }

    false
  }
}

object ConsoleMouseListener {
  private val FileOpenRP = new RequestProcessor(classOf[ConsoleMouseListener])
}