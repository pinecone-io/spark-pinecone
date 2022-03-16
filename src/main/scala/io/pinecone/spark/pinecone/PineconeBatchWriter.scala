package io.pinecone.spark.pinecone

import org.apache.spark.sql.connector.write.{
  BatchWrite,
  DataWriterFactory,
  PhysicalWriteInfo,
  WriterCommitMessage
}
import org.slf4j.LoggerFactory

case class PineconeBatchWriter(pineconeOptions: PineconeOptions) extends BatchWrite {
  private val log = LoggerFactory.getLogger(getClass)

  override def createBatchWriterFactory(info: PhysicalWriteInfo): DataWriterFactory = {
    PineconeDataWriterFactory(pineconeOptions)
  }

  override def commit(messages: Array[WriterCommitMessage]): Unit = {
    val totalVectorsWritten = messages.map(_.asInstanceOf[PineconeCommitMessage].vectorCount).sum

    log.info(
      s"""A total of $totalVectorsWritten vectors written to index "${pineconeOptions.indexName}""""
    )
  }

  override def abort(messages: Array[WriterCommitMessage]): Unit = {}

  // Pinecone is inherently a key-value store, so running an upsert operation with
  // vectors with the same ID due to speculative execution or other mechanisms will result
  // in the same end result. It is the pipeline's developer responsibility to ensure that the initial
  // data doesn't have any vectors with shared IDs, a case which might result in a different outcome
  // each run.
  override def useCommitCoordinator(): Boolean = false

  override def toString: String = s"""PineconeBatchWriter(index="${pineconeOptions.indexName}")"""

}
