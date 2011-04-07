/*
 * xtc - The eXTensible Compiler
 * Copyright (C) 2007 Robert Grimm
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * version 2.1 as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301,
 * USA.
 */
package xtc.util;

/**
 * A nonce.
 *
 * @author Robert Grimm
 * @version $Revision: 1.2 $
 */
public class Nonce {
  
  /** The number. */
  private final long number;

  /**
   * Create a new nonce.
   *
   * @param number The number.
   */
  private Nonce(long number) {
    this.number = number;
  }

  /**
   * Get this nonce's number.
   *
   * @return The number.
   */
  public long getNumber() {
    return number;
  }

  public String toString() {
    return "nonce(" + number + ')';
  }

  // ========================================================================

  /** The current count. */
  private static long count = 0;

  /**
   * Create a new nonce.
   *
   * @return The new nonce.
   */
  public static Nonce create() {
    Nonce n = new Nonce(count++);
    if (0 == count) throw new AssertionError("Out of nonces");
    return n;
  }

}
