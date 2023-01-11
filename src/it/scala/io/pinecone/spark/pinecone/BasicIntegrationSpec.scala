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

    val pineconeOptions = Map(
      PineconeOptions.PINECONE_API_KEY_CONF      -> System.getenv("PINECONE_API_KEY"),
      PineconeOptions.PINECONE_ENVIRONMENT_CONF  -> System.getenv("PINECONE_ENVIRONMENT"),
      PineconeOptions.PINECONE_PROJECT_NAME_CONF -> System.getenv("PINECONE_PROJECT"),
      PineconeOptions.PINECONE_INDEX_NAME_CONF   -> System.getenv("PINECONE_INDEX")
    )

    df.count() should be(7)

//    If env variable is set run tests else skip
    if(System.getenv("TEST_RUNNING_INDEX") != null) {
      df.write
        .format("io.pinecone.spark.pinecone")
        .options(pineconeOptions)
        .mode(SaveMode.Overwrite)
        .save()
    }
  }

}
