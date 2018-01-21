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
        String fileName = path.getName().replace("_", "");
        switch (fileName) {
            case "skript.pdf":
                return context.getString(R.string.ana_starting_script);
            case "riemann1.pdf":
                return context.getString(R.string.ana_riemann_script);
            default:
                Matcher exerciseMatcher = Pattern.compile("uebung(\\d+)\\.pdf").matcher(fileName);
                Matcher tutoriumMatcher = Pattern.compile("tutorium(\\d+)\\.pdf").matcher(fileName);
                if (exerciseMatcher.matches()) {
                    int number = Integer.parseInt(exerciseMatcher.group(1));
                    return String.format(context.getString(R.string.sheet_name_format), number);
                } else if (tutoriumMatcher.matches()) {
                    int number = Integer.parseInt(tutoriumMatcher.group(1));
                    return String.format(context.getString(R.string.tutorium_name_format), number);
                } else {
                    return path.getName();
                }
        }
    }

    @Override
    protected void filterDownloadDocuments() {
        SparseArray<DownloadDocument> scripts = new SparseArray<>();
        SparseArray<DownloadDocument> exerciseSheets = new SparseArray<>();
        SparseArray<DownloadDocument> tutoriumSheets = new SparseArray<>();
        ArrayList<DownloadDocument> leftover = new ArrayList<>();
        String exercisePatternString = context.getString(R.string.sheet_name_format)
                .replace("%d", "(\\d+)");
        String tutoriumPatternString = context.getString(R.string.tutorium_name_format)
                .replace("%d", "(\\d+)");
        Pattern exercisePattern = Pattern.compile(exercisePatternString);
        Pattern tutoriumPattern = Pattern.compile(tutoriumPatternString);
        for (DownloadDocument dd : downloadDocuments) {
            Matcher exerciseMatcher = exercisePattern.matcher(dd.title);
            Matcher tutoriumMatcher = tutoriumPattern.matcher(dd.title);
            if (exerciseMatcher.matches()) {
                //This checks whether this is an exercise sheet or not
                exerciseSheets.put(Integer.parseInt(exerciseMatcher.group(1)), dd);
            } else if (tutoriumMatcher.matches()) {
                //This checks whether this is a tutorium sheet or not
                tutoriumSheets.put(Integer.parseInt(tutoriumMatcher.group(1)), dd);
            } else if (dd.title.equals(context.getString(R.string.ana_starting_script))) {
                scripts.put(1, dd);
            } else if (dd.title.equals(context.getString(R.string.ana_riemann_script))) {
                scripts.put(2, dd);
            } else {
                leftover.add(dd);
            }
        }
        downloadDocuments.clear();

        for (int i = 0; i < scripts.size(); i++) {
            // Reversed to have the most recent sheet at the top
            downloadDocuments.add(scripts.valueAt(i));
        }
        for (int i = exerciseSheets.size() - 1; i >= 0; i--) {
            // Reversed to have the most recent sheet at the top
            downloadDocuments.add(exerciseSheets.valueAt(i));
        }
        for (int i = tutoriumSheets.size() - 1; i >= 0; i--) {
            // Reversed to have the most recent sheet at the top
            downloadDocuments.add(tutoriumSheets.valueAt(i));
        }
        downloadDocuments.addAll(leftover);
    }
}
