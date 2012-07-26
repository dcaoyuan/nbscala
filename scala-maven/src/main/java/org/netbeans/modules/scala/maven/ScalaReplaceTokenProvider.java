/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2011 Sun Microsystems, Inc.
 */

package org.netbeans.modules.scala.maven;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.modules.maven.spi.actions.ActionConvertor;
import org.netbeans.modules.maven.spi.actions.ReplaceTokenProvider;
import org.netbeans.spi.project.ActionProvider;
import org.netbeans.spi.project.ProjectServiceProvider;
import org.netbeans.spi.project.SingleMethod;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Lookup;

/**
 * @see org.netbeans.modules.maven.execute.DefaultReplaceTokenProvider
 */
@ProjectServiceProvider(service={ReplaceTokenProvider.class, ActionConvertor.class}, projectType="org-netbeans-modules-maven")
public class ScalaReplaceTokenProvider implements ReplaceTokenProvider, ActionConvertor {

    private static final String CLASSNAME = "className";
    private static final String CLASSNAME_EXT = "classNameWithExtension";
    private static final String PACK_CLASSNAME = "packageClassName";
    private static final String CLASSPATHSCOPE = "classPathScope";

    private final Project project;

    public ScalaReplaceTokenProvider(Project project) {
        this.project = project;
    }

    @Override public Map<String,String> createReplacements(String action, Lookup lookup) {
        FileObject f = lookup.lookup(FileObject.class);
        if (f == null) {
            SingleMethod m = lookup.lookup(SingleMethod.class);
            if (m != null) {
                f = m.getFile();
            }
        }
        if (f != null && "text/x-scala".equals(f.getMIMEType())) {
            for (SourceGroup g : ProjectUtils.getSources(project).getSourceGroups(ScalaSourcesImpl.TYPE_SCALA)) {
                String relPath = FileUtil.getRelativePath(g.getRootFolder(), f);
                if (relPath != null) {
                    Map<String,String> replaceMap = new HashMap<String,String>();
                    replaceMap.put(CLASSNAME_EXT, f.getNameExt());
                    replaceMap.put(CLASSNAME, f.getName());
                    String pack = FileUtil.getRelativePath(g.getRootFolder(), f.getParent());
                    if (pack != null) { //#141175
                        replaceMap.put(PACK_CLASSNAME, (pack + (pack.length() > 0 ? "." : "") + f.getName()).replace('/', '.'));
                    } else {
                        replaceMap.put(PACK_CLASSNAME, f.getName());
                    }
                    replaceMap.put(CLASSPATHSCOPE, g.getName().equals(ScalaSourcesImpl.NAME_SCALATESTSOURCE) ? "test" : "runtime");
                    return replaceMap;
                }
            }
        }
        return Collections.emptyMap();
    }

    @Override public String convert(String action, Lookup lookup) {
        if (ActionProvider.COMMAND_RUN_SINGLE.equals(action) ||
            ActionProvider.COMMAND_DEBUG_SINGLE.equals(action)) {
            FileObject f = lookup.lookup(FileObject.class);
            if (f != null && "text/x-scala".equals(f.getMIMEType())) {
                //TODO this only applies to scala files with main() method.
                return action + ".main";
            }
        }
        return null;
    }

}
