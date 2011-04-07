/*
 * xtc - The eXTensible Compiler
 * Copyright (C) 2007-2008 Robert Grimm
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
package xtc.type;

import java.io.IOException;

/**
 * The superclass of all type parameters.  When a type is
 * parameterized, all occurrences of the same parameter should also be
 * the same instance of a subclass.  Furthermore, the type should be
 * wrapped in a {@link ParameterizedT} listing all parameters.
 * Instantiation of a parameterized type does not require replacing
 * all parameters, but rather should be implemented by wrapping the
 * paramterized type in an {@link InstantiatedT} listing all
 * arguments.  To support efficient unification, this class implements
 * union/find operations with path compression through the {@link
 * #bind(Type)} and {@link #lookup()} operations.
 *
 * @author Robert Grimm
 * @version $Revision: 1.4 $
 */
public abstract class Parameter extends Type {

  /** The binding. */
  private Type binding;

  /** Create a new parameter. */
  public Parameter() {
    // Nothing to do.
  }

  /**
   * Create a new parameter.
   *
   * @param template The type whose annotations to copy.
   */
  public Parameter(Type template) {
    super(template);
  }

  public boolean isParameter() {
    return true;
  }

  public Parameter toParameter() {
    return this;
  }

  /**
   * Bind this parameter to the specified type.
   *
   * @param type The type.
   * @throws IllegalStateException Signals that this parameter is
   *   sealed or already bound.
   */
  public void bind(Type type) {
    checkNotSealed();
    if (null != binding) {
      throw new IllegalStateException("Parameter already bound");
    }

    binding = type;
    if (binding.isParameter()) binding = binding.toParameter().lookup();
  }

  /**
   * Look up this parameter's binding.  If this parameter is not
   * bound, this method returns the parameter.  Otherwise, it returns
   * the bound type.
   *
   * @return This parameter's binding.
   */
  public Type lookup() {
    if (null == binding) return this;
    if (binding.isParameter()) binding = binding.toParameter().lookup();
    return binding;
  }

}
