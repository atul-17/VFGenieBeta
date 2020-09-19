package com.libre.alexa.alexa.userpoolManager.DBManager;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.*;

import java.util.HashSet;

@DynamoDBTable(tableName = "3PDA_endpoints")
public class Endpoints {
    private String username;
    private String refresh_token;
    private HashSet<String> endpointDetailsArray = new HashSet<>();

   /* @DynamoDBIndexRangeKey(attributeName = "Title")
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }*/

    @DynamoDBHashKey(attributeName = "username")
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @DynamoDBAttribute(attributeName = "endpointDetailsArray")
    public HashSet<String> getEndpointDetailsArray() {
        return endpointDetailsArray;
    }

    public void setEndpointDetailsArray(HashSet<String> endpointDetailsArray) {
        this.endpointDetailsArray = endpointDetailsArray;
    }

    @DynamoDBAttribute(attributeName = "refresh_token")
    public String getRefresh_token() {
        return refresh_token;
    }
    public void setRefresh_token(String refresh_token) {
        this.refresh_token = refresh_token;
    }

}