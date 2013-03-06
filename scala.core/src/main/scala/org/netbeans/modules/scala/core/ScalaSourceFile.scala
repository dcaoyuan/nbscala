package org.netbeans.modules.scala.core

import java.lang.ref.Reference
import java.lang.ref.WeakReference
import org.netbeans.api.lexer.TokenHierarchy
import org.netbeans.editor.BaseDocument
import org.netbeans.modules.parsing.api.Snapshot
import org.netbeans.modules.parsing.api.Source
import org.openide.filesystems.FileObject
import org.openide.filesystems.FileUtil
import scala.collection.mutable.ArrayBuffer
import scala.tools.nsc.io.AbstractFile
import scala.tools.nsc.io.PlainFile
import scala.tools.nsc.io.VirtualFile
import scala.reflect.internal.util.Position
import scala.reflect.internal.util.SourceFile
import scala.reflect.internal.util.BatchSourceFile
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
    val javaIoFile = if (fileObject ne null) FileUtil.toFile(fileObject) else null
    if (javaIoFile ne null) new PlainFile(javaIoFile) else new VirtualFile("<current>", "")
  }
  
  lazy val source = Source.create(fileObject) // if has been created, will return existed one
  
  private var _snapshot: Snapshot = _
  def snapshot = {
    if (_snapshot eq null) {
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
  
  def doc = source.getDocument(false).asInstanceOf[BaseDocument]
  def tokenHierarchy: TokenHierarchy[_] = snapshot.getTokenHierarchy
  def content = snapshot.getText.toString.toCharArray
  
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

  /** 
   * Convert offset to line in this source file
   * Lines are numbered from 0
   */
  def offsetToLine(offset: Int): Int = {
    val lines = lineIndices
    def findLine(lo: Int, hi: Int): Int = {
      val mid = (lo + hi) / 2
      if (lo < hi) {
        if (offset < lines(mid)) findLine(lo, mid)
        else if (offset > lines(mid)) findLine(mid + 1, hi)
        else mid
      } else if (lo == hi) {
        mid 
      } else mid
    }
    findLine(0, lines.length - 1) // use (lines.length - 1) instead of lines.length here
  }
  
  override 
  def hashCode = file.file.hashCode

  override 
  def equals(that : Any) = that match {
    case that : BatchSourceFile => file.path == that.file.path && start == that.start
    case that : ScalaSourceFile => file.file == that.file.file // compare underlying java io file.
    case _ => false
  }

}
