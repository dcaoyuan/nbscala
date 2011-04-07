/*
 * xtc - The eXTensible Compiler
 * Copyright (C) 2007 New York University
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * version 2.1 as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301,
 * USA.
 */
package xtc.typical;

/**
 * The superclass of all variants.
 *
 * @author Laune Harris
 * @author Robert Grimm
 * @version $Revision: 1.3 $
 */
public abstract class Variant<T extends Tuple> {

  /** The variant's tuple. */
  protected T tuple;

  /**
   * Create a new variant.
   *
   * @param tuple The tuple.
   */
  public Variant(T tuple) {
    this.tuple = tuple;
  }
  
  /** Create a new variant. */
  protected Variant() {
    // empty
  }
  
  /**
   * Get this variant's name.
   *
   * @return The name.
   */
  public abstract String getName();

  /**
   * Get this variant's tuple.
   *
   * @return The tuple.
   */
  public T getTuple() {
    return tuple;
  }

  public int hashCode() {
    return getName().hashCode() + 7 * tuple.hashCode();
  }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (! (o instanceof Variant)) return false;
    Variant<?> other = (Variant<?>)o;
    return getName().equals(other.getName()) && tuple.equals(other.tuple);
  }

  public String toString() {
    return getName() + tuple.toString();
  }

} 
