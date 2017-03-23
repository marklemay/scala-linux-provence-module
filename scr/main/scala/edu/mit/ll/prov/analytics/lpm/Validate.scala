package edu.mit.ll.prov.analytics.lpm

import edu.mit.ll.prov.analytics.lpm.LpmMessage._
import fs2.Stream
import fs2.Task


object Validate {

  //TODO: either return types more helpful??
  
  def validate(f:String):Unit = validate(LpmProtocol.frames.decodeMmap(new java.io.FileInputStream(f).getChannel))
  
  def validate(itr: Stream[Task, LpmMsg]):Unit = validate(new Stream2Iter(itr))
  
  def validate(itr: Iterator[LpmMsg]):Unit = {

    itr.next() match {
      case Boot(0, _) => {}
      case _ => assert(false, "must start with boot")
    }

    var activeIds = Set(0L)

    for (msg <- itr) {
      assert(activeIds.contains(msg.credid))
      msg match {
        case CredFork(_, newId) => activeIds += newId
        case CredFree(id) => activeIds -= id
        case _ => {}
      }
    }

  }
}