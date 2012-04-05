/*
 * Copyright (c) 2002-2007, Marc Prud'hommeaux. All rights reserved.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 */
package org.netbeans.modules.scala.console.readline

// import scala.collection.JavaConversions._

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
import java.io.File
import scala.collection.mutable.ArrayBuffer

class FileNameCompletor extends Completor {
  
  var pwd:File = null;

  def setPwd(pwd:File) {
    this.pwd = pwd;
  }

  override def complete(buf:String, cursor:Int, candidates:ArrayBuffer[String]):Int = {
    val buffer:String = if(buf == null) "" else buf;  

    var translated:String = buffer;

    var dir:File = null;
    if (pwd != null && pwd.exists()) {
      dir = pwd;
    } else {
      // special character: ~ maps to the user's home directory
      if (translated.startsWith("~" + File.separator)) {
        translated = System.getProperty("user.home") + translated.substring(1);
      } else if (translated.startsWith("~")) {
        translated = new File(System.getProperty("user.home")).getParentFile().getAbsolutePath();
      } else if (!(translated.startsWith(File.separator))) {
        translated = new File("").getAbsolutePath() + File.separator + translated;
      }

      val f = new File(translated);

      if (translated.endsWith(File.separator)) {
        dir = f;
      } else {
        dir = f.getParentFile();
      }
    }

    var entries:Array[File] = if(dir == null) Array() else dir.listFiles
    
    try {
      return matchFiles(buffer, translated, entries, candidates);
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
  def matchFiles(buffer:String, translated:String, entries:Array[File], candidates:ArrayBuffer[String]): Int = {
    if (entries == null) {
      return -1;
    }

    // first pass: just count the matches
    val matches = entries.count(_.getAbsolutePath.startsWith(translated))

    // green - executable
    // blue - directory
    // red - compressed
    // cyan - symlink
    entries.foreach{
      (entry) => {
        if (entry.getAbsolutePath().startsWith(translated)) {                
          val name = entry.getName() + (if ((matches == 1) && entry.isDirectory()) {File.separator } else { " " });
          // if (entries [i].isDirectory ()) { name = new ANSIBuffer ().blue (name).toString (); }
          candidates += name;
        }
      }
    }      
    buffer.lastIndexOf(File.separator) + File.separator.length();
  }

  
}
