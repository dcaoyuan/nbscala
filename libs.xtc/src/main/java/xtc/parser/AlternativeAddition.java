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

/**
 * An alternative addition.  The superclass's attributes are ignored.
 *
 * @author Robert Grimm
 * @version $Revision: 1.5 $
 */
public class AlternativeAddition extends PartialProduction {

  /** The sequence relative to which the choice is to be added. */
  public SequenceName sequence;

  /**
   * The flag for whether the choice is to be added after or before
   * the sequence.
   */
  public boolean isBefore;

  /**
   * Create a new alternative addition.
   *
   * @param dType The declared type.
   * @param name The name.
   * @param choice The choice.
   * @param sequence The sequence.
   * @param isBefore The before flag.
   */
  public AlternativeAddition(String dType, NonTerminal name,
                             OrderedChoice choice, SequenceName sequence,
                             boolean isBefore) {
    super(null, dType, name, choice);
    this.sequence = sequence;
    this.isBefore = isBefore;
  }

  public boolean isAddition() {
    return true;
  }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (! (o instanceof AlternativeAddition)) return false;
    AlternativeAddition other = (AlternativeAddition)o;
    if (! name.equals(other.name)) return false;
    if (null == type) {
      if (! dType.equals(other.dType)) return false;
    } else {
      if (! type.equals(other.type)) return false;
    }
    if (! sequence.equals(other.sequence)) return false;
    return isBefore == other.isBefore;
  }

}
