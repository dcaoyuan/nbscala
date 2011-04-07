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

import xtc.tree.Attribute;
import xtc.tree.Location;

/**
 * The superclass of all wrapped types.  A wrapped type adds (mostly)
 * symbolic information to another, more basic type.
 *
 * @author Robert Grimm
 * @version $Revision: 1.37 $
 */
public abstract class WrappedT extends Type {

  /** The actual type. */
  private Type type;

  /**
   * Create a new wrapped type.
   *
   * @param type The actual type.
   */
  public WrappedT(Type type) {
    this.type = type;
  }

  /**
   * Create a new wrapped type.
   *
   * @param template The type whose annotations to copy.
   * @param type The actual type.
   */
  public WrappedT(Type template, Type type) {
    super(template);
    this.type = type;
  }

  public Type seal() {
    if (! isSealed()) {
      super.seal();
      type.seal();
    }
    return this;
  }

  public Type.Tag tag() {
    return type.tag();
  }

  public boolean isWrapped() {
    return true;
  }

  public WrappedT toWrapped() {
    return this;
  }

  public boolean hasLocation(boolean forward) {
    Location loc = super.getLocation(false);
    return null != loc ? true : forward ? type.hasLocation(true) : false;
  }

  public Location getLocation(boolean forward) {
    Location loc = super.getLocation(false);
    return null != loc ? loc : forward ? type.getLocation(true) : null;
  }

  public boolean hasLanguage(boolean forward) {
    return null != language ? true : forward ? type.hasLanguage(true) : false;
  }

  public Language getLanguage(boolean forward) {
    return null != language ? language : forward ? type.getLanguage(true) : null;
  }

  public boolean hasScope(boolean forward) {
    return null != scope ? true : forward ? type.hasScope(true) : false;
  }

  public String getScope(boolean forward) {
    return null != scope ? scope : forward ? type.getScope(true) : null;
  }

  public boolean hasConstant(boolean forward) {
    return null != constant ? true : forward ? type.hasConstant(true) : false;
  }

  public Constant getConstant(boolean forward) {
    return null != constant ? constant : forward ? type.getConstant(true) : null;
  }

  public boolean hasShape(boolean forward) {
    return null != shape ? true : forward ? type.hasShape(true) : false;
  }

  public Reference getShape(boolean forward) {
    return null != shape ? shape : forward ? type.getShape(true) : null;
  }

  public boolean hasAttribute(Attribute att, boolean forward) {
    return (null != attributes) && attributes.contains(att) ? true :
      forward ? type.hasAttribute(att, true) : false;
  }

  public Attribute getAttribute(String name, boolean forward) {
    Attribute att = Attribute.get(name, attributes);
    return null != att ? att : forward ? type.getAttribute(name, true) : null;
  }

  /**
   * Get the type.
   *
   * @return The type.
   */
  public Type getType() {
    return type;
  }

  /**
   * Set the type.
   *
   * @param type The type.
   * @throws IllegalStateException Signals that this type is sealed.
   */
  public void setType(Type type) {
    checkNotSealed();
    this.type = type;
  }

  public boolean hasAnnotated() {
    return type.hasAnnotated();
  }

  public AnnotatedT toAnnotated() {
    return type.toAnnotated();
  }

  public boolean hasAlias() {
    return type.hasAlias();
  }

  public AliasT toAlias() {
    return type.toAlias();
  }

  public boolean hasEnum() {
    return type.hasEnum();
  }

  public EnumT toEnum() {
    return type.toEnum();
  }

  public boolean hasEnumerator() {
    return type.hasEnumerator();
  }

  public EnumeratorT toEnumerator() {
    return type.toEnumerator();
  }

  public boolean hasVariable() {
    return type.hasVariable();
  }

  public VariableT toVariable() {
    return type.toVariable();
  }

  public boolean hasInstantiated() {
    return type.hasInstantiated();
  }

  public InstantiatedT toInstantiated() {
    return type.toInstantiated();
  }

  public boolean hasParameterized() {
    return type.hasParameterized();
  }

  public ParameterizedT toParameterized() {
    return type.toParameterized();
  }

  public StructOrUnionT toStructOrUnion() {
    return type.toStructOrUnion();
  }

  public boolean hasTagged() {
    return type.hasTagged();
  }

  public Tagged toTagged() {
    return type.toTagged();
  }

  public Type resolve() {
    return type.resolve();
  }

  /**
   * Get this type's hash code.  This method forwards the method
   * invocation to the wrapped type.
   *
   * @return The hash code.
   */
  public int hashCode() {
    return type.hashCode();
  }

  /**
   * Determine whether this type equals the specified object.  This
   * method forwards the method invocation to the wrapped type.
   *
   * @param o The object.
   * @return <code>true</code> if this type equals the object.
   */
  public boolean equals(Object o) {
    return getType().equals(o);
  }

}
