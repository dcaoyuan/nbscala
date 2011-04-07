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
package xtc.type;

import java.util.ArrayList;

import xtc.Constants;

import xtc.util.SymbolTable;

/**
 * A factory for programmatically and concisely creating C types.
 *
 * @author Robert Grimm
 * @version $Revision: 1.1 $
 */
public class CFactory {

  /** A constant void. */
  private static final Type CONST_VOID;

  /** A constant char. */
  private static final Type CONST_CHAR;

  /** A pointer to char. */
  private static final Type PTR_2_CHAR;

  static {
    CONST_VOID =
      VoidT.TYPE.annotate().attribute(Constants.ATT_CONSTANT).seal();
    CONST_CHAR =
      NumberT.CHAR.annotate().attribute(Constants.ATT_CONSTANT).seal();
    PTR_2_CHAR =
      new PointerT(NumberT.CHAR).seal();
  }

  // =========================================================================

  /** The prefix for declarations. */
  private String prefix;

  /** The symbol table scope. */
  private SymbolTable.Scope scope;

  /**
   * Create a new C factory.
   *
   * @param prefix The prefix for declarations.
   * @param scope The symbol table scope.
   */
  public CFactory(String prefix, SymbolTable.Scope scope) {
    this.prefix = prefix;
    this.scope  = scope;
  }

  // =========================================================================

  /**
   * Get the canonical void type.
   *
   * @return <code>VoidT.TYPE</code>.
   */
  public Type v() {
    return VoidT.TYPE;
  }

  /**
   * Get a constant void type.
   *
   * @return A sealed constant void type.
   */
  public Type vc() {
    return CONST_VOID;
  }

  /**
   * Get the canonical char type.
   *
   * @return <code>NumberT.CHAR</code>.
   */
  public Type c() {
    return NumberT.CHAR;
  }

  /**
   * Get a constant char type.
   *
   * @return A sealed constant char type.
   */
  public Type cc() {
    return CONST_CHAR;
  }

  /**
   * Get the canonical int type.
   *
   * @return <code>NumberT.INT</code>.
   */
  public Type i() {
    return NumberT.INT;
  }

  /**
   * Get the canonical sizeof type.
   *
   * @return <code>NumberT.SIZEOF</code>.
   */
  public Type size() {
    return C.SIZEOF;
  }

  /**
   * Create a new pointer type.
   *
   * @param type The pointed-to type.
   * @return The new pointer type.
   */
  public PointerT p(Type type) {
    return new PointerT(type);
  }

  /**
   * Create a new restricted pointer type.
   *
   * @param type The pointed-to type.
   * @return The new restricted pointer type.
   */
  public Type pr(Type type) {
    return new PointerT(type).annotate().attribute(Constants.ATT_RESTRICT);
  }

  /**
   * Get a pointer to void.
   *
   * @return A sealed pointer to void.
   */
  public Type p2v() {
    return PointerT.TO_VOID;
  }

  /**
   * Get a pointer to char.
   *
   * @return A sealed pointer to char.
   */
  public Type p2c() {
    return PTR_2_CHAR;
  }

  /**
   * Create a new function type with no parameters and a void return
   * type.
   *
   * @return The new function type.
   */
  public Type f() {
    return f(v());
  }

  /**
   * Create a new function type with no parameters.
   *
   * @param result The result type.
   * @return The new function type.
   */
  public Type f(Type result) {
    return new FunctionT(result, new ArrayList<Type>(0), false).
      attribute(Constants.ATT_STYLE_NEW);
  }

  /**
   * Create a new function type.
   *
   * @param result The result type.
   * @param param The parameter type.
   * @return The new function type.
   */
  public Type f(Type result, Type param) {
    ArrayList<Type> params = new ArrayList<Type>(1);
    params.add(param);
    FunctionT function = new FunctionT(result, params, false);
    function.addAttribute(Constants.ATT_STYLE_NEW);
    return function;
  }

  /**
   * Create a new function type.
   *
   * @param result The result type.
   * @param param1 The first parameter type.
   * @param param2 The second parameter type.
   * @return The new function type.
   */
  public Type f(Type result, Type param1, Type param2) {
    ArrayList<Type> params = new ArrayList<Type>(2);
    params.add(param1);
    params.add(param2);
    FunctionT function = new FunctionT(result, params, false);
    function.addAttribute(Constants.ATT_STYLE_NEW);
    return function;
  }

  /**
   * Create a new function type.
   *
   * @param result The result type.
   * @param param1 The first parameter type.
   * @param param2 The second parameter type.
   * @param param3 The third parameter type.
   * @return The new function type.
   */
  public Type f(Type result, Type param1, Type param2, Type param3) {
    ArrayList<Type> params = new ArrayList<Type>(3);
    params.add(param1);
    params.add(param2);
    params.add(param3);
    FunctionT function = new FunctionT(result, params, false);
    function.addAttribute(Constants.ATT_STYLE_NEW);
    return function;
  }

  // =========================================================================

  /**
   * Mark the specified type as constant.  Note that this method
   * directly modifies the specified type.
   *
   * @param type The type.
   * @return The constant type.
   */
  public Type constant(Type type) {
    return type.attribute(Constants.ATT_CONSTANT);
  }

  /**
   * Mark the specified type as builtin.  Note that this method
   * directly modifies the specified type.
   *
   * @param type The type.
   * @return The builtin type.
   */
  public Type builtin(Type type) {
    return type.attribute(Constants.ATT_BUILTIN);
  }

  // =========================================================================

  /**
   * Add the specified declaration to this factory's symbol table
   * scope.  The specified name is prefixed with this factory's
   * prefix.  The specified type is marked as a built-in type.  If the
   * <code>nofix</code> flag is set, this method also adds a
   * declaration without this factory's prefix.
   * 
   * @param nofix The nofix flag.
   * @param name The name.
   * @param type The type.
   * @return This factory.
   */
  public CFactory decl(boolean nofix, String name, Type type) {
    if (nofix) {
      scope.define(name, builtin(type));
    }
    scope.define(prefix + name, builtin(type));
    return this;
  }

  // =========================================================================

  /**
   * Declare built-in functions for C in this factory's symbol table
   * scope.
   *
   * @param nofix The flag for whether to also declare symbols without
   *   this factory's prefix.
   */
  public void declareBuiltIns(boolean nofix) {

    // C99 7.20.3
    decl(nofix, "calloc",       f(p2v(), size(), size()));
    decl(nofix, "free",         f(v(), p2v()));
    decl(nofix, "malloc",       f(p2v(), size()));
    decl(nofix, "realloc",      f(p2v(), p2v(), size()));

    // C99 7.20.4
    decl(nofix, "abort",        f());
    decl(nofix, "atexit",       f(i(), p(f())));
    decl(nofix, "exit",         f(v(), i()));
    decl(nofix, "_Exit",        f(v(), i()));
    decl(nofix, "getenv",       f(p2c(), p(cc())));
    decl(nofix, "system",       f(i(), p(cc())));

    // C99 7.21.2
    decl(nofix, "memcpy",       f(p2v(), pr(v()), pr(vc()), size()));

    // C99 7.21.6
    decl(nofix, "memset",       f(p2v(), p2v(), i(), size()));

    // GCC extension
    decl(nofix, "stpcpy",       f(p2c(), p2c(), p(cc())));

  }

}
