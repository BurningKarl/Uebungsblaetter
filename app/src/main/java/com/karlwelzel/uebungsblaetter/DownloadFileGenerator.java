package com.karlwelzel.uebungsblaetter;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

/**
 * Created by karl on 28.10.17.
 */

public class DownloadFileGenerator extends AsyncTask<Integer, Integer, Integer> {
    private static final String PREFS_NAME = "UEBUNGSBLAETTER";

    protected Context context;
    protected URL directoryURL;
    protected File directoryFile;
    protected String generatorID;
    protected OnListUpdateListener listener = null;
    protected SharedPreferences preferences;

    protected ArrayList<DownloadFile> downloadFiles;
    protected ArrayList<DownloadFile> localFiles;

    protected ProgressDialog progressDialog;

    public DownloadFileGenerator(@NonNull Context context, URL directoryURL, File directoryFile,
                                 String generatorID) {
        this.context = context;
        this.directoryURL = directoryURL;
        this.directoryFile = directoryFile;
        this.generatorID = generatorID;
        preferences = context.getSharedPreferences(PREFS_NAME, 0);
        String allFiles = preferences.getString(generatorID, null);
        downloadFiles = new ArrayList<>();
        if (allFiles != null) {
            final String[] strings = allFiles.split("\\|");
            URL url;
            File file;
            for (String string : strings) {
                try {
                    url = new URL(string);
                    file = urlToFile(url);
                    downloadFiles.add(new DownloadFile(url, file, getTitle(url, file)));
                } catch (MalformedURLException e) {
                    Log.d("Generator", "MalformedURLException: " + string);
                    //e.printStackTrace();
                }
            }
        }
        localFiles = new ArrayList<>();
    }

    protected DownloadFileGenerator(@NonNull Context context, URL directoryURL, File directoryFile,
                                    String generatorID, ArrayList<DownloadFile> downloadFiles,
                                    ArrayList<DownloadFile> localFiles, OnListUpdateListener listener,
                                    SharedPreferences preferences) {
        this.context = context;
        this.directoryURL = directoryURL;
        this.directoryFile = directoryFile;
        this.generatorID = generatorID;
        this.downloadFiles = downloadFiles;
        this.localFiles = localFiles;
        this.listener = listener;
        this.preferences = preferences;
    }

    protected File urlToFile(URL url) {
        return new File(directoryFile, new File(url.getPath()).getName());
    }

    public void setListener(OnListUpdateListener listener) {
        this.listener = listener;
    }

    // Should be overridden by subclasses
    protected String getTitle(URL url, File path) {
        return path.getName();
    }

    public void localScan() {
        localFiles.clear();
        for (DownloadFile df : downloadFiles) {
            if (df.file.exists()) {
                localFiles.add(df);
            }
        }
        notifyListener();
    }

    protected void notifyListener() {
        if (listener != null) {
            DownloadFile[] array = new DownloadFile[localFiles.size()];
            listener.onListUpdate(localFiles.toArray(array));
        } else {
            Log.e("Generator", "An OnListUpdateListener should have been set");
        }
    }

    protected void filterDownloadFiles() {
        // Should be overridden by subclasses
        // TODO: Implement subclasses and the filterDownloadFiles function
        // The subclasses should manipulate the items that are displayed and the order in which
        // they appear
    }

    public void download() {
        execute();
    }

    protected void openProgressbar() {
        Log.d("DownloadManager", "openProgressbar");
        progressDialog = new ProgressDialog(context);
        progressDialog.setMessage("");
        progressDialog.setIndeterminate(false);
        progressDialog.setMax(100);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setCancelable(true);
        progressDialog.show();
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        openProgressbar();
    }

    protected void onProgressUpdate(Integer... progress) {
        if (progress[0] == -1) {
            // Downloading the index file
            progressDialog.setMessage(context.getString(R.string.download_message_index_file));
            progressDialog.setProgress(100);
        } else {
            progressDialog.setMessage(String.format(context.getResources().getString(R.string.download_message),
                    downloadFiles.get(progress[0]).title));
            progressDialog.setProgress(progress[1]);
        }
    }

    protected void downloadFile(int index) throws IOException {
        publishProgress(index, 0);
        DownloadFile currentFile = downloadFiles.get(index);
        currentFile.file.getParentFile().mkdirs();
        URLConnection connection = currentFile.url.openConnection();
        connection.connect();
        int lengthOfFile = connection.getContentLength();
        BufferedInputStream input = new BufferedInputStream(connection.getInputStream());
        FileOutputStream output = new FileOutputStream(currentFile.file);
        byte data[] = new byte[1024];
        int count;
        long total = 0;
        while (progressDialog.isShowing() && !isCancelled() && (count = input.read(data)) != -1) {
            total += count;
            publishProgress(index, (int) (total * 100) / lengthOfFile);
            output.write(data, 0, count);
        }
        output.flush();
        output.close();
        input.close();
    }

    protected DownloadFile hrefToDownloadFile(String href) throws MalformedURLException {
        URL linkURL = new URL(directoryURL.toString() + "/" + href);
        File linkFile = urlToFile(linkURL);
        return new DownloadFile(linkURL, linkFile, getTitle(linkURL, linkFile));
    }

    protected void updateDownloadFiles() throws IOException {
        publishProgress(-1);
        downloadFiles.clear();
        Document htmlDocument = Jsoup.connect(directoryURL.toString()).get();
        Element table = htmlDocument.getElementsByTag("table").first();
        for (Element link : table.getElementsByTag("a")) {
            if (link.hasAttr("href") && link.attr("href").endsWith(".pdf")) {
                downloadFiles.add(hrefToDownloadFile(link.attr("href")));
            }
        }
        filterDownloadFiles();

        SharedPreferences.Editor editor = preferences.edit();
        StringBuilder allFilesBuilder;
        if (downloadFiles.size() != 0) {
            allFilesBuilder = new StringBuilder(downloadFiles.get(0).url.toString());
            for (int i = 1; i < downloadFiles.size(); i++) {
                allFilesBuilder.append("|").append(downloadFiles.get(i).url.toString());
            }
        } else {
            allFilesBuilder = new StringBuilder();
        }
        editor.putString(generatorID, allFilesBuilder.toString());
        editor.apply();
    }

    @Override
    protected Integer doInBackground(Integer... someNumber) {
        // Get a list of files from directoryURL first, update downloadFiles
        // and then download all of the them.
        try {
            updateDownloadFiles();
            for (int i = 0; i < downloadFiles.size(); i++) {
                if (!downloadFiles.get(i).file.exists()) {
                    downloadFile(i);
                }
            }
        } catch (IOException e) {
            Log.d("downloadFile", "Download failed. Skipping the rest");
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(Integer result) {
        progressDialog.cancel();
        localScan();
    }

    public DownloadFileGenerator copy() {
        return new DownloadFileGenerator(context, directoryURL, directoryFile, generatorID,
                downloadFiles, localFiles, listener, preferences);
    }

    interface OnListUpdateListener {
        void onListUpdate(DownloadFile... files);
    }
}
