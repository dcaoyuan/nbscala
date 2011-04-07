/*
 * xtc - The eXTensible Compiler
 * Copyright (C) 2004-2007 Robert Grimm
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
package xtc.parser;

import xtc.util.Runtime;

import xtc.type.AST;

/**
 * Visitor to turn generic productions into void productions.  Note
 * that this visitor also voids productions that have {@link
 * xtc.tree.Node} or {@link xtc.tree.GNode} as their types.
 *
 * <p />This visitor assumes that the entire grammar is contained in a
 * single module.
 *
 * @author Robert Grimm
 * @version $Revision: 1.11 $
 */
public class GenericVoider extends GrammarVisitor {

  /**
   * Create a new generic voider.
   *
   * @param runtime The runtime.
   * @param analyzer The analyzer utility.
   */
  public GenericVoider(Runtime runtime, Analyzer analyzer) {
    super(runtime, analyzer);
  }

  /**
   * Check the specified binding or string match.  If the bound or
   * matched element has been voided by this visitor, this method
   * reports an error.  However, bindings to yyValue in voided
   * productions are eliminated.
   *
   * @param op The binding or string match.
   * @return The checked element.
   */
  protected Element check(UnaryOperator op) {
    assert (op instanceof Binding) || (op instanceof StringMatch);

    Element bound = Analyzer.strip(op.element);
    if (bound instanceof NonTerminal) {
      NonTerminal    nt = (NonTerminal)bound;
      FullProduction p  = analyzer.lookup(nt);

      if (p.getBooleanProperty(Properties.VOIDED)) {
        if (op instanceof StringMatch) {
          runtime.error("string match for now voided nonterminal '"+nt+"'", op);
        } else {
          if (CodeGenerator.VALUE.equals(((Binding)op).name) &&
              analyzer.current().getBooleanProperty(Properties.VOIDED)) {
            // If the current production has been voided (i.e., was
            // generic) and the binding's name is yyValue, we can
            // safely eliminate the binding.  It was used to pass a
            // value through instead of creating a generic node.
            return nt;
          } else {
            runtime.error("binding for now voided nonterminal '" + nt + "'", op);
          }
        }
      }
    }

    return op;
  }
  
  /** Visit the specified grammar. */
  public Object visit(Module m) {
    // Initialize the per-grammar state.
    analyzer.register(this);
    analyzer.init(m);

    // First, mark all productions returning nodes or lists of nodes
    // as void.
    for (Production p : m.productions) {
      if (AST.isNode(p.type) ||
          (AST.isList(p.type) && AST.isNode(AST.getArgument(p.type)))) {
        p.type = AST.VOID;
        p.setProperty(Properties.VOIDED, Boolean.TRUE);
      }
    }

    // Second, make sure that no void production is bound.
    for (Production p : m.productions) analyzer.process(p);

    // Done.
    return null;
  }

  /** Visit the specified binding. */
  public Element visit(Binding b) {
    b.element = (Element)dispatch(b.element);
    return check(b);
  }

  /** Visit the specified string match. */
  public Element visit(StringMatch m) {
    m.element = (Element)dispatch(m.element);
    return check(m);
  }
  
}
