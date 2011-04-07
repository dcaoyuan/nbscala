/*
 * xtc - The eXTensible Compiler
 * Copyright (C) 2005-2007 Robert Grimm
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
package xtc.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * An iterator over nothing.
 *
 * @author Robert Grimm
 * @version $Revision: 1.10 $
 */
public class EmptyIterator<T> implements Iterator<T> {

  /** The canonical empty iterator. */
  private static final EmptyIterator VALUE = new EmptyIterator();

  /** Create a new empty iterator. */
  private EmptyIterator() { /* Nothing to do. */ }

  public boolean hasNext() {
    return false;
  }

  public T next() {
    throw new NoSuchElementException();
  }

  public void remove() {
    throw new UnsupportedOperationException();
  }

  /**
   * Get the canoncial empty iterator.
   *
   * @return The canonical empty iterator.
   */
  @SuppressWarnings({ "unchecked", "cast" })
  public static final <T> EmptyIterator<T> value() {
    return (EmptyIterator<T>)VALUE;
  }

}
