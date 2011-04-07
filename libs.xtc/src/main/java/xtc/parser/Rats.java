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

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;

import java.util.ArrayList;

import xtc.Constants;

import xtc.util.Tool;
import xtc.util.Utilities;

import xtc.tree.Attribute;
import xtc.tree.Node;
import xtc.tree.Printer;

import xtc.type.AST;
import xtc.type.JavaAST;

/**
 * The command line interface to <i>Rats&#033;</i>, the packrat parser
 * generator for Java.
 *
 * @author Robert Grimm
 * @version $Revision: 1.189 $
 */
public class Rats extends Tool {

  /** Create a new instance of <i>Rats&#033;</i>. */
  public Rats() { /* Nothing to do. */ }

  public String getName() {
    return "Rats! Parser Generator";
  }

  public String getCopy() {
    return Constants.COPY;
  }

  public String getExplanation() {
    return
      "By default, Rats! performs all optimizations besides the " +
      "errors2 and left1 optimizations.  If one or more " +
      "individual optimizations are specified as command line flags, all " +
      "other optimizations are automatically disabled.  The choices2 " +
      "optimization includes choices1, errors1 is complimentary to errors2, " +
      "and left1 and left2 are mutually exclusive.";
  }

  public void init() {
    super.init();
    runtime.
      bool("loaded", "optionLoaded", false,
           "Print every module after loading, then stop.").
      bool("instantiated", "optionInstantiated", false,
           "Print all modules after loading and instantiating them, then stop.").
      bool("dependencies", "optionDependencies", false,
           "Print module dependencies after loading and instantiating, " +
           "then stop.").
      bool("applied", "optionApplied", false,
           "Print all modules after applying modifications, then stop.").
      bool("valued", "optionValued", false,
           "Print grammar after reducing it to expressions that directly " +
           "contribute to AST, then stop.").
      bool("processed", "optionProcessed", false,
           "Print full grammar before code generation, then stop.").
      bool("html", "optionHtml", false,
           "Create HTML for instantiated, applied, valued, or processed " +
           "options.").
      bool("variant", "optionVariant", false,
           "Enforce variant types for productions having node values.").
      bool("ast", "optionASTDefinition", false,
           "Print a formal definition of the grammar's AST, then stop.").
      bool("lgpl", "optionLGPL", false,
           "Create an LGPL compliant parser.").
      att("option", "grammarOption", true,
          "Add the specified attribute to the grammar's options.").
      bool("Onone", "doNotOptimize", false,
           "Perform no optimizations.").
      bool("Ochunks", "optimizeChunks", true,
           "Break memoization table into chunks.").
      bool("Ogrammar", "optimizeGrammar", true,
           "Fold duplicate productions and eliminate dead productions.").
      bool("Oterminals", "optimizeTerminals", true,
           "Optimize the recognition of terminals, incl. by using switches.").
      bool("Ocost", "optimizeCost", true,
           "Perform cost-based inlining.").
      bool("Otransient", "optimizeTransient", true,
           "Do not memoize transient productions.").
      bool("Onontransient", "optimizeNonTransient", true,
           "Mark suitable productions as transient.").
      bool("Orepeated", "optimizeRepeated", true,
           "Do not desugar transient repetitions.").
      bool("Oleft1", "optimizeLeftRecursions", false,
           "Convert direct left-recursions into equivalent right-recursions.").
      bool("Oleft2", "optimizeLeftIterations", true,
           "Convert direct left-recursions into equivalent iterations.").
      bool("Ooptional", "optimizeOptional", true,
           "Do not desugar options.").
      bool("Ochoices1", "optimizeChoices1", true,
           "Inline transient void or text-only productions into choices.").
      bool("Ochoices2", "optimizeChoices2", true,
           "Inline productions with the inline attribute into choices.").
      bool("Oerrors1", "optimizeErrors1", true,
           "Avoid creating parse errors for individual terms.").
      bool("Oerrors2", "optimizeErrors2", false,
           "Avoid creating parse errors for transient productions.").
      bool("Ovalues", "optimizeValues", true,
           "Avoid creating duplicate semantic values.").
      bool("Omatches", "optimizeMatches", true,
           "Optimize the performance of string matches.").
      bool("Oprefixes", "optimizePrefixes", true,
           "Fold common prefixes in choices.").
      bool("Ognodes", "optimizeGenericNodes", true,
           "Optimize the creation of generic nodes.").
      bool("Olocation", "optimizeLocation", true,
           "Optimize the annotation of nodes with their source locations.");
  }

  public void prepare() {
    boolean explicitOptimizations = runtime.hasPrefixValue("optimize");
    boolean doNotOptimize         =
      runtime.hasValue("doNotOptimize") &&
      runtime.test("doNotOptimize");

    // Check optimization options.
    if (explicitOptimizations && doNotOptimize) {
      runtime.error("no optimizations incompatible with explicitly specified " +
                    "optimizations");
    }
    
    if (runtime.hasValue("optimizeLeftRecursions") &&
        runtime.test("optimizeLeftRecursions") &&
        runtime.hasValue("optimizeLeftIterations") &&
        runtime.test("optimizeLeftIterations")) {
      runtime.error("left1 option mutually exclusive with left2 option");
    }

    // Now, fill in the defaults.
    if (explicitOptimizations || doNotOptimize) {
      runtime.initFlags("optimize", false);
    }
    runtime.initDefaultValues();

    // Perform consistency checking of other options.
    if (runtime.test("optionSilent") && runtime.test("optionVerbose")) {
      runtime.error("can't run in silent and verbose mode at the same time");
    }
    if (runtime.test("optionLoaded")) {
      if (runtime.test("optionInstantiated")) {
        runtime.error("loaded option incompatible with instantiated option");
      }
      if (runtime.test("optionApplied")) {
        runtime.error("loaded option incompatible with applied option");
      }
      if (runtime.test("optionValued")) {
        runtime.error("loaded option incompatiable with valued option");
      }
      if (runtime.test("optionProcessed")) {
        runtime.error("loaded option incompatible with processed option");
      }
      if (runtime.test("optionASTDefinition")) {
        runtime.error("loaded option incompatible with ast option");
      }
    }
    if (runtime.test("optionInstantiated")) {
      if (runtime.test("optionApplied")) {
        runtime.error("instantiated option incompatible with applied option");
      }
      if (runtime.test("optionValued")) {
        runtime.error("instantiated option incompatible with valued option");
      }
      if (runtime.test("optionProcessed")) {
        runtime.error("instantiated option incompatible with processed option");
      }
      if (runtime.test("optionASTDefinition")) {
        runtime.error("instantiated option incompatible with ast option");
      }
    }
    if (runtime.test("optionApplied")) {
      if (runtime.test("optionValued")) {
        runtime.error("applied option incompatible with valued option");
      }
      if (runtime.test("optionProcessed")) {
        runtime.error("applied option incompatible with processed option");
      }
      if (runtime.test("optionASTDefinition")) {
        runtime.error("applied option incompatible with ast option");
      }
    }
    if (runtime.test("optionValued")) {
      if (runtime.test("optionProcessed")) {
        runtime.error("valued option incompatible with processed option");
      }
      if (runtime.test("optionASTDefinition")) {
        runtime.error("valued option incompatible with ast option");
      }
    }
    if (runtime.test("optionProcessed") && runtime.test("optionASTDefinition")) {
      runtime.error("processed option incompatible with ast option");
    }
    if (runtime.test("optionHtml")) {
      if (runtime.test("optionLoaded")) {
        runtime.error("loaded option incompatible with html option");
      } else if ((! runtime.test("optionInstantiated")) &&
                 (! runtime.test("optionApplied")) &&
                 (! runtime.test("optionValued")) &&
                 (! runtime.test("optionProcessed"))) {
        runtime.error("html option requires instantiated, applied, valued or " +
                      "processed option");
      }
    }
  }

  public Node parse(Reader in, File file) throws IOException, ParseException {
    long    length = file.length();
    if (Integer.MAX_VALUE < length) {
      throw new IllegalArgumentException(file + ": file too large");
    }
    PParser parser = new PParser(in, file.toString(), (int)length);
    Result  result = parser.pModule(0);
    Module  mod    = (Module)parser.value(result);
    String  name   = file.getName();

    // Chop off extension.
    int    idx  = name.lastIndexOf('.');
    if (-1 != idx) name = name.substring(0, idx);
    
    // Make sure the unqualified module name and the unqualified
    // file name match.
    if (! name.equals(Utilities.unqualify(mod.name.name))) {
      runtime.error("module name '" + mod.name.name +
                    "' inconsistent with file name '" + file + "'", mod.name);
    }

    // Return the module.
    return mod;
  }

  public void process(Node node) {
    Module module = (Module)node;

    // --------------------------------------------------------------------
    //                  Analyze and transform module
    // --------------------------------------------------------------------
    
    // Prepare for the work.
    Analyzer                  ana    = new Analyzer();
    AST                       ast    = new JavaAST();
    Simplifier                simple = new Simplifier(runtime, ana);
    DeadProductionEliminator  dead   =
      new DeadProductionEliminator(runtime, ana);
    DuplicateProductionFolder dup    =
      new DuplicateProductionFolder(runtime, ana);
    PrefixFolder              prefix = new PrefixFolder(runtime, ana);
    MetaDataCreator           meta   = new MetaDataCreator();
    ReferenceCounter          ref    = new ReferenceCounter(runtime, ana);
    TransientMarker           trans  = new TransientMarker(runtime, ana);
    Inliner                   line   = new Inliner(runtime, ana);
    
    // Add options from the command line.
    if (null == module.attributes) {
      module.attributes = new ArrayList<Attribute>();
    }
    module.attributes.addAll(runtime.getAttributeList("grammarOption"));
    
    // Resolve all dependencies and check for well-formedness.  Note
    // that Resolver marks all text-only productions and recognizes
    // direct left-recursions.
    module = (Module)new Resolver(runtime, ana, ast).dispatch(module);
    if (runtime.test("optionLoaded") ||
        runtime.test("optionApplied") ||
        (null == module)) {
      return;
    }

    // If the grammar has the genericAsVoid attribute, void out
    // generic productions.
    if (module.hasAttribute(Constants.ATT_GENERIC_AS_VOID)) {
      new GenericVoider(runtime, ana).dispatch(module);
    }

    // Start simplifying the grammar: Find the real root, simplify
    // expressions, void out repetitions, options, and nested choices
    // without a value, and remove dead productions.
    new RootFinder(runtime, ana).dispatch(module);
    simple.dispatch(module);
    if (runtime.test("optimizeGrammar")) dead.dispatch(module);
    new ElementVoider(runtime, ana).dispatch(module);

    // Determine a grammar's variants.
    if (runtime.test("optionVariant")) {
      new VariantSorter(runtime, ana, ast).dispatch(module);
      if (0 < runtime.errorCount()) return;
    }

    // Further simplify the grammar: Fold duplicate productions, fold
    // common prefixes, inline productions, and mark productions as
    // transient.
    //
    // Note that we need to fold duplicates before marking productions
    // as transient, because a folded duplicate may be referenced more
    // than once and thus should not be marked as transient, even
    // though the non-folded productions could be marked as transient.
    if (runtime.test("optimizeGrammar") &&
        ! runtime.test("optionASTDefinition")) {
      dup.dispatch(module);
    }
    if (runtime.test("optimizePrefixes")) prefix.dispatch(module);
    boolean changed = false;
    do {
      changed = ((Boolean)line.dispatch(module)).booleanValue();
      if (changed) {
        simple.dispatch(module);
        if (runtime.test("optimizeGrammar")) dead.dispatch(module);
        if (runtime.test("optimizePrefixes")) prefix.dispatch(module);
      }
      if (runtime.test("optimizeNonTransient")) {
        meta.dispatch(module);
        ref.dispatch(module);
        trans.dispatch(module);
      }
    } while (changed);

    // If requested, annotate the grammar to preserve all formatting.
    if (module.hasAttribute(Constants.ATT_PARSE_TREE)) {
      new Tokenizer(runtime, ana).dispatch(module);
      new Annotator(runtime, ana).dispatch(module);
    }

    // Determine each production's semantic value: make the values
    // explicit; lift nested choices, repetitions, and options;
    // transform repetitions, options, and direct left recursions;
    // and, finally, check that every alternative has a semantic
    // value.
    new Transformer(runtime, ana, ast).dispatch(module);
    if (! runtime.test("optionValued")) {
      new ListMaker(runtime, ana, ast).dispatch(module);
      new DirectLeftRecurser(runtime, ana, ast).dispatch(module);
      new Generifier(runtime, ana).dispatch(module);
      new ValueChecker(runtime, ana).dispatch(module);
    }

    // If there were errors, we are done.
    if (0 < runtime.errorCount()) return;

    // If requested, print the reduced grammar and then stop.
    if (runtime.test("optionValued")) {
      new TreeExtractor(runtime, ana, ast, false).dispatch(module);
      if (runtime.test("optionHtml")) {
        new HtmlPrinter(runtime, ana, ast, true).dispatch(module);
      } else {
        new PrettyPrinter(runtime.console(), ast, true).dispatch(module);
        runtime.console().flush();
      }
      return;
    }

    // Optimize the grammar.
    if (runtime.test("optimizeChoices1") || runtime.test("optimizeChoices2")) {
      meta.dispatch(module);
      ref.dispatch(module);
      new ChoiceExpander(runtime, ana).dispatch(module);
    }
    if (runtime.test("optimizeTerminals")) {
      new ProductionVoider(runtime, ana).dispatch(module);
      new TerminalOptimizer(runtime, ana).dispatch(module);
    }
    if (runtime.test("optimizePrefixes")) {
      // Perform prefix folding again, as the expanding of choices may
      // lead to new common prefixes.
      prefix.dispatch(module);
    }
    if (runtime.test("optimizeGrammar")) {
      // Do duplicate production folding again, as the desugaring of
      // options and repetitions can lead to new duplicates.  Do
      // dead production elimination again, as the expanding of
      // choices can lead to new deaths.
      dead.dispatch(module);
      if (! runtime.test("optionASTDefinition")) dup.dispatch(module);
    }
    if (runtime.test("optimizePrefixes")) {
      // Check for unreachable alternatives (again, since this was
      // already done inside Resolver).  We do this after dead
      // production elemination to avoid duplicate error messages.
      new ReachabilityChecker(runtime, ana).dispatch(module);
      if (0 < runtime.errorCount()) return;
    }

    meta.dispatch(module);
    ref.dispatch(module);
    if (runtime.test("optimizeNonTransient")) {
      trans.dispatch(module);
    }
    new MetaDataSetter(runtime, ana, ast).dispatch(module);
    if (0 < runtime.errorCount()) return;

    // --------------------------------------------------------------------
    //              Print AST definition and processed grammar
    // --------------------------------------------------------------------

    if (runtime.test("optionASTDefinition")) {
      new TreeTyper(runtime, ana, ast).dispatch(module);
      return;

    } else if (runtime.test("optionProcessed")) {
      if (runtime.test("optionHtml")) {
        new HtmlPrinter(runtime, ana, ast, true).dispatch(module);
      } else {
        new PrettyPrinter(runtime.console(), ast, true).dispatch(module);
        runtime.console().flush();
      }
      return;
    }
    
    // --------------------------------------------------------------------
    //                        Generate parser
    // --------------------------------------------------------------------
    
    File file = new File(runtime.getOutputDirectory(),
                         Utilities.getName(module.getClassName()) + ".java");
    Printer out;
    try {
      out = new Printer(new PrintWriter(runtime.getWriter(file)));
    } catch (IOException x) {
      if (null == x.getMessage()) {
        runtime.error(file.toString() + ": I/O error");
      } else {
        runtime.error(file.toString() + ": " + x.getMessage());
      }
      return;
    }
    printHeader(out);
    new CodeGenerator(runtime, ana, ast, out).dispatch(module);
    out.flush().close();
  }

  /**
   * Run the packrat parser generator with the specified arguments.
   * Invoking <i>Rats!</i> without arguments will print information
   * about its usage.
   *
   * @param args The command line arguments.
   */
  public static void main(String[] args) {
    new Rats().run(args);
  }

}
