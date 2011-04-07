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

import xtc.util.Runtime;

/**
 * Visitor to optimize the recognition of terminals by introducing
 * character switches.  Note that the different alternatives in an
 * ordered choice must have been normalized into sequences.
 *
 * @author Robert Grimm
 * @version $Revision: 1.30 $
 */
public class TerminalOptimizer extends GrammarVisitor {

  /**
   * Create a new terminal optimizer.
   *
   * @param runtime The runtime.
   * @param analyzer The analyzer utility.
   */
  public TerminalOptimizer(Runtime runtime, Analyzer analyzer) {
    super(runtime, analyzer);
  }

  /** Visit the specified ordered choice. */
  public Element visit(OrderedChoice c) {
    // First, see if we can generate any character switches that span
    // several alternatives.
    for (int i=0; i<c.alternatives.size(); i++) {
      if (analyzer.hasTerminalPrefix(c.alternatives.get(i))) {
        // Are there more?
        int j;
        for (j=i+1; j<c.alternatives.size(); j++) {
          if (! analyzer.hasTerminalPrefix(c.alternatives.get(j))) {
            break;
          }
        }

        if (i != j-1) {
          // There are alternatives to optimize. Normalize them first.
          for (int k=i; k<j; k++) {
            c.alternatives.set(k,
               analyzer.normalizeTerminals(c.alternatives.get(k)));
          }

          Element result = null;
          for (int k=i; k<j; k++) {
            result = analyzer.joinTerminals(c.alternatives.get(k), result);
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
            System.err.println("[Folding terminals in " +
                               analyzer.current().qName + "]");
          }
        }
      }
    }

    // Next, process the alternatives individually.  Note that the number
    // of alternatives may have changed.
    final int size = c.alternatives.size();
    for (int i=0; i<size; i++) {
      c.alternatives.set(i, (Sequence)dispatch(c.alternatives.get(i)));
    }

    return c;
  }

  /** Visit the specified sequence. */
  public Element visit(Sequence s) {
    // Process the elements first.
    final int size = s.size();
    for (int i=0; i<size; i++) {
      s.elements.set(i, (Element)dispatch(s.get(i)));
    }

    // Now, see if we can turn character classes into character
    // switches.
    for (int i=s.size()-1; i>=0; i--) {
      Element e = s.get(i);

      if (e instanceof CharClass) {
        CharClass klass = (CharClass)e;
        final int kount = klass.count();

        if ((1 < kount) && (kount <= Analyzer.MAX_COUNT)) {
          CharSwitch sw = new CharSwitch(klass, s.subSequence(i+1));
          sw.setLocation(s);
          s.elements.set(i, sw);
          s = s.subSequence(0, i+1);

          if (runtime.test("optionVerbose")) {
            System.err.println("[Creating char switch in " +
                               analyzer.current().qName + "]");
          }
        }
      }
    }

    // Done.
    return s;
  }

  /** Visit the specified predicate. */
  public Element visit(Predicate p) {
    // Ignore the element.  In general, switch statements may lead to
    // embedded ordered choices, which are not (yet) supported by the
    // code generator.
    return p;
  }

}
