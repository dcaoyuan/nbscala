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

import java.util.List;

import xtc.util.Nonce;

/**
 * The interface of all tagged C types.  Note that all tagged C types
 * must have a name, even if they are unnamed in source code.  For
 * unnamed tagged types, the name must be of the form
 * <code>tag(<i>nonce</i>)</code>.
 *
 * @author Robert Grimm
 * @version $Revision: 1.5 $
 */
public interface Tagged {

  /**
   * Determine whether this tagged type is an enum.
   *
   * @return <code>true</code> if this tagged type is an enum.
   */
  public boolean isEnum();

  /**
   * Determine whether this tagged type is a struct.
   *
   * @return <code>true</code> if this tagged type is a struct.
   */
  public boolean isStruct();

  /**
   * Determine whether this tagged type is a union.
   *
   * @return <code>true</code> if this tagged type is a union.
   */
  public boolean isUnion();

  /**
   * Get this tagged type's nonce.  The nonce is used to determine
   * equality of tagged types while still allowing for several
   * type instances.
   *
   * @return The nonce.
   */
  public Nonce getNonce();

  /**
   * Determine whether this tagged type is unnamed.  Note that an
   * unnamed tagged type still has a name of the form
   * <code>tag(<i>nonce</i>)</code>.
   *
   * @return <code>true</code> if this tagged type is unnamed.
   */
  public boolean isUnnamed();

  /**
   * Determine whether this tagged type has the specified name.
   *
   * @param name The name.
   * @return <code>true</code> if this tagged type has the name.
   */
  public boolean hasName(String name);

  /**
   * Get the name.
   *
   * @return The name.
   */
  public String getName();

  /**
   * Look up the member with the specified name.  If this type is a
   * struct or union and has any unnnamed struct or union fields, this
   * method also tries to look up the name in the unnamed struct or
   * union.
   *
   * @param name The name.
   * @return The type or {@link ErrorT#TYPE} if the tagged type has no
   *   such member.
   */
  public Type lookup(String name);

  /**
   * Get the number of members.  For struct and union types, the
   * returned count excludes any anonymous bit-fields.  It also
   * excludes the individual members of an unnamed struct or union
   * member.
   *
   * @return The number of members or <code>-1</code> if this type is
   *   incomplete.
   */
  public int getMemberCount();

  /**
   * Get the member with the specified index.  For struct and union
   * types, anonymous bit-fields are not considered.
   *
   * @param index The index.
   * @return The corresponding member.
   * @throws IndexOutOfBoundsException Signals that the index is out
   *   of range.
   */
  public Type getMember(int index);

  /**
   * Get the members of the tagged type.
   *
   * @return The list of members or <code>null</code> if this type is
   *   incomplete.
   */
  public List<? extends Type> getMembers();

}
