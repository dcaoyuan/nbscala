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

import java.io.IOException;

/**
 * An array type.  An array can either be of fixed or variable length,
 * with a length of <code>-1</code> indicating that a fixed length
 * array is incomplete.
 *
 * @author Robert Grimm
 * @version $Revision: 1.40 $
 */
public class ArrayT extends DerivedT {

  /** The element type. */
  private Type type;

  /** The flag for whether the array is variable length. */
  private boolean varlength;

  /** The length. */
  private long length;

  /**
   * Create a new, incomplete array type.
   *
   * @param type The element type.
   */
  public ArrayT(Type type) {
    this(type, -1);
  }

  /**
   * Create a new variable length array.
   *
   * @param type The element type.
   * @param varlength The flag for whether this array is of variable
   *   length, which must be <code>true</code>.
   */
  public ArrayT(Type type, boolean varlength) {
    this.type      = type;
    this.varlength = varlength;
    this.length    = -1;
  }

  /**
   * Create a new array type.
   *
   * @param type The element type.
   * @param length The length.
   */
  public ArrayT(Type type, long length) {
    this.type      = type;
    this.varlength = false;
    this.length    = length;
  }

  /**
   * Create a new array type.
   *
   * @param template The type whose annotations to copy.
   * @param type The element type.
   * @param varlength The flag for whether this array is variable.
   * @param length The length.
   */
  public ArrayT(Type template, Type type, boolean varlength, long length) {
    super(template);
    this.type      = type;
    this.varlength = varlength;
    this.length    = length;
  }

  public Type seal() {
    if (! isSealed()) {
      super.seal();
      type.seal();
    }
    return this;
  }

  public ArrayT copy() {
    return new ArrayT(this, type, varlength, length);
  }

  public Type.Tag tag() {
    return Type.Tag.ARRAY;
  }

  public boolean isArray() {
    return true;
  }

  public ArrayT toArray() {
    return this;
  }

  /**
   * Get the element type.
   *
   * @return The element type.
   */
  public Type getType() {
    return type;
  }

  /**
   * Determine whether the array is of variable length.
   *
   * @return <code>true</code> if this array is variable length.
   */
  public boolean isVarLength() {
    return varlength;
  }

  /**
   * Set the variable length flag.
   *
   * @param varlength The variable length flag.
   * @throws IllegalStateException Signals that this type is sealed.
   */
  public void setVarLength(boolean varlength) {
    checkNotSealed();
    this.varlength = varlength;
  }

  /**
   * Determine whether this array has a length.
   *
   * @return <code>true</code> if this array has a length.
   */
  public boolean hasLength() {
    return -1 != length;
  }

  /**
   * Get the length.
   *
   * @return The length or <code>-1</code> if this type is either of
   *   variable length or incomplete.
   */
  public long getLength() {
    return length;
  }

  /**
   * Set the length.
   *
   * @param length The length.
   * @throws IllegalStateException Signals that this type is sealed.
   */
  public void setLength(long length) {
    checkNotSealed();
    this.length = length;
  }

  public int hashCode() {
    return type.hashCode();
  }

  public boolean equals(Object o) {
    if (! (o instanceof Type)) return false;
    Type t = resolve(o);

    if (this == t) return true;
    if (! t.isArray()) return false;
    ArrayT other = (ArrayT)t;

    if (this.varlength != other.varlength) return false;
    if (this.length != other.length) return false;
    return this.type.equals(other.type);
  }

  public void write(Appendable out) throws IOException {
    out.append("array(");
    type.write(out);
    if (varlength) {
      out.append(", *");
    } else if (-1 != length) {
      out.append(", ");
      out.append(Long.toString(length));
    }
    out.append(')');
  }

}
