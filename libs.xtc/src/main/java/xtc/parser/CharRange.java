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
package xtc.parser;

import xtc.tree.Node;

/**
 * A character range for a character classs.
 *
 * @see CharClass
 *
 * @author Robert Grimm
 * @version $Revision: 1.10 $
 */
public class CharRange extends Node implements Comparable<CharRange> {

  /** The first character. */
  public final char first;

  /** The last character. */
  public final char last;

  /**
   * Create a new single-character range with the specified character.
   *
   * @param c The character.
   */
  public CharRange(final char c) {
    this(c, c);
  }

  /**
   * Create a new character range with the specified characters.
   *
   * @param first The first character.
   * @param last The last character.
   */
  public CharRange(final char first, final char last) {
    if (first > last) {
      this.first = last;
      this.last  = first;
    } else {
      this.first = first;
      this.last  = last;
    }
  }

  /**
   * Determine the number of characters covered by this character
   * range.
   *
   * @return The number of characters for this character range.
   */
  public int count() {
    return (last - first + 1);
  }

  /**
   * Determine whether this character range contains the specified
   * character.
   *
   * @param c The character.
   * @return <code>true</code> if this character range contains
   *   the specified character.
   */
  public boolean contains(final char c) {
    return ((first <= c) && (c <= last));
  }

  public int hashCode() {
    if (first == last) {
      return first;
    } else {
      return first + last;
    }
  }

  public boolean equals(final Object o) {
    if (this == o) return true;
    if (! (o instanceof CharRange)) return false;
    CharRange other = (CharRange)o;
    if (first != other.first) return false;
    return (last == other.last);
  }

  public int compareTo(final CharRange other) {
    return this.first - other.first;
  }

}
