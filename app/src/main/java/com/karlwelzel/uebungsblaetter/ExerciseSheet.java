package com.karlwelzel.uebungsblaetter;

import android.util.Log;

import java.io.File;
import java.util.Date;

/**
 * Created by karl on 15.10.17.
 */

@SuppressWarnings("WeakerAccess")
public class ExerciseSheet {
    /*
     * TODO: Maybe merge ExerciseSheet and DownloadFile
     * This would be useful because they have overlapping information, the name of ExerciseSheet
     * does not fit its content anymore and because both of them are just containers for some
     * information.
    */
    public final String title;
    public final File file;
    public Date date;

    public ExerciseSheet(File file, String title, Date date) {
        this.file = file;
        this.title = title;
        this.date = date;
        Log.d("ExerciseSheet", "title="+this.title);
    }
}
