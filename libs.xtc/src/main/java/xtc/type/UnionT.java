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
package xtc.type;

import java.io.IOException;

import java.util.List;

import xtc.util.Nonce;

/**
 * A union type.
 *
 * @author Robert Grimm
 * @version $Revision: 1.20 $
 */
public class UnionT extends StructOrUnionT {

  /**
   * Create a new, incomplete union type.  The newly created union
   * type has a fresh nonce.
   *
   * @param tag The tag.
   * @throws NullPointerException Signals a null tag.
   */
  public UnionT(String tag) {
    super(null, Nonce.create(), tag, null);
  }

  /**
   * Create a new union type with a fresh nonce.  The newly created
   * union type has a fresh nonce.
   *
   * @param tag The tag.
   * @param members The members.
   * @throws NullPointerException Signals a null tag.
   */
  public UnionT(String tag, List<VariableT> members) {
    super(null, Nonce.create(), tag, members);
  }

  /**
   * Create a new union type.
   *
   * @param template The type whose annotations to copy.
   * @param nonce The nonce.
   * @param tag The tag.
   * @param members The members.
   * @throws NullPointerException Signals a null tag.
   */
  public UnionT(Type template, Nonce nonce, String tag,
                List<VariableT> members) {
    super(template, nonce, tag, members);
  }

  public UnionT copy() {
    return new UnionT(this, nonce, name, copy(members));
  }

  public Type.Tag tag() {
    return Type.Tag.UNION;
  }

  public boolean isUnion() {
    return true;
  }

  public UnionT toUnion() {
    return this;
  }

  public void write(Appendable out) throws IOException {
    out.append("union ");
    out.append(name);
  }

}
