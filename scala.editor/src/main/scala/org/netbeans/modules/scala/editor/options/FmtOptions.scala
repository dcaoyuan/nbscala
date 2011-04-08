/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved.
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
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2008 Sun Microsystems, Inc.
 */
package org.netbeans.modules.scala.editor.options

import java.awt.{Container, Rectangle}
import java.awt.event.{ActionEvent, ActionListener}
import java.beans.PropertyChangeSupport
import java.util.prefs.{AbstractPreferences, BackingStoreException, Preferences}
import javax.swing.{ComboBoxModel, JCheckBox, JComboBox, JComponent, JEditorPane, JPanel, JTextField, JViewport}
import javax.swing.event.{DocumentEvent, DocumentListener}
import javax.swing.text.EditorKit

import org.netbeans.api.editor.mimelookup.{MimeLookup, MimePath}
import org.netbeans.api.editor.settings.SimpleValueNames

import org.netbeans.modules.options.editor.spi.{PreferencesCustomizer, PreviewProvider}
import org.netbeans.modules.scala.core.ScalaMimeResolver
import org.netbeans.modules.scala.editor.{ScalaFormatter}
import org.netbeans.spi.options.OptionsPanelController
import org.openide.util.{Exceptions, HelpCtx, NbBundle}

import scala.collection.mutable.ArrayBuffer

object FmtOptions {

  val expandTabToSpaces = SimpleValueNames.EXPAND_TABS
  val tabSize = SimpleValueNames.TAB_SIZE
  val spacesPerTab = SimpleValueNames.SPACES_PER_TAB
  val indentSize = SimpleValueNames.INDENT_SHIFT_WIDTH
  val rightMargin = SimpleValueNames.TEXT_LIMIT_WIDTH
  val continuationIndentSize = "continuationIndentSize" //NOI18N
  val reformatComments = "reformatComments" //NOI18N
  val indentXml = "indentXml" //NOI18N

  var codeStyleProducer: CodeStyleProducer = _
  var lastValues: Preferences = _

  private var kitClass: Class[_ <: EditorKit] = _
  private val DEFAULT_PROFILE = "default" // NOI18N

  private val mimePath = ScalaMimeResolver.MIME_TYPE

  def getDefaultAsInt(key: String): Int = {
    defaults.get(key).get.toInt
  }

  def getDefaultAsBoolean(key: String): Boolean = {
    defaults.get(key).get.toBoolean
  }

  def getDefaultAsString(key: String): String = {
    defaults.get(key).get
  }

  def getPreferences: Preferences = {
    MimeLookup.getLookup(mimePath).lookup(classOf[Preferences])
  }

  def getGlobalExpandTabToSpaces: Boolean = {
    getPreferences.getBoolean(SimpleValueNames.EXPAND_TABS, getDefaultAsBoolean(SimpleValueNames.EXPAND_TABS))
  }

  def getGlobalTabSize: Int = {
    getPreferences.getInt(SimpleValueNames.TAB_SIZE, getDefaultAsInt(SimpleValueNames.TAB_SIZE))
  }

  // Ruby needs its own indent size; the global "4" isn't a good match
  //    public static int getGlobalIndentSize() {
  //        org.netbeans.editor.Formatter f = (org.netbeans.editor.Formatter)Settings.getValue(getKitClass(), "formatter");
  //        if (f != null)
  //            return f.getShiftWidth();
  //        return getDefaultAsInt(indentSize);
  //    }

  def getGlobalRightMargin: Int = {
    getPreferences.getInt(SimpleValueNames.TEXT_LIMIT_WIDTH, getDefaultAsInt(SimpleValueNames.TEXT_LIMIT_WIDTH))
  }

  def getKitClass: Class[_ <: EditorKit] = {
    if (kitClass == null) {
      kitClass = MimeLookup.getLookup(MimePath.get(mimePath)).lookup(classOf[EditorKit]) match {
        case null => classOf[EditorKit]
        case kit => kit.getClass.asInstanceOf[Class[EditorKit]]
      }
    }
    kitClass
  }

  /**
   * @Note CloneableEditorSupport.getEditorKit(String) causes scalac's all `pos` wrong?
   * so I just copied most of the code from CloneableEditorSupport.getEditorKit(String)
   */
  def getEditorKit(mimePath: String): EditorKit = {
    val kit = MimeLookup.getLookup(MimePath.parse(mimePath)).lookup(classOf[EditorKit]) match {
      case null => MimeLookup.getLookup(MimePath.parse("text/plain")).lookup(classOf[EditorKit])
      case x => x
    }

    // * Don't use the prototype instance straightaway
    kit.clone.asInstanceOf[EditorKit]
  }

  def flush {
    try {
      getPreferences.flush
    } catch {case e: BackingStoreException => Exceptions.printStackTrace(e)}
  }

  def getCurrentProfileId: String = {
    return DEFAULT_PROFILE
  }

  def createCodeStyle(p: Preferences): CodeStyle = {
    CodeStyle.getDefault(null)
    codeStyleProducer.create(p)
  }

  def isInteger(optionID: String): Boolean = {
    val value = defaults.get(optionID).get

    try {
      value.toInt
      true
    } catch {case e: NumberFormatException => false}
  }

  def getLastValue(optionID: String): String = {
    val p = if (lastValues == null) getPreferences else lastValues
    p.get(optionID, getDefaultAsString(optionID))
  }

  // Private section ---------------------------------------------------------
  // * The defaults will be override by default preferences of all languages and defaultPreferences in
  // * [Editors]/[text]/[x-scala]/[Preferences]/[Defaults]
  private var defaults = Map(SimpleValueNames.TEXT_LIMIT_WIDTH -> "80",
                             SimpleValueNames.EXPAND_TABS -> "true",
                             SimpleValueNames.TAB_SIZE -> "2",
                             SimpleValueNames.INDENT_SHIFT_WIDTH -> "2",
                             continuationIndentSize -> "2",
                             reformatComments -> "false",
                             indentXml -> "true"
  )


  // Customizer section ---------------------------------------------------------
  object Customizer {
    val OPTION_ID = "org.netbeans.modules.scala.editor.options.FormattingOptions.ID"
    private val LOAD = 0
    private val STORE = 1
    private val ADD_LISTENERS = 2
  }
  
  class Customizer(preferences: Preferences,
                   id: String,
                   panel: JPanel,
                   apreviewText: String,
                   forcedOptions: Array[String]*
  ) extends ActionListener
       with DocumentListener
       with PreviewProvider
       with PreferencesCustomizer {

    import Customizer._

    private val previewText = if (apreviewText != null) apreviewText else NbBundle.getMessage(classOf[FmtOptions], "SAMPLE_Default")
    private var isChanged = false
    private val components = new ArrayBuffer[JComponent]
    private var previewPane: JEditorPane = _

    private val pcs = new PropertyChangeSupport(this)

    // * Scan the panel for its components
    scan(panel, components)

    // * Initialize the preview preferences
    val forcedPrefs = new PreviewPreferences
    forcedOptions foreach {option => forcedPrefs.put(option(0), option(1))}
    val previewPrefs = new ProxyPreferences(preferences, forcedPrefs)

    // * Load and hook up all the components
    loadFrom(preferences)
    addListeners

    protected def addListeners {
      scan(ADD_LISTENERS, null)
    }

    protected def loadFrom(p: Preferences) {
      scan(LOAD, p)
    }
    
    protected def storeTo(p: Preferences) {
      scan(STORE, p)
    }

    protected def notifyChanged() {
      storeTo(preferences)
      refreshPreview
    }

    def getPreviewComponent: JComponent = {
      if (previewPane == null) {
        previewPane = new JEditorPane
        previewPane.getAccessibleContext.setAccessibleName(NbBundle.getMessage(classOf[FmtOptions], "AN_Preview")) //NOI18N
        previewPane.getAccessibleContext.setAccessibleDescription(NbBundle.getMessage(classOf[FmtOptions], "AD_Preview")) //NOI18N
        previewPane.putClientProperty("HighlightsLayerIncludes", "^org\\.netbeans\\.modules\\.editor\\.lib2\\.highlighting\\.SyntaxHighlighting$") //NOI18N
        previewPane.setEditorKit(getEditorKit(mimePath))
        previewPane.setEditable(false)
      }
      previewPane
    }

    def refreshPreview {
      val jep = getPreviewComponent.asInstanceOf[JEditorPane]
      try {
        val rm = previewPrefs.getInt(SimpleValueNames.TEXT_LIMIT_WIDTH, getDefaultAsInt(SimpleValueNames.TEXT_LIMIT_WIDTH))
        jep.putClientProperty("TextLimitLine", rm) //NOI18N
      } catch {case e: NumberFormatException =>}

      var rm = 30
      try {
        rm = previewPrefs.getInt(SimpleValueNames.TEXT_LIMIT_WIDTH, getDefaultAsInt(SimpleValueNames.TEXT_LIMIT_WIDTH))

        // Estimate text line in preview pane

        val pc = if (previewPane.getParent.isInstanceOf[JViewport]) {
          previewPane.getParent.asInstanceOf[JViewport]
        } else previewPane
        val font = pc.getFont
        val metrics = pc.getFontMetrics(font)
        val cw = metrics.charWidth('x')
        if (cw > 0) {
          val nrm = pc.getWidth / cw
          if (nrm > 3) {
            rm = nrm - 2
          }
        }

        //pane.putClientProperty("TextLimitLine", rm); // NOI18N
      } catch {case e:NumberFormatException =>}

      jep.setIgnoreRepaint(true)
      jep.setText(previewText)

      val codeStyle = CodeStyle.get(previewPrefs)
      val formatter = new ScalaFormatter(codeStyle, rm)
      formatter.reindent(null, jep.getDocument, 0, jep.getDocument.getLength, null, false)

      jep.setIgnoreRepaint(false)
      jep.scrollRectToVisible(new Rectangle(0, 0, 10, 10))
      jep.repaint(100)
    }

    def changed {
      if (!isChanged) {
        isChanged = true
        pcs.firePropertyChange(OptionsPanelController.PROP_CHANGED, false, true)
      }
      pcs.firePropertyChange(OptionsPanelController.PROP_VALID, null, null)
    }

    // ActionListener implementation ---------------------------------------
    def actionPerformed(e: ActionEvent) {
      changed
    }

    // DocumentListener implementation -------------------------------------
    def insertUpdate(e: DocumentEvent) {
      changed
    }

    def removeUpdate(e: DocumentEvent) {
      changed
    }

    def changedUpdate(e: DocumentEvent) {
      changed
    }

    // Private methods -----------------------------------------------------

    private def performOperation(operation: Int, jc: JComponent, optionID: String, p: Preferences) {
      operation match {
        case LOAD => loadData(jc, optionID, p)
        case STORE => storeData(jc, optionID, p)
        case ADD_LISTENERS => addListener(jc)
        case _ =>
      }
    }

    private def scan(what: Int, p: Preferences) {
      for (jc <- components) {
        jc.getClientProperty(OPTION_ID) match {
          case x: String => performOperation(what, jc, x, p)
          case xs: Array[String] => xs foreach {performOperation(what, jc, _, p)}
          case _ =>
        }
      }
    }

    private def scan(container: Container, components: ArrayBuffer[JComponent]) {
      container.getComponents foreach {c =>
        if (c.isInstanceOf[JComponent]) {
          val jc = c.asInstanceOf[JComponent]
          val o = jc.getClientProperty(OPTION_ID)
          if (o.isInstanceOf[String] || o.isInstanceOf[Array[String]])
            components += jc
        }

        if (c.isInstanceOf[Container]) {
          scan(c.asInstanceOf[Container], components)
        }
      }
    }

    /**
     * Very smart method which tries to set the values in the components correctly
     */
    private def loadData(jc: JComponent, optionID: String, node: Preferences) {
      jc match {
        case field: JTextField =>
          field.setText(node.get(optionID, getDefaultAsString(optionID)))

        case checkBox: JCheckBox =>
          val df = getDefaultAsBoolean(optionID)
          checkBox.setSelected( node.getBoolean(optionID, df))

        case cb: JComboBox =>
          val value = node.get(optionID, getDefaultAsString(optionID))
          val model = createModel(value)
          cb.setModel(model)
          val item = whichItem(value, model)
          cb.setSelectedItem(item)

        case _ =>
      }
    }

    private def storeData(jc: JComponent, optionID: String, node: Preferences) {
      jc match {
        case field: JTextField =>
          val text = field.getText

          // * test for numbers
          if (isInteger(optionID)) {
            try {
              text.toInt
            } catch {case e: NumberFormatException => return}
          }

          // * watch out, tabSize, spacesPerTab, indentSize and expandTabToSpaces
          // fall back on getGlopalXXX() values and not getDefaultAsXXX value,
          // which is why we must not remove them. Proper solution would be to
          // store formatting preferences to MimeLookup and not use NbPreferences.
          // The problem currently is that MimeLookup based Preferences do not support subnodes.
          if (!optionID.equals(tabSize) &&
              !optionID.equals(spacesPerTab) && !optionID.equals(indentSize) &&
              getDefaultAsString(optionID).equals(text)
          ) {
            node.remove(optionID)
          } else {
            node.put(optionID, text)
          }

        case checkBox: JCheckBox =>
          if (!optionID.equals(expandTabToSpaces) && getDefaultAsBoolean(optionID) == checkBox.isSelected)
            node.remove(optionID)
          else
            node.putBoolean(optionID, checkBox.isSelected)

        case comboBox: JComboBox =>
          val value = comboBox.getSelectedItem.asInstanceOf[ComboItem].value
          if (getDefaultAsString(optionID).equals(value))
            node.remove(optionID)
          else
            node.put(optionID,value)
          
        case _ =>
      }
    }

    private def addListener(jc: JComponent) {
      jc match {
        case field: JTextField =>
          field.addActionListener(this)
          field.getDocument.addDocumentListener(this)

        case checkBox: JCheckBox =>
          checkBox.addActionListener(this)

        case cb: JComboBox =>
          cb.addActionListener(this)
          
        case _ =>
      }
    }

    private def createModel(value: String): ComboBoxModel = {

      //            // is it braces placement?
      //            for (ComboItem comboItem : bracePlacement) {
      //                if ( value.equals( comboItem.value) ) {
      //                    return new DefaultComboBoxModel( bracePlacement );
      //                }
      //            }
      //
      //            // is it braces generation?
      //            for (ComboItem comboItem : bracesGeneration) {
      //                if ( value.equals( comboItem.value) ) {
      //                    return new DefaultComboBoxModel( bracesGeneration );
      //                }
      //            }
      //
      //            // is it wrap
      //            for (ComboItem comboItem : wrap) {
      //                if ( value.equals( comboItem.value) ) {
      //                    return new DefaultComboBoxModel( wrap );
      //                }
      //            }

      null
    }

    private def whichItem(value: String, model: ComboBoxModel): ComboItem = {
      for (i <- 0 until model.getSize) {
        val item = model.getElementAt(i).asInstanceOf[ComboItem]
        if (value.equals(item.value)) {
          return item
        }
      }
      null
    }

    // PreferencesCustomizer implementation --------------------------------

    def getComponent: JComponent = panel

    def getDisplayName: String = panel.getName

    def getId: String = id

    def getHelpCtx: HelpCtx = null

    case class ComboItem(value: String, key: String) {
      val displayName = NbBundle.getMessage(classOf[FmtOptions], key)
      override def toString = displayName
    }
  }

  // PreferencesCustomizer.Factory implementation ------------------------

  class Factory(id: String,
                panelClass: Class[_ <: JPanel],
                previewText:String,
                forcedOptions: Array[String]*
  ) extends PreferencesCustomizer.Factory {
    
    def create(preferences: Preferences): PreferencesCustomizer = {
      try {
        new Customizer(preferences, id, panelClass.newInstance, previewText, forcedOptions: _*)
      } catch {case e: Exception => null}
    }
  } // End of CategorySupport.Factory class

  trait CodeStyleProducer {
    def create(preferences: Preferences): CodeStyle
  }

  class PreviewPreferences extends AbstractPreferences(null, "") {

    private var map = Map[String, AnyRef]()

    protected def putSpi(key: String, value: String) {
      map += key -> value
    }

    protected def getSpi(key: String): String = {
      map.get(key).get.asInstanceOf[String]
    }

    protected def removeSpi(key: String) = {
      map -= key
    }

    @throws(classOf[BackingStoreException])
    protected def removeNodeSpi {
      throw new UnsupportedOperationException("Not supported yet.")
    }

    @throws(classOf[BackingStoreException])
    protected def keysSpi: Array[String] = {
      map.keysIterator.toList.toArray
    }

    @throws(classOf[BackingStoreException])
    protected def childrenNamesSpi: Array[String] = {
      throw new UnsupportedOperationException("Not supported yet.")
    }

    protected def childSpi(name: String): AbstractPreferences = {
      throw new UnsupportedOperationException("Not supported yet.")
    }

    @throws(classOf[BackingStoreException])
    protected def syncSpi {
      throw new UnsupportedOperationException("Not supported yet.")
    }

    @throws(classOf[BackingStoreException])
    protected def flushSpi {
      throw new UnsupportedOperationException("Not supported yet.")
    }
  }

  // read-only, no subnodes
  class ProxyPreferences(delegates: Preferences*) extends AbstractPreferences(null, "") {

    protected def putSpi(key: String, value: String) {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    protected def getSpi(key: String): String = {
      delegates.find{_.get(key, null) != null}.map(_.get(key, null)).getOrElse(null)
    }

    protected def removeSpi(key: String) {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @throws(classOf[BackingStoreException])
    protected def removeNodeSpi {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @throws(classOf[BackingStoreException])
    protected def keysSpi: Array[String] = {
      var keys = Set[String]()
      for (p <- delegates) {
        keys ++= p.keys
      }
      keys.toArray
    }

    @throws(classOf[BackingStoreException])
    protected def childrenNamesSpi: Array[String] = {
      throw new UnsupportedOperationException("Not supported yet.")
    }

    protected def childSpi(name: String): AbstractPreferences = {
      throw new UnsupportedOperationException("Not supported yet.")
    }

    @throws(classOf[BackingStoreException])
    protected def syncSpi {
      throw new UnsupportedOperationException("Not supported yet.")
    }

    @throws(classOf[BackingStoreException])
    protected def flushSpi {
      throw new UnsupportedOperationException("Not supported yet.")
    }
  }

}

class FmtOptions {}
