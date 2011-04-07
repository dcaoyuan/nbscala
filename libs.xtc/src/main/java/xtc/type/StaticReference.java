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
 * Representation of a reference to statically allocated memory.  A
 * static reference has neither a base nor an offset.
 *
 * @author Robert Grimm
 * @version $Revision: 1.3 $
 */
public class StaticReference extends VariableReference {

  /**
   * Create a new anonymous static reference.
   *
   * @param type The declared type.
   */
  public StaticReference(Type type) {
    super(type);
  }

  /**
   * Create a new static reference.
   *
   * @param name The declared name.
   * @param type The declared type.
   */
  public StaticReference(String name, Type type) {
    super(name, type);
  }

  public boolean isStatic() {
    return true;
  }

  public boolean isConstant() {
    return true;
  }

}
