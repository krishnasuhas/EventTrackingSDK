package com.mobilewalla.eventtracking.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.mobilewalla.eventtracking.model.Event;

import java.util.List;

@Dao
public interface EventDao {
    @Query("SELECT * FROM EVENT")
    List<Event> getAll();

    @Query("SELECT * FROM EVENT WHERE uuid LIKE :id limit 1")
    Event getEventById(int id);

    @Query("SELECT COUNT(*) FROM EVENT")
    int getCount();

    @Insert
    void insert(Event... events);

    @Delete
    void delete(Event event);

    @Delete
    void deleteAll(Event... events);

    @Query("DELETE FROM EVENT")
    void clear();
}
