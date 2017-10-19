package com.karlwelzel.uebungsblaetter;

import android.content.SharedPreferences;
import android.util.Log;

import java.io.File;
import java.util.Date;

/**
 * Created by karl on 15.10.17.
 */

public class ExerciseSheet {
    public String title;
    public File file;
    public Date date;

    public ExerciseSheet(File file, String title, Date date) {
        this.file = file;
        this.title = title;
        this.date = date;
        Log.d("ExerciseSheet", "title="+this.title);
    }
}
