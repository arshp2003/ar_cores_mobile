package com.google.ar.core.examples.java.helloar;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

public class ScreenRecordingForegroundService extends android.app.Service {
  private static final String CHANNEL_ID = "screen_recording_foreground_channel";
  private static final int NOTIFICATION_ID = 1001;
  private static final String ACTION_START = "com.google.ar.core.examples.java.helloar.START_FOREGROUND";
  private static final String ACTION_STOP = "com.google.ar.core.examples.java.helloar.STOP_FOREGROUND";

  public static void start(Context context) {
    Intent intent = new Intent(context, ScreenRecordingForegroundService.class);
    intent.setAction(ACTION_START);
    ContextCompat.startForegroundService(context, intent);
  }

  public static void stop(Context context) {
    Intent intent = new Intent(context, ScreenRecordingForegroundService.class);
    intent.setAction(ACTION_STOP);
    ContextCompat.startForegroundService(context, intent);
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    if (intent == null) {
      return START_NOT_STICKY;
    }

    String action = intent.getAction();
    if (ACTION_STOP.equals(action)) {
      stopForeground(true);
      stopSelf();
      return START_NOT_STICKY;
    }

    if (ACTION_START.equals(action)) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        startForeground(NOTIFICATION_ID, buildNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
      } else {
        startForeground(NOTIFICATION_ID, buildNotification());
      }
      return START_STICKY;
    }

    return START_NOT_STICKY;
  }

  private Notification buildNotification() {
    NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    if (manager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      NotificationChannel channel =
          new NotificationChannel(
              CHANNEL_ID,
              getString(R.string.screen_recording_channel_name),
              NotificationManager.IMPORTANCE_LOW);
      channel.setDescription(getString(R.string.screen_recording_channel_description));
      manager.createNotificationChannel(channel);
    }

    Intent activityIntent = new Intent(this, HelloArActivity.class);
    activityIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
    PendingIntent contentIntent =
        PendingIntent.getActivity(
            this,
            0,
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

    return new NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle(getString(R.string.screen_recording_notification_title))
        .setContentText(getString(R.string.screen_recording_notification_message))
        .setSmallIcon(android.R.drawable.presence_video_online)
        .setContentIntent(contentIntent)
        .setOngoing(true)
        .setCategory(NotificationCompat.CATEGORY_SERVICE)
        .build();
  }

  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }
}
