package com.libre.alexa;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.widget.Toast;

import com.libre.alexa.luci.DiscoverySearchService;
import com.libre.alexa.luci.LSSDPNodes;
import com.libre.alexa.luci.LUCIPacket;
import com.libre.alexa.netty.BusProvider;
import com.libre.alexa.netty.LibreDeviceInteractionListner;
import com.libre.alexa.netty.NettyData;
import com.squareup.otto.Subscribe;

/**
 * Created by praveena on 8/4/15.
 */
public class DeviceDiscoveryFragment extends Fragment {

    private static final String TAG = DiscoverySearchService.class.getName();
    LibreDeviceInteractionListner libreDeviceInteractionListner;
    Object busEventListener = new Object() {
        @Subscribe
        public void newDeviceFound(final LSSDPNodes nodes) {

            /* This below if loop is introduced to handle the case where Device state from the DUT could be Null sometimes
            * Ideally the device state should not be null but we are just handling it to make sure it will not result in any crash!
            *
            * */
            if (nodes==null||nodes.getDeviceState()==null)
            {

                if (getActivity()!=null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getActivity(), getString(R.string.deviceStateNull) + nodes.getDeviceState(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                return;
            }
            else if(libreDeviceInteractionListner!=null)
            {
                libreDeviceInteractionListner.newDeviceFound(nodes);
            }
        }

        @Subscribe
        public void newMessageRecieved(NettyData nettyData){

            if(libreDeviceInteractionListner!=null)
            {
                LUCIPacket packet=new LUCIPacket(nettyData.getMessage());
                libreDeviceInteractionListner.messageRecieved(nettyData);
            }
        }

        @Subscribe
        public void deviceGotRemoved(String ipaddress){

            if(libreDeviceInteractionListner!=null)
            {
                libreDeviceInteractionListner.deviceGotRemoved(ipaddress);
            }
        }

    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public  void registerForDeviceEvents(LibreDeviceInteractionListner libreListner){
        this.libreDeviceInteractionListner=libreListner;
    }

    public  void unRegisterForDeviceEvents(){
        this.libreDeviceInteractionListner=null;
    }




    @Override
    public void onResume() {
        super.onResume();
        try {
            BusProvider.getInstance().register(busEventListener);
        }catch (Exception e){

        }
    }

    @Override
    public void onStop() {
        super.onStop();
        try {
            BusProvider.getInstance().unregister(busEventListener);
        }
        catch (Exception e){

        }
    }
    
}
