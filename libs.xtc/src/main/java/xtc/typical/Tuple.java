/*
 * xtc - The eXTensible Compiler
 * Copyright (C) 2007 New York University
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

/**
 * The superclass of all anonymous tuples.
 *
 * @author Laune Harris
 * @version $Revision: 1.38 $
 */
public abstract class Tuple {

  /** Create a new tuple. */
  public Tuple() { /* Nothing to do. */ }

  /**
   * Get this tuple's size.
   *
   * @return The size.
   */
  public abstract int size();

  // =========================================================================

  /** The canonical empty tuple. */
  public final static T0 EMPTY = new T0();

  /** Get the first element. */
  public <A> A get1() {
    throw new IllegalStateException("Empty tuple");
  }
  
  /** Get the second element. */
  public <B> B get2() {
    throw new IllegalStateException("Empty tuple");
  }

  /** Get the third element. */
  public <C> C get3() {
    throw new IllegalStateException("Empty tuple");
  }
  
  /** Get the fourth element. */
  public <D> D get4() {
    throw new IllegalStateException("Empty tuple");
  }

  /** Get the fifth element. */
  public <E> E get5() {
    throw new IllegalStateException("Empty tuple");
  }

  /** Get the sixth element. */
  public <F> F get6() {
    throw new IllegalStateException("Empty tuple");
  }

  /** Get the seventh element. */
  public <G> G get7() {
    throw new IllegalStateException("Empty tuple");
  }

  /** A tuple with zero members. */
  public static class T0 extends Tuple {
   
    /** Create an empty tuple. */
    public T0() { /* Nothing to do. */ }

    public int size() {
      return 0;
    }

    public int hashCode() {
      return 0;
    }

    public boolean equals(Object o) {
      if (this == o) return true;
      
      return (o instanceof T0);
    }

    public String toString() {
      return "()";
    } 

  }
  
  /** A tuple with one member. */
  public static class T1<A> extends Tuple {
    
    /** The first member. */
    A member1;

    /** Create a new T1. */
    public T1(A m1){
      member1 = m1;
    } 

    public int size() {
      return 1;
    }
    
    @SuppressWarnings("unchecked")
    public A get1() {
      return member1;
    }

    public int hashcode() {
      return member1.hashCode() + 7;
    }
    
    public boolean equals(Object o) {
      if (this == o) return true;
     
      if (o instanceof T1) {
        T1<?> t = (T1<?>)o;
        return member1.equals(t.member1);
      }       
      
      return false;
    }
    
    public String toString() {
      return "(" + toString(member1) + ")";
    }
    
  }
  
  /** A tuple witn 2 members. */
  public static class T2<A,B> extends Tuple {
    
    /** The first member. */ 
    A member1;
    
    /** The second member. */
    B member2;

    /** Create a new T2. */
    public T2(A m1, B m2){
      member1 = m1;
      member2 = m2;
    } 

    public int size() {
      return 2;
    }

    @SuppressWarnings("unchecked")
    public A get1() {
      return member1;
    }

    @SuppressWarnings("unchecked")
    public  B get2() {
      return member2;
    }

    public int hashCode(){
      return member1.hashCode() * member2.hashCode() + 7;
    }
    
    public boolean equals(Object o) {
      if (this == o) return true;
      
      if (this == o) return true;
      
      if (o instanceof T2) {
        T2<?,?> other = (T2<?,?>)o;
        return member1.equals(other.member1) && member2.equals(other.member2);
      }
      return false;
    }
      
    public String toString() {
      return "(" + toString(member1) + "," + toString(member2) + ")";
    }
  }
  
  /** A tuple witn 3 members. */
  public static class T3<A,B,C> extends Tuple{
    
    /** The first member. */
    A member1;
    
    /** The second member. */
    B member2;

    /** The third member. */
    C member3;

    /** Createa a new T3.*/
    public T3(A m1, B m2, C m3){
      member1 = m1;
      member2 = m2;
      member3 = m3;
    } 

    public int size() {
      return 3;
    }

    @SuppressWarnings("unchecked")
    public A get1() {
      return member1;
    }
    
    @SuppressWarnings("unchecked")
    public B get2() {
      return member2;
    }
    
    @SuppressWarnings("unchecked")
    public C get3() { 
      return member3;
    }
    
    public int hashCode(){
      return member1.hashCode() * member2.hashCode() * member3.hashCode() + 7;
    }

    public boolean equals(Object o) {
      if (this == o) return true;
      
      if (o instanceof T3) {
        T3<?,?,?> t = (T3<?,?,?>)o;
        
        return member1.equals(t.get1()) && member2.equals(t.get2()) &&
          member3.equals(t.get3());        
      }
      return false;
    }
        
    public String toString() {
      return "(" + toString(member1) + "," + toString(member2) + 
        "," +  toString(member3) + ")";
    }
  }

  /** A tuple witn 4 members. */
  public static class T4<A,B,C,D> extends Tuple {
    
    /** The first member. */
    A member1;
    
    /** The second member. */
    B member2;

    /** The third member. */
    C member3;
    
    /** The fourth member. */
    D member4;

    /** Create a new T4. */
    public T4(A m1, B m2, C m3, D m4){
      member1 = m1;
      member2 = m2;
      member3 = m3;
      member4 = m4;
    } 

    public int size() {
      return 4;
    }

    @SuppressWarnings("unchecked")
    public A get1() {
      return member1;
    }
    
    @SuppressWarnings("unchecked")
    public B get2() {
      return member2;
    }
    
    @SuppressWarnings("unchecked")
    public C get3() {
      return member3;
    }
    
    @SuppressWarnings("unchecked")
    public D get4() {
      return member4;
    }
    
    public int hashCode(){
      return member1.hashCode() * member2.hashCode() * member3.hashCode() * 
        member4.hashCode() + 7;
    }
    
    public boolean equals(Object o) {
      if (this == o) return true;

      if (o instanceof T4) {
        T4<?,?,?,?> t = (T4<?,?,?,?>)o;
        
        return member1.equals(t.get1()) && member2.equals(t.get2()) && 
          member3.equals(t.get3()) && member4.equals(t.get4());
      }
      
      return false;
    }
    
    public String toString() {
      return "(" + toString(member1) + "," + toString(member2) + "," +
        toString(member3) + "," + toString(member4) + ")";
    }
    
  }

  /**  A tuple witn 5 members. */
  public static class T5<A,B,C,D,E> extends Tuple{
    
    /** The first member. */
    A member1;

    /** The second member. */
    B member2;

    /** The third member. */
    C member3;

    /** The fourth member. */
    D member4;

    /** The fifth member. */
    E member5;

    /** Create a new T5. */
    public T5(A m1, B m2, C m3, D m4, E m5){
      member1 = m1;
      member2 = m2;
      member3 = m3;
      member4 = m4;
      member5 = m5;
    } 

    public int size() {
      return 5;
    }

    @SuppressWarnings("unchecked")
    public A get1() {
      return member1;
    }
    
    @SuppressWarnings("unchecked")
    public B get2() {
      return member2;
    }
    
    @SuppressWarnings("unchecked")
    public C get3() {
      return member3;
    }
    
    @SuppressWarnings("unchecked")
    public D get4() {
      return member4;
    }

    @SuppressWarnings("unchecked")
    public E get5() {
      return member5;
    }

    public int hashCode(){
      return member1.hashCode() * member2.hashCode() * member3.hashCode() * 
        member4.hashCode() * member5.hashCode() + 7;
    }

    public boolean equals(Object o) {
      if (this == o) return true;

      if (o instanceof T5) {
        T5<?,?,?,?,?> t = (T5<?,?,?,?,?>)o;
        
        return member1.equals(t.get1()) && member2.equals(t.get2()) &&
          member3.equals(t.get3()) && member4.equals(t.get4()) &&
          member5.equals(t.get5());
      }

      return false;
    }

    public String toString() {
      return "(" + toString(member1) + "," + toString(member2) + "," +
        toString(member3) + "," + toString(member4) + "," + 
        toString(member5) + ")";
    }
    
  }
  
  /** A tuple with 6 members. */
  @SuppressWarnings ("unchecked")
  public static class T6<A,B,C,D,E,F> extends Tuple {
    
    /** The first member. */
    A member1;
    
    /** The second member. */
    B member2;

    /** The third member. */
    C member3;

    /** The fourth member. */
    D member4;

    /** The fifth member. */
    E member5;

    /** The sixth member. */
    F member6;
    
    /** Create a new T6. */
    public T6(A m1, B m2, C m3, D m4, E m5, F m6){
      member1 = m1;
      member2 = m2;
      member3 = m3;
      member4 = m4;
      member5 = m5;
      member6 = m6;
    } 

    public int size() {
      return 6;
    }

    public A get1() {
      return member1;
    }
    
    public B get2() {
      return member2;
    }
    
    public C get3() {
      return member3;
    }
    
    public D get4() {
      return member4;
    }

    public E get5() {
      return member5;
    }
    
    public F get6() {
      return member6;
    }
    
    public int hashCode(){
      return member1.hashCode() * member2.hashCode() * member3.hashCode() * 
        member4.hashCode() * member5.hashCode() * member6.hashCode() + 7;
    }
    
    public boolean equals(Object o) {
      if (this == o) return true;
                 
      if (o instanceof T6) {
        T6<?,?,?,?,?,?> t = (T6<?,?,?,?,?,?>)o;
        
        return member1.equals(t.get1()) && member2.equals(t.get2()) &&
          member3.equals(t.get3()) && member4.equals(t.get4()) &&
          member5.equals(t.get5()) && member6.equals(t.get6());
      }
      return false;
    }
    
    public String toString() {
      return "(" + toString(member1) + "," + toString(member2) + "," + 
        toString(member3) + "," + toString(member4) +  "," + 
        toString(member5) + "," + toString(member6) + ")";
    }
  }
 
  /** A tuple with 7 members. */
  @SuppressWarnings ("unchecked")
  public static class T7<A,B,C,D,E,F,G> extends Tuple {
    
    /** The first member. */
    A member1;

    /** The second member. */
    B member2;

    /** The third member. */
    C member3;

    /** The fourth member. */
    D member4;
    
    /** The fifth member. */
    E member5;

    /** The sixth member. */
    F member6;

    /** The seventh member. */
    G member7;
    
    /** Create a new T7. */
    public T7(A m1, B m2, C m3, D m4, E m5, F m6, G m7){
      member1 = m1;
      member2 = m2;
      member3 = m3;
      member4 = m4;
      member5 = m5;
      member6 = m6;
      member7 = m7;
    } 

    public int size() {
      return 7;
    }
    
    public A get1() {
      return member1;
    }
    
    public B get2() {
      return member2;
    }
    
    public C get3() {
      return member3;
    }
    
    public D get4() {
      return member4;
    }

    public E get5() {
      return member5;
    }
    
    public F get6() {
      return member6;
    }
    
    public G get7() {
      return member7;
    }
    
    public int hashCode(){
      return member1.hashCode() * member2.hashCode() * member3.hashCode() * 
        member4.hashCode() * member5.hashCode() * member7.hashCode() + 7;
    }

    public boolean equals(Object o) {
      if (o instanceof T7) {
        T7<?,?,?,?,?,?,?> t = (T7<?,?,?,?,?,?,?>)o;
        
        return member1.equals(t.get1()) && member2.equals(t.get2()) &&
          member3.equals(t.get3()) && member4.equals(t.get4()) &&
          member5.equals(t.get5()) && member6.equals(t.get6()) &&
          member7.equals(t.get7());        
      }
      
      return false;
    }

    public String toString() {
      return "(" + toString(member1) + "," + toString(member2) + "," + 
        toString(member3) + "," + toString(member4) +  "," + 
        toString(member5) + "," + toString(member6) + "," + 
        toString(member7) + ")";
    }
  }

  /**
   * Print an object as a string.
   *
   * @param o The object.
   * @return The corresponding string.
   */
  public final static String toString(Object o) {
    return (null == o) ? "?" : o.toString();
  }
    
}
