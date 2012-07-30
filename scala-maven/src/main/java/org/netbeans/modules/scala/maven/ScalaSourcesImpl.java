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

import java.util.ArrayList;
import java.util.List;
import javax.swing.event.ChangeListener;
import org.netbeans.api.java.project.JavaProjectConstants;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.api.project.Sources;
import static org.netbeans.modules.scala.maven.Bundle.*;
import org.netbeans.spi.project.ProjectServiceProvider;
import org.netbeans.spi.project.SourceGroupModifierImplementation;
import org.netbeans.spi.project.support.GenericSources;
import org.openide.filesystems.FileObject;
import org.openide.util.NbBundle;

@ProjectServiceProvider(service={Sources.class, SourceGroupModifierImplementation.class}, projectType="org-netbeans-modules-maven")
public class ScalaSourcesImpl implements Sources, SourceGroupModifierImplementation {

    public static final String TYPE_SCALA = "scala";
    public static final String NAME_SCALASOURCE = "91ScalaSourceRoot";
    public static final String NAME_SCALATESTSOURCE = "92ScalaTestSourceRoot";

    private final Project project;

    public ScalaSourcesImpl(Project project) {
        this.project = project;
    }

    @Override public SourceGroup[] getSourceGroups(String type) {
        if (TYPE_SCALA.equals(type)) {
            List<SourceGroup> groups = new ArrayList<SourceGroup>();
            maybeAddGroup(groups, false);
            maybeAddGroup(groups, true);
            return groups.toArray(new SourceGroup[groups.size()]);
        } else {
            return new SourceGroup[0];
        }
    }

    @NbBundle.Messages({
        "SG_ScalaSources=Scala Packages",
        "SG_Test_ScalaSources=Scala Test Packages"
    })
    private void maybeAddGroup(List<SourceGroup> groups, boolean test) {
        //XXX we should consult the maven project configuration as well
        FileObject root = project.getProjectDirectory().getFileObject("src/" + (test ? "test" : "main") + "/scala");
        if (root != null) {
            groups.add(GenericSources.group(project, root, test ? NAME_SCALATESTSOURCE : NAME_SCALASOURCE, test ? SG_Test_ScalaSources() : SG_ScalaSources(), null, null));
        }
    }

    @Override public void addChangeListener(ChangeListener listener) {
        // XXX listen to creation/deletion of roots
    }

    @Override public void removeChangeListener(ChangeListener listener) {}

    @Override public SourceGroup createSourceGroup(String type, String hint) {
        // XXX this looks weird, cannot tell where something is created..
        if (!canCreateSourceGroup(type, hint)) {
            return null;
        }
        List<SourceGroup> groups = new ArrayList<SourceGroup>();
        maybeAddGroup(groups, JavaProjectConstants.SOURCES_HINT_TEST.equals(hint));
        return groups.isEmpty() ? null : groups.get(0);
    }

    @Override public boolean canCreateSourceGroup(String type, String hint) {
        return TYPE_SCALA.equals(type) && (JavaProjectConstants.SOURCES_HINT_MAIN.equals(hint) || JavaProjectConstants.SOURCES_HINT_TEST.equals(hint));
    }

}
