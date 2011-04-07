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

import xtc.tree.Visitor;

/**
 * Visitor to provide a conservative estimate for the cost of parsing
 * a production.  One unit of cost is approximately equivalent to the
 * effort involved in parsing a character and testing it for a
 * specific value.  The cost estimate for a production includes the
 * costs for any other productions referenced by that production.  If
 * the cost cannot be statically determined (for example, for a
 * repetition) or a set of productions is mutually recursive, the cost
 * is assumed to be unlimited, as represented by
 * <code>Integer.MAX_VALUE</code>.  This visitor must be invoked by
 * visiting a grammar, which clears any previous cost estimates and
 * annotates each production with its estimated cost.
 *
 * <p />This visitor assumes that the entire grammar is contained in a
 * single module.
 *
 * @author Robert Grimm
 * @version $Revision: 1.26 $
 */
public class CostEstimator extends Visitor {

  /** The analyzer utility. */
  protected final Analyzer analyzer;

  /**
   * Create a new cost estimator.
   *
   * @param analyzer The analyzer utility.
   */
  public CostEstimator(Analyzer analyzer) {
    this.analyzer = analyzer;
  }

  /** Visit the specified grammar. */
  public void visit(Module m) {
    // Initialize the per-grammar state.
    analyzer.register(this);
    analyzer.init(m);

    // Clear any previous cost estimates.
    for (Production p : m.productions) p.removeProperty(Properties.COST);

    // Now, set the cost estimates.
    for (Production p : m.productions) {
      if (! p.hasProperty(Properties.COST)) {
        analyzer.process(p);
      }
    }
  }

  /** Visit the specified production. */
  public Integer visit(Production p) {
    analyzer.workingOn(p.qName);
    Integer cost = (Integer)dispatch(p.choice);
    analyzer.notWorkingOn(p.qName);
    p.setProperty(Properties.COST, cost);
    return cost;
  }

  /** Visit the specified ordered choice. */
  public Integer visit(OrderedChoice c) {
    // In the worst case, none of the alternatives parses.  Hence, the
    // overall cost is the sum of the costs of all alternatives.
    int cost = 0;
    for (Sequence s : c.alternatives) {
      cost = add(cost, (Integer)dispatch(s));
    }
    return cost;
  }

  /** Visit the specified repetition. */
  public Integer visit(Repetition r) {
    return Integer.MAX_VALUE;
  }

  /** Visit the specified option. */
  public Integer visit(Option o) {
    // In the worst case, the optional element does not parse.  Hence,
    // we add one unit for the empty case.
    return add(1, (Integer)dispatch(o.element));
  }

  /** Visit the specified sequence. */
  public Integer visit(Sequence s) {
    int cost = 0;
    for (Element e : s.elements) {
      cost = add(cost, (Integer)dispatch(e));
    }
    return cost;
  }

  /** Visit the specified predicate. */
  public Integer visit(Predicate p) {
    // We add one unit for the test implied by a predicate.
    return add(1, (Integer)dispatch(p.element));
  }

  /** Visit the specified voided element. */
  public Integer visit(VoidedElement v) {
    // Voiding elements comes for free as no code is generated.
    return (Integer)dispatch(v.element);
  }

  /** Visit the specified binding. */
  public Integer visit(Binding b) {
    // Bindings are pretty much for free.
    return (Integer)dispatch(b.element);
  }

  /** Visit the specified string match. */
  public Integer visit(StringMatch m) {
    // We add one unit for the test performed by a string match.
    return add(1, (Integer)dispatch(m.element));
  }

  /** Visit the specified nonterminal. */
  public Integer visit(NonTerminal nt) {
    Production p = analyzer.lookup(nt);

    if (analyzer.isBeingWorkedOn(p.qName)) {
      // A recursion has unlimited cost.
      return Integer.MAX_VALUE;
    } else {
      // We add one unit for testing the nonterminal.
      return add(1, p.hasProperty(Properties.COST)?
                 (Integer)p.getProperty(Properties.COST) : (Integer)dispatch(p));
    }
  }

  /** Visit the specified string literal. */
  public Integer visit(StringLiteral l) {
    // Each character in the string literal requires a test.
    return l.text.length();
  }

  /** Visit the specified character case. */
  public Integer visit(CharCase c) {
    if (null == c.element) {
      return 0;
    } else {
      return (Integer)dispatch(c.element);
    }
  }

  /** Visit the specified character switch. */
  public Integer visit(CharSwitch sw) {
    // A character switch has disjoint cases, so the cost is the
    // maximum cost of the cases and default (+ 1 for the character
    // switch itself).
    int cost = 0;
    for (CharCase kase : sw.cases) {
      cost = Math.max(cost, (Integer)dispatch(kase) + 1);
    }
    if (null == sw.base) {
      cost = Math.max(cost, (Integer)dispatch(sw.base) + 1);
    }
    return cost;
  }

  /**
   * Visit the specified terminal.  This method provides the default
   * implementation for any character elements, character classes, and
   * character literals.
   */
  public Integer visit(Terminal t) {
    return 1;
  }

  /** Visit the specified node marker. */
  public Integer visit(NodeMarker m) {
    return 0;
  }

  /** Visit the specified action. */
  public Integer visit(Action a) {
    // An action typically creates a semantic value, so the cost is 1.
    return 1;
  }

  /** Visit the specified parser action. */
  public Integer visit(ParserAction a) {
    // Parser actions may consume arbritrary inputs, so they have
    // unlimited cost.
    return Integer.MAX_VALUE;
  }

  /** Visit the specified null literal. */
  public Integer visit(NullLiteral l) {
    // Null literals are free.
    return 0;
  }

  /** Visit the specified string value. */
  public Integer visit(StringValue v) {
    // A string value without a static text creates a string, so the
    // cost is 1.
    return null == v.text ? 1 : 0;
  }

  /** Visit the specified proper list value. */
  public Integer visit(ProperListValue v) {
    // A proper list value creates a pair for each element.
    return v.elements.size();
  }

  /** Visit the specified action base value. */
  public Integer visit(ActionBaseValue v) {
    // An action base value applies all actions on the list, so the
    // cost is proportional to the length of the list.
    return Integer.MAX_VALUE;
  }

  /** Visit the specified generic node value. */
  public Integer visit(GenericNodeValue v) {
    // A generic value creates a generic node, so the cost is 2 (1 for
    // the node, 1 for the children).
    return 2;
  }

  /** Visit the specified generic action value. */
  public Integer visit(GenericActionValue v) {
    // A generic action value creates a new action, so the cost is 1.
    return 1;
  }

  /** Visit the specified generic recursion value. */
  public Integer visit(GenericRecursionValue v) {
    // A generic recursion value creates a new action and a new pair,
    // so the cost is 2.
    return 2;
  }

  /**
   * Visit the specified value element.  This method provides the
   * default implementation for null values and empty list values.
   */
  public Integer visit(ValueElement v) {
    return 0;
  }

  /**
   * Add the two specified estimates.
   *
   * @param e1 The first estimate.
   * @param e2 The second estimate.
   * @return The sum.
   */
  protected static int add(final int e1, final int e2) {
    if ((Integer.MAX_VALUE == e1) || (Integer.MAX_VALUE == e2)) {
      return Integer.MAX_VALUE;
    } else {
      long sum = e1 + e2;
      if (Integer.MAX_VALUE < sum) {
        return Integer.MAX_VALUE;
      } else {
        return (int)sum;
      }
    }
  }

}
