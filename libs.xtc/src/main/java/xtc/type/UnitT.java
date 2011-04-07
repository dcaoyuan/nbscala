/*
 * xtc - The eXTensible Compiler
 * Copyright (C) 2007 Robert Grimm
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
 * A unit type.
 *
 * @author Robert Grimm
 * @version $Revision: 1.1 $
 */
public class UnitT extends Type {

  /** The canonical unit type. */
  public static final UnitT TYPE = new UnitT();

  /** Seal the canonical type. */
  static {
    TYPE.seal();
  }

  /** Create a new unit type. */
  public UnitT() {
    // Nothing to do.
  }

  /**
   * Create a new unit type.
   *
   * @param template The type whose annotations to copy.
   */
  public UnitT(Type template) {
    super(template);
  }

  public UnitT copy() {
    return new UnitT(this);
  }

  public Type.Tag tag() {
    return Type.Tag.UNIT;
  }

  public boolean isUnit() {
    return true;
  }

  public UnitT toUnit() {
    return this;
  }

  public int hashCode() {
    return 7;
  }

  public boolean equals(Object o) {
    if (! (o instanceof Type)) return false;
    return resolve(o).isUnit();
  }

  public void write(Appendable out) throws IOException {
    out.append("unit");
  }

  public String toString() {
    return "unit";
  }

}
