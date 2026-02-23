/*
 * Copyright 2017 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.ar.core.examples.java.helloar;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.PointF;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.JsonWriter;
import android.util.JsonReader;
import android.util.JsonToken;
import android.view.Display;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.documentfile.provider.DocumentFile;
import android.text.TextUtils;
import com.google.ar.core.Anchor;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.ArCoreApk.Availability;
import com.google.ar.core.Camera;
import com.google.ar.core.Config;
import com.google.ar.core.Config.InstantPlacementMode;
import com.google.ar.core.DepthPoint;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.InstantPlacementPoint;
import com.google.ar.core.LightEstimate;
import com.google.ar.core.Plane;
import com.google.ar.core.Point;
import com.google.ar.core.Point.OrientationMode;
import com.google.ar.core.PointCloud;
import com.google.ar.core.Session;
import com.google.ar.core.Trackable;
import com.google.ar.core.TrackingFailureReason;
import com.google.ar.core.TrackingState;
import com.google.ar.core.Pose;
import com.google.ar.core.examples.java.common.helpers.CameraPermissionHelper;
import com.google.ar.core.examples.java.common.helpers.DepthSettings;
import com.google.ar.core.examples.java.common.helpers.DisplayRotationHelper;
import com.google.ar.core.examples.java.common.helpers.FullScreenHelper;
import com.google.ar.core.examples.java.common.helpers.InstantPlacementSettings;
import com.google.ar.core.examples.java.common.helpers.SnackbarHelper;
import com.google.ar.core.examples.java.common.helpers.TapHelper;
import com.google.ar.core.examples.java.common.helpers.TrackingStateHelper;
import com.google.ar.core.examples.java.common.samplerender.Framebuffer;
import com.google.ar.core.examples.java.common.samplerender.GLError;
import com.google.ar.core.examples.java.common.samplerender.Mesh;
import com.google.ar.core.examples.java.common.samplerender.SampleRender;
import com.google.ar.core.examples.java.common.samplerender.Shader;
import com.google.ar.core.examples.java.common.samplerender.Texture;
import com.google.ar.core.examples.java.common.samplerender.VertexBuffer;
import com.google.ar.core.examples.java.common.samplerender.arcore.BackgroundRenderer;
import com.google.ar.core.examples.java.common.samplerender.arcore.PlaneRenderer;
import com.google.ar.core.examples.java.common.samplerender.arcore.SpecularCubemapFilter;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.NotYetAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.BufferedOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * This is a simple example that shows how to create an augmented reality (AR) application using the
 * ARCore API. The application will display any detected planes and will allow the user to tap on a
 * plane to place a 3D model.
 */
public class HelloArActivity extends AppCompatActivity implements SampleRender.Renderer {

  private static final String TAG = HelloArActivity.class.getSimpleName();

  private static final String SEARCHING_PLANE_MESSAGE = "Searching for surfaces...";
  private static final String WAITING_FOR_TAP_MESSAGE = "Tap on a surface to place an object.";

  private static final String PREFS_NAME = "hello_ar_prefs";
  private static final String PREF_RECORDING_DIR = "recording_output_dir";
  private static final String PREF_RECORDING_TREE_URI = "recording_output_tree_uri";
  private static final String PREF_RECORDING_AUDIO_ENABLED = "recording_audio_enabled";
  private static final String RECORDING_TIMESTAMP_PATTERN = "yyyyMMdd_HHmmss";
  private static final String RECORDING_FILE_EXTENSION = ".mp4";
  private static final String DEFAULT_RECORDING_SUBDIR = "ar_recordings";
  private static final String RECORDING_MIME_TYPE = "video/mp4";
  private static final long RECORDING_TIMER_UPDATE_INTERVAL_MS = 1000L;

  // See the definition of updateSphericalHarmonicsCoefficients for an explanation of these
  // constants.
  private static final float[] sphericalHarmonicFactors = {
    0.282095f,
    -0.325735f,
    0.325735f,
    -0.325735f,
    0.273137f,
    -0.273137f,
    0.078848f,
    -0.273137f,
    0.136569f,
  };

  private static final float Z_NEAR = 0.1f;
  private static final float Z_FAR = 100f;

  private static final int CUBEMAP_RESOLUTION = 16;
  private static final int CUBEMAP_NUMBER_OF_IMPORTANCE_SAMPLES = 32;

  // Rendering. The Renderers are created here, and initialized when the GL surface is created.
  private GLSurfaceView surfaceView;

  private boolean installRequested;

  private Session session;
  private static final String SCREEN_CAPTURE_DISPLAY_NAME = "HelloArScreenCapture";

  private final SnackbarHelper messageSnackbarHelper = new SnackbarHelper();
  private DisplayRotationHelper displayRotationHelper;
  private final TrackingStateHelper trackingStateHelper = new TrackingStateHelper(this);
  private TapHelper tapHelper;
  private SampleRender render;

  private SharedPreferences sharedPreferences;
  private ImageButton recordButton;
  private View recordingStatusContainer;
  private View recordingStatusIndicator;
  private TextView recordingTimerText;
  private Handler recordingTimerHandler;
  private long recordingStartElapsedRealtime = 0L;
  private boolean startRecordingRequested = false;
  private final Runnable recordingTimerRunnable =
      new Runnable() {
        @Override
        public void run() {
          if (!isRecording) {
            return;
          }
          updateRecordingTimerUi();
          if (recordingTimerHandler != null) {
            recordingTimerHandler.postDelayed(this, RECORDING_TIMER_UPDATE_INTERVAL_MS);
          }
        }
      };

  private ActivityResultLauncher<Uri> directoryPickerLauncher;
  private boolean isRecording = false;
  private String currentRecordingPath = null;
  private Uri currentRecordingContentUri = null;
  private String currentRecordingDisplayLabel = null;
  private boolean recordingDotVisible = true;
  private String currentRecordingJsonPath = null;
  private Uri currentRecordingJsonUri = null;
  private RecordingDestination currentRecordingDestination = null;
  private RecordingDataWriter recordingDataWriter = null;
  private long recordingFrameIndex = 0L;
  private MediaProjectionManager mediaProjectionManager;
  private MediaProjection mediaProjection;
  private MediaProjection.Callback mediaProjectionCallback;
  private MediaRecorder mediaRecorder;
  private VirtualDisplay virtualDisplay;
  private ActivityResultLauncher<Intent> screenCaptureLauncher;
  private RecordingDestination pendingRecordingDestination = null;
  private final Object planeLock = new Object();
  private final Map<Plane, Integer> planeIds = new IdentityHashMap<>();
  private int nextPlaneId = 1;
  private ParcelFileDescriptor recordingParcelFileDescriptor = null;
  private boolean pendingStartAfterPermission = false;
  private boolean shouldRecordAudio = false;

  private MeasurementOverlayView measurementOverlayView;

  private final List<MeasurementRecord> measurementRecords = new ArrayList<>();
  private int nextAnchorId = 1;
  private int nextMeasurementId = 1;
  private final Object anchorLock = new Object();

  private PlaneRenderer planeRenderer;
  private BackgroundRenderer backgroundRenderer;
  private Framebuffer virtualSceneFramebuffer;
  private boolean hasSetTextureNames = false;

  private final DepthSettings depthSettings = new DepthSettings();
  private boolean[] depthSettingsMenuDialogCheckboxes = new boolean[2];

  private final InstantPlacementSettings instantPlacementSettings = new InstantPlacementSettings();
  private boolean[] instantPlacementSettingsMenuDialogCheckboxes = new boolean[1];
  // Assumed distance from the device camera to the surface on which user will try to place objects.
  // This value affects the apparent scale of objects while the tracking method of the
  // Instant Placement point is SCREENSPACE_WITH_APPROXIMATE_DISTANCE.
  // Values in the [0.2, 2.0] meter range are a good choice for most AR experiences. Use lower
  // values for AR experiences where users are expected to place objects on surfaces close to the
  // camera. Use larger values for experiences where the user will likely be standing and trying to
  // place an object on the ground or floor in front of them.
  private static final float APPROXIMATE_DISTANCE_METERS = 2.0f;

  // Point Cloud
  private VertexBuffer pointCloudVertexBuffer;
  private Mesh pointCloudMesh;
  private Shader pointCloudShader;
  private VertexBuffer anchorPointVertexBuffer;
  private Mesh anchorPointMesh;
  private Shader anchorPointShader;
  private FloatBuffer anchorPointFloatBuffer;
  private final float[] anchorPointTmpTranslation = new float[3];
  // Keep track of the last point cloud rendered to avoid updating the VBO if point cloud
  // was not changed.  Do this using the timestamp since we can't compare PointCloud objects.
  private long lastPointCloudTimestamp = 0;

  // Virtual object (ARCore pawn)
  private Mesh virtualObjectMesh;
  private Shader virtualObjectShader;
  private Texture virtualObjectAlbedoTexture;
  private Texture virtualObjectAlbedoInstantPlacementTexture;
  private Texture virtualObjectPbrTexture;

  private final List<WrappedAnchor> wrappedAnchors = new ArrayList<>();

  // Environmental HDR
  private Texture dfgTexture;
  private SpecularCubemapFilter cubemapFilter;

  // Temporary matrix allocated here to reduce number of allocations for each frame.
  private final float[] modelMatrix = new float[16];
  private final float[] viewMatrix = new float[16];
  private final float[] projectionMatrix = new float[16];
  private final float[] modelViewMatrix = new float[16]; // view x model
  private final float[] modelViewProjectionMatrix = new float[16]; // projection x view x model
  private final float[] sphericalHarmonicsCoefficients = new float[9 * 3];
  private final float[] viewInverseMatrix = new float[16];
  private final float[] worldLightDirection = {0.0f, 0.0f, 0.0f, 0.0f};
  private final float[] viewLightDirection = new float[4]; // view x world light direction

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    surfaceView = findViewById(R.id.surfaceview);
    measurementOverlayView = findViewById(R.id.measurement_overlay_view);

    sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
    shouldRecordAudio = sharedPreferences.getBoolean(PREF_RECORDING_AUDIO_ENABLED, false);
    mediaProjectionManager =
        (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);

    // Start the foreground service early to satisfy MediaProjection requirements
    ScreenRecordingForegroundService.start(this);
    screenCaptureLauncher =
        registerForActivityResult(
            new StartActivityForResult(),
            result -> {
              if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                startScreenRecordingWithPermissionResult(result);
              } else {
                Log.w(TAG, "Screen capture permission denied");
                if (pendingRecordingDestination != null) {
                  pendingRecordingDestination.deleteArtifacts(this);
                }
                pendingRecordingDestination = null;
                pendingStartAfterPermission = false;
                resetRecordingState();
                updateRecordingUiState();
                Toast.makeText(this, R.string.recording_prepare_failed, Toast.LENGTH_SHORT).show();
              }
            });

    recordButton = findViewById(R.id.record_button);
    recordingStatusContainer = findViewById(R.id.recording_status_container);
    recordingStatusIndicator = findViewById(R.id.recording_status_indicator);
    recordingTimerText = findViewById(R.id.recording_timer_text);
    recordingTimerHandler = new Handler(Looper.getMainLooper());
    if (recordButton != null) {
      recordButton.setOnClickListener(v -> onRecordButtonClicked());
    }
    updateRecordingUiState();

    directoryPickerLauncher =
        registerForActivityResult(
            new ActivityResultContracts.OpenDocumentTree(),
            uri -> {
              if (uri == null) {
                Toast.makeText(
                        HelloArActivity.this,
                        R.string.recording_directory_picker_unavailable,
                        Toast.LENGTH_SHORT)
                    .show();
                return;
              }
              try {
                final int takeFlags =
                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
                getContentResolver().takePersistableUriPermission(uri, takeFlags);
                sharedPreferences
                    .edit()
                    .putString(PREF_RECORDING_TREE_URI, uri.toString())
                    .remove(PREF_RECORDING_DIR)
                    .apply();
                Toast.makeText(
                        HelloArActivity.this,
                        R.string.recording_output_saved_uri,
                        Toast.LENGTH_SHORT)
                    .show();
              } catch (SecurityException e) {
                Log.e(TAG, "Failed to take persistable uri permission", e);
                Toast.makeText(
                        HelloArActivity.this,
                        R.string.recording_no_directory_error,
                        Toast.LENGTH_SHORT)
                    .show();
              }
            });

    if (measurementOverlayView != null) {
      measurementOverlayView.setOnAnchorDeleteListener(
          anchorId -> {
            if (surfaceView != null) {
              surfaceView.queueEvent(() -> removeAnchorById(anchorId));
            } else {
              removeAnchorById(anchorId);
            }
          });
      measurementOverlayView.setOnPassthroughTouchListener(
          event -> {
            if (surfaceView != null) {
              surfaceView.dispatchTouchEvent(event);
            }
          });
      measurementOverlayView.setOnMeasurementInteractionListener(
          new MeasurementOverlayView.OnMeasurementInteractionListener() {
            @Override
            public float requestDistanceMeters(int startAnchorId, int endAnchorId) {
              synchronized (anchorLock) {
                Anchor start = getAnchorById(startAnchorId);
                Anchor end = getAnchorById(endAnchorId);
                return calculateDistanceBetweenAnchors(start, end);
              }
            }

            @Override
            public void onMeasurementCreated(int startAnchorId, int endAnchorId) {
              if (startAnchorId == endAnchorId) {
                return;
              }
              Runnable task =
                  () -> {
                    synchronized (anchorLock) {
                      Anchor start = getAnchorById(startAnchorId);
                      Anchor end = getAnchorById(endAnchorId);
                      if (start == null || end == null) {
                        return;
                      }
                      int measurementId = nextMeasurementId++;
                      measurementRecords.add(
                          new MeasurementRecord(measurementId, startAnchorId, endAnchorId));
                    }
                  };
              task.run();
            }

            @Override
            public void onMeasurementDeleted(int measurementId) {
              Runnable task =
                  () -> {
                    synchronized (anchorLock) {
                      measurementRecords.removeIf(record -> record.id == measurementId);
                    }
                  };
              if (surfaceView != null) {
                surfaceView.queueEvent(task);
              } else {
                task.run();
              }
            }
          });
    }
    Button resetButton = findViewById(R.id.reset_button);
    if (resetButton != null) {
      resetButton.setOnClickListener(
          v -> {
            Runnable task =
                () -> {
                  synchronized (anchorLock) {
                    for (WrappedAnchor wrappedAnchor : wrappedAnchors) {
                      wrappedAnchor.getAnchor().detach();
                    }
                    wrappedAnchors.clear();
                    measurementRecords.clear();
                    nextAnchorId = 1;
                    nextMeasurementId = 1;
                  }
                  runOnUiThread(
                      () -> {
                        if (measurementOverlayView != null) {
                          measurementOverlayView.clear();
                        }
                      });
                  resetArSession();
                };
            if (surfaceView != null) {
              surfaceView.queueEvent(task);
            } else {
              task.run();
            }
          });
    }
    displayRotationHelper = new DisplayRotationHelper(/* context= */ this);

    // Set up touch listener.
    tapHelper = new TapHelper(/* context= */ this);
    surfaceView.setOnTouchListener(tapHelper);

    // Set up renderer.
    render = new SampleRender(surfaceView, this, getAssets());

    installRequested = false;

    depthSettings.onCreate(this);
    instantPlacementSettings.onCreate(this);
    ImageButton settingsButton = findViewById(R.id.settings_button);
    settingsButton.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            PopupMenu popup = new PopupMenu(HelloArActivity.this, v);
            popup.setOnMenuItemClickListener(HelloArActivity.this::settingsMenuClick);
            popup.inflate(R.menu.settings_menu);
            popup.show();
          }
        });
  }

  /** Menu button to launch feature specific settings. */
  protected boolean settingsMenuClick(MenuItem item) {
    if (item.getItemId() == R.id.depth_settings) {
      launchDepthSettingsMenuDialog();
      return true;
    } else if (item.getItemId() == R.id.instant_placement_settings) {
      launchInstantPlacementSettingsMenuDialog();
      return true;
    } else if (item.getItemId() == R.id.recording_output_settings) {
      if (isRecording) {
        Toast.makeText(this, R.string.recording_in_progress_warning, Toast.LENGTH_SHORT).show();
      } else {
        showRecordingOutputDialog();
      }
      return true;
    }
    return false;
  }

  @Override
  protected void onDestroy() {
    try {
      if (isRecording) {
        stopRecordingInternal();
      }
    } catch (RuntimeException e) {
      Log.w(TAG, "Failed to stop recorder during onDestroy", e);
    }

    releaseGlResources();

    if (session != null) {
      // Explicitly close ARCore Session to release native resources.
      // Review the API reference for important considerations before calling close() in apps with
      // more complicated lifecycle requirements:
      // https://developers.google.com/ar/reference/java/arcore/reference/com/google/ar/core/Session#close()
      session.close();
      session = null;
    }

    super.onDestroy();
  }

  @Override
  protected void onResume() {
    super.onResume();

    if (session == null) {
      Exception exception = null;
      String message = null;
      try {
        // Always check the latest availability.
        Availability availability = ArCoreApk.getInstance().checkAvailability(this);

        // In all other cases, try to install ARCore and handle installation failures.
        if (availability != Availability.SUPPORTED_INSTALLED) {
          switch (ArCoreApk.getInstance().requestInstall(this, !installRequested)) {
            case INSTALL_REQUESTED:
              installRequested = true;
              return;
            case INSTALLED:
              break;
          }
        }

        // ARCore requires camera permissions to operate. If we did not yet obtain runtime
        // permission on Android M and above, now is a good time to ask the user for it.
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
          CameraPermissionHelper.requestCameraPermission(this);
          return;
        }

        // Create the session.
        session = new Session(/* context= */ this);
      } catch (UnavailableArcoreNotInstalledException
          | UnavailableUserDeclinedInstallationException e) {
        message = "Please install ARCore";
        exception = e;
      } catch (UnavailableApkTooOldException e) {
        message = "Please update ARCore";
        exception = e;
      } catch (UnavailableSdkTooOldException e) {
        message = "Please update this app";
        exception = e;
      } catch (UnavailableDeviceNotCompatibleException e) {
        message = "This device does not support AR";
        exception = e;
      } catch (Exception e) {
        message = "Failed to create AR session";
        exception = e;
      }

      if (message != null) {
        messageSnackbarHelper.showError(this, message);
        Log.e(TAG, "Exception creating session", exception);
        return;
      }
    }

    // Note that order matters - see the note in onPause(), the reverse applies here.
    try {
      configureSession();
      // To record a live camera session for later playback, call
      // `session.startRecording(recordingConfig)` at anytime. To playback a previously recorded AR
      // session instead of using the live camera feed, call
      // `session.setPlaybackDatasetUri(Uri)` before calling `session.resume()`. To
      // learn more about recording and playback, see:
      // https://developers.google.com/ar/develop/java/recording-and-playback
      session.resume();
    } catch (CameraNotAvailableException e) {
      messageSnackbarHelper.showError(this, "Camera not available. Try restarting the app.");
      session = null;
      return;
    }

    surfaceView.onResume();
    displayRotationHelper.onResume();
  }

  @Override
  public void onPause() {
    super.onPause();
    if (session != null) {
      if (isRecording) {
        stopRecordingInternal();
      }
      // Note that the order matters - GLSurfaceView is paused first so that it does not try
      // to query the session. If Session is paused before GLSurfaceView, GLSurfaceView may
      // still call session.update() and get a SessionPausedException.
      displayRotationHelper.onPause();
      surfaceView.onPause();
      session.pause();
    }
  }

  private void releaseGlResources() {
    hasSetTextureNames = false;

    if (surfaceView == null) {
      return;
    }

    surfaceView.queueEvent(
        () -> {
          if (virtualSceneFramebuffer != null) {
            virtualSceneFramebuffer.close();
            virtualSceneFramebuffer = null;
          }

          if (backgroundRenderer != null) {
            backgroundRenderer.close();
            backgroundRenderer = null;
          }

          if (planeRenderer != null) {
            planeRenderer.close();
            planeRenderer = null;
          }

          if (pointCloudMesh != null) {
            pointCloudMesh.close();
            pointCloudMesh = null;
          }
          if (pointCloudVertexBuffer != null) {
            pointCloudVertexBuffer.close();
            pointCloudVertexBuffer = null;
          }
          if (pointCloudShader != null) {
            pointCloudShader.close();
            pointCloudShader = null;
          }

          if (anchorPointMesh != null) {
            anchorPointMesh.close();
            anchorPointMesh = null;
          }
          if (anchorPointVertexBuffer != null) {
            anchorPointVertexBuffer.close();
            anchorPointVertexBuffer = null;
          }
          if (anchorPointShader != null) {
            anchorPointShader.close();
            anchorPointShader = null;
          }
          anchorPointFloatBuffer = null;

          if (virtualObjectMesh != null) {
            virtualObjectMesh.close();
            virtualObjectMesh = null;
          }
          if (virtualObjectShader != null) {
            virtualObjectShader.close();
            virtualObjectShader = null;
          }
          if (virtualObjectAlbedoTexture != null) {
            virtualObjectAlbedoTexture.close();
            virtualObjectAlbedoTexture = null;
          }
          if (virtualObjectAlbedoInstantPlacementTexture != null) {
            virtualObjectAlbedoInstantPlacementTexture.close();
            virtualObjectAlbedoInstantPlacementTexture = null;
          }
          if (virtualObjectPbrTexture != null) {
            virtualObjectPbrTexture.close();
            virtualObjectPbrTexture = null;
          }

          if (cubemapFilter != null) {
            cubemapFilter.close();
            cubemapFilter = null;
          }
          if (dfgTexture != null) {
            dfgTexture.close();
            dfgTexture = null;
          }
        });
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
    super.onRequestPermissionsResult(requestCode, permissions, results);
    if (!CameraPermissionHelper.hasCameraPermission(this)) {
      // Use toast instead of snackbar here since the activity will exit.
      Toast.makeText(this, "Camera permission is needed to run this application", Toast.LENGTH_LONG)
          .show();
      if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(this)) {
        // Permission denied with checking "Do not ask again".
        CameraPermissionHelper.launchPermissionSettings(this);
      }
      finish();
    }
  }

  @Override
  public void onWindowFocusChanged(boolean hasFocus) {
    super.onWindowFocusChanged(hasFocus);
    FullScreenHelper.setFullScreenOnWindowFocusChanged(this, hasFocus);
  }

  @Override
  public void onSurfaceCreated(SampleRender render) {
    // Prepare the rendering objects. This involves reading shaders and 3D model files, so may throw
    // an IOException.
    try {
      planeRenderer = new PlaneRenderer(render);
      backgroundRenderer = new BackgroundRenderer(render);
      virtualSceneFramebuffer = new Framebuffer(render, /* width= */ 1, /* height= */ 1);

      cubemapFilter =
          new SpecularCubemapFilter(
              render, CUBEMAP_RESOLUTION, CUBEMAP_NUMBER_OF_IMPORTANCE_SAMPLES);
      // Load DFG lookup table for environmental lighting
      dfgTexture =
          new Texture(
              render,
              Texture.Target.TEXTURE_2D,
              Texture.WrapMode.CLAMP_TO_EDGE,
              /* useMipmaps= */ false);
      // The dfg.raw file is a raw half-float texture with two channels.
      final int dfgResolution = 64;
      final int dfgChannels = 2;
      final int halfFloatSize = 2;

      ByteBuffer buffer =
          ByteBuffer.allocateDirect(dfgResolution * dfgResolution * dfgChannels * halfFloatSize);
      try (InputStream is = getAssets().open("models/dfg.raw")) {
        is.read(buffer.array());
      }
      // SampleRender abstraction leaks here.
      GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, dfgTexture.getTextureId());
      GLError.maybeThrowGLException("Failed to bind DFG texture", "glBindTexture");
      GLES30.glTexImage2D(
          GLES30.GL_TEXTURE_2D,
          /* level= */ 0,
          GLES30.GL_RG16F,
          /* width= */ dfgResolution,
          /* height= */ dfgResolution,
          /* border= */ 0,
          GLES30.GL_RG,
          GLES30.GL_HALF_FLOAT,
          buffer);
      GLError.maybeThrowGLException("Failed to populate DFG texture", "glTexImage2D");

      // Point cloud
      pointCloudShader =
          Shader.createFromAssets(
                  render,
                  "shaders/point_cloud.vert",
                  "shaders/point_cloud.frag",
                  /* defines= */ null)
              .setVec4(
                  "u_Color", new float[] {31.0f / 255.0f, 188.0f / 255.0f, 210.0f / 255.0f, 1.0f})
              .setFloat("u_PointSize", 5.0f);
      // four entries per vertex: X, Y, Z, confidence
      pointCloudVertexBuffer =
          new VertexBuffer(render, /* numberOfEntriesPerVertex= */ 4, /* entries= */ null);
      final VertexBuffer[] pointCloudVertexBuffers = {pointCloudVertexBuffer};
      pointCloudMesh =
          new Mesh(
              render, Mesh.PrimitiveMode.POINTS, /* indexBuffer= */ null, pointCloudVertexBuffers);

      anchorPointShader =
          Shader.createFromAssets(
                  render,
                  "shaders/point_cloud.vert",
                  "shaders/point_cloud.frag",
                  /* defines= */ null)
              .setVec4("u_Color", new float[] {1.0f, 0.2f, 0.2f, 1.0f})
              .setFloat("u_PointSize", 28.0f);
      anchorPointVertexBuffer =
          new VertexBuffer(render, /* numberOfEntriesPerVertex= */ 4, /* entries= */ null);
      final VertexBuffer[] anchorPointVertexBuffers = {anchorPointVertexBuffer};
      anchorPointMesh =
          new Mesh(
              render,
              Mesh.PrimitiveMode.POINTS,
              /* indexBuffer= */ null,
              anchorPointVertexBuffers);

      // Virtual object to render (ARCore pawn)
      virtualObjectAlbedoTexture =
          Texture.createFromAsset(
              render,
              "models/pawn_albedo.png",
              Texture.WrapMode.CLAMP_TO_EDGE,
              Texture.ColorFormat.SRGB);
      virtualObjectAlbedoInstantPlacementTexture =
          Texture.createFromAsset(
              render,
              "models/pawn_albedo_instant_placement.png",
              Texture.WrapMode.CLAMP_TO_EDGE,
              Texture.ColorFormat.SRGB);
      virtualObjectPbrTexture =
          Texture.createFromAsset(
              render,
              "models/pawn_roughness_metallic_ao.png",
              Texture.WrapMode.CLAMP_TO_EDGE,
              Texture.ColorFormat.LINEAR);

      virtualObjectMesh = Mesh.createFromAsset(render, "models/pawn.obj");
      virtualObjectShader =
          Shader.createFromAssets(
                  render,
                  "shaders/environmental_hdr.vert",
                  "shaders/environmental_hdr.frag",
                  /* defines= */ new HashMap<String, String>() {
                    {
                      put(
                          "NUMBER_OF_MIPMAP_LEVELS",
                          Integer.toString(cubemapFilter.getNumberOfMipmapLevels()));
                    }
                  })
              .setTexture("u_AlbedoTexture", virtualObjectAlbedoTexture)
              .setTexture("u_RoughnessMetallicAmbientOcclusionTexture", virtualObjectPbrTexture)
              .setTexture("u_Cubemap", cubemapFilter.getFilteredCubemapTexture())
              .setTexture("u_DfgTexture", dfgTexture);
    } catch (IOException e) {
      Log.e(TAG, "Failed to read a required asset file", e);
      messageSnackbarHelper.showError(this, "Failed to read a required asset file: " + e);
    }
  }

  @Override
  public void onSurfaceChanged(SampleRender render, int width, int height) {
    displayRotationHelper.onSurfaceChanged(width, height);
    virtualSceneFramebuffer.resize(width, height);
  }

  @Override
  public void onDrawFrame(SampleRender render) {
    if (session == null) {
      return;
    }

    // Texture names should only be set once on a GL thread unless they change. This is done during
    // onDrawFrame rather than onSurfaceCreated since the session is not guaranteed to have been
    // initialized during the execution of onSurfaceCreated.
    if (!hasSetTextureNames) {
      session.setCameraTextureNames(
          new int[] {backgroundRenderer.getCameraColorTexture().getTextureId()});
      hasSetTextureNames = true;
    }

    // -- Update per-frame state

    // Notify ARCore session that the view size changed so that the perspective matrix and
    // the video background can be properly adjusted.
    displayRotationHelper.updateSessionIfNeeded(session);

    // Obtain the current frame from the AR Session. When the configuration is set to
    // UpdateMode.BLOCKING (it is by default), this will throttle the rendering to the
    // camera framerate.
    Frame frame;
    try {
      frame = session.update();
    } catch (CameraNotAvailableException e) {
      Log.e(TAG, "Camera not available during onDrawFrame", e);
      messageSnackbarHelper.showError(this, "Camera not available. Try restarting the app.");
      return;
    }
    Camera camera = frame.getCamera();

    // Update BackgroundRenderer state to match the depth settings.
    try {
      backgroundRenderer.setUseDepthVisualization(
          render, depthSettings.depthColorVisualizationEnabled());
      backgroundRenderer.setUseOcclusion(render, depthSettings.useDepthForOcclusion());
    } catch (IOException e) {
      Log.e(TAG, "Failed to read a required asset file", e);
      messageSnackbarHelper.showError(this, "Failed to read a required asset file: " + e);
      return;
    }
    // BackgroundRenderer.updateDisplayGeometry must be called every frame to update the coordinates
    // used to draw the background camera image.
    backgroundRenderer.updateDisplayGeometry(frame);

    if (camera.getTrackingState() == TrackingState.TRACKING
        && (depthSettings.useDepthForOcclusion()
            || depthSettings.depthColorVisualizationEnabled())) {
      try (Image depthImage = frame.acquireDepthImage16Bits()) {
        backgroundRenderer.updateCameraDepthTexture(depthImage);
      } catch (NotYetAvailableException e) {
        // This normally means that depth data is not available yet. This is normal so we will not
        // spam the logcat with this.
      }
    }

    // Handle one tap per frame.
    handleTap(frame, camera);

    // Keep the screen unlocked while tracking, but allow it to lock when tracking stops.
    trackingStateHelper.updateKeepScreenOnFlag(camera.getTrackingState());

    // Show a message based on whether tracking has failed, if planes are detected, and if the user
    // has placed any objects.
    String message = null;
    if (camera.getTrackingState() == TrackingState.PAUSED) {
      if (camera.getTrackingFailureReason() == TrackingFailureReason.NONE) {
        message = SEARCHING_PLANE_MESSAGE;
      } else {
        message = TrackingStateHelper.getTrackingFailureReasonString(camera);
      }
    } else if (hasTrackingPlane()) {
      if (wrappedAnchors.isEmpty()) {
        message = WAITING_FOR_TAP_MESSAGE;
      }
    } else {
      message = SEARCHING_PLANE_MESSAGE;
    }
    if (message == null) {
      messageSnackbarHelper.hide(this);
    } else {
      messageSnackbarHelper.showMessage(this, message);
    }

    boolean renderedScene =
        drawScene(
            render,
            frame,
            camera,
            /*updatePointCloudBuffer=*/ true,
            /*updateLightEstimation=*/ true);

    if (renderedScene) {
      recordFrameData(frame);
      updateMeasurementUi(viewMatrix, projectionMatrix);
    }
    
    // Check if recording was requested and start it safely on UI thread
    if (startRecordingRequested) {
      startRecordingRequested = false;
      runOnUiThread(() -> startRecordingSafely());
    }
  }

  private boolean drawScene(
      SampleRender render,
      Frame frame,
      Camera camera,
      boolean updatePointCloudBuffer,
      boolean updateLightEstimation) {
    if (frame.getTimestamp() != 0) {
      backgroundRenderer.drawBackground(render);
    }

    if (camera.getTrackingState() == TrackingState.PAUSED) {
      return false;
    }

    camera.getProjectionMatrix(projectionMatrix, 0, Z_NEAR, Z_FAR);
    camera.getViewMatrix(viewMatrix, 0);

    if (updatePointCloudBuffer) {
      try (PointCloud pointCloud = frame.acquirePointCloud()) {
        if (pointCloud.getTimestamp() > lastPointCloudTimestamp) {
          pointCloudVertexBuffer.set(pointCloud.getPoints());
          lastPointCloudTimestamp = pointCloud.getTimestamp();
        }
      }
    }

    Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
    pointCloudShader.setMat4("u_ModelViewProjection", modelViewProjectionMatrix);
    render.draw(pointCloudMesh, pointCloudShader);

    planeRenderer.drawPlanes(
        render,
        session.getAllTrackables(Plane.class),
        camera.getDisplayOrientedPose(),
        projectionMatrix);

    if (updateLightEstimation) {
      updateLightEstimation(frame.getLightEstimate(), viewMatrix);
    }

    // Draw anchor markers directly to default framebuffer (works in both main and recording contexts)
    final java.util.List<WrappedAnchor> wrappedAnchorsSnapshot;
    synchronized (anchorLock) {
      wrappedAnchorsSnapshot = new java.util.ArrayList<>(wrappedAnchors);
    }

    if (anchorPointMesh != null && anchorPointShader != null && anchorPointVertexBuffer != null) {
      int maxAnchors = wrappedAnchorsSnapshot.size();
      if (maxAnchors > 0) {
        // Ensure buffer is large enough (4 floats per anchor: x, y, z, w)
        int requiredFloats = maxAnchors * 4;
        if (anchorPointFloatBuffer == null || anchorPointFloatBuffer.capacity() < requiredFloats) {
          anchorPointFloatBuffer =
              ByteBuffer.allocateDirect(requiredFloats * Float.BYTES)
                  .order(java.nio.ByteOrder.nativeOrder())
                  .asFloatBuffer();
        }
        
        // Clear buffer before use
        anchorPointFloatBuffer.clear();
        int trackingCount = 0;
        for (WrappedAnchor wrappedAnchor : wrappedAnchorsSnapshot) {
          Anchor anchor = wrappedAnchor.getAnchor();
          if (anchor.getTrackingState() != TrackingState.TRACKING) {
            continue;
          }
          anchor.getPose().getTranslation(anchorPointTmpTranslation, 0);
          anchorPointFloatBuffer.put(anchorPointTmpTranslation[0]);
          anchorPointFloatBuffer.put(anchorPointTmpTranslation[1]);
          anchorPointFloatBuffer.put(anchorPointTmpTranslation[2]);
          anchorPointFloatBuffer.put(1.0f);
          trackingCount++;
        }

        if (trackingCount > 0) {
          anchorPointFloatBuffer.flip();
          try {
            anchorPointVertexBuffer.set(anchorPointFloatBuffer);
            anchorPointShader.setMat4("u_ModelViewProjection", modelViewProjectionMatrix);
            render.draw(anchorPointMesh, anchorPointShader);
          } catch (Exception e) {
            Log.w(TAG, "Failed to render anchor points", e);
            // Continue without rendering anchor points
          }
        }
      }
    }

    render.clear(virtualSceneFramebuffer, 0f, 0f, 0f, 0f);

    for (WrappedAnchor wrappedAnchor : wrappedAnchorsSnapshot) {
      Anchor anchor = wrappedAnchor.getAnchor();
      if (anchor.getTrackingState() != TrackingState.TRACKING) {
        continue;
      }

      anchor.getPose().toMatrix(modelMatrix, 0);
      Matrix.multiplyMM(modelViewMatrix, 0, viewMatrix, 0, modelMatrix, 0);
      Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, modelViewMatrix, 0);
    }

    backgroundRenderer.drawVirtualScene(render, virtualSceneFramebuffer, Z_NEAR, Z_FAR);
    return true;
  }

  private ParcelFileDescriptor openParcelFileDescriptor(Uri uri) {
    if (uri == null) {
      return null;
    }
    try {
      return getContentResolver().openFileDescriptor(uri, "w");
    } catch (FileNotFoundException e) {
      Log.e(TAG, "Failed to open ParcelFileDescriptor", e);
      return null;
    }
  }

  private void onRecordButtonClicked() {
    if (session == null) {
      Toast.makeText(this, R.string.recording_output_invalid_path, Toast.LENGTH_SHORT).show();
      return;
    }
    if (!isRecording) {
      // Set flag to start recording on next frame completion
      startRecordingRequested = true;
    } else {
      stopRecordingInternal();
    }
  }


  private void showStopRecordingDialog() {
    if (!isRecording) {
      return;
    }
    
    new androidx.appcompat.app.AlertDialog.Builder(this)
        .setTitle("Stop Recording")
        .setMessage("Do you want to stop recording?")
        .setPositiveButton("Stop", (dialog, which) -> {
          if (isRecording) {
            stopRecordingInternal();
          }
        })
        .setNegativeButton("Cancel", null)
        .show();
  }

  private void startRecordingSafely() {
    if (isRecording || session == null || mediaProjectionManager == null) {
      return;
    }

    RecordingDestination destination = createRecordingDestination();
    if (destination == null) {
      return;
    }

    pendingRecordingDestination = destination;
    pendingStartAfterPermission = true;

    try {
      Intent captureIntent = mediaProjectionManager.createScreenCaptureIntent();
      screenCaptureLauncher.launch(captureIntent);
    } catch (ActivityNotFoundException e) {
      Log.e(TAG, "Screen capture intent unavailable", e);
      Toast.makeText(this, R.string.recording_prepare_failed, Toast.LENGTH_SHORT).show();
      pendingRecordingDestination.deleteArtifacts(this);
      pendingRecordingDestination = null;
      pendingStartAfterPermission = false;
    }
  }

  private void startScreenRecordingWithPermissionResult(ActivityResult result) {
    if (!pendingStartAfterPermission) {
      return;
    }
    pendingStartAfterPermission = false;

    if (mediaProjectionManager == null || pendingRecordingDestination == null) {
      Log.w(TAG, "Cannot start recording: prerequisites missing");
      return;
    }

    Intent data = result.getData();
    if (result.getResultCode() != Activity.RESULT_OK || data == null) {
      Log.w(TAG, "Screen capture permission denied by user");
      pendingRecordingDestination.deleteArtifacts(this);
      pendingRecordingDestination = null;
      resetRecordingState();
      updateRecordingUiState();
      Toast.makeText(this, R.string.recording_prepare_failed, Toast.LENGTH_SHORT).show();
      return;
    }

    mediaProjection = mediaProjectionManager.getMediaProjection(result.getResultCode(), data);
    if (mediaProjection == null) {
      Log.e(TAG, "MediaProjection returned null");
      pendingRecordingDestination.deleteArtifacts(this);
      pendingRecordingDestination = null;
      resetRecordingState();
      updateRecordingUiState();
      Toast.makeText(this, R.string.recording_prepare_failed, Toast.LENGTH_SHORT).show();
      return;
    }

    DisplayMetrics metrics = new DisplayMetrics();
    WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
    Display display = null;
    if (windowManager != null) {
      display = windowManager.getDefaultDisplay();
      if (display != null) {
        display.getRealMetrics(metrics);
      } else {
        metrics.setTo(getResources().getDisplayMetrics());
      }
    } else {
      metrics.setTo(getResources().getDisplayMetrics());
    }

    int captureWidth = metrics.widthPixels;
    int captureHeight = metrics.heightPixels;
    if (surfaceView != null && surfaceView.getWidth() > 0 && surfaceView.getHeight() > 0) {
      captureWidth = surfaceView.getWidth();
      captureHeight = surfaceView.getHeight();
    }

    // Ensure even dimensions for encoder compatibility.
    if ((captureWidth & 1) == 1) {
      captureWidth -= 1;
    }
    if ((captureHeight & 1) == 1) {
      captureHeight -= 1;
    }

    if (captureWidth <= 0 || captureHeight <= 0) {
      Log.e(TAG, "Invalid capture dimensions" + captureWidth + "x" + captureHeight);
      handlePreparationFailure();
      return;
    }

    if (!prepareMediaRecorder(pendingRecordingDestination, captureWidth, captureHeight, metrics.densityDpi)) {
      handlePreparationFailure();
      return;
    }

    try {
      recordingDataWriter = RecordingDataWriter.start(this, pendingRecordingDestination);
    } catch (IOException e) {
      Log.e(TAG, "Failed to create JSON writer", e);
      Toast.makeText(this, R.string.recording_prepare_failed, Toast.LENGTH_SHORT).show();
      handlePreparationFailure();
      return;
    }

    currentRecordingDestination = pendingRecordingDestination;
    currentRecordingDisplayLabel = pendingRecordingDestination.getVideoDisplayName();
    currentRecordingPath = pendingRecordingDestination.getVideoFilePath();
    currentRecordingContentUri = pendingRecordingDestination.getVideoUri();
    currentRecordingJsonPath = pendingRecordingDestination.getJsonFilePath();
    currentRecordingJsonUri = pendingRecordingDestination.getJsonUri();
    recordingFrameIndex = 0L;
    synchronized (planeLock) {
      planeIds.clear();
      nextPlaneId = 1;
    }

    mediaProjectionCallback =
        new MediaProjection.Callback() {
          @Override
          public void onStop() {
            runOnUiThread(() -> handleProjectionStoppedExternally());
          }
        };
    mediaProjection.registerCallback(mediaProjectionCallback, new Handler(Looper.getMainLooper()));

    VirtualDisplay createdDisplay =
        mediaProjection.createVirtualDisplay(
            SCREEN_CAPTURE_DISPLAY_NAME,
            captureWidth,
            captureHeight,
            metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            mediaRecorder.getSurface(),
            null,
            null);

    if (createdDisplay == null) {
      Log.e(TAG, "Failed to create virtual display");
      Toast.makeText(this, R.string.recording_prepare_failed, Toast.LENGTH_SHORT).show();
      handlePreparationFailure();
      return;
    }

    virtualDisplay = createdDisplay;

    try {
      mediaRecorder.start();
    } catch (RuntimeException e) {
      Log.e(TAG, "Failed to start MediaRecorder", e);
      Toast.makeText(this, R.string.recording_prepare_failed, Toast.LENGTH_SHORT).show();
      handlePreparationFailure();
      return;
    }

    pendingRecordingDestination = null;
    isRecording = true;
    recordingStartElapsedRealtime = SystemClock.elapsedRealtime();
    recordingDotVisible = true;
    startRecordingTimer();
    updateRecordingUiState();
    Toast.makeText(this, R.string.recording_start_success, Toast.LENGTH_SHORT).show();
  }

  private void handlePreparationFailure() {
    stopScreenRecording();
    if (pendingRecordingDestination != null) {
      pendingRecordingDestination.deleteArtifacts(this);
      pendingRecordingDestination = null;
    }
    resetRecordingState();
    updateRecordingUiState();
  }

  private boolean prepareMediaRecorder(
      RecordingDestination destination, int width, int height, int densityDpi) {
    releaseMediaRecorder(true);

    mediaRecorder = new MediaRecorder();
    try {
      if (shouldRecordAudio) {
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
      }
      mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
      mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
      if (shouldRecordAudio) {
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mediaRecorder.setAudioEncodingBitRate(128_000);
        mediaRecorder.setAudioSamplingRate(44_100);
      }
      mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
      mediaRecorder.setVideoEncodingBitRate(Math.max(width * height * 5, 4_000_000));
      mediaRecorder.setVideoFrameRate(30);
      mediaRecorder.setVideoSize(width, height);

      WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
      if (windowManager != null) {
        Display display = windowManager.getDefaultDisplay();
        if (display != null) {
          mediaRecorder.setOrientationHint(getRotationDegrees(display.getRotation()));
        }
      }

      if (destination.getVideoFilePath() != null) {
        mediaRecorder.setOutputFile(destination.getVideoFilePath());
      } else {
        closeRecordingParcel();
        recordingParcelFileDescriptor = openParcelFileDescriptor(destination.getVideoUri());
        if (recordingParcelFileDescriptor == null) {
          throw new IOException("Failed to open recording destination");
        }
        mediaRecorder.setOutputFile(recordingParcelFileDescriptor.getFileDescriptor());
      }

      mediaRecorder.prepare();
      return true;
    } catch (IOException | RuntimeException e) {
      Log.e(TAG, "Unable to prepare MediaRecorder", e);
      Toast.makeText(this, R.string.recording_prepare_failed, Toast.LENGTH_SHORT).show();
      releaseMediaRecorder(true);
      closeRecordingParcel();
      return false;
    }
  }

  private void handleProjectionStoppedExternally() {
    boolean success = stopScreenRecording();

    ScreenRecordingForegroundService.stop(this);

    if (recordingDataWriter != null) {
      recordingDataWriter.finish();
      recordingDataWriter = null;
    }

    String displayName = currentRecordingDisplayLabel;
    if (!success && currentRecordingDestination != null) {
      currentRecordingDestination.deleteArtifacts(this);
    }

    resetRecordingState();
    updateRecordingUiState();

    if (success) {
      if (!TextUtils.isEmpty(displayName)) {
        Toast.makeText(
                this, getString(R.string.recording_stop_success, displayName), Toast.LENGTH_LONG)
            .show();
      } else {
        Toast.makeText(this, R.string.recording_stop_generic, Toast.LENGTH_SHORT).show();
      }
    } else {
      Toast.makeText(this, R.string.recording_stopped_error, Toast.LENGTH_SHORT).show();
    }
  }

  private boolean stopScreenRecording() {
    boolean success = true;

    try {
      if (mediaRecorder != null) {
        mediaRecorder.stop();
      }
    } catch (RuntimeException e) {
      Log.e(TAG, "Failed to stop MediaRecorder", e);
      success = false;
    }

    releaseMediaRecorder(true);

    if (virtualDisplay != null) {
      virtualDisplay.release();
      virtualDisplay = null;
    }

    if (mediaProjectionCallback != null && mediaProjection != null) {
      mediaProjection.unregisterCallback(mediaProjectionCallback);
    }
    mediaProjectionCallback = null;

    if (mediaProjection != null) {
      mediaProjection.stop();
      mediaProjection = null;
    }

    closeRecordingParcel();

    return success;
  }

  private void stopRecordingInternal() {
    if (pendingStartAfterPermission) {
      pendingStartAfterPermission = false;
      if (pendingRecordingDestination != null) {
        pendingRecordingDestination.deleteArtifacts(this);
        pendingRecordingDestination = null;
      }
      updateRecordingUiState();
      return;
    }

    if (!isRecording || session == null) {
      return;
    }

    boolean success = stopScreenRecording();

    if (recordingDataWriter != null) {
      recordingDataWriter.finish();
      recordingDataWriter = null;
    }

    String displayName = currentRecordingDisplayLabel;
    if (!success && currentRecordingDestination != null) {
      currentRecordingDestination.deleteArtifacts(this);
    }

    resetRecordingState();
    updateRecordingUiState();

    if (success) {
      if (!TextUtils.isEmpty(displayName)) {
        Toast.makeText(
                this, getString(R.string.recording_stop_success, displayName), Toast.LENGTH_LONG)
            .show();
      } else {
        Toast.makeText(this, R.string.recording_stop_generic, Toast.LENGTH_SHORT).show();
      }
    } else {
      Toast.makeText(this, R.string.recording_stopped_error, Toast.LENGTH_SHORT).show();
    }
  }

  private void releaseMediaRecorder(boolean releaseCompletely) {
    if (mediaRecorder == null) {
      return;
    }

    try {
      mediaRecorder.reset();
    } catch (RuntimeException e) {
      Log.w(TAG, "Failed to reset MediaRecorder", e);
    }

    if (releaseCompletely) {
      mediaRecorder.release();
      mediaRecorder = null;
    }
  }

  private void closeRecordingParcel() {
    if (recordingParcelFileDescriptor != null) {
      try {
        recordingParcelFileDescriptor.close();
      } catch (IOException e) {
        Log.w(TAG, "Failed to close ParcelFileDescriptor", e);
      }
      recordingParcelFileDescriptor = null;
    }
  }

  private int getRotationDegrees(int rotation) {
    switch (rotation) {
      case Surface.ROTATION_90:
        return 0;
      case Surface.ROTATION_180:
        return 270;
      case Surface.ROTATION_270:
        return 180;
      case Surface.ROTATION_0:
      default:
        return 90;
    }
  }

  private void updateRecordingUiState() {
    if (recordButton != null) {
      // Hide button during recording so it doesn't appear in screen recordings
      recordButton.setVisibility(isRecording ? View.INVISIBLE : View.VISIBLE);
      recordButton.setBackgroundResource(
          isRecording ? R.drawable.record_button_recording : R.drawable.record_button_idle);
      recordButton.setContentDescription(
          getString(isRecording ? R.string.record_button_stop : R.string.record_button_start));
      recordButton.setEnabled(true);
    }
    if (recordingStatusContainer != null) {
      recordingStatusContainer.setVisibility(isRecording ? View.VISIBLE : View.GONE);
      // Add touch listener to stop recording when button is hidden
      if (isRecording) {
        recordingStatusContainer.setOnTouchListener((v, event) -> {
          if (event.getAction() == MotionEvent.ACTION_DOWN) {
            // Show confirmation dialog to stop recording
            showStopRecordingDialog();
            return true;
          }
          return false;
        });
      } else {
        recordingStatusContainer.setOnTouchListener(null);
      }
    }
    if (!isRecording && recordingTimerText != null) {
      recordingTimerText.setText(getString(R.string.recording_timer_default));
    }
    if (!isRecording && recordingStatusIndicator != null) {
      recordingStatusIndicator.setVisibility(View.VISIBLE);
      recordingDotVisible = true;
    }
  }

  private void showRecordingOutputDialog() {
    View contentView =
        getLayoutInflater().inflate(R.layout.dialog_recording_settings, null, false);
    SwitchCompat audioSwitch = contentView.findViewById(R.id.recording_audio_switch);
    audioSwitch.setChecked(shouldRecordAudio);
    audioSwitch.setOnCheckedChangeListener(
        (buttonView, isChecked) -> updateAudioPreference(isChecked));

    String[] options = {
      getString(R.string.recording_pick_folder_option),
      getString(R.string.recording_use_app_folder_option)
    };
    new AlertDialog.Builder(this)
        .setTitle(R.string.recording_output_settings_title)
        .setView(contentView)
        .setItems(
            options,
            (dialog, which) -> {
              if (which == 0) {
                directoryPickerLauncher.launch(getStoredRecordingTreeUri());
              } else if (which == 1) {
                File defaultDir = ensureDefaultRecordingDirectory();
                if (defaultDir != null) {
                  sharedPreferences
                      .edit()
                      .putString(PREF_RECORDING_DIR, defaultDir.getAbsolutePath())
                      .remove(PREF_RECORDING_TREE_URI)
                      .apply();
                  Toast.makeText(
                          this, R.string.recording_output_saved_default, Toast.LENGTH_SHORT)
                      .show();
                } else {
                  Toast.makeText(this, R.string.recording_output_invalid_path, Toast.LENGTH_SHORT)
                      .show();
                }
              }
            })
        .setPositiveButton(android.R.string.ok, null)
        .show();
  }

  private void updateAudioPreference(boolean enableAudio) {
    shouldRecordAudio = enableAudio;
    sharedPreferences.edit().putBoolean(PREF_RECORDING_AUDIO_ENABLED, enableAudio).apply();
  }

  private RecordingDestination createRecordingDestination() {
    String timestamp = new SimpleDateFormat(RECORDING_TIMESTAMP_PATTERN, Locale.US).format(new Date());
    String baseName = "AR_" + timestamp;

    if (hasStoredTreeUri()) {
      Uri treeUri = getStoredRecordingTreeUri();
      DocumentFile treeDirectory =
          treeUri != null ? DocumentFile.fromTreeUri(this, treeUri) : null;
      if (treeDirectory == null || !treeDirectory.canWrite() || !treeDirectory.isDirectory()) {
        Toast.makeText(this, R.string.recording_no_directory_error, Toast.LENGTH_SHORT).show();
        return null;
      }

      int suffix = 0;
      while (suffix < 1000) {
        String candidateBase = suffix == 0 ? baseName : baseName + "_" + suffix;
        DocumentFile existingVideo = treeDirectory.findFile(candidateBase + RECORDING_FILE_EXTENSION);
        DocumentFile existingJson = treeDirectory.findFile(candidateBase + ".json");
        if (existingVideo != null || existingJson != null) {
          suffix++;
          continue;
        }

        DocumentFile videoDoc =
            treeDirectory.createFile(
                RECORDING_MIME_TYPE, candidateBase + RECORDING_FILE_EXTENSION);
        if (videoDoc == null) {
          suffix++;
          continue;
        }

        DocumentFile jsonDoc =
            treeDirectory.createFile("application/json", candidateBase + ".json");
        if (jsonDoc == null) {
          videoDoc.delete();
          suffix++;
          continue;
        }

        return new RecordingDestination(
            candidateBase,
            videoDoc.getName(),
            null,
            null,
            videoDoc.getUri(),
            jsonDoc.getUri());
      }

      Toast.makeText(this, R.string.recording_prepare_failed, Toast.LENGTH_SHORT).show();
      return null;
    }

    String directoryPath = getOrCreateRecordingDirectoryPath();
    if (TextUtils.isEmpty(directoryPath)) {
      Toast.makeText(this, R.string.recording_output_invalid_path, Toast.LENGTH_SHORT).show();
      return null;
    }

    File directory = new File(directoryPath);
    if (!directory.exists() && !directory.mkdirs()) {
      Toast.makeText(this, R.string.recording_output_invalid_path, Toast.LENGTH_SHORT).show();
      return null;
    }

    String candidateBase = baseName;
    int suffix = 0;
    while (true) {
      File videoFile = new File(directory, candidateBase + RECORDING_FILE_EXTENSION);
      File jsonFile = new File(directory, candidateBase + ".json");
      if (!videoFile.exists() && !jsonFile.exists()) {
        return new RecordingDestination(
            candidateBase,
            videoFile.getName(),
            videoFile,
            jsonFile,
            null,
            null);
      }
      suffix++;
      candidateBase = baseName + "_" + suffix;
    }
  }

  private void recordFrameData(Frame frame) {
    if (!isRecording || recordingDataWriter == null) {
      return;
    }

    long timestampNs = frame.getTimestamp();
    long frameIndex = recordingFrameIndex++;
    Map<Integer, float[]> anchorData = new LinkedHashMap<>();
    synchronized (anchorLock) {
      for (WrappedAnchor wrappedAnchor : wrappedAnchors) {
        Anchor anchor = wrappedAnchor.getAnchor();
        if (anchor.getTrackingState() != TrackingState.TRACKING) {
          continue;
        }
        float[] translation = anchor.getPose().getTranslation();
        anchorData.put(
            wrappedAnchor.getId(), new float[] {translation[0], translation[1], translation[2]});
      }
    }

    Map<Integer, PlaneFrameData> planeData = new LinkedHashMap<>();
    if (session != null) {
      Collection<Plane> allPlanes = session.getAllTrackables(Plane.class);
      synchronized (planeLock) {
        Iterator<Map.Entry<Plane, Integer>> iterator = planeIds.entrySet().iterator();
        while (iterator.hasNext()) {
          Map.Entry<Plane, Integer> entry = iterator.next();
          Plane plane = entry.getKey();
          if (plane.getTrackingState() == TrackingState.STOPPED || plane.getSubsumedBy() != null) {
            iterator.remove();
          }
        }
        for (Plane plane : allPlanes) {
          if (plane.getTrackingState() != TrackingState.TRACKING || plane.getSubsumedBy() != null) {
            continue;
          }
          int planeId = planeIds.computeIfAbsent(plane, unused -> nextPlaneId++);
          planeData.put(planeId, PlaneFrameData.fromPlane(plane));
        }
      }
    }

    recordingDataWriter.appendFrame(frameIndex, timestampNs, anchorData, planeData);
  }

  private static final class PlaneFrameData {
    final float[] centerPose;
    final float[] extent;
    final float[] polygon;
    final Plane.Type type;

    private PlaneFrameData(float[] centerPose, float[] extent, float[] polygon, Plane.Type type) {
      this.centerPose = centerPose;
      this.extent = extent;
      this.polygon = polygon;
      this.type = type;
    }

    static PlaneFrameData fromPlane(Plane plane) {
      float[] poseTranslation = new float[3];
      float[] poseRotation = new float[4];
      plane.getCenterPose().getTranslation(poseTranslation, 0);
      plane.getCenterPose().getRotationQuaternion(poseRotation, 0);
      float[] centerPose =
          new float[] {
            poseTranslation[0],
            poseTranslation[1],
            poseTranslation[2],
            poseRotation[0],
            poseRotation[1],
            poseRotation[2],
            poseRotation[3]
          };

      float[] extent = new float[] {plane.getExtentX(), plane.getExtentZ()};
      FloatBuffer polygonBuffer = plane.getPolygon();
      FloatBuffer duplicated = polygonBuffer.duplicate();
      duplicated.rewind();
      float[] polygon = new float[duplicated.remaining()];
      duplicated.get(polygon);

      return new PlaneFrameData(centerPose, extent, polygon, plane.getType());
    }
  }

  private void startRecordingTimer() {
    if (recordingTimerHandler != null) {
      recordingTimerHandler.removeCallbacks(recordingTimerRunnable);
      recordingTimerHandler.post(recordingTimerRunnable);
    }
  }

  private void stopRecordingTimer() {
    if (recordingTimerHandler != null) {
      recordingTimerHandler.removeCallbacks(recordingTimerRunnable);
    }
  }

  private void updateRecordingTimerUi() {
    if (!isRecording) {
      return;
    }
    long elapsedMillis = SystemClock.elapsedRealtime() - recordingStartElapsedRealtime;
    int totalSeconds = (int) (elapsedMillis / 1000L);
    int minutes = totalSeconds / 60;
    int seconds = totalSeconds % 60;
    if (recordingTimerText != null) {
      recordingTimerText.setText(String.format(Locale.US, "%02d:%02d", minutes, seconds));
    }
    if (recordingStatusIndicator != null) {
      recordingStatusIndicator.setVisibility(recordingDotVisible ? View.VISIBLE : View.INVISIBLE);
      recordingDotVisible = !recordingDotVisible;
    }
  }

  private void resetRecordingState() {
    stopRecordingTimer();
    isRecording = false;
    currentRecordingPath = null;
    currentRecordingContentUri = null;
    currentRecordingDisplayLabel = null;
    currentRecordingJsonPath = null;
    currentRecordingJsonUri = null;
    currentRecordingDestination = null;
    recordingStartElapsedRealtime = 0L;
    recordingDotVisible = true;
    recordingFrameIndex = 0L;
    synchronized (planeLock) {
      planeIds.clear();
      nextPlaneId = 1;
    }
  }

  private boolean hasStoredTreeUri() {
    return !TextUtils.isEmpty(sharedPreferences.getString(PREF_RECORDING_TREE_URI, null));
  }

  private Uri getStoredRecordingTreeUri() {
    String stored = sharedPreferences.getString(PREF_RECORDING_TREE_URI, null);
    return TextUtils.isEmpty(stored) ? null : Uri.parse(stored);
  }

  private String getOrCreateRecordingDirectoryPath() {
    String storedPath = sharedPreferences.getString(PREF_RECORDING_DIR, null);
    if (!TextUtils.isEmpty(storedPath)) {
      File storedDir = new File(storedPath);
      if (storedDir.exists() || storedDir.mkdirs()) {
        return storedDir.getAbsolutePath();
      }
    }
    File defaultDir = ensureDefaultRecordingDirectory();
    if (defaultDir == null) {
      return null;
    }
    return defaultDir.getAbsolutePath();
  }

  private File ensureDefaultRecordingDirectory() {
    File defaultDir = new File(getExternalFilesDir(null), DEFAULT_RECORDING_SUBDIR);
    if (!defaultDir.exists() && !defaultDir.mkdirs()) {
      return null;
    }
    return defaultDir;
  }

  private static final class RecordingDestination {
    private final String baseName;
    private final String videoDisplayName;
    private final File videoFile;
    private final File jsonFile;
    private final Uri videoUri;
    private final Uri jsonUri;

    RecordingDestination(
        String baseName,
        String videoDisplayName,
        File videoFile,
        File jsonFile,
        Uri videoUri,
        Uri jsonUri) {
      this.baseName = baseName;
      this.videoDisplayName = videoDisplayName;
      this.videoFile = videoFile;
      this.jsonFile = jsonFile;
      this.videoUri = videoUri;
      this.jsonUri = jsonUri;
    }

    String getBaseName() {
      return baseName;
    }

    String getVideoDisplayName() {
      return videoDisplayName;
    }

    String getVideoFilePath() {
      return videoFile != null ? videoFile.getAbsolutePath() : null;
    }

    Uri getVideoUri() {
      return videoUri;
    }

    String getJsonFilePath() {
      return jsonFile != null ? jsonFile.getAbsolutePath() : null;
    }

    Uri getJsonUri() {
      return jsonUri;
    }

    void deleteArtifacts(Context context) {
      if (videoFile != null && videoFile.exists()) {
        //noinspection ResultOfMethodCallIgnored
        videoFile.delete();
      }
      if (jsonFile != null && jsonFile.exists()) {
        //noinspection ResultOfMethodCallIgnored
        jsonFile.delete();
      }
      if (videoUri != null) {
        DocumentFile doc = DocumentFile.fromSingleUri(context, videoUri);
        if (doc != null) {
          doc.delete();
        }
      }
      if (jsonUri != null) {
        DocumentFile doc = DocumentFile.fromSingleUri(context, jsonUri);
        if (doc != null) {
          doc.delete();
        }
      }
    }
  }

  private static final class RecordingDataWriter {
    private static final String KEY_VIDEO = "video";
    private static final String KEY_COORDINATE_SYSTEM = "coordinate_system";
    private static final String KEY_UNIT = "unit";
    private static final String KEY_FRAMES = "frames";
    private static final String VALUE_COORDINATE_SYSTEM = "arcore_world";
    private static final String VALUE_UNIT = "meters";

    private final ExecutorService executor;
    private final OutputStream outputStream;
    private final JsonWriter jsonWriter;
    private final String videoDisplayName;
    private final boolean ownsFile;
    private final RecordingDestination destination;
    private boolean closed = false;

    static RecordingDataWriter start(Context context, RecordingDestination destination)
        throws IOException {
      OutputStream outputStream;
      boolean ownsFile = false;
      if (destination.getJsonFilePath() != null) {
        File jsonFile = new File(destination.getJsonFilePath());
        outputStream = new BufferedOutputStream(new FileOutputStream(jsonFile));
        ownsFile = true;
      } else if (destination.getJsonUri() != null) {
        outputStream =
            new BufferedOutputStream(
                context.getContentResolver().openOutputStream(destination.getJsonUri(), "w"));
        if (outputStream == null) {
          throw new IOException("Unable to open JSON output stream");
        }
      } else {
        throw new IOException("No JSON destination available");
      }

      JsonWriter writer =
          new JsonWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));
      writer.setIndent("  ");

      ExecutorService executor = Executors.newSingleThreadExecutor();
      RecordingDataWriter dataWriter =
          new RecordingDataWriter(executor, outputStream, writer, destination, ownsFile);
      dataWriter.writeHeader();
      return dataWriter;
    }

    private RecordingDataWriter(
        ExecutorService executor,
        OutputStream outputStream,
        JsonWriter jsonWriter,
        RecordingDestination destination,
        boolean ownsFile)
        throws IOException {
      this.executor = executor;
      this.outputStream = outputStream;
      this.jsonWriter = jsonWriter;
      this.destination = destination;
      this.videoDisplayName = destination.getVideoDisplayName();
      this.ownsFile = ownsFile;
    }

    private void writeHeader() throws IOException {
      jsonWriter.beginObject();
      jsonWriter.name(KEY_VIDEO).value(videoDisplayName);
      jsonWriter.name(KEY_COORDINATE_SYSTEM).value(VALUE_COORDINATE_SYSTEM);
      jsonWriter.name(KEY_UNIT).value(VALUE_UNIT);
      jsonWriter.name(KEY_FRAMES);
      jsonWriter.beginObject();
      jsonWriter.flush();
    }

    void appendFrame(
        long frameIndex,
        long timestampNs,
        Map<Integer, float[]> anchors,
        Map<Integer, PlaneFrameData> planes) {
      if (closed) {
        return;
      }
      Map<Integer, float[]> anchorSnapshot = new LinkedHashMap<>(anchors);
      Map<Integer, PlaneFrameData> planeSnapshot = new LinkedHashMap<>(planes);
      try {
        executor.submit(
            () -> {
              try {
                if (anchorSnapshot.isEmpty() && planeSnapshot.isEmpty()) {
                  return;
                }
                jsonWriter.name(Long.toString(frameIndex));
                jsonWriter.beginObject();
                jsonWriter.name("timestamp_ns").value(timestampNs);
                jsonWriter.name("anchors");
                jsonWriter.beginObject();
                for (Map.Entry<Integer, float[]> entry : anchorSnapshot.entrySet()) {
                  jsonWriter.name("anchor_" + entry.getKey());
                  float[] values = entry.getValue();
                  jsonWriter.beginObject();
                  jsonWriter.name("x").value(values[0]);
                  jsonWriter.name("y").value(values[1]);
                  jsonWriter.name("z").value(values[2]);
                  jsonWriter.endObject();
                }
                jsonWriter.endObject();
                jsonWriter.name("planes");
                jsonWriter.beginObject();
                for (Map.Entry<Integer, PlaneFrameData> entry : planeSnapshot.entrySet()) {
                  PlaneFrameData data = entry.getValue();
                  jsonWriter.name("plane_" + entry.getKey());
                  jsonWriter.beginObject();
                  jsonWriter.name("type").value(data.type.name());
                  jsonWriter.name("center_pose");
                  jsonWriter.beginArray();
                  for (float value : data.centerPose) {
                    jsonWriter.value(value);
                  }
                  jsonWriter.endArray();
                  jsonWriter.name("extent");
                  jsonWriter.beginArray();
                  for (float value : data.extent) {
                    jsonWriter.value(value);
                  }
                  jsonWriter.endArray();
                  jsonWriter.name("polygon");
                  jsonWriter.beginArray();
                  for (float value : data.polygon) {
                    jsonWriter.value(value);
                  }
                  jsonWriter.endArray();
                  jsonWriter.endObject();
                }
                jsonWriter.endObject();
                jsonWriter.endObject();
                jsonWriter.flush();
              } catch (IOException e) {
                Log.e(TAG, "Failed to write frame data", e);
              }
            });
      } catch (RejectedExecutionException e) {
        Log.w(TAG, "Recording data writer executor rejected frame", e);
      }
    }

    void finish() {
      if (closed) {
        return;
      }
      closed = true;
      executor.shutdown();
      try {
        executor.awaitTermination(2, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      try {
        jsonWriter.endObject();
        jsonWriter.endObject();
        jsonWriter.flush();
      } catch (IOException e) {
        Log.e(TAG, "Failed to finish JSON writing", e);
      }
      try {
        jsonWriter.close();
      } catch (IOException e) {
        Log.w(TAG, "Failed to close JsonWriter", e);
      }
      try {
        outputStream.close();
      } catch (IOException e) {
        Log.w(TAG, "Failed to close JSON output stream", e);
      }
    }
  }

  // Handle only one tap per frame, as taps are usually low frequency compared to frame rate.
  private void handleTap(Frame frame, Camera camera) {
    MotionEvent tap = tapHelper.poll();
    if (tap != null && camera.getTrackingState() == TrackingState.TRACKING) {
      List<HitResult> hitResultList;
      if (instantPlacementSettings.isInstantPlacementEnabled()) {
        hitResultList =
            frame.hitTestInstantPlacement(tap.getX(), tap.getY(), APPROXIMATE_DISTANCE_METERS);
      } else {
        hitResultList = frame.hitTest(tap);
      }
      for (HitResult hit : hitResultList) {
        // If any plane, Oriented Point, or Instant Placement Point was hit, create an anchor.
        Trackable trackable = hit.getTrackable();
        // If a plane was hit, check that it was hit inside the plane polygon.
        // DepthPoints are only returned if Config.DepthMode is set to AUTOMATIC.
        if ((trackable instanceof Plane
                && ((Plane) trackable).isPoseInPolygon(hit.getHitPose())
                && (PlaneRenderer.calculateDistanceToPlane(hit.getHitPose(), camera.getPose()) > 0))
            || (trackable instanceof Point
                && ((Point) trackable).getOrientationMode()
                    == OrientationMode.ESTIMATED_SURFACE_NORMAL)
            || (trackable instanceof InstantPlacementPoint)
            || (trackable instanceof DepthPoint)) {
          // Cap the number of objects created. This avoids overloading both the
          // rendering system and ARCore.
          final int wrappedAnchorsCount;
          synchronized (anchorLock) {
            wrappedAnchorsCount = wrappedAnchors.size();
          }
          if (wrappedAnchorsCount >= 20) {
            WrappedAnchor toRemove;
            synchronized (anchorLock) {
              if (wrappedAnchors.isEmpty()) {
                break;
              }
              toRemove = wrappedAnchors.get(0);
            }
            if (toRemove != null) {
              removeAnchorById(toRemove.getId());
            }
          }

          // Adding an Anchor tells ARCore that it should track this position in
          // space. This anchor is created on the Plane to place the 3D model
          // in the correct position relative both to the world and to the plane.
          Anchor newAnchor = hit.createAnchor();
          int anchorId = nextAnchorId++;
          synchronized (anchorLock) {
            wrappedAnchors.add(new WrappedAnchor(anchorId, newAnchor, trackable));
          }
          // For devices that support the Depth API, shows a dialog to suggest enabling
          // depth-based occlusion. This dialog needs to be spawned on the UI thread.
          this.runOnUiThread(this::showOcclusionDialogIfNeeded);

          // Hits are sorted by depth. Consider only closest hit on a plane, Oriented Point, or
          // Instant Placement Point.
          break;
        }
      }
    }
  }

  /**
   * Shows a pop-up dialog on the first call, determining whether the user wants to enable
   * depth-based occlusion. The result of this dialog can be retrieved with useDepthForOcclusion().
   */
  private void showOcclusionDialogIfNeeded() {
    boolean isDepthSupported = session.isDepthModeSupported(Config.DepthMode.AUTOMATIC);
    if (!depthSettings.shouldShowDepthEnableDialog() || !isDepthSupported) {
      return; // Don't need to show dialog.
    }

    // Asks the user whether they want to use depth-based occlusion.
    new AlertDialog.Builder(this)
        .setTitle(R.string.options_title_with_depth)
        .setMessage(R.string.depth_use_explanation)
        .setPositiveButton(
            R.string.button_text_enable_depth,
            (DialogInterface dialog, int which) -> {
              depthSettings.setUseDepthForOcclusion(true);
            })
        .setNegativeButton(
            R.string.button_text_disable_depth,
            (DialogInterface dialog, int which) -> {
              depthSettings.setUseDepthForOcclusion(false);
            })
        .show();
  }

  private void launchInstantPlacementSettingsMenuDialog() {
    resetSettingsMenuDialogCheckboxes();
    Resources resources = getResources();
    new AlertDialog.Builder(this)
        .setTitle(R.string.options_title_instant_placement)
        .setMultiChoiceItems(
            resources.getStringArray(R.array.instant_placement_options_array),
            instantPlacementSettingsMenuDialogCheckboxes,
            (DialogInterface dialog, int which, boolean isChecked) ->
                instantPlacementSettingsMenuDialogCheckboxes[which] = isChecked)
        .setPositiveButton(
            R.string.done,
            (DialogInterface dialogInterface, int which) -> applySettingsMenuDialogCheckboxes())
        .setNegativeButton(
            android.R.string.cancel,
            (DialogInterface dialog, int which) -> resetSettingsMenuDialogCheckboxes())
        .show();
  }

  /** Shows checkboxes to the user to facilitate toggling of depth-based effects. */
  private void launchDepthSettingsMenuDialog() {
    // Retrieves the current settings to show in the checkboxes.
    resetSettingsMenuDialogCheckboxes();

    // Shows the dialog to the user.
    Resources resources = getResources();
    if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
      // With depth support, the user can select visualization options.
      new AlertDialog.Builder(this)
          .setTitle(R.string.options_title_with_depth)
          .setMultiChoiceItems(
              resources.getStringArray(R.array.depth_options_array),
              depthSettingsMenuDialogCheckboxes,
              (DialogInterface dialog, int which, boolean isChecked) ->
                  depthSettingsMenuDialogCheckboxes[which] = isChecked)
          .setPositiveButton(
              R.string.done,
              (DialogInterface dialogInterface, int which) -> applySettingsMenuDialogCheckboxes())
          .setNegativeButton(
              android.R.string.cancel,
              (DialogInterface dialog, int which) -> resetSettingsMenuDialogCheckboxes())
          .show();
    } else {
      // Without depth support, no settings are available.
      new AlertDialog.Builder(this)
          .setTitle(R.string.options_title_without_depth)
          .setPositiveButton(
              R.string.done,
              (DialogInterface dialogInterface, int which) -> applySettingsMenuDialogCheckboxes())
          .show();
    }
  }

  private void applySettingsMenuDialogCheckboxes() {
    depthSettings.setUseDepthForOcclusion(depthSettingsMenuDialogCheckboxes[0]);
    depthSettings.setDepthColorVisualizationEnabled(depthSettingsMenuDialogCheckboxes[1]);
    instantPlacementSettings.setInstantPlacementEnabled(
        instantPlacementSettingsMenuDialogCheckboxes[0]);
    configureSession();
  }

  private void resetSettingsMenuDialogCheckboxes() {
    depthSettingsMenuDialogCheckboxes[0] = depthSettings.useDepthForOcclusion();
    depthSettingsMenuDialogCheckboxes[1] = depthSettings.depthColorVisualizationEnabled();
    instantPlacementSettingsMenuDialogCheckboxes[0] =
        instantPlacementSettings.isInstantPlacementEnabled();
  }

  /** Checks if we detected at least one plane. */
  private boolean hasTrackingPlane() {
    for (Plane plane : session.getAllTrackables(Plane.class)) {
      if (plane.getTrackingState() == TrackingState.TRACKING) {
        return true;
      }
    }
    return false;
  }

  /** Update state based on the current frame's light estimation. */
  private void updateLightEstimation(LightEstimate lightEstimate, float[] viewMatrix) {
    if (lightEstimate.getState() != LightEstimate.State.VALID) {
      virtualObjectShader.setBool("u_LightEstimateIsValid", false);
      return;
    }
    virtualObjectShader.setBool("u_LightEstimateIsValid", true);

    Matrix.invertM(viewInverseMatrix, 0, viewMatrix, 0);
    virtualObjectShader.setMat4("u_ViewInverse", viewInverseMatrix);

    updateMainLight(
        lightEstimate.getEnvironmentalHdrMainLightDirection(),
        lightEstimate.getEnvironmentalHdrMainLightIntensity(),
        viewMatrix);
    updateSphericalHarmonicsCoefficients(
        lightEstimate.getEnvironmentalHdrAmbientSphericalHarmonics());
    cubemapFilter.update(lightEstimate.acquireEnvironmentalHdrCubeMap());
  }

  private void updateMainLight(float[] direction, float[] intensity, float[] viewMatrix) {
    // We need the direction in a vec4 with 0.0 as the final component to transform it to view space
    worldLightDirection[0] = direction[0];
    worldLightDirection[1] = direction[1];
    worldLightDirection[2] = direction[2];
    Matrix.multiplyMV(viewLightDirection, 0, viewMatrix, 0, worldLightDirection, 0);
    virtualObjectShader.setVec4("u_ViewLightDirection", viewLightDirection);
    virtualObjectShader.setVec3("u_LightIntensity", intensity);
  }

  private void updateSphericalHarmonicsCoefficients(float[] coefficients) {
    // Pre-multiply the spherical harmonics coefficients before passing them to the shader. The
    // constants in sphericalHarmonicFactors were derived from three terms:
    //
    // 1. The normalized spherical harmonics basis functions (y_lm)
    //
    // 2. The lambertian diffuse BRDF factor (1/pi)
    //
    // 3. A <cos> convolution. This is done to so that the resulting function outputs the irradiance
    // of all incoming light over a hemisphere for a given surface normal, which is what the shader
    // (environmental_hdr.frag) expects.
    //
    // You can read more details about the math here:
    // https://google.github.io/filament/Filament.html#annex/sphericalharmonics

    if (coefficients.length != 9 * 3) {
      throw new IllegalArgumentException(
          "The given coefficients array must be of length 27 (3 components per 9 coefficients");
    }

    // Apply each factor to every component of each coefficient
    for (int i = 0; i < 9 * 3; ++i) {
      sphericalHarmonicsCoefficients[i] = coefficients[i] * sphericalHarmonicFactors[i / 3];
    }
    virtualObjectShader.setVec3Array(
        "u_SphericalHarmonicsCoefficients", sphericalHarmonicsCoefficients);
  }

  /** Configures the session with feature settings. */
  private void configureSession() {
    Config config = session.getConfig();
    config.setLightEstimationMode(Config.LightEstimationMode.ENVIRONMENTAL_HDR);
    if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
      config.setDepthMode(Config.DepthMode.AUTOMATIC);
    } else {
      config.setDepthMode(Config.DepthMode.DISABLED);
    }
    if (instantPlacementSettings.isInstantPlacementEnabled()) {
      config.setInstantPlacementMode(InstantPlacementMode.LOCAL_Y_UP);
    } else {
      config.setInstantPlacementMode(InstantPlacementMode.DISABLED);
    }
    session.configure(config);
  }

  private float calculateDistanceBetweenAnchors(Anchor first, Anchor second) {
    if (first == null || second == null) {
      return Float.NaN;
    }

    float[] firstPose = first.getPose().getTranslation();
    float[] secondPose = second.getPose().getTranslation();

    float dx = firstPose[0] - secondPose[0];
    float dy = firstPose[1] - secondPose[1];
    float dz = firstPose[2] - secondPose[2];

    return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
  }

  private void updateMeasurementUi(float[] viewMatrix, float[] projectionMatrix) {
    if (measurementOverlayView == null) {
      return;
    }

    List<Anchor> trackingAnchors = new ArrayList<>();
    List<Integer> trackingAnchorIds = new ArrayList<>();
    synchronized (anchorLock) {
      for (WrappedAnchor wrappedAnchor : wrappedAnchors) {
        Anchor anchor = wrappedAnchor.getAnchor();
        if (anchor.getTrackingState() == TrackingState.TRACKING) {
          trackingAnchors.add(anchor);
          trackingAnchorIds.add(wrappedAnchor.getId());
        }
      }
    }

    if (trackingAnchors.isEmpty()) {
      runOnUiThread(
          () -> {
            measurementOverlayView.setAnchors(Collections.emptyList(), Collections.emptyList());
            measurementOverlayView.setMeasurementLines(Collections.emptyList());
          });
      return;
    }

    int width = measurementOverlayView.getWidth();
    int height = measurementOverlayView.getHeight();
    if (width == 0 || height == 0) {
      width = surfaceView.getWidth();
      height = surfaceView.getHeight();
    }
    if (width == 0 || height == 0) {
      return;
    }

    float[] viewProjectionMatrix = new float[16];
    Matrix.multiplyMM(viewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0);

    List<PointF> screenPoints = new ArrayList<>();
    boolean projectionFailed = false;
    for (Anchor anchor : trackingAnchors) {
      PointF point =
          worldToScreen(anchor.getPose().getTranslation(), viewProjectionMatrix, width, height);
      if (point == null) {
        projectionFailed = true;
        break;
      }
      screenPoints.add(point);
    }

    if (projectionFailed || screenPoints.size() != trackingAnchors.size()) {
      return;
    }

    final List<PointF> overlayPoints = new ArrayList<>(screenPoints);
    final List<Integer> overlayAnchorIds = new ArrayList<>(trackingAnchorIds);
    final Set<Integer> trackingAnchorIdSet = new HashSet<>(trackingAnchorIds);
    final Map<Integer, Anchor> overlayAnchorMap = new HashMap<>();
    for (int i = 0; i < trackingAnchors.size(); ++i) {
      overlayAnchorMap.put(trackingAnchorIds.get(i), trackingAnchors.get(i));
    }
    final List<MeasurementOverlayView.MeasurementLine> overlayLines =
        buildMeasurementLines(trackingAnchorIdSet, overlayAnchorMap);

    runOnUiThread(
        () -> {
          measurementOverlayView.setAnchors(overlayPoints, overlayAnchorIds);
          measurementOverlayView.setMeasurementLines(overlayLines);
        });
  }

  private List<MeasurementOverlayView.MeasurementLine> buildMeasurementLines(
      Set<Integer> trackingAnchorIds, Map<Integer, Anchor> anchorMap) {
    List<MeasurementOverlayView.MeasurementLine> lines = new ArrayList<>();
    synchronized (anchorLock) {
      for (MeasurementRecord record : measurementRecords) {
        if (!trackingAnchorIds.contains(record.startAnchorId)
            || !trackingAnchorIds.contains(record.endAnchorId)) {
          continue;
        }
        Anchor start = anchorMap.get(record.startAnchorId);
        Anchor end = anchorMap.get(record.endAnchorId);
        if (start == null || end == null) {
          continue;
        }
        float distance = calculateDistanceBetweenAnchors(start, end);
        if (Float.isNaN(distance)) {
          continue;
        }
        lines.add(
            new MeasurementOverlayView.MeasurementLine(
                record.id, record.startAnchorId, record.endAnchorId, distance));
      }
    }
    return lines;
  }

  private PointF worldToScreen(float[] worldPosition, float[] viewProjectionMatrix, int width, int height) {
    if (worldPosition == null) {
      return null;
    }

    float[] worldPoint = {worldPosition[0], worldPosition[1], worldPosition[2], 1.0f};
    float[] clipCoords = new float[4];
    Matrix.multiplyMV(clipCoords, 0, viewProjectionMatrix, 0, worldPoint, 0);

    if (clipCoords[3] <= 0f) {
      return null;
    }

    float ndcX = clipCoords[0] / clipCoords[3];
    float ndcY = clipCoords[1] / clipCoords[3];

    float screenX = (ndcX * 0.5f + 0.5f) * width;
    float screenY = (0.5f - ndcY * 0.5f) * height;
    return new PointF(screenX, screenY);
  }
  private Anchor getAnchorById(int anchorId) {
    synchronized (anchorLock) {
      return getAnchorByIdLocked(anchorId);
    }
  }

  private Anchor getAnchorByIdLocked(int anchorId) {
    for (WrappedAnchor wrappedAnchor : wrappedAnchors) {
      if (wrappedAnchor.getId() == anchorId) {
        return wrappedAnchor.getAnchor();
      }
    }
    return null;
  }

  private void removeAnchorById(int anchorId) {
    WrappedAnchor toDetach = null;
    synchronized (anchorLock) {
      Iterator<WrappedAnchor> iterator = wrappedAnchors.iterator();
      while (iterator.hasNext()) {
        WrappedAnchor candidate = iterator.next();
        if (candidate.getId() == anchorId) {
          toDetach = candidate;
          iterator.remove();
          break;
        }
      }
      if (toDetach != null) {
        measurementRecords.removeIf(record -> record.containsAnchor(anchorId));
      }
    }
    if (toDetach != null) {
      toDetach.getAnchor().detach();
    }
  }

  private void resetArSession() {
    runOnUiThread(
        () -> {
          Session previousSession = session;
          if (previousSession == null) {
            return;
          }
          try {
            session = null;
            previousSession.pause();
            previousSession.close();

            Session newSession = new Session(/* context= */ this);
            session = newSession;
            configureSession();
            session.resume();
            hasSetTextureNames = false;
          } catch (UnavailableArcoreNotInstalledException
              | UnavailableApkTooOldException
              | UnavailableSdkTooOldException
              | UnavailableDeviceNotCompatibleException e) {
            Log.e(TAG, "Failed to reset AR session", e);
            messageSnackbarHelper.showError(this, "Unable to reset AR session: " + e.getMessage());
          } catch (CameraNotAvailableException e) {
            Log.e(TAG, "Camera not available during reset", e);
            messageSnackbarHelper.showError(this, "Camera not available. Try restarting the app.");
          } catch (Exception e) {
            Log.e(TAG, "Unexpected error resetting AR session", e);
            messageSnackbarHelper.showError(this, "Failed to reset AR session");
          }
        });
  }

  private static class MeasurementRecord {
    final int id;
    final int startAnchorId;
    final int endAnchorId;

    MeasurementRecord(int id, int startAnchorId, int endAnchorId) {
      this.id = id;
      this.startAnchorId = startAnchorId;
      this.endAnchorId = endAnchorId;
    }

    boolean matches(int firstAnchorId, int secondAnchorId) {
      return (startAnchorId == firstAnchorId && endAnchorId == secondAnchorId)
          || (startAnchorId == secondAnchorId && endAnchorId == firstAnchorId);
    }

    boolean containsAnchor(int anchorId) {
      return startAnchorId == anchorId || endAnchorId == anchorId;
    }
  }
}

/**
 * Associates an Anchor with the trackable it was attached to. This is used to be able to check
 * whether or not an Anchor originally was attached to an {@link InstantPlacementPoint}.
 */
class WrappedAnchor {
  private final int id;
  private final Anchor anchor;
  private final Trackable trackable;

  public WrappedAnchor(int id, Anchor anchor, Trackable trackable) {
    this.id = id;
    this.anchor = anchor;
    this.trackable = trackable;
  }

  public int getId() {
    return id;
  }

  public Anchor getAnchor() {
    return anchor;
  }

  public Trackable getTrackable() {
    return trackable;
  }
}
