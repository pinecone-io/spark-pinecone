package io.pinecone.spark.pinecone

import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.connector.write.{DataWriter, DataWriterFactory, LogicalWriteInfo}

case class PineconeDataWriterFactory(pineconeOptions: PineconeOptions)
    extends DataWriterFactory
    with Serializable {
  override def createWriter(partitionId: Int, taskId: Long): DataWriter[InternalRow] = {
    PineconeDataWriter(partitionId, taskId, pineconeOptions)
  }
}
