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

package org.netbeans.modules.scala.debugger.projects;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.netbeans.api.java.platform.JavaPlatformManager;

import org.netbeans.spi.debugger.jpda.SourcePathProvider;
import org.netbeans.spi.debugger.ContextProvider;

import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.classpath.GlobalPathRegistry;
import org.netbeans.api.java.classpath.GlobalPathRegistryEvent;
import org.netbeans.api.java.classpath.GlobalPathRegistryListener;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.api.java.project.JavaProjectConstants;
import org.netbeans.api.java.queries.SourceForBinaryQuery;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.api.project.Sources;
import org.netbeans.api.project.ui.OpenProjects;
import org.netbeans.spi.java.classpath.ClassPathProvider;
import org.netbeans.spi.java.classpath.PathResourceImplementation;
import org.netbeans.spi.java.classpath.support.ClassPathSupport;

import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileStateInvalidException;
import org.openide.filesystems.FileUtil;
import org.openide.filesystems.JarFileSystem;
import org.openide.filesystems.URLMapper;
import org.openide.util.Exceptions;
import org.openide.util.WeakListeners;


/**
 *
 * @author Jan Jancura
 */
public class SourcePathProviderImpl extends SourcePathProvider {
    
    private static boolean          verbose = 
        System.getProperty ("netbeans.debugger.sourcepathproviderimpl") != null;
    
    private static Logger logger = Logger.getLogger("org.netbeans.modules.scala.debugger.projects");
    
    private static final Pattern thisDirectoryPattern = Pattern.compile("(/|\\A)\\./");
    private static final Pattern parentDirectoryPattern = Pattern.compile("(/|\\A)([^/]+?)/\\.\\./");

    /** Contains all known source paths + jdk source path for JPDAStart task */
    private ClassPath               originalSourcePath;
    /** Contains the additional source roots, added at a later time to the original roots. */
    private Set<String>             additionalSourceRoots;
    /** Contains just the source paths which are selected for debugging. */
    private ClassPath               smartSteppingSourcePath;
    private String[]                projectSourceRoots;
    private PropertyChangeSupport   pcs;
    private PathRegistryListener    pathRegistryListener;
    
    public SourcePathProviderImpl () {
        pcs = new PropertyChangeSupport (this);
    }

    public SourcePathProviderImpl (ContextProvider contextProvider) {
        pcs = new PropertyChangeSupport (this);
        //this.session = (Session) contextProvider.lookupFirst 
        //    (null, Session.class);
        Map properties = contextProvider.lookupFirst(null, Map.class);
        
        // 2) get default allSourceRoots of source roots used for stepping
        if (properties != null) {
            smartSteppingSourcePath = (ClassPath) properties.get ("sourcepath");
            ClassPath jdkCP = (ClassPath) properties.get ("jdksources");
            if ( (jdkCP == null) && (JavaPlatform.getDefault () != null) )
                jdkCP = JavaPlatform.getDefault ().getSourceFolders ();
            originalSourcePath = jdkCP == null ? 
                smartSteppingSourcePath :
                ClassPathSupport.createProxyClassPath (
                    new ClassPath[] {
                        jdkCP,
                        smartSteppingSourcePath
                    }
            );
            projectSourceRoots = getSourceRoots(originalSourcePath);
            Set<FileObject> preferredRoots = new HashSet<FileObject>();
            preferredRoots.addAll(Arrays.asList(originalSourcePath.getRoots()));
            Set<FileObject> globalRoots = new TreeSet<FileObject>(new FileObjectComparator());
            globalRoots.addAll(GlobalPathRegistry.getDefault().getSourceRoots());
            globalRoots.removeAll(preferredRoots);
            ClassPath globalCP = ClassPathSupport.createClassPath(globalRoots.toArray(new FileObject[0]));
            originalSourcePath = ClassPathSupport.createProxyClassPath(
                    originalSourcePath,
                    globalCP
            );
        } else {
            pathRegistryListener = new PathRegistryListener();
            GlobalPathRegistry.getDefault().addGlobalPathRegistryListener(
                    WeakListeners.create(GlobalPathRegistryListener.class,
                                         pathRegistryListener,
                                         GlobalPathRegistry.getDefault()));
            JavaPlatformManager.getDefault ().addPropertyChangeListener(
                    WeakListeners.propertyChange(pathRegistryListener,
                                                 JavaPlatformManager.getDefault()));
            
            List<FileObject> allSourceRoots = new ArrayList<FileObject>();
            Set<FileObject> preferredRoots = new HashSet<FileObject>();
            Set<FileObject> addedBinaryRoots = new HashSet<FileObject>();
            Project mainProject = OpenProjects.getDefault().getMainProject();
            if (mainProject != null) {
                SourceGroup[] sgs = ProjectUtils.getSources(mainProject).getSourceGroups(JavaProjectConstants.SOURCES_TYPE_JAVA);
                for (SourceGroup sg : sgs) {
                    ClassPath ecp = ClassPath.getClassPath(sg.getRootFolder(), ClassPath.EXECUTE);
                    if (ecp == null) {
                        ecp = ClassPath.getClassPath(sg.getRootFolder(), ClassPath.SOURCE);
                    }
                    if (ecp != null) {
                        FileObject[] binaryRoots = ecp.getRoots();
                        for (FileObject fo : binaryRoots) {
                            if (addedBinaryRoots.contains(fo)) {
                                continue;
                            }
                            addedBinaryRoots.add(fo);
                            try {
                                FileObject[] roots = SourceForBinaryQuery.findSourceRoots(fo.getURL()).getRoots();
                                for (FileObject fr : roots) {
                                    if (!preferredRoots.contains(fr)) {
                                        allSourceRoots.add(fr);
                                        preferredRoots.add(fr);
                                    }
                                }
                            } catch (FileStateInvalidException ex) {
                                Exceptions.printStackTrace(ex);
                            }
                        }
                    }
                }
            }
            Set<FileObject> globalRoots = new TreeSet<FileObject>(new FileObjectComparator());
            globalRoots.addAll(GlobalPathRegistry.getDefault().getSourceRoots());
            for (FileObject fo : globalRoots) {
                if (!preferredRoots.contains(fo)) {
                    allSourceRoots.add(fo);
                }
            }
            
            List<FileObject> allSourceRoots1 = new ArrayList<FileObject>(allSourceRoots.size());
            for (FileObject fo: allSourceRoots) {
                if (FileUtil.isArchiveFile(fo)) {
                    allSourceRoots1.add(FileUtil.getArchiveRoot(fo));
                } else {
                    allSourceRoots1.add(fo);
                }
            }
            originalSourcePath = createClassPath(allSourceRoots1);
            
            projectSourceRoots = getSourceRoots(originalSourcePath);
            
            JavaPlatform[] platforms = JavaPlatformManager.getDefault ().
                getInstalledPlatforms ();
            int i, k = platforms.length;
            for (i = 0; i < k; i++) {
                FileObject[] roots = platforms [i].getSourceFolders ().
                    getRoots ();
                int j, jj = roots.length;
                for (j = 0; j < jj; j++)
                    allSourceRoots1.remove (roots [j]);
            }
            smartSteppingSourcePath = createClassPath(allSourceRoots1);
        }
        
        if (verbose) 
            System.out.println 
                ("SPPI: init originalSourcePath " + originalSourcePath);    
        if (verbose) 
            System.out.println (
                "SPPI: init smartSteppingSourcePath " + smartSteppingSourcePath
            );    
    }
    
    /** 
     * Creates a ClassPath, while guarding for an IllegalArgumentException, 
     * as exemplified in  org.netbeans.modules.debugger.jpda.projects.SourcePathProviderImpl
     * @param froots file object list from which to create the classpath
     * @return 
     */
    private ClassPath createClassPath(List<FileObject> froots) {
        List<PathResourceImplementation> pris = new ArrayList<PathResourceImplementation> ();
        for (FileObject fo : froots) {
            if (fo != null && fo.canRead()) {
                try {
                    URL url = fo.toURL();
                    pris.add(ClassPathSupport.createResource(url));
                } catch (IllegalArgumentException iaex) {
                    // Can be thrown from ClassPathSupport.createResource()
                    // Ignore - bad source root
                    //logger.log(Level.INFO, "Invalid source root = "+fo, iaex);
                    logger.warning(iaex.getLocalizedMessage());
                }
            }
        }
        return ClassPathSupport.createClassPath(pris);
    }
    
    

    /**
     * Translates a relative path ("java/lang/Thread.java") to url 
     * ("file:///C:/Sources/java/lang/Thread.java"). Uses GlobalPathRegistry
     * if global == true.
     *
     * @param relativePath a relative path (java/lang/Thread.java)
     * @param global true if global path should be used
     * @return url or <code>null</code>
     */
    public String getURL (String relativePath, boolean global) {    if (verbose) System.out.println ("SPPI: getURL " + relativePath + " global " + global);
        FileObject fo;
        relativePath = normalize(relativePath);
        if (originalSourcePath == null) {
            fo = GlobalPathRegistry.getDefault().findResource(relativePath);
        } else {
            synchronized (this) {
                if (!global) {
                    fo = smartSteppingSourcePath.findResource(relativePath);
                                                                    if (verbose) System.out.println ("SPPI:   fo " + fo);
                } else {
                    fo = originalSourcePath.findResource(relativePath);
                                                                    if (verbose) System.out.println ("SPPI:   fo " + fo);
                }
            }
        }
        if (fo == null) return null;
        try {
            return fo.getURL ().toString ();
        } catch (FileStateInvalidException e) {                     if (verbose) System.out.println ("SPPI:   FileStateInvalidException");
            return null;
        }
    }
    
    /**
     * Translates a relative path to all possible URLs.
     * Uses GlobalPathRegistry if global == true.
     *
     * @param relativePath a relative path (java/lang/Thread.java)
     * @param global true if global path should be used
     * @return url
     */
    public String[] getAllURLs (String relativePath, boolean global) {      if (verbose) System.out.println ("SPPI: getURL " + relativePath + " global " + global);
        List<FileObject> fos;
        relativePath = normalize(relativePath);
        if (originalSourcePath == null) {
            fos = new ArrayList<FileObject>();
            for (ClassPath cp : GlobalPathRegistry.getDefault().getPaths(ClassPath.SOURCE)) {
                fos.addAll(cp.findAllResources(relativePath));
            }
        } else {
            synchronized (this) {
                if (!global) {
                    fos = smartSteppingSourcePath.findAllResources(relativePath);
                                                                            if (verbose) System.out.println ("SPPI:   fos " + fos);
                } else {
                    fos = originalSourcePath.findAllResources(relativePath);
                                                                            if (verbose) System.out.println ("SPPI:   fos " + fos);
                }
            }
        }
        List<String> urls = new ArrayList<String>(fos.size());
        for (FileObject fo : fos) {
            try {
                urls.add(fo.getURL().toString());
            } catch (FileStateInvalidException e) {                         if (verbose) System.out.println ("SPPI:   FileStateInvalidException for "+fo);
                // skip it
            }
        }
        return urls.toArray(new String[0]);
    }
    
    /**
     * Returns relative path for given url.
     *
     * @param url a url of resource file
     * @param directorySeparator a directory separator character
     * @param includeExtension whether the file extension should be included 
     *        in the result
     *
     * @return relative path
     */
    public String getRelativePath (
        String url, 
        char directorySeparator, 
        boolean includeExtension
    ) {
        // 1) url -> FileObject
        FileObject fo = null;                                       if (verbose) System.out.println ("SPPI: getRelativePath " + url);
        try {
            fo = URLMapper.findFileObject (new URL (url));          if (verbose) System.out.println ("SPPI:   fo " + fo);
        } catch (MalformedURLException e) {
            //e.printStackTrace ();
            return null;
        }
        String relativePath = smartSteppingSourcePath.getResourceName (
            fo, 
            directorySeparator,
            includeExtension
        );
        if (relativePath == null) {
            // fallback to FileObject's class path
            ClassPath cp = ClassPath.getClassPath (fo, ClassPath.SOURCE);
            if (cp == null)
                cp = ClassPath.getClassPath (fo, ClassPath.COMPILE);
            if (cp == null) return null;
            relativePath = cp.getResourceName (
                fo, 
                directorySeparator,
                includeExtension
            );
        }
        return relativePath;
    }
    
    /**
     * Returns the source root (if any) for given url.
     *
     * @param url a url of resource file
     *
     * @return the source root or <code>null</code> when no source root was found.
     */
    @Override
    public synchronized String getSourceRoot(String url) {
        FileObject fo;
        try {
            fo = URLMapper.findFileObject(new java.net.URL(url));
        } catch (java.net.MalformedURLException ex) {
            fo = null;
        }
        FileObject[] roots = null;
        if (fo != null) {
            ClassPath cp = ClassPath.getClassPath(fo, ClassPath.SOURCE);
            if (cp != null) {
                roots = cp.getRoots();
            }
        }
        if (roots == null) {
            roots = originalSourcePath.getRoots();
        }
        for (FileObject fileObject : roots) {
            try {
                String rootURL = fileObject.getURL().toString();
                if (url.startsWith(rootURL)) {
                    String root = getRoot(fileObject);
                    if (root != null) {
                        return root;
                    }
                }
            } catch (FileStateInvalidException ex) {
                // Invalid source root - skip
            }
        }
        return null; // not found
    }
    
    private String[] getSourceRoots(ClassPath classPath) {
        FileObject[] sourceRoots = classPath.getRoots();
        List<String> roots = new ArrayList<String>(sourceRoots.length);
        for (FileObject fo : sourceRoots) {
            String root = getRoot(fo);
            if (root != null) {
                roots.add(root);
            }
        }
        return roots.toArray(new String[0]);
    }
    
    /**
     * Returns allSourceRoots of original source roots.
     *
     * @return allSourceRoots of original source roots
     */
    public synchronized String[] getOriginalSourceRoots () {
        return getSourceRoots(originalSourcePath);
    }
    
    /**
     * Returns array of source roots.
     *
     * @return array of source roots
     */
    public synchronized String[] getSourceRoots () {
        return getSourceRoots(smartSteppingSourcePath);
    }
    
    /**
     * Returns the project's source roots.
     * 
     * @return array of source roots belonging to the project
     */
    public String[] getProjectSourceRoots() {
        return projectSourceRoots;
    }
    
    /**
     * Sets array of source roots.
     *
     * @param sourceRoots a new array of sourceRoots
     */
    public void setSourceRoots (String[] sourceRoots) {
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("SourcePathProviderImpl.setSourceRoots("+java.util.Arrays.asList(sourceRoots)+")");
        }
        Set<String> newRoots = new HashSet<String>(Arrays.asList(sourceRoots));
        ClassPath oldCP = null;
        ClassPath newCP = null;
        synchronized (this) {
            List<FileObject> sourcePath = new ArrayList<FileObject>(
                    Arrays.asList(smartSteppingSourcePath.getRoots()));
            List<FileObject> sourcePathOriginal = new ArrayList<FileObject>(
                    Arrays.asList(originalSourcePath.getRoots()));

            // First check whether there are some new source roots
            Set<String> newOriginalRoots = new HashSet<String>(newRoots);
            for (FileObject fo : sourcePathOriginal) {
                newOriginalRoots.remove(getRoot(fo));
            }
            if (!newOriginalRoots.isEmpty()) {
                for (String root : newOriginalRoots) {
                    FileObject fo = getFileObject(root);
                    if (fo != null) {
                        sourcePathOriginal.add(fo);
                    }
                }
                originalSourcePath =
                        ClassPathSupport.createClassPath(
                            sourcePathOriginal.toArray(new FileObject[0]));
                if (additionalSourceRoots == null) {
                    additionalSourceRoots = new HashSet<String>();
                }
                additionalSourceRoots.addAll(newOriginalRoots);
            }

            // Then correct the smart-stepping path
            Set<String> newSteppingRoots = new HashSet<String>(newRoots);
            for (FileObject fo : sourcePath) {
                newSteppingRoots.remove(getRoot(fo));
            }
            Set<FileObject> removedSteppingRoots = new HashSet<FileObject>();
            Set<FileObject> removedOriginalRoots = new HashSet<FileObject>();
            for (FileObject fo : sourcePath) {
                String spr = getRoot(fo);
                if (!newRoots.contains(spr)) {
                    removedSteppingRoots.add(fo);
                    if (additionalSourceRoots != null && additionalSourceRoots.contains(spr)) {
                        removedOriginalRoots.add(fo);
                        additionalSourceRoots.remove(spr);
                        if (additionalSourceRoots.size() == 0) {
                            additionalSourceRoots = null;
                        }
                    }
                }
            }
            if (removedOriginalRoots.size() > 0) {
                sourcePathOriginal.removeAll(removedOriginalRoots);
                originalSourcePath =
                        ClassPathSupport.createClassPath(
                            sourcePathOriginal.toArray(new FileObject[0]));
            }
            if (newSteppingRoots.size() > 0 || removedSteppingRoots.size() > 0) {
                for (String root : newSteppingRoots) {
                    FileObject fo = getFileObject(root);
                    if (fo != null) {
                        sourcePath.add(fo);
                    }
                }
                sourcePath.removeAll(removedSteppingRoots);
                oldCP = smartSteppingSourcePath;
                smartSteppingSourcePath =
                        ClassPathSupport.createClassPath(
                            sourcePath.toArray(new FileObject[0]));
                newCP = smartSteppingSourcePath;
            }
        }
        
        if (oldCP != null) {
            pcs.firePropertyChange (PROP_SOURCE_ROOTS, oldCP, newCP);
        }
    }
    
    /**
     * Adds property change listener.
     *
     * @param l new listener.
     */
    public void addPropertyChangeListener (PropertyChangeListener l) {
        pcs.addPropertyChangeListener (l);
    }

    /**
     * Removes property change listener.
     *
     * @param l removed listener.
     */
    public void removePropertyChangeListener (
        PropertyChangeListener l
    ) {
        pcs.removePropertyChangeListener (l);
    }
    
    
    // helper methods ..........................................................
    
    /**
     * Normalizes the given path by removing unnecessary "." and ".." sequences.
     * This normalization is needed because the compiler stores source paths like "foo/../inc.jsp" into .class files. 
     * Such paths are not supported by our ClassPath API.
     * TODO: compiler bug? report to JDK?
     * 
     * @param path path to normalize
     * @return normalized path without "." and ".." elements
     */ 
    public static String normalize(String path) {
      for (Matcher m = thisDirectoryPattern.matcher(path); m.find(); )
      {
        path = m.replaceAll("$1");
        m = thisDirectoryPattern.matcher(path);
      }
      for (Matcher m = parentDirectoryPattern.matcher(path); m.find(); )
      {
        if (!m.group(2).equals("..")) {
          path = path.substring(0, m.start()) + m.group(1) + path.substring(m.end());
          m = parentDirectoryPattern.matcher(path);        
        }
      }
      return path;
    }
    
    /**
     * Returns source root for given ClassPath root as String, or <code>null</code>.
     */
    private static String getRoot(FileObject fileObject) {
        File f = null;
        String path = "";
        try {
            if (fileObject.getFileSystem () instanceof JarFileSystem) {
                f = ((JarFileSystem) fileObject.getFileSystem ()).getJarFile ();
                if (!fileObject.isRoot()) {
                    path = "!/"+fileObject.getPath();
                }
            } else {
                f = FileUtil.toFile (fileObject);
            }
        } catch (FileStateInvalidException ex) {
        }
        if (f != null) {
            return f.getAbsolutePath () + path;
        } else {
            return null;
        }
    }

    /**
     * Returns FileObject for given String.
     */
    private FileObject getFileObject (String file) {
        File f = new File (file);
        FileObject fo = FileUtil.toFileObject (f);
        String path = null;
        if (fo == null && file.contains("!/")) {
            int index = file.indexOf("!/");
            f = new File(file.substring(0, index));
            fo = FileUtil.toFileObject (f);
            path = file.substring(index + "!/".length());
        }
        if (fo != null && FileUtil.isArchiveFile (fo)) {
            fo = FileUtil.getArchiveRoot (fo);
            if (path !=null) {
                fo = fo.getFileObject(path);
            }
        }
        return fo;
    }
    
    private class PathRegistryListener implements GlobalPathRegistryListener, PropertyChangeListener {
        
        public void pathsAdded(GlobalPathRegistryEvent event) {
            List<FileObject> addedRoots = new ArrayList<FileObject>();
            for (ClassPath cp : event.getChangedPaths()) {
                for (FileObject fo : cp.getRoots()) {
                    addedRoots.add(fo);
                }
            }
            if (addedRoots.size() > 0) {
                synchronized (SourcePathProviderImpl.this) {
                    List<FileObject> sourcePaths = new ArrayList<FileObject>(
                            Arrays.asList(originalSourcePath.getRoots()));
                    sourcePaths.addAll(addedRoots);
                    originalSourcePath =
                            ClassPathSupport.createClassPath(
                                sourcePaths.toArray(new FileObject[0]));

                    sourcePaths = new ArrayList<FileObject>(
                            Arrays.asList(smartSteppingSourcePath.getRoots()));
                    sourcePaths.addAll(addedRoots);
                    smartSteppingSourcePath =
                            ClassPathSupport.createClassPath(
                                sourcePaths.toArray(new FileObject[0]));
                }
                pcs.firePropertyChange (PROP_SOURCE_ROOTS, null, null);
            }
        }
        
        public void pathsRemoved(GlobalPathRegistryEvent event) {
            List<FileObject> removedRoots = new ArrayList<FileObject>();
            for (ClassPath cp : event.getChangedPaths()) {
                for (FileObject fo : cp.getRoots()) {
                    removedRoots.add(fo);
                }
            }
            if (removedRoots.size() > 0) {
                synchronized (SourcePathProviderImpl.this) {
                    List<FileObject> sourcePaths = new ArrayList<FileObject>(
                            Arrays.asList(originalSourcePath.getRoots()));
                    sourcePaths.removeAll(removedRoots);
                    originalSourcePath =
                            ClassPathSupport.createClassPath(
                                sourcePaths.toArray(new FileObject[0]));

                    sourcePaths = new ArrayList<FileObject>(
                            Arrays.asList(smartSteppingSourcePath.getRoots()));
                    sourcePaths.removeAll(removedRoots);
                    smartSteppingSourcePath =
                            ClassPathSupport.createClassPath(
                                sourcePaths.toArray(new FileObject[0]));
                }
                pcs.firePropertyChange (PROP_SOURCE_ROOTS, null, null);
            }
        }

        public void propertyChange(PropertyChangeEvent evt) {
            // JDK sources changed
            JavaPlatform[] platforms = JavaPlatformManager.getDefault ().
                getInstalledPlatforms ();
            boolean changed = false;
            synchronized (SourcePathProviderImpl.this) {
                List<FileObject> sourcePaths = new ArrayList<FileObject>(
                        Arrays.asList(originalSourcePath.getRoots()));
                for(JavaPlatform jp : platforms) {
                    FileObject[] roots = jp.getSourceFolders().getRoots ();
                    for (FileObject fo : roots) {
                        if (!sourcePaths.contains(fo)) {
                            sourcePaths.add(fo);
                            changed = true;
                        }
                    }
                }
                if (changed) {
                    originalSourcePath =
                            ClassPathSupport.createClassPath(
                                sourcePaths.toArray(new FileObject[0]));
                }
            }
            if (changed) {
                pcs.firePropertyChange (PROP_SOURCE_ROOTS, null, null);
            }
        }
    }
    
    private static final class FileObjectComparator implements Comparator<FileObject> {

        public int compare(FileObject fo1, FileObject fo2) {
            String r1 = getRoot(fo1);
            String r2 = getRoot(fo2);
            if (r1 == null) {
                return -1;
            }
            if (r2 == null) {
                return +1;
            }
            return r1.compareTo(r2);
        }
        
    }
}
