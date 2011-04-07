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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import xtc.Constants;

import xtc.tree.Utility;
import xtc.tree.Visitor;

import xtc.type.AST;
import xtc.type.Type;
import xtc.type.Wildcard;
import xtc.type.VoidT;

import xtc.util.Utilities;

/** 
 * Utility for analyzing and modifying grammar modules.  This class
 * provides functionality that helps process either a {@link Grammar
 * collection of modules} or a single {@link Module module}.  In
 * particular, it provides:</ul>
 *
 * <li>A mapping from module names to modules, which is initialized
 * through {@link #init(Grammar)} and accessed through {@link
 * #lookup(String)} and {@link #lookup(ModuleName)}.  After
 * initialization, the grammar can be accessed through {@link
 * #grammar()}.<p /></li>
 *
 * <li>A mapping from nonterminals to productions, which is
 * initialized either through {@link #init(Grammar)} or {@link
 * #init(Module)} and accessed through {@link #lookup(NonTerminal)},
 * {@link #lookup(NonTerminal,Module)}, and {@link
 * #lookupGlobally(NonTerminal)}.  When initializing this mapping
 * through a grammar, {@link #lookup(NonTerminal) relative look ups}
 * can only be performed within the context of {@link #process(Module)
 * processing} a module.  Note that the mapping contains both
 * qualified and unqualified names.<p /></li>
 *
 * <li>Methods to {@link #isTopLevel(Module) test for} and {@link
 * #topLevel() access} the top-level (or root) module of a grammar.<p
 * /></li>
 *
 * <li>Methods to {@link #add(Module) add} and {@link #remove(Module)
 * remove} modules.  These methods update both the mapping from module
 * names to modules and from nonterminals to productions, but they do
 * not update the current grammar.<p /></li>
 *
 * <li>Methods to {@link #isImported(String,Module) test} whether a
 * module is imported by another, to {@link #trace trace} a module's
 * dependencies, and to {@link #hasAttribute(Module,String,Set) test}
 * whether a module or any of its dependencies has a specified
 * attribute.<p /></li>
 *
 * <li>Methods to {@link #isDefined(Production,Module) test} whether a
 * production is defined by a module and to {@link
 * #isImported(Production,Module) test} whether a production is
 * imported by a module.<p /></li>
 *
 * <li>Methods to start {@link #process(Module) processing} a module
 * and to {@link #currentModule() determine} the current module;
 * additionally, methods to {@link #enter(Production) switch to} a
 * different module and {@link #exit(Object) switch back} again when
 * processing productions from several modules.<p /></li>
 *
 * <li>A method to uniquely {@link #uniquify()} the names of all
 * productions across all modules.<p /></li>
 *
 * <li>Methods to start {@link #process(Production) processing} a
 * production and to {@link #current() determine} the current
 * production.<p /></li>
 *
 * <li>A working set, marked set, and processed set to determine
 * properties of productions.  The idea behind these three sets is
 * that the working set keeps track of all productions during an
 * analysis pass and is used to prevent infinite recursions, the
 * marked set tracks the productions having the property, and the
 * processed set (which typically is a superset of the marked set)
 * tracks the analyzed productions.  The working set is accessed
 * through {@link #workingOn(NonTerminal)}, {@link
 * #notWorkingOn(NonTerminal)}, {@link #notWorkingOnAny()}, {@link
 * #isBeingWorkedOn(NonTerminal)}, and {@link #working()}.  The marked
 * set is accessed through {@link #mark(NonTerminal)}, {@link
 * #mark(Collection)}, {@link #markAll()}, {@link
 * #unmark(NonTerminal)}, {@link #unmarkAll()}, {@link #hasMarked()},
 * {@link #isMarked(NonTerminal)}, and {@link #marked()}.  Finally,
 * the processed set is accessed through {@link #clearProcessed()},
 * {@link #processed(NonTerminal)}, and {@link
 * #isProcessed(NonTerminal)}.  Note that all nonterminals used in
 * these sets should be fully qualified.<p /></li>
 *
 * <li>Methods to add and remove productions from a grammar.  New
 * productions are prepared for addition through {@link
 * #add(FullProduction)} and committed to the grammar through {@link
 * #addNewProductionsAt(int)}.  Existing productions are removed
 * through {@link #remove(FullProduction)}.<p /></li>
 *
 * <li>A set of methods for creating synthetic variable names and
 * nonterminals for new productions: {@link #variable()}, {@link
 * #split()}, {@link #choice()}, {@link #star()}, {@link #plus()},
 * {@link #option()}, {@link #tail()}, and {@link #shared()}.  Also, a
 * method to test whether a given {@link #isSynthetic(String)
 * variable} or {@link #isSynthetic(NonTerminal) nonterminal} is
 * synthetic.  Besides shared variables, synthetic variables can only
 * be created while {@link #process(Production) processing} a
 * production.<p /></li>
 *
 * <li>A method to {@link #strip(Element) strip} unnecessary ordered
 * choices and sequences from an element and a method to {@link
 * #stripChoices(OrderedChoice) strip} only ordered choices.<p /></li>
 *
 * <li>A method to {@link #copy(Element) copy} an element.<p /></li>
 *
 * <li>A set of methods for optimizing sequences that start with
 * terminals through character switches: {@link
 * #hasTerminalPrefix(Sequence)}, {@link
 * #normalizeTerminals(Sequence)}, and {@link
 * #joinTerminals(Sequence,Element)}.<p /></li>
 *
 * <li>The corresponding set of methods for folding common prefixes:
 * {@link #haveCommonPrefix(Sequence,Sequence)}, {@link
 * #normalizePrefix(Sequence,Sequence)}, and {@link
 * #joinPrefixes(Sequence,Element)}.<p /></li>
 *
 * <li>A method to {@link #matchingText(Element) get the text} of an
 * element.<p /></li>
 *
 * <li>A method to determine whether an element {@link
 * #restrictsInput(Element) restricts the input}.<p /></li>
 *
 * <li>A method to determine whether an element {@link
 * #consumesInput(Element) consumes the input}.<p /></li>
 *
 * <li>A method to determine whether an element {@link
 * #matchesEmpty(Element) matches the empty input}.<p /></li>
 *
 * <li>A method to determine whether an element {@link
 * #isNotFollowedBy(Element) relies only on not-followed-by
 * predicates}.<p /></li>
 *
 * <li>A set of methods to process bindings, notably a method to
 * determine whether an element {@link #isBindable(Element) can be
 * bound}, a method to {@link #bind(List) add a binding} to a list of
 * elements, a method to {@link #getBinding(List) access the only
 * binding} in a list of elements, and methods to {@link
 * #unbind(Element) unbind} and {@link #stripAndUnbind(Element) strip
 * and unbind} an element.<p /></li>
 *
 * <li>A method to {@link #getValue(List,boolean) get a list's
 * value}.</li>
 *
 * <li>Two methods to determine whether {@link
 * #setsValue(Element,boolean) an element} or {@link
 * #setsValue(List,boolean) a list} sets the semantic value, either
 * through a binding, semantic action, or value element.  Another
 * method to {@link #setsNullValue(List) determine} whether a list
 * sets the semantic value to <code>null</code>.<p /></li>
 *
 * <li>A method to determine whether an element's list value {@link
 * #mayBeNull(Element) may be null}.<p /></li>
 *
 * <li>A method to {@link #type(Element) type} an element.<p /></li>
 *
 * </ul>
 *
 * To utilize this analyzer, the utility must be initialized with
 * {@link #init(Grammar) a grammar} or {@link #init(Module) a module}
 * and the visitor must be {@link #register registered}.  When
 * processing modules in a grammar, this utility must be {@link
 * #process(Module) notified}.  When processing productions, this
 * utility must also be {@link #process(Production) notified}.
 *
 * <p />The analyzer utility tracks the current grammar or module so
 * that it need not recreate its internal state as long as the same
 * analyzer utility is used across different visitors.
 *
 * @author Robert Grimm
 * @version $Revision: 1.147 $
 */
public class Analyzer extends Utility {

  /**
   * The separator character for creating new nonterminals, which
   * should be illegal in regular variable or nonterminal names.
   *
   */
  public static final String SEPARATOR = "$$";

  /** The base name for nonterminals representing shared productions. */
  public static final String SHARED = SEPARATOR + "Shared";

  /** The base name for synthetic variables. */
  public static final String VARIABLE = "v$";

  /** The name for dummy variables (in case of errors). */
  public static final String DUMMY = VARIABLE + "dummy";

  /** The suffix for nonterminals representing split alternatives. */
  public static final String SPLIT = SEPARATOR + "Split";

  /** The suffix for nonterminals representing choices. */
  public static final String CHOICE = SEPARATOR + "Choice";
  
  /**
   * The suffix for nonterminals representing zero or more
   * repetitions.
   */
  public static final String STAR = SEPARATOR + "Star";

  /**
   * The suffix for nonterminals representing one or more
   * repetitions.
   */
  public static final String PLUS = SEPARATOR + "Plus";

  /** The suffix for nonterminals representing options. */
  public static final String OPTION = SEPARATOR + "Option";

  /** The suffix for nonterminals representing tail productions. */
  public static final String TAIL = SEPARATOR + "Tail";

  /**
   * The maximum character count for turning character classes into
   * character switches.
   */
  public static final int MAX_COUNT = 22;

  // =======================================================================

  /** The element copier. */
  protected final Copier xerox;

  // =======================================================================

  /** Flag for whether we are processing a grammar or a module. */
  protected boolean isGrammarMode = false;

  // =======================================================================

  /** The grammar. */
  protected Grammar grammar;

  /** The map from module names (as strings) to module objects. */
  protected Map<String, Module> moduleMap;

  /**
   * The grammar-wide map from nonterminals to productions.  For each
   * production, this map contains two entries, one mapping the fully
   * qualified name to the production, the other mappiing the
   * unqualified name to either a production (if the set of modules
   * only contains a single production with the unqualified name) or a
   * list of productions (if the set of resolved modules contains more
   * than one production with the unqualified name).  Note that this
   * map does not contain partial productions.
   */
  protected Map<NonTerminal, Object> grammarPMap;

  /** The current grammar module. */
  protected Module mCurrent;

  // =======================================================================

  /** The self-contained grammar module. */
  protected Module module;

  /** The map from nonterminals to productions for the current module. */
  protected Map<NonTerminal, FullProduction> pMap;

  /** The current production. */
  protected Production pCurrent;

  // =======================================================================

  /**
   * The set of nonterminals corresponding to productions currently
   * being processed.
   */
  protected Set<NonTerminal> pWorking;

  /**
   * The set of nonterminals corresponding to productions having
   * been marked.
   */
  protected Set<NonTerminal> pMarked;

  /**
   * The set of nonterminals corresponding to productions having
   * been processed.
   */
  protected Set<NonTerminal> pProcessed;

  /** The list of newly added productions. */
  protected List<Production> pNew;

  /** The count of synthetic variables for the current production. */
  protected int varCount;

  /** The count of splits for the current production. */
  protected int splitCount;

  /** The count of lifted choices for the current production. */
  protected int choiceCount;

  /** The count of desugared star repetitions for the current production. */
  protected int starCount;

  /** The count of desugared plus repetitions for the current production. */
  protected int plusCount;

  /** The count of desugared options for the current production. */
  protected int optionCount;

  /** The count of tail productions for the current production. */
  protected int tailCount;

  /** The count of shared productions. */
  protected int sharedCount;

  // =======================================================================

  /** Create a new analyzer utility. */
  public Analyzer() {
    xerox       = new Copier();
    moduleMap   = new HashMap<String, Module>();
    grammarPMap = new HashMap<NonTerminal, Object>();
    pMap        = new HashMap<NonTerminal, FullProduction>();
    pWorking    = new HashSet<NonTerminal>();
    pMarked     = new HashSet<NonTerminal>();
    pProcessed  = new HashSet<NonTerminal>();
    pNew        = new ArrayList<Production>();
    sharedCount = 1;
  }

  /** Forcibly reset the analyzer utility. */
  public void reset() {
    isGrammarMode  = false;
    grammar        = null;
    moduleMap.clear();
    grammarPMap.clear();
    mCurrent       = null;
    module         = null;
    pCurrent       = null;
    pMap.clear();
    pWorking.clear();
    pMarked.clear();
    pProcessed.clear();
    pNew.clear();
    varCount       = 1;
    splitCount     = 1;
    choiceCount    = 1;
    starCount      = 1;
    plusCount      = 1;
    optionCount    = 1;
    tailCount      = 1;
    sharedCount    = 1;
  }

  // =======================================================================

  /**
   * Initialize this analyzer for the specified grammar.  This method
   * initializes the map from module names to modules and the
   * grammar-wide map from nonterminals to productions.  It also sets
   * the {@link Production#qName qualified name} field for each
   * production (if it has not been initialized).  This method also
   * clears the sets of marked and processed nonterminals.  It should
   * be called before iterating over the grammar's modules.
   *
   * @param g The grammar.
   */
  public void init(Grammar g) {
    // Initialize the map from module names to modules and the
    // grammar-wide map from nonterminals to productions.
    if (grammar != g) {
      grammar = g;

      moduleMap.clear();
      grammarPMap.clear();
      
      for (Module m : g.modules) {
        // Record the mapping between module name and module.
        moduleMap.put(m.name.name, m);

        // Record the productions.
        for (Production p : m.productions) {
          // Fill in the qualified name.
          if (null == p.qName) {
            p.qName = p.name.qualify(m.name.name);
            p.qName.setLocation(p.name);
          }
          
          // Record the mappings, but skip partial productions as well
          // as duplicate definitions within the same grammar module.
          if (p.isFull()) {
            addToGrammarMap((FullProduction)p);
          }
        }
      }
    }
    
    // Clear the sets of marked and processed productions.
    pMarked.clear();
    pProcessed.clear();
    
    // Clear the current production.
    pCurrent = null;
    
    // Clear the current module.
    mCurrent = null;

    // Set the processing mode.
    isGrammarMode = true;
  }

  /**
   * Determine whether the module with the specified name is imported
   * by the specified module.
   *
   * @param name The module name.
   * @param m The depending module.
   * @return <code>true</code> if the specified module (or any of the
   *   modules modified by that module) imports the module with the
   *   specified name.
   */
  public boolean isImported(String name, Module m) {
    // Check the specified module and all modules modified by that
    // module.
    while (null != m) {
      if (null != m.dependencies) {
        for (ModuleDependency dep : m.dependencies) {
          if (dep.isImport() && dep.visibleName().name.equals(name)) {
            return true;
          }
        }

        if (null != m.modification) {
          // Continue checking with the modified module.
          m = lookup(m.modification.visibleName());
        } else {
          // No more dependencies to check.
          return false;
        }
      } else {
        // No more dependencies to check.
        return false;
      }
    }

    // No more modules to check.
    return false;
  }

  /**
   * Trace the specified module's dependencies.  This method traces
   * the transitive closure of the specified module's import and
   * modification dependencies.  It adds the names of all imported
   * modules to the imports set (excluding the name of the specified
   * module) and the names of all modified modules to the modified
   * map.  The corresponding value in this mapping is
   * <code>Boolean.TRUE</code> if the module is modified more than
   * once.  Otherwise, it is false.  This method assumes that each
   * module modification's {@link Module#modification modification}
   * field has been correctly initialized.  Furthermore, the specified
   * set and map should be empty, with the exception of the set of
   * imported modules containing the name of the root module.
   *
   * @param m The module.
   * @param imports The imported modules.
   * @param modified The modified modules.
   */
  public void trace(Module m, Set<ModuleName> imports,
                    Map<ModuleName, Boolean> modified) {
    // Trace any modifications.
    if (null != m.modification) {
      if (modified.containsKey(m.modification.visibleName())) {
        modified.put(m.modification.visibleName(), Boolean.TRUE);

      } else {
        modified.put(m.modification.visibleName(), Boolean.FALSE);
        Module m2 = lookup(m.modification.visibleName());
        if (null != m2) {
          trace(m2, imports, modified);
        }
      }
    }

    // Trace any imports.
    if (null != m.dependencies) {
      for (ModuleDependency dep : m.dependencies) {
        if (dep.isImport()) {
          if (! imports.contains(dep.visibleName())) {
            imports.add(dep.visibleName());
            Module m2 = lookup(dep.visibleName());
            if (null != m2) {
              trace(m2, imports, modified);
            }
          }
        }
      }
    }
  }

  /**
   * Determine whether the specified module or any of its import or
   * modification dependencies has an attribute with the specified
   * name.
   *
   * @param m The module.
   * @param name The attribute name.
   * @param visited An empty helper set.
   * @return <code>true</code> if the specified module or any of its
   *   dependencies has the specified attribute.
   */
  public boolean hasAttribute(Module m, String name, Set<ModuleName> visited) {
    if (! visited.contains(m.name)) {
      // Add this module to set of visited modules.
      visited.add(m.name);

      // Check this module's attributes and then dependencies.
      if (m.hasAttribute(name)) {
        return true;

      } else if (null != m.dependencies) {
        for (ModuleDependency dep : m.dependencies) {
          if (dep.isImport() || dep.isModification()) {
            Module m2 = lookup(dep.visibleName());

            if ((null != m2) && hasAttribute(m2, name, visited)) {
              return true;
            }
          }
        }
      }
    }

    // Done.
    return false;
  }

  /**
   * Add the specified module to the current grammar.  This method
   * adds the specified module to the map from module names to modules
   * and its productions to the grammar-wide map from nonterminals to
   * productions.  However, it does not add the specified module to
   * the grammar's {@link Grammar#modules list of modules}.
   *
   * @param m The module.
   * @throws IllegalStateException Signals that this analyzer has not
   *   been initialized with a grammar.
   */
  public void add(Module m) {
    // Make sure we are in the right state.
    if (! isGrammarMode) {
      throw new IllegalStateException("Not initialized with grammar");
    }

    // Only add the module if it is not already part of the grammar.
    if (! moduleMap.containsKey(m.name.name)) {
      // Add the module mapping.
      moduleMap.put(m.name.name, m);
      
      // Add the productions.
      for (Production p : m.productions) {
        if (p.isFull()) {
          addToGrammarMap((FullProduction)p);
        }
      }
    }
  }

  /**
   * Remove the specified module from the current grammar.  This
   * method removes the specified module from the map from module
   * names to modules and its productions from the grammar-wide map
   * from nonterminals to productions.  Note that this method does not
   * remove the specified module from the grammar's {@link
   * Grammar#modules list of modules}.
   *
   * @param m The module.
   * @throws IllegalStateException Signals that this analyzer has not
   *   been initialized with a grammar.
   */
  public void remove(Module m) {
    // Make sure we are in the right state.
    if (! isGrammarMode) {
      throw new IllegalStateException("Not initialized with grammar");
    }

    // Remove the module mapping.
    moduleMap.remove(m.name.name);

    // Remove the productions.
    for (Production p : m.productions) {
      if (p.isFull()) {
        removeFromGrammarMap((FullProduction)p);
      }
    }
  }

  /**
   * Cast the specified object as a list of full productions.
   *
   * @param o The object.
   * @return The object as a list of full productions.
   */
  @SuppressWarnings("unchecked")
  private List<FullProduction> toFullProductionList(Object o) {
    return (List<FullProduction>)o;
  }

  /**
   * Add the specified production to the grammar-wide production map.
   *
   * @param p The production.
   */
  private void addToGrammarMap(FullProduction p) {
    if (! grammarPMap.containsKey(p.qName)) {
      grammarPMap.put(p.qName, p);

      if (grammarPMap.containsKey(p.name)) {
        Object o = grammarPMap.get(p.name);

        // There is more than one production with the same unqualified
        // name, so we keep a list of productions.
        if (o instanceof FullProduction) {
          List<FullProduction> l = new ArrayList<FullProduction>();
          l.add((FullProduction)o);
          l.add(p);
          grammarPMap.put(p.name, l);

        } else {
          List<FullProduction> l = toFullProductionList(o);
          l.add(p);
        }

      } else {
        grammarPMap.put(p.name, p);
      }
    }
  }

  /**
   * Remove the specified production from the grammar-wide production
   * map.
   *
   * @param p The production.
   */
  private void removeFromGrammarMap(FullProduction p) {
    grammarPMap.remove(p.qName);

    Object o = grammarPMap.get(p.name);

    if (o instanceof FullProduction) {
      // Make sure we only remove the unqualified mapping if it
      // actually describes the production to be removed.
      if (p.qName.equals(((FullProduction)o).qName)) {
        grammarPMap.remove(p.name);
      }

    } else {
      List<FullProduction> l = toFullProductionList(o);
      for (Iterator<FullProduction> iter = l.iterator(); iter.hasNext(); ) {
        if (p.qName.equals(iter.next().qName)) {
          iter.remove();
          break;
        }
      }

      if (1 == l.size()) {
        o = l.get(0);
        grammarPMap.put(p.name, o);
      }
    }
  }

  /**
   * Get the initializing grammar.
   *
   * @see #init(Grammar)
   *
   * @return The grammar.
   * @throws IllegalStateException Signals that this analyzer has not
   *   been initialized with a grammar.
   */
  public Grammar grammar() {
    // Make sure we are in the right state.
    if (! isGrammarMode) {
      throw new IllegalStateException("Not initialized with grammar");
    }

    // Do the work.
    return grammar;
  }

  /**
   * Determine whether the specified module is the top-level module.
   *
   * @param m The module.
   * @return <code>true</code> if the specified module is the
   *   top-level module.
   */
  public boolean isTopLevel(Module m) {
    return isGrammarMode? (m == grammar.modules.get(0)) : (m == module);
  }

  /**
   * Get the top-level module.
   *
   * @return The top-level module.
   */
  public Module topLevel() {
    return isGrammarMode? grammar.modules.get(0) : module;
  }

  // =======================================================================

  /**
   * Initialize this analyzer for the specified module.  This method
   * initializes the map from nonterminals to productions.  It also
   * clears the sets of marked and processed nonterminals.  It should
   * be called before iterating over the module's productions.
   *
   * @param m The self-contained module.
   */
  public void init(Module m) {
    // Initialize the map from nonterminals to productions and the set
    // of top-level nonterminals.
    if (module != m) {
      module = m;

      pMap.clear();
      for (Production p : m.productions) {
        if (p.isFull()) {
          pMap.put(p.name, (FullProduction)p);
          pMap.put(p.qName, (FullProduction)p);
        }
      }
    }

    // Clear the sets of marked and processed productions.
    pMarked.clear();
    pProcessed.clear();

    // Clear the current production.
    pCurrent = null;

    // Clear the current module.
    mCurrent = null;

    // Set the processing mode.
    isGrammarMode = false;
  }

  /**
   * Get the initializing module.
   *
   * @see #init(Module)
   *
   * @return The module.
   * @throws IllegalStateException Signals that this analyzer has not
   *   been initialized with a module.
   */
  public Module module() {
    // Make sure we are in the right state.
    if (isGrammarMode) {
      throw new IllegalStateException("Not initialized with module");
    }

    // Do the work.
    return module;
  }

  // =======================================================================

  /**
   * Look up the specified module.
   *
   * @param module The module name.
   * @return The corresponding module or <code>null</code> if there is
   *   no such module.
   * @throws IllegalStateException Signals that this analyzer has not
   *   been initialized with a grammar.
   */
  public Module lookup(String module) {
    // Make sure we are in the right state.
    if (! isGrammarMode) {
      throw new IllegalStateException("Not initialized with grammar");
    }

    // Do the work.
    return moduleMap.get(module);
  }

  /**
   * Look up the specified module.
   *
   * @param module The module name.
   * @return The corresponding module or <code>null</code> if there is
   *   no such module.
   * @throws IllegalStateException Signals that this analyzer has not
   *   been initialized with a grammar.
   */
  public Module lookup(ModuleName module) {
    // Make sure we are in the right state.
    if (! isGrammarMode) {
      throw new IllegalStateException("Not initialized with grammar");
    }

    // Do the work.
    return moduleMap.get(module.name);
  }

  /**
   * Look up the production for the specified qualified nonterminal.
   *
   * @param nt The nonterminal.
   * @return The corresponding production or <code>null</code> if
   *   there is no such production.
   */
  public FullProduction lookupGlobally(NonTerminal nt) {
    if (isGrammarMode) {
      return nt.isQualified()? (FullProduction)grammarPMap.get(nt) : null;
    } else {
      return pMap.get(nt);
    }
  }

  /**
   * Look up the production for the specified nonterminal, which is
   * also defined by the specified module.
   *
   * @param nt The nonterminal
   * @param m The module.
   * @return The corresponding production or <code>null</code> if the
   *   specified module does not define a production with the
   *   specified nonterminal.
   * @throws IllegalArgumentException
   *   Signals multiple definitions for the specified nonterminal.
   * @throws IllegalStateException Signals that this analyzer has not
   *   been initialized with a grammar.
   */
  public FullProduction lookup(NonTerminal nt, Module m) {
    if (! isGrammarMode) {
      throw new IllegalStateException("Not initialized with grammar");
    }

    if (nt.isQualified()) {
      if (nt.getQualifier().equals(m.name.name)) {
        if (grammarPMap.containsKey(nt)) {
          return (FullProduction)grammarPMap.get(nt);
        } else {
          // Continue with the unqualified nonterminal.
          nt = nt.unqualify();
        }
      } else {
        // Wrong module name.
        return null;
      }
    }

    Object o = grammarPMap.get(nt);

    if (null == o) {
      return null;

    } else if (o instanceof FullProduction) {
      FullProduction p = (FullProduction)o;

      return isDefined(p, m)? p : null;

    } else {
      FullProduction result = null;
      for (FullProduction p : toFullProductionList(o)) {
        if (isDefined(p, m)) {
          if (null == result) {
            result = p;
          } else {
            throw new IllegalArgumentException("Multiple definitions for " + nt);
          }
        }
      }

      return result;
    }
  }

  /**
   * Look up the production for the specified nonterminal.  Note that,
   * when processing a grammar, this method requires initializing
   * calls to {@link #process(Module)} to correctly resolve
   * nonterminals.
   *
   * @param nt The nonterminal.
   * @return The corresponding production or <code>null</code>
   *   if there is no such production.
   * @throws IllegalArgumentException
   *   Signals that there are multiple, ambiguous productions.
   */
  public FullProduction lookup(NonTerminal nt) {
    if (isGrammarMode) {
      // If the nonterminal is qualified, the corresponding production
      // can be defined (1) in the current module or one of the
      // modules modified by the current module or (2) in one of the
      // imported modules or one of the modules modified by the
      // imported modules.
      if (nt.isQualified()) {
        String qualifier = nt.getQualifier();

        if (qualifier.equals(mCurrent.name.name)) {
          return lookup(nt, mCurrent);

        } else if (isImported(qualifier, mCurrent)) {
          Module m = lookup(qualifier);

          return (null != m)? lookup(nt, m) : null;

        } else {
          return null;
        }
      }

      // The nonterminal is unqualified.
      Object o = grammarPMap.get(nt);
      
      if (null == o) {
        return null;
        
      } else if (o instanceof FullProduction) {
        FullProduction p = (FullProduction)o;

        return (isDefined(p, mCurrent) || isImported(p, mCurrent))? p : null;
        
      } else {
        FullProduction       result = null;
        List<FullProduction> list   = toFullProductionList(o);

        // If the nonterminal is defined in the current module, it
        // takes precedence over all other definitions.
        for (FullProduction p : list) {
          if (isDefined(p, mCurrent)) {
            if (null == result) {
              result = p;
            } else {
              throw new IllegalArgumentException("Multiple definitions for "+nt);
            }
          }
        }

        if (null != result) {
          return result;
        }

        // Otherwise, we look for a single imported nonterminal.
        for (FullProduction p : list) {
          if (isImported(p, mCurrent)) {
            if (null == result) {
              result = p;
            } else {
              throw new IllegalArgumentException("Multiple imported " +
                                                 "definitions for " + nt);
            }
          }
        }

        return result;
      }

    } else {
      return pMap.get(nt);
    }
  }

  /**
   * Determine whether the specified production is defined by the
   * specified module.  This method works correctly in the presence of
   * not yet applied module modifications, but it requires that this
   * analyzer has been initialized with the corresponding grammar.
   *
   * @param p The production.
   * @param m The module.
   * @return <code>true</code> if the production is defined by the
   *   specified module.
   */
  public boolean isDefined(Production p, Module m) {
    final String qualifier = p.qName.getQualifier();

    // First, check the specified module.
    if (m.name.name.equals(qualifier)) {
      return true;
    }

    // Next, walk the chain of modification dependencies.
    while (null != m.modification) {
      m = moduleMap.get(m.modification.visibleName().name);

      if (null == m) {
        break;
      } else if (m.name.name.equals(qualifier)) {
        return true;
      }
    }

    // Finally, give up.
    return false;
  }

  /**
   * Determine whether the specified production is defined by a module
   * imported by the specified module.  This method works correctly in
   * the presence of not yet applied module modifications, but it
   * requires that this analyzer has been initialized with the
   * corresponding grammar.
   *
   * @param p The production.
   * @param m The module.
   * @return <code>true</code> if the specified production is imported
   *   by the specified module.
   */
  public boolean isImported(Production p, Module m) {
    // First, check the imports of the specified module.
    if (isImported1(p, m)) {
      return (! p.hasAttribute(Constants.ATT_PRIVATE));
    }

    // Next, walk the chain of modification dependencies.
    while (null != m.modification) {
      m = moduleMap.get(m.modification.visibleName().name);

      if (null == m) {
        break;
      } else if (isImported1(p, m)) {
        return (! p.hasAttribute(Constants.ATT_PRIVATE));
      }
    }

    // Finally, give up.
    return false;
  }

  /**
   * Determine whether the specified production is defined by a module
   * imported by the specified module.  This method does not follow
   * modification dependencies.
   *
   * @param p The production.
   * @param m The module.
   * @return <code>true</code> if the specified production is imported
   *   by the specified module.
   */
  private boolean isImported1(Production p, Module m) {
    if (null != m.dependencies) {
      for (ModuleDependency dep : m.dependencies) {
        if (dep.isImport()) {
          m = moduleMap.get(dep.visibleName().name);
          
          if ((null != m) && isDefined(p, m)) {
            return true;
          }
        }
      }
    }

    return false;
  }

  // =======================================================================

  /**
   * Rename all productions to have unique names.  Note that this
   * method only works, if the corresponding visitor has initialized
   * the analyzer with a {@link #init(Grammar) grammar}.  Further note
   * that this method may change a production's {@link Production#name
   * name} as well as the internal mapping from nonterminals to
   * productions.  This method assumes that all partial productions
   * have been applied.
   */
  public void uniquify() {
    // This operation only makes sense when we are processing a
    // collection of modules.
    if (! isGrammarMode) {
      throw new IllegalStateException("Not initialized with grammar");
    }

    // The set of processed productions and the set of mappings to
    // remove.
    Set<NonTerminal> renamed = new HashSet<NonTerminal>();
    Set<NonTerminal> remove  = new HashSet<NonTerminal>();

    // Process all productions to determine the unique names.
    for (Module m : grammar.modules) {
      for (Iterator<Production> iter=m.productions.iterator();iter.hasNext();) {
        FullProduction p = (FullProduction)iter.next();

        // Make sure we don't process this production twice.
        if (renamed.contains(p.qName)) {
          continue;
        }

        // Look up the unqualified name and process all productions
        // with that name.
        Object o = grammarPMap.get(p.name);
        if (o instanceof FullProduction) {
          // There is no need for renaming.
          renamed.add(p.qName);

        } else {
          List<FullProduction> l = toFullProductionList(o);

          // Remember to remove this mapping later on.
          remove.add(p.name);

          // Create a list of names, only qualified by the last
          // identifier of the qualifier.
          List<NonTerminal> names = new ArrayList<NonTerminal>();
          for (FullProduction p2 : l) {
            String qual = p2.qName.getQualifier();

            if (Utilities.isQualified(qual)) {
              names.add(p2.name.qualify(Utilities.getName(qual)));
            } else {
              names.add(p2.qName);
            }
          }

          // If the list has only unique names, we use these names.
          // Otherwise, we use the fully qualified names.
          if (names.size() == new HashSet<NonTerminal>(names).size()) {
            Iterator<FullProduction> iter2 = l.iterator();
            Iterator<NonTerminal>    iter3 = names.iterator();
            while (iter2.hasNext()) {
              FullProduction p2 = iter2.next();
              p2.name           = iter3.next();
              renamed.add(p2.qName);
            }

          } else {
            for (FullProduction p2 : l) {
              p2.name = p2.qName;
              renamed.add(p2.qName);
            }
          }
        }
      }
    }

    // Now, change all nonterminals in the grammar.
    new Renamer(null, this, new Renamer.Translation() {
        public NonTerminal map(NonTerminal nt, Analyzer analyzer) {
          NonTerminal result = analyzer.lookup(nt).name;

          if (nt.equals(result)) {
            // Be sure to return the original nonterminal, which has
            // the right source location.
            result = nt;

          } else {
            // Create a copy of the nonterminal and preserve the
            // original's location.
            result = new NonTerminal(result.name);
            result.setLocation(nt);
          }

          return result;
        }}).dispatch(grammar);

    // Finally, fix the grammar-wide production map.
    for (NonTerminal name : remove) {
      grammarPMap.remove(name);
    }
  }

  // =======================================================================

  /**
   * Process the specified module.  This method sets the current
   * module when processing an entire grammar.
   *
   * @param m The module.
   */
  public void process(Module m) {
    if (isGrammarMode) {
      mCurrent = m;
    } else if (m != module) {
      throw new IllegalArgumentException("Invalid module " + m);
    }
  }

  /**
   * Get the module currently being processed.
   *
   * @return The current module.
   */
  public Module currentModule() {
    return isGrammarMode? mCurrent : module;
  }

  /**
   * Enter the specified production.  This method temporarily switches
   * the current module to the production's module.
   *
   * @param p The production.
   * @return A closure for {@link #exit(Object)}.
   */
  public Object enter(Production p) {
    if (isGrammarMode) {
      Module m = mCurrent;
      mCurrent = lookup(p.qName.getQualifier());
      return m;
    } else {
      return null;
    }
  }

  /**
   * Exit a previously entered production.  This method restores the
   * current module to the state before the call to {@link
   * #enter(Production)}.
   *
   * @param closure The closure returned from <code>enter()</code>.
   */
  public void exit(Object closure) {
    if (isGrammarMode) {
      mCurrent = (Module)closure;
    }
  }

  /**
   * Process the specified production.  This method clears the set of
   * working nonterminals.  It also resets the counters for creating
   * new variables and nonterminals (besides the counter for shared
   * productions).  It then invokes this analyzer's visitor on the
   * specified production.  This method should be called within the
   * loop iterating over a grammar module's productions, but not at
   * other locations within a visitor.
   *
   * @param p The production.
   */
  public void process(Production p) {
    // Initialize the per-production state.
    pWorking.clear();

    varCount    = 1;
    splitCount  = 1;
    choiceCount = 1;
    starCount   = 1;
    plusCount   = 1;
    optionCount = 1;
    tailCount   = 1;

    // Remember the current production.
    pCurrent    = p;

    // Now, actually process the production.
    visitor().dispatch(p);
  }

  /**
   * Get the production currently being processed.
   *
   * @return The current production.
   */
  public Production current() {
    return pCurrent;
  }

  // =======================================================================

  /**
   * Set the status of the specified nonterminal as being worked on.
   *
   * @param nt The nonterminal.
   */
  public void workingOn(NonTerminal nt) {
    pWorking.add(nt);
  }

  /**
   * Set the status of the specified nonterminal as not being worked
   * on.
   *
   * @param nt The nonterminal.
   */
  public void notWorkingOn(NonTerminal nt) {
    pWorking.remove(nt);
  }

  /** Set the status of all nonterminals as not being worked on. */
  public void notWorkingOnAny() {
    pWorking.clear();
  }

  /**
   * Determine whether the specified nonterminal is being worked on.
   *
   * @param nt The nonterminal.
   * @return <code>true</code> if the nonterminal is being worked
   *    on.
   */
  public boolean isBeingWorkedOn(NonTerminal nt) {
    return pWorking.contains(nt);
  }

  /**
   * Get the set of nonterminals being worked on.  Note that the
   * caller must copy the set if it keeps the reference to the
   * returned set after the next call to {@link #process}.
   *
   * @return The working set.
   */
  public Set<NonTerminal> working() {
    return pWorking;
  }

  // =======================================================================

  /**
   * Mark the specified nonterminal.
   *
   * @param nt The nonterminal.
   */
  public void mark(NonTerminal nt) {
    pMarked.add(nt);
  }

  /**
   * Mark the specified nonterminals.
   *
   * @param nts The list of nonterminals.
   */
  public void mark(Collection<NonTerminal> nts) {
    pMarked.addAll(nts);
  }

  /** Mark all of a grammar module's nonterminals. */
  public void markAll() {
    for (Production p : module.productions) {
      pMarked.add(p.qName);
    }
  }

  /**
   * Unmark the specified nonterminal.
   *
   * @param nt The nonterminal.
   */
  public void unmark(NonTerminal nt) {
    pMarked.remove(nt);
  }

  /** Unmark all nonterminals. */
  public void unmarkAll() {
    pMarked.clear();
  }

  /**
   * Determine whether any nonterminals are marked.
   *
   * @return <code>true</code> if any nonterminal has been marked.
   */
  public boolean hasMarked() {
    return (! pMarked.isEmpty());
  }

  /**
   * Determine whether the specified nonterminal has been marked.
   *
   * @param nt The nonterminal.
   * @return <code>true</code> if the nonterminal has been
   *   marked.
   */
  public boolean isMarked(NonTerminal nt) {
    return pMarked.contains(nt);
  }

  /**
   * Get the set of marked nonterminals.  Note that the caller must
   * copy the set if it keeps the reference to the returned set after
   * the next use of this analyzer.
   *
   * @return The marked set.
   */
  public Set<NonTerminal> marked() {
    return pMarked;
  }

  // =======================================================================

  /** Clear the processed status of all nonterminals. */
  public void clearProcessed() {
    pProcessed.clear();
  }

  /**
   * Set the status of the specified nonterminal as processed.
   *
   * @param nt The nonterminal.
   */
  public void processed(NonTerminal nt) {
    pProcessed.add(nt);
  }

  /**
   * Determine whether the specified nonterminal has been processed.
   *
   * @param nt The nonterminal.
   * @return <code>true</code> if the nonterminal has been processed.
   */
  public boolean isProcessed(NonTerminal nt) {
    return pProcessed.contains(nt);
  }

  // =======================================================================

  /**
   * Clear the list of newly generated productions.  This method needs
   * to be called before a processing step that may add new
   * productions through {@link #add(FullProduction)} and {@link
   * #addNewProductionsAt(int)}.
   */
  public void startAdding() {
    pNew.clear();
  }

  /**
   * Prepare the specified production for addition to the grammar
   * module.  This method adds the specified production to the list of
   * newly generated productions.  It also adds the production to the
   * map from nonterminals to productions and marks it as {@link
   * Constants#SYNTHETIC synthetic}.  However, addition is not
   * complete: the productions in the list of newly generated
   * productions still need to be added into the grammar itself.  This
   * is typically done within the main loop iterating over a grammar's
   * productions and thus through a separate {@link
   * #addNewProductionsAt(int) method}.
   * 
   * @param p The new production.
   */
  public void add(FullProduction p) {
    p.setProperty(Constants.SYNTHETIC, Boolean.TRUE);
    pNew.add(p);
    pMap.put(p.name, p);
    pMap.put(p.qName, p);
  }

  /**
   * Add the newly generated productions to the grammar itself.  This
   * method adds the productions collected through {@link
   * #add(FullProduction) add()} into the current grammar at the
   * specified index of the grammar's list of productions.
   *
   * @param idx The index into the grammar's list of productions.
   * @return The number of productions added.
   */
  public int addNewProductionsAt(int idx) {
    final int size = pNew.size();

    if (0 != size) {
      module.productions.addAll(idx, pNew);
    }

    return size;
  }

  /**
   * Prepare the specified production for removal from the grammar.
   * This method removes the specified production from the mapping
   * from nonterminals to productions and, if present, from the set of
   * top-level nonterminals.  However, removal is not complete: the
   * production still needs to be removed from the grammar itself.
   * This is typically done within the main loop iterating over a
   * grammar's productions.
   *
   * @param p The production.
   */
  public void remove(FullProduction p) {
    pMap.remove(p.name);
    pMap.remove(p.qName);
  }

  // =======================================================================

  /**
   * Reset the counter for synthetic variables.  Note that {@link
   * #process(Production)} already resets this counter; this method
   * provides more fine-grained control.
   */
  public void resetVarCount() {
    varCount = 1;
  }

  /**
   * Get the current counter for synthetic variables.
   *
   * @return The current counter.
   */
  public int getVarCount() {
    return varCount;
  }

  /**
   * Set the counter for synthetic variables.
   *
   * @param count The new counter value.
   */
  public void setVarCount(int count) {
    varCount = count;
  }

  /**
   * Create a new synthetic variable.
   *
   * @return The name of the synthetic variable.
   */
  public String variable() {
    return VARIABLE + Integer.toString(varCount++);
  }

  /**
   * Create a new synthetic variable with the specified marker.
   *
   * @param marker The marker.
   * @return The name of the synthetic variable.
   */
  public String variable(String marker) {
    return VARIABLE + marker + "$" + Integer.toString(varCount++);
  }

  /**
   * Determine whether the specified variable is a synthetic variable.
   *
   * @param var The variable name.
   * @return <code>true</code> if the specified variable name is
   *   a synthetic variable returned by {@link #variable()}.
   */
  public static boolean isSynthetic(String var) {
    return var.startsWith(VARIABLE);
  }

  /**
   * Create a new nonterminal for a split.
   *
   * @return The new nonterminal.
   */
  public NonTerminal split() {
    return
      new NonTerminal(pCurrent.name + SPLIT + Integer.toString(splitCount++));
  }

  /**
   * Create a new nonterminal for a choice.
   *
   * @return The new nonterminal.
   */
  public NonTerminal choice() {
    return
      new NonTerminal(pCurrent.name + CHOICE + Integer.toString(choiceCount++));
  }

  /**
   * Create a new nonterminal for zero or more repetitions.
   *
   * @return The new nonterminal.
   */
  public NonTerminal star() {
    return new NonTerminal(pCurrent.name + STAR + Integer.toString(starCount++));
  }

  /**
   * Create a new nonterminal for one or more repetitions.
   *
   * @return The new nonterminal.
   */
  public NonTerminal plus() {
    return new NonTerminal(pCurrent.name + PLUS + Integer.toString(plusCount++));
  }

  /**
   * Create a new nonterminal for an option.
   *
   * @return The new nonterminal.
   */
  public NonTerminal option() {
    return
      new NonTerminal(pCurrent.name + OPTION + Integer.toString(optionCount++));
  }

  /**
   * Create a new nonterminal for a tail production.
   *
   * @return The new nonterminal.
   */
  public NonTerminal tail() {
    return new NonTerminal(pCurrent.name + TAIL + Integer.toString(tailCount++));
  }

  /**
   * Create a new nonterminal for a shared production.
   *
   * @return The new nonterminal.
   */
  public NonTerminal shared() {
    return new NonTerminal(SHARED + Integer.toString(sharedCount++));
  }

  /**
   * Determine whether the specified nonterminal is synthetic.
   *
   * @param nt The nonterminal.
   * @return <code>true</code> if the nonterminal is synthetic.
   */
  public static boolean isSynthetic(NonTerminal nt) {
    return (-1 != nt.name.indexOf(SEPARATOR));
  }

  // =======================================================================

  /**
   * Strip unnecessary ordered choices and sequences from the specified
   * element.  A choice or sequence is unnecessary if it contains only
   * a single element.
   *
   * @param e The element.
   * @return The stripped element.
   */
  public static Element strip(Element e) {
    switch (e.tag()) {
    case CHOICE:
      OrderedChoice c = (OrderedChoice)e;
      if (1 == c.alternatives.size()) {
        e = strip(c.alternatives.get(0));
      }
      break;
    case SEQUENCE:
      Sequence s = (Sequence)e;
      if (1 == s.size()) {
        e = strip(s.get(0));
      }
      break;
    }
    return e;
  }

  /**
   * Strip unnecessary ordered choices from the specified choice.  A
   * choice is unnecessary if it contains only a single choice.
   *
   * @param c The choice.
   * @return The stripped choice.
   */
  public static OrderedChoice stripChoices(OrderedChoice c) {
    boolean changed = false;

    do {
      if (1 == c.alternatives.size()) {
        Element e = c.alternatives.get(0);

        switch (e.tag()) {
        case CHOICE:
          c       = (OrderedChoice)e;
          changed = true;
          break;

        case SEQUENCE:
          Sequence s = (Sequence)e;
          if ((1 == s.size()) && (s.get(0) instanceof OrderedChoice)) {
            c       = (OrderedChoice)s.get(0);
            changed = true;
          }
          break;
        }
      }
    } while (changed);

    return c;
  }

  // =======================================================================

  /**
   * Make a deep copy of the specified module.
   *
   * @param m The module.
   * @return A deep copy.
   */
  public Module copy(Module m) {
    return (Module)xerox.dispatch(m);
  }

  /**
   * Make a deep copy of the specified element.
   *
   * @param e The element.
   * @return A deep copy.
   */
  public <T extends Element> T copy(T e) {
    return xerox.copy(e);
  }

  // =======================================================================

  /**
   * Determine whether the specified sequence starts with terminals
   * that can be optimized.  This method returns <code>true</code> if
   * the terminals can be optimized through character switches.
   * Currently, this is only the case for character and string
   * literals.
   *
   * @param s The sequence.
   * @return <code>true</code> if the sequence starts with optimizable
   *   terminals.
   */
  public boolean hasTerminalPrefix(Sequence s) {
    if (1 <= s.size()) {
      Element e = s.get(0);

      if ((e instanceof CharLiteral) ||
          (e instanceof StringLiteral) ||
          ((e instanceof CharClass) &&
           (((CharClass)e).count() <= MAX_COUNT))) {
        return true;
      }
    }

    return false;
  }

  /**
   * Determine the length of the specified sequence after
   * normalization.
   *
   * @param s The sequence
   * @return The size of the normalized sequence or -1 if the sequence
   *   already is in normal form.
   */
  private int normalLength(Sequence s) {
    final int size   = s.size();
    boolean   normal = true;
    int       count  = 0;

    loop: for (int i=0; i<size; i++) {
      Element e = s.get(i);

      switch (e.tag()) {
      case CHAR_LITERAL:
        normal = false;
        count++;
        break;

      case STRING_LITERAL:
        normal = false;
        count += ((StringLiteral)e).text.length();
        break;

      case CHAR_CLASS:
        count++;
        break;

      default:
        count += (size-i);
        break loop;
      }
    }

    return (normal)? -1 : count;
  }

  /**
   * Normalize the specified sequence for {@link
   * #joinTerminals(Sequence,Element) joining} with other elements
   * during terminal optimization.  Currently, this method converts
   * string literals into equivalent subsequences of character
   * literals.
   *
   * @param s The sequence.
   * @return The normalized sequence.
   */
  public Sequence normalizeTerminals(Sequence s) {
    final int nl = normalLength(s);
    if (-1 == nl) return s;

    final int l  = s.size();
    Sequence  s2 = new Sequence(new ArrayList<Element>(nl));
    s2.setLocation(s);

    loop: for (int i=0; i<l; i++) {
      Element e = s.get(i);

      switch (e.tag()) {
      case CHAR_LITERAL:
        s2.add(new CharClass(((CharLiteral)e).c));
        break;

      case STRING_LITERAL: {
        StringLiteral sl = (StringLiteral)e;

        for (int j=0; j<sl.text.length(); j++) {
          s2.add(new CharClass(sl.text.charAt(j)));
        }
      } break;

      case CHAR_CLASS:
        s2.add(e);
        break;

      default:
        s2.addAll(s.elements.subList(i, l));
        break loop;
      }
    }

    return s2;
  }

  /**
   * Join the specified sequence with the specified element.  Note
   * that the specified sequence must have been {@link
   * #normalizeTerminals(Sequence) normalized}.  Further note that the
   * combined element is guaranteed to either be a sequence or an
   * ordered choice.
   *
   * @param source The source sequence.
   * @param target The target element.
   * @return The combined element.
   */
  public Element joinTerminals(Sequence source, Element target) {
    // Handle trivial case first.  Otherwise, normalize target.
    if (null == target) {
      return source;

    } else if (target instanceof Sequence) {
      // Strip sequence containing only an ordered choice.
      Sequence s = (Sequence)target;

      if (1 == s.size()) {
        Element e = s.get(0);

        if (e instanceof OrderedChoice) {
          target = e;
        }
      }
    }

    // Now, do the real joining.
    if (target instanceof Sequence) {
      final Sequence  t  = (Sequence)target;
      final Element   t1 = t.isEmpty() ? null : t.get(0);
      final Element   s1 = source.isEmpty() ? null : source.get(0);

      if ((s1 instanceof CharClass) &&
          (t1 instanceof CharClass) &&
          s1.equals(t1)) {
        // Both sequences start with the same character class.
        // Combine them into a single sequence, independent of the
        // class's count.
        Sequence result = new Sequence(joinTerminals(source.subSequence(1),
                                                     t.subSequence(1)));
        result.setLocation(source);
        result.elements.add(0, s1);

        return result;

      } else if ((s1 instanceof CharClass) &&
                 (((CharClass)s1).count() <= MAX_COUNT)) {
        CharClass sk = (CharClass)s1;

        if (t1 instanceof CharClass) {
          CharClass tk = (CharClass)t1;

          if (tk.count() <= MAX_COUNT) {
            // Both sequences start with different character classes.
            // Try to combine them into a new character switch.
            return joinTerminals(source,
                                 new Sequence(new
                                              CharSwitch(tk,
                                                         t.subSequence(1))));
          }

          // Fall through to creating an ordered choice.

        } else if (t1 instanceof CharSwitch) {
          CharSwitch sw    = (CharSwitch)t1;

          // Strip the exclusive flag and then look for an existing case.
          CharClass  klass = new CharClass(sk.ranges);
          CharCase   kase  = sw.hasCase(klass);

          if (sk.exclusive) {
            // We can only join the source into the character switch
            // if the switch covers exactly the non-exclusive version.
            if ((null != kase) && (1 == sw.cases.size())) {
              sw.base = joinTerminals(source.subSequence(1), sw.base);

              return target;
            }

            // Fall through to creating an ordered choice.

          } else {
            if (null != kase) {
              // Join the sequence into the existing character case.
              kase.element = joinTerminals(source.subSequence(1), kase.element);

              return target;

            } else if ((! sw.overlaps(klass)) && (null == sw.base)) {
              // If there is no overlap with an existing case and the
              // switch does not contain an exclusive character class,
              // add a new character case.
              sw.cases.add(new CharCase(klass, source.subSequence(1)));

              return target;
            }

            // Fall through to creating an ordered choice.
          }
        }
      }

      // Create a new choice with the target and source.
      OrderedChoice c = new OrderedChoice();
      c.alternatives.add(Sequence.ensure(target));
      c.alternatives.add(source);
      return c;

    } else if (target instanceof OrderedChoice) {
      // Join the source with the last alternative.
      OrderedChoice c = (OrderedChoice)target;
      final int     l = c.alternatives.size();
      Element       e = joinTerminals(source, c.alternatives.get(l-1));

      if (e instanceof OrderedChoice) {
        c.alternatives.remove(l-1);
        c.alternatives.addAll(((OrderedChoice)e).alternatives);
      } else {
        c.alternatives.set(l-1, Sequence.ensure(e));
      }

      return c;

    } else {
      // Join the source with a new sequence containing the target
      // element.
      return joinTerminals(source, new Sequence(target));
    }
  }

  // =======================================================================

  /**
   * Determine whether the specified sequences start with prefixes
   * that can be folded.
   *
   * @param s1 The first sequence.
   * @param s2 The second sequence.
   * @return <code>true</code> if the two sequences start with prefixes
   *   that can be folded.
   */
  public boolean haveCommonPrefix(Sequence s1, Sequence s2) {
    final Element e1 = s1.isEmpty() ? null : s1.get(0);
    final Element e2 = s2.isEmpty() ? null : s2.get(0);

    if (e1 instanceof Binding) {
      // FIXME: Handle bindings to yyValue
      return e1.equals(e2);

    } else if (null != e1) {
      return e1.equals(e2);

    } else {
      return false;
    }
  }

  /**
   * Normalize the specified sequences for {@link
   * #joinPrefixes(Sequence,Element) joining} with other sequences
   * during prefix folding.  The first specified sequence should
   * always be the first sequence that has a common prefix with
   * sequences following in an ordered choice, while the second
   * specified sequence should iterate over the following sequences.
   *
   * @param s1 The first sequence.
   * @param s2 The second sequence.
   * @return The normalized second sequence.
   */
  public Sequence normalizePrefix(Sequence s1, Sequence s2) {
    // FIXME: Handle bindings to yyValue
    return s2;
  }

  /**
   * Join the specified sequence with the specified element.  Note
   * that the specified sequence must have been {@link
   * #normalizePrefix(Sequence,Sequence) normalized}. Further note
   * that that the combined element is guaranteed to either be a
   * sequence or an ordered choice.
   *
   * @param source The source sequence.
   * @param target The target element.
   * @return The combined element.
   */
  public Element joinPrefixes(Sequence source, Element target) {
    // Handle trivial case first.  Otherwise, normalize target.
    if (null == target) {
      return source;

    } else if (target instanceof Sequence) {
      // Strip sequence containing only an ordered choice.
      Sequence s = (Sequence)target;

      if (1 == s.size()) {
        Element e = s.get(0);

        if (e instanceof OrderedChoice) {
          target = e;
        }
      }
    }

    // Now, do the real joining.
    if (target instanceof Sequence) {
      final Sequence  t  = (Sequence)target;

      if (source.equals(t)) {
        // Both sequences are the same. Return just one of them.
        return source;
      }

      final Element   t1 = t.isEmpty() ? null : t.get(0);
      final Element   s1 = source.isEmpty() ? null : source.get(0);

      if ((null != s1) && s1.equals(t1)) {
        // Both sequences start with the same prefix. Combine them
        // into a single sequence.
        Sequence result = new Sequence(joinPrefixes(source.subSequence(1),
                                                    t.subSequence(1)));
        // The new sequence has the same source location as the
        // source.
        result.setLocation(source);
        result.elements.add(0, s1);

        return result;
      }

      // Create a new choice with the target and source.
      OrderedChoice c = new OrderedChoice();
      c.alternatives.add(Sequence.ensure(target));
      c.alternatives.add(source);
      return c;

    } else if (target instanceof OrderedChoice) {
      // Join the source with the last alternative.
      OrderedChoice c = (OrderedChoice)target;
      final int     l = c.alternatives.size();
      Element       e = joinPrefixes(source, c.alternatives.get(l-1));

      if (e instanceof OrderedChoice) {
        c.alternatives.remove(l-1);
        c.alternatives.addAll(((OrderedChoice)e).alternatives);
      } else {
        c.alternatives.set(l-1, Sequence.ensure(e));
      }

      return c;

    } else {
      // Join the source with a new sequence containing the target
      // element.
      return joinPrefixes(source, new Sequence(target));
    }
  }

  // =======================================================================

  /**
   * Get the text matched by the specified element.  This method
   * analyzes the specified element, and, if the element always
   * matches the same text, this method returns the static text.
   * Otherwise, this method returns <code>null</code>.  Note that this
   * method ignores predicates, actions, node markers, null literals,
   * and value elements, as they do not change the text matched by an
   * element.  Further note that this method recursively analyzes
   * referenced nonterminals.
   *
   * @param e The element.
   * @return The constant text.
   */
  public String matchingText(Element e) {
    final StringBuilder buf = new StringBuilder();
    return matchingText(e, buf) ? buf.toString() : null;
  }

  /**
   * Determine the specified element's static text.
   *
   * @param e The element.
   * @param buf The buffer for the static text.
   * @return <code>true</code> if the element has a static text.
   */
  private boolean matchingText(Element e, StringBuilder buf) {
    switch (e.tag()) {
    case CHOICE: {
      OrderedChoice c = (OrderedChoice)e;
      if (1 == c.alternatives.size()) {
        return matchingText(c.alternatives.get(0), buf);
      } else {
        return false;
      }
    }

    case SEQUENCE:
      for (Element el : ((Sequence)e).elements) {
        if (! matchingText(el, buf)) return false;
      }
      return true;

    case FOLLOWED_BY:
    case NOT_FOLLOWED_BY:
    case SEMANTIC_PREDICATE:
    case ACTION:
    case NODE_MARKER:
    case NULL:
      return true;

    case BINDING:
    case STRING_MATCH:
      return matchingText(((UnaryOperator)e).element, buf);

    case NONTERMINAL:
      return matchingText(lookup((NonTerminal)e).choice, buf);

    case STRING_LITERAL:
      buf.append(((StringLiteral)e).text);
      return true;

    case CHAR_LITERAL:
      buf.append(((CharLiteral)e).c);
      return true;

    case CHAR_CLASS: {
      CharClass c = (CharClass)e;
      if (1 == c.ranges.size()) {
        CharRange r = c.ranges.get(0);
        if (r.first == r.last) {
          buf.append(r.first);
          return true;
        }
      }
      return false;
    }

    default:
      return e instanceof ValueElement;
    }
  }

  // =======================================================================

  /**
   * Determine whether the specified element restricts the input.
   * Note that this method requires that the production and module
   * containing the element are currently being processed; i.e., the
   * corresponding <code>process()</code> invocations must have been
   * performed.  Further note that this method internally uses this
   * analyzer's working set.  Finally, note that, after processing a
   * production, this method sets the production's {@link
   * Properties#RESTRICT} property.
   *
   * @param e The element.
   * @return <code>true</code> if the element restricts the input.
   */
  public boolean restrictsInput(Element e) {
    return (Boolean)restrictsInputVisitor.dispatch(e);
  }

  /** The restricts input visitor. */
  @SuppressWarnings("unused")
  private final Visitor restrictsInputVisitor = new Visitor() {
      public Boolean visit(FullProduction p) {
        // Perform internal consistency checks.
        assert ! isBeingWorkedOn(p.qName);
        assert ! p.hasProperty(Properties.RESTRICT);

        // Enter the production's module.
        Object closure = enter(p);
        
        // Mark the production as being processed.
        workingOn(p.qName);
        
        // Process the production.
        Boolean result = (Boolean)dispatch(p.choice);
        
        // Remember the result.
        p.setProperty(Properties.RESTRICT, result);
        
        // Unmark the production.
        notWorkingOn(p.qName);
        
        // Exit the production's module.
        exit(closure);
        
        // Done.
        return result;
      }

      public Boolean visit(OrderedChoice c) {
        for (Sequence alt : c.alternatives) {
          if (! (Boolean)dispatch(alt)) return Boolean.FALSE;
        }
        return Boolean.TRUE;
      }
      
      public Boolean visit(Repetition r) {
        return r.once;
      }

      public Boolean visit(Option o) {
        return Boolean.FALSE;
      }

      public Boolean visit(Sequence s) {
        for (Element e : s.elements) {
          if ((Boolean)dispatch(e)) return Boolean.TRUE;
        }
        return Boolean.FALSE;
      }

      public Boolean visit(Predicate p) {
        // We assume that semantic predicates restrict the input.
        return Boolean.TRUE;
      }

      public Boolean visit(NonTerminal nt) {
        FullProduction p;

        try {
          p = lookup(nt);
        } catch (IllegalArgumentException x) {
          return Boolean.TRUE;
        }

        if (null != p) {
          if (p.hasProperty(Properties.RESTRICT)) {
            return (Boolean)p.getProperty(Properties.RESTRICT);
          } else if (isBeingWorkedOn(p.qName)) {
            return Boolean.TRUE;
          } else {
            return (Boolean)dispatch(p);
          }
        } else {
          return Boolean.TRUE;
        }
      }

      public Boolean visit(Terminal t) {
        return Boolean.TRUE;
      }

      public Boolean visit(UnaryOperator op) {
        // The default for bindings, string matches, and voided elements.
        return (Boolean)dispatch(op.element);
      }
      
      public Boolean visit(ParserAction pa) {
        // Parser actions are assumed to consume some input.
        return Boolean.TRUE;
      }
      
      public Boolean visit(Element e) {
        // Actions, parse tree nodes, null literals, and value
        // elements do not consume any input.
        return Boolean.FALSE;
      }
    };

  // =======================================================================

  /**
   * Determine whether the specified element may consume the input.
   * Note that this method requires that the production and module
   * containing the element are currently being processed; i.e., the
   * corresponding <code>process()</code> invocations must have been
   * performed.  Further note that this method internally uses this
   * analyzer's working set.  Finally, note that, after processing a
   * production, this method sets the production's {@link
   * Properties#CONSUMER} property.
   *
   * @param e The element.
   * @return <code>true</code> if the element consumes the input.
   */
  public boolean consumesInput(Element e) {
    return (Boolean)consumesInputVisitor.dispatch(e);
  }

  /** The consumes input visitor. */
  @SuppressWarnings("unused")
  private final Visitor consumesInputVisitor = new Visitor() {
      public Boolean visit(FullProduction p) {
        // Perform internal consistency checks.
        assert ! isBeingWorkedOn(p.qName);
        assert ! p.hasProperty(Properties.CONSUMER);

        // Enter the production's module.
        Object closure = enter(p);
        
        // Mark the production as being processed.
        workingOn(p.qName);
        
        // Process the production.
        Boolean result = (Boolean)dispatch(p.choice);
        
        // Remember the result.
        p.setProperty(Properties.CONSUMER, result);
        
        // Unmark the production.
        notWorkingOn(p.qName);
        
        // Exit the production's module.
        exit(closure);
        
        // Done.
        return result;
      }

      public Boolean visit(OrderedChoice c) {
        for (Sequence alt : c.alternatives) {
          if ((Boolean)dispatch(alt)) return Boolean.TRUE;
        }
        return Boolean.FALSE;
      }

      public Boolean visit(Sequence s) {
        for (Element e : s.elements) {
          if ((Boolean)dispatch(e)) return Boolean.TRUE;
        }
        return Boolean.FALSE;
      }
      
      public Boolean visit(Predicate p) {
        // Predicates inspect the input but do not consume it.
        return Boolean.FALSE;
      }

      public Boolean visit(NonTerminal nt) {
        FullProduction p;

        try {
          p = lookup(nt);
        } catch (IllegalArgumentException x) {
          return Boolean.TRUE;
        }

        if (null != p) {
          if (p.hasProperty(Properties.CONSUMER)) {
            return (Boolean)p.getProperty(Properties.CONSUMER);
          } else if (isBeingWorkedOn(p.qName)) {
            return Boolean.TRUE;
          } else {
            return (Boolean)dispatch(p);
          }
        } else {
          return Boolean.TRUE;
        }
      }

      public Boolean visit(Terminal t) {
        return Boolean.TRUE;
      }

      public Boolean visit(UnaryOperator op) {
        return (Boolean)dispatch(op.element);
      }
      
      public Boolean visit(ParserAction pa) {
        // Parser actions are assumed to consume some input.
        return Boolean.TRUE;
      }
      
      public Boolean visit(Element e) {
        // Actions, parse tree nodes, null literals, and value
        // elements do not consume any input.
        return Boolean.FALSE;
      }
    };

  // =======================================================================

  /**
   * Determine whether the specified element matches the empty input.
   * Note that this method requires that the production and module
   * containing the element are currently being processed; i.e., the
   * corresponding <code>process()</code> invocations must have been
   * performed.  Further note that this method internally uses this
   * analyzer's working set.  Finally, note that, after processing a
   * production, this method sets the production's {@link
   * Properties#EMPTY} property.
   *
   * @param e The element.
   * @return <code>true</code> if the element matches the empty input.
   */
  public boolean matchesEmpty(Element e) {
    return (Boolean)matchesEmptyVisitor.dispatch(e);
  }

  /** The matches empty visitor. */
  @SuppressWarnings("unused")
  private final Visitor matchesEmptyVisitor = new Visitor() {
      public Boolean visit(FullProduction p) {
        // Perform internal consistency checks.
        assert ! isBeingWorkedOn(p.qName);
        assert ! p.hasProperty(Properties.EMPTY);

        // Enter the production's module.
        Object closure = enter(p);
        
        // Mark the production as being processed.
        workingOn(p.qName);
        
        // Process the production.
        Boolean result = (Boolean)dispatch(p.choice);
        
        // Remember the result.
        p.setProperty(Properties.EMPTY, result);
        
        // Unmark the production.
        notWorkingOn(p.qName);
        
        // Exit the production's module.
        exit(closure);
        
        // Done.
        return result;
      }

      public Boolean visit(OrderedChoice c) {
        for (Sequence alt : c.alternatives) {
          if ((Boolean)dispatch(alt)) return Boolean.TRUE;
        }
        return Boolean.FALSE;
      }
      
      public Boolean visit(Repetition r) {
        return ! r.once;
      }

      public Boolean visit(Option o) {
        return Boolean.TRUE;
      }

      public Boolean visit(Sequence s) {
        for (Element e : s.elements) {
          if (! (Boolean)dispatch(e)) return Boolean.FALSE;
        }
        return Boolean.TRUE;
      }

      public Boolean visit(FollowedBy p) {
        return (Boolean)dispatch(p.element);
      }
      
      public Boolean visit(NotFollowedBy p) {
        return ! (Boolean)dispatch(p.element);
      }
      
      public Boolean visit(SemanticPredicate p) {
        // We assume that semantic predicates restrict input.
        return Boolean.FALSE;
      }

      public Boolean visit(NonTerminal nt) {
        FullProduction p;

        try {
          p = lookup(nt);
        } catch (IllegalArgumentException x) {
          return Boolean.FALSE;
        }

        if (null != p) {
          if (p.hasProperty(Properties.EMPTY)) {
            return (Boolean)p.getProperty(Properties.EMPTY);
          } else if (isBeingWorkedOn(p.qName)) {
            return Boolean.FALSE;
          } else {
            return (Boolean)dispatch(p);
          }
        } else {
          return Boolean.FALSE;
        }
      }

      public Boolean visit(Terminal t) {
        return Boolean.FALSE;
      }

      public Boolean visit(UnaryOperator op) {
        // The default for bindings, string matches, and voided elements.
        return (Boolean)dispatch(op.element);
      }
      
      public Boolean visit(ParserAction pa) {
        // Parser actions are assumed to consume some input.
        return Boolean.FALSE;
      }
      
      public Boolean visit(Element e) {
        // Actions, parse tree nodes, null literals, and value
        // elements do not consume any input.
        return Boolean.TRUE;
      }
    };

  // =======================================================================

  /**
   * Determine whether the specified element is a not-followed-by
   * predicate.  Note that this method requires that the production
   * and module containing the element are currently being processed;
   * i.e., the corresponding <code>process()</code> invocations must
   * have been performed.
   *
   * @param e The element.
   * @return <code>true</code> if the specified element is a
   *   not-followed-by predicate.
   */
  public boolean isNotFollowedBy(Element e) {
    return (Boolean)isNotFollowedByVisitor.dispatch(strip(e));
  }

  /** The is not-followed-by visitor. */
  @SuppressWarnings("unused")
  private final Visitor isNotFollowedByVisitor = new Visitor() {
      public Boolean visit(FullProduction p) {
        Object  closure = enter(p);
        Boolean result  = (Boolean)dispatch(strip(p.choice));
        exit(closure);
        return result;
      }

      public Boolean visit(OrderedChoice c) {
        for (Sequence alt : c.alternatives) {
          if (! (Boolean)dispatch(alt)) return Boolean.FALSE;
        }
        return Boolean.TRUE;
      }

      public Boolean visit(Sequence s) {
        for (Element e : s.elements) {
          if (! (Boolean)dispatch(e)) return Boolean.FALSE;
        }
        return Boolean.TRUE;
      }

      public Boolean visit(NonTerminal nt) {
        FullProduction p;
        try {
          p = lookup(nt);
        } catch (IllegalArgumentException x) {
          return Boolean.FALSE;
        }

        return null != p ? (Boolean)dispatch(p) : Boolean.FALSE;
      }

      public Boolean visit(NotFollowedBy p) {
        return Boolean.TRUE;
      }
    
      public Boolean visit(Element e) {
        return Boolean.FALSE;
      }
    };

  // =======================================================================

  /**
   * Determine whether the specified element can be bound.  This
   * method returns <code>true</code> if the specified element is
   * recognized by {@link #bind(List)} and {@link #bind(List,String)}
   * as capturing a list's semantic value.
   *
   * @param e The element.
   * @return <code>true</code> if the element can be bound.
   */
  public boolean isBindable(Element e) {
    switch (e.tag()) {
    case NONTERMINAL:
      return ! AST.isVoid(lookup((NonTerminal)e).type);
    case NULL:
    case CHOICE:
    case OPTION:
    case REPETITION:
    case ANY_CHAR:
    case CHAR_CLASS:
    case CHAR_LITERAL:
    case STRING_LITERAL:
    case STRING_MATCH:
    case PARSE_TREE_NODE:
    case BINDING:
      return true;
    default:
      return false;
    }
  }

  // =======================================================================

  /**
   * Bind the elements in the specified list.  This method analyzes
   * the specified list and, if possible, adds in a binding for the
   * semantic value of the elements in the list.  If the list does not
   * have a unique binding to capture the semantic value, this method
   * returns <code>null</code> to indicate the need for an explicitly
   * specified semantic value.
   *
   * @param l The list to bind.
   * @return The corresponding binding.
   */
  public Binding bind(List<Element> l) {
    return bind(l, null);
  }

  /**
   * Bind the elements in the specified list.  This method analyzes
   * the specified list and, if possible, adds in a binding for the
   * semantic value of the elements in the list.  If the list does not
   * have a unique binding to capture the semantic value, this method
   * returns <code>null</code> to indicate the need for an explicitly
   * specified semantic value.
   *
   * @param l The list to bind.
   * @param marker The marker for the binding variable or
   *   <code>null</code> if no marker should be used.
   * @return The corresponding binding.
   */
  public Binding bind(List<Element> l, String marker) {
    Binding   binding = null;
    Element   bound   = null;
    int       idx     = -1;

    final int length  = l.size();
    loop: for (int i=0; i<length; i++) {
      Element e = l.get(i);

      switch (e.tag()) {
      case NONTERMINAL:
        // Void productions have no meaningful value. Skip them.
        if (AST.isVoid(lookup((NonTerminal)e).type)) break;
        // Fall through.

      case NULL:
      case CHOICE:
      case OPTION:
      case REPETITION:
      case ANY_CHAR:
      case CHAR_CLASS:
      case CHAR_LITERAL:
      case STRING_LITERAL:
      case STRING_MATCH:
      case PARSE_TREE_NODE:
        if (-1 == idx) {
          bound   = e;
          idx     = i;
          break;
        } else {
          binding = null;
          idx     = -1;
          break loop;
        }

      case BINDING:
        if (-1 == idx) {
          binding = (Binding)e;
          idx     = i;
          break;
        } else {
          binding = null;
          idx     = -1;
          break loop;
        }

      case FOLLOWED_BY:
      case NOT_FOLLOWED_BY:
      case SEMANTIC_PREDICATE:
      case VOIDED:
      case NODE_MARKER:
        // Predicates, voided elements, and node markers have no
        // meaningful value.  Skip them.
        break;

      case ACTION:
        // Skip actions that do not set the semantic value.
        if (! ((Action)e).setsValue()) break;
        // Fall through.
        
      default:
        // Embedded sequences, character switches, parser actions, and
        // value elements cannot be bound.  All bets are off.
        binding = null;
        idx     = -1;
        break loop;
      }
    }

    if (null != binding) {
      // There is a single element that already has a binding.
      return binding;

    } else if (-1 == idx) {
      // We require an explicitly specified semantic value.
      return null;

    } else {
      if (null == marker) {
        binding = new Binding(variable(), bound);
      } else {
        binding = new Binding(variable(marker), bound);
      }
      l.set(idx, binding);
      return binding;
    }
  }

  /**
   * Get the binding for the specified list's value.  If the list has
   * a single binding, this method returns it; otherwise (i.e., if the
   * sequence has no binding or multiple bindings), this method
   * returns <code>null</code>.
   *
   * @param l The list.
   * @return The list's only binding (if it exists).
   */
  public static Binding getBinding(List<Element> l) {
    Binding binding  = null;
    for (Element e : l) {
      if (e instanceof Binding) {
        if (null == binding) {
          binding = (Binding)e;
        } else {
          return null;
        }
      }
    }

    return binding;
  }

  /**
   * Unbind the specified element.  If the element is a binding, this
   * method returns the bound element.  Otherwise, it returns the
   * element.
   *
   * @param e The element.
   * @return The unbound element.
   */
  public static Element unbind(Element e) {
    return e instanceof Binding ? ((Binding)e).element : e;
  }

  /**
   * Strip and unbind the specified element.  Invoking this method on
   * element <code>e</code> is semantically equivalent to:<pre>
   * Analyzer.strip(Analyzer.unbind(Analyzer.strip(e)))
   * </pre>
   *
   * @param e The element.
   * @return The stripped and unbound element.
   */
  public static Element stripAndUnbind(Element e) {
    return strip(unbind(strip(e)));
  }

  // =======================================================================

  /**
   * Get the specified list's semantic value.  If the specified list
   * explicitly sets the list's semantic value through a binding, a
   * semantic action, a parser action, or a value element, this method
   * returns the last such element.  Next, if the list has a single
   * element with a semantic value, this method returns that element.
   * Otherwise, this method returns <code>null</code>.
   *
   * @param list The list.
   * @param ignoreActions The flag for whether to ignore (parser)
   *   actions.
   * @return The list's value or <code>null</code> if the value cannot
   *   be determined.
   */
  public Element getValue(List<Element> list, boolean ignoreActions) {
    Element value = null;

    // First, try to find any explicit assignments of yyValue.
    for (Element e : list) {
      switch (e.tag()) {
      case BINDING: {
        Binding b = (Binding)e;
        if (CodeGenerator.VALUE.equals(b.name)) value = b;
      } break;

      case ACTION: {
        Action a = (Action)e;
        if ((! ignoreActions) && a.setsValue()) value = a;
      } break;

      case PARSER_ACTION:
        if (! ignoreActions) value = e;
        break;

      case ACTION_BASE_VALUE:
      case BINDING_VALUE:
      case GENERIC_ACTION_VALUE:
      case GENERIC_RECURSION_VALUE:
      case GENERIC_NODE_VALUE:
      case EMPTY_LIST_VALUE:
      case PROPER_LIST_VALUE:
      case NULL_VALUE:
      case STRING_VALUE:
      case TOKEN_VALUE:
        value = e;
        break;
      }
    }
    if (null != value) return value;

    // Second, try to find a single value.
    boolean foundMany = false;
    for (Element e : list) {
      switch (e.tag()) {
      case NONTERMINAL:
        if (AST.isVoid(lookup((NonTerminal)e).type)) break;
        // Fall through.

      case NULL:
      case CHOICE:
      case OPTION:
      case REPETITION:
      case ANY_CHAR:
      case CHAR_CLASS:
      case CHAR_LITERAL:
      case STRING_LITERAL:
      case STRING_MATCH:
      case PARSE_TREE_NODE:
      case BINDING:
        if (! foundMany) {
          if (null == value) {
            value     = e;
          } else {
            foundMany = true;
            value     = null;
          }
        }
        break;

      default:
        // No value.
      }
    }

    return value;
  }

  // =======================================================================

  /**
   * Determine whether the specified list of elements sets the
   * semantic value.  This method determines whether any element
   * either is a binding to <code>yyValue</code>, an action setting
   * <code>yyValue</code>, or a value element.
   *
   * @param l The list.
   * @param all The flag for whether all alternatives of nested
   *   choices need to set the semantic value.
   * @return <code>true</code> if the list sets the semantic value.
   */
  public static boolean setsValue(List<Element> l, boolean all) {
    return setsValue(new Sequence(l), all);
  }

  /**
   * Determine whether the specified element sets the semantic value.
   * This method determines whether the specified element contains
   * either an explicit binding to <code>yyValue</code>, an action
   * setting <code>yyValue</code>, or a value element.
   *
   * @param e The element.
   * @param all The flag for whether all alternatives of choices need
   *   to set the semantic value.
   * @return <code>true</code> if the element sets the semantic value.
   */
  @SuppressWarnings("unused")
  public static boolean setsValue(Element e, final boolean all) {
    return (Boolean)new Visitor() {
        private boolean isLast = true;

        public Boolean visit(OrderedChoice c) {
          if (! isLast) return Boolean.FALSE;

          for (Sequence alt : c.alternatives) {
            if (all) {
              if (! (Boolean)dispatch(alt)) return Boolean.FALSE;
            } else {
              if ((Boolean)dispatch(alt)) return Boolean.TRUE;
            }
          }
          return all;
        }

        public Boolean visit(Sequence s) {
          if (! isLast) return Boolean.FALSE;

          for (Iterator<Element> iter=s.elements.iterator(); iter.hasNext();) {
            isLast = ! iter.hasNext();
            if ((Boolean)dispatch(iter.next())) {
              isLast = true;
              return Boolean.TRUE;
            }
          }

          isLast = true;
          return Boolean.FALSE;
        }

        public Boolean visit(VoidedElement v) {
          return (Boolean)dispatch(v.element);
        }

        public Boolean visit(Binding b) {
          return CodeGenerator.VALUE.equals(b.name);
        }

        public Boolean visit(StringMatch m) {
          return (Boolean)dispatch(m.element);
        }

        public Boolean visit(Action a) {
          return a.setsValue();
        }

        public Boolean visit(ParserAction a) {
          return Boolean.TRUE;
        }

        public Boolean visit(ValueElement v) {
          return Boolean.TRUE;
        }

        public Boolean visit(Element e) {
          return Boolean.FALSE;
        }
      }.dispatch(e);
  }

  // =======================================================================

  /**
   * Determine whether the specified list of elements sets the
   * semantic value to <code>null</code>.  This method determines
   * whether the only value-setting element is a null value.  It
   * ignores nested choices and sequences.
   *
   * @param l The list.
   * @return <code>true</code> if the list sets the semantic value to
   *   <code>null</code>.
   */
  public static boolean setsNullValue(List<Element> l) {
    boolean hasNull = false;

    for (Element e : l) {
      switch (e.tag()) {
      case BINDING:
        if (CodeGenerator.VALUE.equals(((Binding)e).name)) return false;
        break;
      case ACTION:
        if (((Action)e).setsValue()) return false;
        break;
      case NULL_VALUE:
        hasNull = true;
        break;
      case ACTION_BASE_VALUE:
      case BINDING_VALUE:
      case GENERIC_ACTION_VALUE:
      case GENERIC_RECURSION_VALUE:
      case GENERIC_NODE_VALUE:
      case EMPTY_LIST_VALUE:
      case PROPER_LIST_VALUE:
      case STRING_VALUE:
      case TOKEN_VALUE:
        return false;
      }
    }

    return hasNull;
  }

  // =======================================================================

  /**
   * Determine whether the list value of the specified element may be
   * <code>null</code>.  This method returns <code>true</code> if the
   * specified element is an option or a nonterminal representing a
   * desguared option.
   *
   * @param element The element.
   * @return <code>true</code> if the element's value may be
   *   <code>null</code>.
   */
  public boolean mayBeNull(Element element) {
    switch (element.tag()) {
    case OPTION:
      return true;
    case NONTERMINAL:
      NonTerminal nt = (NonTerminal)element;
      Production  p  = lookup(nt);
      if (p.getBooleanProperty(Properties.OPTION)) return true;
    }

    return false;
  }

  // =======================================================================

  /**
   * Type the specified element.  This method requires that this
   * analyzer has been initialized with a grammar contained in a
   * single module.  It utilizes the type representation of {@link
   * AST} and may return a {@link Wildcard}.
   *
   * @param element The element.
   * @return The type.
   * @throws IllegalArgumentException Signals that the element cannot
   *   be typed.
   */
  public Type type(Element element) {
    switch (element.tag()) {
    case CHOICE:
    case SEQUENCE:
      return AST.ANY;

    case REPETITION: {
      Binding b = Analyzer.
        getBinding(Sequence.ensure(((Repetition)element).element).elements);
      return AST.listOf(null == b ? AST.ANY : type(b.element));
    }

    case OPTION: {
      Binding b = Analyzer.
        getBinding(Sequence.ensure(((Option)element).element).elements);
      return null == b ? AST.ANY : AST.markOptional(type(b.element));
    }

    case VOIDED:
      return VoidT.TYPE;

    case BINDING:
      return type(((Binding)element).element);

    case NONTERMINAL:
      return lookup((NonTerminal)element).type.deannotate();

    case ANY_CHAR:
    case CHAR_CLASS:
    case CHAR_LITERAL:
    case CHAR_SWITCH:
      return AST.CHAR;

    case STRING_LITERAL:
      return AST.STRING;

    case STRING_MATCH:
      return module.hasAttribute(Constants.ATT_PARSE_TREE) ?
        AST.NODE : AST.STRING;

    case PARSE_TREE_NODE:
      return AST.NODE;

    case NULL:
      return Wildcard.TYPE;

    default:
      throw new IllegalArgumentException("Unable to type " + element);
    }
  }

}
