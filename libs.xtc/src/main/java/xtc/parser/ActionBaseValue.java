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
 * Element to set the semantic value to the result of applying a list
 * of {@link xtc.util.Action actions}.  The actions are applied by
 * invoking {@link ParserBase#apply(Pair,Object)} on the list and a
 * seed value.
 *
 * @author Robert Grimm
 * @version $Revision: 1.11 $
 */
public class ActionBaseValue extends ValueElement {

  /** The binding for the list. */
  public final Binding list;

  /** The binding for the seed. */
  public final Binding seed;

  /**
   * Create a new action base value.
   *
   * @param list The binding for the list.
   * @param seed The binding for the seed.
   */
  public ActionBaseValue(Binding list, Binding seed) {
    this.list = list;
    this.seed = seed;
  }

  public Tag tag() {
    return Tag.ACTION_BASE_VALUE;
  }

  public int hashCode() {
    return 13 * list.hashCode() + seed.hashCode();
  }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (! (o instanceof ActionBaseValue)) return false;
    ActionBaseValue other = (ActionBaseValue)o;
    if (! list.equals(other.list)) return false;
    return seed.equals(other.seed);
  }

}
