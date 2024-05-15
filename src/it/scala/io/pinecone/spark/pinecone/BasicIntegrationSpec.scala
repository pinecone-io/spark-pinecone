package io.pinecone.spark.pinecone

import org.apache.spark.SparkConf
import org.apache.spark.sql.{SaveMode, SparkSession}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

class BasicIntegrationSpec extends AnyFlatSpec with should.Matchers {
  "Run" should "just work" in {
    val conf = new SparkConf()
      .setMaster("local[*]")
    val spark = SparkSession.builder().config(conf).getOrCreate()

    val df = spark.read
      .option("multiLine", value = true)
      .option("mode", "PERMISSIVE")
      .schema(COMMON_SCHEMA)
      .json("src/it/resources/sample.jsonl")
      .repartition(2)

    df.count() should be(7)

    val pineconeOptions = Map(
      PineconeOptions.PINECONE_API_KEY_CONF -> System.getenv("PINECONE_API_KEY"),
      PineconeOptions.PINECONE_INDEX_NAME_CONF -> System.getenv("PINECONE_INDEX")
    )

    df.write
      .format("io.pinecone.spark.pinecone.Pinecone")
      .options(pineconeOptions)
      .mode(SaveMode.Append)
      .save()
  }
}
