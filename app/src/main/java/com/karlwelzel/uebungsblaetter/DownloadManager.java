package com.karlwelzel.uebungsblaetter;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
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

// TODO: Rethink variable names and Log tag
public class DownloadManager extends AsyncTask<Integer, Integer, Integer> {
    private DownloadManagerSettings settings;

    private ArrayList<DownloadDocument> downloadDocuments;
    private ArrayList<DownloadDocument> localDocuments;

    private OnListUpdateListener listener = null;
    private SharedPreferences preferences;
    private ProgressDialog progressDialog;

    public DownloadManager(String managerName, URL directoryURL, File directoryFile) {
        settings = new DownloadManagerSettings(managerName, directoryURL, directoryFile);
        preferences = MainActivity.getContext().getSharedPreferences(settings.managerName,
                Context.MODE_PRIVATE);
        downloadDocuments = loadDownloadDocuments();
        localDocuments = new ArrayList<>();
    }

    public DownloadManager(DownloadManagerSettings settings) {
        this.settings = settings;
        preferences = MainActivity.getContext().getSharedPreferences(settings.managerName,
                Context.MODE_PRIVATE);
        downloadDocuments = loadDownloadDocuments();
        localDocuments = new ArrayList<>();
    }

    public DownloadManager(DownloadManagerSettings settings,
                           ArrayList<DownloadDocument> downloadDocuments,
                           ArrayList<DownloadDocument> localDocuments,
                           OnListUpdateListener listener,
                           SharedPreferences preferences) {
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
        preferences = MainActivity.getContext().getSharedPreferences(settings.managerName,
                Context.MODE_PRIVATE);
    }

    /* Getter and setter functions */
    public DownloadManagerSettings getSettings() {
        return settings;
    }

    public void setMaximumPoints(int value) {
        Log.d("DownloadManager|" + settings.managerName, "setMaximumPoints:\n" + value);
        if (value <= 0) {
            settings.maximumPoints = 10;
        } else {
            settings.maximumPoints = value;
        }
    }

    public void setSheetRegex(String value) {
        Log.d("DownloadManager|" + settings.managerName, "setSheetRegex:\n" + value);
        if (value == null) {
            settings.sheetRegex = "";
        } else {
            settings.sheetRegex = value;
        }
    }

    public void setStickiedTitles(ArrayList<String> value) {
        String dataString = new Gson().toJson(value);
        Log.d("DownloadManager|" + settings.managerName, "setStickiedTitles:\n" + dataString);
        if (value == null) {
            settings.stickiedTitles = new ArrayList<>();
        } else {
            settings.stickiedTitles = value;
        }
    }

    public void setTitleMap(HashMap<String, String> value) {
        String dataString = new Gson().toJson(value);
        Log.d("DownloadManager|" + settings.managerName, "setTitleMap:\n" + dataString);
        if (value == null) {
            settings.titleMap = new HashMap<>();
        } else {
            settings.titleMap = value;
        }
    }


    /* Simple helper functions */
    public String getPointsText() {
        double sum_points = 0;
        int number_of_sheets = 0;
        for (DownloadDocument downloadDocument : localDocuments) {
            if (downloadDocument.getPoints() >= 0) {
                sum_points += downloadDocument.getPoints();
                number_of_sheets += 1;
            }
        }
        StringBuilder builder = new StringBuilder();
        builder.append(MainActivity.getContext().getString(R.string.points_view_prefix));
        builder.append(" ");
        if (number_of_sheets == 0) {
            builder.append("---");
        } else {
            double averagePoints = sum_points / number_of_sheets;
            double percentage = 100 * sum_points / number_of_sheets / settings.maximumPoints;

            builder.append(String.format(Locale.GERMAN, "%.1f", averagePoints));
            builder.append("/");
            builder.append(String.format(Locale.GERMAN, "%d", settings.maximumPoints));
            builder.append(" ~ ");
            builder.append(String.format(Locale.GERMAN, "%.0f", percentage));
            builder.append("%");
        }
        return builder.toString();
    }

    private File urlToFile(URL url) {
        return new File(settings.getDirectory(), new File(url.getPath()).getName());
    }

    private int suggestionToSheetNumber(String titleSuggestion) {
        Pattern pattern = Pattern.compile(settings.sheetRegex);
        Matcher matcher = pattern.matcher(titleSuggestion);
        if (matcher.matches() && matcher.groupCount() >= 1) {
            return Integer.valueOf(matcher.group(1));
        } else {
            return -1;
        }
    }

    private String suggestionToTitle(String titleSuggestion, int sheetNumber) {
        if (settings.titleMap.containsKey(titleSuggestion)) {
            return settings.titleMap.get(titleSuggestion);
        } else if (sheetNumber >= 0) {
            return MainActivity.getContext().getString(R.string.sheet_title_format, sheetNumber);
        } else {
            return titleSuggestion;
        }
    }

    private DownloadDocument linkElementToDownloadDocument(Element link)
            throws MalformedURLException {
        URL linkURL = new URL(link.attr("abs:href"));
        File linkFile = urlToFile(linkURL);
        String titleSuggestion = link.text();
        int sheetNumber = suggestionToSheetNumber(titleSuggestion);
        String title = suggestionToTitle(titleSuggestion, sheetNumber);
        return new DownloadDocument(linkURL, linkFile, titleSuggestion, title, sheetNumber);
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
            if (settings.stickiedTitles.contains(dd.title)) {
                stickied.put(settings.stickiedTitles.indexOf(dd.title), dd);
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
        Log.d("DownloadManager", "saveDownloadDocuments:\n" + dataString);
        editor.apply();
    }

    private ArrayList<DownloadDocument> loadDownloadDocuments() {
        Type collectionType = new TypeToken<ArrayList<DownloadDocument>>() {
        }.getType();
        String dataString = preferences.getString("documents", "[]");
        //Log.d("DownloadManager", "loadDownloadDocuments:\n" + dataString);
        return new Gson().fromJson(dataString, collectionType);
    }

    /* Functions for scanning and downloading */
    public void localScan() {
        localDocuments.clear();
        for (DownloadDocument dd : downloadDocuments) {
            if (dd.file.exists()) {
                localDocuments.add(dd);
            }
        }
        notifyListener();
    }

    public void download() {
        execute();
    }

    private void openProgressbar() {
        Log.d("DownloadManager", "openProgressbar");
        progressDialog = new ProgressDialog(MainActivity.getContext());
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
        Context context = MainActivity.getContext();
        if (progress[0] == -1) {
            // Downloading the index file
            progressDialog.setMessage(context.getString(R.string.download_message_index_file));
            progressDialog.setProgress(100);
        } else {
            progressDialog.setMessage(context.getString(R.string.download_message,
                    downloadDocuments.get(progress[0]).title));
            progressDialog.setProgress(progress[1]);
        }
    }

    private void downloadDocument(int index) throws IOException {
        publishProgress(index, 0);
        DownloadDocument currentFile = downloadDocuments.get(index);
        currentFile.file.getParentFile().mkdirs();
        URLConnection connection = currentFile.url.openConnection();
        //TODO: Remove this line
        if (currentFile.url.toString().equals("http://www.math.uni-bonn.de/people/gjasso/auth/LA_ss18.pdf")) {
            String userCredentials = "v1g4:frobenius";
            String basicAuth = "Basic " + Base64.encodeToString(userCredentials.getBytes(), Base64.NO_WRAP);
            connection.setRequestProperty("Authorization", basicAuth);
        }
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

    private void addDownloadDocumentByLinkElement(Element link) throws IOException {
        //Create download Document
        DownloadDocument dd = linkElementToDownloadDocument(link);
        HttpURLConnection connection = (HttpURLConnection) dd.url.openConnection();
        //TODO: Remove this line
        if (link.attr("href").equals("http://www.math.uni-bonn.de/people/gjasso/auth/LA_ss18.pdf")) {
            String userCredentials = "v1g4:frobenius";
            String basicAuth = "Basic " + Base64.encodeToString(userCredentials.getBytes(), Base64.NO_WRAP);
            connection.setRequestProperty("Authorization", basicAuth);
        }
        connection.setRequestMethod("HEAD");
        Log.d("DownloadManager", "Response code: " + connection.getResponseCode());
        //Check if it can be downloaded
        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            //Set date from Last-Modified header
            Date lastModified = new Date(connection.getLastModified());
            dd.setDate(lastModified);

            //Set points from local files (if known)
            for (DownloadDocument ld : localDocuments) {
                if (ld.equals(dd)) { //Compares urls
                    dd.setPoints(ld.getPoints());
                    break;
                }
            }
            downloadDocuments.add(dd);
        }

    }

    private void updateDownloadDocuments() throws IOException {
        publishProgress(-1);
        downloadDocuments.clear();
        Document htmlDocument = Jsoup.connect(settings.directoryURL.toString()).get();
        for (Element link : htmlDocument.getElementsByAttributeValueEnding("href", ".pdf")) {
            Log.d("DownloadManager", "Link found: " + link.attr("href"));
            addDownloadDocumentByLinkElement(link);
        }
        sortDownloadDocuments();
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
            Log.d("DownloadManager", "Download failed. Skipping the rest");
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
