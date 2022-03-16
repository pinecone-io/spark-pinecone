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
      .schema(COMMON_SCHEMA)
      .json("src/it/resources/sample.jsonl")
      .repartition(2)

    val pineconeOptions = Map(
      PineconeOptions.PINECONE_API_KEY_CONF      -> System.getenv("PINECONE_API_KEY"),
      PineconeOptions.PINECONE_ENVIRONMENT_CONF  -> System.getenv("PINECONE_ENVIRONMENT"),
      PineconeOptions.PINECONE_PROJECT_NAME_CONF -> System.getenv("PINECONE_PROJECT"),
      PineconeOptions.PINECONE_INDEX_NAME_CONF   -> System.getenv("PINECONE_INDEX")
    )

    df.write
      .options(pineconeOptions)
      .format("io.pinecone.spark.pinecone.Pinecone")
      .mode(SaveMode.Append)
      .save()

    df.count() should be(7)
  }

}
