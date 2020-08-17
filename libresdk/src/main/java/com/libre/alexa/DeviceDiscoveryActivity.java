
package com.libre.alexa;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;

import androidx.core.app.ActivityCompat;

import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.libre.alexa.LErrorHandeling.LibreError;
import com.libre.alexa.Ls9Sac.GcastUpdateData;
import com.libre.alexa.Ls9Sac.GcastUpdateStatusAvailableListView;
import com.libre.alexa.Scanning.Constants;
import com.libre.alexa.Scanning.ScanningHandler;
import com.libre.alexa.alexa.ControlConstants;
import com.libre.alexa.alexa.userpoolManager.AlexaListeners.AlexaLoginListener;
import com.libre.alexa.app.dlna.dmc.gui.abstractactivity.UpnpListenerActivity;
import com.libre.alexa.app.dlna.dmc.processor.impl.UpnpProcessorImpl;
import com.libre.alexa.app.dlna.dmc.processor.upnp.CoreUpnpService;
import com.libre.alexa.app.dlna.dmc.server.ContentTree;
import com.libre.alexa.app.dlna.dmc.utility.PlaybackHelper;
import com.libre.alexa.app.dlna.dmc.utility.UpnpDeviceManager;
import com.libre.alexa.constants.LSSDPCONST;
import com.libre.alexa.constants.MIDCONST;
import com.libre.alexa.luci.DiscoverySearchService;
import com.libre.alexa.luci.LSSDPNodeDB;
import com.libre.alexa.luci.LSSDPNodes;
import com.libre.alexa.luci.LUCIControl;
import com.libre.alexa.luci.LUCIPacket;
import com.libre.alexa.luci.Utils;
import com.libre.alexa.netty.BusProvider;
import com.libre.alexa.netty.LibreDeviceInteractionListner;
import com.libre.alexa.netty.NettyAndroidClient;
import com.libre.alexa.netty.NettyData;
import com.libre.alexa.netty.RemovedLibreDevice;
import com.libre.alexa.util.LibreLogger;
import com.squareup.otto.Subscribe;

import org.fourthline.cling.model.meta.RemoteDevice;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import static com.libre.alexa.UpdateHashMap.updateHashMap;


/*import android.support.design.widget.Snackbar;*/


/**
 * Created by praveena on 7/27/15.
 */
public class DeviceDiscoveryActivity extends UpnpListenerActivity {

    private static final String TAG = DiscoverySearchService.class.getName();
    LibreDeviceInteractionListner libreDeviceInteractionListner;

    /*change this timeout if want to show error for long time*/
    public final int ERROR_TIMEOUT = 2000;


    protected UpnpProcessorImpl m_upnpProcessor;
    private LocalNetworkStateReceiver networkStatereceiver;
    //  private NewNetworkStateReceiver networkStatereceiver;
    private IntentFilter intentFilter;
    private ProgressDialog mProgressDialog;

    private boolean listenToNetworkChanges = true;
    private boolean listenToWifiConnectingStatus = true;
    protected AlertDialog alert;
    protected AlertDialog alertRestartApp;
    AlertDialog alertDialog;
    private boolean isActivityPaused = false;
    static AlexaLoginListener alexaLoginListener;
    UpdateHashMap updateHash = UpdateHashMap.getInstance();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LibreLogger.d(this, "on Create is Called");

        intentFilter = new IntentFilter();
        intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        networkStatereceiver = new LocalNetworkStateReceiver();

        registerReceiver(networkStatereceiver, intentFilter);


        //WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        //   LibreApplication.activeSSID = getconnectedSSIDname(getApplicationContext());

    }

    public void somethingWentWrong(Context context) {
        try {
            alert = null;
            AlertDialog.Builder builder = new AlertDialog.Builder(context);

            builder.setMessage(getResources().getString(R.string.somethingWentWrong))
                    .setCancelable(false)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            alert.dismiss();
                        }
                    });

            if (alert == null) {
                alert = builder.show();
                TextView messageView = (TextView) alert.findViewById(android.R.id.message);
                messageView.setGravity(Gravity.CENTER);
            }

            alert.show();

        } catch (Exception e) {

        }
    }

    public String getconnectedSSIDname(Context mContext) {
        try {
            if (mContext == null)
                return null;
            WifiManager wifiManager;
            wifiManager = (WifiManager) mContext.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wifiManager == null)
                return null;
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if (wifiInfo == null)
                return null;
            String ssid = wifiInfo.getSSID();
            LibreLogger.d(this, "getconnectedSSIDname wifiInfo = " + wifiInfo.toString());
            if (ssid != null && ssid.startsWith("\"") && ssid.endsWith("\"")) {
                ssid = ssid.substring(1, ssid.length() - 1);
            }

            return ssid;
        } catch (Exception e) {
            return null;
        }
    }


    /*sending dmr stop command from application*/
    public void stopDMRPlayback(String currentIpaddress) {
        try {
            LibreLogger.d(this, "Going to Remove the DMR Playback");
            RemoteDevice renderingDevice = UpnpDeviceManager.getInstance().getRemoteDMRDeviceByIp(currentIpaddress);
            /* For the New DMR Implementation , whenever a freeing master
             * we are Stopping the Playback */
            if (renderingDevice != null) {
                String renderingUDN = renderingDevice.getIdentity().getUdn().toString();
                PlaybackHelper playbackHelper = LibreApplication.PLAYBACK_HELPER_MAP.get(renderingUDN);
                ScanningHandler mScanHandler = ScanningHandler.getInstance();
                SceneObject currentSceneObject = mScanHandler.getSceneObjectFromCentralRepo(currentIpaddress);
                try {
                    if (playbackHelper != null && renderingDevice != null
                            && currentSceneObject != null
                            && currentSceneObject.getPlayUrl() != null
                            && !currentSceneObject.getPlayUrl().equalsIgnoreCase("")
                            && currentSceneObject.getPlayUrl().contains(LibreApplication.LOCAL_IP)
                            && currentSceneObject.getPlayUrl().contains(ContentTree.AUDIO_PREFIX)

                    ) {

                        playbackHelper.StopPlayback();
                    }
                } catch (Exception e) {

                    LibreLogger.d(this, "Handling the exception while sending the stop command ");
                }
                LibreApplication.PLAYBACK_HELPER_MAP.remove(renderingUDN);
            }
        } catch (Exception e) {
            LibreLogger.d(this, "Removing the DMR Playback is Exception");
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        m_upnpProcessor = new UpnpProcessorImpl(this);
        m_upnpProcessor.bindUpnpService();
        m_upnpProcessor.addListener(this);
        m_upnpProcessor.addListener(UpnpDeviceManager.getInstance());
        Log.d("UpnpDeviceDiscovery", "onStart");
    }

    //    private void readVOXControlValue(String ip) {
//        new LUCIControl(ip).SendCommand(257,"0", LSSDPCONST.LUCI_SET);
//    }
    protected void onPause() {
        super.onPause();
        isActivityPaused = true;


        Log.d("UpnpDeviceDiscovery", "onPause");
    }

    Object busEventListener = new Object() {
        @Subscribe
        public void newDeviceFound(final LSSDPNodes nodes) {

            //  LSSDPNodesUpdate updateNode = getLssdpNodesUpdate(nodes.getIP());
            //CreateUpdatedHashMap(nodes.getIP(),);
            LibreLogger.d(this, "Device VOX suma in new device found" + nodes.getFriendlyname());

            /*    *//* This below if loop is in troduced to handle the case where Device state from the DUT could be Null sometimes
             * Ideally the device state should not be null but we are just handling it to make sure it will not result in any crash!
             *
             * *//*
            if (nodes == null || nodes.getDeviceState() == null) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(DeviceDiscoveryActivity.this, "Alert! Device State is null " + nodes.getDeviceState(), Toast.LENGTH_SHORT).show();
                    }
                });
                return;
            }


            else if (nodes.getDeviceState()!=null){


                LUCIControl control = new LUCIControl(nodes.getIP());

                control.sendAsynchronousCommand();

                *//*we are sending individual volume to all including Master*//*
                control.SendCommand(MIDCONST.VOLUEM_CONTROL, null, LSSDPCONST.LUCI_GET);
                 *//*reading 10 for new source*//*
                control.SendCommand(MIDCONST.NEW_SOURCE, null, LSSDPCONST.LUCI_GET);
                if (nodes.getDeviceState() != null && nodes.getDeviceState().contains("M")) {
                    *//*we are sending zone volume Master*//*
                    control.SendCommand(MIDCONST.ZONE_VOLUME, null, LSSDPCONST.LUCI_GET);
                }


                control.SendCommand(MIDCONST.GCAST_COMMAND, null, LSSDPCONST.LUCI_GET);


                control.sendGoogleTOSAcceptanceIfRequired(getApplicationContext(), nodes.getgCastVerision());
                *//* if New Device is Found but we already removed  the SceneObject so Recreating it ... *//*
                if (nodes.getDeviceState() != null && nodes.getDeviceState().contains("M")) {
                    if (!ScanningHandler.getInstance().isIpAvailableInCentralSceneRepo(nodes.getIP())) {
                        SceneObject sceneObjec = new SceneObject(" ", nodes.getFriendlyname(), 0, nodes.getIP());
                        ScanningHandler.getInstance().putSceneObjectToCentralRepo(nodes.getIP(), sceneObjec);
                        LibreLogger.d(this, "Master is not available in Central Repo So Created SceneObject " + nodes.getIP());
                    }
                }


            }

        */
            if (libreDeviceInteractionListner != null) {
                /*requesting volume for each device while new device added*/
                /*posting messages to */
                libreDeviceInteractionListner.newDeviceFound(nodes);
            }
            readLedControlValue(nodes.getIP());
            if (nodes.getgCastVerision() != null)
                checkForTheSACDeviceSuccessDialog(nodes);
        }

        @Subscribe
        public void newMessageRecieved(NettyData nettyData) {
            LUCIPacket dummyPacket = new LUCIPacket(nettyData.getMessage());
            LibreLogger.d(this, "New message appeared for the device " + nettyData.getRemotedeviceIp() +
                    "For the CommandStatus " +
                    dummyPacket.getCommandStatus() + " for the command " + dummyPacket.getCommand());

            LUCIPacket packet = new LUCIPacket(nettyData.getMessage());
            LibreLogger.d(this, "SUMA VOX message New message appeared for the device for the get packet command\n" + packet.getCommand() + "get message\n" + nettyData.getMessage() + "get remote device ip\n" + nettyData.getRemotedeviceIp());


            if (libreDeviceInteractionListner != null) {
                // LSSDPNodesUpdate updateNode = getLssdpNodesUpdate(nodes.getIP());
//                updateNode.setFriendlyname(nodes.getFriendlyname());
//                updateNode.setFwVersion(nodes.getVersion());
//
//                updateHash.CreateUpdatedHashMap(nodes.getIP(), updateNode);
                //  getLssdpNodesUpdate(LSSDPNodeDB.getInstance().getTheNodeBasedOnTheIpAddress(nettyData.getRemotedeviceIp()));

//                LUCIPacket packet = new LUCIPacket(nettyData.getMessage());
//                if (packet.getCommand() == MIDCONST.VOLUEM_CONTROL) {
//                    String volumeMessage = new String(packet.getpayload());
//                    try {
//                        int volume = Integer.parseInt(volumeMessage);
////                        this map is having all volumes
//                        LibreApplication.INDIVIDUAL_VOLUME_MAP.put(nettyData.getRemotedeviceIp(), volume);
//                        Log.d("VolumeMapUpdating", "" + volume);
//                    } catch (Exception e) {
//                        LibreLogger.d(this, "Exception occurred in newMessageReceived");
//                    }
//
//                }
//                if (packet.getCommand() == MIDCONST.ZONE_VOLUME) {
//                    String volumeMessage = new String(packet.getpayload());
//                    try {
//                        int volume = Integer.parseInt(volumeMessage);
////                        this map is having Masters volume
//                        LibreApplication.ZONE_VOLUME_MAP.put(nettyData.getRemotedeviceIp(), volume);
//                        Log.d("ZoneVolumeMapUpdating", "" + volume);
//                    } catch (Exception e) {
//                        LibreLogger.d(this, "Exception occurred in newMessageReceived");
//                    }
//                }
//                if (packet.getCommand() == MIDCONST.MID_SCENE_NAME) {
//                    String msg = new String(packet.getpayload());
//                    try {
//                        LibreLogger.d(this, "Scene Name updation in DeviceDiscoveryActivity");
//                        ScanningHandler mScanHandler = ScanningHandler.getInstance();
//                        SceneObject mSceneObj = mScanHandler.getSceneObjectFromCentralRepo(nettyData.getRemotedeviceIp());
//                        if (mSceneObj != null)
//                            mSceneObj.setSceneName(msg);
//                    } catch (Exception e) {
//                        LibreLogger.d(this, "Exception occurred in newMessageReceived");
//                    }
//                }
//
////                this has been added for DMR hijacking issue
//                if (packet.getCommand() == MIDCONST.NEW_SOURCE) {
//                    String msg = new String(packet.getpayload());
//                    try {
//                        int duration = Integer.parseInt(msg);
//                        LibreLogger.d(this, "Current source updation in DeviceDiscoveryActivity");
//                        Log.d("DMR_CURRENT_SOURCE_BASE", "" + duration);
//                        ScanningHandler mScanHandler = ScanningHandler.getInstance();
//                        SceneObject mSceneObj = mScanHandler.getSceneObjectFromCentralRepo(nettyData.getRemotedeviceIp());
//                        if (mSceneObj != null)
//                            mSceneObj.setCurrentSource(duration);
//                    } catch (Exception e) {
//                        LibreLogger.d(this, "Exception occurred in newMessageReceived");
//                    }
//                }
//
//                if(packet.getCommand() == MIDCONST.NETWORK_STATUS_CHANGE){
//                    String msg = new String(packet.getpayload());
//                    try {
//
//                        LibreLogger.d(this, "Current NetworkS Status updation in DeviceDiscoveryActivity" + nettyData.getRemotedeviceIp());
//                        LibreLogger.d(this, "Current Net status" +  msg);
//                        ScanningHandler mScanHandler = ScanningHandler.getInstance();
//                        SceneObject mSceneObj = mScanHandler.getSceneObjectFromCentralRepo(nettyData.getRemotedeviceIp());
//                        if (mSceneObj != null)
//                            mSceneObj.setCurrentSource(duration);
//                    } catch (Exception e) {
//                        LibreLogger.d(this, "Exception occurred in newMessageReceived");
//                    }
//                }
////                Updating the last notified Time for all the Device
//                if(nettyData!=null){
//                    if(LUCIControl.luciSocketMap.containsKey(nettyData.getRemotedeviceIp()))
//                        LUCIControl.luciSocketMap.get(nettyData.getRemotedeviceIp()).setLastNotifiedTime(System.currentTimeMillis());
//                }
                if (dummyPacket.getCommand() == 208) {
                    /*alexa debug code*/
                    LibreLogger.d(this, "208 mb received");
                }
                if (dummyPacket.getCommand() == 237) {
                    LibreLogger.d(this, "VOX message" + nettyData.getMessage());
                }
                if (nettyData != null)
                    libreDeviceInteractionListner.messageRecieved(nettyData);
            }

            handleTheGoogleCastMessages(nettyData);

        }

        @Subscribe
        public void deviceGotRemovedFromIpAddress(String deviceDeviceAddress) {


            if (deviceDeviceAddress != null) {
                if (libreDeviceInteractionListner != null && deviceDeviceAddress != null) {
                    libreDeviceInteractionListner.deviceGotRemoved(deviceDeviceAddress);
                }

            }


        }

        @Subscribe
        public void deviceGotRemoved(RemovedLibreDevice deviceDevice) {


            if (deviceDevice != null) {

                if (LibreApplication.isApplicationActiveOnUI()) {
                    if (LSSDPNodeDB.getInstance().getTheNodeBasedOnTheIpAddress(deviceDevice.getmIpAddress()) != null) {
                        alertDialogForDeviceNotAvailable(LSSDPNodeDB.getInstance().getTheNodeBasedOnTheIpAddress(deviceDevice.getmIpAddress()));
                    }
                } else {

                    GoAndRemoveTheDevice(deviceDevice.getmIpAddress());

                    if (libreDeviceInteractionListner != null && deviceDevice != null) {
                        libreDeviceInteractionListner.deviceGotRemoved(deviceDevice.getmIpAddress());
                    }


                }


            }


        }


        /**/
        @Subscribe
        public void gcastNewInernetFirmwareFound(GcastUpdateData mData) {
            if (true) {
                Log.d(TAG, "gcastNewInernetFirmwareFound() called with: " +
                        "isGcastInternet = [" + mData.getmDeviceName() + "]"
                        + LibreApplication.GCAST_UPDATE_AVAILABE_LIST_DATA.keySet().toString());
                Intent intent = new Intent(DeviceDiscoveryActivity.this,
                        GcastUpdateStatusAvailableListView.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                /* finish();*/
                /*if (ScanningHandler.getInstance().getSceneObjectFromCentralRepo().size() > 0) {
                    Intent intent = new Intent(DeviceDiscoveryActivity.this, ActiveScenesListActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    Intent intent = new Intent(DeviceDiscoveryActivity.this, ActiveScenesListActivity.class);
                    startActivity(intent);
                    finish();
                }*/

            }

        }

        @Subscribe
        public void libreErrorReceived(LibreError libreError) {

            if (libreDeviceInteractionListner != null && !isActivityPaused) {
                showErrorMessage(libreError);
            }
        }
    };

//    private LSSDPNodesUpdate getLssdpNodesUpdate(LSSDPNodes ip) {
//        if (updateHash.getUpdateNode(ip) != null) {
//            LibreLogger.d(this, "Device VOX suma in new device found getupdate node");
//
//            return updateHash.getUpdateNode(ip);
//        }
//        LibreLogger.d(this, "Device VOX suma in new device found getupdate node newly update");
//        return new LSSDPNodesUpdate();
//
//    }

    //call in upnp serice
    public void readEnvVox(String speakerIpaddress) {
        new LUCIControl(speakerIpaddress).SendCommand(MIDCONST.MID_ENV_READ, "READ_RouterCertPaired", LSSDPCONST.LUCI_SET);
        Log.d(TAG, "sentRouterCertPaired");
    }

    private void readLedControlValue(String ip) {
        new LUCIControl(ip).SendCommand(208, "READ_LEDControl", LSSDPCONST.LUCI_SET);
    }

    public void parseControlJson(JSONObject window, SceneObject currentSceneObject) {
        try {
            String conrolJson = window.getString("ControlsJson");
            JSONArray controlsJsonArr = new JSONArray(conrolJson);
            if (controlsJsonArr != null) {
                boolean[] flags = {false, false, false};
                for (int i = 0; i < controlsJsonArr.length(); i++) {
                    LibreLogger.d(this, "JSON recieved for Alexa controls " + controlsJsonArr.get(i));
                    // sample JSON {\"enabled\":true,\"name\":\"PLAY_PAUSE\",\"selected\":false,\"type\":\"BUTTON\"}
                    JSONObject jsonObject = controlsJsonArr.getJSONObject(i);
                    String name = jsonObject.getString("name");
                    boolean enabled = jsonObject.getBoolean("enabled");
                    switch (name) {
                        case ControlConstants.PLAY_PAUSE:
                            if (enabled) {
                                flags[0] = true;
                            }
                            break;
                        case ControlConstants.NEXT:
                            if (enabled) {
                                flags[1] = true;
                            }
                            break;
                        case ControlConstants.PREVIOUS:
                            if (enabled) {
                                flags[2] = true;
                            }
                            break;
                    }
                }
                currentSceneObject.setAlexaControls(flags);
            }
        } catch (Exception e) {

        }
    }

    public void setControlIconsForAlexa(SceneObject currentSceneObject, ImageView play, ImageView next, ImageView previous) {
        if (currentSceneObject == null) {
            return;
        }
        boolean[] controlsArr = currentSceneObject.getControlsValue();
        if (controlsArr == null) {
            return;
        }
        play.setEnabled(controlsArr[0]);
        play.setClickable(controlsArr[0]);
        if (!controlsArr[0]) {
            play.setImageResource(R.mipmap.play_without_glow);
        }

        next.setEnabled(controlsArr[1]);
        next.setClickable(controlsArr[1]);
        if (controlsArr[1]) {
            next.setImageResource(R.mipmap.prev_with_glow);
        } else {
            next.setImageResource(R.mipmap.prev_without_glow);
        }

        previous.setEnabled(controlsArr[2]);
        previous.setClickable(controlsArr[2]);
        if (controlsArr[2]) {
            next.setImageResource(R.mipmap.next_with_glow);
        } else {
            next.setImageResource(R.mipmap.next_without_glow);
        }
    }

    public void showToast(Context context, String text) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
    }

    private int getCountOfDevice() {

        ArrayList<LSSDPNodes> lssdpNodesArrayList = LSSDPNodeDB.getInstance().GetDB();
        int i = 0;
        for (LSSDPNodes lssdpNodes : lssdpNodesArrayList) {
            if (lssdpNodes != null)
                i++;
        }
        return i;
    }

    private void handleTheGoogleCastMessages(NettyData nettyData) {

        LUCIPacket packet = new LUCIPacket(nettyData.getMessage());
        LUCIPacket dummyPacket = new LUCIPacket(nettyData.getMessage());

        if (packet.getCommand() == MIDCONST.GCAST_MANUAL_UPGRADE) {
            String msg = new String(packet.getpayload());
            LSSDPNodeDB mLssdpNodeDb = LSSDPNodeDB.getInstance();
            final LSSDPNodes mNode = mLssdpNodeDb.getTheNodeBasedOnTheIpAddress(nettyData.getRemotedeviceIp());
            /* if I am Getting Manual Upgrade for Sacc DEvice we should Discard it */
            if (mNode != null && mNode.getFriendlyname().equalsIgnoreCase(LibreApplication.thisSACConfiguredFromThisApp))
                return;
            /* if NO Update is coming for Any Device We are Just Showing the Dialog as NO Update Available  */
            if (msg.equalsIgnoreCase(Constants.GCAST_NO_UPDATE)) {
                showAlertDialogMessageForGCastMsgBoxes("No Update Available", mNode);
            } else if (msg.equalsIgnoreCase(Constants.GCAST_UPDATE_STARTED)) {
                /* if Update is Started For any of Other Device We are creating a pbject and putting it in Hashmap  */
                msg = getApplicationContext().getString(R.string.downloadingtheFirmare);
                final GcastUpdateData mGcastData = new GcastUpdateData(
                        mNode.getIP(),
                        mNode.getFriendlyname(),
                        msg,
                        0
                );
                LibreApplication.GCAST_UPDATE_AVAILABE_LIST_DATA.put(mNode.getIP(), mGcastData);
                showAlertDialogForGcastUpdate(mNode, mGcastData);
            }

        }
        if (packet.getCommand() == MIDCONST.GCAST_PROGRESS_STATUS) { // 66 Message Box

            String msg = new String(packet.getpayload());
            LibreLogger.d(this, "GCAST_PROGRESS_STATUS " +
                    nettyData.getRemotedeviceIp() +
                    "Message " +
                    msg + " Command " + dummyPacket.getCommand());

            LSSDPNodeDB mLssdpNodeDb = LSSDPNodeDB.getInstance();
            final LSSDPNodes mNode = mLssdpNodeDb.getTheNodeBasedOnTheIpAddress(nettyData.getRemotedeviceIp());
            GcastUpdateData mGcastData = LibreApplication.GCAST_UPDATE_AVAILABE_LIST_DATA.get(
                    nettyData.getRemotedeviceIp()
            );
            if (mNode == null)
                return;
            boolean mNewData = false;
            if (mGcastData == null) {
                mGcastData = new GcastUpdateData(
                        mNode.getIP(),
                        mNode.getFriendlyname(),
                        "",
                        0
                );
                mNewData = true;
                LibreApplication.GCAST_UPDATE_AVAILABE_LIST_DATA.put(mNode.getIP(), mGcastData);
            }

            /*if Gcast COmplete State or For SAC Device then we are showing Device will reboot
             * because after firmwareupgraade COmpleted we are giving ok and coming back to PlayNEwScreen */
            if (msg.equalsIgnoreCase(Constants.GCAST_COMPLETE)
                    && mNode != null &&
                    !mNode.getFriendlyname().equalsIgnoreCase(LibreApplication.thisSACConfiguredFromThisApp)) {
                mGcastData.setmProgressValue(100);
                mGcastData.setmGcastUpdate(getApplicationContext().getString(R.string.gcast_update_done));
                showAlertDialogMessageForGCastMsgBoxes("Firmware Update Completed For ", mNode, "," + getApplicationContext().getString(R.string.deviceRebooting));
                return;
            }

            /*if Gcast FaileState  back to PlayNEwScreen */
            if (msg.equalsIgnoreCase("255")
                    && mNode != null &&
                    !mNode.getFriendlyname().equalsIgnoreCase(LibreApplication.thisSACConfiguredFromThisApp)) {
                showAlertDialogMessageForGCastMsgBoxes("Firmware Update Failed For ", mNode);
                mGcastData.setmGcastUpdate(getApplicationContext().getString(R.string.gcastFailed));
                return;
            }

            try {
                mGcastData.setmProgressValue(Integer.valueOf(msg));
            } catch (Exception e) {
                e.printStackTrace();

            }
            if (mNewData)
                showAlertDialogForGcastUpdate(mNode, mGcastData);


        }

        /**for GCAST internet upgrade*/
        if (packet.getCommand() == MIDCONST.GCAST_UPDATE_MSGBOX) {

            String msg = new String(packet.getpayload());
            LibreLogger.d(this, "GCAST_UPDATE_MSGBOX " + nettyData.getRemotedeviceIp() +
                    "Message " +
                    msg + " Command " + dummyPacket.getCommand());

            LSSDPNodeDB mLssdpNodeDb = LSSDPNodeDB.getInstance();
            final LSSDPNodes mNode = mLssdpNodeDb.getTheNodeBasedOnTheIpAddress(nettyData.getRemotedeviceIp());
            if (mNode != null && !mNode.getFriendlyname().
                    equalsIgnoreCase(LibreApplication.thisSACConfiguredFromThisApp)) {
                if (msg.equalsIgnoreCase(Constants.GCAST_NO_UPDATE)) {
                    return;
                }
                GcastUpdateData mGcastData = LibreApplication.GCAST_UPDATE_AVAILABE_LIST_DATA.get(
                        nettyData.getRemotedeviceIp()
                );
                if (mNode == null)
                    return;
                boolean mNewData = false;
                if (mGcastData == null) {
                    mGcastData = new GcastUpdateData(
                            mNode.getIP(),
                            mNode.getFriendlyname(),
                            "",
                            0
                    );
                    mNewData = true;
                    LibreApplication.GCAST_UPDATE_AVAILABE_LIST_DATA.put(mNode.getIP(), mGcastData);
                }
                if (msg.equalsIgnoreCase(Constants.GCAST_UPDATE_STARTED)) {
                    mGcastData.setmGcastUpdate(getApplicationContext().getString(R.string.downloadingtheFirmare));
                } else if (msg.equalsIgnoreCase(Constants.GCAST_NO_UPDATE)) {
                    mGcastData.setmGcastUpdate(getApplicationContext().getString(R.string.noupdateAvailable));
                } else if (msg.equalsIgnoreCase(Constants.GCAST_UPDATE_IMAGE_AVAILABLE)) {
                    mGcastData.setmGcastUpdate(getApplicationContext().getString(R.string.upgrading));
                } else {
                    try {
                        int mProgressValue = Integer.valueOf(msg);
                        mGcastData.setmGcastUpdate(getApplicationContext().getString(R.string.downloadingtheFirmare));
                        mGcastData.setmProgressValue(mProgressValue);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if (mNewData)
                    showAlertDialogForGcastUpdate(mNode, mGcastData);
            }
        }


/*

        if (packet.getCommand() == MIDCONST.GCAST_TOS_SHARE_COMMAND) {
            String msg = new String(packet.getpayload());

            LibreLogger.d(this, "Message recieved for 226 is " + msg);

                    */
        /* Modified by Praveen to send the request for listening to 222 in the background *//*

            if (msg!=null&& msg.trim().length()>0) {
                        */
        /* this means that device is sending the T:<TOSStatus>:Timezone *//*

                GoogleTOSTimeZone googleTOSTimeZone=new GoogleTOSTimeZone(msg);
                LibreApplication.GOOGLE_TIMEZONE_MAP.put(nettyData.getRemotedeviceIp(), googleTOSTimeZone);
                if(!googleTOSTimeZone.isTOSAccepted()) {

                    if (LibreApplication.GOOGLE_TOS_ACCEPTED) {
                        /// getTosAndUpdateTheValue(nettyData.getRemotedeviceIp(), googleTOSTimeZone);
                        URLclient mURL = new URLclient(nettyData.getRemotedeviceIp(),true);
                        mURL.getTheEurekaJson(googleTOSTimeZone);
                    }
                           */
/* if (googleTOSTimeZone.isTOSAccepted()&&googleTOSTimeZone.isShareAccepted())
                                luciControl.SendCommand(MIDCONST.GCAST_TOS_SHARE_COMMAND, "3:" + googleTOSTimeZone.getTimezone() + TimeZone.getDefault().getID().toString(), LSSDPCONST.LUCI_SET);
                            else if (googleTOSTimeZone.isTOSAccepted()==true&&googleTOSTimeZone.isShareAccepted()==false)
                                luciControl.SendCommand(MIDCONST.GCAST_TOS_SHARE_COMMAND, "1:" + googleTOSTimeZone.getTimezone() + TimeZone.getDefault().getID().toString(), LSSDPCONST.LUCI_SET);
                            else if (googleTOSTimeZone.isTOSAccepted()==false&&googleTOSTimeZone.isShareAccepted()==true)
                                luciControl.SendCommand(MIDCONST.GCAST_TOS_SHARE_COMMAND, "2:" + googleTOSTimeZone.getTimezone() + TimeZone.getDefault().getID().toString(), LSSDPCONST.LUCI_SET);
                            else if (googleTOSTimeZone.isTOSAccepted()==false&&googleTOSTimeZone.isShareAccepted()==false)
                                luciControl.SendCommand(MIDCONST.GCAST_TOS_SHARE_COMMAND, "0:" + googleTOSTimeZone.getTimezone() + TimeZone.getDefault().getID().toString(), LSSDPCONST.LUCI_SET);
                            else
                                LibreLogger.d(this,"Message recieved for 226 is didnt sent from the ");*//*

         */
        /* if(!googleTOSTimeZone.isShareAccepted() || !googleTOSTimeZone.isTOSAccepted()) {
         *//*
         */
        /*This means this is the device configured with our app*//*
         */
/*
                                 googleTOSTimeZone.setTimezone(TimeZone.getDefault().getID().toString());
                                 LUCIControl luciControl = new LUCIControl(nettyData.getRemotedeviceIp());
                                 luciControl.SendCommand(MIDCONST.GCAST_TOS_SHARE_COMMAND, "" + googleTOSTimeZone.getTimezone() + TimeZone.getDefault().getID().toString(), LSSDPCONST.LUCI_SET);
                                 LibreApplication.thisSACDeviceNeeds226 = null;
                             }
*//*

                }

            }

        }
*/

        /**for GCAST internet upgrade*/
        if (packet.getCommand() == MIDCONST.GCAST_COMMAND) {

                   /* LSSDPNodeDB mLssdpNodeDb = LSSDPNodeDB.getInstance();
                    final LSSDPNodes mNode = mLssdpNodeDb.getTheNodeBasedOnTheIpAddress(nettyData.getRemotedeviceIp());

                    String msg = new String(packet.getpayload());

                    if (msg.equalsIgnoreCase("4")) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(CoreUpnpService.this);
                        builder.setMessage(mNode.getFriendlyname() + " Cast Received an Update and rebooting")
                                .setCancelable(false)
                                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.dismiss();

                                        *//**which means you have got for current master and navigate to ActiveScene*//*
                                        BusProvider.getInstance().post(true);


                                    }
                                });
                        AlertDialog alert = builder.create();
                        alert.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                        alert.show();
                    }
                    if (msg.equalsIgnoreCase("2")) {

                        AlertDialog.Builder builder = new AlertDialog.Builder(CoreUpnpService.this);
                        builder.setTitle("New Update Available!").setMessage(mNode.getFriendlyname() + " - Do you wish to update the firmware now?")
                                .setCancelable(false)
                                .setNegativeButton("Later", new  DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        dialogInterface.cancel();
                                    }
                                })
                                .setPositiveButton("Update", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {

                                        LUCIControl luciControl = new LUCIControl(nettyData.getRemotedeviceIp());
                                        luciControl.SendCommand(65, "", LSSDPCONST.LUCI_SET);
                                        dialog.dismiss();
                                        *//**which means you have got for current master and navigate to ActiveScene*//*
                                        BusProvider.getInstance().post(true);
                                    }
                                });

                        AlertDialog alert = builder.create();
                        alert.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                        alert.show();
                    }*/
        }


    }


    /*overriding this method to hide Volume bar in all over app except now playing screen*/
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {

        int action = event.getAction();
        int keyCode = event.getKeyCode();


        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                if (action == KeyEvent.ACTION_DOWN) {
                /*    LibreError error = new LibreError("Sorry!", Constants.VOLUME_PRESSED_MESSAGE);
                    showErrorMessage(error);*/
                }
                return true;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if (action == KeyEvent.ACTION_DOWN) {
                 /*   LibreError error = new LibreError("Sorry!", Constants.VOLUME_PRESSED_MESSAGE);
                    showErrorMessage(error);*/
                }
                return true;
            default:
                return super.dispatchKeyEvent(event);

        }
    }

    public void CreateUpdatedHashMap(String ipAddress, LSSDPNodesUpdate updateNode) {
        updateHashMap.put(ipAddress, updateNode);
        LibreLogger.d(this, "Device VOX suma in create update hash map" + updateNode);

    }

    /*this method is to show error in whole application*/
    public void showErrorMessage(final LibreError message) {
        try {

            if (LibreApplication.hideErrorMessage)
                return;

//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//
//            LibreLogger.d(this, "Showing the supertaloast " + System.currentTimeMillis());
//            SuperToast superToast = new SuperToast(DeviceDiscoveryActivity.this);
//
//            if (message != null && message.getErrorMessage().contains("is no longer available")) {
//                LibreLogger.d(this, "Device go removed showing error in Device Discovery");
//                superToast.setGravity(Gravity.CENTER, 0, 0);
//            }
//            if(message.getmTimeout()==0) {//TimeoutDefault
//                superToast.setDuration(SuperToast.Duration.LONG);
//            }else{
//                superToast.setDuration(SuperToast.Duration.VERY_SHORT);
//            }
//            superToast.setText("" + message);
//                    superToast.setAnimations(SuperToast.Animations.FLYIN);
//                    superToast.setIcon(SuperToast.Icon.Dark.INFO, SuperToast.IconPosition.LEFT);
//                    superToast.show();
//			 if (message != null && message.getErrorMessage().contains(""))
//                superToast.setGravity(Gravity.CENTER, 0, 0);
//                }
//            });
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    public void registerForDeviceEvents(LibreDeviceInteractionListner libreListner) {
        this.libreDeviceInteractionListner = libreListner;
    }

    public void unRegisterForDeviceEvents() {
        this.libreDeviceInteractionListner = null;
    }

    public void registerAlexaLoginListener(AlexaLoginListener alexaLoginListener) {
        this.alexaLoginListener = alexaLoginListener;
    }

    public void unRegisterAlexaLoginListener() {
        this.alexaLoginListener = null;
    }

    public AlexaLoginListener getAlexaLoginListener() {
        return this.alexaLoginListener;
    }

    public void intentToHome(Context context) {
        Intent newIntent;
        if (LSSDPNodeDB.getInstance().GetDB().size() > 0) {
            newIntent = new Intent(context, ActiveScenesListActivity.class);
            newIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(newIntent);
            finish();
        } else {
            newIntent = new Intent(context, ConfigureActivity.class);
            startActivity(newIntent);
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        isActivityPaused = false;
        enableNetworkChangeCallBack();
        enableNetworkOffCallBack();
        intentFilter = new IntentFilter();
        intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        networkStatereceiver = new LocalNetworkStateReceiver();

        /*this is for without restarting application*/
//        networkStatereceiver = new NewNetworkStateReceiver();

        try {
            registerReceiver(networkStatereceiver, intentFilter);
        } catch (Exception e) {

        }
        if (LibreApplication.mCleanUpIsDoneButNotRestarted) {
         /*   if (!DeviceDiscoveryActivity.this.isFinishing()) {
                AlertDialog.Builder builder = new AlertDialog.Builder(DeviceDiscoveryActivity.this);

                builder.setMessage("Network Change Happened,App Will Restart")
                        .setCancelable(false)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Toast.makeText(getApplicationContext(), "Libre App Should Restart.. ..... ", Toast.LENGTH_SHORT).show();
                                restartApp(DeviceDiscoveryActivity.this);
                                //restartApplicationForNetworkChanges(newContext, ssid);
                            }
                        });
                if (alert == null)
                    alert = builder.create();

                alert.show();

            }*/
            restartApp(DeviceDiscoveryActivity.this);
            LibreApplication.mCleanUpIsDoneButNotRestarted = false;
        }
        try {
            BusProvider.getInstance().register(busEventListener);
        } catch (Exception e) {

        }

        /** no need to start notification now for genie app so disabling the notification for now*/
//        Intent in = new Intent(DeviceDiscoveryActivity.this, DMRDeviceListenerForegroundService.class);
//        in.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        in.setAction(Constants.ACTION.STARTFOREGROUND_ACTION);
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            startForegroundService(in);
//        } else {
//            startService(in);
//        }

    }

    @Override
    protected void onStop() {
        Log.d("NetworkChanged", "DeviceDiscovery onStop");

        if (m_upnpProcessor != null) {
            m_upnpProcessor.removeListener(UpnpDeviceManager.getInstance());
            m_upnpProcessor.unbindUpnpService();
        }


        try {
            BusProvider.getInstance().unregister(busEventListener);
        } catch (Exception e) {

        }

        super.onStop();
    }




    /* For DMR playback */

    @Override
    public void onStartComplete() {
        super.onStartComplete();
    }

    @Override
    protected void onDestroy() {
/*

        if (m_upnpProcessor != null) {
            m_upnpProcessor.removeListener(UpnpDeviceManager.getInstance());
            m_upnpProcessor.unbindUpnpService();
        }
*/

        this.libreDeviceInteractionListner = null;

        try {
            this.unregisterReceiver(networkStatereceiver);
            LibreLogger.d(this, "DeviceDiscovery onDestroy");
        } catch (Exception e) {
            LibreLogger.d(this, "Exception happened while unregistering the reciever!! " + e.getMessage());

        }
        super.onDestroy();


    }


    public CoreUpnpService.Binder getUpnpBinder() {
        return m_upnpProcessor.getBinder();
    }

    class LocalNetworkStateReceiver extends BroadcastReceiver {


        private static final String TAG = "NetworkStateReceiver";
        private NetworkInterface m_interfaceCache = null;

        private List<Handler> handlerList = new ArrayList<Handler>();

        public LocalNetworkStateReceiver() {
            //m_nwStateListener = nwStateListener;
        }

        public void registerforNetchange(Handler handle) {
            handlerList.add(handle);

        }

        public void alertBoxForNetworkOff() {
            if (!DeviceDiscoveryActivity.this.isFinishing()) {
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                        DeviceDiscoveryActivity.this);

                // set title
                alertDialogBuilder.setTitle(getString(R.string.wifiConnectivityStatus));

                // set dialog message
                alertDialogBuilder
                        .setMessage(getString(R.string.connectToWifi))
                        .setCancelable(false)
                        .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // if this button is clicked, close
                                // current activity
                                dialog.cancel();
                                LibreApplication.mCleanUpIsDoneButNotRestarted = false;
                                /*  startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));*/
                                Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                startActivityForResult(intent, 1234);

                            }
                        })
                        /*.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // if this button is clicked, just close
                                // the dialog box and do nothing
                                Toast.makeText(DeviceDiscoveryActivity.this, getString(R.string.connectToWifi), Toast.LENGTH_SHORT).show();
                                ;
                                dialog.cancel();
                            }
                        })*/;

                // create alert dialog
                if (alertDialog == null)
                    alertDialog = alertDialogBuilder.create();

                // show it

                alertDialog.show();
            }
        }

        public void unregisterforNetchange(Handler handle) {

            Iterator<Handler> i = handlerList.iterator();
            while (i.hasNext()) {
                Handler o = i.next();
                if (o == handle)
                    i.remove();
            }

        }


        @Override
        public void onReceive(final Context context, Intent intent) {

            final Context newContext = getApplicationContext();


            if (!listenToNetworkChanges)
                return;

            ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();


            if ((activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting()) == false &&
                    listenToWifiConnectingStatus) {
//                Toast.makeText(getApplicationContext(), "Network is disconnected", Toast.LENGTH_SHORT).show();

                try {

                    if (activeNetworkInfo != null) {
                        LibreLogger.d(this, "Active network info:" + activeNetworkInfo.isConnectedOrConnecting());
                        if (activeNetworkInfo.isConnected() && activeNetworkInfo.getType() == ConnectivityManager.TYPE_MOBILE)
                            LibreLogger.d(this, "Active network type:MOBILE");
                    } else {
                        LibreLogger.d(this, "Active network interface is null...and hence we will show the alert box");
                    }
                } catch (Exception e) {

                }

                alertBoxForNetworkOff();
                if (LibreApplication.activeSSID != null && !LibreApplication.activeSSID.equals(""))
                    LibreApplication.mActiveSSIDBeforeWifiOff = LibreApplication.activeSSID;
                LibreApplication.activeSSID = "";
                return;
            }

            if (!intent.getAction().equals("android.net.conn.TETHER_STATE_CHANGED")
                    && !intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION))
                return;

            WifiManager wifiManager =
                    (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

            final String ssid = getconnectedSSIDname(newContext);
            if (ssid == null || ssid.equals("") || ssid.equals("<unknown ssid>"))
                return;

            Log.d(TAG, "Libreid" + LibreApplication.activeSSID + "Connected id" + ssid);

            if (!ssid.equals(LibreApplication.activeSSID)) {
                if (!listenToNetworkChanges)
                    return;
                /* CleanUp To Be done */


                if (!ssid.contains(Constants.WAC_SSID)) {

                    //Toast.makeText(context, getString(R.string.restartingToast), Toast.LENGTH_LONG).show();
                    cleanUpcode(newContext, true);
                    // restartApp(newContext);

                    if (!DeviceDiscoveryActivity.this.isFinishing()) {
                        /* If Restarting of Network Is happening We can discard the Network Change */
                        try {
                            if (alertDialog != null && alertDialog.isShowing()) {
                                alertDialog.dismiss();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        AlertDialog.Builder builder = new AlertDialog.Builder(DeviceDiscoveryActivity.this);
                        String mMessage = String.format(getString(R.string.restartTitle));//, ssid, LibreApplication.mActiveSSIDBeforeWifiOff);
                        builder.setMessage(mMessage)
                                .setCancelable(false)
                                .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        alertRestartApp = null;
                                        //Toast.makeText(getApplicationContext(), getString(R.string.appRestarting), Toast.LENGTH_SHORT).show();
                                        restartApp(newContext);
                                        //restartApplicationForNetworkChanges(newContext, ssid);
                                    }
                                });
                           /* .setNegativeButton(getString(R.string.gotoSettings), new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                    startActivity(intent);
                                    alert.cancel();
                                }
                            });*/
                        if (alertRestartApp == null)
                            alertRestartApp = builder.create();

                        alertRestartApp.show();

                    }

                }
//                Toast.makeText(newContext, "Network changed", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "ssid" + ssid + "Libressid" + LibreApplication.activeSSID);


            }
            Log.d(TAG, "Receiver is changing");

        }

        public String getActiveSSID(Context context) {
            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            return wifiManager.getConnectionInfo().getSSID();
        }

        public NetworkInterface getActiveNetworkInterface() {

            Enumeration<NetworkInterface> interfaces = null;
            try {
                interfaces = NetworkInterface.getNetworkInterfaces();
            } catch (SocketException e) {
                return null;
            }

            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                Enumeration<InetAddress> inetAddresses = iface.getInetAddresses();


                /* Check if we have a non-local address. If so, this is the active
                 * interface.
                 *
                 * This isn't a perfect heuristic: I have devices which this will
                 * still detect the wrong interface on, but it will handle the
                 * common cases of wifi-only and Ethernet-only.
                 */
                if (iface.getName().startsWith("w")) {
                    //this is a perfect hack for getting wifi alone

                    while (inetAddresses.hasMoreElements()) {
                        InetAddress addr = inetAddresses.nextElement();

                        if (!(addr.isLoopbackAddress() || addr.isLinkLocalAddress())) {
                            Log.d("LSSDP", "DisplayName" + iface.getDisplayName() + "Name" + iface.getName());

                            return iface;
                        }
                    }
                }
            }

            return null;
        }

    }

    public void GoAndRemoveTheDevice(String ipadddress) {
        if (ipadddress != null) {
            if (libreDeviceInteractionListner != null)
                libreDeviceInteractionListner.deviceGotRemoved(ipadddress);
            LSSDPNodeDB mNodeDB1 = LSSDPNodeDB.getInstance();
            LSSDPNodes mNode = mNodeDB1.getTheNodeBasedOnTheIpAddress(ipadddress);
            String mIpAddress = ipadddress;


            LSSDPNodeDB mNodeDB = LSSDPNodeDB.getInstance();
            try {
                if (ScanningHandler.getInstance().isIpAvailableInCentralSceneRepo(mIpAddress)) {
                    boolean status = ScanningHandler.getInstance().removeSceneMapFromCentralRepo(mIpAddress);
                    LibreLogger.d(this, "Active Scene Adapter For the Master " + status);
                }
                try {
                    String masterIPAddress = mIpAddress;
                    /*for (String masterIPAddress : ScanningHandler.getInstance().centralSceneObjectRepo.keySet())*/
                    {
                        RemoteDevice renderingDevice = UpnpDeviceManager.getInstance().getRemoteDMRDeviceByIp(masterIPAddress);
                        String renderingUDN = renderingDevice.getIdentity().getUdn().toString();
                        PlaybackHelper playbackHelper = LibreApplication.PLAYBACK_HELPER_MAP.get(renderingUDN);

                        ScanningHandler mScanHandler = ScanningHandler.getInstance();
                        SceneObject currentSceneObject = mScanHandler.getSceneObjectFromCentralRepo(masterIPAddress);


                        try {
                            if (playbackHelper != null
                                    && renderingDevice != null
                                    && currentSceneObject != null
                                    && currentSceneObject.getPlayUrl() != null
                                    && !currentSceneObject.getPlayUrl().equalsIgnoreCase("")
                                    && currentSceneObject.getPlayUrl().contains(LibreApplication.LOCAL_IP)
                                    && currentSceneObject.getPlayUrl().contains(ContentTree.AUDIO_PREFIX)

                            ) {

                                playbackHelper.StopPlayback();
                            }
                        } catch (Exception e) {

                            LibreLogger.d(this, "Handling the exception while sending the stop command ");
                        }

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            } catch (Exception e) {
                LibreLogger.d(this, "Active Scene Adapter" + "Removal Exception ");
            }
            mNodeDB.clearNode(mIpAddress);
        }

    }

    public void alertDialogForDeviceNotAvailable(final LSSDPNodes mNode) {
        {
            if (!DeviceDiscoveryActivity.this.isFinishing()) {
                if (mNode == null)
                    return;
                alertDialog = null;
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                        DeviceDiscoveryActivity.this);

                // set title
                alertDialogBuilder.setTitle(getString(R.string.deviceNotAvailable));

                // set dialog message
                alertDialogBuilder
                        .setMessage(getString(R.string.removeDeviceMsg) + " " + mNode.getFriendlyname() + " " + getString(R.string.removeDeviceMsg2))
                        .setCancelable(false)
                        .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // if this button is clicked, close
                                // current activity
                                alertDialog.dismiss();
                                /*if (libreDeviceInteractionListner!=null)
                                libreDeviceInteractionListner.deviceGotRemoved(mNode.getIP());*/

                                GoAndRemoveTheDevice(mNode.getIP());

                            }
                        });

                // create alert dialog
                if (alertDialog == null)
                    alertDialog = alertDialogBuilder.create();

                // show it

                alertDialog.show();
            }
        }
    }

    public void disableNetworkOffCallBack() {
        listenToWifiConnectingStatus = false;
    }

    public void enableNetworkOffCallBack() {
        listenToWifiConnectingStatus = true;
    }

    public void disableNetworkChangeCallBack() {
        listenToNetworkChanges = false;
    }

    public void enableNetworkChangeCallBack() {
        listenToNetworkChanges = true;
    }

    /* Whenever we are going to Do app Restarting,its not restarting the app properly and We are doing Cleanup and then We are restarting the APP
     * Till i HAave to Analyse more on this code */
    public void cleanUpcode(Context context, boolean mCompleteCleanup) {
        LibreLogger.d(this, "Cleanup is going To Call");
        LibreApplication.mCleanUpIsDoneButNotRestarted = true;
        LibreApplication application = (LibreApplication) getApplication();
        try {


            for (NettyAndroidClient mAndroid : LUCIControl.luciSocketMap.values().toArray(new NettyAndroidClient[LUCIControl.luciSocketMap.size()])) {
                if (mAndroid != null && mAndroid.getHandler() != null && mAndroid.getHandler().mChannelContext != null)
                    mAndroid.getHandler().mChannelContext.close();
            }

            for (ChannelHandlerContext mAndroidChannelHandlerContext : LUCIControl.channelHandlerContextMap.values().toArray(new ChannelHandlerContext[
                    LUCIControl.channelHandlerContextMap.size()])) {
                if (mAndroidChannelHandlerContext != null)
                    mAndroidChannelHandlerContext.getChannel().close();
            }
            try {
                for (String masterIPAddress : ScanningHandler.getInstance().centralSceneObjectRepo.keySet()) {
                    RemoteDevice renderingDevice = UpnpDeviceManager.getInstance().getRemoteDMRDeviceByIp(masterIPAddress);
                    String renderingUDN = renderingDevice.getIdentity().getUdn().toString();
                    PlaybackHelper playbackHelper = LibreApplication.PLAYBACK_HELPER_MAP.get(renderingUDN);

                    String mIpaddress = masterIPAddress;
                    ScanningHandler mScanHandler = ScanningHandler.getInstance();
                    SceneObject currentSceneObject = mScanHandler.getSceneObjectFromCentralRepo(mIpaddress);


                    try {
                        if (playbackHelper != null
                                && renderingDevice != null
                                && currentSceneObject != null
                                && currentSceneObject.getPlayUrl() != null
                                && !currentSceneObject.getPlayUrl().equalsIgnoreCase("")
                                && currentSceneObject.getPlayUrl().contains(LibreApplication.LOCAL_IP)
                                && currentSceneObject.getPlayUrl().contains(ContentTree.AUDIO_PREFIX)

                        ) {

                            playbackHelper.StopPlayback();
                        }
                    } catch (Exception e) {

                        LibreLogger.d(this, "Handling the exception while sending the stop command ");
                    }

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            LibreApplication.PLAYBACK_HELPER_MAP.clear();
            /* *//* Got This Exception :  W/System.err﹕ java.lang.ArrayStoreException: source[0] of type java.lang.String
            cannot be stored in destination array of type com.libre.app.dlna.dmc.utility.PlaybackHelper[]*//**//**//*
            for (PlaybackHelper mHelper : LibreApplication.PLAYBACK_HELPER_MAP.values().toArray(new PlaybackHelper[LibreApplication.PLAYBACK_HELPER_MAP.size()])) {
                try {
                    if (mHelper != null)
                        mHelper.StopPlayback();
                } catch (Exception e) {

                }*/


            application.getScanThread().clearNodes();
            application.getScanThread().mRunning = false;
            application.getScanThread().close();

            application.getScanThread().mAliveNotifyListenerSocket = null;
            application.getScanThread().mDatagramSocketForSendingMSearch = null;
            ScanningHandler.getInstance().clearSceneObjectsFromCentralRepo();

            if (mCompleteCleanup) {
                this.libreDeviceInteractionListner = null;
                try {
                    BusProvider.getInstance().unregister(busEventListener);
                } catch (Exception e) {

                }
            }
/*
            application.getScanThread().mMulticastSocketAliveNotify.close();
            application.getScanThread().mMulticastSocket.close();*/

            /* Adding to make sure the ne chane
            if (application.isApplicationActiveOnUI()) {
                Toast.makeText(context, "Network changed", Toast.LENGTH_LONG).show();
            /*ActivityCompat.finishAffinity(this);*/

            //     if (LibreApplication.isApplicationActiveOnUI())
            //  Toast.makeText(context, getString(R.string.restartingToast), Toast.LENGTH_LONG).show();
            if (application.getScanThread().nettyServer.mServerChannel != null && application.getScanThread().nettyServer.mServerChannel.isBound()) {
                application.getScanThread().nettyServer.mServerChannel.unbind();
                ChannelFuture serverClose = application.getScanThread().nettyServer.mServerChannel.close();
                serverClose.awaitUninterruptibly();


                /*to resolve hang issue*/
//                application.getScanThread().nettyServer.bootstrap.releaseExternalResources();
            }

            application.closeScanThread();
            LUCIControl.luciSocketMap.clear();
            LUCIControl.channelHandlerContextMap.clear();
            LSSDPNodeDB.getInstance().clearDB();
            application.clearApplicationCollections();
            LibreLogger.d(this, "Cleanup GOintFor Binder ");
            if (getUpnpBinder() == null)
                return;
            try {
                if (m_upnpProcessor != null) {
                    m_upnpProcessor.removeListener(this);
                    m_upnpProcessor.removeListener(UpnpDeviceManager.getInstance());
                    m_upnpProcessor.unbindUpnpService();

                    LibreLogger.d(this, "Cleanup UnBinded THe Service ");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        LibreLogger.d(this, "Cleanup is done Successfully");
        Log.d("NetworkChanged", "BroadcastReceiver Intent");
    }

    /* Created by Karuna, To fix the RestartApp Issue
     * * Till i HAave to Analyse more on this code */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public void restartApp(Context context) {
        Log.d("NetworkChanged", "App is Restarting");
        //finish();
        /* Stopping ForeGRound Service Whenwe are Restarting the APP
        * but as now commenting it since it is not required for genie
        * */
//        Intent in = new Intent(DeviceDiscoveryActivity.this, DMRDeviceListenerForegroundService.class);
//        in.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        in.setAction(Constants.ACTION.STOPFOREGROUND_ACTION);
//        stopService(in);

        Intent mStartActivity = new Intent(context, SpalshScreenActivity.class);
        /*sending to let user know that app is restarting*/
        mStartActivity.putExtra(SpalshScreenActivity.APP_RESTARTING, true);
        int mPendingIntentId = 123456;
        PendingIntent mPendingIntent = PendingIntent.getActivity(context, mPendingIntentId, mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager mgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 200, mPendingIntent);

        /* * Finish this activity, and tries to finish all activities immediately below it
         * in the current task that have the same affinity.*/
        ActivityCompat.finishAffinity(this);
        /* Killing our Android App with The PID For the Safe Case */
        int pid = android.os.Process.myPid();
        android.os.Process.killProcess(pid);
        //System.exit(0);

    }

    /* Restarting Alll my Sockets whenever Configuring a SAC Device to SAME AP .,
     * * Till i HAave to Analyse more on this code */
    public boolean restartAllSockets(Context context) {

        ConnectivityManager connection_manager =
                (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkRequest.Builder request = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            /*request = new NetworkRequest.Builder();

            request.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);

            connection_manager.registerNetworkCallback(request.build(), new ConnectivityManager.NetworkCallback() {

                @Override
                public void onAvailable(Network network) {

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//                        ConnectivityManager.setProcessDefaultNetwork(network);
                        LibreLogger.d(this,"Karuna 1" + "App Called Here Device");

                    }
                }
            });*/

            /*LibreApplication application = (LibreApplication) getApplication();
            application.initiateServices();*/
        } else {
            LibreLogger.d(this, "Karuna 2" + "App Called Here Device 1");
            LibreApplication application = (LibreApplication) getApplication();
            application.initiateServices();

        }

        return true;

    }


    public String phoneIpAddress() {
        Utils mUtil = new Utils();
        return mUtil.getIPAddress(true);
    }

    /* Restartie Applicaiton whenever a network Change happend in the APP .,
     * * Till i HAave to Analyse more on this code */
    public void restartApplicationForNetworkChanges(Context context, String ssid) {
        cleanUpcode(context, true);
        restartApp(context);
       /* Log.e(TAG, "ssid" + ssid + "Libressid" + LibreApplication.activeSSID);
        LibreApplication.activeSSID = ssid;
        LibreApplication application = (LibreApplication) context.getApplicationContext();
        try {
            if (getUpnpBinder() == null)
                return;

            getUpnpBinder().renew();
            application.getScanThread().clearNodes();
            application.getScanThread().mRunning = false;

            ScanningHandler.getInstance().clearSceneObjectsFromCentralRepo();
            application.restart();
            Toast.makeText(context, "Network changed", Toast.LENGTH_SHORT).show();
            if (application.getScanThread().nettyServer.mServerChannel != null && application.getScanThread().nettyServer.mServerChannel.isBound()) {
                application.getScanThread().nettyServer.mServerChannel.unbind();
                ChannelFuture serverClose = application.getScanThread().nettyServer.mServerChannel.close();
                serverClose.awaitUninterruptibly();


                *//*to resolve hang issue*//*
//                application.getScanThread().nettyServer.bootstrap.releaseExternalResources();
            }

        } catch (SocketException e) {
            e.printStackTrace();
        }

        Log.d("NetworkChanged", "BroadcastReceiver Intent");
        //finish();
        Intent mStartActivity = new Intent(context, SpalshScreenActivity.class);
                *//*sending to let user know that app is restarting*//*
        mStartActivity.putExtra(SpalshScreenActivity.APP_RESTARTING, true);
        int mPendingIntentId = 123456;
        PendingIntent mPendingIntent = PendingIntent.getActivity(context, mPendingIntentId, mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager mgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 300, mPendingIntent);
        System.exit(0);*/
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1234) {
            LibreLogger.d(this, "came back from wifi list");
            ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            if (mWifi.isConnected()) {
                if (!DeviceDiscoveryActivity.this.isFinishing()) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(DeviceDiscoveryActivity.this);
                    String mMessage = String.format(getString(R.string.restartTitle));//, ssid, LibreApplication.mActiveSSIDBeforeWifiOff);
                    builder.setMessage(mMessage)
                            .setCancelable(false)
                            .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    alertRestartApp = null;

                                    LibreApplication.mCleanUpIsDoneButNotRestarted = true;
                                    //Toast.makeText(getApplicationContext(), getString(R.string.appRestarting), Toast.LENGTH_SHORT).show();
                                    restartApp(getApplicationContext());
                                    //restartApplicationForNetworkChanges(newContext, ssid);
                                }
                            });
                           /* .setNegativeButton(getString(R.string.gotoSettings), new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                    startActivity(intent);
                                    alert.cancel();
                                }
                            });*/
                    if (alertRestartApp == null)
                        alertRestartApp = builder.create();

                    alertRestartApp.show();

                }
            }
        }
    }


    private void checkForTheSACDeviceSuccessDialog(LSSDPNodes node) {

        SharedPreferences sharedPreferences = getApplicationContext()
                .getSharedPreferences("sac_configured", Context.MODE_PRIVATE);
        String str = sharedPreferences.getString("deviceFriendlyName", "");
        if (str != null && str.equalsIgnoreCase(node.getFriendlyname().toString()) && node.getgCastVerision() != null) {


            LibreApplication.thisSACDeviceNeeds226 = node.getFriendlyname();
            if (LibreApplication.GCAST_UPDATE_AVAILABE_LIST_DATA.containsKey(node.getIP()))
                LibreApplication.GCAST_UPDATE_AVAILABE_LIST_DATA.remove(node.getIP());

            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                    this);

            // set title
            alertDialogBuilder.setTitle("Configure successful");

            // set dialog message
            alertDialogBuilder
                    .setMessage("Cast your favorite music and radio apps from your phone or tablet to your " + node.getFriendlyname().toString())
                    .setCancelable(false)
                    .setNegativeButton("No Thanks", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.cancel();
                        }
                    })
                    .setPositiveButton("Learn More", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.setData(Uri.parse("https://www.google.com/cast/audio/learn"));
                            startActivity(intent);
                        }
                    });

            AlertDialog sAcalertDialog = alertDialogBuilder.create();
            /*sAcalertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);*/
            sAcalertDialog.show();
            sharedPreferences.edit().remove("deviceFriendlyName").commit();
        } else if (node != null && node.getgCastVerision() == null && str.equalsIgnoreCase(node.getFriendlyname())) {
            sharedPreferences.edit().remove("deviceFriendlyName").commit();
        }

    }


    AlertDialog sAcalertDialog = null;

    public void showAlertDialogMessageForGCastMsgBoxes(String Message, LSSDPNodes node) {
        if (!LibreApplication.thisSACConfiguredFromThisApp.equalsIgnoreCase(""))
            return;

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                DeviceDiscoveryActivity.this);

        // set title
        alertDialogBuilder.setTitle("Firmware Upgrade");

        // set dialog message
        alertDialogBuilder
                .setMessage(Message + node.getFriendlyname().toString())
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        sAcalertDialog = null;
                    }
                });
        if (sAcalertDialog == null)
            sAcalertDialog = alertDialogBuilder.create();

        /*sAcalertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);*/
        sAcalertDialog.show();
        try {
            sAcalertDialog.show();
        } catch (Exception e) {
            LibreLogger.d(this, "Permission Denied");
        }
    }

    // overloaded this method because, on success we have to show "Device is Rebooting(an extra text at the end)
    public void showAlertDialogMessageForGCastMsgBoxes(String Message, LSSDPNodes node, String Message2) {
        if (!LibreApplication.thisSACConfiguredFromThisApp.equalsIgnoreCase(""))
            return;

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                DeviceDiscoveryActivity.this);

        // set title
        alertDialogBuilder.setTitle("Firmware Upgrade");

        // set dialog message
        alertDialogBuilder
                .setMessage(Message + node.getFriendlyname().toString() + Message2)
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        sAcalertDialog = null;
                    }
                });
        if (sAcalertDialog == null)
            sAcalertDialog = alertDialogBuilder.create();


        sAcalertDialog.show();
        try {
            sAcalertDialog.show();
        } catch (Exception e) {
            LibreLogger.d(this, "Permission Denied");
        }
    }


    AlertDialog malert = null;

    public void showAlertDialogForGcastUpdate(LSSDPNodes mNode, final GcastUpdateData mGcastData) {
        if (!LibreApplication.thisSACConfiguredFromThisApp.equalsIgnoreCase(""))
            return;
        AlertDialog.Builder builder = new AlertDialog.Builder(DeviceDiscoveryActivity.this);
        // set title
        builder.setTitle("Firmware Upgrade");
        builder.setMessage(mNode.getFriendlyname() + " New Update is available. firmware upgrade in progress with New Update")
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                        malert = null;
                        /**which means you have got
                         *  for current master and navigate to ActiveScene*/
                        BusProvider.getInstance().post(mGcastData);

                    }
                });
        // create alert dialog

        if (malert == null)
            malert = builder.create();

        /*malert.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);*/
        try {
            malert.show();
        } catch (Exception e) {
            LibreLogger.d(this, "Permission Denied");
        }
    }

}