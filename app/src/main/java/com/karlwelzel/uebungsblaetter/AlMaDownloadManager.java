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
        downloadDocuments.add(hrefToDownloadDocument(
                new URL("http://www.ins.uni-bonn.de/teaching/vorlesungen/AlMaWS13"),
                "script.pdf"));
        Pattern p = Pattern.compile(".*/Uebung/Blatt\\d+\\.pdf");
        SparseArray<DownloadDocument> foundDocuments = new SparseArray<>();
        for (int i = 0; i < localFiles.size(); i++) {
            if (p.matcher(localFiles.get(i).url.toString()).matches()) {
                foundDocuments.put(i, localFiles.get(i));
            }
        }
        for (int i = 1; ; i++) {
            if (foundDocuments.indexOfKey(i) >= 0) { // is equivalent to containsKey with HashMap
                downloadDocuments.add(foundDocuments.get(i));
            } else {
                DownloadDocument df = hrefToDownloadDocument(
                        String.format("Uebung/Blatt%d.pdf", i));
                df.setDate(new Date());
                HttpURLConnection connection = (HttpURLConnection) df.url.openConnection();
                connection.setRequestMethod("HEAD");
                Log.d("updateDownloadDocuments", "Response Code for " + df.url.toString() + " is "
                        + connection.getResponseCode());
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
