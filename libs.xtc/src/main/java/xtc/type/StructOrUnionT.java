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

import java.util.List;

import xtc.util.Nonce;

/**
 * The superclass of struct and union types.
 *
 * @author Robert Grimm
 * @version $Revision: 1.42 $
 */
public abstract class StructOrUnionT extends DerivedT implements Tagged {

  /** The nonce. */
  protected final Nonce nonce;

  /** The name. */
  protected final String name;

  /** The list of members represents as {@link VariableT}. */
  protected List<VariableT> members;

  /**
   * Create a new struct or union type.
   *
   * @param template The type whose annotations to copy.
   * @param nonce The nonce.
   * @param name The name.
   * @param members The members.
   * @throws NullPointerException Signals a null name.
   */
  public StructOrUnionT(Type template, Nonce nonce, String name,
                        List<VariableT> members) {
    super(template);
    if (null == name) throw new NullPointerException("Null name");
    this.nonce   = nonce;
    this.name    = name;
    this.members = members;
  }

  /**
   * Seal this struct or union.  If this struct or union is
   * incomplete, i.e., does not have any members, invocations to this
   * method have no effect.
   */
  public Type seal() {
    if (null != members) {
      if (! isSealed()) {
        super.seal();
        members = Type.seal(members);
      }
    }
    return this;
  }

  public StructOrUnionT toStructOrUnion() {
    return this;
  }

  public boolean hasTagged() {
    return true;
  }

  public Tagged toTagged() {
    return this;
  }

  public Nonce getNonce() {
    return nonce;
  }

  public boolean isUnnamed() {
    return name.startsWith("tag(");
  }

  public boolean hasName(String name) {
    return name.equals(this.name);
  }

  public String getName() {
    return name;
  }

  public Type lookup(String name) {
    for (VariableT member : members) {
      if (member.hasName(name)) return member;

      if ((! member.hasName()) && (! member.hasWidth())) {
        // The member is an unnamed struct or union.
        Type nested = ((StructOrUnionT)member.resolve()).lookup(name);
        if (! nested.isError()) return nested;
      }
    }
    return ErrorT.TYPE;
  }

  public int getMemberCount() {
    if (null == members) {
      return -1;
    } else {
      int count = 0;
      for (VariableT member : members) {
        // If the member has a name, it counts.  If the member does
        // not have a name and does not have a width, it is an
        // unnamed struct/union and counts.
        if (member.hasName() || (! member.hasWidth())) count++;
      }
      return count;
    }
  }

  public VariableT getMember(int index) {
    int count = -1;
    for (VariableT member: members) {
      if (member.hasName() || (! member.hasWidth())) {
        count++;
        if (index == count) return member;
      }
    }
    throw new IndexOutOfBoundsException("Index: "+index+", Size: "+(count+1));
  }

  public List<VariableT> getMembers() {
    return members;
  }

  /**
   * Set the members.
   *
   * @param members The members.
   * @throws IllegalStateException Signals that this type is sealed.
   */
  public void setMembers(List<VariableT> members) {
    checkNotSealed();
    this.members = members;
  }

  public int hashCode() {
    return name.hashCode();
  }

  /**
   * Determine whether this type equals the specified object.  A
   * struct or union equals the specified object if the specified
   * object is a struct or union with the same nonce.
   *
   * @param o The object.
   * @return <code>true</code> if this type equals the object.
   */
  public boolean equals(Object o) {
    if (! (o instanceof Type)) return false;
    Type t = (Type)o;
    return t.hasTagged() && (nonce == t.toTagged().getNonce());
  }

}
