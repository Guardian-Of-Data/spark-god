package com.example.spark

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._
import scala.util.Random

/**
 * Test application with various load scenarios and intentional failures
 * for testing SparkGod AI's log analysis capabilities
 */
object TestApplication {

  def main(args: Array[String]): Unit = {
    val testMode = if (args.length > 0) args(0) else "normal"
    
    println(s"🚀 SparkGod Test Application Starting - Mode: $testMode")
    println("📊 Attempting to start Spark UI: http://localhost:4040")
    println("🔧 SparkGod Plugin UI will be available after Spark starts")
    println("🤖 SparkGod AI Chat: http://localhost:3000 (React UI)")
    println("=" * 60)
    
    // SparkSession 생성을 try-catch로 감싸서 실패해도 계속 실행
    val sparkOpt: Option[SparkSession] = try {
      val spark = SparkSession.builder()
        .appName(s"SparkGod Test - $testMode")
        .config("spark.plugins", "org.apache.spark.SparkGodPlugin")
        .config("spark.eventLog.enabled", "true")
        .config("spark.eventLog.dir", "file:///tmp/spark-events")
        .config("spark.history.fs.logDirectory", "file:///tmp/spark-events")
        .config("spark.sql.adaptive.enabled", "true")
        .config("spark.sql.adaptive.coalescePartitions.enabled", "true")
        .getOrCreate()
      
      println("✅ SparkSession created successfully!")
      println("📊 Spark Web UI: http://localhost:4040")
      println("🔧 SparkGod Plugin UI: http://localhost:4040 (Custom Plugin tab)")
      Some(spark)
      
    } catch {
      case e: Exception =>
        println(s"❌ Failed to create SparkSession: ${e.getMessage}")
        e.printStackTrace()
        println("⚠️ Continuing without Spark for demonstration purposes...")
        None
    }
    
    // SparkSession이 있으면 테스트 실행, 없으면 데모 모드
    sparkOpt match {
      case Some(spark) =>
        try {
          testMode.toLowerCase match {
            case "normal" => runNormalTest(spark)
            case "memory" => runMemoryStressTest(spark)
            case "shuffle" => runShuffleHeavyTest(spark)
            case "error" => runErrorTest(spark)
            case "timeout" => runTimeoutTest(spark)
            case "gc" => runGCStressTest(spark)
            case "safe" => runSafeTest(spark)
            case _ => 
              println("Available test modes: normal, memory, shuffle, error, timeout, gc, safe")
              runSafeTest(spark)
          }
          
          println("🎯 Test completed! Keeping application alive for SparkGod analysis...")
          
        } catch {
          case e: Exception =>
            println(s"❌ Test execution failed: ${e.getMessage}")
            e.printStackTrace()
            println("🔍 Test failed but keeping Spark UI alive for analysis...")
        } finally {
          // Spark UI를 유지하기 위해 SparkSession을 바로 종료하지 않음
          println("📊 SparkGod UI: http://localhost:4040")
          println("🤖 SparkGod AI Chat: http://localhost:3000")
          println("� Prelss Ctrl+C to stop the application")
          
          try {
            while (true) {
              Thread.sleep(30000) // 30초마다 체크
              println(s"⏰ Spark application running... ${new java.util.Date()}")
            }
          } catch {
            case _: InterruptedException =>
              println("🛑 Application interrupted by user")
          } finally {
            println("🛑 Stopping Spark application...")
            spark.stop()
          }
        }
        
      case None =>
        // SparkSession 생성 실패 시 데모 모드
        println("🎭 Running in demo mode (without Spark)...")
        runDemoMode(testMode)
    }
  }

  def runNormalTest(spark: SparkSession): Unit = {
    println("✅ Running normal test scenario...")
    
    // Create sample data
    val data = (1 to 1000).map(i => (i, s"user_$i", Random.nextDouble() * 100, Random.nextInt(10)))
    val df = spark.createDataFrame(data).toDF("id", "name", "score", "category")
    
    println("📊 Sample data created:")
    df.show(10)
    
    // Basic operations
    val count = df.count()
    val avgScore = df.agg(org.apache.spark.sql.functions.avg("score")).first().getDouble(0)
    val maxScore = df.agg(org.apache.spark.sql.functions.max("score")).first().getDouble(0)
    
    println(s"📈 Statistics: Count=$count, Avg Score=${avgScore.formatted("%.2f")}, Max Score=${maxScore.formatted("%.2f")}")
    
    // Group by operations
    val categoryStats = df.groupBy("category")
      .agg(
        org.apache.spark.sql.functions.count("id").as("count"), 
        org.apache.spark.sql.functions.avg("score").as("avg_score")
      )
      .orderBy("category")
    
    println("📊 Category statistics:")
    categoryStats.show()
    
    println("✅ Normal test completed successfully!")
    println("📊 SparkGod UI available at http://localhost:4040")
    println("🤖 Ask SparkGod AI about the current configuration!")
  }

  def runMemoryStressTest(spark: SparkSession): Unit = {
    println("💾 Running memory stress test scenario...")
    
    try {
      // Create large dataset to stress memory
      val largeData = (1 to 1000000).map { i =>
        (i, s"large_data_$i", Random.nextDouble() * 1000, 
         s"category_${Random.nextInt(100)}", 
         Random.alphanumeric.take(100).mkString) // 큰 문자열
      }
      
      val df = spark.createDataFrame(largeData)
        .toDF("id", "name", "value", "category", "description")
      
      println("💾 Large dataset created, performing memory-intensive operations...")
      
      // Memory intensive operations
      df.cache() // 메모리에 캐시
      
      val result1 = df.groupBy("category")
        .agg(
          org.apache.spark.sql.functions.count("id").as("count"),
          org.apache.spark.sql.functions.avg("value").as("avg_value"),
          org.apache.spark.sql.functions.stddev("value").as("stddev_value"),
          org.apache.spark.sql.functions.collect_list("name").as("names") // 메모리 사용량 증가
        )
      
      println(s"📊 Cached data count: ${df.count()}")
      println(s"📊 Category groups: ${result1.count()}")
      
      // 추가 메모리 압박
      val result2 = df.crossJoin(df.limit(100)) // 크로스 조인으로 메모리 압박
      println(s"💥 Cross join result count: ${result2.count()}")
      
      println("✅ Memory stress test completed!")
      
    } catch {
      case e: Exception =>
        println(s"💥 Memory stress test failed: ${e.getMessage}")
        println("🔍 This is expected behavior for stress testing!")
        println("📊 Spark UI remains available for analysis!")
    }
    
    println("📊 Check SparkGod UI for memory usage analysis!")
    println("🤖 Ask SparkGod AI about memory optimization!")
  }

  def runShuffleHeavyTest(spark: SparkSession): Unit = {
    println("🔄 Running shuffle-heavy test scenario...")
    
    try {
      // Create data that will cause heavy shuffling
      val data1 = (1 to 100000).map(i => (Random.nextInt(1000), s"table1_$i", Random.nextDouble()))
      val data2 = (1 to 100000).map(i => (Random.nextInt(1000), s"table2_$i", Random.nextDouble()))
      
      val df1 = spark.createDataFrame(data1).toDF("key", "name1", "value1")
      val df2 = spark.createDataFrame(data2).toDF("key", "name2", "value2")
      
      println("🔄 Created datasets for shuffle operations...")
      
      // Heavy shuffle operations
      val joined = df1.join(df2, "key") // 조인으로 셔플 발생
      val grouped = joined.groupBy("key")
        .agg(
          org.apache.spark.sql.functions.count("key").as("count"),
          org.apache.spark.sql.functions.avg("value1").as("avg_value1"),
          org.apache.spark.sql.functions.avg("value2").as("avg_value2")
        )
      
      println(s"🔄 Join result count: ${joined.count()}")
      println(s"🔄 Grouped result count: ${grouped.count()}")
      
      // 추가 셔플 작업
      val repartitioned = grouped.repartition(200, org.apache.spark.sql.functions.col("key"))
      val sorted = repartitioned.orderBy(org.apache.spark.sql.functions.desc("count"))
      
      println("🔄 Performing additional shuffle operations...")
      sorted.show(20)
      
      println("✅ Shuffle-heavy test completed!")
      
    } catch {
      case e: Exception =>
        println(s"💥 Shuffle test failed: ${e.getMessage}")
        println("🔍 This is expected behavior for stress testing!")
        println("📊 Spark UI remains available for analysis!")
    }
    
    println("📊 Check SparkGod UI for shuffle performance metrics!")
    println("🤖 Ask SparkGod AI about shuffle optimization!")
  }

  def runErrorTest(spark: SparkSession): Unit = {
    println("❌ Running intentional error test scenario...")
    
    // Create normal data first
    val data = (1 to 1000).map(i => (i, s"test_$i", if (i % 100 == 0) null else Random.nextDouble()))
    val df = spark.createDataFrame(data).toDF("id", "name", "value")
    
    println("⚠️ Created data with null values...")
    
    try {
      // Operation that will cause errors
      val result = df.select(
        org.apache.spark.sql.functions.col("id"),
        org.apache.spark.sql.functions.col("name"),
        (org.apache.spark.sql.functions.col("value") / org.apache.spark.sql.functions.lit(0)).as("division_by_zero"), // 의도적 오류
        org.apache.spark.sql.functions.col("value").cast("int").as("int_value")
      )
      
      println("💥 Attempting division by zero...")
      result.show(10)
      
    } catch {
      case e: Exception =>
        println(s"💥 Expected error occurred: ${e.getMessage}")
    }
    
    try {
      // Another error scenario - accessing non-existent column
      val badResult = df.select(org.apache.spark.sql.functions.col("non_existent_column"))
      badResult.show()
      
    } catch {
      case e: Exception =>
        println(s"💥 Column access error: ${e.getMessage}")
    }
    
    try {
      // Memory allocation error - 더 안전한 크기로 조정
      val hugeArray = Array.fill(10000000)(Random.nextDouble()) // 10M elements
      println(s"🔥 Created large array: ${hugeArray.length}")
      
    } catch {
      case e: OutOfMemoryError =>
        println(s"💥 Out of memory error: ${e.getMessage}")
      case e: Exception =>
        println(s"💥 Memory allocation error: ${e.getMessage}")
    }
    
    println("⚠️ Error test completed with intentional failures!")
    println("📊 SparkGod UI shows error details - perfect for analysis!")
    println("🤖 Ask SparkGod AI to analyze the errors and suggest solutions!")
  }

  def runTimeoutTest(spark: SparkSession): Unit = {
    println("⏰ Running timeout test scenario...")
    
    try {
      // Set short timeouts to trigger timeout errors
      spark.conf.set("spark.network.timeout", "10s")
      spark.conf.set("spark.sql.broadcastTimeout", "5s")
      
      val data = (1 to 500000).map(i => (i, s"timeout_test_$i", Random.nextDouble()))
      val df = spark.createDataFrame(data).toDF("id", "name", "value")
      
      println("⏰ Created large dataset with short timeout settings...")
      
      try {
        // Operations that might timeout
        val result = df.repartition(1000) // 많은 파티션으로 리파티션
          .groupBy("name")
          .agg(org.apache.spark.sql.functions.sum("value"))
          .collect() // collect으로 모든 데이터를 드라이버로
        
        println(s"⏰ Collected ${result.length} results")
        
      } catch {
        case e: Exception =>
          println(s"⏰ Timeout or network error: ${e.getMessage}")
          println("🔍 This is expected behavior for timeout testing!")
      }
      
      println("⏰ Timeout test completed!")
      
    } catch {
      case e: Exception =>
        println(s"💥 Timeout test setup failed: ${e.getMessage}")
        println("🔍 This is expected behavior for stress testing!")
        println("📊 Spark UI remains available for analysis!")
    }
    
    println("📊 Check SparkGod UI for timeout-related issues!")
    println("🤖 Ask SparkGod AI about network and timeout optimization!")
  }

  def runGCStressTest(spark: SparkSession): Unit = {
    println("🗑️ Running GC stress test scenario...")
    
    try {
      // Create many short-lived objects to stress GC
      for (iteration <- 1 to 10) {
        println(s"🗑️ GC Stress iteration $iteration/10")
        
        try {
          val tempData = (1 to 100000).map { i =>
            // Create objects that will be quickly garbage collected
            val largeString = Random.alphanumeric.take(1000).mkString
            val tempMap = Map(
              "id" -> i,
              "data" -> largeString,
              "timestamp" -> System.currentTimeMillis(),
              "random" -> Random.nextDouble()
            )
            (i, largeString, tempMap.toString)
          }
          
          val df = spark.createDataFrame(tempData).toDF("id", "large_data", "metadata")
          
          // Quick operations that create temporary objects
          val count = df.count()
          val sample = df.sample(0.1).collect()
          
          println(s"🗑️ Iteration $iteration: processed $count records, sampled ${sample.length}")
          
        } catch {
          case e: Exception =>
            println(s"💥 GC iteration $iteration failed: ${e.getMessage}")
            println("🔍 Continuing with next iteration...")
        }
        
        // Force some GC pressure
        System.gc()
        Thread.sleep(5000) // 5초 대기
      }
      
      println("🗑️ GC stress test completed!")
      
    } catch {
      case e: Exception =>
        println(s"💥 GC stress test failed: ${e.getMessage}")
        println("🔍 This is expected behavior for stress testing!")
        println("📊 Spark UI remains available for analysis!")
    }
    
    println("📊 SparkGod UI shows GC metrics and memory patterns!")
    println("🤖 Ask SparkGod AI about GC optimization and memory tuning!")
  }

  def runSafeTest(spark: SparkSession): Unit = {
    println("🛡️ Running safe test scenario (guaranteed not to crash)...")
    
    try {
      // 매우 작은 데이터셋으로 안전한 테스트
      val data = (1 to 100).map(i => (i, s"safe_$i", i * 1.5))
      val df = spark.createDataFrame(data).toDF("id", "name", "value")
      
      println("📊 Safe sample data created:")
      df.show(10)
      
      // 안전한 기본 연산들
      val count = df.count()
      println(s"📈 Total records: $count")
      
      // 간단한 필터링
      val filtered = df.filter(org.apache.spark.sql.functions.col("id") > 50)
      val filteredCount = filtered.count()
      println(s"📊 Filtered records (id > 50): $filteredCount")
      
      // 안전한 집계
      val avgValue = df.agg(org.apache.spark.sql.functions.avg("value")).first().getDouble(0)
      println(s"📈 Average value: ${avgValue.formatted("%.2f")}")
      
      // 간단한 정렬
      val sorted = df.orderBy(org.apache.spark.sql.functions.desc("value"))
      println("📊 Top 5 records by value:")
      sorted.show(5)
      
      println("✅ Safe test completed successfully!")
      println("📊 This test is designed to never crash - perfect for SparkGod UI testing!")
      
    } catch {
      case e: Exception =>
        println(s"⚠️ Even safe test had an issue: ${e.getMessage}")
        println("🛡️ But continuing anyway to keep Spark UI alive...")
    }
  }

  def runDemoMode(testMode: String): Unit = {
    println(s"🎭 Demo Mode: Simulating $testMode test scenario...")
    println("📝 This demonstrates what would happen if Spark was running:")
    
    testMode.toLowerCase match {
      case "normal" =>
        println("✅ Normal test would create sample data and perform basic operations")
        println("📊 Statistics would be calculated (count, average, max)")
        println("📈 Category grouping and aggregations would be performed")
        
      case "memory" =>
        println("💾 Memory stress test would create large datasets (1M records)")
        println("🔄 Memory-intensive operations like caching and cross joins")
        println("⚠️ This might cause OutOfMemoryError - perfect for SparkGod analysis!")
        
      case "shuffle" =>
        println("🔄 Shuffle-heavy test would perform large joins and repartitioning")
        println("📊 Multiple datasets would be joined causing shuffle operations")
        println("🎯 Performance bottlenecks would be visible in Spark UI")
        
      case "error" =>
        println("❌ Error test would intentionally cause various failures:")
        println("  💥 Division by zero errors")
        println("  🔍 Non-existent column access")
        println("  💾 Memory allocation errors")
        
      case "timeout" =>
        println("⏰ Timeout test would set short timeouts and perform long operations")
        println("🌐 Network timeout errors would be triggered")
        println("📡 Broadcast timeout issues would occur")
        
      case "gc" =>
        println("🗑️ GC stress test would create many short-lived objects")
        println("♻️ Garbage collection pressure would be applied")
        println("📈 Memory usage patterns would stress the JVM")
        
      case _ =>
        println("❓ Unknown test mode - available: normal, memory, shuffle, error, timeout, gc")
    }
    
    println("\n🎯 Demo completed! In real mode, you would:")
    println("📊 Check Spark UI at http://localhost:4040")
    println("🤖 Ask SparkGod AI to analyze the results")
    println("🔧 Use SparkGod Plugin for detailed configuration analysis")
    
    // 데모 모드에서도 계속 실행
    println("\n💡 Keeping demo application alive...")
    println("🤖 You can still test SparkGod AI Chat at http://localhost:3000")
    println("💡 Press Ctrl+C to stop the application")
    
    try {
      while (true) {
        Thread.sleep(60000) // 1분마다 체크
        println(s"⏰ Demo mode running... ${new java.util.Date()}")
      }
    } catch {
      case _: InterruptedException =>
        println("🛑 Demo interrupted by user")
    }
  }
}
