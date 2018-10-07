package com.karlwelzel.uebungsblaetter;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.content.FileProvider;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 * Created by karl on 15.10.17.
 */

public class DownloadDocumentsAdapter extends ArrayAdapter<DownloadDocument>
        implements DownloadManager.OnListUpdateListener,
        DialogManager.OnDownloadDocumentSettingsChangedListener,
        DialogManager.OnDownloadManagerSettingsChangedListener {

    private static final int ITEM_LAYOUT_ID = R.layout.sheet_listview_item;

    private final CoordinatorLayout contentLayout;
    private final TextView pointsView;
    private DownloadManager manager;
    private SwipeRefreshLayout swipeRefreshLayout = null;
    private final OnManagerChangedListener managerChangedListener;
    private final OnDownloadRequestedListener downloadRequestedListener;

    public DownloadDocumentsAdapter(
            @NonNull MainActivity activity, TextView pointsView, DownloadManager manager,
            @NonNull OnManagerChangedListener managerChangedListener,
            @NonNull OnDownloadRequestedListener downloadRequestedListener) {
        super(activity, ITEM_LAYOUT_ID);
        this.contentLayout = activity.findViewById(R.id.contentLayout);
        this.pointsView = pointsView;
        this.manager = manager;
        this.managerChangedListener = managerChangedListener;
        this.downloadRequestedListener = downloadRequestedListener;

        manager.setAdapter(this);
        registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                updatePointsViewText();
            }
        });
    }

    /* Getter and setter methods */
    public CoordinatorLayout getContentLayout() {
        return contentLayout;
    }

    public DownloadManager getManager() {
        return manager;
    }

    /* Helper functions */
    public String concatWithNewlines(Collection<String> words) {
        StringBuilder wordList = new StringBuilder();
        for (String word : words) {
            wordList.append(word).append("\n");
        }
        return new String(wordList.deleteCharAt(wordList.length() - 1));
    }

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

    private void openDownloadDocumentSettings(final DownloadDocument document) {
        /* Opens a dialog to edit the DownloadDocument:
         * - title
         * - points
         * - maximum points
         */

        Log.d("DownloadDocsAdapter|" + manager.getName(), "openDownloadDocumentSettings");

        String pointsDefault, maximumPointsDefault;
        if (document.getPoints() < 0) {
            pointsDefault = "";
            maximumPointsDefault = Integer.toString(manager.getMaximumPoints());
        } else {
            pointsDefault = Double.toString(document.getPoints());
            maximumPointsDefault = Integer.toString(document.getMaximumPoints());
        }
        DialogManager.openDownloadDocumentSettings(document, getContext(), document.title, R.string.save,
                document.title, pointsDefault, maximumPointsDefault, this);
    }

    @Override
    public void onDownloadDocumentSettingsChanged(DownloadDocument document, String titleInput,
                                                  String pointsInput, String maximumPointsInput) {
        //titleMap
        boolean titleChanged = false;
        String title = titleInput.trim();
        if (!title.isEmpty()) {
            if (!title.equals(document.title)) {
                Log.d("DownloadDocsAdapter|" + manager.getName(), "title changed: " + title);
                titleChanged = true;
            }
        } else {
            Log.d("DownloadDocsAdapter|" + manager.getName(),
                    "titleInput is not a valid title");
            Snackbar.make(contentLayout,
                    R.string.not_a_valid_name, Snackbar.LENGTH_SHORT)
                    .show();
            return;
        }

        //points
        boolean pointsChanged = false;
        double points;
        try {
            points = Double.parseDouble(pointsInput);
        } catch (NumberFormatException e) {
            points = -1;
        }
        if (points != document.getPoints()) {
            Log.d("DownloadDocsAdapter|" + manager.getName(), "points changed: " + points);
            pointsChanged = true;
        }

        //maximumPoints
        boolean maximumPointsChanged = false;
        int maximumPoints;
        try {
            maximumPoints = Integer.parseInt(maximumPointsInput);
            if (maximumPoints != document.getMaximumPoints()) {
                Log.d("DownloadDocsAdapter|" + manager.getName(), "maximumPoints changed: " + maximumPoints);
                maximumPointsChanged = true;
            }
        } catch (NumberFormatException e) {
            Log.d("DownloadDocsAdapter|" + manager.getName(),
                    "maximumPointsInput is not a number");
            Snackbar.make(contentLayout,
                    R.string.not_a_valid_number, Snackbar.LENGTH_SHORT)
                    .show();
            return;
        }

        if (titleChanged || pointsChanged || maximumPointsChanged) {
            if (titleChanged) {
                if (title.equals(document.titleId)) {
                    manager.getTitleMap().remove(document.titleId);
                } else {
                    manager.getTitleMap().put(document.titleId, title);
                }
            }
            if (pointsChanged) {
                document.setPoints(points);
            }
            if (maximumPointsChanged) {
                document.setMaximumPoints(maximumPoints);
            }

            if (titleChanged) {
                managerChangedListener.onManagerChanged();
                downloadRequestedListener.onDownloadRequested(false);
            } else {
                manager.saveDownloadDocuments();
                notifyDataSetChanged();
            }
        }
    }

    public void openDownloadManagerSettings() {
        /* Opens a dialog to edit the DownloadManager
         * - name
         * - maximumPoints
         * - sheetRegex
         * - stickiedTitles
         * - username and password
         */

        Log.d("DownloadDocsAdapter|" + manager.getName(), "openDownloadManagerSettings");

        DialogManager.openDownloadManagerSettings(getContext(), manager.getName(),
                R.string.save, manager.getName(), manager.getDirectoryURL().toString(),
                Integer.toString(manager.getMaximumPoints()), manager.getSheetRegex(),
                concatWithNewlines(manager.getStickiedTitles()), manager.getUsername(),
                manager.getPassword(), this);
    }

    @Override
    public void onDownloadManagerSettingsChanged(
            String nameInput, String urlInput, String maximumPointsInput,
            String sheetRegex, String stickiedTitlesInput, String username,
            String password) {
        //name
        boolean nameChanged = false;
        String name = nameInput.trim();
        if (!name.isEmpty()) {
            if (!name.equals(manager.getName())) {
                Log.d("DownloadDocsAdapter|" + manager.getName(),
                        "name changed: " + name);
                nameChanged = true;
            }
        } else {
            Log.d("DownloadDocsAdapter|" + manager.getName(),
                    "nameInput is not a valid name");
            Snackbar.make(contentLayout,
                    R.string.not_a_valid_name, Snackbar.LENGTH_SHORT)
                    .show();
            return;
        }
        //url
        boolean urlChanged = false;
        URL url;
        try {
            url = new URL(urlInput);
            if (!url.equals(manager.getDirectoryURL())) {
                Log.d("DownloadDocsAdapter|" + manager.getName(),
                        "url changed: " + url.toString());
                urlChanged = true;
            }
        } catch (MalformedURLException e) {
            Log.d("DownloadDocsAdapter|" + manager.getName(),
                    "urlInput is not a valid url");
            Snackbar.make(contentLayout,
                    R.string.not_a_valid_url, Snackbar.LENGTH_SHORT)
                    .show();
            return;
        }
        //maximumPoints
        boolean maximumPointsChanged = false;
        int maximumPoints;
        try {
            maximumPoints = Integer.parseInt(maximumPointsInput);
            if (maximumPoints != manager.getMaximumPoints()) {
                Log.d("DownloadDocsAdapter|" + manager.getName(),
                        "Maximum points changed: " + maximumPoints);
                maximumPointsChanged = true;
            }
        } catch (NumberFormatException e) {
            Log.d("DownloadDocsAdapter|" + manager.getName(),
                    "maximumPointsInput is not a number");
            Snackbar.make(contentLayout,
                    R.string.not_a_valid_number, Snackbar.LENGTH_SHORT)
                    .show();
            return;
        }
        //sheetRegex
        boolean sheetRegexChanged = false;
        if (!sheetRegex.equals(manager.getSheetRegex())) {
            Log.d("DownloadDocsAdapter|" + manager.getName(),
                    "SheetRegex changed: " + sheetRegex);
            sheetRegexChanged = true;
        }

        //stickiedTitles
        boolean stickiedTitlesChanged = false;
        String[] stickiedTitlesArray = stickiedTitlesInput.split("\n+");
        ArrayList<String> stickiedTitles = new ArrayList<>(Arrays.asList(stickiedTitlesArray));
        if (!stickiedTitles.equals(manager.getStickiedTitles())) {
            Log.d("DownloadDocsAdapter|" + manager.getName(),
                    "stickiedTitles changed");
            stickiedTitlesChanged = true;
        }

        //username and password
        boolean credentialsChanged = false;
        if (!username.equals(manager.getUsername())) {
            Log.d("DownloadDocsAdapter|" + manager.getName(),
                    "username changed: " + username);
            credentialsChanged = true;
        }
        if (!password.equals(manager.getPassword())) {
            Log.d("DownloadDocsAdapter|" + manager.getName(),
                    "password changed: " + password);
            credentialsChanged = true;
        }

        if (nameChanged) {
            manager.setName(name);
        }
        if (urlChanged) {
            manager.setDirectoryURL(url);
        }
        if (maximumPointsChanged) {
            manager.setMaximumPoints(maximumPoints);
        }
        if (sheetRegexChanged) {
            manager.setSheetRegex(sheetRegex);
        }
        if (stickiedTitlesChanged) {
            manager.setStickiedTitles(stickiedTitles);
        }
        if (credentialsChanged) {
            manager.setUsername(username);
            manager.setPassword(password);
        }

        if (urlChanged || credentialsChanged) {
            managerChangedListener.onManagerChanged();
            downloadRequestedListener.onDownloadRequested(true);
        } else if (maximumPointsChanged || sheetRegexChanged || nameChanged
                || stickiedTitlesChanged) {
            managerChangedListener.onManagerChanged();
            downloadRequestedListener.onDownloadRequested(false);
        }

    }


    /* DownloadManager interactions */
    public void completeDownload(SwipeRefreshLayout layout) {
        Log.d("DownloadDocsAdapter|" + manager.getName(), "completeDownload");
        swipeRefreshLayout = layout;
        manager.download();
    }

    public void completeDownloadOffline(SwipeRefreshLayout layout) {
        Log.d("DownloadDocsAdapter|" + manager.getName(), "completeDownloadOffline");
        swipeRefreshLayout = layout;
        manager.downloadOffline();
    }

    public void onListUpdate(DownloadDocument... documents) {
        clear();
        addAll(documents);
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(false);
            swipeRefreshLayout = null;
        }
        Log.d("DownloadDocsAdapter|" + manager.getName(),
                "manager status " + manager.getStatus().name());
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
        LinearLayout textLayout;

        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(ITEM_LAYOUT_ID, parent, false);
            textLayout = convertView.findViewById(R.id.textLayout);
            titleView = convertView.findViewById(R.id.titleText);
            subtitleView = convertView.findViewById(R.id.subtitleText);
            textLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    DownloadDocument document = (DownloadDocument) v.getTag(R.id.file_tag);
                    Log.d("DownloadDocsAdapter|" + manager.getName(),
                            "Opened: " + document.title);
                    openPDFDocument(document.file);
                }
            });
            textLayout.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    DownloadDocument document = (DownloadDocument) v.getTag(R.id.file_tag);
                    Log.d("DownloadDocsAdapter|" + manager.getName(),
                            "Opened settings: " + document.title);
                    openDownloadDocumentSettings(document);
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
        if (downloadDocument != null) {
            titleView.setText(downloadDocument.title);
            textLayout.setTag(R.id.file_tag, downloadDocument);
            subtitleView.setText(downloadDocument.getSubtitle());
        } else {
            Log.e("DownloadDocsAdapter|" + getManager().getName(),
                    "A DownloadDocument in the adapters list is null!");
        }

        // Return the completed view to render on screen
        return convertView;
    }

    /* Listener stuff */
    interface OnManagerChangedListener {
        void onManagerChanged();
    }

    interface OnDownloadRequestedListener {
        void onDownloadRequested(boolean downloadNecessary);
    }

    private static final class ViewHolder {
        private final LinearLayout textLayout;
        private final TextView titleView, subtitleView;

        public ViewHolder(LinearLayout textLayout, TextView titleView, TextView subtitleView) {
            this.textLayout = textLayout;
            this.titleView = titleView;
            this.subtitleView = subtitleView;
        }
    }
}
