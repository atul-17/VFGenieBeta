package com.libre.alexa.activities.newFlow

import android.app.ProgressDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import com.airbnb.lottie.LottieDrawable
import com.libre.alexa.DeviceDiscoveryActivity
import com.libre.alexa.LibreApplication
import com.libre.alexa.ManageDevice.ManageDeviceAdapter
import com.libre.alexa.R
import com.libre.alexa.Scanning.ScanningHandler
import com.libre.alexa.SceneObject
import com.libre.alexa.constants.CommandType
import com.libre.alexa.constants.LSSDPCONST
import com.libre.alexa.constants.MIDCONST
import com.libre.alexa.luci.LSSDPNodes
import com.libre.alexa.luci.LUCIControl
import com.libre.alexa.luci.LUCIPacket
import com.libre.alexa.netty.LibreDeviceInteractionListner
import com.libre.alexa.netty.NettyData
import com.libre.alexa.util.LibreLogger
import com.libre.alexa.utils.UtilClass
import kotlinx.android.synthetic.main.activity_vodafone_alexa_dinner_time_settings.*
import kotlinx.android.synthetic.main.custom_vdf_animation_view.*
import kotlinx.android.synthetic.main.toolbar_white_bg_with_settings.*
import org.json.JSONObject

class VodafoneAlexaDinnerTimeSettingsActivity : DeviceDiscoveryActivity(), LibreDeviceInteractionListner {

    var bundle = Bundle()

    var node: LSSDPNodes? = null

    var TAG = VodafoneAlexaDinnerTimeSettingsActivity::class.java.simpleName


    var utilClass = UtilClass()


    private var currentSceneObject: SceneObject? = null

    private var mScanHandler: ScanningHandler? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vodafone_alexa_dinner_time_settings)


        disableNetworkChangeCallBack()

        bundle = intent.extras!!
        if (bundle != null) {
            node = bundle.getSerializable("deviceDetails") as LSSDPNodes
        }

        mScanHandler = ScanningHandler.getInstance()

        currentSceneObject = ScanningHandler.getInstance().getSceneObjectFromCentralRepo(node?.ip)

        llBack.setOnClickListener {
            onBackPressed()
        }


        registerForDeviceEvents(this)


        if (node?.ip != null) {
           showLotteAnimationView()
            alexaSkillStatus(node!!.ip,
                    MIDCONST.ALEXA_SKILL_STATUS,
                    "STATUS::".plus(buildAlexaSkillStatusJsonData().toString()))
            checkAlexaStatus()

        }



        etDeviceName.setText(node?.friendlyname)

        //firmware version
        tvSystemFw.text = node?.version

        tvMacAddress.text = buildMacAddressFormatToDisplay(node?.usn)


        genieVolumeBar!!.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {}

            override fun onStartTrackingTouch(seekBar: SeekBar) {}

            override fun onStopTrackingTouch(seekBar: SeekBar) {

                if (!doVolumeChange(seekBar.progress))
                    showToast(this@VodafoneAlexaDinnerTimeSettingsActivity, "Action Failed")
            }
        })
    }

    override fun onResume() {
        super.onResume()
        initVolumeSeekbar()
    }

    fun showLotteAnimationView() {
        tvLoadingReason.text = "Please wait..."
        tvLoadingReason.visibility = View.VISIBLE
        llSettings.visibility = View.GONE
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
            llSettings.visibility = View.VISIBLE
        }
    }

    fun initVolumeSeekbar() {
        if (LibreApplication.INDIVIDUAL_VOLUME_MAP.containsKey(currentSceneObject!!.ipAddress)) {
            genieVolumeBar!!.progress = LibreApplication.INDIVIDUAL_VOLUME_MAP[currentSceneObject?.ipAddress!!]!!
        } else {
            val control = LUCIControl(currentSceneObject!!.ipAddress)
            control.SendCommand(MIDCONST.VOLUEM_CONTROL, null, LSSDPCONST.LUCI_GET)
            if (currentSceneObject!!.volumeValueInPercentage >= 0)
                genieVolumeBar!!.progress = currentSceneObject!!.volumeValueInPercentage
        }
    }

    internal fun doVolumeChange(currentVolumePosition: Int): Boolean {
        /* We can make use of CurrentIpAddress instead of CurrenScneObject.getIpAddress*/
        val control = LUCIControl(node?.ip)
        control.SendCommand(MIDCONST.VOLUEM_CONTROL, "" + currentVolumePosition, LSSDPCONST.LUCI_SET)
        currentSceneObject!!.volumeValueInPercentage = currentVolumePosition
        mScanHandler!!.putSceneObjectToCentralRepo(node?.ip, currentSceneObject)
        return true
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



    fun buildAlexaSkillStatusJsonData(): JSONObject {
        val jsonObject = JSONObject()
        val skillId = "amzn1.ask.skill.2bf3853d-ecfc-460e-b2cd-6c239e003340"

        jsonObject.put("baseUrl", "https://api.amazonalexa.com")
        jsonObject.put("endpointPath", "/v1/alexaApiEndpoint")
        jsonObject.put("skillPath", "/v0/skills/$skillId/enablement")


        return jsonObject
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
                      stopLotteAnimationView()
                        tvFamilySkillStatus.text = "ENABLED"
                    }

                    "STATUS_DISABLED" -> {
                        stopLotteAnimationView()
                        tvFamilySkillStatus.text = "DISABLED"
                    }

                    "STATUS_RETRY" -> {
                        alexaSkillStatus(node!!.ip,
                                MIDCONST.ALEXA_SKILL_STATUS,
                                "STATUS::".plus(buildAlexaSkillStatusJsonData().toString()))
                    }
                    "STATUS_ERROR" -> {
                        stopLotteAnimationView()
                        tvFamilySkillStatus.text = ""
                        utilClass.buildSnackBarWithoutButton(this, window.decorView.findViewById(android.R.id.content),
                                "There seems to be an error while checking  the alexa Skill status")
                    }
                    else -> {
                        stopLotteAnimationView()
                    }
                }
            }
            MIDCONST.CHECK_ALEXA_LOGIN_STATUS -> {
                val payload: String = messagePacket.getpayload().toString(Charsets.UTF_8)
                Log.d(TAG, "receivedSkillPayloadMessage: $payload")
                when (payload) {
                    "LOGGEDIN", "READY" -> {
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

            MIDCONST.VOLUEM_CONTROL -> {
                val message = String(messagePacket.getpayload())
                try {
                    val duration = Integer.parseInt(message)
                    currentSceneObject!!.volumeValueInPercentage = duration
                    LibreApplication.INDIVIDUAL_VOLUME_MAP[packet?.getRemotedeviceIp()] = duration
                    if (mScanHandler!!.isIpAvailableInCentralSceneRepo(node?.ip)) {
                        mScanHandler!!.putSceneObjectToCentralRepo(node?.ip, currentSceneObject)
                        genieVolumeBar!!.progress = LibreApplication.INDIVIDUAL_VOLUME_MAP[currentSceneObject?.ipAddress!!]!!
                    }
                    LibreLogger.d(this, "Recieved the current volume to be " + currentSceneObject!!.volumeValueInPercentage)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun deviceDiscoveryAfterClearingTheCacheStarted() {

    }

}