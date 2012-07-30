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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.netbeans.api.java.project.JavaProjectConstants;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.api.project.Sources;
import org.netbeans.modules.maven.spi.nodes.AbstractMavenNodeList;
import org.netbeans.spi.java.project.support.ui.PackageView;
import org.netbeans.spi.project.ui.support.NodeFactory;
import org.netbeans.spi.project.ui.support.NodeList;
import org.openide.filesystems.FileObject;
import org.openide.nodes.Node;
import org.openide.util.RequestProcessor;

/**
 *
 * @author mkleint
 */
@NodeFactory.Registration(projectType="org-netbeans-modules-maven",position=139)
public class ScalaSourcesNodeFactory implements NodeFactory {

    private static final RequestProcessor RP = new RequestProcessor(ScalaSourcesNodeFactory.class);
    
    @Override
    public NodeList<?> createNodes(Project project) {
        return  new NList(project);
    }
    
    private static class NList extends AbstractMavenNodeList<SourceGroup> implements ChangeListener {
        private Project project;
        private NList(Project prj) {
            project = prj;
        }
        
        @Override
        public List<SourceGroup> keys() {
            //#169192 check roots against java roots and if the same don't show twice.
            Set<FileObject> javaroots = new HashSet<FileObject>();
            SourceGroup[] javasg = ProjectUtils.getSources(project).getSourceGroups(JavaProjectConstants.SOURCES_TYPE_JAVA);
            for (SourceGroup sg : javasg) {
                javaroots.add(sg.getRootFolder());
            }

            List<SourceGroup> list = new ArrayList<SourceGroup>();
            Sources srcs = ProjectUtils.getSources(project);
            SourceGroup[] groovygroup = srcs.getSourceGroups(ScalaSourcesImpl.TYPE_SCALA);
            for (int i = 0; i < groovygroup.length; i++) {
                if (!javaroots.contains(groovygroup[i].getRootFolder())) {
                    list.add(groovygroup[i]);
                }
            }
            return list;
        }
        
        @Override
        public Node node(SourceGroup group) {
            return PackageView.createPackageView(group);
        }
        
        @Override
        public void addNotify() {
            Sources srcs = ProjectUtils.getSources(project);
            srcs.addChangeListener(this);
        }
        
        @Override
        public void removeNotify() {
            Sources srcs = ProjectUtils.getSources(project);
            srcs.removeChangeListener(this);
        }

        @Override
        public void stateChanged(ChangeEvent arg0) {
            //#167372 break the stack trace chain to prevent deadlocks.
            RP.post(new Runnable() {
                @Override
                public void run() {
                    fireChange();
                }
            });
        }
    }
}
