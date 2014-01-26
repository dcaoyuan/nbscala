package org.netbeans.modules.scala.sbt

import org.openide.filesystems.FileObject
import org.openide.loaders.MultiDataObject
import org.openide.loaders.MultiFileLoader

class SbtDataObject(pf: FileObject, loader: MultiFileLoader) extends MultiDataObject(pf, loader) {
  registerEditor("text/x-sbt", false);

  @Override
  override protected def associateLookup(): Int = 1
}