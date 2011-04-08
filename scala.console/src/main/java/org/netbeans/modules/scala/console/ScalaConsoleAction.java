package org.netbeans.modules.scala.console;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;
import org.openide.windows.TopComponent;

/**
 * Action which shows Scala Console component.
 */
public class ScalaConsoleAction extends AbstractAction {
    
    public ScalaConsoleAction() {
        super(NbBundle.getMessage(ScalaConsoleAction.class, "CTL_ScalaConsoleAction"));
        putValue(SMALL_ICON,new ImageIcon(Utilities.loadImage(ScalaConsoleTopComponent.ICON_PATH, true)));
    }
    
    public void actionPerformed(ActionEvent evt) {
        TopComponent win = ScalaConsoleTopComponent.findInstance();
        win.open();
        win.requestActive();
    }
    
}
