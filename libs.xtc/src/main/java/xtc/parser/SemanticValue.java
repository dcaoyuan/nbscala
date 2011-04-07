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
 * A semantic value.
 *
 * @author Robert Grimm
 * @version $Revision: 1.23 $
 */
public final class SemanticValue extends Result {

  /** The actual value. */
  public final Object value;

  /**
   * The embedded parse error.  An embedded parse error is the most
   * specific parse error encountered during the parse leading to this
   * semantic value (typically, returned by an unsuccessful option).
   * It is used to possibly replace a less specific parse error
   * generated while continuing to parse the input.  This field must
   * not be <code>null</code>; instead, a {@link ParseError#DUMMY
   * dummy parse error} should be used.
   */
  public final ParseError error;

  /**
   * Create a new semantic value.
   *
   * @param value The value.
   * @param index The index into the rest of the input.
   */
  public SemanticValue(final Object value, final int index) {
    this(value, index, ParseError.DUMMY);
  }

  /**
   * Create a new semantic value.
   *
   * @param value The value.
   * @param index The index into the rest of the input.
   * @param error The embedded parse error.
   */
  public SemanticValue(final Object value, final int index,
                       final ParseError error) {
    super(index);
    this.value = value;
    this.error = error;
  }

  public boolean hasValue() {
    return true;
  }

  public boolean hasValue(final String s) {
    return s.equals(this.value);
  }

  public boolean hasValueIgnoreCase(final String s) {
    return s.equalsIgnoreCase(this.value.toString());
  }

  @SuppressWarnings("unchecked")
  public <T> T semanticValue() {
    return (T)value;
  }

  public ParseError parseError() {
    return error;
  }

  public ParseError select(final ParseError error) {
    return this.error.index <= error.index ? error : this.error;
  }

  public ParseError select(final ParseError error, final int index) {
    return this.error.index <= index || this.error.index <= error.index ?
      error : this.error;
  }

  public SemanticValue createValue(final Object value, final ParseError error) {
    return value == this.value && error == this.error ?
      this : new SemanticValue(value, index, error);
  }

}
