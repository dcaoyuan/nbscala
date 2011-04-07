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

import xtc.Constants;

import xtc.type.AST;

import xtc.util.Runtime;

/**
 * Visitor to inline productions.  If cost-based inlining is enabled,
 * this visitor inlines productions with a {@link CostEstimator cost
 * estimate} less or equal to the {@link #MAX_COST maximum cost}.
 * However, to avoid changing a production's semantic value,
 * cost-based inlining is limited to void, text-only, and token-level
 * productions.  Independent of whether cost-based inlining is enabled
 * or not, this visitor inlines productions that are referenced from a
 * production consisting solely of a nonterminal (which, optionally,
 * may be bound to {@link CodeGenerator#VALUE}).  Note that generic
 * productions are never inlined; neither are productions with the
 * state attribute.
 *
 * <p />Note that, to be effective, this visitor requires that
 * text-only productions have been {@link TextTester marked} as such.
 * Similarly, token-level productions need to have beeen {@link
 * Tokenizer marked} as such.  Further note that, if this visitor
 * inlines productions, the resulting grammar needs to be {@link
 * Simplifier simplified} before further processing.  Also note that
 * this visitor may create new opportunities for the {@link
 * DeadProductionEliminator elimination of dead productions}.  This
 * visitor assumes that the entire grammar is contained in a single
 * module.
 *
 * @author Robert Grimm
 * @version $Revision: 1.53 $
 */
public class Inliner extends GrammarVisitor {

  /** The maximum cost for inlining productions at arbitrary positions. */
  public static final int MAX_COST = 1;

  /** The flag for whether to inline non-transient productions. */
  public static final boolean INLINE_PERSISTENT = true;

  /** Flag for whether the grammar has the state attribute. */
  protected boolean attributeState;

  /** Flag for whether this production inliner has inlined a production. */
  protected boolean inlined;

  /**
   * Create a new production inliner.
   *
   * @param runtime The runtime.
   * @param analyzer The analyzer utility.
   */
  public Inliner(Runtime runtime, Analyzer analyzer) {
    super(runtime, analyzer);
  }

  /**
   * Determine whether the specified production is void, text-only, or
   * token-level.
   *
   * @param p The production.
   * @return <code>true</code> if the production is either text-only
   *  or void.
   */
  protected boolean isBasic(final Production p) {
    return (AST.isVoid(p.type) || 
            p.getBooleanProperty(Properties.TEXT_ONLY) ||
            p.getBooleanProperty(Properties.TOKEN));
  }

  /**
   * Record that the specified production has been inlined and, if
   * necessary, print a message to the console.
   *
   * @param p The production.
   */
  protected void inlined(Production p) {
    inlined = true;

    if (runtime.test("optionVerbose")) {
      System.err.println("[Inlining " + p.qName + " into " +
                         analyzer.current().qName + "]");
    }
  }

  /**
   * Process the specified grammar.
   *
   * @param m The grammar module.
   * @return <code>Boolean.TRUE</code> if the grammar has been modified,
   *   otherwise <code>Boolean.FALSE</code>.
   */
  public Object visit(Module m) {
    CostEstimator cost    = null;
    boolean       changed = false;

    if (runtime.test("optimizeCost")) {
      cost = new CostEstimator(analyzer);
    }

    // Perform inlining until we reach a fixed-point.  We are
    // guaranteed to reach a fixed-point because the cost estimator's
    // estimate for a production includes the costs of any referenced
    // productions, thus marking all recursive productions with an
    // unlimited cost.
    do {
      // Annotate productions with their cost estimates.
      if (runtime.test("optimizeCost")) {
        cost.dispatch(m);
      }

      // Initialize the per-grammar state.
      analyzer.register(this);
      analyzer.init(m);
      attributeState = m.hasAttribute(Constants.ATT_STATEFUL.getName());
      inlined        = false;

      // Process the productions.
      for (Production p : m.productions) analyzer.process(p);

      // Did the grammar change?
      if (inlined) {
        changed = true;
      }
    } while (inlined);

    return (changed)? Boolean.TRUE : Boolean.FALSE;
  }

  /** Visit the specified nonterminal. */
  public Element visit(NonTerminal nt) {
    boolean bound = isBound;
    isBound       = false;

    // Get the referenced production.
    FullProduction p = analyzer.lookup(nt);

    if (Generifier.isGeneric(p) || AST.isList(p.type) ||
        p.hasAttribute(Constants.ATT_EXPLICIT) ||
        (attributeState &&
         (p.hasAttribute(Constants.ATT_STATEFUL) ||
          p.hasAttribute(Constants.ATT_RESETTING)))) {
      // Never inline generic or list-valued productions, nor
      // productions with the explicit, state, or reset attribute.
      return nt;
    }

    // Get the referenced production's expression.
    Element e = Analyzer.strip(p.choice);

    if (e instanceof NonTerminal) {
      // We only inline a lone nonterminal if the production is
      // transient.  That way, a lone nonterminal can be used to force
      // memoization of a transient production.
      if (! p.isMemoized() && ! p.hasAttribute(Constants.ATT_NO_INLINE)) {
        inlined(p);
        // Copy the nonterminal being inlined and update the copy's
        // location to the original nonterminal's location, so that
        // errors point to the right location.
        NonTerminal copy = new NonTerminal((NonTerminal)e);
        copy.setLocation(nt);
        return copy;
      } else {
        return nt;
      }

    } else if (e instanceof Binding) {
      Binding b  = (Binding)e;
      Element e2 = Analyzer.strip(b.element);

      if (CodeGenerator.VALUE.equals(b.name) &&
          (e2 instanceof NonTerminal)) {
        // We only inline a lone nonterminal if the production is
        // transient.  That way, a lone nonterminal can be used to
        // force memoization of a transient production.
        if (! p.isMemoized() && ! p.hasAttribute(Constants.ATT_NO_INLINE)) {
          inlined(p);
          // Copy the nonterminal being inlined and update the copy's
          // location to the original nonterminal's location, so that
          // errors point to the right location.
          NonTerminal copy = new NonTerminal((NonTerminal)e2);
          copy.setLocation(nt);
          return copy;
        } else {
          return nt;
        }
      }
    }

    if ((! (isBasic(analyzer.current()) &&
            (INLINE_PERSISTENT ||
             (! analyzer.current().isMemoized())))) ||
        bound) {
      // If the current production is not void or text-only or if the
      // nonterminal is bound, we don't do any other inlining.
      return nt;

    } else if (runtime.test("optimizeCost") &&
               (MAX_COST >= (Integer)p.getProperty(Properties.COST)) &&
               ! p.hasAttribute(Constants.ATT_NO_INLINE) &&
               (INLINE_PERSISTENT || (! p.isMemoized()))) {
      // If the referenced production's cost estimate is low enough,
      // inline the referenced production.
      inlined(p);
      return analyzer.copy(p.choice);

    } else {
      return nt;
    }
  }

}
