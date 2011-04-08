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

import org.netbeans.modules.csl.editor.semantic.GsfSemanticLayer
import org.netbeans.modules.csl.editor.semantic.SemanticHighlighter
import org.netbeans.spi.editor.highlighting.HighlightsLayer
import org.netbeans.spi.editor.highlighting.HighlightsLayerFactory;
import org.netbeans.spi.editor.highlighting.HighlightsLayerFactory.Context;
import org.netbeans.spi.editor.highlighting.ZOrder;

/**
 * This class is used to enable "Print to HTML..." @see org.netbeans.modules.editor.ExportHtmlAction#export
 * including semantic highlightings. The tracking stack is:
 *   ExportHtmlAction#export -> BaseDocument#print -> DrawEnfine#draw -> drawInternal -> initInfo -> ctx.editorUI.getDrawLayerList().currentLayers()
 *   -> HighlightingDrawLayer#init -> fakePane.putClientProperty("HighlightsLayerIncludes", new String [] { "^.*NonLexerSyntaxHighlighting$", "^.*SyntaxHighlighting$"})
 * Where, will filter layer according to fakePane's property "HighlightsLayerIncludes" in:
 *   HighlightingManager#getHighlights -> getContainer -> rebuildContainer -> paneFilter.filterLayers.
 * Where,
 *   paneFilter = new RegExpFilter(pane.getClientProperty(PROP_HL_INCLUDES), pane.getClientProperty(PROP_HL_EXCLUDES));
 *
 * That is, we need the layer has `layerTypeId` matching "^.*SyntaxHighlighting$"
 *
 * @author Caoyuan Deng
 */
class ScalaHighlightsLayerFactory extends HighlightsLayerFactory {

  def createLayers(context: Context): Array[HighlightsLayer] = {
    val semantic = GsfSemanticLayer.getLayer(classOf[SemanticHighlighter], context.getDocument)
    if (semantic != null) {
      semantic.clearColoringCache
      Array(
        HighlightsLayer.create(classOf[SemanticHighlighter].getName + "-SyntaxHighlighting", ZOrder.SYNTAX_RACK.forPosition(1001), false, semantic)
      )
    } else Array[HighlightsLayer]()
  }



}
