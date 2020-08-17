package com.libre.alexa.utils

import android.content.Context
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.android.volley.*
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.snackbar.Snackbar
import com.libre.alexa.util.LibreLogger
import org.json.JSONObject
import android.content.SharedPreferences
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.android.volley.toolbox.HurlStack
import com.libre.alexa.R
import com.libre.alexa.utils.interfaces.ApiSucessCallbackInterface
import com.libre.alexa.utils.interfaces.SucessCallbackInterfaceWithParams
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

}