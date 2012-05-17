/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved.
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
 * Portions Copyrighted 2008 Sun Microsystems, Inc.
 */
package org.netbeans.modules.scala.editor

import java.io.FileNotFoundException
import java.util.logging.Logger

import org.netbeans.modules.scala.core.ScalaGlobal

import scala.collection.mutable.HashSet
import scala.reflect.NameTransformer
import scala.tools.nsc.symtab.Flags

abstract class JavaStubGenerator {

  import JavaStubGenerator._

  private final val LOGGER = Logger.getLogger(this.getClass.getName)

  val global: ScalaGlobal
  import global._
  import definitions._

  /** Constants for readability */
  private final val IS_OBJECT = true;
  private final val IS_NOT_OBJECT = false;
  private final val IS_NOT_TRAIT = false;

  private var isTrait = false
  private var isObject = false
  private var isCompanion = false

  @throws(classOf[FileNotFoundException])
  def genClass(pkgName: String, clzName: String, syms: Array[Symbol]): CharSequence = {
    val javaCode = new StringBuilder(1024)

    if (!pkgName.isEmpty) {
      javaCode ++= "package" ++= pkgName ++= ";\n"
    }

    val sym = syms match {
      //syms takes the form of (class, object, trait)
      case Array(null, null, traitSym) => // trait
        isTrait = true
        javaCode ++= modifiers(traitSym) ++= "interface "
        traitSym
      case Array(null, objSym, null) => // single object
        isObject = true
        javaCode ++= modifiers(objSym) ++= "final class "
        objSym
      case Array(classSym, null, null) => // single class
        javaCode ++= modifiers(classSym) ++= "class "
        classSym
      case Array(classSym, objSym, null) => // companion object + class
        isCompanion = true
        javaCode ++= modifiers(classSym) ++= "class "
        classSym
      case Array(_, obj, traitSym) => // companion object + trait ?
        isTrait = true
        javaCode ++= modifiers(traitSym) ++= "interface "
        traitSym
    }

    // * we get clzName, do not try javaSig, which contains package string
    // * and may be binary name, for example, an object's name will be "object$"
    javaCode ++= clzName

    val tpe = tryTpe(sym)
    javaSig(sym, tpe) match {
      case Some(sig) => javaCode ++= getGenericPart(sig)
      case None =>
    }

    val qName = sym.fullName

    val superClass = sym.superClass
    val superQName = superClass match {
      case null => ""
      case x => x.fullName
    }

    var extended = false
    if (!isTrait && superQName.length > 0) {
      javaCode ++= " extends "
      extended = true

      javaSig(superClass, superClass.tpe) match {
        case Some(sig) => javaCode ++= sig
        case None => javaCode ++= encodeQName(superQName)
      }
    }


    if (tpe != null) {
      val itr = tpe.baseClasses.tail.iterator // head is always `java.lang.Object`?
      var implemented = false
      while (itr.hasNext) {
        val base = itr.next
        base.fullName  match {
          case `superQName` =>
          case `qName` =>
          case "java.lang.Object" =>
          case "scala.Any" =>  // javaSig of "scala.Any" will be "java.lang.Object"
          case baseQName =>
            if (base.isTrait) {
              if (isTrait) {
                if (!extended) {
                  javaCode ++= " extends "
                  extended = true
                } else {
                  javaCode ++= ", "
                }
              } else {
                if (!implemented) {
                  javaCode ++= " implements "
                  implemented = true
                } else {
                  javaCode ++= ", "
                }
              }

              javaCode ++= encodeQName(baseQName)
            } else { // base is class
              if (isTrait) {
                // * shound not happen or error of "interface extends a class", ignore it
              } else {
                if (!extended) {
                  javaCode ++= " extends "
                  extended = true
                } else {
                  javaCode ++= ", "
                }

                javaSig(base, base.tpe) match {
                  case Some(sig) => javaCode ++= sig
                  case None => javaCode ++= encodeQName(baseQName)
                }
              }
            }
        }
      }

      javaCode ++= " {\n"

      if (isCompanion) {
        javaCode ++= new JavaMemberStubGenerator(IS_NOT_OBJECT, IS_NOT_TRAIT).
                     genJavaMembers(sym, tpe)

        val oSym = syms(1)
        val oTpe = tryTpe(oSym)

        if (oTpe != null) {
          javaCode ++= new JavaMemberStubGenerator(IS_OBJECT, IS_NOT_TRAIT).
                       genJavaMembers(oSym, oTpe)
        }
      } else {
        javaCode ++= new JavaMemberStubGenerator(isObject, isTrait).
                     genJavaMembers(sym, tpe)
      }

      if (!isTrait) {
        javaCode ++= dollarTagMethod ++= "\n" // should implement scala.ScalaObject
      }

      javaCode ++= "}\n"
    } else {
      javaCode ++= " {\n"

      if (!isTrait) {
        javaCode ++= dollarTagMethod ++= "\n" // should implement scala.ScalaObject
      }

      javaCode ++= "}\n"
    }

    //Log.log(Level.INFO, "Java stub: {0}", sw)

    javaCode.toString
  }

  private def tryTpe(sym: Symbol): Type = {
    try {
      sym.tpe
    } catch {case _ => null}
  }

  /**
   * Returns true if the type is not null and an of its members have the deferred flag.
   */
  private def isAbstractClass(tpe: Type): Boolean = {
    tpe != null && (tpe.members exists (_ hasFlag Flags.DEFERRED))
  }

  private case class JavaMemberStubGenerator(isObject: Boolean, isTrait: Boolean) {
    
    /** 
     * Generates java code for all non-private members of a Type
     *  
     * @param sym - The Symbol representing the trait, class, or object for which
     *   to generate code
     * @param tpe - The resolved object type of the symbol, from which to collect
     *   non-private members
     */
    def genJavaMembers(sym: Symbol, tpe: Type): String = {
      val javaCode = new StringBuilder
      for (member <- tpe.members if !member.hasFlag(Flags.PRIVATE)) {
        val memberType = try {
          member.tpe
        } catch {case ex => ScalaGlobal.resetLate(global, ex); null}
        javaCode ++= genJavaMember(sym, member, memberType)
      }
      return javaCode.toString
    }

    private def genJavaMember(sym: Symbol, member: Symbol, memberType: Type): String = {
      if (memberType == null || ScalaUtil.isInherited(sym, member)) {
        return ""
      }

      member match {
        case _ if member.isTrait || member.isClass || member.isModule =>
          return "" // @todo
        case _ if member.isConstructor =>
          return genJavaConstructor(sym, member, memberType)
        case _ if member.isMethod =>
          return genJavaMethod(member, memberType)
        case _ if member.isVariable =>
          return "" // do nothing
        case _ if member.isValue =>
          return genJavaValue(member, memberType)
        case _ =>
          return ""
      }
    }

    private def genJavaConstructor(sym: Symbol, member: Symbol, memberType: Type): String = {
      if (isTrait || isObject) {
        return ""
      }
      return modifiers(member) + encodeName(sym.nameString) + params(memberType.params) + " {}\n"
    }

    private def genJavaValue(member: Symbol, memberType: Type): String = {
      if (isTrait) { // Should this go in the calling code instead?
        return ""
      }
      val mResTpe = memberType.resultType
      val mResSym = mResTpe.typeSymbol
      val mResQName = javaSig(mResSym, mResTpe) match {
        case Some(sig) => sig
        case None => encodeType(mResSym.fullName)
      }
      return modifiers(member) + " " + mResQName + " " + member.nameString + ";\n"
    }

    //TODO: Refactor this down to a smaller method
    private def genJavaMethod(member: Symbol, memberType: Type): String = {
      val javaCode = new StringBuilder
      val mSName = member.nameString
      val mResTpe = try {
        memberType.resultType
      } catch {case ex => ScalaGlobal.resetLate(global, ex); null}

      if (mResTpe != null && mSName != "$init$" && mSName != "synchronized") {
        val mResSym = mResTpe.typeSymbol

        javaCode ++= modifiers(member)
        if (isObject && !isTrait) javaCode ++= "static final "

        val mResQName = javaSig(mResSym, mResTpe) match {
          case Some(sig) => sig
          case None => encodeType(mResSym.fullName)
        }

        javaSig(member, memberType) match {
          case Some(sig) =>
            javaCode ++= sig
          case None =>
            // method return type
            javaCode ++= mResQName ++= " "
            // method name
            javaCode ++= encodeName(mSName)
            // method parameters
            javaCode ++= params(memberType.params) ++= " "
        }

        // method body or ";"
        if (!isTrait && !member.hasFlag(Flags.DEFERRED)) {
          javaCode ++= "{" ++= returnStrOfType(mResQName) ++= "}\n"
        } else {
          javaCode ++= ";\n"
        }
      }
      return javaCode.toString
    }
  }
  
  private val dollarTagMethod = "public int $tag() throws java.rmi.RemoteException {return 0;}"

  /**
   * Creates a string of java modifiers from the Symbol, followed by a space.
   * e.g. public, protected, or private and possibly abstract
   * 
   */
  private def modifiers(sym: Symbol): String = {
    val sb = new StringBuilder
    if (sym hasFlag Flags.PRIVATE) {
      sb.append("private ")
    } else if (sym hasFlag Flags.PROTECTED) {
      sb.append("protected ")
    } else {
      sb.append("public ")
    }

    if (sym.isClass  && !sym.isTrait        && sym.hasFlag(Flags.ABSTRACT)) sb.append("abstract ")
    if (sym.isMethod && !sym.isConstructor  && sym.hasFlag(Flags.DEFERRED)) sb.append("abstract ")

    sb.toString
  }

  def classSName(sym: Symbol): String = {
    if (isNestedTemplate(sym)) {
      classSName(sym.owner) + "$" + encodeName(sym.nameString)
    } else encodeName(sym.nameString)
  }

  def isNestedTemplate(sym: Symbol): Boolean = {
    (sym.isTrait || sym.isModule || sym.isClass) && !sym.isRoot && !sym.owner.isPackageClass
  }
  
  private def params(params: List[Symbol]): String = {
    val sb = new StringBuffer
    sb.append("(")

    val paramNames = new HashSet[String]
    var i = 0
    val itr = params.iterator
    while (itr.hasNext) {
      val param = itr.next

      val tpe = try {
        param.tpe
      } catch {case ex => ScalaGlobal.resetLate(global, ex); null}
      
      if (tpe != null) {
        sb.append(encodeQName(tpe.typeSymbol.fullName))
      } else {
        sb.append("Object")
      }
      sb.append(" ")

      val name = param.nameString
      if (name.length > 0 && paramNames.add(name)) {
        sb.append(name)
      } else {
        sb.append("a")
        sb.append(i)
      }
      
      if (itr.hasNext) sb.append(", ")
      
      i += 1
    }

    sb.append(")")
    
    sb.toString
  }

  def getGenericPart(classJavaSig: String): String = {
    classJavaSig.indexOf('<') match {
      case -1 => ""
      case i  => classJavaSig.substring(i, classJavaSig.length)
    }
  }

  // ----- @see scala.tools.nsc.transform.Erasure

  def javaSig(sym: Symbol, info: Type): Option[String] = {

    def jsig(tp: Type): String = jsig2(false, Nil, tp)

    def jsig2(toplevel: Boolean, tparams: List[Symbol], atp: Type): String = {
      if (atp == null) return "Object"

      val tp = atp.dealias
      tp match {
        case st: SubType =>
          jsig2(toplevel, tparams, st.supertype)

        case ExistentialType(tparams, tpe) =>
          jsig2(toplevel, tparams, tpe)

        case TypeRef(pre, sym, args) =>
          def argSig(tp: Type) =
            if (tparams contains tp.typeSymbol) {
              val bounds = tp.typeSymbol.info.bounds

              if (!(AnyRefClass.tpe <:< bounds.hi))  "? extends " + jsig(bounds.hi)
              else if (!(bounds.lo <:< NullClass.tpe)) "? super " + jsig(bounds.lo)
              else "?"
            } else if (tp.typeSymbol == UnitClass) {
              jsig(ObjectClass.tpe)
            } else {
              boxedClass get tp.typeSymbol match {
                case Some(boxed) => jsig(boxed.tpe)
                case None => jsig(tp)
              }
            }
          def argsSig(args: List[Type]) = (if (args.isEmpty) "" else (args map argSig).mkString("<", ", ", ">"))
          def classSig: String = "L" + sym.fullName + global.genJVM.moduleSuffix(sym)
          //"L" + (sym.fullName + global.genJVM.moduleSuffix(sym)).replace('.', '/')
          def classSigSuffix: String = "." + sym.name

          sym match {
            case ArrayClass =>
              (args map jsig).mkString + "[]"
              //ARRAY_TAG.toString + (args map jsig).mkString
            case _ if sym.isTypeParameterOrSkolem && !sym.owner.isTypeParameterOrSkolem =>
              // * not a higher-order type parameter, as these are suppressed
              sym.nameString
            case AnyClass | AnyValClass | SingletonClass =>
              jsig(ObjectClass.tpe)
            case UnitClass =>
              jsig(BoxedUnitClass.tpe)
            case NothingClass =>
              jsig(RuntimeNothingClass.tpe)
            case NullClass =>
              jsig(RuntimeNullClass.tpe)
            case _ if isValueClass(sym) =>
              tagOfClass(sym)
            case _ if sym.isClass =>
              sym.fullName match {
                case "scala.<byname>" =>
                  "scala.Function0" + argsSig(args)
                case "scala.<repeated>" =>
                  "scala.collection.Sequence" + argsSig(args)
                case _ =>
                  val postPre = if (needsJavaSig(pre)) {
                    val s = jsig(pre)
                    if (s.charAt(0) == 'L') s.substring(0, s.length - 1) + classSigSuffix else classSig
                  } else classSig

                  (if (postPre.charAt(0) == 'L') postPre.substring(1, postPre.length) else postPre) + argsSig(args)
              }
            case _ => jsig(erasure.erasure(tp))
          }

        case PolyType(tparams, restpe) =>
          def hiBounds(bounds: TypeBounds): List[Type] = bounds.hi.normalize match {
            case RefinedType(parents, _) => parents map analyzer.normalize
            case tp => List(tp)
          }
          def boundSig(bounds: List[Type]) = {
            def isClassBound(t: Type) = !t.typeSymbol.isTrait
            val classBound = bounds find isClassBound match {
              case Some(t) => jsig(t)
              case None => ""
            }
            " extends " + classBound + (for (t <- bounds if !isClassBound(t)) yield ":" + jsig(t)).mkString
          }
          def paramSig(tsym: Symbol) = tsym.name + boundSig(hiBounds(tsym.info.bounds))
          
          //assert(!tparams.isEmpty)
          if (tparams.isEmpty) {
            (if (restpe.typeSymbol == UnitClass) "void" else jsig(restpe)) + " " + sym.name + "()"
          } else {
            (if (toplevel) (tparams map paramSig).mkString("<", ", ", ">") else "") + " " + jsig(restpe)
          }

        case MethodType(params, restpe) =>
          def paramsSig(params: List[Symbol]) = {
            var i = 0
            params map {x =>
              var name = x.name + ""
              name = if (name.length > 0) name else "a" + i
              i += 1
              jsig(x.tpe) + " " + name
            } mkString("(", ", ", ")")
          }

          (if (sym.isConstructor) "" else if (restpe.typeSymbol == UnitClass) "void" else jsig(restpe)) + " " +
          sym.name + paramsSig(params)

        case NullaryMethodType(restpe) =>
          sym.name + ""
          
        case RefinedType(parents, decls) if (!parents.isEmpty) =>
          jsig(parents.head)

        case ClassInfoType(parents, _, _) =>
          (parents map jsig).mkString

        case AnnotatedType(_, atp, _) =>
          jsig(atp)

        case _ =>
          val etp = erasure.erasure(tp)
          if (etp eq tp) throw new UnknownSig
          else jsig(etp)
      }
    }

    if (info == null) return None
    try {
      Some(jsig2(true, Nil, info))
    } catch {case ex: UnknownSig => LOGGER.warning(sym + " has UnknownSig"); None}
  }

  class UnknownSig extends Exception

  private lazy val tagOfClass = Map[Symbol, String](
    ByteClass    -> "byte",
    CharClass    -> "char",
    DoubleClass  -> "double",
    FloatClass   -> "float",
    IntClass     -> "int",
    LongClass    -> "long",
    ShortClass   -> "int",
    BooleanClass -> "boolean",
    UnitClass    -> "void"
  )

  private object NeedsSigCollector extends TypeCollector(false) {
    def traverse(tp: Type) {
      if (!result) {
        tp match {
          case st: SubType =>
            traverse(st.supertype)
          case TypeRef(pre, sym, args) =>
            if (sym == ArrayClass) args foreach traverse
            else if (sym.isTypeParameterOrSkolem || sym.isExistentiallyBound || !args.isEmpty) result = true
            else if (!sym.owner.isPackageClass) traverse(pre)
          case PolyType(_, _) | ExistentialType(_, _) =>
            result = true
          case RefinedType(parents, decls) =>
            if (!parents.isEmpty) traverse(parents.head)
          case ClassInfoType(parents, _, _) =>
            parents foreach traverse
          case AnnotatedType(_, atp, _) =>
            traverse(atp)
          case _ => mapOver(tp)
        }
      }
    }
  }

  private def needsJavaSig(tp: Type) = NeedsSigCollector.collect(tp)

  // ----- end of scala.tools.nsc.transform.Erasure.scala

  /*
   * to java name
   */
  private def encodeName(scalaTermName: String): String = {
    NameTransformer.encode(scalaTermName)
  }

  /*
   * to java type name
   */
  private def encodeType(scalaTypeQName: String): String = {
    scalaTypeQName match {
      case "scala.runtime.BoxedUnit" => "void"
      case "scala.Unit" => "void"
      case _ => encodeQName(scalaTypeQName)
    }
  }

  private def encodeQName(qName: String): String = {
    qName.lastIndexOf('.') match {
      case -1 => encodeName(qName)
      case i =>
        val pkgName = qName.substring(0, i + 1) // with last '.'
        val sName = qName.substring(i + 1, qName.length)
        pkgName + encodeName(sName)
    }
  }
}

object JavaStubGenerator {
  private def returnStrOfType(tpe: String) = tpe match {
    case "scala.runtime.BoxedUnit" => "return;"
    case "scala.Unit" => "return;"
    case "void"    => "return;"
    case "double"  => "return 0.0;"
    case "float"   => "return 0.0f;"
    case "long"    => "return 0L;"
    case "int"     => "return 0;"
    case "short"   => "return 0;"
    case "byte"    => "return 0;"
    case "boolean" => "return false;"
    case "char"    => "return 0;"
    case _ => "return null;"
  }
}
