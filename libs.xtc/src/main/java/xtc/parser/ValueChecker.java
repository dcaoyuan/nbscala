/*
 * xtc - The eXTensible Compiler
 * Copyright (C) 2006-2007 Robert Grimm
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

import xtc.util.Runtime;

/**
 * Visitor to ensure that every alternative has a semantic value.
 * This visitor assumes that the entire grammar is contained in a
 * single module and that any automatically deduced semantic values,
 * including value elements, have been added to the grammar.
 *
 * @author Robert Grimm
 * @version $Revision: 1.12 $
 */
public class ValueChecker extends Visitor {

  /** The runtime. */
  protected Runtime runtime;

  /** The analyzer. */
  protected Analyzer analyzer;

  /** The list of elements. */
  protected List<Element> elements;

  /**
   * Create a new value checker.
   *
   * @param runtime The runtime.
   * @param analyzer The analyzer.
   */
  public ValueChecker(Runtime runtime, Analyzer analyzer) {
    this.runtime  = runtime;
    this.analyzer = analyzer;
    elements      = new ArrayList<Element>();
  }

  /** Visit the specified module. */
  public void visit(Module m) {
    // Initialize the per-grammar state.
    analyzer.register(this);
    analyzer.init(m);
    elements.clear();

    // Process the productions.
    for (Production p : m.productions) dispatch(p);
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

    // Check for a semantic value.
    if (! s.hasTrailingChoice()) {
      if (! Analyzer.setsValue(elements, false)) {
        final int size = elements.size();

        if (0 == size) {
          runtime.error("empty alternative without semantic value", s);
        } else if (elements.get(size-1).hasLocation()) {
          runtime.error("last element in alternative without semantic value",
                        elements.get(size-1));
        } else {
          runtime.error("alternative without semantic value", s);
        }
      }
    }

    // Remove any elements added by processing the sequence.
    if (0 == base) {
      elements.clear();
    } else {
      elements.subList(base, elements.size()).clear();
    }
  }

}
