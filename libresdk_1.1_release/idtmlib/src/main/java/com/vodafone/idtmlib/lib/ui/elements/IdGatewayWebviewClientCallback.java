package com.vodafone.idtmlib.lib.ui.elements;

public interface IdGatewayWebviewClientCallback {
    void onRedirectResult(String code);
    void onUserCanceled();
    void onUserFailedToLogin();
    void onAuthId(String authId);
}
