/*
 * xtc - The eXTensible Compiler
 * Copyright (C) 2004 Robert Grimm
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
 * The superclass of memoization table columns.
 *
 * @author Robert Grimm
 * @version $Revision: 1.4 $
 */
public abstract class Column {

  /** The file name. */
  public String  file;
  
  /**
   * The flag for whether the previous character was a carriage
   * return.
   */
  public boolean seenCR;

  /** The current line. */
  public int     line;

  /** The current column. */
  public int     column;

}
