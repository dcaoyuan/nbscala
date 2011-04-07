/*
 * xtc - The eXTensible Compiler
 * Copyright (C) 2005-2008 Robert Grimm
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import xtc.Constants;

import xtc.tree.Attribute;
import xtc.tree.Visitor;

import xtc.type.AST;
import xtc.type.ErrorT;
import xtc.type.Type;

import xtc.util.Runtime;
import xtc.util.Utilities;

/**
 * Visitor to resolve grammar module dependencies.
 *
 * <p />Note that this visitor {@link TextTester marks} text-only
 * productions as such.  It also {@link LeftRecurser detects} left
 * recursions and marks direct left recursions.  Furthermore, it sets
 * a grammar's {@link Properties#GENERIC} and {@link
 * Properties#RECURSIVE} properties as appropriate.
 *
 * @author Robert Grimm
 * @version $Revision: 1.128 $
 */
public class Resolver extends Visitor {

  /** The runtime. */
  protected final Runtime runtime;

  /** The analyzer utility. */
  protected final Analyzer analyzer;

  /** The type operations. */
  protected final AST ast;

  /** The current checking phase. */
  protected int phase;

  /** The identity hash map of erroneous nonterminals. */
  protected Map<NonTerminal, NonTerminal> badNTs;

  /** The flag for whether the current module has a stateful attribute. */
  protected boolean hasState;

  /** The flag for whether the current module is a mofunctor. */
  protected boolean isMofunctor;

  /** Flag for whether the current expression is a predicate. */
  protected boolean isPredicate;

  /** The set of sequence names for the current production. */
  protected Set<SequenceName> sequenceNames;

  /**
   * Create a new resolver.
   *
   * @param runtime The runtime.
   * @param analyzer The analyzer utility.
   * @param ast The type operations.
   */
  public Resolver(Runtime runtime, Analyzer analyzer, AST ast) {
    this.runtime  = runtime;
    this.analyzer = analyzer;
    this.ast      = ast;
    badNTs        = new IdentityHashMap<NonTerminal, NonTerminal>();
    sequenceNames = new HashSet<SequenceName>();
  }

  /**
   * Print the specified module's signature.
   *
   * @param m The module.
   */
  protected void signature(Module m) {
    // Print the globally visible module name.
    System.out.print("module ");
    System.out.print(m.name.name);

    // If the module has been instantiated from another, print the
    // original invocation.
    if (m.name.hasProperty(Constants.ORIGINAL) ||
        (null != m.parameters)) {
      System.out.print(" = ");
      ModuleName base =
        m.name.hasProperty(Constants.ORIGINAL) ?
        (ModuleName)m.name.getProperty(Constants.ORIGINAL) :
        m.name;
      System.out.print(base.name);
      if (null == m.parameters) {
        System.out.print("()");
      } else {
        System.out.print(m.parameters.toString());
      }
    }

    if (null != m.dependencies) {
      // Print the modification, if any.
      for (ModuleDependency dep : m.dependencies) {
        if (dep.isModification()) {
          System.out.println();
          System.out.print("       modifies ");
          System.out.print(dep.visibleName().name);
          break;
        }
      }

      // Print the imports, if any.
      boolean first = true;
      for (ModuleDependency dep : m.dependencies) {
        if (dep.isImport()) {
          if (first) {
            System.out.println();
            System.out.print("       imports ");
            first = false;
          } else {
            System.out.println(',');
            System.out.print("               ");
          }
          System.out.print(dep.visibleName().name);
        }
      }
    }

    // Done.
    System.out.println(';');
  }

  /**
   * Load the module with the specified name.
   *
   * @param name The name.
   * @return The corresponding grammar module.
   * @throws IllegalArgmentException
   *   Signals that the file is too large.
   * @throws FileNotFoundException
   *   Signals that the file does not exist.
   * @throws IOException
   *   Signals an exceptional condition while accessing the file.
   * @throws ParseException
   *   Signals a parse error.
   */
  protected Module load(String name) throws IOException, ParseException {
    File file = runtime.locate(Utilities.toPath(name, Constants.EXT_GRAMMAR));

    if (! file.exists()) {
      throw new FileNotFoundException(file + ": file not found");
    } else if (! file.isFile()) {
      throw new IllegalArgumentException(file + ": not a file");
    } else if (Integer.MAX_VALUE < file.length()) {
      throw new IllegalArgumentException(file + ": file too large");
    }

    Reader in = null;
    try {
      in             = runtime.getReader(file);
      PParser parser = new PParser(in, file.toString(), (int)file.length());
      return (Module)parser.value(parser.pModule(0));
    } finally {
      if (null != in) {
        try {
          in.close();
        } catch (IOException x) {
          // Nothing to see here. Move on.
        }
      }
    }
  }

  /**
   * Rename the specified module.  This method renames all module
   * names in the specified module, including the module's {@link
   * Module#name name}, the module's {@link Module#dependencies
   * dependencies}, and each production's {@link Production#qName
   * qualified name} (if it is not <code>null</code>).
   *
   * @param module The module.
   * @param renaming The renaming.
   */
  protected void rename(Module module, ModuleMap renaming) {
    // Process the name.
    module.name = module.name.rename(renaming);

    // Process the parameters.
    if (null != module.parameters) {
      module.parameters.rename(renaming);
    }

    // Process the dependencies.
    if (null != module.dependencies) {
      for (ModuleDependency dep : module.dependencies) dep.rename(renaming);
    }

    // Process the productions.
    Renamer renamer = new Renamer(runtime, analyzer, renaming);
    for (Production p : module.productions) {
      if (null != p.qName) {
        p.qName = p.qName.rename(renaming);
      }
      renamer.dispatch(p);
    }
  }

  /**
   * Strip the specified production's ordered choice.
   *
   * @see Analyzer#stripChoices
   *
   * @param p The production to strip.
   * @return The specified production.
   */
  protected Production strip(Production p) {
    if ((null != p) && (null != p.choice)) {
      p.choice = Analyzer.stripChoices(p.choice);
    }
    return p;
  }

  /**
   * Apply the specified alternative addition to the specified full
   * production.
   *
   * @param p1 The alternative addition.
   * @param p2 The full production.
   */
  protected void apply(AlternativeAddition p1, FullProduction p2) {
    final int length2 = p2.choice.alternatives.size();
    int       index   = -1;
    for (int i=0; i<length2; i++) {
      if (p1.sequence.equals(p2.choice.alternatives.get(i).name)) {
        index = i;
        break;
      }
    }

    if (-1 == index) {
      StringBuilder buf = new StringBuilder();
      buf.append("unable to add new alternative");
      if (1 != p1.choice.alternatives.size()) {
        buf.append('s');
      }
      if (p1.isBefore) {
        buf.append(" before ");
      } else {
        buf.append(" after ");
      }
      buf.append("non-existent alternative '");
      buf.append(p1.sequence.name);
      buf.append("'");
      runtime.error(buf.toString(), p1.sequence);

    } else {
      if (! p1.isBefore) {
        index++;
      }
      p2.choice.alternatives.addAll(index, p1.choice.alternatives);
    }
  }

  /**
   * Apply the specified alternative removal to the specified full
   * production.
   *
   * @param p1 The alternative removal.
   * @param p2 The full production.
   */
  protected void apply(AlternativeRemoval p1, FullProduction p2) {
    for (SequenceName name : p1.sequences) {
      boolean removed = false;

      for (Iterator<Sequence> iter=p2.choice.alternatives.iterator();
           iter.hasNext(); ) {
        if (name.equals(iter.next().name)) {
          iter.remove();
          removed = true;
          break;
        }
      }

      if (! removed) {
        runtime.error("unable to remove non-existent alternative '" + name +
                      "'", name);
      }
    }
  }
  
  /**
   * Apply the specified production override to the specified full
   * production.
   *
   * @param p1 The production override.
   * @param p2 The full production.
   */
  protected void apply(ProductionOverride p1, FullProduction p2) {
    // Override the attributes.
    if (null != p1.attributes) {
      p2.attributes = p1.attributes;
    }

    // Override the element.
    if (null != p1.choice) {
      if (p1.isComplete) {
        p2.choice = p1.choice;

      } else {
        for (Sequence s1 : p1.choice.alternatives) {
          if (null == s1.name) {
            runtime.error("overriding sequence without name", s1);

          } else {
            final int     length2 = p2.choice.alternatives.size();
            boolean       changed = false;
            for (int i=0; i<length2; i++) {
              Sequence    s2      = p2.choice.alternatives.get(i);
              if (s1.name.equals(s2.name)) {
                p2.choice.alternatives.set(i, s1);
                changed = true;
                break;
              }
            }

            if (! changed) {
              runtime.error("unable to override non-existent alternative '" +
                            s1.name + "'", s1);
            }
          }
        }
      }
    }
  }

  /**
   * Load all dependent modules.  This method loads the transitive
   * closure of all dependent modules for the specified top-level
   * module and returns the closure as a list, with the specified
   * module being the first list element.  It checks that the
   * specified top-level module is not parameterized, that the
   * parameters of parameterized modules are well-formed, that
   * arguments match parameters when instantiating parameterized
   * modules, that different instantiations are consistent with each
   * other, that a module does not have more than one modifies clause,
   * and that module modifications do not result in circular
   * dependencies.  This method also reports any file access and
   * parsing errors.  Finally, this method fills in the auxiliary
   * modification field for all loaded modules.
   *
   * @param m The module.
   * @return The corresponding grammar or <code>null</code> in the
   *   case of errors.
   */
  protected Grammar load(Module m) {
    // Make sure the top-level module is not parameterized.
    if ((null != m.parameters) && (0 < m.parameters.size())) {
      runtime.error("parameterized top-level module '" + m.name.name + "'",
                    m.name);
      return null;
    }

    // Print top-level module if so requested.
    PrettyPrinter pp = null;
    if (runtime.test("optionLoaded")) {
      pp = new PrettyPrinter(runtime.console(), ast, true);
      pp.dispatch(m);
      pp.flush();
    }

    // Prepare for loading all dependent modules.  The modules list
    // contains all correctly loaded modules.  The module map contains
    // a mapping between visible module names (as strings) and
    // modules.  The loaded map contains a mapping between visible
    // module names and the corresponding module dependencies (even if
    // the module contained errors).  The conflicts map is treated as
    // a set that contains conflicting module dependencies that were
    // already reported to the user.
    List<Module>                      modules   =
      new ArrayList<Module>();
    Map<String, Module>               moduleMap =
      new HashMap<String,Module>();
    Map<ModuleName, ModuleDependency> loaded    =
      new HashMap<ModuleName, ModuleDependency>();
    Map<ModuleDependency, Boolean>    conflicts =
      new IdentityHashMap<ModuleDependency, Boolean>();
    modules.add(m);
    moduleMap.put(m.name.name, m);
    loaded.put(m.name, new ModuleImport(m.name));

    // Process the top-level module's dependencies and fill in the
    // auxiliary modification field.
    List<ModuleDependency> workList = new ArrayList<ModuleDependency>();
    if (null != m.dependencies) {
      for (ModuleDependency dep : m.dependencies) {
        if (dep.isModification()) {
          // Only process one modification per module and do not allow
          // self-mutilation.
          boolean process = true;
          if (null != m.modification) {
            runtime.error("duplicate modifies declaration", dep);
            process = false;
          }
          if (dep.visibleName().equals(m.name)) {
            runtime.error("module '" + m.name + "' modifies itself", dep);
            process = false;
          }
          if (process) {
            m.modification = (ModuleModification)dep;
            workList.add(dep);
          }

        } else {
          workList.add(dep);
        }
      }
    }

    // Print the signature if so requested.
    if (runtime.test("optionDependencies")) {
      signature(m);
    }

    // Do the actual loading.
    while (! workList.isEmpty()) {
      ModuleDependency dep = workList.remove(0);

      // Make sure that every module is processed only once.
      if (loaded.containsKey(dep.visibleName())) {
        ModuleDependency dep2 = loaded.get(dep.visibleName());

        // Since modules live in a global name space, all dependencies
        // need to be consistent with each other.  In particular, two
        // dependency declarations for the a module with the same name
        // either need to have exactly the same structure or the later
        // declaration must be a straight-forward declaration without
        // any arguments or target name.
        if (! dep.isConsistentWith(dep2)) {
          if (! conflicts.containsKey(dep2)) {
            conflicts.put(dep2, Boolean.TRUE);
            runtime.error("inconsistent instantiation of module '" + 
                          dep2.module + dep2.arguments + "' as '" + 
                          dep2.visibleName() + "'", dep2);
          }
          runtime.error("inconsistent instantiation of module '" + dep.module +
                        dep.arguments + "' as '" + dep.visibleName() + "'", dep);
        }

        continue;
      }

      // Be verbose if so desired.
      if (runtime.test("optionVerbose")) {
        String path = Utilities.toPath(dep.module.name, Constants.EXT_GRAMMAR);
        File   file = null;
        try {
          file = runtime.locate(path);
        } catch (Exception x) {
          // Ignore.
        }
        if (null == file) {
          System.err.println("[Loading module " + dep.module + "]");
        } else {
          System.err.println("[Loading module " + dep.module + " from " +
                             file + "]");
        }
      }

      // Load module.
      Module m2 = null;

      try {
        m2 = load(dep.module.name);
      } catch (IllegalArgumentException x) {
        runtime.error(x.getMessage(), dep);

      } catch (FileNotFoundException x) {
        runtime.error(x.getMessage(), dep);

      } catch (IOException x) {
        if (null == x.getMessage()) {
          runtime.error("I/O error while accessing corresponding file", dep);
        } else {
          runtime.error(x.getMessage(), dep);
        }

      } catch (ParseException x) {
        System.err.print(x.getMessage());
        runtime.error();
      }

      // Remember that we touched this module, even if it didn't load
      // as there is no need to generate the same error message more
      // than once.
      loaded.put(dep.visibleName(), dep);

      // All the other work requires that we actually loaded the
      // module.
      if (null == m2) continue;

      // Print the module if so requested.
      if (runtime.test("optionLoaded")) {
        pp.dispatch(m2);
        pp.flush();
      }

      // Track errors in this module.
      boolean moduleError = false;

      // Make sure the module name matches the desired module name.
      if (! dep.module.equals(m2.name)) {
        String path = Utilities.toPath(dep.module.name, Constants.EXT_GRAMMAR);
        File   file = null;
        try {
          file = runtime.locate(path);
        } catch (Exception x) {
          // Ignore.
        }
        if (null == file) {
          runtime.error("module name '" + m2.name +
                        "' inconsistent with file name", m2.name);
          moduleError = true;
        } else {
          runtime.error("module name '" + m2.name +
                        "' inconsistent with file name " + file, m2.name);
          moduleError = true;
        }
      }

      // Check the module's parameters (if any).
      if (null != m2.parameters) {
        final int size = m2.parameters.size();
        for (int i=0; i<size; i++) {
          ModuleName name = m2.parameters.get(i);

          if (m2.name.equals(name)) {
            runtime.error("module parameter '" + name + "' same as module name",
                          name);
            moduleError = true;
          }
          
          for (int j=0; j<i; j++) {
            if (name.equals(m2.parameters.get(j))) {
              runtime.error("duplicate module parameter '" + name + "'", name);
              moduleError = true;
              break;
            }
          }
        }
      }

      // Check that argument and parameter numbers match.
      final int argumentNumber  = dep.arguments.size();
      final int parameterNumber =
        (null != m2.parameters)? m2.parameters.size() : 0;
      if (argumentNumber != parameterNumber) {
        StringBuilder buf = new StringBuilder();
        buf.append(argumentNumber);
        buf.append(" argument");
        if (1 != argumentNumber) {
          buf.append('s');
        }
        buf.append(" for module '");
        buf.append(m2.name.name);
        buf.append("' with ");
        buf.append(parameterNumber);
        buf.append(" parameter");
        if (1 != parameterNumber) {
          buf.append('s');
        }
        runtime.error(buf.toString(), dep);
        moduleError = true;
      }

      // Only continue processing this module if there were no errors.
      if (moduleError) continue;

      // Perform any module renaming.
      if ((0 != argumentNumber) || (! dep.module.equals(dep.visibleName()))) {
        if (runtime.test("optionVerbose")) {
          StringBuilder buf = new StringBuilder();

          buf.append("[Instantiating module ");
          buf.append(m2.name);
          buf.append('(');
          for (int i=0; i<argumentNumber; i++) {
            buf.append(dep.arguments.get(i).name);
            buf.append('/');
            buf.append(m2.parameters.get(i).name);
            if (i+1 < argumentNumber) {
              buf.append(", ");
            }
          }
          buf.append(") as ");
          buf.append(dep.visibleName().name);
          buf.append(']');
          System.err.println(buf.toString());
        }

        // Construct a module map for renaming.
        final ModuleMap renaming;
        if (0 != argumentNumber) {
          renaming = new ModuleMap(m2.parameters, dep.arguments);
        } else {
          renaming = new ModuleMap();
        }

        // If the module's declared name and the visible name differ,
        // add the corresponding mapping to the module map.
        if (! dep.module.equals(dep.visibleName())) {
          renaming.put(dep.module, dep.visibleName());
        }

        // Rename the module.
        rename(m2, renaming);

        // The parameters are nulled out below to enable the printing
        // of module dependencies.
      }

      // Add the module to the list of modules and put the module in
      // the module map.
      modules.add(m2);
      moduleMap.put(m2.name.name, m2);

      // Process any further dependencies and fill in the module's
      // auxiliary modification field.
      if (null != m2.dependencies) {
        for (ModuleDependency dep2 : m2.dependencies) {
          if (dep2.isModification()) {
            // Only process one modification per module and do not
            // allow self-mutilation.
            boolean process = true;
            if (null != m2.modification) {
              runtime.error("duplicate modifies declaration", dep2);
              process = false;
            }
            if (dep2.visibleName().equals(m2.name)) {
              runtime.error("module '" + m2.name + "' modifies itself", dep2);
              process = false;
            }
            if (process) {
              m2.modification = (ModuleModification)dep2;
              workList.add(dep2);
            }

          } else {
            workList.add(dep2);
          }
        }
      }

      // Print the signature, if so desired.
      if (runtime.test("optionDependencies")) {
        signature(m2);
      }
      if (null != m2.parameters) {
        m2.setProperty(Constants.ARGUMENTS, m2.parameters);
        m2.parameters = null;
      }
    }

    // Now, check for circular module modifications.  The checked set
    // tracks the names of modules that have been checked.  The
    // checking map contains a mapping from module names (as module
    // names) to booleans.  False indicates that the module has been
    // checked once.  True indicates that the module has been checked
    // twice, meaning it is part of a circular chain of dependencies.
    Map<ModuleName, Boolean> checking = new HashMap<ModuleName, Boolean>();
    Set<ModuleName>          checked  = new HashSet<ModuleName>();
    for (Module m2 : modules) {
      // If the module has been checked already, move on.
      if (checked.contains(m2.name)) continue;

      // Reset the checking map.
      checking.clear();

      do {
        if (checking.containsKey(m2.name)) {
          boolean circular = checking.get(m2.name);

          if (circular) {
            // We are checking this module for the third time and can
            // therefore stop walking the dependencies.
            break;

          } else {
            // We are checking this module for the second time.
            checking.put(m2.name, Boolean.TRUE);
          }

        } else {
          // We are checking this module for the first time.
          checking.put(m2.name, Boolean.FALSE);
        }

        // If this module has a modifies clause, we check the
        // corresponding module next.
        if (null != m2.modification) {
          // Look up the corresponding module.
          m2 = moduleMap.get(m2.modification.visibleName().name);

          if (null == m2) {
            // This is an unresolved or erroneous dependency.  We need
            // to stop here.
            break;
          }

        } else {
          // This module does not modify another.
          break;
        }
      } while (true);

      // Remember all visited modules and report errors for all
      // circular dependencies.  We do this in order of the resolved
      // modules to print errors in a standardized order.
      for (Module m3 : modules) {
        if (checking.containsKey(m3.name)) {
          // Remember as visited.
          checked.add(m3.name);

          // Report error.
          if (checking.get(m3.name)) {
            runtime.error("circular modifies dependency", m3.modification);
          }
        }
      }
    }

    // Create the grammar.
    Grammar g = new Grammar(modules);

    // Print instantiated modules, if so requested.
    if (runtime.test("optionInstantiated")) {
      if (runtime.test("optionHtml")) {
        new HtmlPrinter(runtime, analyzer, ast, false).dispatch(g);
      } else {
        pp = new PrettyPrinter(runtime.console(), ast, true);
        pp.dispatch(g);
        pp.flush();
      }
    }

    // We are done. 
   return runtime.seenError()? null : g;
  }

  /**
   * Perform basic checking for the specified grammar.  This method
   * does all correctness checking besides checking the contents of
   * partial productions and checking for left-recursive definitions.
   * Note that this method initializes the analyzer with the specified
   * grammar.
   *
   * @param g The grammar.
   * @return The list of global attributes, if any.
   */
  protected List<Attribute> check(Grammar g) {
    // Initialize the analyzer.
    analyzer.register(this);
    analyzer.init(g);
    phase = 1;

    // Initialize the top-level module and the list of global
    // attributes.
    Module          root       = g.modules.get(0);
    List<Attribute> attributes = new ArrayList<Attribute>();

    // Make sure that any stateful attributes agree in their class
    // names.  Additionally, collect any stateful, set, or flag
    // attributes in the global list of attributes.
    String   stateName  = null;
    boolean  stateError = false;
    for (Module m2 : g.modules) {
      // Process stateful attribute.
      if (m2.hasAttribute(Constants.ATT_STATEFUL.getName())) {
        Attribute state =
          Attribute.get(Constants.ATT_STATEFUL.getName(), m2.attributes);
        Object    value = state.getValue();
        if ((null != value) &&
            (value instanceof String) &&
            (! ((String)value).startsWith("\""))) {
          if (null == stateName) {
            attributes.add(state);
            stateName = (String)state.getValue();
          }
          if (! stateName.equals(state.getValue())) {
            stateError = true;
          }
        }
      }
        
      // Process set and flag attributes.
      if (m2.hasAttribute(Constants.NAME_STRING_SET) ||
          m2.hasAttribute(Constants.NAME_FLAG)) {
        for (Attribute att : m2.attributes) {
          String name = att.getName();
          if ((Constants.NAME_STRING_SET.equals(name) ||
               Constants.NAME_FLAG.equals(name)) &&
              (! attributes.contains(att))) {
            attributes.add(att);
          }
        }
      }
    }
     
    // Now, do the actual well-formedness checking.
    Set<ModuleName> visited     = new HashSet<ModuleName>();
    boolean         hasTopLevel = false;
    for (Module m2 : g.modules) {
      analyzer.process(m2);
      hasState    = false;
      isMofunctor = (null != m2.modification);

      if (null != m2.attributes) {
        final int length = m2.attributes.size();
        for (int i=0; i<length; i++) {
          Attribute att   = m2.attributes.get(i);
          String    name  = att.getName();
          Object    value = att.getValue();

          if ((! Constants.ATT_WITH_LOCATION.equals(att)) &&
              (! Constants.ATT_CONSTANT.equals(att)) &&
              (! Constants.ATT_RAW_TYPES.equals(att)) &&
              (! Constants.ATT_VERBOSE.equals(att)) &&
              (! Constants.ATT_NO_WARNINGS.equals(att)) &&
              (! Constants.ATT_IGNORING_CASE.equals(att)) &&
              (! Constants.ATT_STATEFUL.getName().equals(name)) &&
              (! Constants.NAME_PARSER.equals(name)) &&
              (! Constants.NAME_MAIN.equals(name)) &&
              (! Constants.NAME_PRINTER.equals(name)) &&
              (! Constants.NAME_VISIBILITY.equals(name)) &&
              (! Constants.NAME_STRING_SET.equals(name)) &&
              (! Constants.NAME_FLAG.equals(name)) &&
              (! Constants.NAME_FACTORY.equals(name)) &&
              (! Constants.ATT_FLATTEN.equals(att)) &&
              (! Constants.ATT_GENERIC_AS_VOID.equals(att)) &&
              (! Constants.ATT_PARSE_TREE.equals(att)) &&
              (! Constants.ATT_PROFILE.equals(att)) &&
              (! Constants.ATT_DUMP.equals(att))) {
            runtime.error("unrecognized grammar-wide attribute '"+att+"'", att);
            
          } else {
            for (int j=0; j<i; j++) {
              Attribute att2 = m2.attributes.get(j);
              if (name.equals(Constants.NAME_STRING_SET) ||
                  name.equals(Constants.NAME_FLAG)) {
                if (att.equals(att2)) {
                  runtime.error("duplicate attribute '"+att+"'", att);
                  break;
                } else if ((null != value) && value.equals(att2.getValue())) {
                  runtime.error("duplicate field name '" + att + "'", att);
                }

              } else if (name.equals(att2.getName())) {
                runtime.error("duplicate attribute '" + name + "'", att);
                break;
              }
            }
          }
          
          if (Constants.ATT_STATEFUL.getName().equals(name)) {
            if (null == value) {
              runtime.error("stateful attribute without class name", att);
            } else if (! (value instanceof String)) {
              runtime.error("stateful attribute with invalid value", att);
            } else if (((String)value).startsWith("\"")) {
              runtime.error("stateful attribute with invalid value", att);
            } else if (stateError) {
              runtime.error("inconsistent state class across modules", att);
            }
            hasState = true;

          } else if (Constants.NAME_PARSER.equals(name)) {
            if (null == value) {
              runtime.error("parser attribute without class name", att);
            } else if (! (value instanceof String)) {
              runtime.error("parser attribute with invalid value", att);
            } else if (((String)value).startsWith("\"")) {
              runtime.error("parser attribute with invalid value", att);
            }
            
          } else if (Constants.NAME_MAIN.equals(name)) {
            if (runtime.test("optionLGPL")) {
              runtime.error("main attribute incompatible with LGPL", att);
            } else if (null == value) {
              runtime.error("main attribute without nonterminal value", att);
            } else if (! (value instanceof String)) {
              runtime.error("main attribute with invalid value", att);
            } else if (((String)value).startsWith("\"")) {
              runtime.error("main attribute with invalid value", att);
            } else {
              NonTerminal nt  = new NonTerminal((String)value);
              Production  p   = null;
              boolean     err = false;

              try {
                p = analyzer.lookup(nt);
              } catch (IllegalArgumentException x) {
                runtime.error("main attribute with ambiguous nonterminal '" +
                              nt + "'", att);
                err = true;
              }

              if (! err) {
                if (null == p) {
                  runtime.error("main attribute with undefined nonterminal '" +
                                nt + "'", att);
                } else if (! analyzer.isDefined(p, m2)) {
                  runtime.error("main attribute with another module's " +
                                "nonterminal '" + nt + "'", att);
                } else if (! p.hasAttribute(Constants.ATT_PUBLIC)) {
                  runtime.error("main attribute with non-public nonterminal '" +
                                nt + "'", att);
                }
              }
            }
            
          } else if (Constants.NAME_PRINTER.equals(name)) {
            if (runtime.test("optionLGPL")) {
              runtime.error("printer attribute incompatible with LGPL", att);
            } else if (null == value) {
              runtime.error("printer attribute without class name", att);
            } else if (! (value instanceof String)) {
              runtime.error("printer attribute with invalid value", att);
            } else if (((String)value).startsWith("\"")) {
              runtime.error("printer attribute with invalid value", att);
            }
            if (! m2.hasAttribute(Constants.NAME_MAIN)) {
              runtime.error("printer attribute without main attribute", att);
            }
            
          } else if (Constants.NAME_VISIBILITY.equals(name)) {
            if (null == value) {
              runtime.error("visibility attribute without value", att);
            } else if ((! Constants.ATT_PUBLIC.getValue().equals(value)) &&
                       (! Constants.ATT_PACKAGE_PRIVATE.getValue().
                        equals(value))) {
              runtime.error("visibility attribute with invalid value", att);
            }

          } else if (Constants.NAME_STRING_SET.equals(name)) {
            if (null == value) {
              runtime.error("string set attribute without set value", att);
            } else if (! (value instanceof String)) {
              runtime.error("string set attribute with invalid value", att);
            } else if (((String)value).startsWith("\"")) {
              runtime.error("string set attribute with invalid value", att);
            }

          } else if (Constants.NAME_FLAG.equals(name)) {
            if (null == value) {
              runtime.error("flag attribute without flag value", att);
            } else if (! (value instanceof String)) {
              runtime.error("flag attribute with invalid value", att);
            } else if (((String)value).startsWith("\"")) {
              runtime.error("flag attribute with invalid value", att);
            }

          } else if (Constants.NAME_FACTORY.equals(name)) {
            if (null == value) {
              runtime.error("factory attribute without class name", att);
            } else if (! (value instanceof String)) {
              runtime.error("factory attribute with invalid value", att);
            } else if (((String)value).startsWith("\"")) {
              runtime.error("factory attribute with invalud value", att);
            }

          } else if (Constants.ATT_GENERIC_AS_VOID.equals(att)) {
            if (m2.hasAttribute(Constants.ATT_PARSE_TREE)) {
              runtime.error("genericAsVoid attribute incompatible with " +
                            "withParseTree attribute", att);
            }

          } else if (Constants.ATT_DUMP.equals(att)) {
            if (runtime.test("optionLGPL")) {
              runtime.error("dump attribute incompatible with LGPL", att);
            }

          } else if (Constants.ATT_RAW_TYPES.equals(att)) {
            runtime.warning("the rawTypes attribute has been deprecated", att);
            runtime.errConsole().loc(att).
              pln(": warning: and will be removed in a future release").flush();
          }
        }
      }

      // If this module does not have a stateful attribute, check the
      // the current module's direct imports as well as any modified
      // modules and their direct imports.
      if (! hasState) {
        hasState =
          analyzer.hasAttribute(m2, Constants.ATT_STATEFUL.getName(), visited);
        visited.clear();
      }

      // Check the productions.
      for (Production p : m2.productions) {
        // Initialize the per-production state.
        sequenceNames.clear();

        // Process the production.
        analyzer.process(p);

        // Check for duplicate definitions.
        try {
          if ((! p.isPartial()) && (p != analyzer.lookup(p.name))) {
            runtime.error("duplicate definition for nonterminal '" + p.name +
                          "'", p);
          }
        } catch (IllegalArgumentException x) {
          runtime.error("duplicate definition for nonterminal '" + p.name +
                        "'", p);
        }

        // Process top-level productions.  Only public productions
        // from the top-level module are recognized as top-level for
        // the entire grammar.
        if (p.hasAttribute(Constants.ATT_PUBLIC)) {
          if (analyzer.isDefined(p, root)) {
            hasTopLevel = true;
          } else {
            p.attributes.remove(Constants.ATT_PUBLIC);
          }
        }
      }
    }

    // Check for top-level nonterminals.
    if (! hasTopLevel) {
      runtime.error("no public nonterminal",
                    g.modules.get(0).productions.get(0));
    }

    // Done.
    return attributes;
  }

  /**
   * Apply any module modifications.  This method applies all module
   * modifications appearing in the specified grammar.  It also
   * removes unused modules from the grammar.  This method assumes
   * that all module dependencies have been successfully resolved,
   * i.e., the specified grammar contains all module dependencies.
   * This method also assumes that each module modification's {@link
   * Module#modification modification} field has been correctly
   * initialized.
   *
   * @param g The grammar.
   */
  protected void applyModifications(Grammar g) {
    // Initialize the analyzer.
    analyzer.register(this);
    analyzer.init(g);
    phase = 2;

    // Calculate the transitive closure of imported and modified
    // modules.
    Module                   root     = g.modules.get(0);
    Set<ModuleName>          imports  = new HashSet<ModuleName>();
    Map<ModuleName, Boolean> modified = new HashMap<ModuleName, Boolean>();
    imports.add(root.name);
    analyzer.trace(root, imports, modified);

    // Remove any modules that are not reachable from the top-level
    // module (i.e., modules that have been instantiated but never
    // imported or modified).
    for (Iterator<Module> iter = g.modules.iterator(); iter.hasNext(); ) {
      Module m = iter.next();
      if ((! imports.contains(m.name)) && (! modified.containsKey(m.name))) {
        if (runtime.test("optionVerbose")) {
          System.err.println("[Unloading unused module " + m.name + "]");
        }
        analyzer.remove(m);
        iter.remove();
      }
    }

    // If there are any modifications, apply them.
    if (0 != modified.size()) {
      // Set up the list of module modifications (in dependency order)
      // and the base module they are applied to.  Also track the set
      // of modules with errors.  Furthermore, track the set of
      // modules checked in phase 2.  Finally, create the maps for the
      // target module's productions and for both the target and
      // source modules' productions.
      List<Module>                 mofunctors        = new ArrayList<Module>();
      Module                       base;
      Set<Module>                  erroneous         = new HashSet<Module>();
      Set<Module>                  checked           = new HashSet<Module>();
      Map<NonTerminal, Production> targetProductions =
        new HashMap<NonTerminal, Production>();
      Map<NonTerminal, Production> productions       =
        new HashMap<NonTerminal, Production>();

      while (true) {
        // Find a module modification to apply.
        mofunctors.clear();
        base = null;
        for (Module m : g.modules) {
          if ((null != m.modification) &&
              (! erroneous.contains(m))) {
            // We found a module modification without errors.  Follow
            // the the chain of dependencies until we reach the base
            // module.
            do {
              mofunctors.add(m);
              m = analyzer.lookup(m.modification.visibleName());
            } while (null != m.modification);
            base = m;

            if (erroneous.contains(base)) {
              // We have seen an error before.
              erroneous.addAll(mofunctors);
              base = null;
            }

            // We are done looking for a module modification.
            break;
          }
        }

        if (null == base) {
          // Nothing left to do, either because all module
          // modifications have been applied or there have been
          // errors.
          break;
        }

        // Set up the target module.
        Module target = base;

        // Iterate over the module modifications and apply them to the
        // target.
        for (int i=mofunctors.size()-1; i>=0; i--) {
          Module source = mofunctors.get(i);

          if (runtime.test("optionVerbose")) {
            System.err.println("[Applying module " + source.name +
                               " to module " + target.name + "]");
          }

          // If the target module is modified in more than one way or
          // both modified and imported, then we need to modify a
          // copy.
          if (modified.get(target.name) ||
              (modified.containsKey(target.name) &&
               imports.contains(target.name))) {
            if (runtime.test("optionVerbose")) {
              System.err.println("[Copying modified module " + target.name +
                                 "]");
            }
            target = analyzer.copy(target);

          } else {
            // Remove the module from the analyzer's state, since we
            // will update that state after applying the modification.
            // Also, remove the module from the grammar, as it
            // replaces the source module.
            if (runtime.test("optionVerbose")) {
              System.err.println("[Removing modified module " + target.name +
                                 "]");
            }
            analyzer.remove(target);
            g.remove(target);
          }

          // Since the source module is effectively replaced by the
          // target module, we remove it from the analyzer state and
          // replace it in the grammar.  We need to preserve the
          // position in the grammar as the top-level module may be a
          // module modification.
          if (runtime.test("optionVerbose")) {
            System.err.println("[Removing modifying module " + source.name +
                               "]");
          }
          analyzer.remove(source);
          g.replace(source, target);

          // Rename the target module.
          rename(target, new ModuleMap(target.name, source.name));

          // Patch the target module's documentation comment.
          target.documentation = source.documentation;

          // Explicitly replace the target module's name with the
          // source module's to preserve the original name, if any.
          target.name = source.name;

          // Similarly, patch the arguments property.
          target.removeProperty(Constants.ARGUMENTS);
          if (source.hasProperty(Constants.ARGUMENTS)) {
            target.setProperty(Constants.ARGUMENTS,
                               source.getProperty(Constants.ARGUMENTS));
          }

          // Add the source module's dependencies, with exception of
          // the modification, to the target module's dependencies.
          // Note that we don't need to check whether the source
          // module has dependencies, as it must have at least one
          // modification dependency.
          Iterator<ModuleDependency> iter2 = source.dependencies.iterator();
          while (iter2.hasNext()) {
            if (iter2.next().isModification()) {
              iter2.remove();
              // We continue iterating, just in case that there are
              // several modifications.
            }
          }

          if (null == target.dependencies) {
            target.dependencies = source.dependencies;
          } else {
            target.dependencies.addAll(source.dependencies);
          }

          // Add the source module's header, body, and footer actions
          // to the target module, if they exist.
          if (null != source.header) {
            if (null == target.header) {
              target.header = source.header;
            } else {
              target.header.add(source.header);
            }
          }

          if (null != source.body) {
            if (null == target.body) {
              target.body = source.body;
            } else {
              target.body.add(source.body);
            }
          }

          if (null != source.footer) {
            if (null == target.footer) {
              target.footer = source.footer;
            } else {
              target.footer.add(source.footer);
            }
          }

          // Replace the target module's options with the source
          // module's options.  However, if the target module has a
          // stateful, set, or flag option, while the source module
          // does not have that same option, add the corresponding
          // option to the list of options first.
          if (null != target.attributes) {
            // Process stateful attribute.
            if (target.hasAttribute(Constants.ATT_STATEFUL.getName())) {
              if (null == source.attributes) {
                source.attributes = new ArrayList<Attribute>();
              }
              if (! source.hasAttribute(Constants.ATT_STATEFUL.getName())) {
                source.attributes.
                  add(Attribute.get(Constants.ATT_STATEFUL.getName(),
                                    target.attributes));
              }
            }

            // Process set and flag attributes.
            if (target.hasAttribute(Constants.NAME_STRING_SET) ||
                target.hasAttribute(Constants.NAME_FLAG)) {
              if (null == source.attributes) {
                source.attributes = new ArrayList<Attribute>();
              }
              for (Attribute att : target.attributes) {
                String name = att.getName();
                if ((Constants.NAME_STRING_SET.equals(name) ||
                     Constants.NAME_FLAG.equals(name)) &&
                    (! source.attributes.contains(att))) {
                  source.attributes.add(att);
                }
              }
            }
          }
          target.attributes = source.attributes;

          // Build a map of the target module's productions and a map
          // of both the target and source modules' productions.
          // While building the maps, also strip each production.
          targetProductions.clear();
          productions.clear();
          for (Production p : target.productions) {
            p = strip(p);
            if (p.isFull() && (! productions.containsKey(p.name))) {
              targetProductions.put(p.name, p);
              productions.put(p.name, p);
            }
          }
          for (Production p : source.productions) {
            p = strip(p);
            if (p.isFull() && (! productions.containsKey(p.name))) {
              productions.put(p.name, p);
            }
          }

          // Process the source module's productions.
          int errorCount = runtime.errorCount();
          for (Production p1 : source.productions) {
            FullProduction p2 = (FullProduction)productions.get(p1.name);
            
            if (p1.isFull()) {
              // If the source module's production is a duplicate
              // definition, that error has already been reported.
              // Otherwise, we simply add it to the target module.
              if (! targetProductions.containsKey(p1.name)) {
                target.productions.add(p1);
                targetProductions.put(p1.name, p1);
              }

            } else if (null != p2) {
              if (p1.isAddition()) {
                apply((AlternativeAddition)p1, p2);

              } else if (p1.isRemoval()) {
                apply((AlternativeRemoval)p1, p2);

              } else if (p1.isOverride()) {
                apply((ProductionOverride)p1, p2);

              } else {
                throw new AssertionError("Unrecognized production " + p1);
              }
            }
          }

          // Add the target module into the analyzer's state.  Also,
          // mark the target module as checked in phase 2.
          if (runtime.test("optionVerbose")) {
            System.err.println("[Adding resulting module " + target.name + "]");
          }
          analyzer.add(target);
          checked.add(target);

          // Re-check the target module for duplicate sequence names
          // and for ambiguous or undefined nonterminals.
          analyzer.process(target);
          for (Production p : target.productions) {
            sequenceNames.clear();
            analyzer.process(p);
          }

          // If we have seen any errors, we mark the target module and
          // any remaining module modifications as erroneous.
          if (runtime.errorCount() != errorCount) {
            erroneous.add(target);
            List<Module> l = mofunctors.subList(0, i);
            erroneous.addAll(l);
            checked.addAll(l);
          }
        }

        // Recalculate the transitive closure of imported and modified
        // modules.  We have to re-initialize the root, since that
        // module may have changed.
        root = g.modules.get(0);
        imports.clear();
        modified.clear();
        imports.add(root.name);
        analyzer.trace(root, imports, modified);

        // We do not need to remove unreachable modules, because the
        // initial removal has already removed all of them.
      }

      // Check all modules that have not been checked in phase 2 in
      // order to detect amgibuous nonterminals.
      phase = 3;
      for (Module m : g.modules) {
        if (! checked.contains(m)) {
          analyzer.process(m);
          for (Production p : m.productions) {
            analyzer.process(p);
          }
        }
      }
    }

    // Print the resulting modules if requested.
    if (runtime.test("optionApplied")) {
      if (runtime.test("optionHtml")) {
        new HtmlPrinter(runtime, analyzer, ast, false).dispatch(g);
      } else {
        PrettyPrinter pp = new PrettyPrinter(runtime.console(), ast, true);
        pp.dispatch(g);
        pp.flush();
      }
    }

    // Done.
  }

  /**
   * Intern the grammar's types.  This method assumes that all module
   * modifications have been applied.
   *
   * @param g The grammar.
   */
  protected void internTypes(Grammar g) {
    // Determine the grammar's features.
    boolean hasNode       = false;
    boolean hasToken      = false;
    boolean hasFormatting = false;
    boolean hasAction     = false;

    // Check for parse trees.
    if (g.modules.get(0).hasAttribute(Constants.ATT_PARSE_TREE)) {
      hasToken      = true;
      hasFormatting = true;
    }
    
    // Check for generic productions, including generic productions
    // that are directly left-recursive.
    for (Module m : g.modules) {
      for (Production p : m.productions) {
        // Check for generic productions.
        if (ast.isGenericNode(p.dType)) {
          hasNode = true;
          
          // Check for direct left recursions in generic productions.
          for (Sequence s : p.choice.alternatives) {
            final Element first = s.isEmpty() ? null :
              Analyzer.stripAndUnbind(s.get(0));

            if (p.name.equals(first) || p.qName.equals(first)) {
              hasAction = true;
            }
          }
        }
      }
    }

    // Only annotate the grammar if the productions are not going to
    // be voided out.
    if (! g.modules.get(0).hasAttribute(Constants.ATT_GENERIC_AS_VOID)) {
      if (hasNode) {
        g.modules.get(0).setProperty(Properties.GENERIC, Boolean.TRUE);
      }
      if (hasAction) {
        g.modules.get(0).setProperty(Properties.RECURSIVE, Boolean.TRUE);
      }
    }

    // Initialize the grammar's type map.
    ast.initialize(hasNode, hasToken, hasFormatting, hasAction);

    // Intern the types.
    for (Module m : g.modules) {
      for (Production p : m.productions) {
        Type t;

        try {
          t = ast.intern(p.dType);
        } catch (IllegalArgumentException x) {
          runtime.error(x.getMessage() + " for production '" + p.name + "'", p);
          t = ErrorT.TYPE;
        }

        p.type = t;
      }
    }

    // Check variant productions.
    for (Module m : g.modules) {
      for (Production p : m.productions) {
        if (p.hasAttribute(Constants.ATT_VARIANT)) {
          if (AST.isToken(p.type)) {
            runtime.error("variant production for token type", p);
          } else if (! AST.isNode(p.type)) {
            runtime.error("variant production without node type", p);
          }
        }
      }
    }
  }

  /**
   * Check for left-recursive productions.  This method assumes that
   * all module modifications have been applied.
   *
   * @param g The grammar.
   */
  protected void checkRecursions(Grammar g) {
    new TextTester(runtime, analyzer).dispatch(g);
    LeftRecurser     recurser  = new LeftRecurser(runtime, analyzer);
    recurser.dispatch(g);
    Set<NonTerminal> recursive = recurser.recursive();

    for (Module m : g.modules) {
      for (Production p : m.productions) {
        if (recursive.contains(p.qName)) {
          // Report the production as malformed.
          runtime.error("left-recursive definition for nonterminal '" +
                        p.name + "'", p);
        }
      }
    }
  }

  /**
   * Check repetitions for elements that accept the empty input.  This
   * method assumes that all module modifications have been applied.
   *
   * @param g The grammar.
   */
  protected void checkRepetitions(Grammar g) {
    analyzer.register(checkRepetitionsVisitor);
    analyzer.init(g);

    for (Module m : g.modules) {
      analyzer.process(m);
      for (Production p : m.productions) {
        if (p.isFull()) {
          analyzer.process(p);
        }
      }
    }
  }

  /** The visitor for checking repetitions. */
  @SuppressWarnings("unused")
  private final Visitor checkRepetitionsVisitor = new Visitor() {
      public void visit(Repetition r) {
        dispatch(r.element);
        if (analyzer.matchesEmpty(r.element) &&
            ! (Analyzer.strip(r.element) instanceof Action)) {
          runtime.error("repeated element matches empty input", r);
        }
      }
      public void visit(Option o) {
        dispatch(o.element);
        if (! analyzer.restrictsInput(o.element) &&
            ! (Analyzer.strip(o.element) instanceof Action) &&
            ! analyzer.grammar().modules.get(0).
              hasAttribute(Constants.ATT_NO_WARNINGS) &&
            ! analyzer.current().hasAttribute(Constants.ATT_NO_WARNINGS)) {
          runtime.warning("optional element already matches empty input", o);
        }
      }
      public void visit(Production p) {
        dispatch(p.choice);
      }
      public void visit(OrderedChoice c) {
        for (Sequence alt : c.alternatives) dispatch(alt);
      }
      public void visit(Sequence s) {
        for (Element e : s.elements) dispatch(e);
      }
      public void visit(UnaryOperator op) {
        dispatch(op.element);
      }
      public void visit(Element e) {
        // Nothing to do.
      }
    };

  /**
   * Check that explicit productions do not match the empty input.
   *
   * @param g The grammar.
   */
  protected void checkExplicit(Grammar g) {
    analyzer.register(checkExplicitVisitor);
    analyzer.init(g);

    for (Module m : g.modules) {
      analyzer.process(m);
      for (Production p : m.productions) {
        if (p.isFull()) analyzer.process(p);
      }
    }
  }

  /** The visitor for checking explicit productions. */
  @SuppressWarnings("unused")
  private final Visitor checkExplicitVisitor = new Visitor() {
      public void visit(Production p) {
        if (p.hasAttribute(Constants.ATT_EXPLICIT) &&
            analyzer.matchesEmpty(p.choice)) {
          runtime.error("explicit production matches empty input", p);
        }
      }
    };


  /**
   * Combine the modules in the specified grammar.  This method
   * combines the modules in the grammar by modifying the grammar's
   * top-level module and then returns that module.
   *
   * @param g The grammar.
   * @param attributes The list of global attributes.
   * @return The grammar as a single module.
   */
  protected Module combine(Grammar g, List<Attribute> attributes) {
    // Get the top-level module.
    Module m = g.modules.get(0);

    // Null out the dependencies.
    m.dependencies = null;
    m.modification = null;

    // Make sure that the single module has all global attributes.
    if (0 < attributes.size()) {
      if (null == m.attributes) {
        m.attributes = attributes;

      } else {
        for (Attribute att : attributes) {
          if (! m.attributes.contains(att)) {
            m.attributes.add(att);
          }
        }
      }
    }

    // Collect headers, bodies, and footers.  We need to make sure
    // that we do not add the same header, body, or footer more than
    // once, since, in the presence of parameterized modules and
    // module modifications, the same source module can be
    // instantiated several times.
    List<Action> headers = new ArrayList<Action>();
    List<Action> bodies  = new ArrayList<Action>();
    List<Action> footers = new ArrayList<Action>();
    for (Module m2 : g.modules) {
      if ((null != m2.header) && (! headers.contains(m2.header))) {
        headers.add(m2.header);
      }

      if ((null != m2.body) && (! bodies.contains(m2.body))) {
        bodies.add(m2.body);
      }

      if ((null != m2.footer) && (! footers.contains(m2.footer))) {
        footers.add(m2.footer);
      }
    }

    // Combine all unique headers, bodies, and footers into a single
    // header, body, and footer, respectively.
    m.header = null;
    for (Action a : headers) {
      if (null == m.header) {
        m.header = a;
      } else {
        m.header.add(a);
      }
    }

    m.body = null;
    for (Action a : bodies) {
      if (null == m.body) {
        m.body = a;
      } else {
        m.body.add(a);
      }
    }

    m.footer = null;
    for (Action a : footers) {
      if (null == m.footer) {
        m.footer = a;
      } else {
        m.footer.add(a);
      }
    }

    // Now add in all productions.
    for (Module m2 : g.modules) {
      if (m != m2) {
        m.productions.addAll(m2.productions);
      }
    }

    return m;
  }

  /**
   * Visit the specified module.
   *
   * @param m The module to resolve.
   * @return A single, self-contained grammar module or
   *   <code>null</code> on an error condition.
   */
  public Object visit(Module m) {
    // Reset the resolver state.
    badNTs.clear();

    // ----------------------------------------------------------------------
    //                       Load all dependent modules.
    // ----------------------------------------------------------------------

    Grammar g = load(m);

    // Only continue if there were no errors.
    if (runtime.seenError() ||
        runtime.test("optionLoaded") ||
        runtime.test("optionDependencies") ||
        runtime.test("optionInstantiated")) {
      return null;
    }

    // ----------------------------------------------------------------------
    //                     Check as much as possible.
    // ----------------------------------------------------------------------

    List<Attribute> attributes = check(g);

    // ----------------------------------------------------------------------
    //                    Apply any module modifications.
    // ----------------------------------------------------------------------

    applyModifications(g);
    if (runtime.test("optionApplied")) return null;

    // ----------------------------------------------------------------------
    //                          Intern types.
    // ----------------------------------------------------------------------

    internTypes(g);

    // ----------------------------------------------------------------------
    //   Perform checks that require that modifications have been applied.
    // ----------------------------------------------------------------------

    checkRecursions(g);
    checkRepetitions(g);
    new ReachabilityChecker(runtime, analyzer).dispatch(g);
    checkExplicit(g);

    // Only continue if there were no errors.
    if (runtime.seenError()) return null;

    // ----------------------------------------------------------------------
    //                 Rename nonterminals for consistency.
    // ----------------------------------------------------------------------

    analyzer.uniquify();

    // ----------------------------------------------------------------------
    //                Combine all modules into a single module.
    // ----------------------------------------------------------------------

    return combine(g, attributes);
  }

  /** Analyze the specified production. */
  public void visit(Production p) {
    if (1 == phase) {
      if (Constants.ATT_PACKAGE_PRIVATE.getValue().equals(p.dType) ||
          Constants.ATT_INLINE.getName().equals(p.dType)) {
        runtime.error("attribute '" + p.dType + "' as type for production '" +
                      p.name + "'", p);
        
      } else if (Constants.ATT_WITH_LOCATION.getName().equals(p.dType) ||
                 Constants.ATT_CONSTANT.getName().equals(p.dType) ||
                 Constants.ATT_EXPLICIT.getName().equals(p.dType) ||
                 Constants.ATT_NO_INLINE.getName().equals(p.dType) ||
                 Constants.ATT_MEMOIZED.getName().equals(p.dType) ||
                 Constants.ATT_VERBOSE.getName().equals(p.dType) ||
                 Constants.ATT_VARIANT.getName().equals(p.dType) ||
                 Constants.ATT_IGNORING_CASE.getName().equals(p.dType) ||
                 Constants.ATT_STATEFUL.getName().equals(p.dType) ||
                 Constants.ATT_RESETTING.getName().equals(p.dType)) {
        if (! p.isPartial() &&
            ! analyzer.grammar().modules.get(0).
              hasAttribute(Constants.ATT_NO_WARNINGS) &&
            ! p.hasAttribute(Constants.ATT_NO_WARNINGS)) {
          runtime.warning("attribute '" + p.dType + "' as type for production " +
                          p.name + "'", p);
        }
      }
      
      if (p.isPartial()) {
        if (! isMofunctor) {
          runtime.error("production modification '" + p.name +
                        "' without modifies declaration", p);
          
        } else {
          // Make sure the corresponding full production is defined
          // and the types match.
          Production p2 = null;
          try {
            p2 = analyzer.lookup(p.name);
          } catch (IllegalArgumentException x) {
            // Nothing to do.
          }
          
          if ((null == p2) ||
              (! analyzer.isDefined(p2, analyzer.currentModule()))) {
            runtime.error("production modification '" + p.name +
                          "' without full production", p);
            
          } else if (! p.dType.equals(p2.dType)) {
            runtime.error("type '" + p.dType + "' of production modification '" +
                          p.name + "' does not match full production's type", p);
          }
        }
      }
      
      if (null != p.attributes) {
        final int length = p.attributes.size();
        for (int i=0; i<length; i++) {
          Attribute att = p.attributes.get(i);
          
          if ((! Constants.ATT_PUBLIC.equals(att)) &&
              (! Constants.ATT_PROTECTED.equals(att)) &&
              (! Constants.ATT_PRIVATE.equals(att)) &&
              (! Constants.ATT_TRANSIENT.equals(att)) &&
              (! Constants.ATT_INLINE.equals(att)) &&
              (! Constants.ATT_NO_INLINE.equals(att)) &&
              (! Constants.ATT_MEMOIZED.equals(att)) &&
              (! Constants.ATT_WITH_LOCATION.equals(att)) &&
              (! Constants.ATT_CONSTANT.equals(att)) &&
              (! Constants.ATT_VARIANT.equals(att)) &&
              (! Constants.ATT_EXPLICIT.equals(att)) &&
              (! Constants.ATT_VERBOSE.equals(att)) &&
              (! Constants.ATT_NO_WARNINGS.equals(att)) &&
              (! Constants.ATT_IGNORING_CASE.equals(att)) &&
              (! Constants.ATT_STATEFUL.equals(att)) &&
              (! Constants.ATT_RESETTING.equals(att))) {
            runtime.error("unrecognized per-production attribute '" + att +
                          "'", att);

          } else if ((! hasState) && Constants.ATT_STATEFUL.equals(att)) {
            runtime.error("stateful attribute without grammar-wide stateful " +
                          "attribute", att);
            
          } else if ((! hasState) && Constants.ATT_RESETTING.equals(att)) {
            runtime.error("resetting attribute without grammar-wide stateful " +
                          "attribute", att);
            
          } else {
            for (int j=0; j<i; j++) {
              if (att.equals(p.attributes.get(j))) {
                runtime.error("duplicate attribute '" + att.getName() + "'",
                              att);
                break;
              }
            }
          }
        }

        if (p.hasAttribute(Constants.ATT_MEMOIZED)) {
          if (p.hasAttribute(Constants.ATT_TRANSIENT)) {
            runtime.error("memozied attribute contradicts transient attribute",
                          Attribute.get(Constants.ATT_MEMOIZED.getName(),
                                        p.attributes));
          }
          if (p.hasAttribute(Constants.ATT_INLINE)) {
            runtime.error("memoized attribute contradicts inline attribute",
                          Attribute.get(Constants.ATT_MEMOIZED.getName(),
                                        p.attributes));
          }
        }
        if (p.hasAttribute(Constants.ATT_TRANSIENT) &&
            p.hasAttribute(Constants.ATT_INLINE)) {
          runtime.error("inline attribute subsumes transient attribute",
                        Attribute.get(Constants.ATT_INLINE.getName(),
                                      p.attributes));
        }
        if (p.hasAttribute(Constants.ATT_INLINE) &&
            p.hasAttribute(Constants.ATT_NO_INLINE)) {
          runtime.error("inline attribute contradicts noinline attribute",
                        Attribute.get(Constants.ATT_NO_INLINE.getName(),
                                      p.attributes));
        }
      }
    }

    dispatch(p.choice);
  }
  
  /** Analyze the specified ordered choice. */
  public void visit(OrderedChoice c) {
    for (Sequence alt : c.alternatives) dispatch(alt);
  }

  /** Analyze the specified repetition. */
  public void visit(Repetition r) {
    if (1 == phase) {
      if (Analyzer.strip(r.element) instanceof Action) {
        runtime.error("repeated action", r);
      }
    }
    dispatch(r.element);
  }

  /** Analyze the specified option.  */
  public void visit(Option o) {
    if (1 == phase) {
      if (Analyzer.strip(o.element) instanceof Action) {
        runtime.error("optional action", o);
      }
    }
    dispatch(o.element);
  }
  
  /** Analyze the specified sequence. */
  public void visit(Sequence s) {
    if ((1 == phase) || (2 == phase)) {
      if (null != s.name) {
        if (sequenceNames.contains(s.name)) {
          runtime.error("duplicate sequence name '" + s.name + "'", s);
        } else {
          sequenceNames.add(s.name);
        }
      }
    }

    for (Element e : s.elements) dispatch(e);
  }

  /** Analyze the specified predicate. */
  public void visit(Predicate p) {
    if (1 == phase) {
      if (isPredicate) {
        runtime.error("syntactic predicate within syntactic predicate", p);
      }
    }

    boolean pred = isPredicate;
    isPredicate  = true;
    dispatch(p.element);
    isPredicate  = pred;
  }

  /** Analyze the specified semantic predicate. */
  public void visit(SemanticPredicate p ) {
    if (1 == phase) {
      if (! (p.element instanceof Action)) {
        runtime.error("malformed semantic predicate", p);
      } else {
        Action a = (Action)p.element;
        if ((null == a.code) || (0 >= a.code.size())) {
          runtime.error("empty test for semantic predicate", p);
        }
      }
    }

    dispatch(p.element);
  }

  /** Analyze the specified voided element. */
  public void visit(VoidedElement v) {
    if (1 == phase) {
      // We allow void:void:e and void:nt for void nonterminals, since
      // the simplifier removes the unnecessary voided elements.
      Element voided = Analyzer.strip(v.element);

      switch (voided.tag()) {
      case FOLLOWED_BY:
      case NOT_FOLLOWED_BY:
      case SEMANTIC_PREDICATE:
        runtime.error("voided predicate", v);
        break;

      case BINDING:
        runtime.error("voided binding", v);
        break;

      case ACTION:
        runtime.error("voided action", v);
        break;

      case PARSER_ACTION:
        runtime.error("voided parser action", v);
        break;

      case NULL:
        runtime.error("voided null literal", v);
        break;

      case NODE_MARKER:
        runtime.error("voided node marker", v);
        break;
      }
    }

    dispatch(v.element);
  }

  /** Analyze the specified binding. */
  public void visit(Binding b) {
    if (1 == phase) {
      Element bound = Analyzer.strip(b.element);

      switch (bound.tag()) {
      case FOLLOWED_BY:
      case NOT_FOLLOWED_BY:
      case SEMANTIC_PREDICATE:
        runtime.error("binding for predicate", b);
        break;

      case VOIDED:
        runtime.error("binding for voided element", b);
        break;

      case BINDING:
        runtime.error("binding for binding", b);
        break;

      case NONTERMINAL: {
        NonTerminal nt = (NonTerminal)bound;
        Production  p  = null;
        try {
          p = analyzer.lookup(nt);
        } catch (IllegalArgumentException x) {
          // Nothing to do.
        }
        if ((null != p) && ast.isVoid(p.dType)) {
          runtime.error("binding for void nonterminal '" + nt + "'", b);
        }
      } break;

      case ACTION:
        runtime.error("binding for action", b);
        break;

      case PARSER_ACTION:
        runtime.error("binding for parser action", b);
        break;

      case NODE_MARKER:
        runtime.error("binding for node marker", b);
        break;
      }
    }

    dispatch(b.element);
  }

  /** Analyze the specified string match. */
  public void visit(StringMatch m) {
    if (1 == phase) {
      Element matched = Analyzer.strip(m.element);

      switch (matched.tag()) {
      case FOLLOWED_BY:
      case NOT_FOLLOWED_BY:
      case SEMANTIC_PREDICATE:
        runtime.error("string match on predicate", m);
        break;

      case VOIDED:
        runtime.error("string match on voided element", m);
        break;

      case BINDING:
        runtime.error("string match on binding " +
                      "(try binding the match instead)", m);
        break;

      case STRING_MATCH:
        runtime.error("string match on another string match", m);
        break;

      case NONTERMINAL: {
        NonTerminal nt = (NonTerminal)matched;
        Production  p  = null;
        try {
          p = analyzer.lookup(nt);
        } catch (IllegalArgumentException x) {
          // Nothing to do.
        }
        if ((null != p) && ast.isVoid(p.dType)) {
          runtime.error("string match on void nonterminal '" + nt + "'", m);
        }
      } break;

      case ANY_CHAR:
      case CHAR_CLASS:
      case CHAR_LITERAL:
      case CHAR_SWITCH:
      case STRING_LITERAL:
        runtime.error("match for terminal", m);
        break;

      case ACTION:
        runtime.error("match for action", m);
        break;

      case PARSER_ACTION:
        runtime.error("match for parser action", m);
        break;

      case NODE_MARKER:
        runtime.error("match for node marker", m);
        break;
      }
    }

    dispatch(m.element);
  }
  
  /** Analyze the specified nonterminal. */
  public void visit(NonTerminal nt) {
    Production p = null;

    try {
      p = analyzer.lookup(nt);
    } catch (IllegalArgumentException x) {
      if (nt.hasProperty(Constants.ORIGINAL)) {
        if (! badNTs.containsKey(nt)) {
          runtime.error("ambiguous renamed nonterminal '" + nt + "'", nt);
          badNTs.put(nt, nt);
        }
      } else {
        if (! badNTs.containsKey(nt)) {
          runtime.error("ambiguous nonterminal '" + nt + "'", nt);
          badNTs.put(nt, nt);
        }
      }
      return;
    }
    if (null == p) {
      if (nt.hasProperty(Constants.ORIGINAL)) {
        if (! badNTs.containsKey(nt)) {
          runtime.error("undefined renamed nonterminal '" + nt + "'", nt);
          badNTs.put(nt, nt);
        }
      } else {
        if (! badNTs.containsKey(nt)) {
          runtime.error("undefined nonterminal '" + nt + "'", nt);
          badNTs.put(nt, nt);
        }
      }
    }
  }

  /** Analyze the specified terminal. */
  public void visit(Terminal t) {
    // Nothing to do.
  }

  /** Analyze the specified string literal. */
  public void visit(StringLiteral l) {
    if (1 == phase) {
      if (0 == l.text.length()) {
        runtime.error("empty string literal", l);
      }
    }
  }

  /** Analyze the specified character class. */
  public void visit(CharClass c) {
    if (1 != phase) return;

    final int length = c.ranges.size();

    if (0 >= length) {
      runtime.error("empty character class", c);

    } else {
      ArrayList<CharRange> list = new ArrayList<CharRange>(c.ranges);
      Collections.sort(list);

      for (int i=0; i<length-1; i++) {
        CharRange r1 = list.get(i);
        CharRange r2 = list.get(i+1);
        
        if (r1.last >= r2.first) {
          boolean single1 = (r1.first == r1.last);
          boolean single2 = (r2.first == r2.last);

          if (single1) {
            if (single2) {
              runtime.error("duplicate character '" +
                            Utilities.escape(r1.last, Utilities.FULL_ESCAPES) +
                            "' in character class", c);
            } else {
              runtime.error("character '" +
                            Utilities.escape(r1.last, Utilities.FULL_ESCAPES) +
                            "' already contained in range " +
                            Utilities.escape(r2.first, Utilities.FULL_ESCAPES) +
                            "-" + Utilities.escape(r2.last,
                                                   Utilities.FULL_ESCAPES), c);
            }
          } else {
            if (single2) {
              runtime.error("character '" +
                            Utilities.escape(r2.first, Utilities.FULL_ESCAPES) +
                            "' already contained in range " +
                            Utilities.escape(r1.first, Utilities.FULL_ESCAPES) +
                            "-" + Utilities.escape(r1.last,
                                                   Utilities.FULL_ESCAPES), c);
            } else {
              runtime.error("ranges " +
                            Utilities.escape(r1.first, Utilities.FULL_ESCAPES) +
                            "-" + Utilities.escape(r1.last,
                                                   Utilities.FULL_ESCAPES) +
                            " and " +
                            Utilities.escape(r2.first, Utilities.FULL_ESCAPES) +
                            "-" + Utilities.escape(r2.last,
                                                   Utilities.FULL_ESCAPES) +
                            " overlap", c);
            }
          }
        }
      }
    }
  }

  /** Analyze the specified null literal. */
  public void visit(NullLiteral l) {
    // Nothing to do.
  }

  /** Analyze the specified node marker. */
  public void visit(NodeMarker m) {
    if (! ast.isGenericNode(analyzer.current().dType)) {
      runtime.error("node marker in non-generic production", m);
    } else if (isPredicate) {
      runtime.error("node marker in predicate", m);
    }
  }

  /** Analyze the specified action. */
  public void visit(Action a) {
    // Nothing to do.
  }

  /** Analyze the specified parser action. */
  public void visit(ParserAction pa) {
    if (1 == phase) {
      if (! (pa.element instanceof Action)) {
        runtime.error("malformed parser action", pa);
      }
      if (isPredicate) {
        runtime.error("parser action within syntactic predicate", pa);
      }
    }

    dispatch(pa.element);
  }

  /** Analyze the specified internal element. */
  public void visit(InternalElement e) {
    if (1 == phase) {
      runtime.error("internal element", (Element)e);
    }
  }

}
