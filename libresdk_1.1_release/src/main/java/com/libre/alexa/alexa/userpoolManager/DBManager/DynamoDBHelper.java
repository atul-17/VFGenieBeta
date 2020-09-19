package com.libre.alexa.alexa.userpoolManager.DBManager;

import android.os.Bundle;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.libre.alexa.alexa.userpoolManager.AlexaListeners.DynamoDBAdditionListener;
import com.libre.alexa.alexa.userpoolManager.AlexaListeners.DynamoDBDeletionListener;
import com.libre.alexa.alexa.userpoolManager.AlexaListeners.DynamoDBUpdationListener;
import com.libre.alexa.alexa.userpoolManager.AlexaUtils.AlexaConstants;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.*;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.*;
import com.amazonaws.services.dynamodbv2.model.*;
import com.libre.alexa.util.LibreLogger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

/**
 * Created by bhargav on 19/6/17.
 */
public class DynamoDBHelper {

    static DynamoDBHelper dynamoDBHelper;
    BasicAWSCredentials credentials;
    DynamoDBMapper mapper;

    private DynamoDBHelper(){
        credentials = new BasicAWSCredentials("AKIAI5GKKKKY7RBJ5IRA", "jwe1wwjfFttBpfxFOX+6hHTOUEPZrB4xosljbl6t");
        AmazonDynamoDBClient ddbClient = new AmazonDynamoDBClient(credentials);
        Region usWest2 = Region.getRegion(Regions.US_WEST_2);
        ddbClient.setRegion(usWest2);
        mapper = new DynamoDBMapper(ddbClient);
    }
    public static DynamoDBHelper getInstance(){
        if (dynamoDBHelper == null){
            dynamoDBHelper = new DynamoDBHelper();
        }
        return dynamoDBHelper;
    }

    public void deleteItem(final Bundle deleteBundle, final DynamoDBDeletionListener dynamoDBDeletionListener){
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (isMaxHitReached(deleteBundle)){
                    dynamoDBDeletionListener.itemDeletionFailed(deleteBundle);
                    return;
                }
                Endpoints endpoints = new Endpoints();
                String username = deleteBundle.getString(AlexaConstants.BUNDLE_ALEXA_USERNAME);
                endpoints.setUsername(username);
                String usn = deleteBundle.getString(AlexaConstants.BUNDLE_ENDPOINT_ID);
                String friendlyName = deleteBundle.getString(AlexaConstants.BUNDLE_FRIENDLY_NAME);
                String description = deleteBundle.getString(AlexaConstants.BUNDLE_DESCRIPTION);
                HashSet<String> endpointDetailsArray = null;
                try {
                    endpoints = getItem(username);
                    endpointDetailsArray = getRemovedEndpointDetailsArray(usn,friendlyName,endpoints);
                    endpoints.setEndpointDetailsArray(endpointDetailsArray);
                  //  endpoints.setRefresh_token(AlexaConstants.ALEXA_REFRESH_TOKEN);
                    /*if (endpointDetailsArray == null || endpointDetailsArray.size()<=0){
                        mapper.delete(endpoints);
                    }else*/ {
                        mapper.save(endpoints);
                    }
                    dynamoDBDeletionListener.itemDeleted(deleteBundle);
                } catch (JSONException e) {
                    dynamoDBDeletionListener.itemDeletionFailed(deleteBundle);
                    e.printStackTrace();
                }catch (ConditionalCheckFailedException e) {
                    int hitCount = deleteBundle.getInt(AlexaConstants.BUNDLE_HIT_COUNT);
                    hitCount++;
                    deleteBundle.putInt(AlexaConstants.BUNDLE_HIT_COUNT,hitCount);
                    // Another process updated this item after we loaded it, so try again with the newest data
                    deleteItem(deleteBundle,dynamoDBDeletionListener);
                }


            }
        }).start();
    }
    public void updateItem(final Bundle updateBundle, final DynamoDBAdditionListener dynamoDBAccessListener){
        putItem(updateBundle,dynamoDBAccessListener);
    }
    public boolean isMaxHitReached(Bundle bundle){
        if (bundle!=null){
            int hitCount = bundle.getInt(AlexaConstants.BUNDLE_HIT_COUNT);
            LibreLogger.d(this,"dynamo db hit count is "+hitCount);
            if (hitCount > AlexaConstants.HIT_COUNT_LIMIT){
                return true;
            }
        }else {
            return true;
        }
        return false;
    }
    public void putItem(final Bundle putBundle, final DynamoDBAdditionListener dynamoDBAccessListener){
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (isMaxHitReached(putBundle)){
                    dynamoDBAccessListener.onItemAdditionFailed(putBundle);
                    return;
                }
                String username = putBundle.getString(AlexaConstants.BUNDLE_ALEXA_USERNAME);
                Endpoints endpoints = getItem(username);
                endpoints.setUsername(username);
                String usn = putBundle.getString(AlexaConstants.BUNDLE_ENDPOINT_ID);
                String friendlyName = putBundle.getString(AlexaConstants.BUNDLE_FRIENDLY_NAME);
                String description = putBundle.getString(AlexaConstants.BUNDLE_DESCRIPTION);
                HashSet<String> endpointDetailsArray = getUpdatedEndpointDetailsArrayAfterAddition(putBundle,endpoints);
                endpoints.setEndpointDetailsArray(endpointDetailsArray);
               // endpoints.setRefresh_token(AlexaConstants.ALEXA_REFRESH_TOKEN);
                try {
                    mapper.save(endpoints);
                    dynamoDBAccessListener.onItemAdded(putBundle);
                } catch (ConditionalCheckFailedException e) {
                    int hitCount = putBundle.getInt(AlexaConstants.BUNDLE_HIT_COUNT);
                    hitCount++;
                    putBundle.putInt(AlexaConstants.BUNDLE_HIT_COUNT,hitCount);
                    // Another process updated this item after we loaded it, so try again with the newest data
                    putItem(putBundle,dynamoDBAccessListener);
                }

            }
        }).start();

    }
    public void updateItem(final Bundle updateBundle,final DynamoDBUpdationListener dynamoDBUpdationListener){
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (isMaxHitReached(updateBundle)){
                    dynamoDBUpdationListener.itemUpdationFailed(updateBundle);
                    return;
                }
                Endpoints endpoints = new Endpoints();
                String username = updateBundle.getString(AlexaConstants.BUNDLE_ALEXA_USERNAME);
                String endPointId = updateBundle.getString(AlexaConstants.BUNDLE_ENDPOINT_ID);
                String friendlyName = updateBundle.getString(AlexaConstants.BUNDLE_FRIENDLY_NAME);
                endpoints = getItem(username);
                HashSet<String> endpointDetailsArray = getUpdatedEndpointDetailsArray(endPointId,friendlyName,endpoints);
                endpoints.setEndpointDetailsArray(endpointDetailsArray);
                //endpoints.setUsername(username);
               // endpoints.setRefresh_token(AlexaConstants.ALEXA_REFRESH_TOKEN);
                try {
                    mapper.save(endpoints);
                    dynamoDBUpdationListener.itemUpdated(updateBundle);
                } catch (ConditionalCheckFailedException e) {
                    int hitCount = updateBundle.getInt(AlexaConstants.BUNDLE_HIT_COUNT);
                    hitCount++;
                    updateBundle.putInt(AlexaConstants.BUNDLE_HIT_COUNT,hitCount);
                    // Another process updated this item after we loaded it, so try again with the newest data
                    updateItem(updateBundle,dynamoDBUpdationListener);
                }

            }
        }).start();
    }
    private Endpoints getItem(String username){
        Endpoints endpoints = mapper.load(Endpoints.class,username);
        if (endpoints==null){
            endpoints = new Endpoints();
        }
        return endpoints;
    }
    public boolean removeEndpointDetailsArray(String usn,String friendlyName,HashSet<String> endPointArray) throws JSONException {
        Iterator iter = endPointArray.iterator();
        while (iter.hasNext()){
            String jsonString = (String) iter.next();
            JSONObject jsonObject = new JSONObject(jsonString);
            if (jsonObject.getString(AlexaConstants.JSONKey.FRIENDLY_NAME).equalsIgnoreCase(friendlyName)
                    && jsonObject.getString(AlexaConstants.JSONKey.ENDPOINT_ID).equals(usn)){
                //remove this item
                endPointArray.remove(jsonString);
                return true;
            }
        }
        return false;
    }

    public HashSet<String> getRemovedEndpointDetailsArray(String usn,String friendlyName,Endpoints endpoints) throws JSONException {
        HashSet<String> endPointArray = new HashSet<String>();
        if (endpoints == null){
            return null;
        }
        endPointArray = endpoints.getEndpointDetailsArray();
        Iterator iter = endPointArray.iterator();
        while (iter.hasNext()){
            String jsonString = (String) iter.next();
            if (jsonString==null && jsonString.isEmpty()){
                break;
            }
            JSONObject jsonObject = new JSONObject(jsonString);
            if (jsonObject.getString(AlexaConstants.JSONKey.FRIENDLY_NAME).equalsIgnoreCase(friendlyName)
                    && jsonObject.getString(AlexaConstants.JSONKey.ENDPOINT_ID).equals(usn)){
                //remove this item
                endPointArray.remove(jsonString);
                break;
            }
        }
        if (endPointArray == null || endPointArray.size() == 0){
            return null;
        }
        return endPointArray;
    }

     public HashSet<String> getUpdatedEndpointDetailsArrayAfterAddition(Bundle putBundle, Endpoints endpoints) {
        HashSet<String> endPointArray = new HashSet<String>();


        try {
            if (endpoints != null) {
               endPointArray = endpoints.getEndpointDetailsArray();
            }
            JSONObject endPointJsonObject = getEndpointJsonObject(putBundle);
                removeEndpointFromArrayIfExist(endPointJsonObject,endPointArray,endpoints);
                endPointArray.add(endPointJsonObject.toString());
            LibreLogger.d(this,"json to put to DB is "+endPointJsonObject.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return endPointArray;
    }
    public HashSet<String> getUpdatedEndpointDetailsArray(String usn, String friendlyName, Endpoints endpoints) {
        HashSet<String> endPointArray = new HashSet<String>();
        try {
            if (endpoints != null) {
                endPointArray = endpoints.getEndpointDetailsArray();
            }
            //update after getting all array
            Iterator iter = endPointArray.iterator();
            while (iter.hasNext()){
                String jsonString = (String) iter.next();
                JSONObject jsonObject = new JSONObject(jsonString);
                if (jsonObject.getString(AlexaConstants.JSONKey.ENDPOINT_ID).equals(usn)){
                    endPointArray.remove(jsonString);
                //update this item
                    jsonObject.put(AlexaConstants.JSONKey.FRIENDLY_NAME,friendlyName);
                    endPointArray.add(jsonObject.toString());
                    break;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return endPointArray;
    }

    private void removeEndpointFromArrayIfExist(JSONObject endPointJsonObject, HashSet<String> endPointArray,Endpoints endpoints){
        try {
            String friendlyName = endPointJsonObject.getString(AlexaConstants.JSONKey.FRIENDLY_NAME);
            String usn = endPointJsonObject.getString(AlexaConstants.JSONKey.ENDPOINT_ID);
            removeEndpointDetailsArray(usn,friendlyName,endPointArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private boolean isExist(JSONObject endPointJsonObject, HashSet<String> endPointArray) {
        Iterator iter = endPointArray.iterator();
        while (iter.hasNext()) {
            String jsonString = (String) iter.next();
            JSONObject jsonObject = null;
            try {
                jsonObject = new JSONObject(jsonString);
                String friendlyName = endPointJsonObject.getString(AlexaConstants.JSONKey.FRIENDLY_NAME);
                String usn = endPointJsonObject.getString(AlexaConstants.JSONKey.ENDPOINT_ID);
                if (jsonObject.getString(AlexaConstants.JSONKey.FRIENDLY_NAME).equals(friendlyName)
                        && jsonObject.getString(AlexaConstants.JSONKey.ENDPOINT_ID).equals(usn)){
                    return true;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    private ArrayList<String> getSpeakerTypeList(){
        ArrayList<String> typeList = new ArrayList<>();
        typeList.add(AlexaConstants.AlexaInterfaces.AUDIO_PLAYER);
        typeList.add(AlexaConstants.AlexaInterfaces.SPEAKER);
        typeList.add(AlexaConstants.AlexaInterfaces.PLAYBACK_CONTROLLER);
        typeList.add(AlexaConstants.AlexaInterfaces.TEMPLATE_RUNTIME);
        typeList.add(AlexaConstants.AlexaInterfaces.PLAYBACK_STATE_REPORTER);
        return typeList;
    }

    private ArrayList<String> getSpeakerTypeList(boolean isLEDEnabled) {
        ArrayList<String> typeList = getSpeakerTypeList();
        if (isLEDEnabled) {
            //typeList.add(AlexaConstants.AlexaInterfaces.POWER_CONTROLLER);
            typeList.add(AlexaConstants.AlexaInterfaces.COLOR_CONTROLLER);
        }
        return typeList;
    }

    private JSONObject getEndpointJsonObject(Bundle putBundle) throws JSONException {

        String usn = putBundle.getString(AlexaConstants.BUNDLE_ENDPOINT_ID);
        String friendlyName = putBundle.getString(AlexaConstants.BUNDLE_FRIENDLY_NAME);
        String description = putBundle.getString(AlexaConstants.BUNDLE_DESCRIPTION);
        boolean isLEDEnabled = putBundle.getBoolean(AlexaConstants.BUNDLE_IS_LED_ENABLED);

        ArrayList<String> typeList = getSpeakerTypeList(isLEDEnabled);
        JSONObject endPointJsonObject = new JSONObject();
        endPointJsonObject.put(AlexaConstants.JSONKey.ENDPOINT_ID, usn);
        endPointJsonObject.put(AlexaConstants.JSONKey.ENDPOINT_TYPE_ID, "Alexa_Libre");
        endPointJsonObject.put(AlexaConstants.JSONKey.MANUFACTURER_NAME, "3PDA");
        endPointJsonObject.put(AlexaConstants.JSONKey.FRIENDLY_NAME, friendlyName);
        endPointJsonObject.put(AlexaConstants.JSONKey.DESCRIPTION, description);
        if(isLEDEnabled){
            JSONArray displayCategoriesArray = new JSONArray();
            displayCategoriesArray.put("LIGHT");
            endPointJsonObject.put(AlexaConstants.JSONKey.DISPLAY_CATEGORIES, displayCategoriesArray);
        }
        JSONObject cookiesObj = new JSONObject();
        cookiesObj.put("abc", "abc");
        cookiesObj.put("def", "def");
        endPointJsonObject.put(AlexaConstants.JSONKey.COOKIE, cookiesObj);
        JSONArray speakerTypeArray = new JSONArray();
        JSONObject capabilitiesObj;
        for (String interfaceType : typeList) {
            switch (interfaceType){
                case AlexaConstants.AlexaInterfaces.POWER_CONTROLLER:
                    speakerTypeArray.put(getPowerControlJson());
                    break;
                case AlexaConstants.AlexaInterfaces.COLOR_CONTROLLER:
                    speakerTypeArray.put(getColorControlJson());
                    break;
                default:
                    capabilitiesObj = new JSONObject();
                    capabilitiesObj.put(AlexaConstants.JSONKey.TYPE, "AlexaInterface");
                    capabilitiesObj.put(AlexaConstants.JSONKey.INTERFACE, interfaceType);
                    capabilitiesObj.put(AlexaConstants.JSONKey.VERSION, "1.0");
                    speakerTypeArray.put(capabilitiesObj);
            }
        }
        endPointJsonObject.put("capabilities", speakerTypeArray);
        LibreLogger.d(this,"final power JSON is "+endPointJsonObject.toString());
        return endPointJsonObject;

    }

    public JSONObject getPowerControlJson() {

         JSONObject mainObj = new JSONObject();
        try {
            mainObj.put(AlexaConstants.JSONKey.TYPE, "AlexaInterface");
            mainObj.put(AlexaConstants.JSONKey.INTERFACE, "Alexa.PowerController");
            mainObj.put(AlexaConstants.JSONKey.VERSION, "3");

            JSONObject propertiesObj = new JSONObject();
            propertiesObj.put(AlexaConstants.JSONKey.PROACTIVELY_REPORTED,false);
            propertiesObj.put(AlexaConstants.JSONKey.RETRIEVABLE,false);

            JSONArray array = new JSONArray();
            JSONObject nameObj = new JSONObject();
            nameObj.put(AlexaConstants.JSONKey.NAME,"powerState");
            array.put(nameObj);
            propertiesObj.put(AlexaConstants.JSONKey.SUPPORTED,array);

            mainObj.put(AlexaConstants.JSONKey.PROPERTIES,propertiesObj);


            LibreLogger.d(this," power JSON is "+mainObj.toString());

        } catch (JSONException e) {
            e.printStackTrace();
        }


        return mainObj;
    }

    public JSONObject getColorControlJson() {

        JSONObject mainObj = new JSONObject();
        try {
            mainObj.put(AlexaConstants.JSONKey.TYPE, "AlexaInterface");
            mainObj.put(AlexaConstants.JSONKey.INTERFACE, "Alexa.ColorController");
            mainObj.put(AlexaConstants.JSONKey.VERSION, "3");

            JSONObject propertiesObj = new JSONObject();
            propertiesObj.put(AlexaConstants.JSONKey.PROACTIVELY_REPORTED,false);
            propertiesObj.put(AlexaConstants.JSONKey.RETRIEVABLE,false);

            JSONArray array = new JSONArray();
            JSONObject nameObj = new JSONObject();
            nameObj.put(AlexaConstants.JSONKey.NAME,"color");
            array.put(nameObj);
            propertiesObj.put(AlexaConstants.JSONKey.SUPPORTED,array);

            mainObj.put(AlexaConstants.JSONKey.PROPERTIES,propertiesObj);

            LibreLogger.d(this," power JSON is "+mainObj.toString());

        } catch (JSONException e) {
            e.printStackTrace();
        }


        return mainObj;
    }
}
