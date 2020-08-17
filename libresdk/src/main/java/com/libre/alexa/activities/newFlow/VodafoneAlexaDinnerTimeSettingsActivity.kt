package com.libre.alexa.activities.newFlow

import android.app.ProgressDialog
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.libre.alexa.DeviceDiscoveryActivity
import com.libre.alexa.R
import com.libre.alexa.constants.LSSDPCONST
import com.libre.alexa.constants.MIDCONST
import com.libre.alexa.luci.LSSDPNodes
import com.libre.alexa.luci.LUCIControl
import com.libre.alexa.luci.LUCIPacket
import com.libre.alexa.luci.Utils
import com.libre.alexa.netty.LibreDeviceInteractionListner
import com.libre.alexa.netty.NettyData
import com.libre.alexa.util.LibreLogger
import com.libre.alexa.utils.UtilClass
import kotlinx.android.synthetic.main.activity_vodafone_alexa_dinner_time_settings.*
import kotlinx.android.synthetic.main.toolbar_white_bg_with_settings.*
import org.json.JSONObject

class VodafoneAlexaDinnerTimeSettingsActivity : DeviceDiscoveryActivity(), LibreDeviceInteractionListner {

    var bundle = Bundle()

    var node: LSSDPNodes? = null

    var TAG = VodafoneAlexaDinnerTimeSettingsActivity::class.java.simpleName

    var progressDialog: ProgressDialog? = null

    var utilClass = UtilClass()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vodafone_alexa_dinner_time_settings)

        disableNetworkChangeCallBack()

        bundle = intent.extras!!
        if (bundle != null) {
            node = bundle.getSerializable("deviceDetails") as LSSDPNodes
        }

        llBack.setOnClickListener {
            onBackPressed()
        }

        registerForDeviceEvents(this)


        if (node?.ip != null) {
            showLoader()
            alexaSkillStatus(node!!.ip,
                    MIDCONST.ALEXA_SKILL_STATUS,
                    "STATUS::".plus(buildAlexaSkillStatusJsonData().toString()))
            checkAlexaStatus()

        }

        createNewProgressDialog()

        etDeviceName.setText(node?.friendlyname)

        //firmware version
        tvSystemFw.text = node?.version

        tvMacAddress.text = buildMacAddressFormatToDisplay(node?.usn)


    }


    fun checkAlexaStatus() {
        val luciControl = LUCIControl(node?.ip)
        luciControl.SendCommand(MIDCONST.CHECK_ALEXA_LOGIN_STATUS, "GETLOGINSTAT", LSSDPCONST.LUCI_SET)
    }


    fun alexaSkillStatus(ip: String, command: Int, activateSkillJsonData: String) {
        LUCIControl(ip).SendCommand(command, activateSkillJsonData, LSSDPCONST.LUCI_SET)
        Log.d(TAG, "sentSkillStatus ".plus(activateSkillJsonData.toString()))
    }

    private fun buildMacAddressFormatToDisplay(macAddressString: String?): String {
        if (node?.usn != null) {
            return macAddressString!!.replace("..(?!$)", "$0:")
        }
        return ""
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(0,
                R.anim.right_to_left_anim_transition);
    }

    fun showLoader() {
        progressDialog?.show()
    }

    fun dismissLoader() {
        progressDialog?.dismiss()
    }

    fun buildAlexaSkillStatusJsonData(): JSONObject {
        val jsonObject = JSONObject()
        val skillId = "amzn1.ask.skill.2bf3853d-ecfc-460e-b2cd-6c239e003340"

        jsonObject.put("baseUrl", "https://api.amazonalexa.com")
        jsonObject.put("endpointPath", "/v1/alexaApiEndpoint")
        jsonObject.put("skillPath", "/v0/skills/$skillId/enablement")


        return jsonObject
    }

    fun createNewProgressDialog() {
        progressDialog = ProgressDialog(this@VodafoneAlexaDinnerTimeSettingsActivity)
        progressDialog?.setCancelable(false)
        progressDialog?.setMessage("Please Wait...")
    }

    override fun newDeviceFound(node: LSSDPNodes?) {

    }

    override fun deviceGotRemoved(ipaddress: String?) {

    }

    override fun messageRecieved(packet: NettyData?) {
        LibreLogger.d(this, "New message appeared for the device " + packet!!.getRemotedeviceIp())
        val messagePacket = LUCIPacket(packet!!.getMessage())

        when (messagePacket.command) {
            MIDCONST.ALEXA_SKILL_STATUS -> {
                val payload: String = messagePacket.getpayload().toString(Charsets.UTF_8)
                Log.d(TAG, "receivedSkillPayloadMessage: $payload")

                when (payload) {
                    "STATUS_ENABLED" -> {
                        dismissLoader()
                        tvFamilySkillStatus.text = "ENABLED"
                    }

                    "STATUS_DISABLED" -> {
                        dismissLoader()
                        tvFamilySkillStatus.text = "DISABLED"
                    }

                    "STATUS_RETRY" -> {
                        alexaSkillStatus(node!!.ip,
                                MIDCONST.ALEXA_SKILL_STATUS,
                                "STATUS::".plus(buildAlexaSkillStatusJsonData().toString()))
                    }
                    "STATUS_ERROR" -> {
                        dismissLoader()
                        tvFamilySkillStatus.text = ""
                        utilClass.buildSnackBarWithoutButton(this, window.decorView.findViewById(android.R.id.content),
                                "There seems to be an error while checking  the alexa Skill status")
                    }
                    else -> {
                        dismissLoader()
                    }
                }
            }
            MIDCONST.CHECK_ALEXA_LOGIN_STATUS -> {
                val payload: String = messagePacket.getpayload().toString(Charsets.UTF_8)
                Log.d(TAG, "receivedSkillPayloadMessage: $payload")
                when (payload) {
                    "LOGGEDIN","READY" -> {
                        runOnUiThread {
                            tvAmazonLoginStatus.text = "Logged-In"
                        }
                    }

                    "NOLOGIN" -> {
                        runOnUiThread {
                            tvAmazonLoginStatus.text = "Logged-out"
                        }
                    }
                }
            }
        }
    }

    override fun deviceDiscoveryAfterClearingTheCacheStarted() {

    }

}