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
 * Visitor to fill in meta-data reference counts.  This visitor
 * determines the usage and self counts for a grammar's productions.
 * Note that this visitor does not create the meta-data records; they
 * must be created with the {@link MetaDataCreator meta-data creator}
 * before applying this visitor.  Further note that the grammar must
 * have been {@link Simplifier simplified} and {@link Inliner inlined}
 * before applying this visitor.  Finally, note that this visitor
 * assumes that the entire grammar is contained in a single module.
 *
 * @author Robert Grimm
 * @version $Revision: 1.22 $
 */
public class ReferenceCounter extends GrammarVisitor {

  /**
   * The flag for whether the current production is a transformable
   * direct left-recursive production.
   */
  protected boolean isTransformable;

  /**
   * Create a new reference counter.
   *
   * @param runtime The runtime.
   * @param analyzer The analyzer utility.
   */
  public ReferenceCounter(Runtime runtime, Analyzer analyzer) {
    super(runtime, analyzer);
  }

  /** Visit the specified grammar. */
  public Object visit(Module m) {
    // Initialize the per-grammar state.
    analyzer.register(this);
    analyzer.init(m);

    // Clear the usage and self counts.
    for (Production p : m.productions) {
      MetaData md   = (MetaData)p.getProperty(Properties.META_DATA);
      md.usageCount = 0;
      md.selfCount  = 0;
    }

    // Determine the usage and self counts.
    for (Production p : m.productions) {
      isTransformable = 
        ((runtime.test("optimizeLeftRecursions") ||
          runtime.test("optimizeLeftIterations")) &&
         DirectLeftRecurser.isTransformable((FullProduction)p));
      analyzer.process(p);
    }

    // Done.
    return null;
  }

  /** Visit the specified ordered choice. */
  public Element visit(OrderedChoice c) {
    boolean top      = isTopLevel;
    isTopLevel       = false;
    isBound          = false;
    boolean last     = isLastElement;

    final int length = c.alternatives.size();
    for (int i=0; i<length; i++) {
      isLastElement  = top || last;
      needsSequence  = true;
      Sequence s     = c.alternatives.get(i);
      if (top && isTransformable &&
          DirectLeftRecurser.isRecursive(s,
                                         (FullProduction)analyzer.current())) {
        dispatch(s.subSequence(1));
      } else {
        dispatch(s);
      }
    }

    isLastElement    = false;
    needsSequence    = false;

    // There is no need to set the alternatives again, as the
    // reference counter does not modify the grammar.
    return c;
  }

  /** Visit the specified nonterminal. */
  public Element visit(NonTerminal nt) {
    MetaData md = (MetaData)analyzer.lookup(nt).
      getProperty(Properties.META_DATA);
    md.usageCount++;
    if (analyzer.current().name.equals(nt)) {
      md.selfCount++;
    }

    // If the nonterminal appears within a once-or-more repetition in
    // a non-transient production, we need to count it twice.  After
    // all, once-or-more repetitions in non-transient productions are
    // desugared into the equivalent right-recursive productions.  If
    // the nonterminal appears within an ordered choice inside that
    // repetition, it may, after all, not need to be counted twice,
    // since ordered choices are often lifted into their own
    // productions.  However, reproducing the lifting logic here is
    // prohibitive and, consequently, we make a conservative
    // assumption that all nonterminals appearing within once-or-more
    // repetitions in non-transient productions need to be counted
    // twice.
    if (isRepeatedOnce && 
        (analyzer.current().isMemoized() ||
         (! runtime.test("optimizeRepeated")))) {
      md.usageCount++;
      if (analyzer.current().name.equals(nt)) {
        md.selfCount++;
      }
    }

    // Done.
    return nt;
  }

}
