package com.google.ar.core.examples.java.helloar

import android.content.Intent
import android.os.Bundle
import com.google.ar.core.examples.java.helloar.HelloArActivity
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

private const val CHANNEL_NAME = "arcore_hello"

class MainActivity : FlutterActivity() {

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL_NAME).setMethodCallHandler { call, result ->
            when (call.method) {
                "launchHelloAr" -> {
                    startActivity(Intent(this, HelloArActivity::class.java))
                    result.success(null)
                }
                else -> result.notImplemented()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }
}
