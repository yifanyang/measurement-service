package com.google.firebase.measurement

import com.google.firebase.measurement.collectors.android.AndroidSizeCollector
import com.google.firebase.measurement.collectors.android.Measurement
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class RootController(val collector: AndroidSizeCollector) {

  @GetMapping("/android/size")
  fun get(
    @RequestParam(name = "group_id") groupId: String,
    @RequestParam(name = "artifact_id") artifactId: String,
    @RequestParam(name = "version") version: String,
  ): List<Measurement> = collector.measure(groupId, artifactId, version)
}
