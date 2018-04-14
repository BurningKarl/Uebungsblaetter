package com.karlwelzel.uebungsblaetter;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
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
 * - dropdown to choose the subject
 * - option to add more subjects
 * - option to change script names and sheet regex
 * - option to change maximumPoints
 * */

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
    private static final String DIRECTORY_NAME = "Uebungsblaetter_SS18";
    private static final String ANALYSIS_URL = "http://www.math.uni-bonn.de/ag/ana/SoSe2018/V1G2_SS_18";
    private static final String ALGORITHMIC_MATHEMATICS_URL = "http://ins.uni-bonn.de/teaching/vorlesungen/AlmaSS18";
    private static final String LINEAR_ALGEBRA_URL = "http://www.math.uni-bonn.de/people/gjasso/teaching/sose18/v1g4/";

    private static Context mContext;

    private TextView mTextMessage;
    private TextView mPointsView;
    private ListView mListView;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private SheetsListViewAdapter listViewAdapterAnalysis;
    private SheetsListViewAdapter listViewAdapterAlgorithmicMathematics;
    private SheetsListViewAdapter listViewAdapterLinearAlgebra;

    private final BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_analysis:
                    mTextMessage.setText(R.string.analysis);
                    mListView.setAdapter(listViewAdapterAnalysis);
                    break;
                case R.id.navigation_algorithmic_mathematics:
                    mTextMessage.setText(R.string.algorithmic_mathematics);
                    mListView.setAdapter(listViewAdapterAlgorithmicMathematics);
                    break;
                case R.id.navigation_linear_algebra:
                    mTextMessage.setText(R.string.linear_algebra);
                    mListView.setAdapter(listViewAdapterLinearAlgebra);
                    break;
                default:
                    return false;
            }
            ((SheetsListViewAdapter) mListView.getAdapter()).completeScan();
            ((SheetsListViewAdapter) mListView.getAdapter()).updatePointsViewText();
            return true;
        }

    };

    public static Context getContext() {
        return mContext;
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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        setContentView(R.layout.activity_main);

        verifyPermissions();

        mTextMessage = findViewById(R.id.message);
        mPointsView = findViewById(R.id.points_view);
        mListView = findViewById(R.id.sheets_list_view);
        mSwipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                ((SheetsListViewAdapter) mListView.getAdapter())
                        .completeDownload(mSwipeRefreshLayout);
            }
        });

        File dirPath = new File(getExternalFilesDir(null), DIRECTORY_NAME);
        DownloadManager analysisDownloadManager = null,
                algorithmicMathematicsDownloadManager = null,
                linearAlgebraDownloadManager = null;
        try {
            analysisDownloadManager = new DownloadManager(
                    "Analysis",
                    new URL(ANALYSIS_URL),
                    new File(dirPath, getString(R.string.analysis)),
                    40);
            algorithmicMathematicsDownloadManager = new DownloadManager(
                    "AlgorithmicMathematics",
                    new URL(ALGORITHMIC_MATHEMATICS_URL),
                    new File(dirPath, getString(R.string.algorithmic_mathematics)),
                    20);
            linearAlgebraDownloadManager = new DownloadManager(
                    "LinearAlgebra",
                    new URL(LINEAR_ALGEBRA_URL),
                    new File(dirPath, getString(R.string.linear_algebra)),
                    16);

            HashMap<String, String> almaMap = new HashMap<>();
            almaMap.put("Alternatives Vorlesungsskript (H. Harbrecht)", "Skript");
            algorithmicMathematicsDownloadManager.setTitleMap(almaMap);
            ArrayList<String> almaStickied = new ArrayList<>();
            almaStickied.add("Skript");
            algorithmicMathematicsDownloadManager.setStickiedDocuments(almaStickied);
            algorithmicMathematicsDownloadManager.setSheetRegex("Blatt (\\+d)");

            HashMap<String, String> laMap = new HashMap<>();
            laMap.put("Herunterladen", "Skript");
            linearAlgebraDownloadManager.setTitleMap(laMap);
            ArrayList<String> laStickied = new ArrayList<>();
            laStickied.add("Skript");
            linearAlgebraDownloadManager.setStickiedDocuments(laStickied);
            linearAlgebraDownloadManager.setSheetRegex("Übungszettel (\\d+)[*]?");


        } catch (MalformedURLException e) {
            e.printStackTrace();
            finish();
            System.exit(0);
        }

        listViewAdapterAnalysis = new SheetsListViewAdapter(this, mPointsView,
                analysisDownloadManager);
        listViewAdapterAlgorithmicMathematics = new SheetsListViewAdapter(this, mPointsView,
                algorithmicMathematicsDownloadManager);
        listViewAdapterLinearAlgebra = new SheetsListViewAdapter(this, mPointsView,
                linearAlgebraDownloadManager);

        BottomNavigationView navigation = findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        navigation.setSelectedItemId(R.id.navigation_analysis);
    }

    @Override
    protected void onResume() {
        super.onResume();

        BottomNavigationView navigation = findViewById(R.id.navigation);
        navigation.setSelectedItemId(navigation.getSelectedItemId());
    }
}
