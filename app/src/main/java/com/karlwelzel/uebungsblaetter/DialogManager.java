package com.karlwelzel.uebungsblaetter;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.StringRes;
import android.support.design.widget.TextInputEditText;
import android.view.LayoutInflater;
import android.view.View;

/**
 * Created by karl on 5.10.18.
 */

public class DialogManager {
    // TODO: Add a new DownloadManagerSettingsDialogFragment class
    public static void openDownloadManagerSettings(
            Context context, String title, @StringRes int positiveButton, String nameDefault,
            String urlDefault, String maximumPointsDefault, String sheetRegexDefault,
            String stickiedTitlesDefault, String usernameDefault, String passwordDefault,
            final OnDownloadManagerSettingsChangedListener listener) {
        LayoutInflater inflater = LayoutInflater.from(context);
        @SuppressLint("InflateParams")
        View dialogView = inflater.inflate(R.layout.dialog_download_manager_settings, null);
        final TextInputEditText nameInput = dialogView.findViewById(R.id.name_input);
        final TextInputEditText urlInput = dialogView.findViewById(R.id.url_input);
        final TextInputEditText maximumPointsInput = dialogView.findViewById(R.id.maximum_points_input);
        final TextInputEditText sheetRegexInput = dialogView.findViewById(R.id.sheet_regex_input);
        final TextInputEditText stickiedTitlesInput = dialogView.findViewById(R.id.stickied_titles_input);
        final TextInputEditText usernameInput = dialogView.findViewById(R.id.username_input);
        final TextInputEditText passwordInput = dialogView.findViewById(R.id.password_input);
        nameInput.setText(nameDefault);
        urlInput.setText(urlDefault);
        maximumPointsInput.setText(maximumPointsDefault);
        sheetRegexInput.setText(sheetRegexDefault);
        stickiedTitlesInput.setText(stickiedTitlesDefault);
        usernameInput.setText(usernameDefault);
        passwordInput.setText(passwordDefault);
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setView(dialogView)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(positiveButton,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int which) {
                                listener.onDownloadManagerSettingsChanged(
                                        nameInput.getText().toString(),
                                        urlInput.getText().toString(),
                                        maximumPointsInput.getText().toString(),
                                        sheetRegexInput.getText().toString(),
                                        stickiedTitlesInput.getText().toString(),
                                        usernameInput.getText().toString(),
                                        passwordInput.getText().toString()
                                );
                            }
                        })
                .show();

    }

    public static void openDownloadManagerSettings(
            Context context, @StringRes int title, @StringRes int positiveButton,
            final OnDownloadManagerSettingsChangedListener listener) {
        openDownloadManagerSettings(context, context.getString(title), positiveButton,
                "", "", "", "",
                "", "", "", listener);
    }

    interface OnDownloadManagerSettingsChangedListener {
        void onDownloadManagerSettingsChanged(
                String nameInput, String urlInput, String maximumPointsInput,
                String sheetRegexInput, String stickiedTitlesInput,
                String usernameInput, String passwordInput);
    }
}
