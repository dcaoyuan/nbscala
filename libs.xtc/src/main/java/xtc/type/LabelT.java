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
 * A label type.
 *
 * @author Robert Grimm
 * @version $Revision: 1.15 $
 */
public class LabelT extends Type {

  /** The name. */
  private String name;

  /**
   * Create a new label type.
   *
   * @param name The name.
   */
  public LabelT(String name) {
    this.name = name;
  }

  /**
   * Create a new label type.
   *
   * @param template The type whose annotations to copy.
   * @param name The name.
   */
  public LabelT(Type template, String name) {
    super(template);
    this.name = name;
  }

  public LabelT copy() {
    return new LabelT(this, name);
  }

  public Type.Tag tag() {
    return Type.Tag.LABEL;
  }

  public boolean isLabel() {
    return true;
  }

  public LabelT toLabel() {
    return this;
  }

  /**
   * Get the name.
   *
   * @return The name.
   */
  public String getName() {
    return name;
  }

  public int hashCode() {
    return name.hashCode();
  }

  public boolean equals(Object o) {
    if (! (o instanceof Type)) return false;
    Type t = resolve(o);

    if (this == t) return true;
    if (! t.isLabel()) return false;
    return name.equals(((LabelT)t).name);
  }

  public void write(Appendable out) throws IOException {
    out.append("label(");
    out.append(name);
    out.append(')');
  }

}
