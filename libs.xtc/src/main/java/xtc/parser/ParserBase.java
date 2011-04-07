/*
 * xtc - The eXTensible Compiler
 * Copyright (C) 2004-2007 Robert Grimm
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * version 2.1 as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301,
 * USA.
 */
package xtc.parser;

import java.io.IOException;
import java.io.Reader;

import xtc.tree.Locatable;
import xtc.tree.Location;

import xtc.util.Action;
import xtc.util.Pair;

/**
 * The base class for packrat parsers.
 *
 * @author Robert Grimm
 * @version $Revision: 1.17 $
 */
public abstract class ParserBase {

  /** The platform's line separator. */
  public static final String NEWLINE = System.getProperty("line.separator");

  /**
   * The start index for lines.  Note that this constant mirrors
   * {@link xtc.Constants#FIRST_LINE} to avoid parsers depending on
   * that class.
   */
  public static final int FIRST_LINE = 1;

  /**
   * The start index for columns.  Note that this constant mirrors
   * {@link xtc.Constants#FIRST_COLUMN} to avoid parsers depending on
   * that class.
   */
  public static final int FIRST_COLUMN = 1;

  /**
   * The default size for the arrays storing the memoization table's
   * columns.
   */
  public static final int INIT_SIZE = 4096;

  /**
   * The increment for the arrays storing the memoization table's
   * columns.
   */
  public static final int INCR_SIZE = 4096;

  // -------------------------------------------------------------------------

  /** The reader for the character stream to be parsed. */
  protected Reader      yyReader;

  /** The number of characters consumed from the character stream. */
  protected int         yyCount;

  /** The flag for whether the end-of-file has been reached. */
  protected boolean     yyEOF;

  /** The characters consumed so far. */
  protected char[]      yyData;

  /** The memoization table columns. */
  protected Column[]    yyColumns;

  // -------------------------------------------------------------------------

  /**
   * Create a new parser base.
   *
   * @param reader The reader for the character stream to be parsed.
   * @param file The name of the file backing the character stream.
   * @throws NullPointerException Signals a null file name.
   */
  public ParserBase(final Reader reader, final String file) {
    this(reader, file, INIT_SIZE - 1);
  }

  /**
   * Create a new parser base.
   *
   * @param reader The reader for the character stream to be parsed.
   * @param file The name of the file backing the character stream.
   * @param size The length of the character stream.
   * @throws NullPointerException Signals a null file name.
   * @throws IllegalArgumentException Signals a negative file size.
   */
  public ParserBase(final Reader reader, final String file, final int size) {
    if (null == file) {
      throw new NullPointerException("Null file");
    } else if (size < 0) {
      throw new IllegalArgumentException("Negative size: " + size);
    }

    yyReader     = reader;
    yyCount      = 0;
    yyEOF        = false;
    yyData       = new char[size + 1];
    yyColumns    = new Column[size + 1];

    Column c     = newColumn();
    c.file       = file;
    c.seenCR     = false;
    c.line       = FIRST_LINE;
    c.column     = FIRST_COLUMN;

    yyColumns[0] = c;
  }

  // -------------------------------------------------------------------------

  /**
   * Reset this parser to the specified index.  This method discards
   * the input and all memoized intermediate results up to and
   * excluding the specified index.  The index should be determined by
   * accessing {@link SemanticValue#index} from a previous,
   * <i>successful</i> parse (i.e., the result must be a {@link
   * SemanticValue semantic value}).
   *
   * @param index The index.
   * @throws IndexOutOfBoundsException Signals an invalid index.
   */
  public final void resetTo(final int index) {
    // Check the specified index.
    if (0 > index) {
      throw new IndexOutOfBoundsException("Parser index: " + index);

    } else if (0 == index) {
      // There's nothing to see here. Move on.
      return;

    } else if (index >= yyCount) {
      throw new IndexOutOfBoundsException("Parser index: " + index);
    }

    // Get the column at the specified index (to make sure we have the
    // corresponding location information) and construct its
    // replacement.
    Column c1 = column(index);
    Column c2 = newColumn();

    c2.file   = c1.file;
    c2.seenCR = c1.seenCR;
    c2.line   = c1.line;
    c2.column = c1.column;

    yyColumns[0] = c2;

    // Next, shift any read-in characters.
    final int length = yyCount - index;

    System.arraycopy(yyData, index, yyData, 0, length);

    // Next, clear the rest of the memoization table.
    for (int i=length; i<yyCount; i++) {
      yyData[i] = 0;
    }
    for (int i=1; i<yyCount; i++) {
      yyColumns[i] = null;
    }

    // Finally, fix the count.
    yyCount = length;

    // Done.
  }

  // -------------------------------------------------------------------------

  /**
   * Grow the memoization table by the specified increment.
   *
   * @param incr The increment.
   */
  private void growBy(int incr) {
    char[]   oldValues  = yyData;
    yyData              = new char[oldValues.length + incr];
    System.arraycopy(oldValues, 0, yyData, 0, oldValues.length);

    Column[] oldColumns = yyColumns;
    yyColumns           = new Column[oldColumns.length + incr];
    System.arraycopy(oldColumns, 0, yyColumns, 0, oldColumns.length);
  }

  // -------------------------------------------------------------------------

  /**
   * Create a new column.  A concrete implementation of this method
   * should simply return a new memoization table column.
   *
   * @return A new memoization table column.
   */
  protected abstract Column newColumn();

  /**
   * Get the column at the specified index.  If the column at the
   * specified index has not been created yet, it is created as a
   * side-effect of calling this method.
   *
   * @param index The index.
   * @return The corresponding column.
   * @throws IndexOutOfBoundsException Signals an invalid index.
   */
  protected final Column column(final int index) {
    // A memoized production may try to access the entry just past the
    // current end of the table before the corresponding character has
    // been read.  Hence, we may need to grow the table.
    if (yyColumns.length == index) growBy(INCR_SIZE);

    // Note that the array access below will generate an index out of
    // bounds exception for invalid indices.
    Column c = yyColumns[index];
    if (null != c) return c;

    // Find the last non-null column.
    Column last = null;
    int    start;
    for (start=index; start>=0; start--) {
      last = yyColumns[start];
      if (null != last) break;
    }

    // Now, carry the location information forward.
    int     line   = last.line;
    int     column = last.column;
    boolean seenCR = last.seenCR;

    for (int i=start; i<index; i++) {
      switch (yyData[i]) {
      case '\t':
        column = ((column >> 3) + 1) << 3;
        seenCR = false;
        break;
      case '\n':
        if (! seenCR) {
          line++;
          column = FIRST_COLUMN;
        }
        seenCR = false;
        break;
      case '\r':
        line++;
        column = FIRST_COLUMN;
        seenCR = true;
        break;
      default:
        column++;
        seenCR = false;
      }
    }

    // Create the new column.
    c                = newColumn();
    c.file           = last.file;
    c.seenCR         = seenCR;
    c.line           = line;
    c.column         = column;
    yyColumns[index] = c;

    return c;
  }

  // -------------------------------------------------------------------------
  
  /**
   * Parse a character at the specified index.
   *
   * @param index The index.
   * @return The character or -1 if the end-of-file has been reached.
   * @throws IOException
   *   Signals an exceptional condition while accessing the character
   *   stream.
   */
  protected final int character(final int index) throws IOException {
    // Have we seen the end-of-file?
    if (yyEOF) {
      if (index < yyCount - 1) {
        return yyData[index];
      } else if (index < yyCount) {
        return -1;
      } else {
        throw new IndexOutOfBoundsException("Parser index: " + index);
      }
    }

    // Have we already read the desired character?
    if (index < yyCount) {
      return yyData[index];
    } else if (index != yyCount) {
      throw new IndexOutOfBoundsException("Parser index: " + index);
    }

    // Read another character.
    final int c    = yyReader.read();
    final int incr = (-1 == c)? 1 : INCR_SIZE;

    // Do we have enough space?
    if (yyData.length <= yyCount) {
      growBy(incr);
    }

    if (-1 == c) {
      // Remember the end-of-file.
      yyEOF = true;

    } else {
      // Remember the character.
      yyData[index] = (char)c;
    }
    yyCount++;

    // Done.
    return c;
  }

  /**
   * Get the difference between the specified indices.
   *
   * @param start The start index.
   * @param end The end index.
   * @return The difference as a string.
   */
  protected final String difference(final int start, final int end) {
    return (start==end)? "" : new String(yyData, start, end-start);
  }

  /**
   * Determine whether the specified index represents the end-of-file.
   *
   * @param index The index.
   * @return <code>true</code> if the specified index represents EOF.
   */
  public final boolean isEOF(final int index) {
    return yyEOF && (index == yyCount - 1);
  }

  /**
   * Get the line at the specified index.
   *
   * @param index The index.
   * @return The corresponding line, without any line terminating
   *   characters.
   * @throws IndexOutOfBoundsException Signals an invalid index.
   * @throws IOException Signals an I/O error.
   */
  public final String lineAt(int index) throws IOException {
    if (0 > index) {
      throw new IndexOutOfBoundsException("Parser index: " + index);
    }

    // Normalize index for line terminating positions.
    if ((0 < index) &&
        ('\n' == character(index)) &&
        ('\r' == character(index - 1))) {
      index--;
    }

    int start = index;
    int end   = index;
    int c;

    // Find the end of the line.
    c = character(end);
    while ((-1 != c) && ('\r' != c) && ('\n' != c)) {
      end++;
      c = character(end);
    }

    // Find the start of the line.
    while (true) {
      if (0 == start) {
        break;
      }
      c = character(start - 1);
      if (('\r' == c) || ('\n' == c)) {
        break;
      }
      start--;
    }

    // Done.
    return difference(start, end);
  }

  // -------------------------------------------------------------------------
  
  /**
   * Get the location for the specified index.
   *
   * @param index The index.
   * @return The corresponding location.
   */
  public final Location location(final int index) {
    final Column c = column(index);

    return new Location(c.file, c.line, c.column, index, yyCount - 1);
  }

  /**
   * Set the location for the specified index.  This method updates
   * the internal location based on, for example, a line marker
   * recognized by the parser.  
   *
   * <p />This method must be called before any nodes are created for
   * positions at or beyond the specified index &mdash; unless the
   * specified file, line, and column are the same as the internal
   * location for the index.  The line number may be one less than the
   * start index for lines ({@link #FIRST_LINE}), to account for a
   * line marker being present in the input.  The column number is
   * generally be expected to be the start index for columns ({@link
   * #FIRST_COLUMN}), again accounting for a line marker being present
   * in the input.
   *
   * @param index The index.
   * @param file The new file name.
   * @param line The new line number.
   * @param column The new column number.
   * @throws NullPointerException Signals a null file name.
   * @throws IllegalArgumentException Signals an invalid line or
   *   column number.
   * @throws IndexOutOfBoundsException Signals an invalid index.
   * @throws IllegalStateException Signals that the index comes at or
   *   before any memoized results.
   */
  protected final void setLocation(final int index, final String file,
                                   final int line, final int column) {
    // Check the file, line, and column.
    if (null == file) {
      throw new NullPointerException("Null file");
    } else if (FIRST_LINE-1 > line) {
      throw new IllegalArgumentException("Invalid line number: " + line);
    } else if (FIRST_COLUMN > column) {
      throw new IllegalArgumentException("Invalid column number: " + column);
    }

    // Make sure the index is valid.
    if (index < 0 || yyCount <= index) {
      throw new IndexOutOfBoundsException("Parser index: " + index);
    }

    // Detect repeated calls for the same location.
    Column c = yyColumns[index];
    if (null != c) {
      if (file.equals(c.file) && line == c.line && column == c.column) {
        // We ignore repeated calls for the same index and location.
        return;
      } else if (0 != index) {
        // The first column always exists, so we can't signal for a 0 index.
        throw new IllegalStateException("Location at index " + index +
                                        " is already committed");
      }
    }

    // Check that no further columns have been allocated.
    for (int i=index+1; i<yyCount; i++) {
      if (null != yyColumns[i]) {
        throw new IllegalStateException("Location at index " + index +
                                        " is already committed");
      }
    }

    // Actually update the internal location.  Note that we call
    // column() instead of allocating the column directly to correctly
    // carry forward the seenCR flag.
    c        = column(index);
    c.file   = file;
    c.line   = line;
    c.column = column;
  }
  
  /**
   * Set the location for the specified locatable object.  This method
   * is equivalent to:<pre>
   *   if ((null != locatable) && (! locatable.hasLocation())) {
   *     locatable.setLocation(location(index));
   *   }
   * </pre>
   *
   * @param locatable The locatable object.
   * @param index The index.
   */
  public final void setLocation(final Locatable locatable, final int index) {
    if ((null != locatable) && (! locatable.hasLocation())) {
      Column c = column(index);
      locatable.setLocation(new Location(c.file, c.line, c.column, index, yyCount - 1));
    }
  }

  // -------------------------------------------------------------------------

  /**
   * Apply the specified actions on the specified seed.  This method
   * applies all {@link xtc.util.Action actions} on the specified
   * list, using the result of the previous action as the argument to
   * the next action.  The argument to the first action is the
   * specified seed.  If the specified list is empty, this method
   * simply returns the specified seed.
   *
   * @param actions The actions to apply.
   * @param seed The initial argument.
   * @return The result of applying the actions.
   */
  protected final <T> T apply(Pair<Action<T>> actions, T seed) {
    while (! actions.isEmpty()) {
      seed    = actions.head().run(seed);
      actions = actions.tail();
    }
    return seed;
  }

  /**
   * Apply the specified actions on the specified seed while also
   * setting the results' locations.  This method applies all {@link
   * xtc.util.Action actions} on the specified list, using the result
   * of the previous action as the argument to the next action.  For
   * the result of each application, it also sets the location.  The
   * argument to the first action is the specified seed.  If the
   * specified list is empty, this method simply returns the specified
   * seed.
   *
   * @param actions The actions to apply.
   * @param seed The initial argument.
   * @param index The index representing the current parser location.
   * @return The result of applying the actions.
   */
  protected final <T extends Locatable> T apply(Pair<Action<T>> actions,
                                                T seed, final int index) {
    if (! actions.isEmpty()) {
      final Location loc = location(index);

      do {
        seed    = actions.head().run(seed);
        seed.setLocation(loc);
        actions = actions.tail();
      } while (! actions.isEmpty());
    }

    return seed;
  }

  // -------------------------------------------------------------------------

  /**
   * Format the specified parse error.  The specified error must have
   * been created by this parser.
   *
   * @param error The error.
   * @return The corresponding error message.
   * @throws IOException Signals an I/O error while creating the error
   *   message.
   */
  public final String format(ParseError error) throws IOException {
    final StringBuilder buf = new StringBuilder();

    // The error's location.
    Column c = null;
    if (-1 != error.index) {
      c = column(error.index);
      buf.append(c.file);
      buf.append(':');
      buf.append(c.line);
      buf.append(':');
      buf.append(c.column);
      buf.append(": ");
    }

    // The error's actual message.
    buf.append("error: ");
    buf.append(error.msg);

    // The error's line with a position marker.
    if (-1 != error.index) {
      final String line = lineAt(error.index);
      final int    size = line.length();

      buf.append(NEWLINE);
      for (int i=0; i<size; i++) buf.append(line.charAt(i));
      buf.append(NEWLINE);
      for (int i=FIRST_COLUMN; i<c.column; i++) buf.append(' ');
      buf.append('^');
      buf.append(NEWLINE);
    }

    // Done.
    return buf.toString();
  }

  /**
   * Signal the specified parse error as a parse exception.  The
   * specified error must have been created by this parser.
   *
   * @param error The parse error.
   * @throws ParseException Signals the error.
   * @throws IOException Signals an I/O error while creating the
   *   exception's detail message.
   */
  public final void signal(ParseError error) throws ParseException,IOException {
    throw new ParseException(format(error));
  }

  /**
   * Extract the specified result's value.  If the result is a {@link
   * SemanticValue}, this method returns the actual value; if it is a
   * {@link ParseError}, it signals a parse exception with the
   * corresponding message.  The specified result must have been
   * created by this parser.
   *
   * @param r The result.
   * @return The corresponding value.
   * @throws ParseException Signals that the result represents a parse
   *   error.
   * @throws IOException Signals an I/O error while creating the parse
   *   error's detail message.
   */
  public final Object value(Result r) throws ParseException, IOException {
    if (! r.hasValue()) signal(r.parseError());
    return r.semanticValue();
  }
  
  // -------------------------------------------------------------------------
  
  /**
   * Get the next few characters from the specified index.
   *
   * @param index The index.
   * @return The next few characters.
   */
  protected final String peek(final int index) {
    int limit = yyEOF? yyCount - 1 : yyCount;
    if (index >= limit) return "";
    limit     = Math.min(index + 20, limit);
    return new String(yyData, index, limit-index);
  }
  
  // -------------------------------------------------------------------------

  /**
   * Cast the specified object.  This method is used to avoid spurious
   * compiler warnings for parsers utilizing generic types.
   *
   * @param o The object.
   * @return The cast object.
   */
  @SuppressWarnings("unchecked")
  protected static final <T> T cast(Object o) {
    return (T)o;
  }

  /**
   * Cast the list starting at the specified pair.  This method is
   * used to avoid spurious compiler warnings for parsers utilizing
   * generic types.
   *
   * @param p The list.
   * @return The cast list.
   */
  @SuppressWarnings("unchecked")
  protected static final <T> Pair<T> cast(Pair<?> p) {
    return (Pair<T>)p;
  }

}
