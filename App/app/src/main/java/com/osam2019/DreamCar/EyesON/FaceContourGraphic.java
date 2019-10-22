package com.osam2019.DreamCar.EyesON;

import android.annotation.SuppressLint;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Pair;


import com.google.firebase.ml.vision.common.FirebaseVisionPoint;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceContour;
import com.osam2019.DreamCar.EyesON.google.GraphicOverlay;
import com.osam2019.DreamCar.EyesON.google.GraphicOverlay.Graphic;

import java.util.ArrayList;
import java.util.List;


public class FaceContourGraphic extends Graphic {

  private static final float FACE_POSITION_RADIUS = 3.0f;
  private static final float ID_TEXT_SIZE = 30.0f;
  private static final float ID_Y_OFFSET = 80.0f;
  private static final float ID_X_OFFSET = -70.0f;
  private static final float BOX_STROKE_WIDTH = 5.0f;

  private final Paint facePositionPaint;
  private final Paint idPaint;
  private final Paint boxPaint;

  private volatile FirebaseVisionFace firebaseVisionFace;
  public static float LeftEyeOpenProbability = (float) 0.0;
  public static float RightEyeOpenProbability = (float) 0.0;

  public class ContourVar{
    public FirebaseVisionFaceContour vContour;
    public int color;
    public float lineWidth;
    public float circleRadius;
    ContourVar(){}
    ContourVar(FirebaseVisionFaceContour vContour, int color, float lineWidth, float circleRadius){
      this.vContour=vContour;
      this.color = color;
      this.lineWidth = lineWidth;
      this.circleRadius = circleRadius;
    }
  }

  private static List<ContourVar> contourList;

  public FaceContourGraphic(GraphicOverlay overlay, FirebaseVisionFace face) {
    super(overlay);

    this.firebaseVisionFace = face;
    final int selectedColor = Color.WHITE;

    facePositionPaint = new Paint();
    facePositionPaint.setColor(selectedColor);

    idPaint = new Paint();
    idPaint.setColor(Color.WHITE);
    idPaint.setTextSize(ID_TEXT_SIZE);


    boxPaint = new Paint();
    boxPaint.setColor(Color.RED);
    boxPaint.setStyle(Paint.Style.STROKE);
    boxPaint.setStrokeWidth(BOX_STROKE_WIDTH);

    contourList = new ArrayList<>();

    contourList.add(new ContourVar(face.getContour(FirebaseVisionFaceContour.LEFT_EYE), Color.WHITE, 2.0f, 2.0f));
    contourList.add(new ContourVar(face.getContour(FirebaseVisionFaceContour.RIGHT_EYE), Color.WHITE, 2.0f, 2.0f));

    contourList.add(new ContourVar(face.getContour(FirebaseVisionFaceContour.LEFT_EYEBROW_BOTTOM), Color.BLACK, 2.0f, 4.0f));
    contourList.add(new ContourVar(face.getContour(FirebaseVisionFaceContour.RIGHT_EYEBROW_BOTTOM), Color.BLACK, 2.0f, 4.0f));

    contourList.add(new ContourVar(face.getContour(FirebaseVisionFaceContour.FACE), Color.BLUE, 2.0f, 4.0f));


  }
  public void drawCanvas(Canvas canvas, List<ContourVar> contourList){
    for(ContourVar contour : contourList) {
      for (int i = 1; i < contour.vContour.getPoints().size(); i++) {
        FirebaseVisionPoint point = contour.vContour.getPoints().get(i);
        FirebaseVisionPoint prevPoint = contour.vContour.getPoints().get(i - 1);
        float px = translateX(point.getX());
        float py = translateY(point.getY());
        facePositionPaint.setColor(contour.color);
        canvas.drawCircle(px, py, contour.circleRadius, facePositionPaint);
        Paint paint = new Paint();
        paint.setColor(contour.color);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(contour.lineWidth);
        canvas.drawLine(px, py, translateX(prevPoint.getX()), translateY(prevPoint.getY()), paint);
      }
    }
  }

  @SuppressLint("DefaultLocale")
  @Override
  public void draw(Canvas canvas) {
    FirebaseVisionFace face = firebaseVisionFace;
    if (face == null) {
      return;
    }

    float x = translateX(face.getBoundingBox().centerX());
    float y = translateY(face.getBoundingBox().centerY());
    float xOffset = scaleX(face.getBoundingBox().width() / 2.0f);
    float yOffset = scaleY(face.getBoundingBox().height() / 2.0f);
    float left = x - xOffset;
    float top = y - yOffset;
    float right = x + xOffset;
    float bottom = y + yOffset;
    canvas.drawRect(left, top, right, bottom, boxPaint);


    drawCanvas(canvas, contourList);


    LeftEyeOpenProbability = face.getLeftEyeOpenProbability();
    RightEyeOpenProbability = face.getRightEyeOpenProbability();
    LeftEyeOpenProbability = LeftEyeOpenProbability < 0 ? -2 : LeftEyeOpenProbability;
    RightEyeOpenProbability = RightEyeOpenProbability < 0 ? -2 : RightEyeOpenProbability;

    if (LeftEyeOpenProbability >= 0) {
      canvas.drawText(
              "left eye: " + String.format("%.2f", LeftEyeOpenProbability),
              x + ID_X_OFFSET * 6,
              y,
              idPaint);
    }
    if (RightEyeOpenProbability >= 0) {
      canvas.drawText(
              "right eye: " + String.format("%.2f", RightEyeOpenProbability),
              x - ID_X_OFFSET,
              y,
              idPaint);
    }

  }
}