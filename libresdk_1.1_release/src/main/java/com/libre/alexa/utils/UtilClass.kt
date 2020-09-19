package com.libre.alexa.utils


import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.provider.Settings
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import com.android.volley.*
import com.android.volley.toolbox.HurlStack
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.snackbar.Snackbar
import com.google.common.reflect.Reflection.getPackageName
import com.libre.alexa.R
import com.libre.alexa.util.LibreLogger
import com.libre.alexa.utils.interfaces.ApiSucessCallbackInterface
import com.libre.alexa.utils.interfaces.ONButtonClickFromCustomAlertDialog
import com.libre.alexa.utils.interfaces.SucessCallbackInterfaceWithParams
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import kotlin.collections.HashMap


class UtilClass {


    fun buildSnackBarWithoutButton(context: Context, view: View, message: String) {
        val snackbar = Snackbar.make(view, message, Snackbar.LENGTH_LONG)
        snackbar.view.setBackgroundColor(ContextCompat.getColor(context, R.color.brand_red))
        snackbar.show()
    }


    fun generateToken(
            context: Context,
            grantType: String?,
            redrectUri: String?,
            code: String?,
            refreshToken: String?,
            apiSucessCallbackInterface: ApiSucessCallbackInterface
    ) {
        val url = ApiConstants.BASE_URL.plus("token")

        val requestQueue = Volley.newRequestQueue(context)

        LibreLogger.d(this, url)

        val tokenRequest = object : StringRequest(
                Request.Method.POST, url,
                Response.Listener { response ->

                    LibreLogger.d(this, "response$response")
                },

                Response.ErrorListener { volleyError ->

                    if (volleyError is TimeoutError || volleyError is NoConnectionError) {

                        buildSnackBarWithoutButton(
                                context,
                                (context as AppCompatActivity).window.decorView.findViewById(android.R.id.content),
                                "Seems your internet connection is slow, please try in sometime"
                        )

                    } else if (volleyError is AuthFailureError) {

                        buildSnackBarWithoutButton(
                                context,
                                (context as AppCompatActivity).window.decorView.findViewById(android.R.id.content),
                                "AuthFailure error occurred, please try again later"
                        )

                    } else if (volleyError is ServerError) {

                        buildSnackBarWithoutButton(
                                context,
                                (context as AppCompatActivity).window.decorView.findViewById(android.R.id.content),
                                "Server error occurred, please try again later"
                        )

                    } else if (volleyError is NetworkError) {

                        buildSnackBarWithoutButton(
                                context,
                                (context as AppCompatActivity).window.decorView.findViewById(android.R.id.content),
                                "Network error occurred, please try again later"
                        )

                    } else if (volleyError is ParseError) {
                        buildSnackBarWithoutButton(
                                context,
                                (context as AppCompatActivity).window.decorView.findViewById(android.R.id.content),
                                "Parser error occurred, please try again later"
                        )
                    }


                }) {
            override fun getHeaders(): MutableMap<String, String> {

                val params = HashMap<String, String>()

                val clientIdString =
                        context.resources.getString(R.string.vodafone_app_login_client_id)

                val clientSecret =
                        context.resources.getString(R.string.vodafone_app_login_client_secret)

                val authHeaderDataString = clientIdString.plus(":").plus(clientSecret)

                val authHeaderByteData = authHeaderDataString.toByteArray()


                val authoriationHeaderBase64 =
                        Base64.encodeToString(authHeaderByteData, Base64.NO_WRAP)


                params["Authorization"] = "Basic".plus(" ").plus(authoriationHeaderBase64)

                params["Content-Type"] = "application/x-www-form-urlencoded"

                LibreLogger.d(this, "header$params")

                return params
            }

            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()

                params["grant_type"] = grantType!!
                params["redirect_uri"] = redrectUri!!

                params["code"] = code!!

                if (refreshToken != null) {
                    params["refresh_token"] = refreshToken!!
                }

                return params
            }
        }


        tokenRequest.retryPolicy = DefaultRetryPolicy(
                30000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )

        requestQueue.add(tokenRequest)
    }


    fun getShareCodeForAlexaSkillActivation(

            accessToken: String?,
            context: Context,
            apiSucessCallbackInterface: ApiSucessCallbackInterface
    ) {


        val baseUrl = ApiConstants.BASE_URL.plus("share-code?")

        val requestQueue = Volley.newRequestQueue(context)

        val uri = Uri.parse(baseUrl)
                .buildUpon()
                .appendQueryParameter("grant_id", context.resources.getString(R.string.vodafone_gid))
                .build().toString()


        val stringRequest = object : StringRequest(Request.Method.GET, uri, Response.Listener {

            response ->

            LibreLogger.d(this, "share_code_response$response")

            val responseObject = JSONObject(response)

            val sharedPref: SharedPreferences =
                    context.getSharedPreferences("Libre", Context.MODE_PRIVATE)
            val editor: SharedPreferences.Editor = sharedPref.edit()

            editor.putString("sharingCode", responseObject.getString("sharing_code"))

            editor.apply()

            apiSucessCallbackInterface.onApiSucess(true)

        }, Response.ErrorListener { volleyError ->

            apiSucessCallbackInterface.onApiSucess(false)

            if (volleyError is TimeoutError || volleyError is NoConnectionError) {

                buildSnackBarWithoutButton(
                        context,
                        (context as AppCompatActivity).window.decorView.findViewById(android.R.id.content),
                        "Seems your internet connection is slow, please try in sometime"
                )

            } else if (volleyError is AuthFailureError) {

                buildSnackBarWithoutButton(
                        context,
                        (context as AppCompatActivity).window.decorView.findViewById(android.R.id.content),
                        "AuthFailure error occurred, please try again later"
                )

            } else if (volleyError is ServerError) {

                buildSnackBarWithoutButton(
                        context,
                        (context as AppCompatActivity).window.decorView.findViewById(android.R.id.content),
                        "Server error occurred, please try again later"
                )

            } else if (volleyError is NetworkError) {

                buildSnackBarWithoutButton(
                        context,
                        (context as AppCompatActivity).window.decorView.findViewById(android.R.id.content),
                        "Network error occurred, please try again later"
                )

            } else if (volleyError is ParseError) {
                buildSnackBarWithoutButton(
                        context,
                        (context as AppCompatActivity).window.decorView.findViewById(android.R.id.content),
                        "Parser error occurred, please try again later"
                )
            }

        }) {
            override fun getHeaders(): MutableMap<String, String> {
                val params = Hashtable<String, String>()

                params["Content-Type"] = "application/json"
                params["Authorization"] = "Bearer $accessToken"

                Log.d("atul_share_code_headers", params.toString())

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


    fun getAuthCodeForAlexaSkillActivation(
            context: Context,
            shareCode: String?,
            redirectURI: String?,
            sucessCallbackInterfaceWithParams: SucessCallbackInterfaceWithParams) {

        var state: String = UUID.randomUUID().toString()

        var url = Uri.parse(ApiConstants.BASE_URL + "authorize?")
                .buildUpon()
                .appendQueryParameter("response_type", "code")
                .appendQueryParameter(
                        "client_id",
                        context.resources.getString(R.string.vodafone_app_alexa_activation_link_client_id)
                )
                .appendQueryParameter("state", state)
                .appendQueryParameter("scope", "openid offline_access OPENID_TOKEN_SHARING_RECEIVER")
                .build().toString()


        url = url.plus(("&redirect_uri=").plus(redirectURI)).plus("&login_hint=").plus("sc:")
                .plus(shareCode)

        val requestQueue = Volley.newRequestQueue(context, object : HurlStack() {
            override fun createConnection(url: URL?): HttpURLConnection {
                val connection = super.createConnection(url)
                connection.instanceFollowRedirects = false
                return connection
            }
        })

        val stringRequest: StringRequest =
                object :
                        StringRequest(Request.Method.GET, url.toString(), Response.Listener { response ->

                        }, Response.ErrorListener { volleyError ->
                            if (volleyError.networkResponse != null) {
                                if (volleyError.networkResponse.statusCode == 302) {
                                    //url gets redirected
                                    val redirectedUrl: String? =
                                            volleyError.networkResponse.headers["Location"]
                                    sucessCallbackInterfaceWithParams.sucessCallbackWithParameter(redirectedUrl.toString())
                                }
                            }

                            if (volleyError is TimeoutError || volleyError is NoConnectionError) {
                                buildSnackBarWithoutButton(
                                        context,
                                        (context as AppCompatActivity).window.decorView.findViewById(android.R.id.content),
                                        "Seems your internet connection is slow, please try in sometime"
                                )
                            } else if (volleyError is AuthFailureError) {
                                buildSnackBarWithoutButton(
                                        context,
                                        (context as AppCompatActivity).window.decorView.findViewById(android.R.id.content),
                                        "AuthFailure error occurred, please try again later"
                                )

                            } else if (volleyError is ServerError) {
                                if (volleyError.networkResponse.statusCode != 302) {
                                    buildSnackBarWithoutButton(
                                            context,
                                            (context as AppCompatActivity).window.decorView.findViewById(android.R.id.content),
                                            "Server error occurred, please try again later"
                                    )
                                }

                            } else if (volleyError is NetworkError) {

                                buildSnackBarWithoutButton(
                                        context,
                                        (context as AppCompatActivity).window.decorView.findViewById(android.R.id.content),
                                        "Network error occurred, please try again later"
                                )

                            } else if (volleyError is ParseError) {
                                buildSnackBarWithoutButton(
                                        context,
                                        (context as AppCompatActivity).window.decorView.findViewById(android.R.id.content),
                                        "Parser error occurred, please try again later"
                                )
                            }
                        }) {

                }
        stringRequest.retryPolicy = DefaultRetryPolicy(
                30000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )

        requestQueue.add(stringRequest)
    }

    fun buildCustomAlertForLocationPermission(appCompatActivity: AppCompatActivity, onButtonClickFromCustomAlertDialog: ONButtonClickFromCustomAlertDialog) {


        if (!appCompatActivity.isFinishing) {
            val builder: AlertDialog.Builder =
                    AlertDialog.Builder(appCompatActivity, R.style.CustomTransparentDialog)

            val viewGroup: ViewGroup = appCompatActivity.findViewById(android.R.id.content)

            val dialogView: View =
                    LayoutInflater.from(appCompatActivity)
                            .inflate(R.layout.custom_location_permission_dialog, viewGroup, false)

            builder.setView(dialogView);

            builder.setCancelable(false)

            val alertDialog: AlertDialog = builder.create()

            val btnTryAgain: AppCompatButton = dialogView.findViewById(R.id.btnTryAgain)

            val btnGotoSettings: AppCompatButton = dialogView.findViewById(R.id.btnGotoSettings)

            btnTryAgain.setOnClickListener {
                alertDialog.dismiss()
                onButtonClickFromCustomAlertDialog.onButtonClick(btnTryAgain.text.toString())
            }

            btnGotoSettings.setOnClickListener {
                alertDialog.dismiss()
                onButtonClickFromCustomAlertDialog.onButtonClick(btnGotoSettings.text.toString())
            }
            alertDialog!!.show()
        }
    }


    fun buildCustomUserDoesNotActivateAlexaLogin(appCompatActivity: AppCompatActivity, onButtonClickFromCustomAlertDialog: ONButtonClickFromCustomAlertDialog) {


        if (!appCompatActivity.isFinishing) {
            val builder: AlertDialog.Builder =
                    AlertDialog.Builder(appCompatActivity, R.style.CustomTransparentDialog)

            val viewGroup: ViewGroup = appCompatActivity.findViewById(android.R.id.content)

            val dialogView: View =
                    LayoutInflater.from(appCompatActivity)
                            .inflate(R.layout.vdf_layout_user_does_not_activate_alexa, viewGroup, false)

            builder.setView(dialogView);

            builder.setCancelable(false)

            val alertDialog: AlertDialog = builder.create()

            val btnSignWithAlexa: AppCompatButton = dialogView.findViewById(R.id.btnSignWithAlexa)


            btnSignWithAlexa.setOnClickListener {
                alertDialog.dismiss()
                onButtonClickFromCustomAlertDialog.onButtonClick(btnSignWithAlexa.text.toString())
            }

            alertDialog!!.show()
        }
    }

    fun buildCustomFullScreenAlexaLoginError(appCompatActivity: AppCompatActivity, onButtonClickFromCustomAlertDialog: ONButtonClickFromCustomAlertDialog) {
        if (!appCompatActivity.isFinishing) {

            val builder: AlertDialog.Builder =
                    AlertDialog.Builder(appCompatActivity, R.style.CustomTransparentDialog)

            val viewGroup: ViewGroup = appCompatActivity.findViewById(android.R.id.content)

            val dialogView = LayoutInflater.from(appCompatActivity)
                    .inflate(R.layout.custom_alexa_login_error_layout, viewGroup, false)

            builder.setView(dialogView);

            builder.setCancelable(false)

            val alertDialog: AlertDialog = builder.create()

            val btnAmazonAlexaApp: AppCompatButton = dialogView.findViewById(R.id.btnAmazonAlexaApp)

            val btnTryAgain: AppCompatButton = dialogView.findViewById(R.id.btnTryAgain)

            btnAmazonAlexaApp.setOnClickListener {
                alertDialog.dismiss()
                onButtonClickFromCustomAlertDialog.onButtonClick(btnAmazonAlexaApp.text.toString())
            }

            btnTryAgain.setOnClickListener {
                alertDialog.dismiss()
                onButtonClickFromCustomAlertDialog.onButtonClick(btnTryAgain.text.toString())
            }

            alertDialog!!.show()

        }
    }

    fun buildCustomFullScreenForLs9DeviceNotFound(appCompatActivity: AppCompatActivity, onButtonClickFromCustomAlertDialog: ONButtonClickFromCustomAlertDialog) {
        if (!appCompatActivity.isFinishing) {

            val builder: AlertDialog.Builder =
                    AlertDialog.Builder(appCompatActivity, R.style.CustomTransparentDialog)

            val viewGroup: ViewGroup = appCompatActivity.findViewById(android.R.id.content)

            val dialogView = LayoutInflater.from(appCompatActivity)
                    .inflate(R.layout.custom_layout_when_super_wifi_is_not_found, viewGroup, false)

            builder.setView(dialogView);

            builder.setCancelable(false)

            val alertDialog: AlertDialog = builder.create()

            val btnTryAgain: AppCompatButton = dialogView.findViewById(R.id.btnTryAgain)

            btnTryAgain.setOnClickListener {
                alertDialog.dismiss()
                onButtonClickFromCustomAlertDialog.onButtonClick(btnTryAgain.text.toString())
            }

            alertDialog!!.show()
        }
    }

    fun showCustomDialogViewForAlexaLogout(appCompatActivity: AppCompatActivity,
                             onButtonClickFromCustomAlertDialog: ONButtonClickFromCustomAlertDialog) {
        if (!appCompatActivity.isFinishing) {

            val builder: AlertDialog.Builder =
                    AlertDialog.Builder(appCompatActivity, R.style.CustomTransparentDialog)

            val viewGroup: ViewGroup = appCompatActivity.findViewById(android.R.id.content)


            val dialogView = LayoutInflater.from(appCompatActivity).inflate(R.layout.custom_alert_vdf_alexa_logout, viewGroup, false)


            builder.setView(dialogView);

            builder.setCancelable(false)

            val alertDialog: AlertDialog = builder.create()

            val btnLogout: AppCompatButton = dialogView.findViewById(R.id.btnLogout)

            val btnLoggedIn: AppCompatButton = dialogView.findViewById(R.id.btnLoggedIn)

            btnLogout.setOnClickListener {
                alertDialog!!.dismiss()
                onButtonClickFromCustomAlertDialog.onButtonClick(btnLogout.text.toString())
            }

            btnLoggedIn.setOnClickListener {
                alertDialog!!.dismiss()
                onButtonClickFromCustomAlertDialog.onButtonClick(btnLoggedIn.text.toString())
            }
            alertDialog!!.show()
        }
    }

}