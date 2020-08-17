package com.libre.alexa.alexa_signin;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import com.libre.alexa.ActiveScenesListActivity;
import com.libre.alexa.Ls9Sac.ConnectingToMainNetwork;
import com.libre.alexa.Network.LSSDPDeviceNetworkSettings;
import com.libre.alexa.R;
import com.libre.alexa.SourcesOptionActivity;
import com.libre.alexa.constants.MIDCONST;
import com.libre.alexa.luci.LSSDPNodeDB;
import com.libre.alexa.luci.LSSDPNodes;
import com.libre.alexa.luci.LUCIControl;
import com.libre.alexa.util.LibreLogger;


/**
 * Created by amrit on 12/14/2016.
 */

public class AlexaThingsToTryDoneActivity extends Activity implements View.OnClickListener {

    private String speakerIpaddress;
    private TextView text_done,mAlexaApp;
    private Button signOutBtn,changeLangBtn;
    private TextView back;
    private String fromActivity;
    private String prevScreen;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.alexa_things_to_try_done);

        Intent myIntent = getIntent();
        if (myIntent != null) {
            speakerIpaddress = myIntent.getStringExtra("speakerIpaddress");
        }

        text_done = (TextView) findViewById(R.id.text_done);
        mAlexaApp = (TextView)findViewById(R.id.tv3);
        changeLangBtn = (Button) findViewById(R.id.changeLangBtn);
        signOutBtn = (Button) findViewById(R.id.signOutBtn);
        back = (TextView) findViewById(R.id.back);

        text_done.setOnClickListener(this);
        mAlexaApp.setOnClickListener(this);
        changeLangBtn.setOnClickListener(this);
        signOutBtn.setOnClickListener(this);
        back.setOnClickListener(this);

        fromActivity = getIntent().getStringExtra("fromActivity");
        prevScreen = getIntent().getStringExtra("prevScreen");

        if (prevScreen!=null && !prevScreen.isEmpty()) {
            if (prevScreen.equalsIgnoreCase(AlexaSignInActivity.class.getSimpleName())) {
                changeLangBtn.setVisibility(View.GONE);
                signOutBtn.setVisibility(View.GONE);
            }
        }

//        if (fromActivity.equalsIgnoreCase(ConnectingToMainNetwork.class.getSimpleName())){
//            signOutBtn.setVisibility(View.GONE);
//        }
    }

    public void launchTheApp(String appPackageName) {

        Intent intent = getPackageManager().getLaunchIntentForPackage(appPackageName);
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);

        } else {

            redirectingToPlayStore(intent, appPackageName);

        }

    }


    public void redirectingToPlayStore(Intent intent, String appPackageName) {

        try {

            intent = new Intent(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setData(Uri.parse("market://details?id=" + appPackageName));
            startActivity(intent);

        } catch (android.content.ActivityNotFoundException anfe) {

            intent = new Intent(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setData(Uri.parse("http://play.google.com/store/apps/details?id=" + appPackageName));
            startActivity(intent);

        }

    }
    @Override
    public void onClick(View v) {

        String readAlexaRefreshToken = "READ_AlexaRefreshToken";
        LUCIControl luciControl = new LUCIControl(speakerIpaddress);
        int id = v.getId();
        if (id == R.id.signOutBtn) {
            luciControl.SendCommand(MIDCONST.ALEXA_COMMAND, "SIGN_OUT", 2);
            LSSDPNodes mNode = LSSDPNodeDB.getInstance().getTheNodeBasedOnTheIpAddress(speakerIpaddress);
            if (mNode != null) {
                mNode.setAlexaRefreshToken("");
            }

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

//                luciControl.SendCommand(208, readAlexaRefreshToken,1);
            finish();
            LibreLogger.d(this, "clicked sign out");
        } else if (id == R.id.changeLangBtn) {
            Intent newIntent = new Intent(AlexaThingsToTryDoneActivity.this, AlexaLangUpdateActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            newIntent.putExtra("current_deviceip", speakerIpaddress);
            newIntent.putExtra("fromActivity", fromActivity);
            newIntent.putExtra("prevScreen", AlexaThingsToTryDoneActivity.class.getSimpleName());
            startActivity(newIntent);
            finish();
        } else if (id == R.id.text_done || id == R.id.back) {
            if (fromActivity != null && !fromActivity.isEmpty()) {
                if (fromActivity.equals(SourcesOptionActivity.class.getSimpleName())
                        || fromActivity.equals(AlexaSignInActivity.class.getSimpleName())) {
                    LibreLogger.d(this, "navigating to sources screen");
                    Intent intent = new Intent(AlexaThingsToTryDoneActivity.this, SourcesOptionActivity.class)
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra("current_ipaddress", speakerIpaddress);
                    startActivity(intent);
                    finish();
                } else if (fromActivity.equals(ConnectingToMainNetwork.class.getSimpleName())) {
                    Intent ssid = new Intent(AlexaThingsToTryDoneActivity.this, ActiveScenesListActivity.class)
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(ssid);
                    finish();
                } else if (fromActivity.equals(LSSDPDeviceNetworkSettings.class.getSimpleName())) {
                    Intent intent = new Intent(AlexaThingsToTryDoneActivity.this, LSSDPDeviceNetworkSettings.class)
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra("ip_address", speakerIpaddress);
                    startActivity(intent);
                    finish();
                }
            }
        } else if (id == R.id.tv3) {
            launchTheApp("com.amazon.dee.app");
        }

    }

}
