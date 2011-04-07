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
import xtc.util.Runtime;

import xtc.type.AST;

/**
 * Visitor to simplify a grammar.  This visitor folds nested choices
 * and sequences into the embedding choice or sequence, eliminates
 * choices and sequences with only one alternative or element (with
 * the exception of the top-level choice of a production), and reduces
 * repetitions and options to their simplest form.  It also reduces
 * trivial character classes and string literals to character
 * literals.  Note that the resulting grammar may violate the
 * requirements of {@link CodeGenerator code generation}.
 *
 * @author Robert Grimm
 * @version $Revision: 1.54 $
 */
public class Simplifier extends GrammarVisitor {

  /**
   * Create a new simplifier.
   *
   * @param runtime The runtime.
   * @param analyzer The analyzer utility.
   */
  public Simplifier(Runtime runtime, Analyzer analyzer) {
    super(runtime, analyzer);
  }

  /** Visit the specified ordered choice. */
  public Element visit(OrderedChoice c) {
    boolean top  = isTopLevel;
    isTopLevel   = false;

    // Process the alternatives.
    for (int i=0; i<c.alternatives.size(); i++) {
      needsSequence = true;
      Sequence s    = (Sequence)dispatch(c.alternatives.get(i));

      if ((1 == s.size()) && (s.get(0) instanceof OrderedChoice)) {
        // Fold in the nested choice.
        OrderedChoice c2 = (OrderedChoice)s.get(0);
        c.alternatives.remove(i);
        c.alternatives.addAll(i, c2.alternatives);
        i += (c2.alternatives.size()-1);

      } else {
        // Replace with the processed alternative.
        c.alternatives.set(i, s);
      }
    }
    needsSequence = false;

    // Eliminate nested choices with a single alternative.
    if ((! top) && (1 == c.alternatives.size())) {
      return c.alternatives.get(0);
    } else {
      return c;
    }
  }

  /** Visit the specified repetition. */
  public Element visit(Repetition r) {
    isTopLevel    = false;
    needsSequence = true;

    Element e     = (Element)dispatch(r.element);
    Element naked = Analyzer.strip(e);

    if (naked instanceof Repetition) {
      Repetition r2 = (Repetition)naked;
      r.once        = r.once && r2.once;
      r.element     = r2.element;
      return r;

    } else if (naked instanceof Option) {
      r.once    = false;
      r.element = ((Option)naked).element;
      return r;

    } else {
      r.element = e;
      return r;
    }
  }

  /** Visit the specified option. */
  public Element visit(Option o) {
    isTopLevel    = false;
    needsSequence = true;

    Element e     = (Element)dispatch(o.element);
    Element naked = Analyzer.strip(e);

    if (naked instanceof Option) {
      o.element = ((Option)naked).element;
      return o;

    } else if (naked instanceof Repetition) {
      ((Repetition)naked).once = false;
      // Transfer the location info.
      naked.setLocation(o);
      return naked;

    } else {
      o.element = e;
      return o;
    }
  }

  /** Visit the specified sequence. */
  public Element visit(Sequence s) {
    isTopLevel       = false;
    boolean preserve = needsSequence;
    needsSequence    = false;
    
    // Process the elements of the sequence first.
    for (int i=0; i<s.size(); i++) {
      Element e = (Element)dispatch(s.get(i));

      if (e instanceof Sequence) {
        // Fold in a nested sequence.
        Sequence s2 = (Sequence)e;
        s.elements.remove(i);
        s.elements.addAll(i, s2.elements);
        i += (s2.size()-1);

      } else {
        // Replace with the processed element.
        s.elements.set(i, e);
      }
    }

    // Now, see if we can create any exclusive character classes.
    int size = s.size();
    for (int i=0; i<size-1; i++) {
      Element e1 = s.get(i);
      Element e2 = s.get(i+1);

      if ((e1 instanceof NotFollowedBy) && (e2 instanceof AnyChar)) {
        e1 = ((NotFollowedBy)e1).element;

        if (e1 instanceof CharClass) {
          CharClass c = (CharClass)e1;

          if (! c.exclusive) {
            c.exclusive = true;
            s.elements.set(i, c);
            s.elements.remove(i+1);
            size--;
          }

        } else if (e1 instanceof CharLiteral) {
          CharClass c = new CharClass(true, new ArrayList<CharRange>(1));
          c.ranges.add(new CharRange(((CharLiteral)e1).c));
          s.elements.set(i, c);
          s.elements.remove(i+1);
          size--;
        }
      }
    }

    // Eliminate sequences with a single element.  However, if the
    // parent element requires a sequence, this sequence is not
    // eliminated.
    if ((1 == s.size()) && (! preserve)) {
      return s.get(0);
    } else {
      return s;
    }
  }

  /** Visit the specified voided element. */
  public Element visit(VoidedElement v) {
    isTopLevel    = false;
    needsSequence = false;

    v.element     = Analyzer.strip((Element)dispatch(v.element));

    // If the voided element is a void nonterminal, eliminate the
    // voided element wrapper.  Next, if the voided element is another
    // voided element, also eliminate the voided element wrapper.
    // Otherwise, if the element is a sequence, restore it to a choice
    // so that it can be lifted.
    if ((v.element instanceof NonTerminal) &&
        AST.isVoid(analyzer.lookup((NonTerminal)v.element).type)) {
      return v.element;

    } else if (v.element instanceof VoidedElement) {
      return v.element;

    } else if (v.element instanceof Sequence) {
      OrderedChoice c = new OrderedChoice(new ArrayList<Sequence>(1));
      c.alternatives.add((Sequence)v.element);
      c.setLocation(v.element);
      v.element       = c;
    }

    return v;
  }

  /** Visit the specified binding. */
  public Element visit(Binding b) {
    isTopLevel    = false;
    needsSequence = false;

    b.element     = Analyzer.strip((Element)dispatch(b.element));

    if (b.element instanceof Sequence) {
      // If the element is a sequence, restore it to an ordered choice
      // so that it will be lifted.
      OrderedChoice c = new OrderedChoice(new ArrayList<Sequence>(1));
      c.alternatives.add((Sequence)b.element);
      c.setLocation(b.element);
      b.element       = c;
    }

    return b;
  }

  /** Visit the specified string match. */
  public Element visit(StringMatch m) {
    isTopLevel    = false;
    needsSequence = false;

    m.element     = Analyzer.strip((Element)dispatch(m.element));

    if (m.element instanceof Sequence) {
      // If the element is a sequence, restore it to an ordered choice
      // so that it will be lifted.
      OrderedChoice c = new OrderedChoice(new ArrayList<Sequence>(1));
      c.alternatives.add((Sequence)m.element);
      c.setLocation(m.element);
      m.element       = c;

    } else if (((m.element instanceof Repetition) &&
                (! analyzer.current().isMemoized()) &&
                runtime.test("optimizeRepeated")) ||
               ((m.element instanceof Option) &&
                runtime.test("optimizeOptional"))) {
      // If the element is a repetition or option that won't be
      // desugared, restore it to an ordered choice so that it will be
      // lifted.
      OrderedChoice c = new OrderedChoice(new ArrayList<Sequence>(1));
      c.alternatives.add(new Sequence(m.element));
      c.setLocation(m.element);
      m.element       = c;
    }

    return m;
  }

  /** Visit the specified character class. */
  public Element visit(CharClass c) {
    isTopLevel    = false;
    needsSequence = false;

    // Turn non-exclusive character classes with a single range and
    // equal first and last characters into a character literal.
    if ((! c.exclusive) && (1 == c.ranges.size())) {
      CharRange r = c.ranges.get(0);
      if (r.first == r.last) {
        CharLiteral cl = new CharLiteral(r.first);
        // Transfer the location info.
        cl.setLocation(c);
        return cl;
      }
    }

    return c.normalize();
  }

}
