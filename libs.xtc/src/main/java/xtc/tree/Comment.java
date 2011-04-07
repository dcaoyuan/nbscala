/*
 * xtc - The eXTensible Compiler
 * Copyright (C) 2004-2006 Robert Grimm
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

import java.util.ArrayList;
import java.util.List;

import xtc.util.Utilities;

/**
 * A source code comment.
 *
 * @author Robert Grimm
 * @version $Revision: 1.11 $
 */
public class Comment extends Annotation {

  /** The kind of comment. */
  public static enum Kind {
    /** A single line. */ SINGLE_LINE,
    /** Multiple lines. */ MULTIPLE_LINES,
    /** Documentation. */ DOCUMENTATION
  }

  /** The kind. */
  public Kind kind;

  /** The actual text, one line per list entry. */
  public List<String> text;

  /**
   * Create a new comment.
   *
   * @param kind The kind.
   * @param text The text.
   */
  public Comment(Kind kind, List<String> text) {
    this(kind, text, null);
  }

  /**
   * Create a new comment.
   *
   * @param kind The kind.
   * @param text The text.
   * @param node The node.
   */
  public Comment(Kind kind, List<String> text, Node node) {
    super(node);
    this.kind = kind;
    this.text = text;
  }

  public int hashCode() {
    return text.hashCode();
  }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (! (o instanceof Comment)) return false;
    Comment other = (Comment)o;
    if (kind != other.kind) return false;
    if (! text.equals(other.text)) return false;
    if (null == node) return (null == other.node);
    return node.equals(other.node);
  }

  /**
   * Create a new code documentation comment.  If the specified text
   * is <code>null</code>, it is passed through.
   *
   * @param s The text of the comment, including start and end markers.
   * @return The comment.
   */
  public static Comment documentation(String s) {
    // Make sure we have something to work with.
    if (null == s) {
      return null;
    } else if (4 > s.length()) {
      throw new IllegalArgumentException("Invalid documentation comment");
    }

    // Split the text.
    s           = s.substring(3, s.length() - 2);
    String[] ss = Utilities.COMMENT_NEWLINE.split(s);

    // Trim the first and last line.
    if (0 < ss.length) {
      ss[0] = ss[0].trim();
      if (1 != ss.length) {
        ss[ss.length - 1] = ss[ss.length - 1].trim();
      }
    }

    // Find empty lines at beginning and end.
    int start = ss.length;
    int end   = ss.length - 1;
    
    for (int i=0; i<ss.length; i++) {
      if (! "".equals(ss[i])) {
        start = i;
        break;
      }
    }

    for (int i=ss.length-1; i>=0; i--) {
      if (! "".equals(ss[i])) {
        end = i;
        break;
      }
    }

    // Create a new list without any leading or trailing empty lines.
    List<String> l = new ArrayList<String>(end - start + 1);
    for (int i=start; i<=end; i++) {
      l.add(ss[i]);
    }

    // Done.
    return new Comment(Kind.DOCUMENTATION, l);
  }

}
