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

/**
 * Element to set the semantic value to a new empty list.
 *
 * @author Robert Grimm
 * @version $Revision: 1.9 $
 */
public class EmptyListValue extends ListValue {

  /** The canonical empty list value element. */
  public static final EmptyListValue VALUE = new EmptyListValue();

  /** Hide the constructor. */
  private EmptyListValue() { /* Nothing to do. */ }

  public Tag tag() {
    return Tag.EMPTY_LIST_VALUE;
  }

  public int hashCode() {
    return 11;
  }

  public boolean equals(Object o) {
    if (VALUE == o) return true;
    return (o instanceof EmptyListValue);
  }

}
