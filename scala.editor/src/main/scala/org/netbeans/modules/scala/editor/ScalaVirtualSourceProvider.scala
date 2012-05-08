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

import java.io.File
import java.io.FileNotFoundException
import java.util.logging.Logger
import org.netbeans.modules.csl.api.ElementKind
import org.netbeans.modules.java.preprocessorbridge.spi.VirtualSourceProvider
import org.netbeans.modules.parsing.api.ParserManager
import org.netbeans.modules.parsing.api.ResultIterator
import org.netbeans.modules.parsing.api.Source
import org.netbeans.modules.parsing.api.UserTask
import org.netbeans.modules.parsing.impl.indexing.TimeStamps
import org.netbeans.modules.parsing.spi.ParseException
import org.openide.filesystems.{FileUtil}
import org.openide.util.Exceptions

import org.netbeans.api.language.util.ast.{AstScope}
import org.netbeans.modules.scala.core.ScalaGlobal
import org.netbeans.modules.scala.core.ScalaParserResult
import org.netbeans.modules.scala.core.ast.ScalaDfns
import scala.collection.mutable.ArrayBuffer

import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet
import scala.reflect.NameTransformer
import scala.tools.nsc.symtab.Flags


/**
 * Virtual java source
 *
 * @author Caoyuan Deng
 */

/* @org.openide.util.lookup.ServiceProviders(
 Array(new org.openide.util.lookup.ServiceProvider(service = classOf[JavaSourceProvider]),
 new org.openide.util.lookup.ServiceProvider(service = classOf[VirtualSourceProvider]))
 ) */

/**
 * This requires also a Java Indexer to be enabled for scala mimetype
 * @see layer.xml:
 *      <file name="JavaIndexer.shadow">
 *          <attr name="originalFile" stringvalue="Editors/text/x-java/JavaIndexer.instance"/>
 *      </file>
 *
 * @Note: don't use full class name `classOf[org.netbeans.modules.java.preprocessorbridge.spi.VirtualSourceProvider]`, here
 * instead, should use `classOf[VirtualSourceProvider]`, otherwise, lookup cannot find it. Why? don't know ...
 */
@org.openide.util.lookup.ServiceProvider(service = classOf[VirtualSourceProvider])
class ScalaVirtualSourceProvider extends VirtualSourceProvider {
  import ScalaVirtualSourceProvider._

  private val log2 = Logger.getLogger(this.getClass.getName)

  log2.info(this.getClass.getSimpleName + " is created")

  /** @Todo
   * The only reason to implement JavaSourceProvider is to get a none-null JavaSource#forFileObject,
   * the JavaSource instance is a must currently when eval expression under debugging. see issue #150903
   */
  /* def forFileObject(fo: FileObject): JavaSourceProvider.PositionTranslatingJavaFileFilterImplementation = {
   if (!"text/x-scala".equals(FileUtil.getMIMEType(fo)) && !"scala".equals(fo.getExt)) {  //NOI18N
   return null
   } else {
   new JavaSourceProvider.PositionTranslatingJavaFileFilterImplementation {
   def getOriginalPosition(javaSourcePosition: Int): Int = -1
   def getJavaSourcePosition(originalPosition: Int): Int = -1
   def filterReader(r: Reader): Reader = r
   def filterCharSequence(charSequence: CharSequence): CharSequence = ""
   def filterWriter(w: Writer): Writer = w
   def addChangeListener(listener: ChangeListener) {}
   def removeChangeListener(listener: ChangeListener) {}
   }
   }
   } */

  override def getSupportedExtensions: java.util.Set[String] = {
    java.util.Collections.singleton("scala") // NOI18N
  }

  override def index: Boolean = true

  override def translate(files: java.lang.Iterable[File], sourceRoot: File, result: VirtualSourceProvider.Result) {
    val root = FileUtil.toFileObject(sourceRoot)
    val timeStamps = TimeStamps.forRoot(root.getURL, false)

    val itr = files.iterator
    while (itr.hasNext) {
      val file = itr.next
      val fo = FileUtil.toFileObject(file)
      // JavaIndexer tends to reindex all dependent (via VirtualSources calculating) files
      // when dependee source file is modified, it's not neccessary for VirtualSource in my opinion,
      // so, filter them here:
      val isUpToDate = timeStamps.checkAndStoreTimestamp(fo, FileUtil.getRelativePath(root, fo))
      if (!isUpToDate) {
        val t0 = System.currentTimeMillis
        translate(file, sourceRoot, result)
        log2.info("Translated " + fo.getNameExt + " in " + (System.currentTimeMillis - t0) + "ms")
      }
    }
  }

  private def translate(file: File, sourceRoot: File, result: VirtualSourceProvider.Result) {
    val fo = FileUtil.toFileObject(file)
    
    if (fo == null) return

    val rootFo = FileUtil.toFileObject(sourceRoot)
    try {
      val source = Source.create(fo)
      /** @Note: do not use UserTask to parse it? which may cause "refershing workspace" */
      // FIXME can we move this out of task (?)
      ParserManager.parse(java.util.Collections.singleton(source), new UserTask {
            
          @throws(classOf[ParseException])
          override def run(ri: ResultIterator) {
            val pr = ri.getParserResult.asInstanceOf[ScalaParserResult]
            val global = pr.global
            val rootScope = pr.rootScope
            val tmpls = new ArrayBuffer[ScalaDfns#ScalaDfn]
            visit(rootScope, tmpls)
            process(global, tmpls.toList)
          }

          private def visit(scope: AstScope, tmpls: ArrayBuffer[ScalaDfns#ScalaDfn]): Unit = {
            for (dfn <- scope.dfns;
                 kind = dfn.getKind if kind == ElementKind.CLASS || kind== ElementKind.MODULE
            ) {
              tmpls += dfn.asInstanceOf[ScalaDfns#ScalaDfn]
            }

            scope.subScopes foreach {visit(_, tmpls)}
          }

          private def process(globalx: ScalaGlobal, tmpls: List[ScalaDfns#ScalaDfn]) = {
            tmpls match {
              case Nil =>
                // * source is probably broken and there is no AST
                // * let's generate empty Java stub with simple name equal to file name
                var pkg = FileUtil.getRelativePath(rootFo, fo.getParent)
                if (pkg != null) {
                  pkg = pkg.replace('/', '.')
                  val sb = new StringBuilder
                  if (!pkg.equals("")) { // NOI18N
                    sb.append("package " + pkg + ";") // NOI18N
                  }
                  val name = fo.getName
                  sb.append("public class ").append(name).append(" implements scala.ScalaObject {public int $tag() throws java.rmi.RemoteException {return 0;}}"); // NOI18N
                  //@Todo diable result add till we get everything ok
                  //result.add(file, pkg, file.getName(), sb.toString());
                }
              case _ =>
                val generator = new JavaStubGenerator {val global: globalx.type = globalx}
                import globalx._

                val emptySyms: Array[Symbol] = Array(null, null, null)
                val clzNameToSyms = new HashMap[String, Array[Symbol]] // clzName -> (class, object, trait)

                for (tmpl <- tmpls;
                     sym = tmpl.symbol.asInstanceOf[Symbol] if sym != NoSymbol; // avoid strange file name, for example: <error: class ActorProxy>.java
                     symSName = sym.nameString if symSName.length > 0 && symSName.charAt(0) != '<' // @todo <any>
                ) {
                  val clzName = generator.classSName(sym)
                  val syms = clzNameToSyms.getOrElse(clzName, emptySyms) match {
                    case Array(c, o, t) =>
                      if (sym.isTrait) { // isTrait also isClass, so determine trait before class
                        Array(c, o, sym)
                      } else if (sym.isModule) { // object
                        Array(c, sym, t)
                      } else { // class
                        Array(sym, o, t)
                      }
                  }
                  
                  clzNameToSyms += (clzName -> syms)
                }
                
                for ((clzName, syms) <- clzNameToSyms) {
                  try {
                    val pkgQName = syms find (_ != null) match {
                      case Some(sym) => sym.enclosingPackage match {
                          case null => ""
                          case packaging => packaging.fullName match {
                              case "<empty>" => ""
                              case x => x
                            }
                        }
                      case _ => ""
                    }
                    
                    val javaStub = generator.genClass(pkgQName, clzName, syms)
                   
                    result.add(file, pkgQName, clzName, javaStub)
                  } catch {case ex: FileNotFoundException => Exceptions.printStackTrace(ex)}
                }
            }
          }
        })
    } catch {case ex: ParseException => Exceptions.printStackTrace(ex)}
  }

  private abstract class JavaStubGenerator {
    val global: ScalaGlobal
    import global._
    import definitions._
    
    /** Contants for readability */
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
          javaCode ++= genMemebers(sym, tpe, IS_NOT_OBJECT, IS_NOT_TRAIT)

          val oSym = syms(1)
          val oTpe = tryTpe(oSym)

          if (oTpe != null) {
            javaCode ++= genMemebers(oSym, oTpe, IS_OBJECT, IS_NOT_TRAIT)
          }

        } else {
          javaCode ++= genMemebers(sym, tpe, isObject, isTrait)
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

    private def genMemebers(sym: Symbol, tpe: Type, isObject: Boolean, isTrait: Boolean): String = {
      val javaCode = new StringBuilder
      for (m <- tpe.members if !m.hasFlag(Flags.PRIVATE)) {
        val mTpe = try {
          m.tpe
        } catch {case ex => ScalaGlobal.resetLate(global, ex); null}

        if (mTpe != null && !ScalaUtil.isInherited(sym, m)) {
          val mSName = m.nameString
          m match {
            case _ if m.isTrait || m.isClass || m.isModule =>
              // @todo
            case _ if m.isConstructor =>
              if (!isTrait && !isObject) {
                javaCode ++= modifiers(m) ++= encodeName(sym.nameString)
                // * parameters
                javaCode ++= params(mTpe.params) ++= " {}\n"
              }
            case _ if m.isMethod =>
              val mResTpe = try {
                mTpe.resultType
              } catch {case ex => ScalaGlobal.resetLate(global, ex); null}

              if (mResTpe != null && mSName != "$init$" && mSName != "synchronized") {
                val mResSym = mResTpe.typeSymbol
                
                javaCode ++= modifiers(m)
                if (isObject && !isTrait) javaCode ++= "static final "

                val mResQName = javaSig(mResSym, mResTpe) match {
                  case Some(sig) => sig
                  case None => encodeType(mResSym.fullName)
                }

                javaSig(m, mTpe) match {
                  case Some(sig) =>
                    javaCode ++= sig
                  case None =>
                    // method return type
                    javaCode ++= mResQName ++= " "
                    // method name
                    javaCode ++= encodeName(mSName)
                    // method parameters
                    javaCode ++= params(mTpe.params) ++= " "
                }

                // method body or ";"
                if (!isTrait && !m.hasFlag(Flags.DEFERRED)) {
                  javaCode ++= "{" ++= returnStrOfType(mResQName) ++= "}\n"
                } else {
                  javaCode ++= ";\n"
                }
              }
            case _ if m.isVariable =>
              // do nothing
            case _ if m.isValue =>
              if (!isTrait) {
                javaCode ++= modifiers(m) ++= " "
                val mResTpe = mTpe.resultType
                val mResSym = mResTpe.typeSymbol
                val mResQName = javaSig(mResSym, mResTpe) match {
                  case Some(sig) => sig
                  case None => encodeType(mResSym.fullName)
                }
                javaCode ++= mResQName ++= " " ++= mSName ++= ";\n"
              }
            case _ =>
          }
        }
      }
      return javaCode.toString
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
      } catch {case ex: UnknownSig => log2.warning(sym + " has UnknownSig"); None}
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
}

object ScalaVirtualSourceProvider {
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
