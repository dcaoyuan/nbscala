/*
 * xtc - The eXTensible Compiler
 * Copyright (C) 2004-2007 Robert Grimm
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

import java.util.List;

import xtc.Constants;

import xtc.tree.Attribute;
import xtc.tree.Comment;
import xtc.tree.Node;

/**
 * A grammar module.
 *
 * @author Robert Grimm
 * @version $Revision: 1.19 $
 */
public class Module extends Node {

  /** The option documentation comment. */
  public Comment documentation;

  /** The module name. */
  public ModuleName name;

  /** The optional list of parameters. */
  public ModuleList parameters = null;

  /** The optional list of {@link ModuleDependency Module dependencies}. */
  public List<ModuleDependency> dependencies = null;

  /**
   * The auxiliary field referencing this module's module
   * modification, if the list of dependencies contains it.
   */
  public ModuleModification modification = null;

  /** The optional initial action code. */
  public Action header = null;

  /** The optional main action code. */
  public Action body = null;

  /** The optional final action code. */
  public Action footer = null;

  /**
   * The optional attribute list.  Note that while a module's
   * attributes are represented as a list, they should be treated as a
   * set.
   */
  public List<Attribute> attributes = null;

  /** The list of productions. */
  public List<Production> productions;

  /** Create a new grammar module. */
  public Module() { /* Nothing to do. */ }

  /**
   * Create a new grammar module.
   *
   * @param documentation The documentation.
   * @param name The module name.
   * @param parameters The list of parameters.
   * @param dependencies The list of dependencies.
   * @param header The header.
   * @param body The body.
   * @param footer The footer.
   * @param attributes The list of attributes.
   * @param productions The list of productions.
   */
  public Module(Comment documentation, ModuleName name, ModuleList parameters,
                List<ModuleDependency> dependencies,
                Action header, Action body, Action footer,
                List<Attribute> attributes, List<Production> productions) {
    this.documentation = documentation;
    this.name          = name;
    this.parameters    = parameters;
    this.dependencies  = dependencies;
    this.header        = header;
    this.body          = body;
    this.footer        = footer;
    this.attributes    = attributes;
    this.productions   = productions;
  }

  /**
   * Determine whether this module has the specified attribute.
   *
   * @param att The attribute.
   * @return <code>true</code> if this module has the specified
   *   attribute.
   */
  public boolean hasAttribute(Attribute att) {
    return ((null != attributes) && attributes.contains(att));
  }

  /**
   * Determine whether this module has an attribute with the
   * specified name.
   *
   * @param name The name.
   * @return <code>true</code> if this module has an attribute
   *   with the specified name.
   */
  public boolean hasAttribute(String name) {
    return null != Attribute.get(name, attributes);
  }

  /**
   * Get the value of the attribute with the specified name.
   *
   * @param name The name.
   * @return The corresponding attribute's value.
   */
  public Object getAttributeValue(String name) {
    return Attribute.get(name, attributes).getValue();
  }

  /**
   * Get the class name for this grammar module.  If this grammar
   * module has a {@link Constants#NAME_PARSER parser} attribute, the
   * class name is the value of that attribute.  Otherwise, it is the
   * module name.
   *
   * @return The class name.
   */
  public String getClassName() {
    Attribute att = Attribute.get(Constants.NAME_PARSER, attributes);
    return null == att ? name.name : (String)att.getValue();
  }

}
