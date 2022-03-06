import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession
import org.apache.spark.streaming.{Seconds, StreamingContext}

object FirstStream extends App {
  // Create a local StreamingContext with two working thread and batch interval of 1 second.
  // The master requires 2 cores to prevent from a starvation scenario.
  val conf = new SparkConf().setMaster("local[2]").setAppName("NetworkWordCount")
  val ssc = new StreamingContext(conf = conf, batchDuration = Seconds(60))
  ssc.sparkContext.setLogLevel("ERROR")
//  streamHdfsFile(ssc)
  def streamHdfsFile(ssc: StreamingContext): Unit ={
//    Stream a new file in the BigData directory after the streaming has started
    val lines = ssc.textFileStream("hdfs://localhost:9000/BigData/")
    val words = lines.flatMap(_.split(" "))
    val wordsCount = words.map((_, 1)).reduceByKey(_+_)
    wordsCount.print()
    words.foreachRDD(rdd => {
//      Get the singleton instance of SparkSession
      if(!rdd.isEmpty()) {
        val spark = SparkSession.builder.config(rdd.sparkContext.getConf).getOrCreate()

        import spark.implicits._
        //      convert RDD to DataFrame
        val wordsDataFrame = rdd.toDF("word")
        //      Create a temporary view
        wordsDataFrame.createOrReplaceTempView("words")
        //      Do word count on DataFrame using SQL and print it
        val wordsCountDF = spark.sql("SELECT word, count(*) FROM words GROUP BY word")
        wordsCountDF.show()
      }
    })

    ssc.start()
    ssc.awaitTermination()
  }
  streamWordCount(ssc)

  def streamWordCount(ssc: StreamingContext) {
    //  Create a DStream that will connect to hostname:port, like localhost:9999
    val lines = ssc.socketTextStream("localhost", 9999)
    //  Split each line into words
    val words = lines.flatMap(x => x.split(" "))
    words.foreachRDD(rdd => {
      if(!rdd.isEmpty()){
//        Create new spark session
        val spark = SparkSession.builder.config(rdd.sparkContext.getConf).getOrCreate()
        import spark.implicits._
        val wordDF = rdd.toDF("word")
        wordDF.createOrReplaceTempView("words")
        spark.sql("SELECT word, count(*) FROM words GROUP BY word").show()
      }
    })
    //  Count each word in each batch
    val pairs = words.map((_, 1))
    val wordCount = pairs.reduceByKey(_ + _)
    //  Print the first ten elements of each RDD generated in the DStream to the console
    wordCount.print()
    ssc.start() // Start the streaming computation
    //  ssc.stop()
    ssc.awaitTermination() // Wait for the streaming computation to terminate
  }
}
