/*
 * xtc - The eXTensible Compiler
 * Copyright (C) 2005-2007 Robert Grimm
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import xtc.Constants;

import xtc.tree.Node;
import xtc.tree.Printer;

/**
 * A symbol table.  This class implements a symbol table, which maps
 * symbols represented as strings to values of any type.  The mapping
 * is organized into hierarchical {@link Scope scopes}, which allows
 * for multiple definitions of the same symbol across different
 * scopes.  Additionally, a symbol may have multiple definitions
 * within the same scope: if the corresponding value is a Java
 * collections framework list, it is recognized as a multiply defined
 * symbol.  Scopes are named, with names being represented as strings.
 * Both scope names and symbols can be unqualified &mdash; that is,
 * they need to be resolved relative to the {@link #current() current
 * scope} &mdash; or qualified by the {@link Constants#QUALIFIER
 * qualification character} '<code>.</code>' &mdash; that is, they are
 * resolved relative to the symbol table's {@link #root() root}.  Once
 * {@link #enter(String) created}, a scope remains in the symbol table
 * and the corresponding AST node should be associated with that scope
 * by setting the corresponding {@link Constants#SCOPE property} to
 * the scope's qualified name.  Subsequent traversals over that node
 * can then automatically {@link #enter(Node) enter} and {@link
 * #exit(Node) exit} that scope.  Alternatively, if traversing out of
 * tree order, the current scope can be set {@link
 * #setScope(SymbolTable.Scope) explicitly}.
 *
 * <p />To support different name spaces within the same scope, this
 * class can optionally {@link #toNameSpace mangle} and {@link
 * #fromNameSpace unmangle} unqualified symbols.  By convention, a
 * name in any name space besides the default name space is prefixed
 * by the name of the name space and an opening parenthesis
 * '<code>(</code>' and suffixed by a closing parenthesis
 * '<code>)</code>'.
 *
 * @author Robert Grimm
 * @version $Revision: 1.34 $
 */
public class SymbolTable {

  /**
   * A symbol table scope.  A scope has a name and may have a parent
   * (unless it is the root scope), one or more nested scopes, and one
   * or more definitions.
   */
  public static class Scope {

    /** The name. */
    String name;

    /** The fully qualified name. */
    String qName;

    /** The parent scope. */
    Scope parent;

    /** The nested scopes, if any. */
    Map<String, Scope> scopes;

    /** The map from symbols to values, if any. */
    Map<String, Object> symbols;

    /**
     * Create a new root scope with the specified name, which may be
     * the empty string.
     *
     * @param name The name.
     */
    Scope(String name) {
      this.name  = name;
      this.qName = name;
    }

    /**
     * Create a new nested scope with the specified unqualified name
     * and parent.
     *
     * @param name The unqualified name.
     * @param parent The parent.
     * @throws IllegalArgumentException
     *   Signals that the specified parent already has a nested scope
     *   with the specified name.
     */
    Scope(String name, Scope parent) {
      if ((null != parent.scopes) && parent.scopes.containsKey(name)) {
        throw new IllegalArgumentException("Scope " + parent.qName +
                                           " already contains scope " + name);
      }
      this.name   = name;
      this.qName  = Utilities.qualify(parent.qName, name);
      this.parent = parent;
      if (null == parent.scopes) {
        parent.scopes = new HashMap<String, Scope>();
      }
      parent.scopes.put(name, this);
    }

    /**
     * Get this scope's unqualfied name.
     *
     * @return This scope's unqualified name.
     */
    public String getName() {
      return name;
    }

    /**
     * Get this scope's qualified name.
     *
     * @return This scope's qualified name.
     */
    public String getQualifiedName() {
      return qName;
    }

    /**
     * Update this scope's qualified name relative to the parent
     * scope's qualified name.  This method also requalifies any
     * nested scopes' qualified names.  It must not be called on the
     * root scope.
     */
    void requalify() {
      qName = Utilities.qualify(parent.qName, name);

      if (null != scopes) {
        for (Scope scope : scopes.values()) {
          scope.requalify();
        }
      }
    }

    /**
     * Determine whether this scope is the root scope.
     *
     * @return <code>true</code> if this scope is the root scope.
     */
    public boolean isRoot() {
      return (null == parent);
    }

    /**
     * Get this scope's parent.
     *
     * @return This scope's parent scope or <code>null</code> if this
     *   scope does not have a parent (i.e., is the root scope).
     */
    public Scope getParent() {
      return parent;
    }

    /**
     * Determine whether this scope has any nested scopes.
     *
     * @return <code>true</code> if this scope has any nested scopes.
     */
    public boolean hasNested() {
      return ((null != scopes) && (0 < scopes.size()));
    }

    /**
     * Get an iterator over the names of all nested scopes.
     *
     * @return An iterator over the nested scopes.
     */
    public Iterator<String> nested() {
      if (null == scopes) {
        return EmptyIterator.value();
      } else {
        return scopes.keySet().iterator();
      }
    }

    /**
     * Determine whether this scope has the specified unqualified
     * nested scope.
     *
     * @param name The nested scope's unqualified name.
     * @return <code>true</code> if the corresponding scope exists.
     */
    public boolean hasNested(String name) {
      return (null != getNested(name));
    }

    /**
     * Get the nested scope with the specified unqualified name.
     *
     * @param name The nested scope's unqualified name.
     * @return The corresponding scope or <code>null</code> if there is
     *   no such scope.
     */
    public Scope getNested(String name) {
      return (null == scopes)? null : scopes.get(name);
    }

    /**
     * Determine whether the scope with the specified unqualified name
     * can be merged into this scope.  A nested scope can be merged if
     * it (1) does not contain any bindings with the same names as
     * this scope's bindings and (2) does not have any children with
     * the same names as this scope's children.
     *
     * @param name The nested scope's unqualified name.
     * @return <code>true</code> if the scope can be merged.
     * @throws IllegalArgumentException Signals that this scope does
     *   not have a nested scope with the specified name.
     */
    public boolean isMergeable(String name) {
      Scope nested = getNested(name);

      if (null == nested) {
        throw new IllegalArgumentException("Scope " + qName + " does not " +
                                           " contain scope " + name);
      }

      if (null != nested.scopes) {
        // Note that this scope must have nested scopes, since we just
        // looked one up.
        for (String s : nested.scopes.keySet()) {
          if ((! s.equals(name)) && this.scopes.containsKey(s)) {
            return false;
          }
        }
      }

      if ((null != this.symbols) && (null != nested.symbols)) {
        for (String s : nested.symbols.keySet()) {
          if (this.symbols.containsKey(s)) {
            return false;
          }
        }
      }

      return true;
    }

    /**
     * Merge the nested scope with the specified unqualified name into
     * this scope.
     *
     * @param name The nested scope's unqualified name.
     * @throws IllegalArgumentException Signals that (1) this scope
     *   does not have a nested scope with the specified name, (2) any
     *   of the nested scope's children has the same name as one of
     *   this scope's children, or (3) any of the nested scope's
     *   bindings has the same name as one of this scope's bindings.
     */
    public void merge(String name) {
      final Scope nested = getNested(name);

      // Make sure the nested scope is mergeable.  Note that the
      // nested scope must exist in the consequence of the
      // if-statement, since isMergeable signals an exception for
      // non-existent scopes.
      if (! isMergeable(name)) {
        throw new IllegalArgumentException("Scope " + nested.qName +
                                           " cannot be merged into the parent");
      }

      // Remove the nested scope.
      this.scopes.remove(name);

      // Add the nested scope's children.
      if (null != nested.scopes) {
        this.scopes.putAll(nested.scopes);

        for (Scope s : nested.scopes.values()) {
          s.parent = this;
          s.requalify();
        }
      }

      // Add the nested scope's bindings.
      if (null != nested.symbols) {
        if (null == this.symbols) {
          this.symbols = nested.symbols;
        } else {
          this.symbols.putAll(nested.symbols);
        }
      }

      // Invalidate the nested scope.
      nested.parent  = null;
      nested.name    = null;
      nested.qName   = null;
      nested.scopes  = null;
      nested.symbols = null;
    }

    /**
     * Determine whether this scope has any local definitions.
     *
     * @return <code>true</code> if this scope has any local
     *   definitions.
     */
    public boolean hasSymbols() {
      return ((null != symbols) && (0 < symbols.size()));
    }

    /**
     * Get an iterator over the all locally defined symbols.
     *
     * @return An iterator over the locally defined symbols.
     */
    public Iterator<String> symbols() {
      if (null == symbols) {
        return EmptyIterator.value();
      } else {
        return symbols.keySet().iterator();
      }
    }

    /**
     * Determine whether the specified symbol is defined in this
     * scope.
     *
     * @param symbol The unqualified symbol.
     * @return <code>true</code> if the symbol is defined in this
     *   scope.
     */
    public boolean isDefinedLocally(String symbol) {
      return (null == symbols)? false : symbols.containsKey(symbol);
    }

    /**
     * Determine whether the specified unqualified symbol is defined
     * in this scope or any of its ancestors.
     *
     * @param symbol The unqualified symbol.
     * @return <code>true</code> if the symbol is defined in this scope
     *   or any of its ancestors.
     */
    public boolean isDefined(String symbol) {
      return (null != lookupScope(symbol));
    }

    /**
     * Determine whether the specified symbol is defined multiple
     * times in this scope or any of its ancestors.
     *
     * @param symbol The unqualified symbol.
     * @return <code>true</code> if the symbol is defined multiple
     *   times.
     */
    public boolean isDefinedMultiply(String symbol) {
      Scope scope = lookupScope(symbol);
      return (null == scope)? false : scope.symbols.get(symbol) instanceof List;
    }

    /**
     * Get the scope defining the specified unqualified symbol.  This
     * method searches this scope and all its ancestors, returning the
     * first defining scope.
     *
     * @param symbol The unqualified symbol.
     * @return The definining scope or <code>null</code> if there is
     *   no such scope.
     */
    public Scope lookupScope(String symbol) {
      Scope scope = this;
      do {
        if ((null != scope.symbols) && (scope.symbols.containsKey(symbol))) {
          return scope;
        }
        scope = scope.parent;
      } while (null != scope);
      return null;
    }

    /**
     * Get the value for the specified unqualified symbol.  This
     * method searches this scope and all its ancestors, returning the
     * value of the first definition.
     *
     * @param symbol The unqualified symbol.
     * @return The corresponding value or <code>null</code> if there is
     *   no definition.
     */
    public Object lookup(String symbol) {
      Scope scope = lookupScope(symbol);
      return (null == scope)? null : scope.symbols.get(symbol);
    }

    /**
     * Get the scope named by the specified unqualified symbol, which
     * is nested in the scope defining the symbol.  This method
     * searches this scope and all its ancestors, up to the first
     * defining scope.  It then looks for the nested scope with the
     * same name.
     *
     * @param symbol The unqualified symbol.
     * @return The bound scope or <code>null</code> if there is no
     *   definition or nested scope with the same name.
     */
    public Scope lookupBoundScope(String symbol) {
      Scope scope = lookupScope(symbol);
      return (null == scope)? null : scope.getNested(symbol);
    }

    /**
     * Get the local value for the specified unqualified symbol.
     *
     * @param symbol The unqualified symbol.
     * @return The corresponding value or <code>null</code> if there is
     *   no local definition.
     */
    public Object lookupLocally(String symbol) {
      return (null == symbols)? null : symbols.get(symbol);
    }

    /**
     * Set the specified symbol's value to the specified value in this
     * scope.
     *
     * @param symbol The unqualified symbol.
     * @param value The value.
     */
    public void define(String symbol, Object value) {
      if (null == symbols) {
        symbols = new HashMap<String, Object>();
      }
      symbols.put(symbol, value);
    }

    /**
     * Add the specified value to the specified symbol's values in
     * this scope.
     *
     * @param symbol The unqualified symbol.
     * @param value The value.
     */
    @SuppressWarnings("unchecked")
    public void addDefinition(String symbol, Object value) {
      if (null == symbols) {
        symbols = new HashMap<String, Object>();
      }

      if (symbols.containsKey(symbol)) {
        Object o = symbols.get(symbol);

        if (o instanceof List) {
          ((List<Object>)o).add(value);

        } else {
          List<Object> l = new ArrayList<Object>();
          l.add(o);
          l.add(value);
          symbols.put(symbol, l);
        }

      } else {
        symbols.put(symbol, value);
      }
    }

    /**
     * Undefine the specified unqualified symbol.  If the symbol is
     * defined in this scope, this method removes all its values.
     *
     * @param symbol The unqualified symbol.
     */
    public void undefine(String symbol) {
      if (null != symbols) {
        symbols.remove(symbol);
      }
    }

    /**
     * Qualify the specified unqualified symbol with this scope's
     * name.
     *
     * @param symbol The unqualified symbol.
     * @return The qualified symbol.
     */
    public String qualify(String symbol) {
      return Utilities.qualify(qName, symbol);
    }

    /**
     * Dump the contents of this scope.  This method pretty prints the
     * contents of this scope and all nested scopes with the specified
     * printer.  If the printer is registered with a visitor, that
     * visitor is used for formatting any node values.
     *
     * @param printer The printer, which need not be registered with a
     *   visitor.
     */
    public void dump(Printer printer) {
      boolean hasVisitor = (null != printer.visitor());

      printer.indent().p('.').p(name).pln(" = {").incr();

      if (null != symbols) {
        for (Map.Entry<String,Object> entry : symbols.entrySet()) {
          String symbol = entry.getKey();
          Object value  = entry.getValue();

          printer.indent().p(symbol).p(" = ");
          if (null == value) {
            printer.p("null");
          } else if (hasVisitor && (value instanceof Node)) {
            printer.p((Node)value);
          } else if (value instanceof String) {
            printer.p('"').escape((String)value, Utilities.JAVA_ESCAPES).p('"');
          } else {
            try {
              printer.p(value.toString());
            } catch (final Exception e) {
              printer.p(value.getClass().getName() + "@?");
            }
          }
          printer.pln(';');

          Scope nested = getNested(symbol);
          if (null != nested) {
            nested.dump(printer);
          }
        }
      }

      if (null != scopes) {
        for (Scope nested : scopes.values()) {
          if ((null == symbols) || (! symbols.containsKey(nested.name))) {
            nested.dump(printer);
          }
        }
      }

      printer.decr().indent().pln("};");
    }

  }

  // =========================================================================

  /** The root scope. */
  protected Scope root;

  /** The current scope. */
  protected Scope current;

  /** The fresh name count. */
  protected int freshNameCount;

  /** The fresh identifier count. */
  protected int freshIdCount;

  // =========================================================================

  /**
   * Create a new symbol table with the empty string as the root
   * scope's name.
   */
  public SymbolTable() {
    this("");
  }

  /**
   * Create a new symbol table.
   *
   * @param root The name of the root scope.
   */
  public SymbolTable(String root) {
    this.root      = new Scope(root);
    current        = this.root;
    freshNameCount = 0;
    freshIdCount   = 0;
  }

  // =========================================================================

  /**
   * Clear this symbol table.  This method deletes all scopes and
   * their definitions from this symbol table.
   */
  public void reset() {
    root.scopes    = null;
    root.symbols   = null;
    current        = root;
    freshNameCount = 0;
    freshIdCount   = 0;
  }

  /**
   * Get the root scope.
   *
   * @return The root scope.
   */
  public Scope root() {
    return root;
  }

  /**
   * Get the current scope.
   *
   * @return The current scope.
   */
  public Scope current() {
    return current;
  }

  /**
   * Get the scope with the specified qualified name.
   *
   * @param name The qualified name.
   * @return The corresponding scope or <code>null</code> if no such
   *   scope exits.
   */
  public Scope getScope(String name) {
    // Optimize for the common case where the specified name denotes a
    // scope directly nested in the current scope.
    Scope scope = current;
    if (name.startsWith(scope.qName) && 
        (name.lastIndexOf(Constants.QUALIFIER) == scope.qName.length())) {
      return scope.getNested(Utilities.getName(name));
    }

    String[] components = Utilities.toComponents(name);
    scope               = root.name.equals(components[0])? root : null;
    int      index      = 1;

    while ((null != scope) && (index < components.length)) {
      scope = scope.getNested(components[index]);
      index++;
    }

    return scope;
  }

  /**
   * Set the current scope to the specified scope.
   *
   * @param scope The new current scope.
   * @throws IllegalArgumentException Signals that this symbol table's
   *   root is not the specified scope's root.
   */
  public void setScope(Scope scope) {
    // Check the specified scope.
    Scope s = scope;
    while (null != s.parent) s = s.parent;
    if (s != root) {
      throw new IllegalArgumentException("Scope " + scope.qName + " not " +
                                         "in this symbol table " + this);
    }

    // Make the scope the current scope.
    current = scope;
  }

  /**
   * Determine whether the specified symbol is defined.  If the symbol
   * is qualified, this method checks whether the symbol is defined in
   * the named scope.  Otherwise, it checks whether the symbol is
   * defined in the current scope or one of its ancestors.
   *
   * @param symbol The symbol.
   * @return <code>true</code> if the specified symbol is defined.
   */
  public boolean isDefined(String symbol) {
    Scope scope = lookupScope(symbol);
    if ((null == scope) || (null == scope.symbols)) {
      return false;
    } else {
      return scope.symbols.containsKey(Utilities.unqualify(symbol));
    }
  }

  /**
   * Determine whether the specified symbol is define multiple times.
   * If the symbol is qualified, this method checks whether the symbol
   * has multiple definitions in the named scope.  Otherwise, it
   * checks whether the symbol has multiple definitions in the current
   * scope or one of its ancestors.
   *
   * @param symbol The symbol.
   * @return <code>true</code> if the specified symbol is multiply
   *   defined.
   */
  public boolean isDefinedMultiply(String symbol) {
    Scope scope = lookupScope(symbol);
    if ((null == scope) || (null == scope.symbols)) {
      return false;
    } else {
      return scope.symbols.get(Utilities.unqualify(symbol)) instanceof List;
    }
  }

  /**
   * Get the scope for the specified symbol.  If the symbol is
   * qualified, this method returns the named scope (without checking
   * whether the symbol is defined in that scope).  Otherwise, it
   * searches the current scope and all its ancestors, returning the
   * first defining scope.
   *
   * @param symbol The symbol.
   * @return The corresponding scope or <code>null</code> if no such
   *   scope exits.
   */
  public Scope lookupScope(String symbol) {
    if (Utilities.isQualified(symbol)) {
      return getScope(Utilities.getQualifier(symbol));

    } else {
      return current.lookupScope(symbol);
    }
  }

  /**
   * Get the value for the specified symbol.  If the symbol is
   * qualified, this method returns the definition within the named
   * scope.  Otherwise, it searches the current scope and all its
   * ancestors, returning the value of the first definition.
   *
   * @param symbol The symbol.
   * @return The corresponding value or <code>null</code> if no such
   *   definition exists.
   */
  public Object lookup(String symbol) {
    Scope scope = lookupScope(symbol);
    if ((null == scope) || (null == scope.symbols)) {
      return null;
    } else {
      return scope.symbols.get(Utilities.unqualify(symbol));
    }
  }

  /**
   * Enter the scope with the specified unqualified name.  If the
   * current scope does not have a scope with the specified name, a
   * new scope with the specified name is created.  In either case,
   * the scope with that name becomes the current scope.
   *
   * @param name The unqualified name.
   */
  public void enter(String name) {
    Scope parent = current;
    Scope child  = parent.getNested(name);
    if (null == child) {
      child      = new Scope(name, parent);
    }
    current = child;
  }

  /**
   * Exit the current scope.
   *
   * @throws IllegalStateException
   *   Signals that the current scope is the root scope.
   */
  public void exit() {
    if (null == current.parent) {
      throw new IllegalStateException("Unable to exit root scope");
    }
    current = current.parent;
  }

  /**
   * Delete the scope with the specified unqualified name.  If the
   * current scope contains a nested scope with the specified name,
   * this method deletes that scope and <em>all its contents</em>,
   * including nested scopes.
   *
   * @param name The unqualified name.
   */
  public void delete(String name) {
    if (null != current.scopes) {
      current.scopes.remove(name);
    }
  }

  /**
   * Determine whether the specified node has an associated {@link
   * Constants#SCOPE scope}.
   *
   * @param n The node.
   * @return <code>true</code> if the node has an associated scope.
   */
  public static boolean hasScope(Node n) {
    return n.hasProperty(Constants.SCOPE);
  }
  
  /**
   * Mark the specified node.  If the node does not have an associated
   * {@link Constants#SCOPE scope}, this method set the property with
   * the current scope's qualified name.
   *
   * @param n The node.
   */
  public void mark(Node n) {
    if (! n.hasProperty(Constants.SCOPE)) {
      n.setProperty(Constants.SCOPE, current.getQualifiedName());
    }
  }

  /**
   * Enter the specified node.  If the node has an associated {@link
   * Constants#SCOPE scope}, this method tries to enter the scope.
   * Otherwise, it does not change the scope.
   *
   * @param n The node.
   * @throws IllegalStateException Signals that the node's scope is
   *   invalid or not nested within the current scope.
   */
  public void enter(Node n) {
    if (n.hasProperty(Constants.SCOPE)) {
      String name  = n.getStringProperty(Constants.SCOPE);
      Scope  scope = getScope(name);

      if (null == scope) {
        throw new IllegalStateException("Invalid scope " + name);
      } else if (scope.getParent() != current) {
        throw new IllegalStateException("Scope " + name + " not nested in " +
                                        current.getQualifiedName());
      }

      current = scope;
    }
  }

  /**
   * Exit the specified node.  If the node has an associated {@link
   * Constants#SCOPE scope}, the current scope is exited.
   *
   * @param n The node.
   */
  public void exit(Node n) {
    if (n.hasProperty(Constants.SCOPE)) {
      exit();
    }
  }

  /**
   * Create a fresh name.  The returned name has
   * "<code>anonymous</code>" as it base name.
   *
   * @see #freshName(String)
   * 
   * @return A fresh name.
   */
  public String freshName() {
    return freshName("anonymous");
  }

  /**
   * Create a fresh name incorporating the specified base name.  The
   * returned name is of the form
   * <code><i>name</i>(<i>count</i>)</code>.
   *
   * @param base The base name.
   * @return The corresponding fresh name.
   */
  public String freshName(String base) {
    StringBuilder buf = new StringBuilder();
    buf.append(base);
    buf.append(Constants.START_OPAQUE);
    buf.append(freshNameCount++);
    buf.append(Constants.END_OPAQUE);
    return buf.toString();
  }

  /**
   * Create a fresh C identifier.  The returned identifier has
   * "<code>tmp</code>" as its base name.
   *
   * @see #freshCId(String)
   *
   * @return A fresh C identifier.
   */
  public String freshCId() {
    return freshCId("tmp");
  }

  /**
   * Create a fresh C identifier incorporating the specified base
   * name.  The returned name is of the form
   * <code>__<i>name</i>_<i>count</i></code>.
   *
   * @param base The base name.
   * @return The corresponding fresh C identifier.
   */
  public String freshCId(String base) {
    StringBuilder buf = new StringBuilder();
    buf.append("__");
    buf.append(base);
    buf.append('_');
    buf.append(freshIdCount++);
    return buf.toString();
  }

  /**
   * Create a fresh Java identifier.  The returned identifier has
   * "<code>tmp</code>" as its base name.
   *
   * @see #freshJavaId(String)
   *
   * @return A fresh Java identifier.
   */
  public String freshJavaId() {
    return freshJavaId("tmp");
  }

  /**
   * Create a fresh Java identifier incorporating the specified base
   * name.  The returned name is of the form
   * <code><i>name</i>$<i>count</i></code>.
   *
   * @param base The base name.
   * @return The corresponding fresh Java identifier.
   */
  public String freshJavaId(String base) {
    StringBuilder buf = new StringBuilder();
    buf.append(base);
    buf.append('$');
    buf.append(freshIdCount++);
    return buf.toString();
  }

  /**
   * Convert the specified unqualified symbol to a symbol in the
   * specified name space.
   *
   * @param symbol The symbol
   * @param space The name space.
   * @return The mangled symbol.
   */
  public static String toNameSpace(String symbol, String space) {
    return space + Constants.START_OPAQUE + symbol + Constants.END_OPAQUE;
  }

  /**
   * Determine whether the specified symbol is in the specified name
   * space.
   *
   * @param symbol The symbol.
   * @param space The name space.
   * @return <code>true</code> if the symbol is mangled symbol in the
   *   name space.
   */
  public static boolean isInNameSpace(String symbol, String space) {
    try {
      return (symbol.startsWith(space) &&
              (Constants.START_OPAQUE == symbol.charAt(space.length())) &&
              symbol.endsWith(END_OPAQUE));
    } catch (IndexOutOfBoundsException x) {
      return false;
    }
  }

  /** The end of opaqueness marker as a string. */
  private static final String END_OPAQUE =
    Character.toString(Constants.END_OPAQUE);

  /**
   * Convert the specified unqualified symbol within a name space to a
   * symbol without a name space.
   *
   * @param symbol The mangled symbol within a name space.
   * @return The corresponding symbol without a name space.
   */
  public static String fromNameSpace(String symbol) {
    int start = symbol.indexOf(Constants.START_OPAQUE);
    int end   = symbol.length() - 1;
    if ((0 < start) && (Constants.END_OPAQUE == symbol.charAt(end))) {
      return symbol.substring(start + 1, end);
    } else {
      throw new IllegalArgumentException("Not a mangled symbol '"+symbol+"'");
    }
  }

  /**
   * Conver the specified C macro identifier into a symbol table scope
   * name.
   *
   * @param id The macro identifier.
   * @return The corresponding symbol table scope name.
   */
  public static String toMacroScopeName(String id) {
    return toNameSpace(id, "macro");
  }

  /**
   * Determine whether the specified scope name represents a macro's
   * scope.
   *
   * @param name The name.
   * @return <code>true</code> if the name denotes a macro scope.
   */
  public static boolean isMacroScopeName(String name) {
    return isInNameSpace(name, "macro");
  }

  /**
   * Convert the specified C function identifier into a symbol table
   * scope name.
   *
   * @param id The function identifier.
   * @return The corresponding symbol table scope name.
   */
  public static String toFunctionScopeName(String id) {
    return toNameSpace(id, "function");
  }

  /**
   * Determine whether the specified scope name represents a
   * function's scope.
   *
   * @param name The name.
   * @return <code>true</code> if the name denotes a function scope.
   */
  public static boolean isFunctionScopeName(String name) {
    return isInNameSpace(name, "function");
  }

  /**
   * Convert the specified C struct, union, or enum tag into a symbol
   * table name.
   *
   * @param tag The tag.
   * @return The corresponding symbol table name.
   */
  public static String toTagName(String tag) {
    return toNameSpace(tag, "tag");
  }

  /**
   * Convert the specified label identifier into a symbol table name.
   *
   * @param id The identifier.
   * @return The corresponding symbol table name.
   */
  public static String toLabelName(String id) {
    return toNameSpace(id, "label");
  }

  /**
   * Convert the specified method identifier into a symbol table name.
   *
   * @param id The method identifier.
   * @return The corresponding symbol table name.
   */
  public static String toMethodName(String id) {
    return toNameSpace(id, "method");
  }

}
