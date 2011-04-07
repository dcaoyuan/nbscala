/*
 * xtc - The eXTensible Compiler
 * Copyright (C) 2006-2007 Robert Grimm
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
 * Element to collect a production's text as a token.
 *
 * @author Robert Grimm
 * @version $Revision: 1.3 $
 */
public class TokenValue extends ValueElement {

  /** The canonical token value element with unknown text. */
  public static final TokenValue VALUE = new TokenValue();

  /** The statically known text, which is optional. */
  public final String text;

  /** Create a new token value. */
  public TokenValue() {
    text = null;
  }

  /**
   * Create a new token value.
   *
   * @param text The statically determined text.
   */
  public TokenValue(String text) {
    this.text = text;
  }

  public Tag tag() {
    return Tag.TOKEN_VALUE;
  }

  public int hashCode() {
    return null == text ? 7 : text.hashCode();
  }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (! (o instanceof TokenValue)) return false;
    TokenValue other = (TokenValue)o;
    return null == text ? null == other.text : text.equals(other.text);
  }

}
