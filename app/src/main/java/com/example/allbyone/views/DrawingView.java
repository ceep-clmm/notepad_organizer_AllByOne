package com.example.allbyone.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;

import com.example.allbyone.models.Stroke;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class DrawingView extends View {

    private static class StrokeData {
        Path path;
        List<PointF> points;
        Paint paint;

        StrokeData(Path path, List<PointF> points, Paint paint) {
            this.path = path;
            this.points = points;
            this.paint = paint;
        }
    }

    private final List<StrokeData> strokeDataList = new ArrayList<>();
    private Path currentPath;
    private List<PointF> currentStroke;
    private Paint currentPaint;

    private float strokeWidth = 6f;
    private int strokeAlpha = 255;
    private int strokeColor = 0xFF000000;

    private float scaleFactor = 1.0f;
    private float translateX = 0f;
    private float translateY = 0f;

    private float lastTouchX = 0f;
    private float lastTouchY = 0f;

    private final ScaleGestureDetector scaleDetector;

    private boolean isScaling = false;
    private boolean isPanning = false;

    private final Stack<List<StrokeData>> undoStack = new Stack<>();
    private final Stack<List<StrokeData>> redoStack = new Stack<>();

    public DrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        initPaint();
    }

    private void initPaint() {
        currentPaint = new Paint();
        currentPaint.setAntiAlias(true);
        currentPaint.setColor(strokeColor);
        currentPaint.setAlpha(strokeAlpha);
        currentPaint.setStyle(Paint.Style.STROKE);
        currentPaint.setStrokeJoin(Paint.Join.ROUND);
        currentPaint.setStrokeCap(Paint.Cap.ROUND);
        currentPaint.setStrokeWidth(strokeWidth);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.save();
        canvas.translate(translateX, translateY);
        canvas.scale(scaleFactor, scaleFactor);
        canvas.drawColor(Color.WHITE);

        for (StrokeData stroke : strokeDataList) {
            canvas.drawPath(stroke.path, stroke.paint);
        }

        if (currentPath != null && currentPaint != null) {
            canvas.drawPath(currentPath, currentPaint);
        }

        canvas.restore();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);
        int pointerCount = event.getPointerCount();

        if (pointerCount == 2) {
            isPanning = true;
            float x = (event.getX(0) + event.getX(1)) / 2f;
            float y = (event.getY(0) + event.getY(1)) / 2f;

            if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
                float dx = x - lastTouchX;
                float dy = y - lastTouchY;
                translateX += dx;
                translateY += dy;
                invalidate();
            }

            lastTouchX = x;
            lastTouchY = y;
            return true;
        }

        if (scaleDetector.isInProgress()) {
            isScaling = true;
            return true;
        }

        if (isPanning || isScaling) {
            if (event.getActionMasked() == MotionEvent.ACTION_UP || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                isPanning = false;
                isScaling = false;
            }
            return true;
        }

        float x = (event.getX() - translateX) / scaleFactor;
        float y = (event.getY() - translateY) / scaleFactor;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                currentPath = new Path();
                currentPath.moveTo(x, y);
                currentStroke = new ArrayList<>();
                currentStroke.add(new PointF(x, y));

                Paint newPaint = new Paint(currentPaint);
                currentPaint = newPaint;
                break;

            case MotionEvent.ACTION_MOVE:
                if (currentStroke != null) {
                    PointF last = currentStroke.get(currentStroke.size() - 1);
                    currentPath.quadTo(last.x, last.y, (last.x + x) / 2, (last.y + y) / 2);
                    currentStroke.add(new PointF(x, y));
                }
                break;

            case MotionEvent.ACTION_UP:
                if (currentStroke != null && currentStroke.size() > 1) {
                    saveToUndoStack();
                    strokeDataList.add(new StrokeData(currentPath, new ArrayList<>(currentStroke), currentPaint));
                }
                currentPath = null;
                currentStroke = null;
                break;
        }

        invalidate();
        return true;
    }

    public List<PointF> getPoints() {
        List<PointF> allPoints = new ArrayList<>();
        for (StrokeData data : strokeDataList) {
            if (!allPoints.isEmpty()) {
                allPoints.add(null);
            }
            allPoints.addAll(data.points);
        }
        return allPoints;
    }

    public void undo() {
        if (!undoStack.isEmpty()) {
            redoStack.push(cloneStrokes(strokeDataList));
            strokeDataList.clear();
            strokeDataList.addAll(undoStack.pop());
            invalidate();
        }
    }

    public void redo() {
        if (!redoStack.isEmpty()) {
            undoStack.push(cloneStrokes(strokeDataList));
            strokeDataList.clear();
            strokeDataList.addAll(redoStack.pop());
            invalidate();
        }
    }

    private void saveToUndoStack() {
        undoStack.push(cloneStrokes(strokeDataList));
        redoStack.clear();
    }

    private List<StrokeData> cloneStrokes(List<StrokeData> original) {
        List<StrokeData> clone = new ArrayList<>();
        for (StrokeData data : original) {
            Paint paintCopy = new Paint(data.paint);
            List<PointF> pointsCopy = new ArrayList<>(data.points);
            Path pathCopy = new Path();
            if (!pointsCopy.isEmpty()) {
                PointF first = pointsCopy.get(0);
                pathCopy.moveTo(first.x, first.y);
                for (int i = 1; i < pointsCopy.size(); i++) {
                    PointF last = pointsCopy.get(i - 1);
                    PointF point = pointsCopy.get(i);
                    pathCopy.quadTo(last.x, last.y, (last.x + point.x) / 2, (last.y + point.y) / 2);
                }
            }
            clone.add(new StrokeData(pathCopy, pointsCopy, paintCopy));
        }
        return clone;
    }

    public void setStrokeWidth(float width) {
        this.strokeWidth = width;
        currentPaint.setStrokeWidth(width);
        invalidate();
    }

    public float getStrokeWidth() {
        return strokeWidth;
    }

    public void setAlphaValue(int alpha) {
        this.strokeAlpha = alpha;
        currentPaint.setAlpha(alpha);
        invalidate();
    }

    public int getAlphaValue() {
        return strokeAlpha;
    }

    public void setColor(int color) {
        this.strokeColor = color;
        currentPaint.setColor(color);
        invalidate();
    }

    public int getColor() {
        return strokeColor;
    }

    public void setStrokeColor(int color) {
        this.strokeColor = color;
        if (currentPaint != null) {
            currentPaint.setColor(color);
        }
        invalidate();
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            scaleFactor *= detector.getScaleFactor();
            scaleFactor = Math.max(0.5f, Math.min(scaleFactor, 5.0f));
            invalidate();
            return true;
        }
    }

    public Bitmap exportToBitmap() {
        Bitmap bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        draw(canvas);
        return bitmap;
    }

    public List<Stroke> getStrokes() {
        List<Stroke> strokes = new ArrayList<>();
        for (StrokeData data : strokeDataList) {
            strokes.add(new Stroke(new ArrayList<>(data.points), new Paint(data.paint)));
        }
        return strokes;
    }

    public void setStrokes(List<Stroke> strokes) {
        strokeDataList.clear();
        for (Stroke stroke : strokes) {
            Path path = new Path();
            List<PointF> points = stroke.points;
            if (points == null || points.size() < 2) continue;
            path.moveTo(points.get(0).x, points.get(0).y);
            for (int i = 1; i < points.size(); i++) {
                PointF last = points.get(i - 1);
                PointF point = points.get(i);
                path.quadTo(last.x, last.y, (last.x + point.x) / 2, (last.y + point.y) / 2);
            }
            strokeDataList.add(new StrokeData(path, points, new Paint(stroke.paint)));
        }
        invalidate();
    }

}