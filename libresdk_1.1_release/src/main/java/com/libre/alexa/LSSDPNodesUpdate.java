package com.libre.alexa;

public class LSSDPNodesUpdate {

    private String friendlyname;
    private String fwVersion = "";
    private String nodeaddress;
    private Integer fwState;
    private String USN;
    private boolean isFirmwareUpdateNeeded;
    private int firmwareStatus=2; //0-update 1-updating 2-up-to date

    public int getFirmwareStatus() {
        return firmwareStatus;
    }

    public void setFirmwareStatus(int firmwareStatus) {
        this.firmwareStatus = firmwareStatus;
    }

    public boolean isFirmwareUpdateNeeded() {
        return isFirmwareUpdateNeeded;
    }

    public void setFirmwareUpdateNeeded(boolean firmwareUpdateNeeded) {
        isFirmwareUpdateNeeded = firmwareUpdateNeeded;
    }

    public String getFriendlyname() {
        return friendlyname;
    }

    public void setFriendlyname(String friendlyname) {
        this.friendlyname = friendlyname;
    }

    public String getFwVersion() {
        return fwVersion;
    }

    public void setFwVersion(String fwVersion) {
        this.fwVersion = fwVersion;
    }

    public String getNodeaddress() {
        return nodeaddress;
    }

    public void setNodeaddress(String nodeaddress) {
        this.nodeaddress = nodeaddress;
    }

    public Integer getFwState() {
        return fwState;
    }

    public void setFwState(Integer fwState) {
        this.fwState = fwState;
    }

    public String getUSN() {
        return USN;
    }

    public void setUSN(String USN) {
        this.USN = USN;
    }
}

