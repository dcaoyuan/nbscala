/*
 * xtc - The eXTensible Compiler
 * Copyright (C) 2004-2007 Robert Grimm
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
 * The superclass of all annotations.  An annotation adds information
 * (such as comments) to an abstract syntax tree node; it is usually
 * ignored while processing the AST.
 *
 * @author Robert Grimm
 * @version $Revision: 1.17 $
 */
public abstract class Annotation extends Node {

  /** The annotated node. */
  Node node;

  /** Create a new, empty annotation. */
  public Annotation() {
    node = null;
  }

  /**
   * Create a new annotation for the specified node.
   *
   * @param node The node.
   */
  public Annotation(Node node) {
    this.node = node;
  }

  public String getTokenText() {
    return node.getTokenText();
  }

  public boolean isAnnotation() {
    return true;
  }

  public Annotation toAnnotation() {
    return this;
  }

  /**
   * Get the annotated node.
   *
   * @return The annotated node.
   */
  public Node getNode() {
    return node;
  }

  /**
   * Set the annotated node.
   *
   * @param node The new node.
   */
  public void setNode(Node node) {
    this.node = node;
  }

  public Node strip() {
    return null == node ? null : node.strip();
  }

  /**
   * Return the inner-most annotation.  This method strips all nested
   * annotations, starting with this annotation, until it reaches the
   * inner-most annotation.
   *
   * @return The inner-most annotation.
   */
  public Annotation innerMost() {
    Annotation n = this;

    while (n.node instanceof Annotation) {
      n = (Annotation)n.node;
    }

    return n;
  }

}
