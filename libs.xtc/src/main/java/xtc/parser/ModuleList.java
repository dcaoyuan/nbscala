/*
 * xtc - The eXTensible Compiler
 * Copyright (C) 2005-2006 Robert Grimm
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
 * A list of module names as a node.
 *
 * @author Robert Grimm
 * @version $Revision: 1.12 $
 */
public class ModuleList extends Node {

  /** The list of {@link ModuleName module names}. */
  public List<ModuleName> names;

  /**
   * Create a new module list.
   *
   * @param names The list of names.
   */
  public ModuleList(List<ModuleName> names) {
    this.names = names;
  }

  public int hashCode() {
    return names.hashCode();
  }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (! (o instanceof ModuleList)) return false;
    return names.equals(((ModuleList)o).names);
  }

  /**
   * Determine whether this module list is empty.
   *
   * @return <code>true</code> if this is an empty module list.
   */
  public boolean isEmpty() {
    return names.isEmpty();
  }

  /**
   * Get the size of this module list.
   *
   * @return The size.
   */
  public int size() {
    return names.size();
  }

  /**
   * Get the module name at the specified index.
   *
   * @param idx The index.
   * @return The module name at that position.
   * @throws IndexOutOfBoundsException
   *   Signals that the index is out of range.
   */
  public ModuleName get(int idx) {
    return names.get(idx);
  }

  /**
   * Rename this module list.  This method {@link
   * ModuleName#rename(ModuleMap) renames} all module names in this
   * module list using the specified module map.
   *
   * @param renaming The module map.
   * @return This list.
   */
  public ModuleList rename(ModuleMap renaming) {
    final int length = names.size();
    for (int i=0; i<length; i++) {
      names.set(i, names.get(i).rename(renaming));
    }
    return this;
  }

  public String toString() {
    StringBuilder buf  = new StringBuilder();
    Iterator<?>   iter = names.iterator();
    buf.append('(');
    while (iter.hasNext()) {
      buf.append(iter.next().toString());
      if (iter.hasNext()) {
        buf.append(", ");
      }
    }
    buf.append(')');
    return buf.toString();
  }

}
