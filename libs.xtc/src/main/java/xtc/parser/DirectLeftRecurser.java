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
import java.util.List;

import xtc.Constants;

import xtc.tree.Attribute;
import xtc.tree.Visitor;

import xtc.type.AST;
import xtc.type.Type;

import xtc.util.Runtime;
import xtc.util.Utilities;

/**
 * Visitor to transform direct left-recursions into equivalent
 * right-recursions.  As a practical matter, this visitor transforms
 * only void, text-only, token-level, and generic productions.  For
 * generic productions, it builds on {@link xtc.util.Action actions}
 * to ensure that the resulting right-recursive productions preserve
 * the structure of the original productions' semantic values.  In
 * particular, left-associative data structures remain
 * left-associative, even after transformation.  This visitor requires
 * that all nested choices that do not appear as the last element in a
 * sequence have been lifted.  It also assumes that the entire grammar
 * is contained in a single module.
 *
 * <p />This visitor may report errors to the user.
 *
 * @author Robert Grimm
 * @version $Revision: 1.82 $
 */
public class DirectLeftRecurser extends Visitor {

  /** The flag for processing the recursive case. */
  public static final int STATE_RECURSION = 1;

  /** The flag for processing the base case. */
  public static final int STATE_BASE = STATE_RECURSION + 1;

  /** The runtime. */
  protected final Runtime runtime;

  /** The analyzer utility. */
  protected final Analyzer analyzer;

  /** The type operations. */
  protected final AST ast;

  /** The flag for whether we are transforming a void production. */
  protected boolean isVoid;

  /** The flag for whether we are transforming a text-only production. */
  protected boolean isTextOnly;

  /** The flag for whether we are transforming a token-level production. */
  protected boolean isToken;

  /** The flag for whether we are transforming a generic production. */
  protected boolean isGeneric;

  /**
   * The flag for whether the grammar has the {@link Constants#ATT_CONSTANT
   * constant} attribute.
   */
  protected boolean hasConstant;

  /**
   * The flag for whether the grammar has the {@link
   * Constants#ATT_PARSE_TREE parseTree} attribute.
   */
  protected boolean hasParseTree;

  /** The current state. */
  protected int state;

  /**
   * Flag for whether the current element is the top-level element of
   * a production.
   */
  protected boolean isTopLevel;

  /** Flag for whether we have processed an ordered choice. */
  protected boolean seenChoice;

  /**
   * The list of variables representing the children of the generic
   * node to be created.
   */
  protected List<Binding> children;

  /** The list of node markers. */
  protected List<NodeMarker> markers;

  /** The new tail production. */
  protected FullProduction pTail;

  /** The binding for the seed value. */
  protected Binding seed;

  /** The name of the action's argument. */
  protected String varAction;

  /**
   * Create a new direct left-recurser.
   *
   * @param runtime The runtime.
   * @param analyzer The analyzer utility.
   * @param ast The type operations.
   */
  public DirectLeftRecurser(Runtime runtime, Analyzer analyzer, AST ast) {
    this.runtime  = runtime;
    this.analyzer = analyzer;
    this.ast      = ast;
    this.children = new ArrayList<Binding>();
    this.markers  = new ArrayList<NodeMarker>();
  }

  /**
   * Create a binding for the specified element.  This method also
   * adds the name of the bound variable to the end of the list of
   * children.
   *
   * @param e The element to bind.
   * @return The corresponding binding.
   */
  protected Binding bind(Element e) {
    Binding b = new Binding(analyzer.variable(Generifier.MARKER), e);
    children.add(b);
    return b;
  }

  /** Process the specified grammar. */
  public void visit(Module m) {
    if ((! runtime.test("optimizeLeftRecursions")) &&
        (! runtime.test("optimizeLeftIterations"))) {
      return;
    }

    // Initialize the analyzer.
    analyzer.register(this);
    analyzer.init(m);

    // Initialize the per-grammar state.
    hasConstant  = m.hasAttribute(Constants.ATT_CONSTANT);
    hasParseTree = m.hasAttribute(Constants.ATT_PARSE_TREE);

    // Process the productions.
    for (int i=0; i<m.productions.size(); i++) {
      FullProduction p = (FullProduction)m.productions.get(i);

      if (! isTransformable(p)) continue;

      // Note to user.
      if (runtime.test("optionVerbose")) {
        System.err.println("[Transforming left-recursion in " + p.qName + "]");
      }

      // Process the production.
      analyzer.startAdding();
      analyzer.process(p);

      // If there are new productions, add them to the grammar and
      // skip them for further processing.
      i += analyzer.addNewProductionsAt(i+1);

      // The production is not transformable anymore.
      p.removeProperty(Properties.TRANSFORMABLE);
    }
  }

  /** Visit the specified production. */
  public void visit(FullProduction p) {
    // Reset pre-production state.
    isVoid     = AST.isVoid(p.type);
    isTextOnly = p.getBooleanProperty(Properties.TEXT_ONLY);
    isToken    = p.getBooleanProperty(Properties.TOKEN);
    isGeneric  = AST.isGenericNode(p.type);

    isTopLevel = true;
    seenChoice = false;
    children.clear();
    markers.clear();
    seed       = null;
    varAction  = null;

    // Create the new right-recursive production and the action
    // variable.  Note that any public, explicit, stateful, and
    // resetting attributes are not inherited.
    List<Attribute> attributes = new ArrayList<Attribute>(p.attributes);
    attributes.remove(Constants.ATT_PUBLIC);
    attributes.remove(Constants.ATT_EXPLICIT);
    attributes.remove(Constants.ATT_STATEFUL);
    attributes.remove(Constants.ATT_RESETTING);
    if (isGeneric &&
        (! attributes.contains(Constants.ATT_CONSTANT)) && (! hasConstant)) {
      // The semantic value is an anonmymous inner class that
      // references outside bindings, which, in turn, must be
      // constant.
      attributes.add(Constants.ATT_CONSTANT);
    }
    if (runtime.test("optimizeLeftIterations") &&
        (! attributes.contains(Constants.ATT_TRANSIENT))) {
      // A new tail production always is transient.
      attributes.add(Constants.ATT_TRANSIENT);
    }
    // While a new tail production may be transient, it is never
    // inline.
    attributes.remove(Constants.ATT_INLINE);

    Type type   = null;
    if (isGeneric) {
      if (runtime.test("optimizeLeftIterations")) {
        // The tail production returns a single action.
        type    = AST.actionOf(p.type);

      } else {
        // The tail production returns a list of actions.
        type    = AST.listOf(AST.actionOf(p.type));
      }

    } else {
      // The tail production returns nothing.
      type      = AST.VOID;
    }
    pTail       = new FullProduction(attributes, type, analyzer.tail(), null,
                                     new OrderedChoice());
    pTail.qName = pTail.name.qualify(analyzer.module().name.name);

    if (isGeneric) {
      varAction = analyzer.variable();
    }

    // Process the production's element.
    p.choice = (OrderedChoice)dispatch(p.choice);

    // Patch the type of this production (but only for dynamic nodes).
    if (isGeneric && AST.isDynamicNode(p.type)) p.type = AST.NODE;

    // Add the base alternative to the right-recursive production.
    if (! runtime.test("optimizeLeftIterations")) {
      Sequence s = new Sequence();

      if (isGeneric) {
        s.add(EmptyListValue.VALUE);
      } else {
        s.add(NullValue.VALUE);
      }
      pTail.choice.alternatives.add(s);
    }

    // Prepare the right-recursive production for addition to the
    // grammar.
    if (! (runtime.test("optimizeLeftIterations") && 
           (isVoid || isTextOnly || isToken))) {
      analyzer.add(pTail);
    }

    // Mark the production as a transformed left-recursive production.
    if (isGeneric) {
      Generifier.markGenericRecursion((FullProduction)analyzer.current(),
                                      runtime.test("optionVerbose"));
    }
  }

  /** Visit the specified ordered choice. */
  public Element visit(OrderedChoice c) {
    boolean top = isTopLevel;
    isTopLevel  = false;

    // Process the alternatives.
    for (int i=0; i<c.alternatives.size(); i++) {
      Sequence alternative = c.alternatives.get(i);

      if (top) {
        // Alternatives in the top-level choice need to be processed
        // differently, depending on whether they represent a
        // left-recursion or a base case.
        if (isRecursive(alternative, (FullProduction)analyzer.current())) {
          state = STATE_RECURSION;

          // Make sure that the recursive case in generic productions
          // actually has an automatically determinable value.
          if (isGeneric && Analyzer.setsValue(alternative, false)) {
            runtime.error("unable to determine value of recursion",alternative);
          }

          // Remove the first, directly left-recursive element from
          // the sequence.
          alternative.elements.remove(0);

          // Remove the sequence from the base production, process it,
          // and add the result to the new recursive production.  If
          // the result is a sequence with an ordered choice as its
          // only element, we add the alternatives directly to avoid a
          // choice nested within a choice.
          c.alternatives.remove(i);
          i--;
          Sequence result = (Sequence)dispatch(alternative);
          if ((1 == result.size()) &&
              (result.get(0) instanceof OrderedChoice)) {
            pTail.choice.alternatives.
              addAll(((OrderedChoice)result.get(0)).alternatives);
          } else {
            pTail.choice.alternatives.add(result);
          }

        } else {
          state = STATE_BASE;

          if (isGeneric) {
            // Bind the seed value.
            Binding b = Analyzer.getBinding(alternative.elements);

            if (null == b) {
              // No binding found. Try to create one.
              b = analyzer.bind(alternative.elements, Generifier.MARKER);
              
              if (null == b) {
                runtime.error("unable to determine value of recursion's "+
                              "base case", alternative);
                b = new Binding(Analyzer.DUMMY, alternative);
              }
            }
            seed = b;

            // Check the seed value's type.
            if (! Analyzer.DUMMY.equals(b.name)) {
              Type type = analyzer.type(b.element);

              if (AST.isAny(type)) {
                if ((! analyzer.module().
                     hasAttribute(Constants.ATT_NO_WARNINGS)) &&
                    (! analyzer.current().
                     hasAttribute(Constants.ATT_NO_WARNINGS))) {
                  runtime.error("value of recursion's base case may not " +
                                "be a node", b.element);
                }
              } else if ((! type.resolve().isWildcard()) &&
                         (! AST.isNode(type))) {
                runtime.error("value of recursion's base case not a node",
                              b.element);
              }
            }
          }

          // There is nothing to do for void and text-only productions.

          // Process the alternative.
          c.alternatives.set(i, (Sequence)dispatch(alternative));
        }

      } else {
        // Alternatives in nested choices require no special processing.
        c.alternatives.set(i, (Sequence)dispatch(alternative));
      }
    }

    // Record that we have seen a choice.
    seenChoice = true;

    // Done.
    return c;
  }

  /** Visit the specified repetition. */
  public Element visit(Repetition r) {
    isTopLevel = false;

    if (isGeneric && (STATE_RECURSION == state)) {
      return bind(r);
    } else {
      return r;
    }
  }

  /** Visit the specified option. */
  public Element visit(Option o) {
    isTopLevel = false;

    if (isGeneric && (STATE_RECURSION == state)) {
      return bind(o);
    } else {
      return o;
    }
  }

  /** Visit the specified sequence. */
  public Element visit(Sequence s) {
    isTopLevel     = false;

    // Remember the current number of children and markers.
    final int base  = children.size();
    final int base2 = markers.size();

    // Process the elements of the sequence.
    final int size = s.size();
    for (int i=0; i<size; i++) {
      s.elements.set(i, (Element)dispatch(s.get(i)));
    }

    // If this sequence has not ended with a choice, add the
    // appropriate semantic value.
    if (seenChoice) {
      seenChoice = false;

    } else if (STATE_RECURSION == state) {
      if (runtime.test("optimizeLeftIterations")) {
        if (isGeneric) {
          // Add a generic action value.
          String name = analyzer.current().qName.name;
          if (! markers.isEmpty()) {
            name = Utilities.qualify(Utilities.getQualifier(name),
                                     markers.get(markers.size()-1).name);
          }

          final List<Binding> formatting;
          if (s.hasProperty(Properties.FORMATTING)) {
            formatting = Properties.getFormatting(s);
          } else {
            formatting = new ArrayList<Binding>(0);
          }

          s.add(new GenericActionValue(name, varAction,
                                       new ArrayList<Binding>(children),
                                       formatting));
        }

        // There's nothing to add for void and text-only productions.

      } else {
        if (isGeneric) {
          // Add a recursive invocation and a generic recursion value.
          final Binding b = new Binding(analyzer.variable(), pTail.name);
          s.add(b);

          String name = analyzer.current().qName.name;
          if (! markers.isEmpty()) {
            name = Utilities.qualify(Utilities.getQualifier(name),
                                     markers.get(markers.size()-1).name);
          }

          final List<Binding> formatting;
          if (s.hasProperty(Properties.FORMATTING)) {
            formatting = Properties.getFormatting(s);
          } else {
            formatting = new ArrayList<Binding>(0);
          }

          s.add(new GenericRecursionValue(name, varAction,
                                          new ArrayList<Binding>(children), 
                                          formatting, b));
        } else {
          // Add a recursive invocation and a null value.
          s.add(pTail.name);
          s.add(NullValue.VALUE);
        }
      }

    } else if (STATE_BASE == state) {
      if (runtime.test("optimizeLeftIterations")) {
        if (isGeneric) {
          // Add a binding to the repeated tail nonterminal, which, in
          // turn, must be bound so that the production voider does
          // not inadvertently void the tail production.  Also note
          // that the repeated element must be a sequence to preserve
          // the code generator's contract.  After the binding, add an
          // action base value.
          Binding    b1 = new Binding(analyzer.variable(), pTail.name);
          Repetition r  = new Repetition(false, new Sequence(b1));
          Binding    b2 = new Binding(analyzer.variable(), r);
          s.add(b2).add(new ActionBaseValue(b2, seed));

        } else {
          // Add a copy of the repeated tail expression and the
          // appropriate text or null value.
          Element  e = analyzer.copy(Analyzer.strip(pTail.choice));
          s.add(new Repetition(false, Sequence.ensure(e)));
          if (isTextOnly) {
            s.add(StringValue.VALUE);
          } else if (isToken) {
            s.add(TokenValue.VALUE);
          } else {
            s.add(NullValue.VALUE);
          }
        }

      } else {
        if (isGeneric) {
          // Add the bound tail nonterminal and an action base value.
          Binding b = new Binding(analyzer.variable(), pTail.name);
          s.add(b).add(new ActionBaseValue(b, seed));

        } else if (isTextOnly) {
          // Add a text value.
          s.add(pTail.name).add(StringValue.VALUE);

        } else if (isToken) {
          // Add a token value.
          s.add(pTail.name).add(TokenValue.VALUE);

        } else {
          // Add a null value.
          s.add(pTail.name).add(NullValue.VALUE);
        }
      }

    } else {
      throw new IllegalStateException("Invalid state " + state);
    }

    // Remove any children and markers added by processing the sequence.
    if (isGeneric && (STATE_RECURSION == state)) {
      if (0 == base) {
        children.clear();
      } else {
        children.subList(base, children.size()).clear();
      }

      if (0 == base2) {
        markers.clear();
      } else {
        markers.subList(base2, markers.size()).clear();
      }
    }

    // Done.
    return s;
  }

  /** Visit the specified binding. */
  public Element visit(Binding b) {
    isTopLevel = false;

    if (isGeneric && (STATE_RECURSION == state)) {
      // Make sure the binding is not to CodeGenerator.VALUE, i.e., yyValue.
      if (CodeGenerator.VALUE.equals(b.name)) {
        runtime.error("illegal binding to yyValue in left-recursive sequence",
                      b);
      }

      // Record the binding.
      children.add(b);

      // We assume that the bound expression does not require any
      // further processing.  I.e., if it is a repetition, option, or
      // choice, it already has been lifted and replaced by a
      // nonterminal.
    }

    return b;
  }

  /** Visit the specified string match. */
  public Element visit(StringMatch m) {
    isTopLevel = false;

    if (isGeneric && (STATE_RECURSION == state)) {
      return bind(m);
    } else {
      return m;
    }
  }

  /** Visit the specified nonterminal. */
  public Element visit(NonTerminal nt) {
    isTopLevel   = false;

    if (isGeneric && (STATE_RECURSION == state)) {
      FullProduction p = analyzer.lookup(nt);
      if (AST.isVoid(p.type)) {
        return nt;
      } else {
        return bind(nt);
      }

    } else {
      return nt;
    }
  }

  /** Visit the specified string literal. */
  public Element visit(StringLiteral l) {
    isTopLevel = false;

    if (isGeneric && (STATE_RECURSION == state)) {
      return bind(l);
    } else {
      return l;
    }
  }

  /** Visit the specified parse tree node. */
  public Element visit(ParseTreeNode n) {
    isTopLevel = false;

    if (isGeneric && (STATE_RECURSION == state)) {
      return bind(n);
    } else {
      return n;
    }
  }

  /** Visit the specified null literal. */
  public Element visit(NullLiteral l) {
    isTopLevel = false;

    if (isGeneric && (STATE_RECURSION == state)) {
      return bind(l);
    } else {
      return l;
    }
  }

  /** Visit the specified node marker. */
  public Element visit(NodeMarker m) {
    isTopLevel = false;
    markers.add(m);
    return m;
  }

  /**
   * Visit the specified element.  This method provides the default
   * implementation for repetitions, options, predicates, voided
   * elements, character terminals, (parser) actions, and value
   * elements.
   */
  public Element visit(Element e) {
    isTopLevel = false;
    return e;
  }

  /**
   * Determine whether the specified production contains a direct
   * left-recursion that is transformable into the corresponding
   * right-recursion by this visitor.  Note that, for a production to
   * be transformable, all direct left-recursions must precede the
   * corresponding base cases.  Further nore that this method assumes
   * that the specified production's element is an ordered choice of
   * sequences.
   *
   * @param p The production.
   * @return <code>true</code> if the production is transformable
   *   by this visitor.
   */
  public static boolean isTransformable(FullProduction p) {
    // Currently, only void, text-only, token-level and generic node
    // productions can be transformed.
    if (! (AST.isVoid(p.type) ||
           p.getBooleanProperty(Properties.TEXT_ONLY) ||
           p.getBooleanProperty(Properties.TOKEN) ||
           AST.isGenericNode(p.type))) {
      return false;
    } else if (p.hasProperty(Properties.TRANSFORMABLE)) {
      return p.getBooleanProperty(Properties.TRANSFORMABLE);
    }

    // Analyze the top-level alternatives.
    boolean seenRecursion = false;
    boolean seenBase      = false;
    for (Sequence s : p.choice.alternatives) {
      // An empty sequence cannot be directly left-recursive nor can
      // it be a valid base case (which needs to match some input).
      if (s.isEmpty()) {
        p.setProperty(Properties.TRANSFORMABLE, Boolean.FALSE);
        return false;
      }

      final Element e = Analyzer.stripAndUnbind(s.get(0));
      if (p.name.equals(e) || p.qName.equals(e)) {
        // The sequence represents a direct left-recursion.

        if ((! seenRecursion) || (! seenBase)) {
          // A direct left-recursion before a base case.
          seenRecursion = true;
        } else {
          // A direct left-recursion after a base case.
          p.setProperty(Properties.TRANSFORMABLE, Boolean.FALSE);
          return false;
        }

      } else {
        // The sequence represents a base case.

        if (! seenRecursion) {
          // A base case without a preceding direct left-recursion.
          p.setProperty(Properties.TRANSFORMABLE, Boolean.FALSE);
          return false;
        } else {
          seenBase = true;
        }
      }
    }

    // We need at least one direct left-recursion and one base
    // sequence.
    if (seenRecursion && seenBase) {
      p.setProperty(Properties.TRANSFORMABLE, Boolean.TRUE);
      return true;
    } else {
      p.setProperty(Properties.TRANSFORMABLE, Boolean.FALSE);
      return false;
    }
  }

  /**
   * Determine whether the specified sequence is directly
   * left-recursive.  The specified sequence must be a top-level
   * alternative of the specified production, which, in turn, must be
   * {@link #isTransformable transformable}.
   *
   * @param s The sequence.
   * @param p The production.
   * @return <code>true</code> if the specified sequence is directly
   *   left-recursive.
   */
  public static boolean isRecursive(Sequence s, FullProduction p) {
    if (s.isEmpty()) return false;

    final Element e = Analyzer.stripAndUnbind(s.get(0));
    return (p.name.equals(e) || p.qName.equals(e));
  }

  /**
   * Determine whether the specified sequence represents a base case
   * for a direct left-recursion.  The specified sequence must be a
   * top-level alternative for the specified production, which, in
   * turn, must be {@link #isTransformable transformable}.
   *
   * @param s The sequence.
   * @param p The production.
   * @return <code>true</code> if the specified sequence is a base
   *   case for the direct left-recursion.
   */
  public static boolean isBase(Sequence s, FullProduction p) {
    return (! isRecursive(s, p));
  }

}
