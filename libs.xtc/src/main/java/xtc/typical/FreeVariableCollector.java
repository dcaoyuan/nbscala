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

import java.util.Set;
import java.util.HashSet;

import java.util.List;
import java.util.ArrayList;

import xtc.tree.Visitor;
import xtc.tree.GNode;
import xtc.tree.Node;

/** 
 * Visitor to add all find all free identifiers in an guard expression. 
 * This might need some refinement and synchronisation with the symbol 
 * table. 
 *
 * @author Anh Le, Laune Harris
 * @version $Revision: 1.2 $
 */
public class FreeVariableCollector extends Visitor {
    
  /* The list of identifiers. */
  protected final Set<String> idlist;
   
  /** 
   * Create a new free variable collector. 
   *
   * @param n The root of the expression.
   */
  public FreeVariableCollector(GNode n) {
    idlist = new HashSet<String>();
    dispatch(n);
  }
  
  /** 
   * Create a new free variable collector. 
   *
   * @param n The root of the expression.
   * @param idlist The current idlist.
   */
  public FreeVariableCollector(GNode n, Set<String> idlist) {
    this.idlist = idlist;
    dispatch(n);
  }
    
  /**
   * Get the list of identifiers in this guard expression. 
   *
   * @return The list of identifiers.
   */
  public Set<String> getIdentifiers() {
    return idlist;
  }
    
  /** 
   * Get the identifers in a guard expression. 
   *
   * @param n The node.
   */
  public void visitGuardExpression(GNode n) {
    dispatch(n.getGeneric(0));
  }
    
  /** 
   * Get the identifiers in a tuple literal.
   *
   * @param n The node.
   */
  public void visitTupleLiteral(GNode n) {
    for (int i = 0; i < n.size(); i++) 
      dispatch(n.getGeneric(i));
  }
    
  /** 
   * Get the identifiers in a logical or expression. 
   *
   * @param n The node.
   */
  public void visitLogicalOrExpression(GNode n) {
    dispatch(n.getGeneric(0));
    dispatch(n.getGeneric(2));
  }
    
  /** 
   * Get the identifiers in a logical and expression. 
   *
   * @param n The node.
   */    
  public void visitLogicalAndExpression(GNode n) {
    dispatch(n.getGeneric(0));
    dispatch(n.getGeneric(2));
  }
    
  /** 
   * Get the identifiers in an equality expression. 
   *
   * @param n The node.
   */    
  public void visitEqualityExpression(GNode n) {
    dispatch(n.getGeneric(0));
    dispatch(n.getGeneric(2));
  }
    
  /** 
   * Get the identifiers in a relational expression. 
   *
   * @param n The node.
   */    
  public void visitRelationalExpression(GNode n) {
    dispatch(n.getGeneric(0));
    dispatch(n.getGeneric(2));
  }
    
  /** 
   * Get the identifiers in an additive expression. 
   *
   * @param n The node.
   */
  public void visitAdditiveExpression(GNode n) {
    dispatch(n.getGeneric(0));
    dispatch(n.getGeneric(2));
  }
    
  /** 
   * Get the identifiers in an concatenation expression. 
   *
   * @param n The node.
   */
  public void visitConcatenationExpression(GNode n) {
    dispatch(n.getGeneric(0));
    dispatch(n.getGeneric(2));
  }
    
  /** 
   * Get the identifiers in a multiplicative expression. 
   *
   * @param n The node.
   */
  public void visitMultiplicativeExpresion(GNode n) {
    dispatch(n.getGeneric(0));
    dispatch(n.getGeneric(2));
  }
    
  /** 
   * Get the identifiers in a cons expression. 
   *
   * @param n The node.
   */
  public void visitConsExpression(GNode n) {
    dispatch(n.getGeneric(0));
    dispatch(n.getGeneric(1));
  }

  /** 
   * Get the identifers in a function application. 
   *
   * @param n The node.
   */
  public void visitFunctionApplication(GNode n) {
    int size = n.size();
    if (3 == size) dispatch(n.getGeneric(2));
    else {
      dispatch(n.getGeneric(0));
      dispatch(n.getGeneric(1));
    }      
  }
   
  /** 
   * Get the identifers in an argument list. 
   *
   * @param n The node.
   */
  public void visitArguments(GNode n) {
    for (int i = 0; i < n.size(); i++) 
      dispatch(n.getGeneric(i));
  }
    
  /** 
   * Get the identifiers in a predicate expression. 
   *
   * @param n The node.
   */
  public void visitPredicateExpression(GNode n) {
    dispatch(n.getGeneric(1));
  }
    
  /** Get the identifiers in a field expression. */
  public void visitFieldExpression(GNode n) {
    dispatch(n.getGeneric(0));      
  }     
    
  /** 
   * Get the identifiers in a logicalnegation expression. 
   *
   * @param n The node.
   */
  public void visitLogicalNegationExpression(GNode n) {
    dispatch(n.getGeneric(0));
  }
    
  /** 
   * Get the identifiers in a let expression. 
   *
   * @param n The node.
   */
  public void visitLetExpression(GNode n) {
    // Get id in the expression  
    Set<String> ids1 = 
      new FreeVariableCollector(n.getGeneric(1)).getIdentifiers();
    
    // Remove the bound ids
    Set<String> ids2 = 
      new FreeVariableCollector(n.getGeneric(0),ids1).getIdentifiers();
      
    idlist.addAll(ids2);    
  }
    
  /** 
   * Get (bound) identifiers in let bindings. 
   *
   * @param n The node.
   */
  public void visitLetBindings(GNode n) {
    for (int i = 0; i < n.size(); i++) {
      dispatch(n.getGeneric(i));
    }
  }
    
  /** 
   * Get bound identifier in a let binding. 
   *
   * @param n The node.
   */
  public void visitLetBinding(GNode n) {
    Node var = n.getGeneric(0);
    if ("Variable".equals(var.getName())) idlist.remove(var.getString(0));
  }
  
  /** 
   * Get identifiers in a function expression. 
   *
   * @param n The node.
   */
  public void visitFunctionExpression(GNode n) {
    dispatch(n.getGeneric(0));
  }
  
  /** 
   * Get identifiers in a match expression. 
   *
   * @param n The node.
   */
  public void visitMatchExpression(GNode n) {
    dispatch(n.getGeneric(0));
    
    // Process pattern matching
    Node pmatching = n.getGeneric(1);
    for (int i = 0; i < pmatching.size(); i++) {
      Node pmatch = pmatching.getGeneric(i);
          
      // Get ids in the right hand side expression
      Set<String> ids1 = 
        new FreeVariableCollector(pmatch.getGeneric(1)).getIdentifiers();
      
      // Remove bound ids
      Set<String> ids2 = 
        new FreeVariableCollector(pmatch.getGeneric(0), ids1).getIdentifiers();
        
      idlist.addAll(ids2);
    } 
  }
    
  /** 
   * Get (bound) identifiers in patterns. 
   *
   * @param n The node.
   */
  public void visitPatterns(GNode n) {
    // Because all patterns must have the same bound variables, it is 
    //   sufficient to get variables from the first patterns
    dispatch(n.getGeneric(0));
  }
   
  /** 
   * Get (bound) identifiers in TuplePattern. 
   *
   * @param n The node.
   */
  public void visitTuplePattern(GNode n) {
    for (int i = 0; i < n.size(); i ++) {
      dispatch(n.getGeneric(i));
    }
  }
    
  /** 
   * Get (bound) identifiers in WhenPattern. 
   *
   * @param n The node.
   */
  public void visitWhenPattern(GNode n) {
    dispatch(n.getGeneric(0));
  }
    
  /** 
   * Get (bound) identifiers in AsPattern. 
   *
   * @param n The node.
   */
  public void visitAsPattern(GNode n) {
    dispatch(n.getGeneric(0));
    final String id = n.getString(1);
    idlist.remove(id);
  }
    
  /** 
   * Get (bound) identifiers in TypedPattern. 
   *
   * @param n The node.
   */
  public void visitTypedPattern(GNode n) {
    dispatch(n.getGeneric(0));
  }
  
  /** 
   * Get (bound) identifiers in ConsPattern. 
   *
   * @param n The node.
   */
  public void visitConsPattern(GNode n) {
    dispatch(n.getGeneric(0));
    dispatch(n.getGeneric(1));
  }
    
  /** 
   * Get (bound) identifier in a Variable. 
   *
   * @param n The node.
   */
  public void visitVariable(GNode n) {
    idlist.remove(n.getString(0));
  }
    
  /** 
   * Get (bound) identifiers in a TypeConstructorPattern. 
   *
   * @param n The node.
   */
  public void visitTypeConstructorPattern(GNode n) {
    for (int i = 1; i < n.size(); i++) {
      dispatch(n.getGeneric(i));
    }    
  }
    
  /** 
   * Get (bound) identifiers in PatternParameters. 
   *
   * @param n The node.
   */
  public void visitPatternParameters(GNode n) {
    for (int i = 0; i < n.size(); i++) {
      dispatch(n.getGeneric(i));
    }  
  }
    
  /** 
   * Get (bound) identifiers in a ListPattern. 
   *
   * @param n The node.
   */
  public void visitListPattern(GNode n) {
    for (int i = 0; i < n.size(); i++) {
      dispatch(n.getGeneric(i));
    } 
  }
    
  /** 
   * Get (bound) identifiers in a RecordPattern. 
   *
   * @param n The node.
   */
  public void visitRecordPattern(GNode n) {
    for (int i = 0; i < n.size(); i++) {
      dispatch(n.getGeneric(i));
    } 
  }
   
  /** 
   * Get (bound) identifiers in a FieldPattern. 
   *
   * @param n The node.
   */
  public void visitFieldPattern(GNode n) {
    dispatch(n.getGeneric(1));
  }
    
  /** 
   * Get identifiers in a TupleConstructor. 
   *
   * @param n The node.
   */
  public void visitTupleConstructor(GNode n) {
    for (int i = 1; i < n.size(); i++) {
      dispatch(n.getGeneric(i));
    } 
  } 
    
  /** 
   * Get identifiers in a RecordExpression. 
   *
   * @param n The node.
   */
  public void visitRecordExpression(GNode n) {
    for (int i = 0; i < n.size(); i++) {
      dispatch(n.getGeneric(i));
    } 
  }
    
  /** 
   * Get identifiers in a WithExpression. 
   *
   * @param n The node.
   */
  public void visitWithExpression(GNode n) {
    dispatch(n.getGeneric(0));
  }
    
  /** 
   * Get identifiers in a FieldAssignment. 
   *
   * @param n The node.
   */
  public void visitFieldAssignment(GNode n) {
    dispatch(n.getGeneric(1));
  }
    
  /** 
   * Get identifiers in a ListLiteral. 
   *
   * @param n The node.
   */
  public void visitListLiteral(GNode n) {
    for (int i = 0; i < n.size(); i++) {
      dispatch(n.getGeneric(i));
    } 
  }
    
  /** 
   * Get the identifers in a lower id. 
   *
   * @param n The node.
   */
  public void visitLowerID(GNode n) {
    idlist.add(n.getString(0));
  }
  
  /** 
   * Get the identifiers in other nodes. 
   *
   * @param n The node.
   */
  public void visit(GNode n) {
    // do nothing
  }
    
}  
