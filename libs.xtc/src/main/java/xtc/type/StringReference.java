/*
 * xtc - The eXTensible Compiler
 * Copyright (C) 2006-2007 Robert Grimm
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
package xtc.type;

import java.io.IOException;

import xtc.util.Utilities;

/**
 * Representation of a reference to a constant string.  A string
 * reference neither has a base nor an offset.
 *
 * @author Robert Grimm
 * @version $Revision: 1.9 $
 */
public class StringReference extends Reference {

  /** The string. */
  private final String literal;

  /**
   * Create a new string reference.  The specified type must be a
   * valid character type.
   *
   * @param literal The string literal.
   * @param type The literal's declared type.
   * @throws IllegalArgumentException Signals that the type is not a
   *   string type.
   */
  public StringReference(String literal, Type type) {
    super(type);
    this.literal = literal;
  }

  public boolean isString() {
    return true;
  }

  /**
   * Get this string reference's literal.
   *
   * @return The literal.
   */
  public String getLiteral() {
    return literal;
  }

  public boolean isConstant() {
    return true;
  }

  public int hashCode() {
    return literal.hashCode();
  }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (! (o instanceof StringReference)) return false;
    StringReference other = (StringReference)o;
    return (this.type.equals(other.type) &&
            this.literal.equals(other.literal));
  }

  public void write(Appendable out) throws IOException {
    // If the type is not a character, we assume it's a wide character.
    if (type.hasTag(Type.Tag.INTEGER)) {
      switch (type.resolve().toInteger().getKind()) {
      default:
        out.append('L');
        break;
      case CHAR:
      case S_CHAR:
      case U_CHAR:
        // Nothing to do.
      }
    }

    out.append('"');
    Utilities.escape(literal, out, Utilities.C_ESCAPES);
    out.append('"');
  }

}
