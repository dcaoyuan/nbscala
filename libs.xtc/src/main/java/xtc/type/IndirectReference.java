/*
 * xtc - The eXTensible Compiler
 * Copyright (C) 2006-2007 Robert Grimm
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
package xtc.type;

import java.io.IOException;

/**
 * Representation of an indirect reference.  
 *
 * @author Robert Grimm
 * @version $Revision: 1.5 $
 */
public class IndirectReference extends RelativeReference {

  /**
   * Create a new indirect reference.  The specified base reference
   * must have a pointer type.  The type of the newly created indirect
   * reference is the pointed-to type, unless that type is an array,
   * in which case the type is the array's element type.
   *
   * @param base The base reference.
   * @throws ClassCastException Signals that the base reference does
   *   not have a pointer type.
   */
  public IndirectReference(Reference base) {
    super(base.type.toPointer().getType(), base);
  }

  public boolean isPrefix() {
    return true;
  }

  public boolean isIndirect() {
    return true;
  }

  public int hashCode() {
    return base.hashCode();
  }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (! (o instanceof IndirectReference)) return false;
    return this.base.equals(((IndirectReference)o).base);
  }

  public void write(Appendable out) throws IOException {
    out.append('*');
    base.write(out);
  }

}
