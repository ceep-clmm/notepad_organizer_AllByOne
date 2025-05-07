package com.example.allbyone.models;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "projects")
public class Project {

    @PrimaryKey(autoGenerate = true)
    public long id;
    public String name;
    public String type;
    public long createdAt; // Время создания
    public String content; // Содержимое (например, для холста)
    public int folderId; // ID папки, если проект в папке
    public int labelId; // ID метки

    public String strokesJson;


    // Конструктор по умолчанию
    public Project() {
    }

    public Project(String name, String type, String content, long createdAt, int folderId, int labelId) {
        this.name = name;
        this.type = type;
        this.content = content;
        this.createdAt = createdAt;
        this.folderId = folderId;
        this.labelId = labelId;
    }

    public String getStrokesJson() {
        return strokesJson;
    }

    public void setStrokesJson(String strokesJson) {
        this.strokesJson = strokesJson;
    }

}
