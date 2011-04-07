/*
 * xtc - The eXTensible Compiler
 * Copyright (C) 2007-2008 Robert Grimm
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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import xtc.Constants;
import xtc.Constants.FuzzyBoolean;

import xtc.tree.Printer;

import xtc.util.Utilities;

/**
 * Common type operations for <em>Rats!</em> ASTs.
 * 
 * <p />This class supports two views on a grammar's generic AST.  The
 * first view is dynamically typed, with all generic AST nodes
 * represented by the canonical node type.  The second view is
 * statically typed, with all generic AST nodes represented by tuples
 * organized into variants.  Either way, this class supports the
 * following types:<ul>
 *
 * <li>The void type represented by {@link VoidT}.</li>
 *
 * <li>The unit type represented by {@link UnitT}.</li>
 *
 * <li>Characters represented by an {@link InternalT} with name
 * "char".</li>
 *
 * <li>Strings represented by an {@link InternalT} with name
 * "string".</li>
 *
 * <li>Tokens represented by an {@link InternalT} with name
 * "token".</li>
 *
 * <li>Dynamically typed nodes represented by an {@link InternalT}
 * with name "node".</li>
 *
 * <li>Statically typed nodes represented by a {@link VariantT} with
 * {@link TupleT} elements.</li>
 *
 * <li>Parse tree annotations represented by an {@link InternalT}
 * with name "formatting".</li>
 *
 * <li>Lists represented by an {@link InternalT} with name
 * "list".</li>
 *
 * <li>Actions represented by an {@link InternalT} with name
 * "action".</li>
 *
 * </ul>
 *
 * All node types have the {@link Constants#ATT_NODE} attribute; the
 * dynamically typed node representing generic productions also has
 * the {@link Constants#ATT_GENERIC} attribute.
 *
 * <p />In addition to generic ASTs, this class also supports
 * user-defined types, which must be represented by types that are not
 * listed above.
 *
 * <p />Concrete subclasses specify the mapping between strings and
 * types.  For mapping internal representations back to strings, the
 * void type has name "void", the unit type has name "unit", and the
 * wildcard has name "?".
 *
 * @author Robert Grimm
 * @version $Revision: 1.39 $
 */
public abstract class AST {

  /** The set of internal type names. */
  public static final Set<String> INTERNAL;

  /** The canonical void type, which is {@link VoidT#TYPE}. */
  public static final Type VOID = VoidT.TYPE;

  /** The canonical any type. */
  public static final Type ANY;

  /** The canonical character reference type. */
  public static final Type CHAR;

  /** The canonical string type. */
  public static final Type STRING;

  /** The canonical token type. */
  public static final Type TOKEN;

  /** The canonical dynamically typed node type. */
  public static final Type NODE;

  /** The canonical null node type. */
  public static final Type NULL_NODE;

  /** The canonical dynamically typed generic node type. */
  public static final Type GENERIC;

  /** The canonical formatting node type. */
  public static final Type FORMATTING;

  /** The canonical parameterized list type. */
  public static final Type LIST;

  /** The canonical list instantiated with a wildcard element type. */
  public static final Type WILD_LIST;

  /** The canonical parameterized action type. */
  public static final Type ACTION;

  /** The canonical action instantiated with a wildcard element type. */
  public static final Type WILD_ACTION;

  static {
    // Create the set of internal type names.
    Set<String> internal = new HashSet<String>();

    internal.add("any");
    internal.add("char");
    internal.add("string");
    internal.add("token");
    internal.add("node");
    internal.add("formatting");
    internal.add("list");
    internal.add("action");
    
    INTERNAL = Collections.unmodifiableSet(internal);

    // Create the canonical types.
    ANY           = new InternalT("any");
    CHAR          = new InternalT("char");
    STRING        = new InternalT("string");
    TOKEN         = new InternalT("token");
    TOKEN.addAttribute(Constants.ATT_NODE);
    NODE          = new InternalT("node");
    NODE.addAttribute(Constants.ATT_NODE);
    NULL_NODE     = new UnitT();
    NULL_NODE.addAttribute(Constants.ATT_NODE);
    GENERIC       = new InternalT("node");
    GENERIC.addAttribute(Constants.ATT_NODE);
    GENERIC.addAttribute(Constants.ATT_GENERIC);
    FORMATTING    = new InternalT("formatting");
    FORMATTING.addAttribute(Constants.ATT_NODE);
    LIST          = new ParameterizedT(new NamedParameter("element"),
                                       new InternalT("list"));
    WILD_LIST     = new InstantiatedT(Wildcard.TYPE, LIST);
    ACTION        = new ParameterizedT(new NamedParameter("element"),
                                       new InternalT("action"));
    WILD_ACTION   = new InstantiatedT(Wildcard.TYPE, ACTION);

    // Seal the canincal types.
    ANY.seal();
    CHAR.seal();
    STRING.seal();
    TOKEN.seal();
    NULL_NODE.seal();
    NODE.seal();
    GENERIC.seal();
    FORMATTING.seal();
    LIST.seal();
    WILD_LIST.seal();
    ACTION.seal();
    WILD_ACTION.seal();
  }

  // ==========================================================================

  /** The map from strings to type representations. */
  protected final Map<String,Type> externToIntern;

  /**
   * The map from internal type names to external types.  Valid type
   * names are void, unit, any, char, string, token, node, formatting,
   * list, and action as well as ? for wildcards.
   */
  protected final Map<String,String> internToExtern;

  /**
   * The list of imported module names.  Each module name should end
   * with the separator necessary for creating a fully qualified type
   * name by appending a simple name.
   */
  protected final List<String> importedModules;

  /** The map from simple type names to fully qualified type names. */
  protected final Map<String, String> importedTypes;

  /** The map from variant names to variant types. */
  protected final Map<String, VariantT> variants;

  /** The map from variant names to nodes. */
  protected final Map<String, Set<String>> variantNodes;

  /** The map from unqualified variant names to original names. */
  protected final Map<String, String> originalNames;

  /** The map from tuple names to tuple types. */
  protected final Map<String, TupleT> tuples;

  /** The map from tuple names to variants containing the tuples. */
  protected final Map<String, List<VariantT>> tupleVariants;

  /**
   * Create a new instance.  This constructor allocates the internal
   * data structures for mapping between external and internal types
   * but does not initialize them.
   *
   * @see #initialize(boolean,boolean,boolean,boolean)
   */
  public AST() {
    externToIntern  = new HashMap<String,Type>();
    internToExtern  = new HashMap<String,String>();
    importedModules = new ArrayList<String>();
    importedTypes   = new HashMap<String,String>();
    variants        = new HashMap<String, VariantT>();
    variantNodes    = new HashMap<String, Set<String>>();
    originalNames   = new HashMap<String, String>();
    tuples          = new HashMap<String, TupleT>();
    tupleVariants   = new HashMap<String, List<VariantT>>();
  }

  /**
   * Initialize the mapping between external and internal
   * representations.  This method fills the {@link #externToIntern}
   * and {@link #internToExtern} data structures.
   *
   * @param hasNode Flag to indicate use of built-in nodes.
   * @param hasToken Flag to indicate use of tokens.
   * @param hasFormatting Flag to indicate use of formatting.
   * @param hasAction Flag to indicate use of actions.
   */
  public abstract void initialize(boolean hasNode, boolean hasToken, 
                                  boolean hasFormatting, boolean hasAction);

  // ==========================================================================

  /**
   * Import the specified module.  This method adds the module to the
   * list of imported modules {@link #importedModules}.  The specified
   * module name must end with the appropriate separator.
   *
   * @param module The module name.
   */
  public void importModule(String module) {
    if (! importedModules.contains(module)) {
      importedModules.add(module);
    }
  }

  /**
   * Import the specified type.  This method adds a mapping from the
   * specified simple type name to the specified qualified type name
   * to the imported types {@link #importedTypes}.
   *
   * @param qualified The fully qualified name.
   * @param simple The simple name.
   */
  public void importType(String qualified, String simple) {
    if (importedTypes.containsKey(simple)) {
      assert qualified.equals(importedTypes.get(simple));
    } else {
      importedTypes.put(simple, qualified);
    }
  }

  // ==========================================================================

  /**
   * Determine whether the specified string represents the void type.
   *
   * @param s The type as a string.
   * @return <code>true</code> if the string represents the void type.
   */
  public abstract boolean isVoid(String s);

  /**
   * Determine whether the specified string represents the generic
   * node type.
   *
   * @param s The type as a string.
   * @return <code>true</code> if the string represents the generic
   *   node type.
   */
  public abstract boolean isGenericNode(String s);

  // ==========================================================================

  /**
   * Convert the specified string representation of a type into the
   * type.  This method defers to {@link #internList(String)}, {@link
   * #internAction(String)}, and {@link #internUser(String)} for list,
   * action, and user-defined types, respectively.
   *
   * @param s The type as a string.
   * @return The type.
   * @throws IllegalArgumentException Signals that the string
   *   representation is not a valid type.
   */
  public Type intern(String s) {
    // Try the map from strings to types.
    Type type = externToIntern.get(s);
    if (null != type) return type;

    // Try as a list.
    type = internList(s);
    if (! type.isError()) return type;

    // Try as an action.
    type = internAction(s);
    if (! type.isError()) return type;

    // Treat as user-defined.
    return internUser(s);
  }

  /**
   * Convert the specified string representation of a list type into
   * the type.
   *
   * @param s The list type as a string.
   * @return The type or {@link ErrorT#TYPE} if the string does not
   *   represent a list.
   */
  protected abstract Type internList(String s);

  /**
   * Convert the specified string representation of an action type
   * into the type.
   *
   * @param s The action type as a string.
   * @return The type or {@link ErrorT#TYPE} if the string does not
   *   represent an action.
   */
  protected abstract Type internAction(String s);

  /**
   * Convert the specified string representation of a user-defined
   * type into its internal representation.
   *
   * @param s The user-defined type as a string.
   * @return The type.
   * @throws IllegalArgumentException Signals that the string
   *   representation is not a valid type.
   */
  protected abstract Type internUser(String s);

  // ==========================================================================

  /**
   * Convert the specified type to a string.  This method defers to
   * {@link #externList(Type)}, {@link #externAction(Type)}, and
   * {@link #externUser(Type)} for list, action, and user-defined
   * types, respectively.
   *
   * @param type The type.
   * @return The type as a string.
   */
  public String extern(Type type) {
    String s;

    switch (type.tag()) {
    case VARIANT:
    case TUPLE:
      if (type.hasAttribute(Constants.ATT_NODE)) {
        return internToExtern.get("node");
      } else {
        return externUser(type);
      }

    case INTERNAL: {
      String name = type.resolve().toInternal().getName();
      
      if ("list".equals(name)) {
        return externList(type);
      } else if ("action".equals(name)) {
        return externAction(type);
      } else if (INTERNAL.contains(name)) {
        return internToExtern.get(name);
      } else {
        return externUser(type);
      }
    }

    case UNIT:
      return type.hasAttribute(Constants.ATT_NODE) ?
        internToExtern.get("node") : internToExtern.get("unit");

    case VOID:
      return internToExtern.get("void");

    case WILDCARD:
      return internToExtern.get(type.resolve().toWildcard().getName());

    case ERROR:
      throw new AssertionError("Error type");

    default:
      return externUser(type);
    }
  }

  /**
   * Convert the specified list type to a string.
   *
   * @param type The list type.
   * @return The type as a string.
   */
  protected abstract String externList(Type type);

  /**
   * Convert the specified action type to a string.
   *
   * @param type The action type.
   * @return The type as a string.
   */
  protected abstract String externAction(Type type);

  /**
   * Convert the specified user-defined type to a string.
   *
   * @param type The user-defined type.
   * @return The type as a string.
   */
  protected abstract String externUser(Type type);

  // ==========================================================================

  /**
   * Determine whether instances of the specified type have a source
   * location.  This method defers to {@link #hasLocationUser(Type)}
   * for user-defined types.
   *
   * @param type The type.
   * @return The inexact answer.
   */
  public FuzzyBoolean hasLocation(Type type) {
    switch (type.tag()) {
    case VARIANT:
    case TUPLE:
      if (type.hasAttribute(Constants.ATT_NODE)) {
        return FuzzyBoolean.TRUE;
      } else {
        return hasLocationUser(type);
      }

    case INTERNAL:
      if (type.hasAttribute(Constants.ATT_NODE)) {
        return FuzzyBoolean.TRUE;
      } else {
        String name = type.resolve().toInternal().getName();

        if ("any".equals(name)) {
          return FuzzyBoolean.MAYBE;
        } else if (INTERNAL.contains(name)) {
          return FuzzyBoolean.FALSE;
        } else {
          return hasLocationUser(type);
        }
      }

    case UNIT:
      return type.hasAttribute(Constants.ATT_NODE) ?
        FuzzyBoolean.TRUE : FuzzyBoolean.FALSE;

    case VOID:
      return FuzzyBoolean.FALSE;

    case NAMED_PARAMETER:
    case INTERNAL_PARAMETER:
    case WILDCARD:
      return FuzzyBoolean.MAYBE;

    case ERROR:
      throw new AssertionError("Error type");

    default:
      return hasLocationUser(type);
    }
  }

  /**
   * Determine whether instances of the specified user-defined type
   * have a source location.
   *
   * @param type The type.
   * @return The inexact answer.
   */
  protected abstract FuzzyBoolean hasLocationUser(Type type);

  // ==========================================================================

  /**
   * Determine whether the specified type is optional.
   *
   * @param type The type.
   * @return <code>true</code> if the specified type is optional.
   */
  public static boolean isOptional(Type type) {
    return type.hasAttribute(Constants.ATT_OPTIONAL);
  }

  /**
   * Determine whether the specified type is variable.
   *
   * @param type The type.
   * @return <code>true</code> if the specified type is variable.
   */
  public static boolean isVariable(Type type) {
    return type.hasAttribute(Constants.ATT_VARIABLE);
  }

  // ==========================================================================

  /**
   * Determine whether the specified type is the void type.
   *
   * @param type The type.
   * @return <code>true</code> if the type is the void type.
   */
  public static boolean isVoid(Type type) {
    return type.resolve().isVoid();
  }

  /**
   * Determine whether the specified type is the any type.
   *
   * @param type The type.
   * @return <code>true</code> if the type is the any type.
   */
  public static boolean isAny(Type type) {
    Type r = type.resolve();
    return r.isInternal() && "any".equals(r.toInternal().getName());
  }

  /**
   * Determine whether the specified type is a character.
   *
   * @param type The type.
   * @return <code>true</code> if the type is a character.
   */
  public static boolean isChar(Type type) {
    Type r = type.resolve();
    return r.isInternal() && "char".equals(r.toInternal().getName());
  }

  /**
   * Determine whether the specified type is a string.
   *
   * @param type The type.
   * @return <code>true</code> if the type is a string.
   */
  public static boolean isString(Type type) {
    Type r = type.resolve();
    return r.isInternal() && "string".equals(r.toInternal().getName());
  }

  /**
   * Determine whether the specified type is a token.
   *
   * @param type The type.
   * @return <code>true</code> if the type is a token.
   */
  public static boolean isToken(Type type) {
    Type r = type.resolve();
    return r.isInternal() && "token".equals(r.toInternal().getName());
  }

  /**
   * Determine whether the specified type is a node.
   *
   * @param type The type.
   * @return <code>true</code> if the type is a node.
   */
  public static boolean isNode(Type type) {
    return type.hasAttribute(Constants.ATT_NODE);
  }

  /**
   * Determine whether the specified type is a dynamically typed node.
   *
   * @param type The type.
   * @return <code>true</code> if the type is a dynamically typed
   *   node.
   */
  public static boolean isDynamicNode(Type type) {
    Type r = type.resolve();
    return r.isInternal() && "node".equals(r.toInternal().getName());
  }

  /**
   * Determine whether the specified type is a null node.
   *
   * @param type The type.
   * @return <code>true</code> if the type is a null node.
   */
  public static boolean isNullNode(Type type) {
    return type.hasAttribute(Constants.ATT_NODE) && type.resolve().isUnit();
  }

  /**
   * Determine whether the specified type is a statically typed node.
   *
   * @param type The type.
   * @return <code>true</code> if the type is a statically typed node.
   */
  public static boolean isStaticNode(Type type) {
    return type.hasAttribute(Constants.ATT_NODE) && type.resolve().isVariant();
  }

  /**
   * Determine whether the specified type is a generic node.
   *
   * @param type The type.
   * @return <code>true</code> if the type is a generic node.
   */
  public static boolean isGenericNode(Type type) {
    return (type.hasAttribute(Constants.ATT_GENERIC) &&
            type.hasAttribute(Constants.ATT_NODE));
  }

  /**
   * Determine whether the specified type is a formatting node.
   *
   * @param type The type.
   * @return <code>true</code> if the type is a formatting node.
   */
  public static boolean isFormatting(Type type) {
    Type r = type.resolve();
    return r.isInternal() && "formatting".equals(r.toInternal().getName());
  }

  /**
   * Determine whether the specified type is a list.
   *
   * @param type The type.
   * @return <code>true</code> if the type is a list.
   */
  public static boolean isList(Type type) {
    Type r = type.resolve();
    return r.isInternal() && "list".equals(r.toInternal().getName());
  }

  /**
   * Determine whether the specified type is an action.
   *
   * @param type The type.
   * @return <code>true</code> if the type is an action.
   */
  public static boolean isAction(Type type) {
    Type r = type.resolve();
    return r.isInternal() && "action".equals(r.toInternal().getName());
  }

  /**
   * Get the specified instantiated type's only argument.
   *
   * @param type The instantiated type.
   * @return The argument type.
   */
  public static Type getArgument(Type type) {
    assert type.hasInstantiated();
    assert 1 == type.toInstantiated().getArguments().size();
    return type.toInstantiated().getArguments().get(0);
  }

  /**
   * Determine whether the specified type is user-defined.
   *
   * @param type The type.
   * @return <code>true</code> if the type is a user-defined type.
   */
  public static boolean isUser(Type type) {
    switch (type.tag()) {
    case VARIANT:
    case TUPLE:
      return ! type.hasAttribute(Constants.ATT_NODE);

    case INTERNAL:
      return ! INTERNAL.contains(type.resolve().toInternal().getName());

    case UNIT:
    case VOID:
    case ERROR:
    case NAMED_PARAMETER:
    case INTERNAL_PARAMETER:
    case WILDCARD:
      return false;

    default:
      return true;
    }
  }

  // ==========================================================================

  /**
   * Mark the specified type as optional.
   *
   * @param type The type.
   * @return The optional type.
   */
  public static Type markOptional(Type type) {
    return type.annotate().attribute(Constants.ATT_OPTIONAL);
  }

  /**
   * Mark the specified type as variable.
   *
   * @param type The type.
   * @return The variable type.
   */
  public static Type markVariable(Type type) {
    return type.annotate().attribute(Constants.ATT_VARIABLE);
  }

  // ==========================================================================

  /**
   * Convert the specified production's name into a variant name.
   * This method converts the name from <em>Rats!</em>' camel case
   * naming convention into ML's lower case with underscores naming
   * convention.  It preserves any qualifier.
   *
   * @param name The production's name.
   * @return The corresponding variant name.
   */
  public String toVariantName(String name) {
    if (Utilities.isQualified(name)) {
      final String qualifier = Utilities.getQualifier(name);
      final String unqual    = Utilities.getName(name);
      return Utilities.qualify(qualifier, Utilities.split(unqual, '_'));
    } else {
      return Utilities.split(name, '_');
    }
  }

  /**
   * Determine whether a variant type with the specified name has been
   * created before.  This method first converts the name from
   * <em>Rats!</em>' camel case naming convention into ML's lower case
   * with underscores naming convention.  It then checks whether a
   * variant type with that name has been returned by {@link
   * #toVariant(String,boolean)} before.
   *
   * @see #toVariantName(String)
   *
   * @param name The name in camel case.
   * @return <code>true</code> if a variant type with the name exists.
   */
  public boolean hasVariant(String name) {
    return variants.containsKey(toVariantName(name));
  }

  /**
   * Get the variant type with the specified name.  This method first
   * converts the name from <em>Rats!</em>' camel case naming
   * convention into ML's lower case with underscores naming
   * convention.  Then, if this method has not been invoked on the
   * specified name before, it returns a new variant type with an
   * empty list of tuples.  Otherwise, it simply returns the
   * previously created variant type.  The returned variant type has
   * the {@link Constants#ATT_NODE} attribute.
   *
   * @see #toVariantName(String)
   *
   * @param name The name in camel case.
   * @param poly The flag for whether the variant is polymorphic.
   * @return The corresponding variant type.
   */
  public VariantT toVariant(String name, boolean poly) {
    final String vname = toVariantName(name);

    final VariantT variant;
    if (variants.containsKey(vname)) {
      variant = variants.get(vname);
      assert poly == variant.isPolymorphic();
    } else {
      variant = new VariantT(vname, poly, new ArrayList<TupleT>());
      variant.addAttribute(Constants.ATT_NODE);
      variants.put(vname, variant);
      variantNodes.put(vname, new HashSet<String>());
      originalNames.put(vname, name);
    }

    return variant;
  }

  /**
   * Get the original name for the specified variant.
   *
   * <p />The specified variant must have been created with {@link
   * #toVariant(String,boolean)}.
   *
   * @param variant The variant.
   * @return The original name in <em>Rats!</em>' camel case.
   */
  public String toOriginal(VariantT variant) {
    final String vname = variant.getName();

    // Make sure the variant is registered.
    assert null != vname;
    assert variants.containsKey(vname);
    assert variant == variants.get(vname);

    return originalNames.get(vname);
  }

  // ==========================================================================

  /**
   * Determine whether a tuple type with the specified name has been
   * created before.
   *
   * @param name The name.
   * @return <code>true</code> if a tuple type with the name exists.
   */
  public boolean hasTuple(String name) {
    return tuples.containsKey(name);
  }

  /**
   * Determine whether the tuple type with the specified name has been
   * created before and is monomorphic.
   *
   * @param name The name.
   * @return <code>true</code> if a tuple type with the name exists
   *   and is monomorphic.
   */
  public boolean isMonomorphic(String name) {
    return (tuples.containsKey(name) &&
            (0 < tupleVariants.get(name).size()) &&
            (! tupleVariants.get(name).get(0).isPolymorphic()));
  }

  /**
   * Get the tuple type with the specified name.  If this method has
   * not been invoked on the specified name before, it returns a new
   * tuple type, which is incomplete.  Otherwise, it simply returns
   * the previoulsy created tuple type.
   *
   * @param name The name.
   * @return The corresponding tuple type.
   */
  public TupleT toTuple(String name) {
    final TupleT tuple;

    if (tuples.containsKey(name)) {
      tuple = tuples.get(name);
    } else {
      tuple = new TupleT(name);
      tuples.put(name, tuple);
      tupleVariants.put(name, new ArrayList<VariantT>());
    }

    return tuple;
  }

  /**
   * Add the specified tuple type to the specified variant type.  If
   * the tuple is not a member of the specified variant, this method
   * adds it, while also updating its internal state.
   *
   * <p />The specified tuple must have been created with {@link
   * #toTuple(String)} or {@link #toTuple(VariantT)}.  The specified
   * variant must have been created with {@link
   * #toVariant(String,boolean)}.
   *
   * @param tuple The tuple.
   * @param variant The variant.
   */
  public void add(TupleT tuple, VariantT variant) {
    final String tname = tuple.getName();
    final String vname = variant.getName();

    // Make sure the tuple is registered.
    assert tuples.containsKey(tname);
    assert tuple == tuples.get(tname);
    assert tupleVariants.containsKey(tname);

    // Make sure the variant is registered.
    if (null == vname) {
      assert variant.isPolymorphic();
    } else {
      assert variants.containsKey(vname);
      assert variant == variants.get(vname);
    }

    // Actually add the tuple.
    final Type t = variant.lookup(tname);
    if (t.isError()) {
      assert variant.isPolymorphic() ||
        (null != vname &&
         ! variantNodes.get(vname).contains(tuple.getSimpleName()));

      if (! variant.isPolymorphic()) {
        variantNodes.get(vname).add(tuple.getSimpleName());
      }
      if (null != vname) {
        tupleVariants.get(tname).add(variant);
      }
      variant.getTuples().add(tuple);

    } else {
      assert t == tuple;
    }
  }

  /**
   * Get the specified tuple's variants.
   *
   * <p />The specified tuple must have been created with {@link
   * #toTuple(String)} or {@link #toTuple(VariantT)}.  It must have
   * been added to any variants with {@link #add(TupleT,VariantT)}.
   *
   * @param tuple The tuple.
   * @return The tuple's variants.
   */
  public List<VariantT> toVariants(TupleT tuple) {
    final String tname = tuple.getName();

    // Make sure the tuple is registered.
    assert tuples.containsKey(tname);
    assert tuple == tuples.get(tname);

    List<VariantT> variants = tupleVariants.get(tname);
    assert null != variants;
    return variants;
  }

  // ==========================================================================

  /**
   * Get the polymorphic tuple for the specified variant.
   *
   * <p />The specified variant must have been created with {@link
   * #toVariant(String,boolean)}.
   *
   * @param variant The variant.
   * @return The corresponding polymorphic tuple.
   */
  public TupleT toTuple(VariantT variant) {
    final String vname = variant.getName();

    // Make sure the variant is registered.
    assert ! variant.isPolymorphic();
    assert variants.containsKey(vname);
    assert variant == variants.get(vname);

    final String qualifier = variant.getQualifier();

    String tname = "Some" + Utilities.unqualify(originalNames.get(vname));
    if (isMonomorphic(Utilities.qualify(qualifier, tname))) {
      tname = "Just" + tname;

      while (isMonomorphic(Utilities.qualify(qualifier, tname))) {
        tname = tname + "1";
      }
    }

    TupleT tuple = toTuple(Utilities.qualify(qualifier, tname));
    if (null == tuple.getTypes()) {
      tuple.setTypes(new ArrayList<Type>(1));
      tuple.getTypes().add(variant);
    }

    return tuple;
  }

  // ==========================================================================

  /**
   * Determine whether the specified variants overlap.  Two variants
   * overlap if they include tuples representing the same generic
   * node.
   *
   * <p />The specified variants must have been created with {@link
   * #toVariant(String,boolean)}.
   *
   * @param v1 The first variant.
   * @param v2 The second variant.
   * @return <code>true</code> if the two variants overlap.
   */
  public boolean overlap(VariantT v1, VariantT v2) {
    if (v1.isPolymorphic()) {
      for (TupleT tuple : v1.getTuples()) {
        if (overlap(tuple.getTypes().get(0).toVariant(), v2)) return true;
      }

    } else if (v2.isPolymorphic()) {
      for (TupleT tuple : v2.getTuples()) {
        if (overlap(v1, tuple.getTypes().get(0).toVariant())) return true;
      }

    } else {
      final String vname1 = v1.getName();
      final String vname2 = v2.getName();

      assert variants.containsKey(vname1);
      assert v1 == variants.get(vname1);
      assert variants.containsKey(vname2);
      assert v2 == variants.get(vname2);

      final Set<String> nodes1 = variantNodes.get(v1.getName());
      final Set<String> nodes2 = variantNodes.get(v2.getName());

      for (String s : nodes1) {
        if (nodes2.contains(s)) return true;
      }
    }

    return false;
  }

  // ==========================================================================

  /** The metadata for a grammar's statically typed nodes. */
  public static class MetaData {

    /** Create a new metadata record. */
    public MetaData() {
      reachable  = new HashSet<String>();
      modularize = false;
    }

    /** The names of reachable variants. */
    public Set<String> reachable;

    /** The flag for requiring separate modules. */
    public boolean modularize;

  }

  /**
   * Determine the metadata for the specified variant.
   *
   * @param variant The variant.
   * @return The corresponding metadata.
   */
  public MetaData getMetaData(VariantT variant) {
    MetaData meta = new MetaData();
    fillIn(variant, meta, new HashSet<String>());
    return meta;
  }

  /**
   * Fill in the specified metadata for the specified type.
   *
   * @param type The type.
   * @param meta The metadata.
   * @param names The simple variant names processed so far.
   */
  private void fillIn(Type type, MetaData meta, Set<String> names) {
    switch (type.tag()) {
    case VARIANT: {
      final VariantT     variant = type.resolve().toVariant();
      final String       qname   = variant.getName();
      final String       sname   = variant.getSimpleName();
      final List<TupleT> tuples  = variant.getTuples();

      if (null == qname) {
        for (TupleT t : tuples) fillIn(t, meta, names);
      } else if (! meta.reachable.contains(qname)) {
        meta.reachable.add(qname);
        if (names.contains(sname)) {
          meta.modularize = true;
        } else {
          names.add(sname);
        }
        for (TupleT t : tuples) fillIn(t, meta, names);
      }
    } break;

    case TUPLE: {
      final List<Type> types = type.resolve().toTuple().getTypes();
      if (null != types) for (Type t : types) fillIn(t, meta, names);
    } break;

    case INTERNAL: {
      final String name = type.resolve().toInternal().getName();

      if ("list".equals(name) || "action".equals(name)) {
        if (type.hasInstantiated()) {
          fillIn(getArgument(type), meta, names);
        }
      }
    } break;

    default:
      // Nothing to do.
    }
  }

  // ==========================================================================

  /**
   * Create a new list type.
   *
   * @param element The element type.
   * @return The corresponding list type.
   */
  public static Type listOf(Type element) {
    return new InstantiatedT(element, LIST);
  }

  /**
   * Create a new action type.
   *
   * @param element The element type.
   * @return The corresponding action type.
   */
  public static Type actionOf(Type element) {
    return new InstantiatedT(element, ACTION);
  }

  // ==========================================================================

  /**
   * Unify the specified types.  If the strict flag is set, statically
   * and dynamically typed nodes do not unify.  If the flag is not
   * set, they do unify and otherwise incompatible types unify to the
   * any type.  This method defers to {@link
   * #unify(VariantT,VariantT)} for statically typed nodes and to
   * {@link #unifyUser(Type,Type,boolean)} for user-defined types.
   *
   * @param t1 The first type.
   * @param t2 The second type.
   * @param strict The flag for strict unification.
   * @return The unified type or {@link ErrorT#TYPE} if the two types
   *   do not unify.
   */
  public Type unify(Type t1, Type t2, boolean strict) {
    // Get the trivial cases out of the way.
    if (t1 == t2) return t1;
    if (t1.hasError() || t2.hasError()) return ErrorT.TYPE;
    if (t1.resolve().isParameter()) return t2;
    if (t2.resolve().isParameter()) return t1;

    // Now, do the real work.
    Type result = unify1(t1, t2, strict);

    // Adjust the result for non-strict unification.
    if (result.isError() && ! strict) result = ANY;

    // Preserve any variable or optional attribute, in this precedence
    // order.
    if (! result.isError()) {
      if (isVariable(t1) || isVariable(t2)) {
        result = markVariable(result);
      } else if (isOptional(t1) || isOptional(t2)) {
        result = markOptional(result);
      }
    }

    // Done.
    return result;
  }

  /**
   * Actually unify the specified types.
   *
   * @param t1 The first type.
   * @param t2 The second type.
   * @param strict The flag for strict unification.
   * @return The unified type or {@link ErrorT#TYPE} if the two types
   *   do not unify.
   */
  private Type unify1(Type t1, Type t2, boolean strict) {
    Type r1 = t1.resolve(), r2 = t2.resolve();

    if (t1.hasInstantiated() && t2.hasInstantiated()) {
      InstantiatedT i1 = t1.toInstantiated(), i2 = t2.toInstantiated();
      List<Type>    a1 = i1.getArguments(),   a2 = i2.getArguments();

      if (a1.size() != a2.size()) return ErrorT.TYPE;

      Type base = unify(i1.getType(), i2.getType(), true);
      if (base.isError()) return ErrorT.TYPE;

      List<Type> args = new ArrayList<Type>(a1.size());
      for (int i=0; i<a1.size(); i++) {
        Type t = unify(a1.get(i), a2.get(i), true);
        if (t.isError()) return ErrorT.TYPE;
        args.add(t);
      }

      return new InstantiatedT(args, base);

    } else if (t1.hasInstantiated() || t2.hasInstantiated()) {
      return ErrorT.TYPE;

    } else if (isUser(t1) && isUser(t2)) {
      return unifyUser(t1, t2, strict);

    } else if (isNode(t1) && isNode(t2)) {
      if (isNullNode(t1)) {
        return r2;

      } else if (isNullNode(t2)) {
        return r1;

      } else if (isToken(t1) && isToken(t2)) {
        return TOKEN;

      } else if ((isToken(t1) || isDynamicNode(t1) || isFormatting(t1)) &&
                 (isToken(t2) || isDynamicNode(t2) || isFormatting(t2))) {
        return NODE;

      } else if (r1.isVariant() && r2.isVariant()) {
        return unify(r1.toVariant(), r2.toVariant());

      } else if (strict) {
        return ErrorT.TYPE;

      } else {
        return NODE;
      }

    } else if (r1.isVoid() && r2.isVoid()) {
      return VoidT.TYPE;

    } else if (r1.isUnit() && r2.isUnit()) {
      return UnitT.TYPE;

    } else if (isChar(t1) && isChar(t2)) {
      return CHAR;

    } else if (isString(t1) && isString(t2)) {
      return STRING;

    } else if (isList(t1) && isList(t2)) {
      return LIST;

    } else if (isAction(t1) && isAction(t2)) {
      return ACTION;

    } else {
      return ErrorT.TYPE;
    }
  }

  /**
   * Unify the specified statically typed nodes.  Statically typed
   * nodes unify through polymorphic variant types, unless they
   * reference the same underlying AST node.
   *
   * @param v1 The first variant.
   * @param v2 The second variant.
   * @return The unified variant.
   */
  protected Type unify(VariantT v1, VariantT v2) {
    // Get the trivial case out of the way.
    if ((null != v1.getName()) && v1.getName().equals(v2.getName())) return v1;

    // Unify the two variants.
    List<TupleT> tuples = new ArrayList<TupleT>();
    VariantT     result = new VariantT(null, true, tuples);
    result.addAttribute(Constants.ATT_NODE);

    if (v1.isPolymorphic()) {
      tuples.addAll(v1.getTuples());

      if (v2.isPolymorphic()) {
        for (TupleT tuple : v2.getTuples()) {
          if (! tuples.contains(tuple)) {
            final VariantT v3 = tuple.getTypes().get(0).toVariant();
            if (overlap(v1, v3)) return ErrorT.TYPE;
            tuples.add(tuple);
          }
        }

      } else {
        TupleT tuple = toTuple(v2);
        if (! tuples.contains(tuple)) {
          if (overlap(v1, v2)) return ErrorT.TYPE;
          tuples.add(tuple);
        }
      }

    } else if (v2.isPolymorphic()) {
      tuples.addAll(v2.getTuples());

      TupleT tuple = toTuple(v1);
      if (! tuples.contains(tuple)) {
        if (overlap(v1, v2)) return ErrorT.TYPE;
        tuples.add(tuple);
      }

    } else {
      if (overlap(v1, v2)) return ErrorT.TYPE;
      tuples.add(toTuple(v1));
      tuples.add(toTuple(v2));
    }

    return result;
  }

  /**
   * Unify the specified user-defined types.  Note that this method
   * need not handle instantiated types but must preserve parameterize
   * types.
   *
   * @param t1 The first user-defined type.
   * @param t2 The second user-defined type.
   * @param strict The flag for strict unification.
   * @return The unified type or {@link ErrorT#TYPE} if the two types
   *   do not unify.
   */
  protected abstract Type unifyUser(Type t1, Type t2, boolean strict);

  // ==========================================================================

  /**
   * Flatten the specified tuple type.  If the specified tuple has a
   * list element, this method combines the first such list type with
   * all succeeding element types into a single list type, modifying
   * the specified tuple type.
   *
   * @param tuple The tuple type.
   * @param strict The flag for strict unification.
   * @return The updated tuple type or {@link ErrorT#TYPE} if types
   *   cannot be unified.
   */
  public Type flatten(TupleT tuple, boolean strict) {
    final List<Type> types = tuple.getTypes();
    final int        size  = types.size();

    int     index    = -1;    // The index of the first list.
    Type    element  = null;  // The element type of the list.
    boolean optional = false; // The flag for optional elements.

    for (int i=0; i<size; i++) {
      Type t = types.get(i);

      if (-1 == index) {
        if (isList(t)) {
          index = i;

          t = getArgument(t);
          if (isOptional(t)) optional = true;
          t = t.deannotate();

          element = t;
        }

      } else {
        if (isList(t)) t = getArgument(t);
        if (isOptional(t)) optional = true;
        t = t.deannotate();

        element = unify(element, t, strict);
        if (element.isError()) return ErrorT.TYPE;
      }
    }

    if (-1 != index) {
      // Create the flattened list type.
      if (optional) element = markOptional(element);
      Type list = listOf(element);
      if (isVariable(types.get(index))) list = markVariable(list);

      // Update the tuple type.
      types.subList(index, size).clear();
      types.add(list);
    }

    return tuple;
  }

  // ==========================================================================

  /**
   * Combine the specified tuple types into a consistent type.  The
   * types must be tuples with the same name.
   *
   * @param tuple1 The first tuple.
   * @param tuple2 The second tuple.
   * @param flatten The flag for flattening lists.
   * @param strict The flag for strict unification.
   * @return The combined tuple type or {@link ErrorT#TYPE} if the two
   *   tuple types cannot be combined into a consistent type.
   */
  public Type combine(TupleT tuple1, TupleT tuple2,
                      boolean flatten, boolean strict) {

    assert tuple1.getName().equals(tuple2.getName());

    // If both tuple types are equal, we are done.
    if ((tuple1 == tuple2) || tuple1.equals(tuple2)) return tuple1;

    // Set up.
    final List<Type> types1 = tuple1.getTypes(), types2 = tuple2.getTypes();
    final int        size1  = types1.size(),     size2  = types2.size();

    // If lists are flattened, then find the first list across both
    // tuple types and determine the combined list element type.
    int     listIdx  = Integer.MAX_VALUE;
    Type    elemT    = Wildcard.TYPE;
    boolean variable = false; // Flag for list being variable.
    boolean optional = false; // Flag for element being optional.

    if (flatten) {
      // Find the first list type across both tuple types.
      for (int i=0; i<size1; i++) {
        Type t = types1.get(i);

        if (isList(t)) {
          listIdx  = i;
          variable = isVariable(t);
          break;
        }
      }

      for (int i=0; i<size2; i++) {
        Type t = types2.get(i);

        if (isList(t)) {
          if (i < listIdx) {
            listIdx  = i;
            variable = isVariable(t);
          }
          break;
        }
      }

      // Determine the combined list element type.
      if (Integer.MAX_VALUE != listIdx) {
        for (int i=listIdx; i<size1; i++) {
          Type t = types1.get(i);
          if (isList(t)) t = getArgument(t);
          if (isOptional(t)) optional = true;
          t = t.deannotate();

          elemT  = unify(elemT, t, strict);
          if (elemT.isError()) return ErrorT.TYPE;
        }

        for (int i=listIdx; i<size2; i++) {
          Type t = types2.get(i);
          if (isList(t)) t = getArgument(t);
          if (isOptional(t)) optional = true;
          t = t.deannotate();

          elemT  = unify(elemT, t, strict);
          if (elemT.isError()) return ErrorT.TYPE;
        }
      }
    }

    // Determine the combined tuple's element types.
    final List<Type> types3 = new ArrayList<Type>(Math.max(size1, size2));
    final int        size3  = Math.min(Math.max(size1, size2), listIdx);

    for (int i=0; i<size3; i++) {
      final Type t1 = (i < size1) ? types1.get(i) : null;
      final Type t2 = (i < size2) ? types2.get(i) : null;

      Type t3;
      if (null == t1) {
        t3 = markVariable(t2.deannotate());
      } else if (null == t2) {
        t3 = markVariable(t1.deannotate());
      } else {
        t3 = unify(t1.deannotate(), t2.deannotate(), strict);
        if (t3.isError()) return ErrorT.TYPE;
        if (isVariable(t1) || isVariable(t2)) {
          t3 = markVariable(t3);
        } else if (isOptional(t1) || isOptional(t2)) {
          t3 = markOptional(t3);
        } else if ((t1.resolve().isWildcard() || t2.resolve().isWildcard()) &&
                   ! t3.resolve().isWildcard()) {
          t3 = markOptional(t3);
        }
      }

      types3.add(t3);
    }

    if (Integer.MAX_VALUE != listIdx) {
      if (optional) elemT = markOptional(elemT);

      Type list = listOf(elemT);
      if (variable || (listIdx > size1) || (listIdx > size2)) {
        // The new trailing list is variable if (1) the original list
        // is variable or (2) the original list is at a position
        // beyond one tuple's elements plus one (which accounts for
        // the empty list not having any children).
        list = markVariable(list);
      }

      types3.add(list);
    }

    // Done.
    return new TupleT(tuple1.getName(), types3);
  }

  // ==========================================================================

  /**
   * Ensure that the specified type is concrete.  This method replaces
   * occurrences of the wildcard type with the specified replacement;
   * though it does not process variant types to avoid infinite
   * recursions.  It assumes that list and action types are
   * instantiated.
   *
   * @see #concretizeTuples(VariantT,Type)
   *
   * @param type The type.
   * @param concrete The concrete replacement for wildcards.
   * @return The concrete type.
   */
  public Type concretize(Type type, Type concrete) {
    Type resolved = type.resolve(), result = null;

    if (resolved.isWildcard()) {
      result = concrete;

    } else if (resolved.isTuple()) {
      TupleT     tuple    = resolved.toTuple();
      List<Type> elements = tuple.getTypes();
      boolean    isCopy   = false;

      for (int i=0; i<elements.size(); i++) {
        Type element = concretize(elements.get(i), concrete);

        if (elements.get(i) != element) {
          if (! isCopy) {
            elements = new ArrayList<Type>(elements);
            isCopy   = true;
          }
          elements.set(i, element);
        }
      }

      if (isCopy) result = new TupleT(tuple.getName(), elements);

    } else if (isList(type)) {
      Type el = concretize(getArgument(type), concrete);
      if (el != getArgument(type)) result = listOf(el);

    } else if (isAction(type)) {
      Type el = concretize(getArgument(type), concrete);
      if (el != getArgument(type)) result = actionOf(el);
    }

    if (null == result) {
      return type;

    } else {
      if (isVariable(type)) {
        result = markVariable(result);
      } else if (isOptional(type)) {
        result = markOptional(result);
      }

      return result;
    }
  }

  /**
   * Concretize the specified variant type's tuples.  This method
   * updates any tuples in place.
   *
   * @see #concretize(Type,Type)
   *
   * @param variant The variant.
   * @param concrete The concrete replacement for wildcards.
   */
  public void concretizeTuples(VariantT variant, Type concrete) {
    List<TupleT> tuples = variant.getTuples();

    if (null != tuples) {
      for (int i=0; i<tuples.size(); i++) {
        tuples.set(i, concretize(tuples.get(i), concrete).toTuple());
      }
    }
  }

  // ==========================================================================

  /**
   * Print the specified type.
   *
   * @param printer The printer.
   * @param type The type.
   * @param refIsDecl The flag for whether a variant type reference
   *   also is a declaration.
   * @param qualified The flag for printing qualified names.
   * @param module The current module name, which may be <code>null</code>.
   */
  public void print(Type type, Printer printer, boolean refIsDecl,
                    boolean qualified, String module) {
    switch (type.tag()) {
    case VARIANT: {
      final VariantT     variant = type.resolve().toVariant();
      final boolean      poly    = variant.isPolymorphic();
      final List<TupleT> tuples  = variant.getTuples();

      final String name;
      if (! qualified ||
          (null != module && module.equals(variant.getQualifier()))) {
        name = variant.getSimpleName();
      } else {
        name = variant.getName();
      }

      if (null == name) {
        printer.pln('[').incr().incr();
        for (TupleT tuple : tuples) {
          printer.indent().p("| `");
          print(tuple, true, printer, qualified, module);
          printer.pln();
        }
        printer.decr().decr().indentMore().p(']');

      } else if (! refIsDecl) {
        printer.p(name);

      } else if (poly) {
        printer.indent().p("mltype ").p(name).pln(" = [").incr();
        for (TupleT tuple : tuples) {
          printer.indent().p("| `");
          print(tuple, true, printer, qualified, module);
          printer.pln();
        }
        printer.decr().indent().pln("];");

      } else if (1 == tuples.size()) {
        printer.indent().p("mltype ").p(name).p(" = ");
        print(tuples.get(0), false, printer, qualified, module);
        printer.pln(" ;");

      } else {
        printer.indent().p("mltype ").p(name).pln(" =").incr();
        for (Iterator<TupleT> iter = tuples.iterator(); iter.hasNext(); ) {
          printer.indent().p("| ");
          print(iter.next(), false, printer, qualified, module);
          if (iter.hasNext()) printer.pln();
        }
        printer.pln(" ;").decr();
      }
    } break;

    case TUPLE:
      print(type.resolve().toTuple(), false, printer, qualified, module);
      break;

    case INTERNAL: {
      final String name = type.resolve().toInternal().getName();

      if ("list".equals(name) || "action".equals(name)) {
        if (type.hasInstantiated()) {
          print(type.toInstantiated().getArguments().get(0),
                printer, false, qualified, module);
        } else {
          print(type.toParameterized().getParameters().get(0),
                printer, false, qualified, module);
        }
        printer.p(' ');
      }

      printer.p(name);
    } break;

    case UNIT:
    case VOID:
      printer.p("bottom");
      break;

    case NAMED_PARAMETER:
    case INTERNAL_PARAMETER:
    case WILDCARD:
      printer.p("'").p(type.resolve().toString());
      break;
      
    case ERROR:
      printer.p("<error>");
      break;

    default:
      throw new AssertionError("Invalid type " + type);
    }

    if (! refIsDecl) {
      if (type.hasAttribute(Constants.ATT_VARIABLE)) {
        printer.p(" var");
      } else if (type.hasAttribute(Constants.ATT_OPTIONAL)) {
        printer.p(" opt");
      }
    }
  }

  /**
   * Print the specified tuple.
   *
   * @param tuple The tuple.
   * @param poly The flag for polymorphic tuples.
   * @param printer The printer.
   * @param qualified The flag for printing qualified names.
   * @param module The current module name, which may be <code>null</code>.
   */
  private void print(TupleT tuple, boolean poly, Printer printer,
                     boolean qualified, String module) {
    String name = tuple.getName();
    if (! qualified || ! poly ||
        (null != module && module.equals(Utilities.getQualifier(name)))) {
      name = tuple.getSimpleName();
    }

    printer.p(name);
    final List<Type> types = tuple.getTypes();
    if ((null != types) && ! types.isEmpty()) {
      printer.p(" of ");
      for (Iterator<Type> iter = types.iterator(); iter.hasNext(); ) {
        Type t = iter.next();
        
        if (t.resolve().isVariant() &&
            (null == t.resolve().toVariant().getName())) {
          print(t, printer, false, qualified, module);
          if (iter.hasNext()) printer.p(" * ");
        } else {
          printer.buffer();
          print(t, printer, false, qualified, module);
          if (iter.hasNext()) printer.p(" * ");
          printer.fitMore();
        }
      }
    }
  }

}
