package org.netbeans.modules.scala.app;

import java.awt.event.KeyEvent;
import java.io.File;
import java.util.logging.Level;
import junit.framework.Test;
import org.netbeans.jellytools.NbDialogOperator;
import org.netbeans.jellytools.NewProjectWizardOperator;
import org.netbeans.jellytools.ProjectsTabOperator;
import org.netbeans.jellytools.TopComponentOperator;
import org.netbeans.jellytools.actions.ActionNoBlock;
import org.netbeans.jellytools.nodes.Node;
import org.netbeans.jellytools.nodes.ProjectRootNode;
import org.netbeans.jellytools.properties.editors.FileCustomEditorOperator;
import org.netbeans.jemmy.operators.JButtonOperator;
import org.netbeans.jemmy.operators.JTableOperator;
import org.netbeans.jemmy.operators.JTextPaneOperator;
import org.netbeans.junit.NbModuleSuite;
import org.netbeans.junit.NbTestCase;

public class ApplicationTest extends NbTestCase {

    private final String SAMPLE_SCALA_MAVEN = System.getProperty("sample-scala-maven");
    private final String SAMPLE_SCALA_JAVA_MAVEN = System.getProperty("sample-scala-java-maven");

    public static Test suite() {
        return NbModuleSuite.createConfiguration(ApplicationTest.class).
                gui(true).
                clusters(".*").
                failOnMessage(Level.SEVERE).
                // TODO: Can be enables if the java.lang.ClassCastException: org.netbeans.api.project.ProjectUtils$AnnotateIconProxyProjectInformation
                // cannot be cast to org.netbeans.modules.scala.project.J2SEProject$Info is solved
                //                failOnException(Level.INFO).
                suite();
    }

    public ApplicationTest(String n) {
        super(n);
    }

    public void testApplication() {
        new ActionNoBlock("Help|About", null).performMenu();
        new NbDialogOperator("About").closeByButton();
    }

    /**
     * Test if the ScalaShell is enabled and opens.
     */
    public void testInteractiveScalaShell() {
        String scalaHome = System.getenv("SCALA_HOME");
        if (scalaHome == null || scalaHome.trim().equals("")) {
            return; // No Scala Home is set, Shell will not work.
        }
        File scalaHomeDir = new File(scalaHome);
        assertTrue("Environment Variable SCALA_HOME=" + scalaHome + " doesn't point to an existing directory", scalaHomeDir.exists());
        assertTrue("Environment Variable SCALA_HOME=" + scalaHome + " doesn't point to a directory", scalaHomeDir.isDirectory());
        ActionNoBlock openScalaShell = new ActionNoBlock("Window|Other|Interactive Scala Shell", null);
        // TODO: If the Action Interactive Sacla Shell is missing the test will block here. Some check before this point, if it exist, would be nice.
        openScalaShell.performMenu();
        TopComponentOperator scalaConsole = new TopComponentOperator("Scala Console");
        assertTrue("Scala Console not showing", scalaConsole.isShowing());
        // TODO: Same as before, a test for existence would be nice.
        JTextPaneOperator theConsole = new JTextPaneOperator(scalaConsole);
        assertTrue("Scala Console is not Enabled", theConsole.isEnabled());
        theConsole.enterText(":quit");
        theConsole.waitComponentShowing(false);
        assertFalse("Scala Console still visible, should be closed", theConsole.isShowing());
    }

    public void testNewScalaProject() {
        NewProjectWizardOperator newProjectWizard = NewProjectWizardOperator.invoke();
        newProjectWizard.selectCategory("Scala");
        newProjectWizard.selectProject("Scala Application");
        newProjectWizard.next();
        newProjectWizard.finish();

        // TODO: Cause of the missing JUnit dependencies, a resolve references Dialog is opening.
        // IDEA: Change the default Project to have no dependency to JUnit
        new NbDialogOperator("Open Project").btClose();

        // TODO: Some asserts or an actuall compile to make sure the project is active and alive.
    }

    /**
     * A Test to see if a scala maven project is discovert and the scala sources
     * are displayed in the source node.
     */
    // TODO: Can only be enabled if http://netbeans.org/bugzilla/show_bug.cgi?id=216738 is solved.
    public void ignoreTestScalaMavenProject() {
        new ActionNoBlock("File|Import|From ZIP", null).performMenu();
        new NbDialogOperator("Import Project(s) from ZIP").pushKey(KeyEvent.VK_TAB);
        new NbDialogOperator("Import Project(s) from ZIP").pushKey(KeyEvent.VK_SPACE);
        new FileCustomEditorOperator("Öffnen").setSelectedFile(SAMPLE_SCALA_MAVEN);
        new FileCustomEditorOperator("Öffnen").pushKey(KeyEvent.VK_ENTER);
        new JButtonOperator(new NbDialogOperator("Import Project(s) from ZIP"), "Import").push();
        ProjectsTabOperator pto = ProjectsTabOperator.invoke();
        ProjectRootNode prn = pto.getProjectRootNode("sample-scala-maven");
        Node node = new Node(prn, "Scala Packages|sample|App.scala");
        node.select();
        node.performPopupAction("Open");
        new ActionNoBlock("Window|Action Items", null).performMenu();
        TopComponentOperator actionItems = new TopComponentOperator("Action Items");
        actionItems.makeComponentVisible();
        JTableOperator t = new JTableOperator(actionItems);
        int row = t.findCellColumn("SampleJavaError");
        assertTrue("No Error SampleJavaError in the Action Itmes found, but should be", row >= 0);
        row = t.findCellColumn("Blub");
        assertTrue("Error Blub in the Action Itmes found, but should not be", row == -1);
    }

    /**
     * A Test to see if a combinded scala java maven project is discovert and
     * contains no errors. At the moment this is the case.
     */
    // TODO: Can only be enabled if http://netbeans.org/bugzilla/show_bug.cgi?id=216738 is solved.
    public void ignoreTestScalaJavaMavenProject() {
        new ActionNoBlock("File|Import|From ZIP", null).performMenu();
        new NbDialogOperator("Import Project(s) from ZIP").pushKey(KeyEvent.VK_TAB);
        new NbDialogOperator("Import Project(s) from ZIP").pushKey(KeyEvent.VK_SPACE);
        new FileCustomEditorOperator("Öffnen").setSelectedFile(SAMPLE_SCALA_JAVA_MAVEN);
        new FileCustomEditorOperator("Öffnen").pushKey(KeyEvent.VK_ENTER);
        new JButtonOperator(new NbDialogOperator("Import Project(s) from ZIP"), "Import").push();
        ProjectsTabOperator pto = ProjectsTabOperator.invoke();
        ProjectRootNode prn = pto.getProjectRootNode("sample-scala-java-maven");
        prn.select();
        Node node = new Node(prn, "Source Packages|sample|Runner.java");
        node.select();
        node.performPopupAction("Open");
        // Opening the Action Items to see if the selecte Java Class has an Error.
        new ActionNoBlock("Window|Action Items", null).performMenu();
        TopComponentOperator actionItems = new TopComponentOperator("Action Items");
        actionItems.makeComponentVisible();
        JTableOperator t = new JTableOperator(actionItems);
        int row = t.findCellColumn("Runner.java");
        // The following Assert should validate to -1, meaning there is no row containing the String Runner.java
        // At the moment this is not the case. A mixed scala-java project works, but displays erros in the ui.
        // assertTrue("No Error Runner.java in the Action Itmes found, but should be", row == -1);
    }

}
