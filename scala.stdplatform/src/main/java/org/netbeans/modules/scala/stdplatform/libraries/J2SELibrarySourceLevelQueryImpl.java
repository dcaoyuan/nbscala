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

import java.io.DataInputStream;
import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.project.libraries.Library;
import org.netbeans.api.project.libraries.LibraryManager;
import org.netbeans.spi.java.classpath.support.ClassPathSupport;
import org.netbeans.spi.java.queries.SourceLevelQueryImplementation;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.filesystems.URLMapper;

public class J2SELibrarySourceLevelQueryImpl implements SourceLevelQueryImplementation {
    
    private static final String JDK_12 = "1.2";     //NOI18N
    private static final String JDK_13 = "1.3";     //NOI18N
    private static final String JDK_14 = "1.4";     //NOI18N
    private static final String JDK_15 = "1.5";     //NOI18N
    private static final String JDK_UNKNOWN = "";   //NOI18N
    private static final String CLASS = "class";    //NOI18N
    private static final int CF_MAGIC = 0xCAFEBABE;
    private static final int CF_INVALID = -1;
    private static final int CF_11 = 0x2d;
    private static final int CF_12 = 0x2e;
    private static final int CF_13 = 0x2f;
    private static final int CF_14 = 0x30;
    private static final int CF_15 = 0x31;
    
    //Cache for source level
    private Map<Library,String> sourceLevelCache = new WeakHashMap<Library,String>();
    
    //Cache for last used library, helps since queries are sequential
    private /*Soft*/Reference<FileObject> lastUsedRoot;
    private /*Weak*/Reference<Library> lastUsedLibrary;
    
    /** Creates a new instance of J2SELibrarySourceLevelQueryImpl */
    public J2SELibrarySourceLevelQueryImpl() {
    }
    
    public String getSourceLevel(org.openide.filesystems.FileObject javaFile) {        
        Library ll = this.isLastUsed (javaFile);
        if (ll != null) {
            return getSourceLevel (ll);
        }
        for (LibraryManager mgr : LibraryManager.getOpenManagers()) {
            for (Library lib : mgr.getLibraries()) {
                if (!lib.getType().equals(J2SELibraryTypeProvider.LIBRARY_TYPE)) {
                    continue;
                }
                List<URL> sourceRoots = lib.getContent(J2SELibraryTypeProvider.VOLUME_TYPE_SRC);
                if (sourceRoots.isEmpty()) {
                    continue;
                }
                ClassPath cp = ClassPathSupport.createClassPath(sourceRoots.toArray(new URL[sourceRoots.size()]));
                FileObject root = cp.findOwnerRoot(javaFile);
                if (root != null) {
                    setLastUsedRoot(root, lib);
                    return getSourceLevel(lib);
                }
            }
        }
        return null;
    }    
    
    private String getSourceLevel (Library lib) {
        String slevel = sourceLevelCache.get(lib);
        if (slevel == null) {
            slevel = getSourceLevel(lib.getContent(J2SELibraryTypeProvider.VOLUME_TYPE_CLASSPATH));
            this.sourceLevelCache.put (lib,slevel);
        }
        return slevel == JDK_UNKNOWN ? null : slevel;                
    }
    
    private String getSourceLevel (List cpRoots) {
        FileObject classFile = getClassFile (cpRoots);
        if (classFile == null) {
            return JDK_UNKNOWN;
        }
        int version = getClassFileMajorVersion (classFile);
        if (version == CF_11 || version == CF_12) {
            return JDK_12;
        }
        else if (version == CF_13) {
            return JDK_13;
        }
        else if (version == CF_14) {
            return JDK_14;
        }
        else if (version >= CF_15) {
            return JDK_15;
        }        
        return JDK_UNKNOWN;
    }
    
    private FileObject getClassFile (List cpRoots) {
        for (Iterator it = cpRoots.iterator(); it.hasNext();) {
            FileObject root = URLMapper.findFileObject((URL)it.next());
            if (root == null) {
                continue;
            }
            FileObject cf = findClassFile (root);
            if (cf != null) {
                return cf;
            }
        }
        return null;
    }
    
    private FileObject findClassFile (FileObject root) {
        if (root.isData()) {
            if (CLASS.equals(root.getExt())) {
                return root;
            }
            else {
                return null;
            }
        }
        else {
            FileObject[] children = root.getChildren();
            for (int i=0; i<children.length; i++) {
                FileObject result = findClassFile(children[i]);
                if (result != null) {
                    return result;
                }
            }
            return null;
        }
    }
    
    private int getClassFileMajorVersion (FileObject classFile) {
        DataInputStream in = null;
        try {
            in = new DataInputStream (classFile.getInputStream());
            int magic = in.readInt();   
            if (CF_MAGIC != magic) {
                return CF_INVALID;
            }
            short minor = in.readShort(); //Ignore it
            short major = in.readShort();
            return major;
        } catch (IOException e) {
            return CF_INVALID;
        } finally {
            if (in != null) {
                try {
                    in.close ();
                } catch (IOException e) {
                    //Ignore it, can not recover
                }
            }
        }
    }
    
    private synchronized void setLastUsedRoot (FileObject root, Library lib) {
        lastUsedRoot = new SoftReference<FileObject>(root);
        lastUsedLibrary = new WeakReference<Library>(lib);
    }
    
    private synchronized Library isLastUsed (FileObject javaFile) {
        if (lastUsedRoot == null) {
            return null;
        }
        
        FileObject root = lastUsedRoot.get();
        if (root == null) {
            return null;
        }
        
        if (root.equals(javaFile) || FileUtil.isParentOf(root,javaFile)) {
            return lastUsedLibrary.get();
        }
        return null;
    }
    
}
