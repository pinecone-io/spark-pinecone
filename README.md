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
Due to various libraries provided by Databricks, please use the assembly jar from s3 to avoid dependency conflict.
S3 path for assembly jar:
1. v1.2.0 (latest): s3://pinecone-jars/1.2.0/spark-pinecone-uberjar.jar
2. v1.1.0: s3://pinecone-jars/1.1.0/spark-pinecone-uberjar.jar
3. v1.0.0: s3://pinecone-jars/1.0.0/spark-pinecone-uberjar.jar 
4. v0.2.2: s3://pinecone-jars/0.2.2/spark-pinecone-uberjar.jar
5. v0.2.1: s3://pinecone-jars/0.2.1/spark-pinecone-uberjar.jar
6. v0.1.4: s3://pinecone-jars/spark-pinecone-uberjar.jar

## Example
To connect to Pinecone with Spark you'll have to retrieve the api key from [your Pinecone console](https://app.pinecone.io). 
Navigate to your project and click the "API Keys" button on the sidebar. The sample.jsonl file used in the examples below
can be found [here](https://github.com/pinecone-io/spark-pinecone/blob/main/src/it/resources/sample.jsonl).

### Batch upsert
Below are examples in Python and Scala for batch upserting vectors in Pinecone DB.

#### Python
```python
from pyspark import SparkConf
from pyspark.sql import SparkSession
from pyspark.sql.types import StructType, StructField, ArrayType, FloatType, StringType, LongType

# Your API key and index name
api_key = "PINECONE_API_KEY"
index_name = "PINECONE_INDEX_NAME"
source_tag = "PINECONE_SOURCE_TAG"

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
    .json("src/test/resources/sample.jsonl")

# Show if the read was successful
df.show()

# Write the dataFrame to Pinecone in batches 
df.write \
    .option("pinecone.apiKey", api_key) \
    .option("pinecone.indexName", index_name) \
    .option("pinecone.sourceTag", source_tag) \
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
  // Your API key and index name
  val apiKey = "PINECONE_API_KEY"
  val indexName = "PINECONE_INDEX_NAME"
  val sourceTag = "PINECONE_SOURCE_TAG"

  // Configure Spark to run locally with all available cores
  val conf = new SparkConf()
    .setMaster("local[*]")

  // Create a Spark session with the defined configuration
  val spark = SparkSession.builder().config(conf).getOrCreate()

  // Read the JSON file into a DataFrame, applying the COMMON_SCHEMA
  val df = spark.read
    .option("multiLine", value = true)
    .option("mode", "PERMISSIVE")
    .schema(COMMON_SCHEMA)
    .json("src/test/resources/sample.jsonl") // path to sample.jsonl

  // Define Pinecone options as a Map
  val pineconeOptions = Map(
    PineconeOptions.PINECONE_API_KEY_CONF -> apiKey,
    PineconeOptions.PINECONE_INDEX_NAME_CONF -> indexName,
    PineconeOptions.PINECONE_SOURCE_TAG_CONF -> sourceTag
  )

  // Show if the read was successful
  df.show(df.count().toInt)
  
  // Write the DataFrame to Pinecone using the defined options in batches
  df.write
    .options(pineconeOptions)
    .format("io.pinecone.spark.pinecone.Pinecone")
    .mode(SaveMode.Append)
    .save()
}
```


### Stream upsert
Below are examples in Python and Scala for streaming upserts of vectors in Pinecone DB.

#### Python
```python
from pyspark.sql import SparkSession
from pyspark.sql.types import StructType, StructField, ArrayType, FloatType, StringType, LongType
import os

# Your API key and index name
api_key = "PINECONE_API_KEY"
index_name = "PINECONE_INDEX_NAME"
source_tag = "PINECONE_SOURCE_TAG"

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

# Initialize Spark session
spark = SparkSession.builder \
    .appName("StreamUpsertExample") \
    .config("spark.sql.shuffle.partitions", 3) \
    .master("local") \
    .getOrCreate()

# Read the stream of JSON files, applying the schema from the input directory
lines = spark.readStream \
    .option("multiLine", True) \
    .option("mode", "PERMISSIVE") \
    .schema(COMMON_SCHEMA) \
    .json("path/to/input/directory/")

# Write the stream to Pinecone using the defined options
upsert = lines.writeStream \
    .format("io.pinecone.spark.pinecone.Pinecone") \
    .option("pinecone.apiKey", api_key) \
    .option("pinecone.indexName", index_name) \
    .option("pinecone.sourceTag", source_tag) \
    .option("checkpointLocation", "path/to/checkpoint/dir") \
    .outputMode("append") \
    .start()

upsert.awaitTermination()
```

### Scala
```scala
import io.pinecone.spark.pinecone.{COMMON_SCHEMA, PineconeOptions}
import org.apache.spark.SparkConf
import org.apache.spark.sql.{SaveMode, SparkSession}

object MainApp extends App {
  // Your API key and index name
  val apiKey = "PINECONE_API_KEY"
  val indexName = "PINECONE_INDEX_NAME"

  // Create a Spark session
  val spark = SparkSession.builder()
    .appName("StreamUpsertExample")
    .config("spark.sql.shuffle.partitions", 3)
    .master("local")
    .getOrCreate()

  // Read the JSON files into a DataFrame, applying the COMMON_SCHEMA from input directory
  val lines = spark.readStream
    .option("multiLine", value = true)
    .option("mode", "PERMISSIVE")
    .schema(COMMON_SCHEMA)
    .json("path/to/input/directory/")

  // Define Pinecone options as a Map
  val pineconeOptions = Map(
    PineconeOptions.PINECONE_API_KEY_CONF -> System.getenv("PINECONE_API_KEY"),
    PineconeOptions.PINECONE_INDEX_NAME_CONF -> System.getenv("PINECONE_INDEX"),
    PineconeOptions.PINECONE_SOURCE_TAG_CONF -> System.getenv("PINECONE_SOURCE_TAG")
  )

  // Write the stream to Pinecone using the defined options
  val upsert = lines
    .writeStream
    .format("io.pinecone.spark.pinecone.Pinecone")
    .options(pineconeOptions)
    .option("checkpointLocation", "path/to/checkpoint/dir")
    .outputMode("append")
    .start()

  upsert.awaitTermination()
}
```
