/*
 * xtc - The eXTensible Compiler
 * Copyright (C) 2007-2008 Robert Grimm
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
package xtc.type;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import xtc.Constants;

import xtc.tree.Attribute;
import xtc.tree.Printer;
import xtc.tree.VisitingException;
import xtc.tree.Visitor;

/**
 * A visitor to print types in C-like source form.  This visitor's
 * functionality is available through the {@link #print(Type)} and
 * {@link #print(Type,String)} methods.
 *
 * @author Robert Grimm
 * @version $Revision: 1.6 $
 */
public class SourcePrinter extends Visitor {

  /** The set of attributes to print literally. */
  private static Set<Attribute> LITERALS = new HashSet<Attribute>();

  static {
    LITERALS.add(Constants.ATT_ABSTRACT);
    LITERALS.add(Constants.ATT_CONSTANT); // Must be printed separately.
    LITERALS.add(Constants.ATT_INLINE);
    LITERALS.add(Constants.ATT_NATIVE);
    LITERALS.add(Constants.ATT_RESTRICT);
    LITERALS.add(Constants.ATT_STRICT_FP);
    LITERALS.add(Constants.ATT_SYNCHRONIZED);
    LITERALS.add(Constants.ATT_THREAD_LOCAL); // Must be printed separately.
    LITERALS.add(Constants.ATT_TRANSIENT);
    LITERALS.add(Constants.ATT_VOLATILE);
  }

  /** The printer utility. */
  protected final Printer printer;

  /** The base type. */
  protected Type base;

  /** The list of derived types, with the outer-most type first. */
  protected List<Type> derived;

  /** The optional variable name. */
  protected String name;

  /**
   * The flag for whether the next token needs to be preceded by a
   * space.
   */
  protected boolean needsSpace;

  /**
   * Create a new source printer.  Note that this visitor is
   * <em>not</em> registered with the printer utility.
   *
   * @param printer The printer utility.
   */
  public SourcePrinter(Printer printer) {
    this.printer = printer;
  }

  /**
   * Print the specified type.  If the specified type contains a
   * {@link VariableT}, this method prints a declaration for that
   * type's name.  Otherwise, it prints an abstract declaration.  Note
   * that the printed declaration is <em>not</em> followed by any
   * delimiter such as a semicolon.
   *
   * @param type The type.
   * @throws IllegalArgumentException Signals that the specified type
   *   contains an error type or more than one field, member,
   *   parameter, or variable type.
   */
  public void print(Type type) {
    print(type, null);
  }

  /**
   * Print the specified type and variable as a declaration.  Note
   * that the printed declaration is <em>not</em> followed by any
   * delimiter such as a semicolon.
   *
   * @param type The type.
   * @param variable The variable name.
   * @throws IllegalArgumentException Signals that the specified type
   *   contains a field, member, parameter, variable, or error type.
   */
  public void print(Type type, String variable) {
    // Save internal state.
    Type       savedBase       = base;
    List<Type> savedDerived    = derived;
    String     savedName       = name;
    boolean    savedNeedsSpace = needsSpace;

    base       = type;
    derived    = null;
    name       = variable;
    needsSpace = false;

    try {
      dispatch(type);
    } catch (VisitingException x) {
      // Unwrap runtime exceptions.
      if (x.getCause() instanceof RuntimeException) {
        throw (RuntimeException)x.getCause();
      } else {
        throw x;
      }
    } finally {
      // Restore internal state.
      base       = savedBase;
      derived    = savedDerived;
      name       = savedName;
      needsSpace = savedNeedsSpace;
    }
  }

  /**
   * Set the variable name.
   *
   * @param variable The variable name.
   * @throws IllegalArgumentException Signals a duplicate variable
   *   name.
   */
  protected void setVariable(String variable) {
    if (null != name) {
      throw new IllegalArgumentException("duplicate variable name");
    } else {
      name = variable;
    }
  }

  /**
   * Add the specified type to the list of derived types.
   *
   * @param type The type.
   */
  protected void addDerived(Type type) {
    if (null == derived) derived = new ArrayList<Type>();
    derived.add(type);
  }

  /**
   * Emit a space.  If the {@link #needsSpace} flag is set, this
   * method emits the space and clears the flag.
   */
  protected void space() {
    if (needsSpace) {
      printer.p(' ');
      needsSpace = false;
    }
  }

  /**
   * Emit any derived types.  This method also prints the variable
   * name, if it is available.  It must be invoked after printing a
   * non-derived type.
   */
  protected void printDerived() {
    /*
    if (null != derived) {
      for (int i=0; i<derived.size(); i++) {
        System.out.print(i);
        System.out.print(": ");
        System.out.println(derived.get(i).toString());
      }
    }
    */

    // Print any pointer types.
    if (null != derived) {
      boolean isPointer = true; // Track nested array or function types.
      for (int i=derived.size()-1; i>=0; i--) {
        Type t = derived.get(i);
        Type r = t.resolve();

        if (! r.isPointer()) {
          // Remember a nested array or function type.
          isPointer = false;
        } else {
          // Print a parenthesis for a nested array or function type.
          if (! isPointer) {
            space();
            printer.p('(');
            needsSpace = false;
            isPointer  = true;
          }

          // Print the pointer.
          printPointer(t);
        }
      }
    }

    // Print the variable name if available.
    if (null != name) {
      space();
      printer.p(name);
    }

    // Print any array and function types.
    if (null != derived) {
      final int size      = derived.size();
      boolean   isPointer = false;
      for (int i=0; i<size; i++) {
        Type t = derived.get(i);
        Type r = t.resolve();

        if (r.isPointer()) {
          // Print a parenthesis for a nested array or function type.
          if (! isPointer) {
            for (int j=i+1; j<size; j++) {
              if (! derived.get(j).resolve().isPointer()) {
                space();
                printer.p(')');
                break;
              }
            }
            isPointer = true;
          }

        } else if (r.isArray()) {
          isPointer = false;
          printArray(t);

        } else if (r.isFunction()) {
          isPointer = false;
          printFunction(t);
        }
      }
    }
  }

  /**
   * Print the specified pointer type.
   *
   * @param type The pointer type, which may be wrapped
   */
  protected void printPointer(Type type) {
    // Verify the pointer type.
    if (! type.resolve().isPointer()) {
      throw new IllegalStateException("printing non-pointer as pointer");
    }

    // Print the pointer.
    space();
    printer.p('*');

    // Print any qualifiers.
    if (hasAttributes(type)) {
      printer.p(' ');
      printAttributes(type);
    }
  }

  /**
   * Print the specified array type.
   *
   * @param type The array type, which may be wrapped.
   */
  protected void printArray(Type type) {
    // Resolve and verify the array type.
    Type r = type.resolve();
    if (! r.isArray()) {
      throw new IllegalStateException("printing non-array as array");
    }

    // Print the array type.
    ArrayT a = r.toArray();
    space();
    printer.p('[');

    // Print any qualifiers.
    if (hasAttributes(type)) {
      printAttributes(type);
    }

    // Print any length.
    if (a.isVarLength()) {
      space();
      printer.p('*');
    } else if (a.hasLength()) {
      space();
      printer.p(a.getLength());
    }

    printer.p(']');
  }

  /**
   * Print the specified function type.
   *
   * @param type The function type, which may be wrapped.
   */
  protected void printFunction(Type type) {
    // Resolve and verify the function type.
    Type r = type.resolve();
    if (! r.isFunction()) {
      throw new IllegalStateException("printing non-function as function");
    }

    // Print the function type.
    FunctionT f = (FunctionT)r;
    space();
    printer.p('(');

    if (f.hasAttribute(Constants.ATT_STYLE_NEW)) {
      List<Type> params = f.getParameters();
      if (0 == params.size()) {
        printer.p("void");
      } else {
        for (Iterator<Type> iter=params.iterator(); iter.hasNext(); ) {
          print(iter.next());
          if (iter.hasNext()) printer.p(", ");
        }
      }
    }

    printer.p(')');
  }

  /**
   * Determine whether the specified attribute is printable.
   *
   * @param att The attribute.
   * @return <code>true</code> if the attribute is printable.
   */
  public static boolean isPrintable(Attribute att) {
    if (LITERALS.contains(att)) return true;

    String name = att.getName();
    return (Constants.NAME_STORAGE.equals(name) ||
            (Constants.NAME_VISIBILITY.equals(name) &&
             (! Constants.ATT_PACKAGE_PRIVATE.equals(att))));
  }

  /**
   * Determine whether the specified type or any wrapped types have
   * any printable attributes.
   *
   * @param type The type.
   * @return <code>true</code> if the type or any wrapped types have
   *   any printable attributes.
   */
  public static boolean hasAttributes(Type type) {
    do {
      if (type.hasAttributes()) {
        for (Attribute att : type.attributes()) {
          if (isPrintable(att)) return true;
        }
      }

      if (! type.isWrapped()) return false;
      type = type.toWrapped().getType();
    } while (true);
  }

  /**
   * Print the attributes for the specified type and any wrapped
   * types.
   *
   * @param type The type.
   */
  protected void printAttributes(Type type) {
    do {
      if (type.hasAttributes()) {
        for (Attribute att : type.attributes()) dispatch(att);
      }

      if (! type.isWrapped()) return;
      type = type.toWrapped().getType();
    } while (true);
  }

  /** Print the specified attribute. */
  public void visit(Attribute att) {
    String name = att.getName();

    if (Constants.NAME_STORAGE.equals(name)) {
      space();
      printer.p(att.getValue().toString());
      needsSpace = true;

    } else if (Constants.NAME_VISIBILITY.equals(name)) {
      if (! Constants.ATT_PACKAGE_PRIVATE.equals(att)) {
        space();
        printer.p(att.getValue().toString());
        needsSpace = true;
      }

    } else if (Constants.ATT_CONSTANT.equals(att)) {
      space();
      printer.p("const");
      needsSpace = true;

    } else if (Constants.ATT_THREAD_LOCAL.equals(att)) {
      space();
      printer.p("__thread");
      needsSpace = true;

    } else if (LITERALS.contains(att)) {
      space();
      printer.p(name);
      needsSpace = true;
    }
  }

  /** Print the specified void type. */
  public void visit(VoidT t) {
    printAttributes(base);
    space();
    printer.p("void");
    needsSpace = true;
    printDerived();
  }

  /** Print the specified number type. */
  public void visit(NumberT t) {
    printAttributes(base);
    space();
    printer.p(t.toString());
    needsSpace = true;
    printDerived();
  }

  /** Print the specified struct type. */
  public void visit(StructT t) {
    if (t.isUnnamed()) throw new IllegalArgumentException("anonymous struct");
    printAttributes(base);
    space();
    printer.p("struct ").p(t.getName());
    needsSpace = true;
    printDerived();
  }

  /** Print the specified union type. */
  public void visit(UnionT t) {
    if (t.isUnnamed()) throw new IllegalArgumentException("anonymous union");
    printAttributes(base);
    space();
    printer.p("union ").p(t.getName());
    needsSpace = true;
    printDerived();
  }

  /** Print the specified enum type. */
  public void visit(EnumT t) {
    if (t.isUnnamed()) throw new IllegalArgumentException("anonymous enum");
    printAttributes(base);
    space();
    printer.p("enum " ).p(t.getName());
    needsSpace = true;
    printDerived();
  }

  /** Print the specified class or interface type. */
  public void visit(ClassOrInterfaceT t) {
    space();
    printer.p(t.getName());
    needsSpace = true;
    printDerived();
  }

  /** Print the specified alias type. */
  public void visit(AliasT t) {
    printAttributes(base);
    space();
    printer.p(t.getName());
    needsSpace = true;
    printDerived();
  }

  /** Print the specified internal type. */
  public void visit(InternalT t) {
    printAttributes(base);
    space();
    printer.p(t.getName());
    needsSpace = true;
    printDerived();
  }

  /** Print the specified pointer type. */
  public void visit(PointerT t) {
    addDerived(base);
    base = t.getType();
    dispatch(base);
  }

  /** Print the specified array type. */
  public void visit(ArrayT t) {
    addDerived(base);
    base = t.getType();
    dispatch(base);
  }
  
  /** Print the specified function type. */
  public void visit(FunctionT t) {
    addDerived(base);
    base = t.getResult();
    dispatch(base);
  }

  /** Print the specified variable type. */
  public void visit(VariableT t) {
    setVariable(t.getName());
    dispatch(t.getType());
  }

  /** Print the specified wrapped type. */
  public void visit(WrappedT t) {
    dispatch(t.getType());
  }

  /** Print the specified error type. */
  public void visit(ErrorT t) {
    throw new IllegalArgumentException("error type");
  }

}
