package com.libre.alexa.viewmodels

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Response
import com.android.volley.VolleyLog
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.libre.alexa.models.*
import com.libre.alexa.utils.ApiConstants
//import com.vodafone.idtmlib.lib.network.Response
import org.json.JSONObject

class ApisViewModel(application: Application) : AndroidViewModel(application) {

    private var getModelAddDeviceRepo: MutableLiveData<ModelAddDeviceRepo>? = null

    private var getAllDeviceRepo: MutableLiveData<ModelGetAllDeviceRepo>? = null

    private var getEditDeviceRepo: MutableLiveData<ModelEditDeviceRepo>? = null

    private var apisViewModel: ApisViewModel? = null


    val TAG = ApisViewModel::class.java.simpleName

    init {
        VolleyLog.DEBUG = true;
    }

    fun postModelAddUser(deviceId: String, friendlyName: String): LiveData<ModelAddDeviceRepo>? {
        if (getModelAddDeviceRepo == null) {
            getModelAddDeviceRepo = MutableLiveData<ModelAddDeviceRepo>()
            //call the api
            addDevicePosApi(deviceId, friendlyName)
        }
        return getModelAddDeviceRepo
    }

    fun getAllDevices(): LiveData<ModelGetAllDeviceRepo>? {
        if (getAllDeviceRepo == null) {
            getAllDeviceRepo = MutableLiveData<ModelGetAllDeviceRepo>()
            //call the api
            getAllDevicesGetApi()
        }
        return getAllDeviceRepo
    }

    fun getEditDeviceRepoResponse(
            deviceId: String,
            friendlyName: String
    ): LiveData<ModelEditDeviceRepo>? {
        if (getEditDeviceRepo == null) {
            getEditDeviceRepo = MutableLiveData<ModelEditDeviceRepo>()
            editDeviceNamePostApi(deviceId, friendlyName)
        }
        return getEditDeviceRepo
    }

    fun editDeviceNamePostApi(deviceId: String, friendlyName: String) {
        val context = getApplication<Application>().applicationContext

        val requestQueue = Volley.newRequestQueue(context)

        var uri = Uri.parse(ApiConstants.LIBRE_CLOUD_BASE_URL.plus("v1/vdf/deviceanduser/"))
                .buildUpon()


        val jsonObjectRequest: JsonObjectRequest = object : JsonObjectRequest(
                Method.POST,
                uri.toString(),
                null,
                Response.Listener { response ->

                    val modelEditDeviceRepo = ModelEditDeviceRepo()

                    val modelEditDeviceResponse = ModelAllDevicesResponse()

                    modelEditDeviceResponse.code = response.getInt("code")
                    modelEditDeviceResponse.msg = response.getString("msg")

                    modelEditDeviceRepo.modelEditDeviceResponse = modelEditDeviceResponse
                    getEditDeviceRepo?.value = modelEditDeviceRepo

                },
                Response.ErrorListener { volleyError ->
                    val modelEditDeviceRepo = ModelEditDeviceRepo()

                    modelEditDeviceRepo.volleyError = volleyError
                    getEditDeviceRepo?.value = modelEditDeviceRepo
                }) {
            override fun getHeaders(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["Authorization"] = "Bearer ".plus(
                        context.getSharedPreferences("Libre", Context.MODE_PRIVATE)
                                .getString("accessToken", null)
                )
                params["Content-Type"] = "application/x-www-form-urlencoded"
                return params
            }

            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["action"] = "edit"
                params["deviceId"] = deviceId
                params["friendlyName"] = friendlyName
                return params
            }
        }
        jsonObjectRequest.retryPolicy = DefaultRetryPolicy(
                30000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )

        requestQueue.add(jsonObjectRequest)

    }

    fun updateAccessToken(token: String, context: Context) {
        val sharedPref: SharedPreferences =
                context.getSharedPreferences("Libre", Context.MODE_PRIVATE)
        val editor: SharedPreferences.Editor = sharedPref.edit()
        editor.putString("accessToken", token)
        editor.apply()

    }


    fun getAllDevicesGetApi() {
        val context = getApplication<Application>().applicationContext

        val requestQueue = Volley.newRequestQueue(context)

        var uri = Uri.parse(ApiConstants.LIBRE_CLOUD_BASE_URL.plus("v1/vdf/deviceanduser/"))
                .buildUpon()


        val jsonStringRequest: StringRequest = object : StringRequest(Method.POST, uri.toString(), Response.Listener { response ->

            val responseObject = JSONObject(response)

            val modelGetAllDeviceRepo = ModelGetAllDeviceRepo()

            val modelAllDevicesResponse = ModelAllDevicesResponse()

            val modelAllDevicesResponseList: MutableList<ModelGetAllDevicesResponse> =
                    ArrayList()

            modelAllDevicesResponse.code = responseObject.getInt("code")
            modelAllDevicesResponse.msg = responseObject.getString("msg")

            if (responseObject.has("devicelist")) {

                val deviceListJSonArray = responseObject.getJSONArray("devicelist")

                for (i in 0 until deviceListJSonArray.length()) {

                    val deviceJsonObject: JSONObject = deviceListJSonArray[i] as JSONObject

                    val modelGetAllDevicesResponse = ModelGetAllDevicesResponse()
                    modelGetAllDevicesResponse.deviceId =
                            deviceJsonObject.getString("deviceId")
                    modelGetAllDevicesResponse.friendlyName =
                            deviceJsonObject.getString("friendlyName")

                    modelAllDevicesResponseList.add(modelGetAllDevicesResponse)
                }
            }
            modelAllDevicesResponse.modelGetAllDevicesResponseList = modelAllDevicesResponseList

            modelGetAllDeviceRepo.modelGetAllDeviceRepo = modelAllDevicesResponse
            getAllDeviceRepo?.value = modelGetAllDeviceRepo

        }, Response.ErrorListener {

            volleyError ->
            val modelGetAllDeviceRepo = ModelGetAllDeviceRepo()
            modelGetAllDeviceRepo.volleyError = volleyError
            getAllDeviceRepo?.value = modelGetAllDeviceRepo

        }) {
            override fun getHeaders(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["Authorization"] = "Bearer ".plus(
                        context.getSharedPreferences("Libre", Context.MODE_PRIVATE)
                                .getString("accessToken", null)
                )
                params["Content-Type"] = "application/x-www-form-urlencoded"
                return params
            }

            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["action"] = "getAll"
                return params
            }
        }

        jsonStringRequest.retryPolicy = DefaultRetryPolicy(
                30000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )

        requestQueue.add(jsonStringRequest)
    }


    fun addDevicePosApi(deviceId: String, friendlyName: String) {
        val context = getApplication<Application>().applicationContext

        val requestQueue = Volley.newRequestQueue(context)

        var uri = Uri.parse(ApiConstants.LIBRE_CLOUD_BASE_URL.plus("v1/vdf/deviceanduser/"))
                .buildUpon()

        val stringRequest: StringRequest = object : StringRequest(Method.POST, uri.toString(),
                Response.Listener { response ->
                    val modelAddDeviceRepo = ModelAddDeviceRepo()
                    val modelDeviceAddUserResponse = ModelDeviceAddUserResponse()

                    val responseObject = JSONObject(response)

                    modelDeviceAddUserResponse.code = responseObject.getString("code")
                    modelDeviceAddUserResponse.msg = responseObject.getString("msg")

                    modelAddDeviceRepo.modelDeviceAddUserResponse = modelDeviceAddUserResponse
                    getModelAddDeviceRepo?.value = modelAddDeviceRepo

                }, Response.ErrorListener { volleyError ->
            val modelAddDeviceRepo = ModelAddDeviceRepo()
            modelAddDeviceRepo.volleyError = volleyError

            getModelAddDeviceRepo?.value = modelAddDeviceRepo
        }) {
            override fun getHeaders(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["Authorization"] = "Bearer ".plus(
                        context.getSharedPreferences("Libre", Context.MODE_PRIVATE)
                                .getString("accessToken", null)
                )
                params["Content-Type"] = "application/x-www-form-urlencoded"
                Log.d(TAG, "addDevicePost".plus(params))
                return params
            }

            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["action"] = "add"
                params["deviceId"] = deviceId
                params["friendlyName"] = friendlyName
                Log.d(TAG, "addDeviceApi: ".plus(params))
                return params
            }
        }

        stringRequest.retryPolicy = DefaultRetryPolicy(
                30000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )

        requestQueue.add(stringRequest)
    }


}