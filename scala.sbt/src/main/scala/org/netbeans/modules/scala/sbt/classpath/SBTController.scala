package org.netbeans.modules.scala.sbt.classpath

import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import java.beans.PropertyChangeSupport
import java.io.File
import java.io.IOException
import java.net.MalformedURLException
import org.netbeans.api.java.classpath.ClassPath
import org.netbeans.api.progress.ProgressHandleFactory
import org.netbeans.api.project.Project
import org.netbeans.modules.scala.sbt.console.SBTConsoleTopComponent
import org.netbeans.modules.scala.sbt.project.ProjectConstants
import org.openide.ErrorManager
import org.openide.filesystems.FileChangeAdapter
import org.openide.filesystems.FileEvent
import org.openide.filesystems.FileObject
import org.openide.filesystems.FileRenameEvent
import org.openide.filesystems.FileUtil
import org.openide.util.NbBundle
import org.openide.util.RequestProcessor
import scala.collection.mutable.ArrayBuffer

case class LibraryEntry(
  mainJavaSrcs:  Array[File], 
  testJavaSrcs:  Array[File], 
  mainScalaSrcs: Array[File], 
  testScalaSrcs: Array[File], 
  mainCps: Array[File], 
  testCps: Array[File],
  depPrjs: Array[File]
)

/**
 *
 * @author Caoyuan Deng
 */
class SBTController(project: Project, isEnabled$: Boolean) {
  import SBTController._

  private var _sbtConsoleEnabled = false
  private val sbtResolver = new SBTResolver()
  private final val pcs = new PropertyChangeSupport(this)
  private final val descriptorFileListener = new DescriptorFileListener
  private val lock = new Object()
  private var _descriptorFile: FileObject = _
  private var _libraryEntry: LibraryEntry = _
  @volatile private var isUnderResolving = false

  isEnabled = isEnabled$
  addPropertyChangeListener(sbtResolver)

  def isEnabled = _sbtConsoleEnabled
  def isEnabled_=(enableState: Boolean) {
    val oldEnableState = _sbtConsoleEnabled
    _sbtConsoleEnabled = enableState
    if (oldEnableState != _sbtConsoleEnabled) {
      pcs.firePropertyChange(SBT_ENABLE_STATE_CHANGE, oldEnableState, _sbtConsoleEnabled)
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

  def triggerSbtResolution {
    sbtResolver.triggerResolution
  }
  
  private def parseClasspathXml(file: File): LibraryEntry = {
    val mainJavaSrcs = new ArrayBuffer[File]()
    val testJavaSrcs = new ArrayBuffer[File]()
    val mainScalaSrcs = new ArrayBuffer[File]()
    val testScalaSrcs = new ArrayBuffer[File]()
    val mainCps = new ArrayBuffer[File]()
    val testCps = new ArrayBuffer[File]()
    val depPrjs = new ArrayBuffer[File]()

    val projectFo = project.getProjectDirectory
    val projectDir = FileUtil.toFile(projectFo)
    val classpath = scala.xml.XML.loadFile(file)
    classpath match {
      case <classpath>{ entries @ _* }</classpath> =>
        for (entry @ <classpathentry>{ _* }</classpathentry> <- entries) {
          (entry \ "@kind").text match {
            case "src" =>
              val path = (entry \ "@path").text.trim
              val isTest = (entry \ "@scope").text.trim.equalsIgnoreCase("test")
              val isProject = !((entry \ "@exported") isEmpty)
              val srcFo = projectFo.getFileObject(path)
              if (srcFo != null && !isProject) {
                val isJava = srcFo.getPath.split("/") find (_ == "java") isDefined
                val srcDir = FileUtil.toFile(srcFo)
                if (isTest) {
                  if (isJava) {
                    testJavaSrcs += srcDir
                  } else {
                    testScalaSrcs += srcDir
                  }
                } else {
                  if (isJava) {
                    mainJavaSrcs += srcDir
                  } else {
                    mainScalaSrcs += srcDir
                  }
                }
              }
              
              val output = (entry \ "@output").text.trim // classes folder
              val outputDir = if (isProject) {
                new File(output)
              } else {
                new File(projectDir, output)
              }
              if (isTest) {
                testCps += outputDir
              } else {
                mainCps += outputDir
              }
              
              if (isProject) {
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
    
    LibraryEntry(mainJavaSrcs  map FileUtil.normalizeFile toArray,
                 testJavaSrcs  map FileUtil.normalizeFile toArray,
                 mainScalaSrcs map FileUtil.normalizeFile toArray,
                 testScalaSrcs map FileUtil.normalizeFile toArray,
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
        case ClassPath.SOURCE => libraryEntry.mainJavaSrcs ++ libraryEntry.testJavaSrcs ++ libraryEntry.mainScalaSrcs ++ libraryEntry.mainScalaSrcs
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

  def getSources(tpe: String, test: Boolean): Array[File] = {
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

  private class SBTResolver extends PropertyChangeListener {

    private val resolverTask = RequestProcessor.getDefault().create(new Runnable() {
        def run() {
          val progressHandle = ProgressHandleFactory.createHandle(NbBundle.getMessage(classOf[SBTController], "LBL_Resolving_Progress"))
          progressHandle.start
          SBTConsoleTopComponent.openInstance(project, true, "eclipse gen-netbeans=true"){result =>
            isUnderResolving = false
            pcs.firePropertyChange(SBT_LIBRARY_RESOLVED, null, null)
            progressHandle.finish
            println(result)
          }
        }
      }
    )

    def propertyChange(evt: PropertyChangeEvent) {
      evt.getPropertyName match {
        case DESCRIPTOR_CHANGE | DESCRIPTOR_CONTENT_CHANGE =>
          //triggerResolution
        case _ =>
      }
    }

    def triggerResolution {
      if (!isUnderResolving) {
        isUnderResolving = true
        resolverTask.run
      }
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

object SBTController {
  val DescriptorFileName = ".classpath_nb"
  
  val DESCRIPTOR_CHANGE = "sbtDescriptorChange"
  val DESCRIPTOR_CONTENT_CHANGE = "sbtDescriptorContentChange"
  val SBT_ENABLE_STATE_CHANGE = "sbtEnableStateChange"
  val SBT_LIBRARY_RESOLVED = "sbtLibraryResolved"  
}