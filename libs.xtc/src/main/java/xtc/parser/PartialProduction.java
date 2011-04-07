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

/**
 * A partial production.
 *
 * @author Robert Grimm
 * @version $Revision: 1.10 $
 */
public abstract class PartialProduction extends Production {

  /**
   * Create a new partial production.
   *
   * @param attributes The list of attributes.
   * @param dType The declared type.
   * @param name The name.
   * @param choice The choice.
   */
  public PartialProduction(List<Attribute> attributes, String dType,
                           NonTerminal name, OrderedChoice choice) {
    super(attributes, dType, name, null, choice);
  }

  public boolean isPartial() {
    return true;
  }

}
