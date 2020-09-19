package com.libre.alexa.alexa_signin;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Message;
import androidx.annotation.IdRes;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.libre.alexa.ActiveScenesListActivity;
import com.libre.alexa.DeviceDiscoveryActivity;
import com.libre.alexa.LErrorHandeling.LibreError;
import com.libre.alexa.Ls9Sac.ConnectingToMainNetwork;
import com.libre.alexa.Network.LSSDPDeviceNetworkSettings;
import com.libre.alexa.R;
import com.libre.alexa.Scanning.Constants;
import com.libre.alexa.SourcesOptionActivity;
import com.libre.alexa.alexa.userpoolManager.AlexaUtils.AlexaConstants;
import com.libre.alexa.constants.LSSDPCONST;
import com.libre.alexa.constants.LUCIMESSAGES;
import com.libre.alexa.constants.MIDCONST;
import com.libre.alexa.luci.LSSDPNodes;
import com.libre.alexa.luci.LUCIControl;
import com.libre.alexa.luci.LUCIPacket;
import com.libre.alexa.netty.LibreDeviceInteractionListner;
import com.libre.alexa.netty.NettyData;
import com.libre.alexa.util.LibreLogger;

import org.json.JSONException;
import org.json.JSONObject;

public class AlexaLangUpdateActivity extends DeviceDiscoveryActivity implements LibreDeviceInteractionListner {
    private RadioGroup chooseLangRg;
    private TextView done, back;
    private String selectedLang = "";
    private boolean changesDone;
    private LUCIControl luciControl;
    private String currentDeviceIp;
    private String fromActivity;
    private String prevScreen;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            if (msg.what == Constants.ALEXA_CHECK_TIMEOUT) {
                closeLoader();
                LibreError libreError = new LibreError(currentDeviceIp,getString(R.string.requestTimeout));
                showErrorMessage(libreError);
            }
        }
    };

    private ProgressDialog progressDialog;

    private void closeLoader() {
        if (progressDialog!=null && progressDialog.isShowing())
            progressDialog.dismiss();
        mHandler.removeMessages(Constants.ALEXA_CHECK_TIMEOUT);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alexa_update_lang);

        chooseLangRg = (RadioGroup) findViewById(R.id.chooseLangRg);
        done = (TextView) findViewById(R.id.done);
        back = (TextView) findViewById(R.id.back);

        fromActivity = getIntent().getStringExtra("fromActivity");
        prevScreen = getIntent().getStringExtra("prevScreen");

        chooseLangRg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, @IdRes int i) {
                changesDone = true;
                done.setText("Done");
                /*checkedId*/
                if (i == R.id.enUsRb) {
                    selectedLang = AlexaConstants.Languages.ENG_US;
                } else if (i == R.id.engUkRb) {
                    selectedLang = AlexaConstants.Languages.ENG_GB;
                } else if (i == R.id.engINRb) {
                    selectedLang = AlexaConstants.Languages.ENG_IN;
                } else if (i == R.id.deutschRb) {
                    selectedLang = AlexaConstants.Languages.DE;
                } else if (i == R.id.japanRb) {
                    selectedLang = AlexaConstants.Languages.JP;
                }
            }
        });

        done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (done.getText().equals("Skip")){
                    Intent newIntent = new Intent(AlexaLangUpdateActivity.this, AlexaThingsToTryDoneActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    newIntent.putExtra("speakerIpaddress", currentDeviceIp);
                    newIntent.putExtra("fromActivity", fromActivity);
                    startActivity(newIntent);
                    finish();
                    return;
                }

                if (changesDone && !selectedLang.isEmpty()){
                    sendUpdatedLangToDevice();
                }
                if (fromActivity!=null && !fromActivity.isEmpty()){
                    Intent newIntent = new Intent(AlexaLangUpdateActivity.this, AlexaThingsToTryDoneActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    newIntent.putExtra("speakerIpaddress", currentDeviceIp);
                    newIntent.putExtra("fromActivity", fromActivity);
                    startActivity(newIntent);
                    finish();
                }
            }
        });

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (fromActivity != null && !fromActivity.isEmpty()) {
                    if (fromActivity.equals(SourcesOptionActivity.class.getSimpleName())
                            || fromActivity.equals(AlexaSignInActivity.class.getSimpleName())) {
                        Intent intent = new Intent(AlexaLangUpdateActivity.this, SourcesOptionActivity.class)
                                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.putExtra("current_ipaddress", currentDeviceIp);
                        startActivity(intent);
                        finish();
                    } else if (fromActivity.equals(ConnectingToMainNetwork.class.getSimpleName())) {
                        Intent ssid = new Intent(AlexaLangUpdateActivity.this, ActiveScenesListActivity.class)
                                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(ssid);
                        finish();
                    } else if (fromActivity.equals(LSSDPDeviceNetworkSettings.class.getSimpleName())) {
                        Intent intent = new Intent(AlexaLangUpdateActivity.this, LSSDPDeviceNetworkSettings.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.putExtra("ip_address", currentDeviceIp);
                        startActivity(intent);
                        finish();
                    }
                }
            }
        });

        if (getIntent()!=null
                && getIntent().getStringExtra("current_deviceip")!=null
                && !getIntent().getStringExtra("current_deviceip").isEmpty()){
            currentDeviceIp = getIntent().getStringExtra("current_deviceip");
        }

        luciControl = new LUCIControl(currentDeviceIp);
        if (prevScreen.equalsIgnoreCase(AlexaSignInActivity.class.getSimpleName())){
            done.setText("Done");
            back.setVisibility(View.VISIBLE);
        } else {
            back.setVisibility(View.GONE);
        }
    }

    private void sendUpdatedLangToDevice() {
        luciControl.SendCommand(MIDCONST.ALEXA_COMMAND, LUCIMESSAGES.UPDATE_LOCALE+ selectedLang, LSSDPCONST.LUCI_SET);
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerForDeviceEvents(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        luciControl.SendCommand(MIDCONST.ALEXA_COMMAND, "DEVICE_METADATA_REQUEST", LSSDPCONST.LUCI_SET);
    }

    private void showloader() {
        if (progressDialog!=null && progressDialog.isShowing())
            progressDialog.dismiss();
        progressDialog = new ProgressDialog(AlexaLangUpdateActivity.this);
        progressDialog.setCancelable(false);
        progressDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

        /*If you happen to get the error : "requestFeature() must be called before adding content",
        the solution is to call progressDialog.show() BEFORE you call progressDialog.setContentView(R.layout.progressdialog)*/
        progressDialog.show();
        progressDialog.setContentView(R.layout.progress_loader);
        mHandler.sendEmptyMessageDelayed(Constants.ALEXA_CHECK_TIMEOUT,Constants.INTERNET_PLAY_TIMEOUT);
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
    public void messageRecieved(NettyData dataRecived) {
        String ipaddressRecieved = dataRecived.getRemotedeviceIp();

        LUCIPacket packet = new LUCIPacket(dataRecived.getMessage());
        LibreLogger.d(this, "Message recieved for ipaddress " + ipaddressRecieved + "command is " + packet.getCommand());

        if (currentDeviceIp.equalsIgnoreCase(ipaddressRecieved)) {
            switch (packet.getCommand()) {

                case MIDCONST.ALEXA_COMMAND: {
                    closeLoader();
                    String alexaMessage = new String(packet.getpayload());
                    LibreLogger.d(this, "Alexa Value From 230  " + alexaMessage);
                    try {
                        JSONObject jsonRootObject = new JSONObject(alexaMessage);
                        JSONObject jsonObject = jsonRootObject.getJSONObject(LUCIMESSAGES.TAG_WINDOW_CONTENT);
                        String locale = jsonObject.optString("LOCALE").toString();
                        updateLang(locale);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }


                }
                break;
            }
        }
    }

    private void updateLang(String locale) {
        switch (locale){
            case AlexaConstants.Languages.ENG_US:
                chooseLangRg.check(R.id.enUsRb);
                break;
            case AlexaConstants.Languages.ENG_GB:
                chooseLangRg.check(R.id.engUkRb);
                break;
            case AlexaConstants.Languages.ENG_IN:
                chooseLangRg.check(R.id.engINRb);
                break;
            case AlexaConstants.Languages.DE:
                chooseLangRg.check(R.id.deutschRb);
                break;
            case AlexaConstants.Languages.JP:
                chooseLangRg.check(R.id.japanRb);
                break;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        unRegisterForDeviceEvents();
    }
}
