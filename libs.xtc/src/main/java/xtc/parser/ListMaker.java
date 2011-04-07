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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import xtc.tree.Visitor;

import xtc.type.AST;
import xtc.type.Type;
import xtc.type.Wildcard;

import xtc.util.Runtime;

/**
 * Visitor to add lists as semantic values.
 *
 * @author Robert Grimm
 * @version $Revision: 1.9 $
 */
public class ListMaker extends Visitor {

  /** The marker for synthetic variables. */
  public static final String MARKER = "l";

  /** The runtime. */
  protected final Runtime runtime;

  /** The analyzer. */
  protected final Analyzer analyzer;

  /** The type operations. */
  protected final AST ast;

  /** The current production's element type. */
  protected Type element;

  /** The list of elements. */
  protected List<Element> elements;

  /**
   * Create a new list maker.
   *
   * @param runtime The runtime.
   * @param analyzer The analyzer.
   * @param ast The type operations.
   */
  public ListMaker(Runtime runtime, Analyzer analyzer, AST ast) {
    this.runtime  = runtime;
    this.analyzer = analyzer;
    this.ast      = ast;
    elements      = new ArrayList<Element>();
  }

  /** Visit the specified module. */
  public void visit(Module m) {
    // Initialize the per-grammar state.
    analyzer.register(this);
    analyzer.init(m);
    elements.clear();

    // Process the productions.
    for (Production p : m.productions) {
      if (AST.isList(p.type)) {
        // Initialize the element type.
        if (runtime.test("optionVariant") &&
            AST.isDynamicNode(AST.getArgument(p.type))) {
          element = Wildcard.TYPE;
        } else {
          element = null;
        }

        // Actually process the production.
        analyzer.process(p);

        // Patch the production's type.
        if ((null != element) && ! element.isError()) {
          p.type = AST.listOf(ast.concretize(element, AST.NULL_NODE));
        }
      }
    }
  }

  /** Visit the specified full production. */
  public void visit(FullProduction p) {
    dispatch(p.choice);
  }

  /** Visit the specified choice. */
  public void visit(OrderedChoice c) {
    for (Sequence alt : c.alternatives) dispatch(alt);
  }

  /** Visit the specified sequence. */
  public void visit(Sequence s) {
    // Remember the current number of elements.
    final int base = elements.size();

    // Process the elements of the sequence.
    for (Iterator<Element> iter = s.elements.iterator(); iter.hasNext(); ) {
      Element e = iter.next();

      if ((! iter.hasNext()) && (e instanceof OrderedChoice)) {
        // Continue with the trailing choice.
        dispatch(e);
      } else {
        // Add the current element to the list of traversed elements.
        elements.add(e);
      }
    }

    // If we have no trailing choice and the elements do not set the
    // semantic value, we need to add a semantic value.
    if ((! s.hasTrailingChoice()) && (! Analyzer.setsValue(elements, false))) {
      List<Binding> bindings = new ArrayList<Binding>();

      // Iterate over the elements and collect all bindable elements.
      for (int i=0; i<elements.size(); i++) {
        Element e = elements.get(i);

        if (e instanceof Binding) {
          bindings.add((Binding)e);
        } else if (analyzer.isBindable(e)) {
          Binding b = new Binding(analyzer.variable(MARKER), e);
          elements.set(i, b);
          bindings.add(b);
        }
      }

      // Update the element type.
      if ((null != element) && ! element.isError()) {
        for (Binding b : bindings) {
          Type t = analyzer.type(b.element);
          if (AST.isList(t)) t = AST.getArgument(t);

          Type u = ast.unify(element, t, true);
          if (u.isError()) {
            runtime.error("unable to determine consistent list element type", s);
            runtime.errConsole().loc(s).p(": error: 1st type is '");
            ast.print(element, runtime.errConsole(), false, true, null);
            runtime.errConsole().pln("'");
            runtime.errConsole().loc(s).p(": error: 2nd type is '");
            ast.print(t, runtime.errConsole(), false, true, null);
            runtime.errConsole().pln("'").flush();
          }
          element = u;
        }
      }

      // Ensure the alternative has a semantic value.
      if (bindings.isEmpty()) {
        // An empty list value.
        s.add(EmptyListValue.VALUE);
      } else if ((1 == bindings.size()) &&
                 AST.isList(analyzer.type(bindings.get(0).element))) {
        // The only binding already has a list value.  Pass it through.
        Binding b = bindings.get(0);
        if (Analyzer.isSynthetic(b.name)) {
          // Rename the binding.
          b.name = CodeGenerator.VALUE;
        } else {
          // Preserve the user-specified variable name.  Note that the
          // bindings name cannot be yyValue due to the
          // Analyzer.setValue() test above.
          s.add(new BindingValue(b));
        }

      } else {
        // Check whether the last binding has a list value.
        Binding last = bindings.get(bindings.size()-1);

        if (AST.isList(analyzer.type(last.element))) {
          bindings.remove(bindings.size()-1);
          s.add(new ProperListValue(analyzer.current().type, bindings, last));
        } else {
          s.add(new ProperListValue(analyzer.current().type, bindings, null));
        }
      }
    }

    // Patch back any added binding.
    int size = s.size();

    if (s.hasTrailingChoice() ||
        ((0 != size) && (s.get(size-1) instanceof ValueElement))) {
      // Ignore trailing choices and value elements.
      size--;
    }

    for (int i=0; i<size; i++) {
      Element e = elements.get(base + i);
      if (s.get(i) != e) s.elements.set(i, e);
    }

    // Remove any elements added by processing the sequence.
    if (0 == base) {
      elements.clear();
    } else {
      elements.subList(base, elements.size()).clear();
    }
  }

}
