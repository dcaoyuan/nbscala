/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2010 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
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

package org.netbeans.modules.scala.maven;

import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import org.openide.ErrorManager;
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.text.Line;
import org.openide.windows.OutputEvent;
import org.openide.windows.OutputListener;


/**
 * compile error editor annotation
 * @author  Milos Kleint 
 */
public final class CompileAnnotation /*extends Annotation */implements /*PropertyChangeListener,*/ OutputListener {
    
    File clazzfile; //for tests..
    private int lineNum;
    private String text;
    
    public CompileAnnotation(File clazz, String line, String textAnn) {
        clazzfile = clazz;
        text = textAnn;
        try {
            lineNum = Integer.parseInt(line);
        } catch (NumberFormatException exc) {
            lineNum = -1;
        }
    }
    
    
    
    @Override
    public void outputLineSelected(OutputEvent ev) {
    }
    
    /** Called when some sort of action is performed on a line.
     * @param ev the event describing the line
     */
    @Override
    public void outputLineAction(OutputEvent ev) {
        FileUtil.refreshFor(clazzfile);
        FileObject file = FileUtil.toFileObject(clazzfile);
        if (file == null) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }
        try {
            DataObject dob = DataObject.find(file);
            EditorCookie ed = dob.getLookup().lookup(EditorCookie.class);
            if (ed != null && file == dob.getPrimaryFile()) {
                if (lineNum == -1) {
                    ed.open();
                } else {
                    ed.openDocument();
                    try {
                        Line l = ed.getLineSet().getOriginal(lineNum - 1);
                        if (! l.isDeleted()) {
                            l.show(Line.ShowOpenType.REUSE, Line.ShowVisibilityType.FOCUS);
                        }
                    } catch (IndexOutOfBoundsException ioobe) {
                        // Probably harmless. Bogus line number.
                        ed.open();
                    }
                }
//                attachAllInFile(ed, this);
            } else {
                Toolkit.getDefaultToolkit().beep();
            }
        } catch (DataObjectNotFoundException donfe) {
            ErrorManager.getDefault().notify(donfe);
        } catch (IOException ioe) {
            ErrorManager.getDefault().notify(ioe);
        }
    }
    
    /** Called when a line is cleared from the buffer of known lines.
     * @param ev the event describing the line
     */
    @Override
    public void outputLineCleared(OutputEvent ev) {
    }
    
    @Override
    public String toString() {
        return "error[" + clazzfile + ":" + lineNum + ":" + text + "]"; // NOI18N
    }
    
}
