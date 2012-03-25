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

class History(historyFile:File) {
    if (historyFile != null) setHistoryFile(historyFile)
  
   val history = new ArrayBuffer[String]()

    var output:PrintWriter = null

    var maxSize = 500;

    var currentIndex = 0;

    /**
     * Construstor: initialize a blank history.
     */
    def this() = this(null)

    def setHistoryFile(historyFile:File) {
        if (historyFile.isFile()) {
            load(new FileInputStream(historyFile));
        }

        setOutput(new PrintWriter(new FileWriter(historyFile), true));
        flushBuffer();
    }

    /**
     * Load the history buffer from the specified InputStream.
     */
    def load(in:InputStream) {
        load(new InputStreamReader(in));
    }

    /**
     * Load the history buffer from the specified Reader.
     */
    def load(reader:Reader) {
        val breader = new BufferedReader(reader);
        val lines = new ArrayBuffer[String]();
        var line = breader.readLine();

        while (line != null) {
            lines += line;
            line = breader.readLine();
        }
        lines.foreach(addToHistory(_))
    }

    def size():Int = history.size

    /**
     * Clear the history buffer
     */
    def clear() {
        history.clear();
        currentIndex = 0;
    }

    /**
     * Add the specified buffer to the end of the history. The pointer is set to
     * the end of the history buffer.
     */
    def addToHistory(buffer:String) {
        // don't append duplicates to the end of the buffer
        if ((!history.isEmpty)
                && buffer.equals(history(history.size - 1))) {
            return;
        }

        history += buffer;

        while (history.size > getMaxSize()) {
            history.remove(0);
        }

        currentIndex = history.size;

        if (getOutput() != null) {
            getOutput().println(buffer);
            getOutput().flush();
        }
    }

    /**
     * Flush the entire history buffer to the output PrintWriter.
     */
    def flushBuffer() {
        if (getOutput() == null) return
        history.foreach(getOutput.println(_))
        getOutput().flush();
    }

    /**
     * This moves the history to the last entry. This entry is one position
     * before the moveToEnd() position.
     *
     * @return Returns false if there were no history entries or the history
     *         index was already at the last entry.
     */
    def moveToLastEntry():Boolean = {
        val lastEntry = history.size - 1;
        if (lastEntry >= 0 && lastEntry != currentIndex) {
            currentIndex = history.size - 1;
            return true;
        }
        return false;
    }

    /**
     * Move to the end of the history buffer. This will be a blank entry, after
     * all of the other entries.
     */
    def moveToEnd() = currentIndex = history.size;

    /**
     * Set the maximum size that the history buffer will store.
     */
    def setMaxSize(maxSize:Int) = this.maxSize = maxSize;
    

    /**
     * Get the maximum size that the history buffer will store.
     */
   def getMaxSize():Int = maxSize

    /**
     * The output to which all history elements will be written (or null of
     * history is not saved to a buffer).
     */
   def setOutput(output:PrintWriter) = this.output = output;

    /**
     * Returns the PrintWriter that is used to store history elements.
     */
    def getOutput():PrintWriter = this.output;

    /**
     * Returns the current history index.
     */
    def getCurrentIndex():Int = currentIndex;

    /**
     * Return the content of the current buffer.
     */
    def current():String = {
        if (currentIndex >= history.size) {
            return "";
        }
        return history(currentIndex);
    }

    /**
     * Move the pointer to the previous element in the buffer.
     *
     * @return true if we successfully went to the previous element
     */
    def previous():Boolean = {
        if (currentIndex <= 0) {
            return false;
        }
        currentIndex = currentIndex - 1;
        return true;
    }

    /**
     * Move the pointer to the next element in the buffer.
     *
     * @return true if we successfully went to the next element
     */
    def next():Boolean = {
        if (currentIndex >= history.size) {
            return false;
        }

        currentIndex = currentIndex + 1;
        return true;
    }

    /**
     * Returns an immutable list of the history buffer.
     */
    def getHistoryList():Seq[String] = history

    /**
     * Returns the standard {@link AbstractCollection#toString} representation
     * of the history list.
     */
   override def toString():String = history.toString();    

    /**
     * Moves the history index to the first entry.
     *
     * @return Return false if there are no entries in the history or if the
     *         history is already at the beginning.
     */
    def moveToFirstEntry():Boolean = {
      if (!history.isEmpty && currentIndex != 0) {
            currentIndex = 0;
            return true;
        }

        return false;
    }
  
}
