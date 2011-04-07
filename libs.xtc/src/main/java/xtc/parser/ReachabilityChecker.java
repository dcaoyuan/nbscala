/*
 * xtc - The eXTensible Compiler
 * Copyright (C) 2007-2008 Robert Grimm
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

import xtc.tree.Visitor;

import xtc.util.Runtime;

/**
 * Visitor to ensure that every alternative is reachable.
 *
 * @author Robert Grimm
 * @version $Revision: 1.4 $
 */
public class ReachabilityChecker extends Visitor {

  /** The runtime. */
  protected Runtime runtime;

  /** The analyzer. */
  protected Analyzer analyzer;

  /**
   * Create a new reachability checker.
   *
   * @param runtime The runtime.
   * @param analyzer The analyzer.
   */
  public ReachabilityChecker(Runtime runtime, Analyzer analyzer) {
    this.runtime  = runtime;
    this.analyzer = analyzer;
  }

  /** Visit the specified grammar. */
  public void visit(Grammar g) {
    // Initialize the per-grammar state.
    analyzer.register(this);
    analyzer.init(g);

    // Process the modules and productions.
    for (Module m : g.modules) {
      analyzer.process(m);
      for (Production p : m.productions) {
        if (p.isFull()) {
          analyzer.process(p);
        }
      }
    }
  }

  /** Visit the specified module. */
  public void visit(Module m) {
    // Initialize the per-grammar state.
    analyzer.register(this);
    analyzer.init(m);

    // Process the productions.
    for (Production p : m.productions) analyzer.process(p);
  }

  /** Visit the specified full production. */
  public void visit(FullProduction p) {
    dispatch(p.choice);
  }

  /** Visit the specified choice. */
  public void visit(OrderedChoice c) {
    final int size = c.alternatives.size();
    for (int i=0; i<size; i++) {
      Sequence s = c.alternatives.get(i);
      if (! analyzer.restrictsInput(s) && (i < size-1)) {
        runtime.error("unreachable alternative", c.alternatives.get(i+1));
        break;
      } else {
        dispatch(s);
      }
    }
  }

  /** Visit the specified sequence. */
  public void visit(Sequence s) {
    for (Element e : s.elements) dispatch(e);
  }

  /** Visit the specified unary operator. */
  public void visit(UnaryOperator op) {
    dispatch(op.element);
  }

  /** Visit the specified character switch. */
  public void visit(CharSwitch s) {
    for (CharCase kase : s.cases) {
      dispatch(kase.element);
    }
    dispatch(s.base);
  }

  /** Visit the specified element. */
  public void visit(Element e) {
    // Nothing to do.
  }

}
