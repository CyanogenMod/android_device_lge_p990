package com.cyanogenmod.LGEServices;

import com.lge.bridge.BridgeService;
import com.lge.secureclock.SecureClockService;

import android.os.ServiceManager;
import android.os.Bundle;

import android.app.Service;

import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import android.util.Log;

public class LGEServices extends Service {

    @Override
    public void onCreate() {
        Log.d("LGEServices","Starting bridge");
        ServiceManager.addService("bridge", new BridgeService(getApplicationContext()));
        Log.d("LGEServices","Starting secureclock");
        ServiceManager.addService("secureclock", new SecureClockService(getApplicationContext()));
    }
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}

