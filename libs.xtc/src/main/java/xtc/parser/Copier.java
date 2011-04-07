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
import java.util.List;

import xtc.tree.Attribute;
import xtc.tree.Comment;
import xtc.tree.Node;
import xtc.tree.Visitor;

/**
 * Visitor to copy grammar nodes.  This visitor makes deep copies of
 * grammars, modules, productions, and elements.  Note that when
 * copying elements, this visitor must be invoked through the {@link
 * #copy(Element)} method.  Further note that, if the element to be
 * copied contains a generic node value or a generic recursion value,
 * the element must also contain all bindings referenced by that value
 * element.  Otherwise, an <code>IllegalArgumentException</code> is
 * signalled.
 *
 * @author Robert Grimm
 * @version $Revision: 1.51 $
 */
public class Copier extends Visitor {

  /** The list of source bindings. */
  protected List<Binding> source;

  /** The list of target bindings. */
  protected List<Binding> target;

  /** Create a new copier. */
  public Copier() {
    source = new ArrayList<Binding>();
    target = new ArrayList<Binding>();
  }

  /**
   * Match the specified binding with its copy.
   *
   * @param b The binding.
   * @return The corresponding copy.
   * @throws IllegalArgumentException Signals that the specified
   *   binding has no copy.
   */
  protected Binding match(Binding b) {
    final int size = source.size();

    int idx = -1;
    for (int i=0; i<size; i++) {
      if (b == source.get(i)) {
        idx = i;
        break;
      }
    }

    if (-1 == idx) {
      throw new IllegalArgumentException("Copying element without binding for " +
                                         b.name);
    }

    return target.get(idx);
  }

  /**
   * Patch the specified bindings.  This method replaces the source
   * bindings in the specified list of bindings with the corresponding
   * copies, thus ensuring that the list contains previously copied
   * bindings.
   *
   * @param bindings The bindings to patch.
   */
  protected void patch(List<Binding> bindings) {
    final int size = bindings.size();
    for (int i=0; i<size; i++) {
      bindings.set(i, match(bindings.get(i)));
    }
  }

  /**
   * Copy the specified element.
   *
   * @param e The element.
   * @return A deep copy.
   * @throws IllegalArgumentException
   *   Signals that the specified element is incomplete.
   */
  @SuppressWarnings("unchecked")
  public <T extends Element> T copy(T e) {
    // Clear the lists of bindings.
    source.clear();
    target.clear();

    return (T)dispatch(e);
  }

  /** Copy the specified grammar. */
  public Grammar visit(Grammar g) {
    Grammar copy = new Grammar(new ArrayList<Module>(g.modules.size()));
    copy.setLocation(g);
    for (Module m : g.modules) {
      copy.modules.add((Module)dispatch(m));
    }
    return copy;
  }

  /** Copy the specified module. */
  public Module visit(Module m) {
    Module copy            = new Module();
    copy.setLocation(m);
    copy.documentation     = (Comment)dispatch(m.documentation);
    copy.name              = m.name;
    copy.parameters        = (ModuleList)dispatch(m.parameters);
    if (null != m.dependencies) {
      copy.dependencies    =
        new ArrayList<ModuleDependency>(m.dependencies.size());
      for (ModuleDependency dep : m.dependencies) {
        copy.dependencies.add((ModuleDependency)dispatch(dep));
      }
    }
    copy.modification      = m.modification;
    copy.header            = (Action)dispatch(m.header);
    copy.body              = (Action)dispatch(m.body);
    copy.footer            = (Action)dispatch(m.footer);
    if (null != m.attributes) {
      copy.attributes      = new ArrayList<Attribute>(m.attributes);
    }
    copy.productions       = new ArrayList<Production>(m.productions.size());
    for (Production p : m.productions) {
      copy.productions.add((Production)dispatch(p));
    }
    return copy;
  }

  /** Copy the specified comment. */
  public Comment visit(Comment c) {
    Node    node  = (Node)dispatch(c.getNode());
    Comment copy  = new Comment(c.kind, new ArrayList<String>(c.text), node);
    copy.setLocation(c);

    return copy;
  }

  /** Copy the specified module import declaration. */
  public ModuleImport visit(ModuleImport i) {
    ModuleImport copy =
      new ModuleImport(i.module,
                       (ModuleList)dispatch(i.arguments),
                       i.target);
    copy.setLocation(i);
    return copy;
  }

  /** Copy the specified module instantiation declaration. */
  public ModuleInstantiation visit(ModuleInstantiation i) {
    ModuleInstantiation copy =
      new ModuleInstantiation(i.module,
                              (ModuleList)dispatch(i.arguments),
                              i.target);
    copy.setLocation(i);
    return copy;
  }

  /** Copy the specified module modification. */
  public ModuleModification visit(ModuleModification m) {
    ModuleModification copy =
      new ModuleModification(m.module,
                             (ModuleList)dispatch(m.arguments),
                             m.target);
    copy.setLocation(m);
    return copy;
  }

  /** Copy the specified module list. */
  public ModuleList visit(ModuleList l) {
    ModuleList copy = new ModuleList(new ArrayList<ModuleName>(l.names));
    copy.setLocation(l);
    return copy;
  }

  /** Copy the specified full production. */
  public FullProduction visit(FullProduction p) {
    FullProduction copy =
      new FullProduction(null, p.type, p.name, p.qName, copy(p.choice));
    copy.setLocation(p);
    if (null != p.attributes) {
      copy.attributes   = new ArrayList<Attribute>(p.attributes);
    }
    copy.dType          = p.dType;
    return copy;
  }

  /** Copy the specified alternative addition. */
  public AlternativeAddition visit(AlternativeAddition p) {
    AlternativeAddition copy =
      new AlternativeAddition(p.dType, p.name, copy(p.choice),
                              p.sequence, p.isBefore);
    copy.setLocation(p);
    copy.type                = p.type;
    copy.qName               = p.qName;
    return copy;
  }

  /** Copy the specified alternative removal. */
  public AlternativeRemoval visit(AlternativeRemoval p) {
    AlternativeRemoval copy =
      new AlternativeRemoval(p.dType, p.name,
                             new ArrayList<SequenceName>(p.sequences));
    copy.setLocation(p);
    copy.type               = p.type;
    copy.qName              = p.qName;
    return copy;
  }

  /** Copy the specified production override. */
  public ProductionOverride visit(ProductionOverride p) {
    ProductionOverride copy =
      new ProductionOverride(p.dType, p.name, copy(p.choice), p.isComplete);
    copy.setLocation(p);
    if (null != p.attributes) {
      copy.attributes       = new ArrayList<Attribute>(p.attributes);
    }
    copy.type               = p.type;
    copy.qName              = p.qName;
    return copy;
  }

  /** Copy the specified ordered choice. */
  public OrderedChoice visit(OrderedChoice c) {
    final int     length = c.alternatives.size();
    OrderedChoice copy   = new OrderedChoice(new ArrayList<Sequence>(length));
    copy.setLocation(c);
    for (Sequence alt : c.alternatives) {
      copy.alternatives.add((Sequence)dispatch(alt));
    }
    return copy;
  }

  /** Copy the specified repetition. */
  public Repetition visit(Repetition r) {
    Repetition copy = new Repetition(r.once, (Element)dispatch(r.element));
    copy.setLocation(r);
    return copy;
  }

  /** Copy the specified option. */
  public Option visit(Option o) {
    Option copy = new Option((Element)dispatch(o.element));
    copy.setLocation(o);
    return copy;
  }

  /** Copy the specified sequence. */
  public Sequence visit(Sequence s) {
    final int size = s.size();
    Sequence  copy = new Sequence(s.name, new ArrayList<Element>(size));
    copy.setLocation(s);
    for (int i=0; i<size; i++) {
      copy.add((Element)dispatch(s.get(i)));
    }
    return copy;
  }

  /** Copy the specified followed-by predicate. */
  public FollowedBy visit(FollowedBy p) {
    FollowedBy copy = new FollowedBy((Element)dispatch(p.element));
    copy.setLocation(p);
    return copy;
  }

  /** Copy the specified not-followed-by predicate. */
  public NotFollowedBy visit(NotFollowedBy p) {
    NotFollowedBy copy = new NotFollowedBy((Element)dispatch(p.element));
    copy.setLocation(p);
    return copy;
  }

  /** Copy the specified semantic predicate. */
  public SemanticPredicate visit(SemanticPredicate p) {
    SemanticPredicate copy = new SemanticPredicate((Action)dispatch(p.element));
    copy.setLocation(p);
    return copy;
  }

  /** Copy the specified voided element. */
  public VoidedElement visit(VoidedElement v) {
    VoidedElement copy = new VoidedElement((Element)dispatch(v.element));
    copy.setLocation(v);
    return copy;
  }

  /** Copy the specified binding. */
  public Binding visit(Binding b) {
    Binding copy = new Binding(b.name, (Element)dispatch(b.element));
    copy.setLocation(b);
    source.add(b);
    target.add(copy);

    return copy;
  }

  /** Copy the specified string match. */
  public StringMatch visit(StringMatch m) {
    StringMatch copy = new StringMatch(m.text, (Element)dispatch(m.element));
    copy.setLocation(copy);

    return copy;
  }

  /** Copy the specified character class. */
  public CharClass visit(CharClass c) {
    CharClass copy =
      new CharClass(c.exclusive, new ArrayList<CharRange>(c.ranges.size()));
    copy.setLocation(c);
    copy.ranges.addAll(c.ranges);
    return copy;
  }

  /** Copy the specified character case. */
  public CharCase visit(CharCase c) {
    CharCase copy = new CharCase((CharClass)dispatch(c.klass),
                                 (Element)dispatch(c.element));
    copy.setLocation(c);
    return copy;
  }

  /** Copy the specified character switch. */
  public CharSwitch visit(CharSwitch s) {
    final int  length = s.cases.size();
    CharSwitch copy   = new CharSwitch(new ArrayList<CharCase>(length));
    copy.setLocation(s);
    for (CharCase kase : s.cases) {
      copy.cases.add((CharCase)dispatch(kase));
    }
    copy.base         = (Element)dispatch(s.base);
    return copy;
  }

  /** Copy the specified action. */
  public Action visit(Action a) {
    Action copy = new Action(new ArrayList<String>(a.code),
                             new ArrayList<Integer>(a.indent));
    copy.setLocation(a);
    return copy;
  }

  /** Copy the specified parser action. */
  public ParserAction visit(ParserAction pa) {
    ParserAction copy = new ParserAction((Action)dispatch(pa.element));
    copy.setLocation(pa);
    return copy;
  }

  /** Copy the specified parse tree node. */
  public ParseTreeNode visit(ParseTreeNode n) {
    ParseTreeNode copy =
      new ParseTreeNode(new ArrayList<Binding>(n.predecessors), null,
                        new ArrayList<Binding>(n.successors));
    copy.setLocation(n);
    patch(copy.predecessors);
    if (null != n.node) copy.node = match(n.node);
    patch(copy.successors);

    // Done.
    return copy;
  }

  /** Copy the specified binding value. */
  public BindingValue visit(BindingValue v) {
    BindingValue copy = new BindingValue(match(v.binding));
    copy.setLocation(v);

    // Done.
    return copy;
  }

  /** Copy the specified proper list value. */
  public ProperListValue visit(ProperListValue v) {
    ProperListValue copy =
      new ProperListValue(v.type, new ArrayList<Binding>(v.elements), null);
    copy.setLocation(v);
    patch(copy.elements);
    if (null != v.tail) copy.tail = match(v.tail);

    // Done.
    return copy;
  }

  /** Copy the specified action base value. */
  public ActionBaseValue visit(ActionBaseValue v) {
    ActionBaseValue copy = new ActionBaseValue(match(v.list), match(v.seed));
    copy.setLocation(v);

    // Done.
    return copy;
  }

  /** Copy the specified generic node value. */
  public GenericNodeValue visit(GenericNodeValue v) {
    GenericNodeValue copy =
      new GenericNodeValue(v.name, new ArrayList<Binding>(v.children),
                           new ArrayList<Binding>(v.formatting));
    copy.setLocation(v);
    patch(copy.children);
    patch(copy.formatting);

    // Done.
    return copy;
  }

  /** Copy the specified generic action value. */
  public GenericActionValue visit(GenericActionValue v) {
    GenericActionValue copy = new
      GenericActionValue(v.name, v.first, new ArrayList<Binding>(v.children),
                         new ArrayList<Binding>(v.formatting));
    copy.setLocation(v);
    patch(copy.children);
    patch(copy.formatting);

    // Done.
    return copy;
  }

  /** Copy the specified generic recursion value. */
  public GenericRecursionValue visit(GenericRecursionValue v) {
    GenericRecursionValue copy = new
      GenericRecursionValue(v.name, v.first, new ArrayList<Binding>(v.children),
                            new ArrayList<Binding>(v.formatting),
                            match(v.list));
    copy.setLocation(v);
    patch(copy.children);
    patch(copy.formatting);

    // Done.
    return copy;
  }

  /**
   * Visit the specified element.  This method provides the default
   * implementation for nonterminals, terminals (besides character
   * classes and switches), node markers, null literals, and value
   * elements (besides properlists, generic node, generic action, and
   * generic recursion values), which are immutable and not containers
   * for other elements.
   */
  public Element visit(Element e) {
    return e;
  }

}
