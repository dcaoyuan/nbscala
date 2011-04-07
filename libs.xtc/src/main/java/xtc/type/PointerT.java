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
 * A pointer type.
 *
 * @author Robert Grimm
 * @version $Revision: 1.31 $
 */
public class PointerT extends DerivedT {

  /** The canonical pointer to void. */
  public static final PointerT TO_VOID = new PointerT(VoidT.TYPE);

  static {
    TO_VOID.seal();
  }

  /** The pointed-to type. */
  private Type type;

  /**
   * Create a new pointer type.
   *
   * @param type The pointed-to type.
   */
  public PointerT(Type type) {
    this.type = type;
  }

  /**
   * Create a new pointer type.
   *
   * @param template The type whose annotations to copy.
   * @param type The pointed-to type.
   */
  public PointerT(Type template, Type type) {
    super(template);
    this.type = type;
  }

  public PointerT copy() {
    return new PointerT(this, type.copy());
  }

  public Type seal() {
    if (! isSealed()) {
      super.seal();
      type.seal();
    }
    return this;
  }

  public Type.Tag tag() {
    return Type.Tag.POINTER;
  }

  public boolean isPointer() {
    return true;
  }

  public PointerT toPointer() {
    return this;
  }

  /**
   * Get the pointed-to type.
   *
   * @return The pointed-to type.
   */
  public Type getType() {
    return type;
  }

  public int hashCode() {
    return type.hashCode();
  }

  public boolean equals(Object o) {
    if (! (o instanceof Type)) return false;
    Type t = resolve(o);

    if (this == t) return true;
    if (! t.isPointer()) return false;
    return type.equals(((PointerT)t).type);
  }

  public void write(Appendable out) throws IOException {
    out.append("pointer(");
    type.write(out);
    out.append(')');
  }

}
