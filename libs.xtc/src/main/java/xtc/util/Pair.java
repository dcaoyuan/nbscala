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
package xtc.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Implementation of a pair.  Pairs are used to construct
 * singly-linked lists, not unlike cons cells in Scheme.  Generally,
 * pairs should be treated as immutable data structures.  In
 * particular, a pair must not be modified if it is memoized by a
 * <i>Rats!</i>-generated parser.
 *
 * @author Robert Grimm
 * @version $Revision: 1.41 $
 */
public class Pair<T> implements Iterable<T> {

  /** The pair representing the empty list. */
  public static final Pair EMPTY = new Pair();

  /** The head referencing the value. */
  T head;

  /** The tail referencing the next pair. */
  Pair<T> tail;

  /** Create a new empty pair. */
  private Pair() {
    head = null;
    tail = null;
  }

  /**
   * Create a new pair.  The newly created pair represents a singleton
   * list.
   *
   * @param head The head.
   */
  public Pair(T head) {
    this.head = head;
    this.tail = Pair.empty();
  }

  /**
   * Create a new pair.
   *
   * @param head The head.
   * @param tail The tail.
   * @throws NullPointerException
   *    Signals that <code>tail</code> is <code>null</code>.
   */
  public Pair(T head, Pair<T> tail) {
    if (null == tail) {
      throw new NullPointerException("Null tail");
    }

    this.head = head;
    this.tail = tail;
  }

  /**
   * Get a hashcode for the list starting at this pair.
   *
   * @return A hashcode.
   */
  public int hashCode() {
    Pair pair = this;
    int  hash = 1;

    while (EMPTY != pair) {
      Object head = pair.head;

      hash = 31 * hash + (null == head ? 0 : head.hashCode());
      pair = pair.tail;
    }

    return hash;
  }

  /**
   * Determine whether the list starting at this pair equals the
   * specified object.
   *
   * @param o The object.
   * @return <code>true</code> if the list starting at this pair
   *   equals the object.
   */
  public boolean equals(Object o) {
    if (this == o) return true;
    if (! (o instanceof Pair)) return false;

    Pair p1 = this;
    Pair p2 = (Pair)o;

    while ((EMPTY != p1) && (EMPTY != p2)) {
      Object h1 = p1.head;
      Object h2 = p2.head;

      if (! (null == h1 ? null == h2 : h1.equals(h2))) return false;

      p1 = p1.tail;
      p2 = p2.tail;
    }

    return (EMPTY == p1) && (EMPTY == p2);
  }

  /**
   * Determine whether the list starting at this pair is empty.
   *
   * @return <code>true</code> if the list is empty.
   */
  public boolean isEmpty() {
    return this == EMPTY;
  }

  /**
   * Get the head.
   *
   * @return The head.
   * @throws IllegalStateException
   *    Signals that this pair represents the empty list.
   */
  public T head() {
    if (this == EMPTY) {
      throw new IllegalStateException("Empty list");
    }

    return head;
  }

  /**
   * Set the head.
   *
   * @param head The new head.
   * @return The old head.
   * @throws IllegalStateException
   *   Signals that this pair represents the empty list.
   */
  public T setHead(T head) {
    if (this == EMPTY) {
      throw new IllegalStateException("Empty list");
    }

    T old = this.head;
    this.head  = head;
    return old;
  }

  /**
   * Get the tail.
   *
   * @return The tail.
   * @throws IllegalStateException
   *    Signals that this pair represents the empty list.
   */
  public Pair<T> tail() {
    if (this == EMPTY) {
      throw new IllegalStateException("Empty list");
    }

    return tail;
  }

  /**
   * Set the tail.
   *
   * @param tail The new tail.
   * @return The old tail.
   * @throws NullPointerException
   *   Signals that <code>tail</code> is <code>null</code>.
   * @throws IllegalStateException
   *   Signals that this pair represents the empty list.
   */
  public Pair<T> setTail(Pair<T> tail) {
    if (null == tail) {
      throw new NullPointerException("Null tail");
    } else if (this == EMPTY) {
      throw new IllegalStateException("Empty list");
    }

    Pair<T> old = this.tail;
    this.tail   = tail;
    return old;
  }

  /**
   * Get the element at the specified index of the list starting at
   * this pair.  Note that this method's performance is linear to the
   * index.
   *
   * @param index The element's index.
   * @return The element.
   * @throws IndexOutOfBoundsException Signals an invalid index.
   */
  public T get(int index) {
    if (0 > index) {
      throw new IndexOutOfBoundsException("Index: "+index+", Size: " +size());
    }

    Pair<T> pair = this;
    int     size = 0;

    while (EMPTY != pair) {
      if (index == size) return pair.head;
      size++;
      pair = pair.tail;
    }

    throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
  }

  /**
   * Replace the element at the specified index of the list starting
   * at this pair.  Note that this method's performance is linear to
   * the index.
   *
   * @param index The element's index.
   * @param element The new element.
   * @return The old element.
   * @throws IndexOutOfBoundsException Signals an invalid index.
   */
  public T set(int index, T element) {
    if (0 > index) {
      throw new IndexOutOfBoundsException("Index: "+index+", Size: "+size());
    }

    Pair<T> pair = this;
    int     size = 0;

    while (EMPTY != pair) {
      if (index == size) {
        T old     = pair.head;
        pair.head = element;
        return old;
      }
      size++;
      pair = pair.tail;
    }

    throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
  }

  /**
   * Get the size of the list starting at this pair.  Note that this
   * method's performance is linear to the size of the list.
   *
   * @return The size.
   */
  public int size() {
    Pair pair = this;
    int  size = 0;

    while (pair != EMPTY) {
      size++;
      pair = pair.tail;
    }

    return size;
  }

  /**
   * Determine whether the list starting at this pair contains the
   * specified element.  If the specified element is not
   * <code>null</code>, the implementation invokes
   * <code>equals()</code> on the element.
   *
   * @param o The element.
   * @return <code>true</code> if the list starting at this pair
   *   contains the element.
   */
  public boolean contains(Object o) {
    Pair pair = this;

    while (EMPTY != pair) {
      Object head = pair.head;
      if (null == o ? null == head : o.equals(head)) return true;
      pair = pair.tail;
    }
    
    return false;
  }

  /**
   * Determine whether the list starting at this pair consists of
   * no elements.
   *
   * @return <code>true</code> if the list starting at this pair
   *   consists of no elements.
   */
  public boolean consists() {
    return EMPTY == this;
  }

  /**
   * Determine whether the list starting at this pair consists of the
   * specified object.  If the specified object is not
   * <code>null</code>, the implementation invokes
   * <code>equals()</code> on the object.
   *
   * @param o The object.
   * @return <code>true</code> if the list starting at this pair
   *   consists of the object.
   */
  public boolean consists(Object o) {
    return ((EMPTY != this) &&
            (null == o ? null == this.head : o.equals(this.head)) &&
            (EMPTY == this.tail));
  }

  /**
   * Determine whether the list starting at this pair consists of the
   * specified two objects.  If any of the specified objects is not
   * <code>null</code>, the implementation invokes
   * <code>equals()</code> on the object.
   *
   * @param o1 The first object.
   * @param o2 The second object.
   * @return <code>true</code> if the list starting at this pair
   *   consists of the two objects.
   */
  public boolean consists(Object o1, Object o2) {
    Pair pair = this;

    if (! ((EMPTY != pair) &&
           (null == o1 ? null == pair.head : o1.equals(pair.head)))) {
      return false;
    }
    pair = pair.tail;

    if (! ((EMPTY != pair) &&
           (null == o2 ? null == pair.head : o2.equals(pair.head)))) {
      return false;
    }

    return EMPTY == pair.tail;
  }

  /**
   * Determine whether the list starting at this pair consists of the
   * specified three objects.  If any of the specified objects is not
   * <code>null</code>, the implementation invokes
   * <code>equals()</code> on the object.
   *
   * @param o1 The first object.
   * @param o2 The second object.
   * @param o3 The third object.
   * @return <code>true</code> if the list starting at this pair
   *   consists of the three objects.
   */
  public boolean consists(Object o1, Object o2, Object o3) {
    Pair pair = this;

    if (! ((EMPTY != pair) &&
           (null == o1 ? null == pair.head : o1.equals(pair.head)))) {
      return false;
    }
    pair = pair.tail;

    if (! ((EMPTY != pair) &&
           (null == o2 ? null == pair.head : o2.equals(pair.head)))) {
      return false;
    }
    pair = pair.tail;

    if (! ((EMPTY != pair) &&
           (null == o3 ? null == pair.head : o3.equals(pair.head)))) {
      return false;
    }

    return EMPTY == pair.tail;
  }

  /**
   * Determine whether the list starting at this pair consists of the
   * specified four objects.  If any of the specified objects is not
   * <code>null</code>, the implementation invokes
   * <code>equals()</code> on the object.
   *
   * @param o1 The first object.
   * @param o2 The second object.
   * @param o3 The third object.
   * @param o4 The fourth object.
   * @return <code>true</code> if the list starting at this pair
   *   consists of the four objects.
   */
  public boolean consists(Object o1, Object o2, Object o3, Object o4) {
    Pair pair = this;

    if (! ((EMPTY != pair) &&
           (null == o1 ? null == pair.head : o1.equals(pair.head)))) {
      return false;
    }
    pair = pair.tail;

    if (! ((EMPTY != pair) &&
           (null == o2 ? null == pair.head : o2.equals(pair.head)))) {
      return false;
    }
    pair = pair.tail;

    if (! ((EMPTY != pair) &&
           (null == o3 ? null == pair.head : o3.equals(pair.head)))) {
      return false;
    }
    pair = pair.tail;

    if (! ((EMPTY != pair) &&
           (null == o4 ? null == pair.head : o4.equals(pair.head)))) {
      return false;
    }

    return EMPTY == pair.tail;
  }

  /**
   * Determine whether the list starting at this pair consists of the
   * specified five objects.  If any of the specified objects is not
   * <code>null</code>, the implementation invokes
   * <code>equals()</code> on the object.
   *
   * @param o1 The first object.
   * @param o2 The second object.
   * @param o3 The third object.
   * @param o4 The fourth object.
   * @param o5 The fifth object.
   * @return <code>true</code> if the list starting at this pair
   *   consists of the five objects.
   */
  public boolean consists(Object o1,Object o2,Object o3,Object o4,Object o5) {
    Pair pair = this;

    if (! ((EMPTY != pair) &&
           (null == o1 ? null == pair.head : o1.equals(pair.head)))) {
      return false;
    }
    pair = pair.tail;

    if (! ((EMPTY != pair) &&
           (null == o2 ? null == pair.head : o2.equals(pair.head)))) {
      return false;
    }
    pair = pair.tail;

    if (! ((EMPTY != pair) &&
           (null == o3 ? null == pair.head : o3.equals(pair.head)))) {
      return false;
    }
    pair = pair.tail;

    if (! ((EMPTY != pair) &&
           (null == o4 ? null == pair.head : o4.equals(pair.head)))) {
      return false;
    }
    pair = pair.tail;

    if (! ((EMPTY != pair) &&
           (null == o5 ? null == pair.head : o5.equals(pair.head)))) {
      return false;
    }

    return EMPTY == pair.tail;
  }

  /**
   * Determine whether the list starting at this pair consists of the
   * specified objects.  If any of the specified objects is not
   * <code>null</code>, the implementation invokes
   * <code>equals()</code> on the object.
   *
   * @param os The objects.
   * @return <code>true</code> if the list starting at this pair
   *   consists of the specified objects.
   */
  public boolean consists(Object... os) {
    Pair pair = this;
    int  size = 0;

    while ((EMPTY != pair) && (size < os.length)) {
      Object h1 = pair.head;
      Object h2 = os[size];

      if (! (null == h2 ? null == h1 : h2.equals(h1))) return false;

      pair = pair.tail;
      size++;
    }

    return (EMPTY == pair) && (os.length == size);
  }

  /**
   * Get an iterator over the values of the list starting at this
   * pair.
   *
   * @return The iterator.
   */
  public Iterator<T> iterator() {
    return new Iterator<T>() {
        private Pair<T> pair = Pair.this;

        public boolean hasNext() {
          return (EMPTY != pair);
        }

        public T next() {
          if (EMPTY == pair) {
            throw new NoSuchElementException();
          } else {
            T h  = pair.head;
            pair = pair.tail;
            return h;
          }
        }

        public void remove() {
          throw new UnsupportedOperationException();
        }
      };
  }

  /**
   * Reverse the list starting at this pair in place.  This method
   * destructively reverses the list starting at this pair.  Note that
   * this method's performance is linear in the size of the list.
   *
   * @return The new head of the list.
   */
  public Pair<T> reverse() {
    Pair<T> forward  = this;
    Pair<T> backward = Pair.empty();

    while (EMPTY != forward) {
      Pair<T> temp  = backward;
      backward      = forward;
      forward       = forward.tail;
      backward.tail = temp;
    }

    return backward;
  }

  /**
   * Add the specified element to the list starting at this pair.  If
   * this list is the empty list, this method simply allocates a new
   * pair.  Otherwise, it modifies this list's last non-empty element.
   * In either case, this method returns the newly created pair to
   * facilitate incremental construction of lists.
   *
   * @param element The element.
   * @return The newly added pair.
   * @throws IllegalStateException Signals that this pair represent
   *   the empty list.
   */
  public Pair<T> add(T element) {
    if (this == EMPTY) {
      Pair<T> p = new Pair<T>(element);
      return p;
    }

    Pair<T> last = this;
    while (EMPTY != last.tail) last = last.tail;
    last.tail    = new Pair<T>(element);
    return last.tail;
  }

  /**
   * Set the tail of this list's last pair to the specified value.
   * Effectively, this method destructively appends the list starting
   * at the specified pair to the list starting at this pair.  Its
   * performance is linear in the size of the list.
   *
   * @see #append(Pair)
   *
   * @param tail The new tail.
   * @throws NullPointerException Signals that <code>tail</code> is
   *   <code>null</code>.
   * @throws IllegalStateException Signals that this pair represents
   *   the empty list.
   */
  public void setLastTail(Pair<T> tail) {
    if (null == tail) {
      throw new NullPointerException("Null tail");
    } else if (EMPTY == this) {
      throw new IllegalStateException("Empty list");
    }

    Pair<T> pair = this;
    while (EMPTY != pair.tail) {
      pair = pair.tail;
    }

    pair.tail = tail;
  }

  /**
   * Set the tail of a copy of this list's last pair to the specified
   * value.  Effectively, this method non-destructively appends the
   * list starting at the specified pair to the list starting at this
   * pair.  Its performance is linear in the size of the list.
   *
   * @see #setLastTail(Pair)
   *
   * @param tail The new tail.
   * @return The new head.
   * @throws NullPointerException Signals that <code>tail</code> is
   *   <code>null</code>.
   */
  public Pair<T> append(Pair<T> tail) {
    if (null == tail) {
      throw new NullPointerException("Null tail");
    } else if (EMPTY == this) {
      return tail;
    }

    // This pair's current pair, the copy's head, the copy's current
    // pair.
    Pair<T> pair, copy, cursor;

    pair   = this;
    copy   = new Pair<T>(pair.head);
    cursor = copy;

    while (EMPTY != pair.tail) {
      pair        = pair.tail;
      cursor.tail = new Pair<T>(pair.head);
      cursor      = cursor.tail;
    }

    cursor.tail = tail;
    return copy;
  }

  /**
   * Combine the elements on the list starting at this pair with the
   * elements on the list starting at the specified pair.  Note that
   * this method's performance is quadratic in the size of the lists.
   *
   * @param list The other list.
   * @return The set union.
   */
  public Pair<T> combine(Pair<T> list) {
    Pair<T> pair   = this;
    Pair<T> result = list;

    while (EMPTY != pair) {
      T head = pair.head;
      if (! list.contains(head)) result = new Pair<T>(head, result);
      pair = pair.tail;
    }

    return result;
  }

  /**
   * Intersect the elements on the list starting at this pair with the
   * elements on the list starting at the specified pair.  Note that
   * this method's performance is quadratic in the size of the lists.
   *
   * @param list The other list.
   * @return The set intersection.
   */
  public Pair<T> intersect(Pair<T> list) {
    Pair<T> pair   = this;
    Pair<T> result = Pair.empty();

    while (EMPTY != pair) {
      T head = pair.head;
      if (list.contains(head)) result = new Pair<T>(head, result);
      pair = pair.tail;
    }

    return result;
  }

  /**
   * Subtract the elements on the list starting at the specified list
   * from the elements on the list starting at this pair.  Note that
   * this method's performance is quadratic in the size of the lists.
   *
   * @param list The list to subtract.
   * @return The set subtraction.
   */
  public Pair<T> subtract(Pair<T> list) {
    Pair<T> pair   = this;
    Pair<T> result = Pair.empty();

    while (EMPTY != pair) {
      T head = pair.head;
      if (! list.contains(head)) result = new Pair<T>(head, result);
      pair = pair.tail;
    }

    return result;
  }

  /**
   * Add the values of the list starting at this pair in order to the
   * specified Java collections framework list.
   *
   * @param l The list.
   */
  public void addTo(List<? super T> l) {
    Pair<T> p = this;

    while (EMPTY != p) {
      l.add(p.head);
      p = p.tail;
    }
  }

  /**
   * Convert the list starting at this pair into a Java collections
   * framework list.  Note that this method's performance is linear to
   * the size of the list.
   *
   * @return The list.
   */
  public List<T> list() {
    ArrayList<T> l = new ArrayList<T>(size());
    addTo(l);
    return l;
  }

  /**
   * Get a string representation for the list starting at this pair.
   * Note that this method's performance is linear to the size of the
   * list.
   *
   * @return A string representation.
   */
  public String toString() {
    Pair          pair = this;
    StringBuilder buf  = new StringBuilder();
    buf.append('[');

    while (EMPTY != pair) {
      Object head = pair.head;
      
      buf.append(null == head ? "null" : head.toString());

      pair = pair.tail;
      if (EMPTY != pair) buf.append(", ");
    }

    buf.append(']');
    return buf.toString();
  }

  /**
   * Get the canoncial empty pair.
   *
   * @return The canonical empty pair.
   */
  @SuppressWarnings({ "unchecked", "cast" })
  public static final <T> Pair<T> empty() {
    return (Pair<T>)EMPTY;
  }

}
