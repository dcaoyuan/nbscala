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

/**
 * An alternative removal.  An alternative removal's attributes,
 * qualified name, and choice fields are ignored.
 *
 * @author Robert Grimm
 * @version $Revision: 1.6 $
 */
public class AlternativeRemoval extends PartialProduction {

  /** The list of {@link SequenceName sequences} to be removed. */
  public List<SequenceName> sequences;

  /**
   * Create a new alternative removal.
   *
   * @param dType The declared type.
   * @param name The name.
   * @param sequences The sequence names.
   */
  public AlternativeRemoval(String dType, NonTerminal name,
                            List<SequenceName> sequences) {
    super(null, dType, name, null);
    this.sequences = sequences;
  }

  public boolean isRemoval() {
    return true;
  }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (! (o instanceof AlternativeRemoval)) return false;
    AlternativeRemoval other = (AlternativeRemoval)o;
    if (! name.equals(other.name)) return false;
    if (null == type) {
      if (! dType.equals(other.dType)) return false;
    } else {
      if (! type.equals(other.type)) return false;
    }
    return sequences.equals(other.sequences);
  }

}
