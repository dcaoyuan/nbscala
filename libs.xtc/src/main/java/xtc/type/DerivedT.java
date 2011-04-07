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

/**
 * The superclass of all derived types.
 *
 * @author Robert Grimm
 * @version $Revision: 1.4 $
 */
public abstract class DerivedT extends Type {

  /** Create a new derived type. */
  public DerivedT() {
    // Nothing to do.
  }

  /**
   * Create a new derived type.
   *
   * @param template The type whose annotations to copy.
   */
  public DerivedT(Type template) {
    super(template);
  }

  /**
   * Determine whether this type is derived.
   *
   * @return <code>true</code>.
   */
  public boolean isDerived() {
    return true;
  }

}
