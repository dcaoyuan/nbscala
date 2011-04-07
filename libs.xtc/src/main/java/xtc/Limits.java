/*
 * xtc - The eXTensible Compiler
 * Copyright (C) 2005-2008 Robert Grimm
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
package xtc;

import java.math.BigInteger;

/**
 * The platform-dependent C type limits.
 *
 * <p />To recreate this class, compile <code>limits.c</code> in the same
 * directory as this interface and run the resulting executable while
 * piping standard output to <code>Limits.java</code>.
 *
 * <p />The rank for pointer difference, sizeof, and wide character types
 * reflects the ordering <code>char</code>, <code>short</code>,
 * <code>int</code>, <code>long</code>, and <code>long long</code>,
 * starting at 1 and ignoring the sign.  This program requires that the
 * <code>__PTRDIFF_TYPE__</code>, <code>__SIZE_TYPE__</code>, and
 * <code>__WCHAR_TYPE__</code> preprocessor macros are defined.
 *
 * @author Robert Grimm
 * @version $Revision: 1.32 $
 */
public class Limits {

  /** Hide constructor. */
  private Limits() { /* Nothing to do. */ }

  // --------------------------------------------------------------------------

  /** The name and version of the operating system. */
  public static final String OS = "Mac OS X 10.5.6";

  /** The processor architecture. */
  public static final String ARCH = "i386";

  /** The flag for the ELF object format. */
  public static final boolean IS_ELF = false;

  /** The name of the C compiler. */
  public static final String COMPILER_NAME = "gcc";

  /** The C compiler version. */
  public static final String COMPILER_VERSION =
    "4.0.1 (Apple Inc. build 5490)";

  /** The major C compiler version. */
  public static final int COMPILER_VERSION_MAJOR = 4;

  /** The minor C compiler version. */
  public static final int COMPILER_VERSION_MINOR = 0;

  // --------------------------------------------------------------------------

  /** The flag for whether the machine is big endian. */
  public static final boolean IS_BIG_ENDIAN = false;

  /** The size of void types. */
  public static final int VOID_SIZE = 1;

  /** The alignment of void types. */
  public static final int VOID_ALIGN = 1;

  /** The size of function types. */
  public static final int FUNCTION_SIZE = 1;

  /** The alignment of function types. */
  public static final int FUNCTION_ALIGN = 1;

  /** The size of pointer types. */
  public static final int POINTER_SIZE = 4;

  /** The alignment of pointer types. */
  public static final int POINTER_ALIGN = 4;

  /** The natural alignment of pointer types. */
  public static final int POINTER_NAT_ALIGN = 4;

  /** The size of pointer difference types. */
  public static final int PTRDIFF_SIZE = 4;

  /** The rank of pointer difference types. */
  public static final int PTRDIFF_RANK = 3;

  /** The size of sizeof expressions. */
  public static final int SIZEOF_SIZE = 4;

  /** The rank of sizeof expressions. */
  public static final int SIZEOF_RANK = 4;

  /** The maximum size of fixed size arrays. */
  public static final BigInteger ARRAY_MAX = BigInteger.valueOf(1073741824L);

  // --------------------------------------------------------------------------

  /** The size of boolean types. */
  public static final int BOOL_SIZE = 1;

  /** The alignment of boolean types. */
  public static final int BOOL_ALIGN = 1;

  /** The natural alignment of boolean types. */
  public static final int BOOL_NAT_ALIGN = 1;

  // --------------------------------------------------------------------------

  /** The flag for whether <code>char</code> is signed. */
  public static final boolean IS_CHAR_SIGNED = true;

  /** The bit width of char types. */
  public static final int CHAR_BITS = 8;

  /** The minimum value of signed char types. */
  public static final BigInteger CHAR_MIN = new BigInteger("-128");

  /** The maximum value of signed char types. */
  public static final BigInteger CHAR_MAX = new BigInteger("127");

  /** The maximum value of unsigned char types. */
  public static final BigInteger UCHAR_MAX = new BigInteger("255");

  // --------------------------------------------------------------------------

  /** The flag for whether <code>wchar_t</code> is signed. */
  public static final boolean IS_WCHAR_SIGNED = true;

  /** The size of wide char types. */
  public static final int WCHAR_SIZE = 4;

  /** The rank of wide character types. */
  public static final int WCHAR_RANK = 3;

  // --------------------------------------------------------------------------

  /**
   * The flag for whether string literals consist of
   * <code>const char</code> elements.
   */
  public static final boolean IS_STRING_CONST = true;

  // --------------------------------------------------------------------------

  /** The size of short types. */
  public static final int SHORT_SIZE = 2;

  /** The alignment of short types. */
  public static final int SHORT_ALIGN = 2;

  /** The natural alignment of short types. */
  public static final int SHORT_NAT_ALIGN = 2;

  /** The minimum value of signed short types. */
  public static final BigInteger SHORT_MIN = new BigInteger("-32768");

  /** The maximum value of signed short types. */
  public static final BigInteger SHORT_MAX = new BigInteger("32767");

  /** The maximum value of unsigned short types. */
  public static final BigInteger USHORT_MAX = new BigInteger("65535");

  // --------------------------------------------------------------------------

  /** The flag for whether <code>int</code> is signed in bit-fields. */
  public static final boolean IS_INT_SIGNED = true;

  /** The size of int types. */
  public static final int INT_SIZE = 4;

  /** The alignment of int types. */
  public static final int INT_ALIGN = 4;

  /** The natural alignment of int types. */
  public static final int INT_NAT_ALIGN = 4;

  /** The minimum value of signed int types. */
  public static final BigInteger INT_MIN = new BigInteger("-2147483648");

  /** The maximum value of signed int types. */
  public static final BigInteger INT_MAX = new BigInteger("2147483647");

  /** The maximum value of unsigned int types. */
  public static final BigInteger UINT_MAX = new BigInteger("4294967295");

  // --------------------------------------------------------------------------

  /** The size of long types. */
  public static final int LONG_SIZE = 4;

  /** The alignment of long types. */
  public static final int LONG_ALIGN = 4;

  /** The natural alignment of long types. */
  public static final int LONG_NAT_ALIGN = 4;

  /** The minimum value of signed long types. */
  public static final BigInteger LONG_MIN = new BigInteger("-2147483648");

  /** The maximum value of signed long types. */
  public static final BigInteger LONG_MAX = new BigInteger("2147483647");

  /** The maximum value of unsigned long types. */
  public static final BigInteger ULONG_MAX = new BigInteger("4294967295");

  // --------------------------------------------------------------------------

  /** The size of long long types. */
  public static final int LONG_LONG_SIZE = 8;

  /** The alignment of long long types. */
  public static final int LONG_LONG_ALIGN = 4;

  /** The natural alignment of long long types. */
  public static final int LONG_LONG_NAT_ALIGN = 8;

  /** The minimum value of signed long long types. */
  public static final BigInteger LONG_LONG_MIN =
    new BigInteger("-9223372036854775808");

  /** The maximum value of signed long long types. */
  public static final BigInteger LONG_LONG_MAX =
    new BigInteger("9223372036854775807");

  /** The maximum value of unsigned long long types. */
  public static final BigInteger ULONG_LONG_MAX =
    new BigInteger("18446744073709551615");

  // --------------------------------------------------------------------------

  /** The size of float types. */
  public static final int FLOAT_SIZE = 4;

  /** The alignment of float types. */
  public static final int FLOAT_ALIGN = 4;

  /** The natural alignment of float types. */
  public static final int FLOAT_NAT_ALIGN = 4;

  /** The size of double types. */
  public static final int DOUBLE_SIZE = 8;

  /** The alignment of double types. */
  public static final int DOUBLE_ALIGN = 4;

  /** The natural alignment of double types. */
  public static final int DOUBLE_NAT_ALIGN = 8;

  /** The size of long double types. */
  public static final int LONG_DOUBLE_SIZE = 16;

  /** The alignment of long double types. */
  public static final int LONG_DOUBLE_ALIGN = 16;

  /** The natural alignment of long double types. */
  public static final int LONG_DOUBLE_NAT_ALIGN = 16;

  // --------------------------------------------------------------------------

  /**
   * Convert the specified size to the corresponding bit width.
   *
   * @param size The size.
   * @return The corresponding bit width.
   */
  public static long toWidth(long size) {
    return size * CHAR_BITS;
  }

  // --------------------------------------------------------------------------

  /**
   * Determine whether the specified value fits into a char.
   *
   * @param value The value.
   * @return <code>true</code> if the value fits into a char.
   */
  public static boolean fitsChar(BigInteger value) {
    return ((CHAR_MIN.compareTo(value) <= 0) &&
            (CHAR_MAX.compareTo(value) >= 0));
  }

  /**
   * Determine whether the specified value fits into an unsigned
   * char.
   *
   * @param value The value.
   * @return <code>true</code> if the value fits into an unsigned
   *   char.
   */
  public static boolean fitsUnsignedChar(BigInteger value) {
    return ((BigInteger.ZERO.compareTo(value) <= 0) &&
            (UCHAR_MAX.compareTo(value) >= 0));
  }

  /**
   * Determine whether the specified value fits into a short.
   *
   * @param value The value.
   * @return <code>true</code> if the value fits into a short.
   */
  public static boolean fitsShort(BigInteger value) {
    return ((SHORT_MIN.compareTo(value) <= 0) &&
            (SHORT_MAX.compareTo(value) >= 0));
  }

  /**
   * Determine whether the specified value fits into an unsigned
   * short.
   *
   * @param value The value.
   * @return <code>true</code> if the value fits into an unsigned
   *   short.
   */
  public static boolean fitsUnsignedShort(BigInteger value) {
    return ((BigInteger.ZERO.compareTo(value) <= 0) &&
            (USHORT_MAX.compareTo(value) >= 0));
  }

  /**
   * Determine whether the specified value fits into an int.
   *
   * @param value The value.
   * @return <code>true</code> if the value fits into an int.
   */
  public static boolean fitsInt(BigInteger value) {
    return ((INT_MIN.compareTo(value) <= 0) &&
            (INT_MAX.compareTo(value) >= 0));
  }

  /**
   * Determine whether the specified value fits into an unsigned
   * int.
   *
   * @param value The value.
   * @return <code>true</code> if the value fits into an unsigned
   *   int.
   */
  public static boolean fitsUnsignedInt(BigInteger value) {
    return ((BigInteger.ZERO.compareTo(value) <= 0) &&
            (UINT_MAX.compareTo(value) >= 0));
  }

  /**
   * Determine whether the specified value fits into a long.
   *
   * @param value The value.
   * @return <code>true</code> if the value fits into a long.
   */
  public static boolean fitsLong(BigInteger value) {
    return ((LONG_MIN.compareTo(value) <= 0) &&
            (LONG_MAX.compareTo(value) >= 0));
  }

  /**
   * Determine whether the specified value fits into an unsigned
   * long.
   *
   * @param value The value.
   * @return <code>true</code> if the value fits into an unsigned
   *   long.
   */
  public static boolean fitsUnsignedLong(BigInteger value) {
    return ((BigInteger.ZERO.compareTo(value) <= 0) &&
            (ULONG_MAX.compareTo(value) >= 0));
  }

  /**
   * Determine whether the specified value fits into a long long.
   *
   * @param value The value.
   * @return <code>true</code> if the value fits into a long long.
   */
  public static boolean fitsLongLong(BigInteger value) {
    return ((LONG_LONG_MIN.compareTo(value) <= 0) &&
            (LONG_LONG_MAX.compareTo(value) >= 0));
  }

  /**
   * Determine whether the specified value fits into an unsigned
   * long long.
   *
   * @param value The value.
   * @return <code>true</code> if the value fits into an unsigned
   *   long long.
   */
  public static boolean fitsUnsignedLongLong(BigInteger value) {
    return ((BigInteger.ZERO.compareTo(value) <= 0) &&
            (ULONG_LONG_MAX.compareTo(value) >= 0));
  }

  // --------------------------------------------------------------------------

  /** The modulo of signed char types. */
  private static final BigInteger CHAR_MOD = CHAR_MAX.add(BigInteger.ONE);

  /** The modulo of unsigned char types. */
  private static final BigInteger UCHAR_MOD = UCHAR_MAX.add(BigInteger.ONE);

  /**
   * Mask the specified value as a signed char.
   *
   * @param value The value.
   * @return The value as a signed char.
   */
  public static BigInteger maskAsSignedChar(BigInteger value) {
    return value.remainder(CHAR_MOD);
  }

  /**
   * Mask the specified value as an unsigned char.
   *
   * @param value The value.
   * @return The value as an unsigned char.
   */
  public static BigInteger maskAsUnsignedChar(BigInteger value) {
    return (value.signum() >= 0) ? value.remainder(UCHAR_MOD) :
      UCHAR_MOD.add(value.remainder(UCHAR_MOD));
  }

  // --------------------------------------------------------------------------

  /** The modulo of signed short types. */
  private static final BigInteger SHORT_MOD = SHORT_MAX.add(BigInteger.ONE);

  /** The modulo of unsigned short types. */
  private static final BigInteger USHORT_MOD = USHORT_MAX.add(BigInteger.ONE);

  /**
   * Mask the specified value as a signed short.
   *
   * @param value The value.
   * @return The value as a signed short.
   */
  public static BigInteger maskAsShort(BigInteger value) {
    return value.remainder(SHORT_MOD);
  }

  /**
   * Mask the specified value as an unsigned short.
   *
   * @param value The value.
   * @return The value as an unsigned short.
   */
  public static BigInteger maskAsUnsignedShort(BigInteger value) {
    return (value.signum() >= 0) ? value.remainder(USHORT_MOD) :
      USHORT_MOD.add(value.remainder(USHORT_MOD));
  }

  // --------------------------------------------------------------------------

  /** The modulo of signed int types. */
  private static final BigInteger INT_MOD = INT_MAX.add(BigInteger.ONE);

  /** The modulo of unsigned int types. */
  private static final BigInteger UINT_MOD = UINT_MAX.add(BigInteger.ONE);

  /**
   * Mask the specified value as a signed int.
   *
   * @param value The value.
   * @return The value as a signed int.
   */
  public static BigInteger maskAsInt(BigInteger value) {
    return value.remainder(INT_MOD);
  }

  /**
   * Mask the specified value as an unsigned int.
   *
   * @param value The value.
   * @return The value as an unsigned int.
   */
  public static BigInteger maskAsUnsignedInt(BigInteger value) {
    return (value.signum() >= 0) ? value.remainder(UINT_MOD) :
      UINT_MOD.add(value.remainder(UINT_MOD));
  }

  // --------------------------------------------------------------------------

  /** The modulo of signed long types. */
  private static final BigInteger LONG_MOD = LONG_MAX.add(BigInteger.ONE);

  /** The modulo of unsigned long types. */
  private static final BigInteger ULONG_MOD = ULONG_MAX.add(BigInteger.ONE);

  /**
   * Mask the specified value as a signed long.
   *
   * @param value The value.
   * @return The value as a signed long.
   */
  public static BigInteger maskAsLong(BigInteger value) {
    return value.remainder(LONG_MOD);
  }

  /**
   * Mask the specified value as an unsigned long.
   *
   * @param value The value.
   * @return The value as an unsigned long.
   */
  public static BigInteger maskAsUnsignedLong(BigInteger value) {
    return (value.signum() >= 0) ? value.remainder(ULONG_MOD) :
      ULONG_MOD.add(value.remainder(ULONG_MOD));
  }

  // --------------------------------------------------------------------------

  /** The modulo of signed long long types. */
  private static final BigInteger LONG_LONG_MOD =
    LONG_LONG_MAX.add(BigInteger.ONE);

  /** The modulo of unsigned long long types. */
  private static final BigInteger ULONG_LONG_MOD =
    ULONG_LONG_MAX.add(BigInteger.ONE);

  /**
   * Mask the specified value as a signed long long.
   *
   * @param value The value.
   * @return The value as a signed long long.
   */
  public static BigInteger maskAsLongLong(BigInteger value) {
    return value.remainder(LONG_LONG_MOD);
  }

  /**
   * Mask the specified value as an unsigned long long.
   *
   * @param value The value.
   * @return The value as an unsigned long long.
   */
  public static BigInteger maskAsUnsignedLongLong(BigInteger value) {
    return (value.signum() >= 0) ? value.remainder(ULONG_LONG_MOD) :
      ULONG_LONG_MOD.add(value.remainder(ULONG_LONG_MOD));
  }

}
