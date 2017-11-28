package com.karlwelzel.uebungsblaetter;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.SparseArray;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Collections.reverse;

/**
 * Created by karl on 29.10.17.
 */

public class AlMaDownloadManager extends DownloadManager {
    public AlMaDownloadManager(@NonNull Context context, URL directoryURL, File directoryFile,
                               String generatorID) {
        super(context, directoryURL, directoryFile, generatorID);
    }

    // This constructor is used in DownloadFileGenerator.copy
    protected AlMaDownloadManager(@NonNull Context context, URL directoryURL, File directoryFile,
                                  String generatorID, ArrayList<DownloadDocument> downloadDocuments,
                                  ArrayList<DownloadDocument> localFiles,
                                  OnListUpdateListener listener, SharedPreferences preferences) {
        super(context, directoryURL, directoryFile, generatorID, downloadDocuments, localFiles,
                listener, preferences);
    }

    @Override
    protected void updateDownloadDocuments() throws IOException {
        publishProgress(-1);
        downloadDocuments.clear();
        SparseArray<DownloadDocument> foundDocuments = new SparseArray<>();
        Pattern pattern_script = Pattern.compile(".*/AlMaWS13/script.pdf");
        Pattern pattern = Pattern.compile(".*/AlmaWS17/Uebung/Blatt(\\d+)\\.pdf");
        String input;
        Matcher matcher;
        int number;
        for (int i = 0; i < localFiles.size(); i++) {
            input = localFiles.get(i).url.toString();
            matcher = pattern.matcher(input);
            if (matcher.matches()) {
                number = Integer.parseInt(matcher.group(1));
                foundDocuments.put(number, localFiles.get(i));
            }
            if (pattern_script.matcher(input).matches()) {
                foundDocuments.put(0, localFiles.get(i));
            }
        }
        for (int i = 0; ; i++) {
            if (foundDocuments.indexOfKey(i) >= 0) { // is equivalent to containsKey with HashMap
                downloadDocuments.add(foundDocuments.get(i));
            } else if (i == 0) { // this is the script
                DownloadDocument df = hrefToDownloadDocument(
                        new URL("http://www.ins.uni-bonn.de/teaching/vorlesungen/AlMaWS13"),
                        "script.pdf");
                df.setDate(new Date());
                downloadDocuments.add(df);
            } else {
                DownloadDocument df = hrefToDownloadDocument(
                        String.format("Uebung/Blatt%d.pdf", i));
                df.setDate(new Date());
                HttpURLConnection connection = (HttpURLConnection) df.url.openConnection();
                connection.setRequestMethod("HEAD");
                Log.d("updateDownloadDocuments", "Response Code for " + df.url.toString()
                        + " is " + connection.getResponseCode());
                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    downloadDocuments.add(df);
                } else {
                    break;
                }
            }
        }

        filterDownloadDocuments();
        saveDownloadDocuments();
    }

    @Override
    protected String getTitle(URL url, File path) {
        if (path.getName().equals("script.pdf")) {
            return context.getString(R.string.script);
        } else {
            Pattern pattern = Pattern.compile("Blatt(\\d+)\\.pdf");
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
        reverse(downloadDocuments);
        downloadDocuments.add(0, script);
    }
}
