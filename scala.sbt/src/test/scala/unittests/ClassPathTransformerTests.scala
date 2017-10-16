
package unittests

import java.io.File
import org.junit.Test
import org.netbeans.api.project.Project
import org.netbeans.modules.scala.sbt.classpath.ProjectFileUrlConverter
import org.netbeans.modules.scala.sbt.classpath.ProjectFileUrlConverter
import org.netbeans.modules.scala.sbt.classpath.ProjectFileUrlConverter
import org.netbeans.modules.scala.sbt.classpath.ProjectFileUrlConverter
import org.openide.filesystems.FileObject
import org.openide.filesystems.FileUtil
import org.junit.Assert
import org.openide.util.Lookup

class ClassPathTransformerTests {

  /**
   * @param args the command line arguments
   */
  @Test
  def testFileExists(): Unit = {
    val f1 = new File("/bin/bash")
    val fo1 = FileUtil.toFileObject(f1)

    Assert.assertTrue(f1.isFile)
    Assert.assertTrue(f1.exists())
    Assert.assertTrue(fo1 != null)

    Assert.assertFalse(f1.toURI.toURL.toString.endsWith("/"))
  }

  @Test
  def testDirectoryConverts(): Unit = {
    val f1 = new File("/bin/")
    val fo1 = FileUtil.toFileObject(f1)

    Assert.assertTrue(f1.isDirectory)
    Assert.assertTrue(f1.exists())
    Assert.assertTrue(fo1 != null)

    Assert.assertTrue(f1.toURI.toURL.toString.endsWith("/"))
  }

  @Test
  def testWithExistingFile() = {
    val file = new File("/bin/bash")
    val dummyProj = new Project() {
      def getLookup(): Lookup = Lookup.EMPTY
      def getProjectDirectory(): FileObject = FileUtil.toFileObject(new File("."))
    }
    val foundUrl = ProjectFileUrlConverter.convert(dummyProj, file)

    assert(!foundUrl.toString.endsWith("/"), s"an existing file in $foundUrl must not end with a slash")
  }

  @Test
  def testWithExistingDirectory() = {
    val file = new File("/bin")
    val dummyProj = new Project() {
      def getLookup(): Lookup = Lookup.EMPTY
      def getProjectDirectory(): FileObject = FileUtil.toFileObject(new File("."))
    }
    val foundUrl = ProjectFileUrlConverter.convert(dummyProj, file)

    assert(foundUrl.toString.endsWith("/"), s"an existing directory in $foundUrl must end with a slash")
  }
  @Test
  def testWithNonExistingDirectory() = {
    val file = new File("/foobar")
    val dummyProj = new Project() {
      def getLookup(): Lookup = Lookup.EMPTY
      def getProjectDirectory(): FileObject = FileUtil.toFileObject(new File("."))
    }
    val foundUrl = ProjectFileUrlConverter.convert(dummyProj, file)

    assert(foundUrl.toString.endsWith("/"), s"an non-existing directory in $foundUrl must end with a slash")
  }

}
