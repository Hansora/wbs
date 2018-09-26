package com.example.biophone;


import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.Calendar;

public class DatabaseHelper extends SQLiteOpenHelper {
  static final private String DBNAME = "heart_rate.sqlite";
  static final private int VERSION = 1;
  static final CharSequence dateText  = android.text.format.DateFormat.format("yyyy_MM_dd", Calendar.getInstance());

  public DatabaseHelper(Context context) {
    super(context, DBNAME, null, VERSION);
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    String SQL = "CREATE TABLE IF NOT EXISTS heart_rate_" + dateText + " (id INTEGER PRIMARY KEY AUTOINCREMENT, time TIMESTAMP, HR INTEGER)";
    db.execSQL(SQL);
  }

  public void insertData(SQLiteDatabase db, CharSequence time, int HR) {
    String SQL = "INSERT INTO heart_rate_" + dateText + " (time, HR) VALUES ('" + time + "', " + HR + ")";
    db.execSQL(SQL);
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

  }
}
