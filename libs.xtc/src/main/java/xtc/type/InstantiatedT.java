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
 * An instantiated type.
 *
 * @author Robert Grimm
 * @version $Revision: 1.4 $
 */
public class InstantiatedT extends WrappedT {

  /** The list of arguments. */
  private List<Type> arguments;

  /**
   * Create a new instantiated type.  The specified type must contain
   * a parameterized type with a single parameter.
   *
   * @param argument The argument.
   * @param type The type.
   * @throws IllegalArgumentException Signals that the specified type
   *   does not contain a parameterized type or that the number of
   *   arguments does not match the parameterized type's number of
   *   parameters.
   */
  public InstantiatedT(Type argument, Type type) {
    super(type);
    this.arguments = new ArrayList<Type>(1);
    this.arguments.add(argument);
    if (! type.hasParameterized()) {
      throw new IllegalArgumentException("Not a parameterized type " + type);
    } else if (1 != type.toParameterized().getParameters().size()) {
      throw new IllegalArgumentException("Wrong number of parameters " + type);
    }
  }

  /**
   * Create a new instantiated type.  The specified type must contain
   * a parameterized type with the same number of parameters as the
   * specified arguments.
   *
   * @param arguments The arguments.
   * @param type The type.
   * @throws IllegalArgumentException Signals that the specified type
   *   does not contain a parameterized type or that the number of
   *   arguments does not match the parameterized type's number of
   *   parameters.
   */
  public InstantiatedT(List<Type> arguments, Type type) {
    super(type);
    this.arguments = arguments;
    if (! type.hasParameterized()) {
      throw new IllegalArgumentException("Not a parameterized type " + type);
    } else if (arguments.size()!=type.toParameterized().getParameters().size()) {
      throw new IllegalArgumentException("Wrong number of parameters " + type);
    }
  }

  /**
   * Create a new instantiated type.
   *
   * @param template The type whose annotations to copy.
   * @param arguments The arguments.
   * @param type The type.
   * @throws IllegalArgumentException Signals that the specified type
   *   does not contain a parameterized type or that the number of
   *   arguments does not match the parameterized type's number of
   *   parameters.
   */
  public InstantiatedT(Type template, List<Type> arguments, Type type) {
    super(template, type);
    this.arguments = arguments;
    if (! type.hasParameterized()) {
      throw new IllegalArgumentException("Not a parameterized type " + type);
    } else if (arguments.size()!=type.toParameterized().getParameters().size()) {
      throw new IllegalArgumentException("Wrong number of parameters " + type);
    }
  }

  public InstantiatedT copy() {
    return new InstantiatedT(this, Type.copy(arguments), getType().copy());
  }

  public Type seal() {
    if (! isSealed()) {
      super.seal();
      arguments = Type.seal(arguments);
    }
    return this;
  }

  public Type.Tag wtag() {
    return Type.Tag.INSTANTIATED;
  }

  public boolean isInstantiated() {
    return true;
  }

  public boolean hasInstantiated() {
    return true;
  }

  public InstantiatedT toInstantiated() {
    return this;
  }

  /**
   * Get this instantiated type's arguments.
   *
   * @return The arguments.
   */
  public List<Type> getArguments() {
    return arguments;
  }

  /**
   * Determine whether this type equals the specified object.  This
   * instantiated type equals the specified object if the object is an
   * equal wrapped type instantiated with equal types.
   *
   * @param o The object.
   * @return <code>true</code> if this type equals the specified
   *   object.
   */
  public boolean equals(Object o) {
    if (! (o instanceof Type)) return false;
    Type t = cast(o);

    if (! t.hasInstantiated()) return false;
    InstantiatedT other = t.toInstantiated();
    if (arguments.size() != other.arguments.size()) return false;
    for (int i=0; i<arguments.size(); i++) {
      if (! arguments.get(i).equals(other.arguments.get(i))) return false;
    }
    return getType().equals(other.getType());
  }

  public void write(Appendable out) throws IOException {
    if (1 == arguments.size()) {
      out.append("argument(");
    } else {
      out.append("arguments(");
    }
    for (Iterator<Type> iter = arguments.iterator(); iter.hasNext(); ) {
      iter.next().write(out);
      if (iter.hasNext()) out.append(", ");
    }
    out.append(") ");
    getType().write(out);
  }

}
