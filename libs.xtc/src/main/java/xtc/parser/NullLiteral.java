/*
 * xtc - The eXTensible Compiler
 * Copyright (C) 2007 Robert Grimm
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
 * A null literal representing a bindable null value.
 *
 * @author Robert Grimm
 * @version $Revision: 1.2 $
 */
public class NullLiteral extends Literal {

  /** Create a new null literal. */
  public NullLiteral() { /* Nothing to do. */ }

  public Tag tag() {
    return Tag.NULL;
  }

  public int hashCode() {
    return 0;
  }

  public boolean equals(Object o) {
    if (this == o) return true;
    return o instanceof NullLiteral;
  }

  public void write(Appendable out) throws IOException {
    out.append("null");
  }

  public String toString() {
    return "null";
  }

}
