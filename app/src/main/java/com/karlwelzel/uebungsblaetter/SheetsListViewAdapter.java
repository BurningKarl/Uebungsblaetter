package com.karlwelzel.uebungsblaetter;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputEditText;
import android.support.v4.content.FileProvider;
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

/**
 * Created by karl on 15.10.17.
 */

public class SheetsListViewAdapter extends ArrayAdapter<DownloadDocument>
        implements DownloadManager.OnListUpdateListener {

    private static final int itemLayoutId = R.layout.sheet_listview_item;
    private final TextView pointsView;
    private DownloadManager manager;
    private SwipeRefreshLayout swipeRefreshLayout = null;

    private boolean scanFinished = false;

    public SheetsListViewAdapter(@NonNull Context context, TextView pointsView,
                                 DownloadManager manager) {
        super(context, itemLayoutId);
        this.pointsView = pointsView;
        this.manager = manager;
        this.manager.setListener(this);
        registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                updatePointsViewText();
            }
        });
    }

    /* Helper functions */
    public void updatePointsViewText() {
        pointsView.setText(manager.getPointsText());
    }

    private void openPDFDocument(File file) {
        /*MimeTypeMap myMime = MimeTypeMap.getSingleton();
                String mimeType = myMime.getMimeTypeFromExtension("pdf");*/
        Intent newIntent = new Intent(Intent.ACTION_VIEW);
        Uri uriForFile;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            uriForFile = FileProvider.getUriForFile(getContext(),
                    getContext().getApplicationContext().getPackageName() + ".provider", file);
            newIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            uriForFile = Uri.fromFile(file);
        }
        newIntent.setDataAndType(uriForFile, "application/pdf");
        newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            getContext().startActivity(newIntent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(getContext(), "No handler for this type of file.", Toast.LENGTH_LONG).show();
        }
    }

    private void openDownloadDocumentSettings(final DownloadDocument dd) {
        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setTitle(dd.title)
                .setView(R.layout.dialog_downloaddocument_settings)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                try {
                                    TextInputEditText newPointsInput = ((Dialog) dialog).findViewById(R.id.new_points_input);
                                    String inputText = newPointsInput.getText().toString();
                                    dd.setPoints(Double.parseDouble(inputText));
                                } catch (NumberFormatException e) {
                                    Log.d("ListViewAdapter", "Input is not a number -> points deleted");
                                    dd.setPoints(-1);
                                }
                                notifyDataSetChanged();
                                manager.saveDownloadDocuments();
                            }
                        })
                .create();
        //Access the elements in R.layout.dialog_downloaddocument_settings here
        dialog.show();
    }

    /* DownloadManager interactions */
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
    }

    public void onListUpdate(DownloadDocument... files) {
        this.addAll(files);
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(false);
            swipeRefreshLayout = null;
        }
    }

    /* The heart of the adapter */
    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
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
                    DownloadDocument dd = (DownloadDocument) v.getTag(R.id.file_tag);
                    Log.d("ListViewAdapter", "Opened: " + dd.title);
                    openPDFDocument(dd.file);
                }
            });
            textLayout.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    DownloadDocument dd = (DownloadDocument) v.getTag(R.id.file_tag);
                    Log.d("ListViewAdapter", "Opened settings: " + dd.title);
                    openDownloadDocumentSettings(dd);
                    return true;
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
        subtitleView.setText(downloadDocument.getSubtitle());

        // Return the completed view to render on screen
        return convertView;
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
