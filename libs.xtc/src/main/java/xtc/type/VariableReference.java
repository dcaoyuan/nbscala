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
 * Representation of a symbolic memory location.  A variable reference
 * has neither a base nor an offset.
 *
 * @author Robert Grimm
 * @version $Revision: 1.4 $
 */
public abstract class VariableReference extends Reference {

  /** The optional name. */
  private final String name;

  /**
   * Create a new anonymous variable reference.
   *
   * @param type The declared type.
   */
  public VariableReference(Type type) {
    this(null, type);
  }

  /**
   * Create a new variable reference.
   *
   * @param name The declared name.
   * @param type The declared type.
   */
  public VariableReference(String name, Type type) {
    super(type);
    this.name = name;
  }

  public boolean isVariable() {
    return true;
  }

  /**
   * Determine whether this variable reference has a name.
   *
   * @return <code>true</code> if this variable reference has a name.
   */
  public boolean hasName() {
    return null != name;
  }

  /**
   * Get this static reference's name.
   *
   * @return The name.
   */
  public String getName() {
    return name;
  }

  public void write(Appendable out) throws IOException {
    if (null == name) {
      out.append("<anon>");
    } else {
      out.append(name);
    }
  }

}
