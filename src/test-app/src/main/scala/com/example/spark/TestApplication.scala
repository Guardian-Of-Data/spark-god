package com.example.spark

import org.apache.spark.sql.SparkSession

/**
 * Test application to verify the Spark UI Plugin works correctly
 */
object TestApplication {

  def main(args: Array[String]): Unit = {
    // Create SparkSession with plugin enabled
    val spark = SparkSession.builder()
      .appName("Spark God Plugin Test")
      .config("spark.plugins", "org.apache.spark.SparkGodPlugin")
      .config("spark.eventLog.enabled", "true")
      .config("spark.eventLog.dir", "file:///tmp/spark-events")
      .getOrCreate()

    println("Spark UI Plugin Test Application Started")
    println("Check the Spark Web UI at http://localhost:4040")
    println("You should see a new 'Custom Plugin' tab in the navigation")
    
    // Create some sample data and perform operations
    val data = (1 to 100).map(i => (i, s"test_$i", i * 2))
    val df = spark.createDataFrame(data).toDF("id", "name", "value")
    
    println("Sample data created:")
    df.show(5)
    
    // Perform some operations to generate events
    val count = df.count()
    val sum = df.select("value").agg("value" -> "sum").first().getLong(0)
    
    println(s"Total records: $count")
    println(s"Sum of values: $sum")
    
    // Keep the application running for a while to test the UI
    println("Application will run for 30 seconds to allow UI testing...")
    println("Press Ctrl+C to stop the application early")
    
    try {
      Thread.sleep(30000)
    } catch {
      case _: InterruptedException =>
        println("Application interrupted by user")
    }
    
    println("Stopping Spark application...")
    try {
      spark.stop()
      println("Test application completed successfully")
    } catch {
      case e: Exception =>
        println(s"Error stopping Spark application: ${e.getMessage}")
    }
  }
}
