/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2009 Sun Microsystems, Inc. All rights reserved.
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
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2009 Sun Microsystems, Inc.
 */

package org.netbeans.modules.scala.editor


import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.`type`.DeclaredType;
import javax.lang.model.`type`.ExecutableType;
import javax.lang.model.`type`.TypeKind;
import javax.lang.model.`type`.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import org.netbeans.api.java.source.ClassIndex;
import org.netbeans.api.java.source.ClassIndex.NameKind;
import org.netbeans.api.java.source.ClassIndex.SearchScope;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.modules.java.source.JavaSourceAccessor;
import org.netbeans.modules.scala.core.ScalaParserResult
import org.netbeans.modules.scala.core.ScalaSourceUtil
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileStateInvalidException;
import org.openide.util.Exceptions;
import org.netbeans.modules.csl.spi.ParserResult

import scala.collection.JavaConversions._


/**
 *
 * @author Caoyuan Deng
 */
object JavaIndex {

  def get(fo: FileObject, pResult: ScalaParserResult): Option[JavaIndex] = {
    ScalaSourceUtil.getClasspathInfo(fo) match {
      case Some(cpInfo) => cpInfo.getClassIndex match {
          case null => None
          case index => Some(new JavaIndex(index, pResult))
        }
      case None => None
    }
  }
}

class JavaIndex(index: ClassIndex, pResult: ScalaParserResult) {
  //public static final Map<String, List<? extends Element>> TypeQNameToMemebersCache = new HashMap<String, List<? extends Element>>();
  //public static final Set<SearchScope> ALL_SCOPE = EnumSet.allOf(SearchScope.class);
  //public static final Set<SearchScope> SOURCE_SCOPE = EnumSet.of(SearchScope.SOURCE);

  def getPackages(fqnPrefix: String): Set[String] = {
    val pkgNames = index.getPackageNames(fqnPrefix, true, java.util.EnumSet.allOf(classOf[SearchScope]))
    pkgNames.map{x => x}.toSet
  }

}
