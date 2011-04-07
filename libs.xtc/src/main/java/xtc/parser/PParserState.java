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

import java.util.ArrayList;
import java.util.List;

import xtc.util.State;

/**
 * Parser state for parsing <i>Rats&#033;</i> grammars.  Note that
 * this class supports only a single state-modifying transaction.  In
 * other words, calls to {@link #start()}, {@link #commit()}, and
 * {@link #abort()} cannot be nested within each other.
 *
 * @author Robert Grimm
 * @version $Revision: 1.5 $
 */
public class PParserState implements State {

  /** The current indentation level. */
  private int level;

  /** The list of indentation levels. */
  private List<Integer> indent;

  /**
   * The flag for whether we have seen anything besides spaces on the
   * current line.
   */
  private boolean content;

  /** Create a new packrat parser state object. */
  public PParserState() {
    reset(null);
  }

  public void reset(String file) {
    level   = -1;
    indent  = null;
  }

  public void start() {
    level   = 0;
    indent  = new ArrayList<Integer>();
    indent.add(level);
    content = false;
  }

  public void commit() {
    level   = -1;
  }

  public void abort() {
    level   = -1;
  }

  /** Record an opening brace. */
  public void open() {
    if (-1 < level) level++;
  }

  /** Record any character besides spaces. */
  public void content() {
    if (-1 < level) content = true;
  }

  /** Record a newline. */
  public void newline() {
    if (-1 < level) {
      indent.add(level);
      content = false;
    }
  }

  /** Record a closing brace. */
  public void close() {
    if (0 < level) {
      // If the closing brace appears at the beginning of the current
      // line (disregarding spaces), we need to also decrement the
      // last captured indentation level.
      if (! content) {
        final int idx   = indent.size() - 1;
        final int value = indent.get(idx) - 1;
        indent.set(idx, value);
      }
      level--;
    }
  }

  /**
   * Retrieve the list of indentation levels.  The list contains one
   * entry per invocation of {@link #newline()} since the last
   * invocation of {@link #start()}.
   *
   * @return The list of indentation levels.
   */
  public List<Integer> indentations() {
    return indent;
  }

}
