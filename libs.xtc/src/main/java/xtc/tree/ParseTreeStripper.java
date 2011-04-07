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
 * Visitor to strip parse trees.  This visitor eliminates any
 * formatting and tokens, replacing the former with the annotated node
 * and the latter with the text.
 *
 * @author Robert Grimm
 * @version $Revision: 1.2 $
 */
public class ParseTreeStripper extends Visitor {

  /** Create a new parse tree stripper. */
  public ParseTreeStripper() {
    // Nothing to do.
  }

  /** Visit the specified generic node. */
  public GNode visit(GNode n) {
    final int size = n.size();
    for (int i=0; i<size; i++) {
      Object o = n.get(i);
      if (o instanceof Node) {
        o = dispatch((Node)o);
      }
      n.set(i, o);
    }
    return n;
  }

  /** Visit the specified annotation. */
  public Annotation visit(Annotation n) {
    // Preserve the annotation.
    n.setNode((Node)dispatch(n.getNode()));
    return n;
  }

  /** Visit the specified formatting. */
  public Object visit(Formatting f) {
    // Strip the formatting.  Note that the returned object may be a
    // node or a string.
    return dispatch(f.getNode());
  }

  /** Visit the specified token. */
  public String visit(Token t) {
    // Strip the token.
    return t.getTokenText();
  }

}
