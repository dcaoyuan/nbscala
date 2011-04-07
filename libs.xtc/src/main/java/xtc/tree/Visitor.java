/*
 * xtc - The eXTensible Compiler
 * Copyright (C) 2004-2007 Robert Grimm
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
package xtc.tree;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

import java.util.LinkedHashMap;
import java.util.Map;

import xtc.util.Pair;

/**
 * The superclass of all node visitors.
 *
 * <p />Nodes may contain children that are lists of nodes.  To
 * simplify the processing of such lists with visitors, this class
 * defines three helper methods that apply a visitor to a list of
 * nodes:<ul>
 *
 * <li>{@link #iterate(Pair)} invokes <code>dispatch()</code> on each
 * element of the list and ignores any results.</li>
 *
 * <li>{@link #map(Pair)} invokes <code>dispatch()</code> on each
 * element of the list while also collecting the results in a new
 * list.</li>
 *
 * <li>{@link #mapInPlace(Pair)} invokes <code>dispatch()</code> on
 * each element of the list while also updating the list with the
 * results.</li>
 *
 * </ul>
 *
 * Additonally, <code>Node</code> provides helper methods to
 * dynamically test and cast lists of nodes through {@link
 * Node#isList(Object)} and {@link Node#toList(Object)} respectively.
 *
 * @author Robert Grimm
 * @version $Revision: 1.30 $
 */
public abstract class Visitor {

  /** Key for the method lookup cache. */
  static final class CacheKey {

    /** The visitor. */
    public Visitor visitor;

    /** The object identifying the node. */
    public Object  node;

    /**
     * Create a new cache key.
     *
     * @param visitor The visitor.
     * @param node The object identifying the node.
     */
    public CacheKey(Visitor visitor, Object node) {
      this.visitor = visitor;
      this.node    = node;
    }

    public int hashCode() {
      return (37 * visitor.hashCode()) + node.hashCode();
    }

    public boolean equals(Object o) {
      if (! (o instanceof CacheKey)) return false;
      CacheKey other = (CacheKey)o;
      if (! visitor.equals(other.visitor)) return false;
      return node.equals(other.node);
    }

  }

  // ========================================================================

  /** The size of the method lookup cache. */
  private static final int CACHE_SIZE = 300;

  /** The capacity of the method lookup cache. */
  private static final int CACHE_CAPACITY = 400;

  /** The load factor of the method lookup cache. */
  private static final float CACHE_LOAD = (float)0.75;

  /** The method lookup cache. */
  private static final LinkedHashMap<CacheKey, Method> cache;

  /** The pre-allocated cache key for looking up methods. */
  private static final CacheKey key;

  /** The pre-allocated array for passing the argument to invoke(). */
  private static final Object[] arguments;

  /**
   * The pre-allocated array for passing the type argument to
   * getMethod().
   */
  private static final Class[] types;

  static {
    cache     =
      new LinkedHashMap<CacheKey, Method>(CACHE_CAPACITY, CACHE_LOAD, true) {
        protected boolean removeEldestEntry(Map.Entry e) {
          return size() > CACHE_SIZE;
        }
      };
    key       = new CacheKey(null, null);
    arguments = new Object[] { null };
    types     = new Class[]  { null };
  }

  // ========================================================================

  /** Create a new visitor. */
  public Visitor() { /* Nothing to do. */ }

  /**
   * Get a hashcode for this visitor.
   *
   * @return The identity hashcode.
   */
  public final int hashCode() {
    return super.hashCode();
  }

  /**
   * Determine whether this visitor equals the specified object.
   *
   * @param o The object to compare to.
   * @return <code>true</code> if the specified object is this visitor.
   */
  public final boolean equals(Object o) {
    return this == o;
  }

  /**
   * Visit the specified annotation.  This method simply applies this
   * visitor on the node referenced by the annotation, thus ignoring
   * the annotation.
   *
   * @param a The annotation.
   * @return The result of applying this visitor on the referenced node.
   */
  public Object visit(Annotation a) {
    return dispatch(a.node);
  }

  // ========================================================================

  /**
   * Dispatch this visitor on the specified node.  This method
   * determines the closest matching <code>visit()</code> method,
   * invokes it on the specified node, and returns the result.  If the
   * specified node is <code>null</code> or the selected method
   * returns <code>void</code>, this method returns <code>null</code>.
   *
   * @see #unableToVisit(Node)
   *
   * @param n The node.
   * @return The result of dispatching this visitor on the specified
   *   node.
   * @throws VisitorException Signals that no matching
   *   <code>visit()</code> method could be found.
   * @throws VisitingException Signals an exceptional condition while
   *   applying the specified visitor on this node.
   */
  public final Object dispatch(final Node n) {
    // Get the trivial case out of the way.
    if (null == n) return null;

    // Check the method lookup cache.
    Method method;
    key.visitor = this;
    if (n.isGeneric()) {
      key.node = n.getName();
    } else {
      key.node = n.getClass();
    }
    method = cache.get(key);

    if (null == method) {
      // Determine the correct cache value and cache it.
      method = findMethod(n);
      cache.put(new CacheKey(this, key.node), method);
    }

    // Set up the argument.
    arguments[0] = n;

    // Invoke the method.
    try {
      return method.invoke(this, arguments);
    } catch (IllegalAccessException x) {
      throw new VisitorException("Unable to invoke " + method + " on " +
                                 arguments[0]);
    } catch (IllegalArgumentException x) {
      throw new VisitorException("Internal error while visiting node " +
                                 n + " with visitor " + this);
    } catch (InvocationTargetException x) {
      Throwable cause = x.getCause();

      // Rethrow visiting and visitor exceptions.
      if (cause instanceof VisitingException) {
        throw (VisitingException)cause;
      } else if (cause instanceof VisitorException) {
        throw (VisitorException)cause;
      }

      // Throw the appropriate visiting exception.
      throw new VisitingException("Error visiting node " + n + " with " +
                                  "visitor " + this, cause);
    } catch (NullPointerException x) {
      throw new VisitorException("Internal error while visiting node " +
                                 n + " with visitor " + this);
    }
  }

  /**
   * Determine the method for visiting the specified node with this
   * visitor.
   *
   * @param n The node.
   * @return The corresponding method.
   */
  private Method findMethod(final Node n) {
    Class  visitorT = getClass();
    Method method   = null;

    if (n.isGeneric()) {
      // Look for visit<n.getName()>(GNode).
      types[0] = GNode.class;
      try {
        method = visitorT.getMethod("visit" + n.getName(), types);
      } catch (NoSuchMethodException x) {

        // Look for visit(GNode).
        try {
          method = visitorT.getMethod("visit", types);
        } catch (NoSuchMethodException xx) {

          // Look for visit(Node).
          types[0] = Node.class;
          try {
            method = visitorT.getMethod("visit", types);
          } catch (NoSuchMethodException xxx) {
            // Ignore.
          }
        }
      }

    } else {
      // Look for visit(<type>), starting with Type = n.getClass().
      method = findMethod(visitorT, "visit", n.getClass());
    }

    // Look for unableToVisit(Node).
    if (null == method) {
      types[0] = Node.class;
      try {
        method = visitorT.getMethod("unableToVisit", types);
      } catch (NoSuchMethodException x) {
        throw new AssertionError("Unable to find unableToVisit(Node)");
      }
    }

    // Override access control and return method.
    method.setAccessible(true);
    return method;
  }

  /**
   * Find a method for the specified class with the specified name and
   * parameter type.  This method, in addition to looking for a method
   * with the specified parameter type, also tries all interfaces
   * implemented by the parameter type, then the superclass, then the
   * interfaces implemented by the superclass, and so on.
   *
   * @param k The class.
   * @param name The method name.
   * @param paramT The parameter type.
   * @return The method or <code>null</code> if no such method exists.
   */
  private static Method findMethod(Class k, String name, Class paramT) {
    Method method = null;

    do {
      types[0] = paramT;
      try {
        method = k.getMethod(name, types);
      } catch (NoSuchMethodException x) {
        // Try the interfaces implemented by paramT.
        Class[] interfaces = paramT.getInterfaces();
        for (int i=0; i<interfaces.length; i++) {
          types[0] = interfaces[i];
          try {
            method = k.getMethod(name, types);
            break;
          } catch (NoSuchMethodException xx) {
            // Ignore.
          }
        }
        
        // Move on to the superclass.
        paramT = paramT.getSuperclass();
      }
    } while ((null == method) && (Object.class != paramT));

    return method;
  }

  /**
   * Signal that this visitor has no <code>visit()</code> method for
   * the specified node.  The default implementation simply raises a
   * visitor exception.
   *
   * @param node The node.
   * @return The result of processing the node.
   * @throws VisitorException Signals that no matching
   *   <code>visit()</code> method could be found.
   */
  public Object unableToVisit(Node node) {
    if (node.isGeneric()) {
      throw new VisitorException("No method to visit generic node " +
                                 node.getName() + " with visitor " + this);
    } else {
      throw new VisitorException("No method to visit node type " +
                                 node.getClass() + " with visitor " + this);
    }
  }

  // ========================================================================

  /**
   * Iterate this visitor over the specified list.
   *
   * @param list The list.
   */
  public void iterate(Pair<? extends Node> list) {
    while (Pair.EMPTY != list) {
      dispatch(list.head());
      list = list.tail();
    }
  }

  /**
   * Map this visitor over the specified list.
   *
   * @param list The list.
   * @return The list of results.
   */
  public <T> Pair<T> map(Pair<? extends Node> list) {
    if (Pair.EMPTY == list) return Pair.empty();

    final @SuppressWarnings("unchecked")
    T v1 = (T)dispatch(list.head());

    Pair<T> result = new Pair<T>(v1);
    Pair<T> cursor = result;

    while (Pair.EMPTY != list.tail()) {
      list = list.tail();

      final @SuppressWarnings("unchecked")
      T v2 = (T)dispatch(list.head());

      cursor.setTail(new Pair<T>(v2));
      cursor = cursor.tail();
    }

    return result;
  }

  /**
   * Map this visitor over the specified list while also updating the
   * list.
   *
   * @param list The list.
   * @return The updated list.
   */
  public <T extends Node> Pair<T> mapInPlace(Pair<T> list) {
    Pair<T> p = list;

    while (Pair.EMPTY != p) {
      final @SuppressWarnings("unchecked")
      T v = (T)dispatch(p.head());

      p.setHead(v);
      p = p.tail();
    }

    return list;
  }

}
