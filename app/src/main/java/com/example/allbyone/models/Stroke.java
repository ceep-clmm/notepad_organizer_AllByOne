package com.example.allbyone.models;

import android.graphics.Paint;
import android.graphics.PointF;

import java.util.List;

public class Stroke {
    public final List<PointF> points;
    public final Paint paint;

    public Stroke(List<PointF> points, Paint paint) {
        this.points = points;
        this.paint = paint;
    }

    public List<PointF> getPoints() {
        return points;
    }

    public Paint getPaint() {
        return paint;
    }
}
