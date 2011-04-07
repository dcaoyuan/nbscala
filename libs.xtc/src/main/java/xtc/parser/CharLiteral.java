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

import xtc.util.Utilities;

/**
 * A character literal. 
 *
 * @author Robert Grimm
 * @version $Revision: 1.11 $
 */
public class CharLiteral extends CharTerminal {

  /** The character. */
  public final char c;

  /**
   * Create a new character literal with the specified character.
   *
   * @param c The character.
   */
  public CharLiteral(char c) {
    this.c = c;
  }

  public Tag tag() {
    return Tag.CHAR_LITERAL;
  }

  public int hashCode() {
    return c;
  }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (! (o instanceof CharLiteral)) return false;
    return (c == ((CharLiteral)o).c);
  }

  public void write(Appendable out) throws IOException {
    out.append('\'');
    Utilities.escape(c, out, Utilities.JAVA_ESCAPES);
    out.append('\'');
  }

}
