package org.netbeans.modules.scala.maven;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.netbeans.modules.maven.api.output.OutputProcessor;
import org.netbeans.modules.maven.api.output.OutputVisitor;
import org.netbeans.api.java.project.JavaProjectConstants;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.api.project.Sources;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;




/**
 * compilation output processing
 * @author  Milos Kleint
 */
public class ScalaOutputListenerProvider implements OutputProcessor {
    
    private static final String[] SCALAGOALS = new String[] {
        "mojo-execute#scala:compile", //NOI18N
        "mojo-execute#scala:testCompile" //NOI18N
    };
    private Pattern failPattern;
    
    public ScalaOutputListenerProvider() {
        failPattern = Pattern.compile("\\s*(?:\\[WARNING\\])?\\s*(.*)\\.scala\\:([0-9]*)\\:(.*)", Pattern.DOTALL); //NOI18N
    }
    
    @Override
    public void processLine(String line, OutputVisitor visitor) {
            Matcher match = failPattern.matcher(line);
            if (match.matches()) {
                String clazz = match.group(1);
                String lineNum = match.group(2);
                String text = match.group(3);
                File clazzfile = FileUtil.normalizeFile(new File(clazz + ".scala")); //NOI18N
                visitor.setOutputListener(new CompileAnnotation(clazzfile, lineNum,
                        text), true); 
                FileUtil.refreshFor(clazzfile);
                FileObject file = FileUtil.toFileObject(clazzfile);
                String newclazz = clazz;
                if (file != null) {
                    Project prj = FileOwnerQuery.getOwner(file);
                    if (prj != null) {
                        Sources srcs = ProjectUtils.getSources(prj);
                        if (srcs != null) {
                            for (SourceGroup grp : srcs.getSourceGroups(ScalaSourcesImpl.TYPE_SCALA)) {
                                if (FileUtil.isParentOf(grp.getRootFolder(), file)) {
                                    newclazz = FileUtil.getRelativePath(grp.getRootFolder(), file);
                                    if (newclazz.endsWith(".scala")) { //NOI18N
                                        newclazz = newclazz.substring(0, newclazz.length() -".scala".length()); //NOI18N
                                    }
                                }
                            }
                        }
                    }
                }
                line = line.replace(clazz, newclazz); //NOI18N
                visitor.setLine(line);
            }
    }

    @Override
    public String[] getRegisteredOutputSequences() {
        return SCALAGOALS;
    }

    @Override
    public void sequenceStart(String sequenceId, OutputVisitor visitor) {
    }

    @Override
    public void sequenceEnd(String sequenceId, OutputVisitor visitor) {
    }
    
    @Override
    public void sequenceFail(String sequenceId, OutputVisitor visitor) {
    }
    
}
