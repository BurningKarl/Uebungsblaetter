package com.karlwelzel.uebungsblaetter;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
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
    private String managerName;
    private URL directoryURL;
    private File directoryFile;
    private Integer maximumPoints;

    private ArrayList<DownloadDocument> downloadDocuments;
    private ArrayList<DownloadDocument> localFiles;

    private OnListUpdateListener listener = null;
    private SharedPreferences preferences;
    private ProgressDialog progressDialog;

    public DownloadManager(String managerName, URL directoryURL,
                           File directoryFile, Integer maximumPoints) {
        this.managerName = managerName;
        this.directoryURL = directoryURL;
        this.directoryFile = directoryFile;
        this.maximumPoints = maximumPoints;
        preferences = MainActivity.getContext().getSharedPreferences(managerName, Context.MODE_PRIVATE);
        downloadDocuments = loadDownloadDocuments();
        localFiles = new ArrayList<>();
    }

    public DownloadManager(String managerName, URL directoryURL, File directoryFile,
                           Integer maximumPoints, ArrayList<DownloadDocument> downloadDocuments,
                           ArrayList<DownloadDocument> localFiles,
                           OnListUpdateListener listener, SharedPreferences preferences) {
        this.managerName = managerName;
        this.directoryURL = directoryURL;
        this.directoryFile = directoryFile;
        this.maximumPoints = maximumPoints;
        this.downloadDocuments = downloadDocuments;
        this.localFiles = localFiles;
        this.listener = listener;
        this.preferences = preferences;
    }

    public DownloadManager copy() {
        return new DownloadManager(managerName, directoryURL, directoryFile, maximumPoints,
                downloadDocuments, localFiles, listener, preferences);
    }

    /* Simple helper functions */
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
        builder.append(MainActivity.getContext().getString(R.string.points_view_prefix));
        builder.append(" ");
        if (number_of_sheets == 0) {
            builder.append("---");
        } else {
            builder.append(String.format(Locale.GERMAN, "%.1f",
                    sum_points / number_of_sheets));
            builder.append("/");
            builder.append(String.format(Locale.GERMAN, "%d", maximumPoints));
            builder.append(" ~ ");
            builder.append(String.format(Locale.GERMAN, "%.0f",
                    100 * sum_points / number_of_sheets / maximumPoints));
            builder.append("%");
        }
        return builder.toString();
    }

    private File urlToFile(URL url) {
        return new File(directoryFile, new File(url.getPath()).getName());
    }

    private DownloadDocument linkElementToDownloadDocument(Element link)
            throws MalformedURLException {
        URL linkURL = new URL(link.attr("abs:href"));
        File linkFile = urlToFile(linkURL);
        return new DownloadDocument(linkURL, linkFile, getTitle(linkURL, link.text()));
    }

    private String getTitle(URL url, String titleSuggestion) {
        // This function is supposed to return the title of a DownloadDocument based on the url
        // and the path to the file.
        Pattern pattern = Pattern.compile(getSheetRegex());
        Matcher matcher = pattern.matcher(titleSuggestion);
        HashMap<String, String> titleMap = getTitleMap();
        if (titleMap.containsKey(titleSuggestion)) {
            return titleMap.get(titleSuggestion);
        } else if (matcher.matches()) {
            Integer number = Integer.valueOf(matcher.group(1));
            return MainActivity.getContext().getString(R.string.sheet_title_format, number);
        } else {
            return titleSuggestion;
        }
    }

    public String getSheetRegex() {
        return preferences.getString("sheetRegex", "");
    }

    public void setSheetRegex(String value) {
        Log.d("DownloadManager|" + managerName, "setSheetRegex:\n" + value);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("sheetRegex", value);
        editor.apply();
    }

    public ArrayList<String> getStickiedDocuments() {
        Type collectionType = new TypeToken<ArrayList<String>>() {
        }.getType();
        String dataString = preferences.getString("stickiedDocuments", "[]");
        return new Gson().fromJson(dataString, collectionType);
    }

    public void setStickiedDocuments(ArrayList<String> value) {
        SharedPreferences.Editor editor = preferences.edit();
        String dataString = new Gson().toJson(value);
        Log.d("DownloadManager|" + managerName, "setStickiedDocuments:\n" + dataString);
        editor.putString("stickiedDocuments", dataString);
        editor.apply();
    }

    public HashMap<String, String> getTitleMap() {
        Type collectionType = new TypeToken<HashMap<String, String>>() {
        }.getType();
        String dataString = preferences.getString("titleMap", "{}");
        return new Gson().fromJson(dataString, collectionType);
    }

    public void setTitleMap(HashMap<String, String> value) {
        SharedPreferences.Editor editor = preferences.edit();
        String dataString = new Gson().toJson(value);
        Log.d("DownloadManager|" + managerName, "setTitleMap:\n" + dataString);
        editor.putString("titleMap", dataString);
        editor.apply();
    }

    private void sortDownloadDocuments() {
        // The items in downloadDocuments and their order can be manipulated here
        Comparator<DownloadDocument> dateComparator = new Comparator<DownloadDocument>() {
            @Override
            public int compare(DownloadDocument doc1, DownloadDocument doc2) {
                return doc2.getDate().compareTo(doc1.getDate()); //reversed
            }
        };
        SparseArray<DownloadDocument> stickied = new SparseArray<>();
        SparseArray<DownloadDocument> sheets = new SparseArray<>();
        ArrayList<DownloadDocument> leftover = new ArrayList<>();

        ArrayList<String> stickiedTitles = getStickiedDocuments();
        Pattern pattern = Pattern.compile(MainActivity.getContext().getString(R.string.sheet_title_regex));
        Matcher matcher;
        for (DownloadDocument dd : downloadDocuments) {
            matcher = pattern.matcher(dd.title);
            if (stickiedTitles.contains(dd.title)) {
                stickied.put(stickiedTitles.indexOf(dd.title), dd);
            } else if (matcher.matches()) {
                sheets.put(Integer.parseInt(matcher.group(1)), dd);
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
        Log.d("DownloadManager", "loadDownloadDocuments:\n" + dataString);
        return new Gson().fromJson(dataString, collectionType);
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

    private void updateDownloadDocuments() throws IOException {
        publishProgress(-1);
        downloadDocuments.clear();
        Document htmlDocument = Jsoup.connect(directoryURL.toString()).get();
        for (Element link : htmlDocument.getElementsByAttributeValueEnding("href", ".pdf")) {
            Log.d("DownloadManager", "Link found: " + link.attr("href"));
            //Create download Document
            DownloadDocument dd = linkElementToDownloadDocument(link);
            HttpURLConnection connection = (HttpURLConnection) dd.url.openConnection();
            connection.setRequestMethod("HEAD");
            Log.d("DownloadManager", "Response code: " + connection.getResponseCode());
            //Check if it can be downloaded
            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                //Set date from Last-Modified header
                Date lastModified = new Date(connection.getLastModified());
                dd.setDate(lastModified);
                Log.d("DownloadManager", "Date set: " + lastModified);

                //Set points from local files (if known)
                for (DownloadDocument ld : localFiles) {
                    if (ld.equals(dd)) { //Compares urls
                        dd.setPoints(ld.getPoints());
                        Log.d("DownloadManager", "Points set: " + ld.getPoints());
                        break;
                    }
                }
                downloadDocuments.add(dd);
            }
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
            DownloadDocument[] array = new DownloadDocument[localFiles.size()];
            listener.onListUpdate(localFiles.toArray(array));
        } else {
            Log.e("DownloadManager", "An OnListUpdateListener should have been set");
        }
    }

    interface OnListUpdateListener {
        void onListUpdate(DownloadDocument... files);
    }
}
