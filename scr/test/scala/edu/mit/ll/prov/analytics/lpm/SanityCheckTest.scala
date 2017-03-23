package edu.mit.ll.prov.analytics.lpm

import scodec.codecs._
import scodec.bits._
import edu.mit.ll.prov.analytics.lpm.CustomCodecs._

import org.junit._
import Assert._

class SanityCheckTest {

  @Test
  def boundsTest: Unit = {
    assert(uuid.sizeBound.exact.get == 16 * 8)

    assert(sbInode.sizeBound.exact.get == 24 * 8, sbInode.sizeBound.exact.get)
  }

}