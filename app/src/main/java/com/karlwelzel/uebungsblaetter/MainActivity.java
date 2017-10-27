package com.karlwelzel.uebungsblaetter;

import android.Manifest;
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

import java.util.Arrays;

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

    private TextView mTextMessage;
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
                    listViewAdapterAnalysis.completeScan();
                    return true;
                case R.id.navigation_algorithmic_mathematics:
                    mTextMessage.setText(R.string.algorithmic_mathematics);
                    mListView.setAdapter(listViewAdapterAlgorithmicMathematics);
                    listViewAdapterAlgorithmicMathematics.completeScan();
                    return true;
                case R.id.navigation_linear_algebra:
                    mTextMessage.setText(R.string.linear_algebra);
                    mListView.setAdapter(listViewAdapterLinearAlgebra);
                    listViewAdapterLinearAlgebra.completeScan();
                    return true;
            }
            return false;
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
        mListView = (ListView) findViewById(R.id.sheets_list_view);
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                ((SheetsListViewAdapter) mListView.getAdapter()).completeDownload(mSwipeRefreshLayout);
            }
        });
        /*
         * TODO: Refactor the whole download process
         * Possible solution: Check the index site like
         * http://www.math.uni-bonn.de/people/gjasso/resources/pdf/teaching/wise1718/v1g3/
         * download every file that fits some regular expression and then set the names according
         * to this regular expression. All of this would allow to be more flexible with the files
         * that should be shown to the user and would stop the app from downloading nonexistent
         * files
         */
        String[] analysisScriptArray = {"http://www.math.uni-bonn.de/ag/ana/WiSe1718/Analysis1/skript.pdf"};
        String[] algorithmicMathematicsScriptArray = {"http://www.ins.uni-bonn.de/teaching/vorlesungen/AlMaWS13/script.pdf"};
        String[] linearAlgebraScriptArray = {"http://www.math.uni-bonn.de/people/gjasso/resources/pdf/teaching/wise1718/v1g3/LA_2017_1-23.pdf"};
        listViewAdapterAnalysis = new SheetsListViewAdapter(this,
                Arrays.asList(analysisScriptArray),
                "http://www.math.uni-bonn.de/ag/ana/WiSe1718/Analysis1/uebung%d.pdf");
        listViewAdapterAlgorithmicMathematics = new SheetsListViewAdapter(this,
                Arrays.asList(algorithmicMathematicsScriptArray),
                "http://ins.uni-bonn.de/teaching/vorlesungen/AlmaWS17/Uebung/Blatt%d.pdf");
        listViewAdapterLinearAlgebra = new SheetsListViewAdapter(this,
                Arrays.asList(linearAlgebraScriptArray),
                "http://www.math.uni-bonn.de/people/gjasso/resources/pdf/teaching/wise1718/v1g3/u%d_ws1718.pdf");

        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        navigation.setSelectedItemId(R.id.navigation_analysis);
//        mListView.setAdapter(listViewAdapterAnalysis);
//        listViewAdapterAnalysis.completeScan();
    }

}
