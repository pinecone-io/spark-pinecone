package io.pinecone.spark

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.protobuf.{ListValue, Struct, Value}
import org.apache.spark.sql.connector.write.WriterCommitMessage
import org.apache.spark.sql.types.{ArrayType, FloatType, IntegerType, StringType, StructField, StructType}

import scala.collection.JavaConverters._

package object pinecone {
  val COMMON_SCHEMA: StructType =
    new StructType()
      .add("id", StringType, nullable = false)
      .add("namespace", StringType, nullable = true)
      .add("values", ArrayType(FloatType, containsNull = false), nullable = false)
      .add("metadata", StringType, nullable = true)
      .add("sparse_values", StructType(
        Array(
          StructField("indices", ArrayType(IntegerType, containsNull = false), nullable = false),
          StructField("values", ArrayType(FloatType, containsNull = false), nullable = false)
        )
      ), nullable = true)

  private[pinecone] val MAX_ID_LENGTH = 512
  private[pinecone] val MAX_METADATA_SIZE = 5 * math.pow(10, 3) // 5KB
  private[pinecone] val MAX_REQUEST_SIZE = 2 * math.pow(10, 6) // 2MB

  /** Parses the metadata of a vector from a JSON string representation to a ProtoBuf struct
   *
   * @param vectorId
   * the ID of the vector, used for error reporting purposes
   * @param metadataStr
   * \- the JSON string representing the vector's metadata
   * @return
   */
  private[pinecone] def parseAndValidateMetadata(vectorId: String, metadataStr: String): Struct = {
    val structBuilder = Struct.newBuilder()
    val mapper = new ObjectMapper()

    val jsonTree = mapper.readTree(metadataStr)

    for (jsonField <- jsonTree.fields().asScala) {
      val key = jsonField.getKey
      val value = jsonField.getValue

      if (value.isTextual) {
        structBuilder.putFields(key, Value.newBuilder().setStringValue(value.asText()).build())
      } else if (value.isNumber) {
        structBuilder.putFields(key, Value.newBuilder().setNumberValue(value.floatValue()).build())
      } else if (value.isBoolean) {
        structBuilder.putFields(key, Value.newBuilder().setBoolValue(value.booleanValue()).build())
      } else if (value.isArray && value.elements().asScala.toArray.forall(_.isTextual)) {
        val arrayElements = value.elements().asScala.toArray
        val listValueBuilder = ListValue.newBuilder()
        listValueBuilder.addAllValues(
          arrayElements
            .map(element => Value.newBuilder().setStringValue(element.textValue()).build())
            .toIterable
            .asJava
        )

        structBuilder.putFields(
          key,
          Value.newBuilder().setListValue(listValueBuilder.build()).build()
        )
      } else {
        throw InvalidVectorMetadataException(vectorId, key)
      }
    }

    val finalStruct = structBuilder.build()

    if (finalStruct.getSerializedSize >= MAX_METADATA_SIZE) {
      throw PineconeMetadataTooLarge(vectorId)
    }

    finalStruct
  }

  case class PineconeCommitMessage(vectorCount: Int) extends WriterCommitMessage

  trait PineconeException extends Exception {
    def getMessage: String

    def vectorId: String
  }

  case class InvalidVectorMetadataException(vectorId: String, jsonKey: String)
    extends PineconeException {
    override def getMessage: String =
      s"Vector with ID '$vectorId' has invalid metadata field '$jsonKey'. Please refer to the Pinecone.io docs for a longer explanation."
  }

  case class PineconeMetadataTooLarge(vectorId: String) extends PineconeException {
    override def getMessage: String =
      s"Metadata for vector with ID '$vectorId' exceeded the maximum metadata size of $MAX_METADATA_SIZE bytes"
  }

  case class VectorIdTooLongException(vectorId: String) extends PineconeException {
    override def getMessage: String =
      s"Vector with ID starting with ${vectorId.substring(0, 8)}. Must be 512 characters or less. actual: ${vectorId.length}"
  }

  case class NullValueException(vectorId: String) extends PineconeException {
    override def getMessage: String = "Null id or value column found in row. Please ensure id and values are not null."
  }
}
