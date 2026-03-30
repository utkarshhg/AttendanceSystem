package com.attendance.facerecognition.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class FaceBoxOverlay extends View {

    private Paint boxPaint;
    private Paint textPaint;
    private List<FaceData> faceDataList = new ArrayList<>();
    private int imageWidth, imageHeight;

    public static class FaceData {
        public Rect bounds;
        public String id;

        public FaceData(Rect bounds, String id) {
            this.bounds = bounds;
            this.id = id;
        }
    }

    public FaceBoxOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);

        boxPaint = new Paint();
        boxPaint.setColor(Color.GREEN);
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(8f);

        textPaint = new Paint();
        textPaint.setColor(Color.GREEN);
        textPaint.setTextSize(70f);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setFakeBoldText(true);
        textPaint.setShadowLayer(5f, 2f, 2f, Color.BLACK);
    }

    public void drawFaceBoxes(List<FaceData> faces, int imgWidth, int imgHeight) {
        this.faceDataList = faces;
        this.imageWidth = imgWidth;
        this.imageHeight = imgHeight;
        invalidate();
    }

    public void clear() {
        this.faceDataList.clear();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (faceDataList == null || faceDataList.isEmpty() || imageWidth == 0 || imageHeight == 0) return;

        float scaleX = (float) getWidth() / imageHeight;
        float scaleY = (float) getHeight() / imageWidth;
        float scale = Math.max(scaleX, scaleY);

        float offsetX = (getWidth() - (imageHeight * scale)) / 2f;
        float offsetY = (getHeight() - (imageWidth * scale)) / 2f;

        for (FaceData face : faceDataList) {
            float left = face.bounds.left * scale + offsetX;
            float top = face.bounds.top * scale + offsetY;
            float right = face.bounds.right * scale + offsetX;
            float bottom = face.bounds.bottom * scale + offsetY;

            canvas.drawRect(left, top, right, bottom, boxPaint);
            canvas.drawText(face.id, left, top - 20f, textPaint);
        }
    }
}