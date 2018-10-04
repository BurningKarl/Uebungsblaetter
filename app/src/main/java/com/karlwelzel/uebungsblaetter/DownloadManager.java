package com.karlwelzel.uebungsblaetter;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.design.widget.Snackbar;
import android.util.Base64;
import android.util.Log;
import android.util.SparseArray;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by karl on 28.10.17.
 */

/*
 * TODO: Rethink variable names and Log tag
 * Sheets -> Documents in SheetsListViewAdapter
 * downloadDocuments ?-> documents
 */

public class DownloadManager extends AsyncTask<Integer, Integer, Integer> {
    private DownloadManagerSettings settings;

    // The available documents (as displayed online)
    private ArrayList<DownloadDocument> downloadDocuments = new ArrayList<>();
    // The documents, that are already downloaded (always a subset of downloadDocuments)
    private ArrayList<DownloadDocument> localDocuments = new ArrayList<>();

    private OnListUpdateListener listener = null;
    private SharedPreferences preferences;
    private ProgressDialog progressDialog;

    public DownloadManager(String managerName, URL directoryURL, File directoryFile) {
        //Log.d("DownloadManager|" + getName(), "constructor 1 "+managerName+"|"+directoryURL.toString()+"|"+directoryFile.toString());
        settings = new DownloadManagerSettings(managerName, directoryURL, directoryFile);
        updatePreferences();
        loadDownloadDocuments();
        localScan(false);
    }

    public DownloadManager(DownloadManagerSettings settings) {
        //Log.d("DownloadManager|" + getName(), "constructor 2 "+settings.getName()+"|"+settings.getDirectoryURL().toString()+"|"+settings.getDirectory().toString());
        this.settings = settings;
        updatePreferences();
        loadDownloadDocuments();
        localScan(false);
    }

    public DownloadManager(DownloadManagerSettings settings,
                           ArrayList<DownloadDocument> downloadDocuments,
                           ArrayList<DownloadDocument> localDocuments,
                           OnListUpdateListener listener,
                           SharedPreferences preferences) {
        //Log.d("DownloadManager|" + getName(), "constructor 3 "+settings.getName()+"|"+settings.getDirectoryURL().toString()+"|"+settings.getDirectory().toString());
        this.settings = settings;
        this.downloadDocuments = downloadDocuments;
        this.localDocuments = localDocuments;
        this.listener = listener;
        this.preferences = preferences;
    }

    public DownloadManager copy() {
        return new DownloadManager(settings, downloadDocuments, localDocuments, listener,
                preferences);
    }

    public void updatePreferences() {
        preferences = MainActivity.getContext().getSharedPreferences(getManagerID(),
                Context.MODE_PRIVATE);
    }

    /* Getter and setter functions */
    public DownloadManagerSettings getSettings() {
        return settings;
    }

    public String getName() {
        return settings.getName();
    }

    public void setName(String name) {
        Log.d("DownloadManager|" + getName(), "setName:\n" + name);
        settings.setName(name);
    }

    public URL getDirectoryURL() {
        return settings.getDirectoryURL();
    }

    // When setting a new url, downloadDocuments is loaded from preferences
    public void setDirectoryURL(URL directoryURL) {
        Log.d("DownloadManager|" + getName(), "setDirectoryURL:\n" + directoryURL.toString());
        if (!settings.getDirectoryURL().equals(directoryURL)) {
            settings.setDirectoryURL(directoryURL);
            updatePreferences();
            loadDownloadDocuments();
        }
    }

    public File getDirectory() {
        return settings.getDirectory();
    }

    public String getManagerID() {
        return settings.getManagerID();
    }

    public int getMaximumPoints() {
        return settings.maximumPoints;
    }

    public void setMaximumPoints(int value) {
        Log.d("DownloadManager|" + getName(), "setMaximumPoints:\n" + value);
        if (value <= 0) {
            settings.maximumPoints = 10;
        } else {
            settings.maximumPoints = value;
        }
    }

    public String getSheetRegex() {
        return settings.sheetRegex;
    }

    public void setSheetRegex(String value) {
        Log.d("DownloadManager|" + getName(), "setSheetRegex:\n" + value);
        if (value == null) {
            settings.sheetRegex = "";
        } else {
            settings.sheetRegex = value;
        }
    }

    public ArrayList<String> getStickiedTitles() {
        return settings.stickiedTitles;
    }

    public void setStickiedTitles(ArrayList<String> value) {
        String dataString = new Gson().toJson(value);
        Log.d("DownloadManager|" + getName(), "setStickiedTitles:\n" + dataString);
        if (value == null) {
            settings.stickiedTitles = new ArrayList<>();
        } else {
            settings.stickiedTitles = value;
        }
    }

    public HashMap<String, String> getTitleMap() {
        return settings.titleMap;
    }

    public void setTitleMap(HashMap<String, String> value) {
        String dataString = new Gson().toJson(value);
        Log.d("DownloadManager|" + getName(), "setTitleMap:\n" + dataString);
        if (value == null) {
            settings.titleMap = new HashMap<>();
        } else {
            settings.titleMap = value;
        }
    }

    public String getUsername() {
        return settings.username;
    }

    public void setUsername(String value) {
        Log.d("DownloadManager|" + getName(), "setUsername: " + value);
        if (value == null) {
            settings.username = "";
        } else {
            settings.username = value;
        }
    }

    public String getPassword() {
        return settings.password;
    }

    public void setPassword(String value) {
        Log.d("DownloadManager|" + getName(), "setPassword: " + value);
        if (value == null) {
            settings.password = "";
        } else {
            settings.password = value;
        }
    }

    /* Simple helper functions */
    public String getPointsText() {
        double total_points = 0;
        double total_maximum_points = 0;
        int number_of_sheets = 0;
        for (DownloadDocument dd : localDocuments) {
            if (dd.getPoints() >= 0) {
                total_points += dd.getPoints();
                total_maximum_points += dd.getMaximumPoints();
                number_of_sheets += 1;
            }
        }
        StringBuilder builder = new StringBuilder();
        builder.append(MainActivity.getContext().getString(R.string.points_view_prefix));
        builder.append(" ");
        if (number_of_sheets == 0) {
            builder.append("---");
        } else {
            double averagePoints = total_points / number_of_sheets;
            double averageMaximumPoints = total_maximum_points / (double) number_of_sheets;
            double percentage = 100 * total_points / total_maximum_points;

            builder.append(String.format(Locale.GERMAN, "%.1f", averagePoints));
            builder.append("/");
            builder.append(String.format(Locale.GERMAN, "%.0f", averageMaximumPoints));
            builder.append(" ~ ");
            builder.append(String.format(Locale.GERMAN, "%.0f", percentage));
            builder.append("%");
        }
        return builder.toString();
    }

    private void addAuthorization(URLConnection connection) {
        String userCredentials = getUsername() + ":" + getPassword();
        String basicAuth = "Basic " + Base64.encodeToString(userCredentials.getBytes(), Base64.NO_WRAP);
        connection.setRequestProperty("Authorization", basicAuth);
    }

    private File urlToFile(URL url) {
        return new File(getDirectory(), new File(url.getPath()).getName());
    }

    private int suggestionToSheetNumber(String titleSuggestion) {
        Pattern pattern = Pattern.compile(getSheetRegex());
        Matcher matcher = pattern.matcher(titleSuggestion);
        if (matcher.matches() && matcher.groupCount() >= 1) {
            return Integer.valueOf(matcher.group(1));
        } else {
            return -1;
        }
    }

    private String suggestionToTitle(String titleSuggestion, int sheetNumber) {
        if (getTitleMap().containsKey(titleSuggestion)) {
            return getTitleMap().get(titleSuggestion);
        } else if (sheetNumber >= 0) {
            return MainActivity.getContext().getString(R.string.sheet_title_format, sheetNumber);
        } else {
            return titleSuggestion;
        }
    }

    private DownloadDocument urlToDownloadDocument(URL linkURL, String titleSuggestion) {
        File linkFile = urlToFile(linkURL);
        int sheetNumber = suggestionToSheetNumber(titleSuggestion);
        String title = suggestionToTitle(titleSuggestion, sheetNumber);
        return new DownloadDocument(linkURL, linkFile, titleSuggestion, title, sheetNumber,
                getMaximumPoints());
    }

    private DownloadDocument linkElementToDownloadDocument(Element link)
            throws MalformedURLException {
        URL linkURL = new URL(link.attr("abs:href"));
        return urlToDownloadDocument(linkURL, link.text());
    }

    private void sortDownloadDocuments() {
        /*
         * The items in downloadDocuments and their order can be manipulated here
         *
         * All pdf-files are displayed in this order
         * 1. All stickied documents by given order
         * 2. All sheets by number (reversed)
         * 3. Everything else by date
         */

        Comparator<DownloadDocument> dateComparator = new Comparator<DownloadDocument>() {
            @Override
            public int compare(DownloadDocument doc1, DownloadDocument doc2) {
                return doc2.getDate().compareTo(doc1.getDate()); //reversed
            }
        };
        SparseArray<DownloadDocument> stickied = new SparseArray<>();
        SparseArray<DownloadDocument> sheets = new SparseArray<>();
        ArrayList<DownloadDocument> leftover = new ArrayList<>();
        for (DownloadDocument dd : downloadDocuments) {
            if (getStickiedTitles().contains(dd.title)) {
                stickied.put(getStickiedTitles().indexOf(dd.title), dd);
            } else if (dd.sheetNumber >= 0) {
                sheets.put(dd.sheetNumber, dd);
            } else {
                leftover.add(dd);
            }
        }
        downloadDocuments.clear();

        Collections.sort(leftover, dateComparator);
        for (int i = 0; i < stickied.size(); i++) {
            downloadDocuments.add(stickied.valueAt(i));
        }
        for (int i = sheets.size() - 1; i >= 0; i--) {
            downloadDocuments.add(sheets.valueAt(i));
        }
        downloadDocuments.addAll(leftover);
    }

    /* Functions used to save and load DownloadDocuments from SharedPreferences */
    public void saveDownloadDocuments() {
        String dataString = new Gson().toJson(downloadDocuments);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("documents", dataString);
        Log.d("DownloadManager|" + getName(), "saveDownloadDocuments:\n" + dataString);
        editor.apply();
    }

    private void loadDownloadDocuments() {
        Type collectionType = new TypeToken<ArrayList<DownloadDocument>>() {
        }.getType();
        String dataString = preferences.getString("documents", "[]");
        Log.d("DownloadManager|" + getName(), "loadDownloadDocuments:\n" + dataString);
        downloadDocuments = new Gson().fromJson(dataString, collectionType);
    }

    /* Functions for scanning and downloading */
    public void localScan(boolean callNotifyListener) {
        localDocuments.clear();
        for (DownloadDocument dd : downloadDocuments) {
            if (dd.file.exists()) {
                localDocuments.add(dd);
            }
        }
        if (callNotifyListener) {
            notifyListener();
        }
    }

    public void localScan() {
        localScan(true);
    }

    private void updateDownloadDocumentsOffline() {
        ArrayList<DownloadDocument> oldDocuments = new ArrayList<>(downloadDocuments);
        downloadDocuments.clear();
        for (DownloadDocument oldDD : oldDocuments) {
            DownloadDocument dd = urlToDownloadDocument(oldDD.url, oldDD.titleId);
            dd.setDate(oldDD.getDate());
            dd.setPoints(oldDD.getPoints());
            dd.setMaximumPoints(oldDD.getMaximumPoints());
            downloadDocuments.add(dd);
        }
        sortDownloadDocuments();
        saveDownloadDocuments();
    }

    // Does the same as download but without actually updating the documents
    // This is useful after changing sheetRegex or stickiedTitle
    public void downloadOffline() {
        updateDownloadDocumentsOffline();
        localScan();
    }

    public void download() {
        execute();
    }

    private void openProgressbar() {
        Log.d("DownloadManager|" + getName(), "openProgressbar");
        progressDialog = new ProgressDialog(MainActivity.getContext());
        progressDialog.setMessage("");
        progressDialog.setIndeterminate(false);
        progressDialog.setMax(1);
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
        Context context = MainActivity.getContext();
        if (progress[0] == -1) {
            // Downloading the index file
            progressDialog.setMessage(context.getString(R.string.download_message_index_file));
            progressDialog.setProgress(0);
        } else {
            progressDialog.setMessage(context.getString(R.string.download_message,
                    downloadDocuments.get(progress[0]).title));
            progressDialog.setProgress(progress[0] + 1);
        }
    }

    private void downloadDocument(int index) throws IOException {
        publishProgress(index);
        DownloadDocument currentFile = downloadDocuments.get(index);
        currentFile.file.getParentFile().mkdirs();
        URLConnection connection = currentFile.url.openConnection();
        addAuthorization(connection);
        connection.connect();
        int lengthOfFile = connection.getContentLength();
        BufferedInputStream input = new BufferedInputStream(connection.getInputStream());
        FileOutputStream output = new FileOutputStream(currentFile.file);
        byte data[] = new byte[1024];
        int count;
        long total = 0;
        while (progressDialog.isShowing() && !isCancelled() && (count = input.read(data)) != -1) {
            total += count;
            output.write(data, 0, count);
        }
        output.flush();
        output.close();
        input.close();
    }

    private int addDownloadDocumentByLinkElement(Element link) throws IOException {
        //Create download Document
        DownloadDocument dd = linkElementToDownloadDocument(link);
        HttpURLConnection connection = (HttpURLConnection) dd.url.openConnection();
        addAuthorization(connection);
        connection.setRequestMethod("HEAD");
        Log.d("DownloadManager|" + getName(), "Response code: " + connection.getResponseCode());
        //Check if it can be downloaded
        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            //Set date from Last-Modified header
            Date lastModified = new Date(connection.getLastModified());
            dd.setDate(lastModified);

            //Set points from local files (if known)
            for (DownloadDocument ld : localDocuments) {
                if (ld.equals(dd)) { //Compares urls
                    dd.setPoints(ld.getPoints());
                    dd.setMaximumPoints(ld.getMaximumPoints());
                    break;
                }
            }
            downloadDocuments.add(dd);
        }
        return connection.getResponseCode();
    }

    private void updateDownloadDocuments() throws IOException {
        publishProgress(-1);
        downloadDocuments.clear();
        Document htmlDocument = Jsoup.connect(getDirectoryURL().toString()).get();
        boolean someDocumentsInaccessible = false;
        boolean someDocumentsUnauthorized = false;
        for (Element link : htmlDocument.getElementsByAttributeValueEnding("href", ".pdf")) {
            Log.d("DownloadManager|" + getName(), "Link found: " + link.attr("href"));
            int responseCode;
            try {
                responseCode = addDownloadDocumentByLinkElement(link);
            } catch (IOException e) {
                Log.d("DownloadManager|" + getName(), "Accessing this files metadata failed: " + e.getMessage());
                responseCode = -1;
            }
            switch (responseCode) {
                case 200: //successful
                    break;
                case 401: //unauthorized
                    someDocumentsUnauthorized = true;
                    break;
                default: //failed with other reasons
                    someDocumentsInaccessible = true;
                    break;
            }
            //TODO: Break when the user cancels the task
        }
        sortDownloadDocuments();
        saveDownloadDocuments();
        if (someDocumentsInaccessible) {
            Snackbar.make(MainActivity.contentView,
                    R.string.documents_inaccessible, Snackbar.LENGTH_LONG)
                    .show();
        }
        if (someDocumentsUnauthorized) {
            Snackbar.make(MainActivity.contentView,
                    R.string.documents_unauthorized, Snackbar.LENGTH_LONG)
                    .show();
        }
    }

    @Override
    protected Integer doInBackground(Integer... someNumber) {
        // Get a list of files from directoryURL first, update downloadFiles
        // and then download all of the them.
        //TODO: User cancelling should stop the refreshLayout from refreshing
        ArrayList<DownloadDocument> oldDocuments = new ArrayList<>(downloadDocuments);
        boolean indexDownloadSuccessful = true;
        try {
            updateDownloadDocuments();
        } catch (IOException e) {
            e.printStackTrace();
            Log.d("DownloadManager|" + getName(), "Downloading the index file failed. Keep the old documents");
            indexDownloadSuccessful = false;
            downloadDocuments = oldDocuments;
            updateDownloadDocumentsOffline();
            Snackbar.make(MainActivity.contentView,
                    R.string.download_failed_index_file, Snackbar.LENGTH_LONG)
                    .show();
        }
        progressDialog.setMax(downloadDocuments.size());
        if (indexDownloadSuccessful) {
            try {
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
                Log.d("DownloadManager|" + getName(), "Download failed. Skipping the rest");
                e.printStackTrace();
                Snackbar.make(MainActivity.contentView,
                        R.string.download_failed, Snackbar.LENGTH_LONG)
                        .show();

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
    public void setOnListUpdateListener(OnListUpdateListener listener) {
        this.listener = listener;
        notifyListener();
    }

    private void notifyListener() {
        if (listener != null) {
            DownloadDocument[] array = new DownloadDocument[localDocuments.size()];
            listener.onListUpdate(localDocuments.toArray(array));
        } else {
            Log.e("DownloadManager", "An OnListUpdateListener should have been set");
        }
    }

    interface OnListUpdateListener {
        void onListUpdate(DownloadDocument... files);
    }
}
