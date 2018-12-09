package com.radinaradionica.quietatwork;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.Manifest;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class MainActivity extends AppCompatActivity {
    private String WiFiName;
    private TextView LabelSelectedWiFi;
    private TextView LabelSSID;
    private Spinner profileListConnect;
    private Spinner profileListDisconnect;
    private Button btnStartStop;
    private Button btnApply;
    private ListView wifiList;
    private boolean serviceStarted = false;
    private WifiManager wifi;
    private final String[] ringerModes = new String[] {"Silent", "Vibrate", "Normal"};
    private HashMap<String, String> currentList = new HashMap<>();
    private final String ITEM_KEY = "key";
    private ArrayList<HashMap<String, String>> arraylist = new ArrayList<>();
    private SimpleAdapter adapter;
    private long appStartTime;
    private static final int APP_START_TIMEOUT = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        /* Setup UI */
        super.onCreate(savedInstanceState);

        /* ...for timeout after which to allow Apply button enabling. */
        appStartTime = System.currentTimeMillis();

        setContentView(R.layout.activity_main);

        btnApply = findViewById(R.id.buttonApply);
        btnApply.setEnabled(false);
        btnApply.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                savePreferences(getApplicationContext());
            }
        });

        LabelSelectedWiFi = findViewById(R.id.textViewSelected);
        LabelSSID = findViewById(R.id.textViewSSID);
        profileListConnect = findViewById(R.id.spinnerConnectProfile);
        profileListConnect.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                if((System.currentTimeMillis() - appStartTime) > APP_START_TIMEOUT) {
                    btnApply.setEnabled(true);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parentView) {}
        });
        profileListDisconnect = findViewById(R.id.spinnerDisconnectProfile);
        profileListDisconnect.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                if((System.currentTimeMillis() - appStartTime) > APP_START_TIMEOUT) {
                    btnApply.setEnabled(true);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parentView) {}
        });

        ArrayAdapter<String> adapterConnect = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, ringerModes);

        adapterConnect.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        profileListConnect.setAdapter(adapterConnect);
        profileListDisconnect.setAdapter(adapterConnect);


        adapter = new SimpleAdapter(this, arraylist, R.layout.listview_row, new String[] {
                ITEM_KEY}, new int[] { R.id.list_value });



        wifiList = findViewById(R.id.listWiFi);
        wifiList.setAdapter(adapter);
        wifiList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View v, int position, long arg3) {
                @SuppressWarnings("unchecked")
                HashMap<String, String> item = (HashMap<String, String>)wifiList.getItemAtPosition(position);

                String itemText = item.get(ITEM_KEY);

                if(WiFiName.compareTo(itemText) != 0) {
                    WiFiName = itemText;
                    LabelSelectedWiFi.setText(getString(R.string.selected_wifi));
                    LabelSSID.setText(WiFiName);
                    btnApply.setEnabled(true);
                }
            }
        });


        Button btnScan = findViewById(R.id.buttonScan);
        btnScan.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scanWifiNetworks();
            }
        });

        /* Setup wifi scanner */
        wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (!wifi.isWifiEnabled())
        {
            Toast.makeText(this, "WiFi is disabled. Enabling it now.", Toast.LENGTH_LONG).show();
            wifi.setWifiEnabled(true);
        }


        /* Setup background service */
        getServiceState(this);


        if(WiFiName.length() > 1) {
            LabelSelectedWiFi.setText(getString(R.string.selected_wifi));
            LabelSSID.setText(WiFiName);
        }else{
            LabelSelectedWiFi.setText(getString(R.string.no_wifi));
        }

        checkPermissions();

        /* Setup start/stop button */
        btnStartStop = findViewById(R.id.buttonStartStop);

        if(serviceStarted){
            btnStartStop.setText(R.string.service_stop);
        }else{
            btnStartStop.setText(R.string.service_start);
        }

        btnStartStop.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                serviceStarted = !serviceStarted;
                setServiceStarted(getApplicationContext());

                if(serviceStarted){
                    btnStartStop.setText(R.string.service_stop);
                    savePreferences(getApplicationContext());
                }else{
                    btnStartStop.setText(R.string.service_start);
                }
            }
        });

    }

    BroadcastReceiver wifi_receiver= new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            boolean success = intent.getBooleanExtra( WifiManager.EXTRA_RESULTS_UPDATED, false);
            if (success) {
                List<ScanResult> results = wifi.getScanResults();
                int size = results.size();
                context.unregisterReceiver(this);

                try
                {
                    while (size > 0)
                    {
                        size--;

                        String ssid = results.get(size).SSID;

                        if(!currentList.containsKey(ssid)) {
                            currentList.put(ssid, "");

                            HashMap<String, String> item = new HashMap<>();
                            item.put(ITEM_KEY, ssid);
                            arraylist.add(item);
                            adapter.notifyDataSetChanged();
                        }
                    }
                }
                catch (Exception e)
                {
                    Log.w("WifScanner", "Exception: "+e);
                }
            } else {
                // scan failure handling
                Log.w("WifScanner", "Failure ");
            }
        }
    };

    private void scanWifiNetworks(){
//        arraylist.clear();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        registerReceiver(wifi_receiver, intentFilter);

        wifi.startScan();

        Toast.makeText(this, "Scanning....", Toast.LENGTH_SHORT).show();
    }

    private void savePreferences(Context context){
        try {
            SharedPreferences prefs= context.getSharedPreferences("com.radinaradionica.quietatwork", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("ssid", WiFiName);
            editor.putInt("on_connect", (int)profileListConnect.getSelectedItemId());
            editor.putInt("on_disconnect", (int)profileListDisconnect.getSelectedItemId());
            editor.putBoolean("last_detected", false);
            editor.apply();

            btnApply.setEnabled(false);

        } catch (NullPointerException e) {

        }
    }

    private void setServiceStarted(Context context){
        try {
            SharedPreferences prefs= context.getSharedPreferences("com.radinaradionica.quietatwork", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();

            editor.putBoolean("persistent", serviceStarted);
            editor.putBoolean("last_detected", false);
            editor.apply();

            AlarmSchedule backgroundService = new AlarmSchedule();
            if(serviceStarted) {
                backgroundService.setAlarm(context);
            }else{
                backgroundService.cancelAlarm(context);
            }

        } catch (NullPointerException e) {

        }
    }

    private void getServiceState(Context context){
        try {
            SharedPreferences prefs= context.getSharedPreferences("com.radinaradionica.quietatwork", Context.MODE_PRIVATE);

            profileListConnect.setSelection(prefs.getInt("on_connect", 0));
            profileListDisconnect.setSelection(prefs.getInt("on_disconnect", 0));
            serviceStarted = prefs.getBoolean("persistent", false);
            WiFiName = prefs.getString("ssid", "");
        } catch (NullPointerException e) {
            Log.e("MainActivity", "error reading preferences: " +e.getMessage());
        }
    }

    private void checkPermissions(){
        NotificationManager notificationManager =
                (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M  && !notificationManager.isNotificationPolicyAccessGranted()) {

            Intent intent = new Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);

            startActivity(intent);
        }

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 0x12345);
        }else{
            scanWifiNetworks();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 0x12345) {
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
            }
            scanWifiNetworks();
        }
    }

}