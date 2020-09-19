package com.libre.alexa.models;

import java.io.Serializable;

public class ModelStoreDeviceDetails implements Serializable {

    public ModelStoreDeviceDetails(String isActive, Boolean isBlackListed, String macId,
                                   String name, String hostIcon, String hostType) {
        this.isActive = isActive;
        this.isBlackListed = isBlackListed;
        this.macId = macId;
        this.name = name;
        this.hostIcon = hostIcon;
        this.hostType = hostType;
    }

    public String isActive;
    public Boolean isBlackListed;
    public String macId;
    public String name;
    public String hostIcon;
    public String hostType;
}
