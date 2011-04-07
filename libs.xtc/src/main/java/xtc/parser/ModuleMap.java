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

import java.util.HashMap;
import java.util.Map;

/**
 * A mapping between module names.  This class has been designed for
 * the efficient renaming of both {@link ModuleDependency module
 * dependencies} and {@link NonTerminal nonterminals}.
 *
 * @author Robert Grimm
 * @version $Revision: 1.6 $
 */
public class ModuleMap implements Renamer.Translation {
  
  /**
   * The mapping from module names (represented as strings) to module
   * names (represented as {@link ModuleName module names}).
   */
  protected Map<String, ModuleName> map;

  /** Create a new module map. */
  public ModuleMap() {
    map = new HashMap<String, ModuleName>();
  }

  /**
   * Create a new module map.
   *
   * @param key The single initial key.
   * @param value The single initial value.
   */
  public ModuleMap(ModuleName key, ModuleName value) {
    map = new HashMap<String, ModuleName>();
    map.put(key.name, value);
  }

  /**
   * Create a new module map.
   *
   * @param keys The list of keys.
   * @param values The list of values.
   * @throws IllegalArgumentException
   *   Signals that the lists have different lengths.
   */
  public ModuleMap(ModuleList keys, ModuleList values) {
    if (keys.size() != values.size()) {
      throw new IllegalArgumentException("Different numbers of keys and values");
    }

    map      = new HashMap<String, ModuleName>();
    int size = keys.size();
    for (int i=0; i<size; i++) {
      map.put(keys.get(i).name, values.get(i));
    }
  }

  /**
   * Add the specified mapping.
   *
   * @param key The key.
   * @param value The value.
   */
  public void put(ModuleName key, ModuleName value) {
    map.put(key.name, value);
  }

  /**
   * Determine whether this module map contains a mapping for the
   * specified module name.
   *
   * @param key The module name.
   * @return <code>true</code> if this module map has a value for the
   *   key.
   */
  public boolean containsKey(ModuleName key) {
    return map.containsKey(key.name);
  }

  /**
   * Determine whether this module map contains a mapping for the
   * specified module name.
   *
   * @param key The module name.
   * @return <code>true</code> if this module map has a value for the
   *   key.
   */
  public boolean containsKey(String key) {
    return map.containsKey(key);
  }

  /**
   * Look up the specified module name.
   *
   * @param key The module name.
   * @return The corresponding value or <code>null</code> if there is
   *   no mapping for the module name.
   */
  public ModuleName get(ModuleName key) {
    return map.get(key.name);
  }

  /**
   * Look up the specified module name.
   *
   * @param key The module name.
   * @return The corresponding value or <code>null</code> if there is
   *   no mapping for the module name.
   */
  public String get(String key) {
    ModuleName value = map.get(key);
    return (null == value)? null : value.name;
  }

  public NonTerminal map(NonTerminal nt, Analyzer analyzer) {
    return nt.rename(this);
  }

}
