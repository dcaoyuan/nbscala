/*
 * xtc - The eXTensible Compiler
 * Copyright (C) 2005-2007 Robert Grimm
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import xtc.Constants;

import xtc.util.Runtime;
import xtc.util.Utilities;

import xtc.tree.Attribute;
import xtc.tree.Comment;
import xtc.tree.Node;
import xtc.tree.Printer;
import xtc.tree.VisitingException;

import xtc.type.AST;

/**
 * A grammar pretty printer producing HTML.
 *
 * @author Robert Grimm
 * @version $Revision: 1.28 $
 */
public class HtmlPrinter extends PrettyPrinter {

  /** The runtime. */
  protected final Runtime runtime;

  /** The analyzer utility. */
  protected final Analyzer analyzer;

  /** The flag for whether we are processing a grammar or single module. */
  protected boolean isGrammar;

  /** The number of the current production. */
  protected int pNumber = -1;

  /**
   * Create a new HTML printer.
   *
   * @param runtime The runtime.
   * @param analyzer The analyzer.
   * @param ast The type operations.
   * @param verbose The verbose flag.
   */
  public HtmlPrinter(Runtime runtime,Analyzer analyzer,AST ast,boolean verbose) {
    super(ast, verbose);
    this.runtime  = runtime;
    this.analyzer = analyzer;
  }

  protected int stringEscapes() {
    return Utilities.JAVA_HTML_ESCAPES;
  }

  protected int regexEscapes() {
    return Utilities.FULL_HTML_ESCAPES;
  }

  /**
   * Create a printer for the specified file.  This method initializes
   * this class' {@link #printer printer} to a printer writing to the
   * file with the specified name in the runtime's output directory.
   *
   * @param name The file name.
   */
  protected void open(String name) throws IOException {
    File file = new File(runtime.getOutputDirectory(), name);
    printer   = new Printer(new PrintWriter(runtime.getWriter(file)));
  }

  protected void printDocumentation(Module m) {
    Comment c = m.documentation;

    // Make sure we have something to print.
    if (! verbose || null == c || c.text.isEmpty()) {
      return;
    }

    // Start the containing div.
    printer.indent().pln("<div class=\"module-documentation\">");

    List<String> authors = null;
    String       version = null;
    for (String s : c.text) {
      if (s.startsWith("@")) {
        // Process @ tag.
        if (s.startsWith("@author ")) {
          if (null == authors) {
            authors = new ArrayList<String>();
          }
          authors.add(s.substring(8));

        } else if (s.startsWith("@version ")) {
          version = s.substring(9);
        }

      } else {
        printer.indent().pln(s);
      }
    }

    // Do we have any @ tags?
    if ((null != authors) || (null != version)) {
      printer.indent().pln("<dl>");

      if (null != authors) {
        if (1 == authors.size()) {
          printer.indent().pln("<dt>Author:</dt>").incr();
        } else {
          printer.indent().pln("<dt>Authors:</dt>").incr();
        }
        printer.indent().p("<dd>");
        Iterator<String> iter = authors.iterator();
        while (iter.hasNext()) {
          printer.p(iter.next());
          if (iter.hasNext()) {
            printer.p(", ");
          }
        }
        printer.pln("</dd>").decr();
      }

      if (null != version) {
        printer.indent().pln("<dt>Version:</dt>").incr();
        printer.indent().p("<dd>").p(version).pln("</dd>").decr();
      }

      printer.indent().pln("</dl>");
    }

    // Close the div.
    printer.indent().pln("</div>");
  }

  protected void printOption(Module m) {
    if ((null != m.attributes) && (0 < m.attributes.size())) {
      printer.pln().indent().p("option ");

      boolean             isFirst = true;
      Iterator<Attribute> iter    = m.attributes.iterator();
      while (iter.hasNext()) {
        Attribute att       = iter.next();
        String    name      = att.getName();
        String    attText   = att.toString();
        boolean   highlight = (analyzer.isTopLevel(m) ||
                               Constants.ATT_STATEFUL.getName().equals(name) ||
                               Constants.NAME_STRING_SET.equals(name) ||
                               Constants.NAME_FLAG.equals(name));

        if (isFirst) {
          isFirst = false;
        } else if (printer.column()+attText.length()+1>Constants.LINE_LENGTH) {
          printer.pln().indentMore();
        }

        if (highlight) {
          int column = printer.column();
          printer.p("<span class=\"highlight\">").column(column);
        }
        printer.p(attText);
        if (highlight) {
          int column = printer.column();
          printer.p("</span>").column(column);
        }

        if (iter.hasNext()) {
          printer.p(", ");
        } else {
          printer.p(';');
        }
      }
      printer.pln();
    }
  }

  /**
   * Actually print the specified module.
   *
   * @param m The module to print.
   */
  protected void print(Module m) {
    // Set up the HTML.
    printer.indent().pln("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\"");
    printer.indent().p("                      ").
      pln("\"http://www.w3.org/TR/html4/strict.dtd\">");
    printer.indent().pln("<html>");
    printer.indent().pln("<head>");
    printer.indent().p("<!-- Generated by Rats!, version ").p(Constants.VERSION).
      p(", ").p(Constants.COPY).pln(" -->");
    printer.indent().p("<title>Module ").p(m.name).pln("</title>");
    printer.indent().p("<link rel=\"stylesheet\" href=\"grammar.css\" ").
      pln("type=\"text/css\">");
    printer.indent().pln("</head>");
    printer.indent().pln("<body>");

    // Emit module documentation.
    printDocumentation(m);

    // Emit module name and parameters.
    printer.indent().pln("<pre class=\"module-header\">");
    printModule(m);

    // Emit module dependencies.
    if ((null != m.dependencies) && (0 < m.dependencies.size())) {
      printer.pln();
      for (ModuleDependency dep : m.dependencies) {
        printer.p(dep);
      }
    }

    // Emit header, body, and footer actions.
    printActions(m);

    // Emit grammar-wide options.
    printOption(m);
    printer.indent().pln("</pre>");

    // Emit the productions.
    final int length = m.productions.size();
    for (int i=0; i<length; i++) {
      pNumber = i;
      printer.p(m.productions.get(i));
    }

    // Finish the HTML.
    printer.indent().pln("</body>");
  }

  public void visit(Grammar g) {
    // Initialize the per-grammar state.
    analyzer.register(this);
    analyzer.init(g);
    isGrammar = true;

    // Iterate over the grammar's modules.
    for (Module m : g.modules) {
      try {
        open(m.name + ".html");
      } catch (IOException x) {
        throw new VisitingException("Unable to access " + m.name + ".html",
                                    x);
      }
      printer.register(this);
      analyzer.process(m);
      print(m);
      printer.flush();
    }
  }

  public void visit(Module m) {
    // Initialize the per-module state.
    analyzer.register(this);
    analyzer.init(m);
    isGrammar = false;

    try {
      open(m.name + ".html");
    } catch (IOException x) {
      throw new VisitingException("Unable to access " + m.name + ".html",
                                  x);
    }
    printer.register(this);

    // Print the module.
    print(m);

    // Flush the printer.
    printer.flush();
  }

  /**
   * Print a linked version of the specified module name.
   *
   * @param name The module name.
   * @param resolved The flag for whether the name can be resolved.
   */
  protected void print(ModuleName name, boolean resolved) {
    if (resolved) {
      int column = printer.column();
      printer.p("<a href=\"").p(name.name).p(".html\">").column(column).
        p(name.name);
    } else {
      int column = printer.column();
      printer.p("<a class=\"erroneous\" href=\"#\" title=\"Undefined module\">").
        column(column).p(name.name);
    }
    int column = printer.column();
    printer.p("</a>").column(column);
  }

  protected void print(ModuleDependency dep, String name) {
    Module m = analyzer.lookup(dep.visibleName());
    printer.indent().p(name).p(' ');
    if ((null == dep.target) &&
        ((null != m) || (! "instantiate".equals(name)))) {
      print(dep.module, null != m);
    } else {
      printer.p(dep.module);
    }
    if (0 != dep.arguments.size()) {
      printer.p(dep.arguments);
    }
    if ((null != dep.target) &&
        ((null != m) || (! "instantiate".equals(name)))) {
      print(dep.target, null != m);
    } else {
      printer.p(dep.target);
    }
    printer.pln(';');
  }

  protected void enter(Production p) {
    // Print a comment for productions folded from duplicates.
    if (verbose && p.hasProperty(Properties.DUPLICATES)) {
      List<String> sources = Properties.getDuplicates(p);
      printer.indent().pln("<div class=\"production-documentation\">");
      printer.indent().p("The following production is the result of ").
        p("folding duplicates ");
      Iterator<String> iter = sources.iterator();
      while (iter.hasNext()) {
        String name = iter.next();

        printer.buffer();
        if ((1 < sources.size()) && (! iter.hasNext())) {
          printer.p("and ");
        }
        printer.p(name);
        if ((2 == sources.size()) && (iter.hasNext())) {
          printer.p(' ');
        } else if (iter.hasNext()) {
          printer.p(", ");
        } else {
          printer.p('.');
        }
        printer.fit();
      }
      printer.pln();
      printer.indent().pln("</div>");
    }

    // Get ready to print the attributes, type, and name.
    printer.indent().pln("<pre class=\"production-body\">");
    printer.indent();

    // The production's anchor is the nonterminal's name for full
    // productions and the name, followed by a minus sign, followed by
    // the production's number (in the module's list of productions)
    // for partial productions.
    if (p.isFull()) {
      printer.p("<a name=\"").p(p.name.name).p("\"></a>");
    } else {
      printer.p("<a name=\"").p(p.name.name).p('-').p(pNumber).p("\"></a>");
    }

    // Print the attributes and type as is.
    if ((null != p.attributes) && (0 < p.attributes.size())) {
      // Declare attributes as nodes so that overload resolution
      // invokes Printer.p(Node) and therefore visit(Attribute).
      for (Node att : p.attributes) {
        printer.p(att).p(' ');
      }
    }

    if (null != p.type) {
      if (AST.isVoid(p.type)) {
        printer.p("void ");
      } else if (AST.isGenericNode(p.type)) {
        printer.p("generic ");
      } else {
        printer.p(ast.extern(p.type)).p(' ');
      }
    } else if (null != p.dType) {
      printer.p(p.dType).p(' ');
    }

    // If the production is partial, we currently link to the modified
    // production.
    if (p.isPartial()) {
      boolean    duplicate = false;
      Production base      = null;
      try {
        base =
          analyzer.lookup(p.name.qualify(analyzer.currentModule().name.name));
      } catch (IllegalArgumentException x) {
        duplicate = true;
      }
      if (null == base) {
        printer.p("<a class=\"erroneous\" href=\"#\" title=\"");
        if (duplicate) {
          printer.p("Ambiguous nonterminal");
        } else {
          printer.p("Undefined nonterminal");
        }
      } else {
        Module m = analyzer.currentModule();
        String q = base.qName.getQualifier();
        if (q.equals(m.name.name)) {
          printer.p("<a href=\"#").p(p.name.name);
        } else {
          printer.p("<a href=\"").p(q).p(".html#").p(p.name.name);
        }
      }
      printer.p("\">").p(p.name.name).p("</a>");

    } else {
      printer.p(p.name.name);
    }

    // Set the internal state.
    parenChoice   = false;
    parenSequence = false;
  }

  protected void exit(Production p) {
    printer.indent().pln("</pre>");
  }

  public void visit(SequenceName n) {
    printer.p("&lt;").p(n.name).p("&gt;");
    printer.column(printer.column() - 6);
  }

  public void visit(NonTerminal nt) {
    if (newline) printer.indent();
    newline = false;
    printer.buffer();

    // Look up the corresponding production.
    int        column    = printer.column();
    boolean    duplicate = false;
    Production p         = null;
    try {
      p = analyzer.lookup(nt);
    } catch (IllegalArgumentException x) {
      duplicate          = true;
    }

    // Print the corresponding link.
    if (null == p) {
      printer.p("<a class=\"erroneous\" href=\"#\" title=\"");
      if (duplicate) {
        printer.p("Ambiguous nonterminal");
      } else {
        printer.p("Undefined nonterminal");
      }
    } else {
      if (isGrammar) {
        // We need to distinguish between links within the same module
        // and links across modules.
        Module m = analyzer.currentModule();
        String q = p.qName.getQualifier();
        if (q.equals(m.name.name)) {
          printer.p("<a href=\"#").p(p.name.name);
        } else {
          printer.p("<a href=\"").p(q).p(".html#").p(p.name.name);
        }
      } else {
        printer.p("<a href=\"#").p(nt.name);
      }
    }
    printer.p("\">").column(column).p(nt.name);
    column = printer.column();
    printer.p("</a>").column(column).fit();
  }

  protected void print(String s) {
    int column = printer.column();
    int length = s.length();
    for (int i=0; i<length; i++) {
      char c = s.charAt(i);
      if ('<' == c) {
        printer.p("&lt;");
      } else if ('>' == c) {
        printer.p("&gt;");
      } else {
        printer.p(c);
      }
    }
    printer.column(column + length);
  }

  public void visit(StringValue v) {
    if (null == v.text) {
      if (newline) printer.indent();
      newline = false;
      printer.buffer().p("/* value = &lt;text&gt;; */").
        column(printer.column()-6).fit();
    } else {
      format(false, v.text);
    }
  }

  public void visit(TokenValue v) {
    if (null == v.text) {
      if (newline) printer.indent();
      newline = false;
      printer.buffer().p("/* value = Token(&lt;text&gt;); */").
        column(printer.column()-6).fit();
    } else {
      format(true, v.text);
    }
  }

}
