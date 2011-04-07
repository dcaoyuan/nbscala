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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import xtc.util.Pair;

/**
 * A generic node in an abstract syntax tree.
 *
 * <p />A note on memory conservation: Generic nodes created through
 * the {@link #create(String)} or {@link #create(String,int)} methods
 * can have a variable number of children.  While such nodes provide
 * considerable flexibility in creating and managing an abstract
 * syntax tree, their implementation also has a relatively high memory
 * and thus performance overhead.  Consequently, this class provides
 * another set of <code>create()</code> methods, which directly take
 * the new node's children as arguments and return nodes specialized
 * for that number of children.  After creation, the number of
 * children cannot be changed anymore.  Code using generic nodes can
 * test whether a node supports a variable number of children through
 * {@link #hasVariable()} and convert fixed size nodes into variable
 * sized nodes through {@link #ensureVariable(GNode)}.
 *
 * @author Robert Grimm
 * @version $Revision: 1.48 $
 */
public abstract class GNode extends Node {

  /**
   * The superclass of all generic nodes with a fixed number of
   * children.
   */
  static abstract class Fixed extends GNode {

    /** Create a new fixed node. */
    Fixed(String name) {
      super(name);
    }

    public Node add(Object O) {
      throw new UnsupportedOperationException("Generic node with a fixed " +
                                              "number of children");
    }

    public Node add(int index, Object o) {
      throw new UnsupportedOperationException("Generic node with a fixed " +
                                              "number of children");
    }

    public Node addAll(Pair<?> p) {
      throw new UnsupportedOperationException("Generic node with a fixed " +
                                              "number of children");
    }

    public Node addAll(int index, Pair<?> p) {
      throw new UnsupportedOperationException("Generic node with a fixed " +
                                              "number of children");
    }

    public Node addAll(Collection<?> c) {
      throw new UnsupportedOperationException("Generic node with a fixed " +
                                              "number of children");
    }

    public Node addAll(int index, Collection<?> c) {
      throw new UnsupportedOperationException("Generic node with a fixed " +
                                              "number of children");
    }

    public Object remove(int index) {
      throw new UnsupportedOperationException("Generic node with a fixed " +
                                              "number of children");
    }

  }

  // =======================================================================

  /** A generic node with no children. */
  static class Fixed0 extends Fixed {

    Fixed0(String name) {
      super(name);
    }

    Fixed0(Fixed0 node) {
      super(node.name);
    }

    public int size() {
      return 0;
    }

    public Object get(int index) {
      throw new IndexOutOfBoundsException("Index: "+index+", Size: 0");
    }

    public Object set(int index, Object value) {
      throw new IndexOutOfBoundsException("Index: "+index+", Size: 0");
    }

    public void addAllTo(Collection<Object> c) {
      // Nothing to do.
    }

  }

  // =======================================================================

  /** A generic node with one child. */
  static class Fixed1 extends Fixed {

    Object c1;

    Fixed1(String name, Object c1) {
      super(name);
      this.c1 = c1;
    }

    Fixed1(Fixed1 node) {
      this(node.name, node.c1);
    }

    public int size() {
      return 1;
    }

    public Object get(int index) {
      if (0 == index) {
        return c1;
      } else {
        throw new IndexOutOfBoundsException("Index: "+index+", Size: 1");
      }
    }

    public Object set(int index, Object value) {
      if (0 == index) {
        Object old = c1;
        c1         = value;
        return old;
      } else {
        throw new IndexOutOfBoundsException("Index: "+index+", Size: 1");
      }
    }

    public void addAllTo(Collection<Object> c) {
      c.add(c1);
    }

  }

  // =======================================================================

  /** A generic node with two children. */
  static class Fixed2 extends Fixed {

    Object c1;
    Object c2;

    Fixed2(String name, Object c1, Object c2) {
      super(name);
      this.c1 = c1;
      this.c2 = c2;
    }

    Fixed2(Fixed2 node) {
      this(node.name, node.c1, node.c2);
    }

    public int size() {
      return 2;
    }

    public Object get(int index) {
      switch (index) {
      case 0:
        return c1;
      case 1:
        return c2;
      default:
        throw new IndexOutOfBoundsException("Index: "+index+", Size: 2");
      }
    }

    public Object set(int index, Object value) {
      Object old;

      switch (index) {
      case 0:
        old = c1;
        c1  = value;
        return old;
      case 1:
        old = c2;
        c2  = value;
        return old;
      default:
        throw new IndexOutOfBoundsException("Index: "+index+", Size: 2");
      }
    }

    public void addAllTo(Collection<Object> c) {
      c.add(c1);
      c.add(c2);
    }

  }

  // =======================================================================

  /** A generic node with three children. */
  static class Fixed3 extends Fixed {

    Object c1;
    Object c2;
    Object c3;

    Fixed3(String name, Object c1, Object c2, Object c3) {
      super(name);
      this.c1 = c1;
      this.c2 = c2;
      this.c3 = c3;
    }

    Fixed3(Fixed3 node) {
      this(node.name, node.c1, node.c2, node.c3);
    }

    public int size() {
      return 3;
    }

    public Object get(int index) {
      switch (index) {
      case 0:
        return c1;
      case 1:
        return c2;
      case 2:
        return c3;
      default:
        throw new IndexOutOfBoundsException("Index: "+index+", Size: 3");
      }
    }

    public Object set(int index, Object value) {
      Object old;

      switch (index) {
      case 0:
        old = c1;
        c1  = value;
        return old;
      case 1:
        old = c2;
        c2  = value;
        return old;
      case 2:
        old = c3;
        c3  = value;
        return old;
      default:
        throw new IndexOutOfBoundsException("Index: "+index+", Size: 3");
      }
    }

    public void addAllTo(Collection<Object> c) {
      c.add(c1);
      c.add(c2);
      c.add(c3);
    }

  }

  // =======================================================================

  /** A generic node with four children. */
  static class Fixed4 extends Fixed {

    Object c1;
    Object c2;
    Object c3;
    Object c4;

    Fixed4(String name, Object c1, Object c2, Object c3, Object c4) {
      super(name);
      this.c1 = c1;
      this.c2 = c2;
      this.c3 = c3;
      this.c4 = c4;
    }

    Fixed4(Fixed4 node) {
      this(node.name, node.c1, node.c2, node.c3, node.c4);
    }

    public int size() {
      return 4;
    }

    public Object get(int index) {
      switch (index) {
      case 0:
        return c1;
      case 1:
        return c2;
      case 2:
        return c3;
      case 3:
        return c4;
      default:
        throw new IndexOutOfBoundsException("Index: "+index+", Size: 4");
      }
    }

    public Object set(int index, Object value) {
      Object old;

      switch (index) {
      case 0:
        old = c1;
        c1  = value;
        return old;
      case 1:
        old = c2;
        c2  = value;
        return old;
      case 2:
        old = c3;
        c3  = value;
        return old;
      case 3:
        old = c4;
        c4  = value;
        return old;
      default:
        throw new IndexOutOfBoundsException("Index: "+index+", Size: 4");
      }
    }

    public void addAllTo(Collection<Object> c) {
      c.add(c1);
      c.add(c2);
      c.add(c3);
      c.add(c4);
    }

  }

  // =======================================================================

  /** A generic node with five children. */
  static class Fixed5 extends Fixed {

    Object c1;
    Object c2;
    Object c3;
    Object c4;
    Object c5;

    Fixed5(String name, Object c1, Object c2, Object c3, Object c4, Object c5) {
      super(name);
      this.c1 = c1;
      this.c2 = c2;
      this.c3 = c3;
      this.c4 = c4;
      this.c5 = c5;
    }

    Fixed5(Fixed5 node) {
      this(node.name, node.c1, node.c2, node.c3, node.c4, node.c5);
    }

    public int size() {
      return 5;
    }

    public Object get(int index) {
      switch (index) {
      case 0:
        return c1;
      case 1:
        return c2;
      case 2:
        return c3;
      case 3:
        return c4;
      case 4:
        return c5;
      default:
        throw new IndexOutOfBoundsException("Index: "+index+", Size: 5");
      }
    }

    public Object set(int index, Object value) {
      Object old;

      switch (index) {
      case 0:
        old = c1;
        c1  = value;
        return old;
      case 1:
        old = c2;
        c2  = value;
        return old;
      case 2:
        old = c3;
        c3  = value;
        return old;
      case 3:
        old = c4;
        c4  = value;
        return old;
      case 4:
        old = c5;
        c5  = value;
        return old;
      default:
        throw new IndexOutOfBoundsException("Index: "+index+", Size: 5");
      }
    }

    public void addAllTo(Collection<Object> c) {
      c.add(c1);
      c.add(c2);
      c.add(c3);
      c.add(c4);
      c.add(c5);
    }

  }

  // =======================================================================

  /** A generic node with six children. */
  static class Fixed6 extends Fixed {

    Object c1;
    Object c2;
    Object c3;
    Object c4;
    Object c5;
    Object c6;

    Fixed6(String name, Object c1, Object c2, Object c3, Object c4,
           Object c5, Object c6) {
      super(name);
      this.c1 = c1;
      this.c2 = c2;
      this.c3 = c3;
      this.c4 = c4;
      this.c5 = c5;
      this.c6 = c6;
    }

    Fixed6(Fixed6 node) {
      this(node.name, node.c1, node.c2, node.c3, node.c4, node.c5, node.c6);
    }

    public int size() {
      return 6;
    }

    public Object get(int index) {
      switch (index) {
      case 0:
        return c1;
      case 1:
        return c2;
      case 2:
        return c3;
      case 3:
        return c4;
      case 4:
        return c5;
      case 5:
        return c6;
      default:
        throw new IndexOutOfBoundsException("Index: "+index+", Size: 6");
      }
    }

    public Object set(int index, Object value) {
      Object old;

      switch (index) {
      case 0:
        old = c1;
        c1  = value;
        return old;
      case 1:
        old = c2;
        c2  = value;
        return old;
      case 2:
        old = c3;
        c3  = value;
        return old;
      case 3:
        old = c4;
        c4  = value;
        return old;
      case 4:
        old = c5;
        c5  = value;
        return old;
      case 5:
        old = c6;
        c6  = value;
        return old;
      default:
        throw new IndexOutOfBoundsException("Index: "+index+", Size: 6");
      }
    }

    public void addAllTo(Collection<Object> c) {
      c.add(c1);
      c.add(c2);
      c.add(c3);
      c.add(c4);
      c.add(c5);
      c.add(c6);
    }

  }

  // =======================================================================

  /** A generic node with seven children. */
  static class Fixed7 extends Fixed {

    Object c1;
    Object c2;
    Object c3;
    Object c4;
    Object c5;
    Object c6;
    Object c7;

    Fixed7(String name, Object c1, Object c2, Object c3, Object c4,
           Object c5, Object c6, Object c7) {
      super(name);
      this.c1 = c1;
      this.c2 = c2;
      this.c3 = c3;
      this.c4 = c4;
      this.c5 = c5;
      this.c6 = c6;
      this.c7 = c7;
    }

    Fixed7(Fixed7 node) {
      this(node.name, node.c1, node.c2, node.c3, node.c4, node.c5, node.c6,
           node.c7);
    }

    public int size() {
      return 7;
    }

    public Object get(int index) {
      switch (index) {
      case 0:
        return c1;
      case 1:
        return c2;
      case 2:
        return c3;
      case 3:
        return c4;
      case 4:
        return c5;
      case 5:
        return c6;
      case 6:
        return c7;
      default:
        throw new IndexOutOfBoundsException("Index: "+index+", Size: 7");
      }
    }

    public Object set(int index, Object value) {
      Object old;

      switch (index) {
      case 0:
        old = c1;
        c1  = value;
        return old;
      case 1:
        old = c2;
        c2  = value;
        return old;
      case 2:
        old = c3;
        c3  = value;
        return old;
      case 3:
        old = c4;
        c4  = value;
        return old;
      case 4:
        old = c5;
        c5  = value;
        return old;
      case 5:
        old = c6;
        c6  = value;
        return old;
      case 6:
        old = c7;
        c7  = value;
        return old;
      default:
        throw new IndexOutOfBoundsException("Index: "+index+", Size: 7");
      }
    }

    public void addAllTo(Collection<Object> c) {
      c.add(c1);
      c.add(c2);
      c.add(c3);
      c.add(c4);
      c.add(c5);
      c.add(c6);
      c.add(c7);
    }

  }

  // =======================================================================

  /** A generic node with eight children. */
  static class Fixed8 extends Fixed {

    Object c1;
    Object c2;
    Object c3;
    Object c4;
    Object c5;
    Object c6;
    Object c7;
    Object c8;

    Fixed8(String name, Object c1, Object c2, Object c3, Object c4,
           Object c5, Object c6, Object c7, Object c8) {
      super(name);
      this.c1 = c1;
      this.c2 = c2;
      this.c3 = c3;
      this.c4 = c4;
      this.c5 = c5;
      this.c6 = c6;
      this.c7 = c7;
      this.c8 = c8;
    }

    Fixed8(Fixed8 node) {
      this(node.name, node.c1, node.c2, node.c3, node.c4, node.c5, node.c6,
           node.c7, node.c8);
    }

    public int size() {
      return 8;
    }

    public Object get(int index) {
      switch (index) {
      case 0:
        return c1;
      case 1:
        return c2;
      case 2:
        return c3;
      case 3:
        return c4;
      case 4:
        return c5;
      case 5:
        return c6;
      case 6:
        return c7;
      case 7:
        return c8;
      default:
        throw new IndexOutOfBoundsException("Index: "+index+", Size: 8");
      }
    }

    public Object set(int index, Object value) {
      Object old;

      switch (index) {
      case 0:
        old = c1;
        c1  = value;
        return old;
      case 1:
        old = c2;
        c2  = value;
        return old;
      case 2:
        old = c3;
        c3  = value;
        return old;
      case 3:
        old = c4;
        c4  = value;
        return old;
      case 4:
        old = c5;
        c5  = value;
        return old;
      case 5:
        old = c6;
        c6  = value;
        return old;
      case 6:
        old = c7;
        c7  = value;
        return old;
      case 7:
        old = c8;
        c8  = value;
        return old;
      default:
        throw new IndexOutOfBoundsException("Index: "+index+", Size: 8");
      }
    }

    public void addAllTo(Collection<Object> c) {
      c.add(c1);
      c.add(c2);
      c.add(c3);
      c.add(c4);
      c.add(c5);
      c.add(c6);
      c.add(c7);
      c.add(c8);
    }

  }

  // =======================================================================

  /** A generic node with a variable number of children. */
  static class Variable extends GNode {

    /** The list of children. */
    private ArrayList<Object> children;

    /** Create a new variable node. */
    Variable(String name) {
      super(name);
      children = new ArrayList<Object>();
    }

    /** Create a new variable node. */
    Variable(String name, int capacity) {
      super(name);
      children = new ArrayList<Object>(capacity);
    }

    /** Create a new variable node. */
    Variable(String name, ArrayList<Object> children) {
      super(name);
      this.children = children;
    }

    /** Create a new variable node. */
    Variable(Variable node) {
      super(node.name);
      children = new ArrayList<Object>(node.children);
    }

    public boolean hasVariable() {
      return true;
    }

    public Node add(Object o) {
      children.add(o);
      return this;
    }

    public Node add(int index, Object o) {
      children.add(index, o);
      return this;
    }

    public Node addAll(Pair<?> p) {
      p.addTo(children);
      return this;
    }

    public Node addAll(int index, Pair<?> p) {
      p.addTo(children.subList(0, index));
      return this;
    }

    public Node addAll(Collection<?> c) {
      children.addAll(c);
      return this;
    }

    public Node addAll(int index, Collection<?> c) {
      children.addAll(index, c);
      return this;
    }

    public void addAllTo(Collection<Object> c) {
      c.addAll(children);
    }

    public Iterator<Object> iterator() {
      return children.iterator();
    }

    public int size() {
      return children.size();
    }

    public Object get(int index) {
      return children.get(index);
    }

    public Object set(int index, Object value) {
      return children.set(index, value);
    }

    public Object remove(int index) {
      return children.remove(index);
    }

  }

  // =======================================================================

  /**
   * The maximum number of children for generic nodes that are
   * optimized to hold a fixed number of children.
   */
  public static final int MAX_FIXED = 8;

  /** The name. */
  final String name;

  /** Create a new generic node with the specified name. */
  GNode(String name) {
    this.name = name;
  }

  // =======================================================================

  /**
   * Get this generic node's hash code.
   *
   * @return This node's hash code.
   */
  public int hashCode() {
    int hash = name.hashCode();
    int size = size();
    for (int i=0; i<size; i++) {
      Object child = get(i);
      hash = (37 * hash) + (null==child? 0 : child.hashCode());
    }
    return hash;
  }

  /**
   * Determine whether this generic node equals the specified object.
   * This node equals the object, if both are generic nodes with the
   * same names and the same number of equal children.
   *
   * @param o The object to compare to.
   * @return <code>true</code> if this generic node equals the object.
   */
  public boolean equals(Object o) {
    if (this == o) return true;
    if (! (o instanceof GNode)) return false;
    GNode other = (GNode)o;
    if (! name.equals(other.name)) return false;
    int size = size();
    if (other.size() != size) return false;
    for (int i=0; i<size; i++) {
      Object child1 = get(i);
      Object child2 = other.get(i);
      if (! (null==child1? null==child2 : child1.equals(child2))) return false;
    }
    return true;
  }

  // =======================================================================

  /**
   * Determine whether this node is generic.
   *
   * @return <code>true</code>.
   */
  public final boolean isGeneric() {
    return true;
  }

  public final boolean hasTraversal() {
    return true;
  }

  public final String getName() {
    return name;
  }

  public final boolean hasName(String name) {
    return this.name.equals(name);
  }

  // =======================================================================

  /**
   * Create a new generic node with the specified name.  The new node
   * supports a variable number of children and has a default
   * capacity.
   *
   * @param name The name.
   * @return The corresponding generic node.
   */
  public static GNode create(String name) {
    return new Variable(name);
  }

  /**
   * Create a new generic node with the specified name.  The new node
   * supports a variable number of children and has the specified
   * capacity.
   *
   * @param name The name.
   * @param capacity The initial capacity.
   * @return The corresponding generic node.
   * @throws IllegalArgumentException
   *   Signals that the capacity is negative.
   */
  public static GNode create(String name, int capacity) {
    return new Variable(name, capacity);
  }

  /**
   * Create a new generic node with the specified name.  Invoking this
   * method with a true variable flag is equivalent to invoking {@link
   * #create(String)}.  Invoking this method with a false variabel
   * flag results in a generic node with no children.
   *
   * @param name The name.
   * @param variable Flag for whether the new node supports a variable
   *   number of children.
   * @return The corresponding generic node.
   */
  public static GNode create(String name, boolean variable) {
    if (variable) {
      return new Variable(name);
    } else {
      return new Fixed0(name);
    }
  }

  /**
   * Create a new generic node with the specified name and child.  The
   * new node does not support a variable number of children.
   *
   * @param name The name.
   * @param child The child.
   * @return The corresponding generic node.
   */
  public static GNode create(String name, Object child) {
    return new Fixed1(name, child);
  }

  /**
   * Create a new generic node with the specified name and children.
   * The new node does not support a variable number of children.
   *
   * @param name The name.
   * @param c1 The first child.
   * @param c2 The second child.
   * @return The corresponding generic node.
   */
  public static GNode create(String name, Object c1, Object c2) {
    return new Fixed2(name, c1, c2);
  }

  /**
   * Create a new generic node with the specified name and children.
   * The new node does not support a variable number of children.
   *
   * @param name The name.
   * @param c1 The first child.
   * @param c2 The second child.
   * @param c3 The third child.
   * @return The corresponding generic node.
   */
  public static GNode create(String name, Object c1, Object c2, Object c3) {
    return new Fixed3(name, c1, c2, c3);
  }

  /**
   * Create a new generic node with the specified name and children.
   * The new node does not support a variable number of children.
   *
   * @param name The name.
   * @param c1 The first child.
   * @param c2 The second child.
   * @param c3 The third child.
   * @param c4 The fourth child.
   * @return The corresponding generic node.
   */
  public static GNode create(String name, Object c1, Object c2, Object c3,
                             Object c4) {
    return new Fixed4(name, c1, c2, c3, c4);
  }

  /**
   * Create a new generic node with the specified name and children.
   * The new node does not support a variable number of children.
   *
   * @param name The name.
   * @param c1 The first child.
   * @param c2 The second child.
   * @param c3 The third child.
   * @param c4 The fourth child.
   * @param c5 The fifth child.
   * @return The corresponding generic node.
   */
  public static GNode create(String name, Object c1, Object c2, Object c3,
                             Object c4, Object c5) {
    return new Fixed5(name, c1, c2, c3, c4, c5);
  }

  /**
   * Create a new generic node with the specified name and children.
   * The new node does not support a variable number of children.
   *
   * @param name The name.
   * @param c1 The first child.
   * @param c2 The second child.
   * @param c3 The third child.
   * @param c4 The fourth child.
   * @param c5 The fifth child.
   * @param c6 The sixth child.
   * @return The corresponding generic node.
   */
  public static GNode create(String name, Object c1, Object c2, Object c3,
                             Object c4, Object c5, Object c6) {
    return new Fixed6(name, c1, c2, c3, c4, c5, c6);
  }

  /**
   * Create a new generic node with the specified name and children.
   * The new node does not support a variable number of children.
   *
   * @param name The name.
   * @param c1 The first child.
   * @param c2 The second child.
   * @param c3 The third child.
   * @param c4 The fourth child.
   * @param c5 The fifth child.
   * @param c6 The sixth child.
   * @param c7 The seventh child.
   * @return The corresponding generic node.
   */
  public static GNode create(String name, Object c1, Object c2, Object c3,
                             Object c4, Object c5, Object c6, Object c7) {
    return new Fixed7(name, c1, c2, c3, c4, c5, c6, c7);
  }

  /**
   * Create a new generic node with the specified name and children.
   * The new node does not support a variable number of children.
   *
   * @param name The name.
   * @param c1 The first child.
   * @param c2 The second child.
   * @param c3 The third child.
   * @param c4 The fourth child.
   * @param c5 The fifth child.
   * @param c6 The sixth child.
   * @param c7 The seventh child.
   * @param c8 The eigth child.
   * @return The corresponding generic node.
   */
  public static GNode create(String name, Object c1, Object c2, Object c3,
                             Object c4, Object c5, Object c6, Object c7,
                             Object c8) {
    return new Fixed8(name, c1, c2, c3, c4, c5, c6, c7, c8);
  }

  /**
   * Create a new generic node with the list's nodes as its children.
   * If possible, this method returns a fixed size node.
   *
   * @param name The name.
   * @param p The list of children.
   * @return The corresponding generic node.
   */
  public static GNode createFromPair(String name, Pair p) {
    final int size = p.size();
    Object    c1   = null;
    Object    c2   = null;
    Object    c3   = null;
    Object    c4   = null;
    Object    c5   = null;
    Object    c6   = null;
    Object    c7   = null;
    Object    c8   = null;

    switch (size) {
    case 8:
      c1 = p.head();
      p  = p.tail();
      // Fall through.
    case 7:
      c2 = p.head();
      p  = p.tail();
      // Fall through.
    case 6:
      c3 = p.head();
      p  = p.tail();
      // Fall through.
    case 5:
      c4 = p.head();
      p  = p.tail();
      // Fall through.
    case 4:
      c5 = p.head();
      p  = p.tail();
      // Fall through.
    case 3:
      c6 = p.head();
      p  = p.tail();
      // Fall through.
    case 2:
      c7 = p.head();
      p  = p.tail();
      // Fall through.
    case 1:
      c8 = p.head();
      // Fall through.
    case 0:
      // Done.
      break;
    default:
      Variable result = new Variable(name, size);
      result.addAll(p);
      return result;
    }

    switch (size) {
    case 8:
      return new Fixed8(name, c1, c2, c3, c4, c5, c6, c7, c8);
    case 7:
      return new Fixed7(name, c2, c3, c4, c5, c6, c7, c8);
    case 6:
      return new Fixed6(name, c3, c4, c5, c6, c7, c8);
    case 5:
      return new Fixed5(name, c4, c5, c6, c7, c8);
    case 4:
      return new Fixed4(name, c5, c6, c7, c8);
    case 3:
      return new Fixed3(name, c6, c7, c8);
    case 2:
      return new Fixed2(name, c7, c8);
    case 1:
      return new Fixed1(name, c8);
    case 0:
      return new Fixed0(name);
    default:
      throw new AssertionError("Internal error");
    }
  }

  /**
   * Create a new generic node with the specified children.  If
   * possible, this method returns a fixed size node.
   *
   * @param name The name.
   * @param base The first child.
   * @param rest The rest of the children.
   * @return The corresponding generic node.
   */
  public static GNode createFromPair(String name, Object base, Pair rest) {
    final int size = rest.size();
    Object    c2   = null;
    Object    c3   = null;
    Object    c4   = null;
    Object    c5   = null;
    Object    c6   = null;
    Object    c7   = null;
    Object    c8   = null;

    switch (size) {
    case 7:
      c2   = rest.head();
      rest = rest.tail();
      // Fall through.
    case 6:
      c3   = rest.head();
      rest = rest.tail();
      // Fall through.
    case 5:
      c4   = rest.head();
      rest = rest.tail();
      // Fall through.
    case 4:
      c5   = rest.head();
      rest = rest.tail();
      // Fall through.
    case 3:
      c6   = rest.head();
      rest = rest.tail();
      // Fall through.
    case 2:
      c7   = rest.head();
      rest = rest.tail();
      // Fall through.
    case 1:
      c8   = rest.head();
      rest = rest.tail();
      // Fall through.
    case 0:
      // Done.
      break;
    default:
      Variable result = new Variable(name, size + 1);
      result.add(base);
      result.addAll(rest);
      return result;
    }

    switch (size) {
    case 7:
      return new Fixed8(name, base, c2, c3, c4, c5, c6, c7, c8);
    case 6:
      return new Fixed7(name, base, c3, c4, c5, c6, c7, c8);
    case 5:
      return new Fixed6(name, base, c4, c5, c6, c7, c8);
    case 4:
      return new Fixed5(name, base, c5, c6, c7, c8);
    case 3:
      return new Fixed4(name, base, c6, c7, c8);
    case 2:
      return new Fixed3(name, base, c7, c8);
    case 1:
      return new Fixed2(name, base, c8);
    case 0:
      return new Fixed1(name, base);
    default:
      throw new AssertionError("Internal error");
    }
  }

  /**
   * Create a new generic node that is a (shallow) copy of the
   * specified node.
   *
   * @param node The node to copy.
   * @return The copy.
   */
  public static GNode create(GNode node) {
    if (node instanceof Variable) {
      return new Variable((Variable)node);
    } else {
      switch (node.size()) {
      case 0:
        return new Fixed0((Fixed0)node);
      case 1:
        return new Fixed1((Fixed1)node);
      case 2:
        return new Fixed2((Fixed2)node);
      case 3:
        return new Fixed3((Fixed3)node);
      case 4:
        return new Fixed4((Fixed4)node);
      case 5:
        return new Fixed5((Fixed5)node);
      case 6:
        return new Fixed6((Fixed6)node);
      case 7:
        return new Fixed7((Fixed7)node);
      case 8:
        return new Fixed8((Fixed8)node);
      default:
        throw new AssertionError("Internal error");
      }
    }
  }

  // =======================================================================

  /**
   * Ensure that the specified node supports a variable number of
   * children.
   *
   * @param node The generic node.
   * @return A shallow copy of the specified node if it does not
   *   support a variable number of children; otherwise, the specified
   *   node.
   */
  public static GNode ensureVariable(GNode node) {
    if (node instanceof Variable) {
      return node;
    } else {
      ArrayList<Object> children = new ArrayList<Object>(node.size());
      node.addAllTo(children);
      return new Variable(node.name, children);
    }
  }

  /**
   * Test whether the specified object is a generic node, possibly
   * wrapped in annotations.
   *
   * @param o The object.
   * @return <code>true</code> if the object is a possibly annotated
   *   generic node.
   */
  public static final boolean test(Object o) {
    return ((o instanceof Node) && ((Node)o).strip().isGeneric());
  }

  /**
   * Cast the specified object to a generic node.  If the specified
   * object has any {@link Annotation annotations}, they are {@link
   * Node#strip() stripped} before returning the object as a generic
   * node.
   *
   * @see #test(Object)
   *
   * @param o The object.
   * @return The object as a stripped generic node.
   */
  public static final GNode cast(Object o) {
    Node n = (Node)o;
    return (null == n)? null : (GNode)n.strip();
  }

}
