
package broadcastrecievers;

/**
 * Created by praveena on 10/1/15.
 */


        import android.content.BroadcastReceiver;
        import android.content.Context;
        import android.content.Intent;
        import android.net.ConnectivityManager;
        import android.net.wifi.WifiManager;
        import android.os.Handler;
        import android.util.Log;
        import android.widget.Toast;

        import com.libre.alexa.LibreApplication;
        import com.libre.alexa.R;

        import java.net.InetAddress;
        import java.net.NetworkInterface;
        import java.net.SocketException;
        import java.util.ArrayList;
        import java.util.Enumeration;
        import java.util.Iterator;
        import java.util.List;


class NetworkReceiver extends BroadcastReceiver {


    private static final String TAG = "NetworkStateReceiver";
    private NetworkInterface m_interfaceCache = null;

    private static List<Handler> handlerList = new ArrayList<Handler>();

    public NetworkReceiver() {
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
        if (!intent.getAction().equals("android.net.conn.TETHER_STATE_CHANGED")
                && !intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION))
            return;


        WifiManager wifiManager =
                (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        String ssid = getActiveSSID(context);
        if (ssid == null || ssid.equals(""))
            return;

        Log.d(TAG, "Libreid" + LibreApplication.activeSSID + "Connected id" + ssid);

        if (!ssid.equals(LibreApplication.activeSSID)) {
            Log.e(TAG, "ssid" + ssid + "Libressid" + LibreApplication.activeSSID);

            NetworkInterface netIf = getActiveNetworkInterface();
            if (netIf == null)
                return;

            LibreApplication.activeSSID = ssid;
            LibreApplication.mActiveSSIDBeforeWifiOff = LibreApplication.activeSSID;
            LibreApplication application = (LibreApplication) context.getApplicationContext();
            try {
                application.getScanThread().clearNodes();
                application.restart();
                Toast.makeText(context, R.string.networkChanged, Toast.LENGTH_SHORT).show();

            } catch (SocketException e) {
                e.printStackTrace();
            } catch (NullPointerException e) {
                e.printStackTrace();
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
}
