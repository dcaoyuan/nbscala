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
package xtc.tree;

import java.io.IOException;

import java.util.List;

import xtc.util.Pair;

/**
 * A name/value pair.
 *
 * @author Robert Grimm
 * @version $Revision: 1.12 $
 */
public class Attribute extends Node {
  
  /** The name. */
  final String name;

  /** The value. */
  final Object value;

  /**
   * Create a new attribute with the specified name.  The new
   * attribute's value is <code>null</code>.
   *
   * @param name The name.
   */
  public Attribute(String name) {
    this(name, null);
  }

  /**
   * Create a new attribute with the specified name and value.
   *
   * @param name The name.
   * @param value The value.
   */
  public Attribute(String name, Object value) {
    this.name  = name;
    this.value = value;
  }

  public boolean hasTraversal() {
    return true;
  }

  public int size() {
    return null == value ? 1 : 2;
  }

  public Object get(int index) {
    if (0 == index) return name;
    if ((null != value) && (1 == index)) return value;
    throw new IndexOutOfBoundsException("Index : " + index + ", Size: " +
                                        (null == value ? 1 : 2));
  }

  public Object set(int index, Object value) {
    throw new IllegalStateException("Attributes are immutable");
  }

  /**
   * Get the name.
   *
   * @return The name.
   */
  public String getName() {
    return name;
  }

  /**
   * Get the value.
   *
   * @return The value.
   */
  public Object getValue() {
    return value;
  }

  public int hashCode() {
    return name.hashCode();
  }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (! (o instanceof Attribute)) return false;
    Attribute other = (Attribute)o;
    if (! name.equals(other.name)) return false;
    if (null == value) return (null == other.value);
    return value.equals(other.value);
  }

  public void write(Appendable out) throws IOException {
    out.append(name);
    if (null != value) {
      out.append('(');
      if ((value instanceof List) || (value instanceof Pair)) {
        boolean first = true;
        for (Object o : (Iterable<?>)value) {
          if (first) {
            first = false;
          } else {
            out.append(", ");
          }

          if (o instanceof Node) {
            ((Node)o).write(out);
          } else {
            out.append(o.toString());
          }
        }
      } else if (value instanceof Node) {
        ((Node)value).write(out);
      } else {
        out.append(value.toString());
      }
      out.append(')');
    }
  }    

  /**
   * Get the attribute with the specified name from the specified list.
   *
   * @param name The name.
   * @param list The list.
   * @return The corresponding attribute or <code>null</code> if the
   *   list is <code>null</code> or contains no such attribute.
   */
  public static Attribute get(String name, List<Attribute> list) {
    if (null == list) return null;
    for (Attribute att : list) {
      if (name.equals(att.name)) return att;
    }
    return null;
  }

  /**
   * Determine whether the specified lists of attributes are equivalent.
   *
   * @param l1 The first list.
   * @param l2 The second list.
   * @return <code>true</code> if the two lists are equivalent, that is,
   *   contain the same attributes in some order.
   */
  public static boolean areEquivalent(List<Attribute> l1, List<Attribute> l2) {
    if (null == l1) {
      return (null == l2) || (0 == l2.size());
    } else if (null == l2) {
      return (0 == l1.size());
    } else {
      return l1.containsAll(l2) && l2.containsAll(l1);
    }
  }

}
