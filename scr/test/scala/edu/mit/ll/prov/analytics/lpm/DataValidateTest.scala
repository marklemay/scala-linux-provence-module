package edu.mit.ll.prov.analytics.lpm

import scodec.codecs._
import scodec.bits._
import edu.mit.ll.prov.analytics.lpm.CustomCodecs._

import org.junit._
import Assert._

class DataValidateTest {
  @Test
  def dataTest: Unit = {
    //    val s = frames.decodeMmap(new java.io.FileInputStream(raw"""C:\Users\Mark\workspace-neon\lpm-injest\data\lpms\analytics-data\prov-exploit-single-run.log\prov-exploit-single-run.log""").getChannel)

    //    val s = frames.decodeMmap(new java.io.FileInputStream(raw"""C:\Users\Mark\workspace-neon\lpm-injest\data\lpms\analytics-data\prov-benign.log\prov-benign.log""").getChannel)
    //    val s = frames.decodeMmap(new java.io.FileInputStream(raw"""C:\Users\Mark\workspace-neon\lpm-injest\data\lpms\analytics-data\prov-exploit.log\prov-exploit.log""").getChannel)

    Validate.validate(raw"""C:\Users\Mark\workspace-neon\lpm-injest\data\lpms\analytics-data\prov-exploit-single-run.log\prov-exploit-single-run.log""")
    Validate.validate(raw"""C:\Users\Mark\workspace-neon\lpm-injest\data\lpms\analytics-data\prov-benign.log\prov-benign.log""")
    Validate.validate(raw"""C:\Users\Mark\workspace-neon\lpm-injest\data\lpms\analytics-data\prov-exploit.log\prov-exploit.log""")

  }
}