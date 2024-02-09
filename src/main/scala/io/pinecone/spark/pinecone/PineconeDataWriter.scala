package io.pinecone.spark.pinecone

import io.pinecone.proto.{SparseValues, UpsertRequest, Vector => PineconeVector}
import io.pinecone.{PineconeClient, PineconeClientConfig, PineconeConnection, PineconeConnectionConfig}
import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.connector.write.{DataWriter, WriterCommitMessage}
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import scala.collection.mutable

case class PineconeDataWriter(
    partitionId: Int,
    taskId: Long,
    options: PineconeOptions
) extends DataWriter[InternalRow]
    with Serializable {
  private val log = LoggerFactory.getLogger(getClass)
  private val config = new PineconeClientConfig()
    .withApiKey(options.apiKey)
    .withEnvironment(options.environment)
    .withProjectName(options.projectName)
    .withServerSideTimeoutSec(10)

  private val pineconeClient = new PineconeClient(config)
  private val conn: PineconeConnection =
    pineconeClient.connect(new PineconeConnectionConfig().withIndexName(options.indexName));

  private var upsertBuilderMap      = mutable.Map[String, UpsertRequest.Builder]()
  private var currentVectorsInBatch = 0
  private var totalVectorSize       = 0

  private val maxBatchSize = options.maxBatchSize

  // Reporting vars
  var totalVectorsWritten = 0

  override def write(record: InternalRow): Unit = {
    try {
      val id = record.getUTF8String(0).toString
      val namespace = if(!record.isNullAt(1)) record.getUTF8String(1).toString else ""
      val values = record.getArray(2).toFloatArray().map(float2Float).toIterable

      if (id.length > MAX_ID_LENGTH) {
        throw VectorIdTooLongException(id)
      }

      val vectorBuilder = PineconeVector
        .newBuilder()
        .setId(id)

      if (values.nonEmpty) {
        vectorBuilder.addAllValues(values.asJava)
      }

      if (!record.isNullAt(3)) {
        val metadata = record.getUTF8String(3).toString
        val metadataStruct = parseAndValidateMetadata(id, metadata)
        vectorBuilder.setMetadata(metadataStruct)
      }

      if (!record.isNullAt(4)) {
        val sparseVectorStruct = record.getStruct(4, 2)
        if (!sparseVectorStruct.isNullAt(0) && !sparseVectorStruct.isNullAt(1)) {
          val sparseId = sparseVectorStruct.getArray(0).toLongArray().map(_.toInt).map(int2Integer).toIterable
          val sparseValues = sparseVectorStruct.getArray(1).toFloatArray().map(float2Float).toIterable

          val sparseDataBuilder = SparseValues.newBuilder()
            .addAllIndices(sparseId.asJava)
            .addAllValues(sparseValues.asJava)

          vectorBuilder.setSparseValues(sparseDataBuilder.build())
        }
      }

      val vector = vectorBuilder
        .build()

      if ((currentVectorsInBatch == maxBatchSize) ||
        (totalVectorSize + vector.getSerializedSize >= MAX_REQUEST_SIZE) // If the vector will push the request over the size limit
      ) {
        flushBatchToIndex()
      }

      val builder = upsertBuilderMap
        .getOrElseUpdate(
          namespace, {
            UpsertRequest.newBuilder().setNamespace(namespace)
          }
        )

      builder.addVectors(vector)
      upsertBuilderMap.update(namespace, builder)
      currentVectorsInBatch += 1
      totalVectorSize += vector.getSerializedSize
    } catch {
      case e: NullPointerException =>
        log.error(s"Null values in rows: ${e.getMessage}")
        throw NullValueException("")
    }
  }

  override def commit(): WriterCommitMessage = {
    flushBatchToIndex()

    log.debug(s"taskId=$taskId partitionId=$partitionId totalVectorsUpserted=$totalVectorsWritten")

    PineconeCommitMessage(totalVectorsWritten)
  }

  override def abort(): Unit = {
    log.error(
      s"PineconeDataWriter(taskId=$taskId, partitionId=$partitionId) encountered an unhandled error and is shutting down"
    )
    cleanup()
  }

  override def close(): Unit = {
    cleanup()
  }

  /** Frees up all resources before the Writer is shutdown
    */
  private def cleanup(): Unit = {
    conn.close()
  }

  /** Sends all data pinecone and resets the Writer's state.
    */
  private def flushBatchToIndex(): Unit = {
    log.debug(s"Sending ${upsertBuilderMap.size} requests to Pinecone index")
    for (builder <- upsertBuilderMap.values) {
      val request  = builder.build()
      val response = conn.getBlockingStub.upsert(request)
      log.debug(s"Upserted ${response.getUpsertedCount} vectors to ${options.indexName}")
      totalVectorsWritten += response.getUpsertedCount
    }

    log.debug(s"Upsert operation was successful")

    upsertBuilderMap = mutable.Map()
    currentVectorsInBatch = 0
    totalVectorSize = 0
  }
}
