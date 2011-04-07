/*
 * xtc - The eXTensible Compiler
 * Copyright (C) 2005-2006 Robert Grimm
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

import xtc.util.Runtime;

/**
 * Visitor to identify a grammar's real root.  A real root is a
 * top-level nonterminal, whose production directly or indirectly
 * depends on all other top-level nonterminals.  If a grammar has such
 * a root, this visitor annotates the grammar with the {@link
 * Properties#ROOT root} property.
 *
 * <p />Note that this visitor assumes that the entire grammar is
 * contained in a single module.
 *
 * @author Robert Grimm
 * @version $Revision: 1.12 $
 */
public class RootFinder extends GrammarVisitor {

  /** The set of qualified top-level nonterminals. */
  protected Set<NonTerminal> topLevel;

  /**
   * Create a new root finder.
   *
   * @param runtime The runtime.
   * @param analyzer The analyzer utility.
   */
  public RootFinder(Runtime runtime, Analyzer analyzer) {
    super(runtime, analyzer);
    topLevel = new HashSet<NonTerminal>();
  }

  /** Visit the specified grammar. */
  public Object visit(Module m) {
    // Make sure we don't do the work several times.
    if (m.hasProperty(Properties.ROOT)) {
      return null;
    }

    // Initialize the per-grammar state.
    analyzer.register(this);
    analyzer.init(m);

    // Fill in the set of top-level nonterminals.
    topLevel.clear();
    for (Production p : m.productions) {
      if (p.hasAttribute(Constants.ATT_PUBLIC)) {
        topLevel.add(p.qName);
      }
    }

    // Get the trivial case out of the way.
    if (1 == topLevel.size()) {
      m.setProperty(Properties.ROOT, topLevel.toArray()[0]);
      return null;
    }

    // Traverse the grammar, starting with each top-level nonterminal.
    for (NonTerminal nt : topLevel) {
      // Mark the top-level nonterminals.
      analyzer.unmarkAll();
      analyzer.mark(topLevel);

      // Traverse the grammar.
      analyzer.notWorkingOnAny();
      dispatch(nt);

      // Is this nonterminal a real root?
      if (! analyzer.hasMarked()) {
        if (runtime.test("optionVerbose")) {
          System.err.println("[Recognizing " + nt + " as real root]");
        }
        m.setProperty(Properties.ROOT, nt);
        break;
      }
    }

    // Done.
    return null;
  }

  /** Visit the specified nonterminal. */
  public Element visit(NonTerminal nt) {
    Production p = analyzer.lookup(nt);

    if (! analyzer.isBeingWorkedOn(p.qName)) {
      analyzer.workingOn(p.qName);
      analyzer.unmark(p.qName);
      dispatch(p);
    }
    return nt;
  }

}
