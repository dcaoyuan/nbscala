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

import java.io.*;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.*;
import java.net.MalformedURLException;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.api.java.platform.JavaPlatformManager;
import org.netbeans.api.scala.platform.ScalaPlatform;
import org.netbeans.spi.java.classpath.support.ClassPathSupport;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;

import org.openide.util.NbBundle;
import org.openide.filesystems.FileUtil;
import org.openide.modules.InstalledFileLocator;

/**
 * Implementation of the "Default" platform. The information here is extracted
 * from the NetBeans' own runtime.
 *
 * @author Svata Dedic
 */
public class DefaultPlatformImpl extends J2SEPlatformImpl {

    public static final String DEFAULT_PLATFORM_ANT_NAME = "default_platform";           //NOI18N

    @SuppressWarnings("unchecked")  //Properties cast to Map<String,String>
    static ScalaPlatform create(Map<String, String> properties, List<URL> sources, List<URL> javadoc) {
        if (properties == null) {
            properties = new HashMap<String, String>();
        }
        // XXX java.home??
        File scalaHome = getScalaHome();       //NOI18N
        List<URL> installFolders = new ArrayList<URL>();
        if (scalaHome != null) {
            try {
                installFolders.add(scalaHome.toURI().toURL());
            } catch (MalformedURLException mue) {
                Exceptions.printStackTrace(mue);
            }
            if (sources == null) {
                sources = getSources(scalaHome);
            }
            if (javadoc == null) {
                javadoc = getJavadoc(scalaHome);
            }
            properties.put(J2SEPlatformImpl.SYSPROP_SCALA_CLASS_PATH, getScalaClassPath());
        } else {
            sources = Collections.emptyList();
            javadoc = Collections.emptyList();
        }
        return new DefaultPlatformImpl(installFolders, properties, new HashMap(System.getProperties()), sources, javadoc);
    }

    private static File getScalaHome() {
        String scalaHome = System.getProperty("scala.home"); // NOI18N

        if (scalaHome == null) {
            scalaHome = System.getenv("SCALA_HOME"); // NOI18N
            if (scalaHome != null) {
                System.setProperty("scala.home", scalaHome);
            }
        }
        if (scalaHome != null) {
            File scalaHomeFile = FileUtil.normalizeFile(new File(scalaHome));       //NOI18N
            return scalaHomeFile;
        } else {
//            File clusterFile = InstalledFileLocator.getDefault().locate(
//                    "modules/org-netbeans-modules-scala-stdplatform.jar", null, false);
//
//            if (clusterFile != null) {
//                File bundlingScalaFile =
//                        new File(clusterFile.getParentFile().getParentFile().getAbsoluteFile(), "scala"); // NOI18N
//                assert bundlingScalaFile.exists() && bundlingScalaFile.isDirectory() : "No bundling Scala platform found";
//
//                for (File scalaFile : bundlingScalaFile.listFiles()) {
//                    String fileName = scalaFile.getName();
//                    if (scalaFile.isDirectory() && fileName.startsWith("scala")) {
//                        scalaHome = scalaFile.getAbsolutePath();
//                        if (isBroken(FileUtil.toFileObject(scalaFile))) {
//                            continue;
//                        }
//                        System.setProperty("scala.home", scalaHome);
//                        int dash = fileName.indexOf('-');
//                        String scalaVersion = fileName.substring(dash + 1, fileName.length());
//                        System.setProperty("scala.specification.version", scalaVersion);
//
//                        return scalaFile;
//                    }
//                }
//            }

            return null;
        }
    }

    private DefaultPlatformImpl(List<URL> installFolders, Map<String, String> platformProperties,
            Map<String, String> systemProperties, List<URL> sources, List<URL> javadoc) {
        super(null, DEFAULT_PLATFORM_ANT_NAME,
                installFolders, platformProperties, systemProperties, sources, javadoc);
    }

    public void setAntName(String antName) {
        throw new UnsupportedOperationException(); //Default platform ant name can not be changed
    }

    public String getDisplayName() {
        String displayName = super.getDisplayName();
        if (displayName == null) {
            displayName = NbBundle.getMessage(DefaultPlatformImpl.class, "TXT_DefaultPlatform", getSpecification().getVersion().toString());
            this.internalSetDisplayName(displayName);
        }
        return displayName;
    }

    public void setDisplayName(String name) {
        throw new UnsupportedOperationException(); //Default platform name can not be changed
    }

    public ClassPath getBootstrapLibraries() {
        synchronized (this) {
            ClassPath cp = (bootstrap == null ? null : bootstrap.get());
            if (cp != null) {
                return cp;
            }
            File scalaHome = getScalaHome();
            String pathSpec = "";  //NOI18N
            if (scalaHome != null && scalaHome.exists() && scalaHome.canRead()) {
                File scalaLib = new File(scalaHome, "lib");  //NOI18N
                if (scalaLib.exists() && scalaLib.canRead()) {
                    pathSpec = scalaLib.getAbsolutePath() + File.separator + "scala-library.jar";
                }
            }

            cp = Util.createClassPath(pathSpec);

            /** @todo how to deal with project's custom java platform ? */
            JavaPlatform javaPlatform = JavaPlatformManager.getDefault().getDefaultPlatform();
            if (javaPlatform != null) {
                ClassPath javaBootstrap = javaPlatform.getBootstrapLibraries();
                List<ClassPath.Entry> entries = javaBootstrap.entries();
                URL[] urls = new URL[entries.size() + 1];
                for (int i = 0; i < entries.size(); i++) {
                    urls[i] = entries.get(i).getURL();
                }
                if (!cp.entries().isEmpty()) {
                    urls[entries.size()] = cp.entries().get(0).getURL();
                }
                cp = ClassPathSupport.createClassPath(urls);
            }

            bootstrap = new WeakReference<ClassPath>(cp);
            return cp;
        }
    }

    @Override
    public ClassPath getStandardLibraries() {
        ClassPath cp = standardLibs.get();
        if (cp != null) {
            return cp;
        }
        String s = getScalaClassPath();
        System.setProperty(J2SEPlatformImpl.SYSPROP_SCALA_CLASS_PATH, s);
        //String s = System.getProperty(SYSPROP_JAVA_CLASS_PATH);       //NOI18N
//        if (s == null) {
//            s = ""; // NOI18N
//        }
        cp = Util.createClassPath(s);
        standardLibs = new WeakReference<ClassPath>(cp);
        return cp;
    }

    private static String getScalaClassPath() {
        File scalaHome = getScalaHome();
        String s = "";  //NOI18N
        if (scalaHome != null && scalaHome.exists() && scalaHome.canRead()) {
            File scalaLib = new File(scalaHome, "lib");  //NOI18N
            if (scalaLib.exists() && scalaLib.canRead()) {
                s = computeScalaClassPath(null, scalaLib);
            }
        }
        return s;
    }

    private static String computeScalaClassPath(String extraCp, final File scalaLib) {
        StringBuilder cp = new StringBuilder();
        File[] libs = scalaLib.listFiles();

        for (File lib : libs) {
            if (lib.getName().endsWith(".jar")) { // NOI18N

                if (cp.length() > 0) {
                    cp.append(File.pathSeparatorChar);
                }

                cp.append(lib.getAbsolutePath());
            }
        }

        // Add in user-specified jars passed via SCALA_EXTRA_CLASSPATH

        if (extraCp != null && File.pathSeparatorChar != ':') {
            // Ugly hack - getClassPath has mixed together path separator chars
            // (:) and filesystem separators, e.g. I might have C:\foo:D:\bar but
            // obviously only the path separator after "foo" should be changed to ;
            StringBuilder p = new StringBuilder();
            int pathOffset = 0;
            for (int i = 0; i < extraCp.length(); i++) {
                char c = extraCp.charAt(i);
                if (c == ':' && pathOffset != 1) {
                    p.append(File.pathSeparatorChar);
                    pathOffset = 0;
                    continue;
                } else {
                    pathOffset++;
                }
                p.append(c);
            }
            extraCp = p.toString();
        }

        if (extraCp == null) {
            extraCp = System.getenv("SCALA_EXTRA_CLASSPATH"); // NOI18N
        }

        if (extraCp != null) {
            if (cp.length() > 0) {
                cp.append(File.pathSeparatorChar);
            }
            //if (File.pathSeparatorChar != ':' && extraCp.indexOf(File.pathSeparatorChar) == -1 &&
            //        extraCp.indexOf(':') != -1) {
            //    extraCp = extraCp.replace(':', File.pathSeparatorChar);
            //}
            cp.append(extraCp);
        }
        return cp.toString(); // NOI18N
    }

    static List<URL> getSources(File scalaHome) {
        if (scalaHome != null) {
            try {
                File scalaSrc;
                scalaSrc = new File(scalaHome, "src");    //NOI18N
                if (scalaSrc.exists() && scalaSrc.canRead()) {
                    List<URL> srcUrls = new ArrayList<URL>();
                    for (File src : scalaSrc.listFiles()) {
                        /** 
                         * @Note:
                         * GSF's indexing does not support jar, zip yet 
                         */
                        if (src.getName().endsWith(".jar") || src.getName().endsWith(".zip")) { // NOI18N
                            URL url = FileUtil.getArchiveRoot(src.toURI().toURL());
                            srcUrls.add(url);
                        } else if (src.isDirectory()) { // NOI18N
                            URL url = src.toURI().toURL();
                            srcUrls.add(url);
                        }
                    }
//                    URL url = FileUtil.getArchiveRoot(scalaSrcDir.toURI().toURL());
//
//                    //Test for src folder in the src.zip on Mac
//                    if (Utilities.getOperatingSystem() == Utilities.OS_MAC) {
//                        try {
//                            FileObject fo = URLMapper.findFileObject(url);
//                            if (fo != null) {
//                                fo = fo.getFileObject("src");    //NOI18N
//                                if (fo != null) {
//                                    url = fo.getURL();
//                                }
//                            }
//                        } catch (FileStateInvalidException fileStateInvalidException) {
//                            Exceptions.printStackTrace(fileStateInvalidException);
//                        }
//                    }
                    return srcUrls;
                }
            } catch (MalformedURLException e) {
                Exceptions.printStackTrace(e);
            }
        }
        return null;
    }

    static List<URL> getJavadoc(File scalaHome) {
        if (scalaHome != null) {
            File scalaDoc = new File(scalaHome, "doc"); //NOI18N
            if (scalaDoc.isDirectory() && scalaDoc.canRead()) {
                try {
                    return Collections.singletonList(scalaDoc.toURI().toURL());
                } catch (MalformedURLException mue) {
                    Exceptions.printStackTrace(mue);
                }
            }
        }
        return null;
    }

    private static boolean isBroken(FileObject scalaHome) {
        Collection<FileObject> folders = Collections.singleton(scalaHome);
        for (String tool : PlatformConvertor.IMPORTANT_TOOLS) {
            if (Util.findTool(tool, folders, null) == null) {
                return true;
            }
        }
        return false;
    }
}
