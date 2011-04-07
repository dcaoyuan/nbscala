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

import java.util.ArrayList;
import java.util.List;

import xtc.Constants;

import xtc.tree.Visitor;

import xtc.type.AST;

import xtc.util.Runtime;

/**
 * Visitor to recognize token-level productions.
 *
 * <p />This visitor recognizes the boundary between hierarchical and
 * lexical syntax:
 *
 * <li>This visitor recognizes all lexical productions.  A production
 * is lexical if it is text-only or if it is void and only references
 * other lexical productions (if any).  As a result, a lexical
 * production may not contain parser actions, semantic actions that
 * reference {@link CodeGenerator#VALUE}, or bindings to {@link
 * CodeGenerator#VALUE}.</li>
 *
 * <li>This visitor traverses the grammar, starting with its public
 * productions.  When encountering a lexical production it does not
 * traverse into the production; rather, if the lexical production
 * also consumes the input, this visitor marks the production as
 * token-level.</li>
 *
 * <li>This visitor ensures that all void productions are correctly
 * annotated with the {@link Properties#CONSUMER} property, indicating
 * whether they consume the input.</li>
 *
 * </ol>
 *
 * <p />This visitor does <em>not</em> change a token-level
 * production's type, so that later parser generator phases can still
 * distinguish between productions that used to be text-only and that
 * used to be void.  It does however, remove any {@link
 * Properties#TEXT_ONLY} property.
 *
 * <p />This visitor assumes that the entire grammar is contained in a
 * single module and that text-only productions have been marked as
 * such.  It may perform faster if the grammar has been annotated with
 * its real root.
 *
 * @see TextTester
 * @see RootFinder
 *
 * @author Robert Grimm
 * @version $Revision: 1.12 $
 */
public class Tokenizer extends GrammarVisitor {

  /** Visitor to determine which productions are lexical. */
  public static class Tester extends Visitor {

    /** The runtime. */
    protected final Runtime runtime;
    
    /** The analyzer utility. */
    protected final Analyzer analyzer;
    
    /** The flag for whether the current production is lexical. */
    protected boolean isLexical;
    
    /**
     * Create a new lexical tester.
     *
     * @param runtime The runtime.
     * @param analyzer The analyzer utility.
     */
    public Tester(Runtime runtime, Analyzer analyzer) {
      this.runtime  = runtime;
      this.analyzer = analyzer;
    }

    /**
     * Mark the specified production as lexical.
     *
     * @param p The production.
     */
    protected void mark(Production p) {
      if (runtime.test("optionVerbose")) {
        System.err.println("[Recognizing " + p.qName + " as lexical syntax]");
      }
      p.setProperty(Properties.LEXICAL, Boolean.TRUE);
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
        } else if (p.getBooleanProperty(Properties.TEXT_ONLY)) {
          mark(p);
          analyzer.processed(p.qName);
          continue;
        } else if (! AST.isVoid(p.type)) {
          analyzer.processed(p.qName);
          continue;
        }
        
        // Clear the per-production state.
        isLexical = true;
        
        // Process the production.
        analyzer.process(p);
        
        // Tabulate the results.
        if (isLexical) {
          // All visited productions are guaranteed to be lexical.
          for (NonTerminal nt : analyzer.working()) {
            // This lookup is guaranteed to work, as the production's
            // fully qualified name was added by visit(Production).
            Production p2 = analyzer.lookup(nt);
            mark(p2);
            analyzer.processed(p2.qName);
          }
          
        } else {
          // We only know that the current production is not lexical.
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
        if (! isLexical) {
          // We don't need to look any further.
          return;
        }
      }
    }
    
    /** Visit the specified sequence. */
    public void visit(Sequence s) {
      for (Element e : s.elements) {
        dispatch(e);
        if (! isLexical) {
          // We don't need to look any further.
          return;
        }
      }
    }
    
    /** Visit the specified semantic predicate. */
    public void visit(SemanticPredicate p) {
      // Ignore the semantic action.
    }
    
    /** Visit the specified binding. */
    public void visit(Binding b) {
      // We allow bindings in lexical productions, so that they can
      // contain semantic predicates.  However, we disallow a binding to
      // CodeGenerator.VALUE.
      if (CodeGenerator.VALUE.equals(b.name)) {
        isLexical = false;
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
        isLexical = false;
        return;
      }
      
      if (null == p) {
        // No such production. We assume the worst.
        isLexical = false;
        
      } else if (analyzer.isProcessed(p.qName)) {
        // If the corresponding production has already been processed,
        // make sure it is lexical.
        if (! p.getBooleanProperty(Properties.LEXICAL)) {
          isLexical = false;
        }
        
      } else if (! analyzer.isBeingWorkedOn(p.qName)) {
        // The production has not been processed and is not yet under
        // consideration.  If is text-only, accept it.  If it is void,
        // check it out.
        if (p.getBooleanProperty(Properties.TEXT_ONLY)) {
          // Nothing to do.
        } else if (AST.isVoid(p.type)) {
          dispatch(p);
        } else {
          isLexical = false;
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
        if (! isLexical) {
          // We don't need to look any further.
          return;
        }
      }
      dispatch(s.base);
    }
    
    /** Visit the specified terminal. */
    public void visit(Terminal t) {
      // Nothing to do. Terminals are lexical.
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
      isLexical = false;
    }
    
    /** Visit the specified action. */
    public void visit(Action a) {
      if (a.setsValue()) isLexical = false;
    }
    
    /** Visit the specified parser action. */
    public void visit(ParserAction pa) {
      isLexical = false;
    }
    
    /**
     * Visit the specified element. This method provides the default
     * implementation for parse tree nodes and value elements.
     */
    public void visit(Element e) {
      isLexical = false;
    }
    
  }

  // ==========================================================================

  /**
   * Create a new tokenizer.
   *
   * @param runtime The runtime.
   * @param analyzer The analyzer utility.
   */
  public Tokenizer(Runtime runtime, Analyzer analyzer) {
    super(runtime, analyzer);
  }

  /** Visit the specified grammar. */
  public Object visit(Module m) {
    // Recognize lexical syntax first.
    new Tester(runtime, analyzer).dispatch(m);

    // Initialize the per-grammar state.
    analyzer.register(this);
    analyzer.init(m);

    // Make sure that all lexical and void productions are tested for
    // whether they consume the input.
    for (Production p : m.productions) {
      if (p.getBooleanProperty(Properties.LEXICAL) || AST.isVoid(p.type)) {
        analyzer.notWorkingOnAny();
        analyzer.consumesInput(p.qName);
      }
    }

    // Determine which productions to process.
    List<Production> todo;
    if (m.hasProperty(Properties.ROOT)) {
      todo = new ArrayList<Production>(1);
      todo.add(analyzer.lookup((NonTerminal)m.getProperty(Properties.ROOT)));
    } else {
      todo = m.productions;
    }

    // Process the productions.
    for (Production p : todo) {
      // Skip processed or non-public productions.
      if (analyzer.isProcessed (p.qName) ||
          (! p.hasAttribute(Constants.ATT_PUBLIC))) {
        continue;
      }

      // Mark production as processed to avoid recursive processing.
      analyzer.processed(p.qName);

      if (p.getBooleanProperty(Properties.LEXICAL)) {
        // We have reached a lexical production.  If it consumes the
        // input, we mark it as token-level.
        analyzer.notWorkingOnAny();
        if (analyzer.consumesInput(p.qName)) {
          markToken(p, runtime.test("optionVerbose"));
        }
      } else {
        // Recurse into the production.
        analyzer.process(p);
      }
    }

    // Done.
    return null;
  }

  /** Visit the specified nonterminal. */
  public Element visit(NonTerminal nt) {
    FullProduction p = analyzer.lookup(nt);

    if (! analyzer.isProcessed(p.qName)) {
      analyzer.processed(p.qName);
      if (p.getBooleanProperty(Properties.LEXICAL)) {
        if (analyzer.consumesInput(nt)) {
          markToken(p, runtime.test("optionVerbose"));
        }
      } else {
        dispatch(p);
      }
    }

    return nt;
  }

  /**
   * Mark the specified production as token-level.  This method sets
   * the specified production's {@link Properties#TOKEN} property and
   * removes any {@link Properties#TEXT_ONLY} property.  It does,
   * however, <em>not</em> adjust the production's type to
   * <code>Token</code>.
   *
   * @param p The production.
   */
  public static void markToken(Production p, boolean verbose) {
    if (verbose) {
      System.err.println("[Recognizing " + p.qName + " as token-level]");
    }
    p.setProperty(Properties.TOKEN, Boolean.TRUE);
    p.removeProperty(Properties.TEXT_ONLY);
  }

}
