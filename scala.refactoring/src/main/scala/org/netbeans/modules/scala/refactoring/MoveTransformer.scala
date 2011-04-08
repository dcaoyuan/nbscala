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

package org.netbeans.modules.scala.refactoring


import com.sun.source.util.TreePath;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.api.java.source.WorkingCopy;
import org.netbeans.modules.refactoring.api.Problem;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;

/**
 *
 * @author Jan Becicka
 */
class MoveTransformer(move: MoveRefactoringPlugin) {//extends RefactoringVisitor {

/*     private var originalFolder: FileObject = _
    private Set<Element> elementsToImport;
    private Set<ImportTree> importToRemove;
    private var importToAdd: Set[String] = _
    private var isThisFileMoving = false
    private var isThisFileReferencingOldPackage = false;
    private Set<Element> elementsAlreadyImported;
    private var problem: Problem = _
    private var moveToDefaulPackageProblem = false;
    private var originalPackage: String = _
    private SourceUtilsEx.Cache cacheOfSrcFiles = new SourceUtilsEx.Cache();
    private val classes2Move = new HashSet[ElementHandle](move.classes.values)
*/

    private var problem: Problem = _
    
    def getProblem: Problem = {
        problem
    }

/*    @Override
    public void setWorkingCopy(WorkingCopy copy) throws ToPhaseException {
        super.setWorkingCopy(copy);
        originalFolder = workingCopy.getFileObject().getParent();
        originalPackage = RetoucheUtils.getPackageName(originalFolder);
        isThisFileMoving = move.filesToMove.contains(workingCopy.getFileObject());
        elementsToImport = new HashSet<Element>();
        isThisFileReferencingOldPackage = false;
        elementsAlreadyImported = new HashSet<Element>();
        importToRemove = new HashSet<ImportTree>();
        importToAdd = new HashSet<String>();
    }

    @Override
    public Tree visitMemberSelect(MemberSelectTree node, Element p) {
        if (!workingCopy.getTreeUtilities().isSynthetic(getCurrentPath())) {
            final Element el = workingCopy.getTrees().getElement(getCurrentPath());
            if (el != null) {
                if (isElementMoving(el)) {
                    elementsAlreadyImported.add(el);
                    String newPackageName = getTargetPackageName(el);

                    if (!"".equals(newPackageName)) { //
                        Tree nju = make.MemberSelect(make.Identifier(newPackageName), el);
                        rewrite(node, nju);
                    } else {
                        if (!moveToDefaulPackageProblem) {
                            problem = createProblem(problem, false, NbBundle.getMessage(MoveTransformer.class, "ERR_MovingClassToDefaultPackage"));
                            moveToDefaulPackageProblem = true;
                        }
                    }
                } else {
                    if (isThisFileMoving) {
                        if (el.getKind() != ElementKind.PACKAGE
                                && getPackageOf(el).toString().equals(originalPackage)
                                && !(el.getModifiers().contains(Modifier.PUBLIC) || el.getModifiers().contains(Modifier.PROTECTED))
                                && !move.filesToMove.contains(getFileObject(el))) {
                            problem = createProblem(problem, false, NbBundle.getMessage(MoveTransformer.class, "ERR_AccessesPackagePrivateFeature2",workingCopy.getFileObject().getName(),el, getTypeElement(el).getSimpleName()));
                        }
                    } else {
                        if (el.getKind()!=ElementKind.PACKAGE
                                && getPackageOf(el).toString().equals(originalPackage)
                                && !(el.getModifiers().contains(Modifier.PUBLIC) || el.getModifiers().contains(Modifier.PROTECTED))
                                && move.filesToMove.contains(getFileObject(el))) {
                            problem = createProblem(problem, false, NbBundle.getMessage(MoveTransformer.class, "ERR_AccessesPackagePrivateFeature",workingCopy.getFileObject().getName(),el, getTypeElement(el).getSimpleName()));
                        }
                    }
                }
            } else if (isPackageRename() && "*".equals(node.getIdentifier().toString())) { // NOI18N
                ExpressionTree exprTree = node.getExpression();
                TreePath exprPath = workingCopy.getTrees().getPath(workingCopy.getCompilationUnit(), exprTree);
                Element elem = workingCopy.getTrees().getElement(exprPath);
                if (elem != null && elem.getKind() == ElementKind.PACKAGE && isThisPackageMoving((PackageElement) elem)) {
                    String newPackageName = getTargetPackageName(elem);
                    Tree nju = make.MemberSelect(make.Identifier(newPackageName), "*"); // NOI18N
                    rewrite(node, nju);
                }
            }
        }
        return super.visitMemberSelect(node, p);
    }

    @Override
    public Tree visitIdentifier(IdentifierTree node, Element p) {
        if (!workingCopy.getTreeUtilities().isSynthetic(getCurrentPath())) {
            Element el = workingCopy.getTrees().getElement(getCurrentPath());
            if (el != null) {
                if (!isThisFileMoving) {
                    if (isElementMoving(el)) {
                        if (!elementsAlreadyImported.contains(el)) {
                            String targetPackageName = getTargetPackageName(el);
                            if (!RetoucheUtils.getPackageName(workingCopy.getCompilationUnit()).equals(targetPackageName))
                                elementsToImport.add(el);
                        }
                    } else if (el.getKind() != ElementKind.PACKAGE
                            && !(el.getModifiers().contains(Modifier.PUBLIC) || el.getModifiers().contains(Modifier.PROTECTED))
                            && getPackageOf(el).toString().equals(originalPackage)
                            && move.filesToMove.contains(getFileObject(el))) {
                                problem = createProblem(problem, false, NbBundle.getMessage(MoveTransformer.class, "ERR_AccessesPackagePrivateFeature",workingCopy.getFileObject().getName(), el, getTypeElement(el).getSimpleName()));
                    }
                } else {
                    Boolean[] isElementMoving = new Boolean[1];
                    if (isTopLevelClass(el) && !isElementMoving(el, isElementMoving)
                            && getPackageOf(el).toString().equals(originalPackage)) {
                        importToAdd.add(el.toString());
                        isThisFileReferencingOldPackage = true;
                    }
                    if (el.getKind() != ElementKind.PACKAGE
                            && !(el.getModifiers().contains(Modifier.PUBLIC) || el.getModifiers().contains(Modifier.PROTECTED))
                            && !isElementMoving(el, isElementMoving)
                            && getPackageOf(el).toString().equals(originalPackage)
                            && !move.filesToMove.contains(getFileObject(el))) {
                        problem = createProblem(problem, false, NbBundle.getMessage(MoveTransformer.class, "ERR_AccessesPackagePrivateFeature2",workingCopy.getFileObject().getName(),el, getTypeElement(el).getSimpleName()));
                    }
                }
            }
        }

        return super.visitIdentifier(node, p);
    }

    private FileObject getFileObject(Element el) {
        return SourceUtilsEx.getFile(el, workingCopy.getClasspathInfo(), cacheOfSrcFiles);
    }

    private boolean isThisPackageMoving(PackageElement el) {
        return move.packages.contains(el.getQualifiedName().toString());
    }

    private String getTargetPackageName(Element el) {
        return move.getTargetPackageName(getFileObject(el));
    }

    private TypeElement getTypeElement(Element e) {
        TypeElement t = workingCopy.getElementUtilities().enclosingTypeElement(e);
        if (t==null && e instanceof TypeElement) {
            return (TypeElement) e;
        }
        return t;
    }
    static final Problem createProblem(Problem result, boolean isFatal, String message) {
        Problem problem = new Problem(isFatal, message);
        if (result == null) {
            return problem;
        }
        problem.setNext(result);
        return problem;
    }


    private PackageElement getPackageOf(Element el) {
        //return workingCopy.getElements().getPackageOf(el);
        while (el.getKind() != ElementKind.PACKAGE)
            el = el.getEnclosingElement();
        return (PackageElement) el;
    }

    private boolean isPackageRename() {
        return move.isRenameRefactoring;
    }

    private boolean isThisFileReferencedbyOldPackage() {
        Set<FileObject> references = new HashSet<FileObject>(move.whoReferences.get(workingCopy.getFileObject()));
        references.removeAll(move.filesToMove);
        for (FileObject file:references) {
            if (file.getParent().equals(originalFolder))
                return true;
        }
        return false;
    }

//    private boolean isThisFileReferencingOldPackage() {
//        //TODO: correctly implement
//        return true;
//    }

    private boolean isElementMoving(Element el, Boolean[] cache) {
        if (cache[0] == null) {
            cache[0] = isElementMoving(el);
        }

        return cache[0];
    }

    private boolean isElementMoving(Element el) {
        ElementKind kind = el.getKind();
        if (!(kind.isClass() || kind.isInterface())) {
            return false;
        }
        ElementHandle<Element> elHandle = ElementHandle.create(el);
        return classes2Move.contains(elHandle);
    }

    private boolean isTopLevelClass(Element el) {
        return (el.getKind().isClass() ||
                el.getKind().isInterface()) &&
                el.getEnclosingElement().getKind() == ElementKind.PACKAGE;
    }

    @Override
    public Tree visitCompilationUnit(CompilationUnitTree node, Element p) {
        Tree result = super.visitCompilationUnit(node, p);
        if (workingCopy.getTreeUtilities().isSynthetic(getCurrentPath())) {
            return result;
        }
        CompilationUnitTree cut = node;
        List<? extends ImportTree> imports = cut.getImports();
        if (!importToRemove.isEmpty()) {
            List<ImportTree> temp = new ArrayList<ImportTree>(imports);
            temp.removeAll(importToRemove);
            imports = temp;
        }
        if (isThisFileMoving) {
            // change package statement if old and new package exist, i.e.
            // neither old nor new package is default
            String newPckg = move.getTargetPackageName(workingCopy.getFileObject());
            if (node.getPackageName() != null && !"".equals(newPckg)) {
                if (importToRemove.isEmpty()) {
                    rewrite(node.getPackageName(), make.Identifier(newPckg));
                } else {
                    cut = make.CompilationUnit(make.Identifier(newPckg), imports, node.getTypeDecls(), node.getSourceFile());
                }
            } else {
                // in order to handle default package, we have to rewrite whole
                // compilation unit:
                cut = make.CompilationUnit(
                        "".equals(newPckg) ? null : make.Identifier(newPckg),
                        imports,
                        node.getTypeDecls(),
                        node.getSourceFile()
                );
            }
            if (isThisFileReferencingOldPackage) {
                //add import to old package
                ExpressionTree newPackageName = cut.getPackageName();
                if (newPackageName != null) {
                    try {
                        cut = RetoucheUtils.addImports(cut, new LinkedList<String>(importToAdd), make);
                    } catch (IOException ex) {
                        Exceptions.printStackTrace(ex);
                    }
                } else {
                    if (!moveToDefaulPackageProblem) {
                        problem = createProblem(problem, false, NbBundle.getMessage(MoveTransformer.class, "ERR_MovingClassToDefaultPackage"));
                        moveToDefaulPackageProblem = true;
                    }
                }

            }
        } else if (!importToRemove.isEmpty()) {
            cut = make.CompilationUnit(node.getPackageName(), imports, node.getTypeDecls(), node.getSourceFile());
        }

        for (Element el:elementsToImport) {
            String newPackageName = getTargetPackageName(el);
            if (!"".equals(newPackageName)) { // NOI18N
                cut = insertImport(cut, newPackageName + "." +el.getSimpleName(), el, newPackageName); // NOI18N
            }
        }
        rewrite(node, cut);
        return result;
    }

    private CompilationUnitTree insertImport(CompilationUnitTree node, String imp, Element orig, String targetPkgOfOrig) {
        for (ImportTree tree: node.getImports()) {
            if (tree.getQualifiedIdentifier().toString().equals(imp))
                return node;
            if (orig!=null) {
                if (tree.getQualifiedIdentifier().toString().equals(getPackageOf(orig).getQualifiedName()+".*") && isPackageRename()) { // NOI18N
                    rewrite(tree.getQualifiedIdentifier(), make.Identifier(targetPkgOfOrig + ".*")); // NOI18N
                    return node;
                }
            }
        }
        CompilationUnitTree nju = make.insertCompUnitImport(node, 0, make.Import(make.Identifier(imp), false));
        return nju;
    }

    @Override
    public Tree visitImport(ImportTree node, Element p) {
        if (!workingCopy.getTreeUtilities().isSynthetic(getCurrentPath())) {
            final Element el = workingCopy.getTrees().getElement(new TreePath(getCurrentPath(), node.getQualifiedIdentifier()));
            if (el != null) {
                if (isElementMoving(el)) {
                    String newPackageName = getTargetPackageName(el);

                    if (!"".equals(newPackageName)) {
                        String cuPackageName = RetoucheUtils.getPackageName(workingCopy.getCompilationUnit());
                        if (cuPackageName.equals(newPackageName)) { //remove newly created import from same package
                            importToRemove.add(node);
                            return node;
                        }
                    }
                }
            }
        }
        return super.visitImport(node, p);
    } */
    
}
