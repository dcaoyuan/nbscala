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


import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Arrays;
import java.net.URL;
import java.net.URI;
import java.net.MalformedURLException;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.spi.java.classpath.PathResourceImplementation;
import org.netbeans.spi.java.classpath.support.ClassPathSupport;
import org.openide.NotifyDescriptor;
import org.openide.ErrorManager;
import org.openide.filesystems.FileUtil;
import org.openide.util.NbBundle;



public class J2SEPlatformCustomizer extends JTabbedPane {

    private static final int CLASSPATH = 0;
    private static final int SOURCES = 1;
    private static final int JAVADOC = 2;

    private J2SEPlatformImpl platform;    

    public J2SEPlatformCustomizer (J2SEPlatformImpl platform) {
        this.platform = platform;
        this.initComponents ();
    }


    private void initComponents () {
        this.getAccessibleContext().setAccessibleName (NbBundle.getMessage(J2SEPlatformCustomizer.class,"AN_J2SEPlatformCustomizer"));
        this.getAccessibleContext().setAccessibleDescription (NbBundle.getMessage(J2SEPlatformCustomizer.class,"AD_J2SEPlatformCustomizer"));
        this.addTab(NbBundle.getMessage(J2SEPlatformCustomizer.class,"TXT_Classes"), createPathTab(CLASSPATH));
        this.addTab(NbBundle.getMessage(J2SEPlatformCustomizer.class,"TXT_Sources"), createPathTab(SOURCES));
        this.addTab(NbBundle.getMessage(J2SEPlatformCustomizer.class,"TXT_Javadoc"), createPathTab(JAVADOC));
    }


    private JComponent createPathTab (int type) {
        return new PathView (this.platform, type);
    }


    private static class PathView extends JPanel {

        private JList resources;
        private JButton addButton;
//        private JButton addURLButton;
        private JButton removeButton;
        private JButton moveUpButton;
        private JButton moveDownButton;
        private int type;
        private File currentDir;

        public PathView (J2SEPlatformImpl platform, int type) {
            this.type = type;
            this.initComponents (platform);
        }

        private void initComponents (J2SEPlatformImpl platform) {
            this.setLayout(new GridBagLayout());
            JLabel label = new JLabel ();
            String key = null;
            String mneKey = null;
            String ad = null;
            switch (type) {
                case CLASSPATH:
                    key = "TXT_JDKClasspath";       //NOI18N
                    mneKey = "MNE_JDKClasspath";    //NOI18N
                    ad = "AD_JDKClasspath";         //NOI18N
                    break;
                case SOURCES:
                    key = "TXT_JDKSources";         //NOI18N
                    mneKey = "MNE_JDKSources";      //NOI18N
                    ad = "AD_JDKSources";      //NOI18N
                    break;
                case JAVADOC:
                    key = "TXT_JDKJavadoc";         //NOI18N
                    mneKey = "MNE_JDKJavadoc";      //NOI18N
                    ad = "AD_JDKJavadoc";      //NOI18N
                    break;
                default:
                    assert false : "Illegal type of panel";     //NOI18N
                    return;
            }
            label.setText (NbBundle.getMessage(J2SEPlatformCustomizer.class,key));
            label.setDisplayedMnemonic(NbBundle.getMessage(J2SEPlatformCustomizer.class,mneKey).charAt(0));
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = GridBagConstraints.RELATIVE;
            c.gridy = GridBagConstraints.RELATIVE;
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.insets = new Insets (6,6,2,6);
            c.fill = GridBagConstraints.HORIZONTAL;
            c.weightx = 1.0;
            ((GridBagLayout)this.getLayout()).setConstraints(label,c);
            this.add (label);
            this.resources = new JList(new PathModel(platform,type));
            label.setLabelFor (this.resources);
            this.resources.getAccessibleContext().setAccessibleDescription(NbBundle.getMessage(J2SEPlatformCustomizer.class,ad));
            this.resources.addListSelectionListener(new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent e) {
                    selectionChanged ();
                }
            });
            JScrollPane spane = new JScrollPane (this.resources);            
            c = new GridBagConstraints();
            c.gridx = GridBagConstraints.RELATIVE;
            c.gridy = GridBagConstraints.RELATIVE;
            c.gridwidth = 1;
            c.gridheight = 5;
            c.insets = new Insets (0,6,6,6);
            c.fill = GridBagConstraints.BOTH;
            c.weightx = 1.0;
            c.weighty = 1.0;
            ((GridBagLayout)this.getLayout()).setConstraints(spane,c);
            this.add (spane);            
            if (type == SOURCES || type == JAVADOC) {                
                this.addButton = new JButton ();
                String text;
                char mne;
                if (type == SOURCES) {
                    text = NbBundle.getMessage(J2SEPlatformCustomizer.class, "CTL_Add");
                    mne = NbBundle.getMessage(J2SEPlatformCustomizer.class, "MNE_Add").charAt(0);
                    ad = NbBundle.getMessage(J2SEPlatformCustomizer.class, "AD_Add");
                }
                else {
                    text = NbBundle.getMessage(J2SEPlatformCustomizer.class, "CTL_AddZip");
                    mne = NbBundle.getMessage(J2SEPlatformCustomizer.class, "MNE_AddZip").charAt(0);
                    ad = NbBundle.getMessage(J2SEPlatformCustomizer.class, "AD_AddZip");
                }
                this.addButton.setText(text);
                this.addButton.setMnemonic(mne);
                this.addButton.getAccessibleContext().setAccessibleDescription (ad);
                addButton.addActionListener( new ActionListener () {
                    public void actionPerformed(ActionEvent e) {
                        addPathElement ();
                    }
                });
                c = new GridBagConstraints();
                c.gridx = 1;
                c.gridy = 1;
                c.gridwidth = GridBagConstraints.REMAINDER;
                c.fill = GridBagConstraints.HORIZONTAL;
                c.anchor = GridBagConstraints.NORTHWEST;
                c.insets = new Insets (0,6,0,6);
                ((GridBagLayout)this.getLayout()).setConstraints(addButton,c);
                this.add (addButton);
//                if (this.type == JAVADOC) {
//                    addURLButton  = new JButton (NbBundle.getMessage(J2SEPlatformCustomizer.class, "CTL_AddURL"));
//                    addURLButton.setMnemonic(NbBundle.getMessage(J2SEPlatformCustomizer.class, "MNE_AddURL").charAt(0));
//                    addURLButton.addActionListener(new ActionListener () {
//                        public void actionPerformed(ActionEvent e) {
//                            addURLElement ();
//                        }
//                    });
//                    c = new GridBagConstraints();
//                    c.gridx = 1;
//                    c.gridy = 2;
//                    c.gridwidth = GridBagConstraints.REMAINDER;
//                    c.fill = GridBagConstraints.HORIZONTAL;
//                    c.anchor = GridBagConstraints.NORTHWEST;
//                    c.insets = new Insets (0,6,6,12);
//                    ((GridBagLayout)this.getLayout()).setConstraints(addURLButton,c);
//                    this.add (addURLButton);
//                }
                removeButton = new JButton (NbBundle.getMessage(J2SEPlatformCustomizer.class, "CTL_Remove"));
                removeButton.setMnemonic(NbBundle.getMessage(J2SEPlatformCustomizer.class, "MNE_Remove").charAt(0));
                removeButton.getAccessibleContext().setAccessibleDescription (NbBundle.getMessage(J2SEPlatformCustomizer.class,"AD_Remove"));
                removeButton.addActionListener( new ActionListener () {
                    public void actionPerformed(ActionEvent e) {
                        removePathElement ();
                    }
                });
                removeButton.setEnabled(false);
                c = new GridBagConstraints();
                c.gridx = 1;
                c.gridy = 3;
                c.gridwidth = GridBagConstraints.REMAINDER;
                c.fill = GridBagConstraints.HORIZONTAL;
                c.anchor = GridBagConstraints.NORTHWEST;
                c.insets = new Insets (12,6,0,6);
                ((GridBagLayout)this.getLayout()).setConstraints(removeButton,c);
                this.add (removeButton);
                moveUpButton = new JButton (NbBundle.getMessage(J2SEPlatformCustomizer.class, "CTL_Up"));
                moveUpButton.setMnemonic(NbBundle.getMessage(J2SEPlatformCustomizer.class, "MNE_Up").charAt(0));
                moveUpButton.getAccessibleContext().setAccessibleDescription (NbBundle.getMessage(J2SEPlatformCustomizer.class,"AD_Up"));
                moveUpButton.addActionListener( new ActionListener () {
                    public void actionPerformed(ActionEvent e) {
                        moveUpPathElement ();
                    }
                });
                moveUpButton.setEnabled(false);
                c = new GridBagConstraints();
                c.gridx = 1;
                c.gridy = 4;
                c.gridwidth = GridBagConstraints.REMAINDER;
                c.fill = GridBagConstraints.HORIZONTAL;
                c.anchor = GridBagConstraints.NORTHWEST;
                c.insets = new Insets (12,6,0,6);
                ((GridBagLayout)this.getLayout()).setConstraints(moveUpButton,c);
                this.add (moveUpButton);
                moveDownButton = new JButton (NbBundle.getMessage(J2SEPlatformCustomizer.class, "CTL_Down"));
                moveDownButton.setMnemonic (NbBundle.getMessage(J2SEPlatformCustomizer.class, "MNE_Down").charAt(0));
                moveDownButton.getAccessibleContext().setAccessibleDescription(NbBundle.getMessage(J2SEPlatformCustomizer.class,"AD_Down"));
                moveDownButton.addActionListener( new ActionListener () {
                    public void actionPerformed(ActionEvent e) {
                        moveDownPathElement ();
                    }
                });
                moveDownButton.setEnabled(false);
                c = new GridBagConstraints();
                c.gridx = 1;
                c.gridy = 5;
                c.gridwidth = GridBagConstraints.REMAINDER;
                c.fill = GridBagConstraints.HORIZONTAL;
                c.anchor = GridBagConstraints.NORTHWEST;
                c.insets = new Insets (5,6,6,6);
                ((GridBagLayout)this.getLayout()).setConstraints(moveDownButton,c);
                this.add (moveDownButton);
            }
        }

//        private void addURLElement() {
//            JPanel p = new JPanel ();
//            GridBagLayout lm = new GridBagLayout();
//            p.setLayout (lm);
//            GridBagConstraints c = new GridBagConstraints ();
//            c.gridx = c.gridy = GridBagConstraints.RELATIVE;
//            c.insets = new Insets (12,12,12,6);
//            c.anchor = GridBagConstraints.NORTHWEST;
//            JLabel label = new JLabel (NbBundle.getMessage(J2SEPlatformCustomizer.class,"CTL_AddJavadocURLMessage"));
//            label.setDisplayedMnemonic ('U');
//            lm.setConstraints(label,c);
//            p.add (label);
//            c = new GridBagConstraints ();
//            c.gridx = c.gridy = GridBagConstraints.RELATIVE;
//            c.gridwidth = GridBagConstraints.REMAINDER;
//            c.insets = new Insets (12,0,12,6);
//            c.fill = GridBagConstraints.HORIZONTAL;
//            c.anchor = GridBagConstraints.NORTHWEST;
//            JTextField text = new JTextField ();
//            text.setColumns(30);
//            text.setText (NbBundle.getMessage(J2SEPlatformCustomizer.class,"TXT_DefaultProtocol"));
//            text.selectAll();
//            label.setLabelFor(text);
//            lm.setConstraints(text,c);
//            p.add (text);            
//            JButton[] options = new JButton[] {
//                new JButton (NbBundle.getMessage(J2SEPlatformCustomizer.class,"CTL_AddJavadocURLTitle")),
//                new JButton (NbBundle.getMessage(J2SEPlatformCustomizer.class,"CTL_Cancel"))
//            };
//            options[0].setMnemonic(NbBundle.getMessage(J2SEPlatformCustomizer.class,"MNE_Add").charAt(0));
//            options[1].setMnemonic(NbBundle.getMessage(J2SEPlatformCustomizer.class,"MNE_Cancel").charAt(0));
//            DialogDescriptor input = new DialogDescriptor (
//                p,
//                NbBundle.getMessage(J2SEPlatformCustomizer.class,"CTL_AddJavadocURLTitle"),
//                true, options, options[0], DialogDescriptor.DEFAULT_ALIGN, null, null);            
//            if (DialogDisplayer.getDefault().notify(input) == options[0]) {
//                try {
//                    String value = text.getText();
//                    URL url = new URL (value);
//                    ((PathModel)this.resources.getModel()).addPath(url);
//                    this.resources.setSelectedIndex (this.resources.getModel().getSize()-1);
//                } catch (MalformedURLException mue) {
//                    DialogDescriptor.Message message = new DialogDescriptor.Message (
//                        NbBundle.getMessage(J2SEPlatformCustomizer.class,"CTL_InvalidURLFormat"),
//                        DialogDescriptor.ERROR_MESSAGE);
//                    DialogDisplayer.getDefault().notify(message);
//                }
//            }
//        }

        private void addPathElement () {
            JFileChooser chooser = new JFileChooser ();
            FileUtil.preventFileChooserSymlinkTraversal(chooser, null);
            chooser.setMultiSelectionEnabled (true);
            String title = null;
            String message = null;
            String approveButtonName = null;
            String approveButtonNameMne = null;
            if (this.type == SOURCES) {
                title = NbBundle.getMessage (J2SEPlatformCustomizer.class,"TXT_OpenSources");
                message = NbBundle.getMessage (J2SEPlatformCustomizer.class,"TXT_Sources");
                approveButtonName = NbBundle.getMessage (J2SEPlatformCustomizer.class,"TXT_OpenSources");
                approveButtonNameMne = NbBundle.getMessage (J2SEPlatformCustomizer.class,"MNE_OpenSources");
            }
            else if (this.type == JAVADOC) {
                title = NbBundle.getMessage (J2SEPlatformCustomizer.class,"TXT_OpenJavadoc");
                message = NbBundle.getMessage (J2SEPlatformCustomizer.class,"TXT_Javadoc");
                approveButtonName = NbBundle.getMessage (J2SEPlatformCustomizer.class,"TXT_OpenJavadoc");
                approveButtonNameMne = NbBundle.getMessage (J2SEPlatformCustomizer.class,"MNE_OpenJavadoc");
            }
            chooser.setDialogTitle(title);
            chooser.setApproveButtonText(approveButtonName);
            chooser.setApproveButtonMnemonic (approveButtonNameMne.charAt(0));
            chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            //#61789 on old macosx (jdk 1.4.1) these two method need to be called in this order.
            chooser.setAcceptAllFileFilterUsed( false );
            chooser.setFileFilter (new SimpleFileFilter(message,new String[] {"ZIP","JAR"}));   //NOI18N
            if (this.currentDir != null) {
                chooser.setCurrentDirectory(this.currentDir);
            }
            if (chooser.showOpenDialog(this)==JFileChooser.APPROVE_OPTION) {
                File[] fs = chooser.getSelectedFiles();
                PathModel model = (PathModel) this.resources.getModel();
                boolean addingFailed = false;
                int firstIndex = this.resources.getModel().getSize();
                for (int i = 0; i < fs.length; i++) {
                    File f = fs[i];
                    //XXX: JFileChooser workaround (JDK bug #5075580), double click on folder returns wrong file
                    // E.g. for /foo/src it returns /foo/src/src
                    // Try to convert it back by removing last invalid name component
                    if (!f.exists()) {
                        File parent = f.getParentFile();
                        if (parent != null && f.getName().equals(parent.getName()) && parent.exists()) {
                            f = parent;
                        }
                    }
                    addingFailed|=!model.addPath (f);
                }
                if (addingFailed) {
                    new NotifyDescriptor.Message (NbBundle.getMessage(J2SEPlatformCustomizer.class,"TXT_CanNotAddResolve"),
                            NotifyDescriptor.ERROR_MESSAGE);
                }
                int lastIndex = this.resources.getModel().getSize()-1;
                if (firstIndex<=lastIndex) {
                    int[] toSelect = new int[lastIndex-firstIndex+1];
                    for (int i = 0; i < toSelect.length; i++) {
                        toSelect[i] = firstIndex+i;
                    }
                    this.resources.setSelectedIndices(toSelect);
                }
                this.currentDir = FileUtil.normalizeFile(chooser.getCurrentDirectory());
            }
        }

        private void removePathElement () {
            int[] indices = this.resources.getSelectedIndices();
            if (indices.length == 0) {
                return;
            }
            PathModel model = (PathModel) this.resources.getModel();
            model.removePath (indices);
            if ( indices[indices.length-1]-indices.length+1 < this.resources.getModel().getSize()) {
                this.resources.setSelectedIndex (indices[indices.length-1]-indices.length+1);
            }
            else if (indices[0]>0) {
                this.resources.setSelectedIndex (indices[0]-1);
            }
        }

        private void moveDownPathElement () {
            int[] indices = this.resources.getSelectedIndices();
            if (indices.length == 0) {
                return;
            }
            PathModel model = (PathModel) this.resources.getModel();
            model.moveDownPath (indices);
            for (int i=0; i< indices.length; i++) {
                indices[i] = indices[i] + 1;
            }
            this.resources.setSelectedIndices (indices);
        }

        private void moveUpPathElement () {
            int[] indices = this.resources.getSelectedIndices();
            if (indices.length == 0) {
                return;
            }
            PathModel model = (PathModel) this.resources.getModel();
            model.moveUpPath (indices);
            for (int i=0; i< indices.length; i++) {
                indices[i] = indices[i] - 1;
            }
            this.resources.setSelectedIndices (indices);
        }

        private void selectionChanged () {
            if (this.type == CLASSPATH) {
                return;
            }
            int indices[] = this.resources.getSelectedIndices();
            this.removeButton.setEnabled (indices.length > 0);
            this.moveUpButton.setEnabled (indices.length > 0 && indices[0]>0);
            this.moveDownButton.setEnabled(indices.length > 0 && indices[indices.length-1]<this.resources.getModel().getSize()-1);
        }

    }


    private static class PathModel extends AbstractListModel/*<String>*/ {

        private J2SEPlatformImpl platform;
        private int type;
        private java.util.List<URL> data;

        public PathModel (J2SEPlatformImpl platform, int type) {
            this.platform = platform;
            this.type = type;
        }

        public int getSize() {
            return this.getData().size();
        }

        public Object getElementAt(int index) {
            java.util.List<URL> list = this.getData();
            URL url = list.get(index);
            if ("jar".equals(url.getProtocol())) {      //NOI18N
                URL fileURL = FileUtil.getArchiveFile (url);
                if (FileUtil.getArchiveRoot(fileURL).equals(url)) {
                    // really the root
                    url = fileURL;
                } else {
                    // some subdir, just show it as is
                    return url.toExternalForm();
                }
            }
            if ("file".equals(url.getProtocol())) {
                File f = new File (URI.create(url.toExternalForm()));
                return f.getAbsolutePath();
            }
            else {
                return url.toExternalForm();
            }
        }

        private void removePath (int[] indices) {
            java.util.List data = getData();
            for (int i=indices.length-1; i>=0; i--) {
                data.remove(indices[i]);
            }
            updatePlatform ();
            fireIntervalRemoved(this,indices[0],indices[indices.length-1]);
        }

        private void moveUpPath (int[] indices) {
            java.util.List<URL> data = getData ();
            for (int i=0; i<indices.length; i++) {
                URL p2 = data.get (indices[i]);
                URL p1 = data.set (indices[i]-1,p2);
                data.set (indices[i],p1);
            }
            updatePlatform ();
            fireContentsChanged(this,indices[0]-1,indices[indices.length-1]);
        }

        private void moveDownPath (int[] indices) {
            java.util.List<URL> data = getData ();
            for (int i=indices.length-1; i>=0; i--) {
                URL p1 = data.get (indices[i]);
                URL p2 = data.set (indices[i]+1,p1);
                data.set (indices[i],p2);
            }
            updatePlatform();
            fireContentsChanged(this,indices[0],indices[indices.length-1]+1);
        }

        private boolean addPath (File f) {
            try {
                URL url = f.toURI().toURL();
                return this.addPath (url);
            } catch (MalformedURLException mue) {
                return false;
            }
        }

        private boolean addPath (URL url) {
            if (FileUtil.isArchiveFile(url)) {
                url = FileUtil.getArchiveRoot (url);
            }
            else if (!url.toExternalForm().endsWith("/")){
                try {
                    url = new URL (url.toExternalForm()+"/");
                } catch (MalformedURLException mue) {
                    ErrorManager.getDefault().notify(mue);
                }
            }
            java.util.List<URL> data = getData();
            int oldSize = data.size ();
            data.add (url);
            updatePlatform();
            fireIntervalAdded(this,oldSize,oldSize);
            return true;
        }

        private synchronized java.util.List<URL> getData () {
            if (this.data == null) {
                switch (this.type) {
                    case CLASSPATH:
                        this.data = getPathList(this.platform.getBootstrapLibraries());
                        break;
                    case SOURCES:
                        this.data = getPathList(this.platform.getSourceFolders());
                        break;
                    case JAVADOC:
                        this.data = new ArrayList<URL>(this.platform.getJavadocFolders());
                        break;
                }
            }
            return this.data;
        }

        private static java.util.List<URL> getPathList (ClassPath cp) {
            java.util.List<URL> result = new ArrayList<URL> ();
            for (ClassPath.Entry entry : cp.entries()) {
                result.add (entry.getURL());
            }
            return result;
        }

        private static ClassPath createClassPath (java.util.List<URL> roots) {
            java.util.List<PathResourceImplementation> resources = new ArrayList<PathResourceImplementation> ();
            for (URL url : roots) {
                resources.add (ClassPathSupport.createResource(url));
            }
            return ClassPathSupport.createClassPath(resources);
        }

        private void updatePlatform () {
            switch (this.type) {
                case SOURCES:
                    this.platform.setSourceFolders(createClassPath(data));
                    break;
                case JAVADOC:
                    this.platform.setJavadocFolders (data);
                    break;
                default:
                    assert false : "Trying to update unknown property";     //NOI18N
            }
        }
    }


    private static class SimpleFileFilter extends FileFilter {

        private String description;
        private Collection extensions;


        public SimpleFileFilter (String description, String[] extensions) {
            this.description = description;
            this.extensions = Arrays.asList(extensions);
        }

        public boolean accept(File f) {
            if (f.isDirectory())
                return true;
            String name = f.getName();
            int index = name.lastIndexOf('.');   //NOI18N
            if (index <= 0 || index==name.length()-1)
                return false;
            String extension = name.substring (index+1).toUpperCase();
            return this.extensions.contains(extension);
        }

        public String getDescription() {
            return this.description;
        }
    }


}
