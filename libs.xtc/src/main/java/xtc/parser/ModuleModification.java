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
 * A grammar module modification.
 *
 * @author Robert Grimm
 * @version $Revision: 1.5 $
 */
public class ModuleModification extends ModuleDependency {

  /**
   * Create a new module modification.
   *
   * @param module The module name.
   * @param arguments The arguments.
   * @param target The target module name.
   */
  public ModuleModification(ModuleName module, ModuleList arguments,
                            ModuleName target) {
    super(module, arguments, target);
  }

  public boolean isModification() {
    return true;
  }

}
