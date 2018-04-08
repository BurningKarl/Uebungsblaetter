package com.karlwelzel.uebungsblaetter;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
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
    private static final String ANALYSIS_URL = "http://www.math.uni-bonn.de/ag/ana/SoSe2018/Analysis2";
    private static final String ALGORITHMIC_MATHEMATICS_URL = "http://ins.uni-bonn.de/teaching/vorlesungen/AlmaSS18";
    private static final String LINEAR_ALGEBRA_URL = "http://www.math.uni-bonn.de/people/gjasso/resources/pdf/teaching/sose18/v1g4/";

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
        setContentView(R.layout.activity_main);

        verifyPermissions();

        mTextMessage = (TextView) findViewById(R.id.message);
        mPointsView = (TextView) findViewById(R.id.points_view);
        mListView = (ListView) findViewById(R.id.sheets_list_view);
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                ((SheetsListViewAdapter) mListView.getAdapter())
                        .completeDownload(mSwipeRefreshLayout);
            }
        });

        File dirPath = new File(Environment.getExternalStorageDirectory(), DIRECTORY_NAME);
        DownloadManager analysisDownloadManager = null,
                algorithmicMathematicsDownloadManager = null,
                linearAlgebraDownloadManager = null;
        try {
            analysisDownloadManager = new AnaDownloadManager(this,
                    new URL(ANALYSIS_URL), new File(dirPath, getString(R.string.analysis)),
                    "AnalysisFiles");
            algorithmicMathematicsDownloadManager = new AlMaDownloadManager(this,
                    new URL(ALGORITHMIC_MATHEMATICS_URL), new File(dirPath, getString(R.string.algorithmic_mathematics)),
                    "AlgorithmicMathematicsFiles");
            linearAlgebraDownloadManager = new LADownloadManager(this,
                    new URL(LINEAR_ALGEBRA_URL), new File(dirPath, getString(R.string.linear_algebra)),
                    "LinearAlgebraFiles");
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

        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        navigation.setSelectedItemId(R.id.navigation_analysis);
    }

    @Override
    protected void onResume() {
        super.onResume();

        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setSelectedItemId(navigation.getSelectedItemId());
    }
}
