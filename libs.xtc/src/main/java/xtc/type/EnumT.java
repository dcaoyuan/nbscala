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
 * An enumerated type.  Note that the list of members for an
 * enumerated type is the list of enumerators.
 *
 * @author Robert Grimm
 * @version $Revision: 1.40 $
 */
public class EnumT extends WrappedT implements Tagged {

  /** The nonce. */
  private final Nonce nonce;

  /** The name. */
  private final String name;

  /** The list of {@link EnumeratorT enumerators}. */
  private List<EnumeratorT> enumerators;

  /**
   * Create a new, incomplete enum type.  The newly created enum type
   * has a fresh nonce and {@link ErrorT#TYPE} as its type.
   *
   * @param name The name.
   * @throws NullPointerException Signals a null name.
   */
  public EnumT(String name) {
    this(null, ErrorT.TYPE, Nonce.create(), name, null);
  }

  /**
   * Create a new enum type.  The newly created enum type has a fresh
   * nonce.
   *
   * @param type The underlying type.
   * @param name The name.
   * @param enumerators The enumerators.
   * @throws NullPointerException Signals a null name.
   */
  public EnumT(Type type, String name, List<EnumeratorT> enumerators) {
    this(null, type, Nonce.create(), name, enumerators);
  }

  /**
   * Create a new enum type.
   *
   * @param template The type whose annotations to copy.
   * @param type The underlying type.
   * @param nonce The nonce.
   * @param name The name.
   * @param enumerators The enumerators.
   * @throws NullPointerException Signals a null name.
   */
  public EnumT(Type template, Type type, Nonce nonce, String name,
               List<EnumeratorT> enumerators) {
    super(template, type);
    if (null == name) throw new NullPointerException("Null name");
    this.nonce       = nonce;
    this.name        = name;
    this.enumerators = enumerators;
  }

  public EnumT copy() {
    return new EnumT(this, getType().copy(), nonce, name, copy(enumerators));
  }

  /**
   * Seal this enum.  If this enum is incomplete, i.e., does not have
   * any enumerators, invocations to this method have no effect.
   */
  public Type seal() {
    if (null != enumerators) {
      if (! isSealed()) {
        super.seal();
        enumerators = Type.seal(enumerators);
      }
    }
    return this;
  }

  public Type.Tag wtag() {
    return Type.Tag.ENUM;
  }

  public boolean isEnum() {
    return true;
  }

  public boolean hasEnum() {
    return true;
  }

  public EnumT toEnum() {
    return this;
  }

  public boolean hasTagged() {
    return true;
  }

  public Tagged toTagged() {
    return this;
  }

  /**
   * Set the type.  This method {@link EnumeratorT#setType(Type) sets}
   * the enumerators' types to the specified type, which should be an
   * {@link IntegerT integer}.  When modifying an incomplete type,
   * this method should be called after the appropriate call to {@link
   * #setMembers(List)}.
   *
   * @param type The type.
   * @throws IllegalStateException Signals that this type is sealed.
   */
  public void setType(Type type) {
    super.setType(type);
    for (EnumeratorT e : enumerators) e.setType(type);
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
    for (EnumeratorT e : enumerators) {
      if (e.hasName(name)) return e;
    }
    return ErrorT.TYPE;
  }

  public int getMemberCount() {
    return null == enumerators ? -1 : enumerators.size();
  }

  public EnumeratorT getMember(int index) {
    return enumerators.get(index);
  }

  public List<EnumeratorT> getMembers() {
    return enumerators;
  }

  /**
   * Set the list of {@link EnumeratorT enumerators}.  This method
   * does <em>not</em> change the type of the enumerators.
   *
   * @param enumerators The enumerators.
   * @throws IllegalStateException Signals that this type is sealed.
   */
  public void setMembers(List<EnumeratorT> enumerators) {
    checkNotSealed();
    this.enumerators = enumerators;
  }

  public int hashCode() {
    return name.hashCode();
  }

  /**
   * Determine whether this type equals the specified object.  This
   * enum type equals the specified object if the object is an enum
   * type with the same nonce.
   *
   * @param o The object.
   * @return <code>true</code> if this type equals the object.
   */
  public boolean equals(Object o) {
    if (! (o instanceof Type)) return false;
    Type t = (Type)o;
    return t.hasTagged() && (nonce == t.toTagged().getNonce());
  }

  public void write(Appendable out) throws IOException {
    out.append("enum ");
    out.append(name);
  }

}
