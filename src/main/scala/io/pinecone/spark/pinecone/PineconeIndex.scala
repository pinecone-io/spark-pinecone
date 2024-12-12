package io.pinecone.spark.pinecone

import org.apache.spark.sql.connector.catalog.{SupportsWrite, TableCapability}
import org.apache.spark.sql.connector.write.{LogicalWriteInfo, WriteBuilder}
import org.apache.spark.sql.types.StructType

import scala.collection.JavaConverters._
import java.util

case class PineconeIndex(pineconeOptions: PineconeOptions) extends SupportsWrite {
  override def newWriteBuilder(info: LogicalWriteInfo): WriteBuilder =
    PineconeWriteBuilder(pineconeOptions)

  override def schema(): StructType = COMMON_SCHEMA

  override def capabilities(): util.Set[TableCapability] = Set(
    TableCapability.BATCH_WRITE,
    TableCapability.STREAMING_WRITE
  ).asJava

  override def name(): String = pineconeOptions.indexName
}
