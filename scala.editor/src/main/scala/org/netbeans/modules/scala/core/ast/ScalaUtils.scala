/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2009 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2009 Sun Microsystems, Inc.
 */

package org.netbeans.modules.scala.core.ast

import org.netbeans.modules.csl.api.{ElementKind, Modifier}
import org.netbeans.api.language.util.ast.AstItem
import org.netbeans.modules.csl.api.HtmlFormatter

import org.netbeans.modules.scala.core.ScalaGlobal

import scala.tools.nsc.symtab.Flags

trait ScalaUtils {self: ScalaGlobal =>
  
  object ScalaUtil {
    def getModifiers(symbol: Symbol): java.util.Set[Modifier] = {
      val modifiers = new java.util.HashSet[Modifier]

      if (symbol hasFlag Flags.PROTECTED) {
        modifiers.add(Modifier.PROTECTED)
      } else if (symbol hasFlag Flags.PRIVATE) {
        modifiers.add(Modifier.PRIVATE)
      } else {
        modifiers.add(Modifier.PUBLIC)
      }

      if (symbol hasFlag Flags.MUTABLE)    modifiers.add(Modifier.STATIC) // to use STATIC icon only
      if (symbol.isDeprecated) modifiers.add(Modifier.DEPRECATED)

      modifiers
    }

    def getKind(sym: Symbol): ElementKind = {
      try {
        if (sym.isPackage) {
          ElementKind.PACKAGE
        } else if (sym.isClass) {
          ElementKind.CLASS
        } else if (sym.isType) {
          ElementKind.CLASS
        } else if (sym.isTrait) {
          ElementKind.CLASS
        } else if (sym.isModule) {
          ElementKind.MODULE
        } else if (sym.isConstructor) {
          ElementKind.CONSTRUCTOR
        } else if (sym.isConstant) {
          ElementKind.CONSTANT
        } else if (sym.isValue) {
          ElementKind.FIELD
        } else if (sym.isVariable) {
          ElementKind.VARIABLE
        } else if (sym.isMethod) {
          ElementKind.METHOD
        } else if (sym.isValueParameter) {
          ElementKind.PARAMETER
        } else if (sym.isTypeParameter) {
          ElementKind.CLASS
        } else {
          ElementKind.OTHER
        }
      } catch {case t: Throwable =>
          ElementKind.OTHER
          // java.lang.Error: no-symbol does not have owner
          //      at scala.tools.nsc.symtab.Symbols$NoSymbol$.owner(Symbols.scala:1609)
          //      at scala.tools.nsc.symtab.Symbols$Symbol.isLocal(Symbols.scala:346)
      }
    }

    def symbolQualifiedName(symbol: Symbol): String = {
      symbolQualifiedName(symbol, true)
    }

    /**
     * Due to the ugly implementation of scala's Symbols.scala, Symbol#fullName()
     * may cause:
     * java.lang.Error: no-symbol does not have owner
     *        at scala.tools.nsc.symtab.Symbols$NoSymbol$.owner(Symbols.scala:1565)
     * We should bypass it via symbolQualifiedName
     */
    def symbolQualifiedName(symbol: Symbol, forScala: Boolean): String = {
      if (symbol.isError) {
        "<error>"
      } else if (symbol == NoSymbol) {
        "<none>"
      } else {
        var paths: List[String] = Nil
        var owner = symbol.owner
        // remove type parameter part at the beginnig, for example: scala.Array[T0] will be: scala.Array.T0
        if (!symbol.isTypeParameterOrSkolem) {
          paths = symbol.nameString :: paths
        }
        while (!owner.nameString.equals("<none>") && !owner.nameString.equals("<root>")) {
          if (!symbol.isTypeParameterOrSkolem) {
            paths = owner.nameString :: paths
          }
          owner = owner.owner
        }

        paths.reverse
        val sb = paths.mkString(".")

        if (sb.length == 0) {
          if (symbol.isPackage) {
            ""
          } else {
            if (forScala) symbol.nameString else "Object" // it maybe a TypeParameter likes: T0
          }
        } else sb
      }
    }

    def typeQualifiedName(tpe: Type, forScala: Boolean): String = {
      symbolQualifiedName(tpe.typeSymbol, forScala);
    }

    def isInherited(template: Symbol, member: Symbol): Boolean = {
      !symbolQualifiedName(template).equals(symbolQualifiedName(member.enclClass))
    }

    def typeToString(tpe: Type): String = {
      if (tpe == null) return ""
      val str = try {
        tpe.toString
      } catch {
        case ex: java.lang.AssertionError => ScalaGlobal.resetLate(self, ex); null // ignore assert ex from scala
        case ex: Throwable => ScalaGlobal.resetLate(self, ex); null
      }

      if (str != null) str else tpe.termSymbol.nameString
    }

    def htmlFormat(symbol: Symbol, fm: HtmlFormatter): Unit = {
      symbol match {
        case sym if sym.isPackage | sym.isClass | sym.isModule => fm.appendText(sym.nameString)
        case sym if sym.isConstructor =>
          fm.appendText(sym.owner.nameString)
          htmlTypeName(sym, fm)
        case sym if sym.isMethod =>
          fm.appendText(sym.nameString)
          htmlTypeName(sym, fm)
        case sym =>
          fm.appendText(sym.nameString)
          fm.appendText(": ")
          htmlTypeName(sym, fm)
      }
    }

    def tryTpe(sym: Symbol): Type = {
      try {
        sym.tpe
      } catch {case ex => ScalaGlobal.resetLate(self, ex); null}
    }

    def htmlTypeName(sym: Symbol, fm: HtmlFormatter): Unit = {      
      htmlTypeName(tryTpe(sym), fm)
    }

    def htmlTypeName(tpe: Type, fm: HtmlFormatter): Unit = {
      if (tpe == null) return
      tpe match {
        case ErrorType => fm.appendText("<error>")
          // internal: error
        case WildcardType => "_"
          // internal: unknown
        case NoType => fm.appendText("<notype>")
        case NoPrefix => fm.appendText("<noprefix>")
        case ThisType(sym) => 
          fm.appendText(sym.nameString)
          fm.appendText(".this.type")
          // sym.this.type
        case SingleType(pre, sym) =>
          fm.appendText(sym.nameString)
          fm.appendText(".type")
          // pre.sym.type
        case ConstantType(value) => 
          
          // int(2)
        case TypeRef(pre, sym, args) =>
          fm.appendText(sym.nameString)
          if (!args.isEmpty) {
            fm.appendText("[")
            val itr = args.iterator
            while (itr.hasNext) {
              htmlTypeName(itr.next, fm)
              if (itr.hasNext) {
                fm.appendText(", ")
              }
            }
            fm.appendText("]")
          }
          // pre.sym[targs]
        case RefinedType(parents, defs) =>
          // parent1 with ... with parentn { defs }
        case AnnotatedType(annots, tp, selfsym) => htmlTypeName(tp, fm)
          // tp @annots

          // the following are non-value types; you cannot write them down in Scala source.

        case TypeBounds(lo, hi) =>
          fm.appendText(">: ")
          htmlTypeName(lo, fm)
          fm.appendText(" <: ")
          htmlTypeName(hi, fm)
          // >: lo <: hi
        case ClassInfoType(parents, defs, clazz) => 
          htmlTypeName(clazz.tpe, fm)
          // same as RefinedType except as body of class
        case MethodType(paramtypes, result) =>
          if (!paramtypes.isEmpty) {
            fm.appendText("(")
            val itr = paramtypes.iterator
            while (itr.hasNext) {
              fm.`type`(true)
              htmlTypeName(itr.next, fm)
              fm.`type`(false)
              if (itr.hasNext) {
                fm.appendText(", ")
              }
            }
            fm.appendText(")")
          }
          fm.appendText(": ")
          htmlTypeName(result, fm)
          // (paramtypes)result
        case PolyType(tparams, result) =>
          if (!tparams.isEmpty) {
            fm.appendText("[")
            val itr = tparams.iterator
            while (itr.hasNext) {
              fm.`type`(true)
              htmlTypeName(itr.next, fm)
              fm.`type`(false)
              if (itr.hasNext) {
                fm.appendText(", ")
              }
            }
            fm.appendText("]")
          }
          fm.appendText(": ")
          htmlTypeName(result, fm)
          // [tparams]result where result is a MethodType or ClassInfoType
          // or
          // []T  for a eval-by-name type
        case ExistentialType(tparams, result) => 
          fm.appendText("ExistantialType")
          // exists[tparams]result

          // the last five types are not used after phase `typer'.

          //case OverloadedType(pre, tparams, alts) => "Overlaod"
          // all alternatives of an overloaded ident
        case AntiPolyType(pre: Type, targs) => 
          fm.appendText("AntiPolyType")
        case TypeVar(_, _) => tpe.safeToString
          // a type variable
        case DeBruijnIndex(level, index) => 
          fm.appendText("DeBruijnIndex")
        case _ => 
          fm.appendText(tpe.getClass.getSimpleName)
      }
    }

    /**
     * String representation of symbol's definition
     * from scala.tools.nsc.symtab.Symbols
     */
    def htmlDef(sym: Symbol, fm: HtmlFormatter): Unit = {
      // * no-symbol does not have owner
      if (sym == NoSymbol) {
        fm.appendText("<no-symbol>")
        return
      }

      completeIfWithLazyType(sym)
      
      val flags = if (sym.owner.isRefinementClass) {
        sym.flags & Flags.ExplicitFlags & ~Flags.OVERRIDE
      } else sym.flags & Flags.ExplicitFlags

      compose(List(Flags.flagsToString(flags),
                   sym.keyString,
                   sym.varianceString + sym.nameString), fm)
      
      sym match {
        case _ if sym.isPackage | sym.isClass | sym.isTrait =>
          if (sym.hasRawInfo) htmlTypeInfo(sym.rawInfo, fm)
        case _ if sym.isModule => // object, the `rawInfo` is `TypeRef`, we should dive into `sym.moduleClass`
          if (sym.hasRawInfo) htmlTypeInfo(sym.moduleClass.rawInfo, fm)
        case _ if sym.isConstructor =>
          if (sym.hasRawInfo) fm.appendText(sym.infoString(sym.rawInfo))
        case _ if sym.isMethod =>
          if (sym.hasRawInfo) fm.appendText(sym.infoString(sym.rawInfo))
        case _ => 
          if (sym.hasRawInfo) fm.appendText(sym.infoString(sym.rawInfo))
      }      
    }

    /** Concatenate strings separated by spaces */
    private def compose(ss: List[String], fm: HtmlFormatter): Unit = {
      val itr = ss.filter("" !=).iterator
      while (itr.hasNext) {
        fm.appendText(itr.next)
        if (itr.hasNext) fm.appendText(" ")
      }
    }

    def htmlTypeInfo(tpe: Type, fm: HtmlFormatter): Unit = {
      if (tpe == null) return
      tpe match {
        case ErrorType => fm.appendText("<error>")
          // internal: error
        case WildcardType => "_"
          // internal: unknown
        case NoType => fm.appendText("<notype>")
        case NoPrefix => fm.appendText("<noprefix>")
        case ThisType(sym) =>
          fm.appendText(sym.nameString)
          fm.appendText(".this.type")
          // sym.this.type
        case SingleType(pre, sym) =>
          fm.appendText(sym.nameString)
          fm.appendText(".type")
          // pre.sym.type
        case ConstantType(value) =>

          // int(2)
        case TypeRef(pre, sym, args) =>
          fm.appendText(sym.fullName)
          if (!args.isEmpty) {
            fm.appendText("[")
            val itr = args.iterator
            while (itr.hasNext) {
              htmlTypeName(itr.next, fm)
              if (itr.hasNext) {
                fm.appendText(", ")
              }
            }
            fm.appendText("]")
          }
          // pre.sym[targs]
        case RefinedType(parents, defs) =>
          fm.appendText(" extends ")
          val itr = parents.iterator
          while (itr.hasNext) {
            htmlTypeInfo(itr.next, fm)
            if (itr.hasNext) {
              fm.appendHtml("<br>")
              fm.appendText(" with ")
            }
          }
          fm.appendText("{...}")
          // parent1 with ... with parentn { defs }
        case AnnotatedType(annots, tp, selfsym) => htmlTypeInfo(tp, fm)
          // tp @annots

          // the following are non-value types; you cannot write them down in Scala source.

        case TypeBounds(lo, hi) =>
          fm.appendText(">: ")
          htmlTypeInfo(lo, fm)
          fm.appendText(" <: ")
          htmlTypeInfo(hi, fm)
          // >: lo <: hi
        case ClassInfoType(parents, defs, clazz) =>
          //htmlTypeInfo(clazz.tpe, fm)
          fm.appendText(" extends ")
          val itr = parents.iterator
          while (itr.hasNext) {
            htmlTypeInfo(itr.next, fm)
            if (itr.hasNext) {
              fm.appendHtml("<br>")
              fm.appendText(" with ")
            }
          }
          // same as RefinedType except as body of class
        case MethodType(paramtypes, result) =>
          if (!paramtypes.isEmpty) {
            fm.appendText("(")
            val itr = paramtypes.iterator
            while (itr.hasNext) {
              fm.`type`(true)
              htmlTypeInfo(itr.next.tpe, fm)
              fm.`type`(false)
              if (itr.hasNext) {
                fm.appendText(", ")
              }
            }
            fm.appendText(")")
          }
          fm.appendText(": ")
          htmlTypeInfo(result, fm)
          // (paramtypes)result
        case PolyType(tparams, result) =>
          if (!tparams.isEmpty) {
            fm.appendText("[")
            val itr = tparams.iterator
            while (itr.hasNext) {
              fm.`type`(true)
              htmlTypeInfo(itr.next.tpe, fm)
              fm.`type`(false)
              if (itr.hasNext) {
                fm.appendText(", ")
              }
            }
            fm.appendText("]")
          }
          fm.appendText(": ")
          htmlTypeInfo(result, fm)
          // [tparams]result where result is a MethodType or ClassInfoType
          // or
          // []T  for a eval-by-name type
        case ExistentialType(tparams, result) =>
          fm.appendText("ExistantialType")
          // exists[tparams]result

          // the last five types are not used after phase `typer'.

          //case OverloadedType(pre, tparams, alts) => "Overlaod"
          // all alternatives of an overloaded ident
        case AntiPolyType(pre: Type, targs) =>
          fm.appendText("AntiPolyType")
        case TypeVar(_, _) => 
          fm.appendText(tpe.safeToString)
          // a type variable
        case DeBruijnIndex(level, index) =>
          fm.appendText("DeBruijnIndex")
        case _ =>
          fm.appendText(tpe.safeToString)
      }
    }

    def completeIfWithLazyType(sym: Symbol) {
      val topClazz = sym.toplevelClass

      if (topClazz.nameString.indexOf('$') != -1) return // avoid assertion error @see
      
      val (clazz, staticModule) = if (topClazz.isModule) {
        (topClazz.companionClass, topClazz)
      } else {
        (topClazz, topClazz.companionModule)
      }

      if (clazz != NoSymbol && staticModule != NoSymbol) { // avoid Error: NoSymbol does not have owner
        topClazz.rawInfo match {
          case x if !x.isComplete => x.complete(topClazz)
          case _ =>
        }
      }
    }

    def isProperType(sym: Symbol): Boolean = {
      if (sym.isType && sym.hasRawInfo) {
        completeIfWithLazyType(sym)
        sym.rawInfo match {
          case NoType | ErrorType => false
          case _ => true
        }
      } else false
    }

    def importantItem(items: List[AstItem]): AstItem = {
      items map {item =>
        val (sym, baseLevel) = item match {
          case dfn: ScalaDfn => (dfn.symbol, 0)
          case ref: ScalaRef => (ref.symbol, 100)
        }

        val importantLevel = baseLevel + (if (sym == NoSymbol) 90
                                          else if (sym.isClass  || sym.isTrait || sym.isType || sym.isModule) 10
                                          else if (sym.isSetter || sym.hasFlag(Flags.MUTABLE)) 20
                                          else if (sym.isGetter)      30
                                          else if (sym.isConstructor) 40
                                          else if (!sym.isMethod)     50
                                          else 60)

        (importantLevel, item)
      } sortWith {(x1, x2) => x1._1 < x2._1} head match {
        case (_, item) => item
      }
    }

    @throws(classOf[Throwable])
    def symSimpleSig(sym: Symbol) = {
      val tpe = sym.tpe // may throws exception
      typeSimpleSig(tpe)
    }

    def typeSimpleSig(tpe: Type) = {
      val sb = new StringBuilder
      typeSimpleSig_(tpe, sb)
      sb.toString
    }

    /** use to test if type is the same: when they have same typeSimpleSig true, otherwise false */
    private def typeSimpleSig_(tpe: Type, sb: StringBuilder): Unit = {
      if (tpe == null) return
      tpe match {
        case ErrorType =>
          sb.append("<error>")
          // internal: error
        case WildcardType => "_"
          // internal: unknown
        case NoType => sb.append("<notype>")
        case NoPrefix => sb.append("<noprefix>")
        case ThisType(sym) =>
          sb append (sym.fullName)
        case SingleType(pre, sym) =>
          sb append (sym.fullName)
        case ConstantType(value) =>
          // int(2)
        case TypeRef(pre, sym, args) =>
          sb append (sym.fullName)
          sb append (args map (x => typeSimpleSig_(x, sb)) mkString ("[", ",", "]"))
          // pre.sym[targs]
        case RefinedType(parents, defs) =>
          sb append (parents map (x => typeSimpleSig_(x, sb)) mkString (" extends ", "with ", ""))
        case AnnotatedType(annots, tp, selfsym) =>
          typeSimpleSig_(tp, sb)
        case TypeBounds(lo, hi) =>
          sb append (">: ")
          typeSimpleSig_(lo, sb)
          sb append (" <: ")
          typeSimpleSig_(hi, sb)
          // >: lo <: hi
        case ClassInfoType(parents, defs, clazz) =>
          sb append (parents map (x => typeSimpleSig_(x, sb)) mkString (" extends ", " with ", ""))
        case MethodType(paramtypes, result) => // same as RefinedType except as body of class
          sb append (paramtypes map (x => typeSimpleSig_(x.tpe, sb)) mkString("(", ",", ")"))
          sb append (": ")
          typeSimpleSig_(result, sb)
          // (paramtypes): result
        case PolyType(tparams, result) =>
          sb append (tparams map (x => typeSimpleSig_(x.tpe, sb)) mkString("[", ",", "]"))
          sb append (": ")
          typeSimpleSig_(result, sb)
          // [tparams]: result where result is a MethodType or ClassInfoType
          // or
          // []: T  for a eval-by-name type
        case ExistentialType(tparams, result) =>
          sb append ("ExistantialType")
          // exists[tparams]result

          // the last five types are not used after phase `typer'.

          //case OverloadedType(pre, tparams, alts) => "Overlaod"
          // all alternatives of an overloaded ident
        case AntiPolyType(pre: Type, targs) =>
          sb append ("AntiPolyType")
        case TypeVar(_, _) =>
          sb append (tpe.safeToString)
          // a type variable
        case DeBruijnIndex(level, index) =>
          sb append ("DeBruijnIndex")
        case _ =>
          sb append (tpe.safeToString)
      }
    }


  }

}
