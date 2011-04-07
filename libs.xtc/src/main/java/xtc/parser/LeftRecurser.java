/*
 * xtc - The eXTensible Compiler
 * Copyright (C) 2004-2008 Robert Grimm
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

import java.util.HashSet;
import java.util.Set;

import xtc.tree.Visitor;

import xtc.util.Runtime;

/** 
 * Visitor to detect left-recursion in a grammar.
 *
 * <p />This visitor requires that text-only productions {@link
 * TextTester have been marked} as such.
 *
 * @author Robert Grimm
 * @version $Revision: 1.49 $
 */
public class LeftRecurser extends Visitor {

  /** The runtime. */
  protected final Runtime runtime;

  /** The analyzer utility. */
  protected final Analyzer analyzer;

  /** The flag for whether we have seen a terminal. */
  protected boolean  terminated;

  /**
   * Create a new left-recurser.
   *
   * @param runtime The runtime.
   * @param analyzer The analyzer utility.
   */
  public LeftRecurser(Runtime runtime, Analyzer analyzer) {
    this.runtime  = runtime;
    this.analyzer = analyzer;
  }

  /**
   * Get the set of nonterminals corresponding to left-recursive
   * productions.  Note that this method must only be called after
   * visiting the corresponding grammar with this visitor.
   *
   * @return The set of left-recursive nonterminals.
   */
  public Set<NonTerminal> recursive() {
    return new HashSet<NonTerminal>(analyzer.marked());
  }

  /** Visit the specified grammar. */
  public void visit(Grammar g) {
    // Reset the per-grammar state.
    analyzer.register(this);
    analyzer.init(g);

    for (Module m : g.modules) {
      analyzer.process(m);

      for (Production p : m.productions) {
        // Only process full productions that have not been processed.
        if ((! p.isFull()) || analyzer.isProcessed(p.qName)) continue;

        // Reset the per-production state.
        terminated = false;

        // Process the production.
        analyzer.process(p);
      }
    }
  }

  /** Visit the specified self-contained module. */
  public void visit(Module m) {
    // Reset the per-grammar state.
    analyzer.register(this);
    analyzer.init(m);

    for (Production p : m.productions) {
      // Only process full productions that have not been processed.
      if (analyzer.isProcessed(p.qName)) continue;

      // Reset the per-production state.
      terminated = false;

      // Process the production.
      analyzer.process(p);
    }
  }

  /** Visit the specified production. */
  public void visit(FullProduction p) {
    Object closure = analyzer.enter(p);

    // We only keep a production in the working set while we are
    // actively traversing reachable productions.  Otherwise, we might
    // incorrectly classify a production as left-recursive, for
    // example, when traversing productions encoding operator
    // precedence.
    analyzer.workingOn(p.qName);

    if ((runtime.test("optimizeLeftRecursions") ||
         runtime.test("optimizeLeftIterations")) &&
        DirectLeftRecurser.isTransformable(p)) {
      // Directly left-recursive productions get the special treatment
      // by skipping the recursive alternatives in the top-level
      // choice.
      for (Sequence alt : p.choice.alternatives) {
        if (DirectLeftRecurser.isBase(alt, p)) {
          dispatch(alt);
        }
      }

    } else {
      dispatch(p.choice);
    }

    analyzer.notWorkingOn(p.qName);
    analyzer.exit(closure);
    analyzer.processed(p.qName);
  }

  /** Visit the specified ordered choice. */
  public void visit(OrderedChoice c) {
    boolean  more = false;

    for (Sequence alt : c.alternatives) {
      terminated = false;
      dispatch(alt);
      if (! terminated) {
        more = true;
      }
    }

    if (more) {
      terminated = false;
    }
  }

  /** Visit the specified repetition. */
  public void visit(Repetition r) {
    dispatch(r.element);
    if (! r.once) {
      terminated = false;
    }
  }

  /** Visit the specified sequence. */
  public void visit(Sequence s) {
    for (Element e : s.elements) {
      dispatch(e);
      if (terminated) break;
    }
  }

  /** Visit the specified voided element. */
  public void visit(VoidedElement v) {
    dispatch(v.element);
  }

  /** Visit the specified binding. */
  public void visit(Binding b) {
    dispatch(b.element);
  }

  /** Visit the specified string match. */
  public void visit(StringMatch m) {
    dispatch(m.element);
  }

  /** Visit the specified nonterminal. */
  public void visit(NonTerminal nt) {
    FullProduction p;

    try {
      p = analyzer.lookup(nt);
    } catch (IllegalArgumentException x) {
      terminated = true;
      return;
    }

    if (null != p) {
      if (analyzer.isBeingWorkedOn(p.qName)) {
        analyzer.mark(p.qName);
        p.setProperty(Properties.RECURSIVE, Boolean.TRUE);
        terminated = true;

      } else if (! analyzer.isProcessed(p.qName)) {
        dispatch(p);
      } else {
        terminated = true;
      }
    } else {
      terminated = true;
    }
  }

  /** Visit the specified terminal. */
  public void visit(Terminal t) {
    // We can't left-recurse on terminals.
    terminated = true;
  }

  /**
   * Visit the specified unary operator. This method provides the
   * default implementation for options and predicates.
   */
  public void visit(UnaryOperator op) {
    dispatch(op.element);
    terminated = false;
  }

  /**
   * Visit the specified parser action.  Parser actions are assumed to
   * always consume some input.
   */
  public void visit(ParserAction pa) {
    dispatch(pa.element);
    terminated = true;
  }

  /**
   * Visit the specified element. This method provides the default
   * implementation for node markers, actions, parse tree nodes, null
   * literals, and value elements.
   */
  public void visit(Element e) {
    // Nothing to do.
  }

}
