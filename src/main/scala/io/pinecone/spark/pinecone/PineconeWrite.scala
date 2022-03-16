package io.pinecone.spark.pinecone

import org.apache.spark.sql.connector.write.{Write, BatchWrite}

case class PineconeWrite(pineconeOptions: PineconeOptions) extends Write with Serializable {
  override def toBatch: BatchWrite = PineconeBatchWriter(pineconeOptions)
}
