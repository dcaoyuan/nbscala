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

package org.netbeans.modules.scala.stdplatform.wizard;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.modules.scala.stdplatform.platformdefinition.J2SEPlatformImpl;
import org.openide.ErrorManager;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.modules.InstalledFileLocator;
import org.openide.util.Utilities;

/**
 * Rather dummy implementation of the Java Platform, but sufficient for communication
 * inside the Wizard.
 * Made public to allow ide/projectimport to reuse it
 */
public final class NewJ2SEPlatform extends J2SEPlatformImpl implements Runnable {
    
    private static final Logger LOGGER = Logger.getLogger(NewJ2SEPlatform.class.getName());
    
    private static Set<String> propertiesToFix = new HashSet<String> ();
    
    //Properties used by IDE which should be fixed not to use resolved symlink
    static {
        propertiesToFix.add ("scala.boot.class.path");    //NOI18N
        propertiesToFix.add ("scala.boot.library.path");  //NOI18N
        propertiesToFix.add ("scala.library.path");      //NOI18N
        propertiesToFix.add ("scala.ext.dirs");          //NOI18N
        propertiesToFix.add ("scala.home");              //NOI18N       
    }
    
    private boolean valid;

    public static NewJ2SEPlatform create (FileObject installFolder) throws IOException {
        assert installFolder != null;
        Map<String,String> platformProperties = new HashMap<String,String> ();
        return new NewJ2SEPlatform (null,Collections.singletonList(installFolder.getURL()),platformProperties,Collections.<String,String>emptyMap());
    }

    private NewJ2SEPlatform (String name, List<URL> installFolders, Map<String,String> platformProperties, Map<String,String> systemProperties) {
        super(name, name, installFolders, platformProperties, systemProperties,null,null);
    }

    public boolean isValid () {
        return this.valid;
    }

    /**
     * Actually performs the detection and stores relevant information
     * in this Iterator
     */
    public void run() {
        try {
            FileObject scala = findTool("scala");
            if (scala == null)
                return;
            File scalaFile = FileUtil.toFile (scala);
            if (scalaFile == null)
                return;
            String scalapath = scalaFile.getAbsolutePath();
            String filePath = File.createTempFile("nb-platformdetect", "properties").getAbsolutePath();
            final String probePath = getSDKProperties(scalapath, filePath);
            File f = new File(filePath);
            Properties p = new Properties();
            InputStream is = new FileInputStream(f);
            p.load(is);
            Map<String,String> m = new HashMap<String,String>(p.size());
            for (Enumeration en = p.keys(); en.hasMoreElements(); ) {
                String k = (String)en.nextElement();
                String v = p.getProperty(k);                
                if (J2SEPlatformImpl.SYSPROP_SCALA_CLASS_PATH.equals(k)) {
                    v = filterProbe (v, probePath);
                }
                else if (J2SEPlatformImpl.SYSPROP_USER_DIR.equals(k)) {
                    v = ""; //NOI18N
                }
                v = fixSymLinks (k,v);
                m.put(k, v);
            }   
            this.setSystemProperties(m);
            this.valid = true;
            is.close();
            f.delete();
        } catch (IOException ex) {
            LOGGER.log(Level.INFO, "Cannot execute probe process", ex);
            this.valid = false;
        }
    }
    
    
    /**
     * Fixes sun.boot.class.path property if it contains resolved
     * symbolic link. On Suse the jdk is symlinked and during update
     * the link is changed
     *
     */
    private String fixSymLinks (String key, String value) {
        if (Utilities.isUnix() && propertiesToFix.contains (key)) {
            try {
                String[] pathElements = value.split(File.pathSeparator);
                boolean changed = false;
                for (Iterator it = this.getInstallFolders().iterator(); it.hasNext();) {
                    File f = FileUtil.toFile ((FileObject) it.next());
                    if (f != null) {
                        String path = f.getAbsolutePath();
                        String canonicalPath = f.getCanonicalPath();
                        if (!path.equals(canonicalPath)) {
                            for (int i=0; i<pathElements.length; i++) {
                                if (pathElements[i].startsWith(canonicalPath)) {
                                    pathElements[i] = path + pathElements[i].substring(canonicalPath.length());
                                    changed = true;
                                }
                            }
                        }
                    }
                }
                if (changed) {
                    StringBuffer sb = new StringBuffer ();
                    for (int i = 0; i<pathElements.length; i++) {
                        if (i > 0) {
                            sb.append(File.pathSeparatorChar);
                        }
                        sb.append(pathElements[i]);                
                    }
                    return sb.toString();
                }
            } catch (IOException ioe) {
                //Return the original value
            }
        }
        return value;
    }


    private String getSDKProperties(String scalaPath, String path) throws IOException {

        Runtime runtime = Runtime.getRuntime();
        try {
            String[] command = new String[5];
            command[0] = scalaPath;
            command[1] = "-classpath";    //NOI18N
            command[2] = InstalledFileLocator.getDefault().locate("modules/ext/org-netbeans-modules-scala-stdplatform-probe.jar", "org.netbeans.modules.scala.stdplatform", false).getAbsolutePath(); // NOI18N
            command[3] = "org.netbeans.modules.scala.stdplatform.wizard.SDKProbe";
            command[4] = path;
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine(String.format("Executing: %s %s %s %s %s", command[0],command[1],command[2],command[3],command[4]));
            }
            final Process process = runtime.exec(command);
            // PENDING -- this may be better done by using ExecEngine, since
            // it produces a cancellable task.
            process.waitFor();
            int exitValue = process.exitValue();
            if (exitValue != 0)
                throw new IOException();
            return command[2];
        } catch (InterruptedException ex) {
            IOException e = new IOException();
            ErrorManager.getDefault().annotate(e,ex);
            throw e;
        }
    }
}








