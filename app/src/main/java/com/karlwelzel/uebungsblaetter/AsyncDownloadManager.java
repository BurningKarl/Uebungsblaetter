package com.karlwelzel.uebungsblaetter;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

/**
 * Created by karl on 19.10.17.
 */

public class AsyncDownloadManager extends AsyncTask<DownloadFile, Integer, DownloadFile> {

    private Context context;
    private List<DownloadFile> urlList;
    private String formatUrl;
    private String formatPath;
    private OnDownloadFinishedListener downloadListener;
    private OnFileFoundListener fileListener;

    boolean completeDownloadInProcess = false; // Bad solution, to ensure that startDownload gets called

    int downloadFileIndex = 0;
    int downloadFileNumber = 1;

    private DownloadFile currentDownloadFile;
    private ProgressDialog progressDialog;


    public AsyncDownloadManager(Context context, List<DownloadFile> urlList, String formatUrl,
                                String formatPath, OnDownloadFinishedListener downloadListener,
                                OnFileFoundListener fileListener) {
        /*
         * context: Context of the current application
         * urlList: List of urls to download and paths to store the files
         * formatUrl: A url template that generates urls to download
         * formatPath: A path template that generates a path where the files should be stored
         * listener: A listener that gets called every time a Download finishes successfully
         */
        this.context = context;
        this.urlList = urlList;
        this.formatUrl = formatUrl;
        this.formatPath = formatPath;
        this.downloadListener = downloadListener;
        this.fileListener = fileListener;
    }

    private DownloadFile nextFile() {
        Log.d("DownloadManager", "nextFile");
        /*
         * Gives back the next file that should be downloaded
         */
        DownloadFile df;
        if (downloadFileIndex < urlList.size()) {
            df = urlList.get(0);
            downloadFileIndex++;
        } else {
            df = new DownloadFile(String.format(formatUrl, downloadFileNumber),
                    new File(String.format(formatPath, downloadFileNumber)),
                    String.format(context.getResources().getString(R.string.sheet_name_format),
                            downloadFileNumber));
            downloadFileNumber += 1;
        }
        return df;
    }

    private void goToPreviousFile() {
        Log.d("DownloadManager", "goToPreviousFile");
        /*
         * Shifts nextFile() back to a previous file
         */
        if (downloadFileNumber > 1) {
            downloadFileNumber--;
        } else if (downloadFileIndex > 0){
            downloadFileIndex--;
        }
    }

    public void completeScan() {
        Log.d("DownloadManager", "completeScan");
        /*
         * Scans for all documents that should be downloaded and checks whether they exist on the
         * local storage. If so they are skipped and will not be downloaded again.
         */
        DownloadFile df = nextFile();
        while (df.file.exists()) {
            fileListener.onFileFound(df);
            df = nextFile();
        }
        goToPreviousFile();
    }

    public void completeDownload() {
        Log.d("DownloadManager", "completeDownload");
        completeScan();
        completeDownloadInProcess = true;
        this.execute(nextFile());
    }

    private void openProgressbar() {
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
        progressDialog.setMessage(String.format(context.getResources().getString(R.string.download_message),
                currentDownloadFile.getTitle()));
        progressDialog.setProgress(progress[0]);
    }

    @Override
    protected DownloadFile doInBackground(DownloadFile... params) {
        currentDownloadFile = params[0];

        boolean error_occurred = false;
        try {
            publishProgress(0);
            URL url = new URL(currentDownloadFile.url);
            currentDownloadFile.file.getParentFile().mkdirs();
            URLConnection connection = url.openConnection();
            connection.connect();
            int lengthOfFile = connection.getContentLength();
            InputStream input = new BufferedInputStream(connection.getInputStream());
            OutputStream output = new FileOutputStream(currentDownloadFile.file);
            byte data[] = new byte[1024];
            int count;
            long total = 0;
            while (progressDialog.isShowing() && !isCancelled() && (count = input.read(data)) != -1) {
                total += count;
                publishProgress((int) (total * 100) / lengthOfFile);
                output.write(data, 0, count);
            }
            output.flush();
            output.close();
            input.close();
        } catch (FileNotFoundException e) {
            error_occurred = true;
        } catch (Exception e) {
            Log.e("Error: ", e.getMessage());
            e.printStackTrace();
            error_occurred = true;
        }

        if (!progressDialog.isShowing() || error_occurred) {
            currentDownloadFile.file.delete();
            return null;
        } else {
            return currentDownloadFile;
        }
    }

    public AsyncDownloadManager copy() {
        return new AsyncDownloadManager(context, urlList, formatUrl, formatPath, downloadListener,
                fileListener);
    }

    public void setState(int downloadFileIndex, int downloadFileNumber) {
        this.downloadFileIndex = downloadFileIndex;
        this.downloadFileNumber = downloadFileNumber;
    }

    public void reset() {
        downloadFileIndex = 0;
        downloadFileNumber = 1;
    }

    @Override
    protected void onPostExecute(DownloadFile result) {
        progressDialog.cancel();
        if (result != null) {
            downloadListener.onDownloadFinished(result);
            if (completeDownloadInProcess) {
                AsyncDownloadManager asyncDownloadManager = copy();
                asyncDownloadManager.setState(downloadFileIndex, downloadFileNumber);
                asyncDownloadManager.completeDownload();
            }
        } else {
            if (completeDownloadInProcess) {
                completeDownloadInProcess = false;
                //goToPreviousFile(); // Unnoetig
                downloadListener.onCompleteDownloadFinished();
            }
        }
    }

    public interface OnDownloadFinishedListener {
        void onDownloadFinished(DownloadFile result);
        void onCompleteDownloadFinished();
    }

    public interface OnFileFoundListener {
        void onFileFound(DownloadFile result);
    }
}
