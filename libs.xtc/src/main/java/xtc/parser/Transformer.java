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
import java.util.Iterator;
import java.util.List;

import xtc.Constants;

import xtc.tree.Attribute;
import xtc.tree.Visitor;

import xtc.type.AST;
import xtc.type.Type;
import xtc.type.Wildcard;

import xtc.util.Runtime;

/**
 * Visitor to transform productions.  This visitor lifts repetitions,
 * options, and nested choices into their own productions, desugars
 * reptitions and options, and adds the appropriate semantic values to
 * expressions that require them.  It also ensures that repeated,
 * optional, and predicated elements are sequences, thus fulfilling
 * the requirements for {@link CodeGenerator code generation}.  Before
 * applying this visitor on a grammar, the grammar must have been
 * {@link Simplifier simplified}, all text-only productions should be
 * marked as such by applying the {@link TextTester} visitor, and all
 * token-level productions should be marked as such by applying the
 * {@link Tokenizer} visitor.  Note that {@link Generifier} adds the
 * appropriate semantic values for generic productions.  Further note
 * that repetitions in transient productions are not desugared (nor
 * lifted) if the corresponding command line option
 * ("<code>-Orepeated</code") is set.  Also, options are not desugared
 * (nor lifted) if the corresponding command line option
 * ("<code>-Ooptional</code>") is set.  Finally, note that this
 * visitor assumes that the entire grammar is contained in a single
 * module.
 *
 * <p />This visitor may report errors to the user.
 *
 * @author Robert Grimm
 * @version $Revision: 1.127 $
 */
public class Transformer extends Visitor {

  /**
   * The variable marker for bindings in repeated or optional
   * elements.
   */
  public static final String ELEMENT_MARKER = "el";

  // =========================================================================

  /**
   * Visitor to add semantic values.
   *
   * <p />This visitor assumes that each production's top-level
   * element is an ordered choice and that each choice's alternatives
   * are sequences.
   */
  public class Deducer extends Visitor {

    /** The list of elements. */
    protected List<Element> elements;

    /** Create a new deducer. */
    public Deducer() { /* Nothing to do. */ }

    /** Visit the specified production. */
    public void visit(Production p) {
      // Only try to add values to productions that (1) are not
      // generic or list-valued, (2) are not directly left-recursive,
      // or (3) do not contain a quantification (i.e., repetition or
      // option) that will be desugared.
      Element e = Analyzer.strip(p.choice);

      if ((! isGeneric()) &&
          (! isList()) &&
          (! isLeftRecursive()) &&
          ((! (e instanceof Repetition)) || retainRepetitions()) &&
          ((! (e instanceof Option)) || retainOptions())) {
        if (runtime.test("optionVerbose")) {
          System.err.println("[Deducing semantic value for " + p.qName + "]");
        }

        elements = new ArrayList<Element>();
        dispatch(p.choice);
      }
    }

    /** Visit the specified choice. */
    public void visit(OrderedChoice c) {
      // Process the alternatives.
      for (Sequence alt : c.alternatives) dispatch(alt);
    }

    /** Visit the specified sequence. */
    public void visit(Sequence s) {
      // Remember the current number of elements.
      final int base = elements.size();

      // Process the elements of the sequence.
      for (Iterator<Element> iter = s.elements.iterator(); iter.hasNext(); ) {
        Element e = iter.next();

        if ((! iter.hasNext()) && (e instanceof OrderedChoice)) {
          // Continue with the trailing choice.
          dispatch(e);
        } else {
          // Add the current element to the list of traversed elements.
          elements.add(e);
        }
      }

      // Deduce the semantic value if this sequence does not have a
      // trailing choice.
      if (! s.hasTrailingChoice()) {
        if (isVoid()) {
          s.add(NullValue.VALUE);

        } else if (isTextOnly()) {
          String text = analyzer.matchingText(new Sequence(elements));
          if ((null == text) || (! runtime.test("optimizeTerminals"))) {
            s.add(StringValue.VALUE);
          } else {
            s.add(new StringValue(text));
          }

        } else if (isToken()) {
          String text = analyzer.matchingText(new Sequence(elements));
          if ((null == text) || (! runtime.test("optimizeTerminals"))) {
            s.add(TokenValue.VALUE);
          } else {
            s.add(new TokenValue(text));
          }

        } else if (elements.isEmpty()) {
          s.add(NullValue.VALUE);

        } else {
          Binding b = analyzer.bind(elements);
          if (null != b) {
            if (Analyzer.isSynthetic(b.name)) {
              // Patch the variable name to be the semantic value.
              b.name = CodeGenerator.VALUE;
            } else if (! CodeGenerator.VALUE.equals(b.name)) {
              // Preserve the user-specified variable name.
              s.add(new BindingValue(b));
            }
          }
        }
      }

      // Patch back any added binding.
      if ((! isVoid()) && (! isTextOnly()) && (! isToken())) {
        int size = s.size();

        // Ignore trailing choices and value elements.
        if (s.hasTrailingChoice() ||
            ((0 != size) &&
             (s.get(size-1) instanceof ValueElement))) {
          size--;
        }

        // Iterate over all other elements.
        for (int i=0; i<size; i++) {
          Element e = elements.get(base + i);
          if (s.get(i) != e) s.elements.set(i, e);
        }
      }

      // Remove any elements added by processing the sequence.
      if (0 == base) {
        elements.clear();
      } else {
        elements.subList(base, elements.size()).clear();
      }
    }

  }

  // =========================================================================

  /**
   * Visitor to lift nested choices, repetitions, and options.  This
   * visitor also ensures that repeated, optional, and predicated
   * elements are sequences.
   */
  public class Lifter extends GrammarVisitor {

    /** Create a new lifter. */
    public Lifter() {
      super(Transformer.this.runtime, Transformer.this.analyzer);
    }

    /**
     * Bind the specified element.
     *
     * @param e The element.
     * @return The bound element.
     */
    protected Binding bind(Element e) {
      return new Binding(analyzer.variable(ELEMENT_MARKER), e);
    }

    /**
     * Process the specified repeated, optional, or predicated
     * element.  This method processes the specified element, lifting
     * it and adding a binding for its value, either only if
     * necessary.  The result is a sequence to fulfill the
     * corresponding code generation requirement.
     *
     * @param e The element.
     * @param bound The flag for whether the element's value is bound.
     * @return The resulting sequence.
     */
    protected Sequence process(Element e, boolean bound) {
      /*
       * (1) If a repeated, optional, or predicated element explicitly
       * sets a semantic value (i.e., Analyzer.setsValue() returns
       * true), it must be lifted.  Furthermore, a binding must be
       * used to ensure that the lifted choice has a meaningful type.
       *
       * (2) If a repetition or option is bound, the repeated or
       * optional element must have a binding as well, unless the
       * element appears within a text-only or token-level production
       * (whose value is created directly from the buffered input).
       *
       * (3) A sequence's value can only be captured through a binding
       * if it does not explicitly set the semantic value and if it
       * does not contain a choice as its last element.  If either is
       * the case, the sequence must be lifted.
       *
       * (4) A predicated sequence that has a choice as its last
       * element must be lifted.
       *
       * (5) Text-only and token-level productions, by definition,
       * cannot contain bindings to yyValue nor actions that set
       * yyValue.  However, they may contain value elements, causing
       * Analyzer.setsValue() to return true.
       *
       * (6) Setting isLastElement to true results in an ordered
       * choice not being lifted, unless the choice appears in a
       * predicate.
       */

      if (e instanceof OrderedChoice) {

        if ((! isTextOnly()) && (! isToken()) && 
            (bound || Analyzer.setsValue(e, false))) {
          // Bind the choice to preserve its value.  Lift choice.
          return Sequence.ensure((Element)dispatch(bind(e)));

        } else {
          // Try to leave the choice in place.
          isLastElement = true;
          return Sequence.ensure((Element)dispatch(e));
        }

      } else {
        Sequence s = Sequence.ensure(e);

        if ((! isTextOnly()) && (! isToken()) && Analyzer.setsValue(e, false)) {
          // Bind sequence wrapped in choice to preserve value.  Lift choice.
          return Sequence.ensure((Element)dispatch(bind(new OrderedChoice(s))));

        } else if ((! isTextOnly()) && (! isToken()) && 
                   bound && s.hasTrailingChoice()) {
          // Bind sequence wrapped in choice to preserve value.  Lift choice.
          return Sequence.ensure((Element)dispatch(bind(new OrderedChoice(s))));

        } else if ((! isTextOnly()) && (! isToken()) && bound) {
          // Add binding to sequence.
          Binding b = analyzer.bind(s.elements, ELEMENT_MARKER);
          if (null == b) {
            runtime.error("unable to deduce value", s);
          }
          return Sequence.ensure((Element)dispatch(s));

        } else if (isPredicate && s.hasTrailingChoice()) {
          // Wrap sequence in choice and lift choice.
          return Sequence.ensure((Element)dispatch(new OrderedChoice(s)));

        } else {
          // Process sequence.  Try to leave trailing choice in place.
          isLastElement = true;
          return Sequence.ensure((Element)dispatch(s));
        }

      }
    }

    /**
     * Create a new production with the specified type, nonterminal,
     * and ordered choice.  This method creates the production, using
     * the {@link #current() current production's} attributes and
     * text-only and token-level marks, recursively processes the
     * productions, and adds it to the grammar.
     *
     * @param type The type.
     * @param nt The nonterminal.
     * @param c The ordered choice.
     */
    protected void lift(Type type, NonTerminal nt, OrderedChoice c) {
      // Create the production.
      FullProduction p =
        new FullProduction(new ArrayList<Attribute>(current().attributes), type,
                           nt, nt.qualify(analyzer.module().name.name), c);

      // Document activity under verbose mode.
      if (runtime.test("optionVerbose")) {
        System.err.println("[Lifting expression into new production " +
                           p.qName + ']');
      }

      // Do not inherit any public, explicit, stateful, or resetting
      // attribute.
      p.attributes.remove(Constants.ATT_PUBLIC);
      p.attributes.remove(Constants.ATT_EXPLICIT);
      p.attributes.remove(Constants.ATT_STATEFUL);
      p.attributes.remove(Constants.ATT_RESETTING);
      if (isTextOnly()) {
        TextTester.markTextOnly(p, runtime.test("optionVerbose"));
      } else if (isToken()) {
        Tokenizer.markToken(p, runtime.test("optionVerbose"));
      }

      // Recursively process the new production.
      Transformer.this.process(p);

      // Add the new production to the grammar.
      analyzer.add(p);
    }

    /** Visit the specified ordered choice. */
    public Element visit(OrderedChoice c) {
      boolean top      = isTopLevel;
      isTopLevel       = false;
      boolean voided   = isVoided;
      isVoided         = false;
      boolean bound    = isBound;
      isBound          = false;
      boolean last     = isLastElement;
      transformInPlace = top && (Analyzer.strip(c) instanceof Quantification);

      if ((top || last) && (! isPredicate)) {
        // Continue processing.
        final int size = c.alternatives.size();
        for (int i=0; i<size; i++) {
          isLastElement  = top || last;
          c.alternatives.set(i, (Sequence)dispatch(c.alternatives.get(i)));
        }

        isLastElement = false;
        return c;
        
      } else {
        // Lift choice.
        NonTerminal nt = analyzer.choice();
        
        Type type;
        if (isTextOnly()) {
          type = AST.STRING;
        } else if (isToken()) {
          type = AST.TOKEN;
        } else if (bound || ((isGeneric() || isList()) &&
                             (! voided) &&
                             (! isPredicate))) {
          type = AST.ANY;
        } else {
          type = AST.VOID;
        }

        lift(type, nt, c);
        
        isLastElement = false;
        return nt;
      }
    }

    /** Visit the specified repetition. */
    public Element visit(Repetition r) {
      isTopLevel       = false;
      boolean voided   = isVoided;
      isVoided         = false;
      boolean bound    = isBound;
      isBound          = false;
      isLastElement    = false;
      boolean inPlace  = transformInPlace;
      transformInPlace = false;

      // If the repeated optimization is enabled and the production is
      // not memoized, do not lift the repetition.  Also, if the
      // repetition is the only top-level element (besides a wrapping
      // sequence and choice), do not lift the repetition.  However,
      // if the repetition is bound and the production is text-only or
      // token-level, the repetition must be lifted.  Furthermore, if
      // the repetition is the top-level element in a generic
      // production and will be desugared, the repetition must be
      // lifted (since a binding will be added by the Generifier
      // phase).
      if ((retainRepetitions() &&
           ((! bound) || ((! isTextOnly()) && (! isToken())))) ||
          (inPlace && ((! isGeneric()) || retainRepetitions()))) {
        // Process the repeated element.
        boolean b = (bound ||
                     (isGeneric() && (! voided) && (! isPredicate)) ||
                     (isList() && (! voided) && (! isPredicate)) ||
                     (inPlace && (! isVoid())));
        r.element = process(r.element, b);
        return r;
      }

      NonTerminal nt = (r.once)? analyzer.plus() : analyzer.star();

      Type type;
      if (isTextOnly()) {
        type = AST.STRING;
      } else if (isToken()) {
        type = AST.TOKEN;
      } else if (bound || ((isGeneric() || isList()) &&
                           (! voided) &&
                           (! isPredicate))) {
        // Note that the type will be patched during desugaring.
        type = AST.WILD_LIST;
      } else {
        type = AST.VOID;
      }

      OrderedChoice c = new OrderedChoice(r);
      c.setLocation(r);

      lift(type, nt, c);

      return nt;
    }

    /** Visit the specified option. */
    public Element visit(Option o) {
      isTopLevel       = false;
      boolean voided   = isVoided;
      isVoided         = false;
      boolean bound    = isBound;
      isBound          = false;
      isLastElement    = false;
      boolean inPlace  = transformInPlace;
      transformInPlace = false;

      // If the optional optimization is enabled, do not lift the
      // option.  Also, if the option is the only top-level element
      // (besides a wrapping sequence and choice), do not lift the
      // option.  However, if the option is bound and the production
      // is text-only or token-level, the option must be lifted.
      // Furthermore, if the option is the top-level element in a
      // generic production and will be desugared, the option must be
      // lifted (since a binding will be added by the Generifier
      // phase).
      if ((retainOptions() && 
           ((! bound) || ((! isTextOnly()) && (! isToken())))) ||
          (inPlace && ((! isGeneric()) || retainOptions()))) {
        // Process the optional element.
        boolean b = (bound ||
                     (isGeneric() && (! voided) && (! isPredicate)) ||
                     (isList() && (! voided) && (! isPredicate)) ||
                     (inPlace && (! isVoid())));
        o.element = process(o.element, b);
        return o;
      }

      NonTerminal nt = analyzer.option();

      Type type;
      if (isTextOnly()) {
        type = AST.STRING;
      } else if (isToken()) {
        type = AST.TOKEN;
      } else if (bound || ((isGeneric() || isList()) &&
                           (! voided) &&
                           (! isPredicate))) {
        type = AST.ANY;
      } else {
        type = AST.VOID;
      }

      OrderedChoice c = new OrderedChoice(o);
      c.setLocation(o);

      lift(type, nt, c);

      return nt;
    }

    /** Visit the specified predicate. */
    public Element visit(Predicate p) {
      isTopLevel        = false;
      isVoided          = false;
      isBound           = false;
      isLastElement     = false;
      boolean predicate = isPredicate;
      isPredicate       = true;

      p.element         = process(p.element, false);

      isPredicate       = predicate;
      return p;
    }

  }

  // =========================================================================

  /**
   * Visitor to desugar repetitions and options.  This visitor assumes
   * that a production's top-level element is an ordered choice with a
   * sequence for each alternative.
   */
  public class Desugarer extends Visitor {

    /** Create a new desugarer. */
    public Desugarer() { /* Nothing to do. */ }

    /**
     * Process the specified sequence.  This method adds a nonterminal
     * and value element at the end of the specified sequence,
     * recursing into the sequences of any ordered choice appearing as
     * a sequence's last element.  The current production must be a
     * void, text-only, or token-level production.
     *
     * @param s The sequence.
     * @param nt The nonterminal or <code>null</code> if no nonterminal
     *   should be added.
     */
    protected void process(Sequence s, NonTerminal nt) {
      if (s.hasTrailingChoice()) {
        OrderedChoice c = (OrderedChoice)s.get(s.size()-1);
        for (Sequence alt : c.alternatives) process(alt, nt);

      } else {
        if (null != nt) {
          s.add(nt);
        }

        if (isVoid()) {
          s.add(NullValue.VALUE);
        } else if (isTextOnly()) {
          s.add(StringValue.VALUE);
        } else if (isToken()) {
          s.add(TokenValue.VALUE);
        } else {
          assert false;
        }
      }
    }

    /** Visit the specified production. */
    public void visit(Production p) {
      Element e = Analyzer.strip(p.choice);

      if ((e instanceof Repetition) && (! retainRepetitions())) {
        if (runtime.test("optionVerbose")) {
          System.err.println("[Desugaring repetition in " + p.qName + ']');
        }

        // If the repeated element is a sequence without choices and
        // that sequence's value is a nonterminal, record the
        // nonterminal in the production's repeated property.
        Sequence s = (Sequence)((Repetition)e).element;
        if (! s.hasTrailingChoice()) {
          Binding b = Analyzer.getBinding(s.elements);
          if ((null != b) && (b.element instanceof NonTerminal)) {
            p.setProperty(Properties.REPEATED, b.element);
          }
        }

        // Desugar the repetition.
        p.choice = (OrderedChoice)dispatch(e);

      } else if ((e instanceof Option) && (! retainOptions())) {
        if (runtime.test("optionVerbose")) {
          System.err.println("[Desugaring option in " + p.qName + ']');
        }

        p.choice = (OrderedChoice)dispatch(e);
        p.setProperty(Properties.OPTION, Boolean.TRUE);
      }
    }

    /** Visit the specified repetition. */
    public Element visit(Repetition r) {
      Sequence repeated = (Sequence)r.element;

      // Set up the lists of old and new alternatives.
      List<Sequence> oldAlternatives;
      if ((1 == repeated.size()) && repeated.hasTrailingChoice()) {
        oldAlternatives = ((OrderedChoice)repeated.get(0)).alternatives;
      } else {
        oldAlternatives = new ArrayList<Sequence>(1);
        oldAlternatives.add(repeated);
      }

      List<Sequence> newAlternatives =
        new ArrayList<Sequence>(oldAlternatives.size() +
                                ((r.once)? oldAlternatives.size() : 1));

      // Determine the production's generic list type.
      Type type = Wildcard.TYPE;
      if ((! isVoid()) && (! isTextOnly()) && (! isToken())) {
        for (Sequence s : oldAlternatives) {
          Binding  b = Analyzer.getBinding(s.elements);
          if (null == b) {
            runtime.error("unable to bind repeated element", s);
          } else {
            type = ast.unify(type, analyzer.type(b.element), false);
          }
        }

        type = AST.listOf(ast.concretize(type, AST.ANY));

        // If the production is synthetic, patch its type.
        if (Analyzer.isSynthetic(current().name)) {
          if (runtime.test("optionVerbose")) {
            System.err.println("[Adjusting " + current().qName +
                               "'s type to " +
                               ast.extern(type) + ']');
          }

          current().type = type;
        }
      }

      // Process the recursive alternatives.
      for (Sequence s : oldAlternatives) {
        if (r.once) {
          // Copy the base alternative(s) to avoid creating a DAG,
          // which might lead to problems in later phases that modify
          // any elements (e.g., the terminal optimizer may convert
          // character classes into character switches, which
          // internally still use the character class element but with
          // the exclusive flag set to false).
          s = analyzer.copy(s);
        }

        if (isVoid() || isTextOnly() || isToken()) {
          process(s, current().name);

        } else {
          Binding b1 = Analyzer.getBinding(s.elements);
          Binding b2 = new Binding(analyzer.variable(), current().name);
          if (null != b1) {
            s.add(b2).add(new ProperListValue(type, b1, b2));
          }
        }

        newAlternatives.add(s);
      }

      // Process the base alternative(s).
      if (r.once) {
        for (Sequence s : oldAlternatives) {
          if (isVoid() || isTextOnly() || isToken()) {
            process(s, null);

          } else {
            Binding b = Analyzer.getBinding(s.elements);
            if (null != b) {
              s.add(new ProperListValue(type, b));
            }
          }

          newAlternatives.add(s);
        }

      } else {
        Sequence s = new Sequence();

        if (isVoid()) {
          s.add(NullValue.VALUE);
        } else if (isTextOnly()) {
          s.add(StringValue.VALUE);
        } else if (isToken()) {
          s.add(TokenValue.VALUE);
        } else {
          s.add(EmptyListValue.VALUE);
        }

        newAlternatives.add(s);
      }

      // Create the new ordered choice and return it.
      return new OrderedChoice(newAlternatives);
    }

    /** Visit the specified option. */
    public Element visit(Option o) {
      Sequence optional = (Sequence)o.element;

      List<Sequence> oldAlternatives;
      if ((1 == optional.size()) && optional.hasTrailingChoice()) {
        oldAlternatives = ((OrderedChoice)optional.get(0)).alternatives;
      } else {
        oldAlternatives = new ArrayList<Sequence>(1);
        oldAlternatives.add(optional);
      }

      List<Sequence> newAlternatives =
        new ArrayList<Sequence>(oldAlternatives.size() + 1);
      
      // The matching alternatives.
      for (Sequence s : oldAlternatives) {
        if (isVoid() || isTextOnly() || isToken()) {
          process(s, null);

        } else {
          Binding b = Analyzer.getBinding(s.elements);
          if (null != b) {
            // Patch the variable name to be the semantic value.
            b.name = CodeGenerator.VALUE;
          }
        }

        newAlternatives.add(s);
      }

      // The empty alternative.
      Sequence alt = new Sequence();
      
      if (isTextOnly()) {
        alt.add(StringValue.VALUE);
      } else if (isToken()) {
        alt.add(TokenValue.VALUE);
      } else {
        alt.add(NullValue.VALUE);
      }
      newAlternatives.add(alt);

      // Create the new ordered choice and return it.
      return new OrderedChoice(newAlternatives);
    }

  }

  // =========================================================================

  /**
   * Visitor to deduce a production's type.
   */
  public class Typer extends Visitor {

    /** The list of elements. */
    protected List<Element> elements;

    /** The current type. */
    protected Type type;

    /** Create a new typer. */
    public Typer() { /* Nothing to do. */ }

    /** Visit the specified production. */
    public void visit(Production p) {
      // Only try to type production's semantic value if the
      // production is synthetic (i.e. lifted) and the type is the
      // root type.  Note that this condition correctly excludes void,
      // text-only, token-level, and generic productions.
      if (Analyzer.isSynthetic(p.name) && AST.isAny(p.type)) {
        elements = new ArrayList<Element>();
        type     = Wildcard.TYPE;
        dispatch(p.choice);
        if ((! type.isWildcard()) && (! AST.isAny(type))) {
          // Only adjust the type, if a new more specific type has
          // been found.
          if (runtime.test("optionVerbose")) {
            System.err.println("[Adjusting " + p.qName + "'s type to " +
                               ast.extern(type) + ']');
          }

          p.type = ast.concretize(type, AST.ANY);
        }
      }
    }

    /** Visit the specified choice. */
    public void visit(OrderedChoice c) {
      // Process the alternatives.
      for (Sequence alt : c.alternatives) dispatch(alt);
    }

    /** Visit the specified sequence. */
    public void visit(Sequence s) {
      // Remember the current number of elements.
      final int base = elements.size();

      // Process the elements of the sequence.
      for (Iterator<Element> iter = s.elements.iterator(); iter.hasNext(); ) {
        Element e = iter.next();

        if ((! iter.hasNext()) && (e instanceof OrderedChoice)) {
          // Continue with the trailing choice.
          dispatch(e);
        } else {
          // Add the current element to the list of traversed elements.
          elements.add(e);
        }
      }

      // Deduce the semantic value.
      if (! s.hasTrailingChoice()) {
        Binding b = Analyzer.getBinding(elements);

        if ((null != b) &&
            (CodeGenerator.VALUE.equals(b.name) ||
             ((0 != s.size()) &&
              (s.get(s.size()-1) instanceof BindingValue) &&
              b.name.equals(((BindingValue)s.get(s.size()-1)).binding.name)))) {
          // Unify the binding's type with any previously determined
          // type.
          type = ast.unify(type, analyzer.type(b.element), false);

        } else if (! Analyzer.setsNullValue(elements)) {
          // We don't have any meaningful information.  Assume the
          // worst.
          type = AST.ANY;
        }
      }

      // Remove any elements added by processing the sequence.
      if (0 == base) {
        elements.clear();
      } else {
        elements.subList(base, elements.size()).clear();
      }
    }

  }

  // =========================================================================

  /** The runtime. */
  protected final Runtime runtime;

  /** The analyzer. */
  protected final Analyzer analyzer;

  /** The type operations. */
  protected final AST ast;

  /**
   * The flag for whether the current module has the {@link
   * Constants#ATT_PARSE_TREE parseTree} attribute.
   */
  protected boolean hasParseTree;

  /** The current (full) production. */
  protected FullProduction production;

  /**
   * Create a new transformer.
   *
   * @param runtime The runtime.
   * @param analyzer The analyzer utility.
   * @param ast The type operations.
   */
  public Transformer(Runtime runtime, Analyzer analyzer, AST ast) {
    this.runtime  = runtime;
    this.analyzer = analyzer;
    this.ast      = ast;
  }

  /**
   * Process the specified production.  This method may be called
   * recursively while processing another production.
   *
   * @param p The production to process.
   */
  protected void process(FullProduction p) {
    FullProduction saved = production;
    production           = p;

    new Deducer().dispatch(production);
    new Lifter().dispatch(production);
    new Desugarer().dispatch(production);
    new Typer().dispatch(p);

    production = saved;
  }

  /**
   * Determine whether the current production is memoized.
   *
   * @return <code>true</code> if the current production is memoized.
   */
  protected boolean isMemoized() {
    return production.isMemoized();
  }

  /**
   * Determine whether the current production is void.
   *
   * @return <code>true</code> if the current production is void.
   */
  protected boolean isVoid() {
    return AST.isVoid(production.type);
  }

  /**
   * Determine whether the current production is text-only.
   *
   * @return <code>true</code> if the current production is text-only.
   */
  protected boolean isTextOnly() {
    return production.getBooleanProperty(Properties.TEXT_ONLY);
  }

  /**
   * Determine whether the current production is token-level.
   *
   * @return <code>true</code> if the current production is token-level.
   */
  protected boolean isToken() {
    return production.getBooleanProperty(Properties.TOKEN);
  }

  /**
   * Determine whether the current production is generic.
   *
   * @return <code>true</code> if the current production is generic.
   */
  protected boolean isGeneric() {
    return Generifier.isGeneric(production);
  }

  /**
   * Determine whether the current production has a list value.
   *
   * @return <code>true</code> if the current production has a list value.
   */
  protected boolean isList() {
    return AST.isList(production.type);
  }

  /**
   * Determine whether the current production is a directly
   * left-recursive production.
   *
   * @return <code>true</code> if the current production is directly
   *   left-recursive.
   */
  protected boolean isLeftRecursive() {
    return DirectLeftRecurser.isTransformable(production);
  }

  /**
   * Get the current production.
   *
   * @return The current production.
   */
  protected Production current() {
    return production;
  }

  /**
   * Determine whether the current production may contain repetitions.
   *
   * @return <code>true</code> if the production may contain
   *   repetitions.
   */
  protected boolean retainRepetitions() {
    // Note: when reducing a grammar to only the expressions that
    // contribute to the AST, we retain repetitions to reduce clutter.
    return ((runtime.test("optimizeRepeated") && (! isMemoized())) ||
            runtime.test("optionValued"));
  }

  /**
   * Determine whether the current production may contain options.
   *
   * @return <code>true</code> if the production may contain options.
   */
  protected boolean retainOptions() {
    // Note: when reducing a grammar to only the expressions that
    // contribute to the AST, we retain options to reduce clutter.
    return runtime.test("optimizeOptional") || runtime.test("optionValued");
  }

  /** Visit the specified grammar. */
  public void visit(Module m) {
    // Initialize the per-grammar state.
    analyzer.register(this);
    analyzer.init(m);
    hasParseTree = m.hasAttribute(Constants.ATT_PARSE_TREE);

    // Now, process the productions.
    for (int i=0; i<m.productions.size(); i++) {
      Production p = m.productions.get(i);

      // Process the production.
      analyzer.startAdding();
      analyzer.process(p);

      // If there are new productions, add them to the grammar and
      // make sure they are not processed again.
      i += analyzer.addNewProductionsAt(i+1);
    }
  }

  /** Visit the specified production. */
  public void visit(FullProduction p) {
    process(p);
  }

}
