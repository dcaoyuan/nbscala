/*
 * xtc - The eXTensible Compiler
 * Copyright (C) 2004-2007 Robert Grimm
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
 * The interface to all actions.  An action implements a computation
 * that takes a single argument and produces a single result.  Both
 * argument and result have the same type so that actions can be
 * trivially composed with each other.
 *
 * <p />Actions help in producing a left-recursive AST, even though
 * the productions generating the individual AST nodes are
 * right-recursive (as left-recursion is generally illegal for
 * <i>Rats!</i>' grammars).  The basic idea is to create a {@link Pair
 * list} of actions during the right-recursion and then apply the
 * actions onto the semantic value of the base case.
 *
 * <p />To illustrate this use of actions, consider the grammar rule
 * for logical and expressions in C:<pre>
 *   <i>logical-and-expression</i> :
 *     <i>bitwise-or-expression</i>
 *     <i>logical-and-expression</i> <b>&&</b> <i>bitwise-or-expression</i>
 * </pre>
 * Since this grammar rule is left-recursive, it cannot directly be
 * converted into the corresponding <i>Rats!</i> production and must
 * be rewritten as a right-recursion.  At the same time, the
 * corresponding AST should still be left-recursive, as the logical
 * and operator is left-associative.
 *
 * <p />Using actions and {@link xtc.tree.GNode generic nodes} as
 * semantic values, the corresponding right-recursive productions can
 * be written as follows:<pre>
 *   Node LogicalAndExpression =
 *     base:BitwiseOrExpression list:LogicalAndExpressionTail*
 *       { yyValue = apply(list, base); }
 *     ;
 *
 *   Action&lt;Node&gt; LogicalAndExpressionTail =
 *     "&&":Symbol right:BitwiseOrExpression
 *       { yyValue = new Action&lt;Node&gt;() {
 *           public Node run(Node left) {
 *             return GNode.create("LogicalAndExpression", left, right);
 *           }};
 *       }
 *     ;
 * </pre>
 * The semantic action for the <code>LogicalAndExpression</code>
 * production relies on an <code>apply()</code> helper method, which
 * can be written as following:<pre>
 *   &lt;T&gt; T apply(Pair&lt;Action&lt;T&gt;&gt; actions, T seed) {
 *     while (! actions.isEmpty()) {
 *       seed    = actions.head().run(seed);
 *       actions = actions.tail();
 *     }
 *     return seed;
 *   }
 * </pre>
 * In detail, the <code>LogicalAndExpressionTail</code> production
 * recognizes logical and operators followed by the right operands.
 * By using an action as its semantic value, the production delays the
 * actual construction of the corresponding generic node.  Once all
 * logical and operators and the corresponding bitwise or expressions
 * have been parsed, the semantic action for the
 * <code>LogicalAndExpression</code> production applies the actions
 * and creates a left-recursive AST.  Note that this example assumes
 * that the production for bitwise or expressions also has a generic
 * node as its semantic value.  Further note that, if there is no
 * logical and operator in the input, this example simply passes the
 * semantic value of the single bitwise or expression through, which
 * is the desired behavior as it leaves the AST unmodified.  Finally,
 * note that direct left recursions may appear in generic productions,
 * as <i>Rats!</i> automatically transforms them into the
 * corresponding right-recursions while also creating left-recursive
 * semantic values with the help of actions.
 *
 * @author Robert Grimm
 * @version $Revision: 1.10 $
 */
public interface Action<T> {

  /**
   * Perform this action.
   *
   * @param arg The argument.
   * @return The result.
   */
  T run(T arg);

}
