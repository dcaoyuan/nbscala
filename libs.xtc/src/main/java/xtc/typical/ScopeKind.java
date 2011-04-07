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
 * The base class of all scope kinds.
 *
 * @author Anh Le
 * @version $Revision: 1.5 $
 */
public abstract class ScopeKind<T extends Tuple> extends Variant<T> {

  /** The tags for subclasses. */
  public static enum Tag {
    NamedT,
    AnonymousT,
    TemporaryT
  }
    
  /** Create a new scope_kind. */
  protected ScopeKind() {
    //empty
  }

  /**
   * Get the tag of this class
   *
   * @return The tag
   */
  public abstract Tag tag();
    
  /** 
   * Test if this scope is named.
   *
   * @return <code>true</code> if named, false otherwise.
   */
  public boolean isNamed() {
    return false;
  }
    
  /** 
   * Test if this scope is anonymous.
   *
   * @return <code>true</code> if anonymous, false otherwise.
   */
  public boolean isAnonymous() {
    return false;
  }

  /** 
   * Test if this scope is temporary.
   *
   * @return <code>true</code> if temporary, false otherwise.
   */
  public boolean isTemporary() {
    return false;
  }

  /** The named scope. */
  public static class Named extends ScopeKind<Tuple.T1<Name<?>>> {
   
    /**
     * Create a new named scope.
     *
     * @param member1 The name.
     */
    public Named(Name<?> member1) {
      tuple = new Tuple.T1<Name<?>>(member1);
    }

    public final Tag tag() {
      return Tag.NamedT;
    }
    
    public boolean isNamed() {
      return true;
    }

    public String getName() {
      return "Named";
    }
    
    public String toString() {
      return "Named of " + tuple.toString();
    }

  } 

  /** The anonymous scope. */
  public static class Anonymous extends ScopeKind<Tuple.T1<String>> {
    
    /**
     * Create a new anonymous scope.
     *
     * @param member1 A fresh name of the new anonymous scope.
     */
    public Anonymous(String member1) {
      tuple = new Tuple.T1<String>(member1);
    }

    public final Tag tag() {
      return Tag.AnonymousT;
    }
    
    public boolean isAnonymous() {
      return true;
    }
      
    public String getName() {
      return "Anonymous";
    }

    public String toString() {
      return "Anonymous of " + tuple.toString();
    }
    
  } 

  /** The temporary scope. */
  public static class Temporary extends ScopeKind<Tuple.T1<String>> {
    
    /**
     * Create a new temporary scope.
     *
     * @param member1 A fresh name of the new temporary scope.
     */
    public Temporary(String member1) {
      tuple = new Tuple.T1<String>(member1);
    }

    public final Tag tag() {
      return Tag.TemporaryT;
    }
    
    public boolean isTemporary() {
      return true;
    }
   
    public String getName() {
      return "Temporary";
    }

    public String toString() {
      return "Temporary of " + tuple.toString();
    }

  }


}
