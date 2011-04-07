/*
 * xtc - The eXTensible Compiler
 * Copyright (C) 2006 Robert Grimm
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

/**
 * Representation of a reference value.  A reference has a base, type,
 * and an optional offset.  The offset, in turn, either is an index or
 * a field.
 *
 * @author Robert Grimm
 * @version $Revision: 1.2 $
 */
public abstract class RelativeReference extends Reference {

  /** The base. */
  protected Reference base;

  /**
   * Create a new relative reference.
   *
   * @param type The type.
   * @param base The base.
   */
  public RelativeReference(Type type, Reference base) {
    super(type);
    this.base = base;
  }

  public boolean isConstant() {
    return base.isConstant();
  }

  public boolean hasBase() {
    return true;
  }

  public Reference getBase() {
    return base;
  }

}
