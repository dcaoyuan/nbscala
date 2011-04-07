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
 * An optional grammar element.
 *
 * @author Robert Grimm
 * @version $Revision: 1.9 $
 */
public class Option extends Quantification {

  /**
   * Create a new option.
   *
   * @param element The optional grammar element.
   */
  public Option(Element element) {
    super(element);
  }

  public Tag tag() {
    return Tag.OPTION;
  }

  public int hashCode() {
    return element.hashCode();
  }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (! (o instanceof Option)) return false;
    return element.equals(((Option)o).element);
  }

  public void write(Appendable out) throws IOException {
    element.write(out);
    out.append('?');
  }

}
