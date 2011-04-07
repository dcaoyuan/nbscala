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

import java.io.IOException;

import xtc.Constants;

import xtc.util.Utilities;

/**
 * A nonterminal.
 *
 * @author Robert Grimm
 * @version $Revision: 1.22 $
 */
public class NonTerminal extends Element {

  /** The name. */
  public final String name;

  /**
   * Create a new nonterminal with the specified name.
   *
   * @param name The name.
   */
  public NonTerminal(String name) {
    this.name = name;
  }

  /**
   * Create a new nonterminal that is a copy of the specified
   * nonterminal.
   *
   * @param nt The nonterminal.
   */
  public NonTerminal(NonTerminal nt) {
    this.name = nt.name;
  }

  public Tag tag() {
    return Tag.NONTERMINAL;
  }

  public int hashCode() {
    return name.hashCode();
  }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (! (o instanceof NonTerminal)) return false;
    return name.equals(((NonTerminal)o).name);
  }

  /**
   * Determine whether this nonterminal is qualified.
   *
   * @return <code>true</code> if this nonterminal is qualified.
   */
  public boolean isQualified() {
    return Utilities.isQualified(name);
  }

  /**
   * Get this nonterminal's qualifier.  If this nonterminal is
   * qualified, this method returns the qualifier.  Otherwise, it
   * returns <code>null</code>.
   *
   * @return The qualifier.
   */
  public String getQualifier() {
    return Utilities.getQualifier(name);
  }

  /**
   * Qualify this nonterminal.  If this nonterminal is not qualified,
   * this method returns a new nonterminal representing a fully
   * qualified name.  Otherwise, it returns this nonterminal.
   *
   * @param module The module name.
   * @return The corresponding, qualified nonterminal.
   */
  public NonTerminal qualify(String module) {
    if (Utilities.isQualified(name)) {
      return this;
    } else {
      return new NonTerminal(Utilities.qualify(module, name));
    }
  }

  /**
   * Unqualify this nonterminal.  If this nonterminal is qualified,
   * this method returns a new nonterminal representing the
   * unqualified name.  Otherwise, it returns this nonterminal.
   *
   * @return The corresponding, unqualified nonterminal.
   */
  public NonTerminal unqualify() {
    if (Utilities.isQualified(name)) {
      return new NonTerminal(Utilities.getName(name));
    } else {
      return this;
    }
  }

  /**
   * Rename this nonterminal.  If this nonterminal is qualified and
   * the qualifier is a key in the specified module map, this method
   * returns a new nonterminal with the mapping's value as the new
   * qualifier.  The new nonterminal's {@link Constants#ORIGINAL
   * original} property is set to be this nonterminal's original name
   * (i.e., this nonterminal's original property if it has that
   * property or this nonterminal if it does not).  Otherwise, this
   * method returns this nonterminal.
   *
   * @param renaming The module map.
   * @return The renamed nonterminal.
   */
  public NonTerminal rename(ModuleMap renaming) {
    if (Utilities.isQualified(name)) {
      String qualifier = Utilities.getQualifier(name);

      if (renaming.containsKey(qualifier)) {
        NonTerminal original    = this.hasProperty(Constants.ORIGINAL)?
          (NonTerminal)this.getProperty(Constants.ORIGINAL) : this;
        NonTerminal replacement =
          new NonTerminal(Utilities.qualify(renaming.get(qualifier),
                                            Utilities.getName(name)));
        replacement.setProperty(Constants.ORIGINAL, original);
        replacement.setLocation(this); // Preserve for debugging.
        return replacement;
      }
    }

    // Nothing changed. Move on.
    return this;
  }

  /**
   * Convert this nonterminal to the corresponding Java identifier.
   *
   * @return The corresponding Java identifier.
   */
  public String toIdentifier() {
    return Utilities.toIdentifier(name);
  }

  public void write(Appendable out) throws IOException {
    out.append(name);
  }
  
  public String toString() {
    return name;
  }

}
