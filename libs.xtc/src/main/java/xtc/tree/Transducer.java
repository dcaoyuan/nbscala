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
package xtc.tree;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import xtc.util.Utilities;

/** 
 * Visitor to convert trees of generic nodes into methods that
 * programmatically create the trees.  Trees may contain generic nodes
 * that represent pattern variables.  During transduction, pattern
 * variable are replaced with method arguments of the same names.  In
 * general, a variable represents a single child of a newly created
 * generic node.  However, if the variable's type is
 * <code>List&lt;T&gt;</code>, all of the list's elements are directly
 * added as children to the newly created generic node.  Any node
 * representing a pattern variable must have exactly one child that is
 * a string representing the variable's name.
 *
 * @author Robert Grimm
 * @version $Revision: 1.5 $
 */
public class Transducer extends Visitor {

  /** The printer. */
  protected final Printer printer;

  /** The mapping from pattern variable names to their types. */
  protected final Map<String, String> variables;

  /** The temporary variable count. */
  protected int varcount;

  /**
   * Create a new transducer.  The newly created transducer treats all
   * trees as literals, without pattern variables.
   *
   * @param printer The printer.
   */
  public Transducer(Printer printer) {
    this(printer, new HashMap<String, String>());
  }

  /**
   * Create a new transducer.  The mapping from pattern variable names
   * to types may be empty (but not <code>null</code>) to indicate
   * that all tree nodes are literal, i.e., do not contain any "holes"
   * that are filled during creation.
   *
   * @param printer The printer.
   * @param variables The mapping from node names representing pattern
   *   variables to their values' types.
   */
  public Transducer(Printer printer, Map<String, String> variables) {
    this.printer   = printer;
    this.variables = variables;
  }

  /**
   * Determine whether the specified node represents a pattern variable.
   *
   * @param n The node.
   * @return <code>true</code> if it represents a pattern variable.
   */
  public boolean isPatternVariable(Node n) {
    n = n.strip();
    return null == n ? false :
      n.isGeneric() && variables.containsKey(n.getName());
  }

  /**
   * Ensure that the specified node is a well-formed pattern variable.
   *
   * @param n The node.
   * @return The node as a pattern variable.
   * @throws IllegalArgumentException Signals that the specified node
   *   is not a pattern variable or is a malformed pattern variable.
   */
  public GNode toPatternVariable(Node n) {
    n = n.strip();
    if ((null == n) ||
        (! n.isGeneric()) ||
        (! variables.containsKey(n.getName()))) {
      throw new IllegalArgumentException("Not a pattern variable: " + n);
    } else if ((1 != n.size()) || (! Token.test(n.get(0)))) {
      throw new IllegalArgumentException("Malformed pattern variable: " + n);
    }
    return (GNode)n;
  }

  /**
   * Get the pattern variable's name.
   *
   * @param n The node.
   * @return The corresponding variable name.
   * @throws IllegalArgumentException Signals that the node does not
   *   represent a pattern variable or is malformed.
   */
  public String getVariableName(Node n) {
    return Token.cast(toPatternVariable(n).get(0));
  }

  /**
   * Get the pattern variable's type.
   *
   * @param n The node.
   * @return The corresponding variable type.
   * @throws IllegalArgumentException Signals that the node does not
   *   represent a pattern variable or is malformed.
   */
  public String getVariableType(Node n) {
    return variables.get(toPatternVariable(n).getName());
  }

  /**
   * Determine whether the specified type is a list type.
   *
   * @param t The type.
   * @return <code>true</code> if the specified type is a list type.
   */
  public boolean isListType(String t) {
    return t.equals("List") || t.startsWith("List<");
  }

  /**
   * Convert the specified object to a literal.  If the specified
   * variable name is <code>null</code> and the object is a node, the
   * object must be a pattern variable.
   *
   * @param o The object.
   * @param var The variable name for nodes that are not pattern
   *   variables.
   * @return The corresponding literal.
   * @throws IllegalArgumentException Signals that the specified object
   *   is not recognized.
   */
  public String toLiteral(Object o, String var) {
    if (null == o) {
      return "null";
    } else if (Token.test(o)) {
      return '"'+Utilities.escape(Token.cast(o), Utilities.JAVA_ESCAPES)+'"';
    } else if (o instanceof Node) {
      return null == var ? getVariableName((Node)o) : var;
    } else if (o instanceof Boolean) {
      return ((Boolean)o).booleanValue() ? "true" : "false";
    } else if (o instanceof Double) {
      return ((Double)o).doubleValue() + "D";
    } else if (o instanceof Float) {
      return ((Float)o).floatValue() + "F";
    } else if (o instanceof Long) {
      return ((Long)o).longValue() + "L";
    } else if (o instanceof Integer) {
      return o.toString();
    } else if (o instanceof Short) {
      return "Short.valueOf(" + ((Short)o).shortValue() + ')';
    } else if (o instanceof Byte) {
      return "Byte.valueOf(" + ((Byte)o).byteValue() + ')';
    } else if (o instanceof Character) {
      return "'" + ((Character)o).charValue() + "'";
    } else {
      throw new IllegalArgumentException("Unrecognized value: " + o);
    }
  }

  /**
   * Process the specified node.
   *
   * @param method The method name.
   * @param n The node.
   * @throws IllegalArgumentException Signals that the specified is
   *   not a generic node or that it is a pattern variable.
   */
  public void process(String method, Node n) {
    // Perform consistency checks: neither node nor method may be null.
    if (null == method) {
      throw new NullPointerException("Null method name");
    } else if (null == n) {
      throw new NullPointerException("Null node");
    }
    
    n = n.strip();
    if (! GNode.test(n)) {
      throw new IllegalArgumentException("Not an (annotated) generic node: "+n);
    } else if (isPatternVariable(n)) {
      throw new IllegalArgumentException("Pattern variable: " + n);
    }

    // Reset this visitor's state.
    varcount = 0;

    // Declare the list of pattern holes and their types.
    final List<String> holes = new ArrayList<String>();
    final List<String> types = new ArrayList<String>();

    // Fill in the list of pattern holes and their types.
    new Visitor() {
      @SuppressWarnings("unused")
      public void visit(GNode n) {
        if (isPatternVariable(n)) {
          String name = getVariableName(n);
          String type = getVariableType(n);
          int    idx  = holes.indexOf(name);
          if (-1 == idx) {
            holes.add(name);
            types.add(type);
          } else if (! types.get(idx).equals(type)) {
            types.set(idx, "Object");
          }
        } else {
          for (Object o : n) {
            if (o instanceof Node) {
              dispatch((Node)o);
            }
          }
        }
      }
    }.dispatch(n);

    // Emit the method header.
    String desc = Utilities.split(n.getName(), ' ');
    printer.indent().pln("/**");
    printer.indent().p(" * Create ").p(Utilities.toArticle(desc)).p(' ').
      p(desc).pln('.');
    printer.indent().pln(" *");
    for (String h : holes) {
      printer.indent().p(" * @param ").p(h).p(" The ").p(h).pln('.');
    }
    printer.indent().pln(" * @return The generic node.");
    printer.indent().pln(" */");
    printer.indent().p("public Node ").p(method).p('(');
    final int align        = printer.column();
    boolean   first        = true;
    Iterator<String> iterH = holes.iterator();
    Iterator<String> iterT = types.iterator();
    while (iterH.hasNext()) {
      if (! first) {
        printer.buffer();
      }

      printer.p(iterT.next()).p(' ').p(iterH.next());

      if (iterH.hasNext()) {
        printer.p(", ");
      }

      if (first) {
        first = false;
      } else {
        printer.fit(align);
      }
    }
    printer.pln(") {").incr();

    // Emit the method body.
    String result = (String)dispatch(n);
    if (null == result) result = getVariableName(n);
    printer.indent().p("return ").p(result).pln(';');

    // Close the method body.
    printer.decr().indent().pln('}');
  }

  /** Visit the specified generic node. */
  public String visit(GNode n) {
    // Pattern variables do not require any code.
    if (isPatternVariable(n)) return null;

    // Iterate over the children, recursively processing any nodes.
    // Also, determine whether any of the pattern variables used for
    // this node has a list type.
    final int     size    = n.size();
    List<String>  vars    = null;
    boolean       hasList = false;

    if (0 < size) vars = new ArrayList<String>(size);

    for (Object o : n) {
      if (o instanceof Node) {
        Node child = (Node)o;

        if (isPatternVariable(child)) {
          vars.add(null);
          if (isListType(getVariableType(child))) {
            hasList = true;
          }

        } else {
          vars.add((String)dispatch(child));
        }

      } else {
        vars.add(null);
      }
    }

    // Emit code for declaring and creating the node.
    String result = "v$" + (++varcount);

    printer.indent().p("Node ").p(result).p(" = GNode.create(\"").
      p(n.getName()).p("\"");

    // Emit the code for the node's children.
    if (0 == size) {
      // A node with no children.
      printer.pln(", false);");

    } else if (hasList) {
      // A node with a dynamic number of children.
      printer.p(", ");

      // Emit the size expression.
      int     scount   = 0;
      boolean seenList = false;
      
      for (int i=0; i<size; i++) {
        Object o = n.get(i);

        if (o instanceof Node) {
          Node child = (Node)o;

          if (isPatternVariable(child) && isListType(getVariableType(child))) {

            if (seenList) {
              printer.p(" + ");
            } else {
              seenList = true;
            }
            printer.p(getVariableName(child)).p(".size()");

          } else {
            scount++;
          }

        } else {
          scount++;
        }
      }

      if (0 < scount) {
        printer.p(" + ").p(scount);
      }
      printer.pln(").").indentMore();

      // Emit the code to add the children.
      for (int i=0; i<size; i++) {
        Object o = n.get(i);

        printer.buffer();
        if (o instanceof Node) {
          Node child = (Node)o;

          if (isPatternVariable(child) && isListType(getVariableType(child))) {
            printer.p("addAll(");
          } else {
            printer.p("add(");
          }
        } else {
          printer.p("add(");
        }
        printer.p(toLiteral(o, vars.get(i))).p(')');
        if (i<size-1) {
          printer.p('.');
        } else {
          printer.p(';');
        }
        printer.fitMore();
      }
      printer.pln();

    } else {
      // A node with a static number of children.
      printer.p(", ");
      if (GNode.MAX_FIXED < size) printer.p(size).pln(").").indentMore();

      for (int i=0; i<size; i++) {
        printer.buffer();
        if (GNode.MAX_FIXED < size) printer.p("add(");
        printer.p(toLiteral(n.get(i), vars.get(i)));
        if (GNode.MAX_FIXED < size) printer.p(')');

        if (i<size-1) {
          if (GNode.MAX_FIXED < size) {
            printer.p('.');
          } else {
            printer.p(", ");
          }
        } else {
          if (GNode.MAX_FIXED < size) {
            printer.p(';');
          } else {
            printer.p(");");
          }
        }
        printer.fitMore();
      }

      printer.pln();
    }

    return result;
  }

}
