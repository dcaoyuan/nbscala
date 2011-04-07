/*
 * xtc - The eXTensible Compiler
 * Copyright (C) 2007 Robert Grimm
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
package xtc.tree;

/**
 * Visitor to relocate an abstract syntax tree.  This visitor strips
 * all line markers from the abstract syntax tree, while also updating
 * all nodes with locations to reflect the source location relative to
 * the last line marker.
 *
 * @author Robert Grimm
 * @version $Revision: 1.1 $
 */
public class Relocator extends Visitor {

  /** The currently marked file. */
  protected String markedFile;

  /** The currently marked line. */
  protected int markedLine;

  /** The line of the last line marker. */
  protected int baseLine;

  /** Create a new relocator. */
  public Relocator() {
    markedFile = null;
    markedLine = -1;
    baseLine   = -1;
  }

  /**
   * Relocate the specified node.
   *
   * @param n The node.
   */
  protected void relocate(Node n) {
    if ((null == markedFile) || (null == n.location)) return;

    final int line = n.location.line - baseLine - 1 + markedLine;
    if ((line != n.location.line) || (! n.location.file.equals(markedFile))) {
      n.location = new Location(markedFile, line, n.location.column, n.location.offset, n.location.endOffset);
    }
  }

  /** Process the specified node. */
  public Node visit(Node n) {
    relocate(n);

    for (int i=0; i<n.size(); i++) {
      Object o = n.get(i);
      if (o instanceof Node) n.set(i, dispatch((Node)o));
    }

    return n;
  }

  /** Process the specified annotation. */
  public Annotation visit(Annotation a) {
    relocate(a);

    a.node = (Node)dispatch(a.node);

    return a;
  }

  /** Process the specified line marker. */
  public Node visit(LineMarker m) {
    if (null == m.location) {
      throw new IllegalArgumentException("Line marker without location");
    }

    markedFile = m.file;
    markedLine = m.line;
    baseLine   = m.location.line;

    return (Node)dispatch(m.node);
  }

}
