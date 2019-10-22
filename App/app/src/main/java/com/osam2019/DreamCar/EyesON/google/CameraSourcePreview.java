// Copyright 2018 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.osam2019.DreamCar.EyesON.google;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;

import com.google.android.gms.common.images.Size;

import java.io.IOException;

/** Preview the camera image in the screen. */
public class CameraSourcePreview extends ViewGroup {
  private static final String TAG = "MIDemoApp:Preview";

  private Context context;
  private SurfaceView surfaceView;
  private boolean startRequested;
  private boolean surfaceAvailable;
  private CameraSource cameraSource;

  private GraphicOverlay overlay;

  public CameraSourcePreview(Context context, AttributeSet attrs) {
    super(context, attrs);
    this.context = context;
    startRequested = false;
    surfaceAvailable = false;

    surfaceView = new SurfaceView(context);
    surfaceView.getHolder().addCallback(new SurfaceCallback());
    addView(surfaceView);
  }

  public void start(CameraSource cameraSource) throws IOException {
    if (cameraSource == null) {
      stop();
    }

    this.cameraSource = cameraSource;

    if (this.cameraSource != null) {
      startRequested = true;
      startIfReady();
    }
  }

  public void start(CameraSource cameraSource, GraphicOverlay overlay) throws IOException {
    this.overlay = overlay;
    start(cameraSource);
  }

  public void stop() {
    if (cameraSource != null) {
      cameraSource.stop();
    }
  }

  public void release() {
    if (cameraSource != null) {
      cameraSource.release();
      cameraSource = null;
    }
  }

  @SuppressLint("MissingPermission")
  private void startIfReady() throws IOException {
    if (startRequested && surfaceAvailable) {
      cameraSource.start();
      if (overlay != null) {
        Size size = cameraSource.getPreviewSize();
        int min = Math.min(size.getWidth(), size.getHeight());
        int max = Math.max(size.getWidth(), size.getHeight());
        if (isPortraitMode()) {
          // Swap width and height sizes when in portrait, since it will be rotated by
          // 90 degrees
          overlay.setCameraInfo(min, max, cameraSource.getCameraFacing());
        } else {
          overlay.setCameraInfo(max, min, cameraSource.getCameraFacing());
        }
        overlay.clear();
      }
      startRequested = false;
    }
  }

  private class SurfaceCallback implements SurfaceHolder.Callback {
    @Override
    public void surfaceCreated(SurfaceHolder surface) {
      surfaceAvailable = true;
      try {
        startIfReady();
      } catch (IOException e) {
        Log.e(TAG, "Could not start camera source.", e);
      }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surface) {
      surfaceAvailable = false;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}
  }

  @Override
  protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    int previewWidth = 320;
    int previewHeight = 240;
    if (cameraSource != null) {
      Size size = cameraSource.getPreviewSize();
      if (size != null) {
        previewWidth = size.getWidth();
        previewHeight = size.getHeight();
      }
    }

    if (isPortraitMode()) {
      int tmp = previewWidth;
      previewWidth = previewHeight;
      previewHeight = tmp;
    }

    final int viewWidth = right - left;
    final int viewHeight = bottom - top;

    int childWidth;
    int childHeight;
    int childXOffset = 0;
    int childYOffset = 0;
    float widthRatio = (float) viewWidth / (float) previewWidth;
    float heightRatio = (float) viewHeight / (float) previewHeight;

    if (widthRatio > heightRatio) {
      childWidth = viewWidth;
      childHeight = (int) ((float) previewHeight * widthRatio);
      childYOffset = (childHeight - viewHeight) / 2;
    } else {
      childWidth = (int) ((float) previewWidth * heightRatio);
      childHeight = viewHeight;
      childXOffset = (childWidth - viewWidth) / 2;
    }

    for (int i = 0; i < getChildCount(); ++i) {
      getChildAt(i).layout(
              -1 * childXOffset, -1 * childYOffset,
              childWidth - childXOffset, childHeight - childYOffset);
    }

    try {
      startIfReady();
    } catch (IOException e) {
      Log.e(TAG, "Could not start camera source.", e);
    }
  }

  private boolean isPortraitMode() {
    int orientation = context.getResources().getConfiguration().orientation;
    if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
      return false;
    }
    if (orientation == Configuration.ORIENTATION_PORTRAIT) {
      return true;
    }

    Log.d(TAG, "isPortraitMode returning false by default");
    return false;
  }
}
