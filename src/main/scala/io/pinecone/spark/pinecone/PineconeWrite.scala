package io.pinecone.spark.pinecone

import org.apache.spark.sql.connector.write.streaming.StreamingWrite
import org.apache.spark.sql.connector.write.{BatchWrite, Write}

case class PineconeWrite(pineconeOptions: PineconeOptions) extends Write with Serializable {
  override def toBatch: BatchWrite = PineconeBatchWriter(pineconeOptions)

  override def toStreaming: StreamingWrite = PineconeStreamingWriter(pineconeOptions)
}
