/*
 * xtc - The eXTensible Compiler
 * Copyright (C) 2004-2007 Robert Grimm
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
package xtc.parser;

import java.io.IOException;

import java.util.ArrayList;
import java.util.List;

/**
 * A sequence of grammar elements.
 *
 * <p />A sequence may have an optional name.  However, this name is
 * not considered when comparing sequences for equality.
 *
 * @author Robert Grimm
 * @version $Revision: 1.30 $
 */
public class Sequence extends Element {

  /** The optional name. */
  public SequenceName name;

  /** The ordered list of grammar elements. */
  public List<Element> elements;

  /** Create a new, empty sequence. */
  public Sequence() {
    this(null, new ArrayList<Element>());
  }

  /**
   * Create a new, empty sequence with the specified capacity.
   *
   * @param capacity The capacity.
   */
  public Sequence(int capacity) {
    this(null, new ArrayList<Element>(capacity));
  }

  /**
   * Create a new sequence.
   *
   * @param elements The list of elements.
   */
  public Sequence(final List<Element> elements) {
    this(null, elements);
  }

  /**
   * Create a new sequence.
   *
   * @param name The name.
   * @param elements The list of elements.
   */
  public Sequence(final SequenceName name, final List<Element> elements) {
    this.name     = name;
    this.elements = elements;
  }

  /**
   * Create a new sequence with the specified element. If the element
   * is another sequence, the new sequence has the same elements but
   * is a copy. Otherwise, the new sequence has the specified element
   * as its only element.  In either case, the new sequence has the
   * element's location.
   *
   * @param element The element.
   */
  public Sequence(final Element element) {
    if (element instanceof Sequence) {
      Sequence s = (Sequence)element;

      elements   = new ArrayList<Element>(s.elements);
      name       = s.name;
    } else {
      elements = new ArrayList<Element>(1);
      elements.add(element);
    }
    setLocation(element);
  }

  public Tag tag() {
    return Tag.SEQUENCE;
  }

  /**
   * Remove all elements from this sequence.
   *
   * @return This sequence.
   */
  public Sequence clear() {
    elements.clear();
    return this;
  }

  /**
   * Add the specified element to this sequence.
   *
   * @param e The element to add.
   * @return This sequence.
   */
  public Sequence add(Element e) {
    elements.add(e);
    return this;
  }

  /**
   * Add all elements in the specified list to this sequence.
   *
   * @param l The list of elements.
   * @return This sequence.
   */
  public Sequence addAll(List<Element> l) {
    elements.addAll(l);
    return this;
  }

  /**
   * Determine whether this sequence is empty.
   *
   * @return <code>true</code> if this is an empty sequence.
   */
  public boolean isEmpty() {
    return elements.isEmpty();
  }

  /**
   * Get the size of this sequence.
   *
   * @return The size.
   */
  public int size() {
    return elements.size();
  }

  /**
   * Get the element at the specified index.
   *
   * @param idx The index.
   * @return The element at that position.
   * @throws IndexOutOfBoundsException
   *   Signals that the index is out of range.
   */
  public Element get(final int idx) {
    return elements.get(idx);
  }

  /**
   * Determine whether this sequence's last element is an ordered
   * choice.
   *
   * @return <code>true</code> if the last element is a choice.
   */
  public boolean hasTrailingChoice() {
    final int size = elements.size();
    return (0 < size) && (elements.get(size-1) instanceof OrderedChoice);
  }

  /**
   * Create a new subsequence from the specified start index.  The new
   * subsequence ends with the last element of this sequence.
   *
   * @param start The inclusive start index.
   * @return The subsequence.
   * @throws IndexOutOfBoundsException
   *   Signals that the index is out of range.
   */
  public Sequence subSequence(final int start) {
    return subSequence(start, elements.size());
  }

  /**
   * Create a new subsequence for the specified indices.
   *
   * @param start The inclusive start index.
   * @param end The exclusive end index.
   * @return The subsequence.
   * @throws IndexOutOfBoundsException
   *   Signals that the index is out of range.
   */
  public Sequence subSequence(final int start, final int end) {
    // The subsequence has the original's source location.
    Sequence s =
      new Sequence(new ArrayList<Element>(elements.subList(start, end)));
    s.setLocation(this);
    return s;
  }

  public int hashCode() {
    return elements.hashCode();
  }

  public boolean equals(final Object o) {
    if (this == o) return true;
    if (! (o instanceof Sequence)) return false;
    return elements.equals(((Sequence)o).elements);
  }

  public void write(Appendable out) throws IOException {
    out.append('(');
    boolean first = true;
    for (Element e : elements) {
      if (first) {
        first = false;
      } else {
        out.append(' ');
      }
      e.write(out);
    }
    out.append(')');
  }

  /**
   * Ensure that the specified element is a sequence. If the specified
   * element is not a sequence, a new sequence with the specified
   * element as its only element is returned; the newly created
   * sequence has the element's location.
   *
   * @param e The element.
   * @return The element in/as a sequence.
   */
  public static Sequence ensure(final Element e) {
    if (e instanceof Sequence) {
      return (Sequence)e;
    } else {
      Sequence s = new Sequence(e);
      s.setLocation(e);
      return s;
    }
  }

}
