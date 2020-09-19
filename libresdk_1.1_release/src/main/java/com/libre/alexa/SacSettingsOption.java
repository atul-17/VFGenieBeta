package com.libre.alexa;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import android.view.View;
import android.widget.Button;

import com.libre.alexa.Network.NewSacActivity;
import com.libre.alexa.Network.SacActivityScreenForLaunchWifiSettings;

public class SacSettingsOption extends DeviceDiscoveryActivity implements View.OnClickListener {

    Button sacConfigure,pairShareConfigure;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sac_settings_option);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        pairShareConfigure = (Button)findViewById(R.id.pairShareConfigure);
        sacConfigure = (Button)findViewById(R.id.sacConfigure);
        pairShareConfigure.setVisibility(View.GONE);
       pairShareConfigure.setOnClickListener(this);
        sacConfigure.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.pairShareConfigure) {
            startActivity(new Intent(SacSettingsOption.this, SacSettingsPairShare.class));
            finish();
        } else if (id == R.id.sacConfigure) {
            startActivity(new Intent(SacSettingsOption.this, SacActivityScreenForLaunchWifiSettings.class));
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(SacSettingsOption.this, NewSacActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
