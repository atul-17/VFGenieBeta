package com.libre.alexa.activities.newFlow

import android.app.AlertDialog
import android.app.Dialog
import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.LinearLayoutManager
import com.libre.alexa.DeviceDiscoveryActivity
import com.libre.alexa.R
import com.libre.alexa.adapter.ShowDevicesAdapter
import com.libre.alexa.constants.LSSDPCONST
import com.libre.alexa.constants.MIDCONST
import com.libre.alexa.luci.LSSDPNodes
import com.libre.alexa.luci.LUCIControl
import com.libre.alexa.luci.LUCIPacket
import com.libre.alexa.models.ModelStoreDeviceDetails
import com.libre.alexa.netty.LibreDeviceInteractionListner
import com.libre.alexa.netty.NettyData
import com.libre.alexa.utils.UtilClass
import com.libre.alexa.utils.interfaces.AddRemovedCheckedDevicesInterface
import kotlinx.android.synthetic.main.activity_select_devices_for_dinner_time.*
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.lang.Boolean
import java.util.*
import kotlin.collections.ArrayList

class VodafoneAlexaDinnerTimeDeviceListActivity : DeviceDiscoveryActivity(),
        AddRemovedCheckedDevicesInterface, LibreDeviceInteractionListner {


    lateinit var toolbar: View

    lateinit var ivInfo: AppCompatImageView

    lateinit var ivAppSettings: AppCompatImageView

    val utilClass = UtilClass()

    var bundle = Bundle()

    var node: LSSDPNodes? = null

    var TAG = VodafoneAlexaDinnerTimeDeviceListActivity::class.java.simpleName

    var showDevicesAdapter: ShowDevicesAdapter? = null

    var modelStoreDeviceDetailsList: MutableList<ModelStoreDeviceDetails> = ArrayList()

    var modelStoreDeviceDetailsListForPostApi: MutableList<ModelStoreDeviceDetails> = ArrayList()

    var jsonArray: JSONArray? = null

    var progressDialog: ProgressDialog? = null

    var dinnerTimeStatus: Int = 0


    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_devices_for_dinner_time)

        disableNetworkChangeCallBack()
        bundle = intent.extras!!
        if (bundle != null) {
            node = bundle.getSerializable("deviceDetails") as LSSDPNodes
        }

        registerForDeviceEvents(this)

        toolbar = findViewById(R.id.toolbar)

        ivInfo = toolbar.findViewById(R.id.ivInfo)

        ivAppSettings = toolbar.findViewById(R.id.ivAppSettings)

        ivInfo.setOnClickListener {
            //things to try
            val intent = Intent(
                    this@VodafoneAlexaDinnerTimeDeviceListActivity,
                    VodafoneThingsToTryActivity::class.java
            )
            val bundle = Bundle()
            bundle.putSerializable("deviceDetails", node)
            intent.putExtras(bundle)
            startActivity(intent)
            overridePendingTransition(R.anim.left_to_right_anim_tranistion,
                    R.anim.right_to_left_anim_transition);
        }

        ivAppSettings.setOnClickListener {
            //device settings
            val intent = Intent(
                    this@VodafoneAlexaDinnerTimeDeviceListActivity,
                    VodafoneAlexaDinnerTimeSettingsActivity::class.java
            )
            val bundle = Bundle()
            bundle.putSerializable("deviceDetails", node)
            intent.putExtras(bundle)
            startActivity(intent)
            overridePendingTransition(R.anim.left_to_right_anim_tranistion,
                    R.anim.right_to_left_anim_transition);
        }


        val linearLayoutManager =
                LinearLayoutManager(this@VodafoneAlexaDinnerTimeDeviceListActivity)

        show_device_recycler_view.layoutManager = linearLayoutManager

        if (node != null) {
            showLoader()
            readVOXControlValue(node!!.ip)
        }

        btnSave.setOnClickListener {
            showSucessFullMsg(
                    "Save Changes"
                    ,
                    this@VodafoneAlexaDinnerTimeDeviceListActivity.resources.getString(R.string.successfulSentDeviceChanges)
            )
        }

        if (node != null) {
            readDinnerTimer(node!!.ip)
        }

        createNewProgressDialog()
    }

    fun createNewProgressDialog() {
        progressDialog = ProgressDialog(this@VodafoneAlexaDinnerTimeDeviceListActivity)
        progressDialog?.setCancelable(false)
        progressDialog?.setMessage("Please Wait...")
    }


    private fun readVOXControlValue(ip: String) {
        LUCIControl(ip).SendCommand(MIDCONST.GENIE_DEVICE_STATUS, "0", LSSDPCONST.LUCI_SET)
    }

    override fun addCheckedDevice(modelStoreDeviceDetails: ModelStoreDeviceDetails?) {
        modelStoreDeviceDetailsListForPostApi.add(modelStoreDeviceDetails!!)
    }

    override fun removeUnCheckedDevice(macId: String?) {
        val iterator = modelStoreDeviceDetailsListForPostApi.iterator()
        while (iterator.hasNext()) {
            if (iterator.next().macId == macId) {
                iterator.remove()
            }
        }
    }

    fun showLoader() {
        progressDialog?.show()
    }

    fun dismissLoader() {
        progressDialog?.dismiss()
    }

    override fun newDeviceFound(node: LSSDPNodes?) {

    }

    override fun deviceGotRemoved(ipaddress: String?) {

    }

    override fun messageRecieved(dataRecived: NettyData?) {
        val packet = LUCIPacket(dataRecived?.getMessage())
        val voxPayload = String(packet.getpayload())
        Log.d(TAG, "19 msg\n$voxPayload")
        when (packet.command) {
            /** this receives the dinner time device list*/
            19
            -> if (voxPayload.contains("1::")) {
                node?.voxJsonArray = voxPayload
                modelStoreDeviceDetailsList = getDinnerTimeDevicesList(node)

                modelStoreDeviceDetailsList.sortWith(Comparator
                { modelStoreDeviceDetails1, modelStoreDeviceDetails2 ->
                    Boolean.compare(
                            modelStoreDeviceDetails2.isBlackListed,
                            modelStoreDeviceDetails1.isBlackListed
                    )
                })
                addBlackListedDeviceToPostArrayList(modelStoreDeviceDetailsList);
                setDinnerTimeDeviceAdapter(modelStoreDeviceDetailsList)
                runOnUiThread {
                    dismissLoader()
                }
            }
            21 -> {

                /** this data shows the timer for dinner time active */
                if (voxPayload.isNotEmpty()) {
                    val seconds = voxPayload.toInt()
                    if (seconds > 0) {
                        /** Dinner time status : Active*/
                        runOnUiThread {
                            btnSave.isEnabled = false
                            tvDinnerActiveInactive.text = "Dinner Time: Active"
                            llTimeRemaining.visibility = View.VISIBLE
                        }

                        runOnUiThread {
                            val minutes: Int = (seconds % 3600) / 60;
                            val totalSeconds = seconds % 60;

                            var minutesString = minutes.toString()
                            var secondsString = totalSeconds.toString()

                            if (minutes < 10) {
                                minutesString = "0".plus(minutesString)
                            }

                            if (totalSeconds < 10) {
                                secondsString = "0".plus(secondsString)
                            }

                            tvTimeRemaining.text = (minutesString).plus(":").plus(secondsString)

                        }
                    } else {
                        /** Dinner time status : InActive*/
                        runOnUiThread {
                            btnSave.isEnabled = true
                            tvDinnerActiveInactive.text = "Dinner Time: InActive"
                            llTimeRemaining.visibility = View.GONE
                        }
                    }
                }
            }
        }
    }

    override fun deviceDiscoveryAfterClearingTheCacheStarted() {

    }


    fun readDinnerTimer(ip: String) {
        LUCIControl(ip).SendCommand(
                MIDCONST.GENIE_DINNER_TIME_COUNTDOWNTIMER,
                "",
                LSSDPCONST.LUCI_GET
        )
    }


    fun addBlackListedDeviceToPostArrayList(modelStoreDeviceDetailsList: MutableList<ModelStoreDeviceDetails>) {
        for (modelStoreDeviceDetails: ModelStoreDeviceDetails in modelStoreDeviceDetailsList) {
            if (modelStoreDeviceDetails.isBlackListed) {
                modelStoreDeviceDetailsListForPostApi.add(modelStoreDeviceDetails)
            }
        }
    }

    fun setDinnerTimeDeviceAdapter(modelStoreDeviceDetailsList: MutableList<ModelStoreDeviceDetails>) {
        showDevicesAdapter = ShowDevicesAdapter(
                this@VodafoneAlexaDinnerTimeDeviceListActivity,
                modelStoreDeviceDetailsList
        )
        show_device_recycler_view.adapter = showDevicesAdapter
        showDevicesAdapter!!.setAddRemovedCheckedDevicesInterface(this)
    }

    fun getDinnerTimeDevicesList(node: LSSDPNodes?): MutableList<ModelStoreDeviceDetails> {
        var voxJsonArray: String? = null
        if (node != null) {
            voxJsonArray = node.voxJsonArray
            if (voxJsonArray != null) {
                if (voxJsonArray.contains("1::")) {

                    val removemessage = voxJsonArray.toString().replace("1::", "")

                    Log.d(
                            TAG,
                            "voxPayload after removing 1${removemessage}get json array${node?.voxJsonArray}"
                    )

                    var mainObj: JSONObject? = null
                    try {
                        mainObj = JSONObject(removemessage)
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                    try {
                        if (mainObj != null) {
                            val scanListArray = mainObj.getJSONArray("deviceList")
                            if (scanListArray.length() > 0) {

                                modelStoreDeviceDetailsList =
                                        java.util.ArrayList<ModelStoreDeviceDetails>()
                                try {
                                    for (i in 0 until scanListArray.length()) {
                                        val obj = scanListArray[i] as JSONObject
                                        val modelStoreDeviceDetails = ModelStoreDeviceDetails(
                                                obj.getString("isActive"),
                                                obj.getBoolean("isBlackListed"),
                                                obj.getString("macid"),
                                                obj.getString("name"),
                                                obj.getString("hostIcon"),
                                                obj.getString("hostType")
                                        )

                                        modelStoreDeviceDetailsList?.add(modelStoreDeviceDetails)

                                        Log.d(
                                                TAG,
                                                "vox payload json object" + obj.getString("name")
                                        )
                                    }
                                } catch (e: JSONException) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                }
            }
        }
        return modelStoreDeviceDetailsList
    }

    fun makeJsonForPostingTheDevice() {
        jsonArray = JSONArray()
        try {
            for (modelStoreDeviceDetails in modelStoreDeviceDetailsListForPostApi) {
                val jsonObject = JSONObject()
                jsonObject.put("isActive", modelStoreDeviceDetails.isActive)
                jsonObject.put("isBlackListed", modelStoreDeviceDetails.isBlackListed)
                jsonObject.put("macid", modelStoreDeviceDetails.macId)
                jsonObject.put("name", modelStoreDeviceDetails.name)
                jsonArray?.put(jsonObject)
            }
            val mainObject = JSONObject()
            mainObject.put("deviceList", jsonArray)
            val jsonData = "2::$mainObject"

            Log.d(TAG, "postingDinnerTimeDevices$jsonData")
            writeVoxControlValue(node!!.ip, jsonData)

            //reading dinner time
            if (node != null) {
                readDinnerTimer(node!!.ip)
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    private fun writeVoxControlValue(ip: String, finalJsonData: String) {
        LUCIControl(ip).SendCommand(
                MIDCONST.GENIE_DEVICE_STATUS,
                finalJsonData,
                LSSDPCONST.LUCI_SET
        )
    }


    fun showSucessFullMsg(title: String, message: String) {
        //Are you sure save the changes
        if (!isFinishing) {
            val builder: AlertDialog.Builder =
                    AlertDialog.Builder(this@VodafoneAlexaDinnerTimeDeviceListActivity, R.style.CustomAlertDialog)

            val viewGroup: ViewGroup = findViewById(android.R.id.content)

            val dialogView: View =
                    LayoutInflater.from(this@VodafoneAlexaDinnerTimeDeviceListActivity)
                            .inflate(R.layout.custom_confirm_alert, viewGroup, false)

            builder.setView(dialogView);

            builder.setCancelable(false)

            val alertDialog: AlertDialog = builder.create()


            val tvAlertTitle: AppCompatTextView =
                    dialogView!!.findViewById(com.libre.alexa.R.id.tv_alert_title)

            val tvAlertMessage: AppCompatTextView =
                    dialogView!!.findViewById(com.libre.alexa.R.id.tv_alert_message)

            val btnOK: AppCompatButton =
                    dialogView!!.findViewById(com.libre.alexa.R.id.btn_ok)

            val btnCancel: AppCompatButton =
                    dialogView!!.findViewById(com.libre.alexa.R.id.btn_cancel)


            tvAlertTitle.text = title
            tvAlertMessage.text = message


            btnOK.setOnClickListener(View.OnClickListener {
                alertDialog!!.dismiss()
                val intent =
                        Intent(this@VodafoneAlexaDinnerTimeDeviceListActivity, VodafoneLoginRegisterWebViewActivity::class.java)
                startActivity(intent)
                finish()
            })

            btnCancel.setOnClickListener(View.OnClickListener {

                alertDialog!!.dismiss()
            })

            alertDialog!!.show()

        }
    }

}