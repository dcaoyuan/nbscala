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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import xtc.Constants;

import xtc.tree.Attribute;

import xtc.util.Runtime;
import xtc.util.Utilities;

/**
 * Visitor to fold duplicate productions into a single production.
 *
 * <p />This visitor assumes that the entire grammar is contained in a
 * single module.
 *
 * @author Robert Grimm
 * @version $Revision: 1.36 $
 */
public class DuplicateProductionFolder extends GrammarVisitor {

  /** The map of from folded nonterminals to replacement nonterminals. */
  protected final Map<NonTerminal, NonTerminal> folded;

  /**
   * Create a new duplicate production folder.
   *
   * @param runtime The runtime.
   * @param analyzer The analyzer utility.
   */
  public DuplicateProductionFolder(Runtime runtime, Analyzer analyzer) {
    super(runtime, analyzer);
    folded = new HashMap<NonTerminal, NonTerminal>();
  }

  /** Visit the specified grammar. */
  public Object visit(Module m) {
    // Initialize the per-grammar state.
    analyzer.register(this);
    analyzer.init(m);

    // Iterate over the grammar, folding duplicates, until none are
    // found.
    EquivalenceTester tester          = new EquivalenceTester();
    boolean           foundDuplicates = false;
    boolean           changed;
    do {
      // Clear the map of replacement nonterminals.
      folded.clear();

      changed = false;

      // Compare all productions with each other.
      final int l = m.productions.size();
      for (int i=0; i<l; i++) {
        FullProduction p1 = (FullProduction)m.productions.get(i);

        // Skip top-level productions, generic productions, and
        // productions that already have been folded.
        if (p1.hasAttribute(Constants.ATT_PUBLIC) ||
            Generifier.isGeneric(p1) ||
            analyzer.isMarked(p1.qName)) {
          continue;
        }

        // The nonterminal for the shared production, the shared
        // production, and the list of nonterminal names represented
        // by the shared production.
        NonTerminal    nt         = null;
        FullProduction shared     = null;
        List<String>   sources    = null;
        boolean        isTextOnly = false;
        boolean        isToken    = false;
        boolean        hasOption  = false;

        for (int j=i+1; j<l; j++) {
          FullProduction p2 = (FullProduction)m.productions.get(j);

          // Skip top-level productions and productions that are not
          // equivalent.
          if (p2.hasAttribute(Constants.ATT_PUBLIC) ||
              (! tester.areEquivalent(p1, p2)) ||
              (p1.getBooleanProperty(Properties.TEXT_ONLY) !=
               p2.getBooleanProperty(Properties.TEXT_ONLY)) ||
              (p1.getBooleanProperty(Properties.TOKEN) !=
               p2.getBooleanProperty(Properties.TOKEN))) {
            continue;
          }

          // We found duplicate productions.
          foundDuplicates = true;
          changed         = true;

          if (null == nt) {
            // This is the first duplicate pair for the production at
            // index i.
            nt         = analyzer.shared();
            shared     = p1;
            if (p1.hasProperty(Properties.DUPLICATES)) {
              sources  = Properties.getDuplicates(p1);
            } else {
              sources  = new ArrayList<String>();
              sources.add(p1.qName.toString());
            }
            isTextOnly = p1.getBooleanProperty(Properties.TEXT_ONLY);
            isToken    = p1.getBooleanProperty(Properties.TOKEN);
            hasOption  = p1.hasProperty(Properties.OPTION);

            // Establish a mapping to the shared production.
            folded.put(p1.name, nt);
          }

          if (p2.hasProperty(Properties.DUPLICATES)) {
            sources.addAll(Properties.getDuplicates(p2));
          } else {
            sources.add(p2.qName.toString());
          }
          if (p2.hasProperty(Properties.OPTION)) {
            hasOption = true;
          }

          // Mark the production for future removal.
          analyzer.mark(p2.qName);

          // Establish a mapping to the shared production.
          folded.put(p2.name, nt);
        }

        if (null != nt) {
          // Create the shared production.
          shared =
            new FullProduction(new ArrayList<Attribute>(shared.attributes),
                               shared.type, nt,
                               nt.qualify(analyzer.module().name.name),
                               shared.choice);
          shared.setProperty(Properties.DUPLICATES, sources);
          if (isTextOnly) {
            shared.setProperty(Properties.TEXT_ONLY, Boolean.TRUE);
          }
          if (isToken) {
            shared.setProperty(Properties.TOKEN, Boolean.TRUE);
          }
          if (hasOption) {
            shared.setProperty(Properties.OPTION, Boolean.TRUE);
          }

          // Replace the first duplicate with the shared production.
          analyzer.remove(p1);
          m.productions.remove(i);
          analyzer.startAdding();
          analyzer.add(shared);
          analyzer.addNewProductionsAt(i);
        }
      }

      // If we have not found any duplicate productions, we fall out
      // of the loop.
      if (! changed) {
        break;
      }

      // Modify all nonterminals to reference the shared productions.
      for (Production p : m.productions) dispatch(p);

      // Remove duplicate productions from the grammar.
      for (Iterator<Production> iter=m.productions.iterator(); iter.hasNext();) {
        Production p = iter.next();

        if (analyzer.isMarked(p.qName)) {
          analyzer.unmark(p.qName);
          analyzer.remove((FullProduction)p);
          iter.remove();
        }
      }
    } while (changed);

    // Print debugging information.
    if (runtime.test("optionVerbose") && foundDuplicates) {
      Iterator iter = m.productions.iterator();
      while (iter.hasNext()) {
        FullProduction p = (FullProduction)iter.next();

        if (p.hasProperty(Properties.DUPLICATES)) {
          String lst = Utilities.format(Properties.getDuplicates(p));
          System.err.println("[Folding " + lst + " into " + p.qName + ']');
        }
      }
    }

    return null;
  }

  /** Visit the specified nonterminal. */
  public Element visit(NonTerminal nt) {
    NonTerminal alt = folded.get(nt);

    return (null == alt)? nt : alt;
  }

}
