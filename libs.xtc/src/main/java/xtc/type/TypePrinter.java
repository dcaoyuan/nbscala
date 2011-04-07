/*
 * xtc - The eXTensible Compiler
 * Copyright (C) 2005-2007 Robert Grimm
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

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;

import xtc.tree.Attribute;
import xtc.tree.Printer;
import xtc.tree.Visitor;

/**
 * A visitor to print types.
 *
 * @author Robert Grimm
 * @version $Revision: 1.63 $
 */
public class TypePrinter extends Visitor {

  /** The printer utility. */
  protected final Printer printer;

  /** The set of visited, complex types. */
  protected final Map<Object,Object> visited;

  /** The flag for instantiated types. */
  protected boolean isInstantiated;

  /**
   * Create a new type printer.  Note that this constructor {@link
   * xtc.tree.Utility#register registers} the new type printer with
   * the specified printer.
   *
   * @param printer The printer utility.
   */
  public TypePrinter(Printer printer) {
    this.printer   = printer;
    this.visited   = new IdentityHashMap<Object,Object>();
    isInstantiated = false;
    printer.register(this);
  }

  /** Reset this type printer. */
  public void reset() {
    visited.clear();
  }

  // =========================================================================

  /**
   * Print the specified type's annotations.
   *
   * @param t The type.
   * @return <code>true</code> if anything was printed.
   */
  public boolean printAnnotations(Type t) {
    boolean printed = false;

    if (t.hasLocation(false)) {
      printer.p("line(").p(t.getLocation(false).line).p(") ");
      printed = true;
    }
    if (t.hasLanguage(false)) {
      printer.p("language(").p(t.getLanguage(false).toString()).p(") ");
      printed = true;
    }
    if (t.hasScope(false)) {
      printer.p("scope(").p(t.getScope(false)).p(") ");
      printed = true;
    }
    if (t.hasConstant(false)) {
      printer.p("value(").p(t.getConstant(false).getValue().toString()).p(") ");
      printed = true;
    }
    if (t.hasShape(false)) {
      printer.p("shape(").p(t.getShape().toString()).p(") ");
      printed = true;
    }
    if (t.hasAttributes()) {
      for (Attribute att : t.attributes()) {
        printer.p(att).p(' ');
        printed = true;
      }
    }

    return printed;
  }

  // =========================================================================

  /** Print the specified boolean type. */
  public void visit(BooleanT t) {
    printAnnotations(t);
    printer.p("boolean");
  }

  /** Print the specified error type. */
  public void visit(ErrorT t) {
    printAnnotations(t);
    printer.p("** error **");
  }

  /** Print the specified internal type. */
  public void visit(InternalT t) {
    printAnnotations(t);
    printer.p(t.getName());
  }

  /** Print the specified label type. */
  public void visit(LabelT t) {
    printAnnotations(t);
    printer.p("label(").p(t.getName()).p(')');
  }

  /** Print the specified number type. */
  public void visit(NumberT t) {
    printAnnotations(t);
    printer.p(t.toString());
  }

  /** Print the specified package type. */
  public void visit(PackageT t) {
    printAnnotations(t);
    printer.p("package(").p(t.getName()).p(')');
  }

  /** Print the specified type parameter. */
  public void visit(Parameter t) {
    printAnnotations(t);
    printer.p('<').p(t.getName()).p('>');
  }

  /** Print the specified unit type. */
  public void visit(UnitT t) {
    printAnnotations(t);
    printer.p(t.getName());
  }

  /** Print the specified void type. */
  public void visit(VoidT t) {
    printAnnotations(t);
    printer.p("void");
  }

  // =========================================================================

  /** Print the specified array type. */
  public void visit(ArrayT t) {
    printAnnotations(t);
    printer.p("array(").p(t.getType());
    if (t.isVarLength()) {
      printer.p(", *");
    } else if (t.hasLength()) {
      printer.p(", ").p(t.getLength());
    }
    printer.p(')');
  }

  /**
   * Print the interfaces, fields, and methods of the specified class
   * or interface type.
   *
   * @param t The class or interface type.
   */
  public void printBody(ClassOrInterfaceT t) {
    if (! t.getInterfaces().isEmpty()) {
      for (Iterator<Type> iter=t.getInterfaces().iterator(); iter.hasNext();) {
        Type type = iter.next();
        if (type.isAlias() && (null == type.toAlias().getType())) {
          printer.p(type);
        } else {
          printer.p(((InterfaceT)type.resolve()).getQName());
        }
        if (iter.hasNext()) {
          printer.p(", ");
        }
      }
    }

    if (visited.containsKey(t)) return;
    visited.put(t, Boolean.TRUE);

    if (t.getFields().isEmpty() && t.getMethods().isEmpty()) {
      printer.p(" {}");
    } else {
      printer.pln(" {").incr();
      for (Type field : t.getFields()) {
        printer.indent().p(field).pln(';');
      }
      for (Type method : t.getMethods()) {
        printer.indent().p(method).pln(';');
      }
      printer.decr().indent().p('}');
    }
  }

  /** Print the specified class type. */
  public void visit(ClassT t) {
    printer.p("class ").p(t.getQName());
    if (null != t.getParent()) {
      Type type = t.getParent();
      printer.p(" extends ");
      if (type.isAlias() && (null == type.toAlias().getType())) {
        printer.p(type);
      } else {
        printer.p(((ClassT)type.resolve()).getQName());
      }
    }
    if (! t.getInterfaces().isEmpty()) {
      printer.p(" implements ");
    }
    printBody(t);
  }

  /** Print the specified interface type. */
  public void visit(InterfaceT t) {
    printer.p("interface ").p(t.getQName());
    if (! t.getInterfaces().isEmpty()) {
      printer.p(" extends ");
    }
    printBody(t);
  }

  /**
   * Print the specified function or method type's signature.
   *
   * @param t The function or method type.
   */
  public void printSignature(FunctionOrMethodT t) {
    printer.p('(');
    for (Iterator<Type> iter = t.getParameters().iterator(); iter.hasNext(); ) {
      printer.p(iter.next());
      if (iter.hasNext() || t.isVarArgs()) {
        printer.p(", ");
      }
    }
    if (t.isVarArgs()) {
      printer.p("...");
    }
    printer.p(") -> ");
    if (t.getResult().resolve().isFunction()) {
      printer.p('(').p(t.getResult()).p(')');
    } else {
      printer.p(t.getResult());
    }
    if ((null != t.getExceptions()) && (! t.getExceptions().isEmpty())) {
      printer.p(" throws ");
      for (Iterator<Type> iter = t.getExceptions().iterator(); iter.hasNext();) {
        printer.p(iter.next());
        if (iter.hasNext()) printer.p(", ");
      }
    }
  }

  /** Print the specified function type. */
  public void visit(FunctionT t) {
    printAnnotations(t);
    printSignature(t);
  }

  /** Print the specified method type. */
  public void visit(MethodT t) {
    printAnnotations(t);
    printer.p(t.getName()).p(' ');
    printSignature(t);
  }

  /** Print the specified pointer type. */
  public void visit(PointerT t) {
    printAnnotations(t);
    printer.p("pointer(").p(t.getType()).p(')');
  }

  /**
   * Print the specified tagged type.
   *
   * @param kind The kind.
   * @param tag The tagged type.
   */
  public void printTagged(String kind, Tagged tag) {
    printer.p(kind).p(' ').p(tag.getName());
    if ((null != tag.getMembers()) && (! visited.containsKey(tag))) {
      visited.put(tag, Boolean.TRUE);

      if (tag.getMembers().isEmpty()) {
        printer.p(" {}");
      } else {
        printer.pln(" {").incr();
        for (Iterator<?> iter = tag.getMembers().iterator(); iter.hasNext(); ) {
          printer.indent().p((Type)iter.next());
          if ("enum".equals(kind)) {
            if (iter.hasNext()) {
              printer.pln(',');
            } else {
              printer.pln();
            }
          } else {
            printer.pln(';');
          }
        }
        printer.decr().indent().p('}');
      }
    }
  }

  /** Print the specified struct type. */
  public void visit(StructT t) {
    printAnnotations(t);
    printTagged("struct", t);
  }

  /** Print the specified union type. */
  public void visit(UnionT t) {
    printAnnotations(t);
    printTagged("union", t);
  }

  /** Print the specified tuple type. */
  public void visit(TupleT t) {
    printAnnotations(t);
    if (null == t.getName()) {
      printer.p("<anon>");
    } else {
      printer.p(t.getName());
    }
    printer.p('(');
    if (null == t.getTypes()) {
      printer.p("...");
    } else {
      for (Iterator<Type> iter = t.getTypes().iterator(); iter.hasNext(); ) {
        printer.p(iter.next());
        if (iter.hasNext()) printer.p(", ");
      }
    }
    printer.p(')');
  }

  /** Print the specified variant typee. */
  public void visit(VariantT t) {
    printAnnotations(t);
    if (t.isPolymorphic()) printer.p("polymorphic-");
    printer.p("variant ");
    if (null == t.getName()) {
      printer.p("<anonymous>");
    } else {
      printer.p(t.getName());
    }

    if (visited.containsKey(t)) return;
    visited.put(t, Boolean.TRUE);

    if (null == t.getTuples()) {
      printer.p(" { ... }");
    } else {
      printer.pln(" {").incr();
      for (Type var : t.getTuples()) {
        printer.indent().p(var).pln(';');
      }
      printer.decr().indent().p('}');
    }
  }

  // =========================================================================

  /** Print the specified type alias. */
  public void visit(AliasT t) {
    printAnnotations(t);
    printer.p("alias(").p(t.getName());
    if (null != t.getType()) {
      printer.p(", ").p(t.getType());
    }
    printer.p(')');
  }

  /** Print the specified annotated type. */
  public void visit(AnnotatedT t) {
    printAnnotations(t);
    printer.p(t.getType());
  }

  /** Print the specified enumerator. */
  public void visit(EnumeratorT t) {
    printAnnotations(t); // Prints value.
    printer.p("enumerator(").p(t.getType()).p(' ').p(t.getName()).p(')');
  }

  /** Print the specified enum type. */
  public void visit(EnumT t) {
    printAnnotations(t);
    printTagged("enum", t);
  }

  /** Print the specified instantiated type. */
  public void visit(InstantiatedT t) {
    printAnnotations(t);

    // Get the wrapped parameterized type.
    Iterator<Parameter> params = t.toParameterized().getParameters().iterator();
    Iterator<Type>      args   = t.getArguments().iterator();

    printer.p('<');
    while (params.hasNext()) {
      printer.p(params.next()).p(" = ").p(args.next());
      if (params.hasNext()) printer.p(", ");
    }
    printer.p('>');

    isInstantiated = true;
    printer.p(t.getType());
  }

  /** Print the specified parameterized type. */
  public void visit(ParameterizedT t) {
    printAnnotations(t);
    if (isInstantiated) {
      isInstantiated = false;
    } else {
      printer.p('<');
      for (Iterator<Parameter> iter = t.getParameters().iterator();
           iter.hasNext(); ) {
        printer.p(iter.next());
        if (iter.hasNext()) printer.p(", ");
      }
      printer.p("> ");
    }
    printer.p(t.getType());
  }

  /** Print the specified variable type. */
  public void visit(VariableT t) {
    printAnnotations(t);
    switch (t.getKind()) {
    case GLOBAL:
      printer.p("global"); break;
    case LOCAL:
      printer.p("local");  break;
    case PARAMETER:
      printer.p("param");  break;
    case FIELD:
      printer.p("field");  break;
    case BITFIELD:
      printer.p("bitfield"); break;
    }
    printer.p('(').p(t.getType()).p(", ");
    if (t.hasName()) {
      printer.p(t.getName());
    } else {
      printer.p("<anon>");
    }
    if (t.hasWidth()) {
      printer.p(", ").p(t.getWidth());
    }
    printer.p(')');
  }

}
