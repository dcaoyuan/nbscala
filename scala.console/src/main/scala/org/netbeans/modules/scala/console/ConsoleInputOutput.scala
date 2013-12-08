package org.netbeans.modules.scala.console

import java.io.IOException
import java.io.PrintWriter
import java.io.Reader
import org.openide.windows.InputOutput
import org.openide.windows.OutputListener
import org.openide.windows.OutputWriter

/**
 * @author Caoyuan Deng
 */
class ConsoleInputOutput(input: Reader, out: PrintWriter, err: PrintWriter) extends InputOutput {
  private var closed: Boolean = false

  override def closeInputOutput() {
    closed = true
  }

  override def flushReader: Reader = input

  override def getErr: OutputWriter = new CustomOutputWriter(err)

  override def getIn: Reader = input

  override def getOut: OutputWriter = new CustomOutputWriter(out)

  override def isClosed = closed

  override def isErrSeparated = false

  override def isFocusTaken = false

  override def select() {}

  override def setErrSeparated(value: Boolean) {}

  override def setErrVisible(value: Boolean) {}

  override def setFocusTaken(value: Boolean) {}

  override def setInputVisible(value: Boolean) {}

  override def setOutputVisible(value: Boolean) {}

  private class CustomOutputWriter(pw: PrintWriter) extends OutputWriter(pw) {

    @throws(classOf[IOException])
    override def println(s: String, l: OutputListener) {
      println(s)
    }

    @throws(classOf[IOException])
    override def reset() {}
  }
}

