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
package xtc.parser;

import java.util.List;

/**
 * Element to set the semantic value to an {@link xtc.util.Action
 * action}.  This element sets the semantic value to an action that
 * produces a generic node.  The generic node's children are a
 * production's component expressions, with the action's argument
 * being the first child.
 *
 * @author Robert Grimm
 * @version $Revision: 1.9 $
 */
public class GenericActionValue extends GenericValue {

  /**
   * The name of the action's argument, which also becomes the generic
   * node's first child.
   */
  public final String first;

  /**
   * Create a new generic action value.
   *
   * @param name The name of the generic node.
   * @param first The action's argument.
   * @param children The list of children.
   * @param formatting The list of bindings for formatting.
   */
  public GenericActionValue(String name, String first, List<Binding> children,
                            List<Binding> formatting) {
    super(name, children, formatting);
    this.first = first;
  }

  public Tag tag() {
    return Tag.GENERIC_ACTION_VALUE;
  }

  public int hashCode() {
    return name.hashCode();
  }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (! (o instanceof GenericActionValue)) return false;
    GenericActionValue other = (GenericActionValue)o;
    if (! name.equals(other.name)) return false;
    if (! first.equals(other.first)) return false;
    if (! children.equals(other.children)) return false;
    return formatting.equals(other.formatting);
  }

}
