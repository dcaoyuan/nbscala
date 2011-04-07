/*
 * xtc - The eXTensible Compiler
 * Copyright (C) 2004, 2006 Robert Grimm
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 2 as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301,
 * USA.
 */
package xtc.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

import java.util.EventListener;
import java.util.LinkedList;

/**
 * Implementation of a nested reader.  A nested reader combines
 * several streams into a single stream.  It starts by reading from a
 * main stream.  Additional streams are added through the {@link
 * #insert(Reader)} and {@link
 * #insert(Reader,NestedReader.EOFListener)} methods and are consumed
 * completely before returning to read from the previous stream.  Note
 * that inserted streams are automatically closed after having being
 * consumed.  Further note that closing a nested reader closes all
 * streams currently associated with that nested reader.
 *
 * @author Robert Grimm
 * @version $Revision: 1.5 $
 */
public class NestedReader extends Reader {

  /**
   * Event listener to provide notification a stream has reached its
   * end.
   *
   * @see #insert(Reader,NestedReader.EOFListener)
   */
  public static interface EOFListener extends EventListener {

    /** Signal that all characters have been consumed. */
    public void consumed();
  
  }

  /** Flag for whether this nested reader has been closed. */
  protected boolean closed;
    
  /** The current character stream. */
  protected Reader reader;

  /** The corresponding end-of-file listener. */
  protected EOFListener listener;

  /**
   * The stack of readers, with the most recently added stream at the
   * front.
   */
  protected LinkedList<Reader> readerStack;

  /**
   * The stack of listeners, with the most recently added listener at
   * the front.
   */
  protected LinkedList<EOFListener> listenerStack;

  /**
   * Create a new nested reader.
   *
   * @param in The main stream.
   */
  public NestedReader(Reader in) {
    closed        = false;
    reader        = in;
    listener      = null;
    readerStack   = new LinkedList<Reader>();
    listenerStack = new LinkedList<EOFListener>();
  }

  /**
   * Open the specified file.  The implementation of this method
   * simply creates a new file reader with the specified file name.
   *
   * @param file The file name.
   * @return The corresponding character stream.
   * @throws IOException Signals an I/O error.
   */
  public Reader open(String file) throws IOException {
    return new BufferedReader(new FileReader(file));
  }

  /**
   * Insert the specified character stream.  After reading all
   * characters from the specified stream, this nested reader silently
   * returns to reading characters from the current stream.
   * 
   * @param in The stream.
   * @throws IOException Signals an I/O error.
   */
  public void insert(Reader in) throws IOException {
    insert(in, null);
  }

  /**
   * Insert the specified character stream.  After reading all
   * characters from the specified stream, but before returning to
   * read characters from the current stream, this nested reader
   * {@link NestedReader.EOFListener#consumed() notifies} the
   * specified listener.
   *
   * @param in The stream.
   * @param eof The listener to be notified when the specified stream
   *   has been consumed.
   * @throws IOException Signals an I/O error.
   */
  public void insert(Reader in, EOFListener eof) throws IOException {
    synchronized (lock) {
      if (closed) {
        throw new IOException("Nested reader closed");
      }
      readerStack.addFirst(reader);
      listenerStack.addFirst(listener);
      reader   = in;
      listener = eof;
    }
  }

  /**
   * Restore the previous character stream.  This method must be
   * called while holding the {@link #lock}.
   *
   * @throws IOException Signals an I/O error.
   */
  private void restore() throws IOException {
    // Notify the listener and close the current stream.
    if (null != listener) {
      listener.consumed();
    }
    reader.close();

    // Actually restore the previous stream.
    reader   = readerStack.removeFirst();
    listener = listenerStack.removeFirst();
  }

  public int read() throws IOException {
    synchronized (lock) {
      do {
        int result = reader.read();

        // Return on a character or the end-of-file for the main stream.
        if ((-1 != result) || readerStack.isEmpty()) {
          return result;
        }

        // Restore the previous stream.
        restore();

        // Try again.
      } while (true);
    }
  }

  public int read(char[] cbuf, int off, int len) throws IOException {
    synchronized (lock) {
      do {
        int result = reader.read(cbuf, off, len);

        // Return on characters or the end-of-file for the main stream.
        if ((-1 != result) || readerStack.isEmpty()) {
          return result;
        }

        // Restore the previous stream.
        restore();

        // Try again.
      } while (true);
    }
  }

  public void close() throws IOException {
    synchronized (lock) {
      if (closed) {
        return;
      } else {
        closed = true;
      }

      IOException error = null;

      try {
        reader.close();
      } catch (IOException x) {
        error = x;
      }

      while (! readerStack.isEmpty()) {
        reader = readerStack.removeFirst();
        try {
          reader.close();
        } catch (IOException x) {
          error = x;
        }
        listenerStack.removeFirst();
      }

      if (null != error) {
        throw error;
      }
    }
  }

}
