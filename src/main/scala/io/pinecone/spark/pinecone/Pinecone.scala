package io.pinecone.spark.pinecone

import org.apache.spark.sql.connector.catalog.{Table, TableProvider}
import org.apache.spark.sql.connector.expressions.Transform
import org.apache.spark.sql.sources.DataSourceRegister
import org.apache.spark.sql.types.StructType
import org.apache.spark.sql.util.CaseInsensitiveStringMap

import java.util

case class Pinecone() extends TableProvider {
  override def inferSchema(options: CaseInsensitiveStringMap): StructType =
    COMMON_SCHEMA

  override def getTable(
      schema: StructType,
      partitioning: Array[Transform],
      properties: util.Map[String, String]
  ): Table = {
    val pineconeOptions = new PineconeOptions(new CaseInsensitiveStringMap(properties))
    PineconeIndex(pineconeOptions)
  }
}
