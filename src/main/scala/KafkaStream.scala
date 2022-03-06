import com.datastax.spark.connector._
import com.datastax.spark.connector.cql.CassandraConnector
import org.apache.spark.SparkConf
import org.apache.spark.sql.functions.{col, from_json}
import org.apache.spark.sql.types.{IntegerType, LongType, StringType, StructType}
import org.apache.spark.sql.{ForeachWriter, SparkSession}

import scala.util.parsing.json._

class CassandraSinkForeach(spark: SparkSession) extends ForeachWriter[org.apache.spark.sql.Row] {
  // This class implements the interface ForeachWriter, which has methods that get called
  // whenever there is a sequence of rows generated as output

  val connector = CassandraConnector(spark.sparkContext.getConf)

  val keyspace = "capstone"
  val table = "tweet_stream"
  def open(partitionId: Long, version: Long): Boolean = {
    // open connection
    println(s"Open connection")
    true
  }

  def process(record: org.apache.spark.sql.Row) = {
    println(s"Process new $record")
    connector.withSessionDo(session =>
      session.execute(s"""
       insert into ${keyspace}.${table} (id, text, created_at, screen_name,
       followers_count, favourite_count, retweet_count)
       values(${record(0)}, '${record(1)}', '${record(2)}', '${record(3)}',
       ${record(4)}, ${record(5)}, ${record(6)})""")
    )
  }

  def close(errorOrNull: Throwable): Unit = {
    // close the connection
    println(s"Close connection")
  }
}

object KafkaStream extends App {
  val conf = new SparkConf().setAppName("KafkaStream")
    .set("spark.cassandra.connection.host", "localhost")
    .set("spark.streaming.stopGracefullyOnShutdown", "true")
  val spark = SparkSession.builder()
    .master("local[3]")
    .config(conf)
    .getOrCreate()
//    .config("spark.sql.shuffle.partitions", 5)
  import spark.implicits._
  spark.sparkContext.setLogLevel("ERROR")
  val df = spark.readStream.format("kafka")
    .option("kafka.bootstrap.servers", "localhost:9092")
    .option("subscribe", "capstone_tweet")
    .option("startingOffsets", "latest").load()
//  df.printSchema()

  val dataSchema = new StructType()
      .add("id", LongType)
      .add("text", StringType)
      .add("created_at", StringType)
      .add("screen_name", StringType)
      .add("followers_count", IntegerType)
      .add("favourite_count", IntegerType)
      .add("retweet_count", IntegerType)

  val stringDF = df.selectExpr("CAST(value AS STRING)").as[String]
  /* Method 1
//  val tweetDF = df.select(from_json($"value".cast(StringType), dataSchema).as("data")).select(col("data.*"))
   */
//  Method 2
  val tweetDF = stringDF.withColumn("value", from_json(col("value"), dataSchema))
    .select(col("value.*"))
//  val newDF = tweetDF.foreachPartition(row => println(row))

//  tweetDF.rdd.saveToCassandra(keyspace, table)
  tweetDF.writeStream
    .queryName("KafkaToCassandraForeach")
    .outputMode("append")
    .foreach(new CassandraSinkForeach(spark))
    .start()
    .awaitTermination()
  tweetDF.writeStream
    .format("console")
    .outputMode("update")
    .option("truncate", "true")
    .start()
    .awaitTermination()


}
