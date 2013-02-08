package org.netbeans.modules.scala.sbt.project

/**
 * 
 * also @see org.netbeans.api.java.project.JavaProjectConstants
 * 
 * @author Caoyuan Deng
 */
object ProjectConstants {

  /**
   * Java package root sources type.
   * @see org.netbeans.api.project.Sources
   */
  val SOURCES_TYPE_JAVA = "java" // NOI18N

  /**
   * Package root sources type for resources, if these are not put together with Java sources.
   * @see org.netbeans.api.project.Sources
   * @since org.netbeans.modules.java.project/1 1.11
   */
  val SOURCES_TYPE_RESOURCES = "resources" // NOI18N


  /**
   * Hint for <code>SourceGroupModifier</code> to create a <code>SourceGroup</code>
   * for main project codebase.
   * @see org.netbeans.api.project.SourceGroupModifier
   * @since org.netbeans.modules.java.project/1 1.24
   */
  val SOURCES_HINT_MAIN = "main" //NOI18N

  /**
   * Hint for <code>SourceGroupModifier</code> to create a <code>SourceGroup</code>
   * for project's tests.
   * @see org.netbeans.api.project.SourceGroupModifier
   * @since org.netbeans.modules.java.project/1 1.24
   */
  val SOURCES_HINT_TEST = "test" //NOI18N

  /**
   * Standard artifact type representing a JAR file, presumably
   * used as a Java library of some kind.
   * @see org.netbeans.api.project.ant.AntArtifact
   */
  val ARTIFACT_TYPE_JAR = "jar" // NOI18N
    
    
  /**
   * Standard artifact type representing a folder containing classes, presumably
   * used as a Java library of some kind.
   * @see org.netbeans.api.project.ant.AntArtifact
   * @since org.netbeans.modules.java.project/1 1.4
   */
  val ARTIFACT_TYPE_FOLDER = "folder" //NOI18N

  /**
   * Standard command for running Javadoc on a project.
   * @see org.netbeans.spi.project.ActionProvider
   */
  val COMMAND_JAVADOC = "javadoc" // NOI18N
    
  /** 
   * Standard command for reloading a class in a foreign VM and continuing debugging.
   * @see org.netbeans.spi.project.ActionProvider
   */
  val COMMAND_DEBUG_FIX = "debug.fix" // NOI18N
  
  val SOURCES_TYPE_SCALA = "scala"
  
  val NAME_SCALASOURCE      = "81ScalaSourceRoot"
  val NAME_SCALATESTSOURCE  = "82ScalaTestSourceRoot"
  val NAME_JAVATESTSOURCE   = "91JavaTestSourceRoot"
  val NAME_JAVASOURCE       = "92JavaSourceRoot"
  val NAME_DEP_PROJECTS     = "95DepProjects"
  val NAME_DEP_LIBRARIES    = "96DepLibraries"
  val NAME_OTHERSOURCE      = "98OtherSourceRoot"
  
  trait FileType 
  case object SOURCE extends FileType
  case object TEST_SOURCE extends FileType
  case object UNKNOWN extends FileType
}

