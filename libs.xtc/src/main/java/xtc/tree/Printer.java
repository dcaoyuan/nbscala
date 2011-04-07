/*
 * xtc - The eXTensible Compiler
 * Copyright (C) 2004-2007 Robert Grimm
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
package xtc.tree;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

import java.text.BreakIterator;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import xtc.Constants;

import xtc.util.Pair;
import xtc.util.Utilities;

/** 
 * A node pretty printing utility.  This class helps with the pretty
 * printing of syntax trees, including with the generation of source
 * code.  It provides facilities for indenting, escaping, aligning,
 * and line-wrapping text.  Note that, for the facilities of this
 * class to work, newlines should never be printed through a character
 * or string constant (e.g., by using '<code>\n</code>' or
 * '<code>\r</code>') but always by calling the appropriate method.
 *
 * @author Robert Grimm
 * @version $Revision: 1.62 $
 */
public class Printer extends Utility {

  /** The break iterator, if any. */
  protected BreakIterator breaks;

  /** The current print writer to print to. */
  protected PrintWriter out;

  /** The original print writer. */
  protected PrintWriter directOut;

  /** The string writer, if output is currently being buffered. */
  protected StringWriter bufferedOut = null;

  /** The number of outstanding invocations to {@link #buffer()}. */
  protected int buffering = 0;

  /** The current indentation level. */
  protected int indent = 0;

  /** The current column. */
  protected int column = Constants.FIRST_COLUMN;

  /** The current line. */
  protected long line = Constants.FIRST_LINE;

  // ========================================================================

  /**
   * Create a new printer with the specified output stream.  The
   * printer does <i>not</i> flush the specified output stream on
   * newlines.
   *
   * @param out The output stream.
   */
  public Printer(OutputStream out) {
    this(new PrintWriter(out, false));
  }

  /**
   * Create a new printer with the specified writer.  The printer does
   * <i>not</i> flush the specified writer on newlines.
   *
   * @param out The writer.
   */
  public Printer(Writer out) {
    this(new PrintWriter(out, false));
  }

  /**
   * Create a new printer with the specified print writer.
   *
   * @param out The print writer to output to.
   */
  public Printer(PrintWriter out) {
    this.out  = out;
    directOut = out;
  }

  // ========================================================================

  /**
   * Reset this printer.  This method stops buffering (if this printer
   * was buffering) and clears the current indentation level, column
   * number, and line number.
   *
   * @return This printer.
   */
  public Printer reset() {
    stopBuffering();
    indent = 0;
    column = Constants.FIRST_COLUMN;
    line   = Constants.FIRST_LINE;

    return this;
  }

  // ========================================================================

  /**
   * Get the current column number.
   *
   * @return The current column number.
   */
  public int column() {
    return column;
  }

  /**
   * Set the current column to the specified number.
   *
   * @param column The new column number.
   * @return This printer.
   */
  public Printer column(int column) {
    this.column = column;
    return this;
  }

  /**
   * Get the current line number.
   *
   * @return The current line number.
   */
  public long line() {
    return line;
  }

  /**
   * Set the current line to the specified number.
   *
   * @param line The new line number.
   * @return This printer.
   */
  public Printer line(long line) {
    this.line = line;
    return this;
  }

  // ========================================================================

  /**
   * Start buffering the output.  This method starts redirecting all
   * output into a buffer, so that later invocations to {@link
   * #fit()}, {@link #fit(String)}, or {@link #fitMore()} can ensure
   * that the output fits onto the current line.
   *
   * <p />Note that invocations to this method are matched with
   * invocations to the <code>fit()</code> and <code>fitMore()</code>
   * methods.  In other words, the <code>fit()</code> and
   * <code>fitMore()</code> methods only have an effect, if they
   * correspond to the first invocation of this method after (1) this
   * printer has been created, (2) the last invocation of {@link
   * #reset()}, (3) the last invocation of {@link #unbuffer()}, or (4)
   * the last invocation of any methods printing a newline.
   *
   * @return This printer.
   */
  public Printer buffer() {
    if (0 == buffering) {
      // Create a new string writer and make it the current print
      // writer.
      bufferedOut = new StringWriter();
      out         = new PrintWriter(bufferedOut, false);
    }
    buffering++;

    return this;
  }

  /**
   * Reset any buffering.  If this printer is currently buffering the
   * output, this method stops buffering and returns the buffer
   * contents.  Otherwise, it returns the empty string.
   *
   * @return The buffer contents.
   */
  protected String stopBuffering() {
    if (null != bufferedOut) {
      // Flush the current print writer.
      out.flush();

      // Get the buffer contents.
      final String s = bufferedOut.toString();

      // Restore the writers and buffer count.
      out         = directOut;
      bufferedOut = null;
      buffering   = 0;

      return s;
    } else {
      return "";
    }
  }

  /**
   * Ensure that the buffer contents fit onto the current line.  This
   * method writes the buffer contents out.  If the contents do not
   * fit onto the current line, it first writes a newline and then
   * indents the output.
   *
   * @see #buffer()
   *
   * @return This printer.
   */
  public Printer fit() {
    if (1 == buffering) {
      final String s = stopBuffering();

      if (Constants.LINE_LENGTH + Constants.FIRST_COLUMN < column) {
        // We write through this printer's methods to count the buffer
        // contents again.
        out.println();
        column = Constants.FIRST_COLUMN;
        line++;
        indent().p(s);

      } else {
        // We write directly, as the buffer contents have already been
        // counted.
        out.print(s);
      }

    } else if (1 < buffering) {
      buffering--;
    }

    return this;
  }

  /**
   * Ensure that the buffer contents fit onto the current line.  This
   * method writes the buffer contents out.  If the contents do not
   * fit onto the current line, it first writes a newline and then
   * aligns the output with specified alignment.
   *
   * @see #buffer()
   *
   * @param align The alignment.
   * @return This printer.
   */
  public Printer fit(int align) {
    if (1 == buffering) {
      final String s = stopBuffering();

      if (Constants.LINE_LENGTH + Constants.FIRST_COLUMN < column) {
        // We write through this printer's methods to count the buffer
        // contents again.
        out.println();
        column = Constants.FIRST_COLUMN;
        line++;
        align(align).p(s);

      } else {
        // We write directly, as the buffer contents have already been
        // counted.
        out.print(s);
      }

    } else if (1 < buffering) {
      buffering--;
    }

    return this;
  }

  /**
   * Ensure that the buffer contents fit onto the current line.  This
   * method writes the buffer contents out.  If the contents do not
   * fit onto the current line, it first writes a newline, then
   * indents the output, and then writes the specified prefix.
   *
   * @see #buffer()
   *
   * @param prefix The prefix.
   * @return This printer.
   */
  public Printer fit(String prefix) {
    if (1 == buffering) {
      final String s = stopBuffering();

      if (Constants.LINE_LENGTH + Constants.FIRST_COLUMN < column) {
        // We write through this printer's methods to count the buffer
        // contents again.
        out.println();
        column = Constants.FIRST_COLUMN;
        line++;
        indent().p(prefix).p(s);

      } else {
        // We write directly, as the buffer contents have already been
        // counted.
        out.print(s);
      }

    } else if (1 < buffering) {
      buffering--;
    }

    return this;
  }

  /**
   * Ensure that the buffer contents fit onto the current line.  This
   * method writes the buffer contents out.  If the contents do not
   * fit onto the current line, it first writes a newline and then
   * indents the output one tab stop more than the current indentation
   * level.
   *
   * @see #buffer()
   *
   * @return This printer.
   */
  public Printer fitMore() {
    if (1 == buffering) {
      final String s = stopBuffering();

      if (Constants.LINE_LENGTH + Constants.FIRST_COLUMN < column) {
        // We write through this printer's methods to count the buffer
        // contents again.
        out.println();
        column = Constants.FIRST_COLUMN;
        line++;
        indentMore().p(s);

      } else {
        // We write directly, as the buffer contents have already been
        // counted.
        out.print(s);
      }

    } else if (1 < buffering) {
      buffering--;
    }

    return this;
  }

  /**
   * Stop buffering the output.  If the output is currently being
   * buffered, this method writes the buffer contents out and stops
   * buffering.  Otherwise, it has no effect.
   *
   * @return This printer.
   */
  public Printer unbuffer() {
    if (0 < buffering) {
      final String s = stopBuffering();
      out.write(s);
    }

    return this;
  }

  // ========================================================================

  /**
   * Print whitespace to align the output.  This method prints
   * whitespace to cover the difference between the current column
   * number and the specified, absolute alignment.  If the column
   * number is greater or equal the specified alignment, a single
   * space character is printed.
   *
   * @param alignment The number of characters to align at.
   * @return This printer.
   */
  public Printer align(int alignment) {
    int toPrint = alignment - column;
    if (0 >= toPrint) toPrint = 1;
    for (int i=0; i<toPrint; i++) {
      out.write(' ');
    }
    column += toPrint;
    return this;
  }

  // ========================================================================

  /**
   * Get the current indentation level.
   *
   * @return The current indentation level.
   */
  public int level() {
    return indent / Constants.INDENTATION;
  }

  /**
   * Set the current indentation level.
   *
   * @param level The new indentation level.
   * @return This printer.
   * @throws IllegalArgumentException
   *   Signals that the specified level is negative.
   */
  public Printer setLevel(int level) {
    if (0 > level) {
      throw new IllegalArgumentException("Negative indentation level");
    }
    indent = level * Constants.INDENTATION;
    return this;
  }

  /**
   * Increase the current indentation level.
   *
   * @return This printer.
   */
  public Printer incr() {
    indent += Constants.INDENTATION;
    return this;
  }

  /**
   * Decrease the current indentation level.
   *
   * @return This printer.
   */
  public Printer decr() {
    indent -= Constants.INDENTATION;
    return this;
  }

  /**
   * Indent.
   *
   * @return This printer.
   */
  public Printer indent() {
    for (int i=0; i<indent; i++) {
      out.print(' ');
    }

    column += indent;
    return this;
  }

  /**
   * Indent one tab stop less than the current indentation level.
   *
   * @return This printer.
   */
  public Printer indentLess() {
    int w = indent - Constants.INDENTATION;
    if (0 > w) {
      w = 0;
    }

    for (int i=0; i<w; i++) {
      out.print(' ');
    }

    column += w;
    return this;
  }

  /**
   * Indent one tab stop more than the current indentation level.
   *
   * @return This printer.
   */
  public Printer indentMore() {
    final int w = indent + Constants.INDENTATION;

    for (int i=0; i<w; i++) {
      out.print(' ');
    }

    column += w;
    return this;
  }

  // ========================================================================

  /**
   * Print the specified character.
   *
   * @param c The character to print.
   * @return This printer.
   */
  public Printer p(char c) {
    out.print(c);
    column += 1;
    return this;
  }

  /**
   * Print the specified integer.
   *
   * @param i The integer to print.
   * @return This printer.
   */
  public Printer p(int i) {
    return p(Integer.toString(i));
  }

  /**
   * Print the specified long.
   *
   * @param l The long to print.
   * @return This printer.
   */
  public Printer p(long l) {
    return p(Long.toString(l));
  }

  /**
   * Print the specified double.
   *
   * @param d The double to print.
   * @return This printer.
   */
  public Printer p(double d) {
    return p(Double.toString(d));
  }

  /**
   * Print the specified string.
   *
   * @param s The string to print.
   * @return This printer.
   */
  public Printer p(String s) {
    out.print(s);
    column += s.length();
    return this;
  }

  /**
   * Print the specified character followed by a newline.
   *
   * @param c The character to print.
   * @return This printer.
   */
  public Printer pln(char c) {
    unbuffer();
    out.println(c);
    column = Constants.FIRST_COLUMN;
    line++;
    return this;
  }

  /**
   * Print the specified integer followed by a newline.
   *
   * @param i The integer to print.
   * @return This printer.
   */
  public Printer pln(int i) {
    return pln(Integer.toString(i));
  }

  /**
   * Print the specified long followed by a newline.
   *
   * @param l The long to print.
   * @return This printer.
   */
  public Printer pln(long l) {
    return pln(Long.toString(l));
  }

  /**
   * Print the specified double followed by a newline.
   *
   * @param d The double to print.
   * @return This printer.
   */
  public Printer pln(double d) {
    return pln(Double.toString(d));
  }

  /**
   * Print the specified string followed by a newline.
   *
   * @param s The string to print.
   * @return This printer.
   */
  public Printer pln(String s) {
    unbuffer();
    out.println(s);
    column = Constants.FIRST_COLUMN;
    line++;
    return this;
  }

  /**
   * Print a newline.
   *
   * @return This printer.
   */
  public Printer pln() {
    unbuffer();
    out.println();
    column = Constants.FIRST_COLUMN;
    line++;
    return this;
  }

  // ========================================================================

  /**
   * Print the specified character using C escapes.
   *
   * @param c The character to print.
   * @return This printer.
   */
  public Printer escape(char c) {
    return p(Utilities.escape(c, Utilities.C_ESCAPES));
  }

  /**
   * Print the specified character with the specified escape sequences.
   *
   * @see Utilities
   *
   * @param c The character to print.
   * @param flags The escape flags.
   * @return This printer.
   */
  public Printer escape(char c, int flags) {
    return p(Utilities.escape(c, flags));
  }

  /**
   * Print the specified string using C escapes.
   *
   * @param s The string to print.
   * @return This printer.
   */
  public Printer escape(String s) {
    return p(Utilities.escape(s, Utilities.C_ESCAPES));
  }

  /**
   * Print the specified string with the specified escape sequences.
   *
   * @see Utilities
   *
   * @param s The string to print.
   * @param flags The escape flags.
   * @return This printer.
   */
  public Printer escape(String s, int flags) {
    return p(Utilities.escape(s, flags));
  }

  // ========================================================================

  /**
   * Print the specified long while also padding it to the specified
   * width with leading spaces.
   *
   * @param l The long to print.
   * @param width The width.
   * @return This printer.
   */
  public Printer pad(long l, int width) {
    final String text    = Long.toString(l);
    final int    padding = width - text.length();
    for (int i=0; i<padding; i++) p(' ');
    p(text);
    return this;
  }

  // ========================================================================

  /**
   * Print an indented separation comment.
   *
   * @return This printer.
   */
  public Printer sep() {
    unbuffer();
    indent().p("// ");

    final int n = Constants.LINE_LENGTH - indent - 3;
    for (int i=0; i<n; i++) {
      out.print('=');
    }

    out.println();
    column = Constants.FIRST_COLUMN;
    line++;
    return this;
  }

  // ========================================================================

  /**
   * Print the specified text.  This method line-wraps the specified
   * text with the specified per-line alignment.  It does, however,
   * not print the initial alignment or the final end-of-line.
   *
   * @param alignment The per-line alignment.
   * @param text The text.
   */
  public Printer wrap(int alignment, String text) {
    if (null == breaks) {
      breaks = BreakIterator.getLineInstance(Locale.ENGLISH);
    }

    breaks.setText(text);
    int     start = breaks.first();
    int     end   = breaks.next();
    boolean first = true;
    while (BreakIterator.DONE != end) {
      String word = text.substring(start, end);

      if (! first &&
          (Constants.LINE_LENGTH + Constants.FIRST_COLUMN
           < column + word.length())) {
        pln();
        if (Constants.FIRST_COLUMN != alignment) {
          align(alignment);
        }
      }
      p(word);

      start = end;
      end   = breaks.next();
      first = false;
    }

    return this;
  }

  // ========================================================================

  /**
   * Print the specified node.  If the specified node is
   * <code>null</code>, nothing is printed.
   *
   * @param node The node to print.
   * @return This printer.
   */
  public Printer p(Node node) {
    visitor.dispatch(node);
    return this;
  }

  /**
   * Print the specified attribute.
   *
   * @param attribute The attribute.
   * @return This printer.
   */
  public Printer p(Attribute attribute) {
    p(attribute.name);
    if (null != attribute.value) {
      p('(');
      if ((attribute.value instanceof List) ||
          (attribute.value instanceof Pair)) {
        boolean first = true;
        for (Object o : (Iterable<?>)attribute.value) {
          if (first) {
            first = false;
          } else {
            p(", ");
          }
          p(o.toString());
        }
      } else {
        p(attribute.value.toString());
      }
      p(')');
    }
    return this;
  }

  /**
   * Print the specified comment.  Note that this method does
   * <em>not</em> indent the first line, but it does indent all
   * following lines for comments spanning multiple lines.  Further
   * note that this method does <em>not</em> dispatch this printer's
   * visitor on the node contained in the comment.
   *
   * @param comment The comment to print.
   * @return This printer.
   */
  public Printer p(Comment comment) {
    if (0 == comment.text.size()) return this;

    if (Comment.Kind.SINGLE_LINE == comment.kind) {
      p("// ").pln(comment.text.get(0));

    } else {
      if (Comment.Kind.MULTIPLE_LINES == comment.kind) {
        p("/*");
      } else {
        p("/**");
      }

      if (1 == comment.text.size()) {
        p(' ').p(comment.text.get(0)).pln(" */");
      } else {
        pln();
        for (String line : comment.text) {
          indent().p(" * ").pln(line);
        }
        indent().pln(" */");
      }
    }
    return this;
  }

  // ========================================================================

  /**
   * Format the specified node.  Instead of using this printer's
   * visitor to print the specified node, this method emits a general
   * representation of the node, using the node's generic traversal
   * methods if available.
   *
   * @param n The node.
   * @return This printer.
   */
  public Printer format(Node n) {
    return format1(n, false);
  }

  /**
   * Format the specified node.  Instead of using this printer's
   * visitor to print the specified node, this method emits a general
   * representation of the node, using the node's generic traversal
   * methods if available.
   *
   * @param n The node.
   * @param locate The flag for printing a node's location.
   * @return This printer.
   */
  public Printer format(Node n, boolean locate) {
    formatFile = null;
    return format1(n, locate);
  }

  /** The last file name encountered when printing locations. */
  private String formatFile;

  /**
   * Format the specified object.
   *
   * @param o The object.
   * @param locate The flag for printing a node's location.
   * @return This printer.
   */
  private Printer format1(Object o, boolean locate) {
    indent();

    if (null == o) {
      p("null");

    } else if (o instanceof Node) {
      final Node n = (Node)o;

      p(n.getName());

      if (locate && n.hasLocation()) {
        final Location loc = n.getLocation();
        p('@');
        if (! loc.file.equals(formatFile)) {
          p(loc.file).p(':');
          formatFile = loc.file;
        }
        p(loc.line).p(':').p(loc.column);
      }

      p('(');

      if (n.isEmpty()) {
        p(')');
      } else {
        pln().incr().formatElements(n, locate).decr().indent().p(')');
      }

    } else if (o instanceof Pair) {
      final Pair<?> p = (Pair<?>)o;

      if (p.isEmpty()) {
        p("[]");
      } else {
        pln('[').incr().formatElements(p, locate).decr().indent().p(']');
      }

    } else if (o instanceof String) {
      p('"').escape(o.toString(), Utilities.C_ESCAPES).p('"');

    } else {
      p(o.toString());
    }

    return this;
  }

  /**
   * Format the specified composite's elements.
   *
   * @param composite The composite.
   * @param locate The flag for printing a node's location.
   * @return This printer.
   */
  private Printer formatElements(Iterable<?> composite, boolean locate) {
    for (Iterator<?> iter = composite.iterator(); iter.hasNext(); ) {
      format1(iter.next(), locate);
      if (iter.hasNext()) p(',');
      pln();
    }
    return this;
  }

  // ========================================================================

  /**
   * Print the location for the specified locatable object.  First, if
   * the locatable is a node and has a {@link Constants#ORIGINAL}
   * property, that property's locatable value replaces the specified
   * locatable object.  Second, if the actual locatable has a
   * location, this method prints the file name, a colon, the line
   * number, another colon, and the column number.  If the actual
   * locatable does not have a location, nothing is printed.
   *
   * @param locatable The locatable object.
   * @return This printer.
   */
  public Printer loc(Locatable locatable) {
    if (locatable instanceof Node) {
      final Node node = (Node)locatable;

      if (node.hasProperty(Constants.ORIGINAL)) {
        locatable = (Locatable)node.getProperty(Constants.ORIGINAL);
      }
    }

    if (locatable.hasLocation()) {
      final Location loc = locatable.getLocation();
      p(loc.file).p(':').p(loc.line).p(':').p(loc.column);
    }

    return this;
  }

  /**
   * Line this printer up at the specified locatable object's
   * location.
   *
   * @param locatable The locatable object.
   * @return This printer.
   */
  public Printer lineUp(Locatable locatable) {
    return lineUp(locatable, 0);
  }

  /**
   * Line this printer up at the specified number of characters before
   * the specified locatable object's location,
   *
   * @param locatable The locatable object.
   * @param before The number of characters before the object.
   * @return This printer.
   */
  public Printer lineUp(Locatable locatable, int before) {
    if (! locatable.hasLocation()) {
      throw new IllegalArgumentException("Locatable without location " +
                                         locatable);
    }

    final Location loc = locatable.getLocation();

    if (0 > loc.column - before) {
      throw new IllegalArgumentException("Invalid character distance " + before);
    }

    if (loc.line > line) {
      for (int i=0; i<loc.line-line; i++) pln();
      for (int i=0; i<loc.column-before; i++) p(' ');

    } else if ((loc.line == line) && (loc.column-before >= column)) {
      for (int i=0; i<loc.column-before-column; i++) p(' ');

    } else {
      p(' ');
    }

    return this;
  }

  // ========================================================================

  /**
   * Flush the underlying print writer.
   *
   * @return This printer.
   */
  public Printer flush() {
    out.flush();
    return this;
  }

  /**
   * Close this printer.  This method stops buffering (if this printer
   * was buffering) and then closes the underlying print writer.
   */
  public void close() {
    stopBuffering();
    out.close();
  }

}
