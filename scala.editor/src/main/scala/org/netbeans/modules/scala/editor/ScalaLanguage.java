/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2010 Sun Microsystems, Inc. All rights reserved.
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
 * Portions Copyrighted 2010 Sun Microsystems, Inc.
 */
package org.netbeans.modules.scala.editor;

import java.util.Set;
import org.netbeans.api.lexer.Language;
import org.netbeans.modules.csl.api.CodeCompletionHandler;
import org.netbeans.modules.csl.api.DeclarationFinder;
import org.netbeans.modules.csl.api.Formatter;
import org.netbeans.modules.csl.api.HintsProvider;
import org.netbeans.modules.csl.api.InstantRenamer;
import org.netbeans.modules.csl.api.KeystrokeHandler;
import org.netbeans.modules.csl.api.OccurrencesFinder;
import org.netbeans.modules.csl.api.SemanticAnalyzer;
import org.netbeans.modules.csl.api.StructureScanner;
import org.netbeans.modules.csl.spi.DefaultLanguageConfig;
import org.netbeans.modules.parsing.spi.Parser;
import org.netbeans.modules.scala.core.ScalaParser;
import org.netbeans.modules.scala.core.lexer.ScalaTokenId;
import org.netbeans.modules.scala.hints.ScalaHintsProvider;

/**
 * Language/lexing configuration for Scala
 *
 *
 * @Note: Since NetBeans 6.9, csl uses LanguageRegistration, and process source files to auto-generate layer,
 *        What I have to do is either use manually register all relative instance in layer.xml or write it in Java.
 *        @see org.netbeans.modules.csl.core.LanguageRegistrationProcessor
 * @see https://netbeans.org/bugzilla/show_bug.cgi?id=169991
 * What LanguageRegistrationProcessor created is under build/classes/META-INF/generated-layer.xml
 * 
 * @author Caoyuan Deng
 */
@org.netbeans.modules.csl.spi.LanguageRegistration(mimeType = {"text/x-scala"})
@org.netbeans.modules.parsing.spi.indexing.PathRecognizerRegistration(mimeTypes = {"text/x-scala"},
sourcePathIds = {"scala/classpath/source"},
libraryPathIds = {"scala/classpath/boot"},
binaryLibraryPathIds = {})
public class ScalaLanguage extends DefaultLanguageConfig {

  public static String BOOT = "scala/classpath/boot";
  public static String COMPILE = "scala/classpath/compile";
  public static String EXECUTE = "scala/classpath/execute";
  public static String SOURCE = "scala/classpath/source";

  public ScalaLanguage() {
  }

  public Language getLexerLanguage() {
    return ScalaTokenId.language();
  }

  @Override
  public String getLineCommentPrefix() {
    return "//"; // NOI18N
  }

  @Override
  public String getDisplayName() {
    return "Scala"; // NOI18N
  }

  @Override
  public String getPreferredExtension() {
    return "scala"; // NOI18N
  }

  /**
   * @see org.netbeans.modules.scala.platform.ScalaPlatformClassPathProvider and ModuleInstall
   */
  @Override
  public Set<String> getLibraryPathIds() {
    return java.util.Collections.singleton(BOOT);
  }

  @Override
  public Set<String> getSourcePathIds() {
    return java.util.Collections.singleton(SOURCE);
  }

  @Override
  public Parser getParser() {
    return new ScalaParser();
  }

  @Override
  public SemanticAnalyzer getSemanticAnalyzer() {
    return new ScalaSemanticAnalyzer();
  }

  @Override
  public boolean hasOccurrencesFinder() {
    return true;
  }

  @Override
  public OccurrencesFinder getOccurrencesFinder() {
    return new ScalaOccurrencesFinder();
  }

  @Override
  public boolean hasStructureScanner() {
    return true;
  }

  @Override
  public StructureScanner getStructureScanner() {
    return new ScalaStructureAnalyzer();
  }

  @Override
  public DeclarationFinder getDeclarationFinder() {
    return new ScalaDeclarationFinder();
  }

  @Override
  public InstantRenamer getInstantRenamer() {
    return new ScalaInstantRenamer();
  }

  @Override
  public CodeCompletionHandler getCompletionHandler() {
    return new ScalaCodeCompletionHandler();
  }

  @Override
  public KeystrokeHandler getKeystrokeHandler() {
    return new ScalaKeystrokeHandler();
  }

  @Override
  public boolean hasFormatter() {
    return true;
  }

  @Override
  public Formatter getFormatter() {
    return new ScalaFormatter();
  }

  @Override
  public boolean hasHintsProvider() {
    return true;
  }

  // hintsProvider is registered in layer.xml under "csl-hints" folder
  @Override
  public HintsProvider getHintsProvider() {
    return new ScalaHintsProvider();
  }
  //@Override def getIndexerFactory = new ScalaIndexer.Factory
}
