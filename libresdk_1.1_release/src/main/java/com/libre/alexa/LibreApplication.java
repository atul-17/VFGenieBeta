package com.libre.alexa;
/*********************************************************************************************
 * Copyright (C) 2014 Libre Wireless Technology
 * <p/>
 * "Junk Yard Lab" Project
 * <p/>
 * Libre Sync Android App
 * Author: Subhajeet Roy
 ***********************************************************************************************/

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import androidx.multidex.MultiDex;
import android.util.Log;

import com.google.common.collect.ImmutableList;
import com.google.firebase.BuildConfig;
import com.libre.alexa.Ls9Sac.GcastUpdateData;
import com.libre.alexa.Scanning.Constants;
import com.libre.alexa.Scanning.ScanThread;
import com.libre.alexa.Scanning.ScanningHandler;
import com.libre.alexa.app.dlna.dmc.server.MusicServer;
import com.libre.alexa.app.dlna.dmc.utility.PlaybackHelper;
import com.libre.alexa.luci.LSSDPNodeDB;
import com.libre.alexa.luci.LUCIControl;
import com.libre.alexa.util.GoogleTOSTimeZone;
import com.libre.alexa.util.LibreLogger;
import com.vodafone.idtmlib.EnvironmentType;
import com.vodafone.idtmlib.IdtmLib;
import com.vodafone.idtmlib.IdtmLibInjector;
//import com.vodafone.idtmlib.EnvironmentType;
//import com.vodafone.idtmlib.IdtmLib;
//import com.vodafone.idtmlib.IdtmLibInjector;

import org.fourthline.cling.controlpoint.ControlPoint;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.SocketException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;


//import cumulations.cutekit.CuteKit;

public class LibreApplication extends Application implements IdtmLibInjector {

    private static LibreApplication sInstance = null;
    public static boolean haveShowPropritaryGooglePopup=false;
    public static boolean mCleanUpIsDoneButNotRestarted=false;
    public static boolean isDmrOtherPlayback=false;
    public static boolean mStoragePermissionGranted = false;
    public static boolean mLocationPermissionGranted = false;
    public static boolean is3PDAEnabled = false;
    public static final String MY_MODEL = android.os.Build.MODEL;
    public static Handler commandQueuHandler;
    public static String getMYPEMstring="";
    public static short deviceRemoteID=0;
    public static boolean GOOGLE_TOS_ACCEPTED = false;
    public static String thisSACDeviceNeeds226="";
    public static String thisSACConfiguredFromThisApp = "";
    public int m_screenstate;
    public static boolean mLuciThreadInitiated = false;
    public static String alreadyUpdatedLocale="";
    public static boolean spanishLang=false;
    public static boolean englishLang=false;
    public static boolean germanLang=false;
    public static boolean frenchLang=false;
    public static boolean italianLang=false;


    public static int activityOnUiIndex = 0;
    public boolean shouldShowFirmWareDialogue = true;

    public boolean shouldShowFirmWareDialogue() {
        return shouldShowFirmWareDialogue;
    }
    public void setShouldShowFirmWareDialogue(boolean val){
        shouldShowFirmWareDialogue = val;
    }

    public static boolean hideErrorMessage;
    private static final String TAG = LibreApplication.class.getSimpleName();
    //private UpnpDeviceManager deviceManager = new UpnpDeviceManager();
    private int imageViewSize = 700;
    private String currentdmrDeviceUdn = "";
    private String currentdevicename = "";
    public static String SSID;

    private boolean isPlayNewSong = false;
    public static String activeSSID;
    public static String mActiveSSIDBeforeWifiOff;
    public static boolean needToShowNotification3800 = false;
    public static boolean needToShowNotification4000 = false;
    public static boolean needToShowNotification1400= false;

    public static boolean doneLocationChange = false;

    public static boolean userDeniedAppPermission = false;

    public static boolean wifiConnected = true;


    /*this map is having all devices volume(64) based on IP address*/
    public static HashMap<String, Integer> INDIVIDUAL_VOLUME_MAP = new HashMap<>();


    public static HashMap<String, Integer> INDIVIDUAL_RSSI_MAP = new HashMap<>();

    public static HashMap<String, GoogleTOSTimeZone> GOOGLE_TIMEZONE_MAP= new HashMap<>();

    public static LinkedHashMap<String,GcastUpdateData> GCAST_UPDATE_AVAILABE_LIST_DATA = new LinkedHashMap<>();

    /*this map is having zone volume(219) only for master to avoid flickering in active scene*/
    public static HashMap<String, Integer> ZONE_VOLUME_MAP = new HashMap<>();

    public String getDeviceIpAddress() {
        return deviceIpAddress;
    }

    public void setDeviceIpAddress(String deviceIpAddress) {
        this.deviceIpAddress = deviceIpAddress;
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
    }

    private String deviceIpAddress;


    /**
     * **************************************************************
     */
    ScanThread wt = null;
    Thread scanthread = null;

    /**
     * *****************************************************************
     */

    public String getCurrentDmrDeviceUdn() {
        return currentdmrDeviceUdn;
    }

    public void setCurrentDmrDeviceUdn(String dmrDeviceUdn) {
        this.currentdmrDeviceUdn = dmrDeviceUdn;
    }

    public void setSpeakerName(String DevName) {
        this.currentdevicename = DevName;
    }

    public String getSpeakerName() {
        return currentdevicename;
    }
    public static boolean getIs3PDAEnabled(){
        return is3PDAEnabled;
    }

    public boolean isPlayNewSong() {
        return isPlayNewSong;
    }

    public void setPlayNewSong(boolean isPlayNewSong) {
        this.isPlayNewSong = isPlayNewSong;
    }
    public void initiateServices(){
        initLUCIServices();

    }

    private IdtmLib idtmLib;

    public static final String APIX_CLIENT_ID = "FPQ8cZpVQQRjOD51VAH73GvQJTvXOGwe";
    public static final String IDGATEWAY_REDIRECT_URL = "https://cloud.librewireless.com/oauth/redirect/";
    public static final List<String> ID_GATEWAY_ACR_MOBILE =  ImmutableList.of("urn:vodafone:loa:silver:pwd");
    public static final List<String>ID_GATEWAY_ACR_WIFI =  ImmutableList.of("urn:vodafone:loa:silver:pwd");
    public static final List<String>ID_GATEWAY_SCOPE =ImmutableList.of("openid","offline_access","OPENID_TOKEN_SHARING_PRODUCER");
    public static final String LOGIN_HINT = "OPCO:GB";


    @Override
    public IdtmLib getIdtmLib() {
        return idtmLib;
    }

    public static IdtmLib getIdtmLib(Context context) {
        return ((LibreApplication) context.getApplicationContext()).idtmLib;
    }


    boolean mExecuted = false;
    @Override
    public void onCreate() {
        // TODO Auto-generated method stub
        Log.i(TAG, "onCreated");
        super.onCreate();
        //pre_pod - development stage
        //prod- producton stage
        this.idtmLib = new IdtmLib(this, EnvironmentType.PRE_PROD,true);
        this.idtmLib.setupSmapi(this, true);

        sInstance = this;
        setImageViewSize();
        isPlayNewSong = false;
        final ConnectivityManager connection_manager =
                (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        InputStream input = null;
        try {
            input = getApplicationContext().getAssets().open("server.pem");
        } catch (IOException e) {
            e.printStackTrace();
        }
        //symmentrickey_of_productid.txt can't be more than 2 gigs.
        int size = 0;
        try {
            if (input != null) {
                size = input.available();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        byte[] buffer = new byte[size];
        try {
            if (input != null) {
                input.read(buffer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            input.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // byte buffer into a string
        String text = new String(buffer);
        text = text.replaceAll("\n", "");
        LibreLogger.d(this, "msg digest symmentric  standard product ID mavidapplication" + text);
        text = text.replace("-----BEGIN CERTIFICATE-----", "");
        text = text.replace("-----END CERTIFICATE-----", "");

        LibreLogger.d(this, "msg digest symmentric  standard product ID mavidapplication2" + text);
        LibreApplication.getMYPEMstring=text;

        NetworkRequest.Builder request = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP && !mExecuted) {
            request = new NetworkRequest.Builder();

            request.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);

            connection_manager.registerNetworkCallback(request.build(), new ConnectivityManager.NetworkCallback() {

                @Override
                public void onAvailable(Network network) {
                    //if(isMobileDataEnabled()){
                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
                        if(isMobileDataEnabled()) {
                            ConnectivityManager.setProcessDefaultNetwork(network);
                            LibreLogger.d(this,"suma in disable nw first");

                        }
                        else{
                            ConnectivityManager.setProcessDefaultNetwork(network);
                            LibreLogger.d(this,"suma in disable nw sec");

                        }
                    }

                    // }

//                    else{
//                        LibreLogger.d(this,"suma in disable nw noooo");
//
//                    }

                    WifiManager wifiMan = (WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                    WifiInfo wifiInf = wifiMan.getConnectionInfo();
                    int ipAddress = wifiInf.getIpAddress();
                    String ip = String.format("%d.%d.%d.%d", (ipAddress & 0xff),(ipAddress >> 8 & 0xff),(ipAddress >> 16 & 0xff),(ipAddress >> 24 & 0xff));
                    LibreApplication.LOCAL_IP = ip;
                    initiateServices();
                    LibreLogger.d(this,"Karuna" + "App Called Here Device");

                }
            });


        }else{
            LibreLogger.d(this,"Karuna" + "App Called Here Device 1" );
            WifiManager wifiMan = (WifiManager)getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInf = wifiMan.getConnectionInfo();
            int ipAddress = wifiInf.getIpAddress();
            String ip = String.format("%d.%d.%d.%d", (ipAddress & 0xff),(ipAddress >> 8 & 0xff),(ipAddress >> 16 & 0xff),(ipAddress >> 24 & 0xff));
            LibreApplication.LOCAL_IP = ip;
            initiateServices();

        }

        musicServer = getMusicServer();
        //luciReceiver= LuciReceiver.getInstance();

        /*CommandHnadler commandHnadler = new CommandHnadler();
        commandHnadler.start();
        commandQueuHandler=new Handler(commandHnadler.getLooper());*/


        if (BuildConfig.DEBUG) {

            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()   // or .detectAll() for all detectable problems
                    .penaltyLog()
                    .build());

            /* Writng Logs into a File in Background*/

/*            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .penaltyDeath()
                    .build());*//*
             */
        }

      /*  Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler(){

            @Override
            public void uncaughtException(Thread paramThread, Throwable paramThrowable) {
                myHandlingWhenAppForceStopped (paramThread, paramThrowable);
            }
        });*/
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle bundle) {

            }

            @Override
            public void onActivityStarted(Activity activity) {

                activityOnUiIndex++;
            }

            @Override
            public void onActivityResumed(Activity activity) {

                if(wt==null){
                    initLUCIServices();
                }else if (!wt.isSocketAlive()){
                    initLUCIServices();
                }
            }

            @Override
            public void onActivityPaused(Activity activity) {

            }


            @Override
            public void onActivityStopped(Activity activity) {

                activityOnUiIndex--;
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {

            }

            @Override
            public void onActivityDestroyed(Activity activity) {

            }
        }) ;



    }

    public static LibreApplication getInstance() {
        return sInstance ;
    }

    private void myHandlingWhenAppForceStopped(Thread paramThread, Throwable paramThrowable) {
        Log.e("Alert", "Lets See if it Works !!!" +
                "paramThread:::" + paramThread +
                "paramThrowable:::" + paramThrowable);
        /* Killing our Android App with The PID For the Safe Case */
        int pid = android.os.Process.myPid();
        android.os.Process.killProcess(pid);
        System.exit(0);

    }

    private boolean isMobileDataEnabled() {
        boolean mobileDataEnabled = false;
        ConnectivityManager cm1 = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info1 = null;
        if (cm1 != null) {
            info1 = cm1.getActiveNetworkInfo();
        }
        if (info1 != null) {
            if (info1.getType() == ConnectivityManager.TYPE_MOBILE) {
                try {
                    Class cmClass = Class.forName(cm1.getClass().getName());
                    Method method = cmClass.getDeclaredMethod("getMobileDataEnabled");
                    method.setAccessible(true);
                    mobileDataEnabled = (Boolean) method.invoke(cm1);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }
        return mobileDataEnabled;
    }
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    /* Created by Karuna, To fix the RestartApp Issue
     * * Till i HAave to Analyse more on this code */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public void restartApp(Context context) {

        /* Stopping ForeGRound Service Whenwe are Restarting the APP
         * but as now commenting it since it is not required for genie
         * */
//        Intent in = new Intent(getApplicationContext(), DMRDeviceListenerForegroundService.class);
//        in.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        in.setAction(Constants.ACTION.STOPFOREGROUND_ACTION);
//        stopService(in);

//        Intent mStartActivity = new Intent(context, SpalshScreenActivity.class);
//        /*sending to let user know that app is restarting*/
//        mStartActivity.putExtra(SpalshScreenActivity.APP_RESTARTING, true);
//        int mPendingIntentId = 123456;
//        PendingIntent mPendingIntent = PendingIntent.getActivity(context, mPendingIntentId, mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
//        AlarmManager mgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
//        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 200, mPendingIntent);
//
//
//        /* Killing our Android App with The PID For the Safe Case */
//        int pid = android.os.Process.myPid();
//        android.os.Process.killProcess(pid);
//        System.exit(0);

        //restarting the app
        PackageManager packageManager = context.getPackageManager();
        Intent intent = packageManager.getLaunchIntentForPackage(context.getPackageName());
        if (intent != null) {
            ComponentName componentName = intent.getComponent();
            Intent mainIntent = Intent.makeRestartActivityTask(componentName);
            context.startActivity(mainIntent);
            Runtime.getRuntime().exit(0);
        }


    }
    private void setImageViewSize() {
        // TODO Auto-generated method stub
        int width = getResources().getDisplayMetrics().widthPixels;
        imageViewSize = (int) (width * 0.75f);
    }

    public void initLUCIServices() {
        try {
            wt =ScanThread.getInstance();
            wt.setmContext(getApplicationContext());

            LibreLogger.d(this,"Karuna mRunning " + mLuciThreadInitiated);
            if(!LibreApplication.mLuciThreadInitiated) {
                scanthread = new Thread(wt);
                scanthread.start();
                LibreApplication.mLuciThreadInitiated = true;
            }
        } catch (Exception e) {

            e.printStackTrace();
        }


    }
    public void closeScanThread(){
        wt.close();
    }
    public ScanThread getScanThread() {

        /* If Scan Thread is Not Started Because of Some Reasons, we are restarrting it .
         * " Creating the Multiple Threads because we are creating a new Objects " Bug Number : 2968 */
      /* if(wt == null  || (scanthread.getState()!= Thread.State.RUNNABLE
        && scanthread.getState()!= Thread.State.NEW)){

            initLUCIServices();
            LibreLogger.d(this, "ScanThread is Status" + scanthread.getState());
        }*/

        return wt;

    }

    /**
     * clearing all collections related to application
     */
    public void clearApplicationCollections() {
        try {
            Log.d("Scan_Netty", "clearApplicationCollections() called with: " + "");
            PLAYBACK_HELPER_MAP.clear();
            INDIVIDUAL_VOLUME_MAP.clear();
            ZONE_VOLUME_MAP.clear();
            LUCIControl.luciSocketMap.clear();
            LSSDPNodeDB.getInstance().clearDB();
            ScanningHandler.getInstance().clearSceneObjectsFromCentralRepo();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void LSSDPScan() {
        wt.UpdateNodes();
        return;
    }

    public synchronized void restart() throws SocketException {

        wt.close();

        try {


            scanthread = new Thread(wt);
            scanthread.start();


        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * This hashmap stores the DMRplayback helper for a udn
     */
    public static HashMap<String, PlaybackHelper> PLAYBACK_HELPER_MAP = new HashMap<String, PlaybackHelper>();
    public static String LOCAL_UDN = "";
    public static String LOCAL_IP = "";
    /* main Music server */
    private MusicServer musicServer;
    private ControlPoint controlPoint;


    public MusicServer getMusicServer() {
        return MusicServer.getMusicServer();
    }

    public void setControlPoint(ControlPoint controlPoint) {
        this.controlPoint = controlPoint;
    }


    public static boolean isApplicationActiveOnUI(){
        if (activityOnUiIndex>0)
            return true;
        else
            return false;
    }




}
