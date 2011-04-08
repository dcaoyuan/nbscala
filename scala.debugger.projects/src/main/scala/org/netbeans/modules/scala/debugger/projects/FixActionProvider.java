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
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import javax.swing.SwingUtilities;
import org.netbeans.api.debugger.ActionsManager;

import org.netbeans.api.debugger.Breakpoint;
import org.netbeans.api.debugger.DebuggerEngine;
import org.netbeans.api.debugger.DebuggerManagerListener;
import org.netbeans.spi.debugger.ContextProvider;
import org.netbeans.api.debugger.Session;
import org.netbeans.api.debugger.Watch;
import org.netbeans.api.debugger.jpda.JPDADebugger;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.spi.debugger.ActionsProviderSupport;
import org.netbeans.spi.project.ActionProvider;
import org.netbeans.api.java.project.JavaProjectConstants;
import org.netbeans.spi.debugger.ui.EditorContextDispatcher;
import org.openide.ErrorManager;
import org.openide.loaders.DataObject;
import org.openide.nodes.Node;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;
import org.openide.windows.TopComponent;

/**
*
* @author   Jan Jancura
*/
public class FixActionProvider extends ActionsProviderSupport {

    private JPDADebugger debugger;
    private Listener listener;
    
    
    public FixActionProvider (ContextProvider lookupProvider) {
        debugger = lookupProvider.lookupFirst(null, JPDADebugger.class);
        
        listener = new Listener ();
        MainProjectManager.getDefault ().addPropertyChangeListener (listener);
        debugger.addPropertyChangeListener (JPDADebugger.PROP_STATE, listener);
        EditorContextDispatcher.getDefault().addPropertyChangeListener("text/x-scala", listener);
        
        setEnabled (
            ActionsManager.ACTION_FIX,
            shouldBeEnabled ()
        );
    }
    
    private void destroy () {
        debugger.removePropertyChangeListener (JPDADebugger.PROP_STATE, listener);
        MainProjectManager.getDefault ().removePropertyChangeListener (listener);
        EditorContextDispatcher.getDefault().removePropertyChangeListener (listener);
    }
    
    public Set getActions () {
        return Collections.singleton (ActionsManager.ACTION_FIX);
    }
    
    public void doAction (Object action) {
        if (!SwingUtilities.isEventDispatchThread()) {
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
            }
        } else {
            invokeAction();
        }
    }
    
    private void invokeAction() {
        ((ActionProvider) getCurrentProject().getLookup ().lookup (
                ActionProvider.class
            )).invokeAction (
                JavaProjectConstants.COMMAND_DEBUG_FIX, 
                getLookup ()
            );
    }

    /**
     * Returns the project that the active node's fileobject belongs to. 
     * If this cannot be determined for some reason, returns the main project.
     *  
     * @return the project that the active node's fileobject belongs to
     */ 
    private Project getCurrentProject() {
        Node[] nodes = TopComponent.getRegistry ().getActivatedNodes ();
        if (nodes == null || nodes.length == 0) return MainProjectManager.getDefault().getMainProject();
        DataObject dao = (DataObject) nodes[0].getCookie(DataObject.class);
        if (dao == null) return MainProjectManager.getDefault().getMainProject();
        return FileOwnerQuery.getOwner(dao.getPrimaryFile());        
    }
    
    private boolean shouldBeEnabled () {
        // check if current debugger supports this action
        if (!debugger.canFixClasses()) return false;
        // check if current project supports this action
        Project p = getCurrentProject();
        if (p == null) return false;
        ActionProvider actionProvider = (ActionProvider) p.getLookup ().
            lookup (ActionProvider.class);
        if (actionProvider == null) return false;
        String[] sa = actionProvider.getSupportedActions ();
        int i, k = sa.length;
        for (i = 0; i < k; i++)
            if (JavaProjectConstants.COMMAND_DEBUG_FIX.equals (sa [i]))
                break;
        if (i == k) return false;

        // check if this action should be enabled
        return ((ActionProvider) p.getLookup ().lookup (
                ActionProvider.class
            )).isActionEnabled (
                JavaProjectConstants.COMMAND_DEBUG_FIX, 
                getLookup ()
            );
    }
    
    private Lookup getLookup () {
        Node[] nodes = TopComponent.getRegistry ().getActivatedNodes ();
        int i, k = nodes.length;
        ArrayList l = new ArrayList ();
        for (i = 0; i < k; i++) {
            Object o = nodes [i].getCookie (DataObject.class);
            if (o != null)
                l.add (o);
        }
        return Lookups.fixed (l.toArray (new DataObject [l.size ()]));
    }
    
    private class Listener implements PropertyChangeListener, 
    DebuggerManagerListener {
        public Listener () {}
        
        public void propertyChange (PropertyChangeEvent e) {
            boolean en = shouldBeEnabled ();
            setEnabled (
                ActionsManager.ACTION_FIX,
                en
            );
            if (debugger.getState () == JPDADebugger.STATE_DISCONNECTED) 
                destroy ();
        }
        public void sessionRemoved (Session session) {}
        public void breakpointAdded (Breakpoint breakpoint) {}
        public void breakpointRemoved (Breakpoint breakpoint) {}
        public Breakpoint[] initBreakpoints () {return new Breakpoint [0];}
        public void initWatches () {}
        public void sessionAdded (Session session) {}
        public void watchAdded (Watch watch) {}
        public void watchRemoved (Watch watch) {}
        public void engineAdded (DebuggerEngine engine) {}
        public void engineRemoved (DebuggerEngine engine) {}
    }
}
