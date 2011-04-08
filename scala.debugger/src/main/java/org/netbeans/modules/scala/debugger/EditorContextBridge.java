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
 * Software is Sun Micro//S ystems, Inc. Portions Copyright 1997-2007 Sun
 * Micro//S ystems, Inc. All Rights Reserved.
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
package org.netbeans.modules.scala.debugger;

import com.sun.jdi.AbsentInformationException;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.netbeans.api.debugger.Breakpoint.VALIDITY;
import org.netbeans.api.debugger.DebuggerManager;
import org.netbeans.api.debugger.jpda.FieldBreakpoint;
import org.netbeans.api.debugger.jpda.JPDABreakpoint;
import org.netbeans.api.debugger.jpda.LineBreakpoint;
import org.netbeans.api.debugger.jpda.MethodBreakpoint;
import org.netbeans.api.debugger.jpda.CallStackFrame;
import org.netbeans.api.debugger.jpda.JPDAThread;
import org.netbeans.spi.debugger.jpda.EditorContext;
import org.netbeans.spi.debugger.jpda.SourcePathProvider;

import org.openide.ErrorManager;


/**
 *
 * @author Jan Jancura
 */
public class EditorContextBridge {

    public static final String FIELD = "field";
    public static final String METHOD = "method";
    public static final String CLASS = "class";
    public static final String LINE = "line";

    private static EditorContext context;
    
    public static EditorContext getContext () {
        if (context == null) {
            List l = DebuggerManager.getDebuggerManager ().lookup 
                (null, EditorContext.class);
            context = (EditorContext) l.get (0);
            int i, k = l.size ();
            for (i = 1; i < k; i++)
                context = new CompoundContextProvider (
                    (EditorContext) l.get (i),
                    context
                );
        }
        return context;
    }

    
    // ContextProvider methods .................................................
    
    /**
     * Returns signature of method currently selected in editor or <code>null</code>.
     *
     * @return signature of method currently selected in editor or <code>null</code>
     */
    public static String getCurrentMethodSignature () {
        // TODO: return getContext ().getCurrentMethodSignature ();
        try {
        return (String) getContext ().getClass().getMethod("getCurrentMethodSignature", new Class[] {}).
                invoke(getContext(), new Object[] {});
        } catch (java.lang.reflect.InvocationTargetException itex) {
            Throwable tex = itex.getTargetException();
            if (tex instanceof RuntimeException) {
                throw (RuntimeException) tex;
            } else {
                ErrorManager.getDefault().notify(tex);
                return "";
            }
        } catch (Exception ex) {
            ErrorManager.getDefault().notify(ex);
            return "";
        }
    }

    
    
    // utility methods .........................................................

    public static String getFileName (LineBreakpoint b) { 
        try {
            return new File (new URL (b.getURL ()).getFile ()).getName ();
        } catch (MalformedURLException e) {
            return null;
        }
    }

    public static boolean showSource (LineBreakpoint b, Object timeStamp) {
        if (b.getLineNumber () < 1)
            return EditorContextBridge.getContext().showSource (
                b.getURL (),
                1,
                timeStamp
            );
        return EditorContextBridge.getContext().showSource (
            b.getURL (),
            b.getLineNumber (),
            timeStamp
        );
    }

    public static String getDefaultType () {
        String id = getContext().getSelectedIdentifier ();
        try {
            if (id != null) {
                if (id.equals(getContext().getCurrentMethodName())) return METHOD;
                String s = getContext().getCurrentClassName();
                int i = s.lastIndexOf ('.');
                if (i >= 0)
                    s = s.substring (i + 1);
                if (id.equals (s))
                    return CLASS;
                return FIELD;
            } else {
                String s = getContext().getCurrentFieldName ();
                if (s != null && s.length () > 0)
                    return FIELD;
                s = getContext().getCurrentMethodName();
                if (s != null && s.length () > 0)
                    return METHOD;
                if (s != null && s.length () < 1) {
                    s = getContext().getCurrentClassName ();
                    if (s.length () > 0)
                        return CLASS;
                }
            }
        } catch (java.awt.IllegalComponentStateException icsex) {}
        return null;
    }

    public static Object annotate (
        LineBreakpoint b
    ) {
        String url = b.getURL ();
        int lineNumber = b.getLineNumber ();
        if (lineNumber < 1) return null;
        String condition = b.getCondition ();
        boolean isConditional = (condition != null) &&
            !"".equals (condition.trim ()); // NOI18N
        boolean isInvalid = b.getValidity() == VALIDITY.INVALID;
        String annotationType = b.isEnabled () ?
            (isConditional ? EditorContext.CONDITIONAL_BREAKPOINT_ANNOTATION_TYPE :
                             EditorContext.BREAKPOINT_ANNOTATION_TYPE) :
            (isConditional ? EditorContext.DISABLED_CONDITIONAL_BREAKPOINT_ANNOTATION_TYPE :
                             EditorContext.DISABLED_BREAKPOINT_ANNOTATION_TYPE);
        if (isInvalid && b.isEnabled ()) annotationType += "_broken";

        return getContext().annotate (
            url,
            lineNumber,
            annotationType,
            null
        );
    }

    public static Object[] annotate (
        JPDABreakpoint b
    ) {
        String[] URLs;
        int[] lineNumbers;
        String condition;
        if (b instanceof LineBreakpoint) {
            URLs = new String[] { ((LineBreakpoint) b).getURL () };
            lineNumbers = new int[] { ((LineBreakpoint) b).getLineNumber() };
            condition = ((LineBreakpoint) b).getCondition();
        } else if (b instanceof FieldBreakpoint) {
            String className = ((FieldBreakpoint) b).getClassName();
            URLs = getClassURLs(SourcePath.convertClassNameToRelativePath(className));
            lineNumbers = new int[URLs.length];
            for (int i = 0; i < URLs.length; i++) {
                lineNumbers[i] = getContext().getFieldLineNumber(URLs[i], className, ((FieldBreakpoint) b).getFieldName());
            }
            condition = ((FieldBreakpoint) b).getCondition();
        } else if (b instanceof MethodBreakpoint) {
            String[] filters = ((MethodBreakpoint) b).getClassFilters();
            String[] urls = new String[] {};
            int[] lns = new int[] {};
            for (int i = 0; i < filters.length; i++) {
                // TODO: annotate also other matched classes
                if (!filters[i].startsWith("*") && !filters[i].endsWith("*")) {
                    String[] newUrls = getClassURLs(SourcePath.convertClassNameToRelativePath(filters[i]));
                    int[] newlns = new int[newUrls.length];
                    for (int j = 0; j < newUrls.length; j++) {
                       newlns[j] = getContext().getMethodLineNumber(newUrls[j], filters[i],
                                                       ((MethodBreakpoint) b).getMethodName(),
                                                       ((MethodBreakpoint) b).getMethodSignature());
                    }
                    if (urls.length == 0) {
                        urls = newUrls;
                        lns = newlns;
                    } else {
                        String[] l = new String[urls.length + newUrls.length];
                        System.arraycopy(urls, 0, l, 0, urls.length);
                        System.arraycopy(newUrls, 0, l, urls.length, newUrls.length);
                        urls = l;
                        
                        int[] ln = new int[urls.length + newUrls.length];
                        System.arraycopy(lns, 0, ln, 0, lns.length);
                        System.arraycopy(newlns, 0, ln, lns.length, newlns.length);
                        lns = ln;
                    }
                }
            }
            URLs = urls;
            lineNumbers = lns;
            condition = ((MethodBreakpoint) b).getCondition();
        } else {
            return null;
        }
        
        boolean isConditional = (condition != null) &&
            !"".equals (condition.trim ()); // NOI18N
        boolean isInvalid = b.getValidity() == VALIDITY.INVALID;
        String annotationType;
        if (b instanceof LineBreakpoint) {
            annotationType = b.isEnabled () ?
            (isConditional ? EditorContext.CONDITIONAL_BREAKPOINT_ANNOTATION_TYPE :
                             EditorContext.BREAKPOINT_ANNOTATION_TYPE) :
            (isConditional ? EditorContext.DISABLED_CONDITIONAL_BREAKPOINT_ANNOTATION_TYPE :
                             EditorContext.DISABLED_BREAKPOINT_ANNOTATION_TYPE);
        } else if (b instanceof FieldBreakpoint) {
            annotationType = b.isEnabled () ?
                EditorContext.FIELD_BREAKPOINT_ANNOTATION_TYPE :
                EditorContext.DISABLED_FIELD_BREAKPOINT_ANNOTATION_TYPE;
        } else if (b instanceof MethodBreakpoint) {
            annotationType = b.isEnabled () ?
                EditorContext.METHOD_BREAKPOINT_ANNOTATION_TYPE :
                EditorContext.DISABLED_METHOD_BREAKPOINT_ANNOTATION_TYPE;
        } else {
            return null;
        }
        if (isInvalid && b.isEnabled ()) annotationType += "_broken";

        List annotations = new ArrayList(URLs.length);
        for (int i = 0; i < URLs.length; i++) {
            if (lineNumbers[i] >= 1) {
                Object annotation = getContext().annotate (URLs[i], lineNumbers[i], annotationType, null);
                if (annotation != null) {
                    annotations.add(annotation);
                }
            }
        }
        if (annotations.size() == 0) {
            return null;
        } else {
            return annotations.toArray();
        }
    }
    
    private static String[] getClassURLs(String className) {
        SourcePathProvider spp = SourcePath.getDefaultContext();
        // TODO: spp.getAllURLs(className, true);
        try {
        return (String[]) spp.getClass().getMethod("getAllURLs", new Class[] { String.class, Boolean.TYPE }).
                invoke(spp, new Object[] { className, Boolean.TRUE });
        } catch (Exception ex) {
            ErrorManager.getDefault().notify(ex);
            return new String[0];
        }
    }

    public static String getRelativePath (
        JPDAThread thread,
        String stratumn
    ) {
        try {
            return convertSlash (thread.getSourcePath (stratumn));
        } catch (AbsentInformationException e) {
            return getRelativePath (thread.getClassName ());
        }
    }

    public static String getRelativePath (
        CallStackFrame csf,
        String stratumn
    ) {
        try {
            return convertSlash (csf.getSourcePath (stratumn));
        } catch (AbsentInformationException e) {
            return getRelativePath (csf.getClassName ());
        }
    }

    public static String getRelativePath (
        String className
    ) {
        int i = className.indexOf ('$');
        if (i > 0) className = className.substring (0, i);
        String sourceName = className.replace 
            ('.', '/') + ".scala";
        return sourceName;
    }
    
    private static String convertSlash (String original) {
        return original.replace (File.separatorChar, '/');
    }

    
    // innerclasses ............................................................
    
    private static class CompoundContextProvider extends EditorContext {

        private EditorContext cp1, cp2;
        
        CompoundContextProvider (
            EditorContext cp1,
            EditorContext cp2
        ) {
            this.cp1 = cp1;
            this.cp2 = cp2;
        }

        public void createTimeStamp (Object timeStamp) {
            cp1.createTimeStamp (timeStamp);
            cp2.createTimeStamp (timeStamp);
        }

        public void disposeTimeStamp (Object timeStamp) {
            cp1.disposeTimeStamp (timeStamp);
            cp2.disposeTimeStamp (timeStamp);
        }
        
        public void updateTimeStamp (Object timeStamp, String url) {
            cp1.updateTimeStamp (timeStamp, url);
            cp2.updateTimeStamp (timeStamp, url);
        }

        public String getCurrentClassName () {
            String s = cp1.getCurrentClassName ();
            if (s.trim ().length () < 1)
                return cp2.getCurrentClassName ();
            return s;
        }

        public String getCurrentURL () {
            String s = cp1.getCurrentURL ();
            if (s.trim ().length () < 1)
                return cp2.getCurrentURL ();
            return s;
        }
        
        public String getCurrentFieldName () {
            String s = cp1.getCurrentFieldName ();
            if ( (s == null) || (s.trim ().length () < 1))
                return cp2.getCurrentFieldName ();
            return s;
        }
        
        public int getCurrentLineNumber () {
            int i = cp1.getCurrentLineNumber ();
            if (i < 1)
                return cp2.getCurrentLineNumber ();
            return i;
        }
        
        public String getCurrentMethodName () {
            String s = cp1.getCurrentMethodName ();
            if ( (s == null) || (s.trim ().length () < 1))
                return cp2.getCurrentMethodName ();
            return s;
        }
        
        public String getCurrentMethodSignature() {
            String s = null;
            try {
                s = (String) cp1.getClass().getMethod("getCurrentMethodSignature", new Class[] {}). // NOI18N
                        invoke(getContext(), new Object[] {});
            } catch (java.lang.reflect.InvocationTargetException itex) {
                Throwable tex = itex.getTargetException();
                if (tex instanceof RuntimeException) {
                    throw (RuntimeException) tex;
                } else {
                    ErrorManager.getDefault().notify(tex);
                    return null;
                }
            } catch (Exception ex) {
                // Ignore, we have another attempt with cp2
                //ErrorManager.getDefault().notify(ex);
            }
            if ( (s == null) || (s.trim ().length () < 1)) {
                try {
                    s = (String) cp2.getClass().getMethod("getCurrentMethodSignature", new Class[] {}). // NOI18N
                            invoke(getContext(), new Object[] {});
                } catch (java.lang.reflect.InvocationTargetException itex) {
                    Throwable tex = itex.getTargetException();
                    if (tex instanceof RuntimeException) {
                        throw (RuntimeException) tex;
                    } else {
                        ErrorManager.getDefault().notify(tex);
                        return null;
                    }
                } catch (Exception ex) {
                    ErrorManager.getDefault().notify(ex);
                    return null;
                }
            }
            return s;
        }
        
        public String getSelectedIdentifier () {
            String s = cp1.getSelectedIdentifier ();
            if ( (s == null) || (s.trim ().length () < 1))
                return cp2.getSelectedIdentifier ();
            return s;
        }
        
        public String getSelectedMethodName () {
            String s = cp1.getSelectedMethodName ();
            if ( (s == null) || (s.trim ().length () < 1))
                return cp2.getSelectedMethodName ();
            return s;
        }
        
        public void removeAnnotation (Object value) {
            if (value instanceof List) {
                for (Iterator iter = ((List) value).iterator(); iter.hasNext();) {
                    CompoundAnnotation ca = (CompoundAnnotation) iter.next();
                    if (ca.annotation1 != null) {
                        cp1.removeAnnotation (ca.annotation1);
                    }
                    if (ca.annotation2 != null) {
                        cp2.removeAnnotation (ca.annotation2);
                    }
                }
                return;
            }
            CompoundAnnotation ca = (CompoundAnnotation) value;
            if (ca.annotation1 != null) {
                cp1.removeAnnotation (ca.annotation1);
            }
            if (ca.annotation2 != null) {
                cp2.removeAnnotation (ca.annotation2);
            }
        }

        public Object annotate (
            String sourceName,
            int lineNumber,
            String annotationType,
            Object timeStamp
        ) {
            CompoundAnnotation ca = new CompoundAnnotation ();
            ca.annotation1 = cp1.annotate
                (sourceName, lineNumber, annotationType, timeStamp);
            ca.annotation2 = cp2.annotate
                (sourceName, lineNumber, annotationType, timeStamp);
            if (ca.annotation1 != null || ca.annotation2 != null) {
                return ca;
            } else {
                return null;
            }
        }

        @Override
        public Object annotate(String url, int lineNumber, String annotationType, Object timeStamp, JPDAThread thread) {
            CompoundAnnotation ca = new CompoundAnnotation ();
            ca.annotation1 = cp1.annotate
                (url, lineNumber, annotationType, timeStamp, thread);
            ca.annotation2 = cp2.annotate
                (url, lineNumber, annotationType, timeStamp, thread);
            if (ca.annotation1 != null || ca.annotation2 != null) {
                return ca;
            } else {
                return null;
            }
        }

        @Override
        public Object annotate(String url, int startPosition, int endPosition, String annotationType, Object timeStamp) {
            CompoundAnnotation ca = new CompoundAnnotation ();
            ca.annotation1 = cp1.annotate
                (url, startPosition, endPosition, annotationType, timeStamp);
            ca.annotation2 = cp2.annotate
                (url, startPosition, endPosition, annotationType, timeStamp);
            if (ca.annotation1 != null || ca.annotation2 != null) {
                return ca;
            } else {
                return null;
            }
        }                

        public int getLineNumber (Object annotation, Object timeStamp) {
            CompoundAnnotation ca = new CompoundAnnotation ();
            int ln;
            if (ca.annotation1 != null) {
                ln = cp1.getLineNumber (ca.annotation1, timeStamp);
            } else {
                ln = -1;
            }
            if (ln >= 0) return ln;
            if (ca.annotation2 != null) {
                return cp2.getLineNumber (ca.annotation2, timeStamp);
            } else {
                return -1;
            }
        }

        public boolean showSource (String sourceName, int lineNumber, Object timeStamp) {
            return cp1.showSource (sourceName, lineNumber, timeStamp) |
                   cp2.showSource (sourceName, lineNumber, timeStamp);
        }
    
        public int getFieldLineNumber (
            String url, 
            String className, 
            String fieldName
        ) {
            int ln = cp1.getFieldLineNumber (url, className, fieldName);
            if (ln != -1) return ln;
            return cp2.getFieldLineNumber (url, className, fieldName);
        }
    
        public String getClassName (
            String url, 
            int lineNumber
        ) {
            String className = cp1.getClassName (url, lineNumber);
            if (className != null) return className;
            return cp2.getClassName (url, lineNumber);
        }
    
        public String[] getImports (String url) {
            String[] r1 = cp1.getImports (url);
            String[] r2 = cp2.getImports (url);
            String[] r = new String [r1.length + r2.length];
            System.arraycopy (r1, 0, r, 0, r1.length);
            System.arraycopy (r2, 0, r, r1.length, r2.length);
            return r;
        }
        
        public void addPropertyChangeListener (PropertyChangeListener l) {
            cp1.addPropertyChangeListener (l);
            cp2.addPropertyChangeListener (l);
        }
        
        public void removePropertyChangeListener (PropertyChangeListener l) {
            cp1.removePropertyChangeListener (l);
            cp2.removePropertyChangeListener (l);
        }
        
        public void addPropertyChangeListener (
            String propertyName, 
            PropertyChangeListener l
        ) {
            cp1.addPropertyChangeListener (propertyName, l);
            cp2.addPropertyChangeListener (propertyName, l);
        }
        
        public void removePropertyChangeListener (
            String propertyName, 
            PropertyChangeListener l
        ) {
            cp1.removePropertyChangeListener (propertyName, l);
            cp2.removePropertyChangeListener (propertyName, l);
        }
    }
    
    private static class CompoundAnnotation {
        Object annotation1;
        Object annotation2;
    }
}

