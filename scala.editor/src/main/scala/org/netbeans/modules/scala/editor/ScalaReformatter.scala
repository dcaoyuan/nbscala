package org.netbeans.modules.scala.editor

import javax.swing.text.BadLocationException
import org.netbeans.modules.editor.indent.spi.Context
import org.netbeans.modules.editor.indent.spi.ExtraLock
import org.netbeans.modules.editor.indent.spi.ReformatTask
import org.netbeans.modules.parsing.api.Source
import org.netbeans.modules.parsing.impl.Utilities
import org.netbeans.modules.scala.core.ScalaMimeResolver
import org.netbeans.modules.scala.editor.options.CodeStyle
import scalariform.formatter.preferences.AlignParameters
import scalariform.formatter.preferences.FormattingPreferences
import scalariform.formatter.preferences.IndentSpaces
import scalariform.parser.ScalaParserException

class ScalaReformatter(source: Source, context: Context) extends ReformatTask {
  private val doc = context.document

  @throws(classOf[BadLocationException])
  def reformat() {
    val cs = CodeStyle.get(doc)
    val indentRegions = context.indentRegions
    java.util.Collections.reverse(indentRegions)
    val regions = indentRegions.iterator
    val preferences = FormattingPreferences.setPreference(IndentSpaces, cs.indentSize).setPreference(AlignParameters, true)
    while (regions.hasNext) {
      val region = regions.next
      val start = region.getStartOffset
      val end = region.getEndOffset
      val length = end - start
      if (start >= 0 && length > 0) {
        val text = doc.getText(start, length)
        val formattedText = try {
          scalariform.formatter.ScalaFormatter.format(text, preferences)
        } catch {
          case ex: ScalaParserException ⇒ null
        }
        if (formattedText != null && formattedText.length > 0) {
          doc.remove(start, length)
          doc.insertString(start, formattedText, null)
        }
      }
    }
  }

  def reformatLock: ExtraLock = {
    if (ScalaMimeResolver.MIME_TYPE == source.getMimeType) null else new ExtraLock() {
      def lock() {
        Utilities.acquireParserLock
      }

      def unlock() {
        Utilities.releaseParserLock
      }
    }
  }

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
}