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

import java.io.IOException;

/**
 * A repeated grammar element.
 *
 * @author Robert Grimm
 * @version $Revision: 1.11 $
 */
public class Repetition extends Quantification {

  /** Flag for whether the grammar element must appear at least once. */
  public boolean once;

  /**
   * Create a new repetition.
   *
   * @param once Flag for whether the grammar element must appear at least
   *             once.
   * @param element The repeated grammar element.
   */
  public Repetition(boolean once, Element element) {
    super(element);
    this.once    = once;
  }

  public Tag tag() {
    return Tag.REPETITION;
  }

  public int hashCode() {
    return element.hashCode();
  }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (! (o instanceof Repetition)) return false;
    Repetition other = (Repetition)o;
    if (this.once != other.once) return false;
    return element.equals(other.element);
  }

  public void write(Appendable out) throws IOException {
    element.write(out);
    if (once) {
      out.append('+');
    } else {
      out.append('*');
    }
  }

}
