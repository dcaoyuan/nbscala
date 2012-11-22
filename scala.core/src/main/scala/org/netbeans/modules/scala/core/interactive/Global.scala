package org.netbeans.modules.scala.core.interactive

import scala.tools.nsc._
import scala.reflect.internal.util.SourceFile
import scala.tools.nsc.reporters._
import scala.tools.nsc.symtab._
import scala.tools.nsc.ast._

// ======= Modifed by Caoyuan

/** The main class of the presentation compiler in an interactive environment such as an IDE
 */
class Global(_settings: Settings, _reporter: Reporter, projectName: String = ""
) extends scala.tools.nsc.interactive.Global(_settings, _reporter, projectName) {
  
  // @see scala.tools.nsc.interactive.Global.reset(unit: RichCompilationUnit)
  def resetUnitOf(source: SourceFile) {
    unitOfFile(source.file) = new RichCompilationUnit(source)
//    getUnitOf(source) match {
//      case Some(unit) =>
//        unit.depends.clear()
//        unit.defined.clear()
//        unit.synthetics.clear()
//        unit.toCheck.clear()
//        unit.targetPos = NoPosition
//        unit.contexts.clear()
//        unit.problems.clear()
//        unit.body = EmptyTree
//        unit.status = NotLoaded
//      case None =>
//        unitOfFile(source.file) = new RichCompilationUnit(source)
//    }
  }

  final def recoveredType(tree: Tree): Option[Type] = {
    def findViaGet(atree: Tree) = qualToRecoveredType.get(atree) match {
      case None => qualToRecoveredType find {
          case (Select(qual, _), _) => qual == atree
          case (SelectFromTypeTree(qual, _), _) => qual == atree
          case (Apply(fun, _), _) => fun == atree
          case (x, _) => x == atree // usaully Ident tree
        } match {
          case None => None
          case Some((_, tpe)) => Some(tpe)
        }
      case some => some
    }
    
    def findViaPos(atree: Tree) = qualToRecoveredType find {
      case (x@Select(qual, _), _) =>
        (x.pos sameRange atree.pos) || (qual.pos sameRange atree.pos)
      case (x@SelectFromTypeTree(qual, _), _) =>
        (x.pos sameRange atree.pos) || (qual.pos sameRange atree.pos)
      case (x@Apply(fun, _), _) =>
        (x.pos sameRange atree.pos) || (fun.pos sameRange atree.pos)
      case (x, _) =>
        (x.pos sameRange atree.pos) // usaully Ident tree
    } match {
      case None => None
      case Some((_, tpe)) => Some(tpe)
    }

    def find(op: Tree => Option[Type]) = {
      op(tree) match {
        case None =>
          tree match {
            case Select(qual, _) => op(qual)
            case SelectFromTypeTree(qual, _) => op(qual)
            case Apply(fun, _) => op(fun)
            case _ => None
          }
        case some => some
      }
    }

    find(findViaGet) match {
      case None => find(findViaPos)
      case some => some
    }
  }

}

