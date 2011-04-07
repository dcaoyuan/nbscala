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

/**
 * A type alias.
 *
 * @author Robert Grimm
 * @version $Revision: 1.28 $
 */
public class AliasT extends WrappedT {

  /** The name. */
  private String name;

  /**
   * Create a new type alias.
   *
   * @param name The name.
   */
  public AliasT(String name) {
    super(null);
    this.name = name;
  }

  /**
   * Create a new type alias.
   *
   * @param name The name.
   * @param type The type.
   */
  public AliasT(String name, Type type) {
    super(type);
    this.name = name;
  }

  /**
   * Create a new type alias.
   *
   * @param template The type whose annotations to copy.
   * @param name The name.
   * @param type The type.
   */
  public AliasT(Type template, String name, Type type) {
    super(template, type);
    this.name = name;
  }

  /**
   * Seal this alias.  If this alias is incomplete, i.e., does not
   * have a type, invocations to this method have no effect.
   */
  public Type seal() {
    if (null != getType()) {
      if (! isSealed()) {
        super.seal();
      }
    }
    return this;
  }

  public AliasT copy() {
    return new AliasT(this, name, getType().copy());
  }

  public Type.Tag wtag() {
    return Type.Tag.ALIAS;
  }

  public boolean isAlias() {
    return true;
  }

  public boolean hasAlias() {
    return true;
  }

  public AliasT toAlias() {
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

  public void write(Appendable out) throws IOException {
    out.append("alias(");
    out.append(name);
    if (null != getType()) {
      out.append(", ");
      getType().write(out);
    }
    out.append(')');
  }

}
