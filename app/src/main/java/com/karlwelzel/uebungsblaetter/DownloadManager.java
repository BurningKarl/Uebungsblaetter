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
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

/**
 * Created by karl on 28.10.17.
 */

public class DownloadManager extends AsyncTask<Integer, Integer, Integer> {
    private static final String PREFS_NAME = "UEBUNGSBLAETTER";

    protected Context context;
    protected URL directoryURL;
    protected File directoryFile;
    protected String managerID;
    protected OnListUpdateListener listener = null;
    protected SharedPreferences preferences;

    protected ArrayList<DownloadDocument> downloadDocuments;
    protected ArrayList<DownloadDocument> localFiles;

    protected ProgressDialog progressDialog;

    public DownloadManager(@NonNull Context context, URL directoryURL, File directoryFile,
                           String managerID) {
        this.context = context;
        this.directoryURL = directoryURL;
        this.directoryFile = directoryFile;
        this.managerID = managerID;
        preferences = context.getSharedPreferences(PREFS_NAME, 0);

        downloadDocuments = loadDownloadDocuments();
        localFiles = new ArrayList<>();
    }

    protected DownloadManager(@NonNull Context context, URL directoryURL, File directoryFile,
                              String managerID, ArrayList<DownloadDocument> downloadDocuments,
                              ArrayList<DownloadDocument> localFiles,
                              OnListUpdateListener listener, SharedPreferences preferences) {
        this.context = context;
        this.directoryURL = directoryURL;
        this.directoryFile = directoryFile;
        this.managerID = managerID;
        this.downloadDocuments = downloadDocuments;
        this.localFiles = localFiles;
        this.listener = listener;
        this.preferences = preferences;
    }

    public DownloadManager copy() {
        try {
            Class clazz = this.getClass();
            Constructor<?> constructor = clazz.getConstructor(Context.class, URL.class, File.class,
                    String.class, ArrayList.class, ArrayList.class,
                    OnListUpdateListener.class, SharedPreferences.class);
            return (DownloadManager) constructor.newInstance(context, directoryURL,
                    directoryFile, managerID, downloadDocuments, localFiles, listener, preferences);
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException
                | IllegalAccessException e) {
            e.printStackTrace();
            return new DownloadManager(context, directoryURL, directoryFile, managerID,
                    downloadDocuments, localFiles, listener, preferences);
        }
    }

    /* Simple helper functions */
    protected double getMaximumPoints() {
        return 20;
    }

    public String getPointsText() {
        double sum_points = 0;
        int number_of_sheets = 0;
        for (DownloadDocument downloadDocument : localFiles) {
            if (downloadDocument.getPoints() >= 0) {
                sum_points += downloadDocument.getPoints();
                number_of_sheets += 1;
            }
        }
        StringBuilder builder = new StringBuilder();
        builder.append(context.getString(R.string.points_view_prefix));
        builder.append(" ");
        if (number_of_sheets == 0) {
            builder.append("---");
        } else {
            builder.append(String.format(Locale.GERMAN, "%.1f",
                    sum_points / number_of_sheets));
            builder.append("/");
            builder.append(String.format(Locale.GERMAN, "%.1f", getMaximumPoints()));
            builder.append(" ~ ");
            builder.append(String.format(Locale.GERMAN, "%.0f",
                    100 * sum_points / number_of_sheets / getMaximumPoints()));
            builder.append("%");
        }
        return builder.toString();
    }

    protected File urlToFile(URL url) {
        return new File(directoryFile, new File(url.getPath()).getName());
    }

    protected DownloadDocument hrefToDownloadDocument(String href) throws MalformedURLException {
        return hrefToDownloadDocument(directoryURL, href);
    }

    protected DownloadDocument hrefToDownloadDocument(URL curDirectoryURL, String href)
            throws MalformedURLException {
        URL linkURL = new URL(curDirectoryURL.toString() + "/" + href);
        File linkFile = urlToFile(linkURL);
        return new DownloadDocument(linkURL, linkFile, getTitle(linkURL, linkFile));
    }

    /* Functions that should be overridden by subclasses */
    protected String getTitle(URL url, File path) {
        // This function is supposed to return the title of a DownloadDocument based on the url
        // and the path to the file.
        return path.getName();
    }

    protected void filterDownloadDocuments() {
        // The subclasses can manipulate the items that are displayed and the order in which
        // they appear here.
    }

    /* Functions used to save and load DownloadDocuments from SharedPreferences */
    protected void saveDownloadDocuments() {
        Log.d("DownloadManager", "saveDownloadDocuments");
        SharedPreferences.Editor editor = preferences.edit();
        StringBuilder allFilesBuilder;
        if (downloadDocuments.size() != 0) {
            allFilesBuilder = new StringBuilder(downloadDocuments.get(0).serialize());
            for (int i = 1; i < downloadDocuments.size(); i++) {
                allFilesBuilder.append("|").append(downloadDocuments.get(i).serialize());
            }
        } else {
            allFilesBuilder = new StringBuilder();
        }
        String completeString = allFilesBuilder.toString();
        editor.putString(managerID, completeString);
        editor.apply();
    }

    protected ArrayList<DownloadDocument> loadDownloadDocuments() {
        Log.d("DownloadManager", "loadDownloadDocuments");
        String allFiles = preferences.getString(managerID, null);
        ArrayList<DownloadDocument> data = new ArrayList<>();
        if (allFiles != null) {
            final String[] strings = allFiles.split("\\|");
            for (String string : strings) {
                DownloadDocument document = DownloadDocument.deserialize(string);
                if (document != null) {
                    data.add(document);
                }
            }
        }

        return data;
    }

    /* Functions for scanning and downloading */
    public void localScan() {
        localFiles.clear();
        for (DownloadDocument df : downloadDocuments) {
            if (df.file.exists()) {
                localFiles.add(df);
            }
        }
        notifyListener();
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
            progressDialog.setMessage(String.format(context.getString(R.string.download_message),
                    downloadDocuments.get(progress[0]).title));
            progressDialog.setProgress(progress[1]);
        }
    }

    protected void downloadDocument(int index) throws IOException {
        publishProgress(index, 0);
        DownloadDocument currentFile = downloadDocuments.get(index);
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

    protected void updateDownloadDocuments() throws IOException {
        publishProgress(-1);
        downloadDocuments.clear();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MMM-dd HH:mm:ss", Locale.US);
        Document htmlDocument = Jsoup.connect(directoryURL.toString()).get();
        Element table = htmlDocument.getElementsByTag("table").first();
        for (Element row : table.getElementsByTag("tr")) {
            Element link = row.getElementsByTag("a").first();
            if (link == null) continue;
            if (link.hasAttr("href") && link.attr("href").endsWith(".pdf")) {
                Log.d("DownloadManager", "Link found: " + link.attr("href"));
                // Create DownloadDocument
                DownloadDocument newDownloadDocument =
                        hrefToDownloadDocument(link.attr("href"));
                downloadDocuments.add(newDownloadDocument);
                // Set the date if known
                Element timestamp = row.getElementsByClass("m").first();
                try {
                    newDownloadDocument.setDate(dateFormat.parse(timestamp.text()));
                    Log.d("DownloadManager", "Date set: " +
                            dateFormat.parse(timestamp.text()));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                // Set points from localDocuments if known
                for (DownloadDocument ld : localFiles) {
                    if (ld.url.equals(newDownloadDocument.url)) {
                        newDownloadDocument.setPoints(ld.getPoints());
                        Log.d("DownloadManager", "Points set: " + ld.getPoints());
                        break;
                    }
                }
            }
        }
        filterDownloadDocuments();
        saveDownloadDocuments();
    }

    @Override
    protected Integer doInBackground(Integer... someNumber) {
        // Get a list of files from directoryURL first, update downloadFiles
        // and then download all of the them.
        ArrayList<DownloadDocument> oldDocuments = new ArrayList<>(downloadDocuments);
        try {
            updateDownloadDocuments();
            DownloadDocument current;
            for (int i = 0; i < downloadDocuments.size(); i++) {
                current = downloadDocuments.get(i);
                if (!current.file.exists() ||
                        current.getDate() != null &&
                                new Date(current.file.lastModified()).before(current.getDate())) {
                    downloadDocument(i);
                }
            }
        } catch (IOException e) {
            Log.d("downloadDocument", "Download failed. Skipping the rest");
            e.printStackTrace();
            // Add all oldDocuments that could not be downloaded
            for (DownloadDocument dd : oldDocuments) {
                if (!downloadDocuments.contains(dd)) {
                    downloadDocuments.add(dd);
                }
            }
        }
        return null;
    }

    @Override
    protected void onPostExecute(Integer result) {
        progressDialog.cancel();
        localScan();
    }

    /* Listener stuff */
    public void setListener(OnListUpdateListener listener) {
        this.listener = listener;
    }

    protected void notifyListener() {
        if (listener != null) {
            DownloadDocument[] array = new DownloadDocument[localFiles.size()];
            listener.onListUpdate(localFiles.toArray(array));
        } else {
            Log.e("Generator", "An OnListUpdateListener should have been set");
        }
    }

    interface OnListUpdateListener {
        void onListUpdate(DownloadDocument... files);
    }
}
