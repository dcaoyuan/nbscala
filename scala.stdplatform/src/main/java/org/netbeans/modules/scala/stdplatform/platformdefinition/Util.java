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
package org.netbeans.modules.scala.stdplatform.platformdefinition;

import java.text.MessageFormat;
import java.util.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.logging.Logger;
import java.util.zip.ZipFile;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.scala.platform.ScalaPlatform;
import org.netbeans.spi.java.classpath.PathResourceImplementation;
import org.netbeans.spi.java.classpath.support.ClassPathSupport;
import org.openide.ErrorManager;
import org.openide.filesystems.FileUtil;
import org.openide.filesystems.FileObject;
import org.openide.modules.SpecificationVersion;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;

public class Util {

    private Util () {
    }

    static ClassPath createClassPath(String classpath) {
        StringTokenizer tokenizer = new StringTokenizer(classpath, File.pathSeparator);
        List<PathResourceImplementation> list = new ArrayList<PathResourceImplementation>();
        while (tokenizer.hasMoreTokens()) {
            String item = tokenizer.nextToken();
            File f = FileUtil.normalizeFile(new File(item));            
            URL url = getRootURL (f);
            if (url!=null) {
                list.add(ClassPathSupport.createResource(url));
            }
        }
        return ClassPathSupport.createClassPath(list);
    }

    // XXX this method could probably be removed... use standard FileUtil stuff
    static URL getRootURL  (final File f) {        
        try {
            URL url = f.toURI().toURL();
            if (FileUtil.isArchiveFile(url)) {
                url = FileUtil.getArchiveRoot (url);
            }
            else if (!f.exists()) {
                String surl = url.toExternalForm();
                if (!surl.endsWith("/")) {
                    url = new URL (surl+"/");
                }
            }
            else if (f.isFile()) {
                //Slow but it will be called only in very rare cases:
                //file on the classpath for which isArchiveFile returned false
                try {
                    ZipFile z = new ZipFile (f);
                    z.close();
                    url = FileUtil.getArchiveRoot (url);
                } catch (IOException e) {
                    url = null;
                }
            }
            return url;
        } catch (MalformedURLException e) {
            throw new AssertionError(e);            
        }        
    }


    /**
     * Returns normalized name from display name.
     * The normalized name should be used in the Ant properties and external files.
     * @param displayName
     * @return String
     */
    public static String normalizeName (String displayName) {
        StringBuffer normalizedName = new StringBuffer ();
        for (int i=0; i< displayName.length(); i++) {
            char c = displayName.charAt(i);
            if (Character.isJavaIdentifierPart(c) || c =='-' || c =='.') {
                normalizedName.append(c);
            }
            else {
                normalizedName.append('_');
            }
        }
        return normalizedName.toString();
    }

    /**
     * Returns specification version of the given platform.
     *
     * @return instance of SpecificationVersion representing the version; never null
     */
    public static SpecificationVersion getSpecificationVersion(ScalaPlatform plat) {
         String version = plat.getSystemProperties().get("scala.specification.version");   // NOI18N
         if (version == null) {
             version = "1.1";
         }
         return makeSpec(version);
    }

    
    public static FileObject findTool (String toolName, Collection<FileObject> installFolders) {
        return findTool (toolName, installFolders, null);
    }

    public static FileObject findTool (String toolName, Collection<FileObject> installFolders, String archFolderName) {
        assert toolName != null;
        for (FileObject root : installFolders) {
            FileObject bin = root.getFileObject("bin");             //NOI18N
            if (bin == null) {
                continue;
            }
            if (archFolderName != null) {
                bin = bin.getFileObject(archFolderName);
                if (bin == null) {
                    continue;
                }
            }
            FileObject tool = bin.getFileObject(toolName, Utilities.isWindows() ? "bat" : null);    //NOI18N
            if (tool!= null) {
                return tool;
            }
        }
        return null;
    }

    /**
     * Get JRE extension JARs/ZIPs.
     * @param extPath a native-format path for e.g. jre/lib/ext
     * @return a native-format classpath for extension JARs and ZIPs found in it
     */
    public static String getExtensions (String extPath) {
        if (extPath == null) {
            return null;
        }
        StringBuffer sb = new StringBuffer();
        StringTokenizer tk = new StringTokenizer (extPath, File.pathSeparator);
        while (tk.hasMoreTokens()) {
            File extFolder = FileUtil.normalizeFile(new File(tk.nextToken()));
            File[] files = extFolder.listFiles();
            if (files != null) {
                for (int i = 0; i < files.length; i++) {
                    File f = files[i];                   
                    if (!f.exists()) {
                        //May happen, eg. broken link, it is safe to ignore it
                        //since it is an extension directory, but log it.
                        ErrorManager.getDefault().log (ErrorManager.WARNING,
                            MessageFormat.format (NbBundle.getMessage(Util.class,"MSG_BrokenExtension"),
                            new Object[] {f,extFolder}));
                        continue;
                    }
                    if (Utilities.isMac() && "._.DS_Store".equals(f.getName())) {  //NOI18N
                        //Ignore Apple temporary ._.DS_Store files in the lib/ext folder
                        continue;
                    }
                    FileObject fo = FileUtil.toFileObject(f);
                    if (fo == null) {
                        Logger.getLogger(Util.class.getName()).warning("Cannot create FileObject for file: "+f.getAbsolutePath()+" exists: " + f.exists());
                        continue;
                    }
                    if (!FileUtil.isArchiveFile(fo)) {
                        // #42961: Mac OS X has e.g. libmlib_jai.jnilib.
                        continue;
                    }
                    sb.append(File.pathSeparator);
                    sb.append(files[i].getAbsolutePath());
                }
            }
        }
        if (sb.length() == 0) {
            return null;
        }
        return sb.substring(File.pathSeparator.length());
    }

    // copy pasted from org.openide.modules.Dependency:
    /** Try to make a specification version from a string.
     * Deal with errors gracefully and try to recover something from it.
     * E.g. "1.4.0beta" is technically erroneous; correct to "1.4.0".
     */
    private static SpecificationVersion makeSpec(String vers) {
        if (vers != null) {
            try {
                return new SpecificationVersion(vers);
            } catch (NumberFormatException nfe) {
                System.err.println("WARNING: invalid specification version: " + vers); // NOI18N
            }
            do {
                vers = vers.substring(0, vers.length() - 1);
                try {
                    return new SpecificationVersion(vers);
                } catch (NumberFormatException nfe) {
                    // ignore
                }
            } while (vers.length() > 0);
        }
        // Nothing decent in it at all; use zero.
        return new SpecificationVersion("0"); // NOI18N
    }   

}
