package com.vodafone.idtmlib.lib.utils;


import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.provider.Settings;

import com.vodafone.idtmlib.BuildConfig;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class Device {
    private String userAgent;
    private String androidUuid;
    private ConnectivityManager connectivityManager;

    @Inject
    public Device(Context context, ConnectivityManager connectivityManager) {
        this.connectivityManager = connectivityManager;
        // calculating User Agent
        this.userAgent = new StringBuilder()
                .append(BuildConfig.APP_CODENAME)
                .append("/")
                .append(BuildConfig.VERSION_NAME)
                .append(" (Linux; Android ")
                .append(Build.VERSION.RELEASE)
                .append("; ")
                .append(Build.MODEL)
                .append(" Build/")
                .append(Build.ID)
                .append(")")
                .toString();
        // calculating Android UUID
        String androidId = Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.ANDROID_ID);
        byte[] androidIdBytes;
        androidIdBytes = androidId.getBytes(StandardCharsets.UTF_8);
        this.androidUuid = UUID.nameUUIDFromBytes(androidIdBytes).toString();
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getAndroidUuid() {
        return androidUuid;
    }

    public static String getOSVersion() {
        //Ensure that device version is always have length in between 3 and 256 expected by the backend.
        StringBuilder version = new StringBuilder();
        version.append(Build.VERSION.RELEASE);
        while (version.length() < 3) {
            version.append(".0");
        }
        return version.toString();
    }

    public static String getDeviceModel() {
        //Ensure that device version is always have length in between 3 and 256 expected by the backend.
        StringBuilder model = new StringBuilder();
        model.append(Build.MODEL);
        while (model.length() < 3) {
            model.append(".");
        }
        return model.toString();
    }

    public static boolean checkNetworkConnection(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isAvailable() && cm.getActiveNetworkInfo().isConnected();
    }

    public boolean isConnectedViaWifi() {
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        if (activeNetwork != null && activeNetwork.isConnected()) {
            return activeNetwork.getType() == ConnectivityManager.TYPE_WIFI;
        } else {
            return false;
        }
    }
}
