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

import xtc.Constants;

import xtc.tree.Attribute;
import xtc.tree.Node;

import xtc.type.Type;

import xtc.util.Utilities;

/**
 * The superclass of full and partial productions.
 *
 * @author Robert Grimm
 * @version $Revision: 1.36 $
 */
public abstract class Production extends Node {

  /**
   * The optional attribute list.
   *
   * <p />Note that while a production's attributes are represented as
   * an ordered list, they should be treated as a set.
   */
  public List<Attribute> attributes;

  /** The production's declared type as a string. */
  public String dType;

  /**
   * The production's actual type, which must be filled in after
   * parsing a module.
   */
  public Type type;

  /**
   * The name.  A production's name does not have any qualification
   * when the production is parsed.  However, after uniquely renaming
   * all productions to avoid name conflicts in the generated parser,
   * this name may have a qualifier.
   *
   * @see Analyzer#uniquify()
   */
  public NonTerminal name;

  /** The fully qualified name. */
  public NonTerminal qName;

  /** The ordered choice. */
  public OrderedChoice choice;

  /**
   * Create a new production.
   *
   * @param attributes The attributes.
   * @param dType The declared type.
   * @param name The name.
   * @param qName The fully qualified name.
   * @param choice The choice.
   */
  public Production(List<Attribute> attributes, String dType, NonTerminal name,
                    NonTerminal qName, OrderedChoice choice) {
    this.attributes = attributes;
    this.dType      = Utilities.withoutSpace(dType);
    this.name       = name;
    this.qName      = qName;
    this.choice     = choice;
  }

  /**
   * Create a new production.
   *
   * @param attributes The attributes.
   * @param type The actual type.
   * @param name The name.
   * @param qName The fully qualified name.
   * @param choice The choice.
   */
  public Production(List<Attribute> attributes, Type type, NonTerminal name,
                    NonTerminal qName, OrderedChoice choice) {
    this.attributes = attributes;
    this.type       = type;
    this.name       = name;
    this.qName      = qName;
    this.choice     = choice;
  }

  public int hashCode() {
    return (null == qName)? name.hashCode() : qName.hashCode();
  }

  /**
   * Determine whether this production is a {@link FullProduction full
   * production}.
   *
   * @return <code>true</code> if this production is full.
   */
  public boolean isFull() {
    return false;
  }

  /**
   * Determine whether this production is a {@link PartialProduction
   * partial production}.
   *
   * @return <code>true</code> if this production is partial.
   */
  public boolean isPartial() {
    return false;
  }

  /**
   * Determine whether this production is an {@link
   * AlternativeAddition alternative addition}.
   *
   * @return <code>true</code> if this production is an alternative
   *   addition.
   */
  public boolean isAddition() {
    return false;
  }

  /**
   * Determine whether this production is an {@link AlternativeRemoval
   * alternative removal}.
   *
   * @return <code>true</code> if this production is an alternative
   *   removal.
   */
  public boolean isRemoval() {
    return false;
  }

  /**
   * Determine whether this production is a {@link ProductionOverride
   * production override}.
   *
   * @return <code>true</code> if this production is a production
   *   override.
   */
  public boolean isOverride() {
    return false;
  }

  /**
   * Determine whether this production has the specified attribute.
   *
   * @param att The attribute.
   * @return <code>true</code> if this production has the specified
   *   attribute.
   */
  public boolean hasAttribute(Attribute att) {
    return ((null != attributes) && attributes.contains(att));
  }

  /**
   * Determine whether this production is memoized.  This method is
   * semantically equivalent to:
   * <pre>
   * hasAttribute(Constants.ATT_MEMOIZED) ||
   * ((! hasAttribute(Constants.ATT_TRANSIENT)) &&
   *  (! hasAttribute(Constants.ATT_INLINE)))
   * </pre>
   *
   * @return <code>true</code> if this production is memoized.
   */
  public boolean isMemoized() {
    return ((null == attributes) ||
            attributes.contains(Constants.ATT_MEMOIZED) ||
            ((! attributes.contains(Constants.ATT_TRANSIENT)) &&
             (! attributes.contains(Constants.ATT_INLINE))));
  }

}
