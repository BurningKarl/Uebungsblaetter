package com.karlwelzel.uebungsblaetter;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
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

/* TODO: Update the GUI to make new features available to the user
 * The DownloadManager should
 * - set the date from the "Last-Modified" header       Done
 * - have a list of script names                        Done
 * - have a regex for the sheets                        Done
 *
 * All pdf-files are fetched and downloaded from the website then displayed in order
 * 1. All scripts by given order                        Done
 * 2. All sheets by number (reversed)                   Done
 * 3. Everything else by date                           Done
 *
 * Additional GUI elements
 * - tab layout to choose the subject                   Done
 * - option to add more subjects
 * - option to change script names and sheet regex
 * - option to change maximumPoints
 * */

/* TODO: Add new popups for DownloadDocument and DownloadManager
 * Backend changes:
 * - use sheetRegex on titleSuggestion and save the ouput as a new field in DownloadDocument
 * - use titleMap AFTER that on titleSuggestion
 * - changed the stickied array to a stickied SparseArray<String>
 *
 * Popup for DownloadDocument:
 * - points
 * - titleSuggestion (disabled, just display the value)
 * - new title
 * - stickied index (may not collide with other indexes)
 *
 * Popup for DownloadManager:
 * - name
 * - maximumPoints
 * - sheetRegex
 * - delete the manager (later)
 */

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static final int REQUEST_INTERNET = 1;
    private static final String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    private static final String[] PERMISSIONS_INTERNET = {
            Manifest.permission.INTERNET
    };
    private static final String PREFERENCES_NAME = "__MainActivity";
    private static final String DIRECTORY_NAME = "Uebungsblaetter_SS18";
    private static final String ANALYSIS_URL = "http://www.math.uni-bonn.de/ag/ana/SoSe2018/V1G2_SS_18";
    private static final String ALGORITHMIC_MATHEMATICS_URL = "http://ins.uni-bonn.de/teaching/vorlesungen/AlmaSS18";
    private static final String LINEAR_ALGEBRA_URL = "http://www.math.uni-bonn.de/people/gjasso/teaching/sose18/v1g4/";
    private static final String LOGIC_URL = "http://www.math.uni-bonn.de/ag/logik/teaching/2018SS/logik.shtml";

    private static Context activityContext;

    private ArrayList<DownloadManager> managers;
    private ArrayList<SheetsListViewAdapter> adapters;

    private SharedPreferences preferences;

    private TextView pointsView;
    private ListView listView;
    private TabLayout navigationBar;
    private SwipeRefreshLayout swipeRefreshLayout;

    private final TabLayout.OnTabSelectedListener onTabSelectedListener = new TabLayout.OnTabSelectedListener() {
        @Override
        public void onTabSelected(TabLayout.Tab tab) {
            Log.d("MainActivity", "onTabSelected: " + tab.getPosition() + "|" + tab.getText());
            SheetsListViewAdapter chosenAdapter = (SheetsListViewAdapter) tab.getTag();
            if (chosenAdapter == null) {
                Log.e("MainActivity", "Tab has no tag!");
            } else {
                listView.setAdapter(chosenAdapter);
                chosenAdapter.completeScan();
                chosenAdapter.updatePointsViewText();
            }
        }

        @Override
        public void onTabUnselected(TabLayout.Tab tab) {

        }

        @Override
        public void onTabReselected(TabLayout.Tab tab) {
            onTabSelected(tab);
        }
    };

    public static Context getContext() {
        return activityContext;
    }

    private void verifyPermissions() {
        int permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
        }

        permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.INTERNET);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, PERMISSIONS_INTERNET, REQUEST_INTERNET);
        }
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

    public ArrayList<DownloadManager> loadDownloadManagers() {
        Type collectionType = new TypeToken<ArrayList<DownloadManagerSettings>>() {
        }.getType();
        //TODO: Choose a default that does not break the app
        String dataString = preferences.getString("managers", "[]");
        Log.d("MainActivity", "loadDownloadManagers:\n" + dataString);
        ArrayList<DownloadManagerSettings> managerSettings = new Gson().fromJson(dataString, collectionType);
        ArrayList<DownloadManager> downloadManagers = new ArrayList<>();
        for (DownloadManagerSettings settings : managerSettings) {
            downloadManagers.add(new DownloadManager(settings));
        }
        return downloadManagers;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityContext = this;
        setContentView(R.layout.activity_main);

        verifyPermissions();

        pointsView = findViewById(R.id.points_view);
        listView = findViewById(R.id.sheets_list_view);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                ((SheetsListViewAdapter) listView.getAdapter())
                        .completeDownload(swipeRefreshLayout);
            }
        });

        preferences = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE);

        File dirPath = new File(getExternalFilesDir(null), DIRECTORY_NAME);
        DownloadManager analysisDownloadManager = null,
                algorithmicMathematicsDownloadManager = null,
                linearAlgebraDownloadManager = null,
                logicDownloadManager = null;
        try {
            analysisDownloadManager = new DownloadManager(
                    "Ana",
                    new URL(ANALYSIS_URL),
                    new File(dirPath, "Ana"));
            algorithmicMathematicsDownloadManager = new DownloadManager(
                    "AlMa",
                    new URL(ALGORITHMIC_MATHEMATICS_URL),
                    new File(dirPath, "AlMa"));
            linearAlgebraDownloadManager = new DownloadManager(
                    "LA",
                    new URL(LINEAR_ALGEBRA_URL),
                    new File(dirPath, "LA"));
            logicDownloadManager = new DownloadManager("Log",
                    new URL(LOGIC_URL),
                    new File(dirPath, "Log"));

            analysisDownloadManager.setMaximumPoints(50);
            analysisDownloadManager.setSheetRegex("Übungsblatt (\\d+)");

            algorithmicMathematicsDownloadManager.setMaximumPoints(20);
            HashMap<String, String> almaMap = new HashMap<>();
            almaMap.put("Alternatives Vorlesungsskript (H. Harbrecht)", "Skript");
            algorithmicMathematicsDownloadManager.setTitleMap(almaMap);
            ArrayList<String> almaStickied = new ArrayList<>();
            almaStickied.add("Skript");
            algorithmicMathematicsDownloadManager.setStickiedTitles(almaStickied);
            algorithmicMathematicsDownloadManager.setSheetRegex("Blatt (\\d+)");

            linearAlgebraDownloadManager.setMaximumPoints(16);
            HashMap<String, String> laMap = new HashMap<>();
            laMap.put("Herunterladen", "Skript");
            linearAlgebraDownloadManager.setTitleMap(laMap);
            ArrayList<String> laStickied = new ArrayList<>();
            laStickied.add("Skript");
            linearAlgebraDownloadManager.setStickiedTitles(laStickied);
            linearAlgebraDownloadManager.setSheetRegex("Übungszettel (\\d+)[*]?");

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

        // Already works but is impracticable
        //managers = loadDownloadManagers();

        adapters = new ArrayList<>();
        for (DownloadManager manager : managers) {
            adapters.add(new SheetsListViewAdapter(this, pointsView, manager));
        }

        navigationBar = findViewById(R.id.navigation_bar);

        for (int i = 0; i < managers.size(); i++) {
            TabLayout.Tab tab = navigationBar.newTab();
            tab.setText(managers.get(i).getSettings().managerName);
            tab.setTag(adapters.get(i));
            navigationBar.addTab(tab);
        }

        navigationBar.addOnTabSelectedListener(onTabSelectedListener);
        TabLayout.Tab firstTab = navigationBar.getTabAt(0);
        if (firstTab != null) {
            firstTab.select();
        }
    }
}
