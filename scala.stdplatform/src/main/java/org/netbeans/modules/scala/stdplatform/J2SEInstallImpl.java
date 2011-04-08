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

package org.netbeans.modules.scala.stdplatform;

import org.openide.filesystems.*;
import org.openide.util.NbBundle;
import org.openide.ErrorManager;
import org.netbeans.modules.scala.stdplatform.wizard.J2SEWizardIterator;
import org.netbeans.modules.scala.stdplatform.platformdefinition.Util;

import java.io.IOException;
import java.util.Collections;

import org.netbeans.api.scala.platform.ScalaPlatform;
import org.openide.WizardDescriptor;

/**
 * Installer factory for standard J2SE Platforms
 *
 * @author Svatopluk Dedic
 */
class J2SEInstallImpl extends org.netbeans.spi.scala.platform.PlatformInstall {

    J2SEInstallImpl() {
    }
    
    static J2SEInstallImpl create() {        
        return new J2SEInstallImpl();
    }
    
    /**
     * Performs a quick & dirty check whether there's a JRE installed.
     * The method looks into the folder for something, which - depending on 
     * the platform's conventions - has name "java.exe" or "java"
     */
    public boolean accept(FileObject dir) {
        if (!dir.isFolder()) {
            return false;
        }
        FileObject tool = Util.findTool("scala", Collections.singleton(dir));    //NOI18N
        if (tool == null) {
            return false;
        }
        tool = Util.findTool("scalac", Collections.singleton(dir));  //NOI18N
        return tool != null;
    }
    
    public WizardDescriptor.InstantiatingIterator<WizardDescriptor> createIterator(FileObject baseFolder) {
        try {
            return new J2SEWizardIterator(baseFolder);
        } catch (IOException ioe) {
            ErrorManager.getDefault().notify (ioe);
            return null;
        }
    }

    public String getDisplayName() {
        return NbBundle.getMessage(J2SEInstallImpl.class,"TXT_J2SEPlatform");
    }
}
