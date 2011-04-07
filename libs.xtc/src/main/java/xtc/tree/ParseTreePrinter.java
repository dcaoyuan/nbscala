/*
 * xtc - The eXTensible Compiler
 * Copyright (C) 2007 Robert Grimm
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
 * Visitor to print parse trees.
 *
 * @author Robert Grimm
 * @version $Revision: 1.3 $
 */
public class ParseTreePrinter extends Visitor {

  /** The printer. */
  protected final Printer printer;

  /**
   * Create a new parse tree printer.
   *
   * @param printer The printer.
   */
  public ParseTreePrinter(Printer printer) {
    this.printer = printer;
    printer.register(this);
  }

  /** Print the specified generic node. */
  public void visit(GNode n) {
    for (Object o : n) {
      if (o instanceof Node) dispatch((Node)o);
    }
  }

  /** Print the specified formatting node. */
  public void visit(Formatting f) {
    for (Object o : f) {
      if (o instanceof Node) dispatch((Node)o);
    }
  }

  /** Print the specified token. */
  public void visit(Token t) {
    printer.p(t.getTokenText());
  }

}
