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

package org.netbeans.modules.scala.debugger.projects;

import java.beans.PropertyChangeEvent;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.swing.SwingUtilities;
import org.netbeans.api.debugger.ActionsManager;


import org.netbeans.api.debugger.DebuggerEngine;

import org.netbeans.api.debugger.DebuggerManager;
import org.netbeans.api.debugger.DebuggerManagerAdapter;

import org.netbeans.api.debugger.jpda.JPDADebugger;

import org.netbeans.api.debugger.jpda.LineBreakpoint;
import org.netbeans.api.project.Project;
import org.netbeans.spi.debugger.ActionsProviderSupport;
import org.netbeans.spi.debugger.jpda.EditorContext;
import org.netbeans.spi.project.ActionProvider;
import org.openide.ErrorManager;
import org.openide.util.RequestProcessor;

import org.openide.windows.TopComponent;


/**
*
* @author   Jan Jancura
*/
public class RunToCursorActionProvider extends ActionsProviderSupport {
    private final static String MIME_TYPE = "text/x-scala";

    private EditorContext       editor;
    private LineBreakpoint      breakpoint;
    
    {
        editor = DebuggerManager.getDebuggerManager ().lookupFirst (null, EditorContext.class);
        
        Listener listener = new Listener ();
        MainProjectManager.getDefault ().addPropertyChangeListener (listener);
        TopComponent.getRegistry ().addPropertyChangeListener (listener);
        DebuggerManager.getDebuggerManager ().addDebuggerListener (
            DebuggerManager.PROP_DEBUGGER_ENGINES,
            listener
        );

        //PATCH 57824: getOpenedPanes () calls from non AWT threads can
        // lead to deadlock.
        SwingUtilities.invokeLater (new Runnable () {
            public void run () {
                setEnabled (
                    ActionsManager.ACTION_RUN_TO_CURSOR + MIME_TYPE,
                    shouldBeEnabled ()
                );
            }
        });
        //PATCH 57824
    }
    
    public Set getActions () {
        return Collections.singleton (ActionsManager.ACTION_RUN_TO_CURSOR + MIME_TYPE);
    }
    
    public void doAction (Object action) {
        
        // 1) set breakpoint
        removeBreakpoint ();
        createBreakpoint (LineBreakpoint.create (
            editor.getCurrentURL (),
            editor.getCurrentLineNumber ()
        ));
        
        // 2) start debugging of project
        invokeAction();
    }
    
    public void postAction(Object action, final Runnable actionPerformedNotifier) {
        final LineBreakpoint newBreakpoint = LineBreakpoint.create (
            editor.getCurrentURL (),
            editor.getCurrentLineNumber ()
        );
        RequestProcessor.getDefault().post(new Runnable() {
            public void run() {
                // 1) set breakpoint
                removeBreakpoint ();
                createBreakpoint (newBreakpoint);
                try {
                    SwingUtilities.invokeAndWait(new Runnable() {
                        public void run() {
                            invokeAction();
                        }
                    });
                } catch (InterruptedException iex) {
                    // Procceed
                } catch (java.lang.reflect.InvocationTargetException itex) {
                    ErrorManager.getDefault().notify(itex);
                } finally {
                    actionPerformedNotifier.run();
                }
            }
        });
    }
    
    private void invokeAction() {
        ((ActionProvider) MainProjectManager.getDefault ().
            getMainProject ().getLookup ().lookup (
                ActionProvider.class
            )).invokeAction (
                ActionProvider.COMMAND_DEBUG, 
                MainProjectManager.getDefault ().getMainProject ().getLookup ()
            );
    }
    
    private boolean shouldBeEnabled () {
        if (editor.getCurrentLineNumber () < 0) return false;
        if (!editor.getCurrentURL ().endsWith (".scala")) return false;
        
        // check if current project supports this action
        Project p = MainProjectManager.getDefault ().getMainProject ();
        if (p == null) return false;
        ActionProvider actionProvider = (ActionProvider) p.getLookup ().
            lookup (ActionProvider.class);
        if (actionProvider == null) return false;
        String[] sa = actionProvider.getSupportedActions ();
        int i, k = sa.length;
        for (i = 0; i < k; i++)
            if (ActionProvider.COMMAND_DEBUG.equals (sa [i]))
                break;
        if (i == k) return false;

        // check if this action should be enabled
        return ((ActionProvider) p.getLookup ().lookup (
                ActionProvider.class
            )).isActionEnabled (
                ActionProvider.COMMAND_DEBUG, 
                MainProjectManager.getDefault ().getMainProject ().getLookup ()
            );
    }
    
    private void createBreakpoint (LineBreakpoint breakpoint) {
        breakpoint.setHidden (true);
        DebuggerManager.getDebuggerManager ().addBreakpoint (breakpoint);
        this.breakpoint = breakpoint;
    }
    
    private void removeBreakpoint () {
        if (breakpoint != null) {
            DebuggerManager.getDebuggerManager ().removeBreakpoint (breakpoint);
            breakpoint = null;
        }
    }
    
    private class Listener extends DebuggerManagerAdapter {
        public void propertyChange (PropertyChangeEvent e) {
            if (e.getPropertyName () == JPDADebugger.PROP_STATE) {
                int state = ((Integer) e.getNewValue ()).intValue ();
                if ( (state == JPDADebugger.STATE_DISCONNECTED) ||
                     (state == JPDADebugger.STATE_STOPPED)
                ) removeBreakpoint ();
                return;
            }
            setEnabled (
                ActionsManager.ACTION_RUN_TO_CURSOR + MIME_TYPE,
                shouldBeEnabled ()
            );
        }
        
        public void engineAdded (DebuggerEngine engine) {
            JPDADebugger debugger = engine.lookupFirst(null, JPDADebugger.class);
            if (debugger == null) return;
            debugger.addPropertyChangeListener (
                JPDADebugger.PROP_STATE,
                this
            );
        }
        
        public void engineRemoved (DebuggerEngine engine) {
            JPDADebugger debugger = engine.lookupFirst(null, JPDADebugger.class);
            if (debugger == null) return;
            debugger.removePropertyChangeListener (
                JPDADebugger.PROP_STATE,
                this
            );
        }
    }
}
