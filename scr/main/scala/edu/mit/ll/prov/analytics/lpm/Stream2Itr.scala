package edu.mit.ll.prov.analytics.lpm

import scodec.Codec
import scala.reflect.io.File
import scodec.Attempt.Successful
import scodec.DecodeResult
import scala.annotation.tailrec
import scodec.Err
import scala.io.Source
import java.util.UUID
import scala.collection.GenTraversableOnce
import fs2.Task
import scodec.Attempt
import org.neo4j.driver.v1.GraphDatabase
import org.neo4j.driver.v1.AuthTokens

import scodec.codecs._
import scodec.stream.{ decode, StreamDecoder }
import scodec.bits.ByteVector
import scodec.bits._

import shapeless.{ :+:, Coproduct, CNil, Inl, Inr }
import scodec.codecs.CoproductCodec



// a hack to convert a fs2 stream to a more standard iterator
// This could be improved with a non-blocking queue, but the conversion is akward
class Stream2Iter[A](s: fs2.Stream[Task, A]) extends Iterator[A] {

  var Some(Some(r)) = s.uncons1.runLast.unsafeValue()

  def hasNext: Boolean = r.isDefined

  def next(): A = {
    val Some((out, stream)) = r

    //get the next element if it exists
    val Some(Some(rrr)) = stream.uncons1.runLast.unsafeValue()
    r = rrr

    out
  }

}





