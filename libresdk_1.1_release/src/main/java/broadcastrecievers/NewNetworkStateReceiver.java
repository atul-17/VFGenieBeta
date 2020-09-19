package broadcastrecievers;

/**
 * Created by praveena on 10/1/15.
 */


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.libre.alexa.ActiveScenesListActivity;
import com.libre.alexa.LibreApplication;
import com.libre.alexa.R;
import com.libre.alexa.SpalshScreenActivity;
import com.libre.alexa.app.dlna.dmc.processor.impl.UpnpProcessorImpl;
import com.libre.alexa.app.dlna.dmc.processor.upnp.CoreUpnpService;

import org.jboss.netty.channel.ChannelFuture;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;


public class NewNetworkStateReceiver extends BroadcastReceiver {


    private static final String TAG = "NetworkStateReceiver";
    private NetworkInterface m_interfaceCache = null;

    private static List<Handler> handlerList = new ArrayList<Handler>();
    private CoreUpnpService.Binder upnpBinder;

    public NewNetworkStateReceiver() {
        //m_nwStateListener = nwStateListener;
    }

    public static void registerforNetchange(Handler handle) {
        handlerList.add(handle);

    }

    public static void unregisterforNetchange(Handler handle) {

        Iterator<Handler> i = handlerList.iterator();
        while (i.hasNext()) {
            Handler o = i.next();
            if (o == handle)
                i.remove();
        }

    }


    @Override
    public void onReceive(final Context context, Intent intent) {

        final Context newContext = context;

//        if (!listenToNetworkChanges)
//            return;
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();


        if ((activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting()) == false) {
//                        alertBoxForNetworkOff();
            LibreApplication.activeSSID = "";
            return;
        }


        if (!intent.getAction().equals("android.net.conn.TETHER_STATE_CHANGED")
                && !intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION))
            return;


        WifiManager wifiManager =
                (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        String ssid = getActiveSSID(context);
        if (ssid == null || ssid.equals(""))
            return;

        Log.d(TAG, "Libreid" + LibreApplication.activeSSID + "Connected id" + ssid);

        if (!ssid.equals(LibreApplication.activeSSID))

        {
            Log.e(TAG, "ssid" + ssid + "Libressid" + LibreApplication.activeSSID);

            NetworkInterface netIf = getActiveNetworkInterface();
            if (netIf == null)
                return;

            LibreApplication.activeSSID = ssid;
            LibreApplication application = (LibreApplication) context.getApplicationContext();
            try {
                if (getUpnpBinder(context) == null)
                {
                    Log.d(TAG, "onReceive: UPNPBinder is null");
                    return;
                }

              //  getUpnpBinder(context).renew();
                application.getScanThread().mRunning = false;
                /*clearing all collection associated with application*/
                application.clearApplicationCollections();
                application.restart();
                Toast.makeText(context, R.string.networkChanged, Toast.LENGTH_SHORT).show();
                if (application.getScanThread().nettyServer.mServerChannel != null && application.getScanThread().nettyServer.mServerChannel.isBound()) {
                    application.getScanThread().nettyServer.mServerChannel.unbind();
                    ChannelFuture serverClose = application.getScanThread().nettyServer.mServerChannel.close();
                    serverClose.awaitUninterruptibly();
                }
            } catch (SocketException e) {
                e.printStackTrace();
            } finally {
                Log.d("NetworkChanged", "BroadcastReceiver Intent");
                //finish();
                Intent mStartActivity = new Intent(context, ActiveScenesListActivity.class);
                /*sending to let user know that app is restarting*/
                mStartActivity.putExtra(SpalshScreenActivity.APP_RESTARTING, true);
                context.startActivity(mStartActivity);
            }
            Log.e(TAG, "ssid" + ssid + "Libressid" + LibreApplication.activeSSID);
        }
        Log.d(TAG, "Receiver is changing");

    }

    public String getActiveSSID(Context context) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        return wifiManager.getConnectionInfo().getSSID();
    }

    public static NetworkInterface getActiveNetworkInterface() {

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

    public CoreUpnpService.Binder getUpnpBinder(Context context) {
        UpnpProcessorImpl upnpProcessor = new UpnpProcessorImpl(context);
        return upnpProcessor.getBinder();
    }
}
