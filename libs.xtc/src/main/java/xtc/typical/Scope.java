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

import xtc.tree.Node;
 
import xtc.util.Pair;

/**
 * The predefined scope structure.
 *
 * @author Anh Le
 * @version $Revision: 1.4 $
 */

public class Scope extends Variant<Tuple.T2<ScopeKind<?>,Pair<Node>>> {
    
  /**
   * Create a new scope construct.
   *
   * @param member1 The kind of the new scope.
   * @param member2 A list of nodes that have this scope.
   */
  public Scope(ScopeKind<?> member1, Pair<Node> member2) {
    tuple = new Tuple.T2<ScopeKind<?>, Pair<Node>>(member1, member2);
  }    
  
  /** 
   * Test if this constructor is a scope.
   *
   * @return <code>true</code> if scope, false otherwise.
   */
  public boolean isScope() {
    return true;
  }
 
  public String getName() {
    return "Scope";
  }

  public String toString()    {
    return "Scope of " + tuple.toString();
  }

}
