/*
 * xtc - The eXTensible Compiler
 * Copyright (C) 2006 Robert Grimm
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
 * An iterator over one element.
 *
 * @author Robert Grimm
 * @version $Revision: 1.3 $
 */
public class SingletonIterator<T> implements Iterator<T> {

  /** The flag for whether the element is available. */
  private boolean available;

  /** The singleton element. */
  private T element;

  /**
   * Create a new singleton iterator.
   *
   * @param element The element.
   */
  public SingletonIterator(T element) {
    this.element = element;
    available    = true;
  }

  public boolean hasNext() {
    return available;
  }

  public T next() {
    if (available) {
      available = false;
      return element;
    } else {
      throw new NoSuchElementException();
    }
  }

  public void remove() {
    throw new UnsupportedOperationException();
  }

}
