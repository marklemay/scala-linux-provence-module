package edu.mit.ll.prov.analytics.lpm

import scodec.codecs._

import edu.mit.ll.prov.analytics.lpm.LpmMessage.lpmData

import scodec.stream.{ decode => D, StreamDecoder }

object LpmProtocol {

  val frame = variableSizeBytes(uint24L, lpmData, 3 /*bytes*/ )

  //recursively decode
  val frames = D.once(frame).many
}