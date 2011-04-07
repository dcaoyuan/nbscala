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
 * Visitor to set flags indicating which liraries are used. 
 *
 * @author Anh Le
 * @version $Revision: 1.2 $
 */
public class FlagSetter extends Visitor {

  /** A variable to check if List is used. */
  public boolean isListUsed;

  /** A variable to check if ArrayList is used. */
  public boolean isArrayListUsed;  

  /** A variable to check if BigInteger is used. */
  public boolean isBigIntegerUsed;

  /** A variable to check if Pair is used. */
  public boolean isPairUsed;

  /** A variable to check if Node is used. */
  public boolean isNodeUsed;

  /** A variable to check if GNode is used. */
  public boolean isGNodeUsed;

  /** A variable to check if Primitives is used. */
  public boolean isPrimitivesUsed;

  /** A variable to check if Record is used. */
  public boolean isRecordUsed;

  /** A variable to check if Variant is used. */
  public boolean isVariantUsed;

  /** A variable to check if Tuple is used. */
  public boolean isTupleUsed;

  /** A variable to check if Reduction is used. */
  public boolean isReductionUsed;

  /** A variable to check if Name is used. */
  public boolean isNameUsed;

  /** A variable to check if Scope is used. */
  public boolean isScopeUsed;

  /** A variable to check if ScopeKind is used. */
  public boolean isScopeKindUsed;

  /** A variable to check if Analyzer is used. */
  public boolean isAnalyzerUsed;
  
  /** 
   * Create a new flag settor. 
   *
   * @param n The root of the expression.
   * 
   */
  public FlagSetter(GNode n) {
    // Set all flag to false
    isListUsed       = false;
    isArrayListUsed  = false;
    isBigIntegerUsed = false;
    isPairUsed       = false;
    isNodeUsed       = false;
    isGNodeUsed      = false;
    isPrimitivesUsed = false;
    isRecordUsed     = false;
    isVariantUsed    = false;
    isTupleUsed      = false;
    isReductionUsed  = false;
    isNameUsed       = false;
    isScopeUsed      = false;
    isScopeKindUsed  = false; 
    isAnalyzerUsed   = false;
    
    // Dispatch  
    dispatch(n);
  }
  
  /**
   * Check the name of a used library and set the corresponding flag variable.
   *
   * @param name The name of the library.
   */
  private void checkName(String name) {
    if ("List".equals(name))            isListUsed       = true;
    else if ("ArrayList".equals(name))  isArrayListUsed  = true;
    else if ("BigInteger".equals(name)) isBigIntegerUsed = true;
    else if ("Pair".equals(name))       isPairUsed       = true;
    else if ("Node".equals(name))       isNodeUsed       = true;
    else if ("GNode".equals(name))      isGNodeUsed      = true;
    else if ("Primitives".equals(name)) isPrimitivesUsed = true;
    else if ("Record".equals(name))     isRecordUsed     = true;
    else if ("Variant".equals(name))    isVariantUsed    = true;
    else if ("Tuple".equals(name))      isTupleUsed      = true;
    else if ("Reduction".equals(name))  isReductionUsed  = true;
    else if ("Name".equals(name))       isNameUsed       = true;
    else if ("Scope".equals(name))      isScopeUsed      = true;
    else if ("ScopeKind".equals(name))  isScopeKindUsed  = true;
    else if ("Analyzer".equals(name))   isAnalyzerUsed   = true;    
  } 
  
  /** 
   * Set flags in ConstructorDeclaration .
   * 
   * @param n The node
   */
  public void visitConstructorDeclaration(GNode n) {
    dispatch(n.getGeneric(1));
    dispatch(n.getGeneric(3));
    dispatch(n.getGeneric(5));
  } 
  
  /** 
   * Set flags in FieldDeclaration .
   * 
   * @param n The node
   */
  public void visitFieldDeclaration(GNode n) {
    dispatch(n.getGeneric(1));
    dispatch(n.getGeneric(2));
  }  
  
  /** 
   * Set flags in TypeParameter .
   * 
   * @param n The node
   */
  public void visitTypeParameter(GNode n) {
    checkName(n.getString(0));
    dispatch(n.getGeneric(1));
  } 
    
  /** 
   * Set flags in TypeInstantiation .
   * 
   * @param n The node
   */
  public void visitTypeInstantiation(GNode n) {
    checkName(n.getString(0));
    dispatch(n.getGeneric(1));
  }  
  
  /** 
   * Set flags in EnumConstant.
   * 
   * @param n The node
   */
  public void visitEnumConstant(GNode n) {
    dispatch(n.getGeneric(0));
    dispatch(n.getGeneric(2));
    dispatch(n.getGeneric(3));
  } 
  
  /** 
   * Set flags in QualifiedIdentifier.
   * 
   * @param n The node
   */
  public void visitQualifiedIdentifier(GNode n) {
    for (int i = 0; i < n.size(); i++) {
      checkName(n.getString(i));
    }
  }   
  
  /** 
   * Set flags in PrimaryIdentifier.
   * 
   * @param n The node
   */
  public void visitPrimaryIdentifier(GNode n) {
    checkName(n.getString(0));
  }
  
  /** 
   * Set flags in PrimaryIdentifier.
   * 
   * @param n The node
   */
  public void visitDeclarator(GNode n) {
    dispatch(n.getGeneric(2));
  }
  
  /** 
   * Set flags in AnnotationDeclaration.
   * 
   * @param n The node
   */  
  public void visitAnnotationDeclaration(GNode n) {
    dispatch(n.getGeneric(2));
  }       
  
  /** 
   * Set flags in SelectionExpression.
   * 
   * @param n The node
   */  
  public void visitSelectionExpression(GNode n) {
    dispatch(n.getGeneric(0));
  }
  
  /** 
   * Set flags in PostfixExpression.
   * 
   * @param n The node
   */  
  public void visitPostfixExpression(GNode n) {
    dispatch(n.getGeneric(0));
  }
  
  /** 
   * Set flags in Type.
   * 
   * @param n The node
   */  
  public void visitType(GNode n) {
    dispatch(n.getGeneric(0));  
  } 
  
  /** 
   * Set flags in CallExpression.
   * 
   * @param n The node
   */  
  public void visitCallExpression(GNode n) {
    dispatch(n.getGeneric(0));
    dispatch(n.getGeneric(1));
    dispatch(n.getGeneric(3));
  }
  
  /** 
   * Set flags in Expression.
   * 
   * @param n The node
   */  
  public void visitExpression(GNode n) {
    dispatch(n.getGeneric(0));
    dispatch(n.getGeneric(2));
  }
  
  /** 
   * Set flags in EqualityExpression.
   * 
   * @param n The node
   */  
  public void visitEqualityExpression(GNode n) {
    dispatch(n.getGeneric(0));
    dispatch(n.getGeneric(2));
  }
  
  /** 
   * Set flags in RelationalExpression.
   * 
   * @param n The node
   */  
  public void visitRelationalExpression(GNode n) {
    dispatch(n.getGeneric(0));
    dispatch(n.getGeneric(2));
  }
  
  /** 
   * Set flags in ShiftExpression.
   * 
   * @param n The node
   */  
  public void visitShiftExpression(GNode n) {
    dispatch(n.getGeneric(0));
    dispatch(n.getGeneric(2));
  }
  
  /** 
   * Set flags in AdditiveExpression.
   * 
   * @param n The node
   */  
  public void visitAdditiveExpression(GNode n) {
    dispatch(n.getGeneric(0));
    dispatch(n.getGeneric(2));
  }
  
  /** 
   * Set flags in MultiplicativeExpression.
   * 
   * @param n The node
   */  
  public void visitMultiplicativeExpression(GNode n) {
    dispatch(n.getGeneric(0));
    dispatch(n.getGeneric(2));
  }  
  
  /** 
   * Set flags in ClassDeclaration.
   * 
   * @param n The node
   */  
  public void visitClassDeclaration(GNode n) {
    dispatch(n.getGeneric(2));
    dispatch(n.getGeneric(3));
    dispatch(n.getGeneric(4));
    dispatch(n.getGeneric(5));
  } 
  
  /** 
   * Set flags in InterfaceDeclaration.
   * 
   * @param n The node
   */  
  public void visitInterfaceDeclaration(GNode n) {
    dispatch(n.getGeneric(2));
    dispatch(n.getGeneric(3));
    dispatch(n.getGeneric(4));
  }
  
  /** 
   * Set flags in EnumDeclaration.
   * 
   * @param n The node
   */  
  public void visitEnumDeclaration(GNode n) {
    dispatch(n.getGeneric(2));
    dispatch(n.getGeneric(3));
    dispatch(n.getGeneric(4));
  } 
  
  /** 
   * Set flags in EnhancedForControl.
   * 
   * @param n The node
   */  
  public void visitEnhancedForControl(GNode n) {
    dispatch(n.getGeneric(1));
    dispatch(n.getGeneric(3));
  }
  
  /** 
   * Set flags in AnnotationMethod.
   * 
   * @param n The node
   */  
  public void visitAnnotationMethod(GNode n) {
    dispatch(n.getGeneric(1));
    dispatch(n.getGeneric(3));
  } 
  
  /** 
   * Set flags in MethodDeclaration.
   * 
   * @param n The node
   */  
  public void visitMethodDeclaration(GNode n) {
    dispatch(n.getGeneric(1));
    dispatch(n.getGeneric(2));
    dispatch(n.getGeneric(4));
    dispatch(n.getGeneric(7));
  } 
 
  /** 
   * Set flags in BlockDeclaration.
   * 
   * @param n The node
   */    
  public void visitBlockDeclaration(GNode n) {
    dispatch(n.getGeneric(1));
  }
  
  /** 
   * Set flags in LabeledStatement.
   * 
   * @param n The node
   */    
  public void visitLabeledStatement(GNode n) {
    dispatch(n.getGeneric(1));
  }
  
  /** 
   * Set flags in UnaryExpression.
   * 
   * @param n The node
   */    
  public void visitUnaryExpression(GNode n) {
    dispatch(n.getGeneric(1));
  }
  
  /** 
   * Set flags in FormalParameter.
   * 
   * @param n The node
   */    
  public void visitFormalParameter(GNode n) {
    dispatch(n.getGeneric(1));
  }
  
  /** 
   * Set flags in ElementValuePair.
   * 
   * @param n The node
   */    
  public void visitElementValuePair(GNode n) {
    dispatch(n.getGeneric(1));
  }
  
  /** 
   * Set flags in WildcardBound.
   * 
   * @param n The node
   */  
  public void visitWildcardBound(GNode n) {
    dispatch(n.getGeneric(1));
  }
  
  /** 
   * Set flags in this node.
   * 
   * @param n The node
   */  
  public void visitClassBody(GNode n) {
    for (int i = 0; i < n.size(); i++) {
      dispatch(n.getGeneric(i));
    }
  }
  
  /** 
   * Set flags in this node.
   * 
   * @param n The node
   */ 
  public void visitExtension(GNode n) {
    for (int i = 0; i < n.size(); i++) {
      dispatch(n.getGeneric(i));
    }
  }
  
  /** 
   * Set flags in this node.
   * 
   * @param n The node
   */ 
  public void visitImplementation(GNode n) {
    for (int i = 0; i < n.size(); i++) {
      dispatch(n.getGeneric(i));
    }
  }
  
  /** 
   * Set flags in this node.
   * 
   * @param n The node
   */ 
  public void visitBlock(GNode n) {
    for (int i = 0; i < n.size(); i++) {
      dispatch(n.getGeneric(i));
    }
  }
  
  /** 
   * Set flags in this node.
   * 
   * @param n The node
   */ 
  public void visitConditionalStatement(GNode n) {
    for (int i = 0; i < n.size(); i++) {
      dispatch(n.getGeneric(i));
    }
  }
  
  /** 
   * Set flags in this node.
   * 
   * @param n The node
   */ 
  public void visitForStatement(GNode n) {
    for (int i = 0; i < n.size(); i++) {
      dispatch(n.getGeneric(i));
    }
  }
  
  /** 
   * Set flags in this node.
   * 
   * @param n The node
   */ 
  public void visitWhileStatement(GNode n) {
    for (int i = 0; i < n.size(); i++) {
      dispatch(n.getGeneric(i));
    }
  }
  
  /** 
   * Set flags in this node.
   * 
   * @param n The node
   */ 
  public void visitDoWhileStatement(GNode n) {
    for (int i = 0; i < n.size(); i++) {
      dispatch(n.getGeneric(i));
    }
  }
  
  /** 
   * Set flags in this node.
   * 
   * @param n The node
   */ 
  public void visitTryCatchFinallyStatement(GNode n) {
    for (int i = 0; i < n.size(); i++) {
      dispatch(n.getGeneric(i));
    }
  }
  
  /** 
   * Set flags in this node.
   * 
   * @param n The node
   */ 
  public void visitSwitchStatement(GNode n) {
    for (int i = 0; i < n.size(); i++) {
      dispatch(n.getGeneric(i));
    }
  }
  
  /** 
   * Set flags in this node.
   * 
   * @param n The node
   */ 
  public void visitSynchronizedStatement(GNode n) {
    for (int i = 0; i < n.size(); i++) {
      dispatch(n.getGeneric(i));
    }
  }
  
  /** 
   * Set flags in this node.
   * 
   * @param n The node
   */ 
  public void visitReturnStatement(GNode n) {
    for (int i = 0; i < n.size(); i++) {
      dispatch(n.getGeneric(i));
    }
  }
  
  /** 
   * Set flags in this node.
   * 
   * @param n The node
   */ 
  public void visitThrowStatement(GNode n) {
    for (int i = 0; i < n.size(); i++) {
      dispatch(n.getGeneric(i));
    }
  }
  
  /** 
   * Set flags in this node.
   * 
   * @param n The node
   */ 
  public void visitExpressionStatement(GNode n) {
    for (int i = 0; i < n.size(); i++) {
      dispatch(n.getGeneric(i));
    }
  }
  
  /** 
   * Set flags in this node.
   * 
   * @param n The node
   */ 
  public void visitAssertStatement(GNode n) {
    for (int i = 0; i < n.size(); i++) {
      dispatch(n.getGeneric(i));
    }
  }
  
  /** 
   * Set flags in this node.
   * 
   * @param n The node
   */ 
  public void visitBasicForControl(GNode n) {
    for (int i = 0; i < n.size(); i++) {
      dispatch(n.getGeneric(i));
    }
  }
  
  /** 
   * Set flags in this node.
   * 
   * @param n The node
   */ 
  public void visitCatchClause(GNode n) {
    for (int i = 0; i < n.size(); i++) {
      dispatch(n.getGeneric(i));
    }
  }
  
  /** 
   * Set flags in this node.
   * 
   * @param n The node
   */ 
  public void visitCaseClause(GNode n) {
    for (int i = 0; i < n.size(); i++) {
      dispatch(n.getGeneric(i));
    }
  }
  
  /** 
   * Set flags in this node.
   * 
   * @param n The node
   */ 
  public void visitDefaultClause(GNode n) {
    for (int i = 0; i < n.size(); i++) {
      dispatch(n.getGeneric(i));
    }
  }
  
  /** 
   * Set flags in this node.
   * 
   * @param n The node
   */ 
  public void visitExpressionList(GNode n) {
    for (int i = 0; i < n.size(); i++) {
      dispatch(n.getGeneric(i));
    }
  }
  
  /** 
   * Set flags in this node.
   * 
   * @param n The node
   */ 
  public void visitConditionalExpression(GNode n) {
    for (int i = 0; i < n.size(); i++) {
      dispatch(n.getGeneric(i));
    }
  }
  
  /** 
   * Set flags in this node.
   * 
   * @param n The node
   */ 
  public void visitLogicalOrExpression(GNode n) {
    for (int i = 0; i < n.size(); i++) {
      dispatch(n.getGeneric(i));
    }
  }
  
  /** 
   * Set flags in this node.
   * 
   * @param n The node
   */ 
  public void visitLogicalAndExpression(GNode n) {
    for (int i = 0; i < n.size(); i++) {
      dispatch(n.getGeneric(i));
    }
  }
  
  /** 
   * Set flags in this node.
   * 
   * @param n The node
   */ 
  public void visitBitwiseOrExpression(GNode n) {
    for (int i = 0; i < n.size(); i++) {
      dispatch(n.getGeneric(i));
    }
  }
  
  /** 
   * Set flags in this node.
   * 
   * @param n The node
   */ 
  public void visitBitwiseXorExpression(GNode n) {
    for (int i = 0; i < n.size(); i++) {
      dispatch(n.getGeneric(i));
    }
  }
  
  /** 
   * Set flags in this node.
   * 
   * @param n The node
   */ 
  public void visitBitwiseAndExpression(GNode n) {
    for (int i = 0; i < n.size(); i++) {
      dispatch(n.getGeneric(i));
    }
  }
  
  /** 
   * Set flags in this node.
   * 
   * @param n The node
   */ 
  public void visitInstanceOfExpression(GNode n) {
    for (int i = 0; i < n.size(); i++) {
      dispatch(n.getGeneric(i));
    }
  }
  
  /** 
   * Set flags in this node.
   * 
   * @param n The node
   */ 
  public void visitBitwiseNegationExpression(GNode n) {
    for (int i = 0; i < n.size(); i++) {
      dispatch(n.getGeneric(i));
    }
  }
  
  /** 
   * Set flags in this node.
   * 
   * @param n The node
   */ 
  public void visitLogicalNegationExpression(GNode n) {
    for (int i = 0; i < n.size(); i++) {
      dispatch(n.getGeneric(i));
    }
  }
  
  /** 
   * Set flags in this node.
   * 
   * @param n The node
   */ 
  public void visitBasicCastExpression(GNode n) {
    for (int i = 0; i < n.size(); i++) {
      dispatch(n.getGeneric(i));
    }
  }
  
  /** 
   * Set flags in this node.
   * 
   * @param n The node
   */ 
  public void visitCastExpression(GNode n) {
    for (int i = 0; i < n.size(); i++) {
      dispatch(n.getGeneric(i));
    }
  }
  
  /** 
   * Set flags in this node.
   * 
   * @param n The node
   */ 
  public void visitSuperExpression(GNode n) {
    for (int i = 0; i < n.size(); i++) {
      dispatch(n.getGeneric(i));
    }
  }
  
  /** 
   * Set flags in this node.
   * 
   * @param n The node
   */ 
  public void visitSubscriptExpression(GNode n) {
    for (int i = 0; i < n.size(); i++) {
      dispatch(n.getGeneric(i));
    }
  }
  
  /** 
   * Set flags in this node.
   * 
   * @param n The node
   */ 
  public void visitNewClassExpression(GNode n) {
    for (int i = 0; i < n.size(); i++) {
      dispatch(n.getGeneric(i));
    }
  }
  
  /** 
   * Set flags in this node.
   * 
   * @param n The node
   */ 
  public void visitClassLiteralExpression(GNode n) {
    for (int i = 0; i < n.size(); i++) {
      dispatch(n.getGeneric(i));
    }
  }
  
  /** 
   * Set flags in this node.
   * 
   * @param n The node
   */ 
  public void visitThisExpression(GNode n) {
    for (int i = 0; i < n.size(); i++) {
      dispatch(n.getGeneric(i));
    }
  }
  
  /** 
   * Set flags in this node.
   * 
   * @param n The node
   */ 
  public void visitNewArrayExpression(GNode n) {
    for (int i = 0; i < n.size(); i++) {
      dispatch(n.getGeneric(i));
    }
  }
  
  /** 
   * Set flags in this node.
   * 
   * @param n The node
   */ 
  public void visitConcreteDimensions(GNode n) {
    for (int i = 0; i < n.size(); i++) {
      dispatch(n.getGeneric(i));
    }
  }
  
  /** 
   * Set flags in this node.
   * 
   * @param n The node
   */ 
  public void visitArrayInitializer(GNode n) {
    for (int i = 0; i < n.size(); i++) {
      dispatch(n.getGeneric(i));
    }
  }
  
  /** 
   * Set flags in this node.
   * 
   * @param n The node
   */ 
  public void visitArguments(GNode n) {
    for (int i = 0; i < n.size(); i++) {
      dispatch(n.getGeneric(i));
    }
  }
  
  /** 
   * Set flags in this node.
   * 
   * @param n The node
   */ 
  public void visitFormalParameters(GNode n) {
    for (int i = 0; i < n.size(); i++) {
      dispatch(n.getGeneric(i));
    }
  }
  
  /** 
   * Set flags in this node.
   * 
   * @param n The node
   */ 
  public void visitDeclarators(GNode n) {
    for (int i = 0; i < n.size(); i++) {
      dispatch(n.getGeneric(i));
    }
  }
  
  /** 
   * Set flags in this node.
   * 
   * @param n The node
   */ 
  public void visitAnnotations(GNode n) {
    for (int i = 0; i < n.size(); i++) {
      dispatch(n.getGeneric(i));
    }
  }
  
  /** 
   * Set flags in this node.
   * 
   * @param n The node
   */ 
  public void visitAnnotation(GNode n) {
    for (int i = 0; i < n.size(); i++) {
      dispatch(n.getGeneric(i));
    }
  }
  
  /** 
   * Set flags in this node.
   * 
   * @param n The node
   */ 
  public void visitElementValuePairs(GNode n) {
    for (int i = 0; i < n.size(); i++) {
      dispatch(n.getGeneric(i));
    }
  }
  
  /** 
   * Set flags in this node.
   * 
   * @param n The node
   */ 
  public void visitDefaultValue(GNode n) {
    for (int i = 0; i < n.size(); i++) {
      dispatch(n.getGeneric(i));
    }
  }
  
  /** 
   * Set flags in this node.
   * 
   * @param n The node
   */ 
  public void visitEnumConstants(GNode n) {
    for (int i = 0; i < n.size(); i++) {
      dispatch(n.getGeneric(i));
    }
  }
  
  /** 
   * Set flags in this node.
   * 
   * @param n The node
   */ 
  public void visitEnumMembers(GNode n) {
    for (int i = 0; i < n.size(); i++) {
      dispatch(n.getGeneric(i));
    }
  }
  
  /** 
   * Set flags in this node.
   * 
   * @param n The node
   */ 
  public void visitTypeParameters(GNode n) {
    for (int i = 0; i < n.size(); i++) {
      dispatch(n.getGeneric(i));
    }
  }
  
  /** 
   * Set flags in this node.
   * 
   * @param n The node
   */ 
  public void visitBound(GNode n) {
    for (int i = 0; i < n.size(); i++) {
      dispatch(n.getGeneric(i));
    }
  }
  
  /** 
   * Set flags in this node.
   * 
   * @param n The node
   */ 
  public void visitTypeArguments(GNode n) {
    for (int i = 0; i < n.size(); i++) {
      dispatch(n.getGeneric(i));
    }
  }
  
  /** 
   * Set flags in this node.
   * 
   * @param n The node
   */ 
  public void visitWildcard(GNode n) {
    for (int i = 0; i < n.size(); i++) {
      dispatch(n.getGeneric(i));
    }
  }
  
  /** 
   * Set flags in this node.
   * 
   * @param n The node
   */ 
  public void visitInstantiatedType(GNode n) {
    for (int i = 0; i < n.size(); i++) {
      dispatch(n.getGeneric(i));
    }
  }
  
  /** 
   * Set flags in the other nodes.
   * 
   * @param n The node
   */ 
  public void visit(GNode n) {
    // do nothing
  }  
  
}
