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
package org.netbeans.modules.scala.platform;

import org.netbeans.api.scala.platform.ScalaPlatform;
import org.openide.filesystems.*;
import org.openide.cookies.InstanceCookie;
import org.openide.ErrorManager;
import org.openide.loaders.DataObject;

import java.util.*;
import java.io.IOException;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeSupport;
import org.netbeans.api.scala.platform.ScalaPlatformManager;
import org.openide.util.Lookup;

@org.openide.util.lookup.ServiceProvider(service=org.netbeans.modules.scala.platform.ScalaPlatformProvider.class)
public class DefaultScalaPlatformProvider implements ScalaPlatformProvider, FileChangeListener {

    private static final String PLATFORM_STORAGE = "Services/Platforms/org-netbeans-api-scala-Platform";  //NOI18N

    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    private FileObject storage;
    private ScalaPlatform defaultPlatform;

    public DefaultScalaPlatformProvider () {
        storage = FileUtil.getConfigFile(PLATFORM_STORAGE);
        if (storage == null) {
            // Turn this off since it can confuse unit tests running w/o layer merging.
            //assert false : "Cannot find platforms storage";
        }
        else {
            storage.addFileChangeListener (this);
        }
    }

    public ScalaPlatform[] getInstalledPlatforms() {
        List<ScalaPlatform> platforms = new ArrayList<ScalaPlatform>();
        if (storage != null) {
            try {
                for (FileObject platformDefinition : storage.getChildren()) {
                    DataObject dobj = DataObject.find(platformDefinition);
                    InstanceCookie ic = dobj.getCookie(InstanceCookie.class);
                    if (ic == null) {
                        ErrorManager.getDefault().log(ErrorManager.WARNING,"DefaultPlatformStorage: The file: "+    //NOI18N
                            platformDefinition.getNameExt() + " has no InstanceCookie");                           //NOI18N
                        continue;
                    }
                    else  if (ic instanceof InstanceCookie.Of) {
                        if (((InstanceCookie.Of)ic).instanceOf(ScalaPlatform.class)) {
                            platforms.add((ScalaPlatform) ic.instanceCreate());
                        }
                        else {
                            ErrorManager.getDefault().log(ErrorManager.WARNING,"DefaultPlatformStorage: The file: "+    //NOI18N
                                platformDefinition.getNameExt() + " is not an instance of JavaPlatform");                  //NOI18N
                        }
                    }
                    else {
                        Object instance = ic.instanceCreate();
                        if (instance instanceof ScalaPlatform) {
                            platforms.add((ScalaPlatform) instance);
                        }
                        else {
                            ErrorManager.getDefault().log(ErrorManager.WARNING,"DefaultPlatformStorage: The file: "+    //NOI18N
                                platformDefinition.getNameExt() + " is not an instance of JavaPlatform");                  //NOI18N
                        }
                    }
                }
            }catch (ClassNotFoundException cnf) {
                ErrorManager.getDefault().notify (cnf);
            }
            catch (IOException ioe) {
                ErrorManager.getDefault().notify (ioe);
            }
        }
        return platforms.toArray(new ScalaPlatform[platforms.size()]);
    }
    
    public ScalaPlatform getDefaultPlatform() {
        if (this.defaultPlatform == null) {
            ScalaPlatform[] allPlatforms = this.getInstalledPlatforms();
            for (int i=0; i< allPlatforms.length; i++) {
                if ("default_platform".equals(allPlatforms[i].getProperties().get(ScalaPlatformManager.SCALA_PLATFORM_PROP_ID))) {  //NOI18N
                    defaultPlatform = allPlatforms[i];
                    break;
                }
            }
        }
        return this.defaultPlatform;
    }
    

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }


    public void fileFolderCreated(FileEvent fe) {
    }

    public void fileDataCreated(FileEvent fe) {
        firePropertyChange ();
    }

    public void fileChanged(FileEvent fe) {
        firePropertyChange ();
    }

    public void fileDeleted(FileEvent fe) {
        firePropertyChange ();
    }

    public void fileRenamed(FileRenameEvent fe) {
        firePropertyChange ();
    }

    public void fileAttributeChanged(FileAttributeEvent fe) {
    }

    private void firePropertyChange () {
        pcs.firePropertyChange(PROP_INSTALLED_PLATFORMS, null, null);
    }       
    
}
