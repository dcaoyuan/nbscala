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

import xtc.util.Utilities;

/**
 * The superclass of class and interface types.
 *
 * @author Robert Grimm
 * @version $Revision: 1.21 $
 */
public abstract class ClassOrInterfaceT extends DerivedT {

  /** The fully qualfied name. */
  protected String qname;

  /** The list of {@link InterfaceT interfaces}. */
  protected List<Type> interfaces;

  /** The list of {@link VariableT fields}. */
  protected List<Type> fields;

  /** The list of {@link MethodT methods}. */
  protected List<Type> methods;

  /**
   * Create a new class or interface type.
   *
   * @param template The template whose annotations to copy.
   * @param qname The fully qualified name.
   * @param interfaces The list of interfaces.
   * @param fields The list of fields.
   * @param methods The list of methods.
   */
  public ClassOrInterfaceT(Type template, String qname, List<Type> interfaces,
                           List<Type> fields, List<Type> methods) {
    super(template);
    this.qname      = qname;
    this.interfaces = interfaces;
    this.fields     = fields;
    this.methods    = methods;
  }

  public Type seal() {
    if (! isSealed()) {
      super.seal();
      interfaces = Type.seal(interfaces);
      fields     = Type.seal(fields);
      methods    = Type.seal(methods);
    }
    return this;
  }

  /**
   * Get the qualified name.
   *
   * @return The qualified name.
   */
  public String getQName() {
    return qname;
  }

  /**
   * Get the qualifier for this class' or interface's name.
   *
   * @return The qualifier.
   */
  public String getQualifier() {
    return Utilities.getQualifier(qname);
  }

  /**
   * Get the unqualified name.
   *
   * @return The unqualified name.
   */
  public String getName() {
    return Utilities.getName(qname);
  }

  /**
   * Get the list of interfaces.
   *
   * @return The list of interfaces.
   */
  public List<Type> getInterfaces() {
    return interfaces;
  }

  /**
   * Get the list of fields.
   *
   * @return The list of fields.
   */
  public List<Type> getFields() {
    return fields;
  }

  /**
   * Get the list of methods.
   *
   * @return The list of methods.
   */
  public List<Type> getMethods() {
    return methods;
  }

  public int hashCode() {
    return qname.hashCode();
  }

  /**
   * Determine whether this type equals the specified object.  This
   * class or interface equals the specified object if the specified
   * object also is a class or interface with the same name.
   *
   * @param o The object.
   * @return <code>true</code> if this type equals the object.
   */
  public boolean equals(Object o) {
    if (! (o instanceof Type)) return false;
    Type t = resolve(o);

    if (this == t) return true;
    if (! getClass().equals(t.getClass())) return false;
    return qname.equals(((ClassOrInterfaceT)t).qname);
  }

}
