package com.libre.alexa.nowplaying;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.widget.Toolbar;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.libre.alexa.ActiveSceneAdapter;
import com.libre.alexa.ActiveScenesListActivity;
import com.libre.alexa.DeviceDiscoveryActivity;
import com.libre.alexa.LErrorHandeling.LibreError;
import com.libre.alexa.LibreApplication;
import com.libre.alexa.R;
import com.libre.alexa.Scanning.Constants;
import com.libre.alexa.Scanning.ScanningHandler;
import com.libre.alexa.SceneObject;
import com.libre.alexa.TuneInRemoteSourcesList;
import com.libre.alexa.VolumeReceiver;
import com.libre.alexa.app.dlna.dmc.LocalDMSActivity;
import com.libre.alexa.app.dlna.dmc.utility.PlaybackHelper;
import com.libre.alexa.app.dlna.dmc.utility.UpnpDeviceManager;
import com.libre.alexa.constants.DeviceMasterSlaveFreeConstants;
import com.libre.alexa.constants.LSSDPCONST;
import com.libre.alexa.constants.LUCIMESSAGES;
import com.libre.alexa.constants.MIDCONST;
import com.libre.alexa.luci.LSSDPNodeDB;
import com.libre.alexa.luci.LSSDPNodes;
import com.libre.alexa.luci.LUCIControl;
import com.libre.alexa.luci.LUCIPacket;
import com.libre.alexa.netty.LibreDeviceInteractionListner;
import com.libre.alexa.netty.NettyData;
import com.libre.alexa.util.LibreLogger;

import org.fourthline.cling.model.meta.RemoteDevice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by khajan on 1/8/15.
 */
public class NowPlayingActivity extends DeviceDiscoveryActivity implements LibreDeviceInteractionListner, VolumeListener {
    public static boolean isPhoneCallbeingReceived;



    private ArrayList<String> sceneAddressList = new ArrayList<>();
    private int position;
    TextView mDevices, mSources;
    ViewPager nowPlayingViewPager;

    private int mCurrentNowPlayingScene;
    Intent mActiveScenesList;

    private ImageButton m_back;
    private ImageButton homeButton;
    VolumeReceiver volumeReceiver;

    private AudioManager m_audioManager;
//    MyPagerAdapter myPagerAdapter;
//    List<OnChangeVolumeHardKeyListener> hardKeyListenersList = new ArrayList<>();

    private HashMap<Integer, NowPlayingFragment> mPageReferenceMap = new HashMap<>();


    ScanningHandler m_ScanHandler = ScanningHandler.getInstance();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_now_playing);

        m_audioManager = (AudioManager) getSystemService(Service.AUDIO_SERVICE);

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);


        homeButton = (ImageButton) findViewById(R.id.homebutton);
        homeButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub

                LibreLogger.d(this, "user pressed home button ");
                //   luciControl.SendCommand(MIDCONST.MID_REMOTE_UI, GET_HOME, LSSDPCONST.LUCI_SET);

                Intent intent = new Intent(NowPlayingActivity.this, ActiveScenesListActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
        });

        m_back = (ImageButton) findViewById(R.id.back);
        m_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sceneAddressList.size() > 0
                        && mCurrentNowPlayingScene >= 0 && mCurrentNowPlayingScene < sceneAddressList.size()) {
                    SceneObject sceneObject = m_ScanHandler.getSceneObjectFromCentralRepo(sceneAddressList.get(mCurrentNowPlayingScene));
                    if (sceneObject == null)
                        return;
                    if (getSourceIndex(sceneObject.getCurrentSource())==2){
                        RemoteDevice renderingDevice = UpnpDeviceManager.getInstance().getRemoteDMRDeviceByIp( sceneAddressList.get(mCurrentNowPlayingScene));
                        if (renderingDevice != null) {
                            String renderingUDN = renderingDevice.getIdentity().getUdn().toString();
                            PlaybackHelper playbackHelper = LibreApplication.PLAYBACK_HELPER_MAP.get(renderingUDN);
                            if ( playbackHelper==null|| playbackHelper.getDmsHelper() == null) {
                            /*    when we play from DMR and kill the app and open again. The source is 2(DMR).
                                        At that instance playbackHelper or playbackHelper.getDmsHelper() is null.
                                        So navigate to activescenes.*/
                                onBackPressed();
                            }else{
                                Intent localIntent = new Intent(NowPlayingActivity.this, LocalDMSActivity.class);
                                localIntent.putExtra("isLocalDeviceSelected", true);
                                localIntent.putExtra("current_ipaddress", sceneAddressList.get(mCurrentNowPlayingScene));
                                localIntent.putExtra("fromActivity","Nowplaying");
                                startActivity(localIntent);
                                finish();
                            }
                        }else{
                            onBackPressed();
                        }

                    }else if (sceneObject!=null && getSourceIndex(sceneObject.getCurrentSource()) >= 0) {
                        Intent intent = new Intent(NowPlayingActivity.this, TuneInRemoteSourcesList.class);
                        intent.putExtra("current_ipaddress", sceneAddressList.get(mCurrentNowPlayingScene));
                        intent.putExtra("current_source_index_selected", getSourceIndex(sceneObject.getCurrentSource()));
                        startActivity(intent);
                    } else {
                        onBackPressed();
                    }
                }
            }
        });


        Set<Map.Entry<String, SceneObject>> entries = m_ScanHandler.getSceneObjectFromCentralRepo().entrySet(); //ActiveSceneAdapter.mMasterSpecificSlaveAndFreeDeviceMap.entrySet();
        Iterator<Map.Entry<String, SceneObject>> iterator = entries.iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, SceneObject> next = iterator.next();


            /* This is needed to make sure the unwanted free device is not coming */
            LSSDPNodes node = LSSDPNodeDB.getInstance().getTheNodeBasedOnTheIpAddress(next.getValue().getIpAddress());
        /*    if (node != null) {

                if ((node.getDeviceState() != null) && (node.getDeviceState().contains("F") || node.getDeviceState().contains("S"))) {
                    m_ScanHandler.removeSceneMapFromCentralRepo(node.getIP());
                    continue;
                }

            }*/
            /*if((node.getCurrentSource() == MIDCONST.GCAST_SOURCE)
                    && (node.getmPlayStatus() == SceneObject.CURRENTLY_PLAYING)){

            }else*/{
                sceneAddressList.add(next.getKey());
            }
          /*  if((node.getCurrentSource()== MIDCONST.GCAST_SOURCE)
                    && (node.getmPlayStatus()!=SceneObject.CURRENTLY_PLAYING)
                    )
                */
        }
        try {
            String m_CurrentIpadddress = getIntent().getExtras().getString("current_ipaddress");
            if (m_CurrentIpadddress == null) {
                LibreError error = new LibreError("Oops!", "ip address is not present");
                showErrorMessage(error);
				return;
            }
            mCurrentNowPlayingScene = sceneAddressList.indexOf(m_CurrentIpadddress);
        } catch (Exception e) {
            if (getIntent().hasExtra(ActiveSceneAdapter.SCENE_POSITION)) {
                /*Modifiefd by Praveen for Redirecting the screen to Nowplaying screen */
                int position = getIntent().getExtras().getInt(ActiveSceneAdapter.SCENE_POSITION);
                mCurrentNowPlayingScene = position;
            } else {
                String m_CurrentIpadddress = getIntent().getExtras().getString("current_ipaddress");
                mCurrentNowPlayingScene = sceneAddressList.indexOf(m_CurrentIpadddress);
            }
        }

      /*  if (getIntent().hasExtra(ActiveSceneAdapter.SCENE_POSITION)) {
                 *//*Modifiefd by Praveen for Redirecting the screen to Nowplaying screen *//*
            int position = getIntent().getExtras().getInt(ActiveSceneAdapter.SCENE_POSITION);
            mCurrentNowPlayingScene = position;
        } else {
            String m_CurrentIpadddress = getIntent().getExtras().getString("current_ipaddress");
            mCurrentNowPlayingScene = sceneAddressList.indexOf(m_CurrentIpadddress);
        }*/


        nowPlayingViewPager = (ViewPager) findViewById(R.id.now_playing_viewpager);

        nowPlayingViewPager.setAdapter(new MyPagerAdapter(getSupportFragmentManager(), sceneAddressList.size()));

        nowPlayingViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i2) {
                mCurrentNowPlayingScene = i;

            }

            @Override
            public void onPageSelected(int i) {


            }

            @Override
            public void onPageScrollStateChanged(int i) {
            }
        });
        nowPlayingViewPager.setCurrentItem(mCurrentNowPlayingScene);

        /*last && condition to avoid IndexOutOfBoundException*/
        if (sceneAddressList.size() > 0 && mCurrentNowPlayingScene >= 0 && mCurrentNowPlayingScene < sceneAddressList.size()) {
            LUCIControl mLuciControl = new LUCIControl(sceneAddressList.get(mCurrentNowPlayingScene));
            mLuciControl.sendAsynchronousCommand();
            mLuciControl.SendCommand(50, null, LSSDPCONST.LUCI_GET);
        } else {
            onBackPressed();
        }


        PhoneStateChangeListener pscl = new PhoneStateChangeListener();
        TelephonyManager tm = (TelephonyManager)this.getSystemService(Context.TELEPHONY_SERVICE);
        tm.listen(pscl, PhoneStateListener.LISTEN_CALL_STATE);



    }

    public void setSceneName(){
        try {
            SceneObject sceneObject = ScanningHandler.getInstance().getSceneObjectFromCentralRepo(sceneAddressList.get(mCurrentNowPlayingScene));
            TextView sceneNameView = (TextView) findViewById(R.id.choosesong);
            sceneNameView.setText(sceneObject.getSceneName());
            sceneNameView.setText(sceneObject.getSceneName());
            sceneNameView.setEnabled(true);
            sceneNameView.setSelected(true);
        }catch (Exception e){
            LibreLogger.d(this,"Set scenename exception "+e.getMessage()+" "+e.getStackTrace().toString());
        }
    }

    /*this method will return current index */
    private int getSourceIndex(int currentSource) {
        int current_source_index_selected = -1;
        switch (currentSource) {
            case NowPlayingFragment.NETWORK_DEVICES:
                /*Network ID*/
                current_source_index_selected = 0;
                break;

            case NowPlayingFragment.DMR_SOURCE:
                /*FAV Current*/
                current_source_index_selected = 2;
                break;

            case NowPlayingFragment.USB_SOURCE:
                /*USB current*/
                current_source_index_selected = 3;

                break;
            case NowPlayingFragment.SD_CARD:
                /*SD card Current*/
                current_source_index_selected = 4;
                break;
            case NowPlayingFragment.VTUNER_SOURCE:
                /*VTUNER Current*/
                current_source_index_selected = 1;

                break;
            case NowPlayingFragment.TUNEIN_SOURCE:
                /*TUNEIN Current*/
                current_source_index_selected = 2;
                break;

            case NowPlayingFragment.DEEZER_SOURCE:
                current_source_index_selected = 5;
                /*DEEZER*Current*/
                break;
            case NowPlayingFragment.TIDAL_SOURCE:
                /*TIDAL Current*/
                current_source_index_selected = 6;
                break;

            case NowPlayingFragment.FAV_SOURCE:
                /*FAV Current*/
                current_source_index_selected = 7;
                break;

            default:
                current_source_index_selected = -1;
                break;
        }
        return current_source_index_selected;
    }


    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int action = event.getAction();
        int keyCode = event.getKeyCode();
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                if (action == KeyEvent.ACTION_DOWN) {

                    MyPagerAdapter adapter = ((MyPagerAdapter) nowPlayingViewPager.getAdapter());
                    OnChangeVolumeHardKeyListener volumeKeyListener = adapter.getFragment(nowPlayingViewPager.getCurrentItem());
                    if (volumeKeyListener != null)
                        volumeKeyListener.updateVolumeChangesFromHardwareKey(KeyEvent.KEYCODE_VOLUME_UP);


                }
                return true;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if (action == KeyEvent.ACTION_DOWN) {

                    MyPagerAdapter adapter = ((MyPagerAdapter) nowPlayingViewPager.getAdapter());
                    OnChangeVolumeHardKeyListener volumeKeyListener = adapter.getFragment(nowPlayingViewPager.getCurrentItem());
                    if (volumeKeyListener != null)
                        volumeKeyListener.updateVolumeChangesFromHardwareKey(KeyEvent.KEYCODE_VOLUME_DOWN);


                }
                return true;
            default:
                return super.dispatchKeyEvent(event);
        }

    }


    public void StartLSSDPScan() {

        final LibreApplication application = (LibreApplication) getApplication();
        application.getScanThread().clearNodes();
        application.getScanThread().UpdateNodes();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                application.getScanThread().UpdateNodes();
            }
        }, 150);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                application.getScanThread().UpdateNodes();

            }
        }, 650);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                application.getScanThread().UpdateNodes();

            }
        }, 1150);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        /* case R.id.DropThisScene:{
                if (mCurrentNowPlayingScene >= sceneAddressList.size()) {
                    LibreLogger.d(this, "IndexOutOfBoundException occurred while releasing scene");
                    if (m_ScanHandler.getSceneObjectFromCentralRepo().size() <= 0) {
                        Intent intent = new Intent(NowPlayingActivity.this, PlayNewActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        Intent intent = new Intent(NowPlayingActivity.this, ActiveScenesListActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    }
                    return true;
                }

                final LSSDPNodes mMasternode = m_ScanHandler.getLSSDPNodeFromCentralDB(sceneAddressList.get(mCurrentNowPlayingScene));//(sceneAddressList.get(mCurrentNowPlayingScene));
                if (mMasternode == null) {
                    Toast.makeText(this, "Opps!Master not found", Toast.LENGTH_LONG).show();
                    return true;
                }
                Bundle b = new Bundle();
               */
        /*ArrayList<LSSDPNodes> mSlaveListForMyMaster = m_ScanHandler.getSlaveListForMasterIp(
                        sceneAddressList.get(mCurrentNowPlayingScene),
                        m_ScanHandler.getconnectedSSIDname(getApplicationContext()));

                for (LSSDPNodes mSlaveNode : mSlaveListForMyMaster) {
                    LUCIControl luciControl = new LUCIControl(mSlaveNode.getIP());
                    LibreLogger.d("this", "Release Scene Slave Ip List" + mSlaveNode.getFriendlyname());
                    */
        /**/
        /* Sending Asynchronous Registration */
        /**/
        /*
                    luciControl.sendAsynchronousCommand();
                    luciControl.SendCommand(MIDCONST.MID_DDMS, LUCIMESSAGES.SETFREE, LSSDPCONST.LUCI_SET);
                }*/
        /*
                LUCIControl mMluciControl = new LUCIControl(mMasternode.getIP());
                mMluciControl.sendAsynchronousCommand();
                mMluciControl.SendCommand(MIDCONST.MID_JOIN_OR_DROP, LUCIMESSAGES.DROP, LSSDPCONST.LUCI_SET);
                if (m_ScanHandler.removeSceneMapFromCentralRepo(mMasternode.getIP())) {
                    LibreLogger.d("this", "Release Scene Removed From Central Repo " + mMasternode.getIP());
                }

                Message msg = new Message();
                msg.what = DeviceMasterSlaveFreeConstants.LUCI_SEND_FREE_COMMAND;
                b.putString("ipAddress", mMasternode.getIP());
                msg.setData(b);
                mNowPlayingActivityReleaseSceneHandler.sendMessage(msg);

                StartLSSDPScanAfterRelease();

                if (m_ScanHandler.getSceneObjectFromCentralRepo().size() <= 0) {
                    Intent intent = new Intent(NowPlayingActivity.this, PlayNewActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    Intent intent = new Intent(NowPlayingActivity.this, ActiveScenesListActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                }
                return true;
            }*/
        /*case R.id.JoinAllToThisScene:{
                final LSSDPNodes mMasternode = m_ScanHandler.getLSSDPNodeFromCentralDB(sceneAddressList.get(mCurrentNowPlayingScene));//(sceneAddressList.get(mCurrentNowPlayingScene));
                if (mMasternode == null) {
                    Toast.makeText(this, "Opps!Master not found", Toast.LENGTH_LONG).show();
                    return true;
                }

                LUCIControl mMluciControl = new LUCIControl(mMasternode.getIP());
                mMluciControl.sendAsynchronousCommand();
                mMluciControl.SendCommand(MIDCONST.MID_JOIN_OR_DROP, LUCIMESSAGES.JOIN_ALL, LSSDPCONST.LUCI_SET);
                return true;
            }*/
        if (item.getItemId() == R.id.ReleaseScene) {
            final LSSDPNodes mMasternode = m_ScanHandler.getLSSDPNodeFromCentralDB(sceneAddressList.get(mCurrentNowPlayingScene));
            if (mMasternode != null && mMasternode.getCurrentSource() == Constants.GCAST_SOURCE
                    && mMasternode.getmPlayStatus() == SceneObject.CURRENTLY_PLAYING) {
                LibreError error = new LibreError(mMasternode.getFriendlyname(), Constants.SPEAKER_IS_CASTING
                );
                //     showErrorMessage(error);
                return true;
            }
            final AlertDialog.Builder alertDialog = new AlertDialog.Builder(NowPlayingActivity.this);
            alertDialog
                    .setMessage(getResources().getString(R.string.releaseReleaseTitle))
                    .setCancelable(false)
                    .setPositiveButton(getResources().getString(R.string.yes), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            /////// relasing scene
                            if (mCurrentNowPlayingScene >= sceneAddressList.size()) {
                                LibreLogger.d(this, "IndexOutOfBoundException occurred while releasing scene");
                                  /*  if (m_ScanHandler.getSceneObjectFromCentralRepo().size() <= 0) {
                                        Intent intent = new Intent(NowPlayingActivity.this, PlayNewActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        startActivity(intent);
                                        finish();
                                    } else */
                                {
                                    Intent intent = new Intent(NowPlayingActivity.this, ActiveScenesListActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(intent);
                                    finish();
                                }
                            }

                            final LSSDPNodes mMasternode = m_ScanHandler.getLSSDPNodeFromCentralDB(sceneAddressList.get(mCurrentNowPlayingScene));//(sceneAddressList.get(mCurrentNowPlayingScene));
                            if (mMasternode == null) {
                                Toast.makeText(getApplicationContext(), "Opps!Master not found", Toast.LENGTH_LONG).show();
                                return;
                            }
                            Bundle b = new Bundle();
                            ArrayList<LSSDPNodes> mSlaveListForMyMaster = m_ScanHandler.getSlaveListForMasterIp(
                                    sceneAddressList.get(mCurrentNowPlayingScene),
                                    m_ScanHandler.getconnectedSSIDname(getApplicationContext()));

                            for (LSSDPNodes mSlaveNode : mSlaveListForMyMaster) {
                                LUCIControl luciControl = new LUCIControl(mSlaveNode.getIP());
                                LibreLogger.d("this", "Release Scene Slave Ip List" + mSlaveNode.getFriendlyname());
                                //* Sending Asynchronous Registration *//*
                                luciControl.sendAsynchronousCommand();
                                luciControl.SendCommand(MIDCONST.MID_DDMS, LUCIMESSAGES.SETFREE, LSSDPCONST.LUCI_SET);
                            }
                            LUCIControl mMluciControl = new LUCIControl(mMasternode.getIP());
                            mMluciControl.sendAsynchronousCommand();
                            mMluciControl.SendCommand(MIDCONST.MID_DDMS, LUCIMESSAGES.SETFREE, LSSDPCONST.LUCI_SET);
                               /* if (m_ScanHandler.removeSceneMapFromCentralRepo(mMasternode.getIP())) {
                                    LibreLogger.d("this", "Release Scene Removed From Central Repo " + mMasternode.getIP());
                                }*/

                            Message msg = new Message();
                            msg.what = DeviceMasterSlaveFreeConstants.LUCI_SEND_FREE_COMMAND;
                            b.putString("ipAddress", mMasternode.getIP());
                            msg.setData(b);
                            mNowPlayingActivityReleaseSceneHandler.sendMessage(msg);

                            //   StartLSSDPScanAfterRelease();
                                /*Commenting as we are waiting for the 103 for master

                                if (m_ScanHandler.getSceneObjectFromCentralRepo().size() <= 0) {
                                    Intent intent = new Intent(NowPlayingActivity.this, PlayNewActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(intent);
                                    finish();
                                } else {
                                    Intent intent = new Intent(NowPlayingActivity.this, ActiveScenesListActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(intent);
                                    finish();
                                }*/
                        }
                    })
                    .setNegativeButton(getResources().getString(R.string.no), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });
            alertDialog.show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    private void showRadioButtonDialog() {

        if (NowPlayingActivity.this.isFinishing())
            return;

        // custom dialog
        final Dialog dialog = new Dialog(NowPlayingActivity.this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.equalizer_dialog);

//        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        WindowManager.LayoutParams wmlp = dialog.getWindow().getAttributes();
        wmlp.gravity = Gravity.TOP | Gravity.RIGHT;
        dialog.show();

        List<String> stringList = new ArrayList<>();  // here is list

        stringList.add("Normal");
        stringList.add("Rock");
        stringList.add("Jazz");
        stringList.add("Pop");
        stringList.add("Classical");


        RadioGroup rg = (RadioGroup) dialog.findViewById(R.id.radio_group);

        for (int i = 0; i < stringList.size(); i++) {
            RadioButton rb = new RadioButton(this); // dynamically creating RadioButton and adding to RadioGroup.
            rb.setText(stringList.get(i));
            rg.addView(rb);
        }

        dialog.show();

    }


    public void StartLSSDPScanAfterRelease() {

        final LibreApplication application = (LibreApplication) getApplication();
        application.getScanThread().UpdateNodes();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                application.getScanThread().UpdateNodes();
            }
        }, 500);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                application.getScanThread().UpdateNodes();

            }
        }, 1000);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.menu_active_scenes_list
                , menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerForDeviceEvents(this);

        if (mCurrentNowPlayingScene >= sceneAddressList.size()) {
            LibreLogger.d(this, "IndexOutOfBoundException in onResume");
            return;
        }

       /* if(m_upnpProcessor==null){
            m_upnpProcessor = new UpnpProcessorImpl(this);
            m_upnpProcessor.bindUpnpService();
            m_upnpProcessor.addListener(this);
            m_upnpProcessor.addListener(UpnpDeviceManager.getInstance());
            Log.d("UpnpDeviceDiscovery", "onStart");
        }*/
//        disableBroadcastReceiver();
		/*Background Volume is Not Allowed From 1p76 */
        /* Commenting , Because For Gear4 We Have Stopped Using it And To Avoid Spotify And Airplay Volume Jumping Issue
        IntentFilter intentFilter = new IntentFilter();
       intentFilter.addAction("android.media.VOLUME_CHANGED_ACTION");
        if(volumeReceiver == null) {
            volumeReceiver = new VolumeReceiver(sceneAddressList.get(mCurrentNowPlayingScene));
            registerReceiver(volumeReceiver, intentFilter);
        }else{
            volumeReceiver.setIpAddress(sceneAddressList.get(mCurrentNowPlayingScene));
        }*/


        LUCIControl mLuciControl = new LUCIControl(sceneAddressList.get(mCurrentNowPlayingScene));
        mLuciControl.sendAsynchronousCommand();
        mLuciControl.SendCommand(50, null, LSSDPCONST.LUCI_GET);

    }

    protected void onStop() {
        super.onStop();
        unRegisterForDeviceEvents();

        try {
            unregisterReceiver(volumeReceiver);
        } catch (Exception e) {

        }

        /*enabling broadcast main receiver */
//        enableBroadcastReceiver();
    }


    @Override
    public void deviceDiscoveryAfterClearingTheCacheStarted() {

    }

    @Override
    public void newDeviceFound(LSSDPNodes node) {


    }

    @Override
    public void deviceGotRemoved(final String mIpAddress) {

        ///ActiveSceneAdapter.mMasterSpecificSlaveAndFreeDeviceMap.remove(mIpAddress);
        // Toast.makeText(getApplicationContext(),"Device Got Removed "+ mIpAddress,Toast.LENGTH_SHORT).show();
//        LibreLogger.d(this, "Device Got Removed" + mIpAddress);



                //if (m_ScanHandler.getSceneObjectFromCentralRepo().containsKey(mIpAddress))

                if (sceneAddressList.contains(mIpAddress)) {
                    //m_ScanHandler.removeSceneMapFromCentralRepo(mIpAddress);
                    Intent intent = new Intent(NowPlayingActivity.this, ActiveScenesListActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                }
               /* LSSDPNodeDB mNodeDB = LSSDPNodeDB.getInstance();
                if(mNodeDB!=null) {
                    LSSDPNodes mNodeToRemove = mNodeDB.getTheNodeBasedOnTheIpAddress(mIpAddress);
                    if(mNodeToRemove!=null) {
                        if(mNodeToRemove.getDeviceState().equals("M")) {
                            Intent intent = new Intent(NowPlayingActivity.this, ActiveScenesListActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        }
                    }
                }*/

       /* LSSDPNodeDB mNodeDB = LSSDPNodeDB.getInstance();
        if(mNodeDB!=null) {
            LSSDPNodes mNodeToRemove = mNodeDB.getTheNodeBasedOnTheIpAddress(mIpAddress);
            if(mNodeToRemove!=null) {
                if(mNodeToRemove.getDeviceState().equals("M")) {
                    Intent intent = new Intent(NowPlayingActivity.this, ActiveScenesListActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                }
            }
        }*/


    }

    public ProgressDialog mProgressDialog;

    public void closeLoader() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            Log.e("LuciControl", "progress Dialog Closed");
            mProgressDialog.setCancelable(true);
            mProgressDialog.dismiss();
            mProgressDialog.cancel();

        }

    }

    public void showLoader(String msg) {
        if (mProgressDialog == null) {
            Log.e("LUCIControl", "Null");
            mProgressDialog = ProgressDialog.show(NowPlayingActivity.this, getString(R.string.notice), getString(R.string.changing) + msg + "...", true, true, null);
        }
        mProgressDialog.setCancelable(false);

        if (!mProgressDialog.isShowing()) {
            if (!(NowPlayingActivity.this.isFinishing())) {
                mProgressDialog = ProgressDialog.show(NowPlayingActivity.this, getString(R.string.notice), getString(R.string.changing) + msg + "...", true, true, null);
            }
        }
    }

    @Override
    public void messageRecieved(NettyData dataRecived) {
        byte[] buffer = dataRecived.getMessage();
        String ipaddressRecieved = dataRecived.getRemotedeviceIp();

        LUCIPacket packet = new LUCIPacket(dataRecived.getMessage());
        LibreLogger.d(this, "Message recieved for ipaddress " + ipaddressRecieved + "command is " + packet.getCommand());
        if (packet.getCommand() == 50) {
            String message = new String(packet.getpayload());
            //Toast.makeText(getApplicationContext(),"Got Source List" +message,Toast.LENGTH_SHORT).show();

        }
        if (packet.getCommand() == 103) {
            LibreLogger.d(this, "Command 103 " + new String(packet.getpayload()));
            Message successMsg = new Message();
            String message = new String(packet.getpayload());
            Bundle b = new Bundle();
            b.putString("ipAddress", dataRecived.getRemotedeviceIp());
            successMsg.setData(b);
            successMsg.obj = m_ScanHandler.getLSSDPNodeFromCentralDB(dataRecived.getRemotedeviceIp());
            if (message.contains("FREE")) {
                successMsg.what = DeviceMasterSlaveFreeConstants.LUCI_SUCCESS_FREE_COMMAND;
            } else if (message.contains("SLAVE")) {
                successMsg.what = DeviceMasterSlaveFreeConstants.LUCI_SUCCESS_SLAVE_COMMAND;
            } else if (message.contains("MASTER")) {
                successMsg.what = DeviceMasterSlaveFreeConstants.LUCI_SUCCESS_MASTER_COMMAND;
            }
            mNowPlayingActivityReleaseSceneHandler.sendMessage(successMsg);
        }

    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();

        Intent intent = new Intent(NowPlayingActivity.this, ActiveScenesListActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(volumeReceiver);
        }catch(Exception e){

        }
        mNowPlayingActivityReleaseSceneHandler.removeCallbacksAndMessages(this);
    }
//    @Override
//    public void addVolumeKeyListener(OnChangeVolumeHardKeyListener volumeHardKeyListener) {
////        hardKeyListenersList.add(volumeHardKeyListener);
//    }

    public class MyPagerAdapter extends FragmentPagerAdapter {
        private int NUM_ITEMS;


        public MyPagerAdapter(FragmentManager fragmentManager, int count) {
            super(fragmentManager);
            NUM_ITEMS = count;
        }

        public NowPlayingFragment getFragment(int key) {
            return mPageReferenceMap.get(key);
        }

        // Returns total number of pages
        @Override
        public int getCount() {
            return NUM_ITEMS;
        }

        // Returns the fragment to display for that page
        @Override
        public Fragment getItem(int position) {

            NowPlayingFragment nowPlayingFragment = new NowPlayingFragment();
            Bundle bundle = new Bundle();

            SceneObject sceneObject = m_ScanHandler.getSceneObjectFromCentralRepo(sceneAddressList.get(position));
            mCurrentNowPlayingScene = position;
            bundle.putInt("scene_POSITION", position);
            if (sceneObject != null) {
                bundle.putString("scene_IP", sceneObject.getIpAddress());
            }

            nowPlayingFragment.setArguments(bundle);


            mPageReferenceMap.put(position, nowPlayingFragment);
            return nowPlayingFragment;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            super.destroyItem(container, position, object);

            mPageReferenceMap.remove(position);
        }

        // Returns the page title for the top indicator
        @Override
        public CharSequence getPageTitle(int position) {
            return "Page " + position;
        }

    }

    public void UpdateLSSDPNodeList(String ipaddress, String mDeviceState) {

        LSSDPNodes mToBeUpdateNode = m_ScanHandler.getLSSDPNodeFromCentralDB(ipaddress);
        //LSSDPNodes mMasterNode = m_ScanHandler.getLSSDPNodeFromCentralDB(mMasterIP);

        if (mToBeUpdateNode == null)
            return;

        LSSDPNodeDB mNodeDB = LSSDPNodeDB.getInstance();
        if (mDeviceState.equals("Master")) {
            mToBeUpdateNode.setDeviceState("M");
        } else if (mDeviceState.equals("Slave")) {
            mToBeUpdateNode.setDeviceState("S");
          /*  if(m_ScanHandler.getconnectedSSIDname(getApplicationContext())== m_ScanHandler.HN_MODE) {
                mToBeUpdateNode.setcSSID(mMasterNode.getcSSID());
            }else{
                mToBeUpdateNode.setZoneID(mMasterNode.getZoneID());
            }*/
        } else if (mDeviceState.equals("Free")) {
            mToBeUpdateNode.setDeviceState("F");
       /*     if(m_ScanHandler.getconnectedSSIDname(getApplicationContext())== m_ScanHandler.HN_MODE) {
                mToBeUpdateNode.setcSSID("");
            }else{
                mToBeUpdateNode.setZoneID("");
            }*/
        }
        mNodeDB.renewLSSDPNodeDataWithNewNode(mToBeUpdateNode);

    }

    Handler mNowPlayingActivityReleaseSceneHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Message failedMsg = new Message();
            failedMsg.what = DeviceMasterSlaveFreeConstants.LUCI_TIMEOUT_COMMAND;
            switch (msg.what) {

                case DeviceMasterSlaveFreeConstants.LUCI_SEND_FREE_COMMAND: {
                    mNowPlayingActivityReleaseSceneHandler.sendEmptyMessageDelayed(failedMsg.what, 35000);
                    showLoader("Releasing Scene");
                }
                break;

                case DeviceMasterSlaveFreeConstants.LUCI_SUCCESS_FREE_COMMAND: {
                    mNowPlayingActivityReleaseSceneHandler.removeMessages(failedMsg.what);
                    Bundle b = new Bundle();
                    b = msg.getData();
                    String ipaddress = b.getString("ipAddress");
                    closeLoader();

                    LSSDPNodes node = m_ScanHandler.getLSSDPNodeFromCentralDB(ipaddress);
                    /* Crashed For Rashmi, So Fixed with Null Check
                     *  */
                    if(node==null) {
                        return;
                    }
                    LibreLogger.d(this, "Command 103 " + node.getFriendlyname() + node.getDeviceState());
                    /* if ((node != null) && node.getDeviceState().equals("M")) */
                    if ((node != null)  && sceneAddressList!=null && node.getIP().equalsIgnoreCase(sceneAddressList.get(mCurrentNowPlayingScene))) {
                        if (m_ScanHandler.isIpAvailableInCentralSceneRepo(ipaddress)) {
                            m_ScanHandler.removeSceneMapFromCentralRepo(ipaddress);
                        }
                      /*  LSSDPNodeDB mNodeDB = LSSDPNodeDB.getInstance();

                        boolean mMasterFound = false;
                        if (node != null)
                            UpdateLSSDPNodeList(node.getIP(), "Free");
                        for (LSSDPNodes mSlaveNode : m_ScanHandler.getSlaveListForMasterIp(node.getIP(),
                                m_ScanHandler.getconnectedSSIDname(getApplicationContext()))) {
                            UpdateLSSDPNodeList(mSlaveNode.getIP(), "Free");
                        }
                        m_ScanHandler.removeSceneMapFromCentralRepo(node.getIP());
                        for (LSSDPNodes mNode : mNodeDB.GetDB()) {
                            if (mNode.getDeviceState().contains("M") && !mNode.getIP().contains(ipaddress)) {

                                mMasterFound = true;
                            }
                            if (mMasterFound) {
                                break;
                            }
                        }*/
                        boolean mMasterFound = LSSDPNodeDB.getInstance().isAnyMasterIsAvailableInNetwork();
                       /* if (!mMasterFound) {
                            Intent intent = new Intent(NowPlayingActivity.this, PlayNewActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            StartLSSDPScanAfterRelease();
                            ActivityCompat.finishAffinity(NowPlayingActivity.this);
                            finish();
                        } else */{
                            Intent intent = new Intent(NowPlayingActivity.this, ActiveScenesListActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                            startActivity(intent);
                            StartLSSDPScanAfterRelease();
                            finish();

                        }
                    }
                }

                break;
                case DeviceMasterSlaveFreeConstants.LUCI_SUCCESS_MASTER_COMMAND:
                    /*have to handle if we got 103 for any other Master*/
                    break;
                case DeviceMasterSlaveFreeConstants.LUCI_TIMEOUT_COMMAND: {
                    mNowPlayingActivityReleaseSceneHandler.removeMessages(failedMsg.what);

                    if (!(NowPlayingActivity.this.isFinishing())) {
                        new AlertDialog.Builder(NowPlayingActivity.this)
                                .setTitle(getString(R.string.deviceStateChanging))
                                .setMessage("FAILED")
                                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.cancel();
                                    }
                                }).setIcon(android.R.drawable.ic_dialog_alert)
                                .show();
                    }
                }
                break;
                case DeviceMasterSlaveFreeConstants.LUCI_SUCCESS_SLAVE_COMMAND:{
                    Bundle b = new Bundle();
                    b = msg.getData();
                    String ipaddress = b.getString("ipAddress");
                    LSSDPNodes node = m_ScanHandler.getLSSDPNodeFromCentralDB(ipaddress);
                    if(node==null)
                        return;

                    UpdateLSSDPNodeList(node.getIP(), "Slave");
                    LibreLogger.d(this, "Command 103 "+node.getFriendlyname() + "change to slave");
                }
            }
        }
    };

    /**
     * @TODO Khajan
     * This method will disable receiver registered in AndroidManifest file.
     */

    public void disableBroadcastReceiver() {
        ComponentName receiver = new ComponentName(this, VolumeReceiver.class);
        PackageManager pm = this.getPackageManager();

        pm.setComponentEnabledSetting(receiver,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }


    /**
     * @TODO Khajan
     * This method enables the Broadcast receiver registered in the AndroidManifest file.
     */
    public void enableBroadcastReceiver() {
        ComponentName receiver = new ComponentName(this, VolumeReceiver.class);
        PackageManager pm = this.getPackageManager();

        pm.setComponentEnabledSetting(receiver,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
    }



    private class PhoneStateChangeListener extends PhoneStateListener {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            switch(state){
                case TelephonyManager.CALL_STATE_RINGING:
                    LibreLogger.d(this, "Phone is RINGING");
                    isPhoneCallbeingReceived = true;



                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    LibreLogger.d(this, "Phone is RINGING");
                    if (!isPhoneCallbeingReceived) {
                        // Start your new activity
                    } else {
                        // Cancel your old activity
                    }

                    // this should be the last piece of code before the break
                    isPhoneCallbeingReceived = true;
                    break;
                case TelephonyManager.CALL_STATE_IDLE:
                    LibreLogger.d(this, "Phone is IDLE");
                    // this should be the last piece of code before the break
                    isPhoneCallbeingReceived = false;
                    break;
            }
        }
    }

}
