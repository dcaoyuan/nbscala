package org.netbeans.modules.scala.editor.spi

import scalariform.formatter.preferences.IFormattingPreferences

trait ScalariformPrefsProvider {
  def formatPreferences: IFormattingPreferences
}
