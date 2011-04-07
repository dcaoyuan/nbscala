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

import java.util.List;

/**
 * The superclass of all value elements specifying a generic node as
 * the semantic value.
 *
 * @author Robert Grimm
 * @version $Revision: 1.7 $
 */
public abstract class GenericValue extends ValueElement {

  /** The name of the generic node. */
  public final String name;

  /** The list of bindings representing the generic node's children. */
  public final List<Binding> children;

  /** The bindings capturing any formatting. */
  public final List<Binding> formatting;

  /**
   * Create a new generic value.
   *
   * @param name The name.
   * @param children The list of children.
   * @param formatting The list of bindings for formatting.
   */
  public GenericValue(String name, List<Binding> children,
                      List<Binding> formatting) {
    this.name       = name;
    this.children   = children;
    this.formatting = formatting;
  }

}
