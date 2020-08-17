package com.libre.alexa.alexa_signin;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.amazon.identity.auth.device.AuthError;
import com.amazon.identity.auth.device.authorization.api.AmazonAuthorizationManager;
import com.amazon.identity.auth.device.authorization.api.AuthorizationListener;
import com.amazon.identity.auth.device.authorization.api.AuthzConstants;
import com.libre.alexa.ActiveScenesListActivity;
import com.libre.alexa.DeviceDiscoveryActivity;
import com.libre.alexa.Ls9Sac.ConnectingToMainNetwork;
import com.libre.alexa.Network.LSSDPDeviceNetworkSettings;
import com.libre.alexa.R;
import com.libre.alexa.SourcesOptionActivity;
import com.libre.alexa.alexa.CompanionProvisioningInfo;
import com.libre.alexa.alexa.DeviceProvisioningInfo;
import com.libre.alexa.alexa.userpoolManager.AlexaUtils.AlexaConstants;
import com.libre.alexa.constants.LSSDPCONST;
import com.libre.alexa.constants.LUCIMESSAGES;
import com.libre.alexa.constants.MIDCONST;
import com.libre.alexa.luci.LSSDPNodeDB;
import com.libre.alexa.luci.LSSDPNodes;
import com.libre.alexa.luci.LUCIControl;
import com.libre.alexa.luci.LUCIPacket;
import com.libre.alexa.netty.LibreDeviceInteractionListner;
import com.libre.alexa.netty.NettyData;
import com.libre.alexa.util.LibreLogger;

import org.json.JSONException;
import org.json.JSONObject;

public class AlexaSignInActivity extends DeviceDiscoveryActivity implements View.OnClickListener,LibreDeviceInteractionListner {

    private Button bt_signIn;
    private TextView tv_skip;
    private TextView ib_back;
    private Dialog m_progressDlg;
    protected AlertDialog alert;
//    private static DeviceProvisioningInfo mDeviceProvisioningInfo;
    public static final String PRODUCT_ID = "productID";
    public static final String ALEXA_ALL_SCOPE = "alexa:all";
    public static final String DEVICE_SERIAL_NUMBER = "deviceSerialNumber";
    public static final String PRODUCT_INSTANCE_ATTRIBUTES = "productInstanceAttributes";
    public static final String[] APP_SCOPES = {ALEXA_ALL_SCOPE};
    final int ALEXA_META_DATA_TIMER = 0x12;
    private AmazonAuthorizationManager mAuthManager;
    private String speakerIpaddress;
    private String from;
    private AlertDialog alertDialog;
    private boolean invalidApiKey;
    private  LSSDPNodes nodes;
    private int ACCESS_TOKEN_TIMEOUT = 301;

    private boolean isMetaDateRequestSent= false;

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == ACCESS_TOKEN_TIMEOUT
                    || msg.what == ALEXA_META_DATA_TIMER) {
                closeLoader();
                somethingWentWrong(AlexaSignInActivity.this);
            }
        }
        };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alexa_signin);

        if (getIntent() != null) {
            from = getIntent().getStringExtra("fromActivity");
            speakerIpaddress = getIntent().getStringExtra("speakerIpaddress");
//            mDeviceProvisioningInfo = (DeviceProvisioningInfo) getIntent().getSerializableExtra("deviceProvisionInfo");
        }

        bt_signIn = (Button) findViewById(R.id.button);
        tv_skip = (TextView) findViewById(R.id.skip);
        ib_back = (TextView) findViewById(R.id.back);

        ib_back.setOnClickListener(this);
        bt_signIn.setOnClickListener(this);
        tv_skip.setOnClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        try {
            mAuthManager = new AmazonAuthorizationManager(this, Bundle.EMPTY);
            invalidApiKey = false;
        } catch (Exception e) {
            LibreLogger.d(this, "amazon auth exception"+e.getMessage()+"\n"+e.getStackTrace());
            invalidApiKey = true;
            Toast.makeText(this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
            bt_signIn.setEnabled(false);
            bt_signIn.setAlpha(0.5f);
            tv_skip.setVisibility(View.GONE);
//            finish();
        }
        setMetaDateRequestSent(false);
        nodes = LSSDPNodeDB.getInstance().getTheNodeBasedOnTheIpAddress(speakerIpaddress);


        registerForDeviceEvents(this);

    }

    @Override
    public void onClick(View view) {

        int id = view.getId();
        if (id == R.id.button) {/* startActivity(new Intent(this, WebViewActivity.class));*/

            if (invalidApiKey) {
                somethingWentWrong(AlexaSignInActivity.this);
                return;
            }
            if (nodes == null) {
                return;
            }


            if (nodes != null && !isMetaDateRequestSent()) {
                handler.sendEmptyMessageDelayed(ALEXA_META_DATA_TIMER, 20000);
                showLoader();
                AlexaUtils.sendAlexaMetaDataRequest(speakerIpaddress);
                setMetaDateRequestSent(true);
                return;

            }
            Bundle options = new Bundle();

            JSONObject scopeData = new JSONObject();
            JSONObject productInfo = new JSONObject();
            JSONObject productInstanceAttributes = new JSONObject();
            try {
                productInstanceAttributes.put(DEVICE_SERIAL_NUMBER, nodes.getMdeviceProvisioningInfo().getDsn());
                productInfo.put(PRODUCT_ID, nodes.getMdeviceProvisioningInfo().getProductId());
                productInfo.put(PRODUCT_INSTANCE_ATTRIBUTES, productInstanceAttributes);
                scopeData.put(ALEXA_ALL_SCOPE, productInfo);

                String codeChallenge = nodes.getMdeviceProvisioningInfo().getCodeChallenge();
                String codeChallengeMethod = nodes.getMdeviceProvisioningInfo().getCodeChallengeMethod();

                options.putString(AuthzConstants.BUNDLE_KEY.SCOPE_DATA.val, scopeData.toString());
                options.putBoolean(AuthzConstants.BUNDLE_KEY.GET_AUTH_CODE.val, true);
                options.putString(AuthzConstants.BUNDLE_KEY.CODE_CHALLENGE.val, codeChallenge);
                options.putString(AuthzConstants.BUNDLE_KEY.CODE_CHALLENGE_METHOD.val, codeChallengeMethod);
                if (mAuthManager != null) {
                    mAuthManager.authorize(APP_SCOPES, options, new AuthListener());
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else if (id == R.id.back) {
            handleBack();
        } else if (id == R.id.skip) {
            if (from != null && !from.isEmpty()) {
                startActivity(new Intent(this, AlexaThingsToTryDoneActivity.class)
                        .putExtra("speakerIpaddress", speakerIpaddress)
                        .putExtra("fromActivity", from)
                        .putExtra("prevScreen", AlexaSignInActivity.class.getSimpleName()));
                finish();
            }
        }
    }

    @Override
    public void deviceDiscoveryAfterClearingTheCacheStarted() {

    }

    @Override
    public void newDeviceFound(LSSDPNodes node) {

    }

    @Override
    public void deviceGotRemoved(String ipaddress) {

    }

    @Override
    public void messageRecieved(NettyData packet) {
        LibreLogger.d(this, "AlexaSignInActivity: New message appeared for the device " + packet.getRemotedeviceIp());
        LUCIPacket messagePacket = new LUCIPacket(packet.getMessage());
        switch (messagePacket.getCommand()){
            case MIDCONST.ALEXA_COMMAND:

                String alexaMessage = new String(messagePacket.getpayload());
                LibreLogger.d(this, "Alexa Value From 234  " + alexaMessage);
                try {
                    JSONObject jsonRootObject = new JSONObject(alexaMessage);
                    // JSONArray jsonArray = jsonRootObject.optJSONArray("Window CONTENTS");
                    JSONObject jsonObject = jsonRootObject.getJSONObject(LUCIMESSAGES.TAG_WINDOW_CONTENT);
                    String productId = jsonObject.optString("PRODUCT_ID").toString();
                    String dsn = jsonObject.optString("DSN").toString();
                    String sessionId = jsonObject.optString("SESSION_ID").toString();
                    String codeChallenge = jsonObject.optString("CODE_CHALLENGE").toString();
                    String codeChallengeMethod = jsonObject.optString("CODE_CHALLENGE_METHOD").toString();
                    String locale="";
                    if (jsonObject.has("LOCALE"))
                        locale = jsonObject.optString("LOCALE").toString();
                    LSSDPNodes node = LSSDPNodeDB.getInstance().getTheNodeBasedOnTheIpAddress(speakerIpaddress);
                    if (node != null) {
                        DeviceProvisioningInfo mDeviceProvisioningInfo = new DeviceProvisioningInfo(productId, dsn, sessionId, codeChallenge, codeChallengeMethod, locale);
                        node.setMdeviceProvisioningInfo(mDeviceProvisioningInfo);
                        handler.removeMessages(ALEXA_META_DATA_TIMER);
                        setAlexaViews();
                        closeLoader();
                        if (isMetaDateRequestSent()) {
                            performSigninClick();
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }



                /*if there is an error in fetching AccessToken
                {"Title":"ACCESS_TOKENS_STATUS","status":false}
                if successful
            {"Title":"ACCESS_TOKENS_STATUS","status":true}*/
                String message = new String(messagePacket.getpayload());
                LibreLogger.d(this,"Alexa json message is "+message);
                try {
                    JSONObject jsonObject = new JSONObject(message);
                    String title = jsonObject.getString("Title");
                    if (title!=null){
                        if (title.equals(AlexaConstants.ACCESS_TOKENS_STATUS)){
                            closeLoader();
                            boolean status = jsonObject.getBoolean("status");
                            handler.removeMessages(ACCESS_TOKEN_TIMEOUT);
                            if (status){
                                intentToAlexaLangUpdateActivity();
                            } else {
                                somethingWentWrong(AlexaSignInActivity.this);
                            }
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    private void setAlexaViews() {
        bt_signIn.setEnabled(true);
        bt_signIn.setAlpha(1f);
        tv_skip.setVisibility(View.VISIBLE);
    }

    private void performSigninClick() {
        findViewById(R.id.button).performClick();
    }

    private void intentToAlexaLangUpdateActivity() {
        Intent alexaLangScreen = new Intent(AlexaSignInActivity.this, AlexaLangUpdateActivity.class);
        alexaLangScreen.putExtra("current_deviceip", speakerIpaddress);
        alexaLangScreen.putExtra("fromActivity", from);
        alexaLangScreen.putExtra("prevScreen", AlexaSignInActivity.class.getSimpleName());
        startActivity(alexaLangScreen);
        finish();
    }

    private void closeLoader() {
        if (m_progressDlg != null) {
            if (m_progressDlg.isShowing()) {
                m_progressDlg.dismiss();
            }
        }
    }
    private void showLoader() {
        if (AlexaSignInActivity.this.isFinishing())
            return;
        if (m_progressDlg == null)
            m_progressDlg = ProgressDialog.show(AlexaSignInActivity.this, getString(R.string.notice), getString(R.string.fetchingDetails), true, true, null);
        if (!m_progressDlg.isShowing()) {
            m_progressDlg.show();
        }
    }

    public boolean isMetaDateRequestSent() {
        return isMetaDateRequestSent;
    }

    public void setMetaDateRequestSent(boolean metaDateRequestSent) {
        isMetaDateRequestSent = metaDateRequestSent;
    }

    private class AuthListener implements AuthorizationListener {
        @Override
        public void onSuccess(Bundle response) {
            try {
                nodes = LSSDPNodeDB.getInstance().getTheNodeBasedOnTheIpAddress(speakerIpaddress);
                final String authorizationCode = response.getString(AuthzConstants.BUNDLE_KEY.AUTHORIZATION_CODE.val);
                final String redirectUri = mAuthManager.getRedirectUri();
                final String clientId = mAuthManager.getClientId();
                final String sessionId = nodes.getMdeviceProvisioningInfo().getSessionId();

                LibreLogger.d(this,"Alexa Value From 234, session ID "+sessionId);
                final CompanionProvisioningInfo companionProvisioningInfo = new CompanionProvisioningInfo(sessionId, clientId, redirectUri, authorizationCode);
                LUCIControl luciControl = new LUCIControl(speakerIpaddress);

                luciControl.SendCommand(MIDCONST.ALEXA_COMMAND, "AUTHCODE_EXCH:" + companionProvisioningInfo.toJson().toString(), LSSDPCONST.LUCI_SET);
                Log.e("LSSDPNETWORK", "AuthError during authorization" + companionProvisioningInfo.toJson().toString());


               showLoader();
                handler.sendEmptyMessageDelayed(ACCESS_TOKEN_TIMEOUT,25000);

            } catch (AuthError authError) {
                authError.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onError(final AuthError ae) {
            Log.e("LSSDPNETWORK", "AuthError during authorization", ae);
            String error = ae.getMessage();
            if (error == null || error.isEmpty())
                error = ae.toString();
            final String finalError = error;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (!isFinishing()) {
                        showAlertDialog(finalError);
                    }
                }
            });
        }

        @Override
        public void onCancel(Bundle cause) {
            Log.e("LSSDPNETWORK", "User cancelled authorization");
            final String finalError = "User cancelled signin";
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (!isFinishing()) {
                        showAlertDialog(finalError);
                    }
                }
            });
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        unRegisterForDeviceEvents();
        closeLoader();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
       handler.removeCallbacksAndMessages(null);
    }

    private void showAlertDialog(String error) {

        if (alertDialog!=null && alertDialog.isShowing())
            alertDialog.dismiss();

        AlertDialog.Builder builder = new AlertDialog.Builder(AlexaSignInActivity.this);
        builder.setTitle("Alexa Signin Error");
        builder.setMessage(error);
        builder.setNeutralButton("Close", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                alertDialog.dismiss();
            }
        });

        if (alertDialog == null) {
            alertDialog = builder.create();
            alertDialog.show();
        }

    }

    private void handleBack(){
        if (from!=null && !from.isEmpty()){
            if (from.equalsIgnoreCase(ConnectingToMainNetwork.class.getSimpleName())){
                Intent ssid = new Intent(AlexaSignInActivity.this, ActiveScenesListActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(ssid);
                finish();
            } else if (from.equalsIgnoreCase(SourcesOptionActivity.class.getSimpleName())){
                Intent intent = new Intent(AlexaSignInActivity.this, SourcesOptionActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra("current_ipaddress", speakerIpaddress);
                startActivity(intent);
                finish();
            } else if (from.equalsIgnoreCase(LSSDPDeviceNetworkSettings.class.getSimpleName())){
                Intent intent = new Intent(AlexaSignInActivity.this, LSSDPDeviceNetworkSettings.class);
                intent.putExtra("ip_address", speakerIpaddress);
                startActivity(intent);
                finish();
            }
        }
    }
}
