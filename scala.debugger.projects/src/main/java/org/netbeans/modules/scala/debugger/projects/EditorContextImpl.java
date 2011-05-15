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
package org.netbeans.modules.scala.debugger.projects;

import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import javax.swing.text.StyledDocument;
import javax.swing.JEditorPane;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePathScanner;

import java.util.Collections;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;

import org.netbeans.api.debugger.jpda.LineBreakpoint;

import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.JavaSource;

import org.netbeans.modules.parsing.api.ResultIterator;
import org.openide.ErrorManager;
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.URLMapper;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.text.Line;
import org.openide.text.NbDocument;
import org.openide.util.Utilities;
import org.openide.util.WeakListeners;

import org.netbeans.spi.debugger.ui.EditorContextDispatcher;

import org.netbeans.editor.JumpList;
import org.netbeans.modules.csl.api.ElementKind;
import org.netbeans.modules.parsing.api.ParserManager;
import org.netbeans.modules.parsing.api.Source;
import org.netbeans.modules.parsing.api.UserTask;
import org.netbeans.modules.parsing.spi.ParseException;
import org.netbeans.modules.scala.core.ScalaParserResult;
import org.netbeans.modules.scala.core.ScalaSourceUtil;
import org.netbeans.spi.debugger.jpda.EditorContext;
import org.netbeans.spi.debugger.jpda.SourcePathProvider;

/**
 *
 * @author Jan Jancura
 * @author Caoyuan Deng
 * 
 * @todo This EditorContextImpl and the EditorContextImpl in debugger.jpda.projects
 * should be instanced and get lookuped in org.netbeans.modules.debugger.jpda.EditorContextImpl#getContext,
 * but, it seems org.netbeans.api.debugger.Lookup won't init the unloaded modules' META-INFO/debugger/
 * services. We have to apply following ugly hacking:
 *     1. Add debugger.jpda.projects module as run-dependency of this modules, so, 
 *        when this module is loaded first, debugger.jpda.projects module will also
 *        be loaded.
 *     2. Set this module as eager, so, even debugger.jpda.projects is loaded first,
 *        this module will also be loaded
 * 
 */
public class EditorContextImpl extends EditorContext {

    private static String fronting =
            System.getProperty("netbeans.debugger.fronting");
    private PropertyChangeSupport pcs;
    private Map annotationToURL = new HashMap();
    private PropertyChangeListener dispatchListener;
    private EditorContextDispatcher contextDispatcher;

    {
        pcs = new PropertyChangeSupport(this);
        dispatchListener = new EditorContextDispatchListener();
        contextDispatcher = EditorContextDispatcher.getDefault();
        contextDispatcher.addPropertyChangeListener("text/x-scala",
                WeakListeners.propertyChange(dispatchListener, contextDispatcher));
    }

    /**
     * Shows source with given url on given line number.
     *
     * @param url a url of source to be shown
     * @param lineNumber a number of line to be shown
     * @param timeStamp a time stamp to be used
     */
    public boolean showSource(String url, int lineNumber, Object timeStamp) {
        Line l = showSourceLine(url, lineNumber, timeStamp);
        if (l != null) {
            addPositionToJumpList(url, l, 0);
        }
        return l != null;
    }

    static Line showSourceLine(String url, int lineNumber, Object timeStamp) {
        Line l = LineTranslations.getTranslations().getLine(url, lineNumber, timeStamp); // false = use original ln
        if (l == null) {
            ErrorManager.getDefault().log(ErrorManager.WARNING,
                    "Show Source: Have no line for URL = " + url + ", line number = " + lineNumber);
            return null;
        }
        if ("true".equalsIgnoreCase(fronting) || Utilities.isWindows()) {
            l.show(Line.SHOW_REUSE);
            l.show(Line.SHOW_TOFRONT); //FIX 47825
        } else {
            l.show(Line.SHOW_REUSE);
        }
        return l;
    }

    /**
     * Shows source with given url on given line number.
     *
     * @param url a url of source to be shown
     * @param lineNumber a number of line to be shown
     * @param timeStamp a time stamp to be used
     */
    public boolean showSource(String url, int lineNumber, int column, int length, Object timeStamp) {
        Line l = LineTranslations.getTranslations().getLine(url, lineNumber, timeStamp); // false = use original ln
        if (l == null) {
            ErrorManager.getDefault().log(ErrorManager.WARNING,
                    "Show Source: Have no line for URL = " + url + ", line number = " + lineNumber);
            return false;
        }
        if ("true".equalsIgnoreCase(fronting) || Utilities.isWindows()) {
            l.show(Line.SHOW_TOFRONT, column); //FIX 47825
        } else {
            l.show(Line.SHOW_GOTO, column);
        }
        addPositionToJumpList(url, l, column);
        return true;
    }

    /** Add the line offset into the jump history */
    private void addPositionToJumpList(String url, Line l, int column) {
        DataObject dataObject = getDataObject(url);
        if (dataObject != null) {
            EditorCookie ec = dataObject.getLookup().lookup(EditorCookie.class);
            if (ec != null) {
                try {
                    StyledDocument doc = ec.openDocument();
                    JEditorPane[] eps = ec.getOpenedPanes();
                    if (eps != null && eps.length > 0) {
                        JumpList.addEntry(eps[0], NbDocument.findLineOffset(doc, l.getLineNumber()) + column);
                    }
                } catch (java.io.IOException ioex) {
                    ErrorManager.getDefault().notify(ioex);
                }
            }
        }
    }

    /**
     * Creates a new time stamp.
     *
     * @param timeStamp a new time stamp
     */
    public void createTimeStamp(Object timeStamp) {
        LineTranslations.getTranslations().createTimeStamp(timeStamp);
    }

    /**
     * Disposes given time stamp.
     *
     * @param timeStamp a time stamp to be disposed
     */
    public void disposeTimeStamp(Object timeStamp) {
        LineTranslations.getTranslations().disposeTimeStamp(timeStamp);
    }

    /**@Node:
     * the chained annotate method from EditorContextImpl under debug.jpda.projects
     * will also be called, so we do not need add a reduantant annotation
     */
    public Object annotate(
            String url,
            int lineNumber,
            String annotationType,
            Object timeStamp) {
        return null;
        //return annotate(url, lineNumber, annotationType, timeStamp, null);
    }
//    public Object annotate (
//        String url,
//        int lineNumber,
//        String annotationType,
//        Object timeStamp,
//        JPDAThread thread
//    ) {
//        Line l =  LineTranslations.getTranslations().getLine (
//            url,
//            lineNumber,
//            (timeStamp instanceof JPDABreakpoint) ? null : timeStamp
//        );
//        if (l == null) return null;
//        Annotation annotation;
//        if (timeStamp instanceof JPDABreakpoint) {
//            annotation = new DebuggerBreakpointAnnotation(annotationType, l, (JPDABreakpoint) timeStamp);
//        } else {
//            annotation = new DebuggerAnnotation (annotationType, l, thread);
//        }
//        annotationToURL.put (annotation, url);
//
//        return annotation;
//    }
//
//    public Object annotate (
//        String url,
//        int startPosition,
//        int endPosition,
//        String annotationType,
//        Object timeStamp
//    ) {
//        AttributeSet attrs;
//        if (EditorContext.CURRENT_LAST_OPERATION_ANNOTATION_TYPE.equals(annotationType)) {
//            attrs = AttributesUtilities.createImmutable(EditorStyleConstants.WaveUnderlineColor, getColor(annotationType));
//        } else {
//            attrs = AttributesUtilities.createImmutable(StyleConstants.Background, getColor(annotationType));
//        }
//        DebuggerAnnotation annotation;
//        try {
//            annotation = new DebuggerAnnotation(annotationType, attrs, startPosition, endPosition,
//                    URLMapper.findFileObject(new URL(url)));
//        } catch (MalformedURLException ex) {
//            RuntimeException rex = new RuntimeException("Bad URL: "+url);
//            rex.initCause(ex);
//            throw rex;
//        }
//        annotationToURL.put (annotation, url);
//
//        return annotation;
//    }

    private static Color getColor(String annotationType) {
        if (annotationType.endsWith("_broken")) {
            annotationType = annotationType.substring(0, annotationType.length() - "_broken".length());
        }
        if (EditorContext.BREAKPOINT_ANNOTATION_TYPE.equals(annotationType)) {
            return new Color(0xFC9D9F);
        } else if (EditorContext.CURRENT_LINE_ANNOTATION_TYPE.equals(annotationType) ||
                EditorContext.CURRENT_OUT_OPERATION_ANNOTATION_TYPE.equals(annotationType)) {
            return new Color(0xBDE6AA);
        } else if (EditorContext.CURRENT_EXPRESSION_CURRENT_LINE_ANNOTATION_TYPE.equals(annotationType)) {
            return new Color(0xE9FFE6); // 0xE3FFD2// 0xD1FFBC
        } else if (EditorContext.CURRENT_LAST_OPERATION_ANNOTATION_TYPE.equals(annotationType)) {
            return new Color(0x99BB8A);
        } else {
            return new Color(0x0000FF);
        }
    }

    /**
     * Removes given annotation.
     *
     * @return true if annotation has been successfully removed
     */
    public void removeAnnotation(
            Object a) {
        return;
//        if (a instanceof Collection) {
//            Collection annotations = ((Collection) a);
//            for (Iterator it = annotations.iterator(); it.hasNext(); ) {
//                removeAnnotation((Annotation) it.next());
//            }
//        } else {
//            removeAnnotation((Annotation) a);
//        }
    }

//    private void removeAnnotation(Annotation annotation) {
//        annotation.detach ();
//        annotationToURL.remove (annotation);
//    }
    /**
     * Returns line number given annotation is associated with.
     *
     * @param annotation an annotation, or an array of "url" and new Integer(line number)
     * @param timeStamp a time stamp to be used
     *
     * @return line number given annotation is associated with
     */
    public int getLineNumber(
            Object annotation,
            Object timeStamp) {
        if (annotation instanceof LineBreakpoint) {
            // A sort of hack to be able to retrieve the original line.
            LineBreakpoint lb = (LineBreakpoint) annotation;
            return LineTranslations.getTranslations().getOriginalLineNumber(lb, timeStamp);
        }
        /*if (annotation instanceof Object[]) {
        // A sort of hack to be able to retrieve the original line.
        Object[] urlLine = (Object[]) annotation;
        String url = (String) urlLine[0];
        int line = ((Integer) urlLine[1]).intValue();
        return LineTranslations.getTranslations().getOriginalLineNumber(url, line, timeStamp);
        }*/
        Line line;
        if (annotation instanceof DebuggerBreakpointAnnotation) {
            line = ((DebuggerBreakpointAnnotation) annotation).getLine();
        } else {
            line = ((DebuggerAnnotation) annotation).getLine();
        }
        if (timeStamp == null) {
            return line.getLineNumber() + 1;
        }
        String url = (String) annotationToURL.get(annotation);
        Line.Set lineSet = LineTranslations.getTranslations().getLineSet(url, timeStamp);
        return lineSet.getOriginalLineNumber(line) + 1;
    }

    /**
     * Updates timeStamp for gived url.
     *
     * @param timeStamp time stamp to be updated
     * @param url an url
     */
    public void updateTimeStamp(Object timeStamp, String url) {
        LineTranslations.getTranslations().updateTimeStamp(timeStamp, url);
    }

    /**
     * Returns number of line currently selected in editor or <code>-1</code>.
     *
     * @return number of line currently selected in editor or <code>-1</code>
     */
    public int getCurrentLineNumber() {
        return contextDispatcher.getCurrentLineNumber();
    }

    /**
     * Returns number of line currently selected in editor or <code>-1</code>.
     *
     * @return number of line currently selected in editor or <code>-1</code>
     */
    public int getCurrentOffset() {
        JEditorPane ep = contextDispatcher.getCurrentEditor();
        if (ep == null) {
            return -1;
        }
        Caret caret = ep.getCaret();
        if (caret == null) {
            return -1;
        }
        return caret.getDot();
    }

    /**
     * Returns name of class currently selected in editor or empty string.
     *
     * @return name of class currently selected in editor or empty string
     */
    public String getCurrentClassName() {
        String currentClass = getCurrentElement(ElementKind.CLASS);
        if (currentClass == null) {
            return "";
        } else {
            return currentClass;
        }
    }

    /**
     * Returns URL of source currently selected in editor or empty string.
     *
     * @return URL of source currently selected in editor or empty string
     */
    public String getCurrentURL() {
        return contextDispatcher.getCurrentURLAsString();
    }

    /**
     * Returns name of method currently selected in editor or empty string.
     *
     * @return name of method currently selected in editor or empty string
     */
    public String getCurrentMethodName() {
        String currentMethod = getCurrentElement(ElementKind.METHOD);
        if (currentMethod == null) {
            return "";
        } else {
            return currentMethod;
        }
    }

    /**
     * Returns signature of method currently selected in editor or null.
     *
     * @return signature of method currently selected in editor or null
     */
    public String getCurrentMethodSignature() {
        final Element[] elementPtr = new Element[]{null};
        try {
            getCurrentElement(ElementKind.METHOD, elementPtr);
        } catch (final java.awt.IllegalComponentStateException icse) {
            throw new java.awt.IllegalComponentStateException() {

                @Override
                public String getMessage() {
                    icse.getMessage();
                    return createSignature((ExecutableElement) elementPtr[0]);
                }
            };
        }
        if (elementPtr[0] != null) {
            return createSignature((ExecutableElement) elementPtr[0]);
        } else {
            return null;
        }
    }

    /**
     * Returns name of field currently selected in editor or <code>null</code>.
     *
     * @return name of field currently selected in editor or <code>null</code>
     */
    public String getCurrentFieldName() {
        String currentField = getCurrentElement(ElementKind.FIELD);
        if (currentField == null) {
            return "";
        } else {
            return currentField;
        }
        //return getSelectedIdentifier ();
    }

    /**
     * Returns identifier currently selected in editor or <code>null</code>.
     *
     * @return identifier currently selected in editor or <code>null</code>
     */
    public String getSelectedIdentifier() {
        JEditorPane ep = contextDispatcher.getCurrentEditor();
        if (ep == null) {
            return null;
        }
        String s = ep.getSelectedText();
        if (s == null) {
            return null;
        }
        if (Utilities.isJavaIdentifier(s)) {
            return s;
        }
        return null;
    }

    /**
     * Returns method name currently selected in editor or empty string.
     *
     * @return method name currently selected in editor or empty string
     */
    public String getSelectedMethodName() {
        if (SwingUtilities.isEventDispatchThread()) {
            return getSelectedMethodName_();
        } else {
            final String[] mn = new String[1];
            try {
                SwingUtilities.invokeAndWait(new Runnable() {

                    public void run() {
                        mn[0] = getSelectedMethodName_();
                    }
                });
            } catch (InvocationTargetException ex) {
                ErrorManager.getDefault().notify(ex.getTargetException());
            } catch (InterruptedException ex) {
                // interrupted, ignored.
            }
            return mn[0];
        }
    }

    private String getSelectedMethodName_() {
        JEditorPane ep = contextDispatcher.getCurrentEditor();
        if (ep == null) {
            return "";
        }
        StyledDocument doc = (StyledDocument) ep.getDocument();
        if (doc == null) {
            return "";
        }
        int offset = ep.getCaret().getDot();
        String t = null;
//        if ( (ep.getSelectionStart () <= offset) &&
//             (offset <= ep.getSelectionEnd ())
//        )   t = ep.getSelectedText ();
//        if (t != null) return t;

        int line = NbDocument.findLineNumber(
                doc,
                offset);
        int col = NbDocument.findLineColumn(
                doc,
                offset);
        try {
            javax.swing.text.Element lineElem =
                    org.openide.text.NbDocument.findLineRootElement(doc).
                    getElement(line);

            if (lineElem == null) {
                return "";
            }
            int lineStartOffset = lineElem.getStartOffset();
            int lineLen = lineElem.getEndOffset() - lineStartOffset;
            // t contains current line in editor
            t = doc.getText(lineStartOffset, lineLen);

            int identStart = col;
            while (identStart > 0 &&
                    Character.isJavaIdentifierPart(
                    t.charAt(identStart - 1))) {
                identStart--;
            }

            int identEnd = col;
            while (identEnd < lineLen &&
                    Character.isJavaIdentifierPart(t.charAt(identEnd))) {
                identEnd++;
            }
            int i = t.indexOf('(', identEnd);
            if (i < 0) {
                return "";
            }
            if (t.substring(identEnd, i).trim().length() > 0) {
                return "";
            }

            if (identStart == identEnd) {
                return "";
            }
            return t.substring(identStart, identEnd);
        } catch (javax.swing.text.BadLocationException ex) {
            return "";
        }
    }

//    private static TypeElement getTypeElement(CompilationController ci,
//                                              String binaryName,
//                                              String[] classExcludeNames) {
//        ClassScanner cs = new ClassScanner(ci.getTrees(), ci.getElements(),
//                                           binaryName, classExcludeNames);
//        TypeElement te = cs.scan(ci.getCompilationUnit(), null);
//        if (te != null) {
//            return te;
//        } else {
//            return null;
//        }
//    }
    /**
     * Returns line number of given field in given class.
     *
     * @param url the url of file the class is deined in
     * @param className the name of class (or innerclass) the field is 
     *                  defined in
     * @param fieldName the name of field
     *
     * @return line number or -1
     */
    public int getFieldLineNumber(
            String url,
            final String className,
            final String fieldName) {
        final DataObject dataObject = getDataObject(url);
        if (dataObject == null) {
            return -1;
        }
        return getFieldLineNumber(dataObject.getPrimaryFile(), className, fieldName);
    }

    static int getFieldLineNumber(
            FileObject fo,
            final String className,
            final String fieldName) {
        Source source = Source.create(fo);
        if (source == null) {
            return -1;
        }
        final int[] result = new int[]{-1};

        final DataObject dataObject;
        try {
            dataObject = DataObject.find(fo);
        } catch (DataObjectNotFoundException ex) {
            return -1;
        }
        try {
            ParserManager.parse(Collections.singleton(source), new UserTask() {

                @Override
                public void run(ResultIterator resultIterator) throws Exception {
//                    Elements elms = ci.getElements();
//                    TypeElement classElement = getTypeElement(ci, className, null);
//                    if (classElement == null) {
//                        return;
//                    }
//                    List classMemberElements = elms.getAllMembers(classElement);
//                    for (Iterator it = classMemberElements.iterator(); it.hasNext();) {
//                        Element elm = (Element) it.next();
//                        if (elm.getKind() == ElementKind.FIELD) {
//                            String name = ((VariableElement) elm).getSimpleName().toString();
//                            if (name.equals(fieldName)) {
//                                SourcePositions positions = ci.getTrees().getSourcePositions();
//                                Tree tree = ci.getTrees().getTree(elm);
//                                int pos = (int) positions.getStartPosition(ci.getCompilationUnit(), tree);
//                                EditorCookie editor = (EditorCookie) dataObject.getCookie(EditorCookie.class);
//                                result[0] = NbDocument.findLineNumber(editor.openDocument(), pos) + 1;
//                            //return elms.getSourcePosition(elm).getLine();
//                            }
//                        }
//                    }
                }
            });
        } catch (ParseException ex) {
            ErrorManager.getDefault().notify(ex);
            return -1;
        }
        return result[0];
        /*
        CompilationUnitTree cutree = ci.getTree();
        if (cutree == null) return -1;
        List typeDecls = cutree.getTypeDecls();
        ClassTree ctree = findClassTree(typeDecls, className);
         */
        /*
        Elements elms = ci.getElements();
        SourceCookie.Editor sc = (SourceCookie.Editor) dataObject.getCookie
        (SourceCookie.Editor.class);
        if (sc == null) return -1;
        sc.open ();
        StyledDocument sd = sc.getDocument ();
        if (sd == null) return -1;
        ClassElement[] classes = sc.getSource ().getAllClasses ();
        FieldElement fe = null;
        int i, k = classes.length;
        for (i = 0; i < k; i++)
        if (classes [i].getName ().getFullName ().equals (className)) {
        fe = classes [i].getField (Identifier.create (fieldName));
        break;
        }
        if (fe == null) return -1;
        int position = sc.sourceToText (fe).getStartOffset ();
        return NbDocument.findLineNumber (sd, position) + 1;
         */
    }

    /**
     * Returns line number of given method in given class.
     *
     * @param url the url of file the class is deined in
     * @param className the name of class (or innerclass) the method is 
     *                  defined in
     * @param methodName the name of method
     * @param methodSignature the JNI-style signature of the method.
     *        If <code>null</code>, then the first method found is returned.
     *
     * @return line number or -1
     */
    @Override
    public int getMethodLineNumber(
            String url,
            final String className,
            final String methodName,
            final String methodSignature) {
        final DataObject dataObject = getDataObject(url);
        if (dataObject == null) {
            return -1;
        }
        int[] lns = getMethodLineNumbers(dataObject.getPrimaryFile(), className, null, methodName, methodSignature);
        if (lns.length == 0) {
            return -1;
        } else {
            return lns[0];
        }
    }

    static int[] getMethodLineNumbers(
            FileObject fo,
            final String className,
            final String[] classExcludeNames,
            final String methodName,
            final String methodSignature) {
        Source source = Source.create(fo);
        if (source == null) {
            return new int[]{};
        }
        final List<Integer> result = new ArrayList<Integer>();

        final DataObject dataObject;
        try {
            dataObject = DataObject.find(fo);
        } catch (DataObjectNotFoundException ex) {
            return new int[]{};
        }
        try {
            ParserManager.parse(Collections.singleton(source), new UserTask() {

                @Override
                public void run(ResultIterator resultIterator) throws Exception {
//                    TypeElement classElement = getTypeElement(ci, className, classExcludeNames);
//                    if (classElement == null) {
//                        return;
//                    }
//                    List classMemberElements = ci.getElements().getAllMembers(classElement);
//                    for (Iterator it = classMemberElements.iterator(); it.hasNext();) {
//                        Element elm = (Element) it.next();
//                        if (elm.getKind() == ElementKind.METHOD || elm.getKind() == ElementKind.CONSTRUCTOR) {
//                            String name;
//                            if (elm.getKind() == ElementKind.CONSTRUCTOR && !methodName.equals("<init>")) {
//                                name = elm.getEnclosingElement().getSimpleName().toString();
//                            } else {
//                                name = elm.getSimpleName().toString();
//                            }
//                            if (name.equals(methodName)) {
//                                if (methodSignature == null || egualMethodSignatures(methodSignature, createSignature((ExecutableElement) elm))) {
//                                    SourcePositions positions = ci.getTrees().getSourcePositions();
//                                    Tree tree = ci.getTrees().getTree(elm);
//                                    int pos = (int) positions.getStartPosition(ci.getCompilationUnit(), tree);
//                                    EditorCookie editor = (EditorCookie) dataObject.getCookie(EditorCookie.class);
//                                    result.add(new Integer(NbDocument.findLineNumber(editor.openDocument(), pos) + 1));
//                                }
//                            }
//                        }
//                    }
                }
            });
        } catch (ParseException ex) {
            ErrorManager.getDefault().notify(ex);
            return new int[]{};
        }
        int[] resultArray = new int[result.size()];
        for (int i = 0; i < resultArray.length; i++) {
            resultArray[i] = result.get(i).intValue();
        }
        return resultArray;
    }

    private static boolean egualMethodSignatures(String s1, String s2) {
        int i = s1.lastIndexOf(")");
        if (i > 0) {
            s1 = s1.substring(0, i);
        }
        i = s2.lastIndexOf(")");
        if (i > 0) {
            s2 = s2.substring(0, i);
        }
        return s1.equals(s2);
    }

    /** @return { "method name", "method signature", "enclosing class name" }
     */
    @Override
    public String[] getCurrentMethodDeclaration() {
        FileObject fo = contextDispatcher.getCurrentFile();
        if (fo == null) {
            return null;
        }
        JEditorPane ep = contextDispatcher.getCurrentEditor();
        JavaSource js = JavaSource.forFileObject(fo);
        if (js == null) {
            return null;
        }
        final int currentOffset = (ep == null) ? 0 : ep.getCaretPosition();
        final String[] currentMethodPtr = new String[]{null, null, null};
        final Future<Void> scanFinished;
//        try {
//            scanFinished = js.runWhenScanFinished(new CancellableTask<CompilationController>() {
//
//                public void cancel() {
//                }
//
//                public void run(CompilationController ci) throws Exception {
//                    if (ci.toPhase(Phase.RESOLVED).compareTo(Phase.RESOLVED) < 0) {//TODO: ELEMENTS_RESOLVED may be sufficient
//                        ErrorManager.getDefault().log(ErrorManager.WARNING,
//                                "Unable to resolve " + ci.getFileObject() + " to phase " + Phase.RESOLVED + ", current phase = " + ci.getPhase() +
//                                "\nFree memory = " + Runtime.getRuntime().freeMemory());
//                        return;
//                    }
//                    int offset = currentOffset;
//                    //AstScope scope = ci.getTreeUtilities().scopeFor(offset);
//                    String text = ci.getText();
//                    int l = text.length();
//                    char c = 0;
//                    while (offset < l && (c = text.charAt(offset)) != '(' && c != ')' && c != '\n' && c != '\r') {
//                        offset++;
//                    }
//                    if (offset >= l) {
//                        return;
//                    }
//                    if (c == '(') {
//                        offset--;
//                    }
//
//                    Tree tree = ci.getTreeUtilities().pathFor(offset).getLeaf();
//                    if (tree.getKind() == Tree.Kind.METHOD) {
//                        Element el = ci.getTrees().getElement(ci.getTrees().getPath(ci.getCompilationUnit(), tree));
//
//                        //Element el = ci.getTrees().getElement(ci.getTreeUtilities().pathFor(offset));
//                        if (el != null && (el.getKind() == ElementKind.METHOD || el.getKind() == ElementKind.CONSTRUCTOR)) {
//                            currentMethodPtr[0] = el.getSimpleName().toString();
//                            if (currentMethodPtr[0].equals("<init>")) {
//                                // The constructor name is the class name:
//                                currentMethodPtr[0] = el.getEnclosingElement().getSimpleName().toString();
//                            }
//                            currentMethodPtr[1] = createSignature((ExecutableElement) el);
//                            Element enclosingClassElement = el;
//                            TypeElement te = null; // SourceUtils.getEnclosingTypeElement(el);
//                            while (enclosingClassElement != null) {
//                                ElementKind kind = enclosingClassElement.getKind();
//                                if (kind == ElementKind.CLASS || kind == ElementKind.INTERFACE) {
//                                    te = (TypeElement) enclosingClassElement;
//                                    break;
//                                } else {
//                                    enclosingClassElement = enclosingClassElement.getEnclosingElement();
//                                }
//                            }
//                            if (te != null) {
//                                currentMethodPtr[2] = ElementUtilities.getBinaryName(te);
//                            }
//                        }
//                    }
//                }
//            }, true);
//            if (!scanFinished.isDone()) {
//                if (java.awt.EventQueue.isDispatchThread()) {
//                    // Hack: We should not wait for the scan in AWT!
//                    //       Thus we throw IllegalComponentStateException,
//                    //       which returns the data upon call to getMessage()
//                    throw new java.awt.IllegalComponentStateException() {
//
//                        private void waitScanFinished() {
//                            try {
//                                scanFinished.get();
//                            } catch (InterruptedException iex) {
//                            } catch (java.util.concurrent.ExecutionException eex) {
//                                ErrorManager.getDefault().notify(eex);
//                            }
//                        }
//
//                        public String getMessage() {
//                            waitScanFinished();
//                            return currentMethodPtr[0];
//                        }
//
//                        public String getLocalizedMessage() {
//                            waitScanFinished();
//                            return currentMethodPtr[1];
//                        }
//                    };
//                } else {
//                    try {
//                        scanFinished.get();
//                    } catch (InterruptedException iex) {
//                        return null;
//                    } catch (java.util.concurrent.ExecutionException eex) {
//                        ErrorManager.getDefault().notify(eex);
//                        return null;
//                    }
//                }
//            }
//        } catch (IOException ioex) {
//            ErrorManager.getDefault().notify(ioex);
//            return null;
//        }
//        if (currentMethodPtr[0] != null) {
//            return currentMethodPtr;
//        } else {
//            return null;
//        }
        return null;
    }

    private static String createSignature(ExecutableElement elm) {
        StringBuilder signature = new StringBuilder("(");
        for (VariableElement param : elm.getParameters()) {
            String paramType = param.asType().toString();
            signature.append(getSignature(paramType));
        }
        signature.append(')');
        String returnType = elm.getReturnType().toString();
        signature.append(getSignature(returnType));
        return signature.toString();
    }

    private static String getSignature(String javaType) {
        if (javaType.equals("boolean")) {
            return "Z";
        } else if (javaType.equals("byte")) {
            return "B";
        } else if (javaType.equals("char")) {
            return "C";
        } else if (javaType.equals("short")) {
            return "S";
        } else if (javaType.equals("int")) {
            return "I";
        } else if (javaType.equals("long")) {
            return "J";
        } else if (javaType.equals("float")) {
            return "F";
        } else if (javaType.equals("double")) {
            return "D";
        } else if (javaType.endsWith("[]")) {
            return "[" + getSignature(javaType.substring(0, javaType.length() - 2));
        } else {
            return "L" + javaType.replace('.', '/') + ";";
        }
    }

    /**
     * Returns binary class name for given url and line number or null.
     *
     * @param url a url
     * @param lineNumber a line number
     *
     * @return binary class name for given url and line number or null
     */
    public String getClassName(String url, final int lineNumber) {
        DataObject dataObject = getDataObject(url);
        if (dataObject == null) {
            return null;
        }
        FileObject fo = dataObject.getPrimaryFile();
        if (fo == null) {
            return null;
        }
        Source source = Source.create(fo);
        //        if (js == null) return "";
        if (!"text/x-scala".equals(fo.getMIMEType())) {
            /** Should return null instead of "" here, 
             * @see org.netbeans.modules.debugger.jpda.EditorContextBridge#CompoundContextProvider#getClassName
             * @notice this has been fixed in main's rev: 30100e497ae4
             */
            return null;
        }
        EditorCookie ec = (EditorCookie) dataObject.getCookie(EditorCookie.class);
        if (ec == null) {
            return "";
        }
        StyledDocument doc;
        try {
            doc = ec.openDocument();
        } catch (IOException ex) {
            ErrorManager.getDefault().notify(ex);
            return "";
        }
        try {
            final int offset = NbDocument.findLineOffset(doc, lineNumber - 1);
            final String[] result = new String[]{""};

            ParserManager.parse(Collections.singleton(source), new UserTask() {

                @Override
                public void run(ResultIterator ri) throws Exception {
                    ScalaParserResult pResult = (ScalaParserResult) ri.getParserResult(offset);
                    String clzFqn = ScalaSourceUtil.getBinaryClassName(pResult, lineNumber);

                    if (clzFqn == null) {
                        ErrorManager.getDefault().log(
                                ErrorManager.WARNING,
                                "No enclosing class for " + pResult.getSnapshot().getSource().getFileObject() + ", offset = " + offset);
                        result[0] = "";
                    } else {
                        result[0] = clzFqn;
                    }
                }
            });
            return result[0];
        } catch (ParseException ex) {
            ErrorManager.getDefault().notify(ex);
            return "";
        } catch (IndexOutOfBoundsException ioobex) {
            //XXX: log the exception?
            return null;
        }
        /*
        SourceCookie.Editor sc = (SourceCookie.Editor) dataObject.getCookie
        (SourceCookie.Editor.class);
        if (sc == null) return null;
        StyledDocument sd = null;
        try {
        sd = sc.openDocument ();
        } catch (IOException ex) {
        }
        if (sd == null) return null;
        int offset;
        try {
        offset = NbDocument.findLineOffset (sd, lineNumber - 1);
        } catch (IndexOutOfBoundsException ioobex) {
        return null;
        }
        Element element = sc.findElement (offset);

        if (element == null) return "";
        if (element instanceof ClassElement)
        return getClassName ((ClassElement) element);
        if (element instanceof ConstructorElement)
        return getClassName (((ConstructorElement) element).getDeclaringClass ());
        if (element instanceof FieldElement)
        return getClassName (((FieldElement) element).getDeclaringClass ());
        if (element instanceof InitializerElement)
        return getClassName (((InitializerElement) element).getDeclaringClass());
        return "";
         */
    }

    @Override
    public Operation[] getOperations(String url, final int lineNumber,
            final BytecodeProvider bytecodeProvider) {
        DataObject dataObject = getDataObject(url);
        if (dataObject == null) {
            return null;
        }
        FileObject fileObject = dataObject.getPrimaryFile();
        if (fileObject == null) {
            return null;
        }
        if (!"text/x-scala".equals(fileObject.getMIMEType())) {
            return null;
        }

        // at the following step, do not return null to avoid another EditorContextImpl to process
        final Operation ops[][] = new Operation[1][];
        Source source = Source.create(dataObject.getPrimaryFile());
        if (source == null) {
            return ops[0];
        }
        EditorCookie ec = (EditorCookie) dataObject.getCookie(EditorCookie.class);
        if (ec == null) {
            return ops[0];
        }
        final StyledDocument doc;
        try {
            doc = ec.openDocument();
        } catch (IOException ex) {
            ErrorManager.getDefault().notify(ex);
            return ops[0];
        }
        final int offset = findLineOffset(doc, (int) lineNumber);
        try {
            ParserManager.parse(Collections.singleton(source), new UserTask() {

                @Override
                public void run(ResultIterator resultIterator) throws Exception {
//                    AstScope scope = ci.getTreeUtilities().scopeFor(offset);
//                    Element method = scope.getEnclosingMethod();
//                    if (method == null) {
//                        ops[0] = new Operation[]{};
//                        return;
//                    }
//                    Tree methodTree = ci.getTrees().getTree(method);
//                    CompilationUnitTree cu = ci.getCompilationUnit();
//                    ExpressionScanner scanner = new ExpressionScanner(lineNumber, cu, ci.getTrees().getSourcePositions());
//                    ExpressionScanner.ExpressionsInfo info = new ExpressionScanner.ExpressionsInfo();
//                    List<Tree> expTrees = methodTree.accept(scanner, info);
//
//                    //com.sun.source.tree.ExpressionTree expTree = scanner.getExpressionTree();
//                    if (expTrees == null || expTrees.size() == 0) {
//                        ops[0] = new Operation[]{};
//                        return;
//                    }
//                    //Tree[] expTrees = expTreeSet.toArray(new Tree[0]);
//                    SourcePositions sp = ci.getTrees().getSourcePositions();
//                    int treeStartLine =
//                            (int) cu.getLineMap().getLineNumber(
//                            sp.getStartPosition(cu, expTrees.get(0)));
//                    int treeEndLine =
//                            (int) cu.getLineMap().getLineNumber(
//                            sp.getEndPosition(cu, expTrees.get(expTrees.size() - 1)));
//
//                    int[] indexes = bytecodeProvider.indexAtLines(treeStartLine, treeEndLine);
//                    if (indexes == null) {
//                        return;
//                    }
//                    Map<Tree, Operation> nodeOperations = new HashMap<Tree, Operation>();
//                    ops[0] = AST2Bytecode.matchSourceTree2Bytecode(
//                            cu,
//                            ci,
//                            expTrees, info, bytecodeProvider.byteCodes(),
//                            indexes,
//                            bytecodeProvider.constantPool(),
//                            new OperationCreationDelegateImpl(),
//                            nodeOperations);
//                    if (ops[0] != null) {
//                        assignNextOperations(methodTree, cu, ci, bytecodeProvider, expTrees, info, nodeOperations);
//                    }
                }
            });
        } catch (ParseException ex) {
            ErrorManager.getDefault().notify(ex);
            return null;
        }
        return ops[0];
    }

    private void assignNextOperations(Tree methodTree,
            CompilationUnitTree cu,
            CompilationController ci,
            BytecodeProvider bytecodeProvider,
            List<Tree> treeNodes,
            ExpressionScanner.ExpressionsInfo info,
            Map<Tree, Operation> nodeOperations) {
        int length = treeNodes.size();
        for (int treeIndex = 0; treeIndex < length; treeIndex++) {
            Tree node = treeNodes.get(treeIndex);
            Set<Tree> nextNodes = info.getNextExpressions(node);
            if (nextNodes != null) {
                EditorContext.Operation op = nodeOperations.get(node);
                if (op == null) {
                    for (int backIndex = treeIndex - 1; backIndex >= 0; backIndex--) {
                        node = treeNodes.get(backIndex);
                        op = nodeOperations.get(node);
                        if (op != null) {
                            break;
                        }
                    }
                }
                if (op != null) {
//                    for (Tree t : nextNodes) {
//                        EditorContext.Operation nextOp = nodeOperations.get(t);
//                        if (nextOp == null) {
//                            SourcePositions sp = ci.getTrees().getSourcePositions();
//                            int treeStartLine =
//                                    (int) cu.getLineMap().getLineNumber(
//                                    sp.getStartPosition(cu, t));
//                            ExpressionScanner scanner = new ExpressionScanner(treeStartLine, cu, ci.getTrees().getSourcePositions());
//                            ExpressionScanner.ExpressionsInfo newInfo = new ExpressionScanner.ExpressionsInfo();
//                            List<Tree> newExpTrees = methodTree.accept(scanner, newInfo);
//                            treeStartLine =
//                                    (int) cu.getLineMap().getLineNumber(
//                                    sp.getStartPosition(cu, newExpTrees.get(0)));
//                            int treeEndLine =
//                                    (int) cu.getLineMap().getLineNumber(
//                                    sp.getEndPosition(cu, newExpTrees.get(newExpTrees.size() - 1)));
//
//                            int[] indexes = bytecodeProvider.indexAtLines(treeStartLine, treeEndLine);
//                            Map<Tree, Operation> newNodeOperations = new HashMap<Tree, Operation>();
//                            Operation[] newOps = AST2Bytecode.matchSourceTree2Bytecode(
//                                    cu,
//                                    ci,
//                                    newExpTrees, newInfo, bytecodeProvider.byteCodes(),
//                                    indexes,
//                                    bytecodeProvider.constantPool(),
//                                    new OperationCreationDelegateImpl(),
//                                    newNodeOperations);
//                            nextOp = newNodeOperations.get(t);
//                            if (nextOp == null) {
//                                // Next operation not found
//                                System.err.println("Next operation not found!");
//                                continue;
//                            }
//                        }
//                        addNextOperationTo(op, nextOp);
//                    }
                }
            }
        }

    }

    /** return the offset of the first non-whitespace character on the line,
    or -1 when the line does not exist
     */
    private static int findLineOffset(StyledDocument doc, int lineNumber) {
        int offset;
        try {
            offset = NbDocument.findLineOffset(doc, lineNumber - 1);
            int offset2 = NbDocument.findLineOffset(doc, lineNumber);
            try {
                String lineStr = doc.getText(offset, offset2 - offset);
                for (int i = 0; i < lineStr.length(); i++) {
                    if (!Character.isWhitespace(lineStr.charAt(i))) {
                        offset += i;
                        break;
                    }
                }
            } catch (BadLocationException ex) {
                // ignore
            }
        } catch (IndexOutOfBoundsException ioobex) {
            return -1;
        }
        return offset;
    }

    @Override
    public MethodArgument[] getArguments(String url, final Operation operation) {
        DataObject dataObject = getDataObject(url);
        if (dataObject == null) {
            return null;
        }
        FileObject fo = dataObject.getPrimaryFile();
        if (fo == null) {
            return null;
        }
        Source source = Source.create(fo);
        if (source == null) {
            return null;
        }
        final MethodArgument args[][] = new MethodArgument[1][];
        try {
            ParserManager.parse(Collections.singleton(source), new UserTask() {

                @Override
                public void run(ResultIterator resultIterator) throws Exception {
                    int offset = operation.getMethodEndPosition().getOffset();
//                    AstScope scope = ci.getTreeUtilities().scopeFor(offset);
//                    Element method = scope.getEnclosingMethod();
//                    if (method == null) {
//                        return;
//                    }
//                    Tree methodTree = ci.getTrees().getTree(method);
//                    CompilationUnitTree cu = ci.getCompilationUnit();
//                    MethodArgumentsScanner scanner =
//                            new MethodArgumentsScanner(offset, cu, ci.getTrees().getSourcePositions(), true,
//                            new OperationCreationDelegateImpl());
//                    args[0] = methodTree.accept(scanner, null);
//                    args[0] = scanner.getArguments();
                }
            });
        } catch (ParseException ex) {
            ErrorManager.getDefault().notify(ex);
            return null;
        }
        return args[0];
    }

    @Override
    public MethodArgument[] getArguments(String url, final int methodLineNumber) {
        DataObject dataObject = getDataObject(url);
        if (dataObject == null) {
            return null;
        }
        FileObject fo = dataObject.getPrimaryFile();
        if (fo == null) {
            return null;
        }
        Source source = Source.create(fo);
        if (source == null) {
            return null;
        }
        EditorCookie ec = (EditorCookie) dataObject.getCookie(EditorCookie.class);
        if (ec == null) {
            return null;
        }
        final StyledDocument doc;
        try {
            doc = ec.openDocument();
        } catch (IOException ex) {
            ErrorManager.getDefault().notify(ex);
            return null;
        }
        final int offset = findLineOffset(doc, methodLineNumber);
        final MethodArgument args[][] = new MethodArgument[1][];
        try {
            ParserManager.parse(Collections.singleton(source), new UserTask() {

                @Override
                public void run(ResultIterator resultIterator) throws Exception {
//                    AstScope scope = ci.getTreeUtilities().scopeFor(offset);
//                    Element clazz = scope.getEnclosingClass();
//                    if (clazz == null) {
//                        return;
//                    }
//                    Tree methodTree = ci.getTrees().getTree(clazz);
//                    CompilationUnitTree cu = ci.getCompilationUnit();
//                    MethodArgumentsScanner scanner =
//                            new MethodArgumentsScanner(methodLineNumber, cu, ci.getTrees().getSourcePositions(), false,
//                            new OperationCreationDelegateImpl());
//                    args[0] = methodTree.accept(scanner, null);
//                    args[0] = scanner.getArguments();
                }
            });
        } catch (ParseException ex) {
            ErrorManager.getDefault().notify(ex);
            return null;
        }
        return args[0];
    }

    /**
     * Returns list of imports for given source url.
     *
     * @param url the url of source file
     *
     * @return list of imports for given source url
     */
    public String[] getImports(
            String url) {
        DataObject dataObject = getDataObject(url);
        if (dataObject == null) {
            return new String[0];
        }
        FileObject fo = dataObject.getPrimaryFile();
        if (fo == null) {
            return new String[0];
        }
        Source source = Source.create(fo);
        if (source == null) {
            return new String[0];
        }
        final List<String> imports = new ArrayList<String>();
        try {
            ParserManager.parse(Collections.singleton(source), new UserTask() {

                @Override
                public void run(ResultIterator resultIterator) throws Exception {
//                    List importDecl = ci.getCompilationUnit().getImports();
//                    int i = 0;
//                    for (Iterator it = importDecl.iterator(); it.hasNext(); i++) {
//                        ImportTree itree = (ImportTree) it.next();
//                        String importStr = itree.getQualifiedIdentifier().toString();
//                        imports.add(importStr);
//                    }
                }
            });
        } catch (ParseException ex) {
            ErrorManager.getDefault().notify(ex);
            return new String[0];
        }
        return imports.toArray(new String[0]);
        /*
        SourceCookie.Editor sc = (SourceCookie.Editor) dataObject.getCookie
        (SourceCookie.Editor.class);
        if (sc == null) return new String [0];
        Import[] is = sc.getSource ().getImports ();
        int i, k = is.length;
        String[] is2 = new String [k];
        for (i = 0; i < k; i++)
        is2 [i] = is [i].getIdentifier ().getFullName ();
        return is2;
         */
    }

//    private JavaSource getJavaSource(SourcePathProvider sp) {
//        String[] roots = sp.getOriginalSourceRoots();
//        List<FileObject> sourcePathFiles = new ArrayList<FileObject>();
//        for (String root : roots) {
//            FileObject fo = FileUtil.toFileObject(new java.io.File(root));
//            if (fo != null && FileUtil.isArchiveFile(fo)) {
//                fo = FileUtil.getArchiveRoot(fo);
//            }
//            sourcePathFiles.add(fo);
//        }
//        ClassPath bootPath = ClassPathSupport.createClassPath(new FileObject[]{});
//        ClassPath classPath = ClassPathSupport.createClassPath(new FileObject[]{});
//        ClassPath sourcePath = ClassPathSupport.createClassPath(sourcePathFiles.toArray(new FileObject[]{}));
//        return JavaSource.create(ClasspathInfo.create(bootPath, classPath, sourcePath), new FileObject[]{});
//    }
    /**
     * Parse the expression into AST tree and traverse is via the provided visitor.
     *
     * @return the visitor value or <code>null</code>.
     */
    public <R, D> R parseExpression(final String expression, String url, final int line,
            final TreePathScanner<R, D> visitor, final D context,
            final SourcePathProvider sp) {
        DataObject dataObject = getDataObject(url);
        if (dataObject == null) {
            return null;
        }
        FileObject fo = dataObject.getPrimaryFile();
        if (fo == null) {
            return null;
        }
        if (!"text/x-scala".equals(fo.getMIMEType())) {
            return null;
        }
        /** @todo*/
        final R[] retValue = (R[]) new Object[]{null};
        return retValue[0];
//        if (js == null) {
//            js = getJavaSource(sp);
//        }
//        final R[] retValue = (R[]) new Object[]{null                };
//        try {
//            js.runUserActionTask(new Task<CompilationController>() {
//
//                public void run(CompilationController ci) throws Exception {
//                    if (ci.toPhase(Phase.PARSED).compareTo(Phase.PARSED) < 0) {
//                        return;
//                    }
//                    AstScope scope = null;
//                    int offset = 0;
//                    StyledDocument doc = (StyledDocument) ci.getDocument();
//                    if (doc != null) {
//                        offset = findLineOffset(doc, line);
//                        scope = ci.getTreeUtilities().scopeFor(offset);
//                    }
//                    SourcePositions[] sourcePtr = new SourcePositions[]{null                    };
//                    Tree tree = ci.getTreeUtilities().parseExpression(
//                            expression,
//                            sourcePtr);
//                    if (scope != null) {
//                        ci.getTreeUtilities().attributeTree(tree, scope);
//                    }
//                    try {
//                        //context.setTrees(ci.getTrees());
//                        java.lang.reflect.Method setTreesMethod =
//                                context.getClass().getMethod("setTrees", new Class[]{Trees.class                                });
//                        setTreesMethod.invoke(context, ci.getTrees());
//                    } catch (Exception ex) {
//                    }
//                    try {
//                        //context.setCompilationUnit(ci.getCompilationUnit());
//                        java.lang.reflect.Method setCompilationUnitMethod =
//                                context.getClass().getMethod("setCompilationUnit", new Class[]{CompilationUnitTree.class                                });
//                        setCompilationUnitMethod.invoke(context, ci.getCompilationUnit());
//                    } catch (Exception ex) {
//                    }
//                    TreePath treePath = null;
//                    try {
//                        //context.setTrees(ci.getTrees());
//                        java.lang.reflect.Method setTreePathMethod =
//                                context.getClass().getMethod("setTreePath", new Class[]{TreePath.class                                });
//                        if (doc != null) {
//                            treePath = ci.getTreeUtilities().pathFor(offset);
//                            treePath = new TreePath(treePath, tree);
//                            setTreePathMethod.invoke(context, treePath);
//                        }
//                    } catch (Exception ex) {
//                        return;
//                    }
//                    if (treePath != null) {
//                        retValue[0] = visitor.scan(treePath, context);
//                    } else {
//                        retValue[0] = tree.accept(visitor, context);
//                    }
//                }
//            }, true);
//            /*
//            js.runModificationTask(new Task<WorkingCopy>() {
//            public void run(WorkingCopy wc) throws Exception {
//            if (wc.toPhase(Phase.PARSED).compareTo(Phase.PARSED) < 0)
//            return;
//            int offset = findLineOffset((StyledDocument) wc.getDocument(), line);
//            AstScope scope = wc.getTreeUtilities().scopeFor(offset);
//            Element clazz = scope.getEnclosingClass();
//            if (clazz == null) {
//            return ;
//            }
//            ExpressionTree expressionTree = wc.getTreeUtilities().parseExpression(
//            expression,
//            new SourcePositions[] { wc.getTrees().getSourcePositions() }
//            );
//            wc.getTreeUtilities().attributeTree(expressionTree, scope);
//            TreeMaker tm = wc.getTreeMaker();
//            ExpressionStatementTree est = tm.ExpressionStatement(expressionTree);
//            addStatement(tm, wc.getTrees(), wc.getTreeUtilities().pathFor(offset), offset, est);
//            //wc.rewrite(est, est);
//            }
//            });
//             */
//            return retValue[0];
//        } catch (IOException ioex) {
//            ErrorManager.getDefault().notify(ioex);
//            return null;
//        }
    }
    /*
    private static void addStatement(TreeMaker tm, Trees trees, TreePath positionPath, int offset, StatementTree statement) {
    SourcePositions sourcePositions = trees.getSourcePositions();
    CompilationUnitTree root = positionPath.getCompilationUnit();
    Tree newTree = null;
    for (java.util.Iterator<Tree> it = positionPath.iterator(); it.hasNext(); ) {
    Tree element = it.next();
    if (element.getKind() == Tree.Kind.BLOCK && newTree == null) {
    BlockTree block = (BlockTree) element;
    List<? extends StatementTree> statements = block.getStatements();
    int index = 0;
    for (StatementTree st : statements) {
    if (sourcePositions.getStartPosition(root, st) >= offset) {
    break;
    }
    index++;
    }
    newTree = tm.insertBlockStatement(block, index, statement);
    } else {
    
    }
    }
    }
     */

    /**
     * Adds a property change listener.
     *
     * @param l the listener to add
     */
    public void addPropertyChangeListener(PropertyChangeListener l) {
        pcs.addPropertyChangeListener(l);
    }

    /**
     * Removes a property change listener.
     *
     * @param l the listener to remove
     */
    public void removePropertyChangeListener(PropertyChangeListener l) {
        pcs.removePropertyChangeListener(l);
    }

    /**
     * Adds a property change listener.
     *
     * @param propertyName the name of property
     * @param l the listener to add
     */
    public void addPropertyChangeListener(
            String propertyName,
            PropertyChangeListener l) {
        pcs.addPropertyChangeListener(propertyName, l);
    }

    /**
     * Removes a property change listener.
     *
     * @param propertyName the name of property
     * @param l the listener to remove
     */
    public void removePropertyChangeListener(
            String propertyName,
            PropertyChangeListener l) {
        pcs.removePropertyChangeListener(propertyName, l);
    }

    // private helper methods ..................................................
//    public void fileChanged (FileEvent fe) {
//	pcs.firePropertyChange (PROP_LINE_NUMBER, null, null);
//    }
//    
//    public void fileDeleted (FileEvent fe) {}
//    public void fileAttributeChanged (org.openide.filesystems.FileAttributeEvent fe) {}
//    public void fileDataCreated (FileEvent fe) {}
//    public void fileFolderCreated (FileEvent fe) {}
//    public void fileRenamed (org.openide.filesystems.FileRenameEvent fe) {}
    private String getCurrentElement(ElementKind kind) {
        return getCurrentElement(kind, null);
    }

    /** throws IllegalComponentStateException when can not return the data in AWT. */
    private String getCurrentElement(final ElementKind kind, final Element[] elementPtr)
            throws java.awt.IllegalComponentStateException {
        FileObject fo = contextDispatcher.getCurrentFile();
        if (fo == null) {
            return null;
        }
        JEditorPane ep = contextDispatcher.getCurrentEditor();

        JavaSource js = JavaSource.forFileObject(fo);
        if (js == null) {
            return null;
        }
        final int currentOffset;
        final String selectedIdentifier;
        if (ep != null) {
            String s = ep.getSelectedText();
            currentOffset = ep.getCaretPosition();
            if (ep.getSelectionStart() > currentOffset || ep.getSelectionEnd() < currentOffset) {
                s = null; // caret outside of the selection
            }
            if (s != null && Utilities.isJavaIdentifier(s)) {
                selectedIdentifier = s;
            } else {
                selectedIdentifier = null;
            }
        } else {
            selectedIdentifier = null;
            currentOffset = 0;
        }

        final String[] currentElementPtr = new String[]{null};
        final Future<Void> scanFinished;
//        try {
//            scanFinished = js.runWhenScanFinished(new CancellableTask<CompilationController>() {
//
//                public void cancel() {
//                }
//
//                public void run(CompilationController ci) throws Exception {
//                    if (ci.toPhase(Phase.RESOLVED).compareTo(Phase.RESOLVED) < 0) {//TODO: ELEMENTS_RESOLVED may be sufficient
//                        ErrorManager.getDefault().log(ErrorManager.WARNING,
//                                "Unable to resolve " + ci.getFileObject() + " to phase " + Phase.RESOLVED + ", current phase = " + ci.getPhase() +
//                                "\nFree memory = " + Runtime.getRuntime().freeMemory());
//                        return;
//                    }
//                    Element el = null;
//                    if (kind == ElementKind.CLASS) {
//                        boolean isMemberClass = false;
//                        if (selectedIdentifier != null) {
//                            Tree tree = ci.getTreeUtilities().pathFor(currentOffset).getLeaf();
//                            if (tree.getKind() == Tree.Kind.MEMBER_SELECT) {
//                                MemberSelectTree mst = (MemberSelectTree) tree;
//                                el = ci.getTrees().getElement(ci.getTrees().getPath(ci.getCompilationUnit(), mst.getExpression()));
//                                TypeMirror tm = el.asType();
//                                if (tm.getKind().equals(TypeKind.DECLARED)) {
//                                    currentElementPtr[0] = tm.toString();
//                                    isMemberClass = true;
//                                }
//                            }
//                        }
//                        if (!isMemberClass) {
//                            TreePath currentPath = ci.getTreeUtilities().pathFor(currentOffset);
//                            Tree tree = currentPath.getLeaf();
//                            TypeElement te;
//                            if (tree.getKind() == Tree.Kind.CLASS) {
//                                te = (TypeElement) ci.getTrees().getElement(currentPath);
//                            } else {
//                                AstScope scope = ci.getTreeUtilities().scopeFor(currentOffset);
//                                te = scope.getEnclosingClass();
//                            }
//                            if (te != null) {
//                                currentElementPtr[0] = ElementUtilities.getBinaryName(te);
//                            }
//                            el = te;
//                        }
//                    } else if (kind == ElementKind.METHOD) {
//                        AstScope scope = ci.getTreeUtilities().scopeFor(currentOffset);
//                        el = scope.getEnclosingMethod();
//                        if (el != null) {
//                            currentElementPtr[0] = el.getSimpleName().toString();
//                            if (currentElementPtr[0].equals("<init>")) {
//                                // The constructor name is the class name:
//                                currentElementPtr[0] = el.getEnclosingElement().getSimpleName().toString();
//                            }
//                        }
//                    } else if (kind == ElementKind.FIELD) {
//                        int offset = currentOffset;
//
//                        if (selectedIdentifier == null) {
//                            String text = ci.getText();
//                            int l = text.length();
//                            char c = 0; // Search for the end of the field declaration
//                            while (offset < l && (c = text.charAt(offset)) != ';' && c != ',' && c != '\n' && c != '\r') {
//                                offset++;
//                            }
//                            if (offset < l && c == ';' || c == ',') { // we have it, but there might be '=' sign somewhere before
//                                int endOffset = --offset;
//                                int setOffset = -1;
//                                while (offset >= 0 && (c = text.charAt(offset)) != ';' && c != ',' && c != '\n' && c != '\r') {
//                                    if (c == '=') {
//                                        setOffset = offset;
//                                    }
//                                    offset--;
//                                }
//                                if (setOffset > -1) {
//                                    offset = setOffset;
//                                } else {
//                                    offset = endOffset;
//                                }
//                                while (offset >= 0 && Character.isWhitespace(text.charAt(offset))) {
//                                    offset--;
//                                }
//                            }
//                            if (offset < 0) {
//                                offset = 0;
//                            }
//                        }
//                        Tree tree = ci.getTreeUtilities().pathFor(offset).getLeaf();
//                        if (tree.getKind() == Tree.Kind.VARIABLE) {
//                            el = ci.getTrees().getElement(ci.getTrees().getPath(ci.getCompilationUnit(), tree));
//                            if (el.getKind() == ElementKind.FIELD || el.getKind() == ElementKind.ENUM_CONSTANT) {
//                                currentElementPtr[0] = ((VariableTree) tree).getName().toString();
//                            }
//                        } else if (tree.getKind() == Tree.Kind.IDENTIFIER && selectedIdentifier != null) {
//                            IdentifierTree it = (IdentifierTree) tree;
//                            String fieldName = it.getName().toString();
//                            AstScope scope = ci.getTreeUtilities().scopeFor(offset);
//                            TypeElement te = scope.getEnclosingClass();
//                            List<? extends Element> enclosedElms = te.getEnclosedElements();
//                            for (Element elm : enclosedElms) {
//                                if (elm.getKind().equals(ElementKind.FIELD) && elm.getSimpleName().contentEquals(fieldName)) {
//                                    currentElementPtr[0] = fieldName;
//                                    break;
//                                }
//                            }
//
//                        } else if (tree.getKind() == Tree.Kind.MEMBER_SELECT && selectedIdentifier != null) {
//                            MemberSelectTree mst = (MemberSelectTree) tree;
//                            String fieldName = mst.getIdentifier().toString();
//                            el = ci.getTrees().getElement(ci.getTrees().getPath(ci.getCompilationUnit(), mst.getExpression()));
//                            if (el.asType().getKind().equals(TypeKind.DECLARED)) {
//                                List<? extends Element> enclosedElms = ((DeclaredType) el.asType()).asElement().getEnclosedElements();
//                                for (Element elm : enclosedElms) {
//                                    if (elm.getKind().equals(ElementKind.FIELD) && elm.getSimpleName().contentEquals(fieldName)) {
//                                        currentElementPtr[0] = fieldName;
//                                        break;
//                                    }
//                                }
//                            }
//                        }
//                    }
//                    if (elementPtr != null) {
//                        elementPtr[0] = el;
//                    }
//                }
//            }, true);
//            if (!scanFinished.isDone()) {
//                if (java.awt.EventQueue.isDispatchThread()) {
//                    // Hack: We should not wait for the scan in AWT!
//                    //       Thus we throw IllegalComponentStateException,
//                    //       which returns the data upon call to getMessage()
//                    throw new java.awt.IllegalComponentStateException() {
//
//                        private void waitScanFinished() {
//                            try {
//                                scanFinished.get();
//                            } catch (InterruptedException iex) {
//                            } catch (java.util.concurrent.ExecutionException eex) {
//                                ErrorManager.getDefault().notify(eex);
//                            }
//                        }
//
//                        public String getMessage() {
//                            waitScanFinished();
//                            return currentElementPtr[0];
//                        }
//                    };
//                } else {
//                    try {
//                        scanFinished.get();
//                    } catch (InterruptedException iex) {
//                        return null;
//                    } catch (java.util.concurrent.ExecutionException eex) {
//                        ErrorManager.getDefault().notify(eex);
//                        return null;
//                    }
//                }
//            }
//        } catch (IOException ioex) {
//            ErrorManager.getDefault().notify(ioex);
//            return null;
//        }
//        return currentElementPtr[0];
        return null;
    }

    private static DataObject getDataObject(String url) {
        FileObject file;
        try {
            file = URLMapper.findFileObject(new URL(url));
        } catch (MalformedURLException e) {
            return null;
        }

        if (file == null) {
            return null;
        }
        try {
            return DataObject.find(file);
        } catch (DataObjectNotFoundException ex) {
            return null;
        }
    }

    private class EditorContextDispatchListener extends Object implements PropertyChangeListener {

        public void propertyChange(PropertyChangeEvent evt) {
            pcs.firePropertyChange(org.openide.windows.TopComponent.Registry.PROP_CURRENT_NODES, null, null);
        }
    }

    private class OperationCreationDelegateImpl implements AST2Bytecode.OperationCreationDelegate {
        /*
        public Operation createOperation(
        Position startPosition,
        Position endPosition,
        int bytecodeIndex) {
        return EditorContextImpl.this.createOperation(
        startPosition,
        endPosition,
        bytecodeIndex);
        }
         */

        public Operation createMethodOperation(
                Position startPosition,
                Position endPosition,
                Position methodStartPosition,
                Position methodEndPosition,
                String methodName,
                String methodClassType,
                int bytecodeIndex) {
            return EditorContextImpl.this.createMethodOperation(
                    startPosition,
                    endPosition,
                    methodStartPosition,
                    methodEndPosition,
                    methodName,
                    methodClassType,
                    bytecodeIndex);
        }

        public Position createPosition(
                int offset,
                int line,
                int column) {
            return EditorContextImpl.this.createPosition(
                    offset,
                    line,
                    column);
        }

        public void addNextOperationTo(Operation operation, Operation next) {
            EditorContextImpl.this.addNextOperationTo(operation, next);
        }
    }
}
