/*
 * xtc - The eXTensible Compiler
 * Copyright (C) 2005 Robert Grimm
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

import xtc.Constants;

/**
 * A module name as a node.
 *
 * @author Robert Grimm
 * @version $Revision: 1.5 $
 */
public class ModuleName extends Name {

  /**
   * Create a new module name.
   *
   * @param name The name.
   */
  public ModuleName(String name) {
    super(name);
  }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (! (o instanceof ModuleName)) return false;
    return name.equals(((ModuleName)o).name);
  }

  /**
   * Rename this module name.  If this module name is a key in the
   * specified module map, this method returns a new module name that
   * equals the mapping's value.  The new module name's {@link
   * Constants#ORIGINAL original} property is set to be this module
   * name's original name (i.e., this module name's original property
   * if it has that property or this module name if it does not).
   * Otherwise, this method returns this module name.
   *
   * @param renaming The module map.
   * @return The renamed module name.
   */
  public ModuleName rename(ModuleMap renaming) {
    if (renaming.containsKey(this)) {
      ModuleName original = this.hasProperty(Constants.ORIGINAL)?
        (ModuleName)this.getProperty(Constants.ORIGINAL) : this;
      ModuleName newName  = new ModuleName(renaming.get(this).name);
      newName.setProperty(Constants.ORIGINAL, original);
      return newName;

    } else {
      return this;
    }
  }

}
