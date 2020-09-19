package com.libre.alexa.receivers

import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.libre.alexa.LibreApplication
import com.libre.alexa.R
import com.libre.alexa.utils.UtilClass
import com.libre.alexa.utils.interfaces.ONButtonClickFromCustomAlertDialog


class WifiConnectionReceiver : BroadcastReceiver() {


    var alertDialog: AlertDialog? = null

    var isWifiEnabled = false

    override fun onReceive(context: Context?, intent: Intent?) {


        if (intent != null) {

            val wifiStateExtra: Int = intent!!.getIntExtra(WifiManager.EXTRA_WIFI_STATE,
                WifiManager.WIFI_STATE_UNKNOWN)

            when (wifiStateExtra) {
                WifiManager.WIFI_STATE_ENABLED -> {
                    //wifi is turned on
                    LibreApplication.wifiConnected = true
                    isWifiEnabled = true
                    alertDialog?.dismiss()
                    alertDialog = null
                }
                WifiManager.WIFI_STATE_DISABLED -> {
                    LibreApplication.wifiConnected = false
                    buildCustomAlertForWifiConnection(context as AppCompatActivity)
                    isWifiEnabled = false

                }
            }
        }
    }

    fun buildCustomAlertForWifiConnection(appCompatActivity: AppCompatActivity) {


        if (!appCompatActivity.isFinishing) {

            val builder: AlertDialog.Builder =
                AlertDialog.Builder(appCompatActivity, R.style.CustomTransparentDialog)

            val viewGroup: ViewGroup = appCompatActivity.findViewById(android.R.id.content)

            val dialogView: View =
                LayoutInflater.from(appCompatActivity)
                    .inflate(R.layout.custom_vdf_full_screen_alert_wifi_connection, viewGroup, false)

            builder.setView(dialogView);

            builder.setCancelable(false)

            alertDialog = builder.create()

            val btnTryAgain: AppCompatButton = dialogView.findViewById(R.id.btnTryAgain)


            btnTryAgain.setOnClickListener {
                if (isWifiEnabled) {
                    alertDialog?.dismiss()
                }
            }

            alertDialog!!.show()
        }
    }
}