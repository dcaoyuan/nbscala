/*
 * Copyright (c) 2002-2007, Marc Prud'hommeaux. All rights reserved.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 */
package org.netbeans.modules.scala.console.readline

import java.io.File
import scala.collection.mutable.ArrayBuffer

/**
 *  A file name completor takes the buffer and issues a list of
 *  potential completions.
 *
 *  <p>
 *  This completor tries to behave as similar as possible to
 *  <i>bash</i>'s file name completion (using GNU readline)
 *  with the following exceptions:
 *
 *  <ul>
 *  <li>Candidates that are directories will end with "/"</li>
 *  <li>Wildcard regular expressions are not evaluated or replaced</li>
 *  <li>The "~" character can be used to represent the user's home,
 *  but it cannot complete to other users' homes, since java does
 *  not provide any way of determining that easily</li>
 *  </ul>
 *
 *  <p>TODO</p>
 *  <ul>
 *  <li>Handle files with spaces in them</li>
 *  <li>Have an option for file type color highlighting</li>
 *  </ul>
 *
 *  Transformed to Scala by oliver.guenther
 *
 *  @author  <a href="mailto:mwp1@cornell.edu">Marc Prud'hommeaux</a>
 *  @author oliver.guenther at gg-net dot de
 */

class FileNameCompletor extends Completor {

  private var _pwd: File = _

  def pwd_=(pwd: File) {
    _pwd = pwd
  }

  override def complete(buf: String, cursor: Int, candidates: ArrayBuffer[String]): Int = {
    val buffer = if (buf eq null) "" else buf

    val (translated, dir) = if ((_pwd ne null) && _pwd.exists) {
      (buffer, _pwd)
    } else {
      // special character: ~ maps to the user's home directory
      val translated = if (buffer.startsWith("~" + File.separator)) {
        System.getProperty("user.home") + buffer.substring(1)
      } else if (buffer.startsWith("~")) {
        new File(System.getProperty("user.home")).getParentFile.getAbsolutePath
      } else if (!buffer.startsWith(File.separator)) {
        new File("").getAbsolutePath + File.separator + buffer
      } else {
        buffer
      }

      val f = new File(translated)
      val dir = if (translated.endsWith(File.separator)) {
        f
      } else {
        f.getParentFile
      }

      (translated, dir)
    }

    val entries = if (dir eq null) Array[File]() else dir.listFiles

    try {
      return matchFiles(buffer, translated, entries, candidates)
    } finally {
      candidates.sorted
    }
  }

  /**
   *  Match the specified <i>buffer</i> to the array of <i>entries</i>
   *  and enter the matches into the list of <i>candidates</i>. This method
   *  can be overridden in a subclass that wants to do more
   *  sophisticated file name completion.
   *
   *  @param        buffer                the untranslated buffer
   *  @param        translated        the buffer with common characters replaced
   *  @param        entries                the list of files to match
   *  @param        candidates        the list of candidates to populate
   *
   *  @return  the offset of the match
   */
  def matchFiles(buffer: String, translated: String, entries: Array[File], candidates: ArrayBuffer[String]): Int = {
    if (entries eq null) {
      return -1
    }

    // first pass: just count the matches
    val matches = entries count (_.getAbsolutePath.startsWith(translated))

    // green - executable
    // blue - directory
    // red - compressed
    // cyan - symlink
    entries foreach { entry =>
      if (entry.getAbsolutePath.startsWith(translated)) {
        val name = entry.getName + (if (matches == 1 && entry.isDirectory) File.separator else " ")
        // if (entries [i].isDirectory ()) { name = new ANSIBuffer ().blue (name).toString (); }
        candidates += name
      }
    }
    buffer.lastIndexOf(File.separator) + File.separator.length
  }

}
