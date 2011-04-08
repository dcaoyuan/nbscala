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
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
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
package org.netbeans.modules.scala.debugger.actions;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Set;
import org.netbeans.api.debugger.ActionsManager;


import org.netbeans.api.debugger.Breakpoint;
import org.netbeans.api.debugger.DebuggerManager;
import org.netbeans.spi.debugger.ContextProvider;
import org.netbeans.api.debugger.jpda.JPDADebugger;
import org.netbeans.api.debugger.jpda.LineBreakpoint;
import org.netbeans.modules.scala.debugger.EditorContextBridge;
import org.netbeans.spi.debugger.ActionsProviderSupport;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.URLMapper;

import org.openide.util.NbBundle;

/** 
 *
 * @author   Jan Jancura
 */
public class ToggleBreakpointActionProvider extends ActionsProviderSupport
        implements PropertyChangeListener {

    private final static String MIME_TYPE = "text/x-scala";

    private JPDADebugger debugger;

    public ToggleBreakpointActionProvider() {
        EditorContextBridge.getContext().addPropertyChangeListener(this);
    }

    public ToggleBreakpointActionProvider(ContextProvider lookupProvider) {
        debugger = lookupProvider.lookupFirst(null, JPDADebugger.class);
        debugger.addPropertyChangeListener(JPDADebugger.PROP_STATE, this);
        EditorContextBridge.getContext().addPropertyChangeListener(this);
    }

    private void destroy() {
        debugger.removePropertyChangeListener(JPDADebugger.PROP_STATE, this);
        EditorContextBridge.getContext().removePropertyChangeListener(this);
    }

    public void propertyChange(PropertyChangeEvent evt) {
        String url = EditorContextBridge.getContext().getCurrentURL();
        FileObject fo;
        try {
            fo = URLMapper.findFileObject(new URL(url));
        } catch (MalformedURLException muex) {
            fo = null;
        }
        setEnabled(
                ActionsManager.ACTION_TOGGLE_BREAKPOINT + MIME_TYPE,
                (EditorContextBridge.getContext().getCurrentLineNumber() >= 0) &&
                // "text/x-scala" MIMEType will be resolved by scala.editing module, thus this module should run-dependency on scala.editing
                (fo != null && MIME_TYPE.equals(fo.getMIMEType())) // NOI18N
                //(fo != null && (url.endsWith (".scala")))  // NOI18N
                );
        if (debugger != null &&
                debugger.getState() == JPDADebugger.STATE_DISCONNECTED) {
            destroy();
        }
    }

    public Set getActions() {
        return Collections.singleton(ActionsManager.ACTION_TOGGLE_BREAKPOINT + MIME_TYPE);
    }

    public void doAction(Object action) {
        DebuggerManager d = DebuggerManager.getDebuggerManager();

        // 1) get source name & line number
        int ln = EditorContextBridge.getContext().getCurrentLineNumber();
        String url = EditorContextBridge.getContext().getCurrentURL();
        if ("".equals(url.trim())) {
            return;
        }

        // 2) find and remove existing line breakpoint
        LineBreakpoint lb = findBreakpoint(url, ln);
        if (lb != null) {
            d.removeBreakpoint(lb);
            return;
        }
//        Breakpoint[] bs = d.getBreakpoints ();
//        int i, k = bs.length;
//        for (i = 0; i < k; i++) {
//            if (!(bs [i] instanceof LineBreakpoint)) continue;
//            LineBreakpoint lb = (LineBreakpoint) bs [i];
//            if (ln != lb.getLineNumber ()) continue;
//            if (!url.equals (lb.getURL ())) continue;
//            d.removeBreakpoint (lb);
//            return;
//        }

        // 3) create a new line breakpoint
        lb = LineBreakpoint.create(url, ln);
        lb.setPrintText(NbBundle.getBundle(ToggleBreakpointActionProvider.class).getString("CTL_Line_Breakpoint_Print_Text"));
        d.addBreakpoint(lb);
    }

    static LineBreakpoint findBreakpoint(String url, int lineNumber) {
        Breakpoint[] breakpoints = DebuggerManager.getDebuggerManager().getBreakpoints();
        for (int i = 0; i < breakpoints.length; i++) {
            if (!(breakpoints[i] instanceof LineBreakpoint)) {
                continue;
            }
            LineBreakpoint lb = (LineBreakpoint) breakpoints[i];
            if (!lb.getURL().equals(url)) {
                continue;
            }
            if (lb.getLineNumber() == lineNumber) {
                return lb;
            }
        }
        return null;
    }
}
