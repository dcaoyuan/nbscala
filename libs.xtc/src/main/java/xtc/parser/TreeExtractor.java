/*
 * xtc - The eXTensible Compiler
 * Copyright (C) 2007 Robert Grimm
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
 * Visitor to pear down a grammar to the structure of its abstract
 * syntax tree.  This visitor assumes that the entire grammar is
 * contained in a single module.  It also assumes that directly
 * left-recursive generic productions have <em>not</em> been
 * transformed by {@link DirectLeftRecurser}.  Note that, after this
 * visitor has processed the grammar, the grammar violates the code
 * generator's requirements.  Also note that this visitor internally
 * uses {@link Simplifier} and {@link DeadProductionEliminator}.
 *
 * @author Robert Grimm
 * @version $Revision: 1.11 $
 */
public class TreeExtractor extends Visitor {

  /** The runtime. */
  protected final Runtime runtime;

  /** The analyzer utility. */
  protected final Analyzer analyzer;

  /** The common type operations. */
  protected final AST ast;

  /** The flag for removing all elements from lexical productions. */
  protected final boolean keepLexical;

  /** The flag for whether the current production is generic. */
  protected boolean isGeneric;

  /** The flag for whether the current production is list-valued. */
  protected boolean isList;

  /** The flag for whether the current production is text-only. */
  protected boolean isTextOnly;

  /** The flag for whether the current production is token-level. */
  protected boolean isToken;

  /**
   * The flag for whether the current production defines the semantic
   * value in an explicit action.
   */
  protected boolean setsValue;

  /**
   * Create a new tree extractor.
   *
   * @param runtime The runtime.
   * @param analyzer The analyzer utility.
   * @param ast The type operations.
   * @param keepLexical The flag for keeping elements of text-only
   *   or token-level productions.
   */
  public TreeExtractor(Runtime runtime, Analyzer analyzer, AST ast,
                       boolean keepLexical) {
    this.runtime     = runtime;
    this.analyzer    = analyzer;
    this.ast         = ast;
    this.keepLexical = keepLexical;
  }

  /** Visit the specified module. */
  public void visit(Module m) {
    // Initialize the per-grammar state.
    analyzer.register(this);
    analyzer.init(m);

    // Process the productions.
    for (Production p : m.productions) {
      analyzer.process(p);
    }

    // Simplify the grammar and eliminate any dead productions.
    new Simplifier(runtime, analyzer).dispatch(m);
    new DeadProductionEliminator(runtime, analyzer).dispatch(m);

    // Eliminate any actions and attributes.
    m.header     = null;
    m.body       = null;
    m.footer     = null;
    m.attributes = null;

    // Eliminate the production's attributes and internal types.
    for (Production p : m.productions) {
      p.attributes = null;
      p.type       = null;
    }
  }

  /** Visit the specified full production. */
  public void visit(FullProduction p) {
    // Initialize the per-production state.
    isGeneric  = Generifier.isGeneric(p);
    isList     = AST.isList(p.type);
    isTextOnly = p.getBooleanProperty(Properties.TEXT_ONLY);
    isToken    = p.getBooleanProperty(Properties.TOKEN);
    setsValue  = false;

    // Preprocess directly left-recursive generic productions.
    if (isGeneric && DirectLeftRecurser.isTransformable(p)) {
      for (Sequence s : p.choice.alternatives) {
        if (! DirectLeftRecurser.isRecursive(s, p)) {
          Binding b = analyzer.bind(s.elements);
          if (null != b) b.name = CodeGenerator.VALUE;
        }
      }
    }

    // Process the choice.
    if ((isTextOnly || isToken) && (! keepLexical)) {
      p.choice = new OrderedChoice(new Sequence());
      p.setProperty(Properties.REDACTED, Boolean.TRUE);
    } else {
      dispatch(p.choice);
    }

    // Patch the type.
    final String s = ast.extern(p.type);
    if (isGeneric) {
      p.dType = "define<Node>";
    } else if (isTextOnly) {
      p.dType = "define<String>";
    } else if (isToken) {
      p.dType = "define<Token>";
    } else if (isList) {
      p.dType = "define<" + s + '>';
    } else if (setsValue) {
      p.dType = "define<" + s + '>';
    } else {
      p.dType = "expand<" + s + '>';
    }
  }

  /** Visit the specified ordered choice. */
  public void visit(OrderedChoice c) {
    for (Sequence alt : c.alternatives) dispatch(alt);
  }

  /** Visit the specified sequence. */
  public void visit(Sequence s) {
    // Eliminate the sequence name.
    s.name = null;

    // Process the elements.
    for (int i=0; i<s.size(); i++) {
      Element e = s.get(i);

      // If the element is a trailing choice, process its sequences.
      if ((s.size()-1 == i) && (e instanceof OrderedChoice)) {
        dispatch(e);
        continue;
      }

      // If the element is a voided element, semantic predicate, or
      // value element, remove the element and move on to the next
      // element.
      if ((e instanceof VoidedElement) ||
          (e instanceof SemanticPredicate) ||
          (e instanceof ValueElement)) {
        s.elements.remove(i);
        i--;
        continue;
      }

      // If the element is an action that sets the semantic value,
      // simplify it.  Otherwise, remove the action.
      if (e instanceof Action) {
        Action a = (Action)e;

        if (a.setsValue()) {
          setsValue = true;

          a.code.clear();
          a.indent.clear();

          a.code.add("yyValue = ...");
          a.indent.add(0);

        } else {
          s.elements.remove(i);
          i--;
        }
        continue;
      }

      // Process bindings.  In generic productions, we get rid of
      // bindings unless they bind yyValue (since they indicate a
      // passed-through value).  In productions that are neither
      // generic, text-valued, or text-only, we get rid of elements if
      // they are not bound (since they cannot contribute to a
      // semantic value).
      if (e instanceof Binding) {
        Binding b = (Binding)e;

        if ((! isGeneric) ||
            (isGeneric && (! CodeGenerator.VALUE.equals(b.name)))) {
          e = b.element;
          s.elements.set(i, e);
        }
        // Continue processing the element.

      } else if ((! isGeneric) && (! isList) && (! isTextOnly) && (! isToken)) {
        s.elements.remove(i);
        i--;
        continue;
      }

      // Remove any void nonterminals.
      if (e instanceof NonTerminal) {
        FullProduction p = analyzer.lookup((NonTerminal)e);

        if (AST.isVoid(p.type)) {
          s.elements.remove(i);
          i--;
          continue;
        }
      }

      // Process the element.
      dispatch(e);
    }
  }

  /** Visit the specified character case. */
  public void visit(CharCase c) {
    dispatch(c.element);
  }

  /** Visit the specified character switch. */
  public void visit(CharSwitch s) {
    for (CharCase c : s.cases) {
      dispatch(c);
    }
    dispatch(s.base);
  }

  /** Visit the specified unary operator. */
  public void visit(UnaryOperator op) {
    dispatch(op.element);

    // Strip any unnecessary choices and sequences.
    op.element = Analyzer.strip(op.element);
  }

  /** Visit the specified element. */
  public void visit(Element e) {
    // Nothing to do.
  }

}
