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

/**
 * A floating point type.
 *
 * @author Robert Grimm
 * @version $Revision: 1.23 $
 */
public class FloatT extends NumberT {

  /**
   * Create a new floating point type.
   *
   * @param kind The kind.
   * @throws IllegalArgumentException Signals an invalid kind.
   */
  public FloatT(Kind kind) {
    this(null, kind);
  }

  /**
   * Create a new flaoting point type.
   *
   * @param template The type whose annotations to copy.
   * @param kind The kind.
   * @throws IllegalArgumentException Signals an invalid kind.
   */
  public FloatT(Type template, Kind kind) {
    super(template, kind);
    switch (kind) {
    case FLOAT:
    case DOUBLE:
    case LONG_DOUBLE:
    case FLOAT_COMPLEX:
    case DOUBLE_COMPLEX:
    case LONG_DOUBLE_COMPLEX:
      // All is well.
      break;
    default:
      throw new IllegalArgumentException("Not a float kind " + kind);
    }
  }

  public FloatT copy() {
    return new FloatT(this, kind);
  }

  public Type.Tag tag() {
    return Type.Tag.FLOAT;
  }

  public boolean isFloat() {
    return true;
  }

  public FloatT toFloat() {
    return this;
  }

}
