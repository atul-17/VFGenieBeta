package com.libre.alexa.activities.newFlow


import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.airbnb.lottie.LottieDrawable
import com.android.volley.*
import com.libre.alexa.DeviceDiscoveryActivity
import com.libre.alexa.LibreApplication
import com.libre.alexa.R
import com.libre.alexa.Scanning.ScanningHandler
import com.libre.alexa.luci.LSSDPNodeDB
import com.libre.alexa.luci.LSSDPNodes
import com.libre.alexa.luci.LUCIPacket
import com.libre.alexa.models.ModelGetAllDevicesResponse
import com.libre.alexa.netty.LibreDeviceInteractionListner
import com.libre.alexa.netty.NettyData
import com.libre.alexa.util.LibreLogger
import com.libre.alexa.utils.UtilClass
import com.libre.alexa.utils.interfaces.ApiSucessCallbackInterface
import com.libre.alexa.viewmodels.ApisViewModel
import com.vodafone.idtmlib.AccessToken
import com.vodafone.idtmlib.IdtmLib
import com.vodafone.idtmlib.exceptions.IDTMException
import com.vodafone.idtmlib.exceptions.IdtmInProgressException
import com.vodafone.idtmlib.observers.AuthenticateObserver
import com.vodafone.idtmlib.observers.InitializeObserver
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_browser_vodafone.*
import java.lang.ref.WeakReference


class VodafoneLoginRegisterWebViewActivity : DeviceDiscoveryActivity(),
        LibreDeviceInteractionListner {


    lateinit var state: String

    val utilClass = UtilClass()


    var alertWrongNetwork: AlertDialog? = null

    val compositeDisposable = CompositeDisposable()

    val TAG = VodafoneLoginRegisterWebViewActivity::class.java.simpleName

    var isUserLoggedIntoVodafoneAccount = false

    lateinit var apiViewModel: ApisViewModel

    var mScanHandler = ScanningHandler.getInstance()

    var mNode: LSSDPNodes? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_browser_vodafone)

        disableNetworkChangeCallBack()

        apiViewModel =
                ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory(application)).get(
                        ApisViewModel::class.java
                )


        val connectionManager =
                getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        var wifiCheck = connectionManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
        var mobileDataCheck = connectionManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)


        if (!getSharedPreferences("Libre", Context.MODE_PRIVATE).getBoolean("isUserLoggedIn", false)) {
            if (wifiCheck.isConnected || mobileDataCheck.isConnected) {
                showLotteAnimationView()
                initIdtmSdk()
            } else {
                //show no network dialog
                showAlertDialogForConnectingToWifi()
            }
        } else {
            /** Need to take the user to the Dinner time devices
             * screen if the following conditions are met
             * 1:RounteCertPaired is true
             * 2:Amazon login done
             * alex skill is enabled
             * */
            showLotteAnimationView()
            /** getting genie model from the db */
            if (getGenieDevice() != null) {
                AuthenticateThreadGettingLatestToken().start()
            } else {
                Log.d(TAG, "latestTokenNotCalled and now waiting for genie model to be discovered")
            }
        }
    }

    private inner class AuthenticateThreadGettingLatestToken : Thread() {
        override fun run() {
            try {
                val accessToken: AccessToken =
                        LibreApplication.getIdtmLib(this@VodafoneLoginRegisterWebViewActivity)
                                .authenticate(
                                        true, getSharedPreferences("Libre", Context.MODE_PRIVATE)
                                        .getString("accessToken", null)
                                )

                apiViewModel.updateAccessToken(
                        accessToken.token,
                        this@VodafoneLoginRegisterWebViewActivity
                )

                Log.d(TAG, "accessToken+LatestToken ${accessToken.token}")

                Handler(Looper.getMainLooper()).post {
                    addDeviceToCloudIfDeviceIsNoPresent(getGenieDevice()!!)
                }

            } catch (e: Exception) {

                runOnUiThread(Runnable {
                    stopLotteAnimationView()
                    Log.d(TAG, "authenticate sync onError", e)
                })

                try {
                    LibreApplication.getIdtmLib(this@VodafoneLoginRegisterWebViewActivity).logout()
                    LibreApplication.getIdtmLib(this@VodafoneLoginRegisterWebViewActivity).eraseAll()
                    LibreApplication.getIdtmLib(this@VodafoneLoginRegisterWebViewActivity).clearHashes()
                } catch (e: IdtmInProgressException) {
                    LibreApplication.getIdtmLib(this@VodafoneLoginRegisterWebViewActivity).eraseAll()
                    LibreApplication.getIdtmLib(this@VodafoneLoginRegisterWebViewActivity).clearHashes()
                }
                runOnUiThread {
                    showLotteAnimationView()
                }
                initIdtmSdk()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        registerForDeviceEvents(this)
    }

    fun initIdtmSdk() {
        val worker = InitliazeIdtmLibAsyncTask(this@VodafoneLoginRegisterWebViewActivity)
        val workthread = Thread(worker)
        workthread.start()
    }


    fun showLotteAnimationView() {
        if (lotteAnimationView != null) {
            lotteAnimationView.setAnimation("vf_spinner_red_large_300.json")
            lotteAnimationView.repeatCount = LottieDrawable.INFINITE
            lotteAnimationView.tag = "lotteAnimation"
            lotteAnimationView.playAnimation()
        }
    }

    fun stopLotteAnimationView() {
        if (lotteAnimationView != null) {
            lotteAnimationView.clearAnimation();
        }
    }


    fun addDeviceToCloudIfDeviceIsNoPresent(node: LSSDPNodes) {
        /** Yes -> get the mac-id, call the GetAll and check for the genie device IF
         * NO -> call the Add Api by passing the macid
         * then read the 208 command -> the routerCert Paired ie true/false
         * */
        apiViewModel.getAllDevices()
                ?.observe(this@VodafoneLoginRegisterWebViewActivity, Observer { modelGetAllDeviceRepo ->
                    if (modelGetAllDeviceRepo.modelGetAllDeviceRepo?.modelGetAllDevicesResponseList != null) {
                        if (modelGetAllDeviceRepo.modelGetAllDeviceRepo?.modelGetAllDevicesResponseList!!.size > 0) {
                            /**  if the device is not present then add it to cloud*/
                            if (!checkIfTheDeviceIsPresentInTheCloud(
                                            node.usn,
                                            modelGetAllDeviceRepo.modelGetAllDeviceRepo?.modelGetAllDevicesResponseList!!
                                    )
                            ) {
                                addDeviceToCloud(object : ApiSucessCallbackInterface {
                                    override fun onApiSucess(isSucess: Boolean) {
                                        Handler().postDelayed({
                                            mNode = node
                                            readEnvVox(node.ip)
                                        }, 1000)
                                    }
                                }, node)
                            } else {
                                //device is  present:no need to add
                                Log.d(TAG, "Device present, no need to add")
                                mNode = node
                                Handler().postDelayed({
                                    mNode = node
                                    readEnvVox(node.ip)
                                }, 1000)
                            }
                        } else {
                            //no device is present in the cloud
                            //adding it again
                            Log.d(TAG, "No Device present in the cloud at the moment")
                            addDeviceToCloud(object : ApiSucessCallbackInterface {
                                override fun onApiSucess(isSucess: Boolean) {
                                    mNode = node
                                    Handler().postDelayed({
                                        mNode = node
                                        readEnvVox(node.ip)
                                    }, 1000)
                                }
                            }, node)
                        }

                    } else {
                        stopLotteAnimationView()
                        Log.d(TAG, modelGetAllDeviceRepo.volleyError.toString())

                        val volleyError = modelGetAllDeviceRepo.volleyError

                        if (volleyError is TimeoutError || volleyError is NoConnectionError) {

                            utilClass.buildSnackBarWithoutButton(
                                    this@VodafoneLoginRegisterWebViewActivity,
                                    (this@VodafoneLoginRegisterWebViewActivity as AppCompatActivity).window.decorView.findViewById(android.R.id.content),
                                    "Seems your internet connection is slow, please try in sometime"
                            )

                        } else if (volleyError is AuthFailureError) {

                            utilClass.buildSnackBarWithoutButton(
                                    this@VodafoneLoginRegisterWebViewActivity,
                                    (this@VodafoneLoginRegisterWebViewActivity as AppCompatActivity).window.decorView.findViewById(android.R.id.content),
                                    "AuthFailure error occurred, please try again later"
                            )

                        } else if (volleyError is ServerError) {

                            utilClass.buildSnackBarWithoutButton(
                                    this@VodafoneLoginRegisterWebViewActivity,
                                    (this@VodafoneLoginRegisterWebViewActivity as AppCompatActivity).window.decorView.findViewById(android.R.id.content),
                                    "Server error occurred, please try again later"
                            )

                        } else if (volleyError is NetworkError) {

                            utilClass.buildSnackBarWithoutButton(
                                    this@VodafoneLoginRegisterWebViewActivity,
                                    (this@VodafoneLoginRegisterWebViewActivity as AppCompatActivity).window.decorView.findViewById(android.R.id.content),
                                    "Network error occurred, please try again later"
                            )

                        } else if (volleyError is ParseError) {
                            utilClass.buildSnackBarWithoutButton(
                                    this@VodafoneLoginRegisterWebViewActivity,
                                    (this@VodafoneLoginRegisterWebViewActivity as AppCompatActivity).window.decorView.findViewById(android.R.id.content),
                                    "Parser error occurred, please try again later"
                            )
                        }
                    }
                })
    }

    fun addDeviceToCloud(apiSucessCallbackInterface: ApiSucessCallbackInterface, node: LSSDPNodes) {
        apiViewModel.postModelAddUser(node.usn.toString().toLowerCase(), node.friendlyname)
                ?.observe(this@VodafoneLoginRegisterWebViewActivity, Observer { modelGetAllDeviceRepo ->
                    if (modelGetAllDeviceRepo.modelDeviceAddUserResponse != null) {
                        if (modelGetAllDeviceRepo.modelDeviceAddUserResponse!!.code == "0") {
                            //succesfully added device to the cloud
                            apiSucessCallbackInterface.onApiSucess(true)
                        } else {
                            apiSucessCallbackInterface.onApiSucess(false)
                            if (modelGetAllDeviceRepo.modelDeviceAddUserResponse?.code == "1005") {
                                utilClass.buildSnackBarWithoutButton(this, window.decorView.findViewById(android.R.id.content)
                                        , "This device is already registered to a particular account")
                            }
                            Log.d(TAG, "deviceSentError: ".plus(
                                    modelGetAllDeviceRepo.modelDeviceAddUserResponse!!
                                            .msg.plus(" code: ")
                                            .plus(modelGetAllDeviceRepo.modelDeviceAddUserResponse?.code)))
                        }

                    } else {
                        stopLotteAnimationView()
                        Log.d(TAG, modelGetAllDeviceRepo.volleyError.toString())

                        val volleyError = modelGetAllDeviceRepo.volleyError

                        if (volleyError is TimeoutError || volleyError is NoConnectionError) {

                            utilClass.buildSnackBarWithoutButton(
                                    this@VodafoneLoginRegisterWebViewActivity,
                                    (this@VodafoneLoginRegisterWebViewActivity as AppCompatActivity).window.decorView.findViewById(android.R.id.content),
                                    "Seems your internet connection is slow, please try in sometime"
                            )

                        } else if (volleyError is AuthFailureError) {

                            utilClass.buildSnackBarWithoutButton(
                                    this@VodafoneLoginRegisterWebViewActivity,
                                    (this@VodafoneLoginRegisterWebViewActivity as AppCompatActivity).window.decorView.findViewById(android.R.id.content),
                                    "AuthFailure error occurred, please try again later"
                            )

                        } else if (volleyError is ServerError) {

                            utilClass.buildSnackBarWithoutButton(
                                    this@VodafoneLoginRegisterWebViewActivity,
                                    (this@VodafoneLoginRegisterWebViewActivity as AppCompatActivity).window.decorView.findViewById(android.R.id.content),
                                    "Server error occurred, please try again later"
                            )

                        } else if (volleyError is NetworkError) {

                            utilClass.buildSnackBarWithoutButton(
                                    this@VodafoneLoginRegisterWebViewActivity,
                                    (this@VodafoneLoginRegisterWebViewActivity as AppCompatActivity).window.decorView.findViewById(android.R.id.content),
                                    "Network error occurred, please try again later"
                            )

                        } else if (volleyError is ParseError) {
                            utilClass.buildSnackBarWithoutButton(
                                    this@VodafoneLoginRegisterWebViewActivity,
                                    (this@VodafoneLoginRegisterWebViewActivity as AppCompatActivity).window.decorView.findViewById(android.R.id.content),
                                    "Parser error occurred, please try again later"
                            )
                        }
                    }
                })
    }

    fun getGenieDevice(): LSSDPNodes? {
        LibreLogger.d(this,
                "Master is Getting Added T Active Scenes Adapter" + mScanHandler.sceneObjectFromCentralRepo.keys)
        for (nodes in LSSDPNodeDB.getInstance().GetDB()) {
            if (nodes.deviceState != null) {
                return nodes
            }
        }
        return null
    }

    fun gotoSwfPairedActivityOrPairingDinnerTimeDeviceListScreen(
            node: LSSDPNodes,
            isRouterPaired: Boolean
    ) {
        stopLotteAnimationView()
        if (isRouterPaired) {
            val sharedPreferences = getSharedPreferences("Libre", Context.MODE_PRIVATE)
            if (!sharedPreferences.getBoolean("alexaSkillEnablement", false)) {
                //if alexa skill is not enabled goto paired screen
                val intent = Intent(
                        this@VodafoneLoginRegisterWebViewActivity,
                        VodafoneDinnerTimePairedActivity::class.java
                )
                val bundle = Bundle()
                bundle.putSerializable("deviceDetails", node)
                intent.putExtras(bundle)
                startActivity(intent)
                finish()
            } else {
                //if the alexa skill is enabled
                //goto dinner time devices list
                val intent = Intent(
                        this@VodafoneLoginRegisterWebViewActivity,
                        VodafoneAlexaDinnerTimeDeviceListActivity::class.java
                )
                val bundle = Bundle()
                bundle.putSerializable("deviceDetails", node)
                intent.putExtras(bundle)
                startActivity(intent)
                finish()
            }
        } else {
            val intent = Intent(
                    this@VodafoneLoginRegisterWebViewActivity,
                    VodafoneDinnerTimeWiredPairingActivity::class.java
            )
            val bundle = Bundle()
            bundle.putSerializable("deviceDetails", node)
            intent.putExtras(bundle)
            startActivity(intent)
            finish()
        }

        overridePendingTransition(R.anim.left_to_right_anim_tranistion,
                R.anim.right_to_left_anim_transition);

    }

    private fun showAlertDialogForConnectingToWifi() {
        if (!this@VodafoneLoginRegisterWebViewActivity.isFinishing) {
            val builder = AlertDialog.Builder(this@VodafoneLoginRegisterWebViewActivity)
            getString(R.string.restartTitle)
            builder.setMessage(getString(R.string.noNetwork))
                    .setCancelable(false)
                    .setPositiveButton(getString(R.string.gotoSettings)) { dialog, id ->
                        alertWrongNetwork?.dismiss()
                        val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
                        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        startActivityForResult(intent, 1234)
                    }
            alertWrongNetwork = builder.show()
            alertWrongNetwork?.show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1234) {
            val connectionManager =
                    getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            var wifiCheck = connectionManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
            var mobileDataCheck = connectionManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)
            if (wifiCheck.isConnected || mobileDataCheck.isConnected) {
                showLotteAnimationView()
                initIdtmSdk()
            } else {
                showAlertDialogForConnectingToWifi()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.clear()
    }

    private inner class MyAuthenticateObserver : AuthenticateObserver() {
        override fun onHideIdGateway() {

        }

        override fun onComplete(accessToken: AccessToken?) {
            LibreLogger.d(this, "authenticationComplete ${accessToken?.token}")
            isUserLoggedIntoVodafoneAccount = true
            //call share code and auth code apis
            val sharedPref: SharedPreferences =
                    getSharedPreferences("Libre", Context.MODE_PRIVATE)
            val editor: SharedPreferences.Editor = sharedPref.edit()
            editor.putString("accessToken", accessToken?.token)
            editor.putString("tokenType", accessToken?.type)
            editor.putBoolean("isUserLoggedIn", true)
            editor.apply()

            /** According to the ui/ux flow guide lines
             * call GetAll device mgt api and check for genie device
             *  take the user to dinner time devices screen
             * */
            showLotteAnimationView()
            if (isUserLoggedIntoVodafoneAccount) {
                if (getGenieDevice() != null) {
                    addDeviceToCloudIfDeviceIsNoPresent(getGenieDevice()!!)
                }
            }
        }

        override fun onShowIdGateway() {
        }

        override fun onSubscribe(p0: Disposable) {
            LibreLogger.d(this, "authenticate Subscribe")
            compositeDisposable.add(p0)

        }

        override fun onError(e: Throwable) {
            stopLotteAnimationView()

            LibreLogger.d(this, "authenticate Error".plus(e.toString()))
            if (e is IDTMException) {
//                runOnUiThread {
//                    utilClass.buildSnackBarWithoutButton(this@VodafoneLoginRegisterWebViewActivity,
//                            window.decorView.findViewById(android.R.id.content), "There seems to be problem ,please try again after some time")
//                }
                LibreApplication.getIdtmLib(this@VodafoneLoginRegisterWebViewActivity)
                        .authenticate(true, null, MyAuthenticateObserver())

            }
        }

    }


    inner class InitliazeIdtmLibAsyncTask(context: Context) : Runnable {
        private var contextRef: WeakReference<Context>? = null

        init {
            contextRef = WeakReference(context)
        }

        override fun run() {
            LibreApplication.getIdtmLib(contextRef!!.get()).initialize(
                    LibreApplication.APIX_CLIENT_ID,
                    LibreApplication.IDGATEWAY_REDIRECT_URL,
                    LibreApplication.ID_GATEWAY_ACR_MOBILE,
                    LibreApplication.ID_GATEWAY_ACR_WIFI,
                    LibreApplication.ID_GATEWAY_SCOPE,
                    MyIntiliazeObserver(),
                    LibreApplication.LOGIN_HINT
            )
        }

    }

    inner class MyIntiliazeObserver : InitializeObserver() {
        override fun onSubscribe(disposable: Disposable) {

        }

        override fun onError(e: Throwable) {
            LibreLogger.d(this, "initialize onError$e")
        }

        override fun onComplete() {
            LibreLogger.d(this, "initialize onComplete")
            LibreApplication.getIdtmLib(this@VodafoneLoginRegisterWebViewActivity)
                    .authenticate(true, null, MyAuthenticateObserver())
        }
    }

    override fun newDeviceFound(node: LSSDPNodes?) {
        LibreLogger.d(this,
                "New device is found with the ip address = " + node!!.ip + "Device State" + node.deviceState)
        AuthenticateThreadGettingLatestToken().start()
    }

    fun checkIfTheDeviceIsPresentInTheCloud(
        deviceMacId: String,
        modelGetAllDevicesList: MutableList<ModelGetAllDevicesResponse>
    ): Boolean {
        for (modelGenieDevice in modelGetAllDevicesList) {
            if (modelGenieDevice.deviceId!!.equals(deviceMacId, true)) {
                return true
            }
        }
        return false
    }
    override fun deviceGotRemoved(ipaddress: String?) {
        LibreLogger.d(this, "Device is Removed with the ip address = $ipaddress")
        TODO("need to define")
    }

    override fun messageRecieved(nettyData: NettyData?) {
        LibreLogger.d(this, "New message appeared for the device " + nettyData?.getRemotedeviceIp())
        val packet = LUCIPacket(nettyData!!.getMessage())
        val payloadValueString = String(packet.getpayload())
        //RouterCertPaired:false
        Log.d(TAG, "receivedRouterPaired$payloadValueString")
        if (payloadValueString != null) {
            if (packet.command == 208 && !payloadValueString.isEmpty()) {
                if (payloadValueString.contains("RouterCertPaired")) {
                    val parts: Array<String> =
                            payloadValueString.split(":".toRegex()).toTypedArray()
                    val envVoxReadValue = parts[1]
                    Log.d(TAG, "EnvValueRouterCertPaired$envVoxReadValue")
                    mNode?.envVoxReadValue = envVoxReadValue

                    gotoSwfPairedActivityOrPairingDinnerTimeDeviceListScreen(
                            mNode!!,
                            mNode?.envVoxReadValue!!.toBoolean()
                    )
                }
            }
        }

    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

    override fun deviceDiscoveryAfterClearingTheCacheStarted() {

    }
}
