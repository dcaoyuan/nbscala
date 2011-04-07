/*
 * xtc - The eXTensible Compiler
 * Copyright (C) 2005-2006 Robert Grimm
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
 * A source identity marker.
 *
 * @author Robert Grimm
 * @version $Revision: 1.4 $
 */
public class SourceIdentity extends Annotation {

  /** The text of the source identity marker. */
  public String ident;

  /**
   * Create a new source identity marker.
   *
   * @param ident The actual identity.
   * @param node The node.
   */
  public SourceIdentity(String ident, Node node) {
    super(node);
    this.ident = ident;
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
      return ident;
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
      old   = ident;
      ident = (String)value;
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
    return ident.hashCode();
  }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (! (o instanceof SourceIdentity)) return false;
    SourceIdentity other = (SourceIdentity)o;
    if (! ident.equals(other.ident)) return false;
    if (null == node) return (null == other.node);
    return node.equals(other.node);
  }

}
