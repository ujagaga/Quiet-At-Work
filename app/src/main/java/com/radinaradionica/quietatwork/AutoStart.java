package com.radinaradionica.quietatwork;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AutoStart extends BroadcastReceiver
{
    AlarmSchedule backgroundService = new AlarmSchedule();

    @Override
    public void onReceive(Context context, Intent intent)
    {
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED))
        {
            backgroundService.setAlarm(context);
        }
    }
}
