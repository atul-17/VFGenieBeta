package com.libre.alexa.activities.newFlow

import android.app.ProgressDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.lifecycle.ViewModelProvider
import com.airbnb.lottie.LottieDrawable
import com.libre.alexa.DeviceDiscoveryActivity
import com.libre.alexa.LibreApplication
import com.libre.alexa.R
import com.libre.alexa.constants.LSSDPCONST
import com.libre.alexa.constants.MIDCONST
import com.libre.alexa.luci.LSSDPNodes
import com.libre.alexa.luci.LUCIControl
import com.libre.alexa.luci.LUCIPacket
import com.libre.alexa.netty.LibreDeviceInteractionListner
import com.libre.alexa.netty.NettyData
import com.libre.alexa.util.LibreLogger
import com.libre.alexa.utils.UtilClass
import com.libre.alexa.utils.interfaces.ApiSucessCallbackInterface
import com.libre.alexa.utils.interfaces.ONButtonClickFromCustomAlertDialog
import com.libre.alexa.utils.interfaces.SucessCallbackInterfaceWithParams
import com.libre.alexa.viewmodels.ApisViewModel
import com.vodafone.idtmlib.AccessToken
import kotlinx.android.synthetic.main.activity_alexa_skill_enablement_success.*
import kotlinx.android.synthetic.main.activity_vodafone_dinner_time_paired.*
import kotlinx.android.synthetic.main.custom_vdf_animation_view.*
import kotlinx.android.synthetic.main.toolbar_custom_white_bg.*
import org.json.JSONObject

class VodafoneAlexaSkillEnablementSucessActivity : DeviceDiscoveryActivity(),
    LibreDeviceInteractionListner {

    var authCode: String = ""

    var bundle = Bundle()

    lateinit var apiViewModel: ApisViewModel

    val TAG = VodafoneAlexaSkillEnablementSucessActivity::class.java.simpleName

    val utilClass = UtilClass()


    var deviceIpAddress: String = ""

    var node: LSSDPNodes? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alexa_skill_enablement_success)

        disableNetworkChangeCallBack()

        apiViewModel =
            ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory(application)).get(
                ApisViewModel::class.java
            )

        bundle = intent.extras!!

        if (bundle != null) {
            node = bundle.getSerializable("deviceDetails") as LSSDPNodes
        }

        if (node != null) {
            deviceIpAddress = node!!.ip
        }


        btnNext.setOnClickListener {
            gotoSettingUpFamilyRoutineActivity()
        }


        showLotteAnimationView()
        AuthenticateThread().start()

    }


    fun showLotteAnimationView() {
        if (lotteAnimationView != null) {
            llCongrats.visibility = View.GONE

            tvLoadingReason.text = "Registering your Super WiFi Plus device with Amazon"
            tvLoadingReason.visibility = View.VISIBLE
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
            llCongrats.visibility = View.VISIBLE
        }
    }


    override fun onResume() {
        super.onResume()
        registerForDeviceEvents(this)
    }


    private inner class AuthenticateThread : Thread() {
        override fun run() {
            try {
                val accessToken: AccessToken =
                    LibreApplication.getIdtmLib(this@VodafoneAlexaSkillEnablementSucessActivity)
                        .authenticate(
                            true, getSharedPreferences("Libre", Context.MODE_PRIVATE)
                                .getString("accessToken", null)
                        )

                apiViewModel.updateAccessToken(
                    accessToken.token,
                    this@VodafoneAlexaSkillEnablementSucessActivity
                )

                Log.d(TAG, "accessToken+LatestToken ${accessToken.token}")

                getShareCode()

            } catch (e: Exception) {
                stopLotteAnimationView()
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
            this@VodafoneAlexaSkillEnablementSucessActivity,
            object : ApiSucessCallbackInterface {
                override fun onApiSucess(isSucess: Boolean) {
                    getAlexaAuthCode()
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


    fun getAlexaAuthCode() {
        /** then the auth code for  skill activation */
        utilClass.getAuthCodeForAlexaSkillActivation(
            this@VodafoneAlexaSkillEnablementSucessActivity,
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
                            deviceIpAddress,
                            MIDCONST.ALEXA_SKILL_STATUS,
                            "ENABLE::".plus(buildActivateSkillJsonData(alexaSkillAuthCode).toString())
                        )
                    }
                }
            })
    }

    override fun newDeviceFound(node: LSSDPNodes?) {

    }

    override fun deviceGotRemoved(ipaddress: String?) {

    }

    fun buildAlexaSkillStatusJsonData(): JSONObject {
        val jsonObject = JSONObject()
        val skillId = "amzn1.ask.skill.2bf3853d-ecfc-460e-b2cd-6c239e003340"

        jsonObject.put("baseUrl", "https://api.amazonalexa.com")
        jsonObject.put("endpointPath", "/v1/alexaApiEndpoint")
        jsonObject.put("skillPath", "/v0/skills/$skillId/enablement")


        return jsonObject
    }

    override fun messageRecieved(packet: NettyData?) {
        LibreLogger.d(this, "New message appeared for the device " + packet!!.getRemotedeviceIp())
        val messagePacket = LUCIPacket(packet!!.getMessage())

        when (messagePacket.command) {
            MIDCONST.ALEXA_SKILL_STATUS -> {
                val payload: String = messagePacket.getpayload().toString(Charsets.UTF_8)
                Log.d(TAG, "receivedSkillPayloadMessage: $payload")

                when (payload) {
                    "ENABLE_SUCCESS" -> {
                        runOnUiThread {
                            stopLotteAnimationView()
                            /** The skill is enabled sucessfully goto the dinner time devices */

                        }
                        updateAlexaEnablementStatus(true)
                    }

                    "ENABLE_RETRY" -> {
                        AuthenticateThread().start()
                    }

                    "ENABLE_ERROR" -> {

                        runOnUiThread {
                            utilClass.buildCustomFullScreenAlexaLoginError(this@VodafoneAlexaSkillEnablementSucessActivity,
                                object : ONButtonClickFromCustomAlertDialog {
                                    override fun onButtonClick(buttonName: String) {
                                        if (buttonName == "Amazon Alexa App") {
                                            launchTheApp("com.amazon.dee.app")
                                        } else {
                                            //next relase
                                        }
                                    }
                                })
                            stopLotteAnimationView()

                        }
                        /** since we are just showing the user there is an error while enabling the skill
                        but not letting the user enable the skill again from the app */
                        updateAlexaEnablementStatus(true)

                    }


                    /** for now no need to check these statues */
                    "DISABLE_SUCCESS" -> {

                    }

                    "DISABLE_RETRY" -> {

                    }

                    "DISABLE_ERROR" -> {

                    }


                }
            }
        }
    }


    fun launchTheApp(appPackageName: String?) {
        val intent: Intent? = packageManager.getLaunchIntentForPackage(appPackageName!!)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } else {
            redirectingToPlayStore(intent, appPackageName!!)
        }
    }

    fun alexaSkillStatus(ip: String, command: Int, activateSkillJsonData: String) {
        LUCIControl(ip).SendCommand(command, activateSkillJsonData, LSSDPCONST.LUCI_SET)
        Log.d(TAG, "sentSkillStatus ".plus(activateSkillJsonData.toString()))
    }

    fun redirectingToPlayStore(intent: Intent?, appPackageName: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.data = Uri.parse("market://details?id=$appPackageName")
            startActivity(intent)
        } catch (anfe: ActivityNotFoundException) {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.data = Uri.parse("http://play.google.com/store/apps/details?id=$appPackageName")
            startActivity(intent)
        }
    }

    fun updateAlexaEnablementStatus(status: Boolean) {
        val sharedPreferences = getSharedPreferences("Libre", Context.MODE_PRIVATE)
        val editor: SharedPreferences.Editor = sharedPreferences.edit()
        editor.putBoolean("alexaSkillEnablement", status)
        editor.apply()
    }

    fun gotoSettingUpFamilyRoutineActivity() {
        if (VodafoneDinnerTimePairedActivity.vodafoneDinnerTimePairedActivity != null) {
            VodafoneDinnerTimePairedActivity.vodafoneDinnerTimePairedActivity?.finish()
        }
        val intent = Intent(
            this@VodafoneAlexaSkillEnablementSucessActivity,
            VodafoneSettingUpFamilyRoutinesActivity::class.java
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


    override fun deviceDiscoveryAfterClearingTheCacheStarted() {

    }
}