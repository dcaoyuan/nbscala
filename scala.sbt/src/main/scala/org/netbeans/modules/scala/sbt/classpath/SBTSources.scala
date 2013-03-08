package org.netbeans.modules.scala.sbt.classpath

import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import javax.swing.event.ChangeListener
import org.netbeans.modules.scala.core.ProjectResources
import org.netbeans.modules.scala.sbt.project.ProjectConstants
import org.netbeans.api.project.Project
import org.netbeans.api.project.SourceGroup
import org.netbeans.api.project.Sources
import org.netbeans.modules.scala.sbt.project.SBTResolver
import org.netbeans.spi.project.support.GenericSources
import org.openide.filesystems.FileObject
import org.openide.filesystems.FileUtil
import org.openide.util.ChangeSupport
import org.openide.util.NbBundle

/**
 * This is for query on project. (ClassPath.SOURCE is on fileobject)
 * 
 * @author Caoyuan Deng
 */
class SBTSources(project: Project) extends Sources {
  private val cs = new ChangeSupport(this)
  private lazy val sbtResolver = {
    val x = project.getLookup.lookup(classOf[SBTResolver])

    x.addPropertyChangeListener(new PropertyChangeListener() {
        def propertyChange(evt: PropertyChangeEvent) {
          evt.getPropertyName match {
            case SBTResolver.DESCRIPTOR_CHANGE => 
              cs.fireChange
            case _ =>
          }
        }
      })
    
    x
  }  

  import ProjectConstants._
  
  override 
  def getSourceGroups(tpe: String): Array[SourceGroup] = {
    tpe match {
      case Sources.TYPE_GENERIC =>
        // It's necessary for project's PhysicalView (in Files window), 
        // @see org.netbeans.modules.project.ui.PhysicalView#createNodesForProject(Project)
        val projectDir = project.getProjectDirectory
        Array(GenericSources.group(project, projectDir, projectDir.getNameExt, projectDir.getNameExt, null, null))
      
      case ProjectResources.SOURCES_TYPE_JAVA | ProjectResources.SOURCES_TYPE_SCALA | ProjectResources.SOURCES_TYPE_MANAGED =>
        val mainSrcs = maybeAddGroup(tpe, false)
        val testSrcs = maybeAddGroup(tpe, true)
        // We should keep the order Array(main, test), @see org.netbeans.modules.scala.core.ProjectResourcs#findMainAndTestSrcs
        if (mainSrcs.length > 0 && testSrcs.length > 0) {
          Array(mainSrcs(0), testSrcs(0))
        } else if (mainSrcs.length > 0) {
          Array(mainSrcs(0))
        } else {
          Array()
        }
      
      case _ =>
        Array()
    }
  }

  private def maybeAddGroup(tpe: String, isTest: Boolean): Array[SourceGroup] = {
    val roots = if (sbtResolver != null) {
      sbtResolver.getSources(tpe, isTest) map (x => FileUtil.toFileObject(x._1))
    } else {
      // best try
      tpe match {
        case ProjectResources.SOURCES_TYPE_JAVA =>
          Array(project.getProjectDirectory.getFileObject("src/" + (if (isTest) "test" else "main") + "/java"))
        case ProjectResources.SOURCES_TYPE_SCALA =>
          Array(project.getProjectDirectory.getFileObject("src/" + (if (isTest) "test" else "main") + "/scala"))
        case _ => 
          Array[FileObject]()
      }
    }
    
    val name = tpe match {
      case ProjectResources.SOURCES_TYPE_JAVA =>
        if (isTest) NAME_JAVATESTSOURCE else NAME_JAVASOURCE
      case ProjectResources.SOURCES_TYPE_SCALA =>
        if (isTest) NAME_SCALATESTSOURCE else NAME_SCALASOURCE
      case ProjectResources.SOURCES_TYPE_MANAGED =>
        if (isTest) NAME_MANAGEDTESTSOURCE else NAME_MANAGEDSOURCE
      case _ => NAME_OTHERSOURCE
    }
    
    val displayName = tpe match {
      case ProjectResources.SOURCES_TYPE_JAVA =>
        if (isTest) NbBundle.getMessage(classOf[SBTSources], "SG_Test_JavaSources") else NbBundle.getMessage(classOf[SBTSources], "SG_JavaSources")
      case ProjectResources.SOURCES_TYPE_SCALA =>
        if (isTest) NbBundle.getMessage(classOf[SBTSources], "SG_Test_ScalaSources") else NbBundle.getMessage(classOf[SBTSources], "SG_ScalaSources")
      case ProjectResources.SOURCES_TYPE_MANAGED =>
        if (isTest) NbBundle.getMessage(classOf[SBTSources], "SG_Test_ManagedSources") else NbBundle.getMessage(classOf[SBTSources], "SG_ManagedSources")
      case _ => NbBundle.getMessage(classOf[SBTSources], "SG_OtherSources")
    }
    
    for (root <- roots if root != null) yield GenericSources.group(project, root, name, displayName, null, null)
  }

  override 
  def addChangeListener(l: ChangeListener) {
    cs.addChangeListener(l)
  }

  override 
  def removeChangeListener(l: ChangeListener) {
    cs.removeChangeListener(l)
  }
}
