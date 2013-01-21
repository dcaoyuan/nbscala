package org.netbeans.modules.scala.sbt.project

import org.netbeans.api.project.Project
import org.netbeans.modules.scala.sbt.console.SBTConsoleTopComponent
import org.netbeans.spi.project.ui.ProjectOpenedHook

class SBTProjectOpenedHook(project: Project) extends ProjectOpenedHook {
  
  override
  protected def projectOpened() {
    val win = SBTConsoleTopComponent.findInstance(project)
    if (win != null) {
      win.open
      win.requestActive
    }
    //IvyLibraryController ivyLibraryController = project.getLookup().lookup(IvyLibraryController.class);
    //ivyLibraryController.triggerIvyResolution();
    //classpaths = new ClassPath[]{
    //            cpProvider.getClassPath(ClassPathScope.COMPILE),
    //            cpProvider.getClassPath(ClassPathScope.COMPILE_TEST)
    //        };
    //GlobalPathRegistry.getDefault().register(ClassPath.COMPILE, classpaths);
//      
  }

  override
  protected def projectClosed() {
    //GlobalPathRegistry.getDefault().unregister(ClassPath.COMPILE, classpaths);
    //IOTabProvider.getInstance().unregister(project);
  }
}
