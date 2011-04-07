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
 * A variant type.  Variants can be monomorphic or polymorphic, with
 * the latter possibly sharing tuples with other variants.
 * Monomorphic variants must be named and are compared through name
 * equivalence.  Polymorphic variants may be anonymous and are
 * compared through structural equivalence.
 *
 * @author Robert Grimm
 * @version $Revision: 1.12 $
 */
public class VariantT extends DerivedT {

  /** The qualified name. */
  final private String qname;

  /** The qualifier. */
  final private String qualifier;

  /** The simple name. */
  final private String sname;

  /** The flag for whether the variant is polymorphic. */
  final private boolean polymorphic;

  /** The list of tuples. */
  private List<TupleT> tuples;

  /**
   * Create a new incomplete variant type.  The new variant type is
   * not polymorphic.
   *
   * @param name The name.
   * @throws NullPointerException Signals that the name is null.
   */
  public VariantT(String name) {
    this(null, name, false, null);
  }

  /**
   * Create a new variant type.  The new variant type is not
   * polymorphic.
   *
   * @param name The name.
   * @param tuples The tuples.
   * @throws NullPointerException Signals that the name is null.
   */
  public VariantT(String name, List<TupleT> tuples) {
    this(null, name, false, tuples);
  }

  /**
   * Create a new variant type.  Note that polymorphic variants may be
   * anonymous, i.e., have a null name.
   *
   * @param name The name.
   * @param polymorphic The flag for polymorphic variants.
   * @param tuples The tuples.
   * @throws NullPointerException Signals that the name is null for a
   *   monomorphic variant.
   */
  public VariantT(String name, boolean polymorphic, List<TupleT> tuples) {
    this(null, name, polymorphic, tuples);
  }

  /**
   * Create a new variant type.
   *
   * @param template The type whose annotations to copy.
   * @param name The name.
   * @param polymorphic The flag for polymorphic variants.
   * @param tuples The tuples.
   * @throws NullPointerException Signals that the name is null for a
   *   monomorphic variant.
   */
  public VariantT(Type template, String name, boolean polymorphic,
                  List<TupleT> tuples) {
    super(template);
    this.qname       = name;
    this.qualifier   = (null == name) ? null : Utilities.getQualifier(name);
    this.sname       = (null == name) ? null : Utilities.unqualify(name);
    this.polymorphic = polymorphic;
    this.tuples      = tuples;
    if (null == name && ! polymorphic) {
      throw new NullPointerException("Null name");
    }
  }

  public Type seal() {
    if (! isSealed()) {
      super.seal();
      tuples = Type.seal(tuples);
    }
    return this;
  }

  public VariantT copy() {
    return new VariantT(this, qname, polymorphic, copy(tuples));
  }

  public Type.Tag tag() {
    return Type.Tag.VARIANT;
  }

  public boolean isVariant() {
    return true;
  }

  public VariantT toVariant() {
    return this;
  }

  /**
   * Get this variant's name.
   *
   * @return The name or <code>null</code> if this variant is anonymous.
   */
  public String getName() {
    return qname;
  }

  /**
   * Get this variant's qualifier.
   *
   * @return The qualifier or <code>null</code> if this variant does
   *   not have a qualified name.
   */
  public String getQualifier() {
    return qualifier;
  }

  /**
   * Get this variant's simple name.
   *
   * @return The simple name or <code>null</code> if this variant is
   *   anonymous.
   */
  public String getSimpleName() {
    return sname;
  }

  /**
   * Determine whether the variant is polymorphic.
   *
   * @return <code>true</code> if the variant is polymorphic.
   */
  public boolean isPolymorphic() {
    return polymorphic;
  }

  /**
   * Look up the tuple with the specified name.
   *
   * @param name The name.
   * @return The tuple or {@link ErrorT#TYPE} if this variant has no
   *   such tuple.
   */
  public Type lookup(String name) {
    for (TupleT tuple : tuples) if (tuple.hasName(name)) return tuple;
    return ErrorT.TYPE;
  }

  /**
   * Look up the tuple with the specified simple name.
   *
   * @param name The simple name.
   * @return The first such tuple or {@link ErrorT#TYPE} if this
   *   variant has no such tuple.
   */
  public Type lookupSimple(String name) {
    for (TupleT tuple : tuples) if (tuple.hasSimpleName(name)) return tuple;
    return ErrorT.TYPE;
  }

  /**
   * Get this variant's tuples.
   *
   * @return The list of tuples.
   */
  public List<TupleT> getTuples() {
    return tuples;
  }

  /**
   * Set this variant's tuples.
   *
   * @param tuples The new list of tuples.
   */
  public void setTuples(List<TupleT> tuples) {
    checkNotSealed();
    this.tuples = tuples;
  }

  public int hashCode() {
    if (polymorphic) {
      return null == tuples ? 0 : tuples.hashCode();
    } else {
      return qname.hashCode();
    }
  }

  /**
   * Determine whether this variant equals the specified object.  This
   * method implements name equivalence for non-polymorphic variants
   * and structural equivalence for polymorphic variants.
   *
   * @param o The object.
   * @return <code>true</code> if this variant equals the object.
   */
  public boolean equals(Object o) {
    if (! (o instanceof Type)) return false;
    Type t = resolve(o);

    if (this == t) return true;
    if (! t.isVariant()) return false;
    VariantT other = t.toVariant();

    if (polymorphic != other.polymorphic) return false;
    if (! polymorphic) return qname.equals(other.qname);
    if (null == tuples) return null == other.tuples;
    if (tuples.size() != other.tuples.size()) return false;

    for (TupleT tuple : tuples) {
      if (! other.tuples.contains(tuple)) return false;
    }

    for (TupleT tuple : other.tuples) {
      if (! tuples.contains(tuple)) return false;
    }

    return true;
  }

  public void write(Appendable out) throws IOException {
    if (null != qname) {
      out.append("variant ");
      out.append(qname);

    } else {
      out.append("variant(");
      for (Iterator<TupleT> iter = tuples.iterator(); iter.hasNext(); ) {
        out.append('`');
        iter.next().write(out);
        if (iter.hasNext()) out.append(", ");
      }
      out.append(')');
    }
  }

}
