package com.libre.alexa.activities.newFlow

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.airbnb.lottie.LottieDrawable
import com.libre.alexa.DeviceDiscoveryActivity
import com.libre.alexa.LibreApplication
import com.libre.alexa.R
import com.libre.alexa.constants.LSSDPCONST
import com.libre.alexa.constants.MIDCONST
import com.libre.alexa.constants.MIDCONST.GENIE_DEVICE_STATUS
import com.libre.alexa.luci.LSSDPNodeDB
import com.libre.alexa.luci.LSSDPNodes
import com.libre.alexa.luci.LUCIControl
import com.libre.alexa.luci.LUCIPacket
import com.libre.alexa.netty.BusProvider
import com.libre.alexa.netty.LibreDeviceInteractionListner
import com.libre.alexa.netty.NettyData
import com.libre.alexa.util.LibreLogger
import com.libre.alexa.utils.UtilClass
import com.libre.alexa.utils.interfaces.ApiSucessCallbackInterface
import com.libre.alexa.utils.interfaces.SucessCallbackInterfaceWithParams
import com.libre.alexa.viewmodels.ApisViewModel
import com.squareup.otto.Subscribe
import com.vodafone.idtmlib.AccessToken
import kotlinx.android.synthetic.main.activity_vodafone_dinner_time_wired_pairing.*
import kotlinx.android.synthetic.main.custom_vdf_animation_view.*
import org.json.JSONObject

class VodafoneDinnerTimeWiredPairingActivity : DeviceDiscoveryActivity(),
    LibreDeviceInteractionListner {

    var bundle = Bundle()

    var node: LSSDPNodes? = null

    var envVoxReadValue = ""



    val TAG = VodafoneDinnerTimeWiredPairingActivity::class.java.simpleName


    lateinit var apiViewModel: ApisViewModel


    val utilClass = UtilClass()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vodafone_dinner_time_wired_pairing)


        apiViewModel =
            ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory(application)).get(
                ApisViewModel::class.java
            )

        bundle = intent.extras!!
        if (bundle != null) {
            node = bundle.getSerializable("deviceDetails") as LSSDPNodes
        }




        readEnvVoxAlertOne(node?.ip)

        btnNext.setOnClickListener {
            if (node!!.envVoxReadValue != "true") {
                //false/ empry
                showLotteAnimationView()
                readEnvVoxAlertTwo(node!!.ip)
                Handler().postDelayed({
                    readEnvVox(node!!.ip)
                    Handler().postDelayed({
                        if (node!!.envVoxReadValue != null) {
                            if (node!!.envVoxReadValue == "true") {
                                checkAlexaStatus(node!!)
                            } else {
                                stopLotteAnimationView()
                                utilClass.buildSnackBarWithoutButton(
                                    this,
                                    window.decorView.findViewById(android.R.id.content),
                                    "Please Try Again"
                                )
                                readEnvVoxAlertOne(node?.ip)
                            }
                        } else {
                            stopLotteAnimationView()
                            utilClass.buildSnackBarWithoutButton(
                                this,
                                window.decorView.findViewById(android.R.id.content),
                                "Techinical error"
                            )
                        }
                    }, 2000)
                }, 3000)
            } else {
                //true
                checkAlexaStatus(node!!)
            }
        }
    }

    var busEventListener: Any = object : Any() {
        @Subscribe
        fun newMessageRecieved(nettyData: NettyData) {
            val packet = LUCIPacket(nettyData.getMessage())
            val payloadValueString = String(packet.getpayload())
            val mNode = LSSDPNodeDB.getInstance()
                .getTheNodeBasedOnTheIpAddress(nettyData.getRemotedeviceIp())
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
        stopLotteAnimationView()
        val intent = Intent(
            this@VodafoneDinnerTimeWiredPairingActivity,
            VodafoneDinnerTimePairedActivity::class.java
        )
        val bundle = Bundle()
        bundle.putSerializable("deviceDetails", node)
        intent.putExtras(bundle)
        startActivity(intent)
        finish()
        overridePendingTransition(
            R.anim.left_to_right_anim_tranistion,
            R.anim.right_to_left_anim_transition
        );
    }


    fun gotoDinnerTimeDeviceActivity() {
        stopLotteAnimationView()
        val intent = Intent(
            this@VodafoneDinnerTimeWiredPairingActivity,
            VodafoneAlexaDinnerTimeDeviceListActivity::class.java
        )
        val bundle = Bundle()
        bundle.putSerializable("deviceDetails", node)
        intent.putExtras(bundle)
        startActivity(intent)
        finish()
        overridePendingTransition(
            R.anim.left_to_right_anim_tranistion,
            R.anim.right_to_left_anim_transition
        );
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
        registerForDeviceEvents(this)
    }


    fun readEnvVoxAlertTwo(speakerIpaddress: String?) {
        LUCIControl(speakerIpaddress).SendCommand(GENIE_DEVICE_STATUS, "6", LSSDPCONST.LUCI_SET)
        Log.d(TAG, "sentGenieStatus 6")
    }

    fun readEnvVoxAlertOne(speakerIpaddress: String?) {
        LUCIControl(speakerIpaddress).SendCommand(GENIE_DEVICE_STATUS, "5", LSSDPCONST.LUCI_SET)
        Log.d(TAG, "sentGenieStatus 5")
    }


    fun checkAlexaStatus(node: LSSDPNodes) {
        val luciControl = LUCIControl(node?.ip)
        luciControl.SendCommand(
            MIDCONST.CHECK_ALEXA_LOGIN_STATUS,
            "GETLOGINSTAT",
            LSSDPCONST.LUCI_SET
        )
    }

    override fun newDeviceFound(node: LSSDPNodes?) {

    }

    override fun deviceGotRemoved(ipaddress: String?) {

    }


    fun showLotteAnimationView() {
        tvLoadingReason.visibility = View.VISIBLE
        tvLoadingReason.text = "Pairing your Super Wifi Plus device with app"
        llAlmostThere.visibility = View.GONE
        if (lotteAnimationView != null) {
            lotteAnimationView.visibility = View.VISIBLE
            lotteAnimationView.setAnimation("vf_spinner_red_large_300.json")
            lotteAnimationView.repeatCount = LottieDrawable.INFINITE
            lotteAnimationView.tag = "lotteAnimation"
            lotteAnimationView.playAnimation()
        }
    }

    fun stopLotteAnimationView() {
        if (lotteAnimationView != null) {
            tvLoadingReason.visibility = View.GONE
            lotteAnimationView.clearAnimation();
            lotteAnimationView.visibility = View.GONE
            llAlmostThere.visibility = View.VISIBLE
        }
    }


    override fun messageRecieved(nettyData: NettyData?) {
        LibreLogger.d(
            this,
            "New message appeared for the device " + nettyData!!.getRemotedeviceIp()
        )

        val packet = LUCIPacket(nettyData!!.getMessage())
        when (packet.command) {
            MIDCONST.ALEXA_SKILL_STATUS -> {
                val payload: String = packet.getpayload().toString(Charsets.UTF_8)
                Log.d(TAG, "receivedSkillPayloadMessage: $payload")
                when (payload) {
                    "ENABLE_SUCCESS" -> {
                        runOnUiThread {
                            /** The skill is enabled sucessfully goto the dinner time devices */

                            utilClass.buildSnackBarWithoutButton(
                                this@VodafoneDinnerTimeWiredPairingActivity,
                                window.decorView.findViewById(android.R.id.content),
                                "Alexa Skill is successfully enabled"
                            )
                        }
                        updateAlexaEnablementStatus(true)
                        gotoDinnerTimeDeviceActivity()
                    }

                    "ENABLE_RETRY" -> {
                        AuthenticateThread().start()
                    }

                    "ENABLE_ERROR" -> {
                        runOnUiThread {
                            utilClass.buildSnackBarWithoutButton(
                                this, window.decorView.findViewById(android.R.id.content),
                                "There seems to be an error while enabling the alexa Skill"
                            )
                        }
                        /** since we are just showing the user there is an error while enabling the skill
                        but not letting the user enable the skill again from the app */
                        updateAlexaEnablementStatus(true)
                        gotoDinnerTimeDeviceActivity()
                    }
                }
            }
            MIDCONST.CHECK_ALEXA_LOGIN_STATUS -> {
                val payload: String = packet.getpayload().toString(Charsets.UTF_8)
                Log.d(TAG, "receivedSkillPayloadMessage: $payload")
                when (payload) {
                    "NOLOGIN" -> {
                        //alexa login
                        if (node != null) {
                            if (node!!.envVoxReadValue == "true") {
                                gotoSwfPairedActivity()
                            }
                        }
                    }
                    "LOGGEDIN", "READY" -> {
                        if (node != null) {
                            if (node!!.envVoxReadValue == "true") {
                                tvLoadingReason.text = "Registering your Super WiFi Plus device with Amazon"
                                AuthenticateThread().start()
                            }
                        }
                    }
                }
            }
        }
    }


    fun updateAlexaEnablementStatus(status: Boolean) {
        val sharedPreferences = getSharedPreferences("Libre", Context.MODE_PRIVATE)
        val editor: SharedPreferences.Editor = sharedPreferences.edit()
        editor.putBoolean("alexaSkillEnablement", status)
        editor.apply()
    }

    override fun deviceDiscoveryAfterClearingTheCacheStarted() {
    }


    private inner class AuthenticateThread : Thread() {
        override fun run() {
            try {
                val accessToken: AccessToken =
                    LibreApplication.getIdtmLib(this@VodafoneDinnerTimeWiredPairingActivity)
                        .authenticate(
                            true, getSharedPreferences("Libre", Context.MODE_PRIVATE)
                                .getString("accessToken", null)
                        )

                apiViewModel.updateAccessToken(
                    accessToken.token,
                    this@VodafoneDinnerTimeWiredPairingActivity
                )

                Log.d(TAG, "accessToken+LatestToken ${accessToken.token}")

                getShareCode()

            } catch (e: Exception) {
                Log.d(TAG, "authenticate sync onError", e)
                runOnUiThread(Runnable {
                    Log.d(TAG, "Access token error: $e")
                })
            }
        }
    }


    fun getShareCode() {
        /** start calling the sharecode api */
        utilClass.getShareCodeForAlexaSkillActivation(getSharedPreferences(
            "Libre",
            Context.MODE_PRIVATE
        ).getString("accessToken", ""),
            this@VodafoneDinnerTimeWiredPairingActivity,
            object : ApiSucessCallbackInterface {
                override fun onApiSucess(isSucess: Boolean) {
                    getAlexaAuthCode()
                }
            })
    }


    fun getAlexaAuthCode() {
        /** then the auth code for  skill activation */
        utilClass.getAuthCodeForAlexaSkillActivation(
            this@VodafoneDinnerTimeWiredPairingActivity,
            getSharedPreferences(
                "Libre",
                Context.MODE_PRIVATE
            ).getString("sharingCode", ""),
            LibreApplication.IDGATEWAY_REDIRECT_URL,
            object : SucessCallbackInterfaceWithParams {
                override fun sucessCallbackWithParameter(authURl: String) {
                    val url = Uri.parse(authURl)
                    Log.d(TAG, "alexaAuthUrl: ".plus(url))
                    val alexaSkillAuthCode = url.getQueryParameter("code")
                    if (alexaSkillAuthCode != null) {
                        val shardPref = getSharedPreferences("Libre", Context.MODE_PRIVATE)
                        val editor = shardPref.edit()
                        editor.putString("alexaAuthCode", alexaSkillAuthCode)
                        editor.apply()
                        Log.d(TAG, "atul_alexaSkillAuthCode ".plus(alexaSkillAuthCode))
                        enableAlexaSkill(
                            node!!.ip,
                            MIDCONST.ALEXA_SKILL_STATUS,
                            "ENABLE::".plus(buildActivateSkillJsonData(alexaSkillAuthCode).toString())
                        )
                    }
                }
            })
    }


    fun enableAlexaSkill(ip: String, command: Int, activateSkillJsonData: String) {
        LUCIControl(ip).SendCommand(command, activateSkillJsonData, LSSDPCONST.LUCI_SET)
        Log.d(TAG, "sentSkillEnablement ".plus(activateSkillJsonData.toString()))
    }

    fun buildActivateSkillJsonData(authCode: String): JSONObject {
        val jsonObject = JSONObject()
        val skillId = "amzn1.ask.skill.2bf3853d-ecfc-460e-b2cd-6c239e003340"

        jsonObject.put("baseUrl", "https://api.amazonalexa.com")
        jsonObject.put("endpointPath", "/v1/alexaApiEndpoint")
        jsonObject.put("skillPath", "/v0/skills/$skillId/enablement")
        jsonObject.put("skillId", skillId)
        jsonObject.put("stage", "development")
        jsonObject.put("authCode", authCode)
        jsonObject.put("redirectUri", "https://cloud.librewireless.com/oauth/redirect/")

        return jsonObject
    }


}