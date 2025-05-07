package com.example.allbyone;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.ScaleGestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.allbyone.database.AppDatabase;
import com.example.allbyone.models.Project;
import com.example.allbyone.models.Stroke;
import com.example.allbyone.utils.StrokeSerializer;
import com.example.allbyone.views.DrawingView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.OutputStream;
import java.util.List;

public class CanvasActivity extends AppCompatActivity {

    private DrawingView drawingView;
    private EditText titleEditText;
    private long currentProjectId = -1;
    private Project currentProject;
    private boolean isNewProject = false;
    private ScaleGestureDetector scaleGestureDetector;
    private float scaleFactor = 1.0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_canvas);

        drawingView = findViewById(R.id.drawingView);
        titleEditText = findViewById(R.id.titleEditText);
        Button saveButton = findViewById(R.id.saveButton);
        Button exportButton = findViewById(R.id.exportButton);
        ImageButton backButton = findViewById(R.id.backButton);
        ImageButton undoButton = findViewById(R.id.undoButton);
        ImageButton redoButton = findViewById(R.id.redoButton);
        ImageButton brushSettingsButton = findViewById(R.id.brushButton);
        ImageButton colorPickerButton = findViewById(R.id.colorPickerButton);

        undoButton.setOnClickListener(v -> drawingView.undo());
        redoButton.setOnClickListener(v -> drawingView.redo());

        brushSettingsButton.setOnClickListener(v -> showBrushSettingsDialog());

        colorPickerButton.setOnClickListener(v -> showColorPickerDialog());

        currentProjectId = getIntent().getLongExtra("projectId", -1);

        if (currentProjectId != -1) {
            isNewProject = false;
            loadProject(currentProjectId);
        } else {
            isNewProject = true;
        }

        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener());

        drawingView.post(() -> {
            if (isNewProject) {
                JSONObject canvasData = new JSONObject();
                try {
                    canvasData.put("points", new JSONArray());
                } catch (Exception ignored) {}

                Project newProject = new Project();
                newProject.name = "Холст от " + System.currentTimeMillis();
                newProject.type = "canvas";
                newProject.content = canvasData.toString();
                newProject.createdAt = System.currentTimeMillis();
                newProject.folderId = 0;
                newProject.labelId = 0;

                AppDatabase db = AppDatabase.getInstance(this);
                db.projectDao().insert(newProject);

                currentProjectId = newProject.id;
                currentProject = newProject;
                isNewProject = false;

                Log.d("CanvasActivity", "Новый проект создан.");
            }
        });

        saveButton.setOnClickListener(v -> {
            List<PointF> points = drawingView.getPoints();

            JSONObject canvasData = new JSONObject();
            JSONArray pointArray = new JSONArray();
            for (PointF point : points) {
                if (point == null) {
                    pointArray.put(JSONObject.NULL);
                } else {
                    JSONObject obj = new JSONObject();
                    try {
                        obj.put("x", point.x);
                        obj.put("y", point.y);
                    } catch (Exception ignored) {}
                    pointArray.put(obj);
                }
            }

            try {
                canvasData.put("points", pointArray);
            } catch (Exception ignored) {}

            AppDatabase db = AppDatabase.getInstance(this);
            String title = titleEditText.getText().toString();

            if (currentProjectId != -1) {
                Project existingProject = db.projectDao().getById((int) currentProjectId);
                if (existingProject != null) {
                    existingProject.content = canvasData.toString();
                    existingProject.name = title;
                    List<Stroke> strokeList = drawingView.getStrokes();
                    String json = StrokeSerializer.serialize(strokeList).toString();
                    existingProject.setStrokesJson(json);
                    db.projectDao().update(existingProject);
                }
            } else {
                Project newProject = new Project();
                newProject.name = title.isEmpty() ? "Холст от " + System.currentTimeMillis() : title;
                newProject.type = "canvas";
                newProject.content = canvasData.toString();
                newProject.createdAt = System.currentTimeMillis();
                newProject.folderId = 0;
                newProject.labelId = 0;
                db.projectDao().insert(newProject);
            }

            finish();
        });

        exportButton.setOnClickListener(v -> {
            Bitmap bitmap = drawingView.exportToBitmap();
            if (bitmap == null) {
                Toast.makeText(this, "Невозможно экспортировать изображение.", Toast.LENGTH_SHORT).show();
                return;
            }

            String filename = "canvas_export_" + System.currentTimeMillis() + ".png";

            OutputStream outputStream;
            try {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DISPLAY_NAME, filename);
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
                values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/AllByOne");

                ContentResolver resolver = getContentResolver();
                Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

                if (imageUri != null) {
                    outputStream = resolver.openOutputStream(imageUri);
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                    outputStream.close();

                    Toast.makeText(this, "Изображение сохранено в Галерею.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Не удалось сохранить изображение.", Toast.LENGTH_SHORT).show();
                }

            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Ошибка при сохранении: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        backButton.setOnClickListener(v -> finish());
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleGestureDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            scaleFactor *= detector.getScaleFactor();
            scaleFactor = Math.max(0.1f, Math.min(scaleFactor, 5.0f));
            drawingView.setScaleX(scaleFactor);
            drawingView.setScaleY(scaleFactor);
            return true;
        }
    }

    private void loadProject(long projectId) {
        Log.d("CanvasActivity", "Loading project with ID: " + projectId);

        currentProject = AppDatabase.getInstance(this).projectDao().getById((int) projectId);

        if (currentProject != null && "canvas".equals(currentProject.type)) {
            titleEditText.setText(currentProject.name);

            try {
                String strokesJson = currentProject.getStrokesJson();
                if (strokesJson != null && !strokesJson.isEmpty()) {
                    List<Stroke> strokes = StrokeSerializer.deserialize(strokesJson);
                    drawingView.setStrokes(strokes);
                }

            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Ошибка загрузки проекта.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Проект не найден.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showBrushSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Настройки кисти");

        View view = getLayoutInflater().inflate(R.layout.dialog_brush_settings, null);
        SeekBar thicknessSeekBar = view.findViewById(R.id.seekBarThickness);
        SeekBar alphaSeekBar = view.findViewById(R.id.seekBarAlpha);

        thicknessSeekBar.setProgress((int) drawingView.getStrokeWidth());
        alphaSeekBar.setProgress(drawingView.getAlphaValue());

        builder.setView(view);

        builder.setPositiveButton("OK", (dialog, which) -> {
            float thickness = thicknessSeekBar.getProgress();
            int alpha = alphaSeekBar.getProgress();
            drawingView.setStrokeWidth(thickness);
            drawingView.setAlphaValue(alpha);
        });

        builder.setNegativeButton("Отмена", null);
        builder.show();
    }

    private void showColorPickerDialog() {
        final String[] colorNames = {"Красный", "Фиолетовый", "Синий", "Зелёный", "Жёлтый", "Оранжевый", "Белый", "Чёрный"};
        final int[] colors = {
                Color.RED,
                Color.MAGENTA,
                Color.BLUE,
                Color.GREEN,
                Color.YELLOW,
                Color.parseColor("#FFA500"), // Оранжевый
                Color.WHITE,
                Color.BLACK
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Выберите цвет кисти");
        builder.setItems(colorNames, (dialog, which) -> drawingView.setStrokeColor(colors[which]));
        builder.setNegativeButton("Отмена", null);
        builder.show();
    }

}
