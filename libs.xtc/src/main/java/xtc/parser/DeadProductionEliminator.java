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

import java.util.Iterator;

import xtc.Constants;

import xtc.util.Runtime;

/**
 * Visitor to eliminate dead productions.  This visitor eliminates
 * productions that are not reachable from top-level productions.  It
 * may perform faster if the grammar has previously been annotated
 * with its real root.
 *
 * @see RootFinder
 *
 * @author Robert Grimm
 * @version $Revision: 1.39 $
 */
public class DeadProductionEliminator extends GrammarVisitor {

  /**
   * Create a new dead production eliminator.
   *
   * @param runtime The runtime.
   * @param analyzer The analyzer utility.
   */
  public DeadProductionEliminator(Runtime runtime, Analyzer analyzer) {
    super(runtime, analyzer);
  }

  /** Visit the specified grammar. */
  public Object visit(Module m) {
    // Initialize the per-grammar state.
    analyzer.register(this);
    analyzer.init(m);

    // Mark all productions reachable from the top-level nonterminals.
    if (m.hasProperty(Properties.ROOT)) {
      dispatch((NonTerminal)m.getProperty(Properties.ROOT));

    } else {
      for (Production p : m.productions) {
        if (p.hasAttribute(Constants.ATT_PUBLIC)) {
          dispatch(p.name);
        }
      }
    }

    // Remove all productions that have not been marked.
    for(Iterator<Production> iter = m.productions.iterator(); iter.hasNext();) {
      Production p = iter.next();

      if (! analyzer.isMarked(p.qName)) {
        if (runtime.test("optionVerbose")) {
          System.err.println("[Removing dead production " + p.qName + "]");
        }

        analyzer.remove((FullProduction)p);
        iter.remove();
      }
    }

    // Done.
    return null;
  }

  /** Visit the specified nonterminal. */
  public Element visit(NonTerminal nt) {
    FullProduction p = analyzer.lookup(nt);

    if (! analyzer.isMarked(p.qName)) {
      analyzer.mark(p.qName);
      dispatch(p);
    }
    return nt;
  }

}
