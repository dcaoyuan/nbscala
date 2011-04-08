/*
 *                 Sun Public License Notice
 *
 * The contents of this file are subject to the Sun Public License
 * Version 1.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://www.sun.com/
 *
 * The Original Code is NetBeans. The Initial Developer of the Original
 * Code is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.

If you wish your version of this file to be governed by only the CDDL
or only the GPL Version 2, indicate your decision by adding
"[Contributor] elects to include this software in this distribution
under the [CDDL or GPL Version 2] license." If you do not indicate a
single choice of license, a recipient has the option to distribute
your version of this file under either the CDDL, the GPL Version 2 or
to extend the choice of license to its licensees as provided above.
However, if you add GPL Version 2 code and therefore, elected the GPL
Version 2 license, then the option applies only if the new code is
made subject to such option by the copyright holder.
 */

package org.netbeans.modules.scala.project;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.io.InputStream;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.modules.scala.project.ui.customizer.CustomizerProviderImpl;
import org.netbeans.modules.scala.project.ui.customizer.J2SECompositePanelProvider;
import org.netbeans.spi.project.ActionProvider;
import org.netbeans.spi.project.ProjectConfiguration;
import org.netbeans.spi.project.ProjectConfigurationProvider;
import org.netbeans.spi.project.support.ant.EditableProperties;
import org.openide.filesystems.FileChangeAdapter;
import org.openide.filesystems.FileChangeListener;
import org.openide.filesystems.FileEvent;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileRenameEvent;
import org.openide.filesystems.FileUtil;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;

/**
 * Manages configurations for a Java SE project.
 * @author Jesse Glick
 */
final class J2SEConfigurationProvider implements ProjectConfigurationProvider<J2SEConfigurationProvider.Config> {

    private static final Logger LOGGER = Logger.getLogger(J2SEConfigurationProvider.class.getName());

    /**
     * Ant property name for active config.
     */
    public static final String PROP_CONFIG = "config"; // NOI18N
    /**
     * Ant property file which specified active config.
     */
    public static final String CONFIG_PROPS_PATH = "nbproject/private/config.properties"; // NOI18N

    public static final class Config implements ProjectConfiguration {
        /** file basename, or null for default config */
        public final String name;
        private final String displayName;
        public Config(String name, String displayName) {
            this.name = name;
            this.displayName = displayName;
        }
        public String getDisplayName() {
            return displayName;
        }
        public int hashCode() {
            return name != null ? name.hashCode() : 0;
        }
        public boolean equals(Object o) {
            return (o instanceof Config) && Utilities.compareObjects(name, ((Config) o).name);
        }
        public String toString() {
            return "J2SEConfigurationProvider.Config[" + name + "," + displayName + "]"; // NOI18N
        }
    }

    private static final Config DEFAULT = new Config(null,
            NbBundle.getMessage(J2SEConfigurationProvider.class, "J2SEConfigurationProvider.default.label"));

    private final J2SEProject p;
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    private final FileChangeListener fcl = new FileChangeAdapter() {
        public void fileFolderCreated(FileEvent fe) {
            update(fe);
        }
        public void fileDataCreated(FileEvent fe) {
            update(fe);
        }
        public void fileDeleted(FileEvent fe) {
            update(fe);
        }
        public void fileRenamed(FileRenameEvent fe) {
            update(fe);
        }
        private void update(FileEvent ev) {
            LOGGER.log(Level.FINEST, "Received {0}", ev);
            Set<String> oldConfigs = configs != null ? configs.keySet() : Collections.<String>emptySet();
            configDir = p.getProjectDirectory().getFileObject("nbproject/configs"); // NOI18N
            if (configDir != null) {
                configDir.removeFileChangeListener(fclWeak);
                configDir.addFileChangeListener(fclWeak);
                LOGGER.log(Level.FINEST, "(Re-)added listener to {0}", configDir);
            } else {
                LOGGER.log(Level.FINEST, "No nbproject/configs exists");
            }
            calculateConfigs();
            Set<String> newConfigs = configs.keySet();
            if (!oldConfigs.equals(newConfigs)) {
                LOGGER.log(Level.FINER, "Firing " + ProjectConfigurationProvider.PROP_CONFIGURATIONS + ": {0} -> {1}", new Object[] {oldConfigs, newConfigs});
                pcs.firePropertyChange(ProjectConfigurationProvider.PROP_CONFIGURATIONS, null, null);
                // XXX also fire PROP_ACTIVE_CONFIGURATION?
            }
        }
    };
    private final FileChangeListener fclWeak;
    private FileObject configDir;
    private Map<String,Config> configs;
    private FileObject nbp;
    
    public J2SEConfigurationProvider(J2SEProject p) {
        this.p = p;
        fclWeak = FileUtil.weakFileChangeListener(fcl, null);
        nbp = p.getProjectDirectory().getFileObject("nbproject"); // NOI18N
        if (nbp != null) {
            nbp.addFileChangeListener(fclWeak);
            LOGGER.log(Level.FINEST, "Added listener to {0}", nbp);
            configDir = nbp.getFileObject("configs"); // NOI18N
            if (configDir != null) {
                configDir.addFileChangeListener(fclWeak);
                LOGGER.log(Level.FINEST, "Added listener to {0}", configDir);
            }
        }
        p.evaluator().addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                if (PROP_CONFIG.equals(evt.getPropertyName())) {
                    LOGGER.log(Level.FINER, "Refiring " + PROP_CONFIG + " -> " + ProjectConfigurationProvider.PROP_CONFIGURATION_ACTIVE);
                    pcs.firePropertyChange(ProjectConfigurationProvider.PROP_CONFIGURATION_ACTIVE, null, null);
                }
            }
        });
    }

    private void calculateConfigs() {
        configs = new HashMap<String,Config>();
        if (configDir != null) {
            for (FileObject kid : configDir.getChildren()) {
                if (!kid.hasExt("properties")) {
                    continue;
                }
                try {
                    InputStream is = kid.getInputStream();
                    try {
                        Properties p = new Properties();
                        p.load(is);
                        String name = kid.getName();
                        String label = p.getProperty("$label"); // NOI18N
                        configs.put(name, new Config(name, label != null ? label : name));
                    } finally {
                        is.close();
                    }
                } catch (IOException x) {
                    LOGGER.log(Level.INFO, null, x);
                }
            }
        }
        LOGGER.log(Level.FINEST, "Calculated configurations: {0}", configs);
    }

    public Collection<Config> getConfigurations() {
        calculateConfigs();
        List<Config> l = new ArrayList<Config>();
        l.addAll(configs.values());
        Collections.sort(l, new Comparator<Config>() {
            Collator c = Collator.getInstance();
            public int compare(Config c1, Config c2) {
                return c.compare(c1.getDisplayName(), c2.getDisplayName());
            }
        });
        l.add(0, DEFAULT);
        return l;
    }

    public Config getActiveConfiguration() {
        calculateConfigs();
        String config = p.evaluator().getProperty(PROP_CONFIG);
        if (config != null && configs.containsKey(config)) {
            return configs.get(config);
        } else {
            return DEFAULT;
        }
    }

    public void setActiveConfiguration(Config c) throws IllegalArgumentException, IOException {
        if (c != DEFAULT && !configs.values().contains(c)) {
            throw new IllegalArgumentException();
        }
        final String n = c.name;
        EditableProperties ep = p.getUpdateHelper().getProperties(CONFIG_PROPS_PATH);
        if (Utilities.compareObjects(n, ep.getProperty(PROP_CONFIG))) {
            return;
        }
        if (n != null) {
            ep.setProperty(PROP_CONFIG, n);
        } else {
            ep.remove(PROP_CONFIG);
        }
        p.getUpdateHelper().putProperties(CONFIG_PROPS_PATH, ep);
        pcs.firePropertyChange(ProjectConfigurationProvider.PROP_CONFIGURATION_ACTIVE, null, null);
        ProjectManager.getDefault().saveProject(p);
        assert p.getProjectDirectory().getFileObject(CONFIG_PROPS_PATH) != null;
    }

    public boolean hasCustomizer() {
        return true;
    }

    public void customize() {
        p.getLookup().lookup(CustomizerProviderImpl.class).showCustomizer(J2SECompositePanelProvider.RUN);
    }

    public boolean configurationsAffectAction(String command) {
        return command.equals(ActionProvider.COMMAND_RUN) ||
               command.equals(ActionProvider.COMMAND_DEBUG);
    }

    public void addPropertyChangeListener(PropertyChangeListener lst) {
        pcs.addPropertyChangeListener(lst);
    }

    public void removePropertyChangeListener(PropertyChangeListener lst) {
        pcs.removePropertyChangeListener(lst);
    }

}
