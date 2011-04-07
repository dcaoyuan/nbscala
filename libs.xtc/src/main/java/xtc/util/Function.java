/*
 * xtc - The eXTensible Compiler
 * Copyright (C) 2007 Robert Grimm, New York University
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

/**
 * Function definitions. 
 *
 * @author Laune Harris
 * @author Robert Grimm
 * @version $Revision: 1.5 $
 */
public class Function {

  /** Hidden constructor. */
  private Function() { /* Nothing to do. */ }

  /** A function with no arguments. */
  public static interface F0<R> {
    R apply();
  }
  
  /** A function with one argument. */
  public static interface F1<R,A> {
    R apply(A a);
  }
  
  /** A function with two arguments. */
  public static interface F2<R,A,B> {
    R apply(A a,B b);
  }
  
  /** A function with three arguments. */
  public static interface F3<R,A,B,C> {
    R apply(A a,B b,C c);
  }
  
  /** A function with four arguments. */
  public static interface F4<R,A,B,C,D> {
    R apply(A a,B b,C c,D d);
  }

  /** A function with five arguments. */
  public static interface F5<R,A,B,C,D,E> {
    R apply(A a,B b,C c,D d,E e);
  }
  
  /** A function with six arguments. */
  public static interface F6<R,A,B,C,D,E,F> {
    R apply(A a,B b,C c,D d,E e,F f);
  }

  /** A function with seven arguments. */
  public static interface F7<R,A,B,C,D,E,F,G> {
    R apply(A a,B b,C c,D d,E e,F f,G g);
  }
  
  /** A function with eight arguments. */
  public static interface F8<R,A,B,C,D,E,F,G,H> {
    R apply(A a,B b,C c,D d,E e,F f,G g,H h);
  }
  
  /** A function with nine arguments. */
  public static interface F9<R,A,B,C,D,E,F,G,H> {
    R apply(A a,B b,C c,D d,E e,F f,G g,H h);
  }
  
  /** A function with ten arguments. */
  public static interface F10<R,A,B,C,D,E,F,G,H,I> {
    R apply(A a,B b,C c,D d,E e,F f,G g,H h,I i);
  }
  
  /** A function with eleven arguments. */
  public static interface F11<R,A,B,C,D,E,F,G,H,I,J> {
    R apply(A a,B b,C c,D d,E e,F f,G g,H h,I i,J j);
  }
  
  /** A function with twelve arguments. */
  public static interface F12<R,A,B,C,D,E,F,G,H,I,J,K> {
    R apply(A a,B b,C c,D d,E e,F f,G g,H h,I i,J j,K k);
  } 
  
  /** A function with thirteen arguments. */
  public static interface F13<R,A,B,C,D,E,F,G,H,I,J,K,L> {
    R apply(A a,B b,C c,D d,E e,F f,G g,H h,I i,J j,K k,L l);
  } 
  
  /** A function with fourteen arguments. */
  public static interface F14<R,A,B,C,D,E,F,G,H,I,J,K,L,M> {
    R apply(A a,B b,C c,D d,E e,F f,G g,H h,I i,J j,K k,L l,M m);
  } 
  
  /** A function with fifteen arguments. */
  public static interface F15<R,A,B,C,D,E,F,G,H,I,J,K,L,M,N> {
    R apply(A a,B b,C c,D d,E e,F f,G g,H h,I i,J j,K k,L l,M m,N n);
  } 

  /**
   * Iterate the specified function over the specified list.
   *
   * @param function The function.
   * @param list The list.
   */
  public static <T, U>
  void iterate(Function.F1<U,? super T> function, Pair<T> list) {
    while (Pair.EMPTY != list) {
      function.apply(list.head);
      list = list.tail;
    }
  }

  /**
   * Map the specified function over the specified list.  Note that
   * the implementation does not recurse.
   *
   * @param function The function.
   * @param list The list.
   * @return The result of mapping the function.
   */
  public static <T, U>
  Pair<U> map(Function.F1<U,? super T> function, Pair<T> list) {
    if (Pair.EMPTY == list) return Pair.empty();

    Pair<U> result = new Pair<U>(function.apply(list.head));
    Pair<U> cursor = result;

    while (Pair.EMPTY != list.tail) {
      list        = list.tail;
      cursor.tail = new Pair<U>(function.apply(list.head));
      cursor      = cursor.tail;
    }

    return result;
  }

  /**
   * Fold the specified list with the specified function.  This method
   * successively applies the specified function to each of the list's
   * elements and a running result, which is initialized to the
   * specified seed.
   *
   * @param function The function.
   * @param seed The seed value.
   * @param list The list.
   * @return The folded value.
   */
  public static <T, U>
  U foldl(Function.F2<U,? super T,U> function, U seed, Pair<T> list) {
    while (Pair.EMPTY != list) {
      seed = function.apply(list.head, seed);
      list = list.tail;
    }
    return seed;
  }

  /**
   * Determine whether the specified list contains only elements
   * matching the specified predicate.
   *
   * @param pred The predicate.
   * @param list The list.
   * @return <code>true</code> if the list contains only matching
   *   elements.
   */
  public static <T>
  boolean matchesAll(Function.F1<Boolean,? super T> pred, Pair<T> list) {
    while (Pair.EMPTY != list) {
      if (! pred.apply(list.head)) return false;
      list = list.tail;
    }   
    return true;
  }

  /**
   * Determine whether the specified list contains an element matching
   * the specified predicate.
   *
   * @param pred The predicate.
   * @param list The list.
   * @return <code>true</code> if the list contains at least one
   *   matching element.
   */
  public static <T>
  boolean matchesOne(Function.F1<Boolean,? super T> pred, Pair<T> list) {
    while (Pair.EMPTY != list) {
      if (pred.apply(list.head)) return true;
      list = list.tail;
    }   
    return false;
  }

}
