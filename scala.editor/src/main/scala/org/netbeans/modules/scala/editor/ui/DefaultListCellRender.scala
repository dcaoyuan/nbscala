/*
 * Copyright (c) 1998, 2007, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package org.netbeans.modules.scala.editor.ui;

import javax.swing._;
import javax.swing.event._;
import javax.swing.border._;

import java.awt.Component;
import java.awt.Color;
import java.awt.Rectangle;

import java.io.Serializable;
import sun.swing.DefaultLookup;

/**
 * Renders an item in a list.
 * <p>
 * <strong><a name="override">Implementation Note:</a></strong>
 * This class overrides
 * <code>invalidate</code>,
 * <code>validate</code>,
 * <code>revalidate</code>,
 * <code>repaint</code>,
 * <code>isOpaque</code>,
 * and
 * <code>firePropertyChange</code>
 * solely to improve performance.
 * If not overridden, these frequently called methods would execute code paths
 * that are unnecessary for the default list cell renderer.
 * If you write your own renderer,
 * take care to weigh the benefits and
 * drawbacks of overriding these methods.
 *
 * <p>
 *
 * <strong>Warning:</strong>
 * Serialized objects of this class will not be compatible with
 * future Swing releases. The current serialization support is
 * appropriate for short term storage or RMI between applications running
 * the same version of Swing.  As of 1.4, support for long term storage
 * of all JavaBeans<sup><font size="-2">TM</font></sup>
 * has been added to the <code>java.beans</code> package.
 * Please see {@link java.beans.XMLEncoder}.
 *
 * @author Philip Milne
 * @author Hans Muller
 */
class DefaultListCellRenderer[T <: AnyRef] extends JLabel
    with ListCellRenderer[T] with Serializable {
  import DefaultListCellRenderer._

  setOpaque(true);
  setBorder(getNoFocusBorder());
  setName("List.cellRenderer");

  private def getNoFocusBorder(): Border = {
    val border = DefaultLookup.getBorder(this, ui, "List.cellNoFocusBorder");
    if (System.getSecurityManager() ne null) {
      if (border ne null) return border;
      return SAFE_NO_FOCUS_BORDER;
    } else {
      if ((border ne null) && ((noFocusBorder eq null) || noFocusBorder == DEFAULT_NO_FOCUS_BORDER)) {
        return border;
      }
      return noFocusBorder;
    }
  }

  def getListCellRendererComponent(list: JList[_ <: T],
                                   value: T,
                                   index: Int,
                                   _isSelected: Boolean,
                                   cellHasFocus: Boolean): Component = {

    setComponentOrientation(list.getComponentOrientation)

    var isSelected = _isSelected
    var bg: Color = null;
    var fg: Color = null;

    val dropLocation = list.getDropLocation
    if ((dropLocation ne null) && !dropLocation.isInsert && dropLocation.getIndex == index) {

      bg = DefaultLookup.getColor(this, ui, "List.dropCellBackground");
      fg = DefaultLookup.getColor(this, ui, "List.dropCellForeground");

      isSelected = true;
    }

    if (isSelected) {
      setBackground(if (bg eq null) list.getSelectionBackground() else bg);
      setForeground(if (fg eq null) list.getSelectionForeground() else fg);
    } else {
      setBackground(list.getBackground());
      setForeground(list.getForeground());
    }

    if (value.isInstanceOf[Icon]) {
      setIcon(value.asInstanceOf[Icon]);
      setText("");
    } else {
      setIcon(null);
      setText(if (value eq null) "" else value.toString());
    }

    setEnabled(list.isEnabled());
    setFont(list.getFont());

    var border: Border = null;
    if (cellHasFocus) {
      if (isSelected) {
        border = DefaultLookup.getBorder(this, ui, "List.focusSelectedCellHighlightBorder");
      }
      if (border eq null) {
        border = DefaultLookup.getBorder(this, ui, "List.focusCellHighlightBorder");
      }
    } else {
      border = getNoFocusBorder();
    }
    setBorder(border);

    return this;
  }

  /**
   * Overridden for performance reasons.
   * See the <a href="#override">Implementation Note</a>
   * for more information.
   *
   * @since 1.5
   * @return <code>true</code> if the background is completely opaque
   *         and differs from the JList's background;
   *         <code>false</code> otherwise
   */
  override def isOpaque(): Boolean = {
    val back = getBackground();
    var p = getParent();
    if (p ne null) {
      p = p.getParent();
    }
    // p should now be the JList.
    val colorMatch = (back ne null) && (p ne null) &&
      back.equals(p.getBackground()) &&
      p.isOpaque();
    return !colorMatch && super.isOpaque();
  }

  /**
   * Overridden for performance reasons.
   * See the <a href="#override">Implementation Note</a>
   * for more information.
   */
  override def validate() {}

  /**
   * Overridden for performance reasons.
   * See the <a href="#override">Implementation Note</a>
   * for more information.
   *
   * @since 1.5
   */
  override def invalidate() {}

  /**
   * Overridden for performance reasons.
   * See the <a href="#override">Implementation Note</a>
   * for more information.
   *
   * @since 1.5
   */
  override def repaint() {}

  /**
   * Overridden for performance reasons.
   * See the <a href="#override">Implementation Note</a>
   * for more information.
   */
  override def revalidate() {}

  /**
   * Overridden for performance reasons.
   * See the <a href="#override">Implementation Note</a>
   * for more information.
   */
  override def repaint(tm: Long, x: Int, y: Int, width: Int, height: Int) {}

  /**
   * Overridden for performance reasons.
   * See the <a href="#override">Implementation Note</a>
   * for more information.
   */
  override def repaint(r: Rectangle) {}

  /**
   * Overridden for performance reasons.
   * See the <a href="#override">Implementation Note</a>
   * for more information.
   */
  override protected def firePropertyChange(propertyName: String, oldValue: Object, newValue: Object) {
    // Strings get interned...
    if (propertyName == "text" ||
      ((propertyName == "font" || propertyName == "foreground") &&
        (oldValue != newValue) &&
        (getClientProperty(javax.swing.plaf.basic.BasicHTML.propertyKey) ne null))) {

      super.firePropertyChange(propertyName, oldValue, newValue);
    }
  }

  /**
   * Overridden for performance reasons.
   * See the <a href="#override">Implementation Note</a>
   * for more information.
   */
  override def firePropertyChange(propertyName: String, oldValue: Byte, newValue: Byte) {}

  /**
   * Overridden for performance reasons.
   * See the <a href="#override">Implementation Note</a>
   * for more information.
   */
  override def firePropertyChange(propertyName: String, oldValue: Char, newValue: Char) {}

  /**
   * Overridden for performance reasons.
   * See the <a href="#override">Implementation Note</a>
   * for more information.
   */
  override def firePropertyChange(propertyName: String, oldValue: Short, newValue: Short) {}

  /**
   * Overridden for performance reasons.
   * See the <a href="#override">Implementation Note</a>
   * for more information.
   */
  override def firePropertyChange(propertyName: String, oldValue: Int, newValue: Int) {}

  /**
   * Overridden for performance reasons.
   * See the <a href="#override">Implementation Note</a>
   * for more information.
   */
  override def firePropertyChange(propertyName: String, oldValue: Long, newValue: Long) {}

  /**
   * Overridden for performance reasons.
   * See the <a href="#override">Implementation Note</a>
   * for more information.
   */
  override def firePropertyChange(propertyName: String, oldValue: Float, newValue: Float) {}

  /**
   * Overridden for performance reasons.
   * See the <a href="#override">Implementation Note</a>
   * for more information.
   */
  override def firePropertyChange(propertyName: String, oldValue: Double, newValue: Double) {}

  /**
   * Overridden for performance reasons.
   * See the <a href="#override">Implementation Note</a>
   * for more information.
   */
  override def firePropertyChange(propertyName: String, oldValue: Boolean, newValue: Boolean) {}

}

object DefaultListCellRenderer {
  /**
   * An empty <code>Border</code>. This field might not be used. To change the
   * <code>Border</code> used by this renderer override the
   * <code>getListCellRendererComponent</code> method and set the border
   * of the returned component directly.
   */
  private val SAFE_NO_FOCUS_BORDER: Border = new EmptyBorder(1, 1, 1, 1);
  private val DEFAULT_NO_FOCUS_BORDER: Border = new EmptyBorder(1, 1, 1, 1);
  protected val noFocusBorder: Border = DEFAULT_NO_FOCUS_BORDER;

  /**
   * A subclass of DefaultListCellRenderer that implements UIResource.
   * DefaultListCellRenderer doesn't implement UIResource
   * directly so that applications can safely override the
   * cellRenderer property with DefaultListCellRenderer subclasses.
   * <p>
   * <strong>Warning:</strong>
   * Serialized objects of this class will not be compatible with
   * future Swing releases. The current serialization support is
   * appropriate for short term storage or RMI between applications running
   * the same version of Swing.  As of 1.4, support for long term storage
   * of all JavaBeans<sup><font size="-2">TM</font></sup>
   * has been added to the <code>java.beans</code> package.
   * Please see {@link java.beans.XMLEncoder}.
   */
  class UIResource extends DefaultListCellRenderer
      with javax.swing.plaf.UIResource {
  }

}