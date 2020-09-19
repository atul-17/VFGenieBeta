package com.libre.alexa.luci;
/*

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.libre.constants.LSSDPCONST;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import com.libre.netty.NettyAndroidClient;

*/
/**
 * ******************************************************************************************client
 * <p/>
 * Copyright (C) 2014 Libre Wireless Technology
 * <p/>
 * "Junk Yard Lab" Project
 * <p/>
 * Libre Sync Android App
 * Author: Subhajeet Roy
 * <p/>
 * *********************************************************************************************
 *//*



public class LUCIControl {
    private static final String TAG = LUCIControl.class.getSimpleName();
    private Handler m_handler;

    private Handler sending_m_handler;
    private Socket socket;
    //  private  static LUCIControl luciControl;

    private static final int LUCI_CONTROL_PORT = 7777;
    private NetworkInterface mNetIf;
    Thread serverThread = null;
    ServerThread SERVER;
    private String SERVER_IP;
    volatile boolean shutdown = false;
    public static final int LUCI_RESP_PORT = 3333;
    public static HashMap<String, NettyAndroidClient> luciSocketMap = new HashMap<String, NettyAndroidClient>();
    public int tcpSendDataResult = -1;

    public void addhandler(Handler handler) {
        m_handler = handler;
    }

    public void close() {
        shutdown = true;
        Log.d("ZoneMasterFragment", "Shutdown");
    }


    public void shutdown() {
        SERVER.shutdown();
    }


    public LUCIControl() {
        shutdown = true;
    }

    public LUCIControl(String serverIP) {
        shutdown = false;
        SERVER_IP = serverIP;
        mNetIf = Utils.getActiveNetworkInterface();

        SendCommand(3, Utils.getLocalV4Address(mNetIf).getHostAddress() + "," + "3333", LSSDPCONST.LUCI_SET);
        Log.v(TAG, "INIT LUCICONTROL with= " + serverIP + " OurIP=" + Utils.getLocalV4Address(mNetIf).getHostAddress());
        try {

            SERVER = new ServerThread();
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.serverThread = new Thread(SERVER);

        this.serverThread.start();
    }

    public static int TcpSendData(NettyAndroidClient tcpSocket, byte[] message) {

            tcpSocket.write(message);
               return 1;

        */
/*try {
            writer = tcpSocket.getOutputStream();
            Log.e(TAG, "sendto IP " + tcpSocket.getInetAddress().getHostAddress() + tcpSocket.getPort());
            Log.e(TAG, "SendLuciCommand Sent");
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            writer.write(message);
            writer.flush();
            return 1;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return 0;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }*//*


    }

    public void SendLUCICommand(final int MID, final String Data, final String IP) {
        new Thread() {
            public void run() {
                String messageData = null;
                NettyAndroidClient tcpSocketSendCtrl = null;
                int result = -1;
                Log.e(TAG, "SendLUCICommand" + MID);
                Log.e(TAG, "SendLUCICommand" + IP);

                //Todo:
                LUCIPacket packet = new LUCIPacket(Data.getBytes(), (short) Data.length(), (short) MID);

                int server_port = Integer.parseInt("7777");

                InetAddress local = null;
                try {
                    local = InetAddress.getByName(IP);
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
                Log.e("HOSTIP LUCI", IP);
                if (!(luciSocketMap.containsKey(IP))) {

                    try {
                        tcpSocketSendCtrl = new NettyAndroidClient(local, server_port);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    Log.e(TAG, "NewSocket");

                } else {
                    tcpSocketSendCtrl = luciSocketMap.get(IP);
                    Log.e(TAG, "OlderOne");
                }
                Log.e(TAG, "SendLUCICommand" + tcpSocketSendCtrl);

                int msg_length = packet.getlength();
                byte[] message = new byte[msg_length];
                packet.getPacket(message);

                Log.e("LMP", "Mid = " + packet.getCommand() + "Mdata = " + new String(message));
                if (message != null) {
                    tcpSendDataResult = TcpSendData(tcpSocketSendCtrl, message);
                    */
/*try                                                                                                                                                                              {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }*//*

                }
                    else
                    Log.e(TAG, "message is null");
                if (tcpSendDataResult != 0) {
                    Log.d(TAG, "Tcp SendLUCICommand is Successfull ");
                } else {
                    tcpSendDataResult = -1;
                    Log.d(TAG, "Tcp SendLUCICommand is UnSuccessfull ");
                }

              */
/*  try {
                    BufferedReader inputReader = new BufferedReader(new InputStreamReader(tcpSocketSendCtrl.getInputStream()));
                    Log.e(TAG,"INPUTSTREAM");
                    System.out.println("Client said :" + inputReader.readLine());
                }catch(Exception e){
                    Log.e(TAG,"ERROR INPUTSTREAM");
                    e.printStackTrace();
                }*//*

            }
        }.start();


    }


    public void GetLUCICommand(final int MID, final String Data, final String IP) {
        new Thread() {
            public void run() {
                String messageData = null;
                final LUCIPacket packet;
                NettyAndroidClient tcpSocketSendCtrl = null;

                Log.e(TAG, "Get LUCICommand" + MID);
                //Todo:
                if (Data != null) {
                    packet = new LUCIPacket(Data.getBytes(), (short) Data.length(), (short) MID, (byte) LSSDPCONST.LUCI_GET);
                } else {
                    packet = new LUCIPacket(null, (short) 0, (short) MID, (byte) LSSDPCONST.LUCI_GET);
                }
                int server_port = LUCI_CONTROL_PORT;
                int msg_length = packet.getlength();
                byte[] message = new byte[msg_length];
                packet.getPacket(message);
                Log.e("Sathi", "Mid = " + packet.getCommand() + "ServerIP" + IP + "LocalIP" + SERVER_IP);
                InetAddress local = null;
                try {
                    local = InetAddress.getByName(IP);
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
                if (!(luciSocketMap.containsKey(IP))) {

                    try {
                        tcpSocketSendCtrl = new NettyAndroidClient(local, server_port);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
               } else {
                    tcpSocketSendCtrl = luciSocketMap.get(IP);
                }

                if (message != null)

                    tcpSendDataResult = TcpSendData(tcpSocketSendCtrl, message);
                else
                    Log.e(TAG, "message is null");
                if (tcpSendDataResult != 0) {
                    Log.d(TAG, "Tcp GetLUCICommand is Successfull ");
                } else {
                    tcpSendDataResult = -1;
                    Log.d(TAG, "Tcp GetLUCICommand is UnSuccessfull ");
                }

            }
        }.start();


    }

    public void SendCommand(final int cmd, final String data, final int cmd_type) {
        new Thread() {
            public void run() {
                String messageStr = null;
                LUCIPacket packet = null;
                messageStr = data;
                NettyAndroidClient tcpSocketSendCtrl = null;

                if (data != null)
                    packet = new LUCIPacket(messageStr.getBytes(), (short) messageStr.length(), (short) cmd, (byte) cmd_type);
                else
                    packet = new LUCIPacket(null, (short) 0, (short) cmd, (byte) cmd_type);

                int server_port = LUCI_CONTROL_PORT;

                InetAddress local = null;
                try {
                    local = InetAddress.getByName(SERVER_IP);
                } catch (UnknownHostException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                int msg_length = packet.getlength();

                byte[] message = new byte[msg_length];
                packet.getPacket(message);

                if (!(luciSocketMap.containsKey(SERVER_IP))) {

                    try {
                        tcpSocketSendCtrl = new NettyAndroidClient(local, server_port);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
//                    luciSocketMap.put(SERVER_IP, tcpSocketSendCtrl);
                } else {
                    tcpSocketSendCtrl = luciSocketMap.get(SERVER_IP);
                }
                tcpSendDataResult = TcpSendData(tcpSocketSendCtrl, message);

                if (tcpSendDataResult != 0) {
                    Log.d(TAG, "Tcp GetLUCICommand is Successfull ");
                } else {
                    tcpSendDataResult = -1;
                    Log.d(TAG, "Tcp GetLUCICommand is UnSuccessfull ");
                }

            }

        }.start();

    }


    public void SendCommand(final List<LUCIPacket> luciPackets) {
        new Thread() {
            public void run() {
                String messageStr = null;
                LUCIPacket packet = null;

                for (LUCIPacket luciPacket : luciPackets) {
                    int server_port = LUCI_CONTROL_PORT;
                    // DatagramSocket s = null;
                    NettyAndroidClient tcpSocketSendCtrl = null;
                    InetAddress local = null;
                    try {
                        local = InetAddress.getByName(SERVER_IP);
                    } catch (UnknownHostException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                    int msg_length = luciPacket.getlength();

                    byte[] message = new byte[msg_length];
                    luciPacket.getPacket(message);

                    Log.e("Sathi", " before sending as a packet Messagebox id" + luciPacket.getCommand());
                    Log.v("Sathi", new String(message, 0));
                    if (!(luciSocketMap.containsKey(SERVER_IP))) {
                        try {
                            tcpSocketSendCtrl = new NettyAndroidClient(local, server_port);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
//                        luciSocketMap.put(SERVER_IP, tcpSocketSendCtrl);
                    } else {
                        tcpSocketSendCtrl = luciSocketMap.get(SERVER_IP);
                    }
                    */
/*try {
                        tcpSocketSendCtrl = new Socket(SERVER_IP,server_port);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }*//*

                    tcpSendDataResult = TcpSendData(tcpSocketSendCtrl, message);

                 */
/*   try {
                        Thread.sleep(250);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
*//*

                    if (tcpSendDataResult != 0) {
                        Log.d(TAG, "Tcp SendCommand is Successfull ");
                    } else {
                        tcpSendDataResult = -1;
                        Log.d(TAG, "Tcp SendCommand is UnSuccessfull ");
                    }
                }
            }
        }.start();

    }


    public static void SendCommandWithIp(final int cmd, final String data, final int cmd_type,final String ip) {
        new Thread() {
            public void run() {
                String messageStr = null;
                LUCIPacket packet = null;
                messageStr = data;
                NettyAndroidClient tcpSocketSendCtrl = null;

                if (data != null)
                    packet = new LUCIPacket(messageStr.getBytes(), (short) messageStr.length(), (short) cmd, (byte) cmd_type);
                else
                    packet = new LUCIPacket(null, (short) 0, (short) cmd, (byte) cmd_type);

                int server_port = LUCI_CONTROL_PORT;

                InetAddress local = null;
                try {
                    local = InetAddress.getByName(ip);
                } catch (UnknownHostException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                int msg_length = packet.getlength();

                byte[] message = new byte[msg_length];
                packet.getPacket(message);

                if (!(luciSocketMap.containsKey(ip))) {

                    try {
                        tcpSocketSendCtrl = new NettyAndroidClient(local, server_port);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
//                    luciSocketMap.put(SERVER_IP, tcpSocketSendCtrl);
                } else {
                    tcpSocketSendCtrl = luciSocketMap.get(ip);
                }
                 TcpSendData(tcpSocketSendCtrl, message);

            }

        }.start();

    }


    class ServerThread implements Runnable {
        private DatagramSocket mUnicastSocket;
        private NetworkInterface mNetIf;

        public ServerThread() throws IOException {
            mNetIf = Utils.getActiveNetworkInterface();
        }

        public void run() {
            Log.e(TAG, "Receiver is created");
            try {
                mUnicastSocket = new DatagramSocket(null);
                mUnicastSocket.setReuseAddress(true);
                mUnicastSocket.bind(new InetSocketAddress(Utils.getLocalV4Address(mNetIf), LUCI_RESP_PORT));
            } catch (IOException e) {
                Log.e(TAG, "Failed to setup socket ", e);
            }

            while (!shutdown) {
                DatagramPacket dp = null;
                try {
                    dp = receive();
                    InetAddress addr = dp.getAddress();
                    byte[] buffer;
                    buffer = dp.getData();
                    String aString = new String(buffer);
                    String cutString = aString.substring(0, 100);
                    Log.v(TAG, "Response Data: " + cutString);
                    //for(int i=0;i<dp.getLength();i++)
                    //Log.v(TAG,"buffer="+buffer[i]);
                    LUCIPacket packet = new LUCIPacket(buffer);
                    if (m_handler != null) {
                        Message msg = new Message();// = ((Message) m_handler).obtain(m_handler, 0x10, node);
                        msg.what = LSSDPCONST.LUCI_RESP_RECIEVED;
                        msg.obj = packet;
                        m_handler.sendMessage(msg);
                    }
                } catch (IOException e) {
                    Log.e(TAG, " fail.", e);
                }
            }

        }

        public synchronized void shutdown() {
            if (mUnicastSocket.isBound() || !mUnicastSocket.isClosed()) {
                mUnicastSocket.disconnect();
                mUnicastSocket.close();
            }
            Socket tcpsocket = null;
            // Iterate over all vehicles, using the keySet method.
          */
/*  for(String key: luciSocketMap.keySet()) {
                tcpsocket = luciSocketMap.get(key);
                try {
                    tcpsocket.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }*//*

            //  luciSocketMap.clear();
        }

        DatagramPacket receive() throws IOException {
            byte[] buf = new byte[2048];
            DatagramPacket dp = new DatagramPacket(buf, buf.length);
            mUnicastSocket.receive(dp);
            return dp;
        }

    }


}

*/


import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import com.libre.alexa.LibreApplication;
import com.libre.alexa.Scanning.Constants;
import com.libre.alexa.constants.LSSDPCONST;
import com.libre.alexa.constants.MIDCONST;
import com.libre.alexa.netty.NettyAndroidClient;
import com.libre.alexa.util.GoogleTOSTimeZone;
import com.libre.alexa.util.LibreLogger;

import org.jboss.netty.channel.ChannelHandlerContext;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Khajan on 1/8/15.
 */
public class LUCIControl {
    private static final String TAG = LUCIControl.class.getSimpleName();
    private static final int LUCI_CONTROL_PORT = 7777;
    private NetworkInterface mNetIf;
    private String SERVER_IP;
    public static final int LUCI_RESP_PORT = 3333;
    public static ConcurrentHashMap<String, NettyAndroidClient> luciSocketMap = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, ChannelHandlerContext> channelHandlerContextMap = new ConcurrentHashMap<>();
    private boolean isDataSent;

    public LUCIControl(String serverIP) {
        SERVER_IP = serverIP;
    }

    public static boolean TcpSendData(NettyAndroidClient tcpSocket, byte[] message) {
        if (tcpSocket != null) {
            tcpSocket.write(message);
        }
        return true;
    }

    public void deRegister() {
        SendCommand(4, null, LSSDPCONST.LUCI_SET);
    }

    public void sendAsynchronousCommandSpecificPlaces(){
        mNetIf = Utils.getActiveNetworkInterface();
        /* Crash Fixed Reported For Multiple Times SAC and rebooting Fix*/
/*        if ((mNetIf != null)  && (new Utils().getIPAddress(true)!=null))*/ {
            SendCommand(3, new Utils().getIPAddress(true) + "," + LUCI_RESP_PORT, LSSDPCONST.LUCI_SET);
            Log.v(TAG, "INIT LUCICONTROL with= " + SERVER_IP + " OurIP=" +  new Utils().getIPAddress(true) );
        }
    }
    public void sendAsynchronousCommand() {
        /*mNetIf = Utils.getActiveNetworkInterface();
        if (mNetIf != null) {
            SendCommand(3, Utils.getLocalV4Address(mNetIf).getHostAddress() + "," + LUCI_RESP_PORT, LSSDPCONST.LUCI_SET);
            Log.v(TAG, "INIT LUCICONTROL with= " + SERVER_IP + " OurIP=" + Utils.getLocalV4Address(mNetIf).getHostAddress());
        }*/
    }

    public boolean ping(String url) {
        InetAddress addr = null;
        try {
            addr = InetAddress.getByName(url);
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;

        }
        try {
            if (addr.isReachable(2500)) {
                LibreLogger.d(this, "InetAddress" + "\n" + url + "- Respond OK");
                return true;
            } else {
                LibreLogger.d(this, "InetAddress" + "\n" + url + "- Respond NOT OK");
                return false;
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            return false;
        }
    }


    public void SendCommand(final int MID, final String data, final int cmd_type) {
        new Thread() {
            public void run() {
                String messageStr = null;
                LUCIPacket packet = null;
                messageStr = data;
                NettyAndroidClient tcpSocketSendCtrl = null;

                Log.e(TAG, "SendLUCICommand  " + MID + "  ip  " + SERVER_IP);

                if (data != null)
                    packet = new LUCIPacket(messageStr.getBytes(), (short) messageStr.getBytes().length, (short) MID, (byte) cmd_type);
                else
                    packet = new LUCIPacket(null, (short) 0, (short) MID, (byte) cmd_type);
                int server_port = LUCI_CONTROL_PORT;
                InetAddress local = null;
                try {
                    local = InetAddress.getByName(SERVER_IP);
                } catch (UnknownHostException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                int msg_length = packet.getlength();
                byte[] message = new byte[msg_length];
                packet.getPacket(message);

                Log.d("Scan_Netty", "" + SERVER_IP);

                if (SERVER_IP==null)
                    return;;

                if (!(luciSocketMap.containsKey(SERVER_IP))) {
                    // Log.d("Scan_Netty","Socket is Not Available for the IP "+SERVER_IP);
                    Log.d("SCAN_NETTY", "Socket not present for "+SERVER_IP  + " "+packet.getCommand());
            /*
                    try {

                        LibreLogger.d(this, "Socket was not present for " + SERVER_IP);
                        tcpSocketSendCtrl = new NettyAndroidClient(local, server_port);
                        if (ping(SERVER_IP))
                            luciSocketMap.put(SERVER_IP, tcpSocketSendCtrl);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
            */
                } else {
                    Log.d("Scan_Netty","Socket is  Available for the IP "+SERVER_IP);
                    try {
                        tcpSocketSendCtrl = luciSocketMap.get(SERVER_IP);
                        LibreLogger.d(this, "SERVERIPADDRESS*****" + tcpSocketSendCtrl.getRemotehost() + "commad was =" + packet.getCommand());
                        isDataSent = TcpSendData(tcpSocketSendCtrl, message);
                    }catch(Exception e){
                        e.printStackTrace();
                        LibreLogger.d(this, "SERVERIPADDRESS*****" + SERVER_IP + "Addition Exception Happend  in SendCommand ");

                    }

                }
                if (isDataSent) {
                    Log.d(TAG, "Tcp GetLUCICommand is Successfull ");
                } else {
                    isDataSent = false;
                    Log.d(TAG, "Tcp GetLUCICommand is UnSuccessfull ");
                }


                Bundle bundle = new Bundle();
                bundle.putString("data", data);
                bundle.putInt("mid", MID);
                bundle.putInt("cmd_type", cmd_type);
                bundle.putString("server_ip", SERVER_IP);
                //Message msg=LibreApplication.commandQueuHandler.obtainMessage(1);
                // LibreApplication.commandQueuHandler.sendMessage(msg);

            }

        }.start();

    }

    public void SendCommand(final ArrayList<LUCIPacket> luciPackets) {
        new Thread() {
            public void run() {
                String messageStr = null;
                LUCIPacket packet = null;

                for (LUCIPacket luciPacket : luciPackets) {
                    int server_port = LUCI_CONTROL_PORT;
                    // DatagramSocket s = null;
                    NettyAndroidClient tcpSocketSendCtrl = null;
                    InetAddress local = null;
                    try {
                        local = InetAddress.getByName(SERVER_IP);
                    } catch (UnknownHostException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                    int msg_length = luciPacket.getlength();
                    byte[] message = new byte[msg_length];
                    luciPacket.getPacket(message);

                    Log.e("SCAN_NETTY", " before sending as a packet Messagebox id" + luciPacket.getCommand());
                    Log.v("Sathi", new String(message, 0));

                    if (SERVER_IP==null)
                        return;;

                    if (!(luciSocketMap.containsKey(SERVER_IP))) {

                        Log.d("SCAN_NETTY", "Socket not present for "+SERVER_IP  + " "+luciPacket.getCommand());

                        /*       try {
                            tcpSocketSendCtrl = new NettyAndroidClient(local, server_port);
                            if (ping(SERVER_IP))
                                luciSocketMap.put(SERVER_IP, tcpSocketSendCtrl);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        */
                    } else {
                        try {
                            tcpSocketSendCtrl = luciSocketMap.get(SERVER_IP);
                            LibreLogger.d(this, "SERVERIPADDRESS*****" + tcpSocketSendCtrl.getRemotehost() + "commad was =" + luciPacket.getCommand());
                            isDataSent = TcpSendData(tcpSocketSendCtrl, message);
                        }catch(Exception e){
                            e.printStackTrace();
                            LibreLogger.d(this, "SERVERIPADDRESS*****" + SERVER_IP + "Addition Exception Happend  in SendCommand (Listof Packets)");
                        }
                    }
                    if (isDataSent) {
                        Log.d(TAG, "Tcp SendCommand is Successfull ");
                    } else {
                        isDataSent = false;
                        Log.d(TAG, "Tcp SendCommand is UnSuccessfull ");
                    }

                    try {
                        /* Sleeping 100 mili seconds to make sure the message order is correct */
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();

    }


    public static void SendCommandWithIp(final int cmd, final String data, final int cmd_type, final String ip) {
        new Thread() {
            public void run() {
                String messageStr = null;
                LUCIPacket packet = null;
                messageStr = data;
                NettyAndroidClient tcpSocketSendCtrl = null;

                if (data != null)
                    packet = new LUCIPacket(messageStr.getBytes(), (short) messageStr.getBytes().length, (short) cmd, (byte) cmd_type);
                else
                    packet = new LUCIPacket(null, (short) 0, (short) cmd, (byte) cmd_type);

                int server_port = LUCI_CONTROL_PORT;

                InetAddress local = null;
                try {
                    local = InetAddress.getByName(ip);
                } catch (UnknownHostException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                int msg_length = packet.getlength();

                byte[] message = new byte[msg_length];
                packet.getPacket(message);

                /* To handle the odd crash */
                if (ip==null)
                    return;;

                if (!(luciSocketMap.containsKey(ip))) {


                    Log.d("SCAN_NETTY", "Socket not present for "+ip  + " "+cmd);

                    /*Commenting by Praveen to avoid the
                     Socket creation while sending commands to avoid multiple socket creation
                    try {
                        tcpSocketSendCtrl = new NettyAndroidClient(local, server_port);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }*/
//                    luciSocketMap.put(SERVER_IP, tcpSocketSendCtrl);
                } else {
                    try {
                        tcpSocketSendCtrl = luciSocketMap.get(ip);

                        LibreLogger.d(this, "SERVERIPADDRESS*****" + tcpSocketSendCtrl.getRemotehost() + "commad was =" + cmd);
                        TcpSendData(tcpSocketSendCtrl, message);
                    }catch(Exception e){
                        e.printStackTrace();;
                        LibreLogger.d(this, "SERVERIPADDRESS*****" + ip + "Addition Exception Happend  in SendCommandWithIp");
                    }
                }
                // TcpSendData(tcpSocketSendCtrl, message);

            }

        }.start();

    }


    public  void sendGoogleTOSAcceptanceIfRequired( Context mContext,String isCastDevice){

              /* Google cast settings*/
        SharedPreferences sharedPreferences = mContext
                .getSharedPreferences(Constants.SHOWN_GOOGLE_TOS, Context.MODE_PRIVATE);
        String shownGoogle = sharedPreferences.getString(Constants.SHOWN_GOOGLE_TOS, null);
        if (shownGoogle!=null && isCastDevice!=null){
        new Thread(new Runnable() {
            @Override
            public void run() {
                LibreLogger.d(this, "Sending the Google TOS acceptance");
                ArrayList<LSSDPNodes> lssdpNodes = LSSDPNodeDB.getInstance().GetDB();
                for(int i=0;i<lssdpNodes.size();i++){
                    LSSDPNodes node = lssdpNodes.get(i);

                    LUCIControl control = new LUCIControl(node.getIP());
                    GoogleTOSTimeZone googleTOSTimeZone = LibreApplication.GOOGLE_TIMEZONE_MAP.get(node.getIP());

                    if(googleTOSTimeZone!=null && node.getgCastVerision()!=null ) {

                        if(googleTOSTimeZone.isShareAccepted()==false )
                            control.SendCommand(MIDCONST.GCAST_TOS_SHARE_COMMAND,"1:"+googleTOSTimeZone.getTimezone(),LSSDPCONST.LUCI_SET );

                        else
                            control.SendCommand(MIDCONST.GCAST_TOS_SHARE_COMMAND,"3:"+googleTOSTimeZone.getTimezone(),LSSDPCONST.LUCI_SET );

                    }
                }

            }
        }).start();
        }

    }


}

