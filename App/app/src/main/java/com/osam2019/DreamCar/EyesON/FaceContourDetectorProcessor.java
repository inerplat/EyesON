package com.osam2019.DreamCar.EyesON;

import android.graphics.Bitmap;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;
import com.osam2019.DreamCar.EyesON.google.CameraImageGraphic;
import com.osam2019.DreamCar.EyesON.google.FrameMetadata;
import com.osam2019.DreamCar.EyesON.google.GraphicOverlay;
import com.osam2019.DreamCar.EyesON.google.VisionProcessorBase;
import com.osam2019.DreamCar.EyesON.FaceContourGraphic;
import java.io.IOException;
import java.util.List;

/**
 * Face Contour Demo.
 */



public class FaceContourDetectorProcessor extends VisionProcessorBase<List<FirebaseVisionFace>> {

    private static final String TAG = "FaceContourDetectorProc";

    private final FirebaseVisionFaceDetector detector;
    public static int status = 0;
    public static float LeftEyeOpenProbability = (float) 0.0;
    public static float RightEyeOpenProbability = (float) 0.0;
    public static float SmileProbability = (float) 0.0;
    public FaceContourDetectorProcessor(String mode) {
        FirebaseVisionFaceDetectorOptions options = null;
        if(mode.equals("Contour")) {
            options = new FirebaseVisionFaceDetectorOptions.Builder()
                    .setPerformanceMode(FirebaseVisionFaceDetectorOptions.FAST)
                    .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
                    .setContourMode(FirebaseVisionFaceDetectorOptions.ALL_CONTOURS)
                    .build();
            status = 1;
        }
        else if(mode.equals("Detect")){
            options = new FirebaseVisionFaceDetectorOptions.Builder()
                    .setPerformanceMode(FirebaseVisionFaceDetectorOptions.FAST)
                    .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
                    .build();
            status = 0;
        }
        detector = FirebaseVision.getInstance().getVisionFaceDetector(options);
    }
    public static int getStatus(){
        return status;
    }
    @Override
    public void stop() {
        try {
            detector.close();
        } catch (IOException e) {
            Log.e(TAG, "Exception thrown while trying to close Face Contour Detector: " + e);
        }
    }

    @Override
    protected Task<List<FirebaseVisionFace>> detectInImage(FirebaseVisionImage image) {
        return detector.detectInImage(image);
    }

    @Override
    protected void onSuccess(
            @Nullable Bitmap originalCameraImage,
            @NonNull List<FirebaseVisionFace> faces,
            @NonNull FrameMetadata frameMetadata,
            @NonNull GraphicOverlay graphicOverlay) {
        graphicOverlay.clear();
        if (originalCameraImage != null) {
            CameraImageGraphic imageGraphic = new CameraImageGraphic(graphicOverlay, originalCameraImage);
            graphicOverlay.add(imageGraphic);
        }
        for (int i = 0; i < faces.size(); ++i) {
            FirebaseVisionFace face = faces.get(i);
            LeftEyeOpenProbability = face.getLeftEyeOpenProbability();
            RightEyeOpenProbability = face.getRightEyeOpenProbability();
            LeftEyeOpenProbability = LeftEyeOpenProbability < 0 ? -2 : LeftEyeOpenProbability;
            RightEyeOpenProbability = RightEyeOpenProbability < 0 ? -2 : RightEyeOpenProbability;

            FaceContourGraphic faceGraphic = new FaceContourGraphic(graphicOverlay, face);
            graphicOverlay.add(faceGraphic);
            break;
        }
        graphicOverlay.postInvalidate();
    }

    @Override
    protected void onFailure(@NonNull Exception e) {
        Log.e(TAG, "Face detection failed " + e);
    }
}
