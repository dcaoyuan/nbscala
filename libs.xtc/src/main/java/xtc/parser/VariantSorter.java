/*
 * xtc - The eXTensible Compiler
 * Copyright (C) 2007 Robert Grimm
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
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import xtc.Constants;

import xtc.tree.Node;
import xtc.tree.Visitor;

import xtc.type.AST;
import xtc.type.ErrorT;
import xtc.type.TupleT;
import xtc.type.Type;
import xtc.type.VariantT;
import xtc.type.Wildcard;

import xtc.util.Runtime;
import xtc.util.Utilities;

/**
 * Visitor to infer a grammar's variants.
 *
 * <p />This visitor assumes that the entire grammar is contained in a
 * single module.  It also assumes that the grammar's real root has
 * been annotated and that directly left-recursive productions have
 * <em>not</em> been transformed into equivalent right-iterations.
 *
 * @author Robert Grimm
 * @version $Revision: 1.34 $
 */
public class VariantSorter extends Visitor {

  /** The debugging level. */
  private static final int DEBUG = 0;

  // =========================================================================

  /**
   * Visitor to register all generic node names with the type
   * operations class.
   */
  public class Registrar extends Visitor {

    /** The list of elements. */
    protected List<Element> elements;

    /** Create a new registrar. */
    public Registrar() {
      elements = new ArrayList<Element>();
    }

    /** Visit the specified grammar. */
    public void visit(Module m) {
      // Initialize the per-grammar state.
      analyzer.register(this);
      analyzer.init(m);

      elements.clear();

      // Iterate over generic productions.
      for (Production p : m.productions) {
        if (AST.isGenericNode(p.type)) analyzer.process(p);
      }
    }

    /** Visit the specified full production. */
    public void visit(FullProduction p) {
      dispatch(p.choice);
    }

    /** Visit the specified ordered choice. */
    public void visit(OrderedChoice c) {
      for (Sequence alt : c.alternatives) dispatch(alt);
    }

    /** Visit the specified sequence. */
    public void visit(Sequence s) {
      // Remember the current number of elements.
      final int base = elements.size();

      // Add this sequence's elements to the list of elements.
      for (Iterator<Element> iter = s.elements.iterator(); iter.hasNext(); ) {
        final Element e = iter.next();

        if ((! iter.hasNext()) && (e instanceof OrderedChoice)) {
          dispatch(e);
        } else {
          elements.add(e);
        }
      }

      // Actually process the elements.
      if (! s.hasTrailingChoice()) {
        NodeMarker mark = null;

        for (Element e : elements) {
          if (e instanceof NodeMarker) mark = (NodeMarker)e;
        }

        String name = analyzer.current().qName.name;
        if (null != mark) {
          name = Utilities.qualify(Utilities.getQualifier(name), mark.name);
        }

        ast.toTuple(name); // Called for side-effect, i.e., tuple creation.
      }

      // Remove any elements added by this method invocation.
      if (0 == base) {
        elements.clear();
      } else {
        elements.subList(base, elements.size()).clear();
      }
    }

  }

  // =========================================================================

  /** Visitor to determine a generic production's variant type. */
  public class Typer extends Visitor {

    /** The flag for creating a new variant. */
    protected boolean create;

    /** The current production. */
    protected FullProduction production;

    /** The list of elements. */
    protected List<Element> elements;

    /** The set of node names. */
    protected Set<String> names;

    /** The type. */
    protected Type type;

    /** Create a new typer. */
    public Typer() {
      elements = new ArrayList<Element>();
      names    = new HashSet<String>();
    }

    /**
     * Determine the specified generic production's variant type.
     *
     * <p />This method may only be invoked on a generic production
     * that does not yet have a variant type.  It tries to match the
     * production's generic node names to an existing variant type.
     * If such a type exists, it returns that type.  Next, if the
     * production does not create a generic node (even though it is
     * marked as generic), it returns the error type.  Next, if
     * <code>create</code> is <code>true</code>, it returns a new
     * variant type.  Otherwise, it returns the error type.
     *
     * @param p The generic production.
     * @param create The flag for creating a new variant.
     * @return The production's variant type or the error type.
     */
    public Type type(Production p, boolean create) {
      assert AST.isDynamicNode(p.type);
      assert AST.isGenericNode(p.type);
      this.create = create;
      return (Type)dispatch(p);
    }

    /** Visit the specified full production. */
    public Type visit(FullProduction p) {
      // Reset the traversal state.
      production = p;
      elements.clear();
      names.clear();

      // Process the choice.
      dispatch(p.choice);

      // Determine the type.

      // (1) If the production does not create any generic nodes, we
      // signal an error.
      if (names.isEmpty()) return ErrorT.TYPE;

      // (2) If the production creates generic nodes that already have
      // tuples belonging to the same variant, we return that variant.
      VariantT variant = null;
      boolean  isValid = false;

      for (String name : names) {
        if (ast.hasTuple(name)) {
          final List<VariantT> variants = ast.toVariants(ast.toTuple(name));

          if (1 == variants.size()) {
            if (null == variant) {
              variant = variants.get(0);
              isValid = true;
              continue;
            } else if (variant == variants.get(0)) {
              continue;
            }
          }
        }

        isValid = false;
        break;
      }

      if (isValid) return variant;

      // (3) If the production creates a generic node with the same
      // name as another generic production and that production
      // already has a variant type, we return the variant.
      if (1 == names.size()) {
        final String         name = names.iterator().next();
        final FullProduction p2   = analyzer.lookup(new NonTerminal(name));

        if (null != p2 &&
            AST.isGenericNode(p2.type) && AST.isStaticNode(p2.type)) {
          return p2.type;
        }
      }

      // (4) Return a new variant named after this production.
      return create ? ast.toVariant(p.qName.name, false) : ErrorT.TYPE;
    }

    /** Visit the specified ordered choice. */
    public void visit(OrderedChoice c) {
      for (Sequence alt : c.alternatives) dispatch(alt);
    }

    /** Visit the specified sequence. */
    public void visit(Sequence s) {
      // Remember the current number of elements.
      final int base = elements.size();

      // Add this sequence's elements to the list of elements.
      for (Iterator<Element> iter = s.elements.iterator(); iter.hasNext(); ) {
        final Element e = iter.next();

        if (! iter.hasNext() && (e instanceof OrderedChoice)) {
          dispatch(e);
        } else {
          elements.add(e);
        }
      }

      // Actually process the elements.
      if (! s.hasTrailingChoice()) {
        boolean    pass = false;
        NodeMarker mark = null;

        loop: for (Element e : elements) {
          switch (e.tag()) {
          case BINDING: {
            Binding b = (Binding)e;
            if (CodeGenerator.VALUE.equals(b.name)) {
              pass = true;
              break loop;
            }
          } break;

          case NODE_MARKER:
            mark = (NodeMarker)e;
            break;
          }
        }

        if (! pass) {
          String name = production.qName.name;
          if (null != mark) {
            name = Utilities.qualify(Utilities.getQualifier(name), mark.name);
          }
          if (! names.contains(name)) names.add(name);
        }
      }

      // Remove any elements added by this method invocation.
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

  /** The generic production typer. */
  protected final Typer gtyper;

  /** The set of AST nodes resulting in an error. */
  protected Map<Node, Node> malformed;

  /** The flag for whether the current mode is push or pull. */
  protected boolean isPushMode;

  /** The productions to be processed in push mode. */
  protected List<Production> productions;

  /** The flag for whether productions have changed in push mode. */
  protected boolean hasChanged;

  /**
   * The current types.  In push mode, the list contains exactly one
   * variant type, which is being pushed through productions.  In pull
   * mode, the list contains the types of all alternatives, which may
   * be variant types, type parameters, and error types and which are
   * unified in {@link #visit(FullProduction)}.
   */
  protected List<Type> types;

  /** The current production. */
  protected FullProduction production;

  /**
   * The flag for whether the current production is generic.  We need
   * to track this information in an explicit flag, because this
   * visitor processes choices and sequences that will be lifted into
   * their own productions.
   */
  protected boolean isGeneric;

  /** The list of elements representing the current alternative. */
  protected List<Element> elements;

  /**
   * Create a new variant sorter.
   *
   * @param runtime The runtime.
   * @param analyzer The analyzer utility.
   * @param ast The type operations.
   */
  public VariantSorter(Runtime runtime, Analyzer analyzer, AST ast) {
    this.runtime  = runtime;
    this.analyzer = analyzer;
    this.ast      = ast;
    gtyper        = new Typer();
    malformed     = new IdentityHashMap<Node, Node>();
    productions   = new ArrayList<Production>();
    types         = new ArrayList<Type>();
    elements      = new ArrayList<Element>();
  }

  /**
   * Merge the two variants.  This method merges the second variant
   * into the first variant, which must be polymorphic.  If the second
   * variant also is polymorphic, it simply adds the second variant's
   * tuples to the first variant.  Otherwise, it tries to create a new
   * tuple with the second variant's original name and then adds that
   * tuple to the first variant.
   *
   * @param v1 The first variant.
   * @param v2 The second variant.
   * @param p The production for error reporting.
   * @return The first variant or the error type in case of errors.
   */
  protected Type merge(VariantT v1, VariantT v2, Production p) {
    assert v1.isPolymorphic();

    if (ast.unify(v1, v2, true).isError()) {
      runtime.error("production's alternatives have distinct variants", p);
      runtime.errConsole().loc(p).pln(": error: but include same generic node");
      runtime.errConsole().loc(p).p(": error: 1st type is '");
      ast.print(v1, runtime.errConsole(), false, true, null);
      runtime.errConsole().pln("'");
      runtime.errConsole().loc(p).p(": error: 2nd type is '");
      ast.print(v2, runtime.errConsole(), false, true, null);
      runtime.errConsole().pln("'").flush();

      return ErrorT.TYPE;
    }

    Type result = v1;
    if (v2.isPolymorphic()) {
      for (TupleT tuple : v2.getTuples()) ast.add(tuple, v1);
    } else {
      ast.add(ast.toTuple(v2), v1);
    }

    return result;
  }

  /**
   * Set the specified production's type to the specified type.  This
   * method preserves the original type's generic attribute, unless
   * the specified type is the error type.
   *
   * @param p The production.
   * @param t The type.
   */
  protected void setType(Production p, Type t) {
    if (t.isError()) {
      p.type = t;
    } else if (AST.isGenericNode(p.type)) {
      p.type = t.annotate().attribute(Constants.ATT_GENERIC);
    } else {
      p.type = t;
    }

    if (2 <= DEBUG) {
      runtime.console().p(p.qName.name).p(" : ");
      ast.print(p.type, runtime.console(), false, true, null);
      runtime.console().pln().flush();
    }
  }

  /**
   * Push and pull any variant types.
   *
   * @param m The grammar.
   */
  protected void pushPull(Module m) {
    boolean first = true;

    do {
      // Push.
      isPushMode = true;
      hasChanged = first;
      if (first) first = false;
      while (! productions.isEmpty()) {
        Production p = productions.remove(0);
        types.clear();
        types.add(p.type.resolve());
        analyzer.process(p);
      }

      if (hasChanged) {
        // Try to match not-yet-marked generic productions that do not
        // pass the value through to existing variant types.
        for (Production p : m.productions) {
          if (AST.isDynamicNode(p.type) &&
              AST.isGenericNode(p.type) &&
              ! analyzer.setsValue(p.choice, false)) {
            final Type t = gtyper.type(p, false);
            if (! t.isError()) setType(p, t);
          }
        }

        // Pull.
        isPushMode = false;
        hasChanged = false;
        for (Production p : m.productions) {
          if (AST.isDynamicNode(p.type)) {
            analyzer.process(p);
          }
        }
      }
    } while (! productions.isEmpty());
  }
  
  /** Visit the specified grammar. */
  public void visit(Module m) {
    // Register the names for generic nodes first.
    new Registrar().dispatch(m);

    // Initialize the per-grammar state.
    analyzer.register(this);
    analyzer.init(m);

    malformed.clear();
    productions.clear();
    elements.clear();

    // Start with the grammar's root.
    if (! m.hasProperty(Properties.ROOT)) {
      runtime.error("grammar without distinct root", m);
      return;
    } else {
      Production p =
        analyzer.lookup((NonTerminal)m.getProperty(Properties.ROOT));

      if (! AST.isNode(p.type)) {
        runtime.error("grammar's root production does not return a node", p);
        return;
      }

      p.attributes.remove(Constants.ATT_VARIANT);
      setType(p, ast.toVariant(p.qName.name, false));
      productions.add(p);
    }

    // Then process all productions explicitly marked as variant.
    for (Production p : m.productions) {
      if (p.hasAttribute(Constants.ATT_VARIANT)) {
        setType(p, ast.toVariant(p.qName.name, false));
        productions.add(p);
      }
    }
    
    // Push/pull the grammar's variants.
    pushPull(m);

    // Process any remaining generic productions.
    for (Production p : m.productions) {
      if (AST.isDynamicNode(p.type) && AST.isGenericNode(p.type)) {
        Type t = gtyper.type(p, true);

        if (! t.isError()) {
          setType(p, t);
          productions.add(p);
        }
      }
    }
    pushPull(m);

    // Process any remaining non-generic productions.
    for (Production p : m.productions) {
      if (AST.isDynamicNode(p.type)) {
        analyzer.notWorkingOnAny();
        if (! analyzer.consumesInput(p.qName)) {
          p.type = AST.NULL_NODE;
        } else {
          runtime.error("unable to determine static type", p);
        }
      }
    }

    // Print the results of the variant inference in debugging mode.
    if (1 <= DEBUG) {
      if (2 <= DEBUG) runtime.console().pln();

      // Print all variant types.
      Set<String> printed = new HashSet<String>();

      for (Production p : m.productions) {
        if (AST.isStaticNode(p.type)) {
          VariantT variant = p.type.resolve().toVariant();

          if (! printed.contains(variant.getName())) {
            printed.add(variant.getName());
            ast.print(variant, runtime.console(), true, true, null);
            runtime.console().pln();
          }
        }
      }

      // Print all productions that do not have a statically typed
      // node.
      for (Production p : m.productions) {
        if (p.type.isError()) {
          runtime.console().p(p.qName.name).pln(" : *error*");
        } else if (AST.isDynamicNode(p.type)) {
          runtime.console().p(p.qName.name).pln(" : *undetermined*");
        }
      }
      runtime.console().flush();
    }
  }

  /** Visit the specified production. */
  public void visit(FullProduction p) {
    if (2 <= DEBUG) {
      if (isPushMode) {
        runtime.console().p("push ");
      } else {
        runtime.console().p("pull ");
      }
      runtime.console().pln(p.qName.name).flush();
    }

    // Set up the traversal state.
    production = p;
    isGeneric  = AST.isGenericNode(p.type);

    // Update the production's type in push mode.
    if (isPushMode) {
      if (AST.isDynamicNode(p.type)) {
        hasChanged = true; // We need another pull pass.
        setType(p, types.get(0));
      }
    } else {
      types.clear(); // We have not yet seen any alternatives.
    }
    
    // Remember that we are working on the production.
    analyzer.workingOn(p.qName);
    
    // Preprocess directly left-recursive generic productions.
    if (isGeneric && DirectLeftRecurser.isTransformable(p)) {
      for (Sequence s : p.choice.alternatives) {
        if (! DirectLeftRecurser.isRecursive(s, p)) {
          Binding b = analyzer.bind(s.elements);
          if (null != b) b.name = CodeGenerator.VALUE;
        }
      }
    }

    // Actually process the production's choice.
    dispatch(p.choice);

    // Update the production's type in pull mode by unifying the
    // alternatives' types.
    if (! isPushMode) {
      Type    result   = Wildcard.TYPE;
      boolean seenWild = false; // Flag for having seen a wildcard.
      boolean seenPoly = false; // Flag for having seen a polymorphic variant.

      loop: for (Type t : types) {
        switch (t.tag()) {
        case ERROR:
          result = ErrorT.TYPE;
          break loop;

        case WILDCARD:
          seenWild = true;
          if (seenPoly) {
            runtime.error("production requires polymorphic variant", p);
            runtime.errConsole().loc(p).
              pln(": error: but has alternatives without static type").flush();
            result = ErrorT.TYPE;
            break loop;
          }
          break;

        case VARIANT:
          if (seenPoly) {
            result = merge(result.toVariant(), t.toVariant(), p);
            if (result.isError()) break loop;

          } else if (result.isWildcard()) {
            result = t;

          } else if (! result.equals(t)) {
            if (isGeneric) {
              runtime.error("variant '" + result.toVariant().getName() + "' " +
                            "overlaps with '" + t.toVariant().getName() + "'",p);
              result = ErrorT.TYPE;
              break loop;

            } else if (seenWild) {
              runtime.error("production requires polymorphic variant", p);
              runtime.errConsole().loc(p).
                pln(": error: but has alternatives without static type").flush();
              result = ErrorT.TYPE;
              break loop;

            } else {
              VariantT v = result.toVariant();
              result     = merge(ast.toVariant(p.qName.name, true), v, p);
              if (result.isError()) break loop;
              result     = merge(result.toVariant(), t.toVariant(), p);
              if (result.isError()) break loop;
              seenPoly   = true;
            }
          }
          break;

        default:
          throw new AssertionError("Unrecognized type " + t);
        }
      }

      // If we have a meaningful result, update the production's type.
      if (! result.isWildcard()) {
        setType(p, result);

        final Type r = result.resolve();
        if (r.isVariant() && ! r.toVariant().isPolymorphic()) {
          // Push the production's variant type.
          productions.add(p);
        }
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
    
    // Add this sequence's elements to the list of elements.
    for (Iterator<Element> iter = s.elements.iterator(); iter.hasNext(); ) {
      final Element e = iter.next();
      
      if ((! iter.hasNext()) && (e instanceof OrderedChoice)) {
        dispatch(e);
      } else {
        elements.add(e);
      }
    }
    
    // Actually process the elements.
    if (! s.hasTrailingChoice()) {
      if (isGeneric) {
        // The production is generic.  If the alternative passes the
        // value through, determine the last such element.  Otherwise,
        // determine the last node marker (if any).
        Element    pass = null;
        NodeMarker mark = null;

        for (Element e : elements) {
          switch (e.tag()) {
          case BINDING: {
            Binding b = (Binding)e;
            if (CodeGenerator.VALUE.equals(b.name)) pass = b.element;
          } break;
            
          case NODE_MARKER:
            mark = (NodeMarker)e;
            break;
          }
        }

        if (null != pass) {
          // Recurse on the passed through element.
          recurse(pass);

        } else if (isPushMode) {
          // Process the constructor name.
          String name = production.qName.name;
          if (null != mark) {
            name = Utilities.qualify(Utilities.getQualifier(name), mark.name);
          }

          final boolean        hasTuple = ast.hasTuple(name);
          final TupleT         tuple    = ast.toTuple(name);
          final List<VariantT> variants = ast.toVariants(tuple);

          if (! hasTuple || variants.isEmpty()) {
            ast.add(tuple, types.get(0).toVariant());
          } else if (! variants.contains(types.get(0))) {
            runtime.error("tuple '" + name + "' should appear in variant '" +
                          types.get(0).toVariant().getName() + "'", production);
            runtime.errConsole().loc(production).p(": error: but already ").
              p("appears in variant '").p(variants.get(0).getName()).pln("'").
              flush();
            variants.add(types.get(0).toVariant());
          }
        }

      } else {
        // The production passes the value through.
        Element value = analyzer.getValue(elements, true);

        if (null != value) {
          recurse(value);
        }
      }
    }
    
    // Remove any elements added by this method invocation.
    if (0 == base) {
      elements.clear();
    } else {
      elements.subList(base, elements.size()).clear();
    }
  }

  /**
   * Recurse on the specified element.  If the element is an
   * optionally bound nonterminal, this method processes the
   * corresponding production.  If the element is an optionally bound
   * sequence or choice, this method processes the sequence or choice.
   * Otherwise, it reports an error condition and returns.
   *
   * @param e The element.
   */
  protected void recurse(Element e) {
    // Strip any bindings and options.
    e = Analyzer.strip(e);

    while (true) {
      final Element start = e;
      if (e instanceof Binding) e = Analyzer.strip(((Binding)e).element);
      if (e instanceof Option)  e = Analyzer.strip(((Option)e).element);
      if (start == e) break;
    }
    
    // Save the traversal state.
    List<Type>     savedTypes      = types;
    FullProduction savedProduction = production;
    boolean        savedIsGeneric  = isGeneric;
    List<Element>  savedElements   = elements;
    
    // Determine the AST node to recurse on.
    switch (e.tag()) {
    case NONTERMINAL: {
      // Get the production.
      FullProduction p = analyzer.lookup((NonTerminal)e);
      
      // Avoid infinite recursions.
      if (analyzer.isBeingWorkedOn(p.qName)) {
        if (! isPushMode) types.add(Wildcard.TYPE);
        return;
      }

      // Check for previous errors and existing variants.
      if (p.type.isError()) {
        if (! isPushMode) types.add(ErrorT.TYPE);
        return;

      } else if (AST.isStaticNode(p.type)) {
        if (isPushMode) {
          // Make sure the variants are consistent.
          if (! types.get(0).equals(p.type.resolve())) {
            if (! malformed.containsKey(p)) {
              runtime.error("variant '" + types.get(0).toVariant().getName() +
                            "' overlaps with '" + 
                            p.type.resolve().toVariant().getName() + "'", p);
              malformed.put(p, p);
            }
            return;
          }
        }
        if (! isPushMode) types.add(p.type.resolve());
        return;
      }
      
      // The production must not be void.  Neither should it have a
      // string or token value.
      assert ! AST.isVoid(p.type);

      if (AST.isString(p.type)) {
        if (! malformed.containsKey(p)) {
          runtime.error("variant type for production with string value", p);
          malformed.put(p, p);
        }
        if (! isPushMode) types.add(ErrorT.TYPE);
        return;

      } else if (AST.isToken(p.type)) {
        if (! malformed.containsKey(p)) {
          runtime.error("variant type for production with token value", p);
          malformed.put(p, p);
        }
        if (! isPushMode) types.add(ErrorT.TYPE);
        return;
      }

      // Actually recurse.
      if (! isPushMode) types = new ArrayList<Type>();
      elements = new ArrayList<Element>();
      dispatch(p);
      if ((! isPushMode) && (! AST.isDynamicNode(p.type))) {
        // Remember the result.
        savedTypes.add(p.type.resolve());
      }
    } break;
      
    case SEQUENCE:
    case CHOICE:
      // Since the element will be lifted, its production cannot be
      // generic.
      isGeneric = false;
      elements  = new ArrayList<Element>();
      dispatch(e);
      break;

    case NULL:
      // A null literal can assume any type.
      if (! isPushMode) types.add(Wildcard.TYPE);
      return;
      
    default:
      if (! malformed.containsKey(e)) {
        runtime.error("variant type for invalid element", e);
        malformed.put(e, e);
      }
      if (! isPushMode) types.add(ErrorT.TYPE);
      return;
    }
    
    // Restore the traversal state.
    types      = savedTypes;
    production = savedProduction;
    isGeneric  = savedIsGeneric;
    elements   = savedElements;
  }
  
}
