package com.osam2019.DreamCar.EyesON;

import android.annotation.SuppressLint;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Pair;


import com.google.firebase.ml.vision.common.FirebaseVisionPoint;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceContour;
import com.google.firebase.perf.metrics.AddTrace;
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
  private Paint boxPaint;

  private volatile FirebaseVisionFace firebaseVisionFace;
  public static float LeftEyeOpenProbability = (float) 0.0;
  public static float RightEyeOpenProbability = (float) 0.0;
  public static float SmileProbability = (float) 0.0;
  public static int drowsinessTime = 0;
  public static long prevTime=0, nowTime=0;
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
  @AddTrace(name = "onDrawTrace", enabled = true)
  public void draw(Canvas canvas) {
    prevTime = nowTime;
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

    boxPaint = new Paint();
    if(SmileProbability >=0.6)
      boxPaint.setColor(Color.BLUE);
    else if(drowsinessTime >= 600)
      boxPaint.setColor(Color.RED);
    else
      boxPaint.setColor(Color.WHITE);
    boxPaint.setStyle(Paint.Style.STROKE);
    boxPaint.setStrokeWidth(BOX_STROKE_WIDTH);

    canvas.drawRect(left, top, right, bottom, boxPaint);


    drawCanvas(canvas, contourList);


    LeftEyeOpenProbability = face.getLeftEyeOpenProbability();
    RightEyeOpenProbability = face.getRightEyeOpenProbability();
    SmileProbability = face.getSmilingProbability();
    LeftEyeOpenProbability = LeftEyeOpenProbability < 0 ? -2 : LeftEyeOpenProbability;
    RightEyeOpenProbability = RightEyeOpenProbability < 0 ? -2 : RightEyeOpenProbability;

    if (SmileProbability >= 0) {
      canvas.drawText(
              "happiness: " + String.format("%.2f", SmileProbability),
              x + ID_X_OFFSET * 3,
              y - ID_Y_OFFSET,
              idPaint);
    }

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
    nowTime = System.currentTimeMillis();
    if(LeftEyeOpenProbability >= 0.5 || RightEyeOpenProbability >= 0.5)
      drowsinessTime = 0;
    else
      drowsinessTime += nowTime-prevTime;
  }
}