/*
 * xtc - The eXTensible Compiler
 * Copyright (C) 2007 New York University
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
package xtc.typical; 

import xtc.tree.GNode;
import xtc.tree.Node;

import xtc.util.SymbolTable;

/** 
 * Implementation of the let folder, used to collapse a let expression. 
 *
 * @author Anh Le, Laune Harris
 * @version $Revision: 1.1 $
 */
public class LetFolder {

  /** 
   * Create a new let folder. 
   * 
   */
  public LetFolder () { /* Do nothing. */ }
  
  public void collapseLet(GNode n, SymbolTable table) {
    Node bindings = GNode.ensureVariable(n.getGeneric(0));
    Node value = n.getGeneric(1);

    if (value.hasName("LetExpression")) {
      Node vbindings = value.getGeneric(0);
      boolean collapse = true;

      for (int i = 0; i < n.size(); i++) {
        if (n.getGeneric(i).getGeneric(0).hasName("Variable") &&
            containsBinding(vbindings, 
                            n.getGeneric(i).getGeneric(0).getString(0))) {
          collapse = false;
        }
      }
      
      if (collapse) {
        for (int i = 0; i < vbindings.size(); i++) {
          bindings.add(vbindings.getGeneric(i));
        }
        n.set(0, bindings);
        n.set(1, value.getGeneric(1));   
        
        String scopename = (String)value.getProperty("enterScope");
        if (table.current().hasNested(scopename)) {
          table.current().merge(scopename);
        }

        collapseLet(n, table);
      }
    }    
  } 
  
  /**
   * Test if a let binding node contains a named binding.
   *
   * @param n The let binding node.
   * @param binding The name of the binding.
   * @return <code>true</code> if the node contains the binding.
   */
  private boolean containsBinding(Node n, String binding) {
    for (int i = 0; i < n.size(); i++) {
      if (n.getGeneric(i).getGeneric(0).hasName("Variable") &&
          binding.equals(n.getGeneric(i).getGeneric(0).getString(0))) {
        return true;
      }                                                
    }
    return false;    
  }          

} 
