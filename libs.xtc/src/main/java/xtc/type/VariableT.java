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
 * A variable.  This pseudo-type captures the name for globals,
 * locals, parameters, fields, and bitfields.  For the latter, it also
 * captures the field width.
 *
 * @author Robert Grimm
 * @version $Revision: 1.10 $
 */
public class VariableT extends WrappedT {

  /** The variable kind. */
  public static enum Kind {
    /** A global variable. */
    GLOBAL,
    /** A local variable. */
    LOCAL,
    /** A parameter. */
    PARAMETER,
    /** A field. */
    FIELD,
    /** A bit-field. */
    BITFIELD
  }

  // =========================================================================

  /** The kind. */
  private Kind kind;

  /** The name. */
  private String name;

  /** The width for bitfields. */
  private int width;

  /**
   * Create a new variable type.
   *
   * @param template The type whose annotations to copy.
   * @param type The type.
   * @param kind The kind.
   * @param name The name.
   */
  public VariableT(Type template, Type type, Kind kind, String name) {
    super(template, type);
    if (Kind.BITFIELD == kind) {
      throw new IllegalArgumentException("Invalid kind " + kind);
    }
    this.kind  = kind;
    this.name  = name;
    this.width = -1;
  }

  /**
   * Create a new bit-field.
   *
   * @param template The type whose annotations to copy.
   * @param type The type.
   * @param name The name.
   * @param width The width.
   */
  public VariableT(Type template, Type type, String name, int width) {
    super(template, type);
    if (0 > width) {
      throw new IllegalArgumentException("Negative width " + width);
    }
    this.kind  = Kind.BITFIELD;
    this.name  = name;
    this.width = width;
  }

  public VariableT copy() {
    return -1 == width ?
      new VariableT(this, getType().copy(), kind, name)
      : new VariableT(this, getType().copy(), name, width);
  }

  public Type.Tag wtag() {
    return Type.Tag.VARIABLE;
  }

  public boolean isVariable() {
    return true;
  }

  public boolean hasVariable() {
    return true;
  }

  public VariableT toVariable() {
    return this;
  }

  /**
   * Determine whether this variable has the specified kind.
   *
   * @param kind The kind.
   * @return <code>true</code> if this variable has the specified
   *   kind.
   */
  public boolean hasKind(Kind kind) {
    return kind == this.kind;
  }

  /**
   * Get the kind.
   *
   * @return The kind.
   */
  public Kind getKind() {
    return kind;
  }

  /**
   * Determine whether this variable has a name.
   *
   * @return <code>true</code> if this variable has a name.
   */
  public boolean hasName() {
    return null != name;
  }

  /**
   * Determine whether this variable has the specified name.
   *
   * @param name The name.
   * @return <code>true</code> if this variable has the name.
   */
  public boolean hasName(String name) {
    return name.equals(this.name);
  }

  /**
   * Get the name.
   *
   * @return The name.
   */
  public String getName() {
    return name;
  }

  /**
   * Determine whether this variable has a width.
   *
   * @return <code>true</code> if this variable has a width.
   */
  public boolean hasWidth() {
    return -1 != width;
  }

  /**
   * Get this variable's width.  If this variable is a bitfield, this
   * method returns the bitfield's width.  Otherwise, it returns -1.
   *
   * @return This variable's width.
   */
  public int getWidth() {
    return width;
  }

  public void write(Appendable out) throws IOException {
    switch (kind) {
    case GLOBAL:
      out.append("global"); break;
    case LOCAL:
      out.append("local");  break;
    case PARAMETER:
      out.append("param");  break;
    case FIELD:
      out.append("field");  break;
    case BITFIELD:
      out.append("bitfield"); break;
    }
    out.append('(');
    getType().write(out);
    out.append(", ");
    if (null != name) {
      out.append(name);
    } else {
      out.append("<none>");
    }
    if (-1 != width) {
      out.append(", ");
      out.append(Integer.toString(width));
    }
    out.append(')');
  }

  // =========================================================================

  /**
   * Create a new global variable.
   *
   * @param type The type.
   * @param name The name.
   */
  public static VariableT newGlobal(Type type, String name) {
    return new VariableT(null, type, Kind.GLOBAL, name);
  }

  /**
   * Create a new local variable.
   *
   * @param type The type.
   * @param name The name.
   */
  public static VariableT newLocal(Type type, String name) {
    return new VariableT(null, type, Kind.LOCAL, name);
  }

  /**
   * Create a new parameter.
   *
   * @param type The type.
   * @param name The name.
   */
  public static VariableT newParam(Type type, String name) {
    return new VariableT(null, type, Kind.PARAMETER, name);
  }

  /**
   * Create a new field.
   *
   * @param type The type.
   * @param name The name.
   */
  public static VariableT newField(Type type, String name) {
    return new VariableT(null, type, Kind.FIELD, name);
  }

  /**
   * Create a new bitfield.
   *
   * @param type The type.
   * @param name The name.
   * @param width The width.
   */
  public static VariableT newBitfield(Type type, String name, int width) {
    return new VariableT(null, type, name, width);
  }

}
