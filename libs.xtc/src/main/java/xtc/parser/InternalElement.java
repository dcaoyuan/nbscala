/*
 * xtc - The eXTensible Compiler
 * Copyright (C) 2004 Robert Grimm
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

/**
 * The marker interface for internal grammar elements.  Internal
 * grammar elements cannot appear in the abstract syntax tree of a
 * parsed grammar, but rather are added to the tree as part of the
 * packrat parser generator's transformations.  Note that any class
 * implementing this interface must also be a {@link Element}.
 *
 * @author Robert Grimm
 * @version $Revision: 1.5 $
 */
public interface InternalElement {
  /* Empty marker interface. */
}
