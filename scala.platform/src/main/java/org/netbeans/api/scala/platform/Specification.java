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

import org.openide.modules.SpecificationVersion;

/** Specification of the Java SDK
 */
public final class Specification {

    private String name;
    private SpecificationVersion version;
    private Profile[] profiles;


    /**
     * Creates new SDK Specification
     * @param name of the specification e.g J2SE
     * @param version of the specification e.g. 1.4
     */
    public Specification (String name, SpecificationVersion version) {
        this (name, version, null);
    }

    /**
     * Creates new SDK Specification
     * @param name of the specification e.g J2SE
     * @param version of the specification e.g. 1.4
     * @param profiles of the Java SDK
     */
    public Specification (String name, SpecificationVersion version, Profile[] profiles) {
        this.name = name;
        this.version = version;
        this.profiles = profiles;
    }

    /**
     * Returns the name of the Java specification e.g. J2SE
     * @return String
     */
    public final String getName () {
        return this.name;
    }

    /**
     * Returns the version of the Java specification e.g 1.4
     * @return instance of SpecificationVersion
     */
    public final SpecificationVersion getVersion () {
        return this.version;
    }

    /**
     * Returns profiles supported by the Java platform.
     * @return list of profiles, or null if not applicable
     */
    public final Profile[] getProfiles () {
        return this.profiles;
    }

    public int hashCode () {
        int hc = 0;
        if (this.name != null)
            hc = this.name.hashCode() << 16;
        if (this.version != null)
            hc += this.version.hashCode();
        return hc;
    }

    public boolean equals (Object other) {
        if (other instanceof Specification) {
            Specification os = (Specification) other;
            boolean re = this.name == null ? os.name == null : this.name.equals(os.name) &&
                         this.version == null ? os.version == null : this.version.equals (os.version);
            if (!re || this.profiles == null)
                return re;
            if (os.profiles == null || this.profiles.length != os.profiles.length)
                return false;
            for (int i=0; i<os.profiles.length; i++)
                re &= this.profiles[i].equals(os.profiles[i]);
            return re;
        }
        else
            return false;
    }

    public String toString () {
        String str = this.name == null ? "" : this.name + " "; // NOI18N
        str += this.version == null ? "" : this.version + " "; // NOI18N
        if (this.profiles != null) {
            str+="["; // NOI18N
            for (int i = 0; i < profiles.length; i++) {
                str+= profiles[i]+ " "; // NOI18N
            }
            str+="]"; // NOI18N
        }
        return str;
    }

}
