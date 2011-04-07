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

import java.util.ArrayList;
import java.util.List;

import xtc.type.Type;

/**
 * Element to set the semantic value to a list.
 *
 * @author Robert Grimm
 * @version $Revision: 1.7 $
 */
public class ProperListValue extends ListValue {

  /** The type of the proper list. */
  public Type type;

  /** The list of bindings for the elements. */
  public List<Binding> elements;

  /** The optional binding for the tail. */
  public Binding tail;

  /**
   * Create a new singleton list value.
   *
   * @param type The type.
   * @param element The binding for the single element.
   */
  public ProperListValue(Type type, Binding element) {
    this.type     = type;
    this.elements = new ArrayList<Binding>(1);
    this.elements.add(element);
    this.tail     = null;
  }

  /**
   * Create a new list value.
   *
   * @param type The type.
   * @param element The binding for the single element.
   * @param tail The binding for the tail.
   */
  public ProperListValue(Type type, Binding element, Binding tail) {
    this.type     = type;
    this.elements = new ArrayList<Binding>(1);
    this.elements.add(element);
    this.tail     = tail;
  }

  /**
   * Create a new proper list value.
   *
   * @param type The type.
   * @param elements The elements.
   * @param tail The tail.
   */
  public ProperListValue(Type type, List<Binding> elements, Binding tail) {
    this.type     = type;
    this.elements = elements;
    this.tail     = tail;
  }

  public Tag tag() {
    return Tag.PROPER_LIST_VALUE;
  }

  public int hashCode() {
    int hash = null == tail ? 0 : tail.hashCode();
    hash     = 13 * hash + elements.hashCode();
    hash     = 13 * hash + type.hashCode();
    return hash;
  }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (! (o instanceof ProperListValue)) return false;
    ProperListValue other = (ProperListValue)o;
    if (! type.equals(other.type)) return false;
    if (! elements.equals(other.elements)) return false;
    return null == tail ? null == other.tail : tail.equals(other.tail);
  }

}
