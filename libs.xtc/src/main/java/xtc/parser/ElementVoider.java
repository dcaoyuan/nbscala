/*
 * xtc - The eXTensible Compiler
 * Copyright (C) 2005-2007 Robert Grimm
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

import xtc.tree.Location;
import xtc.tree.Visitor;

import xtc.util.Runtime;

import xtc.type.AST;

/**
 * Visitor to void ordered choices, repetitions, and options.  This
 * visitor voids ordered choices, repetitions, and options, if all
 * expressions appearing in such an expression are either void
 * nonterminals or explicitly voided.  It requires that a grammar has
 * been {@link Resolver resolved} into a single module, that text-only
 * productions have been {@link TextTester marked} as such, and that
 * all expressions have been {@link Simplifier simplified}.
 *
 * @author Robert Grimm
 * @version $Revision: 1.15 $
 */
public class ElementVoider extends Visitor {

  /** The runtime. */
  protected final Runtime runtime;

  /** The analyzer utility. */
  protected final Analyzer analyzer;

  /** The flag for the top-level element. */
  protected boolean isTopLevel;

  /** The flag for the last element. */
  protected boolean isLastElement;

  /** The flag for suppressing the insertion of voided elements. */
  protected boolean suppressVoided;

  /** The flag for potentially having a value. */
  protected boolean hasValue;

  /**
   * Create a new element voider.
   *
   * @param runtime The runtime.
   * @param analyzer The analyzer.
   */
  public ElementVoider(Runtime runtime, Analyzer analyzer) {
    this.runtime  = runtime;
    this.analyzer = analyzer;
  }

  /**
   * Wrap the specified element in a voiding operator.
   *
   * @param el The element to wrap.
   * @return The wrapped element.
   */
  protected Element wrap(Element el) {
    Element  result  = new VoidedElement(el);
    Location loc     = el.getLocation();
    result.setLocation(loc);

    if (runtime.test("optionVerbose")) {
      System.err.println("[Voiding expression at " + loc.line + ":" +
                         loc.column + " in " + analyzer.current().qName + "]");
    }

    return result;
  }

  /** Visit the specified self-contained module. */
  public void visit(Module m) {
    // Initialize the per-grammar state.
    analyzer.register(this);
    analyzer.init(m);

    // Process the productioins.
    for (Production p : m.productions) analyzer.process(p);
  }

  /** Visit the specified full production. */
  public void visit(FullProduction p) {
    // Text-only and token-level productions are ignored.
    if (p.getBooleanProperty(Properties.TEXT_ONLY) ||
        p.getBooleanProperty(Properties.TOKEN)) {
      return;
    }

    isTopLevel     = true;
    isLastElement  = false;
    suppressVoided = AST.isVoid(p.type);
    hasValue       = false;

    p.choice       = (OrderedChoice)dispatch(p.choice);
  }

  /** Visit the specified ordered choice. */
  public Element visit(OrderedChoice c) {
    boolean top   = isTopLevel;
    isTopLevel    = false;
    boolean last  = isLastElement;
    boolean value = hasValue;

    if ((! top) && (! last)) {
      hasValue    = false;
    }

    final int size = c.alternatives.size();
    for (int i=0; i<size; i++) {
      isLastElement = top || last;
      c.alternatives.set(i, (Sequence)dispatch(c.alternatives.get(i)));
    }

    isLastElement  = false;
    Element result = c;
    if ((! top) && (! last)) {
      if (hasValue) {
        hasValue   = true;
      } else {
        hasValue   = value;
        if (! suppressVoided) {
          result   = wrap(result);
        }
      }
    }

    return result;
  }

  /** Visit the specified quantification. */
  public Element visit(Quantification q) {
    isTopLevel     = false;
    isLastElement  = true;
    boolean value  = hasValue;
    hasValue       = false;

    q.element      = (Element)dispatch(q.element);

    isLastElement  = false;
    Element result = q;
    if (hasValue) {
      hasValue     = true;
    } else {
      hasValue     = value;
      if (! suppressVoided) {
        result     = wrap(result);
      }
    }

    return result;
  }

  /** Visit the specified sequence. */
  public Element visit(Sequence s) {
    isTopLevel     = false;
    boolean   last = isLastElement;
    final int size = s.size();

    for (int i=0; i<size; i++) {
      isLastElement = last && (i == size-1);
      s.elements.set(i, (Element)dispatch(s.get(i)));
    }

    isLastElement = false;

    return s;
  }

  /** Visit the specified predicate. */
  public Element visit(Predicate p) {
    isTopLevel       = false;
    isLastElement    = true;
    boolean value    = hasValue;
    hasValue         = false;
    boolean suppress = suppressVoided;
    suppressVoided   = true;

    p.element        = (Element)dispatch(p.element);

    isLastElement    = false;
    hasValue         = value;
    suppressVoided   = suppress;

    return p;
  }

  /** Visit the specified voided element. */
  public VoidedElement visit(VoidedElement v) {
    isTopLevel       = false;
    isLastElement    = true;
    boolean value    = hasValue;
    hasValue         = false;
    boolean suppress = suppressVoided;
    suppressVoided   = true;

    v.element        = (Element)dispatch(v.element);

    isLastElement    = false;
    hasValue         = value;
    suppressVoided   = suppress;

    return v;
  }

  /** Visit the specified binding. */
  public Binding visit(Binding b) {
    isTopLevel       = false;
    isLastElement    = true;
    hasValue         = false;
    boolean suppress = suppressVoided;
    suppressVoided   = false;

    b.element        = (Element)dispatch(b.element);
    if (b.element instanceof VoidedElement) {
      runtime.error("binding for expression without value", b);
    }

    isLastElement    = false;
    hasValue         = true;
    suppressVoided   = suppress;

    return b;
  }

  /** Visit the specified string match. */
  public StringMatch visit(StringMatch m) {
    isTopLevel       = false;
    isLastElement    = true;
    hasValue         = false;
    boolean suppress = suppressVoided;
    suppressVoided   = false;

    m.element        = (Element)dispatch(m.element);
    if (m.element instanceof VoidedElement) {
      runtime.error("match for expression without value", m);
    }

    isLastElement    = false;
    hasValue         = true;
    suppressVoided   = suppress;

    return m;
  }

  /** Visit the specified parser action. */
  public ParserAction visit(ParserAction pa) {
    isTopLevel    = false;
    isLastElement = false;
    hasValue      = true;

    return pa;
  }

  /** Visit the specified action. */
  public Action visit(Action a) {
    isTopLevel    = false;
    isLastElement = false;

    if (a.setsValue()) {
      hasValue = true;
    }

    return a;
  }

  /** Visit the specified nonterminal. */
  public NonTerminal visit(NonTerminal nt) {
    isTopLevel    = false;
    isLastElement = false;

    FullProduction p = analyzer.lookup(nt);
    if (! AST.isVoid(p.type)) {
      hasValue = true;
    }

    return nt;
  }

  /**
   * Visit the specified element.  This method provides the default
   * implementation for terminals, parse tree nodes, null literals,
   * node markers, and value elements.
   */
  public Element visit(Element e) {
    isTopLevel    = false;
    isLastElement = false;
    hasValue      = true;
    return e;
  }

}
