package org.netbeans.modules.scala.console

import java.util.logging.Level
import java.util.logging.Logger
import org.openide.loaders.InstanceDataObject

/**
 * @see org.netbeans.core.windows.persistence.PersistenceManager
 * 
 * @author Caoyuan Deng
 */
object TopComponentId {
  private val log = Logger.getLogger(getClass.getName)
  
  private val idEscape = try {
    val x = classOf[InstanceDataObject].getDeclaredMethod("escapeAndCut", classOf[String])
    x.setAccessible(true)
    x
  } catch {
    case ex: Exception => null
  }
  
  private val idUnescape = try {
    val x = classOf[InstanceDataObject].getDeclaredMethod("unescape", classOf[String])
    x.setAccessible(true)
    x
  } catch {
    case ex: Exception => null
  }

  /** 
   * compute filename in the same manner as InstanceDataObject.create
   * [PENDING] in next version this should be replaced by public support
   * likely from FileUtil
   * @see issue #17142
   * 
   */
  def escape(name: String) = {
    if (idEscape != null) {
      try {
        idEscape.invoke(null, name).asInstanceOf[String]
      } catch {
        case ex: Exception => log.log(Level.INFO, "Escape support failed", ex); name
      }
    } else name
  }
    
  /** 
   * compute filename in the same manner as InstanceDataObject.create
   * [PENDING] in next version this should be replaced by public support
   * likely from FileUtil
   * @see issue #17142
   */
  def unescape(name: String) = {
    if (idUnescape != null) {
      try {
        idUnescape.invoke(null, name).asInstanceOf[String]
      } catch {
        case ex: Exception => log.log(Level.INFO, "Escape support failed", ex); name
      }
    } else name
  }

}