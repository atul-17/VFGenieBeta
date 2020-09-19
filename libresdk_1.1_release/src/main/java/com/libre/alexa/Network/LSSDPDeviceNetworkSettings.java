package com.libre.alexa.Network;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.Toolbar;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.util.Linkify;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.libre.alexa.DeviceDiscoveryActivity;

import com.libre.alexa.Gcast;
import com.libre.alexa.LibreApplication;
import com.libre.alexa.LibreWebViewActivity;

import com.libre.alexa.R;
import com.libre.alexa.Scanning.Constants;
import com.libre.alexa.Scanning.ScanningHandler;


import com.libre.alexa.alexa.userpoolManager.AlexaListeners.DynamoDBAdditionListener;
import com.libre.alexa.alexa.userpoolManager.AlexaListeners.DynamoDBDeletionListener;
import com.libre.alexa.alexa.userpoolManager.AlexaListeners.DynamoDBUpdationListener;
import com.libre.alexa.alexa.userpoolManager.AlexaUtils.AlexaConstants;
import com.libre.alexa.alexa.userpoolManager.DBManager.AlexaLoginRepo;
import com.libre.alexa.alexa.userpoolManager.DBManager.DynamoDBHelper;
import com.libre.alexa.alexa.userpoolManager.MainActivity;
import com.libre.alexa.alexa_signin.AlexaContactsActivity;
import com.libre.alexa.constants.CommandType;
import com.libre.alexa.constants.LSSDPCONST;
import com.libre.alexa.constants.MIDCONST;
import com.libre.alexa.led.LedActivity;
import com.libre.alexa.luci.LSSDPNodeDB;
import com.libre.alexa.luci.LSSDPNodes;
import com.libre.alexa.luci.LUCIControl;
import com.libre.alexa.luci.LUCIPacket;
import com.libre.alexa.models.ModelStoreDeviceDetails;
import com.libre.alexa.netty.LibreDeviceInteractionListner;
import com.libre.alexa.netty.NettyData;
import com.libre.alexa.util.LibreLogger;
import com.libre.alexa.utils.DynamicAlertDialogBox;
import com.libre.alexa.utils.interfaces.OnNextButtonClickFromAlertDialogInterface;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class LSSDPDeviceNetworkSettings extends DeviceDiscoveryActivity implements AdapterView.OnItemSelectedListener, LibreDeviceInteractionListner {

    Button btnSave, btnNetworkSettings, btnAdvanceSettings, btnOpenSettingsInCast, btnLink, btnVox;
    Spinner mSpinner;
    Spinner mPresetSpinner;
    String deviceSSID, speakerIpaddress, activityName;
    String DeviceName;
    EditText deviceName;
    TextView ipAddress, firmwareVersion, title, networkSettings, hostVersion;
    ImageButton btnDeviceName;
    String mAudioOutput = "";
    String mAudioPreset = "";
    boolean mSpinnerChanged = false;
    boolean mDeviceNameChanged = false;
    private Dialog m_progressDlg;

    public final int STEREO = 0;
    public final int LEFT = 1;
    public final int RIGHT = 2;

    String deviceEndPointID = "NA";
    RelativeLayout ledController;

    private int LED_SUCCESS = 12;
    private int LED_FAILURE = 13;
    private int LED_TIMEOUT = 14;
    private int TIMEOUT = 4000;

    public static final int ACTIVITYREQUESTCODE = 1;

    public AppCompatButton btnPcoLink;

    List<ModelStoreDeviceDetails> modelStoreDeviceDetailsList = new ArrayList<>();

//    private String productId = "", dsn = "", sessionId = "", codeChallenge = "", codeChallengeMethod = "";


    ScanningHandler mScanHandler = ScanningHandler.getInstance();
    private String locale = "";

    public void StartLSSDPScan() {

        final LibreApplication application = (LibreApplication) getApplication();
        application.getScanThread().clearNodes();
        application.getScanThread().UpdateNodes();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                application.getScanThread().UpdateNodes();
            }
        }, 150);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                application.getScanThread().UpdateNodes();

            }
        }, 650);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                application.getScanThread().UpdateNodes();

            }
        }, 1150);
    }


    Handler ledHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            if (msg.what == LED_SUCCESS) {
                closeLoader();
                ledHandler.removeMessages(LED_TIMEOUT);
                ledController.setVisibility(View.VISIBLE);
            }

            if (msg.what == LED_FAILURE) {
                closeLoader();
                ledHandler.removeMessages(LED_TIMEOUT);
                ledController.setVisibility(View.GONE);
            }

            if (msg.what == LED_TIMEOUT) {
                closeLoader();
                ledController.setVisibility(View.GONE);
            }
        }
    };

    private void enableOrDisableEditDeviceNameButton() {
        LSSDPNodes mNode = LSSDPNodeDB.getInstance().getTheNodeBasedOnTheIpAddress(speakerIpaddress);

        if (mNode != null &&
                mNode.getgCastVerision() != null) {
            btnDeviceName.setVisibility(View.INVISIBLE);
        } else {
            btnDeviceName.setVisibility(View.VISIBLE);
        }
    }

//    private LinearLayout btnAlexaEnable;

    private Button btnAlexaContacts;

    private void hideLinkButton() {
        btnLink.setVisibility(View.GONE);
    }

    private void showLinkButton() {
        btnLink.setVisibility(View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setViews();
        /*checking led status*/
        LUCIControl luciControl = new LUCIControl(speakerIpaddress);
        luciControl.sendAsynchronousCommand();
        luciControl.SendCommand(208, "READ_LEDControl", 2);
        LibreLogger.d(this, "bhargav LED SENT");
        luciControl.SendCommand(208, "READ_audiopreset", 2);
        LibreLogger.d(this, "bhargav AP SENT");
        luciControl.SendCommand(145, null, LSSDPCONST.LUCI_GET);

        hideLinkButton();
        luciControl.SendCommand(MIDCONST.ALEXA_COMMAND, "GET_ENDPOINTINFO", CommandType.SET);

        luciControl.SendCommand(208, "READ_3pdauserid", LSSDPCONST.LUCI_SET);


    }


    private void setDeviceEndPointID(String deviceEndPointID) {
        this.deviceEndPointID = deviceEndPointID;
    }

    private String getDeviceEndPointID() {
        return this.deviceEndPointID;
    }

    private void setViews() {
        //setBtnLink();
    }

    DynamicAlertDialogBox dynamicAlertDialogBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_setup_screen);
        Intent myIntent = getIntent(); // gets the previously created intent
        deviceSSID = myIntent.getStringExtra("DeviceName");
        speakerIpaddress = myIntent.getStringExtra("ip_address");
        activityName = myIntent.getStringExtra("activityName");

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(true);

        registerForDeviceEvents(this);

        btnPcoLink = findViewById(R.id.btnPcoLink);

        dynamicAlertDialogBox = new DynamicAlertDialogBox();
        showLoader();
        readVOXControlValue(speakerIpaddress);
//        readEnvVox(speakerIpaddress);
        ledHandler.sendEmptyMessageDelayed(LED_TIMEOUT, TIMEOUT);

        ledController = (RelativeLayout) findViewById(R.id.led_control_layout);
        ledController.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                Intent ledIntent = new Intent(LSSDPDeviceNetworkSettings.this, LedActivity.class);
                ledIntent.putExtra("DeviceName", deviceSSID);
                ledIntent.putExtra("ip_address", speakerIpaddress);
                startActivity(ledIntent);

            }
        });

        LSSDPNodes deviceNode = LSSDPNodeDB.getInstance().getTheNodeBasedOnTheIpAddress(speakerIpaddress);

        btnSave = (Button) findViewById(R.id.btnSaveNetworkSettings);
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (deviceName.isEnabled()) {
                    new AlertDialog.Builder(LSSDPDeviceNetworkSettings.this)
                            .setTitle(getString(R.string.sceneNameChanging))
                            .setMessage(getString(R.string.sceneNameChangingMsg))
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    goNext();

                                }
                            }).setIcon(android.R.drawable.ic_dialog_alert)
                            .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                }
                            })
                            .show();
                } else
                    goNext();

            }
        });


        LSSDPNodeDB mNodeDB = LSSDPNodeDB.getInstance();
        final LSSDPNodes node = mNodeDB.getTheNodeBasedOnTheIpAddress(speakerIpaddress);
        title = (TextView) findViewById(R.id.textView4);
        title.setText(getString(R.string.deviceName));
        ipAddress = (TextView) findViewById(R.id.ipAddress);
        firmwareVersion = (TextView) findViewById(R.id.firmwareVersion);
        hostVersion = (TextView) findViewById(R.id.hostVersion);
        btnVox = (Button) findViewById(R.id.btnVox);
        //     networkSettings = (TextView) findViewById(R.id.tvNetworkSettings);
        deviceName = (EditText) findViewById(R.id.eTSceneName);
        deviceName.setText(deviceSSID);
        DeviceName = deviceName.getText().toString();
        btnDeviceName = (ImageButton) findViewById(R.id.btnSceneName);
        enableOrDisableEditDeviceNameButton();
        deviceName.setEnabled(false);
        ipAddress.append(speakerIpaddress);


        if (node != null && node.getVersion() != null) {
            String dutExistingFirmware = node.getVersion();

            String[] arrayString = dutExistingFirmware.split("\\.");
            dutExistingFirmware = dutExistingFirmware.substring(0, dutExistingFirmware.indexOf('.'));
            String dutExistingHostVersion = arrayString[1].replaceAll("[a-zA-z]", "");
            /*String mFirmwareVersionToDisplay = dutExistingFirmware.replaceAll("[a-zA-z]", "")+"."+arrayString[1]+"."+
                    arrayString[2];*/
            firmwareVersion.append(dutExistingFirmware.replaceAll("[a-zA-z]", ""));
            hostVersion.append(dutExistingHostVersion);
        } else {
            firmwareVersion.setVisibility(View.GONE);
            hostVersion.setVisibility(View.GONE);
        }


        //speakerIpaddress
        btnAdvanceSettings = (Button) findViewById(R.id.btnAdvanceSettings);
        btnOpenSettingsInCast = (Button) findViewById(R.id.btnOpenSettingsInCast);

        /* LS9 App Will have disable Gcast Settings */
       /* if (deviceNode!=null&& deviceNode.getgCastVerision()!=null) {
            btnAdvanceSettings.setVisibility(View.VISIBLE);
            btnOpenSettingsInCast.setVisibility(View.VISIBLE);
        }
        else */
        {
            btnAdvanceSettings.setVisibility(View.INVISIBLE);
            btnOpenSettingsInCast.setVisibility(View.INVISIBLE);
        }

        btnAdvanceSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                if (googleTosCheckIsSuccess()) {
                    Intent ledIntent = new Intent(LSSDPDeviceNetworkSettings.this, Gcast.class);
                    ledIntent.putExtra("DeviceName", deviceSSID);
                    ledIntent.putExtra("ip_address", speakerIpaddress);
                    startActivity(ledIntent);
                }


            }
        });

        btnOpenSettingsInCast.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // deep linking
                if (googleTosCheckIsSuccess()) {
                    // launchTheApp("com.google.android.apps.chromecast.app");
                    LibreLogger.d(this, "intenting to Chrome cast app. Deeplink-> DEVICE_SETTINGS");
                    launchTheApp("com.google.android.apps.chromecast.app", "DEVICE_SETTINGS");

                }
            }
        });

        btnDeviceName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LSSDPNodes nodes = LSSDPNodeDB.getInstance().getTheNodeBasedOnTheIpAddress(speakerIpaddress);

                if (!deviceName.isEnabled()) {
                    LibreLogger.d(this, "bhargav123 clicked");
                    btnDeviceName.setImageResource(R.mipmap.check);
                    deviceName.setClickable(true);
                    deviceName.setEnabled(true);
                    deviceName.setFocusableInTouchMode(true);
                    deviceName.setFocusable(true);
                    deviceName.requestFocus();
                    deviceName.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.cancwel, 0);
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.showSoftInput(deviceName, InputMethodManager.SHOW_IMPLICIT);
                } else {
                    LibreLogger.d(this, "bhargav123 clicked again");
                    btnDeviceName.setImageResource(R.mipmap.ic_mode_edit_black_24dp);
                    deviceName.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                    if (nodes != null && nodes.getFriendlyname().equals(deviceName.getText().toString())) {
                        LibreLogger.d(this, "No need to update since both names are same");
                        deviceName.setClickable(false);
                        deviceName.setEnabled(false);
                        mDeviceNameChanged = false;
                        return;
                    }
//                    deviceName.setClickable(false);
//                    deviceName.setEnabled(false);
                    if (mDeviceNameChanged) {
                        if (!deviceName.getText().toString().equals("") && !deviceName.getText().toString().trim().equalsIgnoreCase("NULL")) {
                            if (deviceName.getText().toString().getBytes().length > 50) {
                                new AlertDialog.Builder(LSSDPDeviceNetworkSettings.this)
                                        .setTitle(getString(R.string.deviceNameChanging))
                                        .setMessage(getString(R.string.deviceLength))
                                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                                dialog.cancel();
                                            }
                                        }).setIcon(android.R.drawable.ic_dialog_alert)
                                        .show();
                                return;
                            } else {
                                LUCIControl mLuci = new LUCIControl(speakerIpaddress);
                                mLuci.sendAsynchronousCommand();
                                mLuci.SendCommand(MIDCONST.MID_DEVNAME, deviceName.getText().toString(), LSSDPCONST.LUCI_SET);

                                UpdateLSSDPNodeDeviceName(speakerIpaddress, deviceName.getText().toString());
                                showLoaderOnUI();
                                deviceName.setClickable(false);
                                deviceName.setEnabled(false);

                                //update item in db
                                updateDeviceNameInDB(getDeviceEndPointID(), deviceName.getText().toString());
                            }
                        } else {
                            if (!(LSSDPDeviceNetworkSettings.this.isFinishing())) {
                                if (deviceName.getText().toString().equals("")) {
                                    new AlertDialog.Builder(LSSDPDeviceNetworkSettings.this)
                                            .setTitle(getString(R.string.deviceNameChanging))
                                            .setMessage(getString(R.string.deviceNameEmpty))
                                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int which) {
                                                    dialog.cancel();
                                                }
                                            }).setIcon(android.R.drawable.ic_dialog_alert)
                                            .show();
                                }
                            }
                        }


                    }
                }
            }
        });


        final LSSDPNodes mNode = mScanHandler.getLSSDPNodeFromCentralDB(speakerIpaddress);
        final int[] mPreviousLength = new int[1];
        final String[] mTextWatching = new String[1];
        deviceName.addTextChangedListener(new TextWatcher() {

            public void afterTextChanged(Editable s) {
                Log.d("Karuna", "After Text Changed");
                if (deviceName.getText().toString().getBytes().length == 50 && deviceName.isEnabled()) {
                    Toast.makeText(LSSDPDeviceNetworkSettings.this, getString(R.string.deviceLengthReached), Toast.LENGTH_SHORT).show();

                }

                if (deviceName.getText().toString().getBytes().length > 50) {
                    if (mTextWatching[0].isEmpty() || mTextWatching[0] == null) {

                        deviceName.setText(utf8truncate(deviceName.getText().toString(), 50));
                        deviceName.setSelection(mTextWatching[0].length());
                    } else {
                        deviceName.setText(mTextWatching[0]);
                        deviceName.setSelection(mTextWatching[0].length());
                    }
                    Toast.makeText(LSSDPDeviceNetworkSettings.this, getString(R.string.deviceLength), Toast.LENGTH_SHORT).show();/*
                    LibreError error = new LibreError("Sorry!!!!", getString(R.string.deviceLength));
                    showErrorMessage(error);*/
                  /*  new AlertDialog.Builder(LSSDPDeviceNetworkSettings.this)
                            .setTitle(getString(R.string.deviceNameChanging))
                            .setMessage(getString(R.string.deviceLength))
                            .setCancelable(false)
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();

                                }
                            }).setIcon(android.R.drawable.ic_dialog_alert)
                            .show();*/
                } else {
                    mTextWatching[0] = deviceName.getText().toString();
                }
            }

            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
                Log.d("Karuna", "Before Text Changed");
            }

            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
                Log.d("Karuna", "On Text Changed");
                // if(! deviceName.isClickable()) {
                mDeviceNameChanged = true;
            }
        });

        deviceName.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                final int DRAWABLE_RIGHT = 2;
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    try {
                        if (event.getRawX() >= (deviceName.getRight() - deviceName.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {
                            // your action here
                            deviceName.setText("");
                            return true;
                        }
                    } catch (Exception e) {
                        //Toast.makeText(getApplication(), "dsd", Toast.LENGTH_SHORT).show();
                        LibreLogger.d(this, "ignore this log");
                    }
                }
                return false;
            }
        });

        /*this is the data for Audio Presets*/
        final String presetSpinnerData[] = getResources().getStringArray(R.array.audio_preset_array);
        mPresetSpinner = (Spinner) findViewById(R.id.audio_preset_spinner);
        ArrayAdapter<String> presetDataAdapter = new ArrayAdapter<String>
                (this, android.R.layout.simple_spinner_item, presetSpinnerData);
        presetDataAdapter.setDropDownViewResource
                (android.R.layout.simple_spinner_dropdown_item);
        mPresetSpinner.setAdapter(presetDataAdapter);


        /*Speaker type data*/
        final String spinnerData[] = getResources().getStringArray(R.array.audio_output_array);
        mSpinner = (Spinner) findViewById(R.id.ssidSpinner);
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>
                (this, android.R.layout.simple_spinner_item, spinnerData);
        dataAdapter.setDropDownViewResource
                (android.R.layout.simple_spinner_dropdown_item);
        mSpinner.setAdapter(dataAdapter);


        try {
            if (mNode != null) {
                if (Integer.valueOf(mNode.getSpeakerType()) == STEREO) {
                    mSpinner.setSelection(0);
                } else if (Integer.valueOf(mNode.getSpeakerType()) == LEFT) {
                    mSpinner.setSelection(1);
                } else if (Integer.valueOf(mNode.getSpeakerType()) == RIGHT) {
                    mSpinner.setSelection(2);
                }
            } else {
                mSpinner.setSelection(0);
            }
        } catch (Exception e) {

        }


        mPresetSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View arg1, int arg2,
                                       long arg3) {
                //Toast.makeText(getApplicationContext(), "ItemSelected" + spinnerdata[i], Toast.LENGTH_SHORT).show();
                mAudioPreset = "" + (arg2 + 1);
//                mSpinnerChanged = true;

//                String item = (String) parent.getItemAtPosition(arg2);
//                ((TextView) parent.getChildAt(0)).setTextColor(Color.RED);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                // Toast.makeText(getApplicationContext(), "Nothing Item Selected", Toast.LENGTH_SHORT).show();
//                mSpinnerChanged = false;
            }
        });

        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                //Toast.makeText(getApplicationContext(), "ItemSelected" + spinnerdata[i], Toast.LENGTH_SHORT).show();
                mAudioOutput = spinnerData[i];
                mSpinnerChanged = true;
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                // Toast.makeText(getApplicationContext(), "Nothing Item Selected", Toast.LENGTH_SHORT).show();
                mSpinnerChanged = false;
            }
        });

        btnLink = (Button) findViewById(R.id.btnLink);
        if (!LibreApplication.getIs3PDAEnabled()) {
            btnLink.setVisibility(View.GONE);
        } else {
            btnLink.setVisibility(View.GONE);
        }
        btnLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LibreLogger.d(this, "sending 91");
                String id = "bhargav.r";
                LSSDPNodeDB mNodeDB = LSSDPNodeDB.getInstance();
                LSSDPNodes node = mNodeDB.getTheNodeBasedOnTheIpAddress(speakerIpaddress);
                String friendlyName = node.getFriendlyname();
                //mac address
                String endPointID = getDeviceEndPointID();
                if (AlexaLoginRepo.getRepo().isLoggedIn()) {
                    // enter/delete from dynamoDB
                    Bundle bundle = new Bundle();
                    bundle.putString(AlexaConstants.BUNDLE_IPADDRESS, speakerIpaddress);
                    bundle.putString(AlexaConstants.BUNDLE_ALEXA_USERNAME, AlexaLoginRepo.getRepo().getLinkedDeviceDetails().get(speakerIpaddress));
                    bundle.putString(AlexaConstants.BUNDLE_ENDPOINT_ID, endPointID);
                    bundle.putString(AlexaConstants.BUNDLE_FRIENDLY_NAME, friendlyName.trim());
                    bundle.putBoolean(AlexaConstants.BUNDLE_IS_LED_ENABLED, isLEDEnabled(node));
                    bundle.putString(AlexaConstants.BUNDLE_DESCRIPTION, "description");
                    HashMap<String, String> linkedMap = AlexaLoginRepo.getRepo().getLinkedDeviceDetails();
                    if (linkedMap.containsKey(speakerIpaddress)) {
                        // this device is already linked
                        bundle.putString(AlexaConstants.BUNDLE_ALEXA_USERNAME, AlexaLoginRepo.getRepo().getLinkedDeviceDetails().get(speakerIpaddress));
                        unlinkAlexaDevice(bundle);
                    } else {
                        // this device is not linked to any user
                        bundle.putString(AlexaConstants.BUNDLE_ALEXA_USERNAME, AlexaLoginRepo.getRepo().getUsername());
                        linkAlexaDevice(bundle);
                    }
                } else {
                    if (LibreApplication.getIs3PDAEnabled()) {
                        Intent intent = new Intent(LSSDPDeviceNetworkSettings.this, MainActivity.class);
                        intent.putExtra(AlexaConstants.INTENT_FROM_ACTIVITY, AlexaConstants.INTENT_FROM_ACTIVITY_ADVANCED_SETTINGS);
                        startActivity(intent);
                    } else {
                        showToast(getApplicationContext(), "This feature is not available");
                    }

                }
            }
        });

        btnNetworkSettings = (Button) findViewById(R.id.btnNetworkSettings);
        btnNetworkSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                ScanningHandler m_scanHandler = ScanningHandler.getInstance();
                final LSSDPNodes node = m_scanHandler.getLSSDPNodeFromCentralDB(speakerIpaddress);
                if (node == null) {
                    return;
                }
                Intent ssid = new Intent(LSSDPDeviceNetworkSettings.this, ssid_configuration.class);
                ssid.putExtra("DeviceIP", speakerIpaddress);
                ssid.putExtra("DeviceSSID", DeviceName);
                ssid.putExtra("activity", "LSSDPDeviceNetworkSettings");
                startActivity(ssid);
            }
        });

//        if (node != null) {
//            if (node.getEnvVoxReadValue() != null) {
//                if (node.getEnvVoxReadValue().isEmpty()) {
//                    btnVox.setVisibility(View.GONE);
//                } else {
//                    btnVox.setVisibility(View.VISIBLE);
//                }
//            } else {
//                btnVox.setVisibility(View.GONE);
//            }
//        }

//        btnVox.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                LSSDPNodeDB mNodeDB = LSSDPNodeDB.getInstance();
//                final LSSDPNodes node = mNodeDB.getTheNodeBasedOnTheIpAddress(speakerIpaddress);
//                showLoader();
//                readEnvVox(node.getIP());
//                readVOXControlValue(speakerIpaddress);
//                new Handler().postDelayed(new Runnable() {
//                    @Override
//                    public void run() {
//                        if (node.getEnvVoxReadValue() != null) {
//                            if (node.getEnvVoxReadValue().equals("true")) {
//                                //if the envVoxReadValue is true continue as before.
//                                if (getDinnerTimeDevicesList(node).size() == 0) {
//                                    if (!isFinishing()) {
//                                        //there is no Vox devices avaliable
//                                        dynamicAlertDialogBox.showAlertDialog(LSSDPDeviceNetworkSettings.this, "No Genie dinner time devices found ", "", "Next",
//                                                new OnNextButtonClickFromAlertDialogInterface() {
//                                            @Override
//                                            public void onButtonClickFromAlertDialog() {
//                                            }
//                                        });
//                                    }
//                                }else{
//                                    gotoDinnerTimeSelectDevicesList(node);
//                                }
//                            } else {
//                                //envVoxReadValue is false then add alert dialog box
//                                Intent intent = new Intent(LSSDPDeviceNetworkSettings.this, VodafoneDinnerTimeMainSplashActivity.class);
//                                Bundle bundle = new Bundle();
//                                bundle.putSerializable("LSSDPNodes", (Serializable) node);
//                                bundle.putSerializable("deviceDetailsList", (Serializable) getDinnerTimeDevicesList(node));
//                                intent.putExtras(bundle);
//                                startActivity(intent);
//                            }
//                        } else {
//                            gotoDinnerTimeSelectDevicesList(node);
//                        }
//                    }
//                }, 4000);
//
//
//            }
//        });


        if (activityName.contains("ManageDevices")
                ||
                (mNode != null && !mNode.getNetworkMode().equalsIgnoreCase("WLAN")
                )) {
            btnNetworkSettings.setClickable(false);
            btnNetworkSettings.setVisibility(View.INVISIBLE);
            //networkSettings.setVisibility(View.GONE);
        }

        /*if(( mNode!=null &&( (!mNode.getNetworkMode().equalsIgnoreCase("WLAN"))
                || mNode.getgCastVerision()==null
        )
        )){
           btnOpenSettingsInCast.setVisibility(View.GONE);
            btnAdvanceSettings.setVisibility(View.GONE);
        }else*/
        {
            btnOpenSettingsInCast.setVisibility(View.INVISIBLE);
            btnAdvanceSettings.setVisibility(View.INVISIBLE);
        }

//        btnAlexaEnable = (LinearLayout) findViewById(R.id.btnAlexaLogin);
//        btnAlexaEnable.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//
//                if (mNode.getAlexaRefreshToken() == null || mNode.getAlexaRefreshToken().isEmpty()) {
//                    Intent newIntent = new Intent(LSSDPDeviceNetworkSettings.this, AlexaSignInActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                    newIntent.putExtra("speakerIpaddress", speakerIpaddress);
//                    newIntent.putExtra("deviceProvisionInfo", mDeviceProvisioningInfo);
//                    newIntent.putExtra("fromActivity",LSSDPDeviceNetworkSettings.class.getSimpleName());
//                    startActivity(newIntent);
////                    finish();
//                } else {
//
//                    Intent i = new Intent(LSSDPDeviceNetworkSettings.this, AlexaThingsToTryDoneActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                    i.putExtra("speakerIpaddress", speakerIpaddress);
//                    i.putExtra("fromActivity",LSSDPDeviceNetworkSettings.class.getSimpleName());
//                    startActivity(i);
////                    finish();
//                }
//            }
//        });

        btnAlexaContacts = (Button) findViewById(R.id.btnAlexaAddContact);
        btnAlexaContacts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(
                        new Intent(LSSDPDeviceNetworkSettings.this, AlexaContactsActivity.class)
                                .putExtra(Constants.CURRENT_DEVICE_IP, speakerIpaddress));
            }
        });


    }


    public List<ModelStoreDeviceDetails> getDinnerTimeDevicesList(LSSDPNodes node) {
        String voxJsonArray = null;
        if (node != null) {
            voxJsonArray = node.getVoxJsonArray();

            if (voxJsonArray != null) {

                if (voxJsonArray.contains("1::")) {
                    String removemessage = voxJsonArray.toString().replace("1::", "");
                    LibreLogger.d(this, "SUMA VOX New message appeared for the device  removing 1" + removemessage);
                    JSONObject mainObj = null;
                    try {
                        mainObj = new JSONObject(removemessage);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    try {
                        if (mainObj != null) {
                            JSONArray scanListArray = mainObj.getJSONArray("deviceList");

                            if (scanListArray.length() > 0) {
                                LibreLogger.d(this, "SUMA VOX New message appeared for the device scanlist array not equal to null");

                                modelStoreDeviceDetailsList = new ArrayList<>();
                                try {
                                    for (int i = 0; i < scanListArray.length(); i++) {
                                        JSONObject obj = (JSONObject) scanListArray.get(i);
                                        ModelStoreDeviceDetails modelStoreDeviceDetails =
                                                new ModelStoreDeviceDetails(obj.getString("isActive"),
                                                        obj.getBoolean("isBlackListed"),
                                                        obj.getString("macid"),
                                                        obj.getString("name"),"","");

                                        modelStoreDeviceDetailsList.add(modelStoreDeviceDetails);
                                        LibreLogger.d(this, "SUMA VOX New message appeared for the device  geting device name" + obj.getString("name"));

                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }

                            }
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return modelStoreDeviceDetailsList;
    }

//    public void gotoDinnerTimeSelectDevicesList(LSSDPNodes node) {
//        Intent intent = new Intent(LSSDPDeviceNetworkSettings.this, VodafoneSelectDinnerTimeDevicesListActivity.class);
//        Bundle bundle = new Bundle();
//        bundle.putSerializable("deviceDetailsList", (Serializable) getDinnerTimeDevicesList(node));
//        bundle.putSerializable("LSSDPNodes", (Serializable) node);
//        intent.putExtras(bundle);
//        startActivityForResult(intent, ACTIVITYREQUESTCODE);
//    }



    private void updateDeviceNameInDB(String deviceEndPointID, String deviceName) {
        if (AlexaLoginRepo.getRepo().isLinked(speakerIpaddress)) {
            Bundle bundle = new Bundle();
            bundle.putString(AlexaConstants.BUNDLE_ALEXA_USERNAME, AlexaLoginRepo.getRepo().getLinkedDeviceDetails().get(speakerIpaddress));
            bundle.putString(AlexaConstants.BUNDLE_ENDPOINT_ID, deviceEndPointID);
            bundle.putString(AlexaConstants.BUNDLE_FRIENDLY_NAME, deviceName);
            bundle.putInt(AlexaConstants.BUNDLE_HIT_COUNT, 0);

            LibreLogger.d(this, speakerIpaddress + " linked to " + AlexaLoginRepo.getRepo().getLinkedUsername(speakerIpaddress));
            DynamoDBHelper.getInstance().updateItem(bundle, new DynamoDBUpdationListener() {
                @Override
                public void itemUpdated(Bundle bundle) {
                    closeLoader();
                }

                @Override
                public void itemUpdationFailed(Bundle bundle) {
                    closeLoader();
                }
            });
        } else {
            closeLoader();
        }
    }

    private void sendLinkMessageToDevice(String speakerIpaddress, Bundle bundle) {
        LUCIControl luciControl = new LUCIControl(speakerIpaddress);
        String username = bundle.getString(AlexaConstants.BUNDLE_ALEXA_USERNAME);
        luciControl.SendCommand(MIDCONST.ALEXA_COMMAND, "LOGIN_3PDA_USER:" + username, CommandType.SET);
    }

    private void unlinkAlexaDevice(Bundle bundle) {
        showLoaderOnUI();
        LUCIControl luciControl = new LUCIControl((String) bundle.get(AlexaConstants.BUNDLE_IPADDRESS));
        luciControl.SendCommand(MIDCONST.ALEXA_COMMAND, "LOGOUT_3PDA_USER", CommandType.SET);
        bundle.putInt(AlexaConstants.BUNDLE_HIT_COUNT, 0);
        DynamoDBHelper.getInstance().deleteItem(bundle, new DynamoDBDeletionListener() {
            @Override
            public void itemDeleted(Bundle bundle) {
                AlexaLoginRepo.getRepo().getLinkedDeviceDetails().remove(bundle.getString(AlexaConstants.BUNDLE_IPADDRESS));
                closeLoaderOnUIAnUpdateLinkButton(false);
            }

            @Override
            public void itemDeletionFailed(Bundle bundle) {
                closeLoaderOnUIAnUpdateLinkButton(true);
            }
        });
    }

    private void showLoaderOnUI() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                showLoader();
            }
        });
    }

    private void setBtnLink() {
        if (AlexaLoginRepo.getRepo().getLinkedDeviceDetails().containsKey(speakerIpaddress)) {
            //device is linked
            btnLink.setText("unlink" + "(Linked to " + AlexaLoginRepo.getRepo().getLinkedDeviceDetails().get(speakerIpaddress) + ")");
        } else {
            btnLink.setText("link");
        }

    }

    private void closeLoaderOnUIAnUpdateLinkButton(final boolean hasAdded) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //setBtnLink();
                closeLoader();
            }
        });
    }

    private void linkAlexaDevice(Bundle bundle) {
        showLoaderOnUI();
        bundle.putInt(AlexaConstants.BUNDLE_HIT_COUNT, 0);
        DynamoDBHelper.getInstance().putItem(bundle, new DynamoDBAdditionListener() {
            @Override
            public void onItemAdded(Bundle bundle) {
                // item added to dynamo db
                AlexaLoginRepo.getRepo().putLinkedDeviceDetails(bundle.getString(AlexaConstants.BUNDLE_IPADDRESS), bundle.getString(AlexaConstants.BUNDLE_ALEXA_USERNAME));
                sendLinkMessageToDevice(speakerIpaddress, bundle);
                closeLoaderOnUIAnUpdateLinkButton(true);
            }

            @Override
            public void onItemAdditionFailed(Bundle bundle) {
                // item not added to dynamo db
                closeLoaderOnUIAnUpdateLinkButton(false);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case AlexaConstants.ALEXA_INTENT_FOR_RESULT: {
                btnLink.performClick();
            }
            case ACTIVITYREQUESTCODE: {
                readVOXControlValue(speakerIpaddress);
            }

            break;
        }
    }

    public String utf8truncate(String input, int length) {
        StringBuffer result = new StringBuffer(length);
        int resultlen = 0;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            int charlen = 0;
            if (c <= 0x7f) {
                charlen = 1;
            } else if (c <= 0x7ff) {
                charlen = 2;
            } else if (c <= 0xd7ff) {
                charlen = 3;
            } else if (c <= 0xdbff) {
                charlen = 4;
            } else if (c <= 0xdfff) {
                charlen = 0;
            } else if (c <= 0xffff) {
                charlen = 3;
            }
            if (resultlen + charlen > length) {
                break;
            }
            result.append(c);
            resultlen += charlen;
        }
        return result.toString();
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

    public void launchTheApp(String appPackageName, String deepLinkExtension) {

        Intent intent = getPackageManager().getLaunchIntentForPackage(appPackageName);
        if (intent != null) {
            LibreLogger.d(this, "Chrome cast app is available in phone. Deeplink-> DEVICE_SETTINGS");
          /*  intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);*/
            intent = new Intent(appPackageName + "." + deepLinkExtension);
            intent.putExtra(appPackageName + ".extra.IP_ADDRESS", speakerIpaddress);
            startActivity(intent);

        } else {
            LibreLogger.d(this, "Chrome cast app is not available in phone, redirecting to playstore. Deeplink-> DEVICE_SETTINGS");
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

    private void goNext() {
        LUCIControl luci_ = new LUCIControl(speakerIpaddress);
        if (mSpinnerChanged) {


            LSSDPNodeDB mNodeDB = LSSDPNodeDB.getInstance();
            LSSDPNodes node = mNodeDB.getTheNodeBasedOnTheIpAddress(speakerIpaddress);
            if (node == null)
                return;

            luci_.sendAsynchronousCommand();
            if (mAudioOutput.equals("Stereo (S)")) {
                luci_.SendCommand(MIDCONST.MID_SPEAKER_TYPE, "SETSTEREO", LSSDPCONST.LUCI_SET);
                node.setSpeakerType("0");
            } else if (mAudioOutput.equals("Left Speaker (L)")) {
                luci_.SendCommand(MIDCONST.MID_SPEAKER_TYPE, "SETLEFT", LSSDPCONST.LUCI_SET);
                node.setSpeakerType("1");
            } else if (mAudioOutput.equals("Right Speaker (R)")) {
                luci_.SendCommand(MIDCONST.MID_SPEAKER_TYPE, "SETRIGHT", LSSDPCONST.LUCI_SET);
                node.setSpeakerType("2");
            }


            mNodeDB.renewLSSDPNodeDataWithNewNode(node);

//                    StartLSSDPScan();


            finish();
        }

        if (!mAudioPreset.equalsIgnoreCase("")) {
            Log.d("AudioPresetValue", "Sending Data is - " + mAudioPreset);
            luci_.SendCommand(145, mAudioPreset, LSSDPCONST.LUCI_SET);
        }
    }


    @Override
    public void onBackPressed() {

        /* if the user is coming from nowplaying activity then we need to launch the ActiveScenesList */
        // super.onBackPressed();
        if (deviceName.isEnabled()) {
            new AlertDialog.Builder(LSSDPDeviceNetworkSettings.this)
                    .setTitle(getString(R.string.deviceNameChanging))
                    .setMessage(getString(R.string.deviceNameChangingMsg))
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            LSSDPDeviceNetworkSettings.super.onBackPressed();

                        }
                    }).setIcon(android.R.drawable.ic_dialog_alert)
                    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    })
                    .show();
        } else
            super.onBackPressed();

    }

    private void showLoader() {
        if (LSSDPDeviceNetworkSettings.this.isFinishing())
            return;
        if (m_progressDlg == null)
            m_progressDlg = ProgressDialog.show(LSSDPDeviceNetworkSettings.this, getString(R.string.notice), getString(R.string.loading), true, true, null);
        if (!m_progressDlg.isShowing()) {
            m_progressDlg.show();
        }
    }

    private void closeLoader() {
        if (m_progressDlg != null) {
            if (m_progressDlg.isShowing()) {
                m_progressDlg.dismiss();
            }
        }
    }

    public void UpdateLSSDPNodeDeviceName(String ipaddress, String mDeviceName) {

        LSSDPNodes mToBeUpdateNode = mScanHandler.getLSSDPNodeFromCentralDB(ipaddress);

        LSSDPNodeDB mNodeDB = LSSDPNodeDB.getInstance();
        if (mToBeUpdateNode != null) {
            mToBeUpdateNode.setFriendlyname(mDeviceName);
            mNodeDB.renewLSSDPNodeDataWithNewNode(mToBeUpdateNode);
        }

    }

    private void readVOXControlValue(String ip) {
        new LUCIControl(ip).SendCommand(257, "0", LSSDPCONST.LUCI_SET);
    }


    @Override
    protected void onStop() {
        super.onStop();
        closeLoader();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unRegisterForDeviceEvents();
        closeLoader();
    }

    public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {

        switch (position) {
            case 0:
                // Whatever you want to happen when the first item gets selected
                break;
            case 1:
                // Whatever you want to happen when the second item gets selected
                break;
            case 2:
                // Whatever you want to happen when the thrid item gets selected
                break;

        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_sac_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    public void deviceDiscoveryAfterClearingTheCacheStarted() {

    }

    @Override
    public void newDeviceFound(LSSDPNodes node) {

    }

    @Override
    public void deviceGotRemoved(String ipaddress) {
        if (ipaddress != null && speakerIpaddress != null && ipaddress.equals(speakerIpaddress)) {
            onBackPressed();
        }
    }

    @Override
    public void messageRecieved(NettyData dataRecived) {

        byte[] buffer = dataRecived.getMessage();
        String ipaddressRecieved = dataRecived.getRemotedeviceIp();

        LUCIPacket packet = new LUCIPacket(dataRecived.getMessage());
        Log.d("LedStatus", "Message recieved for ipaddress " + ipaddressRecieved + "command is " + packet.getCommand());

        if (speakerIpaddress.equalsIgnoreCase(ipaddressRecieved)) {
            switch (packet.getCommand()) {

                case 208:
                    String message = new String(packet.getpayload());
                    LibreLogger.d(this, "bhargav " + message);
                    String[] messageArray = message.split(":");

                    if (messageArray.length == 0 /*|| messageArray.length == 1*/)
                        return;
                    String audioPreset = messageArray[0];
                    String status = "";
                    if (messageArray.length > 1) {
                        status = messageArray[1];
                    }
                    switch (audioPreset) {
                        case "3pdauserid":
                            closeLoader();
                            showLinkButton();
                            if (status != null && !status.isEmpty()) {
                                AlexaLoginRepo.getRepo().putLinkedDeviceDetails(dataRecived.getRemotedeviceIp(), status);
                                closeLoaderOnUIAnUpdateLinkButton(true);
                            } else {
                                AlexaLoginRepo.getRepo().getLinkedDeviceDetails().remove(dataRecived.getRemotedeviceIp());
                                // setBtnLink();
                            }
                            break;
                        case "LEDControl":
                            if (status.equalsIgnoreCase("0")) {
                                ledHandler.sendEmptyMessage(LED_FAILURE);
                                return;
                            }
                            if (status.equalsIgnoreCase("1")) {
                                ledHandler.sendEmptyMessage(LED_SUCCESS);
                            }
                            break;
                        case "audiopreset":
                            if (!status.equalsIgnoreCase("0")) {
                                mPresetSpinner.setSelection(Integer.parseInt(status) - 1);
                                closeLoader();
                                LibreLogger.d(this, messageArray[1] + "bhargav" + mPresetSpinner.getSelectedItem());
                            }
                            break;
                    }

                case 17:
                    String message1 = new String(packet.getpayload());
                    LibreLogger.d(this, "SUMA VOX New message appeared for the device in 17 msg\n" + message1);
                    if (message1.contains("1::")) {
                        String removemessage = message1.toString().replace("1::", "");
                        LSSDPNodeDB mLssdpNodeDb = LSSDPNodeDB.getInstance();
                        final LSSDPNodes mNode = mLssdpNodeDb.getTheNodeBasedOnTheIpAddress(speakerIpaddress);
                        mNode.setVoxJsonArray(message1);
                        LibreLogger.d(this, "SUMA VOX New message appeared for the device  removing 1" + removemessage + "get json array\n" + mNode.getVoxJsonArray());
                        JSONObject mainObj = null;
                        try {
                            mainObj = new JSONObject(removemessage);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        try {
                            if (mainObj != null) {
                                JSONArray scanListArray = mainObj.getJSONArray("deviceList");
                                //LibreLogger.d(this, "SUMA VOX New message appeared for the device getting json format" + scanListArray.getString(0));
                                for (int i = 0; i < scanListArray.length(); i++) {
                                    JSONObject obj = (JSONObject) scanListArray.get(i);
                                    if (obj.getString("name") == null
                                        /* || (obj.getString("friendlyName").isEmpty())*/) {
                                        continue;
                                    }
                                    LibreLogger.d(this, "SUMA VOX New message appeared for the device  geting device name" + obj.getString("name"));

                                }
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    break;
                case MIDCONST.ALEXA_COMMAND:
                    //got endpoints
                    String endPointDetails = new String(packet.getpayload());
                    LibreLogger.d(this, "bhargav 233 " + endPointDetails);
                    try {
                        JSONObject jsonObject = new JSONObject(endPointDetails);
                        String messageTitle = jsonObject.getString("Title");
                        if (messageTitle.equalsIgnoreCase("ENDPOINT_INFO")) {
                            jsonObject = jsonObject.getJSONObject("Window CONTENTS");
                            String endPointId = jsonObject.getString("ENDPOINT_ID");
                            LibreLogger.d(this, "endpoint ID is " + endPointId);

                            if (endPointId == null) {
                                setDeviceEndPointID("NA");
                            } else if (endPointId.isEmpty()) {
                                setDeviceEndPointID("NA");
                            } else {
                                setDeviceEndPointID(endPointId);
                            }
                            closeLoader();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        // closeLoader();
                    }

                    break;

                case 91:
                    String macID = new String(packet.getpayload());
                    LibreLogger.d(this, "bhargav  mac is id " + macID);
                    break;
                case 145:
                    String audioMessage = new String(packet.getpayload());
                    Log.d("AudioPresetValue", "Received Data is - " + audioMessage);
                    break;

//                case MIDCONST.ALEXA_COMMAND: {
//                    String alexaMessage = new String(packet.getpayload());
//                    LibreLogger.d(this, "Alexa Value From 234  " + alexaMessage);
//                    try {
//                        JSONObject jsonRootObject = new JSONObject(alexaMessage);
//                        // JSONArray jsonArray = jsonRootObject.optJSONArray("Window CONTENTS");
//                        JSONObject jsonObject = jsonRootObject.getJSONObject(LUCIMESSAGES.TAG_WINDOW_CONTENT);
//                        productId = jsonObject.optString("PRODUCT_ID").toString();
//                        dsn = jsonObject.optString("DSN").toString();
//                        sessionId = jsonObject.optString("SESSION_ID").toString();
//                        codeChallenge = jsonObject.optString("CODE_CHALLENGE").toString();
//                        codeChallengeMethod = jsonObject.optString("CODE_CHALLENGE_METHOD").toString();
//                        locale = jsonObject.optString("LOCALE").toString();
//                        mDeviceProvisioningInfo = new DeviceProvisioningInfo(productId, dsn, sessionId, codeChallenge, codeChallengeMethod,locale);
//
//                        if (mDeviceProvisioningInfo != null) {
//                            btnAlexaEnable.setVisibility(View.VISIBLE);
//                        }
//                    } catch (JSONException e) {
//                        e.printStackTrace();
//                        LibreLogger.d(this,""+e.getMessage());
//                    }
//
//
//                }
//                break;

            }

        }

    }

    private boolean googleTosCheckIsSuccess() {
        SharedPreferences sharedPreferences = getApplicationContext()
                .getSharedPreferences(Constants.SHOWN_GOOGLE_TOS, Context.MODE_PRIVATE);
        String shownGoogle = sharedPreferences.getString(Constants.SHOWN_GOOGLE_TOS, null);
        if (shownGoogle == null) {

            showGoogleTosDialog(this);
            return false;
        }
        return true;
    }

    public void showGoogleTosDialog(final Context context) {


        LayoutInflater factory = LayoutInflater.from(this);
        final View gcastDialog = factory.inflate(
                R.layout.gcast_alert_dialog, null);
        TextView textView = (TextView) gcastDialog.findViewById(R.id.google_text);
        textView.setText(clickableString(getString((R.string.google_tos))));
        textView.setAutoLinkMask(RESULT_OK);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
        textView.setGravity(Gravity.LEFT);
        Linkify.addLinks(clickableString(getString((R.string.google_tos))), Linkify.WEB_URLS);

        final AlertDialog.Builder builder = new AlertDialog.Builder(LSSDPDeviceNetworkSettings.this);
        builder.setTitle(getString(R.string.googleTerms))
                .setCancelable(false)
                .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {


                    }
                })
                .setPositiveButton(getString(R.string.accept), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {

                                SharedPreferences sharedpreferences = getApplicationContext()
                                        .getSharedPreferences(Constants.SHOWN_GOOGLE_TOS, Context.MODE_PRIVATE);
                                SharedPreferences.Editor editor = sharedpreferences.edit();
                                editor.putString(Constants.SHOWN_GOOGLE_TOS, "Yes");
                                editor.commit();


                                LSSDPNodes node = LSSDPNodeDB.getInstance().getTheNodeBasedOnTheIpAddress(speakerIpaddress);
                                LUCIControl control = new LUCIControl(speakerIpaddress);
                                control.sendGoogleTOSAcceptanceIfRequired(LSSDPDeviceNetworkSettings.this, node.getgCastVerision());

                            }
                        }).start();

                        Toast.makeText(LSSDPDeviceNetworkSettings.this, getString(R.string.googleCastAccess), Toast.LENGTH_LONG).show();

                    }

                });

        AlertDialog alert = builder.create();
        alert.setView(gcastDialog);
        alert.show();


    }

    private SpannableString clickableString(String sampleString) {
        SpannableString spannableString = new SpannableString(sampleString);
        ClickableSpan googleTermsClicked = new ClickableSpan() {
            @Override
            public void onClick(View textView) {

                Intent intent = new Intent(getApplicationContext(), LibreWebViewActivity.class);
                intent.putExtra("url", "http://www.google.com/intl/en/policies/privacy/");
                startActivity(intent);
/*
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setData(Uri.parse("http://www.google.com/intl/en/policies/privacy/"));
                startActivity(intent);
*/


            }
        };


        ClickableSpan termsClicked = new ClickableSpan() {
            @Override
            public void onClick(View textView) {

                Intent intent = new Intent(getApplicationContext(), LibreWebViewActivity.class);
                intent.putExtra("url", "https://www.google.com/intl/en/policies/terms/");
                startActivity(intent);

/*
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setData(Uri.parse("https://www.google.com/intl/en/policies/terms/"));
                startActivity(intent);
*/
            }
        };

        ClickableSpan privacyClicked = new ClickableSpan() {
            @Override
            public void onClick(View textView) {

                Intent intent = new Intent(getApplicationContext(), LibreWebViewActivity.class);
                intent.putExtra("url", "http://www.google.com/intl/en/policies/privacy/");
                startActivity(intent);

/*
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setData(Uri.parse("https://www.google.com/intl/en/policies/privacy/"));
                startActivity(intent);
*/
            }
        };

        spannableString.setSpan(googleTermsClicked, 4, 25, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannableString.setSpan(termsClicked, 395, 418, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannableString.setSpan(privacyClicked, 422, 444, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        spannableString.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.brand_orange)), 4, 25,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannableString.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.brand_orange)), 395, 418,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannableString.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.brand_orange)), 422, 444,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        return spannableString;
    }

    public boolean isLEDEnabled(LSSDPNodes nodes) {
        if (nodes == null) {
            return false;
        }
        LibreLogger.d(this, "208 message LED value is" + nodes.getLEDControl());
        if (nodes.getLEDControl().equals("1")) {
            return true;
        }
        return false;
    }
}
