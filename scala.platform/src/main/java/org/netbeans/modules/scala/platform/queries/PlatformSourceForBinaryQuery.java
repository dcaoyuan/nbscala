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
package org.netbeans.modules.scala.platform.queries;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.Map;
import java.util.HashMap;
import javax.swing.event.ChangeListener;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.queries.SourceForBinaryQuery;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.filesystems.URLMapper;
import org.netbeans.api.scala.platform.ScalaPlatformManager;
import org.netbeans.api.scala.platform.ScalaPlatform;
import org.netbeans.spi.java.queries.SourceForBinaryQueryImplementation;
import org.openide.util.ChangeSupport;
import org.openide.util.Exceptions;
import org.openide.util.WeakListeners;


/**
 * This implementation of the SourceForBinaryQueryImplementation
 * provides sources for the active platform and project libraries
 */

@org.openide.util.lookup.ServiceProvider(service=org.netbeans.spi.java.queries.SourceForBinaryQueryImplementation.class, position=150)
public class PlatformSourceForBinaryQuery implements SourceForBinaryQueryImplementation {
    
    private static final String JAR_FILE = "jar:file:";                 //NOI18N
    private static final String RTJAR_PATH = "/jre/lib/rt.jar!/";       //NOI18N
    private static final String SRC_ZIP = "/src.zip";                    //NOI18N

    private Map<URL,SourceForBinaryQuery.Result> cache = new HashMap<URL,SourceForBinaryQuery.Result>();

    public PlatformSourceForBinaryQuery () {
    }

    /**
     * Tries to locate the source root for given classpath root.
     * @param binaryRoot the URL of a classpath root (platform supports file and jar protocol)
     * @return FileObject[], never returns null
     */
    public SourceForBinaryQuery.Result findSourceRoots2(URL binaryRoot) {
        SourceForBinaryQuery.Result res = this.cache.get (binaryRoot);
        if (res != null) {
            return res;
        }
        ScalaPlatformManager mgr = ScalaPlatformManager.getDefault();
        for (ScalaPlatform platform : mgr.getInstalledPlatforms()) {
            for (ClassPath.Entry entry : platform.getBootstrapLibraries().entries()) {
                if (entry.getURL().equals (binaryRoot)) {
                    res = new Result(platform);
                    this.cache.put (binaryRoot, res);
                    return res;
                }
            }
            
            for (ClassPath.Entry entry : platform.getStandardLibraries().entries()) {
                if (entry.getURL().equals (binaryRoot)) {
                    res = new Result(platform);
                    this.cache.put (binaryRoot, res);
                    return res;
                }                
            }
        }
        
        String binaryRootS = binaryRoot.toExternalForm();
        if (binaryRootS.startsWith(JAR_FILE)) {
            String srcZipS = null;
            if (binaryRootS.endsWith(RTJAR_PATH)) {
                //Unregistered platform
                srcZipS = binaryRootS.substring(4,binaryRootS.length() - RTJAR_PATH.length()) + SRC_ZIP;
                try {
                    URL srcZip = FileUtil.getArchiveRoot(new URL(srcZipS));
                    FileObject fo = URLMapper.findFileObject(srcZip);
                    if (fo != null) {
                        return new UnregisteredPlatformResult (fo);
                    }
                } catch (MalformedURLException mue) {
                    Exceptions.printStackTrace(mue);
                }
            }
        }
        return null;
    }
    
    public SourceForBinaryQuery.Result findSourceRoots (URL binaryRoot) {
        return this.findSourceRoots2(binaryRoot);
    }
    
    private static class Result implements SourceForBinaryQuery.Result, PropertyChangeListener {
                        
        private ScalaPlatform platform;
        private final ChangeSupport cs = new ChangeSupport(this);
                        
        public Result (ScalaPlatform platform) {
            this.platform = platform;
            this.platform.addPropertyChangeListener(WeakListeners.create(PropertyChangeListener.class, this, platform));
        }
                        
        public FileObject[] getRoots () {       //No need for caching, platforms does.
            ClassPath sources = this.platform.getSourceFolders();
            return sources.getRoots();
        }
                        
        public void addChangeListener (ChangeListener l) {
            assert l != null : "Listener can not be null";  //NOI18N
            cs.addChangeListener(l);
        }
                        
        public void removeChangeListener (ChangeListener l) {
            assert l != null : "Listener can not be null";  //NOI18N
            cs.removeChangeListener(l);
        }
        
        public void propertyChange (PropertyChangeEvent event) {
            if (ScalaPlatform.PROP_SOURCE_FOLDER.equals(event.getPropertyName())) {
                cs.fireChange();
            }
        }

        public boolean preferSources() {
            return false;
        }
        
    }
    
    private static class UnregisteredPlatformResult implements SourceForBinaryQuery.Result {
        
        private FileObject srcRoot;
        
        private UnregisteredPlatformResult (FileObject fo) {
            assert fo != null;
            srcRoot = fo;
        }
    
        public FileObject[] getRoots() {            
            return srcRoot.isValid() ? new FileObject[] {srcRoot} : new FileObject[0];
        }
        
        public void addChangeListener(ChangeListener l) {
            //Not supported, no listening.
        }
        
        public void removeChangeListener(ChangeListener l) {
            //Not supported, no listening.
        }

        public boolean preferSources() {
            return false;
        }
    }}

