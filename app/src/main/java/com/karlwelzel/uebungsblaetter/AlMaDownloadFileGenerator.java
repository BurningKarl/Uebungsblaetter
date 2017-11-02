package com.karlwelzel.uebungsblaetter;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 * Created by karl on 29.10.17.
 */

public class AlMaDownloadFileGenerator extends DownloadFileGenerator {
    public AlMaDownloadFileGenerator(@NonNull Context context, URL directoryURL, File directoryFile,
                                     String generatorID) {
        super(context, directoryURL, directoryFile, generatorID);
    }

    @Override
    protected void updateDownloadFiles() throws IOException {
        publishProgress(-1);
        downloadFiles.clear();
        // "script.pdf"
        // "Uebung/Blatt1.pdf", "Uebung/Blatt2.pdf", ...
        downloadFiles.add(hrefToDownloadFile("script.pdf"));
        Pattern p = Pattern.compile(".*/Uebung/Blatt\\d+\\.pdf");
        ArrayList<String> foundFiles = new ArrayList<>();
        for (int i = 0; i < localFiles.size(); i++) {
            if (p.matcher(localFiles.get(i).url.toString()).matches()) {
                foundFiles.add(localFiles.get(i).url.toString());
            }
        }
        for (int i = 1; ; i++) {
            DownloadFile df = hrefToDownloadFile(String.format("Uebung/Blatt%d.pdf", i));
            if (foundFiles.contains(df.url.toString())) {
                downloadFiles.add(df);
            } else {
                HttpURLConnection connection = (HttpURLConnection) df.url.openConnection();
                connection.setRequestMethod("HEAD");
                Log.d("updateDownloadFiles", "Response Code for " + df.url.toString() + " is "
                        + connection.getResponseCode());
                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    downloadFiles.add(df);
                } else {
                    break;
                }
            }
        }

        filterDownloadFiles();

        SharedPreferences.Editor editor = preferences.edit();
        StringBuilder allFilesBuilder = new StringBuilder();
        for (DownloadFile df : downloadFiles) {
            allFilesBuilder.append(df.url.toString()).append("|");
        }
        allFilesBuilder.deleteCharAt(allFilesBuilder.length() - 1);
        editor.putString(generatorID, allFilesBuilder.toString());
        editor.apply();
    }

}
