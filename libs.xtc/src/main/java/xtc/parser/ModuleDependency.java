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

import java.util.ArrayList;

import xtc.Constants;

import xtc.tree.Node;

/**
 * A grammar module dependency.  Note that two module dependencies are
 * equal if they describe the same module.
 *
 * @author Robert Grimm
 * @version $Revision: 1.16 $
 */
public abstract class ModuleDependency extends Node {

  /** The name of the dependent module. */
  public ModuleName module;

  /** The arguments to the dependent module. */
  public ModuleList arguments;

  /** The optional target module name. */
  public ModuleName target;

  /**
   * Create a new module dependency.  If the specified arguments are
   * <code>null</code>, they are replaced with an empty module list.
   * If the specified target equals the module name, it is replaced by
   * <code>null</code>.
   *
   * @param module The module name.
   * @param arguments The arguments.
   * @param target The target module name.
   */
  public ModuleDependency(ModuleName module, ModuleList arguments,
                          ModuleName target) {
    this.module      = module;
    if (null == arguments) {
      this.arguments = new ModuleList(new ArrayList<ModuleName>(0));
    } else {
      this.arguments = arguments;
    }
    if (module.equals(target)) {
      this.target    = null;
    } else {
      this.target    = target;
    }
  }

  public int hashCode() {
    if (null == target) {
      return module.hashCode();
    } else {
      return target.hashCode();
    }
  }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (! (o instanceof ModuleDependency)) return false;
    ModuleDependency other = (ModuleDependency)o;
    if (! module.equals(other.module)) return false;
    if (null == target) {
      if (null != other.target) return false;
    } else {
      if (! target.equals(other.target)) return false;
    }
    return arguments.equals(other.arguments);
  }

  /**
   * Determine whether this dependency is a {@link ModuleImport module
   * import}.
   *
   * @return <code>true</code> if this dependency is an import.
   */
  public boolean isImport() {
    return false;
  }

  /**
   * Determine whether this dependency is a {@link ModuleInstantiation
   * module instantiation}.
   *
   * @return <code>true</code> if this dependency is an instantiation.
   */
  public boolean isInstantiation() {
    return false;
  }

  /**
   * Determine whether this dependency is a {@link ModuleModification
   * module modification}.
   *
   * @return <code>true</code> if this dependency is a modification.
   */
  public boolean isModification() {
    return false;
  }

  /**
   * Get the visible name for this module dependency.  The visible
   * name is the qualifier available for nonterminals referencing
   * productions in the dependent module.
   *
   * @return The visible name.
   */
  public ModuleName visibleName() {
    return (null == target)? module : target;
  }

  /**
   * Determine whether this module dependency is consistent with the
   * specified dependency, which has been resolved previously.
   *
   * @param dep The previously resolved dependency.
   * @return <code>true</code> if this module dependency is consistent
   *   with the specified one.
   */
  public boolean isConsistentWith(ModuleDependency dep) {
    return (! visibleName().equals(dep.visibleName()) ||
            this.equals(dep) ||
            (arguments.isEmpty() && null == target));
  }

  /**
   * Rename this module dependency.  This method modifies this module
   * dependency based on the specified module map.  If any module name
   * is renamed, the new module name's {@link Constants#ORIGINAL
   * original} property is set to the original module name.
   *
   * @param renaming The module map.
   * @return This module dependency.
   */
  public ModuleDependency rename(ModuleMap renaming) {
    module    = module.rename(renaming);
    arguments = arguments.rename(renaming);
    if (null != target) {
      target  = target.rename(renaming);
    }
    return this;
  }

}
