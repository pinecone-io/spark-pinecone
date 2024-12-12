package io.pinecone.spark.pinecone

import org.apache.spark.sql.connector.write.{PhysicalWriteInfo, WriterCommitMessage}
import org.apache.spark.sql.connector.write.streaming.{StreamingDataWriterFactory, StreamingWrite}
import org.slf4j.LoggerFactory

case class PineconeStreamingWriter(pineconeOptions: PineconeOptions) extends StreamingWrite {
  private val log = LoggerFactory.getLogger(getClass)

  override def createStreamingWriterFactory(info: PhysicalWriteInfo): StreamingDataWriterFactory = {
    PineconeDataWriterFactory(pineconeOptions)
  }

  override def commit(epochId: Long, messages: Array[WriterCommitMessage]): Unit = {
    val totalVectorsWritten = messages.map(_.asInstanceOf[PineconeCommitMessage].vectorCount).sum

    log.info(
      s"""Epoch $epochId: A total of $totalVectorsWritten vectors written to index "${pineconeOptions.indexName}""""
    )
  }

  override def abort(epochId: Long, messages: Array[WriterCommitMessage]): Unit = {
    log.error(s"Epoch $epochId: Write operation aborted")
  }

  // ToDo: confirm if it can be set to false for the streaming case
  //  override def useCommitCoordinator(): Boolean = false

  override def toString: String = s"PineconeStreamingWriter(index=${pineconeOptions.indexName})"
}