package com.libre.alexa.Scanning;/********************************************************************************************* * Copyright (C) 2014 Libre Wireless Technology * <p/> * "Junk Yard Lab" Project * <p/> * Libre Sync Android App * Author: Subhajeet Roy ***********************************************************************************************/import android.content.Context;import android.net.wifi.WifiManager;import android.os.Handler;import android.util.Log;import com.libre.alexa.LibreApplication;import com.libre.alexa.SceneObject;import com.libre.alexa.luci.LSSDPNodeDB;import com.libre.alexa.luci.LSSDPNodes;import com.libre.alexa.luci.LUCIControl;import com.libre.alexa.netty.BusProvider;import com.libre.alexa.netty.NettyAndroidClient;import com.libre.alexa.util.LibreLogger;import java.io.BufferedReader;import java.io.DataOutputStream;import java.io.IOException;import java.io.OutputStream;import java.net.DatagramPacket;import java.net.DatagramSocket;import java.net.InetAddress;import java.net.InetSocketAddress;import java.net.MulticastSocket;import java.net.NetworkInterface;import java.net.ServerSocket;import java.net.Socket;import java.net.SocketException;import java.net.UnknownHostException;import java.text.DateFormat;import java.text.SimpleDateFormat;import java.util.Date;import java.util.Random;import java.util.Scanner;public class ScanThread implements Runnable {    private static ScanThread instance = new ScanThread();    public Context getmContext() {        return mContext;    }    public void setmContext(Context mContext) {        this.mContext = mContext;    }    private Context mContext;    protected ScanThread() {        // Exists only to defeat instantiation.    }    public static ScanThread getInstance() {        if (instance == null) {            instance = new ScanThread();        }        return instance;    }    private final int MSEARCH_TIMEOUT = 30000;    private Handler mTaskHandler = new Handler();    public boolean mBackgroundMSearchStopped = false;    private Runnable mMyTaskRunnable = new Runnable() {        @Override        public void run() {            LibreLogger.d(this, "My task is Sending 1 Minute Once M-Search");      /* do what you need to do */            if (mBackgroundMSearchStopped)                return;            while (!UpdateNodes()) {                mNetIf = Utils.getActiveNetworkInterface();                LibreLogger.d(this, "hang - netif is " + mNetIf);            }      /* and here comes the "trick" */            mTaskHandler.postDelayed(this, MSEARCH_TIMEOUT);        }    };    public ScanningHandler m_ScanningHandler = ScanningHandler.getInstance();    public LSSDPNodeDB lssdpDB = LSSDPNodeDB.getInstance();    private static final String TAG = "ScanThread";    /* public static final String LSSDP_MULTICAST_ADDRESS = "239.255.255.250"; Commenting For HnT Discovery Ip Change*/    public static final String LSSDP_MULTICAST_ADDRESS = "239.255.255.250";    public static final int LSSDP_PORT = 1800;    public static final String ST = "ST";    public static final String LOCATION = "LOCATION";    public static final String NT = "NT";    public static final String NTS = "NTS";    public static final String DEFAULT_ZONEID = "239.255.255.251:3000";    /* Definitions of start line */    public static final String SL_NOTIFY = "NOTIFY * HTTP/1.1";    public static final String SL_MSEARCH = "M-SEARCH * HTTP/1.1";    public static final String SL_OK = "HTTP/1.1 200 OK";    /* Definitions of notification sub type */    public static final String NTS_ALIVE = "ssdp:alive";    public static final String NTS_BYEBYE = "ssdp:byebye";    public static final String NTS_UPDATE = "ssdp:update";    public static final String NTS_BYEBYE_HEADER = "NTS";    public static final String MRA_APMS_MODE = "APMS";    public DatagramSocket mDatagramSocketForSendingMSearch;    public MulticastSocket mAliveNotifyListenerSocket;    private DatagramSocket mUnicastSocket;    private NetworkInterface mNetIf = Utils.getActiveNetworkInterface();    private boolean isSocketCreated = false;    public boolean mRunning = true;    Thread MulticastRx;    Thread UnicastRx;    private boolean __DEBUG__ = false;    boolean shutdown = false;    private Handler m_handler;    NotifyThread notifyThread = new NotifyThread();    AliveNotifyThread mAliveNotifyListeningThread = new AliveNotifyThread();    public NettyServerClass nettyServer = new NettyServerClass();    Socket tcpUnicastsocket;    ServerSocket serverSocket;    Thread lookforMSearch;    BufferedReader in;    /* This is the Random port Number Value For  Sending & Receiving M-Search and Response    * Default Value = -1 */    int mPortToSendMSearchAndGetResponse = -1;    public boolean CreateSocketsForAliveNotifyListener() throws SocketException {        mNetIf = Utils.getActiveNetworkInterface();        if (mNetIf == null) {            Log.d(TAG, "Network interface is false");            return false;        }        mRunning = true;        //for sending LSSDP M-Search        try {            LibreLogger.d(this, "Creating Multicast Socket For Receiving Alive Notify on PORT Number" + LSSDP_PORT);            String name = mNetIf.getName();            Utils mUtil = new Utils();            mAliveNotifyListenerSocket = new MulticastSocket(LSSDP_PORT);            mAliveNotifyListenerSocket.setLoopbackMode(false);            mAliveNotifyListenerSocket.setTrafficClass(0x10);            mAliveNotifyListenerSocket.setReuseAddress(true);            mAliveNotifyListenerSocket.setSoTimeout(0);            mAliveNotifyListenerSocket.setNetworkInterface(mNetIf);            mAliveNotifyListenerSocket.joinGroup                    (InetAddress.getByName(LSSDP_MULTICAST_ADDRESS));            LibreLogger.d(this, "Creation Of Alive Socket " + "Successfully");        } catch (IOException e) {            LibreLogger.d(this, "CreationOf Alive Socket " + "failed ");            e.printStackTrace();            mRunning = false;            return false;        } catch (Exception e) {            e.printStackTrace();            Log.d(TAG, "Exception");            mRunning = false;        }        return true;    }    public int isLSSDPPortAvailableAndReturnAvailablePort() {        ServerSocket socket = null;        boolean portTaken = false;        int START = LSSDP_PORT;        int END = LSSDP_PORT + 1000;        Random random = new Random();        int max = 65535;        int min = 10000;        int mRandomPort = random.nextInt(max - min + 1) + min;        while (socket == null) {            try {                socket = new ServerSocket(mRandomPort++);                LibreLogger.d(this, "Socket Binded To Port Number" + socket.getLocalPort());            } catch (IOException e) {                e.printStackTrace();                LibreLogger.d(this, "Socket Failed Binded To Port Number" + (mRandomPort - 1));            }        }        if (socket != null) {            try {                socket.close();            } catch (IOException e) {                e.printStackTrace();            }        }        return socket.getLocalPort();    }    public boolean CreateSockets() throws SocketException {        boolean mTcpStartedListening = false;         mNetIf = Utils.getActiveNetworkInterface();        if (mNetIf == null) {            Log.d(TAG, "Network interface is false");            return false;        }        //for sending LSSDP M-Search        try {            LibreLogger.d(this, "Creating Multicast Socket For Sending M-Search" );            String name = mNetIf.getName();            Utils mUtil = new Utils();            mPortToSendMSearchAndGetResponse = isLSSDPPortAvailableAndReturnAvailablePort();            mDatagramSocketForSendingMSearch = new DatagramSocket(new InetSocketAddress(mUtil.getIPAddress(true),mPortToSendMSearchAndGetResponse));            mDatagramSocketForSendingMSearch.setTrafficClass(0x10);            mDatagramSocketForSendingMSearch.setReuseAddress(true);            /*mDatagramSocketForSendingMSearch.joinGroup(new InetSocketAddrfess                            (LSSDP_MULTICAST_ADDRESS, LSSDP_PORT),                    NetworkInterface.getByName(name));*/            LibreLogger.d(this, "Creation of Socket Success On the Random Port Number : " + mPortToSendMSearchAndGetResponse);	        /*  KK ., We have to Discover the Device Whenever a TCP Server Creation Got Failed Too , So We have to Start the UDP Unicast Thread before it , So I have made this Change*/            try {                if (mDatagramSocketForSendingMSearch != null && !notifyThread.isAlive()) {                    LibreLogger.d(this,"Notify Thread Started Listening on the Port Number  " + mPortToSendMSearchAndGetResponse);                    notifyThread.start();                    LibreLogger.d(this, "Notify Thread State" + notifyThread.getState());                }            } catch (Exception e) {                e.printStackTrace();            }            mTcpStartedListening = nettyServer.startServer(mPortToSendMSearchAndGetResponse);            LibreLogger.d(this, "TCP Starte Listening Success/Fail  " + mTcpStartedListening);            LibreLogger.d(this, "Task is Creating For Sending M-Search " + MSEARCH_TIMEOUT);            mTaskHandler.postDelayed(mMyTaskRunnable, MSEARCH_TIMEOUT);        } catch (Exception e) {            e.printStackTrace();            mDatagramSocketForSendingMSearch = null;            LibreLogger.d(this, "Creation of Sockets" + "Failure");            return false;        }        return true;    }    public synchronized void close() {        try {            instance = null;            LibreApplication.mLuciThreadInitiated=false;            mRunning = false;            mBackgroundMSearchStopped = true;            if (mDatagramSocketForSendingMSearch != null) {                if (!mDatagramSocketForSendingMSearch.isClosed())                    mDatagramSocketForSendingMSearch.close();            }            if (serverSocket != null) {                if (!serverSocket.isClosed())                    serverSocket.close();            }            if (mAliveNotifyListenerSocket != null) {                if (!mAliveNotifyListenerSocket.isClosed()) {                    mAliveNotifyListenerSocket.close();                }            }        } catch (IOException e) {            e.printStackTrace();        }    }    private boolean sendNotifyResponse(InetAddress mInetaddressOfNotifyReceived, int mPortOfNotifyReceived) {        /*Payloadd Syntax        * Location:127.0.0.0:8080*/        //String mNotifyResponsePayload = "Location:"+mInetaddressOfNotifyReceived.getHostAddress()+":"+mPortOfNotifyReceived;        String mNotifyResponsePayload = "Location:" + mInetaddressOfNotifyReceived.getHostAddress() + ":" + mPortToSendMSearchAndGetResponse;        try {            LibreLogger.d(this, "sendNotifyResponse \t " + "Connecting To " + mInetaddressOfNotifyReceived + "SendingData  " + mNotifyResponsePayload);            Socket mSendNotifyResponseSocket = new Socket(mInetaddressOfNotifyReceived, 58431);            LibreLogger.d(this, "sendNotifyResponse \t " + "Just Connected To" + mSendNotifyResponseSocket.getRemoteSocketAddress());            OutputStream outToServer = mSendNotifyResponseSocket.getOutputStream();            DataOutputStream out = new DataOutputStream(outToServer);            out.writeUTF(mNotifyResponsePayload);            mSendNotifyResponseSocket.close();            return true;        } catch (IOException e) {            e.printStackTrace();            LibreLogger.d(this, "sendNotifyResponse \t " + "IO Exception Happend");            return false;        } catch (Exception e) {            e.printStackTrace();            LibreLogger.d(this, "sendNotifyResponse \t " + "Exception Happend");            return false;        }    }    public void RemovingTheCorrespondingSceneMapFromCentralDB(LSSDPNodes mNode) {        String mIpAddress = mNode.getIP();        LSSDPNodeDB mNodeDB = LSSDPNodeDB.getInstance();        try {            if (!mNode.getmDeviceCap().getmSource().isAlexaAvsSource()					&& ScanningHandler.getInstance().isIpAvailableInCentralSceneRepo(mIpAddress)) {                boolean status = ScanningHandler.getInstance().removeSceneMapFromCentralRepo(mIpAddress);                LibreLogger.d(this, "Removing the Corresponding SceneMap Fro CentralDB status is " + status + " For the ip is " + mIpAddress);            }        } catch (Exception e) {            LibreLogger.d(this, "Removing the Corresponding SceneMap Fro CentralDB status" + "Removal Exception ");        }        mNodeDB.clearNode(mIpAddress);    }    private void CreateOrUpdateMyNewDevice(final LSSDPNodes mInputNode, final String messageType) {            /* It will not Wait for Socket to be created */        if (LUCIControl.luciSocketMap.containsKey(mInputNode.getIP())) {            NettyAndroidClient mExistingAndroidClient = LUCIControl.luciSocketMap.get(mInputNode.getIP());                /*Socket is Already Exists*/            if (mInputNode.getFirstNotification() != null && mInputNode.getFirstNotification().equals("1")                    && (System.currentTimeMillis() - mExistingAndroidClient.getCreationTime()) > 5000) {                Date date = new Date(mExistingAndroidClient.getCreationTime());                DateFormat formatter = new SimpleDateFormat("HH:mm:ss:SSS");                String dateFormatted = formatter.format(date);                date = new Date(mExistingAndroidClient.getLastNotifiedTime());                formatter = new SimpleDateFormat("HH:mm:ss:SSS");                String dateFormattedLastNotified = formatter.format(date);                LibreLogger.d(this,"In Create Or Update My New Device for the FirstNotification is 1 " +                        "and CreationTimeMills's Difference is 5s for Ipaddress --> " + mInputNode.getIP() +                        "And Name of the Device is " + mInputNode.getFriendlyname() +                        "In the Date "+ dateFormatted +                        " In the Last Notified Time is "+ dateFormattedLastNotified);                   /* LibreLogger.d(this, "In Create Or Update My New Device for the FirstNotification is 1 and CreationTimeMills's Difference is 5s for Ipaddress --> " + mInputNode.getIP() +                            "And Name of the Device is " + mInputNode.getFriendlyname());*/                /**closing existing NettyAndroidClient */                LUCIControl.luciSocketMap.get(mInputNode.getIP()).closeSocket();                LUCIControl.luciSocketMap.remove(mInputNode.getIP());                    /* I will remove previous Socket Information */                RemovingTheCorrespondingSceneMapFromCentralDB(mInputNode);                NettyAndroidClient nettyAndroidClient = null;                try {                    nettyAndroidClient = new NettyAndroidClient(mInputNode.getNodeAddress(), 7777);                    LUCIControl.luciSocketMap.put(mInputNode.getIP(), nettyAndroidClient);                    new LUCIControl(mInputNode.getIP()).sendAsynchronousCommandSpecificPlaces();                    LSSDPNodeDB lssdpNodeDB = LSSDPNodeDB.getInstance();                    LSSDPNodes theNodeBasedOnTheIpAddress = lssdpNodeDB.getTheNodeBasedOnTheIpAddress(mInputNode.getIP());                    if (theNodeBasedOnTheIpAddress != null && theNodeBasedOnTheIpAddress.getgCastVerision() != null) {                        // sleep                        try {                            Thread.sleep(250);                        } catch (InterruptedException e) {                            e.printStackTrace();                        }                    }                } catch (Exception e) {                    e.printStackTrace();                }            } else {                LibreLogger.d(this, "In Create Or Update My New Device for the FirstNotification is 0 or CreationTimeMills's Difference is less than 5s for Ipaddress --> " + mInputNode.getIP() +                        "And Name of the Device is " + mInputNode.getFriendlyname());                NettyAndroidClient nettyAndroidClient = LUCIControl.luciSocketMap.get(mInputNode.getIP());                if (!messageType.equals(SL_NOTIFY)) {                    nettyAndroidClient.setLastNotifiedTime(System.currentTimeMillis());                }                LUCIControl.luciSocketMap.put(mInputNode.getIP(), nettyAndroidClient);            }        } else {            LibreLogger.d(this, "In Create Or Update My New Device for the LuciSocket is Not availabe for the ipddress --> " + mInputNode.getIP()                    +                    "And Name of the Device is " + mInputNode.getFriendlyname());                    /* Socket is Not Avaiable*/            LUCIControl.luciSocketMap.put(mInputNode.getIP(), NettyAndroidClient.getDummyInstance());            LibreLogger.d(this, "Socket Creating for Ip " + mInputNode.getIP() + " as a DummyInstance ");            try {                NettyAndroidClient tcpSocketSendCtr = new NettyAndroidClient(mInputNode.getNodeAddress(), 7777);                LibreLogger.d(this, "Socket Created for Ip " + mInputNode.getIP() +                        " Printing From NettyAndroidClient Socket  " + tcpSocketSendCtr.getRemotehost());                //if(tcpSocketSendCtr.isSocketCreated())                {                    if (!messageType.equals(SL_NOTIFY)) {                        tcpSocketSendCtr.setLastNotifiedTime(System.currentTimeMillis());                    }                    LUCIControl.luciSocketMap.put(mInputNode.getIP(), tcpSocketSendCtr);                    new LUCIControl(mInputNode.getIP()).sendAsynchronousCommandSpecificPlaces();                    LSSDPNodeDB lssdpNodeDB = LSSDPNodeDB.getInstance();                    LSSDPNodes theNodeBasedOnTheIpAddress = lssdpNodeDB.getTheNodeBasedOnTheIpAddress(mInputNode.getIP());                    if (theNodeBasedOnTheIpAddress != null && theNodeBasedOnTheIpAddress.getgCastVerision() != null) {                        // sleep                        try {                            Thread.sleep(250);                        } catch (InterruptedException e) {                            e.printStackTrace();                        }                    }                }            } catch (Exception e1) {                e1.printStackTrace();                LibreLogger.d(this, "Socket creation Failed in EchoServerHandler " + mInputNode.getIP());                LUCIControl.luciSocketMap.remove(mInputNode.getIP());                Log.e("Scan_Netty", "Socket creation not required in  EchoServerHandler" + mInputNode.getIP());            }        }                /* Checking Dupicates Irrespective of Socket is Available or not                * */        if ((mInputNode != null) && (mInputNode.getIP() != null) &&                (mInputNode.getDeviceState() != null) && (!m_ScanningHandler.findDupicateNode(mInputNode))) {            LibreLogger.d(this, "New Node is Found For the ipAddress " + mInputNode.getIP());            if(!mInputNode.getUSN().isEmpty()) {                BusProvider.getInstance().post(mInputNode);                m_ScanningHandler.lssdpNodeDB.AddtoDB(mInputNode);                ScanningHandler mScanHandler = ScanningHandler.getInstance();                SceneObject sceneObjec = new SceneObject(" ", "", 0, mInputNode.getIP());                if (!mScanHandler.isIpAvailableInCentralSceneRepo(mInputNode.getIP())) {                    mScanHandler.putSceneObjectToCentralRepo(mInputNode.getIP(), sceneObjec);                }            }else{                LibreLogger.d(this, "USN is Empty " + mInputNode.getIP());            }        }    }    private void LookForNotify() {        DatagramPacket dp = null;        while (mRunning) {            try {                dp = receiveMulticast();                InetAddress addr = dp.getAddress();                int mPort = dp.getPort();                String startLine = parseStartLine(dp);                LibreLogger.d(this, "ReceiveMulticast" + "Received Notify: From " + dp.getAddress().getHostAddress() + " with StartLine " + startLine);                try {                    LibreLogger.d(this, "TcpSocketMap Contains" + LUCIControl.luciSocketMap.keySet().toString());                } catch (Exception e) {                }                /* For TCP SocketRemoval */                try {                    if (startLine.equals(SL_NOTIFY) || startLine.equals(SL_OK)) {                        String mSearchMessageReceivedFromResponse = new String(dp.getData());                        LibreLogger.d(this, "ReceiveMulticast" + "Received Notify: From " + dp.getAddress().getHostAddress() + " with StartLine " + mSearchMessageReceivedFromResponse);                        LSSDPNodes mNodeToBeUpdateOrAdded = m_ScanningHandler.getLSSDPNodeFromMessage(dp.getSocketAddress(), mSearchMessageReceivedFromResponse);                        if (mNodeToBeUpdateOrAdded != null) {                            CreateOrUpdateMyNewDevice(mNodeToBeUpdateOrAdded, startLine);                        }                        continue;                    }                } catch (Exception e) {                }            } catch (Exception e) {            }        }    }    public void run() {        try {            /* Creation of Socket is ALready Done when we are creating the Class and Starting the Thread For Listening on 1800*/            if (mAliveNotifyListenerSocket != null && !mAliveNotifyListeningThread.isAlive() &&                    mAliveNotifyListeningThread.getState() == Thread.State.NEW) {                LibreLogger.d(this, "mAliveNotifyListener Socket Starting on the port Number "+ "1800 ") ;                try {                    mAliveNotifyListeningThread.start();                }catch(IllegalThreadStateException e1){                    e1.printStackTrace();                    LibreLogger.d(this, "mAliveNotifyListener Socket Starting on the port Number "+ "1800 Got Exception ") ;                }            }            /* Creation of Socket , And Start Listening of UDP Unicast & TCP Unicast on the RandomPort Number*/            if (!CreateSockets()) {                return;            }        } catch (SocketException e) {            LibreLogger.d(this, "Create Sockets " + " Got Failed !! ");            e.printStackTrace();            return;        }catch(Exception e){            LibreLogger.d(this, "Socket Creation Failed " + " No Discovery ");            e.printStackTrace();            return;        }        LibreLogger.d(this, "Scan thread Started  " + " Successfully !! ");    }    public synchronized void shutdown() {        Log.v(TAG, "Scan Thread Shutdown");        mRunning = false;    }    DatagramPacket receiveMulticastForPortNumber1800() throws IOException {        byte[] buf = new byte[1024];        DatagramPacket dp = new DatagramPacket(buf, buf.length);        LibreLogger.d(this, "ReceiveMulticast" + "Trying To receieve Notify");        /*if(mFirmwareUpgradeListenerSocket==null){            LibreLogger.d(this,"ReceiveMulticast" + "mMulticastSocketAlive Notify is Null");            CreateSocketsForFirmwareUpgradeListener();        }*/        //  mDatagramSocketForSendingMSearch.receive(dp);        mAliveNotifyListenerSocket.receive(dp);        LibreLogger.d(this, "ReceiveMulticast" + "Received Notify: From " + dp.getAddress() + "Of Length" + dp.getLength());        return dp;    }    DatagramPacket receiveMulticast() throws IOException {        byte[] buf = new byte[1024];        DatagramPacket dp = new DatagramPacket(buf, buf.length);        LibreLogger.d(this, "ReceiveMulticast" + "Trying To receieve Notify");        /*if(mFirmwareUpgradeListenerSocket==null){            LibreLogger.d(this,"ReceiveMulticast" + "mMulticastSocketAlive Notify is Null");            CreateSocketsForFirmwareUpgradeListener();        }*/        //  mDatagramSocketForSendingMSearch.receive(dp);        mDatagramSocketForSendingMSearch.receive(dp);        LibreLogger.d(this, "ReceiveMulticast" + "Received Notify: From " + dp.getAddress() + "Of Length" + dp.getLength());        return dp;    }    private String parseHeaderValue(String content, String headerName) {        Scanner s = new Scanner(content);        s.nextLine(); // Skip the start line        while (s.hasNextLine()) {            String line = s.nextLine();            if (line.equals(""))                return null;            int index = line.indexOf(':');            if (index == -1)                return null;            String header = line.substring(0, index);            if (headerName.equalsIgnoreCase(header.trim())) {                return line.substring(index + 1).trim();            }        }        return null;    }    private String parseHeaderValue(DatagramPacket dp, String headerName) {        return parseHeaderValue(new String(dp.getData()), headerName);    }    private String parseStartLine(String content) {        Scanner s = new Scanner(content);        return s.nextLine();    }    private String parseStartLine(DatagramPacket dp) {        return parseStartLine(new String(dp.getData()));    }    public boolean clearNodes() {        lssdpDB.clearDB();        //m_ScanningHandler.clearSceneObjectsFromCentralRepo();        //LUCIControl.luciSocketMap.clear();        lssdpDB.GetDB().clear();        //mRunning = false;        return __DEBUG__;    }    public synchronized boolean UpdateNodes() {//DB.GetDB().clear();        LibreLogger.d(this, "hang - trying to send Msearch");        NetworkInterface mNetI = Utils.getActiveNetworkInterface();        if (mNetI == null) {            LibreLogger.d(this, "hang - trying to send Msearch but mNetIf is null");            return false;        }        new Thread() {            public void run() {                String MSearchPayload = "M-SEARCH * HTTP/1.1\r\n" +                        "MX: 10\r\n" +                        "ST: urn:schemas-upnp-org:device:DDMSServer:1\r\n" +                        "HOST: 239.255.255.250:1800\r\n" +                        "MAN: \"ssdp:discover\"\r\n" +                        "\r\n";                LibreLogger.d(this, "Sending M-Search"+MSearchPayload);                DatagramPacket MSearch = null;                try {                    MSearch = new DatagramPacket(MSearchPayload.getBytes(),                            MSearchPayload.length(), InetAddress.getByName(LSSDP_MULTICAST_ADDRESS), LSSDP_PORT);                    LibreLogger.d(this,"msearch"+MSearch);                } catch (UnknownHostException e) {                    // TODO Auto-generated catch block                    e.printStackTrace();                    LibreLogger.d(this, "creating M-Search " + "UnSuccessfull with exception " + e);                }                if (mDatagramSocketForSendingMSearch == null) {                    try {                        WifiManager wifi = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);                        wifi.setWifiEnabled(true);                        LibreLogger.d(this, "hang - enabling wifi success");                        recreateSocket();                    } catch (Exception e) {                        LibreLogger.d(this, "hang - enabling wifi failed with exception " + e + " and mContext value is :" + mContext);                    }                }                try {                    mDatagramSocketForSendingMSearch.send(MSearch);                    LibreLogger.d(this, "hang - M-Search value " + MSearch);                    LibreLogger.d(this, "hang - Sending M-Search " + "Successfull");                } catch (Exception e) {                    // TODO Auto-generated catch block                    LibreLogger.d(this, "hang - Sending M-Search " + "UnSuccessfull with exception " + e);                    e.printStackTrace();                    recreateSocket();                }            }        }.start();        //mRunning = true;        return true;    }    public void recreateSocket() {        try {//            Activity activity = (Activity) mContext;//            LibreApplication application = (LibreApplication) activity.getApplication();            LibreLogger.d(this,"hang - trying to create datagram socket again");            while(!CreateSockets()){                LibreLogger.d(this,"hang - creating datagram socket failed. trying again");                return;            }            LibreLogger.d(this,"hang - Recreating socket successfull, sending MSearch");            UpdateNodes();//            application.initiateServices();        } catch (Exception e) {            LibreLogger.d(this, "hang - enabling wifi failed" + " recreateSocket UnSuccessfull with exception " + e);        }    }    public boolean isSocketAlive() {        if (mDatagramSocketForSendingMSearch != null                && mDatagramSocketForSendingMSearch.isClosed()) {            return false;        }        return true;    }    public class            AliveNotifyThread extends Thread {        public AliveNotifyThread() {            try {                /* Not Needed Because of We are using the Same Socket */                boolean variable = CreateSocketsForAliveNotifyListener();                //    InetSocketAddress groupAddress = new InetSocketAddress(LSSDP_MULTICAST_ADDRESS, LSSDP_PORT);                // new MulticastServer(groupAddress).run();                //  Log.e(TAG,"Alive Notify Socket Createed" + variable);            } catch (Exception e) {                e.printStackTrace();            }        }        @Override        public void interrupt() {            Log.e(TAG, "Notify Thread Interrrupted");            super.interrupt();        }        @Override        public void run() {            LookForAliveNotify();        }    }    public void LookForAliveNotify() {        DatagramPacket dp = null;        while (mRunning) {            try {                dp = receiveMulticastForPortNumber1800();                InetAddress addr = dp.getAddress();                int mPort = dp.getPort();                String startLine = parseStartLine(dp);                LibreLogger.d(this, "ReceiveMulticast" + "Received Notify: From " + dp.getAddress().getHostAddress() + " with StartLine " + startLine);                try {                    LibreLogger.d(this, "TcpSocketMap Contains" + LUCIControl.luciSocketMap.keySet().toString());                } catch (Exception e) {                }                /* For TCP SocketRemoval */                try {                        /*if (startLine.equals(SL_FWUPDATE)) {                            String st1 = parseHeaderValue(dp, "HOST");                            String st2 = parseHeaderValue(dp, "PORT");                            String st3 = parseHeaderValue(dp, "FNAME");                            String st4 = parseHeaderValue(dp, "SENDER");                            String st5 = parseHeaderValue(dp, "Model");                            LibreFWUpdatDevice updatDevice = new LibreFWUpdatDevice();                            updatDevice.setIp(st1);                            updatDevice.setPort(st2);                            updatDevice.setFriendlyName(st3);                            updatDevice.setSenderIpAddress(st4);                            updatDevice.setModelName(st5);                            BusProvider.getInstance().post(updatDevice);                        }*/                    if (startLine.equals(SL_NOTIFY) || startLine.equals(SL_OK)) {                        String mSearchMessageReceivedFromResponse = new String(dp.getData());                        LibreLogger.d(this, "ReceiveMulticast" + "Received Notify: From " + dp.getAddress().getHostAddress() + " with StartLine " + mSearchMessageReceivedFromResponse);                        LSSDPNodes mNodeToBeUpdateOrAdded = m_ScanningHandler.getLSSDPNodeFromMessage(dp.getSocketAddress(), mSearchMessageReceivedFromResponse);                        if (mNodeToBeUpdateOrAdded != null) {                            CreateOrUpdateMyNewDevice(mNodeToBeUpdateOrAdded, startLine);                        }                        continue;                    }                     /*   if (startLine.equals(SL_NOTIFY) || startLine.equals(SL_OK)) {                            String mSearchMessageReceivedFromResponse = new String(dp.getData());                            LibreLogger.d(this, "ReceiveMulticast" + "Received Notify: From " + dp.getAddress().getHostAddress() + " with StartLine " + mSearchMessageReceivedFromResponse);                            LSSDPNodes mNodeToBeUpdateOrAdded = m_ScanningHandler.getLSSDPNodeFromMessage(dp.getSocketAddress(),mSearchMessageReceivedFromResponse);                            if(mNodeToBeUpdateOrAdded!=null) {                                CreateOrUpdateMyNewDevice(mNodeToBeUpdateOrAdded);                            }                            continue;                        }*/                } catch (Exception e) {                }            } catch (Exception e) {            }        }    }    public class            NotifyThread extends Thread {        public NotifyThread() {            try {                /* Not Needed Because of We are using the Same Socket */            } catch (Exception e) {                e.printStackTrace();            }        }        @Override        public void interrupt() {            Log.e(TAG, "Notify Thread Interrrupted");            super.interrupt();        }        @Override        public void run() {            LookForNotify();        }    }}