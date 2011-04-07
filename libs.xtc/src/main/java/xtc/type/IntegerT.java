/*
 * xtc - The eXTensible Compiler
 * Copyright (C) 2005-2007 Robert Grimm
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

import xtc.Limits;

/**
 * An integer type.
 *
 * @author Robert Grimm
 * @version $Revision: 1.36 $
 */
public class IntegerT extends NumberT {

  /**
   * Create a new integer type.
   *
   * @param kind The kind.
   * @throws IllegalArgumentException Signals an invalid kind.
   */
  public IntegerT(Kind kind) {
    this(null, kind);
  }

  /**
   * Create a new integer type.
   *
   * @param template The type whose annotations to copy.
   * @param kind The kind.
   * @throws IllegalArgumentException Signals an invalid kind.
   */
  public IntegerT(Type template, Kind kind) {
    super(template, kind);
    switch (kind) {
    case FLOAT:
    case DOUBLE:
    case LONG_DOUBLE:
    case FLOAT_COMPLEX:
    case DOUBLE_COMPLEX:
    case LONG_DOUBLE_COMPLEX:
      throw new IllegalArgumentException("Not an integer kind " + kind);
    default:
      // All is well.
    }
  }

  public IntegerT copy() {
    return new IntegerT(this, kind);
  }

  public Type.Tag tag() {
    return Type.Tag.INTEGER;
  }

  public boolean isInteger() {
    return true;
  }

  public IntegerT toInteger() {
    return this;
  }

  /**
   * Convert the specified rank to the corresponding integer kind.
   * The rank reflects the ordering <code>char</code>,
   * <code>short</code>, <code>int</code>, <code>long</code>, and
   * <code>long long</code>, starting at 1 and ignoring the sign.
   *
   * @param rank The rank.
   * @param signed The flag for a signed integer.
   * @return The corresponding kind.
   * @throws IllegalArgumentException Signals an invalid rank.
   */
  public static Kind fromRank(int rank, boolean signed) {
    switch (rank) {
    case 1:
      if (signed) {
        return Limits.IS_CHAR_SIGNED ? Kind.CHAR : Kind.S_CHAR ;
      } else {
        return Limits.IS_CHAR_SIGNED ? Kind.U_CHAR : Kind.CHAR ;
      }
    case 2:
      return signed ? Kind.SHORT : Kind.U_SHORT;
    case 3:
      return signed ? Kind.INT : Kind.U_INT;
    case 4:
      return signed ? Kind.LONG : Kind.U_LONG;
    case 5:
      return signed ? Kind.LONG_LONG : Kind.U_LONG_LONG;
    default:
      throw new IllegalArgumentException("Invalid rank: " + rank);
    }
  }

}
