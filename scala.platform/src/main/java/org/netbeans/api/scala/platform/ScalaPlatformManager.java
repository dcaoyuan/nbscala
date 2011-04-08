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

package org.netbeans.api.scala.platform;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import org.netbeans.modules.scala.platform.FallbackDefaultScalaPlatform;
import org.netbeans.modules.scala.platform.ScalaPlatformProvider;
import org.openide.util.Lookup;
import org.openide.util.LookupListener;
import org.openide.util.LookupEvent;
import org.openide.modules.SpecificationVersion;

/**
 * ScalaPlatformManager provides access to list of installed Scala Platforms in the system. It can enumerate them,
 * assign serializable IDs to their instances. It also defines a `default' platform, which represents NetBeans'
 * own runtime environment.
 *
 * @author Radko Najman, Svata Dedic, Tomas Zezula
 */
public final class ScalaPlatformManager {

    /**
     * Property name of the installedPlatforms property
     */
    public static final String PROP_INSTALLED_PLATFORMS="installedPlatforms";   //NOI18N
    public static final String SCALA_PLATFORM_PROP_ID = "scala.platform.ant.name"; //NOI18N

    private static ScalaPlatformManager instance = null;

    private Lookup.Result<ScalaPlatformProvider> providers;
    private Collection<? extends ScalaPlatformProvider> lastProviders = Collections.emptySet();
    private boolean providersValid = false;
    private PropertyChangeListener pListener;
    private Collection<ScalaPlatform> cachedPlatforms;
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    /** Creates a new instance of ScalaPlatformManager */
    public ScalaPlatformManager() {
    }

    /** Gets an instance of ScalaPlatformManager. It the instance doesn't exist it will be created.
     * @return the instance of ScalaPlatformManager
     */
    public static synchronized ScalaPlatformManager getDefault() {
        if (instance == null)
            instance = new ScalaPlatformManager();

        return instance;
    }

    /**
     * Returns default platform. The platform the IDE is running on.
     * @return the default platform (never null as of org.netbeans.modules.java.platform/1 1.9)
     */
    public ScalaPlatform getDefaultPlatform() {
        for (ScalaPlatformProvider provider : getProviders()) {
            ScalaPlatform defaultPlatform = provider.getDefaultPlatform ();
            if (defaultPlatform!=null) {
                return defaultPlatform;
            }
        }
        return new FallbackDefaultScalaPlatform();
    }

    /** Gets an array of JavaPlatfrom objects.
     * @return the array of java platform definitions.
     */
    public synchronized ScalaPlatform[] getInstalledPlatforms() {
        if (cachedPlatforms == null) {
            cachedPlatforms = new HashSet<ScalaPlatform>();
            for (ScalaPlatformProvider provider : getProviders()) {
                cachedPlatforms.addAll(Arrays.asList(provider.getInstalledPlatforms()));
            }
        }
        return cachedPlatforms.toArray(new ScalaPlatform[cachedPlatforms.size()]);
    }

    /**
     * Returns platform given by display name and/or specification.
     * @param platformDisplayName display name of platform or null for any name.
     * @param platformSpec Specification of platform or null for platform of any type, in the specification null means all.
     * Specification with null profiles means none or any profile.
     * Specification with Profile(null,null) means any profile but at least 1.
     * For example Specification ("CLDC", new Profile[] { new Profile("MIMDP",null), new Profile(null,null)})
     * matches all CLDC platforms with MIDP profile of any versions and any additional profile.
     * @return ScalaPlatform[], never returns null, may return empty array when no platform matches given
     * query.
     */
    public ScalaPlatform[] getPlatforms (String platformDisplayName, Specification platformSpec) {
        Collection<ScalaPlatform> result = new ArrayList<ScalaPlatform>();
        for (ScalaPlatform platform : getInstalledPlatforms()) {
            String name = platformDisplayName == null ? null : platform.getDisplayName(); //Don't ask for display name when not needed
            Specification spec = platformSpec == null ?  null : platform.getSpecification(); //Don't ask for platform spec when not needed
            if ((platformDisplayName==null || name.equalsIgnoreCase(platformDisplayName)) &&
                (platformSpec == null || compatible (spec, platformSpec))) {
                result.add(platform);
            }
        }
        return result.toArray(new ScalaPlatform[result.size()]);
    }

    /**
     * Adds PropertyChangeListener to the ScalaPlatformManager, the listener is notified
     * when the platform is added,removed or modified.
     * @param l the listener, can not be null
     */
    public void addPropertyChangeListener (PropertyChangeListener l) {
        assert l != null : "Listener can not be null";  //NOI18N
        pcs.addPropertyChangeListener(l);
    }

    /**
     * Removes PropertyChangeListener to the ScalaPlatformManager.
     * @param l the listener, can not be null
     */
    public void removePropertyChangeListener (PropertyChangeListener l) {
        assert l != null : "Listener can not be null";  //NOI18N
        pcs.removePropertyChangeListener(l);
    }

    private void firePropertyChange (String property) {
        pcs.firePropertyChange(property, null, null);
    }

    private static boolean compatible (Specification platformSpec, Specification query) {
        String name = query.getName();
        SpecificationVersion version = query.getVersion();
        return ((name == null || name.equalsIgnoreCase (platformSpec.getName())) &&
            (version == null || version.equals (platformSpec.getVersion())) &&
            compatibleProfiles (platformSpec.getProfiles(), query.getProfiles()));
    }

    private static boolean compatibleProfiles (Profile[] platformProfiles, Profile[] query) {
        if (query == null) {
            return true;
        }
        else if (platformProfiles == null) {
            return false;
        }
        else {
            Collection<Profile> covered = new HashSet<Profile>();
            for (Profile pattern : query) {
                boolean found = false;
                for (Profile p : platformProfiles) {
                    if (compatibleProfile(p, pattern)) {
                        found = true;
                        covered.add(p);
                    }
                }
                if (!found) {
                    return false;
                }
            }
            return covered.size() == platformProfiles.length;
        }
    }

    private static boolean compatibleProfile (Profile platformProfile, Profile query) {
        String name = query.getName();
        SpecificationVersion version = query.getVersion();
        return ((name == null || name.equals (platformProfile.getName())) &&
               (version == null || version.equals (platformProfile.getVersion())));
    }

    private synchronized Collection<? extends ScalaPlatformProvider> getProviders() {
        if (!this.providersValid) {
            if (this.providers == null) {
                this.providers = Lookup.getDefault().lookupResult(ScalaPlatformProvider.class);
                this.providers.addLookupListener (new LookupListener () {
                    public void resultChanged(LookupEvent ev) {
                        resetCache (true);
                        ScalaPlatformManager.this.firePropertyChange(PROP_INSTALLED_PLATFORMS);
                    }
                });
            }
            if (this.pListener == null ) {
                this.pListener = new PropertyChangeListener() {
                    public void propertyChange(PropertyChangeEvent evt) {
                        ScalaPlatformManager.this.resetCache (false);
                        ScalaPlatformManager.this.firePropertyChange(PROP_INSTALLED_PLATFORMS);
                    }
                };
            }
            Collection<? extends ScalaPlatformProvider> instances = this.providers.allInstances();
            Collection<ScalaPlatformProvider> toAdd = new HashSet<ScalaPlatformProvider>(instances);
            toAdd.removeAll (this.lastProviders);
            Collection<ScalaPlatformProvider> toRemove = new HashSet<ScalaPlatformProvider>(this.lastProviders);
            toRemove.removeAll (instances);
            for (ScalaPlatformProvider provider : toRemove) {
                provider.removePropertyChangeListener (pListener);
            }
            for (ScalaPlatformProvider provider : toAdd) {
                provider.addPropertyChangeListener (pListener);
            }
            this.lastProviders = instances;
            providersValid = true;
        }
        return this.lastProviders;
    }


    private synchronized void resetCache (boolean resetProviders) {
        ScalaPlatformManager.this.cachedPlatforms = null;
        this.providersValid &= !resetProviders;
    }

}
