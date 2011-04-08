/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
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
 * Contributor(s):
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2007 Sun
 * Microsystems, Inc. All Rights Reserved.
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
 */
package org.netbeans.modules.scala.core

import java.io.{File, IOException}
import java.net.{MalformedURLException, URL}
import java.util.{Properties}

import org.netbeans.spi.java.classpath.support.ClassPathSupport
import org.openide.filesystems.{FileObject, FileUtil}
import org.openide.util.{Exceptions, Utilities}
import scala.tools.nsc.{Settings}
import scala.collection.mutable.ArrayBuffer

/**
 * 
 * @todo Set scala home via installed scala platform
 * 
 * @author Caoyuan Deng
 */
object ScalaHome {

  def getGlobalForStdLib: ScalaGlobal = {
    val scalaHome = getScalaHome
    val scalaHomeDir: File = try {
      val dir = new File(scalaHome)
      dir.getCanonicalFile
    } catch {case ioe: IOException => Exceptions.printStackTrace(ioe); null}

    val scalaLib = new File(scalaHomeDir, "lib") // NOI18N
    assert(scalaLib.exists) //:  '"' + scalaLib.getAbsolutePath() + "\" exists (\"" + descriptor.getCmd() + "\" is not valid Scala executable?)";

    val settings = new Settings
    settings.verbose.value = false

    settings.sourcepath.tryToSet(List(""))

    val nbUserPath = System.getProperty("netbeans.user")

    //println("nbuser:" + nbUserPath)
    settings.outdir.tryToSet(List(nbUserPath))

    // add boot, compile classpath
    val sb = new StringBuilder
    sb.append(System.getProperty("sun.boot.class.path"))
    sb.append(File.pathSeparator)
    sb.append(scalaLib.getAbsolutePath + File.separator + "scala-library.jar")
        
    //System.out.println("boot:" + sb);
    settings.bootclasspath.value = sb.toString

    sb.delete(0, sb.length - 1)
    sb.append(getJavaClassPath)
    sb.append(File.pathSeparator)
    sb.append(toScalaClassPathString(null, scalaLib))

    //System.out.println("comp:" + sb);
    settings.classpath.value = sb.toString

    val global = new ScalaGlobal(settings, ScalaGlobal.dummyReporter) {
      override def onlyPresentation = true
    }

    global
  }

  def getJavaHome: String = {
    System.getProperty("scala.java.home") match { // NOI18N
      case null => System.getProperty("java.home") // NOI18N
      case x => x
    } 
  }

  def getJavaClassPath: String = {
    System.getProperty("java.class.path") match {
      case null => ""
      case x => x
    }
  }

  def getScalaHome: String = {
    val scalaHome = System.getProperty("scala.home") match { // NOI18N
      case null => System.getenv("SCALA_HOME") // NOI18N
      case x => System.setProperty("scala.home", x); x
    }

    if (scalaHome != null) {
      scalaHome
    } else {
      println("Can not found ${SCALA_HOME}/bin/scala, the environment variable SCALA_HOME may be invalid.\nPlease set proper SCALA_HOME first!")
      null
    }
  }

  def getScala: File = {
    val scalaFo: FileObject = getScalaHome match {
      case null =>
        println("Can not found ${SCALA_HOME}/bin/scala, the environment variable SCALA_HOME may be invalid.\nPlease set proper SCALA_HOME first!")
        null
      case scalaHome =>
        val scalaHomeDir = new File(scalaHome)
        if (scalaHomeDir.exists && scalaHomeDir.isDirectory) {
          try {
            val scalaHomeFo = FileUtil.createData(scalaHomeDir)
            val bin = scalaHomeFo.getFileObject("bin")           //NOI18N
            if (Utilities.isWindows) {
              bin.getFileObject("scala", "exe") match {
                case null => bin.getFileObject("scala", "bat")
                case x => x
              }
            } else {
              bin.getFileObject("scala", null)    //NOI18N
            }
          } catch {case ex: IOException => Exceptions.printStackTrace(ex); null}
        } else null
    }
    
    if (scalaFo != null) {
      FileUtil.toFile(scalaFo)
    } else null
  }

  def getSources(scalaHome: File): List[URL] = {
    if (scalaHome != null) {
      try {
        val scalaSrc = new File(scalaHome, "src")    //NOI18N
        if (scalaSrc != null && scalaSrc.exists && scalaSrc.canRead) {
          val srcUrls = new ArrayBuffer[URL]
          for (src <- scalaSrc.listFiles) {
            /**
             * @Note: GSF's indexing does not support jar, zip yet
             */
            if (src.getName.endsWith(".jar") || src.getName.endsWith(".zip")) { // NOI18N
              val url = FileUtil.getArchiveRoot(src.toURI.toURL)
              srcUrls += url
            } else if (src.isDirectory) { // NOI18N
              val url = src.toURI.toURL
              srcUrls += url
            }
          }
          //                    URL url = FileUtil.getArchiveRoot(scalaSrcDir.toURI().toURL());
          //
          //                    //Test for src folder in the src.zip on Mac
          //                    if (Utilities.getOperatingSystem() == Utilities.OS_MAC) {
          //                        try {
          //                            FileObject fo = URLMapper.findFileObject(url);
          //                            if (fo != null) {
          //                                fo = fo.getFileObject("src");    //NOI18N
          //                                if (fo != null) {
          //                                    url = fo.getURL();
          //                                }
          //                            }
          //                        } catch (FileStateInvalidException fileStateInvalidException) {
          //                            Exceptions.printStackTrace(fileStateInvalidException);
          //                        }
          //                    }
          return srcUrls.toList
        }
      } catch {case e: MalformedURLException => Exceptions.printStackTrace(e)}
    }
    Nil
  }

  def getScaladoc(scalaHome: File): List[URL] = {
    if (scalaHome != null) {
      val scalaDoc = new File(scalaHome, "doc") //NOI18N
      if (scalaDoc != null && scalaDoc.isDirectory && scalaDoc.canRead) {
        try {
          return List(scalaDoc.toURI.toURL)
        } catch {case mue: MalformedURLException => Exceptions.printStackTrace(mue)}
      }
    }
    Nil
  }

  def toScalaClassPathString(aextraCp: String, scalaLib: File): String = {
    var extraCp = aextraCp
    val cp = new StringBuilder
    val libs = scalaLib.listFiles

    for (lib <- libs) {
      if (lib.getName.endsWith(".jar")) { // NOI18N
        if (cp.length > 0) {
          cp.append(File.pathSeparatorChar)
        }
        cp.append(lib.getAbsolutePath)
      }
    }

    // Add in user-specified jars passed via SCALA_EXTRA_CLASSPATH

    if (extraCp != null && File.pathSeparatorChar != ':') {
      // Ugly hack - getClassPath has mixed together path separator chars
      // (:) and filesystem separators, e.g. I might have C:\foo:D:\bar but
      // obviously only the path separator after "foo" should be changed to ;
      val p = new StringBuilder
      var pathOffset = 0
      for (i <- 0 until extraCp.length) {
        extraCp.charAt(i) match {
          case ':' if pathOffset != 1 =>
            p.append(File.pathSeparatorChar)
            pathOffset = 0
          case c =>
            pathOffset += 1
            p.append(c)
        }
      }
      extraCp = p.toString
    }

    if (extraCp == null) {
      extraCp = System.getenv("SCALA_EXTRA_CLASSPATH") // NOI18N
    }

    if (extraCp != null) {
      if (cp.length > 0) {
        cp.append(File.pathSeparatorChar)
      }
      //if (File.pathSeparatorChar != ':' && extraCp.indexOf(File.pathSeparatorChar) == -1 &&
      //        extraCp.indexOf(':') != -1) {
      //    extraCp = extraCp.replace(':', File.pathSeparatorChar);
      //}
      cp.append(extraCp)
    }

    cp.toString // NOI18N
  }

  def getStandardLib(scalaHome: File): Option[File] = {
    if (scalaHome != null) {
      try {
        val scalaLib = new File(scalaHome, "lib")    //NOI18N
        if (scalaLib != null && scalaLib.exists && scalaLib.canRead) {
          return scalaLib.listFiles find {jar => jar.getName == "library.jar"}
        }
      }
    }
    None
  }

  def versionString(scalaHome: File): String = {
    val props = new java.util.Properties
    getStandardLib(scalaHome) foreach {
      case x =>
        val cp = ClassPathSupport.createClassPath(Array(FileUtil.toFileObject(x)): _*)
        cp.findResource("/library.properties") match {
          case null =>
          case propFile =>
            val is = propFile.getInputStream
            try {
              props.load(is)
            } catch {case _ =>} finally {
              if (is != null) is.close
            }
        }
    }

    props.getProperty("version.number", "<unknown>")
  }
  
  private def printProperties(props: Properties): Unit = {
    println("===========================")
    val keys = props.keys
    while (keys.hasMoreElements) {
      val key = keys.nextElement
      val value = props.get(key)
      println(key + ": " + value)
    }
  }
}
