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
 * An ordered choice of grammar elements.
 *
 * @author Robert Grimm
 * @version $Revision: 1.14 $
 */
public class OrderedChoice extends Element {

  /** The ordered list of sequences. */
  public List<Sequence> alternatives;

  /** Create a new ordered choice with no choices. */
  public OrderedChoice() {
    this.alternatives = new ArrayList<Sequence>();
  }

  /**
   * Create a new ordered choice.
   *
   * @param alternatives The list of alternatives.
   */
  public OrderedChoice(List<Sequence> alternatives) {
    this.alternatives = alternatives;
  }

  /**
   * Create a new ordered choice with the specified element as its
   * only alternative.  The constructor {@link
   * Sequence#ensure(Element) ensures} that the element is a sequence.
   * It also copies the element's location into the newly created
   * choice.
   *
   * @param element The element.
   */
  public OrderedChoice(Element element) {
    alternatives = new ArrayList<Sequence>(1);
    alternatives.add(Sequence.ensure(element));
    setLocation(element);
  }

  public Tag tag() {
    return Tag.CHOICE;
  }

  public int hashCode() {
    return alternatives.hashCode();
  }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (! (o instanceof OrderedChoice)) return false;
    return alternatives.equals(((OrderedChoice)o).alternatives);
  }

}
