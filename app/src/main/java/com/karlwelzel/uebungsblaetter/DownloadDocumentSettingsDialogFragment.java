package com.karlwelzel.uebungsblaetter;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

public class DownloadDocumentSettingsDialogFragment extends DialogFragment implements
        DialogInterface.OnClickListener {

    public static final String FRAGMENT_TAG = "DownloadDocumentSettingsDialog";

    private DownloadDocument document = null;
    private String pointsDefault;
    private String maximumPointsDefault;

    private TextInputEditText titleInput;
    private TextInputEditText pointsInput;
    private TextInputEditText maximumPointsInput;

    private OnDownloadDocumentSettingsChangedListener listener = null;

    public void show(FragmentManager manager) {
        super.show(manager, FRAGMENT_TAG);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        /* Opens a dialog to edit the DownloadDocument:
         * - title
         * - points
         * - maximum points
         */

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        View dialogView = inflater.inflate(R.layout.dialog_download_document_settings, null);
        titleInput = dialogView.findViewById(R.id.title_input);
        pointsInput = dialogView.findViewById(R.id.points_input);
        maximumPointsInput = dialogView.findViewById(R.id.maximum_points_input);
        final TextInputLayout titleInputLayout = dialogView.findViewById(R.id.title_input_layout);
        final TextInputLayout pointsInputLayout = dialogView.findViewById(R.id.points_input_layout);
        final TextInputLayout maximumPointsInputLayout = dialogView.findViewById(R.id.maximum_points_input_layout);

        titleInput.setText(document.title);
        pointsInput.setText(pointsDefault);
        maximumPointsInput.setText(maximumPointsDefault);

        // Some extra hints below the input
        // title: If this is not a valid title (disables the save button)
        // points: If the points will be deleted
        // maximumPoints: If this is not a valid number (disables the save button)

        titleInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                AlertDialog dialog = (AlertDialog) getDialog();
                Button positiveButton = dialog.getButton(Dialog.BUTTON_POSITIVE);
                if (s.toString().trim().isEmpty()) {
                    titleInputLayout.setError(getContext().getString(R.string.not_a_valid_name));
                    positiveButton.setEnabled(false);
                } else {
                    titleInputLayout.setError(null);
                    positiveButton.setEnabled(true);
                }
            }
        });

        pointsInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                AlertDialog dialog = (AlertDialog) getDialog();

                boolean isAnInteger = true;
                try {
                    Double.parseDouble(s.toString());
                } catch (NumberFormatException e) {
                    isAnInteger = false;
                    if (!pointsDefault.isEmpty()) {
                        pointsInputLayout.setError(
                                getContext().getString(R.string.points_will_be_deleted));
                    }
                }
                if (isAnInteger) {
                    pointsInputLayout.setError(null);
                }
            }
        });

        maximumPointsInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                AlertDialog dialog = (AlertDialog) getDialog();
                Button positiveButton = dialog.getButton(Dialog.BUTTON_POSITIVE);

                boolean isAnInteger = true;
                try {
                    Double.parseDouble(s.toString());
                } catch (NumberFormatException e) {
                    isAnInteger = false;
                    maximumPointsInputLayout.setError(getContext().getString(R.string.not_a_valid_number));
                    positiveButton.setEnabled(false);
                }
                if (isAnInteger) {
                    maximumPointsInputLayout.setError(null);
                    positiveButton.setEnabled(true);
                }
            }
        });

        builder.setView(dialogView)
                .setTitle(document.title)
                .setPositiveButton(R.string.save, this)
                .setNegativeButton(android.R.string.cancel, this);
        return builder.create();
    }

    public void setListener(OnDownloadDocumentSettingsChangedListener listener) {
        this.listener = listener;
    }

    public void setDefaults(DownloadDocument document, DownloadManager manager) {
        this.document = document;
        if (document.getPoints() < 0) {
            pointsDefault = "";
            maximumPointsDefault = Integer.toString(manager.getMaximumPoints());
        } else {
            pointsDefault = Double.toString(document.getPoints());
            maximumPointsDefault = Integer.toString(document.getMaximumPoints());
        }
    }

    /**
     * This method will be invoked when a button in the dialog is clicked.
     *
     * @param dialog the dialog that received the click
     * @param which  the button that was clicked (ex.
     *               {@link DialogInterface#BUTTON_POSITIVE}) or the position
     */
    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                onSave();
                break;
            case DialogInterface.BUTTON_NEGATIVE:
                getDialog().cancel();
        }
    }

    public void onSave() {
        String titleValue = titleInput.getText().toString();
        String pointsValue = pointsInput.getText().toString();
        String maximumPointsValue = maximumPointsInput.getText().toString();
        //titleMap
        boolean titleChanged = false;
        String title = titleValue.trim();
        if (!title.isEmpty()) {
            if (!title.equals(document.title)) {
                Log.d("DownloadDocumentDialog", "title changed: " + title);
                titleChanged = true;
            }
        } else {
            Log.d("DownloadDocumentDialog",
                    "titleInput is not a valid title");
            return;
        }

        //points
        boolean pointsChanged = false;
        double points;
        try {
            points = Double.parseDouble(pointsValue);
        } catch (NumberFormatException e) {
            points = -1;
        }
        if (points != document.getPoints()) {
            Log.d("DownloadDocumentDialog", "points changed: " + points);
            pointsChanged = true;
        }

        //maximumPoints
        boolean maximumPointsChanged = false;
        int maximumPoints;
        try {
            maximumPoints = Integer.parseInt(maximumPointsValue);
            if (maximumPoints != document.getMaximumPoints()) {
                Log.d("DownloadDocumentDialog", "maximumPoints changed: " + maximumPoints);
                maximumPointsChanged = true;
            }
        } catch (NumberFormatException e) {
            Log.d("DownloadDocumentDialog",
                    "maximumPointsInput is not a number");
            return;
        }

        if (titleChanged || pointsChanged || maximumPointsChanged) {
            if (listener != null) {
                listener.onDownloadDocumentSettingsChanged(document, title, points, maximumPoints);
            } else {
                Log.e("DownloadDocumentDialog", "listener was not set!");
            }
        }

    }

    interface OnDownloadDocumentSettingsChangedListener {
        void onDownloadDocumentSettingsChanged(DownloadDocument document, String title,
                                               double points, int maximumPoints);
    }

}
