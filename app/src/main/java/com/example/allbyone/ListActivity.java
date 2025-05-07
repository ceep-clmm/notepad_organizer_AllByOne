package com.example.allbyone;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.ColorUtils;

import com.example.allbyone.database.AppDatabase;
import com.example.allbyone.models.Project;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.*;

public class ListActivity extends AppCompatActivity {

    private LinearLayout itemsContainer;
    private Button addItemButton, saveButton;
    private ImageButton sortButton, backButton;
    private EditText titleEditText;

    private long projectId = -1;
    private Project currentProject = null;

    private boolean selectionMode = false;
    private ArrayList<View> selectedItems = new ArrayList<>();

    private LinearLayout selectionToolbar;
    private Button selectAllButton, deleteSelectedButton, cancelSelectionButton;

    private static final String PREF_SORT_OPTION = "list_sort_option";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        itemsContainer = findViewById(R.id.itemsContainer);
        addItemButton = findViewById(R.id.addItemButton);
        saveButton = findViewById(R.id.saveListButton);
        backButton = findViewById(R.id.backButton);
        titleEditText = findViewById(R.id.titleEditText);
        sortButton = findViewById(R.id.sortButton);

        selectionToolbar = findViewById(R.id.selectionToolbar);
        selectAllButton = findViewById(R.id.selectAllButton);
        deleteSelectedButton = findViewById(R.id.deleteSelectedButton);
        cancelSelectionButton = findViewById(R.id.cancelSelectionButton);

        selectAllButton.setOnClickListener(v -> selectAllItems());
        deleteSelectedButton.setOnClickListener(v -> confirmDeleteSelectedItems());
        cancelSelectionButton.setOnClickListener(v -> exitSelectionMode());

        sortButton.setOnClickListener(v -> showSortDialog());

        projectId = getIntent().getLongExtra("projectId", -1);
        if (projectId != -1) {
            currentProject = AppDatabase.getInstance(this).projectDao().getById((int) projectId);
            loadProjectData(currentProject);
        } else {
            addNewItem(null);
        }

        addItemButton.setOnClickListener(v -> addNewItem(null));
        saveButton.setOnClickListener(v -> saveProject());
        backButton.setOnClickListener(v -> finish());
    }

    private void loadProjectData(Project project) {
        if (project == null) return;
        titleEditText.setText(project.name);

        try {
            JSONArray itemsArray = new JSONArray(project.content);
            List<JSONObject> itemList = new ArrayList<>();
            for (int i = 0; i < itemsArray.length(); i++) {
                itemList.add(itemsArray.getJSONObject(i));
            }

            int sortOption = getSortOption();
            itemList = sortItems(itemList, sortOption);

            for (JSONObject itemObject : itemList) {
                String text = itemObject.optString("text");
                boolean checked = itemObject.optBoolean("checked", false);
                addNewItem(text, checked);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void saveProject() {
        String title = titleEditText.getText().toString().trim();
        JSONArray itemsArray = new JSONArray();

        for (int i = 0; i < itemsContainer.getChildCount(); i++) {
            View itemView = itemsContainer.getChildAt(i);
            EditText itemEditText = itemView.findViewById(R.id.checklistItemEditText);
            CheckBox checkBox = itemView.findViewById(R.id.checkBox);
            String itemText = itemEditText.getText().toString().trim();

            if (!itemText.isEmpty()) {
                JSONObject itemObject = new JSONObject();
                try {
                    itemObject.put("text", itemText);
                    itemObject.put("checked", checkBox.isChecked());
                    itemsArray.put(itemObject);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        String jsonContent = itemsArray.toString();
        if (currentProject != null) {
            currentProject.name = title.isEmpty() ? "Без названия" : title;
            currentProject.content = jsonContent;
            AppDatabase.getInstance(this).projectDao().update(currentProject);
        } else {
            Project newProject = new Project();
            newProject.name = title.isEmpty() ? "Без названия" : title;
            newProject.type = "list";
            newProject.content = jsonContent;
            newProject.createdAt = System.currentTimeMillis();
            newProject.folderId = 0;
            newProject.labelId = 0;
            AppDatabase.getInstance(this).projectDao().insert(newProject);
        }
        finish();
    }

    private void addNewItem(String text) {
        addNewItem(text, false);
    }

    private void addNewItem(String text, boolean checked) {
        View itemView = getLayoutInflater().inflate(R.layout.item_checklist_entry, itemsContainer, false);
        EditText editText = itemView.findViewById(R.id.checklistItemEditText);
        CheckBox checkBox = itemView.findViewById(R.id.checkBox);

        if (text != null) editText.setText(text);
        checkBox.setChecked(checked);
        applyCheckedStyle(editText, checked);

        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> applyCheckedStyle(editText, isChecked));

        itemView.setOnLongClickListener(v -> {
            if (!selectionMode) {
                enterSelectionMode();
                toggleSelection(itemView);
            }
            return true;
        });

        itemView.setOnClickListener(v -> {
            if (selectionMode) toggleSelection(itemView);
        });

        itemsContainer.addView(itemView);
    }

    private void applyCheckedStyle(EditText editText, boolean isChecked) {
        // Получаем текущую тему
        int nightMode = AppCompatDelegate.getDefaultNightMode();

        int textColor = (nightMode == AppCompatDelegate.MODE_NIGHT_YES)
                ? getResources().getColor(R.color.primary_dark) // Цвет для темной темы
                : getResources().getColor(android.R.color.primary_text_light); // Цвет для светлой темы

        if (isChecked) {
            editText.setPaintFlags(editText.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            editText.setTextColor(textColor);
            editText.setAlpha(0.6f);
        } else {
            editText.setPaintFlags(editText.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            editText.setTextColor(textColor);
            editText.setAlpha(1.0f);
        }
    }


    private void enterSelectionMode() {
        selectionMode = true;
        selectionToolbar.setVisibility(View.VISIBLE);
    }

    private void exitSelectionMode() {
        selectionMode = false;
        selectionToolbar.setVisibility(View.GONE);
        for (View item : selectedItems) {
            item.setBackgroundColor(getResources().getColor(android.R.color.transparent));
        }
        selectedItems.clear();
    }

    private void toggleSelection(View item) {
        if (selectedItems.contains(item)) {
            selectedItems.remove(item);
            item.setBackgroundColor(Color.TRANSPARENT);
        } else {
            selectedItems.add(item);
            item.setBackgroundColor(getSelectionBackgroundColor());
        }

        if (selectedItems.isEmpty()) exitSelectionMode();
    }

    private void selectAllItems() {
        selectedItems.clear();
        for (int i = 0; i < itemsContainer.getChildCount(); i++) {
            View item = itemsContainer.getChildAt(i);
            selectedItems.add(item);
            item.setBackgroundColor(getSelectionBackgroundColor());
        }
    }

    private int getSelectionBackgroundColor() {
        int nightMode = AppCompatDelegate.getDefaultNightMode();
        int baseColor = (nightMode == AppCompatDelegate.MODE_NIGHT_YES)
                ? getResources().getColor(R.color.highlight_dark)
                : getResources().getColor(R.color.highlight_light);

        // прозрачность (0x99 = 60%)
        return ColorUtils.setAlphaComponent(baseColor, 0x99);
    }


    private void confirmDeleteSelectedItems() {
        new AlertDialog.Builder(this)
                .setTitle("Удалить выбранные пункты?")
                .setMessage("Вы уверены, что хотите удалить выбранные элементы?")
                .setPositiveButton("ОК", (dialog, which) -> deleteSelectedItems())
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void deleteSelectedItems() {
        for (View item : selectedItems) {
            itemsContainer.removeView(item);
        }
        exitSelectionMode();
    }

    private void showSortDialog() {
        final String[] options = {
                "Сначала отмеченные",
                "Сначала неотмеченные",
                "По алфавиту (А–Я)",
                "По алфавиту (Я–А)"
        };

        ContextThemeWrapper wrapper = new ContextThemeWrapper(this, R.style.AlertDialogCustomRadioTheme);

        new AlertDialog.Builder(wrapper)
                .setTitle("Сортировка")
                .setSingleChoiceItems(options, getSortOption(), null)
                .setPositiveButton("Сохранить", (dialog, which) -> {
                    int selected = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                    saveSortOption(selected);
                    reloadListWithSorting();
                })
                .setNegativeButton("Отмена", null)
                .show();

    }

    private int getSortOption() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        return prefs.getInt(PREF_SORT_OPTION, 0);
    }

    private void saveSortOption(int option) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.edit().putInt(PREF_SORT_OPTION, option).apply();
    }

    private void reloadListWithSorting() {
        itemsContainer.removeAllViews();
        if (currentProject != null) {
            loadProjectData(currentProject);
        }
    }

    private List<JSONObject> sortItems(List<JSONObject> items, int sortOption) {
        Comparator<JSONObject> comparator;

        switch (sortOption) {
            case 0: // Сначала отмеченные + вторичная по алфавиту
                comparator = (o1, o2) -> {
                    int checkedCompare = Boolean.compare(o2.optBoolean("checked"), o1.optBoolean("checked"));
                    if (checkedCompare != 0) return checkedCompare;
                    return o1.optString("text", "").compareToIgnoreCase(o2.optString("text", ""));
                };
                break;
            case 1: // Сначала неотмеченные + вторичная по алфавиту
                comparator = (o1, o2) -> {
                    int checkedCompare = Boolean.compare(o1.optBoolean("checked"), o2.optBoolean("checked"));
                    if (checkedCompare != 0) return checkedCompare;
                    return o1.optString("text", "").compareToIgnoreCase(o2.optString("text", ""));
                };
                break;
            case 2: // По алфавиту (А–Я)
                comparator = Comparator.comparing(o -> o.optString("text", ""), String.CASE_INSENSITIVE_ORDER);
                break;
            case 3: // По алфавиту (Я–А)
                comparator = (o1, o2) -> o2.optString("text", "").compareToIgnoreCase(o1.optString("text", ""));
                break;
            default:
                return items;
        }

        items.sort(comparator);
        return items;
    }

}
