# spark-pinecone

The official [pinecone.io](https://pinecone.io) spark connector.

## Features
- Please note that the connector's write operation is not atomic - some vectors might be written while others aren't if the operation is stopped or if it fails. 
In practice this shouldn't cause a serious issue. Pinecone is an idempotent key-value store. Re-running the job will result in the desired state without a need to clear the index or calculate some delta from the source data.
- The client currently only supports batch writing of data into pinecone from a specific schema (see the example below).
If you need to use the connector with a streaming pipeline, it is recommended to use a function like `foreachBatch`.

## Support
This client currently only supports Spark 3.5.0, Scala 2.12.X or 2.13.X and Java 8+.

### Databricks and friends
Due to various libraries provided by Databricks (and other runtimes), please use the assembly jar from s3 for now.
S3 path for assembly jar: s3://pinecone-jars/spark-pinecone-uberjar.jar

## Usage

add the following snippet to your `built.sbt` file:
```scala
libraryDependencies += "io.pinecone" %% "spark-pinecone" % "0.1.0"
```


## Example

To connect to Pinecone with Spark you'll have to retrieve the following information from [your Pinecone console](https://app.pinecone.io)

1. API Key: navigate to your project and click the "API Keys" button on the sidebar
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
import org.apache.spark.SparkConf
import org.apache.spark.sql.types._
import org.apache.spark.sql.{SaveMode, SparkSession}

object MainApp extends App {
  val conf = new SparkConf()
    .setMaster("local[*]")
  val spark = SparkSession.builder().config(conf).getOrCreate()

  val df = spark.read
    .schema(new StructType()
      .add("id", StringType,false)
      .add("namespace", StringType)
      .add("values", ArrayType(FloatType),false)
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
