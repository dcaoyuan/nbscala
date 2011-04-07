/*
 * xtc - The eXTensible Compiler
 * Copyright (C) 2008 Robert Grimm
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
 * An internal parameter.
 *
 * @author Robert Grimm
 * @version $Revision: 1.1 $
 */
public class InternalParameter extends Parameter {

  /** The count of already created internal parameters. */
  private static long count = 0;

  /** The instance's nonce. */
  private long nonce;

  /** Create a new internal parameter. */
  public InternalParameter() {
    nonce = ++count;
  }

  /**
   * Create a new internal parameter.
   *
   * @param template The type whose annotations to copy.
   */
  public InternalParameter(Type template) {
    super(template);
    nonce = ++count;
  }

  /**
   * Create a new internal parameter.
   *
   * @param template The type whose annotations to copy.
   * @param nonce The nonce.
   */
  private InternalParameter(Type template, long nonce) {
    super(template);
    this.nonce = nonce;
  }

  public InternalParameter copy() {
    return new InternalParameter(this, nonce);
  }

  public Type.Tag tag() {
    return Type.Tag.INTERNAL_PARAMETER;
  }

  public boolean isInternalParameter() {
    return true;
  }

  public InternalParameter toInternalParameter() {
    return this;
  }

  public int hashCode() {
    return (int)nonce;
  }

  public boolean equals(Object o) {
    if (! (o instanceof Type)) return false;
    Type t = resolve(o);

    if (this == t) return true;
    if (! t.isInternalParameter()) return false;
    return nonce == t.toInternalParameter().nonce;
  }

  public void write(Appendable out) throws IOException {
    out.append(Long.toString(nonce));
  }

  public String toString() {
    return Long.toString(nonce);
  }

}
