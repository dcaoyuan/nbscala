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

import java.util.HashSet;
import java.util.Set;

import xtc.Constants;

import xtc.type.AST;

import xtc.util.Runtime;

/**
 * Visitor to turn the semantic value of a production to void.  This
 * visitor converts non-void productions whose semantic values are
 * never bound to void productions.  As a practical matter, it only
 * converts productions that do not contain (parser) actions.
 *
 * <p />This visitor assumes that the entire grammar is contained in a
 * single module.
 *
 * @author Robert Grimm
 * @version $Revision: 1.39 $
 */
public class ProductionVoider extends GrammarVisitor {

  /** Visitor to determine which productions are voidable. */
  public static class Tester extends GrammarVisitor {

    /** The flag for whether we are currently unmarking productions. */
    protected boolean secondPhase;

    /** The flag for whether a production may be voided. */
    protected boolean voidable;

    /**
     * Create a new tester.
     *
     * @param runtime The runtime.
     * @param analyzer The analyzer utility.
     */
    public Tester(Runtime runtime, Analyzer analyzer) {
      super(runtime, analyzer);
    }

    /**
     * Get the set of nonterminals corresponding to voidable
     * productions.  Note that this method must only be called after
     * visiting the corresponding grammar with this visitor.
     *
     * @return The set of voidable nonterminals.
     */
    public Set<NonTerminal> voidable() {
      return new HashSet<NonTerminal>(analyzer.marked());
    }

    /** Visit the specified grammar. */
    public Object visit(Module m) {
      // Initialize the per-grammar state.
      analyzer.register(this);
      analyzer.init(m);

      // On the first iteration, mark all productions that might be
      // voidable.
      secondPhase   = false;
      for (Production p : m.productions) {
        // Skip top-level productions and productions that are already
        // void.
        if (p.hasAttribute(Constants.ATT_PUBLIC) || AST.isVoid(p.type)) {
          continue;
        }

        // Clear the per-production state.
        voidable = true;

        // Process the production.
        analyzer.process(p);

        // Tabulate the results.
        if (voidable) {
          analyzer.mark(p.qName);
        }
      }

      // On the second iteration, eliminate those productions that are
      // bound.  Note that this time around we process all
      // productions.
      secondPhase = true;
      for (Production p : m.productions) {
        // Process the production.
        analyzer.process(p);
      }

      // Done.
      return null;
    }

    /** Visit the specified binding. */
    public Element visit(Binding b) {
      // We allow bindings in voidable productions, so that they can
      // contain semantic predicates.  However, we disallow bindings
      // to CodeGenerator.VALUE because they act like
      // programmer-specified bindings (e.g., yyValue can be
      // referenced in a semantic predicate) but are also constrained
      // in their type (i.e., yyValue always has the production's
      // type).
      if (CodeGenerator.VALUE.equals(b.name)) {
        voidable = false;
      }
      isBound   = true;
      b.element = (Element)dispatch(b.element);
      return b;
    }

    /** Visit the specified nonterminal. */
    public Element visit(NonTerminal nt) {
      if (secondPhase) {
        if (isBound) {
          Production p = analyzer.lookup(nt);
          if (analyzer.current() != p) {
            // Self-referential productions can still be voided.
            analyzer.unmark(p.qName);
          }
        }
      }
      isBound = false;
      return nt;
    }

    /** Visit the specified semantic predicate. */
    public Element visit(SemanticPredicate p) {
      isBound = false;
      return p;
    }

    /** Visit the specified action. */
    public Element visit(Action a) {
      isBound  = false;
      voidable = ! a.setsValue();
      return a;
    }

    /** Visit the specified parser action. */
    public Element visit(ParserAction pa) {
      isBound  = false;
      voidable = false;
      return pa;
    }

  }

  // =========================================================================

  /**
   * Create a new production voider.
   *
   * @param runtime The runtime.
   * @param analyzer The analyzer utility.
   */
  public ProductionVoider(Runtime runtime, Analyzer analyzer) {
    super(runtime, analyzer);
  }
  
  /** Visit the specified grammar. */
  public Object visit(Module m) {
    boolean changed;

    do {
      changed         = false;
      Tester tester   = new Tester(runtime, analyzer);
      tester.dispatch(m);
      Set    voidable = tester.voidable();
      
      analyzer.register(this);
      analyzer.init(m);

      for (Production p : m.productions) {
        // Only process voidable productions.
        if (! voidable.contains(p.qName)) {
          continue;
        }
        
        // Process the production.
        if (runtime.test("optionVerbose")) {
          System.err.println("[Voiding " + p.qName + "]");
        }
        analyzer.process(p);
        changed = true;
        
        // Patch the type.
        p.type = AST.VOID;
        p.setProperty(Properties.VOIDED, Boolean.TRUE);
      }
    } while (changed);

    // Done.
    return null;
  }

  /** Visit the specified voided element. */
  public Element visit(VoidedElement v) {
    v.element = (Element)dispatch(v.element);

    // The whole production is being voided; eliminate the voider.
    return v.element;
  }

  /** Visit the specified binding. */
  public Element visit(Binding b) {
    b.element = (Element)dispatch(b.element);

    // Remove bindings to synthetic variables, as they are only used
    // inside value elements.
    if (Analyzer.isSynthetic(b.name)) {
      return b.element;
    } else {
      return b;
    }
  }
  
  /** Visit the specified value element. */
  public Element visit(ValueElement e) {
    // All value elements become null value elements.
    return NullValue.VALUE;
  }
  
}
