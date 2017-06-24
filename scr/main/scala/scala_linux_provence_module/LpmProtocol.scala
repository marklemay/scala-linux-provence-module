package scala_linux_provence_module

import scodec.codecs._

import scala_linux_provence_module.LpmMessage.lpmData

import scodec.stream.{ decode => D, StreamDecoder }

object LpmProtocol {

  val frame = variableSizeBytes(uint24L, lpmData, 3 /*bytes*/ )

  //recursively decode
  val frames = D.once(frame).many
}