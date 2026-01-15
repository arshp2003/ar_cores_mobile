/*
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.ar.core.examples.java.common.samplerender;

import android.content.res.AssetManager;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.opengl.GLException;
import android.util.Log;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/** A SampleRender context. */
public class SampleRender {
  private static final String TAG = SampleRender.class.getSimpleName();

  private final AssetManager assetManager;

  private int viewportWidth = 1;
  private int viewportHeight = 1;

  /**
   * Constructs a SampleRender object and instantiates GLSurfaceView parameters.
   *
   * @param glSurfaceView Android GLSurfaceView
   * @param renderer Renderer implementation to receive callbacks
   * @param assetManager AssetManager for loading Android resources
   */
  public SampleRender(GLSurfaceView glSurfaceView, Renderer renderer, AssetManager assetManager) {
    this.assetManager = assetManager;
    glSurfaceView.setPreserveEGLContextOnPause(true);
    glSurfaceView.setEGLContextClientVersion(3);
    glSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
    glSurfaceView.setRenderer(
        new GLSurfaceView.Renderer() {
          @Override
          public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            GLES30.glEnable(GLES30.GL_BLEND);
            GLError.maybeThrowGLException("Failed to enable blending", "glEnable");
            renderer.onSurfaceCreated(SampleRender.this);
          }

          @Override
          public void onSurfaceChanged(GL10 gl, int w, int h) {
            viewportWidth = w;
            viewportHeight = h;
            renderer.onSurfaceChanged(SampleRender.this, w, h);
          }

          @Override
          public void onDrawFrame(GL10 gl) {
            clear(/*framebuffer=*/ null, 0f, 0f, 0f, 1f);
            renderer.onDrawFrame(SampleRender.this);
          }
        });
    glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    glSurfaceView.setWillNotDraw(false);
  }

  /** Draw a {@link Mesh} with the specified {@link Shader}. */
  public void draw(Mesh mesh, Shader shader) {
    draw(mesh, shader, /*framebuffer=*/ null);
  }

  /**
   * Draw a {@link Mesh} with the specified {@link Shader} to the given {@link Framebuffer}.
   *
   * <p>The {@code framebuffer} argument may be null, in which case the default framebuffer is used.
   */
  public void draw(Mesh mesh, Shader shader, Framebuffer framebuffer) {
    useFramebuffer(framebuffer);
    shader.lowLevelUse();
    mesh.lowLevelDraw();
  }

  /**
   * Clear the given framebuffer.
   *
   * <p>The {@code framebuffer} argument may be null, in which case the default framebuffer is
   * cleared.
   */
  public void clear(Framebuffer framebuffer, float r, float g, float b, float a) {
    useFramebuffer(framebuffer);
    GLES30.glClearColor(r, g, b, a);
    GLError.maybeThrowGLException("Failed to set clear color", "glClearColor");
    boolean hasDepthBuffer;
    if (framebuffer == null) {
      int[] depthBits = new int[1];
      GLES30.glGetIntegerv(GLES30.GL_DEPTH_BITS, depthBits, 0);
      hasDepthBuffer = depthBits[0] > 0;
    } else {
      int[] depthAttachmentType = new int[1];
      GLES30.glGetFramebufferAttachmentParameteriv(
          GLES30.GL_FRAMEBUFFER,
          GLES30.GL_DEPTH_ATTACHMENT,
          GLES30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE,
          depthAttachmentType,
          0);
      hasDepthBuffer = depthAttachmentType[0] != GLES30.GL_NONE;
    }
    int clearMask = GLES30.GL_COLOR_BUFFER_BIT;
    if (hasDepthBuffer) {
      GLES30.glDepthMask(true);
      GLError.maybeThrowGLException("Failed to set depth write mask", "glDepthMask");
      clearMask |= GLES30.GL_DEPTH_BUFFER_BIT;
    }

    GLES30.glClear(clearMask);
    int clearError = GLES30.glGetError();
    if (clearError != GLES30.GL_NO_ERROR) {
      // Retry without depth bit for recorder surfaces that misreport depth support.
      GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT);
      int fallbackError = GLES30.glGetError();
      if (fallbackError != GLES30.GL_NO_ERROR) {
        Log.w(TAG, "glClear failed on recording surface, continuing without clear. error=" + fallbackError);
      }
    }
  }

  /**
   * Executes the provided {@link Runnable} while treating the default framebuffer as having the
   * supplied dimensions.
   */
  public void withDefaultFramebufferDimensions(int width, int height, Runnable runnable) {
    int previousWidth = viewportWidth;
    int previousHeight = viewportHeight;
    viewportWidth = width;
    viewportHeight = height;
    try {
      runnable.run();
    } finally {
      viewportWidth = previousWidth;
      viewportHeight = previousHeight;
      GLES30.glViewport(0, 0, previousWidth, previousHeight);
      GLError.maybeThrowGLException("Failed to restore viewport dimensions", "glViewport");
    }
  }

  /** Interface to be implemented for rendering callbacks. */
  public static interface Renderer {
    /**
     * Called by {@link SampleRender} when the GL render surface is created.
     *
     * <p>See {@link GLSurfaceView.Renderer#onSurfaceCreated}.
     */
    public void onSurfaceCreated(SampleRender render);

    /**
     * Called by {@link SampleRender} when the GL render surface dimensions are changed.
     *
     * <p>See {@link GLSurfaceView.Renderer#onSurfaceChanged}.
     */
    public void onSurfaceChanged(SampleRender render, int width, int height);

    /**
     * Called by {@link SampleRender} when a GL frame is to be rendered.
     *
     * <p>See {@link GLSurfaceView.Renderer#onDrawFrame}.
     */
    public void onDrawFrame(SampleRender render);
  }

  /* package-private */
  AssetManager getAssets() {
    return assetManager;
  }

  private void useFramebuffer(Framebuffer framebuffer) {
    int framebufferId;
    int viewportWidth;
    int viewportHeight;
    if (framebuffer == null) {
      framebufferId = 0;
      viewportWidth = this.viewportWidth;
      viewportHeight = this.viewportHeight;
    } else {
      framebufferId = framebuffer.getFramebufferId();
      viewportWidth = framebuffer.getWidth();
      viewportHeight = framebuffer.getHeight();
    }
    GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, framebufferId);
    GLError.maybeThrowGLException("Failed to bind framebuffer", "glBindFramebuffer");
    GLES30.glViewport(0, 0, viewportWidth, viewportHeight);
    GLError.maybeThrowGLException("Failed to set viewport dimensions", "glViewport");
  }
}
