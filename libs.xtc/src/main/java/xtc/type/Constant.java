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

import java.math.BigInteger;

/**
 * Representation of a type's constant value.  A constant value may be
 * one of the following:<ul>
 * <li>A boxed byte, short, int, or long.</li>
 * <li>A big integer.</li>
 * <li>A boxed char.</li>
 * <li>A string.</li>
 * <li>A {@link Reference}.</li>
 * </ul>
 * This class treats a zero as a null value.  It also treats a zero as
 * a boolean false value and any other number as a boolean true value.
 *
 * @author Robert Grimm
 * @version $Revision: 1.11 $
 */
public class Constant {

  /** The kind. */
  public static enum Kind {
    /** An integer. */     INTEGER,
    /** A big integer. */  BIG_INTEGER,
    /** A double. */       DOUBLE,
    /** A character. */    CHARACTER,
    /** A string. */       STRING,
    /** A reference. */    REFERENCE
  }

  // =========================================================================

  /** The kind. */
  private Kind kind;

  /** The value. */
  private Object value;

  /**
   * Create a new constant value.
   *
   * @param value The value.
   * @throws IllegalArgumentException Signals an invalid value.
   */
  public Constant(Object value) {
    this.value = value;
    if (null == value) {
      throw new NullPointerException();

    } else if (value instanceof Number) {
      if ((value instanceof Byte) ||
          (value instanceof Short) ||
          (value instanceof Integer) ||
          (value instanceof Long)) {
        kind = Kind.INTEGER;

      } else if (value instanceof BigInteger) {
        kind = Kind.BIG_INTEGER;

      } else if ((value instanceof Float) ||
                 (value instanceof Double)) {
        kind = Kind.DOUBLE;

      } else {
        throw new IllegalArgumentException("Invalid number " + value);
      }

    } else if (value instanceof Character) {
      kind = Kind.CHARACTER;

    } else if (value instanceof String) {
      kind = Kind.STRING;

    } else if (value instanceof Reference) {
      kind = Kind.REFERENCE;

    } else {
      throw new IllegalArgumentException("invalid value " + value);
    }
  }

  /**
   * Get this constant's kind.
   *
   * @return The kind.
   */
  public Kind getKind() {
    return kind;
  }

  /**
   * Determine whether this constant's value can be treated as a
   * number.
   *
   * @return <code>true</code> if this constant's kind is an integer,
   *   big integer, double, or character.
   */
  public boolean isNumber() {
    switch (kind) {
    case CHARACTER:
    case INTEGER:
    case BIG_INTEGER:
    case DOUBLE:
      return true;
    default:
      return false;
    }
  }

  /**
   * Determine whether this constant's value is a string.
   *
   * @return <code>true</code> if this constant's value is a string.
   */
  public boolean isString() {
    return Kind.STRING == kind;
  }

  /**
   * Determine whether this constant's value is a reference.
   *
   * @return <code>true</code> if this constant's value is a
   *   reference.
   */
  public boolean isReference() {
    return Kind.REFERENCE == kind;
  }

  /**
   * Get this constant's value.
   *
   * @return The value.
   */
  public Object getValue() {
    return value;
  }

  /**
   * Get this constant's value as a long.
   *
   * @return The value as a long.
   * @throws IllegalStateException Signals that this constant cannot
   *   be converted to a long.
   */
  public long longValue() {
    switch (kind) {
    case INTEGER:
    case BIG_INTEGER:
    case DOUBLE:
      return ((Number)value).longValue();
    case CHARACTER:
      return ((Character)value).charValue();
    default:
      throw new IllegalStateException("Not a number " + kind);
    }
  }

  /**
   * Get this constant's value as a big integer.
   *
   * @return The value as a big integer.
   * @throws IllegalStateException Signals that this constant cannot
   *   be converted to a big integer.
   */
  public BigInteger bigIntValue() {
    switch (kind) {
    case INTEGER:
      return BigInteger.valueOf(((Number)value).longValue());
    case BIG_INTEGER:
      return (BigInteger)value;
    case CHARACTER:
      return BigInteger.valueOf(((Character)value).charValue());
    default:
      throw new IllegalStateException("Not a big integer " + kind);
    }
  }

  /**
   * Get this constant's value as a double.
   *
   * @return The value as a double.
   * @throws IllegalStateException Signals that this constant cannot
   *   be converted to a double.
   */
  public double doubleValue() {
    switch (kind) {
    case INTEGER:
    case BIG_INTEGER:
    case DOUBLE:
      return ((Number)value).doubleValue();
    case CHARACTER:
      return ((Character)value).charValue();
    default:
      throw new IllegalStateException("Not a number " + kind);
    }
  }

  /**
   * Get this constant's value as a reference.
   *
   * @return The value as a reference.
   * @throws IllegalStateException Signals that this constant is not a
   *   reference.
   */
  public Reference refValue() {
    switch (kind) {
    case REFERENCE:
      return (Reference)value;
    default:
      throw new IllegalStateException("Not a reference " + kind);
    }
  }

  /**
   * Get this constant's value as a string.
   *
   * @return The value as a string.
   * @throws IllegalStateException Signals that this constant is not a
   *   string.
   */
  public String stringValue() {
    switch (kind) {
    case STRING:
      return (String)value;
    default:
      throw new IllegalStateException("Not a string " + kind);
    }
  }

  /**
   * Determine whether this constant represents a boolean true value.
   *
   * @return <code>true</code> if this constant represents true.
   */
  public boolean isTrue() {
    switch (kind) {
    case INTEGER:
      return 0 != ((Number)value).longValue();
    case BIG_INTEGER:
      return 0 != ((BigInteger)value).signum();
    case DOUBLE:
      return 0 != ((Number)value).doubleValue();
    case CHARACTER:
      return 0 != ((Character)value).charValue();
    case STRING:
      return true;
    case REFERENCE:
      return ! ((Reference)value).isNull();
    default:
      throw new AssertionError("Invalid kind " + kind);
    }
  }

  /**
   * Determine whether this constant represents a null value.
   *
   * @return <code>true</code> if this constant represents null.
   */
  public boolean isNull() {
    switch (kind) {
    case INTEGER:
      return 0 == ((Number)value).longValue();
    case BIG_INTEGER:
      return 0 == ((BigInteger)value).signum();
    case DOUBLE:
      return 0 == ((Number)value).doubleValue();
    case CHARACTER:
      return 0 == ((Character)value).charValue();
    case STRING:
      return false;
    case REFERENCE:
      return ((Reference)value).isNull();
    default:
      throw new AssertionError("Invalid kind " + kind);
    }
  }

}
