package org.netbeans.modules.scala.maven

import java.io.File
import org.netbeans.api.project.Project
import org.netbeans.modules.maven.api.PluginPropertyUtils
import org.netbeans.modules.scala.editor.spi.ScalariformPrefsProvider
import org.netbeans.spi.project.LookupProvider
import org.openide.filesystems.FileAttributeEvent
import org.openide.filesystems.FileChangeListener
import org.openide.filesystems.FileEvent
import org.openide.filesystems.FileRenameEvent
import org.openide.filesystems.FileUtil
import org.openide.util.Lookup
import org.openide.util.lookup.Lookups
import scalariform.formatter.preferences.AllPreferences
import scalariform.formatter.preferences.FormattingPreferences
import scalariform.formatter.preferences.IFormattingPreferences
import scalariform.formatter.preferences.PreferenceDescriptor

object ScalariformPrefs {
  val groupId = "org.scalariform"
  val artifactId = "scalariform-maven-plugin"
  val multiproperty = ""
  val goal = "format"
}

class ScalariformPrefs(project: Project) extends ScalariformPrefsProvider {
  import ScalariformPrefs._

  private var preference: IFormattingPreferences = _

  private val watchingFiles: Array[File] = {
    val pomFo = project.getProjectDirectory.getFileObject("pom.xml")
    val pomFile = FileUtil.normalizeFile(FileUtil.toFile(pomFo))
    if (pomFile != null) Array(pomFile.getParentFile) else Array()
  }
  private val openedProjectUpdater = new Updater(this, watchingFiles)

  def formatPreferences = {
    if (preference == null) {
      var prefs = Map[PreferenceDescriptor[_], Any]()
      for (pref <- AllPreferences.preferences) {
        PluginPropertyUtils.getPluginProperty(project, groupId, artifactId, pref.key, goal, null) match {
          case null =>
          case v =>
            pref.preferenceType.parseValue(v) match {
              case Right(value) => prefs += (pref -> value)
              case Left(ex)     =>
            }
        }
      }
      preference = new FormattingPreferences(prefs)
    }

    preference
  }

  def resetCache() {
    preference = null
  }

  def attachUpdater() {
    openedProjectUpdater.attachAll()
  }

  def detachUpdater() {
    openedProjectUpdater.detachAll()
  }

}

class Updater(prefs: ScalariformPrefs, fileProvider: Array[File]) extends FileChangeListener {

  private var filesToWatch: Array[File] = _
  private var lastTime = 0L

  def fileAttributeChanged(fileAttributeEvent: FileAttributeEvent) {
  }

  def fileChanged(fileEvent: FileEvent) {
    if (lastTime < fileEvent.getTime) {
      lastTime = System.currentTimeMillis
      prefs.resetCache
    }
  }

  def fileDataCreated(fileEvent: FileEvent) {
    if (lastTime < fileEvent.getTime) {
      lastTime = System.currentTimeMillis
      prefs.resetCache
    }
  }

  def fileDeleted(fileEvent: FileEvent) {
    lastTime = System.currentTimeMillis
    prefs.resetCache
  }

  def fileFolderCreated(fileEvent: FileEvent) {
    //TODO possibly remove this fire.. watch for actual path..
    //            prefs.resetCache
  }

  def fileRenamed(fileRenameEvent: FileRenameEvent) {
  }

  def attachAll(): Unit = synchronized {
    val toWatch = fileProvider
    this.filesToWatch = toWatch
    toWatch foreach (FileUtil.addFileChangeListener(this, _))

  }

  def detachAll(): Unit = synchronized {
    if (filesToWatch != null) {
      val toWatch = filesToWatch
      filesToWatch = null
      toWatch foreach (FileUtil.removeFileChangeListener(this, _))
    }
  }
}

class ScalariformPrefsLookupProvider extends LookupProvider {
  def createAdditionalLookup(lookup: Lookup): Lookup = {
    val project = lookup.lookup(classOf[Project])
    Lookups.fixed(new ScalariformPrefs(project))
  }
}

