/*
 * xtc - The eXTensible Compiler
 * Copyright (C) 2004-2008 Robert Grimm
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
package xtc.parser;

/**
 * An erroneous parse.
 *
 * @author Robert Grimm
 * @version $Revision: 1.27 $
 */
public final class ParseError extends Result {

  /**
   * The dummy parse error.  The dummy parse error is used for
   * initializing a production's parse error and then threading the
   * most specific parse error through the production.  It works like
   * a sentinel for managing linked lists, avoiding repeated tests for
   * a <code>null</code> value.
   *
   * @see SemanticValue#error
   */
  public static final ParseError DUMMY = new ParseError("parse error", -1);

  /** The error message. */
  public final String msg;

  /**
   * Create a new parse error.
   *
   * @param msg The error message.
   * @param index The index for the error location.
   */
  public ParseError(final String msg, final int index) {
    super(index);
    this.msg = msg;
  }

  public boolean hasValue() {
    return false;
  }

  public boolean hasValue(final String s) {
    return false;
  }

  public boolean hasValueIgnoreCase(final String s) {
    return false;
  }

  public <T> T semanticValue() {
    throw new
      IllegalStateException("Parse error does not have a semantic value");
  }

  public ParseError parseError() {
    return this;
  }

  public ParseError select(final ParseError other) {
    return this.index <= other.index ? other : this;
  }

  public ParseError select(final ParseError other, final int index) {
    return this.index <= index || this.index <= other.index ? other : this;
  }

  /**
   * Select the more specific parse error.  This method compares this
   * parse error with the specified index and returns a parse error
   * representing the longer parse (creating a new parse error with
   * the specified message and index if necessary).
   *
   * @param msg The error message.
   * @param index The index of the parse error.
   */
  public ParseError select(final String msg, final int index) {
    return this.index <= index ? new ParseError(msg, index) : this;
  }

  public SemanticValue createValue(final Object value, final ParseError error) {
    throw new
      IllegalStateException("Parse error cannot lead to semantic value");
  }

}
