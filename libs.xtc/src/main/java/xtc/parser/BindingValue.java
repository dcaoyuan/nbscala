/*
 * xtc - The eXTensible Compiler
 * Copyright (C) 2006-2007 Robert Grimm
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
 * Element to set the semantic value to a binding.
 *
 * @author Robert Grimm
 * @version $Revision: 1.3 $
 */
public class BindingValue extends ValueElement {

  /** The binding. */
  public final Binding binding;

  /**
   * Create a new binding value.
   *
   * @param binding The binding.
   */
  public BindingValue(Binding binding) {
    this.binding = binding;
  }

  public Tag tag() {
    return Tag.BINDING_VALUE;
  }

  public int hashCode() {
    return binding.hashCode();
  }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (! (o instanceof BindingValue)) return false;
    return binding.equals(((BindingValue)o).binding);
  }

}
