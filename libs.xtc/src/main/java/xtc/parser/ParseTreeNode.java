/*
 * xtc - The eXTensible Compiler
 * Copyright (C) 2006-2007 Robert Grimm
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

import java.io.IOException;

import java.util.List;

/**
 * Element to create a parse tree node capturing formatting.
 *
 * @see xtc.tree.Formatting
 *
 * @author Robert Grimm
 * @version $Revision: 1.4 $
 */
public class ParseTreeNode extends Element implements InternalElement {

  /**
   * The bindings capturing the values preceding the annotated node.
   */
  public List<Binding> predecessors;

  /**
   * The binding capturing the annotated node, which may be
   * <code>null</code>.
   */
  public Binding node;

  /**
   * The bindings capturing the values succeeding the annotated node.
   */
  public List<Binding> successors;

  /**
   * Create a new parse tree node.
   *
   * @param predecessors The predecessors.
   * @param node The node.
   * @param successors The successors.
   */
  public ParseTreeNode(List<Binding> predecessors, Binding node,
                       List<Binding> successors) {
    this.predecessors = predecessors;
    this.node         = node;
    this.successors   = successors;
  }

  public Tag tag() {
    return Tag.PARSE_TREE_NODE;
  }

  public int hashCode() {
    return 49 * predecessors.hashCode() + 7 * successors.hashCode() +
      (null == node ? 0 : node.hashCode());
  }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (! (o instanceof ParseTreeNode)) return false;
    ParseTreeNode other = (ParseTreeNode)o;
    return (predecessors.equals(other.predecessors) &&
            (null == node ? null == other.node : node.equals(other.node)) &&
            successors.equals(other.successors));
  }

  public void write(Appendable out) throws IOException {
    out.append("ParseTreeNode([");
    boolean first = true;
    for (Binding b : predecessors) {
      if (first) {
        first = false;
      } else {
        out.append(", ");
      }
      out.append(b.name);
    }
    out.append("], ");
    if (null == node) {
      out.append("null");
    } else {
      out.append(node.name);
    }
    out.append(", [");
    first = true;
    for (Binding b : successors) {
      if (first) {
        first = false;
      } else {
        out.append(", ");
      }
      out.append(b.name);
    }
    out.append("])");
  }

}
