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
 * An interface type.
 *
 * @author Robert Grimm
 * @version $Revision: 1.12 $
 */
public class InterfaceT extends ClassOrInterfaceT {

  /**
   * Create a new interface type.
   *
   * @param qname The fully qualified name.
   * @param interfaces The list of extended interfaces.
   * @param fields The list of fields.
   * @param methods The list of methods.
   */
  public InterfaceT(String qname, List<Type> interfaces, List<Type> fields,
                    List<Type> methods) {
    super(null, qname, interfaces, fields, methods);
  }

  /**
   * Create a new interface type.
   *
   * @param template The type whose annotations to copy.
   * @param qname The fully qualified name.
   * @param interfaces The list of extended interfaces.
   * @param fields The list of fields.
   * @param methods The list of methods.
   */
  public InterfaceT(Type template, String qname, List<Type> interfaces,
                    List<Type> fields, List<Type> methods) {
    super(template, qname, interfaces, fields, methods);
  }

  public InterfaceT copy() {
    return new InterfaceT(this, qname, copy(interfaces), copy(fields),
                          copy(methods));
  }

  public Type.Tag tag() {
    return Type.Tag.INTERFACE;
  }

  public boolean isInterface() {
    return true;
  }

  public InterfaceT toInterface() {
    return this;
  }

  public void write(Appendable out) throws IOException {
    out.append("interface ");
    out.append(qname);
  }

}
