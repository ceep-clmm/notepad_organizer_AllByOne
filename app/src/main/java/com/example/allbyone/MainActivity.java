package com.example.allbyone;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.ArrayAdapter;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.allbyone.adapters.ProjectAdapter;
import com.example.allbyone.database.AppDatabase;
import com.example.allbyone.models.Project;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProjectAdapter adapter;

    @Override
    protected void onResume() {
        super.onResume();
        loadProjects();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (isDarkThemeEnabled()) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.projectRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        Button newNoteBtn = findViewById(R.id.buttonNewNote);
        newNoteBtn.setOnClickListener(v -> showNewObjectDialog());

        ImageButton buttonSettings = findViewById(R.id.buttonSettings);
        buttonSettings.setOnClickListener(v -> showThemeDialog());


        loadProjects();
    }

    private void loadProjects() {
        List<Project> projectList = AppDatabase.getInstance(this).projectDao().getAll();
        adapter = new ProjectAdapter(
                this,
                projectList,
                this::openProject,
                this::showProjectOptionsDialog // обработка долгого нажатия
        );
        recyclerView.setAdapter(adapter);
    }


    private void showNewObjectDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Создать объект");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final EditText nameInput = new EditText(this);
        nameInput.setHint("Введите имя");
        layout.addView(nameInput);

        final Spinner objectTypeSpinner = new Spinner(this);
        String[] options = {"Заметка", "Перечень", "Холст"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, options);
        objectTypeSpinner.setAdapter(adapter);
        layout.addView(objectTypeSpinner);

        builder.setView(layout);

        builder.setPositiveButton("Создать", (dialog, which) -> {
            String name = nameInput.getText().toString();
            String selected = objectTypeSpinner.getSelectedItem().toString();
            String type;
            switch (selected) {
                case "Заметка":
                    type = "note";
                    break;
                case "Перечень":
                    type = "list";
                    break;
                case "Холст":
                    type = "canvas";
                    break;
                default:
                    type = "note";
            }

            createNewObject(name, type);
        });

        builder.setNegativeButton("Отмена", null);
        builder.show();
    }


    private void createNewObject(String name, String type) {
        Project newProject = new Project();
        newProject.name = name.isEmpty() ? "Без названия" : name;
        newProject.type = type.toLowerCase();
        newProject.content = "";
        newProject.createdAt = System.currentTimeMillis();
        newProject.folderId = 0;
        newProject.labelId = 0;

        AppDatabase.getInstance(this).projectDao().insert(newProject);
        loadProjects();
    }

    // Обработчик кликов на проект
    private void openProject(Project project) {
        Intent intent;
        switch (project.type) {
            case "note":
                intent = new Intent(this, NoteActivity.class);
                break;
            case "list":
                intent = new Intent(this, ListActivity.class);
                break;
            case "canvas":
                intent = new Intent(this, CanvasActivity.class);
                break;
            default:
                return;
        }
        intent.putExtra("projectId", project.id);
        startActivity(intent);
    }

    private void showProjectOptionsDialog(Project project) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Действие с \"" + project.name + "\"");

        String[] options = {"Переименовать", "Удалить"};
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0:
                    showRenameDialog(project);
                    break;
                case 1:
                    showDeleteConfirmation(project);
                    break;
            }
        });

        builder.show();
    }

    private void showRenameDialog(Project project) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Переименовать");

        final EditText input = new EditText(this);
        input.setText(project.name);
        builder.setView(input);

        builder.setPositiveButton("Сохранить", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty()) {
                project.name = newName;
                AppDatabase.getInstance(this).projectDao().update(project);
                loadProjects();
            }
        });

        builder.setNegativeButton("Отмена", null);
        builder.show();
    }

    private void showDeleteConfirmation(Project project) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Удалить объект");
        builder.setMessage("Вы уверены, что хотите удалить \"" + project.name + "\"?");

        builder.setPositiveButton("Удалить", (dialog, which) -> {
            AppDatabase.getInstance(this).projectDao().delete(project);
            loadProjects();
        });

        builder.setNegativeButton("Отмена", null);
        builder.show();
    }

    private void showThemeDialog() {
        final String[] themes = {"Светлая тема", "Тёмная тема"};
        int checkedItem = isDarkThemeEnabled() ? 1 : 0;

        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogCustomRadioTheme);

        builder.setTitle("Выберите тему")
                .setSingleChoiceItems(themes, checkedItem, null)
                .setPositiveButton("Применить", (dialog, which) -> {
                    ListView lw = ((AlertDialog) dialog).getListView();
                    int selectedPosition = lw.getCheckedItemPosition();
                    boolean darkTheme = selectedPosition == 1;

                    saveThemePreference(darkTheme);
                    recreate();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void saveThemePreference(boolean darkTheme) {
        getSharedPreferences("app_settings", MODE_PRIVATE)
                .edit()
                .putBoolean("dark_theme", darkTheme)
                .apply();
    }

    private boolean isDarkThemeEnabled() {
        return getSharedPreferences("app_settings", MODE_PRIVATE)
                .getBoolean("dark_theme", false);
    }



}
