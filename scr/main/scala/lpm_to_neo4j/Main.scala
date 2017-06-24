package lpm_to_neo4j

import scala_linux_provence_module.LpmMessage._
import org.neo4j.driver.v1.GraphDatabase
import org.neo4j.driver.v1.AuthTokens
import scala_linux_provence_module.LpmProtocol
import scala_linux_provence_module.Stream2Iter


/**
this is a stand alone lpm to noe4j importer

compile with

clean compile assembly:single

this will create 

target/lpm-injest-0.1.0-SNAPSHOT-jar-with-dependencies.jar

runnable as

java lpm-injest-0.1.0-SNAPSHOT-jar-with-dependencies.jar <lpm log path> <neo4j bolt url> <username> <pass>

Since this builds the graphs incrimentally you should load each lpm log into a new neo4j database

To keep the size of the graph reasonable it only adds nodes that come from the "httpd" command.
 */
object Main {

  def main(args: Array[String]): Unit = {

    println(args.mkString(", "))
    val Array(path, neo, user, pass) = args.toArray

    val s = LpmProtocol.frames.decodeMmap(new java.io.FileInputStream(path).getChannel)

    val driver = GraphDatabase.driver(neo, AuthTokens.basic(user, pass))

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

          case _ => {}
        }

        msg match {
          case CredFork(_, newId) => activeIds += newId
          case CredFree(id) => activeIds -= id
          case _ => {}
        }
      }
    }
  }
}