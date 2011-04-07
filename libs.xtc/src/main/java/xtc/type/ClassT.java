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

import java.io.IOException;

import java.util.List;

/**
 * A class type.
 *
 * @author Robert Grimm
 * @version $Revision: 1.13 $
 */
public class ClassT extends ClassOrInterfaceT {

  /** The optional parent class. */
  private Type parent;

  /**
   * Create a new class type.
   *
   * @param qname The fully qualified name.
   * @param parent The optional parent class.
   * @param interfaces The list of interfaces.
   * @param fields The list of fields.
   * @param methods The list of methods.
   */
  public ClassT(String qname, Type parent, List<Type> interfaces,
                List<Type> fields, List<Type> methods) {
    super(null, qname, interfaces, fields, methods);
    this.parent = parent;
  }

  /**
   * Create a new class type.
   *
   * @param template The type whose annotations to copy.
   * @param qname The fully qualified name.
   * @param parent The optional parent class.
   * @param interfaces The list of interfaces.
   * @param fields The list of fields.
   * @param methods The list of methods.
   */
  public ClassT(Type template, String qname, Type parent, List<Type> interfaces,
                List<Type> fields, List<Type> methods) {
    super(template, qname, interfaces, fields, methods);
    this.parent = parent;
  }

  public ClassT copy() {
    return new ClassT(this, qname, parent.copy(), copy(interfaces),
                      copy(fields), copy(methods));
  }

  public Type.Tag tag() {
    return Type.Tag.CLASS;
  }

  public boolean isClass() {
    return true;
  }

  public ClassT toClass() {
    return this;
  }

  /**
   * Get the parent class.
   *
   * @return The parent class.
   */
  public Type getParent() {
    return parent;
  }

  public void write(Appendable out) throws IOException {
    out.append("class ");
    out.append(qname);
  }

}
