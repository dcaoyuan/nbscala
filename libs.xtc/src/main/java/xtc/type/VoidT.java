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

/**
 * A void type.
 *
 * @author Robert Grimm
 * @version $Revision: 1.19 $
 */
public class VoidT extends Type {

  /** The canonical void type. */
  public static final VoidT TYPE = new VoidT();

  /** Seal the canonical type. */
  static {
    TYPE.seal();
  }

  /** Create a new void type. */
  public VoidT() {
    // Nothing to do.
  }

  /**
   * Create a new void type.
   *
   * @param template The type whose annotations to copy.
   */
  public VoidT(Type template) {
    super(template);
  }

  public VoidT copy() {
    return new VoidT(this);
  }

  public Type.Tag tag() {
    return Type.Tag.VOID;
  }

  public boolean isVoid() {
    return true;
  }

  public VoidT toVoid() {
    return this;
  }

  public int hashCode() {
    return 7;
  }

  public boolean equals(Object o) {
    if (! (o instanceof Type)) return false;
    return resolve(o).isVoid();
  }

  public void write(Appendable out) throws IOException {
    out.append("void");
  }

  public String toString() {
    return "void";
  }

}
