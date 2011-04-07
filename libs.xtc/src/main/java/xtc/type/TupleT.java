/*
 * xtc - The eXTensible Compiler
 * Copyright (C) 2007 Robert Grimm
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import xtc.util.Utilities;

/**
 * A tuple type.  Tuples may be either anonymous, without a name, or
 * named.  For named tuples, this class provides accessors to the full
 * name and the simple name, which is the full name without any
 * qualifier.
 *
 * @author Robert Grimm
 * @version $Revision: 1.7 $
 */
public class TupleT extends DerivedT {

  /** The qualified name. */
  final private String qname;

  /** The simple name. */
  final private String sname;

  /** The element types. */
  private List<Type> types;

  /** Create a new incomplete and anonymous tuple type. */
  public TupleT() {
    this(null, null, null);
  }

  /**
   * Create a new anonymous tuple type.
   *
   * @param types The types.
   */
  public TupleT(List<Type> types) {
    this(null, null, types);
  }

  /**
   * Create a new incomplete tuple type.
   *
   * @param name The name.
   */
  public TupleT(String name) {
    this(null, name, null);
  }

  /**
   * Create a new tuple type.
   *
   * @param name The name.
   * @param type The only element type.
   */
  public TupleT(String name, Type type) {
    this(null, name, new ArrayList<Type>(1));
    types.add(type);
  }

  /**
   * Create a new tuple type.
   *
   * @param name The name.
   * @param types The element types.
   */
  public TupleT(String name, List<Type> types) {
    this(null, name, types);
  }

  /**
   * Create a new tuple type.
   *
   * @param template The type whose annotations to copy.
   * @param name The name.
   * @param types The element types.
   */
  public TupleT(Type template, String name, List<Type> types) {
    super(template);
    this.qname = name;
    this.sname = (null == name) ? null : Utilities.unqualify(name);
    this.types = types;
  }

  public Type seal() {
    if (! isSealed()) {
      super.seal();
      types = Type.seal(types);
    }
    return this;
  }

  public TupleT copy() {
    return new TupleT(this, qname, copy(types));
  }

  public Type.Tag tag() {
    return Type.Tag.TUPLE;
  }

  public boolean isTuple() {
    return true;
  }

  public TupleT toTuple() {
    return this;
  }

  /**
   * Determine whether this tuple has a name.
   *
   * @return <code>true</code> if this tuple has a name.
   */
  public boolean hasName() {
    return null != qname;
  }

  /**
   * Determine whether this tuple has the specified name.
   *
   * @param name The name.
   * @return <code>true</code> if this tuple has the name.
   */
  public boolean hasName(String name) {
    return name.equals(this.qname);
  }

  /**
   * Determine whether this tuple has the specified simple name.
   *
   * @param name The simple name.
   * @return <code>true</code> if this tuple has the simple name.
   */
  public boolean hasSimpleName(String name) {
    return name.equals(this.sname);
  }

  /**
   * Get this tuple's name.
   *
   * @return The name or <code>null</code> if this tuple is anonymous.
   */
  public String getName() {
    return qname;
  }

  /**
   * Get this tuple's simple name.
   *
   * @return The simple name or <code>null</code> if this tuple is
   *   anonymous.
   */
  public String getSimpleName() {
    return sname;
  }

  /**
   * Get the element types.
   *
   * @return The element types.
   */
  public List<Type> getTypes() {
    return types;
  }

  /**
   * Set the element types.
   *
   * @param types The new element types.
   */
  public void setTypes(List<Type> types) {
    checkNotSealed();
    this.types = types;
  }

  public int hashCode() {
    int hash = 0;
    if (null != qname) hash = qname.hashCode();
    if (null != types) hash = 7 * hash + types.hashCode();
    return hash;
  }

  public boolean equals(Object o) {
    if (! (o instanceof Type)) return false;
    Type t = resolve(o);

    if (this == t) return true;
    if (! t.isTuple()) return false;
    TupleT other = t.toTuple();

    if (null == qname) {
      if (null != other.qname) return false;
    } else {
      if (! qname.equals(other.qname)) return false;
    }
    return null == types ? null == other.types : types.equals(other.types);
  }

  public void write(Appendable out) throws IOException {
    if (null == qname) {
      out.append("<anonymous>");
    } else {
      out.append(qname);
    }
    out.append('(');
    if (null == types) {
      out.append("...");
    } else {
      for (Iterator<Type> iter = types.iterator(); iter.hasNext(); ) {
        iter.next().write(out);
        if (iter.hasNext()) out.append(", ");
      }
    }
    out.append(')');
  }

}
