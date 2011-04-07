/*
 * xtc - The eXTensible Compiler
 * Copyright (C) 2007-2008 Robert Grimm
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
 * A type wildcard.
 *
 * @author Robert Grimm
 * @version $Revision: 1.3 $
 */
public class Wildcard extends Parameter {

  /** The canonical wildcard. */
  public static final Wildcard TYPE;

  static {
    TYPE = new Wildcard();
    TYPE.seal();
  }

  /** Create a new wildcard. */
  public Wildcard() {
    super();
  }

  /**
   * Create a new wildcard.
   *
   * @param template The type whose annotations to copy.
   */
  public Wildcard(Type template) {
    super(template);
  }

  public Wildcard copy() {
    return new Wildcard(this);
  }

  public Type.Tag tag() {
    return Type.Tag.WILDCARD;
  }

  public boolean isWildcard() {
    return true;
  }

  public Wildcard toWildcard() {
    return this;
  }

  /**
   * Bind this wildcard.  Wildcards cannot be bound.
   *
   * @param type The type.
   * @throws IllegalStateException Signals that wildcards cannot be
   *   bound.
   */
  public void bind(Type type) {
    throw new IllegalStateException("Unable to bind wildcard");
  }

  public int hashCode() {
    return 63;
  }

  public boolean equals(Object o) {
    if (! (o instanceof Type)) return false;
    Type t = resolve(o);

    if (this == t) return true;
    return t.isWildcard();
  }

}
