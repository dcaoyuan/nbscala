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
 * Representation of a reference to dynamically allocated memory.  A
 * dynamic reference has neither a base nor an offset.
 *
 * @author Robert Grimm
 * @version $Revision: 1.2 $
 */
public class DynamicReference extends VariableReference {

  /**
   * Create a new anonymous dynamic reference.
   *
   * @param type The type.
   */
  public DynamicReference(Type type) {
    super(type);
  }

  /**
   * Create a new dynamic reference.
   *
   * @param name The name.
   * @param type The type.
   */
  public DynamicReference(String name, Type type) {
    super(name, type);
  }

  public boolean isDynamic() {
    return true;
  }

}
