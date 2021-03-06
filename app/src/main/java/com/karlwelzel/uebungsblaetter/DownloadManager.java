package com.karlwelzel.uebungsblaetter;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
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

public class DownloadManager extends AsyncTask<Void, Integer, Void> {
    private final DownloadManagerSettings settings;

    // The available documents (as displayed online)
    private ArrayList<DownloadDocument> downloadDocuments = new ArrayList<>();
    // The documents, that are already downloaded (always a subset of downloadDocuments)
    private ArrayList<DownloadDocument> localDocuments = new ArrayList<>();

    private OnListUpdateListener listener = null;
    private DownloadDocumentsAdapter adapter = null;
    private SharedPreferences preferences;
    private ProgressDialog progressDialog;

    public DownloadManager(Context context, DownloadManagerSettings settings) {
        //Log.d("DownloadManager|" + getName(), "constructor 2 "+settings.getName()+"|"+settings.getDirectoryURL().toString()+"|"+settings.getDirectory().toString());
        super();
        this.settings = settings;
        updatePreferences(context);
        loadDownloadDocuments();
        localScan(false);
    }

    public DownloadManager(Context context, String managerName, URL directoryURL, File directoryFile) {
        //Log.d("DownloadManager|" + getName(), "constructor 1 "+managerName+"|"+directoryURL.toString()+"|"+directoryFile.toString());
        this(context, new DownloadManagerSettings(managerName, directoryURL, directoryFile));
    }

    public DownloadManager(DownloadManagerSettings settings,
                           ArrayList<DownloadDocument> downloadDocuments,
                           ArrayList<DownloadDocument> localDocuments,
                           OnListUpdateListener listener,
                           DownloadDocumentsAdapter adapter,
                           SharedPreferences preferences) {
        //Log.d("DownloadManager|" + getName(), "constructor 3 "+settings.getName()+"|"+settings.getDirectoryURL().toString()+"|"+settings.getDirectory().toString());
        super();
        this.settings = settings;
        this.downloadDocuments = downloadDocuments;
        this.localDocuments = localDocuments;
        this.listener = listener;
        this.adapter = adapter;
        this.preferences = preferences;
    }

    public DownloadManager copy() {
        return new DownloadManager(settings, downloadDocuments, localDocuments, listener, adapter,
                preferences);
    }


    public void setAdapter(@NonNull DownloadDocumentsAdapter adapter) {
        Log.d("DownloadManager|" + getName(), "setAdapter: " + adapter.getManager().getName());
        this.adapter = adapter;
        setOnListUpdateListener(adapter);
    }

    public void updatePreferences(Context context) {
        preferences = context.getSharedPreferences(getManagerID(),
                Context.MODE_PRIVATE);
    }

    public void updatePreferences() {
        preferences = adapter.getContext().getSharedPreferences(getManagerID(),
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
        double totalPoints = 0;
        double totalMaximumPoints = 0;
        int numberOfSheets = 0;
        for (DownloadDocument document : localDocuments) {
            if (document.getPoints() >= 0) {
                totalPoints += document.getPoints();
                totalMaximumPoints += document.getMaximumPoints();
                numberOfSheets += 1;
            }
        }
        StringBuilder builder = new StringBuilder();
        builder.append(adapter.getContext().getString(R.string.points_view_prefix));
        builder.append(" ");
        if (numberOfSheets == 0) {
            builder.append("---");
        } else {
            // Options for displaying the points
            // option 1: average over points and over maximum points
            //  -> seems to be the obvious way, but is counterintuitive due to rounding errors
            // option 2: constant maximumPoints, points according to percentage
            //  -> Display the percentage as a number that is more directly relatable
            // option 3: total over points and over maximum points
            //  -> the user understand the numbers but has no sense for the because they grow
            //     large very quickly

            // Decision: option 3 + percentage + option 2
            double averagePoints = (totalPoints / totalMaximumPoints) * getMaximumPoints();
            double percentage = 100 * totalPoints / totalMaximumPoints;

            builder.append(String.format(Locale.GERMAN, "%.0f", totalPoints));
            builder.append("/");
            builder.append(String.format(Locale.GERMAN, "%.0f", totalMaximumPoints));
            builder.append(" ~ ");
            builder.append(String.format(Locale.GERMAN, "%.0f", percentage));
            builder.append("%");
            builder.append(" ~ ");
            builder.append(String.format(Locale.GERMAN, "%.1f", averagePoints));
            builder.append("/");
            builder.append(String.format(Locale.GERMAN, "%d", getMaximumPoints()));
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
            return adapter.getContext().getString(R.string.sheet_title_format, sheetNumber);
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
        for (DownloadDocument document : downloadDocuments) {
            if (getStickiedTitles().contains(document.title)) {
                stickied.put(getStickiedTitles().indexOf(document.title), document);
            } else if (document.sheetNumber >= 0) {
                sheets.put(document.sheetNumber, document);
            } else {
                leftover.add(document);
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
        for (DownloadDocument document : downloadDocuments) {
            if (document.file.exists()) {
                localDocuments.add(document);
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
        for (DownloadDocument oldDocument : oldDocuments) {
            DownloadDocument document = urlToDownloadDocument(oldDocument.url, oldDocument.titleId);
            document.setDate(oldDocument.getDate());
            document.setPoints(oldDocument.getPoints());
            document.setMaximumPoints(oldDocument.getMaximumPoints());
            downloadDocuments.add(document);
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
        progressDialog = new ProgressDialog(adapter.getContext());
        progressDialog.setMessage("");
        progressDialog.setIndeterminate(false);
        progressDialog.setMax(1);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setCancelable(true);
        progressDialog.show();
        progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                cancel(false);
            }
        });
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        openProgressbar();
    }

    protected void onProgressUpdate(Integer... progress) {
        Context context = adapter.getContext();
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

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void downloadDocument(int index) throws IOException {
        publishProgress(index);
        DownloadDocument currentDocument = downloadDocuments.get(index);
        currentDocument.file.getParentFile().mkdirs();
        URLConnection connection = currentDocument.url.openConnection();
        addAuthorization(connection);
        connection.connect();
        BufferedInputStream input = new BufferedInputStream(connection.getInputStream());
        FileOutputStream output = new FileOutputStream(currentDocument.file);
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
        DownloadDocument document = linkElementToDownloadDocument(link);
        HttpURLConnection connection = (HttpURLConnection) document.url.openConnection();
        addAuthorization(connection);
        connection.setRequestMethod("HEAD");
        Log.d("DownloadManager|" + getName(), "Response code: " + connection.getResponseCode());
        //Check if it can be downloaded
        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            //Set date from Last-Modified header
            Date lastModified = new Date(connection.getLastModified());
            document.setDate(lastModified);

            //Set points from local files (if known)
            for (DownloadDocument localDocument : localDocuments) {
                if (localDocument.equals(document)) { //Compares urls
                    document.setPoints(localDocument.getPoints());
                    document.setMaximumPoints(localDocument.getMaximumPoints());
                    break;
                }
            }
            downloadDocuments.add(document);
        }
        return connection.getResponseCode();
    }

    private void updateDownloadDocuments() throws IOException, TaskCancelledException {
        publishProgress(-1);
        downloadDocuments.clear();
        Document htmlDocument = Jsoup.connect(getDirectoryURL().toString()).get();
        boolean someDocumentsInaccessible = false;
        boolean someDocumentsUnauthorized = false;
        for (Element link : htmlDocument.getElementsByAttributeValueEnding("href", ".pdf")) {
            if (isCancelled()) {
                throw new TaskCancelledException();
            }
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
        }
        if (isCancelled()) {
            throw new TaskCancelledException();
        }
        sortDownloadDocuments();
        saveDownloadDocuments();
        if (someDocumentsInaccessible) {
            Snackbar.make(adapter.getContentLayout(),
                    R.string.documents_inaccessible, Snackbar.LENGTH_LONG)
                    .show();
        }
        if (someDocumentsUnauthorized) {
            Snackbar.make(adapter.getContentLayout(),
                    R.string.documents_unauthorized, Snackbar.LENGTH_LONG)
                    .show();
        }
    }

    @Override
    protected Void doInBackground(Void... params) {
        // Get a list of files from directoryURL first, update downloadFiles
        // and then download all of the them.
        ArrayList<DownloadDocument> oldDocuments = new ArrayList<>(downloadDocuments);
        boolean indexDownloadSuccessful = true;
        try {
            updateDownloadDocuments();
        } catch (IOException e) {
            e.printStackTrace();
            Log.d("DownloadManager|" + getName(),
                    "Downloading the index file failed. Keep the old documents");
            indexDownloadSuccessful = false;
            Snackbar.make(adapter.getContentLayout(),
                    R.string.download_failed_index_file, Snackbar.LENGTH_LONG)
                    .show();
        } catch (TaskCancelledException e) {
            e.printStackTrace();
            Log.d("DownloadManager|" + getName(),
                    "User cancelled the download of the index file. Keep the old documents");
            indexDownloadSuccessful = false;
        }

        if (indexDownloadSuccessful) {
            progressDialog.setMax(downloadDocuments.size());
            try {
                DownloadDocument currentDocument;
                for (int i = 0; i < downloadDocuments.size(); i++) {
                    if (isCancelled()) {
                        throw new TaskCancelledException();
                    }
                    currentDocument = downloadDocuments.get(i);
                    if (!currentDocument.file.exists() || currentDocument.getDate() != null &&
                            new Date(currentDocument.file.lastModified()).before(currentDocument.getDate())) {
                        downloadDocument(i);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.d("DownloadManager|" + getName(), "Download failed. Skipping the rest");
                Snackbar.make(adapter.getContentLayout(),
                        R.string.download_failed, Snackbar.LENGTH_LONG)
                        .show();

            } catch (TaskCancelledException e) {
                e.printStackTrace();
                Log.d("DownloadManager|" + getName(), "Download cancelled. Skipping the rest");
            }
        } else {
            downloadDocuments = oldDocuments;
            updateDownloadDocumentsOffline();
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void result) {
        progressDialog.cancel();
        localScan();
    }

    @Override
    protected void onCancelled(Void result) {
        onPostExecute(result);
    }

    @SuppressWarnings("unused")
    public class TaskCancelledException extends Exception {
        public TaskCancelledException() {

        }

        public TaskCancelledException(String message) {
            super(message);
        }

        public TaskCancelledException(Throwable cause) {
            super(cause);
        }

        public TaskCancelledException(String message, Throwable cause) {
            super(message, cause);
        }
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
        void onListUpdate(DownloadDocument... documents);
    }
}
