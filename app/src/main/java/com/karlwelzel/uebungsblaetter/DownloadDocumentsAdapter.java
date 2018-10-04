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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 * Created by karl on 15.10.17.
 */

public class DownloadDocumentsAdapter extends ArrayAdapter<DownloadDocument>
        implements DownloadManager.OnListUpdateListener {

    private static final int itemLayoutId = R.layout.sheet_listview_item;
    private final TextView pointsView;
    private DownloadManager manager;
    private SwipeRefreshLayout swipeRefreshLayout = null;
    private OnManagerChangedListener listener = null;

    public DownloadDocumentsAdapter(@NonNull Context context, TextView pointsView,
                                    DownloadManager manager) {
        super(context, itemLayoutId);
        this.pointsView = pointsView;
        this.manager = manager;
        this.manager.setOnListUpdateListener(this);
        registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                updatePointsViewText();
            }
        });
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

    private void openDownloadDocumentSettings(final DownloadDocument dd) {
        /* Opens a dialog to edit the DownloadDocument:
         * - title
         * - points
         * - maximum points
         */

        Log.d("DownloadDocsAdapter|" + manager.getName(),
                "openDownloadDocumentSettings: " + dd.title);
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View dialogView = inflater.inflate(R.layout.dialog_download_document_settings, null);
        final TextInputEditText titleInput = dialogView.findViewById(R.id.title_input);
        titleInput.setText(dd.title);
        final TextInputEditText pointsInput = dialogView.findViewById(R.id.points_input);
        if (dd.getPoints() >= 0) {
            pointsInput.setText(Double.toString(dd.getPoints()));
        }
        final TextInputEditText maximumPointsInput = dialogView.findViewById(R.id.maximum_points_input);
        maximumPointsInput.setText(Integer.toString(dd.getMaximumPoints()));
        new AlertDialog.Builder(getContext())
                .setTitle(dd.title)
                .setView(dialogView)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int which) {
                                //titleMap
                                String title = titleInput.getText().toString();
                                if (!title.equals(dd.title)) {
                                    dd.title = title;
                                    manager.getTitleMap().put(dd.titleId, title);
                                }

                                //points
                                try {
                                    String inputText = pointsInput.getText().toString();
                                    dd.setPoints(Double.parseDouble(inputText));
                                } catch (NumberFormatException e) {
                                    Log.d("DownloadDocsAdapter|" + manager.getName(),
                                            "pointsInput.getText() is not a number -> points deleted");
                                    dd.setPoints(-1);
                                }

                                //maximumPoints
                                try {
                                    String inputText = maximumPointsInput.getText().toString();
                                    dd.setMaximumPoints(Integer.parseInt(inputText));
                                } catch (NumberFormatException e) {
                                    Log.d("DownloadDocsAdapter|" + manager.getName(),
                                            "maximumPointsInput.getText() is not a number");
                                    Snackbar.make(MainActivity.contentView,
                                            R.string.not_a_valid_number, Snackbar.LENGTH_SHORT)
                                            .show();
                                }

                                notifyDataSetChanged();
                                manager.saveDownloadDocuments();
                            }
                        })
                .show();
    }

    public void openDownloadManagerSettings() {
        /* Opens a dialog to edit the DownloadManager
         * - name
         * - maximumPoints
         * - sheetRegex
         * - stickiedTitles
         * - username and password
         * - TODO: deletion (button + confirmation popup)
         */

        Log.d("DownloadDocsAdapter|" + manager.getName(), "openDownloadManagerSettings");

        LayoutInflater inflater = LayoutInflater.from(getContext());
        View dialogView = inflater.inflate(R.layout.dialog_download_manager_settings, null);
        final TextInputEditText nameInput = dialogView.findViewById(R.id.name_input);
        nameInput.setText(manager.getName());
        final TextInputEditText urlInput = dialogView.findViewById(R.id.url_input);
        urlInput.setText(manager.getDirectoryURL().toString());
        final TextInputEditText maximumPointsInput = dialogView.findViewById(R.id.maximum_points_input);
        maximumPointsInput.setText(Integer.toString(manager.getMaximumPoints()));
        final TextInputEditText sheetRegexInput = dialogView.findViewById(R.id.sheet_regex_input);
        sheetRegexInput.setText(manager.getSheetRegex());
        final TextInputEditText stickiedTitlesInput = dialogView.findViewById(R.id.stickied_titles_input);
        stickiedTitlesInput.setText(concatWithNewlines(manager.getStickiedTitles()));
        final TextInputEditText usernameInput = dialogView.findViewById(R.id.username_input);
        usernameInput.setText(manager.getUsername());
        final TextInputEditText passwordInput = dialogView.findViewById(R.id.password_input);
        passwordInput.setText(manager.getPassword());
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
                                String name = nameInput.getText().toString();
                                if (!name.equals(manager.getName())) {
                                    Log.d("DownloadDocsAdapter|" + manager.getName(),
                                            "Name changed: " + name);
                                    manager.setName(name);
                                    nameChanged = true;
                                }
                                //url
                                boolean urlChanged = false;
                                try {
                                    URL url = new URL(urlInput.getText().toString());
                                    if (!url.equals(manager.getDirectoryURL())) {
                                        Log.d("DownloadDocsAdapter|" + manager.getName(),
                                                "URL changed: " + url.toString());
                                        manager.setDirectoryURL(url);
                                        urlChanged = true;
                                    }
                                } catch (MalformedURLException e) {
                                    Log.d("DownloadDocsAdapter|" + manager.getName(),
                                            "urlInput.getText() is not a valid url");
                                    Snackbar.make(MainActivity.contentView,
                                            R.string.not_a_valid_url, Snackbar.LENGTH_SHORT)
                                            .show();
                                }
                                //maximumPoints
                                boolean maximumPointsChanged = false;
                                try {
                                    String inputText = maximumPointsInput.getText().toString();
                                    int maximumPoints = Integer.parseInt(inputText);
                                    if (maximumPoints != manager.getMaximumPoints()) {
                                        Log.d("DownloadDocsAdapter|" + manager.getName(),
                                                "Maximum points changed: " + maximumPoints);
                                        manager.setMaximumPoints(maximumPoints);
                                        maximumPointsChanged = true;
                                    }
                                } catch (NumberFormatException e) {
                                    Log.d("DownloadDocsAdapter|" + manager.getName(),
                                            "maximumPointsInput.getText() is not a number");
                                    Snackbar.make(MainActivity.contentView,
                                            R.string.not_a_valid_number, Snackbar.LENGTH_SHORT)
                                            .show();
                                }
                                //sheetRegex
                                boolean sheetRegexChanged = false;
                                String sheetRegex = sheetRegexInput.getText().toString();
                                if (!sheetRegex.equals(manager.getSheetRegex())) {
                                    Log.d("DownloadDocsAdapter|" + manager.getName(),
                                            "SheetRegex changed: " + sheetRegex);
                                    manager.setSheetRegex(sheetRegex);
                                    sheetRegexChanged = true;
                                }

                                //stickiedTitles
                                boolean stickiedTitlesChanged = false;
                                String inputText = stickiedTitlesInput.getText().toString();
                                String[] stickiedTitlesArray = inputText.split("\n+");
                                ArrayList<String> stickiedTitles =
                                        new ArrayList<>(Arrays.asList(stickiedTitlesArray));
                                if (!stickiedTitles.equals(manager.getStickiedTitles())) {
                                    Log.d("DownloadDocsAdapter|" + manager.getName(),
                                            "stickiedTitles changed");
                                    manager.setStickiedTitles(stickiedTitles);
                                    stickiedTitlesChanged = true;
                                }

                                //username and password
                                boolean credentialsChanged = false;
                                String username = usernameInput.getText().toString();
                                String password = passwordInput.getText().toString();
                                if (!username.equals(manager.getUsername())) {
                                    Log.d("DownloadDocsAdapter|" + manager.getName(),
                                            "username changed: " + username);
                                    manager.setUsername(username);
                                    credentialsChanged = true;
                                }
                                if (!password.equals(manager.getPassword())) {
                                    Log.d("DownloadDocsAdapter|" + manager.getName(),
                                            "password changed: " + password);
                                    manager.setPassword(password);
                                    credentialsChanged = true;
                                }


                                if (urlChanged || credentialsChanged) {
                                    notifyListener(true);
                                } else if (maximumPointsChanged || sheetRegexChanged || nameChanged
                                        || stickiedTitlesChanged) {
                                    notifyListener(false);
                                }
                            }
                        })
                .show();
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

    public void onListUpdate(DownloadDocument... files) {
        clear();
        addAll(files);
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
                    Log.d("DownloadDocsAdapter|" + manager.getName(),
                            "Opened: " + dd.title);
                    openPDFDocument(dd.file);
                }
            });
            textLayout.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    DownloadDocument dd = (DownloadDocument) v.getTag(R.id.file_tag);
                    Log.d("DownloadDocsAdapter|" + manager.getName(),
                            "Opened settings: " + dd.title);
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
