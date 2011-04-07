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

package org.netbeans.modules.languages.execution;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openide.ErrorManager;

/**
 * A RegexpOutputRecognizer is an OutputRecognizer which knows how to recognize
 * filenames and linenumbers based on regular expressions. Patterns are compiled once and then
 * matched for each line.
 *
 * @author Tor Norbye
 */
public class RegexpOutputRecognizer extends OutputRecognizer {

    /** Regexp. for extensions. */
    private static final String EXT_RE = ".*\\.(rb|rake|mab|rjs|rxml|builder)"; // NOI18N
    
    private final Pattern pattern;
    private final int fileGroup;
    private final int lineGroup;
    private final int columnGroup;
    
    private final String extRE;

    public RegexpOutputRecognizer(String regexp) {
        this(regexp, 1, 2, -1, EXT_RE);
    }

    public RegexpOutputRecognizer(String regexp, int fileGroup, int lineGroup, int columnGroup, String extRE) {
        pattern = Pattern.compile(regexp);
        this.fileGroup = fileGroup;
        this.lineGroup = lineGroup;
        this.columnGroup = columnGroup;
        this.extRE = extRE;
    }

    public RegexpOutputRecognizer(String regexp, int fileGroup, int lineGroup, int columnGroup) {
        this(regexp, fileGroup, lineGroup, columnGroup, null);
    }

    @Override
    public FileLocation processLine(String line) {
        // Don't try to match lines that are too long - the java.util.regex library
        // throws stack exceptions (101234)
        if (line.length() > 400) {
            return null;
        }

        Matcher match = pattern.matcher(line);

        if (match.matches()) {
            String file = null;
            int lineno = -1;
            int column = -1;

            if (fileGroup != -1) {
                file = match.group(fileGroup);
                // Make some adjustments - easier to do here than in the regular expression
                // (See 109721 and 109724 for example)
                if (file.startsWith("\"")) { // NOI18N
                    file = file.substring(1);
                }
                if (file.startsWith("./")) { // NOI18N
                    file = file.substring(2);
                }
                if (extRE != null && !(file.matches(extRE) || new File(file).isFile())) {
                    return null;
                }
            }

            if (lineGroup != -1) {
                String linenoStr = match.group(lineGroup);

                try {
                    lineno = Integer.parseInt(linenoStr);
                } catch (NumberFormatException nfe) {
                    ErrorManager.getDefault().notify(nfe);
                    lineno = 0;
                }
            }

            if (columnGroup != -1) {
                String columnStr = match.group(columnGroup);

                try {
                    column = Integer.parseInt(columnStr);
                } catch (NumberFormatException nfe) {
                    ErrorManager.getDefault().notify(nfe);
                    column = 0;
                }
            }

            return new FileLocation(file, lineno, column);
        }

        return null;
    }

    public Pattern getPattern() {
        return pattern;
    }
}
