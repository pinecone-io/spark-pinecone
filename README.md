# spark-pinecone
The official [pinecone.io](https://pinecone.io) spark connector.

## Features
- Please note that the connector's write operation is not atomic - some vectors might be written while others aren't if the operation is stopped or if it fails. 
In practice this shouldn't cause a serious issue. Pinecone is an idempotent key-value store. Re-running the job will result in the desired state without a need to clear the index or calculate some delta from the source data.
- The client currently only supports batch writing of data into pinecone from a specific schema (see the example below).
If you need to use the connector with a streaming pipeline, it is recommended to use a function like `foreachBatch`.

## Support
This client currently supports Spark 3.5.0, Scala 2.12.X or 2.13.X and Java 8+.
- For Scala 2.12, use `spark-pinecone_2.12.jar`: https://central.sonatype.com/artifact/io.pinecone/spark-pinecone_2.12.
- For Scala 2.13, use `spark-pinecone_2.13.jar`: https://central.sonatype.com/artifact/io.pinecone/spark-pinecone_2.13.

Make sure to add the correct JAR file to your project's dependencies according to your Scala version.

### Databricks and friends
Due to various libraries provided by Databricks (and other runtimes), please use the assembly jar from s3 for now.
S3 path for assembly jar: 
1. v1.1.0 (latest): s3://pinecone-jars/1.1.0/spark-pinecone-uberjar.jar
2. v1.0.0 (latest): s3://pinecone-jars/1.0.0/spark-pinecone-uberjar.jar 
3. v0.2.2: s3://pinecone-jars/0.2.2/spark-pinecone-uberjar.jar
4. v0.2.1: s3://pinecone-jars/0.2.1/spark-pinecone-uberjar.jar
5. v0.1.4: s3://pinecone-jars/spark-pinecone-uberjar.jar

## Example
To connect to Pinecone with Spark you'll have to retrieve the api key from [your Pinecone console](https://app.pinecone.io). 
Navigate to your project and click the "API Keys" button on the sidebar.

### PySpark
```python
from pyspark import SparkConf
from pyspark.sql import SparkSession
from pyspark.sql.types import StructType, StructField, ArrayType, FloatType, StringType, LongType

# Your API key, environment, project name, and index name
api_key = "PINECONE_API_KEY"
index_name = "PINECONE_INDEX_NAME"

COMMON_SCHEMA = StructType([
    StructField("id", StringType(), False),
    StructField("namespace", StringType(), True),
    StructField("values", ArrayType(FloatType(), False), False),
    StructField("metadata", StringType(), True),
    StructField("sparse_values", StructType([
        StructField("indices", ArrayType(LongType(), False), False),
        StructField("values", ArrayType(FloatType(), False), False)
    ]), True)
])

# Initialize Spark
spark = SparkSession.builder.getOrCreate()

# Read the file and apply the schema
df = spark.read \
    .option("multiLine", value = True) \
    .option("mode", "PERMISSIVE") \
    .schema(COMMON_SCHEMA) \
    .json("/FileStore/tables/sample-4.jsonl")

# Show if the read was successful
df.show()

df.write \
    .option("pinecone.apiKey", api_key) \
    .option("pinecone.indexName", index_name) \
    .format("io.pinecone.spark.pinecone.Pinecone") \
    .mode("append") \
    .save()
```

### Scala
```scala
import io.pinecone.spark.pinecone.{COMMON_SCHEMA, PineconeOptions}
import org.apache.spark.SparkConf
import org.apache.spark.sql.{SaveMode, SparkSession}

object MainApp extends App {
  val apiKey = "PINECONE_API_KEY"
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