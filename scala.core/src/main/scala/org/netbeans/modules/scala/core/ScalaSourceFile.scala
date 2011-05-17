package org.netbeans.modules.scala.core

import java.lang.ref.Reference
import java.lang.ref.WeakReference
import org.netbeans.api.lexer.TokenHierarchy
import org.netbeans.modules.parsing.api.Snapshot
import org.netbeans.modules.parsing.api.Source
import org.openide.filesystems.FileObject
import org.openide.filesystems.FileUtil
import scala.collection.mutable.ArrayBuffer
import scala.tools.nsc.io.AbstractFile
import scala.tools.nsc.io.PlainFile
import scala.tools.nsc.io.VirtualFile
import scala.tools.nsc.util.Position
import scala.tools.nsc.util.SourceFile
import scala.tools.nsc.util.BatchSourceFile
import scala.tools.nsc.util.Chars._

/**
 * 
 * @author Caoyuan Deng
 */
object ScalaSourceFile {
  private val instances = new java.util.WeakHashMap[FileObject, Reference[ScalaSourceFile]]

  def sourceFileOf(fileObject: FileObject) = instances synchronized {
    val sourceRef = instances.get(fileObject)
    var source = if (sourceRef eq null) null else sourceRef.get

    if (source eq null) {
      source = new ScalaSourceFile(fileObject)
      instances.put(fileObject, new WeakReference[ScalaSourceFile](source))
    }
    
    source
  }
}

/**
 * A file whose contents in editing
 * 
 * @author Caoyuan Deng
 */
class ScalaSourceFile private (val fileObject: FileObject) extends SourceFile {
  
  lazy val file: AbstractFile = {
    val file = if (fileObject != null) FileUtil.toFile(fileObject) else null
    if (file != null) new PlainFile(file) else new VirtualFile("<current>", "")
  }
  
  lazy val source = Source.create(fileObject) // if has been created, will return existed one
  
  private var _snapshot: Snapshot = _
  def snapshot = {
    if (_snapshot == null) {
      refreshSnapshot
    }
    _snapshot
  }
  def snapshot_=(snapshot: Snapshot) {
    _snapshot = snapshot
  }
  def refreshSnapshot {
    _snapshot = source.createSnapshot
  }
  
  def tokenHierarchy: TokenHierarchy[_] = snapshot.getTokenHierarchy
  def content =  _snapshot.getText.toString.toCharArray
  
  override def equals(that : Any) = that match {
    case that : BatchSourceFile => file.path == that.file.path && start == that.start
    case that : ScalaSourceFile => file.path == that.file.path && start == that.start
    case _ => false
  }
  override def hashCode = file.path.## + start.##
  def length = content.length
  def start = 0
  def isSelfContained = true

  override def identifier(pos: Position) = 
    if (pos.isDefined && pos.source == this && pos.point != -1) {
      def isOK(c: Char) = isIdentifierPart(c) || isOperatorPart(c)
      Some(new String(content drop pos.point takeWhile isOK))
    } else {
      super.identifier(pos)
    }
  
  def isLineBreak(idx: Int) =
    if (idx >= length) false else {
      val ch = content(idx)
      // don't identify the CR in CR LF as a line break, since LF will do.
      if (ch == CR) (idx + 1 == length) || (content(idx + 1) != LF)
      else isLineBreakChar(ch)
    }

  def calculateLineIndices(cs: Array[Char]) = {
    val buf = new ArrayBuffer[Int]
    buf += 0
    for (i <- 0 until cs.length) if (isLineBreak(i)) buf += i + 1
    buf += cs.length // sentinel, so that findLine below works smoother
    buf.toArray
  }  
  private lazy val lineIndices: Array[Int] = calculateLineIndices(content)  

  def lineToOffset(index : Int): Int = lineIndices(index)

  private var lastLine = 0

  /** Convert offset to line in this source file
   *  Lines are numbered from 0
   */
  def offsetToLine(offset: Int): Int = {
    val lines = lineIndices
    def findLine(lo: Int, hi: Int, mid: Int): Int =
      if (offset < lines(mid)) findLine(lo, mid - 1, (lo + mid - 1) / 2)
    else if (offset >= lines(mid + 1)) findLine(mid + 1, hi, (mid + 1 + hi) / 2)
    else mid
    lastLine = findLine(0, lines.length, lastLine)
    lastLine
  }
}
