/*
 * xtc - The eXTensible Compiler
 * Copyright (C) 2004-2007 Robert Grimm
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
package xtc.parser;

import java.io.IOException;

/**
 * A binding of a grammar element to a variable.
 *
 * @author Robert Grimm
 * @version $Revision: 1.11 $
 */
public class Binding extends UnaryOperator {

  /** The variable name. */
  public String name;

  /**
   * Create a new binding with the specified variable and element.
   *
   * @param name The variable name.
   * @param element The bound element.
   */
  public Binding(String name, Element element) {
    super(element);
    this.name    = name;
  }

  public Tag tag() {
    return Tag.BINDING;
  }

  public int hashCode() {
    return name.hashCode();
  }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (! (o instanceof Binding)) return false;
    Binding other = (Binding)o;
    if (! name.equals(other.name)) return false;
    return element.equals(other.element);
  }

  public void write(Appendable out) throws IOException {
    out.append(name);
    out.append(':');
    element.write(out);
  }

}
