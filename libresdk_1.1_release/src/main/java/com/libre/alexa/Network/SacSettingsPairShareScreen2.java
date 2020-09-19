package com.libre.alexa.Network;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import android.view.View;
import android.widget.Button;

import com.libre.alexa.ActiveScenesListActivity;
import com.libre.alexa.DeviceDiscoveryActivity;
import com.libre.alexa.R;

public class SacSettingsPairShareScreen2 extends DeviceDiscoveryActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sac_settings_pair_share_screen2);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Button doneButton=(Button)findViewById(R.id.done);
        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SacSettingsPairShareScreen2.this, ActiveScenesListActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
        });

    }
    @Override
    public void onBackPressed() {
        super.onBackPressed();

        finish();
    }
}
