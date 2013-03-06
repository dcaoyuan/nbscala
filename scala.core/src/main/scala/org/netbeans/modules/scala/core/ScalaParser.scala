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
  private val log = Logger.getLogger(this.getClass.getName)

  private var _result: ScalaParserResult = null

  /**
   * Called when some task needs some result of parsing. Task parameter contains 
   * UserTask, or SchedulerTask instance, that requests Parser.Result.
   * 
   * @param task - A task asking for parsing result.
   * @return Result of parsing or null.
   */
  @throws(classOf[ParseException])
  override 
  def getResult(task: Task): Parser.Result = {
    assert(_result != null, "getResult() called prior parse(.) or parse(.) returned a null result") //NOI18N
    _result
  }

  override 
  def cancel(reason: Parser.CancelReason, event: SourceModificationEvent) {
    reason match {
      case Parser.CancelReason.SOURCE_MODIFICATION_EVENT => 
        val fo = event.getModifiedSource.getFileObject
        log.info("Get cancel request for " + fo.getNameExt + ", sourceChanged=" + event.sourceChanged)
        // We'll cancelSemantic only when the event is saying sourceChanged, since only in this case, we can expect a
        // followed parse(..) call. Under other cases there may be no followed parse(..) call.
        // Or even worse, in this case, there may still no followed parse(..) call, anyway, 
        // we have to make strict condition to cancel 
        if (event.sourceChanged && _result != null) _result.tryCancelSemantic
      case _ =>
    }
  }

  /**
   * @see http://forums.netbeans.org/topic43738.html
   * As far as I know, the Parser.parse does not need to actually parse 
   * anything, it can defer the parsing until getResult method is called. The 
   * task there is used to create a background channel to pass additional 
   * language-specific information from the client (Task creator) to the 
   * parser. This is used in Java to pass the Java ClassPath. The Tasks 
   * should IMO be passed into the getResult method in the order the they are 
   * executed (for embedded cases the situation is more complex as I guess 
   * one might get one task more than once as different embeddings are being 
   * processed). 
   */
  @throws(classOf[ParseException])
  override 
  def parse(snapshot: Snapshot, task: Task, event: SourceModificationEvent) {
    val fo = event.getModifiedSource.getFileObject
    log.info("Ready to parse " + fo.getNameExt + ", prev parserResult is " + _result)
    // The SourceModificationEvent seems set sourceModified=true even when switch between editor windows, 
    // so one solution is try to avoid redundant parsing by checking if the content is acutally modified,
    // but we cannot rely on that, since other source may have been changed and cause the current file must
    // refect to this change to make sure if the reference to that file is still correct, 
    // i.e. we need re-parsing it anyway. 
    // But we can make the actual parsing procedure lazily in parser result. @see ScalaParserResult#toSemanticed
    _result = ScalaParserResult(snapshot)
  }

  /**
   * Not used here anymore. keep here for reference.
   */
  private def isIndexUpToDate(fo: FileObject): Boolean = {
    val srcCp = ClassPath.getClassPath(fo, ClassPath.SOURCE)
    if (srcCp ne null) {
      srcCp.getRoots find (FileUtil.isParentOf(_, fo)) foreach {root =>
        val timeStamps = TimeStamps.forRoot(root.toURL, false)
        return (timeStamps ne null) && timeStamps.checkAndStoreTimestamp(fo, FileUtil.getRelativePath(root, fo))
      }
    }
    
    false
  }

  override 
  def addChangeListener(changeListener: ChangeListener) {
    // no-op, we don't support state changes
  }

  override 
  def removeChangeListener(changeListener: ChangeListener) {
    // no-op, we don't support state changes
  }

  private final class Factory extends ParserFactory {
    override 
    def createParser(snapshots: java.util.Collection[Snapshot]): Parser = new ScalaParser
  }
}
