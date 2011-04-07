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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import xtc.Constants;

import xtc.util.Utilities;

import xtc.tree.Attribute;
import xtc.tree.Comment;
import xtc.tree.Node;
import xtc.tree.Printer;
import xtc.tree.Visitor;

import xtc.type.AST;

/**
 * The grammar pretty printer.
 *
 * @author Robert Grimm
 * @version $Revision: 1.117 $
 */
public class PrettyPrinter extends Visitor {

  /** The printer for this pretty printer. */
  protected Printer printer;

  /** The type operations. */
  final protected AST ast;

  /** The flag for whether this pretty printer is verbose. */
  final protected boolean verbose;

  /** Flag for whether the last element ended in a newline. */
  protected boolean newline;

  /** Flag for whether the next ordered choice needs to be parenthesized. */
  protected boolean parenChoice;

  /** Flag for whether the next sequence element needs to be parenthesized. */
  protected boolean parenSequence;

  /** Flag for whether the choice is top-level. */
  protected boolean isTopLevel;

  /**
   * Create a new pretty printer.  Any subclass using this constructor
   * must explicit set the {@link #printer} field and register this
   * visitor with the set printer.
   *
   * @param ast The type operations.
   * @param verbose The verbose flag.
   */
  protected PrettyPrinter(AST ast, boolean verbose) {
    this.ast      = ast;
    this.verbose  = verbose;
  }

  /**
   * Create a new pretty printer.
   *
   * @param printer The printer.
   * @param ast The type operations.
   * @param verbose The verbose flag.
   */
  public PrettyPrinter(Printer printer, AST ast, boolean verbose) {
    this.printer  = printer;
    this.ast      = ast;
    this.verbose  = verbose;
    printer.register(this);
  }

  /**
   * Get the flags for escaping regular strings.
   *
   * @return The flags for escaping strings.
   */
  protected int stringEscapes() {
    return Utilities.JAVA_ESCAPES;
  }

  /**
   * Get the flags for escaping regex strings.
   *
   * @return The flags for escaping regex strings.
   */
  protected int regexEscapes() {
    return Utilities.FULL_ESCAPES;
  }

  /**
   * Determine whether the specified element represents a prefix
   * operator.
   *
   * @param e The element.
   * @return <code>true</code> if the specified element represents a
   *   prefix operator.
   */
  protected boolean isPrefix(Element e) {
    switch (e.tag()) {
    case VOIDED:
    case BINDING:
    case PARSER_ACTION:
    case FOLLOWED_BY:
    case NOT_FOLLOWED_BY:
    case SEMANTIC_PREDICATE:
    case STRING_MATCH:
      return true;
    default:
      return false;
    }
  }

  /** Flush the underlying printer. */
  public void flush() {
    printer.flush();
  }

  /** Print the specified attribute. */
  public void visit(Attribute a) {
    if (Constants.NAME_VISIBILITY.equals(a.getName())) {
      printer.p((String)a.getValue());
    } else {
      printer.p(a.getName());
      Object value = a.getValue();
      if (null != value) {
        printer.p('(').p(value.toString()).p(')');
      }
    }
  }

  /** Print the specified grammar. */
  public void visit(Grammar g) {
    for (Module m : g.modules) dispatch(m);
  }

  /**
   * Print the specified module documentation comment.
   *
   * @param m The module.
   */
  protected void printDocumentation(Module m) {
    if ((! verbose) || (null == m.documentation)) return;

    Comment c    = m.documentation;
    int     size = c.text.size();

    if (0 == size) {
      // Nothing to print.

    } else if (1 == size) {
      printer.indent().p("/** ").p(c.text.get(0)).pln(" */");

    } else {
      printer.indent().pln("/**");
      for (int i=0; i<size; i++) {
        printer.indent().p(" * ").pln(c.text.get(i));
      }
      printer.indent().pln(" */");
    }
  }

  /**
   * Print the specified module's declaration.
   *
   * @param m The module.
   */
  protected void printModule(Module m) {
    printer.indent().p("module " ).p(m.name);
    if ((null != m.parameters) && (0 < m.parameters.size())) {
      printer.p(m.parameters);
    }
    if (verbose &&
        (m.name.hasProperty(Constants.ORIGINAL) ||
         m.hasProperty(Constants.ARGUMENTS))) {
      ModuleName base =
        m.name.hasProperty(Constants.ORIGINAL) ?
        (ModuleName)m.name.getProperty(Constants.ORIGINAL) :
        m.name;
      ModuleList args = (ModuleList)m.getProperty(Constants.ARGUMENTS);
      printer.p(" /* = ").p(base);
      if (null == args) {
        printer.p("()");
      } else {
        printer.p(args);
      }
      printer.p(" */ ");
    }
    printer.pln(';');
  }

  /**
   * Print the specified module's header, body, and footer actions.
   *
   * @param m The module.
   */
  protected void printActions(Module m) {
    // Emit header action.
    if (null != m.header) {
      printer.pln().indent().p("header ").p(m.header);
      if (1 == m.header.code.size()) {
        printer.pln();
      }
    }

    // Emit body action.
    if (null != m.body) {
      printer.pln().indent().p("body ").p(m.body);
      if (1 == m.body.code.size()) {
        printer.pln();
      }
    }

    // Emit footer action.
    if (null != m.footer) {
      printer.pln().indent().p("footer ").p(m.footer);
      if (1 == m.footer.code.size()) {
        printer.pln();
      }
    }
  }

  /**
   * Print the specified module's options.
   *
   * @param m The module.
   */
  protected void printOption(Module m) {
    if ((null != m.attributes) && (0 < m.attributes.size())) {
      printer.pln().indent().p("option ");
      for (Iterator<?> iter = m.attributes.iterator(); iter.hasNext(); ) {
        printer.buffer().p(iter.next().toString());
        if (iter.hasNext()) {
          printer.p(", ");
        } else {
          printer.p(';');
        }
        printer.fitMore();
      }
      printer.pln();
    }
  }

  /** Print the specified module. */
  public void visit(Module m) {
    // Emit header.
    printer.sep();
    printer.indent().p("// Generated by Rats!, version ").p(Constants.VERSION).
      p(", ").p(Constants.COPY).pln('.');
    printer.sep();
    printer.pln();

    // Emit module documentation.
    printDocumentation(m);

    // Emit module name and parameters.
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

    // Emit the productions.
    printer.pln();
    for (Production p : m.productions) {
      printer.p(p);
    }
    printer.sep().pln();
  }

  /**
   * Print the specified module dependency.
   *
   * @param dep The dependency.
   * @param name The corresponding name.
   */
  protected void print(ModuleDependency dep, String name) {
    printer.indent().p(name).p(' ').p(dep.module);
    if (0 != dep.arguments.size()) {
      printer.p(dep.arguments);
    }
    if (null != dep.target) {
      printer.buffer().p(" as ").p(dep.target).fitMore();
    }
    printer.pln(';');
  }

  /** Print the specified module import. */
  public void visit(ModuleImport imp) {
    print(imp, "import");
  }

  /** Print the specified module instantiation. */
  public void visit(ModuleInstantiation ins) {
    print(ins, "instantiate");
  }

  /** Print the specified module modification. */
  public void visit(ModuleModification mod) {
    print(mod, "modify");
  }

  /** Print the specified module list. */
  public void visit(ModuleList list) {
    printer.p('(');
    for (Iterator<ModuleName> iter = list.names.iterator(); iter.hasNext(); ) {
      printer.buffer().p(iter.next());
      if (iter.hasNext()) {
        printer.p(", ");
      }
      printer.fitMore();
    }
    printer.p(')');
  }

  /** Print the specified module name. */
  public void visit(ModuleName name) {
    printer.p(name.name);
  }

  /**
   * Enter the specified production.  This method prints a comment for
   * productions folded from duplicates.  It then prints the specified
   * production's attributes, type, and name.  Finally, it sets {@link
   * #parenChoice} and {@link #parenSequence} to <code>false</code>.
   *
   * @param p The production.
   */
  protected void enter(Production p) {
    // Print a comment for productions folded from duplicates.
    if (verbose && p.hasProperty(Properties.DUPLICATES)) {
      List<String> sources = Properties.getDuplicates(p);
      printer.indent().pln("/*");
      printer.indent().p(" * The following production is the result of ").
        p("folding duplicates ");
      for (Iterator<String> iter = sources.iterator(); iter.hasNext(); ) {
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
        printer.fit(" * ");
      }
      printer.pln();
      printer.indent().pln(" */");
    }

    // Print the attributes, type, and name.
    printer.indent();
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

    printer.p(p.name.name);

    // Set up the internal state.
    parenChoice   = false;
    parenSequence = false;
  }

  /**
   * Exit the specified production.  The default implementation prints
   * a new line.
   *
   * @param p The production.
   */
  protected void exit(Production p) {
    printer.pln();
  }

  /** Print the specified alternative addition. */
  public void visit(AlternativeAddition p) {
    enter(p);
    printer.p(" += ");
    if (! p.isBefore) {
      printer.pln().indentMore().p(p.sequence).pln(" ...");
      printer.indentMore().p("/ ");
    }
    isTopLevel = true;
    printer.p(p.choice);
    if (p.isBefore) {
      printer.indentMore().p("/ ").p(p.sequence).pln(" ...");
    }
    printer.indentMore().pln(';');
    exit(p);
  }

  /** Print the specified alternative removal. */
  public void visit(AlternativeRemoval p) {
    enter(p);
    printer.pln(" -=").incr().indent();
    for (Iterator<SequenceName> iter = p.sequences.iterator(); iter.hasNext();) {
      printer.buffer().p(iter.next());
      if (iter.hasNext()) {
        printer.p(", ");
      }
      printer.fit();
    }
    printer.pln().indent().pln(';').decr();
    exit(p);
  }

  /** Print the specified production override. */
  public void visit(ProductionOverride p) {
    enter(p);
    printer.p(" := ");

    if (null == p.choice) {
      printer.pln("... ;");

    } else if (p.isComplete) {
      isTopLevel = true;
      printer.p(p.choice).indentMore().pln(';');

    } else {
      printer.pln().indentMore().pln("...").indentMore().p("/ ");
      isTopLevel = true;
      printer.p(p.choice).indentMore().pln(';');
    }
    exit(p);
  }

  /** Print the specified full production. */
  public void visit(FullProduction p) {
    enter(p);

    printer.p(" = ");
    if ((1 == p.choice.alternatives.size()) &&
        (0 == p.choice.alternatives.get(0).size())) {
      if (p.getBooleanProperty(Properties.REDACTED)) {
        printer.p("... ");
      } else if (verbose) {
        printer.p("/* Empty */ ");
      }
      printer.pln(';');
    } else {
      isTopLevel = true;
      printer.p(p.choice).indentMore().pln(';');
    }

    exit(p);
  }

  /**
   * Print the specified alternatives.
   *
   * @param alternatives The ordered list of alternatives.
   * @param mark The mark.
   */
  protected void print(List<Sequence> alternatives, String mark) {
    boolean choice   = parenChoice;
    boolean sequence = parenSequence;

    if (choice) {
      printer.p("( ");
    }
    if (isTopLevel) {
      isTopLevel = false;
    } else if (verbose) {
      printer.p("/* ").p(mark).p(" */ ");
    }
    printer.pln().incr();

    boolean first = true;
    for (Sequence s : alternatives) {
      if (first) {
        first = false;
        printer.indent();

      } else {
        printer.indent().p("/ ");
      }

      parenChoice   = true;
      parenSequence = false;
      newline       = false;
      printer.p(s);

      if (! newline) {
        printer.pln();
      }
    }

    printer.decr();
    if (choice) {
      printer.indent().p(')');
    }

    parenChoice   = choice;
    parenSequence = sequence;
    newline       = false;
  }

  /** Print the specified ordered choice. */
  public void visit(OrderedChoice c) {
    print(c.alternatives, "Choice");
  }

  /** Print the specified repetition. */
  public void visit(Repetition r) {
    if (newline) printer.indent();
    boolean choice   = parenChoice;
    boolean sequence = parenSequence;

    newline          = false;
    parenChoice      = true;
    parenSequence    = true;

    printer.buffer();
    if (isPrefix(r.element)) printer.p('(');
    printer.p(r.element);
    if (isPrefix(r.element)) printer.p(')');
    if (r.once) {
      printer.p('+');
    } else {
      printer.p('*');
    }
    printer.fit();
    
    parenChoice      = choice;
    parenSequence    = sequence;
  }

  /** Print the specified option. */
  public void visit(Option o) {
    if (newline) printer.indent();
    boolean choice   = parenChoice;
    boolean sequence = parenSequence;

    newline          = false;
    parenChoice      = true;
    parenSequence    = true;

    printer.buffer();
    if (isPrefix(o.element)) printer.p('(');
    printer.p(o.element);
    if (isPrefix(o.element)) printer.p(')');
    printer.p('?').fit();

    parenChoice      = choice;
    parenSequence    = sequence;
  }

  /** Print the specified sequence name. */
  public void visit(SequenceName n) {
    printer.p('<').p(n.name).p('>');
  }

  /** Print the specified sequence. */
  public void visit(Sequence s) {
    if (newline) printer.indent();
    boolean choice   = parenChoice;
    boolean sequence = parenSequence;

    newline          = false;
    parenChoice      = true;
    parenSequence    = true;

    boolean  first = true;
    for (Element e : s.elements) {
      if (first) {
        first = false;
        if (sequence) {
          printer.p('(');
        }
        if (null != s.name) {
          printer.p(s.name).p(' ');
        }
      } else {
        printer.p(' ');
      }
      printer.p(e);
    }
    if (sequence) {
      printer.p(')');
    }

    parenChoice      = choice;
    parenSequence    = sequence;
  }

  /** Print the specified followed-by element. */
  public void visit(FollowedBy p) {
    if (newline) printer.indent();
    boolean choice   = parenChoice;
    boolean sequence = parenSequence;

    newline          = false;
    parenChoice      = true;
    parenSequence    = true;
    
    printer.buffer().p('&').p(p.element).fit();

    parenChoice      = choice;
    parenSequence    = sequence;
  }

  /** Print the specified not-followed-by element. */
  public void visit(NotFollowedBy p) {
    if (newline) printer.indent();
    boolean choice   = parenChoice;
    boolean sequence = parenSequence;

    newline          = false;
    parenChoice      = true;
    parenSequence    = true;
    
    printer.buffer().p('!').p(p.element).fit();

    parenChoice      = choice;
    parenSequence    = sequence;
  }

  /** Print the specified semantic predicate. */
  public void visit(SemanticPredicate p) {
    if (newline) printer.indent();
    boolean choice   = parenChoice;
    boolean sequence = parenSequence;

    newline          = false;
    parenChoice      = true;
    parenSequence    = true;
    
    printer.buffer().p('&').p(p.element).fit();

    parenChoice      = choice;
    parenSequence    = sequence;
  }

  /** Print the specified voided element. */
  public void visit(VoidedElement v) {
    if (newline) printer.indent();
    newline = false;
    printer.buffer().p("void:").p(v.element).fit();
  }

  /** Print the specified binding. */
  public void visit(Binding b) {
    if (newline) printer.indent();
    newline = false;
    printer.buffer().p(b.name).p(':').p(b.element).fit();
  }

  /** Print the specified string match. */
  public void visit(StringMatch m) {
    if (newline) printer.indent();
    newline = false;
    printer.buffer().p('\"').escape(m.text, stringEscapes()).p("\":").
      p(m.element).fit();
  }

  /** Print the specified nonterminal. */
  public void visit(NonTerminal nt) {
    if (newline) printer.indent();
    newline = false;
    printer.buffer().p(nt.name).fit();
  }

  /** Print the specified string literal. */
  public void visit(StringLiteral l) {
    if (newline) printer.indent();
    newline = false;
    printer.buffer().p('\"').escape(l.text, stringEscapes()).p('\"').
      fit();
  }

  /** Print the specified any character element. */
  public void visit(AnyChar a) {
    if (newline) printer.indent();
    newline = false;
    printer.buffer().p('_').fit();
  }

  /** Print the specified character literal. */
  public void visit(CharLiteral l) {
    if (newline) printer.indent();
    newline = false;
    printer.buffer().p('\'').escape(l.c, stringEscapes()).p('\'').fit();
  }

  /** Print the specified character range. */
  public void visit(CharRange r) {
    if (newline) printer.indent();
    newline  = false;
    if (r.first == r.last) {
      printer.escape(r.first, regexEscapes());
    } else {
      printer.escape(r.first, regexEscapes()).p('-').
        escape(r.last, regexEscapes());
    }
  }

  /** Print the specified character class. */
  public void visit(CharClass c) {
    if (newline) printer.indent();
    newline = false;
    printer.buffer();
    if (c.exclusive) {
      if (verbose) printer.p("/* Exclusive */ ");
      printer.p('!');
    }
    printer.p('[');
    for (CharRange r : c.ranges) {
      printer.p(r);
    }
    printer.p(']');
    if (c.exclusive) {
      printer.p(" .");
    }
    printer.fit();
  }

  /** Print the specified character case. */
  public void visit(CharCase c) {
    if (newline) printer.indent();
    newline = false;

    printer.p(c.klass).p(' ').p(c.element);
  }

  /** Print the specified character switch. */
  public void visit(CharSwitch s) {
    boolean choice   = parenChoice;
    boolean sequence = parenSequence;

    if (choice) {
      printer.p("( ");
    }

    if (verbose) {
      printer.pln("/* Switch */").incr();
    } else {
      printer.pln().incr();
    }

    boolean   firstCase = true;
    boolean   printed   = false;
    CharClass klass     = null;
    for (CharCase kase : s.cases) {
      if (null == kase.element) {
        // If the element of the character case is null, that case
        // corresponds to a formerly exclusive character class.  We do
        // not print such cases here, but rather collect their
        // characters and then print them as part of the base.
        if (null == klass) {
          klass = new CharClass(new ArrayList<CharRange>());
        }
        klass.ranges.addAll(kase.klass.ranges);

      } else {
        if (firstCase) {
          firstCase   = false;
          printer.indent();

        } else {
          printer.indent().p("/ ");
        }

        parenChoice   = true;
        parenSequence = false;
        newline       = false;

        printer.p(kase);
        if (! newline) {
          printer.pln();
        }

        printed       = true;
      }
    }

    if ((null != klass) || (null != s.base)) {
      printer.indent();
      if (printed) {
        printer.p("/ ");
      }

      if (null != klass) {
        newline       = false;

        printer.p('!').p(klass).p(' ');
      }

      if (null != s.base) {
        parenChoice   = true;
        parenSequence = false;
        newline       = false;

        printer.p("_ ").p(s.base);
      }

      if (! newline) {
        printer.pln();
      }
    }

    printer.decr();
    if (choice) {
      printer.indent().p(')');
    }

    parenChoice   = choice;
    parenSequence = sequence;
    newline       = false;
  }

  /**
   * Print the specified action string.
   *
   * @param s The string.
   */
  protected void print(String s) {
    printer.p(s);
  }

  /**
   * Print the specified action.
   *
   * @param a The action to print.
   * @param caret Flag for whether to prefix the action with a caret.
   */
  protected void print(Action a, boolean caret) {
    if (newline) printer.indent();
    if (a.code.isEmpty()) {
      // Nothing to do.
      newline = false;

    } else if (1 == a.code.size()) {
      newline  = false;
      printer.buffer();
      if (caret) printer.p('^');
      printer.p("{ ");
      print(a.code.get(0));
      printer.p(" }").fit();

    } else {
      newline = true;
      if (caret) printer.p('^');

      int baseLevel  = printer.level();
      printer.pln('{').incr();

      int level      = 0;
      Iterator<String>  codeIter   = a.code.iterator();
      Iterator<Integer> indentIter = a.indent.iterator();
      while (codeIter.hasNext()) {
        int    newLevel   = indentIter.next();
        int    diff       = newLevel - level;
        level             = newLevel;

        if (0 < diff) {
          for (int i=0; i<diff; i++) {
            printer.incr();
          }
        } else {
          for (int i=0; i>diff; i--) {
            printer.decr();
          }
        }

        printer.indent();
        print(codeIter.next());
        printer.pln();
      }

      // Restore the base indentation level.  We need to do this
      // explicitly because the removal of empty lines at the end of
      // an action may also remove the corresponding indentation
      // level.
      printer.setLevel(baseLevel).indent().pln('}');
    }
  }

  /** Print the specified node marker. */
  public void visit(NodeMarker m) {
    if (newline) printer.indent();
    newline = false;
    printer.buffer().p('@').p(m.name).fit();
  }

  /** Print the specified action. */
  public void visit(Action a) {
    print(a, false);
  }

  /** Print the specified parser action. */
  public void visit(ParserAction pa) {
    print((Action)pa.element, true);
  }

  /** Print the specified parse tree node. */
  public void visit(ParseTreeNode n) {
    if (newline) printer.indent();
    newline = false;
    printer.buffer().p("Formatting([");
    for (Iterator<Binding> iter = n.predecessors.iterator(); iter.hasNext(); ) {
      printer.p(iter.next().name);
      if (iter.hasNext()) printer.p(", ");
    }
    printer.p("], ");
    if (null == n.node) {
      printer.p("null");
    } else {
      printer.p(n.node.name);
    }
    printer.p(", [");
    for (Iterator<Binding> iter = n.successors.iterator(); iter.hasNext(); ) {
      printer.p(iter.next().name);
      if (iter.hasNext()) printer.p(", ");
    }
    printer.p("])").fit();
  }

  /** Print the specified null literal. */
  public void visit(NullLiteral l) {
    if (newline) printer.indent();
    newline = false;
    printer.buffer().p("null").fit();
  }

  /** Print the specified null value. */
  public void visit(NullValue v) {
    if (newline) printer.indent();
    newline = false;
    printer.buffer().p("/* value = null; */").fit();
  }

  /**
   * Format the specified statically known text value.
   *
   * @param isToken Flag for whether the value is a token.
   * @param text The text.
   */
  protected void format(boolean isToken, String text) {
    if (newline) printer.indent();

    // If the text contains the end sequence for a traditional C
    // comment, we use a C++ comment.
    boolean hasComment = (-1 != text.indexOf("*/"));

    printer.buffer();
    if (hasComment) {
      printer.p("//");
      newline = true;
    } else {
      printer.p("/*");
      newline = false;
    }
    printer.p(" value = ");
    if (isToken) printer.p("Token(");
    printer.p('"').escape(text, stringEscapes()).p('"');
    if (isToken) printer.p(')');
    printer.p(';');
    if (! hasComment) printer.p(" */");
    printer.fit();
    if (hasComment) printer.pln();
  }

  /** Print the specified string value. */
  public void visit(StringValue v) {
    if (null == v.text) {
      if (newline) printer.indent();
      newline  = false;
      printer.buffer().p("/* value = <text>; */").fit();
    } else {
      format(false, v.text);
    }
  }

  /** Print the specified token value. */
  public void visit(TokenValue v) {
    if (null == v.text) {
      if (newline) printer.indent();
      newline = false;
      printer.buffer().p("/* value = Token(<text>); */").fit();
    } else {
      format(true, v.text);
    }
  }

  /** Print the specified binding value. */
  public void visit(BindingValue v) {
    if (newline) printer.indent();
    newline = false;
    printer.buffer().p("/* value = ").p(v.binding.name).p("; */").fit();
  }

  /** Print the specified empty list value. */
  public void visit(EmptyListValue v) {
    if (newline) printer.indent();
    newline  = false;
    printer.buffer().p("/* value = []; */").fit();
  }

  /** Print the specified proper list value. */
  public void visit(ProperListValue v) {
    if (newline) printer.indent();
    newline  = false;
    printer.buffer().p("/* value = ");

    if (null == v.tail) {
      printer.p('[');
      boolean first = true;
      for (Binding b : v.elements) {
        if (first) {
          first = false;
        } else {
          printer.p(", ");
        }
        printer.p(b.name);
      }
      printer.p(']');

    } else {
      for (Binding b : v.elements) {
        printer.p(b.name).p(':');
      }
      printer.p(v.tail.name);
    }
    printer.p("; */").fit();
  }

  /** Print the specified action base value. */
  public void visit(ActionBaseValue v) {
    if (newline) printer.indent();
    newline = false;
    printer.buffer().p("/* value = Action.run(").p(v.list.name).p(", ").
      p(v.seed.name).p("); */").fit();
  }

  /** Print the specified generic node value. */
  public void visit(GenericNodeValue v) {
    if (newline) printer.indent();
    newline = false;

    printer.buffer().p("/* value = ");
    if (0 < v.formatting.size()) printer.p("Formatting([], ");
    printer.p("GNode(").p(Utilities.unqualify(v.name)).p(", [");

    for (Iterator<Binding> iter = v.children.iterator(); iter.hasNext(); ) {
      printer.p(iter.next().name);
      if (iter.hasNext()) {
        printer.p(", ");
      }
    }

    printer.p("])");

    if (0 < v.formatting.size()) {
      printer.p(", [");

      boolean first = true;
      for (Binding b : v.formatting) {
        if (first) {
          first = false;
        } else {
          printer.p(", ");
        }
        printer.p(b.name);
      }

      printer.p("])");
    }

    printer.p("; */").fit();
  }

  /** Print the specified generic action value. */
  public void visit(GenericActionValue v) {
    if (newline) printer.indent();
    newline       = false;

    printer.buffer().p("/* value = Action->");
    if (0 < v.formatting.size()) printer.p("Formatting([], ");
    printer.p("GNode(").p(Utilities.unqualify(v.name)).p(", [");

    Iterator<Binding> iter = v.children.iterator();

    printer.p(v.first);
    if (iter.hasNext()) {
      printer.p(", ");
    }

    while (iter.hasNext()) {
      printer.p(iter.next().name);
      if (iter.hasNext()) {
        printer.p(", ");
      }
    }

    printer.p("])");

    if (0 < v.formatting.size()) {
      printer.p(", [");

      boolean first = true;
      for (Binding b : v.formatting) {
        if (first) {
          first = false;
        } else {
          printer.p(", ");
        }
        printer.p(b.name);
      }

      printer.p("])");
    }

    printer.p("; */").fit();
  }

  /** Print the specified generic recursion value. */
  public void visit(GenericRecursionValue v) {
    if (newline) printer.indent();
    newline       = false;

    printer.buffer().p("/* value = Action->");
    if (0 < v.formatting.size()) printer.p("Formatting([], ");
    printer.p("GNode(").p(Utilities.unqualify(v.name)).p(", [");

    Iterator<Binding> iter = v.children.iterator();

    printer.p(v.first);
    if (iter.hasNext()) {
      printer.p(", ");
    }

    while (iter.hasNext()) {
      printer.p(iter.next().name);
      if (iter.hasNext()) {
        printer.p(", ");
      }
    }

    printer.p("])");

    if (0 < v.formatting.size()) {
      printer.p(", [");

      boolean first = true;
      for (Binding b : v.formatting) {
        if (first) {
          first = false;
        } else {
          printer.p(", ");
        }
        printer.p(b.name);
      }

      printer.p("])");
    }

    printer.p(':').p(v.list.name).p("; */").fit();
  }

}
