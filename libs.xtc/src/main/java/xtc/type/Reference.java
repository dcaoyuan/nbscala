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
 * Representation of a reference.  A reference represents a specific
 * region of memory.  It has a type, which determines the layout of
 * the referenced memory region.  Furthermore, it may optionally have
 * a base and an offset, indicating a memory region relative to
 * another.  The optional base is a reference, and the optional offset
 * is either an index or a field.
 *
 * @author Robert Grimm
 * @version $Revision: 1.13 $
 */
public abstract class Reference {

  /** The type. */
  protected final Type type;

  /**
   * Create a new reference.  Note that this constructor resolves the
   * specified type and strips any arrays.
   *
   * @param type The type.
   */
  public Reference(Type type) {
    type = type.resolve();
    while (type.isArray()) {
      type = type.toArray().getType().resolve();
    }

    this.type = type;
  }

  /**
   * Get this reference's type.
   *
   * @return The type.
   */
  public Type getType() {
    return type;
  }

  /**
   * Determine whether this reference is a null reference.  Note that
   * a reference other than {@link NullReference#NULL} may still
   * return <code>true</code>; notably, an index reference with a null
   * base and a zero offset still is null.
   *
   * @see NullReference
   *
   * @return <code>true</code> if this reference is a null
   *   reference.
   */
  public boolean isNull() {
    return false;
  }

  /**
   * Determine whether this reference is a string reference.
   *
   * @see StringReference
   *
   * @return <code>true</code> if this reference is a string
   *   reference.
   */
  public boolean isString() {
    return false;
  }

  /**
   * Determine whether this reference is a variable reference.
   *
   * @see VariableReference
   *
   * @return <code>true</code> if this reference is a variable
   *   reference.
   */
  public boolean isVariable() {
    return false;
  }

  /**
   * Determine whether this reference is a static reference.
   *
   * @see StaticReference
   *
   * @return <code>true</code> if this reference is a static
   *   reference.
   */
  public boolean isStatic() {
    return false;
  }

  /**
   * Determine whether this reference is a dynamic reference.
   *
   * @see DynamicReference
   *
   * @return <code>true</code> if this reference is a dynamic
   *   reference.
   */
  public boolean isDynamic() {
    return false;
  }

  /**
   * Determine whether this reference represents a prefix operator in
   * C syntax.
   *
   * @return <code>true</code> if this reference represents a
   *   prefix operator.
   */
  public boolean isPrefix() {
    return false;
  }

  /**
   * Determine whether this reference is a cast reference.
   *
   * @return <code>true</code> if this reference is a cast
   *   reference.
   */
  public boolean isCast() {
    return false;
  }

  /**
   * Determine whether this reference is an indirect reference.
   *
   * @return <code>true</code> if this reference is an indirect
   *   reference.
   */
  public boolean isIndirect() {
    return false;
  }

  /**
   * Determine whether this reference has a base.
   *
   * @see RelativeReference
   *
   * @return <code>true</code> if this reference has a base.
   */
  public boolean hasBase() {
    return false;
  }

  /**
   * Get this reference's base.
   *
   * @see RelativeReference
   * 
   * @return The base.
   * @throws IllegalStateException Signals that this reference does
   *   not have a base.
   */
  public Reference getBase() {
    throw new IllegalStateException("not a relative reference");
  }

  /**
   * Determine whether this reference has an index.
   *
   * @see IndexReference
   *
   * @return <code>true</code> if this reference has an index.
   */
  public boolean hasIndex() {
    return false;
  }

  /**
   * Get this reference's index.
   *
   * @return The index.
   * @throws IllegalStateException Signals that this reference does
   *   not have an index.
   */
  public BigInteger getIndex() {
    throw new IllegalStateException("not an index reference");
  }

  /**
   * Determine whether this reference has a field.
   *
   * @see FieldReference
   *
   * @return <code>true</code> if this reference has a field.
   */
  public boolean hasField() {
    return false;
  }

  /**
   * Get this reference's field.
   *
   * @return The field.
   * @throws IllegalStateException Signals that this reference does
   *   not have a field.
   */
  public String getField() {
    throw new IllegalStateException("not a field reference");
  }

  /**
   * Determine whether this reference has an absolute memory location.
   *
   * @return <code>true</code> if this reference has an absolute
   *   memory location.
   */
  public boolean hasLocation() {
    return false;
  }

  /**
   * Get this reference's absolute memory location.
   *
   * @param ops The C operations.
   * @return The memory location.
   * @throws IllegalStateException Signals that this reference does
   *   not have an absolute memory location.
   */
  public BigInteger getLocation(C ops) {
    throw new IllegalStateException();
  }

  /**
   * Determine whether this reference represents a compile-time
   * constant memory location.
   *
   * @return <code>true</code> if this reference represents a
   *   compile-time memory location.
   */
  public boolean isConstant() {
    return false;
  }

  /**
   * Indirect this reference.  This method determines the appropriate
   * reference when using a pointer-decayed type.  For arrays and
   * functions, it simly returns this reference.  For all other types,
   * it returns an indirect reference, with this reference as the
   * base.
   *
   * @param type The reference's declared type (before pointer decay).
   */
  public Reference indirect(Type type) {
    Type resolved = type.resolve();
    if (resolved.isArray() || resolved.isFunction()) {
      return this;
    } else {
      return new IndirectReference(this);
    }
  }

  /**
   * Add the specified value to this reference.
   *
   * @param val The value.
   * @return A reference with an index increased by <code>val</code>.
   */
  public Reference add(long val) {
    if (0 == val) {
      return this;
    } else {
      return add(BigInteger.valueOf(val));
    }
  }

  /**
   * Add the specified value to this reference.
   *
   * @param val The value.
   * @return A reference with an index increased by <code>val</code>.
   */
  public Reference add(BigInteger val) {
    if (val.signum() == 0) {
      return this;
    } else {
      return new IndexReference(this, val);
    }
  }

  /**
   * Subtract the specified value from this reference.
   *
   * @param val The value.
   * @return A reference with an index decreased by <code>val</code>.
   */
  public Reference subtract(long val) {
    if (0 == val) {
      return this;
    } else {
      return subtract(BigInteger.valueOf(val));
    }
  }

  /**
   * Subtract the specified value from this reference.
   *
   * @param val The value.
   * @return A reference with an index decreased by <code>val</code>.
   */
  public Reference subtract(BigInteger val) {
    if(val.signum() == 0) {
      return this;
    } else {
      return new IndexReference(this, val.negate());
    }
  }

  /**
   * Determine the difference between the two references.
   *
   * @param ref The other reference.
   * @return The difference or <code>null</code> if the difference
   *   cannot be statically determined.
   */
  public BigInteger difference(Reference ref) {
    if (hasIndex() && ref.hasIndex()) {
      IndexReference r1 = (IndexReference)this;
      IndexReference r2 = (IndexReference)ref;

      if (r1.base.equals(r2.base)) {
        return r1.index.subtract(r2.index);
      }

    } else if (hasIndex()) {
      IndexReference r1 = (IndexReference)this;

      if (r1.base.equals(ref)) {
        return r1.index;
      }

    } else if (ref.hasIndex()) {
      IndexReference r2 = (IndexReference)ref;

      if (this.equals(r2.base)) {
        return r2.index.negate();
      }
    }

    return null;
  }

  /**
   * Write a human readable representation to the specified
   * appendable.
   * 
   * @param out The appendable.
   * @throws IOException Signals an I/O error.
   */
  public abstract void write(Appendable out) throws IOException;

  public String toString() {
    StringBuilder buf = new StringBuilder();
    try {
      write(buf);
    } catch (IOException x) {
      assert false;
    }
    return buf.toString();
  }    

}
