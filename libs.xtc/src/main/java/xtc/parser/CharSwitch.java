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

/**
 * A character switch terminal.  This internal element is used to
 * improve parser performance in recognizing {@link Terminal
 * terminals}.
 *
 * @author Robert Grimm
 * @version $Revision: 1.8 $
 */
public class CharSwitch extends CharTerminal implements InternalElement {

  /** The list of {@link CharCase character cases}. */
  public List<CharCase> cases;

  /** The optional default element. */
  public Element base;

  /**
   * Create a new character switch element.
   *
   * @param cases The list of cases.
   */
  public CharSwitch(List<CharCase> cases) {
    this(cases, null);
  }

  /**
   * Create a new character switch element.
   *
   * @param cases The list of cases.
   * @param base The default element.
   */
  public CharSwitch(List<CharCase> cases, Element base) {
    this.cases = cases;
    this.base  = base;
  }

  /**
   * Create a new character switch element which includes a case for
   * the specified character class and corresponding element.  If the
   * class is exclusive, the element of the case is left null and the
   * base is set to the specified element.  Furthermore, the specified
   * class is modified to be non-exclusive.
   *
   * @param klass The character class.
   * @param element The corresponding element.
   */
  public CharSwitch(CharClass klass, Element element) {
    this(new ArrayList<CharCase>(), null);

    if (klass.exclusive) {
      klass.exclusive = false;
      base            = element;
      cases.add(new CharCase(klass));
    } else {
      cases.add(new CharCase(klass, element));
    }
  }

  public Tag tag() {
    return Tag.CHAR_SWITCH;
  }

  /**
   * Determine whether this character switch has a character case for
   * the specified character class.  The specified character class
   * must be non-exclusive.
   *
   * @param klass The character class.
   * @return The corresponding character case or <code>null</code> if
   *   this character switch does not have a case for the character
   *   class.
   */
  public CharCase hasCase(final CharClass klass) {
    for (CharCase kase : cases) {
      if (klass.equals(kase.klass)) {
        return kase;
      }
    }
    return null;
  }

  /**
   * Determine whether this character switch has a character case that
   * overlaps the specified character class.  The specified character
   * class must be non-exclusive.
   *
   * @see CharClass#overlaps(CharClass)
   *
   * @param klass The character class.
   * @return <code>true</code> if this character switch has an overlapping
   *   character case.
   */
  public boolean overlaps(final CharClass klass) {
    for (CharCase kase : cases) {
      if (klass.overlaps(kase.klass)) {
        return true;
      }
    }
    return false;
  }

  public int hashCode() {
    int hash = cases.hashCode();

    if (null == base) {
      return hash;
    } else {
      return hash * 37 + base.hashCode();
    }
  }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (! (o instanceof CharSwitch)) return false;
    CharSwitch other = (CharSwitch)o;
    if (null == base) {
      if (base != other.base) return false;
    } else {
      if (! base.equals(other.base)) return false;
    }
    return cases.equals(other.cases);
  }

}
