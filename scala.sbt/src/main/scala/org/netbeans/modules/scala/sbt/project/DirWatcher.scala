package org.netbeans.modules.scala.sbt.project

import java.io.IOException
import java.util.TimerTask
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener
import javax.swing.event.EventListenerList
import org.openide.filesystems.FileObject
import scala.collection.mutable
import scala.collection.JavaConversions._

/**
 *
 * @author Caoyuan Deng
 */
@throws(classOf[IOException])
class DirWatcher(fileName: String) extends TimerTask {
  private val listeners = new EventListenerList()
  private var folders = Set[FileObject]()

  protected val NOT_SURE = Long.MinValue
  protected val fileToLastModified = new mutable.HashMap[FileObject, Long]()

  /**
   * Scan all directories to get current files.
   */
  private def scanFiles: mutable.HashMap[FileObject, Long] = {
    val fileToTime = new mutable.HashMap[FileObject, Long]()

    try {
      for {
        folder <- folders
        file = folder.getFileObject(fileName) if file != null
      } {
        file.lastModified.getTime match {
          case NOT_SURE =>
          case time     => fileToTime(file) = time
        }
      }

    } catch {
      case ex: Exception =>
    }

    fileToTime
  }

  def run {
    check
  }

  def check {
    val newFileToTime = scanFiles

    val checkedFiles = mutable.Set[FileObject]()
    for (file <- newFileToTime.keys) {
      checkedFiles += file

      fileToLastModified.get(file) match {
        case None => // new file
          for (newTime <- newFileToTime.get(file)) {
            fileToLastModified(file) = newTime
            fireChange(FileAdded(file, newTime))
          }
        case Some(oldTime) => // modified file
          for (newTime <- newFileToTime.get(file)) {
            if (oldTime < newTime) {
              fileToLastModified(file) = newTime
              fireChange(FileModified(file, newTime))
            } else {
              // Ingore the old one and current one, only care the newer one.
            }
          }
      }
    }

    // deleted files
    val deletedFiles = fileToLastModified -- checkedFiles
    for ((file, time) <- deletedFiles) {
      fileToLastModified -= file
      fireChange(FileDeleted(file, time))
    }
  }

  def addChangeListener(folder: FileObject, l: ChangeListener) {
    if (!folders.contains(folder)) {
      folders += folder
      check
    }
    listeners.add(classOf[ChangeListener], l)
  }

  def removeChangeListener(l: ChangeListener) {
    listeners.remove(classOf[ChangeListener], l)
  }

  protected def fireChange(evt: FileChangeEvent) {
    listeners.getListeners(classOf[ChangeListener]) foreach (_.stateChanged(evt))
  }
}

sealed abstract class FileChangeEvent(file: FileObject, lastModified: Long) extends ChangeEvent()
final case class FileAdded(file: FileObject, lastModified: Long) extends FileChangeEvent(file, lastModified)
final case class FileDeleted(file: FileObject, lastModified: Long) extends FileChangeEvent(file, lastModified)
final case class FileModified(file: FileObject, lastModified: Long) extends FileChangeEvent(file, lastModified)