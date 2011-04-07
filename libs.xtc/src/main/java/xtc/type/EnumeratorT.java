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

import java.math.BigInteger;

/**
 * An enumerator.  An enumerator's type can be set after creation to
 * support the deduction of the overall enum's type after all
 * enumerators have been seen (C99 6.7.2.2).
 *
 * @author Robert Grimm
 * @version $Revision: 1.39 $
 */
public class EnumeratorT extends WrappedT {

  /** The name. */
  private String name;

  /**
   * Create a new enumerator.  The specified type should be an {@link
   * IntegerT integer} or {@link ErrorT error} type.
   *
   * @param type The type.
   * @param name The name.
   * @param value The value.
   */
  public EnumeratorT(Type type, String name, BigInteger value) {
    super(type);
    this.name  = name;
    constant(value);
  }

  /**
   * Create a new enumerator.
   *
   * @param template The type whose annotations to copy.
   * @param type The type.
   * @param name The name.
   * @param value The value.
   */
  public EnumeratorT(Type template, Type type, String name, BigInteger value) {
    super(template, type);
    this.name    = name;
    constant(value);
  }

  public EnumeratorT copy() {
    return new EnumeratorT(this, getType().copy(), name, constant.bigIntValue());
  }

  public Type.Tag wtag() {
    return Type.Tag.ENUMERATOR;
  }

  public boolean isEnumerator() {
    return true;
  }

  public boolean hasEnumerator() {
    return true;
  }

  public EnumeratorT toEnumerator() {
    return this;
  }

  /**
   * Determine whether this enumerator has the specified name.
   *
   * @param name The name.
   * @return <code>true</code> if this enumerator has the name.
   */
  public boolean hasName(String name) {
    return name.equals(this.name);
  }

  /**
   * Get the name.
   *
   * @return The name.
   */
  public String getName() {
    return name;
  }

  /**
   * Get the value.
   *
   * @return The value.
   */
  public BigInteger getValue() {
    return constant.bigIntValue();
  }

  public void write(Appendable out) throws IOException {
    out.append(name);
  }

}
