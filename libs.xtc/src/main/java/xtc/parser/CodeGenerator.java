/*
 * xtc - The eXTensible Compiler
 * Copyright (C) 2004-2008 Robert Grimm
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import xtc.Constants;
import xtc.Constants.FuzzyBoolean;

import xtc.tree.Attribute;
import xtc.tree.GNode;
import xtc.tree.Locatable;
import xtc.tree.Printer;
import xtc.tree.Visitor;

import xtc.type.AST;
import xtc.type.InstantiatedT;
import xtc.type.Type;

import xtc.util.Runtime;
import xtc.util.Utilities;

/**
 * The code generator. 
 *
 * <p />The code generator makes the following assumptions about the
 * intermediate language:<ul>
 *
 * <li>The entire grammar is contained in a single {@link Module
 * module}.</li>
 *
 * <li>All imported types have been registered with {@link
 * AST#importType} and all imported modules have been registered with
 * {@link AST#importModule}.</li>
 *
 * <li>If the grammar references any types that may be {@link
 * Locatable locatable} (as opposed to definitely being or not being
 * locatable), the grammar must have a {@link Properties#LOCATABLE
 * locatable} property with value <code>Boolean.TRUE</code>.<p /></li>
 *
 * <li>If the grammar contains any {@link GenericValue generic
 * values}, the grammar must have a {@link Properties#GENERIC generic}
 * property with value <code>Boolean.TRUE</code>.<p /></li>
 *
 * <li>If the grammar contains any {@link ActionBaseValue action base
 * values}, {@link GenericActionValue generic action value}, or {@link
 * GenericRecursionValue generic recursion values}, the grammar must
 * have a {@link Properties#RECURSIVE recursive} property with value
 * <code>Boolean.TRUE</code>.<p /></li>
 *
 * <li>Each {@link Production production} must be a {@link
 * FullProduction full production} and must have been annotated with
 * the appropriate {@link MetaData meta-data}.<p /></li>
 *
 * <li>Generally, all {@link Option options}, {@link Repetition
 * repetitions}, and nested {@link OrderedChoice ordered choices} must
 * have been desugared into equivalent productions.  However, an
 * ordered choice may appear as the <i>last</i> element of a sequence
 * (that is <i>not</i> part of a predicate), a repetition may appear
 * if the list of repeated expressions need not be memoized, an an
 * option may appear if its value is not bound or depends on a single
 * bound element.<p /></li>
 *
 * <li>The element of a {@link Repetition repetition} must be a {@link
 * Sequence sequence} (with the last element possibly being an ordered
 * choice; see previous assumption).  If the repetition is bound,
 * {@link Analyzer#bind(List)} must be able to capture the semantic
 * value of the repeated element.<p /></li>
 *
 * <li>The element of a {@link Option option} must be a {@link
 * Sequence sequence} (with the last element possibly being an ordered
 * choice).  If the option is bound, {@link Analyzer#bind(List)} must
 * be able to capture the semantic value of the optional element.<p
 * /></li>
 *
 * <li>The element of a {@link StringMatch string match} must be a
 * {@link NonTerminal nonterminal}.<p /></li>
 *
 * <li>The element of a {@link FollowedBy} or {@link NotFollowedBy}
 * predicate must be a sequence.<p /></li>
 *
 * <li>All elements in a {@link CharSwitch character switch} must
 * either be ordered choices or sequences.  Furthermore, character
 * switches may only appear as the last element in a sequence and not
 * within predicates.<p /></li>
 *
 * </ul>
 *
 * @author Robert Grimm
 * @version $Revision: 1.293 $
 */
public class CodeGenerator extends Visitor {

  /** The size of chunks. */
  public static final int CHUNK_SIZE = 10;

  /** The prefix for parsing method names. */
  public static final String PREFIX_METHOD = "p";

  /** The prefix for field names that memoize the parsers results. */
  public static final String PREFIX_FIELD = "f";

  /** The prefix for field names that count accesses to memoized results. */
  public static final String PREFIX_COUNT_FIELD = "c";

  /** The general prefix for internal parser fields and variables. */
  public static final String PREFIX = "yy";

  /** The name for the variable referencing the verbose mode printer. */
  public static final String PRINTER = PREFIX + "Out";

  /** The name for the variable referencing the global state object. */
  public static final String STATE = PREFIX + "State";

  /** The name of the character parsing method. */
  public static final String PARSE_CHAR = "character";

  /** The name of the index argument. */
  public static final String ARG_INDEX = PREFIX + "Start";

  /** The name of the memoization column variable. */
  public static final String COLUMN = PREFIX + "Column";

  /** The name of the character variable. */
  public static final String CHAR = PREFIX + "C";

  /** The name of the index variable. */
  public static final String INDEX = PREFIX + "Index";

  /** The name for the result variable. */
  public static final String RESULT = PREFIX + "Result";

  /** The name of the predicate index variable. */
  public static final String PRED_INDEX = PREFIX + "PredIndex";

  /** The name for the predicate result variable. */
  public static final String PRED_RESULT = PREFIX + "PredResult";

  /** The name for the predicate matched variable. */
  public static final String PRED_MATCHED = PREFIX + "PredMatched";

  /** The name for the base index variable. */
  public static final String BASE_INDEX = PREFIX + "Base";

  /** The prefix for the index variable for nested choices. */
  public static final String NESTED_CHOICE = PREFIX + "Choice";

  /** The prefix for the index variable for repetitions. */
  public static final String REPETITION = PREFIX + "Repetition";

  /** The prefix for the index variable for options. */
  public static final String OPTION = PREFIX + "Option";

  /**
   * The prefix for the flag indicating that a repetition has been
   * matched at least once.
   */
  public static final String REPEATED = PREFIX + "Repeated";

  /**
   * The prefix for the variable referencing the semantic value of a
   * bound repetition.
   */
  public static final String REP_VALUE = PREFIX + "RepValue";

  /**
   * The prefix for the variable referencing the semantic value of a
   * bound option.
   */
  public static final String OP_VALUE = PREFIX + "OpValue";

  /** The name for the value variable (i.e., <code>yyValue</code>). */
  public static final String VALUE = PREFIX + "Value";

  /** The name for the parse error variable. */
  public static final String PARSE_ERROR = PREFIX + "Error";

  // ========================================================================

  /** The runtime. */
  protected final Runtime runtime;

  /** The analyzer utility. */
  protected final Analyzer analyzer;

  /** The type operations. */
  protected final AST ast;

  /** The printer utility. */
  protected final Printer printer;

  /** The flag for generating debugging code. */
  protected boolean attributeVerbose;

  /**
   * The flag for generating code to annotate nodes with location
   * information.
   */
  protected boolean attributeWithLocation;

  /** The flag for making variable bindings constant. */
  protected boolean attributeConstant;

  /** The flag for flattening lists. */
  protected boolean attributeFlatten;

  /** The flag for generating a parse tree. */
  protected boolean attributeParseTree;

  /** The flag for using raw types. */
  protected boolean attributeRawTypes;

  /** The flag for performing case-insensitive comparisons. */
  protected boolean attributeIgnoringCase;

  /** The flag for using a global state object. */
  protected boolean attributeStateful;

  /** The flag for having a string set attribute. */
  protected boolean attributeStringSet;

  /** The class name for the global state object. */
  protected String stateClassName;

  /** The class name for the generic node factory. */
  protected String factoryClassName;

  /** The flag for creating a main method. */
  protected boolean attributeMain;

  /** The nonterminal for the main method. */
  protected String mainMethodNonterminal = null;

  /** The flag for using a grammar-specified printer in the main method. */
  protected boolean attributePrinter;

  /** The class name for the grammar-specified printer. */
  protected String printerClassName;

  /** The flag for including code to produce a memoization profile. */
  protected boolean attributeProfile;

  /** The flag for including a method to dump the memoization table. */
  protected boolean attributeDump;

  /** The class name for the current grammar. */
  protected String className;

  /** Flag for whether the memoization fields are organized in chunks. */
  protected boolean chunked;

  /** The map from nonterminals to chunk numbers. */
  protected Map<NonTerminal, Integer> chunkMap;

  /** The number of chunks. */
  protected int chunkCount;

  /** The flag for the first element in a top-level choice. */
  protected boolean firstElement;

  /** The saved first element. */
  protected boolean savedFirstElement;

  /** The expression for the base index. */
  protected String baseIndex;

  /** The flag for using the base index. */
  protected boolean useBaseIndex;

  /** The saved base index. */
  protected String savedBaseIndex;

  /** The saved flag for using the base index. */
  protected boolean savedUseBaseIndex;

  /** The indentation level for choices. */
  protected int indentLevel;

  /** The nesting level for nested choices. */
  protected int choiceLevel;

  /** The flag for repetitions. */
  protected boolean repeated;

  /** The saved repetition flag for predicates. */
  protected boolean savedRepeated;

  /** The flag for at-least-once repetitions. */
  protected boolean repeatedOnce;

  /** The saved at-least-once flag for predicates. */
  protected boolean savedRepeatedOnce;

  /** The nesting level for repetitions. */
  protected int repetitionLevel;

  /**
   * The name of the variable referencing the element value for bound
   * repetitions, or <code>null</code> if no such repetition is
   * currently being processed.
   */
  protected String repeatedElement;

  /** The types of bound repetitions, i.e. {@link MetaData#boundRepetitions}. */
  protected List<Type> repetitionTypes;

  /** The flag for options. */
  protected boolean optional;

  /** The saved option flag for predicates. */
  protected boolean savedOptional;

  /** The nesting level for options. */
  protected int optionLevel;

  /**
   * The name of the variable referencing the element value for bound
   * options, or <code>null</code> if no such option is currently
   * being processed.
   */
  protected String optionalElement;

  /** The types of bound options, i.e. {@link MetaData#options}. */
  protected List<Type> optionTypes;

  /** The flag for whether an alternative creates a node value. */
  protected boolean createsNodeValue;

  /** Flag for whether a test has been emitted. */
  protected boolean seenTest;

  /** Flag for whether the current choice ends with a parse error. */
  protected boolean endsWithParseError;

  /** The iterator over the elements of a sequence. */
  protected Iterator<Element> elementIter;

  /** The name of the index variable. */
  protected String indexName;

  /** The name of the result variable. */
  protected String resultName;

  /** The name of the current binding. */
  protected String bindingName;

  /** The element being bound. */
  protected Element bindingElement;

  /** The type of the element being bound. */
  protected Type bindingType;

  /** Flag for whether we are currently emitting a predicate. */
  protected boolean predicate;

  /**
   * Flag for whether the current predicate is a not-followed-by
   * predicate.
   */
  protected boolean notFollowedBy;

  /** The predicate iterator. */
  protected Iterator<Element> predicateIter;

  // ========================================================================

  /**
   * Create a new code generator.
   *
   * @param runtime The runtime.
   * @param analyzer The analyzer.
   * @param ast The type operations.
   * @param printer The printer.
   */
  public CodeGenerator(Runtime runtime, Analyzer analyzer, AST ast, 
                       Printer printer) {
    this.runtime  = runtime;
    this.analyzer = analyzer;
    this.ast      = ast;
    this.printer  = printer;
  }

  // ========================================================================

  /**
   * Get the primitive boolean type as a string.
   *
   * @return The boolean type.
   */
  public String booleanT() {
    return "boolean";
  }

  /**
   * Get the primitive character type as a string.
   *
   * @return The character type.
   */
  public String charT() {
    return "char";
  }

  /**
   * Get the primtive integer type as a string.
   *
   * @return The integer type.
   */
  public String intT() {
    return "int";
  }

  /**
   * Get the primitive index type as a string.
   *
   * @return The index type.
   */
  public String indexT() {
    return "int";
  }

  /**
   * Extern the specified type.  If the specified type is not
   * <code>null</code>, this method returns its external
   * representation.  Otherwise, it returns <code>null</code>.
   *
   * @param type The type.
   * @return The type as a string.
   */
  public String extern(Type type) {
    return null == type ? null : ast.extern(type);
  }

  /**
   * Get the raw, non-generic type for the specified type.
   *
   * @param type The type as a string.
   * @return The raw type as a string.
   */
  public String rawT(String type) {
    final int idx = type.indexOf('<');
    return -1 == idx ? type : type.substring(0, idx);
  }

  // ========================================================================

  /**
   * Get the null expression.
   *
   * @return The null expression.
   */
  public String nullExpr() {
    return "null";
  }

  /**
   * Get a string expression.
   *
   * @param text The string's text.
   * @return The string expression.
   */
  public String stringExpr(String text) {
    return '"' + Utilities.escape(text, Utilities.JAVA_ESCAPES) + '"';
  }

  /**
   * Get the empty list expression.
   *
   * @return The empty list expression.
   */
  public String emptyListExpr() {
    return attributeRawTypes ? "Pair.EMPTY" : "Pair.empty()";
  }

  // ========================================================================

  /**
   * Generate a field name for the specified nonterminal.
   *
   * @param nt The nonterminal.
   * @param prefix The field name's prefix.
   * @return The corresponding field name.
   */
  public String fieldName(NonTerminal nt, String prefix) {
    if (chunked) {
      return COLUMN + ".chunk" + chunkMap.get(nt) + "." + prefix +
        nt.toIdentifier();
    } else {
      return COLUMN + "." + prefix + nt.toIdentifier();
    }
  }

  /**
   * Generate the method name for the specified nonterminal.
   *
   * @param nt The nonterminal.
   * @return The corresponding method name.
   */
  public String methodName(NonTerminal nt) {
    return PREFIX_METHOD + nt.toIdentifier();
  }

  // ========================================================================

  /** Emit code for verbose operation. */
  protected void verbose() {
    printer.sep().pln();

    printer.indent().pln("/**");
    printer.indent().pln(" * Trace entering the specified production.");
    printer.indent().pln(" *");
    printer.indent().pln(" * @param name The name.");
    printer.indent().pln(" * @param index The index.");
    printer.indent().pln(" */");
    printer.indent().pln("protected void traceEnter(String name, int index) {").
      incr();
    printer.indent().pln("if (! DEBUG) return;");
    printer.pln();
    printer.indent().p(PRINTER).p(".p(\"enter \").p(name).p(\" @ \").").
      pln("p(index);");
    printer.indent().p("if (PEEK) ").p(PRINTER).p(".p(\" : \\\"\").").
      pln("escape(peek(index)).p('\\\"');");
    printer.indent().p(PRINTER).pln(".pln().flush();");
    printer.decr().indent().pln('}');
    printer.pln();

    printer.indent().pln("/**");
    printer.indent().p(" * Trace a successful exit from the specified ").
      pln("production.");
    printer.indent().pln(" *");
    printer.indent().pln(" * @param name The name.");
    printer.indent().pln(" * @param index The index.");
    printer.indent().pln(" */");
    printer.indent().p("protected void traceSuccess(String name, ").
      pln("int index) {").incr();
    printer.indent().pln("if (! DEBUG) return;");
    printer.indent().p(PRINTER).p(".p(\"exit \").p(name).p(\" @ \").").
      pln("p(index).pln(\" with match\").flush();");
    printer.decr().indent().pln('}');
    printer.pln();

    printer.indent().pln("/**");
    printer.indent().p(" * Trace a failed exit from the specified ").
      pln("production.");
    printer.indent().pln(" *");
    printer.indent().pln(" * @param name The name.");
    printer.indent().pln(" * @param index The index.");
    printer.indent().pln(" */");
    printer.indent().p("protected void traceFailure(String name, ").
      pln("int index) {").incr();
    printer.indent().pln("if (! DEBUG) return;");
    printer.indent().p(PRINTER).p(".p(\"exit \").p(name).p(\" @ \").").
      pln("p(index).pln(\" with error\").flush();");
    printer.decr().indent().pln('}');
    printer.pln();

    printer.indent().pln("/**");
    printer.indent().p(" * Trace a lookup in the memoization table for ").
      pln("the specified production.");
    printer.indent().pln(" *");
    printer.indent().pln(" * @param name The name.");
    printer.indent().pln(" * @param index The index.");
    printer.indent().pln(" * @param result The result.");
    printer.indent().pln(" */");
    printer.indent().p("protected void traceLookup(String name, int index, ").
      pln("Result result) {").incr();
    printer.indent().pln("if (! DEBUG) return;");
    printer.pln();
    printer.indent().p(PRINTER).p(".p(\"lookup \").p(name).p(\" @ \").").
      pln("p(index);");
    printer.indent().p("if (PEEK) ").p(PRINTER).p(".p(\" : \\\"\").").
      pln("escape(peek(index)).p('\\\"');");
    printer.indent().p(PRINTER).pln(".p(\" -> \");");
    printer.indent().pln("if (result.hasValue()) {").incr();
    printer.indent().p(PRINTER).pln(".p(\"match\");");
    printer.decr().indent().pln("} else {").incr();
    printer.indent().p(PRINTER).pln(".p(\"error\");");
    printer.decr().indent().pln('}');
    printer.indent().p(PRINTER).pln(".pln().flush();");
    printer.decr().indent().pln('}');
    printer.pln();
  }

  // ========================================================================

  /** Emit code for printing the memoization profile. */
  protected void profile() {
    // Emit the method header.
    printer.sep().pln();

    printer.indent().pln("/**");
    printer.indent().pln(" * Print a profile of the memoization table.");
    printer.indent().pln(" *");
    printer.indent().p(" * @param printer The printer for writing the ").
      pln("profile.");
    printer.indent().pln(" */");
    printer.indent().pln("public void profile(Printer printer) {").incr();

    // Emit the profile initialization code.
    printer.indent().pln("// Initialize the profile.");
    if (attributeRawTypes) {
      printer.indent().p("HashMap maxima = new HashMap();");
    } else {
      printer.indent().p("HashMap<String, Integer> maxima = ").
        pln("new HashMap<String, Integer>();");
    }
    printer.pln();

    int maxNameSize = 0;
    for (Production p : analyzer.module().productions) {
      if (runtime.test("optimizeTransient") && ! p.isMemoized()) continue;

      final String name = p.name.toIdentifier();
      maxNameSize = Math.max(maxNameSize, name.length());

      if (attributeRawTypes) {
        printer.indent().p("maxima.put(\"").p(name).
          pln("\", Integer.valueOf(0));");
      } else {
        printer.indent().p("maxima.put(\"").p(name).pln("\", 0);");
      }
    }
    printer.pln();

    // Emit the code to process the memoization table.
    printer.indent().pln("// Process the memoization table.");
    printer.indent().pln("for (int i=0; i<yyCount; i++) {").incr();
    printer.indent().p(className).p("Column column = (").p(className).
      pln("Column)yyColumns[i];");
    printer.pln();
    printer.indent().pln("if (null != column) {").incr();

    if (0 == chunkCount) {
      for (Production p : analyzer.module().productions) {
        if ((! runtime.test("optimizeTransient")) || p.isMemoized()) {
          final String name = p.name.toIdentifier();

          printer.indent().p("profile(maxima, \"").p(name).p("\", ").buffer().
            p("column.").p(PREFIX_COUNT_FIELD).p(name).p(");").
            fit("        ").pln();
        }
      }

    } else {
      int     number = 0;
      int     idx    = CHUNK_SIZE;
      boolean first  = true;

      for (Production p : analyzer.module().productions) {
        if (runtime.test("optimizeTransient") && ! p.isMemoized()) continue;

        if (CHUNK_SIZE <= idx) {
          number++;
          idx = 0;

          if (first) {
            first = false;
          } else {
            printer.decr().indent().pln('}');
            printer.pln();
          }

          printer.indent().p("Chunk").p(number).p(" chunk").p(number).
            p(" = column.chunk").p(number).pln(';');
          printer.indent().p("if (null != chunk").p(number).pln(") {").incr();
        }

        final String name = p.name.toIdentifier();
        printer.indent().p("profile(maxima, \"").p(name).p("\", ").buffer().
          p("chunk").p(number).p('.').p(PREFIX_COUNT_FIELD).p(name).p(");").
          fit("        ").pln();
        idx++;
      }

      printer.decr().indent().pln('}');
    }

    printer.decr().indent().pln('}');
    printer.decr().indent().pln('}');
    printer.pln();

    // Emit the code to print the profile.
    printer.indent().pln("// Print the profile.");

    for (Production p : analyzer.module().productions) {
      if (runtime.test("optimizeTransient") && ! p.isMemoized()) continue;

      printer.indent().p("print(printer, ").p(maxNameSize).p(", maxima, ").
        buffer().p('"').p(p.name.toIdentifier()).p("\");").fitMore().pln();
    }
    printer.decr().indent().pln('}');
    printer.pln();

    // Emit the code for the profile helper method.
    printer.indent().pln("/**");
    printer.indent().p(" * Update the profile for the specified production").
      pln(" and count.");
    printer.indent().pln(" *");
    printer.indent().pln(" * @param maxima The profile.");
    printer.indent().pln(" * @param name The production's name.");
    printer.indent().pln(" * @param count The access count.");
    printer.indent().pln(" */");
    if (attributeRawTypes) {
      printer.indent().p("private void profile(HashMap maxima, String name, ").
        pln("int count) {").incr();
      printer.indent().pln("int old = ((Integer)maxima.get(name)).intValue();");
      printer.indent().pln("int max = Math.max(old, count);");
      printer.indent().p("if (old < max) ").
        pln("maxima.put(name, Integer.valueOf(max));");
    } else {
      printer.indent().p("private void profile(HashMap<String, Integer> ").
        pln("maxima,");
      printer.indent().pln("                     String name, int count) {").
        incr();
      printer.indent().pln("int old = maxima.get(name);");
      printer.indent().pln("int max = Math.max(old, count);");
      printer.indent().pln("if (old < max) maxima.put(name, max);");
    }      
    printer.decr().indent().pln('}');
    printer.pln();

    // Emit the code for the print helper method.
    printer.indent().pln("/**");
    printer.indent().p(" * Print the profile for the specified production").
      pln(" and count.");
    printer.indent().pln(" *");
    printer.indent().pln(" * @param printer The printer.");
    printer.indent().pln(" * @param align The alignment.");
    printer.indent().pln(" * @param maxima The profile.");
    printer.indent().pln(" * @param name The production's name.");
    printer.indent().pln(" */");
    if (attributeRawTypes) {
      printer.indent().pln("private void print(Printer printer, int align,");
      printer.indent().pln("                   HashMap maxima, String name) {").
        incr();
      printer.indent().p("int count = ((Integer)maxima.get(name)).").
        pln("intValue();"); 
    } else {
      printer.indent().pln("private void print(Printer printer, int align,");
      printer.indent().p("                   HashMap<String, Integer> ").
        pln("maxima, String name) {").incr();
      printer.indent().pln("int count = maxima.get(name);");
    }
    printer.indent().pln("align    += 4;"); 
    printer.indent().pln("if (1 != count) {").incr();
    printer.indent().pln("printer.p(\"- \");");
    printer.decr().indent().pln("} else {").incr();
    printer.indent().pln("printer.p(\"* \");");
    printer.decr().indent().pln('}');
    printer.indent().pln("printer.p(name).align(align).p(\" : \").pln(count);");
    printer.decr().indent().pln('}');
    printer.pln();
  }

  // ========================================================================

  /** Emit code for dumping the memoization table. */
  protected void dump() {
    printer.sep().pln();

    printer.indent().pln("/**");
    printer.indent().pln(" * Dump the memoization table.");
    printer.indent().pln(" *");
    printer.indent().pln(" * @param printer The printer for writing the table.");
    printer.indent().pln(" */");
    printer.indent().pln("public void dump(Printer printer) {").incr();
    printer.indent().pln("for (int i=0; i<yyCount; i++) {").incr();
    printer.indent().p(className).p("Column column = (").p(className).
      pln("Column)yyColumns[i];");
    printer.indent().pln("printer.indent().p(i).p(\" = \");");
    printer.pln();

    printer.indent().pln("if (null == column) {").incr();
    printer.indent().pln("printer.pln(\"null;\");");
    printer.pln();

    printer.decr().indent().pln("} else {").incr();
    printer.indent().pln("printer.pln('{').incr();");
    
    if (0 == chunkCount) {
      printer.pln();

      for (Production p : analyzer.module().productions) {
        if ((! runtime.test("optimizeTransient")) || p.isMemoized()) {
          final String name = p.name.toIdentifier();

          printer.indent().p("dump(printer, \"").p(name).p("\", ").
            buffer().p("column.").p(PREFIX_FIELD).p(name).p(");").
            fit("     ").pln();
        }
      }
      
    } else {
      int      number = 0;
      int      idx    = CHUNK_SIZE;
      boolean  first  = true;

      for (Production p : analyzer.module().productions) {
        if (runtime.test("optimizeTransient") && ! p.isMemoized()) continue;

        if (CHUNK_SIZE <= idx) {
          number++;
          idx = 0;

          if (first) {
            first = false;
          } else {
            printer.pln();
            printer.indent().pln("printer.decr().indent().pln(\"};\");");
            printer.decr().indent().pln('}');
          }

          printer.pln();
          printer.indent().p("Chunk").p(number).p(" chunk").p(number).
            p(" = column.chunk").p(number).pln(';');
          printer.indent().p("printer.indent().p(\"Chunk(").p(number).
            p(") = \");");
          printer.pln();

          printer.indent().p("if (null == chunk").p(number).pln(") {").incr();
          printer.indent().pln("printer.pln(\"null;\");");
          printer.pln();

          printer.decr().indent().pln("} else {").incr();
          printer.indent().pln("printer.pln('{').incr();");
          printer.pln();
        }

        final String name = p.name.toIdentifier();
        printer.indent().p("dump(printer, \"").p(name).p("\", ").buffer().
          p("chunk").p(number).p('.').p(PREFIX_FIELD).p(name).p(");").
          fit("     ").pln();
        idx++;
      }

      printer.pln();
      printer.indent().pln("printer.decr().indent().pln(\"};\");");
      printer.decr().indent().pln('}');
    }

    printer.pln();
    printer.indent().pln("printer.decr().indent().pln(\"};\");");
    printer.decr().indent().pln('}');
    printer.decr().indent().pln('}');
    printer.decr().indent().pln('}');
    printer.pln();

    printer.indent().pln("/**");
    printer.indent().pln(" * Dump a memoized result.");
    printer.indent().pln(" *");
    printer.indent().pln(" * @param printer The printer.");
    printer.indent().pln(" * @param name The name of the result.");
    printer.indent().pln(" * @param result The value of the result.");
    printer.indent().pln(" */");
    printer.indent().p("private void dump(Printer printer, String name, ").
      pln("Result result) {").incr();
    printer.indent().pln("printer.indent().p(name).p(\" = \");");
    printer.pln();

    printer.indent().pln("if (null == result) {").incr();
    printer.indent().pln("printer.pln(\"null;\");");
    printer.decr().indent().pln("} else if (result.hasValue()) {").incr();
    printer.indent().pln("printer.p(\"Value(\").p(result.index).pln(\");\");");
    printer.decr().indent().pln("} else {").incr();
    printer.indent().pln("printer.p(\"Error(\").p(result.index).pln(\");\");");
    printer.decr().indent().pln('}');
    printer.decr().indent().pln('}');
    printer.pln();
  }

  // ========================================================================

  /**
   * Emit code for a static main method.
   *
   * @param nt The name of the top-level nonterminal to parse.
   */
  protected void mainMethod(String nt) {
    final int align = (printer.level() * Constants.INDENTATION) +
      (4 * Constants.INDENTATION) + Math.max(6, className.length()) +
      1 + Constants.FIRST_COLUMN;

    printer.sep().pln();

    printer.indent().pln("/**");
    printer.indent().pln(" * Parse the specified files.");
    printer.indent().pln(" *");
    printer.indent().pln(" * @param args The file names.");
    printer.indent().pln(" */");
    printer.indent().pln("public static void main(String[] args) {").incr();
    printer.indent().pln("if ((null == args) || (0 == args.length)) {").incr();
    printer.indent().pln("System.err.println(\"Usage: <file-name>+\");");

    printer.pln();
    printer.decr().indent().pln("} else {").incr();
    printer.indent().pln("for (int i=0; i<args.length; i++) {").incr();
    printer.indent().p("System.err.println(\"Processing \" + args[i] + ").
      pln("\" ...\");");

    printer.pln();
    printer.indent().p("Reader").align(align).pln("in = null;");
    printer.indent().pln("try {").incr();
    printer.indent().p("in").align(align + 3).
      pln("= new BufferedReader(new FileReader(args[i]));");
    printer.indent().p(className).align(align).p("p  = ").
      buffer().p("new ").p(className).
      p("(in, args[i], (int)new File(args[i]).length());").fitMore().pln();
    printer.indent().p("Result").align(align).p("r  = p.p").p(nt).pln("(0);");

    printer.pln();
    printer.indent().pln("if (r.hasValue()) {").incr();
    printer.indent().pln("SemanticValue v = (SemanticValue)r;");

    printer.pln();
    if (attributePrinter) {
      printer.indent().pln("if (v.value instanceof Node) {").incr();
      printer.indent().pln("Printer ptr = new");
      printer.indentMore().p("Printer(new BufferedWriter(new ").
        pln("OutputStreamWriter(System.out)));");
      printer.indent().p("new ").p(printerClassName).
        pln("(ptr).dispatch((Node)v.value);");
      printer.indent().pln("ptr.flush();").pln();
      printer.decr().indent().pln("} else {").incr();
      printer.indent().pln("System.out.println(v.value.toString());");
      printer.decr().indent().pln('}');

    } else {
      printer.indent().pln("if (v.value instanceof Node) {").incr();
      printer.indent().pln("Printer ptr = new");
      printer.indentMore().p("Printer(new BufferedWriter(new ").
        pln("OutputStreamWriter(System.out)));");
      printer.indent().pln("ptr.format((Node)v.value).pln().flush();");
      printer.decr().indent().pln("} else {").incr();
      printer.indent().pln("System.out.println(v.value.toString());");
      printer.decr().indent().pln('}');
    }

    printer.pln();
    printer.decr().indent().pln("} else {").incr();
    printer.indent().pln("ParseError err = (ParseError)r;");
    printer.indent().pln("if (-1 == err.index) {").incr();
    printer.indent().pln("System.err.println(\"  Parse error\");");
    printer.decr().indent().pln("} else {").incr();
    printer.indent().p("System.err.println(\"  \" + p.location(err.index) + ").
      pln("\": \" + err.msg);");
    printer.decr().indent().pln("}");
    printer.decr().indent().pln("}");

    printer.pln();
    printer.decr().indent().pln("} catch (Throwable x) {").incr();
    printer.indent().pln("while (null != x.getCause()) {").incr();
    printer.indent().pln("x = x.getCause();");
    printer.decr().indent().pln("}");
    printer.indent().pln("x.printStackTrace();");
    printer.decr().indent().pln("} finally {").incr();
    printer.indent().pln("try {").incr();
    printer.indent().pln("in.close();");
    printer.decr().indent().pln("} catch (Throwable x) {").incr();
    printer.indent().pln("/* Ignore. */");
    printer.decr().indent().pln('}');
    printer.decr().indent().pln('}');
    printer.decr().indent().pln('}');
    printer.decr().indent().pln('}');
    printer.decr().indent().pln('}');
    printer.pln();
  }

  // ========================================================================

  /** Generate code for the specified grammar. */
  public void visit(Module m) {
    // (Re)Initialize code generator state.
    analyzer.register(this);
    printer.register(this);
    analyzer.init(m);
    className = Utilities.getName(m.getClassName());

    // Record the grammar attributes.
    if (null == m.attributes) {
      m.attributes = new ArrayList<Attribute>();
    }

    attributeVerbose      = m.hasAttribute(Constants.ATT_VERBOSE);
    attributeWithLocation = m.hasAttribute(Constants.ATT_WITH_LOCATION);
    attributeConstant     = m.hasAttribute(Constants.ATT_CONSTANT);
    attributeFlatten      = m.hasAttribute(Constants.ATT_FLATTEN);
    attributeParseTree    = m.hasAttribute(Constants.ATT_PARSE_TREE);
    attributeRawTypes     = m.hasAttribute(Constants.ATT_RAW_TYPES);
    attributeIgnoringCase = m.hasAttribute(Constants.ATT_IGNORING_CASE);
    attributeStateful     = m.hasAttribute(Constants.ATT_STATEFUL.getName());
    attributeStringSet    = m.hasAttribute(Constants.NAME_STRING_SET);
    attributeMain         = m.hasAttribute(Constants.NAME_MAIN);
    attributePrinter      = m.hasAttribute(Constants.NAME_PRINTER);
    attributeProfile      = m.hasAttribute(Constants.ATT_PROFILE);
    attributeDump         = m.hasAttribute(Constants.ATT_DUMP);

    if (attributeStateful) {
      stateClassName =
        (String)m.getAttributeValue(Constants.ATT_STATEFUL.getName());
    }
    if (attributeMain) {
      mainMethodNonterminal =
        (String)m.getAttributeValue(Constants.NAME_MAIN);
    }
    if (attributePrinter) {
      printerClassName =
        (String)m.getAttributeValue(Constants.NAME_PRINTER);
    }
    if (m.hasAttribute(Constants.NAME_FACTORY)) {
      factoryClassName = (String)m.getAttributeValue(Constants.NAME_FACTORY);
    }

    boolean isVerbose = attributeVerbose;
    if (! isVerbose) {
      // Scan productions for verbose attribute.
      for (Production p : m.productions) {
        if (p.hasAttribute(Constants.ATT_VERBOSE)) {
          isVerbose = true;
          break;
        }
      }
    }

    chunked    = false;
    chunkMap   = null;
    chunkCount = 0;

    // Emit package name.
    final String packageName = Utilities.getQualifier(m.getClassName());
    if (null != packageName) {
      printer.indent().p("package ").p(packageName).pln(';');
      printer.pln();
    }

    // Emit imports.
    printer.indent().pln("import java.io.Reader;");
    if (attributeMain) {
      printer.indent().pln("import java.io.BufferedReader;");
      printer.indent().pln("import java.io.BufferedWriter;");
      printer.indent().pln("import java.io.File;");
      printer.indent().pln("import java.io.FileReader;");
      printer.indent().pln("import java.io.OutputStreamWriter;");
    }
    printer.indent().pln("import java.io.IOException;");
    printer.pln();

    if (attributeProfile) {
      printer.indent().pln("import java.util.HashMap;");
    }
    if (attributeStringSet) {
      printer.indent().pln("import java.util.HashSet;");
      printer.indent().pln("import java.util.Set;");
    }
    if (attributeProfile || attributeStringSet) {
      printer.pln();
    }

    if (m.getBooleanProperty(Properties.RECURSIVE)) {
      printer.indent().pln("import xtc.util.Action;");
    }
    printer.indent().pln("import xtc.util.Pair;");
    printer.pln();

    boolean needsNewline = false;
    if (m.getBooleanProperty(Properties.LOCATABLE)) {
      printer.indent().pln("import xtc.tree.Locatable;");
      needsNewline = true;
    }
    if (m.getBooleanProperty(Properties.GENERIC) ||
        attributeMain) {
      printer.indent().pln("import xtc.tree.Node;");
      needsNewline = true;
    }
    if (m.getBooleanProperty(Properties.GENERIC)) {
      if (null == factoryClassName) {
        printer.indent().pln("import xtc.tree.GNode;");
      } else if (Utilities.isQualified(factoryClassName)) {
        printer.indent().p("import ").p(factoryClassName).pln(';');
        factoryClassName = Utilities.getName(factoryClassName);
      }
      needsNewline = true;
    }
    if (attributeParseTree) {
      printer.indent().pln("import xtc.tree.Token;");
      printer.indent().pln("import xtc.tree.Formatting;");
      needsNewline = true;
    }
    if (isVerbose || attributeMain || attributeProfile || attributeDump) {
      printer.indent().pln("import xtc.tree.Printer;");
      needsNewline = true;
    }
    if (attributePrinter) {
      printer.indent().pln("import xtc.tree.Visitor;");
      needsNewline = true;
    }
    if (needsNewline) printer.pln();

    printer.indent().pln("import xtc.parser.ParserBase;");
    printer.indent().pln("import xtc.parser.Column;");
    printer.indent().pln("import xtc.parser.Result;");
    printer.indent().pln("import xtc.parser.SemanticValue;");
    printer.indent().pln("import xtc.parser.ParseError;");
    printer.pln();

    // Emit header.
    if (null != m.header) {
      action(m.header);
      printer.pln();
    }

    // Emit class name.
    printer.indent().pln("/**");
    printer.indent().p(" * Packrat parser for grammar <code>").
      p(m.name.name).pln("</code>.");
    printer.indent().pln(" *");
    printer.indent().p(" * <p />This class has been generated by the ").
      pln("<i>Rats!</i> parser");
    printer.indent().p(" * generator, version ").p(Constants.VERSION).
      p(", ").p(Constants.COPY).pln('.');
    printer.indent().pln(" */");

    if (attributeRawTypes) {
      printer.indent().pln("@SuppressWarnings(\"unchecked\")");
    }

    printer.indent();
    if (m.hasAttribute(Constants.NAME_VISIBILITY)) {
      String visible = (String)m.getAttributeValue(Constants.NAME_VISIBILITY);
      if (Constants.ATT_PUBLIC.getValue().equals(visible)) {
        printer.p("public ");
      }
    } else {
      printer.p("public ");
    }
    printer.p("final class ").p(className).pln(" extends ParserBase {").
      incr().pln();

    // Emit debug flag.
    if (isVerbose) {
      printer.indent().
        p("/** Flag for whether to emit tracing information while ").
        pln("parsing. */");
      printer.indent().pln("public static final boolean DEBUG = true;");
      printer.pln();
      printer.indent().
        pln("/** Flag for whether to emit a peek into the input. */");
      printer.indent().pln("public static final boolean PEEK = true;");
      printer.pln();
    }

    // Emit any sets and flags.
    if (m.hasAttribute(Constants.NAME_STRING_SET) ||
        m.hasAttribute(Constants.NAME_FLAG)) {
      for (Attribute att : m.attributes) {
        if (att.getName().equals(Constants.NAME_STRING_SET)) {
          String  set  = (String)att.getValue();
          printer.indent().p("/** The ").p(set).pln(" set. */");
          if (attributeRawTypes) {
            printer.indent().p("public static final Set ").p(set).
              pln(" = new HashSet();");
          } else {
            printer.indent().p("public static final Set<String> ").p(set).
              pln(" = new HashSet<String>();");
          }
          printer.pln();

        } else if (att.getName().equals(Constants.NAME_FLAG)) {
          String  flag = (String)att.getValue();
          printer.indent().p("/** The ").p(flag).pln(" flag. */");
          printer.indent().p("public static final boolean ").p(flag).
            pln(" = true;");
          printer.pln();
        }
      }
    }
    
    // Determine the number of productions that require memoization.
    int memoCount = 0;

    for (Production p : m.productions) {
      if ((! runtime.test("optimizeTransient")) || p.isMemoized()) {
        memoCount++;
      }
    }

    // To chunk or not to chunk.
    if (runtime.test("optimizeChunks") && (CHUNK_SIZE <= memoCount)) {
      chunked    = true;
      chunkMap   = new HashMap<NonTerminal, Integer>(memoCount * 4 / 3);

      Integer number  = null;
      String  sNumber = null;
      int     i       = CHUNK_SIZE;
      boolean first   = true;

      for (Production p : m.productions) {
        // Skip memoization for productions that are transient.
        if (runtime.test("optimizeTransient") && ! p.isMemoized()) continue;

        if (CHUNK_SIZE <= i) {
          chunkCount++;
          number  = new Integer(chunkCount);
          sNumber = Integer.toString(chunkCount);
          i       = 0;

          if (first) {
            first = false;
            printer.sep();
          } else {
            printer.decr().indent().pln('}');
          }
          printer.pln();
          printer.indent().p("/** Chunk ").p(sNumber).
            pln(" of memoized results. */");
          printer.indent().p("static final class Chunk").p(sNumber).
            pln(" {").incr();
        }

        final NonTerminal nt = p.name;
        chunkMap.put(nt, number);
        i++;

        printer.indent().p("Result ").p(PREFIX_FIELD).p(nt.toIdentifier()).
          pln(';');
        if (attributeProfile) {
          printer.indent().p("int    ").p(PREFIX_COUNT_FIELD).
            p(nt.toIdentifier()).pln(';');
        }
      }

      printer.decr().indent().pln('}');
      printer.pln();
    }

    // Emit column.
    printer.sep().pln();
    printer.indent().pln("/** Memoization table column. */");
    printer.indent().p("static final class ").p(className).
      pln("Column extends Column {").incr();

    if (chunked) {
      for (int i=1; i<=chunkCount; i++) {
        printer.indent().p("Chunk").p(i).p(' ').p("chunk").
          p(i).pln(';');
      }

    } else {
      for (Production p : m.productions) {
        if ((! runtime.test("optimizeTransient")) || p.isMemoized()) {
          printer.indent().p("Result ").p(PREFIX_FIELD).
            p(p.name.toIdentifier()).pln(';');
          if (attributeProfile) {
            printer.indent().p("int    ").p(PREFIX_COUNT_FIELD).
              p(p.name.toIdentifier()).pln(';');
          }
        }
      }
    }

    printer.decr().indent().pln('}');
    printer.pln();

    // Emit global state field and printer field.
    if (attributeStateful || isVerbose) {
      printer.sep().pln();

      if (attributeStateful) {
        printer.indent().pln("/** The global state object. */");
        printer.indent().p("protected final ").p(stateClassName).p(' ').
          p(STATE).pln(';');
        printer.pln();
      }

      if (isVerbose) {
        printer.indent().pln("/** The printer for tracing this parser. */");
        printer.indent().p("protected final Printer ").p(PRINTER).pln(';');
        printer.pln();
      }
    }

    // Emit constructors.
    printer.sep().pln();

    printer.indent().pln("/**");
    printer.indent().pln(" * Create a new packrat parser.");
    printer.indent().pln(" *");
    printer.indent().pln(" * @param reader The reader.");
    printer.indent().pln(" * @param file The file name.");
    printer.indent().pln(" */");
    printer.indent().p("public ").p(className).
      pln("(final Reader reader, final String file) {").incr();
    printer.indent().pln("super(reader, file);");
    if (attributeStateful) {
      printer.indent().p(STATE).p(" = new ").p(stateClassName).pln("();");
    }
    if (isVerbose) {
      printer.indent().p(PRINTER).pln(" = new Printer(System.out);");
    }
    printer.decr().indent().pln('}');
    printer.pln();

    printer.indent().pln("/**");
    printer.indent().pln(" * Create a new packrat parser.");
    printer.indent().pln(" *");
    printer.indent().pln(" * @param reader The file reader.");
    printer.indent().pln(" * @param file The file name.");
    printer.indent().pln(" * @param size The file size.");
    printer.indent().pln(" */");
    printer.indent().p("public ").p(className).
      pln("(final Reader reader, final String file, final int size) {").incr();
    printer.indent().pln("super(reader, file, size);");
    if (attributeStateful) {
      printer.indent().p(STATE).p(" = new ").p(stateClassName).pln("();");
    }
    if (isVerbose) {
      printer.indent().p(PRINTER).pln(" = new Printer(System.out);");
    }
    printer.decr().indent().pln('}');
    printer.pln();

    // Emit code for creating a column.
    printer.sep().pln();
    printer.indent().pln("protected Column newColumn() {").incr();
    printer.indent().p("return new ").p(className).pln("Column();");
    printer.decr().indent().pln('}');
    printer.pln();
    
    // Emit code for productions.
    for (Production p : m.productions) {
      boolean    savedVerbose     = attributeVerbose;
      boolean    savedLocation    = attributeWithLocation;
      boolean    savedConstant    = attributeConstant;
      boolean    savedCase        = attributeIgnoringCase;

      if ((! savedVerbose) && p.hasAttribute(Constants.ATT_VERBOSE)) {
        attributeVerbose          = true;
      }
      if ((! savedLocation) && p.hasAttribute(Constants.ATT_WITH_LOCATION)) {
        attributeWithLocation     = true;
      }
      if ((! savedConstant) && p.hasAttribute(Constants.ATT_CONSTANT)) {
        attributeConstant         = true;
      }
      if ((! savedCase) && p.hasAttribute(Constants.ATT_IGNORING_CASE)) {
        attributeIgnoringCase     = true;
      }

      analyzer.process(p);

      attributeIgnoringCase       = savedCase;
      attributeConstant           = savedConstant;
      attributeWithLocation       = savedLocation;
      attributeVerbose            = savedVerbose;
    }

    // Emit code for body.
    if (null != m.body) {
      printer.sep().pln();
      action(m.body);
      printer.pln();
    }

    // Emit code for toText method.
    if (m.getBooleanProperty(Properties.GENERIC) ||
        m.hasAttribute(Constants.ATT_GENERIC_AS_VOID)) {
      printer.sep().pln();

      if (attributeParseTree) {
        printer.indent().pln("/**");
        printer.indent().p(" * Get the text for the specified annotated ").
          pln("token.");
        printer.indent().pln(" *");
        printer.indent().pln(" * @param n The annotated token.");
        printer.indent().pln(" * @return The corresponding text.");
        printer.indent().pln(" */");
        printer.indent().pln("protected static final String toText(Node n) {").
          incr();
        printer.indent().pln("return n.getTokenText();");
        printer.decr().indent().pln('}');

      } else {
        printer.indent().pln("/**");
        printer.indent().pln(" * Get the specified text.");
        printer.indent().pln(" *");
        printer.indent().pln(" * @param s The text.");
        printer.indent().pln(" * @return The text.");
        printer.indent().pln(" */");
        printer.indent().pln("protected static final String toText(String s) {").
          incr();
        printer.indent().pln("return s;");
        printer.decr().indent().pln('}');
      }

      printer.pln();
    }

    // Emit code for add method.
    if (attributeStringSet) {
      printer.sep().pln();

      printer.indent().pln("/**");
      printer.indent().pln(" * Add the specified values to the specified set.");
      printer.indent().pln(" *");
      printer.indent().pln(" * @param set The set.");
      printer.indent().pln(" * @param values The new values.");
      printer.indent().pln(" */");
      if (attributeRawTypes) {
        printer.indent().p("protected static final ").
          pln("void add(Set set, Object[] values) {").incr();
        printer.indent().pln("for (int i=0; i<values.length; i++) {").incr();
        printer.indent().pln("set.add(values[i]);");
        printer.decr().indent().pln('}');
        printer.decr().indent().pln('}');
      } else {
        printer.indent().p("protected static final ").
          pln("<T> void add(Set<T> set, T[] values) {").incr();
        printer.indent().pln("for (T v : values) set.add(v);");
        printer.decr().indent().pln('}');
      }
      printer.pln();

      printer.indent().pln("/**");
      printer.indent().
        pln(" * Check whether the specified set contains the specified value.");
      printer.indent().pln(" *");
      printer.indent().pln(" * @param set The set.");
      printer.indent().pln(" * @param value The value.");
      printer.indent().
        pln(" * @return <code>true</code> if the set contains the value.");
      printer.indent().pln(" */");
      if (attributeRawTypes) {
        printer.indent().p("protected static final ").
          pln("boolean contains(Set set, Object value) {").incr();
        printer.indent().pln("return set.contains(value);");
        printer.decr().indent().pln('}');
      } else {
        printer.indent().p("protected static final ").
          pln("<T> boolean contains(Set<T> set, T value) {").incr();
        printer.indent().pln("return set.contains(value);");
        printer.decr().indent().pln('}');
      }
      printer.pln();
    }

    // Emit code for verbose operation.
    if (attributeVerbose) {
      verbose();
    }

    // Emit code for profiling the memoization table.
    if (attributeProfile) {
      profile();
    }

    // Emit code for dumping the memoization table.
    if (attributeDump) {
      dump();
    }

    // Emit code for main method.
    if (attributeMain) {
      mainMethod(mainMethodNonterminal);
    }

    // Finish parser class.
    printer.decr().indent().pln('}');

    // Emit footer.
    if (null != m.footer) {
      printer.pln().sep().pln();
      action(m.footer);
    }
  }

  // ========================================================================

  /** Generate code for the specified production. */
  public void visit(FullProduction p) {
    MetaData md     = (MetaData)p.getProperty(Properties.META_DATA);
    repetitionTypes = md.boundRepetitions;
    optionTypes     = md.options;
    String   field  = fieldName(p.name, PREFIX_FIELD);
    String   method = methodName(p.name);

    printer.sep().pln();
    printer.indent().pln("/**");
    printer.indent().p(" * Parse ");
    if (p.getBooleanProperty(Constants.SYNTHETIC)) {
      printer.p("synthetic ");
    }
    printer.p("nonterminal ").buffer().p(p.qName.name).p('.').fit(" * ").pln();
    if (p.hasProperty(Properties.DUPLICATES)) {
      printer.indent();
      printer.p(" * This nonterminal represents the duplicate productions ");

      List<String> src = Properties.getDuplicates(p);
      for (Iterator<String> iter = src.iterator(); iter.hasNext(); ) {
        String name = iter.next();

        printer.buffer();
        if ((1 < src.size()) && (! iter.hasNext())) {
          printer.p("and ");
        }
        printer.p(name);
        if ((2 == src.size()) && (iter.hasNext())) {
          printer.p(' ');
        } else if (iter.hasNext()) {
          printer.p(", ");
        } else {
          printer.p('.');
        }
        printer.fit(" * ");
      }
      printer.pln();
    }
    printer.indent().pln(" *");
    printer.indent().p(" * @param ").p(ARG_INDEX).pln(" The index.");
    printer.indent().pln(" * @return The result.");
    printer.indent().pln(" * @throws IOException Signals an I/O error.");
    printer.indent().pln(" */");
    printer.indent();
    if (p.hasAttribute(Constants.ATT_PUBLIC)) {
      // Top-level parsing methods are public.
      printer.p("public");
    } else {
      // The rest is private.
      printer.p("private");
    }
    long line = printer.line();
    printer.p(" Result ").p(method).p("(final ").p(indexT()).p(' ').
      p(ARG_INDEX).p(") ").buffer().p("throws IOException {").fitMore().pln().
      incr();
    if (line + 1 < printer.line()) {
      printer.pln();
    }

    // Only memoize non-transient productions.
    if ((! runtime.test("optimizeTransient")) || p.isMemoized()) {
      printer.indent().p(className).p("Column ").p(COLUMN).p(" = (").
        p(className).p("Column)column(").p(ARG_INDEX).pln(");");

      if (chunked) {
        String chunk = chunkMap.get(p.name).toString();
        printer.indent().p("if (").p(nullExpr()).p(" == ").p(COLUMN).
          p(".chunk").p(chunk).p(") ").p(COLUMN).p(".chunk").p(chunk).
          p(" = new Chunk").p(chunk).pln("();");
      }

      printer.indent().p("if (").p(nullExpr()).p(" == ").p(field).p(") ").
        buffer().p(field).p(" = ").p(method).p("$1(").p(ARG_INDEX).p(");").
        fitMore().pln();

      if (attributeProfile) {
        printer.indent().p(fieldName(p.name, PREFIX_COUNT_FIELD)).pln("++;");
      }

      if (attributeVerbose) {
        printer.indent().p("traceLookup(\"").p(p.name.toIdentifier()).
          p("\", ").p(ARG_INDEX).p(", ").
          buffer().p(field).p(");").fitMore().pln();
      }

      printer.indent().p("return ").p(field).pln(';');
      printer.decr().indent().pln('}');

      printer.pln();

      printer.indent().p("/** Actually parse ");
      printer.p(p.qName.name).pln(". */");
      line = printer.line();
      printer.indent().p("private Result ").p(method).
        p("$1(final ").p(indexT()).p(' ').p(ARG_INDEX).p(") ").buffer().
        p("throws IOException {").fitMore().pln().incr();
      if (line + 1 < printer.line()) {
        printer.pln();
      }
    }

    // Emit variable declarations.  First, determine the alignment for
    // the variable names by finding the maximum number of characters
    // in a type name.  Second, print the individual declarations.
    String ptype = extern(p.type);
    if (attributeRawTypes) ptype = rawT(ptype);

    int w = Math.max("ParseError".length(), ptype.length());

    if (! attributeRawTypes) {
      for (Type t : repetitionTypes) {
        if (null != t) {
          w = Math.max(w, extern(t).length());
        }
      }
    }

    for (Type t : optionTypes) {
      if (null != t) {
        String s = extern(t);
        if (attributeRawTypes) s = rawT(s);
        w = Math.max(w, s.length());
      }
    }

    int align = (printer.level() * Constants.INDENTATION) + 
      w + 1 + Constants.FIRST_COLUMN;

    if (md.requiresChar) {
      printer.indent().p(intT()).align(align).p(CHAR).pln(';');
    }
    if (md.requiresIndex) {
      printer.indent().p(indexT()).align(align).p(INDEX).pln(';');
    }
    if (md.requiresResult) {
      printer.indent().p("Result").align(align).p(RESULT).pln(';');
    }
    if (md.requiresPredIndex) {
      printer.indent().p(indexT()).align(align).p(PRED_INDEX).pln(';');
    }
    if (md.requiresPredResult) {
      printer.indent().p("Result").align(align).p(PRED_RESULT).pln(';');
    }
    if (md.requiresPredMatch) {
      printer.indent().p(booleanT()).align(align).p(PRED_MATCHED).
        pln(';');
    }
    if (md.requiresBaseIndex) {
      printer.indent().p(indexT()).align(align).p(BASE_INDEX).pln(';');
    }
    for (int i=0; i<md.repetitions.size(); i++) {
      printer.indent().p(indexT()).align(align).p(REPETITION).p(i+1).pln(';');
      if (md.repetitions.get(i)) {
        printer.indent().p(booleanT()).align(align).p(REPEATED).p(i+1).pln(';');
      }
      if (null != repetitionTypes.get(i)) {
        printer.indent();
        if (attributeRawTypes) {
          printer.p(rawT(extern(new InstantiatedT(AST.ANY, AST.LIST))));
        } else {
          printer.p(extern(repetitionTypes.get(i)));
        }
        printer.align(align).p(REP_VALUE).p(i+1).pln(';');
      }
    }
    for (int i=0; i<md.options.size(); i++) {
      printer.indent().p(indexT()).align(align).p(OPTION).p(i+1).
        pln(';');

      Type t = md.options.get(i);
      if (null != t) {
        String s = extern(t);
        if (attributeRawTypes) s = rawT(s);
        printer.indent().p(s).align(align).p(OP_VALUE).p(i+1).pln(';');
      }
    }
    printer.indent().p(ptype).align(align).p(VALUE).pln(';');
    printer.indent().p("ParseError").align(align).p(PARSE_ERROR).
      pln(" = ParseError.DUMMY;");

    // Emit code for verbose operation.
    if (attributeVerbose) {
      printer.pln();
      printer.indent().p("traceEnter(\"").p(p.name.toIdentifier()).
        p("\", ").p(ARG_INDEX).pln(");");
    }

    // Emit code for state management.
    if (attributeStateful) {
      if (p.hasAttribute(Constants.ATT_RESETTING)) {
        printer.pln();
        printer.indent().pln("// Reset the global state object.");
        printer.indent().p(STATE).p(".reset(column(").p(ARG_INDEX).
          pln(").file);");
      }

      if (p.hasAttribute(Constants.ATT_STATEFUL)) {
        printer.pln();
        printer.indent().pln("// Start a state modification.");
        printer.indent().p(STATE).pln(".start();");
      }
    }

    // Emit code for production element.
    indexName          = INDEX;
    resultName         = RESULT;
    baseIndex          = ARG_INDEX;
    useBaseIndex       = true;
    indentLevel        = 0;
    choiceLevel        = -1;
    repeated           = false;
    repeatedOnce       = false;
    repeatedElement    = null;
    savedRepeated      = false;
    savedRepeatedOnce  = false;
    optional           = false;
    optionLevel        = 0;
    optionalElement    = null;
    savedOptional      = false;
    createsNodeValue   = false;
    seenTest           = false;
    endsWithParseError = false;
    dispatch(p.choice);

    if (seenTest) {
      if (attributeStateful && p.hasAttribute(Constants.ATT_STATEFUL)) {
        printer.pln();
        printer.indent().pln("// Abort the state modification.");
        printer.indent().p(STATE).pln(".abort();");
      }
      printer.pln();
      printer.indent().pln("// Done.");
      if (attributeVerbose) {
        printer.indent().p("traceFailure(\"").p(p.name.toIdentifier()).
          p("\", ").p(ARG_INDEX).pln(");");
      }
      if (p.hasAttribute(Constants.ATT_EXPLICIT)) {
        printer.indent().p("return new ParseError(\"").
          p(Utilities.split(p.name.unqualify().name, ' ')).p(" expected\", ").
          p(ARG_INDEX).pln(");");
      } else {
        if (endsWithParseError &&
            (p.isMemoized() || ! runtime.test("optimizeErrors2"))) {
          parseError();
        }
        printer.indent().p("return ").p(PARSE_ERROR).pln(';');
      }
    }
    printer.decr().indent().pln('}');
    printer.pln();
  }

  // ========================================================================

  /**
   * Emit the code for assigning the result variable, threading the
   * parse error, and for testing the result.
   *
   * @param methodName The name of the parser method to use.
   * @param saveIndex The flag for whether to save the index in the
   *   base index variable.
   * @param threadError The flag for whether to thread the parse
   *   error.
   */
  protected void result(String methodName,
                        boolean saveIndex, boolean threadError) {
    printer.pln();

    // Clear the first element flag.
    firstElement    = false;

    // Set up the receiver of the result.
    String receiver = PARSE_CHAR.equals(methodName)? CHAR : resultName;

    // Set up the alignment for the equals sign.
    int align = receiver.length();
    if (saveIndex) {
      align = Math.max(align, BASE_INDEX.length());
    }
    if (! notFollowedBy() && ! PARSE_CHAR.equals(methodName)) {
      align = Math.max(align, PARSE_ERROR.length());
    }
    align += (printer.level() * Constants.INDENTATION) + 1 + 
      Constants.FIRST_COLUMN;

    if (useBaseIndex) {
      // The first result of an ordered choice or repetition as well
      // as the first element after a repetition always builds on the
      // current base index.  The first element of a predicate also
      // builds on the current base index.

      if (saveIndex) {
        // Assign index and result.
        printer.indent().p(BASE_INDEX).align(align).p("= ").buffer().
          p(baseIndex).p(';').fitMore().pln();
        printer.indent().p(receiver).align(align).p("= ").buffer().
          p(methodName).p('(').p(BASE_INDEX).p(");").fitMore().pln();
      } else {
        printer.indent().p(receiver).align(align).p("= ").buffer().
          p(methodName).p('(').p(baseIndex).p(");").fitMore().pln();
      }

      // Thread parse error.
      if (threadError) threadParseError(align);

      useBaseIndex = false;

    } else {
      // All other elements build on the last regular/predicate
      // result, depending on whether we are processing regular or
      // predicate elements.

      if (saveIndex) {
        // Assign parser and result.
        printer.indent().p(BASE_INDEX).align(align).p("= ").buffer().
          p(resultName).p(".index;").fitMore().pln();
        printer.indent().p(receiver).align(align).p("= ").buffer().
          p(methodName).p('(').p(BASE_INDEX).p(");").fitMore().pln();
      } else {
        // Assign result.
        printer.indent().p(receiver).align(align).p("= ").buffer().
          p(methodName).p('(').p(resultName).p(".index);").fitMore().pln();
      }

      // Thread parse error.
      if (threadError) threadParseError(align);
    }
  }

  /** 
   * Emit code for threading parse error.
   *
   * @param align The alignment for the assignment operator.
   */
  protected void threadParseError(int align) {
    printer.indent().p(PARSE_ERROR).align(align).p("= ").buffer().
      p(resultName).p(".select(").p(PARSE_ERROR);
    if (optional) {
      printer.p(", ").p(OPTION).p(optionLevel);
    } else if (repeated && ! repeatedOnce) {
      printer.p(", ").p(REPETITION).p(repetitionLevel);
    }
    printer.p(");").fitMore().pln();
  }

  /** Emit the code testing whether the result has a value. */
  protected void valueTest() {
    printer.indent().p("if (").p(resultName).pln(".hasValue()) {").incr();
  }

  /** Emit the code testing whether the character has a value. */
  protected void charValueTest() {
    printer.indent().p("if (-1 != ").p(CHAR).pln(") {").incr();
  }

  /**
   * Emit the code for testing the result.
   *
   * @param text The expected text value.
   * @param ignoreCase The flag for whether to ignore the case.
   */
  protected void stringValueTest(String text, boolean ignoreCase) {
    if (attributeParseTree) {
      if (ignoreCase) {
        printer.indent().p("if (").p(resultName).pln(".hasValue() &&").
          indent().p("    ((Node)").p(resultName).
          p(".semanticValue()).getTokenText().equalsIgnoreCase(\"").
          escape(text, Utilities.JAVA_ESCAPES).pln("\")) {").incr();

      } else {
        printer.indent().p("if (").p(resultName).pln(".hasValue() &&").
          indent().p("    ((Node)").p(resultName).
          p(".semanticValue()).getTokenText().equals(\"").
          escape(text, Utilities.JAVA_ESCAPES).pln("\")) {").incr();
      }

    } else if (runtime.test("optimizeMatches")) {
      if (ignoreCase) {
        printer.indent().p("if (").p(resultName).p(".hasValueIgnoreCase(\"").
          escape(text, Utilities.JAVA_ESCAPES).pln("\")) {").incr();

      } else {
        printer.indent().p("if (").p(resultName).p(".hasValue(\"").
          escape(text, Utilities.JAVA_ESCAPES).pln("\")) {").incr();
      }

    } else {
      if (ignoreCase) {
        printer.indent().p("if (").p(resultName).pln(".hasValue() &&").
          indent().p("   \"").escape(text, Utilities.JAVA_ESCAPES).
          p("\".equalsIgnoreCase(").p(resultName).
          pln(".semanticValue().toString())) {").incr();

      } else {
        printer.indent().p("if (").p(resultName).pln(".hasValue() &&").
          indent().p("   \"").escape(text, Utilities.JAVA_ESCAPES).
          p("\".equals(").p(resultName).pln(".semanticValue())) {").incr();
      }
    }
  }

  /**
   * Emit the code for assigning the index variable.
   *
   * @param oldIndex The old index.
   * @param isLastChar Flag for whether the just recognized character
   *   is the terminal's last character.
   */
  protected void index(String oldIndex, boolean isLastChar) {
    // The index variable is not used after assignment if the current
    // character is the last character of a predicate and does not
    // appear within a repetition or option.  The current character is
    // the current element for character terminals and the last
    // character for string terminals.

    if (! predicate ||
        (predicate && 
         (predicateIter.hasNext() || ! isLastChar || repeated || optional))) {
      printer.indent().p(indexName).p(" = ").p(oldIndex).pln(" + 1;");
      useBaseIndex = true;
      baseIndex    = indexName;
    }
  }

  /**
   * Emit the code for saving the index variable.
   *
   * @param savedIndex The saved index.
   * @param spacer Any extra space.
   * @param base The current base index.
   */
  protected void saveIndex(String savedIndex, String spacer, String base) {
    if (useBaseIndex) {
      if (! savedIndex.equals(base)) {
        printer.indent().p(savedIndex).p(spacer).p(" = ").p(base).pln(';');
      }
      useBaseIndex = false;
    } else {
      printer.indent().p(savedIndex).p(spacer).p(" = ").p(resultName).
        pln(".index;");
    }
  }

  /**
   * Note that a test has been emitted.  This method should be called
   * at the <i>end</i> of the method that emitted the test.
   */
  protected void tested() {
    seenTest = true;
  }

  /**
   * Emit the code for the next element. If the next element is the
   * last element in the main sequence, the code for returning a
   * semantic value is also emitted.
   *
   * @see #returnValue()
   */
  protected void nextElement() {
    // Process predicate elements first.
    if (predicate) {
      if (predicateIter.hasNext()) {
        // Emit code for the next predicate element.
        dispatch(predicateIter.next());
        return;

      } else if (repeated) {
        // Assign the repetition parser variable and, if necessary,
        // the repeated flag; then continue with the loop.
        printer.pln();
        saveIndex(REPETITION + repetitionLevel, "", baseIndex);

        if (repeatedOnce) {
          printer.indent().p(REPEATED).p(repetitionLevel).pln("   = true;");
        }
        if (null != repeatedElement) {
          printer.indent().p(REP_VALUE).p(repetitionLevel).p("   = ").buffer();
          if (attributeRawTypes) {
            printer.p("new Pair(");
          } else {
            printer.p("new ").p(extern(repetitionTypes.get(repetitionLevel-1))).
              p('(');
          }
          printer.p(repeatedElement).p(", ").p(REP_VALUE).p(repetitionLevel).
            p(");").fitMore().pln();
        }

        printer.indent().pln("continue;");
        return;

      } else if (optional) {
        printer.pln();
        saveIndex(OPTION + optionLevel, " ", baseIndex);

        if (null != optionalElement) {
          printer.indent().p(OP_VALUE).p(optionLevel).p(" = ").
            p(optionalElement).pln(';');
        }

        return;

      } else {
        // Assign matched variable for not-followed-by predicates.
        if (notFollowedBy) {
          printer.pln();
          printer.indent().p(PRED_MATCHED).pln(" = true;");
          return;
        }

        // Restore regular element processing and fall through for
        // followed-by predicates.
        predicate     = false;
        optional      = savedOptional;
        repeated      = savedRepeated;
        repeatedOnce  = savedRepeatedOnce;
        firstElement  = savedFirstElement;
        baseIndex     = savedBaseIndex;
        useBaseIndex  = savedUseBaseIndex;
        indexName     = INDEX;
        resultName    = RESULT;
      }
    }

    // Process the next regular grammar element.
    if (elementIter.hasNext()) {
      dispatch(elementIter.next());

    } else if (repeated) {
      printer.pln();
      saveIndex(REPETITION + repetitionLevel, "", baseIndex);

      if (repeatedOnce) {
        printer.indent().p(REPEATED).p(repetitionLevel).pln("   = true;");
      }
      if (null != repeatedElement) {
        printer.indent().p(REP_VALUE).p(repetitionLevel).p("   = ").buffer();
        if (attributeRawTypes) {
          printer.p("new Pair(");
        } else {
          printer.p("new ").p(extern(repetitionTypes.get(repetitionLevel-1))).
            p('(');
        }
        printer.p(repeatedElement).p(", ").p(REP_VALUE).p(repetitionLevel).
          p(");").fitMore().pln();
      }

      printer.indent().pln("continue;");

    } else if (optional) {
      printer.pln();
      saveIndex(OPTION + optionLevel, " ", baseIndex);

      if (null != optionalElement) {
        printer.indent().p(OP_VALUE).p(optionLevel).p(" = ").
          p(optionalElement).pln(';');
      }

    } else {
      returnValue();
    }
  }

  /**
   * Emit the code for annotating semantic values with their location.
   */
  private void location() {
    // Do not include location information if the grammar does not
    // have the withLocation attribute or the type of the production's
    // semantic value is not a node.  Note that void and text-only
    // productions automatically fall under the second case.
    if (! attributeWithLocation) return;

    if (runtime.test("optimizeLocation") && (! createsNodeValue)) return;

    FuzzyBoolean hasLocation = ast.hasLocation(analyzer.current().type);
    if (FuzzyBoolean.FALSE == hasLocation) {
      return;

    } else if (FuzzyBoolean.MAYBE == hasLocation) {
      printer.indent().p("if (").p(VALUE).pln(" instanceof Locatable) {").incr();
      printer.indent().p("setLocation((Locatable)").p(VALUE).p(", ").
        p(ARG_INDEX).pln(");");
      printer.decr().indent().pln('}');
    
    } else {
      printer.indent().p("setLocation(").p(VALUE).p(", ").p(ARG_INDEX).pln(");");
    }
  }

  /**
   * Emit the code for returning a semantic value.
   */
  protected void returnValue() {
    printer.pln();

    if (attributeStateful &&
        analyzer.current().hasAttribute(Constants.ATT_STATEFUL)) {
      printer.indent().pln("// Commit the state modification.");
      printer.indent().p(STATE).pln(".commit();");
      printer.pln();
    }
    location();

    if (attributeVerbose) {
      printer.indent().p("traceSuccess(\"").
        p(analyzer.current().name.toIdentifier()).p("\", ").p(ARG_INDEX).
        pln(");");
    }

    if (useBaseIndex) {
      printer.indent().p("return new SemanticValue(").p(VALUE).
        p(", ").p(baseIndex).p(", ").p(PARSE_ERROR).pln(");");

      useBaseIndex = false;

    } else {
      if (runtime.test("optimizeValues")) {
        printer.indent().p("return ").p(RESULT).p(".createValue(").
          p(VALUE).p(", ").p(PARSE_ERROR).pln(");");
      } else {
        printer.indent().p("return new SemanticValue(").p(VALUE).
          p(", ").p(RESULT).p(".index, ").p(PARSE_ERROR).pln(");");
      }
    }
  }

  /**
   * Emit the code for generating a parse error based on the
   * production's name.
   */
  protected void parseError() {
    printer.indent().p(PARSE_ERROR).p(" = ").p(PARSE_ERROR).p(".select(\"").
      p(Utilities.split(analyzer.current().name.unqualify().name, ' ')).
      p(" expected\", "). p(ARG_INDEX).pln(");");
  }

  /**
   * Emit the code for generating a parse error based on a fixed text.
   *
   * @param text The expected text.
   */
  protected void parseError(String text) {
    printer.indent().p(PARSE_ERROR).p(" = ").p(PARSE_ERROR).p(".select(\"'").
      escape(text, Utilities.JAVA_ESCAPES | Utilities.ESCAPE_DOUBLE).
      p("' expected\", ").p(BASE_INDEX).pln(");");
  }

  // ========================================================================

  /**
   * Return the name of the parser variable for the current nested
   * choice level.
   *
   * @return The nested choice parser variable.
   */
  protected String nestedChoice() {
    return NESTED_CHOICE + Integer.toString(choiceLevel);
  }

  /** Generate code for the specified ordered choice. */
  public void visit(OrderedChoice c) {
    final String  base    = baseIndex;
    final boolean used    = useBaseIndex;
    final boolean creates = createsNodeValue;
    final int     indent  = indentLevel;
    indentLevel           = printer.level();
    choiceLevel++;

    // Make sure that nested choices are at a deeper scope than the
    // enclosing choice.
    final boolean scoped  = (indent == indentLevel);
    if (scoped) {
      printer.indent().pln("{ // Start scope for nested choice.").incr();
    }

    // For non-top-level choices, declare a parser variable and save
    // the current parser.
    if (0 != choiceLevel) {
      printer.pln();

      if (useBaseIndex) {
        printer.indent().p("final ").p(indexT()).p(' ').p(nestedChoice()).
          p(" = ").p(base).pln(';');
        useBaseIndex = false;
      } else {
        printer.indent().p("final ").p(indexT()).p(' ').p(nestedChoice()).
          p(" = ").buffer().p(resultName).p(".index;").fitMore().pln();
      }
    }

    // Process the alternatives.
    int alternativeNumber = 0;
    for (Sequence s : c.alternatives) {
      elementIter      = s.elements.iterator();
      if (0 == choiceLevel) {
        firstElement   = true;
      }
      baseIndex        = (0 == choiceLevel)? ARG_INDEX : nestedChoice();
      useBaseIndex     = true;
      createsNodeValue = creates;
      seenTest         = false;
      alternativeNumber++;

      printer.pln();
      if (0 == choiceLevel) {
        printer.indent().p("// Alternative ");
      } else {
        printer.indent().p("// Nested alternative ");
      }
      if (null == s.name) {
        printer.p(alternativeNumber).pln('.');
      } else {
        printer.p('<').p(s.name.name).pln(">.");
      }

      nextElement();
    }

    if (scoped) {
      printer.decr().indent().pln("} // End scope for nested choice.");
    }

    choiceLevel--;
    indentLevel  = indent;
    useBaseIndex = used;
    baseIndex    = base;
  }

  // ========================================================================

  /** Generate code for the specified repetition. */
  public void visit(Repetition r) {
    assert r.element instanceof Sequence;

    firstElement        = false;
    String   base       = baseIndex; 
    boolean  used       = useBaseIndex;

    String   repel      = repeatedElement;
    if (hasBinding()) {
      // Per class documentation, the repeated element must be a
      // sequence, whose semantic value is captured by the binding
      // returned by Analyzer.getBinding().
      Binding b         = Analyzer.getBinding(((Sequence)r.element).elements);
      repeatedElement   = b.name;
      bindingType       =
        AST.listOf(ast.concretize(analyzer.type(b.element), AST.ANY));
    } else {
      repeatedElement   = null;
      bindingType       = null;
    }
    String  name        = bindingName;
    bindingName         = null;
    Element bound       = bindingElement;
    bindingElement      = null;
    Type type           = bindingType;
    bindingType         = null;

    boolean rep         = repeated;
    repeated            = true;
    boolean once        = repeatedOnce;
    repeatedOnce        = r.once;
    boolean opt         = optional;
    optional            = false;
    repetitionLevel++;

    // Save current parser.
    printer.pln();
    saveIndex(REPETITION + repetitionLevel, "", base);

    // Reset repeated flag if necessary.
    if (repeatedOnce) {
      printer.indent().p(REPEATED).p(repetitionLevel).pln("   = false;");
    }

    // Reset list value for bound repetitions.
    if (null != name) {
      printer.indent().p(REP_VALUE).p(repetitionLevel).p("   = ").
        p(emptyListExpr()).pln(';');
    }

    // Save current code generation state.
    Iterator<Element> iter;
    if (predicate) {
      iter          = predicateIter;
      predicateIter = ((Sequence)r.element).elements.iterator();
    } else {
      iter          = elementIter;
      elementIter   = ((Sequence)r.element).elements.iterator();
    }

    // Emit code for the repeated elements.
    printer.indent().pln("while (true) {").incr();
    baseIndex    = REPETITION + repetitionLevel;
    useBaseIndex = true;
    nextElement();
    printer.indent().pln("break;");
    printer.decr().indent().pln('}');

    // Restore code generation state.
    if (predicate) {
      predicateIter    = iter;
    } else {
      elementIter      = iter;
    }

    // Emit code for the rest of the current sequence.
    if (repeatedOnce) {
      printer.pln();
      printer.indent().p("if (").p(REPEATED).p(repetitionLevel).pln(") {").
        incr();
    }
    repetitionLevel--;
    repeated           = rep;
    repeatedOnce       = once;
    repeatedElement    = repel;
    optional           = opt;

    bindingName        = name;
    bindingElement     = bound;
    bindingType        = type;
    boolean closeBrace = false;
    String  blockName  = name;
    if (hasBinding()) {
      if (! r.once) {
        printer.indent().p("{ // Start scope for ").p(blockName).pln('.').incr();
        closeBrace     = true;
      }
      binding();
      clearBinding();
    }

    baseIndex       = REPETITION + Integer.toString(repetitionLevel + 1);
    useBaseIndex    = true;
    if (! r.once) {
      seenTest      = false;
    }

    nextElement();

    if (r.once) {
      printer.decr().indent().pln('}');
      tested();
    } else if (closeBrace) {
      printer.decr().indent().p("} // End scope for ").p(blockName).pln('.');
    }
    baseIndex    = base;
    useBaseIndex = used;
  }

  // ========================================================================

  /** Generate code for the specified option. */
  public void visit(Option o) {
    assert o.element instanceof Sequence;

    firstElement        = false;
    String  base        = baseIndex;
    boolean used        = useBaseIndex;

    String  optel       = optionalElement;
    if (hasBinding()) {
      // Per class documentation, the optional element must be a
      // sequence, whose semantic value is captured by
      // Analyzer.getBinding().
      Binding b         = Analyzer.getBinding(((Sequence)o.element).elements);
      optionalElement   = b.name;
      bindingType       =
        ast.concretize(analyzer.type(b.element), AST.ANY);
    } else {
      optionalElement   = null;
      bindingType       = null;
    }
    String name         = bindingName;
    bindingName         = null;
    Element bound       = bindingElement;
    bindingElement      = null;
    Type type           = bindingType;
    bindingType         = null;

    boolean opt         = optional;
    optional            = true;
    boolean rep         = repeated;
    repeated            = false;
    optionLevel++;

    // Save current parser.
    printer.pln();
    saveIndex(OPTION + optionLevel, " ", base);

    // Reset optional value for bound options.
    if (null != name) {
      printer.indent().p(OP_VALUE).p(optionLevel).p(" = ").p(nullExpr()).
        pln(';');
    }

    // Save current code generation state.
    Iterator<Element> iter;
    if (predicate) {
      iter          = predicateIter;
      predicateIter = ((Sequence)o.element).elements.iterator();
    } else {
      iter          = elementIter;
      elementIter   = ((Sequence)o.element).elements.iterator();
    }

    // Emit code for the optional elements.
    baseIndex    = OPTION + optionLevel;
    useBaseIndex = true;
    nextElement();

    // Restore code generation state.
    if (predicate) {
      predicateIter = iter;
    } else {
      elementIter   = iter;
    }

    // Emit code for the rest of the current sequence.
    optionLevel--;
    optional           = opt;
    optionalElement    = optel;
    repeated           = rep;

    bindingName        = name;
    bindingElement     = bound;
    bindingType        = type;
    boolean closeBrace = false;
    String  blockName  = name;
    if (hasBinding()) {
      printer.indent().p("{ // Start scope for ").p(blockName).pln('.').incr();
      closeBrace       = true;
      binding();
      clearBinding();
    }

    baseIndex    = OPTION + Integer.toString(optionLevel + 1);
    useBaseIndex = true;
    seenTest     = false;

    nextElement();

    if (closeBrace) {
      printer.decr().indent().p("} // End scope for ").p(blockName).pln('.');
    }
    baseIndex    = base;
    useBaseIndex = used;
  }

  // ========================================================================

  /** Generate code for the specified followed-by predicate. */
  public void visit(FollowedBy p) {
    assert ! predicate;
    assert p.element instanceof Sequence;

    predicate         = true;
    notFollowedBy     = false;
    savedOptional     = optional;
    optional          = false;
    savedRepeated     = repeated;
    repeated          = false;
    savedRepeatedOnce = repeatedOnce;
    repeatedOnce      = false;
    savedFirstElement = firstElement;
    savedBaseIndex    = baseIndex;
    if (! useBaseIndex) {
      // Only set a new base index if the base index is not used for
      // the next element.
      baseIndex       = RESULT + ".index";
    }
    savedUseBaseIndex = useBaseIndex;
    useBaseIndex      = true;
    indexName         = PRED_INDEX;
    resultName        = PRED_RESULT;
    predicateIter     = ((Sequence)p.element).elements.iterator();

    // Emit code for the followed-by predicate and the rest of the
    // rule sequence.
    nextElement();

    tested();
  }

  // ========================================================================

  /**
   * Determine whether we are processing a not-followed-by predicate.
   *
   * @return <code>true</code> if we are processing a not-followed-by
   * predicate.
   */
  protected boolean notFollowedBy() {
    return (predicate && notFollowedBy);
  }

  /** Generate code for the specified not-followed-by predicate. */
  public void visit(NotFollowedBy p) {
    assert ! predicate;
    assert p.element instanceof Sequence;

    predicate         = true;
    notFollowedBy     = true;
    savedOptional     = optional;
    optional          = false;
    savedRepeated     = repeated;
    repeated          = false;
    savedRepeatedOnce = repeatedOnce;
    repeatedOnce      = false;
    savedFirstElement = firstElement;
    savedBaseIndex    = baseIndex;
    if (! useBaseIndex) {
      // Only set a new base index if the base index is not used for
      // the next element.
      baseIndex       = RESULT + ".index";
    }
    savedUseBaseIndex = useBaseIndex;
    useBaseIndex      = true;
    indexName         = PRED_INDEX;
    resultName        = PRED_RESULT;
    predicateIter     = ((Sequence)p.element).elements.iterator();

    // Emit code for the not-followed-by predicate.
    printer.pln();
    printer.indent().p(PRED_MATCHED).pln(" = false;");

    nextElement();

    // Restore regular element processing.
    predicate     = false;
    optional      = savedOptional;
    repeated      = savedRepeated;
    repeatedOnce  = savedRepeatedOnce;
    firstElement  = savedFirstElement;
    baseIndex     = savedBaseIndex;
    useBaseIndex  = savedUseBaseIndex;
    indexName     = INDEX;
    resultName    = RESULT;

    // Emit code for the rest of the rule sequence.
    printer.pln();
    printer.indent().p("if (! ").p(PRED_MATCHED).pln(") {").incr();

    nextElement();

    printer.decr().indent().pln("} else {").incr();
    parseError();
    printer.decr().indent().pln('}');

    tested();
  }

  // ========================================================================

  /** Generate code for the specified semantic predicate. */
  public void visit(SemanticPredicate p) {
    printer.pln().indent().p("if (");

    Action a = (Action)p.element;
    if (1 == a.code.size()) {
      printer.p(a.code.get(0)).pln(") {").incr();
    } else {
      boolean  first  = true;
      int      column = printer.column();
      for (String s : a.code) {
        if (first) {
          printer.p(s);
          first = false;
        } else {
          printer.pln().align(column).p(s);
        }
      }
      printer.pln(") {").incr();
    }

    nextElement();

    printer.decr().indent().pln('}');

    if (! notFollowedBy()) {
      endsWithParseError = true;
    }

    tested();
  }

  // ========================================================================

  /** Generate code for the specified voided element. */
  public void visit(VoidedElement v) {
    // Visit the element.
    dispatch(v.element);
  }

  // ========================================================================

  /** Generate code for the specified binding. */
  public void visit(Binding b) {
    // Save old name and element.
    String  oldName    = bindingName;
    Element oldElement = bindingElement;
    Type    oldType    = bindingType;

    // Set up new name and element;
    bindingName    = b.name;
    bindingElement = b.element;
    bindingType    = null; // For now, this field is only used in options.

    // Visit element.
    dispatch(b.element);

    // Restore old name and element.
    bindingName    = oldName;
    bindingElement = oldElement;
    bindingType    = oldType;
  }

  /**
   * Determine whether the current element has a binding.
   *
   * @return <code>true</code> if the current element has a binding.
   */
  protected boolean hasBinding() {
    return (null != bindingName);
  }

  /** Actually emit the code for the last visited binding. */
  protected void binding() {
    switch (bindingElement.tag()) {
    case NONTERMINAL: {
      Type type = VALUE.equals(bindingName) ? analyzer.current().type :
        analyzer.lookup((NonTerminal)bindingElement).type;
      Type cast = attributeRawTypes && ! AST.isAny(type) ? type : null;
      binding1(extern(type), bindingName, extern(cast),
               resultName + ".semanticValue()");
    } break;

    case ANY_CHAR:
    case CHAR_CLASS:
    case CHAR_LITERAL:
    case CHAR_SWITCH:
      if (VALUE.equals(bindingName)) {
        binding1(extern(AST.CHAR), bindingName, null,
                 "Character.valueOf((" + charT() + ")" + CHAR + ")");
      } else {
        binding1(charT(), bindingName, null, "(" + charT() + ")" + CHAR);
      }
      break;

    case STRING_LITERAL: {
      final String text = ((StringLiteral)bindingElement).text;
      binding1(extern(AST.STRING), bindingName, null,
               '"' + Utilities.escape(text, Utilities.JAVA_ESCAPES) + '"');
    } break;

    case STRING_MATCH:
      if (attributeParseTree) {
        String cast = attributeRawTypes ? extern(AST.NODE) : null;
        binding1(extern(AST.NODE), bindingName, cast,
                 resultName+".semanticValue()");
      } else {
        binding1(extern(AST.STRING), bindingName, null,
                 "\"" + Utilities.escape(((StringMatch)bindingElement).text,
                                         Utilities.JAVA_ESCAPES) + "\"");
      }
      break;

    case REPETITION: {
      int    level = repetitionLevel + 1;
      String expr  = REP_VALUE + level + ".reverse()";
      if ((! attributeRawTypes) &&
          (! repetitionTypes.get(repetitionLevel).equals(bindingType))) {
        expr = "cast(" + expr + ')';
      }
      binding1(extern(bindingType), bindingName, null, expr);
    } break;

    case OPTION: {
      int    level = optionLevel + 1;
      String cast  = null;
      String expr  = OP_VALUE + level;
      if (! optionTypes.get(optionLevel).equals(bindingType)) {
        if (attributeRawTypes) {
          cast = extern(bindingType);
        } else {
          expr = "cast(" + expr + ')';
        }
      }
      binding1(extern(bindingType), bindingName, cast, expr);
    } break;

    case NULL:
      binding1(extern(AST.ANY), bindingName, null, nullExpr());
      break;

    default:
      throw new AssertionError("Unrecognized binding element " + bindingElement);
    }
  }

  /**
   * Emit the binding code.
   *
   * @param type The variable type as a string.
   * @param name The variable name.
   * @param cast The cast type as a string, or <code>null</code> for no cast.
   * @param expr The value producing expression.
   */
  private void binding1(String type, String name, String cast, String expr) {
    if (attributeRawTypes) {
      type = rawT(type);
      if (null != cast) cast = rawT(cast);
    }

    printer.indent();

    if (VALUE.equals(name)) {
      printer.p(VALUE);
    } else {
      if (attributeConstant) printer.p("final ");
      printer.p(type).p(' ').p(name);
    }

    printer.p(" = ");
    if (null != cast) printer.p('(').p(cast).p(')');
    printer.p(expr).pln(';');
  }

  /** Clear binding information after usage. */
  protected void clearBinding() {
    bindingName    = null;
    bindingElement = null;
  }

  // ========================================================================

  /** Generate code for the specified string match. */
  public void visit(StringMatch m) {
    final boolean first = firstElement;

    // At this point, the element of the string match must be a
    // nonterminal.
    NonTerminal nt = (NonTerminal)m.element;

    result(methodName(nt),
           ! notFollowedBy() && (! runtime.test("optimizeErrors1") || ! first),
           false);
    stringValueTest(m.text, attributeIgnoringCase);

    if (hasBinding()) {
      binding();
      clearBinding();
    }

    nextElement();

    if (notFollowedBy()) {
      printer.decr().indent().pln('}');
    } else if (runtime.test("optimizeErrors1") && first) {
      printer.decr().indent().pln('}');
      endsWithParseError = true;
    } else {
      printer.decr().indent().pln("} else {").incr();
      parseError(m.text);
      printer.decr().indent().pln('}');
    }

    tested();
  }

  // ========================================================================

  /** Generate code for the specified nonterminal. */
  public void visit(NonTerminal nt) {
    result(methodName(nt), false, ! notFollowedBy());
    valueTest();

    if (hasBinding()) {
      binding();
      clearBinding();
    }

    nextElement();

    // If the referenced production is transient and does not generate
    // parse errors, we might need to generate one in this production.
    if (! notFollowedBy() &&
        ! analyzer.lookup(nt).isMemoized() &&
        runtime.test("optimizeErrors2")) {
      endsWithParseError = true;
    }

    printer.decr().indent().pln('}');
    tested();
  }

  // ========================================================================

  /** Generate code for the any character element. */
  public void visit(AnyChar a) {
    String oldIndex = useBaseIndex? baseIndex : resultName + ".index";
    result(PARSE_CHAR, false, false);
    charValueTest();
    index(oldIndex, true);

    if (hasBinding()) {
      binding();
      clearBinding();
    }

    nextElement();

    printer.decr().indent().pln('}');

    if (! notFollowedBy()) {
      endsWithParseError = true;
    }

    tested();
  }
  
  // ========================================================================
  
  /** Generate code for the specified character literal. */
  public void visit(CharLiteral l) {
    String oldIndex = useBaseIndex? baseIndex : resultName + ".index";
    result(PARSE_CHAR, false, false);
    printer.indent().p("if (\'").escape(l.c, Utilities.JAVA_ESCAPES).
      p("\' == ").p(CHAR).pln(") {").incr();
    index(oldIndex, true);

    if (hasBinding()) {
      binding();
      clearBinding();
    }

    nextElement();

    printer.decr().indent().pln('}');

    if (! notFollowedBy()) {
      endsWithParseError = true;
    }

    tested();
  }

  // ========================================================================

  /** Generate code for the specified character class. */
  public void visit(CharClass c) {
    String oldIndex = useBaseIndex? baseIndex : resultName + ".index";
    result(PARSE_CHAR, false, false);
    charValueTest();
    index(oldIndex, true);

    String name;
    if (hasBinding()) {
      binding();
      name = bindingName;
      clearBinding();
      printer.pln();
    } else {
      name = CHAR;
    }

    final int           length = c.ranges.size();
    Iterator<CharRange> iter   = c.ranges.iterator();

    if (1 == length) {
      printer.indent().p("if ");
    } else {
      printer.indent().p("if (");
    }

    while (iter.hasNext()) {
      CharRange r = iter.next();

      if (c.exclusive) {
        if (r.first == r.last) {
          printer.p("(\'").escape(r.first, Utilities.JAVA_ESCAPES).p("\' != ").
            p(name).p(')');
        } else {
          printer.p('(').p(name).p(" < \'").
            escape(r.first, Utilities.JAVA_ESCAPES).p(") || (\'").
            escape(r.last, Utilities.JAVA_ESCAPES).p("\' < ").p(name).p("))");
        }

      } else {
        if (r.first == r.last) {
          printer.p("(\'").escape(r.first, Utilities.JAVA_ESCAPES).p("\' == ").
            p(name).p(')');
        } else {
          printer.p("((\'").escape(r.first, Utilities.JAVA_ESCAPES).p("\' <= ").
            p(name).p(") && (").p(name).p(" <= \'").
            escape(r.last, Utilities.JAVA_ESCAPES).p("\'))");
        }
      }

      if (iter.hasNext()) {
        if (c.exclusive) {
          printer.pln(" &&");
        } else {
          printer.pln(" ||");
        }
        printer.indent().p("    ");
      }
    }

    if (1 == length) {
      printer.pln(" {").incr();
    } else {
      printer.pln(") {").incr();
    }

    nextElement();

    printer.decr().indent().pln('}');
    printer.decr().indent().pln('}');

    if (! notFollowedBy()) {
      endsWithParseError = true;
    }

    tested();
  }

  // ========================================================================

  /** Generate code for the specified literal. */
  public void visit(StringLiteral l) {
    final boolean first  = firstElement;
    final int     length = l.text.length();

    for (int i=0; i<length; i++) {
      char c = l.text.charAt(i);

      String oldIndex = useBaseIndex? baseIndex : resultName + ".index";
      result(PARSE_CHAR,
             0 == i && ! notFollowedBy() &&
             (! runtime.test("optimizeErrors1") || ! first),
             false);
      printer.indent().p("if (\'").escape(c, Utilities.JAVA_ESCAPES).
        p("\' == ").p(CHAR).pln(") {").incr();
      index(oldIndex, i == length-1);
    }

    if (hasBinding()) {
      binding();
      clearBinding();
    }

    nextElement();

    for (int i=0; i<length; i++) {
      if (notFollowedBy()) {
        printer.decr().indent().pln('}');
      } else if (runtime.test("optimizeErrors1") && first) {
        printer.decr().indent().pln('}');
        endsWithParseError = true;
      } else {
        printer.decr().indent().pln("} else {").incr();
        parseError(l.text);
        printer.decr().indent().pln('}');
      }
    }

    tested();
  }

  // ========================================================================

  /** Generate code for the specified character switch. */
  public void visit(CharSwitch s) {
    String oldIndex = useBaseIndex? baseIndex : resultName + ".index";
    result(PARSE_CHAR, false, false);
    charValueTest();
    index(oldIndex, true);
    printer.pln();

    String  base = baseIndex;
    boolean used = useBaseIndex;

    String name;
    if (hasBinding()) {
      binding();
      name = bindingName;
      clearBinding();
      printer.pln();
    } else {
      name = CHAR;
    }

    printer.indent().p("switch (").p(name).pln(") {").incr();

    for (CharCase c : s.cases) {
      for (CharRange r : c.klass.ranges) {
        for (char k = r.first; k <= r.last; k++) {
          printer.indentLess().p("case \'").escape(k, Utilities.JAVA_ESCAPES).
            pln("\':");
        }
      }

      if (null == c.element) {
        printer.indent().pln("/* No match. */");
        printer.indent().pln("break;");

      } else {
        printer.indent().p('{').incr();
        // The line terminator is printed by emitting code for
        // c.element.

        baseIndex    = base;
        useBaseIndex = used;
        seenTest     = false;
        
        if (c.element instanceof OrderedChoice) {
          dispatch(c.element);
        } else {
          elementIter = ((Sequence)c.element).elements.iterator();
          nextElement();
        }
        
        printer.decr().indent().pln('}');
        if (seenTest || optional) {
          printer.indent().pln("break;");
        }
      }

      printer.pln();
    }

    if (null == s.base) {
      printer.indentLess().pln("default:");
      printer.indent().pln("/* No match. */");
    } else {
      printer.indentLess().pln("default:");
      printer.indent().p('{').incr();
      // The line terminator is printed by emitting code for s.base.

      baseIndex    = base;
      useBaseIndex = used;

      if (s.base instanceof OrderedChoice) {
        dispatch(s.base);
      } else {
        elementIter = ((Sequence)s.base).elements.iterator();
        nextElement();
      }
      printer.decr().indent().pln('}');
    }

    printer.decr().indent().pln('}');
    printer.decr().indent().pln('}');

    endsWithParseError = true;
    tested();
  }

  // ========================================================================

  /** Generate code for the specified node marker. */
  public void visit(NodeMarker m) {
    nextElement();
  }

  // ========================================================================

  /** Actually emit code for the specified action. */
  protected void action(Action a) {
    int               baseLevel  = printer.level();
    int               level      = 0;
    Iterator<String>  codeIter   = a.code.iterator();
    Iterator<Integer> indentIter = a.indent.iterator();

    while (codeIter.hasNext()) {
      int newLevel = indentIter.next();
      int diff     = newLevel - level;
      level        = newLevel;

      if (0 < diff) {
        for (int i=0; i<diff; i++) {
          printer.incr();
        }
      } else {
        for (int i=0; i>diff; i--) {
          printer.decr();
        }
      }

      printer.indent().pln(codeIter.next());
    }

    printer.setLevel(baseLevel);
  }

  /** Generate code for the specified action. */
  public void visit(Action a) {
    // If the action sets the semantic value, we conservatively assume
    // that it creates a node value.
    if (a.setsValue()) createsNodeValue = true;

    printer.pln();
    action(a);

    nextElement();
  }

  /** Generate code for the specified parser action. */
  public void visit(ParserAction pa) {
    // We conservatively assume that parser actions may create a node
    // value.
    createsNodeValue = true;

    printer.pln();

    // Set up CodeGenerator.BASE_INDEX.
    saveIndex(BASE_INDEX, "", baseIndex);
    printer.pln();

    // Emit the actual action code.
    action((Action)pa.element);
    printer.pln();

    // Thread parse error.
    if (! notFollowedBy()) threadParseError(0);

    // Test for value.
    valueTest();

    // Assign to CodeGenerator.VALUE, i.e., yyValue.
    printer.indent().p(VALUE).p(" = ");
    if (attributeRawTypes && (! AST.isAny(analyzer.current().type))) {
      printer.p('(').p(rawT(extern(analyzer.current().type))).p(')');
    }
    printer.p(RESULT).p(".semanticValue();");

    // Process the next element.
    nextElement();

    // Finish the value test.
    printer.decr().indent().pln('}');
    tested();
  }

  // ========================================================================

  /** Generate code for the specified parse tree node. */
  public void visit(ParseTreeNode n) {
    // Parse tree nodes may only appear within bindings.
    assert hasBinding();

    // Emit the variable type and name.
    printer.indent();
    if (VALUE.equals(bindingName)) {
      printer.p(VALUE);
    } else {
      if (attributeConstant) printer.p("final ");
      printer.p(extern(AST.NODE)).p(' ').p(bindingName);
    }

    // Determine the name of the annotated node.
    String node = (null == n.node) ? nullExpr() : var(n.node);

    // Emit the class name.
    printer.p(" = Formatting.");

    // Emit the factory method calls.
    if ((1 == n.predecessors.size()) && (0 == n.successors.size())) {
      printer.p("before1(").p(var(n.predecessors.get(0))).p(", ").
        p(node).p(')');

    } else if ((1 == n.predecessors.size()) && (1 == n.successors.size())) {
      printer.p("round1(").p(var(n.predecessors.get(0))).p(", ").
        p(node).p(", ").p(var(n.successors.get(0))).p(')');

    } else if ((0 == n.predecessors.size()) && (1 == n.successors.size())) {
      printer.p("after1(").p(node).p(", ").p(var(n.successors.get(0))).
        p(')');

    } else {
      printer.pln("variable().").indentMore();

      // Emit the calls to add the nodes.
      boolean first = true;
      for (Binding b : n.predecessors) {
        if (first) {
          first = false;
        } else {
          printer.p('.');
        }
        printer.p("add(").p(var(b)).p(')');
      }

      // If there is no node, there are no successors.
      if (null != n.node) {
        if (! first) printer.p('.');
        printer.p("addNode(").p(node).p(')');

        for (Binding b : n.successors) {
          printer.p(".add(").p(var(b)).p(')');
        }
      }
    }

    // Wrap up.
    printer.pln(';');
    clearBinding();

    nextElement();
  }

  // ========================================================================

  /** Generate code for the specified null literal. */
  public void visit(NullLiteral l) {
    // A null literal requires a binding to have a visible effect.
    // However, we only emit the binding, if the variable name is not
    // synthetic.  An emitted binding must be wrapped it in a scope to
    // avoid variable redefinition errors.
    boolean emit = false;
    String  name = null;

    if (hasBinding()) {
      if (! Analyzer.isSynthetic(bindingName)) {
        emit = true;
        name = bindingName;
        printer.indent().p("{ // Start scope for ").p(name).pln('.').incr();
        binding();
      }
      
      // Always clear the binding.
      clearBinding();
    }

    nextElement();

    if (emit) {
      printer.decr().indent().p("} // End scope for ").p(name).pln('.');
    }
  }

  // ========================================================================

  /** Generate code for the specified null value. */
  public void visit(NullValue v) {
    printer.pln();
    printer.indent().p(VALUE).p(" = ").p(nullExpr()).pln(';');

    nextElement();
  }

  /** Emit code for determining the textual different. */
  protected void emitDifference() {
    if (firstElement) {
      printer.p("\"\"");
    } else {
      printer.p("difference(").p(ARG_INDEX).p(", ");
      if (useBaseIndex) {
        printer.p(baseIndex);
      } else {
        printer.p(RESULT).p(".index");
      }
      printer.p(')');
    }
  }

  /** Generate code for the specified string value. */
  public void visit(StringValue v) {
    printer.pln();
    printer.indent().p(VALUE).p(" = ");
    if (null == v.text) {
      emitDifference();
    } else {
      printer.p('"').escape(v.text, Utilities.JAVA_ESCAPES).p('"');
    }
    printer.pln(';');
    
    nextElement();
  }

  /** Generate code for the specified token value. */
  public void visit(TokenValue v) {
    printer.pln();
    printer.indent().p(VALUE).p(" = new Token(");
    if (null == v.text) {
      emitDifference();
    } else {
      printer.p('"').escape(v.text, Utilities.JAVA_ESCAPES).p('"');
    }
    printer.pln(");");

    // If the location optimization is enabled and yyValue is declared
    // to be a node or token, then add the source location directly to
    // the just created node.  Otherwise, just record that the
    // alternative creates a node value.
    final Type type = analyzer.current().type;
    if (attributeWithLocation &&
        runtime.test("optimizeLocation") &&
        AST.isNode(type)) {
      printer.indent().p(VALUE).p(".setLocation(location(").p(ARG_INDEX).
        pln("));");
    } else {
      createsNodeValue = true;
    }

    nextElement();
  }

  /**
   * Convert a binding into the corresponding variable name.  If the
   * specified binding is for a synthetic variable and the bound
   * element is a null literal, this method returns "null" to inline
   * the null value.  Otherwise, it returns the binding's name.
   *
   * @param b The binding.
   * @return The corresponding variable name.
   */
  protected String var(Binding b) {
    return Analyzer.isSynthetic(b.name) && (b.element instanceof NullLiteral) ?
      nullExpr() : b.name;
  }

  /** Generate code for the specified binding value. */
  public void visit(BindingValue v) {
    printer.pln();
    printer.indent().p(VALUE).p(" = ").p(var(v.binding)).pln(';');

    nextElement();
  }

  /** Generate code for the specified empty list value. */
  public void visit(EmptyListValue v) {
    printer.pln().indent().p(VALUE).p(" = ").p(emptyListExpr()).pln(';');

    nextElement();
  }

  /** Generate code for the specified proper list value. */
  public void visit(ProperListValue v) {
    printer.pln();
    printer.indent().p(VALUE).p(" = ");

    boolean first = true;
    for (Binding b : v.elements) {
      if (first) {
        first = false;
      } else {
        printer.p(", ");
      }

      printer.p("new ");
      if (attributeRawTypes) {
        printer.p("Pair");
      } else {
        printer.p(extern(v.type));
      }
      printer.p('(').p(var(b));
    }

    if (null != v.tail) printer.p(", ").p(var(v.tail));

    for (int i=0; i<v.elements.size(); i++) printer.p(')');
    printer.pln(';');

    nextElement();
  }

  /** Generate code for the specified action base value. */
  public void visit(ActionBaseValue v) {
    printer.pln();
    printer.indent().p(VALUE).p(" = ");
    // Do we need a cast?
    if (! AST.isAny(analyzer.current().type)) {
      if (attributeRawTypes) {
        printer.p('(').p(rawT(extern(analyzer.current().type))).p(')');
      }
    }

    printer.p("apply(").p(var(v.list)).p(", ").p(var(v.seed));
    if (attributeWithLocation &&
        FuzzyBoolean.TRUE == ast.hasLocation(analyzer.current().type)) {
      printer.p(", ").p(ARG_INDEX);
    }
    printer.pln(");");

    nextElement();
  }

  /**
   * Calculate the number of a generic node's children.  This method
   * returns the fixed number of children if none of the children has
   * a list value, <code>Integer.MAX_VALUE</code> if any of the
   * children has a non-null list value, and
   * <code>Integer.MIN_VALUE</code> if any of the children has a list
   * value that may be <code>null</code>.
   *
   * @param base The number of children not from the specified list.
   * @param children The list of bindings representing the children.
   * @return The number of children.
   */
  protected int numberOfChildren(int base, List<Binding> children) {
    for (Binding b : children) {
      if (attributeFlatten && AST.isList(analyzer.type(b.element))) {
        if (analyzer.mayBeNull(b.element)) {
          return Integer.MIN_VALUE;
        } else {
          base = Integer.MAX_VALUE;
        }
      } else if (base != Integer.MAX_VALUE) {
        base++;
      }
    }

    return base;
  }

  /**
   * Emit an expression calculating a generic node's number of children.
   *
   * @param base The number of children not from the specified list.
   * @param children The list of bindings representing the children.
   */
  protected void emitNumberOfChildren(int base, List<Binding> children) {
    boolean  printed = false;
    for (Binding b : children) {
      if (attributeFlatten && AST.isList(analyzer.type(b.element))) {
        if (printed) {
          printer.p(" + ");
        } else {
          printed = true;
        }

        boolean test = analyzer.mayBeNull(b.element);
        if (test) {
          printer.pln().indentMore().p('(').p(nullExpr()).p(" == ").p(b.name).
            p(" ? 0 : ");
        }

        // Note: This expression used to contain an explicit cast to
        // Pair, which has been removed since all bindings are
        // declared with their correct types.
        printer.p(b.name).p(".size()");

        if (test) printer.p(')');
      } else {
        base++;
      }
    }

    if (! printed) {
      printer.p(base);
    } else if (0 != base) {
      printer.p(" + ").p(base);
    }
  }

  /**
   * Emit an expression adding the children to a generic node.
   *
   * @param first The name of the optional first child.
   * @param children The list of children.
   */
  protected void emitChildren(String first, List<Binding> children) {
    printer.p(')');

    boolean indent = true;

    if (null != first) {
      if (indent) {
        printer.pln('.').indentMore();
        indent = false;
      } else {
        printer.p('.');
      }

      printer.p("add(").p(first).p(')');
    }

    boolean statement = false;
    for (Binding b : children) {
      if ((! attributeFlatten) || (! AST.isList(analyzer.type(b.element)))) {
        // A non-flattened list value or a non-list value.
        if (statement) {
          printer.pln(';').indent().p(VALUE).p('.');
          statement = false;
          indent    = false;
        } else if (indent) {
          printer.pln('.').indentMore();
          indent = false;
        } else {
          printer.p('.');
        }
        printer.p("add(");

      } else if (analyzer.mayBeNull(b.element)) {
        // A possibly null list value.  Note: The addAll() expression
        // used to contain an explicit cast to Pair, which has been
        // removed since all bindings are declared with their correct
        // types.
        printer.pln(';').indent().p("if (").p(nullExpr()).p(" != ").p(var(b)).
          p(") ").p(VALUE).p(".addAll(");
        statement = true;
        indent    = false;

      } else {
        // A non-null list value.
        if (statement) {
          printer.pln(';').indent().p(VALUE).p('.');
          statement = false;
          indent    = false;
        } else if (indent) {
          printer.pln('.').indentMore();
          indent = false;
        } else {
          printer.p('.');
        }

        // Note: The addAll() expression used to contain an explicit
        // cast to Pair, which has been removed since all bindings are
        // declared with their correct types.
        printer.p("addAll(");
      }
      printer.p(var(b)).p(')');
    }
  }

  /** Emit the class name of the class creating generic nodes. */
  protected void emitFactoryName() {
    if (null == factoryClassName) {
      printer.p("GNode");
    } else {
      printer.p(factoryClassName);
    }
  }

  /**
   * Emit a statement adding formatting to a generic node.
   *
   * @param formatting The list of bindings.
   */
  protected void emitFormatting(List<Binding> formatting) {
    final int size = formatting.size();
    if (0 == size) return;

    printer.indent().p(VALUE).p(" = Formatting.");

    if (1 == size) {
      printer.p("after1(").p(VALUE).p(", ").p(var(formatting.get(0))).p(')');

    } else {
      printer.p("variable().addNode(").p(VALUE).pln(").").indentMore();

      boolean first = true;
      for (Binding b : formatting) {
        if (first) {
          first = false;
        } else {
          printer.p('.');
        }
        printer.p("add(").p(var(b)).p(')');
      }
    }

    printer.pln(';');
  }

  /**
   * Emit the action creating a new generic node.
   *
   * @param v The generic action value.
   */
  protected void emitAction(GenericActionValue v) {
    if (attributeRawTypes) {
      printer.indent().p("public Object run(Object ").p(v.first).pln(") {").
        incr();
    } else {
      printer.indent().p("public Node run(Node ").p(v.first).pln(") {").incr();
    }

    final String  name        = Utilities.unqualify(v.name);
    final int     numChildren = numberOfChildren(1, v.children);
    final boolean defineValue = ((Integer.MIN_VALUE == numChildren) ||
                                 (0 < v.formatting.size()));
    if (defineValue) {
      printer.indent().p("Node ").p(VALUE).p(" = ");
    } else {
      printer.indent().p("return ");
    }
    emitFactoryName();

    boolean emitAdditions = true;
    if (runtime.test("optimizeGenericNodes") && (0 <= numChildren)) {
      if (1 == numChildren) {
        printer.p(".create(\"").p(name).p("\", ").p(v.first).p(')');
        emitAdditions = false;

      } else  if (GNode.MAX_FIXED >= numChildren) {
        printer.p(".create(\"").p(name).p("\", ").p(v.first).p(", ");
        for (Iterator<Binding> iter = v.children.iterator(); iter.hasNext(); ) {
          Binding b = iter.next();
          printer.p(var(b));
          if (iter.hasNext()) {
            printer.p(", ");
          } else {
            printer.p(')');
          }
        }
        emitAdditions = false;

      } else if (1 == v.children.size()) {
        Binding b = v.children.get(0);
        // Note: The cast to Pair for b.name has been removed.
        printer.p(".createFromPair(\"").p(name).p("\", ").p(v.first).
          p(", ").p(var(b)).p(')');
        emitAdditions = false;
      }
    }

    if (emitAdditions) {
      printer.p(".create(\"").p(name).p("\", ");
      emitNumberOfChildren(1, v.children);
      emitChildren(v.first, v.children);
    }

    printer.pln(';');

    emitFormatting(v.formatting);

    if (defineValue) {
      printer.indent().p("return ").p(VALUE).pln(';');
    }
    printer.decr().indent().p('}');
  }

  /** Generate code for the specified generic node value. */
  public void visit(GenericNodeValue v) {
    printer.pln();
    printer.indent().p(VALUE).p(" = ");
    emitFactoryName();

    final String name          = Utilities.unqualify(v.name);
    final int    numChildren   = numberOfChildren(0, v.children);
    boolean      emitAdditions = true;
    if (runtime.test("optimizeGenericNodes") && (0 <= numChildren)) {
      if (0 == numChildren) {
        printer.p(".create(\"").p(name).p("\", false)");
        emitAdditions = false;

      } else if (GNode.MAX_FIXED >= numChildren) {
        printer.p(".create(\"").p(name).p("\", ");
        for (Iterator<Binding> iter = v.children.iterator(); iter.hasNext(); ) {
          Binding b = iter.next();
          printer.p(var(b));
          if (iter.hasNext()) {
            printer.p(", ");
          } else {
            printer.p(')');
          }
        }
        emitAdditions = false;

      } else if (1 == v.children.size()) {
        Binding b = v.children.get(0);
        // Note: The cast to Pair has been removed.
        printer.p(".createFromPair(\"").p(name).p("\", ").p(var(b)).p(')');
        emitAdditions = false;

      } else if (2 == v.children.size()) {
        Binding b1 = v.children.get(0);
        Binding b2 = v.children.get(1);

        if ((! AST.isList(analyzer.type(b1.element))) &&
            AST.isList(analyzer.type(b2.element))) {
          // Note: The cast to Pair for b2.name has been removed.
          printer.p(".createFromPair(\"").p(name).p("\", ").p(var(b1)).
            p(", ").p(var(b2)).p(')');
          emitAdditions = false;
        }
      }
    }

    if (emitAdditions) {
      printer.p(".create(\"").p(name).p("\", ");
      emitNumberOfChildren(0, v.children);
      emitChildren(null, v.children);
    }

    printer.pln(';');

    // If the location optimization is enabled and yyValue is declared
    // to be a node, then add the source location directly to the just
    // created node.  Otherwise, just record that the alternative
    // creates a node value.
    if (attributeWithLocation &&
        runtime.test("optimizeLocation") &&
        AST.isNode(analyzer.current().type)) {
      printer.indent().p(VALUE).p(".setLocation(location(").p(ARG_INDEX).
        pln("));");

    } else {
      createsNodeValue = true;
    }

    emitFormatting(v.formatting);

    nextElement();
  }

  /** Generate code for the specified generic action value. */
  public void visit(GenericActionValue v) {
    printer.pln();
    if (attributeRawTypes) {
      printer.indent().p(VALUE).pln(" = new Action() {").incr();
    } else {
      printer.indent().p(VALUE).pln(" = new Action<Node>() {").incr();
    }

    emitAction(v);

    printer.pln("};").decr();

    nextElement();
  }

  /** Generate code for the specified generic recursion value. */
  public void visit(GenericRecursionValue v) {
    printer.pln();
    if (attributeRawTypes) {
      printer.indent().p(VALUE).pln(" = new Pair(new Action() {").incr();
    } else {
      printer.indent().p(VALUE).
        pln(" = new Pair<Action<Node>>(new Action<Node>() {").incr();
    }

    emitAction(v);

    printer.p("}, ").p(var(v.list)).pln(");").decr();

    nextElement();
  }

}
