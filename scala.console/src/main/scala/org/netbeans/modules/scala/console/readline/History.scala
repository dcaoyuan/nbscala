package org.netbeans.modules.scala.console.readline

import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileWriter
import java.io.InputStream
import java.io.InputStreamReader
import java.io.PrintWriter
import java.io.Reader
import scala.collection.mutable.ArrayBuffer

/**
 *
 * @author Caoyuan Deng
 */
class History(historyFile: File) {
  if (historyFile ne null) setHistoryFile(historyFile)

  val history = new ArrayBuffer[String]()

  private var _output: PrintWriter = null

  private var _maxSize = 500

  private var _currentIndex = 0

  /**
   * Construstor: initialize a blank history.
   */
  def this() = this(null)

  def setHistoryFile(historyFile: File) {
    if (historyFile.isFile) {
      load(new FileInputStream(historyFile))
    }

    output = new PrintWriter(new FileWriter(historyFile), true)
    flushBuffer
  }

  /**
   * Load the history buffer from the specified InputStream.
   */
  def load(in: InputStream) {
    load(new InputStreamReader(in))
  }

  /**
   * Load the history buffer from the specified Reader.
   */
  def load(reader: Reader) {
    val breader = new BufferedReader(reader)
    val lines = new ArrayBuffer[String]()
    var line = breader.readLine
    while (line ne null) {
      lines += line
      line = breader.readLine
    }
    lines foreach (addToHistory(_))
  }

  def size(): Int = history.size

  /**
   * Clear the history buffer
   */
  def clear() {
    history.clear
    _currentIndex = 0
  }

  /**
   * Add the specified buffer to the end of the history. The pointer is set to
   * the end of the history buffer.
   */
  def addToHistory(buffer: String) {
    // don't append duplicates to the end of the buffer
    if (!history.isEmpty && buffer == history(history.size - 1)) {
      return
    }

    history += buffer

    while (history.size > maxSize) {
      history.remove(0)
    }

    _currentIndex = history.size

    if (output ne null) {
      output.println(buffer)
      output.flush
    }
  }

  /**
   * Flush the entire history buffer to the output PrintWriter.
   */
  def flushBuffer() {
    if (output eq null) return
    history foreach (output.println(_))
    output.flush
  }

  /**
   * This moves the history to the last entry. This entry is one position
   * before the moveToEnd() position.
   *
   * @return Returns false if there were no history entries or the history
   *         index was already at the last entry.
   */
  def moveToLastEntry(): Boolean = {
    val lastEntry = history.size - 1
    if (lastEntry >= 0 && lastEntry != _currentIndex) {
      _currentIndex = history.size - 1
      true
    } else {
      false
    }
  }

  /**
   * Move to the end of the history buffer. This will be a blank entry, after
   * all of the other entries.
   */
  def moveToEnd() = _currentIndex = history.size

  /**
   * The maximum size that the history buffer will store.
   */
  def maxSize = _maxSize
  def maxSize_=(maxSize: Int) {
    _maxSize = maxSize
  }

  /**
   * The PrintWriter that is used to store history elements.
   */
  def output = _output
  def output_=(output: PrintWriter) {
    _output = output
  }

  /**
   * Returns the current history index.
   */
  def currentIndex: Int = _currentIndex

  /**
   * Return the content of the current buffer.
   */
  def current(): String = {
    if (_currentIndex >= history.size) {
      ""
    } else {
      history(_currentIndex)
    }
  }

  /**
   * Move the pointer to the previous element in the buffer.
   *
   * @return true if we successfully went to the previous element
   */
  def previous(): Boolean = {
    if (_currentIndex <= 0) {
      false
    } else {
      _currentIndex = _currentIndex - 1
      true
    }
  }

  /**
   * Move the pointer to the next element in the buffer.
   *
   * @return true if we successfully went to the next element
   */
  def next(): Boolean = {
    if (_currentIndex >= history.size) {
      false
    } else {
      _currentIndex = _currentIndex + 1
      true
    }
  }

  /**
   * Returns an immutable list of the history buffer.
   */
  def getHistoryList(): Seq[String] = history

  /**
   * Returns the standard {@link AbstractCollection#toString} representation
   * of the history list.
   */
  override def toString(): String = history.toString

  /**
   * Moves the history index to the first entry.
   *
   * @return Return false if there are no entries in the history or if the
   *         history is already at the beginning.
   */
  def moveToFirstEntry(): Boolean = {
    if (!history.isEmpty && _currentIndex != 0) {
      _currentIndex = 0
      true
    } else {
      false
    }
  }

}
