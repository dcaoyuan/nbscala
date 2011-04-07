/*
 * xtc - The eXTensible Compiler
 * Copyright (C) 2005-2007 Robert Grimm
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

import java.util.List;

/**
 * A method type.
 *
 * @author Robert Grimm
 * @version $Revision: 1.12 $
 */
public class MethodT extends FunctionOrMethodT {

  /**
   * Create a new method type.
   *
   * @param result The result type.
   * @param name The name.
   * @param parameters The list of parameter types.
   * @param varargs The flag for accepting a variable number of
   *   arguments.
   * @param exceptions The list of exceptions, which must not be
   *   <code>null</code>
   */
  public MethodT(Type result, String name, List<Type> parameters,
                 boolean varargs, List<Type> exceptions) {
    super(null, result, name, parameters, varargs, exceptions);
  }

  /**
   * Create a new method type.
   *
   * @param template The type whose annotations to copy.
   * @param result The result type.
   * @param name The name.
   * @param parameters The list of parameter types.
   * @param varargs The flag for accepting a variable number of
   *   arguments.
   * @param exceptions The list of exceptions, which must not be
   *   <code>null</code>
   */
  public MethodT(Type template, Type result, String name, List<Type> parameters,
                 boolean varargs, List<Type> exceptions) {
    super(template, result, name, parameters, varargs, exceptions);
  }

  public MethodT copy() {
    return new MethodT(this, result.copy(), name, copy(parameters), varargs,
                       copy(exceptions));
  }

  public Type.Tag tag() {
    return Type.Tag.METHOD;
  }

  public boolean isMethod() {
    return true;
  }

  public MethodT toMethod() {
    return this;
  }

}
