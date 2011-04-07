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
package xtc.type;

import java.io.IOException;

/**
 * An internal type, identified by its name.
 *
 * @author Robert Grimm
 * @version $Revision: 1.12 $
 */
public class InternalT extends Type {

  /** The canonical variable argument list type. */
  public static final InternalT VA_LIST = new InternalT("__builtin_va_list");

  /** Seal the canonical type. */
  static {
    VA_LIST.seal();
  }

  /** The name. */
  private final String name;

  /**
   * Create a new internal type.
   *
   * @param name The name.
   */
  public InternalT(String name) {
    this.name = name;
  }

  /**
   * Create a new internal type.
   *
   * @param template The type whose annotations to copy.
   * @param name The name.
   */
  public InternalT(Type template, String name) {
    super(template);
    this.name = name;
  }

  public InternalT copy() {
    return new InternalT(this, name);
  }

  public Type.Tag tag() {
    return Type.Tag.INTERNAL;
  }

  public boolean isInternal() {
    return true;
  }

  public InternalT toInternal() {
    return this;
  }

  /**
   * Get the name.
   *
   * @return The name.
   */
  public String getName() {
    return name;
  }

  public int hashCode() {
    return name.hashCode();
  }

  public boolean equals(Object o) {
    if (! (o instanceof Type)) return false;
    Type t = resolve(o);

    if (this == t) return true;
    if (! t.isInternal()) return false;
    return name.equals(((InternalT)t).name);
  }

  public void write(Appendable out) throws IOException {
    out.append(name);
  }

  public String toString() {
    return name;
  }

}
