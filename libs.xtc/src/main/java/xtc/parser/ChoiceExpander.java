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

import xtc.Constants;

import xtc.util.Runtime;

import xtc.type.AST;

/**
 * Visitor to expand choices by inlining productions.  This visitor
 * inlines transient productions if they are essentially the only
 * element appearing in ordered choices' alternatives.
 *
 * <p />Note that this visitor requires that text-only productions
 * have been marked as such, that all {@link ValueElement value
 * elements} have been added to the grammar (notably, for generic
 * productions), and that the grammar's productions have been {@link
 * ReferenceCounter reference counted}.  Also note that this visitor
 * may result in new opportunities for the {@link
 * DeadProductionEliminator elimination of dead productions}.
 *
 * <p />This visitor assumes that the entire grammar is contained in a
 * single module.
 *
 * @author Robert Grimm
 * @version $Revision: 1.36 $
 */
public class ChoiceExpander extends GrammarVisitor {

  /**
   * The processing mode.  In regular mode, this visitor traverses a
   * grammar and inlines into choices; in remove values mode, it
   * removes value elements; in void values mode, it converts all
   * value elements into null value elements; and in text values mode,
   * it converts all value elements into text value elements.
   */
  public static enum Mode { 
    /** Traverse grammar and inline into choices. */ INLINE,
    /** Remove value elements. */ RM_VALUES,
    /** Convert value elements into null value elements. */ VOID_VALUES,
    /** Convert value elements into text value elements. */ TEXT_VALUES,
    /** Convert value elements into token value elements. */ TOKEN_VALUES
  }

  /** The flag for whether the grammar has the state attribute. */
  protected boolean hasState;

  /** The current processing mode. */
  protected Mode mode;

  /**
   * Create a new choice expander.
   *
   * @param runtime The runtime.
   * @param analyzer The analyzer utility.
   */
  public ChoiceExpander(Runtime runtime, Analyzer analyzer) {
    super(runtime, analyzer);
  }

  /**
   * Determine whether the specified alternative is a candidate for
   * replacement.  If the alternative is a candidate, this method
   * returns the nonterminal for inlining.  Otherwise, it returns
   * <code>null</code>.
   *
   * @param alternative The alternative.
   * @param top The flag for whether the alternative appears in a
   *   top-level choice.
   * @return The candidate nonterminal.
   */
  protected NonTerminal candidate(Sequence alternative, boolean top) {
    Element e = Analyzer.strip(alternative);

    if (e instanceof Binding) {
      // Generally, we accept a top-level alternative containing a
      // nonterminal that is bound to yyValue.
      Binding b = (Binding)e;

      if (top &&
          CodeGenerator.VALUE.equals(b.name) &&
          (b.element instanceof NonTerminal)) {
        return (NonTerminal)b.element;
      }

    } else if (AST.isVoid(analyzer.current().type) ||
               analyzer.current().getBooleanProperty(Properties.TEXT_ONLY) ||
               analyzer.current().getBooleanProperty(Properties.TOKEN)) {
      // However, for void, text-only, and token-level productions, we
      // also accept lone nonterminals or a nonterminal followed by a
      // value element.
      if (e instanceof NonTerminal) {
        return (NonTerminal)e;

      } else if (e instanceof Sequence) {
        Sequence s = (Sequence)e;

        if ((2 == s.size()) &&
            (s.get(0) instanceof NonTerminal) &&
            (s.get(1) instanceof ValueElement)) {
          return (NonTerminal)s.get(0);
        }
      }

    }

    return null;
  }

  /**
   * Record that the specified production has been inlined and, if
   * necessary, print a message to the console.
   *
   * @param p The production.
   */
  protected void inlined(Production p) {
    if (runtime.test("optionVerbose")) {
      System.err.println("[Inlining " + p.qName + " into " +
                         analyzer.current().qName + "]");
    }
  }

  /** Visit the specified grammar. */
  public Object visit(Module m) {
    // Initialize the per-grammar state.
    analyzer.register(this);
    analyzer.init(m);
    hasState = m.hasAttribute(Constants.ATT_STATEFUL.getName());
    for (Production p : m.productions) {
      mode = Mode.INLINE;

      if (runtime.test("optimizeChoices2") ||
          AST.isVoid(p.type) ||
          p.getBooleanProperty(Properties.TEXT_ONLY) ||
          p.getBooleanProperty(Properties.TOKEN)) {
        analyzer.process(p);
      }
    }

    return null;
  }

  /** Visit the specified ordered choice. */
  public Element visit(OrderedChoice c) {
    boolean top   = isTopLevel;
    isTopLevel    = false;
    boolean last  = isLastElement;
    isLastElement = false;

    for (int i=0; i<c.alternatives.size(); i++) {
      Sequence alternative = c.alternatives.get(i);

      // Only inline if we are in the correct mode.
      if (Mode.INLINE == mode) {
        NonTerminal date   = candidate(alternative, top);

        if (null != date) {
          Production p  = analyzer.lookup(date);
          MetaData   md = (MetaData)p.getProperty(Properties.META_DATA);

          // If a void, text-only, or token-level production is
          // transient/inline, is not explicit, does not reference
          // itself, and does not have the state or reset attribute,
          // inline a copy of it.  If the production is neither void,
          // text-only, nor token-level, it must have the inline
          // attribute and the choices2 optimization must be enabled.
          if ((((! p.isMemoized()) &&
                (! p.hasAttribute(Constants.ATT_NO_INLINE)) &&
                (! p.hasAttribute(Constants.ATT_EXPLICIT)) &&
                (AST.isVoid(p.type) ||
                 p.getBooleanProperty(Properties.TEXT_ONLY) ||
                 p.getBooleanProperty(Properties.TOKEN))) ||
               (p.hasAttribute(Constants.ATT_INLINE) &&
                runtime.test("optimizeChoices2"))) &&
              (0 == md.selfCount) &&
              (! (hasState &&
                  (p.hasAttribute(Constants.ATT_STATEFUL) ||
                   p.hasAttribute(Constants.ATT_RESETTING))))) {
            OrderedChoice choice = analyzer.copy(p.choice);

            // If the choice about to be inlined has only one
            // alternative (i.e., one alternative replaces another),
            // then we remember the source location of the original
            // alternative.
            if (1 == choice.alternatives.size()) {
              choice.alternatives.get(0).setLocation(alternative);
            }

            // Fix any value elements for the larger expression about
            // to be inlined.

            if ((! top) && (! last)) {
              // Embedded choices do not need any value elements.
              mode   = Mode.RM_VALUES;
              choice = (OrderedChoice)dispatch(choice);
              mode   = Mode.INLINE;

            } else if (AST.isVoid(analyzer.current().type)) {
              // Void productions need null value elements.
              mode   = Mode.VOID_VALUES;
              choice = (OrderedChoice)dispatch(choice);
              mode   = Mode.INLINE;

            } else if (analyzer.current().
                       getBooleanProperty(Properties.TEXT_ONLY) &&
                       (! top)) {
              // Embedded choices in text-only productions need text
              // value elements (but not string value elements).
              mode   = Mode.TEXT_VALUES;
              choice = (OrderedChoice)dispatch(choice);
              mode   = Mode.INLINE;
            } else if (analyzer.current().getBooleanProperty(Properties.TOKEN)) {
              // Token-level productions need token value elements.
              mode   = Mode.TOKEN_VALUES;
              choice = (OrderedChoice)dispatch(choice);
              mode   = Mode.INLINE;
            }
            c.alternatives.remove(i);
            c.alternatives.addAll(i, choice.alternatives);
            
            // Tell the user.
            inlined(p);
            
            // Process the inlined alternative(s).
            i--;
            continue;
          }
        }
      }

      // Process the unmodified alternative.
      if (top || last) isLastElement = true;
      c.alternatives.set(i, (Sequence)dispatch(alternative));
    }
    return c;
  }

  /** Visit the specified sequence. */
  public Element visit(Sequence s) {
    isTopLevel   = false;
    boolean last = isLastElement;
    int     size = s.size();

    // If we are in the remove value elements mode, check whether the
    // last element of the sequence is a value element and, if so,
    // remove it.
    if ((Mode.RM_VALUES == mode) &&
        (s.get(size-1) instanceof ValueElement)) {
      s.elements.remove(size-1);
      size--;
    }

    // Process the sequence's elements.
    for (int i=0; i<size; i++) {
      isLastElement = last && (i == size-1);
      s.elements.set(i, (Element)dispatch(s.get(i)));
    }
    isLastElement   = false;

    // Done.
    return s;
  }

  /** Visit the specified value element. */
  public Element visit(ValueElement v) {
    isTopLevel    = false;
    isLastElement = false;
    if (Mode.VOID_VALUES == mode) {
      return NullValue.VALUE;
    } else if (Mode.TEXT_VALUES == mode) {
      return StringValue.VALUE;
    } else if (Mode.TOKEN_VALUES == mode) {
      return TokenValue.VALUE;
    } else {
      return v;
    }
  }

}
