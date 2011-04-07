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

/**
 * A line marker (as used by GCC).
 *
 * @author Robert Grimm
 * @version $Revision: 1.9 $
 */
public class LineMarker extends Annotation {

  /** The start file flag. */
  public static final int FLAG_START_FILE = 0x01;

  /** The return to file flag. */
  public static final int FLAG_RETURN_TO_FILE = 0x02;

  /** The system header flag. */
  public static final int FLAG_SYSTEM_HEADER = 0x04;

  /** The extern C flag. */
  public static final int FLAG_EXTERN_C = 0x08;

  /** The line number. */
  public int line;

  /** The file name. */
  public String file;

  /** The flags. */
  public int flags;

  /**
   * Create a new line marker.
   *
   * @param line The line number.
   * @param file The file name.
   * @param flags The flags.
   * @param node The marked node.
   */
  public LineMarker(int line, String file, int flags, Node node) {
    super(node);
    this.line  = line;
    this.file  = file;
    this.flags = flags;
  }

  public boolean hasTraversal() {
    return true;
  }

  public int size() {
    return 4;
  }

  public Object get(int index) {
    switch (index) {
    case 0:
      return line;
    case 1:
      return file;
    case 2:
      return flags;
    case 3:
      return node;
    default:
      throw new IndexOutOfBoundsException("Index: " + index + ", Size: 4");
    }
  }

  public Object set(int index, Object value) {
    Object old;

    switch (index) {
    case 0:
      old  = line;
      line = ((Number)value).intValue();
      return old;
    case 1:
      old  = file;
      file = (String)value;
      return old;
    case 2:
      old   = flags;
      flags = ((Number)value).intValue();
      return old;
    case 3:
      old   = node;
      node  = (Node)value;
      return old;
    default:
      throw new IndexOutOfBoundsException("Index: " + index + ", Size: 4");
    }
  }

  public int hashCode() {
    return line;
  }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (! (o instanceof LineMarker)) return false;
    LineMarker other = (LineMarker)o;
    if (line != other.line) return false;
    if (flags != other.flags) return false;
    if (null == file) return (null == other.file);
    if (! file.equals(other.file)) return false;
    if (null == node) return (null == other.node);
    return node.equals(other.node);
  }

}
