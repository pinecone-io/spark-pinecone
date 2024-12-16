package io.pinecone.spark.pinecone

import org.apache.spark.sql.SparkSession

object StreamUpsertExample {

  def main(args: Array[String]): Unit = {

    val spark = SparkSession.builder().appName("StreamUpsertExample")
      .config("spark.sql.shuffle.partitions", 3)
      .master("local")
      .getOrCreate()

    val lines = spark.readStream
      .option("multiLine", value = true)
      .option("mode", "PERMISSIVE")
      .schema(COMMON_SCHEMA)
      .json("src/it/resources/")

    val pineconeOptions = Map(
      PineconeOptions.PINECONE_API_KEY_CONF -> System.getenv("PINECONE_API_KEY"),
      PineconeOptions.PINECONE_INDEX_NAME_CONF -> System.getenv("PINECONE_INDEX"),
      PineconeOptions.PINECONE_SOURCE_TAG_CONF -> System.getenv("PINECONE_SOURCE_TAG")
    )

    val query = lines
      .writeStream
      .format("io.pinecone.spark.pinecone.Pinecone")
      .options(pineconeOptions)
      .option("checkpointLocation", "path/to/checkpoint/dir")
      .outputMode("append")
      .start()

    query.awaitTermination()
  }
}