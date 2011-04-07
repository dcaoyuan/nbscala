/*
 * xtc - The eXTensible Compiler
 * Copyright (C) 2004, 2007 Robert Grimm
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
package xtc.tree;

/**
 * A visiting exception is thrown to indicate an exceptional condition
 * while visiting a node.
 *
 * @author Robert Grimm
 * @version $Revision: 1.6 $
 */
public class VisitingException extends TraversalException {
  
  /**
   * Create a new visiting exception with the specified detail message
   * and cause.
   *
   * @param message The detail message.
   * @param cause The cause.
   */
  public VisitingException(String message, Throwable cause) {
    super(message, cause);
  }

}
