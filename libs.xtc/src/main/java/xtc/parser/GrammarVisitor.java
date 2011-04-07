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

import xtc.tree.Visitor;

import xtc.util.Runtime;

/**
 * The parent class of all grammar module visitors.  This class
 * provides a skeleton visitor for processing {@link Grammar grammars}
 * and self-contained {@link Module modules}, while maintaining a set
 * of useful flags.
 *
 * @author Robert Grimm
 * @version $Revision: 1.24 $
 */
public abstract class GrammarVisitor extends Visitor {

  /** The runtime. */
  protected final Runtime runtime;

  /** The analyzer utility. */
  protected final Analyzer analyzer;

  /**
   * Flag for whether the current element is the top-level element of
   * a production.
   */
  protected boolean isTopLevel;

  /** Flag for whether the current element is voided. */
  protected boolean isVoided;

  /** Flag for whether the current element is bound. */
  protected boolean isBound;

  /**
   * Flag for whether the current element is the last element of a
   * sequence.
   */
  protected boolean isLastElement;

  /** Flag for whether the current element is in a predicate. */
  protected boolean isPredicate;

  /** Flag for whether the current element is repeated at least once. */
  protected boolean isRepeatedOnce;

  /**
   * Flag for whether the parent element requires that the directly
   * embedded elements are sequences for {@link CodeGenerator code
   * generation}.  It is used by the {@link Simplifier} to avoid
   * stripping sequences that need to be restored later on again.
   */
  protected boolean needsSequence;

  /**
   * Flag for whether to transform ordered choices, repetition, and
   * options in place, instead of creating a new production.  This
   * flag is used by the {@link Transformer.Lifter lifter} visitor.
   */
  protected boolean transformInPlace;

  /**
   * Create a new grammar visitor.
   *
   * @param runtime The runtime.
   * @param analyzer The analyzer utility.
   */
  public GrammarVisitor(Runtime runtime, Analyzer analyzer) {
    this.runtime  = runtime;
    this.analyzer = analyzer;
  }

  /** Visit the specified grammar. */
  public Object visit(Grammar g) {
    // Initialize the per-grammar state.
    analyzer.register(this);
    analyzer.init(g);

    // Process the modules.
    for (Module m : g.modules) {
      analyzer.process(m);

      // Process the module's productions.
      for (Production p : m.productions) analyzer.process(p);
    }
    
    // Done.
    return null;
  }

  /** Visit the specified self-contained module. */
  public Object visit(Module m) {
    // Initialize the per-grammar state.
    analyzer.register(this);
    analyzer.init(m);

    // Process the productioins.
    for (Production p : m.productions) analyzer.process(p);

    // Done.
    return null;
  }

  /** Visit the specified production. */
  public Production visit(Production p) {
    Object closure   = analyzer.enter(p);

    // Initialize the per-production flags.
    isTopLevel       = true;
    isVoided         = false;
    isBound          = false;
    isLastElement    = false;
    isPredicate      = false;
    isRepeatedOnce   = false;
    needsSequence    = false;
    transformInPlace = false;

    p.choice         = (OrderedChoice)dispatch(p.choice);

    analyzer.exit(closure);

    return p;
  }

  /** Visit the specified ordered choice. */
  public Element visit(OrderedChoice c) {
    boolean top      = isTopLevel;
    isTopLevel       = false;
    isVoided         = false;
    isBound          = false;
    boolean last     = isLastElement;
    transformInPlace = false;

    final int length = c.alternatives.size();
    for (int i=0; i<length; i++) {
      isLastElement  = top || last;
      needsSequence  = true;
      c.alternatives.set(i, (Sequence)dispatch(c.alternatives.get(i)));
    }

    isLastElement    = false;
    needsSequence    = false;
    return c;
  }

  /** Visit the specified repetition. */
  public Element visit(Repetition r) {
    isTopLevel       = false;
    isVoided         = false;
    isBound          = false;
    isLastElement    = false;
    boolean rep      = isRepeatedOnce;
    isRepeatedOnce   = r.once;
    needsSequence    = false;
    transformInPlace = false;

    r.element        = (Element)dispatch(r.element);

    isRepeatedOnce   = rep;
    return r;
  }

  /** Visit the specified option. */
  public Element visit(Option o) {
    isTopLevel       = false;
    isVoided         = false;
    isBound          = false;
    isLastElement    = false;
    needsSequence    = false;
    transformInPlace = false;

    o.element        = (Element)dispatch(o.element);
    return o;
  }

  /** Visit the specified sequence. */
  public Element visit(Sequence s) {
    isTopLevel       = false;
    isVoided         = false;
    isBound          = false;
    boolean last     = isLastElement;
    needsSequence    = false;

    final int size = s.size();
    for (int i=0; i<size; i++) {
      isLastElement  = last && (i == size-1);
      s.elements.set(i, (Element)dispatch(s.get(i)));
    }

    isLastElement    = false;
    return s;
  }

  /** Visit the specified predicate. */
  public Element visit(Predicate p) {
    isTopLevel       = false;
    isVoided         = false;
    isBound          = false;
    isLastElement    = false;
    boolean seq      = needsSequence;
    needsSequence    = true;
    transformInPlace = false;
    boolean pred     = isPredicate;
    isPredicate      = true;

    p.element        = (Element)dispatch(p.element);

    isPredicate      = pred;
    needsSequence    = seq;
    return p;
  }

  /** Visit the specified semantic predicate. */
  public Element visit(SemanticPredicate p) {
    isTopLevel       = false;
    isVoided         = false;
    isBound          = false;
    isLastElement    = false;
    needsSequence    = false;
    transformInPlace = false;

    p.element        = (Element)dispatch(p.element);
    return p;
  }

  /** Visit the specified voided element. */
  public Element visit(VoidedElement v) {
    isTopLevel       = false;
    isVoided         = true;
    isBound          = false;
    isLastElement    = false;
    needsSequence    = false;
    transformInPlace = false;

    v.element        = (Element)dispatch(v.element);
    return v;
  }

  /** Visit the specified binding. */
  public Element visit(Binding b) {
    isTopLevel       = false;
    isVoided         = false;
    isBound          = true;
    isLastElement    = false;
    needsSequence    = false;
    transformInPlace = false;

    b.element        = (Element)dispatch(b.element);
    return b;
  }

  /** Visit the specified string match. */
  public Element visit(StringMatch m) {
    isTopLevel       = false;
    isVoided         = false;
    isBound          = true;
    isLastElement    = false;
    needsSequence    = false;
    transformInPlace = false;

    m.element        = (Element)dispatch(m.element);
    return m;
  }

  /** Visit the specified character case. */
  public CharCase visit(CharCase c) {
    isTopLevel       = false;
    isVoided         = false;
    isBound          = false;
    isLastElement    = false;
    needsSequence    = false;
    transformInPlace = false;

    c.element        = (Element)dispatch(c.element);
    return c;
  }

  /** Visit the specified character switch. */
  public Element visit(CharSwitch s) {
    isTopLevel       = false;
    isVoided         = false;
    isBound          = false;
    isLastElement    = false;
    needsSequence    = false;
    transformInPlace = false;

    final int length = s.cases.size();
    for (int i=0; i<length; i++) {
      s.cases.set(i, (CharCase)dispatch(s.cases.get(i)));
    }
    s.base           = (Element)dispatch(s.base);
    return s;
  }

  /** Visit the specified parser action. */
  public Element visit(ParserAction pa) {
    isTopLevel       = false;
    isVoided         = false;
    isBound          = false;
    isLastElement    = false;
    needsSequence    = false;
    transformInPlace = false;

    pa.element       = (Element)dispatch(pa.element);
    return pa;
  }

  /**
   * Visit the specified element.  This method provides the default
   * implementation for nonterminals, terminals (besides character
   * switches), node markers, actions, parse tree nodes, null
   * literals, and value elements.
   */
  public Element visit(Element e) {
    isTopLevel       = false;
    isVoided         = false;
    isBound          = false;
    isLastElement    = false;
    needsSequence    = false;
    transformInPlace = false;

    return e;
  }

}
