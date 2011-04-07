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
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2007 Sun
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

package org.netbeans.modules.languages.execution.console;

import java.awt.Color;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.JTextComponent;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

public class TextAreaReadline extends OutputStream implements KeyListener {
    
    JTextComponent area;
    private int startPos;
    private String currentLine;
    
    private Object inEditing = new Object();
    
    public MutableAttributeSet promptStyle;
    public MutableAttributeSet inputStyle;
    public MutableAttributeSet outputStyle;
    public MutableAttributeSet resultStyle;
    
    private JComboBox completeCombo;
    private BasicComboPopup completePopup;
    private int start;
    private int end;
    
    private PrintStream pipedPrintOut;
    
    public TextAreaReadline(JTextComponent area) {
        this(area, null, null);
    }
    
    public TextAreaReadline(JTextComponent area, final String message, PipedInputStream pipedIn) {
        this.area = area;
        
        try {
            this.pipedPrintOut = new PrintStream(new PipedOutputStream(pipedIn));
            ConsoleLineReader.createConsoleLineReader();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        
        area.addKeyListener(this);
        
        // No editing before startPos
        if (area.getDocument() instanceof AbstractDocument) {
            ((AbstractDocument)area.getDocument()).setDocumentFilter(new DocumentFilter() {
                public void insertString(DocumentFilter.FilterBypass fb, int offset, String str, AttributeSet attr) throws BadLocationException {
                    if (offset >= startPos) super.insertString(fb, offset, str, attr);
                }
                
                public void remove(DocumentFilter.FilterBypass fb, int offset, int length) throws BadLocationException {
                    if (offset >= startPos) super.remove(fb, offset, length);
                }
                
                public void replace(DocumentFilter.FilterBypass fb, int offset, int length, String str, AttributeSet attrs) throws BadLocationException {
                    if (offset >= startPos) super.replace(fb, offset, length, str, attrs);
                }
            });
        }
        
        promptStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(promptStyle, new Color(0xa4, 0x00, 0x00));
        
        inputStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(inputStyle, new Color(0x20, 0x4a, 0x87));
        
        outputStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(outputStyle, Color.darkGray);
        
        resultStyle = new SimpleAttributeSet();
        StyleConstants.setItalic(resultStyle, true);
        StyleConstants.setForeground(resultStyle, new Color(0x20, 0x4a, 0x87));
        
        completeCombo = new JComboBox();
        completeCombo.setRenderer(new DefaultListCellRenderer()); // no silly ticks!
        completePopup = new BasicComboPopup(completeCombo);
        
        if (message != null) {
            final MutableAttributeSet messageStyle = new SimpleAttributeSet();
            StyleConstants.setBackground(messageStyle, area.getForeground());
            StyleConstants.setForeground(messageStyle, area.getBackground());
            append(message, messageStyle);
        }
    }
    
    protected void completeAction(KeyEvent event) {
        if (ConsoleLineReader.getCompletor() == null) {
            return;
        }
        
        event.consume();
        
        if (completePopup.isVisible()) return;
        
        List candidates = new LinkedList();
        String bufstr = null;
        try {
            bufstr = area.getText(startPos, area.getCaretPosition() - startPos);
        } catch (BadLocationException e) {
            return;
        }
        
        int cursor = area.getCaretPosition() - startPos;
        
        int position = ConsoleLineReader.getCompletor().complete(bufstr, cursor, candidates);
        
        // no candidates? Fail.
        if (candidates.isEmpty()) {
            return;
        }
        
        if (candidates.size() == 1) {
            replaceText(startPos + position, area.getCaretPosition(), (String) candidates.get(0));
            return;
        }
        
        start = startPos + position;
        end = area.getCaretPosition();
        
        Point pos = area.getCaret().getMagicCaretPosition();
        
        // bit risky if someone changes completor, but useful for method calls
        int cutoff = bufstr.substring(position).lastIndexOf('.') + 1;
        start += cutoff;
        
        if (candidates.size() < 10) {
            completePopup.getList().setVisibleRowCount(candidates.size());
        } else {
            completePopup.getList().setVisibleRowCount(10);
        }
        
        completeCombo.removeAllItems();
        for (Iterator i = candidates.iterator(); i.hasNext();) {
            String item = (String) i.next();
            if (cutoff != 0) item = item.substring(cutoff);
            completeCombo.addItem(item);
        }
        
        completePopup.show(area, pos.x, pos.y + area.getFontMetrics(area.getFont()).getHeight());
    }
    
    protected void backAction(KeyEvent event) {
        if (area.getCaretPosition() <= startPos) {
            event.consume();
        }
    }
    
    protected void upAction(KeyEvent event) {
        event.consume();
        
        if (completePopup.isVisible()) {
            int selected = completeCombo.getSelectedIndex() - 1;
            if (selected < 0) return;
            completeCombo.setSelectedIndex(selected);
            return;
        }
        
        if (! ConsoleLineReader.getHistory().next()) // at end
            currentLine = getLine();
        else
            ConsoleLineReader.getHistory().previous(); // undo check
        
        if (! ConsoleLineReader.getHistory().previous()) {
            return;
        }
        
        String oldLine = ConsoleLineReader.getHistory().current().trim();
        replaceText(startPos, area.getDocument().getLength(), oldLine);
    }
    
    protected void downAction(KeyEvent event) {
        event.consume();
        
        if (completePopup.isVisible()) {
            int selected = completeCombo.getSelectedIndex() + 1;
            if (selected == completeCombo.getItemCount()) return;
            completeCombo.setSelectedIndex(selected);
            return;
        }
        
        if (! ConsoleLineReader.getHistory().next()) {
            return;
        }
        
        String oldLine;
        if (! ConsoleLineReader.getHistory().next()) // at end
            oldLine = currentLine;
        else {
            ConsoleLineReader.getHistory().previous(); // undo check
            oldLine = ConsoleLineReader.getHistory().current().trim();
        }
        
        replaceText(startPos, area.getDocument().getLength(), oldLine);
    }
    
    protected void replaceText(int start, int end, String replacement) {
        try {
            area.getDocument().remove(start, end - start);
            area.getDocument().insertString(start, replacement, inputStyle);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }
    
    protected String getLine() {
        try {
            return area.getText(startPos, area.getDocument().getLength() - startPos);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    protected void enterAction(KeyEvent event) {
        event.consume();
        
        if (completePopup.isVisible()) {
            if (completeCombo.getSelectedItem() != null)
                replaceText(start, end, (String) completeCombo.getSelectedItem());
            completePopup.setVisible(false);
            return;
        }
        
        append("\n", null);
        
        String inputStr = getLine().trim();
        pipedPrintOut.println(inputStr);
        
        ConsoleLineReader.getHistory().addToHistory(inputStr);
        ConsoleLineReader.getHistory().moveToEnd();
        
        area.setCaretPosition(area.getDocument().getLength());
        startPos = area.getDocument().getLength();
        
        synchronized (inEditing) {
            inEditing.notify();
        }
    }
    
    public void keyPressed(KeyEvent event) {
        int code = event.getKeyCode();
        switch (code) {
        case KeyEvent.VK_TAB: completeAction(event); break;
        case KeyEvent.VK_LEFT:
        case KeyEvent.VK_BACK_SPACE: backAction(event); break;
        case KeyEvent.VK_UP: upAction(event); break;
        case KeyEvent.VK_DOWN: downAction(event); break;
        case KeyEvent.VK_ENTER: enterAction(event); break;
        case KeyEvent.VK_HOME: event.consume(); area.setCaretPosition(startPos); break;
        }
        
        if (completePopup.isVisible() &&
                code != KeyEvent.VK_TAB &&
                code != KeyEvent.VK_UP &&
                code != KeyEvent.VK_DOWN ) {
            completePopup.setVisible(false);
        }
    }
    
    public void keyReleased(KeyEvent arg0) { }
    
    public void keyTyped(KeyEvent arg0) { }
    
    /** Output methods **/
    
    protected void append(String str, AttributeSet style) {
        try {
            area.getDocument().insertString(area.getDocument().getLength(), str, style);
        } catch (BadLocationException e) { }
    }
    
    public void write(int b) throws IOException {
        writeString("" + b);
    }
    
    public void write(byte[] b, int off, int len) {
        writeString(new String(b, off, len));
    }
    
    public void write(byte[] b) {
        writeString(new String(b));
    }
    
    private void writeString(final String str) {
        if (str.startsWith("=>")) {
            append(str, resultStyle);
        } else {
            append(str, outputStyle);
        }
        area.setCaretPosition(area.getDocument().getLength());
        startPos = area.getDocument().getLength();
    }
}
