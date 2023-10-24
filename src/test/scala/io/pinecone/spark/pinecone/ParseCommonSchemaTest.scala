package io.pinecone.spark.pinecone

import org.apache.spark.sql.SparkSession
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

class ParseCommonSchemaTest extends AnyFlatSpec with should.Matchers {
  private val spark: SparkSession = SparkSession.builder()
    .appName("SchemaValidationTest")
    .master("local[2]")
    .getOrCreate()

  private val inputFilePath = "/Users/rohan.s/Documents/sdk/spark-pinecone/src/test/resources"

  private val apiKey = "some_api_key"
  private val environment = "us-east4-gcp"
  private val projectName = "f8e8d52"
  private val indexName = "step-test"

  private val pineconeOptions: Map[String, String] = Map(
    PineconeOptions.PINECONE_API_KEY_CONF -> apiKey,
    PineconeOptions.PINECONE_ENVIRONMENT_CONF -> environment,
    PineconeOptions.PINECONE_PROJECT_NAME_CONF -> projectName,
    PineconeOptions.PINECONE_INDEX_NAME_CONF -> indexName
  )

  def afterAll(): Unit = {
    if (spark != null) {
      spark.stop()
    }
  }

  def testInvalidJSON(file: String, testName: String): Unit = {
    it should testName in {
      val df = spark.read
        .option("multiLine", value = true)
        .option("mode", "PERMISSIVE")
        .schema(COMMON_SCHEMA)
        .json(file)
        .repartition(2)
      an[org.apache.spark.sql.AnalysisException] should be thrownBy {
        df.write
          .options(pineconeOptions)
          .format("io.pinecone.spark.pinecone.Pinecone")
          .save()
      }
    }
  }

  // Use the common test function for each test case
  testInvalidJSON(s"$inputFilePath/invalidUpsertInput1.jsonl",
    "throw exception for missing id")
    testInvalidJSON(s"$inputFilePath/invalidUpsertInput2.jsonl",
      "throw exception for missing values")
    testInvalidJSON(s"$inputFilePath/invalidUpsertInput3.jsonl",
      "throw exception for missing sparse vector indices")
    testInvalidJSON(s"$inputFilePath/invalidUpsertInput4.jsonl",
      "throw exception for missing sparse vector values")
}
