/*
 * xtc - The eXTensible Compiler
 * Copyright (C) 2004-2007 Robert Grimm
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * version 2.1 as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301,
 * USA.
 */
package xtc.tree;

import java.io.IOException;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import xtc.util.Pair;
import xtc.util.Utilities;

/**
 * A node in an abstract syntax tree.
 *
 * <p />Subclasses may optionally support two features.  First, a
 * subclass may support <strong>generic tree traversal</strong>.  Such
 * a class must override {@link #hasTraversal()} to return
 * <code>true</code> and must provide meaningful implementations for
 * {@link #size()}, {@link #get(int)}, and {@link #set(int,Object)}.
 * Second, a subclass may support a <strong>variable number of
 * children</strong>.  Such a class must override {@link
 * #hasVariable()} to return <code>true</code> and must provide
 * meaningful implementations for {@link #add(Object)}, {@link
 * #add(int,Object)}, and {@link #remove(int)}.
 *
 * @author Robert Grimm
 * @version $Revision: 1.55 $
 */
public abstract class Node implements Iterable<Object>, Locatable {

  /** The properties. */
  Map<String, Object> properties;
  
  /** The optional source location. */
  Location location;

  // ========================================================================

  /** Create a new node. */
  public Node() { /* Nothing to do. */ }

  /**
   * Create a new node.
   *
   * @param location The source location for the new node.
   */
  public Node(Location location) {
    this.location = location;
  }

  // ========================================================================

  /**
   * Determine whether this node is a token.  User-specified classes
   * must not override this method.
   *
   * @see Token
   *
   * @return <code>true</code> if this node is a token.
   */
  public boolean isToken() {
    return false;
  }

  /**
   * Get this node as a token.  User-specified classes must not
   * override this method.
   *
   * @return This node as a token.
   * @throws ClassCastException Signals that this node is not a token.
   */
  public Token toToken() {
    throw new ClassCastException("Not a token");
  }

  /**
   * Treat this node as a token and get its text.  This method strips
   * away any annotations, treats the resulting node as a token, and
   * returns its text.  User-specified classes must not override this
   * method.
   *
   * @see #strip()
   *
   * @return The token's text.
   * @throws ClassCastException Signals that this node is not a token.
   */
  public String getTokenText() {
    throw new ClassCastException("Not a token");
  }

  /**
   * Determine whether this node is an annotation.  User-specified
   * classes must not override this method.
   * 
   * @see Annotation
   *
   * @return <code>true</code> if this node is an annotation.
   */
  public boolean isAnnotation() {
    return false;
  }

  /**
   * Get this node as an annotation.  User-specified classes must not
   * override this method.
   *
   * @return This node as an annotation.
   * @throws ClassCastException Signals that this node is not an
   *   annotation.
   */
  public Annotation toAnnotation() {
    throw new ClassCastException("Not an annotation");
  }

  /**
   * Determine whether this node is a generic node.  User-specified
   * classes must not override this method.
   *
   * @see GNode
   *
   * @return <code>true</code> if this node is a generic node.
   */
  public boolean isGeneric() {
    return false;
  }

  // ========================================================================

  /**
   * Get the name of this node.  For strongly typed nodes, the name is
   * implicitly specified by the node's class.  For generic nodes, the
   * name is the generic node's explicit name.  The default
   * implementation returns the node's class name.
   *
   * <p />User-specified classes must not override this method.
   *
   * @return The name.
   */
  public String getName() {
    return getClass().getName();
  }

  /**
   * Determine whether this node's name is the same as the specified
   * name.
   *
   * <p />User-specified classes must not override this method.
   *
   * @param name The name.
   * @return <code>true</code> if this node's name equals the
   *   specified name.
   */
  public boolean hasName(final String name) {
    return getClass().getName().equals(name);
  }

  // ========================================================================

  /**
   * Set the value of a property.
   *
   * @param name The property name.
   * @param value The new property value.
   * @return The property's old value or <code>null</code> if the
   * property didn't have a value.
   */
  public Object setProperty(String name, Object value) {
    if (null == properties) {
      properties = new HashMap<String, Object>();
    }
    return properties.put(name, value);
  }

  /**
   * Test if this node has a property.
   *
   * @param name The property name.
   * @return <code>true</code> if this node has a property with the
   *   specified name.
   */
  public boolean hasProperty(String name) {
    if (null == properties) {
      return false;
    } else {
      return properties.containsKey(name);
    }
  }

  /**
   * Get a property value.
   *
   * @param name The property name.
   * @return The property's value or <code>null</code> if the
   *   property doesn't have a value.
   */
  public Object getProperty(String name) {
    if (null == properties) {
      return null;
    } else {
      return properties.get(name);
    }
  }

  /**
   * Get a property value as a boolean.  If this node does not have a
   * property with the specified name, this method returns
   * <code>false</code>.
   *
   * @param name The property name.
   * @return The property's value as a boolean.
   */
  public boolean getBooleanProperty(String name) {
    if (null == properties) {
      return false;
    } else {
      Object o = properties.get(name);

      if (null == o) {
        return false;
      } else {
        return (Boolean)o;
      }
    }
  }

  /**
   * Get a property value as a string.
   *
   * @param name The property name.
   * @return The property's value as a string.
   */
  public String getStringProperty(String name) {
    if (null == properties) {
      return null;
    } else {
      return (String)properties.get(name);
    }
  }

  /**
   * Remove a property.
   *
   * @param name The property name.
   * @return The property's old value or <code>null</code> if the
   *   property didn't have a value.
   */
  public Object removeProperty(String name) {
    if (null == properties) {
      return null;
    } else {
      return properties.remove(name);
    }
  }

  /**
   * Get the set of property names.
   *
   * @return The set of property names.
   */
  public Set<String> properties() {
    if (null == properties) {
      return Collections.emptySet();
    } else {
      return properties.keySet();
    }
  }

  // ========================================================================

  public boolean hasLocation() {
    return null != location;
  }

  public Location getLocation() {
    return location;
  }

  public void setLocation(Location location) {
    this.location = location;
  }

  public void setLocation(Locatable locatable) {
    if (locatable.hasLocation()) this.location = locatable.getLocation();
  }

  // ========================================================================

  /**
   * Determine whether this node supports generic traversal of its
   * children.  The default implementation returns <code>false</code>.
   *
   * @return <code>true</code> if this node supports generic traversal
   *   of its children.
   */
  public boolean hasTraversal() {
    return false;
  }

  /**
   * Determine whether this node has no children.
   *
   * @return <code>true</code> if this node has no children.
   * @throws UnsupportedOperationException Signals that this node
   *   does not support generic traversal.
   */
  public boolean isEmpty() {
    return 0 == size();
  }

  /**
   * Get an iterator over this node's children.
   *
   * <p />Note that instance tests on the iterator's objects may not
   * behave as expected.  Notably, any node may be wrapped in
   * annotations.  Furthermore, any string may be wrapped in a token
   * (and, recursively, in annotations).
   *
   * @see Token#test(Object)
   * @see Token#cast(Object)
   * @see GNode#test(Object)
   * @see GNode#cast(Object)
   *
   * @return An iterator over the children.
   */
  public Iterator<Object> iterator() {
    final int size = size();
    return new Iterator<Object>() {
      int cursor = 0;

      public boolean hasNext() {
        return cursor < size;
      }

      public Object next() {
        if (cursor < size) {
          return get(cursor++);
        } else {
          throw new NoSuchElementException();
        }
      }

      public void remove() {
        throw new UnsupportedOperationException("Down with Iterator.remove()");
      }};
  }

  /**
   * Get the number of children.  The default implementation signals
   * an unsupported operation exception.
   *
   * @return The number of children.
   * @throws UnsupportedOperationException Signals that this node
   *   does not support generic traversal.
   */
  public int size() {
    throw new UnsupportedOperationException();
  }

  /**
   * Get the child at the specified index.  The default implementation
   * signals an unsupported operation exception.
   *
   * <p />Note that instance tests on the returned object may not
   * behave as expected.  Notably, any node may be wrapped in
   * annotations.  Furthermore, any string may be wrapped in a token
   * (and, recursively, in annotations).
   *
   * @see Token#test(Object)
   * @see Token#cast(Object)
   * @see GNode#test(Object)
   * @see GNode#cast(Object)
   *
   * @param index The index.
   * @return The child at that positioin.
   * @throws IndexOutOfBoundsException Signals that the index is out
   *   of range.
   * @throws UnsupportedOperationException Signals that this node
   *   does not support generic traversal.
   */
  public Object get(int index) {
    throw new UnsupportedOperationException();
  }

  /**
   * Get the boolean child at the specified index.
   *
   * @param index The index.
   * @return The child at that position as a boolean.
   * @throws IndexOutOfBoundsException Signals that the index is out
   *   of range.
   * @throws ClassCastException Signals that the child is not a
   *   boolean.
   * @throws UnsupportedOperationException Signals that this node does
   *   not support generic traversal.
   */
  public boolean getBoolean(int index) {
    return (Boolean)get(index);
  }

  /**
   * Get the string child at the specified index.  If the child at the
   * specified index is a string, this method returns it.  Otherwise,
   * it casts the child to a node, strips any annotations, and returns
   * the text of the annotated token.
   *
   * @param index The index.
   * @return The child at that position as a string.
   * @throws IndexOutOfBoundsException Signals that the index is out
   *   of range.
   * @throws ClassCastException Signals that the child is not a string
   *   nor an annotated token.
   * @throws UnsupportedOperationException Signals that this node
   *   does not support generic traversal.
   */
  public String getString(int index) {
    Object o = get(index);
    if (null == o) {
      return null;
    } else if (o instanceof String) {
      return (String)o;
    } else {
      return ((Node)o).getTokenText();
    }
  }

  /**
   * Get the node child at the specified index.
   *
   * @param index The index.
   * @return The child at that position as a node.
   * @throws IndexOutOfBoundsException Signals that the index is out
   *   of range.
   * @throws ClassCastException Signals that the child is not a node.
   * @throws UnsupportedOperationException Signals that this node
   *   does not support generic traversal.
   */
  public Node getNode(int index) {
    return (Node)get(index);
  }

  /**
   * Get the generic node child at the specified index.  If the
   * specified child has any {@link Annotation annotations}, they are
   * {@link Node#strip() stripped} before returning the child as a
   * generic node.
   *
   * @param index The index.
   * @return The child at that position as a stripped generic node.
   * @throws IndexOutOfBoundsException Signals that the index is out
   *   of range.
   * @throws ClassCastException Signals that the child is not a node.
   * @throws UnsupportedOperationException Signals that this node
   *   does not support generic traversal.
   */
  public GNode getGeneric(int index) {
    Object o = get(index);
    return (null == o) ? null : (GNode)((Node)o).strip();
  }

  /**
   * Get the list child at the specified index.
   *
   * @param index The index.
   * @return The list child at that position as a list.
   * @throws IndexOutOfBoundsException Signals that the index is out
   *   of range.
   * @throws ClassCastException Signals that the child is not a list.
   * @throws UnsupportedOperationException Signals that this node
   *   does not support generic traversal.
   */
  @SuppressWarnings("unchecked")
  public <T> Pair<T> getList(int index) {
    return (Pair<T>)get(index);
  }

  /**
   * Set the child at the specified index to the specified value.  The
   * default implementation signals an unsupported operation
   * exception.
   *
   * @param index The index.
   * @param value The new value.
   * @return The old value.
   * @throws IllegalStateException Signals that this node is
   *   immutable.
   * @throws IndexOutOfBoundsException Signals that the index is out
   *   of range.
   * @throws ClassCastException Signals that the value is not of the
   *   necessary type.
   * @throws UnsupportedOperationException Signals that this node
   *   does not support generic traversal.
   */
  public Object set(int index, Object value) {
    throw new UnsupportedOperationException();
  }

  /**
   * Determine the index of the specified object.
   *
   * @param o The object.
   * @return The first index of the child equal to the specified
   *   object or -1 if this node does not have the object as a child.
   * @throws UnsupportedOperationException Signals that this node
   *   does not support generic traversal.
   */
  public int indexOf(Object o) {
    final int size = size();
    for (int i=0; i<size; i++) {
      Object child = get(i);
      if (null == o ? null == child : o.equals(child)) return i;
    }
    return -1;
  }

  /**
   * Determine the last index of the specified object.
   *
   * @param o The object.
   * @return The last index of the child equal to the specified object
   *   or -1 if this node does not have the object as a child.
   * @throws UnsupportedOperationException Signals that this node
   *   does not support generic traversal.
   */
  public int lastIndexOf(Object o) {
    for (int i=size()-1; i>=0; i--) {
      Object child = get(i);
      if (null == o ? null == child : o.equals(child)) return i;
    }
    return -1;
  }

  /**
   * Determine whether this node has the specified object as a child.
   *
   * @param o The object.
   * @return <code>true</code> if this node has the specified object
   *   as a child.
   * @throws UnsupportedOperationException Signals that this node
   *   does not support generic traversal.
   */
  public boolean contains(Object o) {
    return -1 != indexOf(o);
  }

  // =======================================================================

  /**
   * Add all of this node's children to the specified collection.
   *
   * @param c The collection.
   * @throws UnsupportedOperationException Signals that this node
   *   does not support generic traversal.
   */
  public void addAllTo(Collection<Object> c) {
    final int size = size();
    for (int i=0; i<size; i++) {
      c.add(get(i));
    }
  }

  // =======================================================================

  /** 
   * Determine whether this node supports a variable number of
   * children.  Any variable-sized node should also support generic
   * traversal.  The default implementation returns
   * <code>false</code>.
   *
   * @see #hasTraversal()
   *
   * @return <code>true</code> if this node supports a variable number
   *   of children.
   */
  public boolean hasVariable() {
    return false;
  }

  /**
   * Add the specified object as a child.  The default implementation
   * signals an unsupported operation exception.
   *
   * @param o The object.
   * @return This node.
   * @throws UnsupportedOperationException Signals that this node does
   *   not support a variable number of children.
   */
  public Node add(Object o) {
    throw new UnsupportedOperationException();
  }

  /**
   * Add the specified node as a child.  For nodes that are not
   * annotations supporting a variable number of children, this method
   * is semantically equivalent to {@link #add(Object)}.  For
   * annotations supporting a variable number of children, this method
   * adds the annotated node.  Any previously added children precede
   * that node and any children added after the call to this method
   * succeed that node.
   *
   * @param node The node.
   * @return This node.
   * @throws IllegalStateException Signals that this method has
   *   already been invoked on an annotation supporting a variable
   *   number of children.
   * @throws UnsupportedOperationException Signals that this node does
   *   not support a variable number of children.
   */
  public Node addNode(Node node) {
    return add(node);
  }

  /**
   * Add the specified object as a child at the specified index.  The
   * default implementation signals an unsupported operation
   * exception.
   *
   * @param index The index.
   * @param o The object.
   * @return This node.
   * @throws UnsupportedOperationException Signals that this node does
   *   not support a variable number of children.
   */
  public Node add(int index, Object o) {
    throw new UnsupportedOperationException();
  }

  /**
   * Add all values in the list starting with the specified pair as
   * children.
   *
   * @param p The pair.
   * @return This node.
   * @throws UnsupportedOperationException Signals that this node does
   *   not support a variable number of children.
   */
  public Node addAll(Pair<?> p) {
    while (Pair.empty() != p) {
      add(p.head());
      p = p.tail();
    }
    return this;
  }

  /**
   * Add all values in the list starting with the specified pair as
   * children at the specified index.
   *
   * @param index The index.
   * @param p The pair.
   * @return This node.
   * @throws UnsupportedOperationException Signals that this node does
   *   not support a variable number of children.
   */
  public Node addAll(int index, Pair<?> p) {
    while (Pair.empty() != p) {
      add(index++, p.head());
      p = p.tail();
    }
    return this;
  }
      
  /**
   * Add all values in the specified collection as children.
   *
   * @param c The collection.
   * @return This node.
   * @throws UnsupportedOperationException Signals that this node does
   *   not support a variable number of children.
   */
  public Node addAll(Collection<?> c) {
    for (Object o : c) add(o);
    return this;
  }

  /**
   * Add all values in the specified collection as children at the
   * specified index.
   *
   * @param index The index.
   * @param c The collection.
   * @return This node.
   * @throws UnsupportedOperationException Signals that this node does
   *   not support a variable number of children.
   */
  public Node addAll(int index, Collection<?> c) {
    for (Object o : c) add(index++, o);
    return this;
  }

  /**
   * Remove the child at the specified index.  The default
   * implementation signals an unsupported operation exception.
   *
   * @param index The index.
   * @return The removed child.
   * @throws IndexOutOfBoundsException Signals that the index is out
   *   of range.
   * @throws UnsupportedOperationException Signals that this node does
   *   not support a variable number of children.
   */
  public Object remove(int index) {
    throw new UnsupportedOperationException();
  }

  // ========================================================================

  /**
   * Strip any annotations.  This method removes any annotations
   * starting with this node.  The default implementation returns this
   * node.
   *
   * @see Annotation
   *
   * @return The node without annotations.
   */
  public Node strip() {
    return this;
  }

  // ========================================================================

  /**
   * Write a human readable representation to the specified
   * appendable.  If this node supports generic traversal, the default
   * implementation writes this node in algebraic term-format;
   * otherwise, it writes the string returned by {@link
   * Object#toString()}.
   *
   * @param out The appendable.
   * @throws IOException Signals an I/O error.
   */
  public void write(Appendable out) throws IOException {
    if (! hasTraversal()) {
      out.append(super.toString());

    } else {
      out.append(getName());
      out.append('(');
      boolean first = true;
      for (Object o : this) {
        if (first) {
          first = false;
        } else {
          out.append(", ");
        }

        if (null == o) {
          out.append("null");
        } else if (o instanceof String) {
          out.append('"');
          Utilities.escape((String)o, out, Utilities.JAVA_ESCAPES);
          out.append('"');
        } else if (o instanceof Node) {
          ((Node)o).write(out);
        } else {
          out.append(o.toString());
        }
      }
      out.append(')');
    }
  }

  /**
   * Return a human readable representation of this node.  The default
   * implementation creates a new string builder, writes this node to
   * the builder, and then returns the corresponding string.
   * Subclasses should typically override {@link #write(Appendable)}.
   *
   * @return A human readable representation.
   */
  public String toString() {
    StringBuilder buf = new StringBuilder();
    try {
      write(buf);
    } catch (IOException x) {
      assert false;
    }
    return buf.toString();
  }

  // ========================================================================

  /**
   * Determine whether the specified object is a list of nodes.
   *
   * @param o The object.
   * @return <code>true</code> if the specified object is a list of
   *   nodes.
   */
  public static final boolean isList(Object o) {
    if (! (o instanceof Pair)) return false;
    if (Pair.EMPTY == o) return true;
    return ((Pair)o).head() instanceof Node;
  }

  /**
   * Convert the specified object to a list of nodes.
   *
   * @param o The object, which must be a list of nodes.
   * @return The object as a list of nodes.
   * @throws ClassCastException Signals that the object is not a list
   *   of nodes.
   */
  @SuppressWarnings("unchecked")
  public static final Pair<Node> toList(Object o) {
    if (isList(o)) {
      return (Pair<Node>)o;
    } else {
      throw new ClassCastException("Not a list of nodes " + o);
    }
  }

}
