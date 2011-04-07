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
 * A named parameter.
 *
 * @author Robert Grimm
 * @version $Revision: 1.1 $
 */
public class NamedParameter extends Parameter {

  /** The name. */
  private String name;

  /**
   * Create a new named parameter.
   *
   * @param name The name.
   */
  public NamedParameter(String name) {
    this.name = name;
  }

  /**
   * Create a new named parameter.
   *
   * @param template The type whose annotations to copy.
   * @param name The name.
   */
  private NamedParameter(Type template, String name) {
    super(template);
    this.name = name;
  }

  public NamedParameter copy() {
    return new NamedParameter(this, name);
  }

  public Type.Tag tag() {
    return Type.Tag.NAMED_PARAMETER;
  }

  public boolean isNamedParameter() {
    return true;
  }

  public NamedParameter toNamedParameter() {
    return this;
  }

  public int hashCode() {
    return name.hashCode();
  }

  public boolean equals(Object o) {
    if (! (o instanceof Type)) return false;
    Type t = resolve(o);

    if (this == t) return true;
    if (! t.isNamedParameter()) return false;
    return name.equals(t.toNamedParameter().name);
  }

  public void write(Appendable out) throws IOException {
    out.append(name);
  }

  public String toString() {
    return name;
  }

}
