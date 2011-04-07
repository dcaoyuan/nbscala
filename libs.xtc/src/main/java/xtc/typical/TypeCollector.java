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

import xtc.tree.Visitor;
import xtc.tree.GNode;
import xtc.tree.Node; 

/**
 * Visitor to add all types reachable from a given type definition.
 *
 * @author Laune Harris, Anh Le
 * @version $Revision: 1.1 $
 */
public class TypeCollector extends Visitor {
  
  /** Create a new type collector. */
  public TypeCollector() {
    //nothing to do here.
  }

  /**
   * Collect the types in this type definition.
   *
   * @param n The type definition node.
   */
  public void visitTypeDefinition(GNode n) {
    dispatch(n.getGeneric(2));
  }

  /**
   * Collect the types in this attribute definition.
   *
   * @param n The attribute definition node.
   */
  public void visitAttributeDefinition(GNode n) {
    dispatch(n.getGeneric(1));
  }
    
  /**
   * Collect the types in this equal attribute definition.
   *
   * @param n The equal attribute definition node.
   */
  public void visitEqualAttributeDefinition(GNode n) {
    dispatch(n.getGeneric(1));
  }

  /**
   * Collect the types in this record declaration.
   *
   * @param n The record declaration node.
   */
  public void visitRecordDeclaration(GNode n) {
    for (int i = 0; i < n.size(); i++) {
      dispatch(n.getGeneric(i).getGeneric(1));
    }
  }

  /**
   * Collect the types in this constructed type declaration.
   *
   * @param n The constructed type node.
   */
  public void visitConstructedType(GNode n) {
    dispatch(n.getGeneric(0));
  }

  /**
   * Collect the types in this variant declaration declaration.
   *
   * @param n The variant declaration node.
   */
  public void visitVariantDeclaration(GNode n) {
    for (int i = 0; i < n.size(); i++) {
      dispatch(n.getGeneric(i));
    }
  }

  /**
   * Collect the types in this type constructor.
   *
   * @param n The type constructor node.
   */
  public void visitTypeConstructor(GNode n) {
    dispatch(n.getGeneric(1));
  }
  
  /**
   * Collect the types in this tuple type.
   *
   * @param n The tuple type node.
   */
  public void visitTupleType(GNode n) {
    for (int i = 0; i < n.size(); i++) {
      dispatch(n.getGeneric(i));
    }
  }

  /**
   * Collect the types in a namespace definition.
   *
   * @param n The namespace node.
   */
  public void visitNameSpaceDefinition(GNode n) {
    for (int i = 0; i < n.size(); i++) {
      dispatch(n.getGeneric(i));
    }
  }

  /**
   * Collect the type in a other nodes.
   *
   * @param n The node.
   */
  public void visit(GNode n) {
    //nothing to do. 
  }  

}
