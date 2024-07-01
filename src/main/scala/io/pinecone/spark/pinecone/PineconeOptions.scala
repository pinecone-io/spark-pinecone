package io.pinecone.spark.pinecone

import PineconeOptions._
import org.apache.spark.sql.util.CaseInsensitiveStringMap

class PineconeOptions(config: CaseInsensitiveStringMap) extends Serializable {
  private val DEFAULT_BATCH_SIZE = 100

  val maxBatchSize: Int =
    config
      .getInt(PINECONE_BATCH_SIZE_CONF, DEFAULT_BATCH_SIZE)

  val apiKey: String = getKey(PINECONE_API_KEY_CONF, config)
  val indexName: String = getKey(PINECONE_INDEX_NAME_CONF, config)
  val sourceTag: String = getSourceTag(config)

  private def getKey(key: String, config: CaseInsensitiveStringMap): String = {
    Option(config.get(key)).getOrElse(
      throw new RuntimeException(s"Missing required parameter $key")
    )
  }

  private def getSourceTag(config: CaseInsensitiveStringMap): String = {
    val value = Option(config.get(PINECONE_SOURCE_TAG_CONF)).getOrElse("")
    s"spark_$value"
  }
}

object PineconeOptions {
  val PINECONE_BATCH_SIZE_CONF: String   = "pinecone.batchSize"
  val PINECONE_API_KEY_CONF: String      = "pinecone.apiKey"
  val PINECONE_INDEX_NAME_CONF: String   = "pinecone.indexName"
  val PINECONE_SOURCE_TAG_CONF: String   = "pinecone.sourceTag"
}
