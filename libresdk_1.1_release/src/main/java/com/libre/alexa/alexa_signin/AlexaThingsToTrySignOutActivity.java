package com.libre.alexa.alexa_signin;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.libre.alexa.R;
import com.libre.alexa.constants.MIDCONST;
import com.libre.alexa.luci.LSSDPNodeDB;
import com.libre.alexa.luci.LSSDPNodes;
import com.libre.alexa.luci.LUCIControl;

/**
 * Created by amrit on 12/14/2016.
 */

public class AlexaThingsToTrySignOutActivity extends Activity implements View.OnClickListener {

    private String speakerIpaddress;
    private RelativeLayout Rl_sign_out;
    private TextView text_sign_out, mAlexaApp;


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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.alexa_sign_out);

        Intent myIntent = getIntent();
        if (myIntent != null) {
            speakerIpaddress = myIntent.getStringExtra("speakerIpaddress");
           // Toast.makeText(AlexaThingsToTrySignOutActivity.this, "ip address: "+speakerIpaddress,Toast.LENGTH_SHORT).show();
        }

        inItWidgets();
        setEventListeners();
    }

    private void inItWidgets() {
        Rl_sign_out = (RelativeLayout) findViewById(R.id.Rl_sign_out);
        text_sign_out = (TextView) findViewById(R.id.text_sign_out);
        mAlexaApp = (TextView)findViewById(R.id.tv3);
    }

    private void setEventListeners() {
        Rl_sign_out.setOnClickListener(this);
        text_sign_out.setOnClickListener(this);
        mAlexaApp.setOnClickListener(this);
    }



    @Override
    public void onClick(View v) {
        if(v.getId() == Rl_sign_out.getId() || v.getId() == text_sign_out.getId()){

            LUCIControl luciControl = new LUCIControl(speakerIpaddress);
            luciControl.SendCommand(MIDCONST.ALEXA_COMMAND, "SIGN_OUT",2);
            LSSDPNodes mNode =LSSDPNodeDB.getInstance().getTheNodeBasedOnTheIpAddress(speakerIpaddress);
            if(mNode!=null) {
                mNode.setAlexaRefreshToken("");
            }
            String readAlexaRefreshToken = "READ_AlexaRefreshToken";
            luciControl.SendCommand(208, readAlexaRefreshToken,1);
            finish();
            Log.d("Amrit", "clicked sign out");
        }
        if(v.getId() == mAlexaApp.getId()){
            launchTheApp("com.amazon.dee.app");
        }

    }
}
