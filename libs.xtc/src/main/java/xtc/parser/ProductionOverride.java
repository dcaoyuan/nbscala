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

import java.util.List;

import xtc.tree.Attribute;

/**
 * A production override.  This class represents a modification that
 * replaces a production's alternatives.  It can also specify new
 * attributes for a production.  Either the attributes or the choice
 * may be <code>null</code>, in which case the original production's
 * attributes or choice, respectively, remain unmodified.  If the
 * choice is not <code>null</code>, the {@link #isComplete} flag
 * specifies whether the choice replaces the original production's
 * choice or only specific alternatives.
 *
 * @author Robert Grimm
 * @version $Revision: 1.7 $
 */
public class ProductionOverride extends PartialProduction {

  /**
   * The flag for whether the choice overrides the entire production
   * or only specific alternatives.
   */
  public boolean isComplete;

  /**
   * Create a new production override.
   *
   * @param attributes The attributes.
   * @param dType The declared type.
   * @param name The name.
   */
  public ProductionOverride(List<Attribute> attributes, String dType,
                            NonTerminal name) {
    super(attributes, dType, name, null);
    isComplete = false;
  }

  /**
   * Create a new production override.
   *
   * @param dType The declared type.
   * @param name The name.
   * @param choice The choice.
   * @param isComplete The completeness flag.
   */
  public ProductionOverride(String dType, NonTerminal name, OrderedChoice choice,
                            boolean isComplete) {
    super(null, dType, name, choice);
    this.isComplete = isComplete;
  }

  public boolean isOverride() {
    return true;
  }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (! (o instanceof ProductionOverride)) return false;
    ProductionOverride other = (ProductionOverride)o;
    if (isComplete != other.isComplete) return false;
    if (! name.equals(other.name)) return false;
    if (null == type) {
      if (! dType.equals(other.dType)) return false;
    } else {
      if (! type.equals(other.type)) return false;
    }
    if (null == choice) {
      if (choice != other.choice) return false;
    } else {
      if (! choice.equals(other.choice)) return false;
    }
    return Attribute.areEquivalent(attributes, other.attributes);
  }

}
