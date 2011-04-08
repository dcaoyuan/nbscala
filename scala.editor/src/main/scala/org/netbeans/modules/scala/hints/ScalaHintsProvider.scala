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

package org.netbeans.modules.scala.hints

import org.netbeans.modules.scala.core.ast.ScalaRootScope
import scala.collection.JavaConversions
import scala.collection.JavaConversions._

import java.{ lang => jl, util => ju }
import org.netbeans.modules.csl.api.HintsProvider.HintsManager;
import org.netbeans.modules.csl.api._;
import org.netbeans.modules.csl.spi.ParserResult;
import org.netbeans.modules.scala.core.ScalaParserResult
import org.netbeans.api.language.util.ast.AstScope
import scala.collection.mutable.ListBuffer

class ScalaHintsProvider() extends HintsProvider {

    var cancelled = false;

    /**
     * Compute hints applicable to the given compilation info and add to the given result list.
     */
    def computeHints(manager : HintsManager, context : RuleContext, hints : java.util.List[Hint]) : Unit = {
      val parserResult = context.parserResult;
      if (parserResult != null) {
          val scalaParserResult = parserResult.asInstanceOf[ScalaParserResult]
          val rootScope = scalaParserResult.rootScope
          val hintRules  = manager.getHints.asInstanceOf[ju.Map[_, ju.List[ScalaAstRule]]]
          if (!hintRules.isEmpty && !cancelled) {
            try {
              //context.doc.readLock();
              hints.addAll(applyHintRules(manager, context.asInstanceOf[ScalaRuleContext], hintRules.get(ScalaAstRule.ROOT), rootScope))
            } finally {
              //context.doc.readUnlock();
            }

        }
      }
    }

    /**
     * Compute any suggestions applicable to the given caret offset, and add to
     * the given suggestion list.
     */
    def computeSuggestions(manager : HintsManager, context : RuleContext, suggestions : java.util.List[Hint], caretOffset : Int) : Unit = {

    }

    /**
     * Compute any suggestions applicable to the given caret offset, and add to
     * the given suggestion list.
     */
    def computeSelectionHints(manager : HintsManager, context : RuleContext, suggestions : ju.List[Hint], start : Int, end : Int) : Unit = {
      //println("compute selections")
      cancelled = false
      val parserResult = context.parserResult;
      if (parserResult != null) {
        val selHints  = manager.getSelectionHints.asInstanceOf[ju.List[ScalaSelectionRule]]
        if (!selHints.isEmpty && !cancelled) {
          try {
            //context.doc.readLock();
            suggestions.addAll(applySelectionRules(manager, context.asInstanceOf[ScalaRuleContext], selHints, start, end))
          } finally {
            //context.doc.readUnlock();
          }
        }
      }
  }

    /**
     * Process the errors for the given compilation info, and add errors and
     * warning descriptions into the provided hint list. Return any errors
     * that were not added as error descriptions (e.g. had no applicable error rule)
     */
    def computeErrors(manager : HintsManager, context : RuleContext, hints : ju.List[Hint], unhandled : ju.List[Error]) : Unit = {
        //println("compute errors")
        cancelled = false
        val parserResult = context.parserResult;
        if (parserResult != null) {
            val errors = JavaConversions.asBuffer(parserResult.getDiagnostics);
            if (errors != null && !errors.isEmpty) {
                val errHints  = manager.getErrors.asInstanceOf[ju.Map[String, ju.List[ScalaErrorRule]]]

                if (errHints.isEmpty || cancelled) {
                    unhandled.addAll(errors)
                } else {

                    try {
                        //context.doc.readLock();
                        unhandled.addAll(errors.filter(x => !applyRules(x, manager, context.asInstanceOf[ScalaRuleContext], errHints, hints)))
                    } finally {
                        //context.doc.readUnlock();
                    }
                }
            }
        }
    }

   def applyRules(error : Error, manager : HintsManager, context : ScalaRuleContext,  errRules : ju.Map[String, ju.List[ScalaErrorRule]], result : ju.List[Hint]) : Boolean = {
        val code = error.getKey
        //println("code=" + code)
        val rules = errRules.get(code)
        if (rules != null) {
           var added = List[Hint]()
           val applicableRules = for {
               rule <- JavaConversions.asBuffer(rules.asInstanceOf[ju.List[ScalaErrorRule]])
               if rule.appliesTo(context)
           } yield rule
           for (rule <- applicableRules) {
               added ++= rule.createHints(context, error)
           }
           result.addAll(added)
           added.size > 0
        } else {
            false
        }
    }

   def applySelectionRules(manager : HintsManager, context : ScalaRuleContext,  selRules : ju.List[ScalaSelectionRule], start : Int, end : Int) : List[Hint] = {
       val added = ListBuffer[Hint]()
       val applicableRules = for {
           rule <- selRules
           if rule.appliesTo(context)
       } yield rule
       for (rule <- applicableRules) {
           added ++= rule.createHints(context, start, end)
       }
       added.toList
    }

   def applyHintRules(manager : HintsManager, context : ScalaRuleContext,  selRules : ju.List[ScalaAstRule], scope : ScalaRootScope) : List[Hint] = {
       val added = ListBuffer[Hint]()
       val applicableRules = for {
           rule <- selRules
           if rule.appliesTo(context)
       } yield rule
       for (rule <- applicableRules) {
           added ++= rule.createHints(context, scope)
       }
       added.toList
    }


    /**
     * Cancel in-progress processing of hints.
     */
    def cancel() : Unit = {
        cancelled = true;
    }

    /**
     * <p>Optional builtin Rules. Typically you don't use this; you register your rules in your filesystem
     * layer in the gsf-hints/mimetype1/mimetype2 folder, for example gsf-hints/text/x-ruby/.
     * Error hints should go in the "errors" folder, selection hints should go in the "selection" folder,
     * and all other hints should go in the "hints" folder (but note that you can create localized folders
     * and organize them under hints; these categories are shown in the hints options panel.
     * Hints returned from this method will be placed in the "general" folder.
     * </p>
     * <p>
     * This method is primarily intended for rules that should be added dynamically, for example for
     * Rules that have a many different flavors yet a single implementation class (such as
     * JavaScript's StrictWarning rule which wraps a number of builtin parser warnings.)
     *
     * @return A list of rules that are builtin, or null or an empty list when there are no builtins
     */
    def getBuiltinRules() : java.util.List[Rule] = {
        java.util.Collections.emptyList[Rule]
    }


    /**
     * Create a RuleContext object specific to this HintsProvider. This lets implementations of
     * this interface created subclasses of the RuleContext that can be passed around to all
     * the executed rules.
     * @return A new instance of a RuleContext object
     */
    def createRuleContext() : RuleContext = {
        new ScalaRuleContext()
    }

}

