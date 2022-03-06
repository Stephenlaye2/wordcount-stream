ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.11.12"
val sparkVersion = "2.3.2"
val sparkDependencies = Seq(
  "org.apache.spark" %% "spark-core" % sparkVersion,
  "org.apache.spark" %% "spark-sql" % sparkVersion,
  "org.apache.spark" %% "spark-streaming" % sparkVersion,
  "org.apache.spark" %% "spark-streaming-kafka-0-10" % sparkVersion,
  "org.apache.spark" %% "spark-sql-kafka-0-10" % sparkVersion,
  "com.datastax.spark" %% "spark-cassandra-connector" % sparkVersion,
)
// https://mvnrepository.com/artifact/org.apache.spark/spark-sql-kafka-0-10
//libraryDependencies += "org.apache.spark" %% "spark-sql-kafka-0-10" % "2.3.2" % Test
// https://mvnrepository.com/artifact/com.datastax.spark/spark-cassandra-connector
//libraryDependencies += "com.datastax.spark" %% "spark-cassandra-connector" % "2.3.2"


// https://mvnrepository.com/artifact/org.apache.spark
libraryDependencies ++= sparkDependencies

lazy val root = (project in file("."))
  .settings(
    name := "SparkStreaming"
  )
