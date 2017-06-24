package scala_linux_provence_module

import scodec.codecs._
import scodec.bits._
import CustomCodecs._

import org.junit._
import Assert._

class SanityCheckTest {

  @Test
  def boundsTest: Unit = {
    assert(uuid.sizeBound.exact.get == 16 * 8)

    assert(sbInode.sizeBound.exact.get == 24 * 8, sbInode.sizeBound.exact.get)
  }

}