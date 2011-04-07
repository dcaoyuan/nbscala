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

import xtc.util.Utilities;

/**
 * A match of a string to a grammar element.
 *
 * @author Robert Grimm
 * @version $Revision: 1.6 $
 */
public class StringMatch extends UnaryOperator {

  /** The text. */
  public final String text;

  /**
   * Create a new string match with the specified text and element.
   *
   * @param text The text.
   * @param element The matched element.
   */
  public StringMatch(String text, Element element) {
    super(element);
    this.text = text;
  }

  public Tag tag() {
    return Tag.STRING_MATCH;
  }

  public int hashCode() {
    return text.hashCode();
  }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (! (o instanceof StringMatch)) return false;
    StringMatch other = (StringMatch)o;
    if (! text.equals(other.text)) return false;
    return element.equals(other.element);
  }

  public void write(Appendable out) throws IOException {
    out.append('"');
    Utilities.escape(text, out, Utilities.JAVA_ESCAPES);
    out.append("\":");
    element.write(out);
  }

}
