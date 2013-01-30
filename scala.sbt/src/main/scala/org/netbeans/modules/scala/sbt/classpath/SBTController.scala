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
  mainJavaSrcs: Array[FileObject], 
  testJavaSrcs: Array[FileObject], 
  mainScalaSrcs: Array[FileObject], 
  testScalaSrcs: Array[FileObject], 
  mainCps:  Array[FileObject], 
  testCps:  Array[FileObject]
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
  private var isUnderResolving = false

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
  
  private def getDescriptorFile: FileObject = {
    project.getProjectDirectory.getFileObject(".classpath") match {
      case null => 
        triggerSbtResolution
        null
      case fo => fo
    }
  }

  def triggerSbtResolution {
    sbtResolver.triggerResolution
  }
  
  private def parseClasspathXml(file: File): LibraryEntry = {
    val mainJavaSrcs = new ArrayBuffer[FileObject]()
    val testJavaSrcs = new ArrayBuffer[FileObject]()
    val mainScalaSrcs = new ArrayBuffer[FileObject]()
    val testScalaSrcs = new ArrayBuffer[FileObject]()
    val mainCps = new ArrayBuffer[FileObject]()
    val testCps = new ArrayBuffer[FileObject]()

    val projectDir = project.getProjectDirectory
    val classpath = scala.xml.XML.loadFile(file)
    classpath match {
      case <classpath>{entries @ _*}</classpath> =>
        for (entry @ <classpathentry>{_*}</classpathentry> <- entries) {
          (entry \ "@kind").text match {
            case "src" =>
              val path = (entry \ "@path").text.trim
              val isForTest = path.contains("test")
              val src = projectDir.getFileObject(path)
              if (src != null) {
                val isJava = (src.getPath.split("/") find (_ == "java") isDefined)
                if (isForTest) {
                  if (isJava) {
                    testJavaSrcs += src
                  } else {
                    testScalaSrcs += src
                  }
                } else {
                  if (isJava) {
                    mainJavaSrcs += src
                  } else {
                    mainScalaSrcs += src
                  }
                }
              }
              val output = (entry \ "@output").text.trim
              val classes = projectDir.getFileObject(output)
              if (classes != null) {
                if (isForTest) {
                  testCps += classes
                } else {
                  mainCps += classes
                }
              }
              
            case "lib" =>
              val path = (entry \ "@path").text.trim
              val file = new File(path)
              if (file != null && file.exists) {
                val fo = FileUtil.toFileObject(file)
                mainCps += fo
                testCps += fo
              }
              
            case _ =>
          }
        }
    }
    
    LibraryEntry(mainJavaSrcs.toArray, testJavaSrcs.toArray, mainScalaSrcs.toArray, testScalaSrcs.toArray, mainCps.toArray, testCps.toArray)
  }

  def addPropertyChangeListener(propertyChangeListener: PropertyChangeListener) {
    pcs.addPropertyChangeListener(propertyChangeListener)
  }

  def removePropertyChangeListener(propertyChangeListener: PropertyChangeListener) {
    pcs.removePropertyChangeListener(propertyChangeListener)
  }

  def getResolvedLibraries(scope: String): Array[FileObject] = {
    if (libraryEntry != null) {
      scope match {
        case ClassPath.COMPILE => libraryEntry.mainCps //++ libraryEntry.testCps
        case ClassPath.EXECUTE => libraryEntry.mainCps //++ libraryEntry.testCps
        case ClassPath.SOURCE => libraryEntry.mainJavaSrcs ++ libraryEntry.testJavaSrcs ++ libraryEntry.mainScalaSrcs ++ libraryEntry.mainScalaSrcs
        case ClassPath.BOOT => libraryEntry.mainCps filter {cp =>
            val name = cp.getName
            cp.getExt == "jar" && (
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

  def getSources(tpe: String, test: Boolean): Array[FileObject] = {
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

  private class SBTResolver extends PropertyChangeListener {

    private val resolverTask = RequestProcessor.getDefault().create(new Runnable() {
        def run() {
          val progressHandle = ProgressHandleFactory.createHandle(NbBundle.getMessage(classOf[SBTController], "LBL_Resolving_Progress"))
          progressHandle.start
          SBTConsoleTopComponent.findInstance(project){tc =>
            try {
              val ret = tc.console.runSbtCommand("eclipse")
              println(ret)
              isUnderResolving = false
              pcs.firePropertyChange(SBT_LIBRARY_RESOLVED, null, null)
              Option(ret)
            } catch {
              case ex: IOException => ErrorManager.getDefault.notify(ex); None
            } finally {
              progressHandle.finish
            }
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
      descriptorFile = fe.getFile
    }
  }

  @volatile
  private var areRewritingProjectProperties = false

  private def equal(o1: Object, o2: Object): Boolean = {
    if (o1 == null) o2 == null else o1.equals(o2)
  }

  private def firePropertyChange(propertyName: String, oldValue: Object, newValue: Object) {
    if ((oldValue == null && newValue != null) ||
        (oldValue != null && !oldValue.equals(newValue))) {
      pcs.firePropertyChange(propertyName, oldValue, newValue)
    }
  }
}

object SBTController {
  val DESCRIPTOR_CHANGE = "sbtDescriptorChange"
  val DESCRIPTOR_CONTENT_CHANGE = "sbtDescriptorContentChange"
  val SBT_ENABLE_STATE_CHANGE = "sbtEnableStateChange"
  val SBT_LIBRARY_RESOLVED = "sbtLibraryResolved"  
}