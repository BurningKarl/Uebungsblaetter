package com.karlwelzel.uebungsblaetter;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity implements
        DownloadDocumentsAdapter.OnManagerChangedListener, TabLayout.OnTabSelectedListener,
        SwipeRefreshLayout.OnRefreshListener {

    private static final int PERMISSION_REQUEST_CODE = 1;
    private static final String[] PERMISSIONS = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.INTERNET
    };
    private static final String PREFERENCES_NAME = "__MainActivity";
    private static final String DIRECTORY_NAME = "Uebungsblaetter";
    private static final String STATE_TAB = "tab";

    private static final String ANALYSIS_URL = "http://www.math.uni-bonn.de/ag/ana/SoSe2018/V1G2_SS_18";
    private static final String ALGORITHMIC_MATHEMATICS_URL = "https://ins.uni-bonn.de/teachings/ss-2018-203-v1g6-algorithmische-m/";
    private static final String LINEAR_ALGEBRA_URL = "http://www.math.uni-bonn.de/people/gjasso/teaching/sose18/v1g4/";
    private static final String LOGIC_URL = "http://www.math.uni-bonn.de/ag/logik/teaching/2018SS/logik.shtml";

    private static Context activityContext;

    private ArrayList<DownloadManager> managers;
    private ArrayList<DownloadDocumentsAdapter> adapters;

    private SharedPreferences preferences;

    public static View contentView;
    private TextView pointsView;
    private ListView listView;
    private TabLayout navigationBar;
    private TabLayout.Tab activeTab;
    private SwipeRefreshLayout swipeRefreshLayout;

    public static Context getContext() {
        return activityContext;
    }

    /* Permissions */
    private void verifyPermissions() {
        //This does not block the program
        boolean allPermissionsGranted = true;
        for (String permission : PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                allPermissionsGranted = false;
                break;
            }
        }
        if (allPermissionsGranted) {
            Log.d("MainActivity", "All permissions have been granted.");
        } else {
            Log.d("MainActivity", "Some permissions were not granted, requesting them...");
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE: {
                // If request is cancelled, the result arrays are empty.
                boolean allPermissionsGranted = true;
                if (grantResults.length == 0) {
                    allPermissionsGranted = false;
                } else {
                    for (int result : grantResults) {
                        if (result != PackageManager.PERMISSION_GRANTED) {
                            allPermissionsGranted = false;
                        }
                    }
                }
                if (allPermissionsGranted) {
                    Log.d("MainActivity", "All permissions have been actively granted.");
                } else {
                    finish();
                }
            }
        }
    }

    public ArrayList<DownloadManager> loadDownloadManagers() {
        Type collectionType = new TypeToken<ArrayList<DownloadManagerSettings>>() {
        }.getType();
        String dataString = preferences.getString("managers", "[]");
        Log.d("MainActivity", "loadDownloadManagers:\n" + dataString);
        ArrayList<DownloadManagerSettings> managerSettings = new Gson().fromJson(dataString, collectionType);
        ArrayList<DownloadManager> downloadManagers = new ArrayList<>();
        for (DownloadManagerSettings settings : managerSettings) {
            downloadManagers.add(new DownloadManager(settings));
        }
        return downloadManagers;
    }

    public void saveDownloadManagers() {
        ArrayList<DownloadManagerSettings> managerSettings = new ArrayList<>();
        for (DownloadManager manager : managers) {
            managerSettings.add(manager.getSettings());
        }
        String dataString = new Gson().toJson(managerSettings);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("managers", dataString);
        Log.d("MainActivity", "saveDownloadManagers:\n" + dataString);
        editor.apply();
    }

    public void openDeleteDownloadManagerDialog() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.delete_website_are_you_sure)
                .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        deleteActiveDownloadManager();
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                    }
                })
                .show();
    }

    public void deleteActiveDownloadManager() {
        // Deletes the tab, adapter and download manager. Be careful!
        if (managers.size() <= 1) {
            Snackbar.make(contentView, R.string.unable_to_delete_last_tab, Snackbar.LENGTH_SHORT)
                    .show();
        } else {
            DownloadDocumentsAdapter adapter = (DownloadDocumentsAdapter) listView.getAdapter();
            Log.d("MainActivity", "Delete manager " + adapter.getManager().getName()
                    + " on tab " + activeTab.getText());
            managers.remove(adapter.getManager());
            adapters.remove(adapter);
            navigationBar.removeTab(activeTab);
            onManagerChanged();
        }
    }

    public void deleteAllDownloadManagers() { // this for debug purposes only
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove("managers");
        editor.apply();
    }

    public void onManagerChanged() {
        saveDownloadManagers();
        for (int i = 0; i < navigationBar.getTabCount(); i++) {
            navigationBar.getTabAt(i).setText(managers.get(i).getName());
        }
    }

    @Override
    public void onManagerChanged(boolean downloadNecessary) {
        onManagerChanged();
        if (downloadNecessary) {
            swipeRefreshLayout.setRefreshing(true);
            onRefresh();
        } else {
            swipeRefreshLayout.setRefreshing(true);
            ((DownloadDocumentsAdapter) listView.getAdapter()).completeDownloadOffline(swipeRefreshLayout);
        }
    }

    /* Tabs */
    @Override
    public void onTabSelected(TabLayout.Tab tab) {
        Log.d("MainActivity", "onTabSelected: " + tab.getPosition() + "|" + tab.getText());
        activeTab = tab;
        DownloadDocumentsAdapter chosenAdapter = (DownloadDocumentsAdapter) tab.getTag();
        if (chosenAdapter == null) {
            Log.e("MainActivity", "Tab has no tag!");
        } else {
            listView.setAdapter(chosenAdapter);
            chosenAdapter.updatePointsViewText();
        }
    }

    @Override
    public void onTabUnselected(TabLayout.Tab tab) {
    }

    @Override
    public void onTabReselected(TabLayout.Tab tab) {
    }

    @Override
    public void onRefresh() {
        ((DownloadDocumentsAdapter) listView.getAdapter()).completeDownload(swipeRefreshLayout);
    }

    /* Menu */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.add_item:
                // TODO: Implement adding tabs
                break;
            case R.id.delete_item:
                openDeleteDownloadManagerDialog();
                break;
            case R.id.settings_item:
                DownloadDocumentsAdapter adapter = (DownloadDocumentsAdapter) listView.getAdapter();
                adapter.openDownloadManagerSettings();
                break;
            default:
                break;
        }
        return true;
    }

    /* onCreate*/
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putInt(STATE_TAB, navigationBar.getSelectedTabPosition());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            Log.d("onCreate", "savedInstanceState provided");
        }
        activityContext = this;
        setContentView(R.layout.activity_main);

        verifyPermissions();

        contentView = findViewById(android.R.id.content);
        pointsView = findViewById(R.id.points_view);
        listView = findViewById(R.id.sheets_list_view);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setOnRefreshListener(this);

        preferences = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE);
        managers = loadDownloadManagers();

        if (managers.isEmpty()) {
            File dirPath = new File(getExternalFilesDir(null), DIRECTORY_NAME);
            Log.d("Main Activity", "dirPath: " + dirPath.toString());
            DownloadManager analysisDownloadManager = null,
                    algorithmicMathematicsDownloadManager = null,
                    linearAlgebraDownloadManager = null,
                    logicDownloadManager = null;
            try {
                analysisDownloadManager = new DownloadManager(
                        "Ana",
                        new URL(ANALYSIS_URL),
                        dirPath);
                algorithmicMathematicsDownloadManager = new DownloadManager(
                        "AlMa",
                        new URL(ALGORITHMIC_MATHEMATICS_URL),
                        dirPath);
                linearAlgebraDownloadManager = new DownloadManager(
                        "LA",
                        new URL(LINEAR_ALGEBRA_URL),
                        dirPath);
                logicDownloadManager = new DownloadManager("Log",
                        new URL(LOGIC_URL),
                        dirPath);

                analysisDownloadManager.setMaximumPoints(50);
                ArrayList<String> analysisStickied = new ArrayList<>();
                analysisStickied.add("Skript");
                analysisDownloadManager.setStickiedTitles(analysisStickied);
                analysisDownloadManager.setSheetRegex("Übungsblatt (\\d+)");

                algorithmicMathematicsDownloadManager.setMaximumPoints(20);
                HashMap<String, String> almaMap = new HashMap<>();
                almaMap.put("Alternatives Vorlesungsskript (H. Harbrecht)", "Skript");
                algorithmicMathematicsDownloadManager.setTitleMap(almaMap);
                ArrayList<String> almaStickied = new ArrayList<>();
                almaStickied.add("Skript");
                algorithmicMathematicsDownloadManager.setStickiedTitles(almaStickied);
                algorithmicMathematicsDownloadManager.setSheetRegex("Blatt (\\d+)");
                algorithmicMathematicsDownloadManager.setUsername("alma");
                algorithmicMathematicsDownloadManager.setPassword("landau");

                linearAlgebraDownloadManager.setMaximumPoints(16);
                HashMap<String, String> laMap = new HashMap<>();
                laMap.put("Herunterladen", "Skript");
                linearAlgebraDownloadManager.setTitleMap(laMap);
                ArrayList<String> laStickied = new ArrayList<>();
                laStickied.add("Skript");
                linearAlgebraDownloadManager.setStickiedTitles(laStickied);
                linearAlgebraDownloadManager.setSheetRegex("Übungszettel (\\d+)[*]?");
                linearAlgebraDownloadManager.setUsername("v1g4");
                linearAlgebraDownloadManager.setPassword("frobenius");

                logicDownloadManager.setMaximumPoints(20);
                HashMap<String, String> logicMap = new HashMap<>();
                logicMap.put("Skript zur Vorlesung", "Skript");
                logicDownloadManager.setTitleMap(logicMap);
                ArrayList<String> logicStickied = new ArrayList<>();
                logicStickied.add("Skript");
                logicDownloadManager.setStickiedTitles(logicStickied);
                logicDownloadManager.setSheetRegex("Serie (\\d+)");

            } catch (MalformedURLException e) {
                e.printStackTrace();
                finish();
                System.exit(0);
            }

            managers = new ArrayList<>();
            managers.add(analysisDownloadManager);
            managers.add(algorithmicMathematicsDownloadManager);
            managers.add(linearAlgebraDownloadManager);
            managers.add(logicDownloadManager);

            saveDownloadManagers();
        }

        adapters = new ArrayList<>();
        for (DownloadManager manager : managers) {
            DownloadDocumentsAdapter current = new DownloadDocumentsAdapter(this, pointsView, manager);
            current.setListener(this);
            adapters.add(current);
        }

        navigationBar = findViewById(R.id.navigation_bar);
        navigationBar.addOnTabSelectedListener(this);

        for (int i = 0; i < managers.size(); i++) {
            TabLayout.Tab tab = navigationBar.newTab();
            tab.setText(managers.get(i).getName());
            tab.setTag(adapters.get(i));
            navigationBar.addTab(tab);
        }

        if (savedInstanceState != null) {
            int selectedTab = savedInstanceState.getInt(STATE_TAB);
            navigationBar.getTabAt(selectedTab).select();
        }

        //deleteAllDownloadManagers();
    }
}
