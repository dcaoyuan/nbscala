package org.netbeans.modules.scala.sbt.project

import java.io.IOException
import org.netbeans.api.project.Project
import org.netbeans.spi.project.ProjectFactory
import org.netbeans.spi.project.ProjectState
import org.openide.filesystems.FileObject
import org.openide.util.lookup.ServiceProvider

/**
 *  The @ServiceProvider annotation used in the class signature above will cause 
 *  a META-INF/services file to be created when the module is compiled. Within 
 *  that folder, a file named after the fully qualified name of the interface 
 *  will be found, containing the fully qualified name of the implementing class. 
 *  That is the standard JDK mechanism, since JDK 6, for registering implementations 
 *  of interfaces. That is how project types are registered in the NetBeans Plaform.
 *  
 *  @Note This mechanism is applied on Java source file only, so for Scala, we have 
 *  to create META-INF/services file manually.
 *  
 *  @author Caoyuan Deng
 */
@ServiceProvider(service=classOf[ProjectFactory])
class SBTProjectType extends ProjectFactory {
  import SBTProjectType._

  override
  def isProject(projectDirectory: FileObject) = {
    !isMavenProject(projectDirectory) && !isProjectFolder(projectDirectory)
    (getSbtDefinitionFiles(projectDirectory).length > 0 || hasStdScalaSrcDir(projectDirectory))
  }

  @throws(classOf[IOException])  
  override
  def loadProject(dir: FileObject, state: ProjectState): Project = {
    if (isProject(dir)) new SBTProject(dir, state) else null
  }

  @throws(classOf[IOException])
  @throws(classOf[ClassCastException])
  override
  def saveProject(project: Project) {
    // leave unimplemented for the moment
  }

}

object SBTProjectType {
  def getSbtDefinitionFiles(projectDirectory: FileObject): Array[FileObject] = {
    projectDirectory.getChildren filter (x => x.isData && x.getExt == "sbt")
  }
  
  def isMavenProject(projectDirectory: FileObject): Boolean = {
    projectDirectory.getFileObject("pom.xml") != null
  }
  
  def isProjectFolder(projectDirectory: FileObject) = {
    projectDirectory.getNameExt != "project" 
  }
  
  def hasStdScalaSrcDir(projectDirectory: FileObject): Boolean = {
    projectDirectory.getFileObject("src/main/scala") != null
  }
}
