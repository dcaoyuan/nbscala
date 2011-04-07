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
package org.netbeans.modules.languages.execution;

/**
 * An OutputRecognizer is handed one line at a time, and can examine it
 * to discover file or line number information, and produce a {@link RecognizedOutput}
 * object if it recognizes the output. This will usually be a {@link FileLocation}.
 * This can later be resolved into an actual file position by a FileLocator when the user clicks on
 * a link.
 * 
 * @author Tor Norbye
 */
public class OutputRecognizer {
    
    /**
     * Called before output processing is started.
     * Perform initialization of per-run state here, since
     * an output recognizer can be reused multiple times
     * (e.g. typically when the user chooses the Restart
     * button in the Output window.
     */
    public void start() {
    }
    
    /**
     * Called after output processing is done.
     */
    public void finish() {
    }

    /** Called by the IDE for each line in the output */
    public RecognizedOutput processLine(String line) {
        return null;
    }
    
    public interface RecognizedOutput {
        
    }

    public static final class FileLocation implements RecognizedOutput {
        public final String file;
        public final int line;
        public final int column;

        public FileLocation(String file, int line, int column) {
            this.file = file;
            this.line = line;
            this.column = column;
        }
    }

    /**
     * Class which lets output recognizers replace a line of text with
     * multiple lines of text, some plain (typically the original text)
     * as well as additional output that is hyperlinked. Clicking the
     * hyperlink will run the associated action.
     */
    public static final class ActionText implements RecognizedOutput {
        private String[] plainLinesPost;
        private String[] actionLines;
        private Runnable[] actions;
        private String[] plainLinesPre;
        
        public ActionText(String[] plainLinesPre, String[] actionLines, Runnable[] actions, String[] plainLinesPost) {
            this.plainLinesPre = plainLinesPre;
            this.actionLines = actionLines;
            this.actions = actions;
            this.plainLinesPost = plainLinesPost;
            assert actions == null || actions.length == actionLines.length;
        }

        public Runnable[] getActions() {
            return actions;
        }

        public String[] getActionLines() {
            return actionLines;
        }

        public String[] getPlainLinesPost() {
            return plainLinesPost;
        }

        public String[] getPlainLinesPre() {
            return plainLinesPre;
        }
    }
}
