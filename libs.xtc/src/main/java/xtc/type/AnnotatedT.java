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
 * An annotated type.  This pseudo-type is useful for adding
 * annotations to a type without modifying the underlying type.
 *
 * @author Robert Grimm
 * @version $Revision: 1.21 $
 */
public class AnnotatedT extends WrappedT {

  /**
   * Create a new annotated type.
   *
   * @param type The type to annotate.
   */
  public AnnotatedT(Type type) {
    super(type);
  }

  /**
   * Create a new annotated type.
   *
   * @param template The type whose annotations to copy.
   * @param type The type to annotate.
   */
  public AnnotatedT(Type template, Type type) {
    super(template, type);
  }

  public AnnotatedT copy() {
    return new AnnotatedT(this, getType().copy());
  }

  public Type.Tag wtag() {
    return Type.Tag.ANNOTATED;
  }

  public boolean isAnnotated() {
    return true;
  }

  public AnnotatedT toAnnotated() {
    return this;
  }

  public void write(Appendable out) throws IOException {
    out.append("annotated(");
    getType().write(out);
    out.append(')');
  }

}
