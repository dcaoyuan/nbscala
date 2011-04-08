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

package org.netbeans.spi.scala.platform;

import org.openide.WizardDescriptor;
import org.openide.filesystems.FileObject;

/**
 * Defines an API for registering custom Java platform installer. The Installer
 * is responsible for recognizing the platform, through its {@link #accept} method,
 * and for instantiation itself, through the provided wizard iterator.
 *
 * @author Svata Dedic, Tomas Zezula
 */
public abstract class PlatformInstall extends GeneralPlatformInstall {
    /**
     * XXX Javadoc for this method is completely inadequate. What does it do?
     * Determines whether the Recognizer recognizes a Java Platform in 
     * the passed folder. The check done by this method should be quick
     * and should not involve launching the virtual machine. The framework will
     * call a more detailed check later.
     * @return TemplateWizard.Iterator instance responsible for instantiating
     * the platform. The instantiate method of the returned iterator should
     * return the Set containing the created JavaPlatform.
     */
    public abstract WizardDescriptor.InstantiatingIterator<WizardDescriptor> createIterator(FileObject baseFolder);

    /**
     * Checks whether a given folder contains a platform of the supported type.
     * @param baseFolder folder which may be an installation root of a platform
     * @return true if the folder is recognized
     */
    public abstract boolean accept(FileObject baseFolder);    

}
