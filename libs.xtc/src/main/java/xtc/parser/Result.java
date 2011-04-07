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
 * The superclass of all parser results.
 *
 * @author Robert Grimm
 * @version $Revision: 1.23 $
 */
public abstract class Result {

  /** The index into the parser's memoization table. */
  public final int index;

  /**
   * Create a new result with the specified index.
   *
   * @param index The index.
   */
  public Result(int index) {
    this.index = index;
  }

  /**
   * Determine whether this result has a value.
   *
   * @return <code>true</code> if this result has a value.
   */
  public abstract boolean hasValue();

  /**
   * Determine whether this result has the specified string value.
   * The specified value must not be <code>null</code>.
   *
   * @param s The string value.
   * @return <code>true</code> if this result has the specified string
   *   value.
   */
  public abstract boolean hasValue(String s);

  /**
   * Determine whether this result has the specified string value,
   * ignoring case.  The specified value must not be
   * <code>null</code>.
   *
   * @param s The string value.
   * @return <code>true</code> if this result has the specified string
   *   value, ignoring case.
   */
  public abstract boolean hasValueIgnoreCase(String s);

  /**
   * Get the semantic value for this result.
   *
   * @return The semantic value for this result.
   * @throws IllegalStateException
   *   Signals that this result does not have a semantic value.
   */
  public abstract <T> T semanticValue();

  /**
   * Get the parse error for this result.  If the result does not
   * represent a parse error or does not have an embedded parse error,
   * this method must return the {@link ParseError#DUMMY dummy parse
   * error}.
   *
   * @return The parse error for this result.
   */
  public abstract ParseError parseError();

  /**
   * Select the more specific parse error.  This method compares this
   * result (i.e., either the parse error or the embedded parse error)
   * with the specified parse error and returns the one representing
   * the longer parse.
   *
   * @param error The error.
   * @return The more specific parse error.
   */
  public abstract ParseError select(ParseError error);

  /**
   * Select the more specific parse error.  This method comparse this
   * result (i.e., either the parse error or embedded parse error)
   * with specified error and index.  If this result represents an
   * error beyond the specified error and index, it returns this
   * result's error.  Otherwise, it returns the specified parse error.
   *
   * @param error The error.
   * @param index The index.
   * @return The more specific parse error.
   */
  public abstract ParseError select(ParseError error, int index);

  /**
   * Create a semantic value based on this result.  A straight-forward
   * implementation of this method simply creates a new semantic
   * value, using the specified actual value, this result's index,
   * and the specified parse error:<pre>
   *   public SemanticValue value(Object value, ParseError error) {
   *     return new SemanticValue(value, index, error);
   *   }
   * </pre>However, for a result that already is a semantic value, a
   * more sophisticated implementation can avoid creating a new
   * semantic value if the specified actual value and parse error are
   * <i>identical</i> to those for this result.
   *
   * @param value The actual value.
   * @param error The embedded parse error.
   * @throws IllegalStateException Signals that this result is a
   *   parse error.
   */
  public abstract SemanticValue createValue(Object value, ParseError error);

}

