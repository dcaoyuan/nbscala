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

import java.math.BigInteger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import xtc.Constants;

import xtc.tree.Attribute;
import xtc.tree.Location;
import xtc.tree.Locatable;
import xtc.tree.Node;
import xtc.tree.Visitor;

import xtc.util.Runtime;

/**
 * The superclass of all types.
 *
 * <p />The class hierarchy for types distinguishes basic from wrapped
 * types, with wrapped types providing additional information for
 * basic types.  For each basic type, this class provides
 * <code>is<i>Name</i>()</code> and <code>to<i>Name</i>()</code>
 * methods to replace instanceof tests and casts, respectively.  For
 * each wrapped type, this class additionally provides a
 * <code>has<i>Name</i>()</code> method, which identifies instances of
 * the wrapped type even if they are wrapped inside another (wrapped)
 * type.  In other words, invocations of <code>has<i>Name</i>()</code>
 * are forwarded across wrapped types while invocations of
 * <code>is<i>Name</i>()</code> only apply to the outermost type
 * object.  For wrapped types, invocations of
 * <code>to<i>Name</i>()</code> are also forwarded across (other)
 * wrapped types.
 *
 * <p />As an example, consider an int type wrapped in an annotated
 * type and an alias type:
 * <pre>
 * Type i = NumberT.INT;
 * Type j = new AnnotatedT(i);
 * Type k = new AliasT("alias", j);
 * </pre>
 * Then the following method invocations have the following results:
 * <pre>
 * k.isAlias()        &rArr; <i>true</i>
 * k.hasAlias()       &rArr; <i>true</i>
 * k.toAlias()        &rArr; <i>k</i>
 *
 * k.isAnnotated()    &rArr; <i>false</i>
 * k.hasAnnotated()   &rArr; <i>true</i>
 * k.toAnnotated()    &rArr; <i>j</i>
 *
 * k.isInteger()      &rArr; <i>false</i>
 * k.toInteger()      &rArr; <i>error</i>
 * </pre>
 * The {@link #resolve()} method can be used to strip any wrapped
 * types:
 * <pre>
 * Type r = k.resolve();
 *
 * r.isAlias()        &rArr; <i>false</i>
 * r.isAnnotated()    &rArr; <i>false</i>
 * r.isInteger()      &rArr; <i>true</i>
 * r.toInteger()      &rArr; <i>i</i>
 * </pre>
 *
 * <p />The {@link Tag} enumeration also identifies particular types.
 * A type's tag can be accessed through {@link #tag()}, which is
 * forwarded across wrapped types, and through {@link #wtag()}, which
 * is <em>not</em> forwarded across wrapped types.  As a result,
 * <code>tag()</code> identifies basic types independent of whether
 * they are wrapped or not, while <code>wtag()</code> always
 * identifies the outermost type:
 * <pre>
 * k.tag()            &rArr; <i>Tag.INTEGER</i>
 * k.wtag()           &rArr; <i>Tag.ALIAS</i>
 *
 * i.tag()            &rArr; <i>Tag.INTEGER</i>
 * i.tag()            &rArr; <i>Tag.INTEGER</i>
 * </pre>
 *
 * <p />Each type can have one or more of the following
 * annotations:<ul>
 *
 * <li>Its source location represented as a {@link Location}.</li>
 *
 * <li>Its source language represented as a {@link Language} tag.</li>
 *
 * <li>Its scope represented as a {@link String}.</li>
 *
 * <li>Its constant value represented as a {@link Constant}.</li>
 *
 * <li>Its memory shape represented as a {@link Reference}.  Only
 * lvalues can have a shape.</li>
 *
 * <li>Its attributes represented as a list of {@link Attribute}
 * values.</li>
 *
 * </ul>
 * For each kind of annotation, this class defines tester, getter, and
 * setter methods.  The tester and getter methods come in two
 * versions, one that is forwarded across wrapped types and one that
 * uses a boolean parameter to control forwarding.
 *
 * @author Robert Grimm
 * @version $Revision: 1.112 $
 */
public abstract class Type extends Node {

  /**
   * A type's tag.  Only leaves of the type hierarchy have their own
   * tags.  A type's tag is accessed through {@link #tag()} and {@link
   * #wtag()}.
   */
  public static enum Tag {
    /** A boolean. */
    BOOLEAN,
    /** An array. */
    ARRAY,
    /** A class. */
    CLASS,
    /** An interface. */
    INTERFACE,
    /** A function. */
    FUNCTION,
    /** A method. */
    METHOD,
    /** A named parameter. */
    NAMED_PARAMETER,
    /** An internal parameter. */
    INTERNAL_PARAMETER,
    /** A wildcard. */
    WILDCARD,
    /** A pointer. */
    POINTER,
    /** A struct. */
    STRUCT,
    /** A tuple. */
    TUPLE,
    /** A union. */
    UNION,
    /** A variant. */
    VARIANT,
    /** An error. */
    ERROR,
    /** An internal type. */
    INTERNAL,      
    /** A label. */
    LABEL,
    /** A float. */
    FLOAT,
    /** An integer. */
    INTEGER,
    /** A package. */
    PACKAGE,
    /** A unit type. */
    UNIT,
    /** A void type. */
    VOID,
    /** An alias. */
    ALIAS,
    /** An annotated type. */
    ANNOTATED,
    /** An enumerator. */
    ENUMERATOR,
    /** An enum. */
    ENUM,
    /** An instantiated type. */
    INSTANTIATED,
    /** A parameterized type. */
    PARAMETERIZED,
    /** A variable. */
    VARIABLE
  }

  // =========================================================================

  /** The flag for whether this type is sealed. */
  boolean sealed;

  // This type's location is implicit in the Node.

  /** This type's language. */
  Language language;

  /** This type's scope. */
  String scope;

  /** This type's constant value. */
  Constant constant;

  /** This type's shape. */
  Reference shape;

  /** This type's attributes. */
  List<Attribute> attributes;

  // =========================================================================

  /**
   * Create a new type.  The newly created type does not have any
   * annotations and is not sealed.
   */
  public Type() { /* Nothing to do. */ }

  /**
   * Create a new type.  The newly created type is not sealed.  Its
   * annotations are a copy of the specified template's annotations.
   *
   * @param template The type whose annotations to copy.
   */
  public Type(Type template) {
    if (null == template) return;

    setLocation(template);
    this.language = template.language;
    this.scope    = template.scope;
    this.constant = template.constant;
    this.shape    = template.shape;
    if (null != template.attributes) {
      this.attributes = new ArrayList<Attribute>(template.attributes);
    }
  }

  // =========================================================================

  /**
   * Create a deep copy of this type.  The resulting type is not
   * sealed.
   *
   * @return A deep copy of this type.
   */
  public abstract Type copy();

  // =========================================================================

  /**
   * Determine whether this type is sealed.
   *
   * @return <code>true</code> if this type is sealed.
   */
  public boolean isSealed() {
    return sealed;
  }

  /**
   * Seal this type.  Subclasses that reference other types must
   * override this method and, if the instance is not sealed, first
   * invoke the superclass' version and then seal all referenced
   * types.  For example, if a subclass references a single type
   * <code>type</code>, the corresponding overridden method reads:
   * <pre>
   * public Type seal() {
   *   if (! isSealed()) {
   *     super.seal();
   *     type.seal();
   *   }
   *   return this;
   * }
   * </pre>
   * First testing whether a type is sealed and then invoking the
   * superclass' <code>seal()</code> method avoids infinite recursions
   * for mutually recursive types.
   *
   * @see #seal(List)
   *
   * @return This type.
   */
  public Type seal() {
    sealed = true;
    return this;
  }

  /**
   * Ensure that this type is not sealed.  This method must be called
   * by any subclass before modifying its internal state.
   *
   * @throws IllegalStateException Signals that this type is sealed.
   */
  protected void checkNotSealed() {
    if (sealed) {
      throw new IllegalStateException("Type " + this + " is sealed");
    }
  }

  // =========================================================================

  /**
   * Annotate this type.  If this type is not an annotated type or a
   * sealed annotated type, this method wraps this type in a new
   * {@link AnnotatedT}.
   *
   * @return The annotated type.
   */
  public Type annotate() {
    return isAnnotated() && (! isSealed()) ? this : new AnnotatedT(this);
  }

  /**
   * Deannotate this type.  This method strips away any {@link
   * AnnotatedT} from this type.
   *
   * @return The deannotated type.
   */
  public Type deannotate() {
    Type t = this;
    while (t.isAnnotated()) t = t.toAnnotated().getType();
    return t;
  }

  // =========================================================================

  public Object setProperty(String name, Object value) {
    checkNotSealed();
    return super.setProperty(name, value);
  }

  public Object removeProperty(String name) {
    checkNotSealed();
    return super.removeProperty(name);
  }

  public Set<String> properties() {
    if (sealed) {
      return Collections.unmodifiableSet(super.properties());
    } else {
      return properties();
    }
  }

  // =========================================================================

  /**
   * Determine whether this type or any wrapped type has a location.
   * Calling this method on type <code>t</code> is equivalent to:
   * <pre>
   * t.hasLocation(true)
   * </pre>
   *
   * @see #hasLocation(boolean)
   *
   * @return <code>true</code> if this type or any wrapped type has a
   *   location.
   */
  public boolean hasLocation() {
    return hasLocation(true);
  }

  /**
   * Determine whether this type or any wrapped type has a location.
   *
   * @param forward The flag for whether to forward this method across
   *   wrapped types.
   * @return <code>true</code> if this type or any wrapped type has a
   *   location.
   */
  public boolean hasLocation(boolean forward) {
    return super.hasLocation();
  }

  /**
   * Get this type's or any wrapped type's location.  Calling this
   * method on type <code>t</code> is equivalent to:
   * <pre>
   * t.getLocation(true)
   * </pre>
   *
   * @see #getLocation(boolean)
   *
   * @return The location or <code>null</code> if this type or any
   *   wrapped type does not have a location.
   */
  public Location getLocation() {
    return getLocation(true);
  }

  /**
   * Get this type's or any wrapped type's location.
   *
   * @param forward The flag for whether to forward this method across
   *   wrapped types.
   * @return The location or <code>null</code> if this type or any
   *   wrapped type does not have a location.
   */
  public Location getLocation(boolean forward) {
    return super.getLocation();
  }

  /**
   * Set this type's location.
   *
   * @param location The location.
   * @return This type.
   * @throws IllegalStateException Signals that this type is sealed.
   */
  public Type locate(Location location) {
    setLocation(location);
    return this;
  }

  /**
   * Set this type's location.
   *
   * @param locatable The locatable.
   * @return This type.
   * @throws IllegalStateException Signals that this type is sealed.
   */
  public Type locate(Locatable locatable) {
    setLocation(locatable);
    return this;
  }

  public void setLocation(Location location) {
    checkNotSealed();
    super.setLocation(location);
  }

  public void setLocation(Locatable locatable) {
    checkNotSealed();
    super.setLocation(locatable);
  }

  // =========================================================================

  /**
   * Determine whether this type or any wrapped type has a language.
   * Calling this method on type <code>t</code> is equivalent to:
   * <pre>
   * t.hasLanguage(true)
   * </pre>
   *
   * @see #hasLanguage(boolean)
   *
   * @return <code>true</code> if this type or any wrapped type has a
   *   language.
   */
  public boolean hasLanguage() {
    return hasLanguage(true);
  }

  /**
   * Determine whether this type or any wrapped type has a language.
   *
   * @param forward The flag for whether to forward this method across
   *   wrapped types.
   * @return <code>true</code> if this type or any wrapped type has a
   *   language.
   */
  public boolean hasLanguage(boolean forward) {
    return null != language;
  }

  /**
   * Get this type's or any wrapped type's language.  Calling this
   * method on type <code>t</code> is equivalent to:
   * <pre>
   * t.getLanguage(true)
   * </pre>
   *
   * @see #getLanguage(boolean)
   *
   * @return The language or <code>null</code> if this type or any
   *   wrapped type does not have a language.
   */
  public Language getLanguage() {
    return getLanguage(true);
  }

  /**
   * Get this type's or any wrapped type's language.
   *
   * @param forward The flag for whether to forward this method across
   *   wrapped types.
   * @return The language or <code>null</code> if this type or any
   *   wrapped type does not have a language.
   */
  public Language getLanguage(boolean forward) {
    return language;
  }

  /**
   * Set this type's language.
   *
   * @param language The language.
   * @return This type.
   * @throws IllegalStateException Signals that this type is sealed.
   */
  public Type language(Language language) {
    checkNotSealed();
    this.language = language;
    return this;
  }

  // =========================================================================

  /**
   * Determine whether this type or any wrapped type has a scope.
   * Calling this method on type <code>t</code> is equivalent to:
   * <pre>
   * t.hasScope(true)
   * </pre>
   *
   * @see #hasScope(boolean)
   *
   * @return <code>true</code> if this type or any wrapped type has a
   *   scope.
   */
  public boolean hasScope() {
    return hasScope(true);
  }

  /**
   * Determine whether this type or any wrapped type has a scope.
   *
   * @param forward The flag for whether to forward this method across
   *   wrapped types.
   * @return <code>true</code> if this type or any wrapped type has a
   *   scope.
   */
  public boolean hasScope(boolean forward) {
    return null != scope;
  }

  /**
   * Get this type's or any wrapped type's scope.  Calling this method
   * on type <code>t</code> is equivalent to:
   * <pre>
   * t.getScope(true)
   * </pre>
   *
   * @see #getScope(boolean)
   *
   * @return The scope or <code>null</code> if this type or any
   *   wrapped type does not have a scope.
   */
  public String getScope() {
    return getScope(true);
  }

  /**
   * Get this type's or any wrapped type's scope.
   *
   * @param forward The flag for whether to forward this method across
   *   wrapped types.
   * @return The scope or <code>null</code> if this type or any
   *   wrapped type does not have a scope.
   */
  public String getScope(boolean forward) {
    return scope;
  }

  /**
   * Set this type's scope.
   *
   * @param scope The scope.
   * @return This type.
   * @throws IllegalStateException Signals that this type is sealed.
   */
  public Type scope(String scope) {
    checkNotSealed();
    this.scope = scope;
    return this;
  }

  // =========================================================================

  /**
   * Determine whether this type or any wrapped type has a constant.
   * Calling this method on type <code>t</code> is equivalent to:
   * <pre>
   * t.hasConstant(true)
   * </pre>
   *
   * @see #hasConstant(boolean)
   *
   * @return <code>true</code> if this type has a constant.
   */
  public boolean hasConstant() {
    return hasConstant(true);
  }

  /**
   * Determine whether this type or any wrapped type has a constant.
   *
   * @param forward The flag for whether to forward this method across
   *   wrapped types.
   * @return <code>true</code> if this type or any wrapped type has a
   *   constant.
   */
  public boolean hasConstant(boolean forward) {
    return null != constant;
  }

  /**
   * Get this type's or any wrapped type's constant.  Calling this
   * method on type <code>t</code> is equivalent to:
   * <pre>
   * t.getConstant(true)
   * </pre>
   *
   * @see #getConstant(boolean)
   *
   * @return The constant or <code>null</code> if this type or any
   *   wrapped type does not have a constant.
   */
  public Constant getConstant() {
    return getConstant(true);
  }

  /**
   * Get this type's or any wrapped type's constant.
   *
   * @param forward The flag for whether to forward this method across
   *   wrapped types.
   * @return The constant or <code>null</code> if this type or any
   *   wrapped type does not have a constant.
   */
  public Constant getConstant(boolean forward) {
    return constant;
  }

  /**
   * Set this type's constant.
   *
   * @see #constant(Object)
   *
   * @param value The value.
   * @return This type.
   * @throws IllegalStateException Signals that this type is sealed.
   */
  public Type constant(boolean value) {
    return constant(value ? BigInteger.ONE : BigInteger.ZERO);
  }

  /**
   * Set this type's constant.
   *
   * @param value The value.
   * @return This type.
   * @throws IllegalArgumentException Signals an invalid value.
   * @throws IllegalStateException Signals that this type is sealed.
   */
  public Type constant(Object value) {
    checkNotSealed();
    this.constant = new Constant(value);
    return this;
  }

  // =========================================================================

  /**
   * Determine whether this type or any wrapped type has a shape.
   * Calling this method on type <code>t</code> is equivalent to:
   * <pre>
   * t.hasShape(true)
   * </pre>
   *
   * @see #hasShape(boolean)
   *
   * @return <code>true</code> if this type or any wrapped type has a
   *   shape.
   */
  public boolean hasShape() {
    return hasShape(true);
  }

  /**
   * Determine whether this type or any wrapped type has a shape.
   *
   * @param forward The flag for whether to forward this method across
   *   wrapped types.
   * @return <code>true</code> if this type or any wrapped type has a
   *   shape.
   */
  public boolean hasShape(boolean forward) {
    return null != shape;
  }

  /**
   * Get this type's or any wrapped type's shape.  Calling this method
   * on type <code>t</code> is equivalent to:
   * <pre>
   * t.getShape(true)
   * </pre>
   *
   * @see #getShape(boolean)
   *
   * @return The shape or <code>null</code> if this type or any
   *   wrapped type does not have a shape.
   */
  public Reference getShape() {
    return getShape(true);
  }

  /**
   * Get this type's or any wrapped type's shape.
   *
   * @param forward The flag for whether to forward this method across
   *   wrapped types.
   * @return The shape or <code>null</code> if this type or any
   *   wrapped type does not have a shape.
   */
  public Reference getShape(boolean forward) {
    return shape;
  }

  /**
   * Set this type's shape to a variable reference with the specified
   * name.
   *
   * @see StaticReference
   * @see DynamicReference
   * @see #shape(Reference)
   *
   * @param isStatic The flag for whether the variable is static.
   * @param name The variable name.
   * @return This type.
   * @throws IllegalStateException Signals that this type is sealed.
   */
  public Type shape(boolean isStatic, String name) {
    if (isStatic) {
      return shape(new StaticReference(name, resolve()));
    } else {
      return shape(new DynamicReference(name, resolve()));
    }
  }

  /**
   * Set this type's shape.
   *
   * @param shape The shape represented as a reference.
   * @return This type.
   * @throws IllegalStateException Signals that this type is sealed.
   */
  public Type shape(Reference shape) {
    checkNotSealed();
    this.shape = shape;
    return this;
  }

  // =========================================================================

  /**
   * Determine whether this type has any attributes.  Note that this
   * method does <em>not</em> check any wrapped types.
   *
   * @return <code>true</code> if this type has any attributes.
   */
  public boolean hasAttributes() {
    return ((null != attributes) && (! attributes.isEmpty()));
  }

  /**
   * Get this type's attributes.
   *
   * @return This type's attributes.
   */
  public List<Attribute> attributes() {
    if (null == attributes) {
      return Collections.emptyList();
    } else if (sealed) {
      return Collections.unmodifiableList(attributes);
    } else {
      return attributes;
    }
  }

  // =========================================================================

  /**
   * Determine whether this type or any wrapped type has the specified
   * attribute.  Calling this method on type <code>t</code> is
   * equivalent to:
   * <pre>
   * t.hasAttribute(att, true)
   * </pre>
   *
   * @see #hasAttribute(Attribute,boolean)
   *
   * @param att The attribute.
   * @return <code>true</code> if this type or any wrapped type has
   *   the attribute.
   */
  public boolean hasAttribute(Attribute att) {
    return hasAttribute(att, true);
  }

  /**
   * Determine whether this type or any wrapped type has the specified
   * attribute.
   *
   * @param att The attribute.
   * @param forward The flag for whether to forward this method across
   *   wrapped types.
   * @return <code>true</code> if this type or any wrapped type has
   *   the attribute.
   */
  public boolean hasAttribute(Attribute att, boolean forward) {
    return ((null != attributes) && attributes.contains(att));
  }

  /**
   * Determine whether this type has an attribute with the specified
   * name.  Calling this method on type <code>t</code> is equivalent
   * to:
   * <pre>
   * null != t.getAttribute(name, true)
   * </pre>
   *
   * @see #getAttribute(String,boolean)
   *
   * @param name The name.
   * @return <code>true</code> if this type or any wrapped type has
   *   an attribute with the specified name.
   */
  public boolean hasAttribute(String name) {
    return null != getAttribute(name);
  }

  /**
   * Determine whether this type has an attribute with the specified
   * name.  Calling this method on type <code>t</code> is equivalent
   * to:
   * <pre>
   * null != t.getAttribute(name, forward)
   * </pre>
   *
   * @link #getAttribute(String,boolean)
   *
   * @param name The name.
   * @param forward The flag for whether to forward this method across
   *   wrapped types.
   * @return <code>true</code> if this type or any wrapped type has
   *   an attribute with the specified name.
   */
  public boolean hasAttribute(String name, boolean forward) {
    return null != getAttribute(name, forward);
  }

  /**
   * Get the attribute with the specified name.  Calling this method
   * on type <code>t</code> is equivalent to:
   * <pre>
   * t.getAttribute(name, true)
   * </pre>
   *
   * @see #getAttribute(String,boolean)
   *
   * @param name The name.
   * @return An attribute with that name or <code>null</code> if this
   *   type or any wrapped type does not have such an attribute.
   */
  public Attribute getAttribute(String name) {
    return getAttribute(name, true);
  }

  /**
   * Get the attribute with the specified name.
   *
   * @param name The name.
   * @param forward The flag for whether to forward this method across
   *   wrapped types.
   * @return An attribute with the name or <code>null</code> if this
   *   or any wrapped type does not have such an attribute.
   */
  public Attribute getAttribute(String name, boolean forward) {
    return Attribute.get(name, attributes);
  }

  // =========================================================================

  /**
   * Add the specified attribute.  This method adds the specified
   * attribute to this type's list of attributes &mdash; without
   * checking whether the type already has that attribute.  For almost
   * all applications of attributes, it is preferable to use {@link
   * #attribute(Attribute)}, {@link #attribute(List)}, or {@link
   * #attribute(Type)}.
   *
   * @param att The new attribute.
   * @throws IllegalStateException Signals that this type is sealed.
   */
  public void addAttribute(Attribute att) {
    checkNotSealed();
    if (null == attributes) attributes = new ArrayList<Attribute>();
    attributes.add(att);
  }

  /**
   * Remove the specified attribute.  Note that this method does
   * <em>not</em> remove the attribute from any wrapped types.
   *
   * @param att The attribute.
   * @return <code>true</code> if this type had the specified
   *   attribute.
   * @throws IllegalStateException Signals that this type is sealed.
   */
  public boolean removeAttribute(Attribute att) {
    checkNotSealed();
    return null != attributes ? attributes.remove(att) : false;
  }

  // =========================================================================

  /**
   * Annotate this type with the specified attribute.  If this type or
   * any wrapped type does not have the specified attribute, this
   * method adds the attribute to this type's list of attributes.
   *
   * @param att The attribute.
   * @return This type.
   * @throws IllegalStateException Signals that this type is sealed.
   */
  public Type attribute(Attribute att) {
    if (! hasAttribute(att)) {
      addAttribute(att);
    }
    return this;
  }

  /**
   * Annotate this type with the specified attributes.
   *
   * @see #attribute(Attribute)
   *
   * @param attributes The attributes.
   * @return This type.
   * @throws IllegalStateException Signals that this type is sealed.
   */
  public Type attribute(List<Attribute> attributes) {
    for (Attribute att : attributes) {
      if (! hasAttribute(att)) addAttribute(att);
    }
    return this;
  }

  /**
   * Annotate this type with the specified type's attributes.
   *
   * @param template The type whose annotations to copy.
   * @return This type.
   * @throws IllegalStateException Signals that this type is sealed.
   */
  public Type attribute(Type template) {
    do {
      // If the template has any attributes, copy them.
      if (template.hasAttributes()) {
        for (Attribute att : template.attributes()) {
          if (! hasAttribute(att)) addAttribute(att);
        }
      }

      // If the template is a wrapped type, continue with the wrapped type.
      template = template.isWrapped() ? template.toWrapped().getType() : null;
    } while (null != template);

    // Done.
    return this;
  }

  // =========================================================================

  /**
   * Mark the specified node as having this type.  This method sets
   * the node's {@link Constants#TYPE type property} to this type.
   *
   * @param node The node.
   * @throws IllegalArgumentException Signals that the node already
   *   has a type property.
   */
  public void mark(Node node) {
    if (node.hasProperty(Constants.TYPE)) {
      throw new IllegalArgumentException("Node " + node + " already has type");
    } else {
      node.setProperty(Constants.TYPE, this);
    }
  }

  // =========================================================================

  /**
   * Determine whether this type has the specified tag.  Invocations
   * to this method are forwarded across wrapped types.  Calling this
   * method on type <code>t</code> is equivalent to:
   * <pre>
   * tag == t.tag()
   * </pre>
   *
   * @see #tag()
   *
   * @param tag The tag.
   * @return <code>true</code> if this type has the specified tag.
   */
  public boolean hasTag(Tag tag) {
    return tag == tag();
  }

  /**
   * Get this type's tag.  Invocations to this method are forwarded
   * across wrapped types.
   *
   * @see #wtag()
   *
   * @return This type's tag.
   */
  public abstract Tag tag();

  /**
   * Determine whether this wrapped type has the specified tag.
   * Invocations to this method are <em>not</em> forwarded across
   * wrapped types.  Calling this method on type <code>t</code> is
   * equivalent to:
   * <pre>
   * tag == wtag()
   * </pre>
   *
   * @see #wtag()
   *
   * @param tag The tag.
   * @return <code>true</code> if this wrapped type has the specified
   *   tag.
   */
  public boolean hasWTag(Tag tag) {
    return tag == wtag();
  }

  /**
   * Get this wrapped type's tag.  Invocations to this method are
   * <em>not</em> forwarded across wrapped types.
   *
   * @see #tag()
   *
   * @return This wrapped type's tag.
   */
  public Tag wtag() {
    return tag();
  }

  // =========================================================================

  /**
   * Determine whether this type is an error.
   *
   * @link #hasError()
   *
   * @return <code>true</code> if this type is internal.
   */
  public boolean isError() {
    return false;
  }

  /**
   * Determine whether this type has an error.  This method identifies
   * the {@link ErrorT error type} even if it is wrapped.  Calling this
   * method on type <code>t</code> is equivalent to:
   * <pre>
   * Type.Tag.Error == tag()
   * </pre>
   *
   * @see #tag()
   *
   * @return <code>true</code> if this type has an error.
   */
  public boolean hasError() {
    return Tag.ERROR == tag();
  }

  // =========================================================================

  /**
   * Determine whether this type is a type parameter.
   *
   * @return <code>true</code> if this type is a parameter.
   */
  public boolean isParameter() {
    return false;
  }

  /**
   * Get this type as a type parameter.
   *
   * @return This type as a parameter.
   * @throws ClassCastException Signals that this type is not a
   *   parameter.
   */
  public Parameter toParameter() {
    throw new ClassCastException("Not a parameter " + this);
  }

  /**
   * Determine whether this type is a named parameter.
   *
   * @return <code>true</code> if this type is a named parameter.
   */
  public boolean isNamedParameter() {
    return false;
  }

  /**
   * Get this type as a named parameter.
   *
   * @return This type as a named parameter.
   * @throws ClassCastException Signals that this type is not a
   *   named parameter.
   */
  public NamedParameter toNamedParameter() {
    throw new ClassCastException("Not a named parameter " + this);
  }

  /**
   * Determine whether this type is an internal parameter.
   *
   * @return <code>true</code> if this type is an internal parameter.
   */
  public boolean isInternalParameter() {
    return false;
  }

  /**
   * Get this type as an internal parameter.
   *
   * @return This type as an internal parameter.
   * @throws ClassCastException Signals that this type is not an
   *   internal parameter.
   */
  public InternalParameter toInternalParameter() {
    throw new ClassCastException("Not an internal parameter " + this);
  }

  /**
   * Determine whether this type is a wildcard.
   *
   * @return <code>true</code> if this type is a wildcard.
   */
  public boolean isWildcard() {
    return false;
  }

  /**
   * Get this type as a wildcard.
   *
   * @return This type as a wildcard.
   * @throws ClassCastException Signals that this type is not a
   *   wildcard.
   */
  public Wildcard toWildcard() {
    throw new ClassCastException("Not a wildcard " + this);
  }

  // =========================================================================

  /**
   * Determine whether this type is void.
   *
   * @return <code>true</code> if this type is void.
   */
  public boolean isVoid() {
    return false;
  }

  /**
   * Get this type as a void type.
   *
   * @return This type as a void type.
   * @throws ClassCastException Signals that this type is not a void
   *   type.
   */
  public VoidT toVoid() {
    throw new ClassCastException("Not a void " + this);
  }

  /**
   * Determine whether this type is the unit type.
   *
   * @return <code>true</code> if this type is the unit type.
   */
  public boolean isUnit() {
    return false;
  }

  /**
   * Get this type as a unit type.
   *
   * @return This type as a unit type.
   * @throws ClassCastException Signals that this type is not a unit
   *   type.
   */
  public UnitT toUnit() {
    throw new ClassCastException("Not a unit " + this);
  }

  /**
   * Determine whether this type is a boolean.
   *
   * @return <code>true</code> if this type is a boolean.
   */
  public boolean isBoolean() {
    return false;
  }

  /**
   * Get this type as a boolean.
   *
   * @return This type as a boolean.
   * @throws ClassCastException Signals that this type is a not a
   *   boolean.
   */
  public BooleanT toBoolean() {
    throw new ClassCastException("Not a boolean " + this);
  }

  /**
   * Determine whether this type is a number.
   *
   * @return <code>true</code> if this type is a number.
   */
  public boolean isNumber() {
    return false;
  }

  /**
   * Get this type as a number.
   *
   * @return This type as a number.
   * @throws ClassCastException Signals that this type is not a
   *   number.
   */
  public NumberT toNumber() {
    throw new ClassCastException("Not a number " + this);
  }

  /**
   * Determine whether this type is an integer.
   *
   * @return <code>true</code> if this type is an integer.
   */
  public boolean isInteger() {
    return false;
  }

  /**
   * Get this type as an integer.
   *
   * @return This type as an integer.
   * @throws ClassCastException Signals that this type is not an
   *   integer.
   */
  public IntegerT toInteger() {
    throw new ClassCastException("Not an integer " + this);
  }

  /**
   * Determine whether this type is a float.
   *
   * @return <code>true</code> if this type is a float.
   */
  public boolean isFloat() {
    return false;
  }

  /**
   * Get this type as a float.
   *
   * @return This type as a float.
   * @throws ClassCastException Signals that this type is not a
   *   float.
   */
  public FloatT toFloat() {
    throw new ClassCastException("Not a float " + this);
  }

  /**
   * Determine whether this type is internal.
   *
   * @return <code>true</code> if this type is internal.
   */
  public boolean isInternal() {
    return false;
  }

  /**
   * Get this type as an internal type.
   *
   * @return This type as an internal type.
   * @throws ClassCastException Signals that this type is not an
   *   internal type.
   */
  public InternalT toInternal() {
    throw new ClassCastException("Not an internal type " + this);
  }

  /**
   * Determine whether this type is a label.
   *
   * @return <code>true</code> if this type is a label.
   */
  public boolean isLabel() {
    return false;
  }

  /**
   * Get this type as a label.
   *
   * @return This type as a label.
   * @throws ClassCastException Signals that this type is not a
   *   label.
   */
  public LabelT toLabel() {
    throw new ClassCastException("Not a label " + this);
  }

  /**
   * Determine whether this type is a package.
   *
   * @return <code>true</code> if this type is a package.
   */
  public boolean isPackage() {
    return false;
  }

  /**
   * Get this type as a package.
   *
   * @return This type as a package.
   * @throws ClassCastException Signals that this type is not a
   *   package.
   */
  public PackageT toPackage() {
    throw new ClassCastException("Not a package " + this);
  }

  /**
   * Determine whether this type is derived.
   *
   * @return <code>true</code> if this type is derived.
   */
  public boolean isDerived() {
    return false;
  }

  /**
   * Determine whether this type is a pointer.
   *
   * @return <code>true</code> if this type is a pointer.
   */
  public boolean isPointer() {
    return false;
  }

  /**
   * Get this type as a pointer.
   *
   * @return This type as a pointer.
   * @throws ClassCastException Signals that this type is not a
   *   pointer.
   */
  public PointerT toPointer() {
    throw new ClassCastException("Not a pointer " + this);
  }

  /**
   * Determine whether this type is an array.
   *
   * @return <code>true</code> if this type is an array.
   */
  public boolean isArray() {
    return false;
  }

  /**
   * Get this type as an array.
   *
   * @return This type as an array.
   * @throws ClassCastException Signals that this type is not an
   *   array.
   */
  public ArrayT toArray() {
    throw new ClassCastException("Not an array " + this);
  }

  /**
   * Determine whether this type contains a struct or union.
   *
   * @return <code>true</code> if this type contains a struct or
   *   union.
   */
  public boolean hasStructOrUnion() {
    switch (tag()) {
    case STRUCT:
    case UNION:
      return true;
    default:
      return false;
    }
  }

  /**
   * Get this type as a struct or union.
   *
   * @return This type as a struct or union.
   * @throws ClassCastException Signals that this type is not a struct
   *   or union.
   */
  public StructOrUnionT toStructOrUnion() {
    throw new ClassCastException("Not a struct or union " + this);
  }

  /**
   * Determine whether this type is a struct.
   *
   * @return <code>true</code> if this type is a struct.
   */
  public boolean isStruct() {
    return false;
  }

  /**
   * Get this type as a struct.
   *
   * @return This type as a struct.
   * @throws ClassCastException Signas that this type is not a
   *    struct.
   */
  public StructT toStruct() {
    throw new ClassCastException("Not a struct " + this);
  }

  /**
   * Determine whether this type is a union.
   *
   * @return <code>true</code> if this type is a union.
   */
  public boolean isUnion() {
    return false;
  }

  /**
   * Get this type as a union.
   *
   * @return This type as a union.
   * @throws ClassCastException Signals that this type is not a
   *   union.
   */
  public UnionT toUnion() {
    throw new ClassCastException("Not a union " + this);
  }

  /**
   * Determine whether this type is a function.
   *
   * @return <code>true</code> if this type is a function.
   */
  public boolean isFunction() {
    return false;
  }

  /**
   * Get this type as a function.
   *
   * @return This type has a function.
   * @throws ClassCastException Signals that this type is not a
   *   function.
   */
  public FunctionT toFunction() {
    throw new ClassCastException("Not a function " + this);
  }

  /**
   * Determine whether this type is a method.
   *
   * @return <code>true</code> if this type is a method.
   */
  public boolean isMethod() {
    return false;
  }

  /**
   * Get this type as a method.
   *
   * @return This type as a method.
   * @throws ClassCastException Signals that this type is not a
   *   method.
   */
  public MethodT toMethod() {
    throw new ClassCastException("Not a method " + this);
  }

  /**
   * Determine whether this type is a class.
   *
   * @return <code>true</code> if this type is a class.
   */
  public boolean isClass() {
    return false;
  }

  /**
   * Get this type as a class.
   *
   * @return This type as a class.
   * @throws ClassCastException Signals that this type is not a class.
   */
  public ClassT toClass() {
    throw new ClassCastException("Not a class " + this);
  }

  /**
   * Determine whether this type is an interface.
   *
   * @return <code>true</code> if this type is an interface.
   */
  public boolean isInterface() {
    return false;
  }

  /**
   * Get this type as an interface.
   *
   * @return This type as an interface.
   * @throws ClassCastException Signals that this type is not an
   *   interface.
   */
  public InterfaceT toInterface() {
    throw new ClassCastException("Not an interface " + this);
  }

  /**
   * Determine whether this type is an tuple.
   *
   * @return <code>true</code> if this type is an tuple.
   */
  public boolean isTuple() {
    return false;
  }

  /**
   * Get this type as an tuple.
   *
   * @return This type as an tuple.
   * @throws ClassCastException Signals that this type is not an tuple.
   */
  public TupleT toTuple() {
    throw new ClassCastException("Not an tuple " + this);
  }

  /**
   * Determine whether this type is an variant.
   *
   * @return <code>true</code> if this type is an variant.
   */
  public boolean isVariant() {
    return false;
  }

  /**
   * Get this type as an variant.
   *
   * @return This type as an variant.
   * @throws ClassCastException Signals that this type is not an variant.
   */
  public VariantT toVariant() {
    throw new ClassCastException("Not an variant " + this);
  }

  // =========================================================================

  /**
   * Determine whether this type is wrapped.
   *
   * @return <code>true</code> if this type is wrapped.
   */
  public boolean isWrapped() {
    return false;
  }

  /**
   * Get this type as a wrapped type.
   *
   * @return This type as a wrapped type.
   * @throws ClassCastException Signals that this type is not wrapped.
   */
  public WrappedT toWrapped() {
    throw new ClassCastException("Not a wrapped type " + this);
  }

  // =========================================================================

  /**
   * Determine whether this type is annotated.
   *
   * @return <code>true</code> if this type is annotated.
   */
  public boolean isAnnotated() {
    return false;
  }

  /**
   * Determine whether this type has an annotated type.
   *
   * @return <code>true</code> if this type has an annotated type.
   */
  public boolean hasAnnotated() {
    return false;
  }

  /**
   * Get this type as an annotated type.
   *
   * @return This type as an annotated type.
   * @throws ClassCastException Signas that this type is not an
   *   annotated type.
   */
  public AnnotatedT toAnnotated() {
    throw new ClassCastException("Not an annotated type " + this);
  }

  /**
   * Determine whether this type is an alias.
   *
   * @return <code>true</code> if this type is an alias.
   */
  public boolean isAlias() {
    return false;
  }

  /**
   * Determine whether this type contains an alias.
   *
   * @return <code>true</code> if this type contains an alias.
   */
  public boolean hasAlias() {
    return false;
  }

  /**
   * Get this type as an alias.
   *
   * @return This type as an alias.
   * @throws ClassCastException Signals that this type is not an
   *   alias.
   */
  public AliasT toAlias() {
    throw new ClassCastException("Not an alias " + this);
  }

  /**
   * Determine whether this type is an enum.
   *
   * @return <code>true</code> if this type is an enum.
   */
  public boolean isEnum() {
    return false;
  }

  /**
   * Determine whether this type contains an enum.
   *
   * @return <code>true</code> if this type contains an enum.
   */
  public boolean hasEnum() {
    return false;
  }

  /**
   * Get this type as an enum.
   *
   * @return This type as an enum.
   * @throws ClassCastException Signals that this type is not an enum.
   */
  public EnumT toEnum() {
    throw new ClassCastException("Not an enum " + this);
  }

  /**
   * Determine whether this type is an enumerator.
   *
   * @return <code>true</code> if this type is an enumerator.
   */
  public boolean isEnumerator() {
    return false;
  }

  /**
   * Determine whether this type contains an enumerator.
   *
   * @return <code>true</code> if this type contains an enumerator.
   */
  public boolean hasEnumerator() {
    return false;
  }

  /**
   * Get this type as an enumerator.
   *
   * @return This type as an enumerator.
   * @throws ClassCastException Signals that this type is not an
   *   enumerator.
   */
  public EnumeratorT toEnumerator() {
    throw new ClassCastException("Not an enumerator " + this);
  }

  /**
   * Determine whether this type is instantiated.
   *
   * @return <code>true</code> if this type is instantiated.
   */
  public boolean isInstantiated() {
    return false;
  }

  /**
   * Determine whether this type has an instantiated type.
   *
   * @return <code>true</code> if this type has an instantiated type.
   */
  public boolean hasInstantiated() {
    return false;
  }

  /**
   * Get this type as an instantiated type.
   *
   * @return This type as an instantiated type.
   * @throws ClassCastException Signas that this type is not an
   *   instantiated type.
   */
  public InstantiatedT toInstantiated() {
    throw new ClassCastException("Not an instantiated type " + this);
  }

  /**
   * Determine whether this type is parameterized.
   *
   * @return <code>true</code> if this type is parameterized.
   */
  public boolean isParameterized() {
    return false;
  }

  /**
   * Determine whether this type has a parameterized type.
   *
   * @return <code>true</code> if this type has a parameterized type.
   */
  public boolean hasParameterized() {
    return false;
  }

  /**
   * Get this type as a parameterized type.
   *
   * @return This type as a parameterized type.
   * @throws ClassCastException Signas that this type is not a
   *   parameterized type.
   */
  public ParameterizedT toParameterized() {
    throw new ClassCastException("Not a parameterized type " + this);
  }

  /**
   * Determine whether this type is a variable.
   *
   * @return <code>true</code> if this type is a variable.
   */
  public boolean isVariable() {
    return false;
  }

  /**
   * Determine whether this type contains a variable.
   *
   * @return <code>true</code> if this type contains a variable.
   */
  public boolean hasVariable() {
    return false;
  }

  /**
   * Get this type as a variable.
   *
   * @return This type as a variable.
   * @throws ClassCastException Signals that this type does not
   *   contain a variable.
   */
  public VariableT toVariable() {
    throw new ClassCastException("Not a variable " + this);
  }

  // =========================================================================

  /**
   * Determine whether this type is tagged.
   *
   * @return <code>true</code> if this type is tagged.
   */
  public boolean hasTagged() {
    return false;
  }

  /**
   * Get this type as a tagged type.
   *
   * @see #hasTagged()
   *
   * @return This type as a tagged type.
   * @throws ClassCastException Signals that this type is not
   *   tagged.
   */
  public Tagged toTagged() {
    throw new ClassCastException("Not a tagged type " + this);
  }

  // =========================================================================

  /**
   * Determine whether this type is concrete.  This method returns
   * <code>true</code> if this type is not parameterized or is both
   * parameterized and instantiated.
   *
   * @return <code>true</code> if this type is concrete.
   */
  public boolean isConcrete() {
    return (! hasParameterized()) || hasInstantiated();
  }

  // =========================================================================

  /**
   * Resolve this type.  This method removes any symbolic information,
   * i.e., wrapped types, and returns the underlying, "raw" type.
   *
   * @return The resolved type.
   */
  public Type resolve() {
    return this;
  }

  // =========================================================================

  /**
   * Trace this type to the runtime's console.  This method prints
   * this type to the runtime's console using a new instance of {@link
   * TypePrinter}; it is useful for debugging.
   *
   * @param runtime The runtime.
   */
  public void trace(Runtime runtime) {
    // Save the registered visitor.
    Visitor     visitor = runtime.console().visitor();
    TypePrinter printer = new TypePrinter(runtime.console());
    try {
      printer.dispatch(this);
      runtime.console().pln();
    } finally {
      // Restore the previously registered visitor.
      runtime.console().register(visitor);
    }
    runtime.console().flush();
  }


  // =========================================================================

  /**
   * Cast the specified object to a type.
   *
   * @param type The type as an object.
   * @return The type as a type.
   * @throws ClassCastException Signals that the specified object is
   *   not a type.
   */
  public static Type cast(Object type) {
    return (Type)type;
  }

  /**
   * Resolve the specified object as type.
   *
   * @param type The type.
   * @return The resolved type.
   * @throws ClassCastException Signals that the specified object is
   *   not a type.
   */
  public static Type resolve(Object type) {
    return ((Type)type).resolve();
  }

  // =========================================================================

  /**
   * Copy the specified list of types.  A null list is ignored.
   *
   * @param types The list of types.
   * @return A new list of the types' copies.
   */
  @SuppressWarnings("unchecked")
  public static <T extends Type> List<T> copy(List<T> types) {
    if (null == types) return null;

    List<T> copy = new ArrayList<T>(types.size());
    for (T t : types) {
      // This cast can never fail in the presence of covariant return
      // types for copy().
      copy.add((T)t.copy());
    }
    return copy;
  }

  /**
   * Seal the specified list of types.  A null list is ignored.
   *
   * @param types The list of types.
   * @return An unmodifiable list of sealed types.
   */
  public static <T extends Type> List<T> seal(List<T> types) {
    if (null == types) return null;

    for (T t : types) t.seal();
    return Collections.unmodifiableList(types);
  }

}
