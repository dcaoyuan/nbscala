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

import java.util.ArrayList;
import java.util.List;

import xtc.util.Utilities;

/**
 * A semantic action. 
 *
 * @author Robert Grimm
 * @version $Revision: 1.22 $
 */
public class Action extends Element {

  /**
   * The list of source lines for the semantic action.  Note that each
   * source line is a string without the terminating end-of-line
   * character(s).
   */
  public final List<String> code;

  /**
   * The list of indentation levels, one per line of code.  Note that
   * this list must be at least as long as the list of source lines.
   */
  public final List<Integer> indent;

  /**
   * Create a new action with the specified code.  The specified
   * string is broken up into individual lines, removing all
   * end-of-line characters along the way.
   *
   * @param s The code as a string.
   * @param indent The list of indentation levels, one per line
   *   in the specified string.
   * @throws IllegalArgumentException
   *   Signals that <code>indent</code> is too short.
   */
  public Action(String s, List<Integer> indent) {
    String[] ss = Utilities.SPACE_NEWLINE_SPACE.split(s);
    this.indent = indent;

    if (indent.size() < ss.length) {
      throw new
        IllegalArgumentException("List of indentation levels too short");
    }

    // Trim lines and eliminate empty lines at the beginning and end.
    if (0 < ss.length) {
      // We only need to trim the first and last line.
      ss[0] = ss[0].trim();
      if (1 != ss.length) {
        ss[ss.length-1] = ss[ss.length-1].trim();
      }
    }

    // Find empty lines at beginning and end.
    int start = ss.length;
    int end   = ss.length-1;

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

    // Remove empty lines from beginning and end.
    int size  = indent.size();
    for (int i=0; i<start; i++) {
      indent.remove(0);
    }

    for (int i=end+1; i<size; i++) {
      indent.remove(indent.size()-1);
    }

    code = new ArrayList<String>(end - start + 1);
    for (int i=start; i<=end; i++) {
      code.add(ss[i]);
    }
  }

  /**
   * Create a new action with the specified code.
   *
   * @param code The code as a list source lines.
   * @param indent The corresponding indentation levels.
   * @throws IllegalArgumentException
   *   Signals that the number of code lines is inconsistent with the
   *   number of indentation levels.
   */
  public Action(List<String> code, List<Integer> indent) {
    if (indent.size() != code.size()) {
      throw new IllegalArgumentException("Number of code lines and " +
                                         "indentation levels inconsistent");
    }
    this.code   = code;
    this.indent = indent;
  }

  public Tag tag() {
    return Tag.ACTION;
  }

  /**
   * Add the specified action to this action.
   *
   * @param a The action to add.
   */
  public void add(Action a) {
    code.addAll(a.code);
    indent.addAll(a.indent);
  }

  /**
   * Determine whether this action sets the {@link CodeGenerator#VALUE
   * semantic value}.  This method implements a conservative
   * approximation by searching for occurrences of the corresponding
   * variable name.
   *
   * @return <code>true</code> if this action sets the semantic value.
   */
  public boolean setsValue() {
    for (String s : code) {
      if (-1 != s.indexOf(CodeGenerator.VALUE)) {
        return true;
      }
    }
    return false;
  }

  public int hashCode() {
    return code.hashCode();
  }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (! (o instanceof Action)) return false;
    Action other = (Action)o;
    if (! code.equals(other.code)) return false;
    return indent.equals(other.indent);
  }

}
