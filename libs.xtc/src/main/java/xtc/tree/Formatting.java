/*
 * xtc - The eXTensible Compiler
 * Copyright (C) 2007 Robert Grimm
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

import java.util.Collection;
import java.util.ArrayList;

import xtc.util.Pair;

/**
 * An annotation capturing source code formatting.
 *
 * @author Robert Grimm
 * @version $Revision: 1.2 $
 */
public abstract class Formatting extends Annotation {

  /** A formatting annotation with one node before the annotated node. */
  static class Before1 extends Formatting {

    Object b1;

    Before1(Object b1, Node node) {
      super(node);
      this.b1 = b1;
    }

    public int size() {
      return 2;
    }

    public Object get(int index) {
      switch (index) {
      case 0:
        return b1;
      case 1:
        return node;
      default:
        throw new IndexOutOfBoundsException("Index: "+index+", Size: 2");
      }
    }

    public Object set(int index, Object value) {
      Object old;

      switch (index) {
      case 0:
        old  = b1;
        b1   = value;
        return old;
      case 1:
        old  = node;
        node = (Node)value;
        return old;
      default:
        throw new IndexOutOfBoundsException("Index: "+index+", Size: 2");
      }
    }

  }

  // =======================================================================

  /** A formatting annotation with one node after the annotated node. */
  static class After1 extends Formatting {

    Object a1;

    After1(Node node, Object a1) {
      super(node);
      this.a1 = a1;
    }

    public int size() {
      return 2;
    }

    public Object get(int index) {
      switch (index) {
      case 0:
        return node;
      case 1:
        return a1;
      default:
        throw new IndexOutOfBoundsException("Index: "+index+", Size: 2");
      }
    }

    public Object set(int index, Object value) {
      Object old;

      switch (index) {
      case 0:
        old  = node;
        node = (Node)value;
        return old;
      case 1:
        old  = a1;
        a1   = value;
        return old;
      default:
        throw new IndexOutOfBoundsException("Index: "+index+", Size: 2");
      }
    }

  }

  // =======================================================================

  /**
   * A formatting annotation with one node before and after the
   * annotated node.
   */
  static class Round1 extends Formatting {

    Object b1;
    Object a1;

    Round1(Object b1, Node node, Object a1) {
      super(node);
      this.b1 = b1;
      this.a1 = a1;
    }

    public int size() {
      return 3;
    }

    public Object get(int index) {
      switch (index) {
      case 0:
        return b1;
      case 1:
        return node;
      case 2:
        return a1;
      default:
        throw new IndexOutOfBoundsException("Index: "+index+", Size: 3");
      }
    }

    public Object set(int index, Object value) {
      Object old;

      switch (index) {
      case 0:
        old  = b1;
        b1   = value;
        return old;
      case 1:
        old  = node;
        node = (Node)value;
        return old;
      case 2:
        old  = a1;
        a1   = value;
        return old;
      default:
        throw new IndexOutOfBoundsException("Index: "+index+", Size: 3");
      }
    }

  }

  // =======================================================================

  /** A generic formatting annotation. */
  static class RoundN extends Formatting {

    private boolean hasNode;
    private ArrayList<Object> before;
    private ArrayList<Object> after;

    public RoundN() {
      hasNode = false;
      before  = new ArrayList<Object>();
      after   = new ArrayList<Object>();
    }

    public boolean hasVariable() {
      return true;
    }

    public void setNode(Node node) {
      this.node = node;
      hasNode   = true;
    }

    public int size() {
      return hasNode ? before.size() + 1 + after.size() : before.size();
    }

    public Object get(int index) {
      if (! hasNode) {
        return before.get(index);
      } else {
        final int size1 = before.size();
        final int size2 = after.size();
        final int total = size1 + 1 + size2;
        
        if (0 <= index) {
          if (index < size1) {
            return before.get(index);
          } else if (index == size1) {
            return node;
          } else if (index < total) {
            return after.get(index - size1 - 1);
          }
        }

        throw new IndexOutOfBoundsException("Index: "+index+", Size: "+total);
      }
    }

    public Object set(int index, Object value) {
      if (! hasNode) {
        return before.set(index, value);
      } else {
        final int size1 = before.size();
        final int size2 = after.size();
        final int total = size1 + 1 + size2;
        
        if (0 <= index) {
          if (index < size1) {
            return before.set(index, value);
          } else if (index == size1) {
            Node old = node;
            node     = (Node)value;
            return old;
          } else if (index < total) {
            return after.set(index - size1 - 1, value);
          }
        }

        throw new IndexOutOfBoundsException("Index: "+index+", Size: "+total);
      }
    }

    public Node add(Object o) {
      if (! hasNode) {
        before.add(o);
      } else {
        after.add(o);
      }
      return this;
    }

    public Node addNode(Node node) {
      if (hasNode) {
        throw new IllegalStateException("Already has annotated node");
      }

      this.node = node;
      hasNode   = true;
      return this;
    }

    public Node add(int index, Object o) {
      if (! hasNode) {
        before.add(index, o);
        return this;

      } else {
        final int size1 = before.size();
        final int size2 = after.size();
        final int total = size1 + 1 + size2;

        if (0 <= index) {
          if (index < size1) {
            before.add(index, o);
            return this;
          } else if (index == size1) {
            throw new IllegalArgumentException("Can't add to annotated node");
          } else if (index < total) {
            after.add(index - size1 - 1, o);
            return this;
          }
        }

        throw new IndexOutOfBoundsException("Index: "+index+", Size: "+total);
      }
    }

    public Node addAll(Pair<?> p) {
      if (! hasNode) {
        p.addTo(before);
      } else {
        p.addTo(after);
      }
      return this;
    }

    public Node addAll(Collection<?> c) {
      if (! hasNode) {
        before.addAll(c);
      } else {
        after.addAll(c);
      }
      return this;
    }

    public Object remove(int index) {
      if (! hasNode) {
        return before.remove(index);

      } else {
        final int size1 = before.size();
        final int size2 = after.size();
        final int total = size1 + 1 + size2;

        if (0 <= index) {
          if (index < size1) {
            return before.remove(index);
          } else if (size1 == index) {
            throw new IllegalArgumentException("Can't remove annotated node");
          } else if (index < total) {
            return after.remove(index - size1 - 1);
          }
        }

        throw new IndexOutOfBoundsException("Index: "+index+", Size: "+total);
      }
    }

  }

  // =======================================================================

  /** Create a new, empty formatting annotation. */
  Formatting() { /* Nothing to do. */ }

  /**
   * Create a new formatting annotation for the specified node.
   *
   * @param node The node.
   */
  Formatting(Node node) {
    this.node = node;
  }

  public boolean hasTraversal() {
    return true;
  }

  // =======================================================================

  /**
   * Create a formatting annotation.  This method returns an
   * annotation supporting generic traversal.
   *
   * @param before The object before the node.
   * @param node The annotated node.
   * @return The formatting annotation.
   */
  public static Formatting before1(Object before, Node node) {
    return new Before1(before, node);
  }

  /**
   * Create a formatting annotation.  This method returns an
   * annotation supporting generic traversal.
   *
   * @param node The annotated node.
   * @param after The object after the node.
   * @return The formatting annotation.
   */
  public static Formatting after1(Node node, Object after) {
    return new After1(node, after);
  }

  /**
   * Create a formatting annotation.  This method returns an
   * annotation supporting generic traversal.
   *
   * @param before The object before the node.
   * @param node The annotated node.
   * @param after The object after the node.
   * @return The formatting annotation.
   */
  public static Formatting round1(Object before, Node node, Object after) {
    return new Round1(before, node, after);
  }

  /**
   * Create a formatting annotation.  This method returns an
   * annotation supporting generic traversal and adding/removing
   * children.  All children added before a call to {@link
   * #addNode(Node)} or {@link #setNode(Node)} are treated as nodes
   * preceding the annotated node, and all children added after such a
   * call are treated as nodes succeeding the annotated node.
   *
   * @return The formatting annotation.
   */
  public static Formatting variable() {
    return new RoundN();
  }

}
