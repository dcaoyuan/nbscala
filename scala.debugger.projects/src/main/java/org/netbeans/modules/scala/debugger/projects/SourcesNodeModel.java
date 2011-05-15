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

import java.io.File;

import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectInformation;

import org.netbeans.spi.viewmodel.NodeModel;
import org.netbeans.spi.viewmodel.TreeModel;
import org.netbeans.spi.viewmodel.ModelListener;
import org.netbeans.spi.viewmodel.UnknownTypeException;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.NbBundle;

/**
 * @author   Jan Jancura
 */
public class SourcesNodeModel implements NodeModel {

    public static final String SOURCE_ROOT =
        "org/netbeans/modules/debugger/jpda/resources/root";
    public static final String FILTER =
        "org/netbeans/modules/debugger/jpda/resources/Filter";
    
    
    public String getDisplayName (Object o) throws UnknownTypeException {
        if (o == TreeModel.ROOT) {
            return NbBundle.getBundle(SourcesNodeModel.class).getString("CTL_SourcesModel_Column_Name_Name");
        } else
        if (o instanceof String) {
            File f = new File ((String) o);
            if (f.exists ()) {
                FileObject fo = FileUtil.toFileObject (f);
                Project p = FileOwnerQuery.getOwner (fo);
                if (p != null) {
                    ProjectInformation pi = (ProjectInformation) p.getLookup ().
                        lookup (ProjectInformation.class);
                    return java.text.MessageFormat.format(NbBundle.getBundle(SourcesNodeModel.class).getString(
                            "CTL_SourcesModel_Column_Name_ProjectSources"), new Object [] { f.getPath(), pi.getDisplayName() });
                }
                return java.text.MessageFormat.format(NbBundle.getBundle(SourcesNodeModel.class).getString(
                        "CTL_SourcesModel_Column_Name_LibrarySources"), new Object [] { f.getPath() });
            } else
            return (String) o;
        } else
        throw new UnknownTypeException (o);
    }
    
    public String getShortDescription (Object o) throws UnknownTypeException {
        if (o == TreeModel.ROOT)
            return NbBundle.getBundle(SourcesNodeModel.class).getString("CTL_SourcesModel_Column_Name_Desc");
        if (o instanceof String) {
            if (((String) o).startsWith ("D"))
                return NbBundle.getBundle(SourcesNodeModel.class).getString("CTL_SourcesModel_Column_Name_DescExclusion");
            else
                return NbBundle.getBundle(SourcesNodeModel.class).getString("CTL_SourcesModel_Column_Name_DescRoot");
        } else
        throw new UnknownTypeException (o);
    }
    
    public String getIconBase (Object o) throws UnknownTypeException {
        if (o instanceof String) {
            if (((String) o).startsWith ("D"))
                return FILTER;
            else
                return SOURCE_ROOT;
        } else
        throw new UnknownTypeException (o);
    }

    public void addModelListener (ModelListener l) {
    }

    public void removeModelListener (ModelListener l) {
    }
}
