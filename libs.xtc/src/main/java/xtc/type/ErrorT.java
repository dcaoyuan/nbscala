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

/**
 * The pseudo-type indicating a malformed type.
 *
 * @author Robert Grimm
 * @version $Revision: 1.14 $
 */
public class ErrorT extends Type {

  /** The canonical error type. */
  public static final ErrorT TYPE = new ErrorT();

  /** Seal the canonical type. */
  static {
    TYPE.seal();
  }

  /** Create a new error type. */
  public ErrorT() {
    // Nothing to do.
  }

  /**
   * Create a new error type.
   *
   * @param template The type whose annotations to copy.
   */
  public ErrorT(Type template) {
    super(template);
  }

  public ErrorT copy() {
    return new ErrorT(this);
  }

  public Type.Tag tag() {
    return Type.Tag.ERROR;
  }

  public boolean isError() {
    return true;
  }

  public int hashCode() {
    return 7;
  }

  public boolean equals(Object o) {
    if (! (o instanceof Type)) return false;
    return resolve(o).isError();
  }

  public void write(Appendable out) throws IOException {
    out.append("<error>");
  }
  
  public String toString() {
    return "<error>";
  }

}
