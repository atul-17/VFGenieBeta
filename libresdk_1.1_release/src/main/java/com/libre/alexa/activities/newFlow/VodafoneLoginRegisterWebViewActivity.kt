package com.libre.alexa.activities.newFlow


import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.AlertDialog
import android.content.*
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.airbnb.lottie.LottieDrawable
import com.android.volley.*
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import com.libre.alexa.DeviceDiscoveryActivity
import com.libre.alexa.LibreApplication
import com.libre.alexa.R
import com.libre.alexa.Scanning.Constants
import com.libre.alexa.Scanning.ScanningHandler
import com.libre.alexa.constants.LSSDPCONST
import com.libre.alexa.constants.MIDCONST
import com.libre.alexa.luci.LSSDPNodeDB
import com.libre.alexa.luci.LSSDPNodes
import com.libre.alexa.luci.LUCIControl
import com.libre.alexa.luci.LUCIPacket
import com.libre.alexa.models.ModelGetAllDevicesResponse
import com.libre.alexa.netty.LibreDeviceInteractionListner
import com.libre.alexa.netty.NettyData
import com.libre.alexa.util.LibreLogger
import com.libre.alexa.utils.UtilClass
import com.libre.alexa.utils.interfaces.ApiSucessCallbackInterface
import com.libre.alexa.utils.interfaces.ONButtonClickFromCustomAlertDialog
import com.libre.alexa.utils.interfaces.OnGettingGenieDeviceFromDBInterface
import com.libre.alexa.viewmodels.ApisViewModel
import com.vodafone.idtmlib.AccessToken
import com.vodafone.idtmlib.exceptions.IDTMException
import com.vodafone.idtmlib.exceptions.IdtmInProgressException
import com.vodafone.idtmlib.observers.AuthenticateObserver
import com.vodafone.idtmlib.observers.InitializeObserver
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.custom_vdf_animation_view.*
import java.lang.ref.WeakReference
import java.text.DateFormat
import java.util.*


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


    companion object {
        var M_Search_Timeout: Int = 0x77
    }

    private var mHandler = Handler()


    var envVoxRouterPairedValue = false

    var timer = Timer()

    var bgThread: Handler = Handler()


    // location related apis
    private var mFusedLocationClient: FusedLocationProviderClient? = null
    private var mSettingsClient: SettingsClient? = null
    private var mLocationRequest: LocationRequest? = null
    private var mLocationSettingsRequest: LocationSettingsRequest? = null
    private var mLocationCallback: LocationCallback? = null
    private var mCurrentLocation: Location? = null
    private val REQUEST_CHECK_SETTINGS = 100

    var showCustomLocationOnce = false

    var calledLocationUpdatesFromOnCreate = false

    var calledIdtmSdkFromOnCrete = false

    // location last updated time
    private var mLastUpdateTime: String? = null

    var isUserPermanentlyDenied = false

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("is_requesting_updates", mRequestingLocationUpdates!!)
        outState.putParcelable("last_known_location", mCurrentLocation)
        outState.putString("last_updated_on", mLastUpdateTime)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_browser_vodafone)

        registerForDeviceEvents(this)



        mHandler = object : Handler() {
            @SuppressLint("HandlerLeak")
            override fun handleMessage(msg: Message?) {
                when (msg?.what) {
                    M_Search_Timeout -> {
                        mHandler.removeCallbacksAndMessages(M_Search_Timeout)
                        //show the suoer wifi not found
                        runOnUiThread {
                            stopLotteAnimationView()
                            unRegisterForDeviceEvents()
                            utilClass.buildCustomFullScreenForLs9DeviceNotFound(this@VodafoneLoginRegisterWebViewActivity,
                                object : ONButtonClickFromCustomAlertDialog {
                                    override fun onButtonClick(buttonName: String) {
                                        if (buttonName == "Try Again") {
                                            showLotteAnimationView()
                                            bgThread.post {
                                                registerForDeviceEvents(this@VodafoneLoginRegisterWebViewActivity)
                                                mMyTaskRunnableForMSearch.run()
                                                Handler().postDelayed(
                                                    {
                                                        checkingIfUserLoggedInAndCallingTheIdtmSdk()
                                                    }, 2000
                                                )
                                            }
                                        }
                                    }
                                })
                        }

                    }
                }
            }
        }


        disableNetworkChangeCallBack()

        apiViewModel =
            ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory(application)).get(
                ApisViewModel::
                class.java
            )

        registerReceiver(gpsReceiver, IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION));


        //init fused location services
        init()
        // restore the values from saved instance state
        restoreValuesFromBundle(savedInstanceState)

        val connManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val mWifi: NetworkInfo =
            connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)

        if (mWifi.isConnected()) {
            if (checkPermissions() && isLocationEnabled()) {
                //afterPermission()
                checkingVDFTermsAndConditionsAndCallingM_Search()
                calledIdtmSdkFromOnCrete = true
                LibreLogger.d(this, "fused location api suma in onresume1")
            } else {
                //atul: below is the logic on ONRESUME for app and gps location , same added in oncreate
                Dexter.withActivity(this@VodafoneLoginRegisterWebViewActivity)
                    .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                    .withListener(object : PermissionListener {
                        override fun onPermissionGranted(response: PermissionGrantedResponse) {
                            calledLocationUpdatesFromOnCreate = true
                            startLocationUpdates()
                        }

                        override fun onPermissionDenied(response: PermissionDeniedResponse) {
                            if (response.isPermanentlyDenied) {
                                // open device settings when the permission is
                                // denied permanently
                                //atul: instead of show setting dilaog call ur custom dialog here
                                //show custom dialog on deny
                                isUserPermanentlyDenied = true
                                showCustomDialogPermission()
                            } else {
                                Log.d(TAG, "User clikced on deny")
                                callDexterPermission()
                            }

                        }

                        override fun onPermissionRationaleShouldBeShown(
                            permission: PermissionRequest,
                            token: PermissionToken
                        ) {
                            token.continuePermissionRequest()
                        }
                    }).check()
            }
        }
    }

    fun showCustomDialogPermission() {
        if (!showCustomLocationOnce) {
            showCustomLocationOnce = true
            utilClass.buildCustomAlertForLocationPermission(
                this@VodafoneLoginRegisterWebViewActivity,
                object : ONButtonClickFromCustomAlertDialog {
                    override fun onButtonClick(buttonName: String) {
                        showCustomLocationOnce = false
                        if (buttonName == "Try Again") {
                            if (checkPermissions() && isLocationEnabled()) {
                                checkingVDFTermsAndConditionsAndCallingM_Search()
                            } else {
                                //ask for permission
                                callDexterPermission()
                            }
                        } else {
                            val intent = Intent()
                            intent.action =
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            val uri =
                                Uri.fromParts("package", packageName, null)
                            intent.data = uri
                            startActivity(intent)
                        }
                    }
                })
        }
    }

    fun checkingVDFTermsAndConditionsAndCallingM_Search() {
        Handler().postDelayed({
            if (!isNetworkChangeDialogCalled) {
                if (getSharedPreferences("Libre", Context.MODE_PRIVATE).getBoolean(
                        "isTAndCAgreed",
                        false
                    )
                ) {
                    calledIdtmSdkFromOnCrete = true
                    showLotteAnimationView()
                    mMyTaskRunnableForMSearch.run()
                    //call the m-search to discover the device
                    Log.d(TAG, "calling m-search")

                    try {
                        unregisterReceiver(gpsReceiver)
                    } catch (e: IllegalArgumentException) {
                        e.printStackTrace()
                    }
                    checkingIfUserLoggedInAndCallingTheIdtmSdk()
                } else {
                    //take the user to T and C screen
                    val intent = Intent(
                        this@VodafoneLoginRegisterWebViewActivity,
                        VdfTermsAndConditionsActivity::class.java
                    )
                    startActivity(intent)
                    finish()
                }
            }
        }, 200)
    }

    private fun checkPermissions(): Boolean {
        val permissionState = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        return permissionState == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Restoring values from saved instance state
     */
    private fun restoreValuesFromBundle(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey("is_requesting_updates")) {
                mRequestingLocationUpdates = savedInstanceState.getBoolean("is_requesting_updates")
            }
            if (savedInstanceState.containsKey("last_known_location")) {
                mCurrentLocation =
                    savedInstanceState.getParcelable("last_known_location")
            }
            if (savedInstanceState.containsKey("last_updated_on")) {
                mLastUpdateTime = savedInstanceState.getString("last_updated_on")
            }
        }
    }

    private fun init() {
        mFusedLocationClient =
            LocationServices.getFusedLocationProviderClient(this)
        mSettingsClient = LocationServices.getSettingsClient(this)
        mLocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                // location is received
                mCurrentLocation = locationResult.lastLocation
                mLastUpdateTime = DateFormat.getTimeInstance().format(Date())
            }
        }
        mRequestingLocationUpdates = false
        mLocationRequest = LocationRequest()

        mLocationRequest!!.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY)
        mLocationRequest!!.setSmallestDisplacement(30f) //higher priority
        //setInterval as above 1 mins.
        mLocationRequest!!.setInterval(60000) // Update location every 1 minute
        mLocationRequest!!.setFastestInterval(10000)
        val builder =
            LocationSettingsRequest.Builder()
        builder.addLocationRequest(mLocationRequest!!)
        mLocationSettingsRequest = builder.build()
    }

    /**
     * Starting location updates
     * Check whether location settings are satisfied and then
     * location updates will be requested
     */
    private fun startLocationUpdates() {
        mRequestingLocationUpdates = true
        mSettingsClient
            ?.checkLocationSettings(mLocationSettingsRequest)
            ?.addOnSuccessListener(this, OnSuccessListener<LocationSettingsResponse?> {
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                    != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                    != PackageManager.PERMISSION_GRANTED
                ) {
                }
                mFusedLocationClient!!.requestLocationUpdates(
                    mLocationRequest,
                    mLocationCallback, Looper.myLooper()
                )
                Log.d(TAG, "gps_is_on_need_to_call_m-search")
            })
            ?.addOnFailureListener(this, OnFailureListener { e ->
                val statusCode = (e as ApiException).statusCode
                when (statusCode) {
                    LocationSettingsStatusCodes.RESOLUTION_REQUIRED ->
                        // Log.i("LSSDP", "Location settings are not satisfied. Attempting to upgrade " +
                        // "location settings ");
                        try {
                            // Show the dialog by calling startResolutionForResult(), and check the
                            // result in onActivityResult().
                            val rae = e as ResolvableApiException
                            rae.startResolutionForResult(
                                this@VodafoneLoginRegisterWebViewActivity, 100
                            )
                        } catch (sie: IntentSender.SendIntentException) {
                            // Log.i(TAG, "PendingIntent unable to execute request.");
                        }
                    LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                        val errorMessage =
                            "Location settings are inadequate, and cannot be " +
                                    "fixed here. Fix in Settings."
                        //  Log.e(TAG, errorMessage);
                        Toast.makeText(
                            this@VodafoneLoginRegisterWebViewActivity,
                            errorMessage,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            })
    }

    fun callDexterPermission() {
        //            LibreLogger.d(this, "fused location api suma in onresume2")
        Dexter.withActivity(this@VodafoneLoginRegisterWebViewActivity)
            .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(response: PermissionGrantedResponse) {
                    mRequestingLocationUpdates = true
                    startLocationUpdates()
                }

                override fun onPermissionDenied(response: PermissionDeniedResponse) {
                    if (response.isPermanentlyDenied) {
                        // open device settings when the permission is
                        // denied permanently
                        isUserPermanentlyDenied = true
                        showCustomDialogPermission()
                    } else {

                        callDexterPermission()
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permission: PermissionRequest,
                    token: PermissionToken
                ) {
                    token.continuePermissionRequest()
                }
            }).check()

    }

    fun isLocationEnabled(): Boolean {
        val locationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) && !locationManager.isProviderEnabled(
                LocationManager.NETWORK_PROVIDER
            )
        ) {
            //All location services are disabled
            LibreApplication.doneLocationChange = false
            false
        } else {
            LibreApplication.doneLocationChange = true
            true
        }
    }


    fun checkingIfUserLoggedInAndCallingTheIdtmSdk() {
        if (!getSharedPreferences("Libre", Context.MODE_PRIVATE).getBoolean(
                "isUserLoggedIn",
                false
            )
        ) {
            initIdtmSdk()
        } else {
            /** Need to take the user to the Dinner time devices
             * screen if the following conditions are met
             * 1:RounteCertPaired is true
             * 2:Amazon login done
             * alex skill is enabled
             * */
            showLotteAnimationView()
            /** getting genie model from the db */
            AuthenticateThreadGettingLatestToken().start()
        }
    }

    private fun afterPermit() {
        if (!isLocationEnabled()) {
            LibreLogger.d(this, "Location is disabled")
            askToEnableLocationService()
        } else {
            checkingVDFTermsAndConditionsAndCallingM_Search()
        }
    }

    private fun askToEnableLocationService() {
        val builder =
            AlertDialog.Builder(this@VodafoneLoginRegisterWebViewActivity)
        builder.setTitle(resources.getString(R.string.locationServicesIsOff))
            .setMessage(resources.getString(R.string.enableLocationPermit))
            .setPositiveButton(
                resources.getString(R.string.gotoSettings)
            ) { dialogInterface, i ->
                dialogInterface.cancel()
                turnGPSOn()
            }
        builder.create()
        builder.show()
    }

    @TargetApi(Build.VERSION_CODES.M)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        if (requestCode == Constants.PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION
            && grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            // Do something with granted permission
            afterPermit();
        } else if (requestCode == Constants.PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION
            && grantResults[0] == PackageManager.PERMISSION_DENIED
        ) {
            if (!shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                // user checked Never Ask again
                Log.d("asking permit", "permit ACCESS_COARSE_LOCATION Denied for ever")
                // show dialog
                val requestPermission =
                    AlertDialog.Builder(this@VodafoneLoginRegisterWebViewActivity)
                requestPermission.setTitle(getString(R.string.permitNotAvailable))
                    .setMessage(getString(R.string.permissionMsg))
                    .setPositiveButton(
                        getString(R.string.gotoSettings)
                    ) { dialog, which -> //navigate to settings
                        dialog.dismiss()

                    }
                    .setCancelable(false)
                if (alert == null) {
                    alert = requestPermission.create()
                }
                if (alert != null && !alert.isShowing) alert.show()
            }
        }
    }


    private val mMyTaskRunnableForMSearch: Runnable = Runnable {
        val application = application as LibreApplication
        application.scanThread.UpdateNodes()
        Log.d(TAG, "m-Search called")
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

                checkingDBForGenieDevice(object : OnGettingGenieDeviceFromDBInterface {
                    override fun onSuccessfullyGettingTheDevice() {
                        Handler(Looper.getMainLooper()).post {
                            addDeviceToCloudIfDeviceIsNoPresent(getGenieDevice()!!)
                        }
                    }
                })
            } catch (e: Exception) {

                runOnUiThread(Runnable {
                    stopLotteAnimationView()
                    Log.d(TAG, "authenticate sync onError", e)
                })

                try {
                    LibreApplication.getIdtmLib(this@VodafoneLoginRegisterWebViewActivity).logout()
                    LibreApplication.getIdtmLib(this@VodafoneLoginRegisterWebViewActivity)
                        .eraseAll()
                    LibreApplication.getIdtmLib(this@VodafoneLoginRegisterWebViewActivity)
                        .clearHashes()
                } catch (e: IdtmInProgressException) {
                    LibreApplication.getIdtmLib(this@VodafoneLoginRegisterWebViewActivity)
                        .eraseAll()
                    LibreApplication.getIdtmLib(this@VodafoneLoginRegisterWebViewActivity)
                        .clearHashes()
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
        if (checkPermissions()) {
            if (!isLocationEnabled()) {
                LibreLogger.d(this, "Location is disabled")
                if (!calledLocationUpdatesFromOnCreate) {
                    startLocationUpdates()
                }
            } else {
                mRequestingLocationUpdates = true
                if (!calledIdtmSdkFromOnCrete) {
                    Log.d(TAG, "networkChange_calledTermsAndCondtions from onResume")
                    checkingVDFTermsAndConditionsAndCallingM_Search()
                }
            }
        } else {
            if (isUserPermanentlyDenied) {
                showCustomDialogPermission()
            }
        }
    }

    private val gpsReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            if (intent.action == (LocationManager.PROVIDERS_CHANGED_ACTION)) {
                //Do your stuff on GPS status change
                if (checkPermissions() && isLocationEnabled()) {
                    Log.d(TAG, "networkChange_afterPermit called from gpsReceiver")
                    afterPermit()
                }
            }
        }
    }


    fun initIdtmSdk() {
        val worker = InitliazeIdtmLibAsyncTask(this@VodafoneLoginRegisterWebViewActivity)
        val workthread = Thread(worker)
        workthread.start()
    }


    fun showLotteAnimationView() {
        tvLoadingReason.text = "Please wait..."
        tvLoadingReason.visibility = View.VISIBLE
        if (lotteAnimationView != null) {
            lotteAnimationView.setAnimation("vf_spinner_red_large_300.json")
            lotteAnimationView.repeatCount = LottieDrawable.INFINITE
            lotteAnimationView.tag = "lotteAnimation"
            lotteAnimationView.playAnimation()
        }
    }

    private fun turnGPSOn() {
        val intent = Intent(
            Settings.ACTION_LOCATION_SOURCE_SETTINGS
        )
        startActivity(intent)
    }

    fun stopLotteAnimationView() {
        if (lotteAnimationView != null) {
            tvLoadingReason.visibility = View.GONE
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
                            (this@VodafoneLoginRegisterWebViewActivity as AppCompatActivity).window.decorView.findViewById(
                                android.R.id.content
                            ),
                            "Seems your internet connection is slow, please try in sometime"
                        )
                        //show timeout try again error dialog


                    } else if (volleyError is AuthFailureError) {

                        utilClass.buildSnackBarWithoutButton(
                            this@VodafoneLoginRegisterWebViewActivity,
                            (this@VodafoneLoginRegisterWebViewActivity as AppCompatActivity).window.decorView.findViewById(
                                android.R.id.content
                            ),
                            "AuthFailure error occurred, please try again later"
                        )

                    } else if (volleyError is ServerError) {

                        utilClass.buildSnackBarWithoutButton(
                            this@VodafoneLoginRegisterWebViewActivity,
                            (this@VodafoneLoginRegisterWebViewActivity as AppCompatActivity).window.decorView.findViewById(
                                android.R.id.content
                            ),
                            "Server error occurred, please try again later"
                        )

                    } else if (volleyError is NetworkError) {

                        utilClass.buildSnackBarWithoutButton(
                            this@VodafoneLoginRegisterWebViewActivity,
                            (this@VodafoneLoginRegisterWebViewActivity as AppCompatActivity).window.decorView.findViewById(
                                android.R.id.content
                            ),
                            "Network error occurred, please try again later"
                        )

                    } else if (volleyError is ParseError) {
                        utilClass.buildSnackBarWithoutButton(
                            this@VodafoneLoginRegisterWebViewActivity,
                            (this@VodafoneLoginRegisterWebViewActivity as AppCompatActivity).window.decorView.findViewById(
                                android.R.id.content
                            ),
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
                            utilClass.buildSnackBarWithoutButton(
                                this, window.decorView.findViewById(android.R.id.content)
                                , "This device is already registered to a particular account"
                            )
                        } else {
                            //show timeout try again error dialog
                        }
                        Log.d(
                            TAG, "deviceSentError: ".plus(
                                modelGetAllDeviceRepo.modelDeviceAddUserResponse!!
                                    .msg.plus(" code: ")
                                    .plus(modelGetAllDeviceRepo.modelDeviceAddUserResponse?.code)
                            )
                        )

                    }

                } else {
                    stopLotteAnimationView()
                    Log.d(TAG, modelGetAllDeviceRepo.volleyError.toString())

                    val volleyError = modelGetAllDeviceRepo.volleyError

                    if (volleyError is TimeoutError || volleyError is NoConnectionError) {

                        utilClass.buildSnackBarWithoutButton(
                            this@VodafoneLoginRegisterWebViewActivity,
                            (this@VodafoneLoginRegisterWebViewActivity as AppCompatActivity).window.decorView.findViewById(
                                android.R.id.content
                            ),
                            "Seems your internet connection is slow, please try in sometime"
                        )
                        //show timeout try again error dialog


                    } else if (volleyError is AuthFailureError) {

                        utilClass.buildSnackBarWithoutButton(
                            this@VodafoneLoginRegisterWebViewActivity,
                            (this@VodafoneLoginRegisterWebViewActivity as AppCompatActivity).window.decorView.findViewById(
                                android.R.id.content
                            ),
                            "AuthFailure error occurred, please try again later"
                        )

                    } else if (volleyError is ServerError) {

                        utilClass.buildSnackBarWithoutButton(
                            this@VodafoneLoginRegisterWebViewActivity,
                            (this@VodafoneLoginRegisterWebViewActivity as AppCompatActivity).window.decorView.findViewById(
                                android.R.id.content
                            ),
                            "Server error occurred, please try again later"
                        )

                    } else if (volleyError is NetworkError) {

                        utilClass.buildSnackBarWithoutButton(
                            this@VodafoneLoginRegisterWebViewActivity,
                            (this@VodafoneLoginRegisterWebViewActivity as AppCompatActivity).window.decorView.findViewById(
                                android.R.id.content
                            ),
                            "Network error occurred, please try again later"
                        )

                    } else if (volleyError is ParseError) {
                        utilClass.buildSnackBarWithoutButton(
                            this@VodafoneLoginRegisterWebViewActivity,
                            (this@VodafoneLoginRegisterWebViewActivity as AppCompatActivity).window.decorView.findViewById(
                                android.R.id.content
                            ),
                            "Parser error occurred, please try again later"
                        )
                    }
                }
            })
    }

    fun getGenieDevice(): LSSDPNodes? {
        for (nodes in LSSDPNodeDB.getInstance().GetDB()) {
            if (nodes.deviceState != null) {
                return nodes
            }
        }
        return null
    }

    fun checkingDBForGenieDevice(onGettingGenieDeviceFromDBInterface: OnGettingGenieDeviceFromDBInterface) {
        mHandler.sendEmptyMessageDelayed(M_Search_Timeout, 30000)
        Log.d(TAG, "starting timer for m-search")
        var timer = Timer()
        timer.schedule(object : TimerTask() {
            override fun run() {
                if (LSSDPNodeDB.getInstance().GetDB().size > 0) {
                    Log.d(
                        TAG,
                        "checking every half a second for .5 seconds for 30 secs".plus("device name: ")
                            .plus(LSSDPNodeDB.getInstance().GetDB()[0].friendlyname)
                    )
                    timer.cancel()
                    mHandler.removeCallbacksAndMessages(M_Search_Timeout)
                    onGettingGenieDeviceFromDBInterface.onSuccessfullyGettingTheDevice()
                }
            }
        }, 500, 30000)
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

        overridePendingTransition(
            R.anim.left_to_right_anim_tranistion,
            R.anim.right_to_left_anim_transition
        );

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
        mHandler.removeCallbacksAndMessages(M_Search_Timeout)
        unRegisterForDeviceEvents()
        try {
            unregisterReceiver(gpsReceiver)
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        }
    }

    fun checkAlexaStatus(node: LSSDPNodes) {
        val luciControl = LUCIControl(node?.ip)
        luciControl.SendCommand(
            MIDCONST.CHECK_ALEXA_LOGIN_STATUS,
            "GETLOGINSTAT",
            LSSDPCONST.LUCI_SET
        )

        Log.d(TAG, "calling_login_stat")
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
                checkingDBForGenieDevice(object : OnGettingGenieDeviceFromDBInterface {
                    override fun onSuccessfullyGettingTheDevice() {
                        Handler(Looper.getMainLooper()).post {
                            addDeviceToCloudIfDeviceIsNoPresent(getGenieDevice()!!)
                        }
                    }
                })
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
                if (LibreApplication.wifiConnected) {
                    utilClass.buildSnackBarWithoutButton(
                        this@VodafoneLoginRegisterWebViewActivity,
                        window.decorView.findViewById(android.R.id.content),
                        "authenticate Error".plus(e.toString())

                    )
                    LibreApplication.getIdtmLib(this@VodafoneLoginRegisterWebViewActivity)
                        .authenticate(true, null, MyAuthenticateObserver())
                }
            }
        }
    }


    inner class InitliazeIdtmLibAsyncTask(context: Context) : Runnable {
        private var contextRef: WeakReference<Context>? = null

        init {
            contextRef = WeakReference(context)
        }

        override fun run() {
            Log.d(TAG, "idtm called intialize method")
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
            utilClass.buildSnackBarWithoutButton(
                this@VodafoneLoginRegisterWebViewActivity,
                window.decorView.findViewById(android.R.id.content), "idtm init:$e"
            )
            LibreLogger.d(this, "initialize onError$e")
        }

        override fun onComplete() {
            LibreLogger.d(this, "initialize onComplete")
            LibreApplication.getIdtmLib(this@VodafoneLoginRegisterWebViewActivity)
                .authenticate(true, null, MyAuthenticateObserver())
        }
    }

    override fun newDeviceFound(node: LSSDPNodes?) {
        mNode = node
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
            when (packet.command) {
                208 -> {
                    if (!payloadValueString.isEmpty()) {
                        if (payloadValueString.contains("RouterCertPaired")) {
                            val parts: Array<String> =
                                payloadValueString.split(":".toRegex()).toTypedArray()
                            val envVoxReadValue = parts[1]
                            Log.d(TAG, "EnvValueRouterCertPaired$envVoxReadValue")
                            envVoxRouterPairedValue = envVoxReadValue.toBoolean()
                            mNode?.envVoxReadValue = envVoxReadValue

                            /** if the routerCertPaired is true */
                            if (envVoxReadValue.toBoolean()) {
                                checkAlexaStatus(mNode!!)
                            } else {
                                /** if the routerCertPaired is false */
                                bgThread.post {
                                    if (mNode != null) {
                                        if (mNode?.envVoxReadValue != null) {
                                            Log.d(TAG, "envValue: ".plus(mNode?.envVoxReadValue!!))
                                            gotoSwfPairedActivityOrPairingDinnerTimeDeviceListScreen(
                                                mNode!!,
                                                mNode?.envVoxReadValue!!.toBoolean()
                                            )
                                        } else {
                                            readEnvVox(mNode?.ip)
                                        }
                                    } else {
                                        showErrorSnackbar()
                                    }
                                }
                            }
                        }
                    }
                }
                MIDCONST.CHECK_ALEXA_LOGIN_STATUS -> {
                    val payload: String = packet.getpayload().toString(Charsets.UTF_8)
                    Log.d(TAG, "receivedSkillPayloadMessage: $payload")
                    when (payload) {
                        "NOLOGIN" -> {
                            updateAlexaEnablementStatus(false)
                            bgThread.post {
                                if (mNode != null) {
                                    if (mNode?.envVoxReadValue != null) {
                                        Log.d(TAG, "envValue: ".plus(mNode?.envVoxReadValue!!))
                                        gotoSwfPairedActivityOrPairingDinnerTimeDeviceListScreen(
                                            mNode!!,
                                            mNode?.envVoxReadValue!!.toBoolean()
                                        )
                                    } else {
                                        readEnvVox(mNode?.ip)
                                    }
                                } else {
                                    showErrorSnackbar()
                                }
                            }
                        }
                        "LOGGEDIN", "READY"
                        -> {
                            updateAlexaEnablementStatus(true)
                            bgThread.post {
                                if (mNode != null) {
                                    if (mNode?.envVoxReadValue != null) {
                                        Log.d(TAG, "envValue: ".plus(mNode?.envVoxReadValue!!))
                                        gotoSwfPairedActivityOrPairingDinnerTimeDeviceListScreen(
                                            mNode!!,
                                            mNode?.envVoxReadValue!!.toBoolean()
                                        )
                                    } else {
                                        readEnvVox(mNode?.ip)
                                    }
                                } else {
                                    showErrorSnackbar()
                                }
                            }
                        }
                    }
                }

            }
        }

    }

    fun showErrorSnackbar() {
        utilClass.buildSnackBarWithoutButton(
            this@VodafoneLoginRegisterWebViewActivity,
            window.decorView.findViewById(android.R.id.content),
            "Something Went Wrong,Please try again"
        )
        Handler().postDelayed({
            finish()
        }, 2000)
    }

    fun updateAlexaEnablementStatus(status: Boolean) {
        val sharedPreferences = getSharedPreferences("Libre", Context.MODE_PRIVATE)
        val editor: SharedPreferences.Editor = sharedPreferences.edit()
        editor.putBoolean("alexaSkillEnablement", status)
        editor.apply()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

    override fun deviceDiscoveryAfterClearingTheCacheStarted() {

    }
}
