# spark-pinecone
The official [pinecone.io](https://pinecone.io) spark connector.

## Features
- Please note that the connector's write operation is not atomic - some vectors might be written while others aren't if the operation is stopped or if it fails. 
In practice this shouldn't cause a serious issue. Pinecone is an idempotent key-value store. Re-running the job will result in the desired state without a need to clear the index or calculate some delta from the source data.
- The client currently only supports batch writing of data into pinecone from a specific schema (see the example below).
If you need to use the connector with a streaming pipeline, it is recommended to use a function like `foreachBatch`.

## Support
This client currently only supports Spark 3.5.0, Scala 2.12.X or 2.13.X and Java 8+.
- For Scala 2.12, use `spark-pinecone_2.12.jar`.
- For Scala 2.13, use `spark-pinecone_2.13.jar`.

Make sure to add the correct JAR file to your project's dependencies according to your Scala version.

### Databricks and friends
Due to various libraries provided by Databricks (and other runtimes), please use the assembly jar from s3 for now.
S3 path for assembly jar: s3://pinecone-jars/spark-pinecone-uberjar.jar

## Usage
add the following snippet to your `built.sbt` file:
```scala
libraryDependencies += "io.pinecone" %% "spark-pinecone" % "<spark_pinecone_sdk_version>" // example of the spark_pinecone_sdk_version => 0.1.4 and you can get the latest version from maven central or release notes.
```

## Example
To connect to Pinecone with Spark you'll have to retrieve the following information from [your Pinecone console](https://app.pinecone.io)
1. API Key: navigate to your project and click the "API Keys" button on the sidebar.
2. `environment` & `projectName`: check the browser url to fetch the environment. `https://app.pinecone.io/organizations/[org-id]/projects/[environment]:[project_name]/indexes`

### PySpark
```python
from pyspark import SparkConf
from pyspark.sql import SparkSession
from pyspark.sql.types import StructType, StructField, ArrayType, FloatType, StringType

conf = SparkConf().setMaster("local[*]")
spark = SparkSession.builder().config(conf).getOrCreate()

schema = StructType([
    StructField("id", StringType(), False),
    StructField("values", ArrayType(FloatType()), False),
    StructField("namespace", StringType(), True),
    StructField("metadata", StringType(), True),
])

embeddings = None

df = spark.createDataFrame(data=embeddings, schema=schema)

(
    df.write
    .option("pinecone.apiKey", api_key)
    .option("pinecone.environment", environment)
    .option("pinecone.projectName", project_name)
    .option("pinecone.indexName", index_name)
    .format("io.pinecone.spark.pinecone.Pinecone")
    .mode("append")
    .save()
)
```

### Scala
```scala
import io.pinecone.spark.pinecone.{COMMON_SCHEMA, PineconeOptions}
import org.apache.spark.SparkConf
import org.apache.spark.sql.{SaveMode, SparkSession}

object MainApp extends App {
  val apiKey = "PINECONE_API_KEY"
  val environment = "PINECONE_ENVIRONMENT"
  val projectName = "PINECONE_PROJECT_NAME"
  val indexName = "PINECONE_INDEX_NAME"

  val conf = new SparkConf()
    .setMaster("local[*]")
  val spark = SparkSession.builder().config(conf).getOrCreate()

  val df = spark.read
    .option("multiLine", value = true)
    .option("mode", "PERMISSIVE")
    .schema(COMMON_SCHEMA)
    .json("src/test/resources/sample.jsonl") // path to sample.jsonl

  val pineconeOptions = Map(
    PineconeOptions.PINECONE_API_KEY_CONF -> apiKey,
    PineconeOptions.PINECONE_ENVIRONMENT_CONF -> environment,
    PineconeOptions.PINECONE_PROJECT_NAME_CONF -> projectName,
    PineconeOptions.PINECONE_INDEX_NAME_CONF -> indexName
  )

  df.show(df.count().toInt)

  df.write
    .options(pineconeOptions)
    .format("io.pinecone.spark.pinecone.Pinecone")
    .mode(SaveMode.Append)
    .save()
}
```