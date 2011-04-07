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
 * Element to set the semantic value to <code>null</code>.
 *
 * @author Robert Grimm
 * @version $Revision: 1.8 $
 */
public class NullValue extends ValueElement {

  /** The canonical null value element. */
  public static final NullValue VALUE = new NullValue();

  /** Hide the constructor. */
  private NullValue() { /* Nothing to do. */ }

  public Tag tag() {
    return Tag.NULL_VALUE;
  }

  public int hashCode() {
    return 5;
  }

  public boolean equals(Object o) {
    if (VALUE == o) return true;
    return (o instanceof NullValue);
  }

}
