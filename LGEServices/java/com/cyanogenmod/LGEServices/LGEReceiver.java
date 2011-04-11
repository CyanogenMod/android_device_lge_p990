package com.cyanogenmod.LGEServices;

import com.lge.bridge.BridgeService;
import com.lge.secureclock.SecureClockService;

import android.content.Context;
import android.content.Intent;
import android.content.BroadcastReceiver;

import android.util.Log;

public class LGEReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d("LGEServices","Received an intent");
        
        if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            Intent launch = new Intent(context, LGEServices.class);
            context.startService(launch);
        }
   }
}

