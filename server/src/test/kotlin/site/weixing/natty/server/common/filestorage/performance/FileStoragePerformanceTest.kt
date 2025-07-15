package site.weixing.natty.server.common.filestorage.performance
// 很难测试
//import org.junit.jupiter.api.Test
//import org.springframework.beans.factory.annotation.Autowired
//import org.springframework.boot.test.context.SpringBootTest
//import org.springframework.http.MediaType
//import org.springframework.http.client.MultipartBodyBuilder
//import org.springframework.test.context.ActiveProfiles
//import org.springframework.test.web.reactive.server.WebTestClient
//import org.springframework.web.reactive.function.BodyInserters
//import reactor.core.publisher.Flux
//import reactor.core.publisher.Mono
//import reactor.test.StepVerifier
//import java.nio.charset.StandardCharsets
//import java.time.Duration
//import java.util.concurrent.CountDownLatch
//import java.util.concurrent.Executors
//import java.util.concurrent.TimeUnit
//import java.util.concurrent.atomic.AtomicInteger
//import java.util.concurrent.atomic.AtomicLong
//import kotlin.system.measureTimeMillis
//
///**
// * 文件存储性能测试
// * 验证系统在各种负载下的性能表现
// */
//@ActiveProfiles("test")
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//class FileStoragePerformanceTest(@Autowired val webTestClient: WebTestClient) {
//
//    /**
//     * 测试单个文件上传的基准性能
//     */
//    @Test
//    fun `should upload single file within performance baseline`() {
//        val fileName = "performance-test.txt"
//        val fileContent = "Performance test content for baseline measurement"
//
//        val bodyBuilder = MultipartBodyBuilder()
//        bodyBuilder.part("file", fileContent.toByteArray(StandardCharsets.UTF_8))
//            .filename(fileName)
//            .contentType(MediaType.TEXT_PLAIN)
//        bodyBuilder.part("folderId", "performance")
//        bodyBuilder.part("uploaderId", "perf-user")
//
//        val uploadTime = measureTimeMillis {
//            webTestClient.post()
//                .uri("/files/upload")
//                .contentType(MediaType.MULTIPART_FORM_DATA)
//                .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
//                .exchange()
//                .expectStatus().isOk
//                .expectBody()
//                .jsonPath("$.fileId").isNotEmpty
//        }
//
//        // 验证单个小文件上传在合理时间内完成（< 5秒）
//        assert(uploadTime < 5000) { "单文件上传耗时过长: ${uploadTime}ms" }
//        println("单文件上传基准性能: ${uploadTime}ms")
//    }
//
//    /**
//     * 测试大文件上传性能
//     */
//    @Test
//    fun `should handle large file upload efficiently`() {
//        val fileName = "large-file-test.bin"
//        val largeFileSize = 5 * 1024 * 1024 // 5MB
//        val largeContent = ByteArray(largeFileSize) { (it % 256).toByte() }
//
//        val bodyBuilder = MultipartBodyBuilder()
//        bodyBuilder.part("file", largeContent)
//            .filename(fileName)
//            .contentType(MediaType.APPLICATION_OCTET_STREAM)
//        bodyBuilder.part("folderId", "large-files")
//        bodyBuilder.part("uploaderId", "perf-user")
//
//        val uploadTime = measureTimeMillis {
//            webTestClient.mutate()
//                .responseTimeout(Duration.ofMinutes(2)) // 增加超时时间
//                .build()
//                .post()
//                .uri("/files/upload")
//                .contentType(MediaType.MULTIPART_FORM_DATA)
//                .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
//                .exchange()
//                .expectStatus().isOk
//                .expectBody()
//                .jsonPath("$.fileId").isNotEmpty
//        }
//
//        // 验证大文件上传性能（5MB应该在60秒内完成）
//        assert(uploadTime < 60000) { "大文件上传耗时过长: ${uploadTime}ms" }
//
//        // 计算上传速度
//        val speedMBps = (largeFileSize.toDouble() / (1024 * 1024)) / (uploadTime / 1000.0)
//        println("大文件上传性能: ${uploadTime}ms, 速度: ${"%.2f".format(speedMBps)} MB/s")
//
//        // 验证速度不低于基准值（例如：1 MB/s）
//        assert(speedMBps > 1.0) { "上传速度过慢: ${"%.2f".format(speedMBps)} MB/s" }
//    }
//
//    /**
//     * 测试并发上传性能
//     */
//    @Test
//    fun `should handle concurrent uploads efficiently`() {
//        val concurrentCount = 10
//        val executor = Executors.newFixedThreadPool(concurrentCount)
//        val countDownLatch = CountDownLatch(concurrentCount)
//        val successCount = AtomicInteger(0)
//        val totalTime = AtomicLong(0)
//
//        val overallTime = measureTimeMillis {
//            repeat(concurrentCount) { index ->
//                executor.submit {
//                    try {
//                        val fileName = "concurrent-$index.txt"
//                        val fileContent = "Concurrent upload test content $index"
//
//                        val bodyBuilder = MultipartBodyBuilder()
//                        bodyBuilder.part("file", fileContent.toByteArray())
//                            .filename(fileName)
//                        bodyBuilder.part("folderId", "concurrent")
//                        bodyBuilder.part("uploaderId", "perf-user-$index")
//
//                        val individualTime = measureTimeMillis {
//                            webTestClient.post()
//                                .uri("/files/upload")
//                                .contentType(MediaType.MULTIPART_FORM_DATA)
//                                .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
//                                .exchange()
//                                .expectStatus().isOk
//                                .expectBody()
//                                .jsonPath("$.fileId").isNotEmpty
//                        }
//
//                        totalTime.addAndGet(individualTime)
//                        successCount.incrementAndGet()
//                    } catch (e: Exception) {
//                        println("并发上传失败: $e")
//                    } finally {
//                        countDownLatch.countDown()
//                    }
//                }
//            }
//
//            // 等待所有上传完成（最多60秒）
//            assert(countDownLatch.await(60, TimeUnit.SECONDS)) { "并发上传超时" }
//        }
//
//        executor.shutdown()
//
//        // 验证成功率
//        val successRate = successCount.get().toDouble() / concurrentCount
//        assert(successRate >= 0.9) { "并发上传成功率过低: ${"%.2f".format(successRate * 100)}%" }
//
//        // 验证平均响应时间
//        val averageTime = totalTime.get() / successCount.get()
//        assert(averageTime < 10000) { "并发上传平均响应时间过长: ${averageTime}ms" }
//
//        println("并发上传性能: $concurrentCount 个文件, 总耗时: ${overallTime}ms, 成功率: ${"%.2f".format(successRate * 100)}%, 平均响应时间: ${averageTime}ms")
//    }
//
//    /**
//     * 测试流式处理性能
//     */
//    @Test
//    fun `should process files efficiently with processing options`() {
//        val fileName = "processing-perf-test.txt"
//        val fileContent = "Large content for processing performance test. ".repeat(1000) // ~50KB
//
//        val bodyBuilder = MultipartBodyBuilder()
//        bodyBuilder.part("file", fileContent.toByteArray())
//            .filename(fileName)
//        bodyBuilder.part("folderId", "processing-perf")
//        bodyBuilder.part("uploaderId", "perf-user")
//        bodyBuilder.part("enableCompression", "true")
//        bodyBuilder.part("requireEncryption", "true")
//
//        val uploadTime = measureTimeMillis {
//            webTestClient.post()
//                .uri("/files/upload/enhanced")
//                .contentType(MediaType.MULTIPART_FORM_DATA)
//                .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
//                .exchange()
//                .expectStatus().isOk
//                .expectBody()
//                .jsonPath("$.fileId").isNotEmpty
//                .jsonPath("$.processingRequired").isEqualTo(true)
//        }
//
//        // 验证带处理选项的上传在合理时间内完成（< 15秒，因为包含处理时间）
//        assert(uploadTime < 15000) { "处理上传耗时过长: ${uploadTime}ms" }
//        println("流式处理上传性能: ${uploadTime}ms")
//    }
//
//    /**
//     * 测试内存使用效率
//     */
//    @Test
//    fun `should handle multiple large files without memory issues`() {
//        val fileCount = 3
//        val fileSize = 2 * 1024 * 1024 // 2MB each
//
//        // 获取初始内存使用
//        System.gc()
//        val initialMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
//
//        val uploads = (1..fileCount).map { index ->
//            val fileName = "memory-test-$index.bin"
//            val content = ByteArray(fileSize) { (it % 256).toByte() }
//
//            val bodyBuilder = MultipartBodyBuilder()
//            bodyBuilder.part("file", content)
//                .filename(fileName)
//            bodyBuilder.part("folderId", "memory-test")
//            bodyBuilder.part("uploaderId", "perf-user")
//
//            Mono.fromCallable {
//                webTestClient.mutate()
//                    .responseTimeout(Duration.ofMinutes(1))
//                    .build()
//                    .post()
//                    .uri("/files/upload")
//                    .contentType(MediaType.MULTIPART_FORM_DATA)
//                    .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
//                    .exchange()
//                    .expectStatus().isOk
//                    .expectBody()
//                    .jsonPath("$.fileId").isNotEmpty
//            }
//        }
//
//        val totalTime = measureTimeMillis {
//            // 串行处理以观察内存变化
//            uploads.forEach { upload ->
//                StepVerifier.create(upload)
//                    .expectNextCount(1)
//                    .verifyComplete()
//            }
//        }
//
//        // 强制垃圾回收
//        System.gc()
//        Thread.sleep(1000) // 等待GC完成
//
//        val finalMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
//        val memoryIncrease = finalMemory - initialMemory
//        val memoryIncreasePerFile = memoryIncrease / fileCount
//
//        println("内存使用测试: ${fileCount}个文件 (${fileSize / 1024 / 1024}MB each), 总耗时: ${totalTime}ms")
//        println("内存增长: ${memoryIncrease / 1024 / 1024}MB, 平均每文件: ${memoryIncreasePerFile / 1024 / 1024}MB")
//
//        // 验证内存增长在合理范围内（不应该超过文件总大小的2倍）
//        val totalFileSize = fileCount * fileSize
//        assert(memoryIncrease < totalFileSize * 2) {
//            "内存使用过多: ${memoryIncrease / 1024 / 1024}MB, 文件总大小: ${totalFileSize / 1024 / 1024}MB"
//        }
//    }
//
//    /**
//     * 测试系统响应时间分布
//     */
//    @Test
//    fun `should maintain consistent response times under load`() {
//        val requestCount = 20
//        val responseTimes = mutableListOf<Long>()
//
//        repeat(requestCount) { index ->
//            val fileName = "response-time-test-$index.txt"
//            val fileContent = "Response time test content $index"
//
//            val bodyBuilder = MultipartBodyBuilder()
//            bodyBuilder.part("file", fileContent.toByteArray())
//                .filename(fileName)
//            bodyBuilder.part("folderId", "response-time")
//            bodyBuilder.part("uploaderId", "perf-user")
//
//            val responseTime = measureTimeMillis {
//                webTestClient.post()
//                    .uri("/files/upload")
//                    .contentType(MediaType.MULTIPART_FORM_DATA)
//                    .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
//                    .exchange()
//                    .expectStatus().isOk
//                    .expectBody()
//                    .jsonPath("$.fileId").isNotEmpty
//            }
//
//            responseTimes.add(responseTime)
//        }
//
//        // 计算统计信息
//        val averageTime = responseTimes.average()
//        val maxTime = responseTimes.maxOrNull() ?: 0L
//        val minTime = responseTimes.minOrNull() ?: 0L
//        val sortedTimes = responseTimes.sorted()
//        val percentile95 = sortedTimes[(sortedTimes.size * 0.95).toInt()]
//        val percentile99 = sortedTimes[(sortedTimes.size * 0.99).toInt()]
//
//        println("响应时间分析 ($requestCount 个请求):")
//        println("  平均: ${"%.2f".format(averageTime)}ms")
//        println("  最小: ${minTime}ms")
//        println("  最大: ${maxTime}ms")
//        println("  95%ile: ${percentile95}ms")
//        println("  99%ile: ${percentile99}ms")
//
//        // 性能验证
//        assert(averageTime < 5000) { "平均响应时间过长: ${"%.2f".format(averageTime)}ms" }
//        assert(percentile95 < 10000) { "95%ile响应时间过长: ${percentile95}ms" }
//        assert(maxTime < 15000) { "最大响应时间过长: ${maxTime}ms" }
//
//        // 验证响应时间一致性（最大时间不应该超过平均时间的5倍）
//        assert(maxTime < averageTime * 5) {
//            "响应时间变化过大: 最大${maxTime}ms vs 平均${"%.2f".format(averageTime)}ms"
//        }
//    }
//
//    /**
//     * 测试处理状态查询性能
//     */
//    @Test
//    fun `should query processing status efficiently`() {
//        // 先上传一些需要处理的文件
//        repeat(5) { index ->
//            val fileName = "status-query-test-$index.txt"
//            val fileContent = "Status query test content $index"
//
//            val bodyBuilder = MultipartBodyBuilder()
//            bodyBuilder.part("file", fileContent.toByteArray())
//                .filename(fileName)
//            bodyBuilder.part("folderId", "status-query")
//            bodyBuilder.part("uploaderId", "perf-user")
//            bodyBuilder.part("enableCompression", "true")
//
//            webTestClient.post()
//                .uri("/files/upload/enhanced")
//                .contentType(MediaType.MULTIPART_FORM_DATA)
//                .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
//                .exchange()
//                .expectStatus().isOk
//        }
//
//        // 测试状态查询性能
//        val queryTime = measureTimeMillis {
//            webTestClient.get()
//                .uri("/files/processing/status")
//                .exchange()
//                .expectStatus().isOk
//                .expectBody()
//                .jsonPath("$").isArray
//        }
//
//        // 验证状态查询响应时间
//        assert(queryTime < 3000) { "状态查询响应时间过长: ${queryTime}ms" }
//        println("处理状态查询性能: ${queryTime}ms")
//
//        // 测试统计查询性能
//        val statsQueryTime = measureTimeMillis {
//            webTestClient.get()
//                .uri("/files/processing/stats")
//                .exchange()
//                .expectStatus().isOk
//                .expectBody()
//                .jsonPath("$.totalCount").isNumber
//        }
//
//        assert(statsQueryTime < 2000) { "统计查询响应时间过长: ${statsQueryTime}ms" }
//        println("处理统计查询性能: ${statsQueryTime}ms")
//    }
//
//    /**
//     * 测试系统在压力下的稳定性
//     */
//    @Test
//    fun `should remain stable under sustained load`() {
//        val duration = 30000L // 30秒压力测试
//        val startTime = System.currentTimeMillis()
//        val requestCount = AtomicInteger(0)
//        val errorCount = AtomicInteger(0)
//        val executor = Executors.newFixedThreadPool(5)
//
//        // 启动持续负载
//        repeat(5) { threadIndex ->
//            executor.submit {
//                while (System.currentTimeMillis() - startTime < duration) {
//                    try {
//                        val requestId = requestCount.incrementAndGet()
//                        val fileName = "stability-test-$threadIndex-$requestId.txt"
//                        val fileContent = "Stability test content"
//
//                        val bodyBuilder = MultipartBodyBuilder()
//                        bodyBuilder.part("file", fileContent.toByteArray())
//                            .filename(fileName)
//                        bodyBuilder.part("folderId", "stability")
//                        bodyBuilder.part("uploaderId", "perf-user-$threadIndex")
//
//                        webTestClient.post()
//                            .uri("/files/upload")
//                            .contentType(MediaType.MULTIPART_FORM_DATA)
//                            .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
//                            .exchange()
//                            .expectStatus().isOk
//
//                        Thread.sleep(100) // 控制请求频率
//                    } catch (e: Exception) {
//                        errorCount.incrementAndGet()
//                    }
//                }
//            }
//        }
//
//        executor.shutdown()
//        assert(executor.awaitTermination(45, TimeUnit.SECONDS)) { "压力测试超时" }
//
//        val totalRequests = requestCount.get()
//        val totalErrors = errorCount.get()
//        val errorRate = totalErrors.toDouble() / totalRequests
//        val requestsPerSecond = totalRequests.toDouble() / (duration / 1000.0)
//
//        println("稳定性测试结果:")
//        println("  持续时间: ${duration / 1000}s")
//        println("  总请求数: $totalRequests")
//        println("  错误数: $totalErrors")
//        println("  错误率: ${"%.2f".format(errorRate * 100)}%")
//        println("  平均RPS: ${"%.2f".format(requestsPerSecond)}")
//
//        // 验证系统稳定性
//        assert(errorRate < 0.05) { "错误率过高: ${"%.2f".format(errorRate * 100)}%" }
//        assert(requestsPerSecond > 1.0) { "系统吞吐量过低: ${"%.2f".format(requestsPerSecond)} RPS" }
//    }
//}