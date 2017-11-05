package com.karlwelzel.uebungsblaetter;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TwoLineListItem;

import java.io.File;
import java.text.DateFormat;
import java.util.Locale;

/**
 * Created by karl on 15.10.17.
 */

public class SheetsListViewAdapter extends ArrayAdapter<DownloadDocument>
        implements DownloadManager.OnListUpdateListener {

    private static final int itemLayoutId = R.layout.sheet_listview_item;
    private final Context context;
    private DownloadManager manager;
    private SwipeRefreshLayout swipeRefreshLayout = null;

    private boolean scanFinished = false;

    public SheetsListViewAdapter(@NonNull Context context, DownloadManager manager) {
        super(context, itemLayoutId);
        this.context = context;
        this.manager = manager;
        manager.setListener(this);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        /*
         * TODO: Add a dialog for picking the score
         * The user could save his or her score and the score that could have been achieved for a
         * specific exercise sheet. Then next step after that would be to introduce a new bar at
         * the bottom to show the average score for all sheets.
         * Also the subtitleView could be used to display the upload date.
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
                    DownloadDocument df = (DownloadDocument) v.getTag(R.id.file_tag);
                    Log.d("onClick", "Opened: " + df.title);
                    openPDFDocument(df.file);
                }
            });
            ViewHolder viewHolder = new ViewHolder(textLayout, titleView, subtitleView);
            convertView.setTag(R.id.viewholder_tag, viewHolder);
        } else {
            // Lookup view for data population
            ViewHolder viewHolder = (ViewHolder) convertView.getTag(R.id.viewholder_tag);
            textLayout = viewHolder.textLayout;
            titleView = viewHolder.titleView;
            subtitleView = viewHolder.subtitleView;
        }

        // Get the data item for this position
        DownloadDocument downloadDocument = getItem(position);

        // Populate the data into the template view using the data object
        titleView.setText(downloadDocument.title);
        textLayout.setTag(R.id.file_tag, downloadDocument);
        if (downloadDocument.getDate() != null) {
            DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT,
                    DateFormat.SHORT, Locale.GERMAN);
            subtitleView.setText(dateFormat.format(downloadDocument.getDate()));
        }

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
            manager.localScan();
            scanFinished = true;
        }
    }

    public void completeDownload(SwipeRefreshLayout layout) {
        Log.d("ListViewAdapter", "completeDownload");
        swipeRefreshLayout = layout;
        clear();
        manager.download();
        // This is necessary to call execute a second time.
        manager = manager.copy();
        Log.d("ListViewAdapter", "New Generator class: " + manager.getClass().toString());
    }

    public void onListUpdate(DownloadDocument... files) {
        this.addAll(files);
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(false);
            swipeRefreshLayout = null;
        }
    }

    private static final class ViewHolder {
        private final TwoLineListItem textLayout;
        private final TextView titleView, subtitleView;

        public ViewHolder(TwoLineListItem textLayout, TextView titleView, TextView subtitleView) {
            this.textLayout = textLayout;
            this.titleView = titleView;
            this.subtitleView = subtitleView;
        }
    }
}
