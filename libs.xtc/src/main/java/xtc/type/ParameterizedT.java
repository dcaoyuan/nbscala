/*
 * xtc - The eXTensible Compiler
 * Copyright (C) 2007 Robert Grimm
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A parameterized type.
 *
 * @author Robert Grimm
 * @version $Revision: 1.2 $
 */
public class ParameterizedT extends WrappedT {

  /** The list of parameters. */
  private List<Parameter> parameters;

  /**
   * Create a new parameterized type.
   *
   * @param parameter The single parameter.
   * @param type The type.
   */
  public ParameterizedT(Parameter parameter, Type type) {
    super(type);
    this.parameters = new ArrayList<Parameter>(1);
    this.parameters.add(parameter);
  }

  /**
   * Create a new parameterized type.
   *
   * @param parameters The parameters.
   * @param type The type.
   */
  public ParameterizedT(List<Parameter> parameters, Type type) {
    super(type);
    this.parameters = parameters;
  }

  /**
   * Create a new parameterized type.
   *
   * @param template The type whose annotations to copy.
   * @param parameters The parameters.
   * @param type The type.
   */
  public ParameterizedT(Type template, List<Parameter> parameters, Type type) {
    super(template, type);
    this.parameters = parameters;
  }

  public ParameterizedT copy() {
    return new ParameterizedT(this, Type.copy(parameters), getType().copy());
  }

  public Type seal() {
    if (! isSealed()) {
      super.seal();
      parameters = Type.seal(parameters);
    }
    return this;
  }

  public Type.Tag wtag() {
    return Type.Tag.PARAMETERIZED;
  }

  public boolean isParameterized() {
    return true;
  }

  public boolean hasParameterized() {
    return true;
  }

  public ParameterizedT toParameterized() {
    return this;
  }

  /**
   * Get this parameterized type's parameters.
   *
   * @return The parameters.
   */
  public List<Parameter> getParameters() {
    return parameters;
  }

  public void write(Appendable out) throws IOException {
    out.append('<');
    for (Iterator<Parameter> iter = parameters.iterator(); iter.hasNext(); ) {
      iter.next().write(out);
      if (iter.hasNext()) out.append(", ");
    }
    out.append("> ");
    getType().write(out);
  }

}
