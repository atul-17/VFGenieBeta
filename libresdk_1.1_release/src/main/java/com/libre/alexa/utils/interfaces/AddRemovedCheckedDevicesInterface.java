package com.libre.alexa.utils.interfaces;


import com.libre.alexa.models.ModelStoreDeviceDetails;

public interface AddRemovedCheckedDevicesInterface {

    void addCheckedDevice(ModelStoreDeviceDetails modelStoreDeviceDetails);
    void removeUnCheckedDevice(String macId);

}
