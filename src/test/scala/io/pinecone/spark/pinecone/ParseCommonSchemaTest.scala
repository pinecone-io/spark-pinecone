package io.pinecone.spark.pinecone

import org.apache.spark.sql.{SaveMode, SparkSession}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

class ParseCommonSchemaTest extends AnyFlatSpec with should.Matchers {
  private val spark: SparkSession = SparkSession.builder()
    .appName("SchemaValidationTest")
    .master("local[2]")
    .getOrCreate()

  private val inputFilePath = System.getProperty("user.dir") + "/src/test/resources"

  private val apiKey = "some_api_key"
  private val indexName = "step-test"

  private val pineconeOptions: Map[String, String] = Map(
    PineconeOptions.PINECONE_API_KEY_CONF -> apiKey,
    PineconeOptions.PINECONE_INDEX_NAME_CONF -> indexName
  )

  def afterAll(): Unit = {
    if (spark != null) {
      spark.stop()
    }
  }

  def testInvalidJSON(file: String, testName: String): Unit = {
    it should testName in {
      val sparkException = intercept[org.apache.spark.SparkException] {
        val df = spark.read
          .option("multiLine", value = true)
          .option("mode", "PERMISSIVE")
          .schema(COMMON_SCHEMA)
          .json(file)
          .repartition(2)

        df.write
          .options(pineconeOptions)
          .format("io.pinecone.spark.pinecone.Pinecone")
          .mode(SaveMode.Append)
          .save()
      }
      sparkException
        .getCause
        .toString should include("java.lang.NullPointerException: Null value appeared in non-nullable field:")
    }
  }

  def testInvalidSparseIndices(file: String, testName: String): Unit = {
    it should testName in {
      val sparkException = intercept[org.apache.spark.SparkException] {
        val df = spark.read
          .option("multiLine", value = true)
          .option("mode", "PERMISSIVE")
          .schema(COMMON_SCHEMA)
          .json(file)
          .repartition(2)

        df.write
          .options(pineconeOptions)
          .format("io.pinecone.spark.pinecone.Pinecone")
          .mode(SaveMode.Append)
          .save()
      }
      sparkException
        .getCause
        .toString should include("java.lang.IllegalArgumentException: Sparse indices are out of range for unsigned 32-bit integers.")
    }
  }

  testInvalidJSON(s"$inputFilePath/invalidUpsertInput1.jsonl",
    "throw exception for missing id")
  testInvalidJSON(s"$inputFilePath/invalidUpsertInput2.jsonl",
    "throw exception for missing values")
  testInvalidJSON(s"$inputFilePath/invalidUpsertInput3.jsonl",
    "throw exception for missing sparse vector indices and values if sparse_values is defined")
  testInvalidJSON(s"$inputFilePath/invalidUpsertInput4.jsonl",
    "throw exception for missing sparse vector indices if sparse_values and its values are defined")
  testInvalidJSON(s"$inputFilePath/invalidUpsertInput5.jsonl",
    "throw exception for missing sparse vector values if sparse_values and its indices are defined")
  testInvalidJSON(s"$inputFilePath/invalidUpsertInput6.jsonl",
    "throw exception for null in sparse vector indices")
  testInvalidJSON(s"$inputFilePath/invalidUpsertInput7.jsonl",
    "throw exception for null in sparse vector values")
  testInvalidSparseIndices(s"$inputFilePath/invalidUpsertInput8.jsonl",
    "throw exception for invalid sparse vector indices")
  testInvalidSparseIndices(s"$inputFilePath/invalidUpsertInput9.jsonl",
    "throw exception for invalid sparse vector indices2")
}
