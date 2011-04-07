/*
 * xtc - The eXTensible Compiler
 * Copyright (C) 2004 Robert Grimm
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

import xtc.tree.Node;

/**
 * A case within a character switch.
 *
 * @see CharSwitch
 *
 * @author Robert Grimm
 * @version $Revision: 1.5 $
 */
public class CharCase extends Node {

  /**
   * The characters as a character class.  Note that the character
   * class must not be exclusive and should be {@link
   * CharClass#normalize() normalized}.
   */
  public CharClass klass;

  /**
   * The optional element. A <code>null</code> element indicates that
   * this character case's characters do not provide a match.
   */
  public Element element;

  /**
   * Create a new character case.
   *
   * @param klass The character class.
   */
  public CharCase(CharClass klass) {
    this(klass, null);
  }

  /**
   * Create a new character case.
   *
   * @param c The character.
   * @param element The element.
   */
  public CharCase(char c, Element element) {
    this.klass   = new CharClass(c);
    this.element = element;
  }

  /**
   * Create a new character case.
   *
   * @param klass The character class.
   * @param element The element.
   */
  public CharCase(CharClass klass, Element element) {
    this.klass   = klass;
    this.element = element;
  }

  public int hashCode() {
    return klass.hashCode();
  }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (! (o instanceof CharCase)) return false;
    CharCase other = (CharCase)o;
    if (! klass.equals(other.klass)) return false;
    if (null == element) {
      return (null == other.element);
    } else {
      return element.equals(other.element);
    }
  }

}
