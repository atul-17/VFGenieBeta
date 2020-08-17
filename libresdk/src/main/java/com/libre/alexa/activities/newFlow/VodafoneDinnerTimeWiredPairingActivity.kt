package com.libre.alexa.activities.newFlow

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.libre.alexa.R
import com.libre.alexa.constants.LSSDPCONST
import com.libre.alexa.constants.MIDCONST
import com.libre.alexa.constants.MIDCONST.GENIE_DEVICE_STATUS
import com.libre.alexa.luci.LSSDPNodeDB
import com.libre.alexa.luci.LSSDPNodes
import com.libre.alexa.luci.LUCIControl
import com.libre.alexa.luci.LUCIPacket
import com.libre.alexa.netty.BusProvider
import com.libre.alexa.netty.NettyData
import com.libre.alexa.utils.UtilClass
import com.squareup.otto.Subscribe
import kotlinx.android.synthetic.main.activity_vodafone_dinner_time_wired_pairing.*

class VodafoneDinnerTimeWiredPairingActivity : AppCompatActivity() {

    var bundle = Bundle()

    var node: LSSDPNodes? = null

    var envVoxReadValue = ""

    var mProgressDialog: ProgressDialog? = null

    val TAG = VodafoneDinnerTimeWiredPairingActivity::class.java.simpleName

    val utilClass = UtilClass()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vodafone_dinner_time_wired_pairing)


        bundle = intent.extras!!
        if (bundle != null) {
            node = bundle.getSerializable("deviceDetails") as LSSDPNodes
        }


        mProgressDialog = ProgressDialog(this@VodafoneDinnerTimeWiredPairingActivity)
        mProgressDialog!!.setMessage("Please wait...")
        mProgressDialog!!.setCancelable(false)

        readEnvVoxAlertOne(node?.ip)

        btnNext.setOnClickListener {
            if (node!!.envVoxReadValue != "true") {
                //false/ empry
                mProgressDialog!!.show()
                readEnvVoxAlertTwo(node!!.ip)
                Handler().postDelayed({
                    readEnvVox(node!!.ip)
                    Handler().postDelayed({
                        mProgressDialog!!.dismiss()
                        if (node!!.envVoxReadValue != null) {
                            if (node!!.envVoxReadValue == "true") {
                                gotoSwfPairedActivity()
                            } else {
                                utilClass.buildSnackBarWithoutButton(this, window.decorView.findViewById(android.R.id.content), "Please Try Again")
                                readEnvVoxAlertOne(node?.ip)
                            }
                        } else {
                            utilClass.buildSnackBarWithoutButton(this, window.decorView.findViewById(android.R.id.content), "Techinical error")
                        }
                    }, 2000)
                }, 3000)
            } else {
                //true
                gotoSwfPairedActivity()
            }
        }
    }

    var busEventListener: Any = object : Any() {
        @Subscribe
        fun newMessageRecieved(nettyData: NettyData) {
            val packet = LUCIPacket(nettyData.getMessage())
            val payloadValueString = String(packet.getpayload())
            val mNode = LSSDPNodeDB.getInstance().getTheNodeBasedOnTheIpAddress(nettyData.getRemotedeviceIp())
            node = mNode
            //RouterCertPaired:false
            Log.d(TAG, "envValue$payloadValueString");
            if (payloadValueString != null) {
                if (packet.command == 208 && !payloadValueString.isEmpty()) {
                    if (payloadValueString.contains("RouterCertPaired")) {
                        val parts = payloadValueString.split(":".toRegex()).toTypedArray()
                        envVoxReadValue = parts[1]
                        Log.d(TAG, envVoxReadValue);
                        mNode.envVoxReadValue = envVoxReadValue
                    }
                }
            }
        }
    }

    fun gotoSwfPairedActivity() {
        val intent = Intent(this@VodafoneDinnerTimeWiredPairingActivity, VodafoneDinnerTimePairedActivity::class.java)
        val bundle = Bundle()
        bundle.putSerializable("deviceDetails", node)
        intent.putExtras(bundle)
        startActivity(intent)
        finish()
        overridePendingTransition(R.anim.left_to_right_anim_tranistion,
                R.anim.right_to_left_anim_transition);
    }

    override fun onStart() {
        super.onStart()
        BusProvider.getInstance().register(busEventListener)
    }

    override fun onStop() {
        super.onStop()
        BusProvider.getInstance().unregister(busEventListener)
    }

    override fun onResume() {
        super.onResume()
    }


    fun readEnvVoxAlertTwo(speakerIpaddress: String?) {
        LUCIControl(speakerIpaddress).SendCommand(GENIE_DEVICE_STATUS, "6", LSSDPCONST.LUCI_SET)
        Log.d(TAG, "sentGenieStatus 6")
    }

    fun readEnvVoxAlertOne(speakerIpaddress: String?) {
        LUCIControl(speakerIpaddress).SendCommand(GENIE_DEVICE_STATUS, "5", LSSDPCONST.LUCI_SET)
        Log.d(TAG, "sentGenieStatus 5")
    }

    fun readEnvVox(speakerIpaddress: String?) {
        LUCIControl(speakerIpaddress).SendCommand(MIDCONST.MID_ENV_READ, "READ_RouterCertPaired", LSSDPCONST.LUCI_SET)
        Log.d(TAG, "sentRouterStatus 208")
    }
}