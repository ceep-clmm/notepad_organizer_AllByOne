package com.example.allbyone;

import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.*;
import android.text.style.*;
import android.view.*;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.example.allbyone.database.AppDatabase;
import com.example.allbyone.models.Project;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NoteActivity extends AppCompatActivity {

    private EditText noteEditText, titleEditText;
    private ImageButton boldButton, italicButton, underlineButton, highlightButton, imageButton;
    private Spinner fontSpinner;
    private boolean isBold = false, isItalic = false, isUnderlined = false;

    private Project currentProject;
    private long projectId = -1;

    private final String[] fonts = {"sans-serif", "serif", "monospace", "casual", "cursive"};
    private Layout.Alignment currentAlignment = Layout.Alignment.ALIGN_NORMAL;

    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), this::insertImageIntoNote);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note);

        titleEditText = findViewById(R.id.titleEditText);
        noteEditText = findViewById(R.id.noteEditText);
        boldButton = findViewById(R.id.boldButton);
        italicButton = findViewById(R.id.italicButton);
        underlineButton = findViewById(R.id.underlineButton);
        highlightButton = findViewById(R.id.highlightButton);
        imageButton = findViewById(R.id.insertImageButton);
        fontSpinner = findViewById(R.id.fontSpinner);
        ImageButton alignButton = findViewById(R.id.button_align);
        alignButton.setOnClickListener(v -> cycleAlignment());

        Button saveButton = findViewById(R.id.saveButton);
        ImageButton backButton = findViewById(R.id.backButton);

        ArrayAdapter<String> fontAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, fonts);
        fontAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        fontSpinner.setAdapter(fontAdapter);

        projectId = getIntent().getLongExtra("projectId", -1);
        if (projectId != -1) {
            currentProject = AppDatabase.getInstance(this).projectDao().getById((int) projectId);
            if (currentProject != null) {
                titleEditText.setText(currentProject.name);
                String html = currentProject.content;

                Spanned rawSpanned = Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY, source -> {
                    File file = new File(source);
                    if (file.exists()) {
                        Bitmap bitmap = BitmapFactory.decodeFile(source);
                        Drawable drawable = new BitmapDrawable(getResources(), bitmap);
                        drawable.setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
                        return drawable;
                    }
                    return null;
                }, null);

                SpannableStringBuilder fixedSpannable = new SpannableStringBuilder(rawSpanned);

// Обработка выравнивания через парсинг HTML вручную
                Pattern pattern = Pattern.compile("<div style=\"text-align:(left|center|right)\">(.*?)</div>", Pattern.DOTALL);
                Matcher matcher = pattern.matcher(html);

                while (matcher.find()) {
                    String align = matcher.group(1);
                    String content = matcher.group(2);

                    Layout.Alignment alignment;
                    switch (align) {
                        case "center": alignment = Layout.Alignment.ALIGN_CENTER; break;
                        case "right": alignment = Layout.Alignment.ALIGN_OPPOSITE; break;
                        default: alignment = Layout.Alignment.ALIGN_NORMAL; break;
                    }

                    int start = fixedSpannable.toString().indexOf(Html.fromHtml(content).toString());
                    if (start >= 0) {
                        int end = start + Html.fromHtml(content).length();
                        fixedSpannable.setSpan(new AlignmentSpan.Standard(alignment), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                }

                noteEditText.setText(fixedSpannable);

            }
        }

        // Обработчики событий кнопок
        boldButton.setOnClickListener(v -> {
            isBold = !isBold;
            applyStyle(Typeface.BOLD);
        });

        italicButton.setOnClickListener(v -> {
            isItalic = !isItalic;
            applyStyle(Typeface.ITALIC);
        });

        underlineButton.setOnClickListener(v -> {
            isUnderlined = !isUnderlined;
            applyUnderline();
        });

        highlightButton.setOnClickListener(v -> applyHighlight());

        imageButton.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

        fontSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                applyFont(fonts[position]);
            }

            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        saveButton.setOnClickListener(v -> {
            String title = titleEditText.getText().toString().trim();

            String formattedText = getHtmlWithAlignment(noteEditText.getText());

            if (title.isEmpty() && formattedText.trim().isEmpty()) {
                finish();
                return;
            }

            AppDatabase db = AppDatabase.getInstance(this);

            if (currentProject != null) {
                currentProject.name = title.isEmpty() ? "Без названия" : title;
                currentProject.content = formattedText;
                db.projectDao().update(currentProject);
            } else {
                Project newProject = new Project();
                newProject.name = title.isEmpty() ? "Без названия" : title;
                newProject.content = formattedText;
                newProject.type = "note";
                newProject.createdAt = System.currentTimeMillis();
                newProject.folderId = 0;
                newProject.labelId = 0;
                db.projectDao().insert(newProject);
            }

            finish();
        });

        backButton.setOnClickListener(v -> finish());
    }



    private void applyStyle(int style) {
        int start = noteEditText.getSelectionStart();
        int end = noteEditText.getSelectionEnd();
        if (start == end) return;

        Spannable spannable = noteEditText.getText();
        StyleSpan[] spans = spannable.getSpans(start, end, StyleSpan.class);
        for (StyleSpan span : spans) {
            if (span.getStyle() == style) {
                spannable.removeSpan(span);
                return;
            }
        }
        spannable.setSpan(new StyleSpan(style), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private void applyUnderline() {
        int start = noteEditText.getSelectionStart();
        int end = noteEditText.getSelectionEnd();
        if (start == end) return;

        Spannable spannable = noteEditText.getText();
        UnderlineSpan[] spans = spannable.getSpans(start, end, UnderlineSpan.class);
        for (UnderlineSpan span : spans) {
            spannable.removeSpan(span);
            return;
        }
        spannable.setSpan(new UnderlineSpan(), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private void applyHighlight() {
        int start = noteEditText.getSelectionStart();
        int end = noteEditText.getSelectionEnd();
        if (start == end) return;

        Spannable spannable = noteEditText.getText();
        boolean isDarkTheme = (getResources().getConfiguration().uiMode &
                android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        int highlightColor = isDarkTheme ?
                ContextCompat.getColor(this, R.color.highlight_dark) :
                ContextCompat.getColor(this, R.color.highlight_light);

        BackgroundColorSpan[] spans = spannable.getSpans(start, end, BackgroundColorSpan.class);
        for (BackgroundColorSpan span : spans) {
            spannable.removeSpan(span);
            return;
        }

        spannable.setSpan(new BackgroundColorSpan(highlightColor), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private void applyFont(String font) {
        int start = noteEditText.getSelectionStart();
        int end = noteEditText.getSelectionEnd();
        if (start == end) return;

        Spannable spannable = noteEditText.getText();
        TypefaceSpan[] spans = spannable.getSpans(start, end, TypefaceSpan.class);
        for (TypefaceSpan span : spans) {
            if (span.getFamily().equals(font)) {
                spannable.removeSpan(span);
                return;
            }
        }
        spannable.setSpan(new TypefaceSpan(font), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private void insertImageIntoNote(Uri imageUri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);

            int newWidth = bitmap.getWidth() / 3;
            int newHeight = bitmap.getHeight() / 3;
            Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);

            File directory = new File(getFilesDir(), "images");
            if (!directory.exists()) directory.mkdirs();

            String filename = "img_" + System.currentTimeMillis() + "_scaled.png";
            File file = new File(directory, filename);
            FileOutputStream out = new FileOutputStream(file);
            scaledBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.close();

            String imagePath = file.getAbsolutePath();

            Drawable drawable = new BitmapDrawable(getResources(), scaledBitmap);
            drawable.setBounds(0, 0, newWidth, newHeight);
            ImageSpan imageSpan = new ImageSpan(drawable, imagePath);

            SpannableStringBuilder builder = new SpannableStringBuilder("<img src=\"" + imagePath + "\" />");
            builder.setSpan(imageSpan, 0, builder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            builder.setSpan(new AlignmentSpan.Standard(currentAlignment), 0, builder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            int cursorPos = noteEditText.getSelectionStart();
            noteEditText.getText().insert(cursorPos, builder);

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Ошибка при вставке изображения", Toast.LENGTH_SHORT).show();
        }
    }

    private void cycleAlignment() {
        int start = noteEditText.getSelectionStart();
        int end = noteEditText.getSelectionEnd();
        if (start == end) return;

        Spannable text = noteEditText.getText();
        AlignmentSpan.Standard[] spans = text.getSpans(start, end, AlignmentSpan.Standard.class);
        if (spans.length > 0) {
            currentAlignment = spans[0].getAlignment();
            text.removeSpan(spans[0]);
        }

        ImageButton alignButton = findViewById(R.id.button_align);

        switch (currentAlignment) {
            case ALIGN_NORMAL:
                currentAlignment = Layout.Alignment.ALIGN_CENTER;
                alignButton.setImageResource(R.drawable.ic_align_center);
                break;
            case ALIGN_CENTER:
                currentAlignment = Layout.Alignment.ALIGN_OPPOSITE;
                alignButton.setImageResource(R.drawable.ic_align_right);
                break;
            case ALIGN_OPPOSITE:
            default:
                currentAlignment = Layout.Alignment.ALIGN_NORMAL;
                alignButton.setImageResource(R.drawable.ic_align_left);
                break;
        }

        text.setSpan(new AlignmentSpan.Standard(currentAlignment), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private String getHtmlWithAlignment(Spannable text) {
        StringBuilder html = new StringBuilder();
        int i = 0;

        while (i < text.length()) {
            int next = text.nextSpanTransition(i, text.length(), AlignmentSpan.class);
            AlignmentSpan[] spans = text.getSpans(i, next, AlignmentSpan.class);
            Layout.Alignment alignment = (spans.length > 0) ? spans[0].getAlignment() : Layout.Alignment.ALIGN_NORMAL;

            String alignStr = "left";
            if (alignment == Layout.Alignment.ALIGN_CENTER) alignStr = "center";
            else if (alignment == Layout.Alignment.ALIGN_OPPOSITE) alignStr = "right";

            // Выделение фрагмента и сохранение всех стилей
            CharSequence slice = text.subSequence(i, next);
            SpannableString sliceSpan = new SpannableString(slice);

            // Генерация HTML с <br> для \n, но без <p>
            String innerHtml = Html.toHtml(sliceSpan, Html.FROM_HTML_MODE_LEGACY)
                    .replaceAll("(?i)<p[^>]*>", "")    // убираем <p>
                    .replaceAll("(?i)</p>", "");       // убираем </p>

            // Добавление блока с выравниванием и сохранением переносов
            html.append("<div style=\"text-align:")
                    .append(alignStr)
                    .append("; white-space: pre-wrap;\">") // сохраняет ручные \n
                    .append(innerHtml.trim()) // убираем лишние пробелы в начале/конце
                    .append("</div>");

            i = next;
        }

        return html.toString();
    }


}
