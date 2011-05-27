/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */
package org.netbeans.modules.scala.core

import java.util.logging.Logger
import javax.swing.event.ChangeListener
import org.netbeans.api.java.classpath.ClassPath
import org.netbeans.modules.parsing.api.{Snapshot, Task}
import org.netbeans.modules.parsing.impl.indexing.TimeStamps
import org.netbeans.modules.parsing.spi.{ParseException, Parser, ParserFactory, SourceModificationEvent}
import org.openide.filesystems.{FileObject, FileUtil}

/**
 * 
 * @author Caoyuan Deng
 */
class ScalaParser extends Parser {
  import ScalaParser._

  private val log = Logger.getLogger(this.getClass.getName)

  private var _result: ScalaParserResult = _

  @throws(classOf[ParseException])
  override def getResult(task: Task): Parser.Result = {
    assert(_result != null, "getResult() called prior parse() or parse() returned a null result") //NOI18N
    _result
  }

//  override def cancel {
//    if (_result != null) _result.cancelSemantic
//  }
  
  override def cancel(reason: Parser.CancelReason, event: SourceModificationEvent) {
    reason match {
      case Parser.CancelReason.SOURCE_MODIFICATION_EVENT => 
        log.fine("Get cancel request from event: " + event.getModifiedSource + ", sourceChanged=" + event.sourceChanged)
        // We'll cancelSemantic only when the event is saying sourceChanged, since only in this case, we can expect a
        // follow up parse(..) call. There are other cases there won't be a sfollow up parse(..) call.
        if (event.sourceChanged && _result != null) {
          _result.cancelSemantic
        }
      case _ =>
    }
  }

  @throws(classOf[ParseException])
  override def parse(snapshot: Snapshot, task: Task, event: SourceModificationEvent) {
    // The SourceModificationEvent seems set sourceModified=true even when switch between editor windows, 
    // so one solution is try to avoid redundant parsing by checking if the content is acutally modified,
    // but we cannot rely on that, since other source may have been changed and cause the current file must
    // refect to this change to make sure if the reference to that file is still correct, i.e. we need re-parsing
    // it anyway.
    log.fine("Request to parse " + event.getModifiedSource.getFileObject.getNameExt + ", prev parserResult=" + _result)
    log.info("Ready to parse " + snapshot.getSource.getFileObject.getNameExt)
    //  will lazily do true parsing in ScalaParserResult
    _result = new ScalaParserResult(snapshot)
  }

  private def isIndexUpToDate(fo: FileObject): Boolean = {
    val srcCp = ClassPath.getClassPath(fo, ClassPath.SOURCE)
    if (srcCp != null) {
      srcCp.getRoots find {x => FileUtil.isParentOf(x, fo)} foreach {root =>
        val timeStamps = TimeStamps.forRoot(root.getURL, false)
        return timeStamps != null && timeStamps.checkAndStoreTimestamp(fo, FileUtil.getRelativePath(root, fo))
      }
    }
    
    false
  }

  override def addChangeListener(changeListener: ChangeListener) {
    // no-op, we don't support state changes
  }

  override def removeChangeListener(changeListener: ChangeListener) {
    // no-op, we don't support state changes
  }

  private final class Factory extends ParserFactory {
    override def createParser(snapshots: java.util.Collection[Snapshot]): Parser = new ScalaParser
  }
}

object ScalaParser {

  private var version: Long = _
  private val profile = Array(0.0f, 0.0f)

  /** Attempts to sanitize the input buffer */
  abstract class Sanitize
  object Sanitize {
    /** Only parse the current file accurately, don't try heuristics */
    case object NEVER extends Sanitize
    /** Perform no sanitization */
    case object NONE extends Sanitize
    /** Try to remove the trailing . at the caret line */
    case object EDITED_DOT extends Sanitize
    /** Try to remove the trailing . at the error position, or the prior
     * line, or the caret line */
    case object ERROR_DOT extends Sanitize
    /** Try to cut out the error line */
    case object ERROR_LINE extends Sanitize
    /** Try to cut out the current edited line, if known */
    case object EDITED_LINE extends Sanitize
    /** Attempt to add an "end" to the end of the buffer to make it compile */
    case object MISSING_END extends Sanitize
  }
}
