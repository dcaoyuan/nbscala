package org.netbeans.modules.scala.sbt.queries

import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import java.net.URL
import javax.swing.event.ChangeListener
import org.netbeans.api.java.queries.BinaryForSourceQuery
import org.netbeans.api.java.queries.BinaryForSourceQuery.Result
import org.netbeans.api.project.Project
import org.netbeans.modules.scala.core.ProjectResources
import org.netbeans.modules.scala.sbt.project.SBTResolver
import org.netbeans.spi.java.queries.BinaryForSourceQueryImplementation
import org.openide.util.ChangeSupport
import scala.collection.mutable


/**
 * 
 * @author Caoyuan Deng
 */
class SBTBinaryForSourceQuery(project: Project) extends BinaryForSourceQueryImplementation {        
  private val cache = new mutable.HashMap[String, BinResult]()
  private lazy val sbtResolver = {
    val x = project.getLookup.lookup(classOf[SBTResolver])
    
    x.addPropertyChangeListener(new PropertyChangeListener {
        override
        def propertyChange(evt: PropertyChangeEvent) {
          evt.getPropertyName match {
            case SBTResolver.DESCRIPTOR_CHANGE =>
              cache synchronized {
                cache.values foreach (_.fireChange)
                cache.clear
              }
            case _ =>
          }
        }
      }
    )
    
    x
  }
    
  def findBinaryRoots(sourceRoot: URL): Result = cache synchronized {
    assert(sourceRoot != null)
    cache.getOrElseUpdate(sourceRoot.toURI.normalize.toString, {
        import ProjectResources._
        val mainSrcs = sbtResolver.getSources(SOURCES_TYPE_JAVA, false) ++ sbtResolver.getSources(SOURCES_TYPE_SCALA, false) ++ sbtResolver.getSources(SOURCES_TYPE_MANAGED, false)
        val found = mainSrcs filter {_._1.toURI.toURL == sourceRoot}
        if (found.length > 0) {
          new BinResult(found map (_._2.toURI.toURL))
        } else {
          val testSrcs = sbtResolver.getSources(SOURCES_TYPE_JAVA, true) ++ sbtResolver.getSources(SOURCES_TYPE_SCALA, true) ++ sbtResolver.getSources(SOURCES_TYPE_MANAGED, true)
          val found = testSrcs filter {_._1.toURI.toURL == sourceRoot}
          if (found.length > 0) {
            new BinResult(found map (_._2.toURI.toURL))
          } else {
            new BinResult(Array())
          }
        }
      }
    )
  }
    
  class BinResult(urls: Array[URL]) extends BinaryForSourceQuery.Result {
    private val changeSupport = new ChangeSupport(this)
        
    def getRoots: Array[URL] = urls

    def addChangeListener(l: ChangeListener) {
      changeSupport.addChangeListener(l)
    }

    def removeChangeListener(l: ChangeListener) {
      changeSupport.removeChangeListener(l)
    }

    def fireChange {
      changeSupport.fireChange
    }
  }
}
