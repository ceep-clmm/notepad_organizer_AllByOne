package com.example.allbyone.utils;

import android.graphics.Paint;
import android.graphics.PointF;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import com.example.allbyone.models.Stroke;

public class StrokeSerializer {

    public static JSONArray serialize(List<Stroke> strokes) {
        JSONArray jsonArray = new JSONArray();

        try {
            for (Stroke stroke : strokes) {
                JSONObject strokeObj = new JSONObject();

                JSONArray pointArray = new JSONArray();
                for (PointF point : stroke.points) {
                    JSONObject pointObj = new JSONObject();
                    pointObj.put("x", point.x);
                    pointObj.put("y", point.y);
                    pointArray.put(pointObj);
                }

                JSONObject paintObj = new JSONObject();
                paintObj.put("color", stroke.paint.getColor());
                paintObj.put("alpha", stroke.paint.getAlpha());
                paintObj.put("strokeWidth", stroke.paint.getStrokeWidth());

                strokeObj.put("points", pointArray);
                strokeObj.put("paint", paintObj);

                jsonArray.put(strokeObj);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return jsonArray;
    }

    public static List<Stroke> deserialize(String jsonString) {
        List<Stroke> strokes = new ArrayList<>();

        try {
            JSONArray jsonArray = new JSONArray(jsonString);

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject strokeObj = jsonArray.getJSONObject(i);

                List<PointF> points = new ArrayList<>();
                JSONArray pointArray = strokeObj.getJSONArray("points");
                for (int j = 0; j < pointArray.length(); j++) {
                    JSONObject pointObj = pointArray.getJSONObject(j);
                    float x = (float) pointObj.getDouble("x");
                    float y = (float) pointObj.getDouble("y");
                    points.add(new PointF(x, y));
                }

                JSONObject paintObj = strokeObj.getJSONObject("paint");
                Paint paint = new Paint();
                paint.setAntiAlias(true);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeJoin(Paint.Join.ROUND);
                paint.setStrokeCap(Paint.Cap.ROUND);
                paint.setColor(paintObj.getInt("color"));
                paint.setAlpha(paintObj.getInt("alpha"));
                paint.setStrokeWidth((float) paintObj.getDouble("strokeWidth"));

                strokes.add(new Stroke(points, paint));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return strokes;
    }
}
