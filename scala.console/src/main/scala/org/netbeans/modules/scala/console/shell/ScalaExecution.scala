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

package org.netbeans.modules.scala.console.shell

import java.io.File
import java.io.IOException
import org.netbeans.api.project.Project
import org.netbeans.api.project.ui.OpenProjects
import org.netbeans.modules.scala.core.ProjectResources
import org.openide.DialogDisplayer
import org.openide.NotifyDescriptor
import org.openide.filesystems.FileObject
import org.openide.filesystems.FileUtil
import org.openide.modules.InstalledFileLocator
//import org.openide.modules.Places
import org.openide.util.Exceptions
import org.openide.util.Utilities
import scala.collection.mutable.ArrayBuffer

/**
 *
 * @author Caoyuan Deng
 */
object ScalaExecution {

  private val SCALA_MAIN_CLASS = "scala.tools.nsc.MainGenericRunner" 
  private val SBT_MAIN_CLASS = "sbt.xMain"
  
  private val JVM_DEBUG = "-Xdebug -Xnoagent -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000"
    
  def getSbtArgs(sbtHome: String): (String, Array[String]) = {
    val args = new ArrayBuffer[String]()

    val executable = getJavaHome + File.separator + "bin" + File.separator + "java" // NOI18N   
    // XXX Do I need java.exe on Windows?

    args += "-Xmx512M"
    args += "-Xss1M"
    args += "-XX:+CMSClassUnloadingEnabled"
    args += "-XX:MaxPermSize=256M"
        
    args += "-Dsbt.log.noformat=true"
    /** 
     * @Note:
     * jline's UnitTerminal will hang in my Mac OS, when call "stty(...)", why? 
     * Also, from Scala-2.7.1, jline is used for scala shell, we should 
     * disable it here by add "-Djline.terminal=jline.UnsupportedTerminal"?
     * And jline may cause terminal unresponsed after netbeans quited.
     */
    //args += "-Djline.terminal=jline.UnsupportedTerminal" 
    args += "-Djline.WindowsTerminal.directConsole=false"
            
    // TODO - turn off verifier?

    // Main class
    args += "-jar"
    args += getSbtLaunchJar(sbtHome).getAbsolutePath // NOI18N

    // Application arguments follow
        
    (executable, args.toArray)
  }

  def getScalaArgs(scalaHome: String, project: Project = null): (String, Array[String]) = {
    val args = new ArrayBuffer[String]()

    val executable = getJavaHome + File.separator + "bin" + File.separator + "java"

    // additional execution flags specified in the Scala startup script:
    args += "-Xverify:none"
    args += "-da" 
            
    System.getenv("SCALA_EXTRA_VM_ARGS") match {
      case null =>
        args += "-Xmx512m"
        args += "-Xss1024k"
      case extraArgs =>
        if (!extraArgs.contains("-Xmx")) { 
          args += "-Xmx512m"
        }
        if (!extraArgs.contains("-Xss")) {
          args += "-Xss1024k"
        }
        args ++= Utilities.parseParameters(extraArgs)
    }

    val scalaHomeDir = try {
      new File(scalaHome).getCanonicalFile
    } catch  {
      case ex: IOException => Exceptions.printStackTrace(ex); null
    }

    val scalaLib = new File(scalaHomeDir, "lib") 
    args += "-Xbootclasspath/a:" + mkClassPathString(scalaLib)  // @Note jline is a must for for interactive scala console

    if (project != null) {
      val (projectBootCpStr, projectExecCpStr) = ProjectResources.getExecuteClassPathString(project)
      args += "-classpath" 
      args += projectExecCpStr
    } else {
      args += "-classpath" 
      args += computeScalaClassPath(null, scalaLib)
    }
    
    args += "-Dscala.home=" + scalaHomeDir // is this a must for interactive scala console
    args += "-Dscala.usejavacp=true"  // this is a must for -classpath can be imported under interative shell
            
    /** 
     * @Note:
     * jline's UnitTerminal will hang in my Mac OS, when call "stty(...)", why? 
     * Also, from Scala-2.7.1, jline is used for scala shell, we should 
     * disable it here by add "-Djline.terminal=jline.UnsupportedTerminal"
     */
    //args += "-Djline.terminal=scala.tools.jline.UnsupportedTerminal" 
    args += "-Djline.WindowsTerminal.directConsole=false" 

    // Under Windows, you may get:
    //      Failed to created JLineReader: java.lang.NoClassDefFoundError: Could not initialize class org.fusesource.jansi.internal.Kernel32
    //      Falling back to SimpleReader.
    // which may be caused by varies reason, @see http://www.scala-lang.org/node/9795
    // Since we are a pesudo terminal (the ConsoleTerminal), we don't care the behavior of
    // different terminals, or, we can pretend we are a unix terminal
    // But, for jline.UnixTerminal, it will try to send stty command to init the behavior, so
    // we have to cheat it by replace jline.sh and jline.stty as a no harmful and quick command,
    // here's what we do: 
    // @see jline.UnitTerminal#init and TerminalLineSettings
    System.getProperty("os.name").toLowerCase match {
      case os if os.indexOf("windows") != -1 => 
        args += "-Djline.terminal=unix" 
        args += "-Djline.sh=cmd"
        // add switch "/c" here to "Carries out the command specified by string and then terminates",
        // so, the process will terminate and not hang on "process.waitFor()" @see cmd /?
        args += "-Djline.stty=/c\\ echo"  
      case _ =>
        args += "-Djline.terminal=unix" 
        args += "-Djline.sh=sh"
        args += "-Djline.stty=echo"  // avoid to send stty command, it's not necessary for pseudo termnial
    }
    
    // main class
    args += SCALA_MAIN_CLASS 
    // application arguments follow
        
    (executable, args.toArray)
  }
  
  def isWindows: Boolean = System.getProperty("os.name").toLowerCase match {
    case os if os.indexOf("windows") != -1 => true
    case _ => false
  }
  
  def getJavaHome: String = {
    System.getProperty("scala.java.home") match {  
      case null => System.getProperty("java.home") 
      case x => x
    }
  }
  
  def getScalaHome: String = {
    System.getenv("SCALA_HOME") match { 
      case null => 
        val d = new NotifyDescriptor.Message(
          "SCALA_HOME environment variable may not be set, or is invalid.\n" +
          "Please set SCALA_HOME first!", NotifyDescriptor.INFORMATION_MESSAGE
        )
        DialogDisplayer.getDefault().notify(d)
        null
      case scalaHome => System.setProperty("scala.home", scalaHome); scalaHome
    }
  }

  def getSbtHome: String = {
    //System.getProperty("user.home", "~") + File.separator + "myapps" + File.separator + "sbt"
    System.getProperty("netbeans.sbt.home") match {
      case null => null
      case x => x
    }
  }
  
  def getScala: File = {
    var scalaFo: FileObject = null
    val scalaHome = getScalaHome
    if (scalaHome != null) {
      val scalaHomeDir = new File(getScalaHome)
      if (scalaHomeDir.exists && scalaHomeDir.isDirectory) {
        try {
          val scalaHomeFo = FileUtil.createData(scalaHomeDir)
          val bin = scalaHomeFo.getFileObject("bin")             //NOI18N
          if (Utilities.isWindows) {
            scalaFo = bin.getFileObject("scala", "exe")
            if (scalaFo eq null) {
              scalaFo = bin.getFileObject("scala", "bat")
            }
          } else {
            scalaFo = bin.getFileObject("scala", null)    //NOI18N
          }
        } catch  {
          case ex : IOException => Exceptions.printStackTrace(ex)
        }
      }
    }
    if (scalaFo != null) {
      FileUtil.toFile(scalaFo)
    } else {
      val d = new NotifyDescriptor.Message(
        "Can not found ${SCALA_HOME}/bin/scala, the environment variable SCALA_HOME may be invalid.\n" +
        "Please set proper SCALA_HOME first!", NotifyDescriptor.INFORMATION_MESSAGE
      )
      DialogDisplayer.getDefault().notify(d)
      null
    }
  }
    
  def getSbtLaunchJar(sbtHome: String = null): File = {
    sbtHome match {
      case null | "" => getEmbeddedSbtLaunchJar
      case _ => 
        val jar = new File(sbtHome) match {
          case homeDir if (homeDir.exists && homeDir.isDirectory) =>
            try {
              val homeFo = FileUtil.createData(homeDir)
              val binDir = homeFo.getFileObject("bin")
              binDir.getFileObject("sbt-launch", "jar")
            } catch  {
              case ex: IOException => Exceptions.printStackTrace(ex); null
            }
          case _ => null
        }
        if (jar ne null) {
          FileUtil.toFile(jar)
        } else {
          val msg = new NotifyDescriptor.Message(
            "Can not found" + sbtHome + "/bin/sbt-launch.jar\n" +
            "Please set proper sbt home first!", NotifyDescriptor.INFORMATION_MESSAGE)
          DialogDisplayer.getDefault().notify(msg)
          null
        }
    }
  }
  
  private def getEmbeddedSbtLaunchJar: File = {
    val sbtDir = InstalledFileLocator.getDefault.locate("modules/ext/org.scala-sbt",  "org.netbeans.libs.sbt", false)
    if (sbtDir != null && sbtDir.exists && sbtDir.isDirectory) {
      sbtDir.listFiles find {jar =>
        val name = jar.getName
        (name == "sbt-launch" || name.startsWith("sbt-launch-")) && name.endsWith(".jar")
      } foreach {return _}
    }
    
    null
  }
  
  private def mkClassPathString(dir: File, jarNames: Array[String]): String = {
    val dirPath = dir.getAbsolutePath 
    jarNames map (dirPath + File.separator + _) filter {fileName => 
      try {
        val file = new File(fileName)
        file.exists && file.canRead
      } catch {
        case ex: Throwable => false
      }
    } mkString File.pathSeparator
  }

  private def mkClassPathString(dir: File): String = {
    dir.listFiles filter (_.getName.endsWith("jar")) map (_.getAbsolutePath) mkString (File.pathSeparator)
  }
  
  /** Package-private for unit test. */
  def computeScalaClassPath(extraCp: String, scalaLib: File): String = {
    val sb = new StringBuilder()
    val libs = scalaLib.listFiles

    libs filter (_.getName.endsWith("jar")) foreach {lib => 
      if (sb.length > 0) sb.append(File.pathSeparatorChar)
      sb.append(lib.getAbsolutePath)
    }

    // Add in user-specified jars passed via SCALA_EXTRA_CLASSPATH

    val p = new StringBuilder()
    if ((extraCp ne null) && File.pathSeparatorChar != ':') {
      // Ugly hack - getClassPath has mixed together path separator chars
      // (:) and filesystem separators, e.g. I might have C:\foo:D:\bar but
      // obviously only the path separator after "foo" should be changed to ;
      var pathOffset = 0
      extraCp foreach {c => 
        if (c == ':' && pathOffset != 1) {
          p += File.pathSeparatorChar
          pathOffset = 0           
        } else {
          pathOffset += 1
          p += c
        }
      }
      
    }

    if (p.isEmpty && (System.getenv("SCALA_EXTRA_CLASSPATH") ne null)) {
      p ++= System.getenv("SCALA_EXTRA_CLASSPATH") // NOI18N
    }

    if (!p.isEmpty) {
      if (sb.length > 0) {
        sb.append(File.pathSeparatorChar)
      }
      //if (File.pathSeparatorChar != ':' && extraCp.indexOf(File.pathSeparatorChar) == -1 &&
      //        extraCp.indexOf(':') != -1) {
      //    extraCp = extraCp.replace(':', File.pathSeparatorChar);
      //}
      sb.append(p)
    }
    if (Utilities.isWindows)  "\"" + sb.toString + "\""  else sb.toString // NOI18N
  }
  
  def getMainProjectWorkPath: File = {
    var pwd: File = null
    val mainProject = OpenProjects.getDefault.getMainProject
    if (mainProject != null) {
      var fo = mainProject.getProjectDirectory
      if (!fo.isFolder) {
        fo = fo.getParent
      }
      pwd = FileUtil.toFile(fo)
    }
    if (pwd == null) {
      val userHome = System.getProperty("user.home")
      pwd = new File(userHome, "project")
      if (pwd.exists) {
        pwd = if (pwd.isDirectory) new File(userHome) else pwd
      } else {
        pwd.mkdir
        pwd
      }
    }
    pwd
  }
     
}
