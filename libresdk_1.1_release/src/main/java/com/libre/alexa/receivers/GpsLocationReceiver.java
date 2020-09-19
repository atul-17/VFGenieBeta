package com.libre.alexa.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.util.Log;

import com.libre.alexa.LibreApplication;
import com.libre.alexa.util.LibreLogger;

public class GpsLocationReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager != null) {
            boolean isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            if (!isGpsEnabled && !isNetworkEnabled) {
                LibreApplication.doneLocationChange = false;
                LibreLogger.d(this,"suma is n/w change not enabled");
            }else{
                LibreApplication.doneLocationChange = true;
                LibreLogger.d(this,"suma is n/w change enabled true");
            }
        }
    }
}