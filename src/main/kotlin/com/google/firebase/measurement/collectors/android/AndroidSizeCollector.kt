package com.google.firebase.measurement.collectors.android

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Collectors
import kotlin.io.path.createTempDirectory
import kotlin.io.path.fileSize
import kotlin.io.path.name
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

private val logger = LoggerFactory.getLogger(AndroidSizeCollector::class.java)

@Component
class AndroidSizeCollector {
  val src = "test-apps/android"

  fun measure(groupId: String, artifactId: String, version: String): List<Measurement> =
    with(prepareFileSystem()) {
      runGradleBuild(this, groupId, artifactId, version)
      collectApkSize(this, artifactId, version)
    }

  private fun prepareFileSystem(): Path {
    val path = createTempDirectory("android-size-")
    File(src).copyRecursively(path.toFile())
    path.resolve("gradlew").toFile().setExecutable(true)
    return path
  }

  private fun runGradleBuild(cwd: Path, groupId: String, artifactId: String, version: String) {
    logger.info("Running android size measurement at: $cwd")
    val artifact = "$groupId:$artifactId:$version"
    val command = listOf("./gradlew", "assemble", "-Psdks=$artifact")
    val process = ProcessBuilder(command).directory(cwd.toFile()).inheritIO().start()
    process.waitFor().takeIf { it == 0 } ?: throw RuntimeException("Gradle build failed")
  }

  private fun collectApkSize(
    cwd: Path,
    artifactId: String,
    version: String,
  ): List<Measurement> =
    Files.walk(cwd.resolve("app/build/outputs"))
      .filter { it.name.endsWith(".apk") }
      .filter { it.name.startsWith(artifactId) }
      .map {
        val (_, build, abi) = Regex("(.*)::(.*)::(.*).apk").find(it.name)!!.destructured
        val type = if (abi == "universal") "apk ($build)" else "apk ($build / $abi)"
        val size = it.fileSize()
        logger.info("Found apk file \"${it.name}\" of size $size bytes")
        Measurement(artifactId, version, type, size)
      }
      .collect(Collectors.toList())
}

data class Measurement(val artifact: String, val version: String, val type: String, val size: Long)
