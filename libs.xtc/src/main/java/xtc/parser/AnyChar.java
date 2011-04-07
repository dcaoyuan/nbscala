/*
 * xtc - The eXTensible Compiler
 * Copyright (C) 2004-2008 Robert Grimm
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

/**
 * The any character element.
 *
 * @author Robert Grimm
 * @version $Revision: 1.10 $
 */
public class AnyChar extends CharTerminal {

  /** Create a new any character element. */
  public AnyChar() { /* Nothing to do. */ }

  public Tag tag() {
    return Tag.ANY_CHAR;
  }

  public int hashCode() {
    return 3;
  }

  public boolean equals(Object o) {
    return o instanceof AnyChar;
  }

  public void write(Appendable out) throws IOException {
    out.append('_');
  }

  public String toString() {
    return "_";
  }

}
