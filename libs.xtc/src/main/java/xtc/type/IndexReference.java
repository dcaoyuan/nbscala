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
 * Representation of an index reference.
 *
 * @author Robert Grimm
 * @version $Revision: 1.8 $
 */
public class IndexReference extends RelativeReference {

  /** The index. */
  final BigInteger index;

  /**
   * Create a new index reference.
   *
   * @param base The base reference.
   * @param index The index.
   */
  public IndexReference(Reference base, BigInteger index) {
    super(base.type, base);
    this.index = index;
  }

  /**
   * Create a new index reference.
   *
   * @param type The type.
   * @param base The base.
   * @param index The index.
   */
  IndexReference(Type type, Reference base, BigInteger index) {
    super(type, base);
    this.index = index;
  }

  public boolean isNull() {
    return 0 == index.signum() ? base.isNull() : false;
  }

  public boolean hasIndex() {
    return true;
  }

  public BigInteger getIndex() {
    return index;
  }

  public boolean hasLocation() {
    return base.hasLocation();
  }

  public BigInteger getLocation(C ops) {
    if (! base.hasLocation()) throw new IllegalStateException();

    return base.getLocation(ops).
      add(index.multiply(BigInteger.valueOf(ops.getSize(type))));
  }

  public Reference add(BigInteger val) {
    if (val.signum() == 0) {
      return this;
    } else {
      return new IndexReference(type, base, index.add(val));
    }
  }

  public Reference subtract(BigInteger val) {
    if (val.signum() == 0) {
      return this;
    } else {
      return new IndexReference(type, base, index.subtract(val));
    }
  }

  public int hashCode() {
    return index.hashCode();
  }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (! (o instanceof IndexReference)) return false;
    IndexReference other = (IndexReference)o;
    return (this.index.equals(other.index) &&
            this.base.equals(other.base) &&
            this.type.equals(other.type));
  }

  /** The minimum limit for decimal printouts. */
  private static final BigInteger MIN = BigInteger.valueOf(-255);

  /** The maximum limit for decimal printouts. */
  private static final BigInteger MAX = BigInteger.valueOf(255);

  public void write(Appendable out) throws IOException {
    if (base.isPrefix()) out.append('(');
    base.write(out);
    if (base.isPrefix()) out.append(')');
    out.append('[');
    if ((MIN.compareTo(index) <= 0) && (index.compareTo(MAX) <= 0)) {
      out.append(index.toString());
    } else if (-1 == index.signum()) {
      out.append("-0x");
      out.append(index.negate().toString(16));
    } else {
      out.append("0x");
      out.append(index.toString(16));
    }
    out.append(']');
  }

}
