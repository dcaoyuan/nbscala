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
 * Element to set the semantic value to a list of {@link
 * xtc.util.Action actions}.  This element sets the semantic value to
 * a pair, whose value is an action that produces a generic node.  The
 * generic node's children are a production's component expressions,
 * with the action's argument being the first child.
 *
 * @author Robert Grimm
 * @version $Revision: 1.10 $
 */
public class GenericRecursionValue extends GenericActionValue {

  /** The binding for the variable referencing the next pair. */
  public final Binding list;

  /**
   * Create a new generic recursion value.
   *
   * @param name The name of the generic node.
   * @param first The action's argument.
   * @param children The list of children.
   * @param formatting The list of bindings for formatting.
   * @param list The binding for the list.
   */
  public GenericRecursionValue(String name, String first,
                               List<Binding> children,
                               List<Binding> formatting, Binding list) {
    super(name, first, children, formatting);
    this.list = list;
  }

  public Tag tag() {
    return Tag.GENERIC_RECURSION_VALUE;
  }

  public int hashCode() {
    return name.hashCode();
  }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (! (o instanceof GenericRecursionValue)) return false;
    GenericRecursionValue other = (GenericRecursionValue)o;
    if (! name.equals(other.name)) return false;
    if (! first.equals(other.first)) return false;
    if (! list.equals(other.list)) return false;
    if (! children.equals(other.children)) return false;
    return formatting.equals(other.formatting);
  }

}
