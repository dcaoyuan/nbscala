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

import xtc.tree.Visitor;

import xtc.type.AST;

import xtc.util.Runtime;

/**
 * Visitor to identify text-only productions.  A production is
 * text-only if it does not define a semantic value beyond declaring a
 * string value and references only other text-only productions (if
 * any).  Notably, a text-only production may not contain parser
 * actions, semantic actions that reference {@link
 * CodeGenerator#VALUE}, or bindings to {@link CodeGenerator#VALUE}.
 * Note that this visitor only detects such productions but does not
 * add appropriate value elements.
 *
 * @see Transformer
 *
 * @author Robert Grimm
 * @version $Revision: 1.42 $
 */
public class TextTester extends Visitor {

  /** The runtime. */
  protected final Runtime runtime;

  /** The analyzer utility. */
  protected final Analyzer analyzer;

  /** Flag for whether the current production is text-only. */
  protected boolean isTextOnly;

  /**
   * Create a new text tester.
   *
   * @param runtime The runtime.
   * @param analyzer The analyzer utility.
   */
  public TextTester(Runtime runtime, Analyzer analyzer) {
    this.runtime  = runtime;
    this.analyzer = analyzer;
  }

  /** Visit the specified grammar. */
  public void visit(Grammar g) {
    // Initialize the per-grammar state.
    analyzer.register(this);
    analyzer.init(g);

    // Process the modules.
    for (Module m : g.modules) {
      analyzer.process(m);

      for (Production p : m.productions) {
        // Make sure that the production is full, has not been
        // processed already, and returns a string.
        if ((! p.isFull()) || analyzer.isProcessed(p.qName)) {
          continue;
        } else if (! AST.isString(p.type)) {
          analyzer.processed(p.qName);
          continue;
        }
        
        // Clear the per-production state.
        isTextOnly = true;
        
        // Process the production.
        analyzer.process(p);
        
        // Tabulate the results.
        if (isTextOnly) {
          // All visited productions are guaranteed to be text-only.
          for (NonTerminal nt : analyzer.working()) {
            Production p2 = analyzer.lookupGlobally(nt);
            markTextOnly(p2, runtime.test("optionVerbose"));
            analyzer.processed(p2.qName);
          }
          
        } else {
          // We only know that the current production is not text-only.
          analyzer.processed(p.qName);
        }
      }
    }
  }

  /** Visit the specified grammar. */
  public void visit(Module m) {
    // Initialize the per-grammar state.
    analyzer.register(this);
    analyzer.init(m);
    
    // Process the productions.
    for (Production p : m.productions) {
      // Make sure that the production has not been processed
      // already and that it returns a string.
      if (analyzer.isProcessed(p.qName)) {
        continue;
      } else if (! AST.isString(p.type)) {
        analyzer.processed(p.qName);
        continue;
      }
      
      // Clear the per-production state.
      isTextOnly = true;
      
      // Process the production.
      analyzer.process(p);
      
      // Tabulate the results.
      if (isTextOnly) {
        // All visited productions are guaranteed to be text-only.
        for (NonTerminal nt : analyzer.working()) {
          // This lookup is guaranteed to work, as the production's
          // fully qualified name was added by visit(Production).
          Production p2 = analyzer.lookup(nt);

          markTextOnly(p2, runtime.test("optionVerbose"));
          analyzer.processed(p2.qName);
        }
        
      } else {
        // We only know that the current production is not text-only.
        analyzer.processed(p.qName);
      }
    }
  }
  
  /** Visit the specified production. */
  public void visit(Production p) {
    Object closure = analyzer.enter(p);
    analyzer.workingOn(p.qName);
    dispatch(p.choice);
    analyzer.exit(closure);
  }
  
  /** Visit the specified ordered choice. */
  public void visit(OrderedChoice c) {
    for (Sequence alt : c.alternatives) {
      dispatch(alt);
      if (! isTextOnly) {
        // We don't need to look any further.
        return;
      }
    }
  }
  
  /** Visit the specified sequence. */
  public void visit(Sequence s) {
    for (Element e : s.elements) {
      dispatch(e);
      if (! isTextOnly) {
        // We don't need to look any further.
        return;
      }
    }
  }
  
  /** Visit the specified predicate. */
  public void visit(Predicate p) {
    // Ignore the predicate.
  }
  
  /** Visit the specified binding. */
  public void visit(Binding b) {
    // We allow bindings in text-only productions, so that they can
    // contain semantic predicates.  However, we disallow a binding
    // to CodeGenerator.VALUE.
    if (CodeGenerator.VALUE.equals(b.name)) {
      isTextOnly = false;
    } else {
      dispatch(b.element);
    }
  }

  /** Visit the specified nonterminal. */
  public void visit(NonTerminal nt) {
    Production p;

    try {
      p = analyzer.lookup(nt);
    } catch (IllegalArgumentException x) {
      // Too many productions. We assume the worst.
      isTextOnly = false;
      return;
    }

    if (null == p) {
      // No such production. We assume the worst.
      isTextOnly = false;
    
    } else if (analyzer.isProcessed(p.qName)) {
      // If the corresponding production has already been processed,
      // make sure it is text-only.
      if (! p.getBooleanProperty(Properties.TEXT_ONLY)) {
        isTextOnly = false;
      }
      
    } else if (! analyzer.isBeingWorkedOn(p.qName)) {
      // The production has not been processed and is not yet under
      // consideration. If it returns a string, check it out.
      if (AST.isString(p.type)) {
        dispatch(p);
      } else {
        isTextOnly = false;
      }
    }
  }

  /** Visit the specified character case. */
  public void visit(CharCase c) {
    dispatch(c.element);
  }

  /** Visit the specified character switch. */
  public void visit(CharSwitch s) {
    for (CharCase kase : s.cases) {
      dispatch(kase);
      if (! isTextOnly) {
        // We don't need to look any further.
        return;
      }
    }
    dispatch(s.base);
  }
 
  /** Visit the specified terminal. */
  public void visit(Terminal t) {
    // Nothing to do. Terminals are text-only.
  }
  
  /**
   * Visit the specified unary operator. This method provides the
   * default implementation for repetitions, options, syntactic
   * predicates, voided elements, and string matches.
   */
  public void visit(UnaryOperator op) {
    dispatch(op.element);
  }

  /** Visit the specified null literal. */
  public void visit(NullLiteral l) {
    // Nothing to do.
  }

  /** Visit the specified node marker. */
  public void visit(NodeMarker m) {
    isTextOnly = false;
  }

  /**
   * Visit the specified action.
   *
   */
  public void visit(Action a) {
    if (a.setsValue()) isTextOnly = false;
  }

  /** Visit the specified parser action. */
  public void visit(ParserAction pa) {
    isTextOnly = false;
  }
  
  /**
   * Visit the specified element. This method provides the default
   * implementation for parse tree nodes and value elements.
   */
  public void visit(Element e) {
    isTextOnly = false;
  }
  
  /**
   * Mark the specified production as text-only.
   *
   * @param p The production.
   * @param verbose The flag for whether to be verbose.
   */
  public static void markTextOnly(Production p, boolean verbose) {
    if (verbose) {
      System.err.println("[Recognizing " + p.qName + " as text-only]");
    }
    p.setProperty(Properties.TEXT_ONLY, Boolean.TRUE);
  }

}

