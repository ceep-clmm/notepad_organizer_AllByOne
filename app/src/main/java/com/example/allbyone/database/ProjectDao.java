package com.example.allbyone.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;

import com.example.allbyone.models.Project;

import java.util.List;

@Dao
public interface ProjectDao {

    @Insert
    long insert(Project project);

    @Update
    void update(Project project);

    @Delete
    void delete(Project project);

    @Query("SELECT * FROM projects ORDER BY createdAt DESC")
    List<Project> getAll();

    @Query("SELECT * FROM projects WHERE id = :id")
    Project getById(int id);
}

