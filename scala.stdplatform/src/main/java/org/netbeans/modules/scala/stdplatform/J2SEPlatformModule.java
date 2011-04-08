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
package org.netbeans.modules.scala.stdplatform;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openide.cookies.InstanceCookie;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.modules.ModuleInstall;
import org.openide.util.Exceptions;
import org.netbeans.api.scala.platform.ScalaPlatform;
import org.netbeans.api.scala.platform.ScalaPlatformManager;
import org.netbeans.api.scala.platform.Specification;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.spi.project.support.ant.EditableProperties;
import org.netbeans.spi.project.support.ant.PropertyUtils;
import org.netbeans.modules.scala.stdplatform.platformdefinition.PlatformConvertor;
import org.netbeans.modules.scala.stdplatform.platformdefinition.J2SEPlatformImpl;
import org.openide.filesystems.FileUtil;


public class J2SEPlatformModule extends ModuleInstall {
    
    private static final String DEFAULT_PLATFORM = "Services/Platforms/org-netbeans-api-scala-Platform/default_platform.xml";    //NOI18N

    public void restored() {
        super.restored();
        // update source level and J2SE platforms in build.properties file
        updateBuildProperties();
    }

    // implemented as separate method for simpler unit testing
    public static void updateBuildProperties() {
        ProjectManager.mutex().postWriteRequest(
            new Runnable () {
                public void run () {
                    try {
                        recoverDefaultPlatform ();
                        EditableProperties ep = PropertyUtils.getGlobalProperties();
                        boolean save = updateSourceLevel(ep);
                        save |= updateBuildProperties (ep);
                        if (save) {
                            PropertyUtils.putGlobalProperties (ep);
                        }
                    } catch (IOException ioe) {
                        Exceptions.printStackTrace(ioe);
                    }
                }
            });
    }
    
    private static boolean updateSourceLevel(EditableProperties ep) {
        ScalaPlatform platform = ScalaPlatformManager.getDefault().getDefaultPlatform();
        String ver = platform.getSpecification().getVersion().toString();
        if (!ver.equals(ep.getProperty("default.javac.source"))) { //NOI18N
            ep.setProperty("default.javac.source", ver); //NOI18N
            ep.setProperty("default.javac.target", ver); //NOI18N
            return true;
        } else {
            return false;
        }
    }


    private static boolean updateBuildProperties (EditableProperties ep) {
        boolean changed = false;
        ScalaPlatform[] installedPlatforms = ScalaPlatformManager.getDefault().getPlatforms(null, new Specification ("Std",null));   //NOI18N
        for (int i=0; i<installedPlatforms.length; i++) {
            //Handle only platforms created by this module
            if (!installedPlatforms[i].equals (ScalaPlatformManager.getDefault().getDefaultPlatform()) && installedPlatforms[i] instanceof J2SEPlatformImpl) {
                String systemName = ((J2SEPlatformImpl)installedPlatforms[i]).getAntName();
                String key = PlatformConvertor.createName(systemName,"home");   //NOI18N
                if (!ep.containsKey (key)) {
                    try {
                        PlatformConvertor.generatePlatformProperties(installedPlatforms[i], systemName, ep);
                        changed = true;
                    } catch (PlatformConvertor.BrokenPlatformException b) {
                        Logger.getLogger(J2SEPlatformModule.class.getName()).info("Platform: " + installedPlatforms[i].getDisplayName() +" is missing: " + b.getMissingTool());
                    } catch (IOException ioe) {
                        Exceptions.printStackTrace(ioe);
                  }
                }
            }
        }
        return changed;
    }

    private static void recoverDefaultPlatform () {
        final FileObject defaultPlatform = FileUtil.getConfigFile(DEFAULT_PLATFORM);
        if (defaultPlatform != null) {
            try {
                DataObject dobj = DataObject.find(defaultPlatform);
                boolean valid = false;
                InstanceCookie ic = (InstanceCookie) dobj.getCookie(InstanceCookie.class);
                if (ic != null) {
                    try {
                        ic.instanceCreate();
                        valid = true;
                    } catch (Exception e) {
                        //Ignore it, logged bellow
                    }
                }
                if (!valid) {
                    Logger.getLogger("global").log(Level.WARNING,"default_platform.xml is broken, regenerating.");
                    Object attr = defaultPlatform.getAttribute("removeWritables");      //NOI18N
                    if (attr instanceof Callable) {
                        ((Callable)attr).call ();
                    }
                }
            } catch (Exception e) {
                Exceptions.printStackTrace(e);
            }
        }
        else {
            Logger.getLogger("global").log(Level.WARNING,"The default platform is hidden.");  //NOI18N
        }
    }
}
