package io.pinecone.spark.pinecone

import org.apache.spark.sql.connector.write.{LogicalWriteInfo, Write, WriteBuilder}

case class PineconeWriteBuilder(pineconeOptions: PineconeOptions)
    extends WriteBuilder
    with Serializable {
  override def build: Write = PineconeWrite(pineconeOptions)
}
