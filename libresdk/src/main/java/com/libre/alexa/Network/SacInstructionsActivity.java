package com.libre.alexa.Network;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import android.view.View;
import android.widget.Button;

import com.libre.alexa.DeviceDiscoveryActivity;
import com.libre.alexa.R;
import com.libre.alexa.SacSettingsOption;

/**
 * Created by karunakaran on 4/8/2016.
 */
public class SacInstructionsActivity extends DeviceDiscoveryActivity implements View.OnClickListener {
    Button btnNextForSac,pairShareConfigure;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sacinstruction);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        btnNextForSac = (Button)findViewById(R.id.btnNextButtonForSac);
        btnNextForSac.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btnNextButtonForSac) {
            startActivity(new Intent(SacInstructionsActivity.this, SacActivityScreenForLaunchWifiSettings.class));
            finish();
                /*   case R.id.sacConfigure:
                startActivity(new Intent(SacSettingsOption.this,NewSacDevicesActivity.class));
                break;*/
        }
    }
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(SacInstructionsActivity.this, SacSettingsOption.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

}
