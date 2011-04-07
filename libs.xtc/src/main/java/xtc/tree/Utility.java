/*
 * xtc - The eXTensible Compiler
 * Copyright (C) 2004 Robert Grimm
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
package xtc.tree;

/**
 * The superclass of all utilities.  A utility provides state and
 * functionality for visitors.  Utilities are implemented as separate
 * classes (instead of visitors inheriting from utilities) so that the
 * same utility can be reused across visitors and also shared amongst
 * several, composed visitors.
 *
 * @author Robert Grimm
 * @version $Revision: 1.5 $
 */
public abstract class Utility {

  /** The visitor for this utility. */
  Visitor visitor;

  /** Create a new utility. */
  public Utility() { /* Nothing to do. */ }

  /**
   * Set the visitor for this utility.
   *
   * @param visitor The new visitor.
   */
  public void register(Visitor visitor) {
    this.visitor = visitor;
  }

  /**
   * Get the visitor for this utility.
   *
   * @return The visitor.
   */
  public Visitor visitor() {
    return visitor;
  }

}
