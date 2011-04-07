/*
 * xtc - The eXTensible Compiler
 * Copyright (C) 2005, 2007 Robert Grimm
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

/**
 * A command line option.
 *
 * @author Robert Grimm
 * @version $Revision: 1.6 $
 */
class Option {

  /** The kind. */
  public static enum Kind { BOOLEAN, WORD, INTEGER, FILE, DIRECTORY, ATTRIBUTE }

  /** The kind. */
  public final Kind kind;

  /** The external name. */
  public final String external;

  /** The internal name. */
  public final String internal;

  /** The default value, which must be consistent with the kind. */
  public final Object value;

  /** The flag for multiple occurrences. */
  public final boolean multiple;

  /** The description. */
  public final String description;

  /**
   * Create a new option.
   *
   * @param kind The kind.
   * @param external The external name.
   * @param internal The internal name.
   * @param value The default value, which may be <code>null</code>.
   * @param multiple The flag for multiple occurrences.
   * @param description The description.
   */
  public Option(Kind kind, String external, String internal, Object value,
                boolean multiple, String description) {
    this.kind        = kind;
    this.external    = external;
    this.internal    = internal;
    this.value       = value;
    this.multiple    = multiple;
    this.description = description;
  }

}
