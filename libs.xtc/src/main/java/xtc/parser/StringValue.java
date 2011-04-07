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
 * Element to set the semantic value to a string.
 *
 * @author Robert Grimm
 * @version $Revision: 1.5 $
 */
public class StringValue extends ValueElement {

  /** The canonical string value element with unknown text. */
  public static final StringValue VALUE = new StringValue();

  /** The statically known text, which is optional. */
  public final String text;

  /** Create a new string value. */
  public StringValue() {
    text = null;
  }

  /**
   * Create a new string value.
   *
   * @param text The static text.
   */
  public StringValue(String text) {
    this.text = text;
  }

  public Tag tag() {
    return Tag.STRING_VALUE;
  }

  public int hashCode() {
    return null == text ? 7 : text.hashCode();
  }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (! (o instanceof StringValue)) return false;
    StringValue other = (StringValue)o;
    return null == text ? null == other.text : text.equals(other.text);
  }

}
