/*
 * xtc - The eXTensible Compiler
 * Copyright (C) 2004-2006 Robert Grimm
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

import java.util.ArrayList;
import java.util.List;

import xtc.type.Type;

/**
 * The meta-data for a production necessary for code generation.
 *
 * @author Robert Grimm
 * @version $Revision: 1.25 $
 */
public class MetaData {

  /** Flag for whether the production requires a character variable. */
  public boolean requiresChar = false;

  /** Flag for whether the production requires an index variable. */
  public boolean requiresIndex = false;

  /** Flag for whether the production requires a result variable. */
  public boolean requiresResult = false;

  /** Flag for whether the production requires a predicate index variable. */
  public boolean requiresPredIndex = false;
  
  /** Flag for whether the production requires a predicate result variable. */
  public boolean requiresPredResult = false;
  
  /** Flag for whether the production requires a predicate matched variable. */
  public boolean requiresPredMatch = false;

  /** Flag for whether the production requires a base index variable. */
  public boolean requiresBaseIndex = false;

  /** The number of times this production is referenced within the grammar. */
  public int usageCount = 0;

  /** The number of times this production references itself. */
  public int selfCount = 0;

  /**
   * The structure of repetitions for this production.  The length of
   * the list indicates the maximum depth of nested repetitions.  Each
   * list element is a <code>Boolean</code>, which is
   * <code>true</code> if any of the repetitions at that level has its
   * {@link Repetition#once} flag set.
   */
  public List<Boolean> repetitions;

  /**
   * The structure of bound repetitions for this production.  The
   * length of the list indicates the maximum depth of nested
   * repetitions.  Each list element is the (unified) type of all
   * lists at that level; it is <code>null</code> if none of the
   * repetitions has a bound semantic value.
   */
  public List<Type> boundRepetitions;

  /**
   * The structure of options for this production.  The length of the
   * list indicates the maximum depth of nested options.  Each list
   * element is the (unified) type of all bound options at that level;
   * it is <code>null</code> if none of the options has a bound
   * semantic value.
   */
  public List<Type> options;
  
  /**
   * Create a new meta-data record.
   *
   * <p />Note that the constructor allocates a new list for the
   * {@link #repetitions}, {@link #boundRepetitions} and {@link
   * #options} fields.
   */
  public MetaData() {
    repetitions      = new ArrayList<Boolean>();
    boundRepetitions = new ArrayList<Type>();
    options          = new ArrayList<Type>();
  }
 
}
