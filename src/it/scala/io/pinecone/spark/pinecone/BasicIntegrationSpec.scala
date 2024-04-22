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
      PineconeOptions.PINECONE_API_KEY_CONF -> "5754d57b-68e3-45fb-8803-e83fce92f354",
      PineconeOptions.PINECONE_ENVIRONMENT_CONF -> "us-east-1-aws",
      PineconeOptions.PINECONE_PROJECT_NAME_CONF -> "f8e8d52",
      PineconeOptions.PINECONE_INDEX_NAME_CONF -> "test1"
    )

    df.count() should be(7)

    // If env variable is set run tests else skip
//    if (System.getenv("TEST_RUNNING_INDEX") != null) {
      df.write
        .format("io.pinecone.spark.pinecone.Pinecone")
        .options(pineconeOptions)
        .mode(SaveMode.Append)
        .save()
//    }
  }
}
