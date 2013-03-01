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
import scala.reflect.internal.ClassfileConstants._
import scala.reflect.internal.Flags

abstract class JavaStubGenerator extends scala.reflect.internal.transform.Erasure {

  import JavaStubGenerator._

  private final val LOGGER = Logger.getLogger(this.getClass.getName)

  val global: ScalaGlobal
  import global._
  import definitions._

  /** Constants for readability */
  private val IS_OBJECT = true
  private val IS_NOT_OBJECT = false
  private val IS_NOT_TRAIT = false

  private var isTrait = false
  private var isObject = false
  private var isCompanion = false

  @throws(classOf[FileNotFoundException])
  def genClass(pkgName: String, clzName: String, syms: Array[Symbol]): CharSequence = {
    global.askForResponse {() =>
      val javaCode = new StringBuilder(1024)

      if (!pkgName.isEmpty) {
        javaCode ++= "package " ++= pkgName ++= ";\n"
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


      if (tpe ne null) {
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
          javaCode ++= new JavaMemberStubGenerator(IS_NOT_OBJECT, IS_NOT_TRAIT).genJavaMembers(sym, tpe)

          val oSym = syms(1)
          val oTpe = tryTpe(oSym)

          if (oTpe ne null) {
            javaCode ++= new JavaMemberStubGenerator(IS_OBJECT, IS_NOT_TRAIT).genJavaMembers(oSym, oTpe)
          }
        } else {
          javaCode ++= new JavaMemberStubGenerator(isObject, isTrait).genJavaMembers(sym, tpe)
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
    } get match {
      case Left(x) => x
      case Right(ex) => ""
    }
  }

  private def tryTpe(sym: Symbol): Type = {
    try {
      sym.tpe
    } catch {
      case _: Throwable => null
    }
  }

  /**
   * Returns true if the type is not null and an of its members have the deferred flag.
   */
  private def isAbstractClass(tpe: Type): Boolean = {
    (tpe ne null) && (
      (try {
          tpe.members
        } catch {
          case ex: Throwable => EmptyScope
        }
      ) exists (_ hasFlag Flags.DEFERRED)
    )
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
      val javaCode = new StringBuilder()
      val members = try {
        tpe.members
      } catch {
        case ex: Throwable => EmptyScope
      }
      
      for (member <- members if !member.hasFlag(Flags.PRIVATE)) {
        val memberType = try {
          member.tpe
        } catch {
          case ex: Throwable => ScalaGlobal.resetLate(global, ex); null
        }
        javaCode ++= genJavaMember(sym, member, memberType)
      }
      javaCode.toString
    }

    private def genJavaMember(sym: Symbol, member: Symbol, memberType: Type): String = {
      if ((memberType eq null) || ScalaUtil.isInherited(sym, member)) {
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
      modifiers(member) + encodeName(sym.nameString) + params(memberType.params) + " {}\n"
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
      modifiers(member) + " " + mResQName + " " + member.nameString + ";\n"
    }

    //TODO: Refactor this down to a smaller method
    private def genJavaMethod(member: Symbol, memberType: Type): String = {
      val javaCode = new StringBuilder()
      val mSName = member.nameString
      val mResTpe = try {
        memberType.resultType
      } catch {
        case ex: Throwable => ScalaGlobal.resetLate(global, ex); null
      }

      if ((mResTpe ne null) && mSName != "$init$" && mSName != "synchronized") {
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
      javaCode.toString
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

      val tpeName = try {
        encodeQName(param.tpe.typeSymbol.fullName)
      } catch {
        case ex: Throwable => ScalaGlobal.resetLate(global, ex); "Object"
      }
      sb.append(tpeName)
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

  private def hiBounds(bounds: TypeBounds): List[Type] = bounds.hi.normalize match {
    case RefinedType(parents, _) => parents map (_.normalize)
    case tp                      => tp :: Nil
  }

  /** The Java signature of type 'info', for symbol sym. The symbol is used to give the right return
   *  type for constructors.
   */
  def javaSig(sym0: Symbol, info: Type): Option[String] = beforeErasure {
    val isTraitSignature = sym0.enclClass.isTrait

    def superSig(parents: List[Type]) = {
      val ps = (
        if (isTraitSignature) {
          // java is unthrilled about seeing interfaces inherit from classes
          val ok = parents filter (p => p.typeSymbol.isTrait || p.typeSymbol.isInterface)
          // traits should always list Object.
          if (ok.isEmpty || ok.head.typeSymbol != ObjectClass) ObjectClass.tpe :: ok
          else ok
        }
        else parents
      )
      (ps map boxedSig).mkString
    }
    def boxedSig(tp: Type) = jsig(tp, primitiveOK = false)
    def boundsSig(bounds: List[Type]) = {
      val (isTrait, isClass) = bounds partition (_.typeSymbol.isTrait)
      val classPart = isClass match {
        case Nil    => ":" // + boxedSig(ObjectClass.tpe)
        case x :: _ => ":" + boxedSig(x)
      }
      classPart :: (isTrait map boxedSig) mkString ":"
    }
    def paramSig(tsym: Symbol) = tsym.name + boundsSig(hiBounds(tsym.info.bounds))
    def polyParamSig(tparams: List[Symbol]) = (
      if (tparams.isEmpty) ""
      else tparams map paramSig mkString ("<", "", ">")
    )

    // Anything which could conceivably be a module (i.e. isn't known to be
    // a type parameter or similar) must go through here or the signature is
    // likely to end up with Foo<T>.Empty where it needs Foo<T>.Empty$.
    def fullNameInSig(sym: Symbol) = "Array<" + beforeIcode(sym.javaBinaryName) + ">" // "L" + beforeIcode(sym.javaBinaryName)

    def jsig(tp0: Type, existentiallyBound: List[Symbol] = Nil, toplevel: Boolean = false, primitiveOK: Boolean = true): String = {
      val tp = tp0.dealias
      tp match {
        case st: SubType =>
          jsig(st.supertype, existentiallyBound, toplevel, primitiveOK)
        case ExistentialType(tparams, tpe) =>
          jsig(tpe, tparams, toplevel, primitiveOK)
        case TypeRef(pre, sym, args) =>
          def argSig(tp: Type) =
            if (existentiallyBound contains tp.typeSymbol) {
              val bounds = tp.typeSymbol.info.bounds
              if (!(AnyRefClass.tpe <:< bounds.hi)) "+" + boxedSig(bounds.hi)
              else if (!(bounds.lo <:< NullClass.tpe)) "-" + boxedSig(bounds.lo)
              else "*"
            } else {
              boxedSig(tp)
            }
          def classSig = {
            val preRebound = pre.baseType(sym.owner) // #2585
            dotCleanup(
              (
                if (needsJavaSig(preRebound)) {
                  val s = jsig(preRebound, existentiallyBound)
                  if (s.charAt(0) == 'L') {
                    "Array<" + s.substring(1, s.length - 1) + "." + sym.javaSimpleName + ">"
                    //s.substring(0, s.length - 1) + "." + sym.javaSimpleName
                  }
                  else fullNameInSig(sym)
                }
                else fullNameInSig(sym)
              ) + (
                if (args.isEmpty) "" else
                  "<"+(args map argSig).mkString+">"
              ) + (
                ";"
              )
            )
          }

          // If args isEmpty, Array is being used as a type constructor
          if (sym == ArrayClass && args.nonEmpty) {
            if (unboundedGenericArrayLevel(tp) == 1) jsig(ObjectClass.tpe)
            else ARRAY_TAG.toString+(args map (jsig(_))).mkString
          }
          else if (isTypeParameterInSig(sym, sym0)) {
            assert(!sym.isAliasType, "Unexpected alias type: " + sym)
            "" + TVAR_TAG + sym.name + ";"
          }
          else if (sym == AnyClass || sym == AnyValClass || sym == SingletonClass)
            jsig(ObjectClass.tpe)
          else if (sym == UnitClass)
            jsig(BoxedUnitClass.tpe)
          else if (sym == NothingClass)
            jsig(RuntimeNothingClass.tpe)
          else if (sym == NullClass)
            jsig(RuntimeNullClass.tpe)
          else if (isPrimitiveValueClass(sym)) {
            if (!primitiveOK) jsig(ObjectClass.tpe)
            else if (sym == UnitClass) jsig(BoxedUnitClass.tpe)
            else abbrvTag(sym).toString
          }
          else if (sym.isDerivedValueClass) {
            val unboxed     = sym.derivedValueClassUnbox.info.finalResultType
            val unboxedSeen = (tp memberType sym.derivedValueClassUnbox).finalResultType
            def unboxedMsg  = if (unboxed == unboxedSeen) "" else s", seen within ${sym.simpleName} as $unboxedSeen"
            logResult(s"Erasure of value class $sym (underlying type $unboxed$unboxedMsg) is") {
              if (isPrimitiveValueType(unboxedSeen) && !primitiveOK)
                classSig
              else
                jsig(unboxedSeen, existentiallyBound, toplevel, primitiveOK)
            }
          }
          else if (sym.isClass)
            classSig
          else
            jsig(erasure(sym0)(tp), existentiallyBound, toplevel, primitiveOK)
        case PolyType(tparams, restpe) =>
          assert(tparams.nonEmpty)
          val poly = if (toplevel) polyParamSig(tparams) else ""
          poly + jsig(restpe)

        case MethodType(params, restpe) =>
          val buf = new StringBuffer("(")
          params foreach (p => buf append jsig(p.tpe))
          buf append ")"
          buf append (if (restpe.typeSymbol == UnitClass || sym0.isConstructor) VOID_TAG.toString else jsig(restpe))
          buf.toString

        case RefinedType(parent :: _, decls) =>
          boxedSig(parent)
        case ClassInfoType(parents, _, _) =>
          superSig(parents)
        case AnnotatedType(_, atp, _) =>
          jsig(atp, existentiallyBound, toplevel, primitiveOK)
        case BoundedWildcardType(bounds) =>
          println("something's wrong: "+sym0+":"+sym0.tpe+" has a bounded wildcard type")
          jsig(bounds.hi, existentiallyBound, toplevel, primitiveOK)
        case _ =>
          val etp = erasure(sym0)(tp)
          if (etp eq tp) throw new UnknownSig
          else jsig(etp)
      }
    }
    if (needsJavaSig(info)) {
      try Some(jsig(info, toplevel = true))
      catch { case ex: UnknownSig => None }
    }
    else None
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

  // only refer to type params that will actually make it into the sig, this excludes:
  // * higher-order type parameters
  // * type parameters appearing in method parameters
  // * type members not visible in an enclosing template
  private def isTypeParameterInSig(sym: Symbol, initialSymbol: Symbol) = (
    !sym.isHigherOrderTypeParameter &&
    sym.isTypeParameterOrSkolem && (
      (initialSymbol.enclClassChain.exists(sym isNestedIn _)) ||
      (initialSymbol.isMethod && initialSymbol.typeParams.contains(sym))
    )
  )

  // Ensure every '.' in the generated signature immediately follows
  // a close angle bracket '>'.  Any which do not are replaced with '$'.
  // This arises due to multiply nested classes in the face of the
  // rewriting explained at rebindInnerClass.   This should be done in a
  // more rigorous way up front rather than catching it after the fact,
  // but that will be more involved.
  private def dotCleanup(sig: String): String = {
    var last: Char = '\0'
    sig map {
      case '.' if last != '>' => last = '.' ; '$'
      case ch                 => last = ch ; ch
    }
  }

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

  @inline
  private def logResult[T](msg: => String)(result: T): T = {
    LOGGER.warning(msg + ": " + result)
    result
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
