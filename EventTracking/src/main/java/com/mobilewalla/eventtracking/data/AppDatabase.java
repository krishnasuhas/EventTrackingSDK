package com.mobilewalla.eventtracking.data;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.mobilewalla.eventtracking.dao.EventDao;
import com.mobilewalla.eventtracking.model.Event;


@Database(entities = {Event.class}, version = 2)
public abstract class AppDatabase extends RoomDatabase {
    public abstract EventDao eventDao();
}
