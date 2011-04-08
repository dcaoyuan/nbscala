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
package org.netbeans.modules.scala.project.classpath;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.classpath.ClassPath.Entry;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.api.scala.platform.ScalaPlatform;
import org.netbeans.modules.java.api.common.SourceRoots;
import org.netbeans.spi.java.classpath.ClassPathFactory;
import org.netbeans.spi.java.classpath.ClassPathProvider;
import org.netbeans.spi.java.classpath.PathResourceImplementation;
import org.netbeans.spi.java.classpath.support.ClassPathSupport;
import org.netbeans.spi.java.project.classpath.support.ProjectClassPathSupport;
import org.netbeans.spi.project.support.ant.AntProjectHelper;
import org.netbeans.spi.project.support.ant.PropertyEvaluator;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Mutex;
import org.openide.util.WeakListeners;

/**
 * Defines the various class paths for a J2SE project.
 */
public final class ClassPathProviderImpl implements ClassPathProvider, PropertyChangeListener {

    private static final String SCALA_BOOT_CLASSPATH = "scala.boot.classpath";     // NOI18N
    private static final String SCALAC_CLASS_PATH = "scala.classpath";        // NOI18N
    private static final String SCALAC_EXT_PATH = "scala.ext.dirs";            //NOI18N
    private static final String BUILD_CLASSES_DIR = "build.classes.dir"; // NOI18N
    private static final String DIST_JAR = "dist.jar"; // NOI18N
    private static final String BUILD_TEST_CLASSES_DIR = "build.test.classes.dir"; // NOI18N
    private static final String JAVAC_CLASSPATH = "javac.classpath";    //NOI18N
    private static final String JAVAC_TEST_CLASSPATH = "javac.test.classpath";  //NOI18N
    private static final String RUN_CLASSPATH = "run.classpath";    //NOI18N
    private static final String RUN_TEST_CLASSPATH = "run.test.classpath";  //NOI18N
    private final AntProjectHelper helper;
    private final File projectDirectory;
    private final PropertyEvaluator evaluator;
    private final SourceRoots sourceRoots;
    private final SourceRoots testSourceRoots;
    private final ClassPath[] cache = new ClassPath[8];
    private final Map<String, FileObject> dirCache = new HashMap<String, FileObject>();
    private final BootClassPathImplementation bootClassPathImpl;

    public ClassPathProviderImpl(AntProjectHelper helper, PropertyEvaluator evaluator, SourceRoots sourceRoots,
            SourceRoots testSourceRoots) {
        this.helper = helper;
        this.projectDirectory = FileUtil.toFile(helper.getProjectDirectory());
        assert this.projectDirectory != null;
        this.evaluator = evaluator;
        this.sourceRoots = sourceRoots;
        this.testSourceRoots = testSourceRoots;
        this.bootClassPathImpl = new BootClassPathImplementation(evaluator);
        evaluator.addPropertyChangeListener(WeakListeners.propertyChange(this, evaluator));
    }

    private FileObject getDir(final String propname) {
        return ProjectManager.mutex().readAccess(new Mutex.Action<FileObject>() {

            public FileObject run() {
                synchronized (ClassPathProviderImpl.this) {
                    FileObject fo = (FileObject) ClassPathProviderImpl.this.dirCache.get(propname);
                    if (fo == null || !fo.isValid()) {
                        String prop = evaluator.getProperty(propname);
                        if (prop != null) {
                            fo = helper.resolveFileObject(prop);
                            ClassPathProviderImpl.this.dirCache.put(propname, fo);
                        }
                    }
                    return fo;
                }
            }
        });
    }

    private FileObject[] getPrimarySrcPath() {
        return this.sourceRoots.getRoots();
    }

    private FileObject[] getTestSrcDir() {
        return this.testSourceRoots.getRoots();
    }

    private FileObject getBuildClassesDir() {
        return getDir(BUILD_CLASSES_DIR);
    }

    private FileObject getDistJar() {
        return getDir(DIST_JAR);
    }

    private FileObject getBuildTestClassesDir() {
        return getDir(BUILD_TEST_CLASSES_DIR);
    }

    /**
     * Find what a given file represents.
     * @param file a file in the project
     * @return one of: <dl>
     *         <dt>0</dt> <dd>normal source</dd>
     *         <dt>1</dt> <dd>test source</dd>
     *         <dt>2</dt> <dd>built class (unpacked)</dd>
     *         <dt>3</dt> <dd>built test class</dd>
     *         <dt>4</dt> <dd>built class (in dist JAR)</dd>
     *         <dt>-1</dt> <dd>something else</dd>
     *         </dl>
     */
    public int getType(FileObject file) {
        FileObject[] srcPath = getPrimarySrcPath();
        for (int i = 0; i < srcPath.length; i++) {
            FileObject root = srcPath[i];
            if (root.equals(file) || FileUtil.isParentOf(root, file)) {
                return 0;
            }
        }
        srcPath = getTestSrcDir();
        for (int i = 0; i < srcPath.length; i++) {
            FileObject root = srcPath[i];
            if (root.equals(file) || FileUtil.isParentOf(root, file)) {
                return 1;
            }
        }
        FileObject dir = getBuildClassesDir();
        if (dir != null && (dir.equals(file) || FileUtil.isParentOf(dir, file))) {
            return 2;
        }
        dir = getDistJar(); // not really a dir at all, of course
        if (dir != null && dir.equals(FileUtil.getArchiveFile(file))) {
            // XXX check whether this is really the root
            return 4;
        }
        dir = getBuildTestClassesDir();
        if (dir != null && (dir.equals(file) || FileUtil.isParentOf(dir, file))) {
            return 3;
        }
        return -1;
    }

    private ClassPath getCompileTimeClasspath(FileObject file) {
        int type = getType(file);
        return this.getCompileTimeClasspath(type);
    }

    private synchronized ClassPath getCompileTimeClasspath(int type) {
        if (type < 0 || type > 1) {
            // Not a source file.
            return null;
        }

        ClassPath cp = cache[2 + type];
        if (cp == null) {
            List<PathResourceImplementation> resources = new ArrayList<PathResourceImplementation>();

            /**@TODO
             * hacking for get scala platform's classpath and source path,
             * should get them from project's classpath or properties.
             */
            ScalaPlatform scalaPlatform = bootClassPathImpl.findActiveScalaPlatform();
            if (scalaPlatform != null) {
                /**
                 * we are not sure the initial order of gsf classpath and java classpath,
                 * we here just call scalaPlatform.getStandardLibraries() to initial SCALAC_CLASS_PATH
                 */
                ClassPath scalaStdCp = scalaPlatform.getStandardLibraries();
                for (Entry entry : scalaStdCp.entries()) {
                    resources.add(ClassPathSupport.createResource(entry.getURL()));
                }
            }

            if (type == 0) {
                cp = ClassPathFactory.createClassPath(
                        ProjectClassPathSupport.createPropertyBasedClassPathImplementation(
                        projectDirectory, evaluator, new String[]{JAVAC_CLASSPATH, SCALAC_CLASS_PATH})); // NOI18N
            } else {
                cp = ClassPathFactory.createClassPath(
                        ProjectClassPathSupport.createPropertyBasedClassPathImplementation(
                        projectDirectory, evaluator, new String[]{JAVAC_TEST_CLASSPATH, SCALAC_CLASS_PATH})); // NOI18N
            }

            for (ClassPath.Entry entry : cp.entries()) {
                resources.add(ClassPathSupport.createResource(entry.getURL()));
            }
            cp = ClassPathSupport.createClassPath(resources);

            cache[2 + type] = cp;
        }
        return cp;
    }

    private ClassPath getRunTimeClasspath(FileObject file) {
        int type = getType(file);
        if (type < 0 || type > 4) {
            // Unregistered file, or in a JAR.
            // For jar:file:$projdir/dist/*.jar!/**/*.class, it is misleading to use
            // run.classpath since that does not actually contain the file!
            // (It contains file:$projdir/build/classes/ instead.)
            return null;
        } else if (type > 1) {
            type -= 2;            //Compiled source transform into source
        }
        return getRunTimeClasspath(type);
    }

    private synchronized ClassPath getRunTimeClasspath(final int type) {
        ClassPath cp = cache[4 + type];
        if (cp == null) {
            List<PathResourceImplementation> resources = new ArrayList<PathResourceImplementation>();

            ScalaPlatform scalaPlatform = bootClassPathImpl.findActiveScalaPlatform();
            if (scalaPlatform != null) {
                /**
                 * we are not sure the initial order of gsf classpath and java classpath,
                 * we here just call scalaPlatform.getStandardLibraries() to initial SCALAC_CLASS_PATH
                 */
                ClassPath scalaStdLibsCp = scalaPlatform.getStandardLibraries();
                for (ClassPath.Entry entry : scalaStdLibsCp.entries()) {
                    resources.add(ClassPathSupport.createResource(entry.getURL()));
                }
            }

            if (type == 0) {
                cp = ClassPathFactory.createClassPath(
                        ProjectClassPathSupport.createPropertyBasedClassPathImplementation(
                        projectDirectory, evaluator, new String[]{RUN_CLASSPATH, SCALAC_CLASS_PATH})); // NOI18N
            } else if (type == 1) {
                cp = ClassPathFactory.createClassPath(
                        ProjectClassPathSupport.createPropertyBasedClassPathImplementation(
                        projectDirectory, evaluator, new String[]{RUN_TEST_CLASSPATH, SCALAC_CLASS_PATH})); // NOI18N
            } else if (type == 2) {
                //Only to make the CompiledDataNode hapy
                //Todo: Strictly it should return ${run.classpath} - ${build.classes.dir} + ${dist.jar}
                cp = ClassPathFactory.createClassPath(
                        ProjectClassPathSupport.createPropertyBasedClassPathImplementation(
                        projectDirectory, evaluator, new String[]{DIST_JAR, SCALAC_CLASS_PATH})); // NOI18N
            }

            for (ClassPath.Entry entry : cp.entries()) {
                resources.add(ClassPathSupport.createResource(entry.getURL()));
            }
            cp = ClassPathSupport.createClassPath(resources);

            cache[4 + type] = cp;
        }
        return cp;
    }

    private ClassPath getSourcepath(FileObject file) {
        int type = getType(file);
        return this.getSourcepath(type);
    }

    private synchronized ClassPath getSourcepath(int type) {
        if (type < 0 || type > 1) {
            return null;
        }
        ClassPath cp = cache[type];
        if (cp == null) {
            switch (type) {
                case 0:
                    cp = ClassPathFactory.createClassPath(new SourcePathImplementation(this.sourceRoots, helper, evaluator));
                    break;
                case 1:
                    cp = ClassPathFactory.createClassPath(new SourcePathImplementation(this.testSourceRoots, helper, evaluator));
                    break;
            }
        }
        cache[type] = cp;
        return cp;
    }

    private synchronized ClassPath getBootClassPath() {
        ClassPath cp = cache[7];
        if (cp == null) {
            cp = ClassPathFactory.createClassPath(new BootClassPathImplementation(evaluator));
            cache[7] = cp;
        }
        return cp;
    }

    public ClassPath findClassPath(FileObject file, String type) {
        if (type.equals(ClassPath.COMPILE)) {
            return getCompileTimeClasspath(file);
        } else if (type.equals(ClassPath.EXECUTE)) {
            return getRunTimeClasspath(file);
        } else if (type.equals(ClassPath.SOURCE)) {
            return getSourcepath(file);
        } else if (type.equals(ClassPath.BOOT)) {
            return getBootClassPath();
        } else {
            return null;
        }
    }

    /**
     * Returns array of all classpaths of the given type in the project.
     * The result is used for example for GlobalPathRegistry registrations.
     */
    public ClassPath[] getProjectClassPaths(final String type) {
        return ProjectManager.mutex().readAccess(new Mutex.Action<ClassPath[]>() {

            public ClassPath[] run() {
                if (ClassPath.BOOT.equals(type)) {
                    return new ClassPath[]{getBootClassPath()};
                }
                if (ClassPath.COMPILE.equals(type)) {
                    ClassPath[] l = new ClassPath[2];
                    l[0] = getCompileTimeClasspath(0);
                    l[1] = getCompileTimeClasspath(1);
                    return l;
                }
                if (ClassPath.SOURCE.equals(type)) {
                    ClassPath[] l = new ClassPath[2];
                    l[0] = getSourcepath(0);
                    l[1] = getSourcepath(1);
                    return l;
                }
                assert false;
                return null;
            }
        });
    }

    /**
     * Returns the given type of the classpath for the project sources
     * (i.e., excluding tests roots).
     */
    public ClassPath getProjectSourcesClassPath(String type) {
        if (ClassPath.BOOT.equals(type)) {
            return getBootClassPath();
        }
        if (ClassPath.COMPILE.equals(type)) {
            return getCompileTimeClasspath(0);
        }
        if (ClassPath.SOURCE.equals(type)) {
            return getSourcepath(0);
        }
        if (ClassPath.EXECUTE.equals(type)) {
            return getRunTimeClasspath(0);
        }
        assert false;
        return null;
    }

    public synchronized void propertyChange(PropertyChangeEvent evt) {
        dirCache.remove(evt.getPropertyName());
    }

    public String[] getPropertyName(final SourceRoots roots, final String type) {
        if (roots.isTest()) {
            if (ClassPath.COMPILE.equals(type)) {
                return new String[] {JAVAC_TEST_CLASSPATH};
            } else if (ClassPath.EXECUTE.equals(type)) {
                return new String[]{RUN_TEST_CLASSPATH};
            } else {
                return null;
            }
        } else {
            if (ClassPath.COMPILE.equals(type)) {
                return new String[] {JAVAC_CLASSPATH};
            } else if (ClassPath.EXECUTE.equals(type)) {
                return new String[]{RUN_CLASSPATH};
            } else {
                return null;
            }
        }
    }

    public String[] getPropertyName(SourceGroup sg, String type) {
        FileObject root = sg.getRootFolder();
        FileObject[] path = getPrimarySrcPath();
        for (int i = 0; i < path.length; i++) {
            if (root.equals(path[i])) {
                if (ClassPath.COMPILE.equals(type)) {
                    return new String[]{JAVAC_CLASSPATH};
                } else if (ClassPath.EXECUTE.equals(type)) {
                    return new String[]{RUN_CLASSPATH};
                } else {
                    return null;
                }
            }
        }
        path = getTestSrcDir();
        for (int i = 0; i < path.length; i++) {
            if (root.equals(path[i])) {
                if (ClassPath.COMPILE.equals(type)) {
                    return new String[]{JAVAC_TEST_CLASSPATH};
                } else if (ClassPath.EXECUTE.equals(type)) {
                    return new String[]{RUN_TEST_CLASSPATH};
                } else {
                    return null;
                }
            }
        }
        return null;
    }
}
