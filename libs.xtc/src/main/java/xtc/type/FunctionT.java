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
 * A function type.  Note that a C function with an old-style
 * declarator must be annotated with the {@link
 * xtc.Constants#ATT_STYLE_OLD} attribute.  An old-style definition
 * must also be annotated with the {@link xtc.Constants#ATT_DEFINED}
 * attribute.
 *
 * @author Robert Grimm
 * @version $Revision: 1.31 $
 */
public class FunctionT extends FunctionOrMethodT {

  /**
   * Create a new function type.
   *
   * @param result The result type.
   */
  public FunctionT(Type result) {
    super(null, result, null, null, false, null);
  }

  /**
   * Create a new function type.
   *
   * @param result The result type.
   * @param parameters The list of parameter types.
   * @param varargs The flag for accepting a variable number of arguments.
   */
  public FunctionT(Type result, List<Type> parameters, boolean varargs) {
    super(null, result, null, parameters, varargs, null);
  }

  /**
   * Create a new function type.
   *
   * @param template The type whose annotations to copy.
   * @param result The result type.
   * @param parameters The list of parameter types.
   * @param varargs The flag for accepting a variable number of arguments.
   */
  public FunctionT(Type template, Type result, List<Type> parameters,
                   boolean varargs) {
    super(template, result, null, parameters, varargs, null);
  }

  public FunctionT copy() {
    FunctionT copy =
      new FunctionT(this, result.copy(), copy(parameters), varargs);
    if (null != exceptions) {
      copy.exceptions = copy(exceptions);
    }
    return copy;
  }

  public Type.Tag tag() {
    return Type.Tag.FUNCTION;
  }

  public boolean isFunction() {
    return true;
  }

  public FunctionT toFunction() {
    return this;
  }

}
