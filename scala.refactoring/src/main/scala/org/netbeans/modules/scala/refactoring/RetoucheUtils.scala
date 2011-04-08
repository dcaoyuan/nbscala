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
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2007 Sun
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
package org.netbeans.modules.scala.refactoring

import java.awt.Color;
import java.io.CharConversionException;
import java.io.File
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.net.URL
import java.net.URLDecoder
import java.util.StringTokenizer
import java.util.logging.Logger
import javax.swing.text.AttributeSet;
import javax.swing.text.StyleConstants;
import org.netbeans.api.editor.mimelookup.MimeLookup;
import org.netbeans.api.editor.mimelookup.MimePath;
import org.netbeans.api.editor.settings.FontColorSettings;
import org.netbeans.editor.BaseDocument;
import org.netbeans.api.java.classpath.ClassPath
import org.netbeans.api.java.classpath.GlobalPathRegistry
import org.netbeans.api.java.queries.SourceForBinaryQuery
import org.netbeans.api.java.source.ClasspathInfo
import org.netbeans.api.language.util.ast.{AstDfn, AstScope}
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenId;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.api.project.Sources;
import org.netbeans.api.project.ui.OpenProjects;
import org.netbeans.modules.csl.api.ElementKind
import org.netbeans.modules.scala.core.{ScalaMimeResolver, ScalaParserResult, ScalaSourceUtil}
import org.netbeans.modules.scala.core.ast.ScalaItems
import org.netbeans.modules.scala.core.lexer.ScalaTokenId
import org.netbeans.modules.parsing.spi.Parser;
import org.netbeans.modules.parsing.spi.indexing.support.QuerySupport;
import org.netbeans.spi.java.classpath.support.ClassPathSupport
import org.openide.filesystems.FileObject;
import org.openide.util.Lookup;
import org.openide.filesystems.FileUtil;
import org.openide.filesystems.URLMapper
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.util.Exceptions;
import org.openide.xml.XMLUtil;
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashSet

// * @Note CloneableEditorSupport cause highlighting disappear
import org.openide.text.CloneableEditorSupport

/**
 * Various utilies related to Scala refactoring; the generic ones are based
 * on the ones from the Java refactoring module.
 *
 * @author Jan Becicka
 * @author Tor Norbye
 */
object RetoucheUtils {

  val Log = Logger.getLogger(this.getClass.getName)
  lazy val nullPath = ClassPathSupport.createClassPath(Array[FileObject](null): _*)

  def isScalaFile(fo: FileObject): Boolean = {
    fo.getMIMEType == "text/x-scala"
  }

  def getDocument(pr: Parser.Result): BaseDocument = {
    if (pr != null) {
      pr.getSnapshot.getSource.getDocument(true).asInstanceOf[BaseDocument]
    } else null
  }


  /** Compute the names (full and simple, e.g. Foo::Bar and Bar) for the given node, if any, and return as
   * a String[2] = {name,simpleName} */
  /* def String[] getNodeNames(Node node) {
   String name = null;
   String simpleName = null;
   int type = node.getType();
   if (type == org.mozilla.nb.javascript.Token.CALL || type == org.mozilla.nb.javascript.Token.NEW) {
   name = AstUtilities.getCallName(node, true);
   simpleName = AstUtilities.getCallName(node, false);
   } else if (node instanceof Node.StringNode) {
   name = node.getString();
   } else if (node.getType() == org.mozilla.nb.javascript.Token.FUNCTION) {
   name = AstUtilities.getFunctionFqn(node, null);
   if (name != null && name.indexOf('.') != -1) {
   name = name.substring(name.indexOf('.')+1);
   }
   } else {
   return new String[] { null, null};
   }
   // TODO - FUNCTION - also get full name!

   if (simpleName == null) {
   simpleName = name;
   }

   return new String[] { name, simpleName };
   } */

  def findCloneableEditorSupport(pr: Parser.Result): CloneableEditorSupport = {
    try {
      val dob = DataObject.find(pr.getSnapshot.getSource.getFileObject)
      findCloneableEditorSupport(dob)
    } catch {case ex: DataObjectNotFoundException => Exceptions.printStackTrace(ex); null}
  }

  def findCloneableEditorSupport(dob: DataObject): CloneableEditorSupport = {
    dob.getCookie(classOf[org.openide.cookies.OpenCookie]) match {
      case x: CloneableEditorSupport => x
      case _ => dob.getCookie(classOf[org.openide.cookies.EditorCookie]) match {
          case x: CloneableEditorSupport => x
          case _ => null
        }
    }
  }

  def htmlize(input: String): String = {
    try {
      XMLUtil.toElementContent(input)
    } catch {case cce: CharConversionException => Exceptions.printStackTrace(cce); input}
  }

//    /** Return the most distant method in the hierarchy that is overriding the given method, or null */
//    def IndexedMethod getOverridingMethod(JsElementCtx element, CompilationInfo info) {
//        JsIndex index = JsIndex.get(info.getIndex());
//        String fqn = AstUtilities.getFqnName(element.getPath());
//
//        return index.getOverridingMethod(fqn, element.getName());
//    }

  def getHtml(text: String): String = {
    val sb = new StringBuilder
    // TODO - check whether we need Js highlighting or rhtml highlighting
    val th = TokenHierarchy.create(text, ScalaTokenId.language)
    val lookup = MimeLookup.getLookup(MimePath.get(ScalaMimeResolver.MIME_TYPE))
    val settings = lookup.lookup(classOf[FontColorSettings])
    val ts = th.tokenSequence.asInstanceOf[TokenSequence[TokenId]]
    while (ts.moveNext) {
      val token = ts.token
      var category = token.id.name
      var set = settings.getTokenFontColors(category) match {
        case null =>
          category = token.id.primaryCategory match {
            case null => "whitespace" //NOI18N
            case x => x
          }
          settings.getTokenFontColors(category)
        case x => x
      }
      val tokenText = htmlize(token.text.toString)
      sb.append(color(tokenText, set))
    }
    sb.toString
  }

  private def color(string: String, set: AttributeSet): String = {
    if (set == null) {
      return string
    }
    if (string.trim.length == 0) {
      return string.replace(" ", "&nbsp;").replace("\n", "<br>") //NOI18N
    }
    val sb = new StringBuilder(string)
    if (StyleConstants.isBold(set)) {
      sb.insert(0, "<b>") //NOI18N
      sb.append("</b>")   //NOI18N
    }
    if (StyleConstants.isItalic(set)) {
      sb.insert(0, "<i>") //NOI18N
      sb.append("</i>")   //NOI18N
    }
    if (StyleConstants.isStrikeThrough(set)) {
      sb.insert(0, "<s>") //NOI18N
      sb.append("</s>")   //NOI18N
    }
    sb.insert(0, "<font color=" + getHTMLColor(StyleConstants.getForeground(set)) + ">") //NOI18N
    sb.append("</font>")  //NOI18N

    sb.toString
  }

  private def getHTMLColor(c: Color): String = {
    var colorR = "0" + Integer.toHexString(c.getRed)   //NOI18N
    colorR = colorR.substring(colorR.length - 2)

    var colorG = "0" + Integer.toHexString(c.getGreen) //NOI18N
    colorG = colorG.substring(colorG.length - 2)

    var colorB = "0" + Integer.toHexString(c.getBlue)  //NOI18N
    colorB = colorB.substring(colorB.length - 2)

    "#" + colorR + colorG + colorB //NOI18N
  }

  def isElementInOpenProject(f: FileObject): Boolean = {
    if (f == null) return false
    
    val p = FileOwnerQuery.getOwner(f)
    OpenProjects.getDefault.isProjectOpen(p)
  }


  def isFileInOpenProject(file: FileObject): Boolean = {
    assert(file != null)
    val p = FileOwnerQuery.getOwner(file)
    OpenProjects.getDefault.isProjectOpen(p)
  }

  def isValidPackageName(name: String): Boolean = {
    if (name.endsWith(".")) //NOI18N
      return false
    if (name.startsWith("."))  //NOI18N
      return false
    val tokenizer = new StringTokenizer(name, ".") // NOI18N
    while (tokenizer.hasMoreTokens) {
      /* @todo if (!ScalaSourceUtil.isScalaIdentifier(tokenizer.nextToken)) {
        return false
      } */
    }
    true
  }



  def isOnSourceClasspath(fo: FileObject): Boolean = {
    val p = FileOwnerQuery.getOwner(fo)
    if (p == null) {
      return false
    }
    val opened = OpenProjects.getDefault.getOpenProjects
    for (i <- 0 until opened.length) {
      if (p.equals(opened(i)) || opened(i).equals(p)) {
        val sgs = ProjectUtils.getSources(p).getSourceGroups(Sources.TYPE_GENERIC)
        for (j <- 0 until sgs.length) {
          if (fo == sgs(j).getRootFolder) {
            return true
          }
          if (FileUtil.isParentOf(sgs(j).getRootFolder, fo)) {
            return true
          }
        }
        return false
      }
    }
    false
  }

  def isRefactorable(file: FileObject): Boolean = {
    isScalaFile(file) && isFileInOpenProject(file) && isOnSourceClasspath(file)
  }

  /* def getPackageName(folder: FileObject): String = {
    assert(folder.isFolder, "argument must be folder")
    val cp = ClassPath.getClassPath(folder, ClassPath.SOURCE)
    if (cp == null) {
      // see http://www.netbeans.org/issues/show_bug.cgi?id=159228
      throw new IllegalStateException(String.format("No classpath for %s.", folder)) // NOI18N
    }
    cp.getResourceName(folder, '.', false)
  } */

  /* def getPackageName(CompilationUnitTree unit): String = {
    assert unit!=null;
    ExpressionTree name = unit.getPackageName();
    if (name==null) {
      //default package
      return "";
    }
    return name.toString();
  } */

  def getPackageName(url: URL): String = {
    var f =  try {
      val path = URLDecoder.decode(url.getPath, "utf-8") // NOI18N
      FileUtil.normalizeFile(new File(path))
    } catch {case ex: UnsupportedEncodingException => throw new IllegalArgumentException("Cannot create package name for url " + url)} // NOI18N

    var suffix = ""
    do {
      val fo = FileUtil.toFileObject(f)
      if (fo != null) {
        if (suffix == "") return getPackageName(fo)

        val prefix = getPackageName(fo)
        return prefix + (if (prefix == "") "" else ".") + suffix // NOI18N
      }
      if (suffix != "") {
        suffix = "." + suffix // NOI18N
      }
      try {
        suffix = URLDecoder.decode(f.getPath().substring(f.getPath().lastIndexOf(File.separatorChar) + 1), "utf-8") + suffix; // NOI18N
      } catch {case ex: UnsupportedEncodingException => throw new IllegalArgumentException("Cannot create package name for url " + url)} // NOI18N

      f = f.getParentFile
    } while (f !=null )
    throw new IllegalArgumentException("Cannot create package name for url " + url) // NOI18N
  }


// XXX: parsingapi
//    def boolean isClasspathRoot(FileObject fo) {
//        ClassPath cp = ClassPath.getClassPath(fo, ClassPath.SOURCE);
//        if (cp != null) {
//            FileObject f = cp.findOwnerRoot(fo);
//            if (f != null) {
//                return fo.equals(f);
//            }
//        }
//
//        return false;
//    }

  def getPackageName(folder: FileObject): String = {
    assert(folder.isFolder, "argument must be folder") //NOI18N
    val p = FileOwnerQuery.getOwner(folder)
    if (p != null) {
      val s = ProjectUtils.getSources(p)
      for {g <- s.getSourceGroups(Sources.TYPE_GENERIC)
           relativePath = FileUtil.getRelativePath(g.getRootFolder, folder)
           if relativePath != null
      } {
        return relativePath.replace('/', '.') //NOI18N
      }
    }
    
    ""
  }

  @throws(classOf[IOException])
  def getClassPathRoot(url: URL): FileObject = {
    var result = URLMapper.findFileObject(url);
    var f = if (result != null) null else FileUtil.normalizeFile(new File(URLDecoder.decode(url.getPath, "UTF-8"))) //NOI18N
    while (result == null) {
      result = FileUtil.toFileObject(f)
      f = f.getParentFile
    }
    ClassPath.getClassPath(result, ClassPath.SOURCE).findOwnerRoot(result)
  }

  def getClasspathInfoFor(handles: Array[ScalaItems#ScalaItem]): ClasspathInfo = {
    var result = new Array[FileObject](handles.length)
    var i = 0
    for (handle <- handles) {
      val fo = handle.fo
      if (i == 0 && fo == None) {
        result = new Array[FileObject](handles.length + 1)
        result(i) = null
        i += 1
      }
      result(i) = fo.get
      i += 1
    }

    getClasspathInfoFor(result)
  }

  def getClasspathInfoFor(files: Array[FileObject], dependencies: Boolean = true, backSource: Boolean = false): ClasspathInfo = {
    assert(files.length > 0)
    val dependentRoots = new HashSet[URL]
    for (fo <- files) {
      var p: Project = null
      var ownerRoot: FileObject = null
      if (fo != null) {
        p = FileOwnerQuery.getOwner(fo)
        val cp = ClassPath.getClassPath(fo, ClassPath.SOURCE)
        if (cp != null) {
          ownerRoot = cp.findOwnerRoot(fo)
        }
      }
      if (p != null && ownerRoot != null) {
        val sourceRoot = URLMapper.findURL(ownerRoot, URLMapper.INTERNAL)
        if (dependencies) {
          dependentRoots ++= ScalaSourceUtil.getDependentRoots(sourceRoot)
        } else {
          dependentRoots += sourceRoot
        }

        val sgs = ScalaSourceUtil.getScalaJavaSourceGroups(p)
        dependentRoots ++= sgs.map(root => URLMapper.findURL(root.getRootFolder, URLMapper.INTERNAL))
      } else {
        val srcCps = GlobalPathRegistry.getDefault.getPaths(ClassPath.SOURCE).iterator
        while (srcCps.hasNext) {
          dependentRoots ++= srcCps.next.getRoots.map(URLMapper.findURL(_, URLMapper.INTERNAL))
        }
      }
    }

    if (backSource) {
      for (fo <- files if fo != null) {
        val compCp = ClassPath.getClassPath(fo, ClassPath.COMPILE)
        val entries = compCp.entries.iterator
        while (entries.hasNext) {
          val root = entries.next
          val r = SourceForBinaryQuery.findSourceRoots(root.getURL)
          dependentRoots ++= r.getRoots.map(URLMapper.findURL(_, URLMapper.INTERNAL))
        }
      }
    }

    val srcCp = ClassPathSupport.createClassPath(dependentRoots.toArray: _*)
    val bootCp = if (files(0) != null) ClassPath.getClassPath(files(0), ClassPath.BOOT) else nullPath
    var compCp = if (files(0) != null) ClassPath.getClassPath(files(0), ClassPath.COMPILE) else nullPath
    if (compCp == null) {
      // * when file(0) is a class file, there is no compile cp but execute cp, try to get it
      compCp = ClassPath.getClassPath(files(0), ClassPath.EXECUTE)
    }
    // * if no cp found at all log the file and use nullPath since the ClasspathInfo.create
    // * doesn't accept null compile or boot cp.
    if (compCp == null) {
      Log.warning ("No classpath for: " + FileUtil.getFileDisplayName(files(0)) + " " + FileOwnerQuery.getOwner(files(0)))
      compCp = nullPath
    }

    ClasspathInfo.create(bootCp, compCp, srcCp)
  }

  def getScalaFilesInSrcCp(srcCp: ClassPath, excludeReadOnlySourceRoots: Boolean = false): Set[FileObject] = {
    val files = new HashSet[FileObject] // 100
    val sourceRoots = srcCp.getRoots
    val itr = sourceRoots.iterator
    while (itr.hasNext) {
      val root = itr.next
      if (excludeReadOnlySourceRoots && !root.canWrite) {
        // skip read only source roots
      } else {
        val name = root.getName match {
          case "vendor"| "script" => // NOI18N
            // skip non-refactorable parts in renaming
          case _ => addScalaFiles(files, root)
        }
      }
    }

    files.toSet
  }


  def getScalaFilesInProject(fileInProject: FileObject, excludeReadOnlySourceRoots: Boolean = false): Set[FileObject] = {
    val files = new HashSet[FileObject] // 100
    val sourceRoots = QuerySupport.findRoots(fileInProject,
                                             null,
                                             java.util.Collections.singleton(ClassPath.BOOT),
                                             java.util.Collections.emptySet[String])
    val itr = sourceRoots.iterator
    while (itr.hasNext) {
      val root = itr.next
      if (excludeReadOnlySourceRoots && !root.canWrite) {
        // skip read only source roots
      } else {
        val name = root.getName match {
          case "vendor"| "script" => // NOI18N
            // skip non-refactorable parts in renaming
          case _ => addScalaFiles(files, root)
        }
      }
    }

    files.toSet
  }

  private def addScalaFiles(files: HashSet[FileObject], f: FileObject) {
    if (f.isFolder) {
      f.getChildren foreach {addScalaFiles(files, _)}
    } else if (isScalaFile(f)) {
      files.add(f)
    }
  }

  def getTopTemplates(scopes: Seq[AstScope], result: ArrayBuffer[AstDfn]) {
    for (scope <- scopes) {
      result ++= (scope.dfns filter {_.kind match {
            case ElementKind.CLASS | ElementKind.MODULE => true
            case _ => false
          }
        })

      for (x <- scope.bindingDfn if x.kind == ElementKind.PACKAGE) {
        getTopTemplates(scope.subScopes, result)
      }
    }
  }

}
