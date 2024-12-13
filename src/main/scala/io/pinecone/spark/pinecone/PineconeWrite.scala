package io.pinecone.spark.pinecone

import org.apache.spark.sql.connector.write.{Write, BatchWrite}
import org.apache.spark.sql.connector.write.streaming.StreamingWrite

case class PineconeWrite(pineconeOptions: PineconeOptions) extends Write with Serializable {
  override def toBatch: BatchWrite = PineconeBatchWriter(pineconeOptions)
  override def toStreaming: StreamingWrite = PineconeStreamingWriter(pineconeOptions)
}
