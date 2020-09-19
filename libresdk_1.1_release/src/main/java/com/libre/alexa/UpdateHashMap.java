package com.libre.alexa;

import android.util.Log;

import com.libre.alexa.constants.LSSDPCONST;
import com.libre.alexa.constants.MIDCONST;
import com.libre.alexa.luci.LSSDPNodeDB;
import com.libre.alexa.luci.LSSDPNodes;
import com.libre.alexa.luci.LUCIControl;
import com.libre.alexa.util.LibreLogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

public class UpdateHashMap {

    public static LinkedHashMap<String, LSSDPNodesUpdate> updateHashMap = new LinkedHashMap<>();
    private String ipAddress;
    private LSSDPNodesUpdate updateNode;
    private ArrayList<LSSDPNodesUpdate> updateNodeList;
    static UpdateHashMap updateHashMapInstance;

    private UpdateHashMap() {

    }

    public static UpdateHashMap getInstance() {
        if (updateHashMapInstance == null) {
            updateHashMapInstance = new UpdateHashMap();
        }
        return updateHashMapInstance;
    }


    public void readFwUrlForAbsentees() {

        ArrayList<LSSDPNodes> nodeList = LSSDPNodeDB.getInstance().GetDB();

        for(LSSDPNodes node: nodeList){
            if(updateHashMap.get(node.getIP())== null){
                askFirmwareUrl(node.getIP());
            }
        }
    }

    private void askFirmwareUrl(String ip) {
        new LUCIControl(ip).SendCommand(MIDCONST.MID_ENV_READ,"READ_fwdownload_xml", LSSDPCONST.LUCI_SET);
    }


    public void CreateUpdatedHashMap(String ipAddress, LSSDPNodesUpdate updateNode) {
        updateHashMap.put(ipAddress, updateNode);

    }

    public boolean isDeviceAvailableForUpdate() {
        for (String key : updateHashMap.keySet()) {
            if (updateHashMap.get(key).isFirmwareUpdateNeeded()) {
                return true;
            }
        }
        return false;
    }

    public boolean checkIfNodeAlreadyPresentinList(String mNodeIp) {
        return updateHashMap.containsKey(mNodeIp);
    }

    public void updateFWStatusForAlreadyExistingNode(String mNodeIp) {

//        updateHashMap.get(mNodeIp).setFirmwareStatus(2);


    }

    public Integer hashMapSize() {
        return updateHashMap.size();
    }

    public LSSDPNodesUpdate getUpdateNode(String ipAddress) {
        return updateHashMap.get(ipAddress);
    }

    public LSSDPNodesUpdate getEachUpdatedNode(Integer pos) {

        updateNodeList = new ArrayList<LSSDPNodesUpdate>();
        updateNodeList.clear();
        for (HashMap.Entry entry : updateHashMap.entrySet()) {
            Log.d("FirmwareUpdate", "hashmap key: " + entry.getKey() + " hashmap value: " + entry.getValue());
        }

        for (LSSDPNodesUpdate listData : updateHashMap.values()) {
            updateNodeList.add(listData);
            LibreLogger.d(this,"suma in firmware update getting each node"+listData);
        }

        return updateNodeList.get(pos);
    }

}
