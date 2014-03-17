package org.netbeans.modules.scala.sbt.options

import java.beans.PropertyChangeListener
import java.beans.PropertyChangeSupport
import org.netbeans.spi.options.OptionsPanelController
import org.openide.util.Lookup

final class SbtOptionsPanelController extends OptionsPanelController {
  private lazy val panel = new SbtPanel(this)
  private val pcs = new PropertyChangeSupport(this)
  private var _changed = false

  def update {
    panel.load()
    _changed = false
  }

  def applyChanges {
    panel.store()
    _changed = false
  }

  def cancel() {
    // need not do anything special, if no changes have been persisted yet
  }

  def isValid: Boolean = panel.valid

  def isChanged = _changed

  def getHelpCtx = null // new HelpCtx("...ID") if you have a help set

  def getComponent(masterLookup: Lookup) = panel

  def addPropertyChangeListener(l: PropertyChangeListener) =
    pcs.addPropertyChangeListener(l)

  def removePropertyChangeListener(l: PropertyChangeListener) =
    pcs.removePropertyChangeListener(l)

  def changed = {
    val (evt, t1, t2) = if (!_changed) {
      _changed = true
      (OptionsPanelController.PROP_CHANGED, false, true)
    } else (OptionsPanelController.PROP_VALID, null, null)
    pcs.firePropertyChange(evt, t1, t2)
  }
}
