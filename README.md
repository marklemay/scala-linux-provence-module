# scala-linux-provence-module
a helper library for LPM


this is a stand alone lpm to noe4j importer

compile with

clean compile assembly:single

this will create 

target/lpm-injest-0.1.0-SNAPSHOT-jar-with-dependencies.jar

which should be runnable as

java lpm-injest-0.1.0-SNAPSHOT-jar-with-dependencies.jar <lpm log path> <neo4j bolt url> <username> <pass>

Since this builds the graphs incrimentally you should load each lpm log into a new neo4j database

To keep the size of the graph reasonable it only adds nodes that come from the "httpd" command.