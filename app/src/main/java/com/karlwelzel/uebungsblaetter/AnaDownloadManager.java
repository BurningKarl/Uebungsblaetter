package com.karlwelzel.uebungsblaetter;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.util.SparseArray;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by karl on 02.11.17.
 */

public class AnaDownloadManager extends DownloadManager {
    public AnaDownloadManager(@NonNull Context context, URL directoryURL, File directoryFile,
                              String generatorID) {
        super(context, directoryURL, directoryFile, generatorID);
    }

    // This constructor is used in DownloadFileGenerator.copy
    protected AnaDownloadManager(@NonNull Context context, URL directoryURL, File directoryFile,
                                 String generatorID, ArrayList<DownloadDocument> downloadDocuments,
                                 ArrayList<DownloadDocument> localFiles,
                                 OnListUpdateListener listener, SharedPreferences preferences) {
        super(context, directoryURL, directoryFile, generatorID, downloadDocuments, localFiles, listener, preferences);
    }

    @Override
    protected double getMaximumPoints() {
        return 40;
    }

    @Override
    protected String getTitle(URL url, File path) {
        if (path.getName().equals("skript.pdf")) {
            return context.getString(R.string.script);
        } else {
            Pattern pattern = Pattern.compile("uebung(\\d+)\\.pdf");
            Matcher matcher = pattern.matcher(path.getName());
            if (matcher.matches()) {
                int number = Integer.parseInt(matcher.group(1));
                return String.format(context.getString(R.string.sheet_name_format), number);
            } else {
                return path.getName();
            }
        }
    }

    @Override
    protected void filterDownloadDocuments() {
        DownloadDocument script = downloadDocuments.remove(0);
        SparseArray<DownloadDocument> sheets = new SparseArray<>();
        String patternString = context.getString(R.string.sheet_name_format)
                .replace("%d", "(\\d+)");
        Pattern pattern = Pattern.compile(patternString);
        for (DownloadDocument dd : downloadDocuments) {
            Matcher matcher = pattern.matcher(dd.title);
            if (matcher.matches()) {
                //This checks whether this is a sheet or not
                sheets.put(Integer.parseInt(matcher.group(1)), dd);
            }
        }
        downloadDocuments.clear();

        downloadDocuments.add(0, script);
        for (int i = sheets.size() - 1; i >= 1; i--) {
            // Reversed to have the most recent sheet at the top
            downloadDocuments.add(sheets.valueAt(i));
        }
    }
}
