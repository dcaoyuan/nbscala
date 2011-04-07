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
package xtc.tree;

import java.io.IOException;

/**
 * The location in a source file.
 *
 * @author Robert Grimm
 * @version $Revision: 1.12 $
 */
public class Location implements Comparable {

  /** The file name. */
  public final String file;

  /** The line number. */
  public final int line;

  /** The column. */
  public final int column;

  /** The offset. */
  public final int offset;

  /** The end offset. */
  public final int endOffset;

  /**
   * Create a new location.
   *
   * @param file The file name.
   * @param line The line number.
   * @param column The column.
   * @param offset The offset.
   * @param endOffset The end offset.
   */
  public Location(String file, int line, int column, int offset, int endOffset) {
    this.file   = file;
    this.line   = line;
    this.column = column;
    this.offset    = offset;
    this.endOffset = endOffset;
  }

  public int hashCode() {
    return file.hashCode() + line * 7 + column;
  }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (! (o instanceof Location)) return false;
    Location other = (Location)o;
    if (! file.equals(other.file)) return false;
    if (line != other.line) return false;
    return column == other.column;
  }

  public int compareTo(Object o) {
    Location other = (Location)o;

    if (! file.equals(other.file)) {
      return file.compareTo(other.file);
    } else if (line != other.line) {
      return (line < other.line ? -1 : 1);
    } else {
      return (column < other.column ? -1 : (column == other.column ? 0 : 1));
    }
  }

  /**
   * Write this location to the specified appenable.
   *
   * @param out The appendable.
   * @throws IOException Signals an I/O error.
   */
  public void write(Appendable out) throws IOException {
    out.append(file);
    out.append(':');
    out.append(Integer.toString(line));
    out.append(':');
    out.append(Integer.toString(column));
  }

  public String toString() {
    StringBuilder buf = new StringBuilder();

    buf.append(file);
    buf.append(':');
    buf.append(line);
    buf.append(':');
    buf.append(column);

    return buf.toString();
  }    

}
