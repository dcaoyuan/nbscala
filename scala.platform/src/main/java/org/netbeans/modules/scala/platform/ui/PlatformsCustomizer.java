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

package org.netbeans.modules.scala.platform.ui;

import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.swing.JComponent;
import javax.swing.UIManager;
import org.netbeans.api.scala.platform.ScalaPlatform;
import org.netbeans.api.scala.platform.ScalaPlatformManager;
import org.netbeans.modules.scala.platform.wizard.PlatformInstallIterator;
import org.openide.DialogDisplayer;
import org.openide.ErrorManager;
import org.openide.WizardDescriptor;
import org.openide.explorer.ExplorerManager;
import org.openide.explorer.view.BeanTreeView;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataFolder;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Node;
import org.openide.nodes.FilterNode;
import org.openide.nodes.Children;
import org.openide.util.NbBundle;

/**
 * @author  tom
 */
public class PlatformsCustomizer extends javax.swing.JPanel implements PropertyChangeListener, VetoableChangeListener, ExplorerManager.Provider {

    private static final String TEMPLATE = "Templates/Services/Platforms/org-netbeans-api-scala-Platform/javaplatform.xml";  //NOI18N
    private static final String STORAGE = "Services/Platforms/org-netbeans-api-scala-Platform";  //NOI18N   

    private PlatformCategoriesChildren children;
    private ExplorerManager manager;
    private final ScalaPlatform initialPlatform;

    /** Creates new form PlatformsCustomizer */
    public PlatformsCustomizer (ScalaPlatform initialPlatform) {
        this.initialPlatform = (initialPlatform == null) ?
            ScalaPlatformManager.getDefault().getDefaultPlatform() : initialPlatform;
        initComponents();
    }


    public void propertyChange(PropertyChangeEvent evt) {
        if (ExplorerManager.PROP_SELECTED_NODES.equals (evt.getPropertyName())) {
            Node[] nodes = (Node[]) evt.getNewValue();
            if (nodes.length!=1) {
                selectPlatform (null);
            }
            else {
                selectPlatform (nodes[0]);
            }
        }
    }
    
    public void vetoableChange(PropertyChangeEvent evt) throws PropertyVetoException {
        if (ExplorerManager.PROP_SELECTED_NODES.equals (evt.getPropertyName())) {
            Node[] nodes = (Node[]) evt.getNewValue();
            if (nodes.length>1) {
                throw new PropertyVetoException ("Invalid length",evt);   //NOI18N
            }           
        }
    }
        

    public synchronized ExplorerManager getExplorerManager() {
        if (this.manager == null) {
            this.manager = new ExplorerManager ();
            this.manager.setRootContext(new AbstractNode (getChildren()));
            this.manager.addPropertyChangeListener (this);
            this.manager.addVetoableChangeListener(this);
        }
        return manager;
    }

    public @Override void addNotify () {
        super.addNotify();
        this.expandPlatforms (this.initialPlatform);
    }


    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        jPanel3 = new javax.swing.JPanel();
        platforms = new PlatformsView ();
        addButton = new javax.swing.JButton();
        removeButton = new javax.swing.JButton();
        cards = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        platformName = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        platformHome = new javax.swing.JTextField();
        clientArea = new javax.swing.JPanel();
        messageArea = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();

        setLayout(new java.awt.GridBagLayout());

        platforms.setPreferredSize(new java.awt.Dimension(200, 334));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 12, 12, 6);
        add(platforms, gridBagConstraints);
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("org/netbeans/modules/scala/platform/ui/Bundle"); // NOI18N
        platforms.getAccessibleContext().setAccessibleName(bundle.getString("AN_PlatformsCustomizerPlatforms")); // NOI18N
        platforms.getAccessibleContext().setAccessibleDescription(bundle.getString("AD_PlatformsCustomizerPlatforms")); // NOI18N

        addButton.setMnemonic(java.util.ResourceBundle.getBundle("org/netbeans/modules/scala/platform/ui/Bundle").getString("MNE_AddPlatform").charAt(0));
        addButton.setText(bundle.getString("CTL_AddPlatform")); // NOI18N
        addButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addNewPlatform(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 12, 0, 6);
        add(addButton, gridBagConstraints);
        addButton.getAccessibleContext().setAccessibleDescription(bundle.getString("AD_AddPlatform")); // NOI18N

        removeButton.setMnemonic(java.util.ResourceBundle.getBundle("org/netbeans/modules/scala/platform/ui/Bundle").getString("MNE_Remove").charAt(0));
        removeButton.setText(bundle.getString("CTL_Remove")); // NOI18N
        removeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removePlatform(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 6, 0, 6);
        add(removeButton, gridBagConstraints);
        removeButton.getAccessibleContext().setAccessibleDescription(bundle.getString("AD_Remove")); // NOI18N

        cards.setLayout(new java.awt.CardLayout());

        jPanel1.setLayout(new java.awt.GridBagLayout());

        jLabel1.setDisplayedMnemonic(java.util.ResourceBundle.getBundle("org/netbeans/modules/scala/platform/ui/Bundle").getString("MNE_PlatformName").charAt(0));
        jLabel1.setLabelFor(platformName);
        jLabel1.setText(bundle.getString("CTL_PlatformName")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        jPanel1.add(jLabel1, gridBagConstraints);

        platformName.setColumns(25);
        platformName.setEditable(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 6, 0, 0);
        jPanel1.add(platformName, gridBagConstraints);
        platformName.getAccessibleContext().setAccessibleDescription(bundle.getString("AD_PlatformName")); // NOI18N

        jLabel2.setDisplayedMnemonic(java.util.ResourceBundle.getBundle("org/netbeans/modules/scala/platform/ui/Bundle").getString("MNE_PlatformHome").charAt(0));
        jLabel2.setLabelFor(platformHome);
        jLabel2.setText(bundle.getString("CTL_PlatformHome")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(6, 0, 12, 0);
        jPanel1.add(jLabel2, gridBagConstraints);

        platformHome.setColumns(25);
        platformHome.setEditable(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(6, 6, 12, 0);
        jPanel1.add(platformHome, gridBagConstraints);
        platformHome.getAccessibleContext().setAccessibleDescription(bundle.getString("AD_PlatformHome")); // NOI18N

        clientArea.setLayout(new java.awt.GridBagLayout());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.gridheight = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jPanel1.add(clientArea, gridBagConstraints);

        cards.add(jPanel1, "card2");

        messageArea.setLayout(new java.awt.GridBagLayout());
        cards.add(messageArea, "card3");

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 6, 12, 12);
        add(cards, gridBagConstraints);

        jLabel3.setLabelFor(platforms);
        org.openide.awt.Mnemonics.setLocalizedText(jLabel3, bundle.getString("TXT_PlatformsList")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(12, 12, 0, 12);
        add(jLabel3, gridBagConstraints);

        getAccessibleContext().setAccessibleDescription(bundle.getString("AD_PlatformsCustomizer")); // NOI18N
    }// </editor-fold>//GEN-END:initComponents

    private void removePlatform(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removePlatform
        Node[] nodes = getExplorerManager().getSelectedNodes();
        if (nodes.length!=1) {
            assert false : "Illegal number of selected nodes";      //NOI18N
            return;
        }
        DataObject dobj = nodes[0].getLookup().lookup(DataObject.class);
        if (dobj == null) {
            assert false : "Can not find platform definition for node: "+ nodes[0].getDisplayName();      //NOI18N
            return;
        }
        try {
            dobj.delete();
            this.getChildren().refreshPlatforms();
            this.expandPlatforms(null);
        } catch (IOException ioe) {
            ErrorManager.getDefault().notify (ioe);
        }
    }//GEN-LAST:event_removePlatform

    private void addNewPlatform(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addNewPlatform
        try {
            WizardDescriptor wiz = new WizardDescriptor (PlatformInstallIterator.create());
            DataObject template = DataObject.find (FileUtil.getConfigFile(TEMPLATE));
            wiz.putProperty("targetTemplate", template);    //NOI18N
            DataFolder folder = DataFolder.findFolder(FileUtil.getConfigFile(STORAGE));
            wiz.putProperty("targetFolder",folder); //NOI18N
            wiz.putProperty("WizardPanel_autoWizardStyle", Boolean.TRUE); // NOI18N
            wiz.putProperty("WizardPanel_contentDisplayed", Boolean.TRUE); // NOI18N
            wiz.putProperty("WizardPanel_contentNumbered", Boolean.TRUE); // NOI18N
            wiz.setTitle(NbBundle.getMessage(PlatformsCustomizer.class,"CTL_AddPlatformTitle"));
            wiz.setTitleFormat(new java.text.MessageFormat("{0}")); // NOI18N
            Dialog dlg = DialogDisplayer.getDefault().createDialog(wiz);
            try {
                dlg.setVisible(true);
                if (wiz.getValue() == WizardDescriptor.FINISH_OPTION) {
                    this.getChildren().refreshPlatforms();
                    Set result = wiz.getInstantiatedObjects();
                    this.expandPlatforms (result.size() == 0 ? null : (ScalaPlatform)result.iterator().next());
                }
            } finally {
                dlg.dispose();
            }
        } catch (DataObjectNotFoundException dfne) {
            ErrorManager.getDefault().notify (dfne);
        }
        catch (IOException ioe) {
            ErrorManager.getDefault().notify (ioe);
        }
    }//GEN-LAST:event_addNewPlatform


    private synchronized PlatformCategoriesChildren getChildren () {
        if (this.children == null) {
            this.children = new PlatformCategoriesChildren ();
        }
        return this.children;
    }

    private void selectPlatform (Node pNode) {
        this.clientArea.removeAll();
        this.messageArea.removeAll();
        this.removeButton.setEnabled (false);
        if (pNode == null) {
            ((CardLayout)cards.getLayout()).last(cards);
            return;
        }
        JComponent target = messageArea;
        ScalaPlatform platform = pNode.getLookup().lookup(ScalaPlatform.class);
        if (platform != null) {
            this.removeButton.setEnabled (isDefaultPLatform(platform));            
            if (platform.getInstallFolders().size() != 0) {
                this.platformName.setText(pNode.getDisplayName());
                for (FileObject installFolder : platform.getInstallFolders()) {
                    File file = FileUtil.toFile(installFolder);
                    if (file != null) {
                        this.platformHome.setText (file.getAbsolutePath());
                    }
                }
                target = clientArea;
            }
        }            
        if (pNode.hasCustomizer()) {
            Component component = pNode.getCustomizer();
            if (component != null) {
                addComponent(target, component);
            }
        }        
        target.revalidate();
        CardLayout cl = (CardLayout) cards.getLayout();
        if (target == clientArea) {
            cl.first (cards);
        }
        else {
            cl.last (cards);
        }
    }
        
    private static void addComponent (Container container, Component component) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = c.gridy = GridBagConstraints.RELATIVE;
        c.gridheight = c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.weightx = c.weighty = 1.0;
        ((GridBagLayout)container.getLayout()).setConstraints (component,c);
        container.add (component);
    }

    private static boolean isDefaultPLatform (ScalaPlatform platform) {
        ScalaPlatform defaultPlatform = ScalaPlatformManager.getDefault().getDefaultPlatform();
        return defaultPlatform!=null && !defaultPlatform.equals(platform);
    }

    private void expandPlatforms (ScalaPlatform platform) {
        ExplorerManager mgr = this.getExplorerManager();
        Node node = mgr.getRootContext();
        expandAllNodes(this.platforms, node, mgr, platform);
    }

    private static void expandAllNodes (BeanTreeView btv, Node node, ExplorerManager mgr, ScalaPlatform platform) {
        btv.expandNode (node);
        Children ch = node.getChildren();
        if ( ch == Children.LEAF ) {
            if (platform != null && platform.equals(node.getLookup().lookup(ScalaPlatform.class))) {
                try {
                    mgr.setSelectedNodes (new Node[] {node});
                } catch (PropertyVetoException e) {
                    //Ignore it
                }
            }
            return;
        }
        Node nodes[] = ch.getNodes( true );
        for ( int i = 0; i < nodes.length; i++ ) {
            expandAllNodes( btv, nodes[i], mgr, platform);
        }

    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addButton;
    private javax.swing.JPanel cards;
    private javax.swing.JPanel clientArea;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel messageArea;
    private javax.swing.JTextField platformHome;
    private javax.swing.JTextField platformName;
    private org.openide.explorer.view.BeanTreeView platforms;
    private javax.swing.JButton removeButton;
    // End of variables declaration//GEN-END:variables

    
    private static class PlatformsView extends BeanTreeView {
        
        public PlatformsView () {
            super ();
            this.setPopupAllowed (false);
            this.setDefaultActionAllowed(false);
            this.setRootVisible (false);
            this.tree.setEditable(false);
            this.tree.setShowsRootHandles(false);
            this.setBorder(UIManager.getBorder("Nb.ScrollPane.border")); // NOI18N
        }
        
    }
    
    private static class PlatformCategoriesDescriptor implements Comparable<PlatformCategoriesDescriptor> {
        private final String categoryName;
        private final List<Node> platforms;
        private boolean changed = false;
        
        public PlatformCategoriesDescriptor (String categoryName) {
            assert categoryName != null;
            this.categoryName = categoryName;
            this.platforms = new ArrayList<Node>();
        }
        
        public String getName () {
            return this.categoryName;
        }
        
        public List<Node> getPlatform () {                        
            if (changed) {
                //SortedSet can't be used, there can be platforms with the same
                //display name
                Collections.sort(platforms, new PlatformNodeComparator());
                changed = false;
            }
            return Collections.unmodifiableList (this.platforms);
        }
        
        public void add (Node node) {
            this.platforms.add (node);
            this.changed = true;
        }
        
        public @Override int hashCode() {
            return this.categoryName.hashCode ();
        }
        
        public @Override boolean equals(Object other) {
            if (other instanceof PlatformCategoriesDescriptor) {
                PlatformCategoriesDescriptor desc = (PlatformCategoriesDescriptor) other;
                return this.categoryName.equals(desc.categoryName) && 
                this.platforms.size() == desc.platforms.size();
            }
            return false;
        }
        
        public int compareTo(PlatformCategoriesDescriptor desc) {
            return this.categoryName.compareTo (desc.categoryName);
        }
        
    }
    
    private static class PlatformsChildren extends Children.Keys<Node> {
        
        private final List<Node> platforms;
        
        public PlatformsChildren (List<Node> platforms) {
            this.platforms = platforms;
        }

        protected @Override void addNotify() {
            super.addNotify();
            this.setKeys (this.platforms);
        }

        protected @Override void removeNotify() {
            super.removeNotify();
            this.setKeys(new Node[0]);
        }

        protected Node[] createNodes(Node key) {
            return new Node[] {new FilterNode(key, Children.LEAF)};
        }
    }
    
    private static class PlatformCategoryNode extends AbstractNode {
        
        private final PlatformCategoriesDescriptor desc;
        private Node iconDelegate;
        
        public PlatformCategoryNode (PlatformCategoriesDescriptor desc) {
            super (new PlatformsChildren (desc.getPlatform()));
            this.desc = desc;            
            this.iconDelegate = DataFolder.findFolder(FileUtil.getConfigRoot()).getNodeDelegate();
        }
        
        public @Override String getName() {
            return this.desc.getName ();
        }
        
        public @Override String getDisplayName() {
            return this.getName ();
        }
        
        public @Override Image getIcon(int type) {
            return this.iconDelegate.getIcon(type);
        }        
        
        public @Override Image getOpenedIcon(int type) {
            return this.iconDelegate.getOpenedIcon (type);
        }                        
        
    }
    
    private static class PlatformCategoriesChildren extends Children.Keys<PlatformCategoriesDescriptor> {
        
        protected @Override void addNotify() {
            super.addNotify ();
            this.refreshPlatforms ();
        }
        
        protected @Override void removeNotify() {
            super.removeNotify ();
        }
        
        protected Node[] createNodes(PlatformCategoriesDescriptor key) {
            return new Node[] {new PlatformCategoryNode(key)};
        }       
        
        private void refreshPlatforms () {
            FileObject storage = FileUtil.getConfigFile(STORAGE);
            if (storage != null) {
                java.util.Map<String,PlatformCategoriesDescriptor> categories = new HashMap<String,PlatformCategoriesDescriptor>();
                for (FileObject child : storage.getChildren()) {
                    try {
                        DataObject dobj = DataObject.find(child);
                        Node node = dobj.getNodeDelegate();
                        ScalaPlatform platform = node.getLookup().lookup(ScalaPlatform.class);
                        if (platform != null) {
                            String platformType = platform.getSpecification().getName();
                            if (platformType != null) {
                                platformType = platformType.toUpperCase(Locale.ENGLISH);
                                PlatformCategoriesDescriptor platforms = categories.get(platformType);
                                if (platforms == null ) {
                                    platforms = new PlatformCategoriesDescriptor (platformType);
                                    categories.put (platformType, platforms);
                                }
                                platforms.add (node);
                            }
                            else {
                                ErrorManager.getDefault().log ("Platform: "+ platform.getDisplayName() +" has invalid specification.");  //NOI18N
                            }
                        }
                        else {                        
                            ErrorManager.getDefault().log ("Platform node for : "+node.getDisplayName()+" has no platform in its lookup.");   //NOI18N
                        }                    
                    }catch (DataObjectNotFoundException e) {
                        ErrorManager.getDefault().notify(e);
                    }
                 }                                    
                List<PlatformCategoriesDescriptor> keys = new ArrayList<PlatformsCustomizer.PlatformCategoriesDescriptor>(categories.values());
//                if (keys.size() == 1) {
//                    PlatformCategoriesDescriptor desc = (PlatformCategoriesDescriptor) keys.get(0);
//                    this.setKeys (desc.getPlatform());
//                }
//                else {
                    Collections.sort (keys);
                    setKeys(keys);
//                }
            }
        }
        

    }
    
    private static class PlatformNodeComparator implements Comparator<Node> {
        
        public int compare(Node n1, Node n2) {
            return n1.getDisplayName().compareTo(n2.getDisplayName());
        }
    }

}
