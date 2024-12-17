package io.pinecone.spark.pinecone

import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.connector.write.{DataWriter, DataWriterFactory}
import org.apache.spark.sql.connector.write.streaming.StreamingDataWriterFactory

case class PineconeDataWriterFactory(pineconeOptions: PineconeOptions)
  extends DataWriterFactory
    with StreamingDataWriterFactory
    with Serializable {
  override def createWriter(partitionId: Int, taskId: Long): DataWriter[InternalRow] = {
    PineconeDataWriter(partitionId, taskId, pineconeOptions)
  }

  override def createWriter(partitionId: Int, taskId: Long, epochId: Long): DataWriter[InternalRow] = {
    PineconeDataWriter(partitionId, taskId, pineconeOptions)
  }
}
