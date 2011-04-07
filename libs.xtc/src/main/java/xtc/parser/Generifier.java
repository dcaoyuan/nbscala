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

import java.util.ArrayList;
import java.util.List;

import xtc.tree.Visitor;

import xtc.type.AST;

import xtc.util.Runtime;
import xtc.util.Utilities;

/**
 * Visitor to add generic nodes as semantic values.
 *
 * <p />For any production with pseudotype "<code>generic</code>" that
 * does not contain any direct left-recursions (which is also called a
 * generic node production), this visitor adds the appropriate {@link
 * GenericNodeValue generic node value} elements, which create a
 * {@link xtc.tree.GNode generic node} as the productions' semantic
 * value.  The children of such a generic node are the matched
 * component values of the production, though voided elements, void
 * nonterminals, and character terminals are not included.  If an
 * alternative assigns {@link CodeGenerator#VALUE} either through a
 * binding or a semantic action, that alternative's semantic value is
 * the specified semantic value and not a newly generated generic
 * node.  This visitor requires that all nested choices that do not
 * appear as the last element in a sequence have been lifted.  It also
 * assumes that the entire grammar is contained in a single module.
 *
 * <p />Note that this visitor does not process generic productions
 * that contain direct left-recursions; they are processed by {@link
 * DirectLeftRecurser}.
 *
 * @author Robert Grimm
 * @version $Revision: 1.56 $
 */
public class Generifier extends Visitor {

  /** The marker for synthetic variables. */
  public static final String MARKER = "g";

  /** The runtime. */
  protected final Runtime runtime;

  /** The analyzer utility. */
  protected final Analyzer analyzer;

  /**
   * The list of variables representing the children of the generic
   * node to be created.
   */
  protected List<Binding> children;

  /** The list of node markers. */
  protected List<NodeMarker> markers;

  /**
   * Create a new generifier.
   *
   * @param runtime The runtime.
   * @param analyzer The analyzer utility.
   */
  public Generifier(Runtime runtime, Analyzer analyzer) {
    this.runtime  = runtime;
    this.analyzer = analyzer;
    this.children = new ArrayList<Binding>();
    this.markers  = new ArrayList<NodeMarker>();
  }

  /**
   * Create a binding for the specified element.  This method also
   * adds the name of the bound variable to the end of the list of
   * children.
   *
   * @param e The element to bind.
   * @return The corresponding binding.
   */
  protected Binding bind(Element e) {
    Binding b = new Binding(analyzer.variable(MARKER), e);
    children.add(b);
    return b;
  }

  /** Visit the specified grammar. */
  public void visit(Module m) {
    // Initialize the analyzer.
    analyzer.register(this);
    analyzer.init(m);

    // Process the productions.
    for (Production p : m.productions) {
      if (isGenericNode((FullProduction)p)) {
        analyzer.process(p);
      }
    }
  }

  /** Visit the specified production. */
  public void visit(FullProduction p) {
    // Process the production's element.
    p.choice = (OrderedChoice)dispatch(p.choice);

    // Patch the type (but only for dynamically typed productions).
    if (AST.isDynamicNode(p.type)) p.type = AST.NODE;

    // Mark the production as a generic node production.
    markGenericNode(p, runtime.test("optionVerbose"));
  }

  /** Visit the specified ordered choice. */
  public Element visit(OrderedChoice c) {
    // Process the alternatives.
    final int size = c.alternatives.size();
    for (int i=0; i<size; i++) {
      Sequence alternative = c.alternatives.get(i);

      // We only add generic node values to the current alternative if
      // that alternative does not contain any simple values, i.e.,
      // either bindings to CodeGenerator.VALUE or value elements.
      if (! Analyzer.setsValue(alternative, true)) {
        c.alternatives.set(i, (Sequence)dispatch(alternative));
      }
    }

    // Done.
    return c;
  }

  /** Visit the specified repetition. */
  public Element visit(Repetition r) {
    return bind(r);
  }

  /** Visit the specified option. */
  public Element visit(Option o) {
    return bind(o);
  }

  /** Visit the specified sequence. */
  public Element visit(Sequence s) {
    // Remember the current number of children and markers.
    final int base  = children.size();
    final int base2 = markers.size();

    // Process the elements of the sequence.
    final int size = s.size();
    for (int i=0; i<size; i++) {
      s.elements.set(i, (Element)dispatch(s.get(i)));
    }

    // If this sequence has not ended with a choice, add the
    // appropriate semantic value.
    if (! s.hasTrailingChoice()) {
      String name = analyzer.current().qName.name;
      if (! markers.isEmpty()) {
        name = Utilities.qualify(Utilities.getQualifier(name), 
                                 markers.get(markers.size()-1).name);
      }

      final List<Binding> formatting;
      if (s.hasProperty(Properties.FORMATTING)) {
        formatting = Properties.getFormatting(s);
      } else {
        formatting = new ArrayList<Binding>(0);
      }

      s.add(new GenericNodeValue(name, new ArrayList<Binding>(children),
                                 formatting));
    }

    // Remove any children and markers added by processing the sequence.
    if (0 == base) {
      children.clear();
    } else {
      children.subList(base, children.size()).clear();
    }

    if (0 == base2) {
      markers.clear();
    } else {
      markers.subList(base2, markers.size()).clear();
    }

    // Done.
    return s;
  }

  /** Visit the specified binding. */
  public Element visit(Binding b) {
    // Record the binding.
    children.add(b);

    // We assume that the bound expression does not require any
    // further processing.  I.e., if it is a repetition, option, or
    // choice, it already has been lifted and replaced by a
    // nonterminal.

    // Done.
    return b;
  }

  /** Visit the specified string match. */
  public Element visit(StringMatch m) {
    return bind(m);
  }

  /** Visit the specified nonterminal. */
  public Element visit(NonTerminal nt) {
    FullProduction p = analyzer.lookup(nt);
    if (AST.isVoid(p.type)) {
      return nt;
    } else {
      return bind(nt);
    }
  }

  /** Visit the specified string literal. */
  public Element visit(StringLiteral l) {
    return bind(l);
  }

  /** Visit the specified parse tree node. */
  public Element visit(ParseTreeNode n) {
    return bind(n);
  }

  /** Visit the specified null literal. */
  public Element visit(NullLiteral l) {
    return bind(l);
  }

  /** Visit the specified node marker. */
  public Element visit(NodeMarker m) {
    markers.add(m);
    return m;
  }

  /**
   * Visit the specified element.  This method provides the default
   * implementation for predicates, voided elements, character
   * terminals, (parser) actions, and value elements.
   */
  public Element visit(Element e) {
    return e;
  }

  /**
   * Mark the specified production as a generic node production.
   *
   * @param p The production.
   * @param verbose The flag for whether to be verbose.
   */
  public static void markGenericNode(FullProduction p, boolean verbose) {
    if (verbose) {
      System.err.println("[Recognizing " + p.qName + " as generic node]");
    }
    p.setProperty(Properties.GENERIC, Properties.GENERIC_NODE);
  }

  /**
   * Mark the specified production as a generic recursion production.
   *
   * @param p The production.
   * @param verbose The flag for whether to be verbose.
   */
  public static void markGenericRecursion(FullProduction p, boolean verbose) {
    if (verbose) {
      System.err.println("[Recognizing " + p.qName + " as generic recursion]");
    }
    p.setProperty(Properties.GENERIC, Properties.GENERIC_RECURSION);
  }

  /**
   * Determine whether the specified production is a generic node or a
   * generic recursion production.
   *
   * @param p The production.
   * @return <code>true</code> if the specified production is a generic
   *   node or generic recursion production.
   */
  public static boolean isGeneric(FullProduction p) {
    if (p.hasProperty(Properties.GENERIC)) {
      Object value = p.getProperty(Properties.GENERIC);

      return (Properties.GENERIC_NODE.equals(value) ||
              Properties.GENERIC_RECURSION.equals(value));
    } else {
      return AST.isGenericNode(p.type);
    }
  }

  /**
   * Determine whether the specified production is a generic node
   * production.  A production is a generic node production if its
   * semantic value is an automatically generated generic node with
   * the component values as its children.
   *
   * @param p The production.
   * @return <code>true</code> if the specified production is
   *   a generic node production.
   */
  public static boolean isGenericNode(FullProduction p) {
    if (p.hasProperty(Properties.GENERIC)) {
      return Properties.GENERIC_NODE.equals(p.getProperty(Properties.GENERIC));
    } else {
      return (AST.isGenericNode(p.type) &&
              (! DirectLeftRecurser.isTransformable(p)));
    }
  }

  /**
   * Determine whether the specified production is a generic recursion
   * production.  A production is a generic recursion production if
   * its semantic value is an automatically generated generic node and
   * the production, as specified, contains one or more direct
   * left-recursions that can automatically be transformed into the
   * corresponding right-recursions.
   *
   * @see DirectLeftRecurser
   *
   * @param p The production.
   * @return <code>true</code> if the specified production is
   *   a generic recursion production.
   */
  public static boolean isGenericRecursion(FullProduction p) {
    if (p.hasProperty(Properties.GENERIC)) {
      return Properties.GENERIC_RECURSION.
        equals(p.getProperty(Properties.GENERIC));
    } else {
      return (AST.isGenericNode(p.type) &&
              DirectLeftRecurser.isTransformable(p));
    }
  }

}
