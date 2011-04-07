/*
 * xtc - The eXTensible Compiler
 * Copyright (C) 2006-2007 Robert Grimm
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

/**
 * A token.  A token is an immutable tree node containing a source
 * file symbol.
 *
 * @author Robert Grimm
 * @version $Revision: 1.7 $
 */
public final class Token extends Node {

  /** The text. */
  private final String text;

  /**
   * Create a new token.
   *
   * @param text The text.
   */
  public Token(String text) {
    this.text = text;
  }

  public boolean isToken() {
    return true;
  }

  public Token toToken() {
    return this;
  }

  public String getTokenText() {
    return text;
  }

  public boolean hasTraversal() {
    return true;
  }

  public int size() {
    return 1;
  }

  public Object get(int index) {
    if (0 == index) {
      return text;
    } else {
      throw new IndexOutOfBoundsException("Index: " + index + ", Size: 1");
    }
  }

  public Object set(int index, Object value) {
    throw new IllegalStateException("Not modifiable");
  }

  // ========================================================================

  /**
   * Determine whether the specified object represents a string.
   *
   * @param o The object.
   * @return <code>true</code> if the specifed object is a string or a
   *   possibly annotated token.
   */
  public static final boolean test(Object o) {
    return ((o instanceof String) ||
            ((o instanceof Node) && ((Node)o).strip().isToken()));
  }

  /**
   * Cast the specified object to a string.  If the specified object
   * is a string, this method simply returns the string.  Otherwise,
   * it casts the object to a node, strips all annotations, and then
   * returns the resulting token's text.
   *
   * @see #test(Object)
   *
   * @param o The object.
   * @return The corresponding string.
   * @throws ClassCastException Signals that the object does not
   *   describe a string.
   */
  public static final String cast(Object o) {
    if (null == o) {
      return null;
    } else if (o instanceof String) {
      return (String)o;
    } else {
      return ((Node)o).getTokenText();
    }
  }

}
