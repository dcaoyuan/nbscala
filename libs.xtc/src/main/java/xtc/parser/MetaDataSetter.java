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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import xtc.Constants;
import xtc.Constants.FuzzyBoolean;

import xtc.tree.Visitor;

import xtc.type.AST;
import xtc.type.Type;
import xtc.type.Wildcard;

import xtc.util.Runtime;
import xtc.util.Utilities;

/**
 * Visitor to fill in the production meta-data.  Note that this
 * visitor requires that a grammar's {@link Properties#GENERIC} and
 * {@link Properties#RECURSIVE} have been appropriately set.  Also
 * note that this visitor does not create meta-data records; they must
 * be created with the {@link MetaDataCreator meta-data creator}
 * before applying this visitor.  Further note that this visitor does
 * not determine usage and self counts, as they need to be
 * (repeatedly) determined during {@link DeadProductionEliminator dead
 * production elimination}.  Finally, note that this visitor assumes
 * that the entire grammar is contained in a single module.
 *
 * @author Robert Grimm
 * @version $Revision: 1.56 $
 */
public class MetaDataSetter extends Visitor {

  /** The regular expression for matching import declarations. */
  public static final Pattern IMPORT =
    Pattern.compile("import\\s+(static\\s+)??(\\S+?)(\\.\\*)??;");

  /** The runtime. */
  protected final Runtime runtime;

  /** The analyzer utility. */
  protected final Analyzer analyzer;

  /** The type operations. */
  protected final AST ast;

  /**
   * Flag for whether the grammar has the {@link
   * Constants#ATT_WITH_LOCATION withLocation} attribute.
   */
  protected boolean withLocation;

  /**
   * Flag for whether the grammar has the {@link
   * Constants#ATT_PARSE_TREE parseTree} attribute.
   */
  protected boolean hasParseTree;

  /** Flag for whether the grammar requires {@link xtc.tree.Locatable}. */
  protected boolean requiresLocatable;

  /** Flag for whether a production requires a character variable. */
  protected boolean requiresChar;

  /** Flag for whether a production requires an index variable. */
  protected boolean requiresIndex;

  /** Flag for whether a production requires a result variable. */
  protected boolean requiresResult;

  /** Flag for whether a production requires a predicate index variable. */
  protected boolean requiresPredIndex;

  /** Flag for whether a production requires a predicate result variable. */
  protected boolean requiresPredResult;

  /** Flag for whether a production requires a predicate matched variable. */
  protected boolean requiresPredMatch;

  /** Flag for whether a production requires a base index variable. */
  protected boolean requiresBaseIndex;

  /** The structure of repetitions. */
  protected List<Boolean> repetitions;

  /** The structure of bound repetitions. */
  protected List<Type> boundRepetitions;

  /** The structure of options. */
  protected List<Type> options;

  /** Flag for whether the current production may create a node value. */
  protected boolean createsNodeValue;
  
  /**
   * Flag for whether the current element is the top-level element of
   * a production.
   */
  protected boolean isTopLevel;

  /** Flag for whether the current sequence is repeated. */
  protected boolean isRepeated;

  /** Flag for whether the current sequence is optional. */
  protected boolean isOptional;
  
  /**
   * Flag for whether the current element is the first element of a
   * sequence.
   */
  protected boolean isFirstElement;

  /** Flag for whether the next element is bound. */
  protected boolean isBound;
  
  /** Flag for whether we are analyzing a predicate. */
  protected boolean isPredicate;
  
  /** Flag for whether we are analyzing a not-followed-by predicate. */
  protected boolean isNotFollowedBy;

  /**
   * Flag for whether the current element is the last element in a
   * predicate.
   */
  protected boolean isLastInPredicate;

  /** The current nesting level for repetitions. */
  protected int repetitionLevel;

  /** The current nesting level for options. */
  protected int optionLevel;
  
  /**
   * Create a new meta-data setter.
   *
   * @param runtime The runtime.
   * @param analyzer The analyzer utility.
   * @param ast The type operations.
   */
  public MetaDataSetter(Runtime runtime, Analyzer analyzer, AST ast) {
    this.runtime  = runtime;
    this.analyzer = analyzer;
    this.ast      = ast;
  }

  /**
   * Import the specified fully qualified type.
   *
   * @param name The type name.
   */
  protected void importType(String name) {
    ast.importType(name, Utilities.getName(name));
  }

  /** Analyze the specified grammar. */
  public void visit(Module m) {
    // Initialize the per-grammar state.
    analyzer.register(this);
    analyzer.init(m);

    // Process the grammar's imports.
    final String pkg = Utilities.getQualifier(m.getClassName());
    if (null != pkg) ast.importModule(pkg + ".");

    importType("java.io.Reader");
    if (m.hasAttribute(Constants.NAME_MAIN)) {
      importType("java.io.BufferedReader");
      importType("java.io.BufferedWriter");
      importType("java.io.File");
      importType("java.io.FileReader");
      importType("java.io.OutputStreamWriter");
    }
    importType("java.io.IOException");

    if (m.hasAttribute(Constants.ATT_PROFILE)) {
      importType("java.util.HashMap");
    }
    if (m.hasAttribute(Constants.NAME_STRING_SET)) {
      importType("java.util.HashSet");
      importType("java.util.Set");
    }

    if (m.getBooleanProperty(Properties.RECURSIVE)) {
      importType("xtc.util.Action");
    }
    importType("xtc.util.Pair");

    if (m.hasAttribute(Constants.ATT_WITH_LOCATION)) {
      // Assume that we always import this interface.
      importType("xtc.tree.Locatable");
    }
    if (m.getBooleanProperty(Properties.GENERIC) ||
        m.hasAttribute(Constants.NAME_MAIN)) {
      importType("xtc.tree.Node");
    }
    if (m.getBooleanProperty(Properties.GENERIC)) {
      if (m.hasAttribute(Constants.NAME_FACTORY)) {
        String factory = (String)m.getAttributeValue(Constants.NAME_FACTORY);
        if (Utilities.isQualified(factory)) {
          importType(factory);
        }
      } else {
        importType("xtc.tree.GNode");
      }
    }
    if (m.hasAttribute(Constants.ATT_PARSE_TREE)) {
      importType("xtc.tree.Token");
      importType("xtc.tree.Formatting");
    }
    if (m.hasAttribute(Constants.ATT_VERBOSE) ||
        m.hasAttribute(Constants.NAME_MAIN) ||
        m.hasAttribute(Constants.ATT_PROFILE) ||
        m.hasAttribute(Constants.ATT_DUMP)) {
      importType("xtc.tree.Printer");
    }
    if (m.hasAttribute(Constants.NAME_PRINTER)) {
      importType("xtc.tree.Visitor");
    }

    importType("xtc.parser.ParserBase");
    importType("xtc.parser.Column");
    importType("xtc.parser.Result");
    importType("xtc.parser.SemanticValue");
    importType("xtc.parser.ParseError");

    if (null != m.header) {
      for (String line : m.header.code) {
        Matcher matcher = IMPORT.matcher(line);

        if (matcher.lookingAt()) {
          if (null == matcher.group(3)) {
            String name = matcher.group(2);
            try {
              ast.importType(name, Utilities.getName(name));
            } catch (IllegalArgumentException x) {
              runtime.error("inconsistent imports for '" +
                            Utilities.getName(name) + "'", m.header);
            }
          } else if (null == matcher.group(1)) {
            ast.importModule(matcher.group(2) + ".");
          } else {
            ast.importModule(matcher.group(2) + "$");
          }
        }
      }
    }

    // Initialize per-grammar flags.
    withLocation      = m.hasAttribute(Constants.ATT_WITH_LOCATION);
    hasParseTree      = m.hasAttribute(Constants.ATT_PARSE_TREE);
    requiresLocatable = false;

    // Visit all productions.
    for (Production p : m.productions) analyzer.process(p);

    // Record use of locatable interface.
    if (requiresLocatable) {
      m.setProperty(Properties.LOCATABLE, Boolean.TRUE);
    }
  }

  /** Analyze the specified production. */
  public void visit(Production p) {
    MetaData md = (MetaData)p.getProperty(Properties.META_DATA);

    // Initialize per-production flags.
    requiresChar       = false;
    requiresIndex      = false;
    requiresResult     = false;
    requiresPredIndex  = false;
    requiresPredResult = false;
    requiresPredMatch  = false;
    requiresBaseIndex  = false;
    repetitions        = md.repetitions;
    boundRepetitions   = md.boundRepetitions;
    options            = md.options;
    createsNodeValue   = false;
    isTopLevel         = true;
    isRepeated         = false;
    isOptional         = false;
    isFirstElement     = false;
    isBound            = false;
    isPredicate        = false;
    isNotFollowedBy    = false;
    isLastInPredicate  = false;
    repetitionLevel    = 0;
    optionLevel        = 0;

    // Visit the element.
    dispatch(p.choice);

    // Check the type.
    if (withLocation && createsNodeValue) {
      if (FuzzyBoolean.MAYBE == ast.hasLocation(p.type)) {
        requiresLocatable = true;
      }
    }

    // Copy flags into meta-data record.
    md.requiresChar       = requiresChar;
    md.requiresIndex      = requiresIndex;
    md.requiresResult     = requiresResult;
    md.requiresPredIndex  = requiresPredIndex;
    md.requiresPredResult = requiresPredResult;
    md.requiresPredMatch  = requiresPredMatch;
    md.requiresBaseIndex  = requiresBaseIndex;

    // Patch the types for bound repetitions.
    int size = boundRepetitions.size();
    for (int i=0; i<size; i++) {
      Type t = boundRepetitions.get(i);
      if (null != t) {
        boundRepetitions.set(i, AST.listOf(ast.concretize(t, AST.ANY)));
      }
    }

    // Patch the types for bound options.
    size = options.size();
    for (int i=0; i<size; i++) {
      Type t = options.get(i);
      if (null != t) {
        options.set(i, ast.concretize(t, AST.ANY));
      }
    }
  }
  
  /** Analyze the specified ordered choice. */
  public void visit(OrderedChoice c) {
    final boolean top = isTopLevel;
    isTopLevel        = false;

    for (Sequence alt : c.alternatives) {
      if (top) isFirstElement = true;
      dispatch(alt);
    }
  }

  /** Analyze the specified repetition. */
  public void visit(Repetition r) {
    isTopLevel       = false;
    boolean repeated = isRepeated;
    isRepeated       = true;
    boolean optional = isOptional;
    isOptional       = false;
    isFirstElement   = false;
    boolean bound    = isBound;
    isBound          = false;
    repetitionLevel++;

    if (repetitions.size() < repetitionLevel) {
      repetitions.add(Boolean.FALSE);
      boundRepetitions.add(null);
    }
    if (r.once) {
      repetitions.set(repetitionLevel - 1, Boolean.TRUE);
    }
    if (bound) {
      // Make sure the type that level is initialized.
      if (null == boundRepetitions.get(repetitionLevel-1)) {
        boundRepetitions.set(repetitionLevel-1, Wildcard.TYPE);
      }

      // Get the binding, determine the bound element's type, and then
      // unify that type with any previously determined element type.
      final Binding b1    = Analyzer.getBinding(((Sequence)r.element).elements);
      final Type    t1    = analyzer.type(b1.element);
      final Type    unity =
        ast.unify(t1, boundRepetitions.get(repetitionLevel-1), false);
      boundRepetitions.set(repetitionLevel-1, unity);
    }

    dispatch(r.element);

    isRepeated = repeated;
    isOptional = optional;
    repetitionLevel--;
  }

  /** Analyze the specified option. */
  public void visit(Option o) {
    isTopLevel       = false;
    boolean repeated = isRepeated;
    isRepeated       = false;
    boolean optional = isOptional;
    isOptional       = false;
    isFirstElement   = false;
    boolean bound    = isBound;
    isBound          = false;
    optionLevel++;

    if (options.size() < optionLevel) {
      options.add(null);
    }
    if (bound) {
      // Make sure the type at that level is initialized.
      if (null == options.get(optionLevel-1)) {
        options.set(optionLevel-1, Wildcard.TYPE);
      }

      // Get the binding, determine the bound element's type, and then
      // unify that type with any previously determined type.
      final Binding b1    = Analyzer.getBinding(((Sequence)o.element).elements);
      final Type    t1    = analyzer.type(b1.element);
      final Type    unity = ast.unify(t1, options.get(optionLevel-1), false);
      options.set(optionLevel-1, unity);
    }

    dispatch(o.element);

    isRepeated = repeated;
    isOptional = optional;
    optionLevel--;
  }
  
  /** Analyze the specified sequence. */
  public void visit(Sequence s) {
    isTopLevel       = false;
    boolean repeated = isRepeated;
    isRepeated       = false;
    boolean optional = isOptional;
    isOptional       = false;
    isBound          = false;

    final int size   = s.size();
    for (int i=0; i<size; i++) {
      isLastInPredicate =
        isPredicate && (! repeated) && (! optional) && (i == size-1);
      dispatch(s.get(i));
    }

    isRepeated = repeated;
    isOptional = optional;
  }
  
  /** Analyze the specified followed-by predicate. */
  public void visit(FollowedBy p) {
    isTopLevel = false;
    isBound    = false;
    
    boolean first     = isFirstElement;
    isPredicate       = true;
    isNotFollowedBy   = false;
    
    dispatch(p.element);
    
    isPredicate       = false;
    isFirstElement    = first;
  }
  
  /**
   * Determine whether we are processing a not-followed-by predicate.
   *
   * @return <code>true</code> if we are processing a not-followed-by
   * predicate.
   */
  protected boolean isNotFollowedBy() {
    return (isPredicate && isNotFollowedBy);
  }
  
  /** Analyze the specified not-followed-by predicate. */
  public void visit(NotFollowedBy p) {
    isTopLevel        = false;
    isBound           = false;

    requiresPredMatch = true;

    boolean first     = isFirstElement;
    isPredicate       = true;
    isNotFollowedBy   = true;
    
    dispatch(p.element);
    
    isPredicate       = false;
    isFirstElement    = first;
  }

  /** Analyze the specified semantic predicate. */
  public void visit(SemanticPredicate p) {
    isTopLevel = false;
    // No change to parser, therefore no change to isFirstElement.
    isBound    = false;

    dispatch(p.element);
  }

  /** Analyze the specified voided element. */
  public void visit(VoidedElement v) {
    isTopLevel = false;
    // No change to parser, therefore no change to isFirstElement.
    isBound    = false;

    dispatch(v.element);
  }
  
  /** Analyze the specified binding. */
  public void visit(Binding b) {
    isTopLevel = false;
    // No change to parser, therefore no change to isFirstElement.
    isBound    = true;
    
    dispatch(b.element);
  }

  /** Analyze the specified string match. */
  public void visit(StringMatch m) {
    isTopLevel          = false;
    isBound             = false;

    // Determine if we need a base index variable.
    if ((! isNotFollowedBy()) &&
        ((! runtime.test("optimizeErrors1")) || (! isFirstElement))) {
      requiresBaseIndex = true;
    }
    isFirstElement      = false;

    dispatch(m.element);
  }
  
  /** Analyze the specified nonterminal. */
  public void visit(NonTerminal nt) {
    isTopLevel           = false;
    isFirstElement       = false;
    isBound              = false;

    if (isPredicate) {
      requiresPredResult = true;
    } else {
      requiresResult     = true;
    }
  }

  /** Analyze the specified any character element. */
  public void visit(AnyChar a) {
    isTopLevel            = false;
    isFirstElement        = false;
    isBound               = false;
    
    requiresChar          = true;
    if (isPredicate) {
      if (! isLastInPredicate) {
        requiresPredIndex = true;
      }
    } else {
      requiresIndex       = true;
    }
  }

  /** Analyze the specified string literal. */
  public void visit(StringLiteral l) {
    isTopLevel            = false;
    isBound               = false;

    // Determine if we need a base index variable.
    if ((! isNotFollowedBy()) &&
        ((! runtime.test("optimizeErrors1")) || (! isFirstElement))) {
      requiresBaseIndex   = true;
    }
    isFirstElement        = false;

    requiresChar          = true;
    if (isPredicate) {
      if ((! isLastInPredicate) || (1 < l.text.length())) {
        requiresPredIndex = true;
      }
    } else {
      requiresIndex       = true;
    }
  }

  /** Analyze the specified character case. */
  public void visit(CharCase c) {
    if (null != c.element) {
      dispatch(c.element);
    }
  }

  /** Analyzer the specified character switch. */
  public void visit(CharSwitch s) {
    isTopLevel            = false;
    isFirstElement        = false;
    isBound               = false;

    requiresChar          = true;
    if (isPredicate) {
      if (! isLastInPredicate) {
        requiresPredIndex = true;
      }
    } else {
      requiresIndex       = true;
    }

    for (CharCase kase : s.cases) {
      dispatch(kase);
    }
    dispatch(s.base);
  }
  
  /** Analyze the specified terminal. */
  public void visit(Terminal t) {
    isTopLevel            = false;
    isFirstElement        = false;
    isBound               = false;

    requiresChar          = true;
    if (isPredicate) {
      if (! isLastInPredicate) {
        requiresPredIndex = true;
      }
    } else {
      requiresIndex       = true;
    }
  }

  /** Analyze the specified action. */
  public void visit(Action a) {
    if (a.setsValue()) createsNodeValue = true;
    isTopLevel = false;
    // No change to parser, therefore no change to isFirstElement.
    isBound    = false;
  }

  /** Analyze the specified parser action. */
  public void visit(ParserAction pa) {
    createsNodeValue  = true;
    isTopLevel        = false;
    isFirstElement    = false;
    isBound           = false;

    requiresBaseIndex = true;
  }

  /**
   * Determine whether the current production can annotate a node with
   * its location relative to {@link CodeGenerator#VALUE}.
   *
   * @return <code>true</code> if the location annotation can be
   *   optimized.
   */
  protected boolean hasDirectLocation() {
    return (withLocation &&
            runtime.test("optimizeLocation") &&
            AST.isNode(analyzer.current().type));
  }

  /** Analyze the specified token value. */
  public void visit(TokenValue v) {
    if (! hasDirectLocation()) createsNodeValue = true;
    isTopLevel = false;
    // No change to parser, therefore no change to isFirstElement.
    isBound    = false;
  }

  /** Analyze the specified action base value. */
  public void visit(ActionBaseValue v) {
    isTopLevel  = false;
    // No change to parser, therefore no change to isFirstElement.
    isBound     = false;
  }

  /** Analyze the specified generic value. */
  public void visit(GenericValue v) {
    isTopLevel = false;
    // No change to parser, therefore no change to isFirstElement.
    isBound    = false;
  }

  /**
   * Analyze the specified generic action value.  Note that generic
   * recursion values are also generic action values.
   */
  public void visit(GenericActionValue v) {
    if (! hasDirectLocation()) createsNodeValue = true;
    isTopLevel  = false;
    // No change to parser, therefore no change to isFirstElement.
    isBound     = false;
  }

  /**
   * Analyze the specified element.  This method provides the default
   * implementation for parse tree nodes, null literals, node markers,
   * and value elements (besides token values, action base values, and
   * generic values).
   */
  public void visit(Element e) {
    isTopLevel = false;
    // No change to parser, therefore no change to isFirstElement.
    isBound    = false;
  }

}
