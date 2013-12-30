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
import java.util.logging.Level
import java.util.logging.Logger
import org.netbeans.modules.csl.api.ElementKind
import org.netbeans.modules.java.preprocessorbridge.spi.VirtualSourceProvider
import org.netbeans.modules.parsing.api.ParserManager
import org.netbeans.modules.parsing.api.ResultIterator
import org.netbeans.modules.parsing.api.Source
import org.netbeans.modules.parsing.api.UserTask
import org.netbeans.modules.parsing.impl.indexing.TimeStamps
import org.netbeans.modules.parsing.spi.ParseException
import org.openide.filesystems.{ FileUtil, FileObject }
import org.openide.util.Exceptions

import org.netbeans.api.language.util.ast.{ AstScope }
import org.netbeans.modules.scala.core.ScalaGlobal
import org.netbeans.modules.scala.core.ScalaParserResult
import org.netbeans.modules.scala.core.ast.ScalaDfns

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import scala.collection.JavaConversions._

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
  private final val log = Logger.getLogger(this.getClass.getName)
  log.info("Successfully created a " + this.getClass.getSimpleName)

  /**
   * @Todo
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
    val timeStamps = TimeStamps.forRoot(root.toURL, false)

    for (file <- files) {
      val fo = FileUtil.toFileObject(file)
      // JavaIndexer tends to reindex all dependent (via VirtualSources calculating) files
      // when dependee source file is modified, it's not neccessary for VirtualSource in my opinion,
      // so, filter them here:
      val isUpToDate = timeStamps.checkAndStoreTimestamp(fo, FileUtil.getRelativePath(root, fo))
      if (!isUpToDate) {
        translateFile(fo, root, result)
      }
    }
  }

  private def translateFile(fo: FileObject, srcRootFileObj: FileObject, result: VirtualSourceProvider.Result) {
    if (fo == null) return

    try {
      val source = Source.create(fo)
      /** @Note: do not use UserTask to parse it? which may cause "refershing workspace" */
      // FIXME can we move this out of task (?)
      // TODO Clean up the anonymous class
      ParserManager.parse(java.util.Collections.singleton(source), new UserTask {

        @throws(classOf[ParseException])
        override def run(ri: ResultIterator) {
          val t0 = System.currentTimeMillis

          val pr = ri.getParserResult.asInstanceOf[ScalaParserResult]
          val global = pr.global
          val rootScope = pr.rootScope
          val tmpls = new ArrayBuffer[ScalaDfns#ScalaDfn]
          visit(tmpls)(rootScope)
          process(global, tmpls.toList)

          log.info("Translated %s in %d milliseconds.".format(fo.getNameExt, System.currentTimeMillis - t0))
        }

        private def visit(tmpls: ArrayBuffer[ScalaDfns#ScalaDfn])(scope: AstScope) {
          for {
            dfn <- scope.dfns
            kind = dfn.getKind if kind == ElementKind.CLASS || kind == ElementKind.MODULE
          } {
            tmpls += dfn.asInstanceOf[ScalaDfns#ScalaDfn]
          }

          scope.subScopes foreach visit(tmpls)
        }

        private def process(globalx: ScalaGlobal, tmpls: List[ScalaDfns#ScalaDfn]) = {
          tmpls match {
            case Nil =>
              // * source is probably broken and there is no AST
              // * let's generate empty Java stub with simple name equal to file name
              var pkg = FileUtil.getRelativePath(srcRootFileObj, fo.getParent)
              if (pkg ne null) {
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
              globalx.askForResponse { () =>
                val generator = new JavaStubGenerator { val global: globalx.type = globalx }
                import globalx._

                val emptySyms: Array[Symbol] = Array(null, null, null)
                val clzNameToSyms = new HashMap[String, Array[Symbol]] // clzName -> (class, object, trait)

                for {
                  tmpl <- tmpls
                  sym = tmpl.symbol.asInstanceOf[Symbol] if sym != NoSymbol // avoid strange file name, for example: <error: class ActorProxy>.java
                  symSName = sym.nameString if symSName.length > 0 && symSName.charAt(0) != '<' // @todo <any>
                } {
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
                    val pkgQName = syms find (_ ne null) match {
                      case Some(sym) => sym.enclosingPackage match {
                        case null => ""
                        case packaging => packaging.fullName match {
                          case "<empty>" => ""
                          case x         => x
                        }
                      }
                      case _ => ""
                    }

                    val javaStub = generator.genClass(pkgQName, clzName, syms)

                    result.add(FileUtil.toFile(fo), pkgQName, clzName, javaStub)
                  } catch {
                    case ex: FileNotFoundException => Exceptions.printStackTrace(ex)
                  }
                }

              } get match {
                case Left(_)   =>
                case Right(ex) => log.log(Level.WARNING, ex.getMessage, ex)
              }
          }
        }
      })
    } catch { case ex: ParseException => Exceptions.printStackTrace(ex) }
  }
}
