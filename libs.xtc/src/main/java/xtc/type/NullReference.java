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

import java.math.BigInteger;

/**
 * Representation of a null reference.  A null reference represents
 * the null location in memory.  Its type is void; it neither has a
 * base nor an offset.
 *
 * @author Robert Grimm
 * @version $Revision: 1.4 $
 */
public class NullReference extends Reference {

  /** The canonical null reference. */
  public static final NullReference NULL = new NullReference();

  /** Create a new null reference. */
  private NullReference() {
    super(VoidT.TYPE);
  }

  public boolean isNull() {
    return true;
  }

  public boolean hasLocation() {
    return true;
  }

  public BigInteger getLocation(C ops) {
    return BigInteger.ZERO;
  }

  public boolean isConstant() {
    return true;
  }

  public void write(Appendable out) throws IOException {
    out.append("<null>");
  }

}
