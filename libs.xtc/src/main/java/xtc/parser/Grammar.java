/*
 * xtc - The eXTensible Compiler
 * Copyright (C) 2005, 2006 Robert Grimm
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
import java.util.List;

import xtc.tree.Node;

/**
 * A grammar represented as a collection of modules.
 *
 * @author Robert Grimm
 * @version $Revision: 1.26 $
 */
public class Grammar extends Node {

  /**
   * The list of {@link Module modules}.  The first module in this
   * list is the grammar's main module and the following modules are
   * all the dependent modules.
   */
  public List<Module> modules;

  /**
   * Create a new grammar.
   *
   * @param modules The list of modules.
   */
  public Grammar(List<Module> modules) {
    this.modules = modules;
  }

  /**
   * Remove the specified module from this grammar.  This method uses
   * reference equality to locate the module to replace.
   *
   * @param module The module.
   * @throws IllegalArgumentException
   *   Signals that the specified module is not part of this grammar.
   */
  public void remove(final Module module) {
    for (Iterator<Module> iter = modules.iterator(); iter.hasNext(); ) {
      if (module == iter.next()) {
        iter.remove();
        return;
      }
    }
    throw new IllegalArgumentException("Module " + module +
                                       " not part of grammar");
  }

  /**
   * Replace the specified module.  This method uses reference
   * equality to locate the module to replace.
   *
   * @param oldModule The old module.
   * @param newModule The new module.
   * @throws IllegalArgumentException
   *   Signals that the specified old module is not part of this grammar.
   */
  public void replace(final Module oldModule, final Module newModule) {
    final int length = modules.size();
    for (int i=0; i<length; i++) {
      if (oldModule == modules.get(i)) {
        modules.set(i, newModule);
        return;
      }
    }
    throw new IllegalArgumentException("Module " + oldModule +
                                       " not part of grammar");
  }

}
