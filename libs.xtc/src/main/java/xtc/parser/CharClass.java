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
package xtc.parser;

import java.io.IOException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import xtc.util.Utilities;

/**
 * A character class terminal.
 *
 * <p />Note that {@link #equals(Object)} only determines whether the
 * two character class terminals have the same structure (that is, are
 * both exclusive or non-exclusive and have the same list of character
 * ranges), but does not determine whether the two character class
 * terminals recognize the same characters.
 *
 * @author Robert Grimm
 * @version $Revision: 1.18 $
 */
public class CharClass extends CharTerminal {

  /** Parser for a character class specification. */
  public static class Parser {

    /** The string. */
    protected String s;

    /** The index into the string. */
    protected int    idx;

    /**
     * Create a new character class parser for the specified string.
     * Note that the string must not include the leading
     * '<code>[</code>' and trailing '<code>]</code>' characters.
     *
     * @param s The string to parse.
     */
    public Parser(String s) {
      this.s = s;
      idx    = 0;
    }

    /**
     * Determine whether there are more characters.
     *
     * @return <code>true</code> if there are more characters.
     */
    public boolean hasNext() {
      return idx < s.length();
    }

    /**
     * Determine whether the next character is a range delimiter
     * '<code>-</code>'.  Note that this test is <i>destructive</i>:
     * if the next character is a range delimiter, it is consumed.
     *
     * @return <code>true</code> if the next character is a range
     *   delimiter.
     */
    public boolean hasRange() {
      if (idx >= s.length()) {
        return false;
      }

      char c = s.charAt(idx);

      if ('-' == c) {
        idx++;
        return true;
      } else {
        return false;
      }
    }

    /**
     * Return the next character.  If the character is represented by
     * an escape sequence (including Java Unicode and regex-like
     * escapes), it is unescaped.
     *
     * @return The next character.
     */
    public char next() {
      char c = s.charAt(idx);
      idx++;

      if ('\\' != c) {
        return c;

      } else {
        c = s.charAt(idx);
        idx++;

        switch (c) {
        case 'b':
          return '\b';
        case 't':
          return '\t';
        case 'n':
          return '\n';
        case 'f':
          return '\f';
        case 'r':
          return '\r';
        case '"':
          return '"';
        case '\'':
          return '\'';
        case '-':
          return '-';
        case '[':
          return '[';
        case '\\':
          return '\\';
        case ']':
          return ']';
        case 'u':
          idx += 4;
          int n;
          try {
            n = Integer.parseInt(s.substring(idx-4, idx), 16);
          } catch (NumberFormatException x) {
            throw new IllegalArgumentException("Illegal Unicode escape (\'\\u"
                                               + s.substring(idx-4, idx)
                                               + "\')");
          }
          return (char)n;
        default:
          throw new IllegalArgumentException("Illegal character escape (\'\\"
                                             + c + "\')");
        }
      }
    }

  }

  /** The flag for whether the character class is exclusive. */
  public boolean exclusive;

  /**
   * The list of character ranges.  Note that, strictly speaking, this
   * should be a set of disjoint character ranges.  However, it is
   * implemented as a list so that a character class can be printed as
   * it was specified.
   */
  public List<CharRange> ranges;

  /**
   * Create a new, non-exclusive character class.
   *
   * @param ranges The list of character ranges.
   */
  public CharClass(List<CharRange> ranges) {
    this(false, ranges);
  }

  /**
   * Create a new character class.
   *
   * @param exclusive The exclusive flag.
   * @param ranges The list of character ranges.
   */
  public CharClass(boolean exclusive, List<CharRange> ranges) {
    this.exclusive = exclusive;
    this.ranges    = ranges;
  }

  /**
   * Create a new, non-exclusive character class for the specified
   * character.
   *
   * @param c The character.
   */
  public CharClass(char c) {
    exclusive = false;
    ranges    = new ArrayList<CharRange>(1);
    ranges.add(new CharRange(c));
  }

  /**
   * Create a new, non-exclusive character class based on the supplied
   * character class specification.  Note that the character class
   * specification must not include the leading '<code>[</code>' and
   * trailing '<code>]</code>' characters.
   *
   * @param s The character class specification.
   */
  public CharClass(String s) {
    exclusive = false;
    ranges    = new ArrayList<CharRange>();
    Parser p  = new Parser(s);

    while (p.hasNext()) {
      char c1 = p.next();
      char c2 = (p.hasRange())? p.next() : c1;
      ranges.add(new CharRange(c1, c2));
    }
  }

  public Tag tag() {
    return Tag.CHAR_CLASS;
  }

  /**
   * Normalize this character class.  This method sorts the list of
   * character ranges by each range's first character and combines
   * adjacent or overlapping ranges.  However, it does <i>not</i> turn
   * exclusive character classes into non-exclusive ones (as that
   * conversion might negatively impact recognition performance).
   *
   * @return This character class.
   */
  public CharClass normalize() {
    Collections.sort(ranges);

    for (int i=0; i<ranges.size()-1; i++) {
      CharRange r1 = ranges.get(i);
      CharRange r2 = ranges.get(i+1);
      if (r1.last >= r2.last) {
        ranges.remove(i+1);
        i--;
      } else if (r1.last >= r2.first - 1) {
        ranges.set(i, new CharRange(r1.first, r2.last));
        ranges.remove(i+1);
        i--;
      }
    }

    return this;
  }

  /**
   * Determine whether this character class overlaps the specified
   * character class.  Two character classes overlap if they have
   * common characters, though they need not necessarily be the same.
   *
   * @param klass The other character class.
   * @return <code>true</code> if the two character classes overlap.
   * @throws IllegalStateException
   *   Signals that either this or the specified class is exclusive.
   */
  public boolean overlaps(CharClass klass) {
    if (exclusive) {
      throw new IllegalStateException("overlap test for exclusive character " +
                                      "class " + this);
    } else if (klass.exclusive) {
      throw new IllegalStateException("overlap test for exclusive character " +
                                      "class " + klass);
    }

    for (CharRange r1 : klass.ranges) {
      for (CharRange r2 : ranges) {
        if (r1.contains(r2.first) || r1.contains(r2.last) ||
            r2.contains(r1.first) || r2.contains(r1.last)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Determine the number of characters covered by this character
   * class.  Note that for exclusive character classes this method
   * returns the number of <i>excluded</i> characters.
   *
   * @return The number of characters for this character class.
   */
  public int count() {
    int count = 0;
    for (CharRange r : ranges) {
      count += r.count();
    }
    return count;
  }

  public int hashCode() {
    int hash = 0;
    for (CharRange r : ranges) {
      hash += r.hashCode();
    }
    return hash;
  }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (! (o instanceof CharClass)) return false;
    CharClass other = (CharClass)o;
    if (exclusive != other.exclusive) return false;
    if (ranges.size() != other.ranges.size()) return false;
    return ranges.containsAll(other.ranges);
  }

  public void write(Appendable out) throws IOException {
    if (exclusive) {
      out.append('!');
    }

    out.append('[');
    for (CharRange r : ranges) {
      if (r.first == r.last) {
        Utilities.escape(r.first, out, Utilities.FULL_ESCAPES);
      } else {
        Utilities.escape(r.first, out, Utilities.FULL_ESCAPES);
        out.append('-');
        Utilities.escape(r.last,  out, Utilities.FULL_ESCAPES);
      }
    }
    out.append(']');

    if (exclusive) {
      out.append(" _");
    }
  }

}
