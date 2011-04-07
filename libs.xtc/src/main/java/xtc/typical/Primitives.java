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
package xtc.typical;

import java.math.BigInteger;
import java.util.Hashtable;
import java.util.HashSet;
import java.util.Set;
import java.util.HashMap;
import java.util.Iterator;

import xtc.tree.Node;

import xtc.util.Pair;
import xtc.util.Function;

/**
 * The primitives for Typical.
 * 
 * @author Robert Grimm
 * @version $Revision: 1.49 $
 */ 
public class Primitives {

  /** The minimum integer value as a big integer. */
  private static final BigInteger INT_MIN =
    BigInteger.valueOf(Integer.MIN_VALUE);

  /** The maximum integer value as a big integer. */
  private static final BigInteger INT_MAX =
    BigInteger.valueOf(Integer.MAX_VALUE);

  private Primitives() { /* Nothing to do. */ }

  // =========================================================================
  //                             Helper methods
  // =========================================================================

  /**
   * Convert the specified big integer to an int value.
   *
   * @param val The big integer.
   * @return The corresponding int value.
   * @throws IllegalArgumentException Signals that the big integer's
   *   value is too large.
   */
  public static final int toInt(BigInteger val) {
    if ((val.compareTo(INT_MIN) < 0) || (val.compareTo(INT_MAX) > 0)) {
      throw new IllegalArgumentException("integer too large: " + val);
    }
    return val.intValue();
  }

  /**
   * Trace the specified object.
   *
   * @param msg The message prefix.
   * @param o The object.
   */
  public static final void trace(String msg, Object o) {
    System.out.print(msg);
    if (null == o) {
      System.out.println("bottom");
    } else {
      System.out.println(o.toString());
    }
    System.out.flush();
  }

  // =========================================================================
  //                           Testing for bottom
  // =========================================================================

  /** Test a value for bottom. */
  public static final Function.F1<Boolean,Object> isBottom =
    new Function.F1<Boolean,Object>() {
    public Boolean apply(Object o) {
      return null == o;
    }
  };

  // -------------------------------------------------------------------------
  
  /** Test a value for not being bottom. */
  public static final Function.F1<Boolean,Object> isNotBottom =
    new Function.F1<Boolean,Object>() {
    public Boolean apply(Object o) {
      return null != o;
    }
  };

  // =========================================================================
  //                           Boolean operations
  // =========================================================================

  /** Negate a boolean. */
  public static final Function.F1<Boolean,Boolean> not =
    new Function.F1<Boolean,Boolean>() {
    public Boolean apply(Boolean val) {
      return null == val ? null : !val;
    }
  };

  // -------------------------------------------------------------------------
  
  /** And two booleans. */
  public static final Function.F2<Boolean,Boolean,Boolean> and =
    new Function.F2<Boolean,Boolean,Boolean>() {
    public Boolean apply(Boolean val1, Boolean val2) {
      return (null == val1) || (null == val2) ? null : val1 && val2;
    }
  };

  // -------------------------------------------------------------------------
  
  /** Or two booleans. */
  public static final Function.F2<Boolean,Boolean,Boolean> or =
    new Function.F2<Boolean,Boolean,Boolean>() {
    public Boolean apply(Boolean val1, Boolean val2) {
      return (null == val1) || (null == val2) ? null : val1 || val2;
    }
  };

  // =========================================================================
  //                         Comparsion operations
  // =========================================================================

  /** Determine whether two objects are equal. */
  public static final Function.F2<Boolean,Object,Object> equal =
    new Function.F2<Boolean,Object,Object>() {
    public Boolean apply(Object val1, Object val2) {
      return (null == val1) || (null == val2) ? null : val1.equals(val2);
    }
  };

  // -------------------------------------------------------------------------
  
  /** Determine whether one big integer is less than another. */
  public static final Function.F2<Boolean,BigInteger,BigInteger> lessInt =
    new Function.F2<Boolean,BigInteger,BigInteger>() {
    public Boolean apply(BigInteger val1, BigInteger val2) {
      return (null == val1) || (null == val2) ? null : val1.compareTo(val2) < 0;
    }
  };

  // -------------------------------------------------------------------------
  
  /** Determine whether one double is less than another. */
  public static final Function.F2<Boolean,Double,Double> lessFloat64 =
    new Function.F2<Boolean,Double,Double>() {
    public Boolean apply(Double val1, Double val2) {
      return (null == val1) || (null == val2) ? null : val1 < val2;
    }
  };

  // -------------------------------------------------------------------------
  
  /** Determine whether one float is less than another. */
  public static final Function.F2<Boolean,Float,Float> lessFloat32 =
    new Function.F2<Boolean,Float,Float>() {
    public Boolean apply(Float val1, Float val2) {
      return (null == val1) || (null == val2) ? null : val1 < val2;
    }
  };

  // -------------------------------------------------------------------------
  
  /** Determine whether one big integer is less equal than another. */
  public static final Function.F2<Boolean,BigInteger,BigInteger> lessEqualInt =
    new Function.F2<Boolean,BigInteger,BigInteger>() {
    public Boolean apply(BigInteger val1, BigInteger val2) {
      return (null == val1) || (null == val2) ? null : val1.compareTo(val2) <= 0;
    }
  };

  // -------------------------------------------------------------------------
  
  /** Determine whether one double is greater than another. */
  public static final Function.F2<Boolean,Double,Double> lessEqualFloat64 =
    new Function.F2<Boolean,Double,Double>() {
    public Boolean apply(Double val1, Double val2) {
      return (null == val1) || (null == val2) ? null : val1 <= val2;
    }
  };

  // -------------------------------------------------------------------------
  
  /** Determine whether one float is greater than another. */
  public static final Function.F2<Boolean,Float,Float> lessEqualFloat32 =
    new Function.F2<Boolean,Float,Float>() {
    public Boolean apply(Float val1, Float val2) {
      return (null == val1) || (null == val2) ? null : val1 <= val2;
    }
  };

  // -------------------------------------------------------------------------
  
  /** Determine whether one big integer is greater than another. */
  public static final Function.F2<Boolean,BigInteger,BigInteger> greaterInt =
    new Function.F2<Boolean,BigInteger,BigInteger>() {
    public Boolean apply(BigInteger val1, BigInteger val2) {
      return (null == val1) || (null == val2) ? null : val1.compareTo(val2) > 0;
    }
  };

  // -------------------------------------------------------------------------
  
  /** Determine whether one double is greater than another. */
  public static final Function.F2<Boolean,Double,Double> greaterFloat64 =
    new Function.F2<Boolean,Double,Double>() {
    public Boolean apply(Double val1, Double val2) {
      return (null == val1) || (null == val2) ? null : val1 > val2;
    }
  };

  // -------------------------------------------------------------------------
  
  /** Determine whether one float is greater than another. */
  public static final Function.F2<Boolean,Float,Float> greaterFloat32 =
    new Function.F2<Boolean,Float,Float>() {
    public Boolean apply(Float val1, Float val2) {
      return (null == val1) || (null == val2) ? null : val1 > val2;
    }
  };


  // -------------------------------------------------------------------------
  
  /** Determine whether one big integer is greater equal than another. */
  public static final Function.F2<Boolean,BigInteger,BigInteger>greaterEqualInt=
    new Function.F2<Boolean,BigInteger,BigInteger>() {
    public Boolean apply(BigInteger val1, BigInteger val2) {
      return (null == val1) || (null == val2) ? null : val1.compareTo(val2) >= 0;
    }
  };

  // -------------------------------------------------------------------------
  
  /** Determine whether one double is greater than another. */
  public static final Function.F2<Boolean,Double,Double> greaterEqualFloat64 =
    new Function.F2<Boolean,Double,Double>() {
    public Boolean apply(Double val1, Double val2) {
      return (null == val1) || (null == val2) ? null : val1 >= val2;
    }
  };

  // -------------------------------------------------------------------------
  
  /** Determine whether one float is greater than another. */
  public static final Function.F2<Boolean,Float,Float> greaterEqualFloat32 =
    new Function.F2<Boolean,Float,Float>() {
    public Boolean apply(Float val1, Float val2) {
      return (null == val1) || (null == val2) ? null : val1 >= val2;
    }
  };
  
  // =========================================================================
  //                         Arithmetic operations
  // =========================================================================

  /** Negate a big integer. */
  public static final Function.F1<BigInteger,BigInteger> negateInt =
    new Function.F1<BigInteger,BigInteger>() {
    public BigInteger apply(BigInteger val) {
      return null == val ? null : val.negate();
    }
  };

  // -------------------------------------------------------------------------
  
  /** Negate a double. */
  public static final Function.F1<Double,Double> negateFloat64 =
    new Function.F1<Double,Double>() {
    public Double apply(Double val) {
      return null == val ? null : -val;
    }
  };

  // -------------------------------------------------------------------------
  
  /** Negate a float. */
  public static final Function.F1<Float,Float> negateFloat32 =
    new Function.F1<Float,Float>() {
    public Float apply(Float val) {
      return null == val ? null : -val;
    }
  };

  // -------------------------------------------------------------------------

  /** Affirm a big integer. */
  public static final Function.F1<BigInteger,BigInteger> absInt =
    new Function.F1<BigInteger,BigInteger>() {
    public BigInteger apply(BigInteger val) {
      return null == val ? null : val.abs();
    }
  };

  // -------------------------------------------------------------------------
  
  /** Affirm a double. */
  public static final Function.F1<Double,Double> absFloat64 =
    new Function.F1<Double,Double>() {
    public Double apply(Double val) {
      return null == val ? null : +val;
    }
  };

  // -------------------------------------------------------------------------
  
  /** Affirm a float. */
  public static final Function.F1<Float,Float> absFloat32 =
    new Function.F1<Float,Float>() {
    public Float apply(Float val) {
      return null == val ? null : +val;
    }
  };

  // -------------------------------------------------------------------------
  
  /** Add two big integers. */
  public static final Function.F2<BigInteger,BigInteger,BigInteger> addInt =
    new Function.F2<BigInteger,BigInteger,BigInteger>() {
    public BigInteger apply(BigInteger val1, BigInteger val2) {
      return (null == val1) || (null == val2) ? null : val1.add(val2);
    }
  };

  // -------------------------------------------------------------------------
  
  /** Add two doubles. */
  public static final Function.F2<Double,Double,Double> addFloat64 =
    new Function.F2<Double,Double,Double>() {
    public Double apply(Double val1, Double val2) {
      return (null == val1) || (null == val2) ? null : val1 + val2;
    }
  };

  // -------------------------------------------------------------------------
  
  /** Add two floats. */
  public static final Function.F2<Float,Float,Float> addFloat32 =
    new Function.F2<Float,Float,Float>() {
    public Float apply(Float val1, Float val2) {
      return (null == val1) || (null == val2) ? null : val1 + val2;
    }
  };

  // -------------------------------------------------------------------------
  
  /** Subtract two big integers. */
  public static final Function.F2<BigInteger,BigInteger,BigInteger> subtractInt=
    new Function.F2<BigInteger,BigInteger,BigInteger>() {
    public BigInteger apply(BigInteger val1, BigInteger val2) {
      return (null == val1) || (null == val2) ? null : val1.subtract(val2);
    }
  };

  // -------------------------------------------------------------------------
  
  /** Subtract two doubles. */
  public static final Function.F2<Double,Double,Double> subtractFloat64 =
    new Function.F2<Double,Double,Double>() {
    public Double apply(Double val1, Double val2) {
      return (null == val1) || (null == val2) ? null : val1 - val2;
    }
  };

  // -------------------------------------------------------------------------
  
  /** Subtract two floats. */
  public static final Function.F2<Float,Float,Float> subtractFloat32 =
    new Function.F2<Float,Float,Float>() {
    public Float apply(Float val1, Float val2) {
      return (null == val1) || (null == val2) ? null : val1 - val2;
    }
  };

  // -------------------------------------------------------------------------
  
  /** Multiply two big integers. */
  public static final Function.F2<BigInteger,BigInteger,BigInteger> multiplyInt=
    new Function.F2<BigInteger,BigInteger,BigInteger>() {
    public BigInteger apply(BigInteger val1, BigInteger val2) {
      return (null == val1) || (null == val2) ? null : val1.multiply(val2);
    }
  };

  // -------------------------------------------------------------------------
  
  /** Multiply two doubles. */
  public static final Function.F2<Double,Double,Double> multiplyFloat64 =
    new Function.F2<Double,Double,Double>() {
    public Double apply(Double val1, Double val2) {
      return (null == val1) || (null == val2) ? null : val1 * val2;
    }
  };

  // -------------------------------------------------------------------------
  
  /** Multiply two floats. */
  public static final Function.F2<Float,Float,Float> multiplyFloat32 =
    new Function.F2<Float,Float,Float>() {
    public Float apply(Float val1, Float val2) {
      return (null == val1) || (null == val2) ? null : val1 * val2;
    }
  };

  // -------------------------------------------------------------------------
  
  /** Divide two big integers. */
  public static final Function.F2<BigInteger,BigInteger,BigInteger> divideInt =
    new Function.F2<BigInteger,BigInteger,BigInteger>() {
    public BigInteger apply(BigInteger val1, BigInteger val2) {
      return (null == val1) || (null == val2) ? null : val1.divide(val2);
    }
  };

  // -------------------------------------------------------------------------
  
  /** Divide two doubles. */
  public static final Function.F2<Double,Double,Double> divideFloat64 =
    new Function.F2<Double,Double,Double>() {
    public Double apply(Double val1, Double val2) {
      return (null == val1) || (null == val2) ? null : val1 / val2;
    }
  };

  // -------------------------------------------------------------------------
  
  /** Divide two Floats. */
  public static final Function.F2<Float,Float,Float> divideFloat32 =
    new Function.F2<Float,Float,Float>() {
    public Float apply(Float val1, Float val2) {
      return (null == val1) || (null == val2) ? null : val1 / val2;
    }
  };

  // -------------------------------------------------------------------------
  
  /** Determine the modulo of two big integers. */
  public static final Function.F2<BigInteger,BigInteger,BigInteger> modInt =
    new Function.F2<BigInteger,BigInteger,BigInteger>() {
    public BigInteger apply(BigInteger val1, BigInteger val2) {
      return (null == val1) || (null == val2) ? null : val1.mod(val2);
    }
  };

  // -------------------------------------------------------------------------
  
  /** Determine the modulo of two doubles. */
  public static final Function.F2<Double,Double,Double> modFloat64 =
    new Function.F2<Double,Double,Double>() {
    public Double apply(Double val1, Double val2) {
      return (null == val1) || (null == val2) ? null : val1 % val2;
    }
  };

  // -------------------------------------------------------------------------
  
  /** Determine the modulo of two floats. */
  public static final Function.F2<Float,Float,Float> modFloat32 =
    new Function.F2<Float,Float,Float>() {
    public Float apply(Float val1, Float val2) {
      return (null == val1) || (null == val2) ? null : val1 % val2;
    }
  };

  // =========================================================================
  //                           Bitwise operations
  // =========================================================================

  /** Bitwise negate a big integer. */
  public static final Function.F1<BigInteger,BigInteger> negateBits =
    new Function.F1<BigInteger,BigInteger>() {
    public BigInteger apply(BigInteger val) {
      return null == val ? null : val.not();
    }
  };

  // -------------------------------------------------------------------------
  
  /** Bitwise and two big integers. */
  public static final Function.F2<BigInteger,BigInteger,BigInteger> andBits =
    new Function.F2<BigInteger,BigInteger,BigInteger>() {
    public BigInteger apply(BigInteger val1, BigInteger val2) {
      return (null == val1) || (null == val2) ? null : val1.and(val2);
    }
  };

  // -------------------------------------------------------------------------
  
  /** Bitwise or two big integers. */
  public static final Function.F2<BigInteger,BigInteger,BigInteger> orBits =
    new Function.F2<BigInteger,BigInteger,BigInteger>() {
    public BigInteger apply(BigInteger val1, BigInteger val2) {
      return (null == val1) || (null == val2) ? null : val1.or(val2);
    }
  };

  // -------------------------------------------------------------------------
  
  /** Bitwise xor two big integers. */
  public static final Function.F2<BigInteger,BigInteger,BigInteger> xorBits =
    new Function.F2<BigInteger,BigInteger,BigInteger>() {
    public BigInteger apply(BigInteger val1, BigInteger val2) {
      return (null == val1) || (null == val2) ? null : val1.xor(val2);
    }
  };

  // -------------------------------------------------------------------------
  
  /** Shift left a big integer. */
  public static final Function.F2<BigInteger,BigInteger,BigInteger> shiftLeft =
    new Function.F2<BigInteger,BigInteger,BigInteger>() {
    public BigInteger apply(BigInteger val1, BigInteger val2) {
      return (null == val1) || (null == val2) ?
        null : val1.shiftLeft(toInt(val2));
    }
  };

  // -------------------------------------------------------------------------
  
  /** Shift right a big integer. */
  public static final Function.F2<BigInteger,BigInteger,BigInteger> shiftRight =
    new Function.F2<BigInteger,BigInteger,BigInteger>() {
    public BigInteger apply(BigInteger val1, BigInteger val2) {
      return (null == val1) || (null == val2) ?
        null : val1.shiftRight(toInt(val2));
    }
  };

  // -------------------------------------------------------------------------
  
  /** Convert an int to a float. */
  public static final Function.F1<Double, BigInteger> itof =
    new Function.F1<Double, BigInteger>() {
    public Double apply(BigInteger i) {
      return (null == i) ? null : i.doubleValue();
    }
  };

  // -------------------------------------------------------------------------
  
  /** Convert a float to an int  . */
  public static final Function.F1<BigInteger, Double> ftoi =
    new Function.F1<BigInteger, Double>() {
    public BigInteger apply(Double d) {
      return (null == d) ? null : new BigInteger(d.toString());
    }
  };
  
  // =========================================================================
  //                            List operations
  // =========================================================================

  /** Cons a value onto a list. */
  @SuppressWarnings("unchecked")
  public static final Function.F2<Pair,Object,Pair> cons =
    new Function.F2<Pair,Object,Pair>() {
    public Pair apply(Object head, Pair tail) {
      return null == tail ? null : new Pair(head, tail);
    }
  };

  /** Cons a value onto a list. */
  public static final class Cons<T> implements Function.F2<Pair<T>,T,Pair<T>> {
    public Pair<T> apply(T head, Pair<T> tail) {
      return null == tail ? null : new Pair<T>(head, tail);
    }
  }

  /**
   * Magic cast wrapper for Primitives.cons
   * 
   * @param head The head of the pair.
   * @param p The tail of the pair.
   * @return The consed pair. 
   */ 
  @SuppressWarnings("unchecked")
  public static final <T> Pair<T> wrapCons(T head, Pair<T> p) {
    return Primitives.cons.apply(head, p);
  }

  // -------------------------------------------------------------------------

  /** Determine whether a list is empty. */
  public static final Function.F1<Boolean,Pair<?>> isEmpty =
    new Function.F1<Boolean,Pair<?>>() {
    public Boolean apply(Pair<?> list) {
      return null == list ? null : list.isEmpty();
    }
  };

  // -------------------------------------------------------------------------

  /** Get a list's head. */
  public static final Function.F1<Object,Pair<?>> head =
    new Function.F1<Object,Pair<?>>() {
    public Object apply(Pair<?> list) {
      return (null == list) || (list.isEmpty()) ? null : list.head();
    }
  };

  /** Get a list's head. */
  public static final class Head<T> implements Function.F1<T,Pair<T>> {
    public T apply(Pair<T> list) {
      return (null == list) || (list.isEmpty()) ? null : list.head();
    }
  }
  
  /**
   * Magic cast wrapper for head
   *
   * @param p The pair.
   * @return The head of the pair.
   */
  @SuppressWarnings("unchecked")
  public static final <T> T wrapHead(Pair<T> p) {
    return (T)Primitives.head.apply(p);
  }

  // -------------------------------------------------------------------------

  /** Get a list's tail. */
  public static final Function.F1<Pair<?>,Pair<?>> tail =
    new Function.F1<Pair<?>,Pair<?>>() {
    public Pair<?> apply(Pair<?> list) {
      return (null == list) || (list.isEmpty()) ? null : list.tail();
    }
  };

  /** Get a list's tail. */
  public static final class Tail<T> implements Function.F1<Pair<T>,Pair<T>> {
    public Pair<T> apply(Pair<T> list) {
      return (null == list) || (list.isEmpty()) ? null : list.tail();
    }
  }

  /**
   * Magic cast wrapper for Primitives.tail
   *
   * @param p The pair.
   * @return The tail of the pair.
   */ 
  @SuppressWarnings("unchecked")
  public static final <T> Pair<T> wrapTail(Pair<T> p) {
    return (Pair<T>)Primitives.tail.apply(p);
  }

  // -------------------------------------------------------------------------

  /** Get a list's length. */
  public static final Function.F1<BigInteger,Pair<?>> length =
    new Function.F1<BigInteger,Pair<?>>() {
    public BigInteger apply(Pair<?> list) {
      return null == list ? null : BigInteger.valueOf(list.size());
    }
  };

  // -------------------------------------------------------------------------

  /** Get a list's nth element. */
  public static final Function.F2<Object,Pair<?>,BigInteger> nth =
    new Function.F2<Object,Pair<?>,BigInteger>() {
    public Object apply(Pair<?> list, BigInteger index) {
      return (null == list) || (null == index) ? null : list.get(toInt(index));
    }
  };

  /** Get a list's nth element. */
  public static final class Nth<T>
    implements Function.F2<T,Pair<T>,BigInteger> {
    public T apply(Pair<T> list, BigInteger index) {
      return (null == list) || (null == index) ? null : list.get(toInt(index));
    }
  }

  // -------------------------------------------------------------------------

  /** Determine whether a list contains an element. */
  public static final Function.F2<Boolean,Object,Pair<?>> contains =
    new Function.F2<Boolean,Object,Pair<?>>() {
    public Boolean apply(Object elem, Pair<?> list) {
      return (null == elem) || (null == list) ? null : list.contains(elem);
    }
  };

  // -------------------------------------------------------------------------

  /** Determine whether a list element satisfies a predicate. */
  @SuppressWarnings("unchecked")
  public static final
  Function.F2<Boolean,Function.F1<Boolean,Object>,Pair> exists =
    new Function.F2<Boolean,Function.F1<Boolean,Object>,Pair>() {
    public Boolean apply(Function.F1<Boolean,Object> pred, Pair list) {
      return ((null == pred) || (null == list)) ?
        null : Function.matchesOne(pred, list);
    }
  };

  /** Determine whether a list element satisfies a predicate. */
  public static final class Exists<T>
    implements Function.F2<Boolean,Function.F1<Boolean,? super T>,Pair<T>> {
    public Boolean apply(Function.F1<Boolean,? super T> pred, Pair<T> list) {
      return (null == pred) || (null == list) ?
        null : Function.matchesOne(pred, list);
    }
  }
  
  // -------------------------------------------------------------------------

  /** Iterate a function over a list. */
  @SuppressWarnings("unchecked")
  public static final Function.F2<Object,Function.F1<Object,Object>,Pair> iter =
    new Function.F2<Object,Function.F1<Object,Object>,Pair>() {
    public Object apply(Function.F1<Object,Object> func, Pair list) {
      if ((null == func) || (null == list)) return null;
      Function.iterate(func, list);
      return null;
    }
  };

  /** Iterate a function over a list. */
  public static final class Iter<T,U>
    implements Function.F2<T,Function.F1<T,? super U>,Pair<U>> {
    public T apply(Function.F1<T,? super U> func, Pair<U> list) {
      if ((null == func) || (null == list)) return null;
      Function.iterate(func, list);
      return null;
    }
  }

  // -------------------------------------------------------------------------

  /** Map a function over a list. */
  @SuppressWarnings("unchecked")
  public static final Function.F2<Pair,Function.F1<Object,Object>,Pair> map =
    new Function.F2<Pair,Function.F1<Object,Object>,Pair>() {
    public Pair apply(Function.F1<Object,Object> func, Pair list) {
      return (null == func) || (null == list) ? null : Function.map(func, list);
    }
  };

  /** Map a function over a list. */
  public static final class Map<T,U>
    implements Function.F2<Pair<T>,Function.F1<T,? super U>,Pair<U>> {
    public Pair<T> apply(Function.F1<T,? super U> func, Pair<U> list) {
      return (null == func) || (null == list) ? null : Function.map(func, list);
    }
  }

  // -------------------------------------------------------------------------

  /** Fold a list. */
  @SuppressWarnings("unchecked")
  public static final Function.
  F3<Object,Function.F2<Object,Object,Object>,Pair,Object> foldl =
    new Function.F3<Object,Function.F2<Object,Object,Object>,Pair,Object>() {
    public Object apply(Function.F2<Object,Object,Object> f, Pair l, Object s) {
      return (null == f) || (null == l) || (null == s) ?
        null : Function.foldl(f,s, l);
    }
  };

  /** Fold a list. */
  public static final class FoldLeft<T,U>
    implements Function.F3<T,Function.F2<T,? super U,T>,Pair<U>,T> {
    public T apply(Function.F2<T,? super U,T> f, Pair<U> l, T s) {
      return (null == f) || (null == l) || (null == s) ?
        null : Function.foldl(f, s, l);
    }
  }

  // -------------------------------------------------------------------------

  /** Non-destructively append two lists. */
  @SuppressWarnings("unchecked")
  public static final Function.F2<Pair,Pair,Pair> append =
    new Function.F2<Pair,Pair,Pair>() {
    public Pair apply(Pair list1, Pair list2) {
      return (null == list1) || (null == list2) ? null : list1.append(list2);
    }
  };

  /** Non-destructively append two lists. */
  public static final class Append<T>
    implements Function.F2<Pair<T>,Pair<T>,Pair<T>> {
    public Pair<T> apply(Pair<T> list1, Pair<T> list2) {
      return (null == list1) || (null == list2) ? null : list1.append(list2);
    }
  }

  /** 
   * Magic cast wrapper for append
   *
   * @param l The left pair.
   * @param r The right pair.
   * @return The concatenation of the two pairs.
   */ 
  @SuppressWarnings("unchecked")
  public static final <T> Pair<T> wrapAppend(Pair<T> l, Pair<T> r) {
    return Primitives.append.apply(l,r);
  }

   /**
   * Find an object in a pair.
   *
   * @param o The object to find.
   * @param p The list.
   * @return The object or <code>null</code> if the list does not
   *   contain the object.
   */
  public static final <T> T findInPair(Object o, Pair<T> p) {
    if ((null == o) || (null == p)) return null;

    for (T t : p) {
      if (o.equals(t)) return t;
    }

    return null;
  }
  
  /**
   * Copy a list.
   * 
   * @param left The list to copy.
   * @return The copy.
   */
  @SuppressWarnings("unchecked")
  static public final <T,R> Pair<T> copyPair(Pair<R> left) {
    if (null == left) return null;
    Pair<T> result = Pair.empty();
    for (Iterator<R> iter = left.iterator(); iter.hasNext();) {
      result = result.append(new Pair<T>((T)iter.next()));
    }    
    return result;
  }
 
  /**
   * Remove an object from a list.
   *
   * @param o The object to remove.
   * @param left The list to remove an object from.
   * @return The resulted list.
   */
  static public final <T> Pair<T> removeFromPair(Object o, Pair<T> left) {
    if (null == left) return null;
    Pair<T> result = Pair.empty();
   
    for (Iterator<T> iter = left.iterator(); iter.hasNext();) {
      T t = iter.next();
      if (o.equals(t)) continue;
      result = result.append(new Pair<T>(t));
    }
    
    return result;
  }
 

  // -------------------------------------------------------------------------

  /** Determine the set union of two lists. */
  @SuppressWarnings("unchecked")
  public static final Function.F2<Pair,Pair,Pair> union =
    new Function.F2<Pair,Pair,Pair>() {
    public Pair apply(Pair list1, Pair list2) {
      return (null == list1) || (null == list2) ? null : list1.combine(list2);
    }
  };

  /** Determine the set union of two lists. */
  public static final class Union<T>
    implements Function.F2<Pair<T>,Pair<T>,Pair<T>> {
    public Pair<T> apply(Pair<T> list1, Pair<T> list2) {
      return (null == list1) || (null == list2) ? null : list1.combine(list2);
    }
  }

  /** 
   * Magic cast wrapper for Primitives.union
   *
   * @param l The left pair.
   * @param r The right pair.
   * @return The union of the two pairs.
   */ 
  @SuppressWarnings("unchecked")
  public static final <T> Pair<T> wrapUnion(Pair<T> l, Pair<T> r) {
    return Primitives.union.apply(l,r);
  }    

  // -------------------------------------------------------------------------

  /** Determine the set intersection of two lists. */
  @SuppressWarnings("unchecked")
  public static final Function.F2<Pair,Pair,Pair> intersection =
    new Function.F2<Pair,Pair,Pair>() {
    public Pair apply(Pair list1, Pair list2) {
      return (null == list1) || (null == list2) ? null : list1.intersect(list2);
    }
  };

  /** Determine the set intersection of two lists. */
  public static final class Intersection<T>
    implements Function.F2<Pair<T>,Pair<T>,Pair<T>> {
    public Pair<T> apply(Pair<T> list1, Pair<T> list2) {
      return (null == list1) || (null == list2) ? null : list1.intersect(list2);
    }
  }

  // -------------------------------------------------------------------------

  /** Determine the set subtraction of two lists. */
  @SuppressWarnings("unchecked")
  public static final Function.F2<Pair,Pair,Pair> subtraction =
    new Function.F2<Pair,Pair,Pair>() {
    public Pair apply(Pair list1, Pair list2) {
      return (null == list1) || (null == list2) ? null : list1.subtract(list2);
    }
  };

  /** Determine the set subtraction of two lists. */
  public static final class Subtraction<T>
    implements Function.F2<Pair<T>,Pair<T>,Pair<T>> {
    public Pair<T> apply(Pair<T> list1, Pair<T> list2) {
      return (null == list1) || (null == list2) ? null : list1.subtract(list2);
    }
  }

  // =========================================================================
  //                            String operations
  // =========================================================================

  /** Concatenate two strings. */
  public static final Function.F2<String,String,String> concat =
    new Function.F2<String,String,String>() {
    public String apply(String s1, String s2) {
      return (null == s1) || (null == s2) ? null : s1 + s2;
    }
  };

  // -------------------------------------------------------------------------

  /** Get a string's size */
  public static final Function.F1<BigInteger,String> ssize =
    new Function.F1<BigInteger,String>() {
    public BigInteger apply(String s) {
      return null == s ? null : BigInteger.valueOf(s.length());
    }
  };

  // -------------------------------------------------------------------------

  /** Convert a string to a big integer. */
  public static final Function.F2<BigInteger,String, BigInteger> stoi =
    new Function.F2<BigInteger,String, BigInteger>() {
    public BigInteger apply(String s, BigInteger radix) {
      return null == s ? null : new BigInteger(s, radix.intValue());
    }
  };

  // -------------------------------------------------------------------------
  
  /** Check if the first string starts with the second. */
  public static final Function.F2<Boolean,String,String> startsWith =
    new Function.F2<Boolean,String, String>() {
    public Boolean apply (String s, String prefix) {
      return (null == s) || (null == prefix) ? null : s.startsWith(prefix);
    }
  };
  
  // -------------------------------------------------------------------------

  /** Check if the first string starts with the second. */
  public static final Function.F2<Boolean,String,String> startsWithi =
    new Function.F2<Boolean,String, String>() {
    public Boolean apply (String s, String prefix) {
      return (null == s) || (null == prefix) ? null : 
      s.toLowerCase().startsWith(prefix.toLowerCase());
    }
  };

  // -------------------------------------------------------------------------

  /** Check if the first string ends with the second. */
  public static final Function.F2<Boolean,String,String> endsWith =
    new Function.F2<Boolean,String, String>() {
    public Boolean apply (String s, String suffix) {
      return (null == s) || (null == suffix) ? null : s.endsWith(suffix);
    }
  };

  // -------------------------------------------------------------------------

  /** Check if the first string ends with - ignoring case */
  public static final Function.F2<Boolean,String,String> endsWithi =
    new Function.F2<Boolean,String, String>() {
    public Boolean apply (String s, String suffix) {
      return (null == s) || (null == suffix) ? null :
      s.toLowerCase().endsWith(suffix.toLowerCase());
    }
  };
  
  // -------------------------------------------------------------------------

  /** Join strings */
  public static final Function.F1<String, Pair<String>> joinStrings =
    new Function.F1<String, Pair<String>>() {
    public String apply(Pair<String> slist) {
      if (null == slist) return null;
   
      String result = "";
      for (Iterator<String> iter = slist.iterator(); iter.hasNext();) {
        result += iter.next();
      }
      return result.trim();
    }
  };
 
  // -------------------------------------------------------------------------

  /** Get the substrng starting at the specified index */
  public static final Function.F2<String,String,BigInteger> substring =
    new Function.F2<String, String, BigInteger>() {
    public String apply (String s, BigInteger i) {
      return (null == s) || (null == i) ? null : s.substring(i.intValue());
    }
  };

  // -------------------------------------------------------------------------

  /** Get the substrng at the specified range */
  public static final 
    Function.F3<String,String,BigInteger,BigInteger> substring2 =
    new Function.F3<String, String, BigInteger, BigInteger>() {
    public String apply (String s, BigInteger start, BigInteger end) {
      return (null == s) || (null == start) || (null == end) ? null
      : s.substring(start.intValue(), end.intValue());
    }
  };

  // -------------------------------------------------------------------------

  /** Convert a string to a double. */
  public static final Function.F1<Double,String> stof =
    new Function.F1<Double,String>() {
    public Double apply(String s) {
      return null == s ? null : Double.valueOf(s);
    }
  };

  /** Convert an interger to a string. */
  public static final Function.F1<String,BigInteger> itos =
    new Function.F1<String, BigInteger>() {
    public String apply(BigInteger i) {
      return null == i ? null : i.toString();
    }
  };

  /** Convert a float to a string. */
  public static final Function.F1<String,Double> ftos =
    new Function.F1<String, Double>() {
    public String apply(Double f) {
      return null == f ? null : f.toString();
    }
  };

  // =========================================================================
  //                             Nonce operations
  // =========================================================================

  /** The nonce counter. */
  protected static BigInteger nonceCounter = BigInteger.ZERO;

  /** Create a nonce. The implementation is not thread-safe. */
  public static final Function.F0<BigInteger> nonce =
    new Function.F0<BigInteger>() {
    public BigInteger apply() {
      nonceCounter = nonceCounter.add(BigInteger.ONE);
      return nonceCounter;
    }
  };
  
  // =========================================================================
  //                            Debug operations
  // =========================================================================

  /** Trace a value. */
  public static final Function.F1<Object,Object> trace =
    new Function.F1<Object,Object>() {
    public Object apply(Object o) {
      trace("", o);
      return o;
    }
  };

  /** Trace a value. */
  public static final class Trace<T> implements Function.F1<T,T> {
    public T apply(T val) {
      trace("", val);
      return val;
    }
  }

  // -------------------------------------------------------------------------

  /** Trace a value. */
  public static final Function.F2<Object,String,Object> trace2 =
    new Function.F2<Object,String,Object>() {
    public Object apply(String msg, Object o) {
      trace(msg, o);
      return o;
    }
  };

  /** Trace a value. */
  public static final class Trace2<T> implements Function.F2<T,String,T> {
    public T apply(String msg, T val) {
      trace(msg, val);
      return val;
    }
  }
  
  // =========================================================================
  //                  node manipulation functions
  // =========================================================================
  
  /** The typical function for annotating nodes. */
  public static final Function.F3<Object, Node, String, Object> annotate = 
    new Function.F3<Object, Node, String, Object>() {   
      public final Object apply(Node n, String name, Object annotation) {
        if ((null == n) || (null == name) || (null == annotation)) return null;
        n.setProperty(name, annotation);
        return annotation;
      }
    };

  // -------------------------------------------------------------------------

  /** The typical function for annotating lists of nodes. */
  public static final Function.F3<Object,Pair<Node>,String,Object>
    annotateList =
   new Function.F3<Object,Pair<Node>,String,Object>() {
     public final Object apply(Pair<Node> p, String name, Object annotation) {
       for (Iterator<Node> iter = p.iterator(); iter.hasNext();) {
         Node n = iter.next();
         if (null == n) {
           continue;
         }
         n.setProperty(name, annotation);
       }      
       return annotation;
     }
   };
    
  // -------------------------------------------------------------------------

  /** The typical function for getting node annotation  */
  public static final Function.F2<Object, Node, String> getAnnotation = 
    new Function.F2<Object, Node, String>() {
    public final Object apply(Node n, String name) {
      if ((null == n) || (null == name)) return null;
      return n.hasProperty(name) ? n.getProperty(name) : null;
    }
  };

  // -------------------------------------------------------------------------

  /** The typical function for checking if a node has an annotation  */
  public static final Function.F2<Boolean, Node, String> hasAnnotation =
    new Function.F2<Boolean, Node, String>() {
    public final Boolean apply(Node n, String name) {
      return (null == n) || (null == name) ? null : n.hasProperty(name);
    }
  };

  // -------------------------------------------------------------------------

  
  /** The function to get the name of a node. */
  public static final Function.F1<String, Object> node_name = 
    new Function.F1<String, Object>(){ 
      public final String apply(Object n) {
        if (null == n) {
          return "?";
        }
        if (!(n instanceof Node)) {
          throw new IllegalStateException("calling nodeName on non-node");
        }
        return ((Node)n).getName();
      }
    };
   
  // =========================================================================
  //                            Map operations
  // =========================================================================
  /** A get function to access the hash table. */ 
  public static final Function.F2<Object, Object, Hashtable<Object, Object>> 
    get = new Function.F2<Object, Object, Hashtable<Object, Object>>() {
      public final Object apply(Object o, 
                                Hashtable<Object, Object> hashTable) {
        return null == o ? null : hashTable.get(o);
      }
    };

  /** A put function to access the hash table. */
  public static final Function.F3<Void, Object, Object, 
                              Hashtable<Object, Object>> put =
    new Function.F3<Void, Object, Object, Hashtable<Object, Object>>() {
      public final Void apply(Object o, Object ob, 
                              Hashtable<Object, Object> hashTable) {
        if (null == o || null == ob) return null;
        hashTable.put(o,ob);
        return null;
      }
    };  

  // --------------------------------------------------------------------------
  
  /**
   * Get children of a node by indice.
   * 
   * @param n The node to get children.
   * @param from The start index.
   * @param to The end index.
   * @return null if the indice are out of range, otherwise return a 
   *   pair of nodes.
   */  
  @SuppressWarnings("unchecked")
  public static final <T> Pair<T> getChildren(Node n, int from, int to) {
    if (to > n.size()) {
      throw new RuntimeException("bad index for get children");
    }
    
    Pair<T> p = Pair.empty();
    
    if (n.size() > from) {
      p = new Pair<T>((T)n.get(from));
    }
    
    for (int i = from + 1; i < to; i++) {
      p = p.append(new Pair<T>((T)n.get(i)));
    }
 
    return p;
  } 

  // =========================================================================
  //                            Primitive tests and name translation
  // =========================================================================
  
  /** The names of functions that are primitive functions. */
  private static Set<String> PRIMITIVE_FUNCTIONS;

  static {
    PRIMITIVE_FUNCTIONS = new HashSet<String>();
    
    PRIMITIVE_FUNCTIONS.add("nonce");
    PRIMITIVE_FUNCTIONS.add("isBottom");
    PRIMITIVE_FUNCTIONS.add("isNotBottom");
    PRIMITIVE_FUNCTIONS.add("negateInt");
    PRIMITIVE_FUNCTIONS.add("negateFloat64");
    PRIMITIVE_FUNCTIONS.add("negateBits");
    PRIMITIVE_FUNCTIONS.add("andBits");
    PRIMITIVE_FUNCTIONS.add("orBits");
    PRIMITIVE_FUNCTIONS.add("xorBits");
    PRIMITIVE_FUNCTIONS.add("shiftLeft");
    PRIMITIVE_FUNCTIONS.add("shiftRight");
    PRIMITIVE_FUNCTIONS.add("cons");
    PRIMITIVE_FUNCTIONS.add("isEmpty");
    PRIMITIVE_FUNCTIONS.add("head");
    PRIMITIVE_FUNCTIONS.add("tail");
    PRIMITIVE_FUNCTIONS.add("length");
    PRIMITIVE_FUNCTIONS.add("nth");
    PRIMITIVE_FUNCTIONS.add("containts");
    PRIMITIVE_FUNCTIONS.add("exists");
    PRIMITIVE_FUNCTIONS.add("append");
    PRIMITIVE_FUNCTIONS.add("union");
    PRIMITIVE_FUNCTIONS.add("intersection");
    PRIMITIVE_FUNCTIONS.add("subtraction");
    PRIMITIVE_FUNCTIONS.add("concat");
    PRIMITIVE_FUNCTIONS.add("stoi");
    PRIMITIVE_FUNCTIONS.add("stof");
    PRIMITIVE_FUNCTIONS.add("trace");
    PRIMITIVE_FUNCTIONS.add("trace2");
    PRIMITIVE_FUNCTIONS.add("ssize");
    PRIMITIVE_FUNCTIONS.add("ftoi");
    PRIMITIVE_FUNCTIONS.add("joinStrings");
    PRIMITIVE_FUNCTIONS.add("absInt");
    PRIMITIVE_FUNCTIONS.add("absFloat64");
    PRIMITIVE_FUNCTIONS.add("startsWith");
    PRIMITIVE_FUNCTIONS.add("startsWithi");
    PRIMITIVE_FUNCTIONS.add("endsWith");
    PRIMITIVE_FUNCTIONS.add("endsWithi");
    PRIMITIVE_FUNCTIONS.add("substring");
    PRIMITIVE_FUNCTIONS.add("substring2");
    PRIMITIVE_FUNCTIONS.add("iter");
    PRIMITIVE_FUNCTIONS.add("foldl");
    PRIMITIVE_FUNCTIONS.add("annotate");
    PRIMITIVE_FUNCTIONS.add("annotateList");
    PRIMITIVE_FUNCTIONS.add("node_name");
    PRIMITIVE_FUNCTIONS.add("getAnnotation");
    PRIMITIVE_FUNCTIONS.add("hasAnnotation");
    PRIMITIVE_FUNCTIONS.add("map");
  }  
  
  /**
   * Check if a function is a primitive function
   *
   * @param name The function name
   * @return true or false
   */
  public static boolean isPrimitive(String name){
    return PRIMITIVE_FUNCTIONS.contains(name);    
  }  

  /** The mapping from Typical primitives to actual primitive names. */
  private static java.util.Map<String,String> PRIMITIVE_NAMES;

  static {
    PRIMITIVE_NAMES = new HashMap<String,String>();

    PRIMITIVE_NAMES.put("is_bottom", "isBottom");
    PRIMITIVE_NAMES.put("is_not_bottom", "isNotBottom");
    PRIMITIVE_NAMES.put("negate_int", "negateInt");
    PRIMITIVE_NAMES.put("negate_float", "negateFloat64");
    PRIMITIVE_NAMES.put("negate_bits", "negateBits");
    PRIMITIVE_NAMES.put("and_bits", "andBits");
    PRIMITIVE_NAMES.put("or_bits", "orBits");
    PRIMITIVE_NAMES.put("xor_bits", "xorBits");
    PRIMITIVE_NAMES.put("shift_left", "shiftLeft");
    PRIMITIVE_NAMES.put("shift_right", "shiftRight");
    PRIMITIVE_NAMES.put("is_empty", "isEmpty");
    PRIMITIVE_NAMES.put("mem", "contains");
    PRIMITIVE_NAMES.put("not_bottom", "notBottom");
    PRIMITIVE_NAMES.put("is_defined", "isDefined");
    PRIMITIVE_NAMES.put("lookup_locally", "lookupLocally");
    PRIMITIVE_NAMES.put("is_defined_locally", "isDefinedLocally");
    PRIMITIVE_NAMES.put("lookup_node", "lookupNode");
    PRIMITIVE_NAMES.put("define_node", "defineNode");
    PRIMITIVE_NAMES.put("annotate_list", "annotateList");
    PRIMITIVE_NAMES.put("has_annotation", "hasAnnotation");
    PRIMITIVE_NAMES.put("get_annotation", "getAnnotation");
    PRIMITIVE_NAMES.put("fresh_name", "freshName");
    PRIMITIVE_NAMES.put("remove_last", "removeLast");
    PRIMITIVE_NAMES.put("abs_int", "absInt");
    PRIMITIVE_NAMES.put("abs_float", "absFloat64");
    PRIMITIVE_NAMES.put("join_strings", "joinStrings");
    PRIMITIVE_NAMES.put("starts_with", "startsWith");
    PRIMITIVE_NAMES.put("starts_withi", "startsWithi");
    PRIMITIVE_NAMES.put("ends_with", "endsWith");
    PRIMITIVE_NAMES.put("ends_withi", "endsWithi");
    PRIMITIVE_NAMES.put("node_type", "nodeType");
    PRIMITIVE_NAMES.put("is_big_endian", "IS_BIG_ENDIAN");
    PRIMITIVE_NAMES.put("void_size", "VOID_SIZE");
    PRIMITIVE_NAMES.put("bool_size", "BOOL_SIZE");
    PRIMITIVE_NAMES.put("pointer_size", "POINTER_SIZE");
    PRIMITIVE_NAMES.put("pointer_align", "POINTER_ALIGN");
    PRIMITIVE_NAMES.put("pointer_nat_align", "POINTER_NAT_ALIGN");
    PRIMITIVE_NAMES.put("ptrdiff_rank", "PTRDIFF_RANK");
    PRIMITIVE_NAMES.put("sizeof_rank", "SIZEOF_RANK");
    PRIMITIVE_NAMES.put("ptrdiff_size", "PTRDIFF_SIZE");
    PRIMITIVE_NAMES.put("sizeof_size", "SIZEOF_SIZE");
    PRIMITIVE_NAMES.put("array_max", "ARRAY_MAX");
    PRIMITIVE_NAMES.put("is_char_signed", "IS_CHAR_SIGNED");
    PRIMITIVE_NAMES.put("char_bits", "CHAR_BITS");
    PRIMITIVE_NAMES.put("char_min", "CHAR_MIN");
    PRIMITIVE_NAMES.put("char_max", "CHAR_MAX");
    PRIMITIVE_NAMES.put("char_mod", "CHAR_MOD");
    PRIMITIVE_NAMES.put("uchar_mac", "UCHAR_MAX");
    PRIMITIVE_NAMES.put("uchar_mod", "UCHAR_MOD");
    PRIMITIVE_NAMES.put("is_wchar_signed", "IS_WCHAR_SIGNED");
    PRIMITIVE_NAMES.put("wchar_rank", "WCHAR_RANK");
    PRIMITIVE_NAMES.put("wchar_size", "WCHAR_SIZE");
    PRIMITIVE_NAMES.put("void_align", "VOID_ALIGN");
    PRIMITIVE_NAMES.put("bool_align", "BOOL_ALIGN");
    PRIMITIVE_NAMES.put("bool_nat_align", "BOOL_NAT_ALIGN");
    PRIMITIVE_NAMES.put("short_size", "SHORT_SIZE");
    PRIMITIVE_NAMES.put("short_align", "SHORT_ALIGN");
    PRIMITIVE_NAMES.put("short_nat_align", "SHORT_NAT_ALIGN");
    PRIMITIVE_NAMES.put("short_bits", "SHORT_BITS");
    PRIMITIVE_NAMES.put("short_min", "SHORT_MIN");
    PRIMITIVE_NAMES.put("short_max", "SHORT_MAX");
    PRIMITIVE_NAMES.put("short_mod", "SHORT_MOD");
    PRIMITIVE_NAMES.put("ushort_max", "USHORT_MAX");
    PRIMITIVE_NAMES.put("ushort_mod", "USHORT_MOD");
    PRIMITIVE_NAMES.put("is_int_signed", "IS_INT_SIGNED");
    PRIMITIVE_NAMES.put("int_size", "INT_SIZE");
    PRIMITIVE_NAMES.put("int_align", "INT_ALIGN");
    PRIMITIVE_NAMES.put("int_nat_align", "INT_NAT_ALIGN");
    PRIMITIVE_NAMES.put("int_bits", "INT_BITS");
    PRIMITIVE_NAMES.put("int_min", "INT_MIN");
    PRIMITIVE_NAMES.put("int_max", "INT_MAX");
    PRIMITIVE_NAMES.put("int_mod", "INT_MOD");
    PRIMITIVE_NAMES.put("uint_max", "UINT_MAX");
    PRIMITIVE_NAMES.put("uint_mod", "UINT_MOD");
    PRIMITIVE_NAMES.put("long_size", "LONG_SIZE");
    PRIMITIVE_NAMES.put("long_align", "LONG_ALIGN");
    PRIMITIVE_NAMES.put("long_nat_align", "LONG_NAT_ALIGN");
    PRIMITIVE_NAMES.put("long_bits", "LONG_BITS");
    PRIMITIVE_NAMES.put("long_min", "LONG_MIN");
    PRIMITIVE_NAMES.put("long_max", "LONG_MAX");
    PRIMITIVE_NAMES.put("long_mod", "LONG_MOD");
    PRIMITIVE_NAMES.put("ulong_max", "ULONG_MAX");
    PRIMITIVE_NAMES.put("ulong_mod", "ULONG_MOD");
    PRIMITIVE_NAMES.put("long_long_size", "LONG_LONG_SIZE");
    PRIMITIVE_NAMES.put("long_long_align", "LONG_LONG_ALIGN");
    PRIMITIVE_NAMES.put("long_long_nat_align", "LONG_LONG_NAT_ALIGN");
    PRIMITIVE_NAMES.put("long_long_bits", "LONG_LONG_BITS");
    PRIMITIVE_NAMES.put("long_long_min", "LONG_LONG_MIN");
    PRIMITIVE_NAMES.put("long_long_max", "LONG_LONG_MAX");
    PRIMITIVE_NAMES.put("long_long_mod", "LONG_LONG_MOD");
    PRIMITIVE_NAMES.put("ulong_long_max", "ULONG_LONG_MAX");
    PRIMITIVE_NAMES.put("ulong_long_mod", "ULONG_LONG_MOD");
    PRIMITIVE_NAMES.put("float_size", "FLOAT_SIZE");
    PRIMITIVE_NAMES.put("float_align", "FLOAT_ALIGN");
    PRIMITIVE_NAMES.put("float_nat_align", "FLOAT_NAT_ALIGN");
    PRIMITIVE_NAMES.put("double_size", "DOUBLE_SIZE");
    PRIMITIVE_NAMES.put("double_align", "DOUBLE_ALIGN");
    PRIMITIVE_NAMES.put("double_nat_align", "DOUBLE_NAT_ALIGN");
    PRIMITIVE_NAMES.put("long_double_size", "LONG_DOUBLE_SIZE");
    PRIMITIVE_NAMES.put("long_double_align", "LONG_DOUBLE_ALIGN");
    PRIMITIVE_NAMES.put("long_double_nat_align", "LONG_DOUBLE_NAT_ALIGN");
    PRIMITIVE_NAMES.put("function_align", "FUNCTION_ALIGN");
    PRIMITIVE_NAMES.put("function_size", "FUNCTION_SIZE");
  }

  /**
   * Convert a name from ML style to Java style.
   * 
   * @param name The name to convert.
   * @return The converted name.
   */
  public static String convertName(String name){
    return PRIMITIVE_NAMES.containsKey(name) ? PRIMITIVE_NAMES.get(name) : name;    
  }

  /** The names of types that are also integers. */
  private static Set<String> INTEGER_TYPES;

  static {
    INTEGER_TYPES = new HashSet<String>();
    
    INTEGER_TYPES.add("VOID_SIZE");
    INTEGER_TYPES.add("POINTER_SIZE");
    INTEGER_TYPES.add("POINTER_ALIGN");
    INTEGER_TYPES.add("POINTER_NAT_ALIGN");
    INTEGER_TYPES.add("PTRDIFF_RANK");
    INTEGER_TYPES.add("SIZEOF_RANK");
    INTEGER_TYPES.add("PTRDIFF_SIZE");
    INTEGER_TYPES.add("SIZEOF_SIZE");
    INTEGER_TYPES.add("BOOL_SIZE");
    INTEGER_TYPES.add("BOOL_ALIGN");
    INTEGER_TYPES.add("BOOL_NAT_ALIGN");
    INTEGER_TYPES.add("CHAR_BITS");
    INTEGER_TYPES.add("WCHAR_RANK");
    INTEGER_TYPES.add("WCHAR_SIZE");
    INTEGER_TYPES.add("SHORT_SIZE");
    INTEGER_TYPES.add("SHORT_ALIGN");
    INTEGER_TYPES.add("SHORT_NAT_ALIGN");
    INTEGER_TYPES.add("SHORT_BITS");
    INTEGER_TYPES.add("INT_SIZE");
    INTEGER_TYPES.add("INT_ALIGN");
    INTEGER_TYPES.add("INT_NAT_ALIGN");
    INTEGER_TYPES.add("INT_BITS");
    INTEGER_TYPES.add("LONG_SIZE");
    INTEGER_TYPES.add("LONG_ALIGN");
    INTEGER_TYPES.add("LONG_NAT_ALIGN");
    INTEGER_TYPES.add("LONG_BITS");
    INTEGER_TYPES.add("LONG_LONG_SIZE");
    INTEGER_TYPES.add("LONG_LONG_ALIGN");
    INTEGER_TYPES.add("LONG_LONG_NAT_ALIGN");
    INTEGER_TYPES.add("LONG_LONG_BITS");
    INTEGER_TYPES.add("FLOAT_SIZE");
    INTEGER_TYPES.add("FLOAT_ALIGN");
    INTEGER_TYPES.add("FLOAT_NAT_ALIGN");
    INTEGER_TYPES.add("DOUBLE_SIZE");
    INTEGER_TYPES.add("DOUBLE_ALIGN");
    INTEGER_TYPES.add("DOUBLE_NAT_ALIGN");
    INTEGER_TYPES.add("LONG_DOUBLE_SIZE");
    INTEGER_TYPES.add("LONG_DOUBLE_ALIGN");
    INTEGER_TYPES.add("LONG_DOUBLE_NAT_ALIGN");
    INTEGER_TYPES.add("FUNCTION_SIZE");
    INTEGER_TYPES.add("FUNCTION_ALIGN");
  }
  
  /**
   * Check if a machine dependent constant has integer type
   * 
   * @param name The name of the constant
   * @return <code>true</code> if that constant has integer type, 
   *   otherwise <code>false</code>
   */
  public static boolean hasIntegerType(String name) {
    return INTEGER_TYPES.contains(name);    
  }
      
 } 
