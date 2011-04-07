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

/**
 * A grammar module import.
 *
 * @author Robert Grimm
 * @version $Revision: 1.13 $
 */
public class ModuleImport extends ModuleDependency {

  /**
   * Create a new module import.
   *
   * @param module The module name.
   */
  public ModuleImport(ModuleName module) {
    super(module, null, null);
  }

  /**
   * Create a new module import.
   *
   * @param module The module name.
   * @param arguments The arguments.
   * @param target The target module name.
   */
  public ModuleImport(ModuleName module, ModuleList arguments,
                      ModuleName target) {
    super(module, arguments, target);
  }

  public boolean isImport() {
    return true;
  }

}
