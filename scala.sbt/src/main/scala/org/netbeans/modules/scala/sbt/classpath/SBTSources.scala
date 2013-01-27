package org.netbeans.modules.scala.sbt.classpath

import scala.collection.mutable.ArrayBuffer

import javax.swing.event.ChangeListener
import org.netbeans.modules.scala.sbt.project.ProjectConstants
import org.netbeans.api.project.Project
import org.netbeans.api.project.SourceGroup
import org.netbeans.api.project.Sources
import org.netbeans.spi.project.SourceGroupModifierImplementation
import org.netbeans.spi.project.support.GenericSources
import org.openide.filesystems.FileObject
import org.openide.util.NbBundle

/**
 * 
 * @author Caoyuan Deng
 */
class SBTSources(project: Project) extends Sources with SourceGroupModifierImplementation {
  
  override 
  def getSourceGroups(tpe: String): Array[SourceGroup] = {
    tpe match {
      case ProjectConstants.SOURCES_TYPE_JAVA =>
        val groups = new ArrayBuffer[SourceGroup]()
        maybeAddGroup(groups, tpe, false)
        maybeAddGroup(groups, tpe, true)
        groups.toArray
      case ProjectConstants.SOURCES_TYPE_SCALA =>
        val groups = new ArrayBuffer[SourceGroup]()
        maybeAddGroup(groups, tpe, false)
        maybeAddGroup(groups, tpe, true)
        groups.toArray
      case _ =>
        Array[SourceGroup]()
    }
  }

  private def maybeAddGroup(groups: ArrayBuffer[SourceGroup], tpe: String, test: Boolean) {
    val sbtController = project.getLookup.lookup(classOf[SBTResourceController])
    val roots = if (sbtController != null) {
      sbtController.getSources(tpe, test)
    } else {
      // best try
      tpe match {
        case ProjectConstants.SOURCES_TYPE_JAVA =>
          Array(project.getProjectDirectory.getFileObject("src/" + (if (test) "test" else "main") + "/java"))
        case ProjectConstants.SOURCES_TYPE_SCALA =>
          Array(project.getProjectDirectory.getFileObject("src/" + (if (test) "test" else "main") + "/scala"))
        case _ => 
          Array[FileObject]()
      }
    }
    
    val name = tpe match {
      case ProjectConstants.SOURCES_TYPE_JAVA =>
        if (test) ProjectConstants.NAME_JAVATESTSOURCE else ProjectConstants.NAME_JAVASOURCE
      case ProjectConstants.SOURCES_TYPE_SCALA =>
        if (test) ProjectConstants.NAME_SCALATESTSOURCE else ProjectConstants.NAME_SCALASOURCE
      case _ => 
        ProjectConstants.NAME_OTHERSOURCE
    }
    
    val displayName = tpe match {
      case ProjectConstants.SOURCES_TYPE_JAVA =>
        if (test) NbBundle.getMessage(classOf[SBTSources], "SG_Test_JavaSources") else NbBundle.getMessage(classOf[SBTSources], "SG_JavaSources")
      case ProjectConstants.SOURCES_TYPE_SCALA =>
        if (test) NbBundle.getMessage(classOf[SBTSources], "SG_Test_ScalaSources") else NbBundle.getMessage(classOf[SBTSources], "SG_ScalaSources")
      case _ =>
        NbBundle.getMessage(classOf[SBTSources], "SG_OtherSources")
    }
    
    groups ++= {for (root <- roots if root != null) yield GenericSources.group(project, root, name, displayName, null, null)}
  }

  override 
  def addChangeListener(listener: ChangeListener) {
    // XXX listen to creation/deletion of roots
  }

  override 
  def removeChangeListener(listener: ChangeListener ) {}

  override 
  def createSourceGroup(tpe: String, hint: String): SourceGroup = {
    // XXX this looks weird, cannot tell where something is created..
    if (!canCreateSourceGroup(tpe, hint)) {
      return null
    }
    val groups = new ArrayBuffer[SourceGroup]()
    maybeAddGroup(groups, tpe, ProjectConstants.SOURCES_HINT_TEST == hint)
    if (groups.isEmpty) null else groups(0)
  }

  override 
  def canCreateSourceGroup(tpe: String, hint: String): Boolean = {
    (ProjectConstants.SOURCES_TYPE_JAVA == tpe  || ProjectConstants.SOURCES_TYPE_SCALA == tpe) &&
    (ProjectConstants.SOURCES_HINT_MAIN == hint || ProjectConstants.SOURCES_HINT_TEST == hint)
  }
}
