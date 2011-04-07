/*
 * xtc - The eXTensible Compiler
 * Copyright (C) 2005-2007 Robert Grimm
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

import xtc.Constants;

import xtc.tree.Attribute;

import xtc.util.Runtime;

/**
 * Visitor to detect productions that can be treated as transient.
 *
 * <p />Note that this visitor requires that a grammar's productions
 * have been {@link ReferenceCounter reference counted}.  Further note
 * that this visitor assumes that the entire grammar is contained in a
 * single module.
 *
 * @author Robert Grimm
 * @version $Revision: 1.22 $
 */
public class TransientMarker extends GrammarVisitor {

  /**
   * Create a new transient marker.
   *
   * @param runtime The runtime.
   * @param analyzer The analyzer utility.
   */
  public TransientMarker(Runtime runtime, Analyzer analyzer) {
    super(runtime, analyzer);
  }

  /**
   * Visit the specified grammar.
   *
   * @param m The grammar module.
   * @return <code>Boolean.TRUE</code> if the grammar has been modified,
   *   otherwise <code>Boolean.FALSE</code>.
   */
  public Object visit(Module m) {
    // Initialize the per-grammar state.
    analyzer.register(this);
    analyzer.init(m);

    // Process the productions.
    boolean changed = false;
    for (Production p : m.productions) {
      MetaData md = (MetaData)p.getProperty(Properties.META_DATA);

      if ((1 >= md.usageCount) &&
          (! p.hasAttribute(Constants.ATT_TRANSIENT)) &&
          (! p.hasAttribute(Constants.ATT_INLINE)) &&
          (! p.hasAttribute(Constants.ATT_MEMOIZED))) {
        if (runtime.test("optionVerbose")) {
          System.err.println("[Marking " + p.qName + " as transient]");
        }

        if (null == p.attributes) {
          p.attributes = new ArrayList<Attribute>(1);
        }
        p.attributes.add(Constants.ATT_TRANSIENT);
        changed = true;
      }
    }

    // Done.
    return Boolean.valueOf(changed);
  }

}
