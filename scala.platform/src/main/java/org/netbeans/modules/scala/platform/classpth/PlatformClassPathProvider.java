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

package org.netbeans.modules.scala.platform.classpth;


import java.util.Collections;
import org.netbeans.api.java.classpath.ClassPath;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.netbeans.api.scala.platform.ScalaPlatform;
import org.netbeans.api.scala.platform.ScalaPlatformManager;
import org.netbeans.spi.java.classpath.ClassPathProvider;
import org.netbeans.spi.java.classpath.PathResourceImplementation;
import org.netbeans.spi.java.classpath.support.ClassPathSupport;


@org.openide.util.lookup.ServiceProvider(service=org.netbeans.spi.java.classpath.ClassPathProvider.class, position=150)
public class PlatformClassPathProvider implements ClassPathProvider {



    /** Creates a new instance of PlatformClassPathProvider */
    public PlatformClassPathProvider() {
    }
    
    
    public ClassPath findClassPath(FileObject fo, String type) {
        if (fo == null || type == null) {
            throw new IllegalArgumentException();
        }
        ScalaPlatform lp = this.getLastUsedPlatform(fo);
        ScalaPlatform[] platforms;
        if (lp != null) {
            platforms = new ScalaPlatform[] {lp};
        }
        else {
            ScalaPlatformManager manager = ScalaPlatformManager.getDefault();
            platforms = manager.getInstalledPlatforms();
        }
        for (int i=0; i<platforms.length; i++) {
            ClassPath bootClassPath = platforms[i].getBootstrapLibraries();
            ClassPath libraryPath = platforms[i].getStandardLibraries();
            ClassPath sourcePath = platforms[i].getSourceFolders();
            FileObject root = null;
            if (ClassPath.SOURCE.equals(type) && sourcePath != null &&
                (root = sourcePath.findOwnerRoot(fo))!=null) {
                this.setLastUsedPlatform (root,platforms[i]);
                return sourcePath;
            }
            else if (ClassPath.BOOT.equals(type) &&
                    ((bootClassPath != null && (root = bootClassPath.findOwnerRoot (fo))!=null) ||
                    (sourcePath != null && (root = sourcePath.findOwnerRoot(fo)) != null) ||
                    (libraryPath != null && (root = libraryPath.findOwnerRoot(fo))!=null))) {
                this.setLastUsedPlatform (root,platforms[i]);
                return bootClassPath;
            }
            else if (ClassPath.COMPILE.equals(type)) {
                if (libraryPath != null && (root = libraryPath.findOwnerRoot(fo))!=null) {
                    this.setLastUsedPlatform (root,platforms[i]);
                    return libraryPath;
                }
                else if ((bootClassPath != null && (root = bootClassPath.findOwnerRoot (fo))!=null) ||
                    (sourcePath != null && (root = sourcePath.findOwnerRoot(fo)) != null)) {
                    return this.getEmptyClassPath ();
                }
            }
        }
        return null;
    }

    private synchronized ClassPath getEmptyClassPath () {
        if (this.emptyCp == null ) {
            this.emptyCp = ClassPathSupport.createClassPath(Collections.<PathResourceImplementation>emptyList());
        }
        return this.emptyCp;
    }

    private synchronized void setLastUsedPlatform (FileObject root, ScalaPlatform platform) {
        this.lastUsedRoot = root;
        this.lastUsedPlatform = platform;
    }

    private synchronized ScalaPlatform getLastUsedPlatform (FileObject file) {
        if (this.lastUsedRoot != null && FileUtil.isParentOf(this.lastUsedRoot,file)) {
            return lastUsedPlatform;
        }
        else {
            return null;
        }
    }

    private FileObject lastUsedRoot;
    private ScalaPlatform lastUsedPlatform;
    private ClassPath emptyCp;
}
