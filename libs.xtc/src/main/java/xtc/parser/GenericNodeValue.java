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

import java.util.List;

/**
 * Element to set the semantic value to a {@link xtc.tree.GNode
 * generic node}.  The children of the generic node are the
 * production's component expressions.
 *
 * @author Robert Grimm
 * @version $Revision: 1.10 $
 */
public class GenericNodeValue extends GenericValue {

  /**
   * Create a new generic node value.
   *
   * @param name The name.
   * @param children The list of children.
   * @param formatting The list of bindings for formatting.
   */
  public GenericNodeValue(String name, List<Binding> children,
                          List<Binding> formatting) {
    super(name, children, formatting);
  }

  public Tag tag() {
    return Tag.GENERIC_NODE_VALUE;
  }

  public int hashCode() {
    return name.hashCode();
  }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (! (o instanceof GenericNodeValue)) return false;
    GenericNodeValue other = (GenericNodeValue)o;
    if (! name.equals(other.name)) return false;
    if (! children.equals(other.children)) return false;
    return formatting.equals(other.formatting);
  }

}
