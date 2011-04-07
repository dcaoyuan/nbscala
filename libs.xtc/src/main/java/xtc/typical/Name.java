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

import java.util.Iterator;

import xtc.util.Pair;
import xtc.util.SymbolTable;

/**
 * The base class of all NameSpace names.
 *
 * @author Anh Le
 * @version $Revision: 1.6 $
 */
public abstract class Name<T extends Tuple> extends Variant<T> {
    
  /** The tags for subclasses. */
  public static enum Tag {
    SimpleNameT,
    QualifiedNameT
  }

  /** Create a new name. */
  protected Name() {
    //empty
  }

  /**
   * Get the tag of this class
   *
   * @return The tag
   */
  public abstract Tag tag();
    
  /**
   * Tests if this name is simple.
   *
   * @return <code>true</code> if simple, false otherwise.
   */
  public boolean isSimpleName()    {
    return false;
  }
   
  /**
   * Tests if this name is qualified.
   *
   * @return <code>true</code> if simple, false otherwise.
   */
  public boolean isQualifiedName() {
    return false;
  }

  /**
   * Convert the name into a name suitable for symbol table access.
   *
   * @param ns The namespace.
   */
  public abstract String mangle(String ns);

  /** The simple namespace name. */
  public static class SimpleName extends Name<Tuple.T1<String>> {

    /**
     * Create a new simple name.
     * 
     * @param member1 The simple name.
     */
    public SimpleName(String member1)  {
      tuple = new Tuple.T1<String>(member1);
    }

    public final Tag tag() {
      return Tag.SimpleNameT;
    }
   
    public boolean isSimpleName() {
      return true;
    }
    
    public String getName() {
      return "SimpleName";
    }

    public String toString() {
      return "SimpleName of " + tuple.toString();
    }

    public String mangle(String ns) {
      if ("default".equals(ns)) return tuple.get1();
      else return SymbolTable.toNameSpace(tuple.get1(), ns);
    }

  }

  /** The qualified namespace name. */
  public static class QualifiedName extends Name<Tuple.T1<Pair<String>>> {
     
    /**
     * Create a new Qualified name.
     *
     * @param member1 The qualifier.
     */
    public QualifiedName(Pair<String> member1) {
      tuple = new Tuple.T1<Pair<String>>(member1);
    }

    public final Tag tag() {
      return Tag.QualifiedNameT;
    }
    
    public boolean isQualifiedName() {
      return true;
    }

    public String getName() {
      return "QualifiedName";
    }

    public String toString() {
      return "QualifiedName of " + tuple.toString();
    }

    public String mangle(String ns) {
      StringBuilder buf = new StringBuilder();
      
      for (Iterator<String> iter = tuple.get1().iterator(); iter.hasNext(); ) {
        if (iter.hasNext()) {
          buf.append(iter.next());
          buf.append('.');
        } else {
          if (!"default".equals(ns)) {
            buf.append(SymbolTable.toNameSpace(iter.next(), ns));
          } else {
            buf.append(iter.next());
          }           
        }
      }
  
      return buf.toString();
    }
  
  }  

}
