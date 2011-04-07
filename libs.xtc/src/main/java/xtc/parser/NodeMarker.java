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
package xtc.parser;

import java.io.IOException;

/**
 * Element to set a generic node's name.
 *
 * @author Robert Grimm
 * @version $Revision: 1.4 $
 */
public class NodeMarker extends Element {

  /** The name. */
  public final String name;

  /**
   * Create a new node marker with the specified name.
   *
   * @param name The name.
   */
  public NodeMarker(String name) {
    this.name = name;
  }

  public Tag tag() {
    return Tag.NODE_MARKER;
  }

  public int hashCode() {
    return name.hashCode();
  }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (! (o instanceof NodeMarker)) return false;
    return name.equals(((NodeMarker)o).name);
  }

  public void write(Appendable out) throws IOException {
    out.append('@');
    out.append(name);
  }

}
