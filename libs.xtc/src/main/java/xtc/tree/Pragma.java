/*
 * xtc - The eXTensible Compiler
 * Copyright (C) 2004, 2006 Robert Grimm
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

/**
 * A pragma.
 *
 * @author Robert Grimm
 * @version $Revision: 1.3 $
 */
public class Pragma extends Annotation {

  /** The text of the pragma. */
  public String directive;

  /**
   * Create a new pragma.
   *
   * @param directive The actual directive.
   * @param node The node.
   */
  public Pragma(String directive, Node node) {
    super(node);
    this.directive = directive;
  }

  public boolean hasTraversal() {
    return true;
  }

  public int size() {
    return 2;
  }

  public Object get(int index) {
    switch (index) {
    case 0:
      return directive;
    case 1:
      return node;
    default:
      throw new IndexOutOfBoundsException("Index: " + index + ", Size: 2");
    }
  }

  public Object set(int index, Object value) {
    Object old;

    switch (index) {
    case 0:
      old       = directive;
      directive = (String)value;
      return old;
    case 1:
      old       = node;
      node      = (Node)value;
      return old;
    default:
      throw new IndexOutOfBoundsException("Index: " + index + ", Size: 2");
    }
  }

  public int hashCode() {
    return directive.hashCode();
  }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (! (o instanceof Pragma)) return false;
    Pragma other = (Pragma)o;
    if (! directive.equals(other.directive)) return false;
    if (null == node) return (null == other.node);
    return node.equals(other.node);
  }

}
