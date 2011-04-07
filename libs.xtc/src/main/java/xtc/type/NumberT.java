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

import xtc.Limits;

/**
 * The superclass of all number types.
 *
 * @author Robert Grimm
 * @version $Revision: 1.27 $
 */
public abstract class NumberT extends Type {

  /** The number kind. */
  public static enum Kind {
    /** A signed byte. */
    BYTE,
    /** A char. */
    CHAR,
    /** A signed char. */
    S_CHAR,
    /** An unsigned char. */
    U_CHAR,
    /** A signed short. */
    SHORT,
    /** An unsigned short. */
    U_SHORT,
    /**
     * An int.  Per C99 6.7.2, it depends on the implementation
     * whether <code>int</code> appearing by itself in a bit-field is
     * signed or unsigned.  Hence we distinguish between ints and
     * signed ints.  (Whatever.)
     */
    INT,
    /** A signed int. */
    S_INT,
    /** An unsigned int. */
    U_INT,
    /** A signed long. */
    LONG,
    /** An unsigned long. */
    U_LONG,
    /** A signed long long. */
    LONG_LONG,
    /** An unsigned long long. */
    U_LONG_LONG,
    /** A float. */
    FLOAT,
    /** A double. */
    DOUBLE,
    /** A long double. */
    LONG_DOUBLE,
    /** A float complex. */
    FLOAT_COMPLEX,
    /** A double complex. */
    DOUBLE_COMPLEX,
    /** A long double complex. */
    LONG_DOUBLE_COMPLEX
  }

  // =========================================================================

  /** The canonical signed byte type. */
  public static final IntegerT BYTE = new IntegerT(Kind.BYTE);

  /** The canonical char type. */
  public static final IntegerT CHAR = new IntegerT(Kind.CHAR);

  /** The canonical signed char type. */
  public static final IntegerT S_CHAR = new IntegerT(Kind.S_CHAR);

  /** The canonical unsigned char type. */
  public static final IntegerT U_CHAR = new IntegerT(Kind.U_CHAR);

  /** The canonical short type. */
  public static final IntegerT SHORT = new IntegerT(Kind.SHORT);

  /** The canonical unsigned short type. */
  public static final IntegerT U_SHORT = new IntegerT(Kind.U_SHORT);

  /** The canonical int type. */
  public static final IntegerT INT = new IntegerT(Kind.INT);

  /** The canonical signed int type. */
  public static final IntegerT S_INT = new IntegerT(Kind.S_INT);

  /** The cannical unsigned int type. */
  public static final IntegerT U_INT = new IntegerT(Kind.U_INT);

  /** The canonical signed long type. */
  public static final IntegerT LONG = new IntegerT(Kind.LONG);

  /** The canonical unsigned long type. */
  public static final IntegerT U_LONG = new IntegerT(Kind.U_LONG);

  /** The canonical signed long long type. */
  public static final IntegerT LONG_LONG = new IntegerT(Kind.LONG_LONG);

  /** The canonical unsigned long long type. */
  public static final IntegerT U_LONG_LONG = new IntegerT(Kind.U_LONG_LONG);

  /** The canonical float type. */
  public static final FloatT FLOAT = new FloatT(Kind.FLOAT);

  /** The canonical double type. */
  public static final FloatT DOUBLE = new FloatT(Kind.DOUBLE);

  /** The canonical long double type. */
  public static final FloatT LONG_DOUBLE = new FloatT(Kind.LONG_DOUBLE);

  /** The canonical float complex type. */
  public static final FloatT FLOAT_COMPLEX = new FloatT(Kind.FLOAT_COMPLEX);

  /** The canonical double complex type. */
  public static final FloatT DOUBLE_COMPLEX = new FloatT(Kind.DOUBLE_COMPLEX);

  /** The canonical long double complex type. */
  public static final FloatT LONG_DOUBLE_COMPLEX =
    new FloatT(Kind.LONG_DOUBLE_COMPLEX);

  static {
    BYTE.seal();
    CHAR.seal();
    S_CHAR.seal();
    U_CHAR.seal();
    SHORT.seal();
    U_SHORT.seal();
    INT.seal();
    S_INT.seal();
    U_INT.seal();
    LONG.seal();
    U_LONG.seal();
    LONG_LONG.seal();
    U_LONG_LONG.seal();
    FLOAT.seal();
    DOUBLE.seal();
    LONG_DOUBLE.seal();
    FLOAT_COMPLEX.seal();
    DOUBLE_COMPLEX.seal();
    LONG_DOUBLE_COMPLEX.seal();
  }

  // =========================================================================

  /** The kind. */
  protected final Kind kind;

  /**
   * Create a new number type.  The specified kind must be a valid
   * integer or float kind.
   *
   * @param kind The kind.
   */
  public NumberT(Kind kind) {
    this.kind = kind;
  }

  /**
   * Create a new number type.  The specified kind must be a valid
   * integer or float kind.
   *
   * @param template The type whose annotations to copy.
   * @param kind The kind.
   */
  public NumberT(Type template, Kind kind) {
    super(template);
    this.kind = kind;
  }

  public boolean isNumber() {
    return true;
  }

  public NumberT toNumber() {
    return this;
  }

  /**
   * Determine whether this number has the specified kind.  Note that
   * this method uses {@link #equal(Kind,Kind)} to perform the kind
   * comparison.
   *
   * @param kind The kind.
   * @return <code>true</code> if this number has the specified kind.
   */
  public boolean hasKind(Kind kind) {
    return equal(this.kind, kind);
  }

  /**
   * Get the kind.
   *
   * @return The kind.
   */
  public Kind getKind() {
    return kind;
  }

  /**
   * Determine whether this number is signed.
   *
   * @return <code>true</code> if this number is signed.
   */
  public boolean isSigned() {
    switch (kind) {
    case U_CHAR:
    case U_SHORT:
    case U_INT:
    case U_LONG:
    case U_LONG_LONG:
      return false;
    case CHAR:
      return Limits.IS_CHAR_SIGNED;
    default:
      return true;
    }
  }

  public int hashCode() {
    return (kind.ordinal() + 1) * 37;
  }

  /**
   * Determine whether this type equals the specified object.  This
   * number equals the specified object if the specified object also
   * is a number and the two numbers' kinds are either the same or a
   * combination of the {@link Kind#INT int} and {@link Kind#S_INT
   * signed int} kinds.
   *
   * @param o The object.
   * @return <code>true</code> if this type equals the object.
   */
  public boolean equals(Object o) {
    if (! (o instanceof Type)) return false;
    Type t = resolve(o);

    if (this == t) return true;
    if (! t.isNumber()) return false;
    return equal(kind, ((NumberT)t).kind);
  }

  public void write(Appendable out) throws IOException {
    out.append(toString());
  }

  public String toString() {
    switch (kind) {
    case BYTE:
      return "byte";
    case CHAR:
      return "char";
    case S_CHAR:
      return "signed char";
    case U_CHAR:
      return "unsigned char";
    case SHORT:
      return "short";
    case U_SHORT:
      return "unsigned short";
    case INT:
      return "int";
    case S_INT:
      return "signed int";
    case U_INT:
      return "unsigned int";
    case LONG:
      return "long";
    case U_LONG:
      return "unsigned long";
    case LONG_LONG:
      return "long long";
    case U_LONG_LONG:
      return "unsigned long long";
    case FLOAT:
      return "float";
    case DOUBLE:
      return "double";
    case LONG_DOUBLE:
      return "long double";
    case FLOAT_COMPLEX:
      return "float _Complex";
    case DOUBLE_COMPLEX:
      return "double _Complex";
    case LONG_DOUBLE_COMPLEX:
      return "long double _Complex";
    default:
      throw new AssertionError("Invalid kind: " + kind);
    }
  }

  // =========================================================================

  /**
   * Determine whether the specified kinds equal each other.  This
   * method treats implicit, int, and signed int types as equal.
   *
   * @param k1 The first kind.
   * @param k2 The second kind.
   * @return <code>true</code> if the kinds are equal.
   */
  public static boolean equal(Kind k1, Kind k2) {
    return ((k1 == k2) ||
            (((Kind.INT == k1) || (Kind.S_INT == k1)) &&
             ((Kind.INT == k2) || (Kind.S_INT == k2))));
  }

  /**
   * Determine whether the specified kinds equal each other, modulo
   * their signs.  This method treats implicit, int, and signed int
   * types as equal.
   *
   * @param k1 The first kind.
   * @param k2 The second kind.
   * @return <code>true</code> if the kinds are equal modulo their
   *   signs.
   */
  public static boolean equalIgnoringSign(Kind k1, Kind k2) {
    switch (k1) {
    case CHAR:
    case S_CHAR:
    case U_CHAR:
      return (Kind.CHAR == k2) || (Kind.S_CHAR == k2) || (Kind.U_CHAR == k2);
    case SHORT:
    case U_SHORT:
      return (Kind.SHORT == k2) || (Kind.U_SHORT == k2);
    case INT:
    case S_INT:
    case U_INT:
      return (Kind.INT == k2) || (Kind.S_INT == k2) || (Kind.U_INT == k2);
    case LONG:
    case U_LONG:
      return (Kind.LONG == k2) || (Kind.U_LONG == k2);
    case LONG_LONG:
    case U_LONG_LONG:
      return (Kind.LONG_LONG == k2) || (Kind.U_LONG_LONG == k2);
    default:
      return (k1 == k2);
    }
  }

}
