package org.netbeans.modules.scala.editor

import java.io.StringReader
import javax.swing.text.BadLocationException
import javax.swing.text.StyledDocument
import org.netbeans.api.project.FileOwnerQuery
import org.netbeans.modules.editor.indent.spi.Context
import org.netbeans.modules.editor.indent.spi.ExtraLock
import org.netbeans.modules.editor.indent.spi.ReformatTask
import org.netbeans.modules.parsing.api.Source
import org.netbeans.modules.parsing.impl.Utilities
import org.netbeans.modules.scala.core.ScalaMimeResolver
import org.netbeans.modules.scala.editor.options.CodeStyle
import org.netbeans.modules.scala.editor.spi.ScalariformPrefsProvider
import scalariform.formatter.preferences.AlignParameters
import scalariform.formatter.preferences.AlignSingleLineCaseStatements
import scalariform.formatter.preferences.FormattingPreferences
import scalariform.formatter.preferences.IndentSpaces
import scalariform.formatter.preferences.RewriteArrowSymbols
import scalariform.parser.ScalaParserException

class ScalaReformatter(source: Source, context: Context) extends ReformatTask {
  private val doc = context.document.asInstanceOf[StyledDocument]

  val diffOptions = HuntDiff.Options(
    ignoreCase = false,
    ignoreInnerWhitespace = false,
    ignoreLeadingAndtrailingWhitespace = false)

  @throws(classOf[BadLocationException])
  def reformat() {
    val cs = CodeStyle.get(doc)
    val project = FileOwnerQuery.getOwner(source.getFileObject)
    val prefs = if (project != null) {
      val formPrefsProvider = project.getLookup.lookup(classOf[ScalariformPrefsProvider])
      if (formPrefsProvider != null) {
        formPrefsProvider.formatPreferences
      } else {
        ScalaReformatter.defaultPreferences(cs.indentSize)
      }
    } else {
      ScalaReformatter.defaultPreferences(cs.indentSize)
    }

    val indentRegions = context.indentRegions
    java.util.Collections.reverse(indentRegions)
    val regions = indentRegions.iterator

    while (regions.hasNext) {
      val region = regions.next
      val start = region.getStartOffset
      val end = region.getEndOffset
      val length = end - start
      if (start >= 0 && length > 0) {
        val text = doc.getText(start, length)
        val formattedText = try {
          scalariform.formatter.ScalaFormatter.format(text, prefs)
        } catch {
          case ex: ScalaParserException => null
        }

        if (formattedText != null && formattedText.length > 0) {
          val root = doc.getDefaultRootElement

          val diffs = HuntDiff.diff(new StringReader(text), new StringReader(formattedText), diffOptions)
          // reverse the order so we can modify text forward from the end
          java.util.Arrays.sort(diffs, new java.util.Comparator[Diff]() {
            def compare(o1: Diff, o2: Diff) = -o1.firstStart.compareTo(o2.firstStart)
          })

          for (diff <- diffs) {
            diff.tpe match {
              case Diff.ADD =>
                val startLineNo = diff.secondStart
                val startOffset = root.getElement(startLineNo - 1).getStartOffset
                val t = diff.secondText
                doc.insertString(startOffset, t, null)

              case Diff.DELETE =>
                val startLineNo = diff.firstStart
                val endLineNo = diff.firstEnd
                val startOffset = root.getElement(startLineNo - 1).getStartOffset
                val endOffset = root.getElement(endLineNo - 1).getEndOffset
                doc.remove(startOffset, endOffset - startOffset)

              case Diff.CHANGE =>
                val startLineNo = diff.firstStart
                val endLineNo = diff.firstEnd
                val startOffset = root.getElement(startLineNo - 1).getStartOffset
                val endOffset = root.getElement(endLineNo - 1).getEndOffset
                doc.remove(startOffset, endOffset - startOffset)
                val t = diff.secondText
                doc.insertString(startOffset, t, null)
            }
          }
        } else {
          // Cannot be parsed by scalariform, fall back to ScalaFormatter
          new ScalaFormatter(cs, -1).reindent(context)
        }
      }
    }
  }

  def reformatLock: ExtraLock = {
    source.getMimeType match {
      case ScalaMimeResolver.MIME_TYPE => new ExtraLock() {
        def lock() {
          Utilities.acquireParserLock
        }

        def unlock() {
          Utilities.releaseParserLock
        }
      }
      case _ => null
    }
  }

  /**
   * @Note should override it:
   * WARNING [org.netbeans.core.spi.multiview.text.MultiViewCloneableEditor]: Need
   * to override toString() to contain the file name in o.n.api.action.Savable
   * class org.netbeans.modules.csl.core.GsfDataObject$GenericEditorSupport$Environment$SaveSupport
   * with lookup [/home/dcaoyuan/myprjs/nbscala/scala.editor/src/main/scala/org/netbeans/modules/scala/editor/ScalaReformatter.scala@898be162:566f7588,
   * org.netbeans.modules.csl.core.GsfDataNode[name=ScalaReformatter; displayName=ScalaReformatter.scala]
   * [Name=ScalaReformatter, displayName=ScalaReformatter.scala],
   * org.netbeans.modules.csl.core.GsfDataObject@2c6e77c4[/home/dcaoyuan/myprjs/nbscala/scala.editor/src/main/scala/org/netbeans/modules/scala/editor/ScalaReformatter.scala@898be162:566f7588],
   * org.netbeans.modules.csl.core.GsfDataObject$GenericEditorSupport$Environment$SaveSupport@b6d0d86,
   * org.netbeans.core.multiview.MultiViewTopComponentLookup$LookupProxyActionMap@22b1d509,
   * CES: DocumentOpenClose: GenericEditorSupport@1314970463, documentStatus=OPENED, docRef=(GsfDocument@1057563712)]
   */
  override def toString = "Scala Reformatter"
}

object ScalaReformatter {
  /**
   * Reformat task factory produces reformat tasks for the given context.
   * <br/>
   * It should be registered in MimeLookup via xml layer in "/Editors/&lt;mime-type&gt;"
   * folder.
   */
  class Factory extends ReformatTask.Factory {

    /**
     * Create reformatting task.
     *
     * @param context non-null indentation context.
     * @return reformatting task or null if the factory cannot handle the given context.
     */
    def createTask(context: Context): ReformatTask = {
      val source = Source.create(context.document)
      if (source != null) new ScalaReformatter(source, context) else null
    }
  }

  def defaultPreferences(indentSize: Int) = FormattingPreferences()
    .setPreference(IndentSpaces, indentSize)
    .setPreference(RewriteArrowSymbols, false)
    .setPreference(AlignParameters, true)
    .setPreference(AlignSingleLineCaseStatements, true)
}
