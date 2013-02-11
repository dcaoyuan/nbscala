package org.netbeans.modules.scala.sbt.classpath

import java.beans.PropertyChangeListener
import java.beans.PropertyChangeSupport
import java.io.File
import java.io.IOException
import java.net.MalformedURLException
import org.netbeans.api.java.classpath.ClassPath
import org.netbeans.modules.scala.sbt.console.SBTConsoleTopComponent
import org.netbeans.modules.scala.sbt.project.ProjectConstants
import org.netbeans.modules.scala.sbt.project.SBTProject
import org.openide.ErrorManager
import org.openide.filesystems.FileChangeAdapter
import org.openide.filesystems.FileEvent
import org.openide.filesystems.FileObject
import org.openide.filesystems.FileRenameEvent
import org.openide.filesystems.FileUtil
import org.openide.util.NbBundle
import scala.collection.mutable.ArrayBuffer

case class LibraryEntry(
  mainJavaSrcs:   Array[(File, File)], 
  testJavaSrcs:   Array[(File, File)], 
  mainScalaSrcs:  Array[(File, File)], 
  testScalaSrcs:  Array[(File, File)], 
  mainCps:        Array[File], 
  testCps:        Array[File],
  depPrjs:        Array[File]
)

/**
 *
 * @author Caoyuan Deng
 */
class SBTResolver(project: SBTProject, isEnabled$: Boolean) {
  import SBTResolver._

  private var _sbtConsoleEnabled = false
  private final val pcs = new PropertyChangeSupport(this)
  private final val descriptorFileListener = new DescriptorFileListener
  private val lock = new Object()
  private var _descriptorFile: FileObject = _
  private var _libraryEntry: LibraryEntry = _
  @volatile private var isUnderResolving = false

  isEnabled = isEnabled$
  
  def isEnabled = _sbtConsoleEnabled
  def isEnabled_=(enableState: Boolean) {
    val oldEnableState = _sbtConsoleEnabled
    _sbtConsoleEnabled = enableState
    if (oldEnableState != _sbtConsoleEnabled) {
      pcs.firePropertyChange(SBT_ENABLE_STATE_CHANGE, oldEnableState, _sbtConsoleEnabled)
    }
  }

  def triggerSbtResolution {
    if (!isUnderResolving) {
      isUnderResolving = true
      val rootProject = project.getRootProject
      val commands = List("eclipse gen-netbeans=true skip-parents=false")
      val showMessage = NbBundle.getMessage(classOf[SBTResolver], "LBL_Resolving_Progress")
      SBTConsoleTopComponent.openInstance(rootProject, true, commands, showMessage){result =>
        isUnderResolving = false
        pcs.firePropertyChange(SBT_LIBRARY_RESOLVED, null, null)
      }
    }
  }

  def libraryEntry = {
    if (_libraryEntry == null) {
      descriptorFile = getDescriptorFile
    }
    _libraryEntry
  }
  
  private def descriptorFile: FileObject = _descriptorFile
  private def descriptorFile_=(file: FileObject) {
    if (file != null && file.isData) { 
      try {
        val oldFile = _descriptorFile
        if (oldFile != null) {
          oldFile.removeFileChangeListener(descriptorFileListener)
        }
        file.addFileChangeListener(descriptorFileListener)

        _libraryEntry = parseClasspathXml(FileUtil.toFile(file))
      
        firePropertyChange(DESCRIPTOR_CHANGE, oldFile, file)
      } catch {
        case ex: MalformedURLException => ErrorManager.getDefault.notify(ex)
        case ex: Exception => ErrorManager.getDefault.notify(ex)
      }
    }
  }
  
  @throws(classOf[IOException])
  private def getDescriptorFile: FileObject = {
    project.getProjectDirectory.getFileObject(DescriptorFileName) match {
      case null => 
        // create an empty descriptor file to avoid infinite loop to triggerSbtResolution
        val emptyDescFile = project.getProjectDirectory.createData(DescriptorFileName)
        triggerSbtResolution
        emptyDescFile
      case fo => fo
    }
  }

  private def parseClasspathXml(file: File): LibraryEntry = {
    val mainJavaSrcs  = new ArrayBuffer[(File, File)]()
    val testJavaSrcs  = new ArrayBuffer[(File, File)]()
    val mainScalaSrcs = new ArrayBuffer[(File, File)]()
    val testScalaSrcs = new ArrayBuffer[(File, File)]()
    val mainCps = new ArrayBuffer[File]()
    val testCps = new ArrayBuffer[File]()
    val depPrjs = new ArrayBuffer[File]()

    val projectFo = project.getProjectDirectory
    val projectDir = FileUtil.toFile(projectFo)
    try {
      val classpath = scala.xml.XML.loadFile(file)
      classpath match {
        case <classpath>{ entries @ _* }</classpath> =>
          for (entry @ <classpathentry>{ _* }</classpathentry> <- entries) {
            (entry \ "@kind").text match {
              case "src" =>
                val path = (entry \ "@path").text.trim
                val isTest = (entry \ "@scope").text.trim.equalsIgnoreCase("test")
                val isDepProject = !((entry \ "@exported") isEmpty)
                
                val srcFo = projectFo.getFileObject(path)

                val output = (entry \ "@output").text.trim // classes folder
                val outDir = if (isDepProject) {
                  new File(output)
                } else {
                  new File(projectDir, output)
                }

                if (srcFo != null && !isDepProject) {
                  val isJava = srcFo.getPath.split("/") find (_ == "java") isDefined
                  val srcDir = FileUtil.toFile(srcFo)
                  val srcs = if (isTest) {
                    if (isJava) {
                      testJavaSrcs
                    } else {
                      testScalaSrcs
                    }
                  } else {
                    if (isJava) {
                      mainJavaSrcs
                    } else {
                      mainScalaSrcs
                    }
                  }
                  srcs += srcDir -> outDir
                }
              
                if (isTest) {
                  testCps += outDir
                } else {
                  mainCps += outDir
                }
              
                if (isDepProject) {
                  val base = (entry \ "@base").text.trim
                  val baseDir = new File(base)
                  if (baseDir.exists) {
                    depPrjs += baseDir
                  }
                }
              
              case "lib" =>
                val path = (entry \ "@path").text.trim
                val isTest = (entry \ "@scope").text.trim.equalsIgnoreCase("test")
                val libFile = new File(path)
                if (libFile.exists) {
                  if (isTest) {
                    testCps += libFile
                  } else {
                    mainCps += libFile
                  }
                }
              
              case _ =>
            }
          }
      }
    } catch {
      case ex: Exception => 
    }
    
    LibraryEntry(mainJavaSrcs  map {case (s, o) => FileUtil.normalizeFile(s) -> FileUtil.normalizeFile(o)} toArray,
                 testJavaSrcs  map {case (s, o) => FileUtil.normalizeFile(s) -> FileUtil.normalizeFile(o)} toArray,
                 mainScalaSrcs map {case (s, o) => FileUtil.normalizeFile(s) -> FileUtil.normalizeFile(o)} toArray,
                 testScalaSrcs map {case (s, o) => FileUtil.normalizeFile(s) -> FileUtil.normalizeFile(o)} toArray,
                 mainCps map FileUtil.normalizeFile toArray,
                 testCps map FileUtil.normalizeFile toArray,
                 depPrjs map FileUtil.normalizeFile toArray)
  }

  def addPropertyChangeListener(propertyChangeListener: PropertyChangeListener) {
    pcs.addPropertyChangeListener(propertyChangeListener)
  }

  def removePropertyChangeListener(propertyChangeListener: PropertyChangeListener) {
    pcs.removePropertyChangeListener(propertyChangeListener)
  }

  def getResolvedLibraries(scope: String): Array[File] = {
    if (libraryEntry != null) {
      scope match {
        case ClassPath.COMPILE => libraryEntry.mainCps //++ libraryEntry.testCps
        case ClassPath.EXECUTE => libraryEntry.mainCps //++ libraryEntry.testCps
        case ClassPath.SOURCE => libraryEntry.mainJavaSrcs ++ libraryEntry.testJavaSrcs ++ libraryEntry.mainScalaSrcs ++ libraryEntry.mainScalaSrcs map (_._1)
        case ClassPath.BOOT => libraryEntry.mainCps filter {cp =>
            val name = cp.getName
            name.endsWith(".jar") && (
              name.startsWith("scala-library")  ||
              name.startsWith("scala-compiler") ||  // necessary?
              name.startsWith("scala-reflect")      // necessary?
            )
          }
        case _ => Array()
      }
    } else {
      Array()
    }
  }

  def getSources(tpe: String, test: Boolean): Array[(File, File)] = {
    if (libraryEntry != null) {
      tpe match {
        case ProjectConstants.SOURCES_TYPE_JAVA =>
          if (test) libraryEntry.testJavaSrcs else libraryEntry.mainJavaSrcs
        case ProjectConstants.SOURCES_TYPE_SCALA =>
          if (test) libraryEntry.testScalaSrcs else libraryEntry.mainScalaSrcs
        case _ => Array()
      }
    } else {
      Array()
    }
  }
  
  def getDependenciesProjects: Array[File] = {
    if (libraryEntry != null) {
      libraryEntry.depPrjs
    } else {
      Array()
    }
  }

  private class DescriptorFileListener extends FileChangeAdapter {

    override
    def fileChanged(fe: FileEvent) {
      pcs.firePropertyChange(DESCRIPTOR_CONTENT_CHANGE, null, null)
    }

    override
    def fileDeleted(fe: FileEvent) {
      descriptorFile = null
    }

    override
    def fileRenamed(fe: FileRenameEvent) {
      //descriptorFile = fe.getFile
    }
  }

  private def equal(o1: Object, o2: Object): Boolean = {
    if (o1 == null) o2 == null else o1.equals(o2)
  }

  private def firePropertyChange(propertyName: String, oldValue: Object, newValue: Object) {
    if ((oldValue == null && newValue != null) ||
        (oldValue != null && oldValue != newValue)) {
      pcs.firePropertyChange(propertyName, oldValue, newValue)
    }
  }
}

object SBTResolver {
  val DescriptorFileName = ".classpath_nb"
  
  val DESCRIPTOR_CHANGE = "sbtDescriptorChange"
  val DESCRIPTOR_CONTENT_CHANGE = "sbtDescriptorContentChange"
  val SBT_ENABLE_STATE_CHANGE = "sbtEnableStateChange"
  val SBT_LIBRARY_RESOLVED = "sbtLibraryResolved"  
}