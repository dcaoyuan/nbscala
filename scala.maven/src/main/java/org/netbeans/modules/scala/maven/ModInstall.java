/* ==========================================================================
 * Copyright 2007 Mevenide Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * =========================================================================
 */

package org.netbeans.modules.scala.maven;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Set;
import org.openide.modules.ModuleInstall;

/**
 * hack!! remove when http://www.netbeans.org/issues/show_bug.cgi?id=89019
 * is fixed.
 * @author mkleint
 */
public class ModInstall extends ModuleInstall {
    
    /** Creates a new instance of ModInstall */
    public ModInstall() {
    }
    
    /**
     * screw friend dependency.
     */ 
    @Override
    public void validate() throws IllegalStateException {
        try {
            java.lang.Class main = java.lang.Class.forName("org.netbeans.core.startup.Main", false,  //NOI18N
                    Thread.currentThread().getContextClassLoader());
            Method meth = main.getMethod("getModuleSystem", new Class[0]); //NOI18N
            Object moduleSystem = meth.invoke(null, new Object[0]);
            meth = moduleSystem.getClass().getMethod("getManager", new Class[0]); //NOI18N
            Object mm = meth.invoke(moduleSystem, new Object[0]);
            Method moduleMeth = mm.getClass().getMethod("get", new Class[] {String.class}); //NOI18N
            Object mavenModule = moduleMeth.invoke(mm, "org.netbeans.modules.maven"); //NOI18N
            if (mavenModule != null) {
                Method dataMethod = mavenModule.getClass().getSuperclass().getDeclaredMethod("data");
                dataMethod.setAccessible(true);
                Object data = dataMethod.invoke(mavenModule);
                Field frField = data.getClass().getSuperclass().getDeclaredField("friendNames");
                frField.setAccessible(true);
                Set friends = (Set)frField.get(data);
                friends.add("org.netbeans.modules.scala.maven"); //NOI18N
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new IllegalStateException("Cannot fix dependencies for org.netbeans.modules.scala.maven."); //NOI18N
        }
    }
    
    
    
}
