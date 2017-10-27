package com.karlwelzel.uebungsblaetter;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TwoLineListItem;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Created by karl on 15.10.17.
 */

public class SheetsListViewAdapter extends ArrayAdapter<ExerciseSheet>
        implements AsyncDownloadManager.OnDownloadFinishedListener,
        AsyncDownloadManager.OnFileFoundListener{

    private static final String PREFS_NAME = "UEBUNGSBLAETTER";
    private static final String DIRECTORY_NAME = "Uebungsblaetter";

    private final Context context;
    private final SharedPreferences preferences;

    private static final int itemLayoutId = R.layout.sheet_listview_item;
    private AsyncDownloadManager downloadManager;

    private SwipeRefreshLayout swipeRefreshLayout = null;

    private boolean scanFinished = false;

    public SheetsListViewAdapter(@NonNull Context context, List<String> urlList, String formatUrl) {
        super(context, itemLayoutId);
        this.context = context;
        preferences = context.getSharedPreferences(PREFS_NAME, 0);
        File dirPath = new File(Environment.getExternalStorageDirectory(), DIRECTORY_NAME);
        List<DownloadFile> urlList1 = new ArrayList<>();
        for (int i = 0; i < urlList.size(); i++) {
            if (i == 0) {
                urlList1.add(new DownloadFile(urlList.get(i),
                        new File(dirPath, urlList.get(i).substring(urlList.get(i).lastIndexOf("/")+1)),
                        "Skript"));
            } else {
                urlList1.add(new DownloadFile(urlList.get(i),
                        new File(dirPath, urlList.get(i).substring(urlList.get(i).lastIndexOf("/")+1))));
            }
        }
        String formatPath = dirPath + "/" + formatUrl.substring(formatUrl.lastIndexOf("/") + 1);
        downloadManager = new AsyncDownloadManager(context, urlList1, formatUrl, formatPath,
                this, this);
    }

    @SuppressLint("StringFormatInvalid")
    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        Log.d("getView", "getView was called.");
        // Get the data item for this position
        final ExerciseSheet sheet = getItem(position);

        /*
         * TODO: Replace the dialog for picking a date with a dialog for picking the score
         * The user could save his or her score and the score that could have been achieved for a
         * specific exercise sheet. Then next step after that would be to introduce a new bar at
         * the bottom to show the average score for all sheets.
         */

        TextView titleView, subtitleView;
        TwoLineListItem textLayout;
        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(itemLayoutId, parent, false);
            textLayout = convertView.findViewById(R.id.textLayout);
            titleView = convertView.findViewById(R.id.titleText);
            subtitleView = convertView.findViewById(R.id.subtitleText);
            textLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ExerciseSheet sheet = (ExerciseSheet) v.getTag();
                    Log.d("onClick", "Opened: " + sheet.title);
                    openPDFDocument(sheet.file);
                }
            });

            textLayout.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    final ExerciseSheet sheet = (ExerciseSheet) v.getTag();
                    Log.d("onLongClick", "Opened: " + sheet.title);
                    final Calendar calendar = Calendar.getInstance();
                    new DatePickerDialog(context, new DatePickerDialog.OnDateSetListener() {
                        @Override
                        public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                            calendar.set(year, month, dayOfMonth);
                            sheet.date = calendar.getTime();
                            SharedPreferences.Editor editor = preferences.edit();
                            editor.putLong(sheet.file.toString(), sheet.date.getTime());
                            editor.apply();
                            notifyDataSetChanged();
                        }
                    }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)).show();
                    return true;
                }
            });


        } else {
            // Lookup view for data population
            textLayout = convertView.findViewById(R.id.textLayout);
            titleView = convertView.findViewById(R.id.titleText);
            subtitleView = convertView.findViewById(R.id.subtitleText);
        }
        // Populate the data into the template view using the data object
        titleView.setText(sheet.title);
        if (sheet.date != null) {
            subtitleView.setText(String.format(context.getResources()
                    .getString(R.string.date_message_format), sheet.date));
        } else {
            subtitleView.setText(context.getResources().getString(R.string.no_date_message));
        }
        textLayout.setTag(sheet);
        // Return the completed view to render on screen
        return convertView;
    }

    private void openPDFDocument(File file) {
        /*MimeTypeMap myMime = MimeTypeMap.getSingleton();
                String mimeType = myMime.getMimeTypeFromExtension("pdf");*/
        Intent newIntent = new Intent(Intent.ACTION_VIEW);
        newIntent.setDataAndType(Uri.fromFile(file), "application/pdf");
        newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(newIntent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, "No handler for this type of file.", Toast.LENGTH_LONG).show();
        }
    }

    public void completeScan() {
        Log.d("ListViewAdapter", "completeScan");
        if (!scanFinished) {
            clear();
            downloadManager.completeScan();
            downloadManager.reset();
            scanFinished = true;
        }
    }

    public void completeDownload(SwipeRefreshLayout layout) {
        Log.d("ListViewAdapter", "completeScan");
        swipeRefreshLayout = layout;
        clear();
        downloadManager.completeDownload();
    }

    @Override
    public void onDownloadFinished(DownloadFile result) {
        Log.d("ListViewAdapter", "onDownloadFinished");
        onFileFound(result);
    }

    @Override
    public void onCompleteDownloadFinished() {
        Log.d("ListViewAdapter", "onCompleteDownloadFinished");
        swipeRefreshLayout.setRefreshing(false);
        swipeRefreshLayout = null;
        downloadManager = downloadManager.copy();
    }

    @Override
    public void onFileFound(DownloadFile result) {
        Log.d("ListViewAdapter", "onFileFound");
        long time = preferences.getLong(result.file.toString(), -1);
        Date date;
        if (time != -1) {
            date = new Date(time);
        } else {
            date = null;
        }
        add(new ExerciseSheet(result.file, result.title, date));
    }
}
