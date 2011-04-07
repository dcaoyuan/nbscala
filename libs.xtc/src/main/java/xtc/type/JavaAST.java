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

import xtc.Constants.FuzzyBoolean;

import xtc.tree.Locatable;

/**
 * Common type operations for Java ASTs.
 *
 * @author Robert Grimm
 * @version $Revision: 1.9 $
 */
public class JavaAST extends AST {

  /** The set of Java primitive types. */
  public static final Set<String> PRIMITIVES;

  /** The set of Java modifiers. */
  public static final Set<String> MODIFIERS;

  /** The set of Java keywords besides primitive types, modifiers, and void. */
  public static final Set<String> KEYWORDS;

  static {
    Set<String> primitives = new HashSet<String>();

    primitives.add("byte");
    primitives.add("short");
    primitives.add("char");
    primitives.add("int");
    primitives.add("long");
    primitives.add("float");
    primitives.add("double");
    primitives.add("boolean");

    PRIMITIVES = Collections.unmodifiableSet(primitives);

    Set<String> modifiers = new HashSet<String>();

    modifiers.add("public");
    modifiers.add("protected");
    modifiers.add("private");
    modifiers.add("static");
    modifiers.add("abstract");
    modifiers.add("final");
    modifiers.add("native");
    modifiers.add("synchronized");
    modifiers.add("transient");
    modifiers.add("volatile");
    modifiers.add("strictfp");

    MODIFIERS = Collections.unmodifiableSet(primitives);

    Set<String> keywords = new  HashSet<String>();

    keywords.add("assert");
    keywords.add("break");
    keywords.add("case");
    keywords.add("catch");
    keywords.add("class");
    keywords.add("const");
    keywords.add("continue");
    keywords.add("default");
    keywords.add("do");
    keywords.add("else");
    keywords.add("enum");
    keywords.add("extends");
    keywords.add("finally");
    keywords.add("for");
    keywords.add("if");
    keywords.add("goto");
    keywords.add("implements");
    keywords.add("import");
    keywords.add("instanceof");
    keywords.add("interface");
    keywords.add("new");
    keywords.add("package");
    keywords.add("return");
    keywords.add("super");
    keywords.add("switch");
    keywords.add("this");
    keywords.add("throw");
    keywords.add("throws");
    keywords.add("try");
    keywords.add("while");

    KEYWORDS = Collections.unmodifiableSet(keywords);
  }

  // =========================================================================

  /** The map from Java class names to their classes. */
  protected Map<String,Class<?>> resolvedTypes;
  
  /** Create a new Java AST instance. */
  public JavaAST() {
    resolvedTypes = new HashMap<String,Class<?>>();
  }

  // =========================================================================

  public void initialize(boolean hasNode, boolean hasToken,
                         boolean hasFormatting, boolean hasAction) {
    externToIntern.clear();
    internToExtern.clear();

    externToIntern.put("?", Wildcard.TYPE);
    externToIntern.put("void", VoidT.TYPE);
    externToIntern.put("Object", ANY);
    externToIntern.put("java.lang.Object", ANY);
    externToIntern.put("Character", CHAR);
    externToIntern.put("java.lang.Character", CHAR);
    externToIntern.put("String", STRING);
    externToIntern.put("java.lang.String", STRING);
    if (hasToken) externToIntern.put("Token", TOKEN);
    externToIntern.put("xtc.tree.Token", TOKEN);
    if (hasNode) externToIntern.put("Node", NODE);
    externToIntern.put("xtc.tree.Node", NODE);
    externToIntern.put("generic", GENERIC);
    if (hasNode) externToIntern.put("GNode", NODE);
    externToIntern.put("xtc.tree.GNode", NODE);
    if (hasFormatting) externToIntern.put("Formatting", FORMATTING);
    externToIntern.put("xtc.tree.Formatting", FORMATTING);
    externToIntern.put("Pair", WILD_LIST);
    externToIntern.put("xtc.util.Pair", WILD_LIST);
    if (hasAction) externToIntern.put("Action", WILD_ACTION);
    externToIntern.put("xtc.util.Action", WILD_ACTION);

    internToExtern.put("?", "?");
    internToExtern.put("void", "Void");
    internToExtern.put("unit", "Void");
    internToExtern.put("any", "Object");
    internToExtern.put("char", "Character");
    internToExtern.put("string", "String");
    internToExtern.put("token", "Token");
    internToExtern.put("node", "Node");
    internToExtern.put("formatting", "Formatting");
    internToExtern.put("list", "Pair");
    internToExtern.put("action", "Action");
  }

  // ==========================================================================

  public boolean isVoid(String s) {
    return "void".equals(s);
  }

  public boolean isGenericNode(String s) {
    return "generic".equals(s);
  }

  // ==========================================================================

  /** The start index for a fully qualified list's element type. */
  private static final int QLIST_IDX = "xtc.util.Pair<".length();

  /** The start index for a list's element type. */
  private static final int LIST_IDX = "Pair<".length();

  protected Type internList(String s) {
    if (s.startsWith("xtc.util.Pair<")) {
      return new
        InstantiatedT(intern(s.substring(QLIST_IDX, s.length()-1)), LIST);
    } else if (s.startsWith("Pair<")) {
      return new
        InstantiatedT(intern(s.substring(LIST_IDX, s.length()-1)), LIST);
    } else {
      return ErrorT.TYPE;
    }
  }

  /** The start index for a fully qualified action's element type. */
  private static final int QACTION_IDX = "xtc.util.Action<".length();

  /** The start index for an action's element type. */
  private static final int ACTION_IDX = "Action<".length();

  protected Type internAction(String s) {
    if (s.startsWith("xtc.util.Action<")) {
      return new 
        InstantiatedT(intern(s.substring(QACTION_IDX, s.length()-1)), ACTION);
    } else if (s.startsWith("Action<")) {
      return new
        InstantiatedT(intern(s.substring(ACTION_IDX, s.length()-1)), ACTION);
    } else {
      return ErrorT.TYPE;
    }
  }

  protected Type internUser(String s) {
    if (PRIMITIVES.contains(s)) {
      throw new IllegalArgumentException("Java primitive type");
    } else if (MODIFIERS.contains(s)) {
      throw new IllegalArgumentException("Java modifier as type");
    } else if (KEYWORDS.contains(s)) {
      throw new IllegalArgumentException("Java keyword as type");
    }

    final int idx = s.indexOf('<');
    if (-1 == idx) {
      return new ClassT(s, null, null, null, null);

    } else {
      Type   type = new ClassT(s.substring(0, idx), null, null, null, null);
      String args = s.substring(idx+1, s.length()-1);

      List<Parameter> parameters = new ArrayList<Parameter>();
      List<Type>      arguments  = new ArrayList<Type>();

      int count = 1, start = 0;
      do {
        int end = endOfType(args, start);

        parameters.add(new NamedParameter("T" + count));
        arguments.add(intern(args.substring(start, end)));

        start = end + 1;
        count++;
      } while (start < args.length());

      return new InstantiatedT(arguments, new ParameterizedT(parameters, type));
    }
  }

  /**
   * Determine the last index (exclusive) of the generic type argument
   * starting at the specified index.
   *
   * @param args The generic type arguments.
   * @param start The start index.
   * @return The end index (exclusive).
   */
  protected int endOfType(String args, int start) {
    final int size = args.length();

    int end = start, nesting = 0;
    do {
      char c = args.charAt(end);

      switch (c) {
      case '<':
        nesting++;
        break;
      case '>':
        nesting--;
        break;
      case ',':
        if (0 == nesting) return end;
      }

      end++;
    } while (end < size);

    return end;
  }

  // ==========================================================================

  protected String externList(Type type) {
    return "Pair<" + extern(getArgument(type)) + ">";
  }

  protected String externAction(Type type) {
    return "Action<" + extern(getArgument(type)) + ">";
  }

  protected String externUser(Type type) {
    if (type.hasInstantiated() || type.hasParameterized()) {
      StringBuilder buf = new StringBuilder();

      buf.append(extern(type.resolve()));
      buf.append('<');

      Iterator<? extends Type> iter = type.hasInstantiated() ?
        type.toInstantiated().getArguments().iterator() :
        type.toParameterized().getParameters().iterator();
      do {
        buf.append(extern(iter.next()));
        if (iter.hasNext()) buf.append(',');
      } while (iter.hasNext());

      buf.append('>');
      return buf.toString();

    } else {
      if (! type.resolve().isClass()) System.out.println(type);


      return type.resolve().toClass().getQName();
    }
  }

  // ==========================================================================

  protected FuzzyBoolean hasLocationUser(Type type) {
    final Class<?> k = resolve(type.resolve().toClass().getQName());

    if ((null == k) || Object.class.equals(k)) {
      return FuzzyBoolean.MAYBE;
    } else if (Locatable.class.isAssignableFrom(k)) {
      return FuzzyBoolean.TRUE;
    } else {
      return FuzzyBoolean.FALSE;
    }
  }

  // ==========================================================================

  protected Type unifyUser(Type t1, Type t2, boolean strict) {
    Type r1 = t1.resolve(), r2 = t2.resolve();

    if (r1.isClass() && r2.isClass() &&
        r1.toClass().getQName().equals(r2.toClass().getQName())) {
      return t1;
    } else if (strict) {
      return ErrorT.TYPE;
    } else {
      return ANY;
    }
  }

  // ==========================================================================

  /**
   * Resolve the specified type name to its class.  This method relies
   * on the {@link #importedTypes} and {@link #importedModules} data
   * structures to map incomplete type names to fully qualified type
   * names.  It caches results in the {@link #resolvedTypes} data
   * structure to speed up future resolutions.
   *
   * @param name The type name.
   * @return The corresponding class or <code>null</code> if the name
   *   cannot be resolved.
   */
  public Class<?> resolve(String name) {
    if (resolvedTypes.containsKey(name)) return resolvedTypes.get(name);

    Class<?> k = null;
    if (importedTypes.containsKey(name)) {
      // The type has been explicitly imported.
      try {
        k = Class.forName(importedTypes.get(name));
      } catch (ClassNotFoundException x) {
        // Ignore.
      }

    } else {
      // The type should have been implicitly imported.
      for (String module : importedModules) {
        try {
          k = Class.forName(module + name);
          break;
        } catch (ClassNotFoundException x) {
          // Ignore.
        }
      }

      if (null == k) {
        // As a last resort, try the name directly.
        try {
          k = Class.forName(name);
        } catch (ClassNotFoundException x) {
          // Ignore.
        }
      }
    }

    // Remember the resolved class.
    resolvedTypes.put(name, k);
    return k;
  }

}
