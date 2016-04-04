package com.lalith;

import android.app.Dialog;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.NotificationCompat;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.lalith.utils.SdcardLocations;
import com.lalith.utils.SdcardUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private final String TAG = "SdcardScanner";
    private AsyncTask scanFilesTask = null;
    // Progress Dialog
    private ProgressDialog pDialog;
    // Progress dialog type (0 - for Horizontal progress bar)
    public static final int progress_bar_type = 0;
    int filesCount = 0;
    int directoriesCount = 0;
    private Map<String, Long> maxFilesMap;
    private List<String> fileExtensions;
    private NotificationManager mNotifyManager;
    private NotificationCompat.Builder mBuilder;
    private File selectedSdcardPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Scanning started", Snackbar.LENGTH_SHORT)
                        .setAction("Action", null).show();
                getListFiles(selectedSdcardPath);
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        Menu m = navigationView.getMenu();
        SubMenu topChannelMenu = m.addSubMenu("Sdcard Locations");


        int i = 0;
        for (SdcardLocations sdcardLocations : SdcardLocations.getSdcardLocations(this).values()) {
            Log.e(TAG, "==Paths==" + sdcardLocations.title);
            topChannelMenu.add(Menu.NONE, i, Menu.NONE, sdcardLocations.title);
            i++;
        }


        getListFiles(new File(SdcardUtils.getStoragePath()));
    }

    private void createNotification() {
        mNotifyManager = (NotificationManager) getSystemService(this.NOTIFICATION_SERVICE);
        mBuilder = new NotificationCompat.Builder(MainActivity.this);
        mBuilder.setContentTitle("Scanning sdcard")
                .setContentText("Scanning in progress")
                .setSmallIcon(R.drawable.ic_menu_send);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case progress_bar_type: // we set this to 0
                pDialog = new ProgressDialog(this);
                pDialog.setMessage("Scanning sdcard...");
                pDialog.setIndeterminate(false);
                pDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                pDialog.setCancelable(true);
                pDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        if(scanFilesTask != null) {
                            scanFilesTask.cancel(true);
                        }
                    }
                });
                pDialog.show();
                return pDialog;
            default:
                return null;
        }
    }

    private void getListFiles(File parentDir) {
        selectedSdcardPath = parentDir;
        directoriesCount = 0;
        filesCount = 0;
        maxFilesMap = new HashMap<>();
        fileExtensions = new ArrayList<>();
        String storagePath = SdcardUtils.getStoragePath();
        Log.e(TAG, "StoragePath: " + storagePath);

        createNotification();
        scanFilesTask = new ScanFilesTask().execute(parentDir);


    }

    private class ScanFilesTask extends AsyncTask<File, String, List<File>> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showDialog(progress_bar_type);
            pDialog.setMax(SdcardUtils.totalFiles(selectedSdcardPath));
            // Displays the progress bar for the first time.
            mBuilder.setProgress(0, 0, true);
            mNotifyManager.notify(2, mBuilder.build());
        }

        @Override
        protected List<File> doInBackground(File... params) {
            if(isCancelled()){
                // end task right away
                return null;
            }
            List<File> files = scanFiles(params[0], this);
            maxFilesMap = SdcardUtils.sortByComparator(maxFilesMap, false);
            return files;
        }

        @Override
        protected void onPostExecute(List<File> files) {
            super.onPostExecute(files);
            pDialog.setProgress(filesCount);

            // When the loop is finished, updates the notification
            mBuilder.setContentText("Scanning complete")
                    // Removes the progress bar
                    .setProgress(0, 0, false);
            mNotifyManager.notify(2, mBuilder.build());
            TextView filePaths = (TextView) findViewById(R.id.filePaths);

            StringBuffer buffer = new StringBuffer();
            buffer.append("Scanning completed")
                    .append("<br>")
                    .append("Total diretories found: " + directoriesCount)
                    .append("<br>")
                    .append("Total files found: " + filesCount)
                    .append("<br>");

            List<Long> list = new ArrayList<Long>(maxFilesMap.values());
            buffer.append("Average file size is: " + SdcardUtils.calculateLength(SdcardUtils.average(list)) + "<br>");

            filePaths.setText(Html.fromHtml(buffer.toString()));
            
            showPictorialRepresentation();
            pDialog.hide();

        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            pDialog.setProgress(Integer.parseInt(values[0]));
        }

        public void doProgress(String value) {
            publishProgress(value);
        }
    }

    private void showPictorialRepresentation() {
        int i = 1;
        long totalSize = 0;
        for (Map.Entry<String, Long> entry : maxFilesMap.entrySet()) {
            if (i > 5) break;
            totalSize += entry.getValue();
            i++;
        }

         i = 1;
        LinearLayout maxFileSizeColorsLayout = (LinearLayout) findViewById(R.id.maxFileSizeColorsLayout);
        LinearLayout maxFileSizeNamesLayout = (LinearLayout) findViewById(R.id.maxFileSizeNamesLayout);
        int[] colors = getResources().getIntArray(R.array.colors);
        maxFileSizeColorsLayout.removeAllViews();
        maxFileSizeNamesLayout.removeAllViews();
        maxFileSizeColorsLayout.setWeightSum(100);
        for (Map.Entry<String, Long> entry : maxFilesMap.entrySet()) {
            if (i > 5) break;
            ImageView view = new ImageView(this);
            view.setBackgroundColor(colors[i+1]);
            float weightSum =(float)( entry.getValue() * 100/ totalSize) ;
            view.setLayoutParams(new LinearLayout.LayoutParams(0, 30,weightSum));
            maxFileSizeColorsLayout.addView(view);


            //Setting names
            LayoutInflater inflater = LayoutInflater.from(this);
            View layoutView = inflater.inflate(R.layout.name_color_item, null, false);
            ((TextView)layoutView.findViewById(R.id.nameTextView)).setText(entry.getKey() +" ("+SdcardUtils.calculateLength(entry.getValue())+" )");
            ((ImageView)layoutView.findViewById(R.id.colorImageView)).setBackgroundColor(colors[i+1]);
            maxFileSizeNamesLayout.addView(layoutView);
            i++;
        }

        //File Extensions
        LinearLayout fileExtensionColorsLayout = (LinearLayout) findViewById(R.id.fileExtensionsColorsLayout);
        LinearLayout fileExtensionNamesLayout = (LinearLayout) findViewById(R.id.fileExtensionNamesLayout);
        fileExtensionColorsLayout.removeAllViews();
        fileExtensionNamesLayout.removeAllViews();
        fileExtensionColorsLayout.setWeightSum(100);

        Collections.sort(fileExtensions);
        Set<String> fileExtensionKeys = new HashSet<>(fileExtensions);
        Map<String,Long> fileExtensionsMap = new HashMap<>();
        int totalSizeOfExtensions = 0;
        for (String key : fileExtensionKeys) {
            int frequency = Collections.frequency(fileExtensions, key);
            fileExtensionsMap.put(key, (long)frequency);
            //totalSizeOfExtensions += frequency;
        }
        fileExtensionsMap = SdcardUtils.sortByComparator(fileExtensionsMap,false);

        i =1;
        for (Map.Entry<String, Long> entry : fileExtensionsMap.entrySet()) {
            if (i > 5) break;
            totalSizeOfExtensions += entry.getValue();
            i++;
        }
        i=1;
        for (Map.Entry<String, Long> entry : fileExtensionsMap.entrySet()) {
            if (i > 5) break;
                ImageView view = new ImageView(this);
                view.setBackgroundColor(colors[i+1]);
                float weightSum =(float)( entry.getValue() * 100/ totalSizeOfExtensions) ;
                view.setLayoutParams(new LinearLayout.LayoutParams(0, 30,weightSum));
                fileExtensionColorsLayout.addView(view);

                //Setting names
                LayoutInflater inflater = LayoutInflater.from(this);
                View layoutView = inflater.inflate(R.layout.name_color_item, null, false);
                ((TextView)layoutView.findViewById(R.id.nameTextView)).setText("File Extension : " +"."+entry.getKey() +" ("+entry.getValue()+" )");
                ((ImageView)layoutView.findViewById(R.id.colorImageView)).setBackgroundColor(colors[i+1]);
                fileExtensionNamesLayout.addView(layoutView);

                i++;

        }
    }


    private List<File> scanFiles(File parentDir, ScanFilesTask scanFilesTask) {

        List<File> inFiles = new ArrayList<File>();
        File[] files = parentDir.listFiles();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    Log.e(TAG, files[i].getName() + " " + (++directoriesCount));
                    inFiles.addAll(scanFiles(files[i], scanFilesTask));
                } else {
                    inFiles.add(files[i]);
                    maxFilesMap.put(files[i].getName(), files[i].length());
                    fileExtensions.add(SdcardUtils.getFileExtension(files[i].getName()));
                    scanFilesTask.doProgress(++filesCount + "");
                }
            }
        }
        return inFiles;
    }


    class FileExtension implements Comparable<FileExtension> {
        String type;
        int size;

        FileExtension(String c, int s) {
            type = c;
            size = s;
        }

        @Override
        public int compareTo(FileExtension o) {
            return o.size - this.size;
        }
    }

    @Override
    public void onBackPressed() {
        if(scanFilesTask != null){
            // end task right away
           scanFilesTask.cancel(true);
        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }


        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        getListFiles(new File(item.getTitle().toString()));
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

}
