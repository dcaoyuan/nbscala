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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import xtc.tree.Attribute;
import xtc.tree.Visitor;

/**
 * Visitor to test productions and elements for equivalence.  This
 * visitor tests whether two productions or two elements are
 * structurally the same, modulo using different variable names and
 * having different nonterminals (for productions).  It should only be
 * invoked through the {@link #areEquivalent(Production,Production)}
 * and {@link #areEquivalent(Element,Element)} methods.
 *
 * @author Robert Grimm
 * @version $Revision: 1.36 $
 */
public class EquivalenceTester extends Visitor {

  /** The mapping between equivalent nonterminals. */
  protected Map<String, String> nts;

  /** The mapping between equivalent variables. */
  protected Map<String, String> vars;

  /** The element to compare to. */
  protected Element e2;

  /** Create a new equivalence tester. */
  public EquivalenceTester() {
    nts  = new HashMap<String, String>();
    vars = new HashMap<String, String>();
  }

  /**
   * Determine whether the specified productions are equivalent.
   *
   * @param p1 The first production.
   * @param p2 The second production.
   * @return <code>true</code> if the two productions are equivalent.
   */
  public boolean areEquivalent(Production p1, Production p2) {
    if (! Attribute.areEquivalent(p1.attributes, p2.attributes)) return false;
    if (null == p1.type) {
      if (! p1.dType.equals(p2.dType)) return false;
    } else {
      if (! p1.type.equals(p2.type)) return false;
    }
    
    nts.put(p2.name.name, p1.name.name);
    boolean result = areEquivalent(p1.choice, p2.choice);
    nts.remove(p2.name.name);
    
    return result;
  }
  
  /**
   * Determine whether the specified elements are equivalent.
   *
   * @param e1 The first element.
   * @param e2 The second element.
   * @return <code>true</code> if the two elements are equivalent.
   */
  public boolean areEquivalent(Element e1, Element e2) {
    if ((null == e1) || (null == e2)) return (e1 == e2);
    if (! e1.getClass().equals(e2.getClass())) return false;
    this.e2 = e2;
    return (Boolean)dispatch(e1);
  }

  /**
   * Determine whether the specified lists of bindings are equivalent.
   *
   * @param l1 The first list.
   * @param l2 The second list.
   * @return <code>true</code> if the two lists are equivalent.
   */
  protected boolean areEquivalent(List<Binding> l1, List<Binding> l2) {
    if (l1.size() != l2.size()) return false;

    Iterator<Binding> iter1 = l1.iterator();
    Iterator<Binding> iter2 = l2.iterator();

    while (iter1.hasNext()) {
      String var1 = iter1.next().name;
      String var2 = iter2.next().name;

      if ((! var1.equals(var2)) && (! var1.equals(vars.get(var2)))) {
        return false;
      }
    }

    return true;
  }
  
  /**
   * Determine whether the specified generic values are equivalent.
   *
   * @param v1 The first value.
   * @param v2 The second value.
   * @return <code>true</code> if the two values are equivalent.
   */
  protected boolean areEquivalent(GenericValue v1, GenericValue v2) {
    if ((! v1.name.equals(v2.name)) && (! v1.name.equals(nts.get(v2.name)))) {
      return false;
    }

    return areEquivalent(v1.children, v2.children);
  }

  /** Visit the specified ordered choice. */
  public Boolean visit(OrderedChoice c1) {
    OrderedChoice c2 = (OrderedChoice)e2;
    final int     l  = c1.alternatives.size();
    
    if (l != c2.alternatives.size()) {
      return Boolean.FALSE;
    } else {
      for (int i=0; i<l; i++) {
        if (! areEquivalent(c1.alternatives.get(i),
                            c2.alternatives.get(i))) {
          return Boolean.FALSE;
        }
      }
      
      return Boolean.TRUE;
    }
  }
  
  /** Visit the specified repetition. */
  public Boolean visit(Repetition r1) {
    Repetition r2 = (Repetition)e2;
    
    if (r1.once != r2.once) {
      return Boolean.FALSE;
    } else {
      return areEquivalent(r1.element, r2.element);
    }
  }
  
  /** Visit the specified sequence. */
  public Boolean visit(Sequence s1) {
    Sequence  s2 = (Sequence)e2;
    final int l  = s1.elements.size();
    
    if (l != s2.elements.size()) {
      return Boolean.FALSE;
    } else {
      for (int i=0; i<l; i++) {
        if (! areEquivalent(s1.elements.get(i),
                            s2.elements.get(i))) {
          return Boolean.FALSE;
        }
      }
      return Boolean.TRUE;
    }
  }
  
  /** Visit the specified binding. */
  public Boolean visit(Binding b1) {
    Binding b2 = (Binding)e2;
    
    if (b1.name.equals(b2.name)) {
      return areEquivalent(b1.element, b2.element);
      
    } else {
      String alt = vars.get(b2.name);
      
      if (null == alt) {
        // There is no mapping between b1 and b2. Try it.
        vars.put(b2.name, b1.name);
        boolean result = areEquivalent(b1.element, b2.element);
        vars.remove(b2.name);
        return result;
        
      } else if (! b1.name.equals(alt)) {
        // There is a mapping and it is different.
        return Boolean.FALSE;
        
      } else {
        // There is a mapping and it fits.
        return areEquivalent(b1.element, b2.element);
      }
    }
  }

  /** Visit the specified string match. */
  public Boolean visit(StringMatch m1) {
    StringMatch m2 = (StringMatch)e2;

    return m1.text.equals(m2.text) && areEquivalent(m1.element, m2.element);
  }

  /** Visit the specified nonterminal. */
  public Boolean visit(NonTerminal nt1) {
    NonTerminal nt2 = (NonTerminal)e2;
    
    return nt1.equals(nt2) || nt1.name.equals(nts.get(nt2.name));
  }

  /** Visit the specified character switch. */
  public Boolean visit(CharSwitch s1) {
    CharSwitch s2 = (CharSwitch)e2;
    final int  l  = s1.cases.size();

    if (l != s2.cases.size()) {
      return Boolean.FALSE;
    } else {
      for (int i=0; i<l; i++) {
        CharCase c1 = s1.cases.get(i);
        CharCase c2 = s2.cases.get(i);

        if (! areEquivalent(c1.klass, c2.klass)) {
          return Boolean.FALSE;
        } else if (! areEquivalent(c1.element, c2.element)) {
          return Boolean.FALSE;
        }
      }
    }

    if (null == s1.base) {
      return null == s2.base;
    } else {
      if (null == s2.base) {
        return Boolean.FALSE;
      } else {
        return areEquivalent(s1.base, s2.base);
      }
    }
  }
  
  /** Visit the specified parse tree node. */
  public Boolean visit(ParseTreeNode n1) {
    ParseTreeNode n2 = (ParseTreeNode)e2;

    if (! areEquivalent(n1.predecessors, n2.predecessors)) {
      return Boolean.FALSE;
    }

    if ((null == n1.node) != (null == n2.node)) return Boolean.FALSE;

    if ((null != n1.node) &&
        (! n1.node.name.equals(n2.node.name)) &&
        (! n1.node.name.equals(vars.get(n2.node.name)))) {
      return Boolean.FALSE;
    }

    return areEquivalent(n1.successors, n2.successors);
  }
  
  /** Visit the specified proper list value. */
  public Boolean visit(ProperListValue v1) {
    ProperListValue v2 = (ProperListValue)e2;

    if (! v1.type.equals(v2.type)) return Boolean.FALSE;
    if (! areEquivalent(v1.elements, v2.elements)) return Boolean.FALSE;
    if ((null == v1.tail) != (null == v2.tail)) return Boolean.FALSE;
    return ((null == v1.tail) ||
            v1.tail.name.equals(v2.tail.name) ||
            v1.tail.name.equals(vars.get(v2.tail.name)));
  }

  /** Visit the specified binding value. */
  public Boolean visit(BindingValue v1) {
    BindingValue v2 = (BindingValue)e2;

    return (v1.binding.name.equals(v2.binding.name) ||
            v1.binding.name.equals(vars.get(v2.binding.name)));
  }

  /** Visit the specified action base value. */
  public Boolean visit(ActionBaseValue v1) {
    ActionBaseValue v2 = (ActionBaseValue)e2;

    return ((v1.list.name.equals(v2.list.name) ||
             v1.list.name.equals(vars.get(v2.list.name))) &&
            (v1.seed.name.equals(v2.seed.name) ||
             v1.seed.name.equals(vars.get(v2.seed.name))));
  }

  /** Visit the specified generic node value. */
  public Boolean visit(GenericNodeValue v1) {
    GenericNodeValue v2 = (GenericNodeValue)e2;

    return areEquivalent(v1, v2) && areEquivalent(v1.formatting, v2.formatting);
  }


  /** Visit the specified generic action value. */
  public Boolean visit(GenericActionValue v1) {
    GenericActionValue v2  = (GenericActionValue)e2;

    if (! areEquivalent(v1, v2)) return Boolean.FALSE;

    if ((! v1.first.equals(v2.first)) &&
        (! v1.first.equals(vars.get(v2.first)))) {
      return Boolean.FALSE;
    }

    return areEquivalent(v1.formatting, v2.formatting);
  }
  
  /** Visit the specified generic recursion value. */
  public Boolean visit(GenericRecursionValue v1) {
    GenericRecursionValue v2  = (GenericRecursionValue)e2;

    if (! areEquivalent(v1, v2)) return Boolean.FALSE;

    if ((! v1.first.equals(v2.first)) &&
        (! v1.first.equals(vars.get(v2.first)))) {
      return Boolean.FALSE;
    }

    if (! areEquivalent(v1.formatting, v2.formatting)) {
      return Boolean.FALSE;
    }

    return (v1.list.name.equals(v2.list.name) ||
            v1.list.name.equals(vars.get(v2.list.name)));
  }

  /**
   * Visit the specified unary operator.  This method provides the
   * default implementation for options, predicates, voided elements,
   * and parser actions.
   */
  public Boolean visit(UnaryOperator op1) {
    UnaryOperator op2 = (UnaryOperator)e2;
    
    return areEquivalent(op1.element, op2.element);
  }
  
  /**
   * Visit the specified element.  This method provides the default
   * implementation for terminals, node markers, null literals,
   * actions, and null, string, text, empty list, generic string, and
   * generic text values.
   */
  public Boolean visit(Element e1) {
    return e1.equals(e2);
  }
  
}
