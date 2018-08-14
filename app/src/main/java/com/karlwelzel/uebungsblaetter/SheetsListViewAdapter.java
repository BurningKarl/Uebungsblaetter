package com.karlwelzel.uebungsblaetter;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
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
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by karl on 15.10.17.
 */

public class SheetsListViewAdapter extends ArrayAdapter<DownloadDocument>
        implements DownloadManager.OnListUpdateListener {

    private static final int itemLayoutId = R.layout.sheet_listview_item;
    private final TextView pointsView;
    private DownloadManager manager;
    private SwipeRefreshLayout swipeRefreshLayout = null;
    private OnManagerChangedListener listener = null;

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

    // Opens a dialog to edit the DownloadDocument
    private void openDownloadDocumentSettings(final DownloadDocument dd) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View dialogView = inflater.inflate(R.layout.dialog_download_document_settings, null);
        final TextInputEditText newPointsInput = dialogView.findViewById(R.id.new_points_input);
        String pointsString = Double.toString(dd.getPoints());
        Log.d("SheetsListViewAdapter", "Points text is " + pointsString);
        if (dd.getPoints() >= 0) {
            newPointsInput.setText(Double.toString(dd.getPoints()));
        }
        final TextInputEditText newTitleInput = dialogView.findViewById(R.id.new_title_input);
        newTitleInput.setText(dd.title);
        new AlertDialog.Builder(getContext())
                .setTitle(dd.title)
                .setView(dialogView)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int which) {
                                //points
                                try {
                                    String inputText = newPointsInput.getText().toString();
                                    dd.setPoints(Double.parseDouble(inputText));
                                } catch (NumberFormatException e) {
                                    Log.d("SheetsListViewAdapter", "Input is not a number -> points deleted");
                                    dd.setPoints(-1);
                                }
                                //titleMap
                                String newTitle = newTitleInput.getText().toString();
                                if (!newTitle.equals(dd.title)) {
                                    dd.title = newTitle;
                                    manager.getTitleMap().put(dd.titleId, newTitle);
                                }
                                notifyDataSetChanged();
                                manager.saveDownloadDocuments();
                            }
                        })
                .show();
    }

    // Opens a dialog to edit the DownloadManager
    public void openDownloadManagerSettings() {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View dialogView = inflater.inflate(R.layout.dialog_download_manager_settings, null);
        final TextInputEditText newNameInput = dialogView.findViewById(R.id.new_name_input);
        newNameInput.setText(manager.getName());
        final TextInputEditText newUrlInput = dialogView.findViewById(R.id.new_url_input);
        newUrlInput.setText(manager.getDirectoryURL().toString());
        final TextInputEditText newMaximumPointsInput = dialogView.findViewById(R.id.new_maximum_points_input);
        newMaximumPointsInput.setText(Integer.toString(manager.getMaximumPoints()));
        final TextInputEditText newSheetRegexInput = dialogView.findViewById(R.id.new_sheet_regex_input);
        newSheetRegexInput.setText(manager.getSheetRegex());
        new AlertDialog.Builder(getContext())
                .setTitle(manager.getName())
                .setView(dialogView)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int which) {
                                //name
                                boolean nameChanged = false;
                                String newName = newNameInput.getText().toString();
                                if (!newName.equals(manager.getName())) {
                                    Log.d("SheetsListViewAdapter", "Name changed");
                                    manager.setName(newName);
                                    nameChanged = true;
                                }
                                //url
                                boolean urlChanged = false;
                                try {
                                    String inputText = newUrlInput.getText().toString();
                                    URL newUrl = new URL(inputText);
                                    if (!newUrl.equals(manager.getDirectoryURL())) {
                                        Log.d("SheetsListViewAdapter", "URL changed");
                                        manager.setDirectoryURL(newUrl);
                                        urlChanged = true;
                                    }
                                } catch (MalformedURLException e) {
                                    Log.d("SheetsListViewAdapter", "Input is not a valid url");
                                    Snackbar.make(MainActivity.contentView,
                                            R.string.not_a_valid_url, Snackbar.LENGTH_SHORT)
                                            .show();
                                }
                                //maximumPoints
                                boolean maximumPointsChanged = false;
                                try {
                                    String inputText = newMaximumPointsInput.getText().toString();
                                    int newMaximumPoints = Integer.parseInt(inputText);
                                    if (newMaximumPoints != manager.getMaximumPoints()) {
                                        Log.d("SheetsListViewAdapter", "Maximum points changed");
                                        manager.setMaximumPoints(newMaximumPoints);
                                        maximumPointsChanged = true;
                                    }
                                } catch (NumberFormatException e) {
                                    Log.d("SheetsListViewAdapter", "Input is not a number");
                                    Snackbar.make(MainActivity.contentView,
                                            R.string.not_a_valid_number, Snackbar.LENGTH_SHORT)
                                            .show();
                                }
                                //sheetRegex
                                boolean sheetRegexChanged = false;
                                String newSheetRegex = newSheetRegexInput.getText().toString();
                                if (!newSheetRegex.equals(manager.getSheetRegex())) {
                                    Log.d("SheetsListViewAdapter", "SheetRegex changed");
                                    manager.setSheetRegex(newSheetRegex);
                                    sheetRegexChanged = true;
                                }

                                if (urlChanged) {
                                    notifyListener(true);
                                } else if (maximumPointsChanged || sheetRegexChanged || nameChanged) {
                                    notifyListener(false);
                                }
                            }
                        })
                .show();
    }

    /* DownloadManager interactions */
    public void completeScan() {
        Log.d("SheetsListViewAdapter", "completeScan");
        if (!scanFinished) {
            clear();
            manager.localScan();
            scanFinished = true;
        }
    }

    public void completeDownload(SwipeRefreshLayout layout) {
        Log.d("SheetsListViewAdapter", "completeDownload");
        swipeRefreshLayout = layout;
        clear();
        manager.download();
    }

    public void completeDownloadOffline(SwipeRefreshLayout layout) {
        Log.d("SheetsListViewAdapter", "completeDownloadOffline");
        swipeRefreshLayout = layout;
        clear();
        manager.downloadOffline();
    }

    public void onListUpdate(DownloadDocument... files) {
        this.addAll(files);
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(false);
            swipeRefreshLayout = null;
        }
        Log.d("SheetsListViewAdapter", "manager status " + manager.getStatus().name());
        if (!manager.getStatus().equals(AsyncTask.Status.PENDING)) {
            // This is necessary to call execute a second time.
            manager = manager.copy();
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
                    Log.d("SheetsListViewAdapter", "Opened: " + dd.title);
                    openPDFDocument(dd.file);
                }
            });
            textLayout.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    DownloadDocument dd = (DownloadDocument) v.getTag(R.id.file_tag);
                    Log.d("SheetsListViewAdapter", "Opened settings: " + dd.title);
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

    /* Listener stuff */
    public void setListener(OnManagerChangedListener listener) {
        this.listener = listener;
    }

    private void notifyListener(boolean downloadNecessary) {
        if (listener != null) {
            listener.onManagerChanged(downloadNecessary);
        } else {
            Log.e("DownloadManager", "An OnManagerChangedListener should have been set");
        }
    }

    interface OnManagerChangedListener {
        void onManagerChanged(boolean downloadNecessary);
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
