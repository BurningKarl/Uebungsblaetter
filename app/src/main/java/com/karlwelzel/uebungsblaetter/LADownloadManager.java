package com.karlwelzel.uebungsblaetter;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Collections.reverse;

/**
 * Created by karl on 02.11.17.
 */

public class LADownloadManager extends DownloadManager {
    public LADownloadManager(@NonNull Context context, URL directoryURL, File directoryFile,
                             String generatorID) {
        super(context, directoryURL, directoryFile, generatorID);
    }

    // This constructor is used in DownloadFileGenerator.copy
    protected LADownloadManager(@NonNull Context context, URL directoryURL, File directoryFile,
                                String generatorID, ArrayList<DownloadDocument> downloadDocuments,
                                ArrayList<DownloadDocument> localFiles,
                                OnListUpdateListener listener, SharedPreferences preferences) {
        super(context, directoryURL, directoryFile, generatorID, downloadDocuments, localFiles,
                listener, preferences);
    }

    @Override
    protected double getMaximumPoints() {
        return 16;
    }

    @Override
    protected String getTitle(URL url, File path) {
        if (path.getName().equals("LA_2017.pdf")) {
            return context.getString(R.string.script);
        } else {
            Pattern pattern = Pattern.compile("u(\\d+)_ws1718\\.pdf");
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
