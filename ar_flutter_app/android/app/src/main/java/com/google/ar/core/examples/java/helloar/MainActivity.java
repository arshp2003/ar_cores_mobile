package com.google.ar.core.examples.java.helloar;

import android.content.Intent;
import android.os.Bundle;
import io.flutter.embedding.android.FlutterActivity;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.plugin.common.MethodChannel;

public class MainActivity extends FlutterActivity {
    private static final String CHANNEL = "arcore_hello";

    @Override
    public void configureFlutterEngine(FlutterEngine flutterEngine) {
        super.configureFlutterEngine(flutterEngine);
        new MethodChannel(flutterEngine.getDartExecutor().getBinaryMessenger(), CHANNEL)
                .setMethodCallHandler((call, result) -> {
                    if (call.method.equals("launchHelloAr")) {
                        try {
                            Intent intent = new Intent(this, HelloArActivity.class);
                            startActivity(intent);
                            result.success(null);
                        } catch (Exception e) {
                            result.error("AR_ERROR", "Failed to launch AR activity", e.getMessage());
                        }
                    } else {
                        result.notImplemented();
                    }
                });
    }
}
