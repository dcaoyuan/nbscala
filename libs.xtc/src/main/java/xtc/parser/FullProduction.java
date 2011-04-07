/*
 * xtc - The eXTensible Compiler
 * Copyright (C) 2005-2007 Robert Grimm
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

import java.util.List;

import xtc.tree.Attribute;

import xtc.type.Type;

/**
 * A complete production.
 *
 * @author Robert Grimm
 * @version $Revision: 1.8 $
 */
public class FullProduction extends Production {

  /**
   * Create a new full production.  Note that the {@link #qName
   * qualified name} needs to be initialized separately.
   *
   * @param attributes The list of attributes.
   * @param dType The declared type.
   * @param name The name.
   * @param choice The choice.
   */
  public FullProduction(List<Attribute> attributes, String dType,
                        NonTerminal name, OrderedChoice choice) {
    super(attributes, dType, name, null, choice);
  }

  /**
   * Create a new full production.  Note that the {@link #qName
   * qualified name} needs to be initialized separately.
   *
   * @param attributes The list of attributes.
   * @param type The actual type.
   * @param name The name.
   * @param qName The fully qualified name.
   * @param choice The choice.
   */
  public FullProduction(List<Attribute> attributes, Type type,
                        NonTerminal name, NonTerminal qName,
                        OrderedChoice choice) {
    super(attributes, type, name, qName, choice);
  }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (! (o instanceof FullProduction)) return false;
    FullProduction other = (FullProduction)o;
    if (! name.equals(other.name)) return false;
    if (null == qName) {
      if (qName != other.qName) return false;
    } else {
      if (! qName.equals(other.qName)) return false;
    }
    if (null == type) {
      if (! dType.equals(other.dType)) return false;
    } else {
      if (! type.equals(other.type)) return false;
    }
    if (! choice.equals(other.choice)) return false;
    return Attribute.areEquivalent(attributes, other.attributes);
  }

  public boolean isFull() {
    return true;
  }

}
