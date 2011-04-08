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

import java.io.IOException;
import java.lang.ref.*;
import java.util.*;
import org.netbeans.spi.scala.platform.CustomPlatformInstall;
import org.netbeans.spi.scala.platform.GeneralPlatformInstall;

import org.openide.cookies.InstanceCookie;
import org.openide.filesystems.*;
import org.openide.loaders.*;
import org.netbeans.spi.scala.platform.PlatformInstall;
import org.openide.util.NbCollections;

/**
 * Simple helper class, which keeps track of registered PlatformInstallers.
 * It caches its [singleton] instance for a while.
 *
 * @author Svata Dedic
 */
public class InstallerRegistry {
    static final String INSTALLER_REGISTRY_FOLDER = "org-netbeans-api-scala/platform/installers"; // NOI18N
    
    static Reference<InstallerRegistry> defaultInstance = new WeakReference<InstallerRegistry>(null);
    
    private Provider provider;
    private List<GeneralPlatformInstall> platformInstalls;      //Used by unit test
    
    InstallerRegistry(FileObject registryResource) {
        assert registryResource != null;
        this.provider = new Provider (registryResource);
    }
    
    /**
     * Used only by unit tests
     */
    InstallerRegistry (GeneralPlatformInstall[] platformInstalls) {
        assert platformInstalls != null;
        this.platformInstalls = Arrays.asList(platformInstalls);
    }
    
    /**
     * Returns all registered Java platform installers, in the order as
     * they are specified by the module layer(s).
     */
    public List<PlatformInstall> getInstallers () {
        return filter(getAllInstallers(),PlatformInstall.class);
    }
    
    public List<CustomPlatformInstall> getCustomInstallers () {
        return filter(getAllInstallers(),CustomPlatformInstall.class);
    }
    
    public List<GeneralPlatformInstall> getAllInstallers () {
        if (this.platformInstalls != null) {
            //In the unit test
            return platformInstalls;
        }
        else {
            List<GeneralPlatformInstall> list = Collections.emptyList();
            try {
                assert this.provider != null;
                list = NbCollections.checkedListByCopy((List) provider.instanceCreate(), GeneralPlatformInstall.class, true);
            } catch (IOException ex) {
            } catch (ClassNotFoundException ex) {
            }
            return list;
        }
    }
    
    

    /**
     * Creates/acquires an instance of InstallerRegistry
     */
    public static InstallerRegistry getDefault() {
        InstallerRegistry regs = defaultInstance.get();
        if (regs != null)
            return regs;
        regs = new InstallerRegistry(FileUtil.getConfigFile(
            INSTALLER_REGISTRY_FOLDER));
        defaultInstance = new WeakReference<InstallerRegistry>(regs);
        return regs;
    }
    
    
    /**
     * Used only by Unit tests.
     * Sets the {@link InstallerRegistry#defaultInstance} to the new InstallerRegistry instance which 
     * always returns the given GeneralPlatformInstalls
     * @return an instance of InstallerRegistry which has to be hold by strong reference during the test
     */
    static InstallerRegistry prepareForUnitTest (GeneralPlatformInstall[] platformInstalls) {
        InstallerRegistry regs = new InstallerRegistry (platformInstalls);
        defaultInstance = new WeakReference<InstallerRegistry>(regs);
        return regs;
    }
        
    
    private static <T> List<T> filter(List<?> list, Class<T> clazz) {
        List<T> result = new ArrayList<T>(list.size());
        for (Object item : list) {
            if (clazz.isInstance(item)) {
                result.add(clazz.cast(item));
            }
        }
        return result;
    }
    
    private static class Provider extends FolderInstance {
        
        Provider (FileObject registryResource) {            
            super(DataFolder.findFolder(registryResource));
        }
        
        
        protected Object createInstance(InstanceCookie[] cookies) throws java.io.IOException, ClassNotFoundException {
            List<Object> installers = new ArrayList<Object>(cookies.length);
            for (int i = 0; i < cookies.length; i++) {
                InstanceCookie cake = cookies[i];
                Object o = null;
                try {
                    if (cake instanceof InstanceCookie.Of &&
                        !((((InstanceCookie.Of)cake).instanceOf(PlatformInstall.class))  ||
                        (((InstanceCookie.Of)cake).instanceOf(CustomPlatformInstall.class))))
                        continue;
                    o = cake.instanceCreate();
                } catch (IOException ex) {
                } catch (ClassNotFoundException ex) {
                }
                if (o != null)
                    installers.add(o);
            }
            return installers;
        }        
    }
}
