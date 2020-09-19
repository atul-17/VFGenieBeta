package com.libre.alexa.activities.newFlow

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.View
import com.airbnb.lottie.LottieDrawable
import com.amazon.identity.auth.device.AuthError
import com.amazon.identity.auth.device.api.authorization.*
import com.amazon.identity.auth.device.api.workflow.RequestContext
import com.libre.alexa.DeviceDiscoveryActivity
import com.libre.alexa.R
import com.libre.alexa.alexa.CompanionProvisioningInfo
import com.libre.alexa.alexa.DeviceProvisioningInfo
import com.libre.alexa.alexa.userpoolManager.AlexaUtils.AlexaConstants
import com.libre.alexa.alexa_signin.AlexaUtils
import com.libre.alexa.constants.LSSDPCONST
import com.libre.alexa.constants.LUCIMESSAGES
import com.libre.alexa.constants.MIDCONST
import com.libre.alexa.luci.LSSDPNodeDB
import com.libre.alexa.luci.LSSDPNodes
import com.libre.alexa.luci.LUCIControl
import com.libre.alexa.luci.LUCIPacket
import com.libre.alexa.netty.LibreDeviceInteractionListner
import com.libre.alexa.netty.NettyData
import com.libre.alexa.util.LibreLogger
import com.libre.alexa.utils.UtilClass
import com.libre.alexa.utils.interfaces.ONButtonClickFromCustomAlertDialog
import kotlinx.android.synthetic.main.activity_vodafone_dinner_time_paired.*
import kotlinx.android.synthetic.main.custom_vdf_animation_view.*
import org.json.JSONException
import org.json.JSONObject

class VodafoneDinnerTimePairedActivity : DeviceDiscoveryActivity(), LibreDeviceInteractionListner {


    val TAG = VodafoneDinnerTimePairedActivity::class.java.simpleName

    val utilClass = UtilClass()


    val PRODUCT_ID = "productID"
    val ALEXA_ALL_SCOPE = "alexa:all"

    val DEVICE_SERIAL_NUMBER = "deviceSerialNumber"
    val PRODUCT_INSTANCE_ATTRIBUTES = "productInstanceAttributes"

    val APP_SCOPES = arrayOf(ALEXA_ALL_SCOPE)
    val ALEXA_META_DATA_TIMER = 0x12


    private var deviceIpAddress: String? = null

    private var alertDialog: AlertDialog? = null

    private var invalidApiKey = false

    var nodes: LSSDPNodes? = null

    private val ACCESS_TOKEN_TIMEOUT = 301

    private var isMetaDateRequestSent = false

    private var bundle = Bundle()

    private lateinit var requestContext: RequestContext

    companion object {
        var vodafoneDinnerTimePairedActivity: VodafoneDinnerTimePairedActivity? = null

    }


    private val handler: Handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            if (msg.what == ACCESS_TOKEN_TIMEOUT
                || msg.what == ALEXA_META_DATA_TIMER) {
                stopLotteAnimationView()
                somethingWentWrong(this@VodafoneDinnerTimePairedActivity)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        vodafoneDinnerTimePairedActivity = this

        requestContext = RequestContext.create(this@VodafoneDinnerTimePairedActivity)

        requestContext.registerListener(AuthListener())

        setContentView(R.layout.activity_vodafone_dinner_time_paired)

        disableNetworkChangeCallBack()

        bundle = intent.extras!!

        if (bundle != null) {
            nodes = bundle.getSerializable("deviceDetails") as LSSDPNodes
        }

        deviceIpAddress = nodes?.ip


        btnSignWithAlexa.setOnClickListener {

            if (invalidApiKey) {
                somethingWentWrong(this@VodafoneDinnerTimePairedActivity)
                return@setOnClickListener
            }

            if (nodes == null) {
                return@setOnClickListener
            }

            if (nodes != null && !isMetaDateRequestSent()) {
                handler.sendEmptyMessageDelayed(ALEXA_META_DATA_TIMER, 20000)
                showLotteAnimationView()
                AlexaUtils.sendAlexaMetaDataRequest(deviceIpAddress)
                setMetaDateRequestSent(true)
                return@setOnClickListener
            }

            val scopeData = JSONObject()
            val productInstanceAttributes = JSONObject()

            try {
                //codeChallenge
                val codeChallenge = nodes!!.mdeviceProvisioningInfo.codeChallenge
                val codeChallengeMethod = nodes!!.mdeviceProvisioningInfo.codeChallengeMethod

                productInstanceAttributes.put(DEVICE_SERIAL_NUMBER, nodes!!.mdeviceProvisioningInfo.dsn);

                scopeData.put(PRODUCT_INSTANCE_ATTRIBUTES, productInstanceAttributes);
                scopeData.put(PRODUCT_ID, nodes!!.mdeviceProvisioningInfo.productId);


                AuthorizationManager.authorize(AuthorizeRequest.Builder(requestContext)
                    //for avs hosted splash
                    .addScopes(ScopeFactory.scopeNamed("alexa:voice_service:pre_auth"),
                        ScopeFactory.scopeNamed("alexa:all", scopeData))
                    .forGrantType(AuthorizeRequest.GrantType.AUTHORIZATION_CODE)
                    .withProofKeyParameters(codeChallenge, codeChallengeMethod)
                    .build())

            } catch (e: JSONException) {
                e.printStackTrace()
                LibreLogger.d(this, "AmazonLogin_Auth" + "json ex")
            }
        }
    }


    private inner class AuthListener : AuthorizeListener() {
        override fun onSuccess(response: AuthorizeResult?) {
            try {
                nodes = LSSDPNodeDB.getInstance().getTheNodeBasedOnTheIpAddress(deviceIpAddress)

                val authorizationCode = response?.authorizationCode
                val redirectUri = response!!.redirectURI
                val clientId = response!!.clientId
                val sessionId = nodes!!.mdeviceProvisioningInfo.sessionId

                LibreLogger.d(this, "Alexa Value From 234, session ID $sessionId")

                val companionProvisioningInfo = CompanionProvisioningInfo(sessionId, clientId, redirectUri, authorizationCode)
                val luciControl = LUCIControl(deviceIpAddress)

                luciControl.SendCommand(MIDCONST.ALEXA_COMMAND.toInt(), "AUTHCODE_EXCH:" + companionProvisioningInfo.toJson().toString(), LSSDPCONST.LUCI_SET)
                Log.e(TAG, "AuthError during authorization" + companionProvisioningInfo.toJson().toString())
                showLotteAnimationView()
                handler.sendEmptyMessageDelayed(ACCESS_TOKEN_TIMEOUT, 25000)
            } catch (authError: AuthError) {
                authError.printStackTrace()
                runOnUiThread(Runnable {
                    if (!isFinishing()) {
                        stopLotteAnimationView()
                    }
                })
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        override fun onCancel(cause: AuthCancellation?) {
            Log.e(TAG, "User cancelled authorization")
            val finalError = "User cancelled signin"
            runOnUiThread(Runnable {
                if (!isFinishing()) {
                    stopLotteAnimationView()
                    showAlertDialog(finalError)
                }
            })
        }

        override fun onError(ae: AuthError) {
            Log.e(TAG, "AuthError_during_authorization", ae)
            var error = ae.message
            if (error == null || error.isEmpty()) error = ae.toString()
            val finalError: String = error
            if (!isFinishing) {
                stopLotteAnimationView()
                if (finalError == "No code in OAuth2 response") {
                    //if user skips amazon login and then again clicks no thanks button upon asking again.
                    runOnUiThread {
                        utilClass.buildCustomUserDoesNotActivateAlexaLogin(this@VodafoneDinnerTimePairedActivity, object : ONButtonClickFromCustomAlertDialog {
                            override fun onButtonClick(buttonName: String) {
                                if (buttonName == "Sign in with Alexa") {
                                    btnSignWithAlexa.performClick()
                                }
                            }
                        })
                    }
                } else {
                    runOnUiThread {
                        //show the common error dialog
                        showAlertDialog(finalError)
                    }
                }
            }
        }
    }


    fun isMetaDateRequestSent(): Boolean {
        return isMetaDateRequestSent
    }

    override fun onResume() {
        super.onResume()
        try {
            requestContext.onResume()
//            mAuthManager = AmazonAuthorizationManager(this, Bundle.EMPTY)
            invalidApiKey = false
        } catch (e: java.lang.Exception) {
            LibreLogger.d(this, """amazon auth exception${e.message}${e.stackTrace}""".trimIndent())
            invalidApiKey = true
            utilClass.buildSnackBarWithoutButton(this, window.decorView.findViewById(android.R.id.content), "" + e.message)
        }
        setMetaDateRequestSent(false)

        registerForDeviceEvents(this)
    }


    fun setMetaDateRequestSent(metaDateRequestSent: Boolean) {
        isMetaDateRequestSent = metaDateRequestSent
    }

    fun showLotteAnimationView() {
        runOnUiThread {
            rlPairedScreen.visibility = View.GONE
            tvLoadingReason.text = "Please wait..."
            tvLoadingReason.visibility = View.VISIBLE
        }
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
            runOnUiThread {
                tvLoadingReason.visibility = View.GONE
                lotteAnimationView.clearAnimation();
                lotteAnimationView.visibility = View.GONE
                rlPairedScreen.visibility = View.VISIBLE
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        vodafoneDinnerTimePairedActivity = null
    }

    override fun newDeviceFound(node: LSSDPNodes?) {

    }

    override fun deviceGotRemoved(ipaddress: String?) {

    }

    override fun messageRecieved(packet: NettyData?) {
        LibreLogger.d(this, "New message appeared for the device " + packet!!.getRemotedeviceIp())
        val messagePacket = LUCIPacket(packet!!.getMessage())
        when (messagePacket.command) {
            MIDCONST.ALEXA_COMMAND -> {
                val alexaMessage = String(messagePacket.getpayload())
                LibreLogger.d(this, "Alexa Value From 234  $alexaMessage")
                try {
                    val jsonRootObject = JSONObject(alexaMessage)
                    // JSONArray jsonArray = jsonRootObject.optJSONArray("Window CONTENTS");
                    val jsonObject = jsonRootObject.getJSONObject(LUCIMESSAGES.TAG_WINDOW_CONTENT)
                    val productId = jsonObject.optString("PRODUCT_ID").toString()
                    val dsn = jsonObject.optString("DSN").toString()
                    val sessionId = jsonObject.optString("SESSION_ID").toString()
                    val codeChallenge = jsonObject.optString("CODE_CHALLENGE").toString()
                    val codeChallengeMethod = jsonObject.optString("CODE_CHALLENGE_METHOD").toString()
                    var locale = ""
                    if (jsonObject.has("LOCALE")) locale = jsonObject.optString("LOCALE").toString()
                    nodes = LSSDPNodeDB.getInstance().getTheNodeBasedOnTheIpAddress(deviceIpAddress)
                    if (nodes != null) {
                        val mDeviceProvisioningInfo = DeviceProvisioningInfo(productId, dsn, sessionId, codeChallenge, codeChallengeMethod, locale)
                        nodes?.mdeviceProvisioningInfo = mDeviceProvisioningInfo
                        handler.removeMessages(ALEXA_META_DATA_TIMER)
//                        dismissLoader()
                        if (isMetaDateRequestSent()) {
                            btnSignWithAlexa.performClick()
                        }
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }

                /**if there is an error in fetching AccessToken
                {"Title":"ACCESS_TOKENS_STATUS","status":false}
                if successful
                {"Title":"ACCESS_TOKENS_STATUS","status":true}*/

                val message = String(messagePacket.getpayload())
                LibreLogger.d(this, "Alexa json message is $message")
                try {
                    val jsonObject = JSONObject(message)
                    val title = jsonObject.getString("Title")
                    if (title != null) {
                        if (title == AlexaConstants.ACCESS_TOKENS_STATUS) {
                            stopLotteAnimationView()
                            val status = jsonObject.getBoolean("status")
                            handler.removeMessages(ACCESS_TOKEN_TIMEOUT)
                            if (status) {
                                gotoVodafoneAlexaSkillEnablingActivity()
                            } else {
                                somethingWentWrong(this@VodafoneDinnerTimePairedActivity)
                            }
                        }
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun showAlertDialog(error: String) {
        if (alertDialog != null) {
            if (alertDialog!!.isShowing) {
                alertDialog!!.dismiss()
            }
        }
        val builder = AlertDialog.Builder(this@VodafoneDinnerTimePairedActivity)
        builder.setTitle("Alexa Signin Error")
        builder.setMessage(error)
        builder.setNeutralButton("Close") { dialogInterface, i -> alertDialog!!.dismiss() }
        if (alertDialog == null) {
            alertDialog = builder.create()
            alertDialog?.show()
        }
    }

    fun gotoVodafoneAlexaSkillEnablingActivity() {
        val intent = Intent(this@VodafoneDinnerTimePairedActivity, VodafoneAlexaSkillEnablementSucessActivity::class.java)
        val bundle = Bundle()
        bundle.putString("ipAddress", deviceIpAddress)
        bundle.putSerializable("deviceDetails", nodes)
        intent.putExtras(bundle)
        startActivity(intent)
        overridePendingTransition(R.anim.left_to_right_anim_tranistion,
            R.anim.right_to_left_anim_transition);
    }

    override fun deviceDiscoveryAfterClearingTheCacheStarted() {

    }
}