/*
 * xtc - The eXTensible Compiler
 * Copyright (C) 2004-2006 Robert Grimm
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

import xtc.util.Runtime;

/**
 * Visitor to combine common prefixes into a single sequence with an
 * embedded choice.  Note that the different alternatives in an
 * ordered choice must have been normalized into sequences.
 *
 * @author Robert Grimm
 * @version $Revision: 1.18 $
 */
public class PrefixFolder extends GrammarVisitor {

  /**
   * Create a new prefix folder.
   *
   * @param runtime The runtime.
   * @param analyzer The analyzer utility.
   */
  public PrefixFolder(Runtime runtime, Analyzer analyzer) {
    super(runtime, analyzer);
  }

  /** Visit the specified ordered choice. */
  public Element visit(OrderedChoice c) {
    // First, see if we can find any common prefixes across several
    // alternatives.
    for (int i=0; i<c.alternatives.size(); i++) {
      Sequence s = c.alternatives.get(i);

      // Are there more with the same prefix?
      int j;
      for (j=i+1; j<c.alternatives.size(); j++) {
        if (! analyzer.haveCommonPrefix(s, c.alternatives.get(j))) {
          break;
        }
      }

      if (i != j-1) {
        // There are alternatives to optimize. Normalize them first.
        for (int k=i+1; k<j; k++) {
          c.alternatives.set(k,
                          analyzer.normalizePrefix(s, c.alternatives.get(k)));
        }

        Element result = null;
        for (int k=i; k<j; k++) {
          result =
            analyzer.joinPrefixes(c.alternatives.get(k), result);
        }

        // Replace old alternatives with result.
        c.alternatives.subList(i, j).clear();
        
        if (result instanceof Sequence) {
          c.alternatives.add(i, (Sequence)result);
          // Continue processing with i+1.
        } else {
          OrderedChoice choice = (OrderedChoice)result;
          c.alternatives.addAll(i, choice.alternatives);
          i += (choice.alternatives.size() - 1);
        }

        if (runtime.test("optionVerbose")) {
          System.err.println("[Folding prefixes in " +
                             analyzer.current().qName + "]");
        }
      }
    }

    // Next, process the alternatives individually.  Note that the
    // number of alternatives may have changed.
    final int length = c.alternatives.size();
    for (int i=0; i<length; i++) {
      c.alternatives.set(i, (Sequence)dispatch(c.alternatives.get(i)));
    }

    return c;
  }

  /** Visit the specified predicate. */
  public Element visit(Predicate p) {
    // Ignore the element.  In general, prefix folding may lead to
    // embedded ordered choices, which are not (yet) supported by the
    // code generator.
    return p;
  }

}
