package com.radinaradionica.quietatwork;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.util.Log;
import java.util.List;

public class AlarmSchedule extends BroadcastReceiver
{
    private String preferredWiFiSSID;           /* SSID of the wifiHandle network to scan for */
    private int onConnectProfileId = 0;         /* ID of the sound profile to switch to when preferred wifiHandle ssid is detected */
    private int onDisconnectProfileId = 0;      /* ID of the sound profile to switch to when preferred wifiHandle ssid is no longer detected */
    private boolean keepRunningFlag = false;    /* Whether to stop recurrence of the alarm */
    private boolean lastDetected = false;       /* Whether the ssid was previously detected or not */
    private WifiManager wifiHandle;             /* handle to help access the system wifi service */
    private static final int alarmInterval = 30000;

    @Override
    public void onReceive(Context context, Intent intent)
    {/* The alarm service has been called by the system */

        /* Read saved preferences */
        getPreferences(context);

        if(keepRunningFlag){    /* The service is active */

            wifiHandle = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

            if (wifiHandle.isWifiEnabled()) {
                /* Prepare the receiver to accept the wifi scan list. */
                IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
                context.getApplicationContext().registerReceiver(wifiReceiver, intentFilter);

                wifiHandle.startScan();

                Log.d("Alarm", "Scanning....");
            }
        }else {
            /* Request was made to stop the alarm service */
            cancelAlarm(context);
        }
    }

    public void setAlarm(Context context)
    {
        Log.d("AlarmSchedule", "Starting recurring alarm service");

        AlarmManager alarmManager =( AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        Intent alarmIntent = new Intent(context, AlarmSchedule.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, alarmIntent, 0);
        alarmManager.setInexactRepeating(AlarmManager.RTC, System.currentTimeMillis(), alarmInterval, pi);

    }

    public void cancelAlarm(Context context)
    {
        Log.d("AlarmSchedule", "Stopping recurring alarm service");

        Intent alarmIntent = new Intent(context, AlarmSchedule.class);
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, alarmIntent, 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(sender);

    }

    /* The object to receive wifi scan results */
    BroadcastReceiver wifiReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            /* Get the status of the last scan. */
            boolean success = intent.getBooleanExtra( WifiManager.EXTRA_RESULTS_UPDATED, false);
            if (success) {  /* wifi scan was successful */

                /* Unregister the wifi receiver so it can be again registered to the new instance of the scanner */
                context.getApplicationContext().unregisterReceiver(this);

                List<ScanResult> results = wifiHandle.getScanResults();
                int size = results.size();

                try
                {   /* Check if the preferred wifi ssid is among the scan results */

                    boolean detectedPreferredWifiFlag = false;

                    while (size > 0)
                    {
                        size--;

                        String ssid = results.get(size).SSID;

                        if(ssid.compareTo(preferredWiFiSSID) == 0){
                            detectedPreferredWifiFlag = true;
                            break;
                        }
                    }

                    if(lastDetected != detectedPreferredWifiFlag) {
                        /* Detection of the preferred wifi network has changed. */

                        /* Prepare the handle of the system audio control service */
                        AudioManager audioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);

                        if (detectedPreferredWifiFlag){
                            audioManager.setRingerMode(onConnectProfileId);
                        } else {
                            audioManager.setRingerMode(onDisconnectProfileId);
                        }

                        /* Make note that the detection state has changed */
                        setLastState(context, detectedPreferredWifiFlag);
                    }
                }
                catch (Exception e)
                {
                    Log.w("AlarmSchedule", "Exception: "+e);
                }
            }
        }
    };

    private void getPreferences(Context context){
        try {
            SharedPreferences prefs= context.getSharedPreferences("com.radinaradionica.quietatwork", Context.MODE_PRIVATE);

            preferredWiFiSSID = prefs.getString("ssid", "");
            onConnectProfileId = prefs.getInt("on_connect", 0);
            onDisconnectProfileId = prefs.getInt("on_disconnect", 1);
            keepRunningFlag = prefs.getBoolean("persistent", false);
            lastDetected = prefs.getBoolean("last_detected", false);

        } catch (NullPointerException e) {
            Log.e("AlarmSchedule", "error reading preferences: " +e.getMessage());
        }
    }

    private void setLastState(Context context, boolean state){
        try {
            SharedPreferences prefs= context.getSharedPreferences("com.radinaradionica.quietatwork", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("last_detected", state);
            editor.apply();
        } catch (NullPointerException e) {
            Log.e("AlarmSchedule", "error in setLastState: " +e.getMessage());
        }
    }

}
