# spark-pinecone

The official [pinecone.io](https://pinecone.io) spark connector.

## Features
- Please note that the connector's write operation is not atomic - some vectors might be written while others aren't if the operation is stopped or if it fails. 
In practice this shouldn't cause a serious issue. Pinecone is an idempotent key-value store. Re-running the job will result in the desired state without a need to clear the index or calculate some delta from the source data.
- The client currently only supports batch writing of data into pinecone from a specific schema (see the example below).
If you need to use the connector with a streaming pipeline, it is recommended to use a function like `foreachBatch`.

## Support
This client currently only supports Spark 3.2.0, Scala 2.12.X or 2.13.X and Java 8+.

### Databricks and friends
Due to various libraries provided by Databricks (and other runtimes), we recommend you shade the `com.google.protobuf` and `com.google.common`, due to conflicts with the underlying java client.
We plan to release a shaded version in the future.

## Usage

add the following snippet to your `built.sbt` file:
```scala
libraryDependencies += "io.pinecone" %% "spark-pinecone" % "0.1.0"
```


## Example
```scala
import org.apache.spark.SparkConf
import org.apache.spark.sql.types._
import org.apache.spark.sql.{SaveMode, SparkSession}

object MainApp extends App {
  val conf = new SparkConf()
    .setMaster("local[*]")
  val spark = SparkSession.builder().config(conf).getOrCreate()

  val df = spark.read
    .schema(new StructType()
      .add("id", StringType)
      .add("namespace", StringType)
      .add("vector", ArrayType(FloatType))
      .add("metadata", StringType))
    .json("src/test/resources/sample.jsonl")

  val pineconeOptions = Map(
    "pinecone.apiKey" -> "YOUR_API_KEY_HERE",
    "pinecone.environment" -> "ENV_NAME",
    "pinecone.projectName" -> "PROJECT_NAME_HERE",
    "pinecone.indexName" -> "INDEX_NAME"
  )

  df.write
    .options(pineconeOptions)
    .format("io.pinecone.spark.pinecone.Pinecone")
    .mode(SaveMode.Append)
    .save()
}
```