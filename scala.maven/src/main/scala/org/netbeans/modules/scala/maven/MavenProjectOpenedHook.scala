package org.netbeans.modules.scala.maven

import org.netbeans.api.project.Project
import org.netbeans.spi.project.LookupProvider
import org.netbeans.spi.project.ui.ProjectOpenedHook
import org.openide.util.Lookup
import org.openide.util.lookup.Lookups

class MavenProjectOpenedHook(project: Project) extends ProjectOpenedHook {
  protected def projectOpened() {
    val prefs = project.getLookup.lookup(classOf[ScalariformPrefs])
    if (prefs != null) {
      prefs.attachUpdater
    }
  }

  protected def projectClosed() {
    val prefs = project.getLookup.lookup(classOf[ScalariformPrefs])
    if (prefs != null) {
      prefs.detachUpdater
    }
  }

}

class MavenProjectOpenedHookLookupProvider extends LookupProvider {
  def createAdditionalLookup(lookup: Lookup): Lookup = {
    val project = lookup.lookup(classOf[Project])
    Lookups.fixed(new MavenProjectOpenedHook(project))
  }
}

