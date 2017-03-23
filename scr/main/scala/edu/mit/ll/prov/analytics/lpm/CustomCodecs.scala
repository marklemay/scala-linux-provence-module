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
import scodec.codecs._
import scodec.stream.{ decode, StreamDecoder }
import scodec.bits.ByteVector
import scodec.bits._

import shapeless.{ :+:, Coproduct, CNil, Inl, Inr }
import scodec.codecs.CoproductCodec

object CustomCodecs {

  /**
   * Uses a some discriminator d to determine the encoding of the rest of the the bit stream
   */
  final class Desc[D, A](d: Codec[D], m: Map[D, Codec[A]]) extends Codec[A] {

    def defualt(dd: D) = fail[A](Err(s"no maping for $dd"), Err(s"no maping for $dd"))

    val decoder = d flatMap { ds => m.withDefault(defualt)(ds) }

    // Members declared in scodec.Decoder
    override def decode(bits: scodec.bits.BitVector): scodec.Attempt[scodec.DecodeResult[A]] = decoder.decode(bits)

    // Members declared in scodec.Encoder
    override def encode(value: A): scodec.Attempt[scodec.bits.BitVector] = Attempt.Failure(scodec.Err("not yet implemented"))
    override def sizeBound: scodec.SizeBound = scodec.SizeBound.unknown //TODO: could be more explicit

  }

  /**
   * a nieve string decoder that is the exact same as the old one
   */
  //TODO: depricate this for utf8
  final class SimpleCharStr() extends Codec[String] {
    override def decode(buffer: BitVector) = {
      val bs = buffer.toByteArray.map(_.toChar)

      Attempt.successful(DecodeResult(bs.mkString, BitVector.empty))
    }

    // Members declared in scodec.Encoder
    def encode(value: String): scodec.Attempt[scodec.bits.BitVector] = Attempt.Failure(scodec.Err("not yet implemented"))
    def sizeBound: scodec.SizeBound = scodec.SizeBound.unknown
  }

  case class SbInode(sbInode: UUID, ino: ByteVector)
  val sbInode = (
    ("sbInode" | uuid)
    :: ("ino" | bytes(8))).as[SbInode]

  //TODO: chance these are backwards, TODO rename?
  case class FlieMask(
    EXEC: Boolean,
    WRITE: Boolean,
    READ: Boolean,
    APPEND: Boolean,
    ACCESS: Boolean,
    OPEN: Boolean,
    mystery: BitVector)

  val flieMask = fixedSizeBytes(4, bool :: bool :: bool :: bool :: bool :: bool :: bits).as[FlieMask]
}