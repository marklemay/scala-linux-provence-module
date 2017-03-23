package edu.mit.ll.prov.analytics.lpm

import edu.mit.ll.prov.analytics.lpm.LpmMessage._

import org.neo4j.driver.v1.GraphDatabase
import org.neo4j.driver.v1.AuthTokens

object Main {

  def main(args: Array[String]): Unit = {
    // C:\Users\Mark\workspace-neon\lpm-injest\data\lpms\analytics-data\prov-benign.log\prov-benign.log bolt://localhost:7687 neo4j n
    println(args.mkString(", "))
    val Array(path, neo, user, pass) = args.toArray

    val s = LpmProtocol.frames.decodeMmap(new java.io.FileInputStream(path).getChannel)

    val driver = GraphDatabase.driver(neo,
      AuthTokens.basic(user, pass))

    val session = driver.session();

    println("connected", session.isOpen())

    val itr = new Stream2Iter(s)

    var activeIds = Set[Long]()

    for (msg <- itr) {

      msg match {
        case exec: Exec if exec.command == "/usr/sbin/httpd" => {
          activeIds += exec.credid

          val r = session.run(raw"""
        CREATE (n:Start) SET n.cred_id = ${exec.credid}
        """)
          println("...")
          println(r.consume().statement())

        }
        case _ => {}
      }

      if (activeIds.contains(msg.credid)) {

        msg match {
          case Boot(cred, uuid) => {
            val r = session.run(raw"""
        CREATE (n:Boot) SET n.uuid="$uuid", n.cred_id = $cred
        """)
            println("...")
            println(r.consume().statement())
          }
          case CredFork(cred, newcred) => {

            val r = session.run(raw"""
        MATCH (n) WHERE (NOT (n)-[:comp]->()) AND n.cred_id= $cred
        create (n)-[:comp]->(m:CredFork)-[:fork]->(nn:StartCred) SET m.cred_id= $cred, nn.cred_id= $newcred
        
        """)
            println("...")
            println(r.consume().statement())
          }
          case e @ Exec(cred, _, _, _, _) => {

            println("hi", cred)

            val r = session.run(raw"""
        MATCH (n) WHERE (NOT (n)-[:comp]->()) AND n.cred_id= $cred
        create (n)-[:comp]->(m:Exec) SET m.cred_id= $cred, m.cmd = "${e.command}"
        
        """)
            println("...")
            println(r.consume().statement())
          }

          case CredFree(cred) => {

            val r = session.run(raw"""
        MATCH (n) WHERE (NOT (n)-[:comp]->()) AND n.cred_id= $cred
        create (n)-[:comp]->(m:CredFree) SET m.cred_id= $cred
        
        """)
            println("...")
            println(r.consume().statement())
          }

          //          case m: ProvMsg => {
          //
          //            val r = session.run(raw"""
          //        MATCH (n) WHERE (NOT (n)-[:comp]->()) AND n.cred_id= ${m.credid}
          //        create (n)-[:comp]->(m:${m.getClass.getSimpleName}) SET m.cred_id= ${m.credid}
          //        
          //        """)
          //            println("...")
          //            println(r.consume().statement())
          //          }

          //          case m: LpmMsg => {
          //
          //            val r = session.run(raw"""
          //        MATCH (n) WHERE (NOT (n)-[:comp]->()) AND n.cred_id= ${m.credid}
          //        create (n)-[:comp]->(m:${m.getClass.getSimpleName}) SET m.cred_id= ${m.credid}
          //        
          //        """)
          //            println("...")
          //            println(r.consume().statement())
          //          }
          case _ => {}
        }

        msg match {
          case CredFork(_, newId) => activeIds += newId
          case CredFree(id) => activeIds -= id
          case _ => {}
        }
      }
    }

    /*
     * 
edu.mit.ll.prov.analytics.lpm.LpmMessage$InodeP
edu.mit.ll.prov.analytics.lpm.LpmMessage$SetId
edu.mit.ll.prov.analytics.lpm.LpmMessage$InodeDealloc
edu.mit.ll.prov.analytics.lpm.LpmMessage$UnLink
edu.mit.ll.prov.analytics.lpm.LpmMessage$Setattr
edu.mit.ll.prov.analytics.lpm.LpmMessage$FileP
edu.mit.ll.prov.analytics.lpm.LpmMessage$Link
edu.mit.ll.prov.analytics.lpm.LpmMessage$ReadLink
edu.mit.ll.prov.analytics.lpm.LpmMessage$MMap
edu.mit.ll.prov.analytics.lpm.LpmMessage$InodeAlloc

edu.mit.ll.prov.analytics.lpm.LpmMessage$CredFork
edu.mit.ll.prov.analytics.lpm.LpmMessage$Exec
edu.mit.ll.prov.analytics.lpm.LpmMessage$CredFree


---

edu.mit.ll.prov.analytics.lpm.LpmMessage$InodeP
edu.mit.ll.prov.analytics.lpm.LpmMessage$SetId
edu.mit.ll.prov.analytics.lpm.LpmMessage$InodeDealloc
edu.mit.ll.prov.analytics.lpm.LpmMessage$CredFork
edu.mit.ll.prov.analytics.lpm.LpmMessage$UnLink
edu.mit.ll.prov.analytics.lpm.LpmMessage$Setattr
edu.mit.ll.prov.analytics.lpm.LpmMessage$FileP
edu.mit.ll.prov.analytics.lpm.LpmMessage$Link
edu.mit.ll.prov.analytics.lpm.LpmMessage$MMap
edu.mit.ll.prov.analytics.lpm.LpmMessage$InodeAlloc
edu.mit.ll.prov.analytics.lpm.LpmMessage$Exec
edu.mit.ll.prov.analytics.lpm.LpmMessage$CredFree

---

edu.mit.ll.prov.analytics.lpm.LpmMessage$InodeP
edu.mit.ll.prov.analytics.lpm.LpmMessage$SetId
edu.mit.ll.prov.analytics.lpm.LpmMessage$SockSendd
edu.mit.ll.prov.analytics.lpm.LpmMessage$InodeDealloc
edu.mit.ll.prov.analytics.lpm.LpmMessage$CredFork
edu.mit.ll.prov.analytics.lpm.LpmMessage$UnLink
edu.mit.ll.prov.analytics.lpm.LpmMessage$Setattr
edu.mit.ll.prov.analytics.lpm.LpmMessage$FileP
edu.mit.ll.prov.analytics.lpm.LpmMessage$Link
edu.mit.ll.prov.analytics.lpm.LpmMessage$ReadLink
edu.mit.ll.prov.analytics.lpm.LpmMessage$SockRecv
edu.mit.ll.prov.analytics.lpm.LpmMessage$MMap
edu.mit.ll.prov.analytics.lpm.LpmMessage$InodeAlloc
edu.mit.ll.prov.analytics.lpm.LpmMessage$Exec
edu.mit.ll.prov.analytics.lpm.LpmMessage$CredFree

     */
  }
}