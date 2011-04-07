/*
 * xtc - The eXTensible Compiler
 * Copyright (C) 2004-2005 Robert Grimm
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
 * Visitor to rename nonterminals.
 *
 * @author Robert Grimm
 * @version $Revision: 1.5 $
 */
public class Renamer extends GrammarVisitor {

  /**
   * The interface to the actual mapping from nonterminals to
   * nonterminals.
   */
  public interface Translation {

    /**
     * Map the specified nonterminal to its replacement, which may be
     * the same as the original.
     *
     * @param nt The nonterminal.
     * @param analyzer The analyzer utility.
     * @return The translated nonterminal.
     */
    public NonTerminal map(NonTerminal nt, Analyzer analyzer);

  }

  // -----------------------------------------------------------------------

  /** The mapping. */
  protected Translation translation;

  /**
   * Create a new renamer.
   *
   * @param runtime The runtime.
   * @param analyzer The analyzer utility.
   * @param translation The mapping from nonterminals to nonterminals.
   */
  public Renamer(Runtime runtime, Analyzer analyzer, Translation translation) {
    super(runtime, analyzer);
    this.translation = translation;
  }

  /** Visit the specified nonterminal. */
  public Element visit(NonTerminal nt) {
    return translation.map(nt, analyzer);
  }

}
