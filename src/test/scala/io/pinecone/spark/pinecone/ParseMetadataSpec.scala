package io.pinecone.spark.pinecone

import com.fasterxml.jackson.core.JsonParseException
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

class ParseMetadataSpec extends AnyFlatSpec with should.Matchers {
  val mockVectorId = "id"

  it should "throw an InvalidVectorMetadataException if the vector has invalid metadata (list of ints)" in {
    val metadataWithLongId =
      s"""
        |{ "field": [1, 2] }
        |""".stripMargin

    a[InvalidVectorMetadataException] should be thrownBy {
      parseAndValidateMetadata(mockVectorId, metadataWithLongId)
    }
  }

  it should "throw an InvalidVectorMetadataException if the vector has invalid metadata (list with mixed types)" in {
    val metadataWithLongId =
      s"""
         |{ "field": [1, "2"] }
         |""".stripMargin

    a[InvalidVectorMetadataException] should be thrownBy {
      parseAndValidateMetadata(mockVectorId, metadataWithLongId)
    }
  }

  it should "throw an error when the JSON string is invalid JSON (no closing braces)" in {
    val partialJson =
      """
        |{ "hello": "world"
        |""".stripMargin
    a[JsonParseException] should be thrownBy {
      parseAndValidateMetadata(mockVectorId, partialJson)
    }
  }

  it should "throw an error when the JSON string is invalid JSON (Use of single quotes)" in {
    val partialJson =
      """
        |{'hello': "world"}
        |""".stripMargin
    a[JsonParseException] should be thrownBy {
      parseAndValidateMetadata(mockVectorId, partialJson)
    }
  }
}
