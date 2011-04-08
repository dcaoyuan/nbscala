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

package org.netbeans.modules.scala.stdplatform.libraries;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.swing.event.ChangeListener;
import org.netbeans.api.java.queries.SourceForBinaryQuery;
import org.netbeans.api.project.libraries.Library;
import org.netbeans.api.project.libraries.LibraryManager;
import org.netbeans.spi.java.queries.SourceForBinaryQueryImplementation;
import org.openide.ErrorManager;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileStateInvalidException;
import org.openide.filesystems.FileUtil;
import org.openide.filesystems.URLMapper;
import org.openide.util.ChangeSupport;
import org.openide.util.WeakListeners;

/**
 * Finds the locations of sources for various libraries.
 * @author Tomas Zezula
 */
public class J2SELibrarySourceForBinaryQuery implements SourceForBinaryQueryImplementation {

    private final Map<URL,SourceForBinaryQuery.Result> cache = new ConcurrentHashMap<URL,SourceForBinaryQuery.Result>();
    private final Map<URL,URL> normalizedURLCache = new ConcurrentHashMap<URL,URL>();

    /** Default constructor for lookup. */
    public J2SELibrarySourceForBinaryQuery() {}

    public SourceForBinaryQuery.Result findSourceRoots2 (URL binaryRoot) {
        SourceForBinaryQuery.Result res = cache.get(binaryRoot);
        if (res != null) {
            return res;
        }
        boolean isNormalizedURL = isNormalizedURL(binaryRoot);
        for (LibraryManager mgr : LibraryManager.getOpenManagers()) {
            for (Library lib : mgr.getLibraries()) {
                if (lib.getType().equals(J2SELibraryTypeProvider.LIBRARY_TYPE)) {
                    for (URL entry : lib.getContent(J2SELibraryTypeProvider.VOLUME_TYPE_CLASSPATH)) {
                        URL normalizedEntry = entry;
                        if (isNormalizedURL) {
                            normalizedEntry = getNormalizedURL(normalizedEntry);
                        }
                        if (binaryRoot.equals(normalizedEntry)) {
                            res = new Result(entry, lib);
                            cache.put(binaryRoot, res);
                            return res;
                        }
                    }
                }
            }
        }
        return null;
    }
    
    
    public SourceForBinaryQuery.Result findSourceRoots (final URL binaryRoot) {
        return this.findSourceRoots2(binaryRoot);
    }
    
    private URL getNormalizedURL (URL url) {
        //URL is already nornalized, return it
        if (isNormalizedURL(url)) {
            return url;
        }
        //Todo: Should listen on the LibrariesManager and cleanup cache        
        // in this case the search can use the cache onle and can be faster 
        // from O(n) to O(ln(n))
        URL normalizedURL = normalizedURLCache.get(url);
        if (normalizedURL == null) {
            FileObject fo = URLMapper.findFileObject(url);
            if (fo != null) {
                try {
                    normalizedURL = fo.getURL();
                    this.normalizedURLCache.put (url, normalizedURL);
                } catch (FileStateInvalidException e) {
                    ErrorManager.getDefault().notify(e);
                }
            }
        }
        return normalizedURL;
    }
    
    /**
     * Returns true if the given URL is file based, it is already
     * resolved either into file URL or jar URL with file path.
     * @param URL url
     * @return true if  the URL is normal
     */
    private static boolean isNormalizedURL (URL url) {
        if ("jar".equals(url.getProtocol())) { //NOI18N
            url = FileUtil.getArchiveFile(url);
        }
        return "file".equals(url.getProtocol());    //NOI18N
    }
    
    
    private static class Result implements SourceForBinaryQuery.Result, PropertyChangeListener {
        
        private Library lib;
        private URL entry;
        private final ChangeSupport cs = new ChangeSupport(this);
        private FileObject[] cache;
        
        public Result (URL queryFor, Library lib) {
            this.entry = queryFor;
            this.lib = lib;
            this.lib.addPropertyChangeListener(WeakListeners.propertyChange(this, this.lib));
        }
        
        public synchronized FileObject[] getRoots () {
            if (this.cache == null) {
                // entry is not resolved so directly volume content can be searched for it:
                if (this.lib.getContent(J2SELibraryTypeProvider.VOLUME_TYPE_CLASSPATH).contains(entry)) {
                    List<FileObject> result = new ArrayList<FileObject>();
                    for (URL u : lib.getContent(J2SELibraryTypeProvider.VOLUME_TYPE_SRC)) {
                        FileObject sourceRootURL = URLMapper.findFileObject(u);
                        if (sourceRootURL!=null) {
                            result.add (sourceRootURL);
                        }
                    }
                    this.cache = result.toArray(new FileObject[result.size()]);
                }
                else {
                    this.cache = new FileObject[0];
                }
            }
            return this.cache;
        }
        
        public synchronized void addChangeListener (ChangeListener l) {
            assert l != null : "Listener cannot be null"; // NOI18N
            cs.addChangeListener(l);
        }
        
        public synchronized void removeChangeListener (ChangeListener l) {
            assert l != null : "Listener cannot be null"; // NOI18N
            cs.removeChangeListener(l);
        }
        
        public void propertyChange (PropertyChangeEvent event) {
            if (Library.PROP_CONTENT.equals(event.getPropertyName())) {
                synchronized (this) {                    
                    this.cache = null;
                }
                cs.fireChange();
            }
        }

        public boolean preferSources() {
            return false;
        }
        
    }
    
}
