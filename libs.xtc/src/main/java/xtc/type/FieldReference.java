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

import java.math.BigInteger;

/**
 * Representation of a field reference.
 *
 * @author Robert Grimm
 * @version $Revision: 1.7 $
 */
public class FieldReference extends RelativeReference {

  /** The field name. */
  private final String name;

  /**
   * Create a new field reference.  The specified base reference must
   * have a struct or union type with a member of the specified name.
   * The type of the newly created field reference is the named
   * member's type, unless that type is an array, in which case the
   * type is the array's element type.
   *
   * @param base The base reference.
   * @param name The member name.
   * @throws ClassCastException Signals that the base reference is
   *   does not have a struct or union.
   * @throws IllegalArgumentException Signals that the base reference
   *   does not have a struct/union with the named member.
   */
  public FieldReference(Reference base, String name) {
    super(base.type.toStructOrUnion().lookup(name), base);
    this.name = name;

    if (type.isError()) {
      throw new IllegalArgumentException("struct/union without member '" +
                                         name + "'");
    }
  }

  public boolean hasField() {
    return true;
  }

  public String getField() {
    return name;
  }

  public boolean hasLocation() {
    return base.hasLocation();
  }

  public BigInteger getLocation(C ops) {
    if (! base.hasLocation()) throw new IllegalStateException();

    return base.getLocation(ops).
      add(BigInteger.valueOf(ops.getOffset(base.type.toStructOrUnion(), name)));
  }

  public int hashCode() {
    return name.hashCode();
  }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (! (o instanceof FieldReference)) return false;
    FieldReference other = (FieldReference)o;
    return this.name.equals(other.name) && this.base.equals(other.base);
  }

  public void write(Appendable out) throws IOException {
    if (base.isPrefix()) out.append('(');
    base.write(out);
    if (base.isPrefix()) out.append(')');
    out.append('.');
    out.append(name);
  }

}
